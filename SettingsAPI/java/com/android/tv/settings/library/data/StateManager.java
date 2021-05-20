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

import static com.android.tv.settings.library.ManagerUtil.STATE_NETWORK_MAIN;
import static com.android.tv.settings.library.ManagerUtil.STATE_WIFI_DETAILS;

import android.content.Context;
import android.util.Pair;

import com.android.tv.settings.library.UIUpdateCallback;
import com.android.tv.settings.library.network.NetworkMainState;
import com.android.tv.settings.library.network.WifiDetailsState;

import java.util.Map;

/** Manager to handle creation and removal of the {@link State}. */
public class StateManager {
    private StateManager() {
    }

    public static State createState(
            Context context, int stateIdentifier, UIUpdateCallback uiUpdateCallback,
            Map<Integer, Pair<State, Integer>> stateMap) {
        State state = null;
        switch (stateIdentifier) {
            case STATE_NETWORK_MAIN:
                state = new NetworkMainState(context, uiUpdateCallback);
                break;
            case STATE_WIFI_DETAILS:
                state = new WifiDetailsState(context, uiUpdateCallback);
                break;
            default:
                // no-op
        }
        if (!stateMap.containsKey(stateIdentifier)) {
            stateMap.put(stateIdentifier, new Pair(state, 0));
        }
        Pair<State, Integer> stateAndCount = stateMap.get(stateIdentifier);
        stateMap.put(stateIdentifier, new Pair<>(stateAndCount.first, stateAndCount.second + 1));
        return stateAndCount.first;
    }

    public static State getState(int stateIdentifier, Map<Integer, Pair<State, Integer>> stateMap) {
        return stateMap.get(stateIdentifier).first;
    }

    public static void removeState(
            int stateIdentifier, Map<Integer, Pair<State, Integer>> stateMap) {
        Pair<State, Integer> stateAndCount = stateMap.get(stateIdentifier);
        stateMap.put(stateIdentifier, new Pair(stateAndCount.first, stateAndCount.second - 1));
        if (stateAndCount.second == 1) {
            stateMap.remove(stateIdentifier);
        }
    }
}
