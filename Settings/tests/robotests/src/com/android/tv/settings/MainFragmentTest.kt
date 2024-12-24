/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tv.settings

import android.accounts.Account
import android.accounts.AccountManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.icu.text.MessageFormat
import android.telephony.SignalStrength
import androidx.preference.Preference
import com.android.tv.settings.connectivity.ConnectivityListener
import java.util.Locale
import java.util.Optional
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.Spy
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowAccountManager

@RunWith(RobolectricTestRunner::class)
class MainFragmentTest {
    @Spy
    private lateinit var mMainFragment: MainFragment

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        Mockito.doReturn(RuntimeEnvironment.application).`when`(mMainFragment).context
    }

    @Test
    fun testUpdateConnectivity_NoNetwork() {
        val networkPref = Mockito.mock(
            Preference::class.java
        )
        Mockito.doReturn(networkPref)
            .`when`(mMainFragment)
            .findPreference<Preference>(MainFragment.KEY_NETWORK)
        val listener = Mockito.mock(
            ConnectivityListener::class.java
        )
        mMainFragment!!.mConnectivityListenerOptional = Optional.of(listener)

        Mockito.doReturn(false).`when`(listener).isEthernetAvailable
        Mockito.doReturn(false).`when`(listener).isCellConnected
        Mockito.doReturn(false).`when`(listener).isEthernetConnected
        Mockito.doReturn(false).`when`(listener).isWifiEnabledOrEnabling

        mMainFragment.updateConnectivity()

        Mockito.verify(networkPref, Mockito.atLeastOnce())
            .setIcon(R.drawable.ic_wifi_signal_off_white)
    }

    @Test
    fun testUpdateConnectivity_hasEthernet() {
        val networkPref = Mockito.mock(
            Preference::class.java
        )
        Mockito.doReturn(networkPref)
            .`when`(mMainFragment)
            .findPreference<Preference>(MainFragment.KEY_NETWORK)
        val listener = Mockito.mock(
            ConnectivityListener::class.java
        )
        mMainFragment!!.mConnectivityListenerOptional = Optional.of(listener)

        Mockito.doReturn(true).`when`(listener).isEthernetAvailable
        Mockito.doReturn(false).`when`(listener).isCellConnected
        Mockito.doReturn(false).`when`(listener).isEthernetConnected
        Mockito.doReturn(false).`when`(listener).isWifiEnabledOrEnabling

        mMainFragment.updateConnectivity()

        Mockito.verify(networkPref, Mockito.atLeastOnce())
            .setIcon(R.drawable.ic_wifi_signal_off_white)
    }

    @Test
    fun testUpdateConnectivity_hasEthernetConnected() {
        val networkPref = Mockito.mock(
            Preference::class.java
        )
        Mockito.doReturn(networkPref)
            .`when`(mMainFragment)
            .findPreference<Preference>(MainFragment.KEY_NETWORK)
        val listener = Mockito.mock(
            ConnectivityListener::class.java
        )
        mMainFragment!!.mConnectivityListenerOptional = Optional.of(listener)

        Mockito.doReturn(true).`when`(listener).isEthernetAvailable
        Mockito.doReturn(false).`when`(listener).isCellConnected
        Mockito.doReturn(true).`when`(listener).isEthernetConnected
        Mockito.doReturn(false).`when`(listener).isWifiEnabledOrEnabling

        mMainFragment.updateConnectivity()

        Mockito.verify(networkPref, Mockito.atLeastOnce()).setIcon(R.drawable.ic_ethernet_white)
    }

    @Test
    fun testUpdateConnectivity_wifiSignal() {
        val networkPref = Mockito.mock(
            Preference::class.java
        )
        Mockito.doReturn(networkPref)
            .`when`(mMainFragment)
            .findPreference<Preference>(MainFragment.KEY_NETWORK)
        val listener = Mockito.mock(
            ConnectivityListener::class.java
        )
        mMainFragment!!.mConnectivityListenerOptional = Optional.of(listener)

        Mockito.doReturn(false).`when`(listener).isEthernetAvailable
        Mockito.doReturn(false).`when`(listener).isCellConnected
        Mockito.doReturn(false).`when`(listener).isEthernetConnected
        Mockito.doReturn(true).`when`(listener).isWifiEnabledOrEnabling
        Mockito.doReturn(true).`when`(listener).isWifiConnected
        Mockito.doReturn(0).`when`(listener).getWifiSignalStrength(ArgumentMatchers.anyInt())

        mMainFragment.updateConnectivity()

        Mockito.verify(networkPref, Mockito.atLeastOnce())
            .setIcon(R.drawable.ic_wifi_signal_0_white)

        Mockito.doReturn(1).`when`(listener).getWifiSignalStrength(ArgumentMatchers.anyInt())

        mMainFragment.updateConnectivity()

        Mockito.verify(networkPref, Mockito.atLeastOnce())
            .setIcon(R.drawable.ic_wifi_signal_1_white)

        Mockito.doReturn(2).`when`(listener).getWifiSignalStrength(ArgumentMatchers.anyInt())

        mMainFragment.updateConnectivity()

        Mockito.verify(networkPref, Mockito.atLeastOnce())
            .setIcon(R.drawable.ic_wifi_signal_2_white)

        Mockito.doReturn(3).`when`(listener).getWifiSignalStrength(ArgumentMatchers.anyInt())

        mMainFragment.updateConnectivity()

        Mockito.verify(networkPref, Mockito.atLeastOnce())
            .setIcon(R.drawable.ic_wifi_signal_3_white)

        Mockito.doReturn(4).`when`(listener).getWifiSignalStrength(ArgumentMatchers.anyInt())

        mMainFragment.updateConnectivity()

        Mockito.verify(networkPref, Mockito.atLeastOnce())
            .setIcon(R.drawable.ic_wifi_signal_4_white)

        Mockito.doReturn(false).`when`(listener).isWifiConnected

        mMainFragment.updateConnectivity()

        Mockito.verify(networkPref, Mockito.atLeastOnce()).setIcon(R.drawable.ic_wifi_not_connected)
    }

    @Test
    fun testUpdateConnectivity_notConnected() {
        val networkPref = Mockito.mock(
            Preference::class.java
        )
        Mockito.doReturn(networkPref)
            .`when`(mMainFragment)
            .findPreference<Preference>(MainFragment.KEY_NETWORK)
        val listener = Mockito.mock(
            ConnectivityListener::class.java
        )
        mMainFragment!!.mConnectivityListenerOptional = Optional.of(listener)

        Mockito.doReturn(false).`when`(listener).isEthernetAvailable
        Mockito.doReturn(false).`when`(listener).isCellConnected
        Mockito.doReturn(false).`when`(listener).isEthernetConnected
        Mockito.doReturn(false).`when`(listener).isWifiEnabledOrEnabling

        mMainFragment.updateConnectivity()

        Mockito.verify(networkPref, Mockito.atLeastOnce())
            .setIcon(R.drawable.ic_wifi_signal_off_white)
    }

    @Test
    fun testUpdateConnectivity_cellSignal() {
        val networkPref = Mockito.mock(
            Preference::class.java
        )
        Mockito.doReturn(networkPref)
            .`when`(mMainFragment)
            .findPreference<Preference>(MainFragment.KEY_NETWORK)
        val listener = Mockito.mock(
            ConnectivityListener::class.java
        )
        mMainFragment!!.mConnectivityListenerOptional = Optional.of(listener)

        Mockito.doReturn(false).`when`(listener).isEthernetAvailable
        Mockito.doReturn(true).`when`(listener).isCellConnected
        Mockito.doReturn(false).`when`(listener).isEthernetConnected
        Mockito.doReturn(false).`when`(listener).isWifiEnabledOrEnabling
        Mockito.doReturn(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN)
            .`when`<ConnectivityListener>(listener).cellSignalStrength

        mMainFragment.updateConnectivity()

        Mockito.verify(networkPref, Mockito.atLeastOnce())
            .setIcon(R.drawable.ic_cell_signal_0_white)

        Mockito.doReturn(SignalStrength.SIGNAL_STRENGTH_POOR)
            .`when`<ConnectivityListener>(listener).cellSignalStrength

        mMainFragment.updateConnectivity()

        Mockito.verify(networkPref, Mockito.atLeastOnce())
            .setIcon(R.drawable.ic_cell_signal_1_white)

        Mockito.doReturn(SignalStrength.SIGNAL_STRENGTH_MODERATE)
            .`when`<ConnectivityListener>(listener).cellSignalStrength

        mMainFragment.updateConnectivity()

        Mockito.verify(networkPref, Mockito.atLeastOnce())
            .setIcon(R.drawable.ic_cell_signal_2_white)

        Mockito.doReturn(SignalStrength.SIGNAL_STRENGTH_GOOD)
            .`when`<ConnectivityListener>(listener).cellSignalStrength

        mMainFragment.updateConnectivity()

        Mockito.verify(networkPref, Mockito.atLeastOnce())
            .setIcon(R.drawable.ic_cell_signal_3_white)

        Mockito.doReturn(SignalStrength.SIGNAL_STRENGTH_GREAT)
            .`when`<ConnectivityListener>(listener).cellSignalStrength

        mMainFragment.updateConnectivity()

        Mockito.verify(networkPref, Mockito.atLeastOnce())
            .setIcon(R.drawable.ic_cell_signal_4_white)
    }

    @Test
    fun testUpdateAccountPrefInfo_hasOneAccount() {
        val accountsPref = Mockito.mock(
            Preference::class.java
        )
        Mockito.doReturn(accountsPref).`when`(mMainFragment)
            .findPreference<Preference>(MainFragment.KEY_ACCOUNTS_AND_SIGN_IN)
        Mockito.doReturn(true).`when`(accountsPref).isVisible
        val am =
            Shadow.extract<ShadowAccountManager>(AccountManager.get(RuntimeEnvironment.application))
        am.addAccount(Account("test", "test"))

        mMainFragment!!.updateAccountPrefInfo()

        Mockito.verify(accountsPref, Mockito.atLeastOnce())
            .setIcon(R.drawable.ic_accounts_and_sign_in)
        Mockito.verify(accountsPref, Mockito.atLeastOnce()).summary =
            "test"
        Assert.assertTrue(mMainFragment.mHasAccounts)
    }

    @Test
    fun testUpdateAccountPrefInfo_hasNoAccount() {
        val accountsPref = Mockito.mock(
            Preference::class.java
        )
        Mockito.doReturn(accountsPref).`when`(mMainFragment)
            .findPreference<Preference>(MainFragment.KEY_ACCOUNTS_AND_SIGN_IN)
        Mockito.doReturn(true).`when`(accountsPref).isVisible

        mMainFragment!!.updateAccountPrefInfo()

        Mockito.verify(accountsPref, Mockito.atLeastOnce()).setIcon(R.drawable.ic_add_an_account)
        Mockito.verify(accountsPref, Mockito.atLeastOnce())
            .setSummary(R.string.accounts_category_summary_no_account)
        Assert.assertFalse(mMainFragment.mHasAccounts)
    }

    @Test
    fun testUpdateAccountPrefInfo_hasMoreThanOneAccount() {
        val accountsPref = Mockito.mock(
            Preference::class.java
        )
        Mockito.doReturn(RuntimeEnvironment.application.resources).`when`(mMainFragment).resources
        Mockito.doReturn(accountsPref).`when`(mMainFragment)
            .findPreference<Preference>(MainFragment.KEY_ACCOUNTS_AND_SIGN_IN)
        Mockito.doReturn(true).`when`(accountsPref).isVisible
        val am =
            Shadow.extract<ShadowAccountManager>(AccountManager.get(RuntimeEnvironment.application))
        am.addAccount(Account("test", "test"))
        am.addAccount(Account("test2", "test2"))

        mMainFragment!!.updateAccountPrefInfo()

        Mockito.verify(accountsPref, Mockito.atLeastOnce())
            .setIcon(R.drawable.ic_accounts_and_sign_in)
        val msgFormat = MessageFormat(
            RuntimeEnvironment.application.resources.getString(
                R.string.accounts_category_summary
            ),
            Locale.getDefault()
        )
        val arguments: MutableMap<String, Any> = HashMap()
        arguments["count"] = 2
        val summary = msgFormat.format(arguments)
        Mockito.verify(accountsPref, Mockito.atLeastOnce()).summary =
            summary
        Assert.assertTrue(mMainFragment.mHasAccounts)
    }

    @Test
    fun testUpdateAccessoryPref_hasNoAccessory() {
        val accessoryPref = Mockito.mock(
            Preference::class.java
        )
        Mockito.doReturn(accessoryPref).`when`(mMainFragment)
            .findPreference<Preference>(MainFragment.KEY_ACCESSORIES)
        mMainFragment!!.mBtAdapter = Mockito.mock(
            BluetoothAdapter::class.java
        )
        val set: Set<BluetoothDevice> = HashSet()
        Mockito.doReturn(set).`when`(mMainFragment.mBtAdapter)!!.bondedDevices

        mMainFragment.updateAccessoryPref()

        Assert.assertFalse(mMainFragment.mHasBtAccessories)
    }

    @Test
    fun testUpdateAccessoryPref_hasAccessories() {
        val accessoryPref = Mockito.mock(
            Preference::class.java
        )
        Mockito.doReturn(accessoryPref).`when`(mMainFragment)
            .findPreference<Preference>(MainFragment.KEY_ACCESSORIES)
        mMainFragment!!.mBtAdapter = Mockito.mock(
            BluetoothAdapter::class.java
        )
        val set: MutableSet<BluetoothDevice> = HashSet()
        val device = Mockito.mock(
            BluetoothDevice::class.java
        )
        Mockito.doReturn("testDevice").`when`(device).alias
        set.add(device)
        Mockito.doReturn(set).`when`(mMainFragment.mBtAdapter)!!.bondedDevices

        mMainFragment.updateAccessoryPref()

        Assert.assertTrue(mMainFragment.mHasBtAccessories)
    }

    @Test
    fun updateConnectivity_givenConnectivityListenerNull_thenNetworkPreferenceIsNotUpdated() {
        /*
        Currently this is only a unit test that verifies that MainFragment
        does not try to update network preference when ConnectivityListener
        is null.
        TODO:
        Add test to verify that ConnectivityListener is null when wifi scan
        optimisation is enabled.
         */
        val networkPreference = Mockito.mock(
            Preference::class.java
        )
        Mockito.doReturn(networkPreference)
            .`when`(mMainFragment)
            .findPreference<Preference>(MainFragment.KEY_NETWORK)
        mMainFragment!!.mConnectivityListenerOptional = Optional.empty()

        mMainFragment.updateConnectivity()

        Mockito.verifyNoInteractions(networkPreference)
    }
}