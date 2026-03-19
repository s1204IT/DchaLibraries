package java.time.chrono;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Clock;
import java.time.DateTimeException;
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
import java.util.Calendar;
import java.util.Objects;
import java.util.TimeZone;
import sun.util.calendar.CalendarDate;
import sun.util.calendar.LocalGregorianCalendar;

public final class JapaneseDate extends ChronoLocalDateImpl<JapaneseDate> implements ChronoLocalDate, Serializable {
    static final LocalDate MEIJI_6_ISODATE = LocalDate.of(1873, 1, 1);
    private static final long serialVersionUID = -305327627230580483L;
    private transient JapaneseEra era;
    private final transient LocalDate isoDate;
    private transient int yearOfEra;

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public long until(Temporal temporal, TemporalUnit temporalUnit) {
        return super.until(temporal, temporalUnit);
    }

    public static JapaneseDate now() {
        return now(Clock.systemDefaultZone());
    }

    public static JapaneseDate now(ZoneId zoneId) {
        return now(Clock.system(zoneId));
    }

    public static JapaneseDate now(Clock clock) {
        return new JapaneseDate(LocalDate.now(clock));
    }

    public static JapaneseDate of(JapaneseEra japaneseEra, int i, int i2, int i3) {
        Objects.requireNonNull(japaneseEra, "era");
        LocalGregorianCalendar.Date dateNewCalendarDate = JapaneseChronology.JCAL.newCalendarDate((TimeZone) null);
        dateNewCalendarDate.setEra(japaneseEra.getPrivateEra()).setDate(i, i2, i3);
        if (!JapaneseChronology.JCAL.validate(dateNewCalendarDate)) {
            throw new DateTimeException("year, month, and day not valid for Era");
        }
        return new JapaneseDate(japaneseEra, i, LocalDate.of(dateNewCalendarDate.getNormalizedYear(), i2, i3));
    }

    public static JapaneseDate of(int i, int i2, int i3) {
        return new JapaneseDate(LocalDate.of(i, i2, i3));
    }

    static JapaneseDate ofYearDay(JapaneseEra japaneseEra, int i, int i2) {
        Objects.requireNonNull(japaneseEra, "era");
        CalendarDate sinceDate = japaneseEra.getPrivateEra().getSinceDate();
        LocalGregorianCalendar.Date dateNewCalendarDate = JapaneseChronology.JCAL.newCalendarDate((TimeZone) null);
        dateNewCalendarDate.setEra(japaneseEra.getPrivateEra());
        if (i == 1) {
            dateNewCalendarDate.setDate(i, sinceDate.getMonth(), (sinceDate.getDayOfMonth() + i2) - 1);
        } else {
            dateNewCalendarDate.setDate(i, 1, i2);
        }
        JapaneseChronology.JCAL.normalize(dateNewCalendarDate);
        if (japaneseEra.getPrivateEra() != dateNewCalendarDate.getEra() || i != dateNewCalendarDate.getYear()) {
            throw new DateTimeException("Invalid parameters");
        }
        return new JapaneseDate(japaneseEra, i, LocalDate.of(dateNewCalendarDate.getNormalizedYear(), dateNewCalendarDate.getMonth(), dateNewCalendarDate.getDayOfMonth()));
    }

    public static JapaneseDate from(TemporalAccessor temporalAccessor) {
        return JapaneseChronology.INSTANCE.date(temporalAccessor);
    }

    JapaneseDate(LocalDate localDate) {
        if (localDate.isBefore(MEIJI_6_ISODATE)) {
            throw new DateTimeException("JapaneseDate before Meiji 6 is not supported");
        }
        LocalGregorianCalendar.Date privateJapaneseDate = toPrivateJapaneseDate(localDate);
        this.era = JapaneseEra.toJapaneseEra(privateJapaneseDate.getEra());
        this.yearOfEra = privateJapaneseDate.getYear();
        this.isoDate = localDate;
    }

    JapaneseDate(JapaneseEra japaneseEra, int i, LocalDate localDate) {
        if (localDate.isBefore(MEIJI_6_ISODATE)) {
            throw new DateTimeException("JapaneseDate before Meiji 6 is not supported");
        }
        this.era = japaneseEra;
        this.yearOfEra = i;
        this.isoDate = localDate;
    }

    @Override
    public JapaneseChronology getChronology() {
        return JapaneseChronology.INSTANCE;
    }

    @Override
    public JapaneseEra getEra() {
        return this.era;
    }

    @Override
    public int lengthOfMonth() {
        return this.isoDate.lengthOfMonth();
    }

    @Override
    public int lengthOfYear() {
        Calendar calendarCreateCalendar = JapaneseChronology.createCalendar();
        calendarCreateCalendar.set(0, this.era.getValue() + 2);
        calendarCreateCalendar.set(this.yearOfEra, this.isoDate.getMonthValue() - 1, this.isoDate.getDayOfMonth());
        return calendarCreateCalendar.getActualMaximum(6);
    }

    @Override
    public boolean isSupported(TemporalField temporalField) {
        if (temporalField == ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH || temporalField == ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR || temporalField == ChronoField.ALIGNED_WEEK_OF_MONTH || temporalField == ChronoField.ALIGNED_WEEK_OF_YEAR) {
            return false;
        }
        return super.isSupported(temporalField);
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
                    case YEAR_OF_ERA:
                        Calendar calendarCreateCalendar = JapaneseChronology.createCalendar();
                        calendarCreateCalendar.set(0, this.era.getValue() + 2);
                        calendarCreateCalendar.set(this.yearOfEra, this.isoDate.getMonthValue() - 1, this.isoDate.getDayOfMonth());
                        return ValueRange.of(1L, calendarCreateCalendar.getActualMaximum(1));
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
                case DAY_OF_YEAR:
                    Calendar calendarCreateCalendar = JapaneseChronology.createCalendar();
                    calendarCreateCalendar.set(0, this.era.getValue() + 2);
                    calendarCreateCalendar.set(this.yearOfEra, this.isoDate.getMonthValue() - 1, this.isoDate.getDayOfMonth());
                    return calendarCreateCalendar.get(6);
                case YEAR_OF_ERA:
                    return this.yearOfEra;
                case ALIGNED_DAY_OF_WEEK_IN_MONTH:
                case ALIGNED_DAY_OF_WEEK_IN_YEAR:
                case ALIGNED_WEEK_OF_MONTH:
                case ALIGNED_WEEK_OF_YEAR:
                    throw new UnsupportedTemporalTypeException("Unsupported field: " + ((Object) temporalField));
                case ERA:
                    return this.era.getValue();
                default:
                    return this.isoDate.getLong(temporalField);
            }
        }
        return temporalField.getFrom(this);
    }

    private static LocalGregorianCalendar.Date toPrivateJapaneseDate(LocalDate localDate) {
        LocalGregorianCalendar.Date dateNewCalendarDate = JapaneseChronology.JCAL.newCalendarDate((TimeZone) null);
        sun.util.calendar.Era eraPrivateEraFrom = JapaneseEra.privateEraFrom(localDate);
        int year = localDate.getYear();
        if (eraPrivateEraFrom != null) {
            year -= eraPrivateEraFrom.getSinceDate().getYear() - 1;
        }
        dateNewCalendarDate.setEra(eraPrivateEraFrom).setYear(year).setMonth(localDate.getMonthValue()).setDayOfMonth(localDate.getDayOfMonth());
        JapaneseChronology.JCAL.normalize(dateNewCalendarDate);
        return dateNewCalendarDate;
    }

    @Override
    public JapaneseDate with(TemporalField temporalField, long j) {
        if (temporalField instanceof ChronoField) {
            ChronoField chronoField = (ChronoField) temporalField;
            if (getLong(chronoField) == j) {
                return this;
            }
            int i = AnonymousClass1.$SwitchMap$java$time$temporal$ChronoField[chronoField.ordinal()];
            if (i != 3) {
                switch (i) {
                    case 8:
                    case 9:
                        int iCheckValidIntValue = getChronology().range(chronoField).checkValidIntValue(j, chronoField);
                        int i2 = AnonymousClass1.$SwitchMap$java$time$temporal$ChronoField[chronoField.ordinal()];
                        if (i2 == 3) {
                            return withYear(iCheckValidIntValue);
                        }
                        switch (i2) {
                            case 8:
                                return withYear(JapaneseEra.of(iCheckValidIntValue), this.yearOfEra);
                            case 9:
                                return with(this.isoDate.withYear(iCheckValidIntValue));
                        }
                }
            }
            return with(this.isoDate.with(temporalField, j));
        }
        return (JapaneseDate) super.with(temporalField, j);
    }

    @Override
    public JapaneseDate with(TemporalAdjuster temporalAdjuster) {
        return (JapaneseDate) super.with(temporalAdjuster);
    }

    @Override
    public JapaneseDate plus(TemporalAmount temporalAmount) {
        return (JapaneseDate) super.plus(temporalAmount);
    }

    @Override
    public JapaneseDate minus(TemporalAmount temporalAmount) {
        return (JapaneseDate) super.minus(temporalAmount);
    }

    private JapaneseDate withYear(JapaneseEra japaneseEra, int i) {
        return with(this.isoDate.withYear(JapaneseChronology.INSTANCE.prolepticYear(japaneseEra, i)));
    }

    private JapaneseDate withYear(int i) {
        return withYear(getEra(), i);
    }

    @Override
    JapaneseDate plusYears(long j) {
        return with(this.isoDate.plusYears(j));
    }

    @Override
    JapaneseDate plusMonths(long j) {
        return with(this.isoDate.plusMonths(j));
    }

    @Override
    JapaneseDate plusWeeks(long j) {
        return with(this.isoDate.plusWeeks(j));
    }

    @Override
    JapaneseDate plusDays(long j) {
        return with(this.isoDate.plusDays(j));
    }

    @Override
    public JapaneseDate plus(long j, TemporalUnit temporalUnit) {
        return (JapaneseDate) super.plus(j, temporalUnit);
    }

    @Override
    public JapaneseDate minus(long j, TemporalUnit temporalUnit) {
        return (JapaneseDate) super.minus(j, temporalUnit);
    }

    @Override
    JapaneseDate minusYears(long j) {
        return (JapaneseDate) super.minusYears(j);
    }

    @Override
    JapaneseDate minusMonths(long j) {
        return (JapaneseDate) super.minusMonths(j);
    }

    @Override
    JapaneseDate minusWeeks(long j) {
        return (JapaneseDate) super.minusWeeks(j);
    }

    @Override
    JapaneseDate minusDays(long j) {
        return (JapaneseDate) super.minusDays(j);
    }

    private JapaneseDate with(LocalDate localDate) {
        return localDate.equals(this.isoDate) ? this : new JapaneseDate(localDate);
    }

    @Override
    public final ChronoLocalDateTime<JapaneseDate> atTime(LocalTime localTime) {
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
        if (obj instanceof JapaneseDate) {
            return this.isoDate.equals(((JapaneseDate) obj).isoDate);
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
        return new Ser((byte) 4, this);
    }

    void writeExternal(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(get(ChronoField.YEAR));
        dataOutput.writeByte(get(ChronoField.MONTH_OF_YEAR));
        dataOutput.writeByte(get(ChronoField.DAY_OF_MONTH));
    }

    static JapaneseDate readExternal(DataInput dataInput) throws IOException {
        return JapaneseChronology.INSTANCE.date(dataInput.readInt(), (int) dataInput.readByte(), (int) dataInput.readByte());
    }
}
