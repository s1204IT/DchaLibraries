package android.icu.text;

class AbsoluteValueSubstitution extends NFSubstitution {
    AbsoluteValueSubstitution(int pos, NFRuleSet ruleSet, String description) {
        super(pos, ruleSet, description);
    }

    @Override
    public long transformNumber(long number) {
        return Math.abs(number);
    }

    @Override
    public double transformNumber(double number) {
        return Math.abs(number);
    }

    @Override
    public double composeRuleValue(double newRuleValue, double oldRuleValue) {
        return -newRuleValue;
    }

    @Override
    public double calcUpperBound(double oldUpperBound) {
        return Double.MAX_VALUE;
    }

    @Override
    char tokenChar() {
        return '>';
    }
}
