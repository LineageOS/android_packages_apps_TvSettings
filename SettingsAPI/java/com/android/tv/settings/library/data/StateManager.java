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

package com.android.tv.settings.library.data;

import static com.android.tv.settings.library.ManagerUtil.STATE_ALARMS_AND_REMINDERS;
import static com.android.tv.settings.library.ManagerUtil.STATE_ALL_APPS;
import static com.android.tv.settings.library.ManagerUtil.STATE_APPS;
import static com.android.tv.settings.library.ManagerUtil.STATE_APP_MANAGEMENT;
import static com.android.tv.settings.library.ManagerUtil.STATE_APP_USAGE_ACCESS;
import static com.android.tv.settings.library.ManagerUtil.STATE_HIGH_POWER;
import static com.android.tv.settings.library.ManagerUtil.STATE_NETWORK_MAIN;
import static com.android.tv.settings.library.ManagerUtil.STATE_NOTIFICATION_ACCESS;
import static com.android.tv.settings.library.ManagerUtil.STATE_PICTURE_IN_PICTURE;
import static com.android.tv.settings.library.ManagerUtil.STATE_SPECIAL_ACCESS;
import static com.android.tv.settings.library.ManagerUtil.STATE_SYSTEM_ALERT_WINDOW;
import static com.android.tv.settings.library.ManagerUtil.STATE_WIFI_DETAILS;
import static com.android.tv.settings.library.ManagerUtil.STATE_WRITE_SETTINGS;

import android.content.Context;

import com.android.tv.settings.library.State;
import com.android.tv.settings.library.UIUpdateCallback;
import com.android.tv.settings.library.device.apps.AllAppsState;
import com.android.tv.settings.library.device.apps.AppManagementState;
import com.android.tv.settings.library.device.apps.AppsState;
import com.android.tv.settings.library.device.apps.specialaccess.AlarmsAndRemindersState;
import com.android.tv.settings.library.device.apps.specialaccess.AppUsageAccessState;
import com.android.tv.settings.library.device.apps.specialaccess.HighPowerState;
import com.android.tv.settings.library.device.apps.specialaccess.NotificationAccessState;
import com.android.tv.settings.library.device.apps.specialaccess.PictureInPictureState;
import com.android.tv.settings.library.device.apps.specialaccess.SpecialAppAccessState;
import com.android.tv.settings.library.device.apps.specialaccess.SystemAlertWindowState;
import com.android.tv.settings.library.device.apps.specialaccess.WriteSettingsState;
import com.android.tv.settings.library.network.NetworkMainState;
import com.android.tv.settings.library.network.WifiDetailsState;

/** Manager to handle creation and removal of the {@link State}. */
public class StateManager {
    private StateManager() {
    }

    public static State createState(
            Context context, int stateIdentifier, UIUpdateCallback uiUpdateCallback) {
        State state = null;
        switch (stateIdentifier) {
            case STATE_NETWORK_MAIN:
                state = new NetworkMainState(context, uiUpdateCallback);
                break;
            case STATE_WIFI_DETAILS:
                state = new WifiDetailsState(context, uiUpdateCallback);
                break;
            case STATE_ALL_APPS:
                state = new AllAppsState(context, uiUpdateCallback);
                break;
            case STATE_APPS:
                state = new AppsState(context, uiUpdateCallback);
                break;
            case STATE_APP_MANAGEMENT:
                state = new AppManagementState(context, uiUpdateCallback);
                break;
            case STATE_SPECIAL_ACCESS:
                state = new SpecialAppAccessState(context, uiUpdateCallback);
                break;
            case STATE_HIGH_POWER:
                state = new HighPowerState(context, uiUpdateCallback);
                break;
            case STATE_NOTIFICATION_ACCESS:
                state = new NotificationAccessState(context, uiUpdateCallback);
                break;
            case STATE_APP_USAGE_ACCESS:
                state = new AppUsageAccessState(context, uiUpdateCallback);
                break;
            case STATE_PICTURE_IN_PICTURE:
                state = new PictureInPictureState(context, uiUpdateCallback);
                break;
            case STATE_ALARMS_AND_REMINDERS:
                state = new AlarmsAndRemindersState(context, uiUpdateCallback);
                break;
            case STATE_SYSTEM_ALERT_WINDOW:
                state = new SystemAlertWindowState(context, uiUpdateCallback);
                break;
            case STATE_WRITE_SETTINGS:
                state = new WriteSettingsState(context, uiUpdateCallback);
                break;
            default:
                // no-op
        }
        return state;
    }
}
