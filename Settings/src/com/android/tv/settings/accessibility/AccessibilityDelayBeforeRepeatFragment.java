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

import static com.android.tv.settings.util.InstrumentationUtils.logEntrySelected;

import android.app.tvsettings.TvSettingsEnums;
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
public class AccessibilityDelayBeforeRepeatFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private int mCurrentDelayBeforeRepeat;
    private boolean isTwoPanel;
    private KeyRepeatViewModel viewModel;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.accessibility_delay_before_repeat, null);
        viewModel = new ViewModelProvider(requireActivity()).get(KeyRepeatViewModel.class);

        PreferenceScreen delayBeforeRepeatScreen = getPreferenceManager().getPreferenceScreen();
        final Context themedContext = getPreferenceManager().getContext();
        isTwoPanel = FlavorUtils.isTwoPanel(getContext());

        final String[] entries =
                getContext().getResources().
                        getStringArray(R.array.a11y_delay_before_repeat_entries);
        final int[] entryValues =
                getContext().getResources().getIntArray(R.array.a11y_delay_before_repeat_values);

        // Setting initial delay before repeat time out value
        initDelayBeforeRepeatTimeoutValue();

        for (int i = 0; i < entries.length; i++) {
            final RadioPreference radioPreference = new RadioPreference(themedContext);
            radioPreference.setTitle(entries[i]);
            radioPreference.setKey(String.valueOf(entryValues[i]));
            radioPreference.setOnPreferenceChangeListener(this);
            radioPreference.setPersistent(false);
            if (mCurrentDelayBeforeRepeat == entryValues[i]) {
                radioPreference.setChecked(true);
            }
            if (isTwoPanel) {
                if (i == 0) {
                    // Setting information fragment only for default value
                    radioPreference.setFragment(
                            AccessibilityDelayBeforeKeyRepeatInfoFragment.class.getName());
                } else {
                    radioPreference.setFragment(null);
                }
            }
            delayBeforeRepeatScreen.addPreference(radioPreference);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        RadioPreference radioPreference = (RadioPreference) preference;

        // Check if current checked item and previous checked item is same
        if (radioPreference.isChecked()) {
            return false;
        }

        // Clear previous checked items
        PreferenceScreen delayBeforeKeyRepeatScreen = getPreferenceManager().getPreferenceScreen();
        radioPreference.clearOtherRadioPreferences(delayBeforeKeyRepeatScreen);

        // Setting Delay Before Repeat Time
        setDelayBeforeRepeat(radioPreference);

        // Setting information fragment
        final int[] entryValues =
                getContext().getResources().getIntArray(R.array.a11y_delay_before_repeat_values);
        if (isTwoPanel) {
            if (mCurrentDelayBeforeRepeat == entryValues[0]) {
                // Setting information fragment only for default value
                radioPreference.setFragment(
                        AccessibilityDelayBeforeKeyRepeatInfoFragment.class.getName());
            } else {
                radioPreference.setFragment(null);
            }
        }
        // Log delay before repeat selection
        logNewDelayBeforeRepeatTimeoutSelection(mCurrentDelayBeforeRepeat);
        return true;
    }

    @Override
    protected int getPageId() {
        return TvSettingsEnums.SYSTEM_A11Y_KEY_REPEAT_DELAY;
    }

    private void logNewDelayBeforeRepeatTimeoutSelection(int entryValue) {
        final int[] a11yDelayBeforeRepeatOptions = {
                TvSettingsEnums.SYSTEM_A11Y_KEY_REPEAT_DELAY_DEFAULT,
                TvSettingsEnums.SYSTEM_A11Y_KEY_REPEAT_DELAY_THREE_SECONDS,
                TvSettingsEnums.SYSTEM_A11Y_KEY_REPEAT_DELAY_FIVE_SECONDS,
        };
        final int[] entryValues =
                getContext().getResources().getIntArray(R.array.a11y_delay_before_repeat_values);
        for (int i = 0; i < entryValues.length; i++) {
            if (entryValue == entryValues[i]) {
                logEntrySelected(a11yDelayBeforeRepeatOptions[i]);
                return;
            }
        }
    }

    /** Setting Initial Delay Before Repeat time. */
    private void initDelayBeforeRepeatTimeoutValue() {
        final ContentResolver resolver = getContext().getContentResolver();
        mCurrentDelayBeforeRepeat =
                Settings.Secure.getInt(
                        resolver,
                        Settings.Secure.KEY_REPEAT_TIMEOUT_MS,
                        getContext().getResources().getInteger(R.integer.delay_before_key_repeat));
    }

    /** Setting delay before repeat time */
    private void setDelayBeforeRepeat(RadioPreference radioPreference) {
        // Getting new delay value
        mCurrentDelayBeforeRepeat = Integer.parseInt(radioPreference.getKey());

        // Committing new value to content provider
        final ContentResolver resolver = getContext().getContentResolver();
        Settings.Secure.putInt(
                resolver, Settings.Secure.KEY_REPEAT_TIMEOUT_MS, mCurrentDelayBeforeRepeat);

        // Setting radio preference with updated value
        radioPreference.setChecked(true);

        // Update the summary of delay before repeat row
        viewModel.setDelayBeforeKeyRepeatSummary(radioPreference.getTitle().toString());
    }
}
