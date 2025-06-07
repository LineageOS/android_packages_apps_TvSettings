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
package com.example.updatablesettings

import android.Manifest
import android.app.Activity.MODE_PRIVATE
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.android.tv.twopanelsettings.slices.HasSliceUri
import com.android.tv.twopanelsettings.slices.TvSettingsSliceProvider
import com.android.tv.twopanelsettings.slices.builders.PreferenceSliceBuilder
import com.android.tv.twopanelsettings.slices.builders.PreferenceSliceBuilder.RowBuilder
import com.android.tv.twopanelsettings.slices.compat.Slice
import com.android.tv.twopanelsettings.slices.compat.SliceProvider
import com.android.tv.twopanelsettings.slices.compat.SliceViewManager
import java.security.SecureRandom

/**
 * A sample OEM slice provider to augment TV Settings tree. To test
 * - Build and install this APK
 * - grant WRITE_SECURE_SETTINGS permission to be able to read from Google TV slice provider:
 * adb shell pm grant com.example.updatablesettings android.permission.WRITE_SECURE_SETTINGS
 * - Override R.string.main_settings_slice_provider (or whatever page you want to modify) to
 * point to content://com.example.updatablesettings.sliceprovider/main_prefs_x
 * - You should see added mock projector settings in settings tree and clicking them should
 * launch specified intents.
 */
class MainSettingsSliceProvider : TvSettingsSliceProvider() {
    override fun createSlice(sliceBuilder: PreferenceSliceBuilder, sliceUri: Uri): Boolean {
        // Call this where you want Google provided settings to appear within the page, in this
        // case they are in the beginning. If screen title is not set before this call, it's taken
        // from Google provided data. Do not modify data within googlePrefs because TV Settings and
        // Google slice provider can make arbitrary assumptions about structure and content of the
        // settings. For the same reason, upstream any necessary changes to TV Settings code and
        // resources for visibility.
        sliceBuilder.addFromSliceUri(requireContext(), Uri.parse(GOOGLE_SETTINGS_URI))

        // Additional device specific preferences are added in the end.
        val projectorCategory = RowBuilder()
        projectorCategory.className = "androidx.preference.PreferenceCategory"
        projectorCategory.title = "Projector Settings"
        projectorCategory.key = "ProjectorSettingsCategory"
        projectorCategory.properties = Bundle().apply {
            putBoolean("orderingAsAdded", true)
        }

        val pictureSettings = RowBuilder()
        pictureSettings.key = "ProjectorPictureSettings"
        pictureSettings.title = "Picture Settings"
        pictureSettings.setAction(Intent("android.settings.SOUND_SETTINGS"))
        projectorCategory.addPreference(pictureSettings)

        val keystoneCorrection = RowBuilder()
        keystoneCorrection.key = "KeystoneCorrection"
        keystoneCorrection.title = "Keystone Correction"
        keystoneCorrection.setAction(Intent(requireContext(),
            KeystoneCorrectionActivity::class.java))
        projectorCategory.addPreference(keystoneCorrection)

        sliceBuilder.addPreference(projectorCategory)
        return true
    }

    companion object {
        const val GOOGLE_SETTINGS_URI = "content://com.google.android.libraries.tv.widgets" +
                ".updatablesettings.sliceprovider/main_prefs_x"
    }
}