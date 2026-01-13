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

import android.graphics.drawable.Icon
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.android.tv.settings.R
import com.android.tv.settings.connectivity.util.State
import com.android.tv.settings.connectivity.util.StateMachine
import com.android.tv.twopanelsettings.FullScreenDialogFragment

/** State for displaying that sharing the Thread network failed. */
class ShareFailedState(private val activity: FragmentActivity) : State {
    private var fragment: Fragment? = null

    override fun processForward() {
        val fragment = ShareFailedFragment()
        val icon = Icon.createWithResource(activity, R.drawable.ic_info_outline)
        val args = FullScreenDialogFragment.DialogBuilder()
            .setIcon(icon)
            .setTitle(activity.getString(R.string.thread_network_sharing_failed))
            .setMessage(activity.getString(R.string.thread_network_sharing_failure_user_tips))
            .setPositiveButton(activity.getString(R.string.thread_network_share_again))
            .setNegativeButton(activity.getString(android.R.string.cancel))
            .build()
        fragment.arguments = args
        this.fragment = fragment
        if (activity is State.FragmentChangeListener) {
            activity.onFragmentChange(fragment, true)
        }
    }

    override fun processBackward() {
        val stateMachine = ViewModelProvider(activity)[StateMachine::class.java]
        stateMachine.back()
    }

    override fun getFragment(): Fragment? {
        return fragment
    }

    /** Fragment displaying that sharing the Thread network failed. */
    class ShareFailedFragment : FullScreenDialogFragment() {
        private lateinit var stateMachine: StateMachine

        override fun onCreate(savedInstanceState: Bundle?) {
            stateMachine = ViewModelProvider(requireActivity())[StateMachine::class.java]
            super.onCreate(savedInstanceState)
        }

        override fun onButtonPressed(action: Int) {
            when (action) {
                ACTION_POSITIVE -> stateMachine.listener?.onComplete(this, StateMachine.TRY_AGAIN)
                ACTION_NEGATIVE -> requireActivity().finish()
            }
        }
    }
}
