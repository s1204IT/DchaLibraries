package java.time;

import java.io.Serializable;
import java.time.temporal.TemporalAmount;
import java.util.Objects;

public abstract class Clock {
    public abstract ZoneId getZone();

    public abstract Instant instant();

    public abstract Clock withZone(ZoneId zoneId);

    public static Clock systemUTC() {
        return new SystemClock(ZoneOffset.UTC);
    }

    public static Clock systemDefaultZone() {
        return new SystemClock(ZoneId.systemDefault());
    }

    public static Clock system(ZoneId zoneId) {
        Objects.requireNonNull(zoneId, "zone");
        return new SystemClock(zoneId);
    }

    public static Clock tickSeconds(ZoneId zoneId) {
        return new TickClock(system(zoneId), 1000000000L);
    }

    public static Clock tickMinutes(ZoneId zoneId) {
        return new TickClock(system(zoneId), 60000000000L);
    }

    public static Clock tick(Clock clock, Duration duration) {
        Objects.requireNonNull(clock, "baseClock");
        Objects.requireNonNull(duration, "tickDuration");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Tick duration must not be negative");
        }
        long nanos = duration.toNanos();
        if (nanos % 1000000 != 0 && 1000000000 % nanos != 0) {
            throw new IllegalArgumentException("Invalid tick duration");
        }
        if (nanos <= 1) {
            return clock;
        }
        return new TickClock(clock, nanos);
    }

    public static Clock fixed(Instant instant, ZoneId zoneId) {
        Objects.requireNonNull(instant, "fixedInstant");
        Objects.requireNonNull(zoneId, "zone");
        return new FixedClock(instant, zoneId);
    }

    public static Clock offset(Clock clock, Duration duration) {
        Objects.requireNonNull(clock, "baseClock");
        Objects.requireNonNull(duration, "offsetDuration");
        if (duration.equals(Duration.ZERO)) {
            return clock;
        }
        return new OffsetClock(clock, duration);
    }

    protected Clock() {
    }

    public long millis() {
        return instant().toEpochMilli();
    }

    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    public int hashCode() {
        return super.hashCode();
    }

    static final class SystemClock extends Clock implements Serializable {
        private static final long serialVersionUID = 6740630888130243051L;
        private final ZoneId zone;

        SystemClock(ZoneId zoneId) {
            this.zone = zoneId;
        }

        @Override
        public ZoneId getZone() {
            return this.zone;
        }

        @Override
        public Clock withZone(ZoneId zoneId) {
            if (zoneId.equals(this.zone)) {
                return this;
            }
            return new SystemClock(zoneId);
        }

        @Override
        public long millis() {
            return System.currentTimeMillis();
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof SystemClock) {
                return this.zone.equals(((SystemClock) obj).zone);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.zone.hashCode() + 1;
        }

        public String toString() {
            return "SystemClock[" + ((Object) this.zone) + "]";
        }
    }

    static final class FixedClock extends Clock implements Serializable {
        private static final long serialVersionUID = 7430389292664866958L;
        private final Instant instant;
        private final ZoneId zone;

        FixedClock(Instant instant, ZoneId zoneId) {
            this.instant = instant;
            this.zone = zoneId;
        }

        @Override
        public ZoneId getZone() {
            return this.zone;
        }

        @Override
        public Clock withZone(ZoneId zoneId) {
            if (zoneId.equals(this.zone)) {
                return this;
            }
            return new FixedClock(this.instant, zoneId);
        }

        @Override
        public long millis() {
            return this.instant.toEpochMilli();
        }

        @Override
        public Instant instant() {
            return this.instant;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof FixedClock)) {
                return false;
            }
            FixedClock fixedClock = (FixedClock) obj;
            return this.instant.equals(fixedClock.instant) && this.zone.equals(fixedClock.zone);
        }

        @Override
        public int hashCode() {
            return this.instant.hashCode() ^ this.zone.hashCode();
        }

        public String toString() {
            return "FixedClock[" + ((Object) this.instant) + "," + ((Object) this.zone) + "]";
        }
    }

    static final class OffsetClock extends Clock implements Serializable {
        private static final long serialVersionUID = 2007484719125426256L;
        private final Clock baseClock;
        private final Duration offset;

        OffsetClock(Clock clock, Duration duration) {
            this.baseClock = clock;
            this.offset = duration;
        }

        @Override
        public ZoneId getZone() {
            return this.baseClock.getZone();
        }

        @Override
        public Clock withZone(ZoneId zoneId) {
            if (zoneId.equals(this.baseClock.getZone())) {
                return this;
            }
            return new OffsetClock(this.baseClock.withZone(zoneId), this.offset);
        }

        @Override
        public long millis() {
            return Math.addExact(this.baseClock.millis(), this.offset.toMillis());
        }

        @Override
        public Instant instant() {
            return this.baseClock.instant().plus((TemporalAmount) this.offset);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof OffsetClock)) {
                return false;
            }
            OffsetClock offsetClock = (OffsetClock) obj;
            return this.baseClock.equals(offsetClock.baseClock) && this.offset.equals(offsetClock.offset);
        }

        @Override
        public int hashCode() {
            return this.baseClock.hashCode() ^ this.offset.hashCode();
        }

        public String toString() {
            return "OffsetClock[" + ((Object) this.baseClock) + "," + ((Object) this.offset) + "]";
        }
    }

    static final class TickClock extends Clock implements Serializable {
        private static final long serialVersionUID = 6504659149906368850L;
        private final Clock baseClock;
        private final long tickNanos;

        TickClock(Clock clock, long j) {
            this.baseClock = clock;
            this.tickNanos = j;
        }

        @Override
        public ZoneId getZone() {
            return this.baseClock.getZone();
        }

        @Override
        public Clock withZone(ZoneId zoneId) {
            if (zoneId.equals(this.baseClock.getZone())) {
                return this;
            }
            return new TickClock(this.baseClock.withZone(zoneId), this.tickNanos);
        }

        @Override
        public long millis() {
            long jMillis = this.baseClock.millis();
            return jMillis - Math.floorMod(jMillis, this.tickNanos / 1000000);
        }

        @Override
        public Instant instant() {
            if (this.tickNanos % 1000000 == 0) {
                long jMillis = this.baseClock.millis();
                return Instant.ofEpochMilli(jMillis - Math.floorMod(jMillis, this.tickNanos / 1000000));
            }
            return this.baseClock.instant().minusNanos(Math.floorMod(r0.getNano(), this.tickNanos));
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TickClock)) {
                return false;
            }
            TickClock tickClock = (TickClock) obj;
            return this.baseClock.equals(tickClock.baseClock) && this.tickNanos == tickClock.tickNanos;
        }

        @Override
        public int hashCode() {
            return this.baseClock.hashCode() ^ ((int) (this.tickNanos ^ (this.tickNanos >>> 32)));
        }

        public String toString() {
            return "TickClock[" + ((Object) this.baseClock) + "," + ((Object) Duration.ofNanos(this.tickNanos)) + "]";
        }
    }
}
