package com.google.common.util.concurrent;

import com.google.common.util.concurrent.RateLimiter;
import java.util.concurrent.TimeUnit;

abstract class SmoothRateLimiter extends RateLimiter {
    double maxPermits;
    private long nextFreeTicketMicros;
    double stableIntervalMicros;
    double storedPermits;

    abstract void doSetRate(double d, double d2);

    abstract long storedPermitsToWaitTime(double d, double d2);

    static final class SmoothWarmingUp extends SmoothRateLimiter {
        private double halfPermits;
        private double slope;
        private final long warmupPeriodMicros;

        SmoothWarmingUp(RateLimiter.SleepingStopwatch sleepingStopwatch, long j, TimeUnit timeUnit) {
            super(sleepingStopwatch);
            this.warmupPeriodMicros = timeUnit.toMicros(j);
        }

        @Override
        void doSetRate(double d, double d2) {
            double d3;
            double d4 = this.maxPermits;
            this.maxPermits = this.warmupPeriodMicros / d2;
            this.halfPermits = this.maxPermits / 2.0d;
            this.slope = ((3.0d * d2) - d2) / this.halfPermits;
            if (d4 == Double.POSITIVE_INFINITY) {
                this.storedPermits = 0.0d;
                return;
            }
            if (d4 == 0.0d) {
                d3 = this.maxPermits;
            } else {
                d3 = (this.storedPermits * this.maxPermits) / d4;
            }
            this.storedPermits = d3;
        }

        @Override
        long storedPermitsToWaitTime(double d, double d2) {
            long jPermitsToTime;
            double d3 = d - this.halfPermits;
            if (d3 > 0.0d) {
                double dMin = Math.min(d3, d2);
                jPermitsToTime = (long) (((permitsToTime(d3) + permitsToTime(d3 - dMin)) * dMin) / 2.0d);
                d2 -= dMin;
            } else {
                jPermitsToTime = 0;
            }
            return (long) (jPermitsToTime + (this.stableIntervalMicros * d2));
        }

        private double permitsToTime(double d) {
            return this.stableIntervalMicros + (d * this.slope);
        }
    }

    static final class SmoothBursty extends SmoothRateLimiter {
        final double maxBurstSeconds;

        SmoothBursty(RateLimiter.SleepingStopwatch sleepingStopwatch, double d) {
            super(sleepingStopwatch);
            this.maxBurstSeconds = d;
        }

        @Override
        void doSetRate(double d, double d2) {
            double d3 = this.maxPermits;
            this.maxPermits = this.maxBurstSeconds * d;
            if (d3 == Double.POSITIVE_INFINITY) {
                this.storedPermits = this.maxPermits;
                return;
            }
            double d4 = 0.0d;
            if (d3 != 0.0d) {
                d4 = (this.storedPermits * this.maxPermits) / d3;
            }
            this.storedPermits = d4;
        }

        @Override
        long storedPermitsToWaitTime(double d, double d2) {
            return 0L;
        }
    }

    private SmoothRateLimiter(RateLimiter.SleepingStopwatch sleepingStopwatch) {
        super(sleepingStopwatch);
        this.nextFreeTicketMicros = 0L;
    }

    @Override
    final void doSetRate(double d, long j) {
        resync(j);
        double micros = TimeUnit.SECONDS.toMicros(1L) / d;
        this.stableIntervalMicros = micros;
        doSetRate(d, micros);
    }

    @Override
    final double doGetRate() {
        return TimeUnit.SECONDS.toMicros(1L) / this.stableIntervalMicros;
    }

    @Override
    final long queryEarliestAvailable(long j) {
        return this.nextFreeTicketMicros;
    }

    @Override
    final long reserveEarliestAvailable(int i, long j) {
        resync(j);
        long j2 = this.nextFreeTicketMicros;
        double d = i;
        double dMin = Math.min(d, this.storedPermits);
        this.nextFreeTicketMicros += storedPermitsToWaitTime(this.storedPermits, dMin) + ((long) ((d - dMin) * this.stableIntervalMicros));
        this.storedPermits -= dMin;
        return j2;
    }

    private void resync(long j) {
        if (j > this.nextFreeTicketMicros) {
            this.storedPermits = Math.min(this.maxPermits, this.storedPermits + ((j - this.nextFreeTicketMicros) / this.stableIntervalMicros));
            this.nextFreeTicketMicros = j;
        }
    }
}
