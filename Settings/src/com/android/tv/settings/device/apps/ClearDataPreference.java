/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.tv.settings.device.apps;

import static com.android.tv.settings.util.InstrumentationUtils.logEntrySelected;

import android.app.tvsettings.TvSettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.format.Formatter;

import androidx.annotation.NonNull;
import androidx.leanback.widget.GuidanceStylist;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.applications.ApplicationsState;
import com.android.tv.settings.R;

public class ClearDataPreference extends AppActionPreference {
    private boolean mClearingData;

    public ClearDataPreference(Context context, ApplicationsState.AppEntry entry) {
        super(context, entry);

        refresh();
        ConfirmationFragment.prepareArgs(getExtras(), mEntry.info.packageName);
        UserManager userManager = getContext().getSystemService(UserManager.class);
        if (userManager.hasUserRestriction(UserManager.DISALLOW_APPS_CONTROL)) {
            final RestrictedLockUtils.EnforcedAdmin admin =
                    RestrictedLockUtilsInternal.checkIfRestrictionEnforced(context,
                            UserManager.DISALLOW_APPS_CONTROL, UserHandle.myUserId());
            if (admin != null) {
                setDisabledByAdmin(admin);
            } else {
                setEnabled(false);
            }
        }
    }

    public void refresh() {
        setTitle(R.string.device_apps_app_management_clear_data);
        final Context context = getContext();
        setSummary(mClearingData ? context.getString(R.string.computing_size) :
                Formatter.formatFileSize(context, mEntry.dataSize + mEntry.externalDataSize));
        this.setOnPreferenceClickListener(
                preference -> {
                    logEntrySelected(TvSettingsEnums.APPS_ALL_APPS_APP_ENTRY_CLEAR_DATA);
                    return false;
                });
    }

    public void setClearingData(boolean clearingData) {
        mClearingData = clearingData;
        refresh();
    }

    @Override
    public String getFragment() {
        return ConfirmationFragment.class.getName();
    }

    public static class ConfirmationFragment extends AppActionPreference.ConfirmationFragment {
        private static final String ARG_PACKAGE_NAME = "packageName";

        private static void prepareArgs(@NonNull Bundle args, String packageName) {
            args.putString(ARG_PACKAGE_NAME, packageName);
        }

        @NonNull
        @Override
        public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            final AppManagementFragment fragment = (AppManagementFragment) getTargetFragment();
            return new GuidanceStylist.Guidance(
                    getString(R.string.device_apps_app_management_clear_data),
                    getString(R.string.device_apps_app_management_clear_data_desc),
                    fragment.getAppName(),
                    fragment.getAppIcon());
        }

        @Override
        public void onOk() {
            final AppManagementFragment fragment =
                    (AppManagementFragment) getTargetFragment();
            fragment.clearData();
        }
    }
}
