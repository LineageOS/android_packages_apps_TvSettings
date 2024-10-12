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
package com.android.tv.settings.device.displaysound

import android.app.tvsettings.TvSettingsEnums
import android.content.ContentResolver
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.hdmi.HdmiControlManager
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import androidx.preference.TwoStatePreference
import com.android.tv.settings.R
import com.android.tv.settings.SettingsPreferenceFragment
import com.android.tv.settings.device.util.DeviceUtils
import com.android.tv.settings.device.displaysound.PreferredDynamicRangeInfo.MatchContentDynamicRangeInfoFragment
import com.android.tv.settings.overlay.FlavorUtils
import com.android.tv.settings.util.InstrumentationUtils
import com.android.tv.settings.util.ResolutionSelectionUtils
import com.android.tv.settings.util.SliceUtils
import com.android.tv.settings.util.SliceUtilsKt
import com.android.tv.twopanelsettings.slices.SlicePreference
import kotlinx.coroutines.launch

/**
 * The "Display & sound" screen in TV Settings.
 */
@Keep
class DisplaySoundFragment : SettingsPreferenceFragment(), DisplayManager.DisplayListener {
    lateinit var mAudioManager: AudioManager
    lateinit var mHdmiControlManager: HdmiControlManager
    lateinit var mDisplayManager: DisplayManager
    private var mCurrentDeviceName: String? = null
    private var mCurrentMode: Display.Mode? = null

    override fun onAttach(context: Context) {
        mAudioManager = context.getSystemService(AudioManager::class.java) as AudioManager
        mHdmiControlManager =
            context.getSystemService(HdmiControlManager::class.java) as HdmiControlManager
        mDisplayManager = displayManager
        super.onAttach(context)
    }

    private val preferenceScreenResId: Int
        get() = when (FlavorUtils.getFlavor(context)) {
            FlavorUtils.FLAVOR_CLASSIC, FlavorUtils.FLAVOR_TWO_PANEL -> R.xml.display_sound
            FlavorUtils.FLAVOR_X, FlavorUtils.FLAVOR_VENDOR -> R.xml.display_sound_x
            else -> R.xml.display_sound
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(preferenceScreenResId, null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        findPreference<TwoStatePreference>(KEY_SOUND_EFFECTS)?.isChecked = soundEffectsEnabled
        updateCecPreference()

        mCurrentDeviceName = DeviceUtils.getDeviceName(context)
        updateVolumeChangePreference()

        val display = mDisplayManager.getDisplay(Display.DEFAULT_DISPLAY)
        if (display.systemPreferredDisplayMode != null) {
            mDisplayManager.registerDisplayListener(this, null)
            mCurrentMode = mDisplayManager.globalUserPreferredDisplayMode
            updateResolutionTitleDescription(ResolutionSelectionUtils.modeToString(
                mCurrentMode, context))
        } else {
            removePreference(findPreference(KEY_RESOLUTION_TITLE))
        }
        val dynamicRangePreference = findPreference<SwitchPreference>(KEY_DYNAMIC_RANGE)
        if (mDisplayManager.supportedHdrOutputTypes.isEmpty()) {
            removePreference(dynamicRangePreference)
        } else if (FlavorUtils.getFlavor(context) != FlavorUtils.FLAVOR_CLASSIC) {
            createInfoFragments()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                updateDefaultAudioOutputSettings()
            }
        }
        return checkNotNull(super.onCreateView(inflater, container, savedInstanceState))
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::mDisplayManager.isInitialized) {
            mDisplayManager.unregisterDisplayListener(this)
        }
    }

    override fun onResume() {
        super.onResume()
        // Update the subtitle of CEC setting when navigating back to this page.
        updateCecPreference()
        findPreference<SwitchPreference>(KEY_DYNAMIC_RANGE)?.isChecked =
            DisplaySoundUtils.getMatchContentDynamicRangeStatus(mDisplayManager)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (TextUtils.equals(preference.key, KEY_SOUND_EFFECTS)) {
            val soundPref = preference as TwoStatePreference
            InstrumentationUtils
                .logToggleInteracted(
                    TvSettingsEnums.DISPLAY_SOUND_SYSTEM_SOUNDS, soundPref.isChecked)
            soundEffectsEnabled = soundPref.isChecked
        } else if (TextUtils.equals(preference.key, KEY_DYNAMIC_RANGE)) {
            val dynamicPref = preference as SwitchPreference
            DisplaySoundUtils
                .setMatchContentDynamicRangeStatus(context, mDisplayManager, dynamicPref.isChecked)
        }
        return super.onPreferenceTreeClick(preference)
    }

    private var soundEffectsEnabled: Boolean
        get() = getSoundEffectsEnabled(requireActivity().contentResolver)
        private set(enabled) {
            if (enabled) {
                mAudioManager.loadSoundEffects()
            } else {
                mAudioManager.unloadSoundEffects()
            }
            Settings.System.putInt(requireActivity().contentResolver,
                Settings.System.SOUND_EFFECTS_ENABLED, if (enabled) 1 else 0)
        }

    private fun updateCecPreference() {
        findPreference<Preference>(KEY_CEC)?.apply{
            if (this is SlicePreference && SliceUtils.isSliceProviderValid(
                    context, this.uri)) {
                val cecEnabled = (mHdmiControlManager.getHdmiCecEnabled()
                        == HdmiControlManager.HDMI_CEC_CONTROL_ENABLED)
                setSummary(if (cecEnabled) R.string.enabled else R.string.disabled)
                isVisible = true
            } else {
                isVisible = false
            }
        }
    }

    private suspend fun updateDefaultAudioOutputSettings() {
        findPreference<SlicePreference>(KEY_DEFAULT_AUDIO_OUTPUT_SETTINGS_SLICE)?.apply {
            isVisible = SliceUtils.isSliceProviderValid(context,
                this.uri)
                    && SliceUtilsKt.isSettingsSliceEnabled(context,
                this.uri, null)
        }
    }

    private fun updateVolumeChangePreference() {
        findPreference<Preference>(VOLUME_CHANGE)?.apply {
            setTitle(requireContext().resources.getString(R.string.volume_change_settings_title,
                mCurrentDeviceName))
            isVisible = requireContext().resources.getBoolean(R.bool.config_volume_change)
        }
    }

    override fun getPageId(): Int {
        return TvSettingsEnums.DISPLAY_SOUND
    }

    override fun onDisplayAdded(displayId: Int) {}
    override fun onDisplayRemoved(displayId: Int) {}
    override fun onDisplayChanged(displayId: Int) {
        val newMode = mDisplayManager.globalUserPreferredDisplayMode
        if (mCurrentMode != newMode) {
            updateResolutionTitleDescription(
                ResolutionSelectionUtils.modeToString(newMode, context))
            mCurrentMode = newMode
        }
    }

    @get:VisibleForTesting
    val displayManager: DisplayManager
        get() = requireContext().getSystemService(DisplayManager::class.java) as DisplayManager

    private fun updateResolutionTitleDescription(summary: String) {
        findPreference<Preference>(KEY_RESOLUTION_TITLE)?.summary = summary
    }

    private fun removePreference(preference: Preference?) {
        if (preference != null) {
            preferenceScreen.removePreference(preference)
        }
    }

    private fun createInfoFragments() {
        findPreference<Preference>(KEY_DYNAMIC_RANGE)?.fragment =
            MatchContentDynamicRangeInfoFragment::class.java.name
    }

    companion object {
        const val KEY_SOUND_EFFECTS = "sound_effects"
        private const val KEY_CEC = "cec"
        private const val KEY_DEFAULT_AUDIO_OUTPUT_SETTINGS_SLICE = "default_audio_output_settings"
        private const val KEY_RESOLUTION_TITLE = "resolution_selection"
        private const val KEY_DYNAMIC_RANGE = "match_content_dynamic_range"

        private const val VOLUME_CHANGE = "volume_change"

        fun newInstance(): DisplaySoundFragment {
            return DisplaySoundFragment()
        }

        fun getSoundEffectsEnabled(contentResolver: ContentResolver?): Boolean {
            return (Settings.System.getInt(contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 1)
                    != 0)
        }
    }
}