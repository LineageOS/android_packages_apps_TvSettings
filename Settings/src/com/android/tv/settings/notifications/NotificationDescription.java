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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

class NotificationDescription extends SimpleNotificationListItem {

    private static final String TAG = "NotificationDescription";

    private final String mSummaryText;
    private final String mBadgeIconUri;
    private final PendingIntent mContentIntent;
    private final String mPkg;

    NotificationDescription(String summaryText, String badgeIconUri, PendingIntent contentIntent,
            String pkg) {
        mSummaryText = summaryText;
        mBadgeIconUri = badgeIconUri;
        mContentIntent = contentIntent;
        mPkg = pkg;
    }

    @Override
    public String getText() {
        return (!TextUtils.isEmpty(mSummaryText)) ? mSummaryText : null;
    }

    @Override
    public boolean showLeftIconSpace() {
        return false;
    }

    @Override
    public String getBadgeIconUri() {
        return mBadgeIconUri;
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
}
