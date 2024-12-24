/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.tv.twopanelsettings.slices

import android.content.Context
import android.util.AttributeSet
import com.android.tv.twopanelsettings.slices.EmbeddedSlicePreferenceHelper.SlicePreferenceListener

/**
 * An embedded slice preference which would be embedded in common TvSettings preference
 * items, but will automatically update its status and communicates with external apps through
 * slice api.
 */
open class EmbeddedSlicePreference : SlicePreference,
    HasCustomContentDescription {
    private val mHelper: EmbeddedSlicePreferenceHelper
    private var mContentDescription: String? = null

    constructor(context: Context?, uri: String?) : super(context) {
        setUri(uri)
        mHelper = EmbeddedSlicePreferenceHelper(this, getUri())
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        mHelper = EmbeddedSlicePreferenceHelper(this, uri)
    }

    override fun onAttached() {
        super.onAttached()
        mHelper.onAttached()
    }

    override fun onDetached() {
        super.onDetached()
        mHelper.onDetached()
    }

    fun addListener(listener: SlicePreferenceListener?) {
        mHelper.mListener = listener
    }

    fun removeListener(listener: SlicePreferenceListener?) {
        mHelper.mListener = null
    }

    fun update() {
        isEnabled = mHelper.mNewPref!!.isEnabled
        title = mHelper.mNewPref!!.title
        summary = mHelper.mNewPref!!.summary
        icon = mHelper.mNewPref!!.icon
        if (mHelper.mNewPref is HasSliceAction
            && (mHelper.mNewPref as HasSliceAction).sliceAction != null
        ) {
            sliceAction = (mHelper.mNewPref as HasSliceAction).sliceAction
        }
    }


    /**
     * Sets the accessibility content description that will be read to the TalkBack users when they
     * focus on this preference.
     */
    override fun setContentDescription(contentDescription: String) {
        this.mContentDescription = contentDescription
    }

    override fun getContentDescription(): String {
        return mContentDescription!!
    }

    companion object {
        private const val TAG = "EmbeddedSlicePreference"
    }
}