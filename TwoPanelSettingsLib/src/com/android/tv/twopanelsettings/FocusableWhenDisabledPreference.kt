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
package com.android.tv.twopanelsettings

import android.content.Context
import android.util.AttributeSet
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreference

open class FocusableWhenDisabledPreference
@JvmOverloads
constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = androidx.preference.R.attr.switchPreferenceStyle,
  defStyleRes: Int = 0,
) : SwitchPreference(context, attrs, defStyleAttr, defStyleRes) {
  private var mDisabledButFocusable: Boolean = false
  private var mContainer: FocusableWhenDisabledLinearLayout? = null
  private var mWidgetFrame: FocusableWhenDisabledLinearLayout? = null

  init {
    attrs?.let {
      val typedArray =
        context.obtainStyledAttributes(
          it,
          R.styleable.DisabledButFocusablePreference,
          defStyleAttr,
          defStyleRes,
        )
      try {
        mDisabledButFocusable =
          typedArray.getBoolean(
            R.styleable.DisabledButFocusablePreference_disabledButFocusable,
            false,
          )
      } finally {
        typedArray.recycle()
      }
    }
  }

  override fun onBindViewHolder(holder: PreferenceViewHolder) {
    super.onBindViewHolder(holder)

    mContainer =
      (holder.findViewById(R.id.container) as? FocusableWhenDisabledLinearLayout)?.apply {
        setDisabledButFocusableStateActive(mDisabledButFocusable)
      }
    mWidgetFrame =
      (holder.findViewById(android.R.id.widget_frame) as? FocusableWhenDisabledLinearLayout)
        ?.apply { setDisabledButFocusableStateActive(mDisabledButFocusable) }
  }

  override fun onClick() {
    if (mDisabledButFocusable) {
      return
    }

    super.onClick()
  }

  fun getDisabledButFocusable(): Boolean {
    return mDisabledButFocusable
  }

  fun setDisabledButFocusable(state: Boolean) {
    if (mDisabledButFocusable != state) {
      mContainer?.let { it.setDisabledButFocusableStateActive(state) }
      mWidgetFrame?.let { it.setDisabledButFocusableStateActive(state) }
      mDisabledButFocusable = state
      notifyChanged()
    }
  }
}
