package android.icu.text;

import java.text.ParsePosition;

class ModulusSubstitution extends NFSubstitution {
    long divisor;
    private final NFRule ruleToUse;

    ModulusSubstitution(int i, NFRule nFRule, NFRule nFRule2, NFRuleSet nFRuleSet, String str) {
        super(i, nFRuleSet, str);
        this.divisor = nFRule.getDivisor();
        if (this.divisor == 0) {
            throw new IllegalStateException("Substitution with bad divisor (" + this.divisor + ") " + str.substring(0, i) + " | " + str.substring(i));
        }
        if (str.equals(">>>")) {
            this.ruleToUse = nFRule2;
        } else {
            this.ruleToUse = null;
        }
    }

    @Override
    public void setDivisor(int i, short s) {
        this.divisor = NFRule.power(i, s);
        if (this.divisor == 0) {
            throw new IllegalStateException("Substitution with bad divisor");
        }
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && this.divisor == ((ModulusSubstitution) obj).divisor;
    }

    @Override
    public void doSubstitution(long j, StringBuilder sb, int i, int i2) {
        if (this.ruleToUse == null) {
            super.doSubstitution(j, sb, i, i2);
        } else {
            this.ruleToUse.doFormat(transformNumber(j), sb, i + this.pos, i2);
        }
    }

    @Override
    public void doSubstitution(double d, StringBuilder sb, int i, int i2) {
        if (this.ruleToUse == null) {
            super.doSubstitution(d, sb, i, i2);
        } else {
            this.ruleToUse.doFormat(transformNumber(d), sb, i + this.pos, i2);
        }
    }

    @Override
    public long transformNumber(long j) {
        return j % this.divisor;
    }

    @Override
    public double transformNumber(double d) {
        return Math.floor(d % this.divisor);
    }

    @Override
    public Number doParse(String str, ParsePosition parsePosition, double d, double d2, boolean z) {
        if (this.ruleToUse == null) {
            return super.doParse(str, parsePosition, d, d2, z);
        }
        Number numberDoParse = this.ruleToUse.doParse(str, parsePosition, false, d2);
        if (parsePosition.getIndex() != 0) {
            double dComposeRuleValue = composeRuleValue(numberDoParse.doubleValue(), d);
            long j = (long) dComposeRuleValue;
            if (dComposeRuleValue == j) {
                return Long.valueOf(j);
            }
            return new Double(dComposeRuleValue);
        }
        return numberDoParse;
    }

    @Override
    public double composeRuleValue(double d, double d2) {
        return (d2 - (d2 % this.divisor)) + d;
    }

    @Override
    public double calcUpperBound(double d) {
        return this.divisor;
    }

    @Override
    public boolean isModulusSubstitution() {
        return true;
    }

    @Override
    char tokenChar() {
        return '>';
    }
}
