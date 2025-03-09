/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.tv.twopanelsettings.slices.builders;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import com.android.tv.twopanelsettings.slices.compat.Clock;
import com.android.tv.twopanelsettings.slices.compat.Slice;
import com.android.tv.twopanelsettings.slices.compat.SliceProvider;
import com.android.tv.twopanelsettings.slices.compat.SystemClock;

/**
 * A copy of TemplateSliceBuilder from slices support lib. Base class of builders of various
 * template types.
 */
public abstract class TemplateSliceBuilder {
  private final Slice.Builder mBuilder;
  private final TemplateBuilderImpl mImpl;

  protected TemplateSliceBuilder(TemplateBuilderImpl impl) {
    mBuilder = null;
    mImpl = impl;
    setImpl(impl);
  }

  public TemplateSliceBuilder(Context unusedContext, Uri uri) {
    mBuilder = new Slice.Builder(uri);
    mImpl = selectImpl(uri);
    if (mImpl == null) {
      throw new IllegalArgumentException("No valid specs found");
    }
    setImpl(mImpl);
  }

  /** Construct the slice. */
  @NonNull
  public Slice buildForSettings() {
    return mImpl.build();
  }

  @Deprecated
  public androidx.slice.Slice build() {
    return new androidx.slice.Slice(buildForSettings().toBundle());
  }

  protected Slice.Builder getBuilder() {
    return mBuilder;
  }

  abstract void setImpl(TemplateBuilderImpl impl);

  protected TemplateBuilderImpl selectImpl(Uri uri) {
    return null;
  }

  protected Clock getClock() {
    if (SliceProvider.getClock() != null) {
      return SliceProvider.getClock();
    }
    return new SystemClock();
  }
}
