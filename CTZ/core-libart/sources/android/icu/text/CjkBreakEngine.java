package android.icu.text;

import android.icu.impl.Assert;
import android.icu.impl.CharacterIteration;
import android.icu.text.DictionaryBreakEngine;
import java.io.IOException;
import java.text.CharacterIterator;

class CjkBreakEngine extends DictionaryBreakEngine {
    private static final int kMaxKatakanaGroupLength = 20;
    private static final int kMaxKatakanaLength = 8;
    private static final int kint32max = Integer.MAX_VALUE;
    private static final int maxSnlp = 255;
    private DictionaryMatcher fDictionary;
    private static final UnicodeSet fHangulWordSet = new UnicodeSet();
    private static final UnicodeSet fHanWordSet = new UnicodeSet();
    private static final UnicodeSet fKatakanaWordSet = new UnicodeSet();
    private static final UnicodeSet fHiraganaWordSet = new UnicodeSet();

    static {
        fHangulWordSet.applyPattern("[\\uac00-\\ud7a3]");
        fHanWordSet.applyPattern("[:Han:]");
        fKatakanaWordSet.applyPattern("[[:Katakana:]\\uff9e\\uff9f]");
        fHiraganaWordSet.applyPattern("[:Hiragana:]");
        fHangulWordSet.freeze();
        fHanWordSet.freeze();
        fKatakanaWordSet.freeze();
        fHiraganaWordSet.freeze();
    }

    public CjkBreakEngine(boolean z) throws IOException {
        super(1);
        this.fDictionary = null;
        this.fDictionary = DictionaryData.loadDictionaryFor("Hira");
        if (z) {
            setCharacters(fHangulWordSet);
            return;
        }
        UnicodeSet unicodeSet = new UnicodeSet();
        unicodeSet.addAll(fHanWordSet);
        unicodeSet.addAll(fKatakanaWordSet);
        unicodeSet.addAll(fHiraganaWordSet);
        unicodeSet.add(65392);
        unicodeSet.add(12540);
        setCharacters(unicodeSet);
    }

    public boolean equals(Object obj) {
        if (obj instanceof CjkBreakEngine) {
            return this.fSet.equals(((CjkBreakEngine) obj).fSet);
        }
        return false;
    }

    public int hashCode() {
        return getClass().hashCode();
    }

    private static int getKatakanaCost(int i) {
        int[] iArr = {8192, 984, 408, 240, 204, 252, 300, 372, 480};
        if (i > 8) {
            return 8192;
        }
        return iArr[i];
    }

    private static boolean isKatakana(int i) {
        return (i >= 12449 && i <= 12542 && i != 12539) || (i >= 65382 && i <= 65439);
    }

    @Override
    public int divideUpDictionaryRange(CharacterIterator characterIterator, int i, int i2, DictionaryBreakEngine.DequeI dequeI) {
        java.text.StringCharacterIterator stringCharacterIterator;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        int[] iArr;
        int[] iArr2;
        int[] iArr3;
        int i8;
        int i9;
        int katakanaCost;
        if (i >= i2) {
            return 0;
        }
        characterIterator.setIndex(i);
        int i10 = 1;
        int[] iArr4 = new int[(i2 - i) + 1];
        StringBuffer stringBuffer = new StringBuffer("");
        characterIterator.setIndex(i);
        while (characterIterator.getIndex() < i2) {
            stringBuffer.append(characterIterator.current());
            characterIterator.next();
        }
        String string = stringBuffer.toString();
        if (Normalizer.quickCheck(string, Normalizer.NFKC) == Normalizer.YES || Normalizer.isNormalized(string, Normalizer.NFKC, 0)) {
            stringCharacterIterator = new java.text.StringCharacterIterator(string);
            iArr4[0] = 0;
            int iCharCount = 0;
            i3 = 0;
            while (iCharCount < string.length()) {
                iCharCount += Character.charCount(string.codePointAt(iCharCount));
                i3++;
                iArr4[i3] = iCharCount;
            }
        } else {
            String strNormalize = Normalizer.normalize(string, Normalizer.NFKC);
            stringCharacterIterator = new java.text.StringCharacterIterator(strNormalize);
            iArr4 = new int[strNormalize.length() + 1];
            Normalizer normalizer = new Normalizer(string, Normalizer.NFKC, 0);
            iArr4[0] = 0;
            int index = 0;
            i3 = 0;
            while (index < normalizer.endIndex()) {
                normalizer.next();
                i3++;
                index = normalizer.getIndex();
                iArr4[i3] = index;
            }
        }
        int i11 = i3 + 1;
        int[] iArr5 = new int[i11];
        iArr5[0] = 0;
        int i12 = 1;
        while (true) {
            i4 = Integer.MAX_VALUE;
            if (i12 > i3) {
                break;
            }
            iArr5[i12] = Integer.MAX_VALUE;
            i12++;
        }
        int[] iArr6 = new int[i11];
        for (int i13 = 0; i13 <= i3; i13++) {
            iArr6[i13] = -1;
        }
        int[] iArr7 = new int[i3];
        int[] iArr8 = new int[i3];
        int i14 = 0;
        boolean z = false;
        while (i14 < i3) {
            stringCharacterIterator.setIndex(i14);
            if (iArr5[i14] == i4) {
                i8 = i14;
                iArr = iArr8;
                iArr2 = iArr7;
                iArr3 = iArr6;
                i9 = i4;
            } else {
                if (i14 + 20 >= i3) {
                    i7 = i3 - i14;
                } else {
                    i7 = 20;
                }
                int[] iArr9 = new int[i10];
                iArr = iArr8;
                iArr2 = iArr7;
                iArr3 = iArr6;
                i8 = i14;
                i9 = Integer.MAX_VALUE;
                this.fDictionary.matches(stringCharacterIterator, i7, iArr, iArr9, i7, iArr2);
                int i15 = iArr9[0];
                stringCharacterIterator.setIndex(i8);
                if ((i15 == 0 || iArr[0] != 1) && CharacterIteration.current32(stringCharacterIterator) != Integer.MAX_VALUE && !fHangulWordSet.contains(CharacterIteration.current32(stringCharacterIterator))) {
                    iArr2[i15] = 255;
                    iArr[i15] = 1;
                    i15++;
                }
                for (int i16 = 0; i16 < i15; i16++) {
                    int i17 = iArr5[i8] + iArr2[i16];
                    if (i17 < iArr5[iArr[i16] + i8]) {
                        iArr5[iArr[i16] + i8] = i17;
                        iArr3[iArr[i16] + i8] = i8;
                    }
                }
                boolean zIsKatakana = isKatakana(CharacterIteration.current32(stringCharacterIterator));
                if (!z && zIsKatakana) {
                    int i18 = i8 + 1;
                    CharacterIteration.next32(stringCharacterIterator);
                    while (i18 < i3 && i18 - i8 < 20 && isKatakana(CharacterIteration.current32(stringCharacterIterator))) {
                        CharacterIteration.next32(stringCharacterIterator);
                        i18++;
                    }
                    int i19 = i18 - i8;
                    if (i19 < 20 && (katakanaCost = iArr5[i8] + getKatakanaCost(i19)) < iArr5[i18]) {
                        iArr5[i18] = katakanaCost;
                        iArr3[i18] = i8;
                    }
                }
                z = zIsKatakana;
            }
            i14 = i8 + 1;
            i4 = i9;
            iArr8 = iArr;
            iArr7 = iArr2;
            iArr6 = iArr3;
            i10 = 1;
        }
        int[] iArr10 = iArr6;
        int[] iArr11 = new int[i11];
        if (iArr5[i3] == i4) {
            iArr11[0] = i3;
            i5 = 1;
        } else {
            i5 = 0;
            while (i3 > 0) {
                iArr11[i5] = i3;
                i5++;
                i3 = iArr10[i3];
            }
            Assert.assrt(iArr10[iArr11[i5 + (-1)]] == 0);
        }
        if (dequeI.size() == 0 || dequeI.peek() < i) {
            i6 = 0;
            iArr11[i5] = 0;
            i5++;
        } else {
            i6 = 0;
        }
        for (int i20 = i5 - 1; i20 >= 0; i20--) {
            int i21 = iArr4[iArr11[i20]] + i;
            if (!dequeI.contains(i21) && i21 != i) {
                dequeI.push(iArr4[iArr11[i20]] + i);
                i6++;
            }
        }
        if (!dequeI.isEmpty() && dequeI.peek() == i2) {
            dequeI.pop();
            i6--;
        }
        if (!dequeI.isEmpty()) {
            characterIterator.setIndex(dequeI.peek());
        }
        return i6;
    }
}
