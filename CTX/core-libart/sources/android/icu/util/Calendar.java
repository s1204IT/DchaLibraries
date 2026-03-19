package android.icu.util;

import android.icu.impl.CalendarUtil;
import android.icu.impl.ICUCache;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SimpleCache;
import android.icu.impl.SimpleFormatterImpl;
import android.icu.impl.SoftCache;
import android.icu.impl.locale.BaseLocale;
import android.icu.lang.UCharacter;
import android.icu.text.DateFormat;
import android.icu.text.DateFormatSymbols;
import android.icu.text.SimpleDateFormat;
import android.icu.util.ULocale;
import dalvik.system.VMRuntime;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.MissingResourceException;
import libcore.icu.RelativeDateTimeFormatter;

public abstract class Calendar implements Serializable, Cloneable, Comparable<Calendar> {
    static final boolean $assertionsDisabled = false;
    public static final int AM = 0;
    public static final int AM_PM = 9;
    public static final int APRIL = 3;
    public static final int AUGUST = 7;

    @Deprecated
    protected static final int BASE_FIELD_COUNT = 23;
    public static final int DATE = 5;
    public static final int DAY_OF_MONTH = 5;
    public static final int DAY_OF_WEEK = 7;
    public static final int DAY_OF_WEEK_IN_MONTH = 8;
    public static final int DAY_OF_YEAR = 6;
    public static final int DECEMBER = 11;
    public static final int DOW_LOCAL = 18;
    public static final int DST_OFFSET = 16;
    protected static final int EPOCH_JULIAN_DAY = 2440588;
    public static final int ERA = 0;
    public static final int EXTENDED_YEAR = 19;
    public static final int FEBRUARY = 1;
    private static final int FIELD_DIFF_MAX_INT = Integer.MAX_VALUE;
    public static final int FRIDAY = 6;
    protected static final int GREATEST_MINIMUM = 1;
    public static final int HOUR = 10;
    public static final int HOUR_OF_DAY = 11;
    protected static final int INTERNALLY_SET = 1;
    public static final int IS_LEAP_MONTH = 22;
    public static final int JANUARY = 0;
    protected static final int JAN_1_1_JULIAN_DAY = 1721426;
    public static final int JULIAN_DAY = 20;
    public static final int JULY = 6;
    public static final int JUNE = 5;
    protected static final int LEAST_MAXIMUM = 2;
    public static final int MARCH = 2;
    protected static final int MAXIMUM = 3;

    @Deprecated
    protected static final int MAX_FIELD_COUNT = 32;
    private static final int MAX_HOURS = 548;
    protected static final int MAX_JULIAN = 2130706432;
    protected static final long MAX_MILLIS = 183882168921600000L;
    public static final int MAY = 4;
    public static final int MILLISECOND = 14;
    public static final int MILLISECONDS_IN_DAY = 21;
    protected static final int MINIMUM = 0;
    protected static final int MINIMUM_USER_STAMP = 2;
    public static final int MINUTE = 12;
    protected static final long MIN_MILLIS = -184303902528000000L;
    public static final int MONDAY = 2;
    public static final int MONTH = 2;
    public static final int NOVEMBER = 10;
    public static final int OCTOBER = 9;
    protected static final long ONE_DAY = 86400000;
    protected static final int ONE_HOUR = 3600000;
    protected static final int ONE_MINUTE = 60000;
    protected static final int ONE_SECOND = 1000;
    protected static final long ONE_WEEK = 604800000;
    public static final int PM = 1;
    private static final char QUOTE = '\'';
    protected static final int RESOLVE_REMAP = 32;
    public static final int SATURDAY = 7;
    public static final int SECOND = 13;
    public static final int SEPTEMBER = 8;
    public static final int SUNDAY = 1;
    public static final int THURSDAY = 5;
    public static final int TUESDAY = 3;
    public static final int UNDECIMBER = 12;
    protected static final int UNSET = 0;
    public static final int WALLTIME_FIRST = 1;
    public static final int WALLTIME_LAST = 0;
    public static final int WALLTIME_NEXT_VALID = 2;
    public static final int WEDNESDAY = 4;

    @Deprecated
    public static final int WEEKDAY = 0;

    @Deprecated
    public static final int WEEKEND = 1;

    @Deprecated
    public static final int WEEKEND_CEASE = 3;

    @Deprecated
    public static final int WEEKEND_ONSET = 2;
    public static final int WEEK_OF_MONTH = 4;
    public static final int WEEK_OF_YEAR = 3;
    public static final int YEAR = 1;
    public static final int YEAR_WOY = 17;
    public static final int ZONE_OFFSET = 15;
    private static final long serialVersionUID = 6222646104888790989L;
    private ULocale actualLocale;
    private transient boolean areAllFieldsSet;
    private transient boolean areFieldsSet;
    private transient boolean areFieldsVirtuallySet;
    private transient int[] fields;
    private int firstDayOfWeek;
    private transient int gregorianDayOfMonth;
    private transient int gregorianDayOfYear;
    private transient int gregorianMonth;
    private transient int gregorianYear;
    private transient int internalSetMask;
    private transient boolean isTimeSet;
    private boolean lenient;
    private int minimalDaysInFirstWeek;
    private transient int nextStamp;
    private int repeatedWallTime;
    private int skippedWallTime;
    private transient int[] stamp;
    private long time;
    private ULocale validLocale;
    private int weekendCease;
    private int weekendCeaseMillis;
    private int weekendOnset;
    private int weekendOnsetMillis;
    private TimeZone zone;
    protected static final Date MIN_DATE = new Date(-184303902528000000L);
    protected static final Date MAX_DATE = new Date(183882168921600000L);
    private static int STAMP_MAX = VMRuntime.SDK_VERSION_CUR_DEVELOPMENT;
    private static final ICUCache<String, PatternData> PATTERN_CACHE = new SimpleCache();
    private static final String[] DEFAULT_PATTERNS = {"HH:mm:ss z", "HH:mm:ss z", "HH:mm:ss", "HH:mm", "EEEE, yyyy MMMM dd", "yyyy MMMM d", "yyyy MMM d", "yy/MM/dd", "{1} {0}", "{1} {0}", "{1} {0}", "{1} {0}", "{1} {0}"};
    protected static final int MIN_JULIAN = -2130706432;
    private static final int[][] LIMITS = {new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[]{1, 1, 7, 7}, new int[0], new int[]{0, 0, 1, 1}, new int[]{0, 0, 11, 11}, new int[]{0, 0, 23, 23}, new int[]{0, 0, 59, 59}, new int[]{0, 0, 59, 59}, new int[]{0, 0, 999, 999}, new int[]{-43200000, -43200000, 43200000, 43200000}, new int[]{0, 0, 3600000, 3600000}, new int[0], new int[]{1, 1, 7, 7}, new int[0], new int[]{MIN_JULIAN, MIN_JULIAN, 2130706432, 2130706432}, new int[]{0, 0, 86399999, 86399999}, new int[]{0, 0, 1, 1}};
    private static final WeekDataCache WEEK_DATA_CACHE = new WeekDataCache();
    static final int[][][] DATE_PRECEDENCE = {new int[][]{new int[]{5}, new int[]{3, 7}, new int[]{4, 7}, new int[]{8, 7}, new int[]{3, 18}, new int[]{4, 18}, new int[]{8, 18}, new int[]{6}, new int[]{37, 1}, new int[]{35, 17}}, new int[][]{new int[]{3}, new int[]{4}, new int[]{8}, new int[]{40, 7}, new int[]{40, 18}}};
    static final int[][][] DOW_PRECEDENCE = {new int[][]{new int[]{7}, new int[]{18}}};
    private static final int[] FIND_ZONE_TRANSITION_TIME_UNITS = {3600000, 1800000, 60000, 1000};
    private static final int[][] GREGORIAN_MONTH_COUNT = {new int[]{31, 31, 0, 0}, new int[]{28, 29, 31, 31}, new int[]{31, 31, 59, 60}, new int[]{30, 30, 90, 91}, new int[]{31, 31, 120, 121}, new int[]{30, 30, 151, 152}, new int[]{31, 31, 181, 182}, new int[]{31, 31, 212, 213}, new int[]{30, 30, 243, 244}, new int[]{31, 31, UCharacter.UnicodeBlock.TANGUT_COMPONENTS_ID, UCharacter.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F_ID}, new int[]{30, 30, 304, 305}, new int[]{31, 31, 334, 335}};
    private static final String[] FIELD_NAME = {"ERA", "YEAR", "MONTH", "WEEK_OF_YEAR", "WEEK_OF_MONTH", "DAY_OF_MONTH", "DAY_OF_YEAR", "DAY_OF_WEEK", "DAY_OF_WEEK_IN_MONTH", "AM_PM", "HOUR", "HOUR_OF_DAY", "MINUTE", "SECOND", "MILLISECOND", "ZONE_OFFSET", "DST_OFFSET", "YEAR_WOY", "DOW_LOCAL", "EXTENDED_YEAR", "JULIAN_DAY", "MILLISECONDS_IN_DAY"};

    protected abstract int handleComputeMonthStart(int i, int i2, boolean z);

    protected abstract int handleGetExtendedYear();

    protected abstract int handleGetLimit(int i, int i2);

    protected Calendar() {
        this(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
    }

    protected Calendar(TimeZone timeZone, Locale locale) {
        this(timeZone, ULocale.forLocale(locale));
    }

    protected Calendar(TimeZone timeZone, ULocale uLocale) {
        this.lenient = true;
        this.repeatedWallTime = 0;
        this.skippedWallTime = 0;
        this.nextStamp = 2;
        this.zone = timeZone;
        setWeekData(getRegionForCalendar(uLocale));
        setCalendarLocale(uLocale);
        initInternal();
    }

    private void setCalendarLocale(ULocale uLocale) {
        if (uLocale.getVariant().length() != 0 || uLocale.getKeywords() != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(uLocale.getLanguage());
            String script = uLocale.getScript();
            if (script.length() > 0) {
                sb.append(BaseLocale.SEP);
                sb.append(script);
            }
            String country = uLocale.getCountry();
            if (country.length() > 0) {
                sb.append(BaseLocale.SEP);
                sb.append(country);
            }
            String keywordValue = uLocale.getKeywordValue("calendar");
            if (keywordValue != null) {
                sb.append("@calendar=");
                sb.append(keywordValue);
            }
            uLocale = new ULocale(sb.toString());
        }
        setLocale(uLocale, uLocale);
    }

    private void recalculateStamp() {
        this.nextStamp = 1;
        for (int i = 0; i < this.stamp.length; i++) {
            int i2 = -1;
            int i3 = STAMP_MAX;
            for (int i4 = 0; i4 < this.stamp.length; i4++) {
                if (this.stamp[i4] > this.nextStamp && this.stamp[i4] < i3) {
                    i3 = this.stamp[i4];
                    i2 = i4;
                }
            }
            if (i2 < 0) {
                break;
            }
            int[] iArr = this.stamp;
            int i5 = this.nextStamp + 1;
            this.nextStamp = i5;
            iArr[i2] = i5;
        }
        this.nextStamp++;
    }

    private void initInternal() {
        this.fields = handleCreateFields();
        if (this.fields != null) {
            if (this.fields.length >= 23 && this.fields.length <= 32) {
                this.stamp = new int[this.fields.length];
                int i = 4718695;
                for (int i2 = 23; i2 < this.fields.length; i2++) {
                    i |= 1 << i2;
                }
                this.internalSetMask = i;
                return;
            }
        }
        throw new IllegalStateException("Invalid fields[]");
    }

    public static Calendar getInstance() {
        return getInstanceInternal(null, null);
    }

    public static Calendar getInstance(TimeZone timeZone) {
        return getInstanceInternal(timeZone, null);
    }

    public static Calendar getInstance(Locale locale) {
        return getInstanceInternal(null, ULocale.forLocale(locale));
    }

    public static Calendar getInstance(ULocale uLocale) {
        return getInstanceInternal(null, uLocale);
    }

    public static Calendar getInstance(TimeZone timeZone, Locale locale) {
        return getInstanceInternal(timeZone, ULocale.forLocale(locale));
    }

    public static Calendar getInstance(TimeZone timeZone, ULocale uLocale) {
        return getInstanceInternal(timeZone, uLocale);
    }

    private static Calendar getInstanceInternal(TimeZone timeZone, ULocale uLocale) {
        if (uLocale == null) {
            uLocale = ULocale.getDefault(ULocale.Category.FORMAT);
        }
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        Calendar calendarCreateInstance = createInstance(uLocale);
        calendarCreateInstance.setTimeZone(timeZone);
        calendarCreateInstance.setTimeInMillis(System.currentTimeMillis());
        return calendarCreateInstance;
    }

    private static String getRegionForCalendar(ULocale uLocale) {
        String regionForSupplementalData = ULocale.getRegionForSupplementalData(uLocale, true);
        if (regionForSupplementalData.length() == 0) {
            return "001";
        }
        return regionForSupplementalData;
    }

    private enum CalType {
        GREGORIAN("gregorian"),
        ISO8601("iso8601"),
        BUDDHIST("buddhist"),
        CHINESE("chinese"),
        COPTIC("coptic"),
        DANGI("dangi"),
        ETHIOPIC("ethiopic"),
        ETHIOPIC_AMETE_ALEM("ethiopic-amete-alem"),
        HEBREW("hebrew"),
        INDIAN("indian"),
        ISLAMIC("islamic"),
        ISLAMIC_CIVIL("islamic-civil"),
        ISLAMIC_RGSA("islamic-rgsa"),
        ISLAMIC_TBLA("islamic-tbla"),
        ISLAMIC_UMALQURA("islamic-umalqura"),
        JAPANESE("japanese"),
        PERSIAN("persian"),
        ROC("roc"),
        UNKNOWN("unknown");

        String id;

        CalType(String str) {
            this.id = str;
        }
    }

    private static CalType getCalendarTypeForLocale(ULocale uLocale) {
        String calendarType = CalendarUtil.getCalendarType(uLocale);
        if (calendarType != null) {
            String lowerCase = calendarType.toLowerCase(Locale.ENGLISH);
            for (CalType calType : CalType.values()) {
                if (lowerCase.equals(calType.id)) {
                    return calType;
                }
            }
        }
        return CalType.UNKNOWN;
    }

    private static Calendar createInstance(ULocale uLocale) {
        TimeZone timeZone = TimeZone.getDefault();
        CalType calendarTypeForLocale = getCalendarTypeForLocale(uLocale);
        if (calendarTypeForLocale == CalType.UNKNOWN) {
            calendarTypeForLocale = CalType.GREGORIAN;
        }
        switch (calendarTypeForLocale) {
            case GREGORIAN:
                return new GregorianCalendar(timeZone, uLocale);
            case ISO8601:
                GregorianCalendar gregorianCalendar = new GregorianCalendar(timeZone, uLocale);
                gregorianCalendar.setFirstDayOfWeek(2);
                gregorianCalendar.setMinimalDaysInFirstWeek(4);
                return gregorianCalendar;
            case BUDDHIST:
                return new BuddhistCalendar(timeZone, uLocale);
            case CHINESE:
                return new ChineseCalendar(timeZone, uLocale);
            case COPTIC:
                return new CopticCalendar(timeZone, uLocale);
            case DANGI:
                return new DangiCalendar(timeZone, uLocale);
            case ETHIOPIC:
                return new EthiopicCalendar(timeZone, uLocale);
            case ETHIOPIC_AMETE_ALEM:
                EthiopicCalendar ethiopicCalendar = new EthiopicCalendar(timeZone, uLocale);
                ethiopicCalendar.setAmeteAlemEra(true);
                return ethiopicCalendar;
            case HEBREW:
                return new HebrewCalendar(timeZone, uLocale);
            case INDIAN:
                return new IndianCalendar(timeZone, uLocale);
            case ISLAMIC_CIVIL:
            case ISLAMIC_UMALQURA:
            case ISLAMIC_TBLA:
            case ISLAMIC_RGSA:
            case ISLAMIC:
                return new IslamicCalendar(timeZone, uLocale);
            case JAPANESE:
                return new JapaneseCalendar(timeZone, uLocale);
            case PERSIAN:
                return new PersianCalendar(timeZone, uLocale);
            case ROC:
                return new TaiwanCalendar(timeZone, uLocale);
            default:
                throw new IllegalArgumentException("Unknown calendar type");
        }
    }

    public static Locale[] getAvailableLocales() {
        return ICUResourceBundle.getAvailableLocales();
    }

    public static ULocale[] getAvailableULocales() {
        return ICUResourceBundle.getAvailableULocales();
    }

    public static final String[] getKeywordValuesForLocale(String str, ULocale uLocale, boolean z) {
        UResourceBundle uResourceBundle;
        String regionForSupplementalData = ULocale.getRegionForSupplementalData(uLocale, true);
        ArrayList arrayList = new ArrayList();
        UResourceBundle uResourceBundle2 = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "supplementalData", ICUResourceBundle.ICU_DATA_CLASS_LOADER).get("calendarPreferenceData");
        try {
            uResourceBundle = uResourceBundle2.get(regionForSupplementalData);
        } catch (MissingResourceException e) {
            uResourceBundle = uResourceBundle2.get("001");
        }
        String[] stringArray = uResourceBundle.getStringArray();
        if (z) {
            return stringArray;
        }
        for (String str2 : stringArray) {
            arrayList.add(str2);
        }
        for (CalType calType : CalType.values()) {
            if (!arrayList.contains(calType.id)) {
                arrayList.add(calType.id);
            }
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }

    public final Date getTime() {
        return new Date(getTimeInMillis());
    }

    public final void setTime(Date date) {
        setTimeInMillis(date.getTime());
    }

    public long getTimeInMillis() {
        if (!this.isTimeSet) {
            updateTime();
        }
        return this.time;
    }

    public void setTimeInMillis(long j) {
        if (j > 183882168921600000L) {
            if (!isLenient()) {
                throw new IllegalArgumentException("millis value greater than upper bounds for a Calendar : " + j);
            }
            j = 183882168921600000L;
        } else if (j < -184303902528000000L) {
            if (!isLenient()) {
                throw new IllegalArgumentException("millis value less than lower bounds for a Calendar : " + j);
            }
            j = -184303902528000000L;
        }
        this.time = j;
        this.areAllFieldsSet = false;
        this.areFieldsSet = false;
        this.areFieldsVirtuallySet = true;
        this.isTimeSet = true;
        for (int i = 0; i < this.fields.length; i++) {
            int[] iArr = this.fields;
            this.stamp[i] = 0;
            iArr[i] = 0;
        }
    }

    public final int get(int i) {
        complete();
        return this.fields[i];
    }

    protected final int internalGet(int i) {
        return this.fields[i];
    }

    protected final int internalGet(int i, int i2) {
        return this.stamp[i] > 0 ? this.fields[i] : i2;
    }

    public final void set(int i, int i2) {
        if (this.areFieldsVirtuallySet) {
            computeFields();
        }
        this.fields[i] = i2;
        if (this.nextStamp == STAMP_MAX) {
            recalculateStamp();
        }
        int[] iArr = this.stamp;
        int i3 = this.nextStamp;
        this.nextStamp = i3 + 1;
        iArr[i] = i3;
        this.areFieldsVirtuallySet = false;
        this.areFieldsSet = false;
        this.isTimeSet = false;
    }

    public final void set(int i, int i2, int i3) {
        set(1, i);
        set(2, i2);
        set(5, i3);
    }

    public final void set(int i, int i2, int i3, int i4, int i5) {
        set(1, i);
        set(2, i2);
        set(5, i3);
        set(11, i4);
        set(12, i5);
    }

    public final void set(int i, int i2, int i3, int i4, int i5, int i6) {
        set(1, i);
        set(2, i2);
        set(5, i3);
        set(11, i4);
        set(12, i5);
        set(13, i6);
    }

    private static int gregoYearFromIslamicStart(int i) {
        int i2;
        if (i >= 1397) {
            int i3 = i - 1397;
            i2 = (2 * (i3 / 67)) + (i3 % 67 >= 33 ? 1 : 0);
        } else {
            int i4 = i - 1396;
            i2 = (2 * ((i4 / 67) - 1)) + ((-i4) % 67 <= 33 ? 1 : 0);
        }
        return (i + 579) - i2;
    }

    @Deprecated
    public final int getRelatedYear() {
        int i = get(19);
        CalType calType = CalType.GREGORIAN;
        String type = getType();
        CalType[] calTypeArrValues = CalType.values();
        int length = calTypeArrValues.length;
        int i2 = 0;
        while (true) {
            if (i2 >= length) {
                break;
            }
            CalType calType2 = calTypeArrValues[i2];
            if (!type.equals(calType2.id)) {
                i2++;
            } else {
                calType = calType2;
                break;
            }
        }
        switch (calType) {
            case CHINESE:
                return i - 2637;
            case COPTIC:
                return i + 284;
            case DANGI:
                return i - 2333;
            case ETHIOPIC:
                return i + 8;
            case ETHIOPIC_AMETE_ALEM:
                return i - 5492;
            case HEBREW:
                return i - 3760;
            case INDIAN:
                return i + 79;
            case ISLAMIC_CIVIL:
            case ISLAMIC_UMALQURA:
            case ISLAMIC_TBLA:
            case ISLAMIC_RGSA:
            case ISLAMIC:
                return gregoYearFromIslamicStart(i);
            case JAPANESE:
            default:
                return i;
            case PERSIAN:
                return i + 622;
        }
    }

    private static int firstIslamicStartYearFromGrego(int i) {
        int i2;
        if (i >= 1977) {
            int i3 = i - 1977;
            i2 = (2 * (i3 / 65)) + (i3 % 65 >= 32 ? 1 : 0);
        } else {
            int i4 = i - 1976;
            i2 = (2 * ((i4 / 65) - 1)) + ((-i4) % 65 <= 32 ? 1 : 0);
        }
        return (i - 579) + i2;
    }

    @Deprecated
    public final void setRelatedYear(int i) {
        CalType calType = CalType.GREGORIAN;
        String type = getType();
        CalType[] calTypeArrValues = CalType.values();
        int length = calTypeArrValues.length;
        int i2 = 0;
        while (true) {
            if (i2 >= length) {
                break;
            }
            CalType calType2 = calTypeArrValues[i2];
            if (!type.equals(calType2.id)) {
                i2++;
            } else {
                calType = calType2;
                break;
            }
        }
        switch (calType) {
            case CHINESE:
                i += 2637;
                break;
            case COPTIC:
                i -= 284;
                break;
            case DANGI:
                i += 2333;
                break;
            case ETHIOPIC:
                i -= 8;
                break;
            case ETHIOPIC_AMETE_ALEM:
                i += 5492;
                break;
            case HEBREW:
                i += 3760;
                break;
            case INDIAN:
                i -= 79;
                break;
            case ISLAMIC_CIVIL:
            case ISLAMIC_UMALQURA:
            case ISLAMIC_TBLA:
            case ISLAMIC_RGSA:
            case ISLAMIC:
                i = firstIslamicStartYearFromGrego(i);
                break;
            case PERSIAN:
                i -= 622;
                break;
        }
        set(19, i);
    }

    public final void clear() {
        for (int i = 0; i < this.fields.length; i++) {
            int[] iArr = this.fields;
            this.stamp[i] = 0;
            iArr[i] = 0;
        }
        this.areFieldsVirtuallySet = false;
        this.areAllFieldsSet = false;
        this.areFieldsSet = false;
        this.isTimeSet = false;
    }

    public final void clear(int i) {
        if (this.areFieldsVirtuallySet) {
            computeFields();
        }
        this.fields[i] = 0;
        this.stamp[i] = 0;
        this.areFieldsVirtuallySet = false;
        this.areAllFieldsSet = false;
        this.areFieldsSet = false;
        this.isTimeSet = false;
    }

    public final boolean isSet(int i) {
        return this.areFieldsVirtuallySet || this.stamp[i] != 0;
    }

    protected void complete() {
        if (!this.isTimeSet) {
            updateTime();
        }
        if (!this.areFieldsSet) {
            computeFields();
            this.areFieldsSet = true;
            this.areAllFieldsSet = true;
        }
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Calendar calendar = (Calendar) obj;
        if (!isEquivalentTo(calendar) || getTimeInMillis() != calendar.getTime().getTime()) {
            return false;
        }
        return true;
    }

    public boolean isEquivalentTo(Calendar calendar) {
        return getClass() == calendar.getClass() && isLenient() == calendar.isLenient() && getFirstDayOfWeek() == calendar.getFirstDayOfWeek() && getMinimalDaysInFirstWeek() == calendar.getMinimalDaysInFirstWeek() && getTimeZone().equals(calendar.getTimeZone()) && getRepeatedWallTimeOption() == calendar.getRepeatedWallTimeOption() && getSkippedWallTimeOption() == calendar.getSkippedWallTimeOption();
    }

    public int hashCode() {
        boolean z = this.lenient;
        return (z ? 1 : 0) | (this.firstDayOfWeek << 1) | (this.minimalDaysInFirstWeek << 4) | (this.repeatedWallTime << 7) | (this.skippedWallTime << 9) | (this.zone.hashCode() << 11);
    }

    private long compare(Object obj) {
        long time;
        if (obj instanceof Calendar) {
            time = ((Calendar) obj).getTimeInMillis();
        } else if (obj instanceof Date) {
            time = ((Date) obj).getTime();
        } else {
            throw new IllegalArgumentException(obj + "is not a Calendar or Date");
        }
        return getTimeInMillis() - time;
    }

    public boolean before(Object obj) {
        return compare(obj) < 0;
    }

    public boolean after(Object obj) {
        return compare(obj) > 0;
    }

    public int getActualMaximum(int i) {
        switch (i) {
            case 0:
            case 7:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 18:
            case 20:
            case 21:
                return getMaximum(i);
            case 1:
            case 2:
            case 3:
            case 4:
            case 8:
            case 17:
            case 19:
            default:
                return getActualHelper(i, getLeastMaximum(i), getMaximum(i));
            case 5:
                Calendar calendar = (Calendar) clone();
                calendar.setLenient(true);
                calendar.prepareGetActual(i, false);
                return handleGetMonthLength(calendar.get(19), calendar.get(2));
            case 6:
                Calendar calendar2 = (Calendar) clone();
                calendar2.setLenient(true);
                calendar2.prepareGetActual(i, false);
                return handleGetYearLength(calendar2.get(19));
        }
    }

    public int getActualMinimum(int i) {
        switch (i) {
            case 7:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 18:
            case 20:
            case 21:
                return getMinimum(i);
            case 8:
            case 17:
            case 19:
            default:
                return getActualHelper(i, getGreatestMinimum(i), getMinimum(i));
        }
    }

    protected void prepareGetActual(int i, boolean z) {
        set(21, 0);
        if (i == 8) {
            set(5, 1);
            set(7, get(7));
        } else if (i == 17) {
            set(3, getGreatestMinimum(3));
        } else if (i != 19) {
            switch (i) {
                case 1:
                    set(6, getGreatestMinimum(6));
                    break;
                case 2:
                    set(5, getGreatestMinimum(5));
                    break;
                case 3:
                case 4:
                    int i2 = this.firstDayOfWeek;
                    if (z && (i2 = (i2 + 6) % 7) < 1) {
                        i2 += 7;
                    }
                    set(7, i2);
                    break;
            }
        }
        set(i, getGreatestMinimum(i));
    }

    private int getActualHelper(int i, int i2, int i3) {
        int i4;
        if (i2 == i3) {
            return i2;
        }
        if (i3 <= i2) {
            i4 = -1;
        } else {
            i4 = 1;
        }
        Calendar calendar = (Calendar) clone();
        calendar.complete();
        calendar.setLenient(true);
        calendar.prepareGetActual(i, i4 < 0);
        calendar.set(i, i2);
        if (calendar.get(i) != i2 && i != 4 && i4 > 0) {
            return i2;
        }
        while (true) {
            int i5 = i2 + i4;
            calendar.add(i, i4);
            if (calendar.get(i) != i5) {
                return i2;
            }
            if (i5 != i3) {
                i2 = i5;
            } else {
                return i5;
            }
        }
    }

    public final void roll(int i, boolean z) {
        roll(i, z ? 1 : -1);
    }

    public void roll(int i, int i2) {
        int i3;
        int i4;
        if (i2 == 0) {
            return;
        }
        complete();
        switch (i) {
            case 0:
            case 5:
            case 9:
            case 12:
            case 13:
            case 14:
            case 21:
                int actualMinimum = getActualMinimum(i);
                int actualMaximum = (getActualMaximum(i) - actualMinimum) + 1;
                int iInternalGet = ((internalGet(i) + i2) - actualMinimum) % actualMaximum;
                if (iInternalGet < 0) {
                    iInternalGet += actualMaximum;
                }
                set(i, iInternalGet + actualMinimum);
                return;
            case 1:
            case 17:
                boolean z = false;
                int i5 = get(0);
                if (i5 == 0) {
                    String type = getType();
                    if (type.equals("gregorian") || type.equals("roc") || type.equals("coptic")) {
                        i2 = -i2;
                        z = true;
                    }
                }
                int iInternalGet2 = i2 + internalGet(i);
                if (i5 > 0 || iInternalGet2 >= 1) {
                    int actualMaximum2 = getActualMaximum(i);
                    if (actualMaximum2 < 32768) {
                        if (iInternalGet2 < 1) {
                            i = actualMaximum2 - ((-iInternalGet2) % actualMaximum2);
                        } else {
                            if (iInternalGet2 > actualMaximum2) {
                                iInternalGet2 = ((iInternalGet2 - 1) % actualMaximum2) + 1;
                            }
                            i = iInternalGet2;
                        }
                    } else if (iInternalGet2 >= 1) {
                        i = iInternalGet2;
                    }
                } else if (!z) {
                    i = iInternalGet2;
                }
                set(i, i);
                pinField(2);
                pinField(5);
                return;
            case 2:
                int actualMaximum3 = getActualMaximum(2) + 1;
                int iInternalGet3 = (internalGet(2) + i2) % actualMaximum3;
                if (iInternalGet3 < 0) {
                    iInternalGet3 += actualMaximum3;
                }
                set(2, iInternalGet3);
                pinField(5);
                return;
            case 3:
                int iInternalGet4 = internalGet(7) - getFirstDayOfWeek();
                if (iInternalGet4 < 0) {
                    iInternalGet4 += 7;
                }
                int iInternalGet5 = ((iInternalGet4 - internalGet(6)) + 1) % 7;
                if (iInternalGet5 < 0) {
                    iInternalGet5 += 7;
                }
                if (7 - iInternalGet5 < getMinimalDaysInFirstWeek()) {
                    i3 = 8 - iInternalGet5;
                } else {
                    i3 = 1 - iInternalGet5;
                }
                int actualMaximum4 = getActualMaximum(6);
                int iInternalGet6 = ((actualMaximum4 + 7) - (((actualMaximum4 - internalGet(6)) + iInternalGet4) % 7)) - i3;
                int iInternalGet7 = ((internalGet(6) + (i2 * 7)) - i3) % iInternalGet6;
                if (iInternalGet7 < 0) {
                    iInternalGet7 += iInternalGet6;
                }
                int i6 = iInternalGet7 + i3;
                if (i6 < 1) {
                    i6 = 1;
                }
                if (i6 > actualMaximum4) {
                    i6 = actualMaximum4;
                }
                set(6, i6);
                clear(2);
                return;
            case 4:
                int iInternalGet8 = internalGet(7) - getFirstDayOfWeek();
                if (iInternalGet8 < 0) {
                    iInternalGet8 += 7;
                }
                int iInternalGet9 = ((iInternalGet8 - internalGet(5)) + 1) % 7;
                if (iInternalGet9 < 0) {
                    iInternalGet9 += 7;
                }
                if (7 - iInternalGet9 < getMinimalDaysInFirstWeek()) {
                    i4 = 8 - iInternalGet9;
                } else {
                    i4 = 1 - iInternalGet9;
                }
                int actualMaximum5 = getActualMaximum(5);
                int iInternalGet10 = ((actualMaximum5 + 7) - (((actualMaximum5 - internalGet(5)) + iInternalGet8) % 7)) - i4;
                int iInternalGet11 = ((internalGet(5) + (i2 * 7)) - i4) % iInternalGet10;
                if (iInternalGet11 < 0) {
                    iInternalGet11 += iInternalGet10;
                }
                int i7 = iInternalGet11 + i4;
                if (i7 < 1) {
                    i7 = 1;
                }
                if (i7 > actualMaximum5) {
                    i7 = actualMaximum5;
                }
                set(5, i7);
                return;
            case 6:
                long jInternalGet = this.time - (((long) (internalGet(6) - 1)) * 86400000);
                long actualMaximum6 = ((long) getActualMaximum(6)) * 86400000;
                this.time = ((this.time + (((long) i2) * 86400000)) - jInternalGet) % actualMaximum6;
                if (this.time < 0) {
                    this.time += actualMaximum6;
                }
                setTimeInMillis(this.time + jInternalGet);
                return;
            case 7:
            case 18:
                long j = ((long) i2) * 86400000;
                int iInternalGet12 = internalGet(i) - (i == 7 ? getFirstDayOfWeek() : 1);
                if (iInternalGet12 < 0) {
                    iInternalGet12 += 7;
                }
                long j2 = this.time - (((long) iInternalGet12) * 86400000);
                this.time = ((this.time + j) - j2) % 604800000;
                if (this.time < 0) {
                    this.time += 604800000;
                }
                setTimeInMillis(this.time + j2);
                return;
            case 8:
                int iInternalGet13 = (internalGet(5) - 1) / 7;
                int actualMaximum7 = (getActualMaximum(5) - internalGet(5)) / 7;
                long j3 = this.time - (((long) iInternalGet13) * 604800000);
                long j4 = 604800000 * ((long) (iInternalGet13 + actualMaximum7 + 1));
                this.time = ((this.time + (((long) i2) * 604800000)) - j3) % j4;
                if (this.time < 0) {
                    this.time += j4;
                }
                setTimeInMillis(this.time + j3);
                return;
            case 10:
            case 11:
                long timeInMillis = getTimeInMillis();
                int iInternalGet14 = internalGet(i);
                int maximum = getMaximum(i) + 1;
                int i8 = (i2 + iInternalGet14) % maximum;
                if (i8 < 0) {
                    i8 += maximum;
                }
                setTimeInMillis(timeInMillis + (RelativeDateTimeFormatter.HOUR_IN_MILLIS * (((long) i8) - ((long) iInternalGet14))));
                return;
            case 15:
            case 16:
            default:
                throw new IllegalArgumentException("Calendar.roll(" + fieldName(i) + ") not supported");
            case 19:
                set(i, internalGet(i) + i2);
                pinField(2);
                pinField(5);
                return;
            case 20:
                set(i, internalGet(i) + i2);
                return;
        }
    }

    public void add(int i, int i2) {
        boolean zIsLenient;
        int i3;
        int i4;
        if (i2 == 0) {
            return;
        }
        long j = i2;
        boolean z = true;
        int i5 = 0;
        switch (i) {
            case 0:
                set(i, get(i) + i2);
                pinField(0);
                return;
            case 1:
            case 17:
                if (get(0) == 0) {
                    String type = getType();
                    if (type.equals("gregorian") || type.equals("roc") || type.equals("coptic")) {
                        i2 = -i2;
                    }
                }
                zIsLenient = isLenient();
                setLenient(true);
                set(i, get(i) + i2);
                pinField(5);
                if (zIsLenient) {
                    complete();
                    setLenient(zIsLenient);
                    return;
                }
                return;
            case 2:
            case 19:
                zIsLenient = isLenient();
                setLenient(true);
                set(i, get(i) + i2);
                pinField(5);
                if (zIsLenient) {
                }
                break;
            case 3:
            case 4:
            case 8:
                j *= 604800000;
                if (z) {
                    i3 = get(16) + get(15);
                    i5 = get(21);
                } else {
                    i3 = 0;
                }
                setTimeInMillis(getTimeInMillis() + j);
                if (z && (i4 = get(21)) != i5) {
                    long jInternalGetTimeInMillis = internalGetTimeInMillis();
                    int i6 = get(16) + get(15);
                    if (i6 != i3) {
                        long j2 = ((long) (i3 - i6)) % 86400000;
                        if (j2 != 0) {
                            setTimeInMillis(j2 + jInternalGetTimeInMillis);
                            i4 = get(21);
                        }
                        if (i4 != i5) {
                            switch (this.skippedWallTime) {
                                case 0:
                                    if (j2 < 0) {
                                        setTimeInMillis(jInternalGetTimeInMillis);
                                        return;
                                    }
                                    return;
                                case 1:
                                    if (j2 > 0) {
                                        setTimeInMillis(jInternalGetTimeInMillis);
                                        return;
                                    }
                                    return;
                                case 2:
                                    if (j2 > 0) {
                                        jInternalGetTimeInMillis = internalGetTimeInMillis();
                                    }
                                    Long immediatePreviousZoneTransition = getImmediatePreviousZoneTransition(jInternalGetTimeInMillis);
                                    if (immediatePreviousZoneTransition != null) {
                                        setTimeInMillis(immediatePreviousZoneTransition.longValue());
                                        return;
                                    }
                                    throw new RuntimeException("Could not locate a time zone transition before " + jInternalGetTimeInMillis);
                                default:
                                    return;
                            }
                        }
                        return;
                    }
                    return;
                }
                return;
            case 5:
            case 6:
            case 7:
            case 18:
            case 20:
                j *= 86400000;
                if (z) {
                }
                setTimeInMillis(getTimeInMillis() + j);
                if (z) {
                    return;
                } else {
                    return;
                }
            case 9:
                j *= 43200000;
                if (z) {
                }
                setTimeInMillis(getTimeInMillis() + j);
                if (z) {
                }
                break;
            case 10:
            case 11:
                j *= RelativeDateTimeFormatter.HOUR_IN_MILLIS;
                z = false;
                if (z) {
                }
                setTimeInMillis(getTimeInMillis() + j);
                if (z) {
                }
                break;
            case 12:
                j *= RelativeDateTimeFormatter.MINUTE_IN_MILLIS;
                z = false;
                if (z) {
                }
                setTimeInMillis(getTimeInMillis() + j);
                if (z) {
                }
                break;
            case 13:
                j *= 1000;
                z = false;
                if (z) {
                }
                setTimeInMillis(getTimeInMillis() + j);
                if (z) {
                }
                break;
            case 14:
            case 21:
                z = false;
                if (z) {
                }
                setTimeInMillis(getTimeInMillis() + j);
                if (z) {
                }
                break;
            case 15:
            case 16:
            default:
                throw new IllegalArgumentException("Calendar.add(" + fieldName(i) + ") not supported");
        }
    }

    public String getDisplayName(Locale locale) {
        return getClass().getName();
    }

    public String getDisplayName(ULocale uLocale) {
        return getClass().getName();
    }

    @Override
    public int compareTo(Calendar calendar) {
        long timeInMillis = getTimeInMillis() - calendar.getTimeInMillis();
        if (timeInMillis < 0) {
            return -1;
        }
        return timeInMillis > 0 ? 1 : 0;
    }

    public DateFormat getDateTimeFormat(int i, int i2, Locale locale) {
        return formatHelper(this, ULocale.forLocale(locale), i, i2);
    }

    public DateFormat getDateTimeFormat(int i, int i2, ULocale uLocale) {
        return formatHelper(this, uLocale, i, i2);
    }

    protected DateFormat handleGetDateFormat(String str, Locale locale) {
        return handleGetDateFormat(str, (String) null, ULocale.forLocale(locale));
    }

    protected DateFormat handleGetDateFormat(String str, String str2, Locale locale) {
        return handleGetDateFormat(str, str2, ULocale.forLocale(locale));
    }

    protected DateFormat handleGetDateFormat(String str, ULocale uLocale) {
        return handleGetDateFormat(str, (String) null, uLocale);
    }

    protected DateFormat handleGetDateFormat(String str, String str2, ULocale uLocale) {
        FormatConfiguration formatConfiguration = new FormatConfiguration();
        formatConfiguration.pattern = str;
        formatConfiguration.override = str2;
        formatConfiguration.formatData = new DateFormatSymbols(this, uLocale);
        formatConfiguration.loc = uLocale;
        formatConfiguration.cal = this;
        return SimpleDateFormat.getInstance(formatConfiguration);
    }

    private static DateFormat formatHelper(Calendar calendar, ULocale uLocale, int i, int i2) {
        String rawPattern;
        if (i2 < -1 || i2 > 3) {
            throw new IllegalArgumentException("Illegal time style " + i2);
        }
        if (i < -1 || i > 3) {
            throw new IllegalArgumentException("Illegal date style " + i);
        }
        PatternData patternDataMake = PatternData.make(calendar, uLocale);
        String strMergeOverrideStrings = null;
        if (i2 < 0 || i < 0) {
            if (i2 < 0) {
                if (i < 0) {
                    throw new IllegalArgumentException("No date or time style specified");
                }
                int i3 = i + 4;
                rawPattern = patternDataMake.patterns[i3];
                if (patternDataMake.overrides != null) {
                    strMergeOverrideStrings = patternDataMake.overrides[i3];
                }
            } else {
                rawPattern = patternDataMake.patterns[i2];
                if (patternDataMake.overrides != null) {
                    strMergeOverrideStrings = patternDataMake.overrides[i2];
                }
            }
        } else {
            String dateTimePattern = patternDataMake.getDateTimePattern(i);
            int i4 = i + 4;
            rawPattern = SimpleFormatterImpl.formatRawPattern(dateTimePattern, 2, 2, patternDataMake.patterns[i2], patternDataMake.patterns[i4]);
            if (patternDataMake.overrides != null) {
                strMergeOverrideStrings = mergeOverrideStrings(patternDataMake.patterns[i4], patternDataMake.patterns[i2], patternDataMake.overrides[i4], patternDataMake.overrides[i2]);
            }
        }
        DateFormat dateFormatHandleGetDateFormat = calendar.handleGetDateFormat(rawPattern, strMergeOverrideStrings, uLocale);
        dateFormatHandleGetDateFormat.setCalendar(calendar);
        return dateFormatHandleGetDateFormat;
    }

    public static String getDateTimeFormatString(ULocale uLocale, String str, int i, int i2) {
        if (i2 < -1 || i2 > 3) {
            throw new IllegalArgumentException("Illegal time style " + i2);
        }
        if (i < -1 || i > 3) {
            throw new IllegalArgumentException("Illegal date style " + i);
        }
        PatternData patternDataMake = PatternData.make(uLocale, str);
        if (i2 >= 0 && i >= 0) {
            return SimpleFormatterImpl.formatRawPattern(patternDataMake.getDateTimePattern(i), 2, 2, patternDataMake.patterns[i2], patternDataMake.patterns[i + 4]);
        }
        if (i2 < 0) {
            if (i < 0) {
                throw new IllegalArgumentException("No date or time style specified");
            }
            return patternDataMake.patterns[i + 4];
        }
        return patternDataMake.patterns[i2];
    }

    static class PatternData {
        private String[] overrides;
        private String[] patterns;

        public PatternData(String[] strArr, String[] strArr2) {
            this.patterns = strArr;
            this.overrides = strArr2;
        }

        private String getDateTimePattern(int i) {
            return this.patterns[this.patterns.length >= 13 ? 8 + i + 1 : 8];
        }

        private static PatternData make(Calendar calendar, ULocale uLocale) {
            return make(uLocale, calendar.getType());
        }

        private static PatternData make(ULocale uLocale, String str) {
            PatternData patternData;
            String str2 = uLocale.getBaseName() + "+" + str;
            PatternData patternData2 = (PatternData) Calendar.PATTERN_CACHE.get(str2);
            if (patternData2 == null) {
                try {
                    patternData = Calendar.getPatternData(uLocale, str);
                } catch (MissingResourceException e) {
                    patternData = new PatternData(Calendar.DEFAULT_PATTERNS, null);
                }
                patternData2 = patternData;
                Calendar.PATTERN_CACHE.put(str2, patternData2);
            }
            return patternData2;
        }
    }

    private static PatternData getPatternData(ULocale uLocale, String str) {
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, uLocale);
        ICUResourceBundle iCUResourceBundleFindWithFallback = iCUResourceBundle.findWithFallback("calendar/" + str + "/DateTimePatterns");
        if (iCUResourceBundleFindWithFallback == null) {
            iCUResourceBundleFindWithFallback = iCUResourceBundle.getWithFallback("calendar/gregorian/DateTimePatterns");
        }
        int size = iCUResourceBundleFindWithFallback.getSize();
        String[] strArr = new String[size];
        String[] strArr2 = new String[size];
        for (int i = 0; i < size; i++) {
            ICUResourceBundle iCUResourceBundle2 = (ICUResourceBundle) iCUResourceBundleFindWithFallback.get(i);
            int type = iCUResourceBundle2.getType();
            if (type == 0) {
                strArr[i] = iCUResourceBundle2.getString();
            } else if (type == 8) {
                strArr[i] = iCUResourceBundle2.getString(0);
                strArr2[i] = iCUResourceBundle2.getString(1);
            }
        }
        return new PatternData(strArr, strArr2);
    }

    @Deprecated
    public static String getDateTimePattern(Calendar calendar, ULocale uLocale, int i) {
        return PatternData.make(calendar, uLocale).getDateTimePattern(i);
    }

    private static String mergeOverrideStrings(String str, String str2, String str3, String str4) {
        if (str3 == null && str4 == null) {
            return null;
        }
        if (str3 == null) {
            return expandOverride(str2, str4);
        }
        if (str4 == null) {
            return expandOverride(str, str3);
        }
        if (str3.equals(str4)) {
            return str3;
        }
        return expandOverride(str, str3) + ";" + expandOverride(str2, str4);
    }

    private static String expandOverride(String str, String str2) {
        if (str2.indexOf(61) >= 0) {
            return str2;
        }
        boolean z = false;
        char c = ' ';
        StringBuilder sb = new StringBuilder();
        StringCharacterIterator stringCharacterIterator = new StringCharacterIterator(str);
        char cFirst = stringCharacterIterator.first();
        while (true) {
            char c2 = c;
            c = cFirst;
            if (c != 65535) {
                if (c == '\'') {
                    z = !z;
                } else if (!z && c != c2) {
                    if (sb.length() > 0) {
                        sb.append(";");
                    }
                    sb.append(c);
                    sb.append("=");
                    sb.append(str2);
                }
                cFirst = stringCharacterIterator.next();
            } else {
                return sb.toString();
            }
        }
    }

    @Deprecated
    public static class FormatConfiguration {
        private Calendar cal;
        private DateFormatSymbols formatData;
        private ULocale loc;
        private String override;
        private String pattern;

        private FormatConfiguration() {
        }

        @Deprecated
        public String getPatternString() {
            return this.pattern;
        }

        @Deprecated
        public String getOverrideString() {
            return this.override;
        }

        @Deprecated
        public Calendar getCalendar() {
            return this.cal;
        }

        @Deprecated
        public ULocale getLocale() {
            return this.loc;
        }

        @Deprecated
        public DateFormatSymbols getDateFormatSymbols() {
            return this.formatData;
        }
    }

    protected void pinField(int i) {
        int actualMaximum = getActualMaximum(i);
        int actualMinimum = getActualMinimum(i);
        if (this.fields[i] > actualMaximum) {
            set(i, actualMaximum);
        } else if (this.fields[i] < actualMinimum) {
            set(i, actualMinimum);
        }
    }

    protected int weekNumber(int i, int i2, int i3) {
        int firstDayOfWeek = (((i3 - getFirstDayOfWeek()) - i2) + 1) % 7;
        if (firstDayOfWeek < 0) {
            firstDayOfWeek += 7;
        }
        int i4 = ((i + firstDayOfWeek) - 1) / 7;
        return 7 - firstDayOfWeek >= getMinimalDaysInFirstWeek() ? i4 + 1 : i4;
    }

    protected final int weekNumber(int i, int i2) {
        return weekNumber(i, i, i2);
    }

    public int fieldDifference(Date date, int i) {
        long timeInMillis = getTimeInMillis();
        long time = date.getTime();
        int i2 = 0;
        if (timeInMillis < time) {
            int i3 = 0;
            int i4 = 1;
            while (true) {
                setTimeInMillis(timeInMillis);
                add(i, i4);
                long timeInMillis2 = getTimeInMillis();
                if (timeInMillis2 == time) {
                    return i4;
                }
                if (timeInMillis2 > time) {
                    while (true) {
                        int i5 = i4 - i3;
                        if (i5 > 1) {
                            int i6 = (i5 / 2) + i3;
                            setTimeInMillis(timeInMillis);
                            add(i, i6);
                            long timeInMillis3 = getTimeInMillis();
                            if (timeInMillis3 == time) {
                                return i6;
                            }
                            if (timeInMillis3 > time) {
                                i4 = i6;
                            } else {
                                i3 = i6;
                            }
                        } else {
                            i2 = i3;
                            break;
                        }
                    }
                } else {
                    int i7 = Integer.MAX_VALUE;
                    if (i4 < Integer.MAX_VALUE) {
                        int i8 = i4 << 1;
                        if (i8 >= 0) {
                            i7 = i8;
                        }
                        int i9 = i4;
                        i4 = i7;
                        i3 = i9;
                    } else {
                        throw new RuntimeException();
                    }
                }
            }
        } else if (timeInMillis > time) {
            int i10 = -1;
            do {
                int i11 = i2;
                i2 = i10;
                setTimeInMillis(timeInMillis);
                add(i, i2);
                long timeInMillis4 = getTimeInMillis();
                if (timeInMillis4 == time) {
                    return i2;
                }
                if (timeInMillis4 >= time) {
                    i10 = i2 << 1;
                } else {
                    i2 = i11;
                    int i12 = i2;
                    while (i2 - i12 > 1) {
                        int i13 = ((i12 - i2) / 2) + i2;
                        setTimeInMillis(timeInMillis);
                        add(i, i13);
                        long timeInMillis5 = getTimeInMillis();
                        if (timeInMillis5 == time) {
                            return i13;
                        }
                        if (timeInMillis5 < time) {
                            i12 = i13;
                        } else {
                            i2 = i13;
                        }
                    }
                }
            } while (i10 != 0);
            throw new RuntimeException();
        }
        setTimeInMillis(timeInMillis);
        add(i, i2);
        return i2;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.zone = timeZone;
        this.areFieldsSet = false;
    }

    public TimeZone getTimeZone() {
        return this.zone;
    }

    public void setLenient(boolean z) {
        this.lenient = z;
    }

    public boolean isLenient() {
        return this.lenient;
    }

    public void setRepeatedWallTimeOption(int i) {
        if (i != 0 && i != 1) {
            throw new IllegalArgumentException("Illegal repeated wall time option - " + i);
        }
        this.repeatedWallTime = i;
    }

    public int getRepeatedWallTimeOption() {
        return this.repeatedWallTime;
    }

    public void setSkippedWallTimeOption(int i) {
        if (i != 0 && i != 1 && i != 2) {
            throw new IllegalArgumentException("Illegal skipped wall time option - " + i);
        }
        this.skippedWallTime = i;
    }

    public int getSkippedWallTimeOption() {
        return this.skippedWallTime;
    }

    public void setFirstDayOfWeek(int i) {
        if (this.firstDayOfWeek != i) {
            if (i < 1 || i > 7) {
                throw new IllegalArgumentException("Invalid day of week");
            }
            this.firstDayOfWeek = i;
            this.areFieldsSet = false;
        }
    }

    public int getFirstDayOfWeek() {
        return this.firstDayOfWeek;
    }

    public void setMinimalDaysInFirstWeek(int i) {
        if (i >= 1) {
            if (i > 7) {
                i = 7;
            }
        } else {
            i = 1;
        }
        if (this.minimalDaysInFirstWeek != i) {
            this.minimalDaysInFirstWeek = i;
            this.areFieldsSet = false;
        }
    }

    public int getMinimalDaysInFirstWeek() {
        return this.minimalDaysInFirstWeek;
    }

    protected int getLimit(int i, int i2) {
        switch (i) {
            case 4:
                if (i2 == 0) {
                    return getMinimalDaysInFirstWeek() == 1 ? 1 : 0;
                }
                if (i2 == 1) {
                    return 1;
                }
                int minimalDaysInFirstWeek = getMinimalDaysInFirstWeek();
                int iHandleGetLimit = handleGetLimit(5, i2);
                if (i2 == 2) {
                    return (iHandleGetLimit + (7 - minimalDaysInFirstWeek)) / 7;
                }
                return ((iHandleGetLimit + 6) + (7 - minimalDaysInFirstWeek)) / 7;
            case 5:
            case 6:
            case 8:
            case 17:
            case 19:
            default:
                return handleGetLimit(i, i2);
            case 7:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 18:
            case 20:
            case 21:
            case 22:
                return LIMITS[i][i2];
        }
    }

    public final int getMinimum(int i) {
        return getLimit(i, 0);
    }

    public final int getMaximum(int i) {
        return getLimit(i, 3);
    }

    public final int getGreatestMinimum(int i) {
        return getLimit(i, 1);
    }

    public final int getLeastMaximum(int i) {
        return getLimit(i, 2);
    }

    @Deprecated
    public int getDayOfWeekType(int i) {
        if (i < 1 || i > 7) {
            throw new IllegalArgumentException("Invalid day of week");
        }
        if (this.weekendOnset == this.weekendCease) {
            if (i != this.weekendOnset) {
                return 0;
            }
            return this.weekendOnsetMillis == 0 ? 1 : 2;
        }
        if (this.weekendOnset < this.weekendCease) {
            if (i < this.weekendOnset || i > this.weekendCease) {
                return 0;
            }
        } else if (i > this.weekendCease && i < this.weekendOnset) {
            return 0;
        }
        return i == this.weekendOnset ? this.weekendOnsetMillis == 0 ? 1 : 2 : (i != this.weekendCease || this.weekendCeaseMillis >= 86400000) ? 1 : 3;
    }

    @Deprecated
    public int getWeekendTransition(int i) {
        if (i == this.weekendOnset) {
            return this.weekendOnsetMillis;
        }
        if (i == this.weekendCease) {
            return this.weekendCeaseMillis;
        }
        throw new IllegalArgumentException("Not weekend transition day");
    }

    public boolean isWeekend(Date date) {
        setTime(date);
        return isWeekend();
    }

    public boolean isWeekend() {
        int i = get(7);
        int dayOfWeekType = getDayOfWeekType(i);
        switch (dayOfWeekType) {
            case 0:
                break;
            case 1:
                break;
            default:
                int iInternalGet = internalGet(14) + (1000 * (internalGet(13) + (60 * (internalGet(12) + (internalGet(11) * 60)))));
                int weekendTransition = getWeekendTransition(i);
                if (dayOfWeekType == 2) {
                    if (iInternalGet < weekendTransition) {
                    }
                } else if (iInternalGet >= weekendTransition) {
                }
                break;
        }
        return true;
    }

    public Object clone() {
        try {
            Calendar calendar = (Calendar) super.clone();
            calendar.fields = new int[this.fields.length];
            calendar.stamp = new int[this.fields.length];
            System.arraycopy(this.fields, 0, calendar.fields, 0, this.fields.length);
            System.arraycopy(this.stamp, 0, calendar.stamp, 0, this.fields.length);
            calendar.zone = (TimeZone) this.zone.clone();
            return calendar;
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName());
        sb.append("[time=");
        sb.append(this.isTimeSet ? String.valueOf(this.time) : "?");
        sb.append(",areFieldsSet=");
        sb.append(this.areFieldsSet);
        sb.append(",areAllFieldsSet=");
        sb.append(this.areAllFieldsSet);
        sb.append(",lenient=");
        sb.append(this.lenient);
        sb.append(",zone=");
        sb.append(this.zone);
        sb.append(",firstDayOfWeek=");
        sb.append(this.firstDayOfWeek);
        sb.append(",minimalDaysInFirstWeek=");
        sb.append(this.minimalDaysInFirstWeek);
        sb.append(",repeatedWallTime=");
        sb.append(this.repeatedWallTime);
        sb.append(",skippedWallTime=");
        sb.append(this.skippedWallTime);
        for (int i = 0; i < this.fields.length; i++) {
            sb.append(',');
            sb.append(fieldName(i));
            sb.append('=');
            sb.append(isSet(i) ? String.valueOf(this.fields[i]) : "?");
        }
        sb.append(']');
        return sb.toString();
    }

    public static final class WeekData {
        public final int firstDayOfWeek;
        public final int minimalDaysInFirstWeek;
        public final int weekendCease;
        public final int weekendCeaseMillis;
        public final int weekendOnset;
        public final int weekendOnsetMillis;

        public WeekData(int i, int i2, int i3, int i4, int i5, int i6) {
            this.firstDayOfWeek = i;
            this.minimalDaysInFirstWeek = i2;
            this.weekendOnset = i3;
            this.weekendOnsetMillis = i4;
            this.weekendCease = i5;
            this.weekendCeaseMillis = i6;
        }

        public int hashCode() {
            return (((((((((this.firstDayOfWeek * 37) + this.minimalDaysInFirstWeek) * 37) + this.weekendOnset) * 37) + this.weekendOnsetMillis) * 37) + this.weekendCease) * 37) + this.weekendCeaseMillis;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof WeekData)) {
                return false;
            }
            WeekData weekData = (WeekData) obj;
            return this.firstDayOfWeek == weekData.firstDayOfWeek && this.minimalDaysInFirstWeek == weekData.minimalDaysInFirstWeek && this.weekendOnset == weekData.weekendOnset && this.weekendOnsetMillis == weekData.weekendOnsetMillis && this.weekendCease == weekData.weekendCease && this.weekendCeaseMillis == weekData.weekendCeaseMillis;
        }

        public String toString() {
            return "{" + this.firstDayOfWeek + ", " + this.minimalDaysInFirstWeek + ", " + this.weekendOnset + ", " + this.weekendOnsetMillis + ", " + this.weekendCease + ", " + this.weekendCeaseMillis + "}";
        }
    }

    public static WeekData getWeekDataForRegion(String str) {
        return WEEK_DATA_CACHE.createInstance(str, str);
    }

    public WeekData getWeekData() {
        return new WeekData(this.firstDayOfWeek, this.minimalDaysInFirstWeek, this.weekendOnset, this.weekendOnsetMillis, this.weekendCease, this.weekendCeaseMillis);
    }

    public Calendar setWeekData(WeekData weekData) {
        setFirstDayOfWeek(weekData.firstDayOfWeek);
        setMinimalDaysInFirstWeek(weekData.minimalDaysInFirstWeek);
        this.weekendOnset = weekData.weekendOnset;
        this.weekendOnsetMillis = weekData.weekendOnsetMillis;
        this.weekendCease = weekData.weekendCease;
        this.weekendCeaseMillis = weekData.weekendCeaseMillis;
        return this;
    }

    private static WeekData getWeekDataForRegionInternal(String str) {
        UResourceBundle uResourceBundle;
        if (str == null) {
            str = "001";
        }
        UResourceBundle uResourceBundle2 = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "supplementalData", ICUResourceBundle.ICU_DATA_CLASS_LOADER).get("weekData");
        try {
            uResourceBundle = uResourceBundle2.get(str);
        } catch (MissingResourceException e) {
            if (!str.equals("001")) {
                uResourceBundle = uResourceBundle2.get("001");
            } else {
                throw e;
            }
        }
        int[] intVector = uResourceBundle.getIntVector();
        return new WeekData(intVector[0], intVector[1], intVector[2], intVector[3], intVector[4], intVector[5]);
    }

    private static class WeekDataCache extends SoftCache<String, WeekData, String> {
        private WeekDataCache() {
        }

        @Override
        protected WeekData createInstance(String str, String str2) {
            return Calendar.getWeekDataForRegionInternal(str);
        }
    }

    private void setWeekData(String str) {
        if (str == null) {
            str = "001";
        }
        setWeekData(WEEK_DATA_CACHE.getInstance(str, str));
    }

    private void updateTime() {
        computeTime();
        if (isLenient() || !this.areAllFieldsSet) {
            this.areFieldsSet = false;
        }
        this.isTimeSet = true;
        this.areFieldsVirtuallySet = false;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        if (!this.isTimeSet) {
            try {
                updateTime();
            } catch (IllegalArgumentException e) {
            }
        }
        objectOutputStream.defaultWriteObject();
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        initInternal();
        this.isTimeSet = true;
        this.areAllFieldsSet = false;
        this.areFieldsSet = false;
        this.areFieldsVirtuallySet = true;
        this.nextStamp = 2;
    }

    protected void computeFields() {
        int[] iArr = new int[2];
        getTimeZone().getOffset(this.time, false, iArr);
        long j = this.time + ((long) iArr[0]) + ((long) iArr[1]);
        int i = this.internalSetMask;
        for (int i2 = 0; i2 < this.fields.length; i2++) {
            if ((i & 1) == 0) {
                this.stamp[i2] = 1;
            } else {
                this.stamp[i2] = 0;
            }
            i >>= 1;
        }
        long jFloorDivide = floorDivide(j, 86400000L);
        this.fields[20] = ((int) jFloorDivide) + EPOCH_JULIAN_DAY;
        computeGregorianAndDOWFields(this.fields[20]);
        handleComputeFields(this.fields[20]);
        computeWeekFields();
        int i3 = (int) (j - (jFloorDivide * 86400000));
        this.fields[21] = i3;
        this.fields[14] = i3 % 1000;
        int i4 = i3 / 1000;
        this.fields[13] = i4 % 60;
        int i5 = i4 / 60;
        this.fields[12] = i5 % 60;
        int i6 = i5 / 60;
        this.fields[11] = i6;
        this.fields[9] = i6 / 12;
        this.fields[10] = i6 % 12;
        this.fields[15] = iArr[0];
        this.fields[16] = iArr[1];
    }

    private final void computeGregorianAndDOWFields(int i) {
        computeGregorianFields(i);
        int[] iArr = this.fields;
        int iJulianDayToDayOfWeek = julianDayToDayOfWeek(i);
        iArr[7] = iJulianDayToDayOfWeek;
        int firstDayOfWeek = (iJulianDayToDayOfWeek - getFirstDayOfWeek()) + 1;
        if (firstDayOfWeek < 1) {
            firstDayOfWeek += 7;
        }
        this.fields[18] = firstDayOfWeek;
    }

    protected final void computeGregorianFields(int i) {
        int[] iArr = new int[1];
        int iFloorDivide = floorDivide(i - JAN_1_1_JULIAN_DAY, 146097, iArr);
        int i2 = 0;
        int iFloorDivide2 = floorDivide(iArr[0], 36524, iArr);
        int iFloorDivide3 = floorDivide(iArr[0], 1461, iArr);
        int iFloorDivide4 = floorDivide(iArr[0], 365, iArr);
        int i3 = (400 * iFloorDivide) + (100 * iFloorDivide2) + (iFloorDivide3 * 4) + iFloorDivide4;
        int i4 = iArr[0];
        if (iFloorDivide2 != 4 && iFloorDivide4 != 4) {
            i3++;
        } else {
            i4 = 365;
        }
        boolean z = (i3 & 3) == 0 && (i3 % 100 != 0 || i3 % 400 == 0);
        if (i4 >= (z ? 60 : 59)) {
            i2 = z ? 1 : 2;
        }
        int i5 = ((12 * (i2 + i4)) + 6) / 367;
        int i6 = (i4 - GREGORIAN_MONTH_COUNT[i5][z ? (char) 3 : (char) 2]) + 1;
        this.gregorianYear = i3;
        this.gregorianMonth = i5;
        this.gregorianDayOfMonth = i6;
        this.gregorianDayOfYear = i4 + 1;
    }

    private final void computeWeekFields() {
        int i = this.fields[19];
        int i2 = this.fields[7];
        int i3 = this.fields[6];
        int firstDayOfWeek = ((i2 + 7) - getFirstDayOfWeek()) % 7;
        int firstDayOfWeek2 = (((i2 - i3) + 7001) - getFirstDayOfWeek()) % 7;
        int iWeekNumber = ((i3 - 1) + firstDayOfWeek2) / 7;
        if (7 - firstDayOfWeek2 >= getMinimalDaysInFirstWeek()) {
            iWeekNumber++;
        }
        if (iWeekNumber == 0) {
            iWeekNumber = weekNumber(i3 + handleGetYearLength(i - 1), i2);
            i--;
        } else {
            int iHandleGetYearLength = handleGetYearLength(i);
            if (i3 >= iHandleGetYearLength - 5) {
                int i4 = ((firstDayOfWeek + iHandleGetYearLength) - i3) % 7;
                if (i4 < 0) {
                    i4 += 7;
                }
                if (6 - i4 >= getMinimalDaysInFirstWeek() && (i3 + 7) - firstDayOfWeek > iHandleGetYearLength) {
                    i++;
                    iWeekNumber = 1;
                }
            }
        }
        this.fields[3] = iWeekNumber;
        this.fields[17] = i;
        int i5 = this.fields[5];
        this.fields[4] = weekNumber(i5, i2);
        this.fields[8] = ((i5 - 1) / 7) + 1;
    }

    protected int resolveFields(int[][][] iArr) {
        int i = -1;
        int i2 = 0;
        while (i2 < iArr.length && i < 0) {
            int i3 = 0;
            int i4 = i;
            for (int[] iArr2 : iArr[i2]) {
                int i5 = iArr2[0] >= 32 ? 1 : 0;
                int iMax = 0;
                while (true) {
                    if (i5 < iArr2.length) {
                        int i6 = this.stamp[iArr2[i5]];
                        if (i6 == 0) {
                            break;
                        }
                        iMax = Math.max(iMax, i6);
                        i5++;
                    } else if (iMax > i3) {
                        int i7 = iArr2[0];
                        if (i7 < 32 || (i7 = i7 & 31) != 5 || this.stamp[4] < this.stamp[i7]) {
                            i4 = i7;
                        }
                        if (i4 == i7) {
                            i3 = iMax;
                        }
                    }
                }
            }
            i2++;
            i = i4;
        }
        return i >= 32 ? i & 31 : i;
    }

    protected int newestStamp(int i, int i2, int i3) {
        while (i <= i2) {
            if (this.stamp[i] > i3) {
                i3 = this.stamp[i];
            }
            i++;
        }
        return i3;
    }

    protected final int getStamp(int i) {
        return this.stamp[i];
    }

    protected int newerField(int i, int i2) {
        if (this.stamp[i2] > this.stamp[i]) {
            return i2;
        }
        return i;
    }

    protected void validateFields() {
        for (int i = 0; i < this.fields.length; i++) {
            if (this.stamp[i] >= 2) {
                validateField(i);
            }
        }
    }

    protected void validateField(int i) {
        switch (i) {
            case 5:
                validateField(i, 1, handleGetMonthLength(handleGetExtendedYear(), internalGet(2)));
                return;
            case 6:
                validateField(i, 1, handleGetYearLength(handleGetExtendedYear()));
                return;
            case 7:
            default:
                validateField(i, getMinimum(i), getMaximum(i));
                return;
            case 8:
                if (internalGet(i) == 0) {
                    throw new IllegalArgumentException("DAY_OF_WEEK_IN_MONTH cannot be zero");
                }
                validateField(i, getMinimum(i), getMaximum(i));
                return;
        }
    }

    protected final void validateField(int i, int i2, int i3) {
        int i4 = this.fields[i];
        if (i4 < i2 || i4 > i3) {
            throw new IllegalArgumentException(fieldName(i) + '=' + i4 + ", valid range=" + i2 + ".." + i3);
        }
    }

    protected void computeTime() {
        long jComputeMillisInDay;
        if (!isLenient()) {
            validateFields();
        }
        long jJulianDayToMillis = julianDayToMillis(computeJulianDay());
        if (this.stamp[21] >= 2 && newestStamp(9, 14, 0) <= this.stamp[21]) {
            jComputeMillisInDay = internalGet(21);
        } else if (Math.max(Math.abs(internalGet(11)), Math.abs(internalGet(10))) > MAX_HOURS) {
            jComputeMillisInDay = computeMillisInDayLong();
        } else {
            jComputeMillisInDay = computeMillisInDay();
        }
        if (this.stamp[15] >= 2 || this.stamp[16] >= 2) {
            this.time = (jJulianDayToMillis + jComputeMillisInDay) - ((long) (internalGet(15) + internalGet(16)));
            return;
        }
        if (!this.lenient || this.skippedWallTime == 2) {
            int iComputeZoneOffset = computeZoneOffset(jJulianDayToMillis, jComputeMillisInDay);
            long j = (jJulianDayToMillis + jComputeMillisInDay) - ((long) iComputeZoneOffset);
            if (iComputeZoneOffset != this.zone.getOffset(j)) {
                if (!this.lenient) {
                    throw new IllegalArgumentException("The specified wall time does not exist due to time zone offset transition.");
                }
                Long immediatePreviousZoneTransition = getImmediatePreviousZoneTransition(j);
                if (immediatePreviousZoneTransition == null) {
                    throw new RuntimeException("Could not locate a time zone transition before " + j);
                }
                this.time = immediatePreviousZoneTransition.longValue();
                return;
            }
            this.time = j;
            return;
        }
        this.time = (jJulianDayToMillis + jComputeMillisInDay) - ((long) computeZoneOffset(jJulianDayToMillis, jComputeMillisInDay));
    }

    private Long getImmediatePreviousZoneTransition(long j) {
        if (this.zone instanceof BasicTimeZone) {
            TimeZoneTransition previousTransition = ((BasicTimeZone) this.zone).getPreviousTransition(j, true);
            if (previousTransition != null) {
                return Long.valueOf(previousTransition.getTime());
            }
            return null;
        }
        Long previousZoneTransitionTime = getPreviousZoneTransitionTime(this.zone, j, 7200000L);
        if (previousZoneTransitionTime == null) {
            return getPreviousZoneTransitionTime(this.zone, j, 108000000L);
        }
        return previousZoneTransitionTime;
    }

    private static Long getPreviousZoneTransitionTime(TimeZone timeZone, long j, long j2) {
        long j3 = (j - j2) - 1;
        int offset = timeZone.getOffset(j);
        if (offset == timeZone.getOffset(j3)) {
            return null;
        }
        return findPreviousZoneTransitionTime(timeZone, offset, j, j3);
    }

    private static Long findPreviousZoneTransitionTime(TimeZone timeZone, int i, long j, long j2) {
        long j3;
        long j4;
        long j5;
        int[] iArr = FIND_ZONE_TRANSITION_TIME_UNITS;
        int length = iArr.length;
        boolean z = false;
        int i2 = 0;
        while (true) {
            if (i2 < length) {
                long j6 = iArr[i2];
                long j7 = j2 / j6;
                long j8 = j / j6;
                if (j8 <= j7) {
                    i2++;
                } else {
                    j3 = (((j7 + j8) + 1) >>> 1) * j6;
                    z = true;
                    break;
                }
            } else {
                j3 = 0;
                break;
            }
        }
        if (!z) {
            j3 = (j + j2) >>> 1;
        }
        if (z) {
            if (j3 == j) {
                j5 = j;
            } else {
                if (timeZone.getOffset(j3) != i) {
                    return findPreviousZoneTransitionTime(timeZone, i, j, j3);
                }
                j5 = j3;
            }
            j4 = j3 - 1;
        } else {
            j4 = (j + j2) >>> 1;
            j5 = j;
        }
        if (j4 == j2) {
            return Long.valueOf(j5);
        }
        if (timeZone.getOffset(j4) != i) {
            if (z) {
                return Long.valueOf(j5);
            }
            return findPreviousZoneTransitionTime(timeZone, i, j5, j4);
        }
        return findPreviousZoneTransitionTime(timeZone, i, j4, j2);
    }

    @Deprecated
    protected int computeMillisInDay() {
        int i = this.stamp[11];
        int iMax = Math.max(this.stamp[10], this.stamp[9]);
        if (iMax <= i) {
            iMax = i;
        }
        int iInternalGet = 0;
        if (iMax != 0) {
            if (iMax == i) {
                iInternalGet = 0 + internalGet(11);
            } else {
                iInternalGet = 0 + internalGet(10) + (internalGet(9) * 12);
            }
        }
        return (((((iInternalGet * 60) + internalGet(12)) * 60) + internalGet(13)) * 1000) + internalGet(14);
    }

    @Deprecated
    protected long computeMillisInDayLong() {
        int i = this.stamp[11];
        int iMax = Math.max(this.stamp[10], this.stamp[9]);
        if (iMax <= i) {
            iMax = i;
        }
        long jInternalGet = 0;
        if (iMax != 0) {
            if (iMax == i) {
                jInternalGet = 0 + ((long) internalGet(11));
            } else {
                jInternalGet = 0 + ((long) internalGet(10)) + ((long) (internalGet(9) * 12));
            }
        }
        return (((((jInternalGet * 60) + ((long) internalGet(12))) * 60) + ((long) internalGet(13))) * 1000) + ((long) internalGet(14));
    }

    @Deprecated
    protected int computeZoneOffset(long j, int i) {
        boolean z;
        int[] iArr = new int[2];
        long j2 = j + ((long) i);
        if (this.zone instanceof BasicTimeZone) {
            ((BasicTimeZone) this.zone).getOffsetFromLocal(j2, this.skippedWallTime == 1 ? 12 : 4, this.repeatedWallTime == 1 ? 4 : 12, iArr);
        } else {
            this.zone.getOffset(j2, true, iArr);
            if (this.repeatedWallTime == 1) {
                int offset = (iArr[0] + iArr[1]) - this.zone.getOffset((j2 - ((long) (iArr[0] + iArr[1]))) - 21600000);
                if (offset < 0) {
                    this.zone.getOffset(((long) offset) + j2, true, iArr);
                    z = true;
                } else {
                    z = false;
                }
                if (!z && this.skippedWallTime == 1) {
                    this.zone.getOffset(j2 - ((long) (iArr[0] + iArr[1])), false, iArr);
                }
            }
        }
        return iArr[0] + iArr[1];
    }

    @Deprecated
    protected int computeZoneOffset(long j, long j2) {
        boolean z;
        int[] iArr = new int[2];
        long j3 = j + j2;
        if (this.zone instanceof BasicTimeZone) {
            ((BasicTimeZone) this.zone).getOffsetFromLocal(j3, this.skippedWallTime == 1 ? 12 : 4, this.repeatedWallTime == 1 ? 4 : 12, iArr);
        } else {
            this.zone.getOffset(j3, true, iArr);
            if (this.repeatedWallTime == 1) {
                int offset = (iArr[0] + iArr[1]) - this.zone.getOffset((j3 - ((long) (iArr[0] + iArr[1]))) - 21600000);
                if (offset < 0) {
                    this.zone.getOffset(((long) offset) + j3, true, iArr);
                    z = true;
                } else {
                    z = false;
                }
                if (!z && this.skippedWallTime == 1) {
                    this.zone.getOffset(j3 - ((long) (iArr[0] + iArr[1])), false, iArr);
                }
            }
        }
        return iArr[0] + iArr[1];
    }

    protected int computeJulianDay() {
        if (this.stamp[20] >= 2 && newestStamp(17, 19, newestStamp(0, 8, 0)) <= this.stamp[20]) {
            return internalGet(20);
        }
        int iResolveFields = resolveFields(getFieldResolutionTable());
        if (iResolveFields < 0) {
            iResolveFields = 5;
        }
        return handleComputeJulianDay(iResolveFields);
    }

    protected int[][][] getFieldResolutionTable() {
        return DATE_PRECEDENCE;
    }

    protected int handleGetMonthLength(int i, int i2) {
        return handleComputeMonthStart(i, i2 + 1, true) - handleComputeMonthStart(i, i2, true);
    }

    protected int handleGetYearLength(int i) {
        return handleComputeMonthStart(i + 1, 0, false) - handleComputeMonthStart(i, 0, false);
    }

    protected int[] handleCreateFields() {
        return new int[23];
    }

    protected int getDefaultMonthInYear(int i) {
        return 0;
    }

    protected int getDefaultDayInMonth(int i, int i2) {
        return 1;
    }

    protected int handleComputeJulianDay(int i) {
        int iHandleGetExtendedYear;
        int iInternalGet;
        int iInternalGet2;
        int iInternalGet3;
        boolean z = i == 5 || i == 4 || i == 8;
        if (i == 3) {
            iHandleGetExtendedYear = internalGet(17, handleGetExtendedYear());
        } else {
            iHandleGetExtendedYear = handleGetExtendedYear();
        }
        internalSet(19, iHandleGetExtendedYear);
        if (z) {
            iInternalGet = internalGet(2, getDefaultMonthInYear(iHandleGetExtendedYear));
        } else {
            iInternalGet = 0;
        }
        int iHandleComputeMonthStart = handleComputeMonthStart(iHandleGetExtendedYear, iInternalGet, z);
        if (i == 5) {
            if (isSet(5)) {
                return iHandleComputeMonthStart + internalGet(5, getDefaultDayInMonth(iHandleGetExtendedYear, iInternalGet));
            }
            return iHandleComputeMonthStart + getDefaultDayInMonth(iHandleGetExtendedYear, iInternalGet);
        }
        if (i == 6) {
            return iHandleComputeMonthStart + internalGet(6);
        }
        int firstDayOfWeek = getFirstDayOfWeek();
        int iJulianDayToDayOfWeek = julianDayToDayOfWeek(iHandleComputeMonthStart + 1) - firstDayOfWeek;
        if (iJulianDayToDayOfWeek < 0) {
            iJulianDayToDayOfWeek += 7;
        }
        int iResolveFields = resolveFields(DOW_PRECEDENCE);
        if (iResolveFields == 7) {
            iInternalGet2 = internalGet(7) - firstDayOfWeek;
        } else if (iResolveFields == 18) {
            iInternalGet2 = internalGet(18) - 1;
        } else {
            iInternalGet2 = 0;
        }
        int i2 = iInternalGet2 % 7;
        if (i2 < 0) {
            i2 += 7;
        }
        int i3 = (1 - iJulianDayToDayOfWeek) + i2;
        if (i == 8) {
            if (i3 < 1) {
                i3 += 7;
            }
            int iInternalGet4 = internalGet(8, 1);
            if (iInternalGet4 >= 0) {
                iInternalGet3 = i3 + (7 * (iInternalGet4 - 1));
            } else {
                iInternalGet3 = i3 + ((((handleGetMonthLength(iHandleGetExtendedYear, internalGet(2, 0)) - i3) / 7) + iInternalGet4 + 1) * 7);
            }
        } else {
            if (7 - iJulianDayToDayOfWeek < getMinimalDaysInFirstWeek()) {
                i3 += 7;
            }
            iInternalGet3 = i3 + (7 * (internalGet(i) - 1));
        }
        return iHandleComputeMonthStart + iInternalGet3;
    }

    protected int computeGregorianMonthStart(int i, int i2) {
        boolean z = false;
        if (i2 < 0 || i2 > 11) {
            int[] iArr = new int[1];
            i += floorDivide(i2, 12, iArr);
            i2 = iArr[0];
        }
        if (i % 4 == 0 && (i % 100 != 0 || i % 400 == 0)) {
            z = true;
        }
        int i3 = i - 1;
        int iFloorDivide = (((((365 * i3) + floorDivide(i3, 4)) - floorDivide(i3, 100)) + floorDivide(i3, 400)) + JAN_1_1_JULIAN_DAY) - 1;
        if (i2 != 0) {
            return iFloorDivide + GREGORIAN_MONTH_COUNT[i2][z ? (char) 3 : (char) 2];
        }
        return iFloorDivide;
    }

    protected void handleComputeFields(int i) {
        int i2;
        int i3;
        internalSet(2, getGregorianMonth());
        internalSet(5, getGregorianDayOfMonth());
        internalSet(6, getGregorianDayOfYear());
        int gregorianYear = getGregorianYear();
        internalSet(19, gregorianYear);
        if (gregorianYear < 1) {
            i2 = 1 - gregorianYear;
            i3 = 0;
        } else {
            i2 = gregorianYear;
            i3 = 1;
        }
        internalSet(0, i3);
        internalSet(1, i2);
    }

    protected final int getGregorianYear() {
        return this.gregorianYear;
    }

    protected final int getGregorianMonth() {
        return this.gregorianMonth;
    }

    protected final int getGregorianDayOfYear() {
        return this.gregorianDayOfYear;
    }

    protected final int getGregorianDayOfMonth() {
        return this.gregorianDayOfMonth;
    }

    public final int getFieldCount() {
        return this.fields.length;
    }

    protected final void internalSet(int i, int i2) {
        if (((1 << i) & this.internalSetMask) == 0) {
            throw new IllegalStateException("Subclass cannot set " + fieldName(i));
        }
        this.fields[i] = i2;
        this.stamp[i] = 1;
    }

    protected static final boolean isGregorianLeapYear(int i) {
        return i % 4 == 0 && (i % 100 != 0 || i % 400 == 0);
    }

    protected static final int gregorianMonthLength(int i, int i2) {
        return GREGORIAN_MONTH_COUNT[i2][isGregorianLeapYear(i) ? 1 : 0];
    }

    protected static final int gregorianPreviousMonthLength(int i, int i2) {
        if (i2 > 0) {
            return gregorianMonthLength(i, i2 - 1);
        }
        return 31;
    }

    protected static final long floorDivide(long j, long j2) {
        if (j >= 0) {
            return j / j2;
        }
        return ((j + 1) / j2) - 1;
    }

    protected static final int floorDivide(int i, int i2) {
        if (i >= 0) {
            return i / i2;
        }
        return ((i + 1) / i2) - 1;
    }

    protected static final int floorDivide(int i, int i2, int[] iArr) {
        if (i >= 0) {
            iArr[0] = i % i2;
            return i / i2;
        }
        int i3 = ((i + 1) / i2) - 1;
        iArr[0] = i - (i2 * i3);
        return i3;
    }

    protected static final int floorDivide(long j, int i, int[] iArr) {
        if (j >= 0) {
            long j2 = i;
            iArr[0] = (int) (j % j2);
            return (int) (j / j2);
        }
        long j3 = i;
        int i2 = (int) (((j + 1) / j3) - 1);
        iArr[0] = (int) (j - (((long) i2) * j3));
        return i2;
    }

    protected String fieldName(int i) {
        try {
            return FIELD_NAME[i];
        } catch (ArrayIndexOutOfBoundsException e) {
            return "Field " + i;
        }
    }

    protected static final int millisToJulianDay(long j) {
        return (int) (2440588 + floorDivide(j, 86400000L));
    }

    protected static final long julianDayToMillis(int i) {
        return ((long) (i - EPOCH_JULIAN_DAY)) * 86400000;
    }

    protected static final int julianDayToDayOfWeek(int i) {
        int i2 = (i + 2) % 7;
        if (i2 < 1) {
            return i2 + 7;
        }
        return i2;
    }

    protected final long internalGetTimeInMillis() {
        return this.time;
    }

    public String getType() {
        return "unknown";
    }

    @Deprecated
    public boolean haveDefaultCentury() {
        return true;
    }

    public final ULocale getLocale(ULocale.Type type) {
        return type == ULocale.ACTUAL_LOCALE ? this.actualLocale : this.validLocale;
    }

    final void setLocale(ULocale uLocale, ULocale uLocale2) {
        if ((uLocale == null) != (uLocale2 == null)) {
            throw new IllegalArgumentException();
        }
        this.validLocale = uLocale;
        this.actualLocale = uLocale2;
    }
}
