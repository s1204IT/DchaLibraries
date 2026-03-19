package java.time.chrono;

import java.time.DateTimeException;
import java.time.LocalTime;
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
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Comparator;
import java.util.Objects;

public interface ChronoLocalDate extends Temporal, TemporalAdjuster, Comparable<ChronoLocalDate> {
    boolean equals(Object obj);

    Chronology getChronology();

    int hashCode();

    int lengthOfMonth();

    String toString();

    @Override
    long until(Temporal temporal, TemporalUnit temporalUnit);

    ChronoPeriod until(ChronoLocalDate chronoLocalDate);

    static Comparator<ChronoLocalDate> timeLineOrder() {
        return AbstractChronology.DATE_ORDER;
    }

    static ChronoLocalDate from(TemporalAccessor temporalAccessor) {
        if (temporalAccessor instanceof ChronoLocalDate) {
            return (ChronoLocalDate) temporalAccessor;
        }
        Objects.requireNonNull(temporalAccessor, "temporal");
        Chronology chronology = (Chronology) temporalAccessor.query(TemporalQueries.chronology());
        if (chronology == null) {
            throw new DateTimeException("Unable to obtain ChronoLocalDate from TemporalAccessor: " + ((Object) temporalAccessor.getClass()));
        }
        return chronology.date(temporalAccessor);
    }

    default Era getEra() {
        return getChronology().eraOf(get(ChronoField.ERA));
    }

    default boolean isLeapYear() {
        return getChronology().isLeapYear(getLong(ChronoField.YEAR));
    }

    default int lengthOfYear() {
        return isLeapYear() ? 366 : 365;
    }

    @Override
    default boolean isSupported(TemporalField temporalField) {
        if (temporalField instanceof ChronoField) {
            return temporalField.isDateBased();
        }
        return temporalField != null && temporalField.isSupportedBy(this);
    }

    @Override
    default boolean isSupported(TemporalUnit temporalUnit) {
        if (temporalUnit instanceof ChronoUnit) {
            return temporalUnit.isDateBased();
        }
        return temporalUnit != null && temporalUnit.isSupportedBy(this);
    }

    @Override
    default ChronoLocalDate with(TemporalAdjuster temporalAdjuster) {
        return ChronoLocalDateImpl.ensureValid(getChronology(), super.with(temporalAdjuster));
    }

    @Override
    default ChronoLocalDate with(TemporalField temporalField, long j) {
        if (temporalField instanceof ChronoField) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + ((Object) temporalField));
        }
        return ChronoLocalDateImpl.ensureValid(getChronology(), temporalField.adjustInto(this, j));
    }

    @Override
    default ChronoLocalDate plus(TemporalAmount temporalAmount) {
        return ChronoLocalDateImpl.ensureValid(getChronology(), super.plus(temporalAmount));
    }

    @Override
    default ChronoLocalDate plus(long j, TemporalUnit temporalUnit) {
        if (temporalUnit instanceof ChronoUnit) {
            throw new UnsupportedTemporalTypeException("Unsupported unit: " + ((Object) temporalUnit));
        }
        return ChronoLocalDateImpl.ensureValid(getChronology(), temporalUnit.addTo(this, j));
    }

    @Override
    default ChronoLocalDate minus(TemporalAmount temporalAmount) {
        return ChronoLocalDateImpl.ensureValid(getChronology(), super.minus(temporalAmount));
    }

    @Override
    default ChronoLocalDate minus(long j, TemporalUnit temporalUnit) {
        return ChronoLocalDateImpl.ensureValid(getChronology(), super.minus(j, temporalUnit));
    }

    @Override
    default <R> R query(TemporalQuery<R> temporalQuery) {
        if (temporalQuery == TemporalQueries.zoneId() || temporalQuery == TemporalQueries.zone() || temporalQuery == TemporalQueries.offset() || temporalQuery == TemporalQueries.localTime()) {
            return null;
        }
        if (temporalQuery == TemporalQueries.chronology()) {
            return (R) getChronology();
        }
        if (temporalQuery == TemporalQueries.precision()) {
            return (R) ChronoUnit.DAYS;
        }
        return temporalQuery.queryFrom(this);
    }

    @Override
    default Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.EPOCH_DAY, toEpochDay());
    }

    default String format(DateTimeFormatter dateTimeFormatter) {
        Objects.requireNonNull(dateTimeFormatter, "formatter");
        return dateTimeFormatter.format(this);
    }

    default ChronoLocalDateTime<?> atTime(LocalTime localTime) {
        return ChronoLocalDateTimeImpl.of(this, localTime);
    }

    default long toEpochDay() {
        return getLong(ChronoField.EPOCH_DAY);
    }

    @Override
    default int compareTo(ChronoLocalDate chronoLocalDate) {
        int iCompare = Long.compare(toEpochDay(), chronoLocalDate.toEpochDay());
        if (iCompare == 0) {
            return getChronology().compareTo(chronoLocalDate.getChronology());
        }
        return iCompare;
    }

    default boolean isAfter(ChronoLocalDate chronoLocalDate) {
        return toEpochDay() > chronoLocalDate.toEpochDay();
    }

    default boolean isBefore(ChronoLocalDate chronoLocalDate) {
        return toEpochDay() < chronoLocalDate.toEpochDay();
    }

    default boolean isEqual(ChronoLocalDate chronoLocalDate) {
        return toEpochDay() == chronoLocalDate.toEpochDay();
    }
}
