package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.SmoothRateLimiter;
import java.util.concurrent.TimeUnit;

public abstract class RateLimiter {
    private volatile Object mutexDoNotUseDirectly;
    private final SleepingStopwatch stopwatch;

    static abstract class SleepingStopwatch {
        SleepingStopwatch() {
        }

        abstract long readMicros();
    }

    RateLimiter(SleepingStopwatch sleepingStopwatch) {
        this.stopwatch = (SleepingStopwatch) Preconditions.checkNotNull(sleepingStopwatch);
    }

    static RateLimiter create(SleepingStopwatch sleepingStopwatch, double d) {
        SmoothRateLimiter.SmoothBursty smoothBursty = new SmoothRateLimiter.SmoothBursty(sleepingStopwatch, 1.0d);
        smoothBursty.setRate(d);
        return smoothBursty;
    }

    static RateLimiter create(SleepingStopwatch sleepingStopwatch, double d, long j, TimeUnit timeUnit) {
        SmoothRateLimiter.SmoothWarmingUp smoothWarmingUp = new SmoothRateLimiter.SmoothWarmingUp(sleepingStopwatch, j, timeUnit);
        smoothWarmingUp.setRate(d);
        return smoothWarmingUp;
    }

    private Object mutex() {
        Object obj = this.mutexDoNotUseDirectly;
        if (obj == null) {
            synchronized (this) {
                obj = this.mutexDoNotUseDirectly;
                if (obj == null) {
                    obj = new Object();
                    this.mutexDoNotUseDirectly = obj;
                }
            }
        }
        return obj;
    }

    abstract double doGetRate();

    abstract void doSetRate(double d, long j);

    public final double getRate() {
        double dDoGetRate;
        synchronized (mutex()) {
            dDoGetRate = doGetRate();
        }
        return dDoGetRate;
    }

    public final void setRate(double d) {
        Preconditions.checkArgument(d > 0.0d && !Double.isNaN(d), "rate must be positive");
        synchronized (mutex()) {
            doSetRate(d, this.stopwatch.readMicros());
        }
    }

    public String toString() {
        return String.format("RateLimiter[stableRate=%3.1fqps]", Double.valueOf(getRate()));
    }
}
