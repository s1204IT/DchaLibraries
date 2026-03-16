package jp.co.omronsoft.iwnnime.ml.hangul;

import java.util.Comparator;
import jp.co.omronsoft.iwnnime.ml.WnnWord;

public class ListComparatorHangul implements Comparator<WnnWord> {
    @Override
    public int compare(WnnWord word1, WnnWord word2) {
        String string1 = word1.stroke;
        String string2 = word2.stroke;
        boolean supported1 = isHangul(string1.charAt(0));
        boolean supported2 = isHangul(string2.charAt(0));
        if (supported1 == supported2) {
            return string1.compareTo(string2);
        }
        if (supported1) {
            return -1;
        }
        return 1;
    }

    private boolean isHangul(char c) {
        return 44033 <= c && c <= 55199;
    }
}
