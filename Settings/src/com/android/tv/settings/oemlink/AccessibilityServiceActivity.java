/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.tv.settings.oemlink;

import static com.android.tv.settings.overlay.FlavorUtils.X_EXPERIENCE_FLAVORS_MASK;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import androidx.fragment.app.Fragment;

import com.android.tv.settings.TvSettingsActivity;
import com.android.tv.settings.accessibility.AccessibilityServiceFragment;

import java.util.List;

/** An OEM hook for starting a specific accessibility service settings directly. */
public class AccessibilityServiceActivity extends TvSettingsActivity {

    private static final String TAG = "A11yServiceOemLink";
    private static final String A11Y_SERVICE_INFO_EXTRA = "accessibilityServiceInfo";

    @Override
    protected Fragment createSettingsFragment() {
        if (getIntent() == null || getIntent().getExtras() == null
                || getIntent().getExtras().getParcelable(A11Y_SERVICE_INFO_EXTRA) == null) {
            Log.e(TAG, "No accessibility info extras, returning null");
            return null;
        }
        AccessibilityServiceInfo a11yServiceInfo =
                getIntent().getExtras().getParcelable(A11Y_SERVICE_INFO_EXTRA);
        final List<AccessibilityServiceInfo> installedA11yServiceInfos =
                getSystemService(AccessibilityManager.class)
                        .getInstalledAccessibilityServiceList();
        if (installedA11yServiceInfos == null || installedA11yServiceInfos.isEmpty()
                || !installedA11yServiceInfos.contains(a11yServiceInfo)) {
            Log.e(TAG, "Input accessibility service info is absent on device, returning null");
            return null;
        }
        // only trust and use installed a11y service matched
        AccessibilityServiceInfo installedA11yServiceInfo =
                getAccessibilityServiceInfo(a11yServiceInfo, installedA11yServiceInfos);
        if (installedA11yServiceInfo == null) {
            Log.e(TAG,
                    "Cannot find a matched installed accessibility service info, returning null");
            return null;
        }

        Bundle args = new Bundle();
        AccessibilityServiceFragment.prepareArgs(
                args,
                installedA11yServiceInfo.getResolveInfo().serviceInfo.packageName,
                installedA11yServiceInfo.getResolveInfo().serviceInfo.name,
                installedA11yServiceInfo.getSettingsActivityName(),
                installedA11yServiceInfo.getResolveInfo().loadLabel(
                        this.getPackageManager()).toString());
        return com.android.tv.settings.overlay.FlavorUtils.getFeatureFactory(
                        this).getSettingsFragmentProvider()
                .newSettingsFragment(AccessibilityServiceFragment.class.getName(), args);
    }

    /**
     * Retrieves an {@link AccessibilityServiceInfo} object from a list of available service
     * information that matches the ID of a given incoming service information.
     *
     * <p>This method iterates through a provided list of {@link AccessibilityServiceInfo} objects.
     * For each service in the list, it compares its unique identifier (ID) with the ID of the
     * {@code incomingServiceInfo}. If a match is found, the matching {@link
     * AccessibilityServiceInfo} object from the list is returned.
     *
     * @param incomingServiceInfo The {@link AccessibilityServiceInfo} object whose ID is to be
     *     matched. This object provides the target ID for the search.
     * @param serviceInfos A {@link List} of {@link AccessibilityServiceInfo} objects to search
     *     through. This list typically contains information about currently installed or active
     *     accessibility services.
     * @return The {@link AccessibilityServiceInfo} object from {@code serviceInfos} that has the
     *     same ID as {@code incomingServiceInfo}, or {@code null} if no matching service is found
     *     in the list.
     */
    private AccessibilityServiceInfo getAccessibilityServiceInfo(
            AccessibilityServiceInfo incomingServiceInfo,
            List<AccessibilityServiceInfo> serviceInfos) {
        final int serviceInfoCount = serviceInfos.size();
        for (int i = 0; i < serviceInfoCount; i++) {
            AccessibilityServiceInfo serviceInfo = serviceInfos.get(i);
            if (serviceInfo.getId().equals(incomingServiceInfo.getId())) {
                return serviceInfo;
            }
        }
        return null;
    }

    @Override
    protected int getAvailableFlavors() {
        return X_EXPERIENCE_FLAVORS_MASK;
    }
}
