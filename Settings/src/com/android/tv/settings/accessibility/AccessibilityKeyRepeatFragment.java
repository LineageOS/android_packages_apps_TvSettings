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

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.annotation.Keep;
import androidx.lifecycle.ViewModelProvider;
import android.content.ContentResolver;
import android.provider.Settings;
import android.view.View;
import androidx.preference.PreferenceCategory;
import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.settings.accessibility.viewmodel.KeyRepeatViewModel;

@Keep
public class AccessibilityKeyRepeatFragment extends SettingsPreferenceFragment {
    private KeyRepeatViewModel viewModel;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.keyrepeat_accessibility, null);
        viewModel = new ViewModelProvider(requireActivity()).get(KeyRepeatViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Preference delayBeforeRepeatPreference =
                findPreference("accessibility_delay_before_repeat");
        Preference repeatRatePreference = findPreference("accessibility_repeat_rate");

        // Get the initial repeat rate value
        int initialRepeatRate = getCurrentRepeatRate();
        // Convert to summary string for repeat rate
        String initialRepeatRateSummary = convertRepeatRateToSummary(initialRepeatRate);
        // Set initial summary on the preference
        repeatRatePreference.setSummary(initialRepeatRateSummary);
        // Update the ViewModel
        viewModel.setKeyRepeatRateSummary(initialRepeatRateSummary);


        // Get the initial delay before key repeat
        int initialDelayBeforeKeyRepeat = getCurrentDelayBeforeKeyRepeat();
        // Convert to summary string for delay before key repeat
        String initialDelayBeforeKeyRepeatSummary =
                convertDelayBeforeKeyRepeatToSummary(initialDelayBeforeKeyRepeat);
        // Set initial summary on the preference
        delayBeforeRepeatPreference.setSummary(initialDelayBeforeKeyRepeatSummary);
        // Update the ViewModel
        viewModel.setDelayBeforeKeyRepeatSummary(initialDelayBeforeKeyRepeatSummary);


        // Observe the ViewModel's summary for future updates
        viewModel
                .getKeyRepeatRateSummary()
                .observe(
                        getViewLifecycleOwner(),
                        newSummary -> {
                            repeatRatePreference.setSummary(newSummary);
                        });
        // Observe the ViewModel's summary for future updates
        viewModel
                .getDelayBeforeKeyRepeatSummary()
                .observe(
                        getViewLifecycleOwner(),
                        newSummary -> {
                            delayBeforeRepeatPreference.setSummary(newSummary);
                        });
    }

    /**
     * Get current repeat rate
     *
     * @return KEY_REPEAT_DELAY_MS
     */
    private int getCurrentRepeatRate() {
        final ContentResolver resolver = getContext().getContentResolver();
        return Settings.Secure.getInt(
                resolver,
                Settings.Secure.KEY_REPEAT_DELAY_MS,
                getContext().getResources().getInteger(R.integer.key_repeat_rate));
    }

    /**
     * Get current delay before key repeat
     *
     * @return KEY_REPEAT_DELAY_MS
     */
    private int getCurrentDelayBeforeKeyRepeat() {
        final ContentResolver resolver = getContext().getContentResolver();
        return Settings.Secure.getInt(
                resolver,
                Settings.Secure.KEY_REPEAT_TIMEOUT_MS,
                getContext().getResources().getInteger(R.integer.delay_before_key_repeat));
    }

    /**
     * Converting repeat rate to Summary text
     *
     * @param repeatRate The key repeat rate in milliseconds.
     * @return Summary text
     */
    private String convertRepeatRateToSummary(int repeatRate) {
        final String[] entries =
                getContext().getResources().getStringArray(R.array.a11y_repeat_rate_entries);
        final int[] entryValues =
                getContext().getResources().getIntArray(R.array.a11y_repeat_rate_values);
        for (int i = 0; i < entryValues.length; i++) {
            if (repeatRate == entryValues[i]) {
                return entries[i];
            }
        }

        // Handle case where repeatRate doesn't match any predefined values
        return getString(R.string.accessibility_key_repeat_info);
    }

    /**
     * Converting delay before repeat to Summary text
     *
     * @param delayBeforeRepeat The delay before repeat in milliseconds.
     * @return Summary text
     */
    private String convertDelayBeforeKeyRepeatToSummary(int delayBeforeRepeat) {
        final String[] entries =
                getContext().getResources().
                        getStringArray(R.array.a11y_delay_before_repeat_entries);
        final int[] entryValues =
                getContext().getResources().getIntArray(R.array.a11y_delay_before_repeat_values);
        for (int i = 0; i < entryValues.length; i++) {
            if (delayBeforeRepeat == entryValues[i]) {
                return entries[i];
            }
        }

        // Handle case where repeatRate doesn't match any predefined values
        return getString(R.string.accessibility_key_repeat_info);
    }
}
