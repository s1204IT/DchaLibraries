package java.time;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Era;
import java.time.chrono.IsoChronology;
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
import java.time.zone.ZoneOffsetTransition;
import java.util.Objects;
import sun.util.locale.LanguageTag;

public final class LocalDate implements Temporal, TemporalAdjuster, ChronoLocalDate, Serializable {
    static final long DAYS_0000_TO_1970 = 719528;
    private static final int DAYS_PER_CYCLE = 146097;
    private static final long serialVersionUID = 2942565459149668126L;
    private final short day;
    private final short month;
    private final int year;
    public static final LocalDate MIN = of(Year.MIN_VALUE, 1, 1);
    public static final LocalDate MAX = of(Year.MAX_VALUE, 12, 31);

    public static LocalDate now() {
        return now(Clock.systemDefaultZone());
    }

    public static LocalDate now(ZoneId zoneId) {
        return now(Clock.system(zoneId));
    }

    public static LocalDate now(Clock clock) {
        Objects.requireNonNull(clock, "clock");
        Instant instant = clock.instant();
        return ofEpochDay(Math.floorDiv(instant.getEpochSecond() + ((long) clock.getZone().getRules().getOffset(instant).getTotalSeconds()), 86400L));
    }

    public static LocalDate of(int i, Month month, int i2) {
        ChronoField.YEAR.checkValidValue(i);
        Objects.requireNonNull(month, "month");
        ChronoField.DAY_OF_MONTH.checkValidValue(i2);
        return create(i, month.getValue(), i2);
    }

    public static LocalDate of(int i, int i2, int i3) {
        ChronoField.YEAR.checkValidValue(i);
        ChronoField.MONTH_OF_YEAR.checkValidValue(i2);
        ChronoField.DAY_OF_MONTH.checkValidValue(i3);
        return create(i, i2, i3);
    }

    public static LocalDate ofYearDay(int i, int i2) {
        long j = i;
        ChronoField.YEAR.checkValidValue(j);
        ChronoField.DAY_OF_YEAR.checkValidValue(i2);
        boolean zIsLeapYear = IsoChronology.INSTANCE.isLeapYear(j);
        if (i2 == 366 && !zIsLeapYear) {
            throw new DateTimeException("Invalid date 'DayOfYear 366' as '" + i + "' is not a leap year");
        }
        Month monthOf = Month.of(((i2 - 1) / 31) + 1);
        if (i2 > (monthOf.firstDayOfYear(zIsLeapYear) + monthOf.length(zIsLeapYear)) - 1) {
            monthOf = monthOf.plus(1L);
        }
        return new LocalDate(i, monthOf.getValue(), (i2 - monthOf.firstDayOfYear(zIsLeapYear)) + 1);
    }

    public static LocalDate ofEpochDay(long j) {
        long j2;
        long j3 = (j + DAYS_0000_TO_1970) - 60;
        if (j3 < 0) {
            long j4 = ((j3 + 1) / 146097) - 1;
            j2 = j4 * 400;
            j3 += (-j4) * 146097;
        } else {
            j2 = 0;
        }
        long j5 = ((400 * j3) + 591) / 146097;
        long j6 = j3 - ((((365 * j5) + (j5 / 4)) - (j5 / 100)) + (j5 / 400));
        if (j6 < 0) {
            j5--;
            j6 = j3 - ((((365 * j5) + (j5 / 4)) - (j5 / 100)) + (j5 / 400));
        }
        int i = (int) j6;
        int i2 = ((i * 5) + 2) / 153;
        return new LocalDate(ChronoField.YEAR.checkValidIntValue(j5 + j2 + ((long) (i2 / 10))), ((i2 + 2) % 12) + 1, (i - (((i2 * 306) + 5) / 10)) + 1);
    }

    public static LocalDate from(TemporalAccessor temporalAccessor) {
        Objects.requireNonNull(temporalAccessor, "temporal");
        LocalDate localDate = (LocalDate) temporalAccessor.query(TemporalQueries.localDate());
        if (localDate == null) {
            throw new DateTimeException("Unable to obtain LocalDate from TemporalAccessor: " + ((Object) temporalAccessor) + " of type " + temporalAccessor.getClass().getName());
        }
        return localDate;
    }

    public static LocalDate parse(CharSequence charSequence) {
        return parse(charSequence, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static LocalDate parse(CharSequence charSequence, DateTimeFormatter dateTimeFormatter) {
        Objects.requireNonNull(dateTimeFormatter, "formatter");
        return (LocalDate) dateTimeFormatter.parse(charSequence, new TemporalQuery() {
            @Override
            public final Object queryFrom(TemporalAccessor temporalAccessor) {
                return LocalDate.from(temporalAccessor);
            }
        });
    }

    private static LocalDate create(int i, int i2, int i3) {
        if (i3 > 28) {
            int i4 = 31;
            if (i2 == 2) {
                i4 = IsoChronology.INSTANCE.isLeapYear((long) i) ? 29 : 28;
            } else if (i2 == 4 || i2 == 6 || i2 == 9 || i2 == 11) {
                i4 = 30;
            }
            if (i3 > i4) {
                if (i3 == 29) {
                    throw new DateTimeException("Invalid date 'February 29' as '" + i + "' is not a leap year");
                }
                throw new DateTimeException("Invalid date '" + Month.of(i2).name() + " " + i3 + "'");
            }
        }
        return new LocalDate(i, i2, i3);
    }

    private static LocalDate resolvePreviousValid(int i, int i2, int i3) {
        if (i2 == 2) {
            i3 = Math.min(i3, IsoChronology.INSTANCE.isLeapYear((long) i) ? 29 : 28);
        } else if (i2 == 4 || i2 == 6 || i2 == 9 || i2 == 11) {
            i3 = Math.min(i3, 30);
        }
        return new LocalDate(i, i2, i3);
    }

    private LocalDate(int i, int i2, int i3) {
        this.year = i;
        this.month = (short) i2;
        this.day = (short) i3;
    }

    @Override
    public boolean isSupported(TemporalField temporalField) {
        return super.isSupported(temporalField);
    }

    @Override
    public boolean isSupported(TemporalUnit temporalUnit) {
        return super.isSupported(temporalUnit);
    }

    @Override
    public ValueRange range(TemporalField temporalField) {
        if (temporalField instanceof ChronoField) {
            ChronoField chronoField = (ChronoField) temporalField;
            if (chronoField.isDateBased()) {
                switch (chronoField) {
                    case DAY_OF_MONTH:
                        return ValueRange.of(1L, lengthOfMonth());
                    case DAY_OF_YEAR:
                        return ValueRange.of(1L, lengthOfYear());
                    case ALIGNED_WEEK_OF_MONTH:
                        return ValueRange.of(1L, (getMonth() != Month.FEBRUARY || isLeapYear()) ? 5L : 4L);
                    case YEAR_OF_ERA:
                        return ValueRange.of(1L, getYear() <= 0 ? 1000000000L : 999999999L);
                    default:
                        return temporalField.range();
                }
            }
            throw new UnsupportedTemporalTypeException("Unsupported field: " + ((Object) temporalField));
        }
        return temporalField.rangeRefinedBy(this);
    }

    @Override
    public int get(TemporalField temporalField) {
        if (temporalField instanceof ChronoField) {
            return get0(temporalField);
        }
        return super.get(temporalField);
    }

    @Override
    public long getLong(TemporalField temporalField) {
        if (temporalField instanceof ChronoField) {
            if (temporalField == ChronoField.EPOCH_DAY) {
                return toEpochDay();
            }
            if (temporalField == ChronoField.PROLEPTIC_MONTH) {
                return getProlepticMonth();
            }
            return get0(temporalField);
        }
        return temporalField.getFrom(this);
    }

    private int get0(TemporalField temporalField) {
        switch ((ChronoField) temporalField) {
            case DAY_OF_MONTH:
                return this.day;
            case DAY_OF_YEAR:
                return getDayOfYear();
            case ALIGNED_WEEK_OF_MONTH:
                return ((this.day - 1) / 7) + 1;
            case YEAR_OF_ERA:
                return this.year >= 1 ? this.year : 1 - this.year;
            case DAY_OF_WEEK:
                return getDayOfWeek().getValue();
            case ALIGNED_DAY_OF_WEEK_IN_MONTH:
                return ((this.day - 1) % 7) + 1;
            case ALIGNED_DAY_OF_WEEK_IN_YEAR:
                return ((getDayOfYear() - 1) % 7) + 1;
            case EPOCH_DAY:
                throw new UnsupportedTemporalTypeException("Invalid field 'EpochDay' for get() method, use getLong() instead");
            case ALIGNED_WEEK_OF_YEAR:
                return ((getDayOfYear() - 1) / 7) + 1;
            case MONTH_OF_YEAR:
                return this.month;
            case PROLEPTIC_MONTH:
                throw new UnsupportedTemporalTypeException("Invalid field 'ProlepticMonth' for get() method, use getLong() instead");
            case YEAR:
                return this.year;
            case ERA:
                return this.year >= 1 ? 1 : 0;
            default:
                throw new UnsupportedTemporalTypeException("Unsupported field: " + ((Object) temporalField));
        }
    }

    private long getProlepticMonth() {
        return ((((long) this.year) * 12) + ((long) this.month)) - 1;
    }

    @Override
    public IsoChronology getChronology() {
        return IsoChronology.INSTANCE;
    }

    @Override
    public Era getEra() {
        return super.getEra();
    }

    public int getYear() {
        return this.year;
    }

    public int getMonthValue() {
        return this.month;
    }

    public Month getMonth() {
        return Month.of(this.month);
    }

    public int getDayOfMonth() {
        return this.day;
    }

    public int getDayOfYear() {
        return (getMonth().firstDayOfYear(isLeapYear()) + this.day) - 1;
    }

    public DayOfWeek getDayOfWeek() {
        return DayOfWeek.of(((int) Math.floorMod(toEpochDay() + 3, 7L)) + 1);
    }

    @Override
    public boolean isLeapYear() {
        return IsoChronology.INSTANCE.isLeapYear(this.year);
    }

    @Override
    public int lengthOfMonth() {
        short s = this.month;
        if (s == 2) {
            return isLeapYear() ? 29 : 28;
        }
        if (s == 4 || s == 6 || s == 9 || s == 11) {
            return 30;
        }
        return 31;
    }

    @Override
    public int lengthOfYear() {
        return isLeapYear() ? 366 : 365;
    }

    @Override
    public LocalDate with(TemporalAdjuster temporalAdjuster) {
        if (temporalAdjuster instanceof LocalDate) {
            return (LocalDate) temporalAdjuster;
        }
        return (LocalDate) temporalAdjuster.adjustInto(this);
    }

    @Override
    public LocalDate with(TemporalField temporalField, long j) {
        if (temporalField instanceof ChronoField) {
            ChronoField chronoField = (ChronoField) temporalField;
            chronoField.checkValidValue(j);
            switch (chronoField) {
                case DAY_OF_MONTH:
                    return withDayOfMonth((int) j);
                case DAY_OF_YEAR:
                    return withDayOfYear((int) j);
                case ALIGNED_WEEK_OF_MONTH:
                    return plusWeeks(j - getLong(ChronoField.ALIGNED_WEEK_OF_MONTH));
                case YEAR_OF_ERA:
                    if (this.year < 1) {
                        j = 1 - j;
                    }
                    return withYear((int) j);
                case DAY_OF_WEEK:
                    return plusDays(j - ((long) getDayOfWeek().getValue()));
                case ALIGNED_DAY_OF_WEEK_IN_MONTH:
                    return plusDays(j - getLong(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH));
                case ALIGNED_DAY_OF_WEEK_IN_YEAR:
                    return plusDays(j - getLong(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR));
                case EPOCH_DAY:
                    return ofEpochDay(j);
                case ALIGNED_WEEK_OF_YEAR:
                    return plusWeeks(j - getLong(ChronoField.ALIGNED_WEEK_OF_YEAR));
                case MONTH_OF_YEAR:
                    return withMonth((int) j);
                case PROLEPTIC_MONTH:
                    return plusMonths(j - getProlepticMonth());
                case YEAR:
                    return withYear((int) j);
                case ERA:
                    return getLong(ChronoField.ERA) == j ? this : withYear(1 - this.year);
                default:
                    throw new UnsupportedTemporalTypeException("Unsupported field: " + ((Object) temporalField));
            }
        }
        return (LocalDate) temporalField.adjustInto(this, j);
    }

    public LocalDate withYear(int i) {
        if (this.year == i) {
            return this;
        }
        ChronoField.YEAR.checkValidValue(i);
        return resolvePreviousValid(i, this.month, this.day);
    }

    public LocalDate withMonth(int i) {
        if (this.month == i) {
            return this;
        }
        ChronoField.MONTH_OF_YEAR.checkValidValue(i);
        return resolvePreviousValid(this.year, i, this.day);
    }

    public LocalDate withDayOfMonth(int i) {
        if (this.day == i) {
            return this;
        }
        return of(this.year, this.month, i);
    }

    public LocalDate withDayOfYear(int i) {
        if (getDayOfYear() == i) {
            return this;
        }
        return ofYearDay(this.year, i);
    }

    @Override
    public LocalDate plus(TemporalAmount temporalAmount) {
        if (temporalAmount instanceof Period) {
            return plusMonths(((Period) temporalAmount).toTotalMonths()).plusDays(r4.getDays());
        }
        Objects.requireNonNull(temporalAmount, "amountToAdd");
        return (LocalDate) temporalAmount.addTo(this);
    }

    @Override
    public LocalDate plus(long j, TemporalUnit temporalUnit) {
        if (temporalUnit instanceof ChronoUnit) {
            switch ((ChronoUnit) temporalUnit) {
                case DAYS:
                    return plusDays(j);
                case WEEKS:
                    return plusWeeks(j);
                case MONTHS:
                    return plusMonths(j);
                case YEARS:
                    return plusYears(j);
                case DECADES:
                    return plusYears(Math.multiplyExact(j, 10L));
                case CENTURIES:
                    return plusYears(Math.multiplyExact(j, 100L));
                case MILLENNIA:
                    return plusYears(Math.multiplyExact(j, 1000L));
                case ERAS:
                    return with((TemporalField) ChronoField.ERA, Math.addExact(getLong(ChronoField.ERA), j));
                default:
                    throw new UnsupportedTemporalTypeException("Unsupported unit: " + ((Object) temporalUnit));
            }
        }
        return (LocalDate) temporalUnit.addTo(this, j);
    }

    public LocalDate plusYears(long j) {
        if (j == 0) {
            return this;
        }
        return resolvePreviousValid(ChronoField.YEAR.checkValidIntValue(((long) this.year) + j), this.month, this.day);
    }

    public LocalDate plusMonths(long j) {
        if (j == 0) {
            return this;
        }
        long j2 = (((long) this.year) * 12) + ((long) (this.month - 1)) + j;
        return resolvePreviousValid(ChronoField.YEAR.checkValidIntValue(Math.floorDiv(j2, 12L)), ((int) Math.floorMod(j2, 12L)) + 1, this.day);
    }

    public LocalDate plusWeeks(long j) {
        return plusDays(Math.multiplyExact(j, 7L));
    }

    public LocalDate plusDays(long j) {
        if (j == 0) {
            return this;
        }
        return ofEpochDay(Math.addExact(toEpochDay(), j));
    }

    @Override
    public LocalDate minus(TemporalAmount temporalAmount) {
        if (temporalAmount instanceof Period) {
            return minusMonths(((Period) temporalAmount).toTotalMonths()).minusDays(r4.getDays());
        }
        Objects.requireNonNull(temporalAmount, "amountToSubtract");
        return (LocalDate) temporalAmount.subtractFrom(this);
    }

    @Override
    public LocalDate minus(long j, TemporalUnit temporalUnit) {
        return j == Long.MIN_VALUE ? plus(Long.MAX_VALUE, temporalUnit).plus(1L, temporalUnit) : plus(-j, temporalUnit);
    }

    public LocalDate minusYears(long j) {
        return j == Long.MIN_VALUE ? plusYears(Long.MAX_VALUE).plusYears(1L) : plusYears(-j);
    }

    public LocalDate minusMonths(long j) {
        return j == Long.MIN_VALUE ? plusMonths(Long.MAX_VALUE).plusMonths(1L) : plusMonths(-j);
    }

    public LocalDate minusWeeks(long j) {
        return j == Long.MIN_VALUE ? plusWeeks(Long.MAX_VALUE).plusWeeks(1L) : plusWeeks(-j);
    }

    public LocalDate minusDays(long j) {
        return j == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1L) : plusDays(-j);
    }

    @Override
    public <R> R query(TemporalQuery<R> temporalQuery) {
        if (temporalQuery == TemporalQueries.localDate()) {
            return this;
        }
        return (R) super.query(temporalQuery);
    }

    @Override
    public Temporal adjustInto(Temporal temporal) {
        return super.adjustInto(temporal);
    }

    @Override
    public long until(Temporal temporal, TemporalUnit temporalUnit) {
        LocalDate localDateFrom = from((TemporalAccessor) temporal);
        if (temporalUnit instanceof ChronoUnit) {
            switch ((ChronoUnit) temporalUnit) {
                case DAYS:
                    return daysUntil(localDateFrom);
                case WEEKS:
                    return daysUntil(localDateFrom) / 7;
                case MONTHS:
                    return monthsUntil(localDateFrom);
                case YEARS:
                    return monthsUntil(localDateFrom) / 12;
                case DECADES:
                    return monthsUntil(localDateFrom) / 120;
                case CENTURIES:
                    return monthsUntil(localDateFrom) / 1200;
                case MILLENNIA:
                    return monthsUntil(localDateFrom) / 12000;
                case ERAS:
                    return localDateFrom.getLong(ChronoField.ERA) - getLong(ChronoField.ERA);
                default:
                    throw new UnsupportedTemporalTypeException("Unsupported unit: " + ((Object) temporalUnit));
            }
        }
        return temporalUnit.between(this, localDateFrom);
    }

    long daysUntil(LocalDate localDate) {
        return localDate.toEpochDay() - toEpochDay();
    }

    private long monthsUntil(LocalDate localDate) {
        return (((localDate.getProlepticMonth() * 32) + ((long) localDate.getDayOfMonth())) - ((getProlepticMonth() * 32) + ((long) getDayOfMonth()))) / 32;
    }

    @Override
    public Period until(ChronoLocalDate chronoLocalDate) {
        LocalDate localDateFrom = from((TemporalAccessor) chronoLocalDate);
        long prolepticMonth = localDateFrom.getProlepticMonth() - getProlepticMonth();
        int iLengthOfMonth = localDateFrom.day - this.day;
        if (prolepticMonth > 0 && iLengthOfMonth < 0) {
            prolepticMonth--;
            iLengthOfMonth = (int) (localDateFrom.toEpochDay() - plusMonths(prolepticMonth).toEpochDay());
        } else if (prolepticMonth < 0 && iLengthOfMonth > 0) {
            prolepticMonth++;
            iLengthOfMonth -= localDateFrom.lengthOfMonth();
        }
        return Period.of(Math.toIntExact(prolepticMonth / 12), (int) (prolepticMonth % 12), iLengthOfMonth);
    }

    @Override
    public String format(DateTimeFormatter dateTimeFormatter) {
        Objects.requireNonNull(dateTimeFormatter, "formatter");
        return dateTimeFormatter.format(this);
    }

    @Override
    public LocalDateTime atTime(LocalTime localTime) {
        return LocalDateTime.of(this, localTime);
    }

    public LocalDateTime atTime(int i, int i2) {
        return atTime(LocalTime.of(i, i2));
    }

    public LocalDateTime atTime(int i, int i2, int i3) {
        return atTime(LocalTime.of(i, i2, i3));
    }

    public LocalDateTime atTime(int i, int i2, int i3, int i4) {
        return atTime(LocalTime.of(i, i2, i3, i4));
    }

    public OffsetDateTime atTime(OffsetTime offsetTime) {
        return OffsetDateTime.of(LocalDateTime.of(this, offsetTime.toLocalTime()), offsetTime.getOffset());
    }

    public LocalDateTime atStartOfDay() {
        return LocalDateTime.of(this, LocalTime.MIDNIGHT);
    }

    public ZonedDateTime atStartOfDay(ZoneId zoneId) {
        ZoneOffsetTransition transition;
        Objects.requireNonNull(zoneId, "zone");
        LocalDateTime localDateTimeAtTime = atTime(LocalTime.MIDNIGHT);
        if (!(zoneId instanceof ZoneOffset) && (transition = zoneId.getRules().getTransition(localDateTimeAtTime)) != null && transition.isGap()) {
            localDateTimeAtTime = transition.getDateTimeAfter();
        }
        return ZonedDateTime.of(localDateTimeAtTime, zoneId);
    }

    @Override
    public long toEpochDay() {
        long j;
        long j2 = this.year;
        long j3 = this.month;
        long j4 = (365 * j2) + 0;
        if (j2 >= 0) {
            j = j4 + (((3 + j2) / 4) - ((99 + j2) / 100)) + ((j2 + 399) / 400);
        } else {
            j = j4 - (((j2 / (-4)) - (j2 / (-100))) + (j2 / (-400)));
        }
        long j5 = j + (((367 * j3) - 362) / 12) + ((long) (this.day - 1));
        if (j3 > 2) {
            j5--;
            if (!isLeapYear()) {
                j5--;
            }
        }
        return j5 - DAYS_0000_TO_1970;
    }

    @Override
    public int compareTo(ChronoLocalDate chronoLocalDate) {
        if (chronoLocalDate instanceof LocalDate) {
            return compareTo0((LocalDate) chronoLocalDate);
        }
        return super.compareTo(chronoLocalDate);
    }

    int compareTo0(LocalDate localDate) {
        int i = this.year - localDate.year;
        if (i == 0) {
            int i2 = this.month - localDate.month;
            if (i2 == 0) {
                return this.day - localDate.day;
            }
            return i2;
        }
        return i;
    }

    @Override
    public boolean isAfter(ChronoLocalDate chronoLocalDate) {
        if (chronoLocalDate instanceof LocalDate) {
            return compareTo0((LocalDate) chronoLocalDate) > 0;
        }
        return super.isAfter(chronoLocalDate);
    }

    @Override
    public boolean isBefore(ChronoLocalDate chronoLocalDate) {
        if (chronoLocalDate instanceof LocalDate) {
            return compareTo0((LocalDate) chronoLocalDate) < 0;
        }
        return super.isBefore(chronoLocalDate);
    }

    @Override
    public boolean isEqual(ChronoLocalDate chronoLocalDate) {
        if (chronoLocalDate instanceof LocalDate) {
            return compareTo0((LocalDate) chronoLocalDate) == 0;
        }
        return super.isEqual(chronoLocalDate);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return (obj instanceof LocalDate) && compareTo0((LocalDate) obj) == 0;
    }

    @Override
    public int hashCode() {
        int i = this.year;
        return (((i << 11) + (this.month << 6)) + this.day) ^ (i & (-2048));
    }

    @Override
    public String toString() {
        int i = this.year;
        short s = this.month;
        short s2 = this.day;
        int iAbs = Math.abs(i);
        StringBuilder sb = new StringBuilder(10);
        if (iAbs < 1000) {
            if (i < 0) {
                sb.append(i - 10000);
                sb.deleteCharAt(1);
            } else {
                sb.append(i + 10000);
                sb.deleteCharAt(0);
            }
        } else {
            if (i > 9999) {
                sb.append('+');
            }
            sb.append(i);
        }
        sb.append(s < 10 ? "-0" : LanguageTag.SEP);
        sb.append((int) s);
        sb.append(s2 < 10 ? "-0" : LanguageTag.SEP);
        sb.append((int) s2);
        return sb.toString();
    }

    private Object writeReplace() {
        return new Ser((byte) 3, this);
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    void writeExternal(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(this.year);
        dataOutput.writeByte(this.month);
        dataOutput.writeByte(this.day);
    }

    static LocalDate readExternal(DataInput dataInput) throws IOException {
        return of(dataInput.readInt(), dataInput.readByte(), dataInput.readByte());
    }
}
