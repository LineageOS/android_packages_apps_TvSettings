/*
 * Copyright 2020 The Android Open Source Project
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

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Keep;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.tv.settings.R;
import com.android.tv.settings.RadioPreference;
import com.android.tv.settings.SettingsPreferenceFragment;

/**
 * This Fragment is responsible for allowing the user to express a preference for matching the
 * display frame rate to to the frame rate of a video being played.
 */
@Keep
public class MatchContentFrameRateFragment extends SettingsPreferenceFragment {

    private static final String KEY_MATCH_CONTENT_FRAME_RATE = "match_content_frame_rate_option";

    private static final String KEY_MATCH_CONTENT_FRAME_RATE_AUTO = "match_content_frame_rate_auto";
    private static final String KEY_MATCH_CONTENT_FRAME_RATE_ALWAYS =
            "match_content_frame_rate_always";
    private static final String KEY_MATCH_CONTENT_FRAME_RATE_NEVER =
            "match_content_frame_rate_never";

    private String mCurrentPreferenceKey;


    /** @return the new instance of the class */
    public static MatchContentFrameRateFragment newInstance() {
        return new MatchContentFrameRateFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.match_content_frame_rate, null);

        mCurrentPreferenceKey = preferenceKeyFromSetting();
        onPreferenceTreeClick(getRadioPreference(mCurrentPreferenceKey));
    }

    @VisibleForTesting
    PreferenceGroup getPreferenceGroup() {
        return (PreferenceGroup) findPreference(KEY_MATCH_CONTENT_FRAME_RATE);
    }

    RadioPreference getRadioPreference(String key) {
        return (RadioPreference) findPreference(key);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final String key = preference.getKey();
        if (key == null) {
            return super.onPreferenceTreeClick(preference);
        }
        if (preference instanceof RadioPreference) {
            final RadioPreference radioPreference = (RadioPreference) preference;
            radioPreference.setChecked(true);
            radioPreference.clearOtherRadioPreferences(getPreferenceGroup());

            if (key != mCurrentPreferenceKey) {
                switch (key) {
                    case KEY_MATCH_CONTENT_FRAME_RATE_AUTO: {
                        Settings.Secure.putInt(
                                getContext().getContentResolver(),
                                Settings.Secure.MATCH_CONTENT_FRAME_RATE,
                                Settings.Secure.MATCH_CONTENT_FRAMERATE_SEAMLESSS_ONLY);
                        break;
                    }
                    case KEY_MATCH_CONTENT_FRAME_RATE_ALWAYS: {
                        Settings.Secure.putInt(
                                getContext().getContentResolver(),
                                Settings.Secure.MATCH_CONTENT_FRAME_RATE,
                                Settings.Secure.MATCH_CONTENT_FRAMERATE_ALWAYS);
                        break;
                    }
                    case KEY_MATCH_CONTENT_FRAME_RATE_NEVER: {
                        Settings.Secure.putInt(
                                getContext().getContentResolver(),
                                Settings.Secure.MATCH_CONTENT_FRAME_RATE,
                                Settings.Secure.MATCH_CONTENT_FRAMERATE_NEVER);
                        break;
                    }
                    default:
                        throw new IllegalArgumentException(
                                "Unknown match content frame rate pref value"
                                        + ": " + key);
                }
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    private String preferenceKeyFromSetting() {
        int matchContentSetting = Settings.Secure.getInt(
                getContext().getContentResolver(),
                Settings.Secure.MATCH_CONTENT_FRAME_RATE,
                Settings.Secure.MATCH_CONTENT_FRAMERATE_SEAMLESSS_ONLY);
        switch (matchContentSetting) {
            case (Settings.Secure.MATCH_CONTENT_FRAMERATE_NEVER): {
                return KEY_MATCH_CONTENT_FRAME_RATE_NEVER;
            }
            case (Settings.Secure.MATCH_CONTENT_FRAMERATE_SEAMLESSS_ONLY): {
                return KEY_MATCH_CONTENT_FRAME_RATE_AUTO;
            }
            case (Settings.Secure.MATCH_CONTENT_FRAMERATE_ALWAYS): {
                return KEY_MATCH_CONTENT_FRAME_RATE_ALWAYS;
            }
            default:
                throw new IllegalArgumentException("Unknown match content frame rate pref "
                        + "value in stored settings");
        }
    }
}
