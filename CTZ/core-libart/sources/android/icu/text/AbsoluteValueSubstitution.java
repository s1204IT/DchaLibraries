package android.icu.text;

class AbsoluteValueSubstitution extends NFSubstitution {
    AbsoluteValueSubstitution(int i, NFRuleSet nFRuleSet, String str) {
        super(i, nFRuleSet, str);
    }

    @Override
    public long transformNumber(long j) {
        return Math.abs(j);
    }

    @Override
    public double transformNumber(double d) {
        return Math.abs(d);
    }

    @Override
    public double composeRuleValue(double d, double d2) {
        return -d;
    }

    @Override
    public double calcUpperBound(double d) {
        return Double.MAX_VALUE;
    }

    @Override
    char tokenChar() {
        return '>';
    }
}
