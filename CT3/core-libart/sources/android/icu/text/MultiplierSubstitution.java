package android.icu.text;

class MultiplierSubstitution extends NFSubstitution {
    double divisor;

    MultiplierSubstitution(int pos, double divisor, NFRuleSet ruleSet, String description) {
        super(pos, ruleSet, description);
        this.divisor = divisor;
        if (divisor != 0.0d) {
        } else {
            throw new IllegalStateException("Substitution with divisor 0 " + description.substring(0, pos) + " | " + description.substring(pos));
        }
    }

    @Override
    public void setDivisor(int radix, int exponent) {
        this.divisor = Math.pow(radix, exponent);
        if (this.divisor != 0.0d) {
        } else {
            throw new IllegalStateException("Substitution with divisor 0");
        }
    }

    @Override
    public boolean equals(Object that) {
        return super.equals(that) && this.divisor == ((MultiplierSubstitution) that).divisor;
    }

    @Override
    public long transformNumber(long number) {
        return (long) Math.floor(number / this.divisor);
    }

    @Override
    public double transformNumber(double number) {
        if (this.ruleSet == null) {
            return number / this.divisor;
        }
        return Math.floor(number / this.divisor);
    }

    @Override
    public double composeRuleValue(double newRuleValue, double oldRuleValue) {
        return this.divisor * newRuleValue;
    }

    @Override
    public double calcUpperBound(double oldUpperBound) {
        return this.divisor;
    }

    @Override
    char tokenChar() {
        return '<';
    }
}
