package android.icu.text;

class MultiplierSubstitution extends NFSubstitution {
    long divisor;

    MultiplierSubstitution(int i, NFRule nFRule, NFRuleSet nFRuleSet, String str) {
        super(i, nFRuleSet, str);
        this.divisor = nFRule.getDivisor();
        if (this.divisor == 0) {
            throw new IllegalStateException("Substitution with divisor 0 " + str.substring(0, i) + " | " + str.substring(i));
        }
    }

    @Override
    public void setDivisor(int i, short s) {
        this.divisor = NFRule.power(i, s);
        if (this.divisor == 0) {
            throw new IllegalStateException("Substitution with divisor 0");
        }
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && this.divisor == ((MultiplierSubstitution) obj).divisor;
    }

    @Override
    public long transformNumber(long j) {
        return (long) Math.floor(j / this.divisor);
    }

    @Override
    public double transformNumber(double d) {
        if (this.ruleSet == null) {
            return d / this.divisor;
        }
        return Math.floor(d / this.divisor);
    }

    @Override
    public double composeRuleValue(double d, double d2) {
        return d * this.divisor;
    }

    @Override
    public double calcUpperBound(double d) {
        return this.divisor;
    }

    @Override
    char tokenChar() {
        return '<';
    }
}
