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
package com.android.tv.settings.widget;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.android.settingslib.applications.ApplicationsState;
import com.android.tv.settings.overlay.FlavorUtils;

/** Modified version of IconDrawableFactory for TV icons */
public class TvIconDrawableFactory {
  private static final int ROUND_ICON_MIN_SIZE = 160;

  protected final Context mContext;
  protected final PackageManager mPm;

  private TvIconDrawableFactory(Context context) {
    mContext = context;
    mPm = context.getPackageManager();
  }

  public static TvIconDrawableFactory newInstance(Context context) {
    return new TvIconDrawableFactory(context);
  }

  // Returns a rounded application icon if the flavor is Two Panel settings.
  public Drawable maybeGetRoundAppIcon(ApplicationInfo applicationInfo) {
    Drawable icon;
    Drawable packageManagerIcon = mPm.loadItemIcon(applicationInfo, applicationInfo);
    if (!FlavorUtils.isTwoPanel(mContext)) {
      return packageManagerIcon;
    }

    icon = packageManagerIcon;
    if (icon != null) {
      Bitmap iconBitmap = getBitmapFromDrawable(icon);
      Bitmap roundIconBitmap = getRoundBitmap(iconBitmap);
      return new BitmapDrawable(mContext.getResources(), roundIconBitmap);
    } else {
      return packageManagerIcon;
    }
  }


  private static Bitmap getBitmapFromDrawable(Drawable drawable) {
    Bitmap bitmap =
        Bitmap.createBitmap(
            drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    drawable.setBounds(/* left= */ 0, /* top= */ 0, canvas.getWidth(), canvas.getHeight());
    drawable.draw(canvas);
    return bitmap;
  }

  private static Bitmap getRoundBitmap(Bitmap bitmap) {
    int width = bitmap.getWidth();
    int height = bitmap.getHeight();
    int size;
    size = Math.min(width, height);

    Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(output);

    final Paint paint = new Paint();
    final Rect rect =
        new Rect(
            (width - size) / 2,
            (height - size) / 2,
            width - (width - size) / 2,
            height - (height - size) / 2);
    final RectF rectF = new RectF(/* left= */ 0, /* top= */ 0, size, size);

    paint.setAntiAlias(true);
    canvas.drawOval(rectF, paint);
    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    canvas.drawBitmap(bitmap, rect, rectF, paint);

    return output;
  }
}
