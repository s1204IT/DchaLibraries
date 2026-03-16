package com.android.okhttp.internal.http;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class HttpDate {
    private static final ThreadLocal<DateFormat> STANDARD_DATE_FORMAT = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            DateFormat rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
            rfc1123.setTimeZone(TimeZone.getTimeZone("GMT"));
            return rfc1123;
        }
    };
    private static final String[] BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS = {"EEEE, dd-MMM-yy HH:mm:ss zzz", "EEE MMM d HH:mm:ss yyyy", "EEE, dd-MMM-yyyy HH:mm:ss z", "EEE, dd-MMM-yyyy HH-mm-ss z", "EEE, dd MMM yy HH:mm:ss z", "EEE dd-MMM-yyyy HH:mm:ss z", "EEE dd MMM yyyy HH:mm:ss z", "EEE dd-MMM-yyyy HH-mm-ss z", "EEE dd-MMM-yy HH:mm:ss z", "EEE dd MMM yy HH:mm:ss z", "EEE,dd-MMM-yy HH:mm:ss z", "EEE,dd-MMM-yyyy HH:mm:ss z", "EEE, dd-MM-yyyy HH:mm:ss z", "EEE MMM d yyyy HH:mm:ss z"};
    private static final DateFormat[] BROWSER_COMPATIBLE_DATE_FORMATS = new DateFormat[BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS.length];

    public static Date parse(String value) {
        try {
            return STANDARD_DATE_FORMAT.get().parse(value);
        } catch (ParseException e) {
            synchronized (BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS) {
                int count = BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS.length;
                for (int i = 0; i < count; i++) {
                    DateFormat format = BROWSER_COMPATIBLE_DATE_FORMATS[i];
                    if (format == null) {
                        format = new SimpleDateFormat(BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS[i], Locale.US);
                        BROWSER_COMPATIBLE_DATE_FORMATS[i] = format;
                    }
                    try {
                        return format.parse(value);
                    } catch (ParseException e2) {
                    }
                }
                return null;
            }
        }
    }

    public static String format(Date value) {
        return STANDARD_DATE_FORMAT.get().format(value);
    }

    private HttpDate() {
    }
}
