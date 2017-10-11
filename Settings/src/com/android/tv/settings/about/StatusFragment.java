/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.tv.settings.about;

import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;
import android.support.annotation.Nullable;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.tv.settings.R;
import com.android.tv.settings.core.lifecycle.ObservableLeanbackPreferenceFragment;

/**
 * Fragment for showing device hardware info, such as MAC addresses and serial numbers
 */
public class StatusFragment extends ObservableLeanbackPreferenceFragment {

    private static final String KEY_BATTERY_STATUS = "battery_status";
    private static final String KEY_BATTERY_LEVEL = "battery_level";
    private static final String KEY_SIM_STATUS = "sim_status";
    private static final String KEY_IMEI_INFO = "imei_info";

    private SerialNumberPreferenceController mSerialNumberPreferenceController;
    private UptimePreferenceController mUptimePreferenceController;
    private BluetoothAddressPreferenceController mBluetoothAddressPreferenceController;
    private IpAddressPreferenceController mIpAddressPreferenceController;
    private WifiMacAddressPreferenceController mWifiMacAddressPreferenceController;
    private ImsStatusPreferenceController mImsStatusPreferenceController;

    public static StatusFragment newInstance() {
        return new StatusFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        final Context context = getContext();
        final Lifecycle lifecycle = getLifecycle();

        mSerialNumberPreferenceController = new SerialNumberPreferenceController(context);
        mUptimePreferenceController = new UptimePreferenceController(context, lifecycle);
        mBluetoothAddressPreferenceController =
                new BluetoothAddressPreferenceController(context, lifecycle);
        mIpAddressPreferenceController = new IpAddressPreferenceController(context, lifecycle);
        mWifiMacAddressPreferenceController =
                new WifiMacAddressPreferenceController(context, lifecycle);
        mImsStatusPreferenceController = new ImsStatusPreferenceController(context, lifecycle);

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.device_info_status, null);
        final PreferenceScreen screen = getPreferenceScreen();

        // TODO: detect if we have a battery or not
        removePreference(findPreference(KEY_BATTERY_LEVEL));
        removePreference(findPreference(KEY_BATTERY_STATUS));

        mSerialNumberPreferenceController.displayPreference(screen);
        mUptimePreferenceController.displayPreference(screen);
        mBluetoothAddressPreferenceController.displayPreference(screen);
        mIpAddressPreferenceController.displayPreference(screen);
        mWifiMacAddressPreferenceController.displayPreference(screen);
        mImsStatusPreferenceController.displayPreference(screen);

        // Remove SimStatus and Imei for Secondary user as it access Phone b/19165700
        // Also remove on Wi-Fi only devices.
        //TODO: the bug above will surface in split system user mode.
        if (!UserManager.get(getActivity()).isAdminUser()
                || AboutFragment.isWifiOnly(getActivity())) {
            removePreference(findPreference(KEY_SIM_STATUS));
            removePreference(findPreference(KEY_IMEI_INFO));
        }
    }

    private void removePreference(@Nullable Preference preference) {
        if (preference != null) {
            getPreferenceScreen().removePreference(preference);
        }
    }
}
