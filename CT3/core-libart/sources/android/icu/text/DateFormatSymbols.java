package android.icu.text;

import android.icu.impl.CalendarData;
import android.icu.impl.CalendarUtil;
import android.icu.impl.ICUCache;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SimpleCache;
import android.icu.impl.Utility;
import android.icu.text.TimeZoneNames;
import android.icu.util.Calendar;
import android.icu.util.ICUCloneNotSupportedException;
import android.icu.util.TimeZone;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import android.icu.util.UResourceBundleIterator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class DateFormatSymbols implements Serializable, Cloneable {
    public static final int ABBREVIATED = 0;
    static final String ALTERNATE_TIME_SEPARATOR = ".";
    static final String DEFAULT_TIME_SEPARATOR = ":";
    private static ICUCache<String, DateFormatSymbols> DFSCACHE = null;

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
    public static final int NARROW = 2;

    @Deprecated
    public static final int NUMERIC = 2;
    public static final int SHORT = 3;
    public static final int STANDALONE = 1;
    public static final int WIDE = 1;
    static final int millisPerHour = 3600000;
    static final String patternChars = "GyMdkHmsSEDFwWahKzYeugAZvcLQqVUOXxr";
    private static final long serialVersionUID = -5987973545549424702L;
    private ULocale actualLocale;
    String[] ampms;
    String[] ampmsNarrow;
    Map<CapitalizationContextUsage, boolean[]> capitalization;
    String[] eraNames;
    String[] eras;
    String[] leapMonthPatterns;
    String localPatternChars;
    String[] months;
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
    String[] standaloneMonths;
    String[] standaloneNarrowMonths;
    String[] standaloneNarrowWeekdays;
    String[] standaloneQuarters;
    String[] standaloneShortMonths;
    String[] standaloneShortQuarters;
    String[] standaloneShortWeekdays;
    String[] standaloneShorterWeekdays;
    String[] standaloneWeekdays;
    private String timeSeparator;
    private ULocale validLocale;
    String[] weekdays;
    private String[][] zoneStrings;
    private static final String[][] CALENDAR_CLASSES = {new String[]{"GregorianCalendar", "gregorian"}, new String[]{"JapaneseCalendar", "japanese"}, new String[]{"BuddhistCalendar", "buddhist"}, new String[]{"TaiwanCalendar", "roc"}, new String[]{"PersianCalendar", "persian"}, new String[]{"IslamicCalendar", "islamic"}, new String[]{"HebrewCalendar", "hebrew"}, new String[]{"ChineseCalendar", "chinese"}, new String[]{"IndianCalendar", "indian"}, new String[]{"CopticCalendar", "coptic"}, new String[]{"EthiopicCalendar", "ethiopic"}};
    private static final Map<String, CapitalizationContextUsage> contextUsageTypeMap = new HashMap();

    public DateFormatSymbols() {
        this(ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public DateFormatSymbols(Locale locale) {
        this(ULocale.forLocale(locale));
    }

    public DateFormatSymbols(ULocale locale) {
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
        this.capitalization = null;
        initializeData(locale, CalendarUtil.getCalendarType(locale));
    }

    public static DateFormatSymbols getInstance() {
        return new DateFormatSymbols();
    }

    public static DateFormatSymbols getInstance(Locale locale) {
        return new DateFormatSymbols(locale);
    }

    public static DateFormatSymbols getInstance(ULocale locale) {
        return new DateFormatSymbols(locale);
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
        DFSCACHE = new SimpleCache();
    }

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
        METAZONE_SHORT;

        public static CapitalizationContextUsage[] valuesCustom() {
            return values();
        }
    }

    public String[] getEras() {
        return duplicate(this.eras);
    }

    public void setEras(String[] newEras) {
        this.eras = duplicate(newEras);
    }

    public String[] getEraNames() {
        return duplicate(this.eraNames);
    }

    public void setEraNames(String[] newEraNames) {
        this.eraNames = duplicate(newEraNames);
    }

    public String[] getMonths() {
        return duplicate(this.months);
    }

    public String[] getMonths(int context, int width) {
        String[] returnValue = null;
        switch (context) {
            case 0:
                switch (width) {
                    case 0:
                    case 3:
                        returnValue = this.shortMonths;
                        break;
                    case 1:
                        returnValue = this.months;
                        break;
                    case 2:
                        returnValue = this.narrowMonths;
                        break;
                }
                break;
            case 1:
                switch (width) {
                    case 0:
                    case 3:
                        returnValue = this.standaloneShortMonths;
                        break;
                    case 1:
                        returnValue = this.standaloneMonths;
                        break;
                    case 2:
                        returnValue = this.standaloneNarrowMonths;
                        break;
                }
                break;
        }
        if (returnValue == null) {
            throw new IllegalArgumentException("Bad context or width argument");
        }
        return duplicate(returnValue);
    }

    public void setMonths(String[] newMonths) {
        this.months = duplicate(newMonths);
    }

    public void setMonths(String[] newMonths, int context, int width) {
        switch (context) {
            case 0:
                switch (width) {
                    case 0:
                        this.shortMonths = duplicate(newMonths);
                        break;
                    case 1:
                        this.months = duplicate(newMonths);
                        break;
                    case 2:
                        this.narrowMonths = duplicate(newMonths);
                        break;
                }
                break;
            case 1:
                switch (width) {
                    case 0:
                        this.standaloneShortMonths = duplicate(newMonths);
                        break;
                    case 1:
                        this.standaloneMonths = duplicate(newMonths);
                        break;
                    case 2:
                        this.standaloneNarrowMonths = duplicate(newMonths);
                        break;
                }
                break;
        }
    }

    public String[] getShortMonths() {
        return duplicate(this.shortMonths);
    }

    public void setShortMonths(String[] newShortMonths) {
        this.shortMonths = duplicate(newShortMonths);
    }

    public String[] getWeekdays() {
        return duplicate(this.weekdays);
    }

    public String[] getWeekdays(int context, int width) {
        String[] returnValue = null;
        switch (context) {
            case 0:
                switch (width) {
                    case 0:
                        returnValue = this.shortWeekdays;
                        break;
                    case 1:
                        returnValue = this.weekdays;
                        break;
                    case 2:
                        returnValue = this.narrowWeekdays;
                        break;
                    case 3:
                        returnValue = this.shorterWeekdays == null ? this.shortWeekdays : this.shorterWeekdays;
                        break;
                }
                break;
            case 1:
                switch (width) {
                    case 0:
                        returnValue = this.standaloneShortWeekdays;
                        break;
                    case 1:
                        returnValue = this.standaloneWeekdays;
                        break;
                    case 2:
                        returnValue = this.standaloneNarrowWeekdays;
                        break;
                    case 3:
                        returnValue = this.standaloneShorterWeekdays == null ? this.standaloneShortWeekdays : this.standaloneShorterWeekdays;
                        break;
                }
                break;
        }
        if (returnValue == null) {
            throw new IllegalArgumentException("Bad context or width argument");
        }
        return duplicate(returnValue);
    }

    public void setWeekdays(String[] newWeekdays, int context, int width) {
        switch (context) {
            case 0:
                switch (width) {
                    case 0:
                        this.shortWeekdays = duplicate(newWeekdays);
                        break;
                    case 1:
                        this.weekdays = duplicate(newWeekdays);
                        break;
                    case 2:
                        this.narrowWeekdays = duplicate(newWeekdays);
                        break;
                    case 3:
                        this.shorterWeekdays = duplicate(newWeekdays);
                        break;
                }
                break;
            case 1:
                switch (width) {
                    case 0:
                        this.standaloneShortWeekdays = duplicate(newWeekdays);
                        break;
                    case 1:
                        this.standaloneWeekdays = duplicate(newWeekdays);
                        break;
                    case 2:
                        this.standaloneNarrowWeekdays = duplicate(newWeekdays);
                        break;
                    case 3:
                        this.standaloneShorterWeekdays = duplicate(newWeekdays);
                        break;
                }
                break;
        }
    }

    public void setWeekdays(String[] newWeekdays) {
        this.weekdays = duplicate(newWeekdays);
    }

    public String[] getShortWeekdays() {
        return duplicate(this.shortWeekdays);
    }

    public void setShortWeekdays(String[] newAbbrevWeekdays) {
        this.shortWeekdays = duplicate(newAbbrevWeekdays);
    }

    public String[] getQuarters(int context, int width) {
        String[] returnValue = null;
        switch (context) {
            case 0:
                switch (width) {
                    case 0:
                    case 3:
                        returnValue = this.shortQuarters;
                        break;
                    case 1:
                        returnValue = this.quarters;
                        break;
                    case 2:
                        returnValue = null;
                        break;
                }
                break;
            case 1:
                switch (width) {
                    case 0:
                    case 3:
                        returnValue = this.standaloneShortQuarters;
                        break;
                    case 1:
                        returnValue = this.standaloneQuarters;
                        break;
                    case 2:
                        returnValue = null;
                        break;
                }
                break;
        }
        if (returnValue == null) {
            throw new IllegalArgumentException("Bad context or width argument");
        }
        return duplicate(returnValue);
    }

    public void setQuarters(String[] newQuarters, int context, int width) {
        switch (context) {
            case 0:
                switch (width) {
                    case 0:
                        this.shortQuarters = duplicate(newQuarters);
                        break;
                    case 1:
                        this.quarters = duplicate(newQuarters);
                        break;
                }
                break;
            case 1:
                switch (width) {
                    case 0:
                        this.standaloneShortQuarters = duplicate(newQuarters);
                        break;
                    case 1:
                        this.standaloneQuarters = duplicate(newQuarters);
                        break;
                }
                break;
        }
    }

    public String[] getYearNames(int context, int width) {
        if (this.shortYearNames != null) {
            return duplicate(this.shortYearNames);
        }
        return null;
    }

    public void setYearNames(String[] yearNames, int context, int width) {
        if (context != 0 || width != 0) {
            return;
        }
        this.shortYearNames = duplicate(yearNames);
    }

    public String[] getZodiacNames(int context, int width) {
        if (this.shortZodiacNames != null) {
            return duplicate(this.shortZodiacNames);
        }
        return null;
    }

    public void setZodiacNames(String[] zodiacNames, int context, int width) {
        if (context != 0 || width != 0) {
            return;
        }
        this.shortZodiacNames = duplicate(zodiacNames);
    }

    @Deprecated
    public String getLeapMonthPattern(int context, int width) {
        if (this.leapMonthPatterns == null) {
            return null;
        }
        int leapMonthPatternIndex = -1;
        switch (context) {
            case 0:
                switch (width) {
                    case 0:
                    case 3:
                        leapMonthPatternIndex = 1;
                        break;
                    case 1:
                        leapMonthPatternIndex = 0;
                        break;
                    case 2:
                        leapMonthPatternIndex = 2;
                        break;
                }
                break;
            case 1:
                switch (width) {
                    case 0:
                    case 3:
                        leapMonthPatternIndex = 1;
                        break;
                    case 1:
                        leapMonthPatternIndex = 3;
                        break;
                    case 2:
                        leapMonthPatternIndex = 5;
                        break;
                }
                break;
            case 2:
                leapMonthPatternIndex = 6;
                break;
        }
        if (leapMonthPatternIndex < 0) {
            throw new IllegalArgumentException("Bad context or width argument");
        }
        return this.leapMonthPatterns[leapMonthPatternIndex];
    }

    @Deprecated
    public void setLeapMonthPattern(String leapMonthPattern, int context, int width) {
        if (this.leapMonthPatterns == null) {
            return;
        }
        int leapMonthPatternIndex = -1;
        switch (context) {
            case 0:
                switch (width) {
                    case 0:
                        leapMonthPatternIndex = 1;
                        break;
                    case 1:
                        leapMonthPatternIndex = 0;
                        break;
                    case 2:
                        leapMonthPatternIndex = 2;
                        break;
                }
                break;
            case 1:
                switch (width) {
                    case 0:
                        leapMonthPatternIndex = 1;
                        break;
                    case 1:
                        leapMonthPatternIndex = 3;
                        break;
                    case 2:
                        leapMonthPatternIndex = 5;
                        break;
                }
                break;
            case 2:
                leapMonthPatternIndex = 6;
                break;
        }
        if (leapMonthPatternIndex < 0) {
            return;
        }
        this.leapMonthPatterns[leapMonthPatternIndex] = leapMonthPattern;
    }

    public String[] getAmPmStrings() {
        return duplicate(this.ampms);
    }

    public void setAmPmStrings(String[] newAmpms) {
        this.ampms = duplicate(newAmpms);
    }

    public String getTimeSeparatorString() {
        return this.timeSeparator;
    }

    public void setTimeSeparatorString(String newTimeSeparator) {
        this.timeSeparator = newTimeSeparator;
    }

    public String[][] getZoneStrings() {
        if (this.zoneStrings != null) {
            return duplicate(this.zoneStrings);
        }
        String[] tzIDs = TimeZone.getAvailableIDs();
        TimeZoneNames tznames = TimeZoneNames.getInstance(this.validLocale);
        tznames.loadAllDisplayNames();
        TimeZoneNames.NameType[] types = {TimeZoneNames.NameType.LONG_STANDARD, TimeZoneNames.NameType.SHORT_STANDARD, TimeZoneNames.NameType.LONG_DAYLIGHT, TimeZoneNames.NameType.SHORT_DAYLIGHT};
        long now = System.currentTimeMillis();
        String[][] array = (String[][]) Array.newInstance((Class<?>) String.class, tzIDs.length, 5);
        for (int i = 0; i < tzIDs.length; i++) {
            String canonicalID = TimeZone.getCanonicalID(tzIDs[i]);
            if (canonicalID == null) {
                canonicalID = tzIDs[i];
            }
            array[i][0] = tzIDs[i];
            tznames.getDisplayNames(canonicalID, types, now, array[i], 1);
        }
        this.zoneStrings = array;
        return this.zoneStrings;
    }

    public void setZoneStrings(String[][] newZoneStrings) {
        this.zoneStrings = duplicate(newZoneStrings);
    }

    public String getLocalPatternChars() {
        return this.localPatternChars;
    }

    public void setLocalPatternChars(String newLocalPatternChars) {
        this.localPatternChars = newLocalPatternChars;
    }

    public Object clone() {
        try {
            DateFormatSymbols other = (DateFormatSymbols) super.clone();
            return other;
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
        DateFormatSymbols that = (DateFormatSymbols) obj;
        if (Utility.arrayEquals((Object[]) this.eras, (Object) that.eras) && Utility.arrayEquals((Object[]) this.eraNames, (Object) that.eraNames) && Utility.arrayEquals((Object[]) this.months, (Object) that.months) && Utility.arrayEquals((Object[]) this.shortMonths, (Object) that.shortMonths) && Utility.arrayEquals((Object[]) this.narrowMonths, (Object) that.narrowMonths) && Utility.arrayEquals((Object[]) this.standaloneMonths, (Object) that.standaloneMonths) && Utility.arrayEquals((Object[]) this.standaloneShortMonths, (Object) that.standaloneShortMonths) && Utility.arrayEquals((Object[]) this.standaloneNarrowMonths, (Object) that.standaloneNarrowMonths) && Utility.arrayEquals((Object[]) this.weekdays, (Object) that.weekdays) && Utility.arrayEquals((Object[]) this.shortWeekdays, (Object) that.shortWeekdays) && Utility.arrayEquals((Object[]) this.shorterWeekdays, (Object) that.shorterWeekdays) && Utility.arrayEquals((Object[]) this.narrowWeekdays, (Object) that.narrowWeekdays) && Utility.arrayEquals((Object[]) this.standaloneWeekdays, (Object) that.standaloneWeekdays) && Utility.arrayEquals((Object[]) this.standaloneShortWeekdays, (Object) that.standaloneShortWeekdays) && Utility.arrayEquals((Object[]) this.standaloneShorterWeekdays, (Object) that.standaloneShorterWeekdays) && Utility.arrayEquals((Object[]) this.standaloneNarrowWeekdays, (Object) that.standaloneNarrowWeekdays) && Utility.arrayEquals((Object[]) this.ampms, (Object) that.ampms) && Utility.arrayEquals((Object[]) this.ampmsNarrow, (Object) that.ampmsNarrow) && Utility.arrayEquals(this.timeSeparator, that.timeSeparator) && arrayOfArrayEquals(this.zoneStrings, that.zoneStrings) && this.requestedLocale.getDisplayName().equals(that.requestedLocale.getDisplayName())) {
            return Utility.arrayEquals(this.localPatternChars, that.localPatternChars);
        }
        return false;
    }

    protected void initializeData(ULocale desiredLocale, String type) {
        String key = desiredLocale.getBaseName() + "+" + type;
        String ns = desiredLocale.getKeywordValue("numbers");
        if (ns != null && ns.length() > 0) {
            key = key + "+" + ns;
        }
        DateFormatSymbols dfs = DFSCACHE.get(key);
        if (dfs == null) {
            CalendarData calData = new CalendarData(desiredLocale, type);
            initializeData(desiredLocale, calData);
            if (!getClass().getName().equals("android.icu.text.DateFormatSymbols")) {
                return;
            }
            DFSCACHE.put(key, (DateFormatSymbols) clone());
            return;
        }
        initializeData(dfs);
    }

    void initializeData(DateFormatSymbols dfs) {
        this.eras = dfs.eras;
        this.eraNames = dfs.eraNames;
        this.narrowEras = dfs.narrowEras;
        this.months = dfs.months;
        this.shortMonths = dfs.shortMonths;
        this.narrowMonths = dfs.narrowMonths;
        this.standaloneMonths = dfs.standaloneMonths;
        this.standaloneShortMonths = dfs.standaloneShortMonths;
        this.standaloneNarrowMonths = dfs.standaloneNarrowMonths;
        this.weekdays = dfs.weekdays;
        this.shortWeekdays = dfs.shortWeekdays;
        this.shorterWeekdays = dfs.shorterWeekdays;
        this.narrowWeekdays = dfs.narrowWeekdays;
        this.standaloneWeekdays = dfs.standaloneWeekdays;
        this.standaloneShortWeekdays = dfs.standaloneShortWeekdays;
        this.standaloneShorterWeekdays = dfs.standaloneShorterWeekdays;
        this.standaloneNarrowWeekdays = dfs.standaloneNarrowWeekdays;
        this.ampms = dfs.ampms;
        this.ampmsNarrow = dfs.ampmsNarrow;
        this.timeSeparator = dfs.timeSeparator;
        this.shortQuarters = dfs.shortQuarters;
        this.quarters = dfs.quarters;
        this.standaloneShortQuarters = dfs.standaloneShortQuarters;
        this.standaloneQuarters = dfs.standaloneQuarters;
        this.leapMonthPatterns = dfs.leapMonthPatterns;
        this.shortYearNames = dfs.shortYearNames;
        this.shortZodiacNames = dfs.shortZodiacNames;
        this.zoneStrings = dfs.zoneStrings;
        this.localPatternChars = dfs.localPatternChars;
        this.capitalization = dfs.capitalization;
        this.actualLocale = dfs.actualLocale;
        this.validLocale = dfs.validLocale;
        this.requestedLocale = dfs.requestedLocale;
    }

    @Deprecated
    protected void initializeData(ULocale desiredLocale, CalendarData calData) {
        String[] nWeekdays;
        ICUResourceBundle monthPatternsBundle;
        ICUResourceBundle cyclicNameSetsBundle;
        UResourceBundle contextTransformsBundle;
        this.eras = calData.getEras("abbreviated");
        this.eraNames = calData.getEras("wide");
        this.narrowEras = calData.getEras("narrow");
        this.months = calData.getStringArray("monthNames", "wide");
        this.shortMonths = calData.getStringArray("monthNames", "abbreviated");
        this.narrowMonths = calData.getStringArray("monthNames", "narrow");
        this.standaloneMonths = calData.getStringArray("monthNames", "stand-alone", "wide");
        this.standaloneShortMonths = calData.getStringArray("monthNames", "stand-alone", "abbreviated");
        this.standaloneNarrowMonths = calData.getStringArray("monthNames", "stand-alone", "narrow");
        String[] lWeekdays = calData.getStringArray("dayNames", "wide");
        this.weekdays = new String[8];
        this.weekdays[0] = "";
        System.arraycopy(lWeekdays, 0, this.weekdays, 1, lWeekdays.length);
        String[] aWeekdays = calData.getStringArray("dayNames", "abbreviated");
        this.shortWeekdays = new String[8];
        this.shortWeekdays[0] = "";
        System.arraycopy(aWeekdays, 0, this.shortWeekdays, 1, aWeekdays.length);
        String[] sWeekdays = calData.getStringArray("dayNames", "short");
        this.shorterWeekdays = new String[8];
        this.shorterWeekdays[0] = "";
        System.arraycopy(sWeekdays, 0, this.shorterWeekdays, 1, sWeekdays.length);
        try {
            nWeekdays = calData.getStringArray("dayNames", "narrow");
        } catch (MissingResourceException e) {
            try {
                nWeekdays = calData.getStringArray("dayNames", "stand-alone", "narrow");
            } catch (MissingResourceException e2) {
                nWeekdays = calData.getStringArray("dayNames", "abbreviated");
            }
        }
        this.narrowWeekdays = new String[8];
        this.narrowWeekdays[0] = "";
        System.arraycopy(nWeekdays, 0, this.narrowWeekdays, 1, nWeekdays.length);
        String[] swWeekdays = calData.getStringArray("dayNames", "stand-alone", "wide");
        this.standaloneWeekdays = new String[8];
        this.standaloneWeekdays[0] = "";
        System.arraycopy(swWeekdays, 0, this.standaloneWeekdays, 1, swWeekdays.length);
        String[] saWeekdays = calData.getStringArray("dayNames", "stand-alone", "abbreviated");
        this.standaloneShortWeekdays = new String[8];
        this.standaloneShortWeekdays[0] = "";
        System.arraycopy(saWeekdays, 0, this.standaloneShortWeekdays, 1, saWeekdays.length);
        String[] ssWeekdays = calData.getStringArray("dayNames", "stand-alone", "short");
        this.standaloneShorterWeekdays = new String[8];
        this.standaloneShorterWeekdays[0] = "";
        System.arraycopy(ssWeekdays, 0, this.standaloneShorterWeekdays, 1, ssWeekdays.length);
        String[] snWeekdays = calData.getStringArray("dayNames", "stand-alone", "narrow");
        this.standaloneNarrowWeekdays = new String[8];
        this.standaloneNarrowWeekdays[0] = "";
        System.arraycopy(snWeekdays, 0, this.standaloneNarrowWeekdays, 1, snWeekdays.length);
        this.ampms = calData.getStringArray("AmPmMarkers");
        this.ampmsNarrow = calData.getStringArray("AmPmMarkersNarrow");
        this.quarters = calData.getStringArray("quarters", "wide");
        this.shortQuarters = calData.getStringArray("quarters", "abbreviated");
        this.standaloneQuarters = calData.getStringArray("quarters", "stand-alone", "wide");
        this.standaloneShortQuarters = calData.getStringArray("quarters", "stand-alone", "abbreviated");
        try {
            monthPatternsBundle = calData.get("monthPatterns");
        } catch (MissingResourceException e3) {
            monthPatternsBundle = null;
        }
        if (monthPatternsBundle != null) {
            this.leapMonthPatterns = new String[7];
            this.leapMonthPatterns[0] = calData.get("monthPatterns", "wide").get("leap").getString();
            this.leapMonthPatterns[1] = calData.get("monthPatterns", "abbreviated").get("leap").getString();
            this.leapMonthPatterns[2] = calData.get("monthPatterns", "narrow").get("leap").getString();
            this.leapMonthPatterns[3] = calData.get("monthPatterns", "stand-alone", "wide").get("leap").getString();
            this.leapMonthPatterns[4] = calData.get("monthPatterns", "stand-alone", "abbreviated").get("leap").getString();
            this.leapMonthPatterns[5] = calData.get("monthPatterns", "stand-alone", "narrow").get("leap").getString();
            this.leapMonthPatterns[6] = calData.get("monthPatterns", "numeric", "all").get("leap").getString();
        }
        try {
            cyclicNameSetsBundle = calData.get("cyclicNameSets");
        } catch (MissingResourceException e4) {
            cyclicNameSetsBundle = null;
        }
        if (cyclicNameSetsBundle != null) {
            this.shortYearNames = calData.get("cyclicNameSets", "years", "format", "abbreviated").getStringArray();
            this.shortZodiacNames = calData.get("cyclicNameSets", "zodiacs", "format", "abbreviated").getStringArray();
        }
        this.requestedLocale = desiredLocale;
        ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", desiredLocale);
        this.localPatternChars = patternChars;
        ULocale uloc = rb.getULocale();
        setLocale(uloc, uloc);
        this.capitalization = new HashMap();
        boolean[] noTransforms = {false, false};
        CapitalizationContextUsage[] allUsages = CapitalizationContextUsage.valuesCustom();
        for (CapitalizationContextUsage capitalizationContextUsage : allUsages) {
            this.capitalization.put(capitalizationContextUsage, noTransforms);
        }
        try {
            contextTransformsBundle = rb.getWithFallback("contextTransforms");
        } catch (MissingResourceException e5) {
            contextTransformsBundle = null;
        }
        if (contextTransformsBundle != null) {
            UResourceBundleIterator ctIterator = contextTransformsBundle.getIterator();
            while (ctIterator.hasNext()) {
                UResourceBundle contextTransformUsage = ctIterator.next();
                int[] intVector = contextTransformUsage.getIntVector();
                if (intVector.length >= 2) {
                    String usageKey = contextTransformUsage.getKey();
                    CapitalizationContextUsage usage = contextUsageTypeMap.get(usageKey);
                    if (usage != null) {
                        boolean[] transforms = new boolean[2];
                        transforms[0] = intVector[0] != 0;
                        transforms[1] = intVector[1] != 0;
                        this.capitalization.put(usage, transforms);
                    }
                }
            }
        }
        NumberingSystem ns = NumberingSystem.getInstance(desiredLocale);
        String nsName = ns == null ? "latn" : ns.getName();
        String tsPath = "NumberElements/" + nsName + "/symbols/timeSeparator";
        try {
            setTimeSeparatorString(rb.getStringWithFallback(tsPath));
        } catch (MissingResourceException e6) {
            setTimeSeparatorString(DEFAULT_TIME_SEPARATOR);
        }
    }

    private static final boolean arrayOfArrayEquals(Object[][] aa1, Object[][] aa2) {
        if (aa1 == aa2) {
            return true;
        }
        if (aa1 == null || aa2 == null || aa1.length != aa2.length) {
            return false;
        }
        boolean equal = true;
        for (int i = 0; i < aa1.length && (equal = Utility.arrayEquals(aa1[i], (Object) aa2[i])); i++) {
        }
        return equal;
    }

    private final String[] duplicate(String[] srcArray) {
        return (String[]) srcArray.clone();
    }

    private final String[][] duplicate(String[][] srcArray) {
        String[][] aCopy = new String[srcArray.length][];
        for (int i = 0; i < srcArray.length; i++) {
            aCopy[i] = duplicate(srcArray[i]);
        }
        return aCopy;
    }

    public DateFormatSymbols(Calendar cal, Locale locale) {
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
        this.capitalization = null;
        initializeData(ULocale.forLocale(locale), cal.getType());
    }

    public DateFormatSymbols(Calendar cal, ULocale locale) {
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
        this.capitalization = null;
        initializeData(locale, cal.getType());
    }

    public DateFormatSymbols(Class<? extends Calendar> calendarClass, Locale locale) {
        this(calendarClass, ULocale.forLocale(locale));
    }

    public DateFormatSymbols(Class<? extends Calendar> calendarClass, ULocale locale) {
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
        this.capitalization = null;
        String fullName = calendarClass.getName();
        int lastDot = fullName.lastIndexOf(46);
        String className = fullName.substring(lastDot + 1);
        String calType = null;
        String[][] strArr = CALENDAR_CLASSES;
        int length = strArr.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            String[] calClassInfo = strArr[i];
            if (!calClassInfo[0].equals(className)) {
                i++;
            } else {
                calType = calClassInfo[1];
                break;
            }
        }
        initializeData(locale, calType == null ? className.replaceAll("Calendar", "").toLowerCase(Locale.ENGLISH) : calType);
    }

    public DateFormatSymbols(ResourceBundle bundle, Locale locale) {
        this(bundle, ULocale.forLocale(locale));
    }

    public DateFormatSymbols(ResourceBundle bundle, ULocale locale) {
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
        this.capitalization = null;
        initializeData(locale, new CalendarData((ICUResourceBundle) bundle, CalendarUtil.getCalendarType(locale)));
    }

    @Deprecated
    public static ResourceBundle getDateFormatBundle(Class<? extends Calendar> calendarClass, Locale locale) throws MissingResourceException {
        return null;
    }

    @Deprecated
    public static ResourceBundle getDateFormatBundle(Class<? extends Calendar> calendarClass, ULocale locale) throws MissingResourceException {
        return null;
    }

    @Deprecated
    public static ResourceBundle getDateFormatBundle(Calendar cal, Locale locale) throws MissingResourceException {
        return null;
    }

    @Deprecated
    public static ResourceBundle getDateFormatBundle(Calendar cal, ULocale locale) throws MissingResourceException {
        return null;
    }

    public final ULocale getLocale(ULocale.Type type) {
        return type == ULocale.ACTUAL_LOCALE ? this.actualLocale : this.validLocale;
    }

    final void setLocale(ULocale valid, ULocale actual) {
        if ((valid == null) != (actual == null)) {
            throw new IllegalArgumentException();
        }
        this.validLocale = valid;
        this.actualLocale = actual;
    }

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        stream.defaultReadObject();
    }
}
