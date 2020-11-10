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
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
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

    static final String VAL_SURROUND_SOUND_AUTO = "auto";
    static final String VAL_SURROUND_SOUND_NEVER = "never";
    static final String VAL_SURROUND_SOUND_ALWAYS = "always";
    static final String VAL_SURROUND_SOUND_MANUAL = "manual";

    private AudioManager mAudioManager;
    private Map<Integer, Boolean> mFormats;
    private Map<Integer, Boolean> mReportedFormats;
    private List<AbstractPreferenceController> mPreferenceControllers;

    public static DisplaySoundFragment newInstance() {
        return new DisplaySoundFragment();
    }

    @Override
    public void onAttach(Context context) {
        mAudioManager = context.getSystemService(AudioManager.class);
        mFormats = mAudioManager.getSurroundFormats();
        mReportedFormats = mAudioManager.getReportedSurroundFormats();
        super.onAttach(context);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.advanced_sound;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.advanced_sound, null);


        final ListPreference surroundPref = findPreference(KEY_SURROUND_PASSTHROUGH);
        surroundPref.setValue(getSurroundPassthroughSetting(getContext()));
        surroundPref.setOnPreferenceChangeListener(this);

        createFormatPreferences();
        updateFormatPreferencesStates();
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

    /** Creates and adds switches for each surround sound format. */
    private void createFormatPreferences() {
        for (Map.Entry<Integer, Boolean> format : mFormats.entrySet()) {
            int formatId = format.getKey();
            boolean enabled = format.getValue();
            SwitchPreference pref = new SwitchPreference(getPreferenceManager().getContext()) {
                @Override
                public void onBindViewHolder(PreferenceViewHolder holder) {
                    super.onBindViewHolder(holder);
                    // Enabling the view will ensure that the preference is focusable even if it
                    // the preference is disabled. This allows the user to scroll down over the
                    // disabled surround sound formats and see them all.
                    holder.itemView.setEnabled(true);
                }
            };
            pref.setTitle(getFormatDisplayName(formatId));
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
            getPreferenceScreen().addPreference(pref);
        }
    }

    /**
     * @return the display name for each surround sound format.
     */
    private String getFormatDisplayName(int formatId) {
        switch (formatId) {
            case AudioFormat.ENCODING_AC3:
                return getContext().getResources().getString(R.string.surround_sound_format_ac3);
            case AudioFormat.ENCODING_E_AC3:
                return getContext().getResources().getString(R.string.surround_sound_format_e_ac3);
            case AudioFormat.ENCODING_DTS:
                return getContext().getResources().getString(R.string.surround_sound_format_dts);
            case AudioFormat.ENCODING_DTS_HD:
                return getContext().getResources().getString(R.string.surround_sound_format_dts_hd);
            default:
                // Fallback in case new formats have been added that we don't know of.
                return AudioFormat.toDisplayName(formatId);
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
                    break;
                case VAL_SURROUND_SOUND_NEVER:
                    logEntrySelected(
                            TvSettingsEnums.DISPLAY_SOUND_ADVANCED_SOUNDS_SELECT_FORMATS_NONE);
                    setSurroundPassthroughSetting(Settings.Global.ENCODED_SURROUND_OUTPUT_NEVER);
                    break;
                case VAL_SURROUND_SOUND_ALWAYS:
                    // On Android P ALWAYS is replaced by MANUAL.
                    logEntrySelected(
                            TvSettingsEnums.DISPLAY_SOUND_ADVANCED_SOUNDS_SELECT_FORMATS_MANUAL);
                    setSurroundPassthroughSetting(Settings.Global.ENCODED_SURROUND_OUTPUT_ALWAYS);
                    break;
                case VAL_SURROUND_SOUND_MANUAL:
                    logEntrySelected(
                            TvSettingsEnums.DISPLAY_SOUND_ADVANCED_SOUNDS_SELECT_FORMATS_MANUAL);
                    setSurroundPassthroughSetting(Settings.Global.ENCODED_SURROUND_OUTPUT_MANUAL);
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
            case Settings.Global.ENCODED_SURROUND_OUTPUT_AUTO:
            default:
                return VAL_SURROUND_SOUND_AUTO;
            case Settings.Global.ENCODED_SURROUND_OUTPUT_NEVER:
                return VAL_SURROUND_SOUND_NEVER;
            // On Android P ALWAYS is replaced by MANUAL.
            case Settings.Global.ENCODED_SURROUND_OUTPUT_ALWAYS:
            case Settings.Global.ENCODED_SURROUND_OUTPUT_MANUAL:
                return VAL_SURROUND_SOUND_MANUAL;
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
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SOUND;
    }

    @Override
    protected int getPageId() {
        return TvSettingsEnums.DISPLAY_SOUND_ADVANCED_SOUNDS;
    }
}
