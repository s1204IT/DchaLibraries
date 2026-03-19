package java.time.temporal;

import android.icu.text.DateTimePatternGenerator;
import android.icu.util.Calendar;
import android.icu.util.ULocale;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class WeekFields implements Serializable {
    private static final ConcurrentMap<String, WeekFields> CACHE = new ConcurrentHashMap(4, 0.75f, 2);
    public static final WeekFields ISO = new WeekFields(DayOfWeek.MONDAY, 4);
    public static final WeekFields SUNDAY_START = of(DayOfWeek.SUNDAY, 1);
    public static final TemporalUnit WEEK_BASED_YEARS = IsoFields.WEEK_BASED_YEARS;
    private static final long serialVersionUID = -1177360819670808121L;
    private final DayOfWeek firstDayOfWeek;
    private final int minimalDays;
    private final transient TemporalField dayOfWeek = ComputedDayOfField.ofDayOfWeekField(this);
    private final transient TemporalField weekOfMonth = ComputedDayOfField.ofWeekOfMonthField(this);
    private final transient TemporalField weekOfYear = ComputedDayOfField.ofWeekOfYearField(this);
    private final transient TemporalField weekOfWeekBasedYear = ComputedDayOfField.ofWeekOfWeekBasedYearField(this);
    private final transient TemporalField weekBasedYear = ComputedDayOfField.ofWeekBasedYearField(this);

    public static WeekFields of(Locale locale) {
        Objects.requireNonNull(locale, "locale");
        return of(DayOfWeek.SUNDAY.plus(r4.firstDayOfWeek - 1), Calendar.getWeekDataForRegion(ULocale.getRegionForSupplementalData(ULocale.forLocale(locale), true)).minimalDaysInFirstWeek);
    }

    public static WeekFields of(DayOfWeek dayOfWeek, int i) {
        String str = dayOfWeek.toString() + i;
        WeekFields weekFields = CACHE.get(str);
        if (weekFields == null) {
            CACHE.putIfAbsent(str, new WeekFields(dayOfWeek, i));
            return CACHE.get(str);
        }
        return weekFields;
    }

    private WeekFields(DayOfWeek dayOfWeek, int i) {
        Objects.requireNonNull(dayOfWeek, "firstDayOfWeek");
        if (i < 1 || i > 7) {
            throw new IllegalArgumentException("Minimal number of days is invalid");
        }
        this.firstDayOfWeek = dayOfWeek;
        this.minimalDays = i;
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        if (this.firstDayOfWeek == null) {
            throw new InvalidObjectException("firstDayOfWeek is null");
        }
        if (this.minimalDays < 1 || this.minimalDays > 7) {
            throw new InvalidObjectException("Minimal number of days is invalid");
        }
    }

    private Object readResolve() throws InvalidObjectException {
        try {
            return of(this.firstDayOfWeek, this.minimalDays);
        } catch (IllegalArgumentException e) {
            throw new InvalidObjectException("Invalid serialized WeekFields: " + e.getMessage());
        }
    }

    public DayOfWeek getFirstDayOfWeek() {
        return this.firstDayOfWeek;
    }

    public int getMinimalDaysInFirstWeek() {
        return this.minimalDays;
    }

    public TemporalField dayOfWeek() {
        return this.dayOfWeek;
    }

    public TemporalField weekOfMonth() {
        return this.weekOfMonth;
    }

    public TemporalField weekOfYear() {
        return this.weekOfYear;
    }

    public TemporalField weekOfWeekBasedYear() {
        return this.weekOfWeekBasedYear;
    }

    public TemporalField weekBasedYear() {
        return this.weekBasedYear;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return (obj instanceof WeekFields) && hashCode() == obj.hashCode();
    }

    public int hashCode() {
        return (this.firstDayOfWeek.ordinal() * 7) + this.minimalDays;
    }

    public String toString() {
        return "WeekFields[" + ((Object) this.firstDayOfWeek) + ',' + this.minimalDays + ']';
    }

    static class ComputedDayOfField implements TemporalField {
        private final TemporalUnit baseUnit;
        private final String name;
        private final ValueRange range;
        private final TemporalUnit rangeUnit;
        private final WeekFields weekDef;
        private static final ValueRange DAY_OF_WEEK_RANGE = ValueRange.of(1, 7);
        private static final ValueRange WEEK_OF_MONTH_RANGE = ValueRange.of(0, 1, 4, 6);
        private static final ValueRange WEEK_OF_YEAR_RANGE = ValueRange.of(0, 1, 52, 54);
        private static final ValueRange WEEK_OF_WEEK_BASED_YEAR_RANGE = ValueRange.of(1, 52, 53);

        @Override
        public TemporalAccessor resolve(Map map, TemporalAccessor temporalAccessor, ResolverStyle resolverStyle) {
            return resolve((Map<TemporalField, Long>) map, temporalAccessor, resolverStyle);
        }

        static ComputedDayOfField ofDayOfWeekField(WeekFields weekFields) {
            return new ComputedDayOfField("DayOfWeek", weekFields, ChronoUnit.DAYS, ChronoUnit.WEEKS, DAY_OF_WEEK_RANGE);
        }

        static ComputedDayOfField ofWeekOfMonthField(WeekFields weekFields) {
            return new ComputedDayOfField("WeekOfMonth", weekFields, ChronoUnit.WEEKS, ChronoUnit.MONTHS, WEEK_OF_MONTH_RANGE);
        }

        static ComputedDayOfField ofWeekOfYearField(WeekFields weekFields) {
            return new ComputedDayOfField("WeekOfYear", weekFields, ChronoUnit.WEEKS, ChronoUnit.YEARS, WEEK_OF_YEAR_RANGE);
        }

        static ComputedDayOfField ofWeekOfWeekBasedYearField(WeekFields weekFields) {
            return new ComputedDayOfField("WeekOfWeekBasedYear", weekFields, ChronoUnit.WEEKS, IsoFields.WEEK_BASED_YEARS, WEEK_OF_WEEK_BASED_YEAR_RANGE);
        }

        static ComputedDayOfField ofWeekBasedYearField(WeekFields weekFields) {
            return new ComputedDayOfField("WeekBasedYear", weekFields, IsoFields.WEEK_BASED_YEARS, ChronoUnit.FOREVER, ChronoField.YEAR.range());
        }

        private ChronoLocalDate ofWeekBasedYear(Chronology chronology, int i, int i2, int i3) {
            ChronoLocalDate chronoLocalDateDate = chronology.date(i, 1, 1);
            int iStartOfWeekOffset = startOfWeekOffset(1, localizedDayOfWeek(chronoLocalDateDate));
            return chronoLocalDateDate.plus((-iStartOfWeekOffset) + (i3 - 1) + ((Math.min(i2, computeWeek(iStartOfWeekOffset, chronoLocalDateDate.lengthOfYear() + this.weekDef.getMinimalDaysInFirstWeek()) - 1) - 1) * 7), (TemporalUnit) ChronoUnit.DAYS);
        }

        private ComputedDayOfField(String str, WeekFields weekFields, TemporalUnit temporalUnit, TemporalUnit temporalUnit2, ValueRange valueRange) {
            this.name = str;
            this.weekDef = weekFields;
            this.baseUnit = temporalUnit;
            this.rangeUnit = temporalUnit2;
            this.range = valueRange;
        }

        @Override
        public long getFrom(TemporalAccessor temporalAccessor) {
            if (this.rangeUnit == ChronoUnit.WEEKS) {
                return localizedDayOfWeek(temporalAccessor);
            }
            if (this.rangeUnit == ChronoUnit.MONTHS) {
                return localizedWeekOfMonth(temporalAccessor);
            }
            if (this.rangeUnit == ChronoUnit.YEARS) {
                return localizedWeekOfYear(temporalAccessor);
            }
            if (this.rangeUnit == WeekFields.WEEK_BASED_YEARS) {
                return localizedWeekOfWeekBasedYear(temporalAccessor);
            }
            if (this.rangeUnit == ChronoUnit.FOREVER) {
                return localizedWeekBasedYear(temporalAccessor);
            }
            throw new IllegalStateException("unreachable, rangeUnit: " + ((Object) this.rangeUnit) + ", this: " + ((Object) this));
        }

        private int localizedDayOfWeek(TemporalAccessor temporalAccessor) {
            return Math.floorMod(temporalAccessor.get(ChronoField.DAY_OF_WEEK) - this.weekDef.getFirstDayOfWeek().getValue(), 7) + 1;
        }

        private int localizedDayOfWeek(int i) {
            return Math.floorMod(i - this.weekDef.getFirstDayOfWeek().getValue(), 7) + 1;
        }

        private long localizedWeekOfMonth(TemporalAccessor temporalAccessor) {
            int iLocalizedDayOfWeek = localizedDayOfWeek(temporalAccessor);
            int i = temporalAccessor.get(ChronoField.DAY_OF_MONTH);
            return computeWeek(startOfWeekOffset(i, iLocalizedDayOfWeek), i);
        }

        private long localizedWeekOfYear(TemporalAccessor temporalAccessor) {
            int iLocalizedDayOfWeek = localizedDayOfWeek(temporalAccessor);
            int i = temporalAccessor.get(ChronoField.DAY_OF_YEAR);
            return computeWeek(startOfWeekOffset(i, iLocalizedDayOfWeek), i);
        }

        private int localizedWeekBasedYear(TemporalAccessor temporalAccessor) {
            int iLocalizedDayOfWeek = localizedDayOfWeek(temporalAccessor);
            int i = temporalAccessor.get(ChronoField.YEAR);
            int i2 = temporalAccessor.get(ChronoField.DAY_OF_YEAR);
            int iStartOfWeekOffset = startOfWeekOffset(i2, iLocalizedDayOfWeek);
            int iComputeWeek = computeWeek(iStartOfWeekOffset, i2);
            if (iComputeWeek == 0) {
                return i - 1;
            }
            if (iComputeWeek >= computeWeek(iStartOfWeekOffset, ((int) temporalAccessor.range(ChronoField.DAY_OF_YEAR).getMaximum()) + this.weekDef.getMinimalDaysInFirstWeek())) {
                return i + 1;
            }
            return i;
        }

        private int localizedWeekOfWeekBasedYear(TemporalAccessor temporalAccessor) {
            int iComputeWeek;
            int iLocalizedDayOfWeek = localizedDayOfWeek(temporalAccessor);
            int i = temporalAccessor.get(ChronoField.DAY_OF_YEAR);
            int iStartOfWeekOffset = startOfWeekOffset(i, iLocalizedDayOfWeek);
            int iComputeWeek2 = computeWeek(iStartOfWeekOffset, i);
            if (iComputeWeek2 == 0) {
                return localizedWeekOfWeekBasedYear(Chronology.from(temporalAccessor).date(temporalAccessor).minus(i, (TemporalUnit) ChronoUnit.DAYS));
            }
            if (iComputeWeek2 > 50 && iComputeWeek2 >= (iComputeWeek = computeWeek(iStartOfWeekOffset, ((int) temporalAccessor.range(ChronoField.DAY_OF_YEAR).getMaximum()) + this.weekDef.getMinimalDaysInFirstWeek()))) {
                return (iComputeWeek2 - iComputeWeek) + 1;
            }
            return iComputeWeek2;
        }

        private int startOfWeekOffset(int i, int i2) {
            int iFloorMod = Math.floorMod(i - i2, 7);
            int i3 = -iFloorMod;
            if (iFloorMod + 1 > this.weekDef.getMinimalDaysInFirstWeek()) {
                return 7 - iFloorMod;
            }
            return i3;
        }

        private int computeWeek(int i, int i2) {
            return ((i + 7) + (i2 - 1)) / 7;
        }

        @Override
        public <R extends Temporal> R adjustInto(R r, long j) {
            if (this.range.checkValidIntValue(j, this) == r.get(this)) {
                return r;
            }
            if (this.rangeUnit == ChronoUnit.FOREVER) {
                return ofWeekBasedYear(Chronology.from(r), (int) j, r.get(this.weekDef.weekOfWeekBasedYear), r.get(this.weekDef.dayOfWeek));
            }
            return (R) r.plus(r0 - r1, this.baseUnit);
        }

        @Override
        public ChronoLocalDate resolve(Map<TemporalField, Long> map, TemporalAccessor temporalAccessor, ResolverStyle resolverStyle) {
            int intExact = Math.toIntExact(map.get(this).longValue());
            if (this.rangeUnit == ChronoUnit.WEEKS) {
                long jFloorMod = Math.floorMod((this.weekDef.getFirstDayOfWeek().getValue() - 1) + (this.range.checkValidIntValue(r2, this) - 1), 7) + 1;
                map.remove(this);
                map.put(ChronoField.DAY_OF_WEEK, Long.valueOf(jFloorMod));
                return null;
            }
            if (!map.containsKey(ChronoField.DAY_OF_WEEK)) {
                return null;
            }
            int iLocalizedDayOfWeek = localizedDayOfWeek(ChronoField.DAY_OF_WEEK.checkValidIntValue(map.get(ChronoField.DAY_OF_WEEK).longValue()));
            Chronology chronologyFrom = Chronology.from(temporalAccessor);
            if (!map.containsKey(ChronoField.YEAR)) {
                if ((this.rangeUnit == WeekFields.WEEK_BASED_YEARS || this.rangeUnit == ChronoUnit.FOREVER) && map.containsKey(this.weekDef.weekBasedYear) && map.containsKey(this.weekDef.weekOfWeekBasedYear)) {
                    return resolveWBY(map, chronologyFrom, iLocalizedDayOfWeek, resolverStyle);
                }
            } else {
                int iCheckValidIntValue = ChronoField.YEAR.checkValidIntValue(map.get(ChronoField.YEAR).longValue());
                if (this.rangeUnit == ChronoUnit.MONTHS && map.containsKey(ChronoField.MONTH_OF_YEAR)) {
                    return resolveWoM(map, chronologyFrom, iCheckValidIntValue, map.get(ChronoField.MONTH_OF_YEAR).longValue(), intExact, iLocalizedDayOfWeek, resolverStyle);
                }
                if (this.rangeUnit == ChronoUnit.YEARS) {
                    return resolveWoY(map, chronologyFrom, iCheckValidIntValue, intExact, iLocalizedDayOfWeek, resolverStyle);
                }
            }
            return null;
        }

        private ChronoLocalDate resolveWoM(Map<TemporalField, Long> map, Chronology chronology, int i, long j, long j2, int i2, ResolverStyle resolverStyle) {
            ChronoLocalDate chronoLocalDatePlus;
            if (resolverStyle == ResolverStyle.LENIENT) {
                ChronoLocalDate chronoLocalDatePlus2 = chronology.date(i, 1, 1).plus(Math.subtractExact(j, 1L), (TemporalUnit) ChronoUnit.MONTHS);
                chronoLocalDatePlus = chronoLocalDatePlus2.plus(Math.addExact(Math.multiplyExact(Math.subtractExact(j2, localizedWeekOfMonth(chronoLocalDatePlus2)), 7L), i2 - localizedDayOfWeek(chronoLocalDatePlus2)), (TemporalUnit) ChronoUnit.DAYS);
            } else {
                chronoLocalDatePlus = chronology.date(i, ChronoField.MONTH_OF_YEAR.checkValidIntValue(j), 1).plus((((int) (((long) this.range.checkValidIntValue(j2, this)) - localizedWeekOfMonth(r4))) * 7) + (i2 - localizedDayOfWeek(r4)), (TemporalUnit) ChronoUnit.DAYS);
                if (resolverStyle == ResolverStyle.STRICT && chronoLocalDatePlus.getLong(ChronoField.MONTH_OF_YEAR) != j) {
                    throw new DateTimeException("Strict mode rejected resolved date as it is in a different month");
                }
            }
            map.remove(this);
            map.remove(ChronoField.YEAR);
            map.remove(ChronoField.MONTH_OF_YEAR);
            map.remove(ChronoField.DAY_OF_WEEK);
            return chronoLocalDatePlus;
        }

        private ChronoLocalDate resolveWoY(Map<TemporalField, Long> map, Chronology chronology, int i, long j, int i2, ResolverStyle resolverStyle) {
            ChronoLocalDate chronoLocalDatePlus;
            ChronoLocalDate chronoLocalDateDate = chronology.date(i, 1, 1);
            if (resolverStyle == ResolverStyle.LENIENT) {
                chronoLocalDatePlus = chronoLocalDateDate.plus(Math.addExact(Math.multiplyExact(Math.subtractExact(j, localizedWeekOfYear(chronoLocalDateDate)), 7L), i2 - localizedDayOfWeek(chronoLocalDateDate)), (TemporalUnit) ChronoUnit.DAYS);
            } else {
                chronoLocalDatePlus = chronoLocalDateDate.plus((((int) (((long) this.range.checkValidIntValue(j, this)) - localizedWeekOfYear(chronoLocalDateDate))) * 7) + (i2 - localizedDayOfWeek(chronoLocalDateDate)), (TemporalUnit) ChronoUnit.DAYS);
                if (resolverStyle == ResolverStyle.STRICT && chronoLocalDatePlus.getLong(ChronoField.YEAR) != i) {
                    throw new DateTimeException("Strict mode rejected resolved date as it is in a different year");
                }
            }
            map.remove(this);
            map.remove(ChronoField.YEAR);
            map.remove(ChronoField.DAY_OF_WEEK);
            return chronoLocalDatePlus;
        }

        private ChronoLocalDate resolveWBY(Map<TemporalField, Long> map, Chronology chronology, int i, ResolverStyle resolverStyle) {
            ChronoLocalDate chronoLocalDateOfWeekBasedYear;
            int iCheckValidIntValue = this.weekDef.weekBasedYear.range().checkValidIntValue(map.get(this.weekDef.weekBasedYear).longValue(), this.weekDef.weekBasedYear);
            if (resolverStyle == ResolverStyle.LENIENT) {
                chronoLocalDateOfWeekBasedYear = ofWeekBasedYear(chronology, iCheckValidIntValue, 1, i).plus(Math.subtractExact(map.get(this.weekDef.weekOfWeekBasedYear).longValue(), 1L), (TemporalUnit) ChronoUnit.WEEKS);
            } else {
                chronoLocalDateOfWeekBasedYear = ofWeekBasedYear(chronology, iCheckValidIntValue, this.weekDef.weekOfWeekBasedYear.range().checkValidIntValue(map.get(this.weekDef.weekOfWeekBasedYear).longValue(), this.weekDef.weekOfWeekBasedYear), i);
                if (resolverStyle == ResolverStyle.STRICT && localizedWeekBasedYear(chronoLocalDateOfWeekBasedYear) != iCheckValidIntValue) {
                    throw new DateTimeException("Strict mode rejected resolved date as it is in a different week-based-year");
                }
            }
            map.remove(this);
            map.remove(this.weekDef.weekBasedYear);
            map.remove(this.weekDef.weekOfWeekBasedYear);
            map.remove(ChronoField.DAY_OF_WEEK);
            return chronoLocalDateOfWeekBasedYear;
        }

        @Override
        public String getDisplayName(Locale locale) {
            Objects.requireNonNull(locale, "locale");
            if (this.rangeUnit == ChronoUnit.YEARS) {
                String appendItemName = DateTimePatternGenerator.getFrozenInstance(ULocale.forLocale(locale)).getAppendItemName(4);
                return (appendItemName == null || appendItemName.isEmpty()) ? this.name : appendItemName;
            }
            return this.name;
        }

        @Override
        public TemporalUnit getBaseUnit() {
            return this.baseUnit;
        }

        @Override
        public TemporalUnit getRangeUnit() {
            return this.rangeUnit;
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
        public ValueRange range() {
            return this.range;
        }

        @Override
        public boolean isSupportedBy(TemporalAccessor temporalAccessor) {
            if (temporalAccessor.isSupported(ChronoField.DAY_OF_WEEK)) {
                if (this.rangeUnit == ChronoUnit.WEEKS) {
                    return true;
                }
                if (this.rangeUnit == ChronoUnit.MONTHS) {
                    return temporalAccessor.isSupported(ChronoField.DAY_OF_MONTH);
                }
                if (this.rangeUnit == ChronoUnit.YEARS) {
                    return temporalAccessor.isSupported(ChronoField.DAY_OF_YEAR);
                }
                if (this.rangeUnit == WeekFields.WEEK_BASED_YEARS) {
                    return temporalAccessor.isSupported(ChronoField.DAY_OF_YEAR);
                }
                if (this.rangeUnit == ChronoUnit.FOREVER) {
                    return temporalAccessor.isSupported(ChronoField.YEAR);
                }
                return false;
            }
            return false;
        }

        @Override
        public ValueRange rangeRefinedBy(TemporalAccessor temporalAccessor) {
            if (this.rangeUnit == ChronoUnit.WEEKS) {
                return this.range;
            }
            if (this.rangeUnit == ChronoUnit.MONTHS) {
                return rangeByWeek(temporalAccessor, ChronoField.DAY_OF_MONTH);
            }
            if (this.rangeUnit == ChronoUnit.YEARS) {
                return rangeByWeek(temporalAccessor, ChronoField.DAY_OF_YEAR);
            }
            if (this.rangeUnit == WeekFields.WEEK_BASED_YEARS) {
                return rangeWeekOfWeekBasedYear(temporalAccessor);
            }
            if (this.rangeUnit == ChronoUnit.FOREVER) {
                return ChronoField.YEAR.range();
            }
            throw new IllegalStateException("unreachable, rangeUnit: " + ((Object) this.rangeUnit) + ", this: " + ((Object) this));
        }

        private ValueRange rangeByWeek(TemporalAccessor temporalAccessor, TemporalField temporalField) {
            int iStartOfWeekOffset = startOfWeekOffset(temporalAccessor.get(temporalField), localizedDayOfWeek(temporalAccessor));
            ValueRange valueRangeRange = temporalAccessor.range(temporalField);
            return ValueRange.of(computeWeek(iStartOfWeekOffset, (int) valueRangeRange.getMinimum()), computeWeek(iStartOfWeekOffset, (int) valueRangeRange.getMaximum()));
        }

        private ValueRange rangeWeekOfWeekBasedYear(TemporalAccessor temporalAccessor) {
            if (!temporalAccessor.isSupported(ChronoField.DAY_OF_YEAR)) {
                return WEEK_OF_YEAR_RANGE;
            }
            int iLocalizedDayOfWeek = localizedDayOfWeek(temporalAccessor);
            int i = temporalAccessor.get(ChronoField.DAY_OF_YEAR);
            int iStartOfWeekOffset = startOfWeekOffset(i, iLocalizedDayOfWeek);
            int iComputeWeek = computeWeek(iStartOfWeekOffset, i);
            if (iComputeWeek == 0) {
                return rangeWeekOfWeekBasedYear(Chronology.from(temporalAccessor).date(temporalAccessor).minus(i + 7, (TemporalUnit) ChronoUnit.DAYS));
            }
            if (iComputeWeek >= computeWeek(iStartOfWeekOffset, this.weekDef.getMinimalDaysInFirstWeek() + ((int) temporalAccessor.range(ChronoField.DAY_OF_YEAR).getMaximum()))) {
                return rangeWeekOfWeekBasedYear(Chronology.from(temporalAccessor).date(temporalAccessor).plus((r3 - i) + 1 + 7, (TemporalUnit) ChronoUnit.DAYS));
            }
            return ValueRange.of(1L, r0 - 1);
        }

        @Override
        public String toString() {
            return this.name + "[" + this.weekDef.toString() + "]";
        }
    }
}
