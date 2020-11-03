/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.tv.settings.overlay;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.tv.settings.R;

/** An Util class that manages logic related to build flavor and feature. */
// TODO(zhensun@): Integrate FeatureFactory with OverlayUtils
public final class OverlayUtils {

    private static final String TAG = "OverlayUtils";

    // Build flavors of TvSettings that determines
    public static final int FLAVOR_UNDEFINED = -1; // Error/undefined flavor
    public static final int FLAVOR_CLASSIC = 0;    // The ordinary classic one panel settings
    public static final int FLAVOR_TWO_PANEL = 1;  // The two panel settings
    public static final int FLAVOR_X = 2;          // The two panel settings with the X experience
    public static final int FLAVOR_VENDOR = 3;     // The two panel settings with Vendor overlay

    /** Returns the flavor of current TvSettings. */
    public static int getFlavor(@Nullable Context context) {
        if (context == null) {
            Log.w(TAG, "Trying to get flavor from null context. Returning undefined flavor.");
            return FLAVOR_UNDEFINED;
        }
        String flavor = context.getString(R.string.config_tvSettingsFlavor);
        if (TextUtils.isEmpty(flavor)) {
            return FLAVOR_CLASSIC;
        }
        switch (flavor) {
            case "Classic":
                return FLAVOR_CLASSIC;
            case "TwoPanel":
                return FLAVOR_TWO_PANEL;
            case "X":
                return FLAVOR_X;
            case "Vendor":
                return FLAVOR_VENDOR;
            default:
                Log.w(TAG, "Flavor is unspecified. Default to Classic flavor.");
                return FLAVOR_CLASSIC;
        }
    }
}
