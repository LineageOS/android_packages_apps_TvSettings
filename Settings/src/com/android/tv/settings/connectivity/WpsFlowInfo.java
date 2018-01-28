/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.tv.settings.connectivity;

import android.arch.lifecycle.ViewModel;
import android.net.wifi.WifiManager;

/**
 * Class responsible for storing the information of WPS set-up process.
 */
public class WpsFlowInfo extends ViewModel {
    private String mPin;
    private String mErrorMessage;
    private boolean mWpsComplete;
    private boolean mIsActive;
    private WifiManager mWifiManager;
    private WifiManager.WpsCallback mWpsCallback;
    private int mWpsMethod = 0;

    public String getPin() {
        return mPin;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }

    public void setPin(String pin) {
        this.mPin = pin;
    }

    public void setErrorMessage(String errorMessage) {
        this.mErrorMessage = errorMessage;
    }

    public boolean isWpsComplete() {
        return mWpsComplete;
    }

    public void setWpsComplete(boolean wpsComplete) {
        this.mWpsComplete = wpsComplete;
    }

    public WifiManager getWifiManager() {
        return mWifiManager;
    }

    public void setWifiManager(WifiManager wifiManager) {
        this.mWifiManager = wifiManager;
    }

    public boolean isActive() {
        return mIsActive;
    }

    public void setActive(boolean active) {
        mIsActive = active;
    }

    public void setWpsMethod(int wpsMethod) {
        this.mWpsMethod = wpsMethod;
    }

    public int getWpsMethod() {
        return this.mWpsMethod;
    }

    public void setWpsCallback(WifiManager.WpsCallback wpsCallback) {
        this.mWpsCallback = wpsCallback;
    }

    public WifiManager.WpsCallback getWpsCallback() {
        return this.mWpsCallback;
    }
}
