/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.ContentResolver;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.lifecycle.ViewModelProvider;
import com.android.tv.settings.accessibility.viewmodel.BounceKeyViewModel;
import com.android.tv.twopanelsettings.R;
import com.android.tv.twopanelsettings.slices.InfoFragment;

@Keep
public class AccessibilityBounceKeyDetailedFragment extends InfoFragment {

    private BounceKeyViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(BounceKeyViewModel.class);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        // Get the initial Bounce Key Time
        int initialBounceKeyTime = getCurrentBounceKeyTime();

        // Setting up Bounce Keys Title
        TextView bounceKeyInfoTitle = view.requireViewById(R.id.info_title);
        bounceKeyInfoTitle.setVisibility(View.VISIBLE);

        // Setting up Bounce Keys Description
        TextView bounceKeyInfoSummary = view.requireViewById(R.id.info_summary);
        bounceKeyInfoSummary.setVisibility(View.VISIBLE);

        // Setting initial bounce key title and description
        setBounceKeyText(bounceKeyInfoTitle, bounceKeyInfoSummary, initialBounceKeyTime);

        // Observe the ViewModel's Bounce Key Title
        viewModel
                .getBounceKeyValue()
                .observe(
                        getViewLifecycleOwner(),
                        bounceKeyValue -> {
                            // Update your UI elements based on the bounceKey Selected
                            setBounceKeyText(bounceKeyInfoTitle,
                                    bounceKeyInfoSummary, bounceKeyValue);
                        });

        return view;
    }

    /* Set Bounce Keys detailed  */
    private void setBounceKeyText(
            TextView bounceKeyInfoTitle, TextView bounceKeyInfoSummary, int bounceKeyTime) {
        switch (bounceKeyTime) {
            case 500:
                bounceKeyInfoTitle.setText(R.string.bounce_key_timing_half_second_title);
                bounceKeyInfoSummary.setText(R.string.bounce_key_timing_half_second_description);
                break;
            case 1000:
                bounceKeyInfoTitle.setText(R.string.bounce_key_timing_one_second_title);
                bounceKeyInfoSummary.setText(R.string.bounce_key_timing_one_second_description);
                break;
            case 2000:
                bounceKeyInfoTitle.setText(R.string.bounce_key_timing_two_second_title);
                bounceKeyInfoSummary.setText(R.string.bounce_key_timing_two_second_description);
                break;
            default:
                bounceKeyInfoTitle.setText(R.string.bounce_key_timing_default_title);
                bounceKeyInfoSummary.setText(R.string.bounce_key_timing_default_description);
                break;
        }
    }

    /**
     * Get current bounce keys time
     *
     * @return current bounce key value
     */
    private int getCurrentBounceKeyTime() {
        final ContentResolver resolver = getContext().getContentResolver();
        return Settings.Secure.getInt(resolver, Settings.Secure.ACCESSIBILITY_BOUNCE_KEYS, 0);
    }
}
