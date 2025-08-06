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
package com.android.tv.twopanelsettings;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ScrollView;

import com.android.tv.twopanelsettings.R;

/**
 * A ScrollView that limits the maximum height that it can take. Can be used with wrap_content
 */
public final class TwoPanelMaxHeightScrollView extends ScrollView {
  private static final int NO_MAX_HEIGHT = -1;

  private final int maxHeight;

  public TwoPanelMaxHeightScrollView(final Context context, final AttributeSet attrs) {
    super(context, attrs);
    final TypedArray attr =
        context.obtainStyledAttributes(attrs, R.styleable.TwoPanelMaxHeightScrollView, 0, 0);
    maxHeight =
        attr.getDimensionPixelSize(
            R.styleable.TwoPanelMaxHeightScrollView_android_maxHeight, NO_MAX_HEIGHT);
    attr.recycle();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    if (maxHeight != NO_MAX_HEIGHT) {
      heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
    }
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }
}