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

package com.android.tv.settings;

import android.annotation.NonNull;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyIdentifiers;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DpcAuthority;
import android.app.admin.EnforcingAdmin;
import android.app.admin.PolicyEnforcementInfo;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.fragment.app.FragmentActivity;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

public class ActionDisabledByAdminDialog extends FragmentActivity
        implements DialogInterface.OnDismissListener {

    private ActionDisabledByAdminDialogHelper mDialogHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.app.admin.flags.Flags.policyTransparencyRefactorEnabled()) {
            final EnforcingAdmin enforcingAdmin = getEnforcingAdmin(getIntent());
            final String restriction = getRestrictionFromIntent(getIntent());
            mDialogHelper = new ActionDisabledByAdminDialogHelper(this);
            AlertDialog dialog = mDialogHelper.prepareDialogBuilder(restriction, enforcingAdmin)
                    .setOnDismissListener(this)
                    .show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();
        } else {
            final RestrictedLockUtils.EnforcedAdmin enforcedAdmin =
                    getAdminDetailsFromIntent(getIntent());
            final String restriction = getRestrictionFromIntent(getIntent());
            mDialogHelper = new ActionDisabledByAdminDialogHelper(this);
            AlertDialog dialog = mDialogHelper.prepareDialogBuilder(restriction, enforcedAdmin)
                    .setOnDismissListener(this)
                    .show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        final EnforcedAdmin admin = getAdminDetailsFromIntent(intent);
        final String restriction = getRestrictionFromIntent(intent);
        mDialogHelper.updateDialog(restriction, admin);
    }

    @androidx.annotation.VisibleForTesting
    EnforcedAdmin getAdminDetailsFromIntent(Intent intent) {
        final EnforcedAdmin admin = new EnforcedAdmin(null, UserHandle.of(UserHandle.myUserId()));
        if (intent == null) {
            return admin;
        }
        admin.component = intent.getParcelableExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN);
        int userId = intent.getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());

        Bundle adminDetails = null;
        if (admin.component == null) {
            DevicePolicyManager devicePolicyManager = getSystemService(DevicePolicyManager.class);
            adminDetails = devicePolicyManager.getEnforcingAdminAndUserDetails(userId,
                    getRestrictionFromIntent(intent));
            if (adminDetails != null) {
                admin.component = adminDetails.getParcelable(
                        DevicePolicyManager.EXTRA_DEVICE_ADMIN);
            }
        }

        if (intent.hasExtra(Intent.EXTRA_USER)) {
            admin.user = intent.getParcelableExtra(Intent.EXTRA_USER);
        } else {
            if (adminDetails != null) {
                userId = adminDetails.getInt(Intent.EXTRA_USER_ID, UserHandle.myUserId());
            }
            if (userId == UserHandle.USER_NULL) {
                admin.user = null;
            } else {
                admin.user = UserHandle.of(userId);
            }
        }
        return admin;
    }

    @androidx.annotation.VisibleForTesting
    EnforcingAdmin getEnforcingAdmin(Intent intent) {
        if (intent == null) {
            return null;
        }
        if (android.app.admin.flags.Flags.enforcingAdminExtraEnabled() &&
                intent.hasExtra(DevicePolicyManager.EXTRA_ENFORCING_ADMIN)) {
            return intent.getParcelableExtra(DevicePolicyManager.EXTRA_ENFORCING_ADMIN);
        }
        ComponentName componentName = intent.getParcelableExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN);
        UserHandle userHandle = getUserFromIntent(intent);
        String restriction = getRestrictionFromIntent(intent);

        if (componentName == null && restriction != null) {
            DevicePolicyManager devicePolicyManager = getSystemService(DevicePolicyManager.class);
            PolicyEnforcementInfo policyEnforcementInfo =
                    devicePolicyManager.getEnforcingAdminsForPolicy(
                            DevicePolicyIdentifiers.getIdentifierForUserRestriction(restriction),
                            userHandle.getIdentifier());
            return policyEnforcementInfo.getMostImportantEnforcingAdmin();
        }

        if (componentName != null && userHandle != null) {
            return new EnforcingAdmin(componentName.getPackageName(), DpcAuthority.DPC_AUTHORITY,
                    userHandle, componentName);
        }
        return null;
    }

    private UserHandle getUserFromIntent(@NonNull Intent intent) {
        if (intent.hasExtra(Intent.EXTRA_USER)) {
            return intent.getParcelableExtra(Intent.EXTRA_USER);
        }
        int userId = intent.getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());
        return UserHandle.of(userId);
    }

    @androidx.annotation.VisibleForTesting
    String getRestrictionFromIntent(Intent intent) {
        if (intent == null) {
            return null;
        }
        return intent.getStringExtra(DevicePolicyManager.EXTRA_RESTRICTION);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
