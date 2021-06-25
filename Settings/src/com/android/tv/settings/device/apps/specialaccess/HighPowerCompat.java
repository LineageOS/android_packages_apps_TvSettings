/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.tv.settings.device.apps.specialaccess;

import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.Preference;

import com.android.tv.settings.R;
import com.android.tv.settings.compat.HasKeys;
import com.android.tv.settings.compat.PreferenceControllerFragmentCompat;
import com.android.tv.settings.compat.RenderUtil;
import com.android.tv.settings.compat.TsPreference;
import com.android.tv.settings.library.ManagerUtil;
import com.android.tv.settings.library.PreferenceCompat;

import java.util.List;

/**
 * Fragment compat for managing power save allowlist
 */
@Keep
public class HighPowerCompat extends PreferenceControllerFragmentCompat {

    @Override
    public void updateAllPref(List<PreferenceCompat> preferenceCompatList) {
        if (preferenceCompatList == null) {
            return;
        }
        preferenceCompatList.stream()
                .forEach(preferenceCompat -> updatePref(preferenceCompat));
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.manage_high_power_compat, null);
    }

    @Override
    public HasKeys updatePref(PreferenceCompat pref) {
        if (pref == null) {
            return null;
        }
        String[] key = pref.getKey();
        Preference preference = findTargetPreference(key);
        if (preference == null) {
            preference = new TsPreference(getContext(), pref.getKey());
            getPreferenceScreen().addPreference(preference);
        }
        RenderUtil.updatePreference(
                getContext(), (HasKeys) preference, pref, preference.getOrder());
        return (HasKeys) preference;
    }

    @Override
    public int getState() {
        return ManagerUtil.STATE_HIGH_POWER;
    }
}
