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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import androidx.leanback.widget.GuidedActionsStylist;

import com.android.tv.twopanelsettings.FullScreenDialogFragment;
import com.android.tv.settings.R;
import com.android.tv.settings.overlay.FlavorUtils;
import com.android.tv.settings.util.GuidedActionsAlignUtil;
import com.android.tv.settings.widget.SettingsGuidedStepFragment;

import org.lineageos.internal.util.PowerMenuUtils;

import java.util.ArrayList;
import java.util.List;

/** Activity to confirm rebooting the device */
public class RebootConfirmActivity extends FragmentActivity {

    private static final String TAG = "RebootConfirmActivity";

    private static final String ARG_SAFE_MODE = "RebootConfirmFragment.safe_mode";
    private static final String ARG_TITLE = "RebootConfirmFragment.title";
    private static final String ARG_SUMMARY = "RebootConfirmFragment.summary";
    private static final String ARG_DEFAULT_TO_CONFIRM = "RebootConfirmFragment.default_to_confirm";
    private static final String ARG_ADVANCED_RESTART = "RebootConfirmFragment.advanced_restart";

    /** Keys of org.lineageos.platform's config_restartActionsList, in SystemUI's spelling. */
    private static final String ACTION_RESTART = "restart";
    private static final String ACTION_RESTART_RECOVERY = "restart_recovery";
    private static final String ACTION_RESTART_BOOTLOADER = "restart_bootloader";
    private static final String ACTION_RESTART_FASTBOOT = "restart_fastboot";

    /** Values of the matching PowerManager.REBOOT_* constants. */
    private static final String REASON_RECOVERY = "recovery";
    private static final String REASON_BOOTLOADER = "bootloader";
    private static final String REASON_FASTBOOT = "fastboot";

    /** Leanback's own action ids are negative, so index the configured targets from here. */
    private static final long ACTION_ID_RESTART_TARGET_BASE = 1000;

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

    /**
     * Goes through rebootCustom() rather than reboot(), the way the power menu does:
     * ShutdownThread only skips its "Factory data reset" progress dialog for a recovery restart
     * when the custom flag is set.
     */
    protected static void reboot(Context context, @Nullable String reason) {
        final PowerManager pm = context.getSystemService(PowerManager.class);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                pm.rebootCustom(reason);
                return null;
            }
        }.execute();
    }

    /** One entry of config_restartActionsList that we know how to carry out. */
    private static final class RestartTarget {
        final int labelResId;
        @Nullable
        final String reason;

        RestartTarget(int labelResId, @Nullable String reason) {
            this.labelResId = labelResId;
            this.reason = reason;
        }
    }

    /** The restart targets this device offers, in the order it lists them. */
    private static List<RestartTarget> getRestartTargets(Context context) {
        List<RestartTarget> targets = new ArrayList<>();
        for (String action : context.getResources().getStringArray(
                org.lineageos.platform.internal.R.array.config_restartActionsList)) {
            switch (action) {
                case ACTION_RESTART:
                    targets.add(new RestartTarget(R.string.global_action_restart_system, null));
                    break;
                case ACTION_RESTART_RECOVERY:
                    targets.add(new RestartTarget(
                            R.string.global_action_restart_recovery, REASON_RECOVERY));
                    break;
                case ACTION_RESTART_BOOTLOADER:
                    targets.add(new RestartTarget(
                            R.string.global_action_restart_bootloader, REASON_BOOTLOADER));
                    break;
                case ACTION_RESTART_FASTBOOT:
                    targets.add(new RestartTarget(
                            R.string.global_action_restart_fastboot, REASON_FASTBOOT));
                    break;
                default:
                    Log.w(TAG, "Unknown restart action: " + action);
                    break;
            }
        }
        return targets;
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
            boolean advancedRestart = !toSafeMode
                    && PowerMenuUtils.isAdvancedRestartPossible(this)
                    && !getRestartTargets(this).isEmpty();
            String title = extras != null ? extras.getString(ARG_TITLE) : null;
            String summary = extras != null ? extras.getString(ARG_SUMMARY) : null;
            boolean defaultToConfirm = extras != null && extras.getBoolean(ARG_DEFAULT_TO_CONFIRM);
            boolean twoPanel = FlavorUtils.isTwoPanel(getApplicationContext());

            // The full screen dialog only fits two buttons, so on two panel the list of restart
            // targets uses the guided step page too, in the two panel theme from the manifest.
            if (!twoPanel || advancedRestart) {
                if (!twoPanel) {
                    setTheme(R.style.Theme_Leanback_GuidedStep);
                } else {
                    getWindow().setBackgroundDrawableResource(
                            R.drawable.full_screen_dialog_background);
                }
                GuidedStepSupportFragment.addAsRoot(
                        this,
                        GuidedStepRebootConfirmFragment.newInstance(
                                toSafeMode, advancedRestart, title, summary),
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

        /** The targets onCreateActions() built, indexed by action id. */
        private List<RestartTarget> mTargets = new ArrayList<>();

        public static GuidedStepRebootConfirmFragment newInstance(
                boolean safeMode, boolean advancedRestart, @Nullable String title,
                @Nullable String summary) {
            Bundle args = new Bundle(1);
            args.putBoolean(ARG_SAFE_MODE, safeMode);
            args.putBoolean(ARG_ADVANCED_RESTART, advancedRestart);
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

        /** Two panel only gets here for the list of restart targets. */
        private boolean isTwoPanel() {
            return FlavorUtils.isTwoPanel(getContext());
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View view = super.onCreateView(inflater, container, savedInstanceState);
            return isTwoPanel() ? GuidedActionsAlignUtil.createView(view, this) : view;
        }

        @Override
        public GuidedActionsStylist onCreateActionsStylist() {
            return isTwoPanel() ? GuidedActionsAlignUtil.createGuidedActionsStylist()
                    : super.onCreateActionsStylist();
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setSelectedActionPosition(
                    getArguments().getBoolean(ARG_ADVANCED_RESTART, false) ? 0 : 1);
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
            } else if (getArguments().getBoolean(ARG_ADVANCED_RESTART, false)) {
                return new GuidanceStylist.Guidance(
                        getString(R.string.advanced_restart_confirm_title),
                        getString(R.string.advanced_restart_confirm_summary),
                        null,
                        null
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
            if (getArguments().getBoolean(ARG_ADVANCED_RESTART, false)) {
                mTargets = getRestartTargets(context);
                for (int i = 0; i < mTargets.size(); i++) {
                    actions.add(new GuidedAction.Builder(context)
                            .id(ACTION_ID_RESTART_TARGET_BASE + i)
                            .title(mTargets.get(i).labelResId)
                            .build());
                }
                return;
            }
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
            if (isTwoPanel()) {
                // The two panel guided step theme's guidance styles carry no layout sizes, so
                // they only work with the layout made for them.
                return GuidedActionsAlignUtil.createGuidanceStylist();
            }
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
            final long id = action.getId();
            final int index = (int) (id - ACTION_ID_RESTART_TARGET_BASE);
            if (id == GuidedAction.ACTION_ID_OK) {
                reboot(requireContext(), isToSafeMode(requireArguments()));
            } else if (index >= 0 && index < mTargets.size()) {
                reboot(requireContext(), mTargets.get(index).reason);
            } else {
                requireActivity().finish();
            }
        }
    }
}
