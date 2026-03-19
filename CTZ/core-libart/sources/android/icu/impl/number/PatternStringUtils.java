package android.icu.impl.number;

import android.icu.impl.PatternTokenizer;
import android.icu.impl.locale.LanguageTag;
import android.icu.impl.number.Padder;
import android.icu.text.DateFormat;
import android.icu.text.DecimalFormatSymbols;
import java.lang.reflect.Array;
import java.math.BigDecimal;

public class PatternStringUtils {
    static final boolean $assertionsDisabled = false;

    public static String propertiesToPatternString(DecimalFormatProperties decimalFormatProperties) {
        int i;
        int i2;
        int i3;
        int i4;
        int iMin;
        int i5;
        String str;
        String str2;
        StringBuilder sb = new StringBuilder();
        int iMin2 = Math.min(decimalFormatProperties.getSecondaryGroupingSize(), 100);
        int iMin3 = Math.min(decimalFormatProperties.getGroupingSize(), 100);
        int iMin4 = Math.min(decimalFormatProperties.getFormatWidth(), 100);
        Padder.PadPosition padPosition = decimalFormatProperties.getPadPosition();
        String padString = decimalFormatProperties.getPadString();
        int iMax = Math.max(Math.min(decimalFormatProperties.getMinimumIntegerDigits(), 100), 0);
        int iMin5 = Math.min(decimalFormatProperties.getMaximumIntegerDigits(), 100);
        int iMax2 = Math.max(Math.min(decimalFormatProperties.getMinimumFractionDigits(), 100), 0);
        int iMin6 = Math.min(decimalFormatProperties.getMaximumFractionDigits(), 100);
        int iMin7 = Math.min(decimalFormatProperties.getMinimumSignificantDigits(), 100);
        int iMin8 = Math.min(decimalFormatProperties.getMaximumSignificantDigits(), 100);
        boolean decimalSeparatorAlwaysShown = decimalFormatProperties.getDecimalSeparatorAlwaysShown();
        int iMin9 = Math.min(decimalFormatProperties.getMinimumExponentDigits(), 100);
        boolean exponentSignAlwaysShown = decimalFormatProperties.getExponentSignAlwaysShown();
        String positivePrefix = decimalFormatProperties.getPositivePrefix();
        String positivePrefixPattern = decimalFormatProperties.getPositivePrefixPattern();
        String positiveSuffix = decimalFormatProperties.getPositiveSuffix();
        String positiveSuffixPattern = decimalFormatProperties.getPositiveSuffixPattern();
        String negativePrefix = decimalFormatProperties.getNegativePrefix();
        String negativePrefixPattern = decimalFormatProperties.getNegativePrefixPattern();
        String negativeSuffix = decimalFormatProperties.getNegativeSuffix();
        String negativeSuffixPattern = decimalFormatProperties.getNegativeSuffixPattern();
        if (positivePrefixPattern != null) {
            sb.append(positivePrefixPattern);
        }
        AffixUtils.escape(positivePrefix, sb);
        int length = sb.length();
        if (iMin2 == Math.min(100, -1) || iMin3 == Math.min(100, -1) || iMin2 == iMin3) {
            if (iMin2 != Math.min(100, -1)) {
                i = iMin2;
                iMin3 = i;
            } else if (iMin3 != Math.min(100, -1)) {
                i = iMin2;
            } else {
                iMin2 = 0;
                i = 0;
                iMin3 = 0;
            }
            iMin2 = 0;
        } else {
            i = iMin2;
        }
        int i6 = iMin2 + iMin3 + 1;
        BigDecimal roundingIncrement = decimalFormatProperties.getRoundingIncrement();
        StringBuilder sb2 = new StringBuilder();
        if (iMin8 == Math.min(100, -1)) {
            if (roundingIncrement != null) {
                i2 = -roundingIncrement.scale();
                String plainString = roundingIncrement.scaleByPowerOfTen(roundingIncrement.scale()).toPlainString();
                if (plainString.charAt(0) == '-') {
                    sb2.append((CharSequence) plainString, 1, plainString.length());
                } else {
                    sb2.append(plainString);
                }
            }
            while (sb2.length() + i2 < iMax) {
                sb2.insert(0, '0');
            }
            while ((-i2) < iMax2) {
                sb2.append('0');
                i2--;
            }
            int iMax3 = Math.max(i6, sb2.length() + i2);
            if (iMin5 == 100) {
                iMax3 = Math.max(iMin5, iMax3);
                i3 = 1;
            } else {
                i3 = 1;
            }
            iMin = iMin6 == 100 ? Math.min(-iMin6, i2) : i2;
            for (i4 = iMax3 - i3; i4 >= iMin; i4--) {
                int length2 = ((sb2.length() + i2) - i4) - 1;
                if (length2 < 0 || length2 >= sb2.length()) {
                    sb.append('#');
                } else {
                    sb.append(sb2.charAt(length2));
                }
                if (i4 > iMin3 && i > 0 && (i4 - iMin3) % i == 0) {
                    sb.append(',');
                } else if (i4 > 0 && i4 == iMin3) {
                    sb.append(',');
                } else if (i4 == 0 && (decimalSeparatorAlwaysShown || iMin < 0)) {
                    sb.append('.');
                }
            }
            if (iMin9 != Math.min(100, -1)) {
                sb.append('E');
                if (exponentSignAlwaysShown) {
                    sb.append('+');
                }
                for (int i7 = 0; i7 < iMin9; i7++) {
                    sb.append('0');
                }
            }
            int length3 = sb.length();
            if (positiveSuffixPattern != null) {
                sb.append(positiveSuffixPattern);
            }
            AffixUtils.escape(positiveSuffix, sb);
            if (iMin4 == -1) {
                while (iMin4 - sb.length() > 0) {
                    sb.insert(length, '#');
                    length3++;
                }
                i5 = length;
                switch (padPosition) {
                    case BEFORE_PREFIX:
                        int iEscapePaddingString = escapePaddingString(padString, sb, 0);
                        sb.insert(0, '*');
                        int i8 = iEscapePaddingString + 1;
                        length3 += i8;
                        i5 += i8;
                        break;
                    case AFTER_PREFIX:
                        int iEscapePaddingString2 = escapePaddingString(padString, sb, i5);
                        sb.insert(i5, '*');
                        int i9 = iEscapePaddingString2 + 1;
                        i5 += i9;
                        length3 += i9;
                        break;
                    case BEFORE_SUFFIX:
                        escapePaddingString(padString, sb, length3);
                        sb.insert(length3, '*');
                        break;
                    case AFTER_SUFFIX:
                        sb.append('*');
                        escapePaddingString(padString, sb, sb.length());
                        break;
                }
            } else {
                i5 = length;
            }
            if (negativePrefix != null && negativeSuffix == null && (negativePrefixPattern != null || negativeSuffixPattern == null)) {
                if (negativePrefixPattern != null) {
                    str = negativePrefixPattern;
                    if (str.length() == 1 && str.charAt(0) == '-') {
                        str2 = negativeSuffixPattern;
                        if (str2.length() != 0) {
                        }
                    }
                    sb.append(';');
                    if (str != null) {
                        sb.append(str);
                    }
                    AffixUtils.escape(negativePrefix, sb);
                    sb.append((CharSequence) sb, i5, length3);
                    if (str2 != null) {
                        sb.append(str2);
                    }
                    AffixUtils.escape(negativeSuffix, sb);
                }
                return sb.toString();
            }
            str = negativePrefixPattern;
            str2 = negativeSuffixPattern;
            sb.append(';');
            if (str != null) {
            }
            AffixUtils.escape(negativePrefix, sb);
            sb.append((CharSequence) sb, i5, length3);
            if (str2 != null) {
            }
            AffixUtils.escape(negativeSuffix, sb);
            return sb.toString();
        }
        while (sb2.length() < iMin7) {
            sb2.append('@');
        }
        while (sb2.length() < iMin8) {
            sb2.append('#');
        }
        i2 = 0;
        while (sb2.length() + i2 < iMax) {
        }
        while ((-i2) < iMax2) {
        }
        int iMax32 = Math.max(i6, sb2.length() + i2);
        if (iMin5 == 100) {
        }
        if (iMin6 == 100) {
        }
        while (i4 >= iMin) {
        }
        if (iMin9 != Math.min(100, -1)) {
        }
        int length32 = sb.length();
        if (positiveSuffixPattern != null) {
        }
        AffixUtils.escape(positiveSuffix, sb);
        if (iMin4 == -1) {
        }
        if (negativePrefix != null) {
            str = negativePrefixPattern;
            str2 = negativeSuffixPattern;
            sb.append(';');
            if (str != null) {
            }
            AffixUtils.escape(negativePrefix, sb);
            sb.append((CharSequence) sb, i5, length32);
            if (str2 != null) {
            }
            AffixUtils.escape(negativeSuffix, sb);
        }
        return sb.toString();
    }

    private static int escapePaddingString(CharSequence charSequence, StringBuilder sb, int i) {
        if (charSequence == null || charSequence.length() == 0) {
            charSequence = Padder.FALLBACK_PADDING_STRING;
        }
        int length = sb.length();
        int i2 = 1;
        if (charSequence.length() == 1) {
            if (charSequence.equals("'")) {
                sb.insert(i, "''");
            } else {
                sb.insert(i, charSequence);
            }
        } else {
            sb.insert(i, PatternTokenizer.SINGLE_QUOTE);
            for (int i3 = 0; i3 < charSequence.length(); i3++) {
                char cCharAt = charSequence.charAt(i3);
                if (cCharAt == '\'') {
                    sb.insert(i + i2, "''");
                    i2 += 2;
                } else {
                    sb.insert(i + i2, cCharAt);
                    i2++;
                }
            }
            sb.insert(i + i2, PatternTokenizer.SINGLE_QUOTE);
        }
        return sb.length() - length;
    }

    public static String convertLocalized(String str, DecimalFormatSymbols decimalFormatSymbols, boolean z) {
        if (str == null) {
            return null;
        }
        char c = 2;
        String[][] strArr = (String[][]) Array.newInstance((Class<?>) String.class, 21, 2);
        int i = !z ? 1 : 0;
        char c2 = 0;
        strArr[0][i] = "%";
        strArr[0][z ? 1 : 0] = decimalFormatSymbols.getPercentString();
        strArr[1][i] = "‰";
        strArr[1][z ? 1 : 0] = decimalFormatSymbols.getPerMillString();
        strArr[2][i] = ".";
        strArr[2][z ? 1 : 0] = decimalFormatSymbols.getDecimalSeparatorString();
        strArr[3][i] = ",";
        strArr[3][z ? 1 : 0] = decimalFormatSymbols.getGroupingSeparatorString();
        strArr[4][i] = LanguageTag.SEP;
        strArr[4][z ? 1 : 0] = decimalFormatSymbols.getMinusSignString();
        char c3 = 5;
        strArr[5][i] = "+";
        strArr[5][z ? 1 : 0] = decimalFormatSymbols.getPlusSignString();
        strArr[6][i] = ";";
        strArr[6][z ? 1 : 0] = Character.toString(decimalFormatSymbols.getPatternSeparator());
        strArr[7][i] = "@";
        strArr[7][z ? 1 : 0] = Character.toString(decimalFormatSymbols.getSignificantDigit());
        strArr[8][i] = DateFormat.ABBR_WEEKDAY;
        strArr[8][z ? 1 : 0] = decimalFormatSymbols.getExponentSeparator();
        strArr[9][i] = "*";
        strArr[9][z ? 1 : 0] = Character.toString(decimalFormatSymbols.getPadEscape());
        strArr[10][i] = "#";
        strArr[10][z ? 1 : 0] = Character.toString(decimalFormatSymbols.getDigit());
        for (int i2 = 0; i2 < 10; i2++) {
            int i3 = 11 + i2;
            strArr[i3][i] = Character.toString((char) (48 + i2));
            strArr[i3][z ? 1 : 0] = decimalFormatSymbols.getDigitStringsLocal()[i2];
        }
        for (int i4 = 0; i4 < strArr.length; i4++) {
            strArr[i4][z ? 1 : 0] = strArr[i4][z ? 1 : 0].replace(PatternTokenizer.SINGLE_QUOTE, (char) 8217);
        }
        StringBuilder sb = new StringBuilder();
        int length = 0;
        char c4 = 0;
        while (length < str.length()) {
            char cCharAt = str.charAt(length);
            if (cCharAt == '\'') {
                if (c4 == 0) {
                    sb.append(PatternTokenizer.SINGLE_QUOTE);
                } else if (c4 == 1) {
                    sb.append(PatternTokenizer.SINGLE_QUOTE);
                    c4 = 0;
                } else if (c4 == c) {
                    c4 = 3;
                } else if (c4 == 3) {
                    sb.append(PatternTokenizer.SINGLE_QUOTE);
                    sb.append(PatternTokenizer.SINGLE_QUOTE);
                } else if (c4 == 4) {
                    c4 = c3;
                } else {
                    sb.append(PatternTokenizer.SINGLE_QUOTE);
                    sb.append(PatternTokenizer.SINGLE_QUOTE);
                    c4 = 4;
                }
                c4 = 1;
            } else {
                if (c4 == 0 || c4 == 3 || c4 == 4) {
                    int length2 = strArr.length;
                    int i5 = 0;
                    while (true) {
                        if (i5 < length2) {
                            String[] strArr2 = strArr[i5];
                            if (str.regionMatches(length, strArr2[0], 0, strArr2[0].length())) {
                                length += strArr2[0].length() - 1;
                                if (c4 == 3 || c4 == 4) {
                                    sb.append(PatternTokenizer.SINGLE_QUOTE);
                                    c = 0;
                                } else {
                                    c = c4;
                                }
                                sb.append(strArr2[1]);
                            } else {
                                i5++;
                            }
                        } else {
                            int length3 = strArr.length;
                            int i6 = 0;
                            while (true) {
                                if (i6 < length3) {
                                    String[] strArr3 = strArr[i6];
                                    if (str.regionMatches(length, strArr3[1], 0, strArr3[1].length())) {
                                        if (c4 == 0) {
                                            sb.append(PatternTokenizer.SINGLE_QUOTE);
                                            c = 4;
                                        } else {
                                            c = c4;
                                        }
                                        sb.append(cCharAt);
                                    } else {
                                        i6++;
                                    }
                                } else {
                                    if (c4 == 3 || c4 == 4) {
                                        sb.append(PatternTokenizer.SINGLE_QUOTE);
                                        c = 0;
                                    } else {
                                        c = c4;
                                    }
                                    sb.append(cCharAt);
                                }
                            }
                        }
                    }
                } else {
                    sb.append(cCharAt);
                }
                c4 = c;
            }
            length++;
            c = 2;
            c3 = 5;
        }
        if (c4 == 3 || c4 == 4) {
            sb.append(PatternTokenizer.SINGLE_QUOTE);
        } else {
            c2 = c4;
        }
        if (c2 == 0) {
            return sb.toString();
        }
        throw new IllegalArgumentException("Malformed localized pattern: unterminated quote");
    }
}
