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

import android.util.ArrayMap;

import com.android.tv.settings.library.PreferenceCompat;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Manage the creation and removal of {@link PreferenceCompat} for a state. */
public class PreferenceCompatManager {
    private final Map<String, PreferenceCompat> mPrefCompats = new ArrayMap<>();

    public void addPrefCompat(PreferenceCompat preferenceCompat) {
        mPrefCompats.put(getKey(preferenceCompat.getKey()), preferenceCompat);
    }

    public PreferenceCompat getOrCreatePrefCompat(String key) {
        return getOrCreatePrefCompat(new String[]{key});
    }

    public PreferenceCompat getOrCreatePrefCompat(String[] key) {
        String compoundKey = getKey(key);
        if (!mPrefCompats.containsKey(compoundKey)) {
            mPrefCompats.put(compoundKey, new PreferenceCompat(key));
        }
        return mPrefCompats.get(compoundKey);
    }

    private static String getKey(String[] key) {
        return Stream.of(key).collect(Collectors.joining(" "));
    }

    public static Boolean getBoolean(PreferenceCompat preferenceCompat, String infoKey) {
        return Boolean.parseBoolean(preferenceCompat.getInfo(infoKey));
    }
}
