package java.time.chrono;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalUnit;
import java.util.Comparator;
import java.util.Objects;

public interface ChronoLocalDateTime<D extends ChronoLocalDate> extends Temporal, TemporalAdjuster, Comparable<ChronoLocalDateTime<?>> {
    ChronoZonedDateTime<D> atZone(ZoneId zoneId);

    boolean equals(Object obj);

    int hashCode();

    @Override
    boolean isSupported(TemporalField temporalField);

    @Override
    ChronoLocalDateTime<D> plus(long j, TemporalUnit temporalUnit);

    D toLocalDate();

    LocalTime toLocalTime();

    String toString();

    @Override
    ChronoLocalDateTime<D> with(TemporalField temporalField, long j);

    static Comparator<ChronoLocalDateTime<?>> timeLineOrder() {
        return AbstractChronology.DATE_TIME_ORDER;
    }

    static ChronoLocalDateTime<?> from(TemporalAccessor temporalAccessor) {
        if (temporalAccessor instanceof ChronoLocalDateTime) {
            return (ChronoLocalDateTime) temporalAccessor;
        }
        Objects.requireNonNull(temporalAccessor, "temporal");
        Chronology chronology = (Chronology) temporalAccessor.query(TemporalQueries.chronology());
        if (chronology == null) {
            throw new DateTimeException("Unable to obtain ChronoLocalDateTime from TemporalAccessor: " + ((Object) temporalAccessor.getClass()));
        }
        return chronology.localDateTime(temporalAccessor);
    }

    default Chronology getChronology() {
        return toLocalDate().getChronology();
    }

    @Override
    default boolean isSupported(TemporalUnit temporalUnit) {
        return temporalUnit instanceof ChronoUnit ? temporalUnit != ChronoUnit.FOREVER : temporalUnit != null && temporalUnit.isSupportedBy(this);
    }

    @Override
    default ChronoLocalDateTime<D> with(TemporalAdjuster temporalAdjuster) {
        return ChronoLocalDateTimeImpl.ensureValid(getChronology(), super.with(temporalAdjuster));
    }

    @Override
    default ChronoLocalDateTime<D> plus(TemporalAmount temporalAmount) {
        return ChronoLocalDateTimeImpl.ensureValid(getChronology(), super.plus(temporalAmount));
    }

    @Override
    default ChronoLocalDateTime<D> minus(TemporalAmount temporalAmount) {
        return ChronoLocalDateTimeImpl.ensureValid(getChronology(), super.minus(temporalAmount));
    }

    @Override
    default ChronoLocalDateTime<D> minus(long j, TemporalUnit temporalUnit) {
        return ChronoLocalDateTimeImpl.ensureValid(getChronology(), super.minus(j, temporalUnit));
    }

    @Override
    default <R> R query(TemporalQuery<R> temporalQuery) {
        if (temporalQuery == TemporalQueries.zoneId() || temporalQuery == TemporalQueries.zone() || temporalQuery == TemporalQueries.offset()) {
            return null;
        }
        if (temporalQuery == TemporalQueries.localTime()) {
            return (R) toLocalTime();
        }
        if (temporalQuery == TemporalQueries.chronology()) {
            return (R) getChronology();
        }
        if (temporalQuery == TemporalQueries.precision()) {
            return (R) ChronoUnit.NANOS;
        }
        return temporalQuery.queryFrom(this);
    }

    @Override
    default Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.EPOCH_DAY, toLocalDate().toEpochDay()).with(ChronoField.NANO_OF_DAY, toLocalTime().toNanoOfDay());
    }

    default String format(DateTimeFormatter dateTimeFormatter) {
        Objects.requireNonNull(dateTimeFormatter, "formatter");
        return dateTimeFormatter.format(this);
    }

    default Instant toInstant(ZoneOffset zoneOffset) {
        return Instant.ofEpochSecond(toEpochSecond(zoneOffset), toLocalTime().getNano());
    }

    default long toEpochSecond(ZoneOffset zoneOffset) {
        Objects.requireNonNull(zoneOffset, "offset");
        return ((toLocalDate().toEpochDay() * 86400) + ((long) toLocalTime().toSecondOfDay())) - ((long) zoneOffset.getTotalSeconds());
    }

    @Override
    default int compareTo(ChronoLocalDateTime<?> chronoLocalDateTime) {
        int iCompareTo = toLocalDate().compareTo(chronoLocalDateTime.toLocalDate());
        if (iCompareTo == 0) {
            int iCompareTo2 = toLocalTime().compareTo(chronoLocalDateTime.toLocalTime());
            if (iCompareTo2 == 0) {
                return getChronology().compareTo(chronoLocalDateTime.getChronology());
            }
            return iCompareTo2;
        }
        return iCompareTo;
    }

    default boolean isAfter(ChronoLocalDateTime<?> chronoLocalDateTime) {
        long epochDay = toLocalDate().toEpochDay();
        long epochDay2 = chronoLocalDateTime.toLocalDate().toEpochDay();
        return epochDay > epochDay2 || (epochDay == epochDay2 && toLocalTime().toNanoOfDay() > chronoLocalDateTime.toLocalTime().toNanoOfDay());
    }

    default boolean isBefore(ChronoLocalDateTime<?> chronoLocalDateTime) {
        long epochDay = toLocalDate().toEpochDay();
        long epochDay2 = chronoLocalDateTime.toLocalDate().toEpochDay();
        return epochDay < epochDay2 || (epochDay == epochDay2 && toLocalTime().toNanoOfDay() < chronoLocalDateTime.toLocalTime().toNanoOfDay());
    }

    default boolean isEqual(ChronoLocalDateTime<?> chronoLocalDateTime) {
        return toLocalTime().toNanoOfDay() == chronoLocalDateTime.toLocalTime().toNanoOfDay() && toLocalDate().toEpochDay() == chronoLocalDateTime.toLocalDate().toEpochDay();
    }
}
