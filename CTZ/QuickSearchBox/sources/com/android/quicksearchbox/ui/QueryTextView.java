package com.android.quicksearchbox.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class QueryTextView extends EditText {
    private CommitCompletionListener mCommitCompletionListener;

    public interface CommitCompletionListener {
        void onCommitCompletion(int i);
    }

    public QueryTextView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public QueryTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public QueryTextView(Context context) {
        super(context);
    }

    public void setTextSelection(boolean z) {
        if (z) {
            selectAll();
        } else {
            setSelection(length());
        }
    }

    protected void replaceText(CharSequence charSequence) {
        clearComposingText();
        setText(charSequence);
        setTextSelection(false);
    }

    public void setCommitCompletionListener(CommitCompletionListener commitCompletionListener) {
        this.mCommitCompletionListener = commitCompletionListener;
    }

    private InputMethodManager getInputMethodManager() {
        return (InputMethodManager) getContext().getSystemService("input_method");
    }

    public void showInputMethod() {
        InputMethodManager inputMethodManager = getInputMethodManager();
        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(this, 0);
        }
    }

    public void hideInputMethod() {
        InputMethodManager inputMethodManager = getInputMethodManager();
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    @Override
    public void onCommitCompletion(CompletionInfo completionInfo) {
        hideInputMethod();
        replaceText(completionInfo.getText());
        if (this.mCommitCompletionListener != null) {
            this.mCommitCompletionListener.onCommitCompletion(completionInfo.getPosition());
        }
    }
}
