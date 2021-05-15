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

package com.android.tv.settings.library;

import android.annotation.SystemApi;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.ArrayMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *  @hide
 *  Hold the data of a Settings Preference.
 */
@SystemApi
public class PreferenceCompat {
    public static final byte TYPE_RPEFERENCE = 0;
    public static final byte TYPE_PREFERENCE_CATEGORY = 1;
    public static final byte TYPE_PREFERENCE_ACCESS_POINT = 2;
    public static final byte TYPE_PREFERENCE_WIFI_COLLAPSE_CATEGORY = 3;

    private final String[] mKey;
    private String mTitle;
    private String mSummary;
    private String mContentDescription;
    private Bundle mExtras;
    private Intent mIntent;
    private Drawable mIcon;

    // 0 : preference, 1 : preferenceCategory, 2 : AccessPointPreference
    private byte mType;

    // Provide extra information for particular type
    private Map<String, String> mInfoMap;

    // 0 : not updated, 1 : unchecked, 2 : checked
    private byte mChecked;

    // 0 : not updated, 1 : ininvisble, 2: visible
    private byte mVisible;
    private List<PreferenceCompat> mChildPrefCompats;

    /** @hide */
    @SystemApi
    public void setChildPrefCompats(
            List<PreferenceCompat> childPrefCompats) {
        this.mChildPrefCompats = childPrefCompats;
    }

    /** @hide */
    @SystemApi
    public List<PreferenceCompat> getChildPrefCompats() {
        if (mChildPrefCompats == null) {
            mChildPrefCompats = new ArrayList<>();
        }
        return mChildPrefCompats;
    }

    /** @hide */
    @SystemApi
    public int getChildPrefsCount() {
        return mChildPrefCompats == null ? 0 : mChildPrefCompats.size();
    }

    /** @hide */
    @SystemApi
    public void clearChildPrefCompats() {
        mChildPrefCompats = new ArrayList<>();
    }

    /** @hide */
    @SystemApi
    public PreferenceCompat findChildPreferenceCompat(String[] prefKey) {
        if (prefKey == null || prefKey.length != this.mKey.length + 1) {
            return null;
        }
        if (IntStream.range(0, mKey.length).anyMatch(i -> !(mKey[i].equals(prefKey[i])))) {
            return null;
        }
        if (mChildPrefCompats != null) {
            return mChildPrefCompats.stream()
                    .filter(preferenceParcelable ->
                            preferenceParcelable.getKey()[preferenceParcelable.getKey().length - 1]
                                    .equals(prefKey[prefKey.length - 1]))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    /** @hide */
    @SystemApi
    public PreferenceCompat(String key) {
        this.mKey = new String[]{key};
    }

    /** @hide */
    @SystemApi
    public PreferenceCompat(String[] key) {
        this.mKey = key;
    }

    /** @hide */
    @SystemApi
    public PreferenceCompat(String[] key, String title) {
        this.mKey = key;
        this.mTitle = title;
    }

    /** @hide */
    @SystemApi
    public PreferenceCompat(String[] key, String title, String summary) {
        this(key, title);
        this.mSummary = summary;
    }

    /** @hide */
    @SystemApi
    public String[] getKey() {
        return mKey;
    }

    /** @hide */
    @SystemApi
    public String getTitle() {
        return mTitle;
    }

    /** @hide */
    @SystemApi
    public void setTitle(String title) {
        this.mTitle = title;
    }

    /** @hide */
    @SystemApi
    public String getSummary() {
        return mSummary;
    }

    /** @hide */
    @SystemApi
    public void setSummary(String summary) {
        this.mSummary = summary;
    }

    /** @hide */
    @SystemApi
    public Drawable getIcon() {
        return mIcon;
    }

    /** @hide */
    @SystemApi
    public void setIcon(Drawable icon) {
        this.mIcon = icon;
    }

    /** @hide */
    @SystemApi
    public String getContentDescription() {
        return mContentDescription;
    }

    /** @hide */
    @SystemApi
    public int getType() {
        return mType;
    }

    /** @hide */
    @SystemApi
    public void setType(byte type) {
        this.mType = type;
    }

    /** @hide */
    @SystemApi
    public Map<String, String> getInfoMap() {
        if (mInfoMap == null) {
            mInfoMap = new ArrayMap<>();
        }
        return mInfoMap;
    }

    /** @hide */
    @SystemApi
    public void setInfoMap(Map<String, String> info) {
        this.mInfoMap = info;
    }

    /** @hide */
    @SystemApi
    public void addInfo(String key, String value) {
        if (mInfoMap == null) {
            mInfoMap = new ArrayMap<>();
        }
        mInfoMap.put(key, value);
    }

    /** @hide */
    @SystemApi
    public String getInfo(String key) {
        if (mInfoMap == null || !mInfoMap.containsKey(key)) {
            return null;
        }
        return mInfoMap.get(key);
    }

    /** @hide */
    @SystemApi
    public void setContentDescription(String contentDescription) {
        this.mContentDescription = contentDescription;
    }

    /** @hide */
    @SystemApi
    public byte getChecked() {
        return mChecked;
    }

    /** @hide */
    @SystemApi
    public void setChecked(byte checked) {
        this.mChecked = checked;
    }

    /** @hide */
    @SystemApi
    public void setChecked(boolean checked) {
        setChecked(ManagerUtil.getChecked(checked));
    }

    /** @hide */
    @SystemApi
    public void setVisible(boolean visible) {
        setVisible(ManagerUtil.getVisible(visible));
    }

    /** @hide */
    @SystemApi
    public byte getVisible() {
        return mVisible;
    }

    /** @hide */
    @SystemApi
    public void setVisible(byte visible) {
        this.mVisible = visible;
    }

    /** @hide */
    @SystemApi
    public Bundle getExtras() {
        return mExtras;
    }

    /** @hide */
    @SystemApi
    public void setExtras(Bundle extras) {
        this.mExtras = extras;
    }

    /** @hide */
    @SystemApi
    public Intent getIntent() {
        return mIntent;
    }

    /** @hide */
    @SystemApi
    public void setIntent(Intent intent) {
        this.mIntent = intent;
    }

    /** @hide */
    @SystemApi
    public void initChildPreferences() {
        mChildPrefCompats = new ArrayList<>();
    }

    /** @hide */
    @SystemApi
    public void addChildPrefCompat(PreferenceCompat childPrefCompat) {
        if (mChildPrefCompats == null) {
            mChildPrefCompats = new ArrayList<>();
        }
        mChildPrefCompats.add(childPrefCompat);
    }

    @Override
    public String toString() {
        return "PreferenceParcelable{"
                + "key='" + Arrays.toString(mKey) + '\''
                + ", title='" + mTitle + '\''
                + ", summary='" + mSummary + '\''
                + ", contentDescription='" + mContentDescription + '\''
                + ", type=" + mType
                + ", extras=" + mExtras
                + ", intent=" + mIntent
                + ", infoMap=" + mInfoMap
                + ", checked=" + mChecked
                + ", visible=" + mVisible
                + ", childPrefCompats=" + mChildPrefCompats
                + '}';
    }

    /** @hide */
    @SystemApi
    public PreferenceCompat immutableCopy() {
        PreferenceCompat copy = new PreferenceCompat(Arrays.copyOf(mKey, mKey.length));
        copy.setTitle(mTitle);
        copy.setSummary(mSummary);
        copy.setType(mType);
        copy.setChecked(mChecked);
        copy.setVisible(mVisible);
        copy.setContentDescription(mContentDescription);
        if (mExtras != null) {
            copy.setExtras(new Bundle(mExtras));
        }
        if (mIntent != null) {
            copy.setIntent(new Intent(mIntent));
        }
        Map<String, String> infoMapCopy = new ArrayMap<>();
        if (mInfoMap != null) {
            infoMapCopy.putAll(mInfoMap);
        }
        copy.setInfoMap(infoMapCopy);
        if (mChildPrefCompats != null) {
            copy.setChildPrefCompats(mChildPrefCompats.stream()
                    .map(PreferenceCompat::immutableCopy).collect(Collectors.toList()));
        }
        return copy;
    }
}
