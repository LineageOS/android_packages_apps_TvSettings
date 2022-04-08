/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.tv.settings.device;

import android.app.PendingIntent;
import android.app.tvsettings.TvSettingsEnums;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.IntegerRes;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;

import com.android.tv.settings.R;
import com.android.tv.twopanelsettings.slices.builders.PreferenceSliceBuilder;
import com.android.tv.twopanelsettings.slices.builders.PreferenceSliceBuilder.RowBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** The SliceProvider for "Eco settings" */
public class EcoSettingsSliceProvider extends SliceProvider {

    private static final String TAG = "EcoSettingsSliceProvider";
    private static final boolean DEBUG = false;
    private static final String KEY_LIMIT_NETWORK_IN_STANDBY = "limit_network_in_standby";
    private static final String ACTION_ENABLE_LIMIT_NETWORK_IN_STANDBY =
            "com.android.tv.settings.device.ACTION_ENABLE_LIMIT_NETWORK_IN_STANDBY";

    public static final String AUTHORITY = "com.android.tv.settings.device.sliceprovider";
    public static final String ECO_SETTINGS_SLICE_PATH = "eco_settings";
    public static final Uri ECO_SETTINGS_SLICE_URI =
            new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(AUTHORITY)
                    .appendPath(ECO_SETTINGS_SLICE_PATH)
                    .build();

    private final Map<Uri, Integer> mPinnedUris = new ArrayMap<>();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private BroadcastReceiver mEcoSettingsReceiver;

    @Override
    public boolean onCreateSliceProvider() {
        return true;
    }

    @Override
    public PendingIntent onCreatePermissionRequest(Uri sliceUri, String callingPackage) {
        final Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
        final PendingIntent noOpIntent = PendingIntent.getActivity(
                getContext(), 0, settingsIntent, PendingIntent.FLAG_IMMUTABLE);
        return noOpIntent;
    }

    @Override
    public Collection<Uri> onGetSliceDescendants(Uri uri) {
        final List<Uri> uris = new ArrayList<>();
        if (showEcoSettings()) {
            uris.add(ECO_SETTINGS_SLICE_URI);
        }
        return uris;
    }

    private boolean showEcoSettings() {
        return showLimitNetworkInStandbyToggle();
    }

    private boolean showLimitNetworkInStandbyToggle() {
        return getContext().getSystemService(PowerManager.class).isLowPowerStandbySupported();
    }

    @Override
    public void onSlicePinned(Uri sliceUri) {
        mHandler.post(() -> {
            if (DEBUG) {
                Log.d(TAG, "Slice pinned: " + sliceUri);
            }
            registerReceiver();
            if (!mPinnedUris.containsKey(sliceUri)) {
                mPinnedUris.put(sliceUri, 0);
            }
            mPinnedUris.put(sliceUri, mPinnedUris.get(sliceUri) + 1);
        });
    }

    @Override
    public void onSliceUnpinned(Uri sliceUri) {
        mHandler.post(() -> {
            if (DEBUG) {
                Log.d(TAG, "Slice unpinned: " + sliceUri);
            }
            Context context = getContext();
            if (mPinnedUris.containsKey(sliceUri)) {
                int newCount = mPinnedUris.get(sliceUri) - 1;
                mPinnedUris.put(sliceUri, newCount);
                if (newCount == 0) {
                    mPinnedUris.remove(sliceUri);
                }
            }
            if (mPinnedUris.isEmpty() && mEcoSettingsReceiver != null) {
                context.unregisterReceiver(mEcoSettingsReceiver);
                mEcoSettingsReceiver = null;
            }
        });
    }

    private void registerReceiver() {
        mEcoSettingsReceiver = new EcoSettingsReceiver();
        final IntentFilter ecoSettingsIntentFilter = new IntentFilter();
        ecoSettingsIntentFilter.addAction(PowerManager.ACTION_LOW_POWER_STANDBY_ENABLED_CHANGED);
        ecoSettingsIntentFilter.addAction(ACTION_ENABLE_LIMIT_NETWORK_IN_STANDBY);
        getContext().registerReceiver(mEcoSettingsReceiver, ecoSettingsIntentFilter,
                Context.RECEIVER_NOT_EXPORTED);
    }

    private void onLowPowerStandbyEnabledChanged() {
        updateEcoSettings(getContext());
    }

    static void updateEcoSettings(Context context) {
        context.getContentResolver().notifyChange(ECO_SETTINGS_SLICE_URI, null);
    }

    @Override
    public Slice onBindSlice(Uri sliceUri) {
        if (DEBUG) {
            Log.d(TAG, "onBindSlice: " + sliceUri);
        }
        if (ECO_SETTINGS_SLICE_PATH.equals(getFirstSegment(sliceUri))) {
            return createEcoSettingsSlice(sliceUri);
        }
        return null;
    }

    private Slice createEcoSettingsSlice(Uri sliceUri) {
        PreferenceSliceBuilder psb = new PreferenceSliceBuilder(getContext(), sliceUri);
        psb.addScreenTitle(
                new RowBuilder()
                        .setTitle(getString(R.string.device_eco_settings))
                        .setPageId(TvSettingsEnums.SYSTEM_POWER_AND_ENERGY_ECO_SETTINGS));
        updateLimitNetworkInStandbyToggle(psb);
        return psb.build();
    }

    private void updateLimitNetworkInStandbyToggle(PreferenceSliceBuilder psb) {
        if (!showLimitNetworkInStandbyToggle()) {
            if (DEBUG) {
                Log.d(TAG, "Low Power Standby not supported, not showing "
                        + "\"Limit network connection in standby\" toggle");
            }
            return;
        }

        PendingIntent limitNetworkInStandbyTogglePendingIntent;
        final PowerManager powerManager = getContext().getSystemService(PowerManager.class);
        final boolean limitNetworkInStandbyEnabled = powerManager.isLowPowerStandbyEnabled();
        if (limitNetworkInStandbyEnabled) {
            final Intent showConfirmationDialogIntent =
                    new Intent(getContext(), LimitNetworkInStandbyConfirmationDialogActivity.class);
            limitNetworkInStandbyTogglePendingIntent = PendingIntent.getActivity(
                    getContext(), /* requestCode= */ 2, showConfirmationDialogIntent,
                    PendingIntent.FLAG_IMMUTABLE);
        } else {
            final Intent enableLimitNetworkInStandbyIntent = new Intent(
                    ACTION_ENABLE_LIMIT_NETWORK_IN_STANDBY);
            enableLimitNetworkInStandbyIntent.setPackage(getContext().getPackageName());
            limitNetworkInStandbyTogglePendingIntent = PendingIntent.getBroadcast(
                    getContext(), /* requestCode= */ 2, enableLimitNetworkInStandbyIntent,
                    PendingIntent.FLAG_IMMUTABLE);
        }

        psb.addPreference(
                new RowBuilder()
                        .setKey(KEY_LIMIT_NETWORK_IN_STANDBY)
                        .setTitle(getString(R.string.limit_network_in_standby_toggle_title))
                        .setSubtitle(getString(R.string.limit_network_in_standby_toggle_summary))
                        .setInfoTitle(getString(R.string.limit_network_in_standby_toggle_title))
                        .setInfoSummary(getString(R.string.limit_network_in_standby_toggle_info))
                        .setInfoTitleIcon(IconCompat.createWithResource(getContext(),
                                R.drawable.ic_info_outline_base))
                        .addSwitch(limitNetworkInStandbyTogglePendingIntent,
                                limitNetworkInStandbyEnabled)
        );
    }

    private String getString(@IntegerRes int resId) {
        return getContext().getString(resId);
    }

    private static String getFirstSegment(Uri uri) {
        if (uri.getPathSegments().size() > 0) {
            return uri.getPathSegments().get(0);
        }
        return null;
    }

    private class EcoSettingsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (PowerManager.ACTION_LOW_POWER_STANDBY_ENABLED_CHANGED.equals(action)) {
                onLowPowerStandbyEnabledChanged();
            } else if (ACTION_ENABLE_LIMIT_NETWORK_IN_STANDBY.equals(action)) {
                PowerManager powerManager = getContext().getSystemService(PowerManager.class);
                powerManager.setLowPowerStandbyEnabled(true);
            }
        }
    }
}
