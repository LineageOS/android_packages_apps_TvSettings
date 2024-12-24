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

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Keep
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.android.tv.twopanelsettings.R
import com.android.tv.twopanelsettings.TwoPanelSettingsFragment.SliceFragmentCallback

/**
 * A screen presenting a slice in TV settings.
 */
@Keep
open class SliceFragment : SettingsPreferenceFragment(),
    SliceFragmentCallback,
    SliceShard.Callbacks {
    private var mSliceShard: SliceShard? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val uriString = arguments!!.getString(SlicesConstants.TAG_TARGET_URI)
        val initialTitle = arguments!!.getCharSequence(
            SlicesConstants.TAG_SCREEN_TITLE,
            ""
        )
        super.onCreate(savedInstanceState)
        preferenceManager.preferenceComparisonCallback =
            object : PreferenceManager.SimplePreferenceComparisonCallback() {
                override fun arePreferenceContentsTheSame(
                    preference1: Preference,
                    preference2: Preference
                ): Boolean {
                    // Should only check for the default SlicePreference objects, and ignore
                    // other instances of slice reference classes since they all override
                    // Preference.onBindViewHolder(PreferenceViewHolder)
                    return preference1.javaClass == SlicePreference::class.java
                            && super.arePreferenceContentsTheSame(preference1, preference2)
                }
            }
        val themeTypedValue = TypedValue()
        requireContext().theme.resolveAttribute(R.attr.preferenceTheme, themeTypedValue, true)
        val prefContext: Context = ContextThemeWrapper(activity, themeTypedValue.resourceId)
        mSliceShard = SliceShard(this, uriString, this, initialTitle, prefContext)
    }

    override fun showProgressBar(toShow: Boolean) {
        if (toShow) {
            showProgressBar()
        } else {
            hideProgressBar()
        }
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val preferenceScreen = preferenceManager
            .createPreferenceScreen(requireContext())
        setPreferenceScreen(preferenceScreen)
    }

    override fun onPreferenceFocused(preference: Preference) {
        mSliceShard!!.onPreferenceFocused(preference)
    }

    override fun onSeekbarPreferenceChanged(preference: SliceSeekbarPreference, addValue: Int) {
        mSliceShard!!.onSeekbarPreferenceChanged(preference, addValue)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return mSliceShard!!.onPreferenceTreeClick(preference)
                || super.onPreferenceTreeClick(preference)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mSliceShard!!.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            mSliceShard!!.onRestoreInstanceState(savedInstanceState)
        }
    }

    private fun showProgressBar() {
        val progressBar = view?.findViewById<View>(R.id.progress_bar)
        if (progressBar != null) {
            progressBar.bringToFront()
            progressBar.visibility = View.VISIBLE
        }
    }

    private fun hideProgressBar() {
        val progressBar = view?.findViewById<View>(R.id.progress_bar)
        if (progressBar != null) {
            progressBar.visibility = View.GONE
        }
    }

    override fun setSubtitle(subtitle: CharSequence?) {
        val view = this.view
        val decorSubtitle = if (view == null)
            null
        else
            view.findViewById<View>(R.id.decor_subtitle) as TextView
        if (decorSubtitle != null) {
            // This is to remedy some complicated RTL scenario such as Hebrew RTL Account slice with
            // English account name subtitle.
            if (resources.configuration.layoutDirection
                == View.LAYOUT_DIRECTION_RTL
            ) {
                decorSubtitle.gravity = Gravity.TOP or Gravity.RIGHT
            }
            if (TextUtils.isEmpty(subtitle)) {
                decorSubtitle.visibility = View.GONE
            } else {
                decorSubtitle.visibility = View.VISIBLE
                decorSubtitle.text = subtitle
            }
        }
    }

    override fun setIcon(icon: Drawable?) {
        val view = this.view
        val decorIcon =
            if (view == null) null else view.findViewById<View>(R.id.decor_icon) as ImageView
        if (decorIcon != null && icon != null) {
            val decorTitle = view!!.findViewById<TextView>(R.id.decor_title)
            if (decorTitle != null) {
                decorTitle.maxWidth = resources.getDimensionPixelSize(R.dimen.decor_title_width)
            }
            decorIcon.setImageDrawable(icon)
            decorIcon.visibility = View.VISIBLE
        } else if (decorIcon != null) {
            decorIcon.visibility = View.GONE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view =
            super.onCreateView(inflater, container, savedInstanceState) as ViewGroup
        val themedInflater = LayoutInflater.from(view.context)
        val newTitleContainer = themedInflater.inflate(
            R.layout.slice_title_container, container, false
        )
        view.removeView(view.findViewById(R.id.decor_title_container))
        view.addView(newTitleContainer, 0)

        if (newTitleContainer != null) {
            newTitleContainer.outlineProvider = null
            newTitleContainer.setBackgroundResource(R.color.tp_preference_panel_background_color)
        }

        val newContainer =
            themedInflater.inflate(R.layout.slice_progress_bar, container, false)
        if (newContainer != null) {
            (newContainer as ViewGroup).addView(view)
        }
        return newContainer
    }

    val screenTitle: CharSequence?
        get() = mSliceShard!!.screenTitle

    override fun getPageId(): Int {
        return mSliceShard!!.pageId
    }

    @get:Deprecated("")
    val metricsCategory: Int
        get() = 0
}