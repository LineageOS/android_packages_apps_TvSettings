/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.tv.settings.device.displaysound;

import static android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION;
import static android.view.Display.HdrCapabilities.HDR_TYPE_HDR10;
import static android.view.Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS;
import static android.view.Display.HdrCapabilities.HDR_TYPE_HLG;

import static com.android.tv.settings.util.InstrumentationUtils.logEntrySelected;
import static com.android.tv.settings.util.InstrumentationUtils.logToggleInteracted;

import android.app.tvsettings.TvSettingsEnums;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;

import androidx.annotation.Keep;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;

import com.android.settingslib.core.AbstractPreferenceController;
import com.android.tv.settings.PreferenceControllerFragment;
import com.android.tv.settings.R;
import com.android.tv.settings.RadioPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This Fragment is responsible for allowing the user enable or disable the Hdr types which are
 * supported by device.
 */
@Keep
public class HdrFormatSelectionFragment extends PreferenceControllerFragment {

    static final String KEY_HDR_FORMAT_SELECTION = "hdr_format_selection_option";
    static final String KEY_SUPPORTED_HDR_FORMATS = "supported_formats";
    static final String KEY_UNSUPPORTED_HDR_FORMATS = "unsupported_formats";
    static final String KEY_HDR_FORMAT_SELECTION_AUTO = "hdr_format_selection_auto";
    static final String KEY_HDR_FORMAT_SELECTION_MANUAL = "hdr_format_selection_manual";
    static final String KEY_HDR_FORMAT_PREFIX = "hdr_format_";

    static final int[] HDR_FORMATS_DISPLAY_ORDER = {
        HDR_TYPE_DOLBY_VISION, HDR_TYPE_HDR10, HDR_TYPE_HDR10_PLUS, HDR_TYPE_HLG
    };

    private PreferenceCategory mSupportedFormatsPreferenceCategory;
    private PreferenceCategory mUnsupportedFormatsPreferenceCategory;

    private List<AbstractPreferenceController> mPreferenceControllers;

    private Set<Integer> mDeviceHdrTypes;
    private Set<Integer> mDisplayReportedHdrTypes;
    private Set<Integer> mUserDisabledHdrTypes;

    private DisplayManager mDisplayManager;

    /** @return the new instance of the class */
    public static HdrFormatSelectionFragment newInstance() {
        return new HdrFormatSelectionFragment();
    }

    @Override
    public void onAttach(Context context) {
        mDisplayManager = getDisplayManager();
        mDeviceHdrTypes = toSet(getDeviceSupportedHdrTypes());
        mUserDisabledHdrTypes = toSet(mDisplayManager.getUserDisabledHdrTypes());

        Display display = mDisplayManager.getDisplay(Display.DEFAULT_DISPLAY);
        mDisplayReportedHdrTypes = toSet(display.getReportedHdrTypes());

        super.onAttach(context);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.hdr_format_selection;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.hdr_format_selection, null);

        createFormatPreferences();

        String currentPreferenceKey;
        if (mDisplayManager.areUserDisabledHdrTypesAllowed()) {
            currentPreferenceKey = KEY_HDR_FORMAT_SELECTION_AUTO;
            hideFormatPreferences();
        } else {
            currentPreferenceKey = KEY_HDR_FORMAT_SELECTION_MANUAL;
            showFormatPreferences();
        }
        selectRadioPreference(findPreference(currentPreferenceKey));
    }

    @Override
    protected List<AbstractPreferenceController> onCreatePreferenceControllers(Context context) {
        mPreferenceControllers = new ArrayList<>();
        for (int hdrType : mDeviceHdrTypes) {
            if (mDisplayReportedHdrTypes.contains(hdrType)) {
                mPreferenceControllers.add(
                        new HdrFormatPreferenceController(getContext(), hdrType, mDisplayManager));
            }
        }
        return mPreferenceControllers;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();
        if (key != null && preference instanceof RadioPreference) {
            selectRadioPreference(preference);

            switch (key) {
                case KEY_HDR_FORMAT_SELECTION_AUTO: {
                    logEntrySelected(
                            TvSettingsEnums.DISPLAY_SOUND_ADVANCED_DISPLAY_FORMAT_SELECTION_AUTO);
                    mDisplayManager.setAreUserDisabledHdrTypesAllowed(true);
                    hideFormatPreferences();
                    break;
                }
                case KEY_HDR_FORMAT_SELECTION_MANUAL: {
                    logEntrySelected(
                            TvSettingsEnums.DISPLAY_SOUND_ADVANCED_DISPLAY_FORMAT_SELECTION_MANUAL);
                    mDisplayManager.setAreUserDisabledHdrTypesAllowed(false);
                    showFormatPreferences();
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown hdr type selection pref value"
                            + ": " + key);
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    private PreferenceGroup getPreferenceGroup() {
        return (PreferenceGroup) findPreference(KEY_HDR_FORMAT_SELECTION);
    }

    @VisibleForTesting
    DisplayManager getDisplayManager() {
        return getContext().getSystemService(DisplayManager.class);
    }

    @VisibleForTesting
    int[] getDeviceSupportedHdrTypes() {
        return getContext().getResources().getIntArray(R.array.config_deviceSupportedHdrFormats);
    }

    private void selectRadioPreference(Preference preference) {
        final RadioPreference radioPreference = (RadioPreference) preference;
        radioPreference.setChecked(true);
        radioPreference.clearOtherRadioPreferences(getPreferenceGroup());
    }

    private void createFormatPreferences() {
        mSupportedFormatsPreferenceCategory = createPreferenceCategory(
                R.string.hdr_format_supported_title,
                KEY_SUPPORTED_HDR_FORMATS);
        getPreferenceScreen().addPreference(mSupportedFormatsPreferenceCategory);
        mUnsupportedFormatsPreferenceCategory = createPreferenceCategory(
                R.string.hdr_format_unsupported_title,
                KEY_UNSUPPORTED_HDR_FORMATS);
        getPreferenceScreen().addPreference(mUnsupportedFormatsPreferenceCategory);

        for (int hdrType : HDR_FORMATS_DISPLAY_ORDER) {
            if (mDeviceHdrTypes.contains(hdrType)) {
                int titleId = getFormatPreferenceTitleId(hdrType);
                if (titleId == -1) {
                    continue;
                }
                if (mDisplayReportedHdrTypes.contains(hdrType)) {
                    boolean enabled = !mUserDisabledHdrTypes.contains(hdrType);
                    mSupportedFormatsPreferenceCategory.addPreference(
                            createSupportedFormatPreference(titleId, hdrType, enabled));
                } else {
                    mUnsupportedFormatsPreferenceCategory.addPreference(
                            createUnsupportedFormatPreference(titleId, hdrType));
                }
            }
        }
    }

    /** Returns a switch preference for each supported HDR format. */
    private Preference createSupportedFormatPreference(int titleId, int hdrType, boolean enabled) {
        final SwitchPreference pref = new SwitchPreference(getContext()) {
            @Override
            public void onBindViewHolder(PreferenceViewHolder holder) {
                super.onBindViewHolder(holder);
                holder.itemView.setEnabled(true);
            }
        };
        pref.setTitle(titleId);
        pref.setKey(KEY_HDR_FORMAT_PREFIX + hdrType);
        pref.setChecked(enabled);
        if (getLogEntryId(hdrType) != -1) {
            pref.setOnPreferenceClickListener(
                    preference -> {
                        logToggleInteracted(getLogEntryId(hdrType), pref.isChecked());
                        return false;
                    });
        }
        return pref;
    }

    /** Returns a non-switch preference for each unsupported HDR format. */
    private Preference createUnsupportedFormatPreference(int titleId, int hdrType) {
        Preference pref = new Preference(getContext());
        pref.setTitle(titleId);
        pref.setKey(KEY_HDR_FORMAT_PREFIX + hdrType);
        return pref;
    }

    private void showFormatPreferences() {
        getPreferenceScreen().addPreference(mSupportedFormatsPreferenceCategory);
        getPreferenceScreen().addPreference(mUnsupportedFormatsPreferenceCategory);
        updateFormatPreferencesStates();
    }

    private void hideFormatPreferences() {
        getPreferenceScreen().removePreference(mSupportedFormatsPreferenceCategory);
        getPreferenceScreen().removePreference(mUnsupportedFormatsPreferenceCategory);
        updateFormatPreferencesStates();
    }

    private PreferenceCategory createPreferenceCategory(int titleResourceId, String key) {
        PreferenceCategory preferenceCategory = new PreferenceCategory(getContext());
        preferenceCategory.setTitle(titleResourceId);
        preferenceCategory.setKey(key);
        return preferenceCategory;
    }

    private void updateFormatPreferencesStates() {
        for (AbstractPreferenceController controller : mPreferenceControllers) {
            Preference preference = findPreference(controller.getPreferenceKey());
            if (preference != null && preference instanceof SwitchPreference) {
                controller.updateState(preference);
            }
        }
    }

    /**
     * @return the display id for each hdr type.
     */
    private int getFormatPreferenceTitleId(int hdrType) {
        switch (hdrType) {
            case HDR_TYPE_DOLBY_VISION:
                return R.string.hdr_format_dolby_vision;
            case HDR_TYPE_HDR10:
                return R.string.hdr_format_hdr10;
            case HDR_TYPE_HLG:
                return R.string.hdr_format_hlg;
            case HDR_TYPE_HDR10_PLUS:
                return R.string.hdr_format_hdr10plus;
            default:
                return -1;
        }
    }

    private int getLogEntryId(int hdrType) {
        switch(hdrType) {
            case HDR_TYPE_DOLBY_VISION:
                return TvSettingsEnums.DISPLAY_SOUND_ADVANCED_DISPLAY_FORMAT_SELECTION_DOLBY_VISION;
            case HDR_TYPE_HDR10:
                return TvSettingsEnums.DISPLAY_SOUND_ADVANCED_DISPLAY_FORMAT_SELECTION_HDR10;
            case HDR_TYPE_HLG:
                return TvSettingsEnums.DISPLAY_SOUND_ADVANCED_DISPLAY_FORMAT_SELECTION_HLG;
            case HDR_TYPE_HDR10_PLUS:
                return TvSettingsEnums.DISPLAY_SOUND_ADVANCED_DISPLAY_FORMAT_SELECTION_HDR10_PLUS;
            default:
                return -1;
        }
    }

    private Set<Integer> toSet(int[] array) {
        return Arrays.stream(array).boxed().collect(Collectors.toSet());
    }
}
