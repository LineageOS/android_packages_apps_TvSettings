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

package com.android.tv.settings.device;

import android.content.Context;
import android.os.Bundle;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.support.v17.preference.LeanbackPreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.util.Log;

import com.android.tv.settings.R;
import com.android.tv.settings.device.storage.MissingStorageFragment;
import com.android.tv.settings.device.storage.NewStorageActivity;
import com.android.tv.settings.device.storage.StorageFragment;
import com.android.tv.settings.device.storage.StoragePreference;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StorageResetFragment extends LeanbackPreferenceFragment {

    private static final String TAG = "StorageResetFragment";

    private static final String KEY_DEVICE_CATEGORY = "device_storage";
    private static final String KEY_REMOVABLE_CATEGORY = "removable_storage";

    private StorageManager mStorageManager;
    private final StorageEventListener mStorageEventListener = new StorageEventListener();

    public static StorageResetFragment newInstance() {
        return new StorageResetFragment();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        mStorageManager = getContext().getSystemService(StorageManager.class);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.storage_reset, null);
    }

    @Override
    public void onStart() {
        super.onStart();
        mStorageManager.registerListener(mStorageEventListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onStop() {
        super.onStop();
        mStorageManager.unregisterListener(mStorageEventListener);
    }

    private void refresh() {
        final Context themedContext = getPreferenceManager().getContext();

        final List<VolumeInfo> volumes = mStorageManager.getVolumes();
        Collections.sort(volumes, VolumeInfo.getDescriptionComparator());

        final List<VolumeInfo> privateVolumes = new ArrayList<>(volumes.size());
        final List<VolumeInfo> publicVolumes = new ArrayList<>(volumes.size());

        // Find mounted volumes
        for (final VolumeInfo vol : volumes) {
            if (vol.getType() == VolumeInfo.TYPE_PRIVATE) {
                privateVolumes.add(vol);
            } else if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                publicVolumes.add(vol);
            } else {
                Log.d(TAG, "Skipping volume " + vol.toString());
            }
        }

        // Find missing private filesystems
        final List<VolumeRecord> volumeRecords = mStorageManager.getVolumeRecords();
        final List<VolumeRecord> privateMissingVolumes = new ArrayList<>(volumeRecords.size());

        for (final VolumeRecord record : volumeRecords) {
            if (record.getType() == VolumeInfo.TYPE_PRIVATE
                    && mStorageManager.findVolumeByUuid(record.getFsUuid()) == null) {
                privateMissingVolumes.add(record);
            }
        }

        // Find unreadable disks
        final List<DiskInfo> disks = mStorageManager.getDisks();
        final List<DiskInfo> unsupportedDisks = new ArrayList<>(disks.size());
        for (final DiskInfo disk : disks) {
            if (disk.volumeCount == 0 && disk.size > 0) {
                unsupportedDisks.add(disk);
            }
        }

        // Add the prefs
        final PreferenceCategory deviceCategory =
                (PreferenceCategory) findPreference(KEY_DEVICE_CATEGORY);
        deviceCategory.removeAll();

        for (final VolumeInfo volumeInfo : privateVolumes) {
            deviceCategory.addPreference(new VolPreference(themedContext, volumeInfo));
        }

        for (final VolumeRecord volumeRecord : privateMissingVolumes) {
            deviceCategory.addPreference(new MissingPreference(themedContext, volumeRecord));
        }

        final PreferenceCategory removableCategory =
                (PreferenceCategory) findPreference(KEY_REMOVABLE_CATEGORY);
        removableCategory.removeAll();
        // Only show section if there are public/unknown volumes present
        removableCategory.setVisible(publicVolumes.size() + unsupportedDisks.size() > 0);

        for (final VolumeInfo volumeInfo : publicVolumes) {
            removableCategory.addPreference(new VolPreference(themedContext, volumeInfo));
        }
        for (final DiskInfo diskInfo : unsupportedDisks) {
            removableCategory.addPreference(new UnsupportedDiskPreference(themedContext, diskInfo));
        }
    }

    private String getSizeString(VolumeInfo vol) {
        final File path = vol.getPath();
        if (vol.isMountedReadable() && path != null) {
            return String.format(getString(R.string.storage_size),
                    StoragePreference.formatSize(getContext(), path.getTotalSpace()));
        } else {
            return null;
        }
    }

    private class VolPreference extends Preference {
        private final VolumeInfo mVolumeInfo;

        public VolPreference(Context context, VolumeInfo volumeInfo) {
            super(context);
            mVolumeInfo = volumeInfo;
            final String description = mStorageManager
                    .getBestVolumeDescription(mVolumeInfo);
            setTitle(description);
            if (mVolumeInfo.isMountedReadable()) {
                setSummary(getSizeString(mVolumeInfo));
                setFragment(StorageFragment.class.getName());
                StorageFragment.prepareArgs(getExtras(), mVolumeInfo);
            } else {
                setSummary(getString(R.string.storage_unmount_success, description));
            }
        }
    }

    private class MissingPreference extends Preference {
        public MissingPreference(Context context, VolumeRecord volumeRecord) {
            super(context);
            setTitle(volumeRecord.getNickname());
            setSummary(R.string.storage_not_connected);
            setFragment(MissingStorageFragment.class.getName());
            MissingStorageFragment.prepareArgs(getExtras(), volumeRecord.getFsUuid());
        }
    }

    private class UnsupportedDiskPreference extends Preference {
        public UnsupportedDiskPreference(Context context, DiskInfo info) {
            super(context);
            setTitle(info.getDescription());
            setIntent(NewStorageActivity.getNewStorageLaunchIntent(context, null, info.getId()));
        }
    }

    private class StorageEventListener extends android.os.storage.StorageEventListener {
        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            refresh();
        }

        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            refresh();
        }

        @Override
        public void onVolumeRecordChanged(VolumeRecord rec) {
            refresh();
        }

        @Override
        public void onVolumeForgotten(String fsUuid) {
            refresh();
        }

        @Override
        public void onDiskScanned(DiskInfo disk, int volumeCount) {
            refresh();
        }

        @Override
        public void onDiskDestroyed(DiskInfo disk) {
            refresh();
        }

    }

}
