package libcore.icu;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import libcore.util.BasicLruCache;

public final class DateIntervalFormat {
    private static final FormatterCache CACHED_FORMATTERS = new FormatterCache();
    private static final int DAY_IN_MS = 86400000;
    private static final int EPOCH_JULIAN_DAY = 2440588;
    public static final int FORMAT_12HOUR = 64;
    public static final int FORMAT_24HOUR = 128;
    public static final int FORMAT_ABBREV_ALL = 524288;
    public static final int FORMAT_ABBREV_MONTH = 65536;
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

    private static native long createDateIntervalFormat(String str, String str2, String str3);

    private static native void destroyDateIntervalFormat(long j);

    private static native String formatDateInterval(long j, long j2, long j3);

    static class FormatterCache extends BasicLruCache<String, Long> {
        FormatterCache() {
            super(8);
        }

        @Override
        protected void entryEvicted(String key, Long value) {
            DateIntervalFormat.destroyDateIntervalFormat(value.longValue());
        }
    }

    private DateIntervalFormat() {
    }

    public static String formatDateRange(long startMs, long endMs, int flags, String olsonId) {
        if ((flags & 8192) != 0) {
            olsonId = "UTC";
        }
        TimeZone tz = olsonId != null ? TimeZone.getTimeZone(olsonId) : TimeZone.getDefault();
        return formatDateRange(Locale.getDefault(), tz, startMs, endMs, flags);
    }

    public static String formatDateRange(Locale locale, TimeZone tz, long startMs, long endMs, int flags) {
        Calendar endCalendar;
        String dateInterval;
        Calendar startCalendar = Calendar.getInstance(tz);
        startCalendar.setTimeInMillis(startMs);
        if (startMs == endMs) {
            endCalendar = startCalendar;
        } else {
            endCalendar = Calendar.getInstance(tz);
            endCalendar.setTimeInMillis(endMs);
        }
        boolean endsAtMidnight = isMidnight(endCalendar);
        if (startMs != endMs && endsAtMidnight && ((flags & 1) == 0 || dayDistance(startCalendar, endCalendar) <= 1)) {
            endCalendar.roll(5, false);
            endMs -= 86400000;
        }
        String skeleton = toSkeleton(startCalendar, endCalendar, flags);
        synchronized (CACHED_FORMATTERS) {
            dateInterval = formatDateInterval(getFormatter(skeleton, locale.toString(), tz.getID()), startMs, endMs);
        }
        return dateInterval;
    }

    private static long getFormatter(String skeleton, String localeName, String tzName) {
        String key = skeleton + "\t" + localeName + "\t" + tzName;
        Long formatter = CACHED_FORMATTERS.get(key);
        if (formatter != null) {
            return formatter.longValue();
        }
        long address = createDateIntervalFormat(skeleton, localeName, tzName);
        CACHED_FORMATTERS.put(key, Long.valueOf(address));
        return address;
    }

    private static String toSkeleton(Calendar startCalendar, Calendar endCalendar, int flags) {
        if ((524288 & flags) != 0) {
            flags |= 114688;
        }
        String monthPart = "MMMM";
        if ((131072 & flags) != 0) {
            monthPart = "M";
        } else if ((65536 & flags) != 0) {
            monthPart = "MMM";
        }
        String weekPart = "EEEE";
        if ((32768 & flags) != 0) {
            weekPart = "EEE";
        }
        String timePart = "j";
        if ((flags & 128) != 0) {
            timePart = "H";
        } else if ((flags & 64) != 0) {
            timePart = "h";
        }
        if ((flags & 16384) == 0 || (flags & 128) != 0 || !onTheHour(startCalendar) || !onTheHour(endCalendar)) {
            timePart = timePart + "m";
        }
        if (fallOnDifferentDates(startCalendar, endCalendar)) {
            flags |= 16;
        }
        if (fallInSameMonth(startCalendar, endCalendar) && (flags & 32) != 0) {
            flags = flags & (-3) & (-2);
        }
        if ((flags & 19) == 0) {
            flags |= 16;
        }
        if ((flags & 16) != 0 && (flags & 4) == 0 && (flags & 8) == 0 && (!fallInSameYear(startCalendar, endCalendar) || !isThisYear(startCalendar))) {
            flags |= 4;
        }
        StringBuilder builder = new StringBuilder();
        if ((flags & 48) != 0) {
            if ((flags & 4) != 0) {
                builder.append("y");
            }
            builder.append(monthPart);
            if ((flags & 32) == 0) {
                builder.append("d");
            }
        }
        if ((flags & 2) != 0) {
            builder.append(weekPart);
        }
        if ((flags & 1) != 0) {
            builder.append(timePart);
        }
        return builder.toString();
    }

    private static boolean isMidnight(Calendar c) {
        return c.get(11) == 0 && c.get(12) == 0 && c.get(13) == 0 && c.get(14) == 0;
    }

    private static boolean onTheHour(Calendar c) {
        return c.get(12) == 0 && c.get(13) == 0;
    }

    private static boolean fallOnDifferentDates(Calendar c1, Calendar c2) {
        return (c1.get(1) == c2.get(1) && c1.get(2) == c2.get(2) && c1.get(5) == c2.get(5)) ? false : true;
    }

    private static boolean fallInSameMonth(Calendar c1, Calendar c2) {
        return c1.get(2) == c2.get(2);
    }

    private static boolean fallInSameYear(Calendar c1, Calendar c2) {
        return c1.get(1) == c2.get(1);
    }

    private static boolean isThisYear(Calendar c) {
        Calendar now = Calendar.getInstance(c.getTimeZone());
        return c.get(1) == now.get(1);
    }

    private static int dayDistance(Calendar c1, Calendar c2) {
        return julianDay(c2) - julianDay(c1);
    }

    private static int julianDay(Calendar c) {
        long utcMs = c.getTimeInMillis() + ((long) c.get(15)) + ((long) c.get(16));
        return ((int) (utcMs / 86400000)) + EPOCH_JULIAN_DAY;
    }
}
