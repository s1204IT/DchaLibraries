package android.icu.text;

import android.icu.impl.number.DecimalQuantity_DualStorageBCD;
import java.text.ParsePosition;

class FractionalPartSubstitution extends NFSubstitution {
    private final boolean byDigits;
    private final boolean useSpaces;

    FractionalPartSubstitution(int i, NFRuleSet nFRuleSet, String str) {
        super(i, nFRuleSet, str);
        if (str.equals(">>") || str.equals(">>>") || nFRuleSet == this.ruleSet) {
            this.byDigits = true;
            this.useSpaces = !str.equals(">>>");
        } else {
            this.byDigits = false;
            this.useSpaces = true;
            this.ruleSet.makeIntoFractionRuleSet();
        }
    }

    @Override
    public void doSubstitution(double d, StringBuilder sb, int i, int i2) {
        if (!this.byDigits) {
            super.doSubstitution(d, sb, i, i2);
            return;
        }
        DecimalQuantity_DualStorageBCD decimalQuantity_DualStorageBCD = new DecimalQuantity_DualStorageBCD(d);
        decimalQuantity_DualStorageBCD.roundToInfinity();
        boolean z = false;
        for (int lowerDisplayMagnitude = decimalQuantity_DualStorageBCD.getLowerDisplayMagnitude(); lowerDisplayMagnitude < 0; lowerDisplayMagnitude++) {
            if (z && this.useSpaces) {
                sb.insert(this.pos + i, ' ');
            } else {
                z = true;
            }
            this.ruleSet.format(decimalQuantity_DualStorageBCD.getDigit(lowerDisplayMagnitude), sb, i + this.pos, i2);
        }
    }

    @Override
    public long transformNumber(long j) {
        return 0L;
    }

    @Override
    public double transformNumber(double d) {
        return d - Math.floor(d);
    }

    @Override
    public Number doParse(String str, ParsePosition parsePosition, double d, double d2, boolean z) {
        Number number;
        if (!this.byDigits) {
            return super.doParse(str, parsePosition, d, 0.0d, z);
        }
        ParsePosition parsePosition2 = new ParsePosition(1);
        DecimalQuantity_DualStorageBCD decimalQuantity_DualStorageBCD = new DecimalQuantity_DualStorageBCD();
        int i = 0;
        while (str.length() > 0 && parsePosition2.getIndex() != 0) {
            parsePosition2.setIndex(0);
            int iIntValue = this.ruleSet.parse(str, parsePosition2, 10.0d).intValue();
            if (z && parsePosition2.getIndex() == 0 && (number = this.ruleSet.owner.getDecimalFormat().parse(str, parsePosition2)) != null) {
                iIntValue = number.intValue();
            }
            if (parsePosition2.getIndex() != 0) {
                if (iIntValue != 0) {
                    decimalQuantity_DualStorageBCD.appendDigit((byte) iIntValue, i, false);
                    i = 0;
                } else {
                    i++;
                }
                parsePosition.setIndex(parsePosition.getIndex() + parsePosition2.getIndex());
                str = str.substring(parsePosition2.getIndex());
                while (str.length() > 0 && str.charAt(0) == ' ') {
                    str = str.substring(1);
                    parsePosition.setIndex(parsePosition.getIndex() + 1);
                }
            }
        }
        return new Double(composeRuleValue(decimalQuantity_DualStorageBCD.toDouble(), d));
    }

    @Override
    public double composeRuleValue(double d, double d2) {
        return d + d2;
    }

    @Override
    public double calcUpperBound(double d) {
        return 0.0d;
    }

    @Override
    char tokenChar() {
        return '>';
    }
}
