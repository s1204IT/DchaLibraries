package com.android.common;

import android.text.format.Time;
import com.mediatek.vcalendar.valuetype.DateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LegacyHttpDateTime {
    private static final String HTTP_DATE_RFC_REGEXP = "([0-9]{1,2})[- ]([A-Za-z]{3,9})[- ]([0-9]{2,4})[ ]([0-9]{1,2}:[0-9][0-9]:[0-9][0-9])";
    private static final Pattern HTTP_DATE_RFC_PATTERN = Pattern.compile(HTTP_DATE_RFC_REGEXP);
    private static final String HTTP_DATE_ANSIC_REGEXP = "[ ]([A-Za-z]{3,9})[ ]+([0-9]{1,2})[ ]([0-9]{1,2}:[0-9][0-9]:[0-9][0-9])[ ]([0-9]{2,4})";
    private static final Pattern HTTP_DATE_ANSIC_PATTERN = Pattern.compile(HTTP_DATE_ANSIC_REGEXP);

    LegacyHttpDateTime() {
    }

    private static class TimeOfDay {
        int hour;
        int minute;
        int second;

        TimeOfDay(int i, int i2, int i3) {
            this.hour = i;
            this.minute = i2;
            this.second = i3;
        }
    }

    public static long parse(String str) throws IllegalArgumentException {
        int year;
        int i;
        int i2;
        TimeOfDay time;
        int i3;
        int i4;
        int i5;
        Matcher matcher = HTTP_DATE_RFC_PATTERN.matcher(str);
        if (matcher.find()) {
            int date = getDate(matcher.group(1));
            int month = getMonth(matcher.group(2));
            int year2 = getYear(matcher.group(3));
            time = getTime(matcher.group(4));
            i = month;
            i2 = date;
            year = year2;
        } else {
            Matcher matcher2 = HTTP_DATE_ANSIC_PATTERN.matcher(str);
            if (matcher2.find()) {
                int month2 = getMonth(matcher2.group(1));
                int date2 = getDate(matcher2.group(2));
                TimeOfDay time2 = getTime(matcher2.group(3));
                year = getYear(matcher2.group(4));
                i = month2;
                i2 = date2;
                time = time2;
            } else {
                throw new IllegalArgumentException();
            }
        }
        if (year >= 2038) {
            i5 = 0;
            i3 = 2038;
            i4 = 1;
        } else {
            i3 = year;
            i4 = i2;
            i5 = i;
        }
        Time time3 = new Time(DateTime.UTC);
        time3.set(time.second, time.minute, time.hour, i4, i5, i3);
        return time3.toMillis(false);
    }

    private static int getDate(String str) {
        if (str.length() == 2) {
            return ((str.charAt(0) - '0') * 10) + (str.charAt(1) - '0');
        }
        return str.charAt(0) - '0';
    }

    private static int getMonth(String str) {
        int lowerCase = ((Character.toLowerCase(str.charAt(0)) + Character.toLowerCase(str.charAt(1))) + Character.toLowerCase(str.charAt(2))) - 291;
        if (lowerCase == 22) {
            return 0;
        }
        if (lowerCase == 26) {
            return 7;
        }
        if (lowerCase == 29) {
            return 2;
        }
        if (lowerCase == 32) {
            return 3;
        }
        if (lowerCase == 40) {
            return 6;
        }
        if (lowerCase == 42) {
            return 5;
        }
        if (lowerCase == 48) {
            return 10;
        }
        switch (lowerCase) {
            case 9:
                return 11;
            case 10:
                return 1;
            default:
                switch (lowerCase) {
                    case 35:
                        return 9;
                    case 36:
                        return 4;
                    case 37:
                        return 8;
                    default:
                        throw new IllegalArgumentException();
                }
        }
    }

    private static int getYear(String str) {
        if (str.length() == 2) {
            int iCharAt = ((str.charAt(0) - '0') * 10) + (str.charAt(1) - '0');
            if (iCharAt >= 70) {
                return iCharAt + 1900;
            }
            return iCharAt + 2000;
        }
        if (str.length() == 3) {
            return ((str.charAt(0) - '0') * 100) + ((str.charAt(1) - '0') * 10) + (str.charAt(2) - '0') + 1900;
        }
        if (str.length() == 4) {
            return ((str.charAt(0) - '0') * DateTime.THOUSAND) + ((str.charAt(1) - '0') * 100) + ((str.charAt(2) - '0') * 10) + (str.charAt(3) - '0');
        }
        return 1970;
    }

    private static TimeOfDay getTime(String str) {
        int i;
        int iCharAt = str.charAt(0) - '0';
        if (str.charAt(1) != ':') {
            i = 2;
            iCharAt = (iCharAt * 10) + (str.charAt(1) - '0');
        } else {
            i = 1;
        }
        int i2 = i + 1 + 1 + 1 + 1;
        return new TimeOfDay(iCharAt, ((str.charAt(r2) - '0') * 10) + (str.charAt(r3) - '0'), ((str.charAt(i2) - '0') * 10) + (str.charAt(i2 + 1) - '0'));
    }
}
