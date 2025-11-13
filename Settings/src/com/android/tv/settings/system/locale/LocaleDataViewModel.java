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
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.ViewModel;

import com.android.internal.app.LocaleStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.android.tv.settings.R;
import com.android.tv.settings.customization.CustomizationContentProvider;

/**
 * ViewModel to provide data for locale selection.
 */
public class LocaleDataViewModel extends ViewModel {
    static final boolean TRANSLATED_ONLY = false;
    private static final String TAG = "LocaleDataViewModel";
    @VisibleForTesting
    final Map<LocaleStore.LocaleInfo, List<LocaleStore.LocaleInfo>> mLocaleMap =
            new HashMap<>();
    Set<LocaleStore.LocaleInfo> mLocaleInfos;
    Set<String> mUnsupportedLocales;

    public static Locale getCurrentLocale() {
        try {
            return ActivityManager.getService().getConfiguration()
                    .getLocales().get(0);
        } catch (RemoteException e) {
            Log.e(TAG, "Could not retrieve locale", e);
            return null;
        }
    }

    public synchronized Set<LocaleStore.LocaleInfo> getLocaleInfos(
            Context context) {
        if (mLocaleInfos == null) {
            mLocaleInfos = LocaleStore.getLevelLocales(context,
                    /* ignorables= */ Collections.emptySet(),
                    /* parent= */ null,
                    TRANSLATED_ONLY);
        }
        return mLocaleInfos;
    }

    public synchronized void addLocaleInfoList(LocaleStore.LocaleInfo localeInfo, Context context) {
        if (mLocaleMap.containsKey(localeInfo)) {
            return;
        }

        if (mUnsupportedLocales == null) {
            String[] unsupportedLocales = context.getResources().getStringArray(
                    R.array.config_unsupported_locales);
            mUnsupportedLocales = new HashSet<>(Arrays.asList(unsupportedLocales));
        }

        android.content.SharedPreferences sharedPreferences =
                CustomizationContentProvider.Companion.getCustomizationSharedPreferences(
                        context);
        String countryListStr = sharedPreferences.getString("country_list", "");
        Set<String> countryList = new HashSet<>();
        if (!countryListStr.isEmpty()) {
            countryList.addAll(Arrays.asList(countryListStr.toLowerCase().split(",")));
        }

        ArrayList<LocaleStore.LocaleInfo> localeInfoWithCountryList = new ArrayList<>();
        for (LocaleStore.LocaleInfo locale : LocaleStore.getLevelLocales(
                context, Collections.emptySet(), localeInfo, TRANSLATED_ONLY)) {
            if (!mUnsupportedLocales.contains(locale.getId())) {
                if (countryList.isEmpty() || countryList.contains(
                        locale.getLocale().getCountry().toLowerCase())) {
                    localeInfoWithCountryList.add(locale);
                }
            }
        }

        mLocaleMap.put(localeInfo, localeInfoWithCountryList);
    }

    public synchronized List<LocaleStore.LocaleInfo>
    getLocaleInfoList(LocaleStore.LocaleInfo localeInfo) {
        return mLocaleMap.get(localeInfo);
    }
}