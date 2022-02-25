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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidHost;

public class BluetoothHidHostCompat {
    private final BluetoothHidHost mBluetoothHidHost;

    BluetoothHidHostCompat(BluetoothHidHost bluetoothHidHost) {
        mBluetoothHidHost = bluetoothHidHost;
    }

    public boolean connect(BluetoothDevice device) {
        return mBluetoothHidHost.connect(device);
    }

    public boolean disconnect(BluetoothDevice device) {
        return mBluetoothHidHost.disconnect(device);
    }

    public boolean setPriority(BluetoothDevice device, int priority) {
        return mBluetoothHidHost.setPriority(device, priority);
    }
}
