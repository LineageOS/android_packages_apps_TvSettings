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

import static com.android.tv.settings.util.InstrumentationUtils.logEntrySelected;
import static com.android.tv.settings.util.InstrumentationUtils.logToggleInteracted;

import android.app.tvsettings.TvSettingsEnums;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.Keep;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.tv.settings.PreferenceControllerFragment;
import com.android.tv.settings.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The "Advanced sound settings" screen in TV Settings.
 */
@Keep
public class AdvancedVolumeFragment extends PreferenceControllerFragment implements
        Preference.OnPreferenceChangeListener {
    static final String KEY_SURROUND_PASSTHROUGH = "surround_passthrough";
    static final String KEY_SURROUND_SOUND_FORMAT_PREFIX = "surround_sound_format_";
    static final String KEY_SUPPORTED_SURROUND_SOUND = "supported_formats";
    static final String KEY_UNSUPPORTED_SURROUND_SOUND = "unsupported_formats";

    static final String VAL_SURROUND_SOUND_AUTO = "auto";
    static final String VAL_SURROUND_SOUND_NEVER = "never";
    static final String VAL_SURROUND_SOUND_MANUAL = "manual";

    static final int[] SURROUND_SOUND_DISPLAY_ORDER = {
            AudioFormat.ENCODING_AC3, AudioFormat.ENCODING_E_AC3, AudioFormat.ENCODING_DOLBY_TRUEHD,
            AudioFormat.ENCODING_E_AC3_JOC, AudioFormat.ENCODING_DOLBY_MAT,
            AudioFormat.ENCODING_DTS, AudioFormat.ENCODING_DTS_HD
    };

    private Map<Integer, Boolean> mFormats;
    private Map<Integer, Boolean> mReportedFormats;
    private List<AbstractPreferenceController> mPreferenceControllers;
    private PreferenceCategory mSupportedFormatsPreferenceCategory;
    private PreferenceCategory mUnsupportedFormatsPreferenceCategory;

    @Override
    public void onAttach(Context context) {
        AudioManager audioManager = getAudioManager();
        mFormats = audioManager.getSurroundFormats();
        mReportedFormats = audioManager.getReportedSurroundFormats();
        super.onAttach(context);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.advanced_sound;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.advanced_sound, null /* key */);

        final ListPreference surroundPref = findPreference(KEY_SURROUND_PASSTHROUGH);
        String surroundPassthroughSetting = getSurroundPassthroughSetting(getContext());
        surroundPref.setValue(surroundPassthroughSetting);
        surroundPref.setOnPreferenceChangeListener(this);

        createFormatPreferences();
        if (surroundPassthroughSetting == VAL_SURROUND_SOUND_MANUAL) {
            showFormatPreferences();
        } else {
            hideFormatPreferences();
        }
    }

    @Override
    protected List<AbstractPreferenceController> onCreatePreferenceControllers(Context context) {
        mPreferenceControllers = new ArrayList<>(mFormats.size());
        for (Map.Entry<Integer, Boolean> format : mFormats.entrySet()) {
            mPreferenceControllers.add(new SoundFormatPreferenceController(context,
                    format.getKey() /*formatId*/, mFormats, mReportedFormats));
        }
        return mPreferenceControllers;
    }

    @VisibleForTesting
    AudioManager getAudioManager() {
        return getContext().getSystemService(AudioManager.class);
    }

    /** Creates titles and switches for each surround sound format. */
    private void createFormatPreferences() {
        mSupportedFormatsPreferenceCategory = createPreferenceCategory(
                R.string.surround_sound_supported_title,
                KEY_SUPPORTED_SURROUND_SOUND);
        getPreferenceScreen().addPreference(mSupportedFormatsPreferenceCategory);
        mUnsupportedFormatsPreferenceCategory = createPreferenceCategory(
                R.string.surround_sound_unsupported_title,
                KEY_UNSUPPORTED_SURROUND_SOUND);
        getPreferenceScreen().addPreference(mUnsupportedFormatsPreferenceCategory);

        for (int formatId : SURROUND_SOUND_DISPLAY_ORDER) {
            if (mFormats.containsKey(formatId)) {
                boolean enabled = mFormats.get(formatId);

                // If the format is not a known surround sound format, do not create a preference
                // for it.
                int titleId = getFormatDisplayResourceId(formatId);
                if (titleId == -1) {
                    continue;
                }
                final SwitchPreference pref = new SwitchPreference(getContext()) {
                    @Override
                    public void onBindViewHolder(PreferenceViewHolder holder) {
                        super.onBindViewHolder(holder);
                        // Enabling the view will ensure that the preference is focusable even if it
                        // the preference is disabled. This allows the user to scroll down over the
                        // disabled surround sound formats and see them all.
                        holder.itemView.setEnabled(true);
                    }
                };
                pref.setTitle(titleId);
                pref.setKey(KEY_SURROUND_SOUND_FORMAT_PREFIX + formatId);
                pref.setChecked(enabled);
                if (getEntryId(formatId) != -1) {
                    pref.setOnPreferenceClickListener(
                            preference -> {
                                logToggleInteracted(getEntryId(formatId), pref.isChecked());
                                return false;
                            }
                    );
                }
                if (mReportedFormats.containsKey(formatId)) {
                    mSupportedFormatsPreferenceCategory.addPreference(pref);
                } else {
                    mUnsupportedFormatsPreferenceCategory.addPreference(pref);
                }
            }
        }
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

    /**
     * @return the display id for each surround sound format.
     */
    private int getFormatDisplayResourceId(int formatId) {
        switch (formatId) {
            case AudioFormat.ENCODING_AC3:
                return R.string.surround_sound_format_ac3;
            case AudioFormat.ENCODING_E_AC3:
                return R.string.surround_sound_format_e_ac3;
            case AudioFormat.ENCODING_DTS:
                return R.string.surround_sound_format_dts;
            case AudioFormat.ENCODING_DTS_HD:
                return R.string.surround_sound_format_dts_hd;
            case AudioFormat.ENCODING_DOLBY_TRUEHD:
                return R.string.surround_sound_format_dolby_truehd;
            case AudioFormat.ENCODING_E_AC3_JOC:
                return R.string.surround_sound_format_e_ac3_joc;
            case AudioFormat.ENCODING_DOLBY_MAT:
                return R.string.surround_sound_format_dolby_mat;
            default:
                return -1;
        }
    }

    private void updateFormatPreferencesStates() {
        for (AbstractPreferenceController controller : mPreferenceControllers) {
            Preference preference = findPreference(
                    controller.getPreferenceKey());
            if (preference != null) {
                controller.updateState(preference);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (TextUtils.equals(preference.getKey(), KEY_SURROUND_PASSTHROUGH)) {
            final String selection = (String) newValue;
            switch (selection) {
                case VAL_SURROUND_SOUND_AUTO:
                    logEntrySelected(
                            TvSettingsEnums.DISPLAY_SOUND_ADVANCED_SOUNDS_SELECT_FORMATS_AUTO);
                    setSurroundPassthroughSetting(Settings.Global.ENCODED_SURROUND_OUTPUT_AUTO);
                    hideFormatPreferences();
                    break;
                case VAL_SURROUND_SOUND_NEVER:
                    logEntrySelected(
                            TvSettingsEnums.DISPLAY_SOUND_ADVANCED_SOUNDS_SELECT_FORMATS_NONE);
                    setSurroundPassthroughSetting(Settings.Global.ENCODED_SURROUND_OUTPUT_NEVER);
                    hideFormatPreferences();
                    break;
                case VAL_SURROUND_SOUND_MANUAL:
                    logEntrySelected(
                            TvSettingsEnums.DISPLAY_SOUND_ADVANCED_SOUNDS_SELECT_FORMATS_MANUAL);
                    setSurroundPassthroughSetting(Settings.Global.ENCODED_SURROUND_OUTPUT_MANUAL);
                    showFormatPreferences();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown surround sound pref value: "
                            + selection);
            }
            updateFormatPreferencesStates();
            return true;
        }
        return true;
    }

    private void setSurroundPassthroughSetting(int newVal) {
        Settings.Global.putInt(getContext().getContentResolver(),
                Settings.Global.ENCODED_SURROUND_OUTPUT, newVal);
    }

    static String getSurroundPassthroughSetting(Context context) {
        final int value = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.ENCODED_SURROUND_OUTPUT,
                Settings.Global.ENCODED_SURROUND_OUTPUT_AUTO);

        switch (value) {
            case Settings.Global.ENCODED_SURROUND_OUTPUT_MANUAL:
                return VAL_SURROUND_SOUND_MANUAL;
            case Settings.Global.ENCODED_SURROUND_OUTPUT_NEVER:
                return VAL_SURROUND_SOUND_NEVER;
            case Settings.Global.ENCODED_SURROUND_OUTPUT_AUTO:
            default:
                return VAL_SURROUND_SOUND_AUTO;
        }
    }

    private int getEntryId(int formatId) {
        switch(formatId) {
            case AudioFormat.ENCODING_AC4:
                return TvSettingsEnums.DISPLAY_SOUND_ADVANCED_SOUNDS_DAC4;
            case AudioFormat.ENCODING_E_AC3_JOC:
                return TvSettingsEnums.DISPLAY_SOUND_ADVANCED_SOUNDS_DADDP;
            case AudioFormat.ENCODING_AC3:
                return TvSettingsEnums.DISPLAY_SOUND_ADVANCED_SOUNDS_DD;
            case AudioFormat.ENCODING_E_AC3:
                return TvSettingsEnums.DISPLAY_SOUND_ADVANCED_SOUNDS_DDP;
            case AudioFormat.ENCODING_DTS:
                return TvSettingsEnums.DISPLAY_SOUND_ADVANCED_SOUNDS_DTS;
            case AudioFormat.ENCODING_DTS_HD:
                return TvSettingsEnums.DISPLAY_SOUND_ADVANCED_SOUNDS_DTSHD;
            case AudioFormat.ENCODING_AAC_LC:
                return TvSettingsEnums.DISPLAY_SOUND_ADVANCED_SOUNDS_AAC;
            case AudioFormat.ENCODING_DOLBY_TRUEHD:
                return TvSettingsEnums.DISPLAY_SOUND_ADVANCED_SOUNDS_DTHD;
            default:
                return -1;
        }
    }

    @Override
    protected int getPageId() {
        return TvSettingsEnums.DISPLAY_SOUND_ADVANCED_SOUNDS;
    }
}
