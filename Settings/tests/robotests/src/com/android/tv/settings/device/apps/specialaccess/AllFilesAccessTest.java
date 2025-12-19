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
public class AllFilesAccessTest {

    private AllFilesAccess mFragment;
    private AppOpsManager mAppOpsManager;
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = RuntimeEnvironment.application;
        mAppOpsManager = mock(AppOpsManager.class);
        mFragment = new AllFilesAccess();

        Field field = AllFilesAccess.class.getDeclaredField("mAppOpsManager");
        field.setAccessible(true);
        field.set(mFragment, mAppOpsManager);
    }

    @Test
    public void setMode_grant_callsSetUidModeAllowed() {
        ApplicationsState.AppEntry entry = new ApplicationsState.AppEntry(
                mContext,
                new ApplicationInfo(),
                12345);
        entry.info.packageName = "com.example.app";
        entry.info.uid = 12345;
        entry.label = "Test App";
        // entry.extraInfo not needed for setMode checks

        try {
            Method method = AllFilesAccess.class.getDeclaredMethod("setMode",
                    ApplicationsState.AppEntry.class, boolean.class);
            method.setAccessible(true);
            method.invoke(mFragment, entry, true);

            verify(mAppOpsManager).setUidMode(
                    eq(AppOpsManager.OP_MANAGE_EXTERNAL_STORAGE),
                    eq(12345),
                    eq(AppOpsManager.MODE_ALLOWED));

            method.invoke(mFragment, entry, false);
            verify(mAppOpsManager).setUidMode(
                    eq(AppOpsManager.OP_MANAGE_EXTERNAL_STORAGE),
                    eq(12345),
                    eq(AppOpsManager.MODE_ERRORED));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getAppFilter_systemUid_returnsFalse() {
        ApplicationsState.AppEntry entry = new ApplicationsState.AppEntry(
                mContext,
                new ApplicationInfo(),
                12345);
        entry.info.packageName = "com.android.localtransport";
        entry.info.uid = android.os.Process.SYSTEM_UID;
        // extraInfo needs to be set because super.filterApp checks it
        // But we expect it to return false BEFORE calling super if we implement it
        // right,
        // OR super checks it.
        // Wait, super.getAppFilter().filterApp(entry) calls createPermissionStateFor ->
        // ...
        // If we filter it out, we return false.

        // Let's ensure we return false.
        // Our implementation: if uid == SYSTEM_UID return false.

        boolean result = mFragment.getAppFilter().filterApp(entry);
        // assertThat(result).isFalse();
        // Since we don't have Truth/AssertJ easily, verify with logical check or
        // standard JUnit
        if (result) {
            throw new RuntimeException("System UID app should be filtered out");
        }
    }
}
