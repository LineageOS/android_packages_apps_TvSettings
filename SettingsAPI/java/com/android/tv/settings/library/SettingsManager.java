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

import android.annotation.SystemApi;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.Pair;

import com.android.tv.settings.library.data.State;
import com.android.tv.settings.library.data.StateManager;

import java.util.List;

/**
 * @hide
 * Provides access to TvSettings data.
 */
@SystemApi
public final class SettingsManager {
    private static final String TAG = "TvSettingsManager";
    private static final boolean DEBUG = true;
    private final Handler mHandler = new Handler();
    private final ArrayMap<Integer, Pair<State, Integer>> mStateMap = new ArrayMap<>();
    private com.android.tv.settings.library.UIUpdateCallback mUIUpdateCallback;
    private Context mContext;

    /** @hide */
    @SystemApi
    public SettingsManager(Context context) {
        this.mContext = context;
    }

    /** @hide */
    @SystemApi
    public List<PreferenceCompat> getPreferences(int state) {
        return null;
    }

    /** @hide */
    @SystemApi
    public PreferenceCompat getPreference(int state, String key) {
        return null;
    }

    /** @hide */
    @SystemApi
    public void registerListener(com.android.tv.settings.library.UIUpdateCallback callback) {
        mUIUpdateCallback = callback;
    }

    /** @hide */
    @SystemApi
    public void unRegisterListener(UIUpdateCallback listener) {
        mUIUpdateCallback = null;
    }

    /** @hide */
    @SystemApi
    public void onCreate(int state, Bundle extras) {
        StateManager.createState(
                mContext, state, mUIUpdateCallback, mStateMap).onCreate(extras);
    }

    /** @hide */
    @SystemApi
    public void onStart(int state) {
        StateManager.getState(state, mStateMap).onStart();
    }

    /** @hide */
    @SystemApi
    public void onResume(int state) {
        StateManager.getState(state, mStateMap).onResume();
    }

    /** @hide */
    @SystemApi
    public void onPause(int state) {
        StateManager.getState(state, mStateMap).onPause();
    }

    /** @hide */
    @SystemApi
    public void onStop(int state) {
        StateManager.getState(state, mStateMap).onStop();
    }

    /** @hide */
    @SystemApi
    public void onDestroy(int state) {
        StateManager.getState(state, mStateMap).onDestroy();
        StateManager.removeState(state, mStateMap);
    }

    /** @hide */
    @SystemApi
    public void onPreferenceClick(int state, String key, boolean status) {
        StateManager.getState(state, mStateMap).onPreferenceTreeClick(key, status);
    }

    /** @hide */
    @SystemApi
    public void onPreferenceChange(int state, String key, Object newValue) {
        StateManager.getState(state, mStateMap).onPreferenceChange(key, newValue);
    }
}
