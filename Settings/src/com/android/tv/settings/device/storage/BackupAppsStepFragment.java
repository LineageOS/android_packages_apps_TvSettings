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

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.android.tv.settings.R;
import com.android.tv.settings.device.apps.AppInfo;
import com.android.settingslib.applications.ApplicationsState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BackupAppsStepFragment extends GuidedStepFragment implements
        ApplicationsState.Callbacks {

    private static final String TAG = "BackupAppsStepFragment";

    private ApplicationsState mApplicationsState;
    private ApplicationsState.Session mSession;

    private PackageManager mPackageManager;
    private StorageManager mStorageManager;

    private String mVolumeId;

    private IconLoaderTask mIconLoaderTask;
    private final Map<String, Drawable> mIconMap = new ArrayMap<>();

    private final List<ApplicationInfo> mInfos = new ArrayList<>();

    public static BackupAppsStepFragment newInstance(VolumeInfo volumeInfo) {
        final BackupAppsStepFragment fragment = new BackupAppsStepFragment();
        final Bundle b = new Bundle(1);
        b.putString(VolumeInfo.EXTRA_VOLUME_ID, volumeInfo.getId());
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Need mPackageManager before onCreateActions, which is called from super.onCreate
        mPackageManager = getActivity().getPackageManager();
        mStorageManager = getActivity().getSystemService(StorageManager.class);

        mVolumeId = getArguments().getString(VolumeInfo.EXTRA_VOLUME_ID);

        mApplicationsState = ApplicationsState.getInstance(getActivity().getApplication());
        mSession = mApplicationsState.newSession(this);

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSession.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSession.pause();
    }

    @Override
    public @NonNull GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        final String title;
        final VolumeInfo volumeInfo = mStorageManager.findVolumeById(mVolumeId);
        final String volumeDesc = mStorageManager.getBestVolumeDescription(volumeInfo);
        final String primaryStorageVolumeId =
                mPackageManager.getPrimaryStorageCurrentVolume().getId();
        if (TextUtils.equals(primaryStorageVolumeId, volumeInfo.getId())) {
            title = getString(R.string.storage_wizard_back_up_apps_and_data_title, volumeDesc);
        } else {
            title = getString(R.string.storage_wizard_back_up_apps_title, volumeDesc);
        }
        return new GuidanceStylist.Guidance(
                title,
                "",
                "",
                getActivity().getDrawable(R.drawable.ic_settings_storage));
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        actions.addAll(getAppActions(true));
    }

    private List<GuidedAction> getAppActions(boolean refreshIcons) {
        final List<ApplicationInfo> infos = mPackageManager.getInstalledApplications(0);
        final List<ApplicationInfo> usedInfos = new ArrayList<>(infos.size());

        final List<GuidedAction> actions = new ArrayList<>(infos.size());
        for (final ApplicationInfo info : infos) {
            final ApplicationsState.AppEntry entry =
                    mApplicationsState.getEntry(info.packageName,  UserHandle.getUserId(info.uid));
            final VolumeInfo installedVolume = mPackageManager.getPackageCurrentVolume(info);

            if (entry == null || installedVolume == null ||
                    !TextUtils.equals(installedVolume.getId(), mVolumeId)) {
                continue;
            }
            final int index = usedInfos.size();
            usedInfos.add(info);
            final AppInfo appInfo = new AppInfo(getActivity(), entry);
            actions.add(new GuidedAction.Builder()
                    .title(appInfo.getName())
                    .description(appInfo.getSize())
                    .icon(mIconMap.get(info.packageName))
                    .id(index)
                    .build());
        }
        mInfos.clear();
        mInfos.addAll(usedInfos);

        if (refreshIcons) {
            if (mIconLoaderTask != null) {
                mIconLoaderTask.cancel(true);
            }
            mIconLoaderTask = new IconLoaderTask(usedInfos);
            mIconLoaderTask.execute();
        }
        return actions;
    }

    private void updateActions() {
        setActions(getAppActions(true));
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        final int actionId = (int) action.getId();
        final ApplicationInfo info = mInfos.get(actionId);
        final AppInfo appInfo = new AppInfo(getActivity(), mApplicationsState.getEntry(
                info.packageName,  UserHandle.getUserId(info.uid)));

        final MoveAppStepFragment fragment = MoveAppStepFragment.newInstance(info.packageName,
                appInfo.getName());
        getFragmentManager().beginTransaction()
                .addToBackStack(null)
                .replace(android.R.id.content, fragment)
                .commit();
    }

    @Override
    public void onRunningStateChanged(boolean running) {}

    @Override
    public void onPackageListChanged() {
        updateActions();
    }

    @Override
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {

    }

    @Override
    public void onLauncherInfoChanged() {

    }

    @Override
    public void onLoadEntriesCompleted() {

    }

    @Override
    public void onPackageIconChanged() {
        updateActions();
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
        updateActions();
    }

    @Override
    public void onAllSizesComputed() {
        updateActions();
    }

    private class IconLoaderTask extends AsyncTask<Void, Void, Map<String, Drawable>> {
        private final List<ApplicationInfo> mInfos;

        public IconLoaderTask(List<ApplicationInfo> infos) {
            mInfos = infos;
        }

        @Override
        protected Map<String, Drawable> doInBackground(Void... params) {
            // NB: Java doesn't like parameterized generics in varargs
            final Map<String, Drawable> result = new ArrayMap<>(mInfos.size());
            for (final ApplicationInfo info : mInfos) {
                result.put(info.packageName, mPackageManager.getApplicationIcon(info));
            }
            return result;
        }

        @Override
        protected void onPostExecute(Map<String, Drawable> stringDrawableMap) {
            mIconMap.putAll(stringDrawableMap);
            setActions(getAppActions(false));
            mIconLoaderTask = null;
        }
    }

}
