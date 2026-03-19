package com.android.calendar;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.TimeFormatException;
import com.android.calendarcommon2.EventRecurrence;
import java.util.Locale;

public class EventRecurrenceFormatter {
    private static String mLocale;
    private static int[] mMonthRepeatByDayOfWeekIds;
    private static String[][] mMonthRepeatByDayOfWeekStrs;

    public static String getRepeatString(Context context, Resources resources, EventRecurrence eventRecurrence, boolean z) {
        int i;
        String strDayToString;
        String string = "";
        if (z) {
            StringBuilder sb = new StringBuilder();
            if (eventRecurrence.until != null) {
                try {
                    Time time = new Time();
                    time.parse(eventRecurrence.until);
                    sb.append(resources.getString(R.string.endByDate, DateUtils.formatDateTime(context, time.toMillis(false), 131072)));
                } catch (TimeFormatException e) {
                }
            }
            if (eventRecurrence.count > 0) {
                sb.append(resources.getQuantityString(R.plurals.endByCount, eventRecurrence.count, Integer.valueOf(eventRecurrence.count)));
            }
            string = sb.toString();
        }
        if (eventRecurrence.interval > 1) {
            i = eventRecurrence.interval;
        } else {
            i = 1;
        }
        switch (eventRecurrence.freq) {
            case 4:
                return resources.getQuantityString(R.plurals.daily, i, Integer.valueOf(i)) + string;
            case 5:
                if (eventRecurrence.repeatsOnEveryWeekDay()) {
                    return resources.getString(R.string.every_weekday) + string;
                }
                int i2 = 20;
                if (eventRecurrence.bydayCount == 1) {
                    i2 = 10;
                }
                StringBuilder sb2 = new StringBuilder();
                if (eventRecurrence.bydayCount > 0) {
                    int i3 = eventRecurrence.bydayCount - 1;
                    for (int i4 = 0; i4 < i3; i4++) {
                        sb2.append(dayToString(eventRecurrence.byday[i4], i2));
                        sb2.append(", ");
                    }
                    sb2.append(dayToString(eventRecurrence.byday[i3], i2));
                    strDayToString = sb2.toString();
                } else {
                    if (eventRecurrence.startDate == null) {
                        return null;
                    }
                    strDayToString = dayToString(EventRecurrence.timeDay2Day(eventRecurrence.startDate.weekDay), 10);
                }
                return resources.getQuantityString(R.plurals.weekly, i, Integer.valueOf(i), strDayToString) + string;
            case 6:
                if (eventRecurrence.bydayCount == 1) {
                    int i5 = eventRecurrence.startDate.weekDay;
                    cacheMonthRepeatStrings(resources, i5);
                    return resources.getString(R.string.monthly) + " (" + mMonthRepeatByDayOfWeekStrs[i5][(eventRecurrence.startDate.monthDay - 1) / 7] + ")" + string;
                }
                return resources.getString(R.string.monthly) + string;
            case 7:
                return resources.getString(R.string.yearly_plain) + string;
            default:
                return null;
        }
    }

    private static void cacheMonthRepeatStrings(Resources resources, int i) {
        boolean z = !Locale.getDefault().toString().equals(mLocale);
        if (z) {
            mLocale = Locale.getDefault().toString();
        }
        if (mMonthRepeatByDayOfWeekIds == null || z) {
            mMonthRepeatByDayOfWeekIds = new int[7];
            mMonthRepeatByDayOfWeekIds[0] = R.array.repeat_by_nth_sun;
            mMonthRepeatByDayOfWeekIds[1] = R.array.repeat_by_nth_mon;
            mMonthRepeatByDayOfWeekIds[2] = R.array.repeat_by_nth_tues;
            mMonthRepeatByDayOfWeekIds[3] = R.array.repeat_by_nth_wed;
            mMonthRepeatByDayOfWeekIds[4] = R.array.repeat_by_nth_thurs;
            mMonthRepeatByDayOfWeekIds[5] = R.array.repeat_by_nth_fri;
            mMonthRepeatByDayOfWeekIds[6] = R.array.repeat_by_nth_sat;
        }
        if (mMonthRepeatByDayOfWeekStrs == null) {
            mMonthRepeatByDayOfWeekStrs = new String[7][];
        }
        if (mMonthRepeatByDayOfWeekStrs[i] == null || z) {
            mMonthRepeatByDayOfWeekStrs[i] = resources.getStringArray(mMonthRepeatByDayOfWeekIds[i]);
        }
    }

    private static String dayToString(int i, int i2) {
        return DateUtils.getDayOfWeekString(dayToUtilDay(i), i2);
    }

    private static int dayToUtilDay(int i) {
        if (i == 65536) {
            return 1;
        }
        if (i == 131072) {
            return 2;
        }
        if (i == 262144) {
            return 3;
        }
        if (i == 524288) {
            return 4;
        }
        if (i == 1048576) {
            return 5;
        }
        if (i == 2097152) {
            return 6;
        }
        if (i == 4194304) {
            return 7;
        }
        throw new IllegalArgumentException("bad day argument: " + i);
    }
}
