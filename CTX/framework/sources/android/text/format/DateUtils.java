package android.text.format;

import android.app.AlarmManager;
import android.app.job.JobInfo;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.icu.text.MeasureFormat;
import android.icu.util.Measure;
import android.icu.util.MeasureUnit;
import com.android.internal.R;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import libcore.icu.DateIntervalFormat;
import libcore.icu.LocaleData;
import libcore.icu.RelativeDateTimeFormatter;

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

    @Deprecated
    public static final int[] sameYearTable = null;

    @Deprecated
    public static final int[] sameMonthTable = null;

    @Deprecated
    public static String getDayOfWeekString(int i, int i2) {
        String[] strArr;
        LocaleData localeData = LocaleData.get(Locale.getDefault());
        if (i2 == 10) {
            strArr = localeData.longWeekdayNames;
        } else if (i2 != 20 && i2 != 30 && i2 != 40 && i2 == 50) {
            strArr = localeData.tinyWeekdayNames;
        } else {
            strArr = localeData.shortWeekdayNames;
        }
        return strArr[i];
    }

    @Deprecated
    public static String getAMPMString(int i) {
        return LocaleData.get(Locale.getDefault()).amPm[i + 0];
    }

    @Deprecated
    public static String getMonthString(int i, int i2) {
        String[] strArr;
        LocaleData localeData = LocaleData.get(Locale.getDefault());
        if (i2 == 10) {
            strArr = localeData.longMonthNames;
        } else if (i2 != 20 && i2 != 30 && i2 != 40 && i2 == 50) {
            strArr = localeData.tinyMonthNames;
        } else {
            strArr = localeData.shortMonthNames;
        }
        return strArr[i];
    }

    public static CharSequence getRelativeTimeSpanString(long j) {
        return getRelativeTimeSpanString(j, System.currentTimeMillis(), MINUTE_IN_MILLIS);
    }

    public static CharSequence getRelativeTimeSpanString(long j, long j2, long j3) {
        return getRelativeTimeSpanString(j, j2, j3, 65556);
    }

    public static CharSequence getRelativeTimeSpanString(long j, long j2, long j3, int i) {
        return RelativeDateTimeFormatter.getRelativeTimeSpanString(Locale.getDefault(), TimeZone.getDefault(), j, j2, j3, i);
    }

    public static CharSequence getRelativeDateTimeString(Context context, long j, long j2, long j3, int i) {
        int i2 = i;
        if ((i2 & 193) == 1) {
            i2 |= DateFormat.is24HourFormat(context) ? 128 : 64;
        }
        return RelativeDateTimeFormatter.getRelativeDateTimeString(Locale.getDefault(), TimeZone.getDefault(), j, System.currentTimeMillis(), j2, j3, i2);
    }

    private static void initFormatStrings() {
        synchronized (sLock) {
            initFormatStringsLocked();
        }
    }

    private static void initFormatStringsLocked() {
        Resources system = Resources.getSystem();
        Configuration configuration = system.getConfiguration();
        if (sLastConfig == null || !sLastConfig.equals(configuration)) {
            sLastConfig = configuration;
            sElapsedFormatMMSS = system.getString(R.string.elapsed_time_short_format_mm_ss);
            sElapsedFormatHMMSS = system.getString(R.string.elapsed_time_short_format_h_mm_ss);
        }
    }

    public static CharSequence formatDuration(long j) {
        return formatDuration(j, 10);
    }

    public static CharSequence formatDuration(long j, int i) {
        MeasureFormat.FormatWidth formatWidth;
        if (i == 10) {
            formatWidth = MeasureFormat.FormatWidth.WIDE;
        } else if (i == 20 || i == 30 || i == 40) {
            formatWidth = MeasureFormat.FormatWidth.SHORT;
        } else if (i == 50) {
            formatWidth = MeasureFormat.FormatWidth.NARROW;
        } else {
            formatWidth = MeasureFormat.FormatWidth.WIDE;
        }
        MeasureFormat measureFormat = MeasureFormat.getInstance(Locale.getDefault(), formatWidth);
        if (j >= 3600000) {
            return measureFormat.format(new Measure(Integer.valueOf((int) ((j + AlarmManager.INTERVAL_HALF_HOUR) / 3600000)), MeasureUnit.HOUR));
        }
        if (j >= MINUTE_IN_MILLIS) {
            return measureFormat.format(new Measure(Integer.valueOf((int) ((j + JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS) / MINUTE_IN_MILLIS)), MeasureUnit.MINUTE));
        }
        return measureFormat.format(new Measure(Integer.valueOf((int) ((j + 500) / 1000)), MeasureUnit.SECOND));
    }

    public static String formatElapsedTime(long j) {
        return formatElapsedTime(null, j);
    }

    public static String formatElapsedTime(StringBuilder sb, long j) {
        long j2;
        long j3;
        if (j >= 3600) {
            j2 = j / 3600;
            j -= 3600 * j2;
        } else {
            j2 = 0;
        }
        if (j >= 60) {
            j3 = j / 60;
            j -= 60 * j3;
        } else {
            j3 = 0;
        }
        if (sb == null) {
            sb = new StringBuilder(8);
        } else {
            sb.setLength(0);
        }
        java.util.Formatter formatter = new java.util.Formatter(sb, Locale.getDefault());
        initFormatStrings();
        return j2 > 0 ? formatter.format(sElapsedFormatHMMSS, Long.valueOf(j2), Long.valueOf(j3), Long.valueOf(j)).toString() : formatter.format(sElapsedFormatMMSS, Long.valueOf(j3), Long.valueOf(j)).toString();
    }

    public static final CharSequence formatSameDayTime(long j, long j2, int i, int i2) {
        java.text.DateFormat dateInstance;
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(j);
        Date time = gregorianCalendar.getTime();
        GregorianCalendar gregorianCalendar2 = new GregorianCalendar();
        gregorianCalendar2.setTimeInMillis(j2);
        if (gregorianCalendar.get(1) == gregorianCalendar2.get(1) && gregorianCalendar.get(2) == gregorianCalendar2.get(2) && gregorianCalendar.get(5) == gregorianCalendar2.get(5)) {
            dateInstance = java.text.DateFormat.getTimeInstance(i2);
        } else {
            dateInstance = java.text.DateFormat.getDateInstance(i);
        }
        return dateInstance.format(time);
    }

    public static boolean isToday(long j) {
        Time time = new Time();
        time.set(j);
        int i = time.year;
        int i2 = time.month;
        int i3 = time.monthDay;
        time.set(System.currentTimeMillis());
        return i == time.year && i2 == time.month && i3 == time.monthDay;
    }

    public static String formatDateRange(Context context, long j, long j2, int i) {
        return formatDateRange(context, new java.util.Formatter(new StringBuilder(50), Locale.getDefault()), j, j2, i).toString();
    }

    public static java.util.Formatter formatDateRange(Context context, java.util.Formatter formatter, long j, long j2, int i) {
        return formatDateRange(context, formatter, j, j2, i, null);
    }

    public static java.util.Formatter formatDateRange(Context context, java.util.Formatter formatter, long j, long j2, int i, String str) {
        if ((i & 193) == 1) {
            i |= DateFormat.is24HourFormat(context) ? 128 : 64;
        }
        try {
            formatter.out().append(DateIntervalFormat.formatDateRange(j, j2, i, str));
            return formatter;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static String formatDateTime(Context context, long j, int i) {
        return formatDateRange(context, j, j, i);
    }

    public static CharSequence getRelativeTimeSpanString(Context context, long j, boolean z) {
        String dateRange;
        long jCurrentTimeMillis = System.currentTimeMillis();
        long jAbs = Math.abs(jCurrentTimeMillis - j);
        synchronized (DateUtils.class) {
            if (sNowTime == null) {
                sNowTime = new Time();
            }
            if (sThenTime == null) {
                sThenTime = new Time();
            }
            sNowTime.set(jCurrentTimeMillis);
            sThenTime.set(j);
            int i = R.string.preposition_for_date;
            if (jAbs < 86400000 && sNowTime.weekDay == sThenTime.weekDay) {
                dateRange = formatDateRange(context, j, j, 1);
                i = R.string.preposition_for_time;
            } else if (sNowTime.year != sThenTime.year) {
                dateRange = formatDateRange(context, j, j, 131092);
            } else {
                dateRange = formatDateRange(context, j, j, 65552);
            }
            if (z) {
                dateRange = context.getResources().getString(i, dateRange);
            }
        }
        return dateRange;
    }

    public static CharSequence getRelativeTimeSpanString(Context context, long j) {
        return getRelativeTimeSpanString(context, j, false);
    }
}
