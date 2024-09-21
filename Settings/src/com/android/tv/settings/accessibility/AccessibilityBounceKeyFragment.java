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
import android.hardware.input.InputSettings;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.tv.settings.R;
import com.android.tv.settings.RadioPreference;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.settings.overlay.FlavorUtils;
import androidx.lifecycle.ViewModelProvider;
import com.android.tv.settings.accessibility.viewmodel.BounceKeyViewModel;

@Keep
public class AccessibilityBounceKeyFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private int mCurrentBounceKeyTimeout;
    private BounceKeyViewModel viewModel;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.bounce_key_timeout, null);
        viewModel = new ViewModelProvider(requireActivity()).get(BounceKeyViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PreferenceScreen bounceKeyTimeoutScreen = getPreferenceManager().getPreferenceScreen();
        final Context themedContext = getPreferenceManager().getContext();
        final String[] entries =
                getContext().getResources().getStringArray(R.array.bounce_key_timeout_entries);
        final int[] entryValues =
                getContext().getResources().getIntArray(R.array.bounce_key_timeout_values);

        // Setting initial repeat rate
        initBounceKeyTimeoutValue();

        boolean isTwoPanel = FlavorUtils.isTwoPanel(getContext());
        for (int i = 0; i < entries.length; i++) {
            final RadioPreference radioPreference = new RadioPreference(themedContext);
            radioPreference.setTitle(entries[i]);
            radioPreference.setKey(String.valueOf(entryValues[i]));
            radioPreference.setOnPreferenceChangeListener(this);
            radioPreference.setPersistent(false);
            if (mCurrentBounceKeyTimeout == entryValues[i]) {
                radioPreference.setChecked(true);
            }
            if (isTwoPanel) {
                // Setting information fragment only for default value
                radioPreference.setFragment(AccessibilityBounceKeyDetailedFragment.class.getName());
            }
            bounceKeyTimeoutScreen.addPreference(radioPreference);
        }

        // Setting initial bounce key time
        viewModel.setBounceKeyValue(mCurrentBounceKeyTimeout);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        RadioPreference radioPreference = (RadioPreference) preference;

        // Return if current radio button and previous radio button is same
        if (radioPreference.isChecked()) {
            return false;
        }

        // Clear all previously checked radio buttons
        PreferenceScreen bounceKeyScreen = getPreferenceManager().getPreferenceScreen();
        radioPreference.clearOtherRadioPreferences(bounceKeyScreen);

        // Setting new repeat rate
        mCurrentBounceKeyTimeout = Integer.parseInt(radioPreference.getKey());
        settingBounceKeyValue(mCurrentBounceKeyTimeout);

        // Setting selected bounce key time out
        radioPreference.setChecked(true);

        // Setting current bounce key time
        viewModel.setBounceKeyValue(mCurrentBounceKeyTimeout);

        /*TODO -- Log Bounce Key time out Status change.
         *Refer A11y Notification Timeout feature. cl/614754347 */
        return true;
    }

    /** Setting Initial bounce key time out value. */
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
    private void settingBounceKeyValue(int bounceKeyTimeOut) {
        final ContentResolver resolver = getContext().getContentResolver();
        Settings.Secure.putInt(resolver,
                Settings.Secure.ACCESSIBILITY_BOUNCE_KEYS, bounceKeyTimeOut);
    }
}
