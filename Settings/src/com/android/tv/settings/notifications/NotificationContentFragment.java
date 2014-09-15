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

import com.android.tv.settings.notifications.ViewUtil.Canceller;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.tv.settings.R;

public class NotificationContentFragment extends Fragment {

    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_DESCRIPTION = "description";
    private static final String EXTRA_SMALL_ICON = "small_icon";
    private static final String EXTRA_BIG_ICON = "big_icon";

    static NotificationContentFragment newInstance(String title, String description) {
        NotificationContentFragment fragment = new NotificationContentFragment();
        Bundle args = new Bundle();
        putTitleAndDescription(title, description, args);
        fragment.setArguments(args);
        return fragment;
    }

    static NotificationContentFragment newInstance(String title, String description,
            Bitmap largeIcon) {
        NotificationContentFragment fragment = new NotificationContentFragment();
        Bundle args = new Bundle();
        putTitleAndDescription(title, description, args);
        args.putParcelable(EXTRA_BIG_ICON, largeIcon);
        fragment.setArguments(args);
        return fragment;
    }

    static NotificationContentFragment newInstance(String title, String description,
            String smallIconUri) {
        NotificationContentFragment fragment = new NotificationContentFragment();
        Bundle args = new Bundle();
        putTitleAndDescription(title, description, args);
        args.putString(EXTRA_SMALL_ICON, smallIconUri);
        fragment.setArguments(args);
        return fragment;
    }

    private static void putTitleAndDescription(String title, String description, Bundle args) {
        args.putString(EXTRA_TITLE, title);
        args.putString(EXTRA_DESCRIPTION, description);
    }

    private Activity mActivity;
    private Canceller mCanceller;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.notification_content_fragment, container, false);

        Bundle args = getArguments();

        String breadcrumb = getString(R.string.cluster_name_notifications);
        TextView breadcrumbView = (TextView) view.findViewById(R.id.breadcrumb);
        breadcrumbView.setText(breadcrumb);
        breadcrumbView.setVisibility(View.VISIBLE);

        String title = args.getString(EXTRA_TITLE);
        TextView titleView = (TextView) view.findViewById(R.id.title);
        titleView.setText(title);
        titleView.setVisibility(View.VISIBLE);

        String description = args.getString(EXTRA_DESCRIPTION);
        TextView descriptionView = (TextView) view.findViewById(R.id.description);
        descriptionView.setText(description);
        descriptionView.setVisibility(TextUtils.isEmpty(description) ? View.GONE : View.VISIBLE);

        Bitmap largeIcon = args.getParcelable(EXTRA_BIG_ICON);
        String smallIconUri = args.getString(EXTRA_SMALL_ICON);
        ImageView iconBig = (ImageView) view.findViewById(R.id.icon_big);
        ImageView iconSmall = (ImageView) view.findViewById(R.id.icon_small);

        if (largeIcon != null) {
            iconBig.setImageBitmap(largeIcon);
            iconBig.setVisibility(View.VISIBLE);
            iconSmall.setVisibility(View.GONE);
        } else if (smallIconUri != null) {
            iconBig.setVisibility(View.GONE);
            mCanceller = ViewUtil.setImageViewUri(mActivity, iconSmall, smallIconUri);
        } else {
            iconBig.setImageResource(R.drawable.ic_settings_notification);
            iconBig.setBackgroundColor(getResources().getColor(R.color.icon_background));
            iconBig.setVisibility(View.VISIBLE);
            iconSmall.setVisibility(View.GONE);
        }
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public void onDetach() {
        mActivity = null;
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        if (mCanceller != null) {
            mCanceller.cancel();
        }
        super.onDestroyView();
    }
}
