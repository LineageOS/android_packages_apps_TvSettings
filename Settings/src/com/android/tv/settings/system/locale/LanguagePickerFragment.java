/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.tv.settings.system.locale;

import android.app.ActivityManager;
import android.app.tvsettings.TvSettingsEnums;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.app.LocaleHelper;
import com.android.internal.app.LocaleStore;
import com.android.tv.settings.R;
import com.android.tv.settings.RadioPreference;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.settings.system.LanguageFragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Language picker settings screen for locale selection. */
@Keep
public class LanguagePickerFragment extends SettingsPreferenceFragment {
    private static final String TAG = "LanguagePickerFragment";
    private static final String LANGUAGE_PICKER_RADIO_GROUP = "language_picker_group";
    private ArrayList<LocaleStore.LocaleInfo> mLocaleInfos;
    private LocaleDataViewModel mLocaleDataViewModel;
    private static final boolean DEBUG = Build.isDebuggable();
    static final String KEY_LOCALE_INFO = "locale_info";


    public static LanguageFragment newInstance() {
        return new LanguageFragment();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        mLocaleDataViewModel = new ViewModelProvider(getActivity()).get(LocaleDataViewModel.class);
        final Context themedContext = getPreferenceManager().getContext();
        final PreferenceScreen screen =
                getPreferenceManager().createPreferenceScreen(themedContext);
        screen.setTitle(R.string.system_language);

        Locale currentLocale = mLocaleDataViewModel.getCurrentLocale().getValue();
        mLocaleDataViewModel.getCurrentLocale().observe(this, this::updateUI);
        final Set<String> langTagsToIgnore = new HashSet<>();
        boolean translatedOnly = false;
        mLocaleInfos = new ArrayList<>(LocaleStore.getLevelLocales(
                getContext(), langTagsToIgnore, null, translatedOnly));
        final Locale sortingLocale = Locale.getDefault();
        final LocaleHelper.LocaleInfoComparator comp =
                new LocaleHelper.LocaleInfoComparator(sortingLocale, false);
        mLocaleInfos.sort(comp);
        Preference activePref = null;
        for (LocaleStore.LocaleInfo localeInfo : mLocaleInfos) {
            mLocaleDataViewModel.addLocaleInfoList(localeInfo, getActivity(), langTagsToIgnore);
            ArrayList<LocaleStore.LocaleInfo> localeInfoWithCountryList = mLocaleDataViewModel
                    .getLocaleInfoList(localeInfo);
            if (localeInfoWithCountryList != null && localeInfoWithCountryList.size() <= 1) {
                RadioPreference preference = new RadioPreference(getContext());
                preference.setTitle(localeInfo.getFullNameNative());
                if (localeInfo.getLocale().equals(currentLocale)) {
                    activePref = preference;
                    preference.setChecked(true);
                } else {
                    preference.setChecked(false);
                }
                preference.setRadioGroup(LANGUAGE_PICKER_RADIO_GROUP);
                preference.getExtras().putSerializable(KEY_LOCALE_INFO, localeInfo);
                screen.addPreference(preference);
            } else {
                Preference preference = new Preference(getContext());
                preference.setFragment(CountryPickerFragment.class.getSimpleName());
                preference.setTitle(localeInfo.getFullNameNative());
                preference.getExtras().putAll(CountryPickerFragment.prepareArgs(localeInfo));
                preference.setFragment(CountryPickerFragment.class.getName());
                screen.addPreference(preference);

            }
        }

        if (activePref != null && savedInstanceState == null) {
            scrollToPreference(activePref);
        }

        setPreferenceScreen(screen);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof RadioPreference) {
            RadioPreference localePref = (RadioPreference) preference;
            LocaleStore.LocaleInfo localeInfo = localePref.getExtras()
                    .getSerializable(KEY_LOCALE_INFO, LocaleStore.LocaleInfo.class);
            if (localeInfo != null) {
                getContext().getSystemService(ActivityManager.class)
                        .setDeviceLocales(new LocaleList(localeInfo.getLocale()));
                mLocaleDataViewModel.getCurrentLocale().setValue(localeInfo.getLocale());
            }
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void updateUI(Locale locale) {
        if (DEBUG) {
            Log.d(TAG, "Updating UI, Locale changed to:" + locale);
        }
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            Preference pref = getPreferenceScreen().getPreference(i);
            if (pref instanceof RadioPreference) {
                LocaleStore.LocaleInfo localeInfo = (LocaleStore.LocaleInfo)
                        pref.getExtras().getSerializable(KEY_LOCALE_INFO);
                if (localeInfo.getLocale() != null && localeInfo.getLocale().equals(locale)) {
                    ((RadioPreference) pref).clearOtherRadioPreferences(getPreferenceScreen());
                }
            }
        }
    }

    @Override
    protected int getPageId() {
        return TvSettingsEnums.SYSTEM_LANGUAGE;
    }
}
