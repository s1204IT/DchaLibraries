package com.android.calendar;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.TimeFormatException;
import com.android.calendarcommon2.EventRecurrence;

public class EventRecurrenceFormatter {
    private static int[] mMonthRepeatByDayOfWeekIds;
    private static String[][] mMonthRepeatByDayOfWeekStrs;

    public static String getRepeatString(Context context, Resources r, EventRecurrence recurrence, boolean includeEndString) {
        String string;
        String endString = "";
        if (includeEndString) {
            StringBuilder sb = new StringBuilder();
            if (recurrence.until != null) {
                try {
                    Time t = new Time();
                    t.parse(recurrence.until);
                    String dateStr = DateUtils.formatDateTime(context, t.toMillis(false), 131072);
                    sb.append(r.getString(R.string.endByDate, dateStr));
                } catch (TimeFormatException e) {
                }
            }
            if (recurrence.count > 0) {
                sb.append(r.getQuantityString(R.plurals.endByCount, recurrence.count, Integer.valueOf(recurrence.count)));
            }
            endString = sb.toString();
        }
        int interval = recurrence.interval <= 1 ? 1 : recurrence.interval;
        switch (recurrence.freq) {
            case 4:
                return r.getQuantityString(R.plurals.daily, interval, Integer.valueOf(interval)) + endString;
            case 5:
                if (recurrence.repeatsOnEveryWeekDay()) {
                    return r.getString(R.string.every_weekday) + endString;
                }
                int dayOfWeekLength = 20;
                if (recurrence.bydayCount == 1) {
                    dayOfWeekLength = 10;
                }
                StringBuilder days = new StringBuilder();
                if (recurrence.bydayCount > 0) {
                    int count = recurrence.bydayCount - 1;
                    for (int i = 0; i < count; i++) {
                        days.append(dayToString(recurrence.byday[i], dayOfWeekLength));
                        days.append(", ");
                    }
                    days.append(dayToString(recurrence.byday[count], dayOfWeekLength));
                    string = days.toString();
                } else {
                    if (recurrence.startDate == null) {
                        return null;
                    }
                    int day = EventRecurrence.timeDay2Day(recurrence.startDate.weekDay);
                    string = dayToString(day, 10);
                }
                return r.getQuantityString(R.plurals.weekly, interval, Integer.valueOf(interval), string) + endString;
            case 6:
                if (recurrence.bydayCount == 1) {
                    int weekday = recurrence.startDate.weekDay;
                    cacheMonthRepeatStrings(r, weekday);
                    int dayNumber = (recurrence.startDate.monthDay - 1) / 7;
                    return r.getString(R.string.monthly) + " (" + mMonthRepeatByDayOfWeekStrs[weekday][dayNumber] + ")" + endString;
                }
                return r.getString(R.string.monthly) + endString;
            case 7:
                return r.getString(R.string.yearly_plain) + endString;
            default:
                return null;
        }
    }

    private static void cacheMonthRepeatStrings(Resources r, int weekday) {
        if (mMonthRepeatByDayOfWeekIds == null) {
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
        if (mMonthRepeatByDayOfWeekStrs[weekday] == null) {
            mMonthRepeatByDayOfWeekStrs[weekday] = r.getStringArray(mMonthRepeatByDayOfWeekIds[weekday]);
        }
    }

    private static String dayToString(int day, int dayOfWeekLength) {
        return DateUtils.getDayOfWeekString(dayToUtilDay(day), dayOfWeekLength);
    }

    private static int dayToUtilDay(int day) {
        switch (day) {
            case 65536:
                return 1;
            case 131072:
                return 2;
            case 262144:
                return 3;
            case 524288:
                return 4;
            case 1048576:
                return 5;
            case 2097152:
                return 6;
            case 4194304:
                return 7;
            default:
                throw new IllegalArgumentException("bad day argument: " + day);
        }
    }
}
