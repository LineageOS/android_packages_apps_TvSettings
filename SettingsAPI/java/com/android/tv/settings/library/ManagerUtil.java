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

package com.android.tv.settings.library;

import static com.android.tv.settings.library.PreferenceCompat.STATUS_OFF;
import static com.android.tv.settings.library.PreferenceCompat.STATUS_ON;

import android.annotation.SystemApi;

/**
 * @hide
 * Provide constants and utility methods.
 */
@SystemApi
public final class ManagerUtil {
    public static final int OFFSET_MULTIPLIER = 10000;
    public static final int STATE_NETWORK_MAIN = 0;
    public static final int STATE_WIFI_DETAILS = 1;
    public static final int STATE_DEVICE_MAIN = 2;
    public static final int STATE_APPS = 3;
    public static final int STATE_ALL_APPS = 4;
    public static final int STATE_APP_MANAGEMENT = 5;

    public static final String INFO_INTENT = "intent";
    public static final String INFO_NEXT_STATE = "next_state";
    public static final String INFO_WIFI_SIGNAL_LEVEL = "wifi_signal_level";
    public static final String INFO_COLLAPSE = "collapse";

    private ManagerUtil() {
    }

    static byte getChecked(boolean checked) {
        return checked ? STATUS_ON : STATUS_OFF;
    }

    static byte getSelectable(boolean selectable) {
        return selectable ? STATUS_ON : STATUS_OFF;
    }

    static byte getVisible(boolean visible) {
        return visible ? STATUS_ON : STATUS_OFF;
    }

    static byte getEnabled(boolean enabled) {
        return enabled ? STATUS_ON : STATUS_OFF;
    }

    /**
     * @hide
     * Return whether the preference is checked.
     * 0 : not updated, 1 : unchecked, 2 : checked
     */
    @SystemApi
    public static boolean isChecked(PreferenceCompat pref) {
        return pref.getChecked() == STATUS_ON ? true : false;
    }

    /**
     * @hide
     * Return whether the preference is visible.
     * 0 : not updated, 1 : invisible, 2 : visible
     */
    @SystemApi
    public static boolean isVisible(PreferenceCompat pref) {
        return pref.getVisible() == STATUS_OFF ? true : false;
    }

    /**
     * @hide
     * Calculate the compound code based on the state identifier and request code.
     * @param state state identifier
     * @param requestCode requestCode
     * @return compound code
     */
    @SystemApi
    public static int calculateCompoundCode(int state, int requestCode) {
        return OFFSET_MULTIPLIER * (state + 1) + requestCode;
    }

    /**
     * @hide
     * Get the state identifer based on the compound code.
     * @param code compound code
     * @return state identifier
     */
    @SystemApi
    public static int getStateIdentifier(int code) {
        if (code < OFFSET_MULTIPLIER) {
            return -1;
        }
        return code / OFFSET_MULTIPLIER - 1;
    }

    /**
     * @hide
     * Return the request code for a particular state.
     * @param code compound code
     * @return request code
     */
    @SystemApi
    public static int getRequestCode(int code) {
        return code % OFFSET_MULTIPLIER;
    }
}
