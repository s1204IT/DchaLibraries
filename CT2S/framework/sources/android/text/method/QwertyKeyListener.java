package android.text.method;

import android.text.AutoText;
import android.text.Editable;
import android.text.NoCopySpan;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.SparseArray;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;

public class QwertyKeyListener extends BaseKeyListener {
    private static QwertyKeyListener sFullKeyboardInstance;
    private TextKeyListener.Capitalize mAutoCap;
    private boolean mAutoText;
    private boolean mFullKeyboard;
    private static QwertyKeyListener[] sInstance = new QwertyKeyListener[TextKeyListener.Capitalize.values().length * 2];
    private static SparseArray<String> PICKER_SETS = new SparseArray<>();

    static {
        PICKER_SETS.put(65, "ÀÁÂÄÆÃÅĄĀ");
        PICKER_SETS.put(67, "ÇĆČ");
        PICKER_SETS.put(68, "Ď");
        PICKER_SETS.put(69, "ÈÉÊËĘĚĒ");
        PICKER_SETS.put(71, "Ğ");
        PICKER_SETS.put(76, "Ł");
        PICKER_SETS.put(73, "ÌÍÎÏĪİ");
        PICKER_SETS.put(78, "ÑŃŇ");
        PICKER_SETS.put(79, "ØŒÕÒÓÔÖŌ");
        PICKER_SETS.put(82, "Ř");
        PICKER_SETS.put(83, "ŚŠŞ");
        PICKER_SETS.put(84, "Ť");
        PICKER_SETS.put(85, "ÙÚÛÜŮŪ");
        PICKER_SETS.put(89, "ÝŸ");
        PICKER_SETS.put(90, "ŹŻŽ");
        PICKER_SETS.put(97, "àáâäæãåąā");
        PICKER_SETS.put(99, "çćč");
        PICKER_SETS.put(100, "ď");
        PICKER_SETS.put(101, "èéêëęěē");
        PICKER_SETS.put(103, "ğ");
        PICKER_SETS.put(105, "ìíîïīı");
        PICKER_SETS.put(108, "ł");
        PICKER_SETS.put(110, "ñńň");
        PICKER_SETS.put(111, "øœõòóôöō");
        PICKER_SETS.put(114, "ř");
        PICKER_SETS.put(115, "§ßśšş");
        PICKER_SETS.put(116, "ť");
        PICKER_SETS.put(117, "ùúûüůū");
        PICKER_SETS.put(121, "ýÿ");
        PICKER_SETS.put(122, "źżž");
        PICKER_SETS.put(61185, "…¥•®©±[]{}\\|");
        PICKER_SETS.put(47, "\\");
        PICKER_SETS.put(49, "¹½⅓¼⅛");
        PICKER_SETS.put(50, "²⅔");
        PICKER_SETS.put(51, "³¾⅜");
        PICKER_SETS.put(52, "⁴");
        PICKER_SETS.put(53, "⅝");
        PICKER_SETS.put(55, "⅞");
        PICKER_SETS.put(48, "ⁿ∅");
        PICKER_SETS.put(36, "¢£€¥₣₤₱");
        PICKER_SETS.put(37, "‰");
        PICKER_SETS.put(42, "†‡");
        PICKER_SETS.put(45, "–—");
        PICKER_SETS.put(43, "±");
        PICKER_SETS.put(40, "[{<");
        PICKER_SETS.put(41, "]}>");
        PICKER_SETS.put(33, "¡");
        PICKER_SETS.put(34, "“”«»˝");
        PICKER_SETS.put(63, "¿");
        PICKER_SETS.put(44, "‚„");
        PICKER_SETS.put(61, "≠≈∞");
        PICKER_SETS.put(60, "≤«‹");
        PICKER_SETS.put(62, "≥»›");
    }

    private QwertyKeyListener(TextKeyListener.Capitalize cap, boolean autoText, boolean fullKeyboard) {
        this.mAutoCap = cap;
        this.mAutoText = autoText;
        this.mFullKeyboard = fullKeyboard;
    }

    public QwertyKeyListener(TextKeyListener.Capitalize cap, boolean autoText) {
        this(cap, autoText, false);
    }

    public static QwertyKeyListener getInstance(boolean autoText, TextKeyListener.Capitalize cap) {
        int off = (cap.ordinal() * 2) + (autoText ? 1 : 0);
        if (sInstance[off] == null) {
            sInstance[off] = new QwertyKeyListener(cap, autoText);
        }
        return sInstance[off];
    }

    public static QwertyKeyListener getInstanceForFullKeyboard() {
        if (sFullKeyboardInstance == null) {
            sFullKeyboardInstance = new QwertyKeyListener(TextKeyListener.Capitalize.NONE, false, true);
        }
        return sFullKeyboardInstance;
    }

    @Override
    public int getInputType() {
        return makeTextContentType(this.mAutoCap, this.mAutoText);
    }

    @Override
    public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event) {
        int start;
        int count;
        char c;
        int pref = 0;
        if (view != null) {
            pref = TextKeyListener.getInstance().getPrefs(view.getContext());
        }
        int a = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);
        int selStart = Math.min(a, b);
        int selEnd = Math.max(a, b);
        if (selStart < 0 || selEnd < 0) {
            selEnd = 0;
            selStart = 0;
            Selection.setSelection(content, 0, 0);
        }
        int activeStart = content.getSpanStart(TextKeyListener.ACTIVE);
        int activeEnd = content.getSpanEnd(TextKeyListener.ACTIVE);
        int i = event.getUnicodeChar(getMetaState(content, event));
        if (!this.mFullKeyboard && (count = event.getRepeatCount()) > 0 && selStart == selEnd && selStart > 0 && (((c = content.charAt(selStart - 1)) == i || c == Character.toUpperCase(i)) && view != null && showCharacterPicker(view, content, c, false, count))) {
            resetMetaState(content);
            return true;
        }
        if (i == 61185) {
            if (view != null) {
                showCharacterPicker(view, content, KeyCharacterMap.PICKER_DIALOG_INPUT, true, 1);
            }
            resetMetaState(content);
            return true;
        }
        if (i == 61184) {
            if (selStart == selEnd) {
                start = selEnd;
                while (start > 0 && selEnd - start < 4 && Character.digit(content.charAt(start - 1), 16) >= 0) {
                    start--;
                }
            } else {
                start = selStart;
            }
            int ch = -1;
            try {
                String hex = TextUtils.substring(content, start, selEnd);
                ch = Integer.parseInt(hex, 16);
            } catch (NumberFormatException e) {
            }
            if (ch >= 0) {
                selStart = start;
                Selection.setSelection(content, selStart, selEnd);
                i = ch;
            } else {
                i = 0;
            }
        }
        if (i != 0) {
            boolean dead = false;
            if ((Integer.MIN_VALUE & i) != 0) {
                dead = true;
                i &= Integer.MAX_VALUE;
            }
            if (activeStart == selStart && activeEnd == selEnd) {
                boolean replace = false;
                if ((selEnd - selStart) - 1 == 0) {
                    char accent = content.charAt(selStart);
                    int composed = KeyEvent.getDeadChar(accent, i);
                    if (composed != 0) {
                        i = composed;
                        replace = true;
                        dead = false;
                    }
                }
                if (!replace) {
                    Selection.setSelection(content, selEnd);
                    content.removeSpan(TextKeyListener.ACTIVE);
                    selStart = selEnd;
                }
            }
            if ((pref & 1) != 0 && Character.isLowerCase(i) && TextKeyListener.shouldCap(this.mAutoCap, content, selStart)) {
                int where = content.getSpanEnd(TextKeyListener.CAPPED);
                int flags = content.getSpanFlags(TextKeyListener.CAPPED);
                if (where == selStart && ((flags >> 16) & 65535) == i) {
                    content.removeSpan(TextKeyListener.CAPPED);
                } else {
                    int flags2 = i << 16;
                    i = Character.toUpperCase(i);
                    if (selStart == 0) {
                        content.setSpan(TextKeyListener.CAPPED, 0, 0, flags2 | 17);
                    } else {
                        content.setSpan(TextKeyListener.CAPPED, selStart - 1, selStart, flags2 | 33);
                    }
                }
            }
            if (selStart != selEnd) {
                Selection.setSelection(content, selEnd);
            }
            content.setSpan(OLD_SEL_START, selStart, selStart, 17);
            content.replace(selStart, selEnd, String.valueOf((char) i));
            int oldStart = content.getSpanStart(OLD_SEL_START);
            int selEnd2 = Selection.getSelectionEnd(content);
            if (oldStart < selEnd2) {
                content.setSpan(TextKeyListener.LAST_TYPED, oldStart, selEnd2, 33);
                if (dead) {
                    Selection.setSelection(content, oldStart, selEnd2);
                    content.setSpan(TextKeyListener.ACTIVE, oldStart, selEnd2, 33);
                }
            }
            adjustMetaAfterKeypress(content);
            if ((pref & 2) != 0 && this.mAutoText && ((i == 32 || i == 9 || i == 10 || i == 44 || i == 46 || i == 33 || i == 63 || i == 34 || Character.getType(i) == 22) && content.getSpanEnd(TextKeyListener.INHIBIT_REPLACEMENT) != oldStart)) {
                int x = oldStart;
                while (x > 0) {
                    char c2 = content.charAt(x - 1);
                    if (c2 != '\'' && !Character.isLetter(c2)) {
                        break;
                    }
                    x--;
                }
                String rep = getReplacement(content, x, oldStart, view);
                if (rep != null) {
                    for (Replaced replaced : (Replaced[]) content.getSpans(0, content.length(), Replaced.class)) {
                        content.removeSpan(replaced);
                    }
                    char[] orig = new char[oldStart - x];
                    TextUtils.getChars(content, x, oldStart, orig, 0);
                    content.setSpan(new Replaced(orig), x, oldStart, 33);
                    content.replace(x, oldStart, rep);
                }
            }
            if ((pref & 4) != 0 && this.mAutoText) {
                int selEnd3 = Selection.getSelectionEnd(content);
                if (selEnd3 - 3 >= 0 && content.charAt(selEnd3 - 1) == ' ' && content.charAt(selEnd3 - 2) == ' ') {
                    char c3 = content.charAt(selEnd3 - 3);
                    for (int j = selEnd3 - 3; j > 0 && (c3 == '\"' || Character.getType(c3) == 22); j--) {
                        c3 = content.charAt(j - 1);
                    }
                    if (Character.isLetter(c3) || Character.isDigit(c3)) {
                        content.replace(selEnd3 - 2, selEnd3 - 1, ".");
                    }
                }
            }
            return true;
        }
        if (keyCode == 67 && ((event.hasNoModifiers() || event.hasModifiers(2)) && selStart == selEnd)) {
            int consider = 1;
            if (content.getSpanEnd(TextKeyListener.LAST_TYPED) == selStart && content.charAt(selStart - 1) != '\n') {
                consider = 2;
            }
            Replaced[] repl = (Replaced[]) content.getSpans(selStart - consider, selStart, Replaced.class);
            if (repl.length > 0) {
                int st = content.getSpanStart(repl[0]);
                int en = content.getSpanEnd(repl[0]);
                String old = new String(repl[0].mText);
                content.removeSpan(repl[0]);
                if (selStart >= en) {
                    content.setSpan(TextKeyListener.INHIBIT_REPLACEMENT, en, en, 34);
                    content.replace(st, en, old);
                    int en2 = content.getSpanStart(TextKeyListener.INHIBIT_REPLACEMENT);
                    if (en2 - 1 >= 0) {
                        content.setSpan(TextKeyListener.INHIBIT_REPLACEMENT, en2 - 1, en2, 33);
                    } else {
                        content.removeSpan(TextKeyListener.INHIBIT_REPLACEMENT);
                    }
                    adjustMetaAfterKeypress(content);
                    return true;
                }
                adjustMetaAfterKeypress(content);
                return super.onKeyDown(view, content, keyCode, event);
            }
        }
        return super.onKeyDown(view, content, keyCode, event);
    }

    private String getReplacement(CharSequence src, int start, int end, View view) {
        String out;
        int len = end - start;
        boolean changecase = false;
        String replacement = AutoText.get(src, start, end, view);
        if (replacement == null) {
            String key = TextUtils.substring(src, start, end).toLowerCase();
            replacement = AutoText.get(key, 0, end - start, view);
            changecase = true;
            if (replacement == null) {
                return null;
            }
        }
        int caps = 0;
        if (changecase) {
            for (int j = start; j < end; j++) {
                if (Character.isUpperCase(src.charAt(j))) {
                    caps++;
                }
            }
        }
        if (caps == 0) {
            out = replacement;
        } else if (caps != 1 && caps == len) {
            out = replacement.toUpperCase();
        } else {
            out = toTitleCase(replacement);
        }
        if (out.length() == len && TextUtils.regionMatches(src, start, out, 0, len)) {
            return null;
        }
        return out;
    }

    public static void markAsReplaced(Spannable content, int start, int end, String original) {
        Replaced[] repl = (Replaced[]) content.getSpans(0, content.length(), Replaced.class);
        for (Replaced replaced : repl) {
            content.removeSpan(replaced);
        }
        int len = original.length();
        char[] orig = new char[len];
        original.getChars(0, len, orig, 0);
        content.setSpan(new Replaced(orig), start, end, 33);
    }

    private boolean showCharacterPicker(View view, Editable content, char c, boolean insert, int count) {
        String set = PICKER_SETS.get(c);
        if (set == null) {
            return false;
        }
        if (count == 1) {
            new CharacterPickerDialog(view.getContext(), view, content, set, insert).show();
        }
        return true;
    }

    private static String toTitleCase(String src) {
        return Character.toUpperCase(src.charAt(0)) + src.substring(1);
    }

    static class Replaced implements NoCopySpan {
        private char[] mText;

        public Replaced(char[] text) {
            this.mText = text;
        }
    }
}
