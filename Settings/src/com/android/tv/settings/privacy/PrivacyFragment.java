/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.tv.settings.privacy;

import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.settings.overlay.FeatureFactory;
import com.android.tv.settings.util.SliceUtils;
import com.android.tv.twopanelsettings.slices.SlicePreference;

/**
 * The Privacy policies screen in Settings.
 */
@Keep
public class PrivacyFragment extends SettingsPreferenceFragment {

    private static final String KEY_ACCOUNT_SETTINGS_CATEGORY = "accountSettings";
    private static final String KEY_ASSISTANT = "assistant";
    private static final String KEY_PURCHASES = "purchases";

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        setPreferencesFromResource(R.xml.privacy, null);
        findPreference(KEY_ACCOUNT_SETTINGS_CATEGORY).setVisible(false);
        if (FeatureFactory.getFactory(getContext()).getOfflineFeatureProvider()
                .isOfflineMode(getContext())) {
            return;
        }
        Preference assistantSlicePreference = findPreference(KEY_ASSISTANT);
        Preference purchasesSlicePreference = findPreference(KEY_PURCHASES);
        if (assistantSlicePreference instanceof SlicePreference
                && SliceUtils.isSliceProviderValid(
                        getContext(), ((SlicePreference) assistantSlicePreference).getUri())) {
            assistantSlicePreference.setVisible(true);
        }
        if (purchasesSlicePreference instanceof SlicePreference
                && SliceUtils.isSliceProviderValid(
                        getContext(), ((SlicePreference) purchasesSlicePreference).getUri())) {
            purchasesSlicePreference.setVisible(true);
        }
        if (assistantSlicePreference.isVisible() && purchasesSlicePreference.isVisible()) {
            findPreference(KEY_ACCOUNT_SETTINGS_CATEGORY).setVisible(true);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.PRIVACY;
    }
}
