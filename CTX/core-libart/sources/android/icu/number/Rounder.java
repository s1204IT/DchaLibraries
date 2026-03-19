package android.icu.number;

import android.icu.impl.number.DecimalQuantity;
import android.icu.impl.number.MultiplierProducer;
import android.icu.impl.number.RoundingUtils;
import android.icu.util.Currency;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public abstract class Rounder implements Cloneable {
    static final boolean $assertionsDisabled = false;
    MathContext mathContext = RoundingUtils.mathContextUnlimited(RoundingUtils.DEFAULT_ROUNDING_MODE);
    static final InfiniteRounderImpl NONE = new InfiniteRounderImpl();
    static final FractionRounderImpl FIXED_FRAC_0 = new FractionRounderImpl(0, 0);
    static final FractionRounderImpl FIXED_FRAC_2 = new FractionRounderImpl(2, 2);
    static final FractionRounderImpl MAX_FRAC_6 = new FractionRounderImpl(0, 6);
    static final SignificantRounderImpl FIXED_SIG_2 = new SignificantRounderImpl(2, 2);
    static final SignificantRounderImpl FIXED_SIG_3 = new SignificantRounderImpl(3, 3);
    static final SignificantRounderImpl RANGE_SIG_2_3 = new SignificantRounderImpl(2, 3);
    static final FracSigRounderImpl COMPACT_STRATEGY = new FracSigRounderImpl(0, 0, 2, -1);
    static final IncrementRounderImpl NICKEL = new IncrementRounderImpl(BigDecimal.valueOf(0.05d));
    static final CurrencyRounderImpl MONETARY_STANDARD = new CurrencyRounderImpl(Currency.CurrencyUsage.STANDARD);
    static final CurrencyRounderImpl MONETARY_CASH = new CurrencyRounderImpl(Currency.CurrencyUsage.CASH);
    static final PassThroughRounderImpl PASS_THROUGH = new PassThroughRounderImpl();

    @Deprecated
    public abstract void apply(DecimalQuantity decimalQuantity);

    Rounder() {
    }

    public static Rounder unlimited() {
        return constructInfinite();
    }

    public static FractionRounder integer() {
        return constructFraction(0, 0);
    }

    public static FractionRounder fixedFraction(int i) {
        if (i >= 0 && i <= 100) {
            return constructFraction(i, i);
        }
        throw new IllegalArgumentException("Fraction length must be between 0 and 100");
    }

    public static FractionRounder minFraction(int i) {
        if (i >= 0 && i < 100) {
            return constructFraction(i, -1);
        }
        throw new IllegalArgumentException("Fraction length must be between 0 and 100");
    }

    public static FractionRounder maxFraction(int i) {
        if (i >= 0 && i < 100) {
            return constructFraction(0, i);
        }
        throw new IllegalArgumentException("Fraction length must be between 0 and 100");
    }

    public static FractionRounder minMaxFraction(int i, int i2) {
        if (i >= 0 && i2 <= 100 && i <= i2) {
            return constructFraction(i, i2);
        }
        throw new IllegalArgumentException("Fraction length must be between 0 and 100");
    }

    public static Rounder fixedDigits(int i) {
        if (i > 0 && i <= 100) {
            return constructSignificant(i, i);
        }
        throw new IllegalArgumentException("Significant digits must be between 0 and 100");
    }

    public static Rounder minDigits(int i) {
        if (i > 0 && i <= 100) {
            return constructSignificant(i, -1);
        }
        throw new IllegalArgumentException("Significant digits must be between 0 and 100");
    }

    public static Rounder maxDigits(int i) {
        if (i > 0 && i <= 100) {
            return constructSignificant(0, i);
        }
        throw new IllegalArgumentException("Significant digits must be between 0 and 100");
    }

    public static Rounder minMaxDigits(int i, int i2) {
        if (i > 0 && i2 <= 100 && i <= i2) {
            return constructSignificant(i, i2);
        }
        throw new IllegalArgumentException("Significant digits must be between 0 and 100");
    }

    public static Rounder increment(BigDecimal bigDecimal) {
        if (bigDecimal != null && bigDecimal.compareTo(BigDecimal.ZERO) > 0) {
            return constructIncrement(bigDecimal);
        }
        throw new IllegalArgumentException("Rounding increment must be positive and non-null");
    }

    public static CurrencyRounder currency(Currency.CurrencyUsage currencyUsage) {
        if (currencyUsage != null) {
            return constructCurrency(currencyUsage);
        }
        throw new IllegalArgumentException("CurrencyUsage must be non-null");
    }

    public Rounder withMode(RoundingMode roundingMode) {
        return withMode(RoundingUtils.mathContextUnlimited(roundingMode));
    }

    @Deprecated
    public Rounder withMode(MathContext mathContext) {
        if (this.mathContext.equals(mathContext)) {
            return this;
        }
        Rounder rounder = (Rounder) clone();
        rounder.mathContext = mathContext;
        return rounder;
    }

    @Deprecated
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    static Rounder constructInfinite() {
        return NONE;
    }

    static FractionRounder constructFraction(int i, int i2) {
        if (i == 0 && i2 == 0) {
            return FIXED_FRAC_0;
        }
        if (i == 2 && i2 == 2) {
            return FIXED_FRAC_2;
        }
        if (i == 0 && i2 == 6) {
            return MAX_FRAC_6;
        }
        return new FractionRounderImpl(i, i2);
    }

    static Rounder constructSignificant(int i, int i2) {
        if (i == 2 && i2 == 2) {
            return FIXED_SIG_2;
        }
        if (i == 3 && i2 == 3) {
            return FIXED_SIG_3;
        }
        if (i == 2 && i2 == 3) {
            return RANGE_SIG_2_3;
        }
        return new SignificantRounderImpl(i, i2);
    }

    static Rounder constructFractionSignificant(FractionRounder fractionRounder, int i, int i2) {
        FractionRounderImpl fractionRounderImpl = (FractionRounderImpl) fractionRounder;
        if (fractionRounderImpl.minFrac == 0 && fractionRounderImpl.maxFrac == 0 && i == 2) {
            return COMPACT_STRATEGY;
        }
        return new FracSigRounderImpl(fractionRounderImpl.minFrac, fractionRounderImpl.maxFrac, i, i2);
    }

    static Rounder constructIncrement(BigDecimal bigDecimal) {
        if (bigDecimal.equals(NICKEL.increment)) {
            return NICKEL;
        }
        return new IncrementRounderImpl(bigDecimal);
    }

    static CurrencyRounder constructCurrency(Currency.CurrencyUsage currencyUsage) {
        if (currencyUsage == Currency.CurrencyUsage.STANDARD) {
            return MONETARY_STANDARD;
        }
        if (currencyUsage == Currency.CurrencyUsage.CASH) {
            return MONETARY_CASH;
        }
        throw new AssertionError();
    }

    static Rounder constructFromCurrency(CurrencyRounder currencyRounder, Currency currency) {
        CurrencyRounderImpl currencyRounderImpl = (CurrencyRounderImpl) currencyRounder;
        double roundingIncrement = currency.getRoundingIncrement(currencyRounderImpl.usage);
        if (roundingIncrement != 0.0d) {
            return constructIncrement(BigDecimal.valueOf(roundingIncrement));
        }
        int defaultFractionDigits = currency.getDefaultFractionDigits(currencyRounderImpl.usage);
        return constructFraction(defaultFractionDigits, defaultFractionDigits);
    }

    static Rounder constructPassThrough() {
        return PASS_THROUGH;
    }

    Rounder withLocaleData(Currency currency) {
        if (this instanceof CurrencyRounder) {
            return ((CurrencyRounder) this).withCurrency(currency);
        }
        return this;
    }

    int chooseMultiplierAndApply(DecimalQuantity decimalQuantity, MultiplierProducer multiplierProducer) {
        DecimalQuantity decimalQuantityCreateCopy = decimalQuantity.createCopy();
        int magnitude = decimalQuantity.getMagnitude();
        int multiplier = multiplierProducer.getMultiplier(magnitude);
        decimalQuantity.adjustMagnitude(multiplier);
        apply(decimalQuantity);
        if (!decimalQuantity.isZero() && decimalQuantity.getMagnitude() == magnitude + multiplier + 1) {
            decimalQuantity.copyFrom(decimalQuantityCreateCopy);
            int multiplier2 = multiplierProducer.getMultiplier(magnitude + 1);
            decimalQuantity.adjustMagnitude(multiplier2);
            apply(decimalQuantity);
            return multiplier2;
        }
        return multiplier;
    }

    static class InfiniteRounderImpl extends Rounder {
        @Override
        public void apply(DecimalQuantity decimalQuantity) {
            decimalQuantity.roundToInfinity();
            decimalQuantity.setFractionLength(0, Integer.MAX_VALUE);
        }
    }

    static class FractionRounderImpl extends FractionRounder {
        final int maxFrac;
        final int minFrac;

        public FractionRounderImpl(int i, int i2) {
            this.minFrac = i;
            this.maxFrac = i2;
        }

        @Override
        public void apply(DecimalQuantity decimalQuantity) {
            decimalQuantity.roundToMagnitude(Rounder.getRoundingMagnitudeFraction(this.maxFrac), this.mathContext);
            decimalQuantity.setFractionLength(Math.max(0, -Rounder.getDisplayMagnitudeFraction(this.minFrac)), Integer.MAX_VALUE);
        }
    }

    static class SignificantRounderImpl extends Rounder {
        static final boolean $assertionsDisabled = false;
        final int maxSig;
        final int minSig;

        public SignificantRounderImpl(int i, int i2) {
            this.minSig = i;
            this.maxSig = i2;
        }

        @Override
        public void apply(DecimalQuantity decimalQuantity) {
            decimalQuantity.roundToMagnitude(Rounder.getRoundingMagnitudeSignificant(decimalQuantity, this.maxSig), this.mathContext);
            decimalQuantity.setFractionLength(Math.max(0, -Rounder.getDisplayMagnitudeSignificant(decimalQuantity, this.minSig)), Integer.MAX_VALUE);
        }

        public void apply(DecimalQuantity decimalQuantity, int i) {
            decimalQuantity.setFractionLength(this.minSig - i, Integer.MAX_VALUE);
        }
    }

    static class FracSigRounderImpl extends Rounder {
        final int maxFrac;
        final int maxSig;
        final int minFrac;
        final int minSig;

        public FracSigRounderImpl(int i, int i2, int i3, int i4) {
            this.minFrac = i;
            this.maxFrac = i2;
            this.minSig = i3;
            this.maxSig = i4;
        }

        @Override
        public void apply(DecimalQuantity decimalQuantity) {
            int iMin;
            int displayMagnitudeFraction = Rounder.getDisplayMagnitudeFraction(this.minFrac);
            int roundingMagnitudeFraction = Rounder.getRoundingMagnitudeFraction(this.maxFrac);
            if (this.minSig == -1) {
                iMin = Math.max(roundingMagnitudeFraction, Rounder.getRoundingMagnitudeSignificant(decimalQuantity, this.maxSig));
            } else {
                iMin = Math.min(roundingMagnitudeFraction, Rounder.getDisplayMagnitudeSignificant(decimalQuantity, this.minSig));
            }
            decimalQuantity.roundToMagnitude(iMin, this.mathContext);
            decimalQuantity.setFractionLength(Math.max(0, -displayMagnitudeFraction), Integer.MAX_VALUE);
        }
    }

    static class IncrementRounderImpl extends Rounder {
        final BigDecimal increment;

        public IncrementRounderImpl(BigDecimal bigDecimal) {
            this.increment = bigDecimal;
        }

        @Override
        public void apply(DecimalQuantity decimalQuantity) {
            decimalQuantity.roundToIncrement(this.increment, this.mathContext);
            decimalQuantity.setFractionLength(this.increment.scale(), this.increment.scale());
        }
    }

    static class CurrencyRounderImpl extends CurrencyRounder {
        final Currency.CurrencyUsage usage;

        public CurrencyRounderImpl(Currency.CurrencyUsage currencyUsage) {
            this.usage = currencyUsage;
        }

        @Override
        public void apply(DecimalQuantity decimalQuantity) {
            throw new AssertionError();
        }
    }

    static class PassThroughRounderImpl extends Rounder {
        @Override
        public void apply(DecimalQuantity decimalQuantity) {
        }
    }

    private static int getRoundingMagnitudeFraction(int i) {
        if (i == -1) {
            return Integer.MIN_VALUE;
        }
        return -i;
    }

    private static int getRoundingMagnitudeSignificant(DecimalQuantity decimalQuantity, int i) {
        if (i == -1) {
            return Integer.MIN_VALUE;
        }
        return ((decimalQuantity.isZero() ? 0 : decimalQuantity.getMagnitude()) - i) + 1;
    }

    private static int getDisplayMagnitudeFraction(int i) {
        if (i == 0) {
            return Integer.MAX_VALUE;
        }
        return -i;
    }

    private static int getDisplayMagnitudeSignificant(DecimalQuantity decimalQuantity, int i) {
        return ((decimalQuantity.isZero() ? 0 : decimalQuantity.getMagnitude()) - i) + 1;
    }
}
