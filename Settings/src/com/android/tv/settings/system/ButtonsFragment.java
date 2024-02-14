/*
 * Copyright (C) 2021-2024 The LineageOS Project
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
import androidx.preference.ListPreference;

import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;

import org.lineageos.internal.logging.LineageMetricsLogger;

/**
 * The button settings screen in TV settings.
 */
@Keep
public class ButtonsFragment extends SettingsPreferenceFragment {
    private static final String KEY_POWER_BUTTON_LONG_PRESS_ACTION =
            "power_button_long_press_action";

    private static final int LONG_PRESS_POWER_BUTTON_FOR_ASSISTANT = 1;
    private static final int LONG_PRESS_POWER_BUTTON_FOR_POWER_MENU = 0;

    private ListPreference mPowerButtonLongPressAction;

    public static ButtonsFragment newInstance() {
        return new ButtonsFragment();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.buttons, null);

        mPowerButtonLongPressAction = findPreference(KEY_POWER_BUTTON_LONG_PRESS_ACTION);
        if (PowerMenuSettingsUtils.isLongPressPowerSettingAvailable(getContext())) {
            mPowerButtonLongPressAction.setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    int action = Integer.parseInt((String) newValue);
                    switch (action) {
                        case LONG_PRESS_POWER_BUTTON_FOR_ASSISTANT:
                            PowerMenuSettingsUtils.setLongPressPowerForAssistant(getContext());
                            break;
                        case LONG_PRESS_POWER_BUTTON_FOR_POWER_MENU:
                            PowerMenuSettingsUtils.setLongPressPowerForPowerMenu(getContext());
                            break;
                    }
                    return true;
                });
        } else {
            getPreferenceScreen().removePreference(mPowerButtonLongPressAction);
        }
    }
}
