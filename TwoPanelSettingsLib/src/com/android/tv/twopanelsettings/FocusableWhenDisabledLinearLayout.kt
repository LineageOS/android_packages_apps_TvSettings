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
 * limitations under the License
 */

package com.android.tv.twopanelsettings

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

class FocusableWhenDisabledLinearLayout
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
  LinearLayout(context, attrs, defStyleAttr) {
  private var mDisabledButFocusable: Boolean = false

  fun setDisabledButFocusableStateActive(isActive: Boolean) {
    if (mDisabledButFocusable != isActive) {
      mDisabledButFocusable = isActive
      refreshDrawableState()
    }
  }

  fun isDisabledButFocusableStateActive(): Boolean {
    return mDisabledButFocusable
  }

  override fun onCreateDrawableState(extraSpace: Int): IntArray {
    if (mDisabledButFocusable) {
      val drawableState = super.onCreateDrawableState(extraSpace + 1)
      mergeDrawableStates(drawableState, DISABLED_BUT_FOCUSABLE)
      return drawableState
    }
    return super.onCreateDrawableState(extraSpace)
  }

  companion object {
    private val DISABLED_BUT_FOCUSABLE = intArrayOf(R.attr.disabledButFocusable)
  }
}
