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

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Notification.Action;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.RemoteViews;

import com.android.tv.settings.dialog.old.ScrollAdapterFragment;

/**
 * Displays the list of notification actions.
 */
public class ActionListFragment extends ScrollAdapterFragment
        implements NotificationAdapter.NotificationOnClickListener {

    private static final String EXTRA_SUMMARY_TEXT = "summary_text";
    private static final String EXTRA_BADGE_ICON_URI = "badge_icon_uri";
    private static final String EXTRA_CONTENT_INTENT = "content_intent";
    private static final String EXTRA_PACKAGE = "pkg";
    private static final String EXTRA_BIG_CUSTOM_VIEW = "big_content_view";
    private static final String EXTRA_USER_ID = "user_id";
    private static final String EXTRA_ACTIONS = "actions";

    private NotificationAdapter mAdapter;

    public static ActionListFragment newInstance(String summaryText, String badgeIconUri,
            PendingIntent contentIntent, String pkg, RemoteViews bigContentView, int userId,
            Action[] actions) {
        ActionListFragment fragment = new ActionListFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_SUMMARY_TEXT, summaryText);
        args.putString(EXTRA_BADGE_ICON_URI, badgeIconUri);
        args.putParcelable(EXTRA_CONTENT_INTENT, contentIntent);
        args.putString(EXTRA_PACKAGE, pkg);
        args.putParcelable(EXTRA_BIG_CUSTOM_VIEW, bigContentView);
        args.putInt(EXTRA_USER_ID, userId);
        args.putParcelableArray(EXTRA_ACTIONS, actions);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = createAdapter(getActivity(), this, getArguments());
    }

    @Override
    public void onResume() {
        super.onResume();
        getScrollAdapterView().addOnItemChangeListener(mAdapter); // ensure list is built.
        setAdapter(mAdapter);
    }

    @Override
    public void onClick(NotificationListItem item) {
        Activity a = getActivity();
        if (a instanceof NotificationAdapter.NotificationOnClickListener) {
            ((NotificationAdapter.NotificationOnClickListener) a).onClick(item);
        }
    }

    private static NotificationAdapter createAdapter(Context context,
            NotificationAdapter.NotificationOnClickListener listener, Bundle args) {
        NotificationAdapter adapter = new NotificationAdapter(context, listener);
        addDescriptionItem(adapter, args);
        addBigContentViewItem(adapter, args);
        addActionItems(adapter, context, args);
        return adapter;
    }

    private static void addDescriptionItem(NotificationAdapter adaptor, Bundle args) {
        String summaryText = args.getString(EXTRA_SUMMARY_TEXT);
        String badgeIconUri = args.getString(EXTRA_BADGE_ICON_URI);
        PendingIntent contentIntent = args.getParcelable(EXTRA_CONTENT_INTENT);
        String pkg = args.getString(EXTRA_PACKAGE);
        adaptor.add(new NotificationDescription(summaryText, badgeIconUri, contentIntent, pkg));
    }

    private static void addBigContentViewItem(NotificationAdapter adaptor, Bundle args) {
        RemoteViews bigContentView = args.getParcelable(EXTRA_BIG_CUSTOM_VIEW);
        if (bigContentView != null) {
            PendingIntent contentIntent = args.getParcelable(EXTRA_CONTENT_INTENT);
            String pkg = args.getString(EXTRA_PACKAGE);
            adaptor.add(new CustomListItem(bigContentView, contentIntent, pkg));
        }
    }

    private static void addActionItems(NotificationAdapter adaptor, Context context, Bundle args) {
        Parcelable[] actionParcels = args.getParcelableArray(EXTRA_ACTIONS);
        if (actionParcels != null) {
            String pkg = args.getString(EXTRA_PACKAGE);
            int userId = args.getInt(EXTRA_USER_ID);
            for (Parcelable actionParcel : actionParcels) {
                Action action = (Action) actionParcel;
                adaptor.add(new NotificationAction(UriUtil.getIconUri(context, pkg, userId,
                        action.icon), (String) action.title, action.actionIntent));
            }
        }
    }
}
