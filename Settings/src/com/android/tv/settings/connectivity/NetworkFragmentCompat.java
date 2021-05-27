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

package com.android.tv.settings.connectivity;

import static com.android.tv.settings.service.ServiceUtil.INFO_COLLAPSE;
import static com.android.tv.settings.service.ServiceUtil.STATE_NETWORK_MAIN;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.android.tv.settings.R;
import com.android.tv.settings.compat.HasKeys;
import com.android.tv.settings.compat.PreferenceControllerFragmentCompat;
import com.android.tv.settings.compat.RenderUtil;
import com.android.tv.settings.compat.TsCollapsibleCategory;
import com.android.tv.settings.library.PreferenceCompat;

@Keep
public class NetworkFragmentCompat extends PreferenceControllerFragmentCompat {
    private static final String KEY_WIFI_LIST = "wifi_list";
    private static final String KEY_WIFI_COLLAPSE = "wifi_collapse";
    private static final String KEY_ETHERNET_PROXY = "ethernet_proxy";
    private static final String KEY_ETHERNET_DHCP = "ethernet_dhcp";

    private TsCollapsibleCategory mWifiNetworksCategory;
    private Preference mEthernetProxyPref;
    private Preference mEthernetDhcpPref;

    private int getPreferenceScreenResId() {
        return R.xml.network_compat;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager()
                .setPreferenceComparisonCallback(
                        new PreferenceManager.SimplePreferenceComparisonCallback());
        setPreferencesFromResource(getPreferenceScreenResId(), null);
        mWifiNetworksCategory = findPreference(KEY_WIFI_LIST);
        mEthernetProxyPref = findPreference(KEY_ETHERNET_PROXY);
        mEthernetProxyPref.setIntent(
                new Intent()
                        .setAction("com.android.settings.wifi.action.EDIT_PROXY_SETTINGS")
                        .putExtra("network_id", -1));
        mEthernetDhcpPref = findPreference(KEY_ETHERNET_DHCP);
        mEthernetDhcpPref.setIntent(
                new Intent()
                        .setAction("com.android.settings.wifi.action.EDIT_IP_SETTINGS")
                        .putExtra("network_id", "-1"));
    }

    @Override
    public HasKeys updatePref(PreferenceCompat prefParcelable) {
        HasKeys preference = super.updatePref(prefParcelable);
        if (preference == null) {
            return null;
        }
        String[] key = preference.getKeys();
        switch (key[0]) {
            case KEY_WIFI_COLLAPSE:
                boolean collapse = "true".equals(prefParcelable.getInfo(INFO_COLLAPSE));
                ((Preference) preference).setTitle(
                        collapse ? R.string.wifi_setting_see_all : R.string.wifi_setting_see_fewer);
                break;
            default:
        }
        if (prefParcelable.getType() == PreferenceCompat.TYPE_PREFERENCE_WIFI_COLLAPSE_CATEGORY) {
            RenderUtil.updatePreferenceGroup(
                    mWifiNetworksCategory, prefParcelable.getChildPrefCompats());
        }
        return preference;
    }

    @Override
    public int getState() {
        return STATE_NETWORK_MAIN;
    }
}
