/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.tv.settings.device.sound;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v17.preference.LeanbackPreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;
import android.text.TextUtils;

import com.android.tv.settings.R;

public class SoundFragment extends LeanbackPreferenceFragment {
    private static final String KEY_SOUND_EFFECTS = "sound_effects";
    private AudioManager mAudioManager;

    public static SoundFragment newInstance() {
        return new SoundFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final Context themedContext = getPreferenceManager().getContext();
        final PreferenceScreen screen =
                getPreferenceManager().createPreferenceScreen(themedContext);
        screen.setTitle(R.string.device_sound_effects);
        setPreferenceScreen(screen);

        final SwitchPreference soundPref = new SwitchPreference(themedContext);
        soundPref.setKey(KEY_SOUND_EFFECTS);
        soundPref.setTitle(R.string.device_sound_effects);
        soundPref.setChecked(getSoundEffectsEnabled());
        screen.addPreference(soundPref);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), KEY_SOUND_EFFECTS)) {
            final TwoStatePreference soundPref = (TwoStatePreference) preference;
            setSoundEffectsEnabled(soundPref.isChecked());
        }
        return super.onPreferenceTreeClick(preference);
    }

    private boolean getSoundEffectsEnabled() {
        return getSoundEffectsEnabled(getActivity().getContentResolver());
    }

    static boolean getSoundEffectsEnabled(ContentResolver contentResolver) {
        return Settings.System.getInt(contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 1)
                != 0;
    }

    private void setSoundEffectsEnabled(boolean enabled) {
        if (enabled) {
            mAudioManager.loadSoundEffects();
        } else {
            mAudioManager.unloadSoundEffects();
        }
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.SOUND_EFFECTS_ENABLED, enabled ? 1 : 0);
    }

}
