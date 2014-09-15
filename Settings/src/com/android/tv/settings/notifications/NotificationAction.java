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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

class NotificationAction extends SimpleNotificationListItem {

    private static final String TAG = "NotificationAction";

    private final String mIconUri;
    private final String mTitle;
    private final PendingIntent mIntent;

    NotificationAction(String iconUri, String title, PendingIntent intent) {
        mIconUri = iconUri;
        mTitle = title;
        mIntent = intent;
    }

    @Override
    public String getTitle() {
        return (mTitle != null) ? mTitle : "";
    }

    @Override
    public String getSmallIconUri() {
        return mIconUri;
    }

    @Override
    public void launch(Context context) {
        if (mIntent != null) {
            try {
                mIntent.send(context, 0, new Intent());
            } catch (PendingIntent.CanceledException e) {
                // the stack trace isn't very helpful here. Just log the
                // exception message.
                Log.w(TAG, "Sending contentIntent failed: " + e);
            }
        }
    }
}
