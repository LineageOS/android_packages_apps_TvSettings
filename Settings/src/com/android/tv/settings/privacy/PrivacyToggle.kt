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

package com.android.tv.settings.privacy

import android.app.AppOpsManager
import android.content.Context
import android.hardware.SensorPrivacyManager
import android.hardware.SensorPrivacyManager.Sensors.Sensor
import android.os.Bundle
import android.provider.DeviceConfig
import androidx.preference.Preference
import com.android.tv.settings.R

public enum class PrivacyToggle(
        val screenTitle: Int,
        val toggleTitle: Int,
        val toggleInfoTitle: Int,
        val toggleInfoText: Int,
        val appPermissionsTitle: Int,
        val permissionsGroupName: String,
        @Sensor val sensor: Int,
        val appOps: IntArray,
        val deviceConfigName: String,
) {
    CAMERA_TOGGLE(
            R.string.camera,
            R.string.camera_toggle_title,
            R.string.camera_toggle_info_title,
            R.string.camera_toggle_info_content,
            R.string.open_camera_permissions,
            "android.permission-group.CAMERA",
            SensorPrivacyManager.Sensors.CAMERA,
            intArrayOf(AppOpsManager.OP_CAMERA, AppOpsManager.OP_PHONE_CALL_CAMERA),
            "camera_toggle_enabled",
    ),

    MIC_TOGGLE(
            R.string.microphone,
            R.string.mic_toggle_title,
            R.string.mic_toggle_info_title,
            R.string.mic_toggle_info_content,
            R.string.open_mic_permissions,
            "android.permission-group.MICROPHONE",
            SensorPrivacyManager.Sensors.MICROPHONE,
            intArrayOf(AppOpsManager.OP_RECORD_AUDIO, AppOpsManager.OP_PHONE_CALL_MICROPHONE),
            "mic_toggle_enabled",
    );

    fun isPresentAndEnabled(context: Context): Boolean {
        val sensorPrivacyManager = context.getSystemService(SensorPrivacyManager::class.java)
        return sensorPrivacyManager!!.supportsSensorToggle(sensor)
                && DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_PRIVACY,
                deviceConfigName,
                /* defaultValue= */ true)
    }

    /**
     * Hides the preference if the toggle shouldn't be shown and adds the toggle to the extras so
     * the SensorFragment knows which sensor is meant.
     */
    fun preparePreferenceWithSensorFragment(context: Context, pref: Preference?, extrasKey: String) {
        pref?.let {
            if (isPresentAndEnabled(context)) {
                it.extras.putObject(extrasKey, this)
            } else {
                it.isVisible = false
            }
        }
    }
}