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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.app.supervision.SupervisionManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowContextImpl;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(AndroidJUnit4.class)
public class SupervisionDashboardActivityTest {
    public static final String TEST_SUPERVISION_PACKAGE = "com.android.settings.test";
    public static final String TEST_REDIRECT_ACTIVITY = "com.example.FakeRedirectActivity";

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Mock private SupervisionManager mSupervisionManager;

    private ShadowPackageManager mPackageManager;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mPackageManager = shadowOf(mContext.getPackageManager());
        ShadowContextImpl shadowContext = Shadow.extract(((Application) mContext).getBaseContext());
        shadowContext.setSystemService(Context.SUPERVISION_SERVICE, mSupervisionManager);
    }

    @Test
    public void noSupervisionApp_finishesActivity() {
        // Setup no supervision app
        when(mSupervisionManager.getActiveSupervisionAppPackage()).thenReturn(null);

        try (ActivityScenario<SupervisionDashboardActivity> activityScenario =
                ActivityScenario.launch(SupervisionDashboardActivity.class)) {

            // Check that no activity is started
            assertThat(shadowOf((Application) mContext).getNextStartedActivity()).isNull();

            // Check that the dashboard activity is finished
            assertThat(activityScenario.getState()).isEqualTo(Lifecycle.State.DESTROYED);
        }
    }

    @Test
    public void supervisionAppPresent_noMatchingActivity_finishesActivity() {
        // Setup supervision app to be present
        when(mSupervisionManager.getActiveSupervisionAppPackage())
                .thenReturn(TEST_SUPERVISION_PACKAGE);

        try (ActivityScenario<SupervisionDashboardActivity> activityScenario =
                ActivityScenario.launch(SupervisionDashboardActivity.class)) {

            // Check that no activity is started
            assertThat(shadowOf((Application) mContext).getNextStartedActivity()).isNull();

            // Check that the dashboard activity is finished
            assertThat(activityScenario.getState()).isEqualTo(Lifecycle.State.DESTROYED);
        }
    }

    @Test
    public void supervisionAppPresent_hasMatchingActivity_RedirectToSupervisionSettings() {
        // Setup necessary supervision component to be present
        when(mSupervisionManager.getActiveSupervisionAppPackage())
                .thenReturn(TEST_SUPERVISION_PACKAGE);
        setUpRedirectActivityComponent();

        try (ActivityScenario<SupervisionDashboardActivity> activityScenario =
                ActivityScenario.launch(SupervisionDashboardActivity.class)) {

            ShadowApplication shadowApplication = shadowOf((Application) mContext);
            Intent nextActivityIntent = shadowApplication.getNextStartedActivity();

            // Check that the redirect activity is started
            assertThat(nextActivityIntent.getAction())
                    .isEqualTo(SupervisionDashboardActivity.ACTION_SHOW_PARENTAL_CONTROLS);

            // Check that the dashboard activity is finished
            assertThat(activityScenario.getState()).isEqualTo(Lifecycle.State.DESTROYED);
        }
    }

    private void setUpRedirectActivityComponent() {
        ComponentName redirectComponentName =
                new ComponentName(TEST_SUPERVISION_PACKAGE, TEST_REDIRECT_ACTIVITY);
        IntentFilter intentFilter =
                new IntentFilter(SupervisionDashboardActivity.ACTION_SHOW_PARENTAL_CONTROLS);

        mPackageManager.addActivityIfNotPresent(redirectComponentName);
        mPackageManager.addIntentFilterForActivity(redirectComponentName, intentFilter);
    }
}
