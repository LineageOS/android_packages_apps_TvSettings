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

package com.android.tv.settings.device.displaysound;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;

import androidx.annotation.Keep;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;

import com.android.settingslib.core.AbstractPreferenceController;
import com.android.tv.settings.PreferenceControllerFragment;
import com.android.tv.settings.R;
import com.android.tv.settings.RadioPreference;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * This Fragment is responsible for allowing the user to choose the resolution and refresh rate
 * from the list of resolution and refresh rates which are supported by device.
 */
@Keep
public class ResolutionSelectionFragment extends PreferenceControllerFragment {

    static final String KEY_MODE_SELECTION = "resolution_selection_option";
    static final String KEY_RESOLUTION_PREFIX = "resolution_selection_";
    static final String KEY_RESOLUTION_SELECTION_AUTO = "resolution_selection_auto";

    private static final String TAG = ResolutionSelectionFragment.class.getSimpleName();
    private DisplayManager mDisplayManager;
    private Display.Mode[] mModes;
    private int mUserPreferredModeIndex;
    private PreferenceCategory mResolutionPreferenceCategory;

    static final Set<Integer> STANDARD_RESOLUTIONS_IN_ORDER = Set.of(2160, 1080, 720, 576, 480);

    /** @return the new instance of the class */
    public static ResolutionSelectionFragment newInstance() {
        return new ResolutionSelectionFragment();
    }

    @Override
    public void onAttach(Context context) {
        mDisplayManager = getDisplayManager();
        super.onAttach(context);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.resolution_selection;
    }

    @Override
    protected List<AbstractPreferenceController> onCreatePreferenceControllers(Context context) {
        return null;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.resolution_selection, null);
        mResolutionPreferenceCategory = findPreference(KEY_MODE_SELECTION);

        Display display = mDisplayManager.getDisplay(Display.DEFAULT_DISPLAY);
        mModes = display.getSupportedModes();
        Arrays.sort(mModes, new Comparator<Display.Mode>() {
            // Sort in descending order of refresh rate.
            @Override
            public int compare(Display.Mode o1, Display.Mode o2) {
                int resolution1 = Math.min(o1.getPhysicalHeight(), o1.getPhysicalWidth());
                int resolution2 = Math.min(o2.getPhysicalHeight(), o2.getPhysicalWidth());

                // The resolution which is in list of standard resolutions appears before the one
                // which is not.
                if (STANDARD_RESOLUTIONS_IN_ORDER.contains(resolution2)
                        && !STANDARD_RESOLUTIONS_IN_ORDER.contains(resolution1)) {
                    return 1;
                }
                if (STANDARD_RESOLUTIONS_IN_ORDER.contains(resolution1)
                        && !STANDARD_RESOLUTIONS_IN_ORDER.contains(resolution2)) {
                    return -1;
                }
                if (resolution2 == resolution1) {
                    return (int) o2.getRefreshRate() - (int) o1.getRefreshRate();
                }
                return resolution2 - resolution1;
            }
        });

        createPreferences();

        mUserPreferredModeIndex = lookupModeIndex(mDisplayManager.getUserPreferredDisplayMode());
        if (mUserPreferredModeIndex != -1) {
            selectRadioPreference(findPreference(KEY_RESOLUTION_PREFIX + mUserPreferredModeIndex));
        } else {
            selectRadioPreference(findPreference(KEY_RESOLUTION_SELECTION_AUTO));
        }
    }

    private void createPreferences() {
        RadioPreference pref = new RadioPreference(getContext());
        pref.setTitle(getContext().getString(R.string.resolution_selection_auto_title));
        pref.setKey(KEY_RESOLUTION_SELECTION_AUTO);
        mResolutionPreferenceCategory.addPreference(pref);

        for (int i = 0; i < mModes.length; i++) {
            int resolution = Math.min(mModes[i].getPhysicalHeight(), mModes[i].getPhysicalWidth());
            String title = resolution + "p";
            if (resolution == 2160) {
                title = "4k";
            }
            mResolutionPreferenceCategory.addPreference(createResolutionPreference(
                    title,
                    getRefreshRateString(mModes[i].getRefreshRate()) + " Hz",
                    i));
        }
    }

    /** Returns a radio preference for each display mode. */
    private RadioPreference createResolutionPreference(
            String title, String summary, int resolution) {
        RadioPreference pref = new RadioPreference(getContext());
        pref.setTitle(title);
        pref.setSummary(summary);
        pref.setKey(KEY_RESOLUTION_PREFIX + resolution);
        return pref;
    }

    @VisibleForTesting
    DisplayManager getDisplayManager() {
        return getContext().getSystemService(DisplayManager.class);
    }

    private PreferenceGroup getPreferenceGroup() {
        return (PreferenceGroup) findPreference(KEY_MODE_SELECTION);
    }

    private void selectRadioPreference(Preference preference) {
        final RadioPreference radioPreference = (RadioPreference) preference;
        radioPreference.setChecked(true);
        radioPreference.clearOtherRadioPreferences(getPreferenceGroup());
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();

        if (key == null) {
            return super.onPreferenceTreeClick(preference);
        }

        if (preference instanceof RadioPreference) {
            selectRadioPreference(preference);

            if (key.equals(KEY_RESOLUTION_SELECTION_AUTO)) {
                mDisplayManager.clearUserPreferredDisplayMode();
            } else if (key.contains(KEY_RESOLUTION_PREFIX)) {
                int modeIndex = Integer.valueOf(key.substring(KEY_RESOLUTION_PREFIX.length()));
                Display.Mode mode = mModes[modeIndex];
                mDisplayManager.setUserPreferredDisplayMode(mode);
            }
        }

        return super.onPreferenceTreeClick(preference);
    }

    /** Returns the index of Display mode that matches UserPreferredMode */
    public int lookupModeIndex(Display.Mode userPreferredMode) {
        if (userPreferredMode != null) {
            for (int i = 0; i < mModes.length; i++) {
                if (mModes[i].matches(userPreferredMode.getPhysicalWidth(),
                        userPreferredMode.getPhysicalHeight(),
                        userPreferredMode.getRefreshRate())) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String getRefreshRateString(float refreshRate) {
        float roundedRefreshRate = Math.round(refreshRate * 100.0f) / 100.0f;
        if (roundedRefreshRate % 1 == 0) {
            return Integer.toString((int) roundedRefreshRate);
        } else {
            return Float.toString(roundedRefreshRate);
        }
    }
}
