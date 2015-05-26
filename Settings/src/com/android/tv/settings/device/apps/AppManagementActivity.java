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

package com.android.tv.settings.device.apps;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.Log;
import android.widget.Toast;

import com.android.tv.settings.ActionBehavior;
import com.android.tv.settings.ActionKey;
import com.android.tv.settings.R;
import com.android.tv.settings.SettingsConstant;
import com.android.tv.settings.device.storage.MoveAppProgressFragment;
import com.android.tv.settings.device.storage.MoveAppStepFragment;
import com.android.tv.settings.dialog.old.Action;
import com.android.tv.settings.dialog.old.ActionAdapter;
import com.android.tv.settings.dialog.old.ActionFragment;
import com.android.tv.settings.dialog.old.ContentFragment;
import com.android.tv.settings.dialog.old.DialogActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Activity that manages an apps.
 */
public class AppManagementActivity extends DialogActivity implements ActionAdapter.Listener,
        ApplicationsState.Callbacks, DataClearer.Listener, CacheClearer.Listener,
        DefaultClearer.Listener, MoveAppStepFragment.Callback {

    private static final String TAG = "AppManagementActivity";

    private static final String DIALOG_BACKSTACK_TAG = "storageUsed";

    private static final String SAVE_STATE_MOVE_ID = "AppManagementActivity.moveId";

    // Result code identifiers
    private static final int REQUEST_UNINSTALL = 1;
    private static final int REQUEST_MANAGE_SPACE = 2;

    private PackageManager mPackageManager;
    private StorageManager mStorageManager;
    private String mPackageName;
    private ApplicationsState mApplicationsState;
    private ApplicationsState.Session mSession;
    private AppInfo mAppInfo;
    private OpenManager mOpenManager;
    private ForceStopManager mForceStopManager;
    private UninstallManager mUninstallManager;
    private NotificationSetter mNotificationSetter;
    private DataClearer mDataClearer;
    private DefaultClearer mDefaultClearer;
    private CacheClearer mCacheClearer;
    private ActionFragment mActionFragment;

    private int mAppMoveId;
    private final PackageManager.MoveCallback mMoveCallback = new PackageManager.MoveCallback() {
        @Override
        public void onStatusChanged(int moveId, int status, long estMillis) {
            if (moveId != mAppMoveId || !PackageManager.isMoveStatusFinished(status)) {
                return;
            }

            getFragmentManager().popBackStack(DIALOG_BACKSTACK_TAG,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
            // TODO: this isn't quite enough to refresh the UI
            mApplicationsState.invalidatePackage(mPackageName);
            updateActions();

            if (status != PackageManager.MOVE_SUCCEEDED) {
                Log.d(TAG, "Move failure status: " + status);
                Toast.makeText(AppManagementActivity.this,
                        MoveAppProgressFragment.moveStatusToMessage(AppManagementActivity.this,
                                status),
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPackageManager = getPackageManager();
        mStorageManager = getSystemService(StorageManager.class);
        final Uri uri = getIntent().getData();
        if (uri == null) {
            Log.e(TAG, "No app to inspect (missing data uri in intent)");
            finish();
            return;
        }
        mPackageName = uri.getSchemeSpecificPart();
        mApplicationsState = ApplicationsState.getInstance(getApplication());
        mSession = mApplicationsState.newSession(this);
        mSession.resume();
        mAppInfo = new AppInfo(this, mApplicationsState.getEntry(mPackageName));
        mOpenManager = new OpenManager(this, mAppInfo);
        mForceStopManager = new ForceStopManager(this, mAppInfo);
        mUninstallManager = new UninstallManager(this, mAppInfo);
        mNotificationSetter = new NotificationSetter(mAppInfo);
        mDataClearer = new DataClearer(this, mAppInfo);
        mDefaultClearer = new DefaultClearer(this, mAppInfo);
        mCacheClearer = new CacheClearer(this, mAppInfo);
        mActionFragment = ActionFragment.newInstance(getActions());

        mAppMoveId = savedInstanceState != null ?
                savedInstanceState.getInt(SAVE_STATE_MOVE_ID) : -1;

        setContentAndActionFragments(ContentFragment.newInstance(mAppInfo.getName(),
                getString(R.string.device_apps),
                getString(R.string.device_apps_app_management_version, mAppInfo.getVersion()),
                Uri.parse(AppsBrowseInfo.getAppIconUri(this, mAppInfo)),
                getColor(R.color.icon_background)), mActionFragment);

        mPackageManager.registerMoveCallback(mMoveCallback, new Handler());
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(SAVE_STATE_MOVE_ID, mAppMoveId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPackageManager.unregisterMoveCallback(mMoveCallback);
    }

    public static Intent getLaunchIntent(String packageName) {
        Intent i = new Intent();
        i.setComponent(new ComponentName(SettingsConstant.PACKAGE,
                SettingsConstant.PACKAGE + ".device.apps.AppManagementActivity"));
        i.setData(Uri.parse("package:" + packageName));
        return i;
    }

    private void refreshUpdateActions() {
        mApplicationsState = ApplicationsState.getInstance(getApplication());
        mAppInfo = new AppInfo(this, mApplicationsState.getEntry(mPackageName));
        mUninstallManager = new UninstallManager(this, mAppInfo);
        updateActions();
    }

    static class DisableChanger extends AsyncTask<Void, Void, Void> {
        final PackageManager mPm;
        final WeakReference<AppManagementActivity> mActivity;
        final ApplicationInfo mInfo;
        final int mState;

        DisableChanger(AppManagementActivity activity, ApplicationInfo info, int state) {
            mPm = activity.getPackageManager();
            mActivity = new WeakReference<>(activity);
            mInfo = info;
            mState = state;
        }

        @Override
        protected Void doInBackground(Void... params) {
            mPm.setApplicationEnabledSetting(mInfo.packageName, mState, 0);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            AppManagementActivity activity = mActivity.get();
            if (activity != null) {
                activity.refreshUpdateActions();
            }
        }
    }

    @Override
    public void onActionClicked(Action action) {
        ActionKey<ActionType, ActionBehavior> actionKey = new ActionKey<>(
                ActionType.class, ActionBehavior.class, action.getKey());
        ActionType actionType = actionKey.getType();

        switch (actionKey.getBehavior()) {
            case INIT:
                onInit(actionType, action);
                break;
            case OK:
                onOk(actionType);
                break;
            case CANCEL:
                onCancel(actionType);
                break;
            case ON:
                onOn(actionType);
                break;
            case OFF:
                onOff(actionType);
                break;
            default:
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_UNINSTALL:
                if (resultCode == RESULT_OK) {
                    mApplicationsState.removePackage(mPackageName);
                    goToAppSelectScreen();
                }
                break;
            case REQUEST_MANAGE_SPACE:
                mDataClearer.onActivityResult(resultCode);
                break;
        }
    }

    @Override
    public void onRunningStateChanged(boolean running) {
    }

    @Override
    public void onPackageListChanged() {
    }

    @Override
    public void onRebuildComplete() {
    }

    @Override
    public void onPackageIconChanged() {
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
    }

    @Override
    public void onAllSizesComputed() {
        updateActions();
    }

    @Override
    public void dataCleared(boolean succeeded) {
        if (succeeded) {
            mApplicationsState.requestSize(mPackageName);
        } else {
            Log.w(TAG, "Failed to clear data!");
            updateActions();
        }
    }

    @Override
    public void defaultCleared() {
        updateActions();
    }

    @Override
    public void cacheCleared(boolean succeeded) {
        if (succeeded) {
            mApplicationsState.requestSize(mPackageName);
        } else {
            Log.w(TAG, "Failed to clear cache!");
            updateActions();
        }
    }

    private void onInit(ActionType actionType, Action action) {
        switch (actionType) {
            case OPEN:
                onOpen();
                break;
            case PERMISSIONS:
                startManagePermissionsActivity();
                break;
            case NOTIFICATIONS:
                setContentAndActionFragments(createContentFragment(actionType,
                        action), ActionFragment.newInstance(actionType.toSelectableActions(
                        getResources(),
                        (mNotificationSetter.areNotificationsOn()) ? ActionBehavior.ON
                                : ActionBehavior.OFF)));
                break;
            case STORAGE_USED:
                startDialogFragment(MoveAppStepFragment.newInstance(mPackageName,
                        mAppInfo.getName()));
                break;
            default:
                setContentAndActionFragments(createContentFragment(actionType, action),
                        ActionFragment.newInstance(actionType.toActions(getResources())));
                break;
        }
    }

    private void startManagePermissionsActivity() {
        // start new activity to manage app permissions
        Intent intent = new Intent(Intent.ACTION_MANAGE_APP_PERMISSIONS);
        intent.putExtra(Intent.EXTRA_PACKAGE_NAME, mPackageName);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "No app can handle android.intent.action.MANAGE_APP_PERMISSIONS");
        }
    }

    private ContentFragment createContentFragment(ActionType actionType, Action action) {
        String description = actionType.getDesc(getResources());
        String description2 = actionType.getDesc2(getResources());
        String descriptionToUse = null;
        if (description != null) {
            if (description2 != null) {
                descriptionToUse = description + "\n" + description2;
            } else {
                descriptionToUse = description;
            }
        } else if (description2 != null) {
            descriptionToUse = description2;
        }
        return ContentFragment.newInstance(action.getTitle(), mAppInfo.getName(), descriptionToUse,
                Uri.parse(AppsBrowseInfo.getAppIconUri(this, mAppInfo)),
                getColor(R.color.icon_background));
    }

    private void onOk(ActionType actionType) {
        switch (actionType) {
            case CLEAR_CACHE:
                onClearCacheOk();
                break;
            case CLEAR_DATA:
                onClearDataOk();
                break;
            case CLEAR_DEFAULTS:
                onClearDefaultOk();
                break;
            case FORCE_STOP:
                onForceStopOk();
                break;
            case UNINSTALL:
                onUninstallOk();
                break;
            case DISABLE:
                onDisableOk();
                break;
            case ENABLE:
                onEnableOk();
                break;
            default:
                break;
        }
    }

    private void onCancel(ActionType actionType) {
        goToActionSelectScreen();
    }

    private void onOn(ActionType actionType) {
        switch (actionType) {
            case NOTIFICATIONS:
                onNotificationsOn();
                break;
            default:
                break;
        }
    }

    private void onOff(ActionType actionType) {
        switch (actionType) {
            case NOTIFICATIONS:
                onNotificationsOff();
                break;
            default:
                break;
        }
    }

    private void onUninstallOk() {
        mUninstallManager.uninstall(REQUEST_UNINSTALL);
    }

    private void onDisableOk() {
        new DisableChanger(this, mAppInfo.getApplicationInfo(),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER).execute();
        goToActionSelectScreen();
    }

    private void onEnableOk() {
        new DisableChanger(this, mAppInfo.getApplicationInfo(),
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT).execute();
        goToActionSelectScreen();
    }

    private void onNotificationsOn() {
        if (!mNotificationSetter.enableNotifications()) {
            Log.w(TAG, "Failed to enable notifications!");
        }
        goToActionSelectScreen();
    }

    private void onNotificationsOff() {
        if (!mNotificationSetter.disableNotifications()) {
            Log.w(TAG, "Failed to disable notifications!");
        }
        goToActionSelectScreen();
    }

    private void onOpen() {
        mOpenManager.open(mApplicationsState);
        // TODO: figure out what to do here
    }

    private void onForceStopOk() {
        mForceStopManager.forceStop(mApplicationsState);
        goToActionSelectScreen();
    }

    private void onClearDataOk() {
        mDataClearer.clearData(this, REQUEST_MANAGE_SPACE);
        goToActionSelectScreen();
    }

    private void onClearDefaultOk() {
        mDefaultClearer.clearDefault(this);
        goToActionSelectScreen();
    }

    private void onClearCacheOk() {
        mCacheClearer.clearCache(this);
        goToActionSelectScreen();
    }

    private void goToActionSelectScreen() {
        updateActions();
        getFragmentManager().popBackStack(null, 0);
    }

    private void goToAppSelectScreen() {
        finish();
    }

    private ArrayList<Action> getActions() {
        ArrayList<Action> actions = new ArrayList<>();

        final Resources res = getResources();
        if (mOpenManager.canOpen()) {
            actions.add(ActionType.OPEN.toInitAction(res));
        }
        if (mForceStopManager.canForceStop()) {
            actions.add(ActionType.FORCE_STOP.toInitAction(res));
        }
        if (mUninstallManager.canUninstall()) {
            actions.add(ActionType.UNINSTALL.toInitAction(res));
        } else {
            // App is on system partition.
            if (mUninstallManager.canDisable()) {
                if (mUninstallManager.isEnabled()) {
                    actions.add(ActionType.DISABLE.toInitAction(res));
                } else {
                    actions.add(ActionType.ENABLE.toInitAction(res));
                }
            }
        }
        actions.add(ActionType.STORAGE_USED.toInitAction(res, getStorageDescription()));
        actions.add( ActionType.CLEAR_DATA.toInitAction(res, mDataClearer.getDataSize(this)));
        actions.add(ActionType.CLEAR_CACHE.toInitAction(res, mCacheClearer.getCacheSize(this)));
        actions.add(ActionType.CLEAR_DEFAULTS.toInitAction(res,
                mDefaultClearer.getDescription(this)));
        actions.add(ActionType.NOTIFICATIONS.toInitAction(res,
                getString((mNotificationSetter.areNotificationsOn()) ? R.string.settings_on
                        : R.string.settings_off)));
        actions.add(ActionType.PERMISSIONS.toInitAction(res));

        return actions;
    }

    private String getStorageDescription() {
        final ApplicationInfo applicationInfo = mAppInfo.getApplicationInfo();
        final VolumeInfo volumeInfo = mPackageManager.getPackageCurrentVolume(applicationInfo);
        final String volumeDesc = mStorageManager.getBestVolumeDescription(volumeInfo);
        return getString(R.string.device_apps_app_management_storage_used_desc, mAppInfo.getSize(),
                volumeDesc);
    }

    private void updateActions() {
        ((ActionAdapter) mActionFragment.getAdapter()).setActions(getActions());
    }

    @Override
    public void onRequestMovePackageToVolume(String packageName, VolumeInfo destination) {
        // Kick off the move
        mAppMoveId = mPackageManager.movePackage(packageName, destination);
        // Show the progress dialog
        startDialogFragment(MoveAppProgressFragment.newInstance(mAppInfo.getName()));
    }

    private void startDialogFragment(Fragment fragment) {
        // Get rid of any previous wizard screen(s)
        getFragmentManager().popBackStack(DIALOG_BACKSTACK_TAG,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        // Replace it with the progress screen
        getFragmentManager().beginTransaction()
                .addToBackStack(DIALOG_BACKSTACK_TAG)
                .hide(getDialogFragment())
                .hide(getContentFragment())
                .hide(getActionFragment())
                .add(android.R.id.content, fragment)
                .commit();
    }
}
