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

package com.android.tv.settings.device.apps.specialaccess;

import static com.android.tv.settings.overlay.FlavorUtils.FLAVOR_CLASSIC;
import static com.android.tv.settings.overlay.FlavorUtils.FLAVOR_TWO_PANEL;
import static com.android.tv.settings.overlay.FlavorUtils.FLAVOR_VENDOR;
import static com.android.tv.settings.overlay.FlavorUtils.FLAVOR_X;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.tvsettings.TvSettingsEnums;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.TwoStatePreference;

import com.android.settingslib.applications.ApplicationsState;
import com.android.tv.settings.R;
import com.android.tv.settings.overlay.FlavorUtils;
import com.android.tv.settings.widget.SwitchWithSoundPreference;
import com.android.tv.settings.widget.TvIconDrawableFactory;

/**
 * Settings screen for managing "Alarms & Reminders" permission
 */
@Keep
public class AlarmsAndReminders extends ManageAppOp {

    private AppOpsManager mAppOpsManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppOpsManager = getContext().getSystemService(AppOpsManager.class);
    }

    @Override
    public int getAppOpsOpCode() {
        return AppOpsManager.OP_SCHEDULE_EXACT_ALARM;
    }

    @Override
    public String getPermission() {
        return Manifest.permission.SCHEDULE_EXACT_ALARM;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(getPreferenceScreenResId(), null);
    }

    @NonNull
    @Override
    public Preference bindPreference(@NonNull Preference preference,
            ApplicationsState.AppEntry entry) {
        final TwoStatePreference switchPref = (SwitchWithSoundPreference) preference;
        Drawable icon = TvIconDrawableFactory
            .newInstance(getPreferenceManager().getContext())
            .maybeGetRoundAppIcon(entry.info);
        switchPref.setTitle(entry.label);
        switchPref.setKey(entry.info.packageName);
        switchPref.setIcon(icon);
        switchPref.setOnPreferenceChangeListener((pref, newValue) -> {
            findEntriesUsingPackageName(entry.info.packageName)
                    .forEach(packageEntry -> setAlarmsAndRemindersAccess(
                            packageEntry, (Boolean) newValue));
            return true;
        });

        switchPref.setSummary(getPreferenceSummary(entry));
        switchPref.setChecked(((PermissionState) entry.extraInfo).isAllowed());

        return switchPref;
    }

    @NonNull
    @Override
    public Preference createAppPreference() {
        return new SwitchWithSoundPreference(getPreferenceManager().getContext());
    }

    @NonNull
    @Override
    public PreferenceGroup getAppPreferenceGroup() {
        return getPreferenceScreen();
    }

    private void setAlarmsAndRemindersAccess(ApplicationsState.AppEntry entry, boolean grant) {
        mAppOpsManager.setMode(getAppOpsOpCode(),
                entry.info.uid, entry.info.packageName,
                grant ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_ERRORED);
        updateAppList();
    }

    private CharSequence getPreferenceSummary(ApplicationsState.AppEntry entry) {
        if (entry.extraInfo instanceof PermissionState) {
            return getContext().getText(((PermissionState) entry.extraInfo).isAllowed()
                    ? R.string.app_permission_summary_allowed
                    : R.string.app_permission_summary_not_allowed);
        } else {
            return null;
        }
    }

    @Override
    protected int getPageId() {
        return TvSettingsEnums.APPS_SPECIAL_APP_ACCESS_ALARMS_AND_REMINDERS;
    }

    private int getPreferenceScreenResId() {
        switch (FlavorUtils.getFlavor(getContext())) {
            case FLAVOR_CLASSIC:
            case FLAVOR_TWO_PANEL:
                return R.xml.alarms_and_reminders;
            case FLAVOR_X:
            case FLAVOR_VENDOR:
                return R.xml.alarms_and_reminders_x;
            default:
                return R.xml.alarms_and_reminders;
        }
    }
}
