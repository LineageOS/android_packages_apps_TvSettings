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

package com.android.tv.settings.util;

import android.app.slice.SliceManager;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.tv.settings.overlay.FlavorUtils;
import com.android.tv.twopanelsettings.slices.SlicePreference;

import java.util.Collection;

import kotlin.coroutines.Continuation;

/** Utility class for slice **/
public final class SliceUtils {
    private static final String TAG = "SliceUtils";

    public static final String PATH_SLICE_FRAGMENT =
            "com.android.tv.twopanelsettings.slices.SliceFragment";

    /**
     * Check if slice provider exists.
     */
    public static boolean isSliceProviderValid(Context context, String stringUri) {
        if (TextUtils.isEmpty(stringUri)) {
            return false;
        }
        Uri uri = Uri.parse(stringUri);
        ProviderInfo providerInfo =
                context.getPackageManager()
                        .resolveContentProvider(uri.getAuthority(), /* flags= */ 0);
        if (providerInfo == null) {
            Log.i(TAG, "Slice Provider not found for: " + stringUri);
            return false;
        }
        return true;
    }

    public static boolean maybeUseSlice(@Nullable Preference preference,
                                        @Nullable SlicePreference slicePreference) {
        boolean usingSlice = slicePreference != null
                && FlavorUtils.isTwoPanel(slicePreference.getContext())
                && SliceUtilsKt.isSettingsSliceEnabledSync(slicePreference.getContext(),
                    slicePreference.getUri(), /* topLevelSettingsSliceUri = */ null);
        if (slicePreference != null) {
            slicePreference.setVisible(usingSlice);
        }

        if (preference != null) {
            preference.setVisible(!usingSlice);
        }

        return usingSlice;
    }
}
