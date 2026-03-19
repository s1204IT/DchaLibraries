package libcore.icu;

import android.icu.impl.JavaTimeZone;
import android.icu.text.DateFormat;
import android.icu.util.Calendar;
import android.icu.util.GregorianCalendar;
import android.icu.util.TimeZone;
import android.icu.util.ULocale;

public final class DateUtilsBridge {
    public static final int FORMAT_12HOUR = 64;
    public static final int FORMAT_24HOUR = 128;
    public static final int FORMAT_ABBREV_ALL = 524288;
    public static final int FORMAT_ABBREV_MONTH = 65536;
    public static final int FORMAT_ABBREV_RELATIVE = 262144;
    public static final int FORMAT_ABBREV_TIME = 16384;
    public static final int FORMAT_ABBREV_WEEKDAY = 32768;
    public static final int FORMAT_NO_MONTH_DAY = 32;
    public static final int FORMAT_NO_YEAR = 8;
    public static final int FORMAT_NUMERIC_DATE = 131072;
    public static final int FORMAT_SHOW_DATE = 16;
    public static final int FORMAT_SHOW_TIME = 1;
    public static final int FORMAT_SHOW_WEEKDAY = 2;
    public static final int FORMAT_SHOW_YEAR = 4;
    public static final int FORMAT_UTC = 8192;

    public static TimeZone icuTimeZone(java.util.TimeZone timeZone) {
        JavaTimeZone javaTimeZone = new JavaTimeZone(timeZone, null);
        javaTimeZone.freeze();
        return javaTimeZone;
    }

    public static Calendar createIcuCalendar(TimeZone timeZone, ULocale uLocale, long j) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar(timeZone, uLocale);
        gregorianCalendar.setTimeInMillis(j);
        return gregorianCalendar;
    }

    public static String toSkeleton(Calendar calendar, int i) {
        return toSkeleton(calendar, calendar, i);
    }

    public static String toSkeleton(Calendar calendar, Calendar calendar2, int i) {
        if ((524288 & i) != 0) {
            i |= 114688;
        }
        String str = DateFormat.MONTH;
        if ((131072 & i) != 0) {
            str = DateFormat.NUM_MONTH;
        } else if ((65536 & i) != 0) {
            str = DateFormat.ABBR_MONTH;
        }
        String str2 = DateFormat.WEEKDAY;
        if ((32768 & i) != 0) {
            str2 = "EEE";
        }
        String str3 = DateFormat.HOUR;
        int i2 = i & 128;
        if (i2 != 0) {
            str3 = DateFormat.HOUR24;
        } else if ((i & 64) != 0) {
            str3 = "h";
        }
        if ((i & 16384) == 0 || i2 != 0) {
            str3 = str3 + DateFormat.MINUTE;
        } else if (!onTheHour(calendar) || !onTheHour(calendar2)) {
            str3 = str3 + DateFormat.MINUTE;
        }
        if (fallOnDifferentDates(calendar, calendar2)) {
            i |= 16;
        }
        if (fallInSameMonth(calendar, calendar2) && (i & 32) != 0) {
            i = i & (-3) & (-2);
        }
        if ((i & 19) == 0) {
            i |= 16;
        }
        if ((i & 16) != 0 && (i & 4) == 0 && (i & 8) == 0 && (!fallInSameYear(calendar, calendar2) || !isThisYear(calendar))) {
            i |= 4;
        }
        StringBuilder sb = new StringBuilder();
        if ((i & 48) != 0) {
            if ((i & 4) != 0) {
                sb.append(DateFormat.YEAR);
            }
            sb.append(str);
            if ((i & 32) == 0) {
                sb.append(DateFormat.DAY);
            }
        }
        if ((i & 2) != 0) {
            sb.append(str2);
        }
        if ((i & 1) != 0) {
            sb.append(str3);
        }
        return sb.toString();
    }

    public static int dayDistance(Calendar calendar, Calendar calendar2) {
        return calendar2.get(20) - calendar.get(20);
    }

    public static boolean isDisplayMidnightUsingSkeleton(Calendar calendar) {
        return calendar.get(11) == 0 && calendar.get(12) == 0;
    }

    private static boolean onTheHour(Calendar calendar) {
        return calendar.get(12) == 0 && calendar.get(13) == 0;
    }

    private static boolean fallOnDifferentDates(Calendar calendar, Calendar calendar2) {
        return (calendar.get(1) == calendar2.get(1) && calendar.get(2) == calendar2.get(2) && calendar.get(5) == calendar2.get(5)) ? false : true;
    }

    private static boolean fallInSameMonth(Calendar calendar, Calendar calendar2) {
        return calendar.get(2) == calendar2.get(2);
    }

    private static boolean fallInSameYear(Calendar calendar, Calendar calendar2) {
        return calendar.get(1) == calendar2.get(1);
    }

    private static boolean isThisYear(Calendar calendar) {
        Calendar calendar2 = (Calendar) calendar.clone();
        calendar2.setTimeInMillis(System.currentTimeMillis());
        return calendar.get(1) == calendar2.get(1);
    }
}
