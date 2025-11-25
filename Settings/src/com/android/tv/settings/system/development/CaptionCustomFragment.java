/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.tv.settings.system.development;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.CaptioningManager;

import androidx.annotation.Keep;
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.tv.settings.R;

@Keep
public class CaptionCustomFragment extends LeanbackPreferenceFragmentCompat implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_FONT_FAMILY = "font_family";
    private static final String KEY_TEXT_COLOR = "text_color";
    private static final String KEY_TEXT_OPACITY = "text_opacity";
    private static final String KEY_EDGE_TYPE = "edge_type";
    private static final String KEY_EDGE_COLOR = "edge_color";
    private static final String KEY_BACKGROUND_SHOW = "background_show";
    private static final String KEY_BACKGROUND_COLOR = "background_color";
    private static final String KEY_BACKGROUND_OPACITY = "background_opacity";
    private static final String KEY_WINDOW_SHOW = "window_show";
    private static final String KEY_WINDOW_COLOR = "window_color";
    private static final String KEY_WINDOW_OPACITY = "window_opacity";

    private static final int COLOR_MASK = 0x00ffffff;
    private static final int ALPHA_MASK = 0xff000000;

    // User facing color formatting, e.g. #55AA00
    private static final String HEX_COLOR_FORMAT = "#%06X";

    // Default values for the settings.
    private static final int DEFAULT_COLOR_TEXT = Color.WHITE & COLOR_MASK;
    private static final int DEFAULT_COLOR_EDGE = Color.BLACK & COLOR_MASK;
    private static final int DEFAULT_COLOR_BACKGROUND = Color.BLACK & COLOR_MASK;
    private static final int DEFAULT_COLOR_WINDOW = Color.BLACK & COLOR_MASK;

    private static final int ALPHA_100 = 0xff000000;
    private static final int ALPHA_0 = 0x00000000;
    private static final String FONT_DEFAULT = "default";

    private ListPreference mFontFamilyPref;
    private ListPreference mTextColorPref;
    private ListPreference mTextOpacityPref;
    private ListPreference mEdgeTypePref;
    private ListPreference mEdgeColorPref;
    private TwoStatePreference mBackgroundShowPref;
    private ListPreference mBackgroundColorPref;
    private ListPreference mBackgroundOpacityPref;
    private TwoStatePreference mWindowShowPref;
    private ListPreference mWindowColorPref;
    private ListPreference mWindowOpacityPref;

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.caption_custom, null);

        final TypedArray ta = getResources().obtainTypedArray(
                R.array.captioning_color_selector_ids);
        final int colorLen = ta.length();
        final String[] namedColors = getResources().getStringArray(
                R.array.captioning_color_selector_titles);
        final String[] colorNames = new String[colorLen];
        final String[] colorValues = new String[colorLen];
        for (int i = 0; i < colorLen; i++) {
            final int color = ta.getColor(i, 0);
            colorValues[i] = Integer.toHexString(color & COLOR_MASK);
            if (i < namedColors.length) {
                colorNames[i] = namedColors[i];
            } else {
                colorNames[i] = String.format(HEX_COLOR_FORMAT, color & COLOR_MASK);
            }
        }
        ta.recycle();

        mFontFamilyPref = findPreference(KEY_FONT_FAMILY);
        mFontFamilyPref.setOnPreferenceChangeListener(this);

        mTextColorPref = findPreference(KEY_TEXT_COLOR);
        mTextColorPref.setEntries(colorNames);
        mTextColorPref.setEntryValues(colorValues);
        mTextColorPref.setOnPreferenceChangeListener(this);

        mTextOpacityPref = findPreference(KEY_TEXT_OPACITY);
        mTextOpacityPref.setOnPreferenceChangeListener(this);
        mEdgeTypePref = findPreference(KEY_EDGE_TYPE);
        mEdgeTypePref.setOnPreferenceChangeListener(this);

        mEdgeColorPref = findPreference(KEY_EDGE_COLOR);
        mEdgeColorPref.setEntries(colorNames);
        mEdgeColorPref.setEntryValues(colorValues);
        mEdgeColorPref.setOnPreferenceChangeListener(this);

        mBackgroundShowPref = findPreference(KEY_BACKGROUND_SHOW);

        mBackgroundColorPref = findPreference(KEY_BACKGROUND_COLOR);
        mBackgroundColorPref.setEntries(colorNames);
        mBackgroundColorPref.setEntryValues(colorValues);
        mBackgroundColorPref.setOnPreferenceChangeListener(this);

        mBackgroundOpacityPref = findPreference(KEY_BACKGROUND_OPACITY);
        mBackgroundOpacityPref.setOnPreferenceChangeListener(this);

        mWindowShowPref = findPreference(KEY_WINDOW_SHOW);

        mWindowColorPref = findPreference(KEY_WINDOW_COLOR);
        mWindowColorPref.setEntries(colorNames);
        mWindowColorPref.setEntryValues(colorValues);
        mWindowColorPref.setOnPreferenceChangeListener(this);

        mWindowOpacityPref = findPreference(KEY_WINDOW_OPACITY);
        mWindowOpacityPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final String key = preference.getKey();
        if (TextUtils.isEmpty(key)) {
            return super.onPreferenceTreeClick(preference);
        }
        switch (key) {
            case KEY_BACKGROUND_SHOW:
                setCaptionsBackgroundVisible(((TwoStatePreference) preference).isChecked());
                return true;
            case KEY_WINDOW_SHOW:
                setCaptionsWindowVisible(((TwoStatePreference) preference).isChecked());
                return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        if (TextUtils.isEmpty(key)) {
            throw new IllegalStateException("Unknown preference change");
        }
        switch (key) {
            case KEY_FONT_FAMILY -> setCaptionsFontFamily((String) newValue);
            case KEY_TEXT_COLOR -> setCaptionsTextColor((String) newValue);
            case KEY_TEXT_OPACITY -> setCaptionsTextOpacity((String) newValue);
            case KEY_EDGE_TYPE -> setCaptionsEdgeType((String) newValue);
            case KEY_EDGE_COLOR -> setCaptionsEdgeColor((String) newValue);
            case KEY_BACKGROUND_COLOR -> setCaptionsBackgroundColor((String) newValue);
            case KEY_BACKGROUND_OPACITY -> setCaptionsBackgroundOpacity((String) newValue);
            case KEY_WINDOW_COLOR -> setCaptionsWindowColor((String) newValue);
            case KEY_WINDOW_OPACITY -> setCaptionsWindowOpacity((String) newValue);
            default -> throw new IllegalStateException("Preference change with unknown key " + key);
        }
        return true;
    }

    private void refresh() {
        mFontFamilyPref.setValue(getCaptionsFontFamily());
        mTextColorPref.setValue(getCaptionsTextColor());
        mTextOpacityPref.setValue(getCaptionsTextOpacity());
        mEdgeTypePref.setValue(getCaptionsEdgeType());
        mEdgeColorPref.setValue(getCaptionsEdgeColor());
        mBackgroundShowPref.setChecked(isCaptionsBackgroundVisible());
        mBackgroundColorPref.setValue(getCaptionsBackgroundColor());
        mBackgroundOpacityPref.setValue(getCaptionsBackgroundOpacity());
        mWindowShowPref.setChecked(isCaptionsWindowVisible());
        mWindowColorPref.setValue(getCaptionsWindowColor());
        mWindowOpacityPref.setValue(getCaptionsWindowOpacity());
    }

    private String getCaptionsFontFamily() {
        final String typeface = Settings.Secure.getString(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_TYPEFACE);
        return TextUtils.isEmpty(typeface) ? FONT_DEFAULT : typeface;
    }

    private void setCaptionsFontFamily(String fontFamily) {
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_TYPEFACE,
                TextUtils.equals(fontFamily, FONT_DEFAULT) ? null : fontFamily);
    }

    private String getCaptionsTextColor() {
        return Integer.toHexString(Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR, DEFAULT_COLOR_TEXT)
                & COLOR_MASK);
    }

    private void setCaptionsTextColor(String textColor) {
        saveColorWithExistingOpacity(Settings.Secure.ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR,
                textColor);
    }

    private String getCaptionsTextOpacity() {
        return opacityToString(Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR,
                /* default= */ ALPHA_100) & ALPHA_MASK);
    }

    private void setCaptionsTextOpacity(String textOpacity) {
        saveOpacityWithExistingColor(Settings.Secure.ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR,
                textOpacity, DEFAULT_COLOR_TEXT);
    }

    private String getCaptionsEdgeType() {
        return Integer.toString(Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_TYPE,
                /* default= */ CaptioningManager.CaptionStyle.EDGE_TYPE_NONE));
    }

    private void setCaptionsEdgeType(String edgeType) {
        Settings.Secure.putInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_TYPE, Integer.parseInt(edgeType));
    }

    private String getCaptionsEdgeColor() {
        return Integer.toHexString(Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_COLOR, DEFAULT_COLOR_EDGE)
                & COLOR_MASK);
    }

    private void setCaptionsEdgeColor(String edgeColor) {
        Settings.Secure.putInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_COLOR,
                ALPHA_100 | hexStringToInt(edgeColor));
    }

    private boolean isCaptionsBackgroundVisible() {
        return (Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_BACKGROUND_COLOR,
                /* default= */ ALPHA_100) & ALPHA_MASK) != 0;
    }

    private void setCaptionsBackgroundVisible(boolean visible) {
        int alpha = visible ? ALPHA_100 : ALPHA_0;
        Settings.Secure.putInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_BACKGROUND_COLOR,
                alpha | DEFAULT_COLOR_BACKGROUND);
        mBackgroundColorPref.setValue(Integer.toHexString(DEFAULT_COLOR_BACKGROUND));
        mBackgroundOpacityPref.setValue(opacityToString(alpha));
    }

    private String getCaptionsBackgroundColor() {
        return Integer.toHexString(Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_BACKGROUND_COLOR, DEFAULT_COLOR_BACKGROUND)
                & COLOR_MASK);
    }

    private void setCaptionsBackgroundColor(String backgroundColor) {
        saveColorWithExistingOpacity(Settings.Secure.ACCESSIBILITY_CAPTIONING_BACKGROUND_COLOR,
                backgroundColor);
    }

    private String getCaptionsBackgroundOpacity() {
        return opacityToString(Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_BACKGROUND_COLOR,
                /* default= */ ALPHA_100) & ALPHA_MASK);
    }

    private void setCaptionsBackgroundOpacity(String backgroundOpacity) {
        saveOpacityWithExistingColor(Settings.Secure.ACCESSIBILITY_CAPTIONING_BACKGROUND_COLOR,
                backgroundOpacity, DEFAULT_COLOR_BACKGROUND);
    }

    private boolean isCaptionsWindowVisible() {
        return (Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_WINDOW_COLOR,
                /* default= */ ALPHA_0) & ALPHA_MASK) != 0;
    }

    private void setCaptionsWindowVisible(boolean visible) {
        int alpha = visible ? ALPHA_100 : ALPHA_0;
        Settings.Secure.putInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_WINDOW_COLOR,
                alpha | DEFAULT_COLOR_WINDOW);
        mWindowColorPref.setValue(Integer.toHexString(DEFAULT_COLOR_WINDOW));
        mWindowOpacityPref.setValue(opacityToString(alpha));
    }

    private String getCaptionsWindowColor() {
        return Integer.toHexString(Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_WINDOW_COLOR, DEFAULT_COLOR_WINDOW)
                & COLOR_MASK);
    }

    private void setCaptionsWindowColor(String windowColor) {
        saveColorWithExistingOpacity(Settings.Secure.ACCESSIBILITY_CAPTIONING_WINDOW_COLOR,
                windowColor);
    }

    private String getCaptionsWindowOpacity() {
        return opacityToString(Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_WINDOW_COLOR,
                /* default= */ ALPHA_100) & ALPHA_MASK);
    }

    private void setCaptionsWindowOpacity(String windowOpacity) {
        saveOpacityWithExistingColor(Settings.Secure.ACCESSIBILITY_CAPTIONING_WINDOW_COLOR,
                windowOpacity, DEFAULT_COLOR_WINDOW);
    }

    private void saveColorWithExistingOpacity(String secureSetting, String colorString) {
        final int color = hexStringToInt(colorString) & COLOR_MASK;
        final int alpha = Settings.Secure.getInt(getContext().getContentResolver(),
                secureSetting, /* default= */ ALPHA_100) & ALPHA_MASK;
        Settings.Secure.putInt(getContext().getContentResolver(), secureSetting, color | alpha);
    }

    private void saveOpacityWithExistingColor(String secureSetting, String alphaString,
            int defaultColor) {
        int alpha = hexStringToInt(alphaString) & ALPHA_MASK;
        saveOpacityWithExistingColor(secureSetting, alpha, defaultColor);
    }

    private void saveOpacityWithExistingColor(String secureSetting, int alpha, int defaultColor) {
        final int color = Settings.Secure.getInt(getContext().getContentResolver(), secureSetting,
                defaultColor) & COLOR_MASK;
        Settings.Secure.putInt(getContext().getContentResolver(), secureSetting, color | alpha);
    }

    private int hexStringToInt(String value) {
        return Integer.parseUnsignedInt(value, 16);
    }

    private String opacityToString(int opacity) {
        // Never returns a zero opacity string since the ListPreference doesn't contain that option
        // and it will be reset to 100 once the toggle to show the element is enabled again.
        if (opacity == ALPHA_0) {
            return Integer.toHexString(ALPHA_100);
        }
        return Integer.toHexString(opacity);
    }
}
