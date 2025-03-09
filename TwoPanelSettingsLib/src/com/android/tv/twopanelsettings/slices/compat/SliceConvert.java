/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tv.twopanelsettings.slices.compat;

import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_BUNDLE;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_LONG;
import static android.app.slice.SliceItem.FORMAT_REMOTE_INPUT;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.collection.ArraySet;
import androidx.core.graphics.drawable.IconCompat;
import java.util.Set;

/**
 * Convert between {@link com.android.tv.twopanelsettings.slices.compat.Slice
 * com.android.tv.twopanelsettings.slices.compat.Slice} and {@link android.app.slice.Slice
 * android.app.slice.Slice}
 *
 * <p>Slice framework has been deprecated, it will not receive any updates moving forward. If you
 * are looking for a framework that handles communication across apps, consider using {@link
 * android.app.appsearch.AppSearchManager}.
 */
@RequiresApi(28)
// @Deprecated // Supported for TV
public class SliceConvert {

  private static final String TAG = "SliceConvert";

  /**
   * Convert {@link com.android.tv.twopanelsettings.slices.compat.Slice
   * com.android.tv.twopanelsettings.slices.compat.Slice} to {@link android.app.slice.Slice
   * android.app.slice.Slice}
   */
  @SuppressWarnings({"ConstantConditions", "deprecation"})
  @SuppressLint("WrongConstant") // conversion from platform definition
  @Nullable
  public static android.app.slice.Slice unwrap(@Nullable Slice slice) {
    if (slice == null || slice.getUri() == null) {
      return null;
    }
    android.app.slice.Slice.Builder builder =
        new android.app.slice.Slice.Builder(slice.getUri(), unwrap(slice.getSpec()));
    builder.addHints(slice.getHints());
    for (com.android.tv.twopanelsettings.slices.compat.SliceItem item : slice.getItemArray()) {
      switch (item.getFormat()) {
        case FORMAT_SLICE:
          builder.addSubSlice(unwrap(item.getSlice()), item.getSubType());
          break;
        case FORMAT_IMAGE:
          builder.addIcon(item.getIcon().toIcon(), item.getSubType(), item.getHints());
          break;
        case FORMAT_REMOTE_INPUT:
          builder.addRemoteInput(item.getRemoteInput(), item.getSubType(), item.getHints());
          break;
        case FORMAT_ACTION:
          builder.addAction(item.getAction(), unwrap(item.getSlice()), item.getSubType());
          break;
        case FORMAT_TEXT:
          builder.addText(item.getText(), item.getSubType(), item.getHints());
          break;
        case FORMAT_INT:
          builder.addInt(item.getInt(), item.getSubType(), item.getHints());
          break;
        case FORMAT_LONG:
          builder.addLong(item.getLong(), item.getSubType(), item.getHints());
          break;
        case FORMAT_BUNDLE:
          builder.addBundle((Bundle) item.mObj, item.getSubType(), item.getHints());
          break;
      }
    }
    return builder.build();
  }

  private static android.app.slice.SliceSpec unwrap(
      com.android.tv.twopanelsettings.slices.compat.SliceSpec spec) {
    if (spec == null) {
      return null;
    }
    return new android.app.slice.SliceSpec(spec.getType(), spec.getRevision());
  }

  static Set<android.app.slice.SliceSpec> unwrap(
      Set<com.android.tv.twopanelsettings.slices.compat.SliceSpec> supportedSpecs) {
    Set<android.app.slice.SliceSpec> ret = new ArraySet<>();
    if (supportedSpecs != null) {
      for (com.android.tv.twopanelsettings.slices.compat.SliceSpec spec : supportedSpecs) {
        ret.add(unwrap(spec));
      }
    }
    return ret;
  }

  /**
   * Convert {@link android.app.slice.Slice android.app.slice.Slice} to {@link
   * com.android.tv.twopanelsettings.slices.compat.Slice
   * com.android.tv.twopanelsettings.slices.compat.Slice}
   */
  @SuppressWarnings("ConstantConditions") // conditional nullability
  @Nullable
  public static com.android.tv.twopanelsettings.slices.compat.Slice wrap(
      @Nullable android.app.slice.Slice slice, @NonNull Context context) {
    if (slice == null || slice.getUri() == null) {
      return null;
    }
    com.android.tv.twopanelsettings.slices.compat.Slice.Builder builder =
        new com.android.tv.twopanelsettings.slices.compat.Slice.Builder(slice.getUri());
    builder.addHints(slice.getHints());
    builder.setSpec(wrap(slice.getSpec()));
    for (android.app.slice.SliceItem item : slice.getItems()) {
      switch (item.getFormat()) {
        case FORMAT_SLICE:
          builder.addSubSlice(wrap(item.getSlice(), context), item.getSubType());
          break;
        case FORMAT_IMAGE:
          try {
            builder.addIcon(
                IconCompat.createFromIcon(context, item.getIcon()),
                item.getSubType(),
                item.getHints());
          } catch (IllegalArgumentException e) {
            Log.w(TAG, "The icon resource isn't available.", e);
          } catch (Resources.NotFoundException e) {
            Log.w(TAG, "The icon resource isn't available.", e);
          }
          break;
        case FORMAT_REMOTE_INPUT:
          builder.addRemoteInput(item.getRemoteInput(), item.getSubType(), item.getHints());
          break;
        case FORMAT_ACTION:
          builder.addAction(item.getAction(), wrap(item.getSlice(), context), item.getSubType());
          break;
        case FORMAT_TEXT:
          builder.addText(item.getText(), item.getSubType(), item.getHints());
          break;
        case FORMAT_INT:
          builder.addInt(item.getInt(), item.getSubType(), item.getHints());
          break;
        case FORMAT_LONG:
          builder.addLong(item.getLong(), item.getSubType(), item.getHints());
          break;
        case FORMAT_BUNDLE:
          builder.addItem(
              new SliceItem(
                  item.getBundle(), item.getFormat(), item.getSubType(), item.getHints()));
          break;
      }
    }
    return builder.build();
  }

  private static com.android.tv.twopanelsettings.slices.compat.SliceSpec wrap(
      android.app.slice.SliceSpec spec) {
    if (spec == null) {
      return null;
    }
    return new com.android.tv.twopanelsettings.slices.compat.SliceSpec(
        spec.getType(), spec.getRevision());
  }

  /** */
  @NonNull
  // @RestrictTo(RestrictTo.Scope.LIBRARY)
  public static Set<com.android.tv.twopanelsettings.slices.compat.SliceSpec> wrap(
      @Nullable Set<android.app.slice.SliceSpec> supportedSpecs) {
    Set<com.android.tv.twopanelsettings.slices.compat.SliceSpec> ret = new ArraySet<>();
    if (supportedSpecs != null) {
      for (android.app.slice.SliceSpec spec : supportedSpecs) {
        ret.add(wrap(spec));
      }
    }
    return ret;
  }

  private SliceConvert() {}
}
