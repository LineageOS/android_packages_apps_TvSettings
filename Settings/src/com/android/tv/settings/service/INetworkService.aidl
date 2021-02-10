/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.tv.settings.service;

import com.android.tv.settings.service.INetworkServiceListener;
import com.android.tv.settings.service.PreferenceParcelable;

interface INetworkService {
  List<PreferenceParcelable> getPreferences();
  PreferenceParcelable getPreference(String key);
  void registerListener(INetworkServiceListener listener);
  void unRegisterListener(INetworkServiceListener listener);
  void onCreate();
  void onStart();
  void onResume();
  void onPause();
  void onDestroy();
  void onPreferenceClick(String key, boolean status);
}