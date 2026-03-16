package jp.co.omronsoft.iwnnime.ml.jajp;

import java.util.Comparator;
import jp.co.omronsoft.iwnnime.ml.WnnWord;

public class ListComparatorJaJp implements Comparator<WnnWord> {
    @Override
    public int compare(WnnWord word1, WnnWord word2) {
        String string1 = word1.stroke;
        String string2 = word2.stroke;
        boolean hiragana1 = isHiragana(string1.charAt(0));
        boolean hiragana2 = isHiragana(string2.charAt(0));
        if (hiragana1 == hiragana2) {
            return string1.compareTo(string2);
        }
        if (hiragana1) {
            return -1;
        }
        return 1;
    }

    private boolean isHiragana(char c) {
        return 12353 <= c && c <= 12438;
    }
}
