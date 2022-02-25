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

import android.annotation.NonNull;
import android.bluetooth.BluetoothDevice;

public class BluetoothDeviceCompat {
    public BluetoothDeviceCompat(BluetoothDevice bluetoothDevice) {
        mBluetoothDevice = bluetoothDevice;
    }

    private final BluetoothDevice mBluetoothDevice;

    public static final int PAIRING_VARIANT_PASSKEY = BluetoothDevice.PAIRING_VARIANT_PASSKEY;
    public static final int PAIRING_VARIANT_DISPLAY_PIN =
            BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN;
    public static final int PAIRING_VARIANT_OOB_CONSENT =
            BluetoothDevice.PAIRING_VARIANT_OOB_CONSENT;
    public static final String ACTION_PAIRING_CANCEL = BluetoothDevice.ACTION_PAIRING_CANCEL;

    public boolean setPin(@NonNull String pin) {
        return mBluetoothDevice.setPin(pin);
    }
}
