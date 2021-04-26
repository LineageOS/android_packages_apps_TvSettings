/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tv.settings.device

import android.app.tvsettings.TvSettingsEnums
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.media.AudioManager
import android.media.tv.TvInputManager
import android.os.Bundle
import android.os.SystemProperties
import android.os.UserHandle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.annotation.VisibleForTesting
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import com.android.settingslib.development.DevelopmentSettingsEnabler
import com.android.tv.settings.LongClickPreference
import com.android.tv.settings.MainFragment
import com.android.tv.settings.R
import com.android.tv.settings.SettingsPreferenceFragment
import com.android.tv.settings.about.RebootConfirmFragment
import com.android.tv.settings.autofill.AutofillHelper
import com.android.tv.settings.customization.CustomizationConstants
import com.android.tv.settings.customization.Partner
import com.android.tv.settings.customization.PartnerPreferencesMerger
import com.android.tv.settings.device.eco.PowerAndEnergyFragment
import com.android.tv.settings.inputmethod.InputMethodHelper
import com.android.tv.settings.overlay.FlavorUtils
import com.android.tv.settings.privacy.PrivacyToggle
import com.android.tv.settings.privacy.SensorFragment
import com.android.tv.settings.system.SecurityFragment
import com.android.tv.settings.util.InstrumentationUtils
import com.android.tv.settings.util.SliceUtils
import com.android.tv.settings.util.SliceUtilsKt
import com.android.tv.twopanelsettings.TwoPanelSettingsFragment
import com.android.tv.twopanelsettings.slices.SlicePreference
import kotlinx.coroutines.launch

/**
 * The "Device Preferences" screen in TV settings.
 */
@Keep
class DevicePrefFragment : SettingsPreferenceFragment(), LongClickPreference.OnLongClickListener {
    private var mSoundsSwitchPref: TwoStatePreference? = null
    private var mInputSettingNeeded = false
    private var mAudioManager: AudioManager? = null
    private val preferenceScreenResId: Int
        get() = if (isRestricted) {
            R.xml.device_restricted
        } else when (FlavorUtils.getFlavor(context)) {
            FlavorUtils.FLAVOR_CLASSIC -> R.xml.device
            FlavorUtils.FLAVOR_TWO_PANEL -> R.xml.device_two_panel
            FlavorUtils.FLAVOR_X -> R.xml.device_x
            FlavorUtils.FLAVOR_VENDOR -> R.xml.device_vendor
            else -> R.xml.device
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val manager = requireContext().getSystemService(
                Context.TV_INPUT_SERVICE) as TvInputManager
        for (input in manager.tvInputList) {
            if (input.isPassthroughInput) {
                mInputSettingNeeded = true
            }
        }
        mAudioManager = requireContext().getSystemService(AudioManager::class.java) as AudioManager
        if (SystemProperties.getInt("ro.hdmi.device_type", 0) == 4) {
            mInputSettingNeeded = true
        }
        super.onCreate(savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(preferenceScreenResId, null)
        if (Partner.getInstance(context).isCustomizationPackageProvided) {
            PartnerPreferencesMerger.mergePreferences(
                    context,
                    preferenceScreen,
                    CustomizationConstants.DEVICE_SCREEN
            )
        }
        mSoundsSwitchPref = findPreference(KEY_SOUNDS_SWITCH)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        mSoundsSwitchPref?.isChecked = soundEffectsEnabled
        findPreference<Preference>(KEY_INPUTS)?.isVisible = mInputSettingNeeded
        findPreference<LongClickPreference>(KEY_REBOOT)?.setLongClickListener(this)
        PrivacyToggle.MIC_TOGGLE.preparePreferenceWithSensorFragment(context,
                findPreference(KEY_MIC), SensorFragment.TOGGLE_EXTRA)
        PrivacyToggle.CAMERA_TOGGLE.preparePreferenceWithSensorFragment(context,
                findPreference(KEY_CAMERA), SensorFragment.TOGGLE_EXTRA)
        updateDeveloperOptions()
        updateSounds()
        updateGoogleSettings()
        updateCastSettings()
        updateFastpairSettings()
        updateKeyboardAutofillSettings()
        updateAmbientSettings()
        updatePowerAndEnergySettings()
        updateSystemTvSettings()
        hideIfIntentUnhandled(findPreference(KEY_HOME_SETTINGS))
        hideIfIntentUnhandled(findPreference(KEY_CAST_SETTINGS))
        hideIfIntentUnhandled(findPreference(KEY_USAGE))
        viewLifecycleOwner.lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                updateInternalSettings()
                updateAssistantBroadcastSlice()
            }
        }
        return checkNotNull(super.onCreateView(inflater, container, savedInstanceState))
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            KEY_HOME_SETTINGS -> InstrumentationUtils.logEntrySelected(TvSettingsEnums.PREFERENCES_HOME_SCREEN)
            KEY_GOOGLE_SETTINGS -> InstrumentationUtils.logEntrySelected(TvSettingsEnums.PREFERENCES_ASSISTANT)
            KEY_CAST_SETTINGS -> InstrumentationUtils.logEntrySelected(TvSettingsEnums.PREFERENCES_CHROMECAST_SHELL)
            KEY_REBOOT -> InstrumentationUtils.logEntrySelected(TvSettingsEnums.SYSTEM_REBOOT)
            KEY_SOUNDS_SWITCH ->
                mSoundsSwitchPref?.let {
                InstrumentationUtils.logToggleInteracted(TvSettingsEnums.DISPLAY_SOUND_SYSTEM_SOUNDS,
                        it.isChecked)
                soundEffectsEnabled = it.isChecked
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onPreferenceLongClick(preference: Preference): Boolean {
        if (TextUtils.equals(preference.key, KEY_REBOOT)) {
            InstrumentationUtils.logEntrySelected(TvSettingsEnums.SYSTEM_REBOOT)
            val fragment = callbackFragment
            if (fragment is LeanbackSettingsFragmentCompat) {
                fragment.startImmersiveFragment(
                        RebootConfirmFragment.newInstance(true /* safeMode */))
                return true
            } else if (fragment is TwoPanelSettingsFragment) {
                fragment.startImmersiveFragment(
                        RebootConfirmFragment.newInstance(true /* safeMode */))
                return true
            }
        }
        return false
    }

    private var soundEffectsEnabled: Boolean
        get() = Settings.System.getInt(requireActivity().contentResolver,
                Settings.System.SOUND_EFFECTS_ENABLED, 1) != 0
        private set(enabled) {
            if (enabled) {
                mAudioManager?.loadSoundEffects()
            } else {
                mAudioManager?.unloadSoundEffects()
            }
            Settings.System.putInt(requireActivity().contentResolver,
                    Settings.System.SOUND_EFFECTS_ENABLED, if (enabled) 1 else 0)
        }

    private fun hideIfIntentUnhandled(preference: Preference?) {
        if (preference == null || !preference.isVisible) {
            return
        }
        preference.isVisible = MainFragment.systemIntentIsHandled(context, preference.intent) != null
    }

    private val isRestricted: Boolean
        get() = SecurityFragment.isRestrictedProfileInEffect(context)

    @VisibleForTesting
    fun updateDeveloperOptions() {
        findPreference<Preference>(KEY_DEVELOPER)?.isVisible =
                DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context)
    }

    private fun updateSounds() {
        findPreference<Preference>(KEY_SOUNDS)?.isVisible =
                MainFragment
                        .systemIntentIsHandled(context, Intent(MainFragment.ACTION_SOUND)) == null

    }

    private fun updateGoogleSettings() {
        findPreference<Preference>(KEY_GOOGLE_SETTINGS)?.apply {
            val info = MainFragment.systemIntentIsHandled(context,
                    this.intent)
            isVisible = info != null

            info?.let {
                icon = it.activityInfo.loadIcon(requireContext().packageManager)
                title = it.activityInfo.loadLabel(requireContext().packageManager)
            }
        }
    }

    @VisibleForTesting
    fun updateCastSettings() {
        findPreference<Preference>(KEY_CAST_SETTINGS)?.apply {
            val info = MainFragment.systemIntentIsHandled(
                    requireContext(), this.intent)
            if (info != null) {
                try {
                    val targetContext = requireContext()
                            .createPackageContext(if (info.resolvePackageName != null) info.resolvePackageName else info.activityInfo.packageName, 0)
                    this.icon = targetContext.getDrawable(info.getIconResource())
                } catch (e: Resources.NotFoundException) {
                    Log.e(TAG, "Cast settings icon not found", e)
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.e(TAG, "Cast settings icon not found", e)
                } catch (e: SecurityException) {
                    Log.e(TAG, "Cast settings icon not found", e)
                }
                title = info.activityInfo.loadLabel(requireContext().packageManager)
            }
        }

        findPreference<SlicePreference>(KEY_CAST_SETTINGS_SLICE)?.apply {
            isVisible = SliceUtils.isSliceProviderValid(requireContext(), this.uri)
                    && !FlavorUtils.getFeatureFactory(requireContext()).getBasicModeFeatureProvider()
                    .isBasicMode(requireContext())
        }
    }

    private suspend fun updateInternalSettings() {
        findPreference<SlicePreference>(KEY_OVERLAY_INTERNAL_SETTINGS_SLICE)?.apply {
            isVisible = SliceUtils.isSliceProviderValid(context, this.uri)
                    && SliceUtilsKt.isSettingsSliceEnabled(requireContext(), this.uri, null)
        }
    }

    private suspend fun updateAssistantBroadcastSlice() {
        findPreference<Preference>(KEY_ASSISTANT_BROADCAST)?.apply {
            isVisible = SliceUtilsKt.isSettingsSliceEnabled(
                    requireContext(),
                    (this as SlicePreference).uri,
                    RES_TOP_LEVEL_ASSISTANT_SLICE_URI)
        }
    }

    @VisibleForTesting
    fun updateFastpairSettings() {
        findPreference<SlicePreference>(KEY_FASTPAIR_SETTINGS_SLICE)?.apply {
            isVisible = SliceUtils.isSliceProviderValid(context, this.uri)
        }
    }

    @VisibleForTesting
    fun updateKeyboardAutofillSettings() {
        val keyboardPref = findPreference<Preference>(KEY_KEYBOARD)
        val candidates = AutofillHelper.getAutofillCandidates(requireContext(),
                requireContext().packageManager, UserHandle.myUserId())

        // Switch title depends on whether there is autofill
        if (candidates.isEmpty()) {
            keyboardPref?.setTitle(R.string.system_keyboard)
        } else {
            keyboardPref?.setTitle(R.string.system_keyboard_autofill)
        }
        var summary: CharSequence = ""
        // append current keyboard to summary
        val defaultImId = InputMethodHelper.getDefaultInputMethodId(context)
        if (!TextUtils.isEmpty(defaultImId)) {
            val info = InputMethodHelper.findInputMethod(defaultImId,
                    InputMethodHelper.getEnabledSystemInputMethodList(context))
            if (info != null) {
                summary = info.loadLabel(requireContext().packageManager)
            }
        }
        // append current autofill to summary
        val appInfo = AutofillHelper.getCurrentAutofill(requireContext(), candidates)
        if (appInfo != null) {
            val autofillInfo = appInfo.loadLabel()
            if (summary.length > 0) {
                requireContext().getString(R.string.string_concat, summary, autofillInfo)
            } else {
                summary = autofillInfo
            }
        }
        keyboardPref?.summary = summary
    }

    private fun updateAmbientSettings() {
        findPreference<SlicePreference>(KEY_AMBIENT_SETTINGS)?.apply {
            isVisible = SliceUtils.isSliceProviderValid(context, this.uri)
        }
    }

    private fun updatePowerAndEnergySettings() {
        val energySaverPref = findPreference<Preference>(KEY_ENERGY_SAVER)
        val powerAndEnergyPref = findPreference<Preference>(KEY_POWER_AND_ENERGY)
        if (energySaverPref == null || powerAndEnergyPref == null) {
            return
        }
        val showPowerAndEnergy = !PowerAndEnergyFragment.hasOnlyEnergySaverPreference(context)
        powerAndEnergyPref.isVisible = showPowerAndEnergy
        energySaverPref.isVisible = !showPowerAndEnergy
    }

    private fun updateSystemTvSettings() {
        findPreference<SlicePreference>(KEY_SYSTEM_TV_SLICE)?.apply {
            isVisible = SliceUtils.isSliceProviderValid(context, this.uri)
        }
    }

    override fun getPageId(): Int {
        return TvSettingsEnums.SYSTEM
    }

    companion object {
        @JvmField
        @VisibleForTesting
        val KEY_DEVELOPER = "developer"

        @JvmField
        @VisibleForTesting
        val KEY_CAST_SETTINGS = "cast"
        private const val KEY_CAST_SETTINGS_SLICE = "cast_settings"

        @JvmField
        @VisibleForTesting
        val KEY_KEYBOARD = "keyboard"
        private const val TAG = "DeviceFragment"
        private const val KEY_USAGE = "usageAndDiag"
        private const val KEY_INPUTS = "inputs"
        private const val KEY_SOUNDS = "sound_effects"
        private const val KEY_SOUNDS_SWITCH = "sound_effects_switch"
        private const val KEY_GOOGLE_SETTINGS = "google_settings"
        private const val KEY_HOME_SETTINGS = "home"
        private const val KEY_REBOOT = "reboot"
        private const val KEY_MIC = "microphone"
        private const val KEY_CAMERA = "camera"
        private const val KEY_FASTPAIR_SETTINGS_SLICE = "fastpair_slice"
        private const val KEY_OVERLAY_INTERNAL_SETTINGS_SLICE = "overlay_internal"
        private const val KEY_ASSISTANT_BROADCAST = "assistant_broadcast"
        private const val KEY_AMBIENT_SETTINGS = "ambient_settings"
        private const val KEY_ENERGY_SAVER = "energysaver"
        private const val KEY_POWER_AND_ENERGY = "power_and_energy"
        private const val RES_TOP_LEVEL_ASSISTANT_SLICE_URI = "top_level_assistant_slice_uri"
        private const val KEY_SYSTEM_TV_SLICE = "menu_system_tv"
    }
}
