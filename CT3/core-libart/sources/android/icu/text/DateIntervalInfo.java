package android.icu.text;

import android.icu.impl.ICUCache;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SimpleCache;
import android.icu.impl.Utility;
import android.icu.text.DateIntervalFormat;
import android.icu.util.Calendar;
import android.icu.util.Freezable;
import android.icu.util.ICUCloneNotSupportedException;
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
    private static final String DEBUG_SKELETON = null;
    private static final int MINIMUM_SUPPORTED_CALENDAR_FIELD = 13;
    static final int currentSerialVersion = 1;
    private static final long serialVersionUID = 1;
    private String fFallbackIntervalPattern;
    private boolean fFirstDateInPtnIsLaterDate;
    private Map<String, Map<String, PatternInfo>> fIntervalPatterns;
    private transient boolean fIntervalPatternsReadOnly;
    private volatile transient boolean frozen;
    static final String[] CALENDAR_FIELD_TO_PATTERN_LETTER = {"G", DateFormat.YEAR, DateFormat.NUM_MONTH, "w", "W", DateFormat.DAY, "D", DateFormat.ABBR_WEEKDAY, "F", "a", "h", DateFormat.HOUR24, DateFormat.MINUTE, DateFormat.SECOND, "S", DateFormat.ABBR_SPECIFIC_TZ, " ", "Y", "e", "u", "g", "A", " ", " "};
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

        public PatternInfo(String firstPart, String secondPart, boolean firstDateInPtnIsLaterDate) {
            this.fIntervalPatternFirstPart = firstPart;
            this.fIntervalPatternSecondPart = secondPart;
            this.fFirstDateInPtnIsLaterDate = firstDateInPtnIsLaterDate;
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

        public boolean equals(Object a) {
            if (!(a instanceof PatternInfo)) {
                return false;
            }
            PatternInfo patternInfo = (PatternInfo) a;
            return Utility.objectEquals(this.fIntervalPatternFirstPart, patternInfo.fIntervalPatternFirstPart) && Utility.objectEquals(this.fIntervalPatternSecondPart, this.fIntervalPatternSecondPart) && this.fFirstDateInPtnIsLaterDate == patternInfo.fFirstDateInPtnIsLaterDate;
        }

        public int hashCode() {
            int hash = this.fIntervalPatternFirstPart != null ? this.fIntervalPatternFirstPart.hashCode() : 0;
            if (this.fIntervalPatternSecondPart != null) {
                hash ^= this.fIntervalPatternSecondPart.hashCode();
            }
            if (this.fFirstDateInPtnIsLaterDate) {
                return hash ^ (-1);
            }
            return hash;
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

    public DateIntervalInfo(ULocale locale) {
        this.fFirstDateInPtnIsLaterDate = false;
        this.fIntervalPatterns = null;
        this.frozen = false;
        this.fIntervalPatternsReadOnly = false;
        initializeData(locale);
    }

    public DateIntervalInfo(Locale locale) {
        this(ULocale.forLocale(locale));
    }

    private void initializeData(ULocale locale) {
        String key = locale.toString();
        DateIntervalInfo dii = DIICACHE.get(key);
        if (dii == null) {
            setup(locale);
            this.fIntervalPatternsReadOnly = true;
            DIICACHE.put(key, ((DateIntervalInfo) clone()).freeze());
            return;
        }
        initializeFromReadOnlyPatterns(dii);
    }

    private void initializeFromReadOnlyPatterns(DateIntervalInfo dii) {
        this.fFallbackIntervalPattern = dii.fFallbackIntervalPattern;
        this.fFirstDateInPtnIsLaterDate = dii.fFirstDateInPtnIsLaterDate;
        this.fIntervalPatterns = dii.fIntervalPatterns;
        this.fIntervalPatternsReadOnly = true;
    }

    private void setup(ULocale locale) {
        ULocale currentLocale;
        ULocale currentLocale2;
        this.fIntervalPatterns = new HashMap(19);
        this.fFallbackIntervalPattern = "{0} – {1}";
        HashSet<String> skeletonKeyPairs = new HashSet<>();
        try {
            String calendarTypeToUse = locale.getKeywordValue("calendar");
            if (calendarTypeToUse == null) {
                String[] preferredCalendarTypes = Calendar.getKeywordValuesForLocale("calendar", locale, true);
                calendarTypeToUse = preferredCalendarTypes[0];
            }
            if (calendarTypeToUse == null) {
                calendarTypeToUse = "gregorian";
                currentLocale = locale;
                while (true) {
                    try {
                        String name = currentLocale.getName();
                        if (name.length() == 0) {
                            return;
                        }
                        ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", currentLocale);
                        ICUResourceBundle itvDtPtnResource = rb.getWithFallback("calendar/" + calendarTypeToUse + "/intervalFormats");
                        String fallback = itvDtPtnResource.getStringWithFallback(FALLBACK_STRING);
                        setFallbackIntervalPattern(fallback);
                        int size = itvDtPtnResource.getSize();
                        for (int index = 0; index < size; index++) {
                            String skeleton = itvDtPtnResource.get(index).getKey();
                            if (skeleton.compareTo(FALLBACK_STRING) != 0) {
                                ICUResourceBundle intervalPatterns = (ICUResourceBundle) itvDtPtnResource.get(skeleton);
                                int ptnNum = intervalPatterns.getSize();
                                for (int ptnIndex = 0; ptnIndex < ptnNum; ptnIndex++) {
                                    String key = intervalPatterns.get(ptnIndex).getKey();
                                    String skeletonKeyPair = skeleton + "\u0001" + key;
                                    if (!skeletonKeyPairs.contains(skeletonKeyPair)) {
                                        skeletonKeyPairs.add(skeletonKeyPair);
                                        String pattern = intervalPatterns.get(ptnIndex).getString();
                                        int calendarField = -1;
                                        if (key.equals(CALENDAR_FIELD_TO_PATTERN_LETTER[1])) {
                                            calendarField = 1;
                                        } else if (key.equals(CALENDAR_FIELD_TO_PATTERN_LETTER[2])) {
                                            calendarField = 2;
                                        } else if (key.equals(CALENDAR_FIELD_TO_PATTERN_LETTER[5])) {
                                            calendarField = 5;
                                        } else if (key.equals(CALENDAR_FIELD_TO_PATTERN_LETTER[9])) {
                                            calendarField = 9;
                                        } else if (key.equals(CALENDAR_FIELD_TO_PATTERN_LETTER[10]) || key.equals(CALENDAR_FIELD_TO_PATTERN_LETTER[11])) {
                                            calendarField = 10;
                                            key = CALENDAR_FIELD_TO_PATTERN_LETTER[10];
                                        } else if (key.equals(CALENDAR_FIELD_TO_PATTERN_LETTER[12])) {
                                            calendarField = 12;
                                        } else if (key.equals(CALENDAR_FIELD_TO_PATTERN_LETTER[13])) {
                                            calendarField = 13;
                                        }
                                        if (calendarField != -1) {
                                            if (DEBUG_SKELETON != null && DEBUG_SKELETON.equals(skeleton)) {
                                                Map<String, PatternInfo> oldValue = this.fIntervalPatterns.get(skeleton);
                                                setIntervalPatternInternally(skeleton, key, pattern);
                                                Map<String, PatternInfo> newValue = this.fIntervalPatterns.get(skeleton);
                                                if (!Utility.objectEquals(oldValue, newValue)) {
                                                    System.out.println("\n" + currentLocale + ", skeleton: " + skeleton + ", oldValue: " + oldValue + ", newValue: " + newValue);
                                                }
                                            } else {
                                                setIntervalPatternInternally(skeleton, key, pattern);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        try {
                            UResourceBundle parentNameBundle = rb.get("%%Parent");
                            currentLocale2 = new ULocale(parentNameBundle.getString());
                        } catch (MissingResourceException e) {
                            currentLocale2 = currentLocale.getFallback();
                        }
                        if (currentLocale2 == null) {
                            return;
                        }
                        if (currentLocale2.getBaseName().equals("root")) {
                            return;
                        }
                        currentLocale = currentLocale2;
                    } catch (MissingResourceException e2) {
                        return;
                    }
                }
            }
            currentLocale = currentLocale2;
        } catch (MissingResourceException e3) {
        }
    }

    private static int splitPatternInto2Part(String intervalPattern) {
        boolean inQuote = false;
        char prevCh = 0;
        int count = 0;
        int[] patternRepeated = new int[58];
        boolean foundRepetition = false;
        int i = 0;
        while (true) {
            if (i >= intervalPattern.length()) {
                break;
            }
            char ch = intervalPattern.charAt(i);
            if (ch != prevCh && count > 0) {
                int repeated = patternRepeated[prevCh - 'A'];
                if (repeated == 0) {
                    patternRepeated[prevCh - 'A'] = 1;
                    count = 0;
                } else {
                    foundRepetition = true;
                    break;
                }
            }
            if (ch == '\'') {
                if (i + 1 < intervalPattern.length() && intervalPattern.charAt(i + 1) == '\'') {
                    i++;
                } else {
                    inQuote = !inQuote;
                }
            } else if (!inQuote && ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z'))) {
                prevCh = ch;
                count++;
            }
            i++;
        }
        if (count > 0 && !foundRepetition && patternRepeated[prevCh - 'A'] == 0) {
            count = 0;
        }
        return i - count;
    }

    public void setIntervalPattern(String skeleton, int lrgDiffCalUnit, String intervalPattern) {
        if (this.frozen) {
            throw new UnsupportedOperationException("no modification is allowed after DII is frozen");
        }
        if (lrgDiffCalUnit > 13) {
            throw new IllegalArgumentException("calendar field is larger than MINIMUM_SUPPORTED_CALENDAR_FIELD");
        }
        if (this.fIntervalPatternsReadOnly) {
            this.fIntervalPatterns = cloneIntervalPatterns(this.fIntervalPatterns);
            this.fIntervalPatternsReadOnly = false;
        }
        PatternInfo ptnInfo = setIntervalPatternInternally(skeleton, CALENDAR_FIELD_TO_PATTERN_LETTER[lrgDiffCalUnit], intervalPattern);
        if (lrgDiffCalUnit == 11) {
            setIntervalPattern(skeleton, CALENDAR_FIELD_TO_PATTERN_LETTER[9], ptnInfo);
            setIntervalPattern(skeleton, CALENDAR_FIELD_TO_PATTERN_LETTER[10], ptnInfo);
        } else {
            if (lrgDiffCalUnit != 5 && lrgDiffCalUnit != 7) {
                return;
            }
            setIntervalPattern(skeleton, CALENDAR_FIELD_TO_PATTERN_LETTER[5], ptnInfo);
        }
    }

    private PatternInfo setIntervalPatternInternally(String skeleton, String lrgDiffCalUnit, String intervalPattern) {
        Map<String, PatternInfo> patternsOfOneSkeleton = this.fIntervalPatterns.get(skeleton);
        boolean emptyHash = false;
        if (patternsOfOneSkeleton == null) {
            patternsOfOneSkeleton = new HashMap<>();
            emptyHash = true;
        }
        boolean order = this.fFirstDateInPtnIsLaterDate;
        if (intervalPattern.startsWith(LATEST_FIRST_PREFIX)) {
            order = true;
            int prefixLength = LATEST_FIRST_PREFIX.length();
            intervalPattern = intervalPattern.substring(prefixLength, intervalPattern.length());
        } else if (intervalPattern.startsWith(EARLIEST_FIRST_PREFIX)) {
            order = false;
            int earliestFirstLength = EARLIEST_FIRST_PREFIX.length();
            intervalPattern = intervalPattern.substring(earliestFirstLength, intervalPattern.length());
        }
        PatternInfo itvPtnInfo = genPatternInfo(intervalPattern, order);
        patternsOfOneSkeleton.put(lrgDiffCalUnit, itvPtnInfo);
        if (emptyHash) {
            this.fIntervalPatterns.put(skeleton, patternsOfOneSkeleton);
        }
        return itvPtnInfo;
    }

    private void setIntervalPattern(String skeleton, String lrgDiffCalUnit, PatternInfo ptnInfo) {
        Map<String, PatternInfo> patternsOfOneSkeleton = this.fIntervalPatterns.get(skeleton);
        patternsOfOneSkeleton.put(lrgDiffCalUnit, ptnInfo);
    }

    @Deprecated
    public static PatternInfo genPatternInfo(String intervalPattern, boolean laterDateFirst) {
        int splitPoint = splitPatternInto2Part(intervalPattern);
        String firstPart = intervalPattern.substring(0, splitPoint);
        String secondPart = null;
        if (splitPoint < intervalPattern.length()) {
            secondPart = intervalPattern.substring(splitPoint, intervalPattern.length());
        }
        return new PatternInfo(firstPart, secondPart, laterDateFirst);
    }

    public PatternInfo getIntervalPattern(String skeleton, int field) {
        PatternInfo intervalPattern;
        if (field > 13) {
            throw new IllegalArgumentException("no support for field less than SECOND");
        }
        Map<String, PatternInfo> patternsOfOneSkeleton = this.fIntervalPatterns.get(skeleton);
        if (patternsOfOneSkeleton == null || (intervalPattern = patternsOfOneSkeleton.get(CALENDAR_FIELD_TO_PATTERN_LETTER[field])) == null) {
            return null;
        }
        return intervalPattern;
    }

    public String getFallbackIntervalPattern() {
        return this.fFallbackIntervalPattern;
    }

    public void setFallbackIntervalPattern(String fallbackPattern) {
        if (this.frozen) {
            throw new UnsupportedOperationException("no modification is allowed after DII is frozen");
        }
        int firstPatternIndex = fallbackPattern.indexOf("{0}");
        int secondPatternIndex = fallbackPattern.indexOf("{1}");
        if (firstPatternIndex == -1 || secondPatternIndex == -1) {
            throw new IllegalArgumentException("no pattern {0} or pattern {1} in fallbackPattern");
        }
        if (firstPatternIndex > secondPatternIndex) {
            this.fFirstDateInPtnIsLaterDate = true;
        }
        this.fFallbackIntervalPattern = fallbackPattern;
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
            DateIntervalInfo other = (DateIntervalInfo) super.clone();
            other.fFallbackIntervalPattern = this.fFallbackIntervalPattern;
            other.fFirstDateInPtnIsLaterDate = this.fFirstDateInPtnIsLaterDate;
            if (this.fIntervalPatternsReadOnly) {
                other.fIntervalPatterns = this.fIntervalPatterns;
                other.fIntervalPatternsReadOnly = true;
            } else {
                other.fIntervalPatterns = cloneIntervalPatterns(this.fIntervalPatterns);
                other.fIntervalPatternsReadOnly = false;
            }
            other.frozen = false;
            return other;
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException("clone is not supported", e);
        }
    }

    private static Map<String, Map<String, PatternInfo>> cloneIntervalPatterns(Map<String, Map<String, PatternInfo>> patterns) {
        Map<String, Map<String, PatternInfo>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, PatternInfo>> skeletonEntry : patterns.entrySet()) {
            String skeleton = skeletonEntry.getKey();
            Map<String, PatternInfo> patternsOfOneSkeleton = skeletonEntry.getValue();
            Map<String, PatternInfo> oneSetPtn = new HashMap<>();
            for (Map.Entry<String, PatternInfo> calEntry : patternsOfOneSkeleton.entrySet()) {
                String calField = calEntry.getKey();
                PatternInfo value = calEntry.getValue();
                oneSetPtn.put(calField, value);
            }
            result.put(skeleton, oneSetPtn);
        }
        return result;
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
        DateIntervalInfo result = (DateIntervalInfo) cloneUnfrozenDII();
        return result;
    }

    static void parseSkeleton(String skeleton, int[] skeletonFieldWidth) {
        for (int i = 0; i < skeleton.length(); i++) {
            int iCharAt = skeleton.charAt(i) - 'A';
            skeletonFieldWidth[iCharAt] = skeletonFieldWidth[iCharAt] + 1;
        }
    }

    private static boolean stringNumeric(int fieldWidth, int anotherFieldWidth, char patternLetter) {
        if (patternLetter == 'M') {
            if (fieldWidth > 2 || anotherFieldWidth <= 2) {
                if (fieldWidth > 2 && anotherFieldWidth <= 2) {
                    return true;
                }
                return false;
            }
            return true;
        }
        return false;
    }

    DateIntervalFormat.BestMatchInfo getBestSkeleton(String inputSkeleton) {
        String bestSkeleton = inputSkeleton;
        int[] inputSkeletonFieldWidth = new int[58];
        int[] skeletonFieldWidth = new int[58];
        boolean replaceZWithV = false;
        if (inputSkeleton.indexOf(122) != -1) {
            inputSkeleton = inputSkeleton.replace('z', 'v');
            replaceZWithV = true;
        }
        parseSkeleton(inputSkeleton, inputSkeletonFieldWidth);
        int bestDistance = Integer.MAX_VALUE;
        int bestFieldDifference = 0;
        Iterator skeleton$iterator = this.fIntervalPatterns.keySet().iterator();
        while (true) {
            if (!skeleton$iterator.hasNext()) {
                break;
            }
            String skeleton = (String) skeleton$iterator.next();
            for (int i = 0; i < skeletonFieldWidth.length; i++) {
                skeletonFieldWidth[i] = 0;
            }
            parseSkeleton(skeleton, skeletonFieldWidth);
            int distance = 0;
            int fieldDifference = 1;
            for (int i2 = 0; i2 < inputSkeletonFieldWidth.length; i2++) {
                int inputFieldWidth = inputSkeletonFieldWidth[i2];
                int fieldWidth = skeletonFieldWidth[i2];
                if (inputFieldWidth != fieldWidth) {
                    if (inputFieldWidth == 0) {
                        fieldDifference = -1;
                        distance += 4096;
                    } else if (fieldWidth == 0) {
                        fieldDifference = -1;
                        distance += 4096;
                    } else if (stringNumeric(inputFieldWidth, fieldWidth, (char) (i2 + 65))) {
                        distance += 256;
                    } else {
                        distance += Math.abs(inputFieldWidth - fieldWidth);
                    }
                }
            }
            if (distance < bestDistance) {
                bestSkeleton = skeleton;
                bestDistance = distance;
                bestFieldDifference = fieldDifference;
            }
            if (distance == 0) {
                bestFieldDifference = 0;
                break;
            }
        }
        if (replaceZWithV && bestFieldDifference != -1) {
            bestFieldDifference = 2;
        }
        return new DateIntervalFormat.BestMatchInfo(bestSkeleton, bestFieldDifference);
    }

    public boolean equals(Object a) {
        if (a instanceof DateIntervalInfo) {
            DateIntervalInfo dtInfo = (DateIntervalInfo) a;
            return this.fIntervalPatterns.equals(dtInfo.fIntervalPatterns);
        }
        return false;
    }

    public int hashCode() {
        return this.fIntervalPatterns.hashCode();
    }

    @Deprecated
    public Map<String, Set<String>> getPatterns() {
        LinkedHashMap<String, Set<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, PatternInfo>> entry : this.fIntervalPatterns.entrySet()) {
            result.put(entry.getKey(), new LinkedHashSet<>(entry.getValue().keySet()));
        }
        return result;
    }

    @Deprecated
    public Map<String, Map<String, PatternInfo>> getRawPatterns() {
        LinkedHashMap<String, Map<String, PatternInfo>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, PatternInfo>> entry : this.fIntervalPatterns.entrySet()) {
            result.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        return result;
    }
}
