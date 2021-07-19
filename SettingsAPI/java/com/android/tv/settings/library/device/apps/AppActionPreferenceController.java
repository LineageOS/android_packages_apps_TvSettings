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

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.tv.settings.library.PreferenceCompat;
import com.android.tv.settings.library.UIUpdateCallback;
import com.android.tv.settings.library.data.PreferenceCompatManager;
import com.android.tv.settings.library.util.AbstractPreferenceController;

/**
 * Base controller class to handle app action preference.
 */
public abstract class AppActionPreferenceController extends AbstractPreferenceController {
    ApplicationsState.AppEntry mAppEntry;
    static final String INTENT_CONFIRMATION = "android.settings.ui.CONFIRM";
    static final String ARG_PACKAGE_NAME = "packageName";
    static final String EXTRA_GUIDANCE_TITLE = "guidancetitle";
    static final String EXTRA_GUIDANCE_SUBTITLE = "guidanceSubtitle";
    static final String EXTRA_GUIDANCE_BREADCRUMB = "guidanceBreadcrumb";
    static final String EXTRA_GUIDANCE_ICON = "guidanceIcon";

    public AppActionPreferenceController(Context context,
            UIUpdateCallback callback, int stateIdentifier, ApplicationsState.AppEntry appEntry) {
        super(context, callback, stateIdentifier);
        mAppEntry = appEntry;
    }

    /**
     * Set entry and refresh pref.
     *
     * @param entry entry
     */
    public void setEntry(@NonNull ApplicationsState.AppEntry entry) {
        mAppEntry = entry;
        refresh();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceCompatManager screen) {
        refresh();
    }

    @Override
    public void updateState(PreferenceCompat preferenceCompat) {
        refresh();
    }

    protected String getAppName() {
        if (mAppEntry == null) {
            return null;
        }
        mAppEntry.ensureLabel(mContext);
        return mAppEntry.label;
    }

    abstract void refresh();
}
