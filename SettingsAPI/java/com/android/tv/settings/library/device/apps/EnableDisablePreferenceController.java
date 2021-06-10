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


import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import com.android.tv.settings.library.PreferenceCompat;
import com.android.tv.settings.library.UIUpdateCallback;
import com.android.tv.settings.library.data.PreferenceCompatManager;
import com.android.tv.settings.library.util.LibUtils;
import com.android.tv.settings.library.util.ResourcesUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Preference controller to enable disable preference. */
public class EnableDisablePreferenceController extends AppActionPreferenceController {
    private final PackageManager mPackageManager;
    private PreferenceCompat mEnableDisablePreference;
    static final String KEY_ENABLE_DISABLE = "enableDisable";

    public EnableDisablePreferenceController(Context context,
            UIUpdateCallback callback, int stateIdentifier,
            ApplicationsState.AppEntry appEntry) {
        super(context, callback, stateIdentifier, appEntry);
        mPackageManager = context.getPackageManager();
    }

    @Override
    public void displayPreference(PreferenceCompatManager screen) {
        mEnableDisablePreference = screen.getOrCreatePrefCompat(getPreferenceKey());
        super.displayPreference(screen);
    }

    @Override
    public String[] getPreferenceKey() {
        return new String[]{KEY_ENABLE_DISABLE};
    }

    private HashSet<String> getHomePackages() {
        HashSet<String> homePackages = new HashSet<>();
        // Get list of "home" apps and trace through any meta-data references
        List<ResolveInfo> homeActivities = new ArrayList<>();
        mPackageManager.getHomeActivities(homeActivities);
        for (ResolveInfo ri : homeActivities) {
            final String activityPkg = ri.activityInfo.packageName;
            homePackages.add(activityPkg);
            // Also make sure to include anything proxying for the home app
            final Bundle metadata = ri.activityInfo.metaData;
            if (metadata != null) {
                final String metaPkg = metadata.getString(ActivityManager.META_HOME_ALTERNATE);
                if (signaturesMatch(mPackageManager, metaPkg, activityPkg)) {
                    homePackages.add(metaPkg);
                }
            }
        }
        return homePackages;
    }

    public void setEnabled(boolean enabled) {
        mEnableDisablePreference.setEnabled(enabled);
        mUIUpdateCallback.notifyUpdate(mStateIdentifier, mEnableDisablePreference);
    }

    private static boolean signaturesMatch(PackageManager pm, String pkg1, String pkg2) {
        if (pkg1 != null && pkg2 != null) {
            try {
                final int match = pm.checkSignatures(pkg1, pkg2);
                if (match >= PackageManager.SIGNATURE_MATCH) {
                    return true;
                }
            } catch (Exception e) {
                // e.g. named alternate package not found during lookup;
                // this is an expected case sometimes
            }
        }
        return false;
    }

    private boolean canDisable() {
        final HashSet<String> homePackages = getHomePackages();
        PackageInfo packageInfo;
        try {
            packageInfo = mPackageManager.getPackageInfo(mAppEntry.info.packageName,
                    PackageManager.GET_DISABLED_COMPONENTS
                            | PackageManager.GET_UNINSTALLED_PACKAGES
                            | PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return !(homePackages.contains(mAppEntry.info.packageName)
                || LibUtils.isSystemPackage(mContext.getResources(), mPackageManager, packageInfo));
    }

    @Override
    void refresh() {
        if (mAppEntry == null) {
            return;
        }
        if (!UninstallPreferenceController.canUninstall(mAppEntry) && canDisable()) {
            mEnableDisablePreference.setVisible(true);
            if (mAppEntry.info.enabled) {
                mEnableDisablePreference.setTitle(ResourcesUtil.getString(
                        mContext, "device_apps_app_management_disable"));
            } else {
                mEnableDisablePreference.setTitle(ResourcesUtil.getString(mContext,
                        "evice_apps_app_management_enable"));
            }
        } else {
            mEnableDisablePreference.setVisible(false);
        }
    }

}
