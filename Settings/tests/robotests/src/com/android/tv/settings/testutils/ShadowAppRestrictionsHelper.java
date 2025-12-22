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

package com.android.tv.settings.testutils;

import android.content.Context;
import android.os.UserHandle;
import com.android.settingslib.users.AppRestrictionsHelper;
import java.util.ArrayList;
import java.util.List;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(AppRestrictionsHelper.class)
public class ShadowAppRestrictionsHelper {

  // Static mock/fake to control behavior from test
  public static AppRestrictionsHelper.SelectableAppInfo sMockAppInfo;
  public static List<AppRestrictionsHelper.SelectableAppInfo> sVisibleApps = new ArrayList<>();

  @Implementation
  public void __constructor__(Context context, UserHandle user) {
    // no-op
  }

  @Implementation
  public void setLeanback(boolean leanback) {}

  @Implementation
  public void fetchAndMergeApps() {
    // no-op, we control visible apps via sVisibleApps
  }

  @Implementation
  public List<AppRestrictionsHelper.SelectableAppInfo> getVisibleApps() {
    return sVisibleApps;
  }

  @Implementation
  public void setPackageSelected(String packageName, boolean selected) {}

  public static void reset() {
    sVisibleApps.clear();
  }
}
