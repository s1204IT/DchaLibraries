package android.icu.text;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.UResource;
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
import java.util.Iterator;
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
    public TimeUnitFormat(ULocale uLocale) {
        this(uLocale, 0);
    }

    @Deprecated
    public TimeUnitFormat(Locale locale) {
        this(locale, 0);
    }

    @Deprecated
    public TimeUnitFormat(ULocale uLocale, int i) {
        if (i < 0 || i >= 2) {
            throw new IllegalArgumentException("style should be either FULL_NAME or ABBREVIATED_NAME style");
        }
        this.mf = MeasureFormat.getInstance(uLocale, i == 0 ? MeasureFormat.FormatWidth.WIDE : MeasureFormat.FormatWidth.SHORT);
        this.style = i;
        setLocale(uLocale, uLocale);
        this.locale = uLocale;
        this.isReady = false;
    }

    private TimeUnitFormat(ULocale uLocale, int i, NumberFormat numberFormat) {
        this(uLocale, i);
        if (numberFormat != null) {
            setNumberFormat((NumberFormat) numberFormat.clone());
        }
    }

    @Deprecated
    public TimeUnitFormat(Locale locale, int i) {
        this(ULocale.forLocale(locale), i);
    }

    @Deprecated
    public TimeUnitFormat setLocale(ULocale uLocale) {
        if (uLocale != this.locale) {
            this.mf = this.mf.withLocale(uLocale);
            setLocale(uLocale, uLocale);
            this.locale = uLocale;
            this.isReady = false;
        }
        return this;
    }

    @Deprecated
    public TimeUnitFormat setLocale(Locale locale) {
        return setLocale(ULocale.forLocale(locale));
    }

    @Deprecated
    public TimeUnitFormat setNumberFormat(NumberFormat numberFormat) {
        if (numberFormat == this.format) {
            return this;
        }
        if (numberFormat == null) {
            if (this.locale == null) {
                this.isReady = false;
                this.mf = this.mf.withLocale(ULocale.getDefault());
            } else {
                this.format = NumberFormat.getNumberInstance(this.locale);
                this.mf = this.mf.withNumberFormat(this.format);
            }
        } else {
            this.format = numberFormat;
            this.mf = this.mf.withNumberFormat(this.format);
        }
        return this;
    }

    @Override
    @Deprecated
    public StringBuffer format(Object obj, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        return this.mf.format(obj, stringBuffer, fieldPosition);
    }

    @Override
    @Deprecated
    public TimeUnitAmount parseObject(String str, ParsePosition parsePosition) {
        Number number;
        if (!this.isReady) {
            setup();
        }
        int index = parsePosition.getIndex();
        Iterator<TimeUnit> it = this.timeUnitToCountToPatterns.keySet().iterator();
        int i = -1;
        int i2 = 0;
        Number number2 = null;
        String str2 = null;
        TimeUnit timeUnit = null;
        while (true) {
            int i3 = 2;
            if (!it.hasNext()) {
                break;
            }
            TimeUnit next = it.next();
            for (Map.Entry<String, Object[]> entry : this.timeUnitToCountToPatterns.get(next).entrySet()) {
                String key = entry.getKey();
                TimeUnit timeUnit2 = timeUnit;
                int index2 = i;
                int i4 = i2;
                String str3 = str2;
                Number number3 = number2;
                int i5 = 0;
                while (i5 < i3) {
                    MessageFormat messageFormat = (MessageFormat) entry.getValue()[i5];
                    parsePosition.setErrorIndex(-1);
                    parsePosition.setIndex(index);
                    Object object = messageFormat.parseObject(str, parsePosition);
                    if (parsePosition.getErrorIndex() == -1 && parsePosition.getIndex() != index) {
                        Object[] objArr = (Object[]) object;
                        if (objArr.length != 0) {
                            Object obj = objArr[0];
                            if (obj instanceof Number) {
                                number = (Number) obj;
                            } else {
                                try {
                                    number = this.format.parse(obj.toString());
                                } catch (ParseException e) {
                                }
                            }
                        } else {
                            number = null;
                        }
                        int index3 = parsePosition.getIndex() - index;
                        if (index3 > i4) {
                            index2 = parsePosition.getIndex();
                            timeUnit2 = next;
                            i4 = index3;
                            str3 = key;
                            number3 = number;
                        }
                    }
                    i5++;
                    i3 = 2;
                }
                number2 = number3;
                str2 = str3;
                i2 = i4;
                timeUnit = timeUnit2;
                i = index2;
                i3 = 2;
            }
        }
        if (number2 == null && i2 != 0) {
            if (str2.equals(PluralRules.KEYWORD_ZERO)) {
                number2 = 0;
            } else if (str2.equals(PluralRules.KEYWORD_ONE)) {
                number2 = 1;
            } else if (str2.equals(PluralRules.KEYWORD_TWO)) {
                number2 = 2;
            } else {
                number2 = 3;
            }
        }
        if (i2 == 0) {
            parsePosition.setIndex(index);
            parsePosition.setErrorIndex(0);
            return null;
        }
        parsePosition.setIndex(i);
        parsePosition.setErrorIndex(-1);
        return new TimeUnitAmount(number2, timeUnit);
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
        Set<String> keywords = this.pluralRules.getKeywords();
        setup("units/duration", this.timeUnitToCountToPatterns, 0, keywords);
        setup("unitsShort/duration", this.timeUnitToCountToPatterns, 1, keywords);
        this.isReady = true;
    }

    private static final class TimeUnitFormatSetupSink extends UResource.Sink {
        boolean beenHere = false;
        ULocale locale;
        Set<String> pluralKeywords;
        int style;
        Map<TimeUnit, Map<String, Object[]>> timeUnitToCountToPatterns;

        TimeUnitFormatSetupSink(Map<TimeUnit, Map<String, Object[]>> map, int i, Set<String> set, ULocale uLocale) {
            this.timeUnitToCountToPatterns = map;
            this.style = i;
            this.pluralKeywords = set;
            this.locale = uLocale;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean z) {
            TimeUnit timeUnit;
            if (this.beenHere) {
                return;
            }
            this.beenHere = true;
            UResource.Table table = value.getTable();
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                String string = key.toString();
                if (string.equals("year")) {
                    timeUnit = TimeUnit.YEAR;
                } else if (string.equals("month")) {
                    timeUnit = TimeUnit.MONTH;
                } else if (string.equals("day")) {
                    timeUnit = TimeUnit.DAY;
                } else if (string.equals("hour")) {
                    timeUnit = TimeUnit.HOUR;
                } else if (string.equals("minute")) {
                    timeUnit = TimeUnit.MINUTE;
                } else if (string.equals("second")) {
                    timeUnit = TimeUnit.SECOND;
                } else if (string.equals("week")) {
                    timeUnit = TimeUnit.WEEK;
                }
                Map<String, Object[]> treeMap = this.timeUnitToCountToPatterns.get(timeUnit);
                if (treeMap == null) {
                    treeMap = new TreeMap<>();
                    this.timeUnitToCountToPatterns.put(timeUnit, treeMap);
                }
                UResource.Table table2 = value.getTable();
                for (int i2 = 0; table2.getKeyAndValue(i2, key, value); i2++) {
                    String string2 = key.toString();
                    if (this.pluralKeywords.contains(string2)) {
                        Object[] objArr = treeMap.get(string2);
                        if (objArr == null) {
                            objArr = new Object[2];
                            treeMap.put(string2, objArr);
                        }
                        if (objArr[this.style] == null) {
                            objArr[this.style] = new MessageFormat(value.getString(), this.locale);
                        }
                    }
                }
            }
        }
    }

    private void setup(String str, Map<TimeUnit, Map<String, Object[]>> map, int i, Set<String> set) {
        String str2;
        Iterator<String> it;
        try {
            str2 = str;
            try {
                ((ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME, this.locale)).getAllItemsWithFallback(str2, new TimeUnitFormatSetupSink(map, i, set, this.locale));
            } catch (MissingResourceException e) {
            }
        } catch (MissingResourceException e2) {
            str2 = str;
        }
        TimeUnit[] timeUnitArrValues = TimeUnit.values();
        Set<String> keywords = this.pluralRules.getKeywords();
        for (TimeUnit timeUnit : timeUnitArrValues) {
            Map<String, Object[]> treeMap = map.get(timeUnit);
            if (treeMap == null) {
                treeMap = new TreeMap<>();
                map.put(timeUnit, treeMap);
            }
            Map<String, Object[]> map2 = treeMap;
            Iterator<String> it2 = keywords.iterator();
            while (it2.hasNext()) {
                String next = it2.next();
                if (map2.get(next) == null || map2.get(next)[i] == null) {
                    it = it2;
                    searchInTree(str2, i, timeUnit, next, next, map2);
                } else {
                    it = it2;
                }
                it2 = it;
            }
        }
    }

    private void searchInTree(String str, int i, TimeUnit timeUnit, String str2, String str3, Map<String, Object[]> map) {
        ULocale uLocale = this.locale;
        String string = timeUnit.toString();
        ULocale fallback = uLocale;
        while (fallback != null) {
            try {
                MessageFormat messageFormat = new MessageFormat(((ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME, fallback)).getWithFallback(str).getWithFallback(string).getStringWithFallback(str3), this.locale);
                Object[] objArr = map.get(str2);
                if (objArr == null) {
                    objArr = new Object[2];
                    map.put(str2, objArr);
                }
                objArr[i] = messageFormat;
                return;
            } catch (MissingResourceException e) {
                fallback = fallback.getFallback();
            }
        }
        if (fallback == null && str.equals("unitsShort")) {
            searchInTree("units", i, timeUnit, str2, str3, map);
            if (map.get(str2) != null && map.get(str2)[i] != null) {
                return;
            }
        }
        if (!str3.equals(PluralRules.KEYWORD_OTHER)) {
            searchInTree(str, i, timeUnit, str2, PluralRules.KEYWORD_OTHER, map);
            return;
        }
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
        Object[] objArr2 = map.get(str2);
        if (objArr2 == null) {
            objArr2 = new Object[2];
            map.put(str2, objArr2);
        }
        objArr2[i] = messageFormat2;
    }

    @Override
    @Deprecated
    public StringBuilder formatMeasures(StringBuilder sb, FieldPosition fieldPosition, Measure... measureArr) {
        return this.mf.formatMeasures(sb, fieldPosition, measureArr);
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
        TimeUnitFormat timeUnitFormat = (TimeUnitFormat) super.clone();
        timeUnitFormat.format = (NumberFormat) this.format.clone();
        return timeUnitFormat;
    }

    private Object writeReplace() throws ObjectStreamException {
        return this.mf.toTimeUnitProxy();
    }

    private Object readResolve() throws ObjectStreamException {
        return new TimeUnitFormat(this.locale, this.style, this.format);
    }
}
