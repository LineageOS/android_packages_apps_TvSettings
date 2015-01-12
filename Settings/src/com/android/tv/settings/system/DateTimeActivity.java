/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.tv.settings.system;

import android.annotation.NonNull;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.format.DateFormat;
import android.util.Log;

import com.android.tv.settings.R;
import com.android.tv.settings.dialog.Layout;
import com.android.tv.settings.dialog.SettingsLayoutActivity;
import com.android.tv.settings.util.SettingsHelper;

import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

public class DateTimeActivity extends SettingsLayoutActivity {

    private static final String TAG = "DateTimeActivity";
    private static final boolean DEBUG = false;

    private static final String HOURS_12 = "12";
    private static final String HOURS_24 = "24";

    private static final String XMLTAG_TIMEZONE = "timezone";

    private static final int ACTION_AUTO_TIME_ON = 0;
    private static final int ACTION_AUTO_TIME_OFF = 1;
    private static final int ACTION_24HOUR_FORMAT_ON = 3;
    private static final int ACTION_24HOUR_FORMAT_OFF = 4;
    private static final int ACTION_SET_TIMEZONE_BASE = 1<<10;


    private Calendar mDummyDate;
    private boolean mIsResumed;

    private String mNowDate;
    private String mNowTime;

    private SettingsHelper mHelper;

    private final BroadcastReceiver mTimeUpdateIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mIsResumed) {
                updateTimeAndDateStrings();
            }
        }
    };

    private final Layout.StringGetter mTimeStringGetter = new Layout.StringGetter() {
        @Override
        public String get() {
            return mNowTime;
        }
    };

    private final Layout.StringGetter mDateStringGetter = new Layout.StringGetter() {
        @Override
        public String get() {
            return mNowDate;
        }
    };

    private final Layout.StringGetter mTimezoneStringGetter = new Layout.StringGetter() {
        @Override
        public String get() {
            final Calendar now = Calendar.getInstance();
            TimeZone tz = now.getTimeZone();

            Date date = new Date();
            // TODO: this isn't very i18n friendly b/18905558
            return formatOffset(tz.getOffset(date.getTime())) + ", " +
                    tz.getDisplayName(tz.inDaylightTime(date), TimeZone.LONG);
        }
    };

    private final Layout.StringGetter mTimeFormatStringGetter = new Layout.StringGetter() {
        @Override
        public String get() {
            String status = mHelper.getStatusStringFromBoolean(isTimeFormat24h());
            return String.format("%s (%s)", status,
                    DateFormat.getTimeFormat(DateTimeActivity.this).format(mDummyDate.getTime()));
        }
    };

    private final Layout.LayoutGetter mSetDateLayoutGetter = new Layout.LayoutGetter() {
        @Override
        public Layout get() {
            final Resources res = getResources();
            return new Layout()
                    .add(new Layout.Header.Builder(res)
                            .title(R.string.system_date)
                            .description(mDateStringGetter)
                            .enabled(!getAutoState(Settings.Global.AUTO_TIME))
                            .build()
                            .add(new Layout.Action.Builder(res,
                                    SetDateTimeActivity.getSetDateIntent(DateTimeActivity.this))
                                    .title(R.string.system_set_date)
                                    .description(mDateStringGetter)
                                    .build()));
        }
    };

    private final Layout.LayoutGetter mSetTimeLayoutGetter = new Layout.LayoutGetter() {
        @Override
        public Layout get() {
            final Resources res = getResources();
            return new Layout()
                    .add(new Layout.Action.Builder(res,
                            SetDateTimeActivity.getSetTimeIntent(DateTimeActivity.this))
                            .title(R.string.system_set_time)
                            .description(mTimeStringGetter)
                            .enabled(!getAutoState(Settings.Global.AUTO_TIME))
                            .build());

        }
    };

    private Layout.SelectionGroup mAutoDateTimeSelector;
    private Layout.SelectionGroup mTimezoneSelector;
    private Layout.SelectionGroup mTimeFormatSelector;

    private ArrayList<TimeZoneInfo> mTimeZones;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mDummyDate = Calendar.getInstance();

        mHelper = new SettingsHelper(getApplicationContext());

        // Auto date time
        mAutoDateTimeSelector = new Layout.SelectionGroup.Builder(2)
                .add(getString(R.string.action_on_description), null, ACTION_AUTO_TIME_ON)
                .add(getString(R.string.action_off_description), null, ACTION_AUTO_TIME_OFF)
                .build();
        mAutoDateTimeSelector.setSelected(getAutoState(Settings.Global.AUTO_TIME) ?
                ACTION_AUTO_TIME_ON : ACTION_AUTO_TIME_OFF);

        // Time zone
        mTimeZones = getTimeZones(this);
        // Sort the Time Zones list in ascending offset order
        Collections.sort(mTimeZones);
        final TimeZone currentTz = TimeZone.getDefault();

        final Layout.SelectionGroup.Builder tzBuilder =
                new Layout.SelectionGroup.Builder(mTimeZones.size());

        int i = ACTION_SET_TIMEZONE_BASE;
        int currentTzActionId = -1;
        for (final TimeZoneInfo tz : mTimeZones) {
            if (currentTz.getID().equals(tz.tzId)) {
                currentTzActionId = i;
            }
            tzBuilder.add(tz.tzName, formatOffset(tz.tzOffset), i);
            i++;
        }

        mTimezoneSelector = tzBuilder.build();
        mTimezoneSelector.setSelected(currentTzActionId);

        // Time Format
        mTimeFormatSelector = new Layout.SelectionGroup.Builder(2)
                .add(getString(R.string.settings_on), null, ACTION_24HOUR_FORMAT_ON)
                .add(getString(R.string.settings_off), null, ACTION_24HOUR_FORMAT_OFF)
                .build();

        setSampleDate();

        updateTimeAndDateStrings();

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsResumed = true;

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);

        registerReceiver(mTimeUpdateIntentReceiver, filter);

        updateTimeAndDateStrings();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsResumed = false;
        unregisterReceiver(mTimeUpdateIntentReceiver);
    }

    @Override
    public Layout createLayout() {
        final Resources res = getResources();
        return new Layout().breadcrumb(getString(R.string.header_category_preferences))
                .add(new Layout.Header.Builder(res)
                        .icon(R.drawable.ic_settings_datetime)
                        .title(R.string.system_date_time)
                        .build()
                        .add(new Layout.Header.Builder(res)
                                .title(R.string.system_auto_date_time)
                                .description(mAutoDateTimeSelector)
                                .build()
                                .add(mAutoDateTimeSelector))
                        .add(mSetDateLayoutGetter)
                        .add(new Layout.Header.Builder(res)
                                .title(R.string.system_time)
                                .description(mTimeStringGetter)
                                .build()
                                .add(mSetTimeLayoutGetter)
                                .add(new Layout.Header.Builder(res)
                                        .title(R.string.system_set_time_zone)
                                        .description(mTimezoneStringGetter)
                                        .build()
                                        .add(mTimezoneSelector))
                                .add(new Layout.Header.Builder(res)
                                        .title(R.string.system_set_time_format)
                                        .description(mTimeFormatStringGetter)
                                        .build()
                                        .add(mTimeFormatSelector))));
    }

    private void setSampleDate() {
        Calendar now = Calendar.getInstance();
        mDummyDate.setTimeZone(now.getTimeZone());
        // We use December 31st because it's unambiguous when demonstrating the date format.
        // We use 15:14 so we can demonstrate the 12/24 hour options.
        mDummyDate.set(now.get(Calendar.YEAR), 11, 31, 15, 14, 0);
    }

    private boolean getAutoState(String name) {
        try {
            return Settings.Global.getInt(getContentResolver(), name) > 0;
        } catch (SettingNotFoundException snfe) {
            return false;
        }
    }


    private boolean isTimeFormat24h() {
        return DateFormat.is24HourFormat(this);
    }

    private void setTime24Hour(boolean is24Hour) {
        Settings.System.putString(getContentResolver(),
                Settings.System.TIME_12_24,
                is24Hour ? HOURS_24 : HOURS_12);
        updateTimeAndDateStrings();
    }

    private void setAutoDateTime(boolean on) {
        Settings.Global.putInt(getContentResolver(), Settings.Global.AUTO_TIME, on ? 1 : 0);
    }

    // Updates the member strings to reflect the current date and time.
    private void updateTimeAndDateStrings() {
        final Calendar now = Calendar.getInstance();
        java.text.DateFormat dateFormat = DateFormat.getDateFormat(this);
        mNowDate = dateFormat.format(now.getTime());
        java.text.DateFormat timeFormat = DateFormat.getTimeFormat(this);
        mNowTime = timeFormat.format(now.getTime());

        mDateStringGetter.refreshView();
        mTimeStringGetter.refreshView();
    }

    @Override
    public void onActionClicked(Layout.Action action) {
        final int actionId = action.getId();
        switch (actionId) {
            case Layout.Action.ACTION_INTENT:
                startActivity(action.getIntent());
                break;
            case ACTION_AUTO_TIME_ON:
                setAutoDateTime(true);
                mSetDateLayoutGetter.refreshView();
                mSetTimeLayoutGetter.refreshView();
                break;
            case ACTION_AUTO_TIME_OFF:
                setAutoDateTime(false);
                mSetDateLayoutGetter.refreshView();
                mSetTimeLayoutGetter.refreshView();
                break;
            case ACTION_24HOUR_FORMAT_ON:
                setTime24Hour(true);
                break;
            case ACTION_24HOUR_FORMAT_OFF:
                setTime24Hour(false);
                break;
            default:
                if ((actionId & ACTION_SET_TIMEZONE_BASE) != 0) {
                    setTimeZone(mTimeZones.get(actionId - ACTION_SET_TIMEZONE_BASE).tzId);
                }
                break;
        }
    }

    /**
     * Formats the provided timezone offset into a string of the form GMT+XX:XX
     */
    private static String formatOffset(long offset) {
        long off = offset / 1000 / 60;
        final StringBuilder sb = new StringBuilder();

        sb.append("GMT");
        if (off < 0) {
            sb.append('-');
            off = -off;
        } else {
            sb.append('+');
        }

        int hours = (int) (off / 60);
        int minutes = (int) (off % 60);

        sb.append((char) ('0' + hours / 10));
        sb.append((char) ('0' + hours % 10));

        sb.append(':');

        sb.append((char) ('0' + minutes / 10));
        sb.append((char) ('0' + minutes % 10));

        return sb.toString();
    }

    /**
     * Helper class to hold the time zone data parsed from the Time Zones XML
     * file.
     */
    private class TimeZoneInfo implements Comparable<TimeZoneInfo> {
        public final String tzId;
        public final String tzName;
        public final long tzOffset;

        public TimeZoneInfo(String id, String name, long offset) {
            tzId = id;
            tzName = name;
            tzOffset = offset;
        }

        @Override
        public int compareTo(@NonNull TimeZoneInfo another) {
            return (int) (tzOffset - another.tzOffset);
        }
    }

    /**
     * Parses the XML time zone information into an array of TimeZoneInfo
     * objects.
     */
    private ArrayList<TimeZoneInfo> getTimeZones(Context context) {
        ArrayList<TimeZoneInfo> timeZones = new ArrayList<>();
        final long date = Calendar.getInstance().getTimeInMillis();
        try {
            XmlResourceParser xrp = context.getResources().getXml(R.xml.timezones);
            while (xrp.next() != XmlResourceParser.START_TAG)
                continue;
            xrp.next();
            while (xrp.getEventType() != XmlResourceParser.END_TAG) {
                while (xrp.getEventType() != XmlResourceParser.START_TAG &&
                        xrp.getEventType() != XmlResourceParser.END_DOCUMENT) {
                    xrp.next();
                }

                if (xrp.getEventType() == XmlResourceParser.END_DOCUMENT) {
                    break;
                }

                if (xrp.getName().equals(XMLTAG_TIMEZONE)) {
                    String id = xrp.getAttributeValue(0);
                    String displayName = xrp.nextText();
                    TimeZone tz = TimeZone.getTimeZone(id);
                    long offset;
                    if (tz != null) {
                        offset = tz.getOffset(date);
                        timeZones.add(new TimeZoneInfo(id, displayName, offset));
                    } else {
                        continue;
                    }
                }
                while (xrp.getEventType() != XmlResourceParser.END_TAG) {
                    xrp.next();
                }
                xrp.next();
            }
            xrp.close();
        } catch (XmlPullParserException xppe) {
            Log.e(TAG, "Ill-formatted timezones.xml file");
        } catch (java.io.IOException ioe) {
            Log.e(TAG, "Unable to read timezones.xml file");
        }
        return timeZones;
    }

    private void setTimeZone(String tzId) {
        // Update the system timezone value
        final AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.setTimeZone(tzId);

        setSampleDate();
    }
}
