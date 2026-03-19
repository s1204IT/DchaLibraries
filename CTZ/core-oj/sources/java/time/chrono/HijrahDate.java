package java.time.chrono;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
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

public final class HijrahDate extends ChronoLocalDateImpl<HijrahDate> implements ChronoLocalDate, Serializable {
    private static final long serialVersionUID = -5207853542612002020L;
    private final transient HijrahChronology chrono;
    private final transient int dayOfMonth;
    private final transient int monthOfYear;
    private final transient int prolepticYear;

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public long until(Temporal temporal, TemporalUnit temporalUnit) {
        return super.until(temporal, temporalUnit);
    }

    static HijrahDate of(HijrahChronology hijrahChronology, int i, int i2, int i3) {
        return new HijrahDate(hijrahChronology, i, i2, i3);
    }

    static HijrahDate ofEpochDay(HijrahChronology hijrahChronology, long j) {
        return new HijrahDate(hijrahChronology, j);
    }

    public static HijrahDate now() {
        return now(Clock.systemDefaultZone());
    }

    public static HijrahDate now(ZoneId zoneId) {
        return now(Clock.system(zoneId));
    }

    public static HijrahDate now(Clock clock) {
        return ofEpochDay(HijrahChronology.INSTANCE, LocalDate.now(clock).toEpochDay());
    }

    public static HijrahDate of(int i, int i2, int i3) {
        return HijrahChronology.INSTANCE.date(i, i2, i3);
    }

    public static HijrahDate from(TemporalAccessor temporalAccessor) {
        return HijrahChronology.INSTANCE.date(temporalAccessor);
    }

    private HijrahDate(HijrahChronology hijrahChronology, int i, int i2, int i3) {
        hijrahChronology.getEpochDay(i, i2, i3);
        this.chrono = hijrahChronology;
        this.prolepticYear = i;
        this.monthOfYear = i2;
        this.dayOfMonth = i3;
    }

    private HijrahDate(HijrahChronology hijrahChronology, long j) {
        int[] hijrahDateInfo = hijrahChronology.getHijrahDateInfo((int) j);
        this.chrono = hijrahChronology;
        this.prolepticYear = hijrahDateInfo[0];
        this.monthOfYear = hijrahDateInfo[1];
        this.dayOfMonth = hijrahDateInfo[2];
    }

    @Override
    public HijrahChronology getChronology() {
        return this.chrono;
    }

    @Override
    public HijrahEra getEra() {
        return HijrahEra.AH;
    }

    @Override
    public int lengthOfMonth() {
        return this.chrono.getMonthLength(this.prolepticYear, this.monthOfYear);
    }

    @Override
    public int lengthOfYear() {
        return this.chrono.getYearLength(this.prolepticYear);
    }

    @Override
    public ValueRange range(TemporalField temporalField) {
        if (temporalField instanceof ChronoField) {
            if (isSupported(temporalField)) {
                ChronoField chronoField = (ChronoField) temporalField;
                switch (chronoField) {
                    case DAY_OF_MONTH:
                        return ValueRange.of(1L, lengthOfMonth());
                    case DAY_OF_YEAR:
                        return ValueRange.of(1L, lengthOfYear());
                    case ALIGNED_WEEK_OF_MONTH:
                        return ValueRange.of(1L, 5L);
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
                case DAY_OF_MONTH:
                    return this.dayOfMonth;
                case DAY_OF_YEAR:
                    return getDayOfYear();
                case ALIGNED_WEEK_OF_MONTH:
                    return ((this.dayOfMonth - 1) / 7) + 1;
                case DAY_OF_WEEK:
                    return getDayOfWeek();
                case ALIGNED_DAY_OF_WEEK_IN_MONTH:
                    return ((getDayOfWeek() - 1) % 7) + 1;
                case ALIGNED_DAY_OF_WEEK_IN_YEAR:
                    return ((getDayOfYear() - 1) % 7) + 1;
                case EPOCH_DAY:
                    return toEpochDay();
                case ALIGNED_WEEK_OF_YEAR:
                    return ((getDayOfYear() - 1) / 7) + 1;
                case MONTH_OF_YEAR:
                    return this.monthOfYear;
                case PROLEPTIC_MONTH:
                    return getProlepticMonth();
                case YEAR_OF_ERA:
                    return this.prolepticYear;
                case YEAR:
                    return this.prolepticYear;
                case ERA:
                    return getEraValue();
                default:
                    throw new UnsupportedTemporalTypeException("Unsupported field: " + ((Object) temporalField));
            }
        }
        return temporalField.getFrom(this);
    }

    private long getProlepticMonth() {
        return ((((long) this.prolepticYear) * 12) + ((long) this.monthOfYear)) - 1;
    }

    @Override
    public HijrahDate with(TemporalField temporalField, long j) {
        if (temporalField instanceof ChronoField) {
            ChronoField chronoField = (ChronoField) temporalField;
            this.chrono.range(chronoField).checkValidValue(j, chronoField);
            int i = (int) j;
            switch (chronoField) {
                case DAY_OF_MONTH:
                    return resolvePreviousValid(this.prolepticYear, this.monthOfYear, i);
                case DAY_OF_YEAR:
                    return plusDays(Math.min(i, lengthOfYear()) - getDayOfYear());
                case ALIGNED_WEEK_OF_MONTH:
                    return plusDays((j - getLong(ChronoField.ALIGNED_WEEK_OF_MONTH)) * 7);
                case DAY_OF_WEEK:
                    return plusDays(j - ((long) getDayOfWeek()));
                case ALIGNED_DAY_OF_WEEK_IN_MONTH:
                    return plusDays(j - getLong(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH));
                case ALIGNED_DAY_OF_WEEK_IN_YEAR:
                    return plusDays(j - getLong(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR));
                case EPOCH_DAY:
                    return new HijrahDate(this.chrono, j);
                case ALIGNED_WEEK_OF_YEAR:
                    return plusDays((j - getLong(ChronoField.ALIGNED_WEEK_OF_YEAR)) * 7);
                case MONTH_OF_YEAR:
                    return resolvePreviousValid(this.prolepticYear, i, this.dayOfMonth);
                case PROLEPTIC_MONTH:
                    return plusMonths(j - getProlepticMonth());
                case YEAR_OF_ERA:
                    if (this.prolepticYear < 1) {
                        i = 1 - i;
                    }
                    return resolvePreviousValid(i, this.monthOfYear, this.dayOfMonth);
                case YEAR:
                    return resolvePreviousValid(i, this.monthOfYear, this.dayOfMonth);
                case ERA:
                    return resolvePreviousValid(1 - this.prolepticYear, this.monthOfYear, this.dayOfMonth);
                default:
                    throw new UnsupportedTemporalTypeException("Unsupported field: " + ((Object) temporalField));
            }
        }
        return (HijrahDate) super.with(temporalField, j);
    }

    private HijrahDate resolvePreviousValid(int i, int i2, int i3) {
        int monthLength = this.chrono.getMonthLength(i, i2);
        if (i3 > monthLength) {
            i3 = monthLength;
        }
        return of(this.chrono, i, i2, i3);
    }

    @Override
    public HijrahDate with(TemporalAdjuster temporalAdjuster) {
        return (HijrahDate) super.with(temporalAdjuster);
    }

    public HijrahDate withVariant(HijrahChronology hijrahChronology) {
        if (this.chrono == hijrahChronology) {
            return this;
        }
        int dayOfYear = hijrahChronology.getDayOfYear(this.prolepticYear, this.monthOfYear);
        int i = this.prolepticYear;
        int i2 = this.monthOfYear;
        if (this.dayOfMonth <= dayOfYear) {
            dayOfYear = this.dayOfMonth;
        }
        return of(hijrahChronology, i, i2, dayOfYear);
    }

    @Override
    public HijrahDate plus(TemporalAmount temporalAmount) {
        return (HijrahDate) super.plus(temporalAmount);
    }

    @Override
    public HijrahDate minus(TemporalAmount temporalAmount) {
        return (HijrahDate) super.minus(temporalAmount);
    }

    @Override
    public long toEpochDay() {
        return this.chrono.getEpochDay(this.prolepticYear, this.monthOfYear, this.dayOfMonth);
    }

    private int getDayOfYear() {
        return this.chrono.getDayOfYear(this.prolepticYear, this.monthOfYear) + this.dayOfMonth;
    }

    private int getDayOfWeek() {
        return ((int) Math.floorMod(toEpochDay() + 3, 7L)) + 1;
    }

    private int getEraValue() {
        return this.prolepticYear > 1 ? 1 : 0;
    }

    @Override
    public boolean isLeapYear() {
        return this.chrono.isLeapYear(this.prolepticYear);
    }

    @Override
    HijrahDate plusYears(long j) {
        if (j == 0) {
            return this;
        }
        return resolvePreviousValid(Math.addExact(this.prolepticYear, (int) j), this.monthOfYear, this.dayOfMonth);
    }

    @Override
    HijrahDate plusMonths(long j) {
        if (j == 0) {
            return this;
        }
        long j2 = (((long) this.prolepticYear) * 12) + ((long) (this.monthOfYear - 1)) + j;
        return resolvePreviousValid(this.chrono.checkValidYear(Math.floorDiv(j2, 12L)), ((int) Math.floorMod(j2, 12L)) + 1, this.dayOfMonth);
    }

    @Override
    HijrahDate plusWeeks(long j) {
        return (HijrahDate) super.plusWeeks(j);
    }

    @Override
    HijrahDate plusDays(long j) {
        return new HijrahDate(this.chrono, toEpochDay() + j);
    }

    @Override
    public HijrahDate plus(long j, TemporalUnit temporalUnit) {
        return (HijrahDate) super.plus(j, temporalUnit);
    }

    @Override
    public HijrahDate minus(long j, TemporalUnit temporalUnit) {
        return (HijrahDate) super.minus(j, temporalUnit);
    }

    @Override
    HijrahDate minusYears(long j) {
        return (HijrahDate) super.minusYears(j);
    }

    @Override
    HijrahDate minusMonths(long j) {
        return (HijrahDate) super.minusMonths(j);
    }

    @Override
    HijrahDate minusWeeks(long j) {
        return (HijrahDate) super.minusWeeks(j);
    }

    @Override
    HijrahDate minusDays(long j) {
        return (HijrahDate) super.minusDays(j);
    }

    @Override
    public final ChronoLocalDateTime<HijrahDate> atTime(LocalTime localTime) {
        return super.atTime(localTime);
    }

    @Override
    public ChronoPeriod until(ChronoLocalDate chronoLocalDate) {
        HijrahDate hijrahDateDate = getChronology().date((TemporalAccessor) chronoLocalDate);
        long j = ((hijrahDateDate.prolepticYear - this.prolepticYear) * 12) + (hijrahDateDate.monthOfYear - this.monthOfYear);
        int iLengthOfMonth = hijrahDateDate.dayOfMonth - this.dayOfMonth;
        if (j > 0 && iLengthOfMonth < 0) {
            j--;
            iLengthOfMonth = (int) (hijrahDateDate.toEpochDay() - plusMonths(j).toEpochDay());
        } else if (j < 0 && iLengthOfMonth > 0) {
            j++;
            iLengthOfMonth -= hijrahDateDate.lengthOfMonth();
        }
        return getChronology().period(Math.toIntExact(j / 12), (int) (j % 12), iLengthOfMonth);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HijrahDate)) {
            return false;
        }
        HijrahDate hijrahDate = (HijrahDate) obj;
        return this.prolepticYear == hijrahDate.prolepticYear && this.monthOfYear == hijrahDate.monthOfYear && this.dayOfMonth == hijrahDate.dayOfMonth && getChronology().equals(hijrahDate.getChronology());
    }

    @Override
    public int hashCode() {
        int i = this.prolepticYear;
        int i2 = this.monthOfYear;
        int i3 = this.dayOfMonth;
        return (((i << 11) + (i2 << 6)) + i3) ^ (getChronology().getId().hashCode() ^ (i & (-2048)));
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    private Object writeReplace() {
        return new Ser((byte) 6, this);
    }

    void writeExternal(ObjectOutput objectOutput) throws IOException {
        objectOutput.writeObject(getChronology());
        objectOutput.writeInt(get(ChronoField.YEAR));
        objectOutput.writeByte(get(ChronoField.MONTH_OF_YEAR));
        objectOutput.writeByte(get(ChronoField.DAY_OF_MONTH));
    }

    static HijrahDate readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
        return ((HijrahChronology) objectInput.readObject()).date(objectInput.readInt(), (int) objectInput.readByte(), (int) objectInput.readByte());
    }
}
