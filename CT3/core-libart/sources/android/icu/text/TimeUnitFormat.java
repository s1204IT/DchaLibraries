package android.icu.text;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.text.MeasureFormat;
import android.icu.util.Measure;
import android.icu.util.TimeUnit;
import android.icu.util.TimeUnitAmount;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.io.ObjectStreamException;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeMap;

@Deprecated
public class TimeUnitFormat extends MeasureFormat {

    @Deprecated
    public static final int ABBREVIATED_NAME = 1;
    private static final String DEFAULT_PATTERN_FOR_DAY = "{0} d";
    private static final String DEFAULT_PATTERN_FOR_HOUR = "{0} h";
    private static final String DEFAULT_PATTERN_FOR_MINUTE = "{0} min";
    private static final String DEFAULT_PATTERN_FOR_MONTH = "{0} m";
    private static final String DEFAULT_PATTERN_FOR_SECOND = "{0} s";
    private static final String DEFAULT_PATTERN_FOR_WEEK = "{0} w";
    private static final String DEFAULT_PATTERN_FOR_YEAR = "{0} y";

    @Deprecated
    public static final int FULL_NAME = 0;
    private static final int TOTAL_STYLES = 2;
    private static final long serialVersionUID = -3707773153184971529L;
    private NumberFormat format;
    private transient boolean isReady;
    private ULocale locale;
    private transient MeasureFormat mf;
    private transient PluralRules pluralRules;
    private int style;
    private transient Map<TimeUnit, Map<String, Object[]>> timeUnitToCountToPatterns;

    @Deprecated
    public TimeUnitFormat() {
        this.mf = MeasureFormat.getInstance(ULocale.getDefault(), MeasureFormat.FormatWidth.WIDE);
        this.isReady = false;
        this.style = 0;
    }

    @Deprecated
    public TimeUnitFormat(ULocale locale) {
        this(locale, 0);
    }

    @Deprecated
    public TimeUnitFormat(Locale locale) {
        this(locale, 0);
    }

    @Deprecated
    public TimeUnitFormat(ULocale locale, int style) {
        if (style < 0 || style >= 2) {
            throw new IllegalArgumentException("style should be either FULL_NAME or ABBREVIATED_NAME style");
        }
        this.mf = MeasureFormat.getInstance(locale, style == 0 ? MeasureFormat.FormatWidth.WIDE : MeasureFormat.FormatWidth.SHORT);
        this.style = style;
        setLocale(locale, locale);
        this.locale = locale;
        this.isReady = false;
    }

    private TimeUnitFormat(ULocale locale, int style, NumberFormat numberFormat) {
        this(locale, style);
        if (numberFormat == null) {
            return;
        }
        setNumberFormat((NumberFormat) numberFormat.clone());
    }

    @Deprecated
    public TimeUnitFormat(Locale locale, int style) {
        this(ULocale.forLocale(locale), style);
    }

    @Deprecated
    public TimeUnitFormat setLocale(ULocale locale) {
        if (locale != this.locale) {
            this.mf = this.mf.withLocale(locale);
            setLocale(locale, locale);
            this.locale = locale;
            this.isReady = false;
        }
        return this;
    }

    @Deprecated
    public TimeUnitFormat setLocale(Locale locale) {
        return setLocale(ULocale.forLocale(locale));
    }

    @Deprecated
    public TimeUnitFormat setNumberFormat(NumberFormat format) {
        if (format == this.format) {
            return this;
        }
        if (format == null) {
            if (this.locale == null) {
                this.isReady = false;
                this.mf = this.mf.withLocale(ULocale.getDefault());
            } else {
                this.format = NumberFormat.getNumberInstance(this.locale);
                this.mf = this.mf.withNumberFormat(this.format);
            }
        } else {
            this.format = format;
            this.mf = this.mf.withNumberFormat(this.format);
        }
        return this;
    }

    @Override
    @Deprecated
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        return this.mf.format(obj, toAppendTo, pos);
    }

    @Override
    @Deprecated
    public TimeUnitAmount parseObject(String source, ParsePosition pos) {
        int parseDistance;
        if (!this.isReady) {
            setup();
        }
        Number resultNumber = null;
        TimeUnit resultTimeUnit = null;
        int oldPos = pos.getIndex();
        int newPos = -1;
        int longestParseDistance = 0;
        String countOfLongestMatch = null;
        for (TimeUnit timeUnit : this.timeUnitToCountToPatterns.keySet()) {
            Map<String, Object[]> countToPattern = this.timeUnitToCountToPatterns.get(timeUnit);
            for (Map.Entry<String, Object[]> patternEntry : countToPattern.entrySet()) {
                String count = patternEntry.getKey();
                for (int styl = 0; styl < 2; styl++) {
                    MessageFormat pattern = (MessageFormat) patternEntry.getValue()[styl];
                    pos.setErrorIndex(-1);
                    pos.setIndex(oldPos);
                    Object parsed = pattern.parseObject(source, pos);
                    if (pos.getErrorIndex() == -1 && pos.getIndex() != oldPos) {
                        Number temp = null;
                        if (((Object[]) parsed).length != 0) {
                            Object tempObj = ((Object[]) parsed)[0];
                            if (tempObj instanceof Number) {
                                temp = (Number) tempObj;
                            } else {
                                try {
                                    temp = this.format.parse(tempObj.toString());
                                } catch (ParseException e) {
                                }
                            }
                            parseDistance = pos.getIndex() - oldPos;
                            if (parseDistance <= longestParseDistance) {
                                resultNumber = temp;
                                resultTimeUnit = timeUnit;
                                newPos = pos.getIndex();
                                longestParseDistance = parseDistance;
                                countOfLongestMatch = count;
                            }
                        } else {
                            parseDistance = pos.getIndex() - oldPos;
                            if (parseDistance <= longestParseDistance) {
                            }
                        }
                    }
                }
            }
        }
        if (resultNumber == null && longestParseDistance != 0) {
            if (countOfLongestMatch.equals(PluralRules.KEYWORD_ZERO)) {
                resultNumber = 0;
            } else if (countOfLongestMatch.equals(PluralRules.KEYWORD_ONE)) {
                resultNumber = 1;
            } else if (countOfLongestMatch.equals(PluralRules.KEYWORD_TWO)) {
                resultNumber = 2;
            } else {
                resultNumber = 3;
            }
        }
        if (longestParseDistance == 0) {
            pos.setIndex(oldPos);
            pos.setErrorIndex(0);
            return null;
        }
        pos.setIndex(newPos);
        pos.setErrorIndex(-1);
        return new TimeUnitAmount(resultNumber, resultTimeUnit);
    }

    private void setup() {
        if (this.locale == null) {
            if (this.format != null) {
                this.locale = this.format.getLocale(null);
            } else {
                this.locale = ULocale.getDefault(ULocale.Category.FORMAT);
            }
            setLocale(this.locale, this.locale);
        }
        if (this.format == null) {
            this.format = NumberFormat.getNumberInstance(this.locale);
        }
        this.pluralRules = PluralRules.forLocale(this.locale);
        this.timeUnitToCountToPatterns = new HashMap();
        Set<String> pluralKeywords = this.pluralRules.getKeywords();
        setup("units/duration", this.timeUnitToCountToPatterns, 0, pluralKeywords);
        setup("unitsShort/duration", this.timeUnitToCountToPatterns, 1, pluralKeywords);
        this.isReady = true;
    }

    private void setup(String resourceKey, Map<TimeUnit, Map<String, Object[]>> timeUnitToCountToPatterns, int style, Set<String> pluralKeywords) {
        TimeUnit timeUnit;
        try {
            ICUResourceBundle resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME, this.locale);
            ICUResourceBundle unitsRes = resource.getWithFallback(resourceKey);
            int size = unitsRes.getSize();
            for (int index = 0; index < size; index++) {
                String timeUnitName = unitsRes.get(index).getKey();
                if (timeUnitName.equals("year")) {
                    timeUnit = TimeUnit.YEAR;
                } else if (timeUnitName.equals("month")) {
                    timeUnit = TimeUnit.MONTH;
                } else if (timeUnitName.equals("day")) {
                    timeUnit = TimeUnit.DAY;
                } else if (timeUnitName.equals("hour")) {
                    timeUnit = TimeUnit.HOUR;
                } else if (timeUnitName.equals("minute")) {
                    timeUnit = TimeUnit.MINUTE;
                } else if (timeUnitName.equals("second")) {
                    timeUnit = TimeUnit.SECOND;
                } else if (timeUnitName.equals("week")) {
                    timeUnit = TimeUnit.WEEK;
                }
                ICUResourceBundle oneUnitRes = unitsRes.getWithFallback(timeUnitName);
                int count = oneUnitRes.getSize();
                Map<String, Object[]> countToPatterns = timeUnitToCountToPatterns.get(timeUnit);
                if (countToPatterns == null) {
                    countToPatterns = new TreeMap<>();
                    timeUnitToCountToPatterns.put(timeUnit, countToPatterns);
                }
                for (int pluralIndex = 0; pluralIndex < count; pluralIndex++) {
                    String pluralCount = oneUnitRes.get(pluralIndex).getKey();
                    if (pluralKeywords.contains(pluralCount)) {
                        String pattern = oneUnitRes.get(pluralIndex).getString();
                        MessageFormat messageFormat = new MessageFormat(pattern, this.locale);
                        Object[] pair = countToPatterns.get(pluralCount);
                        if (pair == null) {
                            pair = new Object[2];
                            countToPatterns.put(pluralCount, pair);
                        }
                        pair[style] = messageFormat;
                    }
                }
            }
        } catch (MissingResourceException e) {
        }
        TimeUnit[] timeUnits = TimeUnit.values();
        Set<String> keywords = this.pluralRules.getKeywords();
        for (TimeUnit timeUnit2 : timeUnits) {
            Map<String, Object[]> countToPatterns2 = timeUnitToCountToPatterns.get(timeUnit2);
            if (countToPatterns2 == null) {
                countToPatterns2 = new TreeMap<>();
                timeUnitToCountToPatterns.put(timeUnit2, countToPatterns2);
            }
            for (String pluralCount2 : keywords) {
                if (countToPatterns2.get(pluralCount2) == null || countToPatterns2.get(pluralCount2)[style] == null) {
                    searchInTree(resourceKey, style, timeUnit2, pluralCount2, pluralCount2, countToPatterns2);
                }
            }
        }
    }

    private void searchInTree(String resourceKey, int styl, TimeUnit timeUnit, String srcPluralCount, String searchPluralCount, Map<String, Object[]> countToPatterns) {
        ULocale parentLocale = this.locale;
        String srcTimeUnitName = timeUnit.toString();
        while (parentLocale != null) {
            try {
                ICUResourceBundle unitsRes = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME, parentLocale);
                ICUResourceBundle oneUnitRes = unitsRes.getWithFallback(resourceKey).getWithFallback(srcTimeUnitName);
                String pattern = oneUnitRes.getStringWithFallback(searchPluralCount);
                MessageFormat messageFormat = new MessageFormat(pattern, this.locale);
                Object[] pair = countToPatterns.get(srcPluralCount);
                if (pair == null) {
                    pair = new Object[2];
                    countToPatterns.put(srcPluralCount, pair);
                }
                pair[styl] = messageFormat;
                return;
            } catch (MissingResourceException e) {
                parentLocale = parentLocale.getFallback();
            }
        }
        if (parentLocale == null && resourceKey.equals("unitsShort")) {
            searchInTree("units", styl, timeUnit, srcPluralCount, searchPluralCount, countToPatterns);
            if (countToPatterns != null && countToPatterns.get(srcPluralCount) != null && countToPatterns.get(srcPluralCount)[styl] != null) {
                return;
            }
        }
        if (searchPluralCount.equals(PluralRules.KEYWORD_OTHER)) {
            MessageFormat messageFormat2 = null;
            if (timeUnit == TimeUnit.SECOND) {
                messageFormat2 = new MessageFormat(DEFAULT_PATTERN_FOR_SECOND, this.locale);
            } else if (timeUnit == TimeUnit.MINUTE) {
                messageFormat2 = new MessageFormat(DEFAULT_PATTERN_FOR_MINUTE, this.locale);
            } else if (timeUnit == TimeUnit.HOUR) {
                messageFormat2 = new MessageFormat(DEFAULT_PATTERN_FOR_HOUR, this.locale);
            } else if (timeUnit == TimeUnit.WEEK) {
                messageFormat2 = new MessageFormat(DEFAULT_PATTERN_FOR_WEEK, this.locale);
            } else if (timeUnit == TimeUnit.DAY) {
                messageFormat2 = new MessageFormat(DEFAULT_PATTERN_FOR_DAY, this.locale);
            } else if (timeUnit == TimeUnit.MONTH) {
                messageFormat2 = new MessageFormat(DEFAULT_PATTERN_FOR_MONTH, this.locale);
            } else if (timeUnit == TimeUnit.YEAR) {
                messageFormat2 = new MessageFormat(DEFAULT_PATTERN_FOR_YEAR, this.locale);
            }
            Object[] pair2 = countToPatterns.get(srcPluralCount);
            if (pair2 == null) {
                pair2 = new Object[2];
                countToPatterns.put(srcPluralCount, pair2);
            }
            pair2[styl] = messageFormat2;
            return;
        }
        searchInTree(resourceKey, styl, timeUnit, srcPluralCount, PluralRules.KEYWORD_OTHER, countToPatterns);
    }

    @Override
    @Deprecated
    public StringBuilder formatMeasures(StringBuilder appendTo, FieldPosition fieldPosition, Measure... measures) {
        return this.mf.formatMeasures(appendTo, fieldPosition, measures);
    }

    @Override
    @Deprecated
    public MeasureFormat.FormatWidth getWidth() {
        return this.mf.getWidth();
    }

    @Override
    @Deprecated
    public NumberFormat getNumberFormat() {
        return this.mf.getNumberFormat();
    }

    @Override
    @Deprecated
    public Object clone() {
        TimeUnitFormat result = (TimeUnitFormat) super.clone();
        result.format = (NumberFormat) this.format.clone();
        return result;
    }

    private Object writeReplace() throws ObjectStreamException {
        return this.mf.toTimeUnitProxy();
    }

    private Object readResolve() throws ObjectStreamException {
        return new TimeUnitFormat(this.locale, this.style, this.format);
    }
}
