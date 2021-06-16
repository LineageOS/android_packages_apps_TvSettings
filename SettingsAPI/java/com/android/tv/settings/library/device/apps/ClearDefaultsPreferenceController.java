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

package com.android.tv.settings.library.device.apps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.os.IBinder;
import android.os.ServiceManager;

import com.android.tv.settings.library.PreferenceCompat;
import com.android.tv.settings.library.UIUpdateCallback;
import com.android.tv.settings.library.data.PreferenceCompatManager;
import com.android.tv.settings.library.util.ResourcesUtil;

/** Preference controller class to handle clear defaults preference. */
public class ClearDefaultsPreferenceController extends AppActionPreferenceController {
    private static final String KEY_CLEAR_DEFAULTS = "clearDefaults";

    private final IUsbManager mUsbManager;
    private final PackageManager mPackageManager;
    private PreferenceCompat mClearDefaultsPreference;

    public ClearDefaultsPreferenceController(Context context,
            UIUpdateCallback callback, int stateIdentifier,
            ApplicationsState.AppEntry appEntry) {
        super(context, callback, stateIdentifier, appEntry);
        final IBinder usbBinder = ServiceManager.getService(Context.USB_SERVICE);
        mUsbManager = IUsbManager.Stub.asInterface(usbBinder);
        mPackageManager = context.getPackageManager();

    }

    @Override
    public void displayPreference(PreferenceCompatManager screen) {
        mClearDefaultsPreference = screen.getOrCreatePrefCompat(getPreferenceKey());
        super.displayPreference(screen);
    }

    @Override
    void refresh() {
        if (mAppEntry == null) {
            return;
        }
        mClearDefaultsPreference.setTitle(ResourcesUtil.getString(
                mContext, "device_apps_app_management_clear_default"));
        mClearDefaultsPreference.setSummary(AppUtils.getLaunchByDefaultSummary(
                mAppEntry, mUsbManager, mPackageManager, mContext).toString());
        mUIUpdateCallback.notifyUpdate(mStateIdentifier, mClearDefaultsPreference);
    }

    @Override
    public String[] getPreferenceKey() {
        return new String[]{KEY_CLEAR_DEFAULTS};
    }
}
