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
    private static final int EN_AN_MASK = U_MASK(5) | U_MASK(2);
    private static final int R_AL_EN_AN_MASK = R_AL_MASK | EN_AN_MASK;
    private static final int L_EN_MASK = U_MASK(2) | L_MASK;
    private static final int ES_CS_ET_ON_BN_NSM_MASK = ((((U_MASK(3) | U_MASK(6)) | U_MASK(4)) | U_MASK(10)) | U_MASK(18)) | U_MASK(17);
    private static final int L_EN_ES_CS_ET_ON_BN_NSM_MASK = L_EN_MASK | ES_CS_ET_ON_BN_NSM_MASK;
    private static final int R_AL_AN_EN_ES_CS_ET_ON_BN_NSM_MASK = (R_AL_MASK | EN_AN_MASK) | ES_CS_ET_ON_BN_NSM_MASK;
    private static int U_GC_M_MASK = (U_MASK(6) | U_MASK(7)) | U_MASK(8);

    public UTS46(int i) {
        this.options = i;
    }

    @Override
    public StringBuilder labelToASCII(CharSequence charSequence, StringBuilder sb, IDNA.Info info) {
        return process(charSequence, true, true, sb, info);
    }

    @Override
    public StringBuilder labelToUnicode(CharSequence charSequence, StringBuilder sb, IDNA.Info info) {
        return process(charSequence, true, false, sb, info);
    }

    @Override
    public StringBuilder nameToASCII(CharSequence charSequence, StringBuilder sb, IDNA.Info info) {
        process(charSequence, false, true, sb, info);
        if (sb.length() >= 254 && !info.getErrors().contains(IDNA.Error.DOMAIN_NAME_TOO_LONG) && isASCIIString(sb) && (sb.length() > 254 || sb.charAt(253) != '.')) {
            addError(info, IDNA.Error.DOMAIN_NAME_TOO_LONG);
        }
        return sb;
    }

    @Override
    public StringBuilder nameToUnicode(CharSequence charSequence, StringBuilder sb, IDNA.Info info) {
        return process(charSequence, false, false, sb, info);
    }

    private static boolean isASCIIString(CharSequence charSequence) {
        int length = charSequence.length();
        for (int i = 0; i < length; i++) {
            if (charSequence.charAt(i) > 127) {
                return false;
            }
        }
        return true;
    }

    private StringBuilder process(CharSequence charSequence, boolean z, boolean z2, StringBuilder sb, IDNA.Info info) {
        if (sb == charSequence) {
            throw new IllegalArgumentException();
        }
        int i = 0;
        sb.delete(0, Integer.MAX_VALUE);
        resetInfo(info);
        int length = charSequence.length();
        if (length == 0) {
            addError(info, IDNA.Error.EMPTY_LABEL);
            return sb;
        }
        boolean z3 = (this.options & 2) != 0;
        int i2 = 0;
        while (i != length) {
            char cCharAt = charSequence.charAt(i);
            if (cCharAt <= 127) {
                byte b = asciiData[cCharAt];
                if (b > 0) {
                    sb.append((char) (cCharAt + ' '));
                } else if (b >= 0 || !z3) {
                    sb.append(cCharAt);
                    if (cCharAt == '-') {
                        if (i == i2 + 3 && charSequence.charAt(i - 1) == '-') {
                            i++;
                        } else {
                            if (i == i2) {
                                addLabelError(info, IDNA.Error.LEADING_HYPHEN);
                            }
                            int i3 = i + 1;
                            if (i3 == length || charSequence.charAt(i3) == '.') {
                                addLabelError(info, IDNA.Error.TRAILING_HYPHEN);
                            }
                        }
                    } else if (cCharAt != '.') {
                        continue;
                    } else if (z) {
                        i++;
                    } else {
                        if (i == i2) {
                            addLabelError(info, IDNA.Error.EMPTY_LABEL);
                        }
                        if (z2 && i - i2 > 63) {
                            addLabelError(info, IDNA.Error.LABEL_TOO_LONG);
                        }
                        promoteAndResetLabelErrors(info);
                        i2 = i + 1;
                    }
                }
                i++;
            }
            int i4 = i;
            promoteAndResetLabelErrors(info);
            processUnicode(charSequence, i2, i4, z, z2, sb, info);
            if (isBiDi(info) && !hasCertainErrors(info, severeErrors) && (!isOkBiDi(info) || (i2 > 0 && !isASCIIOkBiDi(sb, i2)))) {
                addError(info, IDNA.Error.BIDI);
            }
            return sb;
        }
        if (z2) {
            if (i - i2 > 63) {
                addLabelError(info, IDNA.Error.LABEL_TOO_LONG);
            }
            if (!z && i >= 254 && (i > 254 || i2 < i)) {
                addError(info, IDNA.Error.DOMAIN_NAME_TOO_LONG);
            }
        }
        promoteAndResetLabelErrors(info);
        return sb;
    }

    private StringBuilder processUnicode(CharSequence charSequence, int i, int i2, boolean z, boolean z2, StringBuilder sb, IDNA.Info info) {
        if (i2 == 0) {
            uts46Norm2.normalize(charSequence, sb);
        } else {
            uts46Norm2.normalizeSecondAndAppend(sb, charSequence.subSequence(i2, charSequence.length()));
        }
        int i3 = i;
        boolean z3 = !z2 ? (this.options & 32) != 0 : (this.options & 16) != 0;
        int length = sb.length();
        int i4 = i3;
        while (i4 < length) {
            char cCharAt = sb.charAt(i4);
            if (cCharAt == '.' && !z) {
                int i5 = i4 - i3;
                int iProcessLabel = processLabel(sb, i3, i5, z2, info);
                promoteAndResetLabelErrors(info);
                length += iProcessLabel - i5;
                i3 += iProcessLabel + 1;
                i4 = i3;
            } else if (223 <= cCharAt && cCharAt <= 8205 && (cCharAt == 223 || cCharAt == 962 || cCharAt >= 8204)) {
                setTransitionalDifferent(info);
                if (z3) {
                    length = mapDevChars(sb, i3, i4);
                    z3 = false;
                } else {
                    i4++;
                }
            } else {
                i4++;
            }
        }
        if (i3 == 0 || i3 < i4) {
            processLabel(sb, i3, i4 - i3, z2, info);
            promoteAndResetLabelErrors(info);
        }
        return sb;
    }

    private int mapDevChars(StringBuilder sb, int i, int i2) {
        int length = sb.length();
        boolean z = false;
        while (i2 < length) {
            char cCharAt = sb.charAt(i2);
            if (cCharAt == 223) {
                int i3 = i2 + 1;
                sb.setCharAt(i2, 's');
                i2 = i3 + 1;
                sb.insert(i3, 's');
                length++;
            } else if (cCharAt == 962) {
                sb.setCharAt(i2, (char) 963);
                i2++;
            } else {
                switch (cCharAt) {
                    case 8204:
                    case 8205:
                        sb.delete(i2, i2 + 1);
                        length--;
                        break;
                    default:
                        i2++;
                        continue;
                }
            }
            z = true;
        }
        if (z) {
            sb.replace(i, Integer.MAX_VALUE, uts46Norm2.normalize(sb.subSequence(i, sb.length())));
            return sb.length();
        }
        return length;
    }

    private static boolean isNonASCIIDisallowedSTD3Valid(int i) {
        return i == 8800 || i == 8814 || i == 8815;
    }

    private static int replaceLabel(StringBuilder sb, int i, int i2, CharSequence charSequence, int i3) {
        if (charSequence != sb) {
            sb.delete(i, i2 + i).insert(i, charSequence);
        }
        return i3;
    }

    private int processLabel(StringBuilder sb, int i, int i2, boolean z, IDNA.Info info) {
        StringBuilder sbDecode;
        int i3;
        int length;
        boolean z2;
        int i4 = i2;
        if (i4 >= 4 && sb.charAt(i) == 'x' && sb.charAt(i + 1) == 'n' && sb.charAt(i + 2) == '-' && sb.charAt(i + 3) == '-') {
            try {
                sbDecode = Punycode.decode(sb.subSequence(i + 4, i + i4), null);
                if (!uts46Norm2.isNormalized(sbDecode)) {
                    addLabelError(info, IDNA.Error.INVALID_ACE_LABEL);
                    return markBadACELabel(sb, i, i2, z, info);
                }
                length = sbDecode.length();
                z2 = true;
                i3 = 0;
            } catch (StringPrepParseException e) {
                addLabelError(info, IDNA.Error.PUNYCODE);
                return markBadACELabel(sb, i, i2, z, info);
            }
        } else {
            sbDecode = sb;
            i3 = i;
            length = i4;
            z2 = false;
        }
        if (length == 0) {
            addLabelError(info, IDNA.Error.EMPTY_LABEL);
            return replaceLabel(sb, i, i4, sbDecode, length);
        }
        if (length >= 4 && sbDecode.charAt(i3 + 2) == '-' && sbDecode.charAt(i3 + 3) == '-') {
            addLabelError(info, IDNA.Error.HYPHEN_3_4);
        }
        if (sbDecode.charAt(i3) == '-') {
            addLabelError(info, IDNA.Error.LEADING_HYPHEN);
        }
        int i5 = i3 + length;
        if (sbDecode.charAt(i5 - 1) == '-') {
            addLabelError(info, IDNA.Error.TRAILING_HYPHEN);
        }
        boolean z3 = (this.options & 2) != 0;
        int i6 = i3;
        char c = 0;
        do {
            char cCharAt = sbDecode.charAt(i6);
            if (cCharAt <= 127) {
                if (cCharAt == '.') {
                    addLabelError(info, IDNA.Error.LABEL_HAS_DOT);
                    sbDecode.setCharAt(i6, (char) 65533);
                } else if (z3 && asciiData[cCharAt] < 0) {
                    addLabelError(info, IDNA.Error.DISALLOWED);
                    sbDecode.setCharAt(i6, (char) 65533);
                }
            } else {
                char c2 = (char) (c | cCharAt);
                if (z3 && isNonASCIIDisallowedSTD3Valid(cCharAt)) {
                    addLabelError(info, IDNA.Error.DISALLOWED);
                    sbDecode.setCharAt(i6, (char) 65533);
                } else if (cCharAt == 65533) {
                    addLabelError(info, IDNA.Error.DISALLOWED);
                }
                c = c2;
            }
            i6++;
        } while (i6 < i5);
        int iCodePointAt = sbDecode.codePointAt(i3);
        if ((U_GET_GC_MASK(iCodePointAt) & U_GC_M_MASK) != 0) {
            addLabelError(info, IDNA.Error.LEADING_COMBINING_MARK);
            sbDecode.setCharAt(i3, (char) 65533);
            if (iCodePointAt > 65535) {
                sbDecode.deleteCharAt(i3 + 1);
                length--;
                if (sbDecode == sb) {
                    i4--;
                }
            }
        }
        if (!hasCertainLabelErrors(info, severeErrors)) {
            if ((this.options & 4) != 0 && (!isBiDi(info) || isOkBiDi(info))) {
                checkLabelBiDi(sbDecode, i3, length, info);
            }
            if ((this.options & 8) != 0 && (c & 8204) == 8204 && !isLabelOkContextJ(sbDecode, i3, length)) {
                addLabelError(info, IDNA.Error.CONTEXTJ);
            }
            if ((this.options & 64) != 0 && c >= 183) {
                checkLabelContextO(sbDecode, i3, length, info);
            }
            if (z) {
                if (z2) {
                    if (i4 > 63) {
                        addLabelError(info, IDNA.Error.LABEL_TOO_LONG);
                    }
                    return i4;
                }
                if (c >= 128) {
                    try {
                        StringBuilder sbEncode = Punycode.encode(sbDecode.subSequence(i3, length + i3), null);
                        sbEncode.insert(0, "xn--");
                        if (sbEncode.length() > 63) {
                            addLabelError(info, IDNA.Error.LABEL_TOO_LONG);
                        }
                        return replaceLabel(sb, i, i4, sbEncode, sbEncode.length());
                    } catch (StringPrepParseException e2) {
                        throw new ICUException(e2);
                    }
                }
                if (length > 63) {
                    addLabelError(info, IDNA.Error.LABEL_TOO_LONG);
                }
            }
        } else if (z2) {
            addLabelError(info, IDNA.Error.INVALID_ACE_LABEL);
            return markBadACELabel(sb, i, i4, z, info);
        }
        return replaceLabel(sb, i, i4, sbDecode, length);
    }

    private int markBadACELabel(StringBuilder sb, int i, int i2, boolean z, IDNA.Info info) {
        boolean z2 = true;
        boolean z3 = (this.options & 2) != 0;
        int i3 = i + 4;
        int i4 = i + i2;
        boolean z4 = true;
        do {
            char cCharAt = sb.charAt(i3);
            if (cCharAt <= 127) {
                if (cCharAt == '.') {
                    addLabelError(info, IDNA.Error.LABEL_HAS_DOT);
                    sb.setCharAt(i3, (char) 65533);
                } else if (asciiData[cCharAt] < 0) {
                    if (z3) {
                        sb.setCharAt(i3, (char) 65533);
                    } else {
                        z2 = false;
                    }
                }
                z2 = false;
                z4 = false;
            } else {
                z2 = false;
                z4 = false;
            }
            i3++;
        } while (i3 < i4);
        if (z2) {
            sb.insert(i4, (char) 65533);
            return i2 + 1;
        }
        if (z && z4 && i2 > 63) {
            addLabelError(info, IDNA.Error.LABEL_TOO_LONG);
            return i2;
        }
        return i2;
    }

    private void checkLabelBiDi(CharSequence charSequence, int i, int i2, IDNA.Info info) {
        int iU_MASK;
        int iCodePointAt = Character.codePointAt(charSequence, i);
        int iCharCount = Character.charCount(iCodePointAt) + i;
        int iU_MASK2 = U_MASK(UBiDiProps.INSTANCE.getClass(iCodePointAt));
        if (((~L_R_AL_MASK) & iU_MASK2) != 0) {
            setNotOkBiDi(info);
        }
        int iCharCount2 = i + i2;
        while (true) {
            if (iCharCount < iCharCount2) {
                int iCodePointBefore = Character.codePointBefore(charSequence, iCharCount2);
                iCharCount2 -= Character.charCount(iCodePointBefore);
                int i3 = UBiDiProps.INSTANCE.getClass(iCodePointBefore);
                if (i3 != 17) {
                    iU_MASK = U_MASK(i3);
                    break;
                }
            } else {
                iU_MASK = iU_MASK2;
                break;
            }
        }
        if ((L_MASK & iU_MASK2) == 0 ? ((~R_AL_EN_AN_MASK) & iU_MASK) != 0 : ((~L_EN_MASK) & iU_MASK) != 0) {
            setNotOkBiDi(info);
        }
        int iU_MASK3 = iU_MASK | iU_MASK2;
        while (iCharCount < iCharCount2) {
            int iCodePointAt2 = Character.codePointAt(charSequence, iCharCount);
            iCharCount += Character.charCount(iCodePointAt2);
            iU_MASK3 |= U_MASK(UBiDiProps.INSTANCE.getClass(iCodePointAt2));
        }
        if ((L_MASK & iU_MASK2) != 0) {
            if (((~L_EN_ES_CS_ET_ON_BN_NSM_MASK) & iU_MASK3) != 0) {
                setNotOkBiDi(info);
            }
        } else {
            if (((~R_AL_AN_EN_ES_CS_ET_ON_BN_NSM_MASK) & iU_MASK3) != 0) {
                setNotOkBiDi(info);
            }
            if ((EN_AN_MASK & iU_MASK3) == EN_AN_MASK) {
                setNotOkBiDi(info);
            }
        }
        if ((R_AL_AN_MASK & iU_MASK3) != 0) {
            setBiDi(info);
        }
    }

    private static boolean isASCIIOkBiDi(CharSequence charSequence, int i) {
        char cCharAt;
        int i2 = 0;
        for (int i3 = 0; i3 < i; i3++) {
            char cCharAt2 = charSequence.charAt(i3);
            if (cCharAt2 == '.') {
                if (i3 > i2 && (('a' > (cCharAt = charSequence.charAt(i3 - 1)) || cCharAt > 'z') && ('0' > cCharAt || cCharAt > '9'))) {
                    return false;
                }
                i2 = i3 + 1;
            } else if (i3 == i2) {
                if ('a' > cCharAt2 || cCharAt2 > 'z') {
                    return false;
                }
            } else if (cCharAt2 <= ' ' && (cCharAt2 >= 28 || ('\t' <= cCharAt2 && cCharAt2 <= '\r'))) {
                return false;
            }
        }
        return true;
    }

    private boolean isLabelOkContextJ(CharSequence charSequence, int i, int i2) {
        int i3 = i2 + i;
        for (int i4 = i; i4 < i3; i4++) {
            if (charSequence.charAt(i4) == 8204) {
                if (i4 == i) {
                    return false;
                }
                int iCodePointBefore = Character.codePointBefore(charSequence, i4);
                int iCharCount = i4 - Character.charCount(iCodePointBefore);
                if (uts46Norm2.getCombiningClass(iCodePointBefore) == 9) {
                    continue;
                } else {
                    while (true) {
                        int joiningType = UBiDiProps.INSTANCE.getJoiningType(iCodePointBefore);
                        if (joiningType == 5) {
                            if (iCharCount == 0) {
                                return false;
                            }
                            iCodePointBefore = Character.codePointBefore(charSequence, iCharCount);
                            iCharCount -= Character.charCount(iCodePointBefore);
                        } else {
                            if (joiningType != 3 && joiningType != 2) {
                                return false;
                            }
                            int iCharCount2 = i4 + 1;
                            while (iCharCount2 != i3) {
                                int iCodePointAt = Character.codePointAt(charSequence, iCharCount2);
                                iCharCount2 += Character.charCount(iCodePointAt);
                                int joiningType2 = UBiDiProps.INSTANCE.getJoiningType(iCodePointAt);
                                if (joiningType2 != 5) {
                                    if (joiningType2 != 4 && joiningType2 != 2) {
                                        return false;
                                    }
                                }
                            }
                            return false;
                        }
                    }
                }
            } else if (charSequence.charAt(i4) != 8205) {
                continue;
            } else {
                if (i4 == i) {
                    return false;
                }
                if (uts46Norm2.getCombiningClass(Character.codePointBefore(charSequence, i4)) != 9) {
                    return false;
                }
            }
        }
        return true;
    }

    private void checkLabelContextO(CharSequence charSequence, int i, int i2, IDNA.Info info) {
        int i3 = (i2 + i) - 1;
        byte b = 0;
        for (int i4 = i; i4 <= i3; i4++) {
            char cCharAt = charSequence.charAt(i4);
            if (cCharAt >= 183) {
                if (cCharAt <= 1785) {
                    if (cCharAt == 183) {
                        if (i >= i4 || charSequence.charAt(i4 - 1) != 'l' || i4 >= i3 || charSequence.charAt(i4 + 1) != 'l') {
                            addLabelError(info, IDNA.Error.CONTEXTO_PUNCTUATION);
                        }
                    } else if (cCharAt == 885) {
                        if (i4 >= i3 || 14 != UScript.getScript(Character.codePointAt(charSequence, i4 + 1))) {
                            addLabelError(info, IDNA.Error.CONTEXTO_PUNCTUATION);
                        }
                    } else if (cCharAt == 1523 || cCharAt == 1524) {
                        if (i >= i4 || 19 != UScript.getScript(Character.codePointBefore(charSequence, i4))) {
                            addLabelError(info, IDNA.Error.CONTEXTO_PUNCTUATION);
                        }
                    } else if (1632 <= cCharAt) {
                        if (cCharAt <= 1641) {
                            if (b > 0) {
                                addLabelError(info, IDNA.Error.CONTEXTO_DIGITS);
                            }
                            b = -1;
                        } else if (1776 <= cCharAt) {
                            if (b < 0) {
                                addLabelError(info, IDNA.Error.CONTEXTO_DIGITS);
                            }
                            b = 1;
                        }
                    }
                } else if (cCharAt == 12539) {
                    int iCharCount = i;
                    while (true) {
                        if (iCharCount > i3) {
                            addLabelError(info, IDNA.Error.CONTEXTO_PUNCTUATION);
                            break;
                        }
                        int iCodePointAt = Character.codePointAt(charSequence, iCharCount);
                        int script = UScript.getScript(iCodePointAt);
                        if (script == 20 || script == 22 || script == 17) {
                            break;
                        } else {
                            iCharCount += Character.charCount(iCodePointAt);
                        }
                    }
                }
            }
        }
    }

    private static int U_MASK(int i) {
        return 1 << i;
    }

    private static int U_GET_GC_MASK(int i) {
        return 1 << UCharacter.getType(i);
    }
}
