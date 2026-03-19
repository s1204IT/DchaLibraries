package android.icu.number;

import android.icu.impl.number.AffixPatternProvider;
import android.icu.impl.number.CompactData;
import android.icu.impl.number.ConstantAffixModifier;
import android.icu.impl.number.DecimalQuantity;
import android.icu.impl.number.LongNameHandler;
import android.icu.impl.number.MacroProps;
import android.icu.impl.number.MicroProps;
import android.icu.impl.number.MicroPropsGenerator;
import android.icu.impl.number.MutablePatternModifier;
import android.icu.impl.number.NumberStringBuilder;
import android.icu.impl.number.Padder;
import android.icu.impl.number.PatternStringParser;
import android.icu.number.NumberFormatter;
import android.icu.text.DecimalFormatSymbols;
import android.icu.text.NumberFormat;
import android.icu.text.NumberingSystem;
import android.icu.text.PluralRules;
import android.icu.util.Currency;
import android.icu.util.MeasureUnit;

class NumberFormatterImpl {
    private static final Currency DEFAULT_CURRENCY = Currency.getInstance("XXX");
    final MicroPropsGenerator microPropsGenerator;

    public static NumberFormatterImpl fromMacros(MacroProps macroProps) {
        return new NumberFormatterImpl(macrosToMicroGenerator(macroProps, true));
    }

    public static MicroProps applyStatic(MacroProps macroProps, DecimalQuantity decimalQuantity, NumberStringBuilder numberStringBuilder) {
        MicroProps microPropsProcessQuantity = macrosToMicroGenerator(macroProps, false).processQuantity(decimalQuantity);
        microsToString(microPropsProcessQuantity, decimalQuantity, numberStringBuilder);
        return microPropsProcessQuantity;
    }

    private NumberFormatterImpl(MicroPropsGenerator microPropsGenerator) {
        this.microPropsGenerator = microPropsGenerator;
    }

    public MicroProps apply(DecimalQuantity decimalQuantity, NumberStringBuilder numberStringBuilder) {
        MicroProps microPropsProcessQuantity = this.microPropsGenerator.processQuantity(decimalQuantity);
        microsToString(microPropsProcessQuantity, decimalQuantity, numberStringBuilder);
        return microPropsProcessQuantity;
    }

    private static boolean unitIsCurrency(MeasureUnit measureUnit) {
        return measureUnit != null && "currency".equals(measureUnit.getType());
    }

    private static boolean unitIsNoUnit(MeasureUnit measureUnit) {
        return measureUnit == null || "none".equals(measureUnit.getType());
    }

    private static boolean unitIsPercent(MeasureUnit measureUnit) {
        return measureUnit != null && "percent".equals(measureUnit.getSubtype());
    }

    private static boolean unitIsPermille(MeasureUnit measureUnit) {
        return measureUnit != null && "permille".equals(measureUnit.getSubtype());
    }

    private static MicroPropsGenerator macrosToMicroGenerator(MacroProps macroProps, boolean z) {
        NumberingSystem numberingSystem;
        MicroPropsGenerator microPropsGeneratorWithLocaleData;
        MicroPropsGenerator microPropsGeneratorAddToChain;
        MicroPropsGenerator microPropsGenerator;
        LongNameHandler longNameHandlerForCurrencyLongNames;
        CompactData.CompactType compactType;
        MicroProps microProps = new MicroProps(z);
        boolean zUnitIsCurrency = unitIsCurrency(macroProps.unit);
        boolean zUnitIsNoUnit = unitIsNoUnit(macroProps.unit);
        int i = 1;
        boolean z2 = zUnitIsNoUnit && unitIsPercent(macroProps.unit);
        boolean z3 = zUnitIsNoUnit && unitIsPermille(macroProps.unit);
        boolean z4 = (zUnitIsCurrency || zUnitIsNoUnit) ? false : true;
        boolean z5 = macroProps.sign == NumberFormatter.SignDisplay.ACCOUNTING || macroProps.sign == NumberFormatter.SignDisplay.ACCOUNTING_ALWAYS;
        Currency currency = zUnitIsCurrency ? (Currency) macroProps.unit : DEFAULT_CURRENCY;
        NumberFormatter.UnitWidth unitWidth = NumberFormatter.UnitWidth.SHORT;
        if (macroProps.unitWidth != null) {
            unitWidth = macroProps.unitWidth;
        }
        PluralRules pluralRulesForLocale = macroProps.rules;
        if (macroProps.symbols instanceof NumberingSystem) {
            numberingSystem = (NumberingSystem) macroProps.symbols;
        } else {
            numberingSystem = NumberingSystem.getInstance(macroProps.loc);
        }
        String name = numberingSystem.getName();
        if (z2 || z3) {
            i = 2;
        } else if (zUnitIsCurrency && unitWidth != NumberFormatter.UnitWidth.FULL_NAME) {
            if (z5) {
                i = 7;
            }
        } else {
            i = 0;
        }
        PatternStringParser.ParsedPatternInfo toPatternInfo = PatternStringParser.parseToPatternInfo(NumberFormat.getPatternForStyleAndNumberingSystem(macroProps.loc, name, i));
        if (macroProps.symbols instanceof DecimalFormatSymbols) {
            microProps.symbols = (DecimalFormatSymbols) macroProps.symbols;
        } else {
            microProps.symbols = DecimalFormatSymbols.forNumberingSystem(macroProps.loc, numberingSystem);
        }
        if (macroProps.multiplier != null) {
            microPropsGeneratorWithLocaleData = macroProps.multiplier.copyAndChain(microProps);
        } else {
            microPropsGeneratorWithLocaleData = microProps;
        }
        if (macroProps.rounder != null) {
            microProps.rounding = macroProps.rounder;
        } else if (macroProps.notation instanceof CompactNotation) {
            microProps.rounding = Rounder.COMPACT_STRATEGY;
        } else if (zUnitIsCurrency) {
            microProps.rounding = Rounder.MONETARY_STANDARD;
        } else {
            microProps.rounding = Rounder.MAX_FRAC_6;
        }
        microProps.rounding = microProps.rounding.withLocaleData(currency);
        if (macroProps.grouper != null) {
            microProps.grouping = macroProps.grouper;
        } else if (macroProps.notation instanceof CompactNotation) {
            microProps.grouping = Grouper.minTwoDigits();
        } else {
            microProps.grouping = Grouper.defaults();
        }
        microProps.grouping = microProps.grouping.withLocaleData(toPatternInfo);
        if (macroProps.padder != null) {
            microProps.padding = macroProps.padder;
        } else {
            microProps.padding = Padder.NONE;
        }
        if (macroProps.integerWidth != null) {
            microProps.integerWidth = macroProps.integerWidth;
        } else {
            microProps.integerWidth = IntegerWidth.DEFAULT;
        }
        if (macroProps.sign != null) {
            microProps.sign = macroProps.sign;
        } else {
            microProps.sign = NumberFormatter.SignDisplay.AUTO;
        }
        if (macroProps.decimal != null) {
            microProps.decimal = macroProps.decimal;
        } else {
            microProps.decimal = NumberFormatter.DecimalSeparatorDisplay.AUTO;
        }
        microProps.useCurrency = zUnitIsCurrency;
        if (macroProps.notation instanceof ScientificNotation) {
            microPropsGeneratorWithLocaleData = ((ScientificNotation) macroProps.notation).withLocaleData(microProps.symbols, z, microPropsGeneratorWithLocaleData);
        } else {
            microProps.modInner = ConstantAffixModifier.EMPTY;
        }
        MutablePatternModifier mutablePatternModifier = new MutablePatternModifier(false);
        AffixPatternProvider affixPatternProvider = toPatternInfo;
        if (macroProps.affixProvider != null) {
            affixPatternProvider = macroProps.affixProvider;
        }
        mutablePatternModifier.setPatternInfo(affixPatternProvider);
        mutablePatternModifier.setPatternAttributes(microProps.sign, z3);
        if (mutablePatternModifier.needsPlurals()) {
            if (pluralRulesForLocale == null) {
                pluralRulesForLocale = PluralRules.forLocale(macroProps.loc);
            }
            mutablePatternModifier.setSymbols(microProps.symbols, currency, unitWidth, pluralRulesForLocale);
        } else {
            mutablePatternModifier.setSymbols(microProps.symbols, currency, unitWidth, null);
        }
        if (z) {
            microPropsGeneratorAddToChain = mutablePatternModifier.createImmutableAndChain(microPropsGeneratorWithLocaleData);
        } else {
            microPropsGeneratorAddToChain = mutablePatternModifier.addToChain(microPropsGeneratorWithLocaleData);
        }
        if (z4) {
            if (pluralRulesForLocale == null) {
                pluralRulesForLocale = PluralRules.forLocale(macroProps.loc);
            }
            longNameHandlerForCurrencyLongNames = LongNameHandler.forMeasureUnit(macroProps.loc, macroProps.unit, unitWidth, pluralRulesForLocale, microPropsGeneratorAddToChain);
        } else if (zUnitIsCurrency && unitWidth == NumberFormatter.UnitWidth.FULL_NAME) {
            if (pluralRulesForLocale == null) {
                pluralRulesForLocale = PluralRules.forLocale(macroProps.loc);
            }
            longNameHandlerForCurrencyLongNames = LongNameHandler.forCurrencyLongNames(macroProps.loc, currency, pluralRulesForLocale, microPropsGeneratorAddToChain);
        } else {
            microProps.modOuter = ConstantAffixModifier.EMPTY;
            microPropsGenerator = microPropsGeneratorAddToChain;
            if (!(macroProps.notation instanceof CompactNotation)) {
                PluralRules pluralRulesForLocale2 = pluralRulesForLocale == null ? PluralRules.forLocale(macroProps.loc) : pluralRulesForLocale;
                if ((macroProps.unit instanceof Currency) && macroProps.unitWidth != NumberFormatter.UnitWidth.FULL_NAME) {
                    compactType = CompactData.CompactType.CURRENCY;
                } else {
                    compactType = CompactData.CompactType.DECIMAL;
                }
                return ((CompactNotation) macroProps.notation).withLocaleData(macroProps.loc, name, compactType, pluralRulesForLocale2, z ? mutablePatternModifier : null, microPropsGenerator);
            }
            return microPropsGenerator;
        }
        microPropsGenerator = longNameHandlerForCurrencyLongNames;
        if (!(macroProps.notation instanceof CompactNotation)) {
        }
    }

    private static void microsToString(MicroProps microProps, DecimalQuantity decimalQuantity, NumberStringBuilder numberStringBuilder) {
        microProps.rounding.apply(decimalQuantity);
        if (microProps.integerWidth.maxInt == -1) {
            decimalQuantity.setIntegerLength(microProps.integerWidth.minInt, Integer.MAX_VALUE);
        } else {
            decimalQuantity.setIntegerLength(microProps.integerWidth.minInt, microProps.integerWidth.maxInt);
        }
        int iWriteNumber = writeNumber(microProps, decimalQuantity, numberStringBuilder);
        int iApply = iWriteNumber + microProps.modInner.apply(numberStringBuilder, 0, iWriteNumber);
        if (microProps.padding.isValid()) {
            microProps.padding.padAndApply(microProps.modMiddle, microProps.modOuter, numberStringBuilder, 0, iApply);
        } else {
            microProps.modOuter.apply(numberStringBuilder, 0, iApply + microProps.modMiddle.apply(numberStringBuilder, 0, iApply));
        }
    }

    private static int writeNumber(MicroProps microProps, DecimalQuantity decimalQuantity, NumberStringBuilder numberStringBuilder) {
        if (decimalQuantity.isInfinite()) {
            return 0 + numberStringBuilder.insert(0, microProps.symbols.getInfinity(), NumberFormat.Field.INTEGER);
        }
        if (decimalQuantity.isNaN()) {
            return 0 + numberStringBuilder.insert(0, microProps.symbols.getNaN(), NumberFormat.Field.INTEGER);
        }
        int iWriteIntegerDigits = 0 + writeIntegerDigits(microProps, decimalQuantity, numberStringBuilder);
        if (decimalQuantity.getLowerDisplayMagnitude() < 0 || microProps.decimal == NumberFormatter.DecimalSeparatorDisplay.ALWAYS) {
            iWriteIntegerDigits += numberStringBuilder.insert(iWriteIntegerDigits, microProps.useCurrency ? microProps.symbols.getMonetaryDecimalSeparatorString() : microProps.symbols.getDecimalSeparatorString(), NumberFormat.Field.DECIMAL_SEPARATOR);
        }
        return iWriteIntegerDigits + writeFractionDigits(microProps, decimalQuantity, numberStringBuilder);
    }

    private static int writeIntegerDigits(MicroProps microProps, DecimalQuantity decimalQuantity, NumberStringBuilder numberStringBuilder) {
        int upperDisplayMagnitude = decimalQuantity.getUpperDisplayMagnitude() + 1;
        int iInsertCodePoint = 0;
        for (int i = 0; i < upperDisplayMagnitude; i++) {
            if (microProps.grouping.groupAtPosition(i, decimalQuantity)) {
                iInsertCodePoint += numberStringBuilder.insert(0, microProps.useCurrency ? microProps.symbols.getMonetaryGroupingSeparatorString() : microProps.symbols.getGroupingSeparatorString(), NumberFormat.Field.GROUPING_SEPARATOR);
            }
            byte digit = decimalQuantity.getDigit(i);
            iInsertCodePoint += microProps.symbols.getCodePointZero() != -1 ? numberStringBuilder.insertCodePoint(0, microProps.symbols.getCodePointZero() + digit, NumberFormat.Field.INTEGER) : numberStringBuilder.insert(0, microProps.symbols.getDigitStringsLocal()[digit], NumberFormat.Field.INTEGER);
        }
        return iInsertCodePoint;
    }

    private static int writeFractionDigits(MicroProps microProps, DecimalQuantity decimalQuantity, NumberStringBuilder numberStringBuilder) {
        int iAppend;
        int i = -decimalQuantity.getLowerDisplayMagnitude();
        int i2 = 0;
        for (int i3 = 0; i3 < i; i3++) {
            byte digit = decimalQuantity.getDigit((-i3) - 1);
            if (microProps.symbols.getCodePointZero() != -1) {
                iAppend = numberStringBuilder.appendCodePoint(microProps.symbols.getCodePointZero() + digit, NumberFormat.Field.FRACTION);
            } else {
                iAppend = numberStringBuilder.append(microProps.symbols.getDigitStringsLocal()[digit], NumberFormat.Field.FRACTION);
            }
            i2 += iAppend;
        }
        return i2;
    }
}
