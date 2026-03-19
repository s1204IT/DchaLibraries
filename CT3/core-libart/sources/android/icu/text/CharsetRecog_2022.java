package android.icu.text;

import android.icu.lang.UCharacterEnums;

abstract class CharsetRecog_2022 extends CharsetRecognizer {
    CharsetRecog_2022() {
    }

    int match(byte[] text, int textLen, byte[][] escapeSequences) {
        int j;
        int hits = 0;
        int misses = 0;
        int shifts = 0;
        int i = 0;
        while (i < textLen) {
            if (text[i] == 27) {
                for (byte[] seq : escapeSequences) {
                    if (textLen - i >= seq.length) {
                        while (j < seq.length) {
                            j = seq[j] == text[i + j] ? j + 1 : 1;
                        }
                        hits++;
                        i += seq.length - 1;
                        break;
                    }
                }
                misses++;
                if (text[i] != 14 || text[i] == 15) {
                    shifts++;
                }
            } else if (text[i] != 14) {
                shifts++;
            }
            i++;
        }
        if (hits == 0) {
            return 0;
        }
        int quality = ((hits * 100) - (misses * 100)) / (hits + misses);
        if (hits + shifts < 5) {
            quality -= (5 - (hits + shifts)) * 10;
        }
        if (quality < 0) {
            return 0;
        }
        return quality;
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
        CharsetMatch match(CharsetDetector det) {
            int confidence = match(det.fInputBytes, det.fInputLen, this.escapeSequences);
            if (confidence == 0) {
                return null;
            }
            return new CharsetMatch(det, this, confidence);
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
        CharsetMatch match(CharsetDetector det) {
            int confidence = match(det.fInputBytes, det.fInputLen, this.escapeSequences);
            if (confidence == 0) {
                return null;
            }
            return new CharsetMatch(det, this, confidence);
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
        CharsetMatch match(CharsetDetector det) {
            int confidence = match(det.fInputBytes, det.fInputLen, this.escapeSequences);
            if (confidence == 0) {
                return null;
            }
            return new CharsetMatch(det, this, confidence);
        }
    }
}
