package android.icu.text;

import android.icu.lang.UCharacterEnums;

abstract class CharsetRecog_Unicode extends CharsetRecognizer {
    @Override
    abstract String getName();

    @Override
    abstract CharsetMatch match(CharsetDetector charsetDetector);

    CharsetRecog_Unicode() {
    }

    static int codeUnit16FromBytes(byte b, byte b2) {
        return ((b & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8) | (b2 & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
    }

    static int adjustConfidence(int i, int i2) {
        if (i == 0) {
            i2 -= 10;
        } else if ((i >= 32 && i <= 255) || i == 10) {
            i2 += 10;
        }
        if (i2 < 0) {
            return 0;
        }
        if (i2 > 100) {
            return 100;
        }
        return i2;
    }

    static class CharsetRecog_UTF_16_BE extends CharsetRecog_Unicode {
        CharsetRecog_UTF_16_BE() {
        }

        @Override
        String getName() {
            return "UTF-16BE";
        }

        @Override
        CharsetMatch match(CharsetDetector charsetDetector) {
            byte[] bArr = charsetDetector.fRawInput;
            int iMin = Math.min(bArr.length, 30);
            int i = 0;
            int iAdjustConfidence = 10;
            int i2 = 0;
            while (true) {
                if (i2 >= iMin - 1) {
                    break;
                }
                int iCodeUnit16FromBytes = codeUnit16FromBytes(bArr[i2], bArr[i2 + 1]);
                if (i2 != 0 || iCodeUnit16FromBytes != 65279) {
                    iAdjustConfidence = adjustConfidence(iCodeUnit16FromBytes, iAdjustConfidence);
                    if (iAdjustConfidence == 0 || iAdjustConfidence == 100) {
                        break;
                    }
                    i2 += 2;
                } else {
                    iAdjustConfidence = 100;
                    break;
                }
            }
            if (iMin >= 4 || iAdjustConfidence >= 100) {
                i = iAdjustConfidence;
            }
            if (i > 0) {
                return new CharsetMatch(charsetDetector, this, i);
            }
            return null;
        }
    }

    static class CharsetRecog_UTF_16_LE extends CharsetRecog_Unicode {
        CharsetRecog_UTF_16_LE() {
        }

        @Override
        String getName() {
            return "UTF-16LE";
        }

        @Override
        CharsetMatch match(CharsetDetector charsetDetector) {
            byte[] bArr = charsetDetector.fRawInput;
            int iMin = Math.min(bArr.length, 30);
            int i = 0;
            int iAdjustConfidence = 10;
            int i2 = 0;
            while (true) {
                if (i2 >= iMin - 1) {
                    break;
                }
                int iCodeUnit16FromBytes = codeUnit16FromBytes(bArr[i2 + 1], bArr[i2]);
                if (i2 != 0 || iCodeUnit16FromBytes != 65279) {
                    iAdjustConfidence = adjustConfidence(iCodeUnit16FromBytes, iAdjustConfidence);
                    if (iAdjustConfidence == 0 || iAdjustConfidence == 100) {
                        break;
                    }
                    i2 += 2;
                } else {
                    iAdjustConfidence = 100;
                    break;
                }
            }
            if (iMin >= 4 || iAdjustConfidence >= 100) {
                i = iAdjustConfidence;
            }
            if (i > 0) {
                return new CharsetMatch(charsetDetector, this, i);
            }
            return null;
        }
    }

    static abstract class CharsetRecog_UTF_32 extends CharsetRecog_Unicode {
        abstract int getChar(byte[] bArr, int i);

        @Override
        abstract String getName();

        CharsetRecog_UTF_32() {
        }

        @Override
        CharsetMatch match(CharsetDetector charsetDetector) {
            boolean z;
            byte[] bArr = charsetDetector.fRawInput;
            int i = (charsetDetector.fRawLength / 4) * 4;
            if (i == 0) {
                return null;
            }
            int i2 = 0;
            if (getChar(bArr, 0) != 65279) {
                z = false;
            } else {
                z = true;
            }
            int i3 = 0;
            int i4 = 0;
            for (int i5 = 0; i5 < i; i5 += 4) {
                int i6 = getChar(bArr, i5);
                if (i6 < 0 || i6 >= 1114111 || (i6 >= 55296 && i6 <= 57343)) {
                    i3++;
                } else {
                    i4++;
                }
            }
            int i7 = 80;
            if (!z || i3 != 0) {
                if (!z || i4 <= i3 * 10) {
                    if (i4 <= 3 || i3 != 0) {
                        if (i4 <= 0 || i3 != 0) {
                            if (i4 > i3 * 10) {
                                i2 = 25;
                            }
                            i7 = i2;
                        }
                    } else {
                        i7 = 100;
                    }
                }
            }
            if (i7 == 0) {
                return null;
            }
            return new CharsetMatch(charsetDetector, this, i7);
        }
    }

    static class CharsetRecog_UTF_32_BE extends CharsetRecog_UTF_32 {
        CharsetRecog_UTF_32_BE() {
        }

        @Override
        int getChar(byte[] bArr, int i) {
            return (bArr[i + 3] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) | ((bArr[i + 0] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 24) | ((bArr[i + 1] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 16) | ((bArr[i + 2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8);
        }

        @Override
        String getName() {
            return "UTF-32BE";
        }
    }

    static class CharsetRecog_UTF_32_LE extends CharsetRecog_UTF_32 {
        CharsetRecog_UTF_32_LE() {
        }

        @Override
        int getChar(byte[] bArr, int i) {
            return (bArr[i + 0] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) | ((bArr[i + 3] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 24) | ((bArr[i + 2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 16) | ((bArr[i + 1] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8);
        }

        @Override
        String getName() {
            return "UTF-32LE";
        }
    }
}
