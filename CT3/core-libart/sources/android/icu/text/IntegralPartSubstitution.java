package android.icu.text;

class IntegralPartSubstitution extends NFSubstitution {
    IntegralPartSubstitution(int pos, NFRuleSet ruleSet, String description) {
        super(pos, ruleSet, description);
    }

    @Override
    public long transformNumber(long number) {
        return number;
    }

    @Override
    public double transformNumber(double number) {
        return Math.floor(number);
    }

    @Override
    public double composeRuleValue(double newRuleValue, double oldRuleValue) {
        return newRuleValue + oldRuleValue;
    }

    @Override
    public double calcUpperBound(double oldUpperBound) {
        return Double.MAX_VALUE;
    }

    @Override
    char tokenChar() {
        return '<';
    }
}
