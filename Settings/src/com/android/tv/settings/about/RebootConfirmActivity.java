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

import com.android.tv.twopanelsettings.FullScreenDialogFragment;
import com.android.tv.settings.R;
import com.android.tv.settings.overlay.FlavorUtils;
import com.android.tv.settings.widget.SettingsGuidedStepFragment;

import org.lineageos.internal.util.PowerMenuUtils;

import java.util.List;

/** Activity to confirm rebooting the device */
public class RebootConfirmActivity extends FragmentActivity {

    private static final String ARG_SAFE_MODE = "RebootConfirmFragment.safe_mode";
    private static final String ARG_TITLE = "RebootConfirmFragment.title";
    private static final String ARG_SUMMARY = "RebootConfirmFragment.summary";
    private static final String ARG_DEFAULT_TO_CONFIRM = "RebootConfirmFragment.default_to_confirm";


    /** generate an Intent to start this Activity */
    public static Intent getIntent(Context context, boolean safeMode) {
        return new Intent(context, RebootConfirmActivity.class)
                .putExtra(ARG_SAFE_MODE, safeMode);
    }

    public static Intent getIntent(Context context, boolean safeMode, String title, String summary,
            boolean defaultToConfirm) {
        return new Intent(context, RebootConfirmActivity.class)
                .putExtra(ARG_SAFE_MODE, safeMode)
                .putExtra(ARG_TITLE, title)
                .putExtra(ARG_SUMMARY, summary)
                .putExtra(ARG_DEFAULT_TO_CONFIRM, defaultToConfirm);
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
            Bundle extras = getIntent().getExtras();
            boolean toSafeMode = isToSafeMode(extras);
            String title = extras != null ? extras.getString(ARG_TITLE) : null;
            String summary = extras != null ? extras.getString(ARG_SUMMARY) : null;
            boolean defaultToConfirm = extras != null && extras.getBoolean(ARG_DEFAULT_TO_CONFIRM);
            boolean twoPanel = FlavorUtils.isTwoPanel(getApplicationContext());

            if (!twoPanel) {
                setTheme(R.style.Theme_Leanback_GuidedStep);
                GuidedStepSupportFragment.addAsRoot(
                        this,
                        GuidedStepRebootConfirmFragment.newInstance(toSafeMode, title, summary),
                        android.R.id.content);
            } else {
                setTheme(R.style.TvSettingsDialog_FullScreen);
                FullScreenDialogRebootConfirmFragment dialogFragment =
                        FullScreenDialogRebootConfirmFragment.newInstance(getApplicationContext(),
                                toSafeMode, title, summary, defaultToConfirm);
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
                boolean safeMode, @Nullable String title, @Nullable String summary,
                boolean defaultToConfirm) {
            Bundle args = new FullScreenDialogFragment.DialogBuilder()
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_warning_132dp))
                    .setTitle(title != null ? title :
                                safeMode ? context.getString(R.string.reboot_safemode_confirm)
                                         : context.getString(R.string.system_reboot_confirm))
                    .setMessage(summary != null ? summary :
                            safeMode ? context.getString(R.string.reboot_safemode_desc) : null)
                    .setPositiveButton(safeMode ? context.getString(R.string.reboot_safemode_action)
                            : context.getString(R.string.restart_button_label))
                    .setNegativeButton(context.getString(R.string.settings_cancel))
                    .setInitialFocusOnNegativeButton(!defaultToConfirm)
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
                boolean safeMode, @Nullable String title, @Nullable String summary) {
            Bundle args = new Bundle(1);
            args.putBoolean(ARG_SAFE_MODE, safeMode);
            if (title != null) {
                args.putString(ARG_TITLE, title);
            }
            if (summary != null) {
                args.putString(ARG_SUMMARY, summary);
            }

            GuidedStepRebootConfirmFragment
                    fragment = new GuidedStepRebootConfirmFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setSelectedActionPosition(getActions().size());
        }

        @Override
        public @NonNull
        GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getArguments().getString(ARG_TITLE);
            String summary = getArguments().getString(ARG_SUMMARY);
            if (getArguments().getBoolean(ARG_SAFE_MODE, false)) {
                return new GuidanceStylist.Guidance(
                        title != null ? title : getString(R.string.reboot_safemode_confirm),
                        summary != null ? summary : getString(R.string.reboot_safemode_desc),
                        null,
                        getActivity().getDrawable(R.drawable.ic_warning_132dp)
                );
            } else if (PowerMenuUtils.isAdvancedRestartPossible(getActivity())) {
                return new GuidanceStylist.Guidance(
                        getString(R.string.system_reboot_confirm_cm),
                        null,
                        null,
                        getActivity().getDrawable(R.drawable.ic_warning_132dp)
                );
            } else {
                return new GuidanceStylist.Guidance(
                        title != null ? title : getString(R.string.system_reboot_confirm),
                        summary,
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
                        .icon(R.drawable.ic_restart_alt)
                        .id(GuidedAction.ACTION_ID_OK)
                        .title(R.string.reboot_safemode_action)
                        .build());
            } else if (PowerMenuUtils.isAdvancedRestartPossible(context)) {
                actions.add(new GuidedAction.Builder(context)
                        .icon(R.drawable.ic_restart_alt)
                        .id(GuidedAction.ACTION_ID_OK)
                        .title(R.string.global_action_restart_system)
                        .build());
                actions.add(new GuidedAction.Builder(context)
                        .icon(R.drawable.ic_lock_restart_recovery)
                        .id(GuidedAction.ACTION_ID_YES)
                        .title(R.string.global_action_restart_recovery)
                        .build());
                actions.add(new GuidedAction.Builder(context)
                        .icon(R.drawable.ic_lock_restart_bootloader)
                        .id(GuidedAction.ACTION_ID_NO)
                        .title(R.string.global_action_restart_bootloader)
                        .build());
            } else {
                actions.add(new GuidedAction.Builder(context)
                        .icon(R.drawable.ic_restart_alt)
                        .id(GuidedAction.ACTION_ID_OK)
                        .title(R.string.restart_button_label)
                        .build());
            }
            actions.add(new GuidedAction.Builder(context)
                    .icon(R.drawable.ic_cancel)
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
