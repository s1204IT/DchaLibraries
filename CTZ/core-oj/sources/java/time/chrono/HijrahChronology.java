package java.time.chrono;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import sun.util.calendar.BaseCalendar;
import sun.util.logging.PlatformLogger;

public final class HijrahChronology extends AbstractChronology implements Serializable {
    public static final HijrahChronology INSTANCE;
    private static final String KEY_ID = "id";
    private static final String KEY_ISO_START = "iso-start";
    private static final String KEY_TYPE = "type";
    private static final String KEY_VERSION = "version";
    private static final String PROP_PREFIX = "calendar.hijrah.";
    private static final String PROP_TYPE_SUFFIX = ".type";
    private static final transient Properties calendarProperties;
    private static final long serialVersionUID = 3127340209035924785L;
    private final transient String calendarType;
    private transient int[] hijrahEpochMonthStartDays;
    private transient int hijrahStartEpochMonth;
    private volatile transient boolean initComplete;
    private transient int maxEpochDay;
    private transient int maxMonthLength;
    private transient int maxYearLength;
    private transient int minEpochDay;
    private transient int minMonthLength;
    private transient int minYearLength;
    private final transient String typeId;

    @Override
    public ChronoLocalDate resolveDate(Map map, ResolverStyle resolverStyle) {
        return resolveDate((Map<TemporalField, Long>) map, resolverStyle);
    }

    static {
        try {
            calendarProperties = BaseCalendar.getCalendarProperties();
            try {
                INSTANCE = new HijrahChronology("Hijrah-umalqura");
                AbstractChronology.registerChrono(INSTANCE, "Hijrah");
                AbstractChronology.registerChrono(INSTANCE, "islamic");
                registerVariants();
            } catch (DateTimeException e) {
                PlatformLogger.getLogger("java.time.chrono").severe("Unable to initialize Hijrah calendar: Hijrah-umalqura", e);
                throw new RuntimeException("Unable to initialize Hijrah-umalqura calendar", e.getCause());
            }
        } catch (IOException e2) {
            throw new InternalError("Can't initialize lib/calendars.properties", e2);
        }
    }

    private static void registerVariants() {
        for (String str : calendarProperties.stringPropertyNames()) {
            if (str.startsWith(PROP_PREFIX)) {
                String strSubstring = str.substring(PROP_PREFIX.length());
                if (strSubstring.indexOf(46) < 0 && !strSubstring.equals(INSTANCE.getId())) {
                    try {
                        AbstractChronology.registerChrono(new HijrahChronology(strSubstring));
                    } catch (DateTimeException e) {
                        PlatformLogger.getLogger("java.time.chrono").severe("Unable to initialize Hijrah calendar: " + strSubstring, e);
                    }
                }
            }
        }
    }

    private HijrahChronology(String str) throws DateTimeException {
        if (str.isEmpty()) {
            throw new IllegalArgumentException("calendar id is empty");
        }
        String str2 = PROP_PREFIX + str + PROP_TYPE_SUFFIX;
        String property = calendarProperties.getProperty(str2);
        if (property == null || property.isEmpty()) {
            throw new DateTimeException("calendarType is missing or empty for: " + str2);
        }
        this.typeId = str;
        this.calendarType = property;
    }

    private void checkCalendarInit() {
        if (!this.initComplete) {
            loadCalendarData();
            this.initComplete = true;
        }
    }

    @Override
    public String getId() {
        return this.typeId;
    }

    @Override
    public String getCalendarType() {
        return this.calendarType;
    }

    @Override
    public HijrahDate date(Era era, int i, int i2, int i3) {
        return date(prolepticYear(era, i), i2, i3);
    }

    @Override
    public HijrahDate date(int i, int i2, int i3) {
        return HijrahDate.of(this, i, i2, i3);
    }

    @Override
    public HijrahDate dateYearDay(Era era, int i, int i2) {
        return dateYearDay(prolepticYear(era, i), i2);
    }

    @Override
    public HijrahDate dateYearDay(int i, int i2) {
        HijrahDate hijrahDateOf = HijrahDate.of(this, i, 1, 1);
        if (i2 > hijrahDateOf.lengthOfYear()) {
            throw new DateTimeException("Invalid dayOfYear: " + i2);
        }
        return hijrahDateOf.plusDays(i2 - 1);
    }

    @Override
    public HijrahDate dateEpochDay(long j) {
        return HijrahDate.ofEpochDay(this, j);
    }

    @Override
    public HijrahDate dateNow() {
        return dateNow(Clock.systemDefaultZone());
    }

    @Override
    public HijrahDate dateNow(ZoneId zoneId) {
        return dateNow(Clock.system(zoneId));
    }

    @Override
    public HijrahDate dateNow(Clock clock) {
        return date((TemporalAccessor) LocalDate.now(clock));
    }

    @Override
    public HijrahDate date(TemporalAccessor temporalAccessor) {
        if (temporalAccessor instanceof HijrahDate) {
            return (HijrahDate) temporalAccessor;
        }
        return HijrahDate.ofEpochDay(this, temporalAccessor.getLong(ChronoField.EPOCH_DAY));
    }

    @Override
    public ChronoLocalDateTime<HijrahDate> localDateTime(TemporalAccessor temporalAccessor) {
        return super.localDateTime(temporalAccessor);
    }

    @Override
    public ChronoZonedDateTime<HijrahDate> zonedDateTime(TemporalAccessor temporalAccessor) {
        return super.zonedDateTime(temporalAccessor);
    }

    @Override
    public ChronoZonedDateTime<HijrahDate> zonedDateTime(Instant instant, ZoneId zoneId) {
        return super.zonedDateTime(instant, zoneId);
    }

    @Override
    public boolean isLeapYear(long j) {
        checkCalendarInit();
        return j >= ((long) getMinimumYear()) && j <= ((long) getMaximumYear()) && getYearLength((int) j) > 354;
    }

    @Override
    public int prolepticYear(Era era, int i) {
        if (!(era instanceof HijrahEra)) {
            throw new ClassCastException("Era must be HijrahEra");
        }
        return i;
    }

    @Override
    public HijrahEra eraOf(int i) {
        if (i == 1) {
            return HijrahEra.AH;
        }
        throw new DateTimeException("invalid Hijrah era");
    }

    @Override
    public List<Era> eras() {
        return Arrays.asList(HijrahEra.values());
    }

    @Override
    public ValueRange range(ChronoField chronoField) {
        checkCalendarInit();
        if (chronoField instanceof ChronoField) {
            switch (chronoField) {
                case DAY_OF_MONTH:
                    return ValueRange.of(1L, 1L, getMinimumMonthLength(), getMaximumMonthLength());
                case DAY_OF_YEAR:
                    return ValueRange.of(1L, getMaximumDayOfYear());
                case ALIGNED_WEEK_OF_MONTH:
                    return ValueRange.of(1L, 5L);
                case YEAR:
                case YEAR_OF_ERA:
                    return ValueRange.of(getMinimumYear(), getMaximumYear());
                case ERA:
                    return ValueRange.of(1L, 1L);
                default:
                    return chronoField.range();
            }
        }
        return chronoField.range();
    }

    @Override
    public HijrahDate resolveDate(Map<TemporalField, Long> map, ResolverStyle resolverStyle) {
        return (HijrahDate) super.resolveDate(map, resolverStyle);
    }

    int checkValidYear(long j) {
        if (j < getMinimumYear() || j > getMaximumYear()) {
            throw new DateTimeException("Invalid Hijrah year: " + j);
        }
        return (int) j;
    }

    void checkValidDayOfYear(int i) {
        if (i < 1 || i > getMaximumDayOfYear()) {
            throw new DateTimeException("Invalid Hijrah day of year: " + i);
        }
    }

    void checkValidMonth(int i) {
        if (i < 1 || i > 12) {
            throw new DateTimeException("Invalid Hijrah month: " + i);
        }
    }

    int[] getHijrahDateInfo(int i) {
        checkCalendarInit();
        if (i < this.minEpochDay || i >= this.maxEpochDay) {
            throw new DateTimeException("Hijrah date out of range");
        }
        int iEpochDayToEpochMonth = epochDayToEpochMonth(i);
        return new int[]{epochMonthToYear(iEpochDayToEpochMonth), epochMonthToMonth(iEpochDayToEpochMonth) + 1, (i - epochMonthToEpochDay(iEpochDayToEpochMonth)) + 1};
    }

    long getEpochDay(int i, int i2, int i3) {
        checkCalendarInit();
        checkValidMonth(i2);
        int iYearToEpochMonth = yearToEpochMonth(i) + (i2 - 1);
        if (iYearToEpochMonth < 0 || iYearToEpochMonth >= this.hijrahEpochMonthStartDays.length) {
            throw new DateTimeException("Invalid Hijrah date, year: " + i + ", month: " + i2);
        }
        if (i3 >= 1 && i3 <= getMonthLength(i, i2)) {
            return epochMonthToEpochDay(iYearToEpochMonth) + (i3 - 1);
        }
        throw new DateTimeException("Invalid Hijrah day of month: " + i3);
    }

    int getDayOfYear(int i, int i2) {
        return yearMonthToDayOfYear(i, i2 - 1);
    }

    int getMonthLength(int i, int i2) {
        int iYearToEpochMonth = yearToEpochMonth(i) + (i2 - 1);
        if (iYearToEpochMonth < 0 || iYearToEpochMonth >= this.hijrahEpochMonthStartDays.length) {
            throw new DateTimeException("Invalid Hijrah date, year: " + i + ", month: " + i2);
        }
        return epochMonthLength(iYearToEpochMonth);
    }

    int getYearLength(int i) {
        return yearMonthToDayOfYear(i, 12);
    }

    int getMinimumYear() {
        return epochMonthToYear(0);
    }

    int getMaximumYear() {
        return epochMonthToYear(this.hijrahEpochMonthStartDays.length - 1) - 1;
    }

    int getMaximumMonthLength() {
        return this.maxMonthLength;
    }

    int getMinimumMonthLength() {
        return this.minMonthLength;
    }

    int getMaximumDayOfYear() {
        return this.maxYearLength;
    }

    int getSmallestMaximumDayOfYear() {
        return this.minYearLength;
    }

    private int epochDayToEpochMonth(int i) {
        int iBinarySearch = Arrays.binarySearch(this.hijrahEpochMonthStartDays, i);
        if (iBinarySearch < 0) {
            return (-iBinarySearch) - 2;
        }
        return iBinarySearch;
    }

    private int epochMonthToYear(int i) {
        return (i + this.hijrahStartEpochMonth) / 12;
    }

    private int yearToEpochMonth(int i) {
        return (i * 12) - this.hijrahStartEpochMonth;
    }

    private int epochMonthToMonth(int i) {
        return (i + this.hijrahStartEpochMonth) % 12;
    }

    private int epochMonthToEpochDay(int i) {
        return this.hijrahEpochMonthStartDays[i];
    }

    private int yearMonthToDayOfYear(int i, int i2) {
        int iYearToEpochMonth = yearToEpochMonth(i);
        return epochMonthToEpochDay(i2 + iYearToEpochMonth) - epochMonthToEpochDay(iYearToEpochMonth);
    }

    private int epochMonthLength(int i) {
        return this.hijrahEpochMonthStartDays[i + 1] - this.hijrahEpochMonthStartDays[i];
    }

    private static Properties readConfigProperties(String str) throws Exception {
        Properties properties = new Properties();
        InputStream systemResourceAsStream = ClassLoader.getSystemResourceAsStream(str);
        try {
            properties.load(systemResourceAsStream);
            if (systemResourceAsStream != null) {
                systemResourceAsStream.close();
            }
            return properties;
        } catch (Throwable th) {
            th = th;
            try {
                throw th;
            } catch (Throwable th2) {
                th = th2;
                if (systemResourceAsStream != null) {
                    if (th != null) {
                        try {
                            systemResourceAsStream.close();
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                        }
                    } else {
                        systemResourceAsStream.close();
                    }
                }
                throw th;
            }
        }
    }

    private void loadCalendarData() {
        HashMap map;
        String str;
        int iMin;
        int iMax;
        int epochDay;
        String str2;
        String str3;
        try {
            String property = calendarProperties.getProperty(PROP_PREFIX + this.typeId);
            Objects.requireNonNull(property, "Resource missing for calendar: calendar.hijrah." + this.typeId);
            Properties configProperties = readConfigProperties(property);
            map = new HashMap();
            str = null;
            iMin = Integer.MAX_VALUE;
            iMax = Integer.MIN_VALUE;
            epochDay = 0;
            str2 = null;
            str3 = null;
        } catch (Exception e) {
            PlatformLogger.getLogger("java.time.chrono").severe("Unable to initialize Hijrah calendar proxy: " + this.typeId, e);
            throw new DateTimeException("Unable to initialize HijrahCalendar: " + this.typeId, e);
        }
        for (Map.Entry<Object, Object> entry : configProperties.entrySet()) {
            String str4 = (String) entry.getKey();
            byte b = -1;
            int iHashCode = str4.hashCode();
            if (iHashCode != -1117701862) {
                if (iHashCode != 3355) {
                    if (iHashCode != 3575610) {
                        if (iHashCode == 351608024 && str4.equals("version")) {
                            b = 2;
                        }
                    } else if (str4.equals(KEY_TYPE)) {
                        b = 1;
                    }
                } else if (str4.equals("id")) {
                    b = 0;
                }
            } else if (str4.equals(KEY_ISO_START)) {
                b = 3;
            }
            switch (b) {
                case 0:
                    str = (String) entry.getValue();
                    continue;
                case 1:
                    str2 = (String) entry.getValue();
                    continue;
                case 2:
                    str3 = (String) entry.getValue();
                    continue;
                case 3:
                    int[] ymd = parseYMD((String) entry.getValue());
                    epochDay = (int) LocalDate.of(ymd[0], ymd[1], ymd[2]).toEpochDay();
                    continue;
                default:
                    try {
                        int iIntValue = Integer.valueOf(str4).intValue();
                        map.put(Integer.valueOf(iIntValue), parseMonths((String) entry.getValue()));
                        iMax = Math.max(iMax, iIntValue);
                        iMin = Math.min(iMin, iIntValue);
                        continue;
                    } catch (NumberFormatException e2) {
                        throw new IllegalArgumentException("bad key: " + str4);
                    }
                    break;
            }
            PlatformLogger.getLogger("java.time.chrono").severe("Unable to initialize Hijrah calendar proxy: " + this.typeId, e);
            throw new DateTimeException("Unable to initialize HijrahCalendar: " + this.typeId, e);
        }
        if (!getId().equals(str)) {
            throw new IllegalArgumentException("Configuration is for a different calendar: " + str);
        }
        if (!getCalendarType().equals(str2)) {
            throw new IllegalArgumentException("Configuration is for a different calendar type: " + str2);
        }
        if (str3 == null || str3.isEmpty()) {
            throw new IllegalArgumentException("Configuration does not contain a version");
        }
        if (epochDay != 0) {
            this.hijrahStartEpochMonth = iMin * 12;
            this.minEpochDay = epochDay;
            this.hijrahEpochMonthStartDays = createEpochMonths(this.minEpochDay, iMin, iMax, map);
            this.maxEpochDay = this.hijrahEpochMonthStartDays[this.hijrahEpochMonthStartDays.length - 1];
            while (iMin < iMax) {
                int yearLength = getYearLength(iMin);
                this.minYearLength = Math.min(this.minYearLength, yearLength);
                this.maxYearLength = Math.max(this.maxYearLength, yearLength);
                iMin++;
            }
            return;
        }
        throw new IllegalArgumentException("Configuration does not contain a ISO start date");
    }

    private int[] createEpochMonths(int i, int i2, int i3, Map<Integer, int[]> map) {
        int[] iArr = new int[(((i3 - i2) + 1) * 12) + 1];
        this.minMonthLength = Integer.MAX_VALUE;
        this.maxMonthLength = Integer.MIN_VALUE;
        int i4 = i;
        int i5 = i2;
        int i6 = 0;
        while (i5 <= i3) {
            int[] iArr2 = map.get(Integer.valueOf(i5));
            int i7 = i4;
            int i8 = i6;
            int i9 = 0;
            while (i9 < 12) {
                int i10 = iArr2[i9];
                int i11 = i8 + 1;
                iArr[i8] = i7;
                if (i10 < 29 || i10 > 32) {
                    throw new IllegalArgumentException("Invalid month length in year: " + i2);
                }
                i7 += i10;
                this.minMonthLength = Math.min(this.minMonthLength, i10);
                this.maxMonthLength = Math.max(this.maxMonthLength, i10);
                i9++;
                i8 = i11;
            }
            i5++;
            i6 = i8;
            i4 = i7;
        }
        int i12 = i6 + 1;
        iArr[i6] = i4;
        if (i12 != iArr.length) {
            throw new IllegalStateException("Did not fill epochMonths exactly: ndx = " + i12 + " should be " + iArr.length);
        }
        return iArr;
    }

    private int[] parseMonths(String str) {
        int[] iArr = new int[12];
        String[] strArrSplit = str.split("\\s");
        if (strArrSplit.length != 12) {
            throw new IllegalArgumentException("wrong number of months on line: " + Arrays.toString(strArrSplit) + "; count: " + strArrSplit.length);
        }
        for (int i = 0; i < 12; i++) {
            try {
                iArr[i] = Integer.valueOf(strArrSplit[i]).intValue();
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("bad key: " + strArrSplit[i]);
            }
        }
        return iArr;
    }

    private int[] parseYMD(String str) {
        String strTrim = str.trim();
        try {
            if (strTrim.charAt(4) != '-' || strTrim.charAt(7) != '-') {
                throw new IllegalArgumentException("date must be yyyy-MM-dd");
            }
            return new int[]{Integer.valueOf(strTrim.substring(0, 4)).intValue(), Integer.valueOf(strTrim.substring(5, 7)).intValue(), Integer.valueOf(strTrim.substring(8, 10)).intValue()};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("date must be yyyy-MM-dd", e);
        }
    }

    @Override
    Object writeReplace() {
        return super.writeReplace();
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }
}
