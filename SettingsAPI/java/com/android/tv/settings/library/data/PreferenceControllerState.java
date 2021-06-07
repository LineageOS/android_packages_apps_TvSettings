/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.tv.settings.library.data;

import android.content.Context;
import android.os.Bundle;
import android.util.ArraySet;
import android.util.Log;

import com.android.tv.settings.library.PreferenceCompat;
import com.android.tv.settings.library.UIUpdateCallback;
import com.android.tv.settings.library.util.AbstractPreferenceController;
import com.android.tv.settings.library.util.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * State for managing preferenceCompats controlled through {@link AbstractPreferenceController} and
 * provide Lifecycle components.
 */
public abstract class PreferenceControllerState implements State {
    private static final String TAG = "PreferenceControllerState";
    protected final Context mContext;
    protected UIUpdateCallback mUIUpdateCallback;
    protected PreferenceCompatManager mPreferenceCompatManager;
    public PreferenceControllerState(Context context, UIUpdateCallback callback) {
        mUIUpdateCallback = callback;
        mContext = context;
        mPreferenceCompatManager = new PreferenceCompatManager();
    }
    private final Set<AbstractPreferenceController> mPreferenceControllers = new ArraySet<>();
    private Lifecycle mLifecycle;

    @Override
    public void onCreate(Bundle extras) {
        mLifecycle = new Lifecycle();
        List<AbstractPreferenceController> controllers = onCreatePreferenceControllers(mContext);
        if (controllers == null) {
            controllers = new ArrayList<>();
        }
        mPreferenceControllers.addAll(controllers);
        refreshAllPreferences();
    }

    @Override
    public void onStart() {
        mLifecycle.onStart();
        updatePreferenceStates();
    }

    @Override
    public void onResume() {
        mLifecycle.onResume();
    }

    @Override
    public void onPause() {
        mLifecycle.onPause();
    }

    @Override
    public void onStop() {
        mLifecycle.onStop();
    }

    @Override
    public void onDestroy() {
        mLifecycle.onDestroy();
    }

    @Override
    public void onPreferenceTreeClick(String key, boolean status) {
        Collection<AbstractPreferenceController> controllers =
                new ArrayList<>(mPreferenceControllers);
        for (AbstractPreferenceController controller : controllers) {
            if (keyEquals(key, controller.getPreferenceKey())) {
                controller.handlePreferenceTreeClick(
                        mPreferenceCompatManager.getOrCreatePrefCompat(key), status);
            }
        }
    }

    @Override
    public void onPreferenceChange(String key, Object newValue) {
        // no-op
    }

    @Override
    public abstract int getStateIdentifier();

    private void refreshAllPreferences() {
        Collection<AbstractPreferenceController> controllers =
                new ArrayList<>(mPreferenceControllers);
        for (AbstractPreferenceController controller : controllers) {
            controller.displayPreference(mPreferenceCompatManager);
        }
    }

    /**
     * Update state of each preference managed by PreferenceController.
     */
    protected void updatePreferenceStates() {
        Collection<AbstractPreferenceController> controllers =
                new ArrayList<>(mPreferenceControllers);
        for (AbstractPreferenceController controller : controllers) {
            if (!controller.isAvailable()) {
                continue;
            }
            final String[] key = controller.getPreferenceKey();

            final PreferenceCompat preference = mPreferenceCompatManager.getOrCreatePrefCompat(key);
            if (preference == null) {
                Log.d(TAG, "Cannot find preference with key " + key
                        + " in Controller " + controller.getClass().getSimpleName());
                continue;
            }
            controller.updateState(preference);
        }
    }

    /**
     * Get a list of {@link AbstractPreferenceController} for this fragment.
     */
    protected abstract List<AbstractPreferenceController> onCreatePreferenceControllers(
            Context context);

    protected Lifecycle getLifecycle() {
        return mLifecycle;
    }

    private boolean keyEquals(String key1, String[] key2) {
        return key2 != null && key2.length == 1 && key1 != null && key1.equals(key2[0]);
    }
}
