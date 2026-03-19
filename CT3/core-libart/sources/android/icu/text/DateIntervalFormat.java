package android.icu.text;

import android.icu.impl.CalendarData;
import android.icu.impl.ICUCache;
import android.icu.impl.SimpleCache;
import android.icu.text.DateIntervalInfo;
import android.icu.util.Calendar;
import android.icu.util.DateInterval;
import android.icu.util.Output;
import android.icu.util.TimeZone;
import android.icu.util.ULocale;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DateIntervalFormat extends UFormat {
    private static ICUCache<String, Map<String, DateIntervalInfo.PatternInfo>> LOCAL_PATTERN_CACHE = new SimpleCache();
    private static final long serialVersionUID = 1;
    private SimpleDateFormat fDateFormat;
    private String fDatePattern;
    private String fDateTimeFormat;
    private Calendar fFromCalendar;
    private DateIntervalInfo fInfo;
    private transient Map<String, DateIntervalInfo.PatternInfo> fIntervalPatterns;
    private String fSkeleton;
    private String fTimePattern;
    private Calendar fToCalendar;
    private boolean isDateIntervalInfoDefault;

    static final class BestMatchInfo {
        final int bestMatchDistanceInfo;
        final String bestMatchSkeleton;

        BestMatchInfo(String bestSkeleton, int difference) {
            this.bestMatchSkeleton = bestSkeleton;
            this.bestMatchDistanceInfo = difference;
        }
    }

    private static final class SkeletonAndItsBestMatch {
        final String bestMatchSkeleton;
        final String skeleton;

        SkeletonAndItsBestMatch(String skeleton, String bestMatch) {
            this.skeleton = skeleton;
            this.bestMatchSkeleton = bestMatch;
        }
    }

    private DateIntervalFormat() {
        this.fSkeleton = null;
        this.fIntervalPatterns = null;
        this.fDatePattern = null;
        this.fTimePattern = null;
        this.fDateTimeFormat = null;
    }

    @Deprecated
    public DateIntervalFormat(String skeleton, DateIntervalInfo dtItvInfo, SimpleDateFormat simpleDateFormat) {
        this.fSkeleton = null;
        this.fIntervalPatterns = null;
        this.fDatePattern = null;
        this.fTimePattern = null;
        this.fDateTimeFormat = null;
        this.fDateFormat = simpleDateFormat;
        dtItvInfo.freeze();
        this.fSkeleton = skeleton;
        this.fInfo = dtItvInfo;
        this.isDateIntervalInfoDefault = false;
        this.fFromCalendar = (Calendar) this.fDateFormat.getCalendar().clone();
        this.fToCalendar = (Calendar) this.fDateFormat.getCalendar().clone();
        initializePattern(null);
    }

    private DateIntervalFormat(String skeleton, ULocale locale, SimpleDateFormat simpleDateFormat) {
        this.fSkeleton = null;
        this.fIntervalPatterns = null;
        this.fDatePattern = null;
        this.fTimePattern = null;
        this.fDateTimeFormat = null;
        this.fDateFormat = simpleDateFormat;
        this.fSkeleton = skeleton;
        this.fInfo = new DateIntervalInfo(locale).freeze();
        this.isDateIntervalInfoDefault = true;
        this.fFromCalendar = (Calendar) this.fDateFormat.getCalendar().clone();
        this.fToCalendar = (Calendar) this.fDateFormat.getCalendar().clone();
        initializePattern(LOCAL_PATTERN_CACHE);
    }

    public static final DateIntervalFormat getInstance(String skeleton) {
        return getInstance(skeleton, ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public static final DateIntervalFormat getInstance(String skeleton, Locale locale) {
        return getInstance(skeleton, ULocale.forLocale(locale));
    }

    public static final DateIntervalFormat getInstance(String skeleton, ULocale locale) {
        DateTimePatternGenerator generator = DateTimePatternGenerator.getInstance(locale);
        return new DateIntervalFormat(skeleton, locale, new SimpleDateFormat(generator.getBestPattern(skeleton), locale));
    }

    public static final DateIntervalFormat getInstance(String skeleton, DateIntervalInfo dtitvinf) {
        return getInstance(skeleton, ULocale.getDefault(ULocale.Category.FORMAT), dtitvinf);
    }

    public static final DateIntervalFormat getInstance(String skeleton, Locale locale, DateIntervalInfo dtitvinf) {
        return getInstance(skeleton, ULocale.forLocale(locale), dtitvinf);
    }

    public static final DateIntervalFormat getInstance(String skeleton, ULocale locale, DateIntervalInfo dtitvinf) {
        DateIntervalInfo dtitvinf2 = (DateIntervalInfo) dtitvinf.clone();
        DateTimePatternGenerator generator = DateTimePatternGenerator.getInstance(locale);
        return new DateIntervalFormat(skeleton, dtitvinf2, new SimpleDateFormat(generator.getBestPattern(skeleton), locale));
    }

    @Override
    public Object clone() {
        DateIntervalFormat other = (DateIntervalFormat) super.clone();
        other.fDateFormat = (SimpleDateFormat) this.fDateFormat.clone();
        other.fInfo = (DateIntervalInfo) this.fInfo.clone();
        other.fFromCalendar = (Calendar) this.fFromCalendar.clone();
        other.fToCalendar = (Calendar) this.fToCalendar.clone();
        other.fDatePattern = this.fDatePattern;
        other.fTimePattern = this.fTimePattern;
        other.fDateTimeFormat = this.fDateTimeFormat;
        return other;
    }

    @Override
    public final StringBuffer format(Object obj, StringBuffer appendTo, FieldPosition fieldPosition) {
        if (obj instanceof DateInterval) {
            return format((DateInterval) obj, appendTo, fieldPosition);
        }
        throw new IllegalArgumentException("Cannot format given Object (" + obj.getClass().getName() + ") as a DateInterval");
    }

    public final StringBuffer format(DateInterval dtInterval, StringBuffer appendTo, FieldPosition fieldPosition) {
        this.fFromCalendar.setTimeInMillis(dtInterval.getFromDate());
        this.fToCalendar.setTimeInMillis(dtInterval.getToDate());
        return format(this.fFromCalendar, this.fToCalendar, appendTo, fieldPosition);
    }

    @Deprecated
    public String getPatterns(Calendar fromCalendar, Calendar toCalendar, Output<String> part2) {
        int field;
        if (fromCalendar.get(0) != toCalendar.get(0)) {
            field = 0;
        } else if (fromCalendar.get(1) != toCalendar.get(1)) {
            field = 1;
        } else if (fromCalendar.get(2) != toCalendar.get(2)) {
            field = 2;
        } else if (fromCalendar.get(5) != toCalendar.get(5)) {
            field = 5;
        } else if (fromCalendar.get(9) != toCalendar.get(9)) {
            field = 9;
        } else if (fromCalendar.get(10) != toCalendar.get(10)) {
            field = 10;
        } else if (fromCalendar.get(12) != toCalendar.get(12)) {
            field = 12;
        } else if (fromCalendar.get(13) != toCalendar.get(13)) {
            field = 13;
        } else {
            return null;
        }
        DateIntervalInfo.PatternInfo intervalPattern = this.fIntervalPatterns.get(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field]);
        part2.value = intervalPattern.getSecondPart();
        return intervalPattern.getFirstPart();
    }

    public final StringBuffer format(Calendar fromCalendar, Calendar toCalendar, StringBuffer appendTo, FieldPosition pos) {
        int field;
        Calendar firstCal;
        Calendar secondCal;
        if (!fromCalendar.isEquivalentTo(toCalendar)) {
            throw new IllegalArgumentException("can not format on two different calendars");
        }
        if (fromCalendar.get(0) != toCalendar.get(0)) {
            field = 0;
        } else if (fromCalendar.get(1) != toCalendar.get(1)) {
            field = 1;
        } else if (fromCalendar.get(2) != toCalendar.get(2)) {
            field = 2;
        } else if (fromCalendar.get(5) != toCalendar.get(5)) {
            field = 5;
        } else if (fromCalendar.get(9) != toCalendar.get(9)) {
            field = 9;
        } else if (fromCalendar.get(10) != toCalendar.get(10)) {
            field = 10;
        } else if (fromCalendar.get(12) != toCalendar.get(12)) {
            field = 12;
        } else if (fromCalendar.get(13) != toCalendar.get(13)) {
            field = 13;
        } else {
            return this.fDateFormat.format(fromCalendar, appendTo, pos);
        }
        boolean fromToOnSameDay = field == 9 || field == 10 || field == 12 || field == 13;
        DateIntervalInfo.PatternInfo intervalPattern = this.fIntervalPatterns.get(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field]);
        if (intervalPattern == null) {
            if (this.fDateFormat.isFieldUnitIgnored(field)) {
                return this.fDateFormat.format(fromCalendar, appendTo, pos);
            }
            return fallbackFormat(fromCalendar, toCalendar, fromToOnSameDay, appendTo, pos);
        }
        if (intervalPattern.getFirstPart() == null) {
            return fallbackFormat(fromCalendar, toCalendar, fromToOnSameDay, appendTo, pos, intervalPattern.getSecondPart());
        }
        if (intervalPattern.firstDateInPtnIsLaterDate()) {
            firstCal = toCalendar;
            secondCal = fromCalendar;
        } else {
            firstCal = fromCalendar;
            secondCal = toCalendar;
        }
        String originalPattern = this.fDateFormat.toPattern();
        this.fDateFormat.applyPattern(intervalPattern.getFirstPart());
        this.fDateFormat.format(firstCal, appendTo, pos);
        if (intervalPattern.getSecondPart() != null) {
            this.fDateFormat.applyPattern(intervalPattern.getSecondPart());
            FieldPosition otherPos = new FieldPosition(pos.getField());
            this.fDateFormat.format(secondCal, appendTo, otherPos);
            if (pos.getEndIndex() != 0 || otherPos.getEndIndex() > 0) {
            }
        }
        this.fDateFormat.applyPattern(originalPattern);
        return appendTo;
    }

    private void adjustPosition(String combiningPattern, String pat0, FieldPosition pos0, String pat1, FieldPosition pos1, FieldPosition posResult) {
        int index0 = combiningPattern.indexOf("{0}");
        int index1 = combiningPattern.indexOf("{1}");
        if (index0 < 0 || index1 < 0) {
            return;
        }
        if (index0 < index1) {
            if (pos0.getEndIndex() > 0) {
                posResult.setBeginIndex(pos0.getBeginIndex() + index0);
                posResult.setEndIndex(pos0.getEndIndex() + index0);
                return;
            } else {
                if (pos1.getEndIndex() <= 0) {
                    return;
                }
                int index12 = index1 + (pat0.length() - 3);
                posResult.setBeginIndex(pos1.getBeginIndex() + index12);
                posResult.setEndIndex(pos1.getEndIndex() + index12);
                return;
            }
        }
        if (pos1.getEndIndex() > 0) {
            posResult.setBeginIndex(pos1.getBeginIndex() + index1);
            posResult.setEndIndex(pos1.getEndIndex() + index1);
        } else {
            if (pos0.getEndIndex() <= 0) {
                return;
            }
            int index02 = index0 + (pat1.length() - 3);
            posResult.setBeginIndex(pos0.getBeginIndex() + index02);
            posResult.setEndIndex(pos0.getEndIndex() + index02);
        }
    }

    private final StringBuffer fallbackFormat(Calendar fromCalendar, Calendar toCalendar, boolean fromToOnSameDay, StringBuffer appendTo, FieldPosition pos) {
        boolean formatDatePlusTimeRange;
        String fullPattern = null;
        if (!fromToOnSameDay || this.fDatePattern == null) {
            formatDatePlusTimeRange = false;
        } else {
            formatDatePlusTimeRange = this.fTimePattern != null;
        }
        if (formatDatePlusTimeRange) {
            fullPattern = this.fDateFormat.toPattern();
            this.fDateFormat.applyPattern(this.fTimePattern);
        }
        FieldPosition otherPos = new FieldPosition(pos.getField());
        StringBuffer earlierDate = this.fDateFormat.format(fromCalendar, new StringBuffer(64), pos);
        StringBuffer laterDate = this.fDateFormat.format(toCalendar, new StringBuffer(64), otherPos);
        String fallbackPattern = this.fInfo.getFallbackIntervalPattern();
        adjustPosition(fallbackPattern, earlierDate.toString(), pos, laterDate.toString(), otherPos, pos);
        String fallbackRange = MessageFormat.format(fallbackPattern, earlierDate.toString(), laterDate.toString());
        if (formatDatePlusTimeRange) {
            this.fDateFormat.applyPattern(this.fDatePattern);
            StringBuffer datePortion = new StringBuffer(64);
            otherPos.setBeginIndex(0);
            otherPos.setEndIndex(0);
            StringBuffer datePortion2 = this.fDateFormat.format(fromCalendar, datePortion, otherPos);
            adjustPosition(this.fDateTimeFormat, fallbackRange, pos, datePortion2.toString(), otherPos, pos);
            fallbackRange = MessageFormat.format(this.fDateTimeFormat, fallbackRange, datePortion2.toString());
        }
        appendTo.append(fallbackRange);
        if (formatDatePlusTimeRange) {
            this.fDateFormat.applyPattern(fullPattern);
        }
        return appendTo;
    }

    private final StringBuffer fallbackFormat(Calendar fromCalendar, Calendar toCalendar, boolean fromToOnSameDay, StringBuffer appendTo, FieldPosition pos, String fullPattern) {
        String originalPattern = this.fDateFormat.toPattern();
        this.fDateFormat.applyPattern(fullPattern);
        fallbackFormat(fromCalendar, toCalendar, fromToOnSameDay, appendTo, pos);
        this.fDateFormat.applyPattern(originalPattern);
        return appendTo;
    }

    @Override
    @Deprecated
    public Object parseObject(String source, ParsePosition parse_pos) {
        throw new UnsupportedOperationException("parsing is not supported");
    }

    public DateIntervalInfo getDateIntervalInfo() {
        return (DateIntervalInfo) this.fInfo.clone();
    }

    public void setDateIntervalInfo(DateIntervalInfo newItvPattern) {
        this.fInfo = (DateIntervalInfo) newItvPattern.clone();
        this.isDateIntervalInfoDefault = false;
        this.fInfo.freeze();
        if (this.fDateFormat == null) {
            return;
        }
        initializePattern(null);
    }

    public TimeZone getTimeZone() {
        if (this.fDateFormat != null) {
            return (TimeZone) this.fDateFormat.getTimeZone().clone();
        }
        return TimeZone.getDefault();
    }

    public void setTimeZone(TimeZone zone) {
        TimeZone zoneToSet = (TimeZone) zone.clone();
        if (this.fDateFormat != null) {
            this.fDateFormat.setTimeZone(zoneToSet);
        }
        if (this.fFromCalendar != null) {
            this.fFromCalendar.setTimeZone(zoneToSet);
        }
        if (this.fToCalendar == null) {
            return;
        }
        this.fToCalendar.setTimeZone(zoneToSet);
    }

    public DateFormat getDateFormat() {
        return (DateFormat) this.fDateFormat.clone();
    }

    private void initializePattern(ICUCache<String, Map<String, DateIntervalInfo.PatternInfo>> cache) {
        String fullPattern = this.fDateFormat.toPattern();
        ULocale locale = this.fDateFormat.getLocale();
        String key = null;
        Map<String, DateIntervalInfo.PatternInfo> patterns = null;
        if (cache != null) {
            if (this.fSkeleton != null) {
                key = locale.toString() + "+" + fullPattern + "+" + this.fSkeleton;
            } else {
                key = locale.toString() + "+" + fullPattern;
            }
            Map<String, DateIntervalInfo.PatternInfo> patterns2 = cache.get(key);
            patterns = patterns2;
        }
        if (patterns == null) {
            Map<String, DateIntervalInfo.PatternInfo> intervalPatterns = initializeIntervalPattern(fullPattern, locale);
            patterns = Collections.unmodifiableMap(intervalPatterns);
            if (cache != null) {
                cache.put(key, patterns);
            }
        }
        this.fIntervalPatterns = patterns;
    }

    private Map<String, DateIntervalInfo.PatternInfo> initializeIntervalPattern(String fullPattern, ULocale locale) {
        DateTimePatternGenerator dtpng = DateTimePatternGenerator.getInstance(locale);
        if (this.fSkeleton == null) {
            this.fSkeleton = dtpng.getSkeleton(fullPattern);
        }
        String skeleton = this.fSkeleton;
        HashMap<String, DateIntervalInfo.PatternInfo> intervalPatterns = new HashMap<>();
        StringBuilder date = new StringBuilder(skeleton.length());
        StringBuilder normalizedDate = new StringBuilder(skeleton.length());
        StringBuilder time = new StringBuilder(skeleton.length());
        StringBuilder normalizedTime = new StringBuilder(skeleton.length());
        getDateTimeSkeleton(skeleton, date, normalizedDate, time, normalizedTime);
        String dateSkeleton = date.toString();
        String timeSkeleton = time.toString();
        String normalizedDateSkeleton = normalizedDate.toString();
        String normalizedTimeSkeleton = normalizedTime.toString();
        if (time.length() != 0 && date.length() != 0) {
            CalendarData calData = new CalendarData(locale, (String) null);
            String[] patterns = calData.getDateTimePatterns();
            this.fDateTimeFormat = patterns[8];
        }
        boolean found = genSeparateDateTimePtn(normalizedDateSkeleton, normalizedTimeSkeleton, intervalPatterns, dtpng);
        if (!found) {
            if (time.length() != 0 && date.length() == 0) {
                String pattern = dtpng.getBestPattern(DateFormat.YEAR_NUM_MONTH_DAY + timeSkeleton);
                DateIntervalInfo.PatternInfo ptn = new DateIntervalInfo.PatternInfo(null, pattern, this.fInfo.getDefaultOrder());
                intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[5], ptn);
                intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[2], ptn);
                intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[1], ptn);
            }
            return intervalPatterns;
        }
        if (time.length() != 0) {
            if (date.length() == 0) {
                String pattern2 = dtpng.getBestPattern(DateFormat.YEAR_NUM_MONTH_DAY + timeSkeleton);
                DateIntervalInfo.PatternInfo ptn2 = new DateIntervalInfo.PatternInfo(null, pattern2, this.fInfo.getDefaultOrder());
                intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[5], ptn2);
                intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[2], ptn2);
                intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[1], ptn2);
            } else {
                if (!fieldExistsInSkeleton(5, dateSkeleton)) {
                    skeleton = DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[5] + skeleton;
                    genFallbackPattern(5, skeleton, intervalPatterns, dtpng);
                }
                if (!fieldExistsInSkeleton(2, dateSkeleton)) {
                    skeleton = DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[2] + skeleton;
                    genFallbackPattern(2, skeleton, intervalPatterns, dtpng);
                }
                if (!fieldExistsInSkeleton(1, dateSkeleton)) {
                    genFallbackPattern(1, DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[1] + skeleton, intervalPatterns, dtpng);
                }
                if (this.fDateTimeFormat == null) {
                    this.fDateTimeFormat = "{1} {0}";
                }
                String datePattern = dtpng.getBestPattern(dateSkeleton);
                concatSingleDate2TimeInterval(this.fDateTimeFormat, datePattern, 9, intervalPatterns);
                concatSingleDate2TimeInterval(this.fDateTimeFormat, datePattern, 10, intervalPatterns);
                concatSingleDate2TimeInterval(this.fDateTimeFormat, datePattern, 12, intervalPatterns);
            }
        }
        return intervalPatterns;
    }

    private void genFallbackPattern(int field, String skeleton, Map<String, DateIntervalInfo.PatternInfo> intervalPatterns, DateTimePatternGenerator dtpng) {
        String pattern = dtpng.getBestPattern(skeleton);
        DateIntervalInfo.PatternInfo ptn = new DateIntervalInfo.PatternInfo(null, pattern, this.fInfo.getDefaultOrder());
        intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field], ptn);
    }

    private static void getDateTimeSkeleton(String skeleton, StringBuilder dateSkeleton, StringBuilder normalizedDateSkeleton, StringBuilder timeSkeleton, StringBuilder normalizedTimeSkeleton) {
        int ECount = 0;
        int dCount = 0;
        int MCount = 0;
        int yCount = 0;
        int hCount = 0;
        int HCount = 0;
        int mCount = 0;
        int vCount = 0;
        int zCount = 0;
        for (int i = 0; i < skeleton.length(); i++) {
            char ch = skeleton.charAt(i);
            switch (ch) {
                case 'A':
                case 'K':
                case 'S':
                case 'V':
                case 'Z':
                case 'j':
                case 'k':
                case 's':
                    timeSkeleton.append(ch);
                    normalizedTimeSkeleton.append(ch);
                    break;
                case 'D':
                case 'F':
                case 'G':
                case 'L':
                case 'Q':
                case 'U':
                case 'W':
                case 'Y':
                case 'c':
                case 'e':
                case 'g':
                case 'l':
                case 'q':
                case 'r':
                case 'u':
                case 'w':
                    normalizedDateSkeleton.append(ch);
                    dateSkeleton.append(ch);
                    break;
                case 'E':
                    dateSkeleton.append(ch);
                    ECount++;
                    break;
                case 'H':
                    timeSkeleton.append(ch);
                    HCount++;
                    break;
                case 'M':
                    dateSkeleton.append(ch);
                    MCount++;
                    break;
                case 'a':
                    timeSkeleton.append(ch);
                    break;
                case 'd':
                    dateSkeleton.append(ch);
                    dCount++;
                    break;
                case 'h':
                    timeSkeleton.append(ch);
                    hCount++;
                    break;
                case 'm':
                    timeSkeleton.append(ch);
                    mCount++;
                    break;
                case 'v':
                    vCount++;
                    timeSkeleton.append(ch);
                    break;
                case 'y':
                    dateSkeleton.append(ch);
                    yCount++;
                    break;
                case 'z':
                    zCount++;
                    timeSkeleton.append(ch);
                    break;
            }
        }
        if (yCount != 0) {
            for (int i2 = 0; i2 < yCount; i2++) {
                normalizedDateSkeleton.append('y');
            }
        }
        if (MCount != 0) {
            if (MCount < 3) {
                normalizedDateSkeleton.append('M');
            } else {
                for (int i3 = 0; i3 < MCount && i3 < 5; i3++) {
                    normalizedDateSkeleton.append('M');
                }
            }
        }
        if (ECount != 0) {
            if (ECount <= 3) {
                normalizedDateSkeleton.append('E');
            } else {
                for (int i4 = 0; i4 < ECount && i4 < 5; i4++) {
                    normalizedDateSkeleton.append('E');
                }
            }
        }
        if (dCount != 0) {
            normalizedDateSkeleton.append('d');
        }
        if (HCount != 0) {
            normalizedTimeSkeleton.append('H');
        } else if (hCount != 0) {
            normalizedTimeSkeleton.append('h');
        }
        if (mCount != 0) {
            normalizedTimeSkeleton.append('m');
        }
        if (zCount != 0) {
            normalizedTimeSkeleton.append('z');
        }
        if (vCount == 0) {
            return;
        }
        normalizedTimeSkeleton.append('v');
    }

    private boolean genSeparateDateTimePtn(String dateSkeleton, String timeSkeleton, Map<String, DateIntervalInfo.PatternInfo> intervalPatterns, DateTimePatternGenerator dtpng) {
        String skeleton;
        if (timeSkeleton.length() != 0) {
            skeleton = timeSkeleton;
        } else {
            skeleton = dateSkeleton;
        }
        BestMatchInfo retValue = this.fInfo.getBestSkeleton(skeleton);
        String bestSkeleton = retValue.bestMatchSkeleton;
        int differenceInfo = retValue.bestMatchDistanceInfo;
        if (dateSkeleton.length() != 0) {
            this.fDatePattern = dtpng.getBestPattern(dateSkeleton);
        }
        if (timeSkeleton.length() != 0) {
            this.fTimePattern = dtpng.getBestPattern(timeSkeleton);
        }
        if (differenceInfo == -1) {
            return false;
        }
        if (timeSkeleton.length() == 0) {
            genIntervalPattern(5, skeleton, bestSkeleton, differenceInfo, intervalPatterns);
            SkeletonAndItsBestMatch skeletons = genIntervalPattern(2, skeleton, bestSkeleton, differenceInfo, intervalPatterns);
            if (skeletons != null) {
                bestSkeleton = skeletons.skeleton;
                skeleton = skeletons.bestMatchSkeleton;
            }
            genIntervalPattern(1, skeleton, bestSkeleton, differenceInfo, intervalPatterns);
        } else {
            genIntervalPattern(12, skeleton, bestSkeleton, differenceInfo, intervalPatterns);
            genIntervalPattern(10, skeleton, bestSkeleton, differenceInfo, intervalPatterns);
            genIntervalPattern(9, skeleton, bestSkeleton, differenceInfo, intervalPatterns);
        }
        return true;
    }

    private SkeletonAndItsBestMatch genIntervalPattern(int field, String skeleton, String bestSkeleton, int differenceInfo, Map<String, DateIntervalInfo.PatternInfo> intervalPatterns) {
        DateIntervalInfo.PatternInfo pattern;
        DateIntervalInfo.PatternInfo pattern2;
        SkeletonAndItsBestMatch retValue = null;
        DateIntervalInfo.PatternInfo pattern3 = this.fInfo.getIntervalPattern(bestSkeleton, field);
        if (pattern3 != null) {
            pattern = pattern3;
        } else {
            if (SimpleDateFormat.isFieldUnitIgnored(bestSkeleton, field)) {
                DateIntervalInfo.PatternInfo ptnInfo = new DateIntervalInfo.PatternInfo(this.fDateFormat.toPattern(), null, this.fInfo.getDefaultOrder());
                intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field], ptnInfo);
                return null;
            }
            if (field == 9) {
                DateIntervalInfo.PatternInfo pattern4 = this.fInfo.getIntervalPattern(bestSkeleton, 10);
                if (pattern4 != null) {
                    intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field], pattern4);
                    return null;
                }
                return null;
            }
            String fieldLetter = DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field];
            bestSkeleton = fieldLetter + bestSkeleton;
            skeleton = fieldLetter + skeleton;
            pattern3 = this.fInfo.getIntervalPattern(bestSkeleton, field);
            if (pattern3 == null && differenceInfo == 0) {
                BestMatchInfo tmpRetValue = this.fInfo.getBestSkeleton(skeleton);
                String tmpBestSkeleton = tmpRetValue.bestMatchSkeleton;
                differenceInfo = tmpRetValue.bestMatchDistanceInfo;
                if (tmpBestSkeleton.length() != 0 && differenceInfo != -1) {
                    pattern3 = this.fInfo.getIntervalPattern(tmpBestSkeleton, field);
                    bestSkeleton = tmpBestSkeleton;
                }
            }
            if (pattern3 != null) {
                retValue = new SkeletonAndItsBestMatch(skeleton, bestSkeleton);
                pattern = pattern3;
            }
        }
        if (pattern != null) {
            if (differenceInfo != 0) {
                String part1 = adjustFieldWidth(skeleton, bestSkeleton, pattern.getFirstPart(), differenceInfo);
                String part2 = adjustFieldWidth(skeleton, bestSkeleton, pattern.getSecondPart(), differenceInfo);
                pattern2 = new DateIntervalInfo.PatternInfo(part1, part2, pattern.firstDateInPtnIsLaterDate());
            } else {
                pattern2 = pattern;
            }
            intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field], pattern2);
        }
        return retValue;
    }

    private static String adjustFieldWidth(String inputSkeleton, String bestMatchSkeleton, String bestMatchIntervalPattern, int differenceInfo) {
        if (bestMatchIntervalPattern == null) {
            return null;
        }
        int[] inputSkeletonFieldWidth = new int[58];
        int[] bestMatchSkeletonFieldWidth = new int[58];
        DateIntervalInfo.parseSkeleton(inputSkeleton, inputSkeletonFieldWidth);
        DateIntervalInfo.parseSkeleton(bestMatchSkeleton, bestMatchSkeletonFieldWidth);
        if (differenceInfo == 2) {
            bestMatchIntervalPattern = bestMatchIntervalPattern.replace('v', 'z');
        }
        StringBuilder adjustedPtn = new StringBuilder(bestMatchIntervalPattern);
        boolean inQuote = false;
        char prevCh = 0;
        int count = 0;
        int adjustedPtnLength = adjustedPtn.length();
        int i = 0;
        while (i < adjustedPtnLength) {
            char ch = adjustedPtn.charAt(i);
            if (ch != prevCh && count > 0) {
                char skeletonChar = prevCh;
                if (prevCh == 'L') {
                    skeletonChar = 'M';
                }
                int fieldCount = bestMatchSkeletonFieldWidth[skeletonChar - 'A'];
                int inputFieldCount = inputSkeletonFieldWidth[skeletonChar - 'A'];
                if (fieldCount == count && inputFieldCount > fieldCount) {
                    int count2 = inputFieldCount - fieldCount;
                    for (int j = 0; j < count2; j++) {
                        adjustedPtn.insert(i, prevCh);
                    }
                    i += count2;
                    adjustedPtnLength += count2;
                }
                count = 0;
            }
            if (ch == '\'') {
                if (i + 1 < adjustedPtn.length() && adjustedPtn.charAt(i + 1) == '\'') {
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
        if (count > 0) {
            char skeletonChar2 = prevCh;
            if (prevCh == 'L') {
                skeletonChar2 = 'M';
            }
            int fieldCount2 = bestMatchSkeletonFieldWidth[skeletonChar2 - 'A'];
            int inputFieldCount2 = inputSkeletonFieldWidth[skeletonChar2 - 'A'];
            if (fieldCount2 == count && inputFieldCount2 > fieldCount2) {
                int count3 = inputFieldCount2 - fieldCount2;
                for (int j2 = 0; j2 < count3; j2++) {
                    adjustedPtn.append(prevCh);
                }
            }
        }
        return adjustedPtn.toString();
    }

    private void concatSingleDate2TimeInterval(String dtfmt, String datePattern, int field, Map<String, DateIntervalInfo.PatternInfo> intervalPatterns) {
        DateIntervalInfo.PatternInfo timeItvPtnInfo = intervalPatterns.get(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field]);
        if (timeItvPtnInfo == null) {
            return;
        }
        String timeIntervalPattern = timeItvPtnInfo.getFirstPart() + timeItvPtnInfo.getSecondPart();
        String pattern = MessageFormat.format(dtfmt, timeIntervalPattern, datePattern);
        intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field], DateIntervalInfo.genPatternInfo(pattern, timeItvPtnInfo.firstDateInPtnIsLaterDate()));
    }

    private static boolean fieldExistsInSkeleton(int field, String skeleton) {
        String fieldChar = DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field];
        return skeleton.indexOf(fieldChar) != -1;
    }

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        stream.defaultReadObject();
        initializePattern(this.isDateIntervalInfoDefault ? LOCAL_PATTERN_CACHE : null);
    }

    @Deprecated
    public Map<String, DateIntervalInfo.PatternInfo> getRawPatterns() {
        return this.fIntervalPatterns;
    }
}
