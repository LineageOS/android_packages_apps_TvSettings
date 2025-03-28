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
package com.android.tv.settings.slice

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.android.tv.twopanelsettings.slices.SliceCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClearCachedSlicesReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            return
        }
        val callback = intent.getParcelableExtra(CLEAR_COMPLETED_CALLBACK_EXTRA,
            PendingIntent::class.java)
        CoroutineScope(Dispatchers.IO).launch {
            SliceCacheManager.getInstance(context).clearCachedSlices()
            callback?.send()
        }
    }

    companion object {
        private const val CLEAR_COMPLETED_CALLBACK_EXTRA = "clear_completed_callback"
    }
}