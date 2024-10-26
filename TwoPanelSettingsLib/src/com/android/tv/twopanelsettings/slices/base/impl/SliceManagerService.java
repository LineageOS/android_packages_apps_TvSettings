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

package com.android.tv.twopanelsettings.slices.base.impl;

import android.app.slice.SliceSpec;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;
import android.util.Log;
import androidx.annotation.GuardedBy;
import androidx.annotation.VisibleForTesting;
import java.util.ArrayList;

/** */
public class SliceManagerService {

  private static final String TAG = "SliceManagerService";
  private static final Handler sMainHandler = new Handler(Looper.getMainLooper());

  private final Object mLock = new Object();

  private final Context mContext;

  @GuardedBy("mLock")
  private final ArrayMap<Uri, PinnedSliceState> mPinnedSlicesByUri = new ArrayMap<>();

  public SliceManagerService(Context context) {
    mContext = context;
  }

  public Uri[] getPinnedSlices() {
    ArrayList<Uri> ret = new ArrayList<>();
    synchronized (mLock) {
      for (PinnedSliceState state : mPinnedSlicesByUri.values()) {
        Uri uri = state.getUri();
        ret.add(uri);
      }
    }
    return ret.toArray(new Uri[0]);
  }

  public void pinSlice(Uri uri, SliceSpec[] specs) {
    getOrCreatePinnedSlice(uri).pin(specs);
  }

  public void unpinSlice(Uri uri) {
    try {
      PinnedSliceState slice = getPinnedSlice(uri);
      if (slice != null && slice.unpin()) {
        removePinnedSlice(uri);
      }
    } catch (IllegalStateException exception) {
      Log.w(TAG, exception.getMessage());
    }
  }

  public SliceSpec[] getPinnedSpecs(Uri uri) {
    return getPinnedSlice(uri).getSpecs();
  }

  protected void removePinnedSlice(Uri uri) {
    synchronized (mLock) {
      mPinnedSlicesByUri.remove(uri).destroy();
    }
  }

  private PinnedSliceState getPinnedSlice(Uri uri) {
    synchronized (mLock) {
      PinnedSliceState manager = mPinnedSlicesByUri.get(uri);
      if (manager == null) {
        throw new IllegalStateException(String.format("Slice %s not pinned", uri.toString()));
      }
      return manager;
    }
  }

  private PinnedSliceState getOrCreatePinnedSlice(Uri uri) {
    synchronized (mLock) {
      PinnedSliceState manager = mPinnedSlicesByUri.get(uri);
      if (manager == null) {
        manager = createPinnedSlice(uri);
        mPinnedSlicesByUri.put(uri, manager);
      }
      return manager;
    }
  }

  @VisibleForTesting
  protected PinnedSliceState createPinnedSlice(Uri uri) {
    return new PinnedSliceState(this, uri);
  }

  public Object getLock() {
    return mLock;
  }

  public Context getContext() {
    return mContext;
  }

  public Handler getHandler() {
    return sMainHandler;
  }
}
