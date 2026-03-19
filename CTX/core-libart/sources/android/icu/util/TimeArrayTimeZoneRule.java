package android.icu.util;

import java.util.Arrays;
import java.util.Date;

public class TimeArrayTimeZoneRule extends TimeZoneRule {
    private static final long serialVersionUID = -1117109130077415245L;
    private final long[] startTimes;
    private final int timeType;

    public TimeArrayTimeZoneRule(String str, int i, int i2, long[] jArr, int i3) {
        super(str, i, i2);
        if (jArr == null || jArr.length == 0) {
            throw new IllegalArgumentException("No start times are specified.");
        }
        this.startTimes = (long[]) jArr.clone();
        Arrays.sort(this.startTimes);
        this.timeType = i3;
    }

    public long[] getStartTimes() {
        return (long[]) this.startTimes.clone();
    }

    public int getTimeType() {
        return this.timeType;
    }

    @Override
    public Date getFirstStart(int i, int i2) {
        return new Date(getUTC(this.startTimes[0], i, i2));
    }

    @Override
    public Date getFinalStart(int i, int i2) {
        return new Date(getUTC(this.startTimes[this.startTimes.length - 1], i, i2));
    }

    @Override
    public Date getNextStart(long j, int i, int i2, boolean z) {
        int length = this.startTimes.length - 1;
        while (length >= 0) {
            long utc = getUTC(this.startTimes[length], i, i2);
            if (utc < j || (!z && utc == j)) {
                break;
            }
            length--;
        }
        if (length == this.startTimes.length - 1) {
            return null;
        }
        return new Date(getUTC(this.startTimes[length + 1], i, i2));
    }

    @Override
    public Date getPreviousStart(long j, int i, int i2, boolean z) {
        for (int length = this.startTimes.length - 1; length >= 0; length--) {
            long utc = getUTC(this.startTimes[length], i, i2);
            if (utc < j || (z && utc == j)) {
                return new Date(utc);
            }
        }
        return null;
    }

    @Override
    public boolean isEquivalentTo(TimeZoneRule timeZoneRule) {
        if (!(timeZoneRule instanceof TimeArrayTimeZoneRule)) {
            return false;
        }
        TimeArrayTimeZoneRule timeArrayTimeZoneRule = (TimeArrayTimeZoneRule) timeZoneRule;
        if (this.timeType == timeArrayTimeZoneRule.timeType && Arrays.equals(this.startTimes, timeArrayTimeZoneRule.startTimes)) {
            return super.isEquivalentTo(timeZoneRule);
        }
        return false;
    }

    @Override
    public boolean isTransitionRule() {
        return true;
    }

    private long getUTC(long j, int i, int i2) {
        if (this.timeType != 2) {
            j -= (long) i;
        }
        if (this.timeType == 0) {
            return j - ((long) i2);
        }
        return j;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(", timeType=");
        sb.append(this.timeType);
        sb.append(", startTimes=[");
        for (int i = 0; i < this.startTimes.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(Long.toString(this.startTimes[i]));
        }
        sb.append("]");
        return sb.toString();
    }
}
