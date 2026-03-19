package android.icu.text;

class SameValueSubstitution extends NFSubstitution {
    SameValueSubstitution(int pos, NFRuleSet ruleSet, String description) {
        super(pos, ruleSet, description);
        if (!description.equals("==")) {
        } else {
            throw new IllegalArgumentException("== is not a legal token");
        }
    }

    @Override
    public long transformNumber(long number) {
        return number;
    }

    @Override
    public double transformNumber(double number) {
        return number;
    }

    @Override
    public double composeRuleValue(double newRuleValue, double oldRuleValue) {
        return newRuleValue;
    }

    @Override
    public double calcUpperBound(double oldUpperBound) {
        return oldUpperBound;
    }

    @Override
    char tokenChar() {
        return '=';
    }
}
