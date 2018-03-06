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

package com.android.tv.settings.device;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.annotation.Keep;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import com.android.tv.settings.MainFragment;
import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.settings.app.AutofillHelper;
import com.android.tv.settings.device.sound.SoundFragment;
import com.android.tv.settings.system.SecurityFragment;

import java.util.List;

/**
 * The "Device Preferences" screen in TV settings.
 */
@Keep
public class DevicePrefFragment extends SettingsPreferenceFragment {
    private static final String TAG = "DeviceFragment";

    @VisibleForTesting
    static final String KEY_DEVELOPER = "developer";
    private static final String KEY_USAGE = "usageAndDiag";
    private static final String KEY_INPUTS = "inputs";
    private static final String KEY_SOUNDS = "sound_effects";
    @VisibleForTesting
    static final String KEY_CAST_SETTINGS = "cast";
    private static final String KEY_GOOGLE_SETTINGS = "google_settings";
    private static final String KEY_HOME_SETTINGS = "home";
    @VisibleForTesting
    static final String KEY_AUTOFILL = "autofill";

    private Preference mSoundsPref;
    private boolean mInputSettingNeeded;
    private PackageManagerWrapper mPm;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        if (isRestricted()) {
            setPreferencesFromResource(R.xml.device_restricted, null);
        } else {
            setPreferencesFromResource(R.xml.device, null);
        }
        mSoundsPref = findPreference(KEY_SOUNDS);
        final Preference inputPref = findPreference(KEY_INPUTS);
        if (inputPref != null) {
            inputPref.setVisible(mInputSettingNeeded);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        final TvInputManager manager = (TvInputManager) getContext().getSystemService(
                Context.TV_INPUT_SERVICE);
        if (manager != null) {
            for (final TvInputInfo input : manager.getTvInputList()) {
                if (input.isPassthroughInput()) {
                    mInputSettingNeeded = true;
                }
            }
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mPm = new PackageManagerWrapper(context.getPackageManager());
    }

    @Override
    public void onResume() {
        super.onResume();

        updateDeveloperOptions();
        updateSounds();
        updateGoogleSettings();
        updateCastSettings();
        updateAutofillSettings();
        hideIfIntentUnhandled(findPreference(KEY_HOME_SETTINGS));
        hideIfIntentUnhandled(findPreference(KEY_CAST_SETTINGS));
        hideIfIntentUnhandled(findPreference(KEY_USAGE));
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SETTINGS_TV_DEVICE_CATEGORY;
    }

    private void hideIfIntentUnhandled(Preference preference) {
        if (preference == null || !preference.isVisible()) {
            return;
        }
        preference.setVisible(
                MainFragment.systemIntentIsHandled(getContext(), preference.getIntent()) != null);
    }

    private boolean isRestricted() {
        return SecurityFragment.isRestrictedProfileInEffect(getContext());
    }

    @VisibleForTesting
    void updateDeveloperOptions() {
        final Preference developerPref = findPreference(KEY_DEVELOPER);
        if (developerPref == null) {
            return;
        }

        developerPref.setVisible(DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(
                getContext()));
    }

    private void updateSounds() {
        if (mSoundsPref == null) {
            return;
        }

        mSoundsPref.setIcon(SoundFragment.getSoundEffectsEnabled(getContext().getContentResolver())
                ? R.drawable.ic_volume_up : R.drawable.ic_volume_off);
    }

    private void updateGoogleSettings() {
        final Preference googleSettingsPref = findPreference(KEY_GOOGLE_SETTINGS);
        if (googleSettingsPref != null) {
            final ResolveInfo info = MainFragment.systemIntentIsHandled(getContext(),
                    googleSettingsPref.getIntent());
            googleSettingsPref.setVisible(info != null);
            if (info != null && info.activityInfo != null) {
                googleSettingsPref.setIcon(
                        info.activityInfo.loadIcon(getContext().getPackageManager()));
                googleSettingsPref.setTitle(
                        info.activityInfo.loadLabel(getContext().getPackageManager()));
            }
        }
    }

    @VisibleForTesting
    void updateCastSettings() {
        final Preference castPref = findPreference(KEY_CAST_SETTINGS);
        if (castPref != null) {
            final ResolveInfo info = MainFragment.systemIntentIsHandled(
                        getContext(), castPref.getIntent());
            if (info != null) {
                try {
                    final Context targetContext = getContext()
                            .createPackageContext(info.resolvePackageName != null
                                    ? info.resolvePackageName : info.activityInfo.packageName, 0);
                    castPref.setIcon(targetContext.getDrawable(info.iconResourceId));
                } catch (Resources.NotFoundException | PackageManager.NameNotFoundException
                        | SecurityException e) {
                    Log.e(TAG, "Cast settings icon not found", e);
                }
                castPref.setTitle(info.activityInfo.loadLabel(getContext().getPackageManager()));
            }
        }
    }

    @VisibleForTesting
    void updateAutofillSettings() {
        final Preference autofillPref = findPreference(KEY_AUTOFILL);
        if (autofillPref == null) {
            return;
        }
        List<DefaultAppInfo> candidates = AutofillHelper.getAutofillCandidates(getContext(),
                mPm, UserHandle.myUserId());
        // Hide preference if there is no service on device
        if (candidates.size() == 0) {
            autofillPref.setVisible(false);
            return;
        }
        autofillPref.setVisible(true);
        DefaultAppInfo appInfo = AutofillHelper.getCurrentAutofill(getContext(), candidates);
        if (appInfo != null) {
            autofillPref.setSummary(appInfo.loadLabel());
            autofillPref.setIcon(appInfo.loadIcon());
        } else {
            autofillPref.setSummary(null);
            autofillPref.setIcon(null);
        }
    }
}
