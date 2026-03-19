package android.icu.text;

import android.icu.impl.ICUCache;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.PatternTokenizer;
import android.icu.impl.SimpleCache;
import android.icu.impl.SimpleFormatterImpl;
import android.icu.impl.UResource;
import android.icu.impl.locale.BaseLocale;
import android.icu.impl.number.Padder;
import android.icu.lang.UCharacter;
import android.icu.util.Calendar;
import android.icu.util.Freezable;
import android.icu.util.ICUCloneNotSupportedException;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class DateTimePatternGenerator implements Freezable<DateTimePatternGenerator>, Cloneable {
    private static final String[] CANONICAL_ITEMS;
    private static final Set<String> CANONICAL_SET;
    private static final String[] CLDR_FIELD_APPEND;
    private static final String[] CLDR_FIELD_NAME;
    private static final int DATE_MASK = 1023;
    public static final int DAY = 7;
    public static final int DAYPERIOD = 10;
    public static final int DAY_OF_WEEK_IN_MONTH = 9;
    public static final int DAY_OF_YEAR = 8;
    private static final boolean DEBUG = false;
    private static final int DELTA = 16;
    private static ICUCache<String, DateTimePatternGenerator> DTPNG_CACHE = null;
    public static final int ERA = 0;
    private static final int EXTRA_FIELD = 65536;
    private static final String[] FIELD_NAME;
    private static final int FRACTIONAL_MASK = 16384;
    public static final int FRACTIONAL_SECOND = 14;
    public static final int HOUR = 11;
    private static final String[] LAST_RESORT_ALLOWED_HOUR_FORMAT = {DateFormat.HOUR24};
    static final Map<String, String[]> LOCALE_TO_ALLOWED_HOUR;
    private static final int LONG = -260;
    public static final int MATCH_ALL_FIELDS_LENGTH = 65535;
    public static final int MATCH_HOUR_FIELD_LENGTH = 2048;

    @Deprecated
    public static final int MATCH_MINUTE_FIELD_LENGTH = 4096;
    public static final int MATCH_NO_OPTIONS = 0;

    @Deprecated
    public static final int MATCH_SECOND_FIELD_LENGTH = 8192;
    public static final int MINUTE = 12;
    private static final int MISSING_FIELD = 4096;
    public static final int MONTH = 3;
    private static final int NARROW = -257;
    private static final int NONE = 0;
    private static final int NUMERIC = 256;
    public static final int QUARTER = 2;
    public static final int SECOND = 13;
    private static final int SECOND_AND_FRACTIONAL_MASK = 24576;
    private static final int SHORT = -259;
    private static final int SHORTER = -258;
    private static final int TIME_MASK = 64512;

    @Deprecated
    public static final int TYPE_LIMIT = 16;
    public static final int WEEKDAY = 6;
    public static final int WEEK_OF_MONTH = 5;
    public static final int WEEK_OF_YEAR = 4;
    public static final int YEAR = 1;
    public static final int ZONE = 15;
    private static final int[][] types;
    private transient DistanceInfo _distanceInfo;
    private String[] allowedHourFormats;
    private transient DateTimeMatcher current;
    private TreeMap<DateTimeMatcher, PatternWithSkeletonFlag> skeleton2pattern = new TreeMap<>();
    private TreeMap<String, PatternWithSkeletonFlag> basePattern_pattern = new TreeMap<>();
    private String decimal = "?";
    private String dateTimeFormat = "{1} {0}";
    private String[] appendItemFormats = new String[16];
    private String[] appendItemNames = new String[16];
    private char defaultHourFormatChar = 'H';
    private volatile boolean frozen = false;
    private transient FormatParser fp = new FormatParser();
    private Set<String> cldrAvailableFormatKeys = new HashSet(20);

    private enum DTPGflags {
        FIX_FRACTIONAL_SECONDS,
        SKELETON_USES_CAP_J
    }

    public static final class PatternInfo {
        public static final int BASE_CONFLICT = 1;
        public static final int CONFLICT = 2;
        public static final int OK = 0;
        public String conflictingPattern;
        public int status;
    }

    public static DateTimePatternGenerator getEmptyInstance() {
        DateTimePatternGenerator dateTimePatternGenerator = new DateTimePatternGenerator();
        dateTimePatternGenerator.addCanonicalItems();
        dateTimePatternGenerator.fillInMissing();
        return dateTimePatternGenerator;
    }

    protected DateTimePatternGenerator() {
        this.current = new DateTimeMatcher();
        this._distanceInfo = new DistanceInfo();
    }

    public static DateTimePatternGenerator getInstance() {
        return getInstance(ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public static DateTimePatternGenerator getInstance(ULocale uLocale) {
        return getFrozenInstance(uLocale).cloneAsThawed();
    }

    public static DateTimePatternGenerator getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale));
    }

    @Deprecated
    public static DateTimePatternGenerator getFrozenInstance(ULocale uLocale) {
        String string = uLocale.toString();
        DateTimePatternGenerator dateTimePatternGenerator = DTPNG_CACHE.get(string);
        if (dateTimePatternGenerator != null) {
            return dateTimePatternGenerator;
        }
        DateTimePatternGenerator dateTimePatternGenerator2 = new DateTimePatternGenerator();
        dateTimePatternGenerator2.initData(uLocale);
        dateTimePatternGenerator2.freeze();
        DTPNG_CACHE.put(string, dateTimePatternGenerator2);
        return dateTimePatternGenerator2;
    }

    private void initData(ULocale uLocale) {
        PatternInfo patternInfo = new PatternInfo();
        addCanonicalItems();
        addICUPatterns(patternInfo, uLocale);
        addCLDRData(patternInfo, uLocale);
        setDateTimeFromCalendar(uLocale);
        setDecimalSymbols(uLocale);
        getAllowedHourFormats(uLocale);
        fillInMissing();
    }

    private void addICUPatterns(PatternInfo patternInfo, ULocale uLocale) {
        for (int i = 0; i <= 3; i++) {
            addPattern(((SimpleDateFormat) DateFormat.getDateInstance(i, uLocale)).toPattern(), false, patternInfo);
            SimpleDateFormat simpleDateFormat = (SimpleDateFormat) DateFormat.getTimeInstance(i, uLocale);
            addPattern(simpleDateFormat.toPattern(), false, patternInfo);
            if (i == 3) {
                consumeShortTimePattern(simpleDateFormat.toPattern(), patternInfo);
            }
        }
    }

    private String getCalendarTypeToUse(ULocale uLocale) {
        String keywordValue = uLocale.getKeywordValue("calendar");
        if (keywordValue == null) {
            keywordValue = Calendar.getKeywordValuesForLocale("calendar", uLocale, true)[0];
        }
        if (keywordValue == null) {
            return "gregorian";
        }
        return keywordValue;
    }

    private void consumeShortTimePattern(String str, PatternInfo patternInfo) {
        FormatParser formatParser = new FormatParser();
        formatParser.set(str);
        List<Object> items = formatParser.getItems();
        int i = 0;
        while (true) {
            if (i >= items.size()) {
                break;
            }
            Object obj = items.get(i);
            if (obj instanceof VariableField) {
                VariableField variableField = (VariableField) obj;
                if (variableField.getType() == 11) {
                    this.defaultHourFormatChar = variableField.toString().charAt(0);
                    break;
                }
            }
            i++;
        }
        hackTimes(patternInfo, str);
    }

    private class AppendItemFormatsSink extends UResource.Sink {
        static final boolean $assertionsDisabled = false;

        private AppendItemFormatsSink() {
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean z) {
            UResource.Table table = value.getTable();
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                int appendFormatNumber = DateTimePatternGenerator.getAppendFormatNumber(key);
                if (DateTimePatternGenerator.this.getAppendItemFormat(appendFormatNumber) == null) {
                    DateTimePatternGenerator.this.setAppendItemFormat(appendFormatNumber, value.toString());
                }
            }
        }
    }

    private class AppendItemNamesSink extends UResource.Sink {
        private AppendItemNamesSink() {
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean z) {
            UResource.Table table = value.getTable();
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                int cLDRFieldNumber = DateTimePatternGenerator.getCLDRFieldNumber(key);
                if (cLDRFieldNumber != -1) {
                    UResource.Table table2 = value.getTable();
                    int i2 = 0;
                    while (true) {
                        if (!table2.getKeyAndValue(i2, key, value)) {
                            break;
                        }
                        if (!key.contentEquals("dn")) {
                            i2++;
                        } else if (DateTimePatternGenerator.this.getAppendItemName(cLDRFieldNumber) == null) {
                            DateTimePatternGenerator.this.setAppendItemName(cLDRFieldNumber, value.toString());
                        }
                    }
                }
            }
        }
    }

    private void fillInMissing() {
        for (int i = 0; i < 16; i++) {
            if (getAppendItemFormat(i) == null) {
                setAppendItemFormat(i, "{0} ├{2}: {1}┤");
            }
            if (getAppendItemName(i) == null) {
                setAppendItemName(i, "F" + i);
            }
        }
    }

    private class AvailableFormatsSink extends UResource.Sink {
        PatternInfo returnInfo;

        public AvailableFormatsSink(PatternInfo patternInfo) {
            this.returnInfo = patternInfo;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean z) {
            UResource.Table table = value.getTable();
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                String string = key.toString();
                if (!DateTimePatternGenerator.this.isAvailableFormatSet(string)) {
                    DateTimePatternGenerator.this.setAvailableFormat(string);
                    DateTimePatternGenerator.this.addPatternWithSkeleton(value.toString(), string, !z, this.returnInfo);
                }
            }
        }
    }

    private void addCLDRData(PatternInfo patternInfo, ULocale uLocale) {
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, uLocale);
        String calendarTypeToUse = getCalendarTypeToUse(uLocale);
        try {
            iCUResourceBundle.getAllItemsWithFallback("calendar/" + calendarTypeToUse + "/appendItems", new AppendItemFormatsSink());
        } catch (MissingResourceException e) {
        }
        try {
            iCUResourceBundle.getAllItemsWithFallback("fields", new AppendItemNamesSink());
        } catch (MissingResourceException e2) {
        }
        try {
            iCUResourceBundle.getAllItemsWithFallback("calendar/" + calendarTypeToUse + "/availableFormats", new AvailableFormatsSink(patternInfo));
        } catch (MissingResourceException e3) {
        }
    }

    private void setDateTimeFromCalendar(ULocale uLocale) {
        setDateTimeFormat(Calendar.getDateTimePattern(Calendar.getInstance(uLocale), uLocale, 2));
    }

    private void setDecimalSymbols(ULocale uLocale) {
        setDecimal(String.valueOf(new DecimalFormatSymbols(uLocale).getDecimalSeparator()));
    }

    static {
        HashMap map = new HashMap();
        ((ICUResourceBundle) ICUResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "supplementalData", ICUResourceBundle.ICU_DATA_CLASS_LOADER)).getAllItemsWithFallback("timeData", new DayPeriodAllowedHoursSink(map));
        LOCALE_TO_ALLOWED_HOUR = Collections.unmodifiableMap(map);
        DTPNG_CACHE = new SimpleCache();
        CLDR_FIELD_APPEND = new String[]{"Era", "Year", "Quarter", "Month", "Week", "*", "Day-Of-Week", "Day", "*", "*", "*", "Hour", "Minute", "Second", "*", "Timezone"};
        CLDR_FIELD_NAME = new String[]{"era", "year", "quarter", "month", "week", "weekOfMonth", "weekday", "day", "dayOfYear", "weekdayOfMonth", "dayperiod", "hour", "minute", "second", "*", "zone"};
        FIELD_NAME = new String[]{"Era", "Year", "Quarter", "Month", "Week_in_Year", "Week_in_Month", "Weekday", "Day", "Day_Of_Year", "Day_of_Week_in_Month", "Dayperiod", "Hour", "Minute", "Second", "Fractional_Second", "Zone"};
        CANONICAL_ITEMS = new String[]{"G", DateFormat.YEAR, "Q", DateFormat.NUM_MONTH, "w", "W", DateFormat.ABBR_WEEKDAY, DateFormat.DAY, "D", "F", "a", DateFormat.HOUR24, DateFormat.MINUTE, DateFormat.SECOND, "S", DateFormat.ABBR_GENERIC_TZ};
        CANONICAL_SET = new HashSet(Arrays.asList(CANONICAL_ITEMS));
        types = new int[][]{new int[]{71, 0, SHORT, 1, 3}, new int[]{71, 0, LONG, 4}, new int[]{71, 0, NARROW, 5}, new int[]{121, 1, 256, 1, 20}, new int[]{89, 1, UCharacter.UnicodeBlock.TANGUT_ID, 1, 20}, new int[]{117, 1, 288, 1, 20}, new int[]{114, 1, 304, 1, 20}, new int[]{85, 1, SHORT, 1, 3}, new int[]{85, 1, LONG, 4}, new int[]{85, 1, NARROW, 5}, new int[]{81, 2, 256, 1, 2}, new int[]{81, 2, SHORT, 3}, new int[]{81, 2, LONG, 4}, new int[]{81, 2, NARROW, 5}, new int[]{113, 2, UCharacter.UnicodeBlock.TANGUT_ID, 1, 2}, new int[]{113, 2, -275, 3}, new int[]{113, 2, -276, 4}, new int[]{113, 2, -273, 5}, new int[]{77, 3, 256, 1, 2}, new int[]{77, 3, SHORT, 3}, new int[]{77, 3, LONG, 4}, new int[]{77, 3, NARROW, 5}, new int[]{76, 3, UCharacter.UnicodeBlock.TANGUT_ID, 1, 2}, new int[]{76, 3, -275, 3}, new int[]{76, 3, -276, 4}, new int[]{76, 3, -273, 5}, new int[]{108, 3, UCharacter.UnicodeBlock.TANGUT_ID, 1, 1}, new int[]{119, 4, 256, 1, 2}, new int[]{87, 5, 256, 1}, new int[]{69, 6, SHORT, 1, 3}, new int[]{69, 6, LONG, 4}, new int[]{69, 6, NARROW, 5}, new int[]{69, 6, SHORTER, 6}, new int[]{99, 6, 288, 1, 2}, new int[]{99, 6, -291, 3}, new int[]{99, 6, -292, 4}, new int[]{99, 6, -289, 5}, new int[]{99, 6, -290, 6}, new int[]{101, 6, UCharacter.UnicodeBlock.TANGUT_ID, 1, 2}, new int[]{101, 6, -275, 3}, new int[]{101, 6, -276, 4}, new int[]{101, 6, -273, 5}, new int[]{101, 6, -274, 6}, new int[]{100, 7, 256, 1, 2}, new int[]{103, 7, UCharacter.UnicodeBlock.TANGUT_ID, 1, 20}, new int[]{68, 8, 256, 1, 3}, new int[]{70, 9, 256, 1}, new int[]{97, 10, SHORT, 1, 3}, new int[]{97, 10, LONG, 4}, new int[]{97, 10, NARROW, 5}, new int[]{98, 10, -275, 1, 3}, new int[]{98, 10, -276, 4}, new int[]{98, 10, -273, 5}, new int[]{66, 10, -307, 1, 3}, new int[]{66, 10, -308, 4}, new int[]{66, 10, -305, 5}, new int[]{72, 11, 416, 1, 2}, new int[]{107, 11, 432, 1, 2}, new int[]{104, 11, 256, 1, 2}, new int[]{75, 11, UCharacter.UnicodeBlock.TANGUT_ID, 1, 2}, new int[]{109, 12, 256, 1, 2}, new int[]{115, 13, 256, 1, 2}, new int[]{65, 13, UCharacter.UnicodeBlock.TANGUT_ID, 1, 1000}, new int[]{83, 14, 256, 1, 1000}, new int[]{118, 15, -291, 1}, new int[]{118, 15, -292, 4}, new int[]{122, 15, SHORT, 1, 3}, new int[]{122, 15, LONG, 4}, new int[]{90, 15, -273, 1, 3}, new int[]{90, 15, -276, 4}, new int[]{90, 15, -275, 5}, new int[]{79, 15, -275, 1}, new int[]{79, 15, -276, 4}, new int[]{86, 15, -275, 1}, new int[]{86, 15, -276, 2}, new int[]{86, 15, -277, 3}, new int[]{86, 15, -278, 4}, new int[]{88, 15, -273, 1}, new int[]{88, 15, -275, 2}, new int[]{88, 15, -276, 4}, new int[]{120, 15, -273, 1}, new int[]{120, 15, -275, 2}, new int[]{120, 15, -276, 4}};
    }

    private void getAllowedHourFormats(ULocale uLocale) {
        ULocale uLocaleAddLikelySubtags = ULocale.addLikelySubtags(uLocale);
        String country = uLocaleAddLikelySubtags.getCountry();
        if (country.isEmpty()) {
            country = "001";
        }
        String[] strArr = LOCALE_TO_ALLOWED_HOUR.get(uLocaleAddLikelySubtags.getLanguage() + BaseLocale.SEP + country);
        if (strArr == null && (strArr = LOCALE_TO_ALLOWED_HOUR.get(country)) == null) {
            strArr = LAST_RESORT_ALLOWED_HOUR_FORMAT;
        }
        this.allowedHourFormats = strArr;
    }

    private static class DayPeriodAllowedHoursSink extends UResource.Sink {
        HashMap<String, String[]> tempMap;

        private DayPeriodAllowedHoursSink(HashMap<String, String[]> map) {
            this.tempMap = map;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean z) {
            UResource.Table table = value.getTable();
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                String string = key.toString();
                UResource.Table table2 = value.getTable();
                for (int i2 = 0; table2.getKeyAndValue(i2, key, value); i2++) {
                    if (key.contentEquals("allowed")) {
                        this.tempMap.put(string, value.getStringArrayOrStringAsArray());
                    }
                }
            }
        }
    }

    @Deprecated
    public char getDefaultHourFormatChar() {
        return this.defaultHourFormatChar;
    }

    @Deprecated
    public void setDefaultHourFormatChar(char c) {
        this.defaultHourFormatChar = c;
    }

    private void hackTimes(PatternInfo patternInfo, String str) {
        this.fp.set(str);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        boolean z = false;
        while (true) {
            if (i >= this.fp.items.size()) {
                break;
            }
            Object obj = this.fp.items.get(i);
            if (!(obj instanceof String)) {
                char cCharAt = obj.toString().charAt(0);
                if (cCharAt == 'm') {
                    sb.append(obj);
                    z = true;
                } else if (cCharAt == 's') {
                    if (z) {
                        sb.append(obj);
                        addPattern(sb.toString(), false, patternInfo);
                    }
                } else if (z || cCharAt == 'z' || cCharAt == 'Z' || cCharAt == 'v' || cCharAt == 'V') {
                    break;
                }
            } else if (z) {
                sb.append(this.fp.quoteLiteral(obj.toString()));
            }
            i++;
        }
        BitSet bitSet = new BitSet();
        BitSet bitSet2 = new BitSet();
        for (int i2 = 0; i2 < this.fp.items.size(); i2++) {
            Object obj2 = this.fp.items.get(i2);
            if (obj2 instanceof VariableField) {
                bitSet.set(i2);
                char cCharAt2 = obj2.toString().charAt(0);
                if (cCharAt2 == 's' || cCharAt2 == 'S') {
                    bitSet2.set(i2);
                    for (int i3 = i2 - 1; i3 >= 0 && !bitSet.get(i3); i3++) {
                        bitSet2.set(i2);
                    }
                }
            }
        }
        addPattern(getFilteredPattern(this.fp, bitSet2), false, patternInfo);
    }

    private static String getFilteredPattern(FormatParser formatParser, BitSet bitSet) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < formatParser.items.size(); i++) {
            if (!bitSet.get(i)) {
                Object obj = formatParser.items.get(i);
                if (obj instanceof String) {
                    sb.append(formatParser.quoteLiteral(obj.toString()));
                } else {
                    sb.append(obj.toString());
                }
            }
        }
        return sb.toString();
    }

    @Deprecated
    public static int getAppendFormatNumber(UResource.Key key) {
        for (int i = 0; i < CLDR_FIELD_APPEND.length; i++) {
            if (key.contentEquals(CLDR_FIELD_APPEND[i])) {
                return i;
            }
        }
        return -1;
    }

    @Deprecated
    public static int getAppendFormatNumber(String str) {
        for (int i = 0; i < CLDR_FIELD_APPEND.length; i++) {
            if (CLDR_FIELD_APPEND[i].equals(str)) {
                return i;
            }
        }
        return -1;
    }

    private static int getCLDRFieldNumber(UResource.Key key) {
        for (int i = 0; i < CLDR_FIELD_NAME.length; i++) {
            if (key.contentEquals(CLDR_FIELD_NAME[i])) {
                return i;
            }
        }
        return -1;
    }

    public String getBestPattern(String str) {
        return getBestPattern(str, null, 0);
    }

    public String getBestPattern(String str, int i) {
        return getBestPattern(str, null, i);
    }

    private String getBestPattern(String str, DateTimeMatcher dateTimeMatcher, int i) {
        EnumSet<DTPGflags> enumSetNoneOf = EnumSet.noneOf(DTPGflags.class);
        String strMapSkeletonMetacharacters = mapSkeletonMetacharacters(str, enumSetNoneOf);
        synchronized (this) {
            this.current.set(strMapSkeletonMetacharacters, this.fp, false);
            PatternWithMatcher bestRaw = getBestRaw(this.current, -1, this._distanceInfo, dateTimeMatcher);
            if (this._distanceInfo.missingFieldMask == 0 && this._distanceInfo.extraFieldMask == 0) {
                return adjustFieldTypes(bestRaw, this.current, enumSetNoneOf, i);
            }
            int fieldMask = this.current.getFieldMask();
            String bestAppending = getBestAppending(this.current, fieldMask & 1023, this._distanceInfo, dateTimeMatcher, enumSetNoneOf, i);
            String bestAppending2 = getBestAppending(this.current, fieldMask & 64512, this._distanceInfo, dateTimeMatcher, enumSetNoneOf, i);
            return bestAppending == null ? bestAppending2 == null ? "" : bestAppending2 : bestAppending2 == null ? bestAppending : SimpleFormatterImpl.formatRawPattern(getDateTimeFormat(), 2, 2, bestAppending2, bestAppending);
        }
    }

    private String mapSkeletonMetacharacters(String str, EnumSet<DTPGflags> enumSet) {
        int i;
        char cCharAt;
        StringBuilder sb = new StringBuilder();
        int i2 = 0;
        boolean z = false;
        while (i2 < str.length()) {
            char cCharAt2 = str.charAt(i2);
            if (cCharAt2 == '\'') {
                z = !z;
            } else if (!z) {
                if (cCharAt2 == 'j' || cCharAt2 == 'C') {
                    int i3 = 0;
                    while (true) {
                        int i4 = i2 + 1;
                        if (i4 >= str.length() || str.charAt(i4) != cCharAt2) {
                            break;
                        }
                        i3++;
                        i2 = i4;
                    }
                    int i5 = (i3 & 1) + 1;
                    if (i3 >= 2) {
                        i = 3 + (i3 >> 1);
                    } else {
                        i = 1;
                    }
                    char c = 'a';
                    if (cCharAt2 != 'j') {
                        String str2 = this.allowedHourFormats[0];
                        cCharAt = str2.charAt(0);
                        char cCharAt3 = str2.charAt(str2.length() - 1);
                        if (cCharAt3 == 'b' || cCharAt3 == 'B') {
                            c = cCharAt3;
                        }
                    } else {
                        cCharAt = this.defaultHourFormatChar;
                    }
                    if (cCharAt == 'H' || cCharAt == 'k') {
                        i = 0;
                    }
                    while (true) {
                        int i6 = i - 1;
                        if (i <= 0) {
                            break;
                        }
                        sb.append(c);
                        i = i6;
                    }
                    while (true) {
                        int i7 = i5 - 1;
                        if (i5 > 0) {
                            sb.append(cCharAt);
                            i5 = i7;
                        }
                    }
                } else if (cCharAt2 == 'J') {
                    sb.append('H');
                    enumSet.add(DTPGflags.SKELETON_USES_CAP_J);
                } else {
                    sb.append(cCharAt2);
                }
            }
            i2++;
        }
        return sb.toString();
    }

    public DateTimePatternGenerator addPattern(String str, boolean z, PatternInfo patternInfo) {
        return addPatternWithSkeleton(str, null, z, patternInfo);
    }

    @Deprecated
    public DateTimePatternGenerator addPatternWithSkeleton(String str, String str2, boolean z, PatternInfo patternInfo) {
        DateTimeMatcher dateTimeMatcher;
        checkFrozen();
        if (str2 == null) {
            dateTimeMatcher = new DateTimeMatcher().set(str, this.fp, false);
        } else {
            dateTimeMatcher = new DateTimeMatcher().set(str2, this.fp, false);
        }
        String basePattern = dateTimeMatcher.getBasePattern();
        PatternWithSkeletonFlag patternWithSkeletonFlag = this.basePattern_pattern.get(basePattern);
        if (patternWithSkeletonFlag != null && (!patternWithSkeletonFlag.skeletonWasSpecified || (str2 != null && !z))) {
            patternInfo.status = 1;
            patternInfo.conflictingPattern = patternWithSkeletonFlag.pattern;
            if (!z) {
                return this;
            }
        }
        PatternWithSkeletonFlag patternWithSkeletonFlag2 = this.skeleton2pattern.get(dateTimeMatcher);
        if (patternWithSkeletonFlag2 != null) {
            patternInfo.status = 2;
            patternInfo.conflictingPattern = patternWithSkeletonFlag2.pattern;
            if (!z || (str2 != null && patternWithSkeletonFlag2.skeletonWasSpecified)) {
                return this;
            }
        }
        patternInfo.status = 0;
        patternInfo.conflictingPattern = "";
        PatternWithSkeletonFlag patternWithSkeletonFlag3 = new PatternWithSkeletonFlag(str, str2 != null);
        this.skeleton2pattern.put(dateTimeMatcher, patternWithSkeletonFlag3);
        this.basePattern_pattern.put(basePattern, patternWithSkeletonFlag3);
        return this;
    }

    public String getSkeleton(String str) {
        String string;
        synchronized (this) {
            this.current.set(str, this.fp, false);
            string = this.current.toString();
        }
        return string;
    }

    @Deprecated
    public String getSkeletonAllowingDuplicates(String str) {
        String string;
        synchronized (this) {
            this.current.set(str, this.fp, true);
            string = this.current.toString();
        }
        return string;
    }

    @Deprecated
    public String getCanonicalSkeletonAllowingDuplicates(String str) {
        String canonicalString;
        synchronized (this) {
            this.current.set(str, this.fp, true);
            canonicalString = this.current.toCanonicalString();
        }
        return canonicalString;
    }

    public String getBaseSkeleton(String str) {
        String basePattern;
        synchronized (this) {
            this.current.set(str, this.fp, false);
            basePattern = this.current.getBasePattern();
        }
        return basePattern;
    }

    public Map<String, String> getSkeletons(Map<String, String> map) {
        if (map == null) {
            map = new LinkedHashMap<>();
        }
        for (DateTimeMatcher dateTimeMatcher : this.skeleton2pattern.keySet()) {
            String str = this.skeleton2pattern.get(dateTimeMatcher).pattern;
            if (!CANONICAL_SET.contains(str)) {
                map.put(dateTimeMatcher.toString(), str);
            }
        }
        return map;
    }

    public Set<String> getBaseSkeletons(Set<String> set) {
        if (set == null) {
            set = new HashSet<>();
        }
        set.addAll(this.basePattern_pattern.keySet());
        return set;
    }

    public String replaceFieldTypes(String str, String str2) {
        return replaceFieldTypes(str, str2, 0);
    }

    public String replaceFieldTypes(String str, String str2, int i) {
        String strAdjustFieldTypes;
        synchronized (this) {
            strAdjustFieldTypes = adjustFieldTypes(new PatternWithMatcher(str, null), this.current.set(str2, this.fp, false), EnumSet.noneOf(DTPGflags.class), i);
        }
        return strAdjustFieldTypes;
    }

    public void setDateTimeFormat(String str) {
        checkFrozen();
        this.dateTimeFormat = str;
    }

    public String getDateTimeFormat() {
        return this.dateTimeFormat;
    }

    public void setDecimal(String str) {
        checkFrozen();
        this.decimal = str;
    }

    public String getDecimal() {
        return this.decimal;
    }

    @Deprecated
    public Collection<String> getRedundants(Collection<String> collection) {
        synchronized (this) {
            if (collection == null) {
                try {
                    collection = new LinkedHashSet<>();
                } catch (Throwable th) {
                    throw th;
                }
            }
            for (DateTimeMatcher dateTimeMatcher : this.skeleton2pattern.keySet()) {
                String str = this.skeleton2pattern.get(dateTimeMatcher).pattern;
                if (!CANONICAL_SET.contains(str)) {
                    if (getBestPattern(dateTimeMatcher.toString(), dateTimeMatcher, 0).equals(str)) {
                        collection.add(str);
                    }
                }
            }
        }
        return collection;
    }

    public void setAppendItemFormat(int i, String str) {
        checkFrozen();
        this.appendItemFormats[i] = str;
    }

    public String getAppendItemFormat(int i) {
        return this.appendItemFormats[i];
    }

    public void setAppendItemName(int i, String str) {
        checkFrozen();
        this.appendItemNames[i] = str;
    }

    public String getAppendItemName(int i) {
        return this.appendItemNames[i];
    }

    @Deprecated
    public static boolean isSingleField(String str) {
        char cCharAt = str.charAt(0);
        for (int i = 1; i < str.length(); i++) {
            if (str.charAt(i) != cCharAt) {
                return false;
            }
        }
        return true;
    }

    private void setAvailableFormat(String str) {
        checkFrozen();
        this.cldrAvailableFormatKeys.add(str);
    }

    private boolean isAvailableFormatSet(String str) {
        return this.cldrAvailableFormatKeys.contains(str);
    }

    @Override
    public boolean isFrozen() {
        return this.frozen;
    }

    @Override
    public DateTimePatternGenerator freeze() {
        this.frozen = true;
        return this;
    }

    @Override
    public DateTimePatternGenerator cloneAsThawed() {
        DateTimePatternGenerator dateTimePatternGenerator = (DateTimePatternGenerator) clone();
        this.frozen = false;
        return dateTimePatternGenerator;
    }

    public Object clone() {
        try {
            DateTimePatternGenerator dateTimePatternGenerator = (DateTimePatternGenerator) super.clone();
            dateTimePatternGenerator.skeleton2pattern = (TreeMap) this.skeleton2pattern.clone();
            dateTimePatternGenerator.basePattern_pattern = (TreeMap) this.basePattern_pattern.clone();
            dateTimePatternGenerator.appendItemFormats = (String[]) this.appendItemFormats.clone();
            dateTimePatternGenerator.appendItemNames = (String[]) this.appendItemNames.clone();
            dateTimePatternGenerator.current = new DateTimeMatcher();
            dateTimePatternGenerator.fp = new FormatParser();
            dateTimePatternGenerator._distanceInfo = new DistanceInfo();
            dateTimePatternGenerator.frozen = false;
            return dateTimePatternGenerator;
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException("Internal Error", e);
        }
    }

    @Deprecated
    public static class VariableField {
        private final int canonicalIndex;
        private final String string;

        @Deprecated
        public VariableField(String str) {
            this(str, false);
        }

        @Deprecated
        public VariableField(String str, boolean z) {
            this.canonicalIndex = DateTimePatternGenerator.getCanonicalIndex(str, z);
            if (this.canonicalIndex < 0) {
                throw new IllegalArgumentException("Illegal datetime field:\t" + str);
            }
            this.string = str;
        }

        @Deprecated
        public int getType() {
            return DateTimePatternGenerator.types[this.canonicalIndex][1];
        }

        @Deprecated
        public static String getCanonicalCode(int i) {
            try {
                return DateTimePatternGenerator.CANONICAL_ITEMS[i];
            } catch (Exception e) {
                return String.valueOf(i);
            }
        }

        @Deprecated
        public boolean isNumeric() {
            return DateTimePatternGenerator.types[this.canonicalIndex][2] > 0;
        }

        private int getCanonicalIndex() {
            return this.canonicalIndex;
        }

        @Deprecated
        public String toString() {
            return this.string;
        }
    }

    @Deprecated
    public static class FormatParser {
        private static final UnicodeSet SYNTAX_CHARS = new UnicodeSet("[a-zA-Z]").freeze();
        private static final UnicodeSet QUOTING_CHARS = new UnicodeSet("[[[:script=Latn:][:script=Cyrl:]]&[[:L:][:M:]]]").freeze();
        private transient PatternTokenizer tokenizer = new PatternTokenizer().setSyntaxCharacters(SYNTAX_CHARS).setExtraQuotingCharacters(QUOTING_CHARS).setUsingQuote(true);
        private List<Object> items = new ArrayList();

        @Deprecated
        public FormatParser() {
        }

        @Deprecated
        public final FormatParser set(String str) {
            return set(str, false);
        }

        @Deprecated
        public FormatParser set(String str, boolean z) {
            this.items.clear();
            if (str.length() == 0) {
                return this;
            }
            this.tokenizer.setPattern(str);
            StringBuffer stringBuffer = new StringBuffer();
            StringBuffer stringBuffer2 = new StringBuffer();
            while (true) {
                stringBuffer.setLength(0);
                int next = this.tokenizer.next(stringBuffer);
                if (next != 0) {
                    if (next == 1) {
                        if (stringBuffer2.length() != 0 && stringBuffer.charAt(0) != stringBuffer2.charAt(0)) {
                            addVariable(stringBuffer2, false);
                        }
                        stringBuffer2.append(stringBuffer);
                    } else {
                        addVariable(stringBuffer2, false);
                        this.items.add(stringBuffer.toString());
                    }
                } else {
                    addVariable(stringBuffer2, false);
                    return this;
                }
            }
        }

        private void addVariable(StringBuffer stringBuffer, boolean z) {
            if (stringBuffer.length() != 0) {
                this.items.add(new VariableField(stringBuffer.toString(), z));
                stringBuffer.setLength(0);
            }
        }

        @Deprecated
        public List<Object> getItems() {
            return this.items;
        }

        @Deprecated
        public String toString() {
            return toString(0, this.items.size());
        }

        @Deprecated
        public String toString(int i, int i2) {
            StringBuilder sb = new StringBuilder();
            while (i < i2) {
                Object obj = this.items.get(i);
                if (obj instanceof String) {
                    sb.append(this.tokenizer.quoteLiteral((String) obj));
                } else {
                    sb.append(this.items.get(i).toString());
                }
                i++;
            }
            return sb.toString();
        }

        @Deprecated
        public boolean hasDateAndTimeFields() {
            int type = 0;
            for (Object obj : this.items) {
                if (obj instanceof VariableField) {
                    type |= 1 << ((VariableField) obj).getType();
                }
            }
            return ((type & 1023) != 0) && ((type & 64512) != 0);
        }

        @Deprecated
        public Object quoteLiteral(String str) {
            return this.tokenizer.quoteLiteral(str);
        }
    }

    @Deprecated
    public boolean skeletonsAreSimilar(String str, String str2) {
        if (str.equals(str2)) {
            return true;
        }
        TreeSet<String> set = getSet(str);
        TreeSet<String> set2 = getSet(str2);
        if (set.size() != set2.size()) {
            return false;
        }
        Iterator<String> it = set2.iterator();
        Iterator<String> it2 = set.iterator();
        while (it2.hasNext()) {
            if (types[getCanonicalIndex(it2.next(), false)][1] != types[getCanonicalIndex(it.next(), false)][1]) {
                return false;
            }
        }
        return true;
    }

    private TreeSet<String> getSet(String str) {
        List<Object> items = this.fp.set(str).getItems();
        TreeSet<String> treeSet = new TreeSet<>();
        Iterator<Object> it = items.iterator();
        while (it.hasNext()) {
            String string = it.next().toString();
            if (!string.startsWith("G") && !string.startsWith("a")) {
                treeSet.add(string);
            }
        }
        return treeSet;
    }

    private static class PatternWithMatcher {
        public DateTimeMatcher matcherWithSkeleton;
        public String pattern;

        public PatternWithMatcher(String str, DateTimeMatcher dateTimeMatcher) {
            this.pattern = str;
            this.matcherWithSkeleton = dateTimeMatcher;
        }
    }

    private static class PatternWithSkeletonFlag {
        public String pattern;
        public boolean skeletonWasSpecified;

        public PatternWithSkeletonFlag(String str, boolean z) {
            this.pattern = str;
            this.skeletonWasSpecified = z;
        }

        public String toString() {
            return this.pattern + "," + this.skeletonWasSpecified;
        }
    }

    private void checkFrozen() {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
    }

    private String getBestAppending(DateTimeMatcher dateTimeMatcher, int i, DistanceInfo distanceInfo, DateTimeMatcher dateTimeMatcher2, EnumSet<DTPGflags> enumSet, int i2) {
        if (i != 0) {
            PatternWithMatcher bestRaw = getBestRaw(dateTimeMatcher, i, distanceInfo, dateTimeMatcher2);
            String strAdjustFieldTypes = adjustFieldTypes(bestRaw, dateTimeMatcher, enumSet, i2);
            while (distanceInfo.missingFieldMask != 0) {
                if ((distanceInfo.missingFieldMask & SECOND_AND_FRACTIONAL_MASK) == 16384 && (i & SECOND_AND_FRACTIONAL_MASK) == SECOND_AND_FRACTIONAL_MASK) {
                    bestRaw.pattern = strAdjustFieldTypes;
                    enumSet = EnumSet.copyOf((EnumSet) enumSet);
                    enumSet.add(DTPGflags.FIX_FRACTIONAL_SECONDS);
                    strAdjustFieldTypes = adjustFieldTypes(bestRaw, dateTimeMatcher, enumSet, i2);
                    distanceInfo.missingFieldMask &= -16385;
                } else {
                    int i3 = distanceInfo.missingFieldMask;
                    String strAdjustFieldTypes2 = adjustFieldTypes(getBestRaw(dateTimeMatcher, distanceInfo.missingFieldMask, distanceInfo, dateTimeMatcher2), dateTimeMatcher, enumSet, i2);
                    int topBitNumber = getTopBitNumber(i3 & (~distanceInfo.missingFieldMask));
                    strAdjustFieldTypes = SimpleFormatterImpl.formatRawPattern(getAppendFormat(topBitNumber), 2, 3, strAdjustFieldTypes, strAdjustFieldTypes2, getAppendName(topBitNumber));
                }
            }
            return strAdjustFieldTypes;
        }
        return null;
    }

    private String getAppendName(int i) {
        return "'" + this.appendItemNames[i] + "'";
    }

    private String getAppendFormat(int i) {
        return this.appendItemFormats[i];
    }

    private int getTopBitNumber(int i) {
        int i2 = 0;
        while (i != 0) {
            i >>>= 1;
            i2++;
        }
        return i2 - 1;
    }

    private void addCanonicalItems() {
        PatternInfo patternInfo = new PatternInfo();
        for (int i = 0; i < CANONICAL_ITEMS.length; i++) {
            addPattern(String.valueOf(CANONICAL_ITEMS[i]), false, patternInfo);
        }
    }

    private PatternWithMatcher getBestRaw(DateTimeMatcher dateTimeMatcher, int i, DistanceInfo distanceInfo, DateTimeMatcher dateTimeMatcher2) {
        int distance;
        PatternWithMatcher patternWithMatcher = new PatternWithMatcher("", null);
        DistanceInfo distanceInfo2 = new DistanceInfo();
        int i2 = Integer.MAX_VALUE;
        for (DateTimeMatcher dateTimeMatcher3 : this.skeleton2pattern.keySet()) {
            if (!dateTimeMatcher3.equals(dateTimeMatcher2) && (distance = dateTimeMatcher.getDistance(dateTimeMatcher3, i, distanceInfo2)) < i2) {
                PatternWithSkeletonFlag patternWithSkeletonFlag = this.skeleton2pattern.get(dateTimeMatcher3);
                patternWithMatcher.pattern = patternWithSkeletonFlag.pattern;
                if (patternWithSkeletonFlag.skeletonWasSpecified) {
                    patternWithMatcher.matcherWithSkeleton = dateTimeMatcher3;
                } else {
                    patternWithMatcher.matcherWithSkeleton = null;
                }
                distanceInfo.setTo(distanceInfo2);
                if (distance == 0) {
                    break;
                }
                i2 = distance;
            }
        }
        return patternWithMatcher;
    }

    private String adjustFieldTypes(PatternWithMatcher patternWithMatcher, DateTimeMatcher dateTimeMatcher, EnumSet<DTPGflags> enumSet, int i) {
        this.fp.set(patternWithMatcher.pattern);
        StringBuilder sb = new StringBuilder();
        for (Object obj : this.fp.getItems()) {
            if (obj instanceof String) {
                sb.append(this.fp.quoteLiteral((String) obj));
            } else {
                VariableField variableField = (VariableField) obj;
                StringBuilder sb2 = new StringBuilder(variableField.toString());
                int type = variableField.getType();
                if (enumSet.contains(DTPGflags.FIX_FRACTIONAL_SECONDS) && type == 13) {
                    sb2.append(this.decimal);
                    dateTimeMatcher.original.appendFieldTo(14, sb2);
                } else if (dateTimeMatcher.type[type] != 0) {
                    char fieldChar = dateTimeMatcher.original.getFieldChar(type);
                    int fieldLength = dateTimeMatcher.original.getFieldLength(type);
                    if (fieldChar == 'E' && fieldLength < 3) {
                        fieldLength = 3;
                    }
                    DateTimeMatcher dateTimeMatcher2 = patternWithMatcher.matcherWithSkeleton;
                    if ((type == 11 && (i & 2048) == 0) || ((type == 12 && (i & 4096) == 0) || (type == 13 && (i & 8192) == 0))) {
                        fieldLength = sb2.length();
                    } else if (dateTimeMatcher2 != null) {
                        int fieldLength2 = dateTimeMatcher2.original.getFieldLength(type);
                        boolean zIsNumeric = variableField.isNumeric();
                        boolean zFieldIsNumeric = dateTimeMatcher2.fieldIsNumeric(type);
                        if (fieldLength2 == fieldLength || ((zIsNumeric && !zFieldIsNumeric) || (zFieldIsNumeric && !zIsNumeric))) {
                            fieldLength = sb2.length();
                        }
                    }
                    if (type == 11 || type == 3 || type == 6 || (type == 1 && fieldChar != 'Y')) {
                        fieldChar = sb2.charAt(0);
                    }
                    if (type == 11 && enumSet.contains(DTPGflags.SKELETON_USES_CAP_J)) {
                        fieldChar = this.defaultHourFormatChar;
                    }
                    sb2 = new StringBuilder();
                    while (fieldLength > 0) {
                        sb2.append(fieldChar);
                        fieldLength--;
                    }
                }
                sb.append((CharSequence) sb2);
            }
        }
        return sb.toString();
    }

    @Deprecated
    public String getFields(String str) {
        this.fp.set(str);
        StringBuilder sb = new StringBuilder();
        for (Object obj : this.fp.getItems()) {
            if (obj instanceof String) {
                sb.append(this.fp.quoteLiteral((String) obj));
            } else {
                sb.append("{" + getName(obj.toString()) + "}");
            }
        }
        return sb.toString();
    }

    private static String showMask(int i) {
        StringBuilder sb = new StringBuilder();
        for (int i2 = 0; i2 < 16; i2++) {
            if (((1 << i2) & i) != 0) {
                if (sb.length() != 0) {
                    sb.append(" | ");
                }
                sb.append(FIELD_NAME[i2]);
                sb.append(Padder.FALLBACK_PADDING_STRING);
            }
        }
        return sb.toString();
    }

    private static String getName(String str) {
        int canonicalIndex = getCanonicalIndex(str, true);
        String str2 = FIELD_NAME[types[canonicalIndex][1]];
        if (types[canonicalIndex][2] < 0) {
            return str2 + ":S";
        }
        return str2 + ":N";
    }

    private static int getCanonicalIndex(String str, boolean z) {
        int length = str.length();
        if (length == 0) {
            return -1;
        }
        char cCharAt = str.charAt(0);
        for (int i = 1; i < length; i++) {
            if (str.charAt(i) != cCharAt) {
                return -1;
            }
        }
        int i2 = -1;
        for (int i3 = 0; i3 < types.length; i3++) {
            int[] iArr = types[i3];
            if (iArr[0] == cCharAt) {
                if (iArr[3] > length || iArr[iArr.length - 1] < length) {
                    i2 = i3;
                } else {
                    return i3;
                }
            }
        }
        if (z) {
            return -1;
        }
        return i2;
    }

    private static char getCanonicalChar(int i, char c) {
        if (c == 'h' || c == 'K') {
            return 'h';
        }
        for (int i2 = 0; i2 < types.length; i2++) {
            int[] iArr = types[i2];
            if (iArr[1] == i) {
                return (char) iArr[0];
            }
        }
        throw new IllegalArgumentException("Could not find field " + i);
    }

    private static class SkeletonFields {
        static final boolean $assertionsDisabled = false;
        private static final byte DEFAULT_CHAR = 0;
        private static final byte DEFAULT_LENGTH = 0;
        private byte[] chars;
        private byte[] lengths;

        private SkeletonFields() {
            this.chars = new byte[16];
            this.lengths = new byte[16];
        }

        public void clear() {
            Arrays.fill(this.chars, (byte) 0);
            Arrays.fill(this.lengths, (byte) 0);
        }

        void copyFieldFrom(SkeletonFields skeletonFields, int i) {
            this.chars[i] = skeletonFields.chars[i];
            this.lengths[i] = skeletonFields.lengths[i];
        }

        void clearField(int i) {
            this.chars[i] = 0;
            this.lengths[i] = 0;
        }

        char getFieldChar(int i) {
            return (char) this.chars[i];
        }

        int getFieldLength(int i) {
            return this.lengths[i];
        }

        void populate(int i, String str) {
            for (char c : str.toCharArray()) {
            }
            populate(i, str.charAt(0), str.length());
        }

        void populate(int i, char c, int i2) {
            this.chars[i] = (byte) c;
            this.lengths[i] = (byte) i2;
        }

        public boolean isFieldEmpty(int i) {
            return this.lengths[i] == 0;
        }

        public String toString() {
            return appendTo(new StringBuilder(), false, false).toString();
        }

        public String toString(boolean z) {
            return appendTo(new StringBuilder(), false, z).toString();
        }

        public String toCanonicalString() {
            return appendTo(new StringBuilder(), true, false).toString();
        }

        public String toCanonicalString(boolean z) {
            return appendTo(new StringBuilder(), true, z).toString();
        }

        public StringBuilder appendTo(StringBuilder sb) {
            return appendTo(sb, false, false);
        }

        private StringBuilder appendTo(StringBuilder sb, boolean z, boolean z2) {
            for (int i = 0; i < 16; i++) {
                if (!z2 || i != 10) {
                    appendFieldTo(i, sb, z);
                }
            }
            return sb;
        }

        public StringBuilder appendFieldTo(int i, StringBuilder sb) {
            return appendFieldTo(i, sb, false);
        }

        private StringBuilder appendFieldTo(int i, StringBuilder sb, boolean z) {
            char canonicalChar = (char) this.chars[i];
            byte b = this.lengths[i];
            if (z) {
                canonicalChar = DateTimePatternGenerator.getCanonicalChar(i, canonicalChar);
            }
            for (int i2 = 0; i2 < b; i2++) {
                sb.append(canonicalChar);
            }
            return sb;
        }

        public int compareTo(SkeletonFields skeletonFields) {
            for (int i = 0; i < 16; i++) {
                int i2 = this.chars[i] - skeletonFields.chars[i];
                if (i2 != 0) {
                    return i2;
                }
                int i3 = this.lengths[i] - skeletonFields.lengths[i];
                if (i3 != 0) {
                    return i3;
                }
            }
            return 0;
        }

        public boolean equals(Object obj) {
            return this == obj || (obj != null && (obj instanceof SkeletonFields) && compareTo((SkeletonFields) obj) == 0);
        }

        public int hashCode() {
            return Arrays.hashCode(this.chars) ^ Arrays.hashCode(this.lengths);
        }
    }

    private static class DateTimeMatcher implements Comparable<DateTimeMatcher> {
        private boolean addedDefaultDayPeriod;
        private SkeletonFields baseOriginal;
        private SkeletonFields original;
        private int[] type;

        private DateTimeMatcher() {
            this.type = new int[16];
            this.original = new SkeletonFields();
            this.baseOriginal = new SkeletonFields();
            this.addedDefaultDayPeriod = false;
        }

        public boolean fieldIsNumeric(int i) {
            return this.type[i] > 0;
        }

        public String toString() {
            return this.original.toString(this.addedDefaultDayPeriod);
        }

        public String toCanonicalString() {
            return this.original.toCanonicalString(this.addedDefaultDayPeriod);
        }

        String getBasePattern() {
            return this.baseOriginal.toString(this.addedDefaultDayPeriod);
        }

        DateTimeMatcher set(String str, FormatParser formatParser, boolean z) {
            Arrays.fill(this.type, 0);
            this.original.clear();
            this.baseOriginal.clear();
            this.addedDefaultDayPeriod = false;
            formatParser.set(str);
            for (Object obj : formatParser.getItems()) {
                if (obj instanceof VariableField) {
                    VariableField variableField = (VariableField) obj;
                    String string = variableField.toString();
                    int[] iArr = DateTimePatternGenerator.types[variableField.getCanonicalIndex()];
                    int i = iArr[1];
                    if (!this.original.isFieldEmpty(i)) {
                        char fieldChar = this.original.getFieldChar(i);
                        char cCharAt = string.charAt(0);
                        if (!z && (fieldChar != 'r' || cCharAt != 'U')) {
                            if (fieldChar != 'U' || cCharAt != 'r') {
                                throw new IllegalArgumentException("Conflicting fields:\t" + fieldChar + ", " + string + "\t in " + str);
                            }
                        }
                    } else {
                        this.original.populate(i, string);
                        char c = (char) iArr[0];
                        int i2 = iArr[3];
                        if ("GEzvQ".indexOf(c) >= 0) {
                            i2 = 1;
                        }
                        this.baseOriginal.populate(i, c, i2);
                        int length = iArr[2];
                        if (length > 0) {
                            length += string.length();
                        }
                        this.type[i] = length;
                    }
                }
            }
            if (!this.original.isFieldEmpty(11)) {
                if (this.original.getFieldChar(11) == 'h' || this.original.getFieldChar(11) == 'K') {
                    if (this.original.isFieldEmpty(10)) {
                        int i3 = 0;
                        while (true) {
                            if (i3 >= DateTimePatternGenerator.types.length) {
                                break;
                            }
                            int[] iArr2 = DateTimePatternGenerator.types[i3];
                            if (iArr2[1] != 10) {
                                i3++;
                            } else {
                                this.original.populate(10, (char) iArr2[0], iArr2[3]);
                                this.baseOriginal.populate(10, (char) iArr2[0], iArr2[3]);
                                this.type[10] = iArr2[2];
                                this.addedDefaultDayPeriod = true;
                                break;
                            }
                        }
                    }
                } else if (!this.original.isFieldEmpty(10)) {
                    this.original.clearField(10);
                    this.baseOriginal.clearField(10);
                    this.type[10] = 0;
                }
            }
            return this;
        }

        int getFieldMask() {
            int i = 0;
            for (int i2 = 0; i2 < this.type.length; i2++) {
                if (this.type[i2] != 0) {
                    i |= 1 << i2;
                }
            }
            return i;
        }

        void extractFrom(DateTimeMatcher dateTimeMatcher, int i) {
            for (int i2 = 0; i2 < this.type.length; i2++) {
                if (((1 << i2) & i) == 0) {
                    this.type[i2] = 0;
                    this.original.clearField(i2);
                } else {
                    this.type[i2] = dateTimeMatcher.type[i2];
                    this.original.copyFieldFrom(dateTimeMatcher.original, i2);
                }
            }
        }

        int getDistance(DateTimeMatcher dateTimeMatcher, int i, DistanceInfo distanceInfo) {
            int i2;
            distanceInfo.clear();
            int iAbs = 0;
            for (int i3 = 0; i3 < 16; i3++) {
                if (((1 << i3) & i) != 0) {
                    i2 = this.type[i3];
                } else {
                    i2 = 0;
                }
                int i4 = dateTimeMatcher.type[i3];
                if (i2 != i4) {
                    if (i2 == 0) {
                        iAbs += 65536;
                        distanceInfo.addExtra(i3);
                    } else if (i4 == 0) {
                        iAbs += 4096;
                        distanceInfo.addMissing(i3);
                    } else {
                        iAbs += Math.abs(i2 - i4);
                    }
                }
            }
            return iAbs;
        }

        @Override
        public int compareTo(DateTimeMatcher dateTimeMatcher) {
            int iCompareTo = this.original.compareTo(dateTimeMatcher.original);
            if (iCompareTo > 0) {
                return -1;
            }
            return iCompareTo < 0 ? 1 : 0;
        }

        public boolean equals(Object obj) {
            return this == obj || (obj != null && (obj instanceof DateTimeMatcher) && this.original.equals(((DateTimeMatcher) obj).original));
        }

        public int hashCode() {
            return this.original.hashCode();
        }
    }

    private static class DistanceInfo {
        int extraFieldMask;
        int missingFieldMask;

        private DistanceInfo() {
        }

        void clear() {
            this.extraFieldMask = 0;
            this.missingFieldMask = 0;
        }

        void setTo(DistanceInfo distanceInfo) {
            this.missingFieldMask = distanceInfo.missingFieldMask;
            this.extraFieldMask = distanceInfo.extraFieldMask;
        }

        void addMissing(int i) {
            this.missingFieldMask = (1 << i) | this.missingFieldMask;
        }

        void addExtra(int i) {
            this.extraFieldMask = (1 << i) | this.extraFieldMask;
        }

        public String toString() {
            return "missingFieldMask: " + DateTimePatternGenerator.showMask(this.missingFieldMask) + ", extraFieldMask: " + DateTimePatternGenerator.showMask(this.extraFieldMask);
        }
    }
}
