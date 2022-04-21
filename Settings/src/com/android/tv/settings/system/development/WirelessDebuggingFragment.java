/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.tv.settings.system.development;

import android.content.Context;
import android.database.ContentObserver;
import android.debug.IAdbManager;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.tv.settings.R;
import com.android.tv.settings.RadioPreference;
import com.android.tv.settings.SettingsPreferenceFragment;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Iterator;

/**
 * Fragment shown when clicking in the "Wireless Debugging" preference in
 * the developer options.
 */
@Keep
public class WirelessDebuggingFragment extends SettingsPreferenceFragment {
    private static final String TAG = "WirelessDebuggingFrag";

    private static final String PREF_KEY_ADB_WIRELESS_SELECTION_OPTION =
            "adb_wireless_selection_option";
    private static final String PREF_KEY_ADB_WIRELESS_SELECTION_DISABLE =
            "adb_wireless_selection_disable";
    private static final String PREF_KEY_ADB_WIRELESS_SELECTION_ENABLE =
            "adb_wireless_selection_enable";
    private static final String PREF_KEY_ADB_CODE_PAIRING = "adb_pair_method_code_pref";
    private static final String PREF_KEY_ADB_DEVICE_NAME = "adb_device_name_pref";
    private static final String PREF_KEY_ADB_IP_ADDR = "adb_ip_addr_pref";
    private static final String PREF_KEY_PAIRED_DEVICES_CATEGORY = "adb_paired_devices_category";

    private IAdbManager mAdbManager;
    private ContentObserver mToggleContentObserver;
    private ConnectivityManager mConnectivityManager;

    private PreferenceCategory mAdbWirelessSelectionOption;
    private RadioPreference mAdbWirelessSelectionDisable;
    private RadioPreference mAdbWirelessSelectionEnable;
    private Preference mCodePairingPreference;
    private Preference mDeviceNamePreference;
    private Preference mIpAddrPreference;
    private PreferenceCategory mPairedDevicesCategory;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(getPreferenceScreenResId(), null);

        mAdbManager = IAdbManager.Stub.asInterface(ServiceManager.getService(
                Context.ADB_SERVICE));
        mToggleContentObserver = new ContentObserver(new Handler(Looper.myLooper())) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                updatePreferenceState();
            }
        };
        mConnectivityManager = getContext().getSystemService(ConnectivityManager.class);

        addPreferences();
        initAdbWirelessSelectionOptionPreference();
    }

    @Override
    public void onResume() {
        super.onResume();
        getContext().getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ADB_WIFI_ENABLED),
                false,
                mToggleContentObserver);
        updatePreferenceState();
    }

    @Override
    public void onPause() {
        super.onPause();
        getContext().getContentResolver().unregisterContentObserver(mToggleContentObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private int getPreferenceScreenResId() {
        return R.xml.adb_wireless_settings;
    }

    private void addPreferences() {
        mAdbWirelessSelectionOption = findPreference(PREF_KEY_ADB_WIRELESS_SELECTION_OPTION);
        mAdbWirelessSelectionDisable = findPreference(PREF_KEY_ADB_WIRELESS_SELECTION_DISABLE);
        mAdbWirelessSelectionEnable = findPreference(PREF_KEY_ADB_WIRELESS_SELECTION_ENABLE);
        mCodePairingPreference = findPreference(PREF_KEY_ADB_CODE_PAIRING);
        mDeviceNamePreference = findPreference(PREF_KEY_ADB_DEVICE_NAME);
        mIpAddrPreference = findPreference(PREF_KEY_ADB_IP_ADDR);
        mPairedDevicesCategory = findPreference(PREF_KEY_PAIRED_DEVICES_CATEGORY);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        switch (preference.getKey()) {
            case PREF_KEY_ADB_WIRELESS_SELECTION_ENABLE:
                setWirelessDebuggingRadioButtonEnabled(true);
                Settings.Global.putInt(getContext().getContentResolver(),
                        Settings.Global.ADB_WIFI_ENABLED,
                        1);
                break;
            case PREF_KEY_ADB_WIRELESS_SELECTION_DISABLE:
                setWirelessDebuggingRadioButtonEnabled(false);
                Settings.Global.putInt(getContext().getContentResolver(),
                        Settings.Global.ADB_WIFI_ENABLED,
                        0);
                disablePairing();
                break;
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void initAdbWirelessSelectionOptionPreference() {
        boolean enabled = Settings.Global.getInt(getContext().getContentResolver(),
                Settings.Global.ADB_WIFI_ENABLED, 1) != 0;
        setWirelessDebuggingRadioButtonEnabled(enabled);
    }

    private void updatePreferenceState() {
        if (!isNetworkConnected()) {
            showBlankPreferences();
        } else {
            boolean enabled = Settings.Global.getInt(getContext().getContentResolver(),
                    Settings.Global.ADB_WIFI_ENABLED, 1) != 0;
            if (enabled) {
                showDebuggingPreferences();
                try {
                    int connectionPort = mAdbManager.getAdbWirelessPort();
                    if (connectionPort > 0) {
                        Log.i(TAG, "onEnabled(): connect_port=" + connectionPort);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Unable to request the paired list for Adb wireless");
                }
                updateAdbIpAddressPreference();
            } else {
                showOffMessage();
            }
        }
    }

    private void showBlankPreferences() {
        if (mAdbWirelessSelectionOption != null) {
            mAdbWirelessSelectionOption.setVisible(false);
        }
        if (mCodePairingPreference != null) {
            mCodePairingPreference.setVisible(false);
        }
        if (mDeviceNamePreference != null) {
            mDeviceNamePreference.setVisible(false);
        }
        if (mIpAddrPreference != null) {
            mIpAddrPreference.setVisible(false);
        }
        if (mPairedDevicesCategory != null) {
            mPairedDevicesCategory.setVisible(false);
        }
    }

    private void showOffMessage() {
        setWirelessDebuggingRadioButtonEnabled(false);
        if (mAdbWirelessSelectionOption != null) {
            mAdbWirelessSelectionOption.setVisible(true);
        }
        if (mCodePairingPreference != null) {
            mCodePairingPreference.setVisible(false);
        }
        if (mDeviceNamePreference != null) {
            mDeviceNamePreference.setVisible(false);
        }
        if (mIpAddrPreference != null) {
            mIpAddrPreference.setVisible(false);
        }
        if (mPairedDevicesCategory != null) {
            mPairedDevicesCategory.setVisible(false);
        }
    }

    private void showDebuggingPreferences() {
        setWirelessDebuggingRadioButtonEnabled(true);
        if (mAdbWirelessSelectionOption != null) {
            mAdbWirelessSelectionOption.setVisible(true);
        }
        if (mCodePairingPreference != null) {
            mCodePairingPreference.setVisible(true);
        }
        if (mDeviceNamePreference != null) {
            mDeviceNamePreference.setSummary(getDeviceName());
            mDeviceNamePreference.setVisible(true);
        }
        if (mIpAddrPreference != null) {
            mIpAddrPreference.setVisible(true);
        }
        if (mPairedDevicesCategory != null) {
            mPairedDevicesCategory.setVisible(true);
        }
    }

    private void setWirelessDebuggingRadioButtonEnabled(boolean enabled) {
        if (mAdbWirelessSelectionEnable != null) {
            mAdbWirelessSelectionEnable.setChecked(enabled);
        }
        if (mAdbWirelessSelectionDisable != null) {
            mAdbWirelessSelectionDisable.setChecked(!enabled);
        }
    }

    private void updateAdbIpAddressPreference() {
        String ipAddress = getIpAddressPort();
        if (mIpAddrPreference != null) {
            mIpAddrPreference.setSummary(ipAddress);
        }
    }

    private void disablePairing() {
        try {
            mAdbManager.disablePairing();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to disable pairing");
        }
    }

    private String getDeviceName() {
        String deviceName = Settings.Global.getString(getContext().getContentResolver(),
                Settings.Global.DEVICE_NAME);
        if (deviceName == null) {
            deviceName = Build.MODEL;
        }
        return deviceName;
    }

    private boolean isNetworkConnected() {
        NetworkInfo activeNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private String getIpAddressPort() {
        String ipAddress = getWifiIpv4Address();
        if (ipAddress != null) {
            int port = getAdbWirelessPort();;
            if (port <= 0) {
                return getString(R.string.status_unavailable);
            } else {
                ipAddress += ":" + port;
            }
            return ipAddress;
        } else {
            return getString(R.string.status_unavailable);
        }
    }

    private int getAdbWirelessPort() {
        try {
            return mAdbManager.getAdbWirelessPort();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get the adb wifi port");
        }
        return 0;
    }

    private String getWifiIpv4Address() {
        LinkProperties prop = mConnectivityManager.getLinkProperties(
                mConnectivityManager.getActiveNetwork());
        return formatIpAddresses(prop);
    }

    private static String formatIpAddresses(LinkProperties prop) {
        if (prop == null) {
            return null;
        }

        Iterator<LinkAddress> iter = prop.getAllLinkAddresses().iterator();
        if (!iter.hasNext()) {
            return null;
        }

        StringBuilder addresses = new StringBuilder();
        while (iter.hasNext()) {
            InetAddress addr = iter.next().getAddress();
            if (addr instanceof Inet4Address) {
                addresses.append(addr.getHostAddress());
                break;
            }
        }
        return addresses.toString();
    }
}
