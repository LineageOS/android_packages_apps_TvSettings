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
package com.android.tv.twopanelsettings

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kotlin.jvm.JvmOverloads
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A helper class for performing background operations and publishing results on the UI thread.
 *
 * @param <Result> The type of the result of the background computation.
 */
open class KtAsyncTask<Result>() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var job: Job? = null

    constructor(lifecycleOwner: LifecycleOwner) : this() {
        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() = cancel()
        })
    }

    /**
     * This method is invoked on the UI thread before the task is executed.
     */
    open fun onPreExecute() {}

    /**
     * This method is invoked on a background thread to perform a computation that can
     * take a long time.
     *
     * @return A result, defined by the subclass of this task.
     */
    open fun doInBackground(): Result? = null

    /**
     * This class is intended for Java callers that can't use coroutines, but kotlin callers
     * can override this instead.
     */
    open suspend fun suspendableInBackground(): Result? = doInBackground()

    /**
     * This method is invoked on the UI thread after the background computation finishes.
     * The result of the background computation is passed to this step as a parameter.
     *
     * @param result The result of the operation computed by [.doInBackground].
     */
    open fun onPostExecute(result: Result?) {}

    /**
     * This method is invoked on the UI thread when the task is cancelled.
     */
    open fun onCancelled() {}

    @JvmOverloads
    fun execute(dispatcher: CoroutineDispatcher = Dispatchers.IO) {
        onPreExecute()
        job = scope.launch {
            try {
                val result = withContext(dispatcher) {
                    suspendableInBackground()
                }
                onPostExecute(result)
            } catch (e: CancellationException) {
                onCancelled()
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }

    val isCancelled: Boolean
        get() = job?.isCancelled ?: false
}
