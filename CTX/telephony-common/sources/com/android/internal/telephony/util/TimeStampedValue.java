package com.android.internal.telephony.util;

public final class TimeStampedValue<T> {
    public final long mElapsedRealtime;
    public final T mValue;

    public TimeStampedValue(T t, long j) {
        this.mValue = t;
        this.mElapsedRealtime = j;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TimeStampedValue timeStampedValue = (TimeStampedValue) obj;
        if (this.mElapsedRealtime != timeStampedValue.mElapsedRealtime) {
            return false;
        }
        if (this.mValue != null) {
            return this.mValue.equals(timeStampedValue.mValue);
        }
        if (timeStampedValue.mValue == null) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (this.mValue != null ? this.mValue.hashCode() : 0)) + ((int) (this.mElapsedRealtime ^ (this.mElapsedRealtime >>> 32)));
    }

    public String toString() {
        return "TimeStampedValue{mValue=" + this.mValue + ", elapsedRealtime=" + this.mElapsedRealtime + '}';
    }
}
