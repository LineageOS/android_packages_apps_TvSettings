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

package com.android.tv.settings.library.util;

import android.content.Context;

import com.android.tv.settings.library.PreferenceCompat;
import com.android.tv.settings.library.UIUpdateCallback;
import com.android.tv.settings.library.data.PreferenceCompatManager;


/**
 * A controller that manages event for preference.
 */
public abstract class AbstractPreferenceController {

    private static final String TAG = "AbstractPrefController";

    protected final Context mContext;
    protected final UIUpdateCallback mUIUpdateCallback;
    protected final int mStateIdentifier;

    public AbstractPreferenceController(Context context, UIUpdateCallback callback,
            int stateIdentifier) {
        mUIUpdateCallback = callback;
        mContext = context;
        mStateIdentifier = stateIdentifier;
    }

    /**
     * Displays preference in this controller.
     */
    public void displayPreference(PreferenceCompatManager manager) {
        final String[] prefKey = getPreferenceKey();
        if (prefKey == null) {
            return;
        }
        /* visible */
        setVisible(manager, prefKey, isAvailable() /* visible */);
    }

    /**
     * Updates the current status of preference (summary, switch state, etc)
     */
    public void updateState(PreferenceCompat preference) {
        refreshSummary(preference);
    }

    /**
     * Refresh preference summary with getSummary()
     */
    protected void refreshSummary(PreferenceCompat preference) {
        if (preference == null) {
            return;
        }
        final CharSequence summary = getSummary();
        if (summary == null) {
            // Default getSummary returns null. If subclass didn't override this, there is nothing
            // we need to do.
            return;
        }
        preference.setSummary(summary.toString());
    }

    /**
     * Returns true if preference is available (should be displayed)
     */
    public abstract boolean isAvailable();

    /**
     * Handle preference click.
     * @param preference preference that is clicked
     * @param status status of new state
     * @return whether the click is handled
     */
    public boolean handlePreferenceTreeClick(PreferenceCompat preference, boolean status) {
        return false;
    }

    /**
     * Returns the key for this preference.
     */
    public abstract String[] getPreferenceKey();

    /**
     * Show/hide a preference.
     */
    protected final void setVisible(PreferenceCompatManager manager, String[] prefKey,
            boolean visible) {
        manager.getOrCreatePrefCompat(prefKey).setVisible(visible);
    }

    /**
     * @return a {@link CharSequence} for the summary of the preference.
     */
    public CharSequence getSummary() {
        return null;
    }
}
