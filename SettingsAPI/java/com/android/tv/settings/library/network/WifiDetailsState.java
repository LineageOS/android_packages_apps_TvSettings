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

package com.android.tv.settings.library.network;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.IpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.android.tv.settings.library.ManagerUtil;
import com.android.tv.settings.library.PreferenceCompat;
import com.android.tv.settings.library.State;
import com.android.tv.settings.library.UIUpdateCallback;
import com.android.tv.settings.library.data.PreferenceCompatManager;
import com.android.tv.settings.library.util.ResourcesUtil;

import java.util.ArrayList;
import java.util.List;

public class WifiDetailsState implements State,
        ConnectivityListener.Listener, ConnectivityListener.WifiNetworkListener {
    private static final String TAG = "WifiDetailsState";

    private static final String ARG_ACCESS_POINT_STATE = "apBundle";
    private static final String KEY_CONNECTION_STATUS = "connection_status";
    private static final String KEY_IP_ADDRESS = "ip_address";
    private static final String KEY_MAC_ADDRESS = "mac_address";
    private static final String KEY_SIGNAL_STRENGTH = "signal_strength";
    private static final String KEY_RANDOM_MAC = "random_mac";
    private static final String KEY_PROXY_SETTINGS = "proxy_settings";
    private static final String KEY_IP_SETTINGS = "ip_settings";
    private static final String KEY_FORGET_NETWORK = "forget_network";
    private static final String VALUE_MAC_RANDOM = "random";
    private static final String VALUE_MAC_DEVICE = "device";
    private static final String INTENT_EDIT_PROXY_SETTINGS =
            "com.android.settings.wifi.action.EDIT_PROXY_SETTINGS";
    private static final String INTENT_IP_SETTINGS =
            "com.android.settings.wifi.action.EDIT_IP_SETTINGS";
    private static final String EXTRA_NETWORK_ID = "network_id";
    static final String INTENT_CONFIRMATION = "android.settings.ui.CONFIRM";
    static final String EXTRA_GUIDANCE_TITLE = "guidancetitle";
    static final String EXTRA_GUIDANCE_SUBTITLE = "guidanceSubtitle";
    static final String EXTRA_GUIDANCE_BREADCRUMB = "guidanceBreadcrumb";
    static final String EXTRA_GUIDANCE_ICON = "guidanceIcon";

    private static final int REQUEST_CODE_FORGET_NETWORK = 1;

    private final Context mContext;
    private final UIUpdateCallback mUIUpdateCallback;
    private NetworkModule mNetworkModule;
    private AccessPoint mAccessPoint;
    PreferenceCompatManager mPreferenceCompatManager;
    private PreferenceCompat mConnectionStatusPref;
    private PreferenceCompat mIpAddressPref;
    private PreferenceCompat mMacAddressPref;
    private PreferenceCompat mSignalStrengthPref;
    private PreferenceCompat mProxySettingsPref;
    private PreferenceCompat mIpSettingsPref;
    private PreferenceCompat mForgetNetworkPref;
    private PreferenceCompat mRandomMacPref;

    public static void prepareArgs(@NonNull Bundle args, AccessPoint accessPoint) {
        final Bundle apBundle = new Bundle();
        accessPoint.saveWifiState(apBundle);
        args.putParcelable(ARG_ACCESS_POINT_STATE, apBundle);
    }

    public WifiDetailsState(Context context, UIUpdateCallback uiUpdateCallback) {
        mUIUpdateCallback = uiUpdateCallback;
        mContext = context;
        mNetworkModule = NetworkModule.getInstance(mContext);
    }

    @Override
    public void onAttach() {
        // no-op
    }

    @Override
    public void onCreate(Bundle extras) {
        mNetworkModule = NetworkModule.getInstance(mContext);
        mPreferenceCompatManager = new PreferenceCompatManager();
        mAccessPoint = new AccessPoint(mContext, extras.getBundle(ARG_ACCESS_POINT_STATE));
        if (mUIUpdateCallback != null) {
            mUIUpdateCallback.notifyUpdateScreenTitle(getStateIdentifier(),
                    String.valueOf(mAccessPoint.getSsid()));
        }
        mConnectionStatusPref = mPreferenceCompatManager.getOrCreatePrefCompat(
                KEY_CONNECTION_STATUS);
        mIpAddressPref = mPreferenceCompatManager.getOrCreatePrefCompat(KEY_IP_ADDRESS);
        mMacAddressPref = mPreferenceCompatManager.getOrCreatePrefCompat(KEY_MAC_ADDRESS);
        mSignalStrengthPref = mPreferenceCompatManager.getOrCreatePrefCompat(
                KEY_SIGNAL_STRENGTH);
        mProxySettingsPref = mPreferenceCompatManager.getOrCreatePrefCompat(
                KEY_PROXY_SETTINGS);
        mIpSettingsPref = mPreferenceCompatManager.getOrCreatePrefCompat(KEY_IP_SETTINGS);
        mForgetNetworkPref = mPreferenceCompatManager.getOrCreatePrefCompat(
                KEY_FORGET_NETWORK);
        mRandomMacPref = mPreferenceCompatManager.getOrCreatePrefCompat(KEY_RANDOM_MAC);
        mRandomMacPref.setType(PreferenceCompat.TYPE_LIST);
    }

    @Override
    public void onStart() {
        mNetworkModule.addState(this);
    }

    @Override
    public void onResume() {
        update();
    }

    @Override
    public void onPause() {
        // no-op
    }

    @Override
    public void onStop() {
        mNetworkModule.getConnectivityListener().stop();
        mNetworkModule.removeState(this);
    }

    @Override
    public void onDestroy() {
        // no-op
    }

    @Override
    public void onDetach() {
        // no-op
    }

    @Override
    public boolean onPreferenceTreeClick(String[] key, boolean status) {
        if (KEY_FORGET_NETWORK.equals(key[0])) {
            Intent forgetConfirmIntent = new Intent(INTENT_CONFIRMATION)
                    .putExtra(EXTRA_GUIDANCE_TITLE,
                            ResourcesUtil.getString(mContext, "wifi_forget_network"))
                    .putExtra(EXTRA_GUIDANCE_SUBTITLE, ResourcesUtil.getString(mContext,
                            "wifi_forget_network_description", mAccessPoint.getSsidStr()));
            ((Activity) mContext).startActivityForResult(forgetConfirmIntent,
                    ManagerUtil.calculateCompoundCode(getStateIdentifier(),
                            REQUEST_CODE_FORGET_NETWORK));
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_FORGET_NETWORK) {
            if (resultCode == Activity.RESULT_OK) {
                WifiManager wifiManager = mContext.getSystemService(WifiManager.class);
                wifiManager.forget(mAccessPoint.getConfig().networkId, null);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(String[] key, Object newValue) {
        mNetworkModule.getConnectivityListener().applyMacRandomizationSetting(
                mAccessPoint,
                VALUE_MAC_RANDOM.equals(newValue));
        return true;
    }

    @Override
    public int getStateIdentifier() {
        return ManagerUtil.STATE_WIFI_DETAILS;
    }

    @Override
    public void onConnectivityChange() {
        update();
    }

    @Override
    public void onWifiListChanged() {
        final List<AccessPoint> accessPoints = mNetworkModule
                .getConnectivityListener().getAvailableNetworks();
        for (final AccessPoint accessPoint : accessPoints) {
            if (TextUtils.equals(mAccessPoint.getSsidStr(), accessPoint.getSsidStr())
                    && mAccessPoint.getSecurity() == accessPoint.getSecurity()) {
                // Make sure we're not holding on to the one we inflated from the bundle, because
                // it won't be updated
                mAccessPoint = accessPoint;
                break;
            }
        }
        update();
    }

    private void update() {
        List<PreferenceCompat> preferenceCompats = new ArrayList<>();
        if (mAccessPoint == null) {
            return;
        }
        final boolean active = mAccessPoint.isActive();

        mConnectionStatusPref.setSummary(active
                ? ResourcesUtil.getString(mContext, "connected")
                : ResourcesUtil.getString(mContext, "not_connected"));
        mIpAddressPref.setVisible(active);
        mSignalStrengthPref.setVisible(active);
        preferenceCompats.add(mConnectionStatusPref);
        preferenceCompats.add(mIpAddressPref);
        preferenceCompats.add(mSignalStrengthPref);
        preferenceCompats.add(mMacAddressPref);
        preferenceCompats.add(mProxySettingsPref);
        preferenceCompats.add(mIpSettingsPref);
        preferenceCompats.add(mForgetNetworkPref);

        if (active) {
            mIpAddressPref.setSummary(mNetworkModule.getConnectivityListener().getWifiIpAddress());
            mSignalStrengthPref.setSummary(getSignalStrength());
        }

        // Mac address related Preferences (info entry and random mac setting entry)
        String macAddress = mNetworkModule.getConnectivityListener()
                .getWifiMacAddress(mAccessPoint);
        if (active && !TextUtils.isEmpty(macAddress)) {
            mMacAddressPref.setVisible(true);
            updateMacAddressPref(macAddress);
            updateRandomMacPref();
        } else {
            mMacAddressPref.setVisible(false);
            mRandomMacPref.setVisible(false);
        }

        WifiConfiguration wifiConfiguration = mAccessPoint.getConfig();
        if (wifiConfiguration != null) {
            final int networkId = wifiConfiguration.networkId;
            IpConfiguration.ProxySettings proxySettings =
                    wifiConfiguration.getIpConfiguration().getProxySettings();
            mProxySettingsPref.setSummary(proxySettings == IpConfiguration.ProxySettings.NONE
                    ? ResourcesUtil.getString(mContext, "wifi_action_proxy_none")
                    : ResourcesUtil.getString(mContext, "wifi_action_proxy_manual"));
            mProxySettingsPref.setIntent(new Intent(INTENT_EDIT_PROXY_SETTINGS)
                    .putExtra(EXTRA_NETWORK_ID, networkId));
            IpConfiguration.IpAssignment ipAssignment =
                    wifiConfiguration.getIpConfiguration().getIpAssignment();
            mIpSettingsPref.setSummary(ipAssignment == IpConfiguration.IpAssignment.STATIC
                    ? ResourcesUtil.getString(mContext, "wifi_action_static")
                    : ResourcesUtil.getString(mContext, "wifi_action_dhcp"));
            mIpSettingsPref.setIntent(new Intent(INTENT_IP_SETTINGS)
                    .putExtra(EXTRA_NETWORK_ID, networkId));
        }

        mProxySettingsPref.setVisible(wifiConfiguration != null);
        mIpSettingsPref.setVisible(wifiConfiguration != null);
        mForgetNetworkPref.setVisible(wifiConfiguration != null);
        if (mUIUpdateCallback != null) {
            mUIUpdateCallback.notifyUpdateAll(getStateIdentifier(), preferenceCompats);
        }
    }

    private void updateMacAddressPref(String macAddress) {
        if (WifiInfo.DEFAULT_MAC_ADDRESS.equals(macAddress)) {
            mMacAddressPref.setSummary(
                    ResourcesUtil.getString(mContext, "mac_address_not_available"));
        } else {
            mMacAddressPref.setSummary(macAddress);
        }
        if (mAccessPoint == null || mAccessPoint.getConfig() == null) {
            return;
        }
        // For saved Passpoint network, framework doesn't have the field to keep the MAC choice
        // persistently, so Passpoint network will always use the default value so far, which is
        // randomized MAC address, so don't need to modify title.
        if (mAccessPoint.isPasspoint() || mAccessPoint.isPasspointConfig()) {
            return;
        }
        mMacAddressPref.setTitle(
                (mAccessPoint.getConfig().macRandomizationSetting
                        == WifiConfiguration.RANDOMIZATION_PERSISTENT)
                        ? ResourcesUtil.getString(mContext, "title_randomized_mac_address")
                        : ResourcesUtil.getString(mContext, "title_mac_address"));
    }

    private void updateRandomMacPref() {
        ConnectivityListener connectivityListener = mNetworkModule.getConnectivityListener();
        mRandomMacPref.setVisible(connectivityListener.isMacAddressRandomizationSupported());
        boolean isMacRandomized =
                (connectivityListener.getWifiMacRandomizationSetting(mAccessPoint)
                        == WifiConfiguration.RANDOMIZATION_PERSISTENT);
        mRandomMacPref.setValue(isMacRandomized ? VALUE_MAC_RANDOM : VALUE_MAC_DEVICE);
        if (mAccessPoint.isEphemeral() || mAccessPoint.isPasspoint()
                || mAccessPoint.isPasspointConfig()) {
            mRandomMacPref.setSelectable(PreferenceCompat.STATUS_OFF);
            mRandomMacPref.setSummary(ResourcesUtil.getString(
                    mContext, "mac_address_ephemeral_summary"));
        } else {
            mRandomMacPref.setSelectable(PreferenceCompat.STATUS_ON);
            String[] entries = ResourcesUtil.getStringArray(
                    mContext, "random_mac_settings_entries");
            mRandomMacPref.setHasOnPreferenceChangeListener(true);
            mRandomMacPref.setSummary(entries[isMacRandomized ? 0 : 1]);
        }
    }

    private String getSignalStrength() {
        String[] signalLevels = ResourcesUtil
                .getStringArray(mContext, "wifi_signal_strength");
        if (signalLevels != null) {
            int strength = mNetworkModule.getConnectivityListener()
                    .getWifiSignalStrength(signalLevels.length);
            return signalLevels[strength];
        }
        return "";
    }
}
