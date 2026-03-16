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

    public void setTimestamp(long timestamp) {
        synchronized (sCalendarInstance) {
            sCalendarInstance.setTimeInMillis(timestamp);
            this.day = sCalendarInstance.get(5);
            this.month = sCalendarInstance.get(2);
            this.year = sCalendarInstance.get(1);
            this.timestamp = timestamp;
            this.mCachedStringRepresentation = DateFormat.getDateInstance(3).format(Long.valueOf(timestamp));
        }
    }

    public int hashCode() {
        int result = this.day + 31;
        return (((result * 31) + this.month) * 31) + this.year;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && (obj instanceof SimpleDate)) {
            SimpleDate other = (SimpleDate) obj;
            return this.year == other.year && this.month == other.month && this.day == other.day;
        }
        return false;
    }

    @Override
    public int compareTo(SimpleDate other) {
        int yearDiff = this.year - other.getYear();
        if (yearDiff == 0) {
            int monthDiff = this.month - other.getMonth();
            return monthDiff != 0 ? monthDiff : this.day - other.getDay();
        }
        return yearDiff;
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
