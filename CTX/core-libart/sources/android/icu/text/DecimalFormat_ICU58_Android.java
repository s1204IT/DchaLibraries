package android.icu.text;

import android.icu.impl.ICUConfig;
import android.icu.impl.PatternProps;
import android.icu.impl.Utility;
import android.icu.impl.locale.LanguageTag;
import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;
import android.icu.math.MathContext;
import android.icu.text.NumberFormat;
import android.icu.text.PluralRules;
import android.icu.util.Currency;
import android.icu.util.CurrencyAmount;
import android.icu.util.ULocale;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.ChoiceFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Deprecated
public class DecimalFormat_ICU58_Android extends NumberFormat {
    private static final char CURRENCY_SIGN = 164;
    private static final int CURRENCY_SIGN_COUNT_IN_ISO_FORMAT = 2;
    private static final int CURRENCY_SIGN_COUNT_IN_PLURAL_FORMAT = 3;
    private static final int CURRENCY_SIGN_COUNT_IN_SYMBOL_FORMAT = 1;
    private static final int CURRENCY_SIGN_COUNT_ZERO = 0;
    static final int DOUBLE_FRACTION_DIGITS = 340;
    static final int DOUBLE_INTEGER_DIGITS = 309;
    static final int MAX_INTEGER_DIGITS = 2000000000;
    static final int MAX_SCIENTIFIC_INTEGER_DIGITS = 8;
    public static final int PAD_AFTER_PREFIX = 1;
    public static final int PAD_AFTER_SUFFIX = 3;
    public static final int PAD_BEFORE_PREFIX = 0;
    public static final int PAD_BEFORE_SUFFIX = 2;
    static final char PATTERN_DECIMAL_SEPARATOR = '.';
    static final char PATTERN_DIGIT = '#';
    static final char PATTERN_EIGHT_DIGIT = '8';
    static final char PATTERN_EXPONENT = 'E';
    static final char PATTERN_FIVE_DIGIT = '5';
    static final char PATTERN_FOUR_DIGIT = '4';
    static final char PATTERN_GROUPING_SEPARATOR = ',';
    static final char PATTERN_MINUS_SIGN = '-';
    static final char PATTERN_NINE_DIGIT = '9';
    static final char PATTERN_ONE_DIGIT = '1';
    static final char PATTERN_PAD_ESCAPE = '*';
    private static final char PATTERN_PERCENT = '%';
    private static final char PATTERN_PER_MILLE = 8240;
    static final char PATTERN_PLUS_SIGN = '+';
    private static final char PATTERN_SEPARATOR = ';';
    static final char PATTERN_SEVEN_DIGIT = '7';
    static final char PATTERN_SIGNIFICANT_DIGIT = '@';
    static final char PATTERN_SIX_DIGIT = '6';
    static final char PATTERN_THREE_DIGIT = '3';
    static final char PATTERN_TWO_DIGIT = '2';
    static final char PATTERN_ZERO_DIGIT = '0';
    private static final char QUOTE = '\'';
    private static final int STATUS_INFINITE = 0;
    private static final int STATUS_LENGTH = 3;
    private static final int STATUS_POSITIVE = 1;
    private static final int STATUS_UNDERFLOW = 2;
    static final int currentSerialVersion = 4;
    static final double roundingIncrementEpsilon = 1.0E-9d;
    private static final long serialVersionUID = 864413376551465018L;
    private int PARSE_MAX_EXPONENT;
    private transient BigDecimal actualRoundingIncrement;
    private transient android.icu.math.BigDecimal actualRoundingIncrementICU;
    private transient Set<AffixForCurrency> affixPatternsForCurrency;
    private ArrayList<FieldPosition> attributes;
    private ChoiceFormat currencyChoice;
    private CurrencyPluralInfo currencyPluralInfo;
    private int currencySignCount;
    private Currency.CurrencyUsage currencyUsage;
    private boolean decimalSeparatorAlwaysShown;
    private transient DigitList_Android digitList;
    private boolean exponentSignAlwaysShown;
    private String formatPattern;
    private int formatWidth;
    private byte groupingSize;
    private byte groupingSize2;
    private transient boolean isReadyForParsing;
    private MathContext mathContext;
    private int maxSignificantDigits;
    private byte minExponentDigits;
    private int minSignificantDigits;
    private int multiplier;
    private String negPrefixPattern;
    private String negSuffixPattern;
    private String negativePrefix;
    private String negativeSuffix;
    private char pad;
    private int padPosition;
    private boolean parseBigDecimal;
    boolean parseRequireDecimalPoint;
    private String posPrefixPattern;
    private String posSuffixPattern;
    private String positivePrefix;
    private String positiveSuffix;
    private transient double roundingDouble;
    private transient double roundingDoubleReciprocal;
    private BigDecimal roundingIncrement;
    private transient android.icu.math.BigDecimal roundingIncrementICU;
    private int roundingMode;
    private int serialVersionOnStream;
    private int style;
    private DecimalFormatSymbols symbols;
    private boolean useExponentialNotation;
    private boolean useSignificantDigits;
    private static double epsilon = 1.0E-11d;
    private static final UnicodeSet dotEquivalents = new UnicodeSet(46, 46, 8228, 8228, 12290, 12290, 65042, 65042, 65106, 65106, 65294, 65294, 65377, 65377).freeze();
    private static final UnicodeSet commaEquivalents = new UnicodeSet(44, 44, 1548, 1548, 1643, 1643, UProperty.DOUBLE_LIMIT, UProperty.DOUBLE_LIMIT, 65040, 65041, 65104, 65105, 65292, 65292, 65380, 65380).freeze();
    private static final UnicodeSet strictDotEquivalents = new UnicodeSet(46, 46, 8228, 8228, 65106, 65106, 65294, 65294, 65377, 65377).freeze();
    private static final UnicodeSet strictCommaEquivalents = new UnicodeSet(44, 44, 1643, 1643, 65040, 65040, 65104, 65104, 65292, 65292).freeze();
    private static final UnicodeSet defaultGroupingSeparators = new UnicodeSet(32, 32, 39, 39, 44, 44, 46, 46, 160, 160, 1548, 1548, 1643, 1644, 8192, 8202, 8216, 8217, 8228, 8228, 8239, 8239, 8287, 8287, 12288, 12290, 65040, 65042, 65104, 65106, 65287, 65287, 65292, 65292, 65294, 65294, 65377, 65377, 65380, 65380).freeze();
    private static final UnicodeSet strictDefaultGroupingSeparators = new UnicodeSet(32, 32, 39, 39, 44, 44, 46, 46, 160, 160, 1643, 1644, 8192, 8202, 8216, 8217, 8228, 8228, 8239, 8239, 8287, 8287, 12288, 12288, 65040, 65040, 65104, 65104, 65106, 65106, 65287, 65287, 65292, 65292, 65294, 65294, 65377, 65377).freeze();
    static final UnicodeSet minusSigns = new UnicodeSet(45, 45, 8315, 8315, 8331, 8331, 8722, 8722, 10134, 10134, 65123, 65123, 65293, 65293).freeze();
    static final UnicodeSet plusSigns = new UnicodeSet(43, 43, 8314, 8314, 8330, 8330, 10133, 10133, 64297, 64297, 65122, 65122, 65291, 65291).freeze();
    static final boolean skipExtendedSeparatorParsing = ICUConfig.get("android.icu.text.DecimalFormat.SkipExtendedSeparatorParsing", "false").equals("true");
    static final Unit NULL_UNIT = new Unit("", "");

    public DecimalFormat_ICU58_Android() {
        this.parseRequireDecimalPoint = false;
        this.PARSE_MAX_EXPONENT = 1000;
        this.digitList = new DigitList_Android();
        this.positivePrefix = "";
        this.positiveSuffix = "";
        this.negativePrefix = LanguageTag.SEP;
        this.negativeSuffix = "";
        this.multiplier = 1;
        this.groupingSize = (byte) 3;
        this.groupingSize2 = (byte) 0;
        this.decimalSeparatorAlwaysShown = false;
        this.symbols = null;
        this.useSignificantDigits = false;
        this.minSignificantDigits = 1;
        this.maxSignificantDigits = 6;
        this.exponentSignAlwaysShown = false;
        this.roundingIncrement = null;
        this.roundingIncrementICU = null;
        this.roundingMode = 6;
        this.mathContext = new MathContext(0, 0);
        this.formatWidth = 0;
        this.pad = ' ';
        this.padPosition = 0;
        this.parseBigDecimal = false;
        this.currencyUsage = Currency.CurrencyUsage.STANDARD;
        this.serialVersionOnStream = 4;
        this.attributes = new ArrayList<>();
        this.formatPattern = "";
        this.style = 0;
        this.currencySignCount = 0;
        this.affixPatternsForCurrency = null;
        this.isReadyForParsing = false;
        this.currencyPluralInfo = null;
        this.actualRoundingIncrementICU = null;
        this.actualRoundingIncrement = null;
        this.roundingDouble = 0.0d;
        this.roundingDoubleReciprocal = 0.0d;
        ULocale uLocale = ULocale.getDefault(ULocale.Category.FORMAT);
        String pattern = getPattern(uLocale, 0);
        this.symbols = new DecimalFormatSymbols(uLocale);
        setCurrency(Currency.getInstance(uLocale));
        applyPatternWithoutExpandAffix(pattern, false);
        if (this.currencySignCount == 3) {
            this.currencyPluralInfo = new CurrencyPluralInfo(uLocale);
        } else {
            expandAffixAdjustWidth(null);
        }
    }

    public DecimalFormat_ICU58_Android(String str) {
        this.parseRequireDecimalPoint = false;
        this.PARSE_MAX_EXPONENT = 1000;
        this.digitList = new DigitList_Android();
        this.positivePrefix = "";
        this.positiveSuffix = "";
        this.negativePrefix = LanguageTag.SEP;
        this.negativeSuffix = "";
        this.multiplier = 1;
        this.groupingSize = (byte) 3;
        this.groupingSize2 = (byte) 0;
        this.decimalSeparatorAlwaysShown = false;
        this.symbols = null;
        this.useSignificantDigits = false;
        this.minSignificantDigits = 1;
        this.maxSignificantDigits = 6;
        this.exponentSignAlwaysShown = false;
        this.roundingIncrement = null;
        this.roundingIncrementICU = null;
        this.roundingMode = 6;
        this.mathContext = new MathContext(0, 0);
        this.formatWidth = 0;
        this.pad = ' ';
        this.padPosition = 0;
        this.parseBigDecimal = false;
        this.currencyUsage = Currency.CurrencyUsage.STANDARD;
        this.serialVersionOnStream = 4;
        this.attributes = new ArrayList<>();
        this.formatPattern = "";
        this.style = 0;
        this.currencySignCount = 0;
        this.affixPatternsForCurrency = null;
        this.isReadyForParsing = false;
        this.currencyPluralInfo = null;
        this.actualRoundingIncrementICU = null;
        this.actualRoundingIncrement = null;
        this.roundingDouble = 0.0d;
        this.roundingDoubleReciprocal = 0.0d;
        ULocale uLocale = ULocale.getDefault(ULocale.Category.FORMAT);
        this.symbols = new DecimalFormatSymbols(uLocale);
        setCurrency(Currency.getInstance(uLocale));
        applyPatternWithoutExpandAffix(str, false);
        if (this.currencySignCount == 3) {
            this.currencyPluralInfo = new CurrencyPluralInfo(uLocale);
        } else {
            expandAffixAdjustWidth(null);
        }
    }

    public DecimalFormat_ICU58_Android(String str, DecimalFormatSymbols decimalFormatSymbols) {
        this.parseRequireDecimalPoint = false;
        this.PARSE_MAX_EXPONENT = 1000;
        this.digitList = new DigitList_Android();
        this.positivePrefix = "";
        this.positiveSuffix = "";
        this.negativePrefix = LanguageTag.SEP;
        this.negativeSuffix = "";
        this.multiplier = 1;
        this.groupingSize = (byte) 3;
        this.groupingSize2 = (byte) 0;
        this.decimalSeparatorAlwaysShown = false;
        this.symbols = null;
        this.useSignificantDigits = false;
        this.minSignificantDigits = 1;
        this.maxSignificantDigits = 6;
        this.exponentSignAlwaysShown = false;
        this.roundingIncrement = null;
        this.roundingIncrementICU = null;
        this.roundingMode = 6;
        this.mathContext = new MathContext(0, 0);
        this.formatWidth = 0;
        this.pad = ' ';
        this.padPosition = 0;
        this.parseBigDecimal = false;
        this.currencyUsage = Currency.CurrencyUsage.STANDARD;
        this.serialVersionOnStream = 4;
        this.attributes = new ArrayList<>();
        this.formatPattern = "";
        this.style = 0;
        this.currencySignCount = 0;
        this.affixPatternsForCurrency = null;
        this.isReadyForParsing = false;
        this.currencyPluralInfo = null;
        this.actualRoundingIncrementICU = null;
        this.actualRoundingIncrement = null;
        this.roundingDouble = 0.0d;
        this.roundingDoubleReciprocal = 0.0d;
        createFromPatternAndSymbols(str, decimalFormatSymbols);
    }

    private void createFromPatternAndSymbols(String str, DecimalFormatSymbols decimalFormatSymbols) {
        this.symbols = (DecimalFormatSymbols) decimalFormatSymbols.clone();
        if (str.indexOf(164) >= 0) {
            setCurrencyForSymbols();
        }
        applyPatternWithoutExpandAffix(str, false);
        if (this.currencySignCount == 3) {
            this.currencyPluralInfo = new CurrencyPluralInfo(this.symbols.getULocale());
        } else {
            expandAffixAdjustWidth(null);
        }
    }

    public DecimalFormat_ICU58_Android(String str, DecimalFormatSymbols decimalFormatSymbols, CurrencyPluralInfo currencyPluralInfo, int i) {
        this.parseRequireDecimalPoint = false;
        this.PARSE_MAX_EXPONENT = 1000;
        this.digitList = new DigitList_Android();
        this.positivePrefix = "";
        this.positiveSuffix = "";
        this.negativePrefix = LanguageTag.SEP;
        this.negativeSuffix = "";
        this.multiplier = 1;
        this.groupingSize = (byte) 3;
        this.groupingSize2 = (byte) 0;
        this.decimalSeparatorAlwaysShown = false;
        this.symbols = null;
        this.useSignificantDigits = false;
        this.minSignificantDigits = 1;
        this.maxSignificantDigits = 6;
        this.exponentSignAlwaysShown = false;
        this.roundingIncrement = null;
        this.roundingIncrementICU = null;
        this.roundingMode = 6;
        this.mathContext = new MathContext(0, 0);
        this.formatWidth = 0;
        this.pad = ' ';
        this.padPosition = 0;
        this.parseBigDecimal = false;
        this.currencyUsage = Currency.CurrencyUsage.STANDARD;
        this.serialVersionOnStream = 4;
        this.attributes = new ArrayList<>();
        this.formatPattern = "";
        this.style = 0;
        this.currencySignCount = 0;
        this.affixPatternsForCurrency = null;
        this.isReadyForParsing = false;
        this.currencyPluralInfo = null;
        this.actualRoundingIncrementICU = null;
        this.actualRoundingIncrement = null;
        this.roundingDouble = 0.0d;
        this.roundingDoubleReciprocal = 0.0d;
        create(str, decimalFormatSymbols, i == 6 ? (CurrencyPluralInfo) currencyPluralInfo.clone() : currencyPluralInfo, i);
    }

    private void create(String str, DecimalFormatSymbols decimalFormatSymbols, CurrencyPluralInfo currencyPluralInfo, int i) {
        if (i != 6) {
            createFromPatternAndSymbols(str, decimalFormatSymbols);
        } else {
            this.symbols = (DecimalFormatSymbols) decimalFormatSymbols.clone();
            this.currencyPluralInfo = currencyPluralInfo;
            applyPatternWithoutExpandAffix(this.currencyPluralInfo.getCurrencyPluralPattern(PluralRules.KEYWORD_OTHER), false);
            setCurrencyForSymbols();
        }
        this.style = i;
    }

    @Deprecated
    public DecimalFormat_ICU58_Android(String str, DecimalFormatSymbols decimalFormatSymbols, int i) {
        this.parseRequireDecimalPoint = false;
        this.PARSE_MAX_EXPONENT = 1000;
        this.digitList = new DigitList_Android();
        this.positivePrefix = "";
        this.positiveSuffix = "";
        this.negativePrefix = LanguageTag.SEP;
        this.negativeSuffix = "";
        this.multiplier = 1;
        this.groupingSize = (byte) 3;
        this.groupingSize2 = (byte) 0;
        this.decimalSeparatorAlwaysShown = false;
        this.symbols = null;
        this.useSignificantDigits = false;
        this.minSignificantDigits = 1;
        this.maxSignificantDigits = 6;
        this.exponentSignAlwaysShown = false;
        this.roundingIncrement = null;
        this.roundingIncrementICU = null;
        this.roundingMode = 6;
        this.mathContext = new MathContext(0, 0);
        this.formatWidth = 0;
        this.pad = ' ';
        this.padPosition = 0;
        this.parseBigDecimal = false;
        this.currencyUsage = Currency.CurrencyUsage.STANDARD;
        this.serialVersionOnStream = 4;
        this.attributes = new ArrayList<>();
        this.formatPattern = "";
        this.style = 0;
        this.currencySignCount = 0;
        this.affixPatternsForCurrency = null;
        this.isReadyForParsing = false;
        this.currencyPluralInfo = null;
        this.actualRoundingIncrementICU = null;
        this.actualRoundingIncrement = null;
        this.roundingDouble = 0.0d;
        this.roundingDoubleReciprocal = 0.0d;
        create(str, decimalFormatSymbols, i == 6 ? new CurrencyPluralInfo(decimalFormatSymbols.getULocale()) : null, i);
    }

    @Override
    public StringBuffer format(double d, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        return format(d, stringBuffer, fieldPosition, false);
    }

    private boolean isNegative(double d) {
        return d < 0.0d || (d == 0.0d && 1.0d / d < 0.0d);
    }

    private double round(double d) {
        boolean zIsNegative = isNegative(d);
        if (zIsNegative) {
            d = -d;
        }
        double d2 = d;
        if (this.roundingDouble > 0.0d) {
            return round(d2, this.roundingDouble, this.roundingDoubleReciprocal, this.roundingMode, zIsNegative);
        }
        return d2;
    }

    private double multiply(double d) {
        if (this.multiplier != 1) {
            return d * ((double) this.multiplier);
        }
        return d;
    }

    private StringBuffer format(double d, StringBuffer stringBuffer, FieldPosition fieldPosition, boolean z) {
        StringBuffer stringBufferSubformat;
        double dDoubleValue;
        double dDoubleValue2;
        boolean z2 = false;
        fieldPosition.setBeginIndex(0);
        fieldPosition.setEndIndex(0);
        if (Double.isNaN(d)) {
            if (fieldPosition.getField() == 0 || fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
                fieldPosition.setBeginIndex(stringBuffer.length());
            }
            stringBuffer.append(this.symbols.getNaN());
            if (z) {
                addAttribute(NumberFormat.Field.INTEGER, stringBuffer.length() - this.symbols.getNaN().length(), stringBuffer.length());
            }
            if (fieldPosition.getField() == 0 || fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
                fieldPosition.setEndIndex(stringBuffer.length());
            }
            addPadding(stringBuffer, fieldPosition, 0, 0);
            return stringBuffer;
        }
        double dMultiply = multiply(d);
        boolean zIsNegative = isNegative(dMultiply);
        double dRound = round(dMultiply);
        if (Double.isInfinite(dRound)) {
            int iAppendAffix = appendAffix(stringBuffer, zIsNegative, true, fieldPosition, z);
            if (fieldPosition.getField() == 0 || fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
                fieldPosition.setBeginIndex(stringBuffer.length());
            }
            stringBuffer.append(this.symbols.getInfinity());
            if (z) {
                addAttribute(NumberFormat.Field.INTEGER, stringBuffer.length() - this.symbols.getInfinity().length(), stringBuffer.length());
            }
            if (fieldPosition.getField() == 0 || fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
                fieldPosition.setEndIndex(stringBuffer.length());
            }
            addPadding(stringBuffer, fieldPosition, iAppendAffix, appendAffix(stringBuffer, zIsNegative, false, fieldPosition, z));
            return stringBuffer;
        }
        int iPrecision = precision(false);
        if (this.useExponentialNotation && iPrecision > 0 && dRound != 0.0d && this.roundingMode != 6) {
            int iFloor = (1 - iPrecision) + ((int) Math.floor(Math.log10(Math.abs(dRound))));
            if (iFloor < 0) {
                dDoubleValue2 = android.icu.math.BigDecimal.ONE.movePointRight(-iFloor).doubleValue();
                dDoubleValue = 0.0d;
            } else {
                dDoubleValue = android.icu.math.BigDecimal.ONE.movePointRight(iFloor).doubleValue();
                dDoubleValue2 = 0.0d;
            }
            dRound = round(dRound, dDoubleValue, dDoubleValue2, this.roundingMode, zIsNegative);
        }
        synchronized (this.digitList) {
            DigitList_Android digitList_Android = this.digitList;
            if (!this.useExponentialNotation && !areSignificantDigitsUsed()) {
                z2 = true;
            }
            digitList_Android.set(dRound, iPrecision, z2);
            stringBufferSubformat = subformat(dRound, stringBuffer, fieldPosition, zIsNegative, false, z);
        }
        return stringBufferSubformat;
    }

    @Deprecated
    double adjustNumberAsInFormatting(double d) {
        if (Double.isNaN(d)) {
            return d;
        }
        double dRound = round(multiply(d));
        if (Double.isInfinite(dRound)) {
            return dRound;
        }
        return toDigitList(dRound).getDouble();
    }

    @Deprecated
    DigitList_Android toDigitList(double d) {
        DigitList_Android digitList_Android = new DigitList_Android();
        digitList_Android.set(d, precision(false), false);
        return digitList_Android;
    }

    @Deprecated
    boolean isNumberNegative(double d) {
        if (Double.isNaN(d)) {
            return false;
        }
        return isNegative(multiply(d));
    }

    private static double round(double d, double d2, double d3, int i, boolean z) {
        double dCeil;
        double d4 = d3 == 0.0d ? d / d2 : d * d3;
        if (i == 7) {
            if (d4 != Math.floor(d4)) {
                throw new ArithmeticException("Rounding necessary");
            }
            return d;
        }
        switch (i) {
            case 0:
                dCeil = Math.ceil(d4 - epsilon);
                break;
            case 1:
                dCeil = Math.floor(d4 + epsilon);
                break;
            case 2:
                dCeil = !z ? Math.ceil(d4 - epsilon) : Math.floor(d4 + epsilon);
                break;
            case 3:
                dCeil = !z ? Math.floor(d4 + epsilon) : Math.ceil(d4 - epsilon);
                break;
            default:
                dCeil = Math.ceil(d4);
                double d5 = dCeil - d4;
                double dFloor = Math.floor(d4);
                double d6 = d4 - dFloor;
                switch (i) {
                    case 4:
                        if (d5 > d6 + epsilon) {
                            dCeil = dFloor;
                        }
                        break;
                    case 5:
                        if (d6 <= d5 + epsilon) {
                            dCeil = dFloor;
                        }
                        break;
                    case 6:
                        if (epsilon + d6 < d5) {
                            dCeil = dFloor;
                        } else if (d5 + epsilon >= d6) {
                            double d7 = dFloor / 2.0d;
                            if (d7 == Math.floor(d7)) {
                                dCeil = dFloor;
                            }
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid rounding mode: " + i);
                }
                break;
        }
        return d3 == 0.0d ? dCeil * d2 : dCeil / d3;
    }

    @Override
    public StringBuffer format(long j, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        return format(j, stringBuffer, fieldPosition, false);
    }

    private StringBuffer format(long j, StringBuffer stringBuffer, FieldPosition fieldPosition, boolean z) {
        StringBuffer stringBufferSubformat;
        long j2 = j;
        boolean z2 = false;
        fieldPosition.setBeginIndex(0);
        fieldPosition.setEndIndex(0);
        if (this.actualRoundingIncrementICU != null) {
            return format(android.icu.math.BigDecimal.valueOf(j2), stringBuffer, fieldPosition);
        }
        boolean z3 = j2 < 0;
        if (z3) {
            j2 = -j2;
        }
        if (this.multiplier != 1) {
            if (j2 < 0) {
                if (j2 <= Long.MIN_VALUE / ((long) this.multiplier)) {
                    z2 = true;
                }
            } else if (j2 > Long.MAX_VALUE / ((long) this.multiplier)) {
                z2 = true;
            }
            if (z2) {
                if (z3) {
                    j2 = -j2;
                }
                return format(BigInteger.valueOf(j2), stringBuffer, fieldPosition, z);
            }
        }
        long j3 = j2 * ((long) this.multiplier);
        synchronized (this.digitList) {
            this.digitList.set(j3, precision(true));
            if (this.digitList.wasRounded() && this.roundingMode == 7) {
                throw new ArithmeticException("Rounding necessary");
            }
            stringBufferSubformat = subformat(j3, stringBuffer, fieldPosition, z3, true, z);
        }
        return stringBufferSubformat;
    }

    @Override
    public StringBuffer format(BigInteger bigInteger, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        return format(bigInteger, stringBuffer, fieldPosition, false);
    }

    private StringBuffer format(BigInteger bigInteger, StringBuffer stringBuffer, FieldPosition fieldPosition, boolean z) {
        StringBuffer stringBufferSubformat;
        if (this.actualRoundingIncrementICU != null) {
            return format(new android.icu.math.BigDecimal(bigInteger), stringBuffer, fieldPosition);
        }
        if (this.multiplier != 1) {
            bigInteger = bigInteger.multiply(BigInteger.valueOf(this.multiplier));
        }
        synchronized (this.digitList) {
            this.digitList.set(bigInteger, precision(true));
            if (this.digitList.wasRounded() && this.roundingMode == 7) {
                throw new ArithmeticException("Rounding necessary");
            }
            stringBufferSubformat = subformat(bigInteger.intValue(), stringBuffer, fieldPosition, bigInteger.signum() < 0, true, z);
        }
        return stringBufferSubformat;
    }

    @Override
    public StringBuffer format(BigDecimal bigDecimal, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        return format(bigDecimal, stringBuffer, fieldPosition, false);
    }

    private StringBuffer format(BigDecimal bigDecimal, StringBuffer stringBuffer, FieldPosition fieldPosition, boolean z) {
        StringBuffer stringBufferSubformat;
        if (this.multiplier != 1) {
            bigDecimal = bigDecimal.multiply(BigDecimal.valueOf(this.multiplier));
        }
        if (this.actualRoundingIncrement != null) {
            bigDecimal = bigDecimal.divide(this.actualRoundingIncrement, 0, this.roundingMode).multiply(this.actualRoundingIncrement);
        }
        synchronized (this.digitList) {
            this.digitList.set(bigDecimal, precision(false), (this.useExponentialNotation || areSignificantDigitsUsed()) ? false : true);
            if (this.digitList.wasRounded() && this.roundingMode == 7) {
                throw new ArithmeticException("Rounding necessary");
            }
            stringBufferSubformat = subformat(bigDecimal.doubleValue(), stringBuffer, fieldPosition, bigDecimal.signum() < 0, false, z);
        }
        return stringBufferSubformat;
    }

    @Override
    public StringBuffer format(android.icu.math.BigDecimal bigDecimal, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        StringBuffer stringBufferSubformat;
        if (this.multiplier != 1) {
            bigDecimal = bigDecimal.multiply(android.icu.math.BigDecimal.valueOf(this.multiplier), this.mathContext);
        }
        if (this.actualRoundingIncrementICU != null) {
            bigDecimal = bigDecimal.divide(this.actualRoundingIncrementICU, 0, this.roundingMode).multiply(this.actualRoundingIncrementICU, this.mathContext);
        }
        synchronized (this.digitList) {
            this.digitList.set(bigDecimal, precision(false), (this.useExponentialNotation || areSignificantDigitsUsed()) ? false : true);
            if (this.digitList.wasRounded() && this.roundingMode == 7) {
                throw new ArithmeticException("Rounding necessary");
            }
            stringBufferSubformat = subformat(bigDecimal.doubleValue(), stringBuffer, fieldPosition, bigDecimal.signum() < 0, false, false);
        }
        return stringBufferSubformat;
    }

    private boolean isGroupingPosition(int i) {
        if (!isGroupingUsed() || i <= 0 || this.groupingSize <= 0) {
            return false;
        }
        if (this.groupingSize2 > 0 && i > this.groupingSize) {
            if ((i - this.groupingSize) % this.groupingSize2 != 0) {
                return false;
            }
        } else if (i % this.groupingSize != 0) {
            return false;
        }
        return true;
    }

    private int precision(boolean z) {
        if (areSignificantDigitsUsed()) {
            return getMaximumSignificantDigits();
        }
        if (this.useExponentialNotation) {
            return getMinimumIntegerDigits() + getMaximumFractionDigits();
        }
        if (z) {
            return 0;
        }
        return getMaximumFractionDigits();
    }

    private StringBuffer subformat(int i, StringBuffer stringBuffer, FieldPosition fieldPosition, boolean z, boolean z2, boolean z3) {
        if (this.currencySignCount == 3) {
            return subformat(this.currencyPluralInfo.select(getFixedDecimal(i)), stringBuffer, fieldPosition, z, z2, z3);
        }
        return subformat(stringBuffer, fieldPosition, z, z2, z3);
    }

    PluralRules.FixedDecimal getFixedDecimal(double d) {
        return getFixedDecimal(d, this.digitList);
    }

    PluralRules.FixedDecimal getFixedDecimal(double d, DigitList_Android digitList_Android) {
        int maximumFractionDigits;
        int minimumFractionDigits;
        int i = digitList_Android.count - digitList_Android.decimalAt;
        if (this.useSignificantDigits) {
            maximumFractionDigits = this.maxSignificantDigits - digitList_Android.decimalAt;
            minimumFractionDigits = this.minSignificantDigits - digitList_Android.decimalAt;
            if (minimumFractionDigits < 0) {
                minimumFractionDigits = 0;
            }
            if (maximumFractionDigits < 0) {
                maximumFractionDigits = 0;
            }
        } else {
            maximumFractionDigits = getMaximumFractionDigits();
            minimumFractionDigits = getMinimumFractionDigits();
        }
        int i2 = i < minimumFractionDigits ? minimumFractionDigits : i > maximumFractionDigits ? maximumFractionDigits : i;
        long j = 0;
        if (i2 > 0) {
            for (int iMax = Math.max(0, digitList_Android.decimalAt); iMax < digitList_Android.count; iMax++) {
                j = (j * 10) + ((long) (digitList_Android.digits[iMax] - 48));
            }
            for (int i3 = i2; i3 < i; i3++) {
                j *= 10;
            }
        }
        return new PluralRules.FixedDecimal(d, i2, j);
    }

    private StringBuffer subformat(double d, StringBuffer stringBuffer, FieldPosition fieldPosition, boolean z, boolean z2, boolean z3) {
        if (this.currencySignCount == 3) {
            return subformat(this.currencyPluralInfo.select(getFixedDecimal(d)), stringBuffer, fieldPosition, z, z2, z3);
        }
        return subformat(stringBuffer, fieldPosition, z, z2, z3);
    }

    private StringBuffer subformat(String str, StringBuffer stringBuffer, FieldPosition fieldPosition, boolean z, boolean z2, boolean z3) {
        if (this.style == 6) {
            String currencyPluralPattern = this.currencyPluralInfo.getCurrencyPluralPattern(str);
            if (!this.formatPattern.equals(currencyPluralPattern)) {
                applyPatternWithoutExpandAffix(currencyPluralPattern, false);
            }
        }
        expandAffixAdjustWidth(str);
        return subformat(stringBuffer, fieldPosition, z, z2, z3);
    }

    private StringBuffer subformat(StringBuffer stringBuffer, FieldPosition fieldPosition, boolean z, boolean z2, boolean z3) {
        if (this.digitList.isZero()) {
            this.digitList.decimalAt = 0;
        }
        int iAppendAffix = appendAffix(stringBuffer, z, true, fieldPosition, z3);
        if (this.useExponentialNotation) {
            subformatExponential(stringBuffer, fieldPosition, z3);
        } else {
            subformatFixed(stringBuffer, fieldPosition, z2, z3);
        }
        addPadding(stringBuffer, fieldPosition, iAppendAffix, appendAffix(stringBuffer, z, false, fieldPosition, z3));
        return stringBuffer;
    }

    private void subformatFixed(StringBuffer stringBuffer, FieldPosition fieldPosition, boolean z, boolean z2) {
        int i;
        int i2;
        int i3;
        int i4;
        int i5;
        String str;
        int i6;
        String[] digitStrings = this.symbols.getDigitStrings();
        String groupingSeparatorString = this.currencySignCount == 0 ? this.symbols.getGroupingSeparatorString() : this.symbols.getMonetaryGroupingSeparatorString();
        String decimalSeparatorString = this.currencySignCount == 0 ? this.symbols.getDecimalSeparatorString() : this.symbols.getMonetaryDecimalSeparatorString();
        boolean zAreSignificantDigitsUsed = areSignificantDigitsUsed();
        int maximumIntegerDigits = getMaximumIntegerDigits();
        int minimumIntegerDigits = getMinimumIntegerDigits();
        int length = stringBuffer.length();
        if (fieldPosition.getField() == 0 || fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
            fieldPosition.setBeginIndex(length);
        }
        int minimumSignificantDigits = getMinimumSignificantDigits();
        int maximumSignificantDigits = getMaximumSignificantDigits();
        if (!zAreSignificantDigitsUsed) {
            maximumSignificantDigits = Integer.MAX_VALUE;
            minimumSignificantDigits = 0;
        }
        if (zAreSignificantDigitsUsed) {
            minimumIntegerDigits = Math.max(1, this.digitList.decimalAt);
        }
        if (this.digitList.decimalAt > 0 && minimumIntegerDigits < this.digitList.decimalAt) {
            minimumIntegerDigits = this.digitList.decimalAt;
        }
        if (minimumIntegerDigits > maximumIntegerDigits && maximumIntegerDigits >= 0) {
            i = this.digitList.decimalAt - maximumIntegerDigits;
        } else {
            maximumIntegerDigits = minimumIntegerDigits;
            i = 0;
        }
        int length2 = stringBuffer.length();
        int i7 = maximumIntegerDigits - 1;
        int i8 = 0;
        while (i7 >= 0) {
            String str2 = decimalSeparatorString;
            if (i7 < this.digitList.decimalAt && i < this.digitList.count && i8 < maximumSignificantDigits) {
                stringBuffer.append(digitStrings[this.digitList.getDigitValue(i)]);
                i8++;
                i++;
            } else {
                stringBuffer.append(digitStrings[0]);
                if (i8 > 0) {
                    i8++;
                }
            }
            if (!isGroupingPosition(i7)) {
                str = groupingSeparatorString;
            } else {
                stringBuffer.append(groupingSeparatorString);
                str = groupingSeparatorString;
                if (fieldPosition.getFieldAttribute() == NumberFormat.Field.GROUPING_SEPARATOR && fieldPosition.getBeginIndex() == 0 && fieldPosition.getEndIndex() == 0) {
                    fieldPosition.setBeginIndex(stringBuffer.length() - 1);
                    fieldPosition.setEndIndex(stringBuffer.length());
                }
                if (z2) {
                    i6 = i;
                    addAttribute(NumberFormat.Field.GROUPING_SEPARATOR, stringBuffer.length() - 1, stringBuffer.length());
                }
                i7--;
                decimalSeparatorString = str2;
                groupingSeparatorString = str;
                i = i6;
            }
            i6 = i;
            i7--;
            decimalSeparatorString = str2;
            groupingSeparatorString = str;
            i = i6;
        }
        String str3 = decimalSeparatorString;
        if (fieldPosition.getField() == 0 || fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
            fieldPosition.setEndIndex(stringBuffer.length());
        }
        if (i8 == 0 && this.digitList.count == 0) {
            i8 = 1;
        }
        boolean z3 = (!z && i < this.digitList.count) || (!zAreSignificantDigitsUsed ? getMinimumFractionDigits() <= 0 : i8 >= minimumSignificantDigits);
        if (!z3 && stringBuffer.length() == length2) {
            stringBuffer.append(digitStrings[0]);
        }
        if (z2) {
            addAttribute(NumberFormat.Field.INTEGER, length, stringBuffer.length());
        }
        if (this.decimalSeparatorAlwaysShown || z3) {
            if (fieldPosition.getFieldAttribute() == NumberFormat.Field.DECIMAL_SEPARATOR) {
                fieldPosition.setBeginIndex(stringBuffer.length());
            }
            stringBuffer.append(str3);
            if (fieldPosition.getFieldAttribute() == NumberFormat.Field.DECIMAL_SEPARATOR) {
                fieldPosition.setEndIndex(stringBuffer.length());
            }
            if (z2) {
                i2 = 1;
                addAttribute(NumberFormat.Field.DECIMAL_SEPARATOR, stringBuffer.length() - 1, stringBuffer.length());
            } else {
                i2 = 1;
            }
        }
        if (fieldPosition.getField() == i2 || fieldPosition.getFieldAttribute() == NumberFormat.Field.FRACTION) {
            fieldPosition.setBeginIndex(stringBuffer.length());
        }
        int length3 = stringBuffer.length();
        boolean z4 = fieldPosition instanceof UFieldPosition;
        int maximumFractionDigits = (!zAreSignificantDigitsUsed || (i8 != maximumSignificantDigits && (i8 < minimumSignificantDigits || i != this.digitList.count))) ? zAreSignificantDigitsUsed ? Integer.MAX_VALUE : getMaximumFractionDigits() : 0;
        int i9 = i8;
        int i10 = 0;
        long j = 0;
        int i11 = i;
        int i12 = 0;
        while (i12 < maximumFractionDigits) {
            if (!zAreSignificantDigitsUsed) {
                i4 = maximumFractionDigits;
                if (i12 >= getMinimumFractionDigits() && (z || i11 >= this.digitList.count)) {
                    break;
                }
            } else {
                i4 = maximumFractionDigits;
            }
            i3 = length3;
            if ((-1) - i12 > this.digitList.decimalAt - 1) {
                stringBuffer.append(digitStrings[0]);
                if (z4) {
                    i10++;
                    j *= 10;
                }
            } else {
                if (!z && i11 < this.digitList.count) {
                    int i13 = i11 + 1;
                    byte digitValue = this.digitList.getDigitValue(i11);
                    stringBuffer.append(digitStrings[digitValue]);
                    if (z4) {
                        i10++;
                        i5 = i13;
                        j = (j * 10) + ((long) digitValue);
                    } else {
                        i5 = i13;
                    }
                    i11 = i5;
                } else {
                    stringBuffer.append(digitStrings[0]);
                    if (z4) {
                        i10++;
                        j *= 10;
                    }
                }
                int i14 = i9 + 1;
                if (zAreSignificantDigitsUsed && (i14 == maximumSignificantDigits || (i11 == this.digitList.count && i14 >= minimumSignificantDigits))) {
                    break;
                } else {
                    i9 = i14;
                }
            }
            i12++;
            maximumFractionDigits = i4;
            length3 = i3;
        }
        i3 = length3;
        long j2 = j;
        if (fieldPosition.getField() == 1 || fieldPosition.getFieldAttribute() == NumberFormat.Field.FRACTION) {
            fieldPosition.setEndIndex(stringBuffer.length());
        }
        if (z4) {
            ((UFieldPosition) fieldPosition).setFractionDigits(i10, j2);
        }
        if (z2) {
            if (this.decimalSeparatorAlwaysShown || z3) {
                addAttribute(NumberFormat.Field.FRACTION, i3, stringBuffer.length());
            }
        }
    }

    private void subformatExponential(StringBuffer stringBuffer, FieldPosition fieldPosition, boolean z) {
        int minimumFractionDigits;
        int i;
        int i2;
        int i3;
        int i4;
        String[] digitStringsLocal = this.symbols.getDigitStringsLocal();
        String decimalSeparatorString = this.currencySignCount == 0 ? this.symbols.getDecimalSeparatorString() : this.symbols.getMonetaryDecimalSeparatorString();
        boolean zAreSignificantDigitsUsed = areSignificantDigitsUsed();
        int maximumIntegerDigits = getMaximumIntegerDigits();
        int minimumIntegerDigits = getMinimumIntegerDigits();
        int i5 = 1;
        if (fieldPosition.getField() == 0) {
            fieldPosition.setBeginIndex(stringBuffer.length());
            fieldPosition.setEndIndex(-1);
        } else if (fieldPosition.getField() == 1) {
            fieldPosition.setBeginIndex(-1);
        } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
            fieldPosition.setBeginIndex(stringBuffer.length());
            fieldPosition.setEndIndex(-1);
        } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.FRACTION) {
            fieldPosition.setBeginIndex(-1);
        }
        int length = stringBuffer.length();
        if (zAreSignificantDigitsUsed) {
            minimumFractionDigits = getMinimumSignificantDigits() - 1;
            maximumIntegerDigits = 1;
            minimumIntegerDigits = 1;
        } else {
            minimumFractionDigits = getMinimumFractionDigits();
            if (maximumIntegerDigits > 8) {
                maximumIntegerDigits = 1 < minimumIntegerDigits ? minimumIntegerDigits : 1;
            }
            if (maximumIntegerDigits > minimumIntegerDigits) {
                minimumIntegerDigits = 1;
            }
        }
        int i6 = this.digitList.decimalAt;
        if (maximumIntegerDigits > 1 && maximumIntegerDigits != minimumIntegerDigits) {
            i = (i6 > 0 ? (i6 - 1) / maximumIntegerDigits : (i6 / maximumIntegerDigits) - 1) * maximumIntegerDigits;
        } else {
            i = i6 - ((minimumIntegerDigits > 0 || minimumFractionDigits > 0) ? minimumIntegerDigits : 1);
        }
        int i7 = minimumFractionDigits + minimumIntegerDigits;
        if (!this.digitList.isZero()) {
            minimumIntegerDigits = this.digitList.decimalAt - i;
        }
        int i8 = this.digitList.count;
        if (i7 <= i8) {
            i7 = i8;
        }
        if (minimumIntegerDigits > i7) {
            i7 = minimumIntegerDigits;
        }
        int length2 = -1;
        long j = 0;
        int i9 = 0;
        boolean z2 = false;
        int i10 = -1;
        int i11 = 0;
        while (i9 < i7) {
            if (i9 == minimumIntegerDigits) {
                if (fieldPosition.getField() == 0 || fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
                    fieldPosition.setEndIndex(stringBuffer.length());
                }
                if (z) {
                    length2 = stringBuffer.length();
                    addAttribute(NumberFormat.Field.INTEGER, length, stringBuffer.length());
                }
                if (fieldPosition.getFieldAttribute() == NumberFormat.Field.DECIMAL_SEPARATOR) {
                    fieldPosition.setBeginIndex(stringBuffer.length());
                }
                stringBuffer.append(decimalSeparatorString);
                if (fieldPosition.getFieldAttribute() == NumberFormat.Field.DECIMAL_SEPARATOR) {
                    fieldPosition.setEndIndex(stringBuffer.length());
                }
                int length3 = stringBuffer.length();
                if (z) {
                    i4 = length3;
                    addAttribute(NumberFormat.Field.DECIMAL_SEPARATOR, stringBuffer.length() - i5, stringBuffer.length());
                } else {
                    i4 = length3;
                }
                if (fieldPosition.getField() == 1 || fieldPosition.getFieldAttribute() == NumberFormat.Field.FRACTION) {
                    fieldPosition.setBeginIndex(stringBuffer.length());
                }
                z2 = fieldPosition instanceof UFieldPosition;
                i10 = i4;
            }
            byte digitValue = i9 < this.digitList.count ? this.digitList.getDigitValue(i9) : (byte) 0;
            stringBuffer.append(digitStringsLocal[digitValue]);
            if (!z2) {
                i2 = minimumIntegerDigits;
                i3 = i;
                j = j;
            } else {
                i11++;
                i3 = i;
                i2 = minimumIntegerDigits;
                j = (j * 10) + ((long) digitValue);
            }
            i9++;
            i = i3;
            minimumIntegerDigits = i2;
            i5 = 1;
        }
        int i12 = i;
        long j2 = j;
        if (this.digitList.isZero() && i7 == 0) {
            stringBuffer.append(digitStringsLocal[0]);
        }
        if (i10 == -1 && this.decimalSeparatorAlwaysShown) {
            if (fieldPosition.getFieldAttribute() == NumberFormat.Field.DECIMAL_SEPARATOR) {
                fieldPosition.setBeginIndex(stringBuffer.length());
            }
            stringBuffer.append(decimalSeparatorString);
            if (fieldPosition.getFieldAttribute() == NumberFormat.Field.DECIMAL_SEPARATOR) {
                fieldPosition.setEndIndex(stringBuffer.length());
            }
            if (z) {
                addAttribute(NumberFormat.Field.DECIMAL_SEPARATOR, stringBuffer.length() - 1, stringBuffer.length());
            }
        }
        if (fieldPosition.getField() == 0) {
            if (fieldPosition.getEndIndex() < 0) {
                fieldPosition.setEndIndex(stringBuffer.length());
            }
        } else if (fieldPosition.getField() == 1) {
            if (fieldPosition.getBeginIndex() < 0) {
                fieldPosition.setBeginIndex(stringBuffer.length());
            }
            fieldPosition.setEndIndex(stringBuffer.length());
        } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
            if (fieldPosition.getEndIndex() < 0) {
                fieldPosition.setEndIndex(stringBuffer.length());
            }
        } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.FRACTION) {
            if (fieldPosition.getBeginIndex() < 0) {
                fieldPosition.setBeginIndex(stringBuffer.length());
            }
            fieldPosition.setEndIndex(stringBuffer.length());
        }
        if (z2) {
            ((UFieldPosition) fieldPosition).setFractionDigits(i11, j2);
        }
        if (z) {
            if (length2 < 0) {
                addAttribute(NumberFormat.Field.INTEGER, length, stringBuffer.length());
            }
            if (i10 > 0) {
                addAttribute(NumberFormat.Field.FRACTION, i10, stringBuffer.length());
            }
        }
        if (fieldPosition.getFieldAttribute() == NumberFormat.Field.EXPONENT_SYMBOL) {
            fieldPosition.setBeginIndex(stringBuffer.length());
        }
        stringBuffer.append(this.symbols.getExponentSeparator());
        if (fieldPosition.getFieldAttribute() == NumberFormat.Field.EXPONENT_SYMBOL) {
            fieldPosition.setEndIndex(stringBuffer.length());
        }
        if (z) {
            addAttribute(NumberFormat.Field.EXPONENT_SYMBOL, stringBuffer.length() - this.symbols.getExponentSeparator().length(), stringBuffer.length());
        }
        int i13 = this.digitList.isZero() ? 0 : i12;
        if (i13 < 0) {
            i13 = -i13;
            if (fieldPosition.getFieldAttribute() == NumberFormat.Field.EXPONENT_SIGN) {
                fieldPosition.setBeginIndex(stringBuffer.length());
            }
            stringBuffer.append(this.symbols.getMinusSignString());
            if (fieldPosition.getFieldAttribute() == NumberFormat.Field.EXPONENT_SIGN) {
                fieldPosition.setEndIndex(stringBuffer.length());
            }
            if (z) {
                addAttribute(NumberFormat.Field.EXPONENT_SIGN, stringBuffer.length() - 1, stringBuffer.length());
            }
        } else if (this.exponentSignAlwaysShown) {
            if (fieldPosition.getFieldAttribute() == NumberFormat.Field.EXPONENT_SIGN) {
                fieldPosition.setBeginIndex(stringBuffer.length());
            }
            stringBuffer.append(this.symbols.getPlusSignString());
            if (fieldPosition.getFieldAttribute() == NumberFormat.Field.EXPONENT_SIGN) {
                fieldPosition.setEndIndex(stringBuffer.length());
            }
            if (z) {
                addAttribute(NumberFormat.Field.EXPONENT_SIGN, stringBuffer.length() - 1, stringBuffer.length());
            }
        }
        int length4 = stringBuffer.length();
        this.digitList.set(i13);
        byte b = this.minExponentDigits;
        if (this.useExponentialNotation && b < 1) {
            b = 1;
        }
        for (int i14 = this.digitList.decimalAt; i14 < b; i14++) {
            stringBuffer.append(digitStringsLocal[0]);
        }
        int i15 = 0;
        while (i15 < this.digitList.decimalAt) {
            stringBuffer.append(i15 < this.digitList.count ? digitStringsLocal[this.digitList.getDigitValue(i15)] : digitStringsLocal[0]);
            i15++;
        }
        if (fieldPosition.getFieldAttribute() == NumberFormat.Field.EXPONENT) {
            fieldPosition.setBeginIndex(length4);
            fieldPosition.setEndIndex(stringBuffer.length());
        }
        if (z) {
            addAttribute(NumberFormat.Field.EXPONENT, length4, stringBuffer.length());
        }
    }

    private final void addPadding(StringBuffer stringBuffer, FieldPosition fieldPosition, int i, int i2) {
        int length;
        if (this.formatWidth > 0 && (length = this.formatWidth - stringBuffer.length()) > 0) {
            char[] cArr = new char[length];
            for (int i3 = 0; i3 < length; i3++) {
                cArr[i3] = this.pad;
            }
            switch (this.padPosition) {
                case 0:
                    stringBuffer.insert(0, cArr);
                    break;
                case 1:
                    stringBuffer.insert(i, cArr);
                    break;
                case 2:
                    stringBuffer.insert(stringBuffer.length() - i2, cArr);
                    break;
                case 3:
                    stringBuffer.append(cArr);
                    break;
            }
            if (this.padPosition == 0 || this.padPosition == 1) {
                fieldPosition.setBeginIndex(fieldPosition.getBeginIndex() + length);
                fieldPosition.setEndIndex(fieldPosition.getEndIndex() + length);
            }
        }
    }

    @Override
    public Number parse(String str, ParsePosition parsePosition) {
        return (Number) parse(str, parsePosition, null);
    }

    @Override
    public CurrencyAmount parseCurrency(CharSequence charSequence, ParsePosition parsePosition) {
        return (CurrencyAmount) parse(charSequence.toString(), parsePosition, new Currency[1]);
    }

    private Object parse(String str, ParsePosition parsePosition, Currency[] currencyArr) {
        int iSkipPadding;
        boolean[] zArr;
        char c;
        int i;
        int i2;
        Number numberDivide;
        int index = parsePosition.getIndex();
        if (this.formatWidth > 0 && (this.padPosition == 0 || this.padPosition == 1)) {
            iSkipPadding = skipPadding(str, index);
        } else {
            iSkipPadding = index;
        }
        if (str.regionMatches(iSkipPadding, this.symbols.getNaN(), 0, this.symbols.getNaN().length())) {
            int length = iSkipPadding + this.symbols.getNaN().length();
            if (this.formatWidth > 0 && (this.padPosition == 2 || this.padPosition == 3)) {
                length = skipPadding(str, length);
            }
            parsePosition.setIndex(length);
            return new Double(Double.NaN);
        }
        boolean[] zArr2 = new boolean[3];
        if (this.currencySignCount != 0) {
            if (!parseForCurrency(str, parsePosition, currencyArr, zArr2)) {
                return null;
            }
            zArr = zArr2;
            c = 2;
            i = 0;
            i2 = 1;
        } else {
            if (currencyArr != null) {
                return null;
            }
            zArr = zArr2;
            c = 2;
            i = 0;
            i2 = 1;
            if (!subparse(str, parsePosition, this.digitList, zArr2, currencyArr, this.negPrefixPattern, this.negSuffixPattern, this.posPrefixPattern, this.posSuffixPattern, false, 0)) {
                parsePosition.setIndex(index);
                return null;
            }
        }
        if (zArr[i]) {
            numberDivide = new Double(zArr[i2] ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
        } else if (zArr[c]) {
            numberDivide = zArr[i2] ? new Double("0.0") : new Double("-0.0");
        } else if (!zArr[i2] && this.digitList.isZero()) {
            numberDivide = new Double("-0.0");
        } else {
            int i3 = this.multiplier;
            while (i3 % 10 == 0) {
                this.digitList.decimalAt -= i2;
                i3 /= 10;
            }
            if (!this.parseBigDecimal && i3 == i2 && this.digitList.isIntegral()) {
                if (this.digitList.decimalAt < 12) {
                    long j = 0;
                    if (this.digitList.count > 0) {
                        long j2 = 0;
                        int i4 = i;
                        while (i4 < this.digitList.count) {
                            j2 = ((j2 * 10) + ((long) ((char) this.digitList.digits[i4]))) - 48;
                            i4++;
                        }
                        while (true) {
                            int i5 = i4 + 1;
                            if (i4 >= this.digitList.decimalAt) {
                                break;
                            }
                            j2 *= 10;
                            i4 = i5;
                        }
                        if (!zArr[i2]) {
                            j = -j2;
                        } else {
                            j = j2;
                        }
                    }
                    numberDivide = Long.valueOf(j);
                } else {
                    BigInteger bigInteger = this.digitList.getBigInteger(zArr[i2]);
                    int iBitLength = bigInteger.bitLength();
                    numberDivide = bigInteger;
                    if (iBitLength < 64) {
                        numberDivide = Long.valueOf(bigInteger.longValue());
                    }
                }
            } else {
                android.icu.math.BigDecimal bigDecimalICU = this.digitList.getBigDecimalICU(zArr[i2]);
                if (i3 != i2) {
                    numberDivide = bigDecimalICU.divide(android.icu.math.BigDecimal.valueOf(i3), this.mathContext);
                } else {
                    numberDivide = bigDecimalICU;
                }
            }
        }
        return currencyArr != null ? new CurrencyAmount(numberDivide, currencyArr[i]) : numberDivide;
    }

    private boolean parseForCurrency(String str, ParsePosition parsePosition, Currency[] currencyArr, boolean[] zArr) {
        DigitList_Android digitList_Android;
        ParsePosition parsePosition2;
        boolean[] zArr2;
        int i;
        boolean z;
        boolean zSubparse;
        int errorIndex;
        int index;
        Iterator<AffixForCurrency> it;
        boolean z2;
        boolean[] zArr3;
        ParsePosition parsePosition3;
        DigitList_Android digitList_Android2;
        int errorIndex2;
        int index2;
        int index3 = parsePosition.getIndex();
        if (!this.isReadyForParsing) {
            int i2 = this.currencySignCount;
            setupCurrencyAffixForAllPatterns();
            if (i2 == 3) {
                applyPatternWithoutExpandAffix(this.formatPattern, false);
            } else {
                applyPattern(this.formatPattern, false);
            }
            this.isReadyForParsing = true;
        }
        boolean[] zArr4 = null;
        boolean[] zArr5 = new boolean[3];
        ParsePosition parsePosition4 = new ParsePosition(index3);
        DigitList_Android digitList_Android3 = new DigitList_Android();
        if (this.style == 6) {
            digitList_Android = digitList_Android3;
            parsePosition2 = parsePosition4;
            zArr2 = zArr5;
            i = 3;
            z = true;
            zSubparse = subparse(str, parsePosition4, digitList_Android3, zArr5, currencyArr, this.negPrefixPattern, this.negSuffixPattern, this.posPrefixPattern, this.posSuffixPattern, true, 1);
        } else {
            digitList_Android = digitList_Android3;
            parsePosition2 = parsePosition4;
            zArr2 = zArr5;
            i = 3;
            z = true;
            zSubparse = subparse(str, parsePosition2, digitList_Android, zArr2, currencyArr, this.negPrefixPattern, this.negSuffixPattern, this.posPrefixPattern, this.posSuffixPattern, true, 0);
        }
        if (zSubparse) {
            ParsePosition parsePosition5 = parsePosition2;
            if (parsePosition5.getIndex() <= index3) {
                errorIndex = -1;
            } else {
                index = parsePosition5.getIndex();
                this.digitList = digitList_Android;
                errorIndex = -1;
                zArr4 = zArr2;
                it = this.affixPatternsForCurrency.iterator();
                int i3 = index;
                int i4 = errorIndex;
                boolean[] zArr6 = zArr4;
                z2 = zSubparse;
                while (it.hasNext()) {
                    AffixForCurrency next = it.next();
                    boolean[] zArr7 = new boolean[i];
                    ParsePosition parsePosition6 = new ParsePosition(index3);
                    DigitList_Android digitList_Android4 = new DigitList_Android();
                    int i5 = i4;
                    int i6 = i3;
                    Iterator<AffixForCurrency> it2 = it;
                    if (subparse(str, parsePosition6, digitList_Android4, zArr7, currencyArr, next.getNegPrefix(), next.getNegSuffix(), next.getPosPrefix(), next.getPosSuffix(), true, next.getPatternType())) {
                        if (parsePosition6.getIndex() > i6) {
                            int index4 = parsePosition6.getIndex();
                            this.digitList = digitList_Android4;
                            i3 = index4;
                            z2 = z;
                            zArr6 = zArr7;
                        } else {
                            i3 = i6;
                            z2 = z;
                        }
                        i4 = i5;
                    } else {
                        int errorIndex3 = i5;
                        if (parsePosition6.getErrorIndex() > errorIndex3) {
                            errorIndex3 = parsePosition6.getErrorIndex();
                        }
                        i3 = i6;
                        i4 = errorIndex3;
                    }
                    it = it2;
                    i = 3;
                }
                int i7 = i4;
                int i8 = i3;
                zArr3 = new boolean[3];
                parsePosition3 = new ParsePosition(index3);
                digitList_Android2 = new DigitList_Android();
                if (!subparse(str, parsePosition3, digitList_Android2, zArr3, currencyArr, this.negativePrefix, this.negativeSuffix, this.positivePrefix, this.positiveSuffix, false, 0)) {
                    if (parsePosition3.getIndex() > i8) {
                        index2 = parsePosition3.getIndex();
                        this.digitList = digitList_Android2;
                        zArr6 = zArr3;
                    } else {
                        index2 = i8;
                    }
                    z2 = z;
                    errorIndex2 = i7;
                } else {
                    errorIndex2 = parsePosition3.getErrorIndex() > i7 ? parsePosition3.getErrorIndex() : i7;
                    index2 = i8;
                }
                if (z2) {
                    parsePosition.setErrorIndex(errorIndex2);
                } else {
                    parsePosition.setIndex(index2);
                    parsePosition.setErrorIndex(-1);
                    for (int i9 = 0; i9 < 3; i9++) {
                        zArr[i9] = zArr6[i9];
                    }
                }
                return z2;
            }
        } else {
            errorIndex = parsePosition2.getErrorIndex();
        }
        index = index3;
        it = this.affixPatternsForCurrency.iterator();
        int i32 = index;
        int i42 = errorIndex;
        boolean[] zArr62 = zArr4;
        z2 = zSubparse;
        while (it.hasNext()) {
        }
        int i72 = i42;
        int i82 = i32;
        zArr3 = new boolean[3];
        parsePosition3 = new ParsePosition(index3);
        digitList_Android2 = new DigitList_Android();
        if (!subparse(str, parsePosition3, digitList_Android2, zArr3, currencyArr, this.negativePrefix, this.negativeSuffix, this.positivePrefix, this.positiveSuffix, false, 0)) {
        }
        if (z2) {
        }
        return z2;
    }

    private void setupCurrencyAffixForAllPatterns() {
        if (this.currencyPluralInfo == null) {
            this.currencyPluralInfo = new CurrencyPluralInfo(this.symbols.getULocale());
        }
        this.affixPatternsForCurrency = new HashSet();
        String str = this.formatPattern;
        applyPatternWithoutExpandAffix(getPattern(this.symbols.getULocale(), 1), false);
        this.affixPatternsForCurrency.add(new AffixForCurrency(this.negPrefixPattern, this.negSuffixPattern, this.posPrefixPattern, this.posSuffixPattern, 0));
        Iterator<String> itPluralPatternIterator = this.currencyPluralInfo.pluralPatternIterator();
        HashSet hashSet = new HashSet();
        while (itPluralPatternIterator.hasNext()) {
            String currencyPluralPattern = this.currencyPluralInfo.getCurrencyPluralPattern(itPluralPatternIterator.next());
            if (currencyPluralPattern != null && !hashSet.contains(currencyPluralPattern)) {
                hashSet.add(currencyPluralPattern);
                applyPatternWithoutExpandAffix(currencyPluralPattern, false);
                this.affixPatternsForCurrency.add(new AffixForCurrency(this.negPrefixPattern, this.negSuffixPattern, this.posPrefixPattern, this.posSuffixPattern, 1));
            }
        }
        this.formatPattern = str;
    }

    private final boolean subparse(String str, ParsePosition parsePosition, DigitList_Android digitList_Android, boolean[] zArr, Currency[] currencyArr, String str2, String str3, String str4, String str5, boolean z, int i) {
        int i2;
        int iCharCount;
        int i3;
        boolean z2;
        int i4;
        String str6;
        int i5;
        int i6;
        boolean z3;
        int i7;
        ParsePosition parsePosition2;
        byte b;
        String str7;
        String str8;
        int iCharCount2;
        int[] iArr;
        int iMatchesDigit;
        ParsePosition parsePosition3;
        ?? r15;
        boolean z4;
        int iCompareAffix;
        int i8;
        int iCompareAffix2;
        int index = parsePosition.getIndex();
        int index2 = parsePosition.getIndex();
        if (this.formatWidth > 0 && this.padPosition == 0) {
            index = skipPadding(str, index);
        }
        int i9 = index;
        int iCompareAffix3 = compareAffix(str, i9, false, true, str4, z, i, currencyArr);
        int iCompareAffix4 = compareAffix(str, i9, true, true, str2, z, i, currencyArr);
        if (iCompareAffix3 < 0 || iCompareAffix4 < 0) {
            i2 = iCompareAffix4;
        } else if (iCompareAffix3 > iCompareAffix4) {
            i2 = -1;
        } else if (iCompareAffix4 > iCompareAffix3) {
            i2 = iCompareAffix4;
            iCompareAffix3 = -1;
        }
        boolean z5 = false;
        if (iCompareAffix3 >= 0) {
            iCharCount = i9 + iCompareAffix3;
        } else {
            if (i2 < 0) {
                parsePosition.setErrorIndex(i9);
                return false;
            }
            iCharCount = i9 + i2;
        }
        boolean z6 = true;
        if (this.formatWidth > 0 && this.padPosition == 1) {
            iCharCount = skipPadding(str, iCharCount);
        }
        zArr[0] = false;
        if (str.regionMatches(iCharCount, this.symbols.getInfinity(), 0, this.symbols.getInfinity().length())) {
            iCharCount += this.symbols.getInfinity().length();
            zArr[0] = true;
            b = -1;
            i4 = iCompareAffix3;
            parsePosition2 = parsePosition;
            i6 = 2;
        } else {
            digitList_Android.count = 0;
            digitList_Android.decimalAt = 0;
            String decimalSeparatorString = this.currencySignCount == 0 ? this.symbols.getDecimalSeparatorString() : this.symbols.getMonetaryDecimalSeparatorString();
            String groupingSeparatorString = this.currencySignCount == 0 ? this.symbols.getGroupingSeparatorString() : this.symbols.getMonetaryGroupingSeparatorString();
            String exponentSeparator = this.symbols.getExponentSeparator();
            long j = 0;
            boolean zIsParseStrict = isParseStrict();
            byte b2 = this.groupingSize2 == 0 ? this.groupingSize : this.groupingSize2;
            UnicodeSet equivalentDecimals = skipExtendedSeparatorParsing ? UnicodeSet.EMPTY : getEquivalentDecimals(decimalSeparatorString, zIsParseStrict);
            UnicodeSet unicodeSet = skipExtendedSeparatorParsing ? UnicodeSet.EMPTY : zIsParseStrict ? strictDefaultGroupingSeparators : defaultGroupingSeparators;
            String str9 = groupingSeparatorString;
            int[] iArr2 = {-1};
            String strValueOf = decimalSeparatorString;
            int i10 = 0;
            boolean z7 = false;
            int i11 = 0;
            boolean z8 = false;
            int i12 = -1;
            String strValueOf2 = str9;
            boolean z9 = false;
            int i13 = -1;
            while (iCharCount < str.length()) {
                int iMatchesDigit2 = matchesDigit(str, iCharCount, iArr2);
                if (iMatchesDigit2 <= 0) {
                    i4 = iCompareAffix3;
                    i5 = i11;
                    int[] iArr3 = iArr2;
                    byte b3 = b2;
                    String str10 = strValueOf;
                    int length = str10.length();
                    if (!str.regionMatches(iCharCount, str10, 0, length)) {
                        if (isGroupingUsed()) {
                            str8 = strValueOf2;
                            int length2 = str8.length();
                            str7 = str10;
                            if (str.regionMatches(iCharCount, str8, 0, length2)) {
                                if (!z7) {
                                    if (zIsParseStrict && (!z8 || i12 != -1)) {
                                        i3 = i12;
                                        z2 = zIsParseStrict;
                                        str6 = str7;
                                        z3 = true;
                                        i6 = 2;
                                        break;
                                    }
                                    iCharCount2 = iCharCount + length2;
                                    strValueOf2 = str8;
                                    i11 = i5;
                                    iCompareAffix3 = i4;
                                    iArr2 = iArr3;
                                    b2 = b3;
                                    strValueOf = str7;
                                    z9 = true;
                                    int i14 = iCharCount;
                                    iCharCount = iCharCount2;
                                    i12 = i14;
                                } else {
                                    i3 = i12;
                                    z2 = zIsParseStrict;
                                    str6 = str7;
                                    break;
                                }
                            }
                        } else {
                            str7 = str10;
                            str8 = strValueOf2;
                        }
                        int iCodePointAt = str.codePointAt(iCharCount);
                        if (!z7 && equivalentDecimals.contains(iCodePointAt)) {
                            if (zIsParseStrict && (i12 != -1 || (i13 != -1 && i10 != this.groupingSize))) {
                                i3 = i12;
                                z2 = zIsParseStrict;
                                str6 = str7;
                                z3 = true;
                                i6 = 2;
                                break;
                            }
                            if (isParseIntegerOnly()) {
                                i3 = i12;
                                z2 = zIsParseStrict;
                                str6 = str7;
                                break;
                            }
                            digitList_Android.decimalAt = i5;
                            strValueOf = String.valueOf(Character.toChars(iCodePointAt));
                            iCharCount += Character.charCount(iCodePointAt);
                            strValueOf2 = str8;
                            i11 = i5;
                            iCompareAffix3 = i4;
                            iArr2 = iArr3;
                            b2 = b3;
                            z7 = true;
                        } else {
                            if (isGroupingUsed() && !z9) {
                                UnicodeSet unicodeSet2 = unicodeSet;
                                if (unicodeSet2.contains(iCodePointAt)) {
                                    if (!z7) {
                                        if (zIsParseStrict && (!z8 || i12 != -1)) {
                                            i3 = i12;
                                            z2 = zIsParseStrict;
                                            str6 = str7;
                                            z3 = true;
                                            i6 = 2;
                                            break;
                                        }
                                        strValueOf2 = String.valueOf(Character.toChars(iCodePointAt));
                                        iCharCount2 = Character.charCount(iCodePointAt) + iCharCount;
                                        unicodeSet = unicodeSet2;
                                        i11 = i5;
                                        iCompareAffix3 = i4;
                                        iArr2 = iArr3;
                                        b2 = b3;
                                        strValueOf = str7;
                                        z9 = true;
                                        int i142 = iCharCount;
                                        iCharCount = iCharCount2;
                                        i12 = i142;
                                    } else {
                                        i3 = i12;
                                        z2 = zIsParseStrict;
                                        str6 = str7;
                                        break;
                                    }
                                }
                            }
                            int[] iArr4 = iArr3;
                            str6 = str7;
                            i3 = i12;
                            z2 = zIsParseStrict;
                            i6 = 2;
                            if (str.regionMatches(true, iCharCount, exponentSeparator, 0, exponentSeparator.length())) {
                                int length3 = exponentSeparator.length() + iCharCount;
                                if (length3 < str.length()) {
                                    String plusSignString = this.symbols.getPlusSignString();
                                    String minusSignString = this.symbols.getMinusSignString();
                                    boolean z10 = false;
                                    if (str.regionMatches(length3, plusSignString, 0, plusSignString.length())) {
                                        length3 += plusSignString.length();
                                    } else if (str.regionMatches(length3, minusSignString, 0, minusSignString.length())) {
                                        length3 += minusSignString.length();
                                        z10 = true;
                                    } else {
                                        z10 = false;
                                    }
                                    DigitList_Android digitList_Android2 = new DigitList_Android();
                                    char c = 0;
                                    digitList_Android2.count = 0;
                                    while (length3 < str.length() && (iMatchesDigit = matchesDigit(str, length3, (iArr = iArr4))) > 0) {
                                        digitList_Android2.append((char) (iArr[c] + 48));
                                        length3 += iMatchesDigit;
                                        iArr4 = iArr;
                                        c = 0;
                                    }
                                    if (digitList_Android2.count > 0) {
                                        if (z2 && z9) {
                                            z3 = true;
                                        } else {
                                            if (digitList_Android2.count <= 10) {
                                                digitList_Android2.decimalAt = digitList_Android2.count;
                                                long j2 = digitList_Android2.getLong();
                                                if (z10) {
                                                    j2 = -j2;
                                                }
                                                j = j2;
                                            } else if (z10) {
                                                zArr[2] = true;
                                            } else {
                                                zArr[0] = true;
                                            }
                                            iCharCount = length3;
                                            z3 = false;
                                        }
                                    }
                                }
                            } else {
                                z3 = false;
                            }
                        }
                    } else {
                        if (zIsParseStrict && (i12 != -1 || (i13 != -1 && i10 != this.groupingSize))) {
                            i3 = i12;
                            str6 = str10;
                            z2 = zIsParseStrict;
                            z3 = true;
                            i6 = 2;
                            break;
                        }
                        if (isParseIntegerOnly() || z7) {
                            i3 = i12;
                            str6 = str10;
                            z2 = zIsParseStrict;
                            break;
                        }
                        digitList_Android.decimalAt = i5;
                        iCharCount += length;
                        strValueOf = str10;
                        i11 = i5;
                        iCompareAffix3 = i4;
                        iArr2 = iArr3;
                        b2 = b3;
                        z7 = true;
                    }
                } else {
                    i4 = iCompareAffix3;
                    if (i12 != -1) {
                        if (zIsParseStrict && ((i13 != -1 && i10 != b2) || (i13 == -1 && i10 > b2))) {
                            i3 = i12;
                            z2 = zIsParseStrict;
                            str6 = strValueOf;
                            i5 = i11;
                            z3 = true;
                            i6 = 2;
                            break;
                        }
                        i13 = i12;
                        i10 = 0;
                    }
                    i10++;
                    iCharCount += iMatchesDigit2;
                    if (iArr2[0] != 0 || digitList_Android.count != 0) {
                        i11++;
                        digitList_Android.append((char) (iArr2[0] + 48));
                    } else if (z7) {
                        digitList_Android.decimalAt--;
                    }
                    iCompareAffix3 = i4;
                    i12 = -1;
                    z8 = true;
                }
            }
            i3 = i12;
            z2 = zIsParseStrict;
            i4 = iCompareAffix3;
            str6 = strValueOf;
            i5 = i11;
            i6 = 2;
            z3 = false;
            if (digitList_Android.decimalAt == 0 && isDecimalPatternMatchRequired()) {
                i7 = -1;
                if (this.formatPattern.indexOf(str6) != -1) {
                    parsePosition.setIndex(index2);
                    parsePosition.setErrorIndex(iCharCount);
                    return false;
                }
            } else {
                i7 = -1;
            }
            parsePosition2 = parsePosition;
            int i15 = i3;
            if (i15 != i7) {
                iCharCount = i15;
            }
            if (!z7) {
                digitList_Android.decimalAt = i5;
            }
            if (!z2 || z7) {
                b = -1;
            } else {
                b = -1;
                if (i13 != -1 && i10 != this.groupingSize) {
                    z3 = true;
                }
            }
            if (z3) {
                parsePosition2.setIndex(index2);
                parsePosition2.setErrorIndex(iCharCount);
                return false;
            }
            long j3 = j + ((long) digitList_Android.decimalAt);
            if (j3 < (-getParseMaxDigits())) {
                z6 = true;
                zArr[i6] = true;
            } else {
                z6 = true;
                if (j3 > getParseMaxDigits()) {
                    zArr[0] = true;
                } else {
                    digitList_Android.decimalAt = (int) j3;
                }
            }
            if (!z8 && i5 == 0) {
                parsePosition2.setIndex(index2);
                parsePosition2.setErrorIndex(index2);
                return false;
            }
            z5 = false;
        }
        if (this.formatWidth > 0 && this.padPosition == i6) {
            iCharCount = skipPadding(str, iCharCount);
        }
        if (i4 >= 0) {
            parsePosition3 = parsePosition2;
            r15 = z6;
            z4 = z5;
            iCompareAffix = compareAffix(str, iCharCount, false, false, str5, z, i, currencyArr);
        } else {
            parsePosition3 = parsePosition2;
            r15 = z6;
            z4 = z5;
            iCompareAffix = i4;
        }
        if (i2 >= 0) {
            i8 = iCompareAffix;
            iCompareAffix2 = compareAffix(str, iCharCount, true, false, str3, z, i, currencyArr);
        } else {
            i8 = iCompareAffix;
            iCompareAffix2 = i2;
        }
        if (i8 >= 0 && iCompareAffix2 >= 0) {
            if (i8 > iCompareAffix2) {
                iCompareAffix2 = -1;
            } else if (iCompareAffix2 > i8) {
                i8 = -1;
            }
        }
        if ((i8 >= 0 ? r15 : z4) == (iCompareAffix2 >= 0 ? r15 : z4)) {
            parsePosition3.setErrorIndex(iCharCount);
            return z4;
        }
        if (i8 >= 0) {
            iCompareAffix2 = i8;
        }
        int iSkipPadding = iCharCount + iCompareAffix2;
        if (this.formatWidth > 0 && this.padPosition == 3) {
            iSkipPadding = skipPadding(str, iSkipPadding);
        }
        parsePosition3.setIndex(iSkipPadding);
        zArr[r15] = i8 >= 0 ? r15 : z4;
        if (parsePosition.getIndex() != index2) {
            return r15;
        }
        parsePosition3.setErrorIndex(iSkipPadding);
        return z4;
    }

    private int matchesDigit(String str, int i, int[] iArr) {
        String[] digitStringsLocal = this.symbols.getDigitStringsLocal();
        for (int i2 = 0; i2 < 10; i2++) {
            int length = digitStringsLocal[i2].length();
            if (str.regionMatches(i, digitStringsLocal[i2], 0, length)) {
                iArr[0] = i2;
                return length;
            }
        }
        int iCodePointAt = str.codePointAt(i);
        iArr[0] = UCharacter.digit(iCodePointAt, 10);
        if (iArr[0] < 0) {
            return 0;
        }
        return Character.charCount(iCodePointAt);
    }

    private UnicodeSet getEquivalentDecimals(String str, boolean z) {
        UnicodeSet unicodeSet = UnicodeSet.EMPTY;
        if (z) {
            if (strictDotEquivalents.contains(str)) {
                return strictDotEquivalents;
            }
            if (strictCommaEquivalents.contains(str)) {
                return strictCommaEquivalents;
            }
            return unicodeSet;
        }
        if (dotEquivalents.contains(str)) {
            return dotEquivalents;
        }
        if (commaEquivalents.contains(str)) {
            return commaEquivalents;
        }
        return unicodeSet;
    }

    private final int skipPadding(String str, int i) {
        while (i < str.length() && str.charAt(i) == this.pad) {
            i++;
        }
        return i;
    }

    private int compareAffix(String str, int i, boolean z, boolean z2, String str2, boolean z3, int i2, Currency[] currencyArr) {
        if (currencyArr != null || this.currencyChoice != null || (this.currencySignCount != 0 && z3)) {
            return compareComplexAffix(str2, str, i, i2, currencyArr);
        }
        if (z2) {
            return compareSimpleAffix(z ? this.negativePrefix : this.positivePrefix, str, i);
        }
        return compareSimpleAffix(z ? this.negativeSuffix : this.positiveSuffix, str, i);
    }

    private static boolean isBidiMark(int i) {
        return i == 8206 || i == 8207 || i == 1564;
    }

    private static String trimMarksFromAffix(String str) {
        boolean z;
        int i = 0;
        while (true) {
            if (i < str.length()) {
                if (!isBidiMark(str.charAt(i))) {
                    i++;
                } else {
                    z = true;
                    break;
                }
            } else {
                z = false;
                break;
            }
        }
        if (!z) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        sb.append((CharSequence) str, 0, i);
        for (int i2 = i + 1; i2 < str.length(); i2++) {
            char cCharAt = str.charAt(i2);
            if (!isBidiMark(cCharAt)) {
                sb.append(cCharAt);
            }
        }
        return sb.toString();
    }

    private static int compareSimpleAffix(String str, String str2, int i) {
        if (str.length() > 1) {
            str = trimMarksFromAffix(str);
        }
        int i2 = i;
        int iSkipUWhiteSpace = 0;
        while (iSkipUWhiteSpace < str.length()) {
            int iCharAt = UTF16.charAt(str, iSkipUWhiteSpace);
            int charCount = UTF16.getCharCount(iCharAt);
            if (PatternProps.isWhiteSpace(iCharAt)) {
                boolean z = false;
                while (i2 < str2.length()) {
                    int iCharAt2 = UTF16.charAt(str2, i2);
                    if (iCharAt2 == iCharAt) {
                        iSkipUWhiteSpace += charCount;
                        i2 += charCount;
                        if (iSkipUWhiteSpace != str.length()) {
                            iCharAt = UTF16.charAt(str, iSkipUWhiteSpace);
                            charCount = UTF16.getCharCount(iCharAt);
                            if (PatternProps.isWhiteSpace(iCharAt)) {
                                z = true;
                            }
                        }
                        z = true;
                        break;
                    }
                    if (!isBidiMark(iCharAt2)) {
                        break;
                    }
                    i2++;
                }
                int iSkipPatternWhiteSpace = skipPatternWhiteSpace(str, iSkipUWhiteSpace);
                int iSkipUWhiteSpace2 = skipUWhiteSpace(str2, i2);
                if (iSkipUWhiteSpace2 == i2 && !z) {
                    return -1;
                }
                iSkipUWhiteSpace = skipUWhiteSpace(str, iSkipPatternWhiteSpace);
                i2 = iSkipUWhiteSpace2;
            } else {
                int i3 = iSkipUWhiteSpace;
                boolean z2 = false;
                while (i2 < str2.length()) {
                    int iCharAt3 = UTF16.charAt(str2, i2);
                    if (!z2 && equalWithSignCompatibility(iCharAt3, iCharAt)) {
                        i3 += charCount;
                        i2 += charCount;
                        z2 = true;
                    } else {
                        if (!isBidiMark(iCharAt3)) {
                            break;
                        }
                        i2++;
                    }
                }
                if (!z2) {
                    return -1;
                }
                iSkipUWhiteSpace = i3;
            }
        }
        return i2 - i;
    }

    private static boolean equalWithSignCompatibility(int i, int i2) {
        return i == i2 || (minusSigns.contains(i) && minusSigns.contains(i2)) || (plusSigns.contains(i) && plusSigns.contains(i2));
    }

    private static int skipPatternWhiteSpace(String str, int i) {
        while (i < str.length()) {
            int iCharAt = UTF16.charAt(str, i);
            if (!PatternProps.isWhiteSpace(iCharAt)) {
                break;
            }
            i += UTF16.getCharCount(iCharAt);
        }
        return i;
    }

    private static int skipUWhiteSpace(String str, int i) {
        while (i < str.length()) {
            int iCharAt = UTF16.charAt(str, i);
            if (!UCharacter.isUWhiteSpace(iCharAt)) {
                break;
            }
            i += UTF16.getCharCount(iCharAt);
        }
        return i;
    }

    private static int skipBidiMarks(String str, int i) {
        while (i < str.length()) {
            int iCharAt = UTF16.charAt(str, i);
            if (!isBidiMark(iCharAt)) {
                break;
            }
            i += UTF16.getCharCount(iCharAt);
        }
        return i;
    }

    private int compareComplexAffix(String str, String str2, int i, int i2, Currency[] currencyArr) {
        int iMatch = i;
        int iSkipPatternWhiteSpace = 0;
        while (iSkipPatternWhiteSpace < str.length() && iMatch >= 0) {
            int i3 = iSkipPatternWhiteSpace + 1;
            char cCharAt = str.charAt(iSkipPatternWhiteSpace);
            if (cCharAt == '\'') {
                while (true) {
                    int iIndexOf = str.indexOf(39, i3);
                    if (iIndexOf == i3) {
                        iMatch = match(str2, iMatch, 39);
                        iSkipPatternWhiteSpace = iIndexOf + 1;
                        break;
                    }
                    if (iIndexOf > i3) {
                        iMatch = match(str2, iMatch, str.substring(i3, iIndexOf));
                        iSkipPatternWhiteSpace = iIndexOf + 1;
                        if (iSkipPatternWhiteSpace >= str.length() || str.charAt(iSkipPatternWhiteSpace) != '\'') {
                            break;
                        }
                        iMatch = match(str2, iMatch, 39);
                        i3 = iSkipPatternWhiteSpace + 1;
                    } else {
                        throw new RuntimeException();
                    }
                }
            } else {
                String percentString = null;
                if (cCharAt == '%') {
                    percentString = this.symbols.getPercentString();
                } else if (cCharAt == '+') {
                    percentString = this.symbols.getPlusSignString();
                } else if (cCharAt == '-') {
                    percentString = this.symbols.getMinusSignString();
                } else if (cCharAt == 164) {
                    if (i3 < str.length() && str.charAt(i3) == 164) {
                        i3++;
                    }
                    if (i3 < str.length() && str.charAt(i3) == 164) {
                        i3++;
                    }
                    iSkipPatternWhiteSpace = i3;
                    ULocale locale = getLocale(ULocale.VALID_LOCALE);
                    if (locale == null) {
                        locale = this.symbols.getLocale(ULocale.VALID_LOCALE);
                    }
                    ParsePosition parsePosition = new ParsePosition(iMatch);
                    String str3 = Currency.parse(locale, str2, i2, parsePosition);
                    if (str3 != null) {
                        if (currencyArr != null) {
                            currencyArr[0] = Currency.getInstance(str3);
                        } else if (str3.compareTo(getEffectiveCurrency().getCurrencyCode()) != 0) {
                        }
                        iMatch = parsePosition.getIndex();
                    }
                    iMatch = -1;
                } else if (cCharAt == 8240) {
                    percentString = this.symbols.getPerMillString();
                }
                if (percentString != null) {
                    iMatch = match(str2, iMatch, percentString);
                    iSkipPatternWhiteSpace = i3;
                } else {
                    iMatch = match(str2, iMatch, cCharAt);
                    iSkipPatternWhiteSpace = PatternProps.isWhiteSpace(cCharAt) ? skipPatternWhiteSpace(str, i3) : i3;
                }
            }
        }
        return iMatch - i;
    }

    static final int match(String str, int i, int i2) {
        if (i < 0 || i >= str.length()) {
            return -1;
        }
        int iSkipBidiMarks = skipBidiMarks(str, i);
        if (PatternProps.isWhiteSpace(i2)) {
            int iSkipPatternWhiteSpace = skipPatternWhiteSpace(str, iSkipBidiMarks);
            if (iSkipPatternWhiteSpace == iSkipBidiMarks) {
                return -1;
            }
            return iSkipPatternWhiteSpace;
        }
        if (iSkipBidiMarks >= str.length() || UTF16.charAt(str, iSkipBidiMarks) != i2) {
            return -1;
        }
        return skipBidiMarks(str, iSkipBidiMarks + UTF16.getCharCount(i2));
    }

    static final int match(String str, int i, String str2) {
        int charCount = 0;
        while (charCount < str2.length() && i >= 0) {
            int iCharAt = UTF16.charAt(str2, charCount);
            charCount += UTF16.getCharCount(iCharAt);
            if (!isBidiMark(iCharAt)) {
                i = match(str, i, iCharAt);
                if (PatternProps.isWhiteSpace(iCharAt)) {
                    charCount = skipPatternWhiteSpace(str2, charCount);
                }
            }
        }
        return i;
    }

    public DecimalFormatSymbols getDecimalFormatSymbols() {
        try {
            return (DecimalFormatSymbols) this.symbols.clone();
        } catch (Exception e) {
            return null;
        }
    }

    public void setDecimalFormatSymbols(DecimalFormatSymbols decimalFormatSymbols) {
        this.symbols = (DecimalFormatSymbols) decimalFormatSymbols.clone();
        setCurrencyForSymbols();
        expandAffixes(null);
    }

    private void setCurrencyForSymbols() {
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(this.symbols.getULocale());
        if (this.symbols.getCurrencySymbol().equals(decimalFormatSymbols.getCurrencySymbol()) && this.symbols.getInternationalCurrencySymbol().equals(decimalFormatSymbols.getInternationalCurrencySymbol())) {
            setCurrency(Currency.getInstance(this.symbols.getULocale()));
        } else {
            setCurrency(null);
        }
    }

    public String getPositivePrefix() {
        return this.positivePrefix;
    }

    public void setPositivePrefix(String str) {
        this.positivePrefix = str;
        this.posPrefixPattern = null;
    }

    public String getNegativePrefix() {
        return this.negativePrefix;
    }

    public void setNegativePrefix(String str) {
        this.negativePrefix = str;
        this.negPrefixPattern = null;
    }

    public String getPositiveSuffix() {
        return this.positiveSuffix;
    }

    public void setPositiveSuffix(String str) {
        this.positiveSuffix = str;
        this.posSuffixPattern = null;
    }

    public String getNegativeSuffix() {
        return this.negativeSuffix;
    }

    public void setNegativeSuffix(String str) {
        this.negativeSuffix = str;
        this.negSuffixPattern = null;
    }

    public int getMultiplier() {
        return this.multiplier;
    }

    public void setMultiplier(int i) {
        if (i == 0) {
            throw new IllegalArgumentException("Bad multiplier: " + i);
        }
        this.multiplier = i;
    }

    public BigDecimal getRoundingIncrement() {
        if (this.roundingIncrementICU == null) {
            return null;
        }
        return this.roundingIncrementICU.toBigDecimal();
    }

    public void setRoundingIncrement(BigDecimal bigDecimal) {
        if (bigDecimal == null) {
            setRoundingIncrement((android.icu.math.BigDecimal) null);
        } else {
            setRoundingIncrement(new android.icu.math.BigDecimal(bigDecimal));
        }
    }

    public void setRoundingIncrement(android.icu.math.BigDecimal bigDecimal) {
        int iCompareTo = bigDecimal == null ? 0 : bigDecimal.compareTo(android.icu.math.BigDecimal.ZERO);
        if (iCompareTo < 0) {
            throw new IllegalArgumentException("Illegal rounding increment");
        }
        if (iCompareTo == 0) {
            setInternalRoundingIncrement(null);
        } else {
            setInternalRoundingIncrement(bigDecimal);
        }
        resetActualRounding();
    }

    public void setRoundingIncrement(double d) {
        if (d < 0.0d) {
            throw new IllegalArgumentException("Illegal rounding increment");
        }
        if (d == 0.0d) {
            setInternalRoundingIncrement((android.icu.math.BigDecimal) null);
        } else {
            setInternalRoundingIncrement(android.icu.math.BigDecimal.valueOf(d));
        }
        resetActualRounding();
    }

    @Override
    public int getRoundingMode() {
        return this.roundingMode;
    }

    @Override
    public void setRoundingMode(int i) {
        if (i < 0 || i > 7) {
            throw new IllegalArgumentException("Invalid rounding mode: " + i);
        }
        this.roundingMode = i;
        resetActualRounding();
    }

    public int getFormatWidth() {
        return this.formatWidth;
    }

    public void setFormatWidth(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("Illegal format width");
        }
        this.formatWidth = i;
    }

    public char getPadCharacter() {
        return this.pad;
    }

    public void setPadCharacter(char c) {
        this.pad = c;
    }

    public int getPadPosition() {
        return this.padPosition;
    }

    public void setPadPosition(int i) {
        if (i < 0 || i > 3) {
            throw new IllegalArgumentException("Illegal pad position");
        }
        this.padPosition = i;
    }

    public boolean isScientificNotation() {
        return this.useExponentialNotation;
    }

    public void setScientificNotation(boolean z) {
        this.useExponentialNotation = z;
    }

    public byte getMinimumExponentDigits() {
        return this.minExponentDigits;
    }

    public void setMinimumExponentDigits(byte b) {
        if (b < 1) {
            throw new IllegalArgumentException("Exponent digits must be >= 1");
        }
        this.minExponentDigits = b;
    }

    public boolean isExponentSignAlwaysShown() {
        return this.exponentSignAlwaysShown;
    }

    public void setExponentSignAlwaysShown(boolean z) {
        this.exponentSignAlwaysShown = z;
    }

    public int getGroupingSize() {
        return this.groupingSize;
    }

    public void setGroupingSize(int i) {
        this.groupingSize = (byte) i;
    }

    public int getSecondaryGroupingSize() {
        return this.groupingSize2;
    }

    public void setSecondaryGroupingSize(int i) {
        this.groupingSize2 = (byte) i;
    }

    public MathContext getMathContextICU() {
        return this.mathContext;
    }

    public java.math.MathContext getMathContext() {
        try {
            if (this.mathContext == null) {
                return null;
            }
            return new java.math.MathContext(this.mathContext.getDigits(), RoundingMode.valueOf(this.mathContext.getRoundingMode()));
        } catch (Exception e) {
            return null;
        }
    }

    public void setMathContextICU(MathContext mathContext) {
        this.mathContext = mathContext;
    }

    public void setMathContext(java.math.MathContext mathContext) {
        this.mathContext = new MathContext(mathContext.getPrecision(), 1, false, mathContext.getRoundingMode().ordinal());
    }

    public boolean isDecimalSeparatorAlwaysShown() {
        return this.decimalSeparatorAlwaysShown;
    }

    public void setDecimalPatternMatchRequired(boolean z) {
        this.parseRequireDecimalPoint = z;
    }

    public boolean isDecimalPatternMatchRequired() {
        return this.parseRequireDecimalPoint;
    }

    public void setDecimalSeparatorAlwaysShown(boolean z) {
        this.decimalSeparatorAlwaysShown = z;
    }

    public CurrencyPluralInfo getCurrencyPluralInfo() {
        try {
            if (this.currencyPluralInfo == null) {
                return null;
            }
            return (CurrencyPluralInfo) this.currencyPluralInfo.clone();
        } catch (Exception e) {
            return null;
        }
    }

    public void setCurrencyPluralInfo(CurrencyPluralInfo currencyPluralInfo) {
        this.currencyPluralInfo = (CurrencyPluralInfo) currencyPluralInfo.clone();
        this.isReadyForParsing = false;
    }

    @Override
    public Object clone() {
        try {
            DecimalFormat_ICU58_Android decimalFormat_ICU58_Android = (DecimalFormat_ICU58_Android) super.clone();
            decimalFormat_ICU58_Android.symbols = (DecimalFormatSymbols) this.symbols.clone();
            decimalFormat_ICU58_Android.digitList = new DigitList_Android();
            if (this.currencyPluralInfo != null) {
                decimalFormat_ICU58_Android.currencyPluralInfo = (CurrencyPluralInfo) this.currencyPluralInfo.clone();
            }
            decimalFormat_ICU58_Android.attributes = new ArrayList<>();
            decimalFormat_ICU58_Android.currencyUsage = this.currencyUsage;
            return decimalFormat_ICU58_Android;
        } catch (Exception e) {
            throw new IllegalStateException();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !super.equals(obj)) {
            return false;
        }
        DecimalFormat_ICU58_Android decimalFormat_ICU58_Android = (DecimalFormat_ICU58_Android) obj;
        if (this.currencySignCount != decimalFormat_ICU58_Android.currencySignCount) {
            return false;
        }
        if ((this.style == 6 && (!equals(this.posPrefixPattern, decimalFormat_ICU58_Android.posPrefixPattern) || !equals(this.posSuffixPattern, decimalFormat_ICU58_Android.posSuffixPattern) || !equals(this.negPrefixPattern, decimalFormat_ICU58_Android.negPrefixPattern) || !equals(this.negSuffixPattern, decimalFormat_ICU58_Android.negSuffixPattern))) || this.multiplier != decimalFormat_ICU58_Android.multiplier || this.groupingSize != decimalFormat_ICU58_Android.groupingSize || this.groupingSize2 != decimalFormat_ICU58_Android.groupingSize2 || this.decimalSeparatorAlwaysShown != decimalFormat_ICU58_Android.decimalSeparatorAlwaysShown || this.useExponentialNotation != decimalFormat_ICU58_Android.useExponentialNotation) {
            return false;
        }
        if ((!this.useExponentialNotation || this.minExponentDigits == decimalFormat_ICU58_Android.minExponentDigits) && this.useSignificantDigits == decimalFormat_ICU58_Android.useSignificantDigits) {
            return (!this.useSignificantDigits || (this.minSignificantDigits == decimalFormat_ICU58_Android.minSignificantDigits && this.maxSignificantDigits == decimalFormat_ICU58_Android.maxSignificantDigits)) && this.symbols.equals(decimalFormat_ICU58_Android.symbols) && Utility.objectEquals(this.currencyPluralInfo, decimalFormat_ICU58_Android.currencyPluralInfo) && this.currencyUsage.equals(decimalFormat_ICU58_Android.currencyUsage);
        }
        return false;
    }

    private boolean equals(String str, String str2) {
        if (str == null || str2 == null) {
            return str == null && str2 == null;
        }
        if (str.equals(str2)) {
            return true;
        }
        return unquote(str).equals(unquote(str2));
    }

    private String unquote(String str) {
        StringBuilder sb = new StringBuilder(str.length());
        int i = 0;
        while (i < str.length()) {
            int i2 = i + 1;
            char cCharAt = str.charAt(i);
            if (cCharAt != '\'') {
                sb.append(cCharAt);
            }
            i = i2;
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return (super.hashCode() * 37) + this.positivePrefix.hashCode();
    }

    public String toPattern() {
        if (this.style == 6) {
            return this.formatPattern;
        }
        return toPattern(false);
    }

    public String toLocalizedPattern() {
        if (this.style == 6) {
            return this.formatPattern;
        }
        return toPattern(true);
    }

    private void expandAffixes(String str) {
        this.currencyChoice = null;
        StringBuffer stringBuffer = new StringBuffer();
        if (this.posPrefixPattern != null) {
            expandAffix(this.posPrefixPattern, str, stringBuffer);
            this.positivePrefix = stringBuffer.toString();
        }
        if (this.posSuffixPattern != null) {
            expandAffix(this.posSuffixPattern, str, stringBuffer);
            this.positiveSuffix = stringBuffer.toString();
        }
        if (this.negPrefixPattern != null) {
            expandAffix(this.negPrefixPattern, str, stringBuffer);
            this.negativePrefix = stringBuffer.toString();
        }
        if (this.negSuffixPattern != null) {
            expandAffix(this.negSuffixPattern, str, stringBuffer);
            this.negativeSuffix = stringBuffer.toString();
        }
    }

    private void expandAffix(String str, String str2, StringBuffer stringBuffer) {
        String internationalCurrencySymbol;
        stringBuffer.setLength(0);
        int i = 0;
        while (i < str.length()) {
            int i2 = i + 1;
            char cCharAt = str.charAt(i);
            if (cCharAt == '\'') {
                while (true) {
                    int iIndexOf = str.indexOf(39, i2);
                    if (iIndexOf == i2) {
                        stringBuffer.append('\'');
                        i = iIndexOf + 1;
                        break;
                    } else if (iIndexOf > i2) {
                        stringBuffer.append(str.substring(i2, iIndexOf));
                        i = iIndexOf + 1;
                        if (i >= str.length() || str.charAt(i) != '\'') {
                            break;
                        }
                        stringBuffer.append('\'');
                        i2 = i + 1;
                    } else {
                        throw new RuntimeException();
                    }
                }
            } else {
                if (cCharAt == '%') {
                    stringBuffer.append(this.symbols.getPercentString());
                } else if (cCharAt == '-') {
                    stringBuffer.append(this.symbols.getMinusSignString());
                } else if (cCharAt == 164) {
                    boolean z = true;
                    boolean z2 = i2 < str.length() && str.charAt(i2) == 164;
                    if (!z2 || (i2 = i2 + 1) >= str.length() || str.charAt(i2) != 164) {
                        z = false;
                    } else {
                        i2++;
                        z2 = false;
                    }
                    Currency currency = getCurrency();
                    if (currency == null) {
                        internationalCurrencySymbol = z2 ? this.symbols.getInternationalCurrencySymbol() : this.symbols.getCurrencySymbol();
                    } else if (z && str2 != null) {
                        internationalCurrencySymbol = currency.getName(this.symbols.getULocale(), 2, str2, (boolean[]) null);
                    } else if (!z2) {
                        internationalCurrencySymbol = currency.getName(this.symbols.getULocale(), 0, (boolean[]) null);
                    } else {
                        internationalCurrencySymbol = currency.getCurrencyCode();
                    }
                    stringBuffer.append(internationalCurrencySymbol);
                } else if (cCharAt == 8240) {
                    stringBuffer.append(this.symbols.getPerMillString());
                } else {
                    stringBuffer.append(cCharAt);
                }
                i = i2;
            }
        }
    }

    private int appendAffix(StringBuffer stringBuffer, boolean z, boolean z2, FieldPosition fieldPosition, boolean z3) {
        String str;
        String str2;
        String str3;
        if (this.currencyChoice != null) {
            if (z2) {
                str3 = z ? this.negPrefixPattern : this.posPrefixPattern;
            } else {
                str3 = z ? this.negSuffixPattern : this.posSuffixPattern;
            }
            StringBuffer stringBuffer2 = new StringBuffer();
            expandAffix(str3, null, stringBuffer2);
            stringBuffer.append(stringBuffer2);
            return stringBuffer2.length();
        }
        if (z2) {
            str = z ? this.negativePrefix : this.positivePrefix;
            str2 = z ? this.negPrefixPattern : this.posPrefixPattern;
        } else {
            str = z ? this.negativeSuffix : this.positiveSuffix;
            str2 = z ? this.negSuffixPattern : this.posSuffixPattern;
        }
        if (z3) {
            int iIndexOf = str.indexOf(this.symbols.getCurrencySymbol());
            if (iIndexOf > -1) {
                formatAffix2Attribute(z2, NumberFormat.Field.CURRENCY, stringBuffer, iIndexOf, this.symbols.getCurrencySymbol().length());
            }
            int iIndexOf2 = str.indexOf(this.symbols.getMinusSignString());
            if (iIndexOf2 > -1) {
                formatAffix2Attribute(z2, NumberFormat.Field.SIGN, stringBuffer, iIndexOf2, this.symbols.getMinusSignString().length());
            }
            int iIndexOf3 = str.indexOf(this.symbols.getPercentString());
            if (iIndexOf3 > -1) {
                formatAffix2Attribute(z2, NumberFormat.Field.PERCENT, stringBuffer, iIndexOf3, this.symbols.getPercentString().length());
            }
            int iIndexOf4 = str.indexOf(this.symbols.getPerMillString());
            if (iIndexOf4 > -1) {
                formatAffix2Attribute(z2, NumberFormat.Field.PERMILLE, stringBuffer, iIndexOf4, this.symbols.getPerMillString().length());
            }
            int iIndexOf5 = str2.indexOf("¤¤¤");
            if (iIndexOf5 > -1) {
                formatAffix2Attribute(z2, NumberFormat.Field.CURRENCY, stringBuffer, iIndexOf5, str.length() - iIndexOf5);
            }
        }
        if (fieldPosition.getFieldAttribute() == NumberFormat.Field.SIGN) {
            String minusSignString = z ? this.symbols.getMinusSignString() : this.symbols.getPlusSignString();
            int iIndexOf6 = str.indexOf(minusSignString);
            if (iIndexOf6 > -1) {
                int length = stringBuffer.length() + iIndexOf6;
                fieldPosition.setBeginIndex(length);
                fieldPosition.setEndIndex(length + minusSignString.length());
            }
        } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.PERCENT) {
            int iIndexOf7 = str.indexOf(this.symbols.getPercentString());
            if (iIndexOf7 > -1) {
                int length2 = stringBuffer.length() + iIndexOf7;
                fieldPosition.setBeginIndex(length2);
                fieldPosition.setEndIndex(length2 + this.symbols.getPercentString().length());
            }
        } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.PERMILLE) {
            int iIndexOf8 = str.indexOf(this.symbols.getPerMillString());
            if (iIndexOf8 > -1) {
                int length3 = stringBuffer.length() + iIndexOf8;
                fieldPosition.setBeginIndex(length3);
                fieldPosition.setEndIndex(length3 + this.symbols.getPerMillString().length());
            }
        } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.CURRENCY) {
            if (str.indexOf(this.symbols.getCurrencySymbol()) <= -1) {
                if (str.indexOf(this.symbols.getInternationalCurrencySymbol()) <= -1) {
                    if (str2.indexOf("¤¤¤") > -1) {
                        int length4 = stringBuffer.length() + str2.indexOf("¤¤¤");
                        int length5 = stringBuffer.length() + str.length();
                        fieldPosition.setBeginIndex(length4);
                        fieldPosition.setEndIndex(length5);
                    }
                } else {
                    String internationalCurrencySymbol = this.symbols.getInternationalCurrencySymbol();
                    int length6 = stringBuffer.length() + str.indexOf(internationalCurrencySymbol);
                    int length7 = internationalCurrencySymbol.length() + length6;
                    fieldPosition.setBeginIndex(length6);
                    fieldPosition.setEndIndex(length7);
                }
            } else {
                String currencySymbol = this.symbols.getCurrencySymbol();
                int length8 = stringBuffer.length() + str.indexOf(currencySymbol);
                int length9 = currencySymbol.length() + length8;
                fieldPosition.setBeginIndex(length8);
                fieldPosition.setEndIndex(length9);
            }
        }
        stringBuffer.append(str);
        return str.length();
    }

    private void formatAffix2Attribute(boolean z, NumberFormat.Field field, StringBuffer stringBuffer, int i, int i2) {
        if (!z) {
            i += stringBuffer.length();
        }
        addAttribute(field, i, i2 + i);
    }

    private void addAttribute(NumberFormat.Field field, int i, int i2) {
        FieldPosition fieldPosition = new FieldPosition(field);
        fieldPosition.setBeginIndex(i);
        fieldPosition.setEndIndex(i2);
        this.attributes.add(fieldPosition);
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        return formatToCharacterIterator(obj, NULL_UNIT);
    }

    AttributedCharacterIterator formatToCharacterIterator(Object obj, Unit unit) {
        if (!(obj instanceof Number)) {
            throw new IllegalArgumentException();
        }
        Number number = (Number) obj;
        StringBuffer stringBuffer = new StringBuffer();
        unit.writePrefix(stringBuffer);
        this.attributes.clear();
        if (obj instanceof BigInteger) {
            format((BigInteger) number, stringBuffer, new FieldPosition(0), true);
        } else if (obj instanceof BigDecimal) {
            format((BigDecimal) number, stringBuffer, new FieldPosition(0), true);
        } else if (obj instanceof Double) {
            format(number.doubleValue(), stringBuffer, new FieldPosition(0), true);
        } else if ((obj instanceof Integer) || (obj instanceof Long)) {
            format(number.longValue(), stringBuffer, new FieldPosition(0), true);
        } else {
            throw new IllegalArgumentException();
        }
        unit.writeSuffix(stringBuffer);
        AttributedString attributedString = new AttributedString(stringBuffer.toString());
        for (int i = 0; i < this.attributes.size(); i++) {
            FieldPosition fieldPosition = this.attributes.get(i);
            Format.Field fieldAttribute = fieldPosition.getFieldAttribute();
            attributedString.addAttribute(fieldAttribute, fieldAttribute, fieldPosition.getBeginIndex(), fieldPosition.getEndIndex());
        }
        return attributedString.getIterator();
    }

    private void appendAffixPattern(StringBuffer stringBuffer, boolean z, boolean z2, boolean z3) {
        String str;
        int iIndexOf;
        String str2;
        if (z2) {
            str = z ? this.negPrefixPattern : this.posPrefixPattern;
        } else {
            str = z ? this.negSuffixPattern : this.posSuffixPattern;
        }
        int i = 0;
        if (str == null) {
            if (z2) {
                str2 = z ? this.negativePrefix : this.positivePrefix;
            } else {
                str2 = z ? this.negativeSuffix : this.positiveSuffix;
            }
            stringBuffer.append('\'');
            while (i < str2.length()) {
                char cCharAt = str2.charAt(i);
                if (cCharAt == '\'') {
                    stringBuffer.append(cCharAt);
                }
                stringBuffer.append(cCharAt);
                i++;
            }
            stringBuffer.append('\'');
            return;
        }
        if (!z3) {
            stringBuffer.append(str);
            return;
        }
        while (i < str.length()) {
            char cCharAt2 = str.charAt(i);
            if (cCharAt2 == '%') {
                cCharAt2 = this.symbols.getPercent();
            } else if (cCharAt2 == '\'') {
                iIndexOf = str.indexOf(39, i + 1);
                if (iIndexOf < 0) {
                    throw new IllegalArgumentException("Malformed affix pattern: " + str);
                }
                stringBuffer.append(str.substring(i, iIndexOf + 1));
                i = iIndexOf + 1;
            } else if (cCharAt2 == '-') {
                cCharAt2 = this.symbols.getMinusSign();
            } else if (cCharAt2 == 8240) {
                cCharAt2 = this.symbols.getPerMill();
            }
            if (cCharAt2 == this.symbols.getDecimalSeparator() || cCharAt2 == this.symbols.getGroupingSeparator()) {
                stringBuffer.append('\'');
                stringBuffer.append(cCharAt2);
                stringBuffer.append('\'');
            } else {
                stringBuffer.append(cCharAt2);
            }
            iIndexOf = i;
            i = iIndexOf + 1;
        }
    }

    private String toPattern(boolean z) {
        String string;
        int length;
        char c;
        int minimumIntegerDigits;
        int maximumIntegerDigits;
        int i;
        int i2;
        int i3;
        int i4;
        StringBuffer stringBuffer = new StringBuffer();
        char zeroDigit = z ? this.symbols.getZeroDigit() : PATTERN_ZERO_DIGIT;
        char digit = z ? this.symbols.getDigit() : PATTERN_DIGIT;
        boolean zAreSignificantDigitsUsed = areSignificantDigitsUsed();
        int i5 = 0;
        char significantDigit = zAreSignificantDigitsUsed ? z ? this.symbols.getSignificantDigit() : PATTERN_SIGNIFICANT_DIGIT : (char) 0;
        char groupingSeparator = z ? this.symbols.getGroupingSeparator() : PATTERN_GROUPING_SEPARATOR;
        int i6 = this.formatWidth > 0 ? this.padPosition : -1;
        String str = null;
        if (this.formatWidth > 0) {
            StringBuffer stringBuffer2 = new StringBuffer(2);
            stringBuffer2.append(z ? this.symbols.getPadEscape() : PATTERN_PAD_ESCAPE);
            stringBuffer2.append(this.pad);
            string = stringBuffer2.toString();
        } else {
            string = null;
        }
        if (this.roundingIncrementICU != null) {
            int iScale = this.roundingIncrementICU.scale();
            String string2 = this.roundingIncrementICU.movePointRight(iScale).toString();
            length = string2.length() - iScale;
            str = string2;
        } else {
            length = 0;
        }
        int i7 = 0;
        for (int i8 = 2; i7 < i8; i8 = 2) {
            if (i6 == 0) {
                stringBuffer.append(string);
            }
            appendAffixPattern(stringBuffer, i7 != 0 ? 1 : i5, true, z);
            if (i6 == 1) {
                stringBuffer.append(string);
            }
            int length2 = stringBuffer.length();
            int iMax = isGroupingUsed() ? Math.max(i5, (int) this.groupingSize) : i5;
            if (iMax <= 0 || this.groupingSize2 <= 0) {
                c = significantDigit;
            } else {
                c = significantDigit;
                if (this.groupingSize2 != this.groupingSize) {
                    iMax += this.groupingSize2;
                }
            }
            if (zAreSignificantDigitsUsed) {
                minimumIntegerDigits = getMinimumSignificantDigits();
                maximumIntegerDigits = getMaximumSignificantDigits();
                i = i6;
                i2 = maximumIntegerDigits;
            } else {
                minimumIntegerDigits = getMinimumIntegerDigits();
                maximumIntegerDigits = getMaximumIntegerDigits();
                i = i6;
                i2 = 0;
            }
            char c2 = digit;
            int iMax2 = this.useExponentialNotation ? maximumIntegerDigits > 8 ? 1 : maximumIntegerDigits : zAreSignificantDigitsUsed ? Math.max(maximumIntegerDigits, iMax + 1) : Math.max(Math.max(iMax, getMinimumIntegerDigits()), length) + 1;
            int i9 = iMax2;
            while (i9 > 0) {
                if (!this.useExponentialNotation && i9 < iMax2 && isGroupingPosition(i9)) {
                    stringBuffer.append(groupingSeparator);
                }
                if (zAreSignificantDigitsUsed) {
                    stringBuffer.append((i2 < i9 || i9 <= i2 - minimumIntegerDigits) ? c2 : c);
                    i3 = i2;
                } else {
                    if (str == null || (i4 = length - i9) < 0) {
                        i3 = i2;
                    } else {
                        i3 = i2;
                        if (i4 < str.length()) {
                            stringBuffer.append((char) ((str.charAt(i4) - PATTERN_ZERO_DIGIT) + zeroDigit));
                        }
                    }
                    stringBuffer.append(i9 <= minimumIntegerDigits ? zeroDigit : c2);
                }
                i9--;
                i2 = i3;
            }
            if (!zAreSignificantDigitsUsed) {
                if (getMaximumFractionDigits() > 0 || this.decimalSeparatorAlwaysShown) {
                    stringBuffer.append(z ? this.symbols.getDecimalSeparator() : PATTERN_DECIMAL_SEPARATOR);
                }
                int i10 = length;
                int i11 = 0;
                while (i11 < getMaximumFractionDigits()) {
                    if (str == null || i10 >= str.length()) {
                        stringBuffer.append(i11 < getMinimumFractionDigits() ? zeroDigit : c2);
                    } else {
                        stringBuffer.append(i10 < 0 ? zeroDigit : (char) ((str.charAt(i10) - PATTERN_ZERO_DIGIT) + zeroDigit));
                        i10++;
                    }
                    i11++;
                }
            }
            if (this.useExponentialNotation) {
                if (z) {
                    stringBuffer.append(this.symbols.getExponentSeparator());
                } else {
                    stringBuffer.append(PATTERN_EXPONENT);
                }
                if (this.exponentSignAlwaysShown) {
                    stringBuffer.append(z ? this.symbols.getPlusSign() : PATTERN_PLUS_SIGN);
                }
                for (int i12 = 0; i12 < this.minExponentDigits; i12++) {
                    stringBuffer.append(zeroDigit);
                }
            }
            if (string != null && !this.useExponentialNotation) {
                int length3 = ((this.formatWidth - stringBuffer.length()) + length2) - (i7 == 0 ? this.positivePrefix.length() + this.positiveSuffix.length() : this.negativePrefix.length() + this.negativeSuffix.length());
                while (length3 > 0) {
                    char c3 = c2;
                    stringBuffer.insert(length2, c3);
                    iMax2++;
                    length3--;
                    if (length3 > 1 && isGroupingPosition(iMax2)) {
                        stringBuffer.insert(length2, groupingSeparator);
                        length3--;
                    }
                    c2 = c3;
                }
            }
            char c4 = c2;
            int i13 = i;
            if (i13 == 2) {
                stringBuffer.append(string);
            }
            appendAffixPattern(stringBuffer, i7 != 0, false, z);
            if (i13 == 3) {
                stringBuffer.append(string);
            }
            if (i7 == 0) {
                if (this.negativeSuffix.equals(this.positiveSuffix)) {
                    if (this.negativePrefix.equals(PATTERN_MINUS_SIGN + this.positivePrefix)) {
                        break;
                    }
                }
                stringBuffer.append(z ? this.symbols.getPatternSeparator() : PATTERN_SEPARATOR);
            }
            i7++;
            i6 = i13;
            digit = c4;
            i5 = 0;
            significantDigit = c;
        }
        return stringBuffer.toString();
    }

    public void applyPattern(String str) {
        applyPattern(str, false);
    }

    public void applyLocalizedPattern(String str) {
        applyPattern(str, true);
    }

    private void applyPattern(String str, boolean z) {
        applyPatternWithoutExpandAffix(str, z);
        expandAffixAdjustWidth(null);
    }

    private void expandAffixAdjustWidth(String str) {
        expandAffixes(str);
        if (this.formatWidth > 0) {
            this.formatWidth += this.positivePrefix.length() + this.positiveSuffix.length();
        }
    }

    private void applyPatternWithoutExpandAffix(String str, boolean z) {
        char significantDigit;
        char groupingSeparator;
        char decimalSeparator;
        char digit;
        char patternSeparator;
        char plusSign;
        char padEscape;
        String exponentSeparator;
        char zeroDigit;
        char minusSign;
        char percent;
        char perMill;
        char c;
        int i;
        char c2;
        char c3;
        char c4;
        char c5;
        char c6;
        char c7;
        String str2;
        int i2;
        byte b;
        char c8;
        int i3;
        byte b2;
        int i4;
        int i5;
        char c9;
        int i6;
        boolean z2;
        String str3;
        char c10;
        byte b3;
        char c11;
        int i7;
        ?? r8;
        char c12;
        char c13;
        char c14;
        char c15;
        char c16;
        char c17;
        int i8;
        char c18;
        int i9;
        char c19;
        String str4;
        char c20;
        char c21;
        byte b4;
        int i10;
        char cCharAt;
        int i11;
        int i12;
        byte b5;
        int i13;
        int i14;
        int i15;
        char c22;
        boolean z3;
        char c23;
        String strValueOf = String.valueOf(PATTERN_EXPONENT);
        if (z) {
            zeroDigit = this.symbols.getZeroDigit();
            significantDigit = this.symbols.getSignificantDigit();
            groupingSeparator = this.symbols.getGroupingSeparator();
            decimalSeparator = this.symbols.getDecimalSeparator();
            percent = this.symbols.getPercent();
            perMill = this.symbols.getPerMill();
            digit = this.symbols.getDigit();
            patternSeparator = this.symbols.getPatternSeparator();
            exponentSeparator = this.symbols.getExponentSeparator();
            plusSign = this.symbols.getPlusSign();
            padEscape = this.symbols.getPadEscape();
            minusSign = this.symbols.getMinusSign();
        } else {
            significantDigit = PATTERN_SIGNIFICANT_DIGIT;
            groupingSeparator = PATTERN_GROUPING_SEPARATOR;
            decimalSeparator = PATTERN_DECIMAL_SEPARATOR;
            digit = PATTERN_DIGIT;
            patternSeparator = PATTERN_SEPARATOR;
            plusSign = PATTERN_PLUS_SIGN;
            padEscape = PATTERN_PAD_ESCAPE;
            exponentSeparator = strValueOf;
            zeroDigit = '0';
            minusSign = PATTERN_MINUS_SIGN;
            percent = PATTERN_PERCENT;
            perMill = PATTERN_PER_MILLE;
        }
        char c24 = (char) (zeroDigit + '\t');
        char c25 = plusSign;
        int i16 = 0;
        int i17 = 0;
        boolean z4 = false;
        while (true) {
            String str5 = exponentSeparator;
            if (i16 < 2 && i17 < str.length()) {
                StringBuilder sb = new StringBuilder();
                StringBuilder sb2 = new StringBuilder();
                int length = i17;
                char c26 = padEscape;
                StringBuilder sb3 = sb;
                long j = 0;
                byte b6 = -1;
                int i18 = -1;
                int i19 = -1;
                byte b7 = -1;
                byte b8 = -1;
                int i20 = -1;
                int i21 = 1;
                int i22 = 0;
                int i23 = 0;
                int i24 = 0;
                int i25 = 0;
                int i26 = 1;
                int length2 = 0;
                int i27 = 0;
                boolean z5 = false;
                char cCharAt2 = 0;
                int i28 = 0;
                while (true) {
                    char c27 = minusSign;
                    if (length < str.length()) {
                        char cCharAt3 = str.charAt(length);
                        char c28 = perMill;
                        switch (i21) {
                            case 0:
                                c12 = decimalSeparator;
                                c13 = percent;
                                c14 = patternSeparator;
                                c15 = c26;
                                int i29 = i26;
                                int i30 = i20;
                                c16 = c27;
                                c17 = c28;
                                if (cCharAt3 != digit) {
                                    StringBuilder sb4 = sb3;
                                    int i31 = i23;
                                    byte b9 = b6;
                                    int i32 = i18;
                                    if ((cCharAt3 < zeroDigit || cCharAt3 > c24) && cCharAt3 != significantDigit) {
                                        i8 = i16;
                                        c18 = significantDigit;
                                        i9 = i29;
                                        if (cCharAt3 != groupingSeparator) {
                                            c19 = c12;
                                            if (cCharAt3 == c19) {
                                                if (i32 >= 0) {
                                                    patternError("Multiple decimal separators", str);
                                                }
                                                c20 = zeroDigit;
                                                i18 = i25 + i22 + i24;
                                                i20 = i30;
                                                i23 = i31;
                                                b6 = b9;
                                                c21 = c25;
                                                str4 = str5;
                                            } else {
                                                str4 = str5;
                                                if (str.regionMatches(length, str4, 0, str4.length())) {
                                                    if (b7 >= 0) {
                                                        patternError("Multiple exponential symbols", str);
                                                    }
                                                    if (b9 >= 0) {
                                                        patternError("Grouping separator in exponential", str);
                                                    }
                                                    length += str4.length();
                                                    if (length < str.length()) {
                                                        c21 = c25;
                                                        if (str.charAt(length) == c21) {
                                                            length++;
                                                            z5 = true;
                                                        }
                                                    } else {
                                                        c21 = c25;
                                                    }
                                                    b4 = 0;
                                                    while (length < str.length() && str.charAt(length) == zeroDigit) {
                                                        b4 = (byte) (b4 + 1);
                                                        length++;
                                                    }
                                                    c20 = zeroDigit;
                                                    if ((i25 + i22 < 1 && i31 + i24 < 1) || ((i31 > 0 && i25 > 0) || b4 < 1)) {
                                                        patternError("Malformed exponential", str);
                                                    }
                                                } else {
                                                    c20 = zeroDigit;
                                                    c21 = c25;
                                                    b4 = b7;
                                                }
                                                b7 = b4;
                                                i20 = i30;
                                                i23 = i31;
                                                b6 = b9;
                                                length2 = length;
                                                i18 = i32;
                                                sb3 = sb2;
                                                i26 = i9;
                                                i21 = 2;
                                                length--;
                                            }
                                        } else if (cCharAt3 != '\'' || (i10 = length + 1) >= str.length() || (cCharAt = str.charAt(i10)) == digit || (cCharAt >= zeroDigit && cCharAt <= c24)) {
                                            if (i32 >= 0) {
                                                patternError("Grouping separator after decimal", str);
                                            }
                                            c20 = zeroDigit;
                                            i20 = i30;
                                            i23 = i31;
                                            b8 = b9;
                                            i18 = i32;
                                            c21 = c25;
                                            str4 = str5;
                                            c19 = c12;
                                            sb3 = sb4;
                                            i26 = i9;
                                            b6 = 0;
                                        } else if (cCharAt == '\'') {
                                            length = i10;
                                            if (i32 >= 0) {
                                            }
                                            c20 = zeroDigit;
                                            i20 = i30;
                                            i23 = i31;
                                            b8 = b9;
                                            i18 = i32;
                                            c21 = c25;
                                            str4 = str5;
                                            c19 = c12;
                                            sb3 = sb4;
                                            i26 = i9;
                                            b6 = 0;
                                        } else if (b9 < 0) {
                                            c20 = zeroDigit;
                                            i20 = i30;
                                            i23 = i31;
                                            b6 = b9;
                                            i18 = i32;
                                            c21 = c25;
                                            str4 = str5;
                                            c19 = c12;
                                            sb3 = sb4;
                                            i26 = i9;
                                            i21 = 3;
                                        } else {
                                            i11 = length - 1;
                                            c20 = zeroDigit;
                                            i20 = i30;
                                            i23 = i31;
                                            b6 = b9;
                                            length2 = length;
                                            i18 = i32;
                                            c21 = c25;
                                            sb3 = sb2;
                                            c19 = c12;
                                            i26 = i9;
                                            i21 = 2;
                                            length = i11;
                                            str4 = str5;
                                        }
                                        length++;
                                        str5 = str4;
                                        decimalSeparator = c19;
                                        c25 = c21;
                                        minusSign = c16;
                                        c26 = c15;
                                        perMill = c17;
                                        patternSeparator = c14;
                                        percent = c13;
                                        i16 = i8;
                                        significantDigit = c18;
                                        zeroDigit = c20;
                                    } else {
                                        if (i24 > 0) {
                                            i9 = i29;
                                            StringBuilder sb5 = new StringBuilder();
                                            i8 = i16;
                                            sb5.append("Unexpected '");
                                            sb5.append(cCharAt3);
                                            sb5.append('\'');
                                            patternError(sb5.toString(), str);
                                        } else {
                                            i8 = i16;
                                            i9 = i29;
                                        }
                                        if (cCharAt3 == significantDigit) {
                                            c18 = significantDigit;
                                            i31++;
                                        } else {
                                            i22++;
                                            if (cCharAt3 != zeroDigit) {
                                                int i33 = i25 + i22 + i24;
                                                if (i19 >= 0) {
                                                    int i34 = i19;
                                                    while (i34 < i33) {
                                                        j *= 10;
                                                        i34++;
                                                    }
                                                    i19 = i34;
                                                } else {
                                                    i19 = i33;
                                                }
                                                c18 = significantDigit;
                                                j += (long) (cCharAt3 - zeroDigit);
                                            } else {
                                                c18 = significantDigit;
                                            }
                                        }
                                        if (b9 < 0 || i32 >= 0) {
                                            c20 = zeroDigit;
                                            i20 = i30;
                                            i23 = i31;
                                            b6 = b9;
                                        } else {
                                            c20 = zeroDigit;
                                            b6 = (byte) (b9 + 1);
                                            i20 = i30;
                                            i23 = i31;
                                        }
                                        i18 = i32;
                                        c21 = c25;
                                        str4 = str5;
                                        c19 = c12;
                                    }
                                    sb3 = sb4;
                                    i26 = i9;
                                    length++;
                                    str5 = str4;
                                    decimalSeparator = c19;
                                    c25 = c21;
                                    minusSign = c16;
                                    c26 = c15;
                                    perMill = c17;
                                    patternSeparator = c14;
                                    percent = c13;
                                    i16 = i8;
                                    significantDigit = c18;
                                    zeroDigit = c20;
                                } else {
                                    if (i22 <= 0) {
                                        i12 = i23;
                                        if (i12 <= 0) {
                                            i25++;
                                        }
                                        b5 = b6;
                                        if (b5 < 0) {
                                            i13 = i18;
                                            if (i13 < 0) {
                                                b5 = (byte) (b5 + 1);
                                            }
                                        } else {
                                            i13 = i18;
                                        }
                                        c20 = zeroDigit;
                                        i18 = i13;
                                        i8 = i16;
                                        c18 = significantDigit;
                                        i20 = i30;
                                        i23 = i12;
                                        i26 = i29;
                                        b6 = b5;
                                        c21 = c25;
                                        str4 = str5;
                                        c19 = c12;
                                        length++;
                                        str5 = str4;
                                        decimalSeparator = c19;
                                        c25 = c21;
                                        minusSign = c16;
                                        c26 = c15;
                                        perMill = c17;
                                        patternSeparator = c14;
                                        percent = c13;
                                        i16 = i8;
                                        significantDigit = c18;
                                        zeroDigit = c20;
                                    } else {
                                        i12 = i23;
                                    }
                                    i24++;
                                    b5 = b6;
                                    if (b5 < 0) {
                                    }
                                    c20 = zeroDigit;
                                    i18 = i13;
                                    i8 = i16;
                                    c18 = significantDigit;
                                    i20 = i30;
                                    i23 = i12;
                                    i26 = i29;
                                    b6 = b5;
                                    c21 = c25;
                                    str4 = str5;
                                    c19 = c12;
                                    length++;
                                    str5 = str4;
                                    decimalSeparator = c19;
                                    c25 = c21;
                                    minusSign = c16;
                                    c26 = c15;
                                    perMill = c17;
                                    patternSeparator = c14;
                                    percent = c13;
                                    i16 = i8;
                                    significantDigit = c18;
                                    zeroDigit = c20;
                                }
                                break;
                            case 1:
                            case 2:
                                char c29 = percent;
                                if (cCharAt3 == digit || cCharAt3 == groupingSeparator || cCharAt3 == decimalSeparator || ((cCharAt3 >= zeroDigit && cCharAt3 <= c24) || cCharAt3 == significantDigit)) {
                                    c12 = decimalSeparator;
                                    c14 = patternSeparator;
                                    c15 = c26;
                                    i14 = i26;
                                    i15 = i20;
                                    c16 = c27;
                                    c17 = c28;
                                    if (i21 == 1) {
                                        i11 = length - 1;
                                        c20 = zeroDigit;
                                        i8 = i16;
                                        c18 = significantDigit;
                                        i20 = i15;
                                        c13 = c29;
                                        i26 = i14;
                                        i27 = length;
                                        c21 = c25;
                                        c19 = c12;
                                        i21 = 0;
                                        length = i11;
                                        str4 = str5;
                                        length++;
                                        str5 = str4;
                                        decimalSeparator = c19;
                                        c25 = c21;
                                        minusSign = c16;
                                        c26 = c15;
                                        perMill = c17;
                                        patternSeparator = c14;
                                        percent = c13;
                                        i16 = i8;
                                        significantDigit = c18;
                                        zeroDigit = c20;
                                    } else if (cCharAt3 == '\'') {
                                        int i35 = length + 1;
                                        c13 = c29;
                                        if (i35 < str.length() && str.charAt(i35) == '\'') {
                                            sb3.append(cCharAt3);
                                            c20 = zeroDigit;
                                            i8 = i16;
                                            c18 = significantDigit;
                                            i20 = i15;
                                            i26 = i14;
                                            c21 = c25;
                                            str4 = str5;
                                            c19 = c12;
                                            length = i35;
                                            length++;
                                            str5 = str4;
                                            decimalSeparator = c19;
                                            c25 = c21;
                                            minusSign = c16;
                                            c26 = c15;
                                            perMill = c17;
                                            patternSeparator = c14;
                                            percent = c13;
                                            i16 = i8;
                                            significantDigit = c18;
                                            zeroDigit = c20;
                                        } else {
                                            c20 = zeroDigit;
                                            i21 += 2;
                                            i8 = i16;
                                            c18 = significantDigit;
                                            i20 = i15;
                                            i26 = i14;
                                        }
                                    } else {
                                        c13 = c29;
                                        patternError("Unquoted special character '" + cCharAt3 + '\'', str);
                                        i26 = i14;
                                        sb3.append(cCharAt3);
                                        c20 = zeroDigit;
                                        i8 = i16;
                                        c18 = significantDigit;
                                        i20 = i15;
                                    }
                                } else {
                                    if (cCharAt3 != 164) {
                                        c12 = decimalSeparator;
                                        if (cCharAt3 != '\'') {
                                            if (cCharAt3 == patternSeparator) {
                                                if (i21 == 1 || i16 == 1) {
                                                    patternError("Unquoted special character '" + cCharAt3 + '\'', str);
                                                }
                                                c = zeroDigit;
                                                i6 = length + 1;
                                                i = i16;
                                                c2 = significantDigit;
                                                c6 = patternSeparator;
                                                c7 = c25;
                                                str2 = str5;
                                                i2 = i23;
                                                b = b6;
                                                c8 = c26;
                                                i3 = i18;
                                                b2 = b7;
                                                i4 = i26;
                                                i5 = i20;
                                                c9 = c27;
                                                c5 = c28;
                                                c4 = c29;
                                                c3 = c12;
                                            } else {
                                                if (cCharAt3 != c29) {
                                                    c22 = c28;
                                                    if (cCharAt3 == c22) {
                                                        c15 = c26;
                                                        i15 = i20;
                                                        c16 = c27;
                                                    } else if (cCharAt3 == c27) {
                                                        c16 = c27;
                                                        c13 = c29;
                                                        c17 = c22;
                                                        c14 = patternSeparator;
                                                        c15 = c26;
                                                        i15 = i20;
                                                        cCharAt3 = PATTERN_MINUS_SIGN;
                                                        sb3.append(cCharAt3);
                                                        c20 = zeroDigit;
                                                        i8 = i16;
                                                        c18 = significantDigit;
                                                        i20 = i15;
                                                    } else {
                                                        c16 = c27;
                                                        char c30 = c26;
                                                        if (cCharAt3 == c30) {
                                                            if (i20 >= 0) {
                                                                patternError("Multiple pad specifiers", str);
                                                            }
                                                            i11 = length + 1;
                                                            c15 = c30;
                                                            if (i11 == str.length()) {
                                                                patternError("Invalid pad specifier", str);
                                                            }
                                                            c20 = zeroDigit;
                                                            i8 = i16;
                                                            c18 = significantDigit;
                                                            cCharAt2 = str.charAt(i11);
                                                            c13 = c29;
                                                            c17 = c22;
                                                            c14 = patternSeparator;
                                                            i20 = length;
                                                            c21 = c25;
                                                            c19 = c12;
                                                            length = i11;
                                                            str4 = str5;
                                                            length++;
                                                            str5 = str4;
                                                            decimalSeparator = c19;
                                                            c25 = c21;
                                                            minusSign = c16;
                                                            c26 = c15;
                                                            perMill = c17;
                                                            patternSeparator = c14;
                                                            percent = c13;
                                                            i16 = i8;
                                                            significantDigit = c18;
                                                            zeroDigit = c20;
                                                        } else {
                                                            c15 = c30;
                                                            i15 = i20;
                                                            c13 = c29;
                                                            c17 = c22;
                                                            c14 = patternSeparator;
                                                            i14 = i26;
                                                            i26 = i14;
                                                            sb3.append(cCharAt3);
                                                            c20 = zeroDigit;
                                                            i8 = i16;
                                                            c18 = significantDigit;
                                                            i20 = i15;
                                                        }
                                                    }
                                                } else {
                                                    c15 = c26;
                                                    i15 = i20;
                                                    c16 = c27;
                                                    c22 = c28;
                                                }
                                                c17 = c22;
                                                c14 = patternSeparator;
                                                if (i26 != 1) {
                                                    patternError("Too many percent/permille characters", str);
                                                }
                                                i26 = cCharAt3 == c29 ? 100 : 1000;
                                                cCharAt3 = cCharAt3 == c29 ? PATTERN_PERCENT : PATTERN_PER_MILLE;
                                                c13 = c29;
                                                sb3.append(cCharAt3);
                                                c20 = zeroDigit;
                                                i8 = i16;
                                                c18 = significantDigit;
                                                i20 = i15;
                                            }
                                            break;
                                        } else {
                                            int i36 = length + 1;
                                            if (i36 >= str.length() || str.charAt(i36) != '\'') {
                                                i21 += 2;
                                            } else {
                                                sb3.append(cCharAt3);
                                                length = i36;
                                            }
                                        }
                                    } else {
                                        int i37 = length + 1;
                                        if (i37 < str.length()) {
                                            c12 = decimalSeparator;
                                            z3 = str.charAt(i37) == 164;
                                            if (z3) {
                                                i28 = 1;
                                            } else {
                                                sb3.append(cCharAt3);
                                                length = i37 + 1;
                                                if (length >= str.length() || str.charAt(length) != 164) {
                                                    length = i37;
                                                    i28 = 2;
                                                } else {
                                                    sb3.append(cCharAt3);
                                                    i28 = 3;
                                                }
                                            }
                                        } else {
                                            c12 = decimalSeparator;
                                        }
                                        if (z3) {
                                        }
                                    }
                                    c14 = patternSeparator;
                                    c15 = c26;
                                    i15 = i20;
                                    c16 = c27;
                                    c17 = c28;
                                    c13 = c29;
                                    sb3.append(cCharAt3);
                                    c20 = zeroDigit;
                                    i8 = i16;
                                    c18 = significantDigit;
                                    i20 = i15;
                                }
                                c21 = c25;
                                str4 = str5;
                                c19 = c12;
                                length++;
                                str5 = str4;
                                decimalSeparator = c19;
                                c25 = c21;
                                minusSign = c16;
                                c26 = c15;
                                perMill = c17;
                                patternSeparator = c14;
                                percent = c13;
                                i16 = i8;
                                significantDigit = c18;
                                zeroDigit = c20;
                                break;
                            case 3:
                            case 4:
                                if (cCharAt3 == '\'') {
                                    int i38 = length + 1;
                                    c23 = percent;
                                    if (i38 >= str.length() || str.charAt(i38) != '\'') {
                                        i21 -= 2;
                                    } else {
                                        sb3.append(cCharAt3);
                                        length = i38;
                                    }
                                } else {
                                    c23 = percent;
                                }
                                sb3.append(cCharAt3);
                                c20 = zeroDigit;
                                i8 = i16;
                                c18 = significantDigit;
                                c19 = decimalSeparator;
                                c14 = patternSeparator;
                                c21 = c25;
                                str4 = str5;
                                c15 = c26;
                                c16 = c27;
                                c17 = c28;
                                c13 = c23;
                                length++;
                                str5 = str4;
                                decimalSeparator = c19;
                                c25 = c21;
                                minusSign = c16;
                                c26 = c15;
                                perMill = c17;
                                patternSeparator = c14;
                                percent = c13;
                                i16 = i8;
                                significantDigit = c18;
                                zeroDigit = c20;
                                break;
                            default:
                                c20 = zeroDigit;
                                i8 = i16;
                                c18 = significantDigit;
                                c19 = decimalSeparator;
                                c13 = percent;
                                c14 = patternSeparator;
                                c21 = c25;
                                str4 = str5;
                                c15 = c26;
                                c16 = c27;
                                c17 = c28;
                                sb3 = sb3;
                                length++;
                                str5 = str4;
                                decimalSeparator = c19;
                                c25 = c21;
                                minusSign = c16;
                                c26 = c15;
                                perMill = c17;
                                patternSeparator = c14;
                                percent = c13;
                                i16 = i8;
                                significantDigit = c18;
                                zeroDigit = c20;
                                break;
                        }
                    } else {
                        c = zeroDigit;
                        i = i16;
                        c2 = significantDigit;
                        c3 = decimalSeparator;
                        c4 = percent;
                        c5 = perMill;
                        c6 = patternSeparator;
                        c7 = c25;
                        str2 = str5;
                        i2 = i23;
                        b = b6;
                        c8 = c26;
                        i3 = i18;
                        b2 = b7;
                        i4 = i26;
                        i5 = i20;
                        c9 = c27;
                        i6 = length;
                        length = 0;
                    }
                }
                int i39 = i6;
                if (i21 == 3 || i21 == 4) {
                    patternError("Unterminated quote", str);
                }
                if (length2 == 0) {
                    length2 = str.length();
                }
                int i40 = length2;
                if (length == 0) {
                    length = str.length();
                }
                if (i22 != 0 || i2 != 0 || i25 <= 0 || i3 < 0) {
                    z2 = true;
                    str3 = str2;
                } else {
                    int i41 = i3 == 0 ? i3 + 1 : i3;
                    i24 = i25 - i41;
                    z2 = true;
                    i25 = i41 - 1;
                    str3 = str2;
                    i22 = 1;
                }
                int i42 = i25;
                if (i3 >= 0 || i24 <= 0 || i2 != 0) {
                    if (i3 >= 0) {
                        if (i2 > 0 || i3 < i42) {
                            c10 = c24;
                        } else {
                            c10 = c24;
                            if (i3 <= i42 + i22) {
                            }
                        }
                        c11 = c3;
                        b3 = b8;
                        patternError("Malformed pattern", str);
                    } else {
                        c10 = c24;
                    }
                    if (b != 0) {
                        b3 = b8;
                        if (b3 == 0 || (i2 > 0 && i22 > 0)) {
                            c11 = c3;
                        } else {
                            c11 = c3;
                            if (i21 > 2) {
                                patternError("Malformed pattern", str);
                            }
                        }
                    } else {
                        c11 = c3;
                        b3 = b8;
                    }
                    patternError("Malformed pattern", str);
                }
                if (i5 < 0) {
                    i7 = i27;
                    r8 = i5;
                } else if (i5 == length) {
                    i7 = i27;
                    r8 = 0;
                } else {
                    int i43 = i5 + 2;
                    i7 = i27;
                    if (i43 == i7) {
                        r8 = z2;
                    } else if (i5 == i40) {
                        r8 = 2;
                    } else if (i43 == length) {
                        r8 = 3;
                    } else {
                        patternError("Illegal pad position", str);
                        r8 = i5;
                    }
                }
                if (i == 0) {
                    String string = sb.toString();
                    this.negPrefixPattern = string;
                    this.posPrefixPattern = string;
                    String string2 = sb2.toString();
                    this.negSuffixPattern = string2;
                    this.posSuffixPattern = string2;
                    this.useExponentialNotation = b2 >= 0 ? z2 : false;
                    if (this.useExponentialNotation) {
                        this.minExponentDigits = b2;
                        this.exponentSignAlwaysShown = z5;
                    }
                    int i44 = i42 + i22;
                    int i45 = i44 + i24;
                    int i46 = i3 >= 0 ? i3 : i45;
                    boolean z6 = i2 > 0 ? z2 : false;
                    setSignificantDigitsUsed(z6);
                    if (z6) {
                        setMinimumSignificantDigits(i2);
                        setMaximumSignificantDigits(i2 + i24);
                    } else {
                        int i47 = i46 - i42;
                        setMinimumIntegerDigits(i47);
                        setMaximumIntegerDigits(this.useExponentialNotation ? i42 + i47 : DOUBLE_INTEGER_DIGITS);
                        _setMaximumFractionDigits(i3 >= 0 ? i45 - i3 : 0);
                        setMinimumFractionDigits(i3 >= 0 ? i44 - i3 : 0);
                    }
                    setGroupingUsed(b > 0 ? z2 : false);
                    this.groupingSize = b > 0 ? b : (byte) 0;
                    if (b3 <= 0 || b3 == b) {
                        b3 = 0;
                    }
                    this.groupingSize2 = b3;
                    this.multiplier = i4;
                    setDecimalSeparatorAlwaysShown((i3 == 0 || i3 == i45) ? z2 : false);
                    if (r8 >= 0) {
                        this.padPosition = r8;
                        this.formatWidth = i40 - i7;
                        this.pad = cCharAt2;
                    } else {
                        this.formatWidth = 0;
                    }
                    long j2 = j;
                    if (j2 != 0) {
                        int i48 = i19 - i46;
                        this.roundingIncrementICU = android.icu.math.BigDecimal.valueOf(j2, i48 > 0 ? i48 : 0);
                        if (i48 < 0) {
                            this.roundingIncrementICU = this.roundingIncrementICU.movePointRight(-i48);
                        }
                        this.roundingMode = 6;
                    } else {
                        setRoundingIncrement((android.icu.math.BigDecimal) null);
                    }
                    this.currencySignCount = i28;
                } else {
                    this.negPrefixPattern = sb.toString();
                    this.negSuffixPattern = sb2.toString();
                    z4 = z2;
                }
                i16 = i + 1;
                c25 = c7;
                minusSign = c9;
                padEscape = c8;
                perMill = c5;
                patternSeparator = c6;
                percent = c4;
                significantDigit = c2;
                zeroDigit = c;
                i17 = i39;
                exponentSeparator = str3;
                c24 = c10;
                decimalSeparator = c11;
            }
        }
        if (str.length() == 0) {
            this.posSuffixPattern = "";
            this.posPrefixPattern = "";
            setMinimumIntegerDigits(0);
            setMaximumIntegerDigits(DOUBLE_INTEGER_DIGITS);
            setMinimumFractionDigits(0);
            _setMaximumFractionDigits(DOUBLE_FRACTION_DIGITS);
        }
        if (!z4 || (this.negPrefixPattern.equals(this.posPrefixPattern) && this.negSuffixPattern.equals(this.posSuffixPattern))) {
            this.negSuffixPattern = this.posSuffixPattern;
            this.negPrefixPattern = PATTERN_MINUS_SIGN + this.posPrefixPattern;
        }
        setLocale(null, null);
        this.formatPattern = str;
        if (this.currencySignCount != 0) {
            Currency currency = getCurrency();
            if (currency != null) {
                setRoundingIncrement(currency.getRoundingIncrement(this.currencyUsage));
                int defaultFractionDigits = currency.getDefaultFractionDigits(this.currencyUsage);
                setMinimumFractionDigits(defaultFractionDigits);
                _setMaximumFractionDigits(defaultFractionDigits);
            }
            if (this.currencySignCount == 3 && this.currencyPluralInfo == null) {
                this.currencyPluralInfo = new CurrencyPluralInfo(this.symbols.getULocale());
            }
        }
        resetActualRounding();
    }

    private void patternError(String str, String str2) {
        throw new IllegalArgumentException(str + " in pattern \"" + str2 + '\"');
    }

    @Override
    public void setMaximumIntegerDigits(int i) {
        super.setMaximumIntegerDigits(Math.min(i, MAX_INTEGER_DIGITS));
    }

    @Override
    public void setMinimumIntegerDigits(int i) {
        super.setMinimumIntegerDigits(Math.min(i, DOUBLE_INTEGER_DIGITS));
    }

    public int getMinimumSignificantDigits() {
        return this.minSignificantDigits;
    }

    public int getMaximumSignificantDigits() {
        return this.maxSignificantDigits;
    }

    public void setMinimumSignificantDigits(int i) {
        if (i < 1) {
            i = 1;
        }
        int iMax = Math.max(this.maxSignificantDigits, i);
        this.minSignificantDigits = i;
        this.maxSignificantDigits = iMax;
        setSignificantDigitsUsed(true);
    }

    public void setMaximumSignificantDigits(int i) {
        if (i < 1) {
            i = 1;
        }
        this.minSignificantDigits = Math.min(this.minSignificantDigits, i);
        this.maxSignificantDigits = i;
        setSignificantDigitsUsed(true);
    }

    public boolean areSignificantDigitsUsed() {
        return this.useSignificantDigits;
    }

    public void setSignificantDigitsUsed(boolean z) {
        this.useSignificantDigits = z;
    }

    @Override
    public void setCurrency(Currency currency) {
        super.setCurrency(currency);
        if (currency != null) {
            String name = currency.getName(this.symbols.getULocale(), 0, (boolean[]) null);
            this.symbols.setCurrency(currency);
            this.symbols.setCurrencySymbol(name);
        }
        if (this.currencySignCount != 0) {
            if (currency != null) {
                setRoundingIncrement(currency.getRoundingIncrement(this.currencyUsage));
                int defaultFractionDigits = currency.getDefaultFractionDigits(this.currencyUsage);
                setMinimumFractionDigits(defaultFractionDigits);
                setMaximumFractionDigits(defaultFractionDigits);
            }
            if (this.currencySignCount != 3) {
                expandAffixes(null);
            }
        }
    }

    public void setCurrencyUsage(Currency.CurrencyUsage currencyUsage) {
        if (currencyUsage == null) {
            throw new NullPointerException("return value is null at method AAA");
        }
        this.currencyUsage = currencyUsage;
        Currency currency = getCurrency();
        if (currency != null) {
            setRoundingIncrement(currency.getRoundingIncrement(this.currencyUsage));
            int defaultFractionDigits = currency.getDefaultFractionDigits(this.currencyUsage);
            setMinimumFractionDigits(defaultFractionDigits);
            _setMaximumFractionDigits(defaultFractionDigits);
        }
    }

    public Currency.CurrencyUsage getCurrencyUsage() {
        return this.currencyUsage;
    }

    @Override
    @Deprecated
    protected Currency getEffectiveCurrency() {
        Currency currency = getCurrency();
        if (currency == null) {
            return Currency.getInstance(this.symbols.getInternationalCurrencySymbol());
        }
        return currency;
    }

    @Override
    public void setMaximumFractionDigits(int i) {
        _setMaximumFractionDigits(i);
        resetActualRounding();
    }

    private void _setMaximumFractionDigits(int i) {
        super.setMaximumFractionDigits(Math.min(i, DOUBLE_FRACTION_DIGITS));
    }

    @Override
    public void setMinimumFractionDigits(int i) {
        super.setMinimumFractionDigits(Math.min(i, DOUBLE_FRACTION_DIGITS));
    }

    public void setParseBigDecimal(boolean z) {
        this.parseBigDecimal = z;
    }

    public boolean isParseBigDecimal() {
        return this.parseBigDecimal;
    }

    public void setParseMaxDigits(int i) {
        if (i > 0) {
            this.PARSE_MAX_EXPONENT = i;
        }
    }

    public int getParseMaxDigits() {
        return this.PARSE_MAX_EXPONENT;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        this.attributes.clear();
        objectOutputStream.defaultWriteObject();
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        if (getMaximumIntegerDigits() > MAX_INTEGER_DIGITS) {
            setMaximumIntegerDigits(MAX_INTEGER_DIGITS);
        }
        if (getMaximumFractionDigits() > DOUBLE_FRACTION_DIGITS) {
            _setMaximumFractionDigits(DOUBLE_FRACTION_DIGITS);
        }
        if (this.serialVersionOnStream < 2) {
            this.exponentSignAlwaysShown = false;
            setInternalRoundingIncrement(null);
            this.roundingMode = 6;
            this.formatWidth = 0;
            this.pad = ' ';
            this.padPosition = 0;
            if (this.serialVersionOnStream < 1) {
                this.useExponentialNotation = false;
            }
        }
        if (this.serialVersionOnStream < 3) {
            setCurrencyForSymbols();
        }
        if (this.serialVersionOnStream < 4) {
            this.currencyUsage = Currency.CurrencyUsage.STANDARD;
        }
        this.serialVersionOnStream = 4;
        this.digitList = new DigitList_Android();
        if (this.roundingIncrement != null) {
            setInternalRoundingIncrement(new android.icu.math.BigDecimal(this.roundingIncrement));
        }
        resetActualRounding();
    }

    private void setInternalRoundingIncrement(android.icu.math.BigDecimal bigDecimal) {
        this.roundingIncrementICU = bigDecimal;
        this.roundingIncrement = bigDecimal == null ? null : bigDecimal.toBigDecimal();
    }

    private static final class AffixForCurrency {
        private String negPrefixPatternForCurrency;
        private String negSuffixPatternForCurrency;
        private final int patternType;
        private String posPrefixPatternForCurrency;
        private String posSuffixPatternForCurrency;

        public AffixForCurrency(String str, String str2, String str3, String str4, int i) {
            this.negPrefixPatternForCurrency = null;
            this.negSuffixPatternForCurrency = null;
            this.posPrefixPatternForCurrency = null;
            this.posSuffixPatternForCurrency = null;
            this.negPrefixPatternForCurrency = str;
            this.negSuffixPatternForCurrency = str2;
            this.posPrefixPatternForCurrency = str3;
            this.posSuffixPatternForCurrency = str4;
            this.patternType = i;
        }

        public String getNegPrefix() {
            return this.negPrefixPatternForCurrency;
        }

        public String getNegSuffix() {
            return this.negSuffixPatternForCurrency;
        }

        public String getPosPrefix() {
            return this.posPrefixPatternForCurrency;
        }

        public String getPosSuffix() {
            return this.posSuffixPatternForCurrency;
        }

        public int getPatternType() {
            return this.patternType;
        }
    }

    static class Unit {
        private final String prefix;
        private final String suffix;

        public Unit(String str, String str2) {
            this.prefix = str;
            this.suffix = str2;
        }

        public void writeSuffix(StringBuffer stringBuffer) {
            stringBuffer.append(this.suffix);
        }

        public void writePrefix(StringBuffer stringBuffer) {
            stringBuffer.append(this.prefix);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Unit)) {
                return false;
            }
            Unit unit = (Unit) obj;
            return this.prefix.equals(unit.prefix) && this.suffix.equals(unit.suffix);
        }

        public String toString() {
            return this.prefix + "/" + this.suffix;
        }
    }

    private void resetActualRounding() {
        if (this.roundingIncrementICU != null) {
            android.icu.math.BigDecimal bigDecimalMovePointLeft = getMaximumFractionDigits() > 0 ? android.icu.math.BigDecimal.ONE.movePointLeft(getMaximumFractionDigits()) : android.icu.math.BigDecimal.ONE;
            if (this.roundingIncrementICU.compareTo(bigDecimalMovePointLeft) >= 0) {
                this.actualRoundingIncrementICU = this.roundingIncrementICU;
            } else {
                if (bigDecimalMovePointLeft.equals(android.icu.math.BigDecimal.ONE)) {
                    bigDecimalMovePointLeft = null;
                }
                this.actualRoundingIncrementICU = bigDecimalMovePointLeft;
            }
        } else if (this.roundingMode == 6 || isScientificNotation()) {
            this.actualRoundingIncrementICU = null;
        } else if (getMaximumFractionDigits() > 0) {
            this.actualRoundingIncrementICU = android.icu.math.BigDecimal.ONE.movePointLeft(getMaximumFractionDigits());
        } else {
            this.actualRoundingIncrementICU = android.icu.math.BigDecimal.ONE;
        }
        if (this.actualRoundingIncrementICU == null) {
            setRoundingDouble(0.0d);
            this.actualRoundingIncrement = null;
        } else {
            setRoundingDouble(this.actualRoundingIncrementICU.doubleValue());
            this.actualRoundingIncrement = this.actualRoundingIncrementICU.toBigDecimal();
        }
    }

    private void setRoundingDouble(double d) {
        this.roundingDouble = d;
        if (this.roundingDouble > 0.0d) {
            double d2 = 1.0d / this.roundingDouble;
            this.roundingDoubleReciprocal = Math.rint(d2);
            if (Math.abs(d2 - this.roundingDoubleReciprocal) > roundingIncrementEpsilon) {
                this.roundingDoubleReciprocal = 0.0d;
                return;
            }
            return;
        }
        this.roundingDoubleReciprocal = 0.0d;
    }
}
