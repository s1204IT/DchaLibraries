package android.icu.text;

import java.text.ParsePosition;

class NumeratorSubstitution extends NFSubstitution {
    private final double denominator;
    private final boolean withZeros;

    NumeratorSubstitution(int pos, double denominator, NFRuleSet ruleSet, String description) {
        super(pos, ruleSet, fixdesc(description));
        this.denominator = denominator;
        this.withZeros = description.endsWith("<<");
    }

    static String fixdesc(String description) {
        if (!description.endsWith("<<")) {
            return description;
        }
        return description.substring(0, description.length() - 1);
    }

    @Override
    public boolean equals(Object that) {
        if (!super.equals(that)) {
            return false;
        }
        NumeratorSubstitution that2 = (NumeratorSubstitution) that;
        return this.denominator == that2.denominator && this.withZeros == that2.withZeros;
    }

    @Override
    public void doSubstitution(double number, StringBuffer toInsertInto, int position, int recursionCount) {
        double numberToFormat = transformNumber(number);
        if (this.withZeros && this.ruleSet != null) {
            long nf = (long) numberToFormat;
            int len = toInsertInto.length();
            while (true) {
                nf *= 10;
                if (nf >= this.denominator) {
                    break;
                }
                toInsertInto.insert(this.pos + position, ' ');
                this.ruleSet.format(0L, toInsertInto, position + this.pos, recursionCount);
            }
            position += toInsertInto.length() - len;
        }
        if (numberToFormat == Math.floor(numberToFormat) && this.ruleSet != null) {
            this.ruleSet.format((long) numberToFormat, toInsertInto, position + this.pos, recursionCount);
        } else if (this.ruleSet != null) {
            this.ruleSet.format(numberToFormat, toInsertInto, position + this.pos, recursionCount);
        } else {
            toInsertInto.insert(this.pos + position, this.numberFormat.format(numberToFormat));
        }
    }

    @Override
    public long transformNumber(long number) {
        return Math.round(number * this.denominator);
    }

    @Override
    public double transformNumber(double number) {
        return Math.round(this.denominator * number);
    }

    @Override
    public Number doParse(String text, ParsePosition parsePosition, double baseValue, double upperBound, boolean lenientParse) {
        int zeroCount = 0;
        if (this.withZeros) {
            String workText = text;
            ParsePosition workPos = new ParsePosition(1);
            while (workText.length() > 0 && workPos.getIndex() != 0) {
                workPos.setIndex(0);
                this.ruleSet.parse(workText, workPos, 1.0d).intValue();
                if (workPos.getIndex() == 0) {
                    break;
                }
                zeroCount++;
                parsePosition.setIndex(parsePosition.getIndex() + workPos.getIndex());
                workText = workText.substring(workPos.getIndex());
                while (workText.length() > 0 && workText.charAt(0) == ' ') {
                    workText = workText.substring(1);
                    parsePosition.setIndex(parsePosition.getIndex() + 1);
                }
            }
            text = text.substring(parsePosition.getIndex());
            parsePosition.setIndex(0);
        }
        Number result = super.doParse(text, parsePosition, this.withZeros ? 1.0d : baseValue, upperBound, false);
        if (this.withZeros) {
            long n = result.longValue();
            long d = 1;
            while (d <= n) {
                d *= 10;
            }
            while (zeroCount > 0) {
                d *= 10;
                zeroCount--;
            }
            return new Double(n / d);
        }
        return result;
    }

    @Override
    public double composeRuleValue(double newRuleValue, double oldRuleValue) {
        return newRuleValue / oldRuleValue;
    }

    @Override
    public double calcUpperBound(double oldUpperBound) {
        return this.denominator;
    }

    @Override
    char tokenChar() {
        return '<';
    }
}
