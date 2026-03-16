package com.android.contacts.common.util;

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
    public static final TimeZone UTC_TIMEZONE = TimeZone.getTimeZone("UTC");
    private static final SimpleDateFormat[] DATE_FORMATS = {CommonDateUtils.FULL_DATE_FORMAT, CommonDateUtils.DATE_AND_TIME_FORMAT, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US), new SimpleDateFormat("yyyyMMdd", Locale.US), new SimpleDateFormat("yyyyMMdd'T'HHmmssSSS'Z'", Locale.US), new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US), new SimpleDateFormat("yyyyMMdd'T'HHmm'Z'", Locale.US)};

    static {
        SimpleDateFormat[] arr$ = DATE_FORMATS;
        for (SimpleDateFormat format : arr$) {
            format.setLenient(true);
            format.setTimeZone(UTC_TIMEZONE);
        }
        CommonDateUtils.NO_YEAR_DATE_FORMAT.setTimeZone(UTC_TIMEZONE);
    }

    public static Calendar parseDate(String string, boolean mustContainYear) {
        Date date;
        ParsePosition parsePosition = new ParsePosition(0);
        if (!mustContainYear) {
            if ("--02-29".equals(string)) {
                return getUtcDate(0, 1, 29);
            }
            synchronized (CommonDateUtils.NO_YEAR_DATE_FORMAT) {
                date = CommonDateUtils.NO_YEAR_DATE_FORMAT.parse(string, parsePosition);
            }
            boolean noYearParsed = parsePosition.getIndex() == string.length();
            if (noYearParsed) {
                return getUtcDate(date, true);
            }
        }
        for (int i = 0; i < DATE_FORMATS.length; i++) {
            SimpleDateFormat f = DATE_FORMATS[i];
            synchronized (f) {
                parsePosition.setIndex(0);
                Date date2 = f.parse(string, parsePosition);
                if (parsePosition.getIndex() == string.length()) {
                    return getUtcDate(date2, false);
                }
            }
        }
        return null;
    }

    private static final Calendar getUtcDate(Date date, boolean noYear) {
        Calendar calendar = Calendar.getInstance(UTC_TIMEZONE, Locale.US);
        calendar.setTime(date);
        if (noYear) {
            calendar.set(1, 0);
        }
        return calendar;
    }

    private static final Calendar getUtcDate(int year, int month, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance(UTC_TIMEZONE, Locale.US);
        calendar.clear();
        calendar.set(1, year);
        calendar.set(2, month);
        calendar.set(5, dayOfMonth);
        return calendar;
    }

    public static boolean isYearSet(Calendar cal) {
        return cal.get(1) > 1;
    }

    public static String formatDate(Context context, String string) {
        return formatDate(context, string, true);
    }

    public static String formatDate(Context context, String string, boolean longForm) {
        Calendar cal;
        DateFormat outFormat;
        String str;
        if (string == null) {
            return null;
        }
        String string2 = string.trim();
        if (string2.length() != 0 && (cal = parseDate(string2, false)) != null) {
            boolean isYearSet = isYearSet(cal);
            if (!isYearSet) {
                outFormat = getLocalizedDateFormatWithoutYear(context);
            } else {
                outFormat = longForm ? android.text.format.DateFormat.getLongDateFormat(context) : android.text.format.DateFormat.getDateFormat(context);
            }
            synchronized (outFormat) {
                outFormat.setTimeZone(UTC_TIMEZONE);
                str = outFormat.format(cal.getTime());
            }
            return str;
        }
        return string2;
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
        String yearPattern = pattern.contains("de") ? "[^Mm]*[Yy]+[^Mm]*" : "[^DdMm]*[Yy]+[^DdMm]*";
        try {
            return new SimpleDateFormat(pattern.replaceAll(yearPattern, ""));
        } catch (IllegalArgumentException e) {
            return new SimpleDateFormat(isMonthBeforeDay(context) ? "MMMM dd" : "dd MMMM");
        }
    }

    public static Date getNextAnnualDate(Calendar target) {
        boolean isFeb29 = false;
        Calendar today = Calendar.getInstance();
        today.setTime(new Date());
        today.set(11, 0);
        today.set(12, 0);
        today.set(13, 0);
        today.set(14, 0);
        boolean isYearSet = isYearSet(target);
        int targetYear = target.get(1);
        int targetMonth = target.get(2);
        int targetDay = target.get(5);
        if (targetMonth == 1 && targetDay == 29) {
            isFeb29 = true;
        }
        GregorianCalendar anniversary = new GregorianCalendar();
        if (!isYearSet) {
            targetYear = today.get(1);
        }
        anniversary.set(targetYear, targetMonth, targetDay);
        if (!isYearSet) {
            int anniversaryYear = today.get(1);
            if (anniversary.before(today) || (isFeb29 && !anniversary.isLeapYear(anniversaryYear))) {
                do {
                    anniversaryYear++;
                    if (!isFeb29) {
                        break;
                    }
                } while (!anniversary.isLeapYear(anniversaryYear));
                anniversary.set(anniversaryYear, targetMonth, targetDay);
            }
        }
        return anniversary.getTime();
    }
}
