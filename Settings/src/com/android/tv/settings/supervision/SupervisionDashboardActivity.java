/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.tv.settings.supervision;

import android.app.supervision.SupervisionManager;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import java.util.List;

/**
 * An invisible activity that acts as a redirecting entry point to the active supervision settings
 * page.
 *
 * <p>This activity has no UI of its own. Its sole purpose is to determine the correct supervision
 * settings activity provided by the currently active supervision app, launch it, and then
 * immediately finish itself.
 */
public class SupervisionDashboardActivity extends FragmentActivity {

    public static final String ACTION_SHOW_PARENTAL_CONTROLS =
            "android.settings.SHOW_PARENTAL_CONTROLS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent supervisionSettingsIntent = getSupervisionSettingsIntent();
        if (supervisionSettingsIntent != null) {
            startActivity(supervisionSettingsIntent);
        }

        finish();
    }

    /**
     * Returns an [Intent] to the supervision settings page or null if supervision is disabled or
     * the intent is not resolvable.
     */
    @Nullable
    private Intent getSupervisionSettingsIntent() {
        SupervisionManager supervisionManager = getSystemService(SupervisionManager.class);
        if (supervisionManager == null) {
            return null;
        }

        String supervisionAppPackage = supervisionManager.getActiveSupervisionAppPackage();
        if (supervisionAppPackage == null) {
            return null;
        }

        Intent intent =
                new Intent(ACTION_SHOW_PARENTAL_CONTROLS)
                        .setPackage(supervisionAppPackage)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        List<ResolveInfo> activities =
                getPackageManager().queryIntentActivitiesAsUser(intent, 0, getUserId());

        return activities.isEmpty() ? null : intent;
    }
}
