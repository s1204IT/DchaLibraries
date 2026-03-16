package com.android.server.twilight;

import java.text.DateFormat;
import java.util.Date;

public class TwilightState {
    private final boolean mIsNight;
    private final long mTodaySunrise;
    private final long mTodaySunset;
    private final long mTomorrowSunrise;
    private final long mYesterdaySunset;

    TwilightState(boolean isNight, long yesterdaySunset, long todaySunrise, long todaySunset, long tomorrowSunrise) {
        this.mIsNight = isNight;
        this.mYesterdaySunset = yesterdaySunset;
        this.mTodaySunrise = todaySunrise;
        this.mTodaySunset = todaySunset;
        this.mTomorrowSunrise = tomorrowSunrise;
    }

    public boolean isNight() {
        return this.mIsNight;
    }

    public long getYesterdaySunset() {
        return this.mYesterdaySunset;
    }

    public long getTodaySunrise() {
        return this.mTodaySunrise;
    }

    public long getTodaySunset() {
        return this.mTodaySunset;
    }

    public long getTomorrowSunrise() {
        return this.mTomorrowSunrise;
    }

    public boolean equals(Object o) {
        return (o instanceof TwilightState) && equals((TwilightState) o);
    }

    public boolean equals(TwilightState other) {
        return other != null && this.mIsNight == other.mIsNight && this.mYesterdaySunset == other.mYesterdaySunset && this.mTodaySunrise == other.mTodaySunrise && this.mTodaySunset == other.mTodaySunset && this.mTomorrowSunrise == other.mTomorrowSunrise;
    }

    public int hashCode() {
        return 0;
    }

    public String toString() {
        DateFormat f = DateFormat.getDateTimeInstance();
        return "{TwilightState: isNight=" + this.mIsNight + ", mYesterdaySunset=" + f.format(new Date(this.mYesterdaySunset)) + ", mTodaySunrise=" + f.format(new Date(this.mTodaySunrise)) + ", mTodaySunset=" + f.format(new Date(this.mTodaySunset)) + ", mTomorrowSunrise=" + f.format(new Date(this.mTomorrowSunrise)) + "}";
    }
}
