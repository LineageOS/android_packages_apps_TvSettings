/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.tv.settings;

import static com.android.tv.settings.overlay.FlavorUtils.FLAVOR_CLASSIC;
import static com.android.tv.settings.overlay.FlavorUtils.FLAVOR_TWO_PANEL;
import static com.android.tv.settings.overlay.FlavorUtils.FLAVOR_VENDOR;
import static com.android.tv.settings.overlay.FlavorUtils.FLAVOR_X;
import static com.android.tv.settings.util.InstrumentationUtils.logEntrySelected;
import static com.android.tv.settings.util.InstrumentationUtils.logPageFocused;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.tvsettings.TvSettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.service.settings.suggestions.Suggestion;
import android.telephony.SignalStrength;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Keep;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.suggestions.SuggestionControllerMixinCompat;
import com.android.settingslib.utils.IconCache;
import com.android.tv.settings.HotwordSwitchController.HotwordStateListener;
import com.android.tv.settings.accounts.AccountsFragment;
import com.android.tv.settings.connectivity.ConnectivityListener;
import com.android.tv.settings.overlay.FlavorUtils;
import com.android.tv.settings.suggestions.SuggestionPreference;
import com.android.tv.settings.system.SecurityFragment;
import com.android.tv.settings.util.SliceUtils;
import com.android.tv.twopanelsettings.TwoPanelSettingsFragment;
import com.android.tv.twopanelsettings.slices.SlicePreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The fragment where all good things begin. Evil is handled elsewhere.
 */
@Keep
public class MainFragment extends PreferenceControllerFragment implements
        SuggestionControllerMixinCompat.SuggestionControllerHost, SuggestionPreference.Callback,
        HotwordStateListener {

    private static final String TAG = "MainFragment";
    private static final String KEY_SUGGESTIONS_LIST = "suggestions";
    private static final String KEY_BASIC_MODE_SUGGESTION = "basic_mode_suggestion";
    private static final String KEY_BASIC_MODE_EXIT = "basic_mode_exit";
    @VisibleForTesting
    static final String KEY_ACCOUNTS_AND_SIGN_IN = "accounts_and_sign_in";
    @VisibleForTesting
    static final String KEY_ACCOUNTS_AND_SIGN_IN_SLICE = "accounts_and_sign_in_slice";
    @VisibleForTesting
    static final String KEY_ACCOUNTS_AND_SIGN_IN_BASIC_MODE = "accounts_and_sign_in_basic_mode";
    private static final String KEY_APPLICATIONS = "applications";
    @VisibleForTesting
    static final String KEY_ACCESSORIES = "remotes_and_accessories";
    @VisibleForTesting
    static final String KEY_CONNECTED_DEVICES = "connected_devices";
    private static final String KEY_CONNECTED_DEVICES_SLICE = "connected_devices_slice";
    @VisibleForTesting
    static final String KEY_NETWORK = "network";
    @VisibleForTesting
    static final String KEY_SOUND = "sound";
    public static final String ACTION_SOUND = "com.android.tv.settings.SOUND";
    @VisibleForTesting
    static final String ACTION_CONNECTED_DEVICES = "com.android.tv.settings.CONNECTED_DEVICES";
    @VisibleForTesting
    static final String KEY_PRIVACY = "privacy";
    @VisibleForTesting
    static final String KEY_DISPLAY_AND_SOUND = "display_and_sound";
    @VisibleForTesting
    static final String KEY_QUICK_SETTINGS = "quick_settings";
    private static final String KEY_CHANNELS_AND_INPUTS = "channels_and_inputs";

    private static final String ACTION_ACCOUNTS = "com.android.tv.settings.ACCOUNTS";
    @VisibleForTesting
    ConnectivityListener mConnectivityListener;
    @VisibleForTesting
    PreferenceCategory mSuggestionsList;
    private SuggestionControllerMixinCompat mSuggestionControllerMixin;
    @VisibleForTesting
    IconCache mIconCache;
    @VisibleForTesting
    BluetoothAdapter mBtAdapter;
    @VisibleForTesting
    boolean mHasBtAccessories;
    @VisibleForTesting
    boolean mHasAccounts;

    /** Controllers for the Quick Settings section. */
    private List<AbstractPreferenceController> mPreferenceControllers;
    private HotwordSwitchController mHotwordSwitchController;
    private TakeBugReportController mTakeBugReportController;
    private PreferenceCategory mQuickSettingsList;
    private SwitchPreference mHotwordSwitch;
    private Preference mTakeBugReportPreference;

    private final BroadcastReceiver mBCMReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateAccessoryPref();
        }
    };

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Override
    protected int getPreferenceScreenResId() {
        switch (FlavorUtils.getFlavor(getContext())) {
            case FLAVOR_CLASSIC:
            case FLAVOR_TWO_PANEL:
                return R.xml.main_prefs;
            case FLAVOR_X:
                return R.xml.main_prefs_x;
            case FLAVOR_VENDOR:
                return R.xml.main_prefs_vendor;
            default:
                return R.xml.main_prefs;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mIconCache = new IconCache(getContext());
        mConnectivityListener = new ConnectivityListener(getContext(), this::updateConnectivity,
                getSettingsLifecycle());
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        super.onCreate(savedInstanceState);
        // This is to record the initial start of Settings root in two panel settings case, as the
        // MainFragment is the left-most pane and will not be slided in from preview pane. For
        // classic settings case, the event will be recorded in onResume() as this is an instance
        // of SettingsPreferenceFragment.
        if (getCallbackFragment() instanceof TwoPanelSettingsFragment) {
            logPageFocused(getPageId(), true);
        }
    }

    @Override
    public void onDestroy() {
        if (mHotwordSwitchController != null) {
            mHotwordSwitchController.unregister();
        }
        super.onDestroy();
    }

    private boolean quickSettingsEnabled() {
        return getContext().getResources().getBoolean(R.bool.config_quick_settings_enabled);
    }

    /** @return true if there is at least one available item in quick settings. */
    private boolean shouldShowQuickSettings() {
        for (AbstractPreferenceController controller : mPreferenceControllers) {
            if (controller.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    private void showOrHideQuickSettings() {
        if (shouldShowQuickSettings()) {
            showQuickSettings();
        } else {
            hideQuickSettings();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        showOrHideQuickSettings();
        updateAccountPref();
        updateAccessoryPref();
        updateConnectivity();
        updateBasicModeSuggestion();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    /** Creates the quick settings category and its children. */
    private void showQuickSettings() {
        if (mQuickSettingsList != null) {
            return;
        }
        mQuickSettingsList = new PreferenceCategory(this.getPreferenceManager().getContext());
        mQuickSettingsList.setKey(KEY_QUICK_SETTINGS);
        mQuickSettingsList.setTitle(R.string.header_category_quick_settings);
        mQuickSettingsList.setOrder(1); // at top, but below suggested settings
        getPreferenceScreen().addPreference(mQuickSettingsList);
        if (mHotwordSwitchController != null && mHotwordSwitchController.isAvailable()) {
            mHotwordSwitch = new SwitchPreference(this.getPreferenceManager().getContext());
            mHotwordSwitch.setKey(HotwordSwitchController.KEY_HOTWORD_SWITCH);
            mHotwordSwitch.setOnPreferenceClickListener(
                    preference -> {
                        logEntrySelected(TvSettingsEnums.QUICK_SETTINGS);
                        return false;
                    }
            );
            mHotwordSwitchController.updateState(mHotwordSwitch);
            mQuickSettingsList.addPreference(mHotwordSwitch);
        }
        if (mTakeBugReportController != null && mTakeBugReportController.isAvailable()) {
            mTakeBugReportPreference = new Preference(this.getPreferenceManager().getContext());
            mTakeBugReportPreference.setKey(TakeBugReportController.KEY_TAKE_BUG_REPORT);
            mTakeBugReportPreference.setOnPreferenceClickListener(
                    preference -> {
                        logEntrySelected(TvSettingsEnums.QUICK_SETTINGS);
                        return false;
                    }
            );
            mTakeBugReportController.updateState(mTakeBugReportPreference);
            mQuickSettingsList.addPreference(mTakeBugReportPreference);
        }
    }

    /** Removes the quick settings category and all its children. */
    private void hideQuickSettings() {
        Preference quickSettingsPref = findPreference(KEY_QUICK_SETTINGS);
        if (quickSettingsPref == null) {
            return;
        }
        mQuickSettingsList.removeAll();
        getPreferenceScreen().removePreference(mQuickSettingsList);
        mQuickSettingsList = null;
    }

    @Override
    public void onHotwordStateChanged() {
        if (mHotwordSwitch != null && mHotwordSwitchController != null) {
            mHotwordSwitchController.updateState(mHotwordSwitch);
        }
        showOrHideQuickSettings();
    }

    @Override
    public void onHotwordEnable() {
        try {
            Intent intent = new Intent(HotwordSwitchController.ACTION_HOTWORD_ENABLE);
            intent.setPackage(HotwordSwitchController.ASSISTANT_PGK_NAME);
            startActivityForResult(intent, 0);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Unable to find hotwording activity.", e);
        }
    }

    @Override
    public void onHotwordDisable() {
        try {
            Intent intent = new Intent(HotwordSwitchController.ACTION_HOTWORD_DISABLE);
            intent.setPackage(HotwordSwitchController.ASSISTANT_PGK_NAME);
            startActivityForResult(intent, 0);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Unable to find hotwording activity.", e);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(getPreferenceScreenResId(), null);
        if (isRestricted()) {
            Preference appPref = findPreference(KEY_APPLICATIONS);
            if (appPref != null) {
                appPref.setVisible(false);
            }
            Preference accountsPref = findPreference(KEY_ACCOUNTS_AND_SIGN_IN);
            if (accountsPref != null) {
                accountsPref.setVisible(false);
            }
        }
        if (!supportBluetooth()) {
            Preference accessoryPreference = findPreference(KEY_ACCESSORIES);
            if (accessoryPreference != null) {
                accessoryPreference.setVisible(false);
            }
        }
        if (FlavorUtils.isTwoPanel(getContext())) {
            Preference displaySoundPref = findPreference(KEY_DISPLAY_AND_SOUND);
            if (displaySoundPref != null) {
                displaySoundPref.setVisible(true);
            }
            Preference privacyPref = findPreference(KEY_PRIVACY);
            if (privacyPref != null) {
                privacyPref.setVisible(true);
            }
        }
        if (mHotwordSwitchController != null) {
            mHotwordSwitchController.init(this);
        }
        updateSoundSettings();
    }

    @Override
    protected List<AbstractPreferenceController> onCreatePreferenceControllers(Context context) {
        mPreferenceControllers = new ArrayList<>(2);
        if (quickSettingsEnabled()) {
            mHotwordSwitchController = new HotwordSwitchController(context);
            mTakeBugReportController = new TakeBugReportController(context);
            mPreferenceControllers.add(mHotwordSwitchController);
            mPreferenceControllers.add(mTakeBugReportController);
        }
        return mPreferenceControllers;
    }

    @VisibleForTesting
    void updateConnectivity() {
        final Preference networkPref = findPreference(KEY_NETWORK);
        if (networkPref == null) {
            return;
        }

        if (mConnectivityListener.isCellConnected()) {
            final int signal = mConnectivityListener.getCellSignalStrength();
            switch (signal) {
                case SignalStrength.SIGNAL_STRENGTH_GREAT:
                    networkPref.setIcon(R.drawable.ic_cell_signal_4_white);
                    break;
                case SignalStrength.SIGNAL_STRENGTH_GOOD:
                    networkPref.setIcon(R.drawable.ic_cell_signal_3_white);
                    break;
                case SignalStrength.SIGNAL_STRENGTH_MODERATE:
                    networkPref.setIcon(R.drawable.ic_cell_signal_2_white);
                    break;
                case SignalStrength.SIGNAL_STRENGTH_POOR:
                    networkPref.setIcon(R.drawable.ic_cell_signal_1_white);
                    break;
                case SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN:
                default:
                    networkPref.setIcon(R.drawable.ic_cell_signal_0_white);
                    break;
            }
        } else if (mConnectivityListener.isEthernetConnected()) {
            networkPref.setIcon(R.drawable.ic_ethernet_white);
            networkPref.setSummary(R.string.connectivity_summary_ethernet_connected);
        } else if (mConnectivityListener.isWifiEnabledOrEnabling()) {
            if (mConnectivityListener.isWifiConnected()) {
                final int signal = mConnectivityListener.getWifiSignalStrength(5);
                switch (signal) {
                    case 4:
                        networkPref.setIcon(R.drawable.ic_wifi_signal_4_white);
                        break;
                    case 3:
                        networkPref.setIcon(R.drawable.ic_wifi_signal_3_white);
                        break;
                    case 2:
                        networkPref.setIcon(R.drawable.ic_wifi_signal_2_white);
                        break;
                    case 1:
                        networkPref.setIcon(R.drawable.ic_wifi_signal_1_white);
                        break;
                    case 0:
                    default:
                        networkPref.setIcon(R.drawable.ic_wifi_signal_0_white);
                        break;
                }
                networkPref.setSummary(mConnectivityListener.getSsid());
            } else {
                networkPref.setIcon(R.drawable.ic_wifi_not_connected);
                networkPref.setSummary(R.string.connectivity_summary_no_network_connected);
            }
        } else {
            networkPref.setIcon(R.drawable.ic_wifi_signal_off_white);
            networkPref.setSummary(R.string.connectivity_summary_wifi_disabled);
        }
    }

    @VisibleForTesting
    void updateSoundSettings() {
        final Preference soundPref = findPreference(KEY_SOUND);
        if (soundPref != null) {
            Intent soundIntent = new Intent(ACTION_SOUND);
            final ResolveInfo info = systemIntentIsHandled(getContext(), soundIntent);
            soundPref.setVisible(info != null);
            if (info != null && info.activityInfo != null) {
                String pkgName = info.activityInfo.packageName;
                Drawable icon = getDrawableResource(pkgName, "sound_icon");
                if (icon != null) {
                    soundPref.setIcon(icon);
                }
                String title = getStringResource(pkgName, "sound_pref_title");
                if (!TextUtils.isEmpty(title)) {
                    soundPref.setTitle(title);
                }
                String summary = getStringResource(pkgName, "sound_pref_summary");
                if (!TextUtils.isEmpty(summary)) {
                    soundPref.setSummary(summary);
                }
            }
        }
    }

    /**
     * Extracts a string resource from a given package.
     *
     * @param pkgName  the package name
     * @param resource name, e.g. "my_string_name"
     */
    private String getStringResource(String pkgName, String resourceName) {
        try {
            Context targetContext = getContext().createPackageContext(pkgName, 0);
            int resId = targetContext.getResources().getIdentifier(
                    pkgName + ":string/" + resourceName, null, null);
            if (resId != 0) {
                return targetContext.getResources().getString(resId);
            }
        } catch (Resources.NotFoundException | PackageManager.NameNotFoundException
                | SecurityException e) {
            Log.w(TAG, "Unable to get string resource " + resourceName, e);
        }
        return null;
    }

    /**
     * Extracts an drawable resource from a given package.
     *
     * @param pkgName  the package name
     * @param resource name, e.g. "my_icon_name"
     */
    private Drawable getDrawableResource(String pkgName, String resourceName) {
        try {
            Context targetContext = getContext().createPackageContext(pkgName, 0);
            int resId = targetContext.getResources().getIdentifier(
                    pkgName + ":drawable/" + resourceName, null, null);
            if (resId != 0) {
                return targetContext.getResources().getDrawable(resId);
            }
        } catch (Resources.NotFoundException | PackageManager.NameNotFoundException
                | SecurityException e) {
            Log.w(TAG, "Unable to get drawable resource " + resourceName, e);
        }
        return null;
    }

    /**
     * Returns the ResolveInfo for the system activity that matches given intent filter or null if
     * no such activity exists.
     *
     * @param context Context of the caller
     * @param intent  The intent matching the desired system app
     * @return ResolveInfo of the matching activity or null if no match exists
     */
    public static ResolveInfo systemIntentIsHandled(Context context, Intent intent) {
        if (intent == null) {
            return null;
        }

        final PackageManager pm = context.getPackageManager();
        for (ResolveInfo info : pm.queryIntentActivities(intent, 0)) {
            if (info.activityInfo != null
                    && (info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)
                    == ApplicationInfo.FLAG_SYSTEM) {
                return info;
            }
        }
        return null;
    }

    private static ProviderInfo getProviderInfo(Context context, String authority) {
        return context.getPackageManager().resolveContentProvider(authority, 0);
    }

    @Override
    public void onSuggestionReady(List<Suggestion> data) {
        // Suggestion category is handled differently in basic mode
        if (data == null || data.size() == 0
                || FlavorUtils.getFeatureFactory(getContext())
                .getBasicModeFeatureProvider().isBasicMode(getContext())) {
            if (mSuggestionsList != null) {
                getPreferenceScreen().removePreference(mSuggestionsList);
                mSuggestionsList = null;
            }
            return;
        }

        if (mSuggestionsList == null) {
            mSuggestionsList = new PreferenceCategory(this.getPreferenceManager().getContext());
            mSuggestionsList.setKey(KEY_SUGGESTIONS_LIST);
            mSuggestionsList.setTitle(R.string.header_category_suggestions);
            mSuggestionsList.setOrder(0); // always at top
            getPreferenceScreen().addPreference(mSuggestionsList);
        }
        updateSuggestionList(data);
    }

    @VisibleForTesting
    void updateSuggestionList(List<Suggestion> suggestions) {
        // Remove suggestions that are not in the new list.
        for (int i = 0; i < mSuggestionsList.getPreferenceCount(); i++) {
            SuggestionPreference pref = (SuggestionPreference) mSuggestionsList.getPreference(i);
            boolean isInNewSuggestionList = false;
            for (Suggestion suggestion : suggestions) {
                if (pref.getId().equals(suggestion.getId())) {
                    isInNewSuggestionList = true;
                    break;
                }
            }
            if (!isInNewSuggestionList) {
                mSuggestionsList.removePreference(pref);
            }
        }

        // Add suggestions that are not in the old list and update the existing suggestions.
        for (Suggestion suggestion : suggestions) {
            Preference curPref = findPreference(
                    SuggestionPreference.SUGGESTION_PREFERENCE_KEY + suggestion.getId());
            if (curPref == null) {
                SuggestionPreference newSuggPref = new SuggestionPreference(
                        suggestion, this.getPreferenceManager().getContext(),
                        mSuggestionControllerMixin, this);
                newSuggPref.setIcon(mIconCache.getIcon(suggestion.getIcon()));
                newSuggPref.setTitle(suggestion.getTitle());
                newSuggPref.setSummary(suggestion.getSummary());
                mSuggestionsList.addPreference(newSuggPref);
            } else {
                // Even though the id of suggestion might not change, the details could change.
                // So we need to update icon, title and summary for the suggestions.
                curPref.setIcon(mIconCache.getIcon(suggestion.getIcon()));
                curPref.setTitle(suggestion.getTitle());
                curPref.setSummary(suggestion.getSummary());
            }
        }
    }

    private boolean isRestricted() {
        return SecurityFragment.isRestrictedProfileInEffect(getContext());
    }

    @VisibleForTesting
    void updateAccessoryPref() {
        SlicePreference connectedDevicesSlicePreference =
                (SlicePreference) findPreference(KEY_CONNECTED_DEVICES_SLICE);
        Preference accessoryPreference = findPreference(KEY_ACCESSORIES);
        Preference connectedDevicesPreference = findPreference(KEY_CONNECTED_DEVICES);
        if (connectedDevicesSlicePreference != null
                && FlavorUtils.isTwoPanel(getContext())
                && SliceUtils.isSliceProviderValid(
                getContext(), connectedDevicesSlicePreference.getUri())) {
            connectedDevicesSlicePreference.setVisible(true);
            connectedDevicesPreference.setVisible(false);
            accessoryPreference.setVisible(false);
            ProviderInfo pkgInfo = getProviderInfo(getContext(),
                    Uri.parse(connectedDevicesSlicePreference.getUri()).getAuthority());
            if (pkgInfo != null) {
                updateConnectedDevicePref(pkgInfo.packageName, connectedDevicesSlicePreference);
            }
            return;
        }

        if (connectedDevicesSlicePreference != null) {
            connectedDevicesSlicePreference.setVisible(false);
        }

        if (connectedDevicesPreference != null) {
            Intent intent = new Intent(ACTION_CONNECTED_DEVICES);
            ResolveInfo info = systemIntentIsHandled(getContext(), intent);
            connectedDevicesPreference.setVisible(info != null);
            accessoryPreference.setVisible(info == null);
            if (info != null) {
                updateConnectedDevicePref(
                        info.activityInfo.packageName, connectedDevicesPreference);
                return;
            }
        }
        if (mBtAdapter == null || accessoryPreference == null) {
            return;
        }

        final Set<BluetoothDevice> bondedDevices = mBtAdapter.getBondedDevices();
        mHasBtAccessories = bondedDevices.size() != 0;
    }

    @VisibleForTesting
    void updateAccountPref() {
        Preference accountsPref = findPreference(KEY_ACCOUNTS_AND_SIGN_IN);
        SlicePreference accountsSlicePref =
                (SlicePreference) findPreference(KEY_ACCOUNTS_AND_SIGN_IN_SLICE);
        Preference accountsBasicMode = findPreference(KEY_ACCOUNTS_AND_SIGN_IN_BASIC_MODE);
        Intent intent = new Intent(ACTION_ACCOUNTS);

        if (FlavorUtils.getFeatureFactory(getContext()).getBasicModeFeatureProvider()
                .isBasicMode(getContext())) {
            if (accountsBasicMode != null) {
                accountsBasicMode.setVisible(true);
            }
            if (accountsPref != null) {
                accountsPref.setVisible(false);
            }
            if (accountsSlicePref != null) {
                accountsSlicePref.setVisible(false);
            }
            return;
        } else {
            if (accountsBasicMode != null) {
                accountsBasicMode.setVisible(false);
            }
        }

        // If the intent can be handled, use it.
        if (systemIntentIsHandled(getContext(), intent) != null) {
            accountsPref.setVisible(true);
            accountsPref.setFragment(null);
            accountsPref.setIntent(intent);
            accountsSlicePref.setVisible(false);
            return;
        }

        // If a slice is available, use it to display the accounts settings, otherwise fall back to
        // use AccountsFragment.
        String uri = accountsSlicePref.getUri();
        if (SliceUtils.isSliceProviderValid(getContext(), uri)) {
            accountsPref.setVisible(false);
            accountsSlicePref.setVisible(true);
        } else {
            accountsPref.setVisible(true);
            accountsSlicePref.setVisible(false);
            updateAccountPrefInfo();
        }
    }

    @VisibleForTesting
    void updateAccountPrefInfo() {
        Preference accountsPref = findPreference(KEY_ACCOUNTS_AND_SIGN_IN);
        if (accountsPref != null && accountsPref.isVisible()) {
            final AccountManager am = AccountManager.get(getContext());
            Account[] accounts = am.getAccounts();
            if (accounts.length == 0) {
                mHasAccounts = false;
                accountsPref.setIcon(R.drawable.ic_add_an_account);
                accountsPref.setSummary(R.string.accounts_category_summary_no_account);
                AccountsFragment.setUpAddAccountPrefIntent(accountsPref, getContext());
            } else {
                mHasAccounts = true;
                accountsPref.setIcon(R.drawable.ic_accounts_and_sign_in);
                if (accounts.length == 1) {
                    accountsPref.setSummary(accounts[0].name);
                } else {
                    accountsPref.setSummary(getResources().getQuantityString(
                            R.plurals.accounts_category_summary, accounts.length, accounts.length));
                }
            }
        }
    }

    @VisibleForTesting
    void updateBasicModeSuggestion() {
        PreferenceCategory basicModeSuggestion = findPreference(KEY_BASIC_MODE_SUGGESTION);
        if (basicModeSuggestion == null) {
            return;
        }
        if (FlavorUtils.getFeatureFactory(getContext())
                .getBasicModeFeatureProvider().isBasicMode(getContext())) {
            basicModeSuggestion.setVisible(true);
        } else {
            basicModeSuggestion.setVisible(false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter btChangeFilter = new IntentFilter();
        btChangeFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        btChangeFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        btChangeFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        getContext().registerReceiver(mBCMReceiver, btChangeFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        getContext().unregisterReceiver(mBCMReceiver);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ComponentName componentName = new ComponentName(
                "com.android.settings.intelligence",
                "com.android.settings.intelligence.suggestions.SuggestionService");
        if (!isRestricted()) {
            mSuggestionControllerMixin = new SuggestionControllerMixinCompat(
                    context, this, getSettingsLifecycle(), componentName);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getKey().equals(KEY_ACCOUNTS_AND_SIGN_IN) && !mHasAccounts
                || (preference.getKey().equals(KEY_ACCESSORIES) && !mHasBtAccessories)
                || (preference.getKey().equals(KEY_DISPLAY_AND_SOUND)
                && preference.getIntent() != null)
                || (preference.getKey().equals(KEY_CHANNELS_AND_INPUTS)
                && preference.getIntent() != null)) {
            getContext().startActivity(preference.getIntent());
            return true;
        } else if (preference.getKey().equals(KEY_BASIC_MODE_EXIT)
                && FlavorUtils.getFeatureFactory(getContext())
                .getBasicModeFeatureProvider().isBasicMode(getContext())) {
            if (getActivity() != null) {
                FlavorUtils.getFeatureFactory(getContext())
                        .getBasicModeFeatureProvider().startBasicModeExitActivity(getActivity());
            }
            return true;
        } else {
            return super.onPreferenceTreeClick(preference);
        }
    }

    @Override
    public void onSuggestionClosed(Preference preference) {
        if (mSuggestionsList == null || mSuggestionsList.getPreferenceCount() == 0) {
            return;
        } else if (mSuggestionsList.getPreferenceCount() == 1) {
            getPreferenceScreen().removePreference(mSuggestionsList);
        } else {
            mSuggestionsList.removePreference(preference);
        }
    }

    private boolean supportBluetooth() {
        return getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
    }

    private void updateConnectedDevicePref(String pkgName, Preference pref) {
        Drawable icon = getDrawableResource(pkgName, "connected_devices_pref_icon");
        if (icon != null) {
            pref.setIcon(icon);
        }
        String title =
                (pref instanceof SlicePreference)
                        ? getStringResource(pkgName, "connected_devices_slice_pref_title")
                        : getStringResource(pkgName, "connected_devices_pref_title");
        if (!TextUtils.isEmpty(title)) {
            pref.setTitle(title);
        }
        String summary = getStringResource(pkgName, "connected_devices_pref_summary");
        if (!TextUtils.isEmpty(summary)) {
            pref.setSummary(summary);
        }
        pref.setOnPreferenceClickListener(
                preference -> {
                    logEntrySelected(TvSettingsEnums.CONNECTED_CLASSIC);
                    return false;
                });
    }

    @Override
    protected int getPageId() {
        return TvSettingsEnums.TV_SETTINGS_ROOT;
    }
}
