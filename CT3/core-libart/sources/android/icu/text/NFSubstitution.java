package android.icu.text;

import java.text.ParsePosition;

abstract class NFSubstitution {

    static final boolean f77assertionsDisabled;
    final DecimalFormat numberFormat;
    final int pos;
    final NFRuleSet ruleSet;

    public abstract double calcUpperBound(double d);

    public abstract double composeRuleValue(double d, double d2);

    abstract char tokenChar();

    public abstract double transformNumber(double d);

    public abstract long transformNumber(long j);

    static {
        f77assertionsDisabled = !NFSubstitution.class.desiredAssertionStatus();
    }

    public static NFSubstitution makeSubstitution(int pos, NFRule rule, NFRule rulePredecessor, NFRuleSet ruleSet, RuleBasedNumberFormat formatter, String description) {
        if (description.length() == 0) {
            return null;
        }
        switch (description.charAt(0)) {
            case '<':
                if (rule.getBaseValue() == -1) {
                    throw new IllegalArgumentException("<< not allowed in negative-number rule");
                }
                if (rule.getBaseValue() == -2 || rule.getBaseValue() == -3 || rule.getBaseValue() == -4) {
                    return new IntegralPartSubstitution(pos, ruleSet, description);
                }
                if (ruleSet.isFractionSet()) {
                    return new NumeratorSubstitution(pos, rule.getBaseValue(), formatter.getDefaultRuleSet(), description);
                }
                return new MultiplierSubstitution(pos, rule.getDivisor(), ruleSet, description);
            case '=':
                return new SameValueSubstitution(pos, ruleSet, description);
            case '>':
                if (rule.getBaseValue() == -1) {
                    return new AbsoluteValueSubstitution(pos, ruleSet, description);
                }
                if (rule.getBaseValue() == -2 || rule.getBaseValue() == -3 || rule.getBaseValue() == -4) {
                    return new FractionalPartSubstitution(pos, ruleSet, description);
                }
                if (ruleSet.isFractionSet()) {
                    throw new IllegalArgumentException(">> not allowed in fraction rule set");
                }
                return new ModulusSubstitution(pos, rule.getDivisor(), rulePredecessor, ruleSet, description);
            default:
                throw new IllegalArgumentException("Illegal substitution character");
        }
    }

    NFSubstitution(int pos, NFRuleSet ruleSet, String description) {
        this.pos = pos;
        int descriptionLen = description.length();
        if (descriptionLen >= 2 && description.charAt(0) == description.charAt(descriptionLen - 1)) {
            description = description.substring(1, descriptionLen - 1);
        } else if (descriptionLen != 0) {
            throw new IllegalArgumentException("Illegal substitution syntax");
        }
        if (description.length() == 0) {
            this.ruleSet = ruleSet;
            this.numberFormat = null;
            return;
        }
        if (description.charAt(0) == '%') {
            this.ruleSet = ruleSet.owner.findRuleSet(description);
            this.numberFormat = null;
        } else if (description.charAt(0) == '#' || description.charAt(0) == '0') {
            this.ruleSet = null;
            this.numberFormat = (DecimalFormat) ruleSet.owner.getDecimalFormat().clone();
            this.numberFormat.applyPattern(description);
        } else {
            if (description.charAt(0) == '>') {
                this.ruleSet = ruleSet;
                this.numberFormat = null;
                return;
            }
            throw new IllegalArgumentException("Illegal substitution syntax");
        }
    }

    public void setDivisor(int radix, int exponent) {
    }

    public boolean equals(Object that) {
        if (that == null) {
            return false;
        }
        if (this == that) {
            return true;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        NFSubstitution that2 = (NFSubstitution) that;
        if (this.pos != that2.pos || (this.ruleSet == null && that2.ruleSet != null)) {
            return false;
        }
        return this.numberFormat == null ? that2.numberFormat == null : this.numberFormat.equals(that2.numberFormat);
    }

    public int hashCode() {
        if (f77assertionsDisabled) {
            return 42;
        }
        throw new AssertionError("hashCode not designed");
    }

    public String toString() {
        if (this.ruleSet != null) {
            return tokenChar() + this.ruleSet.getName() + tokenChar();
        }
        return tokenChar() + this.numberFormat.toPattern() + tokenChar();
    }

    public void doSubstitution(long number, StringBuffer toInsertInto, int position, int recursionCount) {
        if (this.ruleSet != null) {
            this.ruleSet.format(transformNumber(number), toInsertInto, position + this.pos, recursionCount);
        } else {
            double numberToFormat = transformNumber(number);
            if (this.numberFormat.getMaximumFractionDigits() == 0) {
                numberToFormat = Math.floor(numberToFormat);
            }
            toInsertInto.insert(this.pos + position, this.numberFormat.format(numberToFormat));
        }
    }

    public void doSubstitution(double number, StringBuffer toInsertInto, int position, int recursionCount) {
        double numberToFormat = transformNumber(number);
        if (Double.isInfinite(numberToFormat)) {
            NFRule infiniteRule = this.ruleSet.findRule(Double.POSITIVE_INFINITY);
            infiniteRule.doFormat(numberToFormat, toInsertInto, position + this.pos, recursionCount);
        } else if (numberToFormat == Math.floor(numberToFormat) && this.ruleSet != null) {
            this.ruleSet.format((long) numberToFormat, toInsertInto, position + this.pos, recursionCount);
        } else if (this.ruleSet != null) {
            this.ruleSet.format(numberToFormat, toInsertInto, position + this.pos, recursionCount);
        } else {
            toInsertInto.insert(this.pos + position, this.numberFormat.format(numberToFormat));
        }
    }

    public Number doParse(String text, ParsePosition parsePosition, double baseValue, double upperBound, boolean lenientParse) {
        Number tempResult;
        double upperBound2 = calcUpperBound(upperBound);
        if (this.ruleSet != null) {
            tempResult = this.ruleSet.parse(text, parsePosition, upperBound2);
            if (lenientParse && !this.ruleSet.isFractionSet() && parsePosition.getIndex() == 0) {
                tempResult = this.ruleSet.owner.getDecimalFormat().parse(text, parsePosition);
            }
        } else {
            tempResult = this.numberFormat.parse(text, parsePosition);
        }
        if (parsePosition.getIndex() != 0) {
            double result = composeRuleValue(tempResult.doubleValue(), baseValue);
            if (result == ((long) result)) {
                return Long.valueOf((long) result);
            }
            return new Double(result);
        }
        return tempResult;
    }

    public final int getPos() {
        return this.pos;
    }

    public boolean isModulusSubstitution() {
        return false;
    }

    public void setDecimalFormatSymbols(DecimalFormatSymbols newSymbols) {
        if (this.numberFormat == null) {
            return;
        }
        this.numberFormat.setDecimalFormatSymbols(newSymbols);
    }
}
