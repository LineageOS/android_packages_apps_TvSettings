/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.tv.settings.accessibility;

import android.content.ComponentName;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.tv.twopanelsettings.R;
import com.android.tv.twopanelsettings.slices.InfoFragment;

@Keep
public class AccessibilityServiceInfoFragment extends InfoFragment {

    private static final String ARG_LABEL = "label";
    private static final String ARG_COMPONENT = "component";
    private static final String ARG_ENABLING = "enabling";

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ComponentName serviceComponent = getArguments().getParcelable(ARG_COMPONENT);
        final boolean enabling = getArguments().getBoolean(ARG_ENABLING);
        final String serviceName = getArguments().getString(ARG_LABEL);

        TextView infoTitle = view.requireViewById(R.id.info_title);
        infoTitle.setVisibility(View.VISIBLE);
        infoTitle.setText(getTitle(serviceName, enabling));

        TextView infoSummary = view.requireViewById(R.id.info_summary);
        infoSummary.setVisibility(View.VISIBLE);

        if (serviceComponent != null
                && serviceComponent
                .flattenToString()
                .equals(
                    getResources()
                        .getString(R.string
                            .accessibility_screen_reader_flattened_component_name))) {
            infoSummary.setText(R.string.screen_reader_summary);
        } else {
            infoSummary.setVisibility(View.GONE);
        }
    }

    private String getTitle(String serviceName, boolean enabling) {
        return getString(!enabling ? R.string.accessibility_service_on_title
                : R.string.accessibility_service_off_title, serviceName);
    }
}

