/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.tv.settings.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.android.tv.settings.overlay.FlavorUtils

/**
 * Blocks overlay windows over UI that performs dangerous operations,
 * to avoid user being mislead by a malicious overlay.
 */
class OverlayWindowBlocker(private val mFragment: Fragment, isMainPanel: Boolean) {
    private var mIsMainPanel = isMainPanel
    private var mIsResumed = false

    constructor(fragment: Fragment) : this(fragment,
        !FlavorUtils.isTwoPanel(fragment.requireContext()))

    init {
        mFragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                mIsResumed = true
                updateOverlayBlocking()
            }

            override fun onPause(owner: LifecycleOwner) {
                mIsResumed = false
                updateOverlayBlocking()
            }
        })
    }

    fun setMainPanel(isMainPanel : Boolean) {
        mIsMainPanel = isMainPanel
        updateOverlayBlocking()
    }

    private fun updateOverlayBlocking() {
        mFragment.activity?.window?.setHideOverlayWindows(mIsResumed && mIsMainPanel)
    }
}
