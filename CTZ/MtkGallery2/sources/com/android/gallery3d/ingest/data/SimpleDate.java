package com.android.gallery3d.ingest.data;

import android.annotation.TargetApi;
import java.text.DateFormat;
import java.util.Calendar;

@TargetApi(12)
public class SimpleDate implements Comparable<SimpleDate> {
    private static Calendar sCalendarInstance = Calendar.getInstance();
    public int day;
    private String mCachedStringRepresentation;
    public int month;
    private long timestamp;
    public int year;

    public void setTimestamp(long j) {
        synchronized (sCalendarInstance) {
            sCalendarInstance.setTimeInMillis(j);
            this.day = sCalendarInstance.get(5);
            this.month = sCalendarInstance.get(2);
            this.year = sCalendarInstance.get(1);
            this.timestamp = j;
            this.mCachedStringRepresentation = DateFormat.getDateInstance(3).format(Long.valueOf(j));
        }
    }

    public int hashCode() {
        return (31 * (((this.day + 31) * 31) + this.month)) + this.year;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != 0 && (obj instanceof SimpleDate) && this.year == obj.year && this.month == obj.month && this.day == obj.day) {
            return true;
        }
        return false;
    }

    @Override
    public int compareTo(SimpleDate simpleDate) {
        int year = this.year - simpleDate.getYear();
        if (year != 0) {
            return year;
        }
        int month = this.month - simpleDate.getMonth();
        if (month != 0) {
            return month;
        }
        return this.day - simpleDate.getDay();
    }

    public int getDay() {
        return this.day;
    }

    public int getMonth() {
        return this.month;
    }

    public int getYear() {
        return this.year;
    }

    public String toString() {
        if (this.mCachedStringRepresentation == null) {
            this.mCachedStringRepresentation = DateFormat.getDateInstance(3).format(Long.valueOf(this.timestamp));
        }
        return this.mCachedStringRepresentation;
    }
}
