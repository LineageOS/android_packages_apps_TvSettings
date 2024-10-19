/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.tv.settings;

import android.annotation.Nullable;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.android.tv.settings.FullScreenDialogFragment;
import com.android.tv.settings.R;
import com.android.tv.settings.overlay.FlavorUtils;

public class FullScreenConfirmationActivity extends FragmentActivity {

    private static final String TAG = "FullScreenConfirmationActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Bundle args = getIntent().getExtras();
            if (args == null) {
                Log.e(TAG, "Unable to launch dialog without dialog args");
                finish();
            }
            FullScreenDialogFragment dialogFragment = new FullScreenDialogFragment();
            dialogFragment.setArguments(args);

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, dialogFragment)
                    .commitAllowingStateLoss();
        }
    }

    public static Intent getIntent(Context context, Bundle fullScreenFragmentArgs) {
        return new Intent(context, FullScreenConfirmationActivity.class)
                .putExtras(fullScreenFragmentArgs);
    }
}