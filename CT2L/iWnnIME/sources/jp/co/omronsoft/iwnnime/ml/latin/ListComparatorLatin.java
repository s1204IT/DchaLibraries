package jp.co.omronsoft.iwnnime.ml.latin;

import java.util.Comparator;
import jp.co.omronsoft.iwnnime.ml.WnnWord;

public class ListComparatorLatin implements Comparator<WnnWord> {
    @Override
    public int compare(WnnWord word1, WnnWord word2) {
        String string1 = word1.candidate;
        String string2 = word2.candidate;
        boolean latin1 = isLatin(string1.charAt(0));
        boolean latin2 = isLatin(string2.charAt(0));
        if (latin1 == latin2) {
            return string1.compareTo(string2);
        }
        if (latin1) {
            return -1;
        }
        return 1;
    }

    private boolean isLatin(char c) {
        if (' ' <= c && c <= 591) {
            return true;
        }
        if (7680 <= c && c <= 7935) {
            return true;
        }
        if (11360 > c || c > 11391) {
            return 42784 <= c && c <= 43007;
        }
        return true;
    }
}
