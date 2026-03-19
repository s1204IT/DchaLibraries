package android.text.method;

import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.GetChars;
import android.text.NoCopySpan;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.UpdateLayout;
import android.view.View;
import java.lang.ref.WeakReference;

public class PasswordTransformationMethod implements TransformationMethod, TextWatcher {
    private static char DOT = 8226;
    private static PasswordTransformationMethod sInstance;

    @Override
    public CharSequence getTransformation(CharSequence charSequence, View view) {
        if (charSequence instanceof Spannable) {
            Spannable spannable = (Spannable) charSequence;
            for (ViewReference viewReference : (ViewReference[]) spannable.getSpans(0, spannable.length(), ViewReference.class)) {
                spannable.removeSpan(viewReference);
            }
            removeVisibleSpans(spannable);
            spannable.setSpan(new ViewReference(view), 0, 0, 34);
        }
        return new PasswordCharSequence(charSequence);
    }

    public static PasswordTransformationMethod getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        sInstance = new PasswordTransformationMethod();
        return sInstance;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        if (charSequence instanceof Spannable) {
            Spannable spannable = (Spannable) charSequence;
            ViewReference[] viewReferenceArr = (ViewReference[]) spannable.getSpans(0, charSequence.length(), ViewReference.class);
            if (viewReferenceArr.length == 0) {
                return;
            }
            View view = null;
            for (int i4 = 0; view == null && i4 < viewReferenceArr.length; i4++) {
                view = (View) viewReferenceArr[i4].get();
            }
            if (view != null && (TextKeyListener.getInstance().getPrefs(view.getContext()) & 8) != 0 && i3 > 0) {
                removeVisibleSpans(spannable);
                if (i3 == 1) {
                    spannable.setSpan(new Visible(spannable, this), i, i3 + i, 33);
                }
            }
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {
    }

    @Override
    public void onFocusChanged(View view, CharSequence charSequence, boolean z, int i, Rect rect) {
        if (!z && (charSequence instanceof Spannable)) {
            removeVisibleSpans((Spannable) charSequence);
        }
    }

    private static void removeVisibleSpans(Spannable spannable) {
        for (Visible visible : (Visible[]) spannable.getSpans(0, spannable.length(), Visible.class)) {
            spannable.removeSpan(visible);
        }
    }

    private static class PasswordCharSequence implements CharSequence, GetChars {
        private CharSequence mSource;

        public PasswordCharSequence(CharSequence charSequence) {
            this.mSource = charSequence;
        }

        @Override
        public int length() {
            return this.mSource.length();
        }

        @Override
        public char charAt(int i) {
            if (this.mSource instanceof Spanned) {
                Spanned spanned = (Spanned) this.mSource;
                int spanStart = spanned.getSpanStart(TextKeyListener.ACTIVE);
                int spanEnd = spanned.getSpanEnd(TextKeyListener.ACTIVE);
                if (i >= spanStart && i < spanEnd) {
                    return this.mSource.charAt(i);
                }
                Visible[] visibleArr = (Visible[]) spanned.getSpans(0, spanned.length(), Visible.class);
                for (int i2 = 0; i2 < visibleArr.length; i2++) {
                    if (spanned.getSpanStart(visibleArr[i2].mTransformer) >= 0) {
                        int spanStart2 = spanned.getSpanStart(visibleArr[i2]);
                        int spanEnd2 = spanned.getSpanEnd(visibleArr[i2]);
                        if (i >= spanStart2 && i < spanEnd2) {
                            return this.mSource.charAt(i);
                        }
                    }
                }
            }
            return PasswordTransformationMethod.DOT;
        }

        @Override
        public CharSequence subSequence(int i, int i2) {
            char[] cArr = new char[i2 - i];
            getChars(i, i2, cArr, 0);
            return new String(cArr);
        }

        @Override
        public String toString() {
            return subSequence(0, length()).toString();
        }

        @Override
        public void getChars(int i, int i2, char[] cArr, int i3) {
            int[] iArr;
            int[] iArr2;
            int spanEnd;
            int length;
            boolean z;
            TextUtils.getChars(this.mSource, i, i2, cArr, i3);
            int spanStart = -1;
            if (this.mSource instanceof Spanned) {
                Spanned spanned = (Spanned) this.mSource;
                spanStart = spanned.getSpanStart(TextKeyListener.ACTIVE);
                spanEnd = spanned.getSpanEnd(TextKeyListener.ACTIVE);
                Visible[] visibleArr = (Visible[]) spanned.getSpans(0, spanned.length(), Visible.class);
                length = visibleArr.length;
                iArr = new int[length];
                iArr2 = new int[length];
                for (int i4 = 0; i4 < length; i4++) {
                    if (spanned.getSpanStart(visibleArr[i4].mTransformer) >= 0) {
                        iArr[i4] = spanned.getSpanStart(visibleArr[i4]);
                        iArr2[i4] = spanned.getSpanEnd(visibleArr[i4]);
                    }
                }
            } else {
                iArr = null;
                iArr2 = null;
                spanEnd = -1;
                length = 0;
            }
            for (int i5 = i; i5 < i2; i5++) {
                if (i5 < spanStart || i5 >= spanEnd) {
                    int i6 = 0;
                    while (true) {
                        if (i6 < length) {
                            if (i5 < iArr[i6] || i5 >= iArr2[i6]) {
                                i6++;
                            } else {
                                z = true;
                                break;
                            }
                        } else {
                            z = false;
                            break;
                        }
                    }
                    if (!z) {
                        cArr[(i5 - i) + i3] = PasswordTransformationMethod.DOT;
                    }
                }
            }
        }
    }

    private static class Visible extends Handler implements UpdateLayout, Runnable {
        private Spannable mText;
        private PasswordTransformationMethod mTransformer;

        public Visible(Spannable spannable, PasswordTransformationMethod passwordTransformationMethod) {
            this.mText = spannable;
            this.mTransformer = passwordTransformationMethod;
            postAtTime(this, SystemClock.uptimeMillis() + 1500);
        }

        @Override
        public void run() {
            this.mText.removeSpan(this);
        }
    }

    private static class ViewReference extends WeakReference<View> implements NoCopySpan {
        public ViewReference(View view) {
            super(view);
        }
    }
}
