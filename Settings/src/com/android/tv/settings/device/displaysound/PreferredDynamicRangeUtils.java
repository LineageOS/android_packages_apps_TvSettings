/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION;

import android.hardware.display.DisplayManager;
import android.hardware.display.HdrConversionMode;
import android.view.Display;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper methods to set and get dynamic range setting.
 *
 * @hide
 */
public class PreferredDynamicRangeUtils {

    /** Gets the match-content dynamic range status */
    public static boolean getMatchContentDynamicRangeStatus(DisplayManager displayManager) {
        return displayManager.getHdrConversionModeSetting().getConversionMode()
                == HdrConversionMode.HDR_CONVERSION_PASSTHROUGH;
    }

    /** Sets the match-content dynamic range status */
    public static void setMatchContentDynamicRangeStatus(DisplayManager displayManager,
            boolean status) {
        HdrConversionMode mode = status
                ? new HdrConversionMode(HdrConversionMode.HDR_CONVERSION_PASSTHROUGH)
                : new HdrConversionMode(HdrConversionMode.HDR_CONVERSION_SYSTEM);

        displayManager.setHdrConversionMode(mode);
    }

    /** Returns if Dolby vision is supported by the device */
    public static boolean isDolbyVisionSupported(Display.Mode[] modes) {
        for (int i = 0; i < modes.length; i++) {
            if (isDolbyVisionSupported(modes[i])) {
                return true;
            }
        }
        return false;
    }

    /** Returns if Dolby vision is supported by the device in case of a specific mode */
    public static boolean isDolbyVisionSupported(Display.Mode mode) {
        return Arrays.stream(mode.getSupportedHdrTypes()).anyMatch(
                hdr -> hdr == HDR_TYPE_DOLBY_VISION);
    }

    /** Converts set to int array */
    public static int[] toArray(Set<Integer> set) {
        return set.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Converts int array to set */
    public static Set<Integer> toSet(int[] array) {
        return Arrays.stream(array).boxed().collect(Collectors.toSet());
    }
}
