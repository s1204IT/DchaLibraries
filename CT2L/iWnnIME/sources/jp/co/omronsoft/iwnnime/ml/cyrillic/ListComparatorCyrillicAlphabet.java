package jp.co.omronsoft.iwnnime.ml.cyrillic;

import java.util.Comparator;
import jp.co.omronsoft.iwnnime.ml.WnnWord;

public class ListComparatorCyrillicAlphabet implements Comparator<WnnWord> {
    @Override
    public int compare(WnnWord word1, WnnWord word2) {
        String string1 = word1.candidate;
        String string2 = word2.candidate;
        boolean supported1 = isCyrillic(string1.charAt(0));
        boolean supported2 = isCyrillic(string2.charAt(0));
        if (supported1 == supported2) {
            return string1.compareTo(string2);
        }
        if (supported1) {
            return -1;
        }
        return 1;
    }

    private boolean isCyrillic(char c) {
        return 1024 <= c && c <= 1327;
    }
}
