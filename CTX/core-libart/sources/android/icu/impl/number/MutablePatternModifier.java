package android.icu.impl.number;

import android.icu.impl.StandardPlural;
import android.icu.impl.locale.XLocaleDistance;
import android.icu.impl.number.AffixUtils;
import android.icu.number.NumberFormatter;
import android.icu.text.DecimalFormatSymbols;
import android.icu.text.PluralRules;
import android.icu.util.Currency;

public class MutablePatternModifier implements Modifier, AffixUtils.SymbolProvider, CharSequence, MicroPropsGenerator {
    static final boolean $assertionsDisabled = false;
    Currency currency;
    int flags;
    boolean inCharSequenceMode;
    boolean isNegative;
    final boolean isStrong;
    int length;
    MicroPropsGenerator parent;
    AffixPatternProvider patternInfo;
    boolean perMilleReplacesPercent;
    StandardPlural plural;
    boolean plusReplacesMinusSign;
    boolean prependSign;
    PluralRules rules;
    NumberFormatter.SignDisplay signDisplay;
    DecimalFormatSymbols symbols;
    NumberFormatter.UnitWidth unitWidth;

    public MutablePatternModifier(boolean z) {
        this.isStrong = z;
    }

    public void setPatternInfo(AffixPatternProvider affixPatternProvider) {
        this.patternInfo = affixPatternProvider;
    }

    public void setPatternAttributes(NumberFormatter.SignDisplay signDisplay, boolean z) {
        this.signDisplay = signDisplay;
        this.perMilleReplacesPercent = z;
    }

    public void setSymbols(DecimalFormatSymbols decimalFormatSymbols, Currency currency, NumberFormatter.UnitWidth unitWidth, PluralRules pluralRules) {
        this.symbols = decimalFormatSymbols;
        this.currency = currency;
        this.unitWidth = unitWidth;
        this.rules = pluralRules;
    }

    public void setNumberProperties(boolean z, StandardPlural standardPlural) {
        this.isNegative = z;
        this.plural = standardPlural;
    }

    public boolean needsPlurals() {
        return this.patternInfo.containsSymbolType(-7);
    }

    public ImmutablePatternModifier createImmutable() {
        return createImmutableAndChain(null);
    }

    public ImmutablePatternModifier createImmutableAndChain(MicroPropsGenerator microPropsGenerator) {
        NumberStringBuilder numberStringBuilder = new NumberStringBuilder();
        NumberStringBuilder numberStringBuilder2 = new NumberStringBuilder();
        if (needsPlurals()) {
            ParameterizedModifier parameterizedModifier = new ParameterizedModifier();
            for (StandardPlural standardPlural : StandardPlural.VALUES) {
                setNumberProperties(false, standardPlural);
                parameterizedModifier.setModifier(false, standardPlural, createConstantModifier(numberStringBuilder, numberStringBuilder2));
                setNumberProperties(true, standardPlural);
                parameterizedModifier.setModifier(true, standardPlural, createConstantModifier(numberStringBuilder, numberStringBuilder2));
            }
            parameterizedModifier.freeze();
            return new ImmutablePatternModifier(parameterizedModifier, this.rules, microPropsGenerator);
        }
        setNumberProperties(false, null);
        ConstantMultiFieldModifier constantMultiFieldModifierCreateConstantModifier = createConstantModifier(numberStringBuilder, numberStringBuilder2);
        setNumberProperties(true, null);
        return new ImmutablePatternModifier(new ParameterizedModifier(constantMultiFieldModifierCreateConstantModifier, createConstantModifier(numberStringBuilder, numberStringBuilder2)), null, microPropsGenerator);
    }

    private ConstantMultiFieldModifier createConstantModifier(NumberStringBuilder numberStringBuilder, NumberStringBuilder numberStringBuilder2) {
        insertPrefix(numberStringBuilder.clear(), 0);
        insertSuffix(numberStringBuilder2.clear(), 0);
        if (this.patternInfo.hasCurrencySign()) {
            return new CurrencySpacingEnabledModifier(numberStringBuilder, numberStringBuilder2, this.isStrong, this.symbols);
        }
        return new ConstantMultiFieldModifier(numberStringBuilder, numberStringBuilder2, this.isStrong);
    }

    public static class ImmutablePatternModifier implements MicroPropsGenerator {
        final MicroPropsGenerator parent;
        final ParameterizedModifier pm;
        final PluralRules rules;

        ImmutablePatternModifier(ParameterizedModifier parameterizedModifier, PluralRules pluralRules, MicroPropsGenerator microPropsGenerator) {
            this.pm = parameterizedModifier;
            this.rules = pluralRules;
            this.parent = microPropsGenerator;
        }

        @Override
        public MicroProps processQuantity(DecimalQuantity decimalQuantity) {
            MicroProps microPropsProcessQuantity = this.parent.processQuantity(decimalQuantity);
            applyToMicros(microPropsProcessQuantity, decimalQuantity);
            return microPropsProcessQuantity;
        }

        public void applyToMicros(MicroProps microProps, DecimalQuantity decimalQuantity) {
            if (this.rules == null) {
                microProps.modMiddle = this.pm.getModifier(decimalQuantity.isNegative());
                return;
            }
            DecimalQuantity decimalQuantityCreateCopy = decimalQuantity.createCopy();
            decimalQuantityCreateCopy.roundToInfinity();
            microProps.modMiddle = this.pm.getModifier(decimalQuantity.isNegative(), decimalQuantityCreateCopy.getStandardPlural(this.rules));
        }
    }

    public MicroPropsGenerator addToChain(MicroPropsGenerator microPropsGenerator) {
        this.parent = microPropsGenerator;
        return this;
    }

    @Override
    public MicroProps processQuantity(DecimalQuantity decimalQuantity) {
        MicroProps microPropsProcessQuantity = this.parent.processQuantity(decimalQuantity);
        if (needsPlurals()) {
            DecimalQuantity decimalQuantityCreateCopy = decimalQuantity.createCopy();
            microPropsProcessQuantity.rounding.apply(decimalQuantityCreateCopy);
            setNumberProperties(decimalQuantity.isNegative(), decimalQuantityCreateCopy.getStandardPlural(this.rules));
        } else {
            setNumberProperties(decimalQuantity.isNegative(), null);
        }
        microPropsProcessQuantity.modMiddle = this;
        return microPropsProcessQuantity;
    }

    @Override
    public int apply(NumberStringBuilder numberStringBuilder, int i, int i2) {
        int iInsertPrefix = insertPrefix(numberStringBuilder, i);
        int i3 = i2 + iInsertPrefix;
        int iInsertSuffix = insertSuffix(numberStringBuilder, i3);
        CurrencySpacingEnabledModifier.applyCurrencySpacing(numberStringBuilder, i, iInsertPrefix, i3, iInsertSuffix, this.symbols);
        return iInsertPrefix + iInsertSuffix;
    }

    @Override
    public int getPrefixLength() {
        enterCharSequenceMode(true);
        int iUnescapedCodePointCount = AffixUtils.unescapedCodePointCount(this, this);
        exitCharSequenceMode();
        return iUnescapedCodePointCount;
    }

    @Override
    public int getCodePointCount() {
        enterCharSequenceMode(true);
        int iUnescapedCodePointCount = AffixUtils.unescapedCodePointCount(this, this);
        exitCharSequenceMode();
        enterCharSequenceMode(false);
        int iUnescapedCodePointCount2 = iUnescapedCodePointCount + AffixUtils.unescapedCodePointCount(this, this);
        exitCharSequenceMode();
        return iUnescapedCodePointCount2;
    }

    @Override
    public boolean isStrong() {
        return this.isStrong;
    }

    private int insertPrefix(NumberStringBuilder numberStringBuilder, int i) {
        enterCharSequenceMode(true);
        int iUnescape = AffixUtils.unescape(this, numberStringBuilder, i, this);
        exitCharSequenceMode();
        return iUnescape;
    }

    private int insertSuffix(NumberStringBuilder numberStringBuilder, int i) {
        enterCharSequenceMode(false);
        int iUnescape = AffixUtils.unescape(this, numberStringBuilder, i, this);
        exitCharSequenceMode();
        return iUnescape;
    }

    @Override
    public CharSequence getSymbol(int i) {
        switch (i) {
            case AffixUtils.TYPE_CURRENCY_QUINT:
                return this.currency.getName(this.symbols.getULocale(), 3, (boolean[]) null);
            case AffixUtils.TYPE_CURRENCY_QUAD:
                return XLocaleDistance.ANY;
            case AffixUtils.TYPE_CURRENCY_TRIPLE:
                return this.currency.getName(this.symbols.getULocale(), 2, this.plural.getKeyword(), (boolean[]) null);
            case AffixUtils.TYPE_CURRENCY_DOUBLE:
                return this.currency.getCurrencyCode();
            case AffixUtils.TYPE_CURRENCY_SINGLE:
                if (this.unitWidth == NumberFormatter.UnitWidth.ISO_CODE) {
                    return this.currency.getCurrencyCode();
                }
                if (this.unitWidth == NumberFormatter.UnitWidth.HIDDEN) {
                    return "";
                }
                if (this.unitWidth == NumberFormatter.UnitWidth.NARROW) {
                    return this.currency.getName(this.symbols.getULocale(), 3, (boolean[]) null);
                }
                return this.currency.getName(this.symbols.getULocale(), 0, (boolean[]) null);
            case AffixUtils.TYPE_PERMILLE:
                return this.symbols.getPerMillString();
            case AffixUtils.TYPE_PERCENT:
                return this.symbols.getPercentString();
            case -2:
                return this.symbols.getPlusSignString();
            case -1:
                return this.symbols.getMinusSignString();
            default:
                throw new AssertionError();
        }
    }

    private void enterCharSequenceMode(boolean z) {
        this.inCharSequenceMode = true;
        this.plusReplacesMinusSign = !this.isNegative && (this.signDisplay == NumberFormatter.SignDisplay.ALWAYS || this.signDisplay == NumberFormatter.SignDisplay.ACCOUNTING_ALWAYS) && !this.patternInfo.positiveHasPlusSign();
        Object[] objArr = this.patternInfo.hasNegativeSubpattern() && (this.isNegative || (this.patternInfo.negativeHasMinusSign() && this.plusReplacesMinusSign));
        this.flags = 0;
        if (objArr != false) {
            this.flags |= 512;
        }
        if (z) {
            this.flags |= 256;
        }
        if (this.plural != null) {
            this.flags |= this.plural.ordinal();
        }
        if (!z || objArr != false) {
            this.prependSign = false;
        } else if (this.isNegative) {
            this.prependSign = this.signDisplay != NumberFormatter.SignDisplay.NEVER;
        } else {
            this.prependSign = this.plusReplacesMinusSign;
        }
        this.length = this.patternInfo.length(this.flags) + (this.prependSign ? 1 : 0);
    }

    private void exitCharSequenceMode() {
        this.inCharSequenceMode = false;
    }

    @Override
    public int length() {
        return this.length;
    }

    @Override
    public char charAt(int i) {
        char cCharAt;
        if (!this.prependSign || i != 0) {
            if (this.prependSign) {
                cCharAt = this.patternInfo.charAt(this.flags, i - 1);
            } else {
                cCharAt = this.patternInfo.charAt(this.flags, i);
            }
        } else {
            cCharAt = '-';
        }
        if (this.plusReplacesMinusSign && cCharAt == '-') {
            return '+';
        }
        if (this.perMilleReplacesPercent && cCharAt == '%') {
            return (char) 8240;
        }
        return cCharAt;
    }

    @Override
    public CharSequence subSequence(int i, int i2) {
        throw new AssertionError();
    }
}
