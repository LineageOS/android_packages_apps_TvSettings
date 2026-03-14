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

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.twopanelsettings.KtAsyncTask;

import java.util.ArrayList;
import java.util.List;

/** A fragment that displays apps disabled by TvWatchdogService and allows re-enabling them. */
public class TvWatchdogDisabledAppsFragment extends SettingsPreferenceFragment {

    private static final String TAG = "TvWatchdogDisabledAppsFragment";
    private static final String KEY_PACKAGES_DISABLED_ON_RESOURCE_OVERUSE =
            "android.tv.KEY_PACKAGES_DISABLED_ON_RESOURCE_OVERUSE";
    private static final String PACKAGES_DISABLED_ON_RESOURCE_OVERUSE_SEPARATOR = ";";

    private PackageManager mPm;

    private static class AppEntry {
        String mPackageName;
        CharSequence mLabel;
        Drawable mIcon;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // The preference screen is built dynamically, so no XML inflation is needed here.
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getContext()));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context context = getContext();
        if (context != null) {
            mPm = context.getPackageManager();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDisabledApps();
    }

    private void refreshDisabledApps() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        // Fetch the disabled apps on a background thread to avoid blocking the UI.
        new LoadDisabledAppsTask(context).execute();
    }

    private void populateDisabledApps(List<AppEntry> disabledApps) {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll(); // Clear any existing preferences.

        if (disabledApps == null || disabledApps.isEmpty()) {
            // Show a message indicating no apps are currently disabled.
            Preference message = new Preference(getContext());
            message.setTitle(R.string.tv_watchdog_no_disabled_apps_title);
            message.setSummary(R.string.tv_watchdog_no_disabled_apps_summary);
            message.setSelectable(false);
            preferenceScreen.addPreference(message);
            return;
        }

        // Add a description at the top.
        Preference description = new Preference(getContext());
        description.setTitle(R.string.tv_watchdog_disabled_apps_description);
        description.setSelectable(false);
        preferenceScreen.addPreference(description);

        for (AppEntry entry : disabledApps) {
            SwitchPreference pref = new SwitchPreference(getContext());

            // Use the pre-loaded data
            pref.setTitle(entry.mLabel);
            pref.setIcon(entry.mIcon);
            pref.setKey(entry.mPackageName);
            pref.setSummary(R.string.tv_watchdog_disabled_app_summary);

            // Prevent the SwitchPreference from restoring stale state (e.g. "true")
            // from SharedPreferences if the user previously toggled this package.
            pref.setPersistent(false);

            // The app is currently disabled, so the switch should be off (unchecked).
            // User toggles it ON to enable the app.
            pref.setChecked(false);

            pref.setOnPreferenceChangeListener(
                    (preference, newValue) -> {
                        boolean enabled = (Boolean) newValue;
                        if (enabled && mPm != null) {
                            // Re-enable the app
                            mPm.setApplicationEnabledSetting(
                                    entry.mPackageName,
                                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                                    0);

                            // Remove this item from the list immediately
                            preferenceScreen.removePreference(preference);

                            // Trigger a refresh to clean up the Secure Setting in the background
                            refreshDisabledApps();
                            return true;
                        }
                        return false;
                    });

            preferenceScreen.addPreference(pref);
        }
    }

    /** AsyncTask to load the list of disabled apps from secure settings in the background. */
    private class LoadDisabledAppsTask extends KtAsyncTask<List<AppEntry>> {

        private final ContentResolver mContentResolver;

        LoadDisabledAppsTask(Context context) {
            super(TvWatchdogDisabledAppsFragment.this);
            mContentResolver = context.getContentResolver();
        }

        @Override
        public List<AppEntry> doInBackground() {
            String setting =
                    Settings.Secure.getStringForUser(
                            mContentResolver,
                            KEY_PACKAGES_DISABLED_ON_RESOURCE_OVERUSE,
                            ActivityManager.getCurrentUser());

            if (TextUtils.isEmpty(setting)) {
                return new ArrayList<>();
            }

            String[] packages = setting.split(PACKAGES_DISABLED_ON_RESOURCE_OVERUSE_SEPARATOR);
            List<String> validDisabledPackageNames = new ArrayList<>();
            List<AppEntry> resultEntries = new ArrayList<>();
            boolean needsUpdate = false;

            for (String rawPkg : packages) {
                if (isCancelled()) {
                    break;
                }

                if (TextUtils.isEmpty(rawPkg)) {
                    continue;
                }

                String pkg = rawPkg.trim();

                try {
                    ApplicationInfo appInfo = mPm.getApplicationInfo(pkg, 0);

                    // The app is disabled if the enabled setting is one of the disabled states.
                    // Check !enabled to catch all disabled states (DISABLED, DISABLED_USER,
                    // DISABLED_UNTIL_USED)
                    if (!appInfo.enabled) {
                        validDisabledPackageNames.add(pkg);

                        // Load Label and Icon HERE (Background Thread)
                        AppEntry entry = new AppEntry();
                        entry.mPackageName = pkg;
                        entry.mLabel = appInfo.loadLabel(mPm);
                        entry.mIcon = appInfo.loadIcon(mPm);
                        resultEntries.add(entry);
                    } else {
                        // App was re-enabled elsewhere, so our list is stale.
                        needsUpdate = true;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    // App was uninstalled, so our list is stale.
                    needsUpdate = true;
                }
            }

            // Self-healing logic: If any app was re-enabled or uninstalled, clean up our settings
            // string.
            if (needsUpdate && !isCancelled()) {
                String newSetting =
                        TextUtils.join(
                                PACKAGES_DISABLED_ON_RESOURCE_OVERUSE_SEPARATOR,
                                validDisabledPackageNames);
                Settings.Secure.putString(
                        mContentResolver, KEY_PACKAGES_DISABLED_ON_RESOURCE_OVERUSE, newSetting);
            }

            return resultEntries;
        }

        @Override
        public void onPostExecute(List<AppEntry> disabledApps) {
            populateDisabledApps(disabledApps);
        }
    }
}
