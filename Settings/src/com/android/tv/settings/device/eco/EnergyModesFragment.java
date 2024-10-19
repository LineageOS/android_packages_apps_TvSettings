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

import static com.android.tv.settings.device.eco.EnergyModesHelper.MODE_HIGH_ENERGY;
import static com.android.tv.settings.util.InstrumentationUtils.logToggleInteracted;

import android.app.Activity;
import android.app.tvsettings.TvSettingsEnums;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Keep;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.tv.settings.FullScreenConfirmationActivity;
import com.android.tv.settings.FullScreenDialogFragment;
import com.android.tv.settings.R;
import com.android.tv.settings.RadioPreference;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.settings.connectivity.util.ThreadNetworkHelper;
import com.android.tv.settings.device.eco.EnergyModesHelper.EnergyMode;
import com.android.tv.settings.overlay.FlavorUtils;
import com.android.tv.twopanelsettings.slices.InfoFragment;

import java.util.Optional;
/**
 * The Energy Modes screen in TV settings.
 */
@Keep
public class EnergyModesFragment extends SettingsPreferenceFragment {
    private static final String TAG = "EnergyModesFragment";

    private static final String EXTRA_ENERGY_MODE_IDENTIFIER = "EXTRA_ENERGY_MODE_IDENTIFIER";
    private static final String RADIO_GROUP_ENERGY_MODES = "energy_modes";

    private EnergyModesHelper mEnergyModesHelper;
    private Optional<ThreadNetworkHelper> mThreadNetworkHelperOptional;
    private boolean isThreadEnabled;
    private EnergyMode newEnergyMode;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        mEnergyModesHelper = new EnergyModesHelper(getContext());
        EnergyMode selectedMode = mEnergyModesHelper.updateEnergyMode();

        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getContext());
        screen.setTitle(R.string.energy_modes);

        Preference titlePreference = new Preference(getContext());
        titlePreference.setTitle(R.string.energy_modes_summary);
        titlePreference.setEnabled(false);
        titlePreference.setSelectable(false);
        titlePreference.setSingleLineTitle(false);
        screen.addPreference(titlePreference);

        for (EnergyMode energyMode : mEnergyModesHelper.getEnergyModes()) {
            RadioPreference radioPreference = createEnergyModeRadioPreference(energyMode);
            if (energyMode.equals(selectedMode)) {
                radioPreference.setChecked(true);
            }
            screen.addPreference(radioPreference);
        }

        setPreferenceScreen(screen);

        mThreadNetworkHelperOptional = Optional.ofNullable(
                ThreadNetworkHelper.getInstance(getContext()));
    }

    @Override
    public void onResume() {
        super.onResume();

        mEnergyModesHelper = new EnergyModesHelper(getContext());
        EnergyMode selectedMode = mEnergyModesHelper.updateEnergyMode();
        for (EnergyMode mode : mEnergyModesHelper.getEnergyModes()) {
            final String key = getContext().getString(mode.identifierRes);
            final RadioPreference radioPreference = findPreference(key);
            radioPreference.setChecked(mode.equals(selectedMode));
        }

        mThreadNetworkHelperOptional.ifPresent(threadNetworkHelper -> {
            threadNetworkHelper.setOnStateChangeListener(mOnThreadNetworkStateChange);
            threadNetworkHelper.registerStateCallback();
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        mThreadNetworkHelperOptional.ifPresent(threadNetworkHelper -> {
            threadNetworkHelper.unregisterStateCallback();
        });
    }

    private RadioPreference createEnergyModeRadioPreference(EnergyMode mode) {
        final RadioPreference radioPreference = new RadioPreference(getContext());
        radioPreference.setPersistent(false);

        radioPreference.setKey(getContext().getString(mode.identifierRes));
        radioPreference.setTitle(mode.titleRes);
        radioPreference.setSummary(mode.subtitleRes);
        radioPreference.setRadioGroup(RADIO_GROUP_ENERGY_MODES);

        if (mode.iconRes != 0) {
            final LayerDrawable compoundIcon =
                    ((LayerDrawable) getContext().getDrawable(R.drawable.compound_icon));

            final Drawable icon = getContext().getDrawable(mode.iconRes);
            final ColorStateList colorStateList = getContext().getResources()
                    .getColorStateList(R.color.preference_icon_color, getContext().getTheme());
            icon.setTintList(colorStateList);

            compoundIcon.setDrawableByLayerId(R.id.foreground, icon);
            radioPreference.setIcon(compoundIcon);
        }

        radioPreference.setFragment(EnergyModeInfoFragment.class.getName());
        Bundle bundle = radioPreference.getExtras();
        bundle.putInt(EXTRA_ENERGY_MODE_IDENTIFIER, mode.identifierRes);

        return radioPreference;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof RadioPreference) {
            RadioPreference modePref = (RadioPreference) preference;

            modePref.setChecked(true);
            modePref.clearOtherRadioPreferences(getPreferenceScreen());

            int energyModeId = preference.getExtras().getInt(EXTRA_ENERGY_MODE_IDENTIFIER);
            EnergyMode currentEnergyMode = mEnergyModesHelper.updateEnergyMode();
            newEnergyMode = mEnergyModesHelper.getEnergyMode(energyModeId);
            if (energyModeId != currentEnergyMode.identifierRes) {
                if (!FlavorUtils.isTwoPanel(getContext())
                        || mEnergyModesHelper.requiresConfirmation(
                                currentEnergyMode, newEnergyMode)) {
                    Intent intent = new Intent(getContext(), EnergyModeConfirmationActivity.class);
                    intent.putExtra(EnergyModeConfirmationActivity.EXTRA_ENERGY_MODE_ID,
                            getContext().getString(energyModeId));
                    getContext().startActivity(intent);
                } else {
                    if (isThreadEnabled && newEnergyMode != MODE_HIGH_ENERGY) {
                        disableThreadNetworkIntentLauncher
                                .launch(getDisableThreadNetworkConfirmationIntent());
                    }
                    else {
                        mEnergyModesHelper.setEnergyMode(newEnergyMode);
                    }
                }
            }

            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    private final ActivityResultLauncher<Intent> disableThreadNetworkIntentLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                mThreadNetworkHelperOptional.get().setEnabled(false);
                                mEnergyModesHelper.setEnergyMode(newEnergyMode);
                                logToggleInteracted(
                                        TvSettingsEnums.NETWORK_T_N, false);
                            }
                        }
                    });

    private final ThreadNetworkHelper.OnStateChangeListener mOnThreadNetworkStateChange =
            new ThreadNetworkHelper.OnStateChangeListener() {
                @Override
                public void isEnabled(boolean enabled) {
                    isThreadEnabled = enabled;
                }
            };

    @Override
    protected int getPageId() {
        return TvSettingsEnums.SYSTEM_POWER_AND_ENERGY_ENERGY_MODES;
    }

    /** Info panel giving more information about an Energy Mode */
    public static class EnergyModeInfoFragment extends InfoFragment {
        @Override
        public View onCreateView(
                LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            int identifier = getArguments().getInt(EXTRA_ENERGY_MODE_IDENTIFIER);
            EnergyModesHelper energyModesHelper = new EnergyModesHelper(getContext());
            EnergyMode mode = energyModesHelper.getEnergyMode(identifier);
            if (mode == null) {
                return null;
            }

            View view = inflater.inflate(R.layout.energy_mode_info_fragment, container, false);

            ImageView titleIcon = view.findViewById(R.id.info_title_icon);

            TextView infoTitle = view.findViewById(R.id.info_title);
            TextView infoSummary = view.findViewById(R.id.info_summary);
            ImageView ecoHintIcon = view.findViewById(R.id.eco_hint_icon);
            TextView ecoHint = view.findViewById(R.id.eco_hint);

            infoTitle.setText(mode.titleRes);
            infoSummary.setText(energyModesHelper.getSummary(mode));
            //TODO(b/321811441): Hide hints until final copy is ready, if not we can remove views.
            ecoHint.setVisibility(View.GONE);
            ecoHintIcon.setVisibility(View.GONE);
            titleIcon.setImageResource(mode.iconRes);

            return view;
        }
    }

    private Intent getDisableThreadNetworkConfirmationIntent() {
        Bundle args = new FullScreenDialogFragment.DialogBuilder()
                .setIcon(Icon.createWithResource(getContext(), R.drawable.ic_info_outline))
                .setTitle(getContext()
                        .getString(R.string.wifi_settings_thread_network_confirmation_title))
                .setMessage(getContext()
                        .getString(R.string.wifi_settings_thread_network_confirmation_message))
                .setPositiveButton(getContext()
                        .getString(
                                R.string.wifi_settings_thread_network_confirmation_button_confirm))
                .setNegativeButton(getContext().getString(R.string.settings_cancel))
                .build();
        return FullScreenConfirmationActivity.getIntent(getContext(), args);
    }
}
