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

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

/**
 * Displays a single notification's custom view.
 */
public class NotificationRemoteViewsFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "NotificationRemoteViewsFragment";
    private static final String EXTRA_CUSTOM_LIST_ITEM = "custom_list_item";

    public static NotificationRemoteViewsFragment newInstance(CustomListItem customListItem) {
        NotificationRemoteViewsFragment fragment = new NotificationRemoteViewsFragment();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_CUSTOM_LIST_ITEM, customListItem);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle icicle) {
        View view = inflater.inflate(R.layout.notification_custom_row, null);
        CustomListItem customListItem = getArguments().getParcelable(EXTRA_CUSTOM_LIST_ITEM);
        ViewGroup customViewParent = (ViewGroup) view.findViewById(R.id.custom_style);
        RemoteViews customView = customListItem.getCustomView();
        if (customView != null) {
            try {
                View remoteView = customView.apply(getActivity(), customViewParent);
                customViewParent.addView(remoteView);
            } catch (Exception e) {
                Log.e(TAG, "Couldn't inflate custom view: " + e);
                return null;
            }
        }
        customViewParent.requestFocus();
        view.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        CustomListItem customListItem = getArguments().getParcelable(EXTRA_CUSTOM_LIST_ITEM);
        customListItem.launch(getActivity());
    }
}
