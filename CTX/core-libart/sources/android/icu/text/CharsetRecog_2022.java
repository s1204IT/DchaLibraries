package android.icu.text;

import android.icu.lang.UCharacterEnums;

abstract class CharsetRecog_2022 extends CharsetRecognizer {
    CharsetRecog_2022() {
    }

    int match(byte[] bArr, int i, byte[][] bArr2) {
        int length = 0;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        while (length < i) {
            if (bArr[length] == 27) {
                for (byte[] bArr3 : bArr2) {
                    if (i - length >= bArr3.length) {
                        for (int i5 = 1; i5 < bArr3.length; i5++) {
                            if (bArr3[i5] != bArr[length + i5]) {
                                break;
                            }
                        }
                        i2++;
                        length += bArr3.length - 1;
                        break;
                    }
                }
                i3++;
                if (bArr[length] != 14 || bArr[length] == 15) {
                    i4++;
                }
            } else if (bArr[length] != 14) {
                i4++;
            }
            length++;
        }
        if (i2 == 0) {
            return 0;
        }
        int i6 = ((100 * i2) - (100 * i3)) / (i3 + i2);
        int i7 = i2 + i4;
        if (i7 < 5) {
            i6 -= (5 - i7) * 10;
        }
        if (i6 < 0) {
            return 0;
        }
        return i6;
    }

    static class CharsetRecog_2022JP extends CharsetRecog_2022 {
        private byte[][] escapeSequences = {new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 36, 40, 67}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 36, 40, 68}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 36, 64}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 36, 65}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 36, 66}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 38, 64}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 40, 66}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 40, 72}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 40, 73}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 40, 74}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 46, 65}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 46, 70}};

        CharsetRecog_2022JP() {
        }

        @Override
        String getName() {
            return "ISO-2022-JP";
        }

        @Override
        CharsetMatch match(CharsetDetector charsetDetector) {
            int iMatch = match(charsetDetector.fInputBytes, charsetDetector.fInputLen, this.escapeSequences);
            if (iMatch == 0) {
                return null;
            }
            return new CharsetMatch(charsetDetector, this, iMatch);
        }
    }

    static class CharsetRecog_2022KR extends CharsetRecog_2022 {
        private byte[][] escapeSequences = {new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 36, 41, 67}};

        CharsetRecog_2022KR() {
        }

        @Override
        String getName() {
            return "ISO-2022-KR";
        }

        @Override
        CharsetMatch match(CharsetDetector charsetDetector) {
            int iMatch = match(charsetDetector.fInputBytes, charsetDetector.fInputLen, this.escapeSequences);
            if (iMatch == 0) {
                return null;
            }
            return new CharsetMatch(charsetDetector, this, iMatch);
        }
    }

    static class CharsetRecog_2022CN extends CharsetRecog_2022 {
        private byte[][] escapeSequences = {new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 36, 41, 65}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 36, 41, 71}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 36, 42, 72}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 36, 41, 69}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 36, 43, 73}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 36, 43, 74}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 36, 43, 75}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 36, 43, 76}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 36, 43, 77}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 78}, new byte[]{UCharacterEnums.ECharacterCategory.OTHER_SYMBOL, 79}};

        CharsetRecog_2022CN() {
        }

        @Override
        String getName() {
            return "ISO-2022-CN";
        }

        @Override
        CharsetMatch match(CharsetDetector charsetDetector) {
            int iMatch = match(charsetDetector.fInputBytes, charsetDetector.fInputLen, this.escapeSequences);
            if (iMatch == 0) {
                return null;
            }
            return new CharsetMatch(charsetDetector, this, iMatch);
        }
    }
}
