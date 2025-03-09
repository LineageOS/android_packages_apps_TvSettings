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
package com.android.tv.settings.connectivity.util;


import android.content.Context;
import android.content.pm.PackageManager;
import android.net.thread.ThreadNetworkController;
import android.net.thread.ThreadNetworkException;
import android.net.thread.ThreadNetworkManager;
import android.os.OutcomeReceiver;

import java.util.List;
import java.util.concurrent.Executor;
/**
 * Helper class to call thread network SDK APIs
 */
public class ThreadNetworkHelper {

    private static final String TAG = "ThreadNetworkHelper";

    // StateCallback and methods
    private final Executor mExecutor;
    private final ThreadNetworkController mThreadNetworkController;
    private final ThreadNetworkController.StateCallback mStateCallback;
    private final OutcomeReceiver<Void, ThreadNetworkException> mThreadNetworkOutComeReceiver =
            new OutcomeReceiver<>() {
                @Override
                public void onResult(Void result) {
                    // Do nothing, state is handled with mStateCallback
                }
            };
    private OnStateChangeListener mOnStateChangeListener;

    private ThreadNetworkHelper(ThreadNetworkController threadNetworkController,
            Executor executor) {
        this.mThreadNetworkController = threadNetworkController;
        this.mExecutor = executor;
        this.mStateCallback = getStateCallback();
    }

    /** Creates new instance if Thread Network service is available or else null */
    public static ThreadNetworkHelper getInstance(Context context) {
        if (context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_THREAD_NETWORK)) {
            ThreadNetworkManager threadNetworkManager =
                    context.getSystemService(ThreadNetworkManager.class);
            if (threadNetworkManager != null) {
                List<ThreadNetworkController> networkControllers =
                        threadNetworkManager.getAllThreadNetworkControllers();
                if (!networkControllers.isEmpty()) {
                    return new ThreadNetworkHelper(
                            networkControllers.getFirst(), context.getMainExecutor());
                }
            }
        }
        return null;
    }

    private ThreadNetworkController.StateCallback getStateCallback() {
        return new ThreadNetworkController.StateCallback() {
            @Override
            public void onDeviceRoleChanged(int i) {

            }

            @Override
            public void onThreadEnableStateChanged(int enabledState) {
                // There might be race condition when listener is detached so check
                // if listener is available
                if (mOnStateChangeListener != null) {
                    mOnStateChangeListener.isEnabled(
                            enabledState == ThreadNetworkController.STATE_ENABLED);
                }
            }
        };
    }

    /** Registers the StateCallback to ThreadNetworkController */
    public void registerStateCallback() {
        mThreadNetworkController.registerStateCallback(mExecutor, mStateCallback);
    }

    /** Unregisters the StateCallback to ThreadNetworkController */
    public void unregisterStateCallback() {
        mThreadNetworkController.unregisterStateCallback(mStateCallback);
    }

    /** Enables/Disables Thread Network */
    public void setEnabled(boolean enable) {
        mThreadNetworkController.setEnabled(enable, mExecutor, mThreadNetworkOutComeReceiver);
    }

    /** Sets the OnStateChangeListener */
    public void setOnStateChangeListener(OnStateChangeListener onStateChangeListener) {
        this.mOnStateChangeListener = onStateChangeListener;
    }

    /** Listener to notify state of Thread Network */
    public interface OnStateChangeListener {
        void isEnabled(boolean enabled);
    }
}
