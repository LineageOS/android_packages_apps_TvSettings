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

package com.android.tv.settings;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.v7.preference.Preference;

import com.android.settingslib.development.DevelopmentSettingsEnabler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class MainFragmentTest {

    @Spy
    private MainFragment mMainFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(RuntimeEnvironment.application).when(mMainFragment).getContext();
    }

    @Test
    public void testUpdateDeveloperOptions_developerDisabled() {
        DevelopmentSettingsEnabler
                .setDevelopmentSettingsEnabled(RuntimeEnvironment.application, false);
        final Preference developerPref = spy(Preference.class);
        doReturn(developerPref).when(mMainFragment).findPreference(MainFragment.KEY_DEVELOPER);
        mMainFragment.updateDeveloperOptions();
        verify(developerPref, atLeastOnce()).setVisible(false);
        verify(developerPref, never()).setVisible(true);
    }

    @Test
    public void testUpdateDeveloperOptions_developerEnabled() {
        DevelopmentSettingsEnabler
                .setDevelopmentSettingsEnabled(RuntimeEnvironment.application, true);
        final Preference developerPref = spy(Preference.class);
        doReturn(developerPref).when(mMainFragment).findPreference(MainFragment.KEY_DEVELOPER);
        mMainFragment.updateDeveloperOptions();
        verify(developerPref, atLeastOnce()).setVisible(true);
        verify(developerPref, never()).setVisible(false);
    }

    @Test
    public void testUpdateCastSettings() {
        final Preference castPref = spy(Preference.class);
        doReturn(castPref).when(mMainFragment).findPreference(MainFragment.KEY_CAST_SETTINGS);
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
        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(
                intent, resolveInfo);

        mMainFragment.updateCastSettings();

        verify(castPref, atLeastOnce()).setTitle("Test Name");
    }
}
