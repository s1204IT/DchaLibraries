package android.view.inputmethod;

import android.content.Context;
import android.content.res.TypedArray;
import android.net.ProxyInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.MetaKeyKeyListener;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewRootImpl;
import com.android.internal.R;

public class BaseInputConnection implements InputConnection {
    static final Object COMPOSING = new ComposingText();
    private static final boolean DEBUG = false;
    private static final String TAG = "BaseInputConnection";
    private Object[] mDefaultComposingSpans;
    final boolean mDummyMode;
    Editable mEditable;
    protected final InputMethodManager mIMM;
    KeyCharacterMap mKeyCharacterMap;
    final View mTargetView;

    BaseInputConnection(InputMethodManager mgr, boolean fullEditor) {
        this.mIMM = mgr;
        this.mTargetView = null;
        this.mDummyMode = !fullEditor;
    }

    public BaseInputConnection(View targetView, boolean fullEditor) {
        this.mIMM = (InputMethodManager) targetView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        this.mTargetView = targetView;
        this.mDummyMode = !fullEditor;
    }

    public static final void removeComposingSpans(Spannable text) {
        text.removeSpan(COMPOSING);
        Object[] sps = text.getSpans(0, text.length(), Object.class);
        if (sps != null) {
            for (int i = sps.length - 1; i >= 0; i--) {
                Object o = sps[i];
                if ((text.getSpanFlags(o) & 256) != 0) {
                    text.removeSpan(o);
                }
            }
        }
    }

    public static void setComposingSpans(Spannable text) {
        setComposingSpans(text, 0, text.length());
    }

    public static void setComposingSpans(Spannable text, int start, int end) {
        Object[] sps = text.getSpans(start, end, Object.class);
        if (sps != null) {
            for (int i = sps.length - 1; i >= 0; i--) {
                Object o = sps[i];
                if (o == COMPOSING) {
                    text.removeSpan(o);
                } else {
                    int fl = text.getSpanFlags(o);
                    if ((fl & R.styleable.Theme_colorSwitchThumbNormal) != 289) {
                        text.setSpan(o, text.getSpanStart(o), text.getSpanEnd(o), (fl & (-52)) | 256 | 33);
                    }
                }
            }
        }
        text.setSpan(COMPOSING, start, end, R.styleable.Theme_preferenceActivityStyle);
    }

    public static int getComposingSpanStart(Spannable text) {
        return text.getSpanStart(COMPOSING);
    }

    public static int getComposingSpanEnd(Spannable text) {
        return text.getSpanEnd(COMPOSING);
    }

    public Editable getEditable() {
        if (this.mEditable == null) {
            this.mEditable = Editable.Factory.getInstance().newEditable(ProxyInfo.LOCAL_EXCL_LIST);
            Selection.setSelection(this.mEditable, 0);
        }
        return this.mEditable;
    }

    @Override
    public boolean beginBatchEdit() {
        return false;
    }

    @Override
    public boolean endBatchEdit() {
        return false;
    }

    protected void reportFinish() {
    }

    @Override
    public boolean clearMetaKeyStates(int states) {
        Editable content = getEditable();
        if (content == null) {
            return false;
        }
        MetaKeyKeyListener.clearMetaKeyState(content, states);
        return true;
    }

    @Override
    public boolean commitCompletion(CompletionInfo text) {
        return false;
    }

    @Override
    public boolean commitCorrection(CorrectionInfo correctionInfo) {
        return false;
    }

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        replaceText(text, newCursorPosition, false);
        this.mIMM.notifyUserAction();
        sendCurrentText();
        return true;
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        Editable content = getEditable();
        if (content == null) {
            return false;
        }
        beginBatchEdit();
        int a = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);
        if (a > b) {
            a = b;
            b = a;
        }
        int ca = getComposingSpanStart(content);
        int cb = getComposingSpanEnd(content);
        if (cb < ca) {
            ca = cb;
            cb = ca;
        }
        if (ca != -1 && cb != -1) {
            if (ca < a) {
                a = ca;
            }
            if (cb > b) {
                b = cb;
            }
        }
        int deleted = 0;
        if (beforeLength > 0) {
            int start = a - beforeLength;
            if (start < 0) {
                start = 0;
            }
            content.delete(start, a);
            deleted = a - start;
        }
        if (afterLength > 0) {
            int b2 = b - deleted;
            int end = b2 + afterLength;
            if (end > content.length()) {
                end = content.length();
            }
            content.delete(b2, end);
        }
        endBatchEdit();
        return true;
    }

    @Override
    public boolean finishComposingText() {
        Editable content = getEditable();
        if (content != null) {
            beginBatchEdit();
            removeComposingSpans(content);
            sendCurrentText();
            endBatchEdit();
            return true;
        }
        return true;
    }

    @Override
    public int getCursorCapsMode(int reqModes) {
        Editable content;
        if (this.mDummyMode || (content = getEditable()) == null) {
            return 0;
        }
        int a = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);
        if (a > b) {
            a = b;
        }
        return TextUtils.getCapsMode(content, a, reqModes);
    }

    @Override
    public ExtractedText getExtractedText(ExtractedTextRequest request, int flags) {
        return null;
    }

    @Override
    public CharSequence getTextBeforeCursor(int length, int flags) {
        Editable content = getEditable();
        if (content == null) {
            return null;
        }
        int a = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);
        if (a > b) {
            a = b;
        }
        if (a <= 0) {
            return ProxyInfo.LOCAL_EXCL_LIST;
        }
        if (length > a) {
            length = a;
        }
        if ((flags & 1) != 0) {
            return content.subSequence(a - length, a);
        }
        return TextUtils.substring(content, a - length, a);
    }

    @Override
    public CharSequence getSelectedText(int flags) {
        Editable content = getEditable();
        if (content == null) {
            return null;
        }
        int a = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);
        if (a > b) {
            a = b;
            b = a;
        }
        if (a == b) {
            return null;
        }
        if ((flags & 1) != 0) {
            return content.subSequence(a, b);
        }
        return TextUtils.substring(content, a, b);
    }

    @Override
    public CharSequence getTextAfterCursor(int length, int flags) {
        Editable content = getEditable();
        if (content == null) {
            return null;
        }
        int a = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);
        if (a > b) {
            b = a;
        }
        if (b < 0) {
            b = 0;
        }
        if (b + length > content.length()) {
            length = content.length() - b;
        }
        if ((flags & 1) != 0) {
            return content.subSequence(b, b + length);
        }
        return TextUtils.substring(content, b, b + length);
    }

    @Override
    public boolean performEditorAction(int actionCode) {
        long eventTime = SystemClock.uptimeMillis();
        sendKeyEvent(new KeyEvent(eventTime, eventTime, 0, 66, 0, 0, -1, 0, 22));
        sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), eventTime, 1, 66, 0, 0, -1, 0, 22));
        return true;
    }

    @Override
    public boolean performContextMenuAction(int id) {
        return false;
    }

    @Override
    public boolean performPrivateCommand(String action, Bundle data) {
        return false;
    }

    @Override
    public boolean requestCursorUpdates(int cursorUpdateMode) {
        return false;
    }

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        replaceText(text, newCursorPosition, true);
        this.mIMM.notifyUserAction();
        return true;
    }

    @Override
    public boolean setComposingRegion(int start, int end) {
        Editable content = getEditable();
        if (content != null) {
            beginBatchEdit();
            removeComposingSpans(content);
            int a = start;
            int b = end;
            if (a > b) {
                a = b;
                b = a;
            }
            int length = content.length();
            if (a < 0) {
                a = 0;
            }
            if (b < 0) {
                b = 0;
            }
            if (a > length) {
                a = length;
            }
            if (b > length) {
                b = length;
            }
            ensureDefaultComposingSpans();
            if (this.mDefaultComposingSpans != null) {
                for (int i = 0; i < this.mDefaultComposingSpans.length; i++) {
                    content.setSpan(this.mDefaultComposingSpans[i], a, b, R.styleable.Theme_preferenceActivityStyle);
                }
            }
            content.setSpan(COMPOSING, a, b, R.styleable.Theme_preferenceActivityStyle);
            sendCurrentText();
            endBatchEdit();
            return true;
        }
        return true;
    }

    @Override
    public boolean setSelection(int start, int end) {
        Editable content = getEditable();
        if (content == null) {
            return false;
        }
        int len = content.length();
        if (start > len || end > len || start < 0 || end < 0) {
            return true;
        }
        if (start == end && MetaKeyKeyListener.getMetaState(content, 2048) != 0) {
            Selection.extendSelection(content, start);
            return true;
        }
        Selection.setSelection(content, start, end);
        return true;
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        synchronized (this.mIMM.mH) {
            ViewRootImpl viewRootImpl = this.mTargetView != null ? this.mTargetView.getViewRootImpl() : null;
            if (viewRootImpl == null && this.mIMM.mServedView != null) {
                viewRootImpl = this.mIMM.mServedView.getViewRootImpl();
            }
            if (viewRootImpl != null) {
                viewRootImpl.dispatchKeyFromIme(event);
            }
        }
        this.mIMM.notifyUserAction();
        return false;
    }

    @Override
    public boolean reportFullscreenMode(boolean enabled) {
        this.mIMM.setFullscreenMode(enabled);
        return true;
    }

    private void sendCurrentText() {
        Editable content;
        int N;
        if (this.mDummyMode && (content = getEditable()) != null && (N = content.length()) != 0) {
            if (N == 1) {
                if (this.mKeyCharacterMap == null) {
                    this.mKeyCharacterMap = KeyCharacterMap.load(-1);
                }
                char[] chars = new char[1];
                content.getChars(0, 1, chars, 0);
                KeyEvent[] events = this.mKeyCharacterMap.getEvents(chars);
                if (events != null) {
                    for (KeyEvent keyEvent : events) {
                        sendKeyEvent(keyEvent);
                    }
                    content.clear();
                    return;
                }
            }
            KeyEvent event = new KeyEvent(SystemClock.uptimeMillis(), content.toString(), -1, 0);
            sendKeyEvent(event);
            content.clear();
        }
    }

    private void ensureDefaultComposingSpans() {
        Context context;
        if (this.mDefaultComposingSpans == null) {
            if (this.mTargetView != null) {
                context = this.mTargetView.getContext();
            } else if (this.mIMM.mServedView != null) {
                context = this.mIMM.mServedView.getContext();
            } else {
                context = null;
            }
            if (context != null) {
                TypedArray ta = context.getTheme().obtainStyledAttributes(new int[]{16843312});
                CharSequence style = ta.getText(0);
                ta.recycle();
                if (style != null && (style instanceof Spanned)) {
                    this.mDefaultComposingSpans = ((Spanned) style).getSpans(0, style.length(), Object.class);
                }
            }
        }
    }

    private void replaceText(CharSequence text, int newCursorPosition, boolean composing) {
        int newCursorPosition2;
        Spannable sp;
        Editable content = getEditable();
        if (content != null) {
            beginBatchEdit();
            int a = getComposingSpanStart(content);
            int b = getComposingSpanEnd(content);
            if (b < a) {
                a = b;
                b = a;
            }
            if (a != -1 && b != -1) {
                removeComposingSpans(content);
            } else {
                a = Selection.getSelectionStart(content);
                b = Selection.getSelectionEnd(content);
                if (a < 0) {
                    a = 0;
                }
                if (b < 0) {
                    b = 0;
                }
                if (b < a) {
                    int tmp = a;
                    a = b;
                    b = tmp;
                }
            }
            if (composing) {
                if (!(text instanceof Spannable)) {
                    sp = new SpannableStringBuilder(text);
                    text = sp;
                    ensureDefaultComposingSpans();
                    if (this.mDefaultComposingSpans != null) {
                        for (int i = 0; i < this.mDefaultComposingSpans.length; i++) {
                            sp.setSpan(this.mDefaultComposingSpans[i], 0, sp.length(), R.styleable.Theme_preferenceActivityStyle);
                        }
                    }
                } else {
                    sp = (Spannable) text;
                }
                setComposingSpans(sp);
            }
            if (newCursorPosition > 0) {
                newCursorPosition2 = newCursorPosition + (b - 1);
            } else {
                newCursorPosition2 = newCursorPosition + a;
            }
            if (newCursorPosition2 < 0) {
                newCursorPosition2 = 0;
            }
            if (newCursorPosition2 > content.length()) {
                newCursorPosition2 = content.length();
            }
            Selection.setSelection(content, newCursorPosition2);
            content.replace(a, b, text);
            endBatchEdit();
        }
    }
}
