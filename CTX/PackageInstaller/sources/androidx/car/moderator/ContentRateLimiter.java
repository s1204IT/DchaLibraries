package androidx.car.moderator;

import android.support.v4.util.Preconditions;
import android.util.Log;
import java.util.concurrent.TimeUnit;

public class ContentRateLimiter {
    private final ElapsedTimeProvider mElapsedTimeProvider;
    private long mFillDelayMs;
    private double mLastCalculatedPermitCount;
    private double mMaxStoredPermits;
    private long mResumeIncrementingMs;
    private boolean mSecondaryFillDelayPermitAvailable;
    private long mStableIntervalMs;
    private boolean mUnlimitedModeEnabled;

    interface ElapsedTimeProvider {
        long getElapsedRealtime();
    }

    public ContentRateLimiter(double acquiredPermitsPerSecond, double maxStoredPermits, long fillDelayMs) {
        this(acquiredPermitsPerSecond, maxStoredPermits, fillDelayMs, new SystemClockTimeProvider());
    }

    ContentRateLimiter(double acquiredPermitsPerSecond, double maxStoredPermits, long fillDelayMs, ElapsedTimeProvider elapsedTimeProvider) {
        this.mSecondaryFillDelayPermitAvailable = true;
        this.mElapsedTimeProvider = elapsedTimeProvider;
        this.mResumeIncrementingMs = this.mElapsedTimeProvider.getElapsedRealtime();
        setAcquiredPermitsRate(acquiredPermitsPerSecond);
        setMaxStoredPermits(maxStoredPermits);
        setPermitFillDelay(fillDelayMs);
        if (Log.isLoggable("ContentRateLimiter", 2)) {
            Log.v("ContentRateLimiter", String.format("permitsPerSecond: %f maxStoredPermits: %f, fillDelayMs %d", Double.valueOf(acquiredPermitsPerSecond), Double.valueOf(maxStoredPermits), Long.valueOf(fillDelayMs)));
        }
    }

    public void setAcquiredPermitsRate(double acquiredPermitsPerSecond) {
        Preconditions.checkArgument(acquiredPermitsPerSecond > 0.0d);
        this.mStableIntervalMs = (long) (TimeUnit.SECONDS.toMillis(1L) / acquiredPermitsPerSecond);
    }

    public void setMaxStoredPermits(double maxStoredPermits) {
        Preconditions.checkArgument(maxStoredPermits >= 0.0d);
        this.mMaxStoredPermits = maxStoredPermits;
        this.mLastCalculatedPermitCount = maxStoredPermits;
    }

    public void setPermitFillDelay(long fillDelayMs) {
        Preconditions.checkArgument(fillDelayMs >= 0);
        this.mFillDelayMs = fillDelayMs;
    }

    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    public boolean tryAcquire(int permits) {
        if (this.mUnlimitedModeEnabled) {
            if (Log.isLoggable("ContentRateLimiter", 3)) {
                Log.d("ContentRateLimiter", "Unlimited mode is enabled.");
            }
            return true;
        }
        double availablePermits = getLastCalculatedPermitCount();
        long nowMs = this.mElapsedTimeProvider.getElapsedRealtime();
        if (Log.isLoggable("ContentRateLimiter", 2)) {
            Log.v("ContentRateLimiter", String.format("Requesting: %d, Stored: %f/%f", Integer.valueOf(permits), Double.valueOf(this.mLastCalculatedPermitCount), Double.valueOf(this.mMaxStoredPermits)));
        }
        if (availablePermits <= permits) {
            setLastCalculatedPermitCount(0.0d, this.mFillDelayMs + nowMs);
            return false;
        }
        if (nowMs < this.mResumeIncrementingMs && this.mSecondaryFillDelayPermitAvailable) {
            setLastCalculatedPermitCount(availablePermits, this.mFillDelayMs + nowMs);
            this.mSecondaryFillDelayPermitAvailable = false;
            if (Log.isLoggable("ContentRateLimiter", 3)) {
                Log.d("ContentRateLimiter", "Used up free secondary permit");
            }
            return true;
        }
        setLastCalculatedPermitCount(availablePermits - ((double) permits), this.mFillDelayMs + nowMs);
        if (Log.isLoggable("ContentRateLimiter", 2)) {
            Log.v("ContentRateLimiter", String.format("permits remaining %s, secondary permit available %s", Double.valueOf(this.mLastCalculatedPermitCount), Boolean.valueOf(this.mSecondaryFillDelayPermitAvailable)));
        }
        this.mSecondaryFillDelayPermitAvailable = true;
        return true;
    }

    public void setUnlimitedMode(boolean enabled) {
        this.mUnlimitedModeEnabled = enabled;
    }

    private double getLastCalculatedPermitCount() {
        long nowMs = this.mElapsedTimeProvider.getElapsedRealtime();
        if (nowMs > this.mResumeIncrementingMs) {
            long deltaMs = nowMs - this.mResumeIncrementingMs;
            double newPermits = deltaMs / this.mStableIntervalMs;
            setLastCalculatedPermitCount(this.mLastCalculatedPermitCount + newPermits, nowMs);
        }
        return this.mLastCalculatedPermitCount;
    }

    private void setLastCalculatedPermitCount(double newCount, long nextMs) {
        this.mLastCalculatedPermitCount = Math.min(this.mMaxStoredPermits, newCount);
        this.mResumeIncrementingMs = nextMs;
    }
}
