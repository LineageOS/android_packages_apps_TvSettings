/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.tv.settings.system;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.UserHandle;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.android.tv.settings.R;

import lineageos.providers.LineageSettings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Android TV remote shortcut keys, and the actions the user set for them in
 * {@link LineageSettings.System#ATV_SHORTCUT_KEY_ACTIONS}, which the LineageParts key handler
 * applies.
 */
final class ShortcutButtons {
    static final String ACTION_NONE = "none";
    static final String ACTION_KEY = "key:";
    static final String ACTION_APP = "app:";

    /** Keys that can be sent instead of a shortcut key, with their names. */
    static final int[] KEYS = {
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        KeyEvent.KEYCODE_MEDIA_STOP,
        KeyEvent.KEYCODE_MEDIA_NEXT,
        KeyEvent.KEYCODE_MEDIA_PREVIOUS,
        KeyEvent.KEYCODE_MEDIA_REWIND,
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
        KeyEvent.KEYCODE_VOLUME_MUTE,
        KeyEvent.KEYCODE_ASSIST,
        KeyEvent.KEYCODE_HOME,
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_MENU,
        KeyEvent.KEYCODE_GUIDE,
        KeyEvent.KEYCODE_CAPTIONS,
    };
    static final int[] KEY_NAMES = {
        R.string.shortcut_button_key_play_pause,
        R.string.shortcut_button_key_stop,
        R.string.shortcut_button_key_next,
        R.string.shortcut_button_key_previous,
        R.string.shortcut_button_key_rewind,
        R.string.shortcut_button_key_fast_forward,
        R.string.shortcut_button_key_mute,
        R.string.shortcut_button_key_assist,
        R.string.shortcut_button_key_home,
        R.string.shortcut_button_key_back,
        R.string.shortcut_button_key_menu,
        R.string.shortcut_button_key_guide,
        R.string.shortcut_button_key_captions,
    };

    /** A shortcut key with its name and default target, from the lineage-sdk resources. */
    static final class Key {
        final int keyCode;
        final String name;
        final String target;

        Key(int keyCode, String name, String target) {
            this.keyCode = keyCode;
            this.name = name;
            this.target = target;
        }
    }

    private ShortcutButtons() {}

    static List<Key> getKeys(Context context) {
        Resources res = context.getResources();
        int[] keyCodes = res.getIntArray(
                org.lineageos.platform.internal.R.array.keyhandler_keycodes);
        String[] targets = res.getStringArray(
                org.lineageos.platform.internal.R.array.keyhandler_packages);
        String[] names = res.getStringArray(
                org.lineageos.platform.internal.R.array.keyhandler_names);
        List<Key> keys = new ArrayList<>();
        for (int i = 0; i < keyCodes.length; i++) {
            String name = i < names.length ? names[i] : KeyEvent.keyCodeToString(keyCodes[i]);
            keys.add(new Key(keyCodes[i], name, targets[i]));
        }
        return keys;
    }

    static Key getKey(Context context, int keyCode) {
        for (Key key : getKeys(context)) {
            if (key.keyCode == keyCode) {
                return key;
            }
        }
        return null;
    }

    /** Returns the user action of each key that has one. */
    static Map<Integer, String> getActions(Context context) {
        Map<Integer, String> actions = new LinkedHashMap<>();
        String value = LineageSettings.System.getStringForUser(context.getContentResolver(),
                LineageSettings.System.ATV_SHORTCUT_KEY_ACTIONS, UserHandle.USER_CURRENT);
        if (!TextUtils.isEmpty(value)) {
            for (String entry : value.split(",")) {
                String[] parts = entry.split(":", 2);
                if (parts.length == 2 && TextUtils.isDigitsOnly(parts[0])) {
                    actions.put(Integer.parseInt(parts[0]), parts[1]);
                }
            }
        }
        return actions;
    }

    /** Sets the user action of keyCode, null restoring its default. */
    static void setAction(Context context, int keyCode, String action) {
        Map<Integer, String> actions = getActions(context);
        if (action == null) {
            actions.remove(keyCode);
        } else {
            actions.put(keyCode, action);
        }
        List<String> entries = new ArrayList<>();
        actions.forEach((key, value) -> entries.add(key + ":" + value));
        LineageSettings.System.putStringForUser(context.getContentResolver(),
                LineageSettings.System.ATV_SHORTCUT_KEY_ACTIONS,
                entries.isEmpty() ? null : TextUtils.join(",", entries),
                UserHandle.USER_CURRENT);
    }

    /** Describes action, null being the default action of key. */
    static CharSequence describe(Context context, Key key, String action) {
        if (action == null) {
            return context.getString(isTargetAvailable(context, key.target)
                    ? R.string.shortcut_button_default : R.string.shortcut_button_default_missing,
                    key.name);
        } else if (action.equals(ACTION_NONE)) {
            return context.getString(R.string.shortcut_button_none);
        } else if (action.startsWith(ACTION_KEY)) {
            int keyCode = Integer.parseInt(action.substring(ACTION_KEY.length()));
            for (int i = 0; i < KEYS.length; i++) {
                if (KEYS[i] == keyCode) {
                    return context.getString(KEY_NAMES[i]);
                }
            }
            return KeyEvent.keyCodeToString(keyCode);
        } else if (action.startsWith(ACTION_APP)) {
            String target = action.substring(ACTION_APP.length());
            ComponentName component = ComponentName.unflattenFromString(target);
            String packageName = component != null ? component.getPackageName() : target;
            try {
                PackageManager pm = context.getPackageManager();
                return pm.getApplicationInfo(packageName, 0).loadLabel(pm);
            } catch (PackageManager.NameNotFoundException e) {
                return packageName;
            }
        }
        return action;
    }

    /** Whether the key handler can launch target: a package, component or intent action. */
    static boolean isTargetAvailable(Context context, String target) {
        PackageManager pm = context.getPackageManager();
        ComponentName component = ComponentName.unflattenFromString(target);
        String packageName = component != null ? component.getPackageName() : target;
        if (pm.getLaunchIntentForPackage(packageName) != null
                || pm.getLeanbackLaunchIntentForPackage(packageName) != null) {
            return true;
        }
        return component == null && new Intent(target).resolveActivity(pm) != null;
    }
}
