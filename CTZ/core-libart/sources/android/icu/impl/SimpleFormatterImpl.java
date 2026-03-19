package android.icu.impl;

import android.icu.text.PluralRules;

public final class SimpleFormatterImpl {
    static final boolean $assertionsDisabled = false;
    private static final int ARG_NUM_LIMIT = 256;
    private static final String[][] COMMON_PATTERNS = {new String[]{"{0} {1}", "\u0002\u0000ā \u0001"}, new String[]{"{0} ({1})", "\u0002\u0000Ă (\u0001ā)"}, new String[]{"{0}, {1}", "\u0002\u0000Ă, \u0001"}, new String[]{"{0} – {1}", "\u0002\u0000ă – \u0001"}};
    private static final char LEN1_CHAR = 257;
    private static final char LEN2_CHAR = 258;
    private static final char LEN3_CHAR = 259;
    private static final int MAX_SEGMENT_LENGTH = 65279;
    private static final char SEGMENT_LENGTH_ARGUMENT_CHAR = 65535;

    private SimpleFormatterImpl() {
    }

    public static String compileToStringMinMaxArguments(CharSequence charSequence, StringBuilder sb, int i, int i2) {
        int i3;
        int i4;
        int iCharAt;
        int i5 = 0;
        int i6 = 1;
        if (i <= 2 && 2 <= i2) {
            for (String[] strArr : COMMON_PATTERNS) {
                if (strArr[0].contentEquals(charSequence)) {
                    return strArr[1];
                }
            }
        }
        int length = charSequence.length();
        sb.ensureCapacity(length);
        sb.setLength(1);
        int i7 = 0;
        int i8 = 0;
        int i9 = 0;
        int i10 = -1;
        while (i7 < length) {
            int i11 = i7 + 1;
            char cCharAt = charSequence.charAt(i7);
            if (cCharAt == '\'') {
                if (i11 < length && (cCharAt = charSequence.charAt(i11)) == '\'') {
                    i11++;
                } else if (i9 != 0) {
                    i9 = i5;
                    i7 = i11;
                } else if (cCharAt == '{' || cCharAt == '}') {
                    i11++;
                    i9 = i6;
                } else {
                    cCharAt = '\'';
                }
            } else {
                if (i9 == 0 && cCharAt == '{') {
                    if (i8 > 0) {
                        sb.setCharAt((sb.length() - i8) - i6, (char) (i8 + 256));
                        i8 = i5;
                    }
                    int i12 = i11 + 1;
                    if (i12 < length && charSequence.charAt(i11) - '0' >= 0 && iCharAt <= 9 && charSequence.charAt(i12) == '}') {
                        i11 += 2;
                    } else {
                        int i13 = i11 - 1;
                        if (i11 < length) {
                            cCharAt = charSequence.charAt(i11);
                            if ('1' <= cCharAt && cCharAt <= '9') {
                                i11 = i12;
                                i4 = cCharAt - 48;
                                while (i11 < length) {
                                    int i14 = i11 + 1;
                                    char cCharAt2 = charSequence.charAt(i11);
                                    if ('0' <= cCharAt2 && cCharAt2 <= '9' && (i4 = (i4 * 10) + (cCharAt2 - 48)) < 256) {
                                        i11 = i14;
                                        cCharAt = cCharAt2;
                                    } else {
                                        i11 = i14;
                                        cCharAt = cCharAt2;
                                        break;
                                    }
                                }
                                if (i4 < 0) {
                                }
                                throw new IllegalArgumentException("Argument syntax error in pattern \"" + ((Object) charSequence) + "\" at index " + i13 + PluralRules.KEYWORD_RULE_SEPARATOR + ((Object) charSequence.subSequence(i13, i11)));
                            }
                            i11 = i12;
                            i4 = -1;
                            if (i4 < 0) {
                            }
                            throw new IllegalArgumentException("Argument syntax error in pattern \"" + ((Object) charSequence) + "\" at index " + i13 + PluralRules.KEYWORD_RULE_SEPARATOR + ((Object) charSequence.subSequence(i13, i11)));
                        }
                        i4 = -1;
                        if (i4 < 0 || cCharAt != '}') {
                            throw new IllegalArgumentException("Argument syntax error in pattern \"" + ((Object) charSequence) + "\" at index " + i13 + PluralRules.KEYWORD_RULE_SEPARATOR + ((Object) charSequence.subSequence(i13, i11)));
                        }
                        iCharAt = i4;
                    }
                    i7 = i11;
                    if (iCharAt > i10) {
                        i10 = iCharAt;
                    }
                    sb.append((char) iCharAt);
                }
                i5 = 0;
                i6 = 1;
            }
            if (i8 == 0) {
                sb.append((char) 65535);
            }
            sb.append(cCharAt);
            i8++;
            if (i8 == MAX_SEGMENT_LENGTH) {
                i8 = 0;
            }
            i7 = i11;
            i5 = 0;
            i6 = 1;
        }
        if (i8 > 0) {
            i3 = 1;
            sb.setCharAt((sb.length() - i8) - 1, (char) (256 + i8));
        } else {
            i3 = 1;
        }
        int i15 = i10 + i3;
        if (i15 < i) {
            throw new IllegalArgumentException("Fewer than minimum " + i + " arguments in pattern \"" + ((Object) charSequence) + "\"");
        }
        if (i15 > i2) {
            throw new IllegalArgumentException("More than maximum " + i2 + " arguments in pattern \"" + ((Object) charSequence) + "\"");
        }
        sb.setCharAt(0, (char) i15);
        return sb.toString();
    }

    public static int getArgumentLimit(String str) {
        return str.charAt(0);
    }

    public static String formatCompiledPattern(String str, CharSequence... charSequenceArr) {
        return formatAndAppend(str, new StringBuilder(), null, charSequenceArr).toString();
    }

    public static String formatRawPattern(String str, int i, int i2, CharSequence... charSequenceArr) {
        StringBuilder sb = new StringBuilder();
        String strCompileToStringMinMaxArguments = compileToStringMinMaxArguments(str, sb, i, i2);
        sb.setLength(0);
        return formatAndAppend(strCompileToStringMinMaxArguments, sb, null, charSequenceArr).toString();
    }

    public static StringBuilder formatAndAppend(String str, StringBuilder sb, int[] iArr, CharSequence... charSequenceArr) {
        if ((charSequenceArr != null ? charSequenceArr.length : 0) < getArgumentLimit(str)) {
            throw new IllegalArgumentException("Too few values.");
        }
        return format(str, charSequenceArr, sb, null, true, iArr);
    }

    public static StringBuilder formatAndReplace(String str, StringBuilder sb, int[] iArr, CharSequence... charSequenceArr) {
        if ((charSequenceArr != null ? charSequenceArr.length : 0) < getArgumentLimit(str)) {
            throw new IllegalArgumentException("Too few values.");
        }
        int i = -1;
        String string = null;
        if (getArgumentLimit(str) > 0) {
            int i2 = 1;
            while (i2 < str.length()) {
                int i3 = i2 + 1;
                char cCharAt = str.charAt(i2);
                if (cCharAt < 256) {
                    if (charSequenceArr[cCharAt] == sb) {
                        if (i3 != 2) {
                            if (string == null) {
                                string = sb.toString();
                            }
                        } else {
                            i = cCharAt;
                        }
                    }
                } else {
                    i3 += cCharAt - 256;
                }
                i2 = i3;
            }
        }
        String str2 = string;
        if (i < 0) {
            sb.setLength(0);
        }
        return format(str, charSequenceArr, sb, str2, false, iArr);
    }

    public static String getTextWithNoArguments(String str) {
        int i = 1;
        StringBuilder sb = new StringBuilder((str.length() - 1) - getArgumentLimit(str));
        while (i < str.length()) {
            int i2 = i + 1;
            int iCharAt = str.charAt(i) - 256;
            if (iCharAt > 0) {
                i = iCharAt + i2;
                sb.append((CharSequence) str, i2, i);
            } else {
                i = i2;
            }
        }
        return sb.toString();
    }

    private static StringBuilder format(String str, CharSequence[] charSequenceArr, StringBuilder sb, String str2, boolean z, int[] iArr) {
        int length;
        if (iArr != null) {
            length = iArr.length;
            for (int i = 0; i < length; i++) {
                iArr[i] = -1;
            }
        } else {
            length = 0;
        }
        int i2 = 1;
        while (i2 < str.length()) {
            int i3 = i2 + 1;
            char cCharAt = str.charAt(i2);
            if (cCharAt < 256) {
                CharSequence charSequence = charSequenceArr[cCharAt];
                if (charSequence == sb) {
                    if (z) {
                        throw new IllegalArgumentException("Value must not be same object as result");
                    }
                    if (i3 == 2) {
                        if (cCharAt < length) {
                            iArr[cCharAt] = 0;
                        }
                    } else {
                        if (cCharAt < length) {
                            iArr[cCharAt] = sb.length();
                        }
                        sb.append(str2);
                    }
                } else {
                    if (cCharAt < length) {
                        iArr[cCharAt] = sb.length();
                    }
                    sb.append(charSequence);
                }
                i2 = i3;
            } else {
                i2 = (cCharAt - 256) + i3;
                sb.append((CharSequence) str, i3, i2);
            }
        }
        return sb;
    }
}
