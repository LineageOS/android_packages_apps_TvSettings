/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.tv.settings.device;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.robolectric.shadow.api.Shadow.extract;

import android.os.UserManager;
import android.provider.Settings;
import androidx.preference.Preference;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.tv.settings.R;
import com.android.tv.settings.testutils.Utils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class})
public class DevicePrefFragmentTest {
    @Spy
    private DevicePrefFragment mDevicePrefFragment;

    private ShadowUserManager mUserManager;
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mUserManager = extract(RuntimeEnvironment.application.getSystemService(UserManager.class));
        mUserManager.setIsAdminUser(true);
        doReturn(RuntimeEnvironment.application).when(mDevicePrefFragment).getContext();
        mDevicePrefFragment.onAttach(RuntimeEnvironment.application);
    }

    @Test
    public void testUpdateDeveloperOptions_developerDisabled() {
        DevelopmentSettingsEnabler
                .setDevelopmentSettingsEnabled(RuntimeEnvironment.application, false);
        final Preference developerPref = mock(Preference.class);
        doReturn(developerPref).when(mDevicePrefFragment)
                .findPreference(DevicePrefFragment.KEY_DEVELOPER);
        mDevicePrefFragment.updateDeveloperOptions();
        verify(developerPref, atLeastOnce()).setVisible(false);
        verify(developerPref, never()).setVisible(true);
    }

    @Test
    public void testUpdateDeveloperOptions_notAdmin() {
        DevelopmentSettingsEnabler
                .setDevelopmentSettingsEnabled(RuntimeEnvironment.application, true);
        mUserManager.setIsAdminUser(false);

        final Preference developerPref = mock(Preference.class);
        doReturn(developerPref).when(mDevicePrefFragment)
                    .findPreference(DevicePrefFragment.KEY_DEVELOPER);
        mDevicePrefFragment.updateDeveloperOptions();
        verify(developerPref, atLeastOnce()).setVisible(false);
        verify(developerPref, never()).setVisible(true);
    }

    @Test
    public void testUpdateDeveloperOptions_developerEnabled() {
        DevelopmentSettingsEnabler
                .setDevelopmentSettingsEnabled(RuntimeEnvironment.application, true);
        final Preference developerPref = mock(Preference.class);
        doReturn(developerPref).when(mDevicePrefFragment)
                .findPreference(DevicePrefFragment.KEY_DEVELOPER);
        mDevicePrefFragment.updateDeveloperOptions();
        verify(developerPref, atLeastOnce()).setVisible(true);
        verify(developerPref, never()).setVisible(false);
    }



    @Test
    public void testUpdateAutofillSettings_selectedNone() {
        final Preference autofillPref = mock(Preference.class);
        doReturn(autofillPref).when(mDevicePrefFragment).findPreference(
                DevicePrefFragment.KEY_KEYBOARD);

        Utils.addAutofill("com.test.AutofillPackage", "com.test.AutofillPackage.MyService");

        Settings.Secure.putString(mDevicePrefFragment.getContext().getContentResolver(),
                Settings.Secure.AUTOFILL_SERVICE, null);

        mDevicePrefFragment.updateKeyboardAutofillSettings();

        verify(autofillPref, atLeastOnce()).setTitle(R.string.system_keyboard_autofill);
        verify(autofillPref, never()).setTitle(R.string.system_keyboard);

        verify(autofillPref, never()).setSummary("com.test.AutofillPackage.MyService");
        verify(autofillPref, atLeastOnce()).setSummary("");
    }
}
