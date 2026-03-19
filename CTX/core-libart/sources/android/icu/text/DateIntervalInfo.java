package android.icu.text;

import android.icu.impl.ICUCache;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SimpleCache;
import android.icu.impl.UResource;
import android.icu.impl.Utility;
import android.icu.impl.number.Padder;
import android.icu.text.DateIntervalFormat;
import android.icu.util.Calendar;
import android.icu.util.Freezable;
import android.icu.util.ICUCloneNotSupportedException;
import android.icu.util.ICUException;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

public class DateIntervalInfo implements Cloneable, Freezable<DateIntervalInfo>, Serializable {
    private static final int MINIMUM_SUPPORTED_CALENDAR_FIELD = 13;
    static final int currentSerialVersion = 1;
    private static final long serialVersionUID = 1;
    private String fFallbackIntervalPattern;
    private boolean fFirstDateInPtnIsLaterDate;
    private Map<String, Map<String, PatternInfo>> fIntervalPatterns;
    private transient boolean fIntervalPatternsReadOnly;
    private volatile transient boolean frozen;
    static final String[] CALENDAR_FIELD_TO_PATTERN_LETTER = {"G", DateFormat.YEAR, DateFormat.NUM_MONTH, "w", "W", DateFormat.DAY, "D", DateFormat.ABBR_WEEKDAY, "F", "a", "h", DateFormat.HOUR24, DateFormat.MINUTE, DateFormat.SECOND, "S", DateFormat.ABBR_SPECIFIC_TZ, Padder.FALLBACK_PADDING_STRING, "Y", "e", "u", "g", "A", Padder.FALLBACK_PADDING_STRING, Padder.FALLBACK_PADDING_STRING};
    private static String CALENDAR_KEY = "calendar";
    private static String INTERVAL_FORMATS_KEY = "intervalFormats";
    private static String FALLBACK_STRING = "fallback";
    private static String LATEST_FIRST_PREFIX = "latestFirst:";
    private static String EARLIEST_FIRST_PREFIX = "earliestFirst:";
    private static final ICUCache<String, DateIntervalInfo> DIICACHE = new SimpleCache();

    public static final class PatternInfo implements Cloneable, Serializable {
        static final int currentSerialVersion = 1;
        private static final long serialVersionUID = 1;
        private final boolean fFirstDateInPtnIsLaterDate;
        private final String fIntervalPatternFirstPart;
        private final String fIntervalPatternSecondPart;

        public PatternInfo(String str, String str2, boolean z) {
            this.fIntervalPatternFirstPart = str;
            this.fIntervalPatternSecondPart = str2;
            this.fFirstDateInPtnIsLaterDate = z;
        }

        public String getFirstPart() {
            return this.fIntervalPatternFirstPart;
        }

        public String getSecondPart() {
            return this.fIntervalPatternSecondPart;
        }

        public boolean firstDateInPtnIsLaterDate() {
            return this.fFirstDateInPtnIsLaterDate;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof PatternInfo)) {
                return false;
            }
            PatternInfo patternInfo = (PatternInfo) obj;
            return Utility.objectEquals(this.fIntervalPatternFirstPart, patternInfo.fIntervalPatternFirstPart) && Utility.objectEquals(this.fIntervalPatternSecondPart, patternInfo.fIntervalPatternSecondPart) && this.fFirstDateInPtnIsLaterDate == patternInfo.fFirstDateInPtnIsLaterDate;
        }

        public int hashCode() {
            int iHashCode = this.fIntervalPatternFirstPart != null ? this.fIntervalPatternFirstPart.hashCode() : 0;
            if (this.fIntervalPatternSecondPart != null) {
                iHashCode ^= this.fIntervalPatternSecondPart.hashCode();
            }
            if (this.fFirstDateInPtnIsLaterDate) {
                return ~iHashCode;
            }
            return iHashCode;
        }

        @Deprecated
        public String toString() {
            return "{first=«" + this.fIntervalPatternFirstPart + "», second=«" + this.fIntervalPatternSecondPart + "», reversed:" + this.fFirstDateInPtnIsLaterDate + "}";
        }
    }

    @Deprecated
    public DateIntervalInfo() {
        this.fFirstDateInPtnIsLaterDate = false;
        this.fIntervalPatterns = null;
        this.frozen = false;
        this.fIntervalPatternsReadOnly = false;
        this.fIntervalPatterns = new HashMap();
        this.fFallbackIntervalPattern = "{0} – {1}";
    }

    public DateIntervalInfo(ULocale uLocale) {
        this.fFirstDateInPtnIsLaterDate = false;
        this.fIntervalPatterns = null;
        this.frozen = false;
        this.fIntervalPatternsReadOnly = false;
        initializeData(uLocale);
    }

    public DateIntervalInfo(Locale locale) {
        this(ULocale.forLocale(locale));
    }

    private void initializeData(ULocale uLocale) {
        String string = uLocale.toString();
        DateIntervalInfo dateIntervalInfo = DIICACHE.get(string);
        if (dateIntervalInfo == null) {
            setup(uLocale);
            this.fIntervalPatternsReadOnly = true;
            DIICACHE.put(string, ((DateIntervalInfo) clone()).freeze());
            return;
        }
        initializeFromReadOnlyPatterns(dateIntervalInfo);
    }

    private void initializeFromReadOnlyPatterns(DateIntervalInfo dateIntervalInfo) {
        this.fFallbackIntervalPattern = dateIntervalInfo.fFallbackIntervalPattern;
        this.fFirstDateInPtnIsLaterDate = dateIntervalInfo.fFirstDateInPtnIsLaterDate;
        this.fIntervalPatterns = dateIntervalInfo.fIntervalPatterns;
        this.fIntervalPatternsReadOnly = true;
    }

    private static final class DateIntervalSink extends UResource.Sink {
        private static final String ACCEPTED_PATTERN_LETTERS = "yMdahHms";
        private static final String DATE_INTERVAL_PATH_PREFIX = "/LOCALE/" + DateIntervalInfo.CALENDAR_KEY + "/";
        private static final String DATE_INTERVAL_PATH_SUFFIX;
        DateIntervalInfo dateIntervalInfo;
        String nextCalendarType;

        public DateIntervalSink(DateIntervalInfo dateIntervalInfo) {
            this.dateIntervalInfo = dateIntervalInfo;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean z) {
            UResource.Table table = value.getTable();
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                if (key.contentEquals(DateIntervalInfo.INTERVAL_FORMATS_KEY)) {
                    if (value.getType() == 3) {
                        this.nextCalendarType = getCalendarTypeFromPath(value.getAliasString());
                        return;
                    } else if (value.getType() == 2) {
                        UResource.Table table2 = value.getTable();
                        for (int i2 = 0; table2.getKeyAndValue(i2, key, value); i2++) {
                            if (value.getType() == 2) {
                                processSkeletonTable(key, value);
                            }
                        }
                        return;
                    }
                }
            }
        }

        public void processSkeletonTable(UResource.Key key, UResource.Value value) {
            CharSequence charSequenceValidateAndProcessPatternLetter;
            String string = key.toString();
            UResource.Table table = value.getTable();
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                if (value.getType() == 0 && (charSequenceValidateAndProcessPatternLetter = validateAndProcessPatternLetter(key)) != null) {
                    setIntervalPatternIfAbsent(string, charSequenceValidateAndProcessPatternLetter.toString(), value);
                }
            }
        }

        public String getAndResetNextCalendarType() {
            String str = this.nextCalendarType;
            this.nextCalendarType = null;
            return str;
        }

        static {
            StringBuilder sb = new StringBuilder();
            sb.append("/");
            sb.append(DateIntervalInfo.INTERVAL_FORMATS_KEY);
            DATE_INTERVAL_PATH_SUFFIX = sb.toString();
        }

        private String getCalendarTypeFromPath(String str) {
            if (str.startsWith(DATE_INTERVAL_PATH_PREFIX) && str.endsWith(DATE_INTERVAL_PATH_SUFFIX)) {
                return str.substring(DATE_INTERVAL_PATH_PREFIX.length(), str.length() - DATE_INTERVAL_PATH_SUFFIX.length());
            }
            throw new ICUException("Malformed 'intervalFormat' alias path: " + str);
        }

        private CharSequence validateAndProcessPatternLetter(CharSequence charSequence) {
            if (charSequence.length() != 1) {
                return null;
            }
            char cCharAt = charSequence.charAt(0);
            if (ACCEPTED_PATTERN_LETTERS.indexOf(cCharAt) < 0) {
                return null;
            }
            if (cCharAt == DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[11].charAt(0)) {
                return DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[10];
            }
            return charSequence;
        }

        private void setIntervalPatternIfAbsent(String str, String str2, UResource.Value value) {
            Map map = (Map) this.dateIntervalInfo.fIntervalPatterns.get(str);
            if (map == null || !map.containsKey(str2)) {
                this.dateIntervalInfo.setIntervalPatternInternally(str, str2, value.toString());
            }
        }
    }

    private void setup(ULocale uLocale) {
        this.fIntervalPatterns = new HashMap(19);
        this.fFallbackIntervalPattern = "{0} – {1}";
        try {
            String keywordValue = uLocale.getKeywordValue("calendar");
            if (keywordValue == null) {
                keywordValue = Calendar.getKeywordValuesForLocale("calendar", uLocale, true)[0];
            }
            if (keywordValue == null) {
                keywordValue = "gregorian";
            }
            DateIntervalSink dateIntervalSink = new DateIntervalSink(this);
            ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, uLocale);
            setFallbackIntervalPattern(iCUResourceBundle.getStringWithFallback(CALENDAR_KEY + "/" + keywordValue + "/" + INTERVAL_FORMATS_KEY + "/" + FALLBACK_STRING));
            HashSet hashSet = new HashSet();
            while (keywordValue != null) {
                if (hashSet.contains(keywordValue)) {
                    throw new ICUException("Loop in calendar type fallback: " + keywordValue);
                }
                hashSet.add(keywordValue);
                iCUResourceBundle.getAllItemsWithFallback(CALENDAR_KEY + "/" + keywordValue, dateIntervalSink);
                keywordValue = dateIntervalSink.getAndResetNextCalendarType();
            }
        } catch (MissingResourceException e) {
        }
    }

    private static int splitPatternInto2Part(String str) {
        boolean z;
        int[] iArr = new int[58];
        int i = 0;
        char c = 0;
        int i2 = 0;
        boolean z2 = false;
        while (true) {
            z = true;
            if (i >= str.length()) {
                z = false;
                break;
            }
            char cCharAt = str.charAt(i);
            if (cCharAt != c && i2 > 0) {
                int i3 = c - 'A';
                if (iArr[i3] != 0) {
                    break;
                }
                iArr[i3] = 1;
                i2 = 0;
            }
            if (cCharAt == '\'') {
                int i4 = i + 1;
                if (i4 >= str.length() || str.charAt(i4) != '\'') {
                    z2 = !z2;
                } else {
                    i = i4;
                }
            } else if (!z2 && ((cCharAt >= 'a' && cCharAt <= 'z') || (cCharAt >= 'A' && cCharAt <= 'Z'))) {
                i2++;
                c = cCharAt;
            }
            i++;
        }
        return i - ((i2 <= 0 || z || iArr[c - 'A'] != 0) ? i2 : 0);
    }

    public void setIntervalPattern(String str, int i, String str2) {
        if (this.frozen) {
            throw new UnsupportedOperationException("no modification is allowed after DII is frozen");
        }
        if (i > 13) {
            throw new IllegalArgumentException("calendar field is larger than MINIMUM_SUPPORTED_CALENDAR_FIELD");
        }
        if (this.fIntervalPatternsReadOnly) {
            this.fIntervalPatterns = cloneIntervalPatterns(this.fIntervalPatterns);
            this.fIntervalPatternsReadOnly = false;
        }
        PatternInfo intervalPatternInternally = setIntervalPatternInternally(str, CALENDAR_FIELD_TO_PATTERN_LETTER[i], str2);
        if (i == 11) {
            setIntervalPattern(str, CALENDAR_FIELD_TO_PATTERN_LETTER[9], intervalPatternInternally);
            setIntervalPattern(str, CALENDAR_FIELD_TO_PATTERN_LETTER[10], intervalPatternInternally);
        } else if (i == 5 || i == 7) {
            setIntervalPattern(str, CALENDAR_FIELD_TO_PATTERN_LETTER[5], intervalPatternInternally);
        }
    }

    private PatternInfo setIntervalPatternInternally(String str, String str2, String str3) {
        boolean z;
        Map<String, PatternInfo> map = this.fIntervalPatterns.get(str);
        boolean z2 = false;
        if (map != null) {
            z = false;
        } else {
            map = new HashMap<>();
            z = true;
        }
        boolean z3 = this.fFirstDateInPtnIsLaterDate;
        if (str3.startsWith(LATEST_FIRST_PREFIX)) {
            str3 = str3.substring(LATEST_FIRST_PREFIX.length(), str3.length());
            z2 = true;
        } else if (str3.startsWith(EARLIEST_FIRST_PREFIX)) {
            str3 = str3.substring(EARLIEST_FIRST_PREFIX.length(), str3.length());
        } else {
            z2 = z3;
        }
        PatternInfo patternInfoGenPatternInfo = genPatternInfo(str3, z2);
        map.put(str2, patternInfoGenPatternInfo);
        if (z) {
            this.fIntervalPatterns.put(str, map);
        }
        return patternInfoGenPatternInfo;
    }

    private void setIntervalPattern(String str, String str2, PatternInfo patternInfo) {
        this.fIntervalPatterns.get(str).put(str2, patternInfo);
    }

    @Deprecated
    public static PatternInfo genPatternInfo(String str, boolean z) {
        String strSubstring;
        int iSplitPatternInto2Part = splitPatternInto2Part(str);
        String strSubstring2 = str.substring(0, iSplitPatternInto2Part);
        if (iSplitPatternInto2Part < str.length()) {
            strSubstring = str.substring(iSplitPatternInto2Part, str.length());
        } else {
            strSubstring = null;
        }
        return new PatternInfo(strSubstring2, strSubstring, z);
    }

    public PatternInfo getIntervalPattern(String str, int i) {
        PatternInfo patternInfo;
        if (i > 13) {
            throw new IllegalArgumentException("no support for field less than SECOND");
        }
        Map<String, PatternInfo> map = this.fIntervalPatterns.get(str);
        if (map != null && (patternInfo = map.get(CALENDAR_FIELD_TO_PATTERN_LETTER[i])) != null) {
            return patternInfo;
        }
        return null;
    }

    public String getFallbackIntervalPattern() {
        return this.fFallbackIntervalPattern;
    }

    public void setFallbackIntervalPattern(String str) {
        if (this.frozen) {
            throw new UnsupportedOperationException("no modification is allowed after DII is frozen");
        }
        int iIndexOf = str.indexOf("{0}");
        int iIndexOf2 = str.indexOf("{1}");
        if (iIndexOf == -1 || iIndexOf2 == -1) {
            throw new IllegalArgumentException("no pattern {0} or pattern {1} in fallbackPattern");
        }
        if (iIndexOf > iIndexOf2) {
            this.fFirstDateInPtnIsLaterDate = true;
        }
        this.fFallbackIntervalPattern = str;
    }

    public boolean getDefaultOrder() {
        return this.fFirstDateInPtnIsLaterDate;
    }

    public Object clone() {
        if (this.frozen) {
            return this;
        }
        return cloneUnfrozenDII();
    }

    private Object cloneUnfrozenDII() {
        try {
            DateIntervalInfo dateIntervalInfo = (DateIntervalInfo) super.clone();
            dateIntervalInfo.fFallbackIntervalPattern = this.fFallbackIntervalPattern;
            dateIntervalInfo.fFirstDateInPtnIsLaterDate = this.fFirstDateInPtnIsLaterDate;
            if (this.fIntervalPatternsReadOnly) {
                dateIntervalInfo.fIntervalPatterns = this.fIntervalPatterns;
                dateIntervalInfo.fIntervalPatternsReadOnly = true;
            } else {
                dateIntervalInfo.fIntervalPatterns = cloneIntervalPatterns(this.fIntervalPatterns);
                dateIntervalInfo.fIntervalPatternsReadOnly = false;
            }
            dateIntervalInfo.frozen = false;
            return dateIntervalInfo;
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException("clone is not supported", e);
        }
    }

    private static Map<String, Map<String, PatternInfo>> cloneIntervalPatterns(Map<String, Map<String, PatternInfo>> map) {
        HashMap map2 = new HashMap();
        for (Map.Entry<String, Map<String, PatternInfo>> entry : map.entrySet()) {
            String key = entry.getKey();
            Map<String, PatternInfo> value = entry.getValue();
            HashMap map3 = new HashMap();
            for (Map.Entry<String, PatternInfo> entry2 : value.entrySet()) {
                map3.put(entry2.getKey(), entry2.getValue());
            }
            map2.put(key, map3);
        }
        return map2;
    }

    @Override
    public boolean isFrozen() {
        return this.frozen;
    }

    @Override
    public DateIntervalInfo freeze() {
        this.fIntervalPatternsReadOnly = true;
        this.frozen = true;
        return this;
    }

    @Override
    public DateIntervalInfo cloneAsThawed() {
        return (DateIntervalInfo) cloneUnfrozenDII();
    }

    static void parseSkeleton(String str, int[] iArr) {
        for (int i = 0; i < str.length(); i++) {
            int iCharAt = str.charAt(i) - 'A';
            iArr[iCharAt] = iArr[iCharAt] + 1;
        }
    }

    private static boolean stringNumeric(int i, int i2, char c) {
        if (c == 'M') {
            if (i > 2 || i2 <= 2) {
                if (i > 2 && i2 <= 2) {
                    return true;
                }
                return false;
            }
            return true;
        }
        return false;
    }

    DateIntervalFormat.BestMatchInfo getBestSkeleton(String str) {
        String strReplace;
        boolean z;
        String str2 = str;
        int[] iArr = new int[58];
        int[] iArr2 = new int[58];
        int i = 0;
        if (str2.indexOf(122) != -1) {
            strReplace = str2.replace('z', 'v');
            z = true;
        } else {
            strReplace = str2;
            z = false;
        }
        parseSkeleton(strReplace, iArr);
        int i2 = Integer.MAX_VALUE;
        Iterator<String> it = this.fIntervalPatterns.keySet().iterator();
        int i3 = 0;
        while (true) {
            if (it.hasNext()) {
                String next = it.next();
                for (int i4 = 0; i4 < iArr2.length; i4++) {
                    iArr2[i4] = 0;
                }
                parseSkeleton(next, iArr2);
                int iAbs = 0;
                int i5 = 1;
                for (int i6 = 0; i6 < iArr.length; i6++) {
                    int i7 = iArr[i6];
                    int i8 = iArr2[i6];
                    if (i7 != i8) {
                        if (i7 == 0 || i8 == 0) {
                            iAbs += 4096;
                            i5 = -1;
                        } else if (stringNumeric(i7, i8, (char) (i6 + 65))) {
                            iAbs += 256;
                        } else {
                            iAbs += Math.abs(i7 - i8);
                        }
                    }
                }
                if (iAbs < i2) {
                    str2 = next;
                    i2 = iAbs;
                    i3 = i5;
                }
                if (iAbs == 0) {
                    break;
                }
            } else {
                i = i3;
                break;
            }
        }
        if (z && i != -1) {
            i = 2;
        }
        return new DateIntervalFormat.BestMatchInfo(str2, i);
    }

    public boolean equals(Object obj) {
        if (obj instanceof DateIntervalInfo) {
            return this.fIntervalPatterns.equals(((DateIntervalInfo) obj).fIntervalPatterns);
        }
        return false;
    }

    public int hashCode() {
        return this.fIntervalPatterns.hashCode();
    }

    @Deprecated
    public Map<String, Set<String>> getPatterns() {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (Map.Entry<String, Map<String, PatternInfo>> entry : this.fIntervalPatterns.entrySet()) {
            linkedHashMap.put(entry.getKey(), new LinkedHashSet(entry.getValue().keySet()));
        }
        return linkedHashMap;
    }

    @Deprecated
    public Map<String, Map<String, PatternInfo>> getRawPatterns() {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (Map.Entry<String, Map<String, PatternInfo>> entry : this.fIntervalPatterns.entrySet()) {
            linkedHashMap.put(entry.getKey(), new LinkedHashMap(entry.getValue()));
        }
        return linkedHashMap;
    }
}
