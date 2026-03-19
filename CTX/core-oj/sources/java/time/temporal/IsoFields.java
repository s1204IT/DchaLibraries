package java.time.temporal;

import android.icu.text.DateTimePatternGenerator;
import android.icu.util.ULocale;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class IsoFields {
    public static final TemporalField DAY_OF_QUARTER = Field.DAY_OF_QUARTER;
    public static final TemporalField QUARTER_OF_YEAR = Field.QUARTER_OF_YEAR;
    public static final TemporalField WEEK_OF_WEEK_BASED_YEAR = Field.WEEK_OF_WEEK_BASED_YEAR;
    public static final TemporalField WEEK_BASED_YEAR = Field.WEEK_BASED_YEAR;
    public static final TemporalUnit WEEK_BASED_YEARS = Unit.WEEK_BASED_YEARS;
    public static final TemporalUnit QUARTER_YEARS = Unit.QUARTER_YEARS;

    private IsoFields() {
        throw new AssertionError((Object) "Not instantiable");
    }

    private enum Field implements TemporalField {
        DAY_OF_QUARTER {
            @Override
            public TemporalAccessor resolve(Map map, TemporalAccessor temporalAccessor, ResolverStyle resolverStyle) {
                return resolve((Map<TemporalField, Long>) map, temporalAccessor, resolverStyle);
            }

            @Override
            public TemporalUnit getBaseUnit() {
                return ChronoUnit.DAYS;
            }

            @Override
            public TemporalUnit getRangeUnit() {
                return IsoFields.QUARTER_YEARS;
            }

            @Override
            public ValueRange range() {
                return ValueRange.of(1L, 90L, 92L);
            }

            @Override
            public boolean isSupportedBy(TemporalAccessor temporalAccessor) {
                return temporalAccessor.isSupported(ChronoField.DAY_OF_YEAR) && temporalAccessor.isSupported(ChronoField.MONTH_OF_YEAR) && temporalAccessor.isSupported(ChronoField.YEAR) && Field.isIso(temporalAccessor);
            }

            @Override
            public ValueRange rangeRefinedBy(TemporalAccessor temporalAccessor) {
                if (!isSupportedBy(temporalAccessor)) {
                    throw new UnsupportedTemporalTypeException("Unsupported field: DayOfQuarter");
                }
                long j = temporalAccessor.getLong(QUARTER_OF_YEAR);
                if (j == 1) {
                    return IsoChronology.INSTANCE.isLeapYear(temporalAccessor.getLong(ChronoField.YEAR)) ? ValueRange.of(1L, 91L) : ValueRange.of(1L, 90L);
                }
                if (j == 2) {
                    return ValueRange.of(1L, 91L);
                }
                if (j == 3 || j == 4) {
                    return ValueRange.of(1L, 92L);
                }
                return range();
            }

            @Override
            public long getFrom(TemporalAccessor temporalAccessor) {
                if (!isSupportedBy(temporalAccessor)) {
                    throw new UnsupportedTemporalTypeException("Unsupported field: DayOfQuarter");
                }
                return temporalAccessor.get(ChronoField.DAY_OF_YEAR) - Field.QUARTER_DAYS[((temporalAccessor.get(ChronoField.MONTH_OF_YEAR) - 1) / 3) + (IsoChronology.INSTANCE.isLeapYear(temporalAccessor.getLong(ChronoField.YEAR)) ? 4 : 0)];
            }

            @Override
            public <R extends Temporal> R adjustInto(R r, long j) {
                long from = getFrom(r);
                range().checkValidValue(j, this);
                return (R) r.with(ChronoField.DAY_OF_YEAR, r.getLong(ChronoField.DAY_OF_YEAR) + (j - from));
            }

            @Override
            public ChronoLocalDate resolve(Map<TemporalField, Long> map, TemporalAccessor temporalAccessor, ResolverStyle resolverStyle) {
                LocalDate localDateOf;
                long jSubtractExact;
                Long l = map.get(ChronoField.YEAR);
                Long l2 = map.get(QUARTER_OF_YEAR);
                if (l == null || l2 == null) {
                    return null;
                }
                int iCheckValidIntValue = ChronoField.YEAR.checkValidIntValue(l.longValue());
                long jLongValue = map.get(DAY_OF_QUARTER).longValue();
                Field.ensureIso(temporalAccessor);
                if (resolverStyle == ResolverStyle.LENIENT) {
                    localDateOf = LocalDate.of(iCheckValidIntValue, 1, 1).plusMonths(Math.multiplyExact(Math.subtractExact(l2.longValue(), 1L), 3L));
                    jSubtractExact = Math.subtractExact(jLongValue, 1L);
                } else {
                    localDateOf = LocalDate.of(iCheckValidIntValue, ((QUARTER_OF_YEAR.range().checkValidIntValue(l2.longValue(), QUARTER_OF_YEAR) - 1) * 3) + 1, 1);
                    if (jLongValue < 1 || jLongValue > 90) {
                        if (resolverStyle == ResolverStyle.STRICT) {
                            rangeRefinedBy(localDateOf).checkValidValue(jLongValue, this);
                        } else {
                            range().checkValidValue(jLongValue, this);
                        }
                    }
                    jSubtractExact = jLongValue - 1;
                }
                map.remove(this);
                map.remove(ChronoField.YEAR);
                map.remove(QUARTER_OF_YEAR);
                return localDateOf.plusDays(jSubtractExact);
            }

            @Override
            public String toString() {
                return "DayOfQuarter";
            }
        },
        QUARTER_OF_YEAR {
            @Override
            public TemporalUnit getBaseUnit() {
                return IsoFields.QUARTER_YEARS;
            }

            @Override
            public TemporalUnit getRangeUnit() {
                return ChronoUnit.YEARS;
            }

            @Override
            public ValueRange range() {
                return ValueRange.of(1L, 4L);
            }

            @Override
            public boolean isSupportedBy(TemporalAccessor temporalAccessor) {
                return temporalAccessor.isSupported(ChronoField.MONTH_OF_YEAR) && Field.isIso(temporalAccessor);
            }

            @Override
            public long getFrom(TemporalAccessor temporalAccessor) {
                if (!isSupportedBy(temporalAccessor)) {
                    throw new UnsupportedTemporalTypeException("Unsupported field: QuarterOfYear");
                }
                return (temporalAccessor.getLong(ChronoField.MONTH_OF_YEAR) + 2) / 3;
            }

            @Override
            public <R extends Temporal> R adjustInto(R r, long j) {
                long from = getFrom(r);
                range().checkValidValue(j, this);
                return (R) r.with(ChronoField.MONTH_OF_YEAR, r.getLong(ChronoField.MONTH_OF_YEAR) + ((j - from) * 3));
            }

            @Override
            public String toString() {
                return "QuarterOfYear";
            }
        },
        WEEK_OF_WEEK_BASED_YEAR {
            @Override
            public TemporalAccessor resolve(Map map, TemporalAccessor temporalAccessor, ResolverStyle resolverStyle) {
                return resolve((Map<TemporalField, Long>) map, temporalAccessor, resolverStyle);
            }

            @Override
            public String getDisplayName(Locale locale) {
                Objects.requireNonNull(locale, "locale");
                String appendItemName = DateTimePatternGenerator.getFrozenInstance(ULocale.forLocale(locale)).getAppendItemName(4);
                return (appendItemName == null || appendItemName.isEmpty()) ? toString() : appendItemName;
            }

            @Override
            public TemporalUnit getBaseUnit() {
                return ChronoUnit.WEEKS;
            }

            @Override
            public TemporalUnit getRangeUnit() {
                return IsoFields.WEEK_BASED_YEARS;
            }

            @Override
            public ValueRange range() {
                return ValueRange.of(1L, 52L, 53L);
            }

            @Override
            public boolean isSupportedBy(TemporalAccessor temporalAccessor) {
                return temporalAccessor.isSupported(ChronoField.EPOCH_DAY) && Field.isIso(temporalAccessor);
            }

            @Override
            public ValueRange rangeRefinedBy(TemporalAccessor temporalAccessor) {
                if (isSupportedBy(temporalAccessor)) {
                    return Field.getWeekRange(LocalDate.from(temporalAccessor));
                }
                throw new UnsupportedTemporalTypeException("Unsupported field: WeekOfWeekBasedYear");
            }

            @Override
            public long getFrom(TemporalAccessor temporalAccessor) {
                if (isSupportedBy(temporalAccessor)) {
                    return Field.getWeek(LocalDate.from(temporalAccessor));
                }
                throw new UnsupportedTemporalTypeException("Unsupported field: WeekOfWeekBasedYear");
            }

            @Override
            public <R extends Temporal> R adjustInto(R r, long j) {
                range().checkValidValue(j, this);
                return (R) r.plus(Math.subtractExact(j, getFrom(r)), ChronoUnit.WEEKS);
            }

            @Override
            public ChronoLocalDate resolve(Map<TemporalField, Long> map, TemporalAccessor temporalAccessor, ResolverStyle resolverStyle) {
                LocalDate localDateWith;
                Long l = map.get(WEEK_BASED_YEAR);
                Long l2 = map.get(ChronoField.DAY_OF_WEEK);
                if (l == null || l2 == null) {
                    return null;
                }
                int iCheckValidIntValue = WEEK_BASED_YEAR.range().checkValidIntValue(l.longValue(), WEEK_BASED_YEAR);
                long jLongValue = map.get(WEEK_OF_WEEK_BASED_YEAR).longValue();
                Field.ensureIso(temporalAccessor);
                LocalDate localDateOf = LocalDate.of(iCheckValidIntValue, 1, 4);
                if (resolverStyle == ResolverStyle.LENIENT) {
                    long jLongValue2 = l2.longValue();
                    if (jLongValue2 > 7) {
                        long j = jLongValue2 - 1;
                        localDateOf = localDateOf.plusWeeks(j / 7);
                        jLongValue2 = (j % 7) + 1;
                    } else if (jLongValue2 < 1) {
                        localDateOf = localDateOf.plusWeeks(Math.subtractExact(jLongValue2, 7L) / 7);
                        jLongValue2 = ((jLongValue2 + 6) % 7) + 1;
                    }
                    localDateWith = localDateOf.plusWeeks(Math.subtractExact(jLongValue, 1L)).with((TemporalField) ChronoField.DAY_OF_WEEK, jLongValue2);
                } else {
                    int iCheckValidIntValue2 = ChronoField.DAY_OF_WEEK.checkValidIntValue(l2.longValue());
                    if (jLongValue < 1 || jLongValue > 52) {
                        if (resolverStyle == ResolverStyle.STRICT) {
                            Field.getWeekRange(localDateOf).checkValidValue(jLongValue, this);
                        } else {
                            range().checkValidValue(jLongValue, this);
                        }
                    }
                    localDateWith = localDateOf.plusWeeks(jLongValue - 1).with((TemporalField) ChronoField.DAY_OF_WEEK, iCheckValidIntValue2);
                }
                map.remove(this);
                map.remove(WEEK_BASED_YEAR);
                map.remove(ChronoField.DAY_OF_WEEK);
                return localDateWith;
            }

            @Override
            public String toString() {
                return "WeekOfWeekBasedYear";
            }
        },
        WEEK_BASED_YEAR {
            @Override
            public TemporalUnit getBaseUnit() {
                return IsoFields.WEEK_BASED_YEARS;
            }

            @Override
            public TemporalUnit getRangeUnit() {
                return ChronoUnit.FOREVER;
            }

            @Override
            public ValueRange range() {
                return ChronoField.YEAR.range();
            }

            @Override
            public boolean isSupportedBy(TemporalAccessor temporalAccessor) {
                return temporalAccessor.isSupported(ChronoField.EPOCH_DAY) && Field.isIso(temporalAccessor);
            }

            @Override
            public long getFrom(TemporalAccessor temporalAccessor) {
                if (isSupportedBy(temporalAccessor)) {
                    return Field.getWeekBasedYear(LocalDate.from(temporalAccessor));
                }
                throw new UnsupportedTemporalTypeException("Unsupported field: WeekBasedYear");
            }

            @Override
            public <R extends Temporal> R adjustInto(R r, long j) {
                if (!isSupportedBy(r)) {
                    throw new UnsupportedTemporalTypeException("Unsupported field: WeekBasedYear");
                }
                int iCheckValidIntValue = range().checkValidIntValue(j, WEEK_BASED_YEAR);
                LocalDate localDateFrom = LocalDate.from((TemporalAccessor) r);
                int i = localDateFrom.get(ChronoField.DAY_OF_WEEK);
                int week = Field.getWeek(localDateFrom);
                if (week == 53 && Field.getWeekRange(iCheckValidIntValue) == 52) {
                    week = 52;
                }
                return (R) r.with(LocalDate.of(iCheckValidIntValue, 1, 4).plusDays((i - r5.get(ChronoField.DAY_OF_WEEK)) + ((week - 1) * 7)));
            }

            @Override
            public String toString() {
                return "WeekBasedYear";
            }
        };

        private static final int[] QUARTER_DAYS = {0, 90, 181, 273, 0, 91, 182, 274};

        @Override
        public boolean isDateBased() {
            return true;
        }

        @Override
        public boolean isTimeBased() {
            return false;
        }

        @Override
        public ValueRange rangeRefinedBy(TemporalAccessor temporalAccessor) {
            return range();
        }

        private static boolean isIso(TemporalAccessor temporalAccessor) {
            return Chronology.from(temporalAccessor).equals(IsoChronology.INSTANCE);
        }

        private static void ensureIso(TemporalAccessor temporalAccessor) {
            if (!isIso(temporalAccessor)) {
                throw new DateTimeException("Resolve requires IsoChronology");
            }
        }

        private static ValueRange getWeekRange(LocalDate localDate) {
            return ValueRange.of(1L, getWeekRange(getWeekBasedYear(localDate)));
        }

        private static int getWeekRange(int i) {
            LocalDate localDateOf = LocalDate.of(i, 1, 1);
            if (localDateOf.getDayOfWeek() == DayOfWeek.THURSDAY) {
                return 53;
            }
            if (localDateOf.getDayOfWeek() == DayOfWeek.WEDNESDAY && localDateOf.isLeapYear()) {
                return 53;
            }
            return 52;
        }

        private static int getWeek(LocalDate localDate) {
            int iOrdinal = localDate.getDayOfWeek().ordinal();
            int dayOfYear = localDate.getDayOfYear() - 1;
            int i = (3 - iOrdinal) + dayOfYear;
            int i2 = (i - ((i / 7) * 7)) - 3;
            if (i2 < -3) {
                i2 += 7;
            }
            if (dayOfYear < i2) {
                return (int) getWeekRange(localDate.withDayOfYear(180).minusYears(1L)).getMaximum();
            }
            int i3 = ((dayOfYear - i2) / 7) + 1;
            if (i3 != 53) {
                return i3;
            }
            if (i2 == -3 || (i2 == -2 && localDate.isLeapYear())) {
                return i3;
            }
            return 1;
        }

        private static int getWeekBasedYear(LocalDate localDate) {
            int year = localDate.getYear();
            int dayOfYear = localDate.getDayOfYear();
            if (dayOfYear <= 3) {
                if (dayOfYear - localDate.getDayOfWeek().ordinal() < -2) {
                    return year - 1;
                }
                return year;
            }
            if (dayOfYear >= 363) {
                if (((dayOfYear - 363) - (localDate.isLeapYear() ? 1 : 0)) - localDate.getDayOfWeek().ordinal() >= 0) {
                    return year + 1;
                }
                return year;
            }
            return year;
        }
    }

    private enum Unit implements TemporalUnit {
        WEEK_BASED_YEARS("WeekBasedYears", Duration.ofSeconds(31556952)),
        QUARTER_YEARS("QuarterYears", Duration.ofSeconds(7889238));

        private final Duration duration;
        private final String name;

        Unit(String str, Duration duration) {
            this.name = str;
            this.duration = duration;
        }

        @Override
        public Duration getDuration() {
            return this.duration;
        }

        @Override
        public boolean isDurationEstimated() {
            return true;
        }

        @Override
        public boolean isDateBased() {
            return true;
        }

        @Override
        public boolean isTimeBased() {
            return false;
        }

        @Override
        public boolean isSupportedBy(Temporal temporal) {
            return temporal.isSupported(ChronoField.EPOCH_DAY);
        }

        @Override
        public <R extends Temporal> R addTo(R r, long j) {
            switch (this) {
                case WEEK_BASED_YEARS:
                    return (R) r.with(IsoFields.WEEK_BASED_YEAR, Math.addExact(r.get(IsoFields.WEEK_BASED_YEAR), j));
                case QUARTER_YEARS:
                    return (R) r.plus(j / 256, ChronoUnit.YEARS).plus((j % 256) * 3, ChronoUnit.MONTHS);
                default:
                    throw new IllegalStateException("Unreachable");
            }
        }

        @Override
        public long between(Temporal temporal, Temporal temporal2) {
            if (temporal.getClass() != temporal2.getClass()) {
                return temporal.until(temporal2, this);
            }
            switch (this) {
                case WEEK_BASED_YEARS:
                    return Math.subtractExact(temporal2.getLong(IsoFields.WEEK_BASED_YEAR), temporal.getLong(IsoFields.WEEK_BASED_YEAR));
                case QUARTER_YEARS:
                    return temporal.until(temporal2, ChronoUnit.MONTHS) / 3;
                default:
                    throw new IllegalStateException("Unreachable");
            }
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
