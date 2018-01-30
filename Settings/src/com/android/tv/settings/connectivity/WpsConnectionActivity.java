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

package com.android.tv.settings.connectivity;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.android.internal.logging.nano.MetricsProto;
import com.android.tv.settings.R;
import com.android.tv.settings.connectivity.setup.SuccessState;
import com.android.tv.settings.connectivity.util.State;
import com.android.tv.settings.connectivity.util.StateMachine;
import com.android.tv.settings.core.instrumentation.InstrumentedActivity;
import com.android.tv.settings.util.ThemeHelper;

/**
 * Activity responsible for setting up Wi-Fi using WPS methods.
 */
public class WpsConnectionActivity extends InstrumentedActivity
        implements State.FragmentChangeListener {
    private static final String WPS_FRAGMENT_TAG = "wps_fragment_tag";

    private StateMachine mStateMachine;
    private State mWpsErrorState;
    private State mWpsEnterPinState;
    private State mWpsScanningState;
    private State mWpsSuccessState;
    private State mWpsStartState;
    private WpsFlowInfo mWpsFlowInfo;

    private final StateMachine.Callback mStateMachineCallback = new StateMachine.Callback() {
        @Override
        public void onFinish(int result) {
            setResult(result);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeHelper.getThemeResource(getIntent()));
        setContentView(R.layout.wifi_container);
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(
                Context.WIFI_SERVICE);
        mStateMachine = ViewModelProviders.of(this).get(StateMachine.class);
        mStateMachine.setCallback(mStateMachineCallback);
        mWpsFlowInfo = ViewModelProviders.of(this).get(WpsFlowInfo.class);
        mWpsFlowInfo.setWifiManager(wifiManager);
        overridePendingTransition(R.anim.wps_activity_open_in, R.anim.wps_activity_open_out);

        if (ThemeHelper.fromSetupWizard(getIntent())) {
            setTitle(getResources().getString(R.string.wifi_wps_title));
        }

        mWpsStartState = new WpsStartState(this);
        mWpsScanningState = new WpsScanningState(this);
        mWpsEnterPinState = new WpsEnterPinState(this);
        mWpsErrorState = new WpsErrorState(this);
        mWpsSuccessState = new SuccessState(this);

        /** Wps Start  **/
        mStateMachine.addState(
                mWpsStartState,
                StateMachine.WPS_ENTER_PIN,
                mWpsEnterPinState);
        mStateMachine.addState(
                mWpsStartState,
                StateMachine.WPS_SCANNING,
                mWpsScanningState
        );

        /** Wps Scanning **/
        mStateMachine.addState(
                mWpsScanningState,
                StateMachine.WPS_SUCCESS,
                mWpsSuccessState
        );
        mStateMachine.addState(
                mWpsScanningState,
                StateMachine.WPS_ERROR,
                mWpsErrorState
        );
        mStateMachine.addState(
                mWpsScanningState,
                StateMachine.WPS_SCANNING,
                mWpsScanningState
        );

        /** Wps Enter Pin **/
        mStateMachine.addState(
                mWpsEnterPinState,
                StateMachine.WPS_ERROR,
                mWpsErrorState);
        mStateMachine.addState(
                mWpsEnterPinState,
                StateMachine.WPS_SUCCESS,
                mWpsSuccessState
        );
        mStateMachine.addState(
                mWpsEnterPinState,
                StateMachine.WPS_ENTER_PIN,
                mWpsEnterPinState
        );

        /** Wps Error **/
        mStateMachine.addState(
                mWpsErrorState,
                StateMachine.WPS_START,
                mWpsStartState
        );

        mWpsFlowInfo.setWpsCallback(new WifiManager.WpsCallback() {
            @Override
            public void onStarted(String pin) {
                if (pin != null && mWpsFlowInfo.isActive()) {
                    mWpsFlowInfo.setPin(pin);
                    mStateMachine.getListener().onComplete(StateMachine.WPS_ENTER_PIN);
                } else {
                    mStateMachine.getListener().onComplete(StateMachine.WPS_SCANNING);
                }
            }

            @Override
            public void onSucceeded() {
                mWpsFlowInfo.setWpsComplete(true);
                if (!mWpsFlowInfo.isActive()) {
                    return;
                }
                mStateMachine.getListener().onComplete(StateMachine.WPS_SUCCESS);
            }

            @Override
            public void onFailed(int reason) {
                mWpsFlowInfo.setWpsComplete(true);
                if (!mWpsFlowInfo.isActive()) {
                    return;
                }

                String errorMessage;
                switch (reason) {
                    case WifiManager.WPS_OVERLAP_ERROR:
                        errorMessage = getString(R.string.wifi_wps_failed_overlap);
                        break;
                    case WifiManager.WPS_WEP_PROHIBITED:
                        errorMessage = getString(R.string.wifi_wps_failed_wep);
                        break;
                    case WifiManager.WPS_TKIP_ONLY_PROHIBITED:
                        errorMessage = getString(R.string.wifi_wps_failed_tkip);
                        break;
                    case WifiManager.IN_PROGRESS:
                        mWpsFlowInfo.getWifiManager().cancelWps(null);
                        if (mStateMachine.getCurrentState() == mWpsScanningState) {
                            mStateMachine.getListener().onComplete(StateMachine.WPS_SCANNING);
                        } else  if (mStateMachine.getCurrentState() == mWpsEnterPinState) {
                            mStateMachine.getListener().onComplete(StateMachine.WPS_ENTER_PIN);
                        }
                        return;
                    case WifiManager.WPS_TIMED_OUT:
                        if (mStateMachine.getCurrentState() == mWpsScanningState) {
                            mStateMachine.getListener().onComplete(StateMachine.WPS_SCANNING);
                        } else  if (mStateMachine.getCurrentState() == mWpsEnterPinState) {
                            mStateMachine.getListener().onComplete(StateMachine.WPS_ENTER_PIN);
                        }
                        return;
                    default:
                        errorMessage = getString(R.string.wifi_wps_failed_generic);
                        break;
                }
                mWpsFlowInfo.setErrorMessage(errorMessage);
                mStateMachine.getListener().onComplete(StateMachine.WPS_ERROR);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Must be set before all other actions.
        mWpsFlowInfo.setActive(true);
        mStateMachine.reset();
        mStateMachine.setStartState(mWpsStartState);
        mStateMachine.start(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mWpsFlowInfo.setActive(false);

        if (!mWpsFlowInfo.isWpsComplete()) {
            mWpsFlowInfo.getWifiManager().cancelWps(null);
        }
    }

    @Override
    public void onBackPressed() {
        mStateMachine.back();
    }

    @Override
    public void onFragmentChange(Fragment fragment, boolean forward) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (forward) {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        }
        transaction.replace(R.id.wifi_container, fragment, WPS_FRAGMENT_TAG).commit();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DIALOG_WPS_SETUP;
    }
}