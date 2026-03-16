package com.android.server.usage;

public class UnixCalendar {
    private static final long DAY_IN_MILLIS = 86400000;
    private static final long MONTH_IN_MILLIS = 2592000000L;
    private static final long WEEK_IN_MILLIS = 604800000;
    private static final long YEAR_IN_MILLIS = 31536000000L;
    private long mTime;

    public UnixCalendar(long time) {
        this.mTime = time;
    }

    public void truncateToDay() {
        this.mTime -= this.mTime % DAY_IN_MILLIS;
    }

    public void truncateToWeek() {
        this.mTime -= this.mTime % WEEK_IN_MILLIS;
    }

    public void truncateToMonth() {
        this.mTime -= this.mTime % MONTH_IN_MILLIS;
    }

    public void truncateToYear() {
        this.mTime -= this.mTime % YEAR_IN_MILLIS;
    }

    public void addDays(int val) {
        this.mTime += ((long) val) * DAY_IN_MILLIS;
    }

    public void addWeeks(int val) {
        this.mTime += ((long) val) * WEEK_IN_MILLIS;
    }

    public void addMonths(int val) {
        this.mTime += ((long) val) * MONTH_IN_MILLIS;
    }

    public void addYears(int val) {
        this.mTime += ((long) val) * YEAR_IN_MILLIS;
    }

    public void setTimeInMillis(long time) {
        this.mTime = time;
    }

    public long getTimeInMillis() {
        return this.mTime;
    }

    public static void truncateTo(UnixCalendar calendar, int intervalType) {
        switch (intervalType) {
            case 0:
                calendar.truncateToDay();
                return;
            case 1:
                calendar.truncateToWeek();
                return;
            case 2:
                calendar.truncateToMonth();
                return;
            case 3:
                calendar.truncateToYear();
                return;
            default:
                throw new UnsupportedOperationException("Can't truncate date to interval " + intervalType);
        }
    }
}
