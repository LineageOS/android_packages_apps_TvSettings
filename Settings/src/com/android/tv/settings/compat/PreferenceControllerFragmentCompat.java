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

package com.android.tv.settings.compat;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.leanback.preference.LeanbackPreferenceFragmentCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.TwoStatePreference;

import com.android.tv.settings.HasSettingsManager;
import com.android.tv.settings.R;
import com.android.tv.settings.library.PreferenceCompat;
import com.android.tv.settings.library.SettingsManager;
import com.android.tv.settings.overlay.FlavorUtils;
import com.android.tv.twopanelsettings.TwoPanelSettingsFragment;

import java.util.List;

/** Provide utility class to render settings preferences. */
public abstract class PreferenceControllerFragmentCompat extends LeanbackPreferenceFragmentCompat {
    private SettingsManager mSettingsManager;
    private String mTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() instanceof HasSettingsManager) {
            mSettingsManager = ((HasSettingsManager) getActivity()).getSettingsManager();
            if (mSettingsManager != null) {
                mSettingsManager.onCreate(getState(), getArguments());
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getActivity() instanceof HasSettingsManager) {
            mSettingsManager = ((HasSettingsManager) getActivity()).getSettingsManager();
            if (mSettingsManager != null) {
                mSettingsManager.onStart(getState());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getCallbackFragment() instanceof TwoPanelSettingsFragment) {
            TwoPanelSettingsFragment parentFragment =
                    (TwoPanelSettingsFragment) getCallbackFragment();
            parentFragment.addListenerForFragment(this);
        }
        if (getActivity() instanceof HasSettingsManager) {
            mSettingsManager = ((HasSettingsManager) getActivity()).getSettingsManager();
            if (mSettingsManager != null) {
                mSettingsManager.onResume(getState());
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getCallbackFragment() instanceof TwoPanelSettingsFragment) {
            TwoPanelSettingsFragment parentFragment =
                    (TwoPanelSettingsFragment) getCallbackFragment();
            parentFragment.removeListenerForFragment(this);
        }
        if (getActivity() instanceof HasSettingsManager) {
            mSettingsManager = ((HasSettingsManager) getActivity()).getSettingsManager();
            if (mSettingsManager != null) {
                mSettingsManager.onPause(getState());
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity() instanceof HasSettingsManager) {
            mSettingsManager = ((HasSettingsManager) getActivity()).getSettingsManager();
            if (mSettingsManager != null) {
                mSettingsManager.onStop(getState());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity() instanceof HasSettingsManager) {
            mSettingsManager = ((HasSettingsManager) getActivity()).getSettingsManager();
            if (mSettingsManager != null) {
                mSettingsManager.onDestroy(getState());
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (getActivity() instanceof HasSettingsManager) {
            mSettingsManager = ((HasSettingsManager) getActivity()).getSettingsManager();
            if (mSettingsManager == null) {
                return true;
            }
            boolean handled = mSettingsManager.onPreferenceClick(
                    getState(),
                    preference.getKey(),
                    preference instanceof TwoStatePreference
                            && ((TwoStatePreference) preference).isChecked());
            if (!handled) {
                return super.onPreferenceTreeClick(preference);
            }
        }
        return true;
    }

    protected Preference findTargetPreference(String[] key) {
        Preference preference = findPreference(key[0]);
        for (int i = 1; i < key.length; i++) {
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
                preference = preferenceGroup.findPreference(key[i]);
            } else {
                return null;
            }
        }
        return preference;
    }

    public HasKeys updatePref(PreferenceCompat prefCompat) {
        if (prefCompat == null) {
            return null;
        }
        String[] key = prefCompat.getKey();
        Preference preference = findTargetPreference(key);
        if (preference == null) {
            return null;
        }

        RenderUtil.updatePreference(
                getContext(), (HasKeys) preference, prefCompat, preference.getOrder());
        return (HasKeys) preference;
    }

    public void updateAllPref(List<PreferenceCompat> preferenceCompatList) {
        if (preferenceCompatList == null) {
            return;
        }
        preferenceCompatList.stream()
                .forEach(preferenceCompat -> updatePref(preferenceCompat));
    }

    public void updateScreenTitle(String title) {
        setTitle(title);
        mTitle = title;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
    }

    public abstract int getState();

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (view != null) {
            TextView titleView = view.findViewById(R.id.decor_title);
            // We rely on getResources().getConfiguration().getLayoutDirection() instead of
            // view.isLayoutRtl() as the latter could return false in some complex scenarios even if
            // it is RTL.
            if (titleView != null) {
                if (mTitle != null) {
                    titleView.setText(mTitle);
                }
                if (getResources().getConfiguration().getLayoutDirection()
                        == View.LAYOUT_DIRECTION_RTL) {
                    titleView.setGravity(Gravity.RIGHT);
                }
            }
            if (FlavorUtils.isTwoPanel(getContext())) {
                ViewGroup decor = view.findViewById(R.id.decor_title_container);
                if (decor != null) {
                    decor.setOutlineProvider(null);
                    decor.setBackgroundResource(R.color.tp_preference_panel_background_color);
                }
            }
        }
    }
}

