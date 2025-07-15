/*
 * Copyright (C) 2014 The Android Open Source Project
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

import static android.os.UserManager.DISALLOW_ADD_WIFI_CONFIG;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProviders;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.tv.settings.R;
import com.android.tv.settings.connectivity.setup.AdvancedWifiOptionsFlow;
import com.android.tv.settings.connectivity.setup.ChooseSecurityState;
import com.android.tv.settings.connectivity.setup.ConnectFailedState;
import com.android.tv.settings.connectivity.setup.ConnectState;
import com.android.tv.settings.connectivity.setup.EasyConnectQRState;
import com.android.tv.settings.connectivity.setup.EnterPasswordState;
import com.android.tv.settings.connectivity.setup.EnterSsidState;
import com.android.tv.settings.connectivity.setup.OptionsOrConnectState;
import com.android.tv.settings.connectivity.setup.SuccessState;
import com.android.tv.settings.connectivity.setup.UserChoiceInfo;
import com.android.tv.settings.connectivity.util.State;
import com.android.tv.settings.connectivity.util.StateMachine;
import com.android.tv.settings.connectivity.util.StateMachineActivity;
import com.android.tv.settings.core.instrumentation.InstrumentedActivity;

/**
 * Manual-style add wifi network (the kind you'd use for adding a hidden or out-of-range network.)
 */
public class AddWifiNetworkActivity extends StateMachineActivity {
    private static final String TAG = "AddWifiNetworkActivity";

    private static final String EXTRA_TYPE = "com.android.tv.settings.connectivity.type";
    private static final String EXTRA_TYPE_EASYCONNECT = "easyconnect";

    /**
     * Create an intent to launch this activity in EasyConnect mode.
     */
    public static Intent createEasyConnectIntent(Context context) {
        return new Intent(context, AddWifiNetworkActivity.class)
                .putExtra(EXTRA_TYPE, EXTRA_TYPE_EASYCONNECT);
    }

    private State mChooseSecurityState;
    private State mConnectFailedState;
    private State mConnectState;
    private State mEnterPasswordState;
    private State mEnterSsidState;
    private State mEasyConnectQrState;
    private State mSuccessState;
    private State mOptionsOrConnectState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isAddWifiAllowed()) {
            EnforcedAdmin admin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(this,
                    UserManager.DISALLOW_CONFIG_WIFI, UserHandle.myUserId());
            if (admin != null) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(this, admin);
            }
            finish();
            return;
        }

        UserChoiceInfo userChoiceInfo = ViewModelProviders.of(this).get(UserChoiceInfo.class);
        userChoiceInfo.getWifiConfiguration().hiddenSSID = true;

        boolean isEasyConnectFlow = EXTRA_TYPE_EASYCONNECT.equals(
                getIntent().getStringExtra(EXTRA_TYPE));

        mEnterSsidState = new EnterSsidState(this);
        mEasyConnectQrState = new EasyConnectQRState(this);
        mChooseSecurityState = new ChooseSecurityState(this);
        mEnterPasswordState = new EnterPasswordState(this);
        mConnectState = new ConnectState(this);
        mConnectFailedState = new ConnectFailedState(this);
        mSuccessState = new SuccessState(this);
        mOptionsOrConnectState = new OptionsOrConnectState(this);
        AdvancedWifiOptionsFlow.createFlow(
                this, true, true, null, mOptionsOrConnectState,
                mConnectState, AdvancedWifiOptionsFlow.START_DEFAULT_PAGE);

        /* Enter SSID */
        mStateMachine.addState(
                mEnterSsidState,
                StateMachine.CONTINUE,
                mChooseSecurityState
        );

        /* Choose security */
        mStateMachine.addState(
                mChooseSecurityState,
                StateMachine.OPTIONS_OR_CONNECT,
                mOptionsOrConnectState
        );
        mStateMachine.addState(
                mChooseSecurityState,
                StateMachine.CONTINUE,
                mEnterPasswordState
        );

        /* Enter Password */
        mStateMachine.addState(
                mEnterPasswordState,
                StateMachine.OPTIONS_OR_CONNECT,
                mOptionsOrConnectState
        );

        /* EasyConnect QR code */
        mStateMachine.addState(
                mEasyConnectQrState,
                StateMachine.CONNECT,
                mConnectState
        );
        mStateMachine.addState(
                mEasyConnectQrState,
                StateMachine.RESULT_FAILURE,
                mConnectFailedState
        );

        /* Options or Connect */
        mStateMachine.addState(
                mOptionsOrConnectState,
                StateMachine.CONNECT,
                mConnectState
        );
        mStateMachine.addState(
                mOptionsOrConnectState,
                StateMachine.RESTART,
                mEnterSsidState
        );

        /* Connect */
        mStateMachine.addState(
                mConnectState,
                StateMachine.RESULT_FAILURE,
                mConnectFailedState
        );
        mStateMachine.addState(
                mConnectState,
                StateMachine.RESULT_SUCCESS,
                mSuccessState
        );

        /* Connect Failed */
        mStateMachine.addState(
                mConnectFailedState,
                StateMachine.TRY_AGAIN,
                isEasyConnectFlow ? mEasyConnectQrState : mOptionsOrConnectState
        );

        mStateMachine.addState(
                mConnectFailedState,
                StateMachine.SELECT_WIFI,
                getFinishState()
        );

        if (isEasyConnectFlow) {
            mStateMachine.setStartState(mEasyConnectQrState);
        } else {
            mStateMachine.setStartState(mEnterSsidState);
        }

        mStateMachine.start(true);
    }

    private boolean isAddWifiAllowed() {
        final UserManager userManager = UserManager.get(this);
        if(userManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_WIFI)) {
            Log.e(TAG, "The user is not allowed to configure Wi-Fi.");
            return false;
        }
        if (userManager.hasUserRestriction(DISALLOW_ADD_WIFI_CONFIG)) {
            Log.e(TAG, "The user is not allowed to add Wi-Fi configuration.");
            return false;
        }
        return true;
    }
}
