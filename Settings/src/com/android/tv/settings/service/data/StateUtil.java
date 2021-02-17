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

package com.android.tv.settings.service.data;

import static com.android.tv.settings.service.ServiceUtil.STATE_NETWORK_MAIN;
import static com.android.tv.settings.service.ServiceUtil.STATE_WIFI_DETAILS;

import android.content.Context;

import com.android.tv.settings.service.ISettingsServiceListener;
import com.android.tv.settings.service.network.NetworkMainState;
import com.android.tv.settings.service.network.WifiDetailsState;

import java.util.Map;

public final class StateUtil {
    private StateUtil() {
    }

    public static State createState(Context context, int state, ISettingsServiceListener listener,
            Map<Integer, State> stateMap) {
        switch (state) {
            case STATE_NETWORK_MAIN :
                stateMap.put(STATE_NETWORK_MAIN, new NetworkMainState(context, listener));
                break;
            case STATE_WIFI_DETAILS :
                stateMap.put(STATE_WIFI_DETAILS, new WifiDetailsState(context, listener));
                break;
            default:
                // no-op
        }
        return stateMap.get(state);
    }

    public static State getState(int state, Map<Integer, State> stateMap) {
        return stateMap.get(state);
    }

    public static void removeState(int state, Map<Integer, State> stateMap) {
        stateMap.remove(state);
    }
}
