package com.android.server.usage;

public class UnixCalendar {
    public static final long DAY_IN_MILLIS = 86400000;
    public static final long MONTH_IN_MILLIS = 2592000000L;
    public static final long WEEK_IN_MILLIS = 604800000;
    public static final long YEAR_IN_MILLIS = 31536000000L;
    private long mTime;

    public UnixCalendar(long j) {
        this.mTime = j;
    }

    public void addDays(int i) {
        this.mTime += ((long) i) * 86400000;
    }

    public void addWeeks(int i) {
        this.mTime += ((long) i) * WEEK_IN_MILLIS;
    }

    public void addMonths(int i) {
        this.mTime += ((long) i) * MONTH_IN_MILLIS;
    }

    public void addYears(int i) {
        this.mTime += ((long) i) * YEAR_IN_MILLIS;
    }

    public void setTimeInMillis(long j) {
        this.mTime = j;
    }

    public long getTimeInMillis() {
        return this.mTime;
    }
}
