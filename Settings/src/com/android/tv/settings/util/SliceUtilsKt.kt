/*
 * Copyright (C) 2024 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.tv.settings.util

import android.app.slice.SliceManager
import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION") // Slices are still supported for TV Settings.
object SliceUtilsKt {
    private const val TAG = "SliceUtilsKt"

    /**
     * Checks if the slice is available in the background if needed
     *
     * @param context                  Current context of the app
     * @param uri                      Settings slice uri
     * @param topLevelSettingsSliceUri Top level settings slice uri, if null, use provided uri to
     * deduce top level settings slice uri.
     * @return returns true if slice is enabled, false otherwise
     */
    @JvmStatic
    suspend fun isSettingsSliceEnabled(context: Context, uri: String?,
                                       topLevelSettingsSliceUri: String?): Boolean {
        if (uri == null) {
            return false
        }
        val sliceManager = context.getSystemService(SliceManager::class.java)
            ?: return false
        return withContext(Dispatchers.IO) {
            return@withContext isSettingsSliceEnabledInternal(
                context, sliceManager,
                uri, topLevelSettingsSliceUri
            )
        }
    }

    /**
     * Checks if the slice is available in the background if needed
     *
     * @param context                  Current context of the app
     * @param uri                      Settings slice uri
     * @param topLevelSettingsSliceUri Top level settings slice uri, if null, use provided uri to
     * deduce top level settings slice uri.
     * @return returns true if slice is enabled, false otherwise
     */
    @JvmStatic
    fun isSettingsSliceEnabledSync(
        context: Context, uri: String?,
        topLevelSettingsSliceUri: String?
    ): Boolean {
        if (uri == null) {
            return false
        }
        val sliceManager = context.getSystemService(SliceManager::class.java)
            ?: return false
        return isSettingsSliceEnabledInternal(context, sliceManager, uri, topLevelSettingsSliceUri)
    }

    private fun isSettingsSliceEnabledInternal(
        context: Context, sliceManager: SliceManager,
        uri: String, topLevelSettingsSliceUri: String?
    ): Boolean {
        val topLevelSettingsSlice = if (topLevelSettingsSliceUri == null) {
            Uri.parse(uri).buildUpon().path("/").build()
        } else {
            Uri.parse(ResourcesUtil.getString(context, topLevelSettingsSliceUri))
        }
        val enabledSlicesUri = sliceManager.getSliceDescendants(topLevelSettingsSlice)
        if (enabledSlicesUri.isEmpty()) {
            // SliceProvider likely does not support listing descendants.
            return sliceManager.bindSlice(Uri.parse(uri), setOf()) != null
        }
        for (sliceUri in enabledSlicesUri) {
            Log.i(TAG, "Enabled slice: $sliceUri")
            if (sliceUri.toString() == uri) {
                return true
            }
        }
        return false
    }
}