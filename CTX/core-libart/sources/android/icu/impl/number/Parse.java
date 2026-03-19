package android.icu.impl.number;

import android.icu.impl.StandardPlural;
import android.icu.impl.TextTrieMap;
import android.icu.impl.locale.LanguageTag;
import android.icu.lang.UCharacter;
import android.icu.text.CurrencyPluralInfo;
import android.icu.text.DecimalFormatSymbols;
import android.icu.text.NumberFormat;
import android.icu.text.UnicodeSet;
import android.icu.util.Currency;
import android.icu.util.CurrencyAmount;
import android.icu.util.ULocale;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Parse {
    static final boolean $assertionsDisabled = false;
    private static final UnicodeSet UNISET_WHITESPACE = new UnicodeSet("[[:Zs:][\\u0009]]").freeze();
    private static final UnicodeSet UNISET_BIDI = new UnicodeSet("[[\\u200E\\u200F\\u061C]]").freeze();
    private static final UnicodeSet UNISET_PERIOD_LIKE = new UnicodeSet("[.\\u2024\\u3002\\uFE12\\uFE52\\uFF0E\\uFF61]").freeze();
    private static final UnicodeSet UNISET_STRICT_PERIOD_LIKE = new UnicodeSet("[.\\u2024\\uFE52\\uFF0E\\uFF61]").freeze();
    private static final UnicodeSet UNISET_COMMA_LIKE = new UnicodeSet("[,\\u060C\\u066B\\u3001\\uFE10\\uFE11\\uFE50\\uFE51\\uFF0C\\uFF64]").freeze();
    private static final UnicodeSet UNISET_STRICT_COMMA_LIKE = new UnicodeSet("[,\\u066B\\uFE10\\uFE50\\uFF0C]").freeze();
    private static final UnicodeSet UNISET_OTHER_GROUPING_SEPARATORS = new UnicodeSet("[\\ '\\u00A0\\u066C\\u2000-\\u200A\\u2018\\u2019\\u202F\\u205F\\u3000\\uFF07]").freeze();
    private static final BigDecimal MIN_LONG_AS_BIG_DECIMAL = new BigDecimal(Long.MIN_VALUE);
    private static final BigDecimal MAX_LONG_AS_BIG_DECIMAL = new BigDecimal(Long.MAX_VALUE);
    protected static final ThreadLocal<ParserState> threadLocalParseState = new ThreadLocal<ParserState>() {
        @Override
        protected ParserState initialValue() {
            return new ParserState();
        }
    };
    protected static final ThreadLocal<ParsePosition> threadLocalParsePosition = new ThreadLocal<ParsePosition>() {
        @Override
        protected ParsePosition initialValue() {
            return new ParsePosition(0);
        }
    };

    @Deprecated
    public static final UnicodeSet UNISET_PLUS = new UnicodeSet(43, 43, 8314, 8314, 8330, 8330, 10133, 10133, 64297, 64297, 65122, 65122, 65291, 65291).freeze();

    @Deprecated
    public static final UnicodeSet UNISET_MINUS = new UnicodeSet(45, 45, 8315, 8315, 8331, 8331, 8722, 8722, 10134, 10134, 65123, 65123, 65293, 65293).freeze();
    public static volatile boolean DEBUGGING = false;

    private enum DigitType {
        INTEGER,
        FRACTION,
        EXPONENT
    }

    public enum GroupingMode {
        DEFAULT,
        RESTRICTED
    }

    public enum ParseMode {
        LENIENT,
        STRICT,
        FAST
    }

    private enum StateName {
        BEFORE_PREFIX,
        AFTER_PREFIX,
        AFTER_INTEGER_DIGIT,
        AFTER_FRACTION_DIGIT,
        AFTER_EXPONENT_SEPARATOR,
        AFTER_EXPONENT_DIGIT,
        BEFORE_SUFFIX,
        BEFORE_SUFFIX_SEEN_EXPONENT,
        AFTER_SUFFIX,
        INSIDE_CURRENCY,
        INSIDE_DIGIT,
        INSIDE_STRING,
        INSIDE_AFFIX_PATTERN
    }

    private enum SeparatorType {
        COMMA_LIKE,
        PERIOD_LIKE,
        OTHER_GROUPING,
        UNKNOWN;

        static SeparatorType fromCp(int i, ParseMode parseMode) {
            if (parseMode == ParseMode.FAST) {
                return UNKNOWN;
            }
            return parseMode == ParseMode.STRICT ? Parse.UNISET_STRICT_COMMA_LIKE.contains(i) ? COMMA_LIKE : Parse.UNISET_STRICT_PERIOD_LIKE.contains(i) ? PERIOD_LIKE : Parse.UNISET_OTHER_GROUPING_SEPARATORS.contains(i) ? OTHER_GROUPING : UNKNOWN : Parse.UNISET_COMMA_LIKE.contains(i) ? COMMA_LIKE : Parse.UNISET_PERIOD_LIKE.contains(i) ? PERIOD_LIKE : Parse.UNISET_OTHER_GROUPING_SEPARATORS.contains(i) ? OTHER_GROUPING : UNKNOWN;
        }
    }

    private static class StateItem {
        static final boolean $assertionsDisabled = false;
        AffixHolder affix;
        CharSequence currentAffixPattern;
        TextTrieMap<Currency.CurrencyStringInfo>.ParseState currentCurrencyTrieState;
        TextTrieMap<Byte>.ParseState currentDigitTrieState;
        DigitType currentDigitType;
        int currentOffset;
        long currentStepwiseParserTag;
        CharSequence currentString;
        boolean currentTrailing;
        int exponent;
        DecimalQuantity_DualStorageBCD fq = new DecimalQuantity_DualStorageBCD();
        int groupingCp;
        long groupingWidths;
        final char id;
        String isoCode;
        StateName name;
        int numDigits;
        String path;
        StateName returnTo1;
        StateName returnTo2;
        boolean sawCurrency;
        boolean sawDecimalPoint;
        boolean sawExponentDigit;
        boolean sawInfinity;
        boolean sawNaN;
        boolean sawNegative;
        boolean sawNegativeExponent;
        boolean sawPrefix;
        boolean sawSuffix;
        int score;
        int trailingCount;
        int trailingZeros;

        StateItem(char c) {
            this.id = c;
        }

        StateItem clear() {
            this.name = StateName.BEFORE_PREFIX;
            this.trailingCount = 0;
            this.score = 0;
            this.fq.clear();
            this.numDigits = 0;
            this.trailingZeros = 0;
            this.exponent = 0;
            this.groupingCp = -1;
            this.groupingWidths = 0L;
            this.isoCode = null;
            this.sawNegative = false;
            this.sawNegativeExponent = false;
            this.sawCurrency = false;
            this.sawNaN = false;
            this.sawInfinity = false;
            this.affix = null;
            this.sawPrefix = false;
            this.sawSuffix = false;
            this.sawDecimalPoint = false;
            this.sawExponentDigit = false;
            this.returnTo1 = null;
            this.returnTo2 = null;
            this.currentString = null;
            this.currentOffset = 0;
            this.currentTrailing = false;
            this.currentAffixPattern = null;
            this.currentStepwiseParserTag = 0L;
            this.currentCurrencyTrieState = null;
            this.currentDigitTrieState = null;
            this.currentDigitType = null;
            this.path = "";
            return this;
        }

        StateItem copyFrom(StateItem stateItem, StateName stateName, int i) {
            this.name = stateName;
            this.score = stateItem.score;
            this.trailingCount = i < 0 ? 0 : stateItem.trailingCount + Character.charCount(i);
            this.fq.copyFrom(stateItem.fq);
            this.numDigits = stateItem.numDigits;
            this.trailingZeros = stateItem.trailingZeros;
            this.exponent = stateItem.exponent;
            this.groupingCp = stateItem.groupingCp;
            this.groupingWidths = stateItem.groupingWidths;
            this.isoCode = stateItem.isoCode;
            this.sawNegative = stateItem.sawNegative;
            this.sawNegativeExponent = stateItem.sawNegativeExponent;
            this.sawCurrency = stateItem.sawCurrency;
            this.sawNaN = stateItem.sawNaN;
            this.sawInfinity = stateItem.sawInfinity;
            this.affix = stateItem.affix;
            this.sawPrefix = stateItem.sawPrefix;
            this.sawSuffix = stateItem.sawSuffix;
            this.sawDecimalPoint = stateItem.sawDecimalPoint;
            this.sawExponentDigit = stateItem.sawExponentDigit;
            this.returnTo1 = stateItem.returnTo1;
            this.returnTo2 = stateItem.returnTo2;
            this.currentString = stateItem.currentString;
            this.currentOffset = stateItem.currentOffset;
            this.currentTrailing = stateItem.currentTrailing;
            this.currentAffixPattern = stateItem.currentAffixPattern;
            this.currentStepwiseParserTag = stateItem.currentStepwiseParserTag;
            this.currentCurrencyTrieState = stateItem.currentCurrencyTrieState;
            this.currentDigitTrieState = stateItem.currentDigitTrieState;
            this.currentDigitType = stateItem.currentDigitType;
            if (Parse.DEBUGGING) {
                this.path = stateItem.path + stateItem.id;
            }
            return this;
        }

        void appendDigit(byte b, DigitType digitType) {
            if (digitType == DigitType.EXPONENT) {
                this.sawExponentDigit = true;
                int i = (this.exponent * 10) + b;
                if (i < this.exponent) {
                    this.exponent = Integer.MAX_VALUE;
                    return;
                } else {
                    this.exponent = i;
                    return;
                }
            }
            this.numDigits++;
            if (digitType == DigitType.FRACTION && b == 0) {
                this.trailingZeros++;
            } else if (digitType == DigitType.FRACTION) {
                this.fq.appendDigit(b, this.trailingZeros, false);
                this.trailingZeros = 0;
            } else {
                this.fq.appendDigit(b, 0, true);
            }
        }

        public boolean hasNumber() {
            return this.numDigits > 0 || this.sawNaN || this.sawInfinity;
        }

        Number toNumber(DecimalFormatProperties decimalFormatProperties) {
            if (this.sawNaN) {
                return Double.valueOf(Double.NaN);
            }
            if (this.sawInfinity) {
                if (this.sawNegative) {
                    return Double.valueOf(Double.NEGATIVE_INFINITY);
                }
                return Double.valueOf(Double.POSITIVE_INFINITY);
            }
            if (this.fq.isZero() && this.sawNegative) {
                return Double.valueOf(-0.0d);
            }
            boolean parseToBigDecimal = decimalFormatProperties.getParseToBigDecimal();
            if (this.exponent == Integer.MAX_VALUE) {
                if (this.sawNegativeExponent && this.sawNegative) {
                    return Double.valueOf(-0.0d);
                }
                if (this.sawNegativeExponent) {
                    return Double.valueOf(0.0d);
                }
                if (this.sawNegative) {
                    return Double.valueOf(Double.NEGATIVE_INFINITY);
                }
                return Double.valueOf(Double.POSITIVE_INFINITY);
            }
            if (this.exponent > 1000) {
                parseToBigDecimal = true;
            }
            BigDecimal multiplier = decimalFormatProperties.getMultiplier();
            if (decimalFormatProperties.getMagnitudeMultiplier() != 0) {
                if (multiplier == null) {
                    multiplier = BigDecimal.ONE;
                }
                multiplier = multiplier.scaleByPowerOfTen(decimalFormatProperties.getMagnitudeMultiplier());
            }
            int i = (this.sawNegativeExponent ? -1 : 1) * this.exponent;
            MathContext mathContextOr34Digits = RoundingUtils.getMathContextOr34Digits(decimalFormatProperties);
            BigDecimal bigDecimal = this.fq.toBigDecimal();
            if (this.sawNegative) {
                bigDecimal = bigDecimal.negate();
            }
            BigDecimal bigDecimalScaleByPowerOfTen = bigDecimal.scaleByPowerOfTen(i);
            if (multiplier != null) {
                bigDecimalScaleByPowerOfTen = bigDecimalScaleByPowerOfTen.divide(multiplier, mathContextOr34Digits);
            }
            BigDecimal bigDecimalStripTrailingZeros = bigDecimalScaleByPowerOfTen.stripTrailingZeros();
            if (!parseToBigDecimal && bigDecimalStripTrailingZeros.scale() <= 0) {
                if (bigDecimalStripTrailingZeros.compareTo(Parse.MIN_LONG_AS_BIG_DECIMAL) >= 0 && bigDecimalStripTrailingZeros.compareTo(Parse.MAX_LONG_AS_BIG_DECIMAL) <= 0) {
                    return Long.valueOf(bigDecimalStripTrailingZeros.longValueExact());
                }
                return bigDecimalStripTrailingZeros.toBigIntegerExact();
            }
            return bigDecimalStripTrailingZeros;
        }

        public CurrencyAmount toCurrencyAmount(DecimalFormatProperties decimalFormatProperties) {
            return new CurrencyAmount(toNumber(decimalFormatProperties), Currency.getInstance(this.isoCode));
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            sb.append(this.path);
            sb.append("] ");
            sb.append(this.name.name());
            if (this.name == StateName.INSIDE_STRING) {
                sb.append("{");
                sb.append(this.currentString);
                sb.append(":");
                sb.append(this.currentOffset);
                sb.append("}");
            }
            if (this.name == StateName.INSIDE_AFFIX_PATTERN) {
                sb.append("{");
                sb.append(this.currentAffixPattern);
                sb.append(":");
                sb.append(AffixUtils.getOffset(this.currentStepwiseParserTag) - 1);
                sb.append("}");
            }
            sb.append(Padder.FALLBACK_PADDING_STRING);
            sb.append(this.fq.toBigDecimal());
            sb.append(" grouping:");
            sb.append(this.groupingCp == -1 ? new char[]{'?'} : Character.toChars(this.groupingCp));
            sb.append(" widths:");
            sb.append(Long.toHexString(this.groupingWidths));
            sb.append(" seen:");
            sb.append(this.sawNegative ? 1 : 0);
            sb.append(this.sawNegativeExponent ? 1 : 0);
            sb.append(this.sawNaN ? 1 : 0);
            sb.append(this.sawInfinity ? 1 : 0);
            sb.append(this.sawPrefix ? 1 : 0);
            sb.append(this.sawSuffix ? 1 : 0);
            sb.append(this.sawDecimalPoint ? 1 : 0);
            sb.append(" trailing:");
            sb.append(this.trailingCount);
            sb.append(" score:");
            sb.append(this.score);
            sb.append(" affix:");
            sb.append(this.affix);
            sb.append(" currency:");
            sb.append(this.isoCode);
            return sb.toString();
        }
    }

    private static class ParserState {
        static final boolean $assertionsDisabled = false;
        boolean caseSensitive;
        int decimalCp1;
        int decimalCp2;
        SeparatorType decimalType1;
        SeparatorType decimalType2;
        TextTrieMap<Byte> digitTrie;
        int groupingCp1;
        int groupingCp2;
        GroupingMode groupingMode;
        SeparatorType groupingType1;
        SeparatorType groupingType2;
        int length;
        ParseMode mode;
        boolean parseCurrency;
        int prevLength;
        DecimalFormatProperties properties;
        DecimalFormatSymbols symbols;
        StateItem[] items = new StateItem[16];
        StateItem[] prevItems = new StateItem[16];
        Set<AffixHolder> affixHolders = new HashSet();

        ParserState() {
            for (int i = 0; i < this.items.length; i++) {
                char c = (char) (65 + i);
                this.items[i] = new StateItem(c);
                this.prevItems[i] = new StateItem(c);
            }
        }

        ParserState clear() {
            this.length = 0;
            this.prevLength = 0;
            this.digitTrie = null;
            this.affixHolders.clear();
            return this;
        }

        void swap() {
            StateItem[] stateItemArr = this.prevItems;
            this.prevItems = this.items;
            this.items = stateItemArr;
            this.prevLength = this.length;
            this.length = 0;
        }

        void swapBack() {
            StateItem[] stateItemArr = this.prevItems;
            this.prevItems = this.items;
            this.items = stateItemArr;
            this.length = this.prevLength;
            this.prevLength = 0;
        }

        StateItem getNext() {
            if (this.length >= this.items.length) {
                this.length = this.items.length - 1;
            }
            StateItem stateItem = this.items[this.length];
            this.length++;
            return stateItem;
        }

        public int lastInsertedIndex() {
            return this.length - 1;
        }

        public StateItem getItem(int i) {
            return this.items[i];
        }

        public String toString() {
            return "<ParseState mode:" + this.mode + " caseSensitive:" + this.caseSensitive + " parseCurrency:" + this.parseCurrency + " groupingMode:" + this.groupingMode + " decimalCps:" + ((char) this.decimalCp1) + ((char) this.decimalCp2) + " groupingCps:" + ((char) this.groupingCp1) + ((char) this.groupingCp2) + " affixes:" + this.affixHolders + ">";
        }
    }

    private static class AffixHolder {
        final boolean negative;
        final String p;
        final String s;
        final boolean strings;
        static final AffixHolder EMPTY_POSITIVE = new AffixHolder("", "", true, false);
        static final AffixHolder EMPTY_NEGATIVE = new AffixHolder("", "", true, true);

        static void addToState(ParserState parserState, DecimalFormatProperties decimalFormatProperties) {
            AffixHolder affixHolderFromPropertiesPositivePattern = fromPropertiesPositivePattern(decimalFormatProperties);
            AffixHolder affixHolderFromPropertiesNegativePattern = fromPropertiesNegativePattern(decimalFormatProperties);
            AffixHolder affixHolderFromPropertiesPositiveString = fromPropertiesPositiveString(decimalFormatProperties);
            AffixHolder affixHolderFromPropertiesNegativeString = fromPropertiesNegativeString(decimalFormatProperties);
            if (affixHolderFromPropertiesPositivePattern != null) {
                parserState.affixHolders.add(affixHolderFromPropertiesPositivePattern);
            }
            if (affixHolderFromPropertiesPositiveString != null) {
                parserState.affixHolders.add(affixHolderFromPropertiesPositiveString);
            }
            if (affixHolderFromPropertiesNegativePattern != null) {
                parserState.affixHolders.add(affixHolderFromPropertiesNegativePattern);
            }
            if (affixHolderFromPropertiesNegativeString != null) {
                parserState.affixHolders.add(affixHolderFromPropertiesNegativeString);
            }
        }

        static AffixHolder fromPropertiesPositivePattern(DecimalFormatProperties decimalFormatProperties) {
            String strReplaceType;
            boolean z;
            String positivePrefixPattern = decimalFormatProperties.getPositivePrefixPattern();
            String positiveSuffixPattern = decimalFormatProperties.getPositiveSuffixPattern();
            if (decimalFormatProperties.getSignAlwaysShown()) {
                String negativePrefixPattern = decimalFormatProperties.getNegativePrefixPattern();
                String negativeSuffixPattern = decimalFormatProperties.getNegativeSuffixPattern();
                if (AffixUtils.containsType(negativePrefixPattern, -1)) {
                    strReplaceType = AffixUtils.replaceType(negativePrefixPattern, -1, '+');
                    z = true;
                } else {
                    strReplaceType = positivePrefixPattern;
                    z = false;
                }
                if (AffixUtils.containsType(negativeSuffixPattern, -1)) {
                    positiveSuffixPattern = AffixUtils.replaceType(negativeSuffixPattern, -1, '+');
                    z = true;
                }
                if (!z) {
                    positivePrefixPattern = "+" + strReplaceType;
                } else {
                    positivePrefixPattern = strReplaceType;
                }
            }
            return getInstance(positivePrefixPattern, positiveSuffixPattern, false, false);
        }

        static AffixHolder fromPropertiesNegativePattern(DecimalFormatProperties decimalFormatProperties) {
            String negativePrefixPattern = decimalFormatProperties.getNegativePrefixPattern();
            String negativeSuffixPattern = decimalFormatProperties.getNegativeSuffixPattern();
            if (negativePrefixPattern == null && negativeSuffixPattern == null) {
                String positivePrefixPattern = decimalFormatProperties.getPositivePrefixPattern();
                negativeSuffixPattern = decimalFormatProperties.getPositiveSuffixPattern();
                if (positivePrefixPattern == null) {
                    negativePrefixPattern = LanguageTag.SEP;
                } else {
                    negativePrefixPattern = LanguageTag.SEP + positivePrefixPattern;
                }
            }
            return getInstance(negativePrefixPattern, negativeSuffixPattern, false, true);
        }

        static AffixHolder fromPropertiesPositiveString(DecimalFormatProperties decimalFormatProperties) {
            String positivePrefix = decimalFormatProperties.getPositivePrefix();
            String positiveSuffix = decimalFormatProperties.getPositiveSuffix();
            if (positivePrefix == null && positiveSuffix == null) {
                return null;
            }
            return getInstance(positivePrefix, positiveSuffix, true, false);
        }

        static AffixHolder fromPropertiesNegativeString(DecimalFormatProperties decimalFormatProperties) {
            String negativePrefix = decimalFormatProperties.getNegativePrefix();
            String negativeSuffix = decimalFormatProperties.getNegativeSuffix();
            if (negativePrefix == null && negativeSuffix == null) {
                return null;
            }
            return getInstance(negativePrefix, negativeSuffix, true, true);
        }

        static AffixHolder getInstance(String str, String str2, boolean z, boolean z2) {
            if (str == null && str2 == null) {
                return z2 ? EMPTY_NEGATIVE : EMPTY_POSITIVE;
            }
            if (str == null) {
                str = "";
            }
            if (str2 == null) {
                str2 = "";
            }
            if (str.length() == 0 && str2.length() == 0) {
                return z2 ? EMPTY_NEGATIVE : EMPTY_POSITIVE;
            }
            return new AffixHolder(str, str2, z, z2);
        }

        AffixHolder(String str, String str2, boolean z, boolean z2) {
            this.p = str;
            this.s = str2;
            this.strings = z;
            this.negative = z2;
        }

        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AffixHolder)) {
                return false;
            }
            AffixHolder affixHolder = (AffixHolder) obj;
            if (!this.p.equals(affixHolder.p) || !this.s.equals(affixHolder.s) || this.strings != affixHolder.strings || this.negative != affixHolder.negative) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            return this.p.hashCode() ^ this.s.hashCode();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append(this.p);
            sb.append("|");
            sb.append(this.s);
            sb.append("|");
            sb.append(this.strings ? 'S' : 'P');
            sb.append("}");
            return sb.toString();
        }
    }

    private static class CurrencyAffixPatterns {
        private static final ConcurrentHashMap<ULocale, CurrencyAffixPatterns> currencyAffixPatterns = new ConcurrentHashMap<>();
        private static final ThreadLocal<DecimalFormatProperties> threadLocalProperties = new ThreadLocal<DecimalFormatProperties>() {
            @Override
            protected DecimalFormatProperties initialValue() {
                return new DecimalFormatProperties();
            }
        };
        private final Set<AffixHolder> set = new HashSet();

        static void addToState(ULocale uLocale, ParserState parserState) {
            CurrencyAffixPatterns currencyAffixPatterns2 = currencyAffixPatterns.get(uLocale);
            if (currencyAffixPatterns2 == null) {
                currencyAffixPatterns.putIfAbsent(uLocale, new CurrencyAffixPatterns(uLocale));
                currencyAffixPatterns2 = currencyAffixPatterns.get(uLocale);
            }
            parserState.affixHolders.addAll(currencyAffixPatterns2.set);
        }

        private CurrencyAffixPatterns(ULocale uLocale) {
            addPattern(NumberFormat.getPatternForStyle(uLocale, 1));
            CurrencyPluralInfo currencyPluralInfo = CurrencyPluralInfo.getInstance(uLocale);
            Iterator<StandardPlural> it = StandardPlural.VALUES.iterator();
            while (it.hasNext()) {
                addPattern(currencyPluralInfo.getCurrencyPluralPattern(it.next().getKeyword()));
            }
        }

        private void addPattern(String str) {
            DecimalFormatProperties decimalFormatProperties = threadLocalProperties.get();
            try {
                PatternStringParser.parseToExistingProperties(str, decimalFormatProperties);
            } catch (IllegalArgumentException e) {
            }
            this.set.add(AffixHolder.fromPropertiesPositivePattern(decimalFormatProperties));
            this.set.add(AffixHolder.fromPropertiesNegativePattern(decimalFormatProperties));
        }
    }

    static TextTrieMap<Byte> makeDigitTrie(String[] strArr) {
        boolean z;
        int i = 0;
        while (true) {
            if (i < 10) {
                String str = strArr[i];
                if (Character.charCount(Character.codePointAt(str, 0)) == str.length()) {
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
            return null;
        }
        TextTrieMap<Byte> textTrieMap = new TextTrieMap<>(false);
        for (int i2 = 0; i2 < 10; i2++) {
            textTrieMap.put(strArr[i2], Byte.valueOf((byte) i2));
        }
        return textTrieMap;
    }

    public static Number parse(String str, DecimalFormatProperties decimalFormatProperties, DecimalFormatSymbols decimalFormatSymbols) {
        ParsePosition parsePosition = threadLocalParsePosition.get();
        parsePosition.setIndex(0);
        return parse(str, parsePosition, decimalFormatProperties, decimalFormatSymbols);
    }

    public static Number parse(CharSequence charSequence, ParsePosition parsePosition, DecimalFormatProperties decimalFormatProperties, DecimalFormatSymbols decimalFormatSymbols) {
        StateItem stateItem_parse = _parse(charSequence, parsePosition, false, decimalFormatProperties, decimalFormatSymbols);
        if (stateItem_parse == null) {
            return null;
        }
        return stateItem_parse.toNumber(decimalFormatProperties);
    }

    public static CurrencyAmount parseCurrency(String str, DecimalFormatProperties decimalFormatProperties, DecimalFormatSymbols decimalFormatSymbols) throws ParseException {
        return parseCurrency(str, null, decimalFormatProperties, decimalFormatSymbols);
    }

    public static CurrencyAmount parseCurrency(CharSequence charSequence, ParsePosition parsePosition, DecimalFormatProperties decimalFormatProperties, DecimalFormatSymbols decimalFormatSymbols) throws ParseException {
        if (parsePosition == null) {
            parsePosition = threadLocalParsePosition.get();
            parsePosition.setIndex(0);
            parsePosition.setErrorIndex(-1);
        }
        StateItem stateItem_parse = _parse(charSequence, parsePosition, true, decimalFormatProperties, decimalFormatSymbols);
        if (stateItem_parse == null) {
            return null;
        }
        return stateItem_parse.toCurrencyAmount(decimalFormatProperties);
    }

    private static StateItem _parse(CharSequence charSequence, ParsePosition parsePosition, boolean z, DecimalFormatProperties decimalFormatProperties, DecimalFormatSymbols decimalFormatSymbols) {
        int i;
        ParserState parserState;
        int i2;
        if (charSequence == null || parsePosition == null || decimalFormatProperties == null || decimalFormatSymbols == null) {
            throw new IllegalArgumentException("All arguments are required for parse.");
        }
        ParseMode parseMode = decimalFormatProperties.getParseMode();
        if (parseMode == null) {
            parseMode = ParseMode.LENIENT;
        }
        boolean parseIntegerOnly = decimalFormatProperties.getParseIntegerOnly();
        boolean parseNoExponent = decimalFormatProperties.getParseNoExponent();
        int i3 = 0;
        boolean z2 = decimalFormatProperties.getGroupingSize() <= 0;
        ParserState parserStateClear = threadLocalParseState.get().clear();
        parserStateClear.properties = decimalFormatProperties;
        parserStateClear.symbols = decimalFormatSymbols;
        parserStateClear.mode = parseMode;
        parserStateClear.parseCurrency = z;
        parserStateClear.groupingMode = decimalFormatProperties.getParseGroupingMode();
        if (parserStateClear.groupingMode == null) {
            parserStateClear.groupingMode = GroupingMode.DEFAULT;
        }
        parserStateClear.caseSensitive = decimalFormatProperties.getParseCaseSensitive();
        parserStateClear.decimalCp1 = Character.codePointAt(decimalFormatSymbols.getDecimalSeparatorString(), 0);
        parserStateClear.decimalCp2 = Character.codePointAt(decimalFormatSymbols.getMonetaryDecimalSeparatorString(), 0);
        parserStateClear.groupingCp1 = Character.codePointAt(decimalFormatSymbols.getGroupingSeparatorString(), 0);
        parserStateClear.groupingCp2 = Character.codePointAt(decimalFormatSymbols.getMonetaryGroupingSeparatorString(), 0);
        parserStateClear.decimalType1 = SeparatorType.fromCp(parserStateClear.decimalCp1, parseMode);
        parserStateClear.decimalType2 = SeparatorType.fromCp(parserStateClear.decimalCp2, parseMode);
        parserStateClear.groupingType1 = SeparatorType.fromCp(parserStateClear.groupingCp1, parseMode);
        parserStateClear.groupingType2 = SeparatorType.fromCp(parserStateClear.groupingCp2, parseMode);
        parserStateClear.getNext().clear().name = StateName.BEFORE_PREFIX;
        if (parseMode == ParseMode.LENIENT || parseMode == ParseMode.STRICT) {
            parserStateClear.digitTrie = makeDigitTrie(decimalFormatSymbols.getDigitStringsLocal());
            AffixHolder.addToState(parserStateClear, decimalFormatProperties);
            if (z) {
                CurrencyAffixPatterns.addToState(decimalFormatSymbols.getULocale(), parserStateClear);
            }
        }
        if (DEBUGGING) {
            System.out.println("Parsing: " + ((Object) charSequence));
            System.out.println(decimalFormatProperties);
            System.out.println(parserStateClear);
        }
        int index = parsePosition.getIndex();
        while (true) {
            if (index < charSequence.length()) {
                int iCodePointAt = Character.codePointAt(charSequence, index);
                parserStateClear.swap();
                int i4 = i3;
                while (i4 < parserStateClear.prevLength) {
                    StateItem stateItem = parserStateClear.prevItems[i4];
                    if (DEBUGGING) {
                        System.out.println(":" + index + stateItem.id + Padder.FALLBACK_PADDING_STRING + stateItem);
                    }
                    switch (stateItem.name) {
                        case BEFORE_PREFIX:
                            if (parseMode == ParseMode.LENIENT || parseMode == ParseMode.FAST) {
                                i2 = 0;
                                acceptMinusOrPlusSign(iCodePointAt, StateName.BEFORE_PREFIX, parserStateClear, stateItem, false);
                                if (parserStateClear.length > 0 && parseMode == ParseMode.FAST) {
                                    break;
                                }
                            } else {
                                i2 = 0;
                            }
                            acceptIntegerDigit(iCodePointAt, StateName.AFTER_INTEGER_DIGIT, parserStateClear, stateItem);
                            if (parserStateClear.length > 0 && parseMode == ParseMode.FAST) {
                                break;
                            } else {
                                acceptBidi(iCodePointAt, StateName.BEFORE_PREFIX, parserStateClear, stateItem);
                                if (parserStateClear.length > 0 && parseMode == ParseMode.FAST) {
                                    break;
                                } else {
                                    acceptWhitespace(iCodePointAt, StateName.BEFORE_PREFIX, parserStateClear, stateItem);
                                    if (parserStateClear.length > 0 && parseMode == ParseMode.FAST) {
                                        break;
                                    } else {
                                        acceptPadding(iCodePointAt, StateName.BEFORE_PREFIX, parserStateClear, stateItem);
                                        if (parserStateClear.length > 0 && parseMode == ParseMode.FAST) {
                                            break;
                                        } else {
                                            acceptNan(iCodePointAt, StateName.BEFORE_SUFFIX, parserStateClear, stateItem);
                                            if (parserStateClear.length > 0 && parseMode == ParseMode.FAST) {
                                                break;
                                            } else {
                                                acceptInfinity(iCodePointAt, StateName.BEFORE_SUFFIX, parserStateClear, stateItem);
                                                if (parserStateClear.length > 0 && parseMode == ParseMode.FAST) {
                                                    break;
                                                } else if (!parseIntegerOnly) {
                                                    acceptDecimalPoint(iCodePointAt, StateName.AFTER_FRACTION_DIGIT, parserStateClear, stateItem);
                                                    if (parserStateClear.length > 0 && parseMode == ParseMode.FAST) {
                                                        break;
                                                    } else {
                                                        if (parseMode == ParseMode.LENIENT || parseMode == ParseMode.STRICT) {
                                                            acceptPrefix(iCodePointAt, StateName.AFTER_PREFIX, parserStateClear, stateItem);
                                                        }
                                                        if (parseMode != ParseMode.LENIENT && parseMode != ParseMode.FAST) {
                                                            break;
                                                        } else if (!z2) {
                                                            acceptGrouping(iCodePointAt, StateName.AFTER_INTEGER_DIGIT, parserStateClear, stateItem);
                                                            if (parserStateClear.length <= 0 || parseMode != ParseMode.FAST) {
                                                                if (z) {
                                                                    acceptCurrency(iCodePointAt, StateName.BEFORE_PREFIX, parserStateClear, stateItem);
                                                                }
                                                            }
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        case AFTER_PREFIX:
                            acceptBidi(iCodePointAt, StateName.AFTER_PREFIX, parserStateClear, stateItem);
                            acceptPadding(iCodePointAt, StateName.AFTER_PREFIX, parserStateClear, stateItem);
                            acceptNan(iCodePointAt, StateName.BEFORE_SUFFIX, parserStateClear, stateItem);
                            acceptInfinity(iCodePointAt, StateName.BEFORE_SUFFIX, parserStateClear, stateItem);
                            acceptIntegerDigit(iCodePointAt, StateName.AFTER_INTEGER_DIGIT, parserStateClear, stateItem);
                            if (!parseIntegerOnly) {
                                acceptDecimalPoint(iCodePointAt, StateName.AFTER_FRACTION_DIGIT, parserStateClear, stateItem);
                            }
                            if (parseMode == ParseMode.LENIENT || parseMode == ParseMode.FAST) {
                                acceptWhitespace(iCodePointAt, StateName.AFTER_PREFIX, parserStateClear, stateItem);
                                if (!z2) {
                                    acceptGrouping(iCodePointAt, StateName.AFTER_INTEGER_DIGIT, parserStateClear, stateItem);
                                }
                                if (z) {
                                    acceptCurrency(iCodePointAt, StateName.AFTER_PREFIX, parserStateClear, stateItem);
                                }
                            }
                            i2 = 0;
                            break;
                        case AFTER_INTEGER_DIGIT:
                            acceptIntegerDigit(iCodePointAt, StateName.AFTER_INTEGER_DIGIT, parserStateClear, stateItem);
                            if (parserStateClear.length <= 0 || parseMode != ParseMode.FAST) {
                                if (!parseIntegerOnly) {
                                    acceptDecimalPoint(iCodePointAt, StateName.AFTER_FRACTION_DIGIT, parserStateClear, stateItem);
                                    if (parserStateClear.length <= 0 || parseMode != ParseMode.FAST) {
                                        if (!z2) {
                                            acceptGrouping(iCodePointAt, StateName.AFTER_INTEGER_DIGIT, parserStateClear, stateItem);
                                            if (parserStateClear.length <= 0 || parseMode != ParseMode.FAST) {
                                                acceptBidi(iCodePointAt, StateName.BEFORE_SUFFIX, parserStateClear, stateItem);
                                                if (parserStateClear.length <= 0 || parseMode != ParseMode.FAST) {
                                                    acceptPadding(iCodePointAt, StateName.BEFORE_SUFFIX, parserStateClear, stateItem);
                                                    if (parserStateClear.length <= 0 || parseMode != ParseMode.FAST) {
                                                        if (!parseNoExponent) {
                                                            acceptExponentSeparator(iCodePointAt, StateName.AFTER_EXPONENT_SEPARATOR, parserStateClear, stateItem);
                                                            if (parserStateClear.length <= 0 || parseMode != ParseMode.FAST) {
                                                                if (parseMode == ParseMode.LENIENT || parseMode == ParseMode.STRICT) {
                                                                    acceptSuffix(iCodePointAt, StateName.AFTER_SUFFIX, parserStateClear, stateItem);
                                                                }
                                                                if (parseMode == ParseMode.LENIENT || parseMode == ParseMode.FAST) {
                                                                    acceptWhitespace(iCodePointAt, StateName.BEFORE_SUFFIX, parserStateClear, stateItem);
                                                                    if ((parserStateClear.length <= 0 || parseMode != ParseMode.FAST) && ((parserStateClear.length <= 0 || parseMode != ParseMode.FAST) && z)) {
                                                                        acceptCurrency(iCodePointAt, StateName.BEFORE_SUFFIX, parserStateClear, stateItem);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            i2 = 0;
                            break;
                        case AFTER_FRACTION_DIGIT:
                            acceptFractionDigit(iCodePointAt, StateName.AFTER_FRACTION_DIGIT, parserStateClear, stateItem);
                            if (parserStateClear.length <= 0 || parseMode != ParseMode.FAST) {
                                acceptBidi(iCodePointAt, StateName.BEFORE_SUFFIX, parserStateClear, stateItem);
                                if (parserStateClear.length <= 0 || parseMode != ParseMode.FAST) {
                                    acceptPadding(iCodePointAt, StateName.BEFORE_SUFFIX, parserStateClear, stateItem);
                                    if (parserStateClear.length <= 0 || parseMode != ParseMode.FAST) {
                                        if (!parseNoExponent) {
                                            acceptExponentSeparator(iCodePointAt, StateName.AFTER_EXPONENT_SEPARATOR, parserStateClear, stateItem);
                                            if (parserStateClear.length <= 0 || parseMode != ParseMode.FAST) {
                                                if (parseMode == ParseMode.LENIENT || parseMode == ParseMode.STRICT) {
                                                    acceptSuffix(iCodePointAt, StateName.AFTER_SUFFIX, parserStateClear, stateItem);
                                                }
                                                if (parseMode == ParseMode.LENIENT || parseMode == ParseMode.FAST) {
                                                    acceptWhitespace(iCodePointAt, StateName.BEFORE_SUFFIX, parserStateClear, stateItem);
                                                    if ((parserStateClear.length <= 0 || parseMode != ParseMode.FAST) && ((parserStateClear.length <= 0 || parseMode != ParseMode.FAST) && z)) {
                                                        acceptCurrency(iCodePointAt, StateName.BEFORE_SUFFIX, parserStateClear, stateItem);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            i2 = 0;
                            break;
                        case AFTER_EXPONENT_SEPARATOR:
                            acceptBidi(iCodePointAt, StateName.AFTER_EXPONENT_SEPARATOR, parserStateClear, stateItem);
                            acceptMinusOrPlusSign(iCodePointAt, StateName.AFTER_EXPONENT_SEPARATOR, parserStateClear, stateItem, true);
                            acceptExponentDigit(iCodePointAt, StateName.AFTER_EXPONENT_DIGIT, parserStateClear, stateItem);
                            i2 = 0;
                            break;
                        case AFTER_EXPONENT_DIGIT:
                            acceptBidi(iCodePointAt, StateName.BEFORE_SUFFIX_SEEN_EXPONENT, parserStateClear, stateItem);
                            acceptPadding(iCodePointAt, StateName.BEFORE_SUFFIX_SEEN_EXPONENT, parserStateClear, stateItem);
                            acceptExponentDigit(iCodePointAt, StateName.AFTER_EXPONENT_DIGIT, parserStateClear, stateItem);
                            if (parseMode == ParseMode.LENIENT || parseMode == ParseMode.STRICT) {
                                acceptSuffix(iCodePointAt, StateName.AFTER_SUFFIX, parserStateClear, stateItem);
                            }
                            if (parseMode == ParseMode.LENIENT || parseMode == ParseMode.FAST) {
                                acceptWhitespace(iCodePointAt, StateName.BEFORE_SUFFIX_SEEN_EXPONENT, parserStateClear, stateItem);
                                if (z) {
                                    acceptCurrency(iCodePointAt, StateName.BEFORE_SUFFIX_SEEN_EXPONENT, parserStateClear, stateItem);
                                }
                            }
                            i2 = 0;
                            break;
                        case BEFORE_SUFFIX:
                            acceptBidi(iCodePointAt, StateName.BEFORE_SUFFIX, parserStateClear, stateItem);
                            acceptPadding(iCodePointAt, StateName.BEFORE_SUFFIX, parserStateClear, stateItem);
                            if (!parseNoExponent) {
                                acceptExponentSeparator(iCodePointAt, StateName.AFTER_EXPONENT_SEPARATOR, parserStateClear, stateItem);
                            }
                            if (parseMode == ParseMode.LENIENT || parseMode == ParseMode.STRICT) {
                                acceptSuffix(iCodePointAt, StateName.AFTER_SUFFIX, parserStateClear, stateItem);
                            }
                            if (parseMode == ParseMode.LENIENT || parseMode == ParseMode.FAST) {
                                acceptWhitespace(iCodePointAt, StateName.BEFORE_SUFFIX, parserStateClear, stateItem);
                                if (z) {
                                    acceptCurrency(iCodePointAt, StateName.BEFORE_SUFFIX, parserStateClear, stateItem);
                                }
                            }
                            i2 = 0;
                            break;
                        case BEFORE_SUFFIX_SEEN_EXPONENT:
                            acceptBidi(iCodePointAt, StateName.BEFORE_SUFFIX_SEEN_EXPONENT, parserStateClear, stateItem);
                            acceptPadding(iCodePointAt, StateName.BEFORE_SUFFIX_SEEN_EXPONENT, parserStateClear, stateItem);
                            if (parseMode == ParseMode.LENIENT || parseMode == ParseMode.STRICT) {
                                acceptSuffix(iCodePointAt, StateName.AFTER_SUFFIX, parserStateClear, stateItem);
                            }
                            if (parseMode == ParseMode.LENIENT || parseMode == ParseMode.FAST) {
                                acceptWhitespace(iCodePointAt, StateName.BEFORE_SUFFIX_SEEN_EXPONENT, parserStateClear, stateItem);
                                if (z) {
                                    acceptCurrency(iCodePointAt, StateName.BEFORE_SUFFIX_SEEN_EXPONENT, parserStateClear, stateItem);
                                }
                            }
                            i2 = 0;
                            break;
                        case AFTER_SUFFIX:
                            if ((parseMode == ParseMode.LENIENT || parseMode == ParseMode.FAST) && z) {
                                acceptBidi(iCodePointAt, StateName.AFTER_SUFFIX, parserStateClear, stateItem);
                                acceptPadding(iCodePointAt, StateName.AFTER_SUFFIX, parserStateClear, stateItem);
                                acceptWhitespace(iCodePointAt, StateName.AFTER_SUFFIX, parserStateClear, stateItem);
                                if (z) {
                                    acceptCurrency(iCodePointAt, StateName.AFTER_SUFFIX, parserStateClear, stateItem);
                                }
                            }
                            i2 = 0;
                            break;
                        case INSIDE_CURRENCY:
                            acceptCurrencyOffset(iCodePointAt, parserStateClear, stateItem);
                            i2 = 0;
                            break;
                        case INSIDE_DIGIT:
                            acceptDigitTrieOffset(iCodePointAt, parserStateClear, stateItem);
                            i2 = 0;
                            break;
                        case INSIDE_STRING:
                            acceptStringOffset(iCodePointAt, parserStateClear, stateItem);
                            i2 = 0;
                            break;
                        case INSIDE_AFFIX_PATTERN:
                            acceptAffixPatternOffset(iCodePointAt, parserStateClear, stateItem);
                            i2 = 0;
                            break;
                        default:
                            i2 = 0;
                            break;
                    }
                    i4++;
                    i3 = i2;
                }
                i = i3;
                if (parserStateClear.length == 0) {
                    parserStateClear.swapBack();
                } else {
                    index += Character.charCount(iCodePointAt);
                    i3 = i;
                }
            } else {
                i = i3;
            }
        }
        if (parserStateClear.length == 0) {
            if (DEBUGGING) {
                System.out.println("No matches found");
                System.out.println("- - - - - - - - - -");
            }
            return null;
        }
        StateItem stateItem2 = null;
        int i5 = i;
        while (i5 < parserStateClear.length) {
            StateItem stateItem3 = parserStateClear.items[i5];
            if (DEBUGGING) {
                System.out.println(":end " + stateItem3);
            }
            if (!stateItem3.hasNumber()) {
                if (DEBUGGING) {
                    System.out.println("-> rejected due to no number value");
                }
            } else {
                if (parseMode == ParseMode.STRICT) {
                    int i6 = (stateItem3.sawPrefix || (stateItem3.affix != null && stateItem3.affix.p.isEmpty())) ? 1 : i;
                    int i7 = (stateItem3.sawSuffix || (stateItem3.affix != null && stateItem3.affix.s.isEmpty())) ? 1 : i;
                    int i8 = (parserStateClear.affixHolders.contains(AffixHolder.EMPTY_POSITIVE) || parserStateClear.affixHolders.contains(AffixHolder.EMPTY_NEGATIVE)) ? 1 : i;
                    if ((i6 == 0 || i7 == 0) && (i6 != 0 || i7 != 0 || i8 == 0)) {
                        parserState = parserStateClear;
                        if (DEBUGGING) {
                            System.out.println("-> rejected due to mismatched prefix/suffix");
                        }
                    } else if (decimalFormatProperties.getMinimumExponentDigits() > 0 && !stateItem3.sawExponentDigit) {
                        if (DEBUGGING) {
                            System.out.println("-> reject due to lack of exponent");
                        }
                    } else {
                        int groupingSize = decimalFormatProperties.getGroupingSize();
                        int secondaryGroupingSize = decimalFormatProperties.getSecondaryGroupingSize();
                        if (groupingSize <= 0) {
                            groupingSize = secondaryGroupingSize;
                        }
                        if (secondaryGroupingSize <= 0) {
                            secondaryGroupingSize = groupingSize;
                        }
                        long j = stateItem3.groupingWidths;
                        int iNumberOfLeadingZeros = 16 - (Long.numberOfLeadingZeros(j) / 4);
                        while (iNumberOfLeadingZeros > 1 && (j & 15) == 0) {
                            if (stateItem3.sawDecimalPoint) {
                                if (DEBUGGING) {
                                    System.out.println("-> rejected due to decimal point after grouping");
                                }
                            } else {
                                j >>>= 4;
                                iNumberOfLeadingZeros--;
                            }
                        }
                        if (groupingSize > 0 && iNumberOfLeadingZeros > 1) {
                            parserState = parserStateClear;
                            if ((j & 15) == groupingSize) {
                                int i9 = iNumberOfLeadingZeros - 1;
                                long j2 = secondaryGroupingSize;
                                if (((j >>> (i9 * 4)) & 15) > j2) {
                                    if (DEBUGGING) {
                                        System.out.println("-> rejected due to final grouping violation");
                                    }
                                } else {
                                    for (int i10 = 1; i10 < i9; i10++) {
                                        if (((j >>> (i10 * 4)) & 15) != j2) {
                                            if (DEBUGGING) {
                                                System.out.println("-> rejected due to inner grouping violation");
                                            }
                                        }
                                    }
                                }
                            } else if (DEBUGGING) {
                                System.out.println("-> rejected due to first grouping violation");
                            }
                        }
                        if (!decimalFormatProperties.getDecimalPatternMatchRequired()) {
                        }
                    }
                } else {
                    parserState = parserStateClear;
                    if (!decimalFormatProperties.getDecimalPatternMatchRequired()) {
                        if (stateItem3.sawDecimalPoint != (decimalFormatProperties.getDecimalSeparatorAlwaysShown() || decimalFormatProperties.getMaximumFractionDigits() != 0)) {
                            if (DEBUGGING) {
                                System.out.println("-> rejected due to decimal point violation");
                            }
                        } else if (z && !stateItem3.sawCurrency) {
                            if (DEBUGGING) {
                                System.out.println("-> rejected due to lack of currency");
                            }
                        } else if (stateItem2 == null || stateItem3.score > stateItem2.score || stateItem3.trailingCount < stateItem2.trailingCount) {
                            stateItem2 = stateItem3;
                        }
                    }
                }
                i5++;
                parserStateClear = parserState;
                i = 0;
            }
            parserState = parserStateClear;
            i5++;
            parserStateClear = parserState;
            i = 0;
        }
        if (DEBUGGING) {
            System.out.println("- - - - - - - - - -");
        }
        if (stateItem2 != null) {
            parsePosition.setIndex(index - stateItem2.trailingCount);
            return stateItem2;
        }
        parsePosition.setErrorIndex(index);
        return null;
    }

    private static void acceptWhitespace(int i, StateName stateName, ParserState parserState, StateItem stateItem) {
        if (UNISET_WHITESPACE.contains(i)) {
            parserState.getNext().copyFrom(stateItem, stateName, i);
        }
    }

    private static void acceptBidi(int i, StateName stateName, ParserState parserState, StateItem stateItem) {
        if (UNISET_BIDI.contains(i)) {
            parserState.getNext().copyFrom(stateItem, stateName, i);
        }
    }

    private static void acceptPadding(int i, StateName stateName, ParserState parserState, StateItem stateItem) {
        String padString = parserState.properties.getPadString();
        if (padString != null && padString.length() != 0 && i == Character.codePointAt(padString, 0)) {
            parserState.getNext().copyFrom(stateItem, stateName, i);
        }
    }

    private static void acceptIntegerDigit(int i, StateName stateName, ParserState parserState, StateItem stateItem) {
        acceptDigitHelper(i, stateName, parserState, stateItem, DigitType.INTEGER);
    }

    private static void acceptFractionDigit(int i, StateName stateName, ParserState parserState, StateItem stateItem) {
        acceptDigitHelper(i, stateName, parserState, stateItem, DigitType.FRACTION);
    }

    private static void acceptExponentDigit(int i, StateName stateName, ParserState parserState, StateItem stateItem) {
        acceptDigitHelper(i, stateName, parserState, stateItem, DigitType.EXPONENT);
    }

    private static void acceptDigitHelper(int i, StateName stateName, ParserState parserState, StateItem stateItem, DigitType digitType) {
        StateItem stateItemCopyFrom;
        byte bDigit = (byte) UCharacter.digit(i, 10);
        if (bDigit >= 0) {
            stateItemCopyFrom = parserState.getNext().copyFrom(stateItem, stateName, -1);
        } else {
            stateItemCopyFrom = null;
        }
        if (bDigit < 0 && (parserState.mode == ParseMode.LENIENT || parserState.mode == ParseMode.STRICT)) {
            if (parserState.digitTrie == null) {
                byte b = bDigit;
                for (byte b2 = 0; b2 < 10; b2 = (byte) (b2 + 1)) {
                    if (i == Character.codePointAt(parserState.symbols.getDigitStringsLocal()[b2], 0)) {
                        stateItemCopyFrom = parserState.getNext().copyFrom(stateItem, stateName, -1);
                        b = b2;
                    }
                }
                bDigit = b;
            } else {
                acceptDigitTrie(i, stateName, parserState, stateItem, digitType);
            }
        }
        recordDigit(stateItemCopyFrom, bDigit, digitType);
    }

    private static void recordDigit(StateItem stateItem, byte b, DigitType digitType) {
        if (stateItem == null) {
            return;
        }
        stateItem.appendDigit(b, digitType);
        if (digitType == DigitType.INTEGER && (stateItem.groupingWidths & 15) < 15) {
            stateItem.groupingWidths++;
        }
    }

    private static void acceptMinusOrPlusSign(int i, StateName stateName, ParserState parserState, StateItem stateItem, boolean z) {
        acceptMinusSign(i, stateName, null, parserState, stateItem, z);
        acceptPlusSign(i, stateName, null, parserState, stateItem, z);
    }

    private static long acceptMinusSign(int i, StateName stateName, StateName stateName2, ParserState parserState, StateItem stateItem, boolean z) {
        if (UNISET_MINUS.contains(i)) {
            StateItem stateItemCopyFrom = parserState.getNext().copyFrom(stateItem, stateName, -1);
            stateItemCopyFrom.returnTo1 = stateName2;
            if (z) {
                stateItemCopyFrom.sawNegativeExponent = true;
            } else {
                stateItemCopyFrom.sawNegative = true;
            }
            return 1 << parserState.lastInsertedIndex();
        }
        return 0L;
    }

    private static long acceptPlusSign(int i, StateName stateName, StateName stateName2, ParserState parserState, StateItem stateItem, boolean z) {
        if (UNISET_PLUS.contains(i)) {
            parserState.getNext().copyFrom(stateItem, stateName, -1).returnTo1 = stateName2;
            return 1 << parserState.lastInsertedIndex();
        }
        return 0L;
    }

    private static void acceptGrouping(int i, StateName stateName, ParserState parserState, StateItem stateItem) {
        if (stateItem.groupingCp == -1) {
            SeparatorType separatorTypeFromCp = SeparatorType.fromCp(i, parserState.mode);
            if (i != parserState.groupingCp1 && i != parserState.groupingCp2) {
                if (separatorTypeFromCp == SeparatorType.UNKNOWN) {
                    return;
                }
                if (parserState.groupingMode == GroupingMode.RESTRICTED) {
                    if (separatorTypeFromCp != parserState.groupingType1 || separatorTypeFromCp != parserState.groupingType2) {
                        return;
                    }
                } else {
                    if (separatorTypeFromCp == SeparatorType.COMMA_LIKE && (parserState.decimalType1 == SeparatorType.COMMA_LIKE || parserState.decimalType2 == SeparatorType.COMMA_LIKE)) {
                        return;
                    }
                    if (separatorTypeFromCp == SeparatorType.PERIOD_LIKE && (parserState.decimalType1 == SeparatorType.PERIOD_LIKE || parserState.decimalType2 == SeparatorType.PERIOD_LIKE)) {
                        return;
                    }
                }
            }
            StateItem stateItemCopyFrom = parserState.getNext().copyFrom(stateItem, stateName, i);
            stateItemCopyFrom.groupingCp = i;
            stateItemCopyFrom.groupingWidths <<= 4;
            return;
        }
        if (i == stateItem.groupingCp) {
            parserState.getNext().copyFrom(stateItem, stateName, i).groupingWidths <<= 4;
        }
    }

    private static void acceptDecimalPoint(int i, StateName stateName, ParserState parserState, StateItem stateItem) {
        if (i == stateItem.groupingCp) {
            return;
        }
        SeparatorType separatorTypeFromCp = SeparatorType.fromCp(i, parserState.mode);
        if (separatorTypeFromCp != parserState.decimalType1 && separatorTypeFromCp != parserState.decimalType2) {
            return;
        }
        if ((separatorTypeFromCp == SeparatorType.OTHER_GROUPING || separatorTypeFromCp == SeparatorType.UNKNOWN) && i != parserState.decimalCp1 && i != parserState.decimalCp2) {
            return;
        }
        parserState.getNext().copyFrom(stateItem, stateName, -1).sawDecimalPoint = true;
    }

    private static void acceptNan(int i, StateName stateName, ParserState parserState, StateItem stateItem) {
        long jAcceptString = acceptString(i, stateName, null, parserState, stateItem, parserState.symbols.getNaN(), 0, false);
        int iNumberOfTrailingZeros = Long.numberOfTrailingZeros(jAcceptString);
        while (true) {
            long j = 1 << iNumberOfTrailingZeros;
            if (j <= jAcceptString) {
                if ((j & jAcceptString) != 0) {
                    parserState.getItem(iNumberOfTrailingZeros).sawNaN = true;
                }
                iNumberOfTrailingZeros++;
            } else {
                return;
            }
        }
    }

    private static void acceptInfinity(int i, StateName stateName, ParserState parserState, StateItem stateItem) {
        long jAcceptString = acceptString(i, stateName, null, parserState, stateItem, parserState.symbols.getInfinity(), 0, false);
        int iNumberOfTrailingZeros = Long.numberOfTrailingZeros(jAcceptString);
        while (true) {
            long j = 1 << iNumberOfTrailingZeros;
            if (j <= jAcceptString) {
                if ((j & jAcceptString) != 0) {
                    parserState.getItem(iNumberOfTrailingZeros).sawInfinity = true;
                }
                iNumberOfTrailingZeros++;
            } else {
                return;
            }
        }
    }

    private static void acceptExponentSeparator(int i, StateName stateName, ParserState parserState, StateItem stateItem) {
        acceptString(i, stateName, null, parserState, stateItem, parserState.symbols.getExponentSeparator(), 0, true);
    }

    private static void acceptPrefix(int i, StateName stateName, ParserState parserState, StateItem stateItem) {
        Iterator<AffixHolder> it = parserState.affixHolders.iterator();
        while (it.hasNext()) {
            acceptAffixHolder(i, stateName, parserState, stateItem, it.next(), true);
        }
    }

    private static void acceptSuffix(int i, StateName stateName, ParserState parserState, StateItem stateItem) {
        if (stateItem.affix != null) {
            acceptAffixHolder(i, stateName, parserState, stateItem, stateItem.affix, false);
            return;
        }
        Iterator<AffixHolder> it = parserState.affixHolders.iterator();
        while (it.hasNext()) {
            acceptAffixHolder(i, stateName, parserState, stateItem, it.next(), false);
        }
    }

    private static void acceptAffixHolder(int i, StateName stateName, ParserState parserState, StateItem stateItem, AffixHolder affixHolder, boolean z) {
        long jAcceptAffixPattern;
        if (affixHolder == null) {
            return;
        }
        String str = z ? affixHolder.p : affixHolder.s;
        if (affixHolder.strings) {
            jAcceptAffixPattern = acceptString(i, stateName, null, parserState, stateItem, str, 0, false);
        } else {
            jAcceptAffixPattern = acceptAffixPattern(i, stateName, parserState, stateItem, str, AffixUtils.nextToken(0L, str));
        }
        int iNumberOfTrailingZeros = Long.numberOfTrailingZeros(jAcceptAffixPattern);
        while (true) {
            long j = 1 << iNumberOfTrailingZeros;
            if (j > jAcceptAffixPattern) {
                return;
            }
            if ((j & jAcceptAffixPattern) != 0) {
                StateItem item = parserState.getItem(iNumberOfTrailingZeros);
                item.affix = affixHolder;
                if (z) {
                    item.sawPrefix = true;
                }
                if (!z) {
                    item.sawSuffix = true;
                }
                if (affixHolder.negative) {
                    item.sawNegative = true;
                }
                item.score += 10;
                if (!affixHolder.negative) {
                    item.score++;
                }
                if (!item.sawPrefix && affixHolder.p.isEmpty()) {
                    item.score += 5;
                }
                if (!item.sawSuffix && affixHolder.s.isEmpty()) {
                    item.score += 5;
                }
            }
            iNumberOfTrailingZeros++;
        }
    }

    private static long acceptStringOffset(int i, ParserState parserState, StateItem stateItem) {
        return acceptString(i, stateItem.returnTo1, stateItem.returnTo2, parserState, stateItem, stateItem.currentString, stateItem.currentOffset, stateItem.currentTrailing);
    }

    private static long acceptString(int i, StateName stateName, StateName stateName2, ParserState parserState, StateItem stateItem, CharSequence charSequence, int i2, boolean z) {
        if (charSequence == null || charSequence.length() == 0) {
            return 0L;
        }
        return acceptStringOrAffixPatternWithIgnorables(i, stateName, stateName2, parserState, stateItem, charSequence, i2, z, true);
    }

    private static long acceptStringNonIgnorable(int i, StateName stateName, StateName stateName2, ParserState parserState, StateItem stateItem, CharSequence charSequence, boolean z, int i2, long j, long j2) {
        int i3 = (int) j;
        int i4 = (int) j2;
        if (!codePointEquals(i2, i, parserState)) {
            return 0L;
        }
        long jAcceptStringHelper = i3 < charSequence.length() ? 0 | acceptStringHelper(i, stateName, stateName2, parserState, stateItem, charSequence, i3, z) : 0L;
        if (i4 >= charSequence.length()) {
            return jAcceptStringHelper | acceptStringHelper(i, stateName, stateName2, parserState, stateItem, charSequence, i4, z);
        }
        return jAcceptStringHelper;
    }

    private static long acceptStringHelper(int i, StateName stateName, StateName stateName2, ParserState parserState, StateItem stateItem, CharSequence charSequence, int i2, boolean z) {
        StateItem stateItemCopyFrom = parserState.getNext().copyFrom(stateItem, null, i);
        stateItemCopyFrom.score++;
        if (i2 < charSequence.length()) {
            stateItemCopyFrom.name = StateName.INSIDE_STRING;
            stateItemCopyFrom.returnTo1 = stateName;
            stateItemCopyFrom.returnTo2 = stateName2;
            stateItemCopyFrom.currentString = charSequence;
            stateItemCopyFrom.currentOffset = i2;
            stateItemCopyFrom.currentTrailing = z;
        } else {
            stateItemCopyFrom.name = stateName;
            if (!z) {
                stateItemCopyFrom.trailingCount = 0;
            }
            stateItemCopyFrom.returnTo1 = stateName2;
            stateItemCopyFrom.returnTo2 = null;
        }
        return 1 << parserState.lastInsertedIndex();
    }

    private static long acceptAffixPatternOffset(int i, ParserState parserState, StateItem stateItem) {
        return acceptAffixPattern(i, stateItem.returnTo1, parserState, stateItem, stateItem.currentAffixPattern, stateItem.currentStepwiseParserTag);
    }

    private static long acceptAffixPattern(int i, StateName stateName, ParserState parserState, StateItem stateItem, CharSequence charSequence, long j) {
        if (charSequence == null || charSequence.length() == 0) {
            return 0L;
        }
        return acceptStringOrAffixPatternWithIgnorables(i, stateName, null, parserState, stateItem, charSequence, j, false, false);
    }

    private static long acceptAffixPatternNonIgnorable(int i, StateName stateName, ParserState parserState, StateItem stateItem, CharSequence charSequence, int i2, long j, long j2) {
        boolean z;
        boolean z2;
        String str;
        boolean z3;
        int i3;
        long jAcceptCurrency;
        long j3;
        int iNumberOfTrailingZeros;
        long j4;
        String perMillString;
        int i4 = -1;
        if (i2 < 0) {
            if (i2 != -15) {
                switch (i2) {
                    case AffixUtils.TYPE_CURRENCY_QUINT:
                    case AffixUtils.TYPE_CURRENCY_QUAD:
                    case AffixUtils.TYPE_CURRENCY_TRIPLE:
                    case AffixUtils.TYPE_CURRENCY_DOUBLE:
                    case AffixUtils.TYPE_CURRENCY_SINGLE:
                        z3 = true;
                        z = false;
                        z2 = false;
                        str = null;
                        break;
                    case AffixUtils.TYPE_PERMILLE:
                        perMillString = parserState.symbols.getPerMillString();
                        if (perMillString.length() == 1 && perMillString.charAt(0) == 8240) {
                            str = perMillString;
                        } else {
                            str = perMillString;
                            i4 = 8240;
                        }
                        z = false;
                        z2 = false;
                        z3 = false;
                        break;
                    case AffixUtils.TYPE_PERCENT:
                        perMillString = parserState.symbols.getPercentString();
                        if (perMillString.length() != 1 || perMillString.charAt(0) != '%') {
                            str = perMillString;
                            i4 = 37;
                        }
                        z = false;
                        z2 = false;
                        z3 = false;
                        break;
                    case -2:
                        z2 = true;
                        z = false;
                        z3 = false;
                        str = null;
                        break;
                    case -1:
                        z = true;
                        z2 = false;
                        break;
                    default:
                        throw new AssertionError();
                }
            }
            if (i4 >= 0 || !codePointEquals(i, i4, parserState)) {
                i3 = 0;
                jAcceptCurrency = 0;
            } else {
                if (j >= 0) {
                    i3 = 0;
                    jAcceptCurrency = 0 | acceptAffixPatternHelper(i, stateName, parserState, stateItem, charSequence, j);
                } else {
                    i3 = 0;
                    jAcceptCurrency = 0;
                }
                if (j2 < 0) {
                    jAcceptCurrency |= acceptAffixPatternHelper(i, stateName, parserState, stateItem, charSequence, j2);
                }
            }
            if (z) {
                if (j >= 0) {
                    jAcceptCurrency |= acceptMinusSign(i, StateName.INSIDE_AFFIX_PATTERN, stateName, parserState, stateItem, false);
                }
                if (j2 < 0) {
                    jAcceptCurrency |= acceptMinusSign(i, stateName, null, parserState, stateItem, false);
                }
                if (jAcceptCurrency == 0) {
                    String minusSignString = parserState.symbols.getMinusSignString();
                    int iCodePointAt = Character.codePointAt(minusSignString, i3);
                    if (minusSignString.length() != Character.charCount(iCodePointAt) || !UNISET_MINUS.contains(iCodePointAt)) {
                        str = minusSignString;
                    }
                }
            }
            if (z2) {
                if (j >= 0) {
                    jAcceptCurrency |= acceptPlusSign(i, StateName.INSIDE_AFFIX_PATTERN, stateName, parserState, stateItem, false);
                }
                if (j2 < 0) {
                    jAcceptCurrency |= acceptPlusSign(i, stateName, null, parserState, stateItem, false);
                }
                if (jAcceptCurrency == 0) {
                    String plusSignString = parserState.symbols.getPlusSignString();
                    int iCodePointAt2 = Character.codePointAt(plusSignString, i3);
                    if (plusSignString.length() != Character.charCount(iCodePointAt2) || !UNISET_MINUS.contains(iCodePointAt2)) {
                        str = plusSignString;
                    }
                }
            }
            if (str != null) {
                if (j >= 0) {
                    jAcceptCurrency |= acceptString(i, StateName.INSIDE_AFFIX_PATTERN, stateName, parserState, stateItem, str, 0, false);
                }
                if (j2 < 0) {
                    jAcceptCurrency |= acceptString(i, stateName, null, parserState, stateItem, str, 0, false);
                }
            }
            if (z3) {
                if (j >= 0) {
                    jAcceptCurrency |= acceptCurrency(i, StateName.INSIDE_AFFIX_PATTERN, stateName, parserState, stateItem);
                }
                if (j2 < 0) {
                    jAcceptCurrency |= acceptCurrency(i, stateName, null, parserState, stateItem);
                }
            }
            j3 = jAcceptCurrency;
            iNumberOfTrailingZeros = Long.numberOfTrailingZeros(j3);
            while (true) {
                j4 = 1 << iNumberOfTrailingZeros;
                if (j4 <= j3) {
                    return j3;
                }
                if ((j4 & j3) != 0) {
                    parserState.getItem(iNumberOfTrailingZeros).currentAffixPattern = charSequence;
                    parserState.getItem(iNumberOfTrailingZeros).currentStepwiseParserTag = j;
                }
                iNumberOfTrailingZeros++;
            }
        } else {
            i4 = i2;
            z = false;
            z2 = false;
        }
        z3 = z2;
        str = null;
        if (i4 >= 0) {
            i3 = 0;
            jAcceptCurrency = 0;
        }
        if (z) {
        }
        if (z2) {
        }
        if (str != null) {
        }
        if (z3) {
        }
        j3 = jAcceptCurrency;
        iNumberOfTrailingZeros = Long.numberOfTrailingZeros(j3);
        while (true) {
            j4 = 1 << iNumberOfTrailingZeros;
            if (j4 <= j3) {
            }
            iNumberOfTrailingZeros++;
        }
    }

    private static long acceptAffixPatternHelper(int i, StateName stateName, ParserState parserState, StateItem stateItem, CharSequence charSequence, long j) {
        StateItem stateItemCopyFrom = parserState.getNext().copyFrom(stateItem, null, i);
        stateItemCopyFrom.score++;
        if (j >= 0) {
            stateItemCopyFrom.name = StateName.INSIDE_AFFIX_PATTERN;
            stateItemCopyFrom.returnTo1 = stateName;
            stateItemCopyFrom.currentAffixPattern = charSequence;
            stateItemCopyFrom.currentStepwiseParserTag = j;
        } else {
            stateItemCopyFrom.name = stateName;
            stateItemCopyFrom.trailingCount = 0;
            stateItemCopyFrom.returnTo1 = null;
        }
        return 1 << parserState.lastInsertedIndex();
    }

    private static long acceptStringOrAffixPatternWithIgnorables(int i, StateName stateName, StateName stateName2, ParserState parserState, StateItem stateItem, CharSequence charSequence, long j, boolean z, boolean z2) {
        long j2;
        int typeOrCp;
        long jNextToken;
        int i2;
        long j3;
        long jNextToken2;
        long j4;
        long jAcceptAffixPatternHelper;
        long jAcceptAffixPatternHelper2;
        if (z2) {
            j2 = j;
            typeOrCp = Character.codePointAt(charSequence, (int) j2);
        } else {
            j2 = j;
            typeOrCp = AffixUtils.getTypeOrCp(j);
        }
        if (isIgnorable(typeOrCp, parserState)) {
            long j5 = j2;
            int typeOrCp2 = typeOrCp;
            long j6 = 0;
            while (true) {
                if (z2) {
                    jNextToken2 = ((long) Character.charCount(typeOrCp2)) + j5;
                } else {
                    jNextToken2 = AffixUtils.nextToken(j5, charSequence);
                }
                jNextToken = jNextToken2;
                if (j6 == 0) {
                    j6 = jNextToken;
                }
                if (z2) {
                    if (jNextToken >= charSequence.length()) {
                        break;
                    }
                    if (!z2) {
                        typeOrCp2 = Character.codePointAt(charSequence, (int) jNextToken);
                    } else {
                        typeOrCp2 = AffixUtils.getTypeOrCp(jNextToken);
                    }
                    if (isIgnorable(typeOrCp2, parserState)) {
                        break;
                    }
                    j5 = jNextToken;
                } else {
                    if (jNextToken < 0) {
                        break;
                    }
                    if (!z2) {
                    }
                    if (isIgnorable(typeOrCp2, parserState)) {
                    }
                }
            }
            if (typeOrCp2 == Integer.MIN_VALUE) {
                if (!codePointEquals(i, typeOrCp, parserState)) {
                    return 0L;
                }
                if (z2) {
                    j4 = j6;
                    jAcceptAffixPatternHelper = acceptStringHelper(i, stateName, stateName2, parserState, stateItem, charSequence, (int) j6, z);
                } else {
                    j4 = j6;
                    jAcceptAffixPatternHelper = acceptAffixPatternHelper(i, stateName, parserState, stateItem, charSequence, j4);
                }
                long j7 = 0 | jAcceptAffixPatternHelper;
                if (j4 != jNextToken) {
                    if (z2) {
                        jAcceptAffixPatternHelper2 = acceptStringHelper(i, stateName, stateName2, parserState, stateItem, charSequence, (int) jNextToken, z);
                    } else {
                        jAcceptAffixPatternHelper2 = acceptAffixPatternHelper(i, stateName, parserState, stateItem, charSequence, jNextToken);
                    }
                    return j7 | jAcceptAffixPatternHelper2;
                }
                return j7;
            }
            if (isIgnorable(i, parserState)) {
                if (z2) {
                    return acceptStringHelper(i, stateName, stateName2, parserState, stateItem, charSequence, (int) j5, z);
                }
                return acceptAffixPatternHelper(i, stateName, parserState, stateItem, charSequence, j5);
            }
            i2 = typeOrCp2;
        } else {
            jNextToken = j2;
            i2 = typeOrCp;
        }
        int typeOrCp3 = i2;
        long j8 = 0;
        while (true) {
            if (z2) {
                jNextToken += (long) Character.charCount(typeOrCp3);
            } else {
                jNextToken = AffixUtils.nextToken(jNextToken, charSequence);
            }
            j3 = j8 == 0 ? jNextToken : j8;
            if (z2) {
                if (jNextToken >= charSequence.length()) {
                    break;
                }
                if (z2) {
                    typeOrCp3 = Character.codePointAt(charSequence, (int) jNextToken);
                } else {
                    typeOrCp3 = AffixUtils.getTypeOrCp(jNextToken);
                }
                if (!isIgnorable(typeOrCp3, parserState)) {
                    break;
                }
                j8 = j3;
            } else if (jNextToken < 0) {
                break;
            }
        }
        if (z2) {
            return acceptStringNonIgnorable(i, stateName, stateName2, parserState, stateItem, charSequence, z, i2, j3, jNextToken);
        }
        return acceptAffixPatternNonIgnorable(i, stateName, parserState, stateItem, charSequence, i2, j3, jNextToken);
    }

    private static void acceptCurrency(int i, StateName stateName, ParserState parserState, StateItem stateItem) {
        acceptCurrency(i, stateName, null, parserState, stateItem);
    }

    private static long acceptCurrency(int i, StateName stateName, StateName stateName2, ParserState parserState, StateItem stateItem) {
        String currencySymbol;
        String internationalCurrencySymbol;
        if (stateItem.sawCurrency) {
            return 0L;
        }
        Currency currency = parserState.properties.getCurrency();
        if (currency != null) {
            String name = currency.getName(parserState.symbols.getULocale(), 0, (boolean[]) null);
            internationalCurrencySymbol = currency.getCurrencyCode();
            currencySymbol = name;
        } else {
            parserState.symbols.getCurrency();
            currencySymbol = parserState.symbols.getCurrencySymbol();
            internationalCurrencySymbol = parserState.symbols.getInternationalCurrencySymbol();
        }
        long jAcceptString = 0 | acceptString(i, stateName, stateName2, parserState, stateItem, currencySymbol, 0, false) | acceptString(i, stateName, stateName2, parserState, stateItem, internationalCurrencySymbol, 0, false);
        int iNumberOfTrailingZeros = Long.numberOfTrailingZeros(jAcceptString);
        while (true) {
            long j = 1 << iNumberOfTrailingZeros;
            if (j > jAcceptString) {
                break;
            }
            if ((j & jAcceptString) != 0) {
                parserState.getItem(iNumberOfTrailingZeros).sawCurrency = true;
                parserState.getItem(iNumberOfTrailingZeros).isoCode = internationalCurrencySymbol;
            }
            iNumberOfTrailingZeros++;
        }
        if (parserState.parseCurrency) {
            ULocale uLocale = parserState.symbols.getULocale();
            return jAcceptString | acceptCurrencyHelper(i, stateName, stateName2, parserState, stateItem, Currency.openParseState(uLocale, i, 1)) | acceptCurrencyHelper(i, stateName, stateName2, parserState, stateItem, Currency.openParseState(uLocale, i, 0));
        }
        return jAcceptString;
    }

    private static void acceptCurrencyOffset(int i, ParserState parserState, StateItem stateItem) {
        acceptCurrencyHelper(i, stateItem.returnTo1, stateItem.returnTo2, parserState, stateItem, stateItem.currentCurrencyTrieState);
    }

    private static long acceptCurrencyHelper(int i, StateName stateName, StateName stateName2, ParserState parserState, StateItem stateItem, TextTrieMap<Currency.CurrencyStringInfo>.ParseState parseState) {
        long jLastInsertedIndex = 0;
        if (parseState == null) {
            return 0L;
        }
        parseState.accept(i);
        Iterator<Currency.CurrencyStringInfo> currentMatches = parseState.getCurrentMatches();
        if (currentMatches != null) {
            StateItem stateItemCopyFrom = parserState.getNext().copyFrom(stateItem, stateName, -1);
            stateItemCopyFrom.returnTo1 = stateName2;
            stateItemCopyFrom.returnTo2 = null;
            stateItemCopyFrom.sawCurrency = true;
            stateItemCopyFrom.isoCode = currentMatches.next().getISOCode();
            jLastInsertedIndex = 0 | (1 << parserState.lastInsertedIndex());
        }
        if (!parseState.atEnd()) {
            StateItem stateItemCopyFrom2 = parserState.getNext().copyFrom(stateItem, StateName.INSIDE_CURRENCY, -1);
            stateItemCopyFrom2.returnTo1 = stateName;
            stateItemCopyFrom2.returnTo2 = stateName2;
            stateItemCopyFrom2.currentCurrencyTrieState = parseState;
            return jLastInsertedIndex | (1 << parserState.lastInsertedIndex());
        }
        return jLastInsertedIndex;
    }

    private static long acceptDigitTrie(int i, StateName stateName, ParserState parserState, StateItem stateItem, DigitType digitType) {
        TextTrieMap<Byte>.ParseState parseStateOpenParseState = parserState.digitTrie.openParseState(i);
        if (parseStateOpenParseState == null) {
            return 0L;
        }
        return acceptDigitTrieHelper(i, stateName, parserState, stateItem, digitType, parseStateOpenParseState);
    }

    private static void acceptDigitTrieOffset(int i, ParserState parserState, StateItem stateItem) {
        acceptDigitTrieHelper(i, stateItem.returnTo1, parserState, stateItem, stateItem.currentDigitType, stateItem.currentDigitTrieState);
    }

    private static long acceptDigitTrieHelper(int i, StateName stateName, ParserState parserState, StateItem stateItem, DigitType digitType, TextTrieMap<Byte>.ParseState parseState) {
        long jLastInsertedIndex = 0;
        if (parseState == null) {
            return 0L;
        }
        parseState.accept(i);
        Iterator<Byte> currentMatches = parseState.getCurrentMatches();
        if (currentMatches != null) {
            byte bByteValue = currentMatches.next().byteValue();
            StateItem stateItemCopyFrom = parserState.getNext().copyFrom(stateItem, stateName, -1);
            stateItemCopyFrom.returnTo1 = null;
            recordDigit(stateItemCopyFrom, bByteValue, digitType);
            jLastInsertedIndex = 0 | (1 << parserState.lastInsertedIndex());
        }
        if (!parseState.atEnd()) {
            StateItem stateItemCopyFrom2 = parserState.getNext().copyFrom(stateItem, StateName.INSIDE_DIGIT, -1);
            stateItemCopyFrom2.returnTo1 = stateName;
            stateItemCopyFrom2.currentDigitTrieState = parseState;
            stateItemCopyFrom2.currentDigitType = digitType;
            return jLastInsertedIndex | (1 << parserState.lastInsertedIndex());
        }
        return jLastInsertedIndex;
    }

    private static boolean codePointEquals(int i, int i2, ParserState parserState) {
        if (!parserState.caseSensitive) {
            i = UCharacter.foldCase(i, true);
            i2 = UCharacter.foldCase(i2, true);
        }
        return i == i2;
    }

    private static boolean isIgnorable(int i, ParserState parserState) {
        if (i < 0) {
            return false;
        }
        if (UNISET_BIDI.contains(i)) {
            return true;
        }
        return parserState.mode == ParseMode.LENIENT && UNISET_WHITESPACE.contains(i);
    }
}
