/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.tv.settings.device.displaysound;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioDeviceVolumeManager;
import android.media.AudioManager;
import android.media.VolumeInfo;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.tv.settings.R;
import com.android.tv.settings.RadioPreference;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.settings.device.util.DeviceUtils;

/**
 * The "Device volume" screen in TV Settings.
 */
@Keep
public class VolumeChangePreferenceFragment extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private SharedPreferences mSharedPreferences;

    private static final String VOLUME_CHANGE_RADIO_GROUP = "volume_change_radio_group";
    private static final String VOLUME_CHANGE_GROUP = "volume_change_group";
    private static final String VOLUME_KEY = "volume_preference";
    private AudioDeviceVolumeManager mADVmgr;

    private static final AudioDeviceAttributes SPEAKER = new AudioDeviceAttributes(
            AudioDeviceAttributes.ROLE_OUTPUT, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, "");

    /** Value of volume index */
    private float mCurrentVolumeIndex;
    private String mCurrentDeviceName;
    private String mCurrentDeviceDefaultVolume;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        setPreferencesFromResource(R.xml.change_device_volume, null);
        PreferenceGroup volumeChangeGroup = (PreferenceGroup) findPreference(VOLUME_CHANGE_GROUP);
        mSharedPreferences = getPreferenceManager().getSharedPreferences();

        mCurrentDeviceName = DeviceUtils.getDeviceName(getContext());
        String volumeChangeSettingsTitle = String.format(
                getContext().getResources().getString(R.string.volume_change_settings_title),
                mCurrentDeviceName
        );
        getPreferenceScreen().setTitle(volumeChangeSettingsTitle);

        mCurrentDeviceDefaultVolume = DeviceUtils.getDeviceDefaultVolume(getContext());
        mCurrentVolumeIndex = mSharedPreferences.getFloat(VOLUME_KEY,
                Float.parseFloat(mCurrentDeviceDefaultVolume));

        String volumeChangeSubtitleUnselect = String.format(
                getString(R.string.volume_change_subtitle_unselect),
                mCurrentDeviceName
        );
        volumeChangeGroup.setTitle(volumeChangeSubtitleUnselect);

        final Context themedContext = getPreferenceManager().getContext();
        final int[] configEntryValues = getContext().getResources()
                .getIntArray(R.array.config_volume_change_entry_values);
        final String[] entryValues = new String[configEntryValues.length];
        for (int i = 0; i < configEntryValues.length; i++) {
            entryValues[i] = String.valueOf(configEntryValues[i] / 100.0f);
        }
        final String[] entries = getContext().getResources()
                .getStringArray(R.array.volume_change_entries);

        mADVmgr = (AudioDeviceVolumeManager) getContext().getSystemService(
                Context.AUDIO_DEVICE_VOLUME_SERVICE);

        for (int i = 0; i < entryValues.length; i++) {
            final RadioPreference preference = new RadioPreference(themedContext);
            preference.setPersistent(true);
            preference.setRadioGroup(VOLUME_CHANGE_RADIO_GROUP);
            preference.setOnPreferenceChangeListener(this);
            preference.setKey(entryValues[i]);
            preference.setTitle(entries[i]);
            if (Float.compare(mCurrentVolumeIndex , Float.parseFloat(entryValues[i])) == 0) {
                preference.setChecked(true);
                commit();
            }
            volumeChangeGroup.addPreference(preference);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        RadioPreference radioPreference = (RadioPreference) preference;
        if (radioPreference.isChecked()) {
            return false;
        }

        String volumeChangeSubtitleSelect = String.format(
                getString(R.string.volume_change_subtitle_select),
                mCurrentDeviceName
        );

        PreferenceGroup volumeChangeGroup = (PreferenceGroup) findPreference(VOLUME_CHANGE_GROUP);
        radioPreference.setChecked(true);
        radioPreference.clearOtherRadioPreferences(volumeChangeGroup);
        volumeChangeGroup.setTitle(volumeChangeSubtitleSelect);
        mCurrentVolumeIndex = Float.parseFloat(preference.getKey());
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putFloat(VOLUME_KEY, mCurrentVolumeIndex);
        editor.apply();
        commit();
        return true;
    }

    protected void commit() {
        if (getContext() == null) return;
        AudioManager am = getContext().getSystemService(AudioManager.class);
        final int minIndex = am.getStreamMinVolume(AudioManager.STREAM_MUSIC);
        final int maxIndex = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        final VolumeInfo volMedia = new VolumeInfo.Builder(AudioManager.STREAM_MUSIC)
                .setMinVolumeIndex(minIndex)
                .setMaxVolumeIndex(maxIndex)
                .build();
        final int volumeIndex = (int) (maxIndex * mCurrentVolumeIndex);
        final VolumeInfo currVol = new VolumeInfo.Builder(volMedia)
                .setVolumeIndex(volumeIndex).build();

        //safe media can block raise volume, disable it
        am.disableSafeMediaVolume();

        mADVmgr.setDeviceVolume(currVol, SPEAKER);
    }
}
