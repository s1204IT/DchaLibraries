package android.view.inputmethod;

import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;

public class InputConnectionWrapper implements InputConnection {
    private int mMissingMethodFlags;
    final boolean mMutable;
    private InputConnection mTarget;

    public InputConnectionWrapper(InputConnection inputConnection, boolean z) {
        this.mMutable = z;
        this.mTarget = inputConnection;
        this.mMissingMethodFlags = InputConnectionInspector.getMissingMethodFlags(inputConnection);
    }

    public void setTarget(InputConnection inputConnection) {
        if (this.mTarget != null && !this.mMutable) {
            throw new SecurityException("not mutable");
        }
        this.mTarget = inputConnection;
        this.mMissingMethodFlags = InputConnectionInspector.getMissingMethodFlags(inputConnection);
    }

    public int getMissingMethodFlags() {
        return this.mMissingMethodFlags;
    }

    @Override
    public CharSequence getTextBeforeCursor(int i, int i2) {
        return this.mTarget.getTextBeforeCursor(i, i2);
    }

    @Override
    public CharSequence getTextAfterCursor(int i, int i2) {
        return this.mTarget.getTextAfterCursor(i, i2);
    }

    @Override
    public CharSequence getSelectedText(int i) {
        return this.mTarget.getSelectedText(i);
    }

    @Override
    public int getCursorCapsMode(int i) {
        return this.mTarget.getCursorCapsMode(i);
    }

    @Override
    public ExtractedText getExtractedText(ExtractedTextRequest extractedTextRequest, int i) {
        return this.mTarget.getExtractedText(extractedTextRequest, i);
    }

    @Override
    public boolean deleteSurroundingTextInCodePoints(int i, int i2) {
        return this.mTarget.deleteSurroundingTextInCodePoints(i, i2);
    }

    @Override
    public boolean deleteSurroundingText(int i, int i2) {
        return this.mTarget.deleteSurroundingText(i, i2);
    }

    @Override
    public boolean setComposingText(CharSequence charSequence, int i) {
        return this.mTarget.setComposingText(charSequence, i);
    }

    @Override
    public boolean setComposingRegion(int i, int i2) {
        return this.mTarget.setComposingRegion(i, i2);
    }

    @Override
    public boolean finishComposingText() {
        return this.mTarget.finishComposingText();
    }

    @Override
    public boolean commitText(CharSequence charSequence, int i) {
        return this.mTarget.commitText(charSequence, i);
    }

    @Override
    public boolean commitCompletion(CompletionInfo completionInfo) {
        return this.mTarget.commitCompletion(completionInfo);
    }

    @Override
    public boolean commitCorrection(CorrectionInfo correctionInfo) {
        return this.mTarget.commitCorrection(correctionInfo);
    }

    @Override
    public boolean setSelection(int i, int i2) {
        return this.mTarget.setSelection(i, i2);
    }

    @Override
    public boolean performEditorAction(int i) {
        return this.mTarget.performEditorAction(i);
    }

    @Override
    public boolean performContextMenuAction(int i) {
        return this.mTarget.performContextMenuAction(i);
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
    public boolean sendKeyEvent(KeyEvent keyEvent) {
        return this.mTarget.sendKeyEvent(keyEvent);
    }

    @Override
    public boolean clearMetaKeyStates(int i) {
        return this.mTarget.clearMetaKeyStates(i);
    }

    @Override
    public boolean reportFullscreenMode(boolean z) {
        return this.mTarget.reportFullscreenMode(z);
    }

    @Override
    public boolean performPrivateCommand(String str, Bundle bundle) {
        return this.mTarget.performPrivateCommand(str, bundle);
    }

    @Override
    public boolean requestCursorUpdates(int i) {
        return this.mTarget.requestCursorUpdates(i);
    }

    @Override
    public Handler getHandler() {
        return this.mTarget.getHandler();
    }

    @Override
    public void closeConnection() {
        this.mTarget.closeConnection();
    }

    @Override
    public boolean commitContent(InputContentInfo inputContentInfo, int i, Bundle bundle) {
        return this.mTarget.commitContent(inputContentInfo, i, bundle);
    }
}
