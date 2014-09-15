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

import com.android.tv.settings.util.IntentUtils;

import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Represents a notification with a Custom View.
 */
class CustomListItem extends SimpleNotificationListItem implements Parcelable {

    private static final String TAG = "CustomListItem";

    private final RemoteViews mContentView;
    private final PendingIntent mContentIntent;
    private final String mPkg;
    private final boolean mLaunchable;

    CustomListItem(RemoteViews contentView) {
        this(contentView, null, null, false);
    }

    CustomListItem(RemoteViews contentView, PendingIntent contentIntent, String pkg) {
        this(contentView, contentIntent, pkg, true);
    }

    CustomListItem(RemoteViews contentView, PendingIntent contentIntent, String pkg,
            boolean launchable) {
        mContentView = contentView;
        mContentIntent = contentIntent;
        mPkg = pkg;
        mLaunchable = launchable;
    }

    @Override
    public void launch(Context context) {
        if(mLaunchable) {
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
    }

    @Override
    public RemoteViews getCustomView() {
        return mContentView;
    }

    public static Parcelable.Creator<CustomListItem> CREATOR =
            new Parcelable.Creator<CustomListItem>() {

                @Override
                public CustomListItem createFromParcel(Parcel source) {
                    RemoteViews contentView = source.readParcelable(
                            RemoteViews.class.getClassLoader());
                    PendingIntent contentIntent = source.readParcelable(
                            PendingIntent.class.getClassLoader());
                    String pkg = source.readString();
                    boolean launchable = (source.readInt() == 1) ? true : false;
                    return new CustomListItem(contentView, contentIntent, pkg, launchable);
                }

                @Override
                public CustomListItem[] newArray(int size) {
                    return new CustomListItem[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mContentView, flags);
        dest.writeParcelable(mContentIntent, flags);
        dest.writeString(mPkg);
        dest.writeInt(mLaunchable ? 1 : 0);
    }

    boolean isLaunchable() {
        return mLaunchable;
    }

    Fragment createDisplayFragment() {
        return NotificationRemoteViewsFragment.newInstance(
                new CustomListItem(mContentView, mContentIntent, mPkg));
    }
}
