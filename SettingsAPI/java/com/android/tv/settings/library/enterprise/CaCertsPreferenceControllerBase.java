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

package com.android.tv.settings.library.enterprise;

import android.content.Context;
import com.android.tv.settings.library.PreferenceCompat;
import com.android.tv.settings.library.UIUpdateCallback;
import com.android.tv.settings.library.overlay.FlavorUtils;
import com.android.tv.settings.library.util.AbstractPreferenceController;
import com.android.tv.settings.library.util.ResourcesUtil;

public abstract class CaCertsPreferenceControllerBase extends AbstractPreferenceController {
    protected final EnterprisePrivacyFeatureProvider mFeatureProvider;

    public CaCertsPreferenceControllerBase(
            Context context, UIUpdateCallback callback, int stateIdentifier) {
        super(context, callback, stateIdentifier);
        mFeatureProvider =
                FlavorUtils.getFeatureFactory(context).getEnterprisePrivacyFeatureProvider(context);
    }

    @Override
    public void updateState(PreferenceCompat preference) {
        final int certs = getNumberOfCaCerts();
        preference.setSummary(ResourcesUtil.getQuantityString(
                mContext, "enterprise_privacy_number_ca_certs", certs, certs));
    }

    @Override
    public boolean isAvailable() {
        return getNumberOfCaCerts() > 0;
    }

    protected abstract int getNumberOfCaCerts();
}
