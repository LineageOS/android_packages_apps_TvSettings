/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.tv.settings.system;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.tv.settings.BaseSettingsFragment;
import com.android.tv.settings.R;
import com.android.tv.settings.library.overlay.FlavorUtils;

public class LiveDisplaySettingsFragment extends BaseSettingsFragment {

    public static final String ACTION_REFRESH_LIVEDISPLAY_PREVIEW = "LiveDisplaySettingsFragment.refresh";

    private View mPreviewWindow;

    public static LiveDisplaySettingsFragment newInstance() {
        return new LiveDisplaySettingsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final ViewGroup v = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        if (v == null) {
            throw new IllegalStateException("Unexpectedly null view from super");
        }
        inflater.inflate(R.layout.livedisplay_preview, v, true);
        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPreviewWindow = view.findViewById(R.id.preview_window);
        if (FlavorUtils.isTwoPanel(getContext())) {
            // Customize the padding and layout of LiveDisplay settings in two panel case
            View v = getView().findViewById(R.id.settings_preference_fragment_container);
            Resources res = getResources();
            ViewGroup.LayoutParams lP = v.getLayoutParams();
            lP.width = res.getDimensionPixelSize(R.dimen.caption_preference_two_panel_width);
            v.setLayoutParams(lP);
            v.setBackgroundColor(res.getColor(R.color.tp_fragment_container_background_color));
            v.setPaddingRelative(
                    res.getDimensionPixelOffset(R.dimen.caption_preference_two_panel_padding_start),
                    v.getPaddingTop(),
                    res.getDimensionPixelOffset(R.dimen.caption_preference_two_panel_padding_end),
                    v.getPaddingBottom());
            ((ViewGroup) v).setClipChildren(false);
            ((ViewGroup) v).setClipToPadding(false);
        }
    }

    @Override
    public void onPreferenceStartInitialScreen() {
        startPreferenceFragment(LiveDisplayFragment.newInstance());
    }
}
