package android.icu.text;

import android.icu.lang.UCharacterEnums;

class CharsetRecog_UTF8 extends CharsetRecognizer {
    CharsetRecog_UTF8() {
    }

    @Override
    String getName() {
        return "UTF-8";
    }

    @Override
    CharsetMatch match(CharsetDetector charsetDetector) {
        int i;
        byte[] bArr = charsetDetector.fRawInput;
        int i2 = 0;
        boolean z = charsetDetector.fRawLength >= 3 && (bArr[0] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) == 239 && (bArr[1] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) == 187 && (bArr[2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) == 191;
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        while (i3 < charsetDetector.fRawLength) {
            byte b = bArr[i3];
            if ((b & Bidi.LEVEL_OVERRIDE) != 0) {
                if ((b & 224) != 192) {
                    if ((b & 240) != 224) {
                        if ((b & 248) != 240) {
                            i4++;
                        } else {
                            i = 3;
                        }
                    } else {
                        i = 2;
                    }
                } else {
                    i = 1;
                }
                while (true) {
                    i3++;
                    if (i3 < charsetDetector.fRawLength) {
                        if ((bArr[i3] & 192) != 128) {
                            i4++;
                            break;
                        }
                        i--;
                        if (i == 0) {
                            i5++;
                            break;
                        }
                    }
                }
            }
            i3++;
        }
        if (!z || i4 != 0) {
            if (!z || i5 <= i4 * 10) {
                if (i5 <= 3 || i4 != 0) {
                    if (i5 <= 0 || i4 != 0) {
                        if (i5 == 0 && i4 == 0) {
                            i2 = 15;
                        } else if (i5 > i4 * 10) {
                            i2 = 25;
                        }
                    } else {
                        i2 = 80;
                    }
                } else {
                    i2 = 100;
                }
            }
        }
        if (i2 == 0) {
            return null;
        }
        return new CharsetMatch(charsetDetector, this, i2);
    }
}
