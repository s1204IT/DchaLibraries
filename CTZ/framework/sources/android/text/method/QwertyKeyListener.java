package android.text.method;

import android.text.AutoText;
import android.text.Editable;
import android.text.NoCopySpan;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.SparseArray;
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

    private QwertyKeyListener(TextKeyListener.Capitalize capitalize, boolean z, boolean z2) {
        this.mAutoCap = capitalize;
        this.mAutoText = z;
        this.mFullKeyboard = z2;
    }

    public QwertyKeyListener(TextKeyListener.Capitalize capitalize, boolean z) {
        this(capitalize, z, false);
    }

    public static QwertyKeyListener getInstance(boolean z, TextKeyListener.Capitalize capitalize) {
        int iOrdinal = (capitalize.ordinal() * 2) + (z ? 1 : 0);
        if (sInstance[iOrdinal] == null) {
            sInstance[iOrdinal] = new QwertyKeyListener(capitalize, z);
        }
        return sInstance[iOrdinal];
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
    public boolean onKeyDown(android.view.View r20, android.text.Editable r21, int r22, android.view.KeyEvent r23) {
        if (r20 == null) {
            r12 = 0;
        } else {
            r12 = android.text.method.TextKeyListener.getInstance().getPrefs(r20.getContext());
        }
        r0 = android.text.Selection.getSelectionStart(r21);
        r1 = android.text.Selection.getSelectionEnd(r21);
        r2 = java.lang.Math.min(r0, r1);
        r0 = java.lang.Math.max(r0, r1);
        if (r2 < 0 || r0 < 0) {
            android.text.Selection.setSelection(r21, 0, 0);
            r13 = 0;
            r14 = 0;
        } else {
            r14 = r0;
            r13 = r2;
        }
        r15 = r21.getSpanStart(android.text.method.TextKeyListener.ACTIVE);
        r6 = r21.getSpanEnd(android.text.method.TextKeyListener.ACTIVE);
        r0 = r23.getUnicodeChar(getMetaState(r21, r23));
        if (!r19.mFullKeyboard && (r17 = r23.getRepeatCount()) > 0 && r13 == r14 && r13 > 0 && (((r4 = r21.charAt(r13 - 1)) == r0 || r4 == java.lang.Character.toUpperCase(r0)) && r20 != null)) {
            r11 = r6;
            if (showCharacterPicker(r20, r21, r4, false, r17)) {
                resetMetaState(r21);
                return true;
            }
        } else {
            r11 = r6;
        }
        if (r0 == 61185) {
            if (r20 != null) {
                showCharacterPicker(r20, r21, android.view.KeyCharacterMap.PICKER_DIALOG_INPUT, true, 1);
            }
            resetMetaState(r21);
            return true;
        }
        if (r0 == 61184) {
            if (r13 == r14) {
                r0 = r14;
                while (r0 > 0 && r14 - r0 < 4 && java.lang.Character.digit(r21.charAt(r0 - 1), 16) >= 0) {
                    r0 = r0 - 1;
                }
                r1 = r0;
            } else {
                r1 = r13;
            }
            try {
                r0 = java.lang.Integer.parseInt(android.text.TextUtils.substring(r21, r1, r14), 16);
            } catch (java.lang.NumberFormatException e) {
                r0 = -1;
            }
            if (r0 >= 0) {
                android.text.Selection.setSelection(r21, r1, r14);
                r13 = r1;
            } else {
                r0 = 0;
            }
        }
        if (r0 != 0) {
            if ((Integer.MIN_VALUE & r0) != 0) {
                r0 = r0 & Integer.MAX_VALUE;
                r10 = true;
            } else {
                r10 = false;
            }
            if (r15 == r13 && r11 == r14) {
                if ((r14 - r13) - 1 != 0 || (r11 = android.view.KeyEvent.getDeadChar(r21.charAt(r13), r0)) == 0) {
                    r11 = r0;
                    r0 = false;
                } else {
                    r0 = true;
                    r10 = false;
                }
                if (!r0) {
                    android.text.Selection.setSelection(r21, r14);
                    r21.removeSpan(android.text.method.TextKeyListener.ACTIVE);
                    r13 = r14;
                }
            } else {
                r11 = r0;
            }
            if ((r12 & 1) != 0 && java.lang.Character.isLowerCase(r11) && android.text.method.TextKeyListener.shouldCap(r19.mAutoCap, r21, r13)) {
                r0 = r21.getSpanEnd(android.text.method.TextKeyListener.CAPPED);
                r5 = r21.getSpanFlags(android.text.method.TextKeyListener.CAPPED);
                if (r0 == r13 && ((r5 >> 16) & 65535) == r11) {
                    r21.removeSpan(android.text.method.TextKeyListener.CAPPED);
                } else {
                    r0 = r11 << 16;
                    r11 = java.lang.Character.toUpperCase(r11);
                    if (r13 == 0) {
                        r21.setSpan(android.text.method.TextKeyListener.CAPPED, 0, 0, r0 | 17);
                    } else {
                        r21.setSpan(android.text.method.TextKeyListener.CAPPED, r13 - 1, r13, r0 | 33);
                    }
                }
            }
            if (r13 != r14) {
                android.text.Selection.setSelection(r21, r14);
            }
            r21.setSpan(android.text.method.QwertyKeyListener.OLD_SEL_START, r13, r13, 17);
            r21.replace(r13, r14, java.lang.String.valueOf((char) r11));
            r0 = r21.getSpanStart(android.text.method.QwertyKeyListener.OLD_SEL_START);
            r3 = android.text.Selection.getSelectionEnd(r21);
            if (r0 < r3) {
                r21.setSpan(android.text.method.TextKeyListener.LAST_TYPED, r0, r3, 33);
                if (r10) {
                    android.text.Selection.setSelection(r21, r0, r3);
                    r21.setSpan(android.text.method.TextKeyListener.ACTIVE, r0, r3, 33);
                }
            }
            adjustMetaAfterKeypress(r21);
            if ((r12 & 2) != 0 && r19.mAutoText && ((r11 == 32 || r11 == 9 || r11 == 10 || r11 == 44 || r11 == 46 || r11 == 33 || r11 == 63 || r11 == 34 || java.lang.Character.getType(r11) == 22) && r21.getSpanEnd(android.text.method.TextKeyListener.INHIBIT_REPLACEMENT) != r0)) {
                r1 = r0;
                while (r1 > 0) {
                    r3 = r21.charAt(r1 - 1);
                    if (r3 != '\'' && !java.lang.Character.isLetter(r3)) {
                        break;
                    }
                    r1 = r1 - 1;
                }
                r3 = getReplacement(r21, r1, r0, r20);
                if (r3 != null) {
                    r8 = (android.text.method.QwertyKeyListener.Replaced[]) r21.getSpans(0, r21.length(), android.text.method.QwertyKeyListener.Replaced.class);
                    r11 = 0;
                    while (r11 < r8.length) {
                        r21.removeSpan(r8[r11]);
                        r11 = r11 + 1;
                    }
                    r8 = new char[r0 - r1];
                    android.text.TextUtils.getChars(r21, r1, r0, r8, 0);
                    r21.setSpan(new android.text.method.QwertyKeyListener.Replaced(r8), r1, r0, 33);
                    r21.replace(r1, r0, r3);
                }
            }
            if ((r12 & 4) != 0 && r19.mAutoText && (r0 = android.text.Selection.getSelectionEnd(r21)) - 3 >= 0) {
                r2 = r0 + (-1);
                if (r21.charAt(r2) == ' ') {
                    r0 = r0 - 2;
                    if (r21.charAt(r0) == ' ') {
                        r3 = r21.charAt(r1);
                        while (r1 > 0 && (r3 == '\"' || java.lang.Character.getType(r3) == 22)) {
                            r3 = r21.charAt(r1 + (-1));
                            r1 = r1 + (-1);
                        }
                        if (java.lang.Character.isLetter(r3) || java.lang.Character.isDigit(r3)) {
                            r21.replace(r0, r2, ".");
                        }
                    }
                }
            }
            return true;
        }
        if (r22 == 67) {
            if (r23.hasNoModifiers()) {
                r3 = 2;
            } else {
                r3 = 2;
                if (r23.hasModifiers(2)) {
                }
            }
            if (r13 == r14) {
                if (r21.getSpanEnd(android.text.method.TextKeyListener.LAST_TYPED) != r13 || r21.charAt(r13 + (-1)) == '\n') {
                    r3 = 1;
                }
                r0 = (android.text.method.QwertyKeyListener.Replaced[]) r21.getSpans(r13 - r3, r13, android.text.method.QwertyKeyListener.Replaced.class);
                if (r0.length > 0) {
                    r3 = r21.getSpanStart(r0[0]);
                    r5 = r21.getSpanEnd(r0[0]);
                    r11 = new java.lang.String(r0[0].mText);
                    r21.removeSpan(r0[0]);
                    if (r13 >= r5) {
                        r21.setSpan(android.text.method.TextKeyListener.INHIBIT_REPLACEMENT, r5, r5, 34);
                        r21.replace(r3, r5, r11);
                        r0 = r21.getSpanStart(android.text.method.TextKeyListener.INHIBIT_REPLACEMENT);
                        r1 = r0 + (-1);
                        if (r1 >= 0) {
                            r21.setSpan(android.text.method.TextKeyListener.INHIBIT_REPLACEMENT, r1, r0, 33);
                        } else {
                            r21.removeSpan(android.text.method.TextKeyListener.INHIBIT_REPLACEMENT);
                        }
                        adjustMetaAfterKeypress(r21);
                        return true;
                    }
                    adjustMetaAfterKeypress(r21);
                    return super.onKeyDown(r20, r21, r22, r23);
                }
            }
        }
        return super.onKeyDown(r20, r21, r22, r23);
    }

    private String getReplacement(CharSequence charSequence, int i, int i2, View view) {
        boolean z;
        int i3;
        int i4 = i2 - i;
        String titleCase = AutoText.get(charSequence, i, i2, view);
        if (titleCase == null) {
            titleCase = AutoText.get(TextUtils.substring(charSequence, i, i2).toLowerCase(), 0, i4, view);
            if (titleCase == null) {
                return null;
            }
            z = true;
        } else {
            z = false;
        }
        if (z) {
            i3 = 0;
            for (int i5 = i; i5 < i2; i5++) {
                if (Character.isUpperCase(charSequence.charAt(i5))) {
                    i3++;
                }
            }
        } else {
            i3 = 0;
        }
        if (i3 != 0) {
            if (i3 != 1 && i3 == i4) {
                titleCase = titleCase.toUpperCase();
            } else {
                titleCase = toTitleCase(titleCase);
            }
        }
        if (titleCase.length() == i4 && TextUtils.regionMatches(charSequence, i, titleCase, 0, i4)) {
            return null;
        }
        return titleCase;
    }

    public static void markAsReplaced(Spannable spannable, int i, int i2, String str) {
        for (Replaced replaced : (Replaced[]) spannable.getSpans(0, spannable.length(), Replaced.class)) {
            spannable.removeSpan(replaced);
        }
        int length = str.length();
        char[] cArr = new char[length];
        str.getChars(0, length, cArr, 0);
        spannable.setSpan(new Replaced(cArr), i, i2, 33);
    }

    private boolean showCharacterPicker(View view, Editable editable, char c, boolean z, int i) {
        String str = PICKER_SETS.get(c);
        if (str == null) {
            return false;
        }
        if (i == 1) {
            new CharacterPickerDialog(view.getContext(), view, editable, str, z).show();
        }
        return true;
    }

    private static String toTitleCase(String str) {
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    static class Replaced implements NoCopySpan {
        private char[] mText;

        public Replaced(char[] cArr) {
            this.mText = cArr;
        }
    }
}
