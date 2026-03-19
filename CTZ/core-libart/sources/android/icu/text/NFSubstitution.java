package android.icu.text;

import java.text.ParsePosition;

abstract class NFSubstitution {
    static final boolean $assertionsDisabled = false;
    private static final long MAX_INT64_IN_DOUBLE = 9007199254740991L;
    final DecimalFormat numberFormat;
    final int pos;
    final NFRuleSet ruleSet;

    public abstract double calcUpperBound(double d);

    public abstract double composeRuleValue(double d, double d2);

    abstract char tokenChar();

    public abstract double transformNumber(double d);

    public abstract long transformNumber(long j);

    public static NFSubstitution makeSubstitution(int i, NFRule nFRule, NFRule nFRule2, NFRuleSet nFRuleSet, RuleBasedNumberFormat ruleBasedNumberFormat, String str) {
        if (str.length() != 0) {
            switch (str.charAt(0)) {
                case '<':
                    if (nFRule.getBaseValue() == -1) {
                        throw new IllegalArgumentException("<< not allowed in negative-number rule");
                    }
                    if (nFRule.getBaseValue() == -2 || nFRule.getBaseValue() == -3 || nFRule.getBaseValue() == -4) {
                        return new IntegralPartSubstitution(i, nFRuleSet, str);
                    }
                    return nFRuleSet.isFractionSet() ? new NumeratorSubstitution(i, nFRule.getBaseValue(), ruleBasedNumberFormat.getDefaultRuleSet(), str) : new MultiplierSubstitution(i, nFRule, nFRuleSet, str);
                case '=':
                    return new SameValueSubstitution(i, nFRuleSet, str);
                case '>':
                    if (nFRule.getBaseValue() == -1) {
                        return new AbsoluteValueSubstitution(i, nFRuleSet, str);
                    }
                    if (nFRule.getBaseValue() == -2 || nFRule.getBaseValue() == -3 || nFRule.getBaseValue() == -4) {
                        return new FractionalPartSubstitution(i, nFRuleSet, str);
                    }
                    if (!nFRuleSet.isFractionSet()) {
                        return new ModulusSubstitution(i, nFRule, nFRule2, nFRuleSet, str);
                    }
                    throw new IllegalArgumentException(">> not allowed in fraction rule set");
                default:
                    throw new IllegalArgumentException("Illegal substitution character");
            }
        }
        return null;
    }

    NFSubstitution(int i, NFRuleSet nFRuleSet, String str) {
        this.pos = i;
        int length = str.length();
        if (length >= 2) {
            int i2 = length - 1;
            if (str.charAt(0) == str.charAt(i2)) {
                str = str.substring(1, i2);
            } else if (length != 0) {
                throw new IllegalArgumentException("Illegal substitution syntax");
            }
        }
        if (str.length() == 0) {
            this.ruleSet = nFRuleSet;
            this.numberFormat = null;
            return;
        }
        if (str.charAt(0) == '%') {
            this.ruleSet = nFRuleSet.owner.findRuleSet(str);
            this.numberFormat = null;
        } else if (str.charAt(0) == '#' || str.charAt(0) == '0') {
            this.ruleSet = null;
            this.numberFormat = (DecimalFormat) nFRuleSet.owner.getDecimalFormat().clone();
            this.numberFormat.applyPattern(str);
        } else {
            if (str.charAt(0) == '>') {
                this.ruleSet = nFRuleSet;
                this.numberFormat = null;
                return;
            }
            throw new IllegalArgumentException("Illegal substitution syntax");
        }
    }

    public void setDivisor(int i, short s) {
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NFSubstitution nFSubstitution = (NFSubstitution) obj;
        if (this.pos != nFSubstitution.pos) {
            return false;
        }
        if (this.ruleSet == null && nFSubstitution.ruleSet != null) {
            return false;
        }
        if (this.numberFormat == null) {
            if (nFSubstitution.numberFormat != null) {
                return false;
            }
        } else if (!this.numberFormat.equals(nFSubstitution.numberFormat)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return 42;
    }

    public String toString() {
        if (this.ruleSet != null) {
            return tokenChar() + this.ruleSet.getName() + tokenChar();
        }
        return tokenChar() + this.numberFormat.toPattern() + tokenChar();
    }

    public void doSubstitution(long j, StringBuilder sb, int i, int i2) {
        if (this.ruleSet != null) {
            this.ruleSet.format(transformNumber(j), sb, i + this.pos, i2);
        } else {
            if (j <= MAX_INT64_IN_DOUBLE) {
                double dTransformNumber = transformNumber(j);
                if (this.numberFormat.getMaximumFractionDigits() == 0) {
                    dTransformNumber = Math.floor(dTransformNumber);
                }
                sb.insert(i + this.pos, this.numberFormat.format(dTransformNumber));
                return;
            }
            sb.insert(i + this.pos, this.numberFormat.format(transformNumber(j)));
        }
    }

    public void doSubstitution(double d, StringBuilder sb, int i, int i2) {
        double dTransformNumber = transformNumber(d);
        if (Double.isInfinite(dTransformNumber)) {
            this.ruleSet.findRule(Double.POSITIVE_INFINITY).doFormat(dTransformNumber, sb, i + this.pos, i2);
            return;
        }
        if (dTransformNumber == Math.floor(dTransformNumber) && this.ruleSet != null) {
            this.ruleSet.format((long) dTransformNumber, sb, i + this.pos, i2);
        } else if (this.ruleSet != null) {
            this.ruleSet.format(dTransformNumber, sb, i + this.pos, i2);
        } else {
            sb.insert(i + this.pos, this.numberFormat.format(dTransformNumber));
        }
    }

    public Number doParse(String str, ParsePosition parsePosition, double d, double d2, boolean z) {
        Number number;
        double dCalcUpperBound = calcUpperBound(d2);
        if (this.ruleSet != null) {
            number = this.ruleSet.parse(str, parsePosition, dCalcUpperBound);
            if (z && !this.ruleSet.isFractionSet() && parsePosition.getIndex() == 0) {
                number = this.ruleSet.owner.getDecimalFormat().parse(str, parsePosition);
            }
        } else {
            number = this.numberFormat.parse(str, parsePosition);
        }
        if (parsePosition.getIndex() != 0) {
            double dComposeRuleValue = composeRuleValue(number.doubleValue(), d);
            long j = (long) dComposeRuleValue;
            if (dComposeRuleValue == j) {
                return Long.valueOf(j);
            }
            return new Double(dComposeRuleValue);
        }
        return number;
    }

    public final int getPos() {
        return this.pos;
    }

    public boolean isModulusSubstitution() {
        return false;
    }

    public void setDecimalFormatSymbols(DecimalFormatSymbols decimalFormatSymbols) {
        if (this.numberFormat != null) {
            this.numberFormat.setDecimalFormatSymbols(decimalFormatSymbols);
        }
    }
}
