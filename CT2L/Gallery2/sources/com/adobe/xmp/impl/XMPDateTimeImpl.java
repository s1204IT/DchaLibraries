package com.adobe.xmp.impl;

import com.adobe.xmp.XMPDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class XMPDateTimeImpl implements XMPDateTime {
    private int day;
    private int hour;
    private int minute;
    private int month;
    private int nanoSeconds;
    private int second;
    private TimeZone timeZone;
    private int year;

    public XMPDateTimeImpl() {
        this.year = 0;
        this.month = 0;
        this.day = 0;
        this.hour = 0;
        this.minute = 0;
        this.second = 0;
        this.timeZone = TimeZone.getTimeZone("UTC");
    }

    public XMPDateTimeImpl(Calendar calendar) {
        this.year = 0;
        this.month = 0;
        this.day = 0;
        this.hour = 0;
        this.minute = 0;
        this.second = 0;
        this.timeZone = TimeZone.getTimeZone("UTC");
        Date date = calendar.getTime();
        TimeZone zone = calendar.getTimeZone();
        GregorianCalendar intCalendar = (GregorianCalendar) Calendar.getInstance(Locale.US);
        intCalendar.setGregorianChange(new Date(Long.MIN_VALUE));
        intCalendar.setTimeZone(zone);
        intCalendar.setTime(date);
        this.year = intCalendar.get(1);
        this.month = intCalendar.get(2) + 1;
        this.day = intCalendar.get(5);
        this.hour = intCalendar.get(11);
        this.minute = intCalendar.get(12);
        this.second = intCalendar.get(13);
        this.nanoSeconds = intCalendar.get(14) * 1000000;
        this.timeZone = intCalendar.getTimeZone();
    }

    @Override
    public int getYear() {
        return this.year;
    }

    @Override
    public void setYear(int year) {
        this.year = Math.min(Math.abs(year), 9999);
    }

    @Override
    public int getMonth() {
        return this.month;
    }

    @Override
    public void setMonth(int month) {
        if (month < 1) {
            this.month = 1;
        } else if (month > 12) {
            this.month = 12;
        } else {
            this.month = month;
        }
    }

    @Override
    public int getDay() {
        return this.day;
    }

    @Override
    public void setDay(int day) {
        if (day < 1) {
            this.day = 1;
        } else if (day > 31) {
            this.day = 31;
        } else {
            this.day = day;
        }
    }

    @Override
    public int getHour() {
        return this.hour;
    }

    @Override
    public void setHour(int hour) {
        this.hour = Math.min(Math.abs(hour), 23);
    }

    @Override
    public int getMinute() {
        return this.minute;
    }

    @Override
    public void setMinute(int minute) {
        this.minute = Math.min(Math.abs(minute), 59);
    }

    @Override
    public int getSecond() {
        return this.second;
    }

    @Override
    public void setSecond(int second) {
        this.second = Math.min(Math.abs(second), 59);
    }

    @Override
    public int getNanoSecond() {
        return this.nanoSeconds;
    }

    @Override
    public void setNanoSecond(int nanoSecond) {
        this.nanoSeconds = nanoSecond;
    }

    @Override
    public int compareTo(Object dt) {
        long d = getCalendar().getTimeInMillis() - ((XMPDateTime) dt).getCalendar().getTimeInMillis();
        if (d != 0) {
            return (int) (d % 2);
        }
        return (int) (((long) (this.nanoSeconds - ((XMPDateTime) dt).getNanoSecond())) % 2);
    }

    @Override
    public TimeZone getTimeZone() {
        return this.timeZone;
    }

    @Override
    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    @Override
    public Calendar getCalendar() {
        GregorianCalendar calendar = (GregorianCalendar) Calendar.getInstance(Locale.US);
        calendar.setGregorianChange(new Date(Long.MIN_VALUE));
        calendar.setTimeZone(this.timeZone);
        calendar.set(1, this.year);
        calendar.set(2, this.month - 1);
        calendar.set(5, this.day);
        calendar.set(11, this.hour);
        calendar.set(12, this.minute);
        calendar.set(13, this.second);
        calendar.set(14, this.nanoSeconds / 1000000);
        return calendar;
    }

    public String getISO8601String() {
        return ISO8601Converter.render(this);
    }

    public String toString() {
        return getISO8601String();
    }
}
