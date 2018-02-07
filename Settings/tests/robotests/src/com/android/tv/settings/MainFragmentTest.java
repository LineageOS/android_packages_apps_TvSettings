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

package com.android.tv.settings;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.support.v7.preference.Preference;
import android.telephony.SignalStrength;

import com.android.tv.settings.connectivity.ConnectivityListener;
import com.android.tv.settings.testutils.ShadowUserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(TvSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class})
public class MainFragmentTest {

    @Spy
    private MainFragment mMainFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(RuntimeEnvironment.application).when(mMainFragment).getContext();
    }

    @Test
    public void testUpdateWifi_NoNetwork() {
        final Preference networkPref = mock(Preference.class);
        doReturn(networkPref).when(mMainFragment).findPreference(MainFragment.KEY_NETWORK);
        final ConnectivityListener listener = mock(ConnectivityListener.class);
        mMainFragment.mConnectivityListener = listener;

        doReturn(false).when(listener).isEthernetAvailable();
        doReturn(false).when(listener).isCellConnected();
        doReturn(false).when(listener).isEthernetConnected();
        doReturn(false).when(listener).isWifiEnabledOrEnabling();

        mMainFragment.updateWifi();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_wifi_signal_off_white);
    }

    @Test
    public void testUpdateWifi_hasEthernet() {
        final Preference networkPref = mock(Preference.class);
        doReturn(networkPref).when(mMainFragment).findPreference(MainFragment.KEY_NETWORK);
        final ConnectivityListener listener = mock(ConnectivityListener.class);
        mMainFragment.mConnectivityListener = listener;

        doReturn(true).when(listener).isEthernetAvailable();
        doReturn(false).when(listener).isCellConnected();
        doReturn(false).when(listener).isEthernetConnected();
        doReturn(false).when(listener).isWifiEnabledOrEnabling();

        mMainFragment.updateWifi();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_wifi_signal_off_white);
    }

    @Test
    public void testUpdateWifi_hasEthernetConnected() {
        final Preference networkPref = mock(Preference.class);
        doReturn(networkPref).when(mMainFragment).findPreference(MainFragment.KEY_NETWORK);
        final ConnectivityListener listener = mock(ConnectivityListener.class);
        mMainFragment.mConnectivityListener = listener;

        doReturn(true).when(listener).isEthernetAvailable();
        doReturn(false).when(listener).isCellConnected();
        doReturn(true).when(listener).isEthernetConnected();
        doReturn(false).when(listener).isWifiEnabledOrEnabling();

        mMainFragment.updateWifi();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_ethernet_white);
    }

    @Test
    public void testUpdateWifi_wifiSignal() {
        final Preference networkPref = mock(Preference.class);
        doReturn(networkPref).when(mMainFragment).findPreference(MainFragment.KEY_NETWORK);
        final ConnectivityListener listener = mock(ConnectivityListener.class);
        mMainFragment.mConnectivityListener = listener;

        doReturn(false).when(listener).isEthernetAvailable();
        doReturn(false).when(listener).isCellConnected();
        doReturn(false).when(listener).isEthernetConnected();
        doReturn(true).when(listener).isWifiEnabledOrEnabling();
        doReturn(0).when(listener).getWifiSignalStrength(anyInt());

        mMainFragment.updateWifi();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_wifi_signal_0_white);

        doReturn(1).when(listener).getWifiSignalStrength(anyInt());

        mMainFragment.updateWifi();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_wifi_signal_1_white);

        doReturn(2).when(listener).getWifiSignalStrength(anyInt());

        mMainFragment.updateWifi();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_wifi_signal_2_white);

        doReturn(3).when(listener).getWifiSignalStrength(anyInt());

        mMainFragment.updateWifi();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_wifi_signal_3_white);

        doReturn(4).when(listener).getWifiSignalStrength(anyInt());

        mMainFragment.updateWifi();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_wifi_signal_4_white);
    }

    @Test
    public void testUpdateWifi_cellSignal() {
        final Preference networkPref = mock(Preference.class);
        doReturn(networkPref).when(mMainFragment).findPreference(MainFragment.KEY_NETWORK);
        final ConnectivityListener listener = mock(ConnectivityListener.class);
        mMainFragment.mConnectivityListener = listener;

        doReturn(false).when(listener).isEthernetAvailable();
        doReturn(true).when(listener).isCellConnected();
        doReturn(false).when(listener).isEthernetConnected();
        doReturn(false).when(listener).isWifiEnabledOrEnabling();
        doReturn(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN)
                .when(listener).getCellSignalStrength();

        mMainFragment.updateWifi();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_cell_signal_0_white);

        doReturn(SignalStrength.SIGNAL_STRENGTH_POOR)
                .when(listener).getCellSignalStrength();

        mMainFragment.updateWifi();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_cell_signal_1_white);

        doReturn(SignalStrength.SIGNAL_STRENGTH_MODERATE)
                .when(listener).getCellSignalStrength();

        mMainFragment.updateWifi();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_cell_signal_2_white);

        doReturn(SignalStrength.SIGNAL_STRENGTH_GOOD)
                .when(listener).getCellSignalStrength();

        mMainFragment.updateWifi();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_cell_signal_3_white);

        doReturn(SignalStrength.SIGNAL_STRENGTH_GREAT)
                .when(listener).getCellSignalStrength();

        mMainFragment.updateWifi();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_cell_signal_4_white);
    }
}
