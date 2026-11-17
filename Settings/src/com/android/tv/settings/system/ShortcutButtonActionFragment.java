/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.tv.settings.system;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.tv.settings.R;
import com.android.tv.settings.RadioPreference;
import com.android.tv.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Picks the action of a remote shortcut key: its default, nothing, another key or an app.
 */
@Keep
public class ShortcutButtonActionFragment extends SettingsPreferenceFragment {
    static final String ARG_KEY_CODE = "key_code";

    private static final String RADIO_GROUP = "shortcut_button_action";
    private static final String KEY_DEFAULT = "default";

    private ShortcutButtons.Key mKey;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getPreferenceManager().getContext();
        mKey = ShortcutButtons.getKey(context, getArguments().getInt(ARG_KEY_CODE));
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
        if (mKey == null) {
            setPreferenceScreen(screen);
            return;
        }
        screen.setTitle(getString(R.string.shortcut_button_title, mKey.name));

        String current = ShortcutButtons.getActions(context).get(mKey.keyCode);
        addRadio(screen, KEY_DEFAULT, ShortcutButtons.describe(context, mKey, null), current);
        addRadio(screen, ShortcutButtons.ACTION_NONE,
                getString(R.string.shortcut_button_none), current);

        PreferenceCategory keys = addCategory(screen, R.string.shortcut_button_send_key);
        for (int i = 0; i < ShortcutButtons.KEYS.length; i++) {
            addRadio(keys, ShortcutButtons.ACTION_KEY + ShortcutButtons.KEYS[i],
                    getString(ShortcutButtons.KEY_NAMES[i]), current);
        }

        PreferenceCategory apps = addCategory(screen, R.string.shortcut_button_open_app);
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> activities = new ArrayList<>(pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0));
        activities.sort(Comparator.comparing(info -> info.loadLabel(pm).toString(),
                String.CASE_INSENSITIVE_ORDER));
        List<String> packages = new ArrayList<>();
        for (ResolveInfo info : activities) {
            String packageName = info.activityInfo.packageName;
            if (packages.contains(packageName)) {
                continue;
            }
            packages.add(packageName);
            RadioPreference preference = addRadio(apps, ShortcutButtons.ACTION_APP + packageName,
                    info.loadLabel(pm), current);
            preference.setIcon(info.loadIcon(pm));
        }
        setPreferenceScreen(screen);
    }

    private PreferenceCategory addCategory(PreferenceScreen screen, int title) {
        PreferenceCategory category = new PreferenceCategory(screen.getContext());
        category.setTitle(title);
        screen.addPreference(category);
        return category;
    }

    private RadioPreference addRadio(PreferenceGroup group, String key, CharSequence title,
            String current) {
        RadioPreference preference = new RadioPreference(group.getContext());
        preference.setKey(key);
        preference.setPersistent(false);
        preference.setTitle(title);
        preference.setRadioGroup(RADIO_GROUP);
        preference.setLayoutResource(R.layout.preference_reversed_widget);
        preference.setChecked(Objects.equals(key, current == null ? KEY_DEFAULT : current));
        group.addPreference(preference);
        return preference;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof RadioPreference radio && mKey != null) {
            String key = radio.getKey();
            ShortcutButtons.setAction(getContext(), mKey.keyCode,
                    KEY_DEFAULT.equals(key) ? null : key);
            radio.setChecked(true);
            uncheckOthers(getPreferenceScreen(), radio);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    /** RadioPreference#clearOtherRadioPreferences only looks at one group, not categories. */
    private static void uncheckOthers(PreferenceGroup group, RadioPreference checked) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference preference = group.getPreference(i);
            if (preference instanceof PreferenceGroup child) {
                uncheckOthers(child, checked);
            } else if (preference instanceof RadioPreference radio && radio != checked) {
                radio.setChecked(false);
            }
        }
    }
}
