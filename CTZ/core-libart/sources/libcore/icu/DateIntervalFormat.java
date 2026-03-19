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

    public static String formatDateRange(long j, long j2, int i, String str) {
        if ((i & 8192) != 0) {
            str = "UTC";
        }
        return formatDateRange(ULocale.getDefault(), DateUtilsBridge.icuTimeZone(str != null ? TimeZone.getTimeZone(str) : TimeZone.getDefault()), j, j2, i);
    }

    public static String formatDateRange(ULocale uLocale, android.icu.util.TimeZone timeZone, long j, long j2, int i) {
        Calendar calendarCreateIcuCalendar;
        String string;
        Calendar calendarCreateIcuCalendar2 = DateUtilsBridge.createIcuCalendar(timeZone, uLocale, j);
        if (j != j2) {
            calendarCreateIcuCalendar = DateUtilsBridge.createIcuCalendar(timeZone, uLocale, j2);
        } else {
            calendarCreateIcuCalendar = calendarCreateIcuCalendar2;
        }
        if (isExactlyMidnight(calendarCreateIcuCalendar)) {
            boolean z = (i & 1) == 1;
            boolean z2 = DateUtilsBridge.dayDistance(calendarCreateIcuCalendar2, calendarCreateIcuCalendar) == 1;
            if ((!z && j != j2) || (z2 && !DateUtilsBridge.isDisplayMidnightUsingSkeleton(calendarCreateIcuCalendar2))) {
                calendarCreateIcuCalendar.add(5, -1);
            }
        }
        String skeleton = DateUtilsBridge.toSkeleton(calendarCreateIcuCalendar2, calendarCreateIcuCalendar, i);
        synchronized (CACHED_FORMATTERS) {
            string = getFormatter(skeleton, uLocale, timeZone).format(calendarCreateIcuCalendar2, calendarCreateIcuCalendar, new StringBuffer(), new FieldPosition(0)).toString();
        }
        return string;
    }

    private static android.icu.text.DateIntervalFormat getFormatter(String str, ULocale uLocale, android.icu.util.TimeZone timeZone) {
        String str2 = str + "\t" + uLocale + "\t" + timeZone;
        android.icu.text.DateIntervalFormat dateIntervalFormat = CACHED_FORMATTERS.get(str2);
        if (dateIntervalFormat != null) {
            return dateIntervalFormat;
        }
        android.icu.text.DateIntervalFormat dateIntervalFormat2 = android.icu.text.DateIntervalFormat.getInstance(str, uLocale);
        dateIntervalFormat2.setTimeZone(timeZone);
        CACHED_FORMATTERS.put(str2, dateIntervalFormat2);
        return dateIntervalFormat2;
    }

    private static boolean isExactlyMidnight(Calendar calendar) {
        return calendar.get(11) == 0 && calendar.get(12) == 0 && calendar.get(13) == 0 && calendar.get(14) == 0;
    }
}
