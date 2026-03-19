package java.time.chrono;

import java.io.Serializable;
import java.time.chrono.ChronoLocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Objects;
import sun.util.locale.LanguageTag;

abstract class ChronoLocalDateImpl<D extends ChronoLocalDate> implements ChronoLocalDate, Temporal, TemporalAdjuster, Serializable {
    private static final long serialVersionUID = 6282433883239719096L;

    abstract D plusDays(long j);

    abstract D plusMonths(long j);

    abstract D plusYears(long j);

    static <D extends ChronoLocalDate> D ensureValid(Chronology chronology, Temporal temporal) {
        D d = (D) temporal;
        if (!chronology.equals(d.getChronology())) {
            throw new ClassCastException("Chronology mismatch, expected: " + chronology.getId() + ", actual: " + d.getChronology().getId());
        }
        return d;
    }

    ChronoLocalDateImpl() {
    }

    @Override
    public D with(TemporalAdjuster temporalAdjuster) {
        return (D) super.with(temporalAdjuster);
    }

    @Override
    public D with(TemporalField temporalField, long j) {
        return (D) super.with(temporalField, j);
    }

    @Override
    public D plus(TemporalAmount temporalAmount) {
        return (D) super.plus(temporalAmount);
    }

    @Override
    public D plus(long j, TemporalUnit temporalUnit) {
        if (temporalUnit instanceof ChronoUnit) {
            switch ((ChronoUnit) temporalUnit) {
                case DAYS:
                    return (D) plusDays(j);
                case WEEKS:
                    return (D) plusDays(Math.multiplyExact(j, 7L));
                case MONTHS:
                    return (D) plusMonths(j);
                case YEARS:
                    return (D) plusYears(j);
                case DECADES:
                    return (D) plusYears(Math.multiplyExact(j, 10L));
                case CENTURIES:
                    return (D) plusYears(Math.multiplyExact(j, 100L));
                case MILLENNIA:
                    return (D) plusYears(Math.multiplyExact(j, 1000L));
                case ERAS:
                    return (D) with((TemporalField) ChronoField.ERA, Math.addExact(getLong(ChronoField.ERA), j));
                default:
                    throw new UnsupportedTemporalTypeException("Unsupported unit: " + ((Object) temporalUnit));
            }
        }
        return (D) super.plus(j, temporalUnit);
    }

    @Override
    public D minus(TemporalAmount temporalAmount) {
        return (D) super.minus(temporalAmount);
    }

    @Override
    public D minus(long j, TemporalUnit temporalUnit) {
        return (D) super.minus(j, temporalUnit);
    }

    D plusWeeks(long j) {
        return (D) plusDays(Math.multiplyExact(j, 7L));
    }

    D minusYears(long j) {
        return j == Long.MIN_VALUE ? (D) ((ChronoLocalDateImpl) plusYears(Long.MAX_VALUE)).plusYears(1L) : (D) plusYears(-j);
    }

    D minusMonths(long j) {
        return j == Long.MIN_VALUE ? (D) ((ChronoLocalDateImpl) plusMonths(Long.MAX_VALUE)).plusMonths(1L) : (D) plusMonths(-j);
    }

    D minusWeeks(long j) {
        return j == Long.MIN_VALUE ? (D) ((ChronoLocalDateImpl) plusWeeks(Long.MAX_VALUE)).plusWeeks(1L) : (D) plusWeeks(-j);
    }

    D minusDays(long j) {
        return j == Long.MIN_VALUE ? (D) ((ChronoLocalDateImpl) plusDays(Long.MAX_VALUE)).plusDays(1L) : (D) plusDays(-j);
    }

    @Override
    public long until(Temporal temporal, TemporalUnit temporalUnit) {
        Objects.requireNonNull(temporal, "endExclusive");
        ChronoLocalDate chronoLocalDateDate = getChronology().date(temporal);
        if (temporalUnit instanceof ChronoUnit) {
            switch ((ChronoUnit) temporalUnit) {
                case DAYS:
                    return daysUntil(chronoLocalDateDate);
                case WEEKS:
                    return daysUntil(chronoLocalDateDate) / 7;
                case MONTHS:
                    return monthsUntil(chronoLocalDateDate);
                case YEARS:
                    return monthsUntil(chronoLocalDateDate) / 12;
                case DECADES:
                    return monthsUntil(chronoLocalDateDate) / 120;
                case CENTURIES:
                    return monthsUntil(chronoLocalDateDate) / 1200;
                case MILLENNIA:
                    return monthsUntil(chronoLocalDateDate) / 12000;
                case ERAS:
                    return chronoLocalDateDate.getLong(ChronoField.ERA) - getLong(ChronoField.ERA);
                default:
                    throw new UnsupportedTemporalTypeException("Unsupported unit: " + ((Object) temporalUnit));
            }
        }
        Objects.requireNonNull(temporalUnit, "unit");
        return temporalUnit.between(this, chronoLocalDateDate);
    }

    private long daysUntil(ChronoLocalDate chronoLocalDate) {
        return chronoLocalDate.toEpochDay() - toEpochDay();
    }

    private long monthsUntil(ChronoLocalDate chronoLocalDate) {
        if (getChronology().range(ChronoField.MONTH_OF_YEAR).getMaximum() != 12) {
            throw new IllegalStateException("ChronoLocalDateImpl only supports Chronologies with 12 months per year");
        }
        return (((chronoLocalDate.getLong(ChronoField.PROLEPTIC_MONTH) * 32) + ((long) chronoLocalDate.get(ChronoField.DAY_OF_MONTH))) - ((getLong(ChronoField.PROLEPTIC_MONTH) * 32) + ((long) get(ChronoField.DAY_OF_MONTH)))) / 32;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return (obj instanceof ChronoLocalDate) && compareTo((ChronoLocalDate) obj) == 0;
    }

    @Override
    public int hashCode() {
        long epochDay = toEpochDay();
        return ((int) (epochDay ^ (epochDay >>> 32))) ^ getChronology().hashCode();
    }

    @Override
    public String toString() {
        long j = getLong(ChronoField.YEAR_OF_ERA);
        long j2 = getLong(ChronoField.MONTH_OF_YEAR);
        long j3 = getLong(ChronoField.DAY_OF_MONTH);
        StringBuilder sb = new StringBuilder(30);
        sb.append(getChronology().toString());
        sb.append(" ");
        sb.append((Object) getEra());
        sb.append(" ");
        sb.append(j);
        sb.append(j2 < 10 ? "-0" : LanguageTag.SEP);
        sb.append(j2);
        sb.append(j3 < 10 ? "-0" : LanguageTag.SEP);
        sb.append(j3);
        return sb.toString();
    }
}
