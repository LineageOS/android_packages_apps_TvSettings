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
public class MediaManagementAppsTest {

  private MediaManagementApps mFragment;
  private AppOpsManager mAppOpsManager;
  private Context mContext;

  @Before
  public void setUp() throws Exception {
    mContext = RuntimeEnvironment.application;
    mAppOpsManager = mock(AppOpsManager.class);
    mFragment = new MediaManagementApps();

    // Inject mock AppOpsManager
    Field appOpsManagerField = MediaManagementApps.class.getDeclaredField("mAppOpsManager");
    appOpsManagerField.setAccessible(true);
    appOpsManagerField.set(mFragment, mAppOpsManager);

    // Inject mock ManageApplicationsController
    Field controllerField = ManageAppOp.class.getDeclaredField("mManageApplicationsController");
    controllerField.setAccessible(true);
    ManageApplicationsController mockController = mock(ManageApplicationsController.class);
    controllerField.set(mFragment, mockController);
  }

  @Test
  public void setCanManageMedia_grant_callsSetUidModeAllowed() throws Exception {
    ApplicationsState.AppEntry entry = new ApplicationsState.AppEntry(mContext,
            new ApplicationInfo(), 1);
    entry.info = new ApplicationInfo();
    entry.info.packageName = "com.test.media.app";
    entry.info.uid = 2001;

    Method method = MediaManagementApps.class.getDeclaredMethod("setCanManageMedia",
            ApplicationsState.AppEntry.class,  boolean.class);
    method.setAccessible(true);
    method.invoke(mFragment, entry, true);

    verify(mAppOpsManager).setUidMode(eq(AppOpsManager.OP_MANAGE_MEDIA), eq(2001),
            eq(AppOpsManager.MODE_ALLOWED));
  }

  @Test
  public void setCanManageMedia_revoke_callsSetUidModeErrored() throws Exception {
    ApplicationsState.AppEntry entry = new ApplicationsState.AppEntry(mContext,
            new ApplicationInfo(), 1);
    entry.info = new ApplicationInfo();
    entry.info.packageName = "com.test.media.app";
    entry.info.uid = 2002;

    Method method = MediaManagementApps.class.getDeclaredMethod("setCanManageMedia",
                    ApplicationsState.AppEntry.class,
                        boolean.class);
    method.setAccessible(true);
    method.invoke(mFragment, entry, false);

    verify(mAppOpsManager).setUidMode(eq(AppOpsManager.OP_MANAGE_MEDIA), eq(2002),
            eq(AppOpsManager.MODE_ERRORED));
  }
}
