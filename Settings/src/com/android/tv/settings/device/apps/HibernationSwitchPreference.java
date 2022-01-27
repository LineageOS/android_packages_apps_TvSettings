/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.MODE_DEFAULT;
import static android.app.AppOpsManager.MODE_IGNORED;
import static android.app.AppOpsManager.OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED;
import static android.provider.DeviceConfig.NAMESPACE_APP_HIBERNATION;

import static com.android.tv.settings.library.util.LibUtils.PROPERTY_APP_HIBERNATION_ENABLED;
import static com.android.tv.settings.library.util.LibUtils.PROPERTY_HIBERNATION_TARGETS_PRE_S_APPS;
import static com.android.tv.settings.util.InstrumentationUtils.logToggleInteracted;

import android.app.AppOpsManager;
import android.app.tvsettings.TvSettingsEnums;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.DeviceConfig;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.SwitchPreference;

import com.android.settingslib.applications.ApplicationsState;
import com.android.tv.settings.R;

public class HibernationSwitchPreference extends SwitchPreference
        implements AppOpsManager.OnOpChangedListener {
    private static final String TAG = "HibernationSwitchPreference";
    private String mPackageName;
    private final AppOpsManager mAppOpsManager;
    private int mPackageUid;
    private boolean mIsPackageSet;
    private boolean mIsPackageExemptByDefault;
    private final Context mContext;

    public HibernationSwitchPreference(Context context, ApplicationsState.AppEntry entry) {
        super(context);
        mAppOpsManager = context.getSystemService(AppOpsManager.class);
        mContext = context;
        if (entry.info != null) {
            setPackageName(entry.info.packageName);
        }
        this.setOnPreferenceClickListener((preference) -> {
            setHibernationEnabledForApp(isChecked());
            logToggleInteracted(TvSettingsEnums.APPS_ALL_APPS_APP_ENTRY_HIBERNATION, isChecked());
            refresh();
            return true;
        });
        refresh();
    }

    public void onResume() {
        if (mIsPackageSet) {
            mAppOpsManager.startWatchingMode(
                    OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED, mPackageName, this);
        }
    }

    public void onPause() {
        mAppOpsManager.stopWatchingMode(this);
    }

    @Override
    public void onOpChanged(String op, String packageName) {
        if (OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED.equals(op)
                && TextUtils.equals(mPackageName, packageName)) {
            refresh();
        }
    }

    /**
     * Set the package. And also retrieve details from package manager. Some packages may be
     * exempted from hibernation by default. This method should only be called to initialize the
     * controller.
     *
     * @param packageName The name of the package whose hibernation state to be managed.
     */
    private void setPackageName(String packageName) {
        mPackageName = packageName;
        final PackageManager packageManager = mContext.getPackageManager();

        // Q- packages exempt by default, except R- on Auto since Auto-Revoke was skipped in Rf
        final int maxTargetSdkVersionForExemptApps =
                packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
                        ? android.os.Build.VERSION_CODES.R
                        : android.os.Build.VERSION_CODES.Q;
        try {
            mPackageUid = packageManager.getPackageUid(packageName, /* flags */ 0);
            mIsPackageExemptByDefault =
                    !hibernationTargetsPreSApps() && packageManager.getTargetSdkVersion(packageName)
                            <= maxTargetSdkVersionForExemptApps;
            mIsPackageSet = true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Package [" + mPackageName + "] is not found!");
            mIsPackageSet = false;
        }
    }

    public void refresh() {
        setTitle(R.string.unused_apps_switch);
        super.setChecked(!isPackageHibernationExemptByUser());
    }

    // Hibernation toggle should be visible only if hibernation is enabled.
    public boolean shouldBeVisible() {
        return isHibernationEnabled() && mIsPackageSet;
    }

    // Set hibernation mode for the app.
    public void setHibernationEnabledForApp(boolean enabled) {
        try {
            mAppOpsManager.setUidMode(OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED, mPackageUid,
                    enabled ? MODE_ALLOWED : MODE_IGNORED);
        } catch (RuntimeException e) {
            Log.w(TAG, "Couldn't set UID mode");
        }
    }

    private boolean isPackageHibernationExemptByUser() {
        if (!mIsPackageSet) return true;
        final int mode = mAppOpsManager.unsafeCheckOpNoThrow(
                OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED, mPackageUid, mPackageName);

        return mode == MODE_DEFAULT ? mIsPackageExemptByDefault : mode != MODE_ALLOWED;
    }

    private static boolean isHibernationEnabled() {
        return DeviceConfig.getBoolean(
                NAMESPACE_APP_HIBERNATION, PROPERTY_APP_HIBERNATION_ENABLED, true);
    }

    private static boolean hibernationTargetsPreSApps() {
        return DeviceConfig.getBoolean(
                NAMESPACE_APP_HIBERNATION, PROPERTY_HIBERNATION_TARGETS_PRE_S_APPS, false);
    }

    /**
     * Set entry and refresh pref.
     *
     * @param entry entry
     */
    public void setEntry(@NonNull ApplicationsState.AppEntry entry) {
        if (entry.info != null) {
            setPackageName(entry.info.packageName);
        }
        refresh();
    }
}
