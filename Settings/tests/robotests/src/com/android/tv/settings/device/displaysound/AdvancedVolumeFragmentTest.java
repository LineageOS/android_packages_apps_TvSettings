/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.android.tv.settings.device.displaysound.AdvancedVolumeFragment.KEY_SURROUND_PASSTHROUGH;
import static com.android.tv.settings.device.displaysound.AdvancedVolumeFragment.KEY_SURROUND_SOUND_FORMAT_PREFIX;
import static com.android.tv.settings.device.displaysound.AdvancedVolumeFragment.VAL_SURROUND_SOUND_AUTO;
import static com.android.tv.settings.device.displaysound.AdvancedVolumeFragment.VAL_SURROUND_SOUND_MANUAL;
import static com.android.tv.settings.device.displaysound.AdvancedVolumeFragment.VAL_SURROUND_SOUND_NEVER;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.android.tv.settings.R;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RunWith(RobolectricTestRunner.class)
public class AdvancedVolumeFragmentTest {

    @Test
    public void testOnPreferenceChange_autoSelected() {
        AdvancedVolumeFragment fragment = createDefaultAdvancedVolumeFragment();
        Preference preference = new Preference(fragment.getContext());
        preference.setKey(KEY_SURROUND_PASSTHROUGH);

        fragment.onPreferenceChange(preference, VAL_SURROUND_SOUND_AUTO);

        assertThat(getSettingsInt(fragment.getContext(), Settings.Global.ENCODED_SURROUND_OUTPUT))
                .isEqualTo(Settings.Global.ENCODED_SURROUND_OUTPUT_AUTO);
    }

    @Test
    public void testOnPreferenceChange_manualSelected() {
        AdvancedVolumeFragment fragment = createDefaultAdvancedVolumeFragment();
        Preference preference = new Preference(fragment.getContext());
        preference.setKey(KEY_SURROUND_PASSTHROUGH);

        fragment.onPreferenceChange(preference, VAL_SURROUND_SOUND_MANUAL);

        assertThat(getSettingsInt(fragment.getContext(), Settings.Global.ENCODED_SURROUND_OUTPUT))
                .isEqualTo(Settings.Global.ENCODED_SURROUND_OUTPUT_MANUAL);
    }

    @Test
    public void testOnPreferenceChange_neverSelected() {
        AdvancedVolumeFragment fragment = createDefaultAdvancedVolumeFragment();
        Preference preference = new Preference(fragment.getContext());
        preference.setKey(KEY_SURROUND_PASSTHROUGH);

        fragment.onPreferenceChange(preference, VAL_SURROUND_SOUND_NEVER);

        assertThat(getSettingsInt(fragment.getContext(), Settings.Global.ENCODED_SURROUND_OUTPUT))
                .isEqualTo(Settings.Global.ENCODED_SURROUND_OUTPUT_NEVER);
    }

    @Test
    public void testOnPreferenceTreeClick_appendsFormatInSettings() {
        Map<Integer, Boolean> formats = ImmutableMap.of(
                AudioFormat.ENCODING_DTS, true,
                AudioFormat.ENCODING_DOLBY_MAT, true,
                AudioFormat.ENCODING_DOLBY_TRUEHD, true);
        Map<Integer, Boolean> reportedFormats = ImmutableMap.of(
                AudioFormat.ENCODING_DTS, true,
                AudioFormat.ENCODING_DOLBY_TRUEHD, true);
        AdvancedVolumeFragment fragment =
                createAdvancedVolumeFragmentWithAudioManagerReturning(formats, reportedFormats);

        Preference preference = new Preference(fragment.getContext());
        preference.setKey(KEY_SURROUND_PASSTHROUGH);
        fragment.onPreferenceChange(preference, VAL_SURROUND_SOUND_MANUAL);

        setSettingsStr(
                fragment.getContext(),
                Settings.Global.ENCODED_SURROUND_OUTPUT_ENABLED_FORMATS,
                Joiner.on(",").join(
                        AudioFormat.ENCODING_DOLBY_TRUEHD, AudioFormat.ENCODING_DOLBY_MAT));
        SwitchPreference pref = new SwitchPreference(fragment.getContext());
        pref.setKey(KEY_SURROUND_SOUND_FORMAT_PREFIX + AudioFormat.ENCODING_DTS);
        pref.setChecked(true);
        fragment.onPreferenceTreeClick(pref);

        assertThat(
                getSettingsStr(
                        fragment.getContext(),
                        Settings.Global.ENCODED_SURROUND_OUTPUT_ENABLED_FORMATS))
                .isEqualTo(Joiner.on(",").join(
                        AudioFormat.ENCODING_DOLBY_MAT,
                        AudioFormat.ENCODING_DTS,
                        AudioFormat.ENCODING_DOLBY_TRUEHD));
    }

    @Test
    public void testOnPreferenceTreeClick_removesFormatFromSettings() {
        Map<Integer, Boolean> formats = ImmutableMap.of(
                AudioFormat.ENCODING_DTS, true,
                AudioFormat.ENCODING_DOLBY_MAT, true,
                AudioFormat.ENCODING_DOLBY_TRUEHD, true);
        Map<Integer, Boolean> reportedFormats = ImmutableMap.of(
                AudioFormat.ENCODING_DTS, true,
                AudioFormat.ENCODING_DOLBY_TRUEHD, true);
        AdvancedVolumeFragment fragment =
                createAdvancedVolumeFragmentWithAudioManagerReturning(formats, reportedFormats);

        Preference preference = new Preference(fragment.getContext());
        preference.setKey(KEY_SURROUND_PASSTHROUGH);
        fragment.onPreferenceChange(preference, VAL_SURROUND_SOUND_MANUAL);

        setSettingsStr(
                fragment.getContext(),
                Settings.Global.ENCODED_SURROUND_OUTPUT_ENABLED_FORMATS,
                Joiner.on(",").join(
                        AudioFormat.ENCODING_DOLBY_MAT,
                        AudioFormat.ENCODING_DTS,
                        AudioFormat.ENCODING_DOLBY_TRUEHD));
        SwitchPreference pref = new SwitchPreference(fragment.getContext());
        pref.setKey(KEY_SURROUND_SOUND_FORMAT_PREFIX + AudioFormat.ENCODING_DTS);
        pref.setChecked(false);
        fragment.onPreferenceTreeClick(pref);

        assertThat(
                getSettingsStr(
                        fragment.getContext(),
                        Settings.Global.ENCODED_SURROUND_OUTPUT_ENABLED_FORMATS))
                .isEqualTo(Joiner.on(",").join(
                        AudioFormat.ENCODING_DOLBY_MAT, AudioFormat.ENCODING_DOLBY_TRUEHD));
    }

    @Test
    public void testGetPreferenceScreen_onManualSelected_returnsFormatsInCorrectPreferenceGroup() {
        Map<Integer, Boolean> formats = ImmutableMap.of(
                AudioFormat.ENCODING_DTS, true,
                AudioFormat.ENCODING_DOLBY_MAT, true);
        Map<Integer, Boolean> reportedFormats = ImmutableMap.of(
                AudioFormat.ENCODING_DTS, true);
        AdvancedVolumeFragment fragment =
                createAdvancedVolumeFragmentWithAudioManagerReturning(formats, reportedFormats);

        Preference preference = new Preference(fragment.getContext());
        preference.setKey(KEY_SURROUND_PASSTHROUGH);
        fragment.onPreferenceChange(preference, VAL_SURROUND_SOUND_MANUAL);

        assertThat(fragment.getPreferenceScreen().getPreferenceCount()).isEqualTo(3);

        Preference supportedFormatPreference =
                fragment.getPreferenceScreen().getPreference(1);
        assertThat(supportedFormatPreference.getTitle()).isEqualTo(
                fragment.getContext().getString(R.string.surround_sound_supported_title));
        assertThat(getChildrenTitles(supportedFormatPreference)).containsExactly(
                fragment.getContext().getString(R.string.surround_sound_format_dts));

        Preference unsupportedFormatPreference =
                fragment.getPreferenceScreen().getPreference(2);
        assertThat(unsupportedFormatPreference.getTitle()).isEqualTo(
                fragment.getContext().getString(R.string.surround_sound_unsupported_title));
        assertThat(getChildrenTitles(unsupportedFormatPreference)).containsExactly(
                fragment.getContext().getString(R.string.surround_sound_format_dolby_mat));
    }

    private List<String> getChildrenTitles(Preference preference) {
        PreferenceCategory category = (PreferenceCategory) preference;

        return IntStream.range(0, category.getPreferenceCount())
                .mapToObj(i -> category.getPreference(i).getTitle().toString())
                .collect(Collectors.toList());
    }

    private String getSettingsStr(Context context, String key) {
        return Settings.Global.getString(context.getContentResolver(), key);
    }

    private void setSettingsStr(Context context, String key, String val) {
        Settings.Global.putString(context.getContentResolver(), key, val);
    }

    private int getSettingsInt(Context context, String key) {
        try {
            return Settings.Global.getInt(
                    context.getContentResolver(), key);
        } catch (Settings.SettingNotFoundException e) {
            throw new IllegalStateException("Unable to locate [" + key + "] setting.");
        }
    }

    private AdvancedVolumeFragment createDefaultAdvancedVolumeFragment() {
        return createAdvancedVolumeFragmentWithAudioManagerReturning(
                Collections.EMPTY_MAP, Collections.EMPTY_MAP);
    }

    private AdvancedVolumeFragment createAdvancedVolumeFragmentWithAudioManagerReturning(
            Map<Integer, Boolean> formats, Map<Integer, Boolean> reportedFormats) {
        AudioManager audioManager = spy(AudioManager.class);
        doReturn(formats).when(audioManager).getSurroundFormats();
        doReturn(reportedFormats).when(audioManager).getReportedSurroundFormats();

        AdvancedVolumeFragment fragment = spy(AdvancedVolumeFragment.class);
        doReturn(audioManager).when(fragment).getAudioManager();

        return FragmentController.of(fragment)
                .create()
                .start()
                .get();
    }
}
