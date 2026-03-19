package android.icu.text;

import android.icu.impl.CalendarData;
import android.icu.impl.DateNumberFormat;
import android.icu.impl.ICUCache;
import android.icu.impl.PatternProps;
import android.icu.impl.PatternTokenizer;
import android.icu.impl.SimpleCache;
import android.icu.lang.UCharacter;
import android.icu.text.DateFormat;
import android.icu.text.DateFormatSymbols;
import android.icu.text.DisplayContext;
import android.icu.text.TimeZoneFormat;
import android.icu.util.BasicTimeZone;
import android.icu.util.Calendar;
import android.icu.util.HebrewCalendar;
import android.icu.util.Output;
import android.icu.util.TimeZone;
import android.icu.util.TimeZoneTransition;
import android.icu.util.ULocale;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.UUID;

public class SimpleDateFormat extends DateFormat {

    private static final int[] f90androidicutextDisplayContextSwitchesValues = null;
    private static final int DECIMAL_BUF_SIZE = 10;
    private static final String FALLBACKPATTERN = "yy/MM/dd HH:mm";
    private static final int HEBREW_CAL_CUR_MILLENIUM_END_YEAR = 6000;
    private static final int HEBREW_CAL_CUR_MILLENIUM_START_YEAR = 5000;
    private static final int ISOSpecialEra = -32000;
    private static final String NUMERIC_FORMAT_CHARS = "ADdFgHhKkmrSsuWwYy";
    private static final String NUMERIC_FORMAT_CHARS2 = "ceLMQq";
    private static final String SUPPRESS_NEGATIVE_PREFIX = "\uab00";
    static final int currentSerialVersion = 2;
    private static final int millisPerHour = 3600000;
    private static final long serialVersionUID = 4774881970558875024L;
    private transient BreakIterator capitalizationBrkIter;
    private transient char[] decDigits;
    private transient char[] decimalBuf;
    private transient long defaultCenturyBase;
    private Date defaultCenturyStart;
    private transient int defaultCenturyStartYear;
    private DateFormatSymbols formatData;
    private transient ULocale locale;
    private HashMap<String, NumberFormat> numberFormatters;
    private String override;
    private HashMap<Character, String> overrideMap;
    private String pattern;
    private transient Object[] patternItems;
    private int serialVersionOnStream;
    private volatile TimeZoneFormat tzFormat;
    private transient boolean useFastFormat;
    private transient boolean useLocalZeroPaddingNumberFormat;
    static boolean DelayedHebrewMonthCheck = false;
    private static final int[] CALENDAR_FIELD_TO_LEVEL = {0, 10, 20, 20, 30, 30, 20, 30, 30, 40, 50, 50, 60, 70, 80, 0, 0, 10, 30, 10, 0, 40, 0, 0};
    private static final int[] PATTERN_CHAR_TO_LEVEL = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 40, -1, -1, 20, 30, 30, 0, 50, -1, -1, 50, 20, 20, -1, 0, -1, 20, -1, 80, -1, 10, 0, 30, 0, 10, 0, -1, -1, -1, -1, -1, -1, 40, -1, 30, 30, 30, -1, 0, 50, -1, -1, 50, -1, 60, -1, -1, -1, 20, 10, 70, -1, 10, 0, 20, 0, 10, 0, -1, -1, -1, -1, -1};
    private static final boolean[] PATTERN_CHAR_IS_SYNTAX = {false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false, false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false, false, false};
    private static ULocale cachedDefaultLocale = null;
    private static String cachedDefaultPattern = null;
    private static final int[] PATTERN_CHAR_TO_INDEX = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 22, -1, -1, 10, 9, 11, 0, 5, -1, -1, 16, 26, 2, -1, 31, -1, 27, -1, 8, -1, 30, 29, 13, 32, 18, 23, -1, -1, -1, -1, -1, -1, 14, -1, 25, 3, 19, -1, 21, 15, -1, -1, 4, -1, 6, -1, -1, -1, 28, 34, 7, -1, 20, 24, 12, 33, 1, 17, -1, -1, -1, -1, -1};
    private static final int[] PATTERN_INDEX_TO_CALENDAR_FIELD = {0, 1, 2, 5, 11, 11, 12, 13, 14, 7, 6, 8, 3, 4, 9, 10, 10, 15, 17, 18, 19, 20, 21, 15, 15, 18, 2, 2, 2, 15, 1, 15, 15, 15, 19, -1};
    private static final int[] PATTERN_INDEX_TO_DATE_FORMAT_FIELD = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35};
    private static final DateFormat.Field[] PATTERN_INDEX_TO_DATE_FORMAT_ATTRIBUTE = {DateFormat.Field.ERA, DateFormat.Field.YEAR, DateFormat.Field.MONTH, DateFormat.Field.DAY_OF_MONTH, DateFormat.Field.HOUR_OF_DAY1, DateFormat.Field.HOUR_OF_DAY0, DateFormat.Field.MINUTE, DateFormat.Field.SECOND, DateFormat.Field.MILLISECOND, DateFormat.Field.DAY_OF_WEEK, DateFormat.Field.DAY_OF_YEAR, DateFormat.Field.DAY_OF_WEEK_IN_MONTH, DateFormat.Field.WEEK_OF_YEAR, DateFormat.Field.WEEK_OF_MONTH, DateFormat.Field.AM_PM, DateFormat.Field.HOUR1, DateFormat.Field.HOUR0, DateFormat.Field.TIME_ZONE, DateFormat.Field.YEAR_WOY, DateFormat.Field.DOW_LOCAL, DateFormat.Field.EXTENDED_YEAR, DateFormat.Field.JULIAN_DAY, DateFormat.Field.MILLISECONDS_IN_DAY, DateFormat.Field.TIME_ZONE, DateFormat.Field.TIME_ZONE, DateFormat.Field.DAY_OF_WEEK, DateFormat.Field.MONTH, DateFormat.Field.QUARTER, DateFormat.Field.QUARTER, DateFormat.Field.TIME_ZONE, DateFormat.Field.YEAR, DateFormat.Field.TIME_ZONE, DateFormat.Field.TIME_ZONE, DateFormat.Field.TIME_ZONE, DateFormat.Field.RELATED_YEAR, DateFormat.Field.TIME_SEPARATOR};
    private static ICUCache<String, Object[]> PARSED_PATTERN_CACHE = new SimpleCache();
    static final UnicodeSet DATE_PATTERN_TYPE = new UnicodeSet("[GyYuUQqMLlwWd]").freeze();

    private static int[] m197getandroidicutextDisplayContextSwitchesValues() {
        if (f90androidicutextDisplayContextSwitchesValues != null) {
            return f90androidicutextDisplayContextSwitchesValues;
        }
        int[] iArr = new int[DisplayContext.valuesCustom().length];
        try {
            iArr[DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[DisplayContext.CAPITALIZATION_FOR_MIDDLE_OF_SENTENCE.ordinal()] = 4;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[DisplayContext.CAPITALIZATION_FOR_STANDALONE.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU.ordinal()] = 3;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[DisplayContext.CAPITALIZATION_NONE.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[DisplayContext.DIALECT_NAMES.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[DisplayContext.LENGTH_FULL.ordinal()] = 7;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[DisplayContext.LENGTH_SHORT.ordinal()] = 8;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[DisplayContext.STANDARD_NAMES.ordinal()] = 9;
        } catch (NoSuchFieldError e9) {
        }
        f90androidicutextDisplayContextSwitchesValues = iArr;
        return iArr;
    }

    private static int getLevelFromChar(char ch) {
        if (ch < PATTERN_CHAR_TO_LEVEL.length) {
            return PATTERN_CHAR_TO_LEVEL[ch & 255];
        }
        return -1;
    }

    private static boolean isSyntaxChar(char ch) {
        if (ch < PATTERN_CHAR_IS_SYNTAX.length) {
            return PATTERN_CHAR_IS_SYNTAX[ch & 255];
        }
        return false;
    }

    private enum ContextValue {
        UNKNOWN,
        CAPITALIZATION_FOR_MIDDLE_OF_SENTENCE,
        CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE,
        CAPITALIZATION_FOR_UI_LIST_OR_MENU,
        CAPITALIZATION_FOR_STANDALONE;

        public static ContextValue[] valuesCustom() {
            return values();
        }
    }

    public SimpleDateFormat() {
        this(getDefaultPattern(), null, null, null, null, true, null);
    }

    public SimpleDateFormat(String pattern) {
        this(pattern, null, null, null, null, true, null);
    }

    public SimpleDateFormat(String pattern, Locale loc) {
        this(pattern, null, null, null, ULocale.forLocale(loc), true, null);
    }

    public SimpleDateFormat(String pattern, ULocale loc) {
        this(pattern, null, null, null, loc, true, null);
    }

    public SimpleDateFormat(String pattern, String override, ULocale loc) {
        this(pattern, null, null, null, loc, false, override);
    }

    public SimpleDateFormat(String pattern, DateFormatSymbols formatData) {
        this(pattern, (DateFormatSymbols) formatData.clone(), null, null, null, true, null);
    }

    @Deprecated
    public SimpleDateFormat(String pattern, DateFormatSymbols formatData, ULocale loc) {
        this(pattern, (DateFormatSymbols) formatData.clone(), null, null, loc, true, null);
    }

    SimpleDateFormat(String pattern, DateFormatSymbols formatData, Calendar calendar, ULocale locale, boolean useFastFormat, String override) {
        this(pattern, (DateFormatSymbols) formatData.clone(), (Calendar) calendar.clone(), null, locale, useFastFormat, override);
    }

    private SimpleDateFormat(String pattern, DateFormatSymbols formatData, Calendar calendar, NumberFormat numberFormat, ULocale locale, boolean useFastFormat, String override) {
        this.serialVersionOnStream = 2;
        this.capitalizationBrkIter = null;
        this.pattern = pattern;
        this.formatData = formatData;
        this.calendar = calendar;
        this.numberFormat = numberFormat;
        this.locale = locale;
        this.useFastFormat = useFastFormat;
        this.override = override;
        initialize();
    }

    @Deprecated
    public static SimpleDateFormat getInstance(Calendar.FormatConfiguration formatConfig) {
        String ostr = formatConfig.getOverrideString();
        boolean useFast = ostr != null && ostr.length() > 0;
        return new SimpleDateFormat(formatConfig.getPatternString(), formatConfig.getDateFormatSymbols(), formatConfig.getCalendar(), null, formatConfig.getLocale(), useFast, formatConfig.getOverrideString());
    }

    private void initialize() {
        if (this.locale == null) {
            this.locale = ULocale.getDefault(ULocale.Category.FORMAT);
        }
        if (this.formatData == null) {
            this.formatData = new DateFormatSymbols(this.locale);
        }
        if (this.calendar == null) {
            this.calendar = Calendar.getInstance(this.locale);
        }
        if (this.numberFormat == null) {
            NumberingSystem ns = NumberingSystem.getInstance(this.locale);
            if (ns.isAlgorithmic()) {
                this.numberFormat = NumberFormat.getInstance(this.locale);
            } else {
                String digitString = ns.getDescription();
                String nsName = ns.getName();
                this.numberFormat = new DateNumberFormat(this.locale, digitString, nsName);
            }
        }
        this.defaultCenturyBase = System.currentTimeMillis();
        setLocale(this.calendar.getLocale(ULocale.VALID_LOCALE), this.calendar.getLocale(ULocale.ACTUAL_LOCALE));
        initLocalZeroPaddingNumberFormat();
        if (this.override == null) {
            return;
        }
        initNumberFormatters(this.locale);
    }

    private synchronized void initializeTimeZoneFormat(boolean bForceUpdate) {
        if (!bForceUpdate) {
            if (this.tzFormat == null) {
            }
        }
        this.tzFormat = TimeZoneFormat.getInstance(this.locale);
        String str = null;
        if (this.numberFormat instanceof DecimalFormat) {
            DecimalFormatSymbols decsym = ((DecimalFormat) this.numberFormat).getDecimalFormatSymbols();
            str = new String(decsym.getDigits());
        } else if (this.numberFormat instanceof DateNumberFormat) {
            str = new String(((DateNumberFormat) this.numberFormat).getDigits());
        }
        if (str != null && !this.tzFormat.getGMTOffsetDigits().equals(str)) {
            if (this.tzFormat.isFrozen()) {
                this.tzFormat = this.tzFormat.cloneAsThawed();
            }
            this.tzFormat.setGMTOffsetDigits(str);
        }
    }

    private TimeZoneFormat tzFormat() {
        if (this.tzFormat == null) {
            initializeTimeZoneFormat(false);
        }
        return this.tzFormat;
    }

    private static synchronized String getDefaultPattern() {
        ULocale defaultLocale = ULocale.getDefault(ULocale.Category.FORMAT);
        if (!defaultLocale.equals(cachedDefaultLocale)) {
            cachedDefaultLocale = defaultLocale;
            Calendar cal = Calendar.getInstance(cachedDefaultLocale);
            try {
                CalendarData calData = new CalendarData(cachedDefaultLocale, cal.getType());
                String[] dateTimePatterns = calData.getDateTimePatterns();
                int glueIndex = 8;
                if (dateTimePatterns.length >= 13) {
                    glueIndex = 12;
                }
                cachedDefaultPattern = MessageFormat.format(dateTimePatterns[glueIndex], dateTimePatterns[3], dateTimePatterns[7]);
            } catch (MissingResourceException e) {
                cachedDefaultPattern = FALLBACKPATTERN;
            }
        }
        return cachedDefaultPattern;
    }

    private void parseAmbiguousDatesAsAfter(Date startDate) {
        this.defaultCenturyStart = startDate;
        this.calendar.setTime(startDate);
        this.defaultCenturyStartYear = this.calendar.get(1);
    }

    private void initializeDefaultCenturyStart(long baseTime) {
        this.defaultCenturyBase = baseTime;
        Calendar tmpCal = (Calendar) this.calendar.clone();
        tmpCal.setTimeInMillis(baseTime);
        tmpCal.add(1, -80);
        this.defaultCenturyStart = tmpCal.getTime();
        this.defaultCenturyStartYear = tmpCal.get(1);
    }

    private Date getDefaultCenturyStart() {
        if (this.defaultCenturyStart == null) {
            initializeDefaultCenturyStart(this.defaultCenturyBase);
        }
        return this.defaultCenturyStart;
    }

    private int getDefaultCenturyStartYear() {
        if (this.defaultCenturyStart == null) {
            initializeDefaultCenturyStart(this.defaultCenturyBase);
        }
        return this.defaultCenturyStartYear;
    }

    public void set2DigitYearStart(Date startDate) {
        parseAmbiguousDatesAsAfter(startDate);
    }

    public Date get2DigitYearStart() {
        return getDefaultCenturyStart();
    }

    @Override
    public void setContext(DisplayContext context) {
        super.setContext(context);
        if (this.capitalizationBrkIter != null) {
            return;
        }
        if (context != DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE && context != DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU && context != DisplayContext.CAPITALIZATION_FOR_STANDALONE) {
            return;
        }
        this.capitalizationBrkIter = BreakIterator.getSentenceInstance(this.locale);
    }

    @Override
    public StringBuffer format(Calendar cal, StringBuffer toAppendTo, FieldPosition pos) {
        TimeZone backupTZ = null;
        if (cal != this.calendar && !cal.getType().equals(this.calendar.getType())) {
            this.calendar.setTimeInMillis(cal.getTimeInMillis());
            backupTZ = this.calendar.getTimeZone();
            this.calendar.setTimeZone(cal.getTimeZone());
            cal = this.calendar;
        }
        StringBuffer result = format(cal, getContext(DisplayContext.Type.CAPITALIZATION), toAppendTo, pos, null);
        if (backupTZ != null) {
            this.calendar.setTimeZone(backupTZ);
        }
        return result;
    }

    private StringBuffer format(Calendar cal, DisplayContext capitalizationContext, StringBuffer toAppendTo, FieldPosition pos, List<FieldPosition> attributes) {
        pos.setBeginIndex(0);
        pos.setEndIndex(0);
        Object[] items = getPatternItems();
        for (int i = 0; i < items.length; i++) {
            if (items[i] instanceof String) {
                toAppendTo.append((String) items[i]);
            } else {
                PatternItem item = (PatternItem) items[i];
                int start = 0;
                if (attributes != null) {
                    start = toAppendTo.length();
                }
                if (this.useFastFormat) {
                    subFormat(toAppendTo, item.type, item.length, toAppendTo.length(), i, capitalizationContext, pos, cal);
                } else {
                    toAppendTo.append(subFormat(item.type, item.length, toAppendTo.length(), i, capitalizationContext, pos, cal));
                }
                if (attributes != null) {
                    int end = toAppendTo.length();
                    if (end - start > 0) {
                        DateFormat.Field attr = patternCharToDateFormatField(item.type);
                        FieldPosition fp = new FieldPosition(attr);
                        fp.setBeginIndex(start);
                        fp.setEndIndex(end);
                        attributes.add(fp);
                    }
                }
            }
        }
        return toAppendTo;
    }

    private static int getIndexFromChar(char ch) {
        if (ch < PATTERN_CHAR_TO_INDEX.length) {
            return PATTERN_CHAR_TO_INDEX[ch & 255];
        }
        return -1;
    }

    protected DateFormat.Field patternCharToDateFormatField(char ch) {
        int patternCharIndex = getIndexFromChar(ch);
        if (patternCharIndex != -1) {
            return PATTERN_INDEX_TO_DATE_FORMAT_ATTRIBUTE[patternCharIndex];
        }
        return null;
    }

    protected String subFormat(char ch, int count, int beginOffset, FieldPosition pos, DateFormatSymbols fmtData, Calendar cal) throws IllegalArgumentException {
        return subFormat(ch, count, beginOffset, 0, DisplayContext.CAPITALIZATION_NONE, pos, cal);
    }

    @Deprecated
    protected String subFormat(char ch, int count, int beginOffset, int fieldNum, DisplayContext capitalizationContext, FieldPosition pos, Calendar cal) {
        StringBuffer buf = new StringBuffer();
        subFormat(buf, ch, count, beginOffset, fieldNum, capitalizationContext, pos, cal);
        return buf.toString();
    }

    @Deprecated
    protected void subFormat(StringBuffer buf, char ch, int count, int beginOffset, int fieldNum, DisplayContext capitalizationContext, FieldPosition pos, Calendar cal) {
        String result;
        int bufstart = buf.length();
        TimeZone tz = cal.getTimeZone();
        long date = cal.getTimeInMillis();
        String result2 = null;
        int patternCharIndex = getIndexFromChar(ch);
        if (patternCharIndex == -1) {
            if (ch != 'l') {
                throw new IllegalArgumentException("Illegal pattern character '" + ch + "' in \"" + this.pattern + '\"');
            }
            return;
        }
        int field = PATTERN_INDEX_TO_CALENDAR_FIELD[patternCharIndex];
        int value = field >= 0 ? patternCharIndex != 34 ? cal.get(field) : cal.getRelatedYear() : 0;
        NumberFormat currentNumberFormat = getNumberFormat(ch);
        DateFormatSymbols.CapitalizationContextUsage capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.OTHER;
        switch (patternCharIndex) {
            case 0:
                if (cal.getType().equals("chinese") || cal.getType().equals("dangi")) {
                    zeroPaddingNumber(currentNumberFormat, buf, value, 1, 9);
                } else if (count == 5) {
                    safeAppend(this.formatData.narrowEras, value, buf);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.ERA_NARROW;
                } else if (count != 4) {
                    safeAppend(this.formatData.eras, value, buf);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.ERA_ABBREV;
                } else {
                    safeAppend(this.formatData.eraNames, value, buf);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.ERA_WIDE;
                }
                break;
            case 1:
            case 18:
                if (this.override != null && ((this.override.compareTo("hebr") == 0 || this.override.indexOf("y=hebr") >= 0) && value > HEBREW_CAL_CUR_MILLENIUM_START_YEAR && value < HEBREW_CAL_CUR_MILLENIUM_END_YEAR)) {
                    value -= 5000;
                }
                if (count != 2) {
                    zeroPaddingNumber(currentNumberFormat, buf, value, count, Integer.MAX_VALUE);
                } else {
                    zeroPaddingNumber(currentNumberFormat, buf, value, 2, 2);
                }
                break;
            case 2:
            case 26:
                if (cal.getType().equals("hebrew")) {
                    boolean isLeap = HebrewCalendar.isLeapYear(cal.get(1));
                    if (isLeap && value == 6 && count >= 3) {
                        value = 13;
                    }
                    if (!isLeap && value >= 6 && count < 3) {
                        value--;
                    }
                }
                int isLeapMonth = (this.formatData.leapMonthPatterns == null || this.formatData.leapMonthPatterns.length < 7) ? 0 : cal.get(22);
                if (count == 5) {
                    if (patternCharIndex == 2) {
                        safeAppendWithMonthPattern(this.formatData.narrowMonths, value, buf, isLeapMonth != 0 ? this.formatData.leapMonthPatterns[2] : null);
                    } else {
                        safeAppendWithMonthPattern(this.formatData.standaloneNarrowMonths, value, buf, isLeapMonth != 0 ? this.formatData.leapMonthPatterns[5] : null);
                    }
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.MONTH_NARROW;
                } else if (count != 4) {
                    if (count != 3) {
                        StringBuffer monthNumber = new StringBuffer();
                        zeroPaddingNumber(currentNumberFormat, monthNumber, value + 1, count, Integer.MAX_VALUE);
                        String[] monthNumberStrings = {monthNumber.toString()};
                        safeAppendWithMonthPattern(monthNumberStrings, 0, buf, isLeapMonth != 0 ? this.formatData.leapMonthPatterns[6] : null);
                    } else if (patternCharIndex != 2) {
                        safeAppendWithMonthPattern(this.formatData.standaloneShortMonths, value, buf, isLeapMonth != 0 ? this.formatData.leapMonthPatterns[4] : null);
                        capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.MONTH_STANDALONE;
                    } else {
                        safeAppendWithMonthPattern(this.formatData.shortMonths, value, buf, isLeapMonth != 0 ? this.formatData.leapMonthPatterns[1] : null);
                        capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.MONTH_FORMAT;
                    }
                } else if (patternCharIndex != 2) {
                    safeAppendWithMonthPattern(this.formatData.standaloneMonths, value, buf, isLeapMonth != 0 ? this.formatData.leapMonthPatterns[3] : null);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.MONTH_STANDALONE;
                } else {
                    safeAppendWithMonthPattern(this.formatData.months, value, buf, isLeapMonth != 0 ? this.formatData.leapMonthPatterns[0] : null);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.MONTH_FORMAT;
                }
                break;
            case 3:
            case 5:
            case 6:
            case 7:
            case 10:
            case 11:
            case 12:
            case 13:
            case 16:
            case 20:
            case 21:
            case 22:
            case 34:
            default:
                zeroPaddingNumber(currentNumberFormat, buf, value, count, Integer.MAX_VALUE);
                break;
            case 4:
                if (value != 0) {
                    zeroPaddingNumber(currentNumberFormat, buf, value, count, Integer.MAX_VALUE);
                } else {
                    zeroPaddingNumber(currentNumberFormat, buf, cal.getMaximum(11) + 1, count, Integer.MAX_VALUE);
                }
                break;
            case 8:
                this.numberFormat.setMinimumIntegerDigits(Math.min(3, count));
                this.numberFormat.setMaximumIntegerDigits(Integer.MAX_VALUE);
                if (count == 1) {
                    value /= 100;
                } else if (count == 2) {
                    value /= 10;
                }
                FieldPosition p = new FieldPosition(-1);
                this.numberFormat.format(value, buf, p);
                if (count > 3) {
                    this.numberFormat.setMinimumIntegerDigits(count - 3);
                    this.numberFormat.format(0L, buf, p);
                }
                break;
            case 9:
                if (count != 5) {
                    safeAppend(this.formatData.narrowWeekdays, value, buf);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.DAY_NARROW;
                } else if (count == 4) {
                    safeAppend(this.formatData.weekdays, value, buf);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.DAY_FORMAT;
                } else if (count == 6 && this.formatData.shorterWeekdays != null) {
                    safeAppend(this.formatData.shorterWeekdays, value, buf);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.DAY_FORMAT;
                } else {
                    safeAppend(this.formatData.shortWeekdays, value, buf);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.DAY_FORMAT;
                }
                break;
            case 14:
                if (count >= 5 && this.formatData.ampmsNarrow != null) {
                    safeAppend(this.formatData.ampmsNarrow, value, buf);
                } else {
                    safeAppend(this.formatData.ampms, value, buf);
                }
                break;
            case 15:
                if (value != 0) {
                    zeroPaddingNumber(currentNumberFormat, buf, value, count, Integer.MAX_VALUE);
                } else {
                    zeroPaddingNumber(currentNumberFormat, buf, cal.getLeastMaximum(10) + 1, count, Integer.MAX_VALUE);
                }
                break;
            case 17:
                if (count < 4) {
                    result = tzFormat().format(TimeZoneFormat.Style.SPECIFIC_SHORT, tz, date);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.METAZONE_SHORT;
                } else {
                    result = tzFormat().format(TimeZoneFormat.Style.SPECIFIC_LONG, tz, date);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.METAZONE_LONG;
                }
                buf.append(result);
                break;
            case 19:
                if (count < 3) {
                    zeroPaddingNumber(currentNumberFormat, buf, value, count, Integer.MAX_VALUE);
                    break;
                } else {
                    value = cal.get(7);
                    if (count != 5) {
                    }
                }
                break;
            case 23:
                buf.append(count < 4 ? tzFormat().format(TimeZoneFormat.Style.ISO_BASIC_LOCAL_FULL, tz, date) : count == 5 ? tzFormat().format(TimeZoneFormat.Style.ISO_EXTENDED_FULL, tz, date) : tzFormat().format(TimeZoneFormat.Style.LOCALIZED_GMT, tz, date));
                break;
            case 24:
                if (count == 1) {
                    result2 = tzFormat().format(TimeZoneFormat.Style.GENERIC_SHORT, tz, date);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.METAZONE_SHORT;
                } else if (count == 4) {
                    result2 = tzFormat().format(TimeZoneFormat.Style.GENERIC_LONG, tz, date);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.METAZONE_LONG;
                }
                buf.append(result2);
                break;
            case 25:
                if (count >= 3) {
                    int value2 = cal.get(7);
                    if (count == 5) {
                        safeAppend(this.formatData.standaloneNarrowWeekdays, value2, buf);
                        capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.DAY_NARROW;
                    } else if (count == 4) {
                        safeAppend(this.formatData.standaloneWeekdays, value2, buf);
                        capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.DAY_STANDALONE;
                    } else if (count == 6 && this.formatData.standaloneShorterWeekdays != null) {
                        safeAppend(this.formatData.standaloneShorterWeekdays, value2, buf);
                        capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.DAY_STANDALONE;
                    } else {
                        safeAppend(this.formatData.standaloneShortWeekdays, value2, buf);
                        capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.DAY_STANDALONE;
                    }
                } else {
                    zeroPaddingNumber(currentNumberFormat, buf, value, 1, Integer.MAX_VALUE);
                }
                break;
            case 27:
                if (count >= 4) {
                    safeAppend(this.formatData.quarters, value / 3, buf);
                } else if (count != 3) {
                    zeroPaddingNumber(currentNumberFormat, buf, (value / 3) + 1, count, Integer.MAX_VALUE);
                } else {
                    safeAppend(this.formatData.shortQuarters, value / 3, buf);
                }
                break;
            case 28:
                if (count >= 4) {
                    safeAppend(this.formatData.standaloneQuarters, value / 3, buf);
                } else if (count != 3) {
                    zeroPaddingNumber(currentNumberFormat, buf, (value / 3) + 1, count, Integer.MAX_VALUE);
                } else {
                    safeAppend(this.formatData.standaloneShortQuarters, value / 3, buf);
                }
                break;
            case 29:
                if (count == 1) {
                    result2 = tzFormat().format(TimeZoneFormat.Style.ZONE_ID_SHORT, tz, date);
                } else if (count == 2) {
                    result2 = tzFormat().format(TimeZoneFormat.Style.ZONE_ID, tz, date);
                } else if (count == 3) {
                    result2 = tzFormat().format(TimeZoneFormat.Style.EXEMPLAR_LOCATION, tz, date);
                } else if (count == 4) {
                    result2 = tzFormat().format(TimeZoneFormat.Style.GENERIC_LOCATION, tz, date);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.ZONE_LONG;
                }
                buf.append(result2);
                break;
            case 30:
                if (this.formatData.shortYearNames != null && value <= this.formatData.shortYearNames.length) {
                    safeAppend(this.formatData.shortYearNames, value - 1, buf);
                    break;
                }
                break;
            case 31:
                if (count == 1) {
                    result2 = tzFormat().format(TimeZoneFormat.Style.LOCALIZED_GMT_SHORT, tz, date);
                } else if (count == 4) {
                    result2 = tzFormat().format(TimeZoneFormat.Style.LOCALIZED_GMT, tz, date);
                }
                buf.append(result2);
                break;
            case 32:
                if (count == 1) {
                    result2 = tzFormat().format(TimeZoneFormat.Style.ISO_BASIC_SHORT, tz, date);
                } else if (count == 2) {
                    result2 = tzFormat().format(TimeZoneFormat.Style.ISO_BASIC_FIXED, tz, date);
                } else if (count == 3) {
                    result2 = tzFormat().format(TimeZoneFormat.Style.ISO_EXTENDED_FIXED, tz, date);
                } else if (count == 4) {
                    result2 = tzFormat().format(TimeZoneFormat.Style.ISO_BASIC_FULL, tz, date);
                } else if (count == 5) {
                    result2 = tzFormat().format(TimeZoneFormat.Style.ISO_EXTENDED_FULL, tz, date);
                }
                buf.append(result2);
                break;
            case 33:
                if (count == 1) {
                    result2 = tzFormat().format(TimeZoneFormat.Style.ISO_BASIC_LOCAL_SHORT, tz, date);
                } else if (count == 2) {
                    result2 = tzFormat().format(TimeZoneFormat.Style.ISO_BASIC_LOCAL_FIXED, tz, date);
                } else if (count == 3) {
                    result2 = tzFormat().format(TimeZoneFormat.Style.ISO_EXTENDED_LOCAL_FIXED, tz, date);
                } else if (count == 4) {
                    result2 = tzFormat().format(TimeZoneFormat.Style.ISO_BASIC_LOCAL_FULL, tz, date);
                } else if (count == 5) {
                    result2 = tzFormat().format(TimeZoneFormat.Style.ISO_EXTENDED_LOCAL_FULL, tz, date);
                }
                buf.append(result2);
                break;
            case 35:
                buf.append(this.formatData.getTimeSeparatorString());
                break;
        }
        if (fieldNum == 0 && capitalizationContext != null && UCharacter.isLowerCase(buf.codePointAt(bufstart))) {
            boolean titlecase = false;
            switch (m197getandroidicutextDisplayContextSwitchesValues()[capitalizationContext.ordinal()]) {
                case 1:
                    titlecase = true;
                    break;
                case 2:
                case 3:
                    if (this.formatData.capitalization != null) {
                        boolean[] transforms = this.formatData.capitalization.get(capContextUsageType);
                        titlecase = capitalizationContext != DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU ? transforms[1] : transforms[0];
                    }
                    break;
            }
            if (titlecase) {
                if (this.capitalizationBrkIter == null) {
                    this.capitalizationBrkIter = BreakIterator.getSentenceInstance(this.locale);
                }
                String firstField = buf.substring(bufstart);
                String firstFieldTitleCase = UCharacter.toTitleCase(this.locale, firstField, this.capitalizationBrkIter, 768);
                buf.replace(bufstart, buf.length(), firstFieldTitleCase);
            }
        }
        if (pos.getBeginIndex() == pos.getEndIndex()) {
            if (pos.getField() == PATTERN_INDEX_TO_DATE_FORMAT_FIELD[patternCharIndex]) {
                pos.setBeginIndex(beginOffset);
                pos.setEndIndex((buf.length() + beginOffset) - bufstart);
            } else if (pos.getFieldAttribute() == PATTERN_INDEX_TO_DATE_FORMAT_ATTRIBUTE[patternCharIndex]) {
                pos.setBeginIndex(beginOffset);
                pos.setEndIndex((buf.length() + beginOffset) - bufstart);
            }
        }
    }

    private static void safeAppend(String[] array, int value, StringBuffer appendTo) {
        if (array == null || value < 0 || value >= array.length) {
            return;
        }
        appendTo.append(array[value]);
    }

    private static void safeAppendWithMonthPattern(String[] array, int value, StringBuffer appendTo, String monthPattern) {
        if (array == null || value < 0 || value >= array.length) {
            return;
        }
        if (monthPattern != null) {
            appendTo.append(MessageFormat.format(monthPattern, array[value]));
        } else {
            appendTo.append(array[value]);
        }
    }

    private static class PatternItem {
        final boolean isNumeric;
        final int length;
        final char type;

        PatternItem(char type, int length) {
            this.type = type;
            this.length = length;
            this.isNumeric = SimpleDateFormat.isNumeric(type, length);
        }
    }

    private Object[] getPatternItems() {
        if (this.patternItems != null) {
            return this.patternItems;
        }
        this.patternItems = PARSED_PATTERN_CACHE.get(this.pattern);
        if (this.patternItems != null) {
            return this.patternItems;
        }
        boolean isPrevQuote = false;
        boolean inQuote = false;
        StringBuilder text = new StringBuilder();
        char itemType = 0;
        int itemLength = 1;
        List<Object> items = new ArrayList<>();
        for (int i = 0; i < this.pattern.length(); i++) {
            char ch = this.pattern.charAt(i);
            if (ch == '\'') {
                if (isPrevQuote) {
                    text.append(PatternTokenizer.SINGLE_QUOTE);
                    isPrevQuote = false;
                } else {
                    isPrevQuote = true;
                    if (itemType != 0) {
                        items.add(new PatternItem(itemType, itemLength));
                        itemType = 0;
                    }
                }
                inQuote = !inQuote;
            } else {
                isPrevQuote = false;
                if (inQuote) {
                    text.append(ch);
                } else if (isSyntaxChar(ch)) {
                    if (ch == itemType) {
                        itemLength++;
                    } else {
                        if (itemType == 0) {
                            if (text.length() > 0) {
                                items.add(text.toString());
                                text.setLength(0);
                            }
                        } else {
                            items.add(new PatternItem(itemType, itemLength));
                        }
                        itemType = ch;
                        itemLength = 1;
                    }
                } else {
                    if (itemType != 0) {
                        items.add(new PatternItem(itemType, itemLength));
                        itemType = 0;
                    }
                    text.append(ch);
                }
            }
        }
        if (itemType == 0) {
            if (text.length() > 0) {
                items.add(text.toString());
                text.setLength(0);
            }
        } else {
            items.add(new PatternItem(itemType, itemLength));
        }
        this.patternItems = items.toArray(new Object[items.size()]);
        PARSED_PATTERN_CACHE.put(this.pattern, this.patternItems);
        return this.patternItems;
    }

    @Deprecated
    protected void zeroPaddingNumber(NumberFormat nf, StringBuffer buf, int value, int minDigits, int maxDigits) {
        if (this.useLocalZeroPaddingNumberFormat && value >= 0) {
            fastZeroPaddingNumber(buf, value, minDigits, maxDigits);
            return;
        }
        nf.setMinimumIntegerDigits(minDigits);
        nf.setMaximumIntegerDigits(maxDigits);
        nf.format(value, buf, new FieldPosition(-1));
    }

    @Override
    public void setNumberFormat(NumberFormat newNumberFormat) {
        super.setNumberFormat(newNumberFormat);
        initLocalZeroPaddingNumberFormat();
        initializeTimeZoneFormat(true);
        if (this.numberFormatters != null) {
            this.numberFormatters = null;
        }
        if (this.overrideMap == null) {
            return;
        }
        this.overrideMap = null;
    }

    private void initLocalZeroPaddingNumberFormat() {
        if (this.numberFormat instanceof DecimalFormat) {
            this.decDigits = ((DecimalFormat) this.numberFormat).getDecimalFormatSymbols().getDigits();
            this.useLocalZeroPaddingNumberFormat = true;
        } else if (this.numberFormat instanceof DateNumberFormat) {
            this.decDigits = ((DateNumberFormat) this.numberFormat).getDigits();
            this.useLocalZeroPaddingNumberFormat = true;
        } else {
            this.useLocalZeroPaddingNumberFormat = false;
        }
        if (!this.useLocalZeroPaddingNumberFormat) {
            return;
        }
        this.decimalBuf = new char[10];
    }

    private void fastZeroPaddingNumber(StringBuffer buf, int value, int minDigits, int maxDigits) {
        int limit = this.decimalBuf.length < maxDigits ? this.decimalBuf.length : maxDigits;
        int index = limit - 1;
        while (true) {
            this.decimalBuf[index] = this.decDigits[value % 10];
            value /= 10;
            if (index == 0 || value == 0) {
                break;
            } else {
                index--;
            }
        }
        int padding = minDigits - (limit - index);
        while (padding > 0 && index > 0) {
            index--;
            this.decimalBuf[index] = this.decDigits[0];
            padding--;
        }
        while (padding > 0) {
            buf.append(this.decDigits[0]);
            padding--;
        }
        buf.append(this.decimalBuf, index, limit - index);
    }

    protected String zeroPaddingNumber(long value, int minDigits, int maxDigits) {
        this.numberFormat.setMinimumIntegerDigits(minDigits);
        this.numberFormat.setMaximumIntegerDigits(maxDigits);
        return this.numberFormat.format(value);
    }

    private static final boolean isNumeric(char formatChar, int count) {
        if (NUMERIC_FORMAT_CHARS.indexOf(formatChar) < 0) {
            return count <= 2 && NUMERIC_FORMAT_CHARS2.indexOf(formatChar) >= 0;
        }
        return true;
    }

    @Override
    public void parse(String text, Calendar cal, ParsePosition parsePos) {
        TimeZoneTransition beforeTrs;
        TimeZoneTransition afterTrs;
        TimeZone backupTZ = null;
        Calendar resultCal = null;
        if (cal != this.calendar && !cal.getType().equals(this.calendar.getType())) {
            this.calendar.setTimeInMillis(cal.getTimeInMillis());
            backupTZ = this.calendar.getTimeZone();
            this.calendar.setTimeZone(cal.getTimeZone());
            resultCal = cal;
            cal = this.calendar;
        }
        int pos = parsePos.getIndex();
        if (pos < 0) {
            parsePos.setErrorIndex(0);
            return;
        }
        Output<TimeZoneFormat.TimeType> tzTimeType = new Output<>(TimeZoneFormat.TimeType.UNKNOWN);
        boolean[] ambiguousYear = {false};
        int numericFieldStart = -1;
        int numericFieldLength = 0;
        int numericStartPos = 0;
        MessageFormat messageFormat = null;
        if (this.formatData.leapMonthPatterns != null && this.formatData.leapMonthPatterns.length >= 7) {
            messageFormat = new MessageFormat(this.formatData.leapMonthPatterns[6], this.locale);
        }
        Object[] items = getPatternItems();
        int i = 0;
        while (i < items.length) {
            if (items[i] instanceof PatternItem) {
                PatternItem field = (PatternItem) items[i];
                if (field.isNumeric && numericFieldStart == -1 && i + 1 < items.length && (items[i + 1] instanceof PatternItem) && ((PatternItem) items[i + 1]).isNumeric) {
                    numericFieldStart = i;
                    numericFieldLength = field.length;
                    numericStartPos = pos;
                }
                if (numericFieldStart != -1) {
                    int len = field.length;
                    if (numericFieldStart == i) {
                        len = numericFieldLength;
                    }
                    pos = subParse(text, pos, field.type, len, true, false, ambiguousYear, cal, messageFormat, tzTimeType);
                    if (pos < 0) {
                        numericFieldLength--;
                        if (numericFieldLength == 0) {
                            parsePos.setIndex(pos);
                            parsePos.setErrorIndex(pos);
                            if (backupTZ != null) {
                                this.calendar.setTimeZone(backupTZ);
                                return;
                            }
                            return;
                        }
                        i = numericFieldStart;
                        pos = numericStartPos;
                    }
                } else if (field.type != 'l') {
                    numericFieldStart = -1;
                    int s = pos;
                    pos = subParse(text, pos, field.type, field.length, false, true, ambiguousYear, cal, messageFormat, tzTimeType);
                    if (pos < 0) {
                        if (pos == ISOSpecialEra) {
                            pos = s;
                            if (i + 1 < items.length) {
                                try {
                                    String patl = (String) items[i + 1];
                                    if (patl == null) {
                                        patl = (String) items[i + 1];
                                    }
                                    int plen = patl.length();
                                    int idx = 0;
                                    while (idx < plen) {
                                        char pch = patl.charAt(idx);
                                        if (!PatternProps.isWhiteSpace(pch)) {
                                            break;
                                        } else {
                                            idx++;
                                        }
                                    }
                                    if (idx == plen) {
                                        i++;
                                    }
                                } catch (ClassCastException e) {
                                    parsePos.setIndex(pos);
                                    parsePos.setErrorIndex(s);
                                    if (backupTZ != null) {
                                        this.calendar.setTimeZone(backupTZ);
                                        return;
                                    }
                                    return;
                                }
                            }
                        } else {
                            parsePos.setIndex(pos);
                            parsePos.setErrorIndex(s);
                            if (backupTZ != null) {
                                this.calendar.setTimeZone(backupTZ);
                                return;
                            }
                            return;
                        }
                    }
                }
            } else {
                numericFieldStart = -1;
                boolean[] complete = new boolean[1];
                pos = matchLiteral(text, pos, items, i, complete);
                if (!complete[0]) {
                    parsePos.setIndex(pos);
                    parsePos.setErrorIndex(pos);
                    if (backupTZ != null) {
                        this.calendar.setTimeZone(backupTZ);
                        return;
                    }
                    return;
                }
            }
            i++;
        }
        if (pos < text.length()) {
            char extra = text.charAt(pos);
            if (extra == '.' && getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_ALLOW_WHITESPACE) && items.length != 0) {
                Object lastItem = items[items.length - 1];
                if ((lastItem instanceof PatternItem) && !((PatternItem) lastItem).isNumeric) {
                    pos++;
                }
            }
        }
        parsePos.setIndex(pos);
        try {
            TimeZoneFormat.TimeType tztype = tzTimeType.value;
            if (ambiguousYear[0] || tztype != TimeZoneFormat.TimeType.UNKNOWN) {
                if (ambiguousYear[0]) {
                    Date parsedDate = ((Calendar) cal.clone()).getTime();
                    if (parsedDate.before(getDefaultCenturyStart())) {
                        cal.set(1, getDefaultCenturyStartYear() + 100);
                    }
                }
                if (tztype != TimeZoneFormat.TimeType.UNKNOWN) {
                    Calendar copy = (Calendar) cal.clone();
                    TimeZone tz = copy.getTimeZone();
                    BasicTimeZone btz = null;
                    if (tz instanceof BasicTimeZone) {
                        btz = (BasicTimeZone) tz;
                    }
                    copy.set(15, 0);
                    copy.set(16, 0);
                    long localMillis = copy.getTimeInMillis();
                    int[] offsets = new int[2];
                    if (btz != null) {
                        if (tztype == TimeZoneFormat.TimeType.STANDARD) {
                            btz.getOffsetFromLocal(localMillis, 1, 1, offsets);
                        } else {
                            btz.getOffsetFromLocal(localMillis, 3, 3, offsets);
                        }
                    } else {
                        tz.getOffset(localMillis, true, offsets);
                        if ((tztype == TimeZoneFormat.TimeType.STANDARD && offsets[1] != 0) || (tztype == TimeZoneFormat.TimeType.DAYLIGHT && offsets[1] == 0)) {
                            tz.getOffset(localMillis - 86400000, true, offsets);
                        }
                    }
                    int resolvedSavings = offsets[1];
                    if (tztype == TimeZoneFormat.TimeType.STANDARD) {
                        if (offsets[1] != 0) {
                            resolvedSavings = 0;
                        }
                    } else if (offsets[1] == 0) {
                        if (btz != null) {
                            long time = localMillis + ((long) offsets[0]);
                            long beforeT = time;
                            long afterT = time;
                            int beforeSav = 0;
                            int afterSav = 0;
                            do {
                                beforeTrs = btz.getPreviousTransition(beforeT, true);
                                if (beforeTrs == null) {
                                    break;
                                }
                                beforeT = beforeTrs.getTime() - 1;
                                beforeSav = beforeTrs.getFrom().getDSTSavings();
                            } while (beforeSav == 0);
                            do {
                                afterTrs = btz.getNextTransition(afterT, false);
                                if (afterTrs == null) {
                                    break;
                                }
                                afterT = afterTrs.getTime();
                                afterSav = afterTrs.getTo().getDSTSavings();
                            } while (afterSav == 0);
                            if (beforeTrs == null || afterTrs == null) {
                                if (beforeTrs != null && beforeSav != 0) {
                                    resolvedSavings = beforeSav;
                                } else if (afterTrs != null && afterSav != 0) {
                                    resolvedSavings = afterSav;
                                } else {
                                    resolvedSavings = btz.getDSTSavings();
                                }
                            } else if (time - beforeT > afterT - time) {
                                resolvedSavings = afterSav;
                            } else {
                                resolvedSavings = beforeSav;
                            }
                        } else {
                            resolvedSavings = tz.getDSTSavings();
                        }
                        if (resolvedSavings == 0) {
                            resolvedSavings = 3600000;
                        }
                    }
                    cal.set(15, offsets[0]);
                    cal.set(16, resolvedSavings);
                }
            }
            if (resultCal != null) {
                resultCal.setTimeZone(cal.getTimeZone());
                resultCal.setTimeInMillis(cal.getTimeInMillis());
            }
            if (backupTZ == null) {
                return;
            }
            this.calendar.setTimeZone(backupTZ);
        } catch (IllegalArgumentException e2) {
            parsePos.setErrorIndex(pos);
            parsePos.setIndex(pos);
            if (backupTZ != null) {
                this.calendar.setTimeZone(backupTZ);
            }
        }
    }

    private int matchLiteral(String text, int pos, Object[] items, int itemIndex, boolean[] complete) {
        String patternLiteral = (String) items[itemIndex];
        int plen = patternLiteral.length();
        int tlen = text.length();
        int idx = 0;
        while (idx < plen && pos < tlen) {
            char pch = patternLiteral.charAt(idx);
            char ich = text.charAt(pos);
            if (PatternProps.isWhiteSpace(pch) && PatternProps.isWhiteSpace(ich)) {
                while (idx + 1 < plen && PatternProps.isWhiteSpace(patternLiteral.charAt(idx + 1))) {
                    idx++;
                }
                while (pos + 1 < tlen && PatternProps.isWhiteSpace(text.charAt(pos + 1))) {
                    pos++;
                }
            } else if (pch != ich) {
                if (ich == '.' && pos == pos && itemIndex > 0 && getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_ALLOW_WHITESPACE)) {
                    Object before = items[itemIndex - 1];
                    if (!(before instanceof PatternItem)) {
                        break;
                    }
                    boolean isNumeric = ((PatternItem) before).isNumeric;
                    if (isNumeric) {
                        break;
                    }
                    pos++;
                } else if ((pch != ' ' && pch != '.') || !getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_ALLOW_WHITESPACE)) {
                    if (pos == pos || !getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_PARTIAL_LITERAL_MATCH)) {
                        break;
                    }
                    idx++;
                } else {
                    idx++;
                }
            }
            idx++;
            pos++;
        }
        complete[0] = idx == plen;
        if (!complete[0] && getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_ALLOW_WHITESPACE) && itemIndex > 0 && itemIndex < items.length - 1 && pos < tlen) {
            Object before2 = items[itemIndex - 1];
            Object after = items[itemIndex + 1];
            if ((before2 instanceof PatternItem) && (after instanceof PatternItem)) {
                char beforeType = ((PatternItem) before2).type;
                char afterType = ((PatternItem) after).type;
                if (DATE_PATTERN_TYPE.contains(beforeType) != DATE_PATTERN_TYPE.contains(afterType)) {
                    int newPos = pos;
                    while (PatternProps.isWhiteSpace(text.charAt(newPos))) {
                        newPos++;
                    }
                    complete[0] = newPos > pos;
                    return newPos;
                }
                return pos;
            }
            return pos;
        }
        return pos;
    }

    protected int matchString(String text, int start, int field, String[] data, Calendar cal) {
        return matchString(text, start, field, data, null, cal);
    }

    @Deprecated
    private int matchString(String text, int start, int field, String[] data, String monthPattern, Calendar cal) {
        String leapMonthName;
        int length;
        int matchLength;
        int matchLength2;
        int count = data.length;
        int bestMatchLength = 0;
        int bestMatch = -1;
        int isLeapMonth = 0;
        for (int i = field == 7 ? 1 : 0; i < count; i++) {
            int length2 = data[i].length();
            if (length2 > bestMatchLength && (matchLength2 = regionMatchesWithOptionalDot(text, start, data[i], length2)) >= 0) {
                bestMatch = i;
                bestMatchLength = matchLength2;
                isLeapMonth = 0;
            }
            if (monthPattern != null && (length = (leapMonthName = MessageFormat.format(monthPattern, data[i])).length()) > bestMatchLength && (matchLength = regionMatchesWithOptionalDot(text, start, leapMonthName, length)) >= 0) {
                bestMatch = i;
                bestMatchLength = matchLength;
                isLeapMonth = 1;
            }
        }
        if (bestMatch >= 0) {
            if (field >= 0) {
                if (field == 1) {
                    bestMatch++;
                }
                cal.set(field, bestMatch);
                if (monthPattern != null) {
                    cal.set(22, isLeapMonth);
                }
            }
            return start + bestMatchLength;
        }
        return ~start;
    }

    private int regionMatchesWithOptionalDot(String text, int start, String data, int length) {
        boolean matches = text.regionMatches(true, start, data, 0, length);
        if (matches) {
            return length;
        }
        if (data.length() > 0 && data.charAt(data.length() - 1) == '.' && text.regionMatches(true, start, data, 0, length - 1)) {
            return length - 1;
        }
        return -1;
    }

    protected int matchQuarterString(String text, int start, int field, String[] data, Calendar cal) {
        int matchLength;
        int count = data.length;
        int bestMatchLength = 0;
        int bestMatch = -1;
        for (int i = 0; i < count; i++) {
            int length = data[i].length();
            if (length > bestMatchLength && (matchLength = regionMatchesWithOptionalDot(text, start, data[i], length)) >= 0) {
                bestMatch = i;
                bestMatchLength = matchLength;
            }
        }
        if (bestMatch >= 0) {
            cal.set(field, bestMatch * 3);
            return start + bestMatchLength;
        }
        return -start;
    }

    protected int subParse(String text, int start, char ch, int count, boolean obeyCount, boolean allowNegative, boolean[] ambiguousYear, Calendar cal) {
        return subParse(text, start, ch, count, obeyCount, allowNegative, ambiguousYear, cal, null, null);
    }

    @Deprecated
    private int subParse(String text, int start, char ch, int count, boolean obeyCount, boolean allowNegative, boolean[] ambiguousYear, Calendar cal, MessageFormat numericLeapMonthFormatter, Output<TimeZoneFormat.TimeType> tzTimeType) {
        TimeZoneFormat.Style style;
        TimeZoneFormat.Style style2;
        TimeZoneFormat.Style style3;
        TimeZoneFormat.Style style4;
        int ps;
        Number number;
        Number number2 = null;
        int value = 0;
        ParsePosition pos = new ParsePosition(0);
        int patternCharIndex = getIndexFromChar(ch);
        if (patternCharIndex == -1) {
            return ~start;
        }
        NumberFormat currentNumberFormat = getNumberFormat(ch);
        int field = PATTERN_INDEX_TO_CALENDAR_FIELD[patternCharIndex];
        if (numericLeapMonthFormatter != null) {
            numericLeapMonthFormatter.setFormatByArgumentIndex(0, currentNumberFormat);
        }
        boolean zEquals = !cal.getType().equals("chinese") ? cal.getType().equals("dangi") : true;
        while (start < text.length()) {
            int c = UTF16.charAt(text, start);
            if (UCharacter.isUWhiteSpace(c) && PatternProps.isWhiteSpace(c)) {
                start += UTF16.getCharCount(c);
            } else {
                pos.setIndex(start);
                if (patternCharIndex == 4 || patternCharIndex == 15 || ((patternCharIndex == 2 && count <= 2) || patternCharIndex == 26 || patternCharIndex == 19 || patternCharIndex == 25 || patternCharIndex == 1 || patternCharIndex == 18 || patternCharIndex == 30 || ((patternCharIndex == 0 && zEquals) || patternCharIndex == 27 || patternCharIndex == 28 || patternCharIndex == 8))) {
                    boolean parsedNumericLeapMonth = false;
                    if (numericLeapMonthFormatter != null && (patternCharIndex == 2 || patternCharIndex == 26)) {
                        Object[] args = numericLeapMonthFormatter.parse(text, pos);
                        if (args != null && pos.getIndex() > start && (args[0] instanceof Number)) {
                            parsedNumericLeapMonth = true;
                            number2 = (Number) args[0];
                            cal.set(22, 1);
                        } else {
                            pos.setIndex(start);
                            cal.set(22, 0);
                        }
                    }
                    if (!parsedNumericLeapMonth) {
                        if (obeyCount) {
                            if (start + count > text.length()) {
                                return ~start;
                            }
                            number2 = parseInt(text, count, pos, allowNegative, currentNumberFormat);
                        } else {
                            number2 = parseInt(text, pos, allowNegative, currentNumberFormat);
                        }
                        if (number2 == null && !allowNumericFallback(patternCharIndex)) {
                            return ~start;
                        }
                    }
                    if (number2 != null) {
                        value = number2.intValue();
                    }
                }
                switch (patternCharIndex) {
                    case 0:
                        if (zEquals) {
                            cal.set(0, value);
                            return pos.getIndex();
                        }
                        if (count == 5) {
                            ps = matchString(text, start, 0, this.formatData.narrowEras, null, cal);
                        } else if (count == 4) {
                            ps = matchString(text, start, 0, this.formatData.eraNames, null, cal);
                        } else {
                            ps = matchString(text, start, 0, this.formatData.eras, null, cal);
                        }
                        if (ps == (~start)) {
                            return ISOSpecialEra;
                        }
                        return ps;
                    case 1:
                    case 18:
                        if (this.override != null && ((this.override.compareTo("hebr") == 0 || this.override.indexOf("y=hebr") >= 0) && value < 1000)) {
                            value += HEBREW_CAL_CUR_MILLENIUM_START_YEAR;
                        } else if (count == 2 && pos.getIndex() - start == 2 && cal.haveDefaultCentury() && UCharacter.isDigit(text.charAt(start)) && UCharacter.isDigit(text.charAt(start + 1))) {
                            int ambiguousTwoDigitYear = getDefaultCenturyStartYear() % 100;
                            ambiguousYear[0] = value == ambiguousTwoDigitYear;
                            value += (value < ambiguousTwoDigitYear ? 100 : 0) + ((getDefaultCenturyStartYear() / 100) * 100);
                        }
                        cal.set(field, value);
                        if (DelayedHebrewMonthCheck) {
                            if (!HebrewCalendar.isLeapYear(value)) {
                                cal.add(2, 1);
                            }
                            DelayedHebrewMonthCheck = false;
                        }
                        return pos.getIndex();
                    case 2:
                    case 26:
                        if (count <= 2 || (number2 != null && getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_ALLOW_NUMERIC))) {
                            cal.set(2, value - 1);
                            if (cal.getType().equals("hebrew") && value >= 6) {
                                if (cal.isSet(1)) {
                                    if (!HebrewCalendar.isLeapYear(cal.get(1))) {
                                        cal.set(2, value);
                                    }
                                } else {
                                    DelayedHebrewMonthCheck = true;
                                }
                            }
                            return pos.getIndex();
                        }
                        boolean haveMonthPat = this.formatData.leapMonthPatterns != null && this.formatData.leapMonthPatterns.length >= 7;
                        int newStart = 0;
                        if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || count == 4) {
                            if (patternCharIndex == 2) {
                                newStart = matchString(text, start, 2, this.formatData.months, haveMonthPat ? this.formatData.leapMonthPatterns[0] : null, cal);
                            } else {
                                newStart = matchString(text, start, 2, this.formatData.standaloneMonths, haveMonthPat ? this.formatData.leapMonthPatterns[3] : null, cal);
                            }
                            if (newStart > 0) {
                                return newStart;
                            }
                        }
                        if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || count == 3) {
                            if (patternCharIndex == 2) {
                                return matchString(text, start, 2, this.formatData.shortMonths, haveMonthPat ? this.formatData.leapMonthPatterns[1] : null, cal);
                            }
                            return matchString(text, start, 2, this.formatData.standaloneShortMonths, haveMonthPat ? this.formatData.leapMonthPatterns[4] : null, cal);
                        }
                        return newStart;
                    case 3:
                    case 5:
                    case 6:
                    case 7:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 16:
                    case 20:
                    case 21:
                    case 22:
                    case 34:
                    default:
                        if (obeyCount) {
                            if (start + count > text.length()) {
                                return -start;
                            }
                            number = parseInt(text, count, pos, allowNegative, currentNumberFormat);
                        } else {
                            number = parseInt(text, pos, allowNegative, currentNumberFormat);
                        }
                        if (number != null) {
                            if (patternCharIndex != 34) {
                                cal.set(field, number.intValue());
                            } else {
                                cal.setRelatedYear(number.intValue());
                            }
                            return pos.getIndex();
                        }
                        return ~start;
                    case 4:
                        if (value == cal.getMaximum(11) + 1) {
                            value = 0;
                        }
                        cal.set(11, value);
                        return pos.getIndex();
                    case 8:
                        int i = pos.getIndex() - start;
                        if (i < 3) {
                            while (i < 3) {
                                value *= 10;
                                i++;
                            }
                        } else {
                            int a = 1;
                            while (i > 3) {
                                a *= 10;
                                i--;
                            }
                            value /= a;
                        }
                        cal.set(14, value);
                        return pos.getIndex();
                    case 9:
                        break;
                    case 14:
                        if (this.formatData.ampmsNarrow == null || count < 5 || getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH)) {
                            int newStart2 = matchString(text, start, 9, this.formatData.ampms, null, cal);
                            if (newStart2 > 0) {
                                return newStart2;
                            }
                        }
                        if (this.formatData.ampmsNarrow != null && (count >= 5 || getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH))) {
                            int newStart3 = matchString(text, start, 9, this.formatData.ampmsNarrow, null, cal);
                            if (newStart3 > 0) {
                                return newStart3;
                            }
                        }
                        return ~start;
                    case 15:
                        if (value == cal.getLeastMaximum(10) + 1) {
                            value = 0;
                        }
                        cal.set(10, value);
                        return pos.getIndex();
                    case 17:
                        TimeZoneFormat.Style style5 = count < 4 ? TimeZoneFormat.Style.SPECIFIC_SHORT : TimeZoneFormat.Style.SPECIFIC_LONG;
                        TimeZone tz = tzFormat().parse(style5, text, pos, tzTimeType);
                        if (tz != null) {
                            cal.setTimeZone(tz);
                            return pos.getIndex();
                        }
                        return ~start;
                    case 19:
                        if (count <= 2 || (number2 != null && getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_ALLOW_NUMERIC))) {
                            cal.set(field, value);
                            return pos.getIndex();
                        }
                        break;
                    case 23:
                        if (count < 4) {
                            style4 = TimeZoneFormat.Style.ISO_BASIC_LOCAL_FULL;
                        } else {
                            style4 = count == 5 ? TimeZoneFormat.Style.ISO_EXTENDED_FULL : TimeZoneFormat.Style.LOCALIZED_GMT;
                        }
                        TimeZone tz2 = tzFormat().parse(style4, text, pos, tzTimeType);
                        if (tz2 != null) {
                            cal.setTimeZone(tz2);
                            return pos.getIndex();
                        }
                        return ~start;
                    case 24:
                        TimeZoneFormat.Style style6 = count < 4 ? TimeZoneFormat.Style.GENERIC_SHORT : TimeZoneFormat.Style.GENERIC_LONG;
                        TimeZone tz3 = tzFormat().parse(style6, text, pos, tzTimeType);
                        if (tz3 != null) {
                            cal.setTimeZone(tz3);
                            return pos.getIndex();
                        }
                        return ~start;
                    case 25:
                        if (count == 1 || (number2 != null && getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_ALLOW_NUMERIC))) {
                            cal.set(field, value);
                            return pos.getIndex();
                        }
                        int newStart4 = 0;
                        if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || count == 4) {
                            newStart4 = matchString(text, start, 7, this.formatData.standaloneWeekdays, null, cal);
                            if (newStart4 > 0) {
                                return newStart4;
                            }
                        }
                        if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || count == 3) {
                            newStart4 = matchString(text, start, 7, this.formatData.standaloneShortWeekdays, null, cal);
                            if (newStart4 > 0) {
                                return newStart4;
                            }
                        }
                        if ((getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || count == 6) && this.formatData.standaloneShorterWeekdays != null) {
                            return matchString(text, start, 7, this.formatData.standaloneShorterWeekdays, null, cal);
                        }
                        return newStart4;
                    case 27:
                        if (count <= 2 || (number2 != null && getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_ALLOW_NUMERIC))) {
                            cal.set(2, (value - 1) * 3);
                            return pos.getIndex();
                        }
                        int newStart5 = 0;
                        if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || count == 4) {
                            newStart5 = matchQuarterString(text, start, 2, this.formatData.quarters, cal);
                            if (newStart5 > 0) {
                                return newStart5;
                            }
                        }
                        if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || count == 3) {
                            return matchQuarterString(text, start, 2, this.formatData.shortQuarters, cal);
                        }
                        return newStart5;
                    case 28:
                        if (count <= 2 || (number2 != null && getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_ALLOW_NUMERIC))) {
                            cal.set(2, (value - 1) * 3);
                            return pos.getIndex();
                        }
                        int newStart6 = 0;
                        if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || count == 4) {
                            newStart6 = matchQuarterString(text, start, 2, this.formatData.standaloneQuarters, cal);
                            if (newStart6 > 0) {
                                return newStart6;
                            }
                        }
                        if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || count == 3) {
                            return matchQuarterString(text, start, 2, this.formatData.standaloneShortQuarters, cal);
                        }
                        return newStart6;
                    case 29:
                        switch (count) {
                            case 1:
                                style3 = TimeZoneFormat.Style.ZONE_ID_SHORT;
                                break;
                            case 2:
                                style3 = TimeZoneFormat.Style.ZONE_ID;
                                break;
                            case 3:
                                style3 = TimeZoneFormat.Style.EXEMPLAR_LOCATION;
                                break;
                            default:
                                style3 = TimeZoneFormat.Style.GENERIC_LOCATION;
                                break;
                        }
                        TimeZone tz4 = tzFormat().parse(style3, text, pos, tzTimeType);
                        if (tz4 != null) {
                            cal.setTimeZone(tz4);
                            return pos.getIndex();
                        }
                        return ~start;
                    case 30:
                        if (this.formatData.shortYearNames != null) {
                            int newStart7 = matchString(text, start, 1, this.formatData.shortYearNames, null, cal);
                            if (newStart7 > 0) {
                                return newStart7;
                            }
                        }
                        if (number2 != null && (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_ALLOW_NUMERIC) || this.formatData.shortYearNames == null || value > this.formatData.shortYearNames.length)) {
                            cal.set(1, value);
                            return pos.getIndex();
                        }
                        return ~start;
                    case 31:
                        TimeZoneFormat.Style style7 = count < 4 ? TimeZoneFormat.Style.LOCALIZED_GMT_SHORT : TimeZoneFormat.Style.LOCALIZED_GMT;
                        TimeZone tz5 = tzFormat().parse(style7, text, pos, tzTimeType);
                        if (tz5 != null) {
                            cal.setTimeZone(tz5);
                            return pos.getIndex();
                        }
                        return ~start;
                    case 32:
                        switch (count) {
                            case 1:
                                style2 = TimeZoneFormat.Style.ISO_BASIC_SHORT;
                                break;
                            case 2:
                                style2 = TimeZoneFormat.Style.ISO_BASIC_FIXED;
                                break;
                            case 3:
                                style2 = TimeZoneFormat.Style.ISO_EXTENDED_FIXED;
                                break;
                            case 4:
                                style2 = TimeZoneFormat.Style.ISO_BASIC_FULL;
                                break;
                            default:
                                style2 = TimeZoneFormat.Style.ISO_EXTENDED_FULL;
                                break;
                        }
                        TimeZone tz6 = tzFormat().parse(style2, text, pos, tzTimeType);
                        if (tz6 != null) {
                            cal.setTimeZone(tz6);
                            return pos.getIndex();
                        }
                        return ~start;
                    case 33:
                        switch (count) {
                            case 1:
                                style = TimeZoneFormat.Style.ISO_BASIC_LOCAL_SHORT;
                                break;
                            case 2:
                                style = TimeZoneFormat.Style.ISO_BASIC_LOCAL_FIXED;
                                break;
                            case 3:
                                style = TimeZoneFormat.Style.ISO_EXTENDED_LOCAL_FIXED;
                                break;
                            case 4:
                                style = TimeZoneFormat.Style.ISO_BASIC_LOCAL_FULL;
                                break;
                            default:
                                style = TimeZoneFormat.Style.ISO_EXTENDED_LOCAL_FULL;
                                break;
                        }
                        TimeZone tz7 = tzFormat().parse(style, text, pos, tzTimeType);
                        if (tz7 != null) {
                            cal.setTimeZone(tz7);
                            return pos.getIndex();
                        }
                        return ~start;
                    case 35:
                        ArrayList<String> data = new ArrayList<>(3);
                        data.add(this.formatData.getTimeSeparatorString());
                        if (!this.formatData.getTimeSeparatorString().equals(":")) {
                            data.add(":");
                        }
                        if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_PARTIAL_LITERAL_MATCH) && !this.formatData.getTimeSeparatorString().equals(".")) {
                            data.add(".");
                        }
                        return matchString(text, start, -1, (String[]) data.toArray(new String[0]), cal);
                }
                int newStart8 = 0;
                if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || count == 4) {
                    newStart8 = matchString(text, start, 7, this.formatData.weekdays, null, cal);
                    if (newStart8 > 0) {
                        return newStart8;
                    }
                }
                if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || count == 3) {
                    newStart8 = matchString(text, start, 7, this.formatData.shortWeekdays, null, cal);
                    if (newStart8 > 0) {
                        return newStart8;
                    }
                }
                if ((getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || count == 6) && this.formatData.shorterWeekdays != null) {
                    newStart8 = matchString(text, start, 7, this.formatData.shorterWeekdays, null, cal);
                    if (newStart8 > 0) {
                        return newStart8;
                    }
                }
                if ((getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || count == 5) && this.formatData.narrowWeekdays != null) {
                    newStart8 = matchString(text, start, 7, this.formatData.narrowWeekdays, null, cal);
                    if (newStart8 > 0) {
                        return newStart8;
                    }
                }
                return newStart8;
            }
        }
        return ~start;
    }

    private boolean allowNumericFallback(int patternCharIndex) {
        if (patternCharIndex == 26 || patternCharIndex == 19 || patternCharIndex == 25 || patternCharIndex == 30 || patternCharIndex == 27 || patternCharIndex == 28) {
            return true;
        }
        return false;
    }

    private Number parseInt(String text, ParsePosition pos, boolean allowNegative, NumberFormat fmt) {
        return parseInt(text, -1, pos, allowNegative, fmt);
    }

    private Number parseInt(String text, int maxDigits, ParsePosition pos, boolean allowNegative, NumberFormat fmt) {
        Number number;
        int nDigits;
        int oldPos = pos.getIndex();
        if (allowNegative) {
            number = fmt.parse(text, pos);
        } else if (fmt instanceof DecimalFormat) {
            String oldPrefix = ((DecimalFormat) fmt).getNegativePrefix();
            ((DecimalFormat) fmt).setNegativePrefix(SUPPRESS_NEGATIVE_PREFIX);
            number = fmt.parse(text, pos);
            ((DecimalFormat) fmt).setNegativePrefix(oldPrefix);
        } else {
            boolean dateNumberFormat = fmt instanceof DateNumberFormat;
            if (dateNumberFormat) {
                ((DateNumberFormat) fmt).setParsePositiveOnly(true);
            }
            number = fmt.parse(text, pos);
            if (dateNumberFormat) {
                ((DateNumberFormat) fmt).setParsePositiveOnly(false);
            }
        }
        if (maxDigits > 0 && (nDigits = pos.getIndex() - oldPos) > maxDigits) {
            double val = number.doubleValue();
            for (int nDigits2 = nDigits - maxDigits; nDigits2 > 0; nDigits2--) {
                val /= 10.0d;
            }
            pos.setIndex(oldPos + maxDigits);
            return Integer.valueOf((int) val);
        }
        return number;
    }

    private String translatePattern(String pat, String from, String to) {
        int ci;
        StringBuilder result = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < pat.length(); i++) {
            char c = pat.charAt(i);
            if (inQuote) {
                if (c == '\'') {
                    inQuote = false;
                }
            } else if (c == '\'') {
                inQuote = true;
            } else if (isSyntaxChar(c) && (ci = from.indexOf(c)) != -1) {
                c = to.charAt(ci);
            }
            result.append(c);
        }
        if (inQuote) {
            throw new IllegalArgumentException("Unfinished quote in pattern");
        }
        return result.toString();
    }

    public String toPattern() {
        return this.pattern;
    }

    public String toLocalizedPattern() {
        return translatePattern(this.pattern, "GyMdkHmsSEDFwWahKzYeugAZvcLQqVUOXxr", this.formatData.localPatternChars);
    }

    public void applyPattern(String pat) {
        this.pattern = pat;
        setLocale(null, null);
        this.patternItems = null;
    }

    public void applyLocalizedPattern(String pat) {
        this.pattern = translatePattern(pat, this.formatData.localPatternChars, "GyMdkHmsSEDFwWahKzYeugAZvcLQqVUOXxr");
        setLocale(null, null);
    }

    public DateFormatSymbols getDateFormatSymbols() {
        return (DateFormatSymbols) this.formatData.clone();
    }

    public void setDateFormatSymbols(DateFormatSymbols newFormatSymbols) {
        this.formatData = (DateFormatSymbols) newFormatSymbols.clone();
    }

    protected DateFormatSymbols getSymbols() {
        return this.formatData;
    }

    public TimeZoneFormat getTimeZoneFormat() {
        return tzFormat().freeze();
    }

    public void setTimeZoneFormat(TimeZoneFormat tzfmt) {
        if (tzfmt.isFrozen()) {
            this.tzFormat = tzfmt;
        } else {
            this.tzFormat = tzfmt.cloneAsThawed().freeze();
        }
    }

    @Override
    public Object clone() {
        SimpleDateFormat other = (SimpleDateFormat) super.clone();
        other.formatData = (DateFormatSymbols) this.formatData.clone();
        if (this.decimalBuf != null) {
            other.decimalBuf = new char[10];
        }
        return other;
    }

    @Override
    public int hashCode() {
        return this.pattern.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        SimpleDateFormat that = (SimpleDateFormat) obj;
        if (this.pattern.equals(that.pattern)) {
            return this.formatData.equals(that.formatData);
        }
        return false;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        if (this.defaultCenturyStart == null) {
            initializeDefaultCenturyStart(this.defaultCenturyBase);
        }
        initializeTimeZoneFormat(false);
        stream.defaultWriteObject();
        stream.writeInt(getContext(DisplayContext.Type.CAPITALIZATION).value());
    }

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        stream.defaultReadObject();
        int capitalizationSettingValue = this.serialVersionOnStream > 1 ? stream.readInt() : -1;
        if (this.serialVersionOnStream < 1) {
            this.defaultCenturyBase = System.currentTimeMillis();
        } else {
            parseAmbiguousDatesAsAfter(this.defaultCenturyStart);
        }
        this.serialVersionOnStream = 2;
        this.locale = getLocale(ULocale.VALID_LOCALE);
        if (this.locale == null) {
            this.locale = ULocale.getDefault(ULocale.Category.FORMAT);
        }
        initLocalZeroPaddingNumberFormat();
        setContext(DisplayContext.CAPITALIZATION_NONE);
        if (capitalizationSettingValue >= 0) {
            DisplayContext[] displayContextArrValuesCustom = DisplayContext.valuesCustom();
            int length = displayContextArrValuesCustom.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                DisplayContext context = displayContextArrValuesCustom[i];
                if (context.value() != capitalizationSettingValue) {
                    i++;
                } else {
                    setContext(context);
                    break;
                }
            }
        }
        if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_PARTIAL_MATCH)) {
            return;
        }
        setBooleanAttribute(DateFormat.BooleanAttribute.PARSE_PARTIAL_LITERAL_MATCH, false);
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        Calendar cal = this.calendar;
        if (obj instanceof Calendar) {
            cal = (Calendar) obj;
        } else if (obj instanceof Date) {
            this.calendar.setTime((Date) obj);
        } else if (obj instanceof Number) {
            this.calendar.setTimeInMillis(((Number) obj).longValue());
        } else {
            throw new IllegalArgumentException("Cannot format given Object as a Date");
        }
        StringBuffer toAppendTo = new StringBuffer();
        FieldPosition pos = new FieldPosition(0);
        List<FieldPosition> attributes = new ArrayList<>();
        format(cal, getContext(DisplayContext.Type.CAPITALIZATION), toAppendTo, pos, attributes);
        AttributedString as = new AttributedString(toAppendTo.toString());
        for (int i = 0; i < attributes.size(); i++) {
            FieldPosition fp = attributes.get(i);
            Format.Field attribute = fp.getFieldAttribute();
            as.addAttribute(attribute, attribute, fp.getBeginIndex(), fp.getEndIndex());
        }
        return as.getIterator();
    }

    ULocale getLocale() {
        return this.locale;
    }

    boolean isFieldUnitIgnored(int field) {
        return isFieldUnitIgnored(this.pattern, field);
    }

    static boolean isFieldUnitIgnored(String pattern, int field) {
        int fieldLevel = CALENDAR_FIELD_TO_LEVEL[field];
        boolean inQuote = false;
        char prevCh = 0;
        int count = 0;
        int i = 0;
        while (i < pattern.length()) {
            char ch = pattern.charAt(i);
            if (ch != prevCh && count > 0) {
                int level = getLevelFromChar(prevCh);
                if (fieldLevel <= level) {
                    return false;
                }
                count = 0;
            }
            if (ch == '\'') {
                if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '\'') {
                    i++;
                } else {
                    inQuote = !inQuote;
                }
            } else if (!inQuote && isSyntaxChar(ch)) {
                prevCh = ch;
                count++;
            }
            i++;
        }
        if (count > 0) {
            int level2 = getLevelFromChar(prevCh);
            return fieldLevel > level2;
        }
        return true;
    }

    @Deprecated
    public final StringBuffer intervalFormatByAlgorithm(Calendar fromCalendar, Calendar toCalendar, StringBuffer appendTo, FieldPosition pos) throws IllegalArgumentException {
        if (!fromCalendar.isEquivalentTo(toCalendar)) {
            throw new IllegalArgumentException("can not format on two different calendars");
        }
        Object[] items = getPatternItems();
        int diffBegin = -1;
        int diffEnd = -1;
        int i = 0;
        while (true) {
            try {
                if (i >= items.length) {
                    break;
                }
                if (diffCalFieldValue(fromCalendar, toCalendar, items, i)) {
                    diffBegin = i;
                    break;
                }
                i++;
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(e.toString());
            }
        }
        if (diffBegin == -1) {
            return format(fromCalendar, appendTo, pos);
        }
        int i2 = items.length - 1;
        while (true) {
            if (i2 < diffBegin) {
                break;
            }
            if (diffCalFieldValue(fromCalendar, toCalendar, items, i2)) {
                diffEnd = i2;
                break;
            }
            i2--;
        }
        if (diffBegin == 0 && diffEnd == items.length - 1) {
            format(fromCalendar, appendTo, pos);
            appendTo.append(" – ");
            format(toCalendar, appendTo, pos);
            return appendTo;
        }
        int highestLevel = 1000;
        for (int i3 = diffBegin; i3 <= diffEnd; i3++) {
            if (!(items[i3] instanceof String)) {
                char ch = ((PatternItem) items[i3]).type;
                int patternCharIndex = getIndexFromChar(ch);
                if (patternCharIndex == -1) {
                    throw new IllegalArgumentException("Illegal pattern character '" + ch + "' in \"" + this.pattern + '\"');
                }
                if (patternCharIndex < highestLevel) {
                    highestLevel = patternCharIndex;
                }
            }
        }
        int i4 = 0;
        while (true) {
            if (i4 >= diffBegin) {
                break;
            }
            try {
                if (lowerLevel(items, i4, highestLevel)) {
                    diffBegin = i4;
                    break;
                }
                i4++;
            } catch (IllegalArgumentException e2) {
                throw new IllegalArgumentException(e2.toString());
            }
        }
        int i5 = items.length - 1;
        while (true) {
            if (i5 <= diffEnd) {
                break;
            }
            if (lowerLevel(items, i5, highestLevel)) {
                diffEnd = i5;
                break;
            }
            i5--;
        }
        if (diffBegin == 0 && diffEnd == items.length - 1) {
            format(fromCalendar, appendTo, pos);
            appendTo.append(" – ");
            format(toCalendar, appendTo, pos);
            return appendTo;
        }
        pos.setBeginIndex(0);
        pos.setEndIndex(0);
        DisplayContext capSetting = getContext(DisplayContext.Type.CAPITALIZATION);
        for (int i6 = 0; i6 <= diffEnd; i6++) {
            if (items[i6] instanceof String) {
                appendTo.append((String) items[i6]);
            } else {
                PatternItem item = (PatternItem) items[i6];
                if (this.useFastFormat) {
                    subFormat(appendTo, item.type, item.length, appendTo.length(), i6, capSetting, pos, fromCalendar);
                } else {
                    appendTo.append(subFormat(item.type, item.length, appendTo.length(), i6, capSetting, pos, fromCalendar));
                }
            }
        }
        appendTo.append(" – ");
        for (int i7 = diffBegin; i7 < items.length; i7++) {
            if (items[i7] instanceof String) {
                appendTo.append((String) items[i7]);
            } else {
                PatternItem item2 = (PatternItem) items[i7];
                if (this.useFastFormat) {
                    subFormat(appendTo, item2.type, item2.length, appendTo.length(), i7, capSetting, pos, toCalendar);
                } else {
                    appendTo.append(subFormat(item2.type, item2.length, appendTo.length(), i7, capSetting, pos, toCalendar));
                }
            }
        }
        return appendTo;
    }

    private boolean diffCalFieldValue(Calendar fromCalendar, Calendar toCalendar, Object[] items, int i) throws IllegalArgumentException {
        if (items[i] instanceof String) {
            return false;
        }
        PatternItem item = (PatternItem) items[i];
        char ch = item.type;
        int patternCharIndex = getIndexFromChar(ch);
        if (patternCharIndex == -1) {
            throw new IllegalArgumentException("Illegal pattern character '" + ch + "' in \"" + this.pattern + '\"');
        }
        int field = PATTERN_INDEX_TO_CALENDAR_FIELD[patternCharIndex];
        if (field >= 0) {
            int value = fromCalendar.get(field);
            int value_2 = toCalendar.get(field);
            if (value != value_2) {
                return true;
            }
        }
        return false;
    }

    private boolean lowerLevel(Object[] items, int i, int level) throws IllegalArgumentException {
        if (items[i] instanceof String) {
            return false;
        }
        PatternItem item = (PatternItem) items[i];
        char ch = item.type;
        int patternCharIndex = getLevelFromChar(ch);
        if (patternCharIndex == -1) {
            throw new IllegalArgumentException("Illegal pattern character '" + ch + "' in \"" + this.pattern + '\"');
        }
        return patternCharIndex >= level;
    }

    public void setNumberFormat(String fields, NumberFormat overrideNF) {
        overrideNF.setGroupingUsed(false);
        String nsName = "$" + UUID.randomUUID().toString();
        if (this.numberFormatters == null) {
            this.numberFormatters = new HashMap<>();
        }
        if (this.overrideMap == null) {
            this.overrideMap = new HashMap<>();
        }
        for (int i = 0; i < fields.length(); i++) {
            char field = fields.charAt(i);
            if ("GyMdkHmsSEDFwWahKzYeugAZvcLQqVUOXxr".indexOf(field) == -1) {
                throw new IllegalArgumentException("Illegal field character '" + field + "' in setNumberFormat.");
            }
            this.overrideMap.put(Character.valueOf(field), nsName);
            this.numberFormatters.put(nsName, overrideNF);
        }
        this.useLocalZeroPaddingNumberFormat = false;
    }

    public NumberFormat getNumberFormat(char field) {
        Character ovrField = Character.valueOf(field);
        if (this.overrideMap != null && this.overrideMap.containsKey(ovrField)) {
            String nsName = this.overrideMap.get(ovrField).toString();
            NumberFormat nf = this.numberFormatters.get(nsName);
            return nf;
        }
        return this.numberFormat;
    }

    private void initNumberFormatters(ULocale loc) {
        this.numberFormatters = new HashMap<>();
        this.overrideMap = new HashMap<>();
        processOverrideString(loc, this.override);
    }

    private void processOverrideString(ULocale loc, String str) {
        int end;
        String nsName;
        boolean fullOverride;
        if (str == null || str.length() == 0) {
            return;
        }
        int start = 0;
        boolean moreToProcess = true;
        while (moreToProcess) {
            int delimiterPosition = str.indexOf(";", start);
            if (delimiterPosition == -1) {
                moreToProcess = false;
                end = str.length();
            } else {
                end = delimiterPosition;
            }
            String currentString = str.substring(start, end);
            int equalSignPosition = currentString.indexOf("=");
            if (equalSignPosition == -1) {
                nsName = currentString;
                fullOverride = true;
            } else {
                nsName = currentString.substring(equalSignPosition + 1);
                Character ovrField = Character.valueOf(currentString.charAt(0));
                this.overrideMap.put(ovrField, nsName);
                fullOverride = false;
            }
            ULocale ovrLoc = new ULocale(loc.getBaseName() + "@numbers=" + nsName);
            NumberFormat nf = NumberFormat.createInstance(ovrLoc, 0);
            nf.setGroupingUsed(false);
            if (fullOverride) {
                setNumberFormat(nf);
            } else {
                this.useLocalZeroPaddingNumberFormat = false;
            }
            if (!fullOverride && !this.numberFormatters.containsKey(nsName)) {
                this.numberFormatters.put(nsName, nf);
            }
            start = delimiterPosition + 1;
        }
    }
}
