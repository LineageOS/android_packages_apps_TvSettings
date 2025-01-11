/*
 * Copyright (C) 2024 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.tv.settings.device.apps

import android.app.Activity
import android.app.tvsettings.TvSettingsEnums
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.DeviceConfig
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import com.android.settingslib.core.AbstractPreferenceController
import com.android.tv.settings.PreferenceControllerFragment
import com.android.tv.settings.R
import com.android.tv.settings.library.util.LibUtils
import com.android.tv.settings.overlay.FlavorUtils
import com.android.tv.settings.util.InstrumentationUtils
import com.android.tv.settings.util.SliceUtilsKt
import com.android.tv.twopanelsettings.slices.SlicePreference
import kotlinx.coroutines.launch

/**
 * Fragment for managing recent apps, and apps permissions.
 */
@Keep
class AppsFragment : PreferenceControllerFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        findPreference<Preference>(KEY_PERMISSIONS)?.apply {
            isVisible = TextUtils.isEmpty(
                    arguments?.getString(AppsActivity.EXTRA_VOLUME_UUID))
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                InstrumentationUtils.logEntrySelected(TvSettingsEnums.APPS_APP_PERMISSIONS)
                false
            }
        }
        findPreference<Preference>(KEY_HIBERNATED_APPS)?.isVisible = isHibernationEnabled

        findPreference<Preference>(KEY_ADD_APPS)?.apply {
            val addAppsUrl = requireContext().getString(R.string.add_apps_intent)
            if (addAppsUrl.isNotEmpty()) {
                isVisible = true
                intent = Intent.parseUri(addAppsUrl, 0)
            } else {
                isVisible = false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val securityPreference = findPreference<Preference>(KEY_SECURITY)
                val overlaySecuritySlicePreference =
                        findPreference<Preference>(KEY_OVERLAY_SECURITY)
                val updateSlicePreference = findPreference<Preference>(KEY_UPDATE)
                if (FlavorUtils.getFeatureFactory(requireContext()).getBasicModeFeatureProvider()
                                .isBasicMode(requireContext())) {
                    showSecurityPreference(securityPreference, overlaySecuritySlicePreference)
                    updateSlicePreference?.isVisible = false
                } else {
                    if (isOverlaySecuritySlicePreferenceEnabled(overlaySecuritySlicePreference)) {
                        showOverlaySecuritySlicePreference(
                                overlaySecuritySlicePreference, securityPreference)
                    } else {
                        showSecurityPreference(securityPreference, overlaySecuritySlicePreference)
                    }
                    updateSlicePreference?.isVisible =
                                isUpdateSlicePreferenceEnabled(updateSlicePreference)
                }
            }
        }
        return checkNotNull(super.onCreateView(inflater, container, savedInstanceState))
    }

    private suspend fun isOverlaySecuritySlicePreferenceEnabled(
            overlaySecuritySlicePreference: Preference?): Boolean {
        return (overlaySecuritySlicePreference is SlicePreference
                && SliceUtilsKt.isSettingsSliceEnabled(requireContext(),
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

    override fun getPreferenceScreenResId(): Int {
        return when (FlavorUtils.getFlavor(context)) {
            FlavorUtils.FLAVOR_X, FlavorUtils.FLAVOR_VENDOR -> R.xml.apps_x
            else -> R.xml.apps
        }
    }

    override fun onCreatePreferenceControllers(context: Context):
            List<AbstractPreferenceController> {
        val activity: Activity? = activity
        val app = activity?.application
        val controllers: MutableList<AbstractPreferenceController> = ArrayList()
        controllers.add(RecentAppsPreferenceController(getContext(), app))
        return controllers
    }

    override fun getPageId(): Int {
        return TvSettingsEnums.APPS
    }

    companion object {
        private const val KEY_PERMISSIONS = "Permissions"
        private const val KEY_SECURITY = "security"
        private const val KEY_OVERLAY_SECURITY = "overlay_security"
        private const val KEY_UPDATE = "update"
        private const val KEY_ADD_APPS="add_apps"
        private const val TOP_LEVEL_SLICE_URI = "top_level_settings_slice_uri"
        private const val KEY_HIBERNATED_APPS = "see_unused_apps"
        @JvmStatic
        fun prepareArgs(b: Bundle, volumeUuid: String?, volumeName: String?) {
            b.putString(AppsActivity.EXTRA_VOLUME_UUID, volumeUuid)
            b.putString(AppsActivity.EXTRA_VOLUME_NAME, volumeName)
        }

        fun newInstance(volumeUuid: String?, volumeName: String?): AppsFragment {
            val b = Bundle(2)
            prepareArgs(b, volumeUuid, volumeName)
            val f = AppsFragment()
            f.arguments = b
            return f
        }

        private val isHibernationEnabled: Boolean
            get() = DeviceConfig.getBoolean(
                    DeviceConfig.NAMESPACE_APP_HIBERNATION,
                    LibUtils.PROPERTY_APP_HIBERNATION_ENABLED, /* defaultValue= */ false)
    }
}