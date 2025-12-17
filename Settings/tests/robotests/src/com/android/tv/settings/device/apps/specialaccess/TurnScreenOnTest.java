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

package com.android.tv.settings.device.apps.specialaccess;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;

import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@RunWith(RobolectricTestRunner.class)
@Config(instrumentedPackages = "com.android.tv.settings")
public class TurnScreenOnTest {

    private TurnScreenOn mFragment;
    private AppOpsManager mAppOpsManager;
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = RuntimeEnvironment.application;
        mAppOpsManager = mock(AppOpsManager.class);
        mFragment = new TurnScreenOn();

        Field field = TurnScreenOn.class.getDeclaredField("mAppOpsManager");
        field.setAccessible(true);
        field.set(mFragment, mAppOpsManager);
    }

    @Test
    public void setTurnScreenOnMode_grant_callsSetUidModeAllowed() {
        // We need to trigger the switch change listener.
        // This requires binding the preference.
        // Construct a fake AppEntry.
        ApplicationsState.AppEntry entry = new ApplicationsState.AppEntry(
                mContext,
                new ApplicationInfo(),
                12345);
        entry.info.packageName = "com.example.app";
        entry.info.uid = 12345; // uid
        entry.label = "Test App";
        // entry.extraInfo not needed for setMode checks

        // We can't easily invoke binderPreference without mocking everything around it.
        // But we can test the private method if we make it package-private or use
        // reflection.
        // OR we can rely on the fact that we can call the method directly if we make it
        // package-private?
        // No, let's just make it package-private for testing or use reflection.
        // Actually, for these simple tests, let's use reflection to invoke
        // 'setTurnScreenOnMode'.

        try {
            Method method = TurnScreenOn.class.getDeclaredMethod("setTurnScreenOnMode",
                    ApplicationsState.AppEntry.class, boolean.class);
            method.setAccessible(true);
            method.invoke(mFragment, entry, true);

            verify(mAppOpsManager).setUidMode(
                    eq(AppOpsManager.OPSTR_TURN_SCREEN_ON),
                    eq(12345),
                    eq(AppOpsManager.MODE_ALLOWED));

            method.invoke(mFragment, entry, false);
            verify(mAppOpsManager).setUidMode(
                    eq(AppOpsManager.OPSTR_TURN_SCREEN_ON),
                    eq(12345),
                    eq(AppOpsManager.MODE_ERRORED));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
