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
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import com.android.tv.settings.R;
import com.android.tv.settings.device.eco.EnergyModesHelper.EnergyMode;
import com.android.tv.settings.util.GuidedActionsAlignUtil;

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
            GuidedStepSupportFragment
                    .addAsRoot(this, ConfirmationFragment.newInstance(energyModeId),
                            android.R.id.content);
        }
    }

    /** Fragment to confirm whether an energy mode should be set */
    public static class ConfirmationFragment extends GuidedStepSupportFragment {

        private EnergyModesHelper mEnergyModesHelper;
        private EnergyMode mEnergyMode;

        static ConfirmationFragment newInstance(String energyModeId) {
            Bundle args = new Bundle();
            args.putString(EXTRA_ENERGY_MODE_ID, energyModeId);
            ConfirmationFragment fragment = new ConfirmationFragment();
            fragment.setArguments(args);
            return fragment;
        }

        private EnergyMode getEnergyMode() {
            if (mEnergyMode != null) {
                return mEnergyMode;
            }

            String energyModeId = getArguments().getString(EXTRA_ENERGY_MODE_ID);
            mEnergyModesHelper = new EnergyModesHelper(getContext());
            mEnergyMode = mEnergyModesHelper.getEnergyMode(energyModeId);
            return mEnergyMode;
        }

        @NonNull
        @Override
        public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            EnergyMode energyMode = getEnergyMode();
            return new GuidanceStylist.Guidance(
                    getString(R.string.energy_modes_confirmation_title,
                            getString(energyMode.titleRes)),
                    mEnergyModesHelper.getSummary(energyMode) + "\n\n"
                            + getString(energyMode.ecoHintRes),
                    /* breadcrumb= */ null,
                    /* icon= */ null);
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions,
                Bundle savedInstanceState) {
            actions.add(new GuidedAction.Builder(getContext())
                    .clickAction(GuidedAction.ACTION_ID_OK)
                    .title(getString(R.string.settings_confirm))
                    .build());
            actions.add(new GuidedAction.Builder(getContext())
                    .clickAction(GuidedAction.ACTION_ID_CANCEL)
                    .build());
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == GuidedAction.ACTION_ID_OK) {
                mEnergyModesHelper.setEnergyMode(mEnergyMode);
            }
            getActivity().finish();
        }

        @Override
        public GuidanceStylist onCreateGuidanceStylist() {
            return GuidedActionsAlignUtil.createGuidanceStylist();
        }
    }
}
