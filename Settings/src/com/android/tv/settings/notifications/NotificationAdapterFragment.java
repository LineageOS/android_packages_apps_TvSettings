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

import com.android.tv.settings.dialog.old.ScrollAdapterFragment;

import android.app.Activity;

public class NotificationAdapterFragment extends ScrollAdapterFragment {

    private NotificationAdapter mAdapter;
    private boolean mAdapterSet;

    @Override
    public void onAttach(Activity a) {
        super.onAttach(a);
        if (a instanceof NotificationActivity) {
            mAdapter = ((NotificationActivity) a).getNotificationAdapter();
            mAdapterSet = false;
        }
    }

    @Override
    public void onDetach() {
        mAdapterSet = false;
        mAdapter = null;
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mAdapter != null && !mAdapterSet) {
            // ensure list is built.
            getScrollAdapterView().addOnItemChangeListener(mAdapter);
            setAdapter(mAdapter);

            if (mAdapter.getCount() >= 2) {
                // If notifications are present, move focus to the first
                // notification on resume.
                setSelection(2);
            }
            mAdapterSet = true;
        }
    }
}
