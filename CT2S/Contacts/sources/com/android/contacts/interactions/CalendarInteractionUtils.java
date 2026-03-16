package com.android.contacts.interactions;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import com.android.contacts.R;
import java.util.Formatter;
import java.util.Locale;

public class CalendarInteractionUtils {
    public static String getDisplayedDatetime(long startMillis, long endMillis, long currentMillis, String localTimezone, boolean allDay, Context context) {
        int flagsTime = 1;
        if (DateFormat.is24HourFormat(context)) {
            flagsTime = 1 | 128;
        }
        Time currentTime = new Time(localTimezone);
        currentTime.set(currentMillis);
        Resources resources = context.getResources();
        String datetimeString = null;
        if (allDay) {
            long localStartMillis = convertAlldayUtcToLocal(null, startMillis, localTimezone);
            long localEndMillis = convertAlldayUtcToLocal(null, endMillis, localTimezone);
            if (singleDayEvent(localStartMillis, localEndMillis, currentTime.gmtoff)) {
                int todayOrTomorrow = isTodayOrTomorrow(context.getResources(), localStartMillis, currentMillis, currentTime.gmtoff);
                if (1 == todayOrTomorrow) {
                    datetimeString = resources.getString(R.string.today);
                } else if (2 == todayOrTomorrow) {
                    datetimeString = resources.getString(R.string.tomorrow);
                }
            }
            if (datetimeString == null) {
                Formatter f = new Formatter(new StringBuilder(50), Locale.getDefault());
                return DateUtils.formatDateRange(context, f, startMillis, endMillis, 18, "UTC").toString();
            }
            return datetimeString;
        }
        if (singleDayEvent(startMillis, endMillis, currentTime.gmtoff)) {
            String timeString = formatDateRange(context, startMillis, endMillis, flagsTime);
            int todayOrTomorrow2 = isTodayOrTomorrow(context.getResources(), startMillis, currentMillis, currentTime.gmtoff);
            if (1 == todayOrTomorrow2) {
                return resources.getString(R.string.today_at_time_fmt, timeString);
            }
            if (2 == todayOrTomorrow2) {
                return resources.getString(R.string.tomorrow_at_time_fmt, timeString);
            }
            String dateString = formatDateRange(context, startMillis, endMillis, 18);
            return resources.getString(R.string.date_time_fmt, dateString, timeString);
        }
        int flagsDatetime = 18 | flagsTime | 65536 | 32768;
        return formatDateRange(context, startMillis, endMillis, flagsDatetime);
    }

    private static long convertAlldayUtcToLocal(Time recycle, long utcTime, String tz) {
        if (recycle == null) {
            recycle = new Time();
        }
        recycle.timezone = "UTC";
        recycle.set(utcTime);
        recycle.timezone = tz;
        return recycle.normalize(true);
    }

    private static boolean singleDayEvent(long startMillis, long endMillis, long localGmtOffset) {
        if (startMillis == endMillis) {
            return true;
        }
        int startDay = Time.getJulianDay(startMillis, localGmtOffset);
        int endDay = Time.getJulianDay(endMillis - 1, localGmtOffset);
        return startDay == endDay;
    }

    private static int isTodayOrTomorrow(Resources r, long dayMillis, long currentMillis, long localGmtOffset) {
        int startDay = Time.getJulianDay(dayMillis, localGmtOffset);
        int currentDay = Time.getJulianDay(currentMillis, localGmtOffset);
        int days = startDay - currentDay;
        if (days == 1) {
            return 2;
        }
        return days != 0 ? 0 : 1;
    }

    private static String formatDateRange(Context context, long startMillis, long endMillis, int flags) {
        String tz;
        if ((flags & 8192) != 0) {
            tz = "UTC";
        } else {
            tz = Time.getCurrentTimezone();
        }
        StringBuilder sb = new StringBuilder(50);
        Formatter f = new Formatter(sb, Locale.getDefault());
        sb.setLength(0);
        String date = DateUtils.formatDateRange(context, f, startMillis, endMillis, flags, tz).toString();
        return date;
    }
}
