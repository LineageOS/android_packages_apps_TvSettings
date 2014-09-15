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

package com.android.tv.settings.notifications;

import com.android.internal.R;
import com.android.tv.settings.util.UriUtils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.UserHandle;
import android.util.Log;

/**
 * Uri util functions for notifications.
 */
class UriUtil {

    private static final String TAG = "UriUtil";

    static Resources getPackageResources(Context context, String pkg, int userId) {

        if (pkg != null) {
            try {
                int userIdToUse = (userId == UserHandle.USER_ALL) ? UserHandle.USER_OWNER : userId;
                return context.getPackageManager().getResourcesForApplicationAsUser(pkg,
                        userIdToUse);
            } catch (NameNotFoundException nnfe) {
                Log.w(TAG, nnfe.toString());
            }
        }
        return null;
    }

    static String getIconUri(Context context, String pkg, int userId, int resourceId) {

        Resources packageResources = getPackageResources(context, pkg, userId);
        String resourceName = getResourceName(packageResources, resourceId);
        if (resourceName != null) {
            Intent.ShortcutIconResource iconResource = new Intent.ShortcutIconResource();
            iconResource.packageName = pkg;
            iconResource.resourceName = resourceName;
            return UriUtils.getShortcutIconResourceUri(iconResource).toString();
        }
        return null;
    }

    static String getResourceName(Resources packageResources, int resourceId) {

        if (resourceId != 0 && packageResources != null) {
            try {
                return packageResources.getResourceName(resourceId);
            } catch (NotFoundException nfe) {
                Log.w(TAG, nfe.toString());
            }
        }
        return null;
    }

    static String getAppIconUri(Context context, String pkg, int userId, int[] iconResourceIds) {

        Resources packageResources = getPackageResources(context, pkg, userId);

        int zeroCount = 0;
        for (int i = 0; i < iconResourceIds.length; i++) {
            if (iconResourceIds[i] == 0) {
                zeroCount++;
            } else if (packageResources != null) {
                String resourceName = getResourceName(packageResources, iconResourceIds[i]);
                if (resourceName != null) {
                    Intent.ShortcutIconResource iconResource = new Intent.ShortcutIconResource();
                    iconResource.packageName = pkg;
                    iconResource.resourceName = resourceName;
                    return UriUtils.getShortcutIconResourceUri(iconResource).toString();
                }
            }
        }

        if (zeroCount == iconResourceIds.length) {
            return UriUtils.getAndroidResourceUri(Resources.getSystem(),
                    R.drawable.sym_def_app_icon);
        } else {
            return UriUtils.getAndroidResourceUri(context.getResources(),
                    R.drawable.sym_app_on_sd_unavailable_icon);
        }
    }
}
