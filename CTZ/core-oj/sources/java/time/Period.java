package java.time;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoPeriod;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sun.util.locale.LanguageTag;

public final class Period implements ChronoPeriod, Serializable {
    private static final long serialVersionUID = -3587258372562876L;
    private final int days;
    private final int months;
    private final int years;
    public static final Period ZERO = new Period(0, 0, 0);
    private static final Pattern PATTERN = Pattern.compile("([-+]?)P(?:([-+]?[0-9]+)Y)?(?:([-+]?[0-9]+)M)?(?:([-+]?[0-9]+)W)?(?:([-+]?[0-9]+)D)?", 2);
    private static final List<TemporalUnit> SUPPORTED_UNITS = Collections.unmodifiableList(Arrays.asList(ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS));

    public static Period ofYears(int i) {
        return create(i, 0, 0);
    }

    public static Period ofMonths(int i) {
        return create(0, i, 0);
    }

    public static Period ofWeeks(int i) {
        return create(0, 0, Math.multiplyExact(i, 7));
    }

    public static Period ofDays(int i) {
        return create(0, 0, i);
    }

    public static Period of(int i, int i2, int i3) {
        return create(i, i2, i3);
    }

    public static Period from(TemporalAmount temporalAmount) {
        if (temporalAmount instanceof Period) {
            return (Period) temporalAmount;
        }
        if ((temporalAmount instanceof ChronoPeriod) && !IsoChronology.INSTANCE.equals(((ChronoPeriod) temporalAmount).getChronology())) {
            throw new DateTimeException("Period requires ISO chronology: " + ((Object) temporalAmount));
        }
        Objects.requireNonNull(temporalAmount, "amount");
        int intExact = 0;
        int intExact2 = 0;
        int intExact3 = 0;
        for (TemporalUnit temporalUnit : temporalAmount.getUnits()) {
            long j = temporalAmount.get(temporalUnit);
            if (temporalUnit == ChronoUnit.YEARS) {
                intExact = Math.toIntExact(j);
            } else if (temporalUnit == ChronoUnit.MONTHS) {
                intExact2 = Math.toIntExact(j);
            } else if (temporalUnit == ChronoUnit.DAYS) {
                intExact3 = Math.toIntExact(j);
            } else {
                throw new DateTimeException("Unit must be Years, Months or Days, but was " + ((Object) temporalUnit));
            }
        }
        return create(intExact, intExact2, intExact3);
    }

    public static Period parse(CharSequence charSequence) {
        Objects.requireNonNull(charSequence, "text");
        Matcher matcher = PATTERN.matcher(charSequence);
        if (matcher.matches()) {
            int i = LanguageTag.SEP.equals(matcher.group(1)) ? -1 : 1;
            String strGroup = matcher.group(2);
            String strGroup2 = matcher.group(3);
            String strGroup3 = matcher.group(4);
            String strGroup4 = matcher.group(5);
            if (strGroup != null || strGroup2 != null || strGroup4 != null || strGroup3 != null) {
                try {
                    return create(parseNumber(charSequence, strGroup, i), parseNumber(charSequence, strGroup2, i), Math.addExact(parseNumber(charSequence, strGroup4, i), Math.multiplyExact(parseNumber(charSequence, strGroup3, i), 7)));
                } catch (NumberFormatException e) {
                    throw new DateTimeParseException("Text cannot be parsed to a Period", charSequence, 0, e);
                }
            }
        }
        throw new DateTimeParseException("Text cannot be parsed to a Period", charSequence, 0);
    }

    private static int parseNumber(CharSequence charSequence, String str, int i) {
        if (str == null) {
            return 0;
        }
        try {
            return Math.multiplyExact(Integer.parseInt(str), i);
        } catch (ArithmeticException e) {
            throw new DateTimeParseException("Text cannot be parsed to a Period", charSequence, 0, e);
        }
    }

    public static Period between(LocalDate localDate, LocalDate localDate2) {
        return localDate.until((ChronoLocalDate) localDate2);
    }

    private static Period create(int i, int i2, int i3) {
        if ((i | i2 | i3) == 0) {
            return ZERO;
        }
        return new Period(i, i2, i3);
    }

    private Period(int i, int i2, int i3) {
        this.years = i;
        this.months = i2;
        this.days = i3;
    }

    @Override
    public long get(TemporalUnit temporalUnit) {
        if (temporalUnit == ChronoUnit.YEARS) {
            return getYears();
        }
        if (temporalUnit == ChronoUnit.MONTHS) {
            return getMonths();
        }
        if (temporalUnit == ChronoUnit.DAYS) {
            return getDays();
        }
        throw new UnsupportedTemporalTypeException("Unsupported unit: " + ((Object) temporalUnit));
    }

    @Override
    public List<TemporalUnit> getUnits() {
        return SUPPORTED_UNITS;
    }

    @Override
    public IsoChronology getChronology() {
        return IsoChronology.INSTANCE;
    }

    @Override
    public boolean isZero() {
        return this == ZERO;
    }

    @Override
    public boolean isNegative() {
        return this.years < 0 || this.months < 0 || this.days < 0;
    }

    public int getYears() {
        return this.years;
    }

    public int getMonths() {
        return this.months;
    }

    public int getDays() {
        return this.days;
    }

    public Period withYears(int i) {
        if (i == this.years) {
            return this;
        }
        return create(i, this.months, this.days);
    }

    public Period withMonths(int i) {
        if (i == this.months) {
            return this;
        }
        return create(this.years, i, this.days);
    }

    public Period withDays(int i) {
        if (i == this.days) {
            return this;
        }
        return create(this.years, this.months, i);
    }

    @Override
    public Period plus(TemporalAmount temporalAmount) {
        Period periodFrom = from(temporalAmount);
        return create(Math.addExact(this.years, periodFrom.years), Math.addExact(this.months, periodFrom.months), Math.addExact(this.days, periodFrom.days));
    }

    public Period plusYears(long j) {
        if (j == 0) {
            return this;
        }
        return create(Math.toIntExact(Math.addExact(this.years, j)), this.months, this.days);
    }

    public Period plusMonths(long j) {
        if (j == 0) {
            return this;
        }
        return create(this.years, Math.toIntExact(Math.addExact(this.months, j)), this.days);
    }

    public Period plusDays(long j) {
        if (j == 0) {
            return this;
        }
        return create(this.years, this.months, Math.toIntExact(Math.addExact(this.days, j)));
    }

    @Override
    public Period minus(TemporalAmount temporalAmount) {
        Period periodFrom = from(temporalAmount);
        return create(Math.subtractExact(this.years, periodFrom.years), Math.subtractExact(this.months, periodFrom.months), Math.subtractExact(this.days, periodFrom.days));
    }

    public Period minusYears(long j) {
        return j == Long.MIN_VALUE ? plusYears(Long.MAX_VALUE).plusYears(1L) : plusYears(-j);
    }

    public Period minusMonths(long j) {
        return j == Long.MIN_VALUE ? plusMonths(Long.MAX_VALUE).plusMonths(1L) : plusMonths(-j);
    }

    public Period minusDays(long j) {
        return j == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1L) : plusDays(-j);
    }

    @Override
    public Period multipliedBy(int i) {
        if (this == ZERO || i == 1) {
            return this;
        }
        return create(Math.multiplyExact(this.years, i), Math.multiplyExact(this.months, i), Math.multiplyExact(this.days, i));
    }

    @Override
    public Period negated() {
        return multipliedBy(-1);
    }

    @Override
    public Period normalized() {
        long totalMonths = toTotalMonths();
        long j = totalMonths / 12;
        int i = (int) (totalMonths % 12);
        if (j == this.years && i == this.months) {
            return this;
        }
        return create(Math.toIntExact(j), i, this.days);
    }

    public long toTotalMonths() {
        return (((long) this.years) * 12) + ((long) this.months);
    }

    @Override
    public Temporal addTo(Temporal temporal) {
        validateChrono(temporal);
        if (this.months == 0) {
            if (this.years != 0) {
                temporal = temporal.plus(this.years, ChronoUnit.YEARS);
            }
        } else {
            long totalMonths = toTotalMonths();
            if (totalMonths != 0) {
                temporal = temporal.plus(totalMonths, ChronoUnit.MONTHS);
            }
        }
        if (this.days != 0) {
            return temporal.plus(this.days, ChronoUnit.DAYS);
        }
        return temporal;
    }

    @Override
    public Temporal subtractFrom(Temporal temporal) {
        validateChrono(temporal);
        if (this.months == 0) {
            if (this.years != 0) {
                temporal = temporal.minus(this.years, ChronoUnit.YEARS);
            }
        } else {
            long totalMonths = toTotalMonths();
            if (totalMonths != 0) {
                temporal = temporal.minus(totalMonths, ChronoUnit.MONTHS);
            }
        }
        if (this.days != 0) {
            return temporal.minus(this.days, ChronoUnit.DAYS);
        }
        return temporal;
    }

    private void validateChrono(TemporalAccessor temporalAccessor) {
        Objects.requireNonNull(temporalAccessor, "temporal");
        Chronology chronology = (Chronology) temporalAccessor.query(TemporalQueries.chronology());
        if (chronology != null && !IsoChronology.INSTANCE.equals(chronology)) {
            throw new DateTimeException("Chronology mismatch, expected: ISO, actual: " + chronology.getId());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Period)) {
            return false;
        }
        Period period = (Period) obj;
        return this.years == period.years && this.months == period.months && this.days == period.days;
    }

    @Override
    public int hashCode() {
        return this.years + Integer.rotateLeft(this.months, 8) + Integer.rotateLeft(this.days, 16);
    }

    @Override
    public String toString() {
        if (this == ZERO) {
            return "P0D";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('P');
        if (this.years != 0) {
            sb.append(this.years);
            sb.append('Y');
        }
        if (this.months != 0) {
            sb.append(this.months);
            sb.append('M');
        }
        if (this.days != 0) {
            sb.append(this.days);
            sb.append('D');
        }
        return sb.toString();
    }

    private Object writeReplace() {
        return new Ser((byte) 14, this);
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    void writeExternal(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(this.years);
        dataOutput.writeInt(this.months);
        dataOutput.writeInt(this.days);
    }

    static Period readExternal(DataInput dataInput) throws IOException {
        return of(dataInput.readInt(), dataInput.readInt(), dataInput.readInt());
    }
}
