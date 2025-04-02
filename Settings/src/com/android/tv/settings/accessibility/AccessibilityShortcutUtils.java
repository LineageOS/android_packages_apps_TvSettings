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

package com.android.tv.settings.accessibility;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.settingslib.accessibility.AccessibilityUtils;

/**
 * Utility class for Accessibility shortcut functions
 */
public class AccessibilityShortcutUtils {
    public static final String ACCESSIBILITY_SHORTCUT_STORE = "accessibility_shortcut";
    public static final String LAST_SHORTCUT_SERVICE = "last_shortcut_service";
    public static final String CHOOSE_TIMES = "choose_times";
    public static final String FIRST_VIEW = "first_view";

    /** Utility function to enable or disable accessibility shortcut functionality. */
    public static void setAccessibilityShortcutEnabled(
            Context context, SharedPreferences mSharedPref, boolean enabled) {
        //Because the first time the shortcut is viewed,
        //getLastShortcutService() is null, resulting in the
        //ACCESSIBILITY_SHORTCUT_TARGET_SERVICE value not being put correctly the first time,
        //so isFirstView is set to differentiate it.
        if (enabled) {
            String updatedComponent = isFirstView(context, mSharedPref)
                    ? getCurrentService(context) :
                    getLastShortcutService(mSharedPref);
            if (!TextUtils.isEmpty(updatedComponent)) {
                SharedPreferences chooseTimes = context.getSharedPreferences(CHOOSE_TIMES,
                        Context.MODE_PRIVATE);
                chooseTimes.edit().putBoolean(FIRST_VIEW, false).apply();
                Settings.Secure.putString(context.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE, updatedComponent);
            }
        } else {
            Settings.Secure.putString(context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE, "");
        }
    }

    /** Returns whether the first time viewing the accessibility shortcut or not. */
    public static boolean isFirstView(Context context, SharedPreferences mSharedPref) {
        SharedPreferences chooseTimes = context.getSharedPreferences(CHOOSE_TIMES,
                Context.MODE_PRIVATE);
        return chooseTimes.getBoolean(FIRST_VIEW, true);
    }

    /** Returns the last used shortcut service from the stored preferences (SharedPreferences). */
    public static String getLastShortcutService(SharedPreferences mSharedPref) {
        return mSharedPref.getString(LAST_SHORTCUT_SERVICE, "");
    }

    /** Stores the current service name (flattened componentName) in the SharedPreferences. */
    public static void putLastShortcutService(SharedPreferences mSharedPref, String serviceName) {
        mSharedPref.edit().putString(LAST_SHORTCUT_SERVICE, serviceName).apply();
    }

    /** Returns the current accessibility service name which flatten string of ComponentMame. */
    public static String getCurrentService(Context context) {
        String shortcutServiceString = AccessibilityUtils
                .getShortcutTargetServiceComponentNameString(context, UserHandle.myUserId());
        if (shortcutServiceString != null) {
            ComponentName shortcutName = ComponentName.unflattenFromString(shortcutServiceString);
            if (shortcutName != null) {
                return shortcutName.flattenToString();
            }
        }
        return null;
    }

}
