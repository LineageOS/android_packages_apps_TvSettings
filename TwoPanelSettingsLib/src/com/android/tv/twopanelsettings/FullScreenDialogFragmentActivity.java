/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.tv.twopanelsettings;

import android.annotation.Nullable;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;

public abstract class FullScreenDialogFragmentActivity extends FragmentActivity {
  /** Returns a FullScreenDialogFragment.DialogBuilder to populate the initial fragment */
  public abstract Bundle provideArguments();

  /** The click listener for the positive button */
  public abstract OnPositiveActionClickedListener onPositiveActionClicked();

  /** The click listener for the negative button */
  public abstract OnNegativeActionClickedListener onNegativeActionClicked();

  /** Icon drawable if we want to pass a drawable instead of an icon */
  public Drawable getDrawableIconForDialog() {
    return null;
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState == null) {
      setTheme(R.style.TvSettingsDialog_FullScreen);
      getSupportFragmentManager()
          .beginTransaction()
          .add(
              android.R.id.content,
              InnerDialogFragment.newInstance(
                  this,
                  provideArguments(),
                  getDrawableIconForDialog(),
                  onPositiveActionClicked(),
                  onNegativeActionClicked()))
          .commitAllowingStateLoss();
    }
  }

  public static class InnerDialogFragment extends FullScreenDialogFragment {
    OnPositiveActionClickedListener onPositiveActionClicked;
    OnNegativeActionClickedListener onNegativeActionClicked;
    Bundle dialogArguments;
    Drawable drawableIcon;

    static InnerDialogFragment newInstance(
        Context context,
        Bundle dialogArguments,
        Drawable drawableIcon,
        OnPositiveActionClickedListener onPositiveActionClicked,
        OnNegativeActionClickedListener onNegativeActionClicked) {
      InnerDialogFragment fragment = new InnerDialogFragment();
      fragment.setArguments(dialogArguments);
      fragment.drawableIcon = drawableIcon;
      fragment.onPositiveActionClicked = onPositiveActionClicked;
      fragment.onNegativeActionClicked = onNegativeActionClicked;
      fragment.dialogArguments = dialogArguments;
      return fragment;
    }

    @Override
    public void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
    }

    @Override
    public void onButtonPressed(int action) {
      if (action == ACTION_POSITIVE && onPositiveActionClicked != null) {
        onPositiveActionClicked.onPositiveActionClicked();
      } else if (action == ACTION_NEGATIVE && onNegativeActionClicked != null) {
        onNegativeActionClicked.onNegativeActionClicked();
      } else {
        getActivity().finish();
      }
    }

    @Override
    public Drawable getDrawableIcon() {
      return drawableIcon;
    }
  }

  public interface OnPositiveActionClickedListener {
    void onPositiveActionClicked();
  }

  public interface OnNegativeActionClickedListener {
    void onNegativeActionClicked();
  }
}
