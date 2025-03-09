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

package com.android.tv.settings.accessories;

import android.bluetooth.BluetoothLeAudio;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothLeAudioConnector implements BluetoothDevicePairer.BluetoothConnector {

    public static final String TAG = "BluetoothLeAudioConnector";

    private static final boolean DEBUG = true;

    private static final int MSG_CONNECT_TIMEOUT = 1;
    private static final int MSG_CONNECT = 2;

    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int CONNECT_DELAY = 10;

    private Context mContext;
    private BluetoothDevice mTarget;
    private BluetoothDevicePairer.OpenConnectionCallback mOpenConnectionCallback;
    private BluetoothLeAudio mLeAudioProfile;
    private boolean mConnectionStateReceiverRegistered = false;

    // TODO: Refactor this handler in both Connector classes
    @SuppressWarnings("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message m) {
            switch (m.what) {
                case MSG_CONNECT_TIMEOUT:
                    Log.w(TAG, "handleMessage(MSG_CONNECT_TIMEOUT)");
                    failed();
                    break;
                case MSG_CONNECT:
                    if (mLeAudioProfile == null) {
                        break;
                    }
                    // must set CONNECTION_POLICY_ALLOWED or auto-connection will not
                    // occur, however this setting does not appear to be sticky
                    // across a reboot
                    Log.i(TAG, "handleMessage(MSG_CONNECT)");
                    mLeAudioProfile.setConnectionPolicy(mTarget,
                        BluetoothProfile.CONNECTION_POLICY_ALLOWED);
                    break;
                default:
                    Log.d(TAG, "handleMessage(" + m.what + "): unhandled");
                    break;
            }
        }
    };

    private BroadcastReceiver mConnectionStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device =
                    (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (DEBUG) {
                Log.d(TAG, "There was a connection status change for: " + device.getAddress());
            }

            if (!device.equals(mTarget)) {
                return;
            }

            if (BluetoothDevice.ACTION_UUID.equals(intent.getAction())) {
                // regardless of the UUID content, at this point, we're sure we can initiate a
                // profile connection.
                Log.d(TAG,
                    "mHandler.sendEmptyMessageDelayed(MSG_CONNECT_TIMEOUT, CONNECT_TIMEOUT_MS)");
                mHandler.sendEmptyMessageDelayed(MSG_CONNECT_TIMEOUT, CONNECT_TIMEOUT_MS);
                if (!mHandler.hasMessages(MSG_CONNECT)) {
                    Log.d(TAG,
                        "mHandler.sendEmptyMessageDelayed(MSG_CONNECT, CONNECT_DELAY) now 10");
                    mHandler.sendEmptyMessageDelayed(MSG_CONNECT, CONNECT_DELAY);
                }
            } else { // BluetoothLeAudio.ACTION_CONNECTION_STATE_CHANGED

                int previousState = intent.getIntExtra(
                        BluetoothLeAudio.EXTRA_PREVIOUS_STATE, BluetoothLeAudio.STATE_CONNECTING);
                int state = intent.getIntExtra(
                        BluetoothLeAudio.EXTRA_STATE, BluetoothLeAudio.STATE_CONNECTING);

                if (DEBUG) {
                    Log.d(TAG, "Connection states: old = " + previousState + ", new = " + state);
                }

                if (previousState == BluetoothLeAudio.STATE_CONNECTING) {
                    if (state == BluetoothLeAudio.STATE_CONNECTED) {
                        Log.i(TAG, "onReceive(): connected");
                        succeeded();
                    } else if (state == BluetoothLeAudio.STATE_DISCONNECTED) {
                        Log.e(TAG, "onReceive(): Failed to connect");
                        failed();
                    }

                    // TODO: Evaluate the correct action for LE Audio

                    Log.d(TAG, "Normally we would unregister and close here: "
                        + device.getAddress());
                    //unregisterConnectionStateReceiver();
                    //closeLeAudioProfileProxy();
                }
            }
        }
    };

    private void succeeded() {
        Log.d(TAG, "succeeded()");
        mHandler.removeCallbacksAndMessages(null);
        mOpenConnectionCallback.succeeded();
    }

    private void failed() {
        Log.e(TAG, "failed()");
        mHandler.removeCallbacksAndMessages(null);
        mOpenConnectionCallback.failed();
    }

    private BluetoothProfile.ServiceListener mServiceConnection =
        new BluetoothProfile.ServiceListener() {

            @Override
            public void onServiceDisconnected(int profile) {
                Log.d(TAG, "onServiceDisconnected(" + profile + ")");
                unregisterConnectionStateReceiver();
            }

            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (DEBUG) {
                    Log.d(TAG, "Connection made to bluetooth proxy." );
                }
                mLeAudioProfile = (BluetoothLeAudio) proxy;
                Log.d(TAG, "onServiceConnected(" + profile
                        +
                        ", ...): Connecting to target: " + mTarget.getAddress());

                registerConnectionStateReceiver();
                // We initiate SDP because connecting to A2DP
                // before services are discovered leads to error.
                mTarget.fetchUuidsWithSdp();
            }
        };


    private BluetoothLeAudioConnector() {
    }

    public BluetoothLeAudioConnector(Context context, BluetoothDevice target,
                                  BluetoothDevicePairer.OpenConnectionCallback callback) {
        mContext = context;
        mTarget = target;
        mOpenConnectionCallback = callback;
    }

    @Override
    public void openConnection(BluetoothAdapter adapter) {
        if (DEBUG) {
            Log.d(TAG, "opening connection");
        }
        if (!adapter.getProfileProxy(mContext, mServiceConnection, BluetoothProfile.LE_AUDIO)) {
            failed();
        }
    }

    @Override
    public void dispose() {
        unregisterConnectionStateReceiver();
        closeLeAudioProfileProxy();
    }

    private void closeLeAudioProfileProxy() {
        mHandler.removeCallbacksAndMessages(null);
        if (mLeAudioProfile != null) {
            try {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                adapter.closeProfileProxy(BluetoothProfile.LE_AUDIO, mLeAudioProfile);
                mLeAudioProfile = null;
            } catch (Throwable t) {
                Log.w(TAG, "Error cleaning up LeAudio proxy", t);
            }
        }
    }

    private void registerConnectionStateReceiver() {
        Log.d(TAG, "registerConnectionStateReceiver()");
        IntentFilter filter =
            new IntentFilter(BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        mContext.registerReceiver(mConnectionStateReceiver, filter);
        mConnectionStateReceiverRegistered = true;
    }

    private void unregisterConnectionStateReceiver() {
        if (mConnectionStateReceiverRegistered) {
            Log.d(TAG, "unregisterConnectionStateReceiver()");
            mContext.unregisterReceiver(mConnectionStateReceiver);
            mConnectionStateReceiverRegistered = false;
        }
    }
}
