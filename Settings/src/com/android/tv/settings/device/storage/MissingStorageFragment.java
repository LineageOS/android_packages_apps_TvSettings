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

package com.android.tv.settings.device.storage;

import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeRecord;
import android.text.TextUtils;
import android.util.Log;

import androidx.leanback.preference.LeanbackPreferenceFragmentCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import android.content.Intent;

import com.android.tv.settings.R;
import com.android.tv.settings.overlay.FlavorUtils;
import com.android.tv.settings.device.StorageResetActivity;
public class MissingStorageFragment extends LeanbackPreferenceFragmentCompat {

    private static final String TAG = "MissingStorageFragment";

    private static final String KEY_FORGET = "forget";

    private String mFsUuid;
    private StorageManager mStorageManager;
    private boolean isTwoPanel;
    public static void prepareArgs(Bundle b, String fsUuid) {
        b.putString(VolumeRecord.EXTRA_FS_UUID, fsUuid);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mFsUuid = getArguments().getString(VolumeRecord.EXTRA_FS_UUID);
        mStorageManager = getContext().getSystemService(StorageManager.class);
        mStorageManager.registerListener(new StorageEventListener());
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mStorageManager.findRecordByUuid(mFsUuid) == null) {
            isTwoPanel = FlavorUtils.isTwoPanel(getContext());
            Log.i(TAG, "FsUuid " + mFsUuid + " vanished upon resuming");
            if(isTwoPanel){
                startActivity(new Intent(getActivity(), StorageResetActivity.class));
                getActivity().finish();
            }else{
                getFragmentManager().popBackStack();
            }
        } else {
            refresh();
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.missing_storage, null);

        final VolumeRecord record = mStorageManager.findRecordByUuid(mFsUuid);

        final PreferenceScreen screen = getPreferenceScreen();
        screen.setTitle(record != null ? record.getNickname() : null);
    }

    private void refresh() {
        final Preference forget = findPreference(KEY_FORGET);
        forget.setFragment(ForgetPrivateConfirmFragment.class.getName());
        ForgetPrivateConfirmFragment.prepareArgs(forget.getExtras(), mFsUuid);
    }

    private class StorageEventListener extends android.os.storage.StorageEventListener {

        @Override
        public void onVolumeForgotten(String fsUuid) {
            if (!TextUtils.equals(fsUuid, mFsUuid) || !isResumed()) {
                return;
            }
            if (mStorageManager.findRecordByUuid(fsUuid) == null) {
                isTwoPanel = FlavorUtils.isTwoPanel(getContext());
                Log.i(TAG, "FsUuid " + mFsUuid + " vanished while resumed");
                if(isTwoPanel)
                    onDestroy();
                else
                    getFragmentManager().popBackStack();
            } else {
                refresh();
            }
        }

        @Override
        public void onVolumeRecordChanged(VolumeRecord rec) {
            if (TextUtils.equals(rec.getFsUuid(), mFsUuid) && isResumed()) {
                refresh();
            }
        }
    }
}
