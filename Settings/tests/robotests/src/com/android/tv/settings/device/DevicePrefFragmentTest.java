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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.shadow.api.Shadow.extract;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserManager;
import android.support.v7.preference.Preference;

import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.tv.settings.TvSettingsRobolectricTestRunner;
import com.android.tv.settings.testutils.ShadowUserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(TvSettingsRobolectricTestRunner.class)
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
    }

    @Test
    public void testUpdateDeveloperOptions_developerDisabled() {
        DevelopmentSettingsEnabler
                .setDevelopmentSettingsEnabled(RuntimeEnvironment.application, false);
        final Preference developerPref = mock(Preference.class);
        doReturn(developerPref).when(mDevicePrefFragment)
                .findPreference(mDevicePrefFragment.KEY_DEVELOPER);
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
                .findPreference(mDevicePrefFragment.KEY_DEVELOPER);
        mDevicePrefFragment.updateDeveloperOptions();
        verify(developerPref, atLeastOnce()).setVisible(true);
        verify(developerPref, never()).setVisible(false);
    }

    @Test
    public void testUpdateCastSettings() {
        final Preference castPref = mock(Preference.class);
        doReturn(castPref).when(mDevicePrefFragment)
                    .findPreference(DevicePrefFragment.KEY_CAST_SETTINGS);
        final Intent intent = new Intent("com.google.android.settings.CAST_RECEIVER_SETTINGS");
        doReturn(intent).when(castPref).getIntent();

        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.resolvePackageName = "com.test.CastPackage";
        final ActivityInfo activityInfo = mock(ActivityInfo.class);
        doReturn("Test Name").when(activityInfo).loadLabel(any(PackageManager.class));
        resolveInfo.activityInfo = activityInfo;
        final ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;
        activityInfo.applicationInfo = applicationInfo;
        final ShadowPackageManager shadowPackageManager = shadowOf(
                RuntimeEnvironment.application.getPackageManager());
        final PackageInfo castPackageInfo = new PackageInfo();
        castPackageInfo.packageName = "com.test.CastPackage";
        shadowPackageManager.addPackage(castPackageInfo);
        shadowPackageManager.addResolveInfoForIntent(intent, resolveInfo);

        mDevicePrefFragment.updateCastSettings();

        verify(castPref, atLeastOnce()).setTitle("Test Name");
    }
}
