package android.icu.impl.number;

import android.icu.impl.Utility;
import android.icu.number.Grouper;
import android.icu.number.IntegerWidth;
import android.icu.number.Notation;
import android.icu.number.NumberFormatter;
import android.icu.number.Rounder;
import android.icu.text.PluralRules;
import android.icu.util.MeasureUnit;
import android.icu.util.ULocale;

public class MacroProps implements Cloneable {
    public AffixPatternProvider affixProvider;
    public NumberFormatter.DecimalSeparatorDisplay decimal;
    public Grouper grouper;
    public IntegerWidth integerWidth;
    public ULocale loc;
    public MultiplierImpl multiplier;
    public Notation notation;
    public Padder padder;
    public Rounder rounder;
    public PluralRules rules;
    public NumberFormatter.SignDisplay sign;
    public Object symbols;
    public Long threshold;
    public MeasureUnit unit;
    public NumberFormatter.UnitWidth unitWidth;

    public void fallback(MacroProps macroProps) {
        if (this.notation == null) {
            this.notation = macroProps.notation;
        }
        if (this.unit == null) {
            this.unit = macroProps.unit;
        }
        if (this.rounder == null) {
            this.rounder = macroProps.rounder;
        }
        if (this.grouper == null) {
            this.grouper = macroProps.grouper;
        }
        if (this.padder == null) {
            this.padder = macroProps.padder;
        }
        if (this.integerWidth == null) {
            this.integerWidth = macroProps.integerWidth;
        }
        if (this.symbols == null) {
            this.symbols = macroProps.symbols;
        }
        if (this.unitWidth == null) {
            this.unitWidth = macroProps.unitWidth;
        }
        if (this.sign == null) {
            this.sign = macroProps.sign;
        }
        if (this.decimal == null) {
            this.decimal = macroProps.decimal;
        }
        if (this.affixProvider == null) {
            this.affixProvider = macroProps.affixProvider;
        }
        if (this.multiplier == null) {
            this.multiplier = macroProps.multiplier;
        }
        if (this.rules == null) {
            this.rules = macroProps.rules;
        }
        if (this.loc == null) {
            this.loc = macroProps.loc;
        }
    }

    public int hashCode() {
        return Utility.hash(this.notation, this.unit, this.rounder, this.grouper, this.padder, this.integerWidth, this.symbols, this.unitWidth, this.sign, this.decimal, this.affixProvider, this.multiplier, this.rules, this.loc);
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MacroProps)) {
            return false;
        }
        MacroProps macroProps = (MacroProps) obj;
        if (!Utility.equals(this.notation, macroProps.notation) || !Utility.equals(this.unit, macroProps.unit) || !Utility.equals(this.rounder, macroProps.rounder) || !Utility.equals(this.grouper, macroProps.grouper) || !Utility.equals(this.padder, macroProps.padder) || !Utility.equals(this.integerWidth, macroProps.integerWidth) || !Utility.equals(this.symbols, macroProps.symbols) || !Utility.equals(this.unitWidth, macroProps.unitWidth) || !Utility.equals(this.sign, macroProps.sign) || !Utility.equals(this.decimal, macroProps.decimal) || !Utility.equals(this.affixProvider, macroProps.affixProvider) || !Utility.equals(this.multiplier, macroProps.multiplier) || !Utility.equals(this.rules, macroProps.rules) || !Utility.equals(this.loc, macroProps.loc)) {
            return false;
        }
        return true;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
