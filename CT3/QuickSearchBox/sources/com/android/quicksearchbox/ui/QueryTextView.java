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

    public QueryTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public QueryTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public QueryTextView(Context context) {
        super(context);
    }

    public void setTextSelection(boolean selectAll) {
        if (selectAll) {
            selectAll();
        } else {
            setSelection(length());
        }
    }

    protected void replaceText(CharSequence text) {
        clearComposingText();
        setText(text);
        setTextSelection(false);
    }

    public void setCommitCompletionListener(CommitCompletionListener listener) {
        this.mCommitCompletionListener = listener;
    }

    private InputMethodManager getInputMethodManager() {
        return (InputMethodManager) getContext().getSystemService("input_method");
    }

    public void showInputMethod() {
        InputMethodManager imm = getInputMethodManager();
        if (imm == null) {
            return;
        }
        imm.showSoftInput(this, 0);
    }

    public void hideInputMethod() {
        InputMethodManager imm = getInputMethodManager();
        if (imm == null) {
            return;
        }
        imm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    @Override
    public void onCommitCompletion(CompletionInfo completion) {
        hideInputMethod();
        replaceText(completion.getText());
        if (this.mCommitCompletionListener == null) {
            return;
        }
        this.mCommitCompletionListener.onCommitCompletion(completion.getPosition());
    }
}
