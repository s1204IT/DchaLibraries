package android.icu.text;

import android.icu.impl.PatternProps;
import android.icu.impl.PatternTokenizer;
import android.icu.impl.Utility;
import android.icu.impl.number.Padder;
import android.icu.text.PluralRules;
import android.icu.util.ULocale;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.List;

final class NFRule {
    static final boolean $assertionsDisabled = false;
    static final int IMPROPER_FRACTION_RULE = -2;
    static final int INFINITY_RULE = -5;
    static final int MASTER_RULE = -4;
    static final int NAN_RULE = -6;
    static final int NEGATIVE_NUMBER_RULE = -1;
    static final int PROPER_FRACTION_RULE = -3;
    private long baseValue;
    private final RuleBasedNumberFormat formatter;
    private String ruleText;
    static final Long ZERO = 0L;
    private static final String[] RULE_PREFIXES = {"<<", "<%", "<#", "<0", ">>", ">%", ">#", ">0", "=%", "=#", "=0"};
    private int radix = 10;
    private short exponent = 0;
    private char decimalPoint = 0;
    private PluralFormat rulePatternFormat = null;
    private NFSubstitution sub1 = null;
    private NFSubstitution sub2 = null;

    public static void makeRules(String str, NFRuleSet nFRuleSet, NFRule nFRule, RuleBasedNumberFormat ruleBasedNumberFormat, List<NFRule> list) {
        NFRule nFRule2;
        NFRule nFRule3 = new NFRule(ruleBasedNumberFormat, str);
        String str2 = nFRule3.ruleText;
        int iIndexOf = str2.indexOf(91);
        int iIndexOf2 = iIndexOf < 0 ? -1 : str2.indexOf(93);
        if (iIndexOf2 < 0 || iIndexOf > iIndexOf2 || nFRule3.baseValue == -3 || nFRule3.baseValue == -1 || nFRule3.baseValue == -5 || nFRule3.baseValue == -6) {
            nFRule3.extractSubstitutions(nFRuleSet, str2, nFRule);
        } else {
            StringBuilder sb = new StringBuilder();
            if ((nFRule3.baseValue > 0 && nFRule3.baseValue % power(nFRule3.radix, nFRule3.exponent) == 0) || nFRule3.baseValue == -2 || nFRule3.baseValue == -4) {
                nFRule2 = new NFRule(ruleBasedNumberFormat, null);
                if (nFRule3.baseValue >= 0) {
                    nFRule2.baseValue = nFRule3.baseValue;
                    if (!nFRuleSet.isFractionSet()) {
                        nFRule3.baseValue++;
                    }
                } else if (nFRule3.baseValue == -2) {
                    nFRule2.baseValue = -3L;
                } else if (nFRule3.baseValue == -4) {
                    nFRule2.baseValue = nFRule3.baseValue;
                    nFRule3.baseValue = -2L;
                }
                nFRule2.radix = nFRule3.radix;
                nFRule2.exponent = nFRule3.exponent;
                sb.append(str2.substring(0, iIndexOf));
                int i = iIndexOf2 + 1;
                if (i < str2.length()) {
                    sb.append(str2.substring(i));
                }
                nFRule2.extractSubstitutions(nFRuleSet, sb.toString(), nFRule);
            } else {
                nFRule2 = null;
            }
            sb.setLength(0);
            sb.append(str2.substring(0, iIndexOf));
            sb.append(str2.substring(iIndexOf + 1, iIndexOf2));
            int i2 = iIndexOf2 + 1;
            if (i2 < str2.length()) {
                sb.append(str2.substring(i2));
            }
            nFRule3.extractSubstitutions(nFRuleSet, sb.toString(), nFRule);
            if (nFRule2 != null) {
                if (nFRule2.baseValue >= 0) {
                    list.add(nFRule2);
                } else {
                    nFRuleSet.setNonNumericalRule(nFRule2);
                }
            }
        }
        if (nFRule3.baseValue >= 0) {
            list.add(nFRule3);
        } else {
            nFRuleSet.setNonNumericalRule(nFRule3);
        }
    }

    public NFRule(RuleBasedNumberFormat ruleBasedNumberFormat, String str) {
        this.ruleText = null;
        this.formatter = ruleBasedNumberFormat;
        this.ruleText = str != null ? parseRuleDescriptor(str) : null;
    }

    private String parseRuleDescriptor(String str) {
        String strSubstring = str;
        int iIndexOf = strSubstring.indexOf(":");
        if (iIndexOf != -1) {
            String strSubstring2 = strSubstring.substring(0, iIndexOf);
            int i = iIndexOf + 1;
            while (i < str.length() && PatternProps.isWhiteSpace(strSubstring.charAt(i))) {
                i++;
            }
            strSubstring = strSubstring.substring(i);
            int length = strSubstring2.length();
            char cCharAt = strSubstring2.charAt(0);
            char cCharAt2 = strSubstring2.charAt(length - 1);
            char c = '0';
            if (cCharAt >= '0') {
                char c2 = '9';
                if (cCharAt <= '9' && cCharAt2 != 'x') {
                    int i2 = 0;
                    char cCharAt3 = 0;
                    long j = 0;
                    while (i2 < length) {
                        cCharAt3 = strSubstring2.charAt(i2);
                        if (cCharAt3 >= '0' && cCharAt3 <= '9') {
                            j = (j * 10) + ((long) (cCharAt3 - '0'));
                        } else {
                            if (cCharAt3 == '/' || cCharAt3 == '>') {
                                break;
                            }
                            if (!PatternProps.isWhiteSpace(cCharAt3) && cCharAt3 != ',' && cCharAt3 != '.') {
                                throw new IllegalArgumentException("Illegal character " + cCharAt3 + " in rule descriptor");
                            }
                        }
                        i2++;
                    }
                    setBaseValue(j);
                    if (cCharAt3 == '/') {
                        i2++;
                        long j2 = 0;
                        while (i2 < length) {
                            cCharAt3 = strSubstring2.charAt(i2);
                            if (cCharAt3 >= c && cCharAt3 <= c2) {
                                j2 = (j2 * 10) + ((long) (cCharAt3 - '0'));
                            } else {
                                if (cCharAt3 == '>') {
                                    break;
                                }
                                if (!PatternProps.isWhiteSpace(cCharAt3) && cCharAt3 != ',' && cCharAt3 != '.') {
                                    throw new IllegalArgumentException("Illegal character " + cCharAt3 + " in rule descriptor");
                                }
                            }
                            i2++;
                            c = '0';
                            c2 = '9';
                        }
                        this.radix = (int) j2;
                        if (this.radix == 0) {
                            throw new IllegalArgumentException("Rule can't have radix of 0");
                        }
                        this.exponent = expectedExponent();
                    }
                    if (cCharAt3 == '>') {
                        while (i2 < length) {
                            if (strSubstring2.charAt(i2) == '>' && this.exponent > 0) {
                                this.exponent = (short) (this.exponent - 1);
                                i2++;
                            } else {
                                throw new IllegalArgumentException("Illegal character in rule descriptor");
                            }
                        }
                    }
                } else if (strSubstring2.equals("-x")) {
                    setBaseValue(-1L);
                } else if (length == 3) {
                    if (cCharAt == '0' && cCharAt2 == 'x') {
                        setBaseValue(-3L);
                        this.decimalPoint = strSubstring2.charAt(1);
                    } else if (cCharAt == 'x' && cCharAt2 == 'x') {
                        setBaseValue(-2L);
                        this.decimalPoint = strSubstring2.charAt(1);
                    } else if (cCharAt == 'x' && cCharAt2 == '0') {
                        setBaseValue(-4L);
                        this.decimalPoint = strSubstring2.charAt(1);
                    } else if (strSubstring2.equals("NaN")) {
                        setBaseValue(-6L);
                    } else if (strSubstring2.equals("Inf")) {
                        setBaseValue(-5L);
                    }
                }
            }
        }
        if (strSubstring.length() > 0 && strSubstring.charAt(0) == '\'') {
            return strSubstring.substring(1);
        }
        return strSubstring;
    }

    private void extractSubstitutions(NFRuleSet nFRuleSet, String str, NFRule nFRule) {
        PluralRules.PluralType pluralType;
        this.ruleText = str;
        this.sub1 = extractSubstitution(nFRuleSet, nFRule);
        if (this.sub1 == null) {
            this.sub2 = null;
        } else {
            this.sub2 = extractSubstitution(nFRuleSet, nFRule);
        }
        String str2 = this.ruleText;
        int iIndexOf = str2.indexOf("$(");
        int iIndexOf2 = iIndexOf >= 0 ? str2.indexOf(")$", iIndexOf) : -1;
        if (iIndexOf2 >= 0) {
            int iIndexOf3 = str2.indexOf(44, iIndexOf);
            if (iIndexOf3 < 0) {
                throw new IllegalArgumentException("Rule \"" + str2 + "\" does not have a defined type");
            }
            String strSubstring = this.ruleText.substring(iIndexOf + 2, iIndexOf3);
            if ("cardinal".equals(strSubstring)) {
                pluralType = PluralRules.PluralType.CARDINAL;
            } else if ("ordinal".equals(strSubstring)) {
                pluralType = PluralRules.PluralType.ORDINAL;
            } else {
                throw new IllegalArgumentException(strSubstring + " is an unknown type");
            }
            this.rulePatternFormat = this.formatter.createPluralFormat(pluralType, str2.substring(iIndexOf3 + 1, iIndexOf2));
        }
    }

    private NFSubstitution extractSubstitution(NFRuleSet nFRuleSet, NFRule nFRule) {
        int i;
        int iIndexOfAnyRulePrefix = indexOfAnyRulePrefix(this.ruleText);
        if (iIndexOfAnyRulePrefix == -1) {
            return null;
        }
        if (this.ruleText.startsWith(">>>", iIndexOfAnyRulePrefix)) {
            i = iIndexOfAnyRulePrefix + 2;
        } else {
            char cCharAt = this.ruleText.charAt(iIndexOfAnyRulePrefix);
            int iIndexOf = this.ruleText.indexOf(cCharAt, iIndexOfAnyRulePrefix + 1);
            if (cCharAt != '<' || iIndexOf == -1 || iIndexOf >= this.ruleText.length() - 1) {
                i = iIndexOf;
            } else {
                int i2 = iIndexOf + 1;
                if (this.ruleText.charAt(i2) == cCharAt) {
                    i = i2;
                }
            }
        }
        if (i == -1) {
            return null;
        }
        int i3 = i + 1;
        NFSubstitution nFSubstitutionMakeSubstitution = NFSubstitution.makeSubstitution(iIndexOfAnyRulePrefix, this, nFRule, nFRuleSet, this.formatter, this.ruleText.substring(iIndexOfAnyRulePrefix, i3));
        this.ruleText = this.ruleText.substring(0, iIndexOfAnyRulePrefix) + this.ruleText.substring(i3);
        return nFSubstitutionMakeSubstitution;
    }

    final void setBaseValue(long j) {
        this.baseValue = j;
        this.radix = 10;
        if (this.baseValue >= 1) {
            this.exponent = expectedExponent();
            if (this.sub1 != null) {
                this.sub1.setDivisor(this.radix, this.exponent);
            }
            if (this.sub2 != null) {
                this.sub2.setDivisor(this.radix, this.exponent);
                return;
            }
            return;
        }
        this.exponent = (short) 0;
    }

    private short expectedExponent() {
        if (this.radix == 0 || this.baseValue < 1) {
            return (short) 0;
        }
        short sLog = (short) (Math.log(this.baseValue) / Math.log(this.radix));
        short s = (short) (sLog + 1);
        if (power(this.radix, s) <= this.baseValue) {
            return s;
        }
        return sLog;
    }

    private static int indexOfAnyRulePrefix(String str) {
        if (str.length() <= 0) {
            return -1;
        }
        int i = -1;
        for (String str2 : RULE_PREFIXES) {
            int iIndexOf = str.indexOf(str2);
            if (iIndexOf != -1 && (i == -1 || iIndexOf < i)) {
                i = iIndexOf;
            }
        }
        return i;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof NFRule)) {
            return false;
        }
        NFRule nFRule = (NFRule) obj;
        return this.baseValue == nFRule.baseValue && this.radix == nFRule.radix && this.exponent == nFRule.exponent && this.ruleText.equals(nFRule.ruleText) && Utility.objectEquals(this.sub1, nFRule.sub1) && Utility.objectEquals(this.sub2, nFRule.sub2);
    }

    public int hashCode() {
        return 42;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.baseValue == -1) {
            sb.append("-x: ");
        } else if (this.baseValue == -2) {
            sb.append(ULocale.PRIVATE_USE_EXTENSION);
            sb.append(this.decimalPoint != 0 ? this.decimalPoint : '.');
            sb.append("x: ");
        } else if (this.baseValue == -3) {
            sb.append('0');
            sb.append(this.decimalPoint != 0 ? this.decimalPoint : '.');
            sb.append("x: ");
        } else if (this.baseValue == -4) {
            sb.append(ULocale.PRIVATE_USE_EXTENSION);
            sb.append(this.decimalPoint != 0 ? this.decimalPoint : '.');
            sb.append("0: ");
        } else if (this.baseValue == -5) {
            sb.append("Inf: ");
        } else if (this.baseValue == -6) {
            sb.append("NaN: ");
        } else {
            sb.append(String.valueOf(this.baseValue));
            if (this.radix != 10) {
                sb.append('/');
                sb.append(this.radix);
            }
            int iExpectedExponent = expectedExponent() - this.exponent;
            for (int i = 0; i < iExpectedExponent; i++) {
                sb.append('>');
            }
            sb.append(PluralRules.KEYWORD_RULE_SEPARATOR);
        }
        if (this.ruleText.startsWith(Padder.FALLBACK_PADDING_STRING) && (this.sub1 == null || this.sub1.getPos() != 0)) {
            sb.append(PatternTokenizer.SINGLE_QUOTE);
        }
        StringBuilder sb2 = new StringBuilder(this.ruleText);
        if (this.sub2 != null) {
            sb2.insert(this.sub2.getPos(), this.sub2.toString());
        }
        if (this.sub1 != null) {
            sb2.insert(this.sub1.getPos(), this.sub1.toString());
        }
        sb.append(sb2.toString());
        sb.append(';');
        return sb.toString();
    }

    public final char getDecimalPoint() {
        return this.decimalPoint;
    }

    public final long getBaseValue() {
        return this.baseValue;
    }

    public long getDivisor() {
        return power(this.radix, this.exponent);
    }

    public void doFormat(long j, StringBuilder sb, int i, int i2) {
        int length;
        int length2 = this.ruleText.length();
        if (this.rulePatternFormat == null) {
            sb.insert(i, this.ruleText);
            length = 0;
        } else {
            length2 = this.ruleText.indexOf("$(");
            int iIndexOf = this.ruleText.indexOf(")$", length2);
            int length3 = sb.length();
            if (iIndexOf < this.ruleText.length() - 1) {
                sb.insert(i, this.ruleText.substring(iIndexOf + 2));
            }
            sb.insert(i, this.rulePatternFormat.format(j / power(this.radix, this.exponent)));
            if (length2 > 0) {
                sb.insert(i, this.ruleText.substring(0, length2));
            }
            length = this.ruleText.length() - (sb.length() - length3);
        }
        if (this.sub2 != null) {
            this.sub2.doSubstitution(j, sb, i - (this.sub2.getPos() > length2 ? length : 0), i2);
        }
        if (this.sub1 != null) {
            NFSubstitution nFSubstitution = this.sub1;
            if (this.sub1.getPos() <= length2) {
                length = 0;
            }
            nFSubstitution.doSubstitution(j, sb, i - length, i2);
        }
    }

    public void doFormat(double d, StringBuilder sb, int i, int i2) {
        double dPower;
        int length;
        int length2 = this.ruleText.length();
        if (this.rulePatternFormat == null) {
            sb.insert(i, this.ruleText);
            length = 0;
        } else {
            length2 = this.ruleText.indexOf("$(");
            int iIndexOf = this.ruleText.indexOf(")$", length2);
            int length3 = sb.length();
            if (iIndexOf < this.ruleText.length() - 1) {
                sb.insert(i, this.ruleText.substring(iIndexOf + 2));
            }
            if (0.0d <= d && d < 1.0d) {
                dPower = Math.round(power(this.radix, this.exponent) * d);
            } else {
                dPower = d / power(this.radix, this.exponent);
            }
            sb.insert(i, this.rulePatternFormat.format((long) dPower));
            if (length2 > 0) {
                sb.insert(i, this.ruleText.substring(0, length2));
            }
            length = this.ruleText.length() - (sb.length() - length3);
        }
        if (this.sub2 != null) {
            this.sub2.doSubstitution(d, sb, i - (this.sub2.getPos() > length2 ? length : 0), i2);
        }
        if (this.sub1 != null) {
            NFSubstitution nFSubstitution = this.sub1;
            if (this.sub1.getPos() <= length2) {
                length = 0;
            }
            nFSubstitution.doSubstitution(d, sb, i - length, i2);
        }
    }

    static long power(long j, short s) {
        if (s < 0) {
            throw new IllegalArgumentException("Exponent can not be negative");
        }
        if (j < 0) {
            throw new IllegalArgumentException("Base can not be negative");
        }
        long j2 = 1;
        while (s > 0) {
            if ((s & 1) == 1) {
                j2 *= j;
            }
            j *= j;
            s = (short) (s >> 1);
        }
        return j2;
    }

    public boolean shouldRollBack(long j) {
        if ((this.sub1 == null || !this.sub1.isModulusSubstitution()) && (this.sub2 == null || !this.sub2.isModulusSubstitution())) {
            return false;
        }
        long jPower = power(this.radix, this.exponent);
        return j % jPower == 0 && this.baseValue % jPower != 0;
    }

    public Number doParse(String str, ParsePosition parsePosition, boolean z, double d) {
        int i;
        int i2;
        int i3;
        int i4;
        int i5 = 0;
        ParsePosition parsePosition2 = new ParsePosition(0);
        int pos = this.sub1 != null ? this.sub1.getPos() : this.ruleText.length();
        int pos2 = this.sub2 != null ? this.sub2.getPos() : this.ruleText.length();
        String strStripPrefix = stripPrefix(str, this.ruleText.substring(0, pos), parsePosition2);
        int length = str.length() - strStripPrefix.length();
        if (parsePosition2.getIndex() == 0 && pos != 0) {
            return ZERO;
        }
        if (this.baseValue == -5) {
            parsePosition.setIndex(parsePosition2.getIndex());
            return Double.valueOf(Double.POSITIVE_INFINITY);
        }
        if (this.baseValue == -6) {
            parsePosition.setIndex(parsePosition2.getIndex());
            return Double.valueOf(Double.NaN);
        }
        double dMax = Math.max(0L, this.baseValue);
        double d2 = 0.0d;
        int index = 0;
        int i6 = 0;
        while (true) {
            parsePosition2.setIndex(i5);
            int i7 = index;
            double d3 = dMax;
            int i8 = pos2;
            String str2 = strStripPrefix;
            double dDoubleValue = matchToDelimiter(strStripPrefix, i6, dMax, this.ruleText.substring(pos, pos2), this.rulePatternFormat, parsePosition2, this.sub1, d).doubleValue();
            if (parsePosition2.getIndex() != 0 || this.sub1 == null) {
                int index2 = parsePosition2.getIndex();
                String strSubstring = str2.substring(parsePosition2.getIndex());
                ParsePosition parsePosition3 = new ParsePosition(0);
                i = i8;
                i2 = 0;
                double dDoubleValue2 = matchToDelimiter(strSubstring, 0, dDoubleValue, this.ruleText.substring(i8), this.rulePatternFormat, parsePosition3, this.sub2, d).doubleValue();
                if (parsePosition3.getIndex() != 0 || this.sub2 == null) {
                    i3 = i7;
                    if (length + parsePosition2.getIndex() + parsePosition3.getIndex() > i3) {
                        d2 = dDoubleValue2;
                        index = length + parsePosition2.getIndex() + parsePosition3.getIndex();
                    }
                    i4 = index2;
                } else {
                    i3 = i7;
                }
                index = i3;
                i4 = index2;
            } else {
                i4 = i6;
                index = i7;
                i = i8;
                i2 = 0;
            }
            int i9 = i;
            if (pos == i9 || parsePosition2.getIndex() <= 0 || parsePosition2.getIndex() >= str2.length() || parsePosition2.getIndex() == i4) {
                break;
            }
            i6 = i4;
            pos2 = i9;
            strStripPrefix = str2;
            i5 = i2;
            dMax = d3;
        }
        parsePosition.setIndex(index);
        if (z && index > 0 && this.sub1 == null) {
            d2 = 1.0d / d2;
        }
        double d4 = d2;
        long j = (long) d4;
        if (d4 == j) {
            return Long.valueOf(j);
        }
        return new Double(d4);
    }

    private String stripPrefix(String str, String str2, ParsePosition parsePosition) {
        int iPrefixLength;
        if (str2.length() != 0 && (iPrefixLength = prefixLength(str, str2)) != 0) {
            parsePosition.setIndex(parsePosition.getIndex() + iPrefixLength);
            return str.substring(iPrefixLength);
        }
        return str;
    }

    private Number matchToDelimiter(String str, int i, double d, String str2, PluralFormat pluralFormat, ParsePosition parsePosition, NFSubstitution nFSubstitution, double d2) {
        if (!allIgnorable(str2)) {
            ParsePosition parsePosition2 = new ParsePosition(0);
            int[] iArrFindText = findText(str, str2, pluralFormat, i);
            int i2 = iArrFindText[0];
            int i3 = iArrFindText[1];
            while (i2 >= 0) {
                String strSubstring = str.substring(0, i2);
                if (strSubstring.length() > 0) {
                    Number numberDoParse = nFSubstitution.doParse(strSubstring, parsePosition2, d, d2, this.formatter.lenientParseEnabled());
                    if (parsePosition2.getIndex() == i2) {
                        parsePosition.setIndex(i2 + i3);
                        return numberDoParse;
                    }
                }
                parsePosition2.setIndex(0);
                int[] iArrFindText2 = findText(str, str2, pluralFormat, i2 + i3);
                i2 = iArrFindText2[0];
                i3 = iArrFindText2[1];
            }
            parsePosition.setIndex(0);
            return ZERO;
        }
        if (nFSubstitution == null) {
            return Double.valueOf(d);
        }
        ParsePosition parsePosition3 = new ParsePosition(0);
        Long l = ZERO;
        Number numberDoParse2 = nFSubstitution.doParse(str, parsePosition3, d, d2, this.formatter.lenientParseEnabled());
        if (parsePosition3.getIndex() == 0) {
            return l;
        }
        parsePosition.setIndex(parsePosition3.getIndex());
        return numberDoParse2 != null ? numberDoParse2 : l;
    }

    private int prefixLength(String str, String str2) {
        if (str2.length() == 0) {
            return 0;
        }
        RbnfLenientScanner lenientScanner = this.formatter.getLenientScanner();
        if (lenientScanner != null) {
            return lenientScanner.prefixLength(str, str2);
        }
        if (str.startsWith(str2)) {
            return str2.length();
        }
        return 0;
    }

    private int[] findText(String str, String str2, PluralFormat pluralFormat, int i) {
        RbnfLenientScanner lenientScanner = this.formatter.getLenientScanner();
        if (pluralFormat != null) {
            FieldPosition fieldPosition = new FieldPosition(0);
            fieldPosition.setBeginIndex(i);
            pluralFormat.parseType(str, lenientScanner, fieldPosition);
            int beginIndex = fieldPosition.getBeginIndex();
            if (beginIndex >= 0) {
                int iIndexOf = this.ruleText.indexOf("$(");
                int iIndexOf2 = this.ruleText.indexOf(")$", iIndexOf) + 2;
                int endIndex = fieldPosition.getEndIndex() - beginIndex;
                String strSubstring = this.ruleText.substring(0, iIndexOf);
                String strSubstring2 = this.ruleText.substring(iIndexOf2);
                if (str.regionMatches(beginIndex - strSubstring.length(), strSubstring, 0, strSubstring.length()) && str.regionMatches(beginIndex + endIndex, strSubstring2, 0, strSubstring2.length())) {
                    return new int[]{beginIndex - strSubstring.length(), endIndex + strSubstring.length() + strSubstring2.length()};
                }
            }
            return new int[]{-1, 0};
        }
        if (lenientScanner != null) {
            return lenientScanner.findText(str, str2, i);
        }
        return new int[]{str.indexOf(str2, i), str2.length()};
    }

    private boolean allIgnorable(String str) {
        if (str == null || str.length() == 0) {
            return true;
        }
        RbnfLenientScanner lenientScanner = this.formatter.getLenientScanner();
        return lenientScanner != null && lenientScanner.allIgnorable(str);
    }

    public void setDecimalFormatSymbols(DecimalFormatSymbols decimalFormatSymbols) {
        if (this.sub1 != null) {
            this.sub1.setDecimalFormatSymbols(decimalFormatSymbols);
        }
        if (this.sub2 != null) {
            this.sub2.setDecimalFormatSymbols(decimalFormatSymbols);
        }
    }
}
