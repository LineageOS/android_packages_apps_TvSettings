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
 * limitations under the License
 */
package com.android.tv.twopanelsettings.slices

import android.net.Uri
import android.view.ContextThemeWrapper
import androidx.lifecycle.Observer
import androidx.preference.Preference
import com.android.tv.twopanelsettings.slices.SlicePreferencesUtil.getEmbeddedItem
import com.android.tv.twopanelsettings.slices.SlicePreferencesUtil.getPreference
import com.android.tv.twopanelsettings.slices.compat.Slice
import com.android.tv.twopanelsettings.slices.compat.widget.ListContent
import com.android.tv.twopanelsettings.slices.compat.widget.SliceContent

/**
 * Helper class to handle the updates for embedded slice preferences.
 */
class EmbeddedSlicePreferenceHelper internal constructor(
    private val mPreference: Preference,
    private val mUri: String
) :
    Observer<Slice?> {
    private val mContext = mPreference.context
    var mListener: SlicePreferenceListener? = null
    var mNewPref: Preference? = null
    private var mSlice: Slice? = null

    fun onAttached() {
        sliceLiveData.observeForever(this)
    }

    fun onDetached() {
        sliceLiveData.removeObserver(this)
    }

    private val sliceLiveData: PreferenceSliceLiveData.SliceLiveDataImpl
        get() = ContextSingleton.getInstance()
            .getSliceLiveData(mContext, Uri.parse(mUri))

    override fun onChanged(slice: Slice?) {
        mSlice = slice
        if (slice == null || slice.hints == null || slice.hints.contains(android.app.slice.Slice.HINT_PARTIAL)) {
            updateVisibility(false)
            return
        }
        update()
    }

    private fun updateVisibility(visible: Boolean) {
        mPreference.isVisible = visible
        if (mListener != null) {
            mListener!!.onChangeVisibility()
        }
    }

    private fun update() {
        val mListContent = ListContent(
            mSlice!!
        )
        val items: List<SliceContent> = mListContent.rowItems
        if (items == null || items.size == 0) {
            updateVisibility(false)
            return
        }
        val embeddedItem = getEmbeddedItem(items)
        // TODO(b/174691340): Refactor this class and integrate the functionality to TsPreference.
        // TODO: Redesign TvSettings project structure so class in twopanelsettings lib can access
        //  FlavorUtils
        // For now, put true here so IconNeedsToBeProcessed will be respected.
        mNewPref = getPreference(
            embeddedItem,
            (mContext as ContextThemeWrapper), null, true, null
        )
        if (mNewPref == null) {
            updateVisibility(false)
            return
        }
        updateVisibility(true)
        if (mPreference is EmbeddedSlicePreference) {
            mPreference.update()
        } else if (mPreference is EmbeddedSliceSwitchPreference) {
            mPreference.update()
        }
    }

    /**
     * Implement this if the container needs to do something when embedded slice preference change
     * visibility.
     */
    interface SlicePreferenceListener {
        /**
         * Callback when the preference change visibility
         */
        fun onChangeVisibility()
    }
}