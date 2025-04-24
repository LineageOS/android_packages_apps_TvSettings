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

package com.android.tv.settings.about;

import static com.android.tv.settings.connectivity.WifiUtils.truncateIpAddress;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.wifi.WifiManager;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.deviceinfo.AbstractConnectivityPreferenceController;
import com.android.tv.settings.R;
import com.android.tv.settings.connectivity.WifiUtils.IpAddressInfoFragment;
import com.android.tv.settings.overlay.FlavorUtils;
import java.util.Iterator;

/** Modified version of IP address preference controller */
public class IpAddressPreferenceController extends AbstractConnectivityPreferenceController {
  @VisibleForTesting static final String KEY_IP_ADDRESS = "wifi_ip_address";

  private static final String[] CONNECTIVITY_INTENTS = {
    ConnectivityManager.CONNECTIVITY_ACTION,
    WifiManager.ACTION_LINK_CONFIGURATION_CHANGED,
    WifiManager.NETWORK_STATE_CHANGED_ACTION,
  };

  private Preference mIpAddress;
  private final ConnectivityManager mCM;
  private Context context;

  public IpAddressPreferenceController(Context context, Lifecycle lifecycle) {
    super(context, lifecycle);
    this.context = context;
    mCM = context.getSystemService(ConnectivityManager.class);
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  public String getPreferenceKey() {
    return KEY_IP_ADDRESS;
  }

  @Override
  public void displayPreference(PreferenceScreen screen) {
    super.displayPreference(screen);
    mIpAddress = screen.findPreference(KEY_IP_ADDRESS);
    updateConnectivity();
  }

  @Override
  protected String[] getConnectivityIntents() {
    return CONNECTIVITY_INTENTS;
  }

  @Override
  protected void updateConnectivity() {
    String ipAddress = getDefaultIpAddresses(mCM);
    if (ipAddress != null) {
      if (FlavorUtils.isTwoPanel(context)) {
        mIpAddress.setSummary(truncateIpAddress(ipAddress));
        mIpAddress.setFragment(IpAddressInfoFragment.class.getName());
        mIpAddress.getExtras().putString(IpAddressInfoFragment.ARG_IP_ADDRESS_EXTRA, ipAddress);
      } else {
        mIpAddress.setSummary(ipAddress);
      }
    } else {
      mIpAddress.setSummary(R.string.status_unavailable);
    }
  }

  /**
   * Returns the default link's IP addresses, if any, taking into account IPv4 and IPv6 style
   * addresses.
   *
   * @param cm ConnectivityManager
   * @return the formatted and newline-separated IP addresses, or null if none.
   */
  private static String getDefaultIpAddresses(ConnectivityManager cm) {
    LinkProperties prop = cm.getLinkProperties(cm.getActiveNetwork());
    return formatIpAddresses(prop);
  }

  private static String formatIpAddresses(LinkProperties prop) {
    if (prop == null) return null;
    Iterator<LinkAddress> iter = prop.getAllLinkAddresses().iterator();
    // If there are no entries, return null
    if (!iter.hasNext()) return null;
    // Concatenate all available addresses, newline separated
    StringBuilder addresses = new StringBuilder();
    while (iter.hasNext()) {
      addresses.append(iter.next().getAddress().getHostAddress());
      if (iter.hasNext()) addresses.append("\n");
    }
    return addresses.toString();
  }
}
