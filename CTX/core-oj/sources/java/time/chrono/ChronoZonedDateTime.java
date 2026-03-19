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
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.Comparator;
import java.util.Objects;

public interface ChronoZonedDateTime<D extends ChronoLocalDate> extends Temporal, Comparable<ChronoZonedDateTime<?>> {
    boolean equals(Object obj);

    ZoneOffset getOffset();

    ZoneId getZone();

    int hashCode();

    @Override
    boolean isSupported(TemporalField temporalField);

    @Override
    ChronoZonedDateTime<D> plus(long j, TemporalUnit temporalUnit);

    ChronoLocalDateTime<D> toLocalDateTime();

    String toString();

    @Override
    ChronoZonedDateTime<D> with(TemporalField temporalField, long j);

    ChronoZonedDateTime<D> withEarlierOffsetAtOverlap();

    ChronoZonedDateTime<D> withLaterOffsetAtOverlap();

    ChronoZonedDateTime<D> withZoneSameInstant(ZoneId zoneId);

    ChronoZonedDateTime<D> withZoneSameLocal(ZoneId zoneId);

    static Comparator<ChronoZonedDateTime<?>> timeLineOrder() {
        return AbstractChronology.INSTANT_ORDER;
    }

    static ChronoZonedDateTime<?> from(TemporalAccessor temporalAccessor) {
        if (temporalAccessor instanceof ChronoZonedDateTime) {
            return (ChronoZonedDateTime) temporalAccessor;
        }
        Objects.requireNonNull(temporalAccessor, "temporal");
        Chronology chronology = (Chronology) temporalAccessor.query(TemporalQueries.chronology());
        if (chronology == null) {
            throw new DateTimeException("Unable to obtain ChronoZonedDateTime from TemporalAccessor: " + ((Object) temporalAccessor.getClass()));
        }
        return chronology.zonedDateTime(temporalAccessor);
    }

    @Override
    default ValueRange range(TemporalField temporalField) {
        if (temporalField instanceof ChronoField) {
            if (temporalField == ChronoField.INSTANT_SECONDS || temporalField == ChronoField.OFFSET_SECONDS) {
                return temporalField.range();
            }
            return toLocalDateTime().range(temporalField);
        }
        return temporalField.rangeRefinedBy(this);
    }

    @Override
    default int get(TemporalField temporalField) {
        if (temporalField instanceof ChronoField) {
            switch ((ChronoField) temporalField) {
                case INSTANT_SECONDS:
                    throw new UnsupportedTemporalTypeException("Invalid field 'InstantSeconds' for get() method, use getLong() instead");
                case OFFSET_SECONDS:
                    return getOffset().getTotalSeconds();
                default:
                    return toLocalDateTime().get(temporalField);
            }
        }
        return super.get(temporalField);
    }

    @Override
    default long getLong(TemporalField temporalField) {
        if (temporalField instanceof ChronoField) {
            switch ((ChronoField) temporalField) {
                case INSTANT_SECONDS:
                    return toEpochSecond();
                case OFFSET_SECONDS:
                    return getOffset().getTotalSeconds();
                default:
                    return toLocalDateTime().getLong(temporalField);
            }
        }
        return temporalField.getFrom(this);
    }

    default D toLocalDate() {
        return (D) toLocalDateTime().toLocalDate();
    }

    default LocalTime toLocalTime() {
        return toLocalDateTime().toLocalTime();
    }

    default Chronology getChronology() {
        return toLocalDate().getChronology();
    }

    @Override
    default boolean isSupported(TemporalUnit temporalUnit) {
        return temporalUnit instanceof ChronoUnit ? temporalUnit != ChronoUnit.FOREVER : temporalUnit != null && temporalUnit.isSupportedBy(this);
    }

    @Override
    default ChronoZonedDateTime<D> with(TemporalAdjuster temporalAdjuster) {
        return ChronoZonedDateTimeImpl.ensureValid(getChronology(), super.with(temporalAdjuster));
    }

    @Override
    default ChronoZonedDateTime<D> plus(TemporalAmount temporalAmount) {
        return ChronoZonedDateTimeImpl.ensureValid(getChronology(), super.plus(temporalAmount));
    }

    @Override
    default ChronoZonedDateTime<D> minus(TemporalAmount temporalAmount) {
        return ChronoZonedDateTimeImpl.ensureValid(getChronology(), super.minus(temporalAmount));
    }

    @Override
    default ChronoZonedDateTime<D> minus(long j, TemporalUnit temporalUnit) {
        return ChronoZonedDateTimeImpl.ensureValid(getChronology(), super.minus(j, temporalUnit));
    }

    @Override
    default <R> R query(TemporalQuery<R> temporalQuery) {
        if (temporalQuery == TemporalQueries.zone() || temporalQuery == TemporalQueries.zoneId()) {
            return (R) getZone();
        }
        if (temporalQuery == TemporalQueries.offset()) {
            return (R) getOffset();
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

    default String format(DateTimeFormatter dateTimeFormatter) {
        Objects.requireNonNull(dateTimeFormatter, "formatter");
        return dateTimeFormatter.format(this);
    }

    default Instant toInstant() {
        return Instant.ofEpochSecond(toEpochSecond(), toLocalTime().getNano());
    }

    default long toEpochSecond() {
        return ((toLocalDate().toEpochDay() * 86400) + ((long) toLocalTime().toSecondOfDay())) - ((long) getOffset().getTotalSeconds());
    }

    @Override
    default int compareTo(ChronoZonedDateTime<?> chronoZonedDateTime) {
        int iCompare = Long.compare(toEpochSecond(), chronoZonedDateTime.toEpochSecond());
        if (iCompare == 0) {
            int nano = toLocalTime().getNano() - chronoZonedDateTime.toLocalTime().getNano();
            if (nano == 0) {
                int iCompareTo = toLocalDateTime().compareTo(chronoZonedDateTime.toLocalDateTime());
                if (iCompareTo == 0) {
                    int iCompareTo2 = getZone().getId().compareTo(chronoZonedDateTime.getZone().getId());
                    if (iCompareTo2 == 0) {
                        return getChronology().compareTo(chronoZonedDateTime.getChronology());
                    }
                    return iCompareTo2;
                }
                return iCompareTo;
            }
            return nano;
        }
        return iCompare;
    }

    default boolean isBefore(ChronoZonedDateTime<?> chronoZonedDateTime) {
        long epochSecond = toEpochSecond();
        long epochSecond2 = chronoZonedDateTime.toEpochSecond();
        return epochSecond < epochSecond2 || (epochSecond == epochSecond2 && toLocalTime().getNano() < chronoZonedDateTime.toLocalTime().getNano());
    }

    default boolean isAfter(ChronoZonedDateTime<?> chronoZonedDateTime) {
        long epochSecond = toEpochSecond();
        long epochSecond2 = chronoZonedDateTime.toEpochSecond();
        return epochSecond > epochSecond2 || (epochSecond == epochSecond2 && toLocalTime().getNano() > chronoZonedDateTime.toLocalTime().getNano());
    }

    default boolean isEqual(ChronoZonedDateTime<?> chronoZonedDateTime) {
        return toEpochSecond() == chronoZonedDateTime.toEpochSecond() && toLocalTime().getNano() == chronoZonedDateTime.toLocalTime().getNano();
    }
}
