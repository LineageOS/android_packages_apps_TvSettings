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

import android.content.Context;
import android.net.EthernetManager;
import android.net.IpConfiguration;

public class EthernetManagerCompat {
    private final EthernetManager mEthernetManager;

    public EthernetManagerCompat(Context context) {
        mEthernetManager = context.getSystemService(EthernetManager.class);
    }

    private void setConfiguration(String var1, IpConfiguration var2) {
        mEthernetManager.setConfiguration(var1, var2);
    }

    private IpConfiguration getConfiguration(String var1) {
        return mEthernetManager.getConfiguration(var1);
    }

    public String[] getAvailableInterfaces() {
        return mEthernetManager.getAvailableInterfaces();
    }
}
