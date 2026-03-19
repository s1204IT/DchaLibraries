package android.icu.text;

import java.text.ParsePosition;

class ModulusSubstitution extends NFSubstitution {
    double divisor;
    private final NFRule ruleToUse;

    ModulusSubstitution(int pos, double divisor, NFRule rulePredecessor, NFRuleSet ruleSet, String description) {
        super(pos, ruleSet, description);
        this.divisor = divisor;
        if (divisor == 0.0d) {
            throw new IllegalStateException("Substitution with bad divisor (" + divisor + ") " + description.substring(0, pos) + " | " + description.substring(pos));
        }
        if (description.equals(">>>")) {
            this.ruleToUse = rulePredecessor;
        } else {
            this.ruleToUse = null;
        }
    }

    @Override
    public void setDivisor(int radix, int exponent) {
        this.divisor = Math.pow(radix, exponent);
        if (this.divisor != 0.0d) {
        } else {
            throw new IllegalStateException("Substitution with bad divisor");
        }
    }

    @Override
    public boolean equals(Object that) {
        if (!super.equals(that)) {
            return false;
        }
        ModulusSubstitution that2 = (ModulusSubstitution) that;
        return this.divisor == that2.divisor;
    }

    @Override
    public void doSubstitution(long number, StringBuffer toInsertInto, int position, int recursionCount) {
        if (this.ruleToUse == null) {
            super.doSubstitution(number, toInsertInto, position, recursionCount);
        } else {
            long numberToFormat = transformNumber(number);
            this.ruleToUse.doFormat(numberToFormat, toInsertInto, position + this.pos, recursionCount);
        }
    }

    @Override
    public void doSubstitution(double number, StringBuffer toInsertInto, int position, int recursionCount) {
        if (this.ruleToUse == null) {
            super.doSubstitution(number, toInsertInto, position, recursionCount);
        } else {
            double numberToFormat = transformNumber(number);
            this.ruleToUse.doFormat(numberToFormat, toInsertInto, position + this.pos, recursionCount);
        }
    }

    @Override
    public long transformNumber(long number) {
        return (long) Math.floor(number % this.divisor);
    }

    @Override
    public double transformNumber(double number) {
        return Math.floor(number % this.divisor);
    }

    @Override
    public Number doParse(String text, ParsePosition parsePosition, double baseValue, double upperBound, boolean lenientParse) {
        if (this.ruleToUse == null) {
            return super.doParse(text, parsePosition, baseValue, upperBound, lenientParse);
        }
        Number tempResult = this.ruleToUse.doParse(text, parsePosition, false, upperBound);
        if (parsePosition.getIndex() != 0) {
            double result = composeRuleValue(tempResult.doubleValue(), baseValue);
            if (result == ((long) result)) {
                return Long.valueOf((long) result);
            }
            return new Double(result);
        }
        return tempResult;
    }

    @Override
    public double composeRuleValue(double newRuleValue, double oldRuleValue) {
        return (oldRuleValue - (oldRuleValue % this.divisor)) + newRuleValue;
    }

    @Override
    public double calcUpperBound(double oldUpperBound) {
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
