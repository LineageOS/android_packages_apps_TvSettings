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
import android.util.TypedValue
import kotlin.lazy

/**
 * A [TextView] that formats a disclosure notice to fit onscreen.
 */
class DisclosureFittingTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : SmoothScalingTextView(context, attrs, defStyleAttr) {
    private var isDuringSearch = false
    private val minTextSizePx: Float by lazy {
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, MIN_TEXT_SIZE_SP, resources.displayMetrics
        )
    }

    override fun requestLayout() {
        // Suppress layout requests during the binary search to avoid layout thrashing.
        if (!isDuringSearch) {
            super.requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.UNSPECIFIED)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        if (heightMode != MeasureSpec.EXACTLY && heightMode != MeasureSpec.AT_MOST) {
            return // Height is not constrained, no need to resize.
        }

        val availableHeight = MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom
        if (availableHeight <= 0 || availableHeight >= measuredHeight) {
            return // Everything fits.
        }


        var low = minTextSizePx
        var high = textSize

        // Binary search for the best text size
        isDuringSearch = true
        while ((high - low) > PRECISION) {
            val mid = (low + high) / 2
            val layoutHeight = getMeasuredHeightForTextSize(mid, widthMeasureSpec)
            if (layoutHeight <= availableHeight) {
                low = mid // It fits, try a larger size
            } else {
                high = mid // It doesn't fit, try a smaller size
            }
        }

        // Apply the best size found.
        setTextSize(TypedValue.COMPLEX_UNIT_PX, low)
        isDuringSearch = false

        // Remeasure with the new text size to finalize dimensions.
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun getMeasuredHeightForTextSize(textSizePx: Float, widthMeasureSpec: Int): Int {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
        super.onMeasure(widthMeasureSpec, MeasureSpec.UNSPECIFIED)
        return measuredHeight
    }

    companion object {
        // Minimum text size in SP.
        private const val MIN_TEXT_SIZE_SP = 8f
        // How close we need to be to the ideal text size.
        private const val PRECISION = 0.5f
    }
}
