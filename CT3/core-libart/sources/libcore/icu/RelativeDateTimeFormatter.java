package libcore.icu;

import android.icu.text.DisplayContext;
import android.icu.text.RelativeDateTimeFormatter;
import android.icu.util.Calendar;
import android.icu.util.ULocale;
import java.util.Locale;
import java.util.TimeZone;
import libcore.util.BasicLruCache;

public final class RelativeDateTimeFormatter {
    private static final FormatterCache CACHED_FORMATTERS = new FormatterCache();
    public static final long DAY_IN_MILLIS = 86400000;
    private static final int DAY_IN_MS = 86400000;
    private static final int EPOCH_JULIAN_DAY = 2440588;
    public static final long HOUR_IN_MILLIS = 3600000;
    public static final long MINUTE_IN_MILLIS = 60000;
    public static final long SECOND_IN_MILLIS = 1000;
    public static final long WEEK_IN_MILLIS = 604800000;
    public static final long YEAR_IN_MILLIS = 31449600000L;

    static class FormatterCache extends BasicLruCache<String, android.icu.text.RelativeDateTimeFormatter> {
        FormatterCache() {
            super(8);
        }
    }

    private RelativeDateTimeFormatter() {
    }

    public static String getRelativeTimeSpanString(Locale locale, TimeZone tz, long time, long now, long minResolution, int flags) {
        DisplayContext displayContext = DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE;
        return getRelativeTimeSpanString(locale, tz, time, now, minResolution, flags, displayContext);
    }

    public static String getRelativeTimeSpanString(Locale locale, TimeZone tz, long time, long now, long minResolution, int flags, DisplayContext displayContext) {
        if (locale == null) {
            throw new NullPointerException("locale == null");
        }
        if (tz == null) {
            throw new NullPointerException("tz == null");
        }
        ULocale icuLocale = ULocale.forLocale(locale);
        android.icu.util.TimeZone icuTimeZone = DateUtilsBridge.icuTimeZone(tz);
        return getRelativeTimeSpanString(icuLocale, icuTimeZone, time, now, minResolution, flags, displayContext);
    }

    private static String getRelativeTimeSpanString(ULocale icuLocale, android.icu.util.TimeZone icuTimeZone, long time, long now, long minResolution, int flags, DisplayContext displayContext) {
        RelativeDateTimeFormatter.Style style;
        RelativeDateTimeFormatter.Direction direction;
        int count;
        RelativeDateTimeFormatter.RelativeUnit unit;
        FormatterCache formatterCache;
        String str;
        long duration = Math.abs(now - time);
        boolean past = now >= time;
        if ((786432 & flags) != 0) {
            style = RelativeDateTimeFormatter.Style.SHORT;
        } else {
            style = RelativeDateTimeFormatter.Style.LONG;
        }
        if (past) {
            direction = RelativeDateTimeFormatter.Direction.LAST;
        } else {
            direction = RelativeDateTimeFormatter.Direction.NEXT;
        }
        boolean relative = true;
        RelativeDateTimeFormatter.AbsoluteUnit aunit = null;
        if (duration < MINUTE_IN_MILLIS && minResolution < MINUTE_IN_MILLIS) {
            count = (int) (duration / 1000);
            unit = RelativeDateTimeFormatter.RelativeUnit.SECONDS;
        } else if (duration < HOUR_IN_MILLIS && minResolution < HOUR_IN_MILLIS) {
            count = (int) (duration / MINUTE_IN_MILLIS);
            unit = RelativeDateTimeFormatter.RelativeUnit.MINUTES;
        } else if (duration < 86400000 && minResolution < 86400000) {
            count = (int) (duration / HOUR_IN_MILLIS);
            unit = RelativeDateTimeFormatter.RelativeUnit.HOURS;
        } else if (duration < WEEK_IN_MILLIS && minResolution < WEEK_IN_MILLIS) {
            count = Math.abs(dayDistance(icuTimeZone, time, now));
            unit = RelativeDateTimeFormatter.RelativeUnit.DAYS;
            if (count == 2) {
                if (past) {
                    formatterCache = CACHED_FORMATTERS;
                    synchronized (formatterCache) {
                        str = getFormatter(icuLocale, style, displayContext).format(RelativeDateTimeFormatter.Direction.LAST_2, RelativeDateTimeFormatter.AbsoluteUnit.DAY);
                    }
                } else {
                    formatterCache = CACHED_FORMATTERS;
                    synchronized (formatterCache) {
                        str = getFormatter(icuLocale, style, displayContext).format(RelativeDateTimeFormatter.Direction.NEXT_2, RelativeDateTimeFormatter.AbsoluteUnit.DAY);
                    }
                }
                if (str != null && !str.isEmpty()) {
                    return str;
                }
            } else if (count == 1) {
                aunit = RelativeDateTimeFormatter.AbsoluteUnit.DAY;
                relative = false;
            } else if (count == 0) {
                aunit = RelativeDateTimeFormatter.AbsoluteUnit.DAY;
                direction = RelativeDateTimeFormatter.Direction.THIS;
                relative = false;
            }
        } else if (minResolution == WEEK_IN_MILLIS) {
            count = (int) (duration / WEEK_IN_MILLIS);
            unit = RelativeDateTimeFormatter.RelativeUnit.WEEKS;
        } else {
            Calendar timeCalendar = DateUtilsBridge.createIcuCalendar(icuTimeZone, icuLocale, time);
            if ((flags & 12) == 0) {
                Calendar nowCalendar = DateUtilsBridge.createIcuCalendar(icuTimeZone, icuLocale, now);
                if (timeCalendar.get(1) != nowCalendar.get(1)) {
                    flags |= 4;
                } else {
                    flags |= 8;
                }
            }
            return DateTimeFormat.format(icuLocale, timeCalendar, flags, displayContext);
        }
        synchronized (CACHED_FORMATTERS) {
            android.icu.text.RelativeDateTimeFormatter formatter = getFormatter(icuLocale, style, displayContext);
            if (relative) {
                return formatter.format(count, direction, unit);
            }
            return formatter.format(direction, aunit);
        }
    }

    public static String getRelativeDateTimeString(Locale locale, TimeZone tz, long time, long now, long minResolution, long transitionResolution, int flags) {
        RelativeDateTimeFormatter.Style style;
        int flags2;
        String dateClause;
        String strCombineDateAndTime;
        if (locale == null) {
            throw new NullPointerException("locale == null");
        }
        if (tz == null) {
            throw new NullPointerException("tz == null");
        }
        ULocale icuLocale = ULocale.forLocale(locale);
        android.icu.util.TimeZone icuTimeZone = DateUtilsBridge.icuTimeZone(tz);
        long duration = Math.abs(now - time);
        if (transitionResolution > WEEK_IN_MILLIS) {
            transitionResolution = WEEK_IN_MILLIS;
        }
        if ((786432 & flags) != 0) {
            style = RelativeDateTimeFormatter.Style.SHORT;
        } else {
            style = RelativeDateTimeFormatter.Style.LONG;
        }
        Calendar timeCalendar = DateUtilsBridge.createIcuCalendar(icuTimeZone, icuLocale, time);
        Calendar nowCalendar = DateUtilsBridge.createIcuCalendar(icuTimeZone, icuLocale, now);
        int days = Math.abs(DateUtilsBridge.dayDistance(timeCalendar, nowCalendar));
        if (duration < transitionResolution) {
            if (days > 0 && minResolution < 86400000) {
                minResolution = 86400000;
            }
            dateClause = getRelativeTimeSpanString(icuLocale, icuTimeZone, time, now, minResolution, flags, DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE);
        } else {
            if (timeCalendar.get(1) != nowCalendar.get(1)) {
                flags2 = 131092;
            } else {
                flags2 = 65560;
            }
            dateClause = DateTimeFormat.format(icuLocale, timeCalendar, flags2, DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE);
        }
        String timeClause = DateTimeFormat.format(icuLocale, timeCalendar, 1, DisplayContext.CAPITALIZATION_NONE);
        DisplayContext capitalizationContext = DisplayContext.CAPITALIZATION_NONE;
        synchronized (CACHED_FORMATTERS) {
            strCombineDateAndTime = getFormatter(icuLocale, style, capitalizationContext).combineDateAndTime(dateClause, timeClause);
        }
        return strCombineDateAndTime;
    }

    private static android.icu.text.RelativeDateTimeFormatter getFormatter(ULocale locale, RelativeDateTimeFormatter.Style style, DisplayContext displayContext) {
        String key = locale + "\t" + style + "\t" + displayContext;
        android.icu.text.RelativeDateTimeFormatter formatter = CACHED_FORMATTERS.get(key);
        if (formatter == null) {
            android.icu.text.RelativeDateTimeFormatter formatter2 = android.icu.text.RelativeDateTimeFormatter.getInstance(locale, null, style, displayContext);
            CACHED_FORMATTERS.put(key, formatter2);
            return formatter2;
        }
        return formatter;
    }

    private static int dayDistance(android.icu.util.TimeZone icuTimeZone, long startTime, long endTime) {
        return julianDay(icuTimeZone, endTime) - julianDay(icuTimeZone, startTime);
    }

    private static int julianDay(android.icu.util.TimeZone icuTimeZone, long time) {
        long utcMs = time + ((long) icuTimeZone.getOffset(time));
        return ((int) (utcMs / 86400000)) + EPOCH_JULIAN_DAY;
    }
}
