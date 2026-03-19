package android.icu.text;

import android.icu.impl.CacheBase;
import android.icu.impl.CalendarUtil;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SoftCache;
import android.icu.impl.UResource;
import android.icu.impl.Utility;
import android.icu.text.TimeZoneNames;
import android.icu.util.Calendar;
import android.icu.util.ICUCloneNotSupportedException;
import android.icu.util.ICUException;
import android.icu.util.TimeZone;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import android.icu.util.UResourceBundleIterator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;

public class DateFormatSymbols implements Serializable, Cloneable {
    public static final int ABBREVIATED = 0;
    static final String ALTERNATE_TIME_SEPARATOR = ".";
    private static final String[] DAY_PERIOD_KEYS;
    static final String DEFAULT_TIME_SEPARATOR = ":";
    private static CacheBase<String, DateFormatSymbols, ULocale> DFSCACHE = null;

    @Deprecated
    public static final int DT_CONTEXT_COUNT = 3;
    static final int DT_LEAP_MONTH_PATTERN_FORMAT_ABBREV = 1;
    static final int DT_LEAP_MONTH_PATTERN_FORMAT_NARROW = 2;
    static final int DT_LEAP_MONTH_PATTERN_FORMAT_WIDE = 0;
    static final int DT_LEAP_MONTH_PATTERN_NUMERIC = 6;
    static final int DT_LEAP_MONTH_PATTERN_STANDALONE_ABBREV = 4;
    static final int DT_LEAP_MONTH_PATTERN_STANDALONE_NARROW = 5;
    static final int DT_LEAP_MONTH_PATTERN_STANDALONE_WIDE = 3;
    static final int DT_MONTH_PATTERN_COUNT = 7;

    @Deprecated
    public static final int DT_WIDTH_COUNT = 4;
    public static final int FORMAT = 0;
    private static final String[] LEAP_MONTH_PATTERNS_PATHS;
    public static final int NARROW = 2;

    @Deprecated
    public static final int NUMERIC = 2;
    public static final int SHORT = 3;
    public static final int STANDALONE = 1;
    public static final int WIDE = 1;
    static final int millisPerHour = 3600000;
    static final String patternChars = "GyMdkHmsSEDFwWahKzYeugAZvcLQqVUOXxrbB";
    private static final long serialVersionUID = -5987973545549424702L;
    String[] abbreviatedDayPeriods;
    private ULocale actualLocale;
    String[] ampms;
    String[] ampmsNarrow;
    Map<CapitalizationContextUsage, boolean[]> capitalization;
    String[] eraNames;
    String[] eras;
    String[] leapMonthPatterns;
    String localPatternChars;
    String[] months;
    String[] narrowDayPeriods;
    String[] narrowEras;
    String[] narrowMonths;
    String[] narrowWeekdays;
    String[] quarters;
    private ULocale requestedLocale;
    String[] shortMonths;
    String[] shortQuarters;
    String[] shortWeekdays;
    String[] shortYearNames;
    String[] shortZodiacNames;
    String[] shorterWeekdays;
    String[] standaloneAbbreviatedDayPeriods;
    String[] standaloneMonths;
    String[] standaloneNarrowDayPeriods;
    String[] standaloneNarrowMonths;
    String[] standaloneNarrowWeekdays;
    String[] standaloneQuarters;
    String[] standaloneShortMonths;
    String[] standaloneShortQuarters;
    String[] standaloneShortWeekdays;
    String[] standaloneShorterWeekdays;
    String[] standaloneWeekdays;
    String[] standaloneWideDayPeriods;
    private String timeSeparator;
    private ULocale validLocale;
    String[] weekdays;
    String[] wideDayPeriods;
    private String[][] zoneStrings;
    private static final String[][] CALENDAR_CLASSES = {new String[]{"GregorianCalendar", "gregorian"}, new String[]{"JapaneseCalendar", "japanese"}, new String[]{"BuddhistCalendar", "buddhist"}, new String[]{"TaiwanCalendar", "roc"}, new String[]{"PersianCalendar", "persian"}, new String[]{"IslamicCalendar", "islamic"}, new String[]{"HebrewCalendar", "hebrew"}, new String[]{"ChineseCalendar", "chinese"}, new String[]{"IndianCalendar", "indian"}, new String[]{"CopticCalendar", "coptic"}, new String[]{"EthiopicCalendar", "ethiopic"}};
    private static final Map<String, CapitalizationContextUsage> contextUsageTypeMap = new HashMap();

    enum CapitalizationContextUsage {
        OTHER,
        MONTH_FORMAT,
        MONTH_STANDALONE,
        MONTH_NARROW,
        DAY_FORMAT,
        DAY_STANDALONE,
        DAY_NARROW,
        ERA_WIDE,
        ERA_ABBREV,
        ERA_NARROW,
        ZONE_LONG,
        ZONE_SHORT,
        METAZONE_LONG,
        METAZONE_SHORT
    }

    public DateFormatSymbols() {
        this(ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public DateFormatSymbols(Locale locale) {
        this(ULocale.forLocale(locale));
    }

    public DateFormatSymbols(ULocale uLocale) {
        this.eras = null;
        this.eraNames = null;
        this.narrowEras = null;
        this.months = null;
        this.shortMonths = null;
        this.narrowMonths = null;
        this.standaloneMonths = null;
        this.standaloneShortMonths = null;
        this.standaloneNarrowMonths = null;
        this.weekdays = null;
        this.shortWeekdays = null;
        this.shorterWeekdays = null;
        this.narrowWeekdays = null;
        this.standaloneWeekdays = null;
        this.standaloneShortWeekdays = null;
        this.standaloneShorterWeekdays = null;
        this.standaloneNarrowWeekdays = null;
        this.ampms = null;
        this.ampmsNarrow = null;
        this.timeSeparator = null;
        this.shortQuarters = null;
        this.quarters = null;
        this.standaloneShortQuarters = null;
        this.standaloneQuarters = null;
        this.leapMonthPatterns = null;
        this.shortYearNames = null;
        this.shortZodiacNames = null;
        this.zoneStrings = null;
        this.localPatternChars = null;
        this.abbreviatedDayPeriods = null;
        this.wideDayPeriods = null;
        this.narrowDayPeriods = null;
        this.standaloneAbbreviatedDayPeriods = null;
        this.standaloneWideDayPeriods = null;
        this.standaloneNarrowDayPeriods = null;
        this.capitalization = null;
        initializeData(uLocale, CalendarUtil.getCalendarType(uLocale));
    }

    public static DateFormatSymbols getInstance() {
        return new DateFormatSymbols();
    }

    public static DateFormatSymbols getInstance(Locale locale) {
        return new DateFormatSymbols(locale);
    }

    public static DateFormatSymbols getInstance(ULocale uLocale) {
        return new DateFormatSymbols(uLocale);
    }

    public static Locale[] getAvailableLocales() {
        return ICUResourceBundle.getAvailableLocales();
    }

    public static ULocale[] getAvailableULocales() {
        return ICUResourceBundle.getAvailableULocales();
    }

    static {
        contextUsageTypeMap.put("month-format-except-narrow", CapitalizationContextUsage.MONTH_FORMAT);
        contextUsageTypeMap.put("month-standalone-except-narrow", CapitalizationContextUsage.MONTH_STANDALONE);
        contextUsageTypeMap.put("month-narrow", CapitalizationContextUsage.MONTH_NARROW);
        contextUsageTypeMap.put("day-format-except-narrow", CapitalizationContextUsage.DAY_FORMAT);
        contextUsageTypeMap.put("day-standalone-except-narrow", CapitalizationContextUsage.DAY_STANDALONE);
        contextUsageTypeMap.put("day-narrow", CapitalizationContextUsage.DAY_NARROW);
        contextUsageTypeMap.put("era-name", CapitalizationContextUsage.ERA_WIDE);
        contextUsageTypeMap.put("era-abbr", CapitalizationContextUsage.ERA_ABBREV);
        contextUsageTypeMap.put("era-narrow", CapitalizationContextUsage.ERA_NARROW);
        contextUsageTypeMap.put("zone-long", CapitalizationContextUsage.ZONE_LONG);
        contextUsageTypeMap.put("zone-short", CapitalizationContextUsage.ZONE_SHORT);
        contextUsageTypeMap.put("metazone-long", CapitalizationContextUsage.METAZONE_LONG);
        contextUsageTypeMap.put("metazone-short", CapitalizationContextUsage.METAZONE_SHORT);
        DFSCACHE = new SoftCache<String, DateFormatSymbols, ULocale>() {
            @Override
            protected DateFormatSymbols createInstance(String str, ULocale uLocale) {
                int iIndexOf = str.indexOf(43) + 1;
                int iIndexOf2 = str.indexOf(43, iIndexOf);
                if (iIndexOf2 < 0) {
                    iIndexOf2 = str.length();
                }
                return new DateFormatSymbols(uLocale, null, str.substring(iIndexOf, iIndexOf2));
            }
        };
        LEAP_MONTH_PATTERNS_PATHS = new String[7];
        LEAP_MONTH_PATTERNS_PATHS[0] = "monthPatterns/format/wide";
        LEAP_MONTH_PATTERNS_PATHS[1] = "monthPatterns/format/abbreviated";
        LEAP_MONTH_PATTERNS_PATHS[2] = "monthPatterns/format/narrow";
        LEAP_MONTH_PATTERNS_PATHS[3] = "monthPatterns/stand-alone/wide";
        LEAP_MONTH_PATTERNS_PATHS[4] = "monthPatterns/stand-alone/abbreviated";
        LEAP_MONTH_PATTERNS_PATHS[5] = "monthPatterns/stand-alone/narrow";
        LEAP_MONTH_PATTERNS_PATHS[6] = "monthPatterns/numeric/all";
        DAY_PERIOD_KEYS = new String[]{"midnight", "noon", "morning1", "afternoon1", "evening1", "night1", "morning2", "afternoon2", "evening2", "night2"};
    }

    public String[] getEras() {
        return duplicate(this.eras);
    }

    public void setEras(String[] strArr) {
        this.eras = duplicate(strArr);
    }

    public String[] getEraNames() {
        return duplicate(this.eraNames);
    }

    public void setEraNames(String[] strArr) {
        this.eraNames = duplicate(strArr);
    }

    @Deprecated
    public String[] getNarrowEras() {
        return duplicate(this.narrowEras);
    }

    public String[] getMonths() {
        return duplicate(this.months);
    }

    public String[] getMonths(int i, int i2) {
        String[] strArr;
        String[] strArr2 = null;
        switch (i) {
            case 0:
                switch (i2) {
                    case 0:
                    case 3:
                        strArr = this.shortMonths;
                        break;
                    case 1:
                        strArr = this.months;
                        break;
                    case 2:
                        strArr = this.narrowMonths;
                        break;
                }
                strArr2 = strArr;
                break;
            case 1:
                switch (i2) {
                    case 0:
                    case 3:
                        strArr2 = this.standaloneShortMonths;
                        break;
                    case 1:
                        strArr2 = this.standaloneMonths;
                        break;
                    case 2:
                        strArr2 = this.standaloneNarrowMonths;
                        break;
                }
                break;
        }
        if (strArr2 == null) {
            throw new IllegalArgumentException("Bad context or width argument");
        }
        return duplicate(strArr2);
    }

    public void setMonths(String[] strArr) {
        this.months = duplicate(strArr);
    }

    public void setMonths(String[] strArr, int i, int i2) {
        switch (i) {
            case 0:
                switch (i2) {
                    case 0:
                        this.shortMonths = duplicate(strArr);
                        break;
                    case 1:
                        this.months = duplicate(strArr);
                        break;
                    case 2:
                        this.narrowMonths = duplicate(strArr);
                        break;
                }
                break;
            case 1:
                switch (i2) {
                    case 0:
                        this.standaloneShortMonths = duplicate(strArr);
                        break;
                    case 1:
                        this.standaloneMonths = duplicate(strArr);
                        break;
                    case 2:
                        this.standaloneNarrowMonths = duplicate(strArr);
                        break;
                }
                break;
        }
    }

    public String[] getShortMonths() {
        return duplicate(this.shortMonths);
    }

    public void setShortMonths(String[] strArr) {
        this.shortMonths = duplicate(strArr);
    }

    public String[] getWeekdays() {
        return duplicate(this.weekdays);
    }

    public String[] getWeekdays(int i, int i2) {
        String[] strArr;
        String[] strArr2 = null;
        switch (i) {
            case 0:
                switch (i2) {
                    case 0:
                        strArr = this.shortWeekdays;
                        break;
                    case 1:
                        strArr = this.weekdays;
                        break;
                    case 2:
                        strArr = this.narrowWeekdays;
                        break;
                    case 3:
                        strArr = this.shorterWeekdays == null ? this.shortWeekdays : this.shorterWeekdays;
                        break;
                }
                strArr2 = strArr;
                break;
            case 1:
                switch (i2) {
                    case 0:
                        strArr2 = this.standaloneShortWeekdays;
                        break;
                    case 1:
                        strArr2 = this.standaloneWeekdays;
                        break;
                    case 2:
                        strArr2 = this.standaloneNarrowWeekdays;
                        break;
                    case 3:
                        strArr2 = this.standaloneShorterWeekdays != null ? this.standaloneShorterWeekdays : this.standaloneShortWeekdays;
                        break;
                }
                break;
        }
        if (strArr2 == null) {
            throw new IllegalArgumentException("Bad context or width argument");
        }
        return duplicate(strArr2);
    }

    public void setWeekdays(String[] strArr, int i, int i2) {
        switch (i) {
            case 0:
                switch (i2) {
                    case 0:
                        this.shortWeekdays = duplicate(strArr);
                        break;
                    case 1:
                        this.weekdays = duplicate(strArr);
                        break;
                    case 2:
                        this.narrowWeekdays = duplicate(strArr);
                        break;
                    case 3:
                        this.shorterWeekdays = duplicate(strArr);
                        break;
                }
                break;
            case 1:
                switch (i2) {
                    case 0:
                        this.standaloneShortWeekdays = duplicate(strArr);
                        break;
                    case 1:
                        this.standaloneWeekdays = duplicate(strArr);
                        break;
                    case 2:
                        this.standaloneNarrowWeekdays = duplicate(strArr);
                        break;
                    case 3:
                        this.standaloneShorterWeekdays = duplicate(strArr);
                        break;
                }
                break;
        }
    }

    public void setWeekdays(String[] strArr) {
        this.weekdays = duplicate(strArr);
    }

    public String[] getShortWeekdays() {
        return duplicate(this.shortWeekdays);
    }

    public void setShortWeekdays(String[] strArr) {
        this.shortWeekdays = duplicate(strArr);
    }

    public String[] getQuarters(int i, int i2) {
        String[] strArr;
        String[] strArr2 = null;
        switch (i) {
            case 0:
                switch (i2) {
                    case 0:
                    case 3:
                        strArr = this.shortQuarters;
                        break;
                    case 1:
                        strArr = this.quarters;
                        break;
                }
                strArr2 = strArr;
                break;
            case 1:
                switch (i2) {
                    case 0:
                    case 3:
                        strArr2 = this.standaloneShortQuarters;
                        break;
                    case 1:
                        strArr2 = this.standaloneQuarters;
                        break;
                }
                break;
        }
        if (strArr2 == null) {
            throw new IllegalArgumentException("Bad context or width argument");
        }
        return duplicate(strArr2);
    }

    public void setQuarters(String[] strArr, int i, int i2) {
        switch (i) {
            case 0:
                switch (i2) {
                    case 0:
                        this.shortQuarters = duplicate(strArr);
                        break;
                    case 1:
                        this.quarters = duplicate(strArr);
                        break;
                }
                break;
            case 1:
                switch (i2) {
                    case 0:
                        this.standaloneShortQuarters = duplicate(strArr);
                        break;
                    case 1:
                        this.standaloneQuarters = duplicate(strArr);
                        break;
                }
                break;
        }
    }

    public String[] getYearNames(int i, int i2) {
        if (this.shortYearNames != null) {
            return duplicate(this.shortYearNames);
        }
        return null;
    }

    public void setYearNames(String[] strArr, int i, int i2) {
        if (i == 0 && i2 == 0) {
            this.shortYearNames = duplicate(strArr);
        }
    }

    public String[] getZodiacNames(int i, int i2) {
        if (this.shortZodiacNames != null) {
            return duplicate(this.shortZodiacNames);
        }
        return null;
    }

    public void setZodiacNames(String[] strArr, int i, int i2) {
        if (i == 0 && i2 == 0) {
            this.shortZodiacNames = duplicate(strArr);
        }
    }

    @Deprecated
    public String getLeapMonthPattern(int i, int i2) {
        byte b;
        byte b2;
        if (this.leapMonthPatterns != null) {
            byte b3 = -1;
            switch (i) {
                case 0:
                    switch (i2) {
                        case 0:
                        case 3:
                            b3 = 1;
                            break;
                        case 1:
                            b = 0;
                            b3 = b;
                            break;
                        case 2:
                            b = 2;
                            b3 = b;
                            break;
                    }
                    break;
                case 1:
                    switch (i2) {
                        case 0:
                        case 3:
                            b3 = 1;
                            break;
                        case 1:
                            b2 = 3;
                            b3 = b2;
                            break;
                        case 2:
                            b2 = 5;
                            b3 = b2;
                            break;
                    }
                    break;
                case 2:
                    b3 = 6;
                    break;
            }
            if (b3 < 0) {
                throw new IllegalArgumentException("Bad context or width argument");
            }
            return this.leapMonthPatterns[b3];
        }
        return null;
    }

    @Deprecated
    public void setLeapMonthPattern(String str, int i, int i2) {
        if (this.leapMonthPatterns != null) {
            byte b = -1;
            switch (i) {
                case 0:
                    switch (i2) {
                        case 0:
                            b = 1;
                            break;
                        case 1:
                            b = 0;
                            break;
                        case 2:
                            b = 2;
                            break;
                    }
                    break;
                case 1:
                    switch (i2) {
                        case 1:
                            b = 3;
                            break;
                        case 2:
                            b = 5;
                            break;
                    }
                    break;
                case 2:
                    b = 6;
                    break;
            }
            if (b >= 0) {
                this.leapMonthPatterns[b] = str;
            }
        }
    }

    public String[] getAmPmStrings() {
        return duplicate(this.ampms);
    }

    public void setAmPmStrings(String[] strArr) {
        this.ampms = duplicate(strArr);
    }

    @Deprecated
    public String getTimeSeparatorString() {
        return this.timeSeparator;
    }

    @Deprecated
    public void setTimeSeparatorString(String str) {
        this.timeSeparator = str;
    }

    public String[][] getZoneStrings() {
        if (this.zoneStrings != null) {
            return duplicate(this.zoneStrings);
        }
        String[] availableIDs = TimeZone.getAvailableIDs();
        TimeZoneNames timeZoneNames = TimeZoneNames.getInstance(this.validLocale);
        timeZoneNames.loadAllDisplayNames();
        TimeZoneNames.NameType[] nameTypeArr = {TimeZoneNames.NameType.LONG_STANDARD, TimeZoneNames.NameType.SHORT_STANDARD, TimeZoneNames.NameType.LONG_DAYLIGHT, TimeZoneNames.NameType.SHORT_DAYLIGHT};
        long jCurrentTimeMillis = System.currentTimeMillis();
        String[][] strArr = (String[][]) Array.newInstance((Class<?>) String.class, availableIDs.length, 5);
        for (int i = 0; i < availableIDs.length; i++) {
            String canonicalID = TimeZone.getCanonicalID(availableIDs[i]);
            if (canonicalID == null) {
                canonicalID = availableIDs[i];
            }
            strArr[i][0] = availableIDs[i];
            timeZoneNames.getDisplayNames(canonicalID, nameTypeArr, jCurrentTimeMillis, strArr[i], 1);
        }
        this.zoneStrings = strArr;
        return this.zoneStrings;
    }

    public void setZoneStrings(String[][] strArr) {
        this.zoneStrings = duplicate(strArr);
    }

    public String getLocalPatternChars() {
        return this.localPatternChars;
    }

    public void setLocalPatternChars(String str) {
        this.localPatternChars = str;
    }

    public Object clone() {
        try {
            return (DateFormatSymbols) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }

    public int hashCode() {
        return this.requestedLocale.toString().hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DateFormatSymbols dateFormatSymbols = (DateFormatSymbols) obj;
        if (Utility.arrayEquals((Object[]) this.eras, (Object) dateFormatSymbols.eras) && Utility.arrayEquals((Object[]) this.eraNames, (Object) dateFormatSymbols.eraNames) && Utility.arrayEquals((Object[]) this.months, (Object) dateFormatSymbols.months) && Utility.arrayEquals((Object[]) this.shortMonths, (Object) dateFormatSymbols.shortMonths) && Utility.arrayEquals((Object[]) this.narrowMonths, (Object) dateFormatSymbols.narrowMonths) && Utility.arrayEquals((Object[]) this.standaloneMonths, (Object) dateFormatSymbols.standaloneMonths) && Utility.arrayEquals((Object[]) this.standaloneShortMonths, (Object) dateFormatSymbols.standaloneShortMonths) && Utility.arrayEquals((Object[]) this.standaloneNarrowMonths, (Object) dateFormatSymbols.standaloneNarrowMonths) && Utility.arrayEquals((Object[]) this.weekdays, (Object) dateFormatSymbols.weekdays) && Utility.arrayEquals((Object[]) this.shortWeekdays, (Object) dateFormatSymbols.shortWeekdays) && Utility.arrayEquals((Object[]) this.shorterWeekdays, (Object) dateFormatSymbols.shorterWeekdays) && Utility.arrayEquals((Object[]) this.narrowWeekdays, (Object) dateFormatSymbols.narrowWeekdays) && Utility.arrayEquals((Object[]) this.standaloneWeekdays, (Object) dateFormatSymbols.standaloneWeekdays) && Utility.arrayEquals((Object[]) this.standaloneShortWeekdays, (Object) dateFormatSymbols.standaloneShortWeekdays) && Utility.arrayEquals((Object[]) this.standaloneShorterWeekdays, (Object) dateFormatSymbols.standaloneShorterWeekdays) && Utility.arrayEquals((Object[]) this.standaloneNarrowWeekdays, (Object) dateFormatSymbols.standaloneNarrowWeekdays) && Utility.arrayEquals((Object[]) this.ampms, (Object) dateFormatSymbols.ampms) && Utility.arrayEquals((Object[]) this.ampmsNarrow, (Object) dateFormatSymbols.ampmsNarrow) && Utility.arrayEquals((Object[]) this.abbreviatedDayPeriods, (Object) dateFormatSymbols.abbreviatedDayPeriods) && Utility.arrayEquals((Object[]) this.wideDayPeriods, (Object) dateFormatSymbols.wideDayPeriods) && Utility.arrayEquals((Object[]) this.narrowDayPeriods, (Object) dateFormatSymbols.narrowDayPeriods) && Utility.arrayEquals((Object[]) this.standaloneAbbreviatedDayPeriods, (Object) dateFormatSymbols.standaloneAbbreviatedDayPeriods) && Utility.arrayEquals((Object[]) this.standaloneWideDayPeriods, (Object) dateFormatSymbols.standaloneWideDayPeriods) && Utility.arrayEquals((Object[]) this.standaloneNarrowDayPeriods, (Object) dateFormatSymbols.standaloneNarrowDayPeriods) && Utility.arrayEquals(this.timeSeparator, dateFormatSymbols.timeSeparator) && arrayOfArrayEquals(this.zoneStrings, dateFormatSymbols.zoneStrings) && this.requestedLocale.getDisplayName().equals(dateFormatSymbols.requestedLocale.getDisplayName()) && Utility.arrayEquals(this.localPatternChars, dateFormatSymbols.localPatternChars)) {
            return true;
        }
        return false;
    }

    protected void initializeData(ULocale uLocale, String str) {
        String str2 = uLocale.getBaseName() + '+' + str;
        String keywordValue = uLocale.getKeywordValue("numbers");
        if (keywordValue != null && keywordValue.length() > 0) {
            str2 = str2 + '+' + keywordValue;
        }
        initializeData(DFSCACHE.getInstance(str2, uLocale));
    }

    void initializeData(DateFormatSymbols dateFormatSymbols) {
        this.eras = dateFormatSymbols.eras;
        this.eraNames = dateFormatSymbols.eraNames;
        this.narrowEras = dateFormatSymbols.narrowEras;
        this.months = dateFormatSymbols.months;
        this.shortMonths = dateFormatSymbols.shortMonths;
        this.narrowMonths = dateFormatSymbols.narrowMonths;
        this.standaloneMonths = dateFormatSymbols.standaloneMonths;
        this.standaloneShortMonths = dateFormatSymbols.standaloneShortMonths;
        this.standaloneNarrowMonths = dateFormatSymbols.standaloneNarrowMonths;
        this.weekdays = dateFormatSymbols.weekdays;
        this.shortWeekdays = dateFormatSymbols.shortWeekdays;
        this.shorterWeekdays = dateFormatSymbols.shorterWeekdays;
        this.narrowWeekdays = dateFormatSymbols.narrowWeekdays;
        this.standaloneWeekdays = dateFormatSymbols.standaloneWeekdays;
        this.standaloneShortWeekdays = dateFormatSymbols.standaloneShortWeekdays;
        this.standaloneShorterWeekdays = dateFormatSymbols.standaloneShorterWeekdays;
        this.standaloneNarrowWeekdays = dateFormatSymbols.standaloneNarrowWeekdays;
        this.ampms = dateFormatSymbols.ampms;
        this.ampmsNarrow = dateFormatSymbols.ampmsNarrow;
        this.timeSeparator = dateFormatSymbols.timeSeparator;
        this.shortQuarters = dateFormatSymbols.shortQuarters;
        this.quarters = dateFormatSymbols.quarters;
        this.standaloneShortQuarters = dateFormatSymbols.standaloneShortQuarters;
        this.standaloneQuarters = dateFormatSymbols.standaloneQuarters;
        this.leapMonthPatterns = dateFormatSymbols.leapMonthPatterns;
        this.shortYearNames = dateFormatSymbols.shortYearNames;
        this.shortZodiacNames = dateFormatSymbols.shortZodiacNames;
        this.abbreviatedDayPeriods = dateFormatSymbols.abbreviatedDayPeriods;
        this.wideDayPeriods = dateFormatSymbols.wideDayPeriods;
        this.narrowDayPeriods = dateFormatSymbols.narrowDayPeriods;
        this.standaloneAbbreviatedDayPeriods = dateFormatSymbols.standaloneAbbreviatedDayPeriods;
        this.standaloneWideDayPeriods = dateFormatSymbols.standaloneWideDayPeriods;
        this.standaloneNarrowDayPeriods = dateFormatSymbols.standaloneNarrowDayPeriods;
        this.zoneStrings = dateFormatSymbols.zoneStrings;
        this.localPatternChars = dateFormatSymbols.localPatternChars;
        this.capitalization = dateFormatSymbols.capitalization;
        this.actualLocale = dateFormatSymbols.actualLocale;
        this.validLocale = dateFormatSymbols.validLocale;
        this.requestedLocale = dateFormatSymbols.requestedLocale;
    }

    private static final class CalendarDataSink extends UResource.Sink {
        static final boolean $assertionsDisabled = false;
        private static final String CALENDAR_ALIAS_PREFIX = "/LOCALE/calendar/";
        private String aliasRelativePath;
        private Set<String> resourcesToVisit;
        Map<String, String[]> arrays = new TreeMap();
        Map<String, Map<String, String>> maps = new TreeMap();
        List<String> aliasPathPairs = new ArrayList();
        String currentCalendarType = null;
        String nextCalendarType = null;

        private enum AliasType {
            SAME_CALENDAR,
            DIFFERENT_CALENDAR,
            GREGORIAN,
            NONE
        }

        CalendarDataSink() {
        }

        void visitAllResources() {
            this.resourcesToVisit = null;
        }

        void preEnumerate(String str) {
            this.currentCalendarType = str;
            this.nextCalendarType = null;
            this.aliasPathPairs.clear();
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean z) {
            boolean z2;
            UResource.Table table = value.getTable();
            HashSet hashSet = null;
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                String string = key.toString();
                AliasType aliasTypeProcessAliasFromValue = processAliasFromValue(string, value);
                if (aliasTypeProcessAliasFromValue != AliasType.GREGORIAN) {
                    if (aliasTypeProcessAliasFromValue == AliasType.DIFFERENT_CALENDAR) {
                        if (hashSet == null) {
                            hashSet = new HashSet();
                        }
                        hashSet.add(this.aliasRelativePath);
                    } else if (aliasTypeProcessAliasFromValue == AliasType.SAME_CALENDAR) {
                        if (!this.arrays.containsKey(string) && !this.maps.containsKey(string)) {
                            this.aliasPathPairs.add(this.aliasRelativePath);
                            this.aliasPathPairs.add(string);
                        }
                    } else if (this.resourcesToVisit == null || this.resourcesToVisit.isEmpty() || this.resourcesToVisit.contains(string) || string.equals("AmPmMarkersAbbr")) {
                        if (string.startsWith("AmPmMarkers")) {
                            if (!string.endsWith("%variant") && !this.arrays.containsKey(string)) {
                                this.arrays.put(string, value.getStringArray());
                            }
                        } else if (string.equals("eras") || string.equals("dayNames") || string.equals("monthNames") || string.equals("quarters") || string.equals("dayPeriod") || string.equals("monthPatterns") || string.equals("cyclicNameSets")) {
                            processResource(string, key, value);
                        }
                    }
                }
            }
            do {
                int i2 = 0;
                boolean z3 = false;
                while (i2 < this.aliasPathPairs.size()) {
                    String str = this.aliasPathPairs.get(i2);
                    if (this.arrays.containsKey(str)) {
                        this.arrays.put(this.aliasPathPairs.get(i2 + 1), this.arrays.get(str));
                    } else if (this.maps.containsKey(str)) {
                        this.maps.put(this.aliasPathPairs.get(i2 + 1), this.maps.get(str));
                    } else {
                        z2 = false;
                        if (!z2) {
                            this.aliasPathPairs.remove(i2 + 1);
                            this.aliasPathPairs.remove(i2);
                            z3 = true;
                        } else {
                            i2 += 2;
                        }
                    }
                    z2 = true;
                    if (!z2) {
                    }
                }
                if (!z3) {
                    break;
                }
            } while (!this.aliasPathPairs.isEmpty());
            if (hashSet != null) {
                this.resourcesToVisit = hashSet;
            }
        }

        protected void processResource(String str, UResource.Key key, UResource.Value value) {
            UResource.Table table = value.getTable();
            HashMap map = null;
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                if (!key.endsWith("%variant")) {
                    String string = key.toString();
                    if (value.getType() == 0) {
                        if (i == 0) {
                            map = new HashMap();
                            this.maps.put(str, map);
                        }
                        map.put(string, value.getString());
                    } else {
                        String str2 = str + "/" + string;
                        if ((!str2.startsWith("cyclicNameSets") || "cyclicNameSets/years/format/abbreviated".startsWith(str2) || "cyclicNameSets/zodiacs/format/abbreviated".startsWith(str2) || "cyclicNameSets/dayParts/format/abbreviated".startsWith(str2)) && !this.arrays.containsKey(str2) && !this.maps.containsKey(str2)) {
                            if (processAliasFromValue(str2, value) == AliasType.SAME_CALENDAR) {
                                this.aliasPathPairs.add(this.aliasRelativePath);
                                this.aliasPathPairs.add(str2);
                            } else if (value.getType() == 8) {
                                this.arrays.put(str2, value.getStringArray());
                            } else if (value.getType() == 2) {
                                processResource(str2, key, value);
                            }
                        }
                    }
                }
            }
        }

        private AliasType processAliasFromValue(String str, UResource.Value value) {
            int iIndexOf;
            if (value.getType() == 3) {
                String aliasString = value.getAliasString();
                if (aliasString.startsWith(CALENDAR_ALIAS_PREFIX) && aliasString.length() > CALENDAR_ALIAS_PREFIX.length() && (iIndexOf = aliasString.indexOf(47, CALENDAR_ALIAS_PREFIX.length())) > CALENDAR_ALIAS_PREFIX.length()) {
                    String strSubstring = aliasString.substring(CALENDAR_ALIAS_PREFIX.length(), iIndexOf);
                    this.aliasRelativePath = aliasString.substring(iIndexOf + 1);
                    if (this.currentCalendarType.equals(strSubstring) && !str.equals(this.aliasRelativePath)) {
                        return AliasType.SAME_CALENDAR;
                    }
                    if (!this.currentCalendarType.equals(strSubstring) && str.equals(this.aliasRelativePath)) {
                        if (strSubstring.equals("gregorian")) {
                            return AliasType.GREGORIAN;
                        }
                        if (this.nextCalendarType == null || this.nextCalendarType.equals(strSubstring)) {
                            this.nextCalendarType = strSubstring;
                            return AliasType.DIFFERENT_CALENDAR;
                        }
                    }
                }
                throw new ICUException("Malformed 'calendar' alias. Path: " + aliasString);
            }
            return AliasType.NONE;
        }
    }

    private DateFormatSymbols(ULocale uLocale, ICUResourceBundle iCUResourceBundle, String str) {
        this.eras = null;
        this.eraNames = null;
        this.narrowEras = null;
        this.months = null;
        this.shortMonths = null;
        this.narrowMonths = null;
        this.standaloneMonths = null;
        this.standaloneShortMonths = null;
        this.standaloneNarrowMonths = null;
        this.weekdays = null;
        this.shortWeekdays = null;
        this.shorterWeekdays = null;
        this.narrowWeekdays = null;
        this.standaloneWeekdays = null;
        this.standaloneShortWeekdays = null;
        this.standaloneShorterWeekdays = null;
        this.standaloneNarrowWeekdays = null;
        this.ampms = null;
        this.ampmsNarrow = null;
        this.timeSeparator = null;
        this.shortQuarters = null;
        this.quarters = null;
        this.standaloneShortQuarters = null;
        this.standaloneQuarters = null;
        this.leapMonthPatterns = null;
        this.shortYearNames = null;
        this.shortZodiacNames = null;
        this.zoneStrings = null;
        this.localPatternChars = null;
        this.abbreviatedDayPeriods = null;
        this.wideDayPeriods = null;
        this.narrowDayPeriods = null;
        this.standaloneAbbreviatedDayPeriods = null;
        this.standaloneWideDayPeriods = null;
        this.standaloneNarrowDayPeriods = null;
        this.capitalization = null;
        initializeData(uLocale, iCUResourceBundle, str);
    }

    @Deprecated
    protected void initializeData(ULocale uLocale, ICUResourceBundle iCUResourceBundle, String str) {
        ICUResourceBundle withFallback;
        Map<String, String> map;
        String str2;
        CalendarDataSink calendarDataSink = new CalendarDataSink();
        if (iCUResourceBundle == null) {
            iCUResourceBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, uLocale);
        }
        while (str != null) {
            ICUResourceBundle iCUResourceBundleFindWithFallback = iCUResourceBundle.findWithFallback("calendar/" + str);
            if (iCUResourceBundleFindWithFallback == null) {
                if (!"gregorian".equals(str)) {
                    str = "gregorian";
                    calendarDataSink.visitAllResources();
                } else {
                    throw new MissingResourceException("The 'gregorian' calendar type wasn't found for the locale: " + uLocale.getBaseName(), getClass().getName(), "gregorian");
                }
            } else {
                calendarDataSink.preEnumerate(str);
                iCUResourceBundleFindWithFallback.getAllItemsWithFallback("", calendarDataSink);
                if (str.equals("gregorian")) {
                    break;
                }
                str = calendarDataSink.nextCalendarType;
                if (str == null) {
                    str = "gregorian";
                    calendarDataSink.visitAllResources();
                }
            }
        }
        Map<String, String[]> map2 = calendarDataSink.arrays;
        Map<String, Map<String, String>> map3 = calendarDataSink.maps;
        this.eras = map2.get("eras/abbreviated");
        this.eraNames = map2.get("eras/wide");
        this.narrowEras = map2.get("eras/narrow");
        this.months = map2.get("monthNames/format/wide");
        this.shortMonths = map2.get("monthNames/format/abbreviated");
        this.narrowMonths = map2.get("monthNames/format/narrow");
        this.standaloneMonths = map2.get("monthNames/stand-alone/wide");
        this.standaloneShortMonths = map2.get("monthNames/stand-alone/abbreviated");
        this.standaloneNarrowMonths = map2.get("monthNames/stand-alone/narrow");
        String[] strArr = map2.get("dayNames/format/wide");
        this.weekdays = new String[8];
        this.weekdays[0] = "";
        System.arraycopy(strArr, 0, this.weekdays, 1, strArr.length);
        String[] strArr2 = map2.get("dayNames/format/abbreviated");
        this.shortWeekdays = new String[8];
        this.shortWeekdays[0] = "";
        System.arraycopy(strArr2, 0, this.shortWeekdays, 1, strArr2.length);
        String[] strArr3 = map2.get("dayNames/format/short");
        this.shorterWeekdays = new String[8];
        this.shorterWeekdays[0] = "";
        System.arraycopy(strArr3, 0, this.shorterWeekdays, 1, strArr3.length);
        String[] strArr4 = map2.get("dayNames/format/narrow");
        if (strArr4 == null && (strArr4 = map2.get("dayNames/stand-alone/narrow")) == null && (strArr4 = map2.get("dayNames/format/abbreviated")) == null) {
            throw new MissingResourceException("Resource not found", getClass().getName(), "dayNames/format/abbreviated");
        }
        this.narrowWeekdays = new String[8];
        this.narrowWeekdays[0] = "";
        System.arraycopy(strArr4, 0, this.narrowWeekdays, 1, strArr4.length);
        String[] strArr5 = map2.get("dayNames/stand-alone/wide");
        this.standaloneWeekdays = new String[8];
        this.standaloneWeekdays[0] = "";
        System.arraycopy(strArr5, 0, this.standaloneWeekdays, 1, strArr5.length);
        String[] strArr6 = map2.get("dayNames/stand-alone/abbreviated");
        this.standaloneShortWeekdays = new String[8];
        this.standaloneShortWeekdays[0] = "";
        System.arraycopy(strArr6, 0, this.standaloneShortWeekdays, 1, strArr6.length);
        String[] strArr7 = map2.get("dayNames/stand-alone/short");
        this.standaloneShorterWeekdays = new String[8];
        this.standaloneShorterWeekdays[0] = "";
        System.arraycopy(strArr7, 0, this.standaloneShorterWeekdays, 1, strArr7.length);
        String[] strArr8 = map2.get("dayNames/stand-alone/narrow");
        this.standaloneNarrowWeekdays = new String[8];
        this.standaloneNarrowWeekdays[0] = "";
        System.arraycopy(strArr8, 0, this.standaloneNarrowWeekdays, 1, strArr8.length);
        this.ampms = map2.get("AmPmMarkers");
        this.ampmsNarrow = map2.get("AmPmMarkersNarrow");
        this.quarters = map2.get("quarters/format/wide");
        this.shortQuarters = map2.get("quarters/format/abbreviated");
        this.standaloneQuarters = map2.get("quarters/stand-alone/wide");
        this.standaloneShortQuarters = map2.get("quarters/stand-alone/abbreviated");
        this.abbreviatedDayPeriods = loadDayPeriodStrings(map3.get("dayPeriod/format/abbreviated"));
        this.wideDayPeriods = loadDayPeriodStrings(map3.get("dayPeriod/format/wide"));
        this.narrowDayPeriods = loadDayPeriodStrings(map3.get("dayPeriod/format/narrow"));
        this.standaloneAbbreviatedDayPeriods = loadDayPeriodStrings(map3.get("dayPeriod/stand-alone/abbreviated"));
        this.standaloneWideDayPeriods = loadDayPeriodStrings(map3.get("dayPeriod/stand-alone/wide"));
        this.standaloneNarrowDayPeriods = loadDayPeriodStrings(map3.get("dayPeriod/stand-alone/narrow"));
        for (int i = 0; i < 7; i++) {
            String str3 = LEAP_MONTH_PATTERNS_PATHS[i];
            if (str3 != null && (map = map3.get(str3)) != null && (str2 = map.get("leap")) != null) {
                if (this.leapMonthPatterns == null) {
                    this.leapMonthPatterns = new String[7];
                }
                this.leapMonthPatterns[i] = str2;
            }
        }
        this.shortYearNames = map2.get("cyclicNameSets/years/format/abbreviated");
        this.shortZodiacNames = map2.get("cyclicNameSets/zodiacs/format/abbreviated");
        this.requestedLocale = uLocale;
        ICUResourceBundle iCUResourceBundle2 = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, uLocale);
        this.localPatternChars = patternChars;
        ULocale uLocale2 = iCUResourceBundle2.getULocale();
        setLocale(uLocale2, uLocale2);
        this.capitalization = new HashMap();
        boolean[] zArr = {false, false};
        for (CapitalizationContextUsage capitalizationContextUsage : CapitalizationContextUsage.values()) {
            this.capitalization.put(capitalizationContextUsage, zArr);
        }
        try {
            withFallback = iCUResourceBundle2.getWithFallback("contextTransforms");
        } catch (MissingResourceException e) {
            withFallback = null;
        }
        if (withFallback != null) {
            UResourceBundleIterator iterator = withFallback.getIterator();
            while (iterator.hasNext()) {
                UResourceBundle next = iterator.next();
                int[] intVector = next.getIntVector();
                if (intVector.length >= 2) {
                    CapitalizationContextUsage capitalizationContextUsage2 = contextUsageTypeMap.get(next.getKey());
                    if (capitalizationContextUsage2 != null) {
                        boolean[] zArr2 = new boolean[2];
                        zArr2[0] = intVector[0] != 0;
                        zArr2[1] = intVector[1] != 0;
                        this.capitalization.put(capitalizationContextUsage2, zArr2);
                    }
                }
            }
        }
        NumberingSystem numberingSystem = NumberingSystem.getInstance(uLocale);
        try {
            setTimeSeparatorString(iCUResourceBundle2.getStringWithFallback("NumberElements/" + (numberingSystem == null ? "latn" : numberingSystem.getName()) + "/symbols/timeSeparator"));
        } catch (MissingResourceException e2) {
            setTimeSeparatorString(DEFAULT_TIME_SEPARATOR);
        }
    }

    private static final boolean arrayOfArrayEquals(Object[][] objArr, Object[][] objArr2) {
        boolean zArrayEquals = true;
        if (objArr == objArr2) {
            return true;
        }
        if (objArr == null || objArr2 == null || objArr.length != objArr2.length) {
            return false;
        }
        for (int i = 0; i < objArr.length && (zArrayEquals = Utility.arrayEquals(objArr[i], (Object) objArr2[i])); i++) {
        }
        return zArrayEquals;
    }

    private String[] loadDayPeriodStrings(Map<String, String> map) {
        String[] strArr = new String[DAY_PERIOD_KEYS.length];
        if (map != null) {
            for (int i = 0; i < DAY_PERIOD_KEYS.length; i++) {
                strArr[i] = map.get(DAY_PERIOD_KEYS[i]);
            }
        }
        return strArr;
    }

    private final String[] duplicate(String[] strArr) {
        return (String[]) strArr.clone();
    }

    private final String[][] duplicate(String[][] strArr) {
        String[][] strArr2 = new String[strArr.length][];
        for (int i = 0; i < strArr.length; i++) {
            strArr2[i] = duplicate(strArr[i]);
        }
        return strArr2;
    }

    public DateFormatSymbols(Calendar calendar, Locale locale) {
        this.eras = null;
        this.eraNames = null;
        this.narrowEras = null;
        this.months = null;
        this.shortMonths = null;
        this.narrowMonths = null;
        this.standaloneMonths = null;
        this.standaloneShortMonths = null;
        this.standaloneNarrowMonths = null;
        this.weekdays = null;
        this.shortWeekdays = null;
        this.shorterWeekdays = null;
        this.narrowWeekdays = null;
        this.standaloneWeekdays = null;
        this.standaloneShortWeekdays = null;
        this.standaloneShorterWeekdays = null;
        this.standaloneNarrowWeekdays = null;
        this.ampms = null;
        this.ampmsNarrow = null;
        this.timeSeparator = null;
        this.shortQuarters = null;
        this.quarters = null;
        this.standaloneShortQuarters = null;
        this.standaloneQuarters = null;
        this.leapMonthPatterns = null;
        this.shortYearNames = null;
        this.shortZodiacNames = null;
        this.zoneStrings = null;
        this.localPatternChars = null;
        this.abbreviatedDayPeriods = null;
        this.wideDayPeriods = null;
        this.narrowDayPeriods = null;
        this.standaloneAbbreviatedDayPeriods = null;
        this.standaloneWideDayPeriods = null;
        this.standaloneNarrowDayPeriods = null;
        this.capitalization = null;
        initializeData(ULocale.forLocale(locale), calendar.getType());
    }

    public DateFormatSymbols(Calendar calendar, ULocale uLocale) {
        this.eras = null;
        this.eraNames = null;
        this.narrowEras = null;
        this.months = null;
        this.shortMonths = null;
        this.narrowMonths = null;
        this.standaloneMonths = null;
        this.standaloneShortMonths = null;
        this.standaloneNarrowMonths = null;
        this.weekdays = null;
        this.shortWeekdays = null;
        this.shorterWeekdays = null;
        this.narrowWeekdays = null;
        this.standaloneWeekdays = null;
        this.standaloneShortWeekdays = null;
        this.standaloneShorterWeekdays = null;
        this.standaloneNarrowWeekdays = null;
        this.ampms = null;
        this.ampmsNarrow = null;
        this.timeSeparator = null;
        this.shortQuarters = null;
        this.quarters = null;
        this.standaloneShortQuarters = null;
        this.standaloneQuarters = null;
        this.leapMonthPatterns = null;
        this.shortYearNames = null;
        this.shortZodiacNames = null;
        this.zoneStrings = null;
        this.localPatternChars = null;
        this.abbreviatedDayPeriods = null;
        this.wideDayPeriods = null;
        this.narrowDayPeriods = null;
        this.standaloneAbbreviatedDayPeriods = null;
        this.standaloneWideDayPeriods = null;
        this.standaloneNarrowDayPeriods = null;
        this.capitalization = null;
        initializeData(uLocale, calendar.getType());
    }

    public DateFormatSymbols(Class<? extends Calendar> cls, Locale locale) {
        this(cls, ULocale.forLocale(locale));
    }

    public DateFormatSymbols(Class<? extends Calendar> cls, ULocale uLocale) {
        String str = null;
        this.eras = null;
        this.eraNames = null;
        this.narrowEras = null;
        this.months = null;
        this.shortMonths = null;
        this.narrowMonths = null;
        this.standaloneMonths = null;
        this.standaloneShortMonths = null;
        this.standaloneNarrowMonths = null;
        this.weekdays = null;
        this.shortWeekdays = null;
        this.shorterWeekdays = null;
        this.narrowWeekdays = null;
        this.standaloneWeekdays = null;
        this.standaloneShortWeekdays = null;
        this.standaloneShorterWeekdays = null;
        this.standaloneNarrowWeekdays = null;
        this.ampms = null;
        this.ampmsNarrow = null;
        this.timeSeparator = null;
        this.shortQuarters = null;
        this.quarters = null;
        this.standaloneShortQuarters = null;
        this.standaloneQuarters = null;
        this.leapMonthPatterns = null;
        this.shortYearNames = null;
        this.shortZodiacNames = null;
        this.zoneStrings = null;
        this.localPatternChars = null;
        this.abbreviatedDayPeriods = null;
        this.wideDayPeriods = null;
        this.narrowDayPeriods = null;
        this.standaloneAbbreviatedDayPeriods = null;
        this.standaloneWideDayPeriods = null;
        this.standaloneNarrowDayPeriods = null;
        this.capitalization = null;
        String name = cls.getName();
        String strSubstring = name.substring(name.lastIndexOf(46) + 1);
        String[][] strArr = CALENDAR_CLASSES;
        int length = strArr.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            String[] strArr2 = strArr[i];
            if (!strArr2[0].equals(strSubstring)) {
                i++;
            } else {
                str = strArr2[1];
                break;
            }
        }
        initializeData(uLocale, str == null ? strSubstring.replaceAll("Calendar", "").toLowerCase(Locale.ENGLISH) : str);
    }

    @Deprecated
    public DateFormatSymbols(ULocale uLocale, String str) {
        this.eras = null;
        this.eraNames = null;
        this.narrowEras = null;
        this.months = null;
        this.shortMonths = null;
        this.narrowMonths = null;
        this.standaloneMonths = null;
        this.standaloneShortMonths = null;
        this.standaloneNarrowMonths = null;
        this.weekdays = null;
        this.shortWeekdays = null;
        this.shorterWeekdays = null;
        this.narrowWeekdays = null;
        this.standaloneWeekdays = null;
        this.standaloneShortWeekdays = null;
        this.standaloneShorterWeekdays = null;
        this.standaloneNarrowWeekdays = null;
        this.ampms = null;
        this.ampmsNarrow = null;
        this.timeSeparator = null;
        this.shortQuarters = null;
        this.quarters = null;
        this.standaloneShortQuarters = null;
        this.standaloneQuarters = null;
        this.leapMonthPatterns = null;
        this.shortYearNames = null;
        this.shortZodiacNames = null;
        this.zoneStrings = null;
        this.localPatternChars = null;
        this.abbreviatedDayPeriods = null;
        this.wideDayPeriods = null;
        this.narrowDayPeriods = null;
        this.standaloneAbbreviatedDayPeriods = null;
        this.standaloneWideDayPeriods = null;
        this.standaloneNarrowDayPeriods = null;
        this.capitalization = null;
        initializeData(uLocale, str);
    }

    public DateFormatSymbols(ResourceBundle resourceBundle, Locale locale) {
        this(resourceBundle, ULocale.forLocale(locale));
    }

    public DateFormatSymbols(ResourceBundle resourceBundle, ULocale uLocale) {
        this.eras = null;
        this.eraNames = null;
        this.narrowEras = null;
        this.months = null;
        this.shortMonths = null;
        this.narrowMonths = null;
        this.standaloneMonths = null;
        this.standaloneShortMonths = null;
        this.standaloneNarrowMonths = null;
        this.weekdays = null;
        this.shortWeekdays = null;
        this.shorterWeekdays = null;
        this.narrowWeekdays = null;
        this.standaloneWeekdays = null;
        this.standaloneShortWeekdays = null;
        this.standaloneShorterWeekdays = null;
        this.standaloneNarrowWeekdays = null;
        this.ampms = null;
        this.ampmsNarrow = null;
        this.timeSeparator = null;
        this.shortQuarters = null;
        this.quarters = null;
        this.standaloneShortQuarters = null;
        this.standaloneQuarters = null;
        this.leapMonthPatterns = null;
        this.shortYearNames = null;
        this.shortZodiacNames = null;
        this.zoneStrings = null;
        this.localPatternChars = null;
        this.abbreviatedDayPeriods = null;
        this.wideDayPeriods = null;
        this.narrowDayPeriods = null;
        this.standaloneAbbreviatedDayPeriods = null;
        this.standaloneWideDayPeriods = null;
        this.standaloneNarrowDayPeriods = null;
        this.capitalization = null;
        initializeData(uLocale, (ICUResourceBundle) resourceBundle, CalendarUtil.getCalendarType(uLocale));
    }

    @Deprecated
    public static ResourceBundle getDateFormatBundle(Class<? extends Calendar> cls, Locale locale) throws MissingResourceException {
        return null;
    }

    @Deprecated
    public static ResourceBundle getDateFormatBundle(Class<? extends Calendar> cls, ULocale uLocale) throws MissingResourceException {
        return null;
    }

    @Deprecated
    public static ResourceBundle getDateFormatBundle(Calendar calendar, Locale locale) throws MissingResourceException {
        return null;
    }

    @Deprecated
    public static ResourceBundle getDateFormatBundle(Calendar calendar, ULocale uLocale) throws MissingResourceException {
        return null;
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

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
    }
}
