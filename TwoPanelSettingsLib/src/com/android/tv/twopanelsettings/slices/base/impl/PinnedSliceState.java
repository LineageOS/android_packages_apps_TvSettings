/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.tv.twopanelsettings.slices.base.impl;

import android.app.slice.SliceSpec;
import android.content.ContentProviderClient;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.VisibleForTesting;

import com.android.tv.twopanelsettings.slices.base.SliceProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Manages the state of a pinned slice.
 */
public class PinnedSliceState {

    private static final String TAG = "PinnedSliceState";

    private final Object mLock;

    private final SliceManagerService mService;
    private final Uri mUri;
    @GuardedBy("mLock")
    private int mListenerCount;
    @GuardedBy("mLock")
    private SliceSpec[] mSupportedSpecs = null;

    private boolean mSlicePinned;

    public PinnedSliceState(SliceManagerService service, Uri uri) {
        mService = service;
        mUri = uri;
        mLock = mService.getLock();
    }

    public SliceSpec[] getSpecs() {
        return mSupportedSpecs;
    }

    public void mergeSpecs(SliceSpec[] supportedSpecs) {
        synchronized (mLock) {
            if (mSupportedSpecs == null) {
                mSupportedSpecs = supportedSpecs;
            } else {
                List<SliceSpec> specs = Arrays.asList(mSupportedSpecs);
                mSupportedSpecs = specs.stream().map(s -> {
                    SliceSpec other = findSpec(supportedSpecs, s.getType());
                    if (other == null) return null;
                    if (other.getRevision() < s.getRevision()) {
                        return other;
                    }
                    return s;
                }).filter(s -> s != null).toArray(SliceSpec[]::new);
            }
        }
    }

    private SliceSpec findSpec(SliceSpec[] specs, String type) {
        for (SliceSpec spec : specs) {
            if (Objects.equals(spec.getType(), type)) {
                return spec;
            }
        }
        return null;
    }

    public Uri getUri() {
        return mUri;
    }

    public void destroy() {
        setSlicePinned(false);
    }

    private void setSlicePinned(boolean pinned) {
        synchronized (mLock) {
            if (mSlicePinned == pinned) return;
            mSlicePinned = pinned;
            if (pinned) {
                mService.getHandler().post(this::handleSendPinned);
            } else {
                mService.getHandler().post(this::handleSendUnpinned);
            }
        }
    }

    public void pin(SliceSpec[] specs) {
        synchronized (mLock) {
            mListenerCount++;
            mergeSpecs(specs);
            setSlicePinned(true);
        }
    }

    public boolean unpin() {
        synchronized (mLock) {
            mListenerCount--;
        }
        return !hasPinOrListener();
    }

    public boolean isListening() {
        synchronized (mLock) {
            return mListenerCount > 0;
        }
    }

    @VisibleForTesting
    public boolean hasPinOrListener() {
        return isListening();
    }

    ContentProviderClient getClient() {
        ContentProviderClient client = mService.getContext().getContentResolver()
                .acquireUnstableContentProviderClient(mUri);
        return client;
    }

    private void checkSelfRemove() {
        if (!hasPinOrListener()) {
            // All the listeners died, remove from pinned state.
            mService.removePinnedSlice(mUri);
        }
    }

    private void handleSendPinned() {
        try (ContentProviderClient client = getClient()) {
            if (client == null) return;
            Bundle b = new Bundle();
            b.putParcelable(SliceProvider.EXTRA_BIND_URI, mUri);
            try {
                client.call(SliceProvider.METHOD_PIN, null, b);
            } catch (Exception e) {
                Log.w(TAG, "Unable to contact " + mUri, e);
            }
        }
    }

    private void handleSendUnpinned() {
        try (ContentProviderClient client = getClient()) {
            if (client == null) return;
            Bundle b = new Bundle();
            b.putParcelable(SliceProvider.EXTRA_BIND_URI, mUri);
            try {
                client.call(SliceProvider.METHOD_UNPIN, null, b);
            } catch (Exception e) {
                Log.w(TAG, "Unable to contact " + mUri, e);
            }
        }
    }
}
