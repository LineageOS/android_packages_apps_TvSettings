/*
 * Copyright 2017 The Android Open Source Project
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

package com.android.tv.twopanelsettings.slices.compat.widget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.core.graphics.drawable.IconCompat;

/** A bunch of utilities for slice UI. */
// @RestrictTo(RestrictTo.Scope.LIBRARY)
// @Deprecated // Supported for TV
public class SliceViewUtil {
  /** */
  public static IconCompat createIconFromDrawable(Drawable d) {
    if (d instanceof BitmapDrawable) {
      return IconCompat.createWithBitmap(((BitmapDrawable) d).getBitmap());
    }
    Bitmap b =
        Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(b);
    d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    d.draw(canvas);
    return IconCompat.createWithBitmap(b);
  }

  /** */
  public static int resolveLayoutDirection(int layoutDir) {
    if (layoutDir == View.LAYOUT_DIRECTION_INHERIT
        || layoutDir == View.LAYOUT_DIRECTION_LOCALE
        || layoutDir == View.LAYOUT_DIRECTION_RTL
        || layoutDir == View.LAYOUT_DIRECTION_LTR) {
      return layoutDir;
    }
    return -1;
  }

  private SliceViewUtil() {}
}
