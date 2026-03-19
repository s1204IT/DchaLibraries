package android.icu.text;

class SameValueSubstitution extends NFSubstitution {
    SameValueSubstitution(int i, NFRuleSet nFRuleSet, String str) {
        super(i, nFRuleSet, str);
        if (str.equals("==")) {
            throw new IllegalArgumentException("== is not a legal token");
        }
    }

    @Override
    public long transformNumber(long j) {
        return j;
    }

    @Override
    public double transformNumber(double d) {
        return d;
    }

    @Override
    public double composeRuleValue(double d, double d2) {
        return d;
    }

    @Override
    public double calcUpperBound(double d) {
        return d;
    }

    @Override
    char tokenChar() {
        return '=';
    }
}
