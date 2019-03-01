/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.tv.settings.about;

import android.app.tvsettings.TvSettingsEnums;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.tv.settings.NopePreferenceController;
import com.android.tv.settings.PreferenceControllerFragment;
import com.android.tv.settings.R;
import com.android.tv.settings.accessories.util.bluetooth.BluetoothAddressPreferenceController;
import com.android.tv.settings.util.SliceUtils;
import com.android.tv.twopanelsettings.slices.SliceShard;
import com.android.tv.twopanelsettings.slices.compat.Slice;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for showing device hardware info, such as MAC addresses and serial numbers
 */
@Keep
public class StatusFragment extends PreferenceControllerFragment implements SliceShard.Callbacks {

    private static final String KEY_BATTERY_STATUS = "battery_status";
    private static final String KEY_BATTERY_LEVEL = "battery_level";

    private SliceShard mSliceShard;

    public static StatusFragment newInstance() {
        return new StatusFragment();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.device_info_status;
    }

    @Override
    protected List<AbstractPreferenceController> onCreatePreferenceControllers(Context context) {
        return ImmutableList.of();
    }

    private void addPreferenceControllers() {
        final List<AbstractPreferenceController> controllers = new ArrayList<>(10);
        final Lifecycle lifecycle = getSettingsLifecycle();
        final Context context = requireContext();

        // TODO: detect if we have a battery or not
        addPreferenceController(new NopePreferenceController(context, KEY_BATTERY_LEVEL));
        addPreferenceController(new BatteryStatusPreferenceController(context, lifecycle));

        addPreferenceController(new SerialNumberPreferenceController(context));
        addPreferenceController(new UptimePreferenceController(context, lifecycle));
        addPreferenceController(new BluetoothAddressPreferenceController(context, lifecycle));
        addPreferenceController(new IpAddressPreferenceController(context, lifecycle));
        addPreferenceController(new MacAddressPreferenceController(context, lifecycle));
        addPreferenceController(new ImsStatusPreferenceController(context, lifecycle));
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        var sliceUri = SliceShard.Companion.getSliceUri(getResources(),
                R.string.status_fragment_slice_uri, R.string.main_fragment_slice_uri,
                "device_info_status");
        if (!SliceUtils.isSliceProviderValid(requireContext(), sliceUri)) {
            setPreferencesFromResource(getPreferenceScreenResId(), null);
            addPreferenceControllers();
            refreshAllPreferences();
            return;
        }

        setPreferencesFromResource(R.xml.settings_loading, null);
        mSliceShard = new SliceShard(this, sliceUri, this,
                getString(R.string.device_status_title),
                SliceShard.Companion.getPrefContext(requireContext()), true);
    }

    @Override
    public void onSlice(@Nullable Slice slice) {
        mSliceShard = null;
        if (slice == null) {
            setPreferencesFromResource(getPreferenceScreenResId(), null);
        }
        addPreferenceControllers();
        updatePreferenceStates();
        refreshAllPreferences();
    }

    protected int getPageId() {
        return TvSettingsEnums.SYSTEM_ABOUT_STATUS;
    }
}
