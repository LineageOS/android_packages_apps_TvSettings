/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.tv.settings.inputmethod;

import android.annotation.UserIdInt;
import android.content.Context;
import android.view.inputmethod.InputMethodInfo;
import android.widget.CompoundButton;
import android.view.ViewGroup;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.inputmethod.InputMethodPreference;
import com.android.tv.settings.overlay.FlavorUtils;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.android.tv.settings.R;

/**
 * Input method preference for Android TV.
 *
 * <p>This preference handle the switch logic for TV.
 */
public class TVInputMethodPreference extends InputMethodPreference {
  public TVInputMethodPreference(
      final Context prefContext,
      final InputMethodInfo imi,
      final boolean isAllowedByOrganization,
      final InputMethodPreference.OnSavePreferenceListener onSaveListener,
      final @UserIdInt int userId) {
    super(prefContext, imi, isAllowedByOrganization, onSaveListener, userId);
    setWidgetLayoutResource(R.layout.preference_switch_input);
  }

  @Override
  public boolean onPreferenceClick(final Preference preference) {
    final CompoundButton switchWidget = getSwitch();
    if (!switchWidget.isEnabled()) {
      return true;
    }
    final boolean newValue = !isChecked();
    switchWidget.setChecked(isChecked());
    callChangeListener(newValue);
    return true;
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {
    super.onBindViewHolder(holder);
    if (FlavorUtils.isTwoPanel(getContext())) {
      setOnFocusChangeListenerTintChanges(holder);
    }
  }

  /**
   * This method sets an OnFocusChangeListener to change the statelist drawable manually because we
   * cannot use duplicateParentState here, because it's possible for the Preference row to have
   * state "enabled" and the switch to be "disabled"
   */
  private void setOnFocusChangeListenerTintChanges(PreferenceViewHolder holder) {
    ViewGroup widgetFrame = (ViewGroup) holder.findViewById(android.R.id.widget_frame);
    View container = (View) widgetFrame.getParent();
    MaterialSwitch switchWidget = (MaterialSwitch) widgetFrame.findViewById(R.id.switchWidget);
    switchWidget.setTrackTintList(getContext().getColorStateList(R.color.control_tint_selector));
    switchWidget.setTrackDecorationTintList(
        getContext().getColorStateList(R.color.control_tint_selector));
    switchWidget.setThumbTintList(getContext().getColorStateList(R.color.thumb_tint_selector));
    container.setOnFocusChangeListener(
        (v, hasFocus) -> {
          if (hasFocus) {
            switchWidget.setTrackTintList(
                getContext().getColorStateList(R.color.control_tint_focused_selector));
            switchWidget.setTrackDecorationTintList(
                getContext().getColorStateList(R.color.control_tint_focused_selector));
            switchWidget.setThumbTintList(
                getContext().getColorStateList(R.color.thumb_tint_focused_selector));
          } else {
            switchWidget.setTrackTintList(
                getContext().getColorStateList(R.color.control_tint_selector));
            switchWidget.setTrackDecorationTintList(
                getContext().getColorStateList(R.color.control_tint_selector));
            switchWidget.setThumbTintList(
                getContext().getColorStateList(R.color.thumb_tint_selector));
          }
        });
  }
}
