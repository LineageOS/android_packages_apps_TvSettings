/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.StringRes;
import com.android.tv.settings.R;
import com.android.tv.twopanelsettings.slices.InfoFragment;
import com.android.wifitrackerlib.WifiEntry;

/** Helper class for Wifi configuration. */
public class WifiUtils {
  @StringRes
  static int getConnectionStatus(WifiEntry wifiEntry) {
    if (wifiEntry.canSignIn()) {
      return R.string.wifi_captive_portal;
    } else if (wifiEntry.getConnectedState() == WifiEntry.CONNECTED_STATE_CONNECTED) {
      return wifiEntry.hasInternetAccess() ? R.string.connected : R.string.wifi_no_internet;
    } else if (wifiEntry.shouldEditBeforeConnect()) {
      return R.string.wifi_bad_password;
    } else if (wifiEntry.isSaved()) {
      return R.string.wifi_saved;
    }
    return R.string.not_connected;
  }

  /** Truncates IP address to 32 characters with ellipse or first whitespace if it is too long */
  public static CharSequence truncateIpAddress(CharSequence input) {
    final int MAX_LENGTH = 28;
    final String ELLIPSIS = " ...";

    if (input.length() > MAX_LENGTH) {
      int truncateAt = MAX_LENGTH;

      for (int i = 0; i < MAX_LENGTH; i++) {
        if (Character.isWhitespace(input.charAt(i))) {
          truncateAt = i;
          break;
        }
      }

      StringBuilder truncated = new StringBuilder(truncateAt + ELLIPSIS.length());
      truncated.append(input, 0, truncateAt);
      truncated.append(ELLIPSIS);
      return truncated.toString();

    } else {
      return input;
    }
  }

  public static class IpAddressInfoFragment extends InfoFragment {
    public static String ARG_IP_ADDRESS_EXTRA = "IP_ADDRESS_EXTRA";

    public IpAddressInfoFragment() {}

    @Override
    public View onCreateView(
        LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View view = super.onCreateView(inflater, container, savedInstanceState);
      CharSequence ipAddress = getArguments().getCharSequence(ARG_IP_ADDRESS_EXTRA);
      ((TextView) view.findViewById(com.android.tv.twopanelsettings.R.id.info_title))
          .setText(getActivity().getString(R.string.title_ip_address));
      view.findViewById(com.android.tv.twopanelsettings.R.id.info_title)
          .setVisibility(View.VISIBLE);
      ((TextView) view.findViewById(com.android.tv.twopanelsettings.R.id.info_summary))
          .setText(ipAddress);
      view.findViewById(com.android.tv.twopanelsettings.R.id.info_summary)
          .setVisibility(View.VISIBLE);
      return view;
    }
  }

  private WifiUtils() {}
}
