package jp.co.omronsoft.iwnnime.ml.jajp;

import java.util.Comparator;
import jp.co.omronsoft.iwnnime.ml.WnnWord;

public class ListComparatorEn implements Comparator<WnnWord> {
    @Override
    public int compare(WnnWord word1, WnnWord word2) {
        String string1 = word1.stroke;
        String string2 = word2.stroke;
        boolean alphabet1 = isAlphabet(string1.charAt(0));
        boolean alphabet2 = isAlphabet(string2.charAt(0));
        if (alphabet1 == alphabet2) {
            return string1.compareTo(string2);
        }
        if (alphabet1) {
            return -1;
        }
        return 1;
    }

    private boolean isAlphabet(char c) {
        return ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
    }
}
