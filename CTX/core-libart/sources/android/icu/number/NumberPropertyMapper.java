package android.icu.number;

import android.icu.impl.StandardPlural;
import android.icu.impl.locale.LanguageTag;
import android.icu.impl.number.AffixPatternProvider;
import android.icu.impl.number.AffixUtils;
import android.icu.impl.number.CustomSymbolCurrency;
import android.icu.impl.number.DecimalFormatProperties;
import android.icu.impl.number.MacroProps;
import android.icu.impl.number.MultiplierImpl;
import android.icu.impl.number.Padder;
import android.icu.impl.number.PatternStringParser;
import android.icu.impl.number.RoundingUtils;
import android.icu.number.NumberFormatter;
import android.icu.number.Rounder;
import android.icu.text.CompactDecimalFormat;
import android.icu.text.CurrencyPluralInfo;
import android.icu.text.DecimalFormatSymbols;
import android.icu.util.Currency;
import android.icu.util.ULocale;
import java.math.BigDecimal;
import java.math.MathContext;

final class NumberPropertyMapper {
    static final boolean $assertionsDisabled = false;

    NumberPropertyMapper() {
    }

    public static UnlocalizedNumberFormatter create(DecimalFormatProperties decimalFormatProperties, DecimalFormatSymbols decimalFormatSymbols) {
        return NumberFormatter.with().macros(oldToNew(decimalFormatProperties, decimalFormatSymbols, null));
    }

    public static UnlocalizedNumberFormatter create(String str, DecimalFormatSymbols decimalFormatSymbols) {
        return create(PatternStringParser.parseToProperties(str), decimalFormatSymbols);
    }

    public static MacroProps oldToNew(DecimalFormatProperties decimalFormatProperties, DecimalFormatSymbols decimalFormatSymbols, DecimalFormatProperties decimalFormatProperties2) {
        Rounder rounderConstructFraction;
        BigDecimal bigDecimal;
        int i;
        MacroProps macroProps = new MacroProps();
        ULocale uLocale = decimalFormatSymbols.getULocale();
        macroProps.symbols = decimalFormatSymbols;
        macroProps.rules = decimalFormatProperties.getPluralRules();
        AffixPatternProvider propertiesAffixPatternProvider = decimalFormatProperties.getCurrencyPluralInfo() == null ? new PropertiesAffixPatternProvider(decimalFormatProperties) : new CurrencyPluralInfoAffixProvider(decimalFormatProperties.getCurrencyPluralInfo());
        macroProps.affixProvider = propertiesAffixPatternProvider;
        boolean z = (decimalFormatProperties.getCurrency() == null && decimalFormatProperties.getCurrencyPluralInfo() == null && decimalFormatProperties.getCurrencyUsage() == null && !propertiesAffixPatternProvider.hasCurrencySign()) ? false : true;
        Currency currencyResolve = CustomSymbolCurrency.resolve(decimalFormatProperties.getCurrency(), uLocale, decimalFormatSymbols);
        Currency.CurrencyUsage currencyUsage = decimalFormatProperties.getCurrencyUsage();
        boolean z2 = currencyUsage != null;
        if (!z2) {
            currencyUsage = Currency.CurrencyUsage.STANDARD;
        }
        if (z) {
            macroProps.unit = currencyResolve;
        }
        int maximumIntegerDigits = decimalFormatProperties.getMaximumIntegerDigits();
        int minimumIntegerDigits = decimalFormatProperties.getMinimumIntegerDigits();
        int maximumFractionDigits = decimalFormatProperties.getMaximumFractionDigits();
        int minimumFractionDigits = decimalFormatProperties.getMinimumFractionDigits();
        int minimumSignificantDigits = decimalFormatProperties.getMinimumSignificantDigits();
        int maximumSignificantDigits = decimalFormatProperties.getMaximumSignificantDigits();
        BigDecimal roundingIncrement = decimalFormatProperties.getRoundingIncrement();
        MathContext mathContextOrUnlimited = RoundingUtils.getMathContextOrUnlimited(decimalFormatProperties);
        boolean z3 = (minimumFractionDigits == -1 && maximumFractionDigits == -1) ? false : true;
        boolean z4 = (minimumSignificantDigits == -1 && maximumSignificantDigits == -1) ? false : true;
        if (z) {
            if (minimumFractionDigits == -1 && maximumFractionDigits == -1) {
                minimumFractionDigits = currencyResolve.getDefaultFractionDigits(currencyUsage);
                maximumFractionDigits = currencyResolve.getDefaultFractionDigits(currencyUsage);
            } else if (minimumFractionDigits == -1) {
                minimumFractionDigits = Math.min(maximumFractionDigits, currencyResolve.getDefaultFractionDigits(currencyUsage));
            } else if (maximumFractionDigits == -1) {
                maximumFractionDigits = Math.max(minimumFractionDigits, currencyResolve.getDefaultFractionDigits(currencyUsage));
            }
        }
        if (minimumIntegerDigits != 0 || maximumFractionDigits == 0) {
            if (minimumFractionDigits < 0) {
                minimumFractionDigits = 0;
            }
            if (maximumFractionDigits < 0) {
                maximumFractionDigits = Integer.MAX_VALUE;
            } else if (maximumFractionDigits < minimumFractionDigits) {
                maximumFractionDigits = minimumFractionDigits;
            }
            if (minimumIntegerDigits <= 0 || minimumIntegerDigits > 100) {
                minimumIntegerDigits = 1;
            }
            if (maximumIntegerDigits >= 0) {
                if (maximumIntegerDigits < minimumIntegerDigits) {
                    maximumIntegerDigits = minimumIntegerDigits;
                } else if (maximumIntegerDigits > 100) {
                    maximumIntegerDigits = -1;
                }
            }
        } else {
            if (minimumFractionDigits <= 0) {
                minimumFractionDigits = 1;
            }
            if (maximumFractionDigits < 0) {
                maximumFractionDigits = Integer.MAX_VALUE;
            } else if (maximumFractionDigits < minimumFractionDigits) {
                maximumFractionDigits = minimumFractionDigits;
            }
            if (maximumIntegerDigits < 0 || maximumIntegerDigits > 100) {
                maximumIntegerDigits = -1;
            }
            minimumIntegerDigits = 0;
        }
        if (z2) {
            rounderConstructFraction = Rounder.constructCurrency(currencyUsage).withCurrency(currencyResolve);
        } else if (roundingIncrement != null) {
            rounderConstructFraction = Rounder.constructIncrement(roundingIncrement);
        } else if (z4) {
            if (minimumSignificantDigits < 1) {
                minimumSignificantDigits = 1;
            } else if (minimumSignificantDigits > 100) {
                minimumSignificantDigits = 100;
            }
            if (maximumSignificantDigits < 0) {
                maximumSignificantDigits = 100;
            } else if (maximumSignificantDigits < minimumSignificantDigits) {
                maximumSignificantDigits = minimumSignificantDigits;
            } else if (maximumSignificantDigits > 100) {
                maximumSignificantDigits = 100;
            }
            rounderConstructFraction = Rounder.constructSignificant(minimumSignificantDigits, maximumSignificantDigits);
        } else {
            rounderConstructFraction = z3 ? Rounder.constructFraction(minimumFractionDigits, maximumFractionDigits) : z ? Rounder.constructCurrency(currencyUsage) : null;
        }
        if (rounderConstructFraction != null) {
            rounderConstructFraction = rounderConstructFraction.withMode(mathContextOrUnlimited);
            macroProps.rounder = rounderConstructFraction;
        }
        macroProps.integerWidth = IntegerWidth.zeroFillTo(minimumIntegerDigits).truncateAt(maximumIntegerDigits);
        int groupingSize = decimalFormatProperties.getGroupingSize();
        int secondaryGroupingSize = decimalFormatProperties.getSecondaryGroupingSize();
        int minimumGroupingDigits = decimalFormatProperties.getMinimumGroupingDigits();
        if (groupingSize <= 0 && secondaryGroupingSize > 0) {
            groupingSize = secondaryGroupingSize;
        }
        if (secondaryGroupingSize <= 0) {
            secondaryGroupingSize = groupingSize;
        }
        macroProps.grouper = Grouper.getInstance((byte) groupingSize, (byte) secondaryGroupingSize, minimumGroupingDigits == 2);
        if (decimalFormatProperties.getFormatWidth() != -1) {
            macroProps.padder = new Padder(decimalFormatProperties.getPadString(), decimalFormatProperties.getFormatWidth(), decimalFormatProperties.getPadPosition());
        }
        macroProps.decimal = decimalFormatProperties.getDecimalSeparatorAlwaysShown() ? NumberFormatter.DecimalSeparatorDisplay.ALWAYS : NumberFormatter.DecimalSeparatorDisplay.AUTO;
        macroProps.sign = decimalFormatProperties.getSignAlwaysShown() ? NumberFormatter.SignDisplay.ALWAYS : NumberFormatter.SignDisplay.AUTO;
        if (decimalFormatProperties.getMinimumExponentDigits() != -1) {
            if (maximumIntegerDigits > 8) {
                macroProps.integerWidth = IntegerWidth.zeroFillTo(minimumIntegerDigits).truncateAt(minimumIntegerDigits);
                i = minimumIntegerDigits;
            } else if (maximumIntegerDigits <= minimumIntegerDigits || minimumIntegerDigits <= 1) {
                i = maximumIntegerDigits;
            } else {
                macroProps.integerWidth = IntegerWidth.zeroFillTo(1).truncateAt(maximumIntegerDigits);
                i = maximumIntegerDigits;
                minimumIntegerDigits = 1;
            }
            int i2 = i < 0 ? -1 : i;
            int i3 = i;
            macroProps.notation = new ScientificNotation(i2, i2 == minimumIntegerDigits, decimalFormatProperties.getMinimumExponentDigits(), decimalFormatProperties.getExponentSignAlwaysShown() ? NumberFormatter.SignDisplay.ALWAYS : NumberFormatter.SignDisplay.AUTO);
            if (macroProps.rounder instanceof FractionRounder) {
                int minimumIntegerDigits2 = decimalFormatProperties.getMinimumIntegerDigits();
                int minimumFractionDigits2 = decimalFormatProperties.getMinimumFractionDigits();
                int maximumFractionDigits2 = decimalFormatProperties.getMaximumFractionDigits();
                if (minimumIntegerDigits2 == 0 && maximumFractionDigits2 == 0) {
                    macroProps.rounder = Rounder.constructInfinite().withMode(mathContextOrUnlimited);
                } else if (minimumIntegerDigits2 == 0 && minimumFractionDigits2 == 0) {
                    macroProps.rounder = Rounder.constructSignificant(1, maximumFractionDigits2 + 1).withMode(mathContextOrUnlimited);
                } else {
                    macroProps.rounder = Rounder.constructSignificant(minimumFractionDigits2 + minimumIntegerDigits2, minimumIntegerDigits2 + maximumFractionDigits2).withMode(mathContextOrUnlimited);
                }
            }
            maximumIntegerDigits = i3;
        }
        if (decimalFormatProperties.getCompactStyle() != null) {
            if (decimalFormatProperties.getCompactCustomData() != null) {
                macroProps.notation = new CompactNotation(decimalFormatProperties.getCompactCustomData());
            } else if (decimalFormatProperties.getCompactStyle() == CompactDecimalFormat.CompactStyle.LONG) {
                macroProps.notation = Notation.compactLong();
            } else {
                macroProps.notation = Notation.compactShort();
            }
            bigDecimal = null;
            macroProps.affixProvider = null;
        } else {
            bigDecimal = null;
        }
        if (decimalFormatProperties.getMagnitudeMultiplier() != 0) {
            macroProps.multiplier = new MultiplierImpl(decimalFormatProperties.getMagnitudeMultiplier());
        } else if (decimalFormatProperties.getMultiplier() != null) {
            macroProps.multiplier = new MultiplierImpl(decimalFormatProperties.getMultiplier());
        }
        if (decimalFormatProperties2 != null) {
            decimalFormatProperties2.setMathContext(mathContextOrUnlimited);
            decimalFormatProperties2.setRoundingMode(mathContextOrUnlimited.getRoundingMode());
            decimalFormatProperties2.setMinimumIntegerDigits(minimumIntegerDigits);
            if (maximumIntegerDigits == -1) {
                maximumIntegerDigits = Integer.MAX_VALUE;
            }
            decimalFormatProperties2.setMaximumIntegerDigits(maximumIntegerDigits);
            if (rounderConstructFraction instanceof CurrencyRounder) {
                rounderConstructFraction = ((CurrencyRounder) rounderConstructFraction).withCurrency(currencyResolve);
            }
            if (rounderConstructFraction instanceof Rounder.FractionRounderImpl) {
                Rounder.FractionRounderImpl fractionRounderImpl = (Rounder.FractionRounderImpl) rounderConstructFraction;
                minimumFractionDigits = fractionRounderImpl.minFrac;
                maximumFractionDigits = fractionRounderImpl.maxFrac;
            } else if (rounderConstructFraction instanceof Rounder.IncrementRounderImpl) {
                BigDecimal bigDecimal2 = ((Rounder.IncrementRounderImpl) rounderConstructFraction).increment;
                minimumFractionDigits = bigDecimal2.scale();
                maximumFractionDigits = bigDecimal2.scale();
                bigDecimal = bigDecimal2;
            } else if (rounderConstructFraction instanceof Rounder.SignificantRounderImpl) {
                Rounder.SignificantRounderImpl significantRounderImpl = (Rounder.SignificantRounderImpl) rounderConstructFraction;
                minimumSignificantDigits = significantRounderImpl.minSig;
                maximumSignificantDigits = significantRounderImpl.maxSig;
            }
            decimalFormatProperties2.setMinimumFractionDigits(minimumFractionDigits);
            decimalFormatProperties2.setMaximumFractionDigits(maximumFractionDigits);
            decimalFormatProperties2.setMinimumSignificantDigits(minimumSignificantDigits);
            decimalFormatProperties2.setMaximumSignificantDigits(maximumSignificantDigits);
            decimalFormatProperties2.setRoundingIncrement(bigDecimal);
        }
        return macroProps;
    }

    private static class PropertiesAffixPatternProvider implements AffixPatternProvider {
        private final String negPrefix;
        private final String negSuffix;
        private final String posPrefix;
        private final String posSuffix;

        public PropertiesAffixPatternProvider(DecimalFormatProperties decimalFormatProperties) {
            String str;
            String strEscape = AffixUtils.escape(decimalFormatProperties.getPositivePrefix());
            String strEscape2 = AffixUtils.escape(decimalFormatProperties.getPositiveSuffix());
            String strEscape3 = AffixUtils.escape(decimalFormatProperties.getNegativePrefix());
            String strEscape4 = AffixUtils.escape(decimalFormatProperties.getNegativeSuffix());
            String positivePrefixPattern = decimalFormatProperties.getPositivePrefixPattern();
            String positiveSuffixPattern = decimalFormatProperties.getPositiveSuffixPattern();
            String negativePrefixPattern = decimalFormatProperties.getNegativePrefixPattern();
            String negativeSuffixPattern = decimalFormatProperties.getNegativeSuffixPattern();
            if (strEscape != null) {
                this.posPrefix = strEscape;
            } else if (positivePrefixPattern != null) {
                this.posPrefix = positivePrefixPattern;
            } else {
                this.posPrefix = "";
            }
            if (strEscape2 != null) {
                this.posSuffix = strEscape2;
            } else if (positiveSuffixPattern != null) {
                this.posSuffix = positiveSuffixPattern;
            } else {
                this.posSuffix = "";
            }
            if (strEscape3 != null) {
                this.negPrefix = strEscape3;
            } else if (negativePrefixPattern != null) {
                this.negPrefix = negativePrefixPattern;
            } else {
                if (positivePrefixPattern == null) {
                    str = LanguageTag.SEP;
                } else {
                    str = LanguageTag.SEP + positivePrefixPattern;
                }
                this.negPrefix = str;
            }
            if (strEscape4 != null) {
                this.negSuffix = strEscape4;
            } else if (negativeSuffixPattern != null) {
                this.negSuffix = negativeSuffixPattern;
            } else {
                this.negSuffix = positiveSuffixPattern == null ? "" : positiveSuffixPattern;
            }
        }

        @Override
        public char charAt(int i, int i2) {
            return getStringForFlags(i).charAt(i2);
        }

        @Override
        public int length(int i) {
            return getStringForFlags(i).length();
        }

        private String getStringForFlags(int i) {
            boolean z = (i & 256) != 0;
            boolean z2 = (i & 512) != 0;
            if (z && z2) {
                return this.negPrefix;
            }
            if (z) {
                return this.posPrefix;
            }
            if (z2) {
                return this.negSuffix;
            }
            return this.posSuffix;
        }

        @Override
        public boolean positiveHasPlusSign() {
            return AffixUtils.containsType(this.posPrefix, -2) || AffixUtils.containsType(this.posSuffix, -2);
        }

        @Override
        public boolean hasNegativeSubpattern() {
            return true;
        }

        @Override
        public boolean negativeHasMinusSign() {
            return AffixUtils.containsType(this.negPrefix, -1) || AffixUtils.containsType(this.negSuffix, -1);
        }

        @Override
        public boolean hasCurrencySign() {
            return AffixUtils.hasCurrencySymbols(this.posPrefix) || AffixUtils.hasCurrencySymbols(this.posSuffix) || AffixUtils.hasCurrencySymbols(this.negPrefix) || AffixUtils.hasCurrencySymbols(this.negSuffix);
        }

        @Override
        public boolean containsSymbolType(int i) {
            return AffixUtils.containsType(this.posPrefix, i) || AffixUtils.containsType(this.posSuffix, i) || AffixUtils.containsType(this.negPrefix, i) || AffixUtils.containsType(this.negSuffix, i);
        }
    }

    private static class CurrencyPluralInfoAffixProvider implements AffixPatternProvider {
        private final AffixPatternProvider[] affixesByPlural = new PatternStringParser.ParsedPatternInfo[StandardPlural.COUNT];

        public CurrencyPluralInfoAffixProvider(CurrencyPluralInfo currencyPluralInfo) {
            for (StandardPlural standardPlural : StandardPlural.VALUES) {
                this.affixesByPlural[standardPlural.ordinal()] = PatternStringParser.parseToPatternInfo(currencyPluralInfo.getCurrencyPluralPattern(standardPlural.getKeyword()));
            }
        }

        @Override
        public char charAt(int i, int i2) {
            return this.affixesByPlural[i & 255].charAt(i, i2);
        }

        @Override
        public int length(int i) {
            return this.affixesByPlural[i & 255].length(i);
        }

        @Override
        public boolean positiveHasPlusSign() {
            return this.affixesByPlural[StandardPlural.OTHER.ordinal()].positiveHasPlusSign();
        }

        @Override
        public boolean hasNegativeSubpattern() {
            return this.affixesByPlural[StandardPlural.OTHER.ordinal()].hasNegativeSubpattern();
        }

        @Override
        public boolean negativeHasMinusSign() {
            return this.affixesByPlural[StandardPlural.OTHER.ordinal()].negativeHasMinusSign();
        }

        @Override
        public boolean hasCurrencySign() {
            return this.affixesByPlural[StandardPlural.OTHER.ordinal()].hasCurrencySign();
        }

        @Override
        public boolean containsSymbolType(int i) {
            return this.affixesByPlural[StandardPlural.OTHER.ordinal()].containsSymbolType(i);
        }
    }
}
