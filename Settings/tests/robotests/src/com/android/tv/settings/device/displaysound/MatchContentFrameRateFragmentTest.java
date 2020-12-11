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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;

import android.provider.Settings;

import androidx.preference.PreferenceGroup;

import com.android.tv.settings.RadioPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class MatchContentFrameRateFragmentTest {
    @Spy
    private MatchContentFrameRateFragment mMatchContentFrameRateFragment;

    @Mock
    private PreferenceGroup mPreferenceGroup;

    private RadioPreference mAutoPreference;

    private RadioPreference mNeverPreference;

    private RadioPreference mAlwaysPreference;

    private static final String KEY_MATCH_CONTENT_FRAME_RATE_AUTO = "match_content_frame_rate_auto";
    private static final String KEY_MATCH_CONTENT_FRAME_RATE_ALWAYS =
            "match_content_frame_rate_always";
    private static final String KEY_MATCH_CONTENT_FRAME_RATE_NEVER =
            "match_content_frame_rate_never";

    private static final int BAD_MATCH_CONTENT_FRAME_RATE_VALUE = -1;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(RuntimeEnvironment.application).when(mMatchContentFrameRateFragment).getContext();
        mMatchContentFrameRateFragment.onAttach(RuntimeEnvironment.application);

        doReturn(mPreferenceGroup).when(mMatchContentFrameRateFragment).getPreferenceGroup();

        mAutoPreference = new RadioPreference(mMatchContentFrameRateFragment.getContext());
        mAutoPreference.setKey(KEY_MATCH_CONTENT_FRAME_RATE_AUTO);
        mPreferenceGroup.addPreference(mAutoPreference);
        mNeverPreference = new RadioPreference(mMatchContentFrameRateFragment.getContext());
        mNeverPreference.setKey(KEY_MATCH_CONTENT_FRAME_RATE_NEVER);
        mPreferenceGroup.addPreference(mNeverPreference);
        mAlwaysPreference = new RadioPreference(mMatchContentFrameRateFragment.getContext());
        mAlwaysPreference.setKey(KEY_MATCH_CONTENT_FRAME_RATE_ALWAYS);
        mPreferenceGroup.addPreference(mAlwaysPreference);
    }

    @Test
    public void testOnPreferenceTreeClick_autoSelected_otherRadioButtonsDisabled() {
        mMatchContentFrameRateFragment.onPreferenceTreeClick(mAutoPreference);
        assertThat(mNeverPreference.isChecked()).isFalse();
        assertThat(mAlwaysPreference.isChecked()).isFalse();
    }

    @Test
    public void testOnPreferenceTreeClick_noneSelected_otherRadioButtonsDisabled() {
        mMatchContentFrameRateFragment.onPreferenceTreeClick(mNeverPreference);
        assertThat(mAutoPreference.isChecked()).isFalse();
        assertThat(mAlwaysPreference.isChecked()).isFalse();
    }

    @Test
    public void testOnPreferenceTreeClick_alwaysSelected_otherRadioButtonsDisabled() {
        mMatchContentFrameRateFragment.onPreferenceTreeClick(mAlwaysPreference);
        assertThat(mAutoPreference.isChecked()).isFalse();
        assertThat(mNeverPreference.isChecked()).isFalse();
    }

    @Test
    public void testOnPreferenceTreeClick_autoSelected_settingModified() {
        mMatchContentFrameRateFragment.onPreferenceTreeClick(mAutoPreference);
        assertThat(Settings.Secure.getInt(
                mMatchContentFrameRateFragment.getContext().getContentResolver(),
                Settings.Secure.MATCH_CONTENT_FRAME_RATE,
                BAD_MATCH_CONTENT_FRAME_RATE_VALUE)).isEqualTo(
                        Settings.Secure.MATCH_CONTENT_FRAMERATE_SEAMLESSS_ONLY);
    }

    @Test
    public void testOnPreferenceTreeClick_neverSelected_settingModified() {
        mMatchContentFrameRateFragment.onPreferenceTreeClick(mNeverPreference);
        assertThat(Settings.Secure.getInt(
                mMatchContentFrameRateFragment.getContext().getContentResolver(),
                Settings.Secure.MATCH_CONTENT_FRAME_RATE,
                BAD_MATCH_CONTENT_FRAME_RATE_VALUE)).isEqualTo(
                        Settings.Secure.MATCH_CONTENT_FRAMERATE_NEVER);
    }

    @Test
    public void testOnPreferenceTreeClick_alwaysSelected_settingModified() {
        mMatchContentFrameRateFragment.onPreferenceTreeClick(mAlwaysPreference);
        assertThat(Settings.Secure.getInt(
                mMatchContentFrameRateFragment.getContext().getContentResolver(),
                Settings.Secure.MATCH_CONTENT_FRAME_RATE,
                BAD_MATCH_CONTENT_FRAME_RATE_VALUE)).isEqualTo(
                    Settings.Secure.MATCH_CONTENT_FRAMERATE_ALWAYS);
    }
}
