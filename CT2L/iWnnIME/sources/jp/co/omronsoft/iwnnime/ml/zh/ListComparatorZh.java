package jp.co.omronsoft.iwnnime.ml.zh;

import java.util.Comparator;
import jp.co.omronsoft.iwnnime.ml.WnnWord;

public class ListComparatorZh implements Comparator<WnnWord> {
    @Override
    public int compare(WnnWord word1, WnnWord word2) {
        String string1 = word1.stroke;
        String string2 = word2.stroke;
        boolean supported1 = isZhongwen(string1.charAt(0));
        boolean supported2 = isZhongwen(string2.charAt(0));
        if (supported1 == supported2) {
            return string1.compareTo(string2);
        }
        if (supported1) {
            return -1;
        }
        return 1;
    }

    private boolean isZhongwen(char c) {
        return 12544 <= c && c <= 12591;
    }
}
