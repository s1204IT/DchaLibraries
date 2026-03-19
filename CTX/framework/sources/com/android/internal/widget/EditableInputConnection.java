package com.android.internal.widget;

import android.os.Bundle;
import android.text.Editable;
import android.text.Spanned;
import android.text.method.KeyListener;
import android.text.style.SuggestionSpan;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.widget.TextView;

public class EditableInputConnection extends BaseInputConnection {
    private static final boolean DEBUG = false;
    private static final String TAG = "EditableInputConnection";
    private int mBatchEditNesting;
    private final TextView mTextView;

    public EditableInputConnection(TextView textView) {
        super((View) textView, true);
        this.mTextView = textView;
    }

    @Override
    public Editable getEditable() {
        TextView textView = this.mTextView;
        if (textView != null) {
            return textView.getEditableText();
        }
        return null;
    }

    @Override
    public boolean beginBatchEdit() {
        synchronized (this) {
            if (this.mBatchEditNesting >= 0) {
                this.mTextView.beginBatchEdit();
                this.mBatchEditNesting++;
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean endBatchEdit() {
        synchronized (this) {
            if (this.mBatchEditNesting > 0) {
                this.mTextView.endBatchEdit();
                this.mBatchEditNesting--;
                return true;
            }
            return false;
        }
    }

    @Override
    public void closeConnection() {
        super.closeConnection();
        synchronized (this) {
            while (this.mBatchEditNesting > 0) {
                endBatchEdit();
            }
            this.mBatchEditNesting = -1;
        }
    }

    @Override
    public boolean clearMetaKeyStates(int i) {
        Editable editable = getEditable();
        if (editable == null) {
            return false;
        }
        KeyListener keyListener = this.mTextView.getKeyListener();
        if (keyListener != null) {
            try {
                keyListener.clearMetaKeyState(this.mTextView, editable, i);
                return true;
            } catch (AbstractMethodError e) {
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean commitCompletion(CompletionInfo completionInfo) {
        this.mTextView.beginBatchEdit();
        this.mTextView.onCommitCompletion(completionInfo);
        this.mTextView.endBatchEdit();
        return true;
    }

    @Override
    public boolean commitCorrection(CorrectionInfo correctionInfo) {
        this.mTextView.beginBatchEdit();
        this.mTextView.onCommitCorrection(correctionInfo);
        this.mTextView.endBatchEdit();
        return true;
    }

    @Override
    public boolean performEditorAction(int i) {
        this.mTextView.onEditorAction(i);
        return true;
    }

    @Override
    public boolean performContextMenuAction(int i) {
        this.mTextView.beginBatchEdit();
        this.mTextView.onTextContextMenuItem(i);
        this.mTextView.endBatchEdit();
        return true;
    }

    @Override
    public ExtractedText getExtractedText(ExtractedTextRequest extractedTextRequest, int i) {
        if (this.mTextView != null) {
            ExtractedText extractedText = new ExtractedText();
            if (this.mTextView.extractText(extractedTextRequest, extractedText)) {
                if ((i & 1) != 0) {
                    this.mTextView.setExtracting(extractedTextRequest);
                }
                return extractedText;
            }
            return null;
        }
        return null;
    }

    @Override
    public boolean performPrivateCommand(String str, Bundle bundle) {
        this.mTextView.onPrivateIMECommand(str, bundle);
        return true;
    }

    @Override
    public boolean commitText(CharSequence charSequence, int i) {
        if (this.mTextView == null) {
            return super.commitText(charSequence, i);
        }
        if (charSequence instanceof Spanned) {
            this.mIMM.registerSuggestionSpansForNotification((SuggestionSpan[]) ((Spanned) charSequence).getSpans(0, charSequence.length(), SuggestionSpan.class));
        }
        this.mTextView.resetErrorChangedFlag();
        boolean zCommitText = super.commitText(charSequence, i);
        this.mTextView.hideErrorIfUnchanged();
        return zCommitText;
    }

    @Override
    public boolean requestCursorUpdates(int i) {
        if ((i & (-4)) != 0 || this.mIMM == null) {
            return false;
        }
        this.mIMM.setUpdateCursorAnchorInfoMode(i);
        if ((i & 1) != 0 && this.mTextView != null && !this.mTextView.isInLayout()) {
            this.mTextView.requestLayout();
        }
        return true;
    }
}
