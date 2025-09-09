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
package com.android.tv.settings.connectivity

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Keep
import com.android.tv.twopanelsettings.R
import com.android.tv.twopanelsettings.slices.InfoFragment

@Keep
class ShareThreadNetworkInfoFragment : InfoFragment() {
    override fun onCreateView(
        inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater!!, container, savedInstanceState)!!
        val infoTitleIconImageView = view.requireViewById<ImageView>(R.id.info_title_icon)
        val infoSummaryTextView = view.requireViewById<TextView>(R.id.info_summary)

        infoTitleIconImageView.setImageResource(R.drawable.ic_info_outline_base)
        infoTitleIconImageView.visibility = View.VISIBLE

        infoSummaryTextView.setText(getString(R.string.share_thread_network_description,
            Build.MODEL))
        infoSummaryTextView.visibility = View.VISIBLE
        return view
    }

    override fun updateInfoFragment() {
        // No-op as this is hosting a static info preview panel
    }

}