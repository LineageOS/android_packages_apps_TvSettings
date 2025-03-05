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
 * limitations under the License.
 */
package com.android.tv.settings.widget

import androidx.leanback.R
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.leanback.app.GuidedStepSupportFragment

open class SettingsGuidedStepFragment : GuidedStepSupportFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val title = view.requireViewById(R.id.guidance_title) as TextView
        title.setMaxLines(4)
        val description = view.requireViewById(R.id.guidance_description) as TextView
        description.setMaxLines(10) // Allow for long text without truncation.
    }
}