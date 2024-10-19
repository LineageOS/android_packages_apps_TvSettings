/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Keep;

import com.android.tv.twopanelsettings.R;
import com.android.tv.twopanelsettings.slices.InfoFragment;

@Keep
public class WiFiThreadNetworkInfoFragment extends InfoFragment {

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ImageView infoTitleIconImageView = view.requireViewById(R.id.info_title_icon);
        TextView infoSummaryTextView = view.requireViewById(R.id.info_summary);

        infoTitleIconImageView.setImageResource(R.drawable.ic_info_outline_base);
        infoTitleIconImageView.setVisibility(View.VISIBLE);

        infoSummaryTextView.setText(R.string.wifi_setting_thread_network_context);
        infoSummaryTextView.setVisibility(View.VISIBLE);
        return view;
    }

    @Override
    public void updateInfoFragment() {
        // No-op as this is hosting a static info preview panel
    }
}
