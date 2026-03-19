package android.icu.text;

import android.icu.impl.ICUCache;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SimpleCache;
import android.icu.impl.SimpleFormatterImpl;
import android.icu.text.DateIntervalInfo;
import android.icu.text.MessagePattern;
import android.icu.util.Calendar;
import android.icu.util.DateInterval;
import android.icu.util.Output;
import android.icu.util.TimeZone;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
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

        BestMatchInfo(String str, int i) {
            this.bestMatchSkeleton = str;
            this.bestMatchDistanceInfo = i;
        }
    }

    private static final class SkeletonAndItsBestMatch {
        final String bestMatchSkeleton;
        final String skeleton;

        SkeletonAndItsBestMatch(String str, String str2) {
            this.skeleton = str;
            this.bestMatchSkeleton = str2;
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
    public DateIntervalFormat(String str, DateIntervalInfo dateIntervalInfo, SimpleDateFormat simpleDateFormat) {
        this.fSkeleton = null;
        this.fIntervalPatterns = null;
        this.fDatePattern = null;
        this.fTimePattern = null;
        this.fDateTimeFormat = null;
        this.fDateFormat = simpleDateFormat;
        dateIntervalInfo.freeze();
        this.fSkeleton = str;
        this.fInfo = dateIntervalInfo;
        this.isDateIntervalInfoDefault = false;
        this.fFromCalendar = (Calendar) this.fDateFormat.getCalendar().clone();
        this.fToCalendar = (Calendar) this.fDateFormat.getCalendar().clone();
        initializePattern(null);
    }

    private DateIntervalFormat(String str, ULocale uLocale, SimpleDateFormat simpleDateFormat) {
        this.fSkeleton = null;
        this.fIntervalPatterns = null;
        this.fDatePattern = null;
        this.fTimePattern = null;
        this.fDateTimeFormat = null;
        this.fDateFormat = simpleDateFormat;
        this.fSkeleton = str;
        this.fInfo = new DateIntervalInfo(uLocale).freeze();
        this.isDateIntervalInfoDefault = true;
        this.fFromCalendar = (Calendar) this.fDateFormat.getCalendar().clone();
        this.fToCalendar = (Calendar) this.fDateFormat.getCalendar().clone();
        initializePattern(LOCAL_PATTERN_CACHE);
    }

    public static final DateIntervalFormat getInstance(String str) {
        return getInstance(str, ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public static final DateIntervalFormat getInstance(String str, Locale locale) {
        return getInstance(str, ULocale.forLocale(locale));
    }

    public static final DateIntervalFormat getInstance(String str, ULocale uLocale) {
        return new DateIntervalFormat(str, uLocale, new SimpleDateFormat(DateTimePatternGenerator.getInstance(uLocale).getBestPattern(str), uLocale));
    }

    public static final DateIntervalFormat getInstance(String str, DateIntervalInfo dateIntervalInfo) {
        return getInstance(str, ULocale.getDefault(ULocale.Category.FORMAT), dateIntervalInfo);
    }

    public static final DateIntervalFormat getInstance(String str, Locale locale, DateIntervalInfo dateIntervalInfo) {
        return getInstance(str, ULocale.forLocale(locale), dateIntervalInfo);
    }

    public static final DateIntervalFormat getInstance(String str, ULocale uLocale, DateIntervalInfo dateIntervalInfo) {
        return new DateIntervalFormat(str, (DateIntervalInfo) dateIntervalInfo.clone(), new SimpleDateFormat(DateTimePatternGenerator.getInstance(uLocale).getBestPattern(str), uLocale));
    }

    @Override
    public synchronized Object clone() {
        DateIntervalFormat dateIntervalFormat;
        dateIntervalFormat = (DateIntervalFormat) super.clone();
        dateIntervalFormat.fDateFormat = (SimpleDateFormat) this.fDateFormat.clone();
        dateIntervalFormat.fInfo = (DateIntervalInfo) this.fInfo.clone();
        dateIntervalFormat.fFromCalendar = (Calendar) this.fFromCalendar.clone();
        dateIntervalFormat.fToCalendar = (Calendar) this.fToCalendar.clone();
        dateIntervalFormat.fDatePattern = this.fDatePattern;
        dateIntervalFormat.fTimePattern = this.fTimePattern;
        dateIntervalFormat.fDateTimeFormat = this.fDateTimeFormat;
        return dateIntervalFormat;
    }

    @Override
    public final StringBuffer format(Object obj, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        if (obj instanceof DateInterval) {
            return format((DateInterval) obj, stringBuffer, fieldPosition);
        }
        throw new IllegalArgumentException("Cannot format given Object (" + obj.getClass().getName() + ") as a DateInterval");
    }

    public final synchronized StringBuffer format(DateInterval dateInterval, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        this.fFromCalendar.setTimeInMillis(dateInterval.getFromDate());
        this.fToCalendar.setTimeInMillis(dateInterval.getToDate());
        return format(this.fFromCalendar, this.fToCalendar, stringBuffer, fieldPosition);
    }

    @Deprecated
    public String getPatterns(Calendar calendar, Calendar calendar2, Output<String> output) {
        char c = 0;
        if (calendar.get(0) == calendar2.get(0)) {
            if (calendar.get(1) != calendar2.get(1)) {
                c = 1;
            } else if (calendar.get(2) != calendar2.get(2)) {
                c = 2;
            } else if (calendar.get(5) != calendar2.get(5)) {
                c = 5;
            } else if (calendar.get(9) != calendar2.get(9)) {
                c = '\t';
            } else if (calendar.get(10) != calendar2.get(10)) {
                c = '\n';
            } else if (calendar.get(12) != calendar2.get(12)) {
                c = '\f';
            } else {
                if (calendar.get(13) == calendar2.get(13)) {
                    return null;
                }
                c = '\r';
            }
        }
        DateIntervalInfo.PatternInfo patternInfo = this.fIntervalPatterns.get(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[c]);
        output.value = patternInfo.getSecondPart();
        return patternInfo.getFirstPart();
    }

    public final synchronized StringBuffer format(Calendar calendar, Calendar calendar2, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        Calendar calendar3 = calendar;
        Calendar calendar4 = calendar2;
        synchronized (this) {
            if (!calendar.isEquivalentTo(calendar2)) {
                throw new IllegalArgumentException("can not format on two different calendars");
            }
            int i = 2;
            if (calendar3.get(0) != calendar4.get(0)) {
                i = 0;
            } else if (calendar3.get(1) != calendar4.get(1)) {
                i = 1;
            } else if (calendar3.get(2) == calendar4.get(2)) {
                if (calendar3.get(5) != calendar4.get(5)) {
                    i = 5;
                } else if (calendar3.get(9) != calendar4.get(9)) {
                    i = 9;
                } else if (calendar3.get(10) != calendar4.get(10)) {
                    i = 10;
                } else if (calendar3.get(12) != calendar4.get(12)) {
                    i = 12;
                } else {
                    if (calendar3.get(13) == calendar4.get(13)) {
                        return this.fDateFormat.format(calendar3, stringBuffer, fieldPosition);
                    }
                    i = 13;
                }
            }
            boolean z = i == 9 || i == 10 || i == 12 || i == 13;
            DateIntervalInfo.PatternInfo patternInfo = this.fIntervalPatterns.get(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[i]);
            if (patternInfo == null) {
                if (this.fDateFormat.isFieldUnitIgnored(i)) {
                    return this.fDateFormat.format(calendar3, stringBuffer, fieldPosition);
                }
                return fallbackFormat(calendar3, calendar4, z, stringBuffer, fieldPosition);
            }
            if (patternInfo.getFirstPart() == null) {
                return fallbackFormat(calendar3, calendar4, z, stringBuffer, fieldPosition, patternInfo.getSecondPart());
            }
            if (patternInfo.firstDateInPtnIsLaterDate()) {
                calendar4 = calendar3;
                calendar3 = calendar4;
            }
            String pattern = this.fDateFormat.toPattern();
            this.fDateFormat.applyPattern(patternInfo.getFirstPart());
            this.fDateFormat.format(calendar3, stringBuffer, fieldPosition);
            if (patternInfo.getSecondPart() != null) {
                this.fDateFormat.applyPattern(patternInfo.getSecondPart());
                FieldPosition fieldPosition2 = new FieldPosition(fieldPosition.getField());
                this.fDateFormat.format(calendar4, stringBuffer, fieldPosition2);
                if (fieldPosition.getEndIndex() == 0 && fieldPosition2.getEndIndex() > 0) {
                    fieldPosition.setBeginIndex(fieldPosition2.getBeginIndex());
                    fieldPosition.setEndIndex(fieldPosition2.getEndIndex());
                }
            }
            this.fDateFormat.applyPattern(pattern);
            return stringBuffer;
        }
    }

    private void adjustPosition(String str, String str2, FieldPosition fieldPosition, String str3, FieldPosition fieldPosition2, FieldPosition fieldPosition3) {
        int iIndexOf = str.indexOf("{0}");
        int iIndexOf2 = str.indexOf("{1}");
        if (iIndexOf < 0 || iIndexOf2 < 0) {
            return;
        }
        if (iIndexOf < iIndexOf2) {
            if (fieldPosition.getEndIndex() > 0) {
                fieldPosition3.setBeginIndex(fieldPosition.getBeginIndex() + iIndexOf);
                fieldPosition3.setEndIndex(fieldPosition.getEndIndex() + iIndexOf);
                return;
            } else {
                if (fieldPosition2.getEndIndex() > 0) {
                    int length = iIndexOf2 + (str2.length() - 3);
                    fieldPosition3.setBeginIndex(fieldPosition2.getBeginIndex() + length);
                    fieldPosition3.setEndIndex(fieldPosition2.getEndIndex() + length);
                    return;
                }
                return;
            }
        }
        if (fieldPosition2.getEndIndex() > 0) {
            fieldPosition3.setBeginIndex(fieldPosition2.getBeginIndex() + iIndexOf2);
            fieldPosition3.setEndIndex(fieldPosition2.getEndIndex() + iIndexOf2);
        } else if (fieldPosition.getEndIndex() > 0) {
            int length2 = iIndexOf + (str3.length() - 3);
            fieldPosition3.setBeginIndex(fieldPosition.getBeginIndex() + length2);
            fieldPosition3.setEndIndex(fieldPosition.getEndIndex() + length2);
        }
    }

    private final StringBuffer fallbackFormat(Calendar calendar, Calendar calendar2, boolean z, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        String pattern;
        boolean z2 = (!z || this.fDatePattern == null || this.fTimePattern == null) ? false : true;
        if (z2) {
            pattern = this.fDateFormat.toPattern();
            this.fDateFormat.applyPattern(this.fTimePattern);
        } else {
            pattern = null;
        }
        String str = pattern;
        FieldPosition fieldPosition2 = new FieldPosition(fieldPosition.getField());
        StringBuffer stringBuffer2 = this.fDateFormat.format(calendar, new StringBuffer(64), fieldPosition);
        StringBuffer stringBuffer3 = this.fDateFormat.format(calendar2, new StringBuffer(64), fieldPosition2);
        String fallbackIntervalPattern = this.fInfo.getFallbackIntervalPattern();
        adjustPosition(fallbackIntervalPattern, stringBuffer2.toString(), fieldPosition, stringBuffer3.toString(), fieldPosition2, fieldPosition);
        String rawPattern = SimpleFormatterImpl.formatRawPattern(fallbackIntervalPattern, 2, 2, stringBuffer2, stringBuffer3);
        if (z2) {
            this.fDateFormat.applyPattern(this.fDatePattern);
            StringBuffer stringBuffer4 = new StringBuffer(64);
            fieldPosition2.setBeginIndex(0);
            fieldPosition2.setEndIndex(0);
            StringBuffer stringBuffer5 = this.fDateFormat.format(calendar, stringBuffer4, fieldPosition2);
            adjustPosition(this.fDateTimeFormat, rawPattern, fieldPosition, stringBuffer5.toString(), fieldPosition2, fieldPosition);
            MessageFormat messageFormat = new MessageFormat("");
            messageFormat.applyPattern(this.fDateTimeFormat, MessagePattern.ApostropheMode.DOUBLE_REQUIRED);
            rawPattern = messageFormat.format(new Object[]{rawPattern, stringBuffer5}, new StringBuffer(128), new FieldPosition(0)).toString();
        }
        stringBuffer.append(rawPattern);
        if (z2) {
            this.fDateFormat.applyPattern(str);
        }
        return stringBuffer;
    }

    private final StringBuffer fallbackFormat(Calendar calendar, Calendar calendar2, boolean z, StringBuffer stringBuffer, FieldPosition fieldPosition, String str) {
        String pattern = this.fDateFormat.toPattern();
        this.fDateFormat.applyPattern(str);
        fallbackFormat(calendar, calendar2, z, stringBuffer, fieldPosition);
        this.fDateFormat.applyPattern(pattern);
        return stringBuffer;
    }

    @Override
    @Deprecated
    public Object parseObject(String str, ParsePosition parsePosition) {
        throw new UnsupportedOperationException("parsing is not supported");
    }

    public DateIntervalInfo getDateIntervalInfo() {
        return (DateIntervalInfo) this.fInfo.clone();
    }

    public void setDateIntervalInfo(DateIntervalInfo dateIntervalInfo) {
        this.fInfo = (DateIntervalInfo) dateIntervalInfo.clone();
        this.isDateIntervalInfoDefault = false;
        this.fInfo.freeze();
        if (this.fDateFormat != null) {
            initializePattern(null);
        }
    }

    public TimeZone getTimeZone() {
        if (this.fDateFormat != null) {
            return (TimeZone) this.fDateFormat.getTimeZone().clone();
        }
        return TimeZone.getDefault();
    }

    public void setTimeZone(TimeZone timeZone) {
        TimeZone timeZone2 = (TimeZone) timeZone.clone();
        if (this.fDateFormat != null) {
            this.fDateFormat.setTimeZone(timeZone2);
        }
        if (this.fFromCalendar != null) {
            this.fFromCalendar.setTimeZone(timeZone2);
        }
        if (this.fToCalendar != null) {
            this.fToCalendar.setTimeZone(timeZone2);
        }
    }

    public synchronized DateFormat getDateFormat() {
        return (DateFormat) this.fDateFormat.clone();
    }

    private void initializePattern(ICUCache<String, Map<String, DateIntervalInfo.PatternInfo>> iCUCache) {
        String str;
        String str2;
        String pattern = this.fDateFormat.toPattern();
        ULocale locale = this.fDateFormat.getLocale();
        Map<String, DateIntervalInfo.PatternInfo> mapUnmodifiableMap = null;
        if (iCUCache == null) {
            str = null;
        } else {
            if (this.fSkeleton != null) {
                str2 = locale.toString() + "+" + pattern + "+" + this.fSkeleton;
            } else {
                str2 = locale.toString() + "+" + pattern;
            }
            str = str2;
            mapUnmodifiableMap = iCUCache.get(str2);
        }
        if (mapUnmodifiableMap == null) {
            mapUnmodifiableMap = Collections.unmodifiableMap(initializeIntervalPattern(pattern, locale));
            if (iCUCache != null) {
                iCUCache.put(str, mapUnmodifiableMap);
            }
        }
        this.fIntervalPatterns = mapUnmodifiableMap;
    }

    private Map<String, DateIntervalInfo.PatternInfo> initializeIntervalPattern(String str, ULocale uLocale) {
        DateTimePatternGenerator dateTimePatternGenerator = DateTimePatternGenerator.getInstance(uLocale);
        if (this.fSkeleton == null) {
            this.fSkeleton = dateTimePatternGenerator.getSkeleton(str);
        }
        String str2 = this.fSkeleton;
        HashMap map = new HashMap();
        StringBuilder sb = new StringBuilder(str2.length());
        StringBuilder sb2 = new StringBuilder(str2.length());
        StringBuilder sb3 = new StringBuilder(str2.length());
        StringBuilder sb4 = new StringBuilder(str2.length());
        getDateTimeSkeleton(str2, sb, sb2, sb3, sb4);
        String string = sb.toString();
        String string2 = sb3.toString();
        String string3 = sb2.toString();
        String string4 = sb4.toString();
        if (sb3.length() != 0 && sb.length() != 0) {
            this.fDateTimeFormat = getConcatenationPattern(uLocale);
        }
        if (!genSeparateDateTimePtn(string3, string4, map, dateTimePatternGenerator)) {
            if (sb3.length() != 0 && sb.length() == 0) {
                DateIntervalInfo.PatternInfo patternInfo = new DateIntervalInfo.PatternInfo(null, dateTimePatternGenerator.getBestPattern(DateFormat.YEAR_NUM_MONTH_DAY + string2), this.fInfo.getDefaultOrder());
                map.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[5], patternInfo);
                map.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[2], patternInfo);
                map.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[1], patternInfo);
            }
            return map;
        }
        if (sb3.length() != 0) {
            if (sb.length() == 0) {
                DateIntervalInfo.PatternInfo patternInfo2 = new DateIntervalInfo.PatternInfo(null, dateTimePatternGenerator.getBestPattern(DateFormat.YEAR_NUM_MONTH_DAY + string2), this.fInfo.getDefaultOrder());
                map.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[5], patternInfo2);
                map.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[2], patternInfo2);
                map.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[1], patternInfo2);
            } else {
                if (!fieldExistsInSkeleton(5, string)) {
                    str2 = DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[5] + str2;
                    genFallbackPattern(5, str2, map, dateTimePatternGenerator);
                }
                if (!fieldExistsInSkeleton(2, string)) {
                    str2 = DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[2] + str2;
                    genFallbackPattern(2, str2, map, dateTimePatternGenerator);
                }
                if (!fieldExistsInSkeleton(1, string)) {
                    genFallbackPattern(1, DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[1] + str2, map, dateTimePatternGenerator);
                }
                if (this.fDateTimeFormat == null) {
                    this.fDateTimeFormat = "{1} {0}";
                }
                String bestPattern = dateTimePatternGenerator.getBestPattern(string);
                concatSingleDate2TimeInterval(this.fDateTimeFormat, bestPattern, 9, map);
                concatSingleDate2TimeInterval(this.fDateTimeFormat, bestPattern, 10, map);
                concatSingleDate2TimeInterval(this.fDateTimeFormat, bestPattern, 12, map);
            }
        }
        return map;
    }

    private String getConcatenationPattern(ULocale uLocale) {
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) ((ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, uLocale)).getWithFallback("calendar/gregorian/DateTimePatterns").get(8);
        if (iCUResourceBundle.getType() == 0) {
            return iCUResourceBundle.getString();
        }
        return iCUResourceBundle.getString(0);
    }

    private void genFallbackPattern(int i, String str, Map<String, DateIntervalInfo.PatternInfo> map, DateTimePatternGenerator dateTimePatternGenerator) {
        map.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[i], new DateIntervalInfo.PatternInfo(null, dateTimePatternGenerator.getBestPattern(str), this.fInfo.getDefaultOrder()));
    }

    private static void getDateTimeSkeleton(String str, StringBuilder sb, StringBuilder sb2, StringBuilder sb3, StringBuilder sb4) {
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        int i6 = 0;
        int i7 = 0;
        int i8 = 0;
        int i9 = 0;
        for (int i10 = 0; i10 < str.length(); i10++) {
            char cCharAt = str.charAt(i10);
            switch (cCharAt) {
                case 'A':
                case 'K':
                case 'S':
                case 'V':
                case 'Z':
                case 'j':
                case 'k':
                case 's':
                    sb3.append(cCharAt);
                    sb4.append(cCharAt);
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
                    sb2.append(cCharAt);
                    sb.append(cCharAt);
                    break;
                case 'E':
                    sb.append(cCharAt);
                    i2++;
                    break;
                case 'H':
                    sb3.append(cCharAt);
                    i3++;
                    break;
                case 'M':
                    sb.append(cCharAt);
                    i4++;
                    break;
                case 'a':
                    sb3.append(cCharAt);
                    break;
                case 'd':
                    sb.append(cCharAt);
                    i5++;
                    break;
                case 'h':
                    sb3.append(cCharAt);
                    i6++;
                    break;
                case 'm':
                    sb3.append(cCharAt);
                    i7++;
                    break;
                case 'v':
                    i8++;
                    sb3.append(cCharAt);
                    break;
                case 'y':
                    sb.append(cCharAt);
                    i++;
                    break;
                case 'z':
                    i9++;
                    sb3.append(cCharAt);
                    break;
            }
        }
        if (i != 0) {
            for (int i11 = 0; i11 < i; i11++) {
                sb2.append('y');
            }
        }
        if (i4 != 0) {
            if (i4 < 3) {
                sb2.append('M');
            } else {
                for (int i12 = 0; i12 < i4 && i12 < 5; i12++) {
                    sb2.append('M');
                }
            }
        }
        if (i2 != 0) {
            if (i2 <= 3) {
                sb2.append('E');
            } else {
                for (int i13 = 0; i13 < i2 && i13 < 5; i13++) {
                    sb2.append('E');
                }
            }
        }
        if (i5 != 0) {
            sb2.append('d');
        }
        if (i3 != 0) {
            sb4.append('H');
        } else if (i6 != 0) {
            sb4.append('h');
        }
        if (i7 != 0) {
            sb4.append('m');
        }
        if (i9 != 0) {
            sb4.append('z');
        }
        if (i8 != 0) {
            sb4.append('v');
        }
    }

    private boolean genSeparateDateTimePtn(String str, String str2, Map<String, DateIntervalInfo.PatternInfo> map, DateTimePatternGenerator dateTimePatternGenerator) {
        String str3;
        String str4;
        String str5 = str2.length() != 0 ? str2 : str;
        BestMatchInfo bestSkeleton = this.fInfo.getBestSkeleton(str5);
        String str6 = bestSkeleton.bestMatchSkeleton;
        int i = bestSkeleton.bestMatchDistanceInfo;
        if (str.length() != 0) {
            this.fDatePattern = dateTimePatternGenerator.getBestPattern(str);
        }
        if (str2.length() != 0) {
            this.fTimePattern = dateTimePatternGenerator.getBestPattern(str2);
        }
        if (i == -1) {
            return false;
        }
        if (str2.length() == 0) {
            String str7 = str5;
            genIntervalPattern(5, str7, str6, i, map);
            SkeletonAndItsBestMatch skeletonAndItsBestMatchGenIntervalPattern = genIntervalPattern(2, str7, str6, i, map);
            if (skeletonAndItsBestMatchGenIntervalPattern != null) {
                String str8 = skeletonAndItsBestMatchGenIntervalPattern.skeleton;
                str3 = skeletonAndItsBestMatchGenIntervalPattern.bestMatchSkeleton;
                str4 = str8;
            } else {
                str3 = str5;
                str4 = str6;
            }
            genIntervalPattern(1, str3, str4, i, map);
            return true;
        }
        String str9 = str5;
        genIntervalPattern(12, str9, str6, i, map);
        genIntervalPattern(10, str9, str6, i, map);
        genIntervalPattern(9, str9, str6, i, map);
        return true;
    }

    private SkeletonAndItsBestMatch genIntervalPattern(int i, String str, String str2, int i2, Map<String, DateIntervalInfo.PatternInfo> map) {
        DateIntervalInfo.PatternInfo patternInfo;
        DateIntervalInfo.PatternInfo intervalPattern = this.fInfo.getIntervalPattern(str2, i);
        SkeletonAndItsBestMatch skeletonAndItsBestMatch = null;
        if (intervalPattern == null) {
            if (SimpleDateFormat.isFieldUnitIgnored(str2, i)) {
                map.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[i], new DateIntervalInfo.PatternInfo(this.fDateFormat.toPattern(), null, this.fInfo.getDefaultOrder()));
                return null;
            }
            if (i == 9) {
                DateIntervalInfo.PatternInfo intervalPattern2 = this.fInfo.getIntervalPattern(str2, 10);
                if (intervalPattern2 != null) {
                    map.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[i], intervalPattern2);
                }
                return null;
            }
            String str3 = DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[i];
            str2 = str3 + str2;
            str = str3 + str;
            intervalPattern = this.fInfo.getIntervalPattern(str2, i);
            if (intervalPattern == null && i2 == 0) {
                BestMatchInfo bestSkeleton = this.fInfo.getBestSkeleton(str);
                String str4 = bestSkeleton.bestMatchSkeleton;
                i2 = bestSkeleton.bestMatchDistanceInfo;
                if (str4.length() != 0 && i2 != -1) {
                    intervalPattern = this.fInfo.getIntervalPattern(str4, i);
                    str2 = str4;
                }
            }
            if (intervalPattern != null) {
                skeletonAndItsBestMatch = new SkeletonAndItsBestMatch(str, str2);
            }
        }
        if (intervalPattern != null) {
            if (i2 != 0) {
                patternInfo = new DateIntervalInfo.PatternInfo(adjustFieldWidth(str, str2, intervalPattern.getFirstPart(), i2), adjustFieldWidth(str, str2, intervalPattern.getSecondPart(), i2), intervalPattern.firstDateInPtnIsLaterDate());
            } else {
                patternInfo = intervalPattern;
            }
            map.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[i], patternInfo);
        }
        return skeletonAndItsBestMatch;
    }

    private static String adjustFieldWidth(String str, String str2, String str3, int i) {
        if (str3 == null) {
            return null;
        }
        int[] iArr = new int[58];
        int[] iArr2 = new int[58];
        DateIntervalInfo.parseSkeleton(str, iArr);
        DateIntervalInfo.parseSkeleton(str2, iArr2);
        if (i == 2) {
            str3 = str3.replace('v', 'z');
        }
        StringBuilder sb = new StringBuilder(str3);
        int length = sb.length();
        int i2 = 0;
        char c = 0;
        int i3 = 0;
        boolean z = false;
        while (true) {
            if (i2 >= length) {
                break;
            }
            char cCharAt = sb.charAt(i2);
            if (cCharAt != c && i3 > 0) {
                int i4 = (c != 'L' ? c : 'M') - 'A';
                int i5 = iArr2[i4];
                int i6 = iArr[i4];
                if (i5 == i3 && i6 > i5) {
                    int i7 = i6 - i5;
                    for (int i8 = 0; i8 < i7; i8++) {
                        sb.insert(i2, c);
                    }
                    i2 += i7;
                    length += i7;
                }
                i3 = 0;
            }
            if (cCharAt == '\'') {
                int i9 = i2 + 1;
                if (i9 >= sb.length() || sb.charAt(i9) != '\'') {
                    z = !z;
                } else {
                    i2 = i9;
                }
            } else if (!z && ((cCharAt >= 'a' && cCharAt <= 'z') || (cCharAt >= 'A' && cCharAt <= 'Z'))) {
                i3++;
                c = cCharAt;
            }
            i2++;
        }
        if (i3 > 0) {
            int i10 = (c != 'L' ? c : 'M') - 'A';
            int i11 = iArr2[i10];
            int i12 = iArr[i10];
            if (i11 == i3 && i12 > i11) {
                int i13 = i12 - i11;
                for (int i14 = 0; i14 < i13; i14++) {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    private void concatSingleDate2TimeInterval(String str, String str2, int i, Map<String, DateIntervalInfo.PatternInfo> map) {
        DateIntervalInfo.PatternInfo patternInfo = map.get(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[i]);
        if (patternInfo != null) {
            map.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[i], DateIntervalInfo.genPatternInfo(SimpleFormatterImpl.formatRawPattern(str, 2, 2, patternInfo.getFirstPart() + patternInfo.getSecondPart(), str2), patternInfo.firstDateInPtnIsLaterDate()));
        }
    }

    private static boolean fieldExistsInSkeleton(int i, String str) {
        return str.indexOf(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[i]) != -1;
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        initializePattern(this.isDateIntervalInfoDefault ? LOCAL_PATTERN_CACHE : null);
    }

    @Deprecated
    public Map<String, DateIntervalInfo.PatternInfo> getRawPatterns() {
        return this.fIntervalPatterns;
    }
}
