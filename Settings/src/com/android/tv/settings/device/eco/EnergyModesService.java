/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.tv.settings.device.eco;

import static android.Manifest.permission.MANAGE_LOW_POWER_STANDBY;

import android.annotation.EnforcePermission;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.android.tv.settings.device.eco.EnergyModesHelper.EnergyMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Service that provides methods to query and set Energy Modes.
 */
public class EnergyModesService extends Service {

    private BinderService mService;

    @Override
    public void onCreate() {
        super.onCreate();
        mService = new BinderService(getApplication());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mService;
    }

    private class BinderService extends IEnergyModesService.Stub {
        private final Context mContext;
        private final EnergyModesHelper mHelper;

        BinderService(Context context) {
            mContext = context;
            mHelper = new EnergyModesHelper(context);
        }

        @Override
        public List<Bundle> getModes() {
            final long ident = Binder.clearCallingIdentity();
            try {
                final List<Bundle> result = new ArrayList<>();
                final List<EnergyMode> energyModes = mHelper.getEnergyModes();
                final EnergyMode currentMode = mHelper.updateEnergyMode();

                for (EnergyMode mode : energyModes) {
                    result.add(convertToBundle(mode, mode == currentMode));
                }

                return result;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        private Bundle convertToBundle(EnergyMode mode, boolean selected) {
            Bundle bundle = new Bundle();

            bundle.putBoolean(KEY_SELECTED, selected);
            bundle.putString(KEY_IDENTIFIER, getString(mode.identifierRes));
            bundle.putParcelable(KEY_ICON, Icon.createWithResource(mContext, mode.iconRes));
            bundle.putInt(KEY_COLOR, getColor(mode.colorRes));
            bundle.putString(KEY_TITLE, getString(mode.titleRes));
            bundle.putString(KEY_SUBTITLE, getString(mode.subtitleRes));
            bundle.putString(KEY_DESCRIPTION, getString(mode.infoTextRes));
            bundle.putString(KEY_SHORT_DESCRIPTION, getString(mode.infoTextRes));
            bundle.putStringArray(KEY_FEATURES_LIST,
                    getResources().getStringArray(mode.featuresRes));
            bundle.putStringArray(KEY_FEATURES,
                    mHelper.getAllowedFeatures(mode).toArray(new String[0]));

            return bundle;
        }

        @EnforcePermission(MANAGE_LOW_POWER_STANDBY)
        @Override
        public void setMode(String identifier) {
            super.setMode_enforcePermission();

            final long ident = Binder.clearCallingIdentity();
            try {
                final EnergyMode mode = mHelper.getEnergyMode(identifier);
                if (mode == null) {
                    throw new IllegalArgumentException("Unknown energy mode: " + identifier);
                }
                mHelper.setEnergyMode(mode);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        @Override
        public String getDefaultMode() {
            final long ident = Binder.clearCallingIdentity();
            try {
                final EnergyMode mode = mHelper.getDefaultEnergyMode();
                if (mode == null) {
                    return null;
                }
                return getString(mode.identifierRes);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }
}
