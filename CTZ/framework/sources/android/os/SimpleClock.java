package android.os;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public abstract class SimpleClock extends Clock {
    private final ZoneId zone;

    @Override
    public abstract long millis();

    public SimpleClock(ZoneId zoneId) {
        this.zone = zoneId;
    }

    @Override
    public ZoneId getZone() {
        return this.zone;
    }

    @Override
    public Clock withZone(ZoneId zoneId) {
        return new SimpleClock(zoneId) {
            @Override
            public long millis() {
                return SimpleClock.this.millis();
            }
        };
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochMilli(millis());
    }
}
