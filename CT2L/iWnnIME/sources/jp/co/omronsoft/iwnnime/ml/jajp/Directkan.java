package jp.co.omronsoft.iwnnime.ml.jajp;

import android.content.SharedPreferences;
import java.util.HashMap;
import jp.co.omronsoft.iwnnime.ml.ComposingText;
import jp.co.omronsoft.iwnnime.ml.LetterConverter;
import jp.co.omronsoft.iwnnime.ml.StrSegment;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;

public class Directkan implements LetterConverter {
    private static final int MAX_LENGTH = 4;
    private static final HashMap<String, String> directkanTable = new HashMap<String, String>() {
        {
            put("1", "ぬ");
            put("2", "ふ");
            put("3", "あ");
            put("4", "う");
            put("5", "え");
            put("6", "お");
            put("7", "や");
            put("8", "ゆ");
            put("9", "よ");
            put("0", "わ");
            put("-", "ほ");
            put("^", "へ");
            put("¥", "ー");
            put("q", "た");
            put("w", "て");
            put("e", "い");
            put("r", "す");
            put("t", "か");
            put("y", "ん");
            put("u", "な");
            put("i", "に");
            put("o", "ら");
            put("p", "せ");
            put("@", "゛");
            put("[", "゜");
            put("a", "ち");
            put("s", "と");
            put("d", "し");
            put("f", "は");
            put("g", "き");
            put("h", "く");
            put("j", "ま");
            put("k", "の");
            put("l", "り");
            put(";", "れ");
            put(":", "け");
            put("]", "む");
            put("z", "つ");
            put("x", "さ");
            put("c", "そ");
            put("v", "ひ");
            put("b", "こ");
            put("n", "み");
            put("m", "も");
            put(iWnnEngine.DECO_OPERATION_SEPARATOR, "ね");
            put(".", "る");
            put("/", "め");
            put("\\", "ろ");
            put("!", "ぬ");
            put("\"", "ふ");
            put("#", "ぁ");
            put("$", "ぅ");
            put("%", "ぇ");
            put("&", "ぉ");
            put("'", "ゃ");
            put("(", "ゅ");
            put(")", "ょ");
            put("を", "を");
            put("=", "ほ");
            put("~", "へ");
            put("|", "ー");
            put("Q", "た");
            put("W", "て");
            put("E", "ぃ");
            put("R", "す");
            put("T", "か");
            put("Y", "ん");
            put("U", "な");
            put("I", "に");
            put("O", "ら");
            put("P", "せ");
            put("`", "゛");
            put("{", "「");
            put("A", "ち");
            put("S", "と");
            put("D", "し");
            put("F", "は");
            put("G", "き");
            put("H", "く");
            put("J", "ま");
            put("K", "の");
            put("L", "り");
            put("+", "れ");
            put("*", "け");
            put("}", "」");
            put("Z", "っ");
            put("X", "さ");
            put("C", "そ");
            put("V", "ひ");
            put("B", "こ");
            put("N", "み");
            put("M", "も");
            put("<", "、");
            put(">", "。");
            put("?", "・");
            put("_", "ろ");
            put("う@", "ヴ");
            put("か@", "が");
            put("き@", "ぎ");
            put("く@", "ぐ");
            put("け@", "げ");
            put("こ@", "ご");
            put("さ@", "ざ");
            put("し@", "じ");
            put("す@", "ず");
            put("せ@", "ぜ");
            put("そ@", "ぞ");
            put("た@", "だ");
            put("ち@", "ぢ");
            put("つ@", "づ");
            put("て@", "で");
            put("と@", "ど");
            put("は@", "ば");
            put("ひ@", "び");
            put("ふ@", "ぶ");
            put("へ@", "べ");
            put("ほ@", "ぼ");
            put("う`", "ヴ");
            put("か`", "が");
            put("き`", "ぎ");
            put("く`", "ぐ");
            put("け`", "げ");
            put("こ`", "ご");
            put("さ`", "ざ");
            put("し`", "じ");
            put("す`", "ず");
            put("せ`", "ぜ");
            put("そ`", "ぞ");
            put("た`", "だ");
            put("ち`", "ぢ");
            put("つ`", "づ");
            put("て`", "で");
            put("と`", "ど");
            put("は`", "ば");
            put("ひ`", "び");
            put("ふ`", "ぶ");
            put("へ`", "べ");
            put("ほ`", "ぼ");
            put("は[", "ぱ");
            put("ひ[", "ぴ");
            put("ふ[", "ぷ");
            put("へ[", "ぺ");
            put("ほ[", "ぽ");
        }
    };

    @Override
    public boolean convert(ComposingText text) {
        int cursor = text.getCursor(1);
        if (cursor <= 0) {
            return false;
        }
        StrSegment[] str = new StrSegment[4];
        int start = 4;
        int checkLength = Math.min(cursor, 4);
        for (int i = 1; i <= checkLength; i++) {
            str[4 - i] = text.getStrSegment(1, cursor - i);
            start--;
        }
        StringBuffer key = new StringBuffer();
        while (start < 4) {
            for (int i2 = start; i2 < 4; i2++) {
                key.append(str[i2].string);
            }
            String match = directkanTable.get(key.toString());
            if (match != null) {
                if (match.length() == 1) {
                    StrSegment[] out = {new StrSegment(match, str[start].from, str[3].to)};
                    text.replaceStrSegment(1, out, 4 - start);
                } else {
                    StrSegment[] out2 = {new StrSegment(match.substring(0, match.length() - 1), str[start].from, str[3].to - 1), new StrSegment(match.substring(match.length() - 1), str[3].to, str[3].to)};
                    text.replaceStrSegment(1, out2, 4 - start);
                }
                return true;
            }
            start++;
            key.delete(0, key.length());
        }
        return false;
    }

    @Override
    public void setPreferences(SharedPreferences pref) {
    }
}
