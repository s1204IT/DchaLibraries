package java.time.chrono;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Period;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class IsoChronology extends AbstractChronology implements Serializable {
    public static final IsoChronology INSTANCE = new IsoChronology();
    private static final long serialVersionUID = -1440403870442975015L;

    @Override
    public ChronoLocalDate resolveDate(Map map, ResolverStyle resolverStyle) {
        return resolveDate((Map<TemporalField, Long>) map, resolverStyle);
    }

    @Override
    ChronoLocalDate resolveYMD(Map map, ResolverStyle resolverStyle) {
        return resolveYMD((Map<TemporalField, Long>) map, resolverStyle);
    }

    @Override
    ChronoLocalDate resolveYearOfEra(Map map, ResolverStyle resolverStyle) {
        return resolveYearOfEra((Map<TemporalField, Long>) map, resolverStyle);
    }

    private IsoChronology() {
    }

    @Override
    public String getId() {
        return "ISO";
    }

    @Override
    public String getCalendarType() {
        return "iso8601";
    }

    @Override
    public LocalDate date(Era era, int i, int i2, int i3) {
        return date(prolepticYear(era, i), i2, i3);
    }

    @Override
    public LocalDate date(int i, int i2, int i3) {
        return LocalDate.of(i, i2, i3);
    }

    @Override
    public LocalDate dateYearDay(Era era, int i, int i2) {
        return dateYearDay(prolepticYear(era, i), i2);
    }

    @Override
    public LocalDate dateYearDay(int i, int i2) {
        return LocalDate.ofYearDay(i, i2);
    }

    @Override
    public LocalDate dateEpochDay(long j) {
        return LocalDate.ofEpochDay(j);
    }

    @Override
    public LocalDate date(TemporalAccessor temporalAccessor) {
        return LocalDate.from(temporalAccessor);
    }

    @Override
    public LocalDateTime localDateTime(TemporalAccessor temporalAccessor) {
        return LocalDateTime.from(temporalAccessor);
    }

    @Override
    public ZonedDateTime zonedDateTime(TemporalAccessor temporalAccessor) {
        return ZonedDateTime.from(temporalAccessor);
    }

    @Override
    public ZonedDateTime zonedDateTime(Instant instant, ZoneId zoneId) {
        return ZonedDateTime.ofInstant(instant, zoneId);
    }

    @Override
    public LocalDate dateNow() {
        return dateNow(Clock.systemDefaultZone());
    }

    @Override
    public LocalDate dateNow(ZoneId zoneId) {
        return dateNow(Clock.system(zoneId));
    }

    @Override
    public LocalDate dateNow(Clock clock) {
        Objects.requireNonNull(clock, "clock");
        return date((TemporalAccessor) LocalDate.now(clock));
    }

    @Override
    public boolean isLeapYear(long j) {
        return (3 & j) == 0 && (j % 100 != 0 || j % 400 == 0);
    }

    @Override
    public int prolepticYear(Era era, int i) {
        if (era instanceof IsoEra) {
            return era == IsoEra.CE ? i : 1 - i;
        }
        throw new ClassCastException("Era must be IsoEra");
    }

    @Override
    public IsoEra eraOf(int i) {
        return IsoEra.of(i);
    }

    @Override
    public List<Era> eras() {
        return Arrays.asList(IsoEra.values());
    }

    @Override
    public LocalDate resolveDate(Map<TemporalField, Long> map, ResolverStyle resolverStyle) {
        return (LocalDate) super.resolveDate(map, resolverStyle);
    }

    @Override
    void resolveProlepticMonth(Map<TemporalField, Long> map, ResolverStyle resolverStyle) {
        Long lRemove = map.remove(ChronoField.PROLEPTIC_MONTH);
        if (lRemove != null) {
            if (resolverStyle != ResolverStyle.LENIENT) {
                ChronoField.PROLEPTIC_MONTH.checkValidValue(lRemove.longValue());
            }
            addFieldValue(map, ChronoField.MONTH_OF_YEAR, Math.floorMod(lRemove.longValue(), 12L) + 1);
            addFieldValue(map, ChronoField.YEAR, Math.floorDiv(lRemove.longValue(), 12L));
        }
    }

    @Override
    LocalDate resolveYearOfEra(Map<TemporalField, Long> map, ResolverStyle resolverStyle) {
        Long lRemove = map.remove(ChronoField.YEAR_OF_ERA);
        if (lRemove != null) {
            if (resolverStyle != ResolverStyle.LENIENT) {
                ChronoField.YEAR_OF_ERA.checkValidValue(lRemove.longValue());
            }
            Long lRemove2 = map.remove(ChronoField.ERA);
            if (lRemove2 == null) {
                Long l = map.get(ChronoField.YEAR);
                if (resolverStyle == ResolverStyle.STRICT) {
                    if (l != null) {
                        addFieldValue(map, ChronoField.YEAR, l.longValue() > 0 ? lRemove.longValue() : Math.subtractExact(1L, lRemove.longValue()));
                        return null;
                    }
                    map.put(ChronoField.YEAR_OF_ERA, lRemove);
                    return null;
                }
                addFieldValue(map, ChronoField.YEAR, (l == null || l.longValue() > 0) ? lRemove.longValue() : Math.subtractExact(1L, lRemove.longValue()));
                return null;
            }
            if (lRemove2.longValue() == 1) {
                addFieldValue(map, ChronoField.YEAR, lRemove.longValue());
                return null;
            }
            if (lRemove2.longValue() == 0) {
                addFieldValue(map, ChronoField.YEAR, Math.subtractExact(1L, lRemove.longValue()));
                return null;
            }
            throw new DateTimeException("Invalid value for era: " + ((Object) lRemove2));
        }
        if (map.containsKey(ChronoField.ERA)) {
            ChronoField.ERA.checkValidValue(map.get(ChronoField.ERA).longValue());
            return null;
        }
        return null;
    }

    @Override
    LocalDate resolveYMD(Map<TemporalField, Long> map, ResolverStyle resolverStyle) {
        int iCheckValidIntValue = ChronoField.YEAR.checkValidIntValue(map.remove(ChronoField.YEAR).longValue());
        if (resolverStyle == ResolverStyle.LENIENT) {
            return LocalDate.of(iCheckValidIntValue, 1, 1).plusMonths(Math.subtractExact(map.remove(ChronoField.MONTH_OF_YEAR).longValue(), 1L)).plusDays(Math.subtractExact(map.remove(ChronoField.DAY_OF_MONTH).longValue(), 1L));
        }
        int iCheckValidIntValue2 = ChronoField.MONTH_OF_YEAR.checkValidIntValue(map.remove(ChronoField.MONTH_OF_YEAR).longValue());
        int iCheckValidIntValue3 = ChronoField.DAY_OF_MONTH.checkValidIntValue(map.remove(ChronoField.DAY_OF_MONTH).longValue());
        if (resolverStyle == ResolverStyle.SMART) {
            if (iCheckValidIntValue2 == 4 || iCheckValidIntValue2 == 6 || iCheckValidIntValue2 == 9 || iCheckValidIntValue2 == 11) {
                iCheckValidIntValue3 = Math.min(iCheckValidIntValue3, 30);
            } else if (iCheckValidIntValue2 == 2) {
                iCheckValidIntValue3 = Math.min(iCheckValidIntValue3, Month.FEBRUARY.length(Year.isLeap(iCheckValidIntValue)));
            }
        }
        return LocalDate.of(iCheckValidIntValue, iCheckValidIntValue2, iCheckValidIntValue3);
    }

    @Override
    public ValueRange range(ChronoField chronoField) {
        return chronoField.range();
    }

    @Override
    public Period period(int i, int i2, int i3) {
        return Period.of(i, i2, i3);
    }

    @Override
    Object writeReplace() {
        return super.writeReplace();
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }
}
