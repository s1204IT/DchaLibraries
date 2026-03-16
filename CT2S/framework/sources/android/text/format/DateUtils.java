package android.text.format;

import android.app.AlarmManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import com.android.internal.R;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import libcore.icu.DateIntervalFormat;
import libcore.icu.LocaleData;

public class DateUtils {

    @Deprecated
    public static final String ABBREV_MONTH_FORMAT = "%b";
    public static final String ABBREV_WEEKDAY_FORMAT = "%a";
    public static final long DAY_IN_MILLIS = 86400000;

    @Deprecated
    public static final int FORMAT_12HOUR = 64;

    @Deprecated
    public static final int FORMAT_24HOUR = 128;
    public static final int FORMAT_ABBREV_ALL = 524288;
    public static final int FORMAT_ABBREV_MONTH = 65536;
    public static final int FORMAT_ABBREV_RELATIVE = 262144;
    public static final int FORMAT_ABBREV_TIME = 16384;
    public static final int FORMAT_ABBREV_WEEKDAY = 32768;

    @Deprecated
    public static final int FORMAT_CAP_AMPM = 256;

    @Deprecated
    public static final int FORMAT_CAP_MIDNIGHT = 4096;

    @Deprecated
    public static final int FORMAT_CAP_NOON = 1024;

    @Deprecated
    public static final int FORMAT_CAP_NOON_MIDNIGHT = 5120;
    public static final int FORMAT_NO_MIDNIGHT = 2048;
    public static final int FORMAT_NO_MONTH_DAY = 32;
    public static final int FORMAT_NO_NOON = 512;

    @Deprecated
    public static final int FORMAT_NO_NOON_MIDNIGHT = 2560;
    public static final int FORMAT_NO_YEAR = 8;
    public static final int FORMAT_NUMERIC_DATE = 131072;
    public static final int FORMAT_SHOW_DATE = 16;
    public static final int FORMAT_SHOW_TIME = 1;
    public static final int FORMAT_SHOW_WEEKDAY = 2;
    public static final int FORMAT_SHOW_YEAR = 4;

    @Deprecated
    public static final int FORMAT_UTC = 8192;
    public static final long HOUR_IN_MILLIS = 3600000;

    @Deprecated
    public static final String HOUR_MINUTE_24 = "%H:%M";

    @Deprecated
    public static final int LENGTH_LONG = 10;

    @Deprecated
    public static final int LENGTH_MEDIUM = 20;

    @Deprecated
    public static final int LENGTH_SHORT = 30;

    @Deprecated
    public static final int LENGTH_SHORTER = 40;

    @Deprecated
    public static final int LENGTH_SHORTEST = 50;
    public static final long MINUTE_IN_MILLIS = 60000;
    public static final String MONTH_DAY_FORMAT = "%-d";
    public static final String MONTH_FORMAT = "%B";
    public static final String NUMERIC_MONTH_FORMAT = "%m";
    public static final long SECOND_IN_MILLIS = 1000;
    public static final String WEEKDAY_FORMAT = "%A";
    public static final long WEEK_IN_MILLIS = 604800000;
    public static final String YEAR_FORMAT = "%Y";
    public static final String YEAR_FORMAT_TWO_DIGITS = "%g";
    public static final long YEAR_IN_MILLIS = 31449600000L;
    private static String sElapsedFormatHMMSS;
    private static String sElapsedFormatMMSS;
    private static Configuration sLastConfig;
    private static Time sNowTime;
    private static Time sThenTime;
    private static final Object sLock = new Object();
    public static final int[] sameYearTable = null;
    public static final int[] sameMonthTable = null;

    @Deprecated
    public static String getDayOfWeekString(int dayOfWeek, int abbrev) {
        String[] names;
        LocaleData d = LocaleData.get(Locale.getDefault());
        switch (abbrev) {
            case 10:
                names = d.longWeekdayNames;
                break;
            case 20:
                names = d.shortWeekdayNames;
                break;
            case 30:
                names = d.shortWeekdayNames;
                break;
            case 40:
                names = d.shortWeekdayNames;
                break;
            case 50:
                names = d.tinyWeekdayNames;
                break;
            default:
                names = d.shortWeekdayNames;
                break;
        }
        return names[dayOfWeek];
    }

    @Deprecated
    public static String getAMPMString(int ampm) {
        return LocaleData.get(Locale.getDefault()).amPm[ampm + 0];
    }

    @Deprecated
    public static String getMonthString(int month, int abbrev) {
        String[] names;
        LocaleData d = LocaleData.get(Locale.getDefault());
        switch (abbrev) {
            case 10:
                names = d.longMonthNames;
                break;
            case 20:
                names = d.shortMonthNames;
                break;
            case 30:
                names = d.shortMonthNames;
                break;
            case 40:
                names = d.shortMonthNames;
                break;
            case 50:
                names = d.tinyMonthNames;
                break;
            default:
                names = d.shortMonthNames;
                break;
        }
        return names[month];
    }

    public static CharSequence getRelativeTimeSpanString(long startTime) {
        return getRelativeTimeSpanString(startTime, System.currentTimeMillis(), MINUTE_IN_MILLIS);
    }

    public static CharSequence getRelativeTimeSpanString(long time, long now, long minResolution) {
        return getRelativeTimeSpanString(time, now, minResolution, 65556);
    }

    public static CharSequence getRelativeTimeSpanString(long time, long now, long minResolution, int flags) {
        long count;
        int resId;
        Resources r = Resources.getSystem();
        boolean abbrevRelative = (786432 & flags) != 0;
        boolean past = now >= time;
        long duration = Math.abs(now - time);
        if (duration < MINUTE_IN_MILLIS && minResolution < MINUTE_IN_MILLIS) {
            count = duration / 1000;
            if (past) {
                if (abbrevRelative) {
                    resId = R.plurals.abbrev_num_seconds_ago;
                } else {
                    resId = R.plurals.num_seconds_ago;
                }
            } else if (abbrevRelative) {
                resId = R.plurals.abbrev_in_num_seconds;
            } else {
                resId = R.plurals.in_num_seconds;
            }
        } else if (duration < 3600000 && minResolution < 3600000) {
            count = duration / MINUTE_IN_MILLIS;
            if (past) {
                if (abbrevRelative) {
                    resId = R.plurals.abbrev_num_minutes_ago;
                } else {
                    resId = R.plurals.num_minutes_ago;
                }
            } else if (abbrevRelative) {
                resId = R.plurals.abbrev_in_num_minutes;
            } else {
                resId = R.plurals.in_num_minutes;
            }
        } else {
            if (duration >= 86400000 || minResolution >= 86400000) {
                if (duration < WEEK_IN_MILLIS && minResolution < WEEK_IN_MILLIS) {
                    return getRelativeDayString(r, time, now);
                }
                return formatDateRange(null, time, time, flags);
            }
            count = duration / 3600000;
            if (past) {
                if (abbrevRelative) {
                    resId = R.plurals.abbrev_num_hours_ago;
                } else {
                    resId = R.plurals.num_hours_ago;
                }
            } else if (abbrevRelative) {
                resId = R.plurals.abbrev_in_num_hours;
            } else {
                resId = R.plurals.in_num_hours;
            }
        }
        String format = r.getQuantityString(resId, (int) count);
        return String.format(format, Long.valueOf(count));
    }

    public static CharSequence getRelativeDateTimeString(Context c, long time, long minResolution, long transitionResolution, int flags) {
        Resources r = Resources.getSystem();
        long now = System.currentTimeMillis();
        long duration = Math.abs(now - time);
        if (transitionResolution > WEEK_IN_MILLIS) {
            transitionResolution = WEEK_IN_MILLIS;
        } else if (transitionResolution < 86400000) {
            transitionResolution = 86400000;
        }
        CharSequence timeClause = formatDateRange(c, time, time, 1);
        if (duration < transitionResolution) {
            CharSequence relativeClause = getRelativeTimeSpanString(time, now, minResolution, flags);
            String result = r.getString(R.string.relative_time, relativeClause, timeClause);
            return result;
        }
        CharSequence dateClause = getRelativeTimeSpanString(c, time, false);
        String result2 = r.getString(R.string.date_time, dateClause, timeClause);
        return result2;
    }

    private static final String getRelativeDayString(Resources r, long day, long today) {
        int resId;
        Locale locale = r.getConfiguration().locale;
        if (locale == null) {
            locale = Locale.getDefault();
        }
        Time startTime = new Time();
        startTime.set(day);
        int startDay = Time.getJulianDay(day, startTime.gmtoff);
        Time currentTime = new Time();
        currentTime.set(today);
        int currentDay = Time.getJulianDay(today, currentTime.gmtoff);
        int days = Math.abs(currentDay - startDay);
        boolean past = today > day;
        if (days == 1) {
            if (past) {
                return LocaleData.get(locale).yesterday;
            }
            return LocaleData.get(locale).tomorrow;
        }
        if (days == 0) {
            return LocaleData.get(locale).today;
        }
        if (past) {
            resId = R.plurals.num_days_ago;
        } else {
            resId = R.plurals.in_num_days;
        }
        String format = r.getQuantityString(resId, days);
        return String.format(format, Integer.valueOf(days));
    }

    private static void initFormatStrings() {
        synchronized (sLock) {
            initFormatStringsLocked();
        }
    }

    private static void initFormatStringsLocked() {
        Resources r = Resources.getSystem();
        Configuration cfg = r.getConfiguration();
        if (sLastConfig == null || !sLastConfig.equals(cfg)) {
            sLastConfig = cfg;
            sElapsedFormatMMSS = r.getString(R.string.elapsed_time_short_format_mm_ss);
            sElapsedFormatHMMSS = r.getString(R.string.elapsed_time_short_format_h_mm_ss);
        }
    }

    public static CharSequence formatDuration(long millis) {
        Resources res = Resources.getSystem();
        if (millis >= 3600000) {
            int hours = (int) ((AlarmManager.INTERVAL_HALF_HOUR + millis) / 3600000);
            return res.getQuantityString(R.plurals.duration_hours, hours, Integer.valueOf(hours));
        }
        if (millis >= MINUTE_IN_MILLIS) {
            int minutes = (int) ((30000 + millis) / MINUTE_IN_MILLIS);
            return res.getQuantityString(R.plurals.duration_minutes, minutes, Integer.valueOf(minutes));
        }
        int seconds = (int) ((500 + millis) / 1000);
        return res.getQuantityString(R.plurals.duration_seconds, seconds, Integer.valueOf(seconds));
    }

    public static String formatElapsedTime(long elapsedSeconds) {
        return formatElapsedTime(null, elapsedSeconds);
    }

    public static String formatElapsedTime(StringBuilder recycle, long elapsedSeconds) {
        long hours = 0;
        long minutes = 0;
        if (elapsedSeconds >= 3600) {
            hours = elapsedSeconds / 3600;
            elapsedSeconds -= 3600 * hours;
        }
        if (elapsedSeconds >= 60) {
            minutes = elapsedSeconds / 60;
            elapsedSeconds -= 60 * minutes;
        }
        long seconds = elapsedSeconds;
        StringBuilder sb = recycle;
        if (sb == null) {
            sb = new StringBuilder(8);
        } else {
            sb.setLength(0);
        }
        java.util.Formatter f = new java.util.Formatter(sb, Locale.getDefault());
        initFormatStrings();
        return hours > 0 ? f.format(sElapsedFormatHMMSS, Long.valueOf(hours), Long.valueOf(minutes), Long.valueOf(seconds)).toString() : f.format(sElapsedFormatMMSS, Long.valueOf(minutes), Long.valueOf(seconds)).toString();
    }

    public static final CharSequence formatSameDayTime(long then, long now, int dateStyle, int timeStyle) {
        java.text.DateFormat f;
        Calendar thenCal = new GregorianCalendar();
        thenCal.setTimeInMillis(then);
        Date thenDate = thenCal.getTime();
        Calendar nowCal = new GregorianCalendar();
        nowCal.setTimeInMillis(now);
        if (thenCal.get(1) == nowCal.get(1) && thenCal.get(2) == nowCal.get(2) && thenCal.get(5) == nowCal.get(5)) {
            f = java.text.DateFormat.getTimeInstance(timeStyle);
        } else {
            f = java.text.DateFormat.getDateInstance(dateStyle);
        }
        return f.format(thenDate);
    }

    public static boolean isToday(long when) {
        Time time = new Time();
        time.set(when);
        int thenYear = time.year;
        int thenMonth = time.month;
        int thenMonthDay = time.monthDay;
        time.set(System.currentTimeMillis());
        return thenYear == time.year && thenMonth == time.month && thenMonthDay == time.monthDay;
    }

    public static String formatDateRange(Context context, long startMillis, long endMillis, int flags) {
        java.util.Formatter f = new java.util.Formatter(new StringBuilder(50), Locale.getDefault());
        return formatDateRange(context, f, startMillis, endMillis, flags).toString();
    }

    public static java.util.Formatter formatDateRange(Context context, java.util.Formatter formatter, long startMillis, long endMillis, int flags) {
        return formatDateRange(context, formatter, startMillis, endMillis, flags, null);
    }

    public static java.util.Formatter formatDateRange(Context context, java.util.Formatter formatter, long startMillis, long endMillis, int flags, String timeZone) {
        if ((flags & 193) == 1) {
            flags |= DateFormat.is24HourFormat(context) ? 128 : 64;
        }
        String range = DateIntervalFormat.formatDateRange(startMillis, endMillis, flags, timeZone);
        try {
            formatter.out().append(range);
            return formatter;
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    public static String formatDateTime(Context context, long millis, int flags) {
        return formatDateRange(context, millis, millis, flags);
    }

    public static CharSequence getRelativeTimeSpanString(Context c, long millis, boolean withPreposition) {
        String result;
        int prepositionId;
        long now = System.currentTimeMillis();
        long span = Math.abs(now - millis);
        synchronized (DateUtils.class) {
            if (sNowTime == null) {
                sNowTime = new Time();
            }
            if (sThenTime == null) {
                sThenTime = new Time();
            }
            sNowTime.set(now);
            sThenTime.set(millis);
            if (span < 86400000 && sNowTime.weekDay == sThenTime.weekDay) {
                result = formatDateRange(c, millis, millis, 1);
                prepositionId = R.string.preposition_for_time;
            } else if (sNowTime.year != sThenTime.year) {
                result = formatDateRange(c, millis, millis, 131092);
                prepositionId = R.string.preposition_for_date;
            } else {
                result = formatDateRange(c, millis, millis, 65552);
                prepositionId = R.string.preposition_for_date;
            }
            if (withPreposition) {
                Resources res = c.getResources();
                result = res.getString(prepositionId, result);
            }
        }
        return result;
    }

    public static CharSequence getRelativeTimeSpanString(Context c, long millis) {
        return getRelativeTimeSpanString(c, millis, false);
    }
}
