package android.icu.text;

import java.text.ParsePosition;

class FractionalPartSubstitution extends NFSubstitution {
    private final boolean byDigits;
    private final boolean useSpaces;

    FractionalPartSubstitution(int pos, NFRuleSet ruleSet, String description) {
        super(pos, ruleSet, description);
        if (description.equals(">>") || description.equals(">>>") || ruleSet == this.ruleSet) {
            this.byDigits = true;
            this.useSpaces = description.equals(">>>") ? false : true;
        } else {
            this.byDigits = false;
            this.useSpaces = true;
            this.ruleSet.makeIntoFractionRuleSet();
        }
    }

    @Override
    public void doSubstitution(double number, StringBuffer toInsertInto, int position, int recursionCount) {
        if (!this.byDigits) {
            super.doSubstitution(number, toInsertInto, position, recursionCount);
            return;
        }
        DigitList dl = new DigitList();
        dl.set(number, 20, true);
        boolean pad = false;
        while (dl.count > Math.max(0, dl.decimalAt)) {
            if (pad && this.useSpaces) {
                toInsertInto.insert(this.pos + position, ' ');
            } else {
                pad = true;
            }
            NFRuleSet nFRuleSet = this.ruleSet;
            byte[] bArr = dl.digits;
            dl.count = dl.count - 1;
            nFRuleSet.format(bArr[r3] - 48, toInsertInto, position + this.pos, recursionCount);
        }
        while (dl.decimalAt < 0) {
            if (pad && this.useSpaces) {
                toInsertInto.insert(this.pos + position, ' ');
            } else {
                pad = true;
            }
            this.ruleSet.format(0L, toInsertInto, position + this.pos, recursionCount);
            dl.decimalAt++;
        }
    }

    @Override
    public long transformNumber(long number) {
        return 0L;
    }

    @Override
    public double transformNumber(double number) {
        return number - Math.floor(number);
    }

    @Override
    public Number doParse(String text, ParsePosition parsePosition, double baseValue, double upperBound, boolean lenientParse) {
        Number n;
        if (!this.byDigits) {
            return super.doParse(text, parsePosition, baseValue, 0.0d, lenientParse);
        }
        String workText = text;
        ParsePosition workPos = new ParsePosition(1);
        DigitList dl = new DigitList();
        while (workText.length() > 0 && workPos.getIndex() != 0) {
            workPos.setIndex(0);
            int digit = this.ruleSet.parse(workText, workPos, 10.0d).intValue();
            if (lenientParse && workPos.getIndex() == 0 && (n = this.ruleSet.owner.getDecimalFormat().parse(workText, workPos)) != null) {
                digit = n.intValue();
            }
            if (workPos.getIndex() != 0) {
                dl.append(digit + 48);
                parsePosition.setIndex(parsePosition.getIndex() + workPos.getIndex());
                workText = workText.substring(workPos.getIndex());
                while (workText.length() > 0 && workText.charAt(0) == ' ') {
                    workText = workText.substring(1);
                    parsePosition.setIndex(parsePosition.getIndex() + 1);
                }
            }
        }
        double result = dl.count == 0 ? 0.0d : dl.getDouble();
        return new Double(composeRuleValue(result, baseValue));
    }

    @Override
    public double composeRuleValue(double newRuleValue, double oldRuleValue) {
        return newRuleValue + oldRuleValue;
    }

    @Override
    public double calcUpperBound(double oldUpperBound) {
        return 0.0d;
    }

    @Override
    char tokenChar() {
        return '>';
    }
}
