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
package com.android.tv.twopanelsettings.slices;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.android.tv.twopanelsettings.slices.builders.PreferenceSliceBuilder;
import com.android.tv.twopanelsettings.slices.compat.Slice;
import com.android.tv.twopanelsettings.slices.compat.SliceProvider;

/**
 * Slices allow adding individual settings and settings pages to TV Settings through content
 * providers hosted in other apps. Select existing settings pages, including top level and
 * System settings can be also customized through Google and OEM slice providers. These pages
 * are cached by TV Settings persistently for performance and reliability and are only refetched
 * when host app is updated.
 * <h1>Use cases</h1>
 * Use cases for slice provider include the following:
 * <ul>
 *     <li>Integrating sections implemented by other Google apps for example Google Play or
 *         Ambient mode</li>
 *     <li>OEMs providing settings for special hardware or software capabilities of specific
 *         devices</li>
 *     <li>Google and OEMs being able to update settings tree without an OTA</li>
 * </ul>
 */
public abstract class TvSettingsSliceProvider extends SliceProvider {
    private static final String CLEAR_CACHE_BROADCAST =
            "com.android.tv.settings.CLEAR_CACHED_SLICES";
    private static final String TV_SETTINGS_PACKAGE =
            "com.android.tv.settings";

    public TvSettingsSliceProvider() {
        super(Manifest.permission.WRITE_SECURE_SETTINGS);
    }

    @Override
    public boolean onCreateSliceProvider() {
        return true;
    }

    @Override
    public Slice onBindSlice(@NonNull Uri sliceUri, @NonNull Bundle extras) {
        PreferenceSliceBuilder builder =
                new PreferenceSliceBuilder(requireContext(), sliceUri,
                        PreferenceSliceBuilder.INFINITY);
        if (!createSlice(builder, sliceUri))
            builder.setNotReady();
        return builder.buildForSettings();
    }

    /**
     * Add preferences and preference categories that should appear on a given settings page. If
     * needed data is not available yet, call {@link PreferenceSliceBuilder#setNotReady} and return
     * to show a progress bar. When data is loaded, call {@link #invalidateSlice} with slice URI
     * to have TV Settings refetch data if the screen is still visible.
     * <p>
     * There are two types of TV Settings slices. Transient slices are fetched every time a page
     * is displayed. Persistent slices are cached by TV Settings and are only refetched when
     * slice host app is updated or system language changes. Call {@link #invalidatesCachedSlices}
     * to refresh these explicitly next time the page is visited.
     * <p>
     * For persistent slices, make sure to return false or invalidate cached slices later if up to
     * data data, for example data in current system language, is not available yet.
     */
    protected abstract boolean createSlice(PreferenceSliceBuilder builder,
            Uri sliceUri);

    /**
     * Invalidate data for a slice URI, triggering a refresh if slice is currently displayed.
     * Use {@link #invalidatesCachedSlices} to clear cached slices, which do not support
     * refresh while displayed, only on next visit. Use
     * {@link PreferenceSliceBuilder#addEmbeddedPreference} to refresh individual settings on
     * a cached page from another slice URI.
     */
    public static void invalidateSlice(Context context, Uri sliceUri) {
        context.getContentResolver().notifyChange(sliceUri, null);
    }

    /**
     * Empties TV Settings slices cache so that next access refetches updated data. This is only
     * applicable to persistently cached slices that are part of updatable settings tree.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    public static void invalidatesCachedSlices(Context context) {
        context.sendBroadcast(new Intent(CLEAR_CACHE_BROADCAST).setPackage(TV_SETTINGS_PACKAGE));
    }

    @Override
    public PendingIntent onCreatePermissionRequest(Uri sliceUri, String callingPackage) {
        final Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
        final PendingIntent noOpIntent = PendingIntent.getActivity(
                getContext(), 0, settingsIntent, PendingIntent.FLAG_IMMUTABLE);
        return noOpIntent;
    }
}
