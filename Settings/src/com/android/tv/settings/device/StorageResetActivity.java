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
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import com.android.settingslib.deviceinfo.StorageMeasurement;
import com.android.settingslib.deviceinfo.StorageMeasurement.MeasurementDetails;
import com.android.settingslib.deviceinfo.StorageMeasurement.MeasurementReceiver;
import com.android.tv.settings.R;
import com.android.tv.settings.device.apps.AppsActivity;
import com.android.tv.settings.device.storage.EjectInternalStepFragment;
import com.android.tv.settings.device.storage.FormatAsPrivateStepFragment;
import com.android.tv.settings.device.storage.FormatAsPublicStepFragment;
import com.android.tv.settings.device.storage.FormattingProgressFragment;
import com.android.tv.settings.device.storage.MoveAppProgressFragment;
import com.android.tv.settings.device.storage.MoveAppStepFragment;
import com.android.tv.settings.device.storage.SlowDriveStepFragment;
import com.android.tv.settings.dialog.Layout;
import com.android.tv.settings.dialog.Layout.Action;
import com.android.tv.settings.dialog.Layout.Header;
import com.android.tv.settings.dialog.Layout.Static;
import com.android.tv.settings.dialog.Layout.Status;
import com.android.tv.settings.dialog.Layout.StringGetter;
import com.android.tv.settings.dialog.SettingsLayoutActivity;
import com.android.tv.settings.util.SettingsAsyncTaskLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity to view storage consumption and factory reset device.
 */
public class StorageResetActivity extends SettingsLayoutActivity
        implements MoveAppStepFragment.Callback, FormatAsPrivateStepFragment.Callback {

    private static final String TAG = "StorageResetActivity";
    private static final long INVALID_SIZE = -1;
    private static final int ACTION_RESET_DEVICE = 1;
    private static final int ACTION_CANCEL = 2;
    private static final int ACTION_CLEAR_CACHE = 3;
    private static final int ACTION_EJECT_PRIVATE = 4;
    private static final int ACTION_EJECT_PUBLIC = 5;
    private static final int ACTION_ERASE_PRIVATE = 6;
    private static final int ACTION_ERASE_PUBLIC = 7;

    /**
     * Support for shutdown-after-reset. If our launch intent has a true value for
     * the boolean extra under the following key, then include it in the intent we
     * use to trigger a factory reset. This will cause us to shut down instead of
     * restart after the reset.
     */
    private static final String SHUTDOWN_INTENT_EXTRA = "shutdown";

    private static final String MOVE_PROGRESS_DIALOG_BACKSTACK_TAG = "moveProgressDialog";
    private static final String FORMAT_DIALOG_BACKSTACK_TAG = "formatDialog";

    private static final String SAVE_STATE_MOVE_ID = "StorageResetActivity.moveId";
    private static final String SAVE_STATE_FORMAT_PRIVATE_DISK_ID =
            "StorageResetActivity.formatPrivateDiskId";
    private static final String SAVE_STATE_FORMAT_PRIVATE_DISK_DESC =
            "StorageResetActivity.formatPrivateDiskDesc";

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
    private PackageManager mPackageManager;

    private final Map<String, StorageLayoutGetter> mStorageLayoutGetters = new ArrayMap<>();
    private final Map<String, SizeStringGetter> mStorageDescriptionGetters = new ArrayMap<>();

    private final StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            switch(vol.getType()) {
                case VolumeInfo.TYPE_PRIVATE:
                case VolumeInfo.TYPE_PUBLIC:
                    mStorageHeadersGetter.refreshView();
                    StorageLayoutGetter getter = mStorageLayoutGetters.get(vol.getId());
                    if (getter != null) {
                        getter.onVolumeUpdated();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private int mAppMoveId;
    private final PackageManager.MoveCallback mMoveCallback = new PackageManager.MoveCallback() {
        @Override
        public void onStatusChanged(int moveId, int status, long estMillis) {
            if (moveId != mAppMoveId || !PackageManager.isMoveStatusFinished(status)) {
                return;
            }

            getFragmentManager().popBackStack(MOVE_PROGRESS_DIALOG_BACKSTACK_TAG,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);

            // TODO: refresh ui

            if (status != PackageManager.MOVE_SUCCEEDED) {
                Log.d(TAG, "Move failure status: " + status);
                Toast.makeText(StorageResetActivity.this,
                        MoveAppProgressFragment.moveStatusToMessage(StorageResetActivity.this,
                                status),
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    // Non-null means we're in the process of formatting this volume
    private String mFormatAsPrivateDiskId;
    private String mFormatAsPrivateVolumeDesc;

    private static final int LOADER_FORMAT_AS_PRIVATE = 0;

    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAppMoveId = savedInstanceState != null ?
                savedInstanceState.getInt(SAVE_STATE_MOVE_ID) : -1;

        mPackageManager = getPackageManager();
        mPackageManager.registerMoveCallback(mMoveCallback, new Handler());

        mStorageManager = getSystemService(StorageManager.class);
        mStorageHeadersGetter.refreshView();

        if (savedInstanceState != null) {
            mFormatAsPrivateDiskId =
                    savedInstanceState.getString(SAVE_STATE_FORMAT_PRIVATE_DISK_ID);
            mFormatAsPrivateVolumeDesc =
                    savedInstanceState.getString(SAVE_STATE_FORMAT_PRIVATE_DISK_DESC);

            kickFormatAsPrivateLoader();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_STATE_MOVE_ID, mAppMoveId);
        outState.putString(SAVE_STATE_FORMAT_PRIVATE_DISK_ID, mFormatAsPrivateDiskId);
        outState.putString(SAVE_STATE_FORMAT_PRIVATE_DISK_DESC, mFormatAsPrivateVolumeDesc);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mStorageManager.registerListener(mStorageListener);
        for (StorageLayoutGetter getter : mStorageLayoutGetters.values()) {
            getter.startListening();
        }
    }

    @Override
    protected void onPause() {
        mStorageManager.unregisterListener(mStorageListener);
        for (StorageLayoutGetter getter : mStorageLayoutGetters.values()) {
            getter.stopListening();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPackageManager.unregisterMoveCallback(mMoveCallback);
    }

    @Override
    public Layout createLayout() {
        return new Layout().breadcrumb(getString(R.string.header_category_device))
                .add(new Header.Builder(getResources())
                        .icon(R.drawable.ic_settings_storage)
                        .title(R.string.device_storage_reset)
                        .build()
                        .add(mStorageHeadersGetter)
                        .add(new Static.Builder(getResources())
                                .title(R.string.storage_reset_section)
                                .build())
                        .add(createResetHeaders())
                );
    }

    private final Layout.LayoutGetter mStorageHeadersGetter = new Layout.LayoutGetter() {
        @Override
        public Layout get() {
            final Resources res = getResources();
            final Layout layout = new Layout();
            if (mStorageManager == null) {
                return layout;
            }
            final List<VolumeInfo> volumes = mStorageManager.getVolumes();
            Collections.sort(volumes, VolumeInfo.getDescriptionComparator());

            final List<VolumeInfo> privateVolumes = new ArrayList<>(volumes.size());
            final List<VolumeInfo> publicVolumes = new ArrayList<>(volumes.size());

            for (final VolumeInfo vol : volumes) {
                if (vol.getType() == VolumeInfo.TYPE_PRIVATE) {
                    privateVolumes.add(vol);
                } else if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                    publicVolumes.add(vol);
                } else {
                    Log.d(TAG, "Skipping volume " + vol.toString());
                }
            }
            if (privateVolumes.size() > 0) {
                layout.add(new Static.Builder(res)
                        .title(R.string.storage_device_storage_section)
                        .build());
            }
            for (final VolumeInfo vol : privateVolumes) {
                layout.add(getVolumeHeader(res, vol));
            }
            if (publicVolumes.size() > 0) {
                layout.add(new Static.Builder(res)
                        .title(R.string.storage_removable_storage_section)
                        .build());
            }
            for (final VolumeInfo vol : publicVolumes) {
                layout.add(getVolumeHeader(res, vol));
            }
            return layout;
        }

        private Header getVolumeHeader(Resources res, VolumeInfo vol) {
            final String volId = vol.getId();
            StorageLayoutGetter storageGetter = mStorageLayoutGetters.get(volId);
            if (storageGetter == null) {
                storageGetter = new StorageLayoutGetter(vol);
                mStorageLayoutGetters.put(volId, storageGetter);
                if (isResumed()) {
                    storageGetter.startListening();
                }
            }
            SizeStringGetter sizeGetter = mStorageDescriptionGetters.get(volId);
            if (sizeGetter == null) {
                sizeGetter = new SizeStringGetter();
                mStorageDescriptionGetters.put(volId, sizeGetter);
            }
            final File path = vol.getPath();
            if (path != null) {
                // TODO: something more dynamic here
                sizeGetter.setSize(path.getTotalSpace());
            }
            return new Header.Builder(res)
                    .title(mStorageManager.getBestVolumeDescription(vol))
                    .description(sizeGetter)
                    .build().add(storageGetter);
        }
    };

    private class StorageLayoutGetter extends Layout.LayoutGetter {

        private final String mVolumeId;
        private final String mVolumeDescription;

        private StorageMeasurement mMeasure;
        private final SizeStringGetter mAppsSize = new SizeStringGetter();
        private final SizeStringGetter mDcimSize = new SizeStringGetter();
        private final SizeStringGetter mMusicSize = new SizeStringGetter();
        private final SizeStringGetter mDownloadsSize = new SizeStringGetter();
        private final SizeStringGetter mCacheSize = new SizeStringGetter();
        private final SizeStringGetter mMiscSize = new SizeStringGetter();
        private final SizeStringGetter mAvailSize = new SizeStringGetter();

        private final MeasurementReceiver mReceiver = new MeasurementReceiver() {

            private MeasurementDetails mLastMeasurementDetails = null;

            @Override
            public void onDetailsChanged(MeasurementDetails details) {
                mLastMeasurementDetails = details;
                updateDetails(mLastMeasurementDetails);
            }
        };

        public StorageLayoutGetter(VolumeInfo volume) {
            mVolumeId = volume.getId();
            mVolumeDescription = mStorageManager.getBestVolumeDescription(volume);
        }

        @Override
        public Layout get() {
            final Resources res = getResources();
            final Layout layout = new Layout();

            final Bundle data = new Bundle(1);
            data.putString(VolumeInfo.EXTRA_VOLUME_ID, mVolumeId);

            final VolumeInfo volume = mStorageManager.findVolumeById(mVolumeId);

            if (volume == null) {
                layout
                        .add(new Status.Builder(res)
                                .title(R.string.storage_not_connected)
                                .build());
            } else if (volume.getType() == VolumeInfo.TYPE_PRIVATE) {
                if (!VolumeInfo.ID_PRIVATE_INTERNAL.equals(mVolumeId)) {
                    layout
                            .add(new Action.Builder(res, ACTION_EJECT_PRIVATE)
                                    .title(R.string.storage_eject)
                                    .data(data)
                                    .build())
                            .add(new Action.Builder(res, ACTION_ERASE_PRIVATE)
                                    .title(R.string.storage_format)
                                    .data(data)
                                    .build());
                }
                layout
                        .add(new Action.Builder(res,
                                new Intent(StorageResetActivity.this, AppsActivity.class))
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
                if (volume.getState() == VolumeInfo.STATE_UNMOUNTED) {
                    layout
                            .add(new Status.Builder(res)
                                    .title(getString(R.string.storage_unmount_success,
                                            mVolumeDescription))
                                    .build());
                } else {
                    layout
                            .add(new Action.Builder(res, ACTION_EJECT_PUBLIC)
                                    .title(R.string.storage_eject)
                                    .data(data)
                                    .build())
                            .add(new Action.Builder(res, ACTION_ERASE_PUBLIC)
                                    .title(R.string.storage_format_for_private)
                                    .data(data)
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
                }
            }
            return layout;
        }

        public void onVolumeUpdated() {
            stopListening();
            startListening();
            refreshView();
        }

        public void startListening() {
            final VolumeInfo volume = mStorageManager.findVolumeById(mVolumeId);
            if (volume != null && volume.isMountedReadable()) {
                final VolumeInfo sharedVolume = mStorageManager.findEmulatedForPrivate(volume);
                mMeasure = new StorageMeasurement(StorageResetActivity.this, volume,
                        sharedVolume);
                mMeasure.setReceiver(mReceiver);
                mMeasure.forceMeasure();
            }
        }

        public void stopListening() {
            if (mMeasure != null) {
                mMeasure.onDestroy();
            }
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
            case ACTION_EJECT_PUBLIC:
                new UnmountTask(this, mStorageManager.findVolumeById(
                        action.getData().getString(VolumeInfo.EXTRA_VOLUME_ID)))
                        .execute();
                break;
            case ACTION_EJECT_PRIVATE: {
                final Fragment f =
                        EjectInternalStepFragment.newInstance(mStorageManager.findVolumeById(
                                action.getData().getString(VolumeInfo.EXTRA_VOLUME_ID)));
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, f)
                        .addToBackStack(null)
                        .commit();
                break;
            }
            case ACTION_ERASE_PUBLIC: {
                // When we erase a public volume, we're intending to use it as a private volume,
                // so launch the format-as-private wizard.
                final Fragment f =
                        FormatAsPrivateStepFragment.newInstance(mStorageManager.findVolumeById(
                                action.getData().getString(VolumeInfo.EXTRA_VOLUME_ID)));
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, f)
                        .addToBackStack(FORMAT_DIALOG_BACKSTACK_TAG)
                        .commit();
                break;
            }
            case ACTION_ERASE_PRIVATE: {
                // When we erase a private volume, we're intending to use it as a public volume,
                // so launch the format-as-public wizard.
                final Fragment f =
                        FormatAsPublicStepFragment.newInstance(mStorageManager.findVolumeById(
                                action.getData().getString(VolumeInfo.EXTRA_VOLUME_ID)));
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, f)
                        .addToBackStack(FORMAT_DIALOG_BACKSTACK_TAG)
                        .commit();
            }
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

    public static class MountTask extends AsyncTask<Void, Void, Exception> {
        private final Context mContext;
        private final StorageManager mStorageManager;
        private final String mVolumeId;
        private final String mDescription;

        public MountTask(Context context, VolumeInfo volume) {
            mContext = context.getApplicationContext();
            mStorageManager = mContext.getSystemService(StorageManager.class);
            mVolumeId = volume.getId();
            mDescription = mStorageManager.getBestVolumeDescription(volume);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                mStorageManager.mount(mVolumeId);
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception e) {
            if (e == null) {
                Toast.makeText(mContext, mContext.getString(R.string.storage_mount_success,
                        mDescription), Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Failed to mount " + mVolumeId, e);
                Toast.makeText(mContext, mContext.getString(R.string.storage_mount_failure,
                        mDescription), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static class UnmountTask extends AsyncTask<Void, Void, Exception> {
        private final Context mContext;
        private final StorageManager mStorageManager;
        private final String mVolumeId;
        private final String mDescription;

        public UnmountTask(Context context, VolumeInfo volume) {
            mContext = context.getApplicationContext();
            mStorageManager = mContext.getSystemService(StorageManager.class);
            mVolumeId = volume.getId();
            mDescription = mStorageManager.getBestVolumeDescription(volume);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                mStorageManager.unmount(mVolumeId);
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception e) {
            if (e == null) {
                Toast.makeText(mContext, mContext.getString(R.string.storage_unmount_success,
                        mDescription), Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Failed to unmount " + mVolumeId, e);
                Toast.makeText(mContext, mContext.getString(R.string.storage_unmount_failure,
                        mDescription), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static class FormatAsPrivateTaskLoader
            extends SettingsAsyncTaskLoader<Map<String, Object>> {

        public static final String RESULT_EXCEPTION = "exception";
        public static final String RESULT_INTERNAL_BENCH = "internalBench";
        public static final String RESULT_PRIVATE_BENCH = "privateBench";

        private final StorageManager mStorageManager;
        private final String mDiskId;

        public FormatAsPrivateTaskLoader(Context context, String diskId) {
            super(context);
            mStorageManager = getContext().getSystemService(StorageManager.class);
            mDiskId = diskId;
        }

        @Override
        protected void onDiscardResult(Map<String, Object> result) {}

        @Override
        public Map<String, Object> loadInBackground() {
            final Map<String, Object> result = new ArrayMap<>(3);
            try {
                mStorageManager.partitionPrivate(mDiskId);
                final Long internalBench = mStorageManager.benchmark(null);
                result.put(RESULT_INTERNAL_BENCH, internalBench);

                final VolumeInfo privateVol = findVolume();
                if (privateVol != null) {
                    final Long externalBench = mStorageManager.benchmark(privateVol.getId());
                    result.put(RESULT_PRIVATE_BENCH, externalBench);
                }
            } catch (Exception e) {
                result.put(RESULT_EXCEPTION, e);
            }
            return result;
        }

        private VolumeInfo findVolume() {
            final List<VolumeInfo> vols = mStorageManager.getVolumes();
            for (final VolumeInfo vol : vols) {
                if (TextUtils.equals(mDiskId, vol.getDiskId())
                        && (vol.getType() == VolumeInfo.TYPE_PRIVATE)) {
                    return vol;
                }
            }
            return null;
        }
    }

    private class FormatAsPrivateLoaderCallback
            implements LoaderManager.LoaderCallbacks<Map<String, Object>> {

        private final String mDiskId;
        private final String mDescription;

        public FormatAsPrivateLoaderCallback(String diskId, String description) {
            mDiskId = diskId;
            mDescription = description;
        }

        @Override
        public Loader<Map<String, Object>> onCreateLoader(int id, Bundle args) {
            return new FormatAsPrivateTaskLoader(StorageResetActivity.this, mDiskId);
        }

        @Override
        public void onLoadFinished(Loader<Map<String, Object>> loader, Map<String, Object> data) {
            if (data == null) {
                // No results yet, wait for something interesting to come in.
                return;
            }

            final Exception e = (Exception) data.get(FormatAsPrivateTaskLoader.RESULT_EXCEPTION);
            if (e == null) {
                Toast.makeText(StorageResetActivity.this, getString(R.string.storage_format_success,
                        mDescription), Toast.LENGTH_SHORT).show();

                final Long internalBench =
                        (Long) data.get(FormatAsPrivateTaskLoader.RESULT_INTERNAL_BENCH);
                final Long privateBench =
                        (Long) data.get(FormatAsPrivateTaskLoader.RESULT_PRIVATE_BENCH);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isResumed() && TextUtils.equals(mDiskId, mFormatAsPrivateDiskId)) {
                            final boolean popped = getFragmentManager().popBackStackImmediate(
                                    FORMAT_DIALOG_BACKSTACK_TAG,
                                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
                            if (internalBench != null && privateBench != null) {
                                final float frac = (float) privateBench / (float) internalBench;
                                Log.d(TAG, "New volume is " + frac + "x the speed of internal");

                                // TODO: better threshold
                                if (popped && privateBench > 2000000000) {
                                    getFragmentManager().beginTransaction()
                                            .addToBackStack(null)
                                            .replace(android.R.id.content,
                                                    SlowDriveStepFragment.newInstance())
                                            .commit();
                                }

                                mFormatAsPrivateDiskId = null;
                                mFormatAsPrivateVolumeDesc = null;
                            }
                        }
                    }
                });

            } else {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isResumed() && TextUtils.equals(mDiskId, mFormatAsPrivateDiskId)) {
                            getFragmentManager().popBackStack(FORMAT_DIALOG_BACKSTACK_TAG,
                                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
                            mFormatAsPrivateDiskId = null;
                            mFormatAsPrivateVolumeDesc = null;
                        }
                    }
                });

                Log.e(TAG, "Failed to format " + mDiskId, e);
                Toast.makeText(StorageResetActivity.this, getString(R.string.storage_format_failure,
                        mDescription), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onLoaderReset(Loader<Map<String, Object>> loader) {}
    }

    public static class FormatAsPublicTask extends AsyncTask<Void, Void, Exception> {
        private final Context mContext;
        private final StorageManager mStorageManager;
        private final String mDiskId;
        private final String mDescription;

        public FormatAsPublicTask(Context context, VolumeInfo volume) {
            mContext = context.getApplicationContext();
            mStorageManager = mContext.getSystemService(StorageManager.class);
            mDiskId = volume.getDiskId();
            mDescription = mStorageManager.getBestVolumeDescription(volume);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                mStorageManager.partitionPublic(mDiskId);
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception e) {
            if (e == null) {
                Toast.makeText(mContext, mContext.getString(R.string.storage_format_success,
                        mDescription), Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Failed to format " + mDiskId, e);
                Toast.makeText(mContext, mContext.getString(R.string.storage_format_failure,
                        mDescription), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestMovePackageToVolume(String packageName, VolumeInfo destination) {
        mAppMoveId = mPackageManager.movePackage(packageName, destination);
        final ApplicationInfo applicationInfo;
        try {
            applicationInfo = mPackageManager
                    .getApplicationInfo(packageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }

        final MoveAppProgressFragment fragment = MoveAppProgressFragment
                .newInstance(mPackageManager.getApplicationLabel(applicationInfo));

        getFragmentManager().beginTransaction()
                .addToBackStack(MOVE_PROGRESS_DIALOG_BACKSTACK_TAG)
                .replace(android.R.id.content, fragment)
                .commit();

    }

    @Override
    public void onRequestFormatAsPrivate(VolumeInfo volumeInfo) {
        final FormattingProgressFragment fragment = FormattingProgressFragment.newInstance();
        getFragmentManager().popBackStack(FORMAT_DIALOG_BACKSTACK_TAG,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getFragmentManager().beginTransaction()
                .addToBackStack(FORMAT_DIALOG_BACKSTACK_TAG)
                .replace(android.R.id.content, fragment)
                .commit();

        mFormatAsPrivateDiskId = volumeInfo.getDiskId();
        mFormatAsPrivateVolumeDesc = mStorageManager.getBestVolumeDescription(volumeInfo);
        kickFormatAsPrivateLoader();
    }

    private void kickFormatAsPrivateLoader() {
        if (!TextUtils.isEmpty(mFormatAsPrivateDiskId)) {
            getLoaderManager().initLoader(LOADER_FORMAT_AS_PRIVATE, null,
                    new FormatAsPrivateLoaderCallback(mFormatAsPrivateDiskId,
                            mFormatAsPrivateVolumeDesc));
        }
    }

    @Override
    public void onCancelFormatDialog() {
        getFragmentManager().popBackStack(FORMAT_DIALOG_BACKSTACK_TAG,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }
}
