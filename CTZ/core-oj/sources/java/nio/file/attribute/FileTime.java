package java.nio.file.attribute;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import sun.util.locale.LanguageTag;

public final class FileTime implements Comparable<FileTime> {
    private static final long DAYS_PER_10000_YEARS = 3652425;
    private static final long HOURS_PER_DAY = 24;
    private static final long MAX_SECOND = 31556889864403199L;
    private static final long MICROS_PER_SECOND = 1000000;
    private static final long MILLIS_PER_SECOND = 1000;
    private static final long MINUTES_PER_HOUR = 60;
    private static final long MIN_SECOND = -31557014167219200L;
    private static final int NANOS_PER_MICRO = 1000;
    private static final int NANOS_PER_MILLI = 1000000;
    private static final long NANOS_PER_SECOND = 1000000000;
    private static final long SECONDS_0000_TO_1970 = 62167219200L;
    private static final long SECONDS_PER_10000_YEARS = 315569520000L;
    private static final long SECONDS_PER_DAY = 86400;
    private static final long SECONDS_PER_HOUR = 3600;
    private static final long SECONDS_PER_MINUTE = 60;
    private Instant instant;
    private final TimeUnit unit;
    private final long value;
    private String valueAsString;

    private FileTime(long j, TimeUnit timeUnit, Instant instant) {
        this.value = j;
        this.unit = timeUnit;
        this.instant = instant;
    }

    public static FileTime from(long j, TimeUnit timeUnit) {
        Objects.requireNonNull(timeUnit, "unit");
        return new FileTime(j, timeUnit, null);
    }

    public static FileTime fromMillis(long j) {
        return new FileTime(j, TimeUnit.MILLISECONDS, null);
    }

    public static FileTime from(Instant instant) {
        Objects.requireNonNull(instant, "instant");
        return new FileTime(0L, null, instant);
    }

    public long to(TimeUnit timeUnit) {
        Objects.requireNonNull(timeUnit, "unit");
        if (this.unit != null) {
            return timeUnit.convert(this.value, this.unit);
        }
        long jConvert = timeUnit.convert(this.instant.getEpochSecond(), TimeUnit.SECONDS);
        if (jConvert == Long.MIN_VALUE || jConvert == Long.MAX_VALUE) {
            return jConvert;
        }
        long jConvert2 = timeUnit.convert(this.instant.getNano(), TimeUnit.NANOSECONDS);
        long j = jConvert + jConvert2;
        if (((jConvert2 ^ j) & (jConvert ^ j)) >= 0) {
            return j;
        }
        if (jConvert < 0) {
            return Long.MIN_VALUE;
        }
        return Long.MAX_VALUE;
    }

    public long toMillis() {
        if (this.unit != null) {
            return this.unit.toMillis(this.value);
        }
        long epochSecond = this.instant.getEpochSecond();
        int nano = this.instant.getNano();
        long j = epochSecond * MILLIS_PER_SECOND;
        if (((Math.abs(epochSecond) | MILLIS_PER_SECOND) >>> 31) == 0 || j / MILLIS_PER_SECOND == epochSecond) {
            return j + ((long) (nano / NANOS_PER_MILLI));
        }
        if (epochSecond < 0) {
            return Long.MIN_VALUE;
        }
        return Long.MAX_VALUE;
    }

    private static long scale(long j, long j2, long j3) {
        if (j > j3) {
            return Long.MAX_VALUE;
        }
        if (j < (-j3)) {
            return Long.MIN_VALUE;
        }
        return j * j2;
    }

    public Instant toInstant() {
        long jScale;
        long jFloorDiv;
        int iFloorMod;
        if (this.instant == null) {
            int i = 0;
            switch (this.unit) {
                case DAYS:
                    jScale = scale(this.value, SECONDS_PER_DAY, 106751991167300L);
                    if (jScale <= MIN_SECOND) {
                        this.instant = Instant.MIN;
                    } else if (jScale >= MAX_SECOND) {
                        this.instant = Instant.MAX;
                    } else {
                        this.instant = Instant.ofEpochSecond(jScale, i);
                    }
                    break;
                case HOURS:
                    jScale = scale(this.value, SECONDS_PER_HOUR, 2562047788015215L);
                    if (jScale <= MIN_SECOND) {
                    }
                    break;
                case MINUTES:
                    jScale = scale(this.value, 60L, 153722867280912930L);
                    if (jScale <= MIN_SECOND) {
                    }
                    break;
                case SECONDS:
                    jScale = this.value;
                    if (jScale <= MIN_SECOND) {
                    }
                    break;
                case MILLISECONDS:
                    jFloorDiv = Math.floorDiv(this.value, MILLIS_PER_SECOND);
                    iFloorMod = ((int) Math.floorMod(this.value, MILLIS_PER_SECOND)) * NANOS_PER_MILLI;
                    long j = jFloorDiv;
                    i = iFloorMod;
                    jScale = j;
                    if (jScale <= MIN_SECOND) {
                    }
                    break;
                case MICROSECONDS:
                    jFloorDiv = Math.floorDiv(this.value, MICROS_PER_SECOND);
                    iFloorMod = ((int) Math.floorMod(this.value, MICROS_PER_SECOND)) * 1000;
                    long j2 = jFloorDiv;
                    i = iFloorMod;
                    jScale = j2;
                    if (jScale <= MIN_SECOND) {
                    }
                    break;
                case NANOSECONDS:
                    jFloorDiv = Math.floorDiv(this.value, NANOS_PER_SECOND);
                    iFloorMod = (int) Math.floorMod(this.value, NANOS_PER_SECOND);
                    long j22 = jFloorDiv;
                    i = iFloorMod;
                    jScale = j22;
                    if (jScale <= MIN_SECOND) {
                    }
                    break;
                default:
                    throw new AssertionError((Object) "Unit not handled");
            }
        }
        return this.instant;
    }

    public boolean equals(Object obj) {
        return (obj instanceof FileTime) && compareTo((FileTime) obj) == 0;
    }

    public int hashCode() {
        return toInstant().hashCode();
    }

    private long toDays() {
        if (this.unit != null) {
            return this.unit.toDays(this.value);
        }
        return TimeUnit.SECONDS.toDays(toInstant().getEpochSecond());
    }

    private long toExcessNanos(long j) {
        if (this.unit != null) {
            return this.unit.toNanos(this.value - this.unit.convert(j, TimeUnit.DAYS));
        }
        return TimeUnit.SECONDS.toNanos(toInstant().getEpochSecond() - TimeUnit.DAYS.toSeconds(j));
    }

    @Override
    public int compareTo(FileTime fileTime) {
        if (this.unit != null && this.unit == fileTime.unit) {
            return Long.compare(this.value, fileTime.value);
        }
        long epochSecond = toInstant().getEpochSecond();
        int iCompare = Long.compare(epochSecond, fileTime.toInstant().getEpochSecond());
        if (iCompare != 0) {
            return iCompare;
        }
        int iCompare2 = Long.compare(toInstant().getNano(), fileTime.toInstant().getNano());
        if (iCompare2 != 0) {
            return iCompare2;
        }
        if (epochSecond != MAX_SECOND && epochSecond != MIN_SECOND) {
            return 0;
        }
        long days = toDays();
        long days2 = fileTime.toDays();
        if (days == days2) {
            return Long.compare(toExcessNanos(days), fileTime.toExcessNanos(days2));
        }
        return Long.compare(days, days2);
    }

    private StringBuilder append(StringBuilder sb, int i, int i2) {
        while (i > 0) {
            sb.append((char) ((i2 / i) + 48));
            i2 %= i;
            i /= 10;
        }
        return sb;
    }

    public String toString() {
        long epochSecond;
        LocalDateTime localDateTimeOfEpochSecond;
        int year;
        if (this.valueAsString == null) {
            int nano = 0;
            if (this.instant == null && this.unit.compareTo(TimeUnit.SECONDS) >= 0) {
                epochSecond = this.unit.toSeconds(this.value);
            } else {
                epochSecond = toInstant().getEpochSecond();
                nano = toInstant().getNano();
            }
            if (epochSecond >= -62167219200L) {
                long j = (epochSecond - SECONDS_PER_10000_YEARS) + SECONDS_0000_TO_1970;
                long jFloorDiv = Math.floorDiv(j, SECONDS_PER_10000_YEARS) + 1;
                localDateTimeOfEpochSecond = LocalDateTime.ofEpochSecond(Math.floorMod(j, SECONDS_PER_10000_YEARS) - SECONDS_0000_TO_1970, nano, ZoneOffset.UTC);
                year = localDateTimeOfEpochSecond.getYear() + (((int) jFloorDiv) * 10000);
            } else {
                long j2 = epochSecond + SECONDS_0000_TO_1970;
                long j3 = j2 / SECONDS_PER_10000_YEARS;
                localDateTimeOfEpochSecond = LocalDateTime.ofEpochSecond((j2 % SECONDS_PER_10000_YEARS) - SECONDS_0000_TO_1970, nano, ZoneOffset.UTC);
                year = localDateTimeOfEpochSecond.getYear() + (((int) j3) * 10000);
            }
            if (year <= 0) {
                year--;
            }
            int nano2 = localDateTimeOfEpochSecond.getNano();
            StringBuilder sb = new StringBuilder(64);
            sb.append(year < 0 ? LanguageTag.SEP : "");
            int iAbs = Math.abs(year);
            if (iAbs < 10000) {
                append(sb, 1000, Math.abs(iAbs));
            } else {
                sb.append(String.valueOf(iAbs));
            }
            sb.append('-');
            append(sb, 10, localDateTimeOfEpochSecond.getMonthValue());
            sb.append('-');
            append(sb, 10, localDateTimeOfEpochSecond.getDayOfMonth());
            sb.append('T');
            append(sb, 10, localDateTimeOfEpochSecond.getHour());
            sb.append(':');
            append(sb, 10, localDateTimeOfEpochSecond.getMinute());
            sb.append(':');
            append(sb, 10, localDateTimeOfEpochSecond.getSecond());
            if (nano2 != 0) {
                sb.append('.');
                int i = 100000000;
                while (nano2 % 10 == 0) {
                    nano2 /= 10;
                    i /= 10;
                }
                append(sb, i, nano2);
            }
            sb.append('Z');
            this.valueAsString = sb.toString();
        }
        return this.valueAsString;
    }
}
