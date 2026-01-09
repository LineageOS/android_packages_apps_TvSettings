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

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
import android.util.LruCache;
import androidx.annotation.NonNull;
import com.android.settingslib.applications.ApplicationsState;
import com.android.tv.settings.overlay.FlavorUtils;
import com.android.tv.settings.R;

/** Modified version of IconDrawableFactory for TV icons */
public class TvIconDrawableFactory implements ComponentCallbacks2 {

  protected final Context mContext;
  protected final PackageManager mPm;

  private final LruCache<String, Drawable> mIconCache;

  private TvIconDrawableFactory(Context context) {
    mContext = context;
    mPm = context.getPackageManager();

    int maxCacheSize = mContext.getResources().getInteger(R.integer.config_icon_cache_max_size);

    mIconCache = new LruCache<String, Drawable>(maxCacheSize) {
      @Override
      protected int sizeOf(String key, Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
          Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
          return bitmap != null ? bitmap.getAllocationByteCount() : 0;
        }
        return 1;
      }
    };

    mContext.registerComponentCallbacks(this);
  }

  public static TvIconDrawableFactory newInstance(Context context) {
    return new TvIconDrawableFactory(context);
  }

  // Returns a rounded application icon if the flavor is Two Panel settings.
  public Drawable maybeGetRoundAppIcon(ApplicationInfo applicationInfo) {
    String pkgName = applicationInfo.packageName;

    Drawable cachedIcon = mIconCache.get(pkgName);
    if (cachedIcon != null) {
      return cachedIcon;
    }

    Drawable packageManagerIcon = mPm.loadItemIcon(applicationInfo, applicationInfo);
    if (!FlavorUtils.isTwoPanel(mContext)) {
      return packageManagerIcon;
    }

    if (packageManagerIcon != null) {
      Bitmap iconBitmap = getBitmapFromDrawable(packageManagerIcon);
      Bitmap roundIconBitmap = getRoundBitmap(iconBitmap);
      Drawable roundIcon = new BitmapDrawable(mContext.getResources(), roundIconBitmap);

      mIconCache.put(pkgName, roundIcon);
      return roundIcon;
    }

    return packageManagerIcon;
  }

  @Override
  public void onTrimMemory(int level) {
    if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE
            || level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
      mIconCache.evictAll();
    }
  }

  @Override
  public void onConfigurationChanged(@NonNull Configuration newConfig) {
  }

  @Override
  public void onLowMemory() {
    mIconCache.evictAll();
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
