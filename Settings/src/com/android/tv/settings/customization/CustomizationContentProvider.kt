/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.tv.settings.customization

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Bundle

/**
 * Content provider for OEM customization.
 *
 * To test, use adb shell content call:
 *
 * Set a custom language list (e.g., English and Spanish):
 * adb shell content call --uri content://com.android.tv.settings.customization --method call \
 *   --extra language_list:s:en,es
 *
 * Set a custom country list (e.g., United States and Germany):
 * adb shell content call --uri content://com.android.tv.settings.customization --method call \
 *   --extra country_list:s:US,DE
 *
 * To clear a list, pass an empty string:
 * adb shell content call --uri content://com.android.tv.settings.customization --method call \
 *   --extra language_list:s:
 */
class CustomizationContentProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val context = context ?: return null
        val sharedPreferences = getCustomizationSharedPreferences(context)
        val editor = sharedPreferences.edit()

        extras?.keySet()?.forEach { key ->
            when (val value = extras.get(key)) {
                is Int -> editor.putInt(key, value)
                is String -> editor.putString(key, value)
                is Float -> editor.putFloat(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Long -> editor.putLong(key, value)
            }
        }
        editor.apply()
        return null
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    companion object {
        private const val SHARED_PREFS_NAME = "tv_settings_customization"

        fun getCustomizationSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
}
