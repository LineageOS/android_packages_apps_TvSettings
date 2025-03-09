/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.tv.settings.about;

import static com.android.tv.settings.overlay.FlavorUtils.FLAVOR_CLASSIC;
import static com.android.tv.settings.overlay.FlavorUtils.FLAVOR_TWO_PANEL;
import static com.android.tv.settings.overlay.FlavorUtils.FLAVOR_VENDOR;
import static com.android.tv.settings.overlay.FlavorUtils.FLAVOR_X;

import android.annotation.Nullable;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import com.android.tv.settings.FullScreenDialogFragment;
import com.android.tv.settings.R;
import com.android.tv.settings.overlay.FlavorUtils;
import com.android.tv.settings.widget.SettingsGuidedStepFragment;

import java.util.List;

/** Activity to confirm rebooting the device */
public class RebootConfirmActivity extends FragmentActivity {

    private static final String ARG_SAFE_MODE = "RebootConfirmFragment.safe_mode";

    /** generate an Intent to start this Activity */
    public static Intent getIntent(Context context, boolean safeMode) {
        return new Intent(context, RebootConfirmActivity.class)
                .putExtra(ARG_SAFE_MODE, safeMode);
    }

    protected static void reboot(Context context, boolean toSafeMode) {
        final PowerManager pm = context.getSystemService(PowerManager.class);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (toSafeMode) {
                    pm.rebootSafeMode();
                } else {
                    pm.reboot(null);
                }
                return null;
            }
        }.execute();
    }

    protected static boolean isToSafeMode(Bundle arguments) {
        boolean toSafeMode = false;
        if (arguments != null) {
            toSafeMode = arguments.getBoolean(ARG_SAFE_MODE, false);
        }
        return toSafeMode;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            boolean toSafeMode = isToSafeMode(getIntent().getExtras());
            boolean twoPanel = FlavorUtils.isTwoPanel(getApplicationContext());

            if (!twoPanel) {
                setTheme(R.style.Theme_Leanback_GuidedStep);
                GuidedStepSupportFragment.addAsRoot(
                        this,
                        GuidedStepRebootConfirmFragment.newInstance(toSafeMode),
                        android.R.id.content);
            } else {
                setTheme(R.style.TvSettingsDialog_FullScreen);
                FullScreenDialogRebootConfirmFragment dialogFragment =
                        FullScreenDialogRebootConfirmFragment.newInstance(getApplicationContext(),
                                toSafeMode);
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(android.R.id.content, dialogFragment)
                        .commitAllowingStateLoss();
            }
        }
    }

    /** Confirmation page in the full screen dialog style */
    public static class FullScreenDialogRebootConfirmFragment extends FullScreenDialogFragment {

        public static FullScreenDialogRebootConfirmFragment newInstance(Context context,
                boolean safeMode) {
            Bundle args = new FullScreenDialogFragment.DialogBuilder()
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_warning_132dp))
                    .setTitle(safeMode ? context.getString(R.string.reboot_safemode_confirm)
                            : context.getString(R.string.system_reboot_confirm))
                    .setMessage(safeMode ? context.getString(R.string.reboot_safemode_desc) : null)
                    .setPositiveButton(safeMode ? context.getString(R.string.reboot_safemode_action)
                            : context.getString(R.string.restart_button_label))
                    .setNegativeButton(context.getString(R.string.settings_cancel))
                    .setInitialFocusOnNegativeButton(true)
                    .build();

            args.putBoolean(ARG_SAFE_MODE, safeMode);
            FullScreenDialogRebootConfirmFragment fragment =
                    new FullScreenDialogRebootConfirmFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onButtonPressed(int action) {
            if (action == ACTION_POSITIVE) {
                reboot(requireContext(), isToSafeMode(requireArguments()));
            } else {
                requireActivity().finish();
            }
        }
    }

    /** Confirmation page in the guided step style */
    @Keep
    public static class GuidedStepRebootConfirmFragment extends SettingsGuidedStepFragment {

        public static GuidedStepRebootConfirmFragment newInstance(
                boolean safeMode) {
            Bundle args = new Bundle(1);
            args.putBoolean(ARG_SAFE_MODE, safeMode);

            GuidedStepRebootConfirmFragment
                    fragment = new GuidedStepRebootConfirmFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setSelectedActionPosition(1);
        }

        @Override
        public @NonNull
        GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            if (getArguments().getBoolean(ARG_SAFE_MODE, false)) {
                return new GuidanceStylist.Guidance(
                        getString(R.string.reboot_safemode_confirm),
                        getString(R.string.reboot_safemode_desc),
                        null,
                        getActivity().getDrawable(R.drawable.ic_warning_132dp)
                );
            } else {
                return new GuidanceStylist.Guidance(
                        getString(R.string.system_reboot_confirm),
                        null,
                        null,
                        getActivity().getDrawable(R.drawable.ic_warning_132dp)
                );
            }
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions,
                Bundle savedInstanceState) {
            final Context context = getActivity();
            if (getArguments().getBoolean(ARG_SAFE_MODE, false)) {
                actions.add(new GuidedAction.Builder(context)
                        .id(GuidedAction.ACTION_ID_OK)
                        .title(R.string.reboot_safemode_action)
                        .build());
            } else {
                actions.add(new GuidedAction.Builder(context)
                        .id(GuidedAction.ACTION_ID_OK)
                        .title(R.string.restart_button_label)
                        .build());
            }
            actions.add(new GuidedAction.Builder(context)
                    .clickAction(GuidedAction.ACTION_ID_CANCEL)
                    .build());
        }

        @Override
        public GuidanceStylist onCreateGuidanceStylist() {
            return new GuidanceStylist() {
                @Override
                public int onProvideLayoutId() {
                    switch (FlavorUtils.getFlavor(getContext())) {
                        case FLAVOR_CLASSIC:
                        case FLAVOR_TWO_PANEL:
                            return R.layout.confirm_guidance;
                        case FLAVOR_X:
                        case FLAVOR_VENDOR:
                            return R.layout.confirm_guidance_x;
                        default:
                            return R.layout.confirm_guidance;
                    }
                }
            };
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == GuidedAction.ACTION_ID_OK) {
                reboot(requireContext(), isToSafeMode(requireArguments()));
            } else {
                requireActivity().finish();
            }
        }
    }
}
