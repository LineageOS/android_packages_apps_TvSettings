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
 * limitations under the License
 */
package com.android.tv.settings

import android.accounts.AccountManager
import android.app.tvsettings.TvSettingsEnums
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.icu.text.MessageFormat
import android.net.Uri
import android.os.Bundle
import android.service.settings.suggestions.Suggestion
import android.telephony.CellSignalStrength
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.annotation.Keep
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.suggestions.SuggestionControllerMixinCompat
import com.android.tv.settings.HotwordSwitchController.HotwordStateListener
import com.android.tv.settings.accounts.AccountsFragment
import com.android.tv.settings.accounts.AccountsUtil
import com.android.tv.settings.connectivity.ActiveNetworkProvider
import com.android.tv.settings.connectivity.ConnectivityListener
import com.android.tv.settings.connectivity.ConnectivityListenerLite
import com.android.tv.settings.customization.CustomizationConstants
import com.android.tv.settings.customization.Partner
import com.android.tv.settings.customization.PartnerPreferencesMerger
import com.android.tv.settings.overlay.FlavorUtils
import com.android.tv.settings.suggestions.SuggestionPreference
import com.android.tv.settings.system.SecurityFragment
import com.android.tv.settings.util.InstrumentationUtils
import com.android.tv.settings.util.SliceUtils
import com.android.tv.twopanelsettings.TwoPanelSettingsFragment
import com.android.tv.twopanelsettings.slices.SlicePreference
import com.android.tv.twopanelsettings.slices.SliceShard
import com.android.tv.twopanelsettings.slices.compat.Slice
import java.util.Locale
import java.util.Optional

/**
 * The fragment where all good things begin. Evil is handled elsewhere.
 */
@Keep
open class MainFragment : PreferenceControllerFragment(),
    SuggestionControllerMixinCompat.SuggestionControllerHost,
    SuggestionPreference.Callback,
    HotwordStateListener,
    SliceShard.Callbacks {
    @VisibleForTesting
    var mConnectivityListenerOptional: Optional<ConnectivityListener>? = null

    @VisibleForTesting
    var mBtAdapter: BluetoothAdapter? = null

    @VisibleForTesting
    var mHasBtAccessories: Boolean = false

    @VisibleForTesting
    var mHasAccounts: Boolean = false

    private var mSuggestionQuickSettingPrefsContainer: SuggestionQuickSettingPrefsContainer? = null

    private val mBCMReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateAccessoryPref()
        }
    }

    private var mConnectivityListenerLite: ConnectivityListenerLite? = null

    private var mSliceShard : SliceShard? = null

    override fun getPreferenceScreenResId(): Int {
        return when (FlavorUtils.getFlavor(context)) {
            FlavorUtils.FLAVOR_CLASSIC, FlavorUtils.FLAVOR_TWO_PANEL -> R.xml.main_prefs
            FlavorUtils.FLAVOR_X -> R.xml.main_prefs_x
            FlavorUtils.FLAVOR_VENDOR -> R.xml.main_prefs_vendor
            else -> R.xml.main_prefs
        }
    }

    override fun onAttach(context: Context) {
        mSuggestionQuickSettingPrefsContainer = SuggestionQuickSettingPrefsContainer(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mSuggestionQuickSettingPrefsContainer!!.onCreate()
        if (isWifiScanOptimisationEnabled) {
            mConnectivityListenerLite = ConnectivityListenerLite(
                requireActivity().applicationContext,
                { activeNetworkProvider: ActiveNetworkProvider ->
                    this.updateConnectivityType(
                        activeNetworkProvider
                    )
                },
                lifecycle
            )
            mConnectivityListenerOptional = Optional.empty()
        } else {
            mConnectivityListenerOptional = Optional.of(
                ConnectivityListener(
                    requireActivity().applicationContext,
                    { this.updateConnectivity() },
                    settingsLifecycle
                )
            )
        }
        mBtAdapter = BluetoothAdapter.getDefaultAdapter()
        super.onCreate(savedInstanceState)
        // This is to record the initial start of Settings root in two panel settings case, as the
        // MainFragment is the left-most pane and will not be slided in from preview pane. For
        // classic settings case, the event will be recorded in onResume() as this is an instance
        // of SettingsPreferenceFragment.
        if (callbackFragment is TwoPanelSettingsFragment) {
            InstrumentationUtils.logPageFocused(pageId, true)
        }
    }

    private val isWifiScanOptimisationEnabled: Boolean
        get() = context!!.resources.getBoolean(R.bool.wifi_scan_optimisation_enabled)

    private val shouldHideChannelsAndInputs: Boolean
        get() = context!!.resources.getBoolean(R.bool.config_hide_channels_and_inputs)

    private fun updateConnectivityType(activeNetworkProvider: ActiveNetworkProvider) {
        val networkPref = findPreference<Preference>(KEY_NETWORK)
            ?: return

        if (activeNetworkProvider.isTypeCellular) {
            networkPref.setIcon(R.drawable.ic_cell_signal_4_white)
        } else if (activeNetworkProvider.isTypeEthernet) {
            networkPref.setIcon(R.drawable.ic_ethernet_white)
            networkPref.setSummary(R.string.connectivity_summary_ethernet_connected)
        } else if (activeNetworkProvider.isTypeWifi) {
            networkPref.setIcon(R.drawable.ic_wifi_signal_4_white)
            networkPref.summary = activeNetworkProvider.ssid
        } else {
            if (activeNetworkProvider.isWifiEnabled) {
                networkPref.setIcon(R.drawable.ic_wifi_not_connected)
                networkPref.setSummary(R.string.connectivity_summary_no_network_connected)
            } else {
                networkPref.setIcon(R.drawable.ic_wifi_signal_off_white)
                networkPref.setSummary(R.string.connectivity_summary_wifi_disabled)
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val sliceUri = getString(R.string.main_fragment_slice_uri)

        if (!SliceUtils.isSliceProviderValid(requireContext(), sliceUri)) {
            setPreferencesFromResource(preferenceScreenResId, null)
            configurePreferences()
        } else {
            setPreferencesFromResource(R.xml.settings_loading, null)

            val themeTypedValue = TypedValue()
            requireContext().theme.resolveAttribute(
                com.android.tv.twopanelsettings.R.attr.preferenceTheme,
                themeTypedValue,
                true
            )
            val prefContext = ContextThemeWrapper(activity, themeTypedValue.resourceId)
            mSliceShard = SliceShard(
                this, sliceUri, this,
                getString(R.string.settings_app_name), prefContext, true
            )
        }
    }

    override fun onSlice(slice: Slice?) {
        if (slice == null) {
            setPreferencesFromResource(preferenceScreenResId, null)
        }
        configurePreferences()
    }

    private fun configurePreferences() {
        if (Partner.getInstance(context).isCustomizationPackageProvided) {
            PartnerPreferencesMerger.mergePreferences(
                context,
                preferenceScreen,
                CustomizationConstants.MAIN_SCREEN
            )
        }
        if (isRestricted) {
            val appPref = findPreference<Preference>(KEY_APPLICATIONS)
            if (appPref != null) {
                appPref.isVisible = false
            }
            val accountsPref = findPreference<Preference>(
                KEY_ACCOUNTS_AND_SIGN_IN
            )
            if (accountsPref != null) {
                accountsPref.isVisible = false
            }
        }
        if (!supportBluetooth()) {
            val accessoryPreference = findPreference<Preference>(KEY_ACCESSORIES)
            if (accessoryPreference != null) {
                accessoryPreference.isVisible = false
            }
        }
        if (FlavorUtils.isTwoPanel(context)) {
            val displaySoundPref = findPreference<Preference>(
                KEY_DISPLAY_AND_SOUND
            )
            if (displaySoundPref != null) {
                displaySoundPref.isVisible = true
            }
            val privacyPref = findPreference<Preference>(KEY_PRIVACY)
            if (privacyPref != null) {
                privacyPref.isVisible = true
            }
        }
        mSuggestionQuickSettingPrefsContainer!!.onCreatePreferences()
        updateSoundSettings()
        SliceUtils.maybeUseSlice(
            findPreference(KEY_DISPLAY_AND_SOUND),
            findPreference(KEY_DISPLAY_AND_SOUND_SLICE)
        )
        mSuggestionQuickSettingPrefsContainer!!.showOrHideQuickSettings()
        updateAccountPref()
        updateAccessoryPref()
        updateBasicModeSuggestion()

        val sliceInputsPreference = findPreference<SlicePreference>(
            KEY_CHANNELS_AND_INPUTS_SLICE
        )
        val channelsAndInputsPreference = findPreference<Preference>(KEY_CHANNELS_AND_INPUTS)
        if (shouldHideChannelsAndInputs) {
            if (sliceInputsPreference != null) {
                sliceInputsPreference.setVisible(false)
            }
            if (channelsAndInputsPreference != null) {
                channelsAndInputsPreference.setVisible(false)
            }
        } else {
            if (sliceInputsPreference != null
                && !SliceUtils.isSliceProviderValid(
                    requireContext(), sliceInputsPreference.uri
                )
            ) {
                sliceInputsPreference.uri = getString(R.string.channels_and_inputs_fallback_slice_uri)
            }
            SliceUtils.maybeUseSlice(findPreference(KEY_CHANNELS_AND_INPUTS), sliceInputsPreference)
        }

        SliceUtils.maybeUseSlice(
            findPreference(KEY_HELP_AND_FEEDBACK),
            findPreference(KEY_HELP_AND_FEEDBACK_SLICE)
        )
    }

    @VisibleForTesting
    fun updateConnectivity() {
        if (!mConnectivityListenerOptional!!.isPresent) {
            return
        }
        val networkPref = findPreference<Preference>(KEY_NETWORK)
            ?: return

        if (mConnectivityListenerOptional!!.get().isCellConnected) {
            val signal = mConnectivityListenerOptional!!.get().cellSignalStrength
            when (signal) {
                CellSignalStrength.SIGNAL_STRENGTH_GREAT -> networkPref.setIcon(
                    R.drawable.ic_cell_signal_4_white
                )

                CellSignalStrength.SIGNAL_STRENGTH_GOOD -> networkPref.setIcon(R.drawable.ic_cell_signal_3_white)
                CellSignalStrength.SIGNAL_STRENGTH_MODERATE -> networkPref.setIcon(
                    R.drawable.ic_cell_signal_2_white
                )

                CellSignalStrength.SIGNAL_STRENGTH_POOR -> networkPref.setIcon(R.drawable.ic_cell_signal_1_white)
                CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN -> networkPref.setIcon(
                    R.drawable.ic_cell_signal_0_white
                )

                else -> networkPref.setIcon(R.drawable.ic_cell_signal_0_white)
            }
        } else if (mConnectivityListenerOptional!!.get().isEthernetConnected) {
            networkPref.setIcon(R.drawable.ic_ethernet_white)
            networkPref.setSummary(R.string.connectivity_summary_ethernet_connected)
        } else if (mConnectivityListenerOptional!!.get().isWifiEnabledOrEnabling) {
            if (mConnectivityListenerOptional!!.get().isWifiConnected) {
                val signal = mConnectivityListenerOptional!!.get().getWifiSignalStrength(5)
                when (signal) {
                    4 -> networkPref.setIcon(R.drawable.ic_wifi_signal_4_white)
                    3 -> networkPref.setIcon(R.drawable.ic_wifi_signal_3_white)
                    2 -> networkPref.setIcon(R.drawable.ic_wifi_signal_2_white)
                    1 -> networkPref.setIcon(R.drawable.ic_wifi_signal_1_white)
                    0 -> networkPref.setIcon(R.drawable.ic_wifi_signal_0_white)
                    else -> networkPref.setIcon(R.drawable.ic_wifi_signal_0_white)
                }
                networkPref.summary = mConnectivityListenerOptional!!.get().ssid
            } else {
                networkPref.setIcon(R.drawable.ic_wifi_not_connected)
                networkPref.setSummary(R.string.connectivity_summary_no_network_connected)
            }
        } else {
            networkPref.setIcon(R.drawable.ic_wifi_signal_off_white)
            networkPref.setSummary(R.string.connectivity_summary_wifi_disabled)
        }
    }

    @VisibleForTesting
    fun updateSoundSettings() {
        val soundPref = findPreference<Preference>(KEY_SOUND)
        if (soundPref != null) {
            val soundIntent = Intent(ACTION_SOUND)
            val info = systemIntentIsHandled(
                context, soundIntent
            )
            soundPref.isVisible = info != null
            if (info?.activityInfo != null) {
                val pkgName = info.activityInfo.packageName
                val icon = getDrawableResource(pkgName, "sound_icon")
                if (icon != null) {
                    soundPref.icon = icon
                }
                val title = getStringResource(pkgName, "sound_pref_title")
                if (!TextUtils.isEmpty(title)) {
                    soundPref.title = title
                }
                val summary = getStringResource(pkgName, "sound_pref_summary")
                if (!TextUtils.isEmpty(summary)) {
                    soundPref.summary = summary
                }
            }
        }
    }

    /**
     * Extracts a string resource from a given package.
     *
     * @param pkgName  the package name
     * @param resource name, e.g. "my_string_name"
     */
    private fun getStringResource(pkgName: String, resourceName: String): String? {
        try {
            val targetContext = context!!.createPackageContext(pkgName, 0)
            val resId = targetContext.resources.getIdentifier(
                "$pkgName:string/$resourceName", null, null
            )
            if (resId != 0) {
                return targetContext.resources.getString(resId)
            }
        } catch (e: Resources.NotFoundException) {
            Log.w(
                TAG,
                "Unable to get string resource $resourceName", e
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(
                TAG,
                "Unable to get string resource $resourceName", e
            )
        } catch (e: SecurityException) {
            Log.w(
                TAG,
                "Unable to get string resource $resourceName", e
            )
        }
        return null
    }

    /**
     * Extracts a drawable resource from a given package.
     *
     * @param pkgName  the package name
     * @param resource name, e.g. "my_icon_name"
     */
    private fun getDrawableResource(pkgName: String, resourceName: String): Drawable? {
        try {
            val targetContext = context!!.createPackageContext(pkgName, 0)
            val resId = targetContext.resources.getIdentifier(
                "$pkgName:drawable/$resourceName", null, null
            )
            if (resId != 0) {
                return targetContext.resources.getDrawable(resId)
            }
        } catch (e: Resources.NotFoundException) {
            Log.w(
                TAG,
                "Unable to get drawable resource $resourceName", e
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(
                TAG,
                "Unable to get drawable resource $resourceName", e
            )
        } catch (e: SecurityException) {
            Log.w(
                TAG,
                "Unable to get drawable resource $resourceName", e
            )
        }
        return null
    }

    private val isRestricted: Boolean
        get() = SecurityFragment.isRestrictedProfileInEffect(context)

    @VisibleForTesting
    fun updateAccessoryPref() {
        val connectedDevicesSlicePreference =
            findPreference<Preference>(KEY_CONNECTED_DEVICES_SLICE) as SlicePreference?
        val accessoryPreference = findPreference<Preference>(KEY_ACCESSORIES)
        val connectedDevicesPreference = findPreference<Preference>(
            KEY_CONNECTED_DEVICES
        )
        if (connectedDevicesSlicePreference != null && FlavorUtils.isTwoPanel(
                context
            )
            && SliceUtils.isSliceProviderValid(
                context, connectedDevicesSlicePreference.uri
            )
        ) {
            connectedDevicesSlicePreference.isVisible = true
            connectedDevicesPreference!!.isVisible = false
            accessoryPreference!!.isVisible = false
            val pkgInfo = getProviderInfo(
                context!!,
                Uri.parse(connectedDevicesSlicePreference.uri).authority!!
            )
            if (pkgInfo != null) {
                updateConnectedDevicePref(pkgInfo.packageName, connectedDevicesSlicePreference)
            }
            return
        }

        if (connectedDevicesSlicePreference != null) {
            connectedDevicesSlicePreference.isVisible = false
        }

        if (connectedDevicesPreference != null) {
            val intent = Intent(ACTION_CONNECTED_DEVICES)
            val info = systemIntentIsHandled(
                context, intent
            )
            connectedDevicesPreference.isVisible = info != null
            accessoryPreference!!.isVisible = info == null
            if (info != null) {
                updateConnectedDevicePref(
                    info.activityInfo.packageName, connectedDevicesPreference
                )
                return
            }
        }
        if (mBtAdapter == null || accessoryPreference == null) {
            return
        }

        val bondedDevices = mBtAdapter!!.bondedDevices
        mHasBtAccessories = bondedDevices!!.size != 0
    }

    @VisibleForTesting
    fun updateAccountPref() {
        val accountsPref = findPreference<Preference>(KEY_ACCOUNTS_AND_SIGN_IN)
        val accountsSlicePref =
            findPreference<Preference>(KEY_ACCOUNTS_AND_SIGN_IN_SLICE) as SlicePreference?
        val accountsBasicMode = findPreference<Preference>(
            KEY_ACCOUNTS_AND_SIGN_IN_BASIC_MODE
        )
        val intent = Intent(ACTION_ACCOUNTS)

        when (AccountsUtil.getAccountsFragmentToLaunch(context)) {
            AccountsUtil.ACCOUNTS_FRAGMENT_RESTRICTED -> {
                // Use the bundled AccountsFragment if restriction active
                if (accountsBasicMode != null) {
                    accountsBasicMode.isVisible = false
                }
                if (accountsSlicePref != null) {
                    accountsSlicePref.isVisible = false
                }
                if (accountsPref != null) {
                    accountsPref.isVisible = true
                }
                return
            }

            AccountsUtil.ACCOUNTS_BASIC_MODE_FRAGMENT -> {
                if (accountsBasicMode != null) {
                    accountsBasicMode.isVisible = true
                }
                if (accountsPref != null) {
                    accountsPref.isVisible = false
                }
                if (accountsSlicePref != null) {
                    accountsSlicePref.isVisible = false
                }
                return
            }

            AccountsUtil.ACCOUNTS_SYSTEM_INTENT -> {
                if (accountsPref != null) {
                    accountsPref.isVisible = true
                    accountsPref.fragment = null
                    accountsPref.intent = intent
                }
                if (accountsSlicePref != null) {
                    accountsSlicePref.isVisible = false
                }
                if (accountsBasicMode != null) {
                    accountsBasicMode.isVisible = false
                }
                return
            }

            AccountsUtil.ACCOUNTS_SLICE_FRAGMENT -> {
                // If a slice is available, use it to display the accounts settings, otherwise
                // fall back to use AccountsFragment.
                if (accountsPref != null) {
                    accountsPref.isVisible = false
                }
                if (accountsSlicePref != null) {
                    accountsSlicePref.isVisible = true
                }
                if (accountsBasicMode != null) {
                    accountsBasicMode.isVisible = false
                }
                return
            }

            AccountsUtil.ACCOUNTS_FRAGMENT_DEFAULT -> {
                if (accountsPref != null) {
                    accountsPref.isVisible = true
                    updateAccountPrefInfo()
                }
                if (accountsSlicePref != null) {
                    accountsSlicePref.isVisible = false
                }
                if (accountsBasicMode != null) {
                    accountsBasicMode.isVisible = false
                }
            }

            else -> {
                if (accountsPref != null) {
                    accountsPref.isVisible = true
                    updateAccountPrefInfo()
                }
                if (accountsSlicePref != null) {
                    accountsSlicePref.isVisible = false
                }
                if (accountsBasicMode != null) {
                    accountsBasicMode.isVisible = false
                }
            }
        }
    }

    @VisibleForTesting
    fun updateAccountPrefInfo() {
        val accountsPref = findPreference<Preference>(KEY_ACCOUNTS_AND_SIGN_IN)
        if (accountsPref != null && accountsPref.isVisible) {
            val am = AccountManager.get(context)
            val accounts = am.accounts
            if (accounts.size == 0) {
                mHasAccounts = false
                accountsPref.setIcon(R.drawable.ic_add_an_account)
                accountsPref.setSummary(R.string.accounts_category_summary_no_account)
                AccountsFragment.setUpAddAccountPrefIntent(accountsPref, context)
            } else {
                mHasAccounts = true
                accountsPref.setIcon(R.drawable.ic_accounts_and_sign_in)
                if (accounts.size == 1) {
                    accountsPref.summary = accounts[0].name
                } else {
                    val msgFormat = MessageFormat(
                        context!!.resources.getString(
                            R.string.accounts_category_summary
                        ),
                        Locale.getDefault()
                    )
                    val arguments: MutableMap<String, Any> = HashMap()
                    arguments["count"] = accounts.size
                    accountsPref.summary = msgFormat.format(arguments)
                }
            }
        }
    }

    @VisibleForTesting
    fun updateBasicModeSuggestion() {
        val basicModeSuggestion = findPreference<PreferenceCategory>(
            KEY_BASIC_MODE_SUGGESTION
        )
            ?: return
        if (FlavorUtils.getFeatureFactory(context)
                .basicModeFeatureProvider.isBasicMode(context!!)
        ) {
            basicModeSuggestion.isVisible = true
        } else {
            basicModeSuggestion.isVisible = false
        }
    }

    override fun onStart() {
        super.onStart()
        updateAccountPref()
        updateAccessoryPref()
        val btChangeFilter = IntentFilter()
        btChangeFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        btChangeFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        btChangeFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        context!!.registerReceiver(mBCMReceiver, btChangeFilter)
        if (mConnectivityListenerLite != null) {
            mConnectivityListenerLite!!.setListener { activeNetworkProvider: ActiveNetworkProvider ->
                this.updateConnectivityType(
                    activeNetworkProvider
                )
            }
        }
        mConnectivityListenerOptional!!.ifPresent { connectivityListener: ConnectivityListener -> connectivityListener.setListener { this.updateConnectivity() } }
    }

    override fun onResume() {
        super.onResume()
        if (isWifiScanOptimisationEnabled) {
            mConnectivityListenerLite!!.handleConnectivityChange()
        } else {
            updateConnectivity()
        }
    }

    override fun onStop() {
        context!!.unregisterReceiver(mBCMReceiver)
        if (mConnectivityListenerLite != null) {
            mConnectivityListenerLite!!.setListener(null)
        }
        mConnectivityListenerOptional!!.ifPresent { connectivityListener: ConnectivityListener ->
            connectivityListener.setListener(
                null
            )
        }
        super.onStop()
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if ((preference.key == KEY_ACCOUNTS_AND_SIGN_IN && !mHasAccounts
                    && !AccountsUtil.isAdminRestricted(context))
            || (preference.key == KEY_ACCESSORIES && !mHasBtAccessories)
            || (preference.key == KEY_DISPLAY_AND_SOUND
                    && preference.intent != null)
            || (preference.key == KEY_CHANNELS_AND_INPUTS
                    && preference.intent != null)
        ) {
            context!!.startActivity(preference.intent)
            return true
        } else if (preference.key == KEY_BASIC_MODE_EXIT
            && FlavorUtils.getFeatureFactory(context)
                .basicModeFeatureProvider.isBasicMode(context!!)
        ) {
            if (activity != null) {
                FlavorUtils.getFeatureFactory(context)
                    .basicModeFeatureProvider.startBasicModeExitActivity(activity)
            }
            return true
        } else {
            return super.onPreferenceTreeClick(preference)
        }
    }

    override fun onDestroy() {
        mSuggestionQuickSettingPrefsContainer!!.onDestroy()
        super.onDestroy()
    }

    override fun onCreatePreferenceControllers(context: Context): List<AbstractPreferenceController> {
        return mSuggestionQuickSettingPrefsContainer!!.onCreatePreferenceControllers(context)
    }

    override fun onSuggestionClosed(preference: Preference) {
        mSuggestionQuickSettingPrefsContainer!!.onSuggestionClosed(preference)
    }

    override fun onSuggestionReady(data: List<Suggestion>) {
        mSuggestionQuickSettingPrefsContainer!!.onSuggestionReady(data)
    }

    override fun onHotwordStateChanged() {
        mSuggestionQuickSettingPrefsContainer!!.onHotwordStateChanged()
    }

    override fun onHotwordEnable() {
        mSuggestionQuickSettingPrefsContainer!!.onHotwordEnable()
    }

    override fun onHotwordDisable() {
        mSuggestionQuickSettingPrefsContainer!!.onHotwordDisable()
    }

    private fun supportBluetooth(): Boolean {
        return activity.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
    }

    private fun updateConnectedDevicePref(pkgName: String, pref: Preference) {
        val icon = getDrawableResource(pkgName, "connected_devices_pref_icon")
        if (icon != null) {
            pref.icon = icon
        }
        val title =
            if ((pref is SlicePreference))
                getStringResource(pkgName, "connected_devices_slice_pref_title")
            else
                getStringResource(pkgName, "connected_devices_pref_title")
        if (!TextUtils.isEmpty(title)) {
            pref.title = title
        }
        val summary = getStringResource(pkgName, "connected_devices_pref_summary")
        if (!TextUtils.isEmpty(summary)) {
            pref.summary = summary
        }
        pref.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { preference: Preference? ->
                InstrumentationUtils.logEntrySelected(
                    TvSettingsEnums.CONNECTED_CLASSIC
                )
                false
            }
    }

    override fun getPageId(): Int {
        return TvSettingsEnums.TV_SETTINGS_ROOT
    }

    override fun setSubtitle(subtitle: CharSequence?) {
    }

    override fun setIcon(icon: Drawable?) {
    }

    companion object {
        private const val TAG = "MainFragment"
        private const val KEY_BASIC_MODE_SUGGESTION = "basic_mode_suggestion"
        private const val KEY_BASIC_MODE_EXIT = "basic_mode_exit"

        @VisibleForTesting
        const val KEY_ACCOUNTS_AND_SIGN_IN: String = "accounts_and_sign_in"

        @VisibleForTesting
        const val KEY_ACCOUNTS_AND_SIGN_IN_SLICE: String = "accounts_and_sign_in_slice"

        @VisibleForTesting
        const val KEY_ACCOUNTS_AND_SIGN_IN_BASIC_MODE: String = "accounts_and_sign_in_basic_mode"
        private const val KEY_APPLICATIONS = "applications"

        @VisibleForTesting
        const val KEY_ACCESSORIES: String = "remotes_and_accessories"

        @VisibleForTesting
        const val KEY_CONNECTED_DEVICES: String = "connected_devices"
        private const val KEY_CONNECTED_DEVICES_SLICE = "connected_devices_slice"

        @VisibleForTesting
        const val KEY_NETWORK: String = "network"

        @VisibleForTesting
        const val KEY_SOUND: String = "sound"
        const val ACTION_SOUND: String = "com.android.tv.settings.SOUND"

        @VisibleForTesting
        const val ACTION_CONNECTED_DEVICES: String = "com.android.tv.settings.CONNECTED_DEVICES"

        @VisibleForTesting
        const val KEY_PRIVACY: String = "privacy"

        @VisibleForTesting
        const val KEY_DISPLAY_AND_SOUND: String = "display_and_sound"
        private const val KEY_DISPLAY_AND_SOUND_SLICE = "display_and_sound_slice"
        private const val KEY_CHANNELS_AND_INPUTS = "channels_and_inputs"
        private const val KEY_CHANNELS_AND_INPUTS_SLICE = "channels_and_inputs_slice"

        private const val KEY_HELP_AND_FEEDBACK = "help_and_feedback"
        private const val KEY_HELP_AND_FEEDBACK_SLICE = "help_and_feedback_slice"

        private const val ACTION_ACCOUNTS = "com.android.tv.settings.ACCOUNTS"

        fun newInstance(): MainFragment {
            return MainFragment()
        }

        /**
         * Returns the ResolveInfo for the system activity that matches given intent filter or null if
         * no such activity exists.
         *
         * @param context Context of the caller
         * @param intent  The intent matching the desired system app
         * @return ResolveInfo of the matching activity or null if no match exists
         */
        @JvmStatic
        fun systemIntentIsHandled(context: Context?, intent: Intent?): ResolveInfo? {
            if (intent == null || context == null) {
                return null
            }

            val pm = context.packageManager
            for (info in pm.queryIntentActivities(intent, 0)) {
                if (info.activityInfo != null
                    && ((info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM)
                            == ApplicationInfo.FLAG_SYSTEM)
                ) {
                    return info
                }
            }
            return null
        }

        private fun getProviderInfo(context: Context, authority: String): ProviderInfo? {
            return context.packageManager.resolveContentProvider(authority, 0)
        }
    }
}
