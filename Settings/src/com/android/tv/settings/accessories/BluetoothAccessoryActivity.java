/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.tv.settings.accessories;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.DrawableRes;

import com.android.tv.settings.BaseSettingsFragment;
import com.android.tv.settings.R;

public class BluetoothAccessoryActivity extends Activity {

    public static final String EXTRA_ACCESSORY_ADDRESS = "accessory_address";
    public static final String EXTRA_ACCESSORY_NAME = "accessory_name";
    public static final String EXTRA_ACCESSORY_ICON_ID = "accessory_icon_res";

    public static Intent createIntent(Context context, String deviceAddress,
            String deviceName, @DrawableRes int iconId) {
        Intent i = new Intent(context, BluetoothAccessoryActivity.class);
        i.putExtra(EXTRA_ACCESSORY_ADDRESS, deviceAddress);
        i.putExtra(EXTRA_ACCESSORY_NAME, deviceName);
        i.putExtra(EXTRA_ACCESSORY_ICON_ID, iconId);
        return i;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String deviceAddress = null;
        String deviceName;
        int deviceImgId;
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            deviceAddress = bundle.getString(EXTRA_ACCESSORY_ADDRESS);
            deviceName = bundle.getString(EXTRA_ACCESSORY_NAME);
            deviceImgId = bundle.getInt(EXTRA_ACCESSORY_ICON_ID);
        } else {
            deviceName = getString(R.string.accessory_options);
            deviceImgId = R.drawable.ic_qs_bluetooth_not_connected;
        }

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(android.R.id.content,
                            SettingsFragment.newInstance(deviceAddress, deviceName, deviceImgId))
                    .commit();
        }
    }

    public static class SettingsFragment extends BaseSettingsFragment {

        public static SettingsFragment newInstance(String deviceAddress, String deviceName,
                int deviceImgId) {
            final Bundle b = new Bundle(3);
            b.putString(EXTRA_ACCESSORY_ADDRESS, deviceAddress);
            b.putString(EXTRA_ACCESSORY_NAME, deviceName);
            b.putInt(EXTRA_ACCESSORY_ICON_ID, deviceImgId);
            final SettingsFragment f = new SettingsFragment();
            f.setArguments(b);
            return f;
        }

        @Override
        public void onPreferenceStartInitialScreen() {
            final Bundle args = getArguments();
            String deviceAddress = args.getString(EXTRA_ACCESSORY_ADDRESS);
            String deviceName = args.getString(EXTRA_ACCESSORY_NAME);
            int deviceImgId = args.getInt(EXTRA_ACCESSORY_ICON_ID);
            startPreferenceFragment(
                    BluetoothAccessoryFragment.newInstance(deviceAddress, deviceName, deviceImgId));

        }
    }
}
