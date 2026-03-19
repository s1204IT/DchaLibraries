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

    public CjkBreakEngine(boolean korean) throws IOException {
        super(1);
        this.fDictionary = null;
        this.fDictionary = DictionaryData.loadDictionaryFor("Hira");
        if (korean) {
            setCharacters(fHangulWordSet);
            return;
        }
        new UnicodeSet();
        UnicodeSet cjSet = new UnicodeSet();
        cjSet.addAll(fHanWordSet);
        cjSet.addAll(fKatakanaWordSet);
        cjSet.addAll(fHiraganaWordSet);
        cjSet.add(65392);
        cjSet.add(12540);
        setCharacters(cjSet);
    }

    public boolean equals(Object obj) {
        if (obj instanceof CjkBreakEngine) {
            return this.fSet.equals(obj.fSet);
        }
        return false;
    }

    public int hashCode() {
        return getClass().hashCode();
    }

    private static int getKatakanaCost(int wordlength) {
        int[] katakanaCost = {8192, 984, 408, 240, 204, 252, 300, 372, 480};
        if (wordlength > 8) {
            return 8192;
        }
        return katakanaCost[wordlength];
    }

    private static boolean isKatakana(int value) {
        if (value < 12449 || value > 12542 || value == 12539) {
            return value >= 65382 && value <= 65439;
        }
        return true;
    }

    @Override
    public int divideUpDictionaryRange(CharacterIterator inText, int startPos, int endPos, DictionaryBreakEngine.DequeI foundBreaks) {
        boolean isNormalized;
        CharacterIterator text;
        if (startPos >= endPos) {
            return 0;
        }
        inText.setIndex(startPos);
        int inputLength = endPos - startPos;
        int[] charPositions = new int[inputLength + 1];
        StringBuffer s = new StringBuffer("");
        inText.setIndex(startPos);
        while (inText.getIndex() < endPos) {
            s.append(inText.current());
            inText.next();
        }
        String prenormstr = s.toString();
        if (Normalizer.quickCheck(prenormstr, Normalizer.NFKC) == Normalizer.YES) {
            isNormalized = true;
        } else {
            isNormalized = Normalizer.isNormalized(prenormstr, Normalizer.NFKC, 0);
        }
        int numChars = 0;
        if (isNormalized) {
            text = new java.text.StringCharacterIterator(prenormstr);
            int index = 0;
            charPositions[0] = 0;
            while (index < prenormstr.length()) {
                int codepoint = prenormstr.codePointAt(index);
                index += Character.charCount(codepoint);
                numChars++;
                charPositions[numChars] = index;
            }
        } else {
            String normStr = Normalizer.normalize(prenormstr, Normalizer.NFKC);
            text = new java.text.StringCharacterIterator(normStr);
            charPositions = new int[normStr.length() + 1];
            Normalizer normalizer = new Normalizer(prenormstr, Normalizer.NFKC, 0);
            int index2 = 0;
            charPositions[0] = 0;
            while (index2 < normalizer.endIndex()) {
                normalizer.next();
                numChars++;
                index2 = normalizer.getIndex();
                charPositions[numChars] = index2;
            }
        }
        int[] bestSnlp = new int[numChars + 1];
        bestSnlp[0] = 0;
        for (int i = 1; i <= numChars; i++) {
            bestSnlp[i] = Integer.MAX_VALUE;
        }
        int[] prev = new int[numChars + 1];
        for (int i2 = 0; i2 <= numChars; i2++) {
            prev[i2] = -1;
        }
        int[] values = new int[numChars];
        int[] lengths = new int[numChars];
        boolean is_prev_katakana = false;
        for (int i3 = 0; i3 < numChars; i3++) {
            text.setIndex(i3);
            if (bestSnlp[i3] != Integer.MAX_VALUE) {
                int maxSearchLength = i3 + 20 < numChars ? 20 : numChars - i3;
                int[] count_ = new int[1];
                this.fDictionary.matches(text, maxSearchLength, lengths, count_, maxSearchLength, values);
                int count = count_[0];
                if ((count == 0 || lengths[0] != 1) && CharacterIteration.current32(text) != Integer.MAX_VALUE && !fHangulWordSet.contains(CharacterIteration.current32(text))) {
                    values[count] = 255;
                    lengths[count] = 1;
                    count++;
                }
                for (int j = 0; j < count; j++) {
                    int newSnlp = bestSnlp[i3] + values[j];
                    if (newSnlp < bestSnlp[lengths[j] + i3]) {
                        bestSnlp[lengths[j] + i3] = newSnlp;
                        prev[lengths[j] + i3] = i3;
                    }
                }
                text.setIndex(i3);
                boolean is_katakana = isKatakana(CharacterIteration.current32(text));
                if (!is_prev_katakana && is_katakana) {
                    int j2 = i3 + 1;
                    CharacterIteration.next32(text);
                    while (j2 < numChars && j2 - i3 < 20 && isKatakana(CharacterIteration.current32(text))) {
                        CharacterIteration.next32(text);
                        j2++;
                    }
                    if (j2 - i3 < 20) {
                        int newSnlp2 = bestSnlp[i3] + getKatakanaCost(j2 - i3);
                        if (newSnlp2 < bestSnlp[j2]) {
                            bestSnlp[j2] = newSnlp2;
                            prev[j2] = i3;
                        }
                    }
                }
                is_prev_katakana = is_katakana;
            }
        }
        int[] t_boundary = new int[numChars + 1];
        int numBreaks = 0;
        if (bestSnlp[numChars] == Integer.MAX_VALUE) {
            t_boundary[0] = numChars;
            numBreaks = 1;
        } else {
            for (int i4 = numChars; i4 > 0; i4 = prev[i4]) {
                t_boundary[numBreaks] = i4;
                numBreaks++;
            }
            Assert.assrt(prev[t_boundary[numBreaks + (-1)]] == 0);
        }
        if (foundBreaks.size() == 0 || foundBreaks.peek() < startPos) {
            t_boundary[numBreaks] = 0;
            numBreaks++;
        }
        int correctedNumBreaks = 0;
        for (int i5 = numBreaks - 1; i5 >= 0; i5--) {
            int pos = charPositions[t_boundary[i5]] + startPos;
            if (!(foundBreaks.contains(pos) || pos == startPos)) {
                foundBreaks.push(charPositions[t_boundary[i5]] + startPos);
                correctedNumBreaks++;
            }
        }
        if (!foundBreaks.isEmpty() && foundBreaks.peek() == endPos) {
            foundBreaks.pop();
            correctedNumBreaks--;
        }
        if (!foundBreaks.isEmpty()) {
            inText.setIndex(foundBreaks.peek());
        }
        return correctedNumBreaks;
    }
}
