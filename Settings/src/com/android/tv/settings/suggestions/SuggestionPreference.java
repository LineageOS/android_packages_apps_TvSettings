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

package com.android.tv.settings.suggestions;

import android.app.PendingIntent;
import android.content.Context;
import android.service.settings.suggestions.Suggestion;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.Log;
import android.view.View;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.suggestions.SuggestionControllerMixin;

/**
 * Custom preference for Suggestions.
 */
public class SuggestionPreference extends Preference {
    private static final String TAG = "SuggestionPreference";
    public static final String SUGGESTION_PREFERENCE_KEY = "suggestion_pref_key";

    private final MetricsFeatureProvider mMetricsFeatureProvider =
            new MetricsFeatureProvider();

    private final Suggestion mSuggestion;
    private final SuggestionControllerMixin mSuggestionControllerMixin;
    private String mId;

    public SuggestionPreference(Suggestion suggestion, Context context,
                SuggestionControllerMixin suggestionControllerMixin) {
        super(context);
        this.mSuggestionControllerMixin = suggestionControllerMixin;
        this.mSuggestion = suggestion;
        this.mId = suggestion.getId();
        setKey(SUGGESTION_PREFERENCE_KEY + mId);
    }

    public String getId() {
        return mId;
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mSuggestion.getPendingIntent().send();
                    mSuggestionControllerMixin.launchSuggestion(mSuggestion);
                    mMetricsFeatureProvider.action(getContext(),
                            MetricsEvent.ACTION_SETTINGS_SUGGESTION, mId);
                } catch (PendingIntent.CanceledException e) {
                    Log.w(TAG, "Failed to start suggestion " + mSuggestion.getTitle());
                }
            }
        });
        mMetricsFeatureProvider.action(getContext(), MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                mId);
    }
}
