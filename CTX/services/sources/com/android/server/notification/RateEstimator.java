package com.android.server.notification;

public class RateEstimator {
    private static final double MINIMUM_DT = 5.0E-4d;
    private static final double RATE_ALPHA = 0.8d;
    private double mInterarrivalTime = 1000.0d;
    private Long mLastEventTime;

    public float update(long j) {
        float f;
        if (this.mLastEventTime == null) {
            f = 0.0f;
        } else {
            this.mInterarrivalTime = getInterarrivalEstimate(j);
            f = (float) (1.0d / this.mInterarrivalTime);
        }
        this.mLastEventTime = Long.valueOf(j);
        return f;
    }

    public float getRate(long j) {
        if (this.mLastEventTime == null) {
            return 0.0f;
        }
        return (float) (1.0d / getInterarrivalEstimate(j));
    }

    private double getInterarrivalEstimate(long j) {
        return (RATE_ALPHA * this.mInterarrivalTime) + (0.19999999999999996d * Math.max((j - this.mLastEventTime.longValue()) / 1000.0d, MINIMUM_DT));
    }
}
