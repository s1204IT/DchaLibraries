package com.mediatek.vcalendar;

public class VCalendarException extends Exception {
    public static final int FILE_READ_EXCEPTION = 3;
    public static final int FORMAT_EXCEPTION = 0;
    public static final int NO_ACCOUNT_EXCEPTION = 1;
    public static final int NO_EVENT_EXCEPTION = 2;
    private static final long serialVersionUID = 1;

    public VCalendarException() {
    }

    public VCalendarException(String str) {
        super(str);
    }

    public VCalendarException(String str, Throwable th) {
        super(str, th);
    }
}
