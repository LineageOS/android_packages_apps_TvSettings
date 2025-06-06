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
import com.android.tv.twopanelsettings.slices.builders.PreferenceSliceBuilder.RowBuilder;
import com.android.tv.twopanelsettings.slices.compat.Slice;
import com.android.tv.twopanelsettings.slices.compat.SliceProvider;

/**
 * Google TV - TvSettings Slices API Integration Guide.
 *
 * <p>This guide outlines how to integrate third-party settings into the Google TV Settings app
 * using its custom Slices API. This API is distinct from deprecated Android slices and is fully
 * supported going forward.
 *
 * <h2>Overview</h2>
 * <p>The TvSettings Slices API allows apps to implement and surface their own settings content
 * directly within the main TV Settings application. This provides a seamless user experience by
 * embedding external settings as if they were part of TV Settings.
 *
 * <p>Additionally, a variation of slices can customize default TV Settings pages such as top level
 * settings, System or Network preferences by rearranging and rename settings without OTA updates
 * and adding new slice preferences from Google and OEMs.
 *
 * <h3>Use Cases</h3>
 * <ul>
 * <li>Integrating sections like "Parental controls" from Google Play.
 * <li>Adding new settings for features like Ambient Mode.
 * <li>Implementing OEM-specific settings for custom hardware.
 * <li>Keeping naming and order of settings consistent across Android releases without OTAs
 * <li>Exposing new 1P and 3P functionality with play store rather than system updates.
 * </ul>
 *
 * <h2>Integration</h2>
 *
 * To integrate with TV Settings, extend {@link TvSettingsSliceProvider} and override
 * {@link #createSlice} to populate the settings screen. Multiple screens can be handled by the
 * same slice provider by checking passed in URI. Export content provider in your app Manifest and
 * ensure access control by requiring WRITE_SECURE_SETTINGS permission or checking caller package.
 * <p>
 * When user interacts with Settings, you will get callbacks configured by
 * {@link RowBuilder#setAction} and {@link RowBuilder#setFollowupAction}. Activity and broadcast
 * callbacks are supported.
 *
 * <h3>All slice settings pages details</h3>
 *
 * <ul>
 *     <li>Override a corresponding string resource, for example
 *         R.string.connected_devices_slice_uri to point to your content provider in device specific
 *         overlay.
 *     <li>Call {@link PreferenceSliceBuilder#addScreenTitle} to describe the page.
 *     <li>Call {@link PreferenceSliceBuilder#addPreference} or
 *          {@link PreferenceSliceBuilder#addEmbeddedPreference} to add desired settings.
 *     <li>If data is not ready yet return false and load it on background (though ANRs are no
 *         longer raised if {@link #createSlice} blocks).
 *     <li>When data is loaded or changes, call {@link #invalidateSlice} to update UI
 * </ul>
 *
 * <h3>Updatable main settings pages details</h3>
 *
 * <ul>
 *     <li>Override a corresponding resource, for example, R.string.main_fragment_slice_uri</li>
 *     <li>Include default settings by calling {@link PreferenceSliceBuilder#addFromSliceUri} with
 *         default URI for the page. This will inherit title screen unless already set.
 *     <li>Add any additional settings at the end.
 *     <li>Actions must use regular rather than pending intents as slices are cached persistently.
 *         Protect activities/broadcasts with WRITE_SECURE_SETTINGS permission if sensitive.
 *     <li>Use {@link PreferenceSliceBuilder#addEmbeddedPreference} if pending intents or up to
 *         date current state are required.
 *     <li>If data is not ready yet (for example content provider host app does not have overlay
 *         for current system language installed) return false. Default settings will be used and
 *         content provider will be called again when settings screen is reopened.
 *     <li>Slice will be refreshed when TV Settings, content provider host app or app referenced in
 *         {@link PreferenceSliceBuilder#addFromSliceUri} is updated. If update is needed earlier,
 *         for example due to language overlay being installed, call
 *         {@link #invalidatesCachedSlices}.
 * </ul>
 *
 * @see PreferenceSliceBuilder
 * @see RowBuilder
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
