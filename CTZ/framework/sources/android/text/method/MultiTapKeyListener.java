package android.text.method;

import android.net.wifi.WifiEnterpriseConfig;
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
        sRecs.put(18, WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
    }

    public MultiTapKeyListener(TextKeyListener.Capitalize capitalize, boolean z) {
        this.mCapitalize = capitalize;
        this.mAutoText = z;
    }

    public static MultiTapKeyListener getInstance(boolean z, TextKeyListener.Capitalize capitalize) {
        int iOrdinal = (capitalize.ordinal() * 2) + (z ? 1 : 0);
        if (sInstance[iOrdinal] == null) {
            sInstance[iOrdinal] = new MultiTapKeyListener(capitalize, z);
        }
        return sInstance[iOrdinal];
    }

    @Override
    public int getInputType() {
        return makeTextContentType(this.mCapitalize, this.mAutoText);
    }

    @Override
    public boolean onKeyDown(View view, Editable editable, int i, KeyEvent keyEvent) {
        int prefs;
        int iIndexOfKey;
        int i2;
        String strValueAt;
        int iIndexOf;
        if (view != null) {
            prefs = TextKeyListener.getInstance().getPrefs(view.getContext());
        } else {
            prefs = 0;
        }
        int selectionStart = Selection.getSelectionStart(editable);
        int selectionEnd = Selection.getSelectionEnd(editable);
        int iMin = Math.min(selectionStart, selectionEnd);
        int iMax = Math.max(selectionStart, selectionEnd);
        int spanStart = editable.getSpanStart(TextKeyListener.ACTIVE);
        int spanEnd = editable.getSpanEnd(TextKeyListener.ACTIVE);
        int spanFlags = (editable.getSpanFlags(TextKeyListener.ACTIVE) & (-16777216)) >>> 24;
        if (spanStart == iMin && spanEnd == iMax && iMax - iMin == 1 && spanFlags >= 0 && spanFlags < sRecs.size()) {
            if (i == 17) {
                char cCharAt = editable.charAt(iMin);
                if (Character.isLowerCase(cCharAt)) {
                    editable.replace(iMin, iMax, String.valueOf(cCharAt).toUpperCase());
                    removeTimeouts(editable);
                    new Timeout(editable);
                    return true;
                }
                if (Character.isUpperCase(cCharAt)) {
                    editable.replace(iMin, iMax, String.valueOf(cCharAt).toLowerCase());
                    removeTimeouts(editable);
                    new Timeout(editable);
                    return true;
                }
            }
            if (sRecs.indexOfKey(i) == spanFlags && (iIndexOf = (strValueAt = sRecs.valueAt(spanFlags)).indexOf(editable.charAt(iMin))) >= 0) {
                int length = (iIndexOf + 1) % strValueAt.length();
                editable.replace(iMin, iMax, strValueAt, length, length + 1);
                removeTimeouts(editable);
                new Timeout(editable);
                return true;
            }
            iIndexOfKey = sRecs.indexOfKey(i);
            if (iIndexOfKey >= 0) {
                Selection.setSelection(editable, iMax, iMax);
                iMin = iMax;
            }
        } else {
            iIndexOfKey = sRecs.indexOfKey(i);
        }
        int i3 = iIndexOfKey;
        if (i3 >= 0) {
            String strValueAt2 = sRecs.valueAt(i3);
            if ((prefs & 1) != 0 && TextKeyListener.shouldCap(this.mCapitalize, editable, iMin)) {
                for (int i4 = 0; i4 < strValueAt2.length(); i4++) {
                    if (Character.isUpperCase(strValueAt2.charAt(i4))) {
                        i2 = i4;
                        break;
                    }
                }
                i2 = 0;
            } else {
                i2 = 0;
            }
            if (iMin != iMax) {
                Selection.setSelection(editable, iMax);
            }
            editable.setSpan(OLD_SEL_START, iMin, iMin, 17);
            editable.replace(iMin, iMax, strValueAt2, i2, i2 + 1);
            int spanStart2 = editable.getSpanStart(OLD_SEL_START);
            int selectionEnd2 = Selection.getSelectionEnd(editable);
            if (selectionEnd2 != spanStart2) {
                Selection.setSelection(editable, spanStart2, selectionEnd2);
                editable.setSpan(TextKeyListener.LAST_TYPED, spanStart2, selectionEnd2, 33);
                editable.setSpan(TextKeyListener.ACTIVE, spanStart2, selectionEnd2, 33 | (i3 << 24));
            }
            removeTimeouts(editable);
            new Timeout(editable);
            if (editable.getSpanStart(this) < 0) {
                for (Object obj : (KeyListener[]) editable.getSpans(0, editable.length(), KeyListener.class)) {
                    editable.removeSpan(obj);
                }
                editable.setSpan(this, 0, editable.length(), 18);
            }
            return true;
        }
        return super.onKeyDown(view, editable, i, keyEvent);
    }

    @Override
    public void onSpanChanged(Spannable spannable, Object obj, int i, int i2, int i3, int i4) {
        if (obj == Selection.SELECTION_END) {
            spannable.removeSpan(TextKeyListener.ACTIVE);
            removeTimeouts(spannable);
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

        public Timeout(Editable editable) {
            this.mBuffer = editable;
            this.mBuffer.setSpan(this, 0, this.mBuffer.length(), 18);
            postAtTime(this, SystemClock.uptimeMillis() + 2000);
        }

        @Override
        public void run() {
            Editable editable = this.mBuffer;
            if (editable != null) {
                int selectionStart = Selection.getSelectionStart(editable);
                int selectionEnd = Selection.getSelectionEnd(editable);
                int spanStart = editable.getSpanStart(TextKeyListener.ACTIVE);
                int spanEnd = editable.getSpanEnd(TextKeyListener.ACTIVE);
                if (selectionStart == spanStart && selectionEnd == spanEnd) {
                    Selection.setSelection(editable, Selection.getSelectionEnd(editable));
                }
                editable.removeSpan(this);
            }
        }
    }

    @Override
    public void onSpanAdded(Spannable spannable, Object obj, int i, int i2) {
    }

    @Override
    public void onSpanRemoved(Spannable spannable, Object obj, int i, int i2) {
    }
}
