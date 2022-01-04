/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.app.Activity;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.debug.AdbManager;
import android.debug.IAdbManager;
import android.debug.PairDevice;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.android.settingslib.widget.FooterPreference;
import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Fragment shown when clicking in the "Wireless Debugging" preference in
 * the developer options.
 */
@Keep
public class WirelessDebuggingFragment extends SettingsPreferenceFragment
        implements WirelessDebuggingEnabler.OnEnabledListener {

    private static final String TAG = "WirelessDebuggingFrag";

    // Activity result from clicking on a paired device.
    private static final int PAIRED_DEVICE_REQUEST = 0;
    public static final String PAIRED_DEVICE_REQUEST_TYPE = "request_type";
    public static final int FORGET_ACTION = 0;

    // Activity result from pairing a device.
    private static final int PAIRING_DEVICE_REQUEST = 1;
    public static final String PAIRING_DEVICE_REQUEST_TYPE = "request_type_pairing";
    public static final int SUCCESS_ACTION = 0;
    public static final int FAIL_ACTION = 1;

    public static final String PAIRED_DEVICE_EXTRA = "paired_device";
    public static final String DEVICE_NAME_EXTRA = "device_name";
    public static final String IP_ADDR_EXTRA = "ip_addr";

    private WirelessDebuggingEnabler mWifiDebuggingEnabler;

    // UI components
    private static final String PREF_KEY_ADB_NETWORK_SWITCH = "adb_root_enable";
    private static final String PREF_KEY_ADB_DEVICE_NAME = "adb_device_name_pref";
    private static final String PREF_KEY_ADB_IP_ADDR = "adb_ip_addr_pref";
    private static final String PREF_KEY_PAIRING_METHODS_CATEGORY = "adb_pairing_methods_category";
    private static final String PREF_KEY_ADB_CODE_PAIRING = "adb_pair_method_code_pref";
    private static final String PREF_KEY_PAIRED_DEVICES_CATEGORY = "adb_paired_devices_category";
    private static final String PREF_KEY_FOOTER_CATEGORY = "adb_wireless_footer_category";

    private SwitchPreference mSwitchWidget;

    private Preference mDeviceNamePreference;
    private Preference mIpAddrPreference;

    private PreferenceCategory mPairingMethodsCategory;
    private Preference mCodePairingPreference;

    private PreferenceCategory mPairedDevicesCategory;

    private PreferenceCategory mFooterCategory;
    private FooterPreference mOffMessagePreference;

    // Map of paired devices, with the device GUID is the key
    private Map<String, AdbPairedDevicePreference> mPairedDevicePreferences;

    private int mConnectionPort;

    // AdbIpAddressPreferenceController
    private static final String PREF_ADB_IP_KEY = "adb_ip_addr_pref";
    private Preference mAdbIpAddrPref;
    private ConnectivityManager mCM;
    private IAdbManager mAdbManager;

    private static final String[] CONNECTIVITY_INTENTS = {
            ConnectivityManager.CONNECTIVITY_ACTION,
            WifiManager.ACTION_LINK_CONFIGURATION_CHANGED,
            WifiManager.NETWORK_STATE_CHANGED_ACTION,
    };

    class PairingCodeDialogListener implements AdbWirelessDialog.AdbWirelessDialogListener {
        @Override
        public void onDismiss() {
            Log.i(TAG, "onDismiss");
            mPairingCodeDialog = null;
            try {
                mAdbManager.disablePairing();
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to cancel pairing");
            }
        }
    }
    final PairingCodeDialogListener mPairingCodeDialogListener = new PairingCodeDialogListener();
    AdbWirelessDialog mPairingCodeDialog;

    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AdbManager.WIRELESS_DEBUG_PAIRED_DEVICES_ACTION.equals(action)) {
                Map<String, PairDevice> newPairedDevicesList =
                        (HashMap<String, PairDevice>) intent.getSerializableExtra(
                            AdbManager.WIRELESS_DEVICES_EXTRA);
                updatePairedDevicePreferences(newPairedDevicesList);
            } else if (AdbManager.WIRELESS_DEBUG_STATE_CHANGED_ACTION.equals(action)) {
                int status = intent.getIntExtra(AdbManager.WIRELESS_STATUS_EXTRA,
                        AdbManager.WIRELESS_STATUS_DISCONNECTED);
                if (status == AdbManager.WIRELESS_STATUS_CONNECTED) {
                    int port = intent.getIntExtra(AdbManager.WIRELESS_DEBUG_PORT_EXTRA, 0);
                    Log.i(TAG, "Got adbwifi port=" + port);
                } else {
                    Log.i(TAG, "adbwifi server disconnected");
                }
            } else if (AdbManager.WIRELESS_DEBUG_PAIRING_RESULT_ACTION.equals(action)) {
                Integer res = intent.getIntExtra(
                        AdbManager.WIRELESS_STATUS_EXTRA,
                        AdbManager.WIRELESS_STATUS_FAIL);

                if (res.equals(AdbManager.WIRELESS_STATUS_PAIRING_CODE)) {
                    String pairingCode = intent.getStringExtra(
                                AdbManager.WIRELESS_PAIRING_CODE_EXTRA);
                    if (mPairingCodeDialog != null) {
                        mPairingCodeDialog.getController().setPairingCode(pairingCode);
                    }
                } else if (res.equals(AdbManager.WIRELESS_STATUS_SUCCESS)) {
                    getActivity().removeDialog(AdbWirelessDialogUiBase.MODE_PAIRING);
                    mPairingCodeDialog = null;
                } else if (res.equals(AdbManager.WIRELESS_STATUS_FAIL)) {
                    getActivity().removeDialog(AdbWirelessDialogUiBase.MODE_PAIRING);
                    mPairingCodeDialog = null;
                    createDialog(AdbWirelessDialogUiBase.MODE_PAIRING_FAILED);
                } else if (res.equals(AdbManager.WIRELESS_STATUS_CONNECTED)) {
                    int port = intent.getIntExtra(AdbManager.WIRELESS_DEBUG_PORT_EXTRA, 0);
                    Log.i(TAG, "Got pairing code port=" + port);
                    String ipAddr = getIpv4Address() + ":" + port;
                    if (mPairingCodeDialog != null) {
                        mPairingCodeDialog.getController().setIpAddr(ipAddr);
                    }
                }
            }
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mWifiDebuggingEnabler = new WirelessDebuggingEnabler(getActivity(),
                mSwitchWidget, this, getLifecycle());

        mSwitchWidget.setChecked(mWifiDebuggingEnablerisAdbWifiEnabled());

        updateConnectivity();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferences();
        mIntentFilter = new IntentFilter(AdbManager.WIRELESS_DEBUG_PAIRED_DEVICES_ACTION);
        mIntentFilter.addAction(AdbManager.WIRELESS_DEBUG_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(AdbManager.WIRELESS_DEBUG_PAIRING_RESULT_ACTION);

        // AdbIpAddressPreferenceController
        mCM = getContext().getSystemService(ConnectivityManager.class);
        mAdbManager = IAdbManager.Stub.asInterface(ServiceManager.getService(
                Context.ADB_SERVICE));
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        setPreferencesFromResource(R.xml.adb_wireless_settings, null);
    }

    private void addPreferences() {
        mSwitchWidget = (SwitchPreference) findPreference(PREF_KEY_ADB_NETWORK_SWITCH);
        mDeviceNamePreference = (Preference) findPreference(PREF_KEY_ADB_DEVICE_NAME);
        mIpAddrPreference = (Preference) findPreference(PREF_KEY_ADB_IP_ADDR);
        mPairingMethodsCategory =
                (PreferenceCategory) findPreference(PREF_KEY_PAIRING_METHODS_CATEGORY);
        mCodePairingPreference = (Preference) findPreference(PREF_KEY_ADB_CODE_PAIRING);
        mCodePairingPreference.setOnPreferenceClickListener(preference -> {
            createDialog(AdbWirelessDialogUiBase.MODE_PAIRING);
            return true;
        });

        mPairedDevicesCategory =
                (PreferenceCategory) findPreference(PREF_KEY_PAIRED_DEVICES_CATEGORY);
        mFooterCategory = (PreferenceCategory) findPreference(PREF_KEY_FOOTER_CATEGORY);

        mOffMessagePreference =
                new FooterPreference(mFooterCategory.getContext());
        final CharSequence title = getText(R.string.adb_wireless_list_empty_off);
        mOffMessagePreference.setTitle(title);
        mFooterCategory.addPreference(mOffMessagePreference);

        // AdbIpAddressPreferenceController
        mAdbIpAddrPref = findPreference(PREF_ADB_IP_KEY);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mWifiDebuggingEnabler.teardownSwitchController();
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();

        //TODO removeDialog(AdbWirelessDialogUiBase.MODE_PAIRING);
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PAIRED_DEVICE_REQUEST) {
            handlePairedDeviceRequest(resultCode, data);
        } else if (requestCode == PAIRING_DEVICE_REQUEST) {
            handlePairingDeviceRequest(resultCode, data);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_ADB_WIRELESS;
    }

    private void createDialog(int dialogId) {
        Dialog d = AdbWirelessDialog.createModal(getActivity(),
                dialogId == AdbWirelessDialogUiBase.MODE_PAIRING
                    ? mPairingCodeDialogListener : null, dialogId);
        if (dialogId == AdbWirelessDialogUiBase.MODE_PAIRING) {
            mPairingCodeDialog = (AdbWirelessDialog) d;
            try {
                mAdbManager.enablePairingByPairingCode();
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to enable pairing");
                mPairingCodeDialog = null;
                d = AdbWirelessDialog.createModal(getActivity(), null,
                        AdbWirelessDialogUiBase.MODE_PAIRING_FAILED);
            }
        }

        if (d != null) {
            d.show();
        }
    }

/*
    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.adb_wireless_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }
*/

    @Override
    public void onEnabled(boolean enabled) {
        Log.i("WirelessDebuggingEnabler", "onEnabled : " + Boolean.toString(enabled));
        if (enabled) {
            showDebuggingPreferences();
            mAdbManager = IAdbManager.Stub.asInterface(ServiceManager.getService(
                    Context.ADB_SERVICE));
            try {
                Map<String, PairDevice> newList = mAdbManager.getPairedDevices();
                updatePairedDevicePreferences(newList);
                mConnectionPort = mAdbManager.getAdbWirelessPort();
                if (mConnectionPort > 0) {
                    Log.i(TAG, "onEnabled(): connect_port=" + mConnectionPort);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to request the paired list for Adb wireless");
            }
            updateConnectivity();
        } else {
            showOffMessage();
        }
    }

    private void showOffMessage() {
        mDeviceNamePreference.setVisible(false);
        mIpAddrPreference.setVisible(false);
        mPairingMethodsCategory.setVisible(false);
        mPairedDevicesCategory.setVisible(false);
        mFooterCategory.setVisible(true);
    }

    private void showDebuggingPreferences() {
        mDeviceNamePreference.setVisible(true);
        mIpAddrPreference.setVisible(true);
        mPairingMethodsCategory.setVisible(true);
        mPairedDevicesCategory.setVisible(true);
        mFooterCategory.setVisible(false);
    }

    private void updatePairedDevicePreferences(Map<String, PairDevice> newList) {
        // TODO(joshuaduong): Move the non-UI stuff into another thread
        // as the processing could take some time.
        if (newList == null) {
            mPairedDevicesCategory.removeAll();
            return;
        }
        if (mPairedDevicePreferences == null) {
            mPairedDevicePreferences = new HashMap<String, AdbPairedDevicePreference>();
        }
        if (mPairedDevicePreferences.isEmpty()) {
            for (Map.Entry<String, PairDevice> entry : newList.entrySet()) {
                AdbPairedDevicePreference p =
                        new AdbPairedDevicePreference(entry.getValue(),
                            mPairedDevicesCategory.getContext());
                mPairedDevicePreferences.put(
                        entry.getKey(),
                        p);
                p.setOnPreferenceClickListener(preference -> {
                    AdbPairedDevicePreference pref =
                            (AdbPairedDevicePreference) preference;
                    //launchPairedDeviceDetailsFragment(pref);
                    return true;
                });
                mPairedDevicesCategory.addPreference(p);
            }
        } else {
            // Remove any devices no longer on the newList
            mPairedDevicePreferences.entrySet().removeIf(entry -> {
                if (newList.get(entry.getKey()) == null) {
                    mPairedDevicesCategory.removePreference(entry.getValue());
                    return true;
                } else {
                    // It is in the newList. Just update the PairDevice value
                    AdbPairedDevicePreference p =
                            entry.getValue();
                    p.setPairedDevice(newList.get(entry.getKey()));
                    p.refresh();
                    return false;
                }
            });
            // Add new devices if any.
            for (Map.Entry<String, PairDevice> entry :
                    newList.entrySet()) {
                if (mPairedDevicePreferences.get(entry.getKey()) == null) {
                    AdbPairedDevicePreference p =
                            new AdbPairedDevicePreference(entry.getValue(),
                                mPairedDevicesCategory.getContext());
                    mPairedDevicePreferences.put(
                            entry.getKey(),
                            p);
                    p.setOnPreferenceClickListener(preference -> {
                        AdbPairedDevicePreference pref =
                                (AdbPairedDevicePreference) preference;
                        //launchPairedDeviceDetailsFragment(pref);
                        return true;
                    });
                    mPairedDevicesCategory.addPreference(p);
                }
            }
        }
    }

    /*private void launchPairedDeviceDetailsFragment(AdbPairedDevicePreference p) {
        // For sending to the device details fragment.
        p.savePairedDeviceToExtras(p.getExtras());
        new SubSettingLauncher(getContext())
                .setTitleRes(R.string.adb_wireless_device_details_title)
                .setDestination(AdbDeviceDetailsFragment.class.getName())
                .setArguments(p.getExtras())
                .setSourceMetricsCategory(getMetricsCategory())
                .setResultListener(this, PAIRED_DEVICE_REQUEST)
                .launch();
    }*/

    void handlePairedDeviceRequest(int result, Intent data) {
        if (result != Activity.RESULT_OK) {
            return;
        }

        Log.i(TAG, "Processing paired device request");
        int requestType = data.getIntExtra(PAIRED_DEVICE_REQUEST_TYPE, -1);

        PairDevice p;

        switch (requestType) {
            case FORGET_ACTION:
                try {
                    p = (PairDevice) data.getParcelableExtra(PAIRED_DEVICE_EXTRA);
                    mAdbManager.unpairDevice(p.getGuid());
                } catch (RemoteException e) {
                    Log.e(TAG, "Unable to forget the device");
                }
                break;
            default:
                break;
        }
    }

    void handlePairingDeviceRequest(int result, Intent data) {
        if (result != Activity.RESULT_OK) {
            return;
        }

        int requestType = data.getIntExtra(PAIRING_DEVICE_REQUEST_TYPE, -1);
        switch (requestType) {
            case FAIL_ACTION:
                createDialog(AdbWirelessDialogUiBase.MODE_PAIRING_FAILED);
                break;
            default:
                break;
        }
    }

    private String getDeviceName() {
        // Keep device name in sync with Settings > About phone > Device name
        String deviceName = Settings.Global.getString(getContext().getContentResolver(),
                Settings.Global.DEVICE_NAME);
        if (deviceName == null) {
            deviceName = Build.MODEL;
        }
        return deviceName;
    }

    // AdbIpAddressPreferenceController
    private void updateConnectivity() {
        String ipAddress = getDefaultIpAddresses(mCM);
        if (ipAddress != null) {
            int port = getPort();
            if (port <= 0) {
                mAdbIpAddrPref.setSummary(R.string.status_unavailable);
            } else {
                ipAddress += ":" + port;
            }
            mAdbIpAddrPref.setSummary(ipAddress);
        } else {
            mAdbIpAddrPref.setSummary(R.string.status_unavailable);
        }
    }

    private int getPort() {
        try {
            return mAdbManager.getAdbWirelessPort();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get the adbwifi port");
        }
        return 0;
    }

    private String getIpv4Address() {
        return getDefaultIpAddresses(mCM);
    }

    /**
     * Returns the default link's IP addresses, if any, taking into account IPv4 and IPv6 style
     * addresses.
     * @param cm ConnectivityManager
     * @return the formatted and newline-separated IP addresses, or null if none.
     */
    private static String getDefaultIpAddresses(ConnectivityManager cm) {
        LinkProperties prop = cm.getActiveLinkProperties();
        return formatIpAddresses(prop);
    }

    private static String formatIpAddresses(LinkProperties prop) {
        if (prop == null) {
            return null;
        }

        Iterator<InetAddress> iter = prop.getAllAddresses().iterator();
        // If there are no entries, return null
        if (!iter.hasNext()) {
            return null;
        }

        // Concatenate all available addresses, newline separated
        StringBuilder addresses = new StringBuilder();
        while (iter.hasNext()) {
            InetAddress addr = iter.next();
            if (addr instanceof Inet4Address) {
                // adb only supports ipv4 at the moment
                addresses.append(addr.getHostAddress());
                break;
            }
        }
        return addresses.toString();
    }
}
