package java.time.chrono;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.Objects;

public final class ThaiBuddhistDate extends ChronoLocalDateImpl<ThaiBuddhistDate> implements ChronoLocalDate, Serializable {
    private static final long serialVersionUID = -8722293800195731463L;
    private final transient LocalDate isoDate;

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public long until(Temporal temporal, TemporalUnit temporalUnit) {
        return super.until(temporal, temporalUnit);
    }

    public static ThaiBuddhistDate now() {
        return now(Clock.systemDefaultZone());
    }

    public static ThaiBuddhistDate now(ZoneId zoneId) {
        return now(Clock.system(zoneId));
    }

    public static ThaiBuddhistDate now(Clock clock) {
        return new ThaiBuddhistDate(LocalDate.now(clock));
    }

    public static ThaiBuddhistDate of(int i, int i2, int i3) {
        return new ThaiBuddhistDate(LocalDate.of(i - 543, i2, i3));
    }

    public static ThaiBuddhistDate from(TemporalAccessor temporalAccessor) {
        return ThaiBuddhistChronology.INSTANCE.date(temporalAccessor);
    }

    ThaiBuddhistDate(LocalDate localDate) {
        Objects.requireNonNull(localDate, "isoDate");
        this.isoDate = localDate;
    }

    @Override
    public ThaiBuddhistChronology getChronology() {
        return ThaiBuddhistChronology.INSTANCE;
    }

    @Override
    public ThaiBuddhistEra getEra() {
        return getProlepticYear() >= 1 ? ThaiBuddhistEra.BE : ThaiBuddhistEra.BEFORE_BE;
    }

    @Override
    public int lengthOfMonth() {
        return this.isoDate.lengthOfMonth();
    }

    @Override
    public ValueRange range(TemporalField temporalField) {
        if (temporalField instanceof ChronoField) {
            if (isSupported(temporalField)) {
                ChronoField chronoField = (ChronoField) temporalField;
                switch (chronoField) {
                    case DAY_OF_MONTH:
                    case DAY_OF_YEAR:
                    case ALIGNED_WEEK_OF_MONTH:
                        return this.isoDate.range(temporalField);
                    case YEAR_OF_ERA:
                        ValueRange valueRangeRange = ChronoField.YEAR.range();
                        return ValueRange.of(1L, getProlepticYear() <= 0 ? (-(valueRangeRange.getMinimum() + 543)) + 1 : 543 + valueRangeRange.getMaximum());
                    default:
                        return getChronology().range(chronoField);
                }
            }
            throw new UnsupportedTemporalTypeException("Unsupported field: " + ((Object) temporalField));
        }
        return temporalField.rangeRefinedBy(this);
    }

    @Override
    public long getLong(TemporalField temporalField) {
        if (temporalField instanceof ChronoField) {
            switch ((ChronoField) temporalField) {
                case YEAR_OF_ERA:
                    int prolepticYear = getProlepticYear();
                    if (prolepticYear < 1) {
                        prolepticYear = 1 - prolepticYear;
                    }
                    return prolepticYear;
                case PROLEPTIC_MONTH:
                    return getProlepticMonth();
                case YEAR:
                    return getProlepticYear();
                case ERA:
                    return getProlepticYear() < 1 ? 0 : 1;
                default:
                    return this.isoDate.getLong(temporalField);
            }
        }
        return temporalField.getFrom(this);
    }

    private long getProlepticMonth() {
        return ((((long) getProlepticYear()) * 12) + ((long) this.isoDate.getMonthValue())) - 1;
    }

    private int getProlepticYear() {
        return this.isoDate.getYear() + 543;
    }

    @Override
    public ThaiBuddhistDate with(TemporalField temporalField, long j) {
        if (temporalField instanceof ChronoField) {
            ChronoField chronoField = (ChronoField) temporalField;
            if (getLong(chronoField) == j) {
                return this;
            }
            switch (chronoField) {
                case YEAR_OF_ERA:
                case YEAR:
                case ERA:
                    int iCheckValidIntValue = getChronology().range(chronoField).checkValidIntValue(j, chronoField);
                    int i = AnonymousClass1.$SwitchMap$java$time$temporal$ChronoField[chronoField.ordinal()];
                    if (i == 4) {
                        LocalDate localDate = this.isoDate;
                        if (getProlepticYear() < 1) {
                            iCheckValidIntValue = 1 - iCheckValidIntValue;
                        }
                        return with(localDate.withYear(iCheckValidIntValue - 543));
                    }
                    switch (i) {
                        case 6:
                            return with(this.isoDate.withYear(iCheckValidIntValue - 543));
                        case 7:
                            return with(this.isoDate.withYear((1 - getProlepticYear()) - 543));
                    }
                case PROLEPTIC_MONTH:
                    getChronology().range(chronoField).checkValidValue(j, chronoField);
                    return plusMonths(j - getProlepticMonth());
            }
            return with(this.isoDate.with(temporalField, j));
        }
        return (ThaiBuddhistDate) super.with(temporalField, j);
    }

    @Override
    public ThaiBuddhistDate with(TemporalAdjuster temporalAdjuster) {
        return (ThaiBuddhistDate) super.with(temporalAdjuster);
    }

    @Override
    public ThaiBuddhistDate plus(TemporalAmount temporalAmount) {
        return (ThaiBuddhistDate) super.plus(temporalAmount);
    }

    @Override
    public ThaiBuddhistDate minus(TemporalAmount temporalAmount) {
        return (ThaiBuddhistDate) super.minus(temporalAmount);
    }

    @Override
    ThaiBuddhistDate plusYears(long j) {
        return with(this.isoDate.plusYears(j));
    }

    @Override
    ThaiBuddhistDate plusMonths(long j) {
        return with(this.isoDate.plusMonths(j));
    }

    @Override
    ThaiBuddhistDate plusWeeks(long j) {
        return (ThaiBuddhistDate) super.plusWeeks(j);
    }

    @Override
    ThaiBuddhistDate plusDays(long j) {
        return with(this.isoDate.plusDays(j));
    }

    @Override
    public ThaiBuddhistDate plus(long j, TemporalUnit temporalUnit) {
        return (ThaiBuddhistDate) super.plus(j, temporalUnit);
    }

    @Override
    public ThaiBuddhistDate minus(long j, TemporalUnit temporalUnit) {
        return (ThaiBuddhistDate) super.minus(j, temporalUnit);
    }

    @Override
    ThaiBuddhistDate minusYears(long j) {
        return (ThaiBuddhistDate) super.minusYears(j);
    }

    @Override
    ThaiBuddhistDate minusMonths(long j) {
        return (ThaiBuddhistDate) super.minusMonths(j);
    }

    @Override
    ThaiBuddhistDate minusWeeks(long j) {
        return (ThaiBuddhistDate) super.minusWeeks(j);
    }

    @Override
    ThaiBuddhistDate minusDays(long j) {
        return (ThaiBuddhistDate) super.minusDays(j);
    }

    private ThaiBuddhistDate with(LocalDate localDate) {
        return localDate.equals(this.isoDate) ? this : new ThaiBuddhistDate(localDate);
    }

    @Override
    public final ChronoLocalDateTime<ThaiBuddhistDate> atTime(LocalTime localTime) {
        return super.atTime(localTime);
    }

    @Override
    public ChronoPeriod until(ChronoLocalDate chronoLocalDate) {
        Period periodUntil = this.isoDate.until(chronoLocalDate);
        return getChronology().period(periodUntil.getYears(), periodUntil.getMonths(), periodUntil.getDays());
    }

    @Override
    public long toEpochDay() {
        return this.isoDate.toEpochDay();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ThaiBuddhistDate) {
            return this.isoDate.equals(((ThaiBuddhistDate) obj).isoDate);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getChronology().getId().hashCode() ^ this.isoDate.hashCode();
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    private Object writeReplace() {
        return new Ser((byte) 8, this);
    }

    void writeExternal(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(get(ChronoField.YEAR));
        dataOutput.writeByte(get(ChronoField.MONTH_OF_YEAR));
        dataOutput.writeByte(get(ChronoField.DAY_OF_MONTH));
    }

    static ThaiBuddhistDate readExternal(DataInput dataInput) throws IOException {
        return ThaiBuddhistChronology.INSTANCE.date(dataInput.readInt(), (int) dataInput.readByte(), (int) dataInput.readByte());
    }
}
