/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.tv.twopanelsettings.slices.base;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;

/** */
@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@SuppressWarnings({"deprecation", "unchecked"})
@SuppressLint({"ConcreteCollection", "NullableCollection"})
public final class BundleCompat {
  @Nullable
  public static <T> T getParcelable(
      @NonNull Bundle in, @Nullable String key, @NonNull Class<T> clazz) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      return in.getParcelable(key, clazz);
    } else {
      T parcelable = in.getParcelable(key);
      return clazz.isInstance(parcelable) ? parcelable : null;
    }
  }

  @Nullable
  public static <T> ArrayList<T> getParcelableArrayList(
      @NonNull Bundle in, @Nullable String key, @NonNull Class<? extends T> clazz) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      return in.getParcelableArrayList(key, clazz);
    } else {
      return (ArrayList<T>) in.getParcelableArrayList(key);
    }
  }

  private BundleCompat() {}
}
