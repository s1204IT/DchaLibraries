package androidx.car.moderator;

import android.os.SystemClock;
import androidx.car.moderator.ContentRateLimiter;

class SystemClockTimeProvider implements ContentRateLimiter.ElapsedTimeProvider {
    SystemClockTimeProvider() {
    }

    @Override
    public long getElapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }
}
