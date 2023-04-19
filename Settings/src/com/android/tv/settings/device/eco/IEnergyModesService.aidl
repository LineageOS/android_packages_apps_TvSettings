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

package com.android.tv.settings.device.eco;

import android.os.Bundle;

/** {@hide} */
interface IEnergyModesService {
    /** Key for a boolean representing whether the energy mode is currently selected. */
    const String KEY_SELECTED = "selected";

    /** Key for a String representing the identifier for an energy mode. */
    const String KEY_IDENTIFIER = "identifier";

    /** Key for an Icon representing the icon of an energy mode. */
    const String KEY_ICON = "icon";

    /** Key for an int representing the color of an energy mode (in ARGB). */
    const String KEY_COLOR = "color";

    /** Key for a String representing the title of an energy mode. */
    const String KEY_TITLE = "title";

    /** Key for a String representing the subtitle of an energy mode. */
    const String KEY_SUBTITLE = "subtitle";

    /** Key for a String representing the description of an energy mode. */
    const String KEY_DESCRIPTION = "description";

    /** Key for a String representing a short description of an energy mode. */
    const String KEY_SHORT_DESCRIPTION = "short_description";

    /** Key for a String array representing the (human-friendly) features of an energy mode. */
    const String KEY_FEATURES_LIST = "features_list";

    /** Key for a String array representing the features of an energy mode. */
    const String KEY_FEATURES = "features";

    /** Returns the list of available energy modes */
    List<Bundle> getModes();

    /** Sets the selected energy mode by the given identifier */
    @EnforcePermission("MANAGE_LOW_POWER_STANDBY")
    void setMode(String identifier);

    /** Returns the identifier of the default energy mode */
    String getDefaultMode();
}
