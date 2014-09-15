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

import android.graphics.Bitmap;
import android.widget.RemoteViews;

abstract class SimpleNotificationListItem implements NotificationListItem {

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public String getText() {
        return null;
    }

    @Override
    public String getSubText() {
        return null;
    }

    @Override
    public long getTimestamp() {
        return -1;
    }

    @Override
    public String getBadgeText() {
        return null;
    }

    @Override
    public Bitmap getLargeIcon() {
        return null;
    }

    @Override
    public String getSmallIconUri() {
        return null;
    }

    @Override
    public String getBadgeIconUri() {
        return null;
    }

    @Override
    public boolean showLeftIconSpace() {
        return true;
    }

    @Override
    public RemoteViews getCustomView() {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
