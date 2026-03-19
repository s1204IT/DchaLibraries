package android.icu.util;

import java.io.Serializable;

public class DateTimeRule implements Serializable {
    public static final int DOM = 0;
    public static final int DOW = 1;
    public static final int DOW_GEQ_DOM = 2;
    public static final int DOW_LEQ_DOM = 3;
    public static final int STANDARD_TIME = 1;
    public static final int UTC_TIME = 2;
    public static final int WALL_TIME = 0;
    private static final long serialVersionUID = 2183055795738051443L;
    private final int dateRuleType;
    private final int dayOfMonth;
    private final int dayOfWeek;
    private final int millisInDay;
    private final int month;
    private final int timeRuleType;
    private final int weekInMonth;
    private static final String[] DOWSTR = {"", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    private static final String[] MONSTR = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    public DateTimeRule(int month, int dayOfMonth, int millisInDay, int timeType) {
        this.dateRuleType = 0;
        this.month = month;
        this.dayOfMonth = dayOfMonth;
        this.millisInDay = millisInDay;
        this.timeRuleType = timeType;
        this.dayOfWeek = 0;
        this.weekInMonth = 0;
    }

    public DateTimeRule(int month, int weekInMonth, int dayOfWeek, int millisInDay, int timeType) {
        this.dateRuleType = 1;
        this.month = month;
        this.weekInMonth = weekInMonth;
        this.dayOfWeek = dayOfWeek;
        this.millisInDay = millisInDay;
        this.timeRuleType = timeType;
        this.dayOfMonth = 0;
    }

    public DateTimeRule(int month, int dayOfMonth, int dayOfWeek, boolean after, int millisInDay, int timeType) {
        this.dateRuleType = after ? 2 : 3;
        this.month = month;
        this.dayOfMonth = dayOfMonth;
        this.dayOfWeek = dayOfWeek;
        this.millisInDay = millisInDay;
        this.timeRuleType = timeType;
        this.weekInMonth = 0;
    }

    public int getDateRuleType() {
        return this.dateRuleType;
    }

    public int getRuleMonth() {
        return this.month;
    }

    public int getRuleDayOfMonth() {
        return this.dayOfMonth;
    }

    public int getRuleDayOfWeek() {
        return this.dayOfWeek;
    }

    public int getRuleWeekInMonth() {
        return this.weekInMonth;
    }

    public int getTimeRuleType() {
        return this.timeRuleType;
    }

    public int getRuleMillisInDay() {
        return this.millisInDay;
    }

    public String toString() {
        String sDate = null;
        String sTimeRuleType = null;
        switch (this.dateRuleType) {
            case 0:
                sDate = Integer.toString(this.dayOfMonth);
                break;
            case 1:
                sDate = Integer.toString(this.weekInMonth) + DOWSTR[this.dayOfWeek];
                break;
            case 2:
                sDate = DOWSTR[this.dayOfWeek] + ">=" + Integer.toString(this.dayOfMonth);
                break;
            case 3:
                sDate = DOWSTR[this.dayOfWeek] + "<=" + Integer.toString(this.dayOfMonth);
                break;
        }
        switch (this.timeRuleType) {
            case 0:
                sTimeRuleType = "WALL";
                break;
            case 1:
                sTimeRuleType = "STD";
                break;
            case 2:
                sTimeRuleType = "UTC";
                break;
        }
        int time = this.millisInDay;
        int millis = time % 1000;
        int time2 = time / 1000;
        int secs = time2 % 60;
        int time3 = time2 / 60;
        int mins = time3 % 60;
        int hours = time3 / 60;
        return "month=" + MONSTR[this.month] + ", date=" + sDate + ", time=" + hours + ":" + (mins / 10) + (mins % 10) + ":" + (secs / 10) + (secs % 10) + "." + (millis / 100) + ((millis / 10) % 10) + (millis % 10) + "(" + sTimeRuleType + ")";
    }
}
