/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.tv.quicksettings;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.View;

public class QuickSettings extends Activity {

    private static final String TAG = "QuickSettings";
    static final int PRESET_SETTING_INDEX = 0;
    static final int INTEGER_SETTING_START_INDEX = 1;

    private int mSlidOutTranslationX;
    private View mRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.side_quicksettings);

        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            mSlidOutTranslationX = -getResources().getDimensionPixelSize(R.dimen.panel_width);
        } else {
            mSlidOutTranslationX = getResources().getDimensionPixelSize(R.dimen.panel_width);
        }

        mRootView = getWindow().getDecorView().findViewById(R.id.main_frame);
        mRootView.setTranslationX(mSlidOutTranslationX);

        if (savedInstanceState == null) {
            final Fragment f = new QuickSettingsFragment();
            getFragmentManager().beginTransaction().add(R.id.side_panel_list, f).commit();
            getFragmentManager().executePendingTransactions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRootView.animate().cancel();
        mRootView.animate().translationX(0).start();
    }

    @Override
    protected void onPause() {
        mRootView.animate().cancel();
        mRootView.animate().translationX(mSlidOutTranslationX).start();
        super.onPause();
    }

}
