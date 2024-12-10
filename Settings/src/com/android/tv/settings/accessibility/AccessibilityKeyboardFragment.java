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

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Keep;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;
import android.text.TextUtils;

import com.android.tv.settings.R;
import com.android.tv.settings.RadioPreference;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.settings.overlay.FlavorUtils;
import androidx.preference.SwitchPreference;

@Keep
public class AccessibilityKeyboardFragment extends SettingsPreferenceFragment {
    private static final String TOGGLE_BOUNCE_KEY = "toggle_bounce_key";
    private int mCurrentBounceKeyTimeout;
    private TwoStatePreference bounceKeyPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.keyboard_accessibility, null);

        // Get initial bounce key value
        initBounceKeyTimeoutValue();

        final boolean isTwoPanel = FlavorUtils.isTwoPanel(getContext());
        bounceKeyPreference = (TwoStatePreference) findPreference(TOGGLE_BOUNCE_KEY);
        bounceKeyPreference.setChecked(mCurrentBounceKeyTimeout != 0);
        if (isTwoPanel) {
            if (mCurrentBounceKeyTimeout == 0) {
                bounceKeyPreference.setFragment(AccessibilityBounceKeyInfoFragment.class.getName());
            } else {
                bounceKeyPreference.setFragment(AccessibilityBounceKeyFragment.class.getName());
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), TOGGLE_BOUNCE_KEY)) {
            if (((SwitchPreference) preference).isChecked()) {
                // If bounce key is on then set initial bounce key value as 500ms
                mCurrentBounceKeyTimeout = 500;
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
}
