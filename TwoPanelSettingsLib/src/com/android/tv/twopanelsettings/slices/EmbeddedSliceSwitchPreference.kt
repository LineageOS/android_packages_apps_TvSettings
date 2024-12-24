/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.app.PendingIntent
import android.app.slice.Slice
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Log
import androidx.preference.TwoStatePreference
import com.android.tv.twopanelsettings.R
import com.android.tv.twopanelsettings.slices.EmbeddedSlicePreferenceHelper.SlicePreferenceListener

/**
 * An embedded slice switch preference which would be embedded in common TvSettings preference
 * items, but will automatically update its status and communicates with external apps through
 * slice api.
 */
class EmbeddedSliceSwitchPreference : SliceSwitchPreference {
    private val mHelper: EmbeddedSlicePreferenceHelper
    private var mUri: String? = null

    override fun onAttached() {
        super.onAttached()
        mHelper.onAttached()
    }

    override fun onDetached() {
        super.onDetached()
        mHelper.onDetached()
    }

    constructor(context: Context?) : super(context) {
        init(null)
        mHelper = EmbeddedSlicePreferenceHelper(this, mUri!!)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
        mHelper = EmbeddedSlicePreferenceHelper(this, mUri!!)
    }

    private fun init(attrs: AttributeSet?) {
        if (attrs != null) {
            initStyleAttributes(attrs)
        }
    }

    private fun initStyleAttributes(attrs: AttributeSet) {
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.SlicePreference
        )
        for (i in a.indexCount - 1 downTo 0) {
            val attr = a.getIndex(i)
            if (attr == R.styleable.SlicePreference_uri) {
                mUri = a.getString(attr)
                break
            }
        }
    }

    fun addListener(listener: SlicePreferenceListener?) {
        mHelper.mListener = listener
    }

    fun removeListener(listener: SlicePreferenceListener?) {
        mHelper.mListener = null
    }

    fun update() {
        title = mHelper.mNewPref!!.title
        summary = mHelper.mNewPref!!.summary
        icon = mHelper.mNewPref!!.icon
        if (mHelper.mNewPref is TwoStatePreference) {
            isChecked = (mHelper.mNewPref as TwoStatePreference).isChecked
        }
        if (mHelper.mNewPref is HasSliceAction) {
            sliceAction = (mHelper.mNewPref as HasSliceAction).sliceAction
        }
        isVisible = true
    }

    public override fun onClick() {
        var newValue = !isChecked
        try {
            if (mAction == null) {
                return
            }
            if (mAction.isToggle) {
                // Update the intent extra state
                val i: Intent = Intent().putExtra(Slice.EXTRA_TOGGLE_STATE, newValue)
                mAction.actionItem!!.fireAction(context, i)
            } else {
                mAction.actionItem!!.fireAction(null, null)
            }
        } catch (e: PendingIntent.CanceledException) {
            newValue = !newValue
            Log.e(TAG, "PendingIntent for slice cannot be sent", e)
        }
        if (callChangeListener(newValue)) {
            isChecked = newValue
        }
    }

    companion object {
        private const val TAG = "EmbeddedSliceSwitchPreference"
    }
}