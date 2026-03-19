package java.time.temporal;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.Chronology;

public final class TemporalQueries {
    static final TemporalQuery<ZoneId> ZONE_ID = new TemporalQuery() {
        @Override
        public final Object queryFrom(TemporalAccessor temporalAccessor) {
            return TemporalQueries.lambda$static$0(temporalAccessor);
        }
    };
    static final TemporalQuery<Chronology> CHRONO = new TemporalQuery() {
        @Override
        public final Object queryFrom(TemporalAccessor temporalAccessor) {
            return TemporalQueries.lambda$static$1(temporalAccessor);
        }
    };
    static final TemporalQuery<TemporalUnit> PRECISION = new TemporalQuery() {
        @Override
        public final Object queryFrom(TemporalAccessor temporalAccessor) {
            return TemporalQueries.lambda$static$2(temporalAccessor);
        }
    };
    static final TemporalQuery<ZoneOffset> OFFSET = new TemporalQuery() {
        @Override
        public final Object queryFrom(TemporalAccessor temporalAccessor) {
            return TemporalQueries.lambda$static$3(temporalAccessor);
        }
    };
    static final TemporalQuery<ZoneId> ZONE = new TemporalQuery() {
        @Override
        public final Object queryFrom(TemporalAccessor temporalAccessor) {
            return TemporalQueries.lambda$static$4(temporalAccessor);
        }
    };
    static final TemporalQuery<LocalDate> LOCAL_DATE = new TemporalQuery() {
        @Override
        public final Object queryFrom(TemporalAccessor temporalAccessor) {
            return TemporalQueries.lambda$static$5(temporalAccessor);
        }
    };
    static final TemporalQuery<LocalTime> LOCAL_TIME = new TemporalQuery() {
        @Override
        public final Object queryFrom(TemporalAccessor temporalAccessor) {
            return TemporalQueries.lambda$static$6(temporalAccessor);
        }
    };

    private TemporalQueries() {
    }

    public static TemporalQuery<ZoneId> zoneId() {
        return ZONE_ID;
    }

    public static TemporalQuery<Chronology> chronology() {
        return CHRONO;
    }

    public static TemporalQuery<TemporalUnit> precision() {
        return PRECISION;
    }

    public static TemporalQuery<ZoneId> zone() {
        return ZONE;
    }

    public static TemporalQuery<ZoneOffset> offset() {
        return OFFSET;
    }

    public static TemporalQuery<LocalDate> localDate() {
        return LOCAL_DATE;
    }

    public static TemporalQuery<LocalTime> localTime() {
        return LOCAL_TIME;
    }

    static ZoneId lambda$static$0(TemporalAccessor temporalAccessor) {
        return (ZoneId) temporalAccessor.query(ZONE_ID);
    }

    static Chronology lambda$static$1(TemporalAccessor temporalAccessor) {
        return (Chronology) temporalAccessor.query(CHRONO);
    }

    static TemporalUnit lambda$static$2(TemporalAccessor temporalAccessor) {
        return (TemporalUnit) temporalAccessor.query(PRECISION);
    }

    static ZoneOffset lambda$static$3(TemporalAccessor temporalAccessor) {
        if (temporalAccessor.isSupported(ChronoField.OFFSET_SECONDS)) {
            return ZoneOffset.ofTotalSeconds(temporalAccessor.get(ChronoField.OFFSET_SECONDS));
        }
        return null;
    }

    static ZoneId lambda$static$4(TemporalAccessor temporalAccessor) {
        ZoneId zoneId = (ZoneId) temporalAccessor.query(ZONE_ID);
        return zoneId != null ? zoneId : (ZoneId) temporalAccessor.query(OFFSET);
    }

    static LocalDate lambda$static$5(TemporalAccessor temporalAccessor) {
        if (temporalAccessor.isSupported(ChronoField.EPOCH_DAY)) {
            return LocalDate.ofEpochDay(temporalAccessor.getLong(ChronoField.EPOCH_DAY));
        }
        return null;
    }

    static LocalTime lambda$static$6(TemporalAccessor temporalAccessor) {
        if (temporalAccessor.isSupported(ChronoField.NANO_OF_DAY)) {
            return LocalTime.ofNanoOfDay(temporalAccessor.getLong(ChronoField.NANO_OF_DAY));
        }
        return null;
    }
}
