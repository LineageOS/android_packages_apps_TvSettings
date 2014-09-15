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

import android.content.Context;

class DismissAllListItem extends SimpleNotificationListItem {

    interface Listener {
        void dismissAllSelected();
    }

    private final String mTitle;
    private final Listener mListener;
    private boolean mEnabled;

    DismissAllListItem(String title, Listener listener) {
        mTitle = title;
        mListener = listener;
        mEnabled = true;
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public void launch(Context context) {
        mListener.dismissAllSelected();
    }

    @Override
    public boolean isEnabled() {
        return mEnabled;
    }

    void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }
}
