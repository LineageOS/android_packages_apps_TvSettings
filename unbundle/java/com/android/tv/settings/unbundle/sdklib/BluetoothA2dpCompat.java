/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;

public class BluetoothA2dpCompat {
    private final BluetoothA2dp mBluetoothA2dp;

    public BluetoothA2dpCompat(BluetoothA2dp bluetoothA2dp) {
        mBluetoothA2dp = bluetoothA2dp;
    }

    public boolean connect(BluetoothDevice device) {
        return mBluetoothA2dp.connect(device);
    }

    public boolean setPriority(BluetoothDevice device, int priority) {
        return mBluetoothA2dp.setPriority(device, priority);
    }
}
