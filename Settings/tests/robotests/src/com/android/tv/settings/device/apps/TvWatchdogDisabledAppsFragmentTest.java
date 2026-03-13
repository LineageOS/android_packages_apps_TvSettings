/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.tv.settings.device.apps;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.tv.flags.Flags;
import android.os.Looper;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.tv.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
public class TvWatchdogDisabledAppsFragmentTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;

    @Mock private Drawable mMockDrawable;

    private static final String KEY_PACKAGES_DISABLED_ON_RESOURCE_OVERUSE =
            "android.tv.KEY_PACKAGES_DISABLED_ON_RESOURCE_OVERUSE";
    private static final String DISABLED_APP_PKG = "com.some.disabled.app";
    private static final String DISABLED_APP_PKG_2 = "com.another.disabled.app";
    private static final String APP_LABEL = "Disabled App";
    private static final String APP_LABEL_2 = "Another Disabled App";
    private static final int FAKE_ICON_RES_ID = 1234;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());

        // Configure the shadow package manager to simulate known packages.
        ShadowPackageManager shadowPackageManager = shadowOf(mContext.getPackageManager());

        // 1. Install the packages as ENABLED by default (simulating the Manifest state).
        // If we installed them as disabled, setting them to "DEFAULT" later would keep them
        // disabled.
        addPackage(shadowPackageManager, DISABLED_APP_PKG, APP_LABEL);
        addPackage(shadowPackageManager, DISABLED_APP_PKG_2, APP_LABEL_2);

        // 2. Explicitly disable them to simulate the Watchdog/User action.
        // This makes getApplicationInfo().enabled return false.
        mContext.getPackageManager()
                .setApplicationEnabledSetting(
                        DISABLED_APP_PKG, PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER, 0);
        mContext.getPackageManager()
                .setApplicationEnabledSetting(
                        DISABLED_APP_PKG_2,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
                        0);

        // Grant the permission required to write to DeviceConfig.
        shadowOf((android.app.Application) ApplicationProvider.getApplicationContext())
                .grantPermissions(android.Manifest.permission.WRITE_DEVICE_CONFIG);

        // Enable the feature flag for all tests in this fragment.
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_TV_WATCHDOG_EMMC_PROTECTION);
    }

    private void addPackage(
            ShadowPackageManager shadowPackageManager, String packageName, String appLabel) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;
        packageInfo.applicationInfo = new ApplicationInfo();
        packageInfo.applicationInfo.packageName = packageName;
        packageInfo.applicationInfo.nonLocalizedLabel = appLabel;
        packageInfo.applicationInfo.icon = FAKE_ICON_RES_ID;
        // Important: Set this to TRUE. This represents the "Manifest" state.
        // The App is intrinsically valid and enabled, but will be turned off via Settings.
        packageInfo.applicationInfo.enabled = true;
        shadowPackageManager.installPackage(packageInfo);
        shadowPackageManager.addDrawableResolution(packageName, FAKE_ICON_RES_ID, mMockDrawable);
    }

    // Helper to wait for background tasks in PAUSED mode
    @SuppressWarnings("ThreadSleep")
    private void waitForBackgroundTasksToFinish() {
        int retries = 5;
        while (retries-- > 0) {
            shadowOf(Looper.getMainLooper()).idle();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        shadowOf(Looper.getMainLooper()).idle();
    }

    private TvWatchdogDisabledAppsFragment createFragment(String disabledAppsSetting) {
        Settings.Secure.putStringForUser(
                mContext.getContentResolver(),
                KEY_PACKAGES_DISABLED_ON_RESOURCE_OVERUSE,
                disabledAppsSetting,
                ActivityManager.getCurrentUser());

        // Use an Activity to host the fragment, ensuring its view is properly created.
        FragmentActivity activity = Robolectric.setupActivity(FragmentActivity.class);
        TvWatchdogDisabledAppsFragment fragment = new TvWatchdogDisabledAppsFragment();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow();

        waitForBackgroundTasksToFinish();
        return fragment;
    }

    @Test
    public void testScreenShowsMessageWhenSettingIsNull() {
        TvWatchdogDisabledAppsFragment fragment = createFragment(null);

        PreferenceScreen preferenceScreen = fragment.getPreferenceScreen();
        assertThat(preferenceScreen.getPreferenceCount()).isEqualTo(1);

        Preference messagePref = preferenceScreen.getPreference(0);
        assertThat(messagePref.getTitle().toString())
                .isEqualTo(mContext.getString(R.string.tv_watchdog_no_disabled_apps_title));
        assertThat(messagePref.getSummary().toString())
                .isEqualTo(mContext.getString(R.string.tv_watchdog_no_disabled_apps_summary));
        assertThat(messagePref.isSelectable()).isFalse();
    }

    @Test
    public void testScreenShowsMessageWhenNoAppsAreDisabled() {
        TvWatchdogDisabledAppsFragment fragment = createFragment("");

        PreferenceScreen preferenceScreen = fragment.getPreferenceScreen();
        assertThat(preferenceScreen.getPreferenceCount()).isEqualTo(1);

        Preference messagePref = preferenceScreen.getPreference(0);
        assertThat(messagePref.getTitle().toString())
                .isEqualTo(mContext.getString(R.string.tv_watchdog_no_disabled_apps_title));
        assertThat(messagePref.getSummary().toString())
                .isEqualTo(mContext.getString(R.string.tv_watchdog_no_disabled_apps_summary));
        assertThat(messagePref.isSelectable()).isFalse();
    }

    @Test
    public void testScreenShowsDisabledAppsAsSwitches() {
        TvWatchdogDisabledAppsFragment fragment = createFragment(DISABLED_APP_PKG);

        PreferenceScreen preferenceScreen = fragment.getPreferenceScreen();
        assertThat(preferenceScreen.getPreferenceCount()).isEqualTo(2); // Description + 1 app

        Preference descriptionPref = preferenceScreen.getPreference(0);
        assertThat(descriptionPref.getTitle().toString())
                .isEqualTo(mContext.getString(R.string.tv_watchdog_disabled_apps_description));

        Preference appPref = preferenceScreen.getPreference(1);
        assertThat(appPref).isInstanceOf(SwitchPreference.class);
        assertThat(appPref.getKey()).isEqualTo(DISABLED_APP_PKG);
        assertThat(appPref.getTitle().toString()).isEqualTo(APP_LABEL);
        assertThat(appPref.getSummary().toString())
                .isEqualTo(mContext.getString(R.string.tv_watchdog_disabled_app_summary));
        assertThat(((SwitchPreference) appPref).isChecked()).isFalse();
        assertThat(appPref.isPersistent()).isFalse();
    }

    @Test
    public void testToggleReEnablesAppAndRemovesFromList() {
        String setting = DISABLED_APP_PKG + ";" + DISABLED_APP_PKG_2;
        TvWatchdogDisabledAppsFragment fragment = createFragment(setting);

        // Initial state should verify both apps are present (3 items)
        PreferenceScreen preferenceScreen = fragment.getPreferenceScreen();
        assertThat(preferenceScreen.getPreferenceCount()).isEqualTo(3);

        // Find the switch for the first app
        SwitchPreference appPref =
                (SwitchPreference) preferenceScreen.findPreference(DISABLED_APP_PKG);
        assertThat(appPref).isNotNull();

        // Pre-emptively set the app to enabled in the REAL PackageManager (FIX: Removed shadowOf).
        // We set it to DEFAULT because that is what the Fragment does.
        // Since we installed the package as "enabled=true" in setUp(), DEFAULT resolves to ENABLED.
        mContext.getPackageManager()
                .setApplicationEnabledSetting(
                        DISABLED_APP_PKG, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0);

        // Simulate user toggling the switch ON (true)
        appPref.getOnPreferenceChangeListener().onPreferenceChange(appPref, true);

        // 1. Verify PackageManager was called to enable the app (or state reflects it)
        assertThat(mContext.getPackageManager().getApplicationEnabledSetting(DISABLED_APP_PKG))
                .isEqualTo(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);

        // 2. Verify the preference was removed immediately
        assertThat(preferenceScreen.getPreferenceCount()).isEqualTo(2);
        assertThat((Preference) preferenceScreen.findPreference(DISABLED_APP_PKG)).isNull();

        // 3. Wait for the background refresh to complete
        waitForBackgroundTasksToFinish();

        // 4. Verify Secure Setting updated
        String updatedSetting =
                Settings.Secure.getStringForUser(
                        mContext.getContentResolver(),
                        KEY_PACKAGES_DISABLED_ON_RESOURCE_OVERUSE,
                        ActivityManager.getCurrentUser());
        assertThat(updatedSetting).isEqualTo(DISABLED_APP_PKG_2);
    }

    @Test
    public void testUninstalledAppIsRemoved() {
        // Simulate an app being uninstalled (removed from PM)
        shadowOf(mContext.getPackageManager()).removePackage(DISABLED_APP_PKG);

        // Create fragment with the setting still pointing to the "uninstalled" app
        TvWatchdogDisabledAppsFragment fragment = createFragment(DISABLED_APP_PKG);

        // The screen should filter out the uninstalled app.
        PreferenceScreen preferenceScreen = fragment.getPreferenceScreen();
        assertThat(preferenceScreen.getPreferenceCount())
                .isEqualTo(1); // Just the "no apps" message

        Preference messagePref = preferenceScreen.getPreference(0);
        assertThat(messagePref.getTitle().toString())
                .isEqualTo(mContext.getString(R.string.tv_watchdog_no_disabled_apps_title));

        // The underlying setting should also be cleaned up
        String updatedSetting =
                Settings.Secure.getStringForUser(
                        mContext.getContentResolver(),
                        KEY_PACKAGES_DISABLED_ON_RESOURCE_OVERUSE,
                        ActivityManager.getCurrentUser());
        assertThat(updatedSetting).isEmpty();
    }

    @Test
    public void testSettingsStringWithWhitespaceIsHandled() {
        // Create a setting string with a leading space after the semicolon.
        // This simulates the adb command "pkg1; pkg2" scenario.
        String settingWithSpace = DISABLED_APP_PKG + "; " + DISABLED_APP_PKG_2;

        TvWatchdogDisabledAppsFragment fragment = createFragment(settingWithSpace);

        PreferenceScreen preferenceScreen = fragment.getPreferenceScreen();

        // If the .trim() fix is working, both apps will be found (3 items: Header + App1 + App2).
        // If the fix is broken, the second app will fail lookup, and count will be 2.
        assertThat(preferenceScreen.getPreferenceCount()).isEqualTo(3);

        // Verify the second app (the one with the space) was correctly identified
        Preference appPref2 = preferenceScreen.findPreference(DISABLED_APP_PKG_2);
        assertThat(appPref2).isNotNull();
        assertThat(appPref2.getTitle().toString()).isEqualTo(APP_LABEL_2);
    }
}
