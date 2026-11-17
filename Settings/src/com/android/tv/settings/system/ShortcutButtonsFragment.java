/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.tv.settings.system;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;

import java.util.Map;

/**
 * Lists the remote shortcut keys, such as streaming app buttons, with their actions.
 */
@Keep
public class ShortcutButtonsFragment extends SettingsPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getPreferenceManager().getContext();
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
        screen.setTitle(R.string.shortcut_buttons_title);
        for (ShortcutButtons.Key key : ShortcutButtons.getKeys(context)) {
            Preference preference = new Preference(context);
            preference.setKey(String.valueOf(key.keyCode));
            preference.setTitle(getString(R.string.shortcut_button_title, key.name));
            preference.setFragment(ShortcutButtonActionFragment.class.getName());
            preference.getExtras().putInt(ShortcutButtonActionFragment.ARG_KEY_CODE, key.keyCode);
            screen.addPreference(preference);
        }
        setPreferenceScreen(screen);
    }

    @Override
    public void onResume() {
        super.onResume();
        Context context = getContext();
        Map<Integer, String> actions = ShortcutButtons.getActions(context);
        for (ShortcutButtons.Key key : ShortcutButtons.getKeys(context)) {
            Preference preference = findPreference(String.valueOf(key.keyCode));
            if (preference != null) {
                preference.setSummary(
                        ShortcutButtons.describe(context, key, actions.get(key.keyCode)));
            }
        }
    }
}
