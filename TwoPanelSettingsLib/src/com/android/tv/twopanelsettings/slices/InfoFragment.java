/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.tv.twopanelsettings.slices;


import android.app.Fragment;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.tv.twopanelsettings.R;

/**
 * Fragment to display informational image and description text for slice.
 */
public class InfoFragment extends Fragment {
    public static final String EXTRA_INFO_HAS_STATUS = "extra_info_has_status";

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.info_fragment, container, false);
        updateInfo(view);
        return view;
    }

    /** Update the infos in InfoFragment **/
    public void updateInfoFragment() {
        updateInfo(getView());
    }

    private void updateInfo(View view) {
        Icon image = getArguments().getParcelable(SlicesConstants.EXTRA_PREFERENCE_INFO_IMAGE);
        Icon titleIcon = getArguments().getParcelable(
                SlicesConstants.EXTRA_PREFERENCE_INFO_TITLE_ICON);
        String title = getArguments().getString(SlicesConstants.EXTRA_PREFERENCE_INFO_TEXT);
        String summary = getArguments().getString(SlicesConstants.EXTRA_PREFERENCE_INFO_SUMMARY);
        boolean hasStatus = getArguments().getBoolean(EXTRA_INFO_HAS_STATUS);
        boolean status = getArguments().getBoolean(SlicesConstants.EXTRA_PREFERENCE_INFO_STATUS);
        if (image != null) {
            ((ImageView) view.findViewById(R.id.info_image))
                    .setImageDrawable(image.loadDrawable(getContext()));
        }
        if (titleIcon != null) {
            ((ImageView) view.findViewById(R.id.info_title_icon))
                    .setImageDrawable(titleIcon.loadDrawable(getContext()));
        }
        if (title != null) {
            ((TextView) view.findViewById(R.id.info_title)).setText(title);
        }
        if (summary != null) {
            ((TextView) view.findViewById(R.id.info_summary)).setText(summary);
        }
        TextView statusView = view.findViewById(R.id.info_status);
        if (hasStatus) {
            statusView.setVisibility(View.VISIBLE);
            if (status) {
                statusView.setTextColor(getResources().getColor(R.color.info_status_on));
                statusView.setText(getString(R.string.info_status_on));
            } else {
                statusView.setTextColor(getResources().getColor(R.color.info_status_off));
                statusView.setText(getString(R.string.info_status_off));
            }
        } else {
            statusView.setVisibility(View.GONE);
        }
    }
}
