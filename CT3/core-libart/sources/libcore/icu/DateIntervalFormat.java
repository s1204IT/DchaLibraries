package libcore.icu;

import android.icu.util.Calendar;
import android.icu.util.ULocale;
import java.text.FieldPosition;
import java.util.TimeZone;
import libcore.util.BasicLruCache;

public final class DateIntervalFormat {
    private static final BasicLruCache<String, android.icu.text.DateIntervalFormat> CACHED_FORMATTERS = new BasicLruCache<>(8);

    private DateIntervalFormat() {
    }

    public static String formatDateRange(long startMs, long endMs, int flags, String olsonId) {
        if ((flags & 8192) != 0) {
            olsonId = "UTC";
        }
        TimeZone tz = olsonId != null ? TimeZone.getTimeZone(olsonId) : TimeZone.getDefault();
        android.icu.util.TimeZone icuTimeZone = DateUtilsBridge.icuTimeZone(tz);
        ULocale icuLocale = ULocale.getDefault();
        return formatDateRange(icuLocale, icuTimeZone, startMs, endMs, flags);
    }

    public static String formatDateRange(ULocale icuLocale, android.icu.util.TimeZone icuTimeZone, long startMs, long endMs, int flags) {
        Calendar endCalendar;
        String string;
        Calendar startCalendar = DateUtilsBridge.createIcuCalendar(icuTimeZone, icuLocale, startMs);
        if (startMs == endMs) {
            endCalendar = startCalendar;
        } else {
            endCalendar = DateUtilsBridge.createIcuCalendar(icuTimeZone, icuLocale, endMs);
        }
        boolean endsAtMidnight = isMidnight(endCalendar);
        if (startMs != endMs && endsAtMidnight && ((flags & 1) == 0 || DateUtilsBridge.dayDistance(startCalendar, endCalendar) <= 1)) {
            endCalendar.add(5, -1);
        }
        String skeleton = DateUtilsBridge.toSkeleton(startCalendar, endCalendar, flags);
        synchronized (CACHED_FORMATTERS) {
            android.icu.text.DateIntervalFormat formatter = getFormatter(skeleton, icuLocale, icuTimeZone);
            string = formatter.format(startCalendar, endCalendar, new StringBuffer(), new FieldPosition(0)).toString();
        }
        return string;
    }

    private static android.icu.text.DateIntervalFormat getFormatter(String skeleton, ULocale locale, android.icu.util.TimeZone icuTimeZone) {
        String key = skeleton + "\t" + locale + "\t" + icuTimeZone;
        android.icu.text.DateIntervalFormat formatter = CACHED_FORMATTERS.get(key);
        if (formatter != null) {
            return formatter;
        }
        android.icu.text.DateIntervalFormat formatter2 = android.icu.text.DateIntervalFormat.getInstance(skeleton, locale);
        formatter2.setTimeZone(icuTimeZone);
        CACHED_FORMATTERS.put(key, formatter2);
        return formatter2;
    }

    private static boolean isMidnight(Calendar c) {
        return c.get(11) == 0 && c.get(12) == 0 && c.get(13) == 0 && c.get(14) == 0;
    }
}
