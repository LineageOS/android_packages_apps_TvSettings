/*
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

import static com.android.tv.settings.device.eco.EnergyModesHelper.isLowPowerStandbySupported;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.TextUtils;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.settings.customization.CustomizationConstants;
import com.android.tv.settings.customization.Partner;
import com.android.tv.settings.customization.PartnerPreferencesMerger;
import com.android.tv.settings.device.LimitNetworkInStandbyConfirmationDialogActivity;
import com.android.tv.settings.library.util.SliceUtils;
import com.android.tv.settings.overlay.FlavorUtils;
import com.android.tv.twopanelsettings.slices.SlicePreference;
import com.android.tv.twopanelsettings.slices.SliceShard;
import com.android.tv.twopanelsettings.slices.compat.Slice;

/** Power and energy settings. */
@Keep
public class PowerAndEnergyFragment extends SettingsPreferenceFragment
        implements SliceShard.Callbacks {
    private static final String KEY_LIMIT_NETWORK = "limit_network_in_standby";
    private static final String KEY_ENERGY_MODES = "energy_modes";

    private SliceShard mSliceShard;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        var sliceUri = getString(R.string.power_energy_fragment_slice_uri);
        if (!SliceUtils.isSliceProviderValid(requireContext(), sliceUri)) {
            setPreferencesFromResource(R.xml.power_and_energy, null);
            configurePreferences();
            return;
        }

        setPreferencesFromResource(R.xml.settings_loading, null);
        mSliceShard = new SliceShard(this, sliceUri, this,
                getString(R.string.power_and_energy),
                SliceShard.Companion.getPrefContext(requireContext()), true);
    }

    @Override
    public void onSlice(@Nullable Slice slice) {
        mSliceShard = null;
        if (slice == null) {
            setPreferencesFromResource(R.xml.power_and_energy, null);
        }
        configurePreferences();
    }

    private void configurePreferences() {
        updateLowPowerStandbyPreferences();
        updatePowerOnBehaviourPreference();

        if (Partner.getInstance(getContext()).isCustomizationPackageProvided()) {
            PartnerPreferencesMerger.mergePreferences(
                    getContext(),
                    getPreferenceScreen(),
                    CustomizationConstants.POWER_AND_ENERGY_SCREEN
            );
        }
    }

    private void updateLowPowerStandbyPreferences() {
        final Context context = getContext();
        final Preference limitNetworkPreference = findPreference(KEY_LIMIT_NETWORK);
        final Preference energyModesPreference = findPreference(KEY_ENERGY_MODES);

        final EnergyModesHelper energyModesHelper = new EnergyModesHelper(context);
        final boolean lowPowerStandbySupported = isLowPowerStandbySupported(context);
        final boolean enableEnergyModes = energyModesHelper.areEnergyModesAvailable();
        final boolean isBasicMode = FlavorUtils.getFeatureFactory(
                        context).getBasicModeFeatureProvider()
                .isBasicMode(context);

        if (limitNetworkPreference != null) {
            limitNetworkPreference.setVisible(
                    lowPowerStandbySupported && !enableEnergyModes && !isBasicMode);
        }
        if (energyModesPreference != null) {
            energyModesPreference.setVisible(lowPowerStandbySupported && enableEnergyModes);
        }

        if (limitNetworkPreference != null && limitNetworkPreference.isVisible()) {
            final PowerManager powerManager = context.getSystemService(PowerManager.class);
            final boolean lowPowerStandbyEnabled = powerManager.isLowPowerStandbyEnabled();
            ((TwoStatePreference) limitNetworkPreference).setChecked(lowPowerStandbyEnabled);
        }
    }

    private void updatePowerOnBehaviourPreference() {
        final Preference powerOnBehaviorPreference = findPreference("power_on_behavior");
        if (powerOnBehaviorPreference != null && SliceUtils.isSliceProviderValid(
                getContext(), ((SlicePreference) powerOnBehaviorPreference).getUri())) {
            powerOnBehaviorPreference.setVisible(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mSliceShard == null) {
            updateLowPowerStandbyPreferences();
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final String key = preference.getKey();
        switch (key) {
            case KEY_LIMIT_NETWORK:
                if (!((TwoStatePreference) preference).isChecked()) {
                    // Confirmation dialog for disabling Low Power Standby
                    Intent intent = new Intent(getContext(),
                            LimitNetworkInStandbyConfirmationDialogActivity.class);
                    getContext().startActivity(intent);
                } else {
                    // Enable Low Power Standby
                    PowerManager powerManager = getContext().getSystemService(PowerManager.class);
                    powerManager.setLowPowerStandbyEnabled(true);
                }
                break;
        }
        return super.onPreferenceTreeClick(preference);
    }

    /** Returns whether this fragment will only present the EnergySaver preference */
    public static boolean hasOnlyEnergySaverPreference(Context context) {
        if (isLowPowerStandbySupported(context)) {
            // Has "Limit Network in Standby" / Energy Modes preference
            return false;
        }

        if (!TextUtils.isEmpty(context.getString(R.string.power_boot_resume_slice_uri))) {
            // Has "Power On Behaviour" preference
            return false;
        }

        return true;
    }
}
