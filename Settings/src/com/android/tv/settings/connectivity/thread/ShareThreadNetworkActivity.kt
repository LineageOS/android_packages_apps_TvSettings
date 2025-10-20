/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.tv.settings.connectivity.thread

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.android.tv.settings.R
import com.android.tv.settings.connectivity.util.StateMachine
import com.android.tv.settings.connectivity.util.StateMachineActivity

class ShareThreadNetworkActivity : StateMachineActivity() {
    private val reauthLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startStateMachine()
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reauthAction = getString(R.string.account_reauth_action)
        val reauthPackage = getString(R.string.account_reauth_package)

        if (reauthAction.isNotEmpty() && reauthPackage.isNotEmpty()) {
            val intent = Intent(reauthAction)
            intent.setPackage(reauthPackage)
            reauthLauncher.launch(intent)
        } else {
            startStateMachine()
        }
    }

    private fun startStateMachine() {
        val shareQRCodeState = ShareQRCodeState(this)
        val shareSuccessState = ShareSuccessState(this)
        val shareFailedState = ShareFailedState(this)
        val timeoutState = TimeoutState(this)

        mStateMachine.addState(
            shareQRCodeState,
            StateMachine.RESULT_SUCCESS,
            shareSuccessState
        )

        mStateMachine.addState(
            shareQRCodeState,
            StateMachine.RESULT_FAILURE,
            shareFailedState
        )

        mStateMachine.addState(
            shareQRCodeState,
            StateMachine.RESULT_TIMEOUT,
            timeoutState
        )

        mStateMachine.addState(
            shareFailedState,
            StateMachine.TRY_AGAIN,
            shareQRCodeState
        )

        mStateMachine.addState(
            shareSuccessState,
            StateMachine.TRY_AGAIN,
            shareQRCodeState
        )

        mStateMachine.addState(
            timeoutState,
            StateMachine.TRY_AGAIN,
            shareQRCodeState
        )

        mStateMachine.setStartState(shareQRCodeState)
        mStateMachine.start(true)
    }
}