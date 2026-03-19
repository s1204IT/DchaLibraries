package android.icu.text;

import android.icu.impl.DateNumberFormat;
import android.icu.impl.DayPeriodRules;
import android.icu.impl.ICUCache;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.PatternProps;
import android.icu.impl.PatternTokenizer;
import android.icu.impl.SimpleCache;
import android.icu.impl.SimpleFormatterImpl;
import android.icu.impl.coll.CollationSettings;
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
import android.icu.util.UResourceBundle;
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
    static final boolean $assertionsDisabled = false;
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
    private transient boolean hasMinute;
    private transient boolean hasSecond;
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
    private static final int[] PATTERN_CHAR_TO_INDEX = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 22, 36, -1, 10, 9, 11, 0, 5, -1, -1, 16, 26, 2, -1, 31, -1, 27, -1, 8, -1, 30, 29, 13, 32, 18, 23, -1, -1, -1, -1, -1, -1, 14, 35, 25, 3, 19, -1, 21, 15, -1, -1, 4, -1, 6, -1, -1, -1, 28, 34, 7, -1, 20, 24, 12, 33, 1, 17, -1, -1, -1, -1, -1};
    private static final int[] PATTERN_INDEX_TO_CALENDAR_FIELD = {0, 1, 2, 5, 11, 11, 12, 13, 14, 7, 6, 8, 3, 4, 9, 10, 10, 15, 17, 18, 19, 20, 21, 15, 15, 18, 2, 2, 2, 15, 1, 15, 15, 15, 19, -1, -2};
    private static final int[] PATTERN_INDEX_TO_DATE_FORMAT_FIELD = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37};
    private static final DateFormat.Field[] PATTERN_INDEX_TO_DATE_FORMAT_ATTRIBUTE = {DateFormat.Field.ERA, DateFormat.Field.YEAR, DateFormat.Field.MONTH, DateFormat.Field.DAY_OF_MONTH, DateFormat.Field.HOUR_OF_DAY1, DateFormat.Field.HOUR_OF_DAY0, DateFormat.Field.MINUTE, DateFormat.Field.SECOND, DateFormat.Field.MILLISECOND, DateFormat.Field.DAY_OF_WEEK, DateFormat.Field.DAY_OF_YEAR, DateFormat.Field.DAY_OF_WEEK_IN_MONTH, DateFormat.Field.WEEK_OF_YEAR, DateFormat.Field.WEEK_OF_MONTH, DateFormat.Field.AM_PM, DateFormat.Field.HOUR1, DateFormat.Field.HOUR0, DateFormat.Field.TIME_ZONE, DateFormat.Field.YEAR_WOY, DateFormat.Field.DOW_LOCAL, DateFormat.Field.EXTENDED_YEAR, DateFormat.Field.JULIAN_DAY, DateFormat.Field.MILLISECONDS_IN_DAY, DateFormat.Field.TIME_ZONE, DateFormat.Field.TIME_ZONE, DateFormat.Field.DAY_OF_WEEK, DateFormat.Field.MONTH, DateFormat.Field.QUARTER, DateFormat.Field.QUARTER, DateFormat.Field.TIME_ZONE, DateFormat.Field.YEAR, DateFormat.Field.TIME_ZONE, DateFormat.Field.TIME_ZONE, DateFormat.Field.TIME_ZONE, DateFormat.Field.RELATED_YEAR, DateFormat.Field.AM_PM_MIDNIGHT_NOON, DateFormat.Field.FLEXIBLE_DAY_PERIOD, DateFormat.Field.TIME_SEPARATOR};
    private static ICUCache<String, Object[]> PARSED_PATTERN_CACHE = new SimpleCache();
    static final UnicodeSet DATE_PATTERN_TYPE = new UnicodeSet("[GyYuUQqMLlwWd]").freeze();

    private enum ContextValue {
        UNKNOWN,
        CAPITALIZATION_FOR_MIDDLE_OF_SENTENCE,
        CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE,
        CAPITALIZATION_FOR_UI_LIST_OR_MENU,
        CAPITALIZATION_FOR_STANDALONE
    }

    private static int getLevelFromChar(char c) {
        if (c < PATTERN_CHAR_TO_LEVEL.length) {
            return PATTERN_CHAR_TO_LEVEL[c & 255];
        }
        return -1;
    }

    private static boolean isSyntaxChar(char c) {
        if (c < PATTERN_CHAR_IS_SYNTAX.length) {
            return PATTERN_CHAR_IS_SYNTAX[c & 255];
        }
        return false;
    }

    public SimpleDateFormat() {
        this(getDefaultPattern(), null, null, null, null, true, null);
    }

    public SimpleDateFormat(String str) {
        this(str, null, null, null, null, true, null);
    }

    public SimpleDateFormat(String str, Locale locale) {
        this(str, null, null, null, ULocale.forLocale(locale), true, null);
    }

    public SimpleDateFormat(String str, ULocale uLocale) {
        this(str, null, null, null, uLocale, true, null);
    }

    public SimpleDateFormat(String str, String str2, ULocale uLocale) {
        this(str, null, null, null, uLocale, false, str2);
    }

    public SimpleDateFormat(String str, DateFormatSymbols dateFormatSymbols) {
        this(str, (DateFormatSymbols) dateFormatSymbols.clone(), null, null, null, true, null);
    }

    @Deprecated
    public SimpleDateFormat(String str, DateFormatSymbols dateFormatSymbols, ULocale uLocale) {
        this(str, (DateFormatSymbols) dateFormatSymbols.clone(), null, null, uLocale, true, null);
    }

    SimpleDateFormat(String str, DateFormatSymbols dateFormatSymbols, Calendar calendar, ULocale uLocale, boolean z, String str2) {
        this(str, (DateFormatSymbols) dateFormatSymbols.clone(), (Calendar) calendar.clone(), null, uLocale, z, str2);
    }

    private SimpleDateFormat(String str, DateFormatSymbols dateFormatSymbols, Calendar calendar, NumberFormat numberFormat, ULocale uLocale, boolean z, String str2) {
        this.serialVersionOnStream = 2;
        this.capitalizationBrkIter = null;
        this.pattern = str;
        this.formatData = dateFormatSymbols;
        this.calendar = calendar;
        this.numberFormat = numberFormat;
        this.locale = uLocale;
        this.useFastFormat = z;
        this.override = str2;
        initialize();
    }

    @Deprecated
    public static SimpleDateFormat getInstance(Calendar.FormatConfiguration formatConfiguration) {
        String overrideString = formatConfiguration.getOverrideString();
        return new SimpleDateFormat(formatConfiguration.getPatternString(), formatConfiguration.getDateFormatSymbols(), formatConfiguration.getCalendar(), null, formatConfiguration.getLocale(), overrideString != null && overrideString.length() > 0, formatConfiguration.getOverrideString());
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
            NumberingSystem numberingSystem = NumberingSystem.getInstance(this.locale);
            String description = numberingSystem.getDescription();
            if (numberingSystem.isAlgorithmic() || description.length() != 10) {
                this.numberFormat = NumberFormat.getInstance(this.locale);
            } else {
                this.numberFormat = new DateNumberFormat(this.locale, description, numberingSystem.getName());
            }
        }
        if (this.numberFormat instanceof DecimalFormat) {
            fixNumberFormatForDates(this.numberFormat);
        }
        this.defaultCenturyBase = System.currentTimeMillis();
        setLocale(this.calendar.getLocale(ULocale.VALID_LOCALE), this.calendar.getLocale(ULocale.ACTUAL_LOCALE));
        initLocalZeroPaddingNumberFormat();
        if (this.override != null) {
            initNumberFormatters(this.locale);
        }
        parsePattern();
    }

    private synchronized void initializeTimeZoneFormat(boolean z) {
        String str;
        if (!z) {
            try {
                if (this.tzFormat == null) {
                    this.tzFormat = TimeZoneFormat.getInstance(this.locale);
                    str = null;
                    if (!(this.numberFormat instanceof DecimalFormat)) {
                        String[] digitStringsLocal = ((DecimalFormat) this.numberFormat).getDecimalFormatSymbols().getDigitStringsLocal();
                        StringBuilder sb = new StringBuilder();
                        for (String str2 : digitStringsLocal) {
                            sb.append(str2);
                        }
                        str = sb.toString();
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
            } catch (Throwable th) {
                throw th;
            }
        } else {
            this.tzFormat = TimeZoneFormat.getInstance(this.locale);
            str = null;
            if (!(this.numberFormat instanceof DecimalFormat)) {
            }
            if (str != null) {
                if (this.tzFormat.isFrozen()) {
                }
                this.tzFormat.setGMTOffsetDigits(str);
            }
        }
    }

    private TimeZoneFormat tzFormat() {
        if (this.tzFormat == null) {
            initializeTimeZoneFormat(false);
        }
        return this.tzFormat;
    }

    private static synchronized String getDefaultPattern() {
        ULocale uLocale = ULocale.getDefault(ULocale.Category.FORMAT);
        if (!uLocale.equals(cachedDefaultLocale)) {
            cachedDefaultLocale = uLocale;
            Calendar calendar = Calendar.getInstance(cachedDefaultLocale);
            try {
                ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, cachedDefaultLocale);
                ICUResourceBundle iCUResourceBundleFindWithFallback = iCUResourceBundle.findWithFallback("calendar/" + calendar.getType() + "/DateTimePatterns");
                if (iCUResourceBundleFindWithFallback == null) {
                    iCUResourceBundleFindWithFallback = iCUResourceBundle.findWithFallback("calendar/gregorian/DateTimePatterns");
                }
                if (iCUResourceBundleFindWithFallback == null || iCUResourceBundleFindWithFallback.getSize() < 9) {
                    cachedDefaultPattern = FALLBACKPATTERN;
                } else {
                    int i = 8;
                    if (iCUResourceBundleFindWithFallback.getSize() >= 13) {
                        i = 12;
                    }
                    cachedDefaultPattern = SimpleFormatterImpl.formatRawPattern(iCUResourceBundleFindWithFallback.getString(i), 2, 2, iCUResourceBundleFindWithFallback.getString(3), iCUResourceBundleFindWithFallback.getString(7));
                }
            } catch (MissingResourceException e) {
                cachedDefaultPattern = FALLBACKPATTERN;
            }
        }
        return cachedDefaultPattern;
    }

    private void parseAmbiguousDatesAsAfter(Date date) {
        this.defaultCenturyStart = date;
        this.calendar.setTime(date);
        this.defaultCenturyStartYear = this.calendar.get(1);
    }

    private void initializeDefaultCenturyStart(long j) {
        this.defaultCenturyBase = j;
        Calendar calendar = (Calendar) this.calendar.clone();
        calendar.setTimeInMillis(j);
        calendar.add(1, -80);
        this.defaultCenturyStart = calendar.getTime();
        this.defaultCenturyStartYear = calendar.get(1);
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

    public void set2DigitYearStart(Date date) {
        parseAmbiguousDatesAsAfter(date);
    }

    public Date get2DigitYearStart() {
        return getDefaultCenturyStart();
    }

    @Override
    public void setContext(DisplayContext displayContext) {
        super.setContext(displayContext);
        if (this.capitalizationBrkIter == null) {
            if (displayContext == DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE || displayContext == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU || displayContext == DisplayContext.CAPITALIZATION_FOR_STANDALONE) {
                this.capitalizationBrkIter = BreakIterator.getSentenceInstance(this.locale);
            }
        }
    }

    @Override
    public StringBuffer format(Calendar calendar, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        TimeZone timeZone;
        if (calendar != this.calendar && !calendar.getType().equals(this.calendar.getType())) {
            this.calendar.setTimeInMillis(calendar.getTimeInMillis());
            timeZone = this.calendar.getTimeZone();
            this.calendar.setTimeZone(calendar.getTimeZone());
            calendar = this.calendar;
        } else {
            timeZone = null;
        }
        StringBuffer stringBuffer2 = format(calendar, getContext(DisplayContext.Type.CAPITALIZATION), stringBuffer, fieldPosition, null);
        if (timeZone != null) {
            this.calendar.setTimeZone(timeZone);
        }
        return stringBuffer2;
    }

    private StringBuffer format(Calendar calendar, DisplayContext displayContext, StringBuffer stringBuffer, FieldPosition fieldPosition, List<FieldPosition> list) {
        int i;
        Object[] objArr;
        PatternItem patternItem;
        int i2 = 0;
        fieldPosition.setBeginIndex(0);
        fieldPosition.setEndIndex(0);
        Object[] patternItems = getPatternItems();
        int i3 = 0;
        while (i3 < patternItems.length) {
            if (patternItems[i3] instanceof String) {
                stringBuffer.append((String) patternItems[i3]);
                objArr = patternItems;
            } else {
                PatternItem patternItem2 = (PatternItem) patternItems[i3];
                int length = list != null ? stringBuffer.length() : i2;
                if (this.useFastFormat) {
                    i = length;
                    objArr = patternItems;
                    patternItem = patternItem2;
                    subFormat(stringBuffer, patternItem2.type, patternItem2.length, stringBuffer.length(), i3, displayContext, fieldPosition, calendar);
                } else {
                    i = length;
                    objArr = patternItems;
                    patternItem = patternItem2;
                    stringBuffer.append(subFormat(patternItem.type, patternItem.length, stringBuffer.length(), i3, displayContext, fieldPosition, calendar));
                }
                if (list != null) {
                    int length2 = stringBuffer.length();
                    if (length2 - i > 0) {
                        FieldPosition fieldPosition2 = new FieldPosition(patternCharToDateFormatField(patternItem.type));
                        fieldPosition2.setBeginIndex(i);
                        fieldPosition2.setEndIndex(length2);
                        list.add(fieldPosition2);
                    }
                }
            }
            i3++;
            patternItems = objArr;
            i2 = 0;
        }
        return stringBuffer;
    }

    private static int getIndexFromChar(char c) {
        if (c < PATTERN_CHAR_TO_INDEX.length) {
            return PATTERN_CHAR_TO_INDEX[c & 255];
        }
        return -1;
    }

    protected DateFormat.Field patternCharToDateFormatField(char c) {
        int indexFromChar = getIndexFromChar(c);
        if (indexFromChar != -1) {
            return PATTERN_INDEX_TO_DATE_FORMAT_ATTRIBUTE[indexFromChar];
        }
        return null;
    }

    protected String subFormat(char c, int i, int i2, FieldPosition fieldPosition, DateFormatSymbols dateFormatSymbols, Calendar calendar) throws IllegalArgumentException {
        return subFormat(c, i, i2, 0, DisplayContext.CAPITALIZATION_NONE, fieldPosition, calendar);
    }

    @Deprecated
    protected String subFormat(char c, int i, int i2, int i3, DisplayContext displayContext, FieldPosition fieldPosition, Calendar calendar) {
        StringBuffer stringBuffer = new StringBuffer();
        subFormat(stringBuffer, c, i, i2, i3, displayContext, fieldPosition, calendar);
        return stringBuffer.toString();
    }

    @Deprecated
    protected void subFormat(StringBuffer stringBuffer, char c, int i, int i2, int i3, DisplayContext displayContext, FieldPosition fieldPosition, Calendar calendar) {
        boolean z;
        char c2;
        boolean z2;
        boolean z3;
        char c3;
        char c4;
        boolean z4;
        boolean z5;
        boolean z6;
        String str;
        DateFormatSymbols.CapitalizationContextUsage capitalizationContextUsage;
        DateFormatSymbols.CapitalizationContextUsage capitalizationContextUsage2;
        boolean z7;
        int length = stringBuffer.length();
        TimeZone timeZone = calendar.getTimeZone();
        long timeInMillis = calendar.getTimeInMillis();
        int indexFromChar = getIndexFromChar(c);
        if (indexFromChar == -1) {
            if (c == 'l') {
                return;
            }
            throw new IllegalArgumentException("Illegal pattern character '" + c + "' in \"" + this.pattern + '\"');
        }
        int i4 = PATTERN_INDEX_TO_CALENDAR_FIELD[indexFromChar];
        int relatedYear = i4 >= 0 ? indexFromChar != 34 ? calendar.get(i4) : calendar.getRelatedYear() : 0;
        NumberFormat numberFormat = getNumberFormat(c);
        DateFormatSymbols.CapitalizationContextUsage capitalizationContextUsage3 = DateFormatSymbols.CapitalizationContextUsage.OTHER;
        switch (indexFromChar) {
            case 0:
                z = true;
                z = true;
                z = true;
                z = true;
                c2 = 0;
                if (calendar.getType().equals("chinese") || calendar.getType().equals("dangi")) {
                    zeroPaddingNumber(numberFormat, stringBuffer, relatedYear, 1, 9);
                } else if (i == 5) {
                    safeAppend(this.formatData.narrowEras, relatedYear, stringBuffer);
                    capitalizationContextUsage3 = DateFormatSymbols.CapitalizationContextUsage.ERA_NARROW;
                } else if (i == 4) {
                    safeAppend(this.formatData.eraNames, relatedYear, stringBuffer);
                    capitalizationContextUsage3 = DateFormatSymbols.CapitalizationContextUsage.ERA_WIDE;
                } else {
                    safeAppend(this.formatData.eras, relatedYear, stringBuffer);
                    capitalizationContextUsage3 = DateFormatSymbols.CapitalizationContextUsage.ERA_ABBREV;
                }
                capitalizationContextUsage2 = capitalizationContextUsage3;
                z7 = z;
                break;
            case 1:
            case 18:
                z2 = true;
                c4 = 0;
                if (this.override != null && ((this.override.compareTo("hebr") == 0 || this.override.indexOf("y=hebr") >= 0) && relatedYear > HEBREW_CAL_CUR_MILLENIUM_START_YEAR && relatedYear < HEBREW_CAL_CUR_MILLENIUM_END_YEAR)) {
                    relatedYear -= 5000;
                }
                if (i != 2) {
                    zeroPaddingNumber(numberFormat, stringBuffer, relatedYear, 2, 2);
                    z3 = z2;
                } else {
                    zeroPaddingNumber(numberFormat, stringBuffer, relatedYear, i, Integer.MAX_VALUE);
                    z3 = z2;
                }
                c2 = c4;
                z = z3;
                capitalizationContextUsage2 = capitalizationContextUsage3;
                z7 = z;
                break;
            case 2:
            case 26:
                z3 = true;
                z6 = true;
                z3 = true;
                z3 = true;
                z3 = true;
                z3 = true;
                if (!calendar.getType().equals("hebrew")) {
                    c3 = 6;
                    int i5 = (this.formatData.leapMonthPatterns != null || this.formatData.leapMonthPatterns.length < 7) ? 0 : calendar.get(22);
                    if (i == 5) {
                        if (i != 4) {
                            c4 = 0;
                            if (i != 3) {
                                StringBuffer stringBuffer2 = new StringBuffer();
                                char c5 = c3;
                                zeroPaddingNumber(numberFormat, stringBuffer2, relatedYear + 1, i, Integer.MAX_VALUE);
                                c4 = 0;
                                safeAppendWithMonthPattern(new String[]{stringBuffer2.toString()}, 0, stringBuffer, i5 != 0 ? this.formatData.leapMonthPatterns[c5] : null);
                            } else if (indexFromChar == 2) {
                                safeAppendWithMonthPattern(this.formatData.shortMonths, relatedYear, stringBuffer, i5 != 0 ? this.formatData.leapMonthPatterns[1] : null);
                                capitalizationContextUsage3 = DateFormatSymbols.CapitalizationContextUsage.MONTH_FORMAT;
                            } else {
                                safeAppendWithMonthPattern(this.formatData.standaloneShortMonths, relatedYear, stringBuffer, i5 != 0 ? this.formatData.leapMonthPatterns[4] : null);
                                capitalizationContextUsage3 = DateFormatSymbols.CapitalizationContextUsage.MONTH_STANDALONE;
                            }
                        } else if (indexFromChar == 2) {
                            String[] strArr = this.formatData.months;
                            if (i5 != 0) {
                                c4 = 0;
                                str = this.formatData.leapMonthPatterns[0];
                            } else {
                                c4 = 0;
                            }
                            safeAppendWithMonthPattern(strArr, relatedYear, stringBuffer, str);
                            capitalizationContextUsage3 = DateFormatSymbols.CapitalizationContextUsage.MONTH_FORMAT;
                        } else {
                            c4 = 0;
                            safeAppendWithMonthPattern(this.formatData.standaloneMonths, relatedYear, stringBuffer, i5 != 0 ? this.formatData.leapMonthPatterns[3] : null);
                            capitalizationContextUsage3 = DateFormatSymbols.CapitalizationContextUsage.MONTH_STANDALONE;
                        }
                        c2 = c4;
                        z = z3;
                        capitalizationContextUsage2 = capitalizationContextUsage3;
                        z7 = z;
                    } else {
                        if (indexFromChar == 2) {
                            safeAppendWithMonthPattern(this.formatData.narrowMonths, relatedYear, stringBuffer, i5 != 0 ? this.formatData.leapMonthPatterns[2] : null);
                        } else {
                            safeAppendWithMonthPattern(this.formatData.standaloneNarrowMonths, relatedYear, stringBuffer, i5 != 0 ? this.formatData.leapMonthPatterns[5] : null);
                        }
                        capitalizationContextUsage3 = DateFormatSymbols.CapitalizationContextUsage.MONTH_NARROW;
                        capitalizationContextUsage2 = capitalizationContextUsage3;
                        c2 = 0;
                        z7 = z6;
                    }
                    break;
                } else {
                    boolean zIsLeapYear = HebrewCalendar.isLeapYear(calendar.get(1));
                    if (zIsLeapYear && relatedYear == 6 && i >= 3) {
                        relatedYear = 13;
                    }
                    if (!zIsLeapYear) {
                        c3 = 6;
                        if (relatedYear >= 6 && i < 3) {
                            relatedYear--;
                        }
                    }
                    if (this.formatData.leapMonthPatterns != null) {
                        if (i == 5) {
                        }
                    }
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
                z = true;
                c2 = 0;
                zeroPaddingNumber(numberFormat, stringBuffer, relatedYear, i, Integer.MAX_VALUE);
                capitalizationContextUsage2 = capitalizationContextUsage3;
                z7 = z;
                break;
            case 4:
                z4 = true;
                z4 = true;
                if (relatedYear == 0) {
                    zeroPaddingNumber(numberFormat, stringBuffer, calendar.getMaximum(11) + 1, i, Integer.MAX_VALUE);
                } else {
                    zeroPaddingNumber(numberFormat, stringBuffer, relatedYear, i, Integer.MAX_VALUE);
                }
                c2 = 0;
                z = z4;
                capitalizationContextUsage2 = capitalizationContextUsage3;
                z7 = z;
                break;
            case 8:
                z4 = true;
                z4 = true;
                this.numberFormat.setMinimumIntegerDigits(Math.min(3, i));
                this.numberFormat.setMaximumIntegerDigits(Integer.MAX_VALUE);
                if (i == 1) {
                    relatedYear /= 100;
                } else if (i == 2) {
                    relatedYear /= 10;
                }
                FieldPosition fieldPosition2 = new FieldPosition(-1);
                this.numberFormat.format(relatedYear, stringBuffer, fieldPosition2);
                if (i > 3) {
                    this.numberFormat.setMinimumIntegerDigits(i - 3);
                    this.numberFormat.format(0L, stringBuffer, fieldPosition2);
                }
                c2 = 0;
                z = z4;
                capitalizationContextUsage2 = capitalizationContextUsage3;
                z7 = z;
                break;
            case 9:
                z5 = true;
                if (i != 5) {
                    safeAppend(this.formatData.narrowWeekdays, relatedYear, stringBuffer);
                    capitalizationContextUsage3 = DateFormatSymbols.CapitalizationContextUsage.DAY_NARROW;
                    z6 = z5;
                } else if (i == 4) {
                    safeAppend(this.formatData.weekdays, relatedYear, stringBuffer);
                    capitalizationContextUsage3 = DateFormatSymbols.CapitalizationContextUsage.DAY_FORMAT;
                    z6 = z5;
                } else if (i != 6 || this.formatData.shorterWeekdays == null) {
                    safeAppend(this.formatData.shortWeekdays, relatedYear, stringBuffer);
                    capitalizationContextUsage3 = DateFormatSymbols.CapitalizationContextUsage.DAY_FORMAT;
                    z6 = z5;
                } else {
                    safeAppend(this.formatData.shorterWeekdays, relatedYear, stringBuffer);
                    capitalizationContextUsage3 = DateFormatSymbols.CapitalizationContextUsage.DAY_FORMAT;
                    z6 = z5;
                }
                capitalizationContextUsage2 = capitalizationContextUsage3;
                c2 = 0;
                z7 = z6;
                break;
            case 14:
                z4 = true;
                z4 = true;
                if (i < 5 || this.formatData.ampmsNarrow == null) {
                    safeAppend(this.formatData.ampms, relatedYear, stringBuffer);
                } else {
                    safeAppend(this.formatData.ampmsNarrow, relatedYear, stringBuffer);
                }
                c2 = 0;
                z = z4;
                capitalizationContextUsage2 = capitalizationContextUsage3;
                z7 = z;
                break;
            case 15:
                z4 = true;
                z4 = true;
                if (relatedYear == 0) {
                    zeroPaddingNumber(numberFormat, stringBuffer, calendar.getLeastMaximum(10) + 1, i, Integer.MAX_VALUE);
                } else {
                    zeroPaddingNumber(numberFormat, stringBuffer, relatedYear, i, Integer.MAX_VALUE);
                }
                c2 = 0;
                z = z4;
                capitalizationContextUsage2 = capitalizationContextUsage3;
                z7 = z;
                break;
            case 17:
                z6 = true;
                if (i < 4) {
                    str = tzFormat().format(TimeZoneFormat.Style.SPECIFIC_SHORT, timeZone, timeInMillis);
                    capitalizationContextUsage = DateFormatSymbols.CapitalizationContextUsage.METAZONE_SHORT;
                } else {
                    str = tzFormat().format(TimeZoneFormat.Style.SPECIFIC_LONG, timeZone, timeInMillis);
                    capitalizationContextUsage = DateFormatSymbols.CapitalizationContextUsage.METAZONE_LONG;
                }
                capitalizationContextUsage3 = capitalizationContextUsage;
                stringBuffer.append(str);
                capitalizationContextUsage2 = capitalizationContextUsage3;
                c2 = 0;
                z7 = z6;
                break;
            case 19:
                z5 = true;
                z4 = true;
                if (i >= 3) {
                    relatedYear = calendar.get(7);
                    if (i != 5) {
                    }
                    capitalizationContextUsage2 = capitalizationContextUsage3;
                    c2 = 0;
                    z7 = z6;
                } else {
                    zeroPaddingNumber(numberFormat, stringBuffer, relatedYear, i, Integer.MAX_VALUE);
                    c2 = 0;
                    z = z4;
                    capitalizationContextUsage2 = capitalizationContextUsage3;
                    z7 = z;
                }
                break;
            case 23:
                z4 = true;
                stringBuffer.append(i < 4 ? tzFormat().format(TimeZoneFormat.Style.ISO_BASIC_LOCAL_FULL, timeZone, timeInMillis) : i == 5 ? tzFormat().format(TimeZoneFormat.Style.ISO_EXTENDED_FULL, timeZone, timeInMillis) : tzFormat().format(TimeZoneFormat.Style.LOCALIZED_GMT, timeZone, timeInMillis));
                c2 = 0;
                z = z4;
                capitalizationContextUsage2 = capitalizationContextUsage3;
                z7 = z;
                break;
            case 24:
                z6 = true;
                if (i == 1) {
                    str = tzFormat().format(TimeZoneFormat.Style.GENERIC_SHORT, timeZone, timeInMillis);
                    capitalizationContextUsage3 = DateFormatSymbols.CapitalizationContextUsage.METAZONE_SHORT;
                } else if (i == 4) {
                    str = tzFormat().format(TimeZoneFormat.Style.GENERIC_LONG, timeZone, timeInMillis);
                    capitalizationContextUsage3 = DateFormatSymbols.CapitalizationContextUsage.METAZONE_LONG;
                }
                stringBuffer.append(str);
                capitalizationContextUsage2 = capitalizationContextUsage3;
                c2 = 0;
                z7 = z6;
                break;
            case 25:
                z6 = true;
                z4 = true;
                z6 = true;
                z6 = true;
                z6 = true;
                if (i >= 3) {
                    int i6 = calendar.get(7);
                    if (i == 5) {
                        safeAppend(this.formatData.standaloneNarrowWeekdays, i6, stringBuffer);
                        capitalizationContextUsage3 = DateFormatSymbols.CapitalizationContextUsage.DAY_NARROW;
                    } else if (i == 4) {
                        safeAppend(this.formatData.standaloneWeekdays, i6, stringBuffer);
                        capitalizationContextUsage3 = DateFormatSymbols.CapitalizationContextUsage.DAY_STANDALONE;
                    } else if (i != 6 || this.formatData.standaloneShorterWeekdays == null) {
                        safeAppend(this.formatData.standaloneShortWeekdays, i6, stringBuffer);
                        capitalizationContextUsage3 = DateFormatSymbols.CapitalizationContextUsage.DAY_STANDALONE;
                    } else {
                        safeAppend(this.formatData.standaloneShorterWeekdays, i6, stringBuffer);
                        capitalizationContextUsage3 = DateFormatSymbols.CapitalizationContextUsage.DAY_STANDALONE;
                    }
                    capitalizationContextUsage2 = capitalizationContextUsage3;
                    c2 = 0;
                    z7 = z6;
                } else {
                    zeroPaddingNumber(numberFormat, stringBuffer, relatedYear, 1, Integer.MAX_VALUE);
                    c2 = 0;
                    z = z4;
                    capitalizationContextUsage2 = capitalizationContextUsage3;
                    z7 = z;
                }
                break;
            case 27:
                z4 = true;
                z4 = true;
                z4 = true;
                if (i >= 4) {
                    safeAppend(this.formatData.quarters, relatedYear / 3, stringBuffer);
                } else if (i == 3) {
                    safeAppend(this.formatData.shortQuarters, relatedYear / 3, stringBuffer);
                } else {
                    zeroPaddingNumber(numberFormat, stringBuffer, (relatedYear / 3) + 1, i, Integer.MAX_VALUE);
                }
                c2 = 0;
                z = z4;
                capitalizationContextUsage2 = capitalizationContextUsage3;
                z7 = z;
                break;
            case 28:
                z4 = true;
                z4 = true;
                z4 = true;
                if (i >= 4) {
                    safeAppend(this.formatData.standaloneQuarters, relatedYear / 3, stringBuffer);
                } else if (i == 3) {
                    safeAppend(this.formatData.standaloneShortQuarters, relatedYear / 3, stringBuffer);
                } else {
                    zeroPaddingNumber(numberFormat, stringBuffer, (relatedYear / 3) + 1, i, Integer.MAX_VALUE);
                }
                c2 = 0;
                z = z4;
                capitalizationContextUsage2 = capitalizationContextUsage3;
                z7 = z;
                break;
            case 29:
                z6 = true;
                if (i == 1) {
                    str = tzFormat().format(TimeZoneFormat.Style.ZONE_ID_SHORT, timeZone, timeInMillis);
                } else if (i == 2) {
                    str = tzFormat().format(TimeZoneFormat.Style.ZONE_ID, timeZone, timeInMillis);
                } else if (i == 3) {
                    str = tzFormat().format(TimeZoneFormat.Style.EXEMPLAR_LOCATION, timeZone, timeInMillis);
                } else if (i == 4) {
                    str = tzFormat().format(TimeZoneFormat.Style.GENERIC_LOCATION, timeZone, timeInMillis);
                    capitalizationContextUsage3 = DateFormatSymbols.CapitalizationContextUsage.ZONE_LONG;
                }
                stringBuffer.append(str);
                capitalizationContextUsage2 = capitalizationContextUsage3;
                c2 = 0;
                z7 = z6;
                break;
            case 30:
                z2 = true;
                z4 = true;
                z2 = true;
                if (this.formatData.shortYearNames != null && relatedYear <= this.formatData.shortYearNames.length) {
                    safeAppend(this.formatData.shortYearNames, relatedYear - 1, stringBuffer);
                    c2 = 0;
                    z = z4;
                    capitalizationContextUsage2 = capitalizationContextUsage3;
                    z7 = z;
                }
                c4 = 0;
                if (this.override != null) {
                    relatedYear -= 5000;
                }
                if (i != 2) {
                }
                c2 = c4;
                z = z3;
                capitalizationContextUsage2 = capitalizationContextUsage3;
                z7 = z;
                break;
            case 31:
                z4 = true;
                if (i == 1) {
                    str = tzFormat().format(TimeZoneFormat.Style.LOCALIZED_GMT_SHORT, timeZone, timeInMillis);
                } else if (i == 4) {
                    str = tzFormat().format(TimeZoneFormat.Style.LOCALIZED_GMT, timeZone, timeInMillis);
                }
                stringBuffer.append(str);
                c2 = 0;
                z = z4;
                capitalizationContextUsage2 = capitalizationContextUsage3;
                z7 = z;
                break;
            case 32:
                z4 = true;
                if (i == 1) {
                    str = tzFormat().format(TimeZoneFormat.Style.ISO_BASIC_SHORT, timeZone, timeInMillis);
                } else if (i == 2) {
                    str = tzFormat().format(TimeZoneFormat.Style.ISO_BASIC_FIXED, timeZone, timeInMillis);
                } else if (i == 3) {
                    str = tzFormat().format(TimeZoneFormat.Style.ISO_EXTENDED_FIXED, timeZone, timeInMillis);
                } else if (i == 4) {
                    str = tzFormat().format(TimeZoneFormat.Style.ISO_BASIC_FULL, timeZone, timeInMillis);
                } else if (i == 5) {
                    str = tzFormat().format(TimeZoneFormat.Style.ISO_EXTENDED_FULL, timeZone, timeInMillis);
                }
                stringBuffer.append(str);
                c2 = 0;
                z = z4;
                capitalizationContextUsage2 = capitalizationContextUsage3;
                z7 = z;
                break;
            case 33:
                z4 = true;
                if (i == 1) {
                    str = tzFormat().format(TimeZoneFormat.Style.ISO_BASIC_LOCAL_SHORT, timeZone, timeInMillis);
                } else if (i == 2) {
                    str = tzFormat().format(TimeZoneFormat.Style.ISO_BASIC_LOCAL_FIXED, timeZone, timeInMillis);
                } else if (i == 3) {
                    str = tzFormat().format(TimeZoneFormat.Style.ISO_EXTENDED_LOCAL_FIXED, timeZone, timeInMillis);
                } else if (i == 4) {
                    str = tzFormat().format(TimeZoneFormat.Style.ISO_BASIC_LOCAL_FULL, timeZone, timeInMillis);
                } else if (i == 5) {
                    str = tzFormat().format(TimeZoneFormat.Style.ISO_EXTENDED_LOCAL_FULL, timeZone, timeInMillis);
                }
                stringBuffer.append(str);
                c2 = 0;
                z = z4;
                capitalizationContextUsage2 = capitalizationContextUsage3;
                z7 = z;
                break;
            case 35:
                if (calendar.get(11) == 12 && ((!this.hasMinute || calendar.get(12) == 0) && (!this.hasSecond || calendar.get(13) == 0))) {
                    int i7 = calendar.get(9);
                    str = i <= 3 ? this.formatData.abbreviatedDayPeriods[i7] : (i == 4 || i > 5) ? this.formatData.wideDayPeriods[i7] : this.formatData.narrowDayPeriods[i7];
                }
                String str2 = str;
                if (str2 == null) {
                    subFormat(stringBuffer, 'a', i, i2, i3, displayContext, fieldPosition, calendar);
                } else {
                    stringBuffer.append(str2);
                }
                z4 = true;
                c2 = 0;
                z = z4;
                capitalizationContextUsage2 = capitalizationContextUsage3;
                z7 = z;
                break;
            case 36:
                DayPeriodRules dayPeriodRules = DayPeriodRules.getInstance(getLocale());
                if (dayPeriodRules == null) {
                    subFormat(stringBuffer, 'a', i, i2, i3, displayContext, fieldPosition, calendar);
                } else {
                    int i8 = calendar.get(11);
                    int i9 = this.hasMinute ? calendar.get(12) : 0;
                    int i10 = this.hasSecond ? calendar.get(13) : 0;
                    DayPeriodRules.DayPeriod dayPeriodForHour = (i8 == 0 && i9 == 0 && i10 == 0 && dayPeriodRules.hasMidnight()) ? DayPeriodRules.DayPeriod.MIDNIGHT : (i8 == 12 && i9 == 0 && i10 == 0 && dayPeriodRules.hasNoon()) ? DayPeriodRules.DayPeriod.NOON : dayPeriodRules.getDayPeriodForHour(i8);
                    if (dayPeriodForHour != DayPeriodRules.DayPeriod.AM && dayPeriodForHour != DayPeriodRules.DayPeriod.PM && dayPeriodForHour != DayPeriodRules.DayPeriod.MIDNIGHT) {
                        int iOrdinal = dayPeriodForHour.ordinal();
                        str = i <= 3 ? this.formatData.abbreviatedDayPeriods[iOrdinal] : (i == 4 || i > 5) ? this.formatData.wideDayPeriods[iOrdinal] : this.formatData.narrowDayPeriods[iOrdinal];
                    }
                    if (str == null && (dayPeriodForHour == DayPeriodRules.DayPeriod.MIDNIGHT || dayPeriodForHour == DayPeriodRules.DayPeriod.NOON)) {
                        dayPeriodForHour = dayPeriodRules.getDayPeriodForHour(i8);
                        int iOrdinal2 = dayPeriodForHour.ordinal();
                        str = i <= 3 ? this.formatData.abbreviatedDayPeriods[iOrdinal2] : (i == 4 || i > 5) ? this.formatData.wideDayPeriods[iOrdinal2] : this.formatData.narrowDayPeriods[iOrdinal2];
                    }
                    String str3 = str;
                    if (dayPeriodForHour == DayPeriodRules.DayPeriod.AM || dayPeriodForHour == DayPeriodRules.DayPeriod.PM || str3 == null) {
                        subFormat(stringBuffer, 'a', i, i2, i3, displayContext, fieldPosition, calendar);
                    } else {
                        stringBuffer.append(str3);
                    }
                }
                z4 = true;
                c2 = 0;
                z = z4;
                capitalizationContextUsage2 = capitalizationContextUsage3;
                z7 = z;
                break;
            case 37:
                stringBuffer.append(this.formatData.getTimeSeparatorString());
                z4 = true;
                c2 = 0;
                z = z4;
                capitalizationContextUsage2 = capitalizationContextUsage3;
                z7 = z;
                break;
        }
        if (i3 == 0 && displayContext != null && UCharacter.isLowerCase(stringBuffer.codePointAt(length))) {
            ?? r6 = z7;
            switch (displayContext) {
                case CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE:
                    break;
                case CAPITALIZATION_FOR_UI_LIST_OR_MENU:
                case CAPITALIZATION_FOR_STANDALONE:
                    if (this.formatData.capitalization != null) {
                        boolean[] zArr = this.formatData.capitalization.get(capitalizationContextUsage2);
                        r6 = displayContext == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU ? zArr[c2] : zArr[z7 ? 1 : 0];
                        break;
                    }
                default:
                    r6 = c2;
                    break;
            }
            if (r6 != 0) {
                if (this.capitalizationBrkIter == null) {
                    this.capitalizationBrkIter = BreakIterator.getSentenceInstance(this.locale);
                }
                stringBuffer.replace(length, stringBuffer.length(), UCharacter.toTitleCase(this.locale, stringBuffer.substring(length), this.capitalizationBrkIter, CollationSettings.CASE_FIRST_AND_UPPER_MASK));
            }
        }
        if (fieldPosition.getBeginIndex() == fieldPosition.getEndIndex()) {
            if (fieldPosition.getField() == PATTERN_INDEX_TO_DATE_FORMAT_FIELD[indexFromChar]) {
                fieldPosition.setBeginIndex(i2);
                fieldPosition.setEndIndex((stringBuffer.length() + i2) - length);
            } else if (fieldPosition.getFieldAttribute() == PATTERN_INDEX_TO_DATE_FORMAT_ATTRIBUTE[indexFromChar]) {
                fieldPosition.setBeginIndex(i2);
                fieldPosition.setEndIndex((stringBuffer.length() + i2) - length);
            }
        }
    }

    private static void safeAppend(String[] strArr, int i, StringBuffer stringBuffer) {
        if (strArr != null && i >= 0 && i < strArr.length) {
            stringBuffer.append(strArr[i]);
        }
    }

    private static void safeAppendWithMonthPattern(String[] strArr, int i, StringBuffer stringBuffer, String str) {
        if (strArr != null && i >= 0 && i < strArr.length) {
            if (str == null) {
                stringBuffer.append(strArr[i]);
            } else {
                stringBuffer.append(SimpleFormatterImpl.formatRawPattern(str, 1, 1, strArr[i]));
            }
        }
    }

    private static class PatternItem {
        final boolean isNumeric;
        final int length;
        final char type;

        PatternItem(char c, int i) {
            this.type = c;
            this.length = i;
            this.isNumeric = SimpleDateFormat.isNumeric(c, i);
        }
    }

    private Object[] getPatternItems() {
        char c;
        boolean z;
        if (this.patternItems != null) {
            return this.patternItems;
        }
        this.patternItems = PARSED_PATTERN_CACHE.get(this.pattern);
        if (this.patternItems != null) {
            return this.patternItems;
        }
        StringBuilder sb = new StringBuilder();
        ArrayList arrayList = new ArrayList();
        int i = 1;
        char c2 = 0;
        boolean z2 = false;
        boolean z3 = false;
        for (int i2 = 0; i2 < this.pattern.length(); i2++) {
            char cCharAt = this.pattern.charAt(i2);
            if (cCharAt == '\'') {
                if (z2) {
                    sb.append(PatternTokenizer.SINGLE_QUOTE);
                    c = c2;
                    z = false;
                } else if (c2 != 0) {
                    arrayList.add(new PatternItem(c2, i));
                    z = true;
                    c = 0;
                } else {
                    c = c2;
                    z = true;
                }
                z3 = !z3;
                char c3 = c;
                z2 = z;
                c2 = c3;
            } else {
                if (z3) {
                    sb.append(cCharAt);
                } else if (!isSyntaxChar(cCharAt)) {
                    if (c2 != 0) {
                        arrayList.add(new PatternItem(c2, i));
                        c2 = 0;
                    }
                    sb.append(cCharAt);
                } else if (cCharAt == c2) {
                    i++;
                } else {
                    if (c2 != 0) {
                        arrayList.add(new PatternItem(c2, i));
                    } else if (sb.length() > 0) {
                        arrayList.add(sb.toString());
                        sb.setLength(0);
                    }
                    i = 1;
                    z2 = false;
                    c2 = cCharAt;
                }
                z2 = false;
            }
        }
        if (c2 != 0) {
            arrayList.add(new PatternItem(c2, i));
        } else if (sb.length() > 0) {
            arrayList.add(sb.toString());
            sb.setLength(0);
        }
        this.patternItems = arrayList.toArray(new Object[arrayList.size()]);
        PARSED_PATTERN_CACHE.put(this.pattern, this.patternItems);
        return this.patternItems;
    }

    @Deprecated
    protected void zeroPaddingNumber(NumberFormat numberFormat, StringBuffer stringBuffer, int i, int i2, int i3) {
        if (this.useLocalZeroPaddingNumberFormat && i >= 0) {
            fastZeroPaddingNumber(stringBuffer, i, i2, i3);
            return;
        }
        numberFormat.setMinimumIntegerDigits(i2);
        numberFormat.setMaximumIntegerDigits(i3);
        numberFormat.format(i, stringBuffer, new FieldPosition(-1));
    }

    @Override
    public void setNumberFormat(NumberFormat numberFormat) {
        super.setNumberFormat(numberFormat);
        initLocalZeroPaddingNumberFormat();
        initializeTimeZoneFormat(true);
        if (this.numberFormatters != null) {
            this.numberFormatters = null;
        }
        if (this.overrideMap != null) {
            this.overrideMap = null;
        }
    }

    private void initLocalZeroPaddingNumberFormat() {
        if (this.numberFormat instanceof DecimalFormat) {
            String[] digitStringsLocal = ((DecimalFormat) this.numberFormat).getDecimalFormatSymbols().getDigitStringsLocal();
            this.useLocalZeroPaddingNumberFormat = true;
            this.decDigits = new char[10];
            int i = 0;
            while (true) {
                if (i >= 10) {
                    break;
                }
                if (digitStringsLocal[i].length() > 1) {
                    this.useLocalZeroPaddingNumberFormat = false;
                    break;
                } else {
                    this.decDigits[i] = digitStringsLocal[i].charAt(0);
                    i++;
                }
            }
        } else if (this.numberFormat instanceof DateNumberFormat) {
            this.decDigits = ((DateNumberFormat) this.numberFormat).getDigits();
            this.useLocalZeroPaddingNumberFormat = true;
        } else {
            this.useLocalZeroPaddingNumberFormat = false;
        }
        if (this.useLocalZeroPaddingNumberFormat) {
            this.decimalBuf = new char[10];
        }
    }

    private void fastZeroPaddingNumber(StringBuffer stringBuffer, int i, int i2, int i3) {
        if (this.decimalBuf.length < i3) {
            i3 = this.decimalBuf.length;
        }
        int i4 = i3 - 1;
        while (true) {
            this.decimalBuf[i4] = this.decDigits[i % 10];
            i /= 10;
            if (i4 == 0 || i == 0) {
                break;
            } else {
                i4--;
            }
        }
        int i5 = i2 - (i3 - i4);
        while (i5 > 0 && i4 > 0) {
            i4--;
            this.decimalBuf[i4] = this.decDigits[0];
            i5--;
        }
        while (i5 > 0) {
            stringBuffer.append(this.decDigits[0]);
            i5--;
        }
        stringBuffer.append(this.decimalBuf, i4, i3 - i4);
    }

    protected String zeroPaddingNumber(long j, int i, int i2) {
        this.numberFormat.setMinimumIntegerDigits(i);
        this.numberFormat.setMaximumIntegerDigits(i2);
        return this.numberFormat.format(j);
    }

    private static final boolean isNumeric(char c, int i) {
        return NUMERIC_FORMAT_CHARS.indexOf(c) >= 0 || (i <= 2 && NUMERIC_FORMAT_CHARS2.indexOf(c) >= 0);
    }

    @Override
    public void parse(String str, Calendar calendar, ParsePosition parsePosition) {
        Calendar calendar2;
        Calendar calendar3;
        TimeZone timeZone;
        int dSTSavings;
        int i;
        TimeZoneTransition previousTransition;
        TimeZoneTransition nextTransition;
        int dSTSavings2;
        int i2;
        int i3;
        int i4;
        boolean[] zArr;
        Output<TimeZoneFormat.TimeType> output;
        Output<DayPeriodRules.DayPeriod> output2;
        int i5;
        Calendar calendar4;
        TimeZone timeZone2;
        Object[] objArr;
        boolean z;
        int i6;
        int i7;
        int i8;
        int i9;
        int i10;
        if (calendar == this.calendar || calendar.getType().equals(this.calendar.getType())) {
            calendar2 = calendar;
            calendar3 = null;
            timeZone = null;
        } else {
            this.calendar.setTimeInMillis(calendar.getTimeInMillis());
            TimeZone timeZone3 = this.calendar.getTimeZone();
            this.calendar.setTimeZone(calendar.getTimeZone());
            timeZone = timeZone3;
            calendar3 = calendar;
            calendar2 = this.calendar;
        }
        int index = parsePosition.getIndex();
        if (index < 0) {
            parsePosition.setErrorIndex(0);
            return;
        }
        Output<DayPeriodRules.DayPeriod> output3 = new Output<>(null);
        Output<TimeZoneFormat.TimeType> output4 = new Output<>(TimeZoneFormat.TimeType.UNKNOWN);
        boolean[] zArr2 = {false};
        MessageFormat messageFormat = (this.formatData.leapMonthPatterns == null || this.formatData.leapMonthPatterns.length < 7) ? null : new MessageFormat(this.formatData.leapMonthPatterns[6], this.locale);
        Object[] patternItems = getPatternItems();
        int i11 = -1;
        int i12 = -1;
        int i13 = 0;
        int i14 = 0;
        int i15 = 0;
        int iMatchLiteral = index;
        while (i13 < patternItems.length) {
            if (patternItems[i13] instanceof PatternItem) {
                PatternItem patternItem = (PatternItem) patternItems[i13];
                if (patternItem.isNumeric && i12 == i11 && (i10 = i13 + 1) < patternItems.length && (patternItems[i10] instanceof PatternItem) && ((PatternItem) patternItems[i10]).isNumeric) {
                    i15 = iMatchLiteral;
                    i6 = i13;
                    i14 = patternItem.length;
                } else {
                    i6 = i12;
                }
                if (i6 != -1) {
                    int i16 = patternItem.length;
                    if (i6 == i13) {
                        i16 = i14;
                    }
                    int i17 = i13;
                    i4 = -1;
                    Object[] objArr2 = patternItems;
                    zArr = zArr2;
                    output = output4;
                    output2 = output3;
                    int i18 = index;
                    i7 = i6;
                    Calendar calendar5 = calendar3;
                    int iSubParse = subParse(str, iMatchLiteral, patternItem.type, i16, true, false, zArr, calendar2, messageFormat, output);
                    if (iSubParse < 0) {
                        i14--;
                        if (i14 == 0) {
                            parsePosition.setIndex(i18);
                            parsePosition.setErrorIndex(iSubParse);
                            if (timeZone != null) {
                                this.calendar.setTimeZone(timeZone);
                                return;
                            }
                            return;
                        }
                        index = i18;
                        calendar3 = calendar5;
                        iMatchLiteral = i15;
                        i11 = -1;
                        patternItems = objArr2;
                        zArr2 = zArr;
                        output4 = output;
                        output3 = output2;
                        i13 = i7;
                        i12 = i13;
                    } else {
                        i8 = iSubParse;
                        i5 = i18;
                        calendar4 = calendar5;
                        i9 = i17;
                        objArr = objArr2;
                        timeZone2 = timeZone;
                    }
                } else {
                    int i19 = iMatchLiteral;
                    int i20 = i13;
                    i4 = -1;
                    Object[] objArr3 = patternItems;
                    zArr = zArr2;
                    output = output4;
                    output2 = output3;
                    i7 = i6;
                    Calendar calendar6 = calendar3;
                    int i21 = index;
                    if (patternItem.type != 'l') {
                        calendar4 = calendar6;
                        timeZone2 = timeZone;
                        int iSubParse2 = subParse(str, i19, patternItem.type, patternItem.length, false, true, zArr, calendar2, messageFormat, output, output2);
                        if (iSubParse2 >= 0) {
                            objArr = objArr3;
                            i5 = i21;
                            i8 = iSubParse2;
                        } else {
                            if (iSubParse2 != ISOSpecialEra) {
                                parsePosition.setIndex(i21);
                                parsePosition.setErrorIndex(i19);
                                if (timeZone2 != null) {
                                    this.calendar.setTimeZone(timeZone2);
                                    return;
                                }
                                return;
                            }
                            i9 = i20 + 1;
                            objArr = objArr3;
                            if (i9 < objArr.length) {
                                try {
                                    String str2 = (String) objArr[i9];
                                    if (str2 == null) {
                                        str2 = (String) objArr[i9];
                                    }
                                    int length = str2.length();
                                    int i22 = 0;
                                    while (i22 < length && PatternProps.isWhiteSpace(str2.charAt(i22))) {
                                        i22++;
                                    }
                                    if (i22 != length) {
                                        i9 = i20;
                                    }
                                    i8 = i19;
                                    i7 = -1;
                                    i5 = i21;
                                } catch (ClassCastException e) {
                                    parsePosition.setIndex(i21);
                                    parsePosition.setErrorIndex(i19);
                                    if (timeZone2 != null) {
                                        this.calendar.setTimeZone(timeZone2);
                                        return;
                                    }
                                    return;
                                }
                            } else {
                                i8 = i19;
                                i5 = i21;
                            }
                        }
                        i9 = i20;
                        i7 = -1;
                    } else {
                        i5 = i21;
                        calendar4 = calendar6;
                        i8 = i19;
                        objArr = objArr3;
                        timeZone2 = timeZone;
                        i9 = i20;
                    }
                }
                i3 = i9;
                iMatchLiteral = i8;
                i12 = i7;
                z = false;
            } else {
                i3 = i13;
                i4 = i11;
                zArr = zArr2;
                output = output4;
                output2 = output3;
                i5 = index;
                calendar4 = calendar3;
                timeZone2 = timeZone;
                objArr = patternItems;
                boolean[] zArr3 = new boolean[1];
                iMatchLiteral = matchLiteral(str, iMatchLiteral, patternItems, i3, zArr3);
                z = false;
                if (!zArr3[0]) {
                    parsePosition.setIndex(i5);
                    parsePosition.setErrorIndex(iMatchLiteral);
                    if (timeZone2 != null) {
                        this.calendar.setTimeZone(timeZone2);
                        return;
                    }
                    return;
                }
                i12 = i4;
            }
            i13 = i3 + 1;
            patternItems = objArr;
            index = i5;
            timeZone = timeZone2;
            i11 = i4;
            zArr2 = zArr;
            output4 = output;
            output3 = output2;
            calendar3 = calendar4;
        }
        boolean[] zArr4 = zArr2;
        Output<TimeZoneFormat.TimeType> output5 = output4;
        Output<DayPeriodRules.DayPeriod> output6 = output3;
        int i23 = index;
        Calendar calendar7 = calendar3;
        TimeZone timeZone4 = timeZone;
        boolean z2 = false;
        int i24 = iMatchLiteral;
        Object[] objArr4 = patternItems;
        if (i24 < str.length() && str.charAt(i24) == '.' && getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_ALLOW_WHITESPACE) && objArr4.length != 0) {
            Object obj = objArr4[objArr4.length - 1];
            if ((obj instanceof PatternItem) && !((PatternItem) obj).isNumeric) {
                i24++;
            }
        }
        if (output6.value != null) {
            DayPeriodRules dayPeriodRules = DayPeriodRules.getInstance(getLocale());
            if (calendar2.isSet(10) || calendar2.isSet(11)) {
                if (calendar2.isSet(11)) {
                    i2 = calendar2.get(11);
                } else {
                    i2 = calendar2.get(10);
                    if (i2 == 0) {
                        i2 = 12;
                    }
                }
                if (i2 == 0 || (13 <= i2 && i2 <= 23)) {
                    calendar2.set(11, i2);
                } else {
                    if (i2 == 12) {
                        i2 = 0;
                    }
                    double midPointForDayPeriod = (((double) i2) + (((double) calendar2.get(12)) / 60.0d)) - dayPeriodRules.getMidPointForDayPeriod(output6.value);
                    if (-6.0d > midPointForDayPeriod || midPointForDayPeriod >= 6.0d) {
                        calendar2.set(9, 1);
                    } else {
                        calendar2.set(9, 0);
                    }
                }
            } else {
                double midPointForDayPeriod2 = dayPeriodRules.getMidPointForDayPeriod(output6.value);
                int i25 = (int) midPointForDayPeriod2;
                int i26 = midPointForDayPeriod2 - ((double) i25) > 0.0d ? 30 : 0;
                calendar2.set(11, i25);
                calendar2.set(12, i26);
            }
        }
        parsePosition.setIndex(i24);
        try {
            TimeZoneFormat.TimeType timeType = output5.value;
            if (zArr4[0] || timeType != TimeZoneFormat.TimeType.UNKNOWN) {
                if (zArr4[0] && ((Calendar) calendar2.clone()).getTime().before(getDefaultCenturyStart())) {
                    calendar2.set(1, getDefaultCenturyStartYear() + 100);
                }
                if (timeType != TimeZoneFormat.TimeType.UNKNOWN) {
                    Calendar calendar8 = (Calendar) calendar2.clone();
                    TimeZone timeZone5 = calendar8.getTimeZone();
                    BasicTimeZone basicTimeZone = timeZone5 instanceof BasicTimeZone ? (BasicTimeZone) timeZone5 : null;
                    calendar8.set(15, 0);
                    calendar8.set(16, 0);
                    long timeInMillis = calendar8.getTimeInMillis();
                    int[] iArr = new int[2];
                    if (basicTimeZone == null) {
                        timeZone5.getOffset(timeInMillis, true, iArr);
                        if ((timeType == TimeZoneFormat.TimeType.STANDARD && iArr[1] != 0) || (timeType == TimeZoneFormat.TimeType.DAYLIGHT && iArr[1] == 0)) {
                            timeZone5.getOffset(timeInMillis - 86400000, true, iArr);
                        }
                    } else if (timeType == TimeZoneFormat.TimeType.STANDARD) {
                        basicTimeZone.getOffsetFromLocal(timeInMillis, 1, 1, iArr);
                    } else {
                        basicTimeZone.getOffsetFromLocal(timeInMillis, 3, 3, iArr);
                    }
                    int i27 = iArr[1];
                    if (timeType == TimeZoneFormat.TimeType.STANDARD) {
                        i = iArr[1] != 0 ? 0 : i27;
                        calendar2.set(15, iArr[0]);
                        calendar2.set(16, i);
                    } else {
                        if (iArr[1] == 0) {
                            if (basicTimeZone != null) {
                                long j = timeInMillis + ((long) iArr[0]);
                                dSTSavings = 0;
                                long time = j;
                                do {
                                    previousTransition = basicTimeZone.getPreviousTransition(time, true);
                                    if (previousTransition == null) {
                                        break;
                                    }
                                    time = previousTransition.getTime() - 1;
                                    dSTSavings = previousTransition.getFrom().getDSTSavings();
                                } while (dSTSavings == 0);
                                int i28 = 0;
                                long j2 = time;
                                long time2 = j;
                                while (true) {
                                    int i29 = i28;
                                    nextTransition = basicTimeZone.getNextTransition(time2, z2);
                                    if (nextTransition == null) {
                                        dSTSavings2 = i29;
                                        break;
                                    }
                                    time2 = nextTransition.getTime();
                                    dSTSavings2 = nextTransition.getTo().getDSTSavings();
                                    if (dSTSavings2 != 0) {
                                        break;
                                    }
                                    i28 = dSTSavings2;
                                    z2 = false;
                                }
                                if (previousTransition == null || nextTransition == null) {
                                    if (previousTransition == null || dSTSavings == 0) {
                                        dSTSavings = (nextTransition == null || dSTSavings2 == 0) ? basicTimeZone.getDSTSavings() : dSTSavings2;
                                    }
                                } else if (j - j2 > time2 - j) {
                                }
                            } else {
                                dSTSavings = timeZone5.getDSTSavings();
                            }
                            i = dSTSavings;
                            if (i == 0) {
                                i = 3600000;
                            }
                        }
                        calendar2.set(15, iArr[0]);
                        calendar2.set(16, i);
                    }
                }
            }
            if (calendar7 != null) {
                calendar7.setTimeZone(calendar2.getTimeZone());
                calendar7.setTimeInMillis(calendar2.getTimeInMillis());
            }
            if (timeZone4 != null) {
                this.calendar.setTimeZone(timeZone4);
            }
        } catch (IllegalArgumentException e2) {
            parsePosition.setErrorIndex(i24);
            parsePosition.setIndex(i23);
            if (timeZone4 != null) {
                this.calendar.setTimeZone(timeZone4);
            }
        }
    }

    private int matchLiteral(String str, int i, Object[] objArr, int i2, boolean[] zArr) {
        String str2 = (String) objArr[i2];
        int length = str2.length();
        int length2 = str.length();
        int i3 = i;
        int i4 = 0;
        while (true) {
            if (i4 >= length || i3 >= length2) {
                break;
            }
            char cCharAt = str2.charAt(i4);
            char cCharAt2 = str.charAt(i3);
            if (PatternProps.isWhiteSpace(cCharAt) && PatternProps.isWhiteSpace(cCharAt2)) {
                while (true) {
                    int i5 = i4 + 1;
                    if (i5 >= length || !PatternProps.isWhiteSpace(str2.charAt(i5))) {
                        break;
                    }
                    i4 = i5;
                }
                while (true) {
                    int i6 = i3 + 1;
                    if (i6 >= length2 || !PatternProps.isWhiteSpace(str.charAt(i6))) {
                        break;
                    }
                    i3 = i6;
                }
            } else if (cCharAt != cCharAt2) {
                if (cCharAt2 != '.' || i3 != i || i2 <= 0 || !getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_ALLOW_WHITESPACE)) {
                    if ((cCharAt == ' ' || cCharAt == '.') && getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_ALLOW_WHITESPACE)) {
                        i4++;
                    } else {
                        if (i3 == i || !getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_PARTIAL_LITERAL_MATCH)) {
                            break;
                        }
                        i4++;
                    }
                } else {
                    Object obj = objArr[i2 - 1];
                    if (!(obj instanceof PatternItem) || ((PatternItem) obj).isNumeric) {
                        break;
                    }
                    i3++;
                }
            }
            i4++;
            i3++;
        }
        zArr[0] = i4 == length;
        if (!zArr[0] && getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_ALLOW_WHITESPACE) && i2 > 0 && i2 < objArr.length - 1 && i < length2) {
            Object obj2 = objArr[i2 - 1];
            Object obj3 = objArr[i2 + 1];
            if ((obj2 instanceof PatternItem) && (obj3 instanceof PatternItem)) {
                if (DATE_PATTERN_TYPE.contains(((PatternItem) obj2).type) != DATE_PATTERN_TYPE.contains(((PatternItem) obj3).type)) {
                    i3 = i;
                    while (PatternProps.isWhiteSpace(str.charAt(i3))) {
                        i3++;
                    }
                    zArr[0] = i3 > i;
                }
            }
        }
        return i3;
    }

    protected int matchString(String str, int i, int i2, String[] strArr, Calendar calendar) {
        return matchString(str, i, i2, strArr, null, calendar);
    }

    @Deprecated
    private int matchString(String str, int i, int i2, String[] strArr, String str2, Calendar calendar) {
        int i3;
        int iRegionMatchesWithOptionalDot;
        String rawPattern;
        int length;
        int length2 = strArr.length;
        if (i2 != 7) {
            i3 = 0;
        } else {
            i3 = 1;
        }
        int i4 = 0;
        int i5 = -1;
        int iRegionMatchesWithOptionalDot2 = 0;
        while (i3 < length2) {
            int length3 = strArr[i3].length();
            if (length3 <= iRegionMatchesWithOptionalDot2 || (iRegionMatchesWithOptionalDot = regionMatchesWithOptionalDot(str, i, strArr[i3], length3)) < 0) {
                iRegionMatchesWithOptionalDot = iRegionMatchesWithOptionalDot2;
            } else {
                i4 = 0;
                i5 = i3;
            }
            if (str2 == null || (length = (rawPattern = SimpleFormatterImpl.formatRawPattern(str2, 1, 1, strArr[i3])).length()) <= iRegionMatchesWithOptionalDot || (iRegionMatchesWithOptionalDot2 = regionMatchesWithOptionalDot(str, i, rawPattern, length)) < 0) {
                iRegionMatchesWithOptionalDot2 = iRegionMatchesWithOptionalDot;
            } else {
                i4 = 1;
                i5 = i3;
            }
            i3++;
        }
        if (i5 >= 0) {
            if (i2 >= 0) {
                if (i2 == 1) {
                    i5++;
                }
                calendar.set(i2, i5);
                if (str2 != null) {
                    calendar.set(22, i4);
                }
            }
            return i + iRegionMatchesWithOptionalDot2;
        }
        return ~i;
    }

    private int regionMatchesWithOptionalDot(String str, int i, String str2, int i2) {
        if (str.regionMatches(true, i, str2, 0, i2)) {
            return i2;
        }
        if (str2.length() > 0 && str2.charAt(str2.length() - 1) == '.') {
            int i3 = i2 - 1;
            if (str.regionMatches(true, i, str2, 0, i3)) {
                return i3;
            }
            return -1;
        }
        return -1;
    }

    protected int matchQuarterString(String str, int i, int i2, String[] strArr, Calendar calendar) {
        int iRegionMatchesWithOptionalDot;
        int length = strArr.length;
        int i3 = -1;
        int i4 = 0;
        for (int i5 = 0; i5 < length; i5++) {
            int length2 = strArr[i5].length();
            if (length2 > i4 && (iRegionMatchesWithOptionalDot = regionMatchesWithOptionalDot(str, i, strArr[i5], length2)) >= 0) {
                i3 = i5;
                i4 = iRegionMatchesWithOptionalDot;
            }
        }
        if (i3 >= 0) {
            calendar.set(i2, i3 * 3);
            return i + i4;
        }
        return -i;
    }

    private int matchDayPeriodString(String str, int i, String[] strArr, int i2, Output<DayPeriodRules.DayPeriod> output) {
        int length;
        int iRegionMatchesWithOptionalDot;
        int i3 = -1;
        int i4 = 0;
        for (int i5 = 0; i5 < i2; i5++) {
            if (strArr[i5] != null && (length = strArr[i5].length()) > i4 && (iRegionMatchesWithOptionalDot = regionMatchesWithOptionalDot(str, i, strArr[i5], length)) >= 0) {
                i3 = i5;
                i4 = iRegionMatchesWithOptionalDot;
            }
        }
        if (i3 >= 0) {
            output.value = DayPeriodRules.DayPeriod.VALUES[i3];
            return i + i4;
        }
        return -i;
    }

    protected int subParse(String str, int i, char c, int i2, boolean z, boolean z2, boolean[] zArr, Calendar calendar) {
        return subParse(str, i, c, i2, z, z2, zArr, calendar, null, null);
    }

    private int subParse(String str, int i, char c, int i2, boolean z, boolean z2, boolean[] zArr, Calendar calendar, MessageFormat messageFormat, Output<TimeZoneFormat.TimeType> output) {
        return subParse(str, i, c, i2, z, z2, zArr, calendar, null, null, null);
    }

    @Deprecated
    private int subParse(String str, int i, char c, int i2, boolean z, boolean z2, boolean[] zArr, Calendar calendar, MessageFormat messageFormat, Output<TimeZoneFormat.TimeType> output, Output<DayPeriodRules.DayPeriod> output2) {
        int i3;
        Number number;
        boolean z3;
        int i4;
        boolean z4;
        int i5;
        int i6;
        NumberFormat numberFormat;
        Number number2;
        int iIntValue;
        Number number3;
        int iMatchString;
        int i7;
        int iMatchString2;
        int i8;
        int i9;
        int i10;
        int i11;
        int i12;
        int i13;
        int i14;
        int iMatchString3;
        int i15;
        int iMatchString4;
        int iMatchString5;
        int i16;
        int iMatchString6;
        int i17;
        int i18;
        TimeZoneFormat.Style style;
        int i19;
        TimeZoneFormat.Style style2;
        TimeZoneFormat.Style style3;
        int iMatchDayPeriodString;
        int i20;
        int iMatchDayPeriodString2;
        int i21;
        Number number4;
        ParsePosition parsePosition = new ParsePosition(0);
        int indexFromChar = getIndexFromChar(c);
        if (indexFromChar == -1) {
            return ~i;
        }
        NumberFormat numberFormat2 = getNumberFormat(c);
        int i22 = PATTERN_INDEX_TO_CALENDAR_FIELD[indexFromChar];
        if (messageFormat != null) {
            messageFormat.setFormatByArgumentIndex(0, numberFormat2);
        }
        boolean z5 = calendar.getType().equals("chinese") || calendar.getType().equals("dangi");
        int charCount = i;
        while (charCount < str.length()) {
            int iCharAt = UTF16.charAt(str, charCount);
            if (UCharacter.isUWhiteSpace(iCharAt) && PatternProps.isWhiteSpace(iCharAt)) {
                charCount += UTF16.getCharCount(iCharAt);
            } else {
                parsePosition.setIndex(charCount);
                if (indexFromChar == 4 || indexFromChar == 15 || ((indexFromChar == 2 && i2 <= 2) || indexFromChar == 26 || indexFromChar == 19 || indexFromChar == 25 || indexFromChar == 1 || indexFromChar == 18 || indexFromChar == 30 || ((indexFromChar == 0 && z5) || indexFromChar == 27 || indexFromChar == 28 || indexFromChar == 8))) {
                    if (messageFormat != null && (indexFromChar == 2 || indexFromChar == 26)) {
                        Object[] objArr = messageFormat.parse(str, parsePosition);
                        if (objArr == null || parsePosition.getIndex() <= charCount) {
                            i3 = i22;
                        } else {
                            i3 = i22;
                            if (objArr[0] instanceof Number) {
                                Number number5 = (Number) objArr[0];
                                calendar.set(22, 1);
                                number = number5;
                                z3 = true;
                                if (z3) {
                                    if (z) {
                                        if (charCount + i2 > str.length()) {
                                            return ~charCount;
                                        }
                                        z4 = z5;
                                        i6 = charCount;
                                        i5 = i3;
                                        i4 = indexFromChar;
                                        numberFormat = numberFormat2;
                                        number3 = parseInt(str, i2, parsePosition, z2, numberFormat);
                                    } else {
                                        i4 = indexFromChar;
                                        z4 = z5;
                                        i5 = i3;
                                        i6 = charCount;
                                        numberFormat = numberFormat2;
                                        number3 = parseInt(str, parsePosition, z2, numberFormat);
                                    }
                                    if (number3 == null && !allowNumericFallback(i4)) {
                                        return ~i6;
                                    }
                                    number = number3;
                                } else {
                                    i4 = indexFromChar;
                                    z4 = z5;
                                    i5 = i3;
                                    i6 = charCount;
                                    numberFormat = numberFormat2;
                                }
                                if (number == null) {
                                    iIntValue = number.intValue();
                                    number2 = number;
                                    switch (i4) {
                                        case 0:
                                            int i23 = iIntValue;
                                            int i24 = i6;
                                            if (z4) {
                                                calendar.set(0, i23);
                                                return parsePosition.getIndex();
                                            }
                                            if (i2 == 5) {
                                                iMatchString = matchString(str, i24, 0, this.formatData.narrowEras, null, calendar);
                                            } else if (i2 == 4) {
                                                iMatchString = matchString(str, i24, 0, this.formatData.eraNames, null, calendar);
                                            } else {
                                                iMatchString = matchString(str, i24, 0, this.formatData.eras, null, calendar);
                                            }
                                            if (iMatchString == (~i24)) {
                                                return ISOSpecialEra;
                                            }
                                            return iMatchString;
                                        case 1:
                                        case 18:
                                            int defaultCenturyStartYear = iIntValue;
                                            int i25 = i6;
                                            int i26 = i5;
                                            if (this.override != null && ((this.override.compareTo("hebr") == 0 || this.override.indexOf("y=hebr") >= 0) && defaultCenturyStartYear < 1000)) {
                                                defaultCenturyStartYear += HEBREW_CAL_CUR_MILLENIUM_START_YEAR;
                                            } else if (i2 == 2 && countDigits(str, i25, parsePosition.getIndex()) == 2 && calendar.haveDefaultCentury()) {
                                                int defaultCenturyStartYear2 = getDefaultCenturyStartYear() % 100;
                                                zArr[0] = defaultCenturyStartYear == defaultCenturyStartYear2;
                                                defaultCenturyStartYear += ((getDefaultCenturyStartYear() / 100) * 100) + (defaultCenturyStartYear >= defaultCenturyStartYear2 ? 0 : 100);
                                            }
                                            calendar.set(i26, defaultCenturyStartYear);
                                            if (DelayedHebrewMonthCheck) {
                                                if (!HebrewCalendar.isLeapYear(defaultCenturyStartYear)) {
                                                    calendar.add(2, 1);
                                                }
                                                DelayedHebrewMonthCheck = false;
                                            }
                                            return parsePosition.getIndex();
                                        case 2:
                                        case 26:
                                            int i27 = iIntValue;
                                            int i28 = i6;
                                            if (i2 <= 2 || (number2 != null && getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_ALLOW_NUMERIC))) {
                                                calendar.set(2, i27 - 1);
                                                if (calendar.getType().equals("hebrew") && i27 >= 6) {
                                                    if (calendar.isSet(1)) {
                                                        if (!HebrewCalendar.isLeapYear(calendar.get(1))) {
                                                            calendar.set(2, i27);
                                                        }
                                                    } else {
                                                        DelayedHebrewMonthCheck = true;
                                                    }
                                                }
                                                return parsePosition.getIndex();
                                            }
                                            boolean z6 = this.formatData.leapMonthPatterns != null && this.formatData.leapMonthPatterns.length >= 7;
                                            if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i2 == 4) {
                                                if (i4 == 2) {
                                                    i7 = 2;
                                                    iMatchString2 = matchString(str, i28, 2, this.formatData.months, z6 ? this.formatData.leapMonthPatterns[0] : null, calendar);
                                                } else {
                                                    i7 = 2;
                                                    iMatchString2 = matchString(str, i28, 2, this.formatData.standaloneMonths, z6 ? this.formatData.leapMonthPatterns[3] : null, calendar);
                                                }
                                                if (iMatchString2 > 0) {
                                                    return iMatchString2;
                                                }
                                                i8 = iMatchString2;
                                            } else {
                                                i7 = 2;
                                                i8 = 0;
                                            }
                                            if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i2 == 3) {
                                                if (i4 == i7) {
                                                    return matchString(str, i28, 2, this.formatData.shortMonths, z6 ? this.formatData.leapMonthPatterns[1] : null, calendar);
                                                }
                                                return matchString(str, i28, 2, this.formatData.standaloneShortMonths, z6 ? this.formatData.leapMonthPatterns[4] : null, calendar);
                                            }
                                            return i8;
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
                                            int i29 = i6;
                                            int i30 = i5;
                                            if (z) {
                                                if (i29 + i2 > str.length()) {
                                                    return -i29;
                                                }
                                                i21 = i30;
                                                number4 = parseInt(str, i2, parsePosition, z2, numberFormat);
                                            } else {
                                                i21 = i30;
                                                number4 = parseInt(str, parsePosition, z2, numberFormat);
                                            }
                                            if (number4 != null) {
                                                if (i4 != 34) {
                                                    calendar.set(i21, number4.intValue());
                                                } else {
                                                    calendar.setRelatedYear(number4.intValue());
                                                }
                                                return parsePosition.getIndex();
                                            }
                                            return ~i29;
                                        case 4:
                                            int i31 = iIntValue;
                                            if (i31 == calendar.getMaximum(11) + 1) {
                                                i31 = 0;
                                            }
                                            calendar.set(11, i31);
                                            return parsePosition.getIndex();
                                        case 8:
                                            int i32 = iIntValue;
                                            int i33 = 1;
                                            int iCountDigits = countDigits(str, i6, parsePosition.getIndex());
                                            if (iCountDigits < 3) {
                                                while (iCountDigits < 3) {
                                                    i32 *= 10;
                                                    iCountDigits++;
                                                }
                                            } else {
                                                while (iCountDigits > 3) {
                                                    i33 *= 10;
                                                    iCountDigits--;
                                                }
                                                i32 /= i33;
                                            }
                                            calendar.set(14, i32);
                                            return parsePosition.getIndex();
                                        case 9:
                                            i9 = 5;
                                            i10 = 6;
                                            i11 = i6;
                                            i12 = 4;
                                            i13 = 3;
                                            break;
                                        case 14:
                                            int i34 = i6;
                                            if (this.formatData.ampmsNarrow == null || i2 < 5 || getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH)) {
                                                i14 = 5;
                                                int iMatchString7 = matchString(str, i34, 9, this.formatData.ampms, null, calendar);
                                                if (iMatchString7 > 0) {
                                                    return iMatchString7;
                                                }
                                            } else {
                                                i14 = 5;
                                            }
                                            if (this.formatData.ampmsNarrow != null && ((i2 >= i14 || getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH)) && (iMatchString3 = matchString(str, i34, 9, this.formatData.ampmsNarrow, null, calendar)) > 0)) {
                                                return iMatchString3;
                                            }
                                            return ~i34;
                                        case 15:
                                            int i35 = iIntValue;
                                            if (i35 == calendar.getLeastMaximum(10) + 1) {
                                                i35 = 0;
                                            }
                                            calendar.set(10, i35);
                                            return parsePosition.getIndex();
                                        case 17:
                                            int i36 = i6;
                                            TimeZone timeZone = tzFormat().parse(i2 < 4 ? TimeZoneFormat.Style.SPECIFIC_SHORT : TimeZoneFormat.Style.SPECIFIC_LONG, str, parsePosition, output);
                                            if (timeZone != null) {
                                                calendar.setTimeZone(timeZone);
                                                return parsePosition.getIndex();
                                            }
                                            return ~i36;
                                        case 19:
                                            i10 = 6;
                                            int i37 = iIntValue;
                                            i11 = i6;
                                            int i38 = i5;
                                            i12 = 4;
                                            i13 = 3;
                                            if (i2 <= 2 || (number2 != null && getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_ALLOW_NUMERIC))) {
                                                calendar.set(i38, i37);
                                                return parsePosition.getIndex();
                                            }
                                            i9 = 5;
                                            break;
                                            break;
                                        case 23:
                                            int i39 = i6;
                                            TimeZone timeZone2 = tzFormat().parse(i2 < 4 ? TimeZoneFormat.Style.ISO_BASIC_LOCAL_FULL : i2 == 5 ? TimeZoneFormat.Style.ISO_EXTENDED_FULL : TimeZoneFormat.Style.LOCALIZED_GMT, str, parsePosition, output);
                                            if (timeZone2 != null) {
                                                calendar.setTimeZone(timeZone2);
                                                return parsePosition.getIndex();
                                            }
                                            return ~i39;
                                        case 24:
                                            int i40 = i6;
                                            TimeZone timeZone3 = tzFormat().parse(i2 < 4 ? TimeZoneFormat.Style.GENERIC_SHORT : TimeZoneFormat.Style.GENERIC_LONG, str, parsePosition, output);
                                            if (timeZone3 != null) {
                                                calendar.setTimeZone(timeZone3);
                                                return parsePosition.getIndex();
                                            }
                                            return ~i40;
                                        case 25:
                                            int i41 = iIntValue;
                                            int i42 = i6;
                                            if (i2 == 1 || (number2 != null && getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_ALLOW_NUMERIC))) {
                                                calendar.set(i5, i41);
                                                return parsePosition.getIndex();
                                            }
                                            if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i2 == 4) {
                                                i16 = 6;
                                                int iMatchString8 = matchString(str, i42, 7, this.formatData.standaloneWeekdays, null, calendar);
                                                if (iMatchString8 > 0) {
                                                    return iMatchString8;
                                                }
                                                iMatchString6 = iMatchString8;
                                            } else {
                                                i16 = 6;
                                                iMatchString6 = 0;
                                            }
                                            if ((getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i2 == 3) && (iMatchString6 = matchString(str, i42, 7, this.formatData.standaloneShortWeekdays, null, calendar)) > 0) {
                                                return iMatchString6;
                                            }
                                            if ((getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i2 == i16) && this.formatData.standaloneShorterWeekdays != null) {
                                                return matchString(str, i42, 7, this.formatData.standaloneShorterWeekdays, null, calendar);
                                            }
                                            return iMatchString6;
                                        case 27:
                                            int i43 = iIntValue;
                                            int i44 = i6;
                                            if (i2 <= 2 || (number2 != null && getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_ALLOW_NUMERIC))) {
                                                calendar.set(2, (i43 - 1) * 3);
                                                return parsePosition.getIndex();
                                            }
                                            if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i2 == 4) {
                                                int iMatchQuarterString = matchQuarterString(str, i44, 2, this.formatData.quarters, calendar);
                                                if (iMatchQuarterString > 0) {
                                                    return iMatchQuarterString;
                                                }
                                                i17 = iMatchQuarterString;
                                            } else {
                                                i17 = 0;
                                            }
                                            if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i2 == 3) {
                                                return matchQuarterString(str, i44, 2, this.formatData.shortQuarters, calendar);
                                            }
                                            return i17;
                                        case 28:
                                            int i45 = iIntValue;
                                            int i46 = i6;
                                            if (i2 <= 2 || (number2 != null && getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_ALLOW_NUMERIC))) {
                                                calendar.set(2, (i45 - 1) * 3);
                                                return parsePosition.getIndex();
                                            }
                                            if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i2 == 4) {
                                                int iMatchQuarterString2 = matchQuarterString(str, i46, 2, this.formatData.standaloneQuarters, calendar);
                                                if (iMatchQuarterString2 > 0) {
                                                    return iMatchQuarterString2;
                                                }
                                                i18 = iMatchQuarterString2;
                                            } else {
                                                i18 = 0;
                                            }
                                            if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i2 == 3) {
                                                return matchQuarterString(str, i46, 2, this.formatData.standaloneShortQuarters, calendar);
                                            }
                                            return i18;
                                        case 29:
                                            int i47 = i6;
                                            switch (i2) {
                                                case 1:
                                                    style = TimeZoneFormat.Style.ZONE_ID_SHORT;
                                                    break;
                                                case 2:
                                                    style = TimeZoneFormat.Style.ZONE_ID;
                                                    break;
                                                case 3:
                                                    style = TimeZoneFormat.Style.EXEMPLAR_LOCATION;
                                                    break;
                                                default:
                                                    style = TimeZoneFormat.Style.GENERIC_LOCATION;
                                                    break;
                                            }
                                            TimeZone timeZone4 = tzFormat().parse(style, str, parsePosition, output);
                                            if (timeZone4 != null) {
                                                calendar.setTimeZone(timeZone4);
                                                return parsePosition.getIndex();
                                            }
                                            return ~i47;
                                        case 30:
                                            int i48 = i6;
                                            if (this.formatData.shortYearNames != null) {
                                                i19 = iIntValue;
                                                int iMatchString9 = matchString(str, i48, 1, this.formatData.shortYearNames, null, calendar);
                                                if (iMatchString9 > 0) {
                                                    return iMatchString9;
                                                }
                                            } else {
                                                i19 = iIntValue;
                                            }
                                            if (number2 != null && (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_ALLOW_NUMERIC) || this.formatData.shortYearNames == null || i19 > this.formatData.shortYearNames.length)) {
                                                calendar.set(1, i19);
                                                return parsePosition.getIndex();
                                            }
                                            return ~i48;
                                        case 31:
                                            int i49 = i6;
                                            TimeZone timeZone5 = tzFormat().parse(i2 < 4 ? TimeZoneFormat.Style.LOCALIZED_GMT_SHORT : TimeZoneFormat.Style.LOCALIZED_GMT, str, parsePosition, output);
                                            if (timeZone5 != null) {
                                                calendar.setTimeZone(timeZone5);
                                                return parsePosition.getIndex();
                                            }
                                            return ~i49;
                                        case 32:
                                            int i50 = i6;
                                            switch (i2) {
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
                                            TimeZone timeZone6 = tzFormat().parse(style2, str, parsePosition, output);
                                            if (timeZone6 != null) {
                                                calendar.setTimeZone(timeZone6);
                                                return parsePosition.getIndex();
                                            }
                                            return ~i50;
                                        case 33:
                                            int i51 = i6;
                                            switch (i2) {
                                                case 1:
                                                    style3 = TimeZoneFormat.Style.ISO_BASIC_LOCAL_SHORT;
                                                    break;
                                                case 2:
                                                    style3 = TimeZoneFormat.Style.ISO_BASIC_LOCAL_FIXED;
                                                    break;
                                                case 3:
                                                    style3 = TimeZoneFormat.Style.ISO_EXTENDED_LOCAL_FIXED;
                                                    break;
                                                case 4:
                                                    style3 = TimeZoneFormat.Style.ISO_BASIC_LOCAL_FULL;
                                                    break;
                                                default:
                                                    style3 = TimeZoneFormat.Style.ISO_EXTENDED_LOCAL_FULL;
                                                    break;
                                            }
                                            TimeZone timeZone7 = tzFormat().parse(style3, str, parsePosition, output);
                                            if (timeZone7 != null) {
                                                calendar.setTimeZone(timeZone7);
                                                return parsePosition.getIndex();
                                            }
                                            return ~i51;
                                        case 35:
                                            int i52 = i6;
                                            int iSubParse = subParse(str, i6, 'a', i2, z, z2, zArr, calendar, messageFormat, output, output2);
                                            if (iSubParse > 0) {
                                                return iSubParse;
                                            }
                                            if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i2 == 3) {
                                                iMatchDayPeriodString = matchDayPeriodString(str, i52, this.formatData.abbreviatedDayPeriods, 2, output2);
                                                if (iMatchDayPeriodString > 0) {
                                                    return iMatchDayPeriodString;
                                                }
                                            } else {
                                                iMatchDayPeriodString = 0;
                                            }
                                            if (!getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH)) {
                                                i20 = 4;
                                                if (i2 == 4) {
                                                }
                                                if ((getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) && i2 != i20) || (iMatchDayPeriodString = matchDayPeriodString(str, i52, this.formatData.narrowDayPeriods, 2, output2)) <= 0) {
                                                    return iMatchDayPeriodString;
                                                }
                                            }
                                            i20 = 4;
                                            iMatchDayPeriodString = matchDayPeriodString(str, i52, this.formatData.wideDayPeriods, 2, output2);
                                            if (iMatchDayPeriodString > 0) {
                                                return iMatchDayPeriodString;
                                            }
                                            return getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) ? iMatchDayPeriodString : iMatchDayPeriodString;
                                            return iMatchDayPeriodString;
                                        case 36:
                                            if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i2 == 3) {
                                                int iMatchDayPeriodString3 = matchDayPeriodString(str, i6, this.formatData.abbreviatedDayPeriods, this.formatData.abbreviatedDayPeriods.length, output2);
                                                if (iMatchDayPeriodString3 > 0) {
                                                    return iMatchDayPeriodString3;
                                                }
                                                iMatchDayPeriodString2 = iMatchDayPeriodString3;
                                            } else {
                                                iMatchDayPeriodString2 = 0;
                                            }
                                            if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i2 == 4) {
                                                iMatchDayPeriodString2 = matchDayPeriodString(str, i6, this.formatData.wideDayPeriods, this.formatData.wideDayPeriods.length, output2);
                                                if (iMatchDayPeriodString2 > 0) {
                                                    return iMatchDayPeriodString2;
                                                }
                                            }
                                            if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i2 == 4) {
                                                iMatchDayPeriodString2 = matchDayPeriodString(str, i6, this.formatData.narrowDayPeriods, this.formatData.narrowDayPeriods.length, output2);
                                                if (iMatchDayPeriodString2 > 0) {
                                                    return iMatchDayPeriodString2;
                                                }
                                            }
                                            return iMatchDayPeriodString2;
                                        case 37:
                                            ArrayList arrayList = new ArrayList(3);
                                            arrayList.add(this.formatData.getTimeSeparatorString());
                                            if (!this.formatData.getTimeSeparatorString().equals(":")) {
                                                arrayList.add(":");
                                            }
                                            if (getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_PARTIAL_LITERAL_MATCH) && !this.formatData.getTimeSeparatorString().equals(".")) {
                                                arrayList.add(".");
                                            }
                                            return matchString(str, i6, -1, (String[]) arrayList.toArray(new String[0]), calendar);
                                    }
                                    if (!getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i2 == i12) {
                                        i15 = i10;
                                        iMatchString4 = matchString(str, i11, 7, this.formatData.weekdays, null, calendar);
                                        if (iMatchString4 <= 0) {
                                            return iMatchString4;
                                        }
                                        iMatchString5 = iMatchString4;
                                    } else {
                                        i15 = i10;
                                        iMatchString5 = 0;
                                    }
                                    if (!getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i2 == i13) {
                                        iMatchString5 = matchString(str, i11, 7, this.formatData.shortWeekdays, null, calendar);
                                        if (iMatchString5 > 0) {
                                            return iMatchString5;
                                        }
                                    }
                                    if ((!getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i2 == i15) && this.formatData.shorterWeekdays != null) {
                                        iMatchString5 = matchString(str, i11, 7, this.formatData.shorterWeekdays, null, calendar);
                                        if (iMatchString5 > 0) {
                                            return iMatchString5;
                                        }
                                    }
                                    if ((!getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i2 == i9) && this.formatData.narrowWeekdays != null) {
                                        iMatchString5 = matchString(str, i11, 7, this.formatData.narrowWeekdays, null, calendar);
                                        if (iMatchString5 > 0) {
                                            return iMatchString5;
                                        }
                                    }
                                    return iMatchString5;
                                }
                                number2 = number;
                            }
                        }
                        parsePosition.setIndex(charCount);
                        calendar.set(22, 0);
                    } else {
                        i3 = i22;
                    }
                    number = null;
                    z3 = false;
                    if (z3) {
                    }
                    if (number == null) {
                    }
                } else {
                    i5 = i22;
                    i4 = indexFromChar;
                    z4 = z5;
                    number2 = null;
                    i6 = charCount;
                    numberFormat = numberFormat2;
                }
                iIntValue = 0;
                switch (i4) {
                }
                if (!getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH)) {
                    i15 = i10;
                    iMatchString4 = matchString(str, i11, 7, this.formatData.weekdays, null, calendar);
                    if (iMatchString4 <= 0) {
                    }
                }
                if (!getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH)) {
                    iMatchString5 = matchString(str, i11, 7, this.formatData.shortWeekdays, null, calendar);
                    if (iMatchString5 > 0) {
                    }
                }
                if (!getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH)) {
                    iMatchString5 = matchString(str, i11, 7, this.formatData.shorterWeekdays, null, calendar);
                    if (iMatchString5 > 0) {
                    }
                } else {
                    iMatchString5 = matchString(str, i11, 7, this.formatData.shorterWeekdays, null, calendar);
                    if (iMatchString5 > 0) {
                    }
                }
                if (!getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH)) {
                    iMatchString5 = matchString(str, i11, 7, this.formatData.narrowWeekdays, null, calendar);
                    if (iMatchString5 > 0) {
                    }
                } else {
                    iMatchString5 = matchString(str, i11, 7, this.formatData.narrowWeekdays, null, calendar);
                    if (iMatchString5 > 0) {
                    }
                }
                return iMatchString5;
            }
        }
        return ~charCount;
    }

    private boolean allowNumericFallback(int i) {
        if (i == 26 || i == 19 || i == 25 || i == 30 || i == 27 || i == 28) {
            return true;
        }
        return false;
    }

    private Number parseInt(String str, ParsePosition parsePosition, boolean z, NumberFormat numberFormat) {
        return parseInt(str, -1, parsePosition, z, numberFormat);
    }

    private Number parseInt(String str, int i, ParsePosition parsePosition, boolean z, NumberFormat numberFormat) {
        Number number;
        int index;
        int index2 = parsePosition.getIndex();
        if (z) {
            number = numberFormat.parse(str, parsePosition);
        } else if (numberFormat instanceof DecimalFormat) {
            DecimalFormat decimalFormat = (DecimalFormat) numberFormat;
            String negativePrefix = decimalFormat.getNegativePrefix();
            decimalFormat.setNegativePrefix(SUPPRESS_NEGATIVE_PREFIX);
            number = numberFormat.parse(str, parsePosition);
            decimalFormat.setNegativePrefix(negativePrefix);
        } else {
            boolean z2 = numberFormat instanceof DateNumberFormat;
            if (z2) {
                ((DateNumberFormat) numberFormat).setParsePositiveOnly(true);
            }
            number = numberFormat.parse(str, parsePosition);
            if (z2) {
                ((DateNumberFormat) numberFormat).setParsePositiveOnly(false);
            }
        }
        if (i > 0 && (index = parsePosition.getIndex() - index2) > i) {
            double dDoubleValue = number.doubleValue();
            for (int i2 = index - i; i2 > 0; i2--) {
                dDoubleValue /= 10.0d;
            }
            parsePosition.setIndex(index2 + i);
            return Integer.valueOf((int) dDoubleValue);
        }
        return number;
    }

    private static int countDigits(String str, int i, int i2) {
        int i3 = 0;
        while (i < i2) {
            int iCodePointAt = str.codePointAt(i);
            if (UCharacter.isDigit(iCodePointAt)) {
                i3++;
            }
            i += UCharacter.charCount(iCodePointAt);
        }
        return i3;
    }

    private String translatePattern(String str, String str2, String str3) {
        int iIndexOf;
        StringBuilder sb = new StringBuilder();
        boolean z = false;
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (z) {
                if (cCharAt == '\'') {
                    z = false;
                }
            } else if (cCharAt != '\'') {
                if (isSyntaxChar(cCharAt) && (iIndexOf = str2.indexOf(cCharAt)) != -1) {
                    cCharAt = str3.charAt(iIndexOf);
                }
            } else {
                z = true;
            }
            sb.append(cCharAt);
        }
        if (z) {
            throw new IllegalArgumentException("Unfinished quote in pattern");
        }
        return sb.toString();
    }

    public String toPattern() {
        return this.pattern;
    }

    public String toLocalizedPattern() {
        return translatePattern(this.pattern, "GyMdkHmsSEDFwWahKzYeugAZvcLQqVUOXxrbB", this.formatData.localPatternChars);
    }

    public void applyPattern(String str) {
        this.pattern = str;
        parsePattern();
        setLocale(null, null);
        this.patternItems = null;
    }

    public void applyLocalizedPattern(String str) {
        this.pattern = translatePattern(str, this.formatData.localPatternChars, "GyMdkHmsSEDFwWahKzYeugAZvcLQqVUOXxrbB");
        setLocale(null, null);
    }

    public DateFormatSymbols getDateFormatSymbols() {
        return (DateFormatSymbols) this.formatData.clone();
    }

    public void setDateFormatSymbols(DateFormatSymbols dateFormatSymbols) {
        this.formatData = (DateFormatSymbols) dateFormatSymbols.clone();
    }

    protected DateFormatSymbols getSymbols() {
        return this.formatData;
    }

    public TimeZoneFormat getTimeZoneFormat() {
        return tzFormat().freeze();
    }

    public void setTimeZoneFormat(TimeZoneFormat timeZoneFormat) {
        if (timeZoneFormat.isFrozen()) {
            this.tzFormat = timeZoneFormat;
        } else {
            this.tzFormat = timeZoneFormat.cloneAsThawed().freeze();
        }
    }

    @Override
    public Object clone() {
        SimpleDateFormat simpleDateFormat = (SimpleDateFormat) super.clone();
        simpleDateFormat.formatData = (DateFormatSymbols) this.formatData.clone();
        if (this.decimalBuf != null) {
            simpleDateFormat.decimalBuf = new char[10];
        }
        return simpleDateFormat;
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
        SimpleDateFormat simpleDateFormat = (SimpleDateFormat) obj;
        return this.pattern.equals(simpleDateFormat.pattern) && this.formatData.equals(simpleDateFormat.formatData);
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        if (this.defaultCenturyStart == null) {
            initializeDefaultCenturyStart(this.defaultCenturyBase);
        }
        initializeTimeZoneFormat(false);
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeInt(getContext(DisplayContext.Type.CAPITALIZATION).value());
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        int i = this.serialVersionOnStream > 1 ? objectInputStream.readInt() : -1;
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
        if (i >= 0) {
            DisplayContext[] displayContextArrValues = DisplayContext.values();
            int length = displayContextArrValues.length;
            int i2 = 0;
            while (true) {
                if (i2 >= length) {
                    break;
                }
                DisplayContext displayContext = displayContextArrValues[i2];
                if (displayContext.value() != i) {
                    i2++;
                } else {
                    setContext(displayContext);
                    break;
                }
            }
        }
        if (!getBooleanAttribute(DateFormat.BooleanAttribute.PARSE_PARTIAL_MATCH)) {
            setBooleanAttribute(DateFormat.BooleanAttribute.PARSE_PARTIAL_LITERAL_MATCH, false);
        }
        parsePattern();
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        Calendar calendar = this.calendar;
        if (obj instanceof Calendar) {
            calendar = (Calendar) obj;
        } else if (obj instanceof Date) {
            this.calendar.setTime((Date) obj);
        } else if (obj instanceof Number) {
            this.calendar.setTimeInMillis(((Number) obj).longValue());
        } else {
            throw new IllegalArgumentException("Cannot format given Object as a Date");
        }
        Calendar calendar2 = calendar;
        StringBuffer stringBuffer = new StringBuffer();
        FieldPosition fieldPosition = new FieldPosition(0);
        ArrayList arrayList = new ArrayList();
        format(calendar2, getContext(DisplayContext.Type.CAPITALIZATION), stringBuffer, fieldPosition, arrayList);
        AttributedString attributedString = new AttributedString(stringBuffer.toString());
        for (int i = 0; i < arrayList.size(); i++) {
            FieldPosition fieldPosition2 = arrayList.get(i);
            Format.Field fieldAttribute = fieldPosition2.getFieldAttribute();
            attributedString.addAttribute(fieldAttribute, fieldAttribute, fieldPosition2.getBeginIndex(), fieldPosition2.getEndIndex());
        }
        return attributedString.getIterator();
    }

    ULocale getLocale() {
        return this.locale;
    }

    boolean isFieldUnitIgnored(int i) {
        return isFieldUnitIgnored(this.pattern, i);
    }

    static boolean isFieldUnitIgnored(String str, int i) {
        int i2 = CALENDAR_FIELD_TO_LEVEL[i];
        int i3 = 0;
        char c = 0;
        int i4 = 0;
        boolean z = false;
        while (i3 < str.length()) {
            char cCharAt = str.charAt(i3);
            if (cCharAt != c && i4 > 0) {
                if (i2 <= getLevelFromChar(c)) {
                    return false;
                }
                i4 = 0;
            }
            if (cCharAt == '\'') {
                int i5 = i3 + 1;
                if (i5 >= str.length() || str.charAt(i5) != '\'') {
                    z = !z;
                } else {
                    i3 = i5;
                }
            } else if (!z && isSyntaxChar(cCharAt)) {
                i4++;
                c = cCharAt;
            }
            i3++;
        }
        return i4 <= 0 || i2 > getLevelFromChar(c);
    }

    @Deprecated
    public final StringBuffer intervalFormatByAlgorithm(Calendar calendar, Calendar calendar2, StringBuffer stringBuffer, FieldPosition fieldPosition) throws IllegalArgumentException {
        int i;
        int i2;
        int i3;
        int i4;
        if (!calendar.isEquivalentTo(calendar2)) {
            throw new IllegalArgumentException("can not format on two different calendars");
        }
        Object[] patternItems = getPatternItems();
        int i5 = 0;
        while (true) {
            try {
                if (i5 < patternItems.length) {
                    if (diffCalFieldValue(calendar, calendar2, patternItems, i5)) {
                        break;
                    }
                    i5++;
                } else {
                    i5 = -1;
                    break;
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(e.toString());
            }
        }
        if (i5 == -1) {
            return format(calendar, stringBuffer, fieldPosition);
        }
        int length = patternItems.length - 1;
        while (true) {
            if (length >= i5) {
                if (diffCalFieldValue(calendar, calendar2, patternItems, length)) {
                    break;
                }
                length--;
            } else {
                length = -1;
                break;
            }
        }
        if (i5 == 0 && length == patternItems.length - 1) {
            format(calendar, stringBuffer, fieldPosition);
            stringBuffer.append(" – ");
            format(calendar2, stringBuffer, fieldPosition);
            return stringBuffer;
        }
        int i6 = 1000;
        for (int i7 = i5; i7 <= length; i7++) {
            if (!(patternItems[i7] instanceof String)) {
                char c = ((PatternItem) patternItems[i7]).type;
                int indexFromChar = getIndexFromChar(c);
                if (indexFromChar == -1) {
                    throw new IllegalArgumentException("Illegal pattern character '" + c + "' in \"" + this.pattern + '\"');
                }
                if (indexFromChar < i6) {
                    i6 = indexFromChar;
                }
            }
        }
        int i8 = 0;
        while (true) {
            if (i8 < i5) {
                try {
                    if (!lowerLevel(patternItems, i8, i6)) {
                        i8++;
                    } else {
                        i = i8;
                        break;
                    }
                } catch (IllegalArgumentException e2) {
                    throw new IllegalArgumentException(e2.toString());
                }
            } else {
                i = i5;
                break;
            }
        }
        int length2 = patternItems.length - 1;
        while (true) {
            if (length2 > length) {
                if (lowerLevel(patternItems, length2, i6)) {
                    i2 = length2;
                    break;
                }
                length2--;
            } else {
                i2 = length;
                break;
            }
        }
        if (i == 0 && i2 == patternItems.length - 1) {
            format(calendar, stringBuffer, fieldPosition);
            stringBuffer.append(" – ");
            format(calendar2, stringBuffer, fieldPosition);
            return stringBuffer;
        }
        fieldPosition.setBeginIndex(0);
        fieldPosition.setEndIndex(0);
        DisplayContext context = getContext(DisplayContext.Type.CAPITALIZATION);
        int i9 = 0;
        while (i9 <= i2) {
            if (patternItems[i9] instanceof String) {
                stringBuffer.append((String) patternItems[i9]);
                i3 = i9;
                i4 = i2;
            } else {
                PatternItem patternItem = (PatternItem) patternItems[i9];
                if (this.useFastFormat) {
                    i3 = i9;
                    i4 = i2;
                    subFormat(stringBuffer, patternItem.type, patternItem.length, stringBuffer.length(), i9, context, fieldPosition, calendar);
                } else {
                    i3 = i9;
                    i4 = i2;
                    stringBuffer.append(subFormat(patternItem.type, patternItem.length, stringBuffer.length(), i3, context, fieldPosition, calendar));
                }
            }
            i9 = i3 + 1;
            i2 = i4;
        }
        stringBuffer.append(" – ");
        while (i < patternItems.length) {
            if (patternItems[i] instanceof String) {
                stringBuffer.append((String) patternItems[i]);
            } else {
                PatternItem patternItem2 = (PatternItem) patternItems[i];
                if (this.useFastFormat) {
                    subFormat(stringBuffer, patternItem2.type, patternItem2.length, stringBuffer.length(), i, context, fieldPosition, calendar2);
                } else {
                    stringBuffer.append(subFormat(patternItem2.type, patternItem2.length, stringBuffer.length(), i, context, fieldPosition, calendar2));
                }
            }
            i++;
        }
        return stringBuffer;
    }

    private boolean diffCalFieldValue(Calendar calendar, Calendar calendar2, Object[] objArr, int i) throws IllegalArgumentException {
        if (objArr[i] instanceof String) {
            return false;
        }
        char c = ((PatternItem) objArr[i]).type;
        int indexFromChar = getIndexFromChar(c);
        if (indexFromChar == -1) {
            throw new IllegalArgumentException("Illegal pattern character '" + c + "' in \"" + this.pattern + '\"');
        }
        int i2 = PATTERN_INDEX_TO_CALENDAR_FIELD[indexFromChar];
        return i2 >= 0 && calendar.get(i2) != calendar2.get(i2);
    }

    private boolean lowerLevel(Object[] objArr, int i, int i2) throws IllegalArgumentException {
        if (objArr[i] instanceof String) {
            return false;
        }
        char c = ((PatternItem) objArr[i]).type;
        int levelFromChar = getLevelFromChar(c);
        if (levelFromChar != -1) {
            return levelFromChar >= i2;
        }
        throw new IllegalArgumentException("Illegal pattern character '" + c + "' in \"" + this.pattern + '\"');
    }

    public void setNumberFormat(String str, NumberFormat numberFormat) {
        numberFormat.setGroupingUsed(false);
        String str2 = "$" + UUID.randomUUID().toString();
        if (this.numberFormatters == null) {
            this.numberFormatters = new HashMap<>();
        }
        if (this.overrideMap == null) {
            this.overrideMap = new HashMap<>();
        }
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if ("GyMdkHmsSEDFwWahKzYeugAZvcLQqVUOXxrbB".indexOf(cCharAt) == -1) {
                throw new IllegalArgumentException("Illegal field character '" + cCharAt + "' in setNumberFormat.");
            }
            this.overrideMap.put(Character.valueOf(cCharAt), str2);
            this.numberFormatters.put(str2, numberFormat);
        }
        this.useLocalZeroPaddingNumberFormat = false;
    }

    public NumberFormat getNumberFormat(char c) {
        Character chValueOf = Character.valueOf(c);
        if (this.overrideMap != null && this.overrideMap.containsKey(chValueOf)) {
            return this.numberFormatters.get(this.overrideMap.get(chValueOf).toString());
        }
        return this.numberFormat;
    }

    private void initNumberFormatters(ULocale uLocale) {
        this.numberFormatters = new HashMap<>();
        this.overrideMap = new HashMap<>();
        processOverrideString(uLocale, this.override);
    }

    private void processOverrideString(ULocale uLocale, String str) {
        boolean z;
        int length;
        boolean z2;
        if (str == null || str.length() == 0) {
            return;
        }
        boolean z3 = true;
        int i = 0;
        while (z3) {
            int iIndexOf = str.indexOf(";", i);
            if (iIndexOf == -1) {
                length = str.length();
                z = false;
            } else {
                z = z3;
                length = iIndexOf;
            }
            String strSubstring = str.substring(i, length);
            int iIndexOf2 = strSubstring.indexOf("=");
            if (iIndexOf2 != -1) {
                String strSubstring2 = strSubstring.substring(iIndexOf2 + 1);
                this.overrideMap.put(Character.valueOf(strSubstring.charAt(0)), strSubstring2);
                strSubstring = strSubstring2;
                z2 = false;
            } else {
                z2 = true;
            }
            NumberFormat numberFormatCreateInstance = NumberFormat.createInstance(new ULocale(uLocale.getBaseName() + "@numbers=" + strSubstring), 0);
            numberFormatCreateInstance.setGroupingUsed(false);
            if (z2) {
                setNumberFormat(numberFormatCreateInstance);
            } else {
                this.useLocalZeroPaddingNumberFormat = false;
            }
            if (!z2 && !this.numberFormatters.containsKey(strSubstring)) {
                this.numberFormatters.put(strSubstring, numberFormatCreateInstance);
            }
            i = iIndexOf + 1;
            z3 = z;
        }
    }

    private void parsePattern() {
        this.hasMinute = false;
        this.hasSecond = false;
        boolean z = false;
        for (int i = 0; i < this.pattern.length(); i++) {
            char cCharAt = this.pattern.charAt(i);
            if (cCharAt == '\'') {
                z = !z;
            }
            if (!z) {
                if (cCharAt == 'm') {
                    this.hasMinute = true;
                }
                if (cCharAt == 's') {
                    this.hasSecond = true;
                }
            }
        }
    }
}
