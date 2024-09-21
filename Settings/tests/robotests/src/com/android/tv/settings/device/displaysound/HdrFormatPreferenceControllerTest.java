/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.tv.settings.device.displaysound;

import static android.view.Display.HdrCapabilities.HDR_TYPE_INVALID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.HdrConversionMode;
import android.view.Display;

import androidx.preference.SwitchPreference;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class HdrFormatPreferenceControllerTest {

    private SwitchPreference mPreference;
    private Context mContext;
    private HdrFormatPreferenceController mHdrPreferenceController;

    @Mock
    private DisplayManager mDisplayManager;
    @Mock
    DevicePolicyManager mDevicePolicyManager;

    // when the user enables an HDR type when Force SDR Conversion is selected, Force SDR Conversion
    // should be reset to System Preferred to allow for HDR output.  This is because
    // the most recent user action (in this case, enabling an HDR type) takes priority over
    // older actions (setting to Force SDR).
    @Test
    public void testOnPreferenceClicked_whenForceSdr_resetsHdrConversion() {
        // Setup
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mDevicePolicyManager).when(mContext).getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        mPreference = new SwitchPreference(mContext);
        mPreference.setChecked(true);
        mHdrPreferenceController = new HdrFormatPreferenceController(mContext,
                Display.HdrCapabilities.HDR_TYPE_HDR10, mDisplayManager);
        Display display = spy(Display.class);
        doReturn(display).when(mDisplayManager).getDisplay(Display.DEFAULT_DISPLAY);
        doReturn(new int[]{Display.HdrCapabilities.HDR_TYPE_HDR10}).when(mDisplayManager)
                .getUserDisabledHdrTypes();
        doNothing().when(mDisplayManager).setUserDisabledHdrTypes(new int[]{});
        doReturn(new HdrConversionMode(
                HdrConversionMode.HDR_CONVERSION_FORCE, HDR_TYPE_INVALID)).when(mDisplayManager)
                .getHdrConversionMode();
        doReturn(new HdrConversionMode(
                HdrConversionMode.HDR_CONVERSION_FORCE, HDR_TYPE_INVALID)).when(mDisplayManager)
                .getHdrConversionModeSetting();
        doNothing().when(mDisplayManager).setHdrConversionMode(
                new HdrConversionMode(HdrConversionMode.HDR_CONVERSION_SYSTEM));

        // Execution
        mHdrPreferenceController.onPreferenceClicked(mPreference);

        // Verification
        verify(mDisplayManager).setHdrConversionMode(
              new HdrConversionMode(HdrConversionMode.HDR_CONVERSION_SYSTEM));
    }

}

