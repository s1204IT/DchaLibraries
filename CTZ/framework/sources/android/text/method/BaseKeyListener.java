package android.text.method;

import android.graphics.Paint;
import android.icu.lang.UCharacter;
import android.mtp.MtpConstants;
import android.text.Editable;
import android.text.Emoji;
import android.text.Layout;
import android.text.NoCopySpan;
import android.text.Selection;
import android.text.Spanned;
import android.text.method.TextKeyListener;
import android.text.style.ReplacementSpan;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import com.android.internal.annotations.GuardedBy;

public abstract class BaseKeyListener extends MetaKeyKeyListener implements KeyListener {
    private static final int CARRIAGE_RETURN = 13;
    private static final int LINE_FEED = 10;
    static final Object OLD_SEL_START = new NoCopySpan.Concrete();

    @GuardedBy("mLock")
    static Paint sCachedPaint = null;
    private final Object mLock = new Object();

    public boolean backspace(View view, Editable editable, int i, KeyEvent keyEvent) {
        return backspaceOrForwardDelete(view, editable, i, keyEvent, false);
    }

    public boolean forwardDelete(View view, Editable editable, int i, KeyEvent keyEvent) {
        return backspaceOrForwardDelete(view, editable, i, keyEvent, true);
    }

    private static boolean isVariationSelector(int i) {
        return UCharacter.hasBinaryProperty(i, 36);
    }

    private static int adjustReplacementSpan(CharSequence charSequence, int i, boolean z) {
        if (!(charSequence instanceof Spanned)) {
            return i;
        }
        Spanned spanned = (Spanned) charSequence;
        ReplacementSpan[] replacementSpanArr = (ReplacementSpan[]) spanned.getSpans(i, i, ReplacementSpan.class);
        for (int i2 = 0; i2 < replacementSpanArr.length; i2++) {
            int spanStart = spanned.getSpanStart(replacementSpanArr[i2]);
            int spanEnd = spanned.getSpanEnd(replacementSpanArr[i2]);
            if (spanStart < i && spanEnd > i) {
                if (!z) {
                    spanStart = spanEnd;
                }
                i = spanStart;
            }
        }
        return i;
    }

    private static int getOffsetForBackspaceKey(CharSequence charSequence, int i) {
        int i2;
        int i3;
        int iCharCount;
        int i4;
        if (i <= 1) {
            return 0;
        }
        int iCharCount2 = i;
        int i5 = 0;
        int iCharCount3 = 0;
        int i6 = 0;
        do {
            int iCodePointBefore = Character.codePointBefore(charSequence, iCharCount2);
            iCharCount2 -= Character.charCount(iCodePointBefore);
            int i7 = 4;
            switch (i5) {
                case 0:
                    int iCharCount4 = Character.charCount(iCodePointBefore);
                    if (iCodePointBefore == 10) {
                        iCharCount3 = iCharCount4;
                        i5 = 1;
                    } else {
                        if (isVariationSelector(iCodePointBefore)) {
                            i2 = 6;
                        } else if (Emoji.isRegionalIndicatorSymbol(iCodePointBefore)) {
                            iCharCount3 = iCharCount4;
                            i5 = 10;
                        } else if (Emoji.isEmojiModifier(iCodePointBefore)) {
                            iCharCount3 = iCharCount4;
                            i5 = i7;
                        } else if (iCodePointBefore == Emoji.COMBINING_ENCLOSING_KEYCAP) {
                            iCharCount3 = iCharCount4;
                            i5 = 2;
                        } else if (Emoji.isEmoji(iCodePointBefore)) {
                            iCharCount3 = iCharCount4;
                            i5 = 7;
                        } else if (iCodePointBefore == Emoji.CANCEL_TAG) {
                            i2 = 12;
                        } else {
                            iCharCount3 = iCharCount4;
                        }
                        i3 = i2;
                        iCharCount3 = iCharCount4;
                        i5 = i3;
                    }
                    if (iCharCount2 > 0) {
                    }
                    return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                case 1:
                    if (iCodePointBefore == 13) {
                        iCharCount3++;
                    }
                    if (iCharCount2 > 0) {
                    }
                    return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                case 2:
                    if (!isVariationSelector(iCodePointBefore)) {
                        if (Emoji.isKeycapBase(iCodePointBefore)) {
                            iCharCount3 += Character.charCount(iCodePointBefore);
                        }
                        if (iCharCount2 > 0) {
                        }
                        return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                    }
                    iCharCount = Character.charCount(iCodePointBefore);
                    i4 = 3;
                    i3 = i4;
                    i6 = iCharCount;
                    i5 = i3;
                    if (iCharCount2 > 0) {
                    }
                    return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                case 3:
                    if (Emoji.isKeycapBase(iCodePointBefore)) {
                        iCharCount3 += Character.charCount(iCodePointBefore) + i6;
                    }
                    if (iCharCount2 > 0) {
                    }
                    return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                case 4:
                    if (!isVariationSelector(iCodePointBefore)) {
                        if (Emoji.isEmojiModifierBase(iCodePointBefore)) {
                            iCharCount3 += Character.charCount(iCodePointBefore);
                        }
                        if (iCharCount2 > 0) {
                        }
                        return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                    }
                    iCharCount = Character.charCount(iCodePointBefore);
                    i4 = 5;
                    i3 = i4;
                    i6 = iCharCount;
                    i5 = i3;
                    if (iCharCount2 > 0) {
                    }
                    return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                case 5:
                    if (Emoji.isEmojiModifierBase(iCodePointBefore)) {
                        iCharCount3 += Character.charCount(iCodePointBefore) + i6;
                    }
                    if (iCharCount2 > 0) {
                    }
                    return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                case 6:
                    if (Emoji.isEmoji(iCodePointBefore)) {
                        iCharCount3 += Character.charCount(iCodePointBefore);
                        i5 = 7;
                        if (iCharCount2 > 0) {
                        }
                        return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                    }
                    if (!isVariationSelector(iCodePointBefore) && UCharacter.getCombiningClass(iCodePointBefore) == 0) {
                        iCharCount3 += Character.charCount(iCodePointBefore);
                    }
                    if (iCharCount2 > 0) {
                    }
                    return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                case 7:
                    i5 = iCodePointBefore == Emoji.ZERO_WIDTH_JOINER ? 8 : 13;
                    if (iCharCount2 > 0) {
                    }
                    return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                case 8:
                    if (Emoji.isEmoji(iCodePointBefore)) {
                        iCharCount3 += Character.charCount(iCodePointBefore) + 1;
                        if (!Emoji.isEmojiModifier(iCodePointBefore)) {
                            i7 = 7;
                        }
                        i5 = i7;
                        if (iCharCount2 > 0) {
                        }
                        return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                    }
                    if (isVariationSelector(iCodePointBefore)) {
                        iCharCount = Character.charCount(iCodePointBefore);
                        i4 = 9;
                        i3 = i4;
                        i6 = iCharCount;
                        i5 = i3;
                        if (iCharCount2 > 0) {
                        }
                        return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                    }
                    if (iCharCount2 > 0) {
                    }
                    return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                case 9:
                    if (Emoji.isEmoji(iCodePointBefore)) {
                        iCharCount3 += i6 + 1 + Character.charCount(iCodePointBefore);
                        i6 = 0;
                        i5 = 7;
                        if (iCharCount2 > 0) {
                        }
                        return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                    }
                    if (iCharCount2 > 0) {
                    }
                    return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                case 10:
                    if (Emoji.isRegionalIndicatorSymbol(iCodePointBefore)) {
                        iCharCount3 += 2;
                        i5 = 11;
                    }
                    if (iCharCount2 > 0) {
                    }
                    return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                case 11:
                    if (Emoji.isRegionalIndicatorSymbol(iCodePointBefore)) {
                        iCharCount3 -= 2;
                        i5 = 10;
                        if (iCharCount2 > 0) {
                        }
                        return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                    }
                    if (iCharCount2 > 0) {
                    }
                    return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                case 12:
                    if (Emoji.isTagSpecChar(iCodePointBefore)) {
                        iCharCount3 += 2;
                        if (iCharCount2 > 0) {
                        }
                        return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                    }
                    iCharCount3 = Emoji.isEmoji(iCodePointBefore) ? iCharCount3 + Character.charCount(iCodePointBefore) : 2;
                    if (iCharCount2 > 0) {
                    }
                    return adjustReplacementSpan(charSequence, i - iCharCount3, true);
                default:
                    throw new IllegalArgumentException("state " + i5 + " is unknown");
            }
        } while (i5 != 13);
        return adjustReplacementSpan(charSequence, i - iCharCount3, true);
    }

    private static int getOffsetForForwardDeleteKey(CharSequence charSequence, int i, Paint paint) {
        int length = charSequence.length();
        if (i >= length - 1) {
            return length;
        }
        return adjustReplacementSpan(charSequence, paint.getTextRunCursor(charSequence, i, length, 0, i, 0), false);
    }

    private boolean backspaceOrForwardDelete(View view, Editable editable, int i, KeyEvent keyEvent, boolean z) {
        int offsetForBackspaceKey;
        Paint paint;
        Paint paint2;
        if (!KeyEvent.metaStateHasNoModifiers(keyEvent.getMetaState() & (-28916))) {
            return false;
        }
        if (deleteSelection(view, editable)) {
            return true;
        }
        boolean z2 = (keyEvent.getMetaState() & 4096) != 0;
        boolean z3 = getMetaState(editable, 1, keyEvent) == 1;
        boolean z4 = getMetaState(editable, 2, keyEvent) == 1;
        if (z2) {
            if (z4 || z3) {
                return false;
            }
            return deleteUntilWordBoundary(view, editable, z);
        }
        if (z4 && deleteLine(view, editable)) {
            return true;
        }
        int selectionEnd = Selection.getSelectionEnd(editable);
        if (z) {
            if (view instanceof TextView) {
                paint2 = ((TextView) view).getPaint();
            } else {
                synchronized (this.mLock) {
                    if (sCachedPaint == null) {
                        sCachedPaint = new Paint();
                    }
                    paint = sCachedPaint;
                }
                paint2 = paint;
            }
            offsetForBackspaceKey = getOffsetForForwardDeleteKey(editable, selectionEnd, paint2);
        } else {
            offsetForBackspaceKey = getOffsetForBackspaceKey(editable, selectionEnd);
        }
        if (selectionEnd == offsetForBackspaceKey) {
            return false;
        }
        editable.delete(Math.min(selectionEnd, offsetForBackspaceKey), Math.max(selectionEnd, offsetForBackspaceKey));
        return true;
    }

    private boolean deleteUntilWordBoundary(View view, Editable editable, boolean z) {
        int length;
        int selectionStart = Selection.getSelectionStart(editable);
        if (selectionStart != Selection.getSelectionEnd(editable)) {
            return false;
        }
        if ((!z && selectionStart == 0) || (z && selectionStart == editable.length())) {
            return false;
        }
        WordIterator wordIterator = null;
        if (view instanceof TextView) {
            wordIterator = ((TextView) view).getWordIterator();
        }
        if (wordIterator == null) {
            wordIterator = new WordIterator();
        }
        if (z) {
            wordIterator.setCharSequence(editable, selectionStart, editable.length());
            int iFollowing = wordIterator.following(selectionStart);
            length = iFollowing == -1 ? editable.length() : iFollowing;
        } else {
            wordIterator.setCharSequence(editable, 0, selectionStart);
            int iPreceding = wordIterator.preceding(selectionStart);
            if (iPreceding == -1) {
                length = selectionStart;
                selectionStart = 0;
            } else {
                length = selectionStart;
                selectionStart = iPreceding;
            }
        }
        editable.delete(selectionStart, length);
        return true;
    }

    private boolean deleteSelection(View view, Editable editable) {
        int selectionStart = Selection.getSelectionStart(editable);
        int selectionEnd = Selection.getSelectionEnd(editable);
        if (selectionEnd < selectionStart) {
            selectionEnd = selectionStart;
            selectionStart = selectionEnd;
        }
        if (selectionStart != selectionEnd) {
            editable.delete(selectionStart, selectionEnd);
            return true;
        }
        return false;
    }

    private boolean deleteLine(View view, Editable editable) {
        Layout layout;
        int lineForOffset;
        int lineStart;
        int lineEnd;
        if ((view instanceof TextView) && (layout = ((TextView) view).getLayout()) != null && (lineEnd = layout.getLineEnd(lineForOffset)) != (lineStart = layout.getLineStart((lineForOffset = layout.getLineForOffset(Selection.getSelectionStart(editable)))))) {
            editable.delete(lineStart, lineEnd);
            return true;
        }
        return false;
    }

    static int makeTextContentType(TextKeyListener.Capitalize capitalize, boolean z) {
        int i;
        switch (capitalize) {
            case CHARACTERS:
                i = 4097;
                break;
            case WORDS:
                i = MtpConstants.RESPONSE_OK;
                break;
            case SENTENCES:
                i = 16385;
                break;
            default:
                i = 1;
                break;
        }
        if (z) {
            return i | 32768;
        }
        return i;
    }

    @Override
    public boolean onKeyDown(View view, Editable editable, int i, KeyEvent keyEvent) {
        boolean zBackspace;
        if (i == 67) {
            zBackspace = backspace(view, editable, i, keyEvent);
        } else if (i == 112) {
            zBackspace = forwardDelete(view, editable, i, keyEvent);
        } else {
            zBackspace = false;
        }
        if (zBackspace) {
            adjustMetaAfterKeypress(editable);
            return true;
        }
        return super.onKeyDown(view, editable, i, keyEvent);
    }

    @Override
    public boolean onKeyOther(View view, Editable editable, KeyEvent keyEvent) {
        if (keyEvent.getAction() != 2 || keyEvent.getKeyCode() != 0) {
            return false;
        }
        int selectionStart = Selection.getSelectionStart(editable);
        int selectionEnd = Selection.getSelectionEnd(editable);
        if (selectionEnd < selectionStart) {
            selectionEnd = selectionStart;
            selectionStart = selectionEnd;
        }
        String characters = keyEvent.getCharacters();
        if (characters == null) {
            return false;
        }
        editable.replace(selectionStart, selectionEnd, characters);
        return true;
    }
}
