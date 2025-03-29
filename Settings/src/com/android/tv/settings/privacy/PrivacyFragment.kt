/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.tv.settings.privacy

import android.app.tvsettings.TvSettingsEnums
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.tv.settings.R
import com.android.tv.settings.SettingsPreferenceFragment
import com.android.tv.settings.overlay.FlavorUtils
import com.android.tv.settings.util.InstrumentationUtils
import com.android.tv.settings.util.SliceUtils
import com.android.tv.settings.util.SliceUtilsKt
import com.android.tv.twopanelsettings.slices.CustomContentDescriptionPreference
import com.android.tv.twopanelsettings.slices.SlicePreference
import com.android.tv.twopanelsettings.slices.SliceShard
import com.android.tv.twopanelsettings.slices.compat.Slice
import kotlinx.coroutines.launch

/**
 * The Privacy policies screen in Settings.
 */
@Keep
class PrivacyFragment : SettingsPreferenceFragment(), SliceShard.Callbacks {
    private var mOverlaySecuritySlicePreference: Preference? = null
    private var mSecurityPreference: Preference? = null
    private var mUpdateSlicePreference: Preference? = null
    private var mSliceShard: SliceShard? = null
    private val preferenceScreenResId: Int
        get() = when (FlavorUtils.getFlavor(context)) {
            FlavorUtils.FLAVOR_X, FlavorUtils.FLAVOR_VENDOR -> R.xml.privacy_x
            else -> R.xml.privacy
        }

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        val sliceUri = getString(R.string.privacy_fragment_slice_uri)
        if (!SliceUtils.isSliceProviderValid(requireContext(), sliceUri)) {
            setPreferencesFromResource(preferenceScreenResId, null)
            return
        }
        setPreferencesFromResource(R.xml.settings_loading, null)
        mSliceShard = SliceShard(this, sliceUri, this,
            getString(R.string.privacy_category_title),
            SliceShard.getPrefContext(requireContext()), true)
    }

    override fun onSlice(slice: Slice?) {
        mSliceShard = null
        if (slice == null) {
            setPreferencesFromResource(preferenceScreenResId, null)
        }
        if (view != null) {
            configurePreferences()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        if (mSliceShard == null) {
            configurePreferences()
        }
        return checkNotNull(super.onCreateView(inflater, container, savedInstanceState))
    }

    private fun configurePreferences() {
        mOverlaySecuritySlicePreference = findPreference(KEY_OVERLAY_SECURITY)
        mSecurityPreference = findPreference(KEY_SECURITY)
        mUpdateSlicePreference = findPreference(KEY_UPDATE)
        val accountPrefCategory = findPreference<PreferenceCategory>(KEY_ACCOUNT_SETTINGS_CATEGORY)
        val assistantSlicePreference = findPreference<Preference>(KEY_ASSISTANT)
        val purchasesSlicePreference = findPreference<Preference>(KEY_PURCHASES)
        val adsPreference = findPreference<Preference>(KEY_ADS)
        val deviceLockPreference = findPreference<Preference>(KEY_DEVICE_LOCK)
        PrivacyToggle.MIC_TOGGLE.preparePreferenceWithSensorFragment(context,
            findPreference(KEY_MIC), SensorFragment.TOGGLE_EXTRA)
        PrivacyToggle.CAMERA_TOGGLE.preparePreferenceWithSensorFragment(context,
            findPreference(KEY_CAMERA), SensorFragment.TOGGLE_EXTRA)
        adsPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent().setAction("com.google.android.gms.settings.ADS_PRIVACY")
            startActivity(intent)
            true
        }
        if (adsPreference is CustomContentDescriptionPreference) {
            adsPreference.contentDescription = resources.getString(R.string.ads_content_description)
        }
        if (FlavorUtils.getFeatureFactory(requireContext()).getBasicModeFeatureProvider()
                .isBasicMode(requireContext())) {
            accountPrefCategory?.isVisible = false
            assistantSlicePreference?.isVisible = false
            purchasesSlicePreference?.isVisible = false
            showSecurityPreference(mSecurityPreference, mOverlaySecuritySlicePreference)
            mUpdateSlicePreference?.isVisible = false
            deviceLockPreference?.isVisible = deviceLockPreference is SlicePreference
                    && SliceUtils.isSliceProviderValid(context, deviceLockPreference.uri)
        } else {
            deviceLockPreference?.isVisible = false;
            assistantSlicePreference?.isVisible = assistantSlicePreference is SlicePreference
                    && SliceUtils.isSliceProviderValid(context, assistantSlicePreference.uri)
            purchasesSlicePreference?.isVisible = purchasesSlicePreference is SlicePreference
                    && SliceUtils.isSliceProviderValid(context, purchasesSlicePreference.uri)
            accountPrefCategory?.isVisible = assistantSlicePreference?.isVisible == true
                    || purchasesSlicePreference?.isVisible == true
            viewLifecycleOwner.lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    if (isOverlaySecuritySlicePreferenceEnabled(mOverlaySecuritySlicePreference)) {
                        showOverlaySecuritySlicePreference(
                            mOverlaySecuritySlicePreference, mSecurityPreference)
                    } else {
                        showSecurityPreference(mSecurityPreference, mOverlaySecuritySlicePreference)
                    }
                    mUpdateSlicePreference?.isVisible =
                        isUpdateSlicePreferenceEnabled(mUpdateSlicePreference)
                }
            }
        }

    }

    private suspend fun isOverlaySecuritySlicePreferenceEnabled(
            overlaySecuritySlicePreference: Preference?): Boolean {
        return (overlaySecuritySlicePreference is SlicePreference
                && SliceUtilsKt.isSettingsSliceEnabled(
                requireContext(),
                overlaySecuritySlicePreference.uri,
                TOP_LEVEL_SLICE_URI))
    }

    private fun showOverlaySecuritySlicePreference(
            overlaySecuritySlicePreference: Preference?,
            securityPreference: Preference?) {
        overlaySecuritySlicePreference?.isVisible = true
        securityPreference?.isVisible = false
    }

    private fun showSecurityPreference(
            securityPreference: Preference?,
            overlaySecuritySlicePreference: Preference?) {
        securityPreference?.isVisible = true
        overlaySecuritySlicePreference?.isVisible = false
    }

    private suspend fun isUpdateSlicePreferenceEnabled(
            updateSlicePreference: Preference?): Boolean {
        return (updateSlicePreference is SlicePreference
                && SliceUtilsKt.isSettingsSliceEnabled(
                requireContext(),
                updateSlicePreference.uri,
                TOP_LEVEL_SLICE_URI))
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            KEY_USAGE -> InstrumentationUtils.logEntrySelected(TvSettingsEnums.PRIVACY_DIAGNOSTICS)
            KEY_ADS -> InstrumentationUtils.logEntrySelected(TvSettingsEnums.PRIVACY_ADS)
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun getPageId(): Int {
        return TvSettingsEnums.PRIVACY
    }

    companion object {
        private const val KEY_ACCOUNT_SETTINGS_CATEGORY = "accountSettings"
        private const val KEY_USAGE = "usageAndDiag"
        private const val KEY_ADS = "ads"
        private const val KEY_ASSISTANT = "assistant"
        private const val KEY_PURCHASES = "purchases"
        private const val KEY_DEVICE_LOCK = "deviceLock"
        private const val KEY_SECURITY = "security"
        private const val KEY_OVERLAY_SECURITY = "overlay_security"
        private const val KEY_MIC = "microphone"
        private const val KEY_CAMERA = "camera"
        private const val KEY_UPDATE = "update"
        private const val TOP_LEVEL_SLICE_URI = "top_level_settings_slice_uri"
    }
}
