package android.icu.text;

class IntegralPartSubstitution extends NFSubstitution {
    IntegralPartSubstitution(int i, NFRuleSet nFRuleSet, String str) {
        super(i, nFRuleSet, str);
    }

    @Override
    public long transformNumber(long j) {
        return j;
    }

    @Override
    public double transformNumber(double d) {
        return Math.floor(d);
    }

    @Override
    public double composeRuleValue(double d, double d2) {
        return d + d2;
    }

    @Override
    public double calcUpperBound(double d) {
        return Double.MAX_VALUE;
    }

    @Override
    char tokenChar() {
        return '<';
    }
}
