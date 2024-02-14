/*
 * Copyright (C) 2021, 2024 The LineageOS Project
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

import android.app.tvsettings.TvSettingsEnums;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.tv.settings.R;
import com.android.tv.settings.RadioPreference;
import com.android.tv.settings.SettingsPreferenceFragment;

import org.lineageos.internal.logging.LineageMetricsLogger;

/**
 * The button settings screen in TV settings.
 */
@Keep
public class ButtonsFragment extends SettingsPreferenceFragment {
    private static final String KEY_GESTURE_POWER_MENU_LONG_PRESS_FOR_ASSISTANT =
            "gesture_power_menu_long_press_for_assistant";
    private static final String KEY_GESTURE_POWER_MENU_LONG_PRESS_FOR_POWER_MENU =
            "gesture_power_menu_long_press_for_power_menu";
    private static final String KEY_GESTURE_POWER_MENU_LONG_PRESS_GROUP =
            "gesture_power_menu_long_press_category";

    private PreferenceGroup mPowerKeyLongPressGroup;
    private RadioPreference mPowerKeyLongPressForAssistant;
    private RadioPreference mPowerKeyLongPressForPowerMenu;

    public static ButtonsFragment newInstance() {
        return new ButtonsFragment();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.buttons, null);

        mPowerKeyLongPressForAssistant =
                findPreference(KEY_GESTURE_POWER_MENU_LONG_PRESS_FOR_ASSISTANT);
        mPowerKeyLongPressForPowerMenu =
                findPreference(KEY_GESTURE_POWER_MENU_LONG_PRESS_FOR_POWER_MENU);
        mPowerKeyLongPressGroup = findPreference(KEY_GESTURE_POWER_MENU_LONG_PRESS_GROUP);

        if (!PowerMenuSettingsUtils.isLongPressPowerSettingAvailable(getContext())) {
            getPreferenceScreen().removePreference(mPowerKeyLongPressGroup);
            return;
        }

        // Set the default value
        if (PowerMenuSettingsUtils.isLongPressPowerForAssistantEnabled(getContext())) {
            mPowerKeyLongPressForAssistant.setChecked(true);
            mPowerKeyLongPressForPowerMenu.setChecked(false);
        } else {
            mPowerKeyLongPressForAssistant.setChecked(false);
            mPowerKeyLongPressForPowerMenu.setChecked(true);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getKey() == null) {
            return super.onPreferenceTreeClick(preference);
        }
        switch (preference.getKey()) {
            case KEY_GESTURE_POWER_MENU_LONG_PRESS_FOR_ASSISTANT:
                PowerMenuSettingsUtils.setLongPressPowerForAssistant(getContext());
                mPowerKeyLongPressForAssistant.clearOtherRadioPreferences(
                        mPowerKeyLongPressGroup);
                return true;
            case KEY_GESTURE_POWER_MENU_LONG_PRESS_FOR_POWER_MENU:
                PowerMenuSettingsUtils.setLongPressPowerForPowerMenu(getContext());
                mPowerKeyLongPressForPowerMenu.clearOtherRadioPreferences(
                        mPowerKeyLongPressGroup);
                return true;
        }
        return super.onPreferenceTreeClick(preference);
    }
}
