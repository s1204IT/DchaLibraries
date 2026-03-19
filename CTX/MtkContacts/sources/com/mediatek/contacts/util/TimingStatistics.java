package com.mediatek.contacts.util;

import android.os.Build;

public class TimingStatistics {
    private static boolean sTimingable = Build.TYPE.equals("eng");
    private String mTargetClassName;
    private long mTimeTotalUsed = 0;
    private long mTimingStart = 0;
    private int mTimingCount = 0;

    public TimingStatistics(String str) {
        this.mTargetClassName = null;
        this.mTargetClassName = str == null ? TimingStatistics.class.getSimpleName() : str;
    }

    public void timingStart() {
        if (!isTimingEnable()) {
            Log.i("TimingStatistics", "[timingStart] fail.");
        } else {
            this.mTimingStart = System.currentTimeMillis();
        }
    }

    public long timingEnd() {
        if (!isTimingEnable()) {
            Log.i("TimingStatistics", "[timingEnd] fail.");
            return 0L;
        }
        long jCurrentTimeMillis = System.currentTimeMillis() - this.mTimingStart;
        this.mTimeTotalUsed += jCurrentTimeMillis;
        this.mTimingCount++;
        return jCurrentTimeMillis;
    }

    public long getTimingTotalUsed() {
        return this.mTimeTotalUsed;
    }

    public int getTimingCount() {
        return this.mTimingCount;
    }

    public static boolean isTimingEnable() {
        return sTimingable;
    }

    public void log(String... strArr) {
        String str;
        if (!isTimingEnable()) {
            return;
        }
        if (strArr == null) {
            str = null;
        } else {
            str = "[" + strArr[0] + "]";
        }
        Log.i("TimingStatistics", "[Performance test][Contacts][Timing][ " + this.mTargetClassName + "]" + str + " mTimeTotalUsed:" + getTimingTotalUsed() + ",mTimingCount:" + this.mTimingCount + ",average:" + ((getTimingTotalUsed() * 1.0d) / ((double) getTimingCount())));
    }
}
