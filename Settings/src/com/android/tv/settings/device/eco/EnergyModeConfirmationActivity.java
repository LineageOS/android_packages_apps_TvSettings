/**
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.tv.settings.device.eco;

import android.annotation.Nullable;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import com.android.tv.settings.R;
import com.android.tv.settings.device.eco.EnergyModesHelper.EnergyMode;
import com.android.tv.settings.overlay.FlavorUtils;
import com.android.tv.settings.util.GuidedActionsAlignUtil;
import com.android.tv.settings.widget.SettingsGuidedStepFragment;
import com.android.tv.twopanelsettings.FullScreenDialogFragment;

import java.util.List;

/** Activity to confirm whether an energy mode should be set */
public class EnergyModeConfirmationActivity extends FragmentActivity {

    private static final String TAG = "EnergyModeConfirmationActivity";
    public static final String EXTRA_ENERGY_MODE_ID = "EXTRA_ENERGY_MODE_ID";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            String energyModeId = getIntent().getStringExtra(EXTRA_ENERGY_MODE_ID);
            EnergyModesHelper energyModesHelper = new EnergyModesHelper(getApplicationContext());
            EnergyMode energyMode = energyModesHelper.getEnergyMode(energyModeId);

            setTheme(R.style.TvSettingsDialog_FullScreen);
            FullScreenDialogConfirmationFragment dialogFragment =
                    FullScreenDialogConfirmationFragment.newInstance(
                            getApplicationContext(), energyMode);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, dialogFragment)
                    .commitAllowingStateLoss();
        }
    }

    /** Confirmation dialog for changing energy mode */
    public static class FullScreenDialogConfirmationFragment extends FullScreenDialogFragment {
        private EnergyModesHelper mEnergyModesHelper;

        static FullScreenDialogConfirmationFragment newInstance(Context context,
                EnergyMode energyMode) {
            Icon hintIcon = null;
            if (energyMode.ecoHintIconRes != 0) {
                hintIcon = Icon.createWithResource(context, energyMode.ecoHintIconRes);
            }
            Bundle args = new DialogBuilder()
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_info_outline))
                    .setTitle(context.getString(R.string.energy_modes_confirmation_title,
                            context.getString(energyMode.titleRes)))
                    .setHintIcon(hintIcon)
                    .setHintText(context.getString(energyMode.ecoHintRes))
                    .setPositiveButton(context.getString(R.string.settings_confirm))
                    .setNegativeButton(context.getString(R.string.settings_cancel))
                    .build();

            args.putString(EXTRA_ENERGY_MODE_ID, context.getString(energyMode.identifierRes));

            FullScreenDialogConfirmationFragment fragment =
                    new FullScreenDialogConfirmationFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        protected int getLayoutResId() {
            return R.layout.energy_modes_full_screen_dialog;
        }

        @Override
        public void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState) {
            mEnergyModesHelper = new EnergyModesHelper(getContext());
            super.onCreate(savedInstanceState);
        }

        private EnergyMode getEnergyMode() {
            String energyModeId = getArguments().getString(EXTRA_ENERGY_MODE_ID);
            return mEnergyModesHelper.getEnergyMode(energyModeId);
        }

        @Override
        public CharSequence getMessage() {
            EnergyMode energyMode = getEnergyMode();
            String summary = getContext().getString(energyMode.infoTextRes);
            StringBuilder message = new StringBuilder(summary);
            if (getContext().getResources().getStringArray(energyMode.featuresRes).length > 0) {
                message.append("\n\n");
                message.append(getContext().getString(R.string.energy_mode_enables));
            }
            return message.toString();
        }

        @Override
        public View createCustomView(ViewGroup parent) {
            // List features in a separate start-aligned list
            TextView featuresView = new TextView(parent.getContext());
            featuresView.setTextAlignment(TextView.TEXT_ALIGNMENT_VIEW_START);
            featuresView.setText(mEnergyModesHelper.getFeaturesList(getEnergyMode()));
            return featuresView;
        }

        @Override
        public void onButtonPressed(int action) {
            if (action == ACTION_POSITIVE) {
                mEnergyModesHelper.setEnergyMode(getEnergyMode());
            }
            getActivity().finish();
        }
    }
}
