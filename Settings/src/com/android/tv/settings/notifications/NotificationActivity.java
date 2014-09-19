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

package com.android.tv.settings.notifications;

import com.android.tv.settings.R;
import com.android.tv.settings.dialog.old.Action;
import com.android.tv.settings.dialog.old.ActionFragment;
import com.android.tv.settings.dialog.old.ContentFragment;
import com.android.tv.settings.dialog.old.DialogActivity;

import android.app.ActivityManager;
import android.app.INotificationManager;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.service.notification.INotificationListener;
import android.service.notification.IStatusBarNotificationHolder;
import android.service.notification.NotificationRankingUpdate;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.android.tv.settings.dialog.old.ScrollAdapterFragment;

import java.util.ArrayList;
import java.util.List;

/*
 * Note: The entries in the ActionFragment of this activity shall be ordered as following:
 * 1- "Dismiss All" action, or "None" if no notifictaions are present
 * 2- Most recent notification (Selected by delault on resume)
 * 3.  and beyond: Other notifications in descending order (newest to oldest)
 */

/**
 * Notification activity which displays Android system notifications.
 */
public class NotificationActivity extends DialogActivity
        implements NotificationAdapter.NotificationOnClickListener, DismissAllListItem.Listener {

    private static final String TAG = "NotificationActivity";
    private static final boolean DEBUG = false;

    private int mMaxNotificationCount;
    private String mMaxNotificationText;
    private int mCurrentUser;
    private SparseArray<ActiveNotificationInfo> mNotificationMap;
    private NotificationAdapter mAdapter;
    private INotificationManager mNoMan;
    private INotificationListener.Stub mListener;
    private DismissAllListItem mDismissAllListItem;
    private NoNotificationsListItem mNoNotificationsListItem;
    private NotificationAdapterFragment mNotificationListFragment;
    private ActiveNotificationInfo mLastNotificationSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMaxNotificationCount = getResources()
                .getInteger(android.R.integer.status_bar_notification_info_maxnum);
        mMaxNotificationText = getString(android.R.string.status_bar_notification_info_overflow);

        mCurrentUser = ActivityManager.getCurrentUser();
        mNotificationMap = new SparseArray<ActiveNotificationInfo>();
        mListener = new INotificationListener.Stub() {
            @Override
            public void onListenerConnected(NotificationRankingUpdate rankingUpdate)
                    throws RemoteException {
                // noop
            }
            @Override
            public void onNotificationPosted(IStatusBarNotificationHolder sbnHolder,
                    NotificationRankingUpdate rankingUpdate) throws RemoteException {
                NotificationActivity.this.onNotificationPosted(sbnHolder.get());
            }

            @Override
            public void onNotificationRemoved(IStatusBarNotificationHolder sbnHolder,
                    NotificationRankingUpdate rankingUpdate) throws RemoteException {
                NotificationActivity.this.onNotificationRemoved(sbnHolder.get());
            }

            @Override
            public void onNotificationRankingUpdate(NotificationRankingUpdate rankingUpdate)
                    throws RemoteException {
                // noop
            }

            @Override
            public void onListenerHintsChanged(int hints) throws RemoteException {
                // noop
            }

            @Override
            public void onInterruptionFilterChanged(int interruptionFilter) throws RemoteException {
               // noop
            }
        };

        mNoMan = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        mAdapter = new NotificationAdapter(this, this);
        mDismissAllListItem = new DismissAllListItem(
                getString(R.string.dismiss_all_notifications), this);
        mNoNotificationsListItem = new NoNotificationsListItem(
                getString(R.string.no_notifications));
        mNotificationListFragment = new NotificationAdapterFragment();

        setContentAndActionFragments(ContentFragment.newInstance(
                getString(R.string.cluster_name_notifications), null, null,
                R.drawable.ic_settings_notification,
                getResources().getColor(R.color.icon_background)), mNotificationListFragment);

    }

    @Override
    protected void onResume() {
        super.onResume();

        mAdapter.clear();
        mNotificationMap.clear();

        List<ActiveNotificationInfo> notifications = loadNotifications();
        if (notifications != null && !notifications.isEmpty()) {
            for (ActiveNotificationInfo notification : notifications) {
                mNotificationMap.put(notification.getId(), notification);
                mAdapter.add(notification);
            }
        }
        addOrRemoveSpecialActions();

        try {
            mNoMan.registerListener(mListener,
                    new ComponentName(getPackageName(), this.getClass().getCanonicalName()),
                    mCurrentUser);
        } catch (RemoteException e) {
            // well, that didn't work out
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            mNoMan.unregisterListener(mListener, mCurrentUser);
        } catch (RemoteException e) {
            // well, that didn't work out
        }
    }

    @Override
    public void onClick(NotificationListItem item) {
        if (item instanceof ActiveNotificationInfo) {
            ActiveNotificationInfo info = (ActiveNotificationInfo) item;
            mLastNotificationSelected = info;
            if (info.isDismissableOnSelect()) {
                cancelNotification(info);
            }

            if (info.hasBigCustomView() || info.hasSmallCustomView()) {
                // If custom view set, go to navigable custom view.
                setActionFragment(info.createDisplayFragment());
            } else if (info.hasActions()) {
                // If we have actions, go to action list.
                setContentAndActionFragments(info.createContentFragment(),
                        info.createActionListFragment());
            } else {
                // If no custom views or actions, launch custom intent or source app.
                info.launch(this);
            }
        } else if (item instanceof CustomListItem) {
            CustomListItem customListItem = (CustomListItem) item;
            if (customListItem.isLaunchable()) {
                customListItem.launch(this);
            } else {
                // Go to navigable custom view
                setActionFragment(customListItem.createDisplayFragment());
            }
        } else {
            item.launch(this);
        }
    }

    @Override
    public void dismissAllSelected() {
        try {
            mNoMan.cancelNotificationsFromListener(mListener, null);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActionClicked(Action action) {
        getFragmentManager().popBackStack();
    }

    public NotificationAdapter getNotificationAdapter() {
        return mAdapter;
    }

    private void cancelNotification(ActiveNotificationInfo info) {
        info.cancel(mNoMan);
    }

    private boolean shouldBeIgnored(StatusBarNotification sbn) {
        // notification types that we want to ignore for TV
        if (TextUtils.equals(sbn.getNotification().category,
                Notification.CATEGORY_RECOMMENDATION)) {
            return true;
        }

        if (TextUtils.equals(sbn.getNotification().category,
                Notification.CATEGORY_TRANSPORT)) {
            return true;
        }
        return false;
    }

    private List<ActiveNotificationInfo> loadNotifications() {
        if (DEBUG) Log.v(TAG, "loadNotifications");

        try {
            StatusBarNotification[] recentNotifications = mNoMan.getActiveNotifications(
                    getPackageName());
            if (DEBUG) {
                Log.v(TAG,
                        "loadNotifications got " + recentNotifications.length + " notifications");
            }
            List<ActiveNotificationInfo> list = new ArrayList<ActiveNotificationInfo>(
                    recentNotifications.length);

            for (StatusBarNotification sbn : recentNotifications) {
                if (!shouldBeIgnored(sbn)) {
                    final ActiveNotificationInfo info = new ActiveNotificationInfo(
                            this, sbn, mMaxNotificationCount, mMaxNotificationText);
                    if (info.getUserId() == UserHandle.USER_ALL ||
                            info.getUserId() == mCurrentUser) {
                        list.add(info);
                    }
                }
            }

            return list;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void onNotificationPosted(final StatusBarNotification notification) {
        if (!shouldBeIgnored(notification)) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (DEBUG) {
                        Log.v(TAG, "onNotificationPosted: " + notification);
                    }

                    // If we previously had a notification with this id, remove it
                    ActiveNotificationInfo info = mNotificationMap.get(notification.getId());
                    if (info != null) {
                        removeNotificationFromExistence(info);
                    }

                    // Add this notification
                    info = new ActiveNotificationInfo(NotificationActivity.this, notification,
                            mMaxNotificationCount, mMaxNotificationText);
                    mAdapter.insert(info, 1);
                    mNotificationMap.put(notification.getId(), info);

                    addOrRemoveSpecialActions();
                }
            });
        }
    }

    private void onNotificationRemoved(final StatusBarNotification notification) {
        if (!shouldBeIgnored(notification)) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (DEBUG) {
                        Log.v(TAG, "onNotificationRemoved: " + notification);
                    }
                    ActiveNotificationInfo info = mNotificationMap.get(notification.getId());
                    if (info != null) {
                        removeNotificationFromExistence(info);
                        addOrRemoveSpecialActions();
                    }
                }
            });
        }
    }

    private void addOrRemoveSpecialActions() {
        // Determine if we should have a dismiss all action
        mAdapter.remove(mDismissAllListItem);
        mAdapter.remove(mNoNotificationsListItem);
        int count = mAdapter.getCount();
        if (count <= 0) {
            mAdapter.add(mNoNotificationsListItem);
        } else {
            mDismissAllListItem.setEnabled(false);
            for (int i = 0; i < count; i++) {
                NotificationListItem item = mAdapter.getItem(i);
                if (item instanceof ActiveNotificationInfo) {
                    ActiveNotificationInfo infoItem = (ActiveNotificationInfo) item;
                    if (infoItem.isDismissableOnDismissAll()) {
                        mDismissAllListItem.setEnabled(true);
                        break;
                    }
                }
            }
            mAdapter.insert(mDismissAllListItem, 0);
        }
    }

    private void removeNotificationFromExistence(ActiveNotificationInfo info) {
        mAdapter.remove(info);
        mNotificationMap.remove(info.getId());
        // If we're currently viewing that notification and it's been
        // updated/removed, get out of there.
        if (mLastNotificationSelected == info && getActionFragment() != mNotificationListFragment) {
            int backstackEntries = getFragmentManager().getBackStackEntryCount();
            for (int i = 0; i < backstackEntries; i++) {
                getFragmentManager().popBackStack();
            }
        }
    }
}
