package android.view.inputmethod;

import android.os.Bundle;
import android.view.KeyEvent;

public class InputConnectionWrapper implements InputConnection {
    final boolean mMutable;
    private InputConnection mTarget;

    public InputConnectionWrapper(InputConnection target, boolean mutable) {
        this.mMutable = mutable;
        this.mTarget = target;
    }

    public void setTarget(InputConnection target) {
        if (this.mTarget != null && !this.mMutable) {
            throw new SecurityException("not mutable");
        }
        this.mTarget = target;
    }

    @Override
    public CharSequence getTextBeforeCursor(int n, int flags) {
        return this.mTarget.getTextBeforeCursor(n, flags);
    }

    @Override
    public CharSequence getTextAfterCursor(int n, int flags) {
        return this.mTarget.getTextAfterCursor(n, flags);
    }

    @Override
    public CharSequence getSelectedText(int flags) {
        return this.mTarget.getSelectedText(flags);
    }

    @Override
    public int getCursorCapsMode(int reqModes) {
        return this.mTarget.getCursorCapsMode(reqModes);
    }

    @Override
    public ExtractedText getExtractedText(ExtractedTextRequest request, int flags) {
        return this.mTarget.getExtractedText(request, flags);
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        return this.mTarget.deleteSurroundingText(beforeLength, afterLength);
    }

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        return this.mTarget.setComposingText(text, newCursorPosition);
    }

    @Override
    public boolean setComposingRegion(int start, int end) {
        return this.mTarget.setComposingRegion(start, end);
    }

    @Override
    public boolean finishComposingText() {
        return this.mTarget.finishComposingText();
    }

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        return this.mTarget.commitText(text, newCursorPosition);
    }

    @Override
    public boolean commitCompletion(CompletionInfo text) {
        return this.mTarget.commitCompletion(text);
    }

    @Override
    public boolean commitCorrection(CorrectionInfo correctionInfo) {
        return this.mTarget.commitCorrection(correctionInfo);
    }

    @Override
    public boolean setSelection(int start, int end) {
        return this.mTarget.setSelection(start, end);
    }

    @Override
    public boolean performEditorAction(int editorAction) {
        return this.mTarget.performEditorAction(editorAction);
    }

    @Override
    public boolean performContextMenuAction(int id) {
        return this.mTarget.performContextMenuAction(id);
    }

    @Override
    public boolean beginBatchEdit() {
        return this.mTarget.beginBatchEdit();
    }

    @Override
    public boolean endBatchEdit() {
        return this.mTarget.endBatchEdit();
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        return this.mTarget.sendKeyEvent(event);
    }

    @Override
    public boolean clearMetaKeyStates(int states) {
        return this.mTarget.clearMetaKeyStates(states);
    }

    @Override
    public boolean reportFullscreenMode(boolean enabled) {
        return this.mTarget.reportFullscreenMode(enabled);
    }

    @Override
    public boolean performPrivateCommand(String action, Bundle data) {
        return this.mTarget.performPrivateCommand(action, data);
    }

    @Override
    public boolean requestCursorUpdates(int cursorUpdateMode) {
        return this.mTarget.requestCursorUpdates(cursorUpdateMode);
    }
}
