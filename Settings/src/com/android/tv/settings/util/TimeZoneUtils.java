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
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

import com.android.tv.settings.R;

/**
 * Utility class for TimeZone related operations.
 */
public class TimeZoneUtils {
    private static final AtomicReference<Map<String, String>> sOverriddenCache =
            new AtomicReference<>();

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
        String overriddenName = getOverriddenDisplayName(context, id);

        final List<Map<String, Object>> zoneList = ZoneGetter.getZonesList(context);
        // Search the list in reverse to prefer overlay entries (which are usually
        // appended)
        // over base entries in case of duplicate IDs defined in XML.
        for (int i = zoneList.size() - 1; i >= 0; i--) {
            final Map<String, Object> zone = zoneList.get(i);
            if (id.equals(zone.get(ZoneGetter.KEY_ID))) {
                String gmt = (String) zone.get(ZoneGetter.KEY_GMT);
                if (overriddenName != null) {
                    return gmt + " " + overriddenName;
                }
                String displayName = (String) zone.get(ZoneGetter.KEY_DISPLAYNAME);
                return gmt + " " + displayName;
            }
        }
        if (overriddenName != null) {
            return overriddenName;
        }
        return ZoneGetter.getTimeZoneOffsetAndName(context, tz, now);
    }

    /**
     * Returns the overridden display name for the given timezone ID, or null if not
     * overridden.
     */
    @androidx.annotation.Nullable
    public static String getOverriddenDisplayName(Context context, String id) {
        Map<String, String> cache = sOverriddenCache.get();
        if (cache != null) {
            return cache.get(id);
        }
        Map<String, String> map = new HashMap<>();
        String[] ids = context.getResources().getStringArray(R.array.config_overridden_timezones);
        String[] names = context.getResources().getStringArray(
                R.array.config_overridden_timezones_names);
        if (ids.length == names.length) {
            for (int i = 0; i < ids.length; i++) {
                map.put(ids[i], names[i]);
            }
        }
        sOverriddenCache.compareAndSet(null, map);
        return map.get(id);
    }

    private TimeZoneUtils() {
        // Utility class
    }
}
