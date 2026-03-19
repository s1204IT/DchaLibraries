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
    public static String getDisplayedDatetime(long j, long j2, long j3, String str, boolean z, Context context) {
        String string;
        int i = DateFormat.is24HourFormat(context) ? 129 : 1;
        Time time = new Time(str);
        time.set(j3);
        Resources resources = context.getResources();
        if (z) {
            String string2 = null;
            long jConvertAlldayUtcToLocal = convertAlldayUtcToLocal(null, j, str);
            if (singleDayEvent(jConvertAlldayUtcToLocal, convertAlldayUtcToLocal(null, j2, str), time.gmtoff)) {
                int iIsTodayOrTomorrow = isTodayOrTomorrow(context.getResources(), jConvertAlldayUtcToLocal, j3, time.gmtoff);
                if (1 == iIsTodayOrTomorrow) {
                    string2 = resources.getString(R.string.today);
                } else if (2 == iIsTodayOrTomorrow) {
                    string2 = resources.getString(R.string.tomorrow);
                }
            }
            if (string2 == null) {
                return DateUtils.formatDateRange(context, new Formatter(new StringBuilder(50), Locale.getDefault()), j, j2, 18, "UTC").toString();
            }
            return string2;
        }
        if (singleDayEvent(j, j2, time.gmtoff)) {
            String dateRange = formatDateRange(context, j, j2, i);
            int iIsTodayOrTomorrow2 = isTodayOrTomorrow(context.getResources(), j, j3, time.gmtoff);
            if (1 == iIsTodayOrTomorrow2) {
                string = resources.getString(R.string.today_at_time_fmt, dateRange);
            } else if (2 == iIsTodayOrTomorrow2) {
                string = resources.getString(R.string.tomorrow_at_time_fmt, dateRange);
            } else {
                string = resources.getString(R.string.date_time_fmt, formatDateRange(context, j, j2, 18), dateRange);
            }
            return string;
        }
        return formatDateRange(context, j, j2, 18 | i | 65536 | 32768);
    }

    private static long convertAlldayUtcToLocal(Time time, long j, String str) {
        if (time == null) {
            time = new Time();
        }
        time.timezone = "UTC";
        time.set(j);
        time.timezone = str;
        return time.normalize(true);
    }

    private static boolean singleDayEvent(long j, long j2, long j3) {
        return j == j2 || Time.getJulianDay(j, j3) == Time.getJulianDay(j2 - 1, j3);
    }

    private static int isTodayOrTomorrow(Resources resources, long j, long j2, long j3) {
        int julianDay = Time.getJulianDay(j, j3) - Time.getJulianDay(j2, j3);
        if (julianDay == 1) {
            return 2;
        }
        if (julianDay == 0) {
            return 1;
        }
        return 0;
    }

    private static String formatDateRange(Context context, long j, long j2, int i) {
        String currentTimezone;
        if ((i & 8192) != 0) {
            currentTimezone = "UTC";
        } else {
            currentTimezone = Time.getCurrentTimezone();
        }
        String str = currentTimezone;
        StringBuilder sb = new StringBuilder(50);
        Formatter formatter = new Formatter(sb, Locale.getDefault());
        sb.setLength(0);
        return DateUtils.formatDateRange(context, formatter, j, j2, i, str).toString();
    }
}
