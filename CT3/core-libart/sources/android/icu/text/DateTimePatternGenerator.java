package android.icu.text;

import android.icu.impl.ICUCache;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.PatternTokenizer;
import android.icu.impl.SimpleCache;
import android.icu.impl.Utility;
import android.icu.util.Calendar;
import android.icu.util.Freezable;
import android.icu.util.ICUCloneNotSupportedException;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.EnumSet;
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
    private static final int DATE_MASK = 1023;
    public static final int DAY = 7;
    public static final int DAYPERIOD = 10;
    public static final int DAY_OF_WEEK_IN_MONTH = 9;
    public static final int DAY_OF_YEAR = 8;
    private static final boolean DEBUG = false;
    private static final int DELTA = 16;
    public static final int ERA = 0;
    private static final int EXTRA_FIELD = 65536;
    private static final int FRACTIONAL_MASK = 16384;
    public static final int FRACTIONAL_SECOND = 14;
    public static final int HOUR = 11;
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
    private static final int NONE = 0;
    private static final int NUMERIC = 256;
    public static final int QUARTER = 2;
    public static final int SECOND = 13;
    private static final int SECOND_AND_FRACTIONAL_MASK = 24576;
    private static final int TIME_MASK = 64512;
    public static final int TYPE_LIMIT = 16;
    public static final int WEEKDAY = 6;
    public static final int WEEK_OF_MONTH = 5;
    public static final int WEEK_OF_YEAR = 4;
    public static final int YEAR = 1;
    public static final int ZONE = 15;
    private transient DistanceInfo _distanceInfo;
    private Set<String> cldrAvailableFormatKeys;
    private transient DateTimeMatcher current;
    private char defaultHourFormatChar;
    private transient FormatParser fp;
    private volatile boolean frozen;
    private static ICUCache<String, DateTimePatternGenerator> DTPNG_CACHE = new SimpleCache();
    private static final String[] CLDR_FIELD_APPEND = {"Era", "Year", "Quarter", "Month", "Week", "*", "Day-Of-Week", "Day", "*", "*", "*", "Hour", "Minute", "Second", "*", "Timezone"};
    private static final String[] CLDR_FIELD_NAME = {"era", "year", "*", "month", "week", "*", "weekday", "day", "*", "*", "dayperiod", "hour", "minute", "second", "*", "zone"};
    private static final String[] FIELD_NAME = {"Era", "Year", "Quarter", "Month", "Week_in_Year", "Week_in_Month", "Weekday", "Day", "Day_Of_Year", "Day_of_Week_in_Month", "Dayperiod", "Hour", "Minute", "Second", "Fractional_Second", "Zone"};
    private static final String[] CANONICAL_ITEMS = {"G", DateFormat.YEAR, "Q", DateFormat.NUM_MONTH, "w", "W", DateFormat.ABBR_WEEKDAY, DateFormat.DAY, "D", "F", DateFormat.HOUR24, DateFormat.MINUTE, DateFormat.SECOND, "S", DateFormat.ABBR_GENERIC_TZ};
    private static final Set<String> CANONICAL_SET = new HashSet(Arrays.asList(CANONICAL_ITEMS));
    private static final int SHORT = -258;
    private static final int LONG = -259;
    private static final int NARROW = -257;
    private static final int[][] types = {new int[]{71, 0, SHORT, 1, 3}, new int[]{71, 0, LONG, 4}, new int[]{121, 1, 256, 1, 20}, new int[]{89, 1, 272, 1, 20}, new int[]{117, 1, 288, 1, 20}, new int[]{114, 1, 304, 1, 20}, new int[]{85, 1, SHORT, 1, 3}, new int[]{85, 1, LONG, 4}, new int[]{85, 1, NARROW, 5}, new int[]{81, 2, 256, 1, 2}, new int[]{81, 2, SHORT, 3}, new int[]{81, 2, LONG, 4}, new int[]{113, 2, 272, 1, 2}, new int[]{113, 2, -242, 3}, new int[]{113, 2, -243, 4}, new int[]{77, 3, 256, 1, 2}, new int[]{77, 3, SHORT, 3}, new int[]{77, 3, LONG, 4}, new int[]{77, 3, NARROW, 5}, new int[]{76, 3, 272, 1, 2}, new int[]{76, 3, -274, 3}, new int[]{76, 3, -275, 4}, new int[]{76, 3, -273, 5}, new int[]{108, 3, 272, 1, 1}, new int[]{119, 4, 256, 1, 2}, new int[]{87, 5, 272, 1}, new int[]{69, 6, SHORT, 1, 3}, new int[]{69, 6, LONG, 4}, new int[]{69, 6, NARROW, 5}, new int[]{99, 6, 288, 1, 2}, new int[]{99, 6, -290, 3}, new int[]{99, 6, -291, 4}, new int[]{99, 6, -289, 5}, new int[]{101, 6, 272, 1, 2}, new int[]{101, 6, -274, 3}, new int[]{101, 6, -275, 4}, new int[]{101, 6, -273, 5}, new int[]{100, 7, 256, 1, 2}, new int[]{68, 8, 272, 1, 3}, new int[]{70, 9, 288, 1}, new int[]{103, 7, 304, 1, 20}, new int[]{97, 10, SHORT, 1}, new int[]{72, 11, 416, 1, 2}, new int[]{107, 11, 432, 1, 2}, new int[]{104, 11, 256, 1, 2}, new int[]{75, 11, 272, 1, 2}, new int[]{109, 12, 256, 1, 2}, new int[]{115, 13, 256, 1, 2}, new int[]{83, 14, 272, 1, 1000}, new int[]{65, 13, 288, 1, 1000}, new int[]{118, 15, -290, 1}, new int[]{118, 15, -291, 4}, new int[]{122, 15, SHORT, 1, 3}, new int[]{122, 15, LONG, 4}, new int[]{90, 15, -273, 1, 3}, new int[]{90, 15, -275, 4}, new int[]{90, 15, -274, 5}, new int[]{79, 15, -274, 1}, new int[]{79, 15, -275, 4}, new int[]{86, 15, -274, 1}, new int[]{86, 15, -275, 2}, new int[]{88, 15, -273, 1}, new int[]{88, 15, -274, 2}, new int[]{88, 15, -275, 4}, new int[]{120, 15, -273, 1}, new int[]{120, 15, -274, 2}, new int[]{120, 15, -275, 4}};
    private TreeMap<DateTimeMatcher, PatternWithSkeletonFlag> skeleton2pattern = new TreeMap<>();
    private TreeMap<String, PatternWithSkeletonFlag> basePattern_pattern = new TreeMap<>();
    private String decimal = "?";
    private String dateTimeFormat = "{1} {0}";
    private String[] appendItemFormats = new String[16];
    private String[] appendItemNames = new String[16];

    public static final class PatternInfo {
        public static final int BASE_CONFLICT = 1;
        public static final int CONFLICT = 2;
        public static final int OK = 0;
        public String conflictingPattern;
        public int status;
    }

    public static DateTimePatternGenerator getEmptyInstance() {
        return new DateTimePatternGenerator();
    }

    protected DateTimePatternGenerator() {
        DateTimeMatcher dateTimeMatcher = null;
        Object[] objArr = 0;
        for (int i = 0; i < 16; i++) {
            this.appendItemFormats[i] = "{0} ├{2}: {1}┤";
            this.appendItemNames[i] = "F" + i;
        }
        this.defaultHourFormatChar = 'H';
        this.frozen = false;
        this.current = new DateTimeMatcher(dateTimeMatcher);
        this.fp = new FormatParser();
        this._distanceInfo = new DistanceInfo(objArr == true ? 1 : 0);
        complete();
        this.cldrAvailableFormatKeys = new HashSet(20);
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
        String localeKey = uLocale.toString();
        DateTimePatternGenerator result = DTPNG_CACHE.get(localeKey);
        if (result != null) {
            return result;
        }
        DateTimePatternGenerator result2 = new DateTimePatternGenerator();
        PatternInfo returnInfo = new PatternInfo();
        String shortTimePattern = null;
        for (int i = 0; i <= 3; i++) {
            result2.addPattern(((SimpleDateFormat) DateFormat.getDateInstance(i, uLocale)).toPattern(), false, returnInfo);
            SimpleDateFormat df = (SimpleDateFormat) DateFormat.getTimeInstance(i, uLocale);
            result2.addPattern(df.toPattern(), false, returnInfo);
            if (i == 3) {
                shortTimePattern = df.toPattern();
                FormatParser fp = new FormatParser();
                fp.set(shortTimePattern);
                List<Object> items = fp.getItems();
                int idx = 0;
                while (true) {
                    if (idx < items.size()) {
                        Object item = items.get(idx);
                        if (item instanceof VariableField) {
                            VariableField fld = (VariableField) item;
                            if (fld.getType() == 11) {
                                result2.defaultHourFormatChar = fld.toString().charAt(0);
                                break;
                            }
                        }
                        idx++;
                    }
                }
            }
        }
        ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", uLocale);
        String calendarTypeToUse = uLocale.getKeywordValue("calendar");
        if (calendarTypeToUse == null) {
            String[] preferredCalendarTypes = Calendar.getKeywordValuesForLocale("calendar", uLocale, true);
            calendarTypeToUse = preferredCalendarTypes[0];
        }
        if (calendarTypeToUse == null) {
            calendarTypeToUse = "gregorian";
        }
        try {
            ICUResourceBundle itemBundle = rb.getWithFallback("calendar/" + calendarTypeToUse + "/appendItems");
            for (int i2 = 0; i2 < itemBundle.getSize(); i2++) {
                ICUResourceBundle formatBundle = (ICUResourceBundle) itemBundle.get(i2);
                String formatName = itemBundle.get(i2).getKey();
                String value = formatBundle.getString();
                result2.setAppendItemFormat(getAppendFormatNumber(formatName), value);
            }
        } catch (MissingResourceException e) {
        }
        try {
            ICUResourceBundle itemBundle2 = rb.getWithFallback("fields");
            for (int i3 = 0; i3 < 16; i3++) {
                if (isCLDRFieldName(i3)) {
                    ICUResourceBundle fieldBundle = itemBundle2.getWithFallback(CLDR_FIELD_NAME[i3]);
                    ICUResourceBundle dnBundle = fieldBundle.getWithFallback("dn");
                    String value2 = dnBundle.getString();
                    result2.setAppendItemName(i3, value2);
                }
            }
        } catch (MissingResourceException e2) {
        }
        ICUResourceBundle availFormatsBundle = null;
        try {
            availFormatsBundle = rb.getWithFallback("calendar/" + calendarTypeToUse + "/availableFormats");
        } catch (MissingResourceException e3) {
        }
        boolean override = true;
        while (availFormatsBundle != null) {
            for (int i4 = 0; i4 < availFormatsBundle.getSize(); i4++) {
                String formatKey = availFormatsBundle.get(i4).getKey();
                if (!result2.isAvailableFormatSet(formatKey)) {
                    result2.setAvailableFormat(formatKey);
                    String formatValue = availFormatsBundle.get(i4).getString();
                    result2.addPatternWithSkeleton(formatValue, formatKey, override, returnInfo);
                }
            }
            ICUResourceBundle pbundle = (ICUResourceBundle) availFormatsBundle.getParent();
            if (pbundle == null) {
                break;
            }
            try {
                availFormatsBundle = pbundle.getWithFallback("calendar/" + calendarTypeToUse + "/availableFormats");
            } catch (MissingResourceException e4) {
                availFormatsBundle = null;
            }
            if (availFormatsBundle != null && pbundle.getULocale().getBaseName().equals("root")) {
                override = false;
            }
        }
        if (shortTimePattern != null) {
            hackTimes(result2, returnInfo, shortTimePattern);
        }
        result2.setDateTimeFormat(Calendar.getDateTimePattern(Calendar.getInstance(uLocale), uLocale, 2));
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(uLocale);
        result2.setDecimal(String.valueOf(dfs.getDecimalSeparator()));
        result2.freeze();
        DTPNG_CACHE.put(localeKey, result2);
        return result2;
    }

    @Deprecated
    public char getDefaultHourFormatChar() {
        return this.defaultHourFormatChar;
    }

    @Deprecated
    public void setDefaultHourFormatChar(char defaultHourFormatChar) {
        this.defaultHourFormatChar = defaultHourFormatChar;
    }

    private static void hackTimes(DateTimePatternGenerator result, PatternInfo returnInfo, String hackPattern) {
        result.fp.set(hackPattern);
        StringBuilder mmss = new StringBuilder();
        boolean gotMm = false;
        int i = 0;
        while (true) {
            if (i >= result.fp.items.size()) {
                break;
            }
            Object item = result.fp.items.get(i);
            if (item instanceof String) {
                if (gotMm) {
                    mmss.append(result.fp.quoteLiteral(item.toString()));
                }
            } else {
                char ch = item.toString().charAt(0);
                if (ch == 'm') {
                    gotMm = true;
                    mmss.append(item);
                } else if (ch == 's') {
                    if (gotMm) {
                        mmss.append(item);
                        result.addPattern(mmss.toString(), false, returnInfo);
                    }
                } else if (gotMm || ch == 'z' || ch == 'Z' || ch == 'v' || ch == 'V') {
                    break;
                }
            }
            i++;
        }
        BitSet variables = new BitSet();
        BitSet nuke = new BitSet();
        for (int i2 = 0; i2 < result.fp.items.size(); i2++) {
            Object item2 = result.fp.items.get(i2);
            if (item2 instanceof VariableField) {
                variables.set(i2);
                char ch2 = item2.toString().charAt(0);
                if (ch2 == 's' || ch2 == 'S') {
                    nuke.set(i2);
                    for (int j = i2 - 1; j >= 0 && !variables.get(j); j++) {
                        nuke.set(i2);
                    }
                }
            }
        }
        String hhmm = getFilteredPattern(result.fp, nuke);
        result.addPattern(hhmm, false, returnInfo);
    }

    private static String getFilteredPattern(FormatParser fp, BitSet nuke) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < fp.items.size(); i++) {
            if (!nuke.get(i)) {
                Object item = fp.items.get(i);
                if (item instanceof String) {
                    result.append(fp.quoteLiteral(item.toString()));
                } else {
                    result.append(item.toString());
                }
            }
        }
        return result.toString();
    }

    @Deprecated
    public static int getAppendFormatNumber(String string) {
        for (int i = 0; i < CLDR_FIELD_APPEND.length; i++) {
            if (CLDR_FIELD_APPEND[i].equals(string)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isCLDRFieldName(int index) {
        return (index >= 0 || index < 16) && CLDR_FIELD_NAME[index].charAt(0) != '*';
    }

    public String getBestPattern(String skeleton) {
        return getBestPattern(skeleton, null, 0);
    }

    public String getBestPattern(String skeleton, int options) {
        return getBestPattern(skeleton, null, options);
    }

    private String getBestPattern(String skeleton, DateTimeMatcher skipMatcher, int options) {
        EnumSet<DTPGflags> flags = EnumSet.noneOf(DTPGflags.class);
        StringBuilder skeletonCopy = new StringBuilder(skeleton);
        boolean inQuoted = false;
        for (int patPos = 0; patPos < skeletonCopy.length(); patPos++) {
            char patChr = skeletonCopy.charAt(patPos);
            if (patChr == '\'') {
                inQuoted = !inQuoted;
            } else if (!inQuoted) {
                if (patChr == 'j') {
                    skeletonCopy.setCharAt(patPos, this.defaultHourFormatChar);
                } else if (patChr == 'J') {
                    skeletonCopy.setCharAt(patPos, 'H');
                    flags.add(DTPGflags.SKELETON_USES_CAP_J);
                }
            }
        }
        synchronized (this) {
            this.current.set(skeletonCopy.toString(), this.fp, false);
            PatternWithMatcher bestWithMatcher = getBestRaw(this.current, -1, this._distanceInfo, skipMatcher);
            if (this._distanceInfo.missingFieldMask == 0 && this._distanceInfo.extraFieldMask == 0) {
                return adjustFieldTypes(bestWithMatcher, this.current, flags, options);
            }
            int neededFields = this.current.getFieldMask();
            String datePattern = getBestAppending(this.current, neededFields & 1023, this._distanceInfo, skipMatcher, flags, options);
            String timePattern = getBestAppending(this.current, neededFields & TIME_MASK, this._distanceInfo, skipMatcher, flags, options);
            return datePattern == null ? timePattern == null ? "" : timePattern : timePattern == null ? datePattern : MessageFormat.format(getDateTimeFormat(), timePattern, datePattern);
        }
    }

    public DateTimePatternGenerator addPattern(String pattern, boolean override, PatternInfo returnInfo) {
        return addPatternWithSkeleton(pattern, null, override, returnInfo);
    }

    @Deprecated
    public DateTimePatternGenerator addPatternWithSkeleton(String pattern, String skeletonToUse, boolean override, PatternInfo returnInfo) {
        DateTimeMatcher matcher;
        DateTimeMatcher dateTimeMatcher = null;
        checkFrozen();
        if (skeletonToUse == null) {
            matcher = new DateTimeMatcher(dateTimeMatcher).set(pattern, this.fp, false);
        } else {
            matcher = new DateTimeMatcher(dateTimeMatcher).set(skeletonToUse, this.fp, false);
        }
        String basePattern = matcher.getBasePattern();
        PatternWithSkeletonFlag previousPatternWithSameBase = this.basePattern_pattern.get(basePattern);
        if (previousPatternWithSameBase != null && (!previousPatternWithSameBase.skeletonWasSpecified || (skeletonToUse != null && !override))) {
            returnInfo.status = 1;
            returnInfo.conflictingPattern = previousPatternWithSameBase.pattern;
            if (!override) {
                return this;
            }
        }
        PatternWithSkeletonFlag previousValue = this.skeleton2pattern.get(matcher);
        if (previousValue != null) {
            returnInfo.status = 2;
            returnInfo.conflictingPattern = previousValue.pattern;
            if (!override || (skeletonToUse != null && previousValue.skeletonWasSpecified)) {
                return this;
            }
        }
        returnInfo.status = 0;
        returnInfo.conflictingPattern = "";
        PatternWithSkeletonFlag patWithSkelFlag = new PatternWithSkeletonFlag(pattern, skeletonToUse != null);
        this.skeleton2pattern.put(matcher, patWithSkelFlag);
        this.basePattern_pattern.put(basePattern, patWithSkelFlag);
        return this;
    }

    public String getSkeleton(String pattern) {
        String string;
        synchronized (this) {
            this.current.set(pattern, this.fp, false);
            string = this.current.toString();
        }
        return string;
    }

    @Deprecated
    public String getSkeletonAllowingDuplicates(String pattern) {
        String string;
        synchronized (this) {
            this.current.set(pattern, this.fp, true);
            string = this.current.toString();
        }
        return string;
    }

    @Deprecated
    public String getCanonicalSkeletonAllowingDuplicates(String pattern) {
        String canonicalString;
        synchronized (this) {
            this.current.set(pattern, this.fp, true);
            canonicalString = this.current.toCanonicalString();
        }
        return canonicalString;
    }

    public String getBaseSkeleton(String pattern) {
        String basePattern;
        synchronized (this) {
            this.current.set(pattern, this.fp, false);
            basePattern = this.current.getBasePattern();
        }
        return basePattern;
    }

    public Map<String, String> getSkeletons(Map<String, String> result) {
        if (result == null) {
            result = new LinkedHashMap<>();
        }
        for (DateTimeMatcher item : this.skeleton2pattern.keySet()) {
            PatternWithSkeletonFlag patternWithSkelFlag = this.skeleton2pattern.get(item);
            String pattern = patternWithSkelFlag.pattern;
            if (!CANONICAL_SET.contains(pattern)) {
                result.put(item.toString(), pattern);
            }
        }
        return result;
    }

    public Set<String> getBaseSkeletons(Set<String> result) {
        if (result == null) {
            result = new HashSet<>();
        }
        result.addAll(this.basePattern_pattern.keySet());
        return result;
    }

    public String replaceFieldTypes(String pattern, String skeleton) {
        return replaceFieldTypes(pattern, skeleton, 0);
    }

    public String replaceFieldTypes(String pattern, String skeleton, int options) {
        String strAdjustFieldTypes;
        synchronized (this) {
            PatternWithMatcher patternNoMatcher = new PatternWithMatcher(pattern, null);
            strAdjustFieldTypes = adjustFieldTypes(patternNoMatcher, this.current.set(skeleton, this.fp, false), EnumSet.noneOf(DTPGflags.class), options);
        }
        return strAdjustFieldTypes;
    }

    public void setDateTimeFormat(String dateTimeFormat) {
        checkFrozen();
        this.dateTimeFormat = dateTimeFormat;
    }

    public String getDateTimeFormat() {
        return this.dateTimeFormat;
    }

    public void setDecimal(String decimal) {
        checkFrozen();
        this.decimal = decimal;
    }

    public String getDecimal() {
        return this.decimal;
    }

    @Deprecated
    public Collection<String> getRedundants(Collection<String> output) {
        synchronized (this) {
            if (output == null) {
                output = new LinkedHashSet<>();
                for (DateTimeMatcher cur : this.skeleton2pattern.keySet()) {
                    PatternWithSkeletonFlag patternWithSkelFlag = this.skeleton2pattern.get(cur);
                    String pattern = patternWithSkelFlag.pattern;
                    if (!CANONICAL_SET.contains(pattern)) {
                        String trial = getBestPattern(cur.toString(), cur, 0);
                        if (trial.equals(pattern)) {
                            output.add(pattern);
                        }
                    }
                }
            } else {
                while (cur$iterator.hasNext()) {
                }
            }
        }
        return output;
    }

    public void setAppendItemFormat(int field, String value) {
        checkFrozen();
        this.appendItemFormats[field] = value;
    }

    public String getAppendItemFormat(int field) {
        return this.appendItemFormats[field];
    }

    public void setAppendItemName(int field, String value) {
        checkFrozen();
        this.appendItemNames[field] = value;
    }

    public String getAppendItemName(int field) {
        return this.appendItemNames[field];
    }

    @Deprecated
    public static boolean isSingleField(String skeleton) {
        char first = skeleton.charAt(0);
        for (int i = 1; i < skeleton.length(); i++) {
            if (skeleton.charAt(i) != first) {
                return false;
            }
        }
        return true;
    }

    private void setAvailableFormat(String key) {
        checkFrozen();
        this.cldrAvailableFormatKeys.add(key);
    }

    private boolean isAvailableFormatSet(String key) {
        return this.cldrAvailableFormatKeys.contains(key);
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
        DateTimePatternGenerator result = (DateTimePatternGenerator) clone();
        this.frozen = false;
        return result;
    }

    public Object clone() {
        try {
            DateTimePatternGenerator result = (DateTimePatternGenerator) super.clone();
            result.skeleton2pattern = (TreeMap) this.skeleton2pattern.clone();
            result.basePattern_pattern = (TreeMap) this.basePattern_pattern.clone();
            result.appendItemFormats = (String[]) this.appendItemFormats.clone();
            result.appendItemNames = (String[]) this.appendItemNames.clone();
            result.current = new DateTimeMatcher(null);
            result.fp = new FormatParser();
            result._distanceInfo = new DistanceInfo(null);
            result.frozen = false;
            return result;
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException("Internal Error", e);
        }
    }

    @Deprecated
    public static class VariableField {
        private final int canonicalIndex;
        private final String string;

        @Deprecated
        public VariableField(String string) {
            this(string, false);
        }

        @Deprecated
        public VariableField(String string, boolean strict) {
            this.canonicalIndex = DateTimePatternGenerator.getCanonicalIndex(string, strict);
            if (this.canonicalIndex < 0) {
                throw new IllegalArgumentException("Illegal datetime field:\t" + string);
            }
            this.string = string;
        }

        @Deprecated
        public int getType() {
            return DateTimePatternGenerator.types[this.canonicalIndex][1];
        }

        @Deprecated
        public static String getCanonicalCode(int type) {
            try {
                return DateTimePatternGenerator.CANONICAL_ITEMS[type];
            } catch (Exception e) {
                return String.valueOf(type);
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
        public final FormatParser set(String string) {
            return set(string, false);
        }

        @Deprecated
        public FormatParser set(String string, boolean strict) {
            this.items.clear();
            if (string.length() == 0) {
                return this;
            }
            this.tokenizer.setPattern(string);
            StringBuffer buffer = new StringBuffer();
            StringBuffer variable = new StringBuffer();
            while (true) {
                buffer.setLength(0);
                int status = this.tokenizer.next(buffer);
                if (status != 0) {
                    if (status == 1) {
                        if (variable.length() != 0 && buffer.charAt(0) != variable.charAt(0)) {
                            addVariable(variable, false);
                        }
                        variable.append(buffer);
                    } else {
                        addVariable(variable, false);
                        this.items.add(buffer.toString());
                    }
                } else {
                    addVariable(variable, false);
                    return this;
                }
            }
        }

        private void addVariable(StringBuffer variable, boolean strict) {
            if (variable.length() == 0) {
                return;
            }
            this.items.add(new VariableField(variable.toString(), strict));
            variable.setLength(0);
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
        public String toString(int start, int limit) {
            StringBuilder result = new StringBuilder();
            for (int i = start; i < limit; i++) {
                Object item = this.items.get(i);
                if (item instanceof String) {
                    String itemString = (String) item;
                    result.append(this.tokenizer.quoteLiteral(itemString));
                } else {
                    result.append(this.items.get(i).toString());
                }
            }
            return result.toString();
        }

        @Deprecated
        public boolean hasDateAndTimeFields() {
            int foundMask = 0;
            for (Object item : this.items) {
                if (item instanceof VariableField) {
                    int type = ((VariableField) item).getType();
                    foundMask |= 1 << type;
                }
            }
            boolean isDate = (foundMask & 1023) != 0;
            boolean isTime = (DateTimePatternGenerator.TIME_MASK & foundMask) != 0;
            if (isDate) {
                return isTime;
            }
            return false;
        }

        @Deprecated
        public Object quoteLiteral(String string) {
            return this.tokenizer.quoteLiteral(string);
        }
    }

    @Deprecated
    public boolean skeletonsAreSimilar(String id, String skeleton) {
        if (id.equals(skeleton)) {
            return true;
        }
        TreeSet<String> parser1 = getSet(id);
        TreeSet<String> parser2 = getSet(skeleton);
        if (parser1.size() != parser2.size()) {
            return false;
        }
        Iterator<String> it2 = parser2.iterator();
        for (String item : parser1) {
            int index1 = getCanonicalIndex(item, false);
            String item2 = it2.next();
            int index2 = getCanonicalIndex(item2, false);
            if (types[index1][1] != types[index2][1]) {
                return false;
            }
        }
        return true;
    }

    private TreeSet<String> getSet(String id) {
        List<Object> items = this.fp.set(id).getItems();
        TreeSet<String> result = new TreeSet<>();
        for (Object obj : items) {
            String item = obj.toString();
            if (!item.startsWith("G") && !item.startsWith("a")) {
                result.add(item);
            }
        }
        return result;
    }

    private static class PatternWithMatcher {
        public DateTimeMatcher matcherWithSkeleton;
        public String pattern;

        public PatternWithMatcher(String pat, DateTimeMatcher matcher) {
            this.pattern = pat;
            this.matcherWithSkeleton = matcher;
        }
    }

    private static class PatternWithSkeletonFlag {
        public String pattern;
        public boolean skeletonWasSpecified;

        public PatternWithSkeletonFlag(String pat, boolean skelSpecified) {
            this.pattern = pat;
            this.skeletonWasSpecified = skelSpecified;
        }

        public String toString() {
            return this.pattern + "," + this.skeletonWasSpecified;
        }
    }

    private void checkFrozen() {
        if (!isFrozen()) {
        } else {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
    }

    private String getBestAppending(DateTimeMatcher source, int missingFields, DistanceInfo distInfo, DateTimeMatcher skipMatcher, EnumSet<DTPGflags> flags, int options) {
        String resultPattern = null;
        if (missingFields != 0) {
            PatternWithMatcher resultPatternWithMatcher = getBestRaw(source, missingFields, distInfo, skipMatcher);
            resultPattern = adjustFieldTypes(resultPatternWithMatcher, source, flags, options);
            while (distInfo.missingFieldMask != 0) {
                if ((distInfo.missingFieldMask & SECOND_AND_FRACTIONAL_MASK) == 16384 && (missingFields & SECOND_AND_FRACTIONAL_MASK) == SECOND_AND_FRACTIONAL_MASK) {
                    resultPatternWithMatcher.pattern = resultPattern;
                    flags = EnumSet.copyOf((EnumSet) flags);
                    flags.add(DTPGflags.FIX_FRACTIONAL_SECONDS);
                    resultPattern = adjustFieldTypes(resultPatternWithMatcher, source, flags, options);
                    distInfo.missingFieldMask &= -16385;
                } else {
                    int startingMask = distInfo.missingFieldMask;
                    PatternWithMatcher tempWithMatcher = getBestRaw(source, distInfo.missingFieldMask, distInfo, skipMatcher);
                    String temp = adjustFieldTypes(tempWithMatcher, source, flags, options);
                    int foundMask = startingMask & (~distInfo.missingFieldMask);
                    int topField = getTopBitNumber(foundMask);
                    resultPattern = MessageFormat.format(getAppendFormat(topField), resultPattern, temp, getAppendName(topField));
                }
            }
        }
        return resultPattern;
    }

    private String getAppendName(int foundMask) {
        return "'" + this.appendItemNames[foundMask] + "'";
    }

    private String getAppendFormat(int foundMask) {
        return this.appendItemFormats[foundMask];
    }

    private int getTopBitNumber(int foundMask) {
        int i = 0;
        while (foundMask != 0) {
            foundMask >>>= 1;
            i++;
        }
        return i - 1;
    }

    private void complete() {
        PatternInfo patternInfo = new PatternInfo();
        for (int i = 0; i < CANONICAL_ITEMS.length; i++) {
            addPattern(String.valueOf(CANONICAL_ITEMS[i]), false, patternInfo);
        }
    }

    private PatternWithMatcher getBestRaw(DateTimeMatcher source, int includeMask, DistanceInfo missingFields, DateTimeMatcher skipMatcher) {
        int distance;
        int bestDistance = Integer.MAX_VALUE;
        PatternWithMatcher bestPatternWithMatcher = new PatternWithMatcher("", null);
        DistanceInfo tempInfo = new DistanceInfo(null);
        for (DateTimeMatcher trial : this.skeleton2pattern.keySet()) {
            if (!trial.equals(skipMatcher) && (distance = source.getDistance(trial, includeMask, tempInfo)) < bestDistance) {
                bestDistance = distance;
                PatternWithSkeletonFlag patternWithSkelFlag = this.skeleton2pattern.get(trial);
                bestPatternWithMatcher.pattern = patternWithSkelFlag.pattern;
                if (patternWithSkelFlag.skeletonWasSpecified) {
                    bestPatternWithMatcher.matcherWithSkeleton = trial;
                } else {
                    bestPatternWithMatcher.matcherWithSkeleton = null;
                }
                missingFields.setTo(tempInfo);
                if (distance == 0) {
                    break;
                }
            }
        }
        return bestPatternWithMatcher;
    }

    private enum DTPGflags {
        FIX_FRACTIONAL_SECONDS,
        SKELETON_USES_CAP_J;

        public static DTPGflags[] valuesCustom() {
            return values();
        }
    }

    private String adjustFieldTypes(PatternWithMatcher patternWithMatcher, DateTimeMatcher inputRequest, EnumSet<DTPGflags> flags, int options) {
        this.fp.set(patternWithMatcher.pattern);
        StringBuilder newPattern = new StringBuilder();
        for (Object item : this.fp.getItems()) {
            if (item instanceof String) {
                newPattern.append(this.fp.quoteLiteral((String) item));
            } else {
                VariableField variableField = (VariableField) item;
                StringBuilder fieldBuilder = new StringBuilder(variableField.toString());
                int type = variableField.getType();
                if (flags.contains(DTPGflags.FIX_FRACTIONAL_SECONDS) && type == 13) {
                    String newField = inputRequest.original[14];
                    fieldBuilder.append(this.decimal);
                    fieldBuilder.append(newField);
                } else if (inputRequest.type[type] != 0) {
                    String reqField = inputRequest.original[type];
                    int reqFieldLen = reqField.length();
                    if (reqField.charAt(0) == 'E' && reqFieldLen < 3) {
                        reqFieldLen = 3;
                    }
                    int adjFieldLen = reqFieldLen;
                    DateTimeMatcher matcherWithSkeleton = patternWithMatcher.matcherWithSkeleton;
                    if ((type == 11 && (options & 2048) == 0) || ((type == 12 && (options & 4096) == 0) || (type == 13 && (options & 8192) == 0))) {
                        adjFieldLen = fieldBuilder.length();
                    } else if (matcherWithSkeleton != null) {
                        String skelField = matcherWithSkeleton.origStringForField(type);
                        int skelFieldLen = skelField.length();
                        boolean patFieldIsNumeric = variableField.isNumeric();
                        boolean skelFieldIsNumeric = matcherWithSkeleton.fieldIsNumeric(type);
                        if (skelFieldLen == reqFieldLen || ((patFieldIsNumeric && !skelFieldIsNumeric) || (skelFieldIsNumeric && !patFieldIsNumeric))) {
                            adjFieldLen = fieldBuilder.length();
                        }
                    }
                    char c = (type == 11 || type == 3 || type == 6 || (type == 1 && reqField.charAt(0) != 'Y')) ? fieldBuilder.charAt(0) : reqField.charAt(0);
                    if (type == 11 && flags.contains(DTPGflags.SKELETON_USES_CAP_J)) {
                        c = this.defaultHourFormatChar;
                    }
                    fieldBuilder = new StringBuilder();
                    for (int i = adjFieldLen; i > 0; i--) {
                        fieldBuilder.append(c);
                    }
                }
                newPattern.append((CharSequence) fieldBuilder);
            }
        }
        return newPattern.toString();
    }

    @Deprecated
    public String getFields(String pattern) {
        this.fp.set(pattern);
        StringBuilder newPattern = new StringBuilder();
        for (Object item : this.fp.getItems()) {
            if (item instanceof String) {
                newPattern.append(this.fp.quoteLiteral((String) item));
            } else {
                newPattern.append("{").append(getName(item.toString())).append("}");
            }
        }
        return newPattern.toString();
    }

    private static String showMask(int mask) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            if (((1 << i) & mask) != 0) {
                if (result.length() != 0) {
                    result.append(" | ");
                }
                result.append(FIELD_NAME[i]);
                result.append(" ");
            }
        }
        return result.toString();
    }

    private static String getName(String s) {
        int i = getCanonicalIndex(s, true);
        String name = FIELD_NAME[types[i][1]];
        int subtype = types[i][2];
        boolean string = subtype < 0;
        if (string) {
            subtype = -subtype;
        }
        return subtype < 0 ? name + ":S" : name + ":N";
    }

    private static int getCanonicalIndex(String s, boolean strict) {
        int len = s.length();
        if (len == 0) {
            return -1;
        }
        int ch = s.charAt(0);
        for (int i = 1; i < len; i++) {
            if (s.charAt(i) != ch) {
                return -1;
            }
        }
        int bestRow = -1;
        for (int i2 = 0; i2 < types.length; i2++) {
            int[] row = types[i2];
            if (row[0] == ch) {
                bestRow = i2;
                if (row[3] <= len && row[row.length - 1] >= len) {
                    return i2;
                }
            }
        }
        if (strict) {
            return -1;
        }
        return bestRow;
    }

    private static class DateTimeMatcher implements Comparable<DateTimeMatcher> {
        private String[] baseOriginal;
        private String[] original;
        private int[] type;

        DateTimeMatcher(DateTimeMatcher dateTimeMatcher) {
            this();
        }

        private DateTimeMatcher() {
            this.type = new int[16];
            this.original = new String[16];
            this.baseOriginal = new String[16];
        }

        public String origStringForField(int field) {
            return this.original[field];
        }

        public boolean fieldIsNumeric(int field) {
            return this.type[field] > 0;
        }

        public String toString() {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                if (this.original[i].length() != 0) {
                    result.append(this.original[i]);
                }
            }
            return result.toString();
        }

        public String toCanonicalString() {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                if (this.original[i].length() != 0) {
                    int j = 0;
                    while (true) {
                        if (j < DateTimePatternGenerator.types.length) {
                            int[] row = DateTimePatternGenerator.types[j];
                            if (row[1] != i) {
                                j++;
                            } else {
                                char originalChar = this.original[i].charAt(0);
                                char repeatChar = (originalChar == 'h' || originalChar == 'K') ? 'h' : (char) row[0];
                                result.append(Utility.repeat(String.valueOf(repeatChar), this.original[i].length()));
                            }
                        }
                    }
                }
            }
            return result.toString();
        }

        String getBasePattern() {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                if (this.baseOriginal[i].length() != 0) {
                    result.append(this.baseOriginal[i]);
                }
            }
            return result.toString();
        }

        DateTimeMatcher set(String pattern, FormatParser fp, boolean allowDuplicateFields) {
            for (int i = 0; i < 16; i++) {
                this.type[i] = 0;
                this.original[i] = "";
                this.baseOriginal[i] = "";
            }
            fp.set(pattern);
            for (Object obj : fp.getItems()) {
                if (obj instanceof VariableField) {
                    VariableField item = (VariableField) obj;
                    String field = item.toString();
                    if (field.charAt(0) != 'a') {
                        int canonicalIndex = item.getCanonicalIndex();
                        int[] row = DateTimePatternGenerator.types[canonicalIndex];
                        int typeValue = row[1];
                        if (this.original[typeValue].length() == 0) {
                            this.original[typeValue] = field;
                            char repeatChar = (char) row[0];
                            int repeatCount = row[3];
                            if ("GEzvQ".indexOf(repeatChar) >= 0) {
                                repeatCount = 1;
                            }
                            this.baseOriginal[typeValue] = Utility.repeat(String.valueOf(repeatChar), repeatCount);
                            int subTypeValue = row[2];
                            if (subTypeValue > 0) {
                                subTypeValue += field.length();
                            }
                            this.type[typeValue] = subTypeValue;
                        } else if (!allowDuplicateFields && (this.original[typeValue].charAt(0) != 'r' || field.charAt(0) != 'U')) {
                            if (this.original[typeValue].charAt(0) != 'U' || field.charAt(0) != 'r') {
                                throw new IllegalArgumentException("Conflicting fields:\t" + this.original[typeValue] + ", " + field + "\t in " + pattern);
                            }
                        }
                    } else {
                        continue;
                    }
                }
            }
            return this;
        }

        int getFieldMask() {
            int result = 0;
            for (int i = 0; i < this.type.length; i++) {
                if (this.type[i] != 0) {
                    result |= 1 << i;
                }
            }
            return result;
        }

        void extractFrom(DateTimeMatcher source, int fieldMask) {
            for (int i = 0; i < this.type.length; i++) {
                if (((1 << i) & fieldMask) != 0) {
                    this.type[i] = source.type[i];
                    this.original[i] = source.original[i];
                } else {
                    this.type[i] = 0;
                    this.original[i] = "";
                }
            }
        }

        int getDistance(DateTimeMatcher other, int includeMask, DistanceInfo distanceInfo) {
            int result = 0;
            distanceInfo.clear();
            for (int i = 0; i < this.type.length; i++) {
                int myType = ((1 << i) & includeMask) == 0 ? 0 : this.type[i];
                int otherType = other.type[i];
                if (myType != otherType) {
                    if (myType == 0) {
                        result += 65536;
                        distanceInfo.addExtra(i);
                    } else if (otherType == 0) {
                        result += 4096;
                        distanceInfo.addMissing(i);
                    } else {
                        result += Math.abs(myType - otherType);
                    }
                }
            }
            return result;
        }

        @Override
        public int compareTo(DateTimeMatcher that) {
            for (int i = 0; i < this.original.length; i++) {
                int comp = this.original[i].compareTo(that.original[i]);
                if (comp != 0) {
                    return -comp;
                }
            }
            return 0;
        }

        public boolean equals(Object other) {
            if (!(other instanceof DateTimeMatcher)) {
                return false;
            }
            DateTimeMatcher that = (DateTimeMatcher) other;
            for (int i = 0; i < this.original.length; i++) {
                if (!this.original[i].equals(that.original[i])) {
                    return false;
                }
            }
            return true;
        }

        public int hashCode() {
            int result = 0;
            for (int i = 0; i < this.original.length; i++) {
                result ^= this.original[i].hashCode();
            }
            return result;
        }
    }

    private static class DistanceInfo {
        int extraFieldMask;
        int missingFieldMask;

        DistanceInfo(DistanceInfo distanceInfo) {
            this();
        }

        private DistanceInfo() {
        }

        void clear() {
            this.extraFieldMask = 0;
            this.missingFieldMask = 0;
        }

        void setTo(DistanceInfo other) {
            this.missingFieldMask = other.missingFieldMask;
            this.extraFieldMask = other.extraFieldMask;
        }

        void addMissing(int field) {
            this.missingFieldMask |= 1 << field;
        }

        void addExtra(int field) {
            this.extraFieldMask |= 1 << field;
        }

        public String toString() {
            return "missingFieldMask: " + DateTimePatternGenerator.showMask(this.missingFieldMask) + ", extraFieldMask: " + DateTimePatternGenerator.showMask(this.extraFieldMask);
        }
    }
}
