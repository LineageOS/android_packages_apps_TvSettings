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

package com.android.tv.settings;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.service.settings.suggestions.Suggestion;
import com.android.settingslib.suggestions.SuggestionController;
import com.android.settingslib.suggestions.SuggestionLoader;
import com.android.settingslib.suggestions.SuggestionLoaderCompat;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.List;

/**
 * Manages IPC communication to SettingsIntelligence for suggestion related services. Copy of
 * SettingsLib SuggestionControllerMixinCompat. Needed because we need to add null checks for mHost
 */
public class TvSuggestionControllerMixinCompat implements
        SuggestionController.ServiceConnectionListener, androidx.lifecycle.LifecycleObserver,
        LoaderManager.LoaderCallbacks<List<Suggestion>> {

    public interface SuggestionControllerHost {
        /**
         * Called when suggestion data fetching is ready.
         */
        void onSuggestionReady(List<Suggestion> data);

        /**
         * Returns {@link LoaderManager} associated with the host. If host is not attached to
         * activity then return null.
         */
        @Nullable
        LoaderManager getLoaderManager();
    }

    private static final String TAG = "SuggestionCtrlMixin";
    private static final boolean DEBUG = false;

    private final Context mContext;
    private final SuggestionController mSuggestionController;
    private final SuggestionControllerHost mHost;

    private boolean mSuggestionLoaded;

    public TvSuggestionControllerMixinCompat(Context context, SuggestionControllerHost host,
            Lifecycle lifecycle, ComponentName componentName) {
        mContext = context.getApplicationContext();
        mHost = host;
        mSuggestionController = new SuggestionController(mContext, componentName,
                    this /* serviceConnectionListener */);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        if (DEBUG) {
            Log.d(TAG, "SuggestionController started");
        }
        mSuggestionController.start();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        if (DEBUG) {
            Log.d(TAG, "SuggestionController stopped.");
        }
        mSuggestionController.stop();
    }

    @Override
    public void onServiceConnected() {
        if (mHost instanceof Fragment && !((Fragment) mHost).isAdded()) {
            // Do not proceed if the fragment is not attached.
            return;
        }
        final LoaderManager loaderManager = mHost.getLoaderManager();
        if (loaderManager != null) {
            loaderManager.restartLoader(SuggestionLoader.LOADER_ID_SUGGESTIONS,
                    null /* args */, this /* callback */);
        }
    }

    @Override
    public void onServiceDisconnected() {
        if (mHost instanceof Fragment && !((Fragment) mHost).isAdded()) {
            // Do not proceed if the fragment is not attached.
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "SuggestionService disconnected");
        }
        final LoaderManager loaderManager = mHost.getLoaderManager();
        if (loaderManager != null) {
            loaderManager.destroyLoader(SuggestionLoader.LOADER_ID_SUGGESTIONS);
        }
    }

    @Override
    public Loader<List<Suggestion>> onCreateLoader(int id, Bundle args) {
        if (id == SuggestionLoader.LOADER_ID_SUGGESTIONS) {
            mSuggestionLoaded = false;
            return new SuggestionLoaderCompat(mContext, mSuggestionController);
        }
        throw new IllegalArgumentException("This loader id is not supported " + id);
    }

    @Override
    public void onLoadFinished(Loader<List<Suggestion>> loader, List<Suggestion> data) {
        mSuggestionLoaded = true;
        mHost.onSuggestionReady(data);
    }

    @Override
    public void onLoaderReset(Loader<List<Suggestion>> loader) {
        mSuggestionLoaded = false;
    }

    public boolean isSuggestionLoaded() {
        return mSuggestionLoaded;
    }

    public void dismissSuggestion(Suggestion suggestion) {
        mSuggestionController.dismissSuggestions(suggestion);
    }

    public void launchSuggestion(Suggestion suggestion) {
        mSuggestionController.launchSuggestion(suggestion);
    }
}
