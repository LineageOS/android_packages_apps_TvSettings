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

import com.android.internal.R;
import com.android.tv.settings.util.IntentUtils;

import android.app.Fragment;
import android.app.INotificationManager;
import android.app.Notification;
import android.app.Notification.Action;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.RemoteException;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.NumberFormat;

class ActiveNotificationInfo extends SimpleNotificationListItem {

    private static final String TAG = "ActiveNotificationInfo";

    private final String mPkg;
    private final int mUser;
    private final int mId;
    private final String mTag;
    private final long mTimestamp;
    private final String mSmallIconUri;
    private final String mAppName;
    private final String mTitle;
    private final String mText;
    private final String mSubtext;
    private final String mBadgeText;
    private final Bitmap mLargeIcon;
    private final PendingIntent mContentIntent;
    private final Action[] mActions;
    private final RemoteViews mContentView;
    private final RemoteViews mBigContentView;
    private final boolean mIsNoClear;
    private final boolean mIsAutoCancel;

    ActiveNotificationInfo(Context context, StatusBarNotification statusBarNotification,
            int maxNotificationCount, String maxNotificationText) {
        mPkg = statusBarNotification.getPackageName();
        mUser = statusBarNotification.getUserId();
        mId = statusBarNotification.getId();
        mTag = statusBarNotification.getTag();
        mTimestamp = statusBarNotification.getPostTime();
        Notification notification = statusBarNotification.getNotification();

        mSmallIconUri = UriUtil.getAppIconUri(context, mPkg, mUser, new int[] {
        notification.icon, getPackageIconResource(context, mPkg) });

        mAppName = getPackageLabel(context, mPkg);

        if(notification.extras != null) {
            mTitle = notification.extras.getString(Notification.EXTRA_TITLE);
            mText = notification.extras.getString(Notification.EXTRA_TEXT);
            mSubtext = notification.extras.getString(Notification.EXTRA_SUB_TEXT);
        } else {
            mTitle = null;
            mText = null;
            mSubtext = null;
        }

        if (notification.number > 0) {
            mBadgeText = (notification.number > maxNotificationCount) ? maxNotificationText
                    : NumberFormat.getIntegerInstance().format(notification.number);
        } else {
            mBadgeText = "";
        }

        mLargeIcon = notification.largeIcon;
        mContentIntent = notification.contentIntent;
        mActions = notification.actions;
        if (notification.contentView != null && notification.contentView.getLayoutId()
                    != R.layout.notification_template_material_base) {
            mContentView = notification.contentView;
        } else {
            mContentView = null;
        }
        mBigContentView = notification.bigContentView;
        mIsNoClear = (notification.flags &
                (Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT)) != 0;
        mIsAutoCancel = (notification.flags & Notification.FLAG_AUTO_CANCEL) != 0;
    }

    @Override
    public String getTitle() {
        return (mContentView == null) ? (mTitle != null) ? mTitle : "" : null;
    }

    @Override
    public String getText() {
        return (mContentView == null && !TextUtils.isEmpty(mText)) ? mText : null;
    }

    @Override
    public String getSubText() {
        return (mContentView == null && !TextUtils.isEmpty(mSubtext)) ? mSubtext : null;
    }

    @Override
    public long getTimestamp() {
        return (mContentView == null) ? mTimestamp : -1;
    }

    @Override
    public String getBadgeText() {
        return (mContentView == null) ? mBadgeText : null;
    }

    @Override
    public RemoteViews getCustomView() {
        return mContentView;
    }

    @Override
    public Bitmap getLargeIcon() {
        return mLargeIcon;
    }

    @Override
    public String getSmallIconUri() {
        return (mLargeIcon != null) ? null : mSmallIconUri;
    }

    @Override
    public String getBadgeIconUri() {
        return (mLargeIcon != null) ? mSmallIconUri : null;
    }

    NotificationContentFragment createContentFragment() {
        String title = mTitle;
        if (TextUtils.isEmpty(title)) {
            title = mAppName;
        }
        if (mContentView != null) {
            return NotificationContentFragment.newInstance(title, mSubtext);
        } else if (mLargeIcon != null) {
            return NotificationContentFragment.newInstance(title, mSubtext, mLargeIcon);
        } else {
            return NotificationContentFragment.newInstance(title, mSubtext, mSmallIconUri);
        }
    }

    Fragment createDisplayFragment() {
        return NotificationRemoteViewsFragment.newInstance(new CustomListItem(
                hasBigCustomView() ? mBigContentView : mContentView, mContentIntent, mPkg));
    }

    ActionListFragment createActionListFragment() {
        String badgeIconUri = (mLargeIcon == null) ? null : mSmallIconUri;
        return ActionListFragment.newInstance(
                mText, badgeIconUri, mContentIntent, mPkg, mBigContentView, mUser, mActions);
    }

    boolean isDismissableOnSelect() {
        return mIsAutoCancel;
    }

    boolean isDismissableOnDismissAll() {
        return !mIsNoClear;
    }

    boolean hasSmallCustomView() {
        return (mContentView != null);
    }

    boolean hasBigCustomView() {
        return (mBigContentView != null);
    }

    @Override
    public void launch(Context context) {
        if (mContentIntent != null) {
            try {
                mContentIntent.send(context, 0, new Intent());
            } catch (PendingIntent.CanceledException e) {
                // the stack trace isn't very helpful here. Just log the
                // exception message.
                Log.w(TAG, "Sending contentIntent failed: " + e);
            }
        } else {
            Intent i = context.getPackageManager().getLaunchIntentForPackage(mPkg);
            if (i != null) {
                IntentUtils.startActivity(context, i);
            }
        }
    }

    int getUserId() {
        return mUser;
    }

    int getId() {
        return mId;
    }

    boolean hasActions() {
        return mActions != null && mActions.length > 0;
    }

    void cancel(INotificationManager notificationManager) {
        try {
            notificationManager.cancelNotificationWithTag(mPkg, mTag, mId, mUser);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to cancel: " + mPkg + ", " + mTag + ", " + mId + ", " + mUser);
        }
    }

    private static int getPackageIconResource(Context context, String pkg) {
        try {
            return context.getPackageManager().getApplicationInfo(pkg, 0).icon;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return 0;
    }

    private static String getPackageLabel(Context context, String pkg) {
        try {
            return context.getPackageManager()
                    .getApplicationInfo(pkg, 0).loadLabel(context.getPackageManager()).toString();
        } catch (PackageManager.NameNotFoundException e) {
        }
        return null;
    }
}
