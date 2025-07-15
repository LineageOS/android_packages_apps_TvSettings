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

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.ViewModelProvider
import com.android.tv.settings.R
import com.android.tv.settings.connectivity.setup.WifiConnectivityGuidedStepFragment
import com.android.tv.settings.connectivity.util.State
import com.android.tv.settings.connectivity.util.StateMachine

/** State for displaying that sharing the Thread network succeeded. */
class ShareSuccessState(private val activity: FragmentActivity) : State {
    private var fragment: Fragment? = null

    override fun processForward() {
        fragment = ShareSuccessFragment()
        if (activity is State.FragmentChangeListener)
            activity.onFragmentChange(fragment, true)
    }

    override fun processBackward() {
        val stateMachine = ViewModelProvider(activity)[StateMachine::class.java]
        stateMachine.back()
    }

    override fun getFragment(): Fragment? {
        return fragment
    }

    /** Fragment displaying that sharing the Thread network succeeded. */
    class ShareSuccessFragment : WifiConnectivityGuidedStepFragment() {
        private lateinit var stateMachine: StateMachine

        override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
            return GuidanceStylist.Guidance(
                getString(R.string.thread_network_sharing_success),
                null,
                null,
                null
            )
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            stateMachine = ViewModelProvider(requireActivity())[StateMachine::class.java]
            super.onCreate(savedInstanceState)
        }

        override fun onCreateActions(
            actions: MutableList<GuidedAction>,
            savedInstanceState: Bundle?
        ) {
            actions.add(
                GuidedAction.Builder(requireContext())
                    .title(R.string.thread_network_share_again)
                    .id(ACTION_ID_SHARE_AGAIN)
                    .build()
            )
            actions.add(
                GuidedAction.Builder(requireContext())
                    .title(android.R.string.ok)
                    .id(ACTION_ID_OK)
                    .build()
            )
        }

        override fun onGuidedActionClicked(action: GuidedAction) {
            when (action.id) {
                ACTION_ID_SHARE_AGAIN -> stateMachine.listener?.onComplete(
                    this,
                    StateMachine.TRY_AGAIN
                )
                ACTION_ID_OK -> requireActivity().finish()
            }
        }

        companion object {
            private const val ACTION_ID_SHARE_AGAIN = 1L
            private const val ACTION_ID_OK = 2L
        }
    }
}