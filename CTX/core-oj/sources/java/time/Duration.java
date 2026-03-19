package java.time;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sun.util.locale.LanguageTag;

public final class Duration implements TemporalAmount, Comparable<Duration>, Serializable {
    private static final long serialVersionUID = 3078945930695997490L;
    private final int nanos;
    private final long seconds;
    public static final Duration ZERO = new Duration(0, 0);
    private static final BigInteger BI_NANOS_PER_SECOND = BigInteger.valueOf(1000000000);
    private static final Pattern PATTERN = Pattern.compile("([-+]?)P(?:([-+]?[0-9]+)D)?(T(?:([-+]?[0-9]+)H)?(?:([-+]?[0-9]+)M)?(?:([-+]?[0-9]+)(?:[.,]([0-9]{0,9}))?S)?)?", 2);

    public static Duration ofDays(long j) {
        return create(Math.multiplyExact(j, 86400L), 0);
    }

    public static Duration ofHours(long j) {
        return create(Math.multiplyExact(j, 3600L), 0);
    }

    public static Duration ofMinutes(long j) {
        return create(Math.multiplyExact(j, 60L), 0);
    }

    public static Duration ofSeconds(long j) {
        return create(j, 0);
    }

    public static Duration ofSeconds(long j, long j2) {
        return create(Math.addExact(j, Math.floorDiv(j2, 1000000000L)), (int) Math.floorMod(j2, 1000000000L));
    }

    public static Duration ofMillis(long j) {
        long j2 = j / 1000;
        int i = (int) (j % 1000);
        if (i < 0) {
            i += 1000;
            j2--;
        }
        return create(j2, i * 1000000);
    }

    public static Duration ofNanos(long j) {
        long j2 = j / 1000000000;
        int i = (int) (j % 1000000000);
        if (i < 0) {
            i = (int) (((long) i) + 1000000000);
            j2--;
        }
        return create(j2, i);
    }

    public static Duration of(long j, TemporalUnit temporalUnit) {
        return ZERO.plus(j, temporalUnit);
    }

    public static Duration from(TemporalAmount temporalAmount) {
        Objects.requireNonNull(temporalAmount, "amount");
        Duration durationPlus = ZERO;
        for (TemporalUnit temporalUnit : temporalAmount.getUnits()) {
            durationPlus = durationPlus.plus(temporalAmount.get(temporalUnit), temporalUnit);
        }
        return durationPlus;
    }

    public static Duration parse(CharSequence charSequence) {
        Objects.requireNonNull(charSequence, "text");
        Matcher matcher = PATTERN.matcher(charSequence);
        if (matcher.matches() && !"T".equals(matcher.group(3))) {
            boolean zEquals = LanguageTag.SEP.equals(matcher.group(1));
            String strGroup = matcher.group(2);
            String strGroup2 = matcher.group(4);
            String strGroup3 = matcher.group(5);
            String strGroup4 = matcher.group(6);
            String strGroup5 = matcher.group(7);
            if (strGroup != null || strGroup2 != null || strGroup3 != null || strGroup4 != null) {
                long number = parseNumber(charSequence, strGroup, 86400, "days");
                long number2 = parseNumber(charSequence, strGroup2, 3600, "hours");
                long number3 = parseNumber(charSequence, strGroup3, 60, "minutes");
                long number4 = parseNumber(charSequence, strGroup4, 1, "seconds");
                try {
                    return create(zEquals, number, number2, number3, number4, parseFraction(charSequence, strGroup5, number4 < 0 ? -1 : 1));
                } catch (ArithmeticException e) {
                    throw ((DateTimeParseException) new DateTimeParseException("Text cannot be parsed to a Duration: overflow", charSequence, 0).initCause(e));
                }
            }
        }
        throw new DateTimeParseException("Text cannot be parsed to a Duration", charSequence, 0);
    }

    private static long parseNumber(CharSequence charSequence, String str, int i, String str2) {
        if (str == null) {
            return 0L;
        }
        try {
            return Math.multiplyExact(Long.parseLong(str), i);
        } catch (ArithmeticException | NumberFormatException e) {
            throw ((DateTimeParseException) new DateTimeParseException("Text cannot be parsed to a Duration: " + str2, charSequence, 0).initCause(e));
        }
    }

    private static int parseFraction(CharSequence charSequence, String str, int i) {
        if (str == null || str.length() == 0) {
            return 0;
        }
        try {
            return Integer.parseInt((str + "000000000").substring(0, 9)) * i;
        } catch (ArithmeticException | NumberFormatException e) {
            throw ((DateTimeParseException) new DateTimeParseException("Text cannot be parsed to a Duration: fraction", charSequence, 0).initCause(e));
        }
    }

    private static Duration create(boolean z, long j, long j2, long j3, long j4, int i) {
        long jAddExact = Math.addExact(j, Math.addExact(j2, Math.addExact(j3, j4)));
        if (z) {
            return ofSeconds(jAddExact, i).negated();
        }
        return ofSeconds(jAddExact, i);
    }

    public static Duration between(Temporal temporal, Temporal temporal2) {
        long j;
        try {
            return ofNanos(temporal.until(temporal2, ChronoUnit.NANOS));
        } catch (ArithmeticException | DateTimeException e) {
            long jUntil = temporal.until(temporal2, ChronoUnit.SECONDS);
            try {
                j = temporal2.getLong(ChronoField.NANO_OF_SECOND) - temporal.getLong(ChronoField.NANO_OF_SECOND);
                if (jUntil > 0 && j < 0) {
                    jUntil++;
                } else if (jUntil < 0 && j > 0) {
                    jUntil--;
                }
            } catch (DateTimeException e2) {
                j = 0;
            }
            return ofSeconds(jUntil, j);
        }
    }

    private static Duration create(long j, int i) {
        if ((((long) i) | j) == 0) {
            return ZERO;
        }
        return new Duration(j, i);
    }

    private Duration(long j, int i) {
        this.seconds = j;
        this.nanos = i;
    }

    @Override
    public long get(TemporalUnit temporalUnit) {
        if (temporalUnit == ChronoUnit.SECONDS) {
            return this.seconds;
        }
        if (temporalUnit == ChronoUnit.NANOS) {
            return this.nanos;
        }
        throw new UnsupportedTemporalTypeException("Unsupported unit: " + ((Object) temporalUnit));
    }

    @Override
    public List<TemporalUnit> getUnits() {
        return DurationUnits.UNITS;
    }

    private static class DurationUnits {
        static final List<TemporalUnit> UNITS = Collections.unmodifiableList(Arrays.asList(ChronoUnit.SECONDS, ChronoUnit.NANOS));

        private DurationUnits() {
        }
    }

    public boolean isZero() {
        return (this.seconds | ((long) this.nanos)) == 0;
    }

    public boolean isNegative() {
        return this.seconds < 0;
    }

    public long getSeconds() {
        return this.seconds;
    }

    public int getNano() {
        return this.nanos;
    }

    public Duration withSeconds(long j) {
        return create(j, this.nanos);
    }

    public Duration withNanos(int i) {
        ChronoField.NANO_OF_SECOND.checkValidIntValue(i);
        return create(this.seconds, i);
    }

    public Duration plus(Duration duration) {
        return plus(duration.getSeconds(), duration.getNano());
    }

    public Duration plus(long j, TemporalUnit temporalUnit) {
        Objects.requireNonNull(temporalUnit, "unit");
        if (temporalUnit == ChronoUnit.DAYS) {
            return plus(Math.multiplyExact(j, 86400L), 0L);
        }
        if (temporalUnit.isDurationEstimated()) {
            throw new UnsupportedTemporalTypeException("Unit must not have an estimated duration");
        }
        if (j == 0) {
            return this;
        }
        if (temporalUnit instanceof ChronoUnit) {
            switch ((ChronoUnit) temporalUnit) {
                case NANOS:
                    return plusNanos(j);
                case MICROS:
                    return plusSeconds((j / 1000000000) * 1000).plusNanos((j % 1000000000) * 1000);
                case MILLIS:
                    return plusMillis(j);
                case SECONDS:
                    return plusSeconds(j);
                default:
                    return plusSeconds(Math.multiplyExact(temporalUnit.getDuration().seconds, j));
            }
        }
        return plusSeconds(temporalUnit.getDuration().multipliedBy(j).getSeconds()).plusNanos(r7.getNano());
    }

    public Duration plusDays(long j) {
        return plus(Math.multiplyExact(j, 86400L), 0L);
    }

    public Duration plusHours(long j) {
        return plus(Math.multiplyExact(j, 3600L), 0L);
    }

    public Duration plusMinutes(long j) {
        return plus(Math.multiplyExact(j, 60L), 0L);
    }

    public Duration plusSeconds(long j) {
        return plus(j, 0L);
    }

    public Duration plusMillis(long j) {
        return plus(j / 1000, (j % 1000) * 1000000);
    }

    public Duration plusNanos(long j) {
        return plus(0L, j);
    }

    private Duration plus(long j, long j2) {
        if ((j | j2) == 0) {
            return this;
        }
        return ofSeconds(Math.addExact(Math.addExact(this.seconds, j), j2 / 1000000000), ((long) this.nanos) + (j2 % 1000000000));
    }

    public Duration minus(Duration duration) {
        long seconds = duration.getSeconds();
        int nano = duration.getNano();
        if (seconds == Long.MIN_VALUE) {
            return plus(Long.MAX_VALUE, -nano).plus(1L, 0L);
        }
        return plus(-seconds, -nano);
    }

    public Duration minus(long j, TemporalUnit temporalUnit) {
        return j == Long.MIN_VALUE ? plus(Long.MAX_VALUE, temporalUnit).plus(1L, temporalUnit) : plus(-j, temporalUnit);
    }

    public Duration minusDays(long j) {
        return j == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1L) : plusDays(-j);
    }

    public Duration minusHours(long j) {
        return j == Long.MIN_VALUE ? plusHours(Long.MAX_VALUE).plusHours(1L) : plusHours(-j);
    }

    public Duration minusMinutes(long j) {
        return j == Long.MIN_VALUE ? plusMinutes(Long.MAX_VALUE).plusMinutes(1L) : plusMinutes(-j);
    }

    public Duration minusSeconds(long j) {
        return j == Long.MIN_VALUE ? plusSeconds(Long.MAX_VALUE).plusSeconds(1L) : plusSeconds(-j);
    }

    public Duration minusMillis(long j) {
        return j == Long.MIN_VALUE ? plusMillis(Long.MAX_VALUE).plusMillis(1L) : plusMillis(-j);
    }

    public Duration minusNanos(long j) {
        return j == Long.MIN_VALUE ? plusNanos(Long.MAX_VALUE).plusNanos(1L) : plusNanos(-j);
    }

    public Duration multipliedBy(long j) {
        if (j == 0) {
            return ZERO;
        }
        if (j == 1) {
            return this;
        }
        return create(toSeconds().multiply(BigDecimal.valueOf(j)));
    }

    public Duration dividedBy(long j) {
        if (j == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        if (j == 1) {
            return this;
        }
        return create(toSeconds().divide(BigDecimal.valueOf(j), RoundingMode.DOWN));
    }

    private BigDecimal toSeconds() {
        return BigDecimal.valueOf(this.seconds).add(BigDecimal.valueOf(this.nanos, 9));
    }

    private static Duration create(BigDecimal bigDecimal) {
        BigInteger bigIntegerExact = bigDecimal.movePointRight(9).toBigIntegerExact();
        BigInteger[] bigIntegerArrDivideAndRemainder = bigIntegerExact.divideAndRemainder(BI_NANOS_PER_SECOND);
        if (bigIntegerArrDivideAndRemainder[0].bitLength() > 63) {
            throw new ArithmeticException("Exceeds capacity of Duration: " + ((Object) bigIntegerExact));
        }
        return ofSeconds(bigIntegerArrDivideAndRemainder[0].longValue(), bigIntegerArrDivideAndRemainder[1].intValue());
    }

    public Duration negated() {
        return multipliedBy(-1L);
    }

    public Duration abs() {
        return isNegative() ? negated() : this;
    }

    @Override
    public Temporal addTo(Temporal temporal) {
        if (this.seconds != 0) {
            temporal = temporal.plus(this.seconds, ChronoUnit.SECONDS);
        }
        if (this.nanos != 0) {
            return temporal.plus(this.nanos, ChronoUnit.NANOS);
        }
        return temporal;
    }

    @Override
    public Temporal subtractFrom(Temporal temporal) {
        if (this.seconds != 0) {
            temporal = temporal.minus(this.seconds, ChronoUnit.SECONDS);
        }
        if (this.nanos != 0) {
            return temporal.minus(this.nanos, ChronoUnit.NANOS);
        }
        return temporal;
    }

    public long toDays() {
        return this.seconds / 86400;
    }

    public long toHours() {
        return this.seconds / 3600;
    }

    public long toMinutes() {
        return this.seconds / 60;
    }

    public long toMillis() {
        return Math.addExact(Math.multiplyExact(this.seconds, 1000L), this.nanos / 1000000);
    }

    public long toNanos() {
        return Math.addExact(Math.multiplyExact(this.seconds, 1000000000L), this.nanos);
    }

    @Override
    public int compareTo(Duration duration) {
        int iCompare = Long.compare(this.seconds, duration.seconds);
        if (iCompare != 0) {
            return iCompare;
        }
        return this.nanos - duration.nanos;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Duration)) {
            return false;
        }
        Duration duration = (Duration) obj;
        return this.seconds == duration.seconds && this.nanos == duration.nanos;
    }

    public int hashCode() {
        return ((int) (this.seconds ^ (this.seconds >>> 32))) + (51 * this.nanos);
    }

    public String toString() {
        if (this == ZERO) {
            return "PT0S";
        }
        long j = this.seconds / 3600;
        int i = (int) ((this.seconds % 3600) / 60);
        int i2 = (int) (this.seconds % 60);
        StringBuilder sb = new StringBuilder(24);
        sb.append("PT");
        if (j != 0) {
            sb.append(j);
            sb.append('H');
        }
        if (i != 0) {
            sb.append(i);
            sb.append('M');
        }
        if (i2 == 0 && this.nanos == 0 && sb.length() > 2) {
            return sb.toString();
        }
        if (i2 < 0 && this.nanos > 0) {
            if (i2 == -1) {
                sb.append("-0");
            } else {
                sb.append(i2 + 1);
            }
        } else {
            sb.append(i2);
        }
        if (this.nanos > 0) {
            int length = sb.length();
            if (i2 < 0) {
                sb.append(2000000000 - ((long) this.nanos));
            } else {
                sb.append(((long) this.nanos) + 1000000000);
            }
            while (sb.charAt(sb.length() - 1) == '0') {
                sb.setLength(sb.length() - 1);
            }
            sb.setCharAt(length, '.');
        }
        sb.append('S');
        return sb.toString();
    }

    private Object writeReplace() {
        return new Ser((byte) 1, this);
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    void writeExternal(DataOutput dataOutput) throws IOException {
        dataOutput.writeLong(this.seconds);
        dataOutput.writeInt(this.nanos);
    }

    static Duration readExternal(DataInput dataInput) throws IOException {
        return ofSeconds(dataInput.readLong(), dataInput.readInt());
    }
}
