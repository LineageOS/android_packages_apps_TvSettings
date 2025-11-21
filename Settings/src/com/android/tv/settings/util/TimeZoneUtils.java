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

package com.android.tv.settings.util;

import android.content.Context;

import com.android.settingslib.datetime.ZoneGetter;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Utility class for TimeZone related operations.
 */
public class TimeZoneUtils {

    /**
     * Returns the display name for the given TimeZone, matching the logic in TimeZoneFragment.
     *
     * @param context The context.
     * @param tz      The TimeZone to get the name for.
     * @param now     The current date for offset calculation fallback.
     * @return The display name.
     */
    public static CharSequence getTimeZoneDisplayName(Context context, TimeZone tz, Date now) {
        final String id = tz.getID();
        final List<Map<String, Object>> zoneList = ZoneGetter.getZonesList(context);
        for (final Map<String, Object> zone : zoneList) {
            if (id.equals(zone.get(ZoneGetter.KEY_ID))) {
                String displayName = (String) zone.get(ZoneGetter.KEY_DISPLAYNAME);
                String gmt = (String) zone.get(ZoneGetter.KEY_GMT);
                return gmt + " " + displayName;
            }
        }
        return ZoneGetter.getTimeZoneOffsetAndName(context, tz, now);
    }

    private TimeZoneUtils() {
        // Utility class
    }
}
