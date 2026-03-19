package android.os;

import android.util.Log;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Arrays;

public class BestClock extends SimpleClock {
    private static final String TAG = "BestClock";
    private final Clock[] clocks;

    public BestClock(ZoneId zoneId, Clock... clockArr) {
        super(zoneId);
        this.clocks = clockArr;
    }

    @Override
    public long millis() {
        for (Clock clock : this.clocks) {
            try {
                return clock.millis();
            } catch (DateTimeException e) {
                Log.w(TAG, e.toString());
            }
        }
        throw new DateTimeException("No clocks in " + Arrays.toString(this.clocks) + " were able to provide time");
    }
}
