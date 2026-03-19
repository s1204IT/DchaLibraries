package android.icu.text;

import android.icu.impl.DontCareFieldPosition;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SimpleCache;
import android.icu.impl.SimplePatternFormatter;
import android.icu.impl.StandardPlural;
import android.icu.impl.UResource;
import android.icu.lang.UCharacterEnums;
import android.icu.text.DateFormat;
import android.icu.text.ListFormatter;
import android.icu.text.PluralRules;
import android.icu.util.Currency;
import android.icu.util.CurrencyAmount;
import android.icu.util.ICUException;
import android.icu.util.Measure;
import android.icu.util.MeasureUnit;
import android.icu.util.TimeZone;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.ConcurrentHashMap;

public class MeasureFormat extends UFormat {
    private static final int CURRENCY_FORMAT = 2;
    private static final int MEASURE_FORMAT = 0;
    private static final int TIME_UNIT_FORMAT = 1;
    private static final Map<ULocale, String> localeIdToRangeFormat;
    static final long serialVersionUID = -7182021401701778240L;
    private final transient MeasureFormatData cache;
    private final transient ImmutableNumberFormat currencyFormat;
    private final transient FormatWidth formatWidth;
    private final transient ImmutableNumberFormat integerFormat;
    private final transient ImmutableNumberFormat numberFormat;
    private final transient NumericFormatters numericFormatters;
    private final transient PluralRules rules;
    private static final SimpleCache<ULocale, MeasureFormatData> localeMeasureFormatData = new SimpleCache<>();
    private static final SimpleCache<ULocale, NumericFormatters> localeToNumericDurationFormatters = new SimpleCache<>();
    private static final Map<MeasureUnit, Integer> hmsTo012 = new HashMap();

    static {
        hmsTo012.put(MeasureUnit.HOUR, 0);
        hmsTo012.put(MeasureUnit.MINUTE, 1);
        hmsTo012.put(MeasureUnit.SECOND, 2);
        localeIdToRangeFormat = new ConcurrentHashMap();
    }

    public enum FormatWidth {
        WIDE(ListFormatter.Style.DURATION, 6),
        SHORT(ListFormatter.Style.DURATION_SHORT, 5),
        NARROW(ListFormatter.Style.DURATION_NARROW, 1),
        NUMERIC(ListFormatter.Style.DURATION_NARROW, 1);

        private static final int INDEX_COUNT = 3;
        private final int currencyStyle;
        private final ListFormatter.Style listFormatterStyle;

        public static FormatWidth[] valuesCustom() {
            return values();
        }

        FormatWidth(ListFormatter.Style style, int currencyStyle) {
            this.listFormatterStyle = style;
            this.currencyStyle = currencyStyle;
        }

        ListFormatter.Style getListFormatterStyle() {
            return this.listFormatterStyle;
        }

        int getCurrencyStyle() {
            return this.currencyStyle;
        }
    }

    public static MeasureFormat getInstance(ULocale locale, FormatWidth formatWidth) {
        return getInstance(locale, formatWidth, NumberFormat.getInstance(locale));
    }

    public static MeasureFormat getInstance(Locale locale, FormatWidth formatWidth) {
        return getInstance(ULocale.forLocale(locale), formatWidth);
    }

    public static MeasureFormat getInstance(ULocale locale, FormatWidth formatWidth, NumberFormat format) {
        PluralRules rules = PluralRules.forLocale(locale);
        NumericFormatters formatters = null;
        MeasureFormatData data = localeMeasureFormatData.get(locale);
        if (data == null) {
            data = loadLocaleData(locale);
            localeMeasureFormatData.put(locale, data);
        }
        if (formatWidth == FormatWidth.NUMERIC) {
            NumericFormatters formatters2 = localeToNumericDurationFormatters.get(locale);
            formatters = formatters2;
            if (formatters == null) {
                formatters = loadNumericFormatters(locale);
                localeToNumericDurationFormatters.put(locale, formatters);
            }
        }
        NumberFormat intFormat = NumberFormat.getInstance(locale);
        intFormat.setMaximumFractionDigits(0);
        intFormat.setMinimumFractionDigits(0);
        intFormat.setRoundingMode(1);
        return new MeasureFormat(locale, data, formatWidth, new ImmutableNumberFormat(format), rules, formatters, new ImmutableNumberFormat(NumberFormat.getInstance(locale, formatWidth.getCurrencyStyle())), new ImmutableNumberFormat(intFormat));
    }

    public static MeasureFormat getInstance(Locale locale, FormatWidth formatWidth, NumberFormat format) {
        return getInstance(ULocale.forLocale(locale), formatWidth, format);
    }

    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        int prevLength = toAppendTo.length();
        FieldPosition fpos = new FieldPosition(pos.getFieldAttribute(), pos.getField());
        if (obj instanceof Collection) {
            Collection<?> coll = (Collection) obj;
            Measure[] measureArr = new Measure[coll.size()];
            int idx = 0;
            for (Object o : coll) {
                if (!(o instanceof Measure)) {
                    throw new IllegalArgumentException(obj.toString());
                }
                measureArr[idx] = o;
                idx++;
            }
            toAppendTo.append((CharSequence) formatMeasures(new StringBuilder(), fpos, measureArr));
        } else if (obj instanceof Measure[]) {
            toAppendTo.append((CharSequence) formatMeasures(new StringBuilder(), fpos, obj));
        } else {
            if (!(obj instanceof Measure)) {
                throw new IllegalArgumentException(obj.toString());
            }
            toAppendTo.append((CharSequence) formatMeasure(obj, this.numberFormat, new StringBuilder(), fpos));
        }
        if (fpos.getBeginIndex() != 0 || fpos.getEndIndex() != 0) {
            pos.setBeginIndex(fpos.getBeginIndex() + prevLength);
            pos.setEndIndex(fpos.getEndIndex() + prevLength);
        }
        return toAppendTo;
    }

    @Override
    public Measure parseObject(String source, ParsePosition pos) {
        throw new UnsupportedOperationException();
    }

    public final String formatMeasures(Measure... measures) {
        return formatMeasures(new StringBuilder(), DontCareFieldPosition.INSTANCE, measures).toString();
    }

    @Deprecated
    public final String formatMeasureRange(Measure lowValue, Measure highValue) {
        MeasureUnit unit = lowValue.getUnit();
        if (!unit.equals(highValue.getUnit())) {
            throw new IllegalArgumentException("Units must match: " + unit + " ≠ " + highValue.getUnit());
        }
        Number lowNumber = lowValue.getNumber();
        Number highNumber = highValue.getNumber();
        boolean isCurrency = unit instanceof Currency;
        UFieldPosition lowFpos = new UFieldPosition();
        UFieldPosition highFpos = new UFieldPosition();
        StringBuffer lowFormatted = null;
        StringBuffer highFormatted = null;
        if (isCurrency) {
            Currency currency = (Currency) unit;
            int fracDigits = currency.getDefaultFractionDigits();
            int maxFrac = this.numberFormat.nf.getMaximumFractionDigits();
            int minFrac = this.numberFormat.nf.getMinimumFractionDigits();
            if (fracDigits != maxFrac || fracDigits != minFrac) {
                DecimalFormat currentNumberFormat = (DecimalFormat) this.numberFormat.get();
                currentNumberFormat.setMaximumFractionDigits(fracDigits);
                currentNumberFormat.setMinimumFractionDigits(fracDigits);
                lowFormatted = currentNumberFormat.format(lowNumber, new StringBuffer(), lowFpos);
                highFormatted = currentNumberFormat.format(highNumber, new StringBuffer(), highFpos);
            }
        }
        if (lowFormatted == null) {
            lowFormatted = this.numberFormat.format(lowNumber, new StringBuffer(), lowFpos);
            highFormatted = this.numberFormat.format(highNumber, new StringBuffer(), highFpos);
        }
        double lowDouble = lowNumber.doubleValue();
        String keywordLow = this.rules.select(new PluralRules.FixedDecimal(lowDouble, lowFpos.getCountVisibleFractionDigits(), lowFpos.getFractionDigits()));
        double highDouble = highNumber.doubleValue();
        String keywordHigh = this.rules.select(new PluralRules.FixedDecimal(highDouble, highFpos.getCountVisibleFractionDigits(), highFpos.getFractionDigits()));
        PluralRanges pluralRanges = PluralRules.Factory.getDefaultFactory().getPluralRanges(getLocale());
        StandardPlural resolvedPlural = pluralRanges.get(StandardPlural.fromString(keywordLow), StandardPlural.fromString(keywordHigh));
        String rangeFormatter = getRangeFormat(getLocale(), this.formatWidth);
        String formattedNumber = SimplePatternFormatter.formatCompiledPattern(rangeFormatter, lowFormatted, highFormatted);
        if (isCurrency) {
            this.currencyFormat.format(Double.valueOf(1.0d));
            Currency currencyUnit = (Currency) unit;
            StringBuilder result = new StringBuilder();
            appendReplacingCurrency(this.currencyFormat.getPrefix(lowDouble >= 0.0d), currencyUnit, resolvedPlural, result);
            result.append(formattedNumber);
            appendReplacingCurrency(this.currencyFormat.getSuffix(highDouble >= 0.0d), currencyUnit, resolvedPlural, result);
            return result.toString();
        }
        String formatter = getPluralFormatter(lowValue.getUnit(), this.formatWidth, resolvedPlural.ordinal());
        return SimplePatternFormatter.formatCompiledPattern(formatter, formattedNumber);
    }

    private void appendReplacingCurrency(String affix, Currency unit, StandardPlural resolvedPlural, StringBuilder result) {
        String replacement = "¤";
        int pos = affix.indexOf("¤");
        if (pos < 0) {
            replacement = "XXX";
            pos = affix.indexOf("XXX");
        }
        if (pos < 0) {
            result.append(affix);
            return;
        }
        result.append(affix.substring(0, pos));
        int currentStyle = this.formatWidth.getCurrencyStyle();
        if (currentStyle == 5) {
            result.append(unit.getCurrencyCode());
        } else {
            result.append(unit.getName(this.currencyFormat.nf.getLocale(ULocale.ACTUAL_LOCALE), currentStyle != 1 ? 2 : 0, resolvedPlural.getKeyword(), (boolean[]) null));
        }
        result.append(affix.substring(replacement.length() + pos));
    }

    public StringBuilder formatMeasurePerUnit(Measure measure, MeasureUnit perUnit, StringBuilder appendTo, FieldPosition pos) {
        MeasureUnit resolvedUnit = MeasureUnit.resolveUnitPerUnit(measure.getUnit(), perUnit);
        if (resolvedUnit != null) {
            Measure newMeasure = new Measure(measure.getNumber(), resolvedUnit);
            return formatMeasure(newMeasure, this.numberFormat, appendTo, pos);
        }
        FieldPosition fpos = new FieldPosition(pos.getFieldAttribute(), pos.getField());
        int offset = withPerUnitAndAppend(formatMeasure(measure, this.numberFormat, new StringBuilder(), fpos), perUnit, appendTo);
        if (fpos.getBeginIndex() != 0 || fpos.getEndIndex() != 0) {
            pos.setBeginIndex(fpos.getBeginIndex() + offset);
            pos.setEndIndex(fpos.getEndIndex() + offset);
        }
        return appendTo;
    }

    public StringBuilder formatMeasures(StringBuilder appendTo, FieldPosition fieldPosition, Measure... measures) {
        Number[] hms;
        if (measures.length == 0) {
            return appendTo;
        }
        if (measures.length == 1) {
            return formatMeasure(measures[0], this.numberFormat, appendTo, fieldPosition);
        }
        if (this.formatWidth == FormatWidth.NUMERIC && (hms = toHMS(measures)) != null) {
            return formatNumeric(hms, appendTo);
        }
        ListFormatter listFormatter = ListFormatter.getInstance(getLocale(), this.formatWidth.getListFormatterStyle());
        if (fieldPosition != DontCareFieldPosition.INSTANCE) {
            return formatMeasuresSlowTrack(listFormatter, appendTo, fieldPosition, measures);
        }
        String[] results = new String[measures.length];
        int i = 0;
        while (i < measures.length) {
            results[i] = formatMeasure(measures[i], i == measures.length + (-1) ? this.numberFormat : this.integerFormat);
            i++;
        }
        return appendTo.append(listFormatter.format(results));
    }

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj instanceof MeasureFormat) && getWidth() == obj.getWidth() && getLocale().equals(obj.getLocale())) {
            return getNumberFormat().equals(obj.getNumberFormat());
        }
        return false;
    }

    public final int hashCode() {
        return (((getLocale().hashCode() * 31) + getNumberFormat().hashCode()) * 31) + getWidth().hashCode();
    }

    public FormatWidth getWidth() {
        return this.formatWidth;
    }

    public final ULocale getLocale() {
        return getLocale(ULocale.VALID_LOCALE);
    }

    public NumberFormat getNumberFormat() {
        return this.numberFormat.get();
    }

    public static MeasureFormat getCurrencyFormat(ULocale locale) {
        return new CurrencyFormat(locale);
    }

    public static MeasureFormat getCurrencyFormat(Locale locale) {
        return getCurrencyFormat(ULocale.forLocale(locale));
    }

    public static MeasureFormat getCurrencyFormat() {
        return getCurrencyFormat(ULocale.getDefault(ULocale.Category.FORMAT));
    }

    MeasureFormat withLocale(ULocale locale) {
        return getInstance(locale, getWidth());
    }

    MeasureFormat withNumberFormat(NumberFormat format) {
        return new MeasureFormat(getLocale(), this.cache, this.formatWidth, new ImmutableNumberFormat(format), this.rules, this.numericFormatters, this.currencyFormat, this.integerFormat);
    }

    private MeasureFormat(ULocale locale, MeasureFormatData data, FormatWidth formatWidth, ImmutableNumberFormat format, PluralRules rules, NumericFormatters formatters, ImmutableNumberFormat currencyFormat, ImmutableNumberFormat integerFormat) {
        setLocale(locale, locale);
        this.cache = data;
        this.formatWidth = formatWidth;
        this.numberFormat = format;
        this.rules = rules;
        this.numericFormatters = formatters;
        this.currencyFormat = currencyFormat;
        this.integerFormat = integerFormat;
    }

    MeasureFormat() {
        this.cache = null;
        this.formatWidth = null;
        this.numberFormat = null;
        this.rules = null;
        this.numericFormatters = null;
        this.currencyFormat = null;
        this.integerFormat = null;
    }

    static class NumericFormatters {
        private DateFormat hourMinute;
        private DateFormat hourMinuteSecond;
        private DateFormat minuteSecond;

        public NumericFormatters(DateFormat hourMinute, DateFormat minuteSecond, DateFormat hourMinuteSecond) {
            this.hourMinute = hourMinute;
            this.minuteSecond = minuteSecond;
            this.hourMinuteSecond = hourMinuteSecond;
        }

        public DateFormat getHourMinute() {
            return this.hourMinute;
        }

        public DateFormat getMinuteSecond() {
            return this.minuteSecond;
        }

        public DateFormat getHourMinuteSecond() {
            return this.hourMinuteSecond;
        }
    }

    private static NumericFormatters loadNumericFormatters(ULocale locale) {
        ICUResourceBundle r = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME, locale);
        return new NumericFormatters(loadNumericDurationFormat(r, "hm"), loadNumericDurationFormat(r, DateFormat.MINUTE_SECOND), loadNumericDurationFormat(r, "hms"));
    }

    private static final class UnitDataSink extends UResource.TableSink {
        MeasureFormatData cacheData;
        String type;
        MeasureUnit unit;
        FormatWidth width;
        UnitPatternSink patternSink = new UnitPatternSink();
        UnitSubtypeSink subtypeSink = new UnitSubtypeSink();
        UnitCompoundSink compoundSink = new UnitCompoundSink();
        UnitTypeSink typeSink = new UnitTypeSink();
        StringBuilder sb = new StringBuilder();

        class UnitPatternSink extends UResource.TableSink {
            String[] patterns;

            UnitPatternSink() {
            }

            void setFormatterIfAbsent(int index, UResource.Value value, int minPlaceholders) {
                if (this.patterns == null) {
                    EnumMap<FormatWidth, String[]> styleToPatterns = UnitDataSink.this.cacheData.unitToStyleToPatterns.get(UnitDataSink.this.unit);
                    if (styleToPatterns == null) {
                        styleToPatterns = new EnumMap<>(FormatWidth.class);
                        UnitDataSink.this.cacheData.unitToStyleToPatterns.put(UnitDataSink.this.unit, styleToPatterns);
                    } else {
                        this.patterns = styleToPatterns.get(UnitDataSink.this.width);
                    }
                    if (this.patterns == null) {
                        this.patterns = new String[MeasureFormatData.PATTERN_COUNT];
                        styleToPatterns.put(UnitDataSink.this.width, this.patterns);
                    }
                }
                if (this.patterns[index] != null) {
                    return;
                }
                this.patterns[index] = SimplePatternFormatter.compileToStringMinMaxPlaceholders(value.getString(), UnitDataSink.this.sb, minPlaceholders, 1);
            }

            @Override
            public void put(UResource.Key key, UResource.Value value) {
                if (key.contentEquals("dnam")) {
                    return;
                }
                if (key.contentEquals("per")) {
                    setFormatterIfAbsent(MeasureFormatData.PER_UNIT_INDEX, value, 0);
                } else {
                    setFormatterIfAbsent(StandardPlural.indexFromString(key), value, 0);
                }
            }
        }

        class UnitSubtypeSink extends UResource.TableSink {
            UnitSubtypeSink() {
            }

            @Override
            public UResource.TableSink getOrCreateTableSink(UResource.Key key, int initialSize) {
                UnitDataSink.this.unit = MeasureUnit.internalGetInstance(UnitDataSink.this.type, key.toString());
                UnitDataSink.this.patternSink.patterns = null;
                return UnitDataSink.this.patternSink;
            }
        }

        class UnitCompoundSink extends UResource.TableSink {
            UnitCompoundSink() {
            }

            @Override
            public void put(UResource.Key key, UResource.Value value) {
                if (!key.contentEquals("per")) {
                    return;
                }
                UnitDataSink.this.cacheData.styleToPerPattern.put(UnitDataSink.this.width, SimplePatternFormatter.compileToStringMinMaxPlaceholders(value.getString(), UnitDataSink.this.sb, 2, 2));
            }
        }

        class UnitTypeSink extends UResource.TableSink {
            UnitTypeSink() {
            }

            @Override
            public UResource.TableSink getOrCreateTableSink(UResource.Key key, int initialSize) {
                if (!key.contentEquals("currency")) {
                    if (key.contentEquals("compound")) {
                        if (!UnitDataSink.this.cacheData.hasPerFormatter(UnitDataSink.this.width)) {
                            return UnitDataSink.this.compoundSink;
                        }
                        return null;
                    }
                    UnitDataSink.this.type = key.toString();
                    return UnitDataSink.this.subtypeSink;
                }
                return null;
            }
        }

        UnitDataSink(MeasureFormatData outputData) {
            this.cacheData = outputData;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value) {
            FormatWidth sourceWidth;
            if (value.getType() == 3 && (sourceWidth = widthFromKey(key)) != null) {
                FormatWidth targetWidth = widthFromAlias(value);
                if (targetWidth == null) {
                    throw new ICUException("Units data fallback from " + ((Object) key) + " to unknown " + value.getAliasString());
                }
                if (this.cacheData.widthFallback[targetWidth.ordinal()] != null) {
                    throw new ICUException("Units data fallback from " + ((Object) key) + " to " + value.getAliasString() + " which falls back to something else");
                }
                this.cacheData.widthFallback[sourceWidth.ordinal()] = targetWidth;
            }
        }

        @Override
        public UResource.TableSink getOrCreateTableSink(UResource.Key key, int initialSize) {
            FormatWidth formatWidthWidthFromKey = widthFromKey(key);
            this.width = formatWidthWidthFromKey;
            if (formatWidthWidthFromKey != null) {
                return this.typeSink;
            }
            return null;
        }

        static FormatWidth widthFromKey(UResource.Key key) {
            if (key.startsWith("units")) {
                if (key.length() == 5) {
                    return FormatWidth.WIDE;
                }
                if (key.regionMatches(5, "Short")) {
                    return FormatWidth.SHORT;
                }
                if (key.regionMatches(5, "Narrow")) {
                    return FormatWidth.NARROW;
                }
                return null;
            }
            return null;
        }

        static FormatWidth widthFromAlias(UResource.Value value) {
            String s = value.getAliasString();
            if (s.startsWith("/LOCALE/units")) {
                if (s.length() == 13) {
                    return FormatWidth.WIDE;
                }
                if (s.length() == 18 && s.endsWith("Short")) {
                    return FormatWidth.SHORT;
                }
                if (s.length() == 19 && s.endsWith("Narrow")) {
                    return FormatWidth.NARROW;
                }
                return null;
            }
            return null;
        }
    }

    private static MeasureFormatData loadLocaleData(ULocale locale) {
        ICUResourceBundle resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME, locale);
        MeasureFormatData cacheData = new MeasureFormatData(null);
        UnitDataSink sink = new UnitDataSink(cacheData);
        resource.getAllTableItemsWithFallback("", sink);
        return cacheData;
    }

    private static final FormatWidth getRegularWidth(FormatWidth width) {
        if (width == FormatWidth.NUMERIC) {
            return FormatWidth.NARROW;
        }
        return width;
    }

    private String getFormatterOrNull(MeasureUnit unit, FormatWidth width, int index) {
        String[] patterns;
        FormatWidth width2 = getRegularWidth(width);
        Map<FormatWidth, String[]> styleToPatterns = this.cache.unitToStyleToPatterns.get(unit);
        String[] patterns2 = styleToPatterns.get(width2);
        if (patterns2 != null && patterns2[index] != null) {
            return patterns2[index];
        }
        FormatWidth fallbackWidth = this.cache.widthFallback[width2.ordinal()];
        if (fallbackWidth == null || (patterns = styleToPatterns.get(fallbackWidth)) == null || patterns[index] == null) {
            return null;
        }
        return patterns[index];
    }

    private String getFormatter(MeasureUnit unit, FormatWidth width, int index) {
        String pattern = getFormatterOrNull(unit, width, index);
        if (pattern == null) {
            throw new MissingResourceException("no formatting pattern for " + unit + ", width " + width + ", index " + index, null, null);
        }
        return pattern;
    }

    private String getPluralFormatter(MeasureUnit unit, FormatWidth width, int index) {
        String pattern;
        if (index != StandardPlural.OTHER_INDEX && (pattern = getFormatterOrNull(unit, width, index)) != null) {
            return pattern;
        }
        return getFormatter(unit, width, StandardPlural.OTHER_INDEX);
    }

    private String getPerFormatter(FormatWidth width) {
        String perPattern;
        FormatWidth width2 = getRegularWidth(width);
        String perPattern2 = this.cache.styleToPerPattern.get(width2);
        if (perPattern2 != null) {
            return perPattern2;
        }
        FormatWidth fallbackWidth = this.cache.widthFallback[width2.ordinal()];
        if (fallbackWidth != null && (perPattern = this.cache.styleToPerPattern.get(fallbackWidth)) != null) {
            return perPattern;
        }
        throw new MissingResourceException("no x-per-y pattern for width " + width2, null, null);
    }

    private int withPerUnitAndAppend(CharSequence formatted, MeasureUnit perUnit, StringBuilder appendTo) {
        int[] offsets = new int[1];
        String perUnitPattern = getFormatterOrNull(perUnit, this.formatWidth, MeasureFormatData.PER_UNIT_INDEX);
        if (perUnitPattern != null) {
            SimplePatternFormatter.formatAndAppend(perUnitPattern, appendTo, offsets, formatted);
            return offsets[0];
        }
        String perPattern = getPerFormatter(this.formatWidth);
        String pattern = getPluralFormatter(perUnit, this.formatWidth, StandardPlural.ONE.ordinal());
        String perUnitString = SimplePatternFormatter.getTextWithNoPlaceholders(pattern).trim();
        SimplePatternFormatter.formatAndAppend(perPattern, appendTo, offsets, formatted, perUnitString);
        return offsets[0];
    }

    private String formatMeasure(Measure measure, ImmutableNumberFormat nf) {
        return formatMeasure(measure, nf, new StringBuilder(), DontCareFieldPosition.INSTANCE).toString();
    }

    private StringBuilder formatMeasure(Measure measure, ImmutableNumberFormat nf, StringBuilder appendTo, FieldPosition fieldPosition) {
        Number n = measure.getNumber();
        ?? unit = measure.getUnit();
        if (unit instanceof Currency) {
            return appendTo.append(this.currencyFormat.format(new CurrencyAmount(n, (Currency) unit), new StringBuffer(), fieldPosition));
        }
        StringBuffer formattedNumber = new StringBuffer();
        StandardPlural pluralForm = QuantityFormatter.selectPlural(n, nf.nf, this.rules, formattedNumber, fieldPosition);
        String formatter = getPluralFormatter(unit, this.formatWidth, pluralForm.ordinal());
        return QuantityFormatter.format(formatter, formattedNumber, appendTo, fieldPosition);
    }

    private static final class MeasureFormatData {
        final EnumMap<FormatWidth, String> styleToPerPattern;
        final Map<MeasureUnit, EnumMap<FormatWidth, String[]>> unitToStyleToPatterns;
        final FormatWidth[] widthFallback;
        static final int PER_UNIT_INDEX = StandardPlural.COUNT;
        static final int PATTERN_COUNT = PER_UNIT_INDEX + 1;

        MeasureFormatData(MeasureFormatData measureFormatData) {
            this();
        }

        private MeasureFormatData() {
            this.widthFallback = new FormatWidth[3];
            this.unitToStyleToPatterns = new HashMap();
            this.styleToPerPattern = new EnumMap<>(FormatWidth.class);
        }

        boolean hasPerFormatter(FormatWidth width) {
            return this.styleToPerPattern.containsKey(width);
        }
    }

    private static final class ImmutableNumberFormat {
        private NumberFormat nf;

        public ImmutableNumberFormat(NumberFormat nf) {
            this.nf = (NumberFormat) nf.clone();
        }

        public synchronized NumberFormat get() {
            return (NumberFormat) this.nf.clone();
        }

        public synchronized StringBuffer format(Number n, StringBuffer buffer, FieldPosition pos) {
            return this.nf.format(n, buffer, pos);
        }

        public synchronized StringBuffer format(CurrencyAmount n, StringBuffer buffer, FieldPosition pos) {
            return this.nf.format(n, buffer, pos);
        }

        public synchronized String format(Number number) {
            return this.nf.format(number);
        }

        public String getPrefix(boolean positive) {
            return positive ? ((DecimalFormat) this.nf).getPositivePrefix() : ((DecimalFormat) this.nf).getNegativePrefix();
        }

        public String getSuffix(boolean positive) {
            return positive ? ((DecimalFormat) this.nf).getPositiveSuffix() : ((DecimalFormat) this.nf).getPositiveSuffix();
        }
    }

    static final class PatternData {
        final String prefix;
        final String suffix;

        public PatternData(String pattern) {
            int pos = pattern.indexOf("{0}");
            if (pos < 0) {
                this.prefix = pattern;
                this.suffix = null;
            } else {
                this.prefix = pattern.substring(0, pos);
                this.suffix = pattern.substring(pos + 3);
            }
        }

        public String toString() {
            return this.prefix + "; " + this.suffix;
        }
    }

    Object toTimeUnitProxy() {
        return new MeasureProxy(getLocale(), this.formatWidth, this.numberFormat.get(), 1);
    }

    Object toCurrencyProxy() {
        return new MeasureProxy(getLocale(), this.formatWidth, this.numberFormat.get(), 2);
    }

    private StringBuilder formatMeasuresSlowTrack(ListFormatter listFormatter, StringBuilder appendTo, FieldPosition fieldPosition, Measure... measures) {
        String[] results = new String[measures.length];
        FieldPosition fpos = new FieldPosition(fieldPosition.getFieldAttribute(), fieldPosition.getField());
        int fieldPositionFoundIndex = -1;
        int i = 0;
        while (i < measures.length) {
            ImmutableNumberFormat nf = i == measures.length + (-1) ? this.numberFormat : this.integerFormat;
            if (fieldPositionFoundIndex == -1) {
                results[i] = formatMeasure(measures[i], nf, new StringBuilder(), fpos).toString();
                if (fpos.getBeginIndex() != 0 || fpos.getEndIndex() != 0) {
                    fieldPositionFoundIndex = i;
                }
            } else {
                results[i] = formatMeasure(measures[i], nf);
            }
            i++;
        }
        ListFormatter.FormattedListBuilder builder = listFormatter.format(Arrays.asList(results), fieldPositionFoundIndex);
        if (builder.getOffset() != -1) {
            fieldPosition.setBeginIndex(fpos.getBeginIndex() + builder.getOffset() + appendTo.length());
            fieldPosition.setEndIndex(fpos.getEndIndex() + builder.getOffset() + appendTo.length());
        }
        return appendTo.append(builder.toString());
    }

    private static DateFormat loadNumericDurationFormat(ICUResourceBundle r, String type) {
        DateFormat result = new SimpleDateFormat(r.getWithFallback(String.format("durationUnits/%s", type)).getString().replace("h", DateFormat.HOUR24));
        result.setTimeZone(TimeZone.GMT_ZONE);
        return result;
    }

    private static Number[] toHMS(Measure[] measures) {
        Integer idxObj;
        int idx;
        Number[] result = new Number[3];
        int lastIdx = -1;
        for (Measure m : measures) {
            if (m.getNumber().doubleValue() < 0.0d || (idxObj = hmsTo012.get(m.getUnit())) == null || (idx = idxObj.intValue()) <= lastIdx) {
                return null;
            }
            lastIdx = idx;
            result[idx] = m.getNumber();
        }
        return result;
    }

    private StringBuilder formatNumeric(Number[] hms, StringBuilder appendable) {
        int startIndex = -1;
        int endIndex = -1;
        for (int i = 0; i < hms.length; i++) {
            if (hms[i] != null) {
                endIndex = i;
                if (startIndex == -1) {
                    startIndex = endIndex;
                }
            } else {
                hms[i] = 0;
            }
        }
        long millis = (long) (((((Math.floor(hms[0].doubleValue()) * 60.0d) + Math.floor(hms[1].doubleValue())) * 60.0d) + Math.floor(hms[2].doubleValue())) * 1000.0d);
        Date d = new Date(millis);
        if (startIndex == 0 && endIndex == 2) {
            return formatNumeric(d, this.numericFormatters.getHourMinuteSecond(), DateFormat.Field.SECOND, hms[endIndex], appendable);
        }
        if (startIndex == 1 && endIndex == 2) {
            return formatNumeric(d, this.numericFormatters.getMinuteSecond(), DateFormat.Field.SECOND, hms[endIndex], appendable);
        }
        if (startIndex == 0 && endIndex == 1) {
            return formatNumeric(d, this.numericFormatters.getHourMinute(), DateFormat.Field.MINUTE, hms[endIndex], appendable);
        }
        throw new IllegalStateException();
    }

    private StringBuilder formatNumeric(Date duration, DateFormat formatter, DateFormat.Field smallestField, Number smallestAmount, StringBuilder appendTo) {
        FieldPosition intFieldPosition = new FieldPosition(0);
        String smallestAmountFormatted = this.numberFormat.format(smallestAmount, new StringBuffer(), intFieldPosition).toString();
        if (intFieldPosition.getBeginIndex() == 0 && intFieldPosition.getEndIndex() == 0) {
            throw new IllegalStateException();
        }
        FieldPosition smallestFieldPosition = new FieldPosition(smallestField);
        String draft = formatter.format(duration, new StringBuffer(), smallestFieldPosition).toString();
        if (smallestFieldPosition.getBeginIndex() != 0 || smallestFieldPosition.getEndIndex() != 0) {
            appendTo.append((CharSequence) draft, 0, smallestFieldPosition.getBeginIndex());
            appendTo.append((CharSequence) smallestAmountFormatted, 0, intFieldPosition.getBeginIndex());
            appendTo.append((CharSequence) draft, smallestFieldPosition.getBeginIndex(), smallestFieldPosition.getEndIndex());
            appendTo.append((CharSequence) smallestAmountFormatted, intFieldPosition.getEndIndex(), smallestAmountFormatted.length());
            appendTo.append((CharSequence) draft, smallestFieldPosition.getEndIndex(), draft.length());
        } else {
            appendTo.append(draft);
        }
        return appendTo;
    }

    private Object writeReplace() throws ObjectStreamException {
        return new MeasureProxy(getLocale(), this.formatWidth, this.numberFormat.get(), 0);
    }

    static class MeasureProxy implements Externalizable {
        private static final long serialVersionUID = -6033308329886716770L;
        private FormatWidth formatWidth;
        private HashMap<Object, Object> keyValues;
        private ULocale locale;
        private NumberFormat numberFormat;
        private int subClass;

        public MeasureProxy(ULocale locale, FormatWidth width, NumberFormat numberFormat, int subClass) {
            this.locale = locale;
            this.formatWidth = width;
            this.numberFormat = numberFormat;
            this.subClass = subClass;
            this.keyValues = new HashMap<>();
        }

        public MeasureProxy() {
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeByte(0);
            out.writeUTF(this.locale.toLanguageTag());
            out.writeByte(this.formatWidth.ordinal());
            out.writeObject(this.numberFormat);
            out.writeByte(this.subClass);
            out.writeObject(this.keyValues);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            in.readByte();
            this.locale = ULocale.forLanguageTag(in.readUTF());
            this.formatWidth = MeasureFormat.fromFormatWidthOrdinal(in.readByte() & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
            this.numberFormat = (NumberFormat) in.readObject();
            if (this.numberFormat == null) {
                throw new InvalidObjectException("Missing number format.");
            }
            this.subClass = in.readByte() & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            this.keyValues = (HashMap) in.readObject();
            if (this.keyValues != null) {
            } else {
                throw new InvalidObjectException("Missing optional values map.");
            }
        }

        private TimeUnitFormat createTimeUnitFormat() throws InvalidObjectException {
            int style;
            if (this.formatWidth == FormatWidth.WIDE) {
                style = 0;
            } else if (this.formatWidth == FormatWidth.SHORT) {
                style = 1;
            } else {
                throw new InvalidObjectException("Bad width: " + this.formatWidth);
            }
            TimeUnitFormat result = new TimeUnitFormat(this.locale, style);
            result.setNumberFormat(this.numberFormat);
            return result;
        }

        private Object readResolve() throws ObjectStreamException {
            switch (this.subClass) {
                case 0:
                    return MeasureFormat.getInstance(this.locale, this.formatWidth, this.numberFormat);
                case 1:
                    return createTimeUnitFormat();
                case 2:
                    return new CurrencyFormat(this.locale);
                default:
                    throw new InvalidObjectException("Unknown subclass: " + this.subClass);
            }
        }
    }

    private static FormatWidth fromFormatWidthOrdinal(int ordinal) {
        FormatWidth[] values = FormatWidth.valuesCustom();
        if (ordinal < 0 || ordinal >= values.length) {
            return FormatWidth.SHORT;
        }
        return values[ordinal];
    }

    @Deprecated
    public static String getRangeFormat(ULocale forLocale, FormatWidth width) {
        String resultString;
        String result;
        if (forLocale.getLanguage().equals("fr")) {
            return getRangeFormat(ULocale.ROOT, width);
        }
        String result2 = localeIdToRangeFormat.get(forLocale);
        if (result2 == null) {
            ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", forLocale);
            ULocale realLocale = rb.getULocale();
            if (!forLocale.equals(realLocale) && (result = localeIdToRangeFormat.get(forLocale)) != null) {
                localeIdToRangeFormat.put(forLocale, result);
                return result;
            }
            NumberingSystem ns = NumberingSystem.getInstance(forLocale);
            try {
                resultString = rb.getStringWithFallback("NumberElements/" + ns.getName() + "/miscPatterns/range");
            } catch (MissingResourceException e) {
                resultString = rb.getStringWithFallback("NumberElements/latn/patterns/range");
            }
            result2 = SimplePatternFormatter.compileToStringMinMaxPlaceholders(resultString, new StringBuilder(), 2, 2);
            localeIdToRangeFormat.put(forLocale, result2);
            if (!forLocale.equals(realLocale)) {
                localeIdToRangeFormat.put(realLocale, result2);
            }
        }
        return result2;
    }
}
