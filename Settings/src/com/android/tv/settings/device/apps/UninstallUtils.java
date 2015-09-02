/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Handles uninstalling of an application.
 */
class UninstallUtils {

    private static Signature sSystemSignature;

    static boolean canUninstall(AppInfo appInfo) {
        return !appInfo.isUpdatedSystemApp() && !appInfo.isSystemApp();
    }

    static boolean isEnabled(AppInfo appInfo) {
        return appInfo.isEnabled();
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

    private static HashSet<String> getHomePackages(PackageManager pm) {
        HashSet<String> homePackages = new HashSet<>();
        // Get list of "home" apps and trace through any meta-data references
        List<ResolveInfo> homeActivities = new ArrayList<>();
        pm.getHomeActivities(homeActivities);
        for (ResolveInfo ri : homeActivities) {
            final String activityPkg = ri.activityInfo.packageName;
            homePackages.add(activityPkg);
            // Also make sure to include anything proxying for the home app
            final Bundle metadata = ri.activityInfo.metaData;
            if (metadata != null) {
                final String metaPkg = metadata.getString(ActivityManager.META_HOME_ALTERNATE);
                if (signaturesMatch(pm, metaPkg, activityPkg)) {
                    homePackages.add(metaPkg);
                }
            }
        }
        return homePackages;
    }

    private static Signature getFirstSignature(PackageInfo pkg) {
        if (pkg != null && pkg.signatures != null && pkg.signatures.length > 0) {
            return pkg.signatures[0];
        }
        return null;
    }

    private static Signature getSystemSignature(PackageManager pm) {
        try {
            final PackageInfo sys = pm.getPackageInfo("android", PackageManager.GET_SIGNATURES);
            return getFirstSignature(sys);
        } catch (NameNotFoundException e) {
        }
        return null;
    }

    /**
     * Determine whether a package is a "system package", in which case certain things (like
     * disabling notifications or disabling the package altogether) should be disallowed.
     */
    private static boolean isSystemPackage(PackageManager pm, PackageInfo pkg) {
        if (sSystemSignature == null) {
            sSystemSignature = getSystemSignature(pm);
        }
        return sSystemSignature != null && sSystemSignature.equals(getFirstSignature(pkg));
    }

    static boolean canDisable(Context context, AppInfo appInfo) {
        final PackageManager pm = context.getPackageManager();
        final HashSet<String> homePackages = getHomePackages(pm);
        PackageInfo packageInfo;
        try {
            packageInfo = pm.getPackageInfo(appInfo.getPackageName(),
                    PackageManager.GET_DISABLED_COMPONENTS |
                    PackageManager.GET_UNINSTALLED_PACKAGES |
                    PackageManager.GET_SIGNATURES);
        } catch (NameNotFoundException e) {
            return false;
        }
        return ! (homePackages.contains(appInfo.getPackageName()) ||
                isSystemPackage(pm, packageInfo));
    }

    static boolean canUninstallUpdates(AppInfo appInfo) {
        return appInfo.isUpdatedSystemApp();
    }

    static Intent getUninstallUpdatesIntent(AppInfo appInfo) {
        if (canUninstallUpdates(appInfo)) {
            return getUninstallIntentInternal(appInfo);
        }
        return null;
    }

    static Intent getUninstallIntent(AppInfo appInfo) {
        if (canUninstall(appInfo)) {
            return getUninstallIntentInternal(appInfo);
        }
        return null;
    }

    private static Intent getUninstallIntentInternal(AppInfo appInfo) {
        Uri packageURI = Uri.parse("package:" + appInfo.getPackageName());
        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI);
        uninstallIntent.putExtra(Intent.EXTRA_UNINSTALL_ALL_USERS, true);
        uninstallIntent.putExtra(Intent.EXTRA_KEY_CONFIRM, true);
        return uninstallIntent;
    }
}
