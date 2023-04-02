/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.tv.settings.system;

import androidx.preference.Preference;

import android.os.Bundle;
import android.text.TextUtils;

import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;

/**
 * The "LiveDisplay" screen in TV settings.
 */
public class LiveDisplayFragment extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_COLOR_PROFILE = "color_profile";

    public static LiveDisplayFragment newInstance() {
        return new LiveDisplayFragment();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.livedisplay, null);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final String key = preference.getKey();
        if (TextUtils.isEmpty(key)) {
            return super.onPreferenceTreeClick(preference);
        }
        switch (key) {
            case KEY_COLOR_PROFILE:
                // TODO
                return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        if (TextUtils.isEmpty(key)) {
            throw new IllegalStateException("Unknown preference change");
        }
        switch (key) {
            case KEY_COLOR_PROFILE:
                // TODO
                break;
            default:
                throw new IllegalStateException("Preference change with unknown key " + key);
        }
        return true;
    }
}
