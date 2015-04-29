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

package com.android.tv.settings.device;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.format.Formatter;

import com.android.settingslib.deviceinfo.StorageMeasurement;
import com.android.settingslib.deviceinfo.StorageMeasurement.MeasurementDetails;
import com.android.settingslib.deviceinfo.StorageMeasurement.MeasurementReceiver;
import com.android.tv.settings.R;
import com.android.tv.settings.device.apps.AppsActivity;
import com.android.tv.settings.dialog.Layout;
import com.android.tv.settings.dialog.Layout.Action;
import com.android.tv.settings.dialog.Layout.Header;
import com.android.tv.settings.dialog.Layout.Status;
import com.android.tv.settings.dialog.Layout.StringGetter;
import com.android.tv.settings.dialog.SettingsLayoutActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Activity to view storage consumption and factory reset device.
 */
public class StorageResetActivity extends SettingsLayoutActivity {

    private static final String TAG = "StorageResetActivity";
    private static final long INVALID_SIZE = -1;
    private static final int ACTION_RESET_DEVICE = 1;
    private static final int ACTION_CANCEL = 2;
    private static final int ACTION_CLEAR_CACHE = 3;

    /**
     * Support for shutdown-after-reset. If our launch intent has a true value for
     * the boolean extra under the following key, then include it in the intent we
     * use to trigger a factory reset. This will cause us to shut down instead of
     * restart after the reset.
     */
    private static final String SHUTDOWN_INTENT_EXTRA = "shutdown";

    private class SizeStringGetter extends StringGetter {
        private long mSize = INVALID_SIZE;

        @Override
        public String get() {
            return String.format(getString(R.string.storage_size), formatSize(mSize));
        }

        public void setSize(long size) {
            mSize = size;
            refreshView();
        }
    }

    private StorageManager mStorageManager;

    private final List<StorageLayoutGetter> mStorageLayoutGetters = new ArrayList<>();

    private final StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            switch(vol.getType()) {
                case VolumeInfo.TYPE_PRIVATE:
                case VolumeInfo.TYPE_PUBLIC:
                    mStorageHeadersGetter.refreshView();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mStorageManager = getSystemService(StorageManager.class);
        mStorageHeadersGetter.refreshView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mStorageManager.registerListener(mStorageListener);
        for (StorageLayoutGetter getter : mStorageLayoutGetters) {
            getter.startListening();
        }
    }

    @Override
    protected void onPause() {
        mStorageManager.unregisterListener(mStorageListener);
        for (StorageLayoutGetter getter : mStorageLayoutGetters) {
            getter.stopListening();
        }
        super.onPause();
    }

    @Override
    public Layout createLayout() {
        return new Layout().breadcrumb(getString(R.string.header_category_device))
                .add(new Header.Builder(getResources())
                        .icon(R.drawable.ic_settings_storage)
                        .title(R.string.device_storage_reset)
                        .build()
                        .add(mStorageHeadersGetter)
                        .add(createResetHeaders())
                );
    }

    private final Layout.LayoutGetter mStorageHeadersGetter = new Layout.LayoutGetter() {
        @Override
        public Layout get() {
            final Layout layout = new Layout();
            if (mStorageManager == null) {
                return layout;
            }
            final List<VolumeInfo> volumes = mStorageManager.getVolumes();
            Collections.sort(volumes, VolumeInfo.getDescriptionComparator());

            if (isResumed()) {
                for (StorageLayoutGetter getter : mStorageLayoutGetters) {
                    getter.stopListening();
                }
            }
            mStorageLayoutGetters.clear();


            for (VolumeInfo vol : volumes) {
                if (vol.getType() != VolumeInfo.TYPE_PRIVATE
                        && vol.getType() != VolumeInfo.TYPE_PUBLIC) {
                    continue;
                }
                final StorageLayoutGetter getter = new StorageLayoutGetter(vol);
                mStorageLayoutGetters.add(getter);
                layout.add(getter);
                if (isResumed()) {
                    getter.startListening();
                }
            }
            return layout;
        }
    };

    private class StorageLayoutGetter extends Layout.LayoutGetter {

        private final VolumeInfo mVolume;
        private final VolumeInfo mSharedVolume;

        private StorageMeasurement mMeasure;
        private final SizeStringGetter mAppsSize = new SizeStringGetter();
        private final SizeStringGetter mDcimSize = new SizeStringGetter();
        private final SizeStringGetter mMusicSize = new SizeStringGetter();
        private final SizeStringGetter mDownloadsSize = new SizeStringGetter();
        private final SizeStringGetter mCacheSize = new SizeStringGetter();
        private final SizeStringGetter mMiscSize = new SizeStringGetter();
        private final SizeStringGetter mAvailSize = new SizeStringGetter();
        private final SizeStringGetter mStorageDescription = new SizeStringGetter();

        private final MeasurementReceiver mReceiver = new MeasurementReceiver() {

            private MeasurementDetails mLastMeasurementDetails = null;

            @Override
            public void onDetailsChanged(MeasurementDetails details) {
                mLastMeasurementDetails = details;
                updateDetails(mLastMeasurementDetails);
            }
        };

        public StorageLayoutGetter(VolumeInfo volume) {
            mVolume = volume;
            mSharedVolume = mStorageManager.findEmulatedForPrivate(mVolume);
        }

        @Override
        public Layout get() {
            final Layout layout = new Layout();
            final Resources res = getResources();
            final Intent appsIntent = new Intent(StorageResetActivity.this, AppsActivity.class);
            final Header header = new Header.Builder(res)
                    .title(mStorageManager.getBestVolumeDescription(mVolume))
                    .description(mStorageDescription)
                    .build();
            if (mVolume.getType() == VolumeInfo.TYPE_PRIVATE) {
                header
                        .add(new Action.Builder(res, appsIntent)
                                .title(R.string.storage_apps_usage)
                                .icon(R.drawable.storage_indicator_apps)
                                .description(mAppsSize)
                                .build())
                        .add(new Status.Builder(res)
                                .title(R.string.storage_dcim_usage)
                                .icon(R.drawable.storage_indicator_dcim)
                                .description(mDcimSize)
                                .build())
                        .add(new Status.Builder(res)
                                .title(R.string.storage_music_usage)
                                .icon(R.drawable.storage_indicator_music)
                                .description(mMusicSize)
                                .build())
                        .add(new Status.Builder(res)
                                .title(R.string.storage_downloads_usage)
                                .icon(R.drawable.storage_indicator_downloads)
                                .description(mDownloadsSize)
                                .build())
                        .add(new Action.Builder(res, ACTION_CLEAR_CACHE)
                                .title(R.string.storage_media_cache_usage)
                                .icon(R.drawable.storage_indicator_cache)
                                .description(mCacheSize)
                                .build())
                        .add(new Status.Builder(res)
                                .title(R.string.storage_media_misc_usage)
                                .icon(R.drawable.storage_indicator_misc)
                                .description(mMiscSize)
                                .build())
                        .add(new Status.Builder(res)
                                .title(R.string.storage_available)
                                .icon(R.drawable.storage_indicator_available)
                                .description(mAvailSize)
                                .build());
            } else {
                header
                        .add(new Status.Builder(res)
                                .title(R.string.storage_media_misc_usage)
                                .icon(R.drawable.storage_indicator_misc)
                                .description(mMiscSize)
                                .build())
                        .add(new Status.Builder(res)
                                .title(R.string.storage_available)
                                .icon(R.drawable.storage_indicator_available)
                                .description(mAvailSize)
                                .build());
            }
            layout.add(header);
            return layout;
        }

        public void startListening() {
            mMeasure = new StorageMeasurement(StorageResetActivity.this, mVolume, mSharedVolume);
            mMeasure.setReceiver(mReceiver);
            mMeasure.forceMeasure();
        }

        public void stopListening() {
            mMeasure.onDestroy();
        }

        private void updateDetails(MeasurementDetails details) {
            final long dcimSize = totalValues(details.mediaSize, Environment.DIRECTORY_DCIM,
                    Environment.DIRECTORY_MOVIES, Environment.DIRECTORY_PICTURES);

            final long musicSize = totalValues(details.mediaSize, Environment.DIRECTORY_MUSIC,
                    Environment.DIRECTORY_ALARMS, Environment.DIRECTORY_NOTIFICATIONS,
                    Environment.DIRECTORY_RINGTONES, Environment.DIRECTORY_PODCASTS);

            final long downloadsSize = totalValues(details.mediaSize, Environment.DIRECTORY_DOWNLOADS);

            mAvailSize.setSize(details.availSize);
            mAppsSize.setSize(details.appsSize);
            mDcimSize.setSize(dcimSize);
            mMusicSize.setSize(musicSize);
            mDownloadsSize.setSize(downloadsSize);
            mCacheSize.setSize(details.cacheSize);
            mMiscSize.setSize(details.miscSize);
            mStorageDescription.setSize(details.totalSize);
        }
    }

    private Header createResetHeaders() {
        final Resources res = getResources();
        return new Header.Builder(res)
                .title(R.string.device_reset)
                .build()
                .add(new Header.Builder(res)
                        .title(R.string.device_reset)
                        .build()
                        .add(new Action.Builder(res, ACTION_RESET_DEVICE)
                                .title(R.string.confirm_factory_reset_device)
                                .build()
                        )
                        .add(new Action.Builder(res, Action.ACTION_BACK)
                                .title(R.string.title_cancel)
                                .defaultSelection()
                                .build())
                )
                .add(new Action.Builder(res, Action.ACTION_BACK)
                        .title(R.string.title_cancel)
                        .defaultSelection()
                        .build());
    }

    @Override
    public void onActionClicked(Action action) {
        switch (action.getId()) {
            case ACTION_RESET_DEVICE:
                if (!ActivityManager.isUserAMonkey()) {
                    Intent resetIntent = new Intent("android.intent.action.MASTER_CLEAR");
                    if (getIntent().getBooleanExtra(SHUTDOWN_INTENT_EXTRA, false)) {
                        resetIntent.putExtra(SHUTDOWN_INTENT_EXTRA, true);
                    }
                    sendBroadcast(resetIntent);
                }
                break;
            case ACTION_CANCEL:
                goBackToTitle(getString(R.string.device_storage_reset));
                break;
            case ACTION_CLEAR_CACHE:
                final DialogFragment fragment = ConfirmClearCacheFragment.newInstance();
                fragment.show(getFragmentManager(), null);
                break;
            default:
                final Intent intent = action.getIntent();
                if (intent != null) {
                    startActivity(intent);
                }
        }
    }

    private static long totalValues(HashMap<String, Long> map, String... keys) {
        long total = 0;
        for (String key : keys) {
            if (map.containsKey(key)) {
                total += map.get(key);
            }
        }
        return total;
    }

    private String formatSize(long size) {
        return (size == INVALID_SIZE) ? getString(R.string.storage_calculating_size)
                : Formatter.formatShortFileSize(this, size);
    }

    /**
     * Dialog to request user confirmation before clearing all cache data.
     */
    public static class ConfirmClearCacheFragment extends DialogFragment {
        public static ConfirmClearCacheFragment newInstance() {
            return new ConfirmClearCacheFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.device_storage_clear_cache_title);
            builder.setMessage(getString(R.string.device_storage_clear_cache_message));

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final PackageManager pm = context.getPackageManager();
                    final List<PackageInfo> infos = pm.getInstalledPackages(0);
                    for (PackageInfo info : infos) {
                        pm.deleteApplicationCacheFiles(info.packageName, null);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }
    }

}
