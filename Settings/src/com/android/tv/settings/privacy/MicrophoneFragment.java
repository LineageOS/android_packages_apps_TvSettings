/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.tv.settings.privacy;

import static com.android.tv.twopanelsettings.slices.SlicesConstants.EXTRA_PREFERENCE_INFO_SUMMARY;
import static com.android.tv.twopanelsettings.slices.SlicesConstants.EXTRA_PREFERENCE_INFO_TEXT;
import static com.android.tv.twopanelsettings.slices.SlicesConstants.EXTRA_PREFERENCE_INFO_TITLE_ICON;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.tv.settings.R;
import com.android.tv.twopanelsettings.TwoPanelSettingsFragment;
import com.android.tv.twopanelsettings.slices.InfoFragment;

/**
 * The microphone settings screen in TV settings.
 */
@Keep
public class MicrophoneFragment extends SensorFragment {
    private static final String MIC_REMOTE_CATEGORY_KEY = "mic_remote_category";
    private static final String MIC_REMOTE_TOGGLE_KEY = "mic_remote_toggle";
    private SwitchPreference mMicRemoteToggle;

    @Override
    public void addHardwareToggle(PreferenceScreen screen, Context themedContext) {
        PreferenceCategory category = new PreferenceCategory(themedContext);
        category.setKey(MIC_REMOTE_CATEGORY_KEY);
        category.setTitle(themedContext.getString(R.string.privacy_assistant_settings_title));
        category.setVisible(mSensorToggle.isChecked());
        screen.addPreference(category);

        mMicRemoteToggle = new SwitchPreference(themedContext);
        mMicRemoteToggle.setFragment(InfoFragment.class.getCanonicalName());
        mMicRemoteToggle.setKey(MIC_REMOTE_TOGGLE_KEY);
        mMicRemoteToggle.setTitle(themedContext.getString(R.string.mic_remote_toggle_title));
        Bundle b = mMicRemoteToggle.getExtras();
        b.putParcelable(EXTRA_PREFERENCE_INFO_TITLE_ICON,
                Icon.createWithResource(themedContext, R.drawable.ic_info_outline_base));
        updateInfoForMicRemoteToggle();
        category.addPreference(mMicRemoteToggle);
    }

    private void updateInfoForMicRemoteToggle() {
        if (mMicRemoteToggle == null) {
            return;
        }
        Context themedContext = getPreferenceManager().getContext();
        Bundle b = mMicRemoteToggle.getExtras();
        b.putCharSequence(EXTRA_PREFERENCE_INFO_TEXT,
                themedContext.getString(
                        mMicRemoteToggle.isChecked() ? R.string.mic_remote_toggle_on_info_title
                                : R.string.mic_remote_toggle_off_info_title));
        b.putCharSequence(EXTRA_PREFERENCE_INFO_SUMMARY,
                themedContext.getString(
                        mMicRemoteToggle.isChecked() ? R.string.mic_remote_toggle_on_info_content
                                : R.string.mic_remote_toggle_off_info_content));
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (SENSOR_TOGGLE_KEY.equals(preference.getKey())) {
            super.onPreferenceTreeClick(preference);
            Preference remoteCategory = findPreference(MIC_REMOTE_CATEGORY_KEY);
            if (remoteCategory != null) {
                remoteCategory.setVisible(((SwitchPreference) preference).isChecked());
            }
            return true;
        } else if (MIC_REMOTE_TOGGLE_KEY.equals(preference.getKey())) {
            updateInfoForMicRemoteToggle();
            if (getParentFragment() instanceof TwoPanelSettingsFragment) {
                ((TwoPanelSettingsFragment) getParentFragment()).refocusPreferenceForceRefresh(
                        mMicRemoteToggle, this);
            }
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }
}
