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
import androidx.lifecycle.ViewModelProvider;
import com.android.tv.settings.R;
import com.android.tv.settings.RadioPreference;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.settings.accessibility.viewmodel.KeyRepeatViewModel;
import com.android.tv.settings.overlay.FlavorUtils;

@Keep
public class AccessibilityRepeatRateFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private int mCurrentRepeatRate;
    private boolean isTwoPanel;
    private KeyRepeatViewModel viewModel;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.accessibility_repeat_rate, null);
        viewModel = new ViewModelProvider(requireActivity()).get(KeyRepeatViewModel.class);
        PreferenceScreen repeatRateScreen = getPreferenceManager().getPreferenceScreen();
        final Context themedContext = getPreferenceManager().getContext();
        isTwoPanel = FlavorUtils.isTwoPanel(getContext());
        final String[] entries =
                getContext().getResources().getStringArray(R.array.a11y_repeat_rate_entries);
        final int[] entryValues =
                getContext().getResources().getIntArray(R.array.a11y_repeat_rate_values);

        // Setting initial repeat rate
        initRepeatRateValue();
        for (int i = 0; i < entries.length; i++) {
            final RadioPreference radioPreference = new RadioPreference(themedContext);
            radioPreference.setTitle(entries[i]);
            radioPreference.setKey(String.valueOf(entryValues[i]));
            radioPreference.setOnPreferenceChangeListener(this);
            radioPreference.setPersistent(false);
            if (mCurrentRepeatRate == entryValues[i]) {
                radioPreference.setChecked(true);
            }
            if (isTwoPanel) {
                if (i == 0) {
                    // Setting information fragment only for default value
                    radioPreference.setFragment(
                            AccessibilityKeyRepeatRateInfoFragment.class.getName());
                } else {
                    radioPreference.setFragment(null);
                }
            }
            repeatRateScreen.addPreference(radioPreference);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        RadioPreference radioPreference = (RadioPreference) preference;

        // Return if current radio button and previous radio button is same
        if (radioPreference.isChecked()) {
            return false;
        }

        // Clear all previously checked radio buttons
        PreferenceScreen keyRepeatRateScreen = getPreferenceManager().getPreferenceScreen();
        radioPreference.clearOtherRadioPreferences(keyRepeatRateScreen);

        // Setting new repeat rate
        setRepeatRate(radioPreference);

        // Setting information fragment
        final int[] entryValues =
                getContext().getResources().getIntArray(R.array.a11y_repeat_rate_values);
        if (isTwoPanel) {
            if (mCurrentRepeatRate == entryValues[0]) {
                // Setting information fragment only for default value
                radioPreference.setFragment(AccessibilityKeyRepeatRateInfoFragment.class.getName());
            } else {
                radioPreference.setFragment(null);
            }
        }
        /*TODO -- Log Accessibility Delay before repeat Status change.
         *Refer A11y Notification Timeout feature. cl/614754347 */
        return true;
    }

    /** Setting Initial repeat rate. */
    private void initRepeatRateValue() {
        final ContentResolver resolver = getContext().getContentResolver();
        mCurrentRepeatRate =
                Settings.Secure.getInt(
                        resolver,
                        Settings.Secure.KEY_REPEAT_DELAY_MS,
                        getContext().getResources().getInteger(R.integer.key_repeat_rate));
    }

    /** Setting repeat rate time */
    private void setRepeatRate(RadioPreference radioPreference) {
        // Getting new repeat rate value
        mCurrentRepeatRate = Integer.parseInt(radioPreference.getKey());

        // Committing new value to content provider
        final ContentResolver resolver = getContext().getContentResolver();
        Settings.Secure.putInt(resolver, Settings.Secure.KEY_REPEAT_DELAY_MS, mCurrentRepeatRate);

        // Setting radio preference with updated value
        radioPreference.setChecked(true);

        // Update the summary of delay before repeat row
        viewModel.setKeyRepeatRateSummary(radioPreference.getTitle().toString());
    }
}
