package java.time.chrono;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import sun.security.x509.PolicyInformation;
import sun.util.logging.PlatformLogger;

public abstract class AbstractChronology implements Chronology {
    static final Comparator<ChronoLocalDate> DATE_ORDER = $$Lambda$AbstractChronology$j22w8kHhJoqCd56hhLQK1G0VLFw.INSTANCE;
    static final Comparator<ChronoLocalDateTime<? extends ChronoLocalDate>> DATE_TIME_ORDER = $$Lambda$AbstractChronology$onW9aZyLFliH5Gg1qLodD_GoPfA.INSTANCE;
    static final Comparator<ChronoZonedDateTime<?>> INSTANT_ORDER = $$Lambda$AbstractChronology$5b0W7uLeaWkn0HLPDKwPXzJ7HPo.INSTANCE;
    private static final ConcurrentHashMap<String, Chronology> CHRONOS_BY_ID = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Chronology> CHRONOS_BY_TYPE = new ConcurrentHashMap<>();

    static int lambda$static$b5a61975$1(ChronoLocalDateTime chronoLocalDateTime, ChronoLocalDateTime chronoLocalDateTime2) {
        int iCompare = Long.compare(chronoLocalDateTime.toLocalDate().toEpochDay(), chronoLocalDateTime2.toLocalDate().toEpochDay());
        if (iCompare == 0) {
            return Long.compare(chronoLocalDateTime.toLocalTime().toNanoOfDay(), chronoLocalDateTime2.toLocalTime().toNanoOfDay());
        }
        return iCompare;
    }

    static int lambda$static$2241c452$1(ChronoZonedDateTime chronoZonedDateTime, ChronoZonedDateTime chronoZonedDateTime2) {
        int iCompare = Long.compare(chronoZonedDateTime.toEpochSecond(), chronoZonedDateTime2.toEpochSecond());
        if (iCompare == 0) {
            return Long.compare(chronoZonedDateTime.toLocalTime().getNano(), chronoZonedDateTime2.toLocalTime().getNano());
        }
        return iCompare;
    }

    static Chronology registerChrono(Chronology chronology) {
        return registerChrono(chronology, chronology.getId());
    }

    static Chronology registerChrono(Chronology chronology, String str) {
        String calendarType;
        Chronology chronologyPutIfAbsent = CHRONOS_BY_ID.putIfAbsent(str, chronology);
        if (chronologyPutIfAbsent == null && (calendarType = chronology.getCalendarType()) != null) {
            CHRONOS_BY_TYPE.putIfAbsent(calendarType, chronology);
        }
        return chronologyPutIfAbsent;
    }

    private static boolean initCache() {
        if (CHRONOS_BY_ID.get("ISO") == null) {
            registerChrono(HijrahChronology.INSTANCE);
            registerChrono(JapaneseChronology.INSTANCE);
            registerChrono(MinguoChronology.INSTANCE);
            registerChrono(ThaiBuddhistChronology.INSTANCE);
            for (AbstractChronology abstractChronology : ServiceLoader.load(AbstractChronology.class, null)) {
                String id = abstractChronology.getId();
                if (id.equals("ISO") || registerChrono(abstractChronology) != null) {
                    PlatformLogger.getLogger("java.time.chrono").warning("Ignoring duplicate Chronology, from ServiceLoader configuration " + id);
                }
            }
            registerChrono(IsoChronology.INSTANCE);
            return true;
        }
        return false;
    }

    static Chronology ofLocale(Locale locale) {
        Objects.requireNonNull(locale, "locale");
        String unicodeLocaleType = locale.getUnicodeLocaleType("ca");
        if (unicodeLocaleType == null || "iso".equals(unicodeLocaleType) || "iso8601".equals(unicodeLocaleType)) {
            return IsoChronology.INSTANCE;
        }
        do {
            Chronology chronology = CHRONOS_BY_TYPE.get(unicodeLocaleType);
            if (chronology != null) {
                return chronology;
            }
        } while (initCache());
        for (Chronology chronology2 : ServiceLoader.load(Chronology.class)) {
            if (unicodeLocaleType.equals(chronology2.getCalendarType())) {
                return chronology2;
            }
        }
        throw new DateTimeException("Unknown calendar system: " + unicodeLocaleType);
    }

    static Chronology of(String str) {
        Objects.requireNonNull(str, PolicyInformation.ID);
        do {
            Chronology chronologyOf0 = of0(str);
            if (chronologyOf0 != null) {
                return chronologyOf0;
            }
        } while (initCache());
        for (Chronology chronology : ServiceLoader.load(Chronology.class)) {
            if (str.equals(chronology.getId()) || str.equals(chronology.getCalendarType())) {
                return chronology;
            }
        }
        throw new DateTimeException("Unknown chronology: " + str);
    }

    private static Chronology of0(String str) {
        Chronology chronology = CHRONOS_BY_ID.get(str);
        if (chronology == null) {
            return CHRONOS_BY_TYPE.get(str);
        }
        return chronology;
    }

    static Set<Chronology> getAvailableChronologies() {
        initCache();
        HashSet hashSet = new HashSet(CHRONOS_BY_ID.values());
        Iterator it = ServiceLoader.load(Chronology.class).iterator();
        while (it.hasNext()) {
            hashSet.add((Chronology) it.next());
        }
        return hashSet;
    }

    protected AbstractChronology() {
    }

    @Override
    public ChronoLocalDate resolveDate(Map<TemporalField, Long> map, ResolverStyle resolverStyle) {
        if (map.containsKey(ChronoField.EPOCH_DAY)) {
            return dateEpochDay(map.remove(ChronoField.EPOCH_DAY).longValue());
        }
        resolveProlepticMonth(map, resolverStyle);
        ChronoLocalDate chronoLocalDateResolveYearOfEra = resolveYearOfEra(map, resolverStyle);
        if (chronoLocalDateResolveYearOfEra != null) {
            return chronoLocalDateResolveYearOfEra;
        }
        if (map.containsKey(ChronoField.YEAR)) {
            if (map.containsKey(ChronoField.MONTH_OF_YEAR)) {
                if (map.containsKey(ChronoField.DAY_OF_MONTH)) {
                    return resolveYMD(map, resolverStyle);
                }
                if (map.containsKey(ChronoField.ALIGNED_WEEK_OF_MONTH)) {
                    if (map.containsKey(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH)) {
                        return resolveYMAA(map, resolverStyle);
                    }
                    if (map.containsKey(ChronoField.DAY_OF_WEEK)) {
                        return resolveYMAD(map, resolverStyle);
                    }
                }
            }
            if (map.containsKey(ChronoField.DAY_OF_YEAR)) {
                return resolveYD(map, resolverStyle);
            }
            if (map.containsKey(ChronoField.ALIGNED_WEEK_OF_YEAR)) {
                if (map.containsKey(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR)) {
                    return resolveYAA(map, resolverStyle);
                }
                if (map.containsKey(ChronoField.DAY_OF_WEEK)) {
                    return resolveYAD(map, resolverStyle);
                }
                return null;
            }
            return null;
        }
        return null;
    }

    void resolveProlepticMonth(Map<TemporalField, Long> map, ResolverStyle resolverStyle) {
        Long lRemove = map.remove(ChronoField.PROLEPTIC_MONTH);
        if (lRemove != null) {
            if (resolverStyle != ResolverStyle.LENIENT) {
                ChronoField.PROLEPTIC_MONTH.checkValidValue(lRemove.longValue());
            }
            ChronoLocalDate chronoLocalDateWith = dateNow().with((TemporalField) ChronoField.DAY_OF_MONTH, 1L).with((TemporalField) ChronoField.PROLEPTIC_MONTH, lRemove.longValue());
            addFieldValue(map, ChronoField.MONTH_OF_YEAR, chronoLocalDateWith.get(ChronoField.MONTH_OF_YEAR));
            addFieldValue(map, ChronoField.YEAR, chronoLocalDateWith.get(ChronoField.YEAR));
        }
    }

    ChronoLocalDate resolveYearOfEra(Map<TemporalField, Long> map, ResolverStyle resolverStyle) {
        int intExact;
        Long lRemove = map.remove(ChronoField.YEAR_OF_ERA);
        if (lRemove == null) {
            if (map.containsKey(ChronoField.ERA)) {
                range(ChronoField.ERA).checkValidValue(map.get(ChronoField.ERA).longValue(), ChronoField.ERA);
                return null;
            }
            return null;
        }
        Long lRemove2 = map.remove(ChronoField.ERA);
        if (resolverStyle != ResolverStyle.LENIENT) {
            intExact = range(ChronoField.YEAR_OF_ERA).checkValidIntValue(lRemove.longValue(), ChronoField.YEAR_OF_ERA);
        } else {
            intExact = Math.toIntExact(lRemove.longValue());
        }
        if (lRemove2 != null) {
            addFieldValue(map, ChronoField.YEAR, prolepticYear(eraOf(range(ChronoField.ERA).checkValidIntValue(lRemove2.longValue(), ChronoField.ERA)), intExact));
            return null;
        }
        if (map.containsKey(ChronoField.YEAR)) {
            addFieldValue(map, ChronoField.YEAR, prolepticYear(dateYearDay(range(ChronoField.YEAR).checkValidIntValue(map.get(ChronoField.YEAR).longValue(), ChronoField.YEAR), 1).getEra(), intExact));
            return null;
        }
        if (resolverStyle == ResolverStyle.STRICT) {
            map.put(ChronoField.YEAR_OF_ERA, lRemove);
            return null;
        }
        if (eras().isEmpty()) {
            addFieldValue(map, ChronoField.YEAR, intExact);
            return null;
        }
        addFieldValue(map, ChronoField.YEAR, prolepticYear(r8.get(r8.size() - 1), intExact));
        return null;
    }

    ChronoLocalDate resolveYMD(Map<TemporalField, Long> map, ResolverStyle resolverStyle) {
        int iCheckValidIntValue = range(ChronoField.YEAR).checkValidIntValue(map.remove(ChronoField.YEAR).longValue(), ChronoField.YEAR);
        if (resolverStyle == ResolverStyle.LENIENT) {
            long jSubtractExact = Math.subtractExact(map.remove(ChronoField.MONTH_OF_YEAR).longValue(), 1L);
            return date(iCheckValidIntValue, 1, 1).plus(jSubtractExact, (TemporalUnit) ChronoUnit.MONTHS).plus(Math.subtractExact(map.remove(ChronoField.DAY_OF_MONTH).longValue(), 1L), (TemporalUnit) ChronoUnit.DAYS);
        }
        int iCheckValidIntValue2 = range(ChronoField.MONTH_OF_YEAR).checkValidIntValue(map.remove(ChronoField.MONTH_OF_YEAR).longValue(), ChronoField.MONTH_OF_YEAR);
        int iCheckValidIntValue3 = range(ChronoField.DAY_OF_MONTH).checkValidIntValue(map.remove(ChronoField.DAY_OF_MONTH).longValue(), ChronoField.DAY_OF_MONTH);
        if (resolverStyle == ResolverStyle.SMART) {
            try {
                return date(iCheckValidIntValue, iCheckValidIntValue2, iCheckValidIntValue3);
            } catch (DateTimeException e) {
                return date(iCheckValidIntValue, iCheckValidIntValue2, 1).with(TemporalAdjusters.lastDayOfMonth());
            }
        }
        return date(iCheckValidIntValue, iCheckValidIntValue2, iCheckValidIntValue3);
    }

    ChronoLocalDate resolveYD(Map<TemporalField, Long> map, ResolverStyle resolverStyle) {
        int iCheckValidIntValue = range(ChronoField.YEAR).checkValidIntValue(map.remove(ChronoField.YEAR).longValue(), ChronoField.YEAR);
        if (resolverStyle == ResolverStyle.LENIENT) {
            return dateYearDay(iCheckValidIntValue, 1).plus(Math.subtractExact(map.remove(ChronoField.DAY_OF_YEAR).longValue(), 1L), (TemporalUnit) ChronoUnit.DAYS);
        }
        return dateYearDay(iCheckValidIntValue, range(ChronoField.DAY_OF_YEAR).checkValidIntValue(map.remove(ChronoField.DAY_OF_YEAR).longValue(), ChronoField.DAY_OF_YEAR));
    }

    ChronoLocalDate resolveYMAA(Map<TemporalField, Long> map, ResolverStyle resolverStyle) {
        int iCheckValidIntValue = range(ChronoField.YEAR).checkValidIntValue(map.remove(ChronoField.YEAR).longValue(), ChronoField.YEAR);
        if (resolverStyle == ResolverStyle.LENIENT) {
            long jSubtractExact = Math.subtractExact(map.remove(ChronoField.MONTH_OF_YEAR).longValue(), 1L);
            return date(iCheckValidIntValue, 1, 1).plus(jSubtractExact, (TemporalUnit) ChronoUnit.MONTHS).plus(Math.subtractExact(map.remove(ChronoField.ALIGNED_WEEK_OF_MONTH).longValue(), 1L), (TemporalUnit) ChronoUnit.WEEKS).plus(Math.subtractExact(map.remove(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH).longValue(), 1L), (TemporalUnit) ChronoUnit.DAYS);
        }
        int iCheckValidIntValue2 = range(ChronoField.MONTH_OF_YEAR).checkValidIntValue(map.remove(ChronoField.MONTH_OF_YEAR).longValue(), ChronoField.MONTH_OF_YEAR);
        ChronoLocalDate chronoLocalDatePlus = date(iCheckValidIntValue, iCheckValidIntValue2, 1).plus(((range(ChronoField.ALIGNED_WEEK_OF_MONTH).checkValidIntValue(map.remove(ChronoField.ALIGNED_WEEK_OF_MONTH).longValue(), ChronoField.ALIGNED_WEEK_OF_MONTH) - 1) * 7) + (range(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH).checkValidIntValue(map.remove(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH).longValue(), ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH) - 1), (TemporalUnit) ChronoUnit.DAYS);
        if (resolverStyle == ResolverStyle.STRICT && chronoLocalDatePlus.get(ChronoField.MONTH_OF_YEAR) != iCheckValidIntValue2) {
            throw new DateTimeException("Strict mode rejected resolved date as it is in a different month");
        }
        return chronoLocalDatePlus;
    }

    ChronoLocalDate resolveYMAD(Map<TemporalField, Long> map, ResolverStyle resolverStyle) {
        int iCheckValidIntValue = range(ChronoField.YEAR).checkValidIntValue(map.remove(ChronoField.YEAR).longValue(), ChronoField.YEAR);
        if (resolverStyle == ResolverStyle.LENIENT) {
            return resolveAligned(date(iCheckValidIntValue, 1, 1), Math.subtractExact(map.remove(ChronoField.MONTH_OF_YEAR).longValue(), 1L), Math.subtractExact(map.remove(ChronoField.ALIGNED_WEEK_OF_MONTH).longValue(), 1L), Math.subtractExact(map.remove(ChronoField.DAY_OF_WEEK).longValue(), 1L));
        }
        int iCheckValidIntValue2 = range(ChronoField.MONTH_OF_YEAR).checkValidIntValue(map.remove(ChronoField.MONTH_OF_YEAR).longValue(), ChronoField.MONTH_OF_YEAR);
        ChronoLocalDate chronoLocalDateWith = date(iCheckValidIntValue, iCheckValidIntValue2, 1).plus((range(ChronoField.ALIGNED_WEEK_OF_MONTH).checkValidIntValue(map.remove(ChronoField.ALIGNED_WEEK_OF_MONTH).longValue(), ChronoField.ALIGNED_WEEK_OF_MONTH) - 1) * 7, (TemporalUnit) ChronoUnit.DAYS).with(TemporalAdjusters.nextOrSame(DayOfWeek.of(range(ChronoField.DAY_OF_WEEK).checkValidIntValue(map.remove(ChronoField.DAY_OF_WEEK).longValue(), ChronoField.DAY_OF_WEEK))));
        if (resolverStyle == ResolverStyle.STRICT && chronoLocalDateWith.get(ChronoField.MONTH_OF_YEAR) != iCheckValidIntValue2) {
            throw new DateTimeException("Strict mode rejected resolved date as it is in a different month");
        }
        return chronoLocalDateWith;
    }

    ChronoLocalDate resolveYAA(Map<TemporalField, Long> map, ResolverStyle resolverStyle) {
        int iCheckValidIntValue = range(ChronoField.YEAR).checkValidIntValue(map.remove(ChronoField.YEAR).longValue(), ChronoField.YEAR);
        if (resolverStyle == ResolverStyle.LENIENT) {
            return dateYearDay(iCheckValidIntValue, 1).plus(Math.subtractExact(map.remove(ChronoField.ALIGNED_WEEK_OF_YEAR).longValue(), 1L), (TemporalUnit) ChronoUnit.WEEKS).plus(Math.subtractExact(map.remove(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR).longValue(), 1L), (TemporalUnit) ChronoUnit.DAYS);
        }
        ChronoLocalDate chronoLocalDatePlus = dateYearDay(iCheckValidIntValue, 1).plus(((range(ChronoField.ALIGNED_WEEK_OF_YEAR).checkValidIntValue(map.remove(ChronoField.ALIGNED_WEEK_OF_YEAR).longValue(), ChronoField.ALIGNED_WEEK_OF_YEAR) - 1) * 7) + (range(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR).checkValidIntValue(map.remove(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR).longValue(), ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR) - 1), (TemporalUnit) ChronoUnit.DAYS);
        if (resolverStyle == ResolverStyle.STRICT && chronoLocalDatePlus.get(ChronoField.YEAR) != iCheckValidIntValue) {
            throw new DateTimeException("Strict mode rejected resolved date as it is in a different year");
        }
        return chronoLocalDatePlus;
    }

    ChronoLocalDate resolveYAD(Map<TemporalField, Long> map, ResolverStyle resolverStyle) {
        int iCheckValidIntValue = range(ChronoField.YEAR).checkValidIntValue(map.remove(ChronoField.YEAR).longValue(), ChronoField.YEAR);
        if (resolverStyle == ResolverStyle.LENIENT) {
            return resolveAligned(dateYearDay(iCheckValidIntValue, 1), 0L, Math.subtractExact(map.remove(ChronoField.ALIGNED_WEEK_OF_YEAR).longValue(), 1L), Math.subtractExact(map.remove(ChronoField.DAY_OF_WEEK).longValue(), 1L));
        }
        ChronoLocalDate chronoLocalDateWith = dateYearDay(iCheckValidIntValue, 1).plus((range(ChronoField.ALIGNED_WEEK_OF_YEAR).checkValidIntValue(map.remove(ChronoField.ALIGNED_WEEK_OF_YEAR).longValue(), ChronoField.ALIGNED_WEEK_OF_YEAR) - 1) * 7, (TemporalUnit) ChronoUnit.DAYS).with(TemporalAdjusters.nextOrSame(DayOfWeek.of(range(ChronoField.DAY_OF_WEEK).checkValidIntValue(map.remove(ChronoField.DAY_OF_WEEK).longValue(), ChronoField.DAY_OF_WEEK))));
        if (resolverStyle == ResolverStyle.STRICT && chronoLocalDateWith.get(ChronoField.YEAR) != iCheckValidIntValue) {
            throw new DateTimeException("Strict mode rejected resolved date as it is in a different year");
        }
        return chronoLocalDateWith;
    }

    ChronoLocalDate resolveAligned(ChronoLocalDate chronoLocalDate, long j, long j2, long j3) {
        ChronoLocalDate chronoLocalDatePlus = chronoLocalDate.plus(j, (TemporalUnit) ChronoUnit.MONTHS).plus(j2, (TemporalUnit) ChronoUnit.WEEKS);
        if (j3 > 7) {
            long j4 = j3 - 1;
            chronoLocalDatePlus = chronoLocalDatePlus.plus(j4 / 7, (TemporalUnit) ChronoUnit.WEEKS);
            j3 = (j4 % 7) + 1;
        } else if (j3 < 1) {
            chronoLocalDatePlus = chronoLocalDatePlus.plus(Math.subtractExact(j3, 7L) / 7, (TemporalUnit) ChronoUnit.WEEKS);
            j3 = ((j3 + 6) % 7) + 1;
        }
        return chronoLocalDatePlus.with(TemporalAdjusters.nextOrSame(DayOfWeek.of((int) j3)));
    }

    void addFieldValue(Map<TemporalField, Long> map, ChronoField chronoField, long j) {
        Long l = map.get(chronoField);
        if (l != null && l.longValue() != j) {
            throw new DateTimeException("Conflict found: " + ((Object) chronoField) + " " + ((Object) l) + " differs from " + ((Object) chronoField) + " " + j);
        }
        map.put(chronoField, Long.valueOf(j));
    }

    @Override
    public int compareTo(Chronology chronology) {
        return getId().compareTo(chronology.getId());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return (obj instanceof AbstractChronology) && compareTo((Chronology) obj) == 0;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode() ^ getId().hashCode();
    }

    @Override
    public String toString() {
        return getId();
    }

    Object writeReplace() {
        return new Ser((byte) 1, this);
    }

    private void readObject(ObjectInputStream objectInputStream) throws ObjectStreamException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    void writeExternal(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(getId());
    }

    static Chronology readExternal(DataInput dataInput) throws IOException {
        return Chronology.of(dataInput.readUTF());
    }
}
