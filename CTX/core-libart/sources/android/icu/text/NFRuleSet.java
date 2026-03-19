package android.icu.text;

import android.icu.impl.PatternProps;
import android.icu.impl.Utility;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.LinkedList;

final class NFRuleSet {
    static final boolean $assertionsDisabled = false;
    static final int IMPROPER_FRACTION_RULE_INDEX = 1;
    static final int INFINITY_RULE_INDEX = 4;
    static final int MASTER_RULE_INDEX = 3;
    static final int NAN_RULE_INDEX = 5;
    static final int NEGATIVE_RULE_INDEX = 0;
    static final int PROPER_FRACTION_RULE_INDEX = 2;
    private static final int RECURSION_LIMIT = 64;
    LinkedList<NFRule> fractionRules;
    private final boolean isParseable;
    private final String name;
    final RuleBasedNumberFormat owner;
    private NFRule[] rules;
    final NFRule[] nonNumericalRules = new NFRule[6];
    private boolean isFractionRuleSet = false;

    public NFRuleSet(RuleBasedNumberFormat ruleBasedNumberFormat, String[] strArr, int i) throws IllegalArgumentException {
        this.owner = ruleBasedNumberFormat;
        String strSubstring = strArr[i];
        if (strSubstring.length() == 0) {
            throw new IllegalArgumentException("Empty rule set description");
        }
        if (strSubstring.charAt(0) == '%') {
            int iIndexOf = strSubstring.indexOf(58);
            if (iIndexOf == -1) {
                throw new IllegalArgumentException("Rule set name doesn't end in colon");
            }
            String strSubstring2 = strSubstring.substring(0, iIndexOf);
            this.isParseable = true ^ strSubstring2.endsWith("@noparse");
            this.name = this.isParseable ? strSubstring2 : strSubstring2.substring(0, strSubstring2.length() - 8);
            while (iIndexOf < strSubstring.length()) {
                iIndexOf++;
                if (!PatternProps.isWhiteSpace(strSubstring.charAt(iIndexOf))) {
                    break;
                }
            }
            strSubstring = strSubstring.substring(iIndexOf);
            strArr[i] = strSubstring;
        } else {
            this.name = "%default";
            this.isParseable = true;
        }
        if (strSubstring.length() == 0) {
            throw new IllegalArgumentException("Empty rule set description");
        }
    }

    public void parseRules(String str) {
        ArrayList<NFRule> arrayList = new ArrayList();
        int length = str.length();
        NFRule nFRule = null;
        int i = 0;
        do {
            int iIndexOf = str.indexOf(59, i);
            if (iIndexOf < 0) {
                iIndexOf = length;
            }
            NFRule.makeRules(str.substring(i, iIndexOf), this, nFRule, this.owner, arrayList);
            if (!arrayList.isEmpty()) {
                nFRule = (NFRule) arrayList.get(arrayList.size() - 1);
            }
            i = iIndexOf + 1;
        } while (i < length);
        long j = 0;
        for (NFRule nFRule2 : arrayList) {
            long baseValue = nFRule2.getBaseValue();
            if (baseValue == 0) {
                nFRule2.setBaseValue(j);
            } else {
                if (baseValue < j) {
                    throw new IllegalArgumentException("Rules are not in order, base: " + baseValue + " < " + j);
                }
                j = baseValue;
            }
            if (!this.isFractionRuleSet) {
                j++;
            }
        }
        this.rules = new NFRule[arrayList.size()];
        arrayList.toArray(this.rules);
    }

    void setNonNumericalRule(NFRule nFRule) {
        long baseValue = nFRule.getBaseValue();
        if (baseValue == -1) {
            this.nonNumericalRules[0] = nFRule;
            return;
        }
        if (baseValue == -2) {
            setBestFractionRule(1, nFRule, true);
            return;
        }
        if (baseValue == -3) {
            setBestFractionRule(2, nFRule, true);
            return;
        }
        if (baseValue == -4) {
            setBestFractionRule(3, nFRule, true);
        } else if (baseValue == -5) {
            this.nonNumericalRules[4] = nFRule;
        } else if (baseValue == -6) {
            this.nonNumericalRules[5] = nFRule;
        }
    }

    private void setBestFractionRule(int i, NFRule nFRule, boolean z) {
        if (z) {
            if (this.fractionRules == null) {
                this.fractionRules = new LinkedList<>();
            }
            this.fractionRules.add(nFRule);
        }
        if (this.nonNumericalRules[i] == null) {
            this.nonNumericalRules[i] = nFRule;
        } else if (this.owner.getDecimalFormatSymbols().getDecimalSeparator() == nFRule.getDecimalPoint()) {
            this.nonNumericalRules[i] = nFRule;
        }
    }

    public void makeIntoFractionRuleSet() {
        this.isFractionRuleSet = true;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof NFRuleSet)) {
            return false;
        }
        NFRuleSet nFRuleSet = (NFRuleSet) obj;
        if (!this.name.equals(nFRuleSet.name) || this.rules.length != nFRuleSet.rules.length || this.isFractionRuleSet != nFRuleSet.isFractionRuleSet) {
            return false;
        }
        for (int i = 0; i < this.nonNumericalRules.length; i++) {
            if (!Utility.objectEquals(this.nonNumericalRules[i], nFRuleSet.nonNumericalRules[i])) {
                return false;
            }
        }
        for (int i2 = 0; i2 < this.rules.length; i2++) {
            if (!this.rules[i2].equals(nFRuleSet.rules[i2])) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        return 42;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.name);
        sb.append(":\n");
        for (NFRule nFRule : this.rules) {
            sb.append(nFRule.toString());
            sb.append("\n");
        }
        for (NFRule nFRule2 : this.nonNumericalRules) {
            if (nFRule2 != null) {
                if (nFRule2.getBaseValue() == -2 || nFRule2.getBaseValue() == -3 || nFRule2.getBaseValue() == -4) {
                    for (NFRule nFRule3 : this.fractionRules) {
                        if (nFRule3.getBaseValue() == nFRule2.getBaseValue()) {
                            sb.append(nFRule3.toString());
                            sb.append("\n");
                        }
                    }
                } else {
                    sb.append(nFRule2.toString());
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    public boolean isFractionSet() {
        return this.isFractionRuleSet;
    }

    public String getName() {
        return this.name;
    }

    public boolean isPublic() {
        return !this.name.startsWith("%%");
    }

    public boolean isParseable() {
        return this.isParseable;
    }

    public void format(long j, StringBuilder sb, int i, int i2) {
        if (i2 >= 64) {
            throw new IllegalStateException("Recursion limit exceeded when applying ruleSet " + this.name);
        }
        findNormalRule(j).doFormat(j, sb, i, i2 + 1);
    }

    public void format(double d, StringBuilder sb, int i, int i2) {
        if (i2 >= 64) {
            throw new IllegalStateException("Recursion limit exceeded when applying ruleSet " + this.name);
        }
        findRule(d).doFormat(d, sb, i, i2 + 1);
    }

    NFRule findRule(double d) {
        if (this.isFractionRuleSet) {
            return findFractionRuleSetRule(d);
        }
        if (Double.isNaN(d)) {
            NFRule nFRule = this.nonNumericalRules[5];
            if (nFRule == null) {
                return this.owner.getDefaultNaNRule();
            }
            return nFRule;
        }
        if (d < 0.0d) {
            if (this.nonNumericalRules[0] != null) {
                return this.nonNumericalRules[0];
            }
            d = -d;
        }
        if (Double.isInfinite(d)) {
            NFRule nFRule2 = this.nonNumericalRules[4];
            if (nFRule2 == null) {
                return this.owner.getDefaultInfinityRule();
            }
            return nFRule2;
        }
        if (d != Math.floor(d)) {
            if (d < 1.0d && this.nonNumericalRules[2] != null) {
                return this.nonNumericalRules[2];
            }
            if (this.nonNumericalRules[1] != null) {
                return this.nonNumericalRules[1];
            }
        }
        if (this.nonNumericalRules[3] != null) {
            return this.nonNumericalRules[3];
        }
        return findNormalRule(Math.round(d));
    }

    private NFRule findNormalRule(long j) {
        if (this.isFractionRuleSet) {
            return findFractionRuleSetRule(j);
        }
        int i = 0;
        if (j < 0) {
            if (this.nonNumericalRules[0] != null) {
                return this.nonNumericalRules[0];
            }
            j = -j;
        }
        int length = this.rules.length;
        if (length > 0) {
            while (i < length) {
                int i2 = (i + length) >>> 1;
                long baseValue = this.rules[i2].getBaseValue();
                if (baseValue == j) {
                    return this.rules[i2];
                }
                if (baseValue <= j) {
                    i = i2 + 1;
                } else {
                    length = i2;
                }
            }
            if (length == 0) {
                throw new IllegalStateException("The rule set " + this.name + " cannot format the value " + j);
            }
            NFRule nFRule = this.rules[length - 1];
            if (nFRule.shouldRollBack(j)) {
                if (length == 1) {
                    throw new IllegalStateException("The rule set " + this.name + " cannot roll back from the rule '" + nFRule + "'");
                }
                return this.rules[length - 2];
            }
            return nFRule;
        }
        return this.nonNumericalRules[3];
    }

    private NFRule findFractionRuleSetRule(double d) {
        int i = 0;
        long baseValue = this.rules[0].getBaseValue();
        for (int i2 = 1; i2 < this.rules.length; i2++) {
            baseValue = lcm(baseValue, this.rules[i2].getBaseValue());
        }
        long jRound = Math.round(baseValue * d);
        long j = Long.MAX_VALUE;
        int i3 = 0;
        while (true) {
            if (i >= this.rules.length) {
                break;
            }
            long baseValue2 = (this.rules[i].getBaseValue() * jRound) % baseValue;
            long j2 = baseValue - baseValue2;
            if (j2 < baseValue2) {
                baseValue2 = j2;
            }
            if (baseValue2 < j) {
                if (baseValue2 != 0) {
                    i3 = i;
                    j = baseValue2;
                } else {
                    i3 = i;
                    break;
                }
            }
            i++;
        }
        int i4 = i3 + 1;
        if (i4 < this.rules.length && this.rules[i4].getBaseValue() == this.rules[i3].getBaseValue() && (Math.round(this.rules[i3].getBaseValue() * d) < 1 || Math.round(d * this.rules[i3].getBaseValue()) >= 2)) {
            i3 = i4;
        }
        return this.rules[i3];
    }

    private static long lcm(long j, long j2) {
        long j3;
        long j4;
        long j5;
        long j6 = j2;
        int i = 0;
        long j7 = j;
        while (true) {
            j3 = j7 & 1;
            if (j3 != 0 || (j6 & 1) != 0) {
                break;
            }
            i++;
            j7 >>= 1;
            j6 >>= 1;
        }
        if (j3 == 1) {
            long j8 = j7;
            j7 = -j6;
            j4 = j6;
            j5 = j8;
        } else {
            j4 = j6;
            j5 = j7;
        }
        while (j7 != 0) {
            while ((j7 & 1) == 0) {
                j7 >>= 1;
            }
            if (j7 <= 0) {
                j4 = -j7;
            } else {
                j5 = j7;
            }
            j7 = j5 - j4;
        }
        return (j / (j5 << i)) * j2;
    }

    public Number parse(String str, ParsePosition parsePosition, double d) {
        ParsePosition parsePosition2 = new ParsePosition(0);
        Long l = NFRule.ZERO;
        if (str.length() == 0) {
            return l;
        }
        Number number = l;
        for (NFRule nFRule : this.nonNumericalRules) {
            if (nFRule != null) {
                Number numberDoParse = nFRule.doParse(str, parsePosition, false, d);
                if (parsePosition.getIndex() > parsePosition2.getIndex()) {
                    parsePosition2.setIndex(parsePosition.getIndex());
                    number = numberDoParse;
                }
                parsePosition.setIndex(0);
            }
        }
        for (int length = this.rules.length - 1; length >= 0 && parsePosition2.getIndex() < str.length(); length--) {
            if (this.isFractionRuleSet || this.rules[length].getBaseValue() < d) {
                Number numberDoParse2 = this.rules[length].doParse(str, parsePosition, this.isFractionRuleSet, d);
                if (parsePosition.getIndex() > parsePosition2.getIndex()) {
                    parsePosition2.setIndex(parsePosition.getIndex());
                    number = numberDoParse2;
                }
                parsePosition.setIndex(0);
            }
        }
        parsePosition.setIndex(parsePosition2.getIndex());
        return number;
    }

    public void setDecimalFormatSymbols(DecimalFormatSymbols decimalFormatSymbols) {
        for (NFRule nFRule : this.rules) {
            nFRule.setDecimalFormatSymbols(decimalFormatSymbols);
        }
        if (this.fractionRules != null) {
            for (int i = 1; i <= 3; i++) {
                if (this.nonNumericalRules[i] != null) {
                    for (NFRule nFRule2 : this.fractionRules) {
                        if (this.nonNumericalRules[i].getBaseValue() == nFRule2.getBaseValue()) {
                            setBestFractionRule(i, nFRule2, false);
                        }
                    }
                }
            }
        }
        for (NFRule nFRule3 : this.nonNumericalRules) {
            if (nFRule3 != null) {
                nFRule3.setDecimalFormatSymbols(decimalFormatSymbols);
            }
        }
    }
}
