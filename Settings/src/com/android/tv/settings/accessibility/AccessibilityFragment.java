/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tv.settings.accessibility;

import static android.content.Context.ACCESSIBILITY_SERVICE;

import static com.android.tv.settings.util.InstrumentationUtils.logToggleInteracted;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.admin.DevicePolicyManager;
import android.app.tvsettings.TvSettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;

import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.accessibility.AccessibilityUtils;
import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.settings.overlay.FlavorUtils;
import com.android.tv.settings.util.SliceUtils;
import com.android.tv.twopanelsettings.slices.SliceShard;
import com.android.tv.twopanelsettings.slices.SliceSwitchPreference;
import com.android.tv.twopanelsettings.slices.compat.Slice;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Fragment for Accessibility settings
 */
@Keep
public class AccessibilityFragment extends SettingsPreferenceFragment
        implements SliceShard.Callbacks {
    private static final String TOGGLE_HIGH_TEXT_CONTRAST_KEY = "toggle_high_text_contrast";
    private static final String TOGGLE_AUDIO_DESCRIPTION_KEY = "toggle_audio_description";
    private static final String TOGGLE_BOLD_TEXT_KEY = "toggle_bold_text";
    private static final String COLOR_CORRECTION_TWOPANEL_KEY = "color_correction_only_twopanel";
    private static final String COLOR_CORRECTION_CLASSIC_KEY = "color_correction_only_classic";
    private static final String ACCESSIBILITY_SHORTCUT_KEY = "accessibility_shortcut";
    private static final String ACCESSIBILITY_FRAGMENT_TAG = "accessibility_fragment";
    private static final String SERVICE_PREF_TAG = "ServicePref:";
    private static final String TOGGLE_BOUNCE_KEY = "toggle_bounce_key";
    private static final int BOLD_TEXT_ADJUSTMENT = 500;
    private static final int FIRST_PREFERENCE_IN_CATEGORY_INDEX = -1;
    private static final int BOUNCE_KEY_TIME_OUT = 500; //milliseconds

    private SharedPreferences mSharedPref;
    private Map<String, String> mServicesComponentSliceUriMap;
    private int mCurrentBounceKeyTimeout;

    private SliceShard mSliceShard;

    PreferenceCategory mServicesPrefCategory;
    PreferenceCategory mControlsPrefCategory;
    private SliceSwitchPreference mEnabledPref;

    private final Map<ComponentName, PreferenceCategory>
            mServiceComponentNameToPreferenceCategoryMap = new ArrayMap<>();

    private enum AccessibilityCategory {
        SCREEN_READERS("accessibility_screen_readers_category",
                R.array.config_preinstalled_screen_reader_services),
        DISPLAY("accessibility_display_category",
                R.array.config_preinstalled_display_services),
        INTERACTION_CONTROLS("accessibility_interaction_controls_category",
                R.array.config_preinstalled_interaction_control_services),
        AUDIO_AND_ONSCREEN_TEXT("accessibility_audio_and_onscreen_text_category",
                R.array.config_preinstalled_audio_and_onscreen_text_services),
        EXPERIMENTAL("accessibility_experimental_category",
                R.array.config_preinstalled_experimental_services),
        SERVICES("accessibility_services_category",
                R.array.config_preinstalled_additional_services);

        final String key;
        final int servicesArrayId;

        AccessibilityCategory(String key, int servicesArrayId) {
            this.key = key;
            this.servicesArrayId = servicesArrayId;
        }

        String getKey() {
            return this.key;
        }

        int getServicesArrayId() {
            return this.servicesArrayId;
        }
    }

    private AccessibilityManager.AccessibilityStateChangeListener
            mAccessibilityStateChangeListener = enabled -> refreshServices();

    /**
     * Create a new instance of the fragment
     * @return New fragment instance
     */
    public static AccessibilityFragment newInstance() {
        return new AccessibilityFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mSliceShard == null) {
            refreshServices();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        AccessibilityManager am = (AccessibilityManager)
                getContext().getSystemService(ACCESSIBILITY_SERVICE);
        if (am != null) {
            am.removeAccessibilityStateChangeListener(mAccessibilityStateChangeListener);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        var isTwoPanel = FlavorUtils.isTwoPanel(getContext());
        var sliceUri = SliceShard.Companion.getSliceUri(getResources(),
            isTwoPanel
                ? R.string.accessibility_fragment_slice_uri_two_panel
                : R.string.accessibility_fragment_slice_uri,
            R.string.main_fragment_slice_uri,
            isTwoPanel ? "accessibility_two_panel" : "accessibility");

        if (!SliceUtils.isSliceProviderValid(requireContext(), sliceUri)) {
            setPreferencesResource();
            configurePreferences();
            return;
        }

        setPreferencesFromResource(R.xml.settings_loading, null);
        mSliceShard = new SliceShard(this, sliceUri, this,
                getString(R.string.accessibility_category_title),
                SliceShard.Companion.getPrefContext(requireContext()), true);
    }

    private void setPreferencesResource() {
        if (FlavorUtils.isTwoPanel(getContext())) {
            setPreferencesFromResource(R.xml.accessibility_two_panel, null);
        } else {
            setPreferencesFromResource(R.xml.accessibility, null);
        }
    }

    @Override
    public void onSlice(@Nullable Slice slice) {
        mSliceShard = null;
        if (slice == null) {
            setPreferencesResource();
        }
        configurePreferences();
    }

    private void configurePreferences() {
        configureServicesMap();
        initBounceKeyTimeoutValue();

        final TwoStatePreference bounceKeyPreference =
                (TwoStatePreference) findPreference(TOGGLE_BOUNCE_KEY);
        bounceKeyPreference.setChecked(mCurrentBounceKeyTimeout != 0);
        if (FlavorUtils.isTwoPanel(getContext())) {
            if (mCurrentBounceKeyTimeout == 0) {
                bounceKeyPreference.setFragment(AccessibilityBounceKeyInfoFragment.class.getName());
            } else {
                bounceKeyPreference.setFragment(AccessibilityBounceKeyFragment.class.getName());
            }
        }

        final TwoStatePreference highContrastPreference =
                (TwoStatePreference) findPreference(TOGGLE_HIGH_TEXT_CONTRAST_KEY);
        highContrastPreference.setChecked(Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED, 0) == 1);

        final TwoStatePreference audioDescriptionPreference =
                (TwoStatePreference) findPreference(TOGGLE_AUDIO_DESCRIPTION_KEY);
        audioDescriptionPreference.setChecked(Settings.Secure.getInt(
                getContext().getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_AUDIO_DESCRIPTION_BY_DEFAULT, 0) == 1);

        final TwoStatePreference boldTextPreference =
                (TwoStatePreference) findPreference(TOGGLE_BOLD_TEXT_KEY);
        boldTextPreference.setChecked(Settings.Secure.getInt(
                getContext().getContentResolver(),
                Settings.Secure.FONT_WEIGHT_ADJUSTMENT, 0) == BOLD_TEXT_ADJUSTMENT);

        if (getContext()
                .getResources()
                .getBoolean(R.bool.config_showAccessibilityColorCorrection)) {
            Preference colorCorrectionPreferenceToSetVisible =
                    FlavorUtils.isTwoPanel(getContext())
                            ? (Preference) findPreference(COLOR_CORRECTION_TWOPANEL_KEY)
                            : (Preference) findPreference(COLOR_CORRECTION_CLASSIC_KEY);
            colorCorrectionPreferenceToSetVisible.setVisible(true);
        }

        mSharedPref = getContext().getSharedPreferences(
                ACCESSIBILITY_SHORTCUT_KEY, Context.MODE_PRIVATE);
        mServicesPrefCategory = findPreference(AccessibilityCategory.SERVICES.getKey());
        mControlsPrefCategory = findPreference(AccessibilityCategory.INTERACTION_CONTROLS.getKey());
        populateServiceToPreferenceCategoryMaps();
        refreshServices();
        AccessibilityManager am = (AccessibilityManager)
                getContext().getSystemService(ACCESSIBILITY_SERVICE);
        if (am != null) {
            am.addAccessibilityStateChangeListener(mAccessibilityStateChangeListener);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), TOGGLE_HIGH_TEXT_CONTRAST_KEY)) {
            logToggleInteracted(
                    TvSettingsEnums.SYSTEM_A11Y_HIGH_CONTRAST_TEXT,
                    ((SwitchPreference) preference).isChecked());
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED,
                    (((SwitchPreference) preference).isChecked() ? 1 : 0));
            return true;
        } else if (TextUtils.equals(preference.getKey(), TOGGLE_AUDIO_DESCRIPTION_KEY)) {
            logToggleInteracted(
                    TvSettingsEnums.SYSTEM_A11Y_AUDIO_DESCRIPTION,
                    ((SwitchPreference) preference).isChecked());
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_AUDIO_DESCRIPTION_BY_DEFAULT,
                    (((SwitchPreference) preference).isChecked() ? 1 : 0));
            return true;
        } else if (TextUtils.equals(preference.getKey(), TOGGLE_BOLD_TEXT_KEY)) {
            logToggleInteracted(
                    TvSettingsEnums.SYSTEM_A11Y_BOLD_TEXT,
                    ((SwitchPreference) preference).isChecked());
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.FONT_WEIGHT_ADJUSTMENT,
                    (((SwitchPreference) preference).isChecked() ? BOLD_TEXT_ADJUSTMENT : 0));
            return true;
        } else if (TextUtils.equals(preference.getKey(), ACCESSIBILITY_SHORTCUT_KEY)
                && FlavorUtils.isTwoPanel(getContext())) {
            logToggleInteracted(
                    TvSettingsEnums.SYSTEM_A11Y_SHORTCUT_ON_OFF,
                    ((SwitchPreference) preference).isChecked());
            AccessibilityShortcutUtils.setAccessibilityShortcutEnabled(
                    getContext(),
                    mSharedPref,
                    ((SwitchPreference) preference).isChecked());
            if (((SwitchPreference) preference).isChecked()) {
                preference.setFragment(AccessibilityShortcutServiceFragment.class.getName());
            } else {
                preference.setFragment(AccessibilityShortcutInfoFragment.class.getName());
            }
            return true;
        } else if (preference instanceof SliceSwitchPreference
                && preference.getKey() != null
                && preference.getKey().startsWith(SERVICE_PREF_TAG)) {
            mEnabledPref = (SliceSwitchPreference) preference;
            mEnabledPref.setChecked(!mEnabledPref.isChecked());
            launchConfirmationFragment(preference.getExtras());
            return true;
        } else if (TextUtils.equals(preference.getKey(), TOGGLE_BOUNCE_KEY)) {
            TwoStatePreference bounceKeyPreference = (TwoStatePreference) preference;
            if (((SwitchPreference) preference).isChecked()) {
                // If bounce key is on then set initial bounce key value as 500ms
                mCurrentBounceKeyTimeout = BOUNCE_KEY_TIME_OUT;
                setBounceKeyTimeoutValue(mCurrentBounceKeyTimeout);
                bounceKeyPreference.setFragment(AccessibilityBounceKeyFragment.class.getName());
            } else {
                // If bounce key is off then set initial bounce key value as 500ms
                mCurrentBounceKeyTimeout = 0;
                setBounceKeyTimeoutValue(mCurrentBounceKeyTimeout);
                bounceKeyPreference.setFragment(AccessibilityBounceKeyInfoFragment.class.getName());
            }
            return true;
        } else {
            return super.onPreferenceTreeClick(preference);
        }
    }

    private void populateServiceToPreferenceCategoryMaps() {
        for (AccessibilityCategory accessibilityCategory : AccessibilityCategory.values()) {
            String[] services = getResources().getStringArray(
                    accessibilityCategory.getServicesArrayId());
            PreferenceCategory prefCategory = findPreference(accessibilityCategory.getKey());
            for (int i = 0; i < services.length; i++) {
                ComponentName component = ComponentName.unflattenFromString(services[i]);
                mServiceComponentNameToPreferenceCategoryMap.put(component, prefCategory);
            }
        }
    }

    private void refreshServices() {
        if (!isAdded() || getActivity() == null) {
            return;
        }
        DevicePolicyManager dpm = getContext().getSystemService(DevicePolicyManager.class);
        final List<AccessibilityServiceInfo> installedServiceInfos =
                getActivity().getSystemService(AccessibilityManager.class)
                        .getInstalledAccessibilityServiceList();
        final Set<ComponentName> enabledServices =
                AccessibilityUtils.getEnabledServicesFromSettings(getActivity());
        final List<String> permittedServices = dpm.getPermittedAccessibilityServices(
                UserHandle.myUserId());

        Preference pref = mControlsPrefCategory.findPreference(ACCESSIBILITY_SHORTCUT_KEY);
        if (installedServiceInfos.size() == 0 && pref != null) {
            mControlsPrefCategory.removePreference(pref);
        } else if (pref != null && FlavorUtils.isTwoPanel(getContext())) {
            String enabledComponents = Settings.Secure.getString(getContext().getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE);
            boolean shortcutEnabled = !TextUtils.isEmpty(enabledComponents)
                    || TextUtils.isEmpty(AccessibilityShortcutUtils
                    .getLastShortcutService(mSharedPref));

            if (shortcutEnabled) {
                pref.setFragment(AccessibilityShortcutServiceFragment.class.getName());
            } else {
                pref.setFragment(AccessibilityShortcutInfoFragment.class.getName());
            }
            ((SwitchPreference) pref).setChecked(shortcutEnabled);
        }

        final boolean accessibilityEnabled = Settings.Secure.getInt(
                getActivity().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1;

        for (final AccessibilityServiceInfo accInfo : installedServiceInfos) {
            final ServiceInfo serviceInfo = accInfo.getResolveInfo().serviceInfo;
            final ComponentName componentName = new ComponentName(serviceInfo.packageName,
                    serviceInfo.name);
            final boolean serviceEnabled = accessibilityEnabled
                    && enabledServices.contains(componentName);
            // permittedServices null means all accessibility services are allowed.
            final boolean serviceAllowed = permittedServices == null
                    || permittedServices.contains(serviceInfo.packageName);

            String title = accInfo.getResolveInfo()
                    .loadLabel(getActivity().getPackageManager()).toString();

            final String key = SERVICE_PREF_TAG + componentName.flattenToString();
            Preference servicePref = findPreference(key);
            if (servicePref == null) {
                if (maybeUseSlice(componentName.flattenToString())) {
                    servicePref = new SliceSwitchPreference(getContext());
                } else {
                    servicePref = new RestrictedPreference(getContext());
                }
                servicePref.setKey(key);
            }
            if (componentName
                .flattenToString()
                    .equals(
                        getResources()
                            .getString(R.string
                                .accessibility_screen_reader_flattened_component_name))) {
                title = getResources().getString(R.string.screen_reader_service_title);
            }
            servicePref.setTitle(title);
            servicePref.setSummary(serviceEnabled ? R.string.settings_on : R.string.settings_off);

            if (serviceAllowed || serviceEnabled) {
                servicePref.setEnabled(true);
                if (maybeUseSlice(componentName.flattenToString())) {
                    AccessibilityServiceConfirmationFragment.prepareArgs(servicePref.getExtras(),
                            componentName, title, !serviceEnabled);
                    ((SliceSwitchPreference) servicePref).setChecked(serviceEnabled);
                    if (serviceEnabled) {
                        servicePref.setFragment(SliceUtils.PATH_SLICE_FRAGMENT);
                        ((SliceSwitchPreference) servicePref)
                                .setUri(mServicesComponentSliceUriMap
                                        .get(componentName.flattenToString()));
                    } else {
                        servicePref.setFragment(AccessibilityServiceInfoFragment.class.getName());
                    }
                } else {
                    AccessibilityServiceFragment.prepareArgs(servicePref.getExtras(),
                            serviceInfo.packageName,
                            serviceInfo.name,
                            accInfo.getSettingsActivityName(),
                            title);
                    servicePref.setFragment(AccessibilityServiceFragment.class.getName());
                }
            } else {
                // Disable accessibility service that are not permitted.
                final EnforcedAdmin admin =
                        RestrictedLockUtilsInternal.checkIfAccessibilityServiceDisallowed(
                                getContext(), serviceInfo.packageName, UserHandle.myUserId());
                if (admin != null) {
                    if (servicePref instanceof SliceSwitchPreference) {
                        ((SliceSwitchPreference) servicePref).setEnabled(false);
                    } else {
                        ((RestrictedPreference) servicePref).setDisabledByAdmin(admin);
                    }
                } else {
                    servicePref.setEnabled(false);
                }
                servicePref.setFragment(null);
            }

            // Make the screen reader component be the first preference in its preference category.
            final String screenReaderFlattenedComponentName = getResources().getString(
                    R.string.accessibility_screen_reader_flattened_component_name);
            if (componentName.flattenToString().equals(screenReaderFlattenedComponentName)) {
                servicePref.setOrder(FIRST_PREFERENCE_IN_CATEGORY_INDEX);
            }

            PreferenceCategory prefCategory = mServicesPrefCategory;
            if (mServiceComponentNameToPreferenceCategoryMap.containsKey(componentName)) {
                prefCategory = mServiceComponentNameToPreferenceCategoryMap.get(componentName);
            }
            // The method "addPreference" only adds the preference if it is not there already.
            if (!shouldShowLetterboxWallpapersSetting(componentName)) {
                prefCategory.addPreference(servicePref);
            }
        }
        mServicesPrefCategory.setVisible(mServicesPrefCategory.getPreferenceCount() != 0);
        mControlsPrefCategory.setVisible(mControlsPrefCategory.getPreferenceCount() != 0);
    }

    private void launchConfirmationFragment(Bundle arguments) {
        AccessibilityServiceConfirmationFragment confirmationFragment =
                new AccessibilityServiceConfirmationFragment();
        confirmationFragment.setArguments(arguments);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, confirmationFragment,
                        ACCESSIBILITY_FRAGMENT_TAG)
                .addToBackStack(null)
                .commit();

        requireActivity()
                .getSupportFragmentManager()
                .setFragmentResultListener(
                    AccessibilityServiceConfirmationFragment.REQUEST_KEY, this,
                        (requestKey, result) -> {
                            String flattenComponentName = result.getString(
                                    AccessibilityServiceConfirmationFragment
                                            .FLATTEN_SERVICE_COMPONENT_NAME, null);
                            if (flattenComponentName != null) {
                                ComponentName componentName = ComponentName
                                        .unflattenFromString(flattenComponentName);
                                boolean enabling = result.getBoolean(
                                        AccessibilityServiceConfirmationFragment.ARG_ENABLING,
                                        false);
                                onAccessibilityServiceConfirmed(componentName, enabling);
                            }
                        });
    }

    private void configureServicesMap() {
        mServicesComponentSliceUriMap = new ArrayMap<>();
        String[] stringArray = getResources().getStringArray(R.array.accessibility_services_map);
        if (stringArray != null && stringArray.length > 0) {
            for (int i = 0; i < stringArray.length; i += 2) {
                String key = stringArray[i];
                String value = (i + 1 < stringArray.length) ? stringArray[i + 1] : null;
                mServicesComponentSliceUriMap.put(key, value);
            }
        }
    }

    private void onAccessibilityServiceConfirmed(ComponentName componentName, boolean enabling) {
        AccessibilityUtils.setAccessibilityServiceState(getActivity(), componentName, enabling);
        if (mEnabledPref != null) {
            mEnabledPref.setChecked(enabling);
            // Check if component is talkback, then log
            if (componentName != null
                    && componentName.flattenToString().equals(
                    getResources().getString(
                            R.string.accessibility_screen_reader_flattened_component_name))) {
                logToggleInteracted(TvSettingsEnums.SYSTEM_A11Y_TALKBACK_ON_OFF, enabling);
            }
        }
        refreshServices();
    }

    private boolean maybeUseSlice(String componentName) {
        boolean usingSlice = mServicesComponentSliceUriMap.containsKey(componentName)
                && FlavorUtils.isTwoPanel(getContext())
                && SliceUtils.isSliceProviderValid(getContext(),
                mServicesComponentSliceUriMap.get(componentName));

        return usingSlice;
    }

    private void initBounceKeyTimeoutValue() {
        final ContentResolver resolver = getContext().getContentResolver();
        mCurrentBounceKeyTimeout =
                Settings.Secure.getInt(resolver, Settings.Secure.ACCESSIBILITY_BOUNCE_KEYS, 0);
    }

    /**
     * Setting new bounce keys value
     *
     * @param bounceKeyTimeOut is the time out value for bounce key feature
     */
    private void setBounceKeyTimeoutValue(int bounceKeyTimeOut) {
        final ContentResolver resolver = getContext().getContentResolver();
        Settings.Secure.putInt(resolver,
                Settings.Secure.ACCESSIBILITY_BOUNCE_KEYS, bounceKeyTimeOut);
    }

    @Override
    protected int getPageId() {
        return TvSettingsEnums.SYSTEM_A11Y;
    }

    // Whether to expose the "Letterbox wallpapers" accessibility entry to users.
    // This is overlayable to allow OEMs that ship the LetterboxWallpapers APK for its
    // AccessibilityService (non-UI) to hide the preference if the UI is not functional
    // or not intended for end users.
    private boolean shouldShowLetterboxWallpapersSetting(ComponentName componentName) {
        return componentName.flattenToString().equals(getResources().getString(
                R.string.accessibility_letterbox_wallpapers_component_name))
                && !getResources().getBoolean(R.bool.config_show_letterbox_wallpapers_setting);
    }
}
