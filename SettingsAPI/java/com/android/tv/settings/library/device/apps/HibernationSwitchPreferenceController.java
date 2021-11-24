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

package com.android.tv.settings.library.device.apps;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.MODE_DEFAULT;
import static android.app.AppOpsManager.MODE_IGNORED;
import static android.app.AppOpsManager.OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED;
import static android.provider.DeviceConfig.NAMESPACE_APP_HIBERNATION;

import static com.android.tv.settings.library.PreferenceCompat.STATUS_OFF;
import static com.android.tv.settings.library.PreferenceCompat.STATUS_ON;
import static com.android.tv.settings.library.util.LibUtils.PROPERTY_APP_HIBERNATION_ENABLED;
import static com.android.tv.settings.library.util.LibUtils.PROPERTY_HIBERNATION_TARGETS_PRE_S_APPS;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.DeviceConfig;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.tv.settings.library.UIUpdateCallback;
import com.android.tv.settings.library.data.PreferenceCompatManager;
import com.android.tv.settings.library.util.AbstractPreferenceController;
import com.android.tv.settings.library.util.ResourcesUtil;

/** Preference controller to handle notifications preference. */
public class HibernationSwitchPreferenceController extends AbstractPreferenceController
        implements AppOpsManager.OnOpChangedListener {
    private static final String TAG = "HibernationSwitchPreference";
    private static final String KEY_HIBERNATION_SWITCH = "hibernationSwitch";
    private ApplicationsState.AppEntry mAppEntry;
    private String mPackageName;
    private final AppOpsManager mAppOpsManager;
    private int mPackageUid;
    private boolean mIsPackageSet;
    private boolean mIsPackageExemptByDefault;

    public HibernationSwitchPreferenceController(Context context,
            UIUpdateCallback callback, int stateIdentifier,
            ApplicationsState.AppEntry appEntry, PreferenceCompatManager preferenceCompatManager) {
        super(context, callback, stateIdentifier, preferenceCompatManager);
        mAppOpsManager = context.getSystemService(AppOpsManager.class);
        mAppEntry = appEntry;
        if (mAppEntry.info != null) {
            setPackageName(mAppEntry.info.packageName);
        }
    }

    @Override
    public boolean isAvailable() {
        return shouldBeVisible();
    }

    // Hibernation toggle should be visible only if hibernation is enabled.
    public boolean shouldBeVisible() {
        return isHibernationEnabled() && mIsPackageSet;
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
        mAppEntry = entry;
        updateAndNotify();
    }

    @Override
    protected void init() {
        mPreferenceCompat.setTitle(ResourcesUtil.getString(mContext,
                "unused_apps_switch"));
        update();
    }

    @Override
    public void update() {
        mPreferenceCompat.setVisible(shouldBeVisible());
        mPreferenceCompat.setChecked(!isPackageHibernationExemptByUser());
    }

    @Override
    public String[] getPreferenceKey() {
        return new String[]{KEY_HIBERNATION_SWITCH};
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
            update();
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(boolean status) {
        return setHibernationEnabledForApp(status);
    }

    // Set hibernation mode for the app.
    public boolean setHibernationEnabledForApp(boolean checked) {
        boolean result = true;
        byte status = checked ? STATUS_ON : STATUS_OFF;
        if (mPreferenceCompat.getChecked() != status) {
            try {
                mAppOpsManager.setUidMode(OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED, mPackageUid,
                        checked ? MODE_ALLOWED : MODE_IGNORED);
                mPreferenceCompat.setChecked(checked);
                mUIUpdateCallback.notifyUpdate(mStateIdentifier, mPreferenceCompat);
            } catch (RuntimeException ex) {
                Log.w(TAG, "Couldn't set UID mode");
                result = false;
            }
        }
        return result;
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
}
