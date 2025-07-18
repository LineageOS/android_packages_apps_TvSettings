/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.tv.settings.about;

import static com.android.tv.settings.util.InstrumentationUtils.logEntrySelected;

import android.app.AlertDialog;
import android.app.tvsettings.TvSettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import com.android.tv.twopanelsettings.FullScreenDialogFragment;
import com.android.tv.twopanelsettings.FullScreenDialogFragmentActivity;
import com.android.tv.settings.PreferenceUtils;
import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.settings.overlay.FlavorUtils;
import com.android.tv.settings.util.SliceUtils;
import com.android.tv.twopanelsettings.slices.SliceShard;
import com.android.tv.twopanelsettings.slices.compat.Slice;

@Keep
public class LegalFragment extends SettingsPreferenceFragment implements SliceShard.Callbacks {

  private static final String KEY_TERMS = "terms";
  private static final String KEY_LICENSE = "license";
  private static final String KEY_COPYRIGHT = "copyright";
  private static final String KEY_WEBVIEW_LICENSE = "webview_license";
  private static final String KEY_ADS = "ads";
  private static final String KEY_CONSUMER_INFORMATION = "consumer_information";

  private SliceShard mSliceShard;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    var sliceUri =
        SliceShard.Companion.getSliceUri(
            getResources(),
            R.string.legal_fragment_slice_uri,
            R.string.main_fragment_slice_uri,
            "about_legal");
    if (!SliceUtils.isSliceProviderValid(requireContext(), sliceUri)) {
      setPreferencesFromResource(R.xml.about_legal, null);
      configurePreferences();
      return;
    }

    setPreferencesFromResource(R.xml.settings_loading, null);
    mSliceShard =
        new SliceShard(
            this,
            sliceUri,
            this,
            getString(R.string.legal_information),
            SliceShard.Companion.getPrefContext(requireContext()),
            true);
  }

  @Override
  public void onSlice(@Nullable Slice slice) {
    mSliceShard = null;
    if (slice == null) {
      setPreferencesFromResource(R.xml.about_legal, null);
    }
    configurePreferences();
  }

  private void configurePreferences() {
    final PreferenceScreen screen = getPreferenceScreen();

    final Context context = getActivity();
    PreferenceUtils.resolveSystemActivityOrRemove(
        context, screen, findPreference(KEY_TERMS), PreferenceUtils.FLAG_SET_TITLE);
    PreferenceUtils.resolveSystemActivityOrRemove(
        context, screen, findPreference(KEY_LICENSE), PreferenceUtils.FLAG_SET_TITLE);
    PreferenceUtils.resolveSystemActivityOrRemove(
        context, screen, findPreference(KEY_COPYRIGHT), PreferenceUtils.FLAG_SET_TITLE);
    PreferenceUtils.resolveSystemActivityOrRemove(
        context, screen, findPreference(KEY_WEBVIEW_LICENSE), PreferenceUtils.FLAG_SET_TITLE);
    if (FlavorUtils.isTwoPanel(getContext())) {
      Preference adsPref = findPreference(KEY_ADS);
      if (adsPref != null) {
        adsPref.setVisible(false);
      }
      Preference consumerInfoPref = findPreference(KEY_CONSUMER_INFORMATION);
      if (consumerInfoPref != null) {}

    } else {
      PreferenceUtils.resolveSystemActivityOrRemove(
          context, screen, findPreference(KEY_ADS), PreferenceUtils.FLAG_SET_TITLE);
    }
  }

  @Override
  public boolean onPreferenceTreeClick(Preference preference) {
    switch (preference.getKey()) {
      case KEY_LICENSE:
        logEntrySelected(TvSettingsEnums.SYSTEM_ABOUT_LEGAL_INFO_OPEN_SOURCE);
        break;
      case KEY_TERMS:
        logEntrySelected(TvSettingsEnums.SYSTEM_ABOUT_LEGAL_INFO_GOOGLE_LEGAL);
        break;
      case KEY_WEBVIEW_LICENSE:
        logEntrySelected(TvSettingsEnums.SYSTEM_ABOUT_LEGAL_INFO_SYSTEM_WEBVIEW);
        break;
      case KEY_CONSUMER_INFORMATION:
        handleConsumerInfoPreference();
        break;
    }
    return super.onPreferenceTreeClick(preference);
  }

  @Override
  protected int getPageId() {
    return TvSettingsEnums.SYSTEM_ABOUT_LEGAL_INFO;
  }

  private void handleConsumerInfoPreference() {
    if (FlavorUtils.isTwoPanel(getContext())) {
      Intent intent = new Intent(getActivity(), ConfirmationDialogFragmentActivity.class);
      startActivity(intent);
    } else {
      new AlertDialog.Builder(getContext())
          .setMessage(getContext().getString(R.string.consumer_information_message))
          .setPositiveButton(
              getContext().getString(R.string.consumer_information_button_ok),
              (dialog, which) -> {})
          .show();
    }
  }

  public static class ConfirmationDialogFragmentActivity extends FullScreenDialogFragmentActivity {
    public Bundle provideArguments() {
      Intent intent = getIntent();
      return new FullScreenDialogFragment.DialogBuilder()
          .setIcon(Icon.createWithResource(this, R.drawable.ic_info_outline))
          .setTitle(getString(R.string.consumer_information_title))
          .setMessage(getString(R.string.consumer_information_message))
          .setPositiveButton(getString(R.string.consumer_information_button_ok))
          .build();
    }

    public FullScreenDialogFragmentActivity.OnPositiveActionClickedListener
        onPositiveActionClicked() {
      return () -> {
        finish();
      };
    }

    public FullScreenDialogFragmentActivity.OnNegativeActionClickedListener
        onNegativeActionClicked() {
      return () -> {
        finish();
      };
    }

    @Override
    public Drawable getDrawableIconForDialog() {
      return null;
    }
  }
}
