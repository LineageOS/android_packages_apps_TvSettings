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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import com.android.tv.settings.library.ManagerUtil;
import com.android.tv.settings.library.PreferenceCompat;
import com.android.tv.settings.library.UIUpdateCallback;
import com.android.tv.settings.library.data.PreferenceControllerState;
import com.android.tv.settings.library.util.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class AppsState extends PreferenceControllerState {
    private static final String KEY_RECENT_USED_APPS = "recently_used_apps_category";
    private static final String KEY_SEE_ALL_APPS = "see_all_apps";
    private static final String KEY_CATEGORY_PERMISSIONS = "category_permissions";
    private static final String KEY_MANAGE_PERMISSIONS = "manage_permissions";
    private static final String KEY_SPECIAL_APP_ACCESS = "special_app_access";
    private static final String KEY_SECURITY = "security";
    public static final String EXTRA_VOLUME_UUID = "volumeUuid";
    public static final String EXTRA_VOLUME_NAME = "volumeName";
    private PreferenceCompat mRecentUsedAppsPref;
    private PreferenceCompat mSeeAllAppsPref;
    private PreferenceCompat mPermissionsPrefCategory;
    private PreferenceCompat mManagePermissionsPref;
    private PreferenceCompat mSpecialAccessPref;
    private PreferenceCompat mSecurityPref;

    public AppsState(Context context,
            UIUpdateCallback callback) {
        super(context, callback);
    }

    @Override
    public void onCreate(Bundle extras) {
        super.onCreate(extras);
        mRecentUsedAppsPref = mPreferenceCompatManager.getOrCreatePrefCompat(KEY_RECENT_USED_APPS);
        mSeeAllAppsPref = mPreferenceCompatManager.getOrCreatePrefCompat(KEY_SEE_ALL_APPS);
        mPermissionsPrefCategory = mPreferenceCompatManager.getOrCreatePrefCompat(
                KEY_CATEGORY_PERMISSIONS);
        mManagePermissionsPref = mPreferenceCompatManager.getOrCreatePrefCompat(
                KEY_MANAGE_PERMISSIONS);
        mSpecialAccessPref = mPreferenceCompatManager.getOrCreatePrefCompat(KEY_SPECIAL_APP_ACCESS);
        mSecurityPref = mPreferenceCompatManager.getOrCreatePrefCompat(KEY_SECURITY);
    }

    @Override
    public void onPreferenceTreeClick(String key, boolean status) {
        super.onPreferenceTreeClick(key, status);
    }

    @Override
    public int getStateIdentifier() {
        return ManagerUtil.STATE_APPS;
    }

    @Override
    protected List<AbstractPreferenceController> onCreatePreferenceControllers(Context context) {
        final Activity activity = (Activity) mContext;
        final Application app = activity != null ? activity.getApplication() : null;
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new RecentAppsPreferenceController(mContext, app, mUIUpdateCallback,
                getStateIdentifier()));
        return controllers;
    }
}