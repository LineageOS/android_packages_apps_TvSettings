/*
 * Copyright (C) 2026 The Android Open Source Project
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
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.VisibleForTesting

/**
 * A [LinearLayout] that formats a disclosure notice to fit onscreen by adjusting text size of
 * contained [TextView]s. Even for a single text view, this provides flexibility beyond framework
 * autosizing as wrap_content heights are supported, with autosizing enforcing maximum height.
 */
class DisclosureFittingLinearLayout
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
  LinearLayout(context, attrs, defStyleAttr) {
  private var isDuringSearch = false

  @VisibleForTesting
  val minTextSizePx: Float
    get() =
      TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        MIN_TEXT_SIZE_DP,
        resources.displayMetrics,
      )

  override fun requestLayout() {
    // Suppress layout requests during the binary search to avoid layout thrashing.
    if (!isDuringSearch) {
      super.requestLayout()
    }
  }

  private fun findTextViews(view: View, textViews: MutableList<TextView>) {
    if (view is TextView) {
      textViews.add(view)
    } else if (view is ViewGroup) {
      for (i in 0 until view.childCount) {
        findTextViews(view.getChildAt(i), textViews)
      }
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, MeasureSpec.UNSPECIFIED)
    if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
      return // Height is not constrained, no need to resize.
    }

    val availableHeight = MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom
    if (availableHeight <= 0 || availableHeight >= measuredHeight) {
      return // Everything fits.
    }

    val textViews = mutableListOf<TextView>()
    findTextViews(this, textViews)
    if (textViews.isEmpty()) {
      return
    }

    var low = minTextSizePx
    var high = textViews[0].textSize

    // Binary search for the best text size
    isDuringSearch = true
    while ((high - low) > PRECISION) {
      val mid = (low + high) / 2
      for (textView in textViews) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mid)
      }
      super.onMeasure(widthMeasureSpec, MeasureSpec.UNSPECIFIED)
      if (measuredHeight <= availableHeight) {
        low = mid // It fits, try a larger size
      } else {
        high = mid // It doesn't fit, try a smaller size
      }
    }

    // Apply the best size found.
    for (textView in textViews) {
      textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, low)
    }
    isDuringSearch = false

    // Remeasure with the new text size to finalize dimensions.
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }

  companion object {
    /** Minimum text size in DP. */
    private const val MIN_TEXT_SIZE_DP = 4f
    /** How close we need to be to the ideal text size. */
    private const val PRECISION = 0.1f
  }
}
