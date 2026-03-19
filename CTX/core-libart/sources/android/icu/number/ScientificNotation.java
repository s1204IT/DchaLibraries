package android.icu.number;

import android.icu.impl.number.DecimalQuantity;
import android.icu.impl.number.MicroProps;
import android.icu.impl.number.MicroPropsGenerator;
import android.icu.impl.number.Modifier;
import android.icu.impl.number.MultiplierProducer;
import android.icu.impl.number.NumberStringBuilder;
import android.icu.number.NumberFormatter;
import android.icu.number.Rounder;
import android.icu.text.DecimalFormatSymbols;
import android.icu.text.NumberFormat;

public class ScientificNotation extends Notation implements Cloneable {
    int engineeringInterval;
    NumberFormatter.SignDisplay exponentSignDisplay;
    int minExponentDigits;
    boolean requireMinInt;

    ScientificNotation(int i, boolean z, int i2, NumberFormatter.SignDisplay signDisplay) {
        this.engineeringInterval = i;
        this.requireMinInt = z;
        this.minExponentDigits = i2;
        this.exponentSignDisplay = signDisplay;
    }

    public ScientificNotation withMinExponentDigits(int i) {
        if (i >= 0 && i < 100) {
            ScientificNotation scientificNotation = (ScientificNotation) clone();
            scientificNotation.minExponentDigits = i;
            return scientificNotation;
        }
        throw new IllegalArgumentException("Integer digits must be between 0 and 100");
    }

    public ScientificNotation withExponentSignDisplay(NumberFormatter.SignDisplay signDisplay) {
        ScientificNotation scientificNotation = (ScientificNotation) clone();
        scientificNotation.exponentSignDisplay = signDisplay;
        return scientificNotation;
    }

    @Deprecated
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    MicroPropsGenerator withLocaleData(DecimalFormatSymbols decimalFormatSymbols, boolean z, MicroPropsGenerator microPropsGenerator) {
        return new ScientificHandler(decimalFormatSymbols, z, microPropsGenerator);
    }

    private static class ScientificHandler implements MicroPropsGenerator, MultiplierProducer, Modifier {
        static final boolean $assertionsDisabled = false;
        int exponent;
        final ScientificNotation notation;
        final MicroPropsGenerator parent;
        final ScientificModifier[] precomputedMods;
        final DecimalFormatSymbols symbols;

        private ScientificHandler(ScientificNotation scientificNotation, DecimalFormatSymbols decimalFormatSymbols, boolean z, MicroPropsGenerator microPropsGenerator) {
            this.notation = scientificNotation;
            this.symbols = decimalFormatSymbols;
            this.parent = microPropsGenerator;
            if (z) {
                this.precomputedMods = new ScientificModifier[25];
                for (int i = -12; i <= 12; i++) {
                    this.precomputedMods[i + 12] = new ScientificModifier(i, this);
                }
                return;
            }
            this.precomputedMods = null;
        }

        @Override
        public MicroProps processQuantity(DecimalQuantity decimalQuantity) {
            MicroProps microPropsProcessQuantity = this.parent.processQuantity(decimalQuantity);
            int i = 0;
            if (decimalQuantity.isZero()) {
                if (this.notation.requireMinInt && (microPropsProcessQuantity.rounding instanceof Rounder.SignificantRounderImpl)) {
                    ((Rounder.SignificantRounderImpl) microPropsProcessQuantity.rounding).apply(decimalQuantity, this.notation.engineeringInterval);
                } else {
                    microPropsProcessQuantity.rounding.apply(decimalQuantity);
                }
            } else {
                i = -microPropsProcessQuantity.rounding.chooseMultiplierAndApply(decimalQuantity, this);
            }
            if (this.precomputedMods != null && i >= -12 && i <= 12) {
                microPropsProcessQuantity.modInner = this.precomputedMods[i + 12];
            } else if (this.precomputedMods != null) {
                microPropsProcessQuantity.modInner = new ScientificModifier(i, this);
            } else {
                this.exponent = i;
                microPropsProcessQuantity.modInner = this;
            }
            microPropsProcessQuantity.rounding = Rounder.constructPassThrough();
            return microPropsProcessQuantity;
        }

        @Override
        public int getMultiplier(int i) {
            int i2 = this.notation.engineeringInterval;
            if (!this.notation.requireMinInt) {
                if (i2 > 1) {
                    i2 = (((i % i2) + i2) % i2) + 1;
                } else {
                    i2 = 1;
                }
            }
            return (i2 - i) - 1;
        }

        @Override
        public int getPrefixLength() {
            return 0;
        }

        @Override
        public int getCodePointCount() {
            throw new AssertionError();
        }

        @Override
        public boolean isStrong() {
            return true;
        }

        @Override
        public int apply(NumberStringBuilder numberStringBuilder, int i, int i2) {
            return doApply(this.exponent, numberStringBuilder, i2);
        }

        private int doApply(int i, NumberStringBuilder numberStringBuilder, int i2) {
            int iInsert = numberStringBuilder.insert(i2, this.symbols.getExponentSeparator(), NumberFormat.Field.EXPONENT_SYMBOL) + i2;
            if (i < 0 && this.notation.exponentSignDisplay != NumberFormatter.SignDisplay.NEVER) {
                iInsert += numberStringBuilder.insert(iInsert, this.symbols.getMinusSignString(), NumberFormat.Field.EXPONENT_SIGN);
            } else if (i >= 0 && this.notation.exponentSignDisplay == NumberFormatter.SignDisplay.ALWAYS) {
                iInsert += numberStringBuilder.insert(iInsert, this.symbols.getPlusSignString(), NumberFormat.Field.EXPONENT_SIGN);
            }
            int iAbs = Math.abs(i);
            int i3 = 0;
            while (true) {
                if (i3 < this.notation.minExponentDigits || iAbs > 0) {
                    iInsert += numberStringBuilder.insert(iInsert - i3, this.symbols.getDigitStringsLocal()[iAbs % 10], NumberFormat.Field.EXPONENT);
                    i3++;
                    iAbs /= 10;
                } else {
                    return iInsert - i2;
                }
            }
        }
    }

    private static class ScientificModifier implements Modifier {
        final int exponent;
        final ScientificHandler handler;

        ScientificModifier(int i, ScientificHandler scientificHandler) {
            this.exponent = i;
            this.handler = scientificHandler;
        }

        @Override
        public int apply(NumberStringBuilder numberStringBuilder, int i, int i2) {
            return this.handler.doApply(this.exponent, numberStringBuilder, i2);
        }

        @Override
        public int getPrefixLength() {
            return 0;
        }

        @Override
        public int getCodePointCount() {
            throw new AssertionError();
        }

        @Override
        public boolean isStrong() {
            return true;
        }
    }
}
