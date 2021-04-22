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

package com.android.tv.settings.util;

import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.Context;
import android.net.Uri;

import androidx.slice.SliceProvider;

import java.util.Optional;

/** Utility class for slice **/
public final class SliceUtils {
    public static final String PATH_SLICE_FRAGMENT =
            "com.android.tv.twopanelsettings.slices.SliceFragment";
    /**
     * Check if slice provider exists.
     */
    public static boolean isSliceProviderValid(Context context, String uri) {
        if (uri == null) {
            return false;
        }
        ContentProviderClient client =
                context.getContentResolver().acquireContentProviderClient(Uri.parse(uri));
        if (client != null) {
            client.close();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if {@link androidx.slice.SliceProvider SliceProvider} exists and is not gated
     * by an experiment.
     */
    public static boolean isSliceEnabled(Context context, String uri) {
        if (uri == null) {
            return false;
        }
        final Optional<ContentProvider> contentProviderOptional = Optional.ofNullable(
                context.getContentResolver().acquireContentProviderClient(Uri.parse(uri)))
                .map(ContentProviderClient::getLocalContentProvider);
        /*
        ContentProviderClient::getLocalContentProvider always returns null. Currently there
        is no way to determine for sure if the slice is enabled for the current device or not.
        Thus this method will always return false. For now including the code to render the
        UI for the SlicePreference but it will not show.
        TODO: Add logic for determining if the slice is actually enabled.
         */
        if (!contentProviderOptional.isPresent()
                || !(contentProviderOptional.get() instanceof SliceProvider)) {
            return false;
        }
        final SliceProvider sliceProvider = (SliceProvider) contentProviderOptional.get();
        try {
            sliceProvider.onSlicePinned(Uri.parse(uri));
            return true;
        } catch (IllegalStateException illegalStateException) {
            return false;
        }
    }
}
