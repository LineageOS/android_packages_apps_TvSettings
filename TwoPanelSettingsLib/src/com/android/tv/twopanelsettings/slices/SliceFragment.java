/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.tv.twopanelsettings.slices;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.tv.twopanelsettings.R;
import com.android.tv.twopanelsettings.TwoPanelSettingsFragment.SliceFragmentCallback;

/**
 * A screen presenting a slice in TV settings.
 */
@Keep
public class SliceFragment extends SettingsPreferenceFragment implements
        SliceFragmentCallback, SliceShard.Callbacks {
    private SliceShard mSliceShard;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        String uriString = getArguments().getString(SlicesConstants.TAG_TARGET_URI);
        CharSequence initialTitle = getArguments().getCharSequence(SlicesConstants.TAG_SCREEN_TITLE,
                "");
        super.onCreate(savedInstanceState);
        getPreferenceManager().setPreferenceComparisonCallback(
                new PreferenceManager.SimplePreferenceComparisonCallback() {
                    @Override
                    public boolean arePreferenceContentsTheSame(Preference preference1,
                            Preference preference2) {
                        // Should only check for the default SlicePreference objects, and ignore
                        // other instances of slice reference classes since they all override
                        // Preference.onBindViewHolder(PreferenceViewHolder)
                        return preference1.getClass() == SlicePreference.class
                                && super.arePreferenceContentsTheSame(preference1, preference2);
                    }
                });
        TypedValue themeTypedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.preferenceTheme, themeTypedValue, true);
        Context prefContext = new ContextThemeWrapper(getActivity(), themeTypedValue.resourceId);
        mSliceShard = new SliceShard(this, uriString, this, initialTitle, prefContext);
    }

    @Override
    public void showProgressBar(boolean toShow) {
        if (toShow) {
            showProgressBar();
        } else {
            hideProgressBar();
        }
    }


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceScreen preferenceScreen = getPreferenceManager()
                .createPreferenceScreen(requireContext());
        setPreferenceScreen(preferenceScreen);
    }

    @Override
    public void onPreferenceFocused(Preference preference) {
        mSliceShard.onPreferenceFocused(preference);
    }

    @Override
    public void onSeekbarPreferenceChanged(SliceSeekbarPreference preference, int addValue) {
        mSliceShard.onSeekbarPreferenceChanged(preference, addValue);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return mSliceShard.onPreferenceTreeClick(preference)
                || super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mSliceShard.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mSliceShard.onRestoreInstanceState(savedInstanceState);
        }
    }

    private void showProgressBar() {
        View view = this.getView();
        View progressBar = view == null ? null : getView().findViewById(R.id.progress_bar);
        if (progressBar != null) {
            progressBar.bringToFront();
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressBar() {
        View view = this.getView();
        View progressBar = view == null ? null : getView().findViewById(R.id.progress_bar);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void setSubtitle(CharSequence subtitle) {
        View view = this.getView();
        TextView decorSubtitle = view == null
                ? null
                : (TextView) view.findViewById(R.id.decor_subtitle);
        if (decorSubtitle != null) {
            // This is to remedy some complicated RTL scenario such as Hebrew RTL Account slice with
            // English account name subtitle.
            if (getResources().getConfiguration().getLayoutDirection()
                    == View.LAYOUT_DIRECTION_RTL) {
                decorSubtitle.setGravity(Gravity.TOP | Gravity.RIGHT);
            }
            if (TextUtils.isEmpty(subtitle)) {
                decorSubtitle.setVisibility(View.GONE);
            } else {
                decorSubtitle.setVisibility(View.VISIBLE);
                decorSubtitle.setText(subtitle);
            }
        }
    }

    @Override
    public void setIcon(Drawable drawable) {
        View view = this.getView();
        ImageView decorIcon = view == null ? null : (ImageView) view.findViewById(R.id.decor_icon);
        if (decorIcon != null && drawable != null) {
            TextView decorTitle = view.findViewById(R.id.decor_title);
            if (decorTitle != null) {
                decorTitle.setMaxWidth(
                        getResources().getDimensionPixelSize(R.dimen.decor_title_width));
            }
            decorIcon.setImageDrawable(drawable);
            decorIcon.setVisibility(View.VISIBLE);
        } else if (decorIcon != null) {
            decorIcon.setVisibility(View.GONE);
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup view =
                (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        LayoutInflater themedInflater = LayoutInflater.from(view.getContext());
        final View newTitleContainer = themedInflater.inflate(
                R.layout.slice_title_container, container, false);
        view.removeView(view.findViewById(R.id.decor_title_container));
        view.addView(newTitleContainer, 0);

        if (newTitleContainer != null) {
            newTitleContainer.setOutlineProvider(null);
            newTitleContainer.setBackgroundResource(R.color.tp_preference_panel_background_color);
        }

        final View newContainer =
                themedInflater.inflate(R.layout.slice_progress_bar, container, false);
        if (newContainer != null) {
            ((ViewGroup) newContainer).addView(view);
        }
        return newContainer;
    }

    public CharSequence getScreenTitle() {
        return mSliceShard.getScreenTitle();
    }

    @Override
    protected int getPageId() {
        return mSliceShard.getPageId();
    }

    @Deprecated
    public int getMetricsCategory() {
        return 0;
    }
}
