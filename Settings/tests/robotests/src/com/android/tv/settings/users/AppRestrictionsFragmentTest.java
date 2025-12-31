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

package com.android.tv.settings.users;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import androidx.fragment.app.FragmentActivity;
import com.android.settingslib.users.AppRestrictionsHelper;
import com.android.tv.settings.testutils.ShadowAppRestrictionsHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAppRestrictionsHelper.class})
public class AppRestrictionsFragmentTest {

  private AppRestrictionsFragment mFragment;
  @Mock private IPackageManager mIPm;

  // We rely on Robolectric's PackageManager for basic stuff, but we inject mIPm.
  private PackageManager mPackageManager;
  private PackageInfo mSystemPackageInfo;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    ShadowAppRestrictionsHelper.reset();
    // Prepare System Package Info
    mSystemPackageInfo = new PackageInfo();
    mSystemPackageInfo.packageName = "android";
    mSystemPackageInfo.signatures = new Signature[] {new Signature("1234")};
    mSystemPackageInfo.applicationInfo = new ApplicationInfo();
    mSystemPackageInfo.applicationInfo.uid = Process.SYSTEM_UID;
  }

  public static class TestAppRestrictionsFragment extends AppRestrictionsFragment {
    @Override
    Bundle getApplicationRestrictions(String packageName) {
      android.os.UserManager userManager =
          org.robolectric.util.ReflectionHelpers.getField(this, "mUserManager");
      android.os.UserHandle user = org.robolectric.util.ReflectionHelpers.getField(this, "mUser");
      if (userManager != null) {
        return userManager.getApplicationRestrictions(packageName, user);
      }
      return new Bundle();
    }
  }

  @Test
  public void testPlatformSignedApp_isMutable_ifNotSystemUid() throws Exception {
    String packageName = "com.example.platformsigned";
    int uid = 10123; // Normal UID
    boolean isPlatformSigned = true;

    setupFragmentAndPopulate(packageName, uid, isPlatformSigned);

    // Verify preference exists
    androidx.preference.Preference p = mFragment.findPreference("pkg_" + packageName);
    assertThat(p).isNotNull();

    // Verify mutable (because it is NOT system UID)
    com.android.tv.settings.users.AppRestrictionsFragment.AppRestrictionsPreference restrictedPref =
        (com.android.tv.settings.users.AppRestrictionsFragment.AppRestrictionsPreference) p;
    assertThat(restrictedPref.isImmutable()).isFalse();
    assertThat(restrictedPref.isChecked()).isTrue();
  }

  @Test
  public void testRegularApp_isMutable() throws Exception {
    String packageName = "com.example.regular";
    int uid = 10123;
    boolean isPlatformSigned = false;

    setupFragmentAndPopulate(packageName, uid, isPlatformSigned);

    androidx.preference.Preference p = mFragment.findPreference("pkg_" + packageName);
    assertThat(p).isNotNull();

    com.android.tv.settings.users.AppRestrictionsFragment.AppRestrictionsPreference restrictedPref =
        (com.android.tv.settings.users.AppRestrictionsFragment.AppRestrictionsPreference) p;
    assertThat(restrictedPref.isImmutable()).isFalse();
    assertThat(restrictedPref.isChecked()).isTrue();
  }

  @Test
  public void testSystemUidApp_isImmutable() throws Exception {
    String packageName = "com.android.systemapp";
    int uid = Process.SYSTEM_UID;
    boolean isPlatformSigned = true;

    setupFragmentAndPopulate(packageName, uid, isPlatformSigned);

    // Verify preference exists
    androidx.preference.Preference p = mFragment.findPreference("pkg_" + packageName);
    assertThat(p).isNotNull();

    // Verify immutable (because it IS system UID)
    com.android.tv.settings.users.AppRestrictionsFragment.AppRestrictionsPreference restrictedPref =
        (com.android.tv.settings.users.AppRestrictionsFragment.AppRestrictionsPreference) p;
    assertThat(restrictedPref.isImmutable()).isTrue();
  }

  private void setupFragmentAndPopulate(String packageName, int uid, boolean isPlatformSigned)
      throws Exception {
    // Prepare the target app info
    PackageInfo pi = new PackageInfo();
    pi.packageName = packageName;
    pi.applicationInfo = new ApplicationInfo();
    pi.applicationInfo.uid = uid;
    pi.applicationInfo.flags = ApplicationInfo.FLAG_INSTALLED;
    pi.signatures =
        isPlatformSigned ? mSystemPackageInfo.signatures : new Signature[] {new Signature("5678")};

    // Setup mocks
    // 1. IPackageManager (for getPackageInfo with signatures)
    // Use anyLong() for flags as they might be long in newer Android
    when(mIPm.getPackageInfo(eq(packageName), anyLong(), anyInt())).thenReturn(pi);

    // 2. Add to ShadowAppRestrictionsHelper visible apps
    AppRestrictionsHelper.SelectableAppInfo selectable =
        new AppRestrictionsHelper.SelectableAppInfo();
    selectable.packageName = packageName;
    selectable.activityName = packageName + ".MainActivity";
    ShadowAppRestrictionsHelper.sVisibleApps.add(selectable);

    // 3. Setup PackageManager for getPackageInfo("android")
    org.robolectric.android.controller.ActivityController<FragmentActivity> controller =
        Robolectric.buildActivity(FragmentActivity.class).create().start();
    FragmentActivity activity = controller.get();
    mPackageManager = activity.getPackageManager();
    ShadowPackageManager shadowPm = org.robolectric.Shadows.shadowOf(mPackageManager);
    shadowPm.installPackage(mSystemPackageInfo);
    shadowPm.installPackage(pi);

    // Add ResolveInfo for GET_RESTRICTION_ENTRIES so hasSettings is true
    android.content.Intent restrictionsIntent =
        new android.content.Intent(android.content.Intent.ACTION_GET_RESTRICTION_ENTRIES);
    // Don't set package, because AppRestrictionsFragment queries with implicit
    // intent
    // restrictionsIntent.setPackage(packageName);
    android.content.pm.ResolveInfo ri = new android.content.pm.ResolveInfo();
    ri.activityInfo = new android.content.pm.ActivityInfo();
    ri.activityInfo.packageName = packageName;
    ri.activityInfo.name = packageName + ".Receiver";
    shadowPm.addResolveInfoForIntent(restrictionsIntent, ri);

    // Start Fragment
    mFragment = new TestAppRestrictionsFragment();
    activity.getSupportFragmentManager().beginTransaction().add(mFragment, null).commitNow();

    // Inject mIPm (private field)
    ReflectionHelpers.setField(mFragment, "mIPm", mIPm);

    // Inject UserManager
    android.os.UserManager userManager = mock(android.os.UserManager.class);
    android.content.pm.UserInfo userInfo = new android.content.pm.UserInfo(0, "TestUser", 0);
    when(userManager.getUsers()).thenReturn(java.util.Collections.singletonList(userInfo));
    when(userManager.getAliveUsers()).thenReturn(java.util.Collections.singletonList(userInfo));
    when(userManager.getUserInfo(0)).thenReturn(userInfo);
    ReflectionHelpers.setField(mFragment, "mUserManager", userManager);

    // Ensure mUser matches
    ReflectionHelpers.setField(mFragment, "mUser", UserHandle.of(0));

    controller.resume(); // Moves state to RESUMED -> triggers populateApps

    // Wait for AsyncTask to complete
    org.robolectric.shadows.ShadowLooper.idleMainLooper();
  }
}
