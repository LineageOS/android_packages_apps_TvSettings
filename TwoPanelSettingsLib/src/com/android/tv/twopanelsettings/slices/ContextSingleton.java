/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.tv.twopanelsettings.slices;

import android.content.Context;
import android.net.Uri;
import android.util.ArrayMap;
import android.util.Log;

import androidx.lifecycle.Observer;

import com.android.tv.twopanelsettings.slices.PreferenceSliceLiveData.SliceLiveDataImpl;
import com.android.tv.twopanelsettings.slices.base.SliceManager;
import com.android.tv.twopanelsettings.slices.compat.Slice;


/**
 * Ensure the SliceLiveData with same uri is created only once across the activity.
 */
public class ContextSingleton {
    private static final String TAG = "TvSettingsContext";
    private static ContextSingleton sInstance;
    private ArrayMap<Uri, SliceLiveDataImpl> mSliceMap;
    private boolean mGivenFullSliceAccess;

    /**
     * Get the instance.
     */
    public static ContextSingleton getInstance() {
        if (sInstance == null) {
            sInstance = new ContextSingleton();
        }
        return sInstance;
    }

    private ContextSingleton() {
        mSliceMap = new ArrayMap<>();
        mGivenFullSliceAccess = false;
    }

    /**
     * Get the corresponding SliceLiveData based on the uri.
     */
    public SliceLiveDataImpl getSliceLiveData(Context context, Uri uri) {
        if (!mSliceMap.containsKey(uri)) {
            mSliceMap.put(uri, PreferenceSliceLiveData.fromUri(context, uri));
        }

        return mSliceMap.get(uri);
    }

    /**
     * Register slice live data observer directly.
     */
    public void addSliceObserver(Context context, Uri uri, Observer<Slice> observer) {
        getSliceLiveData(context, uri).observeForever(observer);
    }

    /**
     * Unregister slice live data observer directly.
     */
    public void removeSliceObserver(Context context, Uri uri, Observer<Slice> observer) {
        getSliceLiveData(context, uri).removeObserver(observer);
    }

    /**
     *  Grant full access to current package.
     */
    public void grantFullAccess(Context ctx, Uri uri) {
        if (!mGivenFullSliceAccess) {
            String currentPackageName = ctx.getApplicationContext().getPackageName();
            // Uri cannot be null here as SliceManagerService calls notifyChange(uri, null) in
            // grantPermissionFromUser.
            SliceManager.from(ctx).grantPermissionFromUser(
                    uri, currentPackageName, true);
            mGivenFullSliceAccess = true;
        }
    }

    /**
     *  Grant full access to specific package.
     */
    public void grantFullAccess(Context ctx, String uri, String packageName) {
        try {
            SliceManager.from(ctx).grantPermissionFromUser(
                    Uri.parse(uri), packageName, true);
        } catch (Exception e) {
            Log.e(TAG, "Cannot grant full access to " + packageName + " " + e);
        }
    }
}
