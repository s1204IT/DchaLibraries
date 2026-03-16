package android.text.method;

import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.method.TextKeyListener;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;

public class MultiTapKeyListener extends BaseKeyListener implements SpanWatcher {
    private static MultiTapKeyListener[] sInstance = new MultiTapKeyListener[TextKeyListener.Capitalize.values().length * 2];
    private static final SparseArray<String> sRecs = new SparseArray<>();
    private boolean mAutoText;
    private TextKeyListener.Capitalize mCapitalize;

    static {
        sRecs.put(8, ".,1!@#$%^&*:/?'=()");
        sRecs.put(9, "abc2ABC");
        sRecs.put(10, "def3DEF");
        sRecs.put(11, "ghi4GHI");
        sRecs.put(12, "jkl5JKL");
        sRecs.put(13, "mno6MNO");
        sRecs.put(14, "pqrs7PQRS");
        sRecs.put(15, "tuv8TUV");
        sRecs.put(16, "wxyz9WXYZ");
        sRecs.put(7, "0+");
        sRecs.put(18, " ");
    }

    public MultiTapKeyListener(TextKeyListener.Capitalize cap, boolean autotext) {
        this.mCapitalize = cap;
        this.mAutoText = autotext;
    }

    public static MultiTapKeyListener getInstance(boolean autotext, TextKeyListener.Capitalize cap) {
        int off = (cap.ordinal() * 2) + (autotext ? 1 : 0);
        if (sInstance[off] == null) {
            sInstance[off] = new MultiTapKeyListener(cap, autotext);
        }
        return sInstance[off];
    }

    @Override
    public int getInputType() {
        return makeTextContentType(this.mCapitalize, this.mAutoText);
    }

    @Override
    public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event) {
        int rec;
        int pref = 0;
        if (view != null) {
            pref = TextKeyListener.getInstance().getPrefs(view.getContext());
        }
        int a = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);
        int selStart = Math.min(a, b);
        int selEnd = Math.max(a, b);
        int activeStart = content.getSpanStart(TextKeyListener.ACTIVE);
        int activeEnd = content.getSpanEnd(TextKeyListener.ACTIVE);
        int rec2 = (content.getSpanFlags(TextKeyListener.ACTIVE) & (-16777216)) >>> 24;
        if (activeStart == selStart && activeEnd == selEnd && selEnd - selStart == 1 && rec2 >= 0 && rec2 < sRecs.size()) {
            if (keyCode == 17) {
                char current = content.charAt(selStart);
                if (Character.isLowerCase(current)) {
                    content.replace(selStart, selEnd, String.valueOf(current).toUpperCase());
                    removeTimeouts(content);
                    new Timeout(content);
                    return true;
                }
                if (Character.isUpperCase(current)) {
                    content.replace(selStart, selEnd, String.valueOf(current).toLowerCase());
                    removeTimeouts(content);
                    new Timeout(content);
                    return true;
                }
            }
            if (sRecs.indexOfKey(keyCode) == rec2) {
                String val = sRecs.valueAt(rec2);
                char ch = content.charAt(selStart);
                int ix = val.indexOf(ch);
                if (ix >= 0) {
                    int ix2 = (ix + 1) % val.length();
                    content.replace(selStart, selEnd, val, ix2, ix2 + 1);
                    removeTimeouts(content);
                    new Timeout(content);
                    return true;
                }
            }
            rec = sRecs.indexOfKey(keyCode);
            if (rec >= 0) {
                Selection.setSelection(content, selEnd, selEnd);
                selStart = selEnd;
            }
        } else {
            rec = sRecs.indexOfKey(keyCode);
        }
        if (rec >= 0) {
            String val2 = sRecs.valueAt(rec);
            int off = 0;
            if ((pref & 1) != 0 && TextKeyListener.shouldCap(this.mCapitalize, content, selStart)) {
                int i = 0;
                while (true) {
                    if (i >= val2.length()) {
                        break;
                    }
                    if (!Character.isUpperCase(val2.charAt(i))) {
                        i++;
                    } else {
                        off = i;
                        break;
                    }
                }
            }
            if (selStart != selEnd) {
                Selection.setSelection(content, selEnd);
            }
            content.setSpan(OLD_SEL_START, selStart, selStart, 17);
            content.replace(selStart, selEnd, val2, off, off + 1);
            int oldStart = content.getSpanStart(OLD_SEL_START);
            int selEnd2 = Selection.getSelectionEnd(content);
            if (selEnd2 != oldStart) {
                Selection.setSelection(content, oldStart, selEnd2);
                content.setSpan(TextKeyListener.LAST_TYPED, oldStart, selEnd2, 33);
                content.setSpan(TextKeyListener.ACTIVE, oldStart, selEnd2, (rec << 24) | 33);
            }
            removeTimeouts(content);
            new Timeout(content);
            if (content.getSpanStart(this) < 0) {
                KeyListener[] methods = (KeyListener[]) content.getSpans(0, content.length(), KeyListener.class);
                for (KeyListener method : methods) {
                    content.removeSpan(method);
                }
                content.setSpan(this, 0, content.length(), 18);
            }
            return true;
        }
        return super.onKeyDown(view, content, keyCode, event);
    }

    @Override
    public void onSpanChanged(Spannable buf, Object what, int s, int e, int start, int stop) {
        if (what == Selection.SELECTION_END) {
            buf.removeSpan(TextKeyListener.ACTIVE);
            removeTimeouts(buf);
        }
    }

    private static void removeTimeouts(Spannable spannable) {
        for (Timeout timeout : (Timeout[]) spannable.getSpans(0, spannable.length(), Timeout.class)) {
            timeout.removeCallbacks(timeout);
            timeout.mBuffer = null;
            spannable.removeSpan(timeout);
        }
    }

    private class Timeout extends Handler implements Runnable {
        private Editable mBuffer;

        public Timeout(Editable buffer) {
            this.mBuffer = buffer;
            this.mBuffer.setSpan(this, 0, this.mBuffer.length(), 18);
            postAtTime(this, SystemClock.uptimeMillis() + 2000);
        }

        @Override
        public void run() {
            Spannable buf = this.mBuffer;
            if (buf != null) {
                int st = Selection.getSelectionStart(buf);
                int en = Selection.getSelectionEnd(buf);
                int start = buf.getSpanStart(TextKeyListener.ACTIVE);
                int end = buf.getSpanEnd(TextKeyListener.ACTIVE);
                if (st == start && en == end) {
                    Selection.setSelection(buf, Selection.getSelectionEnd(buf));
                }
                buf.removeSpan(this);
            }
        }
    }

    @Override
    public void onSpanAdded(Spannable s, Object what, int start, int end) {
    }

    @Override
    public void onSpanRemoved(Spannable s, Object what, int start, int end) {
    }
}
