/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.tv.settings.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.annotation.Keep;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.ArraySet;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Fragment for managing Autofill settings:
 * 1. Choose one autofill service from a list.
 * 2. Launch settings for each autofill service.
 */
@Keep
public class AutofillFragment extends SettingsPreferenceFragment {
    private static final String TAG = "AutofillFragment";

    private static final String KEY_CURRENT_AUTOFILL = "currentAutofill";

    private static final String KEY_AUTOFILL_SETTINGS_PREFIX = "autofillSettings:";

    private PackageManagerWrapper mPm;

    /**
     * @return New fragment instance
     */
    public static AutofillFragment newInstance() {
        return new AutofillFragment();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final Context preferenceContext = getPreferenceManager().getContext();

        setPreferencesFromResource(R.xml.autofill, null);

        final ListPreference currentAutofill = (ListPreference)
                findPreference(KEY_CURRENT_AUTOFILL);
        currentAutofill.setOnPreferenceChangeListener((preference, newValue) -> {
            AutofillHelper.setCurrentAutofill(getContext(), (String) newValue);
            return true;
        });

        updateUi();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mPm = new PackageManagerWrapper(context.getPackageManager());
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUi();
    }

    void updateUi() {
        List<DefaultAppInfo> candidates = getCandidatesIncludeDisable();
        final Preference currentAutofillPref = findPreference(KEY_CURRENT_AUTOFILL);
        updateCurrentAutofillPreference((ListPreference) currentAutofillPref, candidates);
        updatePrefs(candidates);
    }

    List<DefaultAppInfo> getCandidatesIncludeDisable() {
        List<DefaultAppInfo> candidates = AutofillHelper.getAutofillCandidates(getContext(),
                mPm, UserHandle.myUserId());
        DefaultAppInfo disableItem = new DefaultAppInfo(null, null, null);
        candidates.add(0, disableItem);
        return candidates;
    }

    void updateCurrentAutofillPreference(ListPreference currentAutofillPref,
            List<DefaultAppInfo> candidates) {

        DefaultAppInfo app = AutofillHelper.getCurrentAutofill(getContext(), candidates);

        final List<CharSequence> entries = new ArrayList<>(candidates.size());
        final List<CharSequence> values = new ArrayList<>(candidates.size());

        int defaultIndex = candidates.indexOf(app);
        if (defaultIndex < 0) {
            defaultIndex = 0; // disable
        }

        for (final DefaultAppInfo info : candidates) {
            if (info.componentName == null) {
                entries.add(getContext().getString(R.string.autofill_disable));
                values.add(""); // key for "disable"
            } else {
                entries.add(info.loadLabel());
                values.add(info.getKey());
            }
        }

        currentAutofillPref.setEntries(entries.toArray(new CharSequence[entries.size()]));
        currentAutofillPref.setEntryValues(values.toArray(new CharSequence[values.size()]));
        currentAutofillPref.setValueIndex(defaultIndex);
    }

    void updatePrefs(List<DefaultAppInfo> candidates) {
        final Context preferenceContext = getPreferenceManager().getContext();
        final PackageManager packageManager = getActivity().getPackageManager();

        final PreferenceScreen screen = getPreferenceScreen();

        final Set<String> autofillServicesKeys = new ArraySet<>(candidates.size());
        for (final DefaultAppInfo info : candidates) {
            final Intent settingsIntent = AutofillHelper.getAutofillSettingsIntent(getContext(),
                    mPm, info);
            if (settingsIntent == null) {
                continue;
            }
            final String key = KEY_AUTOFILL_SETTINGS_PREFIX + info.getKey();

            Preference preference = findPreference(key);
            if (preference == null) {
                preference = new Preference(preferenceContext);
                screen.addPreference(preference);
            }
            preference.setTitle(info.loadLabel());
            preference.setKey(key);
            preference.setIntent(settingsIntent);
            autofillServicesKeys.add(key);
        }

        for (int i = 0; i < screen.getPreferenceCount();) {
            final Preference preference = screen.getPreference(i);
            final String key = preference.getKey();
            if (!TextUtils.isEmpty(key)
                    && key.startsWith(KEY_AUTOFILL_SETTINGS_PREFIX)
                    && !autofillServicesKeys.contains(key)) {
                screen.removePreference(preference);
            } else {
                i++;
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DEFAULT_AUTOFILL_PICKER;
    }
}
