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

package com.android.tv.settings.service;

import static com.android.tv.settings.util.InstrumentationUtils.logEntrySelected;
import static com.android.tv.settings.util.InstrumentationUtils.logToggleInteracted;

import android.app.Service;
import android.app.tvsettings.TvSettingsEnums;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.settingslib.wifi.AccessPoint;
import com.android.tv.settings.R;
import com.android.tv.settings.connectivity.ConnectivityListener;
import com.android.tv.settings.service.INetworkService;
import com.android.tv.settings.service.data.PreferenceParcelableManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NetworkService extends Service implements ConnectivityListener.Listener,
        ConnectivityListener.WifiNetworkListener,
        AccessPoint.AccessPointListener {
    private static final String TAG = "NetworkService";
    private ConnectivityListener mConnectivityListener;
    private WifiManager mWifiManager;
    private ConnectivityManager mConnectivityManager;
    private boolean mIsWifiHardwarePresent;
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
    private static final int INITIAL_UPDATE_DELAY = 500;

    private static final String WIFI_SIGNAL_LEVEL = "wifi_signal_level";
    private static final String COLLAPSE = "collapse";

    // TODO : create PrefParcelablesManager to share parcelable info across service and provide data
    // for client
    PreferenceParcelable mEnableWifiPref;
    PreferenceParcelable mCollapsePref;
    PreferenceParcelable mAddPref;
    PreferenceParcelable mEthernetCategory;
    PreferenceParcelable mEthernetStatusPref;
    PreferenceParcelable mEthernetProxyPref;
    PreferenceParcelable mAlwaysScan;
    PreferenceParcelable mWifiNetworkCategoryPref;
    PreferenceParcelableManager mPreferenceParcelableManager;

    private INetworkServiceListener mListener;

    private final Handler mHandler = new Handler();
    private long mNoWifiUpdateBeforeMillis;
    private final Runnable mInitialUpdateWifiListRunnable = new Runnable() {
        @Override
        public void run() {
            mNoWifiUpdateBeforeMillis = 0;
            updateWifiList();
        }
    };


    private final INetworkService.Stub mBinder = new INetworkService.Stub() {

        @Override
        public List<PreferenceParcelable> getPreferences() throws RemoteException {

            return null;
        }

        @Override
        public PreferenceParcelable getPreference(String key) throws RemoteException {
            return null;
        }

        @Override
        public void registerListener(INetworkServiceListener listener) throws RemoteException {
            mHandler.post(() -> {
                mListener = listener;
            });
        }

        @Override
        public void unRegisterListener(INetworkServiceListener listener) throws RemoteException {
            mHandler.post(() -> {
                mListener = null;
            });
        }

        @Override
        public void onCreate() throws RemoteException {
            mHandler.post(() -> {
                NetworkService.this.onCreateFragment();
            });
        }

        @Override
        public void onStart() throws RemoteException {
            mHandler.post(() -> {
                NetworkService.this.onStartFragment();
            });
        }

        @Override
        public void onResume() throws RemoteException {
            mHandler.post(() -> {
                NetworkService.this.onResumeFragment();
            });

        }

        @Override
        public void onPause() throws RemoteException {
            mHandler.post(() -> {
                NetworkService.this.onPauseFragment();
            });
        }

        @Override
        public void onDestroy() {
            mHandler.post(() -> {
                NetworkService.this.onDestroyFragment();
            });
        }

        @Override
        public void onPreferenceClick(String key, boolean status) throws RemoteException {
            mHandler.post(() -> {
                NetworkService.this.onPreferenceClick(key, status);
            });
        }
    };

    void onCreateFragment() {
        mPreferenceParcelableManager = new PreferenceParcelableManager();
        mConnectivityListener = new ConnectivityListener(this, this, null);
        mIsWifiHardwarePresent = getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_WIFI);
        mWifiManager = getSystemService(WifiManager.class);
        mConnectivityManager = getSystemService(ConnectivityManager.class);
        mEnableWifiPref = mPreferenceParcelableManager.getOrCreatePrefParcelable(KEY_WIFI_ENABLE);
        mAlwaysScan = mPreferenceParcelableManager.getOrCreatePrefParcelable(KEY_WIFI_ALWAYS_SCAN);
        mCollapsePref = mPreferenceParcelableManager.getOrCreatePrefParcelable(KEY_WIFI_COLLAPSE);
        mCollapsePref.addInfo(COLLAPSE, "true");
        mAddPref = mPreferenceParcelableManager.getOrCreatePrefParcelable(KEY_WIFI_ADD);
        mEthernetCategory = mPreferenceParcelableManager.getOrCreatePrefParcelable(KEY_ETHERNET);
        mEthernetStatusPref = mPreferenceParcelableManager.getOrCreatePrefParcelable(
                KEY_ETHERNET_STATUS);
        mEthernetProxyPref = mPreferenceParcelableManager.getOrCreatePrefParcelable(
                KEY_ETHERNET_PROXY);
        mWifiNetworkCategoryPref = mPreferenceParcelableManager.getOrCreatePrefParcelable(
                KEY_WIFI_LIST);
        mWifiNetworkCategoryPref.addInfo(COLLAPSE, "true");
        mWifiNetworkCategoryPref.setType(
                PreferenceParcelable.TYPE_PREFERENCE_WIFI_COLLAPSE_CATEGORY);
    }

    void onStartFragment() {
        mConnectivityListener.setWifiListener(this);
        mConnectivityListener.start();
        mNoWifiUpdateBeforeMillis = SystemClock.elapsedRealtime() + INITIAL_UPDATE_DELAY;
        updateWifiList();
    }

    void onResumeFragment() {
        updateConnectivity();
    }

    void onDestroyFragment() {
        mConnectivityListener.destroy();
    }

    private void updateConnectivity() {
        List<PreferenceParcelable> preferenceParcelables = new ArrayList<>();
        final boolean wifiEnabled = mIsWifiHardwarePresent
                && mConnectivityListener.isWifiEnabledOrEnabling();
        mEnableWifiPref.setChecked(wifiEnabled);
        preferenceParcelables.add(mEnableWifiPref);

        mWifiNetworkCategoryPref.setVisible(wifiEnabled);
        preferenceParcelables.add(mWifiNetworkCategoryPref);

        mCollapsePref.setVisible(wifiEnabled);
        preferenceParcelables.add(mCollapsePref);

        mAddPref.setVisible(wifiEnabled);
        preferenceParcelables.add(mAddPref);


        if (!wifiEnabled) {
            updateWifiList();
        }

        int scanAlwaysAvailable = 0;
        try {
            scanAlwaysAvailable = Settings.Global.getInt(getContentResolver(),
                    Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE);
        } catch (Settings.SettingNotFoundException e) {
            // Ignore
        }

        mAlwaysScan.setChecked(scanAlwaysAvailable == 1);
        mAlwaysScan.setContentDescription(
                getString(R.string.wifi_setting_always_scan_content_description));

        final boolean ethernetAvailable = mConnectivityListener.isEthernetAvailable();
        mEthernetCategory.setVisible(ethernetAvailable);
        mEthernetStatusPref.setVisible(ethernetAvailable);
        mEthernetProxyPref.setVisible(ethernetAvailable);
        preferenceParcelables.add(mEthernetCategory);
        preferenceParcelables.add(mEthernetStatusPref);
        preferenceParcelables.add(mEthernetProxyPref);
        if (ethernetAvailable) {
            final boolean ethernetConnected =
                    mConnectivityListener.isEthernetConnected();
            mEthernetStatusPref.setTitle(ethernetConnected
                    ? getString(R.string.connected) : getString(R.string.not_connected));
            mEthernetStatusPref.setSummary(mConnectivityListener.getEthernetIpAddress());
        }

        try {
            mListener.notifyUpdateAll(
                    mPreferenceParcelableManager.prefParcelablesCopy(preferenceParcelables));
        } catch (RemoteException e) {
            Log.e(TAG, "remote failed: " + e);
        }
    }

    private void updateWifiList() {
        if (!mIsWifiHardwarePresent || !mConnectivityListener.isWifiEnabledOrEnabling()) {
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

        final Collection<AccessPoint> accessPoints = mConnectivityListener.getAvailableNetworks();
        mWifiNetworkCategoryPref.initChildPreferences();
        for (final AccessPoint accessPoint : accessPoints) {
            accessPoint.setListener(this);
            PreferenceParcelable accessPointPref = new PreferenceParcelable(
                    new String[]{KEY_WIFI_LIST, accessPoint.getKey()});
            accessPointPref.setTitle(accessPoint.getTitle());
            accessPointPref.setType(PreferenceParcelable.TYPE_PREFERENCE_ACCESS_POINT);
            accessPointPref.addInfo(WIFI_SIGNAL_LEVEL, String.valueOf(accessPoint.getLevel()));
            if (accessPoint.isActive() && !isCaptivePortal(accessPoint)) {
                // TODO: link to WifiDetailsFragment
            } else {
                // TODO: link to WifiConnectionActivity
            }
            mWifiNetworkCategoryPref.addChildPrefParcelable(accessPointPref);
        }
        try {
            mListener.notifyUpdate(
                    PreferenceParcelableManager.prefParcelableCopy(mWifiNetworkCategoryPref));
        } catch (RemoteException e) {
            Log.e(TAG, "remote failed: " + e);
        }
    }

    void onPauseFragment() {
        mConnectivityListener.stop();
    }

    void onPreferenceClick(String key, boolean status) {
        switch (key) {
            case KEY_WIFI_ENABLE:
                mConnectivityListener.setWifiEnabled(status);
                mEnableWifiPref.setChecked(status);
                break;
            case KEY_WIFI_COLLAPSE:
                boolean collapse = !("true".equals(mWifiNetworkCategoryPref.getInfo(COLLAPSE)));
                mWifiNetworkCategoryPref.addInfo(COLLAPSE, String.valueOf(collapse));
                mCollapsePref.addInfo(COLLAPSE, String.valueOf(collapse));
                logEntrySelected(
                        collapse
                                ? TvSettingsEnums.NETWORK_SEE_FEWER
                                : TvSettingsEnums.NETWORK_SEE_ALL);
                try {
                    mListener.notifyUpdate(PreferenceParcelableManager.prefParcelableCopy(
                            mWifiNetworkCategoryPref));
                } catch (RemoteException e) {
                    Log.e(TAG, "remote failed: " + e);
                }
                break;
            case KEY_WIFI_ALWAYS_SCAN:
                mAlwaysScan.setChecked(status);
                Settings.Global.putInt(getContentResolver(),
                        Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE,
                        status ? 1 : 0);
                logToggleInteracted(
                        TvSettingsEnums.NETWORK_ALWAYS_SCANNING_NETWORKS, status);
                break;
            case KEY_ETHERNET_STATUS:
                break;
            case KEY_WIFI_ADD:
                logEntrySelected(TvSettingsEnums.NETWORK_ADD_NEW_NETWORK);
                break;
            case KEY_ETHERNET_DHCP:
                logEntrySelected(TvSettingsEnums.NETWORK_ETHERNET_IP_SETTINGS);
                break;
            case KEY_ETHERNET_PROXY:
                logEntrySelected(TvSettingsEnums.NETWORK_ETHERNET_PROXY_SETTINGS);
                break;
            default:
                // no-op
        }
        try {
            mListener.notifyUpdate(mPreferenceParcelableManager.prefParcelableCopy(key));
        } catch (RemoteException e) {
            Log.e(TAG, "remote failed: " + e);
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onConnectivityChange() {
        updateConnectivity();
    }

    @Override
    public void onWifiListChanged() {
        updateWifiList();
    }


    @Override
    public void onAccessPointChanged(AccessPoint accessPoint) {
        PreferenceParcelable accessPointPref = new PreferenceParcelable(
                new String[]{KEY_WIFI_LIST, accessPoint.getKey()});
        try {
            mListener.notifyUpdate(PreferenceParcelableManager.prefParcelableCopy(accessPointPref));
        } catch (RemoteException e) {
            Log.e(TAG, "remote failed: " + e);
        }
    }

    @Override
    public void onLevelChanged(AccessPoint accessPoint) {
        PreferenceParcelable accessPointPref = new PreferenceParcelable(
                new String[]{KEY_WIFI_LIST, accessPoint.getKey()});
        try {
            mListener.notifyUpdate(PreferenceParcelableManager.prefParcelableCopy(accessPointPref));
        } catch (RemoteException e) {
            Log.e(TAG, "remote failed: " + e);
        }
    }


    private boolean isCaptivePortal(AccessPoint accessPoint) {
        if (accessPoint.getDetailedState() != NetworkInfo.DetailedState.CONNECTED) {
            return false;
        }
        NetworkCapabilities nc = mConnectivityManager.getNetworkCapabilities(
                mWifiManager.getCurrentNetwork());
        return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL);
    }
}
