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

import android.os.Bundle;

import androidx.annotation.Keep;

import com.android.tv.settings.R;
import com.android.tv.settings.library.ManagerUtil;

/**
 * This Fragment compat is responsible for allowing the user to express a preference for matching
 * the display frame rate to to the frame rate of a video being played.
 */
@Keep
public class MatchContentRateFragmentCompat extends RadioPreferencesFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        setPreferencesFromResource(R.xml.match_content_rate_compat, null);
    }


    @Override
    public int getStateIdentifier() {
        return ManagerUtil.STATE_MATCH_CONTENT_FRAME;
    }
}