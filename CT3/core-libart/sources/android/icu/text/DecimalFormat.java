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

public class DecimalFormat extends NumberFormat {
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
    private static final char PATTERN_MINUS = '-';
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
    private transient DigitList digitList;
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

    public DecimalFormat() {
        this.parseRequireDecimalPoint = false;
        this.PARSE_MAX_EXPONENT = 1000;
        this.digitList = new DigitList();
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
        ULocale def = ULocale.getDefault(ULocale.Category.FORMAT);
        String pattern = getPattern(def, 0);
        this.symbols = new DecimalFormatSymbols(def);
        setCurrency(Currency.getInstance(def));
        applyPatternWithoutExpandAffix(pattern, false);
        if (this.currencySignCount == 3) {
            this.currencyPluralInfo = new CurrencyPluralInfo(def);
        } else {
            expandAffixAdjustWidth(null);
        }
    }

    public DecimalFormat(String pattern) {
        this.parseRequireDecimalPoint = false;
        this.PARSE_MAX_EXPONENT = 1000;
        this.digitList = new DigitList();
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
        ULocale def = ULocale.getDefault(ULocale.Category.FORMAT);
        this.symbols = new DecimalFormatSymbols(def);
        setCurrency(Currency.getInstance(def));
        applyPatternWithoutExpandAffix(pattern, false);
        if (this.currencySignCount == 3) {
            this.currencyPluralInfo = new CurrencyPluralInfo(def);
        } else {
            expandAffixAdjustWidth(null);
        }
    }

    public DecimalFormat(String pattern, DecimalFormatSymbols symbols) {
        this.parseRequireDecimalPoint = false;
        this.PARSE_MAX_EXPONENT = 1000;
        this.digitList = new DigitList();
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
        createFromPatternAndSymbols(pattern, symbols);
    }

    private void createFromPatternAndSymbols(String pattern, DecimalFormatSymbols inputSymbols) {
        this.symbols = (DecimalFormatSymbols) inputSymbols.clone();
        if (pattern.indexOf(164) >= 0) {
            setCurrencyForSymbols();
        }
        applyPatternWithoutExpandAffix(pattern, false);
        if (this.currencySignCount == 3) {
            this.currencyPluralInfo = new CurrencyPluralInfo(this.symbols.getULocale());
        } else {
            expandAffixAdjustWidth(null);
        }
    }

    public DecimalFormat(String pattern, DecimalFormatSymbols symbols, CurrencyPluralInfo infoInput, int style) {
        this.parseRequireDecimalPoint = false;
        this.PARSE_MAX_EXPONENT = 1000;
        this.digitList = new DigitList();
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
        CurrencyPluralInfo info = infoInput;
        create(pattern, symbols, style == 6 ? (CurrencyPluralInfo) infoInput.clone() : info, style);
    }

    private void create(String pattern, DecimalFormatSymbols inputSymbols, CurrencyPluralInfo info, int inputStyle) {
        if (inputStyle != 6) {
            createFromPatternAndSymbols(pattern, inputSymbols);
        } else {
            this.symbols = (DecimalFormatSymbols) inputSymbols.clone();
            this.currencyPluralInfo = info;
            String currencyPluralPatternForOther = this.currencyPluralInfo.getCurrencyPluralPattern(PluralRules.KEYWORD_OTHER);
            applyPatternWithoutExpandAffix(currencyPluralPatternForOther, false);
            setCurrencyForSymbols();
        }
        this.style = inputStyle;
    }

    DecimalFormat(String pattern, DecimalFormatSymbols inputSymbols, int style) {
        this.parseRequireDecimalPoint = false;
        this.PARSE_MAX_EXPONENT = 1000;
        this.digitList = new DigitList();
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
        create(pattern, inputSymbols, style == 6 ? new CurrencyPluralInfo(inputSymbols.getULocale()) : null, style);
    }

    @Override
    public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
        return format(number, result, fieldPosition, false);
    }

    private boolean isNegative(double number) {
        if (number >= 0.0d) {
            return number == 0.0d && 1.0d / number < 0.0d;
        }
        return true;
    }

    private double round(double number) {
        boolean isNegative = isNegative(number);
        if (isNegative) {
            number = -number;
        }
        if (this.roundingDouble > 0.0d) {
            return round(number, this.roundingDouble, this.roundingDoubleReciprocal, this.roundingMode, isNegative);
        }
        return number;
    }

    private double multiply(double number) {
        if (this.multiplier != 1) {
            return ((double) this.multiplier) * number;
        }
        return number;
    }

    private StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition, boolean parseAttr) {
        StringBuffer stringBufferSubformat;
        fieldPosition.setBeginIndex(0);
        fieldPosition.setEndIndex(0);
        if (Double.isNaN(number)) {
            if (fieldPosition.getField() == 0 || fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
                fieldPosition.setBeginIndex(result.length());
            }
            result.append(this.symbols.getNaN());
            if (parseAttr) {
                addAttribute(NumberFormat.Field.INTEGER, result.length() - this.symbols.getNaN().length(), result.length());
            }
            if (fieldPosition.getField() == 0 || fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
                fieldPosition.setEndIndex(result.length());
            }
            addPadding(result, fieldPosition, 0, 0);
            return result;
        }
        double number2 = multiply(number);
        boolean isNegative = isNegative(number2);
        double number3 = round(number2);
        if (Double.isInfinite(number3)) {
            int prefixLen = appendAffix(result, isNegative, true, fieldPosition, parseAttr);
            if (fieldPosition.getField() == 0 || fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
                fieldPosition.setBeginIndex(result.length());
            }
            result.append(this.symbols.getInfinity());
            if (parseAttr) {
                addAttribute(NumberFormat.Field.INTEGER, result.length() - this.symbols.getInfinity().length(), result.length());
            }
            if (fieldPosition.getField() == 0 || fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
                fieldPosition.setEndIndex(result.length());
            }
            int suffixLen = appendAffix(result, isNegative, false, fieldPosition, parseAttr);
            addPadding(result, fieldPosition, prefixLen, suffixLen);
            return result;
        }
        int precision = precision(false);
        if (this.useExponentialNotation && precision > 0 && number3 != 0.0d && this.roundingMode != 6) {
            int log10RoundingIncr = (1 - precision) + ((int) Math.floor(Math.log10(Math.abs(number3))));
            double roundingIncReciprocal = 0.0d;
            double roundingInc = 0.0d;
            if (log10RoundingIncr < 0) {
                roundingIncReciprocal = android.icu.math.BigDecimal.ONE.movePointRight(-log10RoundingIncr).doubleValue();
            } else {
                roundingInc = android.icu.math.BigDecimal.ONE.movePointRight(log10RoundingIncr).doubleValue();
            }
            number3 = round(number3, roundingInc, roundingIncReciprocal, this.roundingMode, isNegative);
        }
        synchronized (this.digitList) {
            DigitList digitList = this.digitList;
            boolean z = (this.useExponentialNotation || areSignificantDigitsUsed()) ? false : true;
            digitList.set(number3, precision, z);
            stringBufferSubformat = subformat(number3, result, fieldPosition, isNegative, false, parseAttr);
        }
        return stringBufferSubformat;
    }

    @Deprecated
    double adjustNumberAsInFormatting(double number) {
        if (Double.isNaN(number)) {
            return number;
        }
        double number2 = round(multiply(number));
        if (Double.isInfinite(number2)) {
            return number2;
        }
        return toDigitList(number2).getDouble();
    }

    @Deprecated
    DigitList toDigitList(double number) {
        DigitList result = new DigitList();
        result.set(number, precision(false), false);
        return result;
    }

    @Deprecated
    boolean isNumberNegative(double number) {
        if (Double.isNaN(number)) {
            return false;
        }
        return isNegative(multiply(number));
    }

    private static double round(double number, double roundingInc, double roundingIncReciprocal, int mode, boolean isNegative) {
        double div;
        double div2 = roundingIncReciprocal == 0.0d ? number / roundingInc : number * roundingIncReciprocal;
        switch (mode) {
            case 0:
                div = Math.ceil(div2 - epsilon);
                break;
            case 1:
                div = Math.floor(epsilon + div2);
                break;
            case 2:
                div = !isNegative ? Math.ceil(div2 - epsilon) : Math.floor(epsilon + div2);
                break;
            case 3:
                div = !isNegative ? Math.floor(epsilon + div2) : Math.ceil(div2 - epsilon);
                break;
            case 4:
            case 5:
            case 6:
            default:
                double ceil = Math.ceil(div2);
                double ceildiff = ceil - div2;
                double floor = Math.floor(div2);
                double floordiff = div2 - floor;
                switch (mode) {
                    case 4:
                        div = ceildiff > epsilon + floordiff ? floor : ceil;
                        break;
                    case 5:
                        div = floordiff > epsilon + ceildiff ? ceil : floor;
                        break;
                    case 6:
                        if (epsilon + floordiff < ceildiff) {
                            div = floor;
                        } else if (epsilon + ceildiff < floordiff) {
                            div = ceil;
                        } else {
                            double testFloor = floor / 2.0d;
                            div = testFloor != Math.floor(testFloor) ? ceil : floor;
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid rounding mode: " + mode);
                }
                break;
            case 7:
                if (div2 != Math.floor(div2)) {
                    throw new ArithmeticException("Rounding necessary");
                }
                return number;
        }
        return roundingIncReciprocal == 0.0d ? div * roundingInc : div / roundingIncReciprocal;
    }

    @Override
    public StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition) {
        return format(number, result, fieldPosition, false);
    }

    private StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition, boolean parseAttr) {
        StringBuffer stringBufferSubformat;
        boolean tooBig;
        fieldPosition.setBeginIndex(0);
        fieldPosition.setEndIndex(0);
        if (this.actualRoundingIncrementICU != null) {
            return format(android.icu.math.BigDecimal.valueOf(number), result, fieldPosition);
        }
        boolean isNegative = number < 0;
        if (isNegative) {
            number = -number;
        }
        if (this.multiplier != 1) {
            if (number < 0) {
                long cutoff = Long.MIN_VALUE / ((long) this.multiplier);
                tooBig = number <= cutoff;
            } else {
                long cutoff2 = Long.MAX_VALUE / ((long) this.multiplier);
                tooBig = number > cutoff2;
            }
            if (tooBig) {
                if (isNegative) {
                    number = -number;
                }
                return format(BigInteger.valueOf(number), result, fieldPosition, parseAttr);
            }
        }
        long number2 = number * ((long) this.multiplier);
        synchronized (this.digitList) {
            this.digitList.set(number2, precision(true));
            if (this.digitList.wasRounded() && this.roundingMode == 7) {
                throw new ArithmeticException("Rounding necessary");
            }
            stringBufferSubformat = subformat(number2, result, fieldPosition, isNegative, true, parseAttr);
        }
        return stringBufferSubformat;
    }

    @Override
    public StringBuffer format(BigInteger number, StringBuffer result, FieldPosition fieldPosition) {
        return format(number, result, fieldPosition, false);
    }

    private StringBuffer format(BigInteger number, StringBuffer result, FieldPosition fieldPosition, boolean parseAttr) {
        StringBuffer stringBufferSubformat;
        if (this.actualRoundingIncrementICU != null) {
            return format(new android.icu.math.BigDecimal(number), result, fieldPosition);
        }
        if (this.multiplier != 1) {
            number = number.multiply(BigInteger.valueOf(this.multiplier));
        }
        synchronized (this.digitList) {
            this.digitList.set(number, precision(true));
            if (this.digitList.wasRounded() && this.roundingMode == 7) {
                throw new ArithmeticException("Rounding necessary");
            }
            stringBufferSubformat = subformat(number.intValue(), result, fieldPosition, number.signum() < 0, true, parseAttr);
        }
        return stringBufferSubformat;
    }

    @Override
    public StringBuffer format(BigDecimal number, StringBuffer result, FieldPosition fieldPosition) {
        return format(number, result, fieldPosition, false);
    }

    private StringBuffer format(BigDecimal number, StringBuffer result, FieldPosition fieldPosition, boolean parseAttr) {
        StringBuffer stringBufferSubformat;
        if (this.multiplier != 1) {
            number = number.multiply(BigDecimal.valueOf(this.multiplier));
        }
        if (this.actualRoundingIncrement != null) {
            number = number.divide(this.actualRoundingIncrement, 0, this.roundingMode).multiply(this.actualRoundingIncrement);
        }
        synchronized (this.digitList) {
            DigitList digitList = this.digitList;
            int iPrecision = precision(false);
            boolean z = (this.useExponentialNotation || areSignificantDigitsUsed()) ? false : true;
            digitList.set(number, iPrecision, z);
            if (this.digitList.wasRounded() && this.roundingMode == 7) {
                throw new ArithmeticException("Rounding necessary");
            }
            stringBufferSubformat = subformat(number.doubleValue(), result, fieldPosition, number.signum() < 0, false, parseAttr);
        }
        return stringBufferSubformat;
    }

    @Override
    public StringBuffer format(android.icu.math.BigDecimal number, StringBuffer result, FieldPosition fieldPosition) {
        StringBuffer stringBufferSubformat;
        if (this.multiplier != 1) {
            number = number.multiply(android.icu.math.BigDecimal.valueOf(this.multiplier), this.mathContext);
        }
        if (this.actualRoundingIncrementICU != null) {
            number = number.divide(this.actualRoundingIncrementICU, 0, this.roundingMode).multiply(this.actualRoundingIncrementICU, this.mathContext);
        }
        synchronized (this.digitList) {
            DigitList digitList = this.digitList;
            int iPrecision = precision(false);
            boolean z = (this.useExponentialNotation || areSignificantDigitsUsed()) ? false : true;
            digitList.set(number, iPrecision, z);
            if (this.digitList.wasRounded() && this.roundingMode == 7) {
                throw new ArithmeticException("Rounding necessary");
            }
            stringBufferSubformat = subformat(number.doubleValue(), result, fieldPosition, number.signum() < 0, false, false);
        }
        return stringBufferSubformat;
    }

    private boolean isGroupingPosition(int pos) {
        if (!isGroupingUsed() || pos <= 0 || this.groupingSize <= 0) {
            return false;
        }
        return (this.groupingSize2 <= 0 || pos <= this.groupingSize) ? pos % this.groupingSize == 0 : (pos - this.groupingSize) % this.groupingSize2 == 0;
    }

    private int precision(boolean isIntegral) {
        if (areSignificantDigitsUsed()) {
            return getMaximumSignificantDigits();
        }
        if (this.useExponentialNotation) {
            return getMinimumIntegerDigits() + getMaximumFractionDigits();
        }
        if (isIntegral) {
            return 0;
        }
        return getMaximumFractionDigits();
    }

    private StringBuffer subformat(int number, StringBuffer result, FieldPosition fieldPosition, boolean isNegative, boolean isInteger, boolean parseAttr) {
        if (this.currencySignCount == 3) {
            return subformat(this.currencyPluralInfo.select(getFixedDecimal(number)), result, fieldPosition, isNegative, isInteger, parseAttr);
        }
        return subformat(result, fieldPosition, isNegative, isInteger, parseAttr);
    }

    PluralRules.FixedDecimal getFixedDecimal(double number) {
        return getFixedDecimal(number, this.digitList);
    }

    PluralRules.FixedDecimal getFixedDecimal(double number, DigitList dl) {
        int maxFractionalDigits;
        int minFractionalDigits;
        int fractionalDigitsInDigitList = dl.count - dl.decimalAt;
        if (this.useSignificantDigits) {
            maxFractionalDigits = this.maxSignificantDigits - dl.decimalAt;
            minFractionalDigits = this.minSignificantDigits - dl.decimalAt;
            if (minFractionalDigits < 0) {
                minFractionalDigits = 0;
            }
            if (maxFractionalDigits < 0) {
                maxFractionalDigits = 0;
            }
        } else {
            maxFractionalDigits = getMaximumFractionDigits();
            minFractionalDigits = getMinimumFractionDigits();
        }
        int v = fractionalDigitsInDigitList;
        if (fractionalDigitsInDigitList < minFractionalDigits) {
            v = minFractionalDigits;
        } else if (fractionalDigitsInDigitList > maxFractionalDigits) {
            v = maxFractionalDigits;
        }
        long f = 0;
        if (v > 0) {
            for (int i = Math.max(0, dl.decimalAt); i < dl.count; i++) {
                f = (f * 10) + ((long) (dl.digits[i] - 48));
            }
            for (int i2 = v; i2 < fractionalDigitsInDigitList; i2++) {
                f *= 10;
            }
        }
        return new PluralRules.FixedDecimal(number, v, f);
    }

    private StringBuffer subformat(double number, StringBuffer result, FieldPosition fieldPosition, boolean isNegative, boolean isInteger, boolean parseAttr) {
        if (this.currencySignCount == 3) {
            return subformat(this.currencyPluralInfo.select(getFixedDecimal(number)), result, fieldPosition, isNegative, isInteger, parseAttr);
        }
        return subformat(result, fieldPosition, isNegative, isInteger, parseAttr);
    }

    private StringBuffer subformat(String pluralCount, StringBuffer result, FieldPosition fieldPosition, boolean isNegative, boolean isInteger, boolean parseAttr) {
        if (this.style == 6) {
            String currencyPluralPattern = this.currencyPluralInfo.getCurrencyPluralPattern(pluralCount);
            if (!this.formatPattern.equals(currencyPluralPattern)) {
                applyPatternWithoutExpandAffix(currencyPluralPattern, false);
            }
        }
        expandAffixAdjustWidth(pluralCount);
        return subformat(result, fieldPosition, isNegative, isInteger, parseAttr);
    }

    private StringBuffer subformat(StringBuffer result, FieldPosition fieldPosition, boolean isNegative, boolean isInteger, boolean parseAttr) {
        if (this.digitList.isZero()) {
            this.digitList.decimalAt = 0;
        }
        int prefixLen = appendAffix(result, isNegative, true, fieldPosition, parseAttr);
        if (this.useExponentialNotation) {
            subformatExponential(result, fieldPosition, parseAttr);
        } else {
            subformatFixed(result, fieldPosition, isInteger, parseAttr);
        }
        int suffixLen = appendAffix(result, isNegative, false, fieldPosition, parseAttr);
        addPadding(result, fieldPosition, prefixLen, suffixLen);
        return result;
    }

    private void subformatFixed(StringBuffer result, FieldPosition fieldPosition, boolean isInteger, boolean parseAttr) {
        boolean fractionPresent;
        int digitIndex;
        int digitIndex2;
        char[] digits = this.symbols.getDigitsLocal();
        char grouping = this.currencySignCount == 0 ? this.symbols.getGroupingSeparator() : this.symbols.getMonetaryGroupingSeparator();
        char decimal = this.currencySignCount == 0 ? this.symbols.getDecimalSeparator() : this.symbols.getMonetaryDecimalSeparator();
        boolean useSigDig = areSignificantDigitsUsed();
        int maxIntDig = getMaximumIntegerDigits();
        int minIntDig = getMinimumIntegerDigits();
        int intBegin = result.length();
        if (fieldPosition.getField() == 0 || fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
            fieldPosition.setBeginIndex(result.length());
        }
        long fractionalDigits = 0;
        int fractionalDigitsCount = 0;
        int sigCount = 0;
        int minSigDig = getMinimumSignificantDigits();
        int maxSigDig = getMaximumSignificantDigits();
        if (!useSigDig) {
            minSigDig = 0;
            maxSigDig = Integer.MAX_VALUE;
        }
        int count = useSigDig ? Math.max(1, this.digitList.decimalAt) : minIntDig;
        if (this.digitList.decimalAt > 0 && count < this.digitList.decimalAt) {
            count = this.digitList.decimalAt;
        }
        int digitIndex3 = 0;
        if (count > maxIntDig && maxIntDig >= 0) {
            count = maxIntDig;
            digitIndex3 = this.digitList.decimalAt - maxIntDig;
        }
        int sizeBeforeIntegerPart = result.length();
        int posSinceLastGrouping = result.length();
        int i = count - 1;
        int digitIndex4 = digitIndex3;
        while (i >= 0) {
            if (i < this.digitList.decimalAt && digitIndex4 < this.digitList.count && sigCount < maxSigDig) {
                digitIndex2 = digitIndex4 + 1;
                result.append(digits[this.digitList.getDigitValue(digitIndex4)]);
                sigCount++;
            } else {
                result.append(digits[0]);
                if (sigCount > 0) {
                    sigCount++;
                    digitIndex2 = digitIndex4;
                } else {
                    digitIndex2 = digitIndex4;
                }
            }
            if (isGroupingPosition(i)) {
                if (parseAttr) {
                    addAttribute(NumberFormat.Field.INTEGER, posSinceLastGrouping, result.length());
                }
                result.append(grouping);
                if (parseAttr) {
                    addAttribute(NumberFormat.Field.GROUPING_SEPARATOR, result.length() - 1, result.length());
                }
                if (fieldPosition.getFieldAttribute() == NumberFormat.Field.GROUPING_SEPARATOR && fieldPosition.getEndIndex() == 0) {
                    fieldPosition.setBeginIndex(result.length() - 1);
                    fieldPosition.setEndIndex(result.length());
                }
                posSinceLastGrouping = result.length();
            }
            i--;
            digitIndex4 = digitIndex2;
        }
        if (fieldPosition.getField() == 0 || fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
            fieldPosition.setEndIndex(result.length());
        }
        if (parseAttr) {
            addAttribute(NumberFormat.Field.INTEGER, posSinceLastGrouping, result.length());
        }
        if (sigCount == 0 && this.digitList.count == 0) {
            sigCount = 1;
        }
        if (isInteger || digitIndex4 >= this.digitList.count) {
            fractionPresent = !useSigDig ? getMinimumFractionDigits() <= 0 : sigCount >= minSigDig;
        } else {
            fractionPresent = true;
        }
        if (!fractionPresent && result.length() == sizeBeforeIntegerPart) {
            result.append(digits[0]);
        }
        if (parseAttr) {
            addAttribute(NumberFormat.Field.INTEGER, intBegin, result.length());
        }
        if (this.decimalSeparatorAlwaysShown || fractionPresent) {
            if (fieldPosition.getFieldAttribute() == NumberFormat.Field.DECIMAL_SEPARATOR) {
                fieldPosition.setBeginIndex(result.length());
            }
            result.append(decimal);
            if (fieldPosition.getFieldAttribute() == NumberFormat.Field.DECIMAL_SEPARATOR) {
                fieldPosition.setEndIndex(result.length());
            }
            if (parseAttr) {
                addAttribute(NumberFormat.Field.DECIMAL_SEPARATOR, result.length() - 1, result.length());
            }
        }
        if (fieldPosition.getField() == 1 || fieldPosition.getFieldAttribute() == NumberFormat.Field.FRACTION) {
            fieldPosition.setBeginIndex(result.length());
        }
        int fracBegin = result.length();
        boolean recordFractionDigits = fieldPosition instanceof UFieldPosition;
        int count2 = useSigDig ? Integer.MAX_VALUE : getMaximumFractionDigits();
        if (useSigDig && (sigCount == maxSigDig || (sigCount >= minSigDig && digitIndex4 == this.digitList.count))) {
            count2 = 0;
        }
        int i2 = 0;
        while (i2 < count2) {
            if (!useSigDig && i2 >= getMinimumFractionDigits() && (isInteger || digitIndex4 >= this.digitList.count)) {
                break;
            }
            if ((-1) - i2 > this.digitList.decimalAt - 1) {
                result.append(digits[0]);
                if (recordFractionDigits) {
                    fractionalDigitsCount++;
                    fractionalDigits *= 10;
                }
                digitIndex = digitIndex4;
            } else {
                if (!isInteger && digitIndex4 < this.digitList.count) {
                    digitIndex = digitIndex4 + 1;
                    byte digit = this.digitList.getDigitValue(digitIndex4);
                    result.append(digits[digit]);
                    if (recordFractionDigits) {
                        fractionalDigitsCount++;
                        fractionalDigits = (fractionalDigits * 10) + ((long) digit);
                    }
                } else {
                    result.append(digits[0]);
                    if (recordFractionDigits) {
                        fractionalDigitsCount++;
                        fractionalDigits *= 10;
                        digitIndex = digitIndex4;
                    } else {
                        digitIndex = digitIndex4;
                    }
                }
                sigCount++;
                if (useSigDig && (sigCount == maxSigDig || (digitIndex == this.digitList.count && sigCount >= minSigDig))) {
                    break;
                }
            }
            i2++;
            digitIndex4 = digitIndex;
        }
        if (fieldPosition.getField() == 1 || fieldPosition.getFieldAttribute() == NumberFormat.Field.FRACTION) {
            fieldPosition.setEndIndex(result.length());
        }
        if (recordFractionDigits) {
            ((UFieldPosition) fieldPosition).setFractionDigits(fractionalDigitsCount, fractionalDigits);
        }
        if (parseAttr) {
            if (!this.decimalSeparatorAlwaysShown && !fractionPresent) {
                return;
            }
            addAttribute(NumberFormat.Field.FRACTION, fracBegin, result.length());
        }
    }

    private void subformatExponential(StringBuffer result, FieldPosition fieldPosition, boolean parseAttr) {
        int minFracDig;
        int exponent;
        char[] digits = this.symbols.getDigitsLocal();
        char decimal = this.currencySignCount == 0 ? this.symbols.getDecimalSeparator() : this.symbols.getMonetaryDecimalSeparator();
        boolean useSigDig = areSignificantDigitsUsed();
        int maxIntDig = getMaximumIntegerDigits();
        int minIntDig = getMinimumIntegerDigits();
        if (fieldPosition.getField() == 0) {
            fieldPosition.setBeginIndex(result.length());
            fieldPosition.setEndIndex(-1);
        } else if (fieldPosition.getField() == 1) {
            fieldPosition.setBeginIndex(-1);
        } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
            fieldPosition.setBeginIndex(result.length());
            fieldPosition.setEndIndex(-1);
        } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.FRACTION) {
            fieldPosition.setBeginIndex(-1);
        }
        int intBegin = result.length();
        int intEnd = -1;
        int fracBegin = -1;
        if (useSigDig) {
            minIntDig = 1;
            maxIntDig = 1;
            minFracDig = getMinimumSignificantDigits() - 1;
        } else {
            minFracDig = getMinimumFractionDigits();
            if (maxIntDig > 8) {
                maxIntDig = 1;
                if (1 < minIntDig) {
                    maxIntDig = minIntDig;
                }
            }
            if (maxIntDig > minIntDig) {
                minIntDig = 1;
            }
        }
        long fractionalDigits = 0;
        int fractionalDigitsCount = 0;
        boolean recordFractionDigits = false;
        int exponent2 = this.digitList.decimalAt;
        if (maxIntDig > 1 && maxIntDig != minIntDig) {
            exponent = (exponent2 > 0 ? (exponent2 - 1) / maxIntDig : (exponent2 / maxIntDig) - 1) * maxIntDig;
        } else {
            exponent = exponent2 - ((minIntDig > 0 || minFracDig > 0) ? minIntDig : 1);
        }
        int minimumDigits = minIntDig + minFracDig;
        int integerDigits = this.digitList.isZero() ? minIntDig : this.digitList.decimalAt - exponent;
        int totalDigits = this.digitList.count;
        if (minimumDigits > totalDigits) {
            totalDigits = minimumDigits;
        }
        if (integerDigits > totalDigits) {
            totalDigits = integerDigits;
        }
        int i = 0;
        while (i < totalDigits) {
            if (i == integerDigits) {
                if (fieldPosition.getField() == 0 || fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
                    fieldPosition.setEndIndex(result.length());
                }
                if (parseAttr) {
                    intEnd = result.length();
                    addAttribute(NumberFormat.Field.INTEGER, intBegin, result.length());
                }
                if (fieldPosition.getFieldAttribute() == NumberFormat.Field.DECIMAL_SEPARATOR) {
                    fieldPosition.setBeginIndex(result.length());
                }
                result.append(decimal);
                if (fieldPosition.getFieldAttribute() == NumberFormat.Field.DECIMAL_SEPARATOR) {
                    fieldPosition.setEndIndex(result.length());
                }
                if (parseAttr) {
                    int decimalSeparatorBegin = result.length() - 1;
                    addAttribute(NumberFormat.Field.DECIMAL_SEPARATOR, decimalSeparatorBegin, result.length());
                    fracBegin = result.length();
                }
                if (fieldPosition.getField() == 1 || fieldPosition.getFieldAttribute() == NumberFormat.Field.FRACTION) {
                    fieldPosition.setBeginIndex(result.length());
                }
                recordFractionDigits = fieldPosition instanceof UFieldPosition;
            }
            byte digit = i < this.digitList.count ? this.digitList.getDigitValue(i) : (byte) 0;
            result.append(digits[digit]);
            if (recordFractionDigits) {
                fractionalDigitsCount++;
                fractionalDigits = (fractionalDigits * 10) + ((long) digit);
            }
            i++;
        }
        if (this.digitList.isZero() && totalDigits == 0) {
            result.append(digits[0]);
        }
        if (fieldPosition.getField() == 0) {
            if (fieldPosition.getEndIndex() < 0) {
                fieldPosition.setEndIndex(result.length());
            }
        } else if (fieldPosition.getField() == 1) {
            if (fieldPosition.getBeginIndex() < 0) {
                fieldPosition.setBeginIndex(result.length());
            }
            fieldPosition.setEndIndex(result.length());
        } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
            if (fieldPosition.getEndIndex() < 0) {
                fieldPosition.setEndIndex(result.length());
            }
        } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.FRACTION) {
            if (fieldPosition.getBeginIndex() < 0) {
                fieldPosition.setBeginIndex(result.length());
            }
            fieldPosition.setEndIndex(result.length());
        }
        if (recordFractionDigits) {
            ((UFieldPosition) fieldPosition).setFractionDigits(fractionalDigitsCount, fractionalDigits);
        }
        if (parseAttr) {
            if (intEnd < 0) {
                addAttribute(NumberFormat.Field.INTEGER, intBegin, result.length());
            }
            if (fracBegin > 0) {
                addAttribute(NumberFormat.Field.FRACTION, fracBegin, result.length());
            }
        }
        if (fieldPosition.getFieldAttribute() == NumberFormat.Field.EXPONENT_SYMBOL) {
            fieldPosition.setBeginIndex(result.length());
        }
        result.append(this.symbols.getExponentSeparator());
        if (fieldPosition.getFieldAttribute() == NumberFormat.Field.EXPONENT_SYMBOL) {
            fieldPosition.setEndIndex(result.length());
        }
        if (parseAttr) {
            addAttribute(NumberFormat.Field.EXPONENT_SYMBOL, result.length() - this.symbols.getExponentSeparator().length(), result.length());
        }
        if (this.digitList.isZero()) {
            exponent = 0;
        }
        boolean negativeExponent = exponent < 0;
        if ((negativeExponent || this.exponentSignAlwaysShown) && fieldPosition.getFieldAttribute() == NumberFormat.Field.EXPONENT_SIGN) {
            fieldPosition.setBeginIndex(result.length());
        }
        if (negativeExponent) {
            exponent = -exponent;
            result.append(this.symbols.getMinusString());
            if (parseAttr) {
                addAttribute(NumberFormat.Field.EXPONENT_SIGN, result.length() - 1, result.length());
            }
        } else if (this.exponentSignAlwaysShown) {
            result.append(this.symbols.getPlusString());
            if (parseAttr) {
                int expSignBegin = result.length() - 1;
                addAttribute(NumberFormat.Field.EXPONENT_SIGN, expSignBegin, result.length());
            }
        }
        if ((negativeExponent || this.exponentSignAlwaysShown) && fieldPosition.getFieldAttribute() == NumberFormat.Field.EXPONENT_SIGN) {
            fieldPosition.setEndIndex(result.length());
        }
        int expBegin = result.length();
        this.digitList.set(exponent);
        int expDig = this.minExponentDigits;
        if (this.useExponentialNotation && expDig < 1) {
            expDig = 1;
        }
        for (int i2 = this.digitList.decimalAt; i2 < expDig; i2++) {
            result.append(digits[0]);
        }
        int i3 = 0;
        while (i3 < this.digitList.decimalAt) {
            result.append(i3 < this.digitList.count ? digits[this.digitList.getDigitValue(i3)] : digits[0]);
            i3++;
        }
        if (fieldPosition.getFieldAttribute() == NumberFormat.Field.EXPONENT) {
            fieldPosition.setBeginIndex(expBegin);
            fieldPosition.setEndIndex(result.length());
        }
        if (!parseAttr) {
            return;
        }
        addAttribute(NumberFormat.Field.EXPONENT, expBegin, result.length());
    }

    private final void addPadding(StringBuffer result, FieldPosition fieldPosition, int prefixLen, int suffixLen) {
        int len;
        if (this.formatWidth <= 0 || (len = this.formatWidth - result.length()) <= 0) {
            return;
        }
        char[] padding = new char[len];
        for (int i = 0; i < len; i++) {
            padding[i] = this.pad;
        }
        switch (this.padPosition) {
            case 0:
                result.insert(0, padding);
                break;
            case 1:
                result.insert(prefixLen, padding);
                break;
            case 2:
                result.insert(result.length() - suffixLen, padding);
                break;
            case 3:
                result.append(padding);
                break;
        }
        if (this.padPosition != 0 && this.padPosition != 1) {
            return;
        }
        fieldPosition.setBeginIndex(fieldPosition.getBeginIndex() + len);
        fieldPosition.setEndIndex(fieldPosition.getEndIndex() + len);
    }

    @Override
    public Number parse(String text, ParsePosition parsePosition) {
        return (Number) parse(text, parsePosition, null);
    }

    @Override
    public CurrencyAmount parseCurrency(CharSequence text, ParsePosition pos) {
        Currency[] currency = new Currency[1];
        return (CurrencyAmount) parse(text.toString(), pos, currency);
    }

    private Object parse(String text, ParsePosition parsePosition, Currency[] currency) {
        Number n;
        int backup = parsePosition.getIndex();
        int i = backup;
        if (this.formatWidth > 0 && (this.padPosition == 0 || this.padPosition == 1)) {
            i = skipPadding(text, backup);
        }
        if (text.regionMatches(i, this.symbols.getNaN(), 0, this.symbols.getNaN().length())) {
            int i2 = i + this.symbols.getNaN().length();
            if (this.formatWidth > 0 && (this.padPosition == 2 || this.padPosition == 3)) {
                i2 = skipPadding(text, i2);
            }
            parsePosition.setIndex(i2);
            return new Double(Double.NaN);
        }
        boolean[] status = new boolean[3];
        if (this.currencySignCount != 0) {
            if (!parseForCurrency(text, parsePosition, currency, status)) {
                return null;
            }
        } else if (!subparse(text, parsePosition, this.digitList, status, currency, this.negPrefixPattern, this.negSuffixPattern, this.posPrefixPattern, this.posSuffixPattern, false, 0)) {
            parsePosition.setIndex(backup);
            return null;
        }
        if (status[0]) {
            n = new Double(status[1] ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
        } else if (status[2]) {
            n = status[1] ? new Double("0.0") : new Double("-0.0");
        } else if (!status[1] && this.digitList.isZero()) {
            n = new Double("-0.0");
        } else {
            int mult = this.multiplier;
            while (mult % 10 == 0) {
                DigitList digitList = this.digitList;
                digitList.decimalAt--;
                mult /= 10;
            }
            if (!this.parseBigDecimal && mult == 1 && this.digitList.isIntegral()) {
                if (this.digitList.decimalAt < 12) {
                    long l = 0;
                    if (this.digitList.count > 0) {
                        int nx = 0;
                        while (nx < this.digitList.count) {
                            l = ((10 * l) + ((long) ((char) this.digitList.digits[nx]))) - 48;
                            nx++;
                        }
                        while (true) {
                            int nx2 = nx + 1;
                            if (nx >= this.digitList.decimalAt) {
                                break;
                            }
                            l *= 10;
                            nx = nx2;
                        }
                        if (!status[1]) {
                            l = -l;
                        }
                    }
                    n = Long.valueOf(l);
                } else {
                    BigInteger big = this.digitList.getBigInteger(status[1]);
                    n = big.bitLength() < 64 ? Long.valueOf(big.longValue()) : big;
                }
            } else {
                android.icu.math.BigDecimal big2 = this.digitList.getBigDecimalICU(status[1]);
                n = big2;
                if (mult != 1) {
                    n = big2.divide(android.icu.math.BigDecimal.valueOf(mult), this.mathContext);
                }
            }
        }
        if (currency != null) {
            return new CurrencyAmount(n, currency[0]);
        }
        return n;
    }

    private boolean parseForCurrency(String text, ParsePosition parsePosition, Currency[] currency, boolean[] status) {
        boolean found;
        int origPos = parsePosition.getIndex();
        if (!this.isReadyForParsing) {
            int savedCurrencySignCount = this.currencySignCount;
            setupCurrencyAffixForAllPatterns();
            if (savedCurrencySignCount == 3) {
                applyPatternWithoutExpandAffix(this.formatPattern, false);
            } else {
                applyPattern(this.formatPattern, false);
            }
            this.isReadyForParsing = true;
        }
        int maxPosIndex = origPos;
        int maxErrorPos = -1;
        boolean[] savedStatus = null;
        boolean[] tmpStatus = new boolean[3];
        ParsePosition tmpPos = new ParsePosition(origPos);
        DigitList tmpDigitList = new DigitList();
        if (this.style == 6) {
            found = subparse(text, tmpPos, tmpDigitList, tmpStatus, currency, this.negPrefixPattern, this.negSuffixPattern, this.posPrefixPattern, this.posSuffixPattern, true, 1);
        } else {
            found = subparse(text, tmpPos, tmpDigitList, tmpStatus, currency, this.negPrefixPattern, this.negSuffixPattern, this.posPrefixPattern, this.posSuffixPattern, true, 0);
        }
        if (found) {
            if (tmpPos.getIndex() > origPos) {
                maxPosIndex = tmpPos.getIndex();
                savedStatus = tmpStatus;
                this.digitList = tmpDigitList;
            }
        } else {
            maxErrorPos = tmpPos.getErrorIndex();
        }
        for (AffixForCurrency affix : this.affixPatternsForCurrency) {
            boolean[] tmpStatus2 = new boolean[3];
            ParsePosition tmpPos2 = new ParsePosition(origPos);
            DigitList tmpDigitList2 = new DigitList();
            boolean result = subparse(text, tmpPos2, tmpDigitList2, tmpStatus2, currency, affix.getNegPrefix(), affix.getNegSuffix(), affix.getPosPrefix(), affix.getPosSuffix(), true, affix.getPatternType());
            if (result) {
                found = true;
                if (tmpPos2.getIndex() > maxPosIndex) {
                    maxPosIndex = tmpPos2.getIndex();
                    savedStatus = tmpStatus2;
                    this.digitList = tmpDigitList2;
                }
            } else if (tmpPos2.getErrorIndex() > maxErrorPos) {
                maxErrorPos = tmpPos2.getErrorIndex();
            }
        }
        boolean[] tmpStatus3 = new boolean[3];
        ParsePosition tmpPos3 = new ParsePosition(origPos);
        DigitList tmpDigitList3 = new DigitList();
        boolean result2 = subparse(text, tmpPos3, tmpDigitList3, tmpStatus3, currency, this.negativePrefix, this.negativeSuffix, this.positivePrefix, this.positiveSuffix, false, 0);
        if (result2) {
            if (tmpPos3.getIndex() > maxPosIndex) {
                maxPosIndex = tmpPos3.getIndex();
                savedStatus = tmpStatus3;
                this.digitList = tmpDigitList3;
            }
            found = true;
        } else if (tmpPos3.getErrorIndex() > maxErrorPos) {
            maxErrorPos = tmpPos3.getErrorIndex();
        }
        if (!found) {
            parsePosition.setErrorIndex(maxErrorPos);
        } else {
            parsePosition.setIndex(maxPosIndex);
            parsePosition.setErrorIndex(-1);
            for (int index = 0; index < 3; index++) {
                status[index] = savedStatus[index];
            }
        }
        return found;
    }

    private void setupCurrencyAffixForAllPatterns() {
        if (this.currencyPluralInfo == null) {
            this.currencyPluralInfo = new CurrencyPluralInfo(this.symbols.getULocale());
        }
        this.affixPatternsForCurrency = new HashSet();
        String savedFormatPattern = this.formatPattern;
        applyPatternWithoutExpandAffix(getPattern(this.symbols.getULocale(), 1), false);
        AffixForCurrency affixes = new AffixForCurrency(this.negPrefixPattern, this.negSuffixPattern, this.posPrefixPattern, this.posSuffixPattern, 0);
        this.affixPatternsForCurrency.add(affixes);
        Iterator<String> iter = this.currencyPluralInfo.pluralPatternIterator();
        Set<String> currencyUnitPatternSet = new HashSet<>();
        while (iter.hasNext()) {
            String pluralCount = iter.next();
            String currencyPattern = this.currencyPluralInfo.getCurrencyPluralPattern(pluralCount);
            if (currencyPattern != null && !currencyUnitPatternSet.contains(currencyPattern)) {
                currencyUnitPatternSet.add(currencyPattern);
                applyPatternWithoutExpandAffix(currencyPattern, false);
                AffixForCurrency affixes2 = new AffixForCurrency(this.negPrefixPattern, this.negSuffixPattern, this.posPrefixPattern, this.posSuffixPattern, 1);
                this.affixPatternsForCurrency.add(affixes2);
            }
        }
        this.formatPattern = savedFormatPattern;
    }

    private final boolean subparse(String text, ParsePosition parsePosition, DigitList digits, boolean[] status, Currency[] currency, String negPrefix, String negSuffix, String posPrefix, String posSuffix, boolean parseComplexCurrency, int type) {
        int position;
        UnicodeSet groupEquiv;
        int position2 = parsePosition.getIndex();
        int oldStart = parsePosition.getIndex();
        if (this.formatWidth > 0 && this.padPosition == 0) {
            position2 = skipPadding(text, position2);
        }
        int posMatch = compareAffix(text, position2, false, true, posPrefix, parseComplexCurrency, type, currency);
        int negMatch = compareAffix(text, position2, true, true, negPrefix, parseComplexCurrency, type, currency);
        if (posMatch >= 0 && negMatch >= 0) {
            if (posMatch > negMatch) {
                negMatch = -1;
            } else if (negMatch > posMatch) {
                posMatch = -1;
            }
        }
        if (posMatch >= 0) {
            position = position2 + posMatch;
        } else if (negMatch >= 0) {
            position = position2 + negMatch;
        } else {
            parsePosition.setErrorIndex(position2);
            return false;
        }
        if (this.formatWidth > 0 && this.padPosition == 1) {
            position = skipPadding(text, position);
        }
        status[0] = false;
        if (text.regionMatches(position, this.symbols.getInfinity(), 0, this.symbols.getInfinity().length())) {
            position += this.symbols.getInfinity().length();
            status[0] = true;
        } else {
            digits.count = 0;
            digits.decimalAt = 0;
            char[] digitSymbols = this.symbols.getDigitsLocal();
            char decimal = this.currencySignCount == 0 ? this.symbols.getDecimalSeparator() : this.symbols.getMonetaryDecimalSeparator();
            char grouping = this.currencySignCount == 0 ? this.symbols.getGroupingSeparator() : this.symbols.getMonetaryGroupingSeparator();
            String exponentSep = this.symbols.getExponentSeparator();
            boolean sawDecimal = false;
            boolean sawGrouping = false;
            boolean sawDigit = false;
            long exponent = 0;
            boolean strictParse = isParseStrict();
            boolean strictFail = false;
            int lastGroup = -1;
            int digitStart = position;
            int gs2 = this.groupingSize2 == 0 ? this.groupingSize : this.groupingSize2;
            UnicodeSet decimalEquiv = skipExtendedSeparatorParsing ? UnicodeSet.EMPTY : getEquivalentDecimals(decimal, strictParse);
            if (skipExtendedSeparatorParsing) {
                groupEquiv = UnicodeSet.EMPTY;
            } else {
                groupEquiv = strictParse ? strictDefaultGroupingSeparators : defaultGroupingSeparators;
            }
            int digitCount = 0;
            int backup = -1;
            while (position < text.length()) {
                int ch = UTF16.charAt(text, position);
                int digit = ch - digitSymbols[0];
                if (digit < 0 || digit > 9) {
                    digit = UCharacter.digit(ch, 10);
                }
                if (digit < 0 || digit > 9) {
                    digit = 0;
                    while (digit < 10 && ch != digitSymbols[digit]) {
                        digit++;
                    }
                }
                if (digit == 0) {
                    if (strictParse && backup != -1) {
                        if ((lastGroup != -1 && countCodePoints(text, lastGroup, backup) - 1 != gs2) || (lastGroup == -1 && countCodePoints(text, digitStart, position) - 1 > gs2)) {
                            strictFail = true;
                            break;
                        }
                        lastGroup = backup;
                    }
                    backup = -1;
                    sawDigit = true;
                    if (digits.count == 0) {
                        if (sawDecimal) {
                            digits.decimalAt--;
                        }
                    } else {
                        digitCount++;
                        digits.append((char) (digit + 48));
                    }
                    position += UTF16.getCharCount(ch);
                } else if (digit > 0 && digit <= 9) {
                    if (strictParse && backup != -1) {
                        if ((lastGroup != -1 && countCodePoints(text, lastGroup, backup) - 1 != gs2) || (lastGroup == -1 && countCodePoints(text, digitStart, position) - 1 > gs2)) {
                            strictFail = true;
                            break;
                        }
                        lastGroup = backup;
                    }
                    sawDigit = true;
                    digitCount++;
                    digits.append((char) (digit + 48));
                    backup = -1;
                    position += UTF16.getCharCount(ch);
                } else if (ch == decimal) {
                    if (strictParse && (backup != -1 || (lastGroup != -1 && countCodePoints(text, lastGroup, position) != this.groupingSize + 1))) {
                        strictFail = true;
                        break;
                    }
                    if (isParseIntegerOnly() || sawDecimal) {
                        break;
                    }
                    digits.decimalAt = digitCount;
                    sawDecimal = true;
                    position += UTF16.getCharCount(ch);
                } else if (isGroupingUsed() && ch == grouping) {
                    if (!sawDecimal) {
                        if (strictParse && (!sawDigit || backup != -1)) {
                            strictFail = true;
                            break;
                        }
                        backup = position;
                        sawGrouping = true;
                        position += UTF16.getCharCount(ch);
                    } else {
                        break;
                    }
                } else if (!sawDecimal && decimalEquiv.contains(ch)) {
                    if (strictParse && (backup != -1 || (lastGroup != -1 && countCodePoints(text, lastGroup, position) != this.groupingSize + 1))) {
                        strictFail = true;
                        break;
                    }
                    if (isParseIntegerOnly()) {
                        break;
                    }
                    digits.decimalAt = digitCount;
                    decimal = (char) ch;
                    sawDecimal = true;
                    position += UTF16.getCharCount(ch);
                } else if (isGroupingUsed() && !sawGrouping && groupEquiv.contains(ch)) {
                    if (!sawDecimal) {
                        if (strictParse && (!sawDigit || backup != -1)) {
                            strictFail = true;
                            break;
                        }
                        grouping = (char) ch;
                        backup = position;
                        sawGrouping = true;
                        position += UTF16.getCharCount(ch);
                    } else {
                        break;
                    }
                } else if (0 == 0 && text.regionMatches(true, position, exponentSep, 0, exponentSep.length())) {
                    boolean negExp = false;
                    int pos = position + exponentSep.length();
                    if (pos < text.length()) {
                        int ch2 = UTF16.charAt(text, pos);
                        if (ch2 == this.symbols.getPlusSign()) {
                            pos++;
                        } else if (ch2 == this.symbols.getMinusSign()) {
                            pos++;
                            negExp = true;
                        }
                    }
                    DigitList exponentDigits = new DigitList();
                    exponentDigits.count = 0;
                    while (pos < text.length()) {
                        int digit2 = UTF16.charAt(text, pos) - digitSymbols[0];
                        if (digit2 < 0 || digit2 > 9) {
                            digit2 = UCharacter.digit(UTF16.charAt(text, pos), 10);
                        }
                        if (digit2 < 0 || digit2 > 9) {
                            break;
                        }
                        exponentDigits.append((char) (digit2 + 48));
                        pos += UTF16.getCharCount(UTF16.charAt(text, pos));
                    }
                    if (exponentDigits.count > 0) {
                        if (strictParse && (backup != -1 || lastGroup != -1)) {
                            strictFail = true;
                        } else {
                            if (exponentDigits.count > 10) {
                                if (negExp) {
                                    status[2] = true;
                                } else {
                                    status[0] = true;
                                }
                            } else {
                                exponentDigits.decimalAt = exponentDigits.count;
                                exponent = exponentDigits.getLong();
                                if (negExp) {
                                    exponent = -exponent;
                                }
                            }
                            position = pos;
                        }
                    }
                }
            }
            if (digits.decimalAt == 0 && isDecimalPatternMatchRequired() && this.formatPattern.indexOf(decimal) != -1) {
                parsePosition.setIndex(oldStart);
                parsePosition.setErrorIndex(position);
                return false;
            }
            if (backup != -1) {
                position = backup;
            }
            if (!sawDecimal) {
                digits.decimalAt = digitCount;
            }
            if (strictParse && !sawDecimal && lastGroup != -1 && countCodePoints(text, lastGroup, position) != this.groupingSize + 1) {
                strictFail = true;
            }
            if (strictFail) {
                parsePosition.setIndex(oldStart);
                parsePosition.setErrorIndex(position);
                return false;
            }
            long exponent2 = exponent + ((long) digits.decimalAt);
            if (exponent2 < (-getParseMaxDigits())) {
                status[2] = true;
            } else if (exponent2 > getParseMaxDigits()) {
                status[0] = true;
            } else {
                digits.decimalAt = (int) exponent2;
            }
            if (!sawDigit && digitCount == 0) {
                parsePosition.setIndex(oldStart);
                parsePosition.setErrorIndex(oldStart);
                return false;
            }
        }
        if (this.formatWidth > 0 && this.padPosition == 2) {
            position = skipPadding(text, position);
        }
        if (posMatch >= 0) {
            posMatch = compareAffix(text, position, false, false, posSuffix, parseComplexCurrency, type, currency);
        }
        if (negMatch >= 0) {
            negMatch = compareAffix(text, position, true, false, negSuffix, parseComplexCurrency, type, currency);
        }
        if (posMatch >= 0 && negMatch >= 0) {
            if (posMatch > negMatch) {
                negMatch = -1;
            } else if (negMatch > posMatch) {
                posMatch = -1;
            }
        }
        if ((posMatch >= 0) == (negMatch >= 0)) {
            parsePosition.setErrorIndex(position);
            return false;
        }
        if (posMatch >= 0) {
            negMatch = posMatch;
        }
        int position3 = position + negMatch;
        if (this.formatWidth > 0 && this.padPosition == 3) {
            position3 = skipPadding(text, position3);
        }
        parsePosition.setIndex(position3);
        status[1] = posMatch >= 0;
        if (parsePosition.getIndex() == oldStart) {
            parsePosition.setErrorIndex(position3);
            return false;
        }
        return true;
    }

    private int countCodePoints(String str, int start, int end) {
        int count = 0;
        int index = start;
        while (index < end) {
            count++;
            index += UTF16.getCharCount(UTF16.charAt(str, index));
        }
        return count;
    }

    private UnicodeSet getEquivalentDecimals(char decimal, boolean strictParse) {
        UnicodeSet equivSet = UnicodeSet.EMPTY;
        if (strictParse) {
            if (strictDotEquivalents.contains(decimal)) {
                return strictDotEquivalents;
            }
            if (strictCommaEquivalents.contains(decimal)) {
                return strictCommaEquivalents;
            }
            return equivSet;
        }
        if (dotEquivalents.contains(decimal)) {
            return dotEquivalents;
        }
        if (commaEquivalents.contains(decimal)) {
            return commaEquivalents;
        }
        return equivSet;
    }

    private final int skipPadding(String text, int position) {
        while (position < text.length() && text.charAt(position) == this.pad) {
            position++;
        }
        return position;
    }

    private int compareAffix(String text, int pos, boolean isNegative, boolean isPrefix, String affixPat, boolean complexCurrencyParsing, int type, Currency[] currency) {
        if (currency != null || this.currencyChoice != null || (this.currencySignCount != 0 && complexCurrencyParsing)) {
            return compareComplexAffix(affixPat, text, pos, type, currency);
        }
        if (isPrefix) {
            return compareSimpleAffix(isNegative ? this.negativePrefix : this.positivePrefix, text, pos);
        }
        return compareSimpleAffix(isNegative ? this.negativeSuffix : this.positiveSuffix, text, pos);
    }

    private static boolean isBidiMark(int c) {
        return c == 8206 || c == 8207 || c == 1564;
    }

    private static String trimMarksFromAffix(String affix) {
        boolean hasBidiMark = false;
        int idx = 0;
        while (true) {
            if (idx >= affix.length()) {
                break;
            }
            if (!isBidiMark(affix.charAt(idx))) {
                idx++;
            } else {
                hasBidiMark = true;
                break;
            }
        }
        if (!hasBidiMark) {
            return affix;
        }
        StringBuilder buf = new StringBuilder();
        buf.append((CharSequence) affix, 0, idx);
        while (true) {
            idx++;
            if (idx < affix.length()) {
                char c = affix.charAt(idx);
                if (!isBidiMark(c)) {
                    buf.append(c);
                }
            } else {
                return buf.toString();
            }
        }
    }

    private static int compareSimpleAffix(String affix, String input, int pos) {
        String trimmedAffix = affix.length() > 1 ? trimMarksFromAffix(affix) : affix;
        int i = 0;
        while (i < trimmedAffix.length()) {
            int c = UTF16.charAt(trimmedAffix, i);
            int len = UTF16.getCharCount(c);
            if (PatternProps.isWhiteSpace(c)) {
                boolean literalMatch = false;
                while (pos < input.length()) {
                    int ic = UTF16.charAt(input, pos);
                    if (ic == c) {
                        literalMatch = true;
                        i += len;
                        pos += len;
                        if (i == trimmedAffix.length()) {
                            break;
                        }
                        c = UTF16.charAt(trimmedAffix, i);
                        len = UTF16.getCharCount(c);
                        if (!PatternProps.isWhiteSpace(c)) {
                            break;
                        }
                    } else {
                        if (!isBidiMark(ic)) {
                            break;
                        }
                        pos++;
                    }
                }
                int i2 = skipPatternWhiteSpace(trimmedAffix, i);
                int s = pos;
                pos = skipUWhiteSpace(input, pos);
                if (pos == s && !literalMatch) {
                    return -1;
                }
                i = skipUWhiteSpace(trimmedAffix, i2);
            } else {
                boolean match = false;
                while (pos < input.length()) {
                    int ic2 = UTF16.charAt(input, pos);
                    if (!match && equalWithSignCompatibility(ic2, c)) {
                        i += len;
                        pos += len;
                        match = true;
                    } else {
                        if (!isBidiMark(ic2)) {
                            break;
                        }
                        pos++;
                    }
                }
                if (!match) {
                    return -1;
                }
            }
        }
        return pos - pos;
    }

    private static boolean equalWithSignCompatibility(int lhs, int rhs) {
        if (lhs == rhs || (minusSigns.contains(lhs) && minusSigns.contains(rhs))) {
            return true;
        }
        if (plusSigns.contains(lhs)) {
            return plusSigns.contains(rhs);
        }
        return false;
    }

    private static int skipPatternWhiteSpace(String text, int pos) {
        while (pos < text.length()) {
            int c = UTF16.charAt(text, pos);
            if (!PatternProps.isWhiteSpace(c)) {
                break;
            }
            pos += UTF16.getCharCount(c);
        }
        return pos;
    }

    private static int skipUWhiteSpace(String text, int pos) {
        while (pos < text.length()) {
            int c = UTF16.charAt(text, pos);
            if (!UCharacter.isUWhiteSpace(c)) {
                break;
            }
            pos += UTF16.getCharCount(c);
        }
        return pos;
    }

    private static int skipBidiMarks(String text, int pos) {
        while (pos < text.length()) {
            int c = UTF16.charAt(text, pos);
            if (!isBidiMark(c)) {
                break;
            }
            pos += UTF16.getCharCount(c);
        }
        return pos;
    }

    private int compareComplexAffix(String affixPat, String text, int pos, int type, Currency[] currency) {
        int i = 0;
        while (i < affixPat.length() && pos >= 0) {
            int i2 = i + 1;
            char c = affixPat.charAt(i);
            if (c == '\'') {
                int i3 = i2;
                while (true) {
                    int j = affixPat.indexOf(39, i3);
                    if (j == i3) {
                        pos = match(text, pos, 39);
                        i = j + 1;
                    } else if (j > i3) {
                        pos = match(text, pos, affixPat.substring(i3, j));
                        i = j + 1;
                        if (i >= affixPat.length() || affixPat.charAt(i) != '\'') {
                            break;
                        }
                        pos = match(text, pos, 39);
                        i3 = i + 1;
                    } else {
                        throw new RuntimeException();
                    }
                }
            } else {
                switch (c) {
                    case '%':
                        c = this.symbols.getPercent();
                        break;
                    case '-':
                        c = this.symbols.getMinusSign();
                        break;
                    case 164:
                        boolean intl = i2 < affixPat.length() && affixPat.charAt(i2) == 164;
                        i = intl ? i2 + 1 : i2;
                        boolean plural = i < affixPat.length() && affixPat.charAt(i) == 164;
                        if (plural) {
                            i++;
                        }
                        ULocale uloc = getLocale(ULocale.VALID_LOCALE);
                        if (uloc == null) {
                            uloc = this.symbols.getLocale(ULocale.VALID_LOCALE);
                        }
                        ParsePosition ppos = new ParsePosition(pos);
                        String iso = Currency.parse(uloc, text, type, ppos);
                        if (iso != null) {
                            if (currency != null) {
                                currency[0] = Currency.getInstance(iso);
                            } else {
                                Currency effectiveCurr = getEffectiveCurrency();
                                if (iso.compareTo(effectiveCurr.getCurrencyCode()) != 0) {
                                    pos = -1;
                                    break;
                                }
                            }
                            pos = ppos.getIndex();
                        } else {
                            pos = -1;
                            continue;
                        }
                        break;
                    case 8240:
                        c = this.symbols.getPerMill();
                        break;
                }
                pos = match(text, pos, c);
                i = PatternProps.isWhiteSpace(c) ? skipPatternWhiteSpace(affixPat, i2) : i2;
            }
        }
        return pos - pos;
    }

    static final int match(String text, int pos, int ch) {
        if (pos < 0 || pos >= text.length()) {
            return -1;
        }
        int pos2 = skipBidiMarks(text, pos);
        if (PatternProps.isWhiteSpace(ch)) {
            int pos3 = skipPatternWhiteSpace(text, pos2);
            if (pos3 == pos2) {
                return -1;
            }
            return pos3;
        }
        if (pos2 >= text.length() || UTF16.charAt(text, pos2) != ch) {
            return -1;
        }
        return skipBidiMarks(text, UTF16.getCharCount(ch) + pos2);
    }

    static final int match(String text, int pos, String str) {
        int i = 0;
        while (i < str.length() && pos >= 0) {
            int ch = UTF16.charAt(str, i);
            i += UTF16.getCharCount(ch);
            pos = match(text, pos, ch);
            if (PatternProps.isWhiteSpace(ch)) {
                i = skipPatternWhiteSpace(str, i);
            }
        }
        return pos;
    }

    public DecimalFormatSymbols getDecimalFormatSymbols() {
        try {
            return (DecimalFormatSymbols) this.symbols.clone();
        } catch (Exception e) {
            return null;
        }
    }

    public void setDecimalFormatSymbols(DecimalFormatSymbols newSymbols) {
        this.symbols = (DecimalFormatSymbols) newSymbols.clone();
        setCurrencyForSymbols();
        expandAffixes(null);
    }

    private void setCurrencyForSymbols() {
        DecimalFormatSymbols def = new DecimalFormatSymbols(this.symbols.getULocale());
        if (this.symbols.getCurrencySymbol().equals(def.getCurrencySymbol()) && this.symbols.getInternationalCurrencySymbol().equals(def.getInternationalCurrencySymbol())) {
            setCurrency(Currency.getInstance(this.symbols.getULocale()));
        } else {
            setCurrency(null);
        }
    }

    public String getPositivePrefix() {
        return this.positivePrefix;
    }

    public void setPositivePrefix(String newValue) {
        this.positivePrefix = newValue;
        this.posPrefixPattern = null;
    }

    public String getNegativePrefix() {
        return this.negativePrefix;
    }

    public void setNegativePrefix(String newValue) {
        this.negativePrefix = newValue;
        this.negPrefixPattern = null;
    }

    public String getPositiveSuffix() {
        return this.positiveSuffix;
    }

    public void setPositiveSuffix(String newValue) {
        this.positiveSuffix = newValue;
        this.posSuffixPattern = null;
    }

    public String getNegativeSuffix() {
        return this.negativeSuffix;
    }

    public void setNegativeSuffix(String newValue) {
        this.negativeSuffix = newValue;
        this.negSuffixPattern = null;
    }

    public int getMultiplier() {
        return this.multiplier;
    }

    public void setMultiplier(int newValue) {
        if (newValue == 0) {
            throw new IllegalArgumentException("Bad multiplier: " + newValue);
        }
        this.multiplier = newValue;
    }

    public BigDecimal getRoundingIncrement() {
        if (this.roundingIncrementICU == null) {
            return null;
        }
        return this.roundingIncrementICU.toBigDecimal();
    }

    public void setRoundingIncrement(BigDecimal newValue) {
        if (newValue == null) {
            setRoundingIncrement((android.icu.math.BigDecimal) null);
        } else {
            setRoundingIncrement(new android.icu.math.BigDecimal(newValue));
        }
    }

    public void setRoundingIncrement(android.icu.math.BigDecimal newValue) {
        int i = newValue != null ? newValue.compareTo(android.icu.math.BigDecimal.ZERO) : 0;
        if (i < 0) {
            throw new IllegalArgumentException("Illegal rounding increment");
        }
        if (i == 0) {
            setInternalRoundingIncrement(null);
        } else {
            setInternalRoundingIncrement(newValue);
        }
        resetActualRounding();
    }

    public void setRoundingIncrement(double newValue) {
        if (newValue < 0.0d) {
            throw new IllegalArgumentException("Illegal rounding increment");
        }
        if (newValue == 0.0d) {
            setInternalRoundingIncrement((android.icu.math.BigDecimal) null);
        } else {
            setInternalRoundingIncrement(android.icu.math.BigDecimal.valueOf(newValue));
        }
        resetActualRounding();
    }

    @Override
    public int getRoundingMode() {
        return this.roundingMode;
    }

    @Override
    public void setRoundingMode(int roundingMode) {
        if (roundingMode < 0 || roundingMode > 7) {
            throw new IllegalArgumentException("Invalid rounding mode: " + roundingMode);
        }
        this.roundingMode = roundingMode;
        resetActualRounding();
    }

    public int getFormatWidth() {
        return this.formatWidth;
    }

    public void setFormatWidth(int width) {
        if (width < 0) {
            throw new IllegalArgumentException("Illegal format width");
        }
        this.formatWidth = width;
    }

    public char getPadCharacter() {
        return this.pad;
    }

    public void setPadCharacter(char padChar) {
        this.pad = padChar;
    }

    public int getPadPosition() {
        return this.padPosition;
    }

    public void setPadPosition(int padPos) {
        if (padPos < 0 || padPos > 3) {
            throw new IllegalArgumentException("Illegal pad position");
        }
        this.padPosition = padPos;
    }

    public boolean isScientificNotation() {
        return this.useExponentialNotation;
    }

    public void setScientificNotation(boolean useScientific) {
        this.useExponentialNotation = useScientific;
    }

    public byte getMinimumExponentDigits() {
        return this.minExponentDigits;
    }

    public void setMinimumExponentDigits(byte minExpDig) {
        if (minExpDig < 1) {
            throw new IllegalArgumentException("Exponent digits must be >= 1");
        }
        this.minExponentDigits = minExpDig;
    }

    public boolean isExponentSignAlwaysShown() {
        return this.exponentSignAlwaysShown;
    }

    public void setExponentSignAlwaysShown(boolean expSignAlways) {
        this.exponentSignAlwaysShown = expSignAlways;
    }

    public int getGroupingSize() {
        return this.groupingSize;
    }

    public void setGroupingSize(int newValue) {
        this.groupingSize = (byte) newValue;
    }

    public int getSecondaryGroupingSize() {
        return this.groupingSize2;
    }

    public void setSecondaryGroupingSize(int newValue) {
        this.groupingSize2 = (byte) newValue;
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

    public void setMathContextICU(MathContext newValue) {
        this.mathContext = newValue;
    }

    public void setMathContext(java.math.MathContext newValue) {
        this.mathContext = new MathContext(newValue.getPrecision(), 1, false, newValue.getRoundingMode().ordinal());
    }

    public boolean isDecimalSeparatorAlwaysShown() {
        return this.decimalSeparatorAlwaysShown;
    }

    public void setDecimalPatternMatchRequired(boolean value) {
        this.parseRequireDecimalPoint = value;
    }

    public boolean isDecimalPatternMatchRequired() {
        return this.parseRequireDecimalPoint;
    }

    public void setDecimalSeparatorAlwaysShown(boolean newValue) {
        this.decimalSeparatorAlwaysShown = newValue;
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

    public void setCurrencyPluralInfo(CurrencyPluralInfo newInfo) {
        this.currencyPluralInfo = (CurrencyPluralInfo) newInfo.clone();
        this.isReadyForParsing = false;
    }

    @Override
    public Object clone() {
        try {
            DecimalFormat other = (DecimalFormat) super.clone();
            other.symbols = (DecimalFormatSymbols) this.symbols.clone();
            other.digitList = new DigitList();
            if (this.currencyPluralInfo != null) {
                other.currencyPluralInfo = (CurrencyPluralInfo) this.currencyPluralInfo.clone();
            }
            other.attributes = new ArrayList<>();
            other.currencyUsage = this.currencyUsage;
            return other;
        } catch (Exception e) {
            throw new IllegalStateException();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !super.equals(obj)) {
            return false;
        }
        DecimalFormat other = (DecimalFormat) obj;
        if (this.currencySignCount != other.currencySignCount) {
            return false;
        }
        if ((this.style == 6 && (!equals(this.posPrefixPattern, other.posPrefixPattern) || !equals(this.posSuffixPattern, other.posSuffixPattern) || !equals(this.negPrefixPattern, other.negPrefixPattern) || !equals(this.negSuffixPattern, other.negSuffixPattern))) || this.multiplier != other.multiplier || this.groupingSize != other.groupingSize || this.groupingSize2 != other.groupingSize2 || this.decimalSeparatorAlwaysShown != other.decimalSeparatorAlwaysShown || this.useExponentialNotation != other.useExponentialNotation) {
            return false;
        }
        if ((this.useExponentialNotation && this.minExponentDigits != other.minExponentDigits) || this.useSignificantDigits != other.useSignificantDigits) {
            return false;
        }
        if ((!this.useSignificantDigits || (this.minSignificantDigits == other.minSignificantDigits && this.maxSignificantDigits == other.maxSignificantDigits)) && this.symbols.equals(other.symbols) && Utility.objectEquals(this.currencyPluralInfo, other.currencyPluralInfo)) {
            return this.currencyUsage.equals(other.currencyUsage);
        }
        return false;
    }

    private boolean equals(String pat1, String pat2) {
        if (pat1 == null || pat2 == null) {
            return pat1 == null && pat2 == null;
        }
        if (pat1.equals(pat2)) {
            return true;
        }
        return unquote(pat1).equals(unquote(pat2));
    }

    private String unquote(String pat) {
        StringBuilder buf = new StringBuilder(pat.length());
        int i = 0;
        while (i < pat.length()) {
            int i2 = i + 1;
            char ch = pat.charAt(i);
            if (ch != '\'') {
                buf.append(ch);
            }
            i = i2;
        }
        return buf.toString();
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

    private void expandAffixes(String pluralCount) {
        this.currencyChoice = null;
        StringBuffer buffer = new StringBuffer();
        if (this.posPrefixPattern != null) {
            expandAffix(this.posPrefixPattern, pluralCount, buffer, false);
            this.positivePrefix = buffer.toString();
        }
        if (this.posSuffixPattern != null) {
            expandAffix(this.posSuffixPattern, pluralCount, buffer, false);
            this.positiveSuffix = buffer.toString();
        }
        if (this.negPrefixPattern != null) {
            expandAffix(this.negPrefixPattern, pluralCount, buffer, false);
            this.negativePrefix = buffer.toString();
        }
        if (this.negSuffixPattern == null) {
            return;
        }
        expandAffix(this.negSuffixPattern, pluralCount, buffer, false);
        this.negativeSuffix = buffer.toString();
    }

    private void expandAffix(String pattern, String pluralCount, StringBuffer buffer, boolean doFormat) {
        String s;
        buffer.setLength(0);
        int i = 0;
        while (i < pattern.length()) {
            int i2 = i + 1;
            char c = pattern.charAt(i);
            if (c == '\'') {
                int i3 = i2;
                while (true) {
                    int j = pattern.indexOf(39, i3);
                    if (j == i3) {
                        buffer.append('\'');
                        i = j + 1;
                    } else if (j > i3) {
                        buffer.append(pattern.substring(i3, j));
                        i = j + 1;
                        if (i >= pattern.length() || pattern.charAt(i) != '\'') {
                            break;
                        }
                        buffer.append('\'');
                        i3 = i + 1;
                    } else {
                        throw new RuntimeException();
                    }
                }
            } else {
                switch (c) {
                    case '%':
                        c = this.symbols.getPercent();
                        break;
                    case '-':
                        String minusString = this.symbols.getMinusString();
                        buffer.append(minusString);
                        i = i2;
                        continue;
                    case 164:
                        boolean intl = i2 < pattern.length() && pattern.charAt(i2) == 164;
                        boolean plural = false;
                        if (intl) {
                            i = i2 + 1;
                            if (i < pattern.length() && pattern.charAt(i) == 164) {
                                plural = true;
                                intl = false;
                                i++;
                            }
                        } else {
                            i = i2;
                        }
                        Currency currency = getCurrency();
                        if (currency != null) {
                            if (plural && pluralCount != null) {
                                s = currency.getName(this.symbols.getULocale(), 2, pluralCount, new boolean[1]);
                            } else if (!intl) {
                                boolean[] isChoiceFormat = new boolean[1];
                                s = currency.getName(this.symbols.getULocale(), 0, isChoiceFormat);
                                if (isChoiceFormat[0]) {
                                    if (!doFormat) {
                                        if (this.currencyChoice == null) {
                                            this.currencyChoice = new ChoiceFormat(s);
                                        }
                                        s = String.valueOf(CURRENCY_SIGN);
                                    } else {
                                        FieldPosition pos = new FieldPosition(0);
                                        this.currencyChoice.format(this.digitList.getDouble(), buffer, pos);
                                    }
                                    break;
                                }
                            } else {
                                s = currency.getCurrencyCode();
                            }
                        } else {
                            s = intl ? this.symbols.getInternationalCurrencySymbol() : this.symbols.getCurrencySymbol();
                        }
                        buffer.append(s);
                        continue;
                    case 8240:
                        c = this.symbols.getPerMill();
                        break;
                }
                buffer.append(c);
                i = i2;
            }
        }
    }

    private int appendAffix(StringBuffer buf, boolean isNegative, boolean isPrefix, FieldPosition fieldPosition, boolean parseAttr) {
        String affix;
        String pattern;
        String affixPat;
        if (this.currencyChoice != null) {
            if (isPrefix) {
                affixPat = isNegative ? this.negPrefixPattern : this.posPrefixPattern;
            } else {
                affixPat = isNegative ? this.negSuffixPattern : this.posSuffixPattern;
            }
            StringBuffer affixBuf = new StringBuffer();
            expandAffix(affixPat, null, affixBuf, true);
            buf.append(affixBuf);
            return affixBuf.length();
        }
        if (isPrefix) {
            affix = isNegative ? this.negativePrefix : this.positivePrefix;
            pattern = isNegative ? this.negPrefixPattern : this.posPrefixPattern;
        } else {
            affix = isNegative ? this.negativeSuffix : this.positiveSuffix;
            pattern = isNegative ? this.negSuffixPattern : this.posSuffixPattern;
        }
        if (parseAttr) {
            int offset = affix.indexOf(this.symbols.getCurrencySymbol());
            if (offset > -1) {
                formatAffix2Attribute(isPrefix, NumberFormat.Field.CURRENCY, buf, offset, this.symbols.getCurrencySymbol().length());
            }
            int offset2 = affix.indexOf(this.symbols.getMinusString());
            if (offset2 > -1) {
                formatAffix2Attribute(isPrefix, NumberFormat.Field.SIGN, buf, offset2, this.symbols.getMinusString().length());
            }
            int offset3 = affix.indexOf(this.symbols.getPercent());
            if (offset3 > -1) {
                formatAffix2Attribute(isPrefix, NumberFormat.Field.PERCENT, buf, offset3, 1);
            }
            int offset4 = affix.indexOf(this.symbols.getPerMill());
            if (offset4 > -1) {
                formatAffix2Attribute(isPrefix, NumberFormat.Field.PERMILLE, buf, offset4, 1);
            }
            int offset5 = pattern.indexOf("¤¤¤");
            if (offset5 > -1) {
                formatAffix2Attribute(isPrefix, NumberFormat.Field.CURRENCY, buf, offset5, affix.length() - offset5);
            }
        }
        if (fieldPosition.getFieldAttribute() == NumberFormat.Field.CURRENCY) {
            if (affix.indexOf(this.symbols.getCurrencySymbol()) > -1) {
                String aff = this.symbols.getCurrencySymbol();
                int firstPos = affix.indexOf(aff);
                int start = buf.length() + firstPos;
                int end = start + aff.length();
                fieldPosition.setBeginIndex(start);
                fieldPosition.setEndIndex(end);
            } else if (affix.indexOf(this.symbols.getInternationalCurrencySymbol()) > -1) {
                String aff2 = this.symbols.getInternationalCurrencySymbol();
                int firstPos2 = affix.indexOf(aff2);
                int start2 = buf.length() + firstPos2;
                int end2 = start2 + aff2.length();
                fieldPosition.setBeginIndex(start2);
                fieldPosition.setEndIndex(end2);
            } else if (pattern.indexOf("¤¤¤") > -1) {
                int firstPos3 = pattern.indexOf("¤¤¤");
                int start3 = buf.length() + firstPos3;
                int end3 = buf.length() + affix.length();
                fieldPosition.setBeginIndex(start3);
                fieldPosition.setEndIndex(end3);
            }
        }
        buf.append(affix);
        return affix.length();
    }

    private void formatAffix2Attribute(boolean isPrefix, NumberFormat.Field fieldType, StringBuffer buf, int offset, int symbolSize) {
        int begin = offset;
        if (!isPrefix) {
            begin = offset + buf.length();
        }
        addAttribute(fieldType, begin, begin + symbolSize);
    }

    private void addAttribute(NumberFormat.Field field, int begin, int end) {
        FieldPosition pos = new FieldPosition(field);
        pos.setBeginIndex(begin);
        pos.setEndIndex(end);
        this.attributes.add(pos);
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
        StringBuffer text = new StringBuffer();
        unit.writePrefix(text);
        this.attributes.clear();
        if (obj instanceof BigInteger) {
            format((BigInteger) number, text, new FieldPosition(0), true);
        } else if (obj instanceof BigDecimal) {
            format((BigDecimal) number, text, new FieldPosition(0), true);
        } else if (obj instanceof Double) {
            format(number.doubleValue(), text, new FieldPosition(0), true);
        } else if ((obj instanceof Integer) || (obj instanceof Long)) {
            format(number.longValue(), text, new FieldPosition(0), true);
        } else {
            throw new IllegalArgumentException();
        }
        unit.writeSuffix(text);
        AttributedString as = new AttributedString(text.toString());
        for (int i = 0; i < this.attributes.size(); i++) {
            FieldPosition pos = this.attributes.get(i);
            Format.Field attribute = pos.getFieldAttribute();
            as.addAttribute(attribute, attribute, pos.getBeginIndex(), pos.getEndIndex());
        }
        return as.getIterator();
    }

    private void appendAffixPattern(StringBuffer buffer, boolean isNegative, boolean isPrefix, boolean localized) {
        String affixPat;
        String affix;
        if (isPrefix) {
            affixPat = isNegative ? this.negPrefixPattern : this.posPrefixPattern;
        } else {
            affixPat = isNegative ? this.negSuffixPattern : this.posSuffixPattern;
        }
        if (affixPat == null) {
            if (isPrefix) {
                affix = isNegative ? this.negativePrefix : this.positivePrefix;
            } else {
                affix = isNegative ? this.negativeSuffix : this.positiveSuffix;
            }
            buffer.append('\'');
            for (int i = 0; i < affix.length(); i++) {
                char ch = affix.charAt(i);
                if (ch == '\'') {
                    buffer.append(ch);
                }
                buffer.append(ch);
            }
            buffer.append('\'');
            return;
        }
        if (!localized) {
            buffer.append(affixPat);
            return;
        }
        int i2 = 0;
        while (i2 < affixPat.length()) {
            char ch2 = affixPat.charAt(i2);
            switch (ch2) {
                case '%':
                    ch2 = this.symbols.getPercent();
                    break;
                case '\'':
                    int j = affixPat.indexOf(39, i2 + 1);
                    if (j < 0) {
                        throw new IllegalArgumentException("Malformed affix pattern: " + affixPat);
                    }
                    buffer.append(affixPat.substring(i2, j + 1));
                    i2 = j;
                    continue;
                    i2++;
                    break;
                    break;
                case '-':
                    ch2 = this.symbols.getMinusSign();
                    break;
                case 8240:
                    ch2 = this.symbols.getPerMill();
                    break;
            }
            if (ch2 == this.symbols.getDecimalSeparator() || ch2 == this.symbols.getGroupingSeparator()) {
                buffer.append('\'');
                buffer.append(ch2);
                buffer.append('\'');
            } else {
                buffer.append(ch2);
            }
            i2++;
        }
    }

    private String toPattern(boolean localized) {
        String string;
        int minDig;
        int maxDig;
        int length;
        int pos;
        char padEscape;
        StringBuffer result = new StringBuffer();
        char zeroDigit = localized ? this.symbols.getZeroDigit() : PATTERN_ZERO_DIGIT;
        char digit = localized ? this.symbols.getDigit() : PATTERN_DIGIT;
        char sigDigit = 0;
        boolean useSigDig = areSignificantDigitsUsed();
        if (useSigDig) {
            sigDigit = localized ? this.symbols.getSignificantDigit() : PATTERN_SIGNIFICANT_DIGIT;
        }
        char groupingSeparator = localized ? this.symbols.getGroupingSeparator() : PATTERN_GROUPING_SEPARATOR;
        int roundingDecimalPos = 0;
        String roundingDigits = null;
        int padPos = this.formatWidth > 0 ? this.padPosition : -1;
        if (this.formatWidth > 0) {
            StringBuffer stringBuffer = new StringBuffer(2);
            if (localized) {
                padEscape = this.symbols.getPadEscape();
            } else {
                padEscape = PATTERN_PAD_ESCAPE;
            }
            string = stringBuffer.append(padEscape).append(this.pad).toString();
        } else {
            string = null;
        }
        if (this.roundingIncrementICU != null) {
            int i = this.roundingIncrementICU.scale();
            roundingDigits = this.roundingIncrementICU.movePointRight(i).toString();
            roundingDecimalPos = roundingDigits.length() - i;
        }
        int part = 0;
        while (part < 2) {
            if (padPos == 0) {
                result.append(string);
            }
            appendAffixPattern(result, part != 0, true, localized);
            if (padPos == 1) {
                result.append(string);
            }
            int sub0Start = result.length();
            int g = isGroupingUsed() ? Math.max(0, (int) this.groupingSize) : 0;
            if (g > 0 && this.groupingSize2 > 0 && this.groupingSize2 != this.groupingSize) {
                g += this.groupingSize2;
            }
            int maxSigDig = 0;
            if (useSigDig) {
                minDig = getMinimumSignificantDigits();
                maxSigDig = getMaximumSignificantDigits();
                maxDig = maxSigDig;
            } else {
                minDig = getMinimumIntegerDigits();
                maxDig = getMaximumIntegerDigits();
            }
            if (this.useExponentialNotation) {
                if (maxDig > 8) {
                    maxDig = 1;
                }
            } else if (useSigDig) {
                maxDig = Math.max(maxDig, g + 1);
            } else {
                maxDig = Math.max(Math.max(g, getMinimumIntegerDigits()), roundingDecimalPos) + 1;
            }
            int i2 = maxDig;
            while (i2 > 0) {
                if (!this.useExponentialNotation && i2 < maxDig && isGroupingPosition(i2)) {
                    result.append(groupingSeparator);
                }
                if (useSigDig) {
                    result.append((maxSigDig < i2 || i2 <= maxSigDig - minDig) ? digit : sigDigit);
                } else if (roundingDigits != null && (pos = roundingDecimalPos - i2) >= 0 && pos < roundingDigits.length()) {
                    result.append((char) ((roundingDigits.charAt(pos) - '0') + zeroDigit));
                } else {
                    result.append(i2 <= minDig ? zeroDigit : digit);
                }
                i2--;
            }
            if (!useSigDig) {
                if (getMaximumFractionDigits() > 0 || this.decimalSeparatorAlwaysShown) {
                    result.append(localized ? this.symbols.getDecimalSeparator() : PATTERN_DECIMAL_SEPARATOR);
                }
                int pos2 = roundingDecimalPos;
                int i3 = 0;
                while (i3 < getMaximumFractionDigits()) {
                    if (roundingDigits != null && pos2 < roundingDigits.length()) {
                        result.append(pos2 < 0 ? zeroDigit : (char) ((roundingDigits.charAt(pos2) - '0') + zeroDigit));
                        pos2++;
                    } else {
                        result.append(i3 < getMinimumFractionDigits() ? zeroDigit : digit);
                    }
                    i3++;
                }
            }
            if (this.useExponentialNotation) {
                if (localized) {
                    result.append(this.symbols.getExponentSeparator());
                } else {
                    result.append(PATTERN_EXPONENT);
                }
                if (this.exponentSignAlwaysShown) {
                    result.append(localized ? this.symbols.getPlusSign() : PATTERN_PLUS_SIGN);
                }
                for (int i4 = 0; i4 < this.minExponentDigits; i4++) {
                    result.append(zeroDigit);
                }
            }
            if (string != null && !this.useExponentialNotation) {
                int length2 = (this.formatWidth - result.length()) + sub0Start;
                if (part == 0) {
                    length = this.positivePrefix.length() + this.positiveSuffix.length();
                } else {
                    length = this.negativePrefix.length() + this.negativeSuffix.length();
                }
                int add = length2 - length;
                while (add > 0) {
                    result.insert(sub0Start, digit);
                    maxDig++;
                    add--;
                    if (add > 1 && isGroupingPosition(maxDig)) {
                        result.insert(sub0Start, groupingSeparator);
                        add--;
                    }
                }
            }
            if (padPos == 2) {
                result.append(string);
            }
            appendAffixPattern(result, part != 0, false, localized);
            if (padPos == 3) {
                result.append(string);
            }
            if (part == 0) {
                if (this.negativeSuffix.equals(this.positiveSuffix) && this.negativePrefix.equals(PATTERN_MINUS + this.positivePrefix)) {
                    break;
                }
                result.append(localized ? this.symbols.getPatternSeparator() : PATTERN_SEPARATOR);
            }
            part++;
        }
        return result.toString();
    }

    public void applyPattern(String pattern) {
        applyPattern(pattern, false);
    }

    public void applyLocalizedPattern(String pattern) {
        applyPattern(pattern, true);
    }

    private void applyPattern(String pattern, boolean localized) {
        applyPatternWithoutExpandAffix(pattern, localized);
        expandAffixAdjustWidth(null);
    }

    private void expandAffixAdjustWidth(String pluralCount) {
        expandAffixes(pluralCount);
        if (this.formatWidth <= 0) {
            return;
        }
        this.formatWidth += this.positivePrefix.length() + this.positiveSuffix.length();
    }

    private void applyPatternWithoutExpandAffix(String pattern, boolean localized) {
        char after;
        char zeroDigit = PATTERN_ZERO_DIGIT;
        char sigDigit = PATTERN_SIGNIFICANT_DIGIT;
        char groupingSeparator = PATTERN_GROUPING_SEPARATOR;
        char decimalSeparator = PATTERN_DECIMAL_SEPARATOR;
        char percent = PATTERN_PERCENT;
        char perMill = PATTERN_PER_MILLE;
        char digit = PATTERN_DIGIT;
        char separator = PATTERN_SEPARATOR;
        String exponent = String.valueOf(PATTERN_EXPONENT);
        char plus = PATTERN_PLUS_SIGN;
        char padEscape = PATTERN_PAD_ESCAPE;
        char minus = PATTERN_MINUS;
        if (localized) {
            zeroDigit = this.symbols.getZeroDigit();
            sigDigit = this.symbols.getSignificantDigit();
            groupingSeparator = this.symbols.getGroupingSeparator();
            decimalSeparator = this.symbols.getDecimalSeparator();
            percent = this.symbols.getPercent();
            perMill = this.symbols.getPerMill();
            digit = this.symbols.getDigit();
            separator = this.symbols.getPatternSeparator();
            exponent = this.symbols.getExponentSeparator();
            plus = this.symbols.getPlusSign();
            padEscape = this.symbols.getPadEscape();
            minus = this.symbols.getMinusSign();
        }
        char nineDigit = (char) (zeroDigit + '\t');
        boolean gotNegative = false;
        int pos = 0;
        for (int part = 0; part < 2 && pos < pattern.length(); part++) {
            int subpart = 1;
            int sub0Start = 0;
            int sub0Limit = 0;
            int sub2Limit = 0;
            StringBuilder prefix = new StringBuilder();
            StringBuilder suffix = new StringBuilder();
            int decimalPos = -1;
            int multpl = 1;
            int digitLeftCount = 0;
            int zeroDigitCount = 0;
            int digitRightCount = 0;
            int sigDigitCount = 0;
            byte groupingCount = -1;
            byte groupingCount2 = -1;
            int padPos = -1;
            char padChar = 0;
            int incrementPos = -1;
            long incrementVal = 0;
            byte expDigits = -1;
            boolean expSignAlways = false;
            int currencySignCnt = 0;
            StringBuilder affix = prefix;
            int start = pos;
            while (true) {
                if (pos < pattern.length()) {
                    char ch = pattern.charAt(pos);
                    switch (subpart) {
                        case 0:
                            if (ch == digit) {
                                if (zeroDigitCount > 0 || sigDigitCount > 0) {
                                    digitRightCount++;
                                } else {
                                    digitLeftCount++;
                                }
                                if (groupingCount >= 0 && decimalPos < 0) {
                                    groupingCount = (byte) (groupingCount + 1);
                                }
                            } else if ((ch >= zeroDigit && ch <= nineDigit) || ch == sigDigit) {
                                if (digitRightCount > 0) {
                                    patternError("Unexpected '" + ch + '\'', pattern);
                                }
                                if (ch == sigDigit) {
                                    sigDigitCount++;
                                } else {
                                    zeroDigitCount++;
                                    if (ch != zeroDigit) {
                                        int p = digitLeftCount + zeroDigitCount + digitRightCount;
                                        if (incrementPos >= 0) {
                                            while (incrementPos < p) {
                                                incrementVal *= 10;
                                                incrementPos++;
                                            }
                                        } else {
                                            incrementPos = p;
                                        }
                                        incrementVal += (long) (ch - zeroDigit);
                                    }
                                }
                                if (groupingCount >= 0 && decimalPos < 0) {
                                    groupingCount = (byte) (groupingCount + 1);
                                }
                            } else if (ch == groupingSeparator) {
                                if (ch == '\'' && pos + 1 < pattern.length() && (after = pattern.charAt(pos + 1)) != digit && (after < zeroDigit || after > nineDigit)) {
                                    if (after == '\'') {
                                        pos++;
                                        if (decimalPos >= 0) {
                                        }
                                        groupingCount2 = groupingCount;
                                        groupingCount = 0;
                                    } else if (groupingCount < 0) {
                                        subpart = 3;
                                    } else {
                                        subpart = 2;
                                        affix = suffix;
                                        sub0Limit = pos;
                                        pos--;
                                    }
                                } else {
                                    if (decimalPos >= 0) {
                                        patternError("Grouping separator after decimal", pattern);
                                    }
                                    groupingCount2 = groupingCount;
                                    groupingCount = 0;
                                }
                            } else if (ch == decimalSeparator) {
                                if (decimalPos >= 0) {
                                    patternError("Multiple decimal separators", pattern);
                                }
                                decimalPos = digitLeftCount + zeroDigitCount + digitRightCount;
                            } else {
                                if (pattern.regionMatches(pos, exponent, 0, exponent.length())) {
                                    if (expDigits >= 0) {
                                        patternError("Multiple exponential symbols", pattern);
                                    }
                                    if (groupingCount >= 0) {
                                        patternError("Grouping separator in exponential", pattern);
                                    }
                                    pos += exponent.length();
                                    if (pos < pattern.length() && pattern.charAt(pos) == plus) {
                                        expSignAlways = true;
                                        pos++;
                                    }
                                    expDigits = 0;
                                    while (pos < pattern.length() && pattern.charAt(pos) == zeroDigit) {
                                        expDigits = (byte) (expDigits + 1);
                                        pos++;
                                    }
                                    if ((digitLeftCount + zeroDigitCount < 1 && sigDigitCount + digitRightCount < 1) || ((sigDigitCount > 0 && digitLeftCount > 0) || expDigits < 1)) {
                                        patternError("Malformed exponential", pattern);
                                    }
                                }
                                subpart = 2;
                                affix = suffix;
                                sub0Limit = pos;
                                pos--;
                            }
                            pos++;
                            break;
                        case 1:
                        case 2:
                            if (ch == digit || ch == groupingSeparator || ch == decimalSeparator || ((ch >= zeroDigit && ch <= nineDigit) || ch == sigDigit)) {
                                if (subpart == 1) {
                                    subpart = 0;
                                    sub0Start = pos;
                                    pos--;
                                } else if (ch == '\'') {
                                    if (pos + 1 < pattern.length() && pattern.charAt(pos + 1) == '\'') {
                                        pos++;
                                        affix.append(ch);
                                    } else {
                                        subpart += 2;
                                    }
                                } else {
                                    patternError("Unquoted special character '" + ch + '\'', pattern);
                                    affix.append(ch);
                                }
                            } else {
                                if (ch == 164) {
                                    boolean doubled = pos + 1 < pattern.length() && pattern.charAt(pos + 1) == 164;
                                    if (doubled) {
                                        pos++;
                                        affix.append(ch);
                                        if (pos + 1 < pattern.length() && pattern.charAt(pos + 1) == 164) {
                                            pos++;
                                            affix.append(ch);
                                            currencySignCnt = 3;
                                        } else {
                                            currencySignCnt = 2;
                                        }
                                    } else {
                                        currencySignCnt = 1;
                                    }
                                } else if (ch == '\'') {
                                    if (pos + 1 < pattern.length() && pattern.charAt(pos + 1) == '\'') {
                                        pos++;
                                        affix.append(ch);
                                    } else {
                                        subpart += 2;
                                    }
                                } else if (ch == separator) {
                                    if (subpart == 1 || part == 1) {
                                        patternError("Unquoted special character '" + ch + '\'', pattern);
                                    }
                                    sub2Limit = pos;
                                    pos++;
                                    break;
                                } else if (ch == percent || ch == perMill) {
                                    if (multpl != 1) {
                                        patternError("Too many percent/permille characters", pattern);
                                    }
                                    multpl = ch == percent ? 100 : 1000;
                                    ch = ch == percent ? PATTERN_PERCENT : PATTERN_PER_MILLE;
                                } else if (ch == minus) {
                                    ch = PATTERN_MINUS;
                                } else if (ch == padEscape) {
                                    if (padPos >= 0) {
                                        patternError("Multiple pad specifiers", pattern);
                                    }
                                    if (pos + 1 == pattern.length()) {
                                        patternError("Invalid pad specifier", pattern);
                                    }
                                    padPos = pos;
                                    pos++;
                                    padChar = pattern.charAt(pos);
                                }
                                affix.append(ch);
                            }
                            pos++;
                            break;
                        case 3:
                        case 4:
                            if (ch == '\'') {
                                if (pos + 1 < pattern.length() && pattern.charAt(pos + 1) == '\'') {
                                    pos++;
                                    affix.append(ch);
                                } else {
                                    subpart -= 2;
                                }
                            }
                            affix.append(ch);
                            pos++;
                            break;
                        default:
                            pos++;
                            break;
                    }
                }
            }
            if (subpart == 3 || subpart == 4) {
                patternError("Unterminated quote", pattern);
            }
            if (sub0Limit == 0) {
                sub0Limit = pattern.length();
            }
            if (sub2Limit == 0) {
                sub2Limit = pattern.length();
            }
            if (zeroDigitCount == 0 && sigDigitCount == 0 && digitLeftCount > 0 && decimalPos >= 0) {
                int n = decimalPos;
                if (decimalPos == 0) {
                    n++;
                }
                digitRightCount = digitLeftCount - n;
                digitLeftCount = n - 1;
                zeroDigitCount = 1;
            }
            if ((decimalPos < 0 && digitRightCount > 0 && sigDigitCount == 0) || ((decimalPos >= 0 && (sigDigitCount > 0 || decimalPos < digitLeftCount || decimalPos > digitLeftCount + zeroDigitCount)) || groupingCount == 0 || groupingCount2 == 0 || ((sigDigitCount > 0 && zeroDigitCount > 0) || subpart > 2))) {
                patternError("Malformed pattern", pattern);
            }
            if (padPos >= 0) {
                if (padPos == start) {
                    padPos = 0;
                } else if (padPos + 2 == sub0Start) {
                    padPos = 1;
                } else if (padPos == sub0Limit) {
                    padPos = 2;
                } else if (padPos + 2 == sub2Limit) {
                    padPos = 3;
                } else {
                    patternError("Illegal pad position", pattern);
                }
            }
            if (part == 0) {
                String string = prefix.toString();
                this.negPrefixPattern = string;
                this.posPrefixPattern = string;
                String string2 = suffix.toString();
                this.negSuffixPattern = string2;
                this.posSuffixPattern = string2;
                this.useExponentialNotation = expDigits >= 0;
                if (this.useExponentialNotation) {
                    this.minExponentDigits = expDigits;
                    this.exponentSignAlwaysShown = expSignAlways;
                }
                int digitTotalCount = digitLeftCount + zeroDigitCount + digitRightCount;
                int effectiveDecimalPos = decimalPos >= 0 ? decimalPos : digitTotalCount;
                boolean useSigDig = sigDigitCount > 0;
                setSignificantDigitsUsed(useSigDig);
                if (useSigDig) {
                    setMinimumSignificantDigits(sigDigitCount);
                    setMaximumSignificantDigits(sigDigitCount + digitRightCount);
                } else {
                    int minInt = effectiveDecimalPos - digitLeftCount;
                    setMinimumIntegerDigits(minInt);
                    setMaximumIntegerDigits(this.useExponentialNotation ? digitLeftCount + minInt : DOUBLE_INTEGER_DIGITS);
                    _setMaximumFractionDigits(decimalPos >= 0 ? digitTotalCount - decimalPos : 0);
                    setMinimumFractionDigits(decimalPos >= 0 ? (digitLeftCount + zeroDigitCount) - decimalPos : 0);
                }
                setGroupingUsed(groupingCount > 0);
                this.groupingSize = groupingCount > 0 ? groupingCount : (byte) 0;
                if (groupingCount2 <= 0 || groupingCount2 == groupingCount) {
                    groupingCount2 = 0;
                }
                this.groupingSize2 = groupingCount2;
                this.multiplier = multpl;
                setDecimalSeparatorAlwaysShown(decimalPos == 0 || decimalPos == digitTotalCount);
                if (padPos >= 0) {
                    this.padPosition = padPos;
                    this.formatWidth = sub0Limit - sub0Start;
                    this.pad = padChar;
                } else {
                    this.formatWidth = 0;
                }
                if (incrementVal != 0) {
                    int scale = incrementPos - effectiveDecimalPos;
                    this.roundingIncrementICU = android.icu.math.BigDecimal.valueOf(incrementVal, scale > 0 ? scale : 0);
                    if (scale < 0) {
                        this.roundingIncrementICU = this.roundingIncrementICU.movePointRight(-scale);
                    }
                    this.roundingMode = 6;
                } else {
                    setRoundingIncrement((android.icu.math.BigDecimal) null);
                }
                this.currencySignCount = currencySignCnt;
            } else {
                this.negPrefixPattern = prefix.toString();
                this.negSuffixPattern = suffix.toString();
                gotNegative = true;
            }
        }
        if (pattern.length() == 0) {
            this.posSuffixPattern = "";
            this.posPrefixPattern = "";
            setMinimumIntegerDigits(0);
            setMaximumIntegerDigits(DOUBLE_INTEGER_DIGITS);
            setMinimumFractionDigits(0);
            _setMaximumFractionDigits(DOUBLE_FRACTION_DIGITS);
        }
        if (!gotNegative || (this.negPrefixPattern.equals(this.posPrefixPattern) && this.negSuffixPattern.equals(this.posSuffixPattern))) {
            this.negSuffixPattern = this.posSuffixPattern;
            this.negPrefixPattern = PATTERN_MINUS + this.posPrefixPattern;
        }
        setLocale(null, null);
        this.formatPattern = pattern;
        if (this.currencySignCount != 0) {
            Currency theCurrency = getCurrency();
            if (theCurrency != null) {
                setRoundingIncrement(theCurrency.getRoundingIncrement(this.currencyUsage));
                int d = theCurrency.getDefaultFractionDigits(this.currencyUsage);
                setMinimumFractionDigits(d);
                _setMaximumFractionDigits(d);
            }
            if (this.currencySignCount == 3 && this.currencyPluralInfo == null) {
                this.currencyPluralInfo = new CurrencyPluralInfo(this.symbols.getULocale());
            }
        }
        resetActualRounding();
    }

    private void patternError(String msg, String pattern) {
        throw new IllegalArgumentException(msg + " in pattern \"" + pattern + '\"');
    }

    @Override
    public void setMaximumIntegerDigits(int newValue) {
        super.setMaximumIntegerDigits(Math.min(newValue, MAX_INTEGER_DIGITS));
    }

    @Override
    public void setMinimumIntegerDigits(int newValue) {
        super.setMinimumIntegerDigits(Math.min(newValue, DOUBLE_INTEGER_DIGITS));
    }

    public int getMinimumSignificantDigits() {
        return this.minSignificantDigits;
    }

    public int getMaximumSignificantDigits() {
        return this.maxSignificantDigits;
    }

    public void setMinimumSignificantDigits(int min) {
        if (min < 1) {
            min = 1;
        }
        int max = Math.max(this.maxSignificantDigits, min);
        this.minSignificantDigits = min;
        this.maxSignificantDigits = max;
        setSignificantDigitsUsed(true);
    }

    public void setMaximumSignificantDigits(int max) {
        if (max < 1) {
            max = 1;
        }
        int min = Math.min(this.minSignificantDigits, max);
        this.minSignificantDigits = min;
        this.maxSignificantDigits = max;
        setSignificantDigitsUsed(true);
    }

    public boolean areSignificantDigitsUsed() {
        return this.useSignificantDigits;
    }

    public void setSignificantDigitsUsed(boolean useSignificantDigits) {
        this.useSignificantDigits = useSignificantDigits;
    }

    @Override
    public void setCurrency(Currency theCurrency) {
        super.setCurrency(theCurrency);
        if (theCurrency != null) {
            boolean[] isChoiceFormat = new boolean[1];
            String s = theCurrency.getName(this.symbols.getULocale(), 0, isChoiceFormat);
            this.symbols.setCurrency(theCurrency);
            this.symbols.setCurrencySymbol(s);
        }
        if (this.currencySignCount == 0) {
            return;
        }
        if (theCurrency != null) {
            setRoundingIncrement(theCurrency.getRoundingIncrement(this.currencyUsage));
            int d = theCurrency.getDefaultFractionDigits(this.currencyUsage);
            setMinimumFractionDigits(d);
            setMaximumFractionDigits(d);
        }
        if (this.currencySignCount == 3) {
            return;
        }
        expandAffixes(null);
    }

    public void setCurrencyUsage(Currency.CurrencyUsage newUsage) {
        if (newUsage == null) {
            throw new NullPointerException("return value is null at method AAA");
        }
        this.currencyUsage = newUsage;
        Currency theCurrency = getCurrency();
        if (theCurrency == null) {
            return;
        }
        setRoundingIncrement(theCurrency.getRoundingIncrement(this.currencyUsage));
        int d = theCurrency.getDefaultFractionDigits(this.currencyUsage);
        setMinimumFractionDigits(d);
        _setMaximumFractionDigits(d);
    }

    public Currency.CurrencyUsage getCurrencyUsage() {
        return this.currencyUsage;
    }

    @Override
    @Deprecated
    protected Currency getEffectiveCurrency() {
        Currency c = getCurrency();
        if (c == null) {
            return Currency.getInstance(this.symbols.getInternationalCurrencySymbol());
        }
        return c;
    }

    @Override
    public void setMaximumFractionDigits(int newValue) {
        _setMaximumFractionDigits(newValue);
        resetActualRounding();
    }

    private void _setMaximumFractionDigits(int newValue) {
        super.setMaximumFractionDigits(Math.min(newValue, DOUBLE_FRACTION_DIGITS));
    }

    @Override
    public void setMinimumFractionDigits(int newValue) {
        super.setMinimumFractionDigits(Math.min(newValue, DOUBLE_FRACTION_DIGITS));
    }

    public void setParseBigDecimal(boolean value) {
        this.parseBigDecimal = value;
    }

    public boolean isParseBigDecimal() {
        return this.parseBigDecimal;
    }

    public void setParseMaxDigits(int newValue) {
        if (newValue <= 0) {
            return;
        }
        this.PARSE_MAX_EXPONENT = newValue;
    }

    public int getParseMaxDigits() {
        return this.PARSE_MAX_EXPONENT;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        this.attributes.clear();
        stream.defaultWriteObject();
    }

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        stream.defaultReadObject();
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
        this.digitList = new DigitList();
        if (this.roundingIncrement != null) {
            setInternalRoundingIncrement(new android.icu.math.BigDecimal(this.roundingIncrement));
        }
        resetActualRounding();
    }

    private void setInternalRoundingIncrement(android.icu.math.BigDecimal value) {
        this.roundingIncrementICU = value;
        this.roundingIncrement = value != null ? value.toBigDecimal() : null;
    }

    private static final class AffixForCurrency {
        private String negPrefixPatternForCurrency;
        private String negSuffixPatternForCurrency;
        private final int patternType;
        private String posPrefixPatternForCurrency;
        private String posSuffixPatternForCurrency;

        public AffixForCurrency(String negPrefix, String negSuffix, String posPrefix, String posSuffix, int type) {
            this.negPrefixPatternForCurrency = null;
            this.negSuffixPatternForCurrency = null;
            this.posPrefixPatternForCurrency = null;
            this.posSuffixPatternForCurrency = null;
            this.negPrefixPatternForCurrency = negPrefix;
            this.negSuffixPatternForCurrency = negSuffix;
            this.posPrefixPatternForCurrency = posPrefix;
            this.posSuffixPatternForCurrency = posSuffix;
            this.patternType = type;
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

        public Unit(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        public void writeSuffix(StringBuffer toAppendTo) {
            toAppendTo.append(this.suffix);
        }

        public void writePrefix(StringBuffer toAppendTo) {
            toAppendTo.append(this.prefix);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Unit)) {
                return false;
            }
            Unit other = (Unit) obj;
            if (this.prefix.equals(other.prefix)) {
                return this.suffix.equals(other.suffix);
            }
            return false;
        }

        public String toString() {
            return this.prefix + "/" + this.suffix;
        }
    }

    private void resetActualRounding() {
        if (this.roundingIncrementICU != null) {
            android.icu.math.BigDecimal byWidth = getMaximumFractionDigits() > 0 ? android.icu.math.BigDecimal.ONE.movePointLeft(getMaximumFractionDigits()) : android.icu.math.BigDecimal.ONE;
            if (this.roundingIncrementICU.compareTo(byWidth) >= 0) {
                this.actualRoundingIncrementICU = this.roundingIncrementICU;
            } else {
                if (byWidth.equals(android.icu.math.BigDecimal.ONE)) {
                    byWidth = null;
                }
                this.actualRoundingIncrementICU = byWidth;
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

    private void setRoundingDouble(double newValue) {
        this.roundingDouble = newValue;
        if (this.roundingDouble > 0.0d) {
            double rawRoundedReciprocal = 1.0d / this.roundingDouble;
            this.roundingDoubleReciprocal = Math.rint(rawRoundedReciprocal);
            if (Math.abs(rawRoundedReciprocal - this.roundingDoubleReciprocal) <= roundingIncrementEpsilon) {
                return;
            }
            this.roundingDoubleReciprocal = 0.0d;
            return;
        }
        this.roundingDoubleReciprocal = 0.0d;
    }
}
