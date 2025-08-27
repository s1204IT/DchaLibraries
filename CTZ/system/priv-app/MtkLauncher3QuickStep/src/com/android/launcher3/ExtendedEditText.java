package com.android.launcher3;

import android.content.Context;
import android.support.v4.media.subtitle.Cea708CCParser;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import com.android.launcher3.util.UiThreadHelper;

/* loaded from: classes.dex */
public class ExtendedEditText extends EditText {
    private OnBackKeyListener mBackKeyListener;
    private boolean mForceDisableSuggestions;
    private boolean mShowImeAfterFirstLayout;

    public interface OnBackKeyListener {
        boolean onBackKey();
    }

    public ExtendedEditText(Context context) {
        super(context);
        this.mForceDisableSuggestions = false;
    }

    public ExtendedEditText(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mForceDisableSuggestions = false;
    }

    public ExtendedEditText(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mForceDisableSuggestions = false;
    }

    public void setOnBackKeyListener(OnBackKeyListener onBackKeyListener) {
        this.mBackKeyListener = onBackKeyListener;
    }

    @Override // android.widget.TextView, android.view.View
    public boolean onKeyPreIme(int i, KeyEvent keyEvent) {
        if (i == 4 && keyEvent.getAction() == 1) {
            if (this.mBackKeyListener != null) {
                return this.mBackKeyListener.onBackKey();
            }
            return false;
        }
        return super.onKeyPreIme(i, keyEvent);
    }

    @Override // android.widget.TextView, android.view.View
    public boolean onDragEvent(DragEvent dragEvent) {
        return false;
    }

    @Override // android.widget.TextView, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (this.mShowImeAfterFirstLayout) {
            post(new Runnable() { // from class: com.android.launcher3.ExtendedEditText.1
                @Override // java.lang.Runnable
                public void run() {
                    ExtendedEditText.this.showSoftInput();
                    ExtendedEditText.this.mShowImeAfterFirstLayout = false;
                }
            });
        }
    }

    public void showKeyboard() {
        this.mShowImeAfterFirstLayout = !showSoftInput();
    }

    private boolean showSoftInput() {
        return requestFocus() && ((InputMethodManager) getContext().getSystemService("input_method")).showSoftInput(this, 1);
    }

    public void dispatchBackKey() {
        UiThreadHelper.hideKeyboardAsync(getContext(), getWindowToken());
        if (this.mBackKeyListener != null) {
            this.mBackKeyListener.onBackKey();
        }
    }

    public void forceDisableSuggestions(boolean z) {
        this.mForceDisableSuggestions = z;
    }

    @Override // android.widget.TextView
    public boolean isSuggestionsEnabled() {
        return !this.mForceDisableSuggestions && super.isSuggestionsEnabled();
    }

    public void reset() {
        View viewFocusSearch;
        if (!TextUtils.isEmpty(getText())) {
            setText("");
        }
        if (isFocused() && (viewFocusSearch = focusSearch(Cea708CCParser.Const.CODE_C1_CW2)) != null) {
            viewFocusSearch.requestFocus();
        }
        UiThreadHelper.hideKeyboardAsync(getContext(), getWindowToken());
    }
}
