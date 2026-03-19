package android.icu.impl;

import android.icu.lang.UCharacter;
import android.icu.lang.UScript;
import android.icu.text.IDNA;
import android.icu.text.Normalizer2;
import android.icu.text.StringPrepParseException;
import android.icu.util.ICUException;
import java.util.EnumSet;

public final class UTS46 extends IDNA {
    final int options;
    private static final Normalizer2 uts46Norm2 = Normalizer2.getInstance(null, "uts46", Normalizer2.Mode.COMPOSE);
    private static final EnumSet<IDNA.Error> severeErrors = EnumSet.of(IDNA.Error.LEADING_COMBINING_MARK, IDNA.Error.DISALLOWED, IDNA.Error.PUNYCODE, IDNA.Error.LABEL_HAS_DOT, IDNA.Error.INVALID_ACE_LABEL);
    private static final byte[] asciiData = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1};
    private static final int L_MASK = U_MASK(0);
    private static final int R_AL_MASK = U_MASK(1) | U_MASK(13);
    private static final int L_R_AL_MASK = L_MASK | R_AL_MASK;
    private static final int R_AL_AN_MASK = R_AL_MASK | U_MASK(5);
    private static final int EN_AN_MASK = U_MASK(2) | U_MASK(5);
    private static final int R_AL_EN_AN_MASK = R_AL_MASK | EN_AN_MASK;
    private static final int L_EN_MASK = L_MASK | U_MASK(2);
    private static final int ES_CS_ET_ON_BN_NSM_MASK = ((((U_MASK(3) | U_MASK(6)) | U_MASK(4)) | U_MASK(10)) | U_MASK(18)) | U_MASK(17);
    private static final int L_EN_ES_CS_ET_ON_BN_NSM_MASK = L_EN_MASK | ES_CS_ET_ON_BN_NSM_MASK;
    private static final int R_AL_AN_EN_ES_CS_ET_ON_BN_NSM_MASK = (R_AL_MASK | EN_AN_MASK) | ES_CS_ET_ON_BN_NSM_MASK;
    private static int U_GC_M_MASK = (U_MASK(6) | U_MASK(7)) | U_MASK(8);

    public UTS46(int options) {
        this.options = options;
    }

    @Override
    public StringBuilder labelToASCII(CharSequence label, StringBuilder dest, IDNA.Info info) {
        return process(label, true, true, dest, info);
    }

    @Override
    public StringBuilder labelToUnicode(CharSequence label, StringBuilder dest, IDNA.Info info) {
        return process(label, true, false, dest, info);
    }

    @Override
    public StringBuilder nameToASCII(CharSequence name, StringBuilder dest, IDNA.Info info) {
        process(name, false, true, dest, info);
        if (dest.length() >= 254 && !info.getErrors().contains(IDNA.Error.DOMAIN_NAME_TOO_LONG) && isASCIIString(dest) && (dest.length() > 254 || dest.charAt(253) != '.')) {
            addError(info, IDNA.Error.DOMAIN_NAME_TOO_LONG);
        }
        return dest;
    }

    @Override
    public StringBuilder nameToUnicode(CharSequence name, StringBuilder dest, IDNA.Info info) {
        return process(name, false, false, dest, info);
    }

    private static boolean isASCIIString(CharSequence dest) {
        int length = dest.length();
        for (int i = 0; i < length; i++) {
            if (dest.charAt(i) > 127) {
                return false;
            }
        }
        return true;
    }

    private StringBuilder process(CharSequence src, boolean isLabel, boolean toASCII, StringBuilder dest, IDNA.Info info) {
        if (dest == src) {
            throw new IllegalArgumentException();
        }
        dest.delete(0, Integer.MAX_VALUE);
        resetInfo(info);
        int srcLength = src.length();
        if (srcLength == 0) {
            addError(info, IDNA.Error.EMPTY_LABEL);
            return dest;
        }
        boolean disallowNonLDHDot = (this.options & 2) != 0;
        int labelStart = 0;
        int i = 0;
        while (i != srcLength) {
            char c = src.charAt(i);
            if (c <= 127) {
                int cData = asciiData[c];
                if (cData > 0) {
                    dest.append((char) (c + ' '));
                } else if (cData >= 0 || !disallowNonLDHDot) {
                    dest.append(c);
                    if (c == '-') {
                        if (i == labelStart + 3 && src.charAt(i - 1) == '-') {
                            i++;
                        } else {
                            if (i == labelStart) {
                                addLabelError(info, IDNA.Error.LEADING_HYPHEN);
                            }
                            if (i + 1 == srcLength || src.charAt(i + 1) == '.') {
                                addLabelError(info, IDNA.Error.TRAILING_HYPHEN);
                            }
                        }
                    } else if (c != '.') {
                        continue;
                    } else if (isLabel) {
                        i++;
                    } else {
                        if (i == labelStart) {
                            addLabelError(info, IDNA.Error.EMPTY_LABEL);
                        }
                        if (toASCII && i - labelStart > 63) {
                            addLabelError(info, IDNA.Error.LABEL_TOO_LONG);
                        }
                        promoteAndResetLabelErrors(info);
                        labelStart = i + 1;
                    }
                }
                i++;
            }
            promoteAndResetLabelErrors(info);
            processUnicode(src, labelStart, i, isLabel, toASCII, dest, info);
            if (isBiDi(info) && !hasCertainErrors(info, severeErrors) && (!isOkBiDi(info) || (labelStart > 0 && !isASCIIOkBiDi(dest, labelStart)))) {
                addError(info, IDNA.Error.BIDI);
            }
            return dest;
        }
        if (toASCII) {
            if (i - labelStart > 63) {
                addLabelError(info, IDNA.Error.LABEL_TOO_LONG);
            }
            if (!isLabel && i >= 254 && (i > 254 || labelStart < i)) {
                addError(info, IDNA.Error.DOMAIN_NAME_TOO_LONG);
            }
        }
        promoteAndResetLabelErrors(info);
        return dest;
    }

    private StringBuilder processUnicode(CharSequence src, int labelStart, int mappingStart, boolean isLabel, boolean toASCII, StringBuilder dest, IDNA.Info info) {
        boolean doMapDevChars;
        if (mappingStart == 0) {
            uts46Norm2.normalize(src, dest);
        } else {
            uts46Norm2.normalizeSecondAndAppend(dest, src.subSequence(mappingStart, src.length()));
        }
        if (toASCII) {
            doMapDevChars = (this.options & 16) == 0;
        } else {
            doMapDevChars = (this.options & 32) == 0;
        }
        int destLength = dest.length();
        int labelLimit = labelStart;
        while (labelLimit < destLength) {
            char c = dest.charAt(labelLimit);
            if (c == '.' && !isLabel) {
                int labelLength = labelLimit - labelStart;
                int newLength = processLabel(dest, labelStart, labelLength, toASCII, info);
                promoteAndResetLabelErrors(info);
                destLength += newLength - labelLength;
                labelStart += newLength + 1;
                labelLimit = labelStart;
            } else if (223 <= c && c <= 8205 && (c == 223 || c == 962 || c >= 8204)) {
                setTransitionalDifferent(info);
                if (doMapDevChars) {
                    destLength = mapDevChars(dest, labelStart, labelLimit);
                    doMapDevChars = false;
                } else {
                    labelLimit++;
                }
            } else {
                labelLimit++;
            }
        }
        if (labelStart == 0 || labelStart < labelLimit) {
            processLabel(dest, labelStart, labelLimit - labelStart, toASCII, info);
            promoteAndResetLabelErrors(info);
        }
        return dest;
    }

    private int mapDevChars(StringBuilder dest, int labelStart, int mappingStart) {
        int i;
        int length = dest.length();
        boolean didMapDevChars = false;
        int i2 = mappingStart;
        while (i2 < length) {
            char c = dest.charAt(i2);
            switch (c) {
                case 223:
                    didMapDevChars = true;
                    int i3 = i2 + 1;
                    dest.setCharAt(i2, 's');
                    dest.insert(i3, 's');
                    length++;
                    i = i3 + 1;
                    break;
                case 962:
                    didMapDevChars = true;
                    i = i2 + 1;
                    dest.setCharAt(i2, (char) 963);
                    break;
                case 8204:
                case 8205:
                    didMapDevChars = true;
                    dest.delete(i2, i2 + 1);
                    length--;
                    i = i2;
                    break;
                default:
                    i = i2 + 1;
                    break;
            }
            i2 = i;
        }
        if (didMapDevChars) {
            String normalized = uts46Norm2.normalize(dest.subSequence(labelStart, dest.length()));
            dest.replace(labelStart, Integer.MAX_VALUE, normalized);
            return dest.length();
        }
        return length;
    }

    private static boolean isNonASCIIDisallowedSTD3Valid(int c) {
        return c == 8800 || c == 8814 || c == 8815;
    }

    private static int replaceLabel(StringBuilder dest, int destLabelStart, int destLabelLength, CharSequence label, int labelLength) {
        if (label != dest) {
            dest.delete(destLabelStart, destLabelStart + destLabelLength).insert(destLabelStart, label);
        }
        return labelLength;
    }

    private int processLabel(StringBuilder dest, int labelStart, int labelLength, boolean toASCII, IDNA.Info info) {
        boolean wasPunycode;
        StringBuilder labelString;
        int destLabelLength = labelLength;
        if (labelLength >= 4 && dest.charAt(labelStart) == 'x' && dest.charAt(labelStart + 1) == 'n' && dest.charAt(labelStart + 2) == '-' && dest.charAt(labelStart + 3) == '-') {
            wasPunycode = true;
            try {
                StringBuilder fromPunycode = Punycode.decode(dest.subSequence(labelStart + 4, labelStart + labelLength), null);
                boolean isValid = uts46Norm2.isNormalized(fromPunycode);
                if (!isValid) {
                    addLabelError(info, IDNA.Error.INVALID_ACE_LABEL);
                    return markBadACELabel(dest, labelStart, labelLength, toASCII, info);
                }
                labelString = fromPunycode;
                labelStart = 0;
                labelLength = fromPunycode.length();
            } catch (StringPrepParseException e) {
                addLabelError(info, IDNA.Error.PUNYCODE);
                return markBadACELabel(dest, labelStart, labelLength, toASCII, info);
            }
        } else {
            wasPunycode = false;
            labelString = dest;
        }
        if (labelLength == 0) {
            addLabelError(info, IDNA.Error.EMPTY_LABEL);
            return replaceLabel(dest, labelStart, destLabelLength, labelString, labelLength);
        }
        if (labelLength >= 4) {
            if (labelString.charAt(labelStart + 2) == '-') {
                if (labelString.charAt(labelStart + 3) == '-') {
                    addLabelError(info, IDNA.Error.HYPHEN_3_4);
                }
            }
        }
        if (labelString.charAt(labelStart) == '-') {
            addLabelError(info, IDNA.Error.LEADING_HYPHEN);
        }
        if (labelString.charAt((labelStart + labelLength) - 1) == '-') {
            addLabelError(info, IDNA.Error.TRAILING_HYPHEN);
        }
        int i = labelStart;
        int limit = labelStart + labelLength;
        char oredChars = 0;
        boolean disallowNonLDHDot = (this.options & 2) != 0;
        do {
            char c = labelString.charAt(i);
            if (c <= 127) {
                if (c == '.') {
                    addLabelError(info, IDNA.Error.LABEL_HAS_DOT);
                    labelString.setCharAt(i, (char) 65533);
                } else if (disallowNonLDHDot && asciiData[c] < 0) {
                    addLabelError(info, IDNA.Error.DISALLOWED);
                    labelString.setCharAt(i, (char) 65533);
                }
            } else {
                oredChars = (char) (oredChars | c);
                if (disallowNonLDHDot && isNonASCIIDisallowedSTD3Valid(c)) {
                    addLabelError(info, IDNA.Error.DISALLOWED);
                    labelString.setCharAt(i, (char) 65533);
                } else if (c == 65533) {
                    addLabelError(info, IDNA.Error.DISALLOWED);
                }
            }
            i++;
        } while (i < limit);
        int c2 = labelString.codePointAt(labelStart);
        if ((U_GET_GC_MASK(c2) & U_GC_M_MASK) != 0) {
            addLabelError(info, IDNA.Error.LEADING_COMBINING_MARK);
            labelString.setCharAt(labelStart, (char) 65533);
            if (c2 > 65535) {
                labelString.deleteCharAt(labelStart + 1);
                labelLength--;
                if (labelString == dest) {
                    destLabelLength--;
                }
            }
        }
        if (!hasCertainLabelErrors(info, severeErrors)) {
            if ((this.options & 4) != 0 && (!isBiDi(info) || isOkBiDi(info))) {
                checkLabelBiDi(labelString, labelStart, labelLength, info);
            }
            if ((this.options & 8) != 0 && (oredChars & 8204) == 8204 && !isLabelOkContextJ(labelString, labelStart, labelLength)) {
                addLabelError(info, IDNA.Error.CONTEXTJ);
            }
            if ((this.options & 64) != 0 && oredChars >= 183) {
                checkLabelContextO(labelString, labelStart, labelLength, info);
            }
            if (toASCII) {
                if (wasPunycode) {
                    if (destLabelLength > 63) {
                        addLabelError(info, IDNA.Error.LABEL_TOO_LONG);
                    }
                    return destLabelLength;
                }
                if (oredChars >= 128) {
                    try {
                        StringBuilder punycode = Punycode.encode(labelString.subSequence(labelStart, labelStart + labelLength), null);
                        punycode.insert(0, "xn--");
                        if (punycode.length() > 63) {
                            addLabelError(info, IDNA.Error.LABEL_TOO_LONG);
                        }
                        return replaceLabel(dest, labelStart, destLabelLength, punycode, punycode.length());
                    } catch (StringPrepParseException e2) {
                        throw new ICUException(e2);
                    }
                }
                if (labelLength > 63) {
                    addLabelError(info, IDNA.Error.LABEL_TOO_LONG);
                }
            }
        } else if (wasPunycode) {
            addLabelError(info, IDNA.Error.INVALID_ACE_LABEL);
            return markBadACELabel(dest, labelStart, destLabelLength, toASCII, info);
        }
        return replaceLabel(dest, labelStart, destLabelLength, labelString, labelLength);
    }

    private int markBadACELabel(StringBuilder dest, int labelStart, int labelLength, boolean toASCII, IDNA.Info info) {
        boolean disallowNonLDHDot = (this.options & 2) != 0;
        boolean isASCII = true;
        boolean onlyLDH = true;
        int i = labelStart + 4;
        int limit = labelStart + labelLength;
        do {
            char c = dest.charAt(i);
            if (c <= 127) {
                if (c == '.') {
                    addLabelError(info, IDNA.Error.LABEL_HAS_DOT);
                    dest.setCharAt(i, (char) 65533);
                    onlyLDH = false;
                    isASCII = false;
                } else if (asciiData[c] < 0) {
                    onlyLDH = false;
                    if (disallowNonLDHDot) {
                        dest.setCharAt(i, (char) 65533);
                        isASCII = false;
                    }
                }
            } else {
                onlyLDH = false;
                isASCII = false;
            }
            i++;
        } while (i < limit);
        if (onlyLDH) {
            dest.insert(labelStart + labelLength, (char) 65533);
            return labelLength + 1;
        }
        if (toASCII && isASCII && labelLength > 63) {
            addLabelError(info, IDNA.Error.LABEL_TOO_LONG);
            return labelLength;
        }
        return labelLength;
    }

    private void checkLabelBiDi(CharSequence label, int labelStart, int labelLength, IDNA.Info info) {
        int lastMask;
        int c = Character.codePointAt(label, labelStart);
        int i = labelStart + Character.charCount(c);
        int firstMask = U_MASK(UBiDiProps.INSTANCE.getClass(c));
        if (((~L_R_AL_MASK) & firstMask) != 0) {
            setNotOkBiDi(info);
        }
        int labelLimit = labelStart + labelLength;
        while (true) {
            if (i >= labelLimit) {
                lastMask = firstMask;
                break;
            }
            int c2 = Character.codePointBefore(label, labelLimit);
            labelLimit -= Character.charCount(c2);
            int dir = UBiDiProps.INSTANCE.getClass(c2);
            if (dir != 17) {
                lastMask = U_MASK(dir);
                break;
            }
        }
        if ((L_MASK & firstMask) == 0 ? ((~R_AL_EN_AN_MASK) & lastMask) != 0 : ((~L_EN_MASK) & lastMask) != 0) {
            setNotOkBiDi(info);
        }
        int mask = 0;
        while (i < labelLimit) {
            int c3 = Character.codePointAt(label, i);
            i += Character.charCount(c3);
            mask |= U_MASK(UBiDiProps.INSTANCE.getClass(c3));
        }
        if ((L_MASK & firstMask) != 0) {
            if (((~L_EN_ES_CS_ET_ON_BN_NSM_MASK) & mask) != 0) {
                setNotOkBiDi(info);
            }
        } else {
            if (((~R_AL_AN_EN_ES_CS_ET_ON_BN_NSM_MASK) & mask) != 0) {
                setNotOkBiDi(info);
            }
            if ((EN_AN_MASK & mask) == EN_AN_MASK) {
                setNotOkBiDi(info);
            }
        }
        if (((firstMask | mask | lastMask) & R_AL_AN_MASK) == 0) {
            return;
        }
        setBiDi(info);
    }

    private static boolean isASCIIOkBiDi(CharSequence s, int length) {
        char c;
        int labelStart = 0;
        for (int i = 0; i < length; i++) {
            char c2 = s.charAt(i);
            if (c2 == '.') {
                if (i > labelStart && (('a' > (c = s.charAt(i - 1)) || c > 'z') && ('0' > c || c > '9'))) {
                    return false;
                }
                labelStart = i + 1;
            } else if (i == labelStart) {
                if ('a' > c2 || c2 > 'z') {
                    return false;
                }
            } else if (c2 <= ' ' && (c2 >= 28 || ('\t' <= c2 && c2 <= '\r'))) {
                return false;
            }
        }
        return true;
    }

    private boolean isLabelOkContextJ(CharSequence label, int labelStart, int labelLength) {
        int labelLimit = labelStart + labelLength;
        for (int i = labelStart; i < labelLimit; i++) {
            if (label.charAt(i) == 8204) {
                if (i == labelStart) {
                    return false;
                }
                int j = i;
                int c = Character.codePointBefore(label, i);
                int j2 = j - Character.charCount(c);
                if (uts46Norm2.getCombiningClass(c) == 9) {
                    continue;
                } else {
                    while (true) {
                        int type = UBiDiProps.INSTANCE.getJoiningType(c);
                        if (type == 5) {
                            if (j2 == 0) {
                                return false;
                            }
                            c = Character.codePointBefore(label, j2);
                            j2 -= Character.charCount(c);
                        } else {
                            if (type != 3 && type != 2) {
                                return false;
                            }
                            int j3 = i + 1;
                            while (j3 != labelLimit) {
                                int c2 = Character.codePointAt(label, j3);
                                j3 += Character.charCount(c2);
                                int type2 = UBiDiProps.INSTANCE.getJoiningType(c2);
                                if (type2 != 5) {
                                    if (type2 != 4 && type2 != 2) {
                                        return false;
                                    }
                                }
                            }
                            return false;
                        }
                    }
                }
            } else if (label.charAt(i) != 8205) {
                continue;
            } else {
                if (i == labelStart) {
                    return false;
                }
                if (uts46Norm2.getCombiningClass(Character.codePointBefore(label, i)) != 9) {
                    return false;
                }
            }
        }
        return true;
    }

    private void checkLabelContextO(CharSequence label, int labelStart, int labelLength, IDNA.Info info) {
        int labelEnd = (labelStart + labelLength) - 1;
        int arabicDigits = 0;
        for (int i = labelStart; i <= labelEnd; i++) {
            int c = label.charAt(i);
            if (c >= 183) {
                if (c <= 1785) {
                    if (c == 183) {
                        if (labelStart >= i || label.charAt(i - 1) != 'l' || i >= labelEnd || label.charAt(i + 1) != 'l') {
                            addLabelError(info, IDNA.Error.CONTEXTO_PUNCTUATION);
                        }
                    } else if (c == 885) {
                        if (i >= labelEnd || 14 != UScript.getScript(Character.codePointAt(label, i + 1))) {
                            addLabelError(info, IDNA.Error.CONTEXTO_PUNCTUATION);
                        }
                    } else if (c == 1523 || c == 1524) {
                        if (labelStart >= i || 19 != UScript.getScript(Character.codePointBefore(label, i))) {
                            addLabelError(info, IDNA.Error.CONTEXTO_PUNCTUATION);
                        }
                    } else if (1632 <= c) {
                        if (c <= 1641) {
                            if (arabicDigits > 0) {
                                addLabelError(info, IDNA.Error.CONTEXTO_DIGITS);
                            }
                            arabicDigits = -1;
                        } else if (1776 <= c) {
                            if (arabicDigits < 0) {
                                addLabelError(info, IDNA.Error.CONTEXTO_DIGITS);
                            }
                            arabicDigits = 1;
                        }
                    }
                } else if (c == 12539) {
                    int j = labelStart;
                    while (true) {
                        if (j > labelEnd) {
                            addLabelError(info, IDNA.Error.CONTEXTO_PUNCTUATION);
                            break;
                        }
                        int c2 = Character.codePointAt(label, j);
                        int script = UScript.getScript(c2);
                        if (script == 20 || script == 22 || script == 17) {
                            break;
                        } else {
                            j += Character.charCount(c2);
                        }
                    }
                }
            }
        }
    }

    private static int U_MASK(int x) {
        return 1 << x;
    }

    private static int U_GET_GC_MASK(int c) {
        return 1 << UCharacter.getType(c);
    }
}
