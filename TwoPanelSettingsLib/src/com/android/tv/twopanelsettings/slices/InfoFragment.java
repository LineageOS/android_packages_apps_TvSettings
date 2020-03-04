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
    private Icon mImage;
    private String mText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.info_fragment, container, false);
        mImage = getArguments().getParcelable(SlicesConstants.EXTRA_PREFERENCE_INFO_IMAGE);
        mText = getArguments().getString(SlicesConstants.EXTRA_PREFERENCE_INFO_TEXT);
        if (mImage != null) {
            ((ImageView) view.findViewById(R.id.info_image))
                    .setImageDrawable(mImage.loadDrawable(getContext()));
        }
        if (mText != null) {
            ((TextView) view.findViewById(R.id.info_text)).setText(mText);
        }
        return view;
    }
}
