package android.icu.text;

import android.icu.lang.UCharacter;

final class BidiWriter {
    static final char LRM_CHAR = 8206;
    static final int MASK_R_AL = 8194;
    static final char RLM_CHAR = 8207;

    BidiWriter() {
    }

    private static boolean IsCombining(int i) {
        return ((1 << i) & 448) != 0;
    }

    private static String doWriteForward(String str, int i) {
        int i2 = i & 10;
        if (i2 == 0) {
            return str;
        }
        int charCount = 0;
        if (i2 == 2) {
            StringBuffer stringBuffer = new StringBuffer(str.length());
            do {
                int iCharAt = UTF16.charAt(str, charCount);
                charCount += UTF16.getCharCount(iCharAt);
                UTF16.append(stringBuffer, UCharacter.getMirror(iCharAt));
            } while (charCount < str.length());
            return stringBuffer.toString();
        }
        if (i2 == 8) {
            StringBuilder sb = new StringBuilder(str.length());
            while (true) {
                int i3 = charCount + 1;
                char cCharAt = str.charAt(charCount);
                if (!Bidi.IsBidiControlChar(cCharAt)) {
                    sb.append(cCharAt);
                }
                if (i3 < str.length()) {
                    charCount = i3;
                } else {
                    return sb.toString();
                }
            }
        } else {
            StringBuffer stringBuffer2 = new StringBuffer(str.length());
            do {
                int iCharAt2 = UTF16.charAt(str, charCount);
                charCount += UTF16.getCharCount(iCharAt2);
                if (!Bidi.IsBidiControlChar(iCharAt2)) {
                    UTF16.append(stringBuffer2, UCharacter.getMirror(iCharAt2));
                }
            } while (charCount < str.length());
            return stringBuffer2.toString();
        }
    }

    private static String doWriteForward(char[] cArr, int i, int i2, int i3) {
        return doWriteForward(new String(cArr, i, i2 - i), i3);
    }

    static String writeReverse(String str, int i) {
        int iCharAt;
        int charCount;
        StringBuffer stringBuffer = new StringBuffer(str.length());
        switch (i & 11) {
            case 0:
                int length = str.length();
                while (true) {
                    int charCount2 = length - UTF16.getCharCount(UTF16.charAt(str, length - 1));
                    stringBuffer.append(str.substring(charCount2, length));
                    if (charCount2 > 0) {
                        length = charCount2;
                    }
                    break;
                }
                break;
            case 1:
                int length2 = str.length();
                while (true) {
                    int charCount3 = length2;
                    do {
                        iCharAt = UTF16.charAt(str, charCount3 - 1);
                        charCount3 -= UTF16.getCharCount(iCharAt);
                        if (charCount3 > 0) {
                        }
                        stringBuffer.append(str.substring(charCount3, length2));
                        if (charCount3 <= 0) {
                            length2 = charCount3;
                        }
                        break;
                    } while (IsCombining(UCharacter.getType(iCharAt)));
                    stringBuffer.append(str.substring(charCount3, length2));
                    if (charCount3 <= 0) {
                    }
                }
                break;
            default:
                int length3 = str.length();
                while (true) {
                    int iCharAt2 = UTF16.charAt(str, length3 - 1);
                    int charCount4 = length3 - UTF16.getCharCount(iCharAt2);
                    if ((i & 1) != 0) {
                        while (charCount4 > 0 && IsCombining(UCharacter.getType(iCharAt2))) {
                            iCharAt2 = UTF16.charAt(str, charCount4 - 1);
                            charCount4 -= UTF16.getCharCount(iCharAt2);
                        }
                    }
                    if ((i & 8) == 0 || !Bidi.IsBidiControlChar(iCharAt2)) {
                        if ((i & 2) != 0) {
                            int mirror = UCharacter.getMirror(iCharAt2);
                            UTF16.append(stringBuffer, mirror);
                            charCount = UTF16.getCharCount(mirror) + charCount4;
                        } else {
                            charCount = charCount4;
                        }
                        stringBuffer.append(str.substring(charCount, length3));
                    }
                    if (charCount4 > 0) {
                        length3 = charCount4;
                    }
                    break;
                }
                break;
        }
        return stringBuffer.toString();
    }

    static String doWriteReverse(char[] cArr, int i, int i2, int i3) {
        return writeReverse(new String(cArr, i, i2 - i), i3);
    }

    static String writeReordered(Bidi bidi, int i) {
        char[] cArr = bidi.text;
        int iCountRuns = bidi.countRuns();
        if ((bidi.reorderingOptions & 1) != 0) {
            i = (i | 4) & (-9);
        }
        if ((bidi.reorderingOptions & 2) != 0) {
            i = (i | 8) & (-5);
        }
        if (bidi.reorderingMode != 4 && bidi.reorderingMode != 5 && bidi.reorderingMode != 6 && bidi.reorderingMode != 3) {
            i &= -5;
        }
        int i2 = i & 4;
        StringBuilder sb = new StringBuilder(i2 != 0 ? bidi.length * 2 : bidi.length);
        if ((i & 16) == 0) {
            if (i2 == 0) {
                for (int i3 = 0; i3 < iCountRuns; i3++) {
                    BidiRun visualRun = bidi.getVisualRun(i3);
                    if (visualRun.isEvenRun()) {
                        sb.append(doWriteForward(cArr, visualRun.start, visualRun.limit, i & (-3)));
                    } else {
                        sb.append(doWriteReverse(cArr, visualRun.start, visualRun.limit, i));
                    }
                }
            } else {
                byte[] bArr = bidi.dirProps;
                for (int i4 = 0; i4 < iCountRuns; i4++) {
                    BidiRun visualRun2 = bidi.getVisualRun(i4);
                    int i5 = bidi.runs[i4].insertRemove;
                    if (i5 < 0) {
                        i5 = 0;
                    }
                    if (visualRun2.isEvenRun()) {
                        if (bidi.isInverse() && bArr[visualRun2.start] != 0) {
                            i5 |= 1;
                        }
                        char c = (i5 & 1) != 0 ? (char) 8206 : (i5 & 4) != 0 ? (char) 8207 : (char) 0;
                        if (c != 0) {
                            sb.append(c);
                        }
                        sb.append(doWriteForward(cArr, visualRun2.start, visualRun2.limit, i & (-3)));
                        if (bidi.isInverse() && bArr[visualRun2.limit - 1] != 0) {
                            i5 |= 2;
                        }
                        char c2 = (i5 & 2) != 0 ? (char) 8206 : (i5 & 8) != 0 ? (char) 8207 : (char) 0;
                        if (c2 != 0) {
                            sb.append(c2);
                        }
                    } else {
                        if (bidi.isInverse() && !bidi.testDirPropFlagAt(MASK_R_AL, visualRun2.limit - 1)) {
                            i5 |= 4;
                        }
                        char c3 = (i5 & 1) != 0 ? (char) 8206 : (i5 & 4) != 0 ? (char) 8207 : (char) 0;
                        if (c3 != 0) {
                            sb.append(c3);
                        }
                        sb.append(doWriteReverse(cArr, visualRun2.start, visualRun2.limit, i));
                        if (bidi.isInverse() && (Bidi.DirPropFlag(bArr[visualRun2.start]) & MASK_R_AL) == 0) {
                            i5 |= 8;
                        }
                        char c4 = (i5 & 2) != 0 ? (char) 8206 : (i5 & 8) != 0 ? (char) 8207 : (char) 0;
                        if (c4 != 0) {
                            sb.append(c4);
                        }
                    }
                }
            }
        } else if (i2 != 0) {
            byte[] bArr2 = bidi.dirProps;
            while (true) {
                iCountRuns--;
                if (iCountRuns < 0) {
                    break;
                }
                BidiRun visualRun3 = bidi.getVisualRun(iCountRuns);
                if (visualRun3.isEvenRun()) {
                    if (bArr2[visualRun3.limit - 1] != 0) {
                        sb.append(LRM_CHAR);
                    }
                    sb.append(doWriteReverse(cArr, visualRun3.start, visualRun3.limit, i & (-3)));
                    if (bArr2[visualRun3.start] != 0) {
                        sb.append(LRM_CHAR);
                    }
                } else {
                    if ((Bidi.DirPropFlag(bArr2[visualRun3.start]) & MASK_R_AL) == 0) {
                        sb.append(RLM_CHAR);
                    }
                    sb.append(doWriteForward(cArr, visualRun3.start, visualRun3.limit, i));
                    if ((Bidi.DirPropFlag(bArr2[visualRun3.limit - 1]) & MASK_R_AL) == 0) {
                        sb.append(RLM_CHAR);
                    }
                }
            }
        } else {
            while (true) {
                iCountRuns--;
                if (iCountRuns < 0) {
                    break;
                }
                BidiRun visualRun4 = bidi.getVisualRun(iCountRuns);
                if (visualRun4.isEvenRun()) {
                    sb.append(doWriteReverse(cArr, visualRun4.start, visualRun4.limit, i & (-3)));
                } else {
                    sb.append(doWriteForward(cArr, visualRun4.start, visualRun4.limit, i));
                }
            }
        }
        return sb.toString();
    }
}
