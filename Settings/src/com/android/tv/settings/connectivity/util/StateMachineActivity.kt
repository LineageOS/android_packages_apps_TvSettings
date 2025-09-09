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
package com.android.tv.settings.connectivity.util

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.android.tv.settings.R
import com.android.tv.settings.connectivity.AddWifiNetworkActivity
import com.android.tv.settings.connectivity.FinishState
import com.android.tv.settings.connectivity.util.State.FragmentChangeListener
import com.android.tv.settings.core.instrumentation.InstrumentedActivity

open class StateMachineActivity :  InstrumentedActivity(), FragmentChangeListener {
    protected lateinit var mStateMachine: StateMachine
    protected val finishState = FinishState(this)
    private val mStateMachineCallback =
        StateMachine.Callback { result ->
            setResult(result)
            finish()
        }

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wifi_container)
        mStateMachine = ViewModelProvider(this)[StateMachine::class.java]
        mStateMachine.setCallback(mStateMachineCallback)
    }

    override fun onBackPressed() {
        mStateMachine.back()
    }

    override fun onFragmentChange(newFragment: Fragment?, movingForward: Boolean) {
        newFragment ?: return
        val updateTransaction = supportFragmentManager.beginTransaction()
        if (movingForward) {
            updateTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        } else {
            updateTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
        }
        updateTransaction.replace(R.id.wifi_container, newFragment, TAG)
        updateTransaction.commitAllowingStateLoss()
    }

    companion object {
        private const val TAG = "StateMachineActivity"
    }
}