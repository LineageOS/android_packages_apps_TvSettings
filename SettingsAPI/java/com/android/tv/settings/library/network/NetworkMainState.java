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

import static com.android.tv.settings.library.ManagerUtil.INFO_COLLAPSE;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;

import com.android.tv.settings.library.ManagerUtil;
import com.android.tv.settings.library.PreferenceCompat;
import com.android.tv.settings.library.UIUpdateCallback;
import com.android.tv.settings.library.data.PreferenceCompatManager;
import com.android.tv.settings.library.data.State;
import com.android.tv.settings.library.util.ResourcesUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** State to provide data for rendering NetworkFragment. */
public class NetworkMainState implements State, AccessPoint.AccessPointListener,
        ConnectivityListener.WifiNetworkListener, ConnectivityListener.Listener {
    private static final String TAG = "NetworkMainState";
    private static final boolean DEBUG = true;
    private static final String KEY_WIFI_ENABLE = "wifi_enable";
    private static final String KEY_WIFI_LIST = "wifi_list";
    private static final String KEY_WIFI_COLLAPSE = "wifi_collapse";
    private static final String KEY_WIFI_OTHER = "wifi_other";
    private static final String KEY_WIFI_ADD = "wifi_add";
    private static final String KEY_WIFI_ALWAYS_SCAN = "wifi_always_scan";
    private static final String KEY_ETHERNET = "ethernet";
    private static final String KEY_ETHERNET_STATUS = "ethernet_status";
    private static final String KEY_ETHERNET_PROXY = "ethernet_proxy";
    private static final String KEY_ETHERNET_DHCP = "ethernet_dhcp";
    private static final String KEY_DATA_SAVER_SLICE = "data_saver_slice";
    private static final String KEY_DATA_ALERT_SLICE = "data_alert_slice";
    private static final String KEY_NETWORK_DIAGNOSTICS = "network_diagnostics";
    private static final String EXTRA_WIFI_SSID = "wifi_ssid";
    private static final String EXTRA_WIFI_SECURITY_NAME = "wifi_security_name";
    private static final int INITIAL_UPDATE_DELAY = 500;

    private PreferenceCompat mEnableWifiPref;
    private PreferenceCompat mCollapsePref;
    private PreferenceCompat mAddPref;
    private PreferenceCompat mEthernetCategory;
    private PreferenceCompat mEthernetStatusPref;
    private PreferenceCompat mEthernetProxyPref;
    private PreferenceCompat mAlwaysScan;
    private PreferenceCompat mWifiNetworkCategoryPref;
    private PreferenceCompatManager mPreferenceCompatManager;
    private NetworkModule mNetworkModule;
    private final Context mContext;
    private final UIUpdateCallback mUIUpdateCallback;
    private final Handler mHandler = new Handler();
    private long mNoWifiUpdateBeforeMillis;
    private final Runnable mInitialUpdateWifiListRunnable = new Runnable() {
        @Override
        public void run() {
            mNoWifiUpdateBeforeMillis = 0;
            updateWifiList();
        }
    };

    public NetworkMainState(Context context, UIUpdateCallback callback) {
        mUIUpdateCallback = callback;
        mContext = context;
    }

    @Override
    public void onCreate(Bundle extras) {
        mNetworkModule = NetworkModule.getInstance(mContext);
        mPreferenceCompatManager = new PreferenceCompatManager();
        mEnableWifiPref = mPreferenceCompatManager.getOrCreatePrefCompat(KEY_WIFI_ENABLE);
        mAlwaysScan = mPreferenceCompatManager.getOrCreatePrefCompat(KEY_WIFI_ALWAYS_SCAN);
        mCollapsePref = mPreferenceCompatManager.getOrCreatePrefCompat(KEY_WIFI_COLLAPSE);
        mCollapsePref.addInfo(INFO_COLLAPSE, "true");
        mAddPref = mPreferenceCompatManager.getOrCreatePrefCompat(KEY_WIFI_ADD);
        mEthernetCategory = mPreferenceCompatManager.getOrCreatePrefCompat(KEY_ETHERNET);
        mEthernetStatusPref = mPreferenceCompatManager.getOrCreatePrefCompat(
                KEY_ETHERNET_STATUS);
        mEthernetProxyPref = mPreferenceCompatManager.getOrCreatePrefCompat(
                KEY_ETHERNET_PROXY);
        mWifiNetworkCategoryPref = mPreferenceCompatManager.getOrCreatePrefCompat(
                KEY_WIFI_LIST);
        mWifiNetworkCategoryPref.addInfo(INFO_COLLAPSE, "true");
        mWifiNetworkCategoryPref.setType(
                PreferenceCompat.TYPE_PREFERENCE_WIFI_COLLAPSE_CATEGORY);
    }

    @Override
    public void onStart() {
        mNetworkModule.addState(this);
        mNetworkModule.getConnectivityListener().setWifiListener(this);
        mNoWifiUpdateBeforeMillis = SystemClock.elapsedRealtime() + INITIAL_UPDATE_DELAY;
        mNetworkModule.getConnectivityListener().start();
        updateWifiList();
    }

    @Override
    public void onResume() {
        updateConnectivity();
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onStop() {
        mNetworkModule.getConnectivityListener().stop();
        mNetworkModule.removeState(this);
    }

    @Override
    public void onDestroy() {
    }

    private void updateWifiList() {
        if (!mNetworkModule.isWifiHardwarePresent() ||
                !mNetworkModule.getConnectivityListener().isWifiEnabledOrEnabling()) {
            mNoWifiUpdateBeforeMillis = 0;
            return;
        }

        final long now = SystemClock.elapsedRealtime();
        if (mNoWifiUpdateBeforeMillis > now) {
            mHandler.removeCallbacks(mInitialUpdateWifiListRunnable);
            mHandler.postDelayed(mInitialUpdateWifiListRunnable,
                    mNoWifiUpdateBeforeMillis - now);
            return;
        }

        final Collection<AccessPoint> accessPoints =
                mNetworkModule.getConnectivityListener().getAvailableNetworks();
        mWifiNetworkCategoryPref.initChildPreferences();
        for (final AccessPoint accessPoint : accessPoints) {
            accessPoint.setListener(this);
            PreferenceCompat accessPointPref = new PreferenceCompat(
                    new String[]{KEY_WIFI_LIST, accessPoint.getKey()});
            accessPointPref.setTitle(accessPoint.getTitle());
            accessPointPref.setType(PreferenceCompat.TYPE_PREFERENCE_ACCESS_POINT);
            accessPointPref.addInfo(ManagerUtil.INFO_WIFI_SIGNAL_LEVEL,
                    String.valueOf(accessPoint.getLevel()));
            if (accessPoint.isActive() && !isCaptivePortal(accessPoint)) {
                Bundle apBundle = new Bundle();
                accessPoint.saveWifiState(apBundle);
                accessPointPref.setExtras(apBundle);
                accessPointPref.addInfo(ManagerUtil.INFO_NEXT_STATE, String.valueOf(
                        ManagerUtil.STATE_WIFI_DETAILS));
                accessPointPref.setIntent(null);
            } else {
                Intent i = new Intent("com.android.settings.wifi.action.WIFI_CONNECTION_SETTINGS")
                        .putExtra(EXTRA_WIFI_SSID, accessPoint.getSsidStr())
                        .putExtra(EXTRA_WIFI_SECURITY_NAME, accessPoint.getSecurity());
                accessPointPref.setIntent(i);
            }
            mWifiNetworkCategoryPref.addChildPrefCompat(accessPointPref);
        }
        mNetworkModule.setAccessPoints(mWifiNetworkCategoryPref.getChildPrefCompats());
        if (mUIUpdateCallback != null) {
            mUIUpdateCallback.notifyUpdate(getStateIdentifier(), mWifiNetworkCategoryPref);
        }
    }

    @Override
    public void onPreferenceTreeClick(String key, boolean status) {
        switch (key) {
            case KEY_WIFI_ENABLE:
                mNetworkModule.getConnectivityListener().setWifiEnabled(status);
                mEnableWifiPref.setChecked(status);
                break;
            case KEY_WIFI_COLLAPSE:
                boolean collapse = !("true".equals(
                        mWifiNetworkCategoryPref.getInfo(ManagerUtil.INFO_COLLAPSE)));
                mWifiNetworkCategoryPref.addInfo(ManagerUtil.INFO_COLLAPSE,
                        String.valueOf(collapse));
                mCollapsePref.addInfo(ManagerUtil.INFO_COLLAPSE, String.valueOf(collapse));
                if (mUIUpdateCallback != null) {
                    mUIUpdateCallback.notifyUpdate(getStateIdentifier(), mWifiNetworkCategoryPref);
                }
                break;
            case KEY_WIFI_ALWAYS_SCAN:
                mAlwaysScan.setChecked(status);
                Settings.Global.putInt(mContext.getContentResolver(),
                        Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE,
                        status ? 1 : 0);
                break;
            case KEY_ETHERNET_STATUS:
            case KEY_WIFI_ADD:
            case KEY_ETHERNET_DHCP:
            case KEY_ETHERNET_PROXY:
                break;
            default:
                // no-op
        }
        if (mUIUpdateCallback != null) {
            mUIUpdateCallback.notifyUpdate(getStateIdentifier(),
                    mPreferenceCompatManager.getOrCreatePrefCompat(key));
        }
    }

    @Override
    public void onPreferenceChange(String key, Object newValue) {
        // no-op
    }


    private void updateConnectivity() {
        List<PreferenceCompat> preferenceCompats = new ArrayList<>();
        final boolean wifiEnabled = mNetworkModule.isWifiHardwarePresent()
                && mNetworkModule.getConnectivityListener().isWifiEnabledOrEnabling();
        mEnableWifiPref.setChecked(wifiEnabled);
        preferenceCompats.add(mEnableWifiPref);

        mWifiNetworkCategoryPref.setVisible(wifiEnabled);
        preferenceCompats.add(mWifiNetworkCategoryPref);

        mCollapsePref.setVisible(wifiEnabled);
        preferenceCompats.add(mCollapsePref);

        mAddPref.setVisible(wifiEnabled);
        preferenceCompats.add(mAddPref);


        if (!wifiEnabled) {
            updateWifiList();
        }

        int scanAlwaysAvailable = 0;
        try {
            scanAlwaysAvailable = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE);
        } catch (Settings.SettingNotFoundException e) {
            // Ignore
        }

        mAlwaysScan.setChecked(scanAlwaysAvailable == 1);
        mAlwaysScan.setContentDescription(
                ResourcesUtil.getString(mContext, "wifi_setting_always_scan_content_description"));

        final boolean ethernetAvailable =
                mNetworkModule.getConnectivityListener().isEthernetAvailable();
        mEthernetCategory.setVisible(ethernetAvailable);
        mEthernetStatusPref.setVisible(ethernetAvailable);
        mEthernetProxyPref.setVisible(ethernetAvailable);
        preferenceCompats.add(mEthernetCategory);
        preferenceCompats.add(mEthernetStatusPref);
        preferenceCompats.add(mEthernetProxyPref);

        if (ethernetAvailable) {
            final boolean ethernetConnected =
                    mNetworkModule.getConnectivityListener().isEthernetConnected();
            mEthernetStatusPref.setTitle(ethernetConnected
                    ? ResourcesUtil.getString(mContext, "connected")
                    : ResourcesUtil.getString(mContext, "not_connected"));
            mEthernetStatusPref.setSummary(
                    mNetworkModule.getConnectivityListener().getEthernetIpAddress());
        }

        if (mUIUpdateCallback != null) {
            mUIUpdateCallback.notifyUpdateAll(getStateIdentifier(), preferenceCompats);
        }
    }

    @Override
    public void onAccessPointChanged(AccessPoint accessPoint) {
        PreferenceCompat accessPointPref = new PreferenceCompat(
                new String[]{KEY_WIFI_LIST, accessPoint.getKey()});
        if (mUIUpdateCallback != null) {
            mUIUpdateCallback.notifyUpdate(getStateIdentifier(), accessPointPref);
        }
    }

    @Override
    public void onLevelChanged(AccessPoint accessPoint) {
        PreferenceCompat accessPointPref = new PreferenceCompat(
                new String[]{KEY_WIFI_LIST, accessPoint.getKey()});
        if (mUIUpdateCallback != null) {
            mUIUpdateCallback.notifyUpdate(getStateIdentifier(), accessPointPref);
        }
    }

    @Override
    public void onWifiListChanged() {
        updateWifiList();
    }


    @Override
    public int getStateIdentifier() {
        return ManagerUtil.STATE_NETWORK_MAIN;
    }

    private boolean isCaptivePortal(AccessPoint accessPoint) {
        if (accessPoint.getDetailedState() != NetworkInfo.DetailedState.CONNECTED) {
            return false;
        }
        NetworkCapabilities nc = mNetworkModule.getConnectivityManager().getNetworkCapabilities(
                mNetworkModule.getWifiManager().getCurrentNetwork());
        return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL);
    }

    @Override
    public void onConnectivityChange() {
        updateConnectivity();
    }
}
