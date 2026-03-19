package android.icu.text;

import android.icu.impl.DontCareFieldPosition;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SimpleCache;
import android.icu.impl.SimpleFormatterImpl;
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

        FormatWidth(ListFormatter.Style style, int i) {
            this.listFormatterStyle = style;
            this.currencyStyle = i;
        }

        ListFormatter.Style getListFormatterStyle() {
            return this.listFormatterStyle;
        }

        int getCurrencyStyle() {
            return this.currencyStyle;
        }
    }

    public static MeasureFormat getInstance(ULocale uLocale, FormatWidth formatWidth) {
        return getInstance(uLocale, formatWidth, NumberFormat.getInstance(uLocale));
    }

    public static MeasureFormat getInstance(Locale locale, FormatWidth formatWidth) {
        return getInstance(ULocale.forLocale(locale), formatWidth);
    }

    public static MeasureFormat getInstance(ULocale uLocale, FormatWidth formatWidth, NumberFormat numberFormat) {
        NumericFormatters numericFormattersLoadNumericFormatters;
        PluralRules pluralRulesForLocale = PluralRules.forLocale(uLocale);
        MeasureFormatData measureFormatDataLoadLocaleData = localeMeasureFormatData.get(uLocale);
        if (measureFormatDataLoadLocaleData == null) {
            measureFormatDataLoadLocaleData = loadLocaleData(uLocale);
            localeMeasureFormatData.put(uLocale, measureFormatDataLoadLocaleData);
        }
        MeasureFormatData measureFormatData = measureFormatDataLoadLocaleData;
        if (formatWidth == FormatWidth.NUMERIC) {
            numericFormattersLoadNumericFormatters = localeToNumericDurationFormatters.get(uLocale);
            if (numericFormattersLoadNumericFormatters == null) {
                numericFormattersLoadNumericFormatters = loadNumericFormatters(uLocale);
                localeToNumericDurationFormatters.put(uLocale, numericFormattersLoadNumericFormatters);
            }
        } else {
            numericFormattersLoadNumericFormatters = null;
        }
        NumericFormatters numericFormatters = numericFormattersLoadNumericFormatters;
        NumberFormat numberFormat2 = NumberFormat.getInstance(uLocale);
        numberFormat2.setMaximumFractionDigits(0);
        numberFormat2.setMinimumFractionDigits(0);
        numberFormat2.setRoundingMode(1);
        return new MeasureFormat(uLocale, measureFormatData, formatWidth, new ImmutableNumberFormat(numberFormat), pluralRulesForLocale, numericFormatters, new ImmutableNumberFormat(NumberFormat.getInstance(uLocale, formatWidth.getCurrencyStyle())), new ImmutableNumberFormat(numberFormat2));
    }

    public static MeasureFormat getInstance(Locale locale, FormatWidth formatWidth, NumberFormat numberFormat) {
        return getInstance(ULocale.forLocale(locale), formatWidth, numberFormat);
    }

    @Override
    public StringBuffer format(Object obj, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        int length = stringBuffer.length();
        FieldPosition fieldPosition2 = new FieldPosition(fieldPosition.getFieldAttribute(), fieldPosition.getField());
        if (obj instanceof Collection) {
            Collection collection = (Collection) obj;
            Measure[] measureArr = new Measure[collection.size()];
            int i = 0;
            for (Object obj2 : collection) {
                if (!(obj2 instanceof Measure)) {
                    throw new IllegalArgumentException(obj.toString());
                }
                measureArr[i] = (Measure) obj2;
                i++;
            }
            stringBuffer.append((CharSequence) formatMeasures(new StringBuilder(), fieldPosition2, measureArr));
        } else if (obj instanceof Measure[]) {
            stringBuffer.append((CharSequence) formatMeasures(new StringBuilder(), fieldPosition2, (Measure[]) obj));
        } else if (obj instanceof Measure) {
            stringBuffer.append((CharSequence) formatMeasure((Measure) obj, this.numberFormat, new StringBuilder(), fieldPosition2));
        } else {
            throw new IllegalArgumentException(obj.toString());
        }
        if (fieldPosition2.getBeginIndex() != 0 || fieldPosition2.getEndIndex() != 0) {
            fieldPosition.setBeginIndex(fieldPosition2.getBeginIndex() + length);
            fieldPosition.setEndIndex(fieldPosition2.getEndIndex() + length);
        }
        return stringBuffer;
    }

    @Override
    public Measure parseObject(String str, ParsePosition parsePosition) {
        throw new UnsupportedOperationException();
    }

    public final String formatMeasures(Measure... measureArr) {
        return formatMeasures(new StringBuilder(), DontCareFieldPosition.INSTANCE, measureArr).toString();
    }

    @Deprecated
    public final String formatMeasureRange(Measure measure, Measure measure2) {
        StringBuffer stringBuffer;
        MeasureUnit unit = measure.getUnit();
        if (!unit.equals(measure2.getUnit())) {
            throw new IllegalArgumentException("Units must match: " + unit + " ≠ " + measure2.getUnit());
        }
        Number number = measure.getNumber();
        Number number2 = measure2.getNumber();
        boolean z = unit instanceof Currency;
        UFieldPosition uFieldPosition = new UFieldPosition();
        UFieldPosition uFieldPosition2 = new UFieldPosition();
        StringBuffer stringBuffer2 = null;
        if (z) {
            int defaultFractionDigits = ((Currency) unit).getDefaultFractionDigits();
            int maximumFractionDigits = this.numberFormat.nf.getMaximumFractionDigits();
            int minimumFractionDigits = this.numberFormat.nf.getMinimumFractionDigits();
            if (defaultFractionDigits == maximumFractionDigits && defaultFractionDigits == minimumFractionDigits) {
                stringBuffer = null;
            } else {
                DecimalFormat decimalFormat = (DecimalFormat) this.numberFormat.get();
                decimalFormat.setMaximumFractionDigits(defaultFractionDigits);
                decimalFormat.setMinimumFractionDigits(defaultFractionDigits);
                StringBuffer stringBuffer3 = decimalFormat.format(number, new StringBuffer(), uFieldPosition);
                stringBuffer = decimalFormat.format(number2, new StringBuffer(), uFieldPosition2);
                stringBuffer2 = stringBuffer3;
            }
        }
        if (stringBuffer2 == null) {
            stringBuffer2 = this.numberFormat.format(number, new StringBuffer(), uFieldPosition);
            stringBuffer = this.numberFormat.format(number2, new StringBuffer(), uFieldPosition2);
        }
        double dDoubleValue = number.doubleValue();
        String strSelect = this.rules.select(new PluralRules.FixedDecimal(dDoubleValue, uFieldPosition.getCountVisibleFractionDigits(), uFieldPosition.getFractionDigits()));
        double dDoubleValue2 = number2.doubleValue();
        StandardPlural standardPlural = PluralRules.Factory.getDefaultFactory().getPluralRanges(getLocale()).get(StandardPlural.fromString(strSelect), StandardPlural.fromString(this.rules.select(new PluralRules.FixedDecimal(dDoubleValue2, uFieldPosition2.getCountVisibleFractionDigits(), uFieldPosition2.getFractionDigits()))));
        String compiledPattern = SimpleFormatterImpl.formatCompiledPattern(getRangeFormat(getLocale(), this.formatWidth), stringBuffer2, stringBuffer);
        if (!z) {
            return SimpleFormatterImpl.formatCompiledPattern(getPluralFormatter(measure.getUnit(), this.formatWidth, standardPlural.ordinal()), compiledPattern);
        }
        this.currencyFormat.format(Double.valueOf(1.0d));
        Currency currency = (Currency) unit;
        StringBuilder sb = new StringBuilder();
        appendReplacingCurrency(this.currencyFormat.getPrefix(dDoubleValue >= 0.0d), currency, standardPlural, sb);
        sb.append(compiledPattern);
        appendReplacingCurrency(this.currencyFormat.getSuffix(dDoubleValue2 >= 0.0d), currency, standardPlural, sb);
        return sb.toString();
    }

    private void appendReplacingCurrency(String str, Currency currency, StandardPlural standardPlural, StringBuilder sb) {
        String str2 = "¤";
        int iIndexOf = str.indexOf("¤");
        if (iIndexOf < 0) {
            str2 = "XXX";
            iIndexOf = str.indexOf("XXX");
        }
        if (iIndexOf < 0) {
            sb.append(str);
            return;
        }
        sb.append(str.substring(0, iIndexOf));
        int currencyStyle = this.formatWidth.getCurrencyStyle();
        if (currencyStyle == 5) {
            sb.append(currency.getCurrencyCode());
        } else {
            sb.append(currency.getName(this.currencyFormat.nf.getLocale(ULocale.ACTUAL_LOCALE), currencyStyle != 1 ? 2 : 0, standardPlural.getKeyword(), (boolean[]) null));
        }
        sb.append(str.substring(iIndexOf + str2.length()));
    }

    public StringBuilder formatMeasurePerUnit(Measure measure, MeasureUnit measureUnit, StringBuilder sb, FieldPosition fieldPosition) {
        MeasureUnit measureUnitResolveUnitPerUnit = MeasureUnit.resolveUnitPerUnit(measure.getUnit(), measureUnit);
        if (measureUnitResolveUnitPerUnit != null) {
            return formatMeasure(new Measure(measure.getNumber(), measureUnitResolveUnitPerUnit), this.numberFormat, sb, fieldPosition);
        }
        FieldPosition fieldPosition2 = new FieldPosition(fieldPosition.getFieldAttribute(), fieldPosition.getField());
        int iWithPerUnitAndAppend = withPerUnitAndAppend(formatMeasure(measure, this.numberFormat, new StringBuilder(), fieldPosition2), measureUnit, sb);
        if (fieldPosition2.getBeginIndex() != 0 || fieldPosition2.getEndIndex() != 0) {
            fieldPosition.setBeginIndex(fieldPosition2.getBeginIndex() + iWithPerUnitAndAppend);
            fieldPosition.setEndIndex(fieldPosition2.getEndIndex() + iWithPerUnitAndAppend);
        }
        return sb;
    }

    public StringBuilder formatMeasures(StringBuilder sb, FieldPosition fieldPosition, Measure... measureArr) {
        Number[] hms;
        if (measureArr.length == 0) {
            return sb;
        }
        int i = 0;
        if (measureArr.length == 1) {
            return formatMeasure(measureArr[0], this.numberFormat, sb, fieldPosition);
        }
        if (this.formatWidth == FormatWidth.NUMERIC && (hms = toHMS(measureArr)) != null) {
            return formatNumeric(hms, sb);
        }
        ListFormatter listFormatter = ListFormatter.getInstance(getLocale(), this.formatWidth.getListFormatterStyle());
        if (fieldPosition != DontCareFieldPosition.INSTANCE) {
            return formatMeasuresSlowTrack(listFormatter, sb, fieldPosition, measureArr);
        }
        String[] strArr = new String[measureArr.length];
        while (i < measureArr.length) {
            strArr[i] = formatMeasure(measureArr[i], i == measureArr.length - 1 ? this.numberFormat : this.integerFormat);
            i++;
        }
        sb.append(listFormatter.format(strArr));
        return sb;
    }

    public String getUnitDisplayName(MeasureUnit measureUnit) {
        FormatWidth formatWidth;
        FormatWidth regularWidth = getRegularWidth(this.formatWidth);
        EnumMap<FormatWidth, String> enumMap = this.cache.unitToStyleToDnam.get(measureUnit);
        if (enumMap == null) {
            return null;
        }
        String str = enumMap.get(regularWidth);
        if (str == null && (formatWidth = this.cache.widthFallback[regularWidth.ordinal()]) != null) {
            return enumMap.get(formatWidth);
        }
        return str;
    }

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MeasureFormat)) {
            return false;
        }
        MeasureFormat measureFormat = (MeasureFormat) obj;
        return getWidth() == measureFormat.getWidth() && getLocale().equals(measureFormat.getLocale()) && getNumberFormat().equals(measureFormat.getNumberFormat());
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

    public static MeasureFormat getCurrencyFormat(ULocale uLocale) {
        return new CurrencyFormat(uLocale);
    }

    public static MeasureFormat getCurrencyFormat(Locale locale) {
        return getCurrencyFormat(ULocale.forLocale(locale));
    }

    public static MeasureFormat getCurrencyFormat() {
        return getCurrencyFormat(ULocale.getDefault(ULocale.Category.FORMAT));
    }

    MeasureFormat withLocale(ULocale uLocale) {
        return getInstance(uLocale, getWidth());
    }

    MeasureFormat withNumberFormat(NumberFormat numberFormat) {
        return new MeasureFormat(getLocale(), this.cache, this.formatWidth, new ImmutableNumberFormat(numberFormat), this.rules, this.numericFormatters, this.currencyFormat, this.integerFormat);
    }

    private MeasureFormat(ULocale uLocale, MeasureFormatData measureFormatData, FormatWidth formatWidth, ImmutableNumberFormat immutableNumberFormat, PluralRules pluralRules, NumericFormatters numericFormatters, ImmutableNumberFormat immutableNumberFormat2, ImmutableNumberFormat immutableNumberFormat3) {
        setLocale(uLocale, uLocale);
        this.cache = measureFormatData;
        this.formatWidth = formatWidth;
        this.numberFormat = immutableNumberFormat;
        this.rules = pluralRules;
        this.numericFormatters = numericFormatters;
        this.currencyFormat = immutableNumberFormat2;
        this.integerFormat = immutableNumberFormat3;
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

        public NumericFormatters(DateFormat dateFormat, DateFormat dateFormat2, DateFormat dateFormat3) {
            this.hourMinute = dateFormat;
            this.minuteSecond = dateFormat2;
            this.hourMinuteSecond = dateFormat3;
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

    private static NumericFormatters loadNumericFormatters(ULocale uLocale) {
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME, uLocale);
        return new NumericFormatters(loadNumericDurationFormat(iCUResourceBundle, "hm"), loadNumericDurationFormat(iCUResourceBundle, DateFormat.MINUTE_SECOND), loadNumericDurationFormat(iCUResourceBundle, "hms"));
    }

    private static final class UnitDataSink extends UResource.Sink {
        MeasureFormatData cacheData;
        String[] patterns;
        StringBuilder sb = new StringBuilder();
        String type;
        MeasureUnit unit;
        FormatWidth width;

        void setFormatterIfAbsent(int i, UResource.Value value, int i2) {
            if (this.patterns == null) {
                EnumMap<FormatWidth, String[]> enumMap = this.cacheData.unitToStyleToPatterns.get(this.unit);
                if (enumMap == null) {
                    enumMap = new EnumMap<>(FormatWidth.class);
                    this.cacheData.unitToStyleToPatterns.put(this.unit, enumMap);
                } else {
                    this.patterns = enumMap.get(this.width);
                }
                if (this.patterns == null) {
                    this.patterns = new String[MeasureFormatData.PATTERN_COUNT];
                    enumMap.put(this.width, this.patterns);
                }
            }
            if (this.patterns[i] == null) {
                this.patterns[i] = SimpleFormatterImpl.compileToStringMinMaxArguments(value.getString(), this.sb, i2, 1);
            }
        }

        void setDnamIfAbsent(UResource.Value value) {
            EnumMap<FormatWidth, String> enumMap = this.cacheData.unitToStyleToDnam.get(this.unit);
            if (enumMap == null) {
                enumMap = new EnumMap<>(FormatWidth.class);
                this.cacheData.unitToStyleToDnam.put(this.unit, enumMap);
            }
            if (enumMap.get(this.width) == null) {
                enumMap.put(this.width, value.getString());
            }
        }

        void consumePattern(UResource.Key key, UResource.Value value) {
            if (key.contentEquals("dnam")) {
                setDnamIfAbsent(value);
            } else if (key.contentEquals("per")) {
                setFormatterIfAbsent(MeasureFormatData.PER_UNIT_INDEX, value, 1);
            } else {
                setFormatterIfAbsent(StandardPlural.indexFromString(key), value, 0);
            }
        }

        void consumeSubtypeTable(UResource.Key key, UResource.Value value) {
            this.unit = MeasureUnit.internalGetInstance(this.type, key.toString());
            this.patterns = null;
            if (value.getType() == 2) {
                UResource.Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    consumePattern(key, value);
                }
                return;
            }
            throw new ICUException("Data for unit '" + this.unit + "' is in an unknown format");
        }

        void consumeCompoundPattern(UResource.Key key, UResource.Value value) {
            if (key.contentEquals("per")) {
                this.cacheData.styleToPerPattern.put(this.width, SimpleFormatterImpl.compileToStringMinMaxArguments(value.getString(), this.sb, 2, 2));
            }
        }

        void consumeUnitTypesTable(UResource.Key key, UResource.Value value) {
            if (!key.contentEquals("currency")) {
                int i = 0;
                if (key.contentEquals("compound")) {
                    if (!this.cacheData.hasPerFormatter(this.width)) {
                        UResource.Table table = value.getTable();
                        while (table.getKeyAndValue(i, key, value)) {
                            consumeCompoundPattern(key, value);
                            i++;
                        }
                        return;
                    }
                    return;
                }
                if (!key.contentEquals("coordinate")) {
                    this.type = key.toString();
                    UResource.Table table2 = value.getTable();
                    while (table2.getKeyAndValue(i, key, value)) {
                        consumeSubtypeTable(key, value);
                        i++;
                    }
                }
            }
        }

        UnitDataSink(MeasureFormatData measureFormatData) {
            this.cacheData = measureFormatData;
        }

        void consumeAlias(UResource.Key key, UResource.Value value) {
            FormatWidth formatWidthWidthFromKey = widthFromKey(key);
            if (formatWidthWidthFromKey == null) {
                return;
            }
            FormatWidth formatWidthWidthFromAlias = widthFromAlias(value);
            if (formatWidthWidthFromAlias == null) {
                throw new ICUException("Units data fallback from " + ((Object) key) + " to unknown " + value.getAliasString());
            }
            if (this.cacheData.widthFallback[formatWidthWidthFromAlias.ordinal()] != null) {
                throw new ICUException("Units data fallback from " + ((Object) key) + " to " + value.getAliasString() + " which falls back to something else");
            }
            this.cacheData.widthFallback[formatWidthWidthFromKey.ordinal()] = formatWidthWidthFromAlias;
        }

        public void consumeTable(UResource.Key key, UResource.Value value) {
            FormatWidth formatWidthWidthFromKey = widthFromKey(key);
            this.width = formatWidthWidthFromKey;
            if (formatWidthWidthFromKey != null) {
                UResource.Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    consumeUnitTypesTable(key, value);
                }
            }
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
            String aliasString = value.getAliasString();
            if (aliasString.startsWith("/LOCALE/units")) {
                if (aliasString.length() == 13) {
                    return FormatWidth.WIDE;
                }
                if (aliasString.length() == 18 && aliasString.endsWith("Short")) {
                    return FormatWidth.SHORT;
                }
                if (aliasString.length() == 19 && aliasString.endsWith("Narrow")) {
                    return FormatWidth.NARROW;
                }
                return null;
            }
            return null;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean z) {
            UResource.Table table = value.getTable();
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                if (value.getType() == 3) {
                    consumeAlias(key, value);
                } else {
                    consumeTable(key, value);
                }
            }
        }
    }

    private static MeasureFormatData loadLocaleData(ULocale uLocale) {
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME, uLocale);
        MeasureFormatData measureFormatData = new MeasureFormatData();
        iCUResourceBundle.getAllItemsWithFallback("", new UnitDataSink(measureFormatData));
        return measureFormatData;
    }

    private static final FormatWidth getRegularWidth(FormatWidth formatWidth) {
        if (formatWidth == FormatWidth.NUMERIC) {
            return FormatWidth.NARROW;
        }
        return formatWidth;
    }

    private String getFormatterOrNull(MeasureUnit measureUnit, FormatWidth formatWidth, int i) {
        String[] strArr;
        FormatWidth regularWidth = getRegularWidth(formatWidth);
        EnumMap<FormatWidth, String[]> enumMap = this.cache.unitToStyleToPatterns.get(measureUnit);
        String[] strArr2 = enumMap.get(regularWidth);
        if (strArr2 != null && strArr2[i] != null) {
            return strArr2[i];
        }
        FormatWidth formatWidth2 = this.cache.widthFallback[regularWidth.ordinal()];
        if (formatWidth2 != null && (strArr = enumMap.get(formatWidth2)) != null && strArr[i] != null) {
            return strArr[i];
        }
        return null;
    }

    private String getFormatter(MeasureUnit measureUnit, FormatWidth formatWidth, int i) {
        String formatterOrNull = getFormatterOrNull(measureUnit, formatWidth, i);
        if (formatterOrNull == null) {
            throw new MissingResourceException("no formatting pattern for " + measureUnit + ", width " + formatWidth + ", index " + i, null, null);
        }
        return formatterOrNull;
    }

    @Deprecated
    public String getPluralFormatter(MeasureUnit measureUnit, FormatWidth formatWidth, int i) {
        String formatterOrNull;
        if (i != StandardPlural.OTHER_INDEX && (formatterOrNull = getFormatterOrNull(measureUnit, formatWidth, i)) != null) {
            return formatterOrNull;
        }
        return getFormatter(measureUnit, formatWidth, StandardPlural.OTHER_INDEX);
    }

    private String getPerFormatter(FormatWidth formatWidth) {
        String str;
        FormatWidth regularWidth = getRegularWidth(formatWidth);
        String str2 = this.cache.styleToPerPattern.get(regularWidth);
        if (str2 != null) {
            return str2;
        }
        FormatWidth formatWidth2 = this.cache.widthFallback[regularWidth.ordinal()];
        if (formatWidth2 != null && (str = this.cache.styleToPerPattern.get(formatWidth2)) != null) {
            return str;
        }
        throw new MissingResourceException("no x-per-y pattern for width " + regularWidth, null, null);
    }

    private int withPerUnitAndAppend(CharSequence charSequence, MeasureUnit measureUnit, StringBuilder sb) {
        int[] iArr = new int[1];
        String formatterOrNull = getFormatterOrNull(measureUnit, this.formatWidth, MeasureFormatData.PER_UNIT_INDEX);
        if (formatterOrNull != null) {
            SimpleFormatterImpl.formatAndAppend(formatterOrNull, sb, iArr, charSequence);
            return iArr[0];
        }
        SimpleFormatterImpl.formatAndAppend(getPerFormatter(this.formatWidth), sb, iArr, charSequence, SimpleFormatterImpl.getTextWithNoArguments(getPluralFormatter(measureUnit, this.formatWidth, StandardPlural.ONE.ordinal())).trim());
        return iArr[0];
    }

    private String formatMeasure(Measure measure, ImmutableNumberFormat immutableNumberFormat) {
        return formatMeasure(measure, immutableNumberFormat, new StringBuilder(), DontCareFieldPosition.INSTANCE).toString();
    }

    private StringBuilder formatMeasure(Measure measure, ImmutableNumberFormat immutableNumberFormat, StringBuilder sb, FieldPosition fieldPosition) {
        Number number = measure.getNumber();
        MeasureUnit unit = measure.getUnit();
        if (unit instanceof Currency) {
            sb.append(this.currencyFormat.format(new CurrencyAmount(number, (Currency) unit), new StringBuffer(), fieldPosition));
            return sb;
        }
        StringBuffer stringBuffer = new StringBuffer();
        return QuantityFormatter.format(getPluralFormatter(unit, this.formatWidth, QuantityFormatter.selectPlural(number, immutableNumberFormat.nf, this.rules, stringBuffer, fieldPosition).ordinal()), stringBuffer, sb, fieldPosition);
    }

    private static final class MeasureFormatData {
        final EnumMap<FormatWidth, String> styleToPerPattern;
        final Map<MeasureUnit, EnumMap<FormatWidth, String>> unitToStyleToDnam;
        final Map<MeasureUnit, EnumMap<FormatWidth, String[]>> unitToStyleToPatterns;
        final FormatWidth[] widthFallback;
        static final int PER_UNIT_INDEX = StandardPlural.COUNT;
        static final int PATTERN_COUNT = PER_UNIT_INDEX + 1;

        private MeasureFormatData() {
            this.widthFallback = new FormatWidth[3];
            this.unitToStyleToPatterns = new HashMap();
            this.unitToStyleToDnam = new HashMap();
            this.styleToPerPattern = new EnumMap<>(FormatWidth.class);
        }

        boolean hasPerFormatter(FormatWidth formatWidth) {
            return this.styleToPerPattern.containsKey(formatWidth);
        }
    }

    private static final class ImmutableNumberFormat {
        private NumberFormat nf;

        public ImmutableNumberFormat(NumberFormat numberFormat) {
            this.nf = (NumberFormat) numberFormat.clone();
        }

        public synchronized NumberFormat get() {
            return (NumberFormat) this.nf.clone();
        }

        public synchronized StringBuffer format(Number number, StringBuffer stringBuffer, FieldPosition fieldPosition) {
            return this.nf.format(number, stringBuffer, fieldPosition);
        }

        public synchronized StringBuffer format(CurrencyAmount currencyAmount, StringBuffer stringBuffer, FieldPosition fieldPosition) {
            return this.nf.format(currencyAmount, stringBuffer, fieldPosition);
        }

        public synchronized String format(Number number) {
            return this.nf.format(number);
        }

        public String getPrefix(boolean z) {
            return z ? ((DecimalFormat) this.nf).getPositivePrefix() : ((DecimalFormat) this.nf).getNegativePrefix();
        }

        public String getSuffix(boolean z) {
            return z ? ((DecimalFormat) this.nf).getPositiveSuffix() : ((DecimalFormat) this.nf).getNegativeSuffix();
        }
    }

    static final class PatternData {
        final String prefix;
        final String suffix;

        public PatternData(String str) {
            int iIndexOf = str.indexOf("{0}");
            if (iIndexOf < 0) {
                this.prefix = str;
                this.suffix = null;
            } else {
                this.prefix = str.substring(0, iIndexOf);
                this.suffix = str.substring(iIndexOf + 3);
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

    private StringBuilder formatMeasuresSlowTrack(ListFormatter listFormatter, StringBuilder sb, FieldPosition fieldPosition, Measure... measureArr) {
        String[] strArr = new String[measureArr.length];
        FieldPosition fieldPosition2 = new FieldPosition(fieldPosition.getFieldAttribute(), fieldPosition.getField());
        int i = 0;
        int i2 = -1;
        while (i < measureArr.length) {
            ImmutableNumberFormat immutableNumberFormat = i == measureArr.length + (-1) ? this.numberFormat : this.integerFormat;
            if (i2 == -1) {
                strArr[i] = formatMeasure(measureArr[i], immutableNumberFormat, new StringBuilder(), fieldPosition2).toString();
                if (fieldPosition2.getBeginIndex() != 0 || fieldPosition2.getEndIndex() != 0) {
                    i2 = i;
                }
            } else {
                strArr[i] = formatMeasure(measureArr[i], immutableNumberFormat);
            }
            i++;
        }
        ListFormatter.FormattedListBuilder formattedListBuilder = listFormatter.format(Arrays.asList(strArr), i2);
        if (formattedListBuilder.getOffset() != -1) {
            fieldPosition.setBeginIndex(fieldPosition2.getBeginIndex() + formattedListBuilder.getOffset() + sb.length());
            fieldPosition.setEndIndex(fieldPosition2.getEndIndex() + formattedListBuilder.getOffset() + sb.length());
        }
        sb.append(formattedListBuilder.toString());
        return sb;
    }

    private static DateFormat loadNumericDurationFormat(ICUResourceBundle iCUResourceBundle, String str) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(iCUResourceBundle.getWithFallback(String.format("durationUnits/%s", str)).getString().replace("h", DateFormat.HOUR24));
        simpleDateFormat.setTimeZone(TimeZone.GMT_ZONE);
        return simpleDateFormat;
    }

    private static Number[] toHMS(Measure[] measureArr) {
        Integer num;
        int iIntValue;
        Number[] numberArr = new Number[3];
        int length = measureArr.length;
        int i = -1;
        int i2 = 0;
        while (i2 < length) {
            Measure measure = measureArr[i2];
            if (measure.getNumber().doubleValue() < 0.0d || (num = hmsTo012.get(measure.getUnit())) == null || (iIntValue = num.intValue()) <= i) {
                return null;
            }
            numberArr[iIntValue] = measure.getNumber();
            i2++;
            i = iIntValue;
        }
        return numberArr;
    }

    private StringBuilder formatNumeric(Number[] numberArr, StringBuilder sb) {
        int i = -1;
        int i2 = -1;
        for (int i3 = 0; i3 < numberArr.length; i3++) {
            if (numberArr[i3] != null) {
                if (i == -1) {
                    i = i3;
                    i2 = i;
                } else {
                    i2 = i3;
                }
            } else {
                numberArr[i3] = 0;
            }
        }
        Date date = new Date((long) (((((Math.floor(numberArr[0].doubleValue()) * 60.0d) + Math.floor(numberArr[1].doubleValue())) * 60.0d) + Math.floor(numberArr[2].doubleValue())) * 1000.0d));
        if (i == 0 && i2 == 2) {
            return formatNumeric(date, this.numericFormatters.getHourMinuteSecond(), DateFormat.Field.SECOND, numberArr[i2], sb);
        }
        if (i == 1 && i2 == 2) {
            return formatNumeric(date, this.numericFormatters.getMinuteSecond(), DateFormat.Field.SECOND, numberArr[i2], sb);
        }
        if (i == 0 && i2 == 1) {
            return formatNumeric(date, this.numericFormatters.getHourMinute(), DateFormat.Field.MINUTE, numberArr[i2], sb);
        }
        throw new IllegalStateException();
    }

    private StringBuilder formatNumeric(Date date, DateFormat dateFormat, DateFormat.Field field, Number number, StringBuilder sb) {
        FieldPosition fieldPosition = new FieldPosition(0);
        String string = this.numberFormat.format(number, new StringBuffer(), fieldPosition).toString();
        if (fieldPosition.getBeginIndex() == 0 && fieldPosition.getEndIndex() == 0) {
            throw new IllegalStateException();
        }
        FieldPosition fieldPosition2 = new FieldPosition(field);
        String string2 = dateFormat.format(date, new StringBuffer(), fieldPosition2).toString();
        if (fieldPosition2.getBeginIndex() != 0 || fieldPosition2.getEndIndex() != 0) {
            sb.append((CharSequence) string2, 0, fieldPosition2.getBeginIndex());
            sb.append((CharSequence) string, 0, fieldPosition.getBeginIndex());
            sb.append((CharSequence) string2, fieldPosition2.getBeginIndex(), fieldPosition2.getEndIndex());
            sb.append((CharSequence) string, fieldPosition.getEndIndex(), string.length());
            sb.append((CharSequence) string2, fieldPosition2.getEndIndex(), string2.length());
        } else {
            sb.append(string2);
        }
        return sb;
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

        public MeasureProxy(ULocale uLocale, FormatWidth formatWidth, NumberFormat numberFormat, int i) {
            this.locale = uLocale;
            this.formatWidth = formatWidth;
            this.numberFormat = numberFormat;
            this.subClass = i;
            this.keyValues = new HashMap<>();
        }

        public MeasureProxy() {
        }

        @Override
        public void writeExternal(ObjectOutput objectOutput) throws IOException {
            objectOutput.writeByte(0);
            objectOutput.writeUTF(this.locale.toLanguageTag());
            objectOutput.writeByte(this.formatWidth.ordinal());
            objectOutput.writeObject(this.numberFormat);
            objectOutput.writeByte(this.subClass);
            objectOutput.writeObject(this.keyValues);
        }

        @Override
        public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
            objectInput.readByte();
            this.locale = ULocale.forLanguageTag(objectInput.readUTF());
            this.formatWidth = MeasureFormat.fromFormatWidthOrdinal(objectInput.readByte() & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
            this.numberFormat = (NumberFormat) objectInput.readObject();
            if (this.numberFormat == null) {
                throw new InvalidObjectException("Missing number format.");
            }
            this.subClass = objectInput.readByte() & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            this.keyValues = (HashMap) objectInput.readObject();
            if (this.keyValues == null) {
                throw new InvalidObjectException("Missing optional values map.");
            }
        }

        private TimeUnitFormat createTimeUnitFormat() throws InvalidObjectException {
            int i;
            if (this.formatWidth == FormatWidth.WIDE) {
                i = 0;
            } else if (this.formatWidth == FormatWidth.SHORT) {
                i = 1;
            } else {
                throw new InvalidObjectException("Bad width: " + this.formatWidth);
            }
            TimeUnitFormat timeUnitFormat = new TimeUnitFormat(this.locale, i);
            timeUnitFormat.setNumberFormat(this.numberFormat);
            return timeUnitFormat;
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

    private static FormatWidth fromFormatWidthOrdinal(int i) {
        FormatWidth[] formatWidthArrValues = FormatWidth.values();
        if (i < 0 || i >= formatWidthArrValues.length) {
            return FormatWidth.SHORT;
        }
        return formatWidthArrValues[i];
    }

    @Deprecated
    public static String getRangeFormat(ULocale uLocale, FormatWidth formatWidth) {
        String stringWithFallback;
        String str;
        if (uLocale.getLanguage().equals("fr")) {
            return getRangeFormat(ULocale.ROOT, formatWidth);
        }
        String strCompileToStringMinMaxArguments = localeIdToRangeFormat.get(uLocale);
        if (strCompileToStringMinMaxArguments == null) {
            ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, uLocale);
            ULocale uLocale2 = iCUResourceBundle.getULocale();
            if (!uLocale.equals(uLocale2) && (str = localeIdToRangeFormat.get(uLocale)) != null) {
                localeIdToRangeFormat.put(uLocale, str);
                return str;
            }
            try {
                stringWithFallback = iCUResourceBundle.getStringWithFallback("NumberElements/" + NumberingSystem.getInstance(uLocale).getName() + "/miscPatterns/range");
            } catch (MissingResourceException e) {
                stringWithFallback = iCUResourceBundle.getStringWithFallback("NumberElements/latn/patterns/range");
            }
            strCompileToStringMinMaxArguments = SimpleFormatterImpl.compileToStringMinMaxArguments(stringWithFallback, new StringBuilder(), 2, 2);
            localeIdToRangeFormat.put(uLocale, strCompileToStringMinMaxArguments);
            if (!uLocale.equals(uLocale2)) {
                localeIdToRangeFormat.put(uLocale2, strCompileToStringMinMaxArguments);
            }
        }
        return strCompileToStringMinMaxArguments;
    }
}
