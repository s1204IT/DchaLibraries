package com.android.contacts.util;

import android.content.Context;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {
    public static final String NO_YEAR_DATE_FEB29TH = "--02-29";
    public static final TimeZone UTC_TIMEZONE = TimeZone.getTimeZone("UTC");
    private static final SimpleDateFormat[] DATE_FORMATS = {CommonDateUtils.FULL_DATE_FORMAT, CommonDateUtils.DATE_AND_TIME_FORMAT, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US), new SimpleDateFormat("yyyyMMdd", Locale.US), new SimpleDateFormat("yyyyMMdd'T'HHmmssSSS'Z'", Locale.US), new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US), new SimpleDateFormat("yyyyMMdd'T'HHmm'Z'", Locale.US)};

    static {
        for (SimpleDateFormat simpleDateFormat : DATE_FORMATS) {
            simpleDateFormat.setLenient(true);
            simpleDateFormat.setTimeZone(UTC_TIMEZONE);
        }
        CommonDateUtils.NO_YEAR_DATE_FORMAT.setTimeZone(UTC_TIMEZONE);
    }

    public static Calendar parseDate(String str, boolean z) {
        Date date;
        ParsePosition parsePosition = new ParsePosition(0);
        if (!z) {
            if (NO_YEAR_DATE_FEB29TH.equals(str)) {
                return getUtcDate(0, 1, 29);
            }
            synchronized (CommonDateUtils.NO_YEAR_DATE_FORMAT) {
                date = CommonDateUtils.NO_YEAR_DATE_FORMAT.parse(str, parsePosition);
            }
            if (parsePosition.getIndex() == str.length()) {
                return getUtcDate(date, true);
            }
        }
        for (int i = 0; i < DATE_FORMATS.length; i++) {
            SimpleDateFormat simpleDateFormat = DATE_FORMATS[i];
            synchronized (simpleDateFormat) {
                parsePosition.setIndex(0);
                Date date2 = simpleDateFormat.parse(str, parsePosition);
                if (parsePosition.getIndex() == str.length()) {
                    return getUtcDate(date2, false);
                }
            }
        }
        return null;
    }

    private static final Calendar getUtcDate(Date date, boolean z) {
        Calendar calendar = Calendar.getInstance(UTC_TIMEZONE, Locale.US);
        calendar.setTime(date);
        if (z) {
            calendar.set(1, 0);
        }
        return calendar;
    }

    private static final Calendar getUtcDate(int i, int i2, int i3) {
        Calendar calendar = Calendar.getInstance(UTC_TIMEZONE, Locale.US);
        calendar.clear();
        calendar.set(1, i);
        calendar.set(2, i2);
        calendar.set(5, i3);
        return calendar;
    }

    public static boolean isYearSet(Calendar calendar) {
        return calendar.get(1) > 1;
    }

    public static String formatDate(Context context, String str) {
        return formatDate(context, str, true);
    }

    public static String formatDate(Context context, String str, boolean z) {
        Calendar date;
        DateFormat longDateFormat;
        String str2;
        if (str == null) {
            return null;
        }
        String strTrim = str.trim();
        if (strTrim.length() == 0 || (date = parseDate(strTrim, false)) == null) {
            return strTrim;
        }
        if (!isYearSet(date)) {
            longDateFormat = getLocalizedDateFormatWithoutYear(context);
        } else {
            longDateFormat = z ? android.text.format.DateFormat.getLongDateFormat(context) : android.text.format.DateFormat.getDateFormat(context);
        }
        synchronized (longDateFormat) {
            longDateFormat.setTimeZone(UTC_TIMEZONE);
            str2 = longDateFormat.format(date.getTime());
        }
        return str2;
    }

    public static boolean isMonthBeforeDay(Context context) {
        char[] dateFormatOrder = android.text.format.DateFormat.getDateFormatOrder(context);
        for (int i = 0; i < dateFormatOrder.length && dateFormatOrder[i] != 'd'; i++) {
            if (dateFormatOrder[i] == 'M') {
                return true;
            }
        }
        return false;
    }

    public static DateFormat getLocalizedDateFormatWithoutYear(Context context) {
        String pattern = ((SimpleDateFormat) SimpleDateFormat.getDateInstance(1)).toPattern();
        try {
            return new SimpleDateFormat(pattern.replaceAll(pattern.contains("de") ? "[^Mm]*[Yy]+[^Mm]*" : "[^DdMm]*[Yy]+[^DdMm]*", ""));
        } catch (IllegalArgumentException e) {
            return new SimpleDateFormat(isMonthBeforeDay(context) ? "MMMM dd" : "dd MMMM");
        }
    }

    public static Date getNextAnnualDate(Calendar calendar) {
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTime(new Date());
        boolean z = false;
        calendar2.set(11, 0);
        calendar2.set(12, 0);
        calendar2.set(13, 0);
        calendar2.set(14, 0);
        boolean zIsYearSet = isYearSet(calendar);
        int i = calendar.get(1);
        int i2 = calendar.get(2);
        int i3 = calendar.get(5);
        if (i2 == 1 && i3 == 29) {
            z = true;
        }
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        if (!zIsYearSet) {
            i = calendar2.get(1);
        }
        gregorianCalendar.set(i, i2, i3);
        if (!zIsYearSet) {
            int i4 = calendar2.get(1);
            if (gregorianCalendar.before(calendar2) || (z && !gregorianCalendar.isLeapYear(i4))) {
                do {
                    i4++;
                    if (!z) {
                        break;
                    }
                } while (!gregorianCalendar.isLeapYear(i4));
                gregorianCalendar.set(i4, i2, i3);
            }
        }
        return gregorianCalendar.getTime();
    }
}
