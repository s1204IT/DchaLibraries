package com.android.internal.view;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionInspector;
import android.view.inputmethod.InputContentInfo;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.SomeArgs;
import com.android.internal.view.IInputContext;

public abstract class IInputConnectionWrapper extends IInputContext.Stub {
    private static final boolean DEBUG = false;
    private static final int DO_BEGIN_BATCH_EDIT = 90;
    private static final int DO_CLEAR_META_KEY_STATES = 130;
    private static final int DO_CLOSE_CONNECTION = 150;
    private static final int DO_COMMIT_COMPLETION = 55;
    private static final int DO_COMMIT_CONTENT = 160;
    private static final int DO_COMMIT_CORRECTION = 56;
    private static final int DO_COMMIT_TEXT = 50;
    private static final int DO_DELETE_SURROUNDING_TEXT = 80;
    private static final int DO_DELETE_SURROUNDING_TEXT_IN_CODE_POINTS = 81;
    private static final int DO_END_BATCH_EDIT = 95;
    private static final int DO_FINISH_COMPOSING_TEXT = 65;
    private static final int DO_GET_CURSOR_CAPS_MODE = 30;
    private static final int DO_GET_EXTRACTED_TEXT = 40;
    private static final int DO_GET_SELECTED_TEXT = 25;
    private static final int DO_GET_TEXT_AFTER_CURSOR = 10;
    private static final int DO_GET_TEXT_BEFORE_CURSOR = 20;
    private static final int DO_PERFORM_CONTEXT_MENU_ACTION = 59;
    private static final int DO_PERFORM_EDITOR_ACTION = 58;
    private static final int DO_PERFORM_PRIVATE_COMMAND = 120;
    private static final int DO_REQUEST_UPDATE_CURSOR_ANCHOR_INFO = 140;
    private static final int DO_SEND_KEY_EVENT = 70;
    private static final int DO_SET_COMPOSING_REGION = 63;
    private static final int DO_SET_COMPOSING_TEXT = 60;
    private static final int DO_SET_SELECTION = 57;
    private static final String TAG = "IInputConnectionWrapper";
    private Handler mH;

    @GuardedBy("mLock")
    private InputConnection mInputConnection;
    private Looper mMainLooper;
    private Object mLock = new Object();

    @GuardedBy("mLock")
    private boolean mFinished = false;

    protected abstract boolean isActive();

    protected abstract void onUserAction();

    class MyHandler extends Handler {
        MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            IInputConnectionWrapper.this.executeMessage(message);
        }
    }

    public IInputConnectionWrapper(Looper looper, InputConnection inputConnection) {
        this.mInputConnection = inputConnection;
        this.mMainLooper = looper;
        this.mH = new MyHandler(this.mMainLooper);
    }

    public InputConnection getInputConnection() {
        InputConnection inputConnection;
        synchronized (this.mLock) {
            inputConnection = this.mInputConnection;
        }
        return inputConnection;
    }

    protected boolean isFinished() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mFinished;
        }
        return z;
    }

    @Override
    public void getTextAfterCursor(int i, int i2, int i3, IInputContextCallback iInputContextCallback) {
        dispatchMessage(obtainMessageIISC(10, i, i2, i3, iInputContextCallback));
    }

    @Override
    public void getTextBeforeCursor(int i, int i2, int i3, IInputContextCallback iInputContextCallback) {
        dispatchMessage(obtainMessageIISC(20, i, i2, i3, iInputContextCallback));
    }

    @Override
    public void getSelectedText(int i, int i2, IInputContextCallback iInputContextCallback) {
        dispatchMessage(obtainMessageISC(25, i, i2, iInputContextCallback));
    }

    @Override
    public void getCursorCapsMode(int i, int i2, IInputContextCallback iInputContextCallback) {
        dispatchMessage(obtainMessageISC(30, i, i2, iInputContextCallback));
    }

    @Override
    public void getExtractedText(ExtractedTextRequest extractedTextRequest, int i, int i2, IInputContextCallback iInputContextCallback) {
        dispatchMessage(obtainMessageIOSC(40, i, extractedTextRequest, i2, iInputContextCallback));
    }

    @Override
    public void commitText(CharSequence charSequence, int i) {
        dispatchMessage(obtainMessageIO(50, i, charSequence));
    }

    @Override
    public void commitCompletion(CompletionInfo completionInfo) {
        dispatchMessage(obtainMessageO(55, completionInfo));
    }

    @Override
    public void commitCorrection(CorrectionInfo correctionInfo) {
        dispatchMessage(obtainMessageO(56, correctionInfo));
    }

    @Override
    public void setSelection(int i, int i2) {
        dispatchMessage(obtainMessageII(57, i, i2));
    }

    @Override
    public void performEditorAction(int i) {
        dispatchMessage(obtainMessageII(58, i, 0));
    }

    @Override
    public void performContextMenuAction(int i) {
        dispatchMessage(obtainMessageII(59, i, 0));
    }

    @Override
    public void setComposingRegion(int i, int i2) {
        dispatchMessage(obtainMessageII(63, i, i2));
    }

    @Override
    public void setComposingText(CharSequence charSequence, int i) {
        dispatchMessage(obtainMessageIO(60, i, charSequence));
    }

    @Override
    public void finishComposingText() {
        dispatchMessage(obtainMessage(65));
    }

    @Override
    public void sendKeyEvent(KeyEvent keyEvent) {
        dispatchMessage(obtainMessageO(70, keyEvent));
    }

    @Override
    public void clearMetaKeyStates(int i) {
        dispatchMessage(obtainMessageII(130, i, 0));
    }

    @Override
    public void deleteSurroundingText(int i, int i2) {
        dispatchMessage(obtainMessageII(80, i, i2));
    }

    @Override
    public void deleteSurroundingTextInCodePoints(int i, int i2) {
        dispatchMessage(obtainMessageII(81, i, i2));
    }

    @Override
    public void beginBatchEdit() {
        dispatchMessage(obtainMessage(90));
    }

    @Override
    public void endBatchEdit() {
        dispatchMessage(obtainMessage(95));
    }

    @Override
    public void performPrivateCommand(String str, Bundle bundle) {
        dispatchMessage(obtainMessageOO(120, str, bundle));
    }

    @Override
    public void requestUpdateCursorAnchorInfo(int i, int i2, IInputContextCallback iInputContextCallback) {
        dispatchMessage(obtainMessageISC(140, i, i2, iInputContextCallback));
    }

    public void closeConnection() {
        dispatchMessage(obtainMessage(150));
    }

    @Override
    public void commitContent(InputContentInfo inputContentInfo, int i, Bundle bundle, int i2, IInputContextCallback iInputContextCallback) {
        dispatchMessage(obtainMessageIOOSC(160, i, inputContentInfo, bundle, i2, iInputContextCallback));
    }

    void dispatchMessage(Message message) {
        if (Looper.myLooper() == this.mMainLooper) {
            executeMessage(message);
            message.recycle();
        } else {
            this.mH.sendMessage(message);
        }
    }

    void executeMessage(Message message) {
        SomeArgs someArgs;
        IInputContextCallback iInputContextCallback;
        int i;
        InputConnection inputConnection;
        IInputContextCallback iInputContextCallback2;
        int i2;
        InputConnection inputConnection2;
        IInputContextCallback iInputContextCallback3;
        int i3;
        InputConnection inputConnection3;
        IInputContextCallback iInputContextCallback4;
        int i4;
        InputConnection inputConnection4;
        IInputContextCallback iInputContextCallback5;
        int i5;
        InputConnection inputConnection5;
        IInputContextCallback iInputContextCallback6;
        int i6;
        InputConnection inputConnection6;
        IInputContextCallback iInputContextCallback7;
        int i7;
        InputConnection inputConnection7;
        int i8 = message.what;
        switch (i8) {
            case 55:
                InputConnection inputConnection8 = getInputConnection();
                if (inputConnection8 == null || !isActive()) {
                    Log.w(TAG, "commitCompletion on inactive InputConnection");
                    return;
                } else {
                    inputConnection8.commitCompletion((CompletionInfo) message.obj);
                    return;
                }
            case 56:
                InputConnection inputConnection9 = getInputConnection();
                if (inputConnection9 == null || !isActive()) {
                    Log.w(TAG, "commitCorrection on inactive InputConnection");
                    return;
                } else {
                    inputConnection9.commitCorrection((CorrectionInfo) message.obj);
                    return;
                }
            case 57:
                InputConnection inputConnection10 = getInputConnection();
                if (inputConnection10 == null || !isActive()) {
                    Log.w(TAG, "setSelection on inactive InputConnection");
                    return;
                } else {
                    inputConnection10.setSelection(message.arg1, message.arg2);
                    return;
                }
            case 58:
                InputConnection inputConnection11 = getInputConnection();
                if (inputConnection11 == null || !isActive()) {
                    Log.w(TAG, "performEditorAction on inactive InputConnection");
                    return;
                } else {
                    inputConnection11.performEditorAction(message.arg1);
                    return;
                }
            case 59:
                InputConnection inputConnection12 = getInputConnection();
                if (inputConnection12 == null || !isActive()) {
                    Log.w(TAG, "performContextMenuAction on inactive InputConnection");
                    return;
                } else {
                    inputConnection12.performContextMenuAction(message.arg1);
                    return;
                }
            case 60:
                InputConnection inputConnection13 = getInputConnection();
                if (inputConnection13 == null || !isActive()) {
                    Log.w(TAG, "setComposingText on inactive InputConnection");
                    return;
                } else {
                    inputConnection13.setComposingText((CharSequence) message.obj, message.arg1);
                    onUserAction();
                    return;
                }
            default:
                switch (i8) {
                    case 80:
                        InputConnection inputConnection14 = getInputConnection();
                        if (inputConnection14 == null || !isActive()) {
                            Log.w(TAG, "deleteSurroundingText on inactive InputConnection");
                            return;
                        } else {
                            inputConnection14.deleteSurroundingText(message.arg1, message.arg2);
                            return;
                        }
                    case 81:
                        InputConnection inputConnection15 = getInputConnection();
                        if (inputConnection15 == null || !isActive()) {
                            Log.w(TAG, "deleteSurroundingTextInCodePoints on inactive InputConnection");
                            return;
                        } else {
                            inputConnection15.deleteSurroundingTextInCodePoints(message.arg1, message.arg2);
                            return;
                        }
                    default:
                        switch (i8) {
                            case 10:
                                someArgs = (SomeArgs) message.obj;
                                try {
                                    try {
                                        iInputContextCallback = (IInputContextCallback) someArgs.arg6;
                                        i = someArgs.argi6;
                                        inputConnection = getInputConnection();
                                    } finally {
                                    }
                                } catch (RemoteException e) {
                                    Log.w(TAG, "Got RemoteException calling setTextAfterCursor", e);
                                }
                                if (inputConnection != null && isActive()) {
                                    iInputContextCallback.setTextAfterCursor(inputConnection.getTextAfterCursor(message.arg1, message.arg2), i);
                                    return;
                                }
                                Log.w(TAG, "getTextAfterCursor on inactive InputConnection");
                                iInputContextCallback.setTextAfterCursor(null, i);
                                return;
                            case 20:
                                someArgs = (SomeArgs) message.obj;
                                try {
                                    try {
                                        iInputContextCallback2 = (IInputContextCallback) someArgs.arg6;
                                        i2 = someArgs.argi6;
                                        inputConnection2 = getInputConnection();
                                    } finally {
                                    }
                                } catch (RemoteException e2) {
                                    Log.w(TAG, "Got RemoteException calling setTextBeforeCursor", e2);
                                }
                                if (inputConnection2 != null && isActive()) {
                                    iInputContextCallback2.setTextBeforeCursor(inputConnection2.getTextBeforeCursor(message.arg1, message.arg2), i2);
                                    return;
                                }
                                Log.w(TAG, "getTextBeforeCursor on inactive InputConnection");
                                iInputContextCallback2.setTextBeforeCursor(null, i2);
                                return;
                            case 25:
                                someArgs = (SomeArgs) message.obj;
                                try {
                                    try {
                                        iInputContextCallback3 = (IInputContextCallback) someArgs.arg6;
                                        i3 = someArgs.argi6;
                                        inputConnection3 = getInputConnection();
                                    } finally {
                                    }
                                } catch (RemoteException e3) {
                                    Log.w(TAG, "Got RemoteException calling setSelectedText", e3);
                                }
                                if (inputConnection3 != null && isActive()) {
                                    iInputContextCallback3.setSelectedText(inputConnection3.getSelectedText(message.arg1), i3);
                                    return;
                                }
                                Log.w(TAG, "getSelectedText on inactive InputConnection");
                                iInputContextCallback3.setSelectedText(null, i3);
                                return;
                            case 30:
                                someArgs = (SomeArgs) message.obj;
                                try {
                                    try {
                                        iInputContextCallback4 = (IInputContextCallback) someArgs.arg6;
                                        i4 = someArgs.argi6;
                                        inputConnection4 = getInputConnection();
                                    } finally {
                                    }
                                } catch (RemoteException e4) {
                                    Log.w(TAG, "Got RemoteException calling setCursorCapsMode", e4);
                                }
                                if (inputConnection4 != null && isActive()) {
                                    iInputContextCallback4.setCursorCapsMode(inputConnection4.getCursorCapsMode(message.arg1), i4);
                                    return;
                                }
                                Log.w(TAG, "getCursorCapsMode on inactive InputConnection");
                                iInputContextCallback4.setCursorCapsMode(0, i4);
                                return;
                            case 40:
                                someArgs = (SomeArgs) message.obj;
                                try {
                                    try {
                                        iInputContextCallback5 = (IInputContextCallback) someArgs.arg6;
                                        i5 = someArgs.argi6;
                                        inputConnection5 = getInputConnection();
                                    } finally {
                                    }
                                } catch (RemoteException e5) {
                                    Log.w(TAG, "Got RemoteException calling setExtractedText", e5);
                                }
                                if (inputConnection5 != null && isActive()) {
                                    iInputContextCallback5.setExtractedText(inputConnection5.getExtractedText((ExtractedTextRequest) someArgs.arg1, message.arg1), i5);
                                    return;
                                }
                                Log.w(TAG, "getExtractedText on inactive InputConnection");
                                iInputContextCallback5.setExtractedText(null, i5);
                                return;
                            case 50:
                                InputConnection inputConnection16 = getInputConnection();
                                if (inputConnection16 == null || !isActive()) {
                                    Log.w(TAG, "commitText on inactive InputConnection");
                                    return;
                                } else {
                                    inputConnection16.commitText((CharSequence) message.obj, message.arg1);
                                    onUserAction();
                                    return;
                                }
                            case 63:
                                InputConnection inputConnection17 = getInputConnection();
                                if (inputConnection17 == null || !isActive()) {
                                    Log.w(TAG, "setComposingRegion on inactive InputConnection");
                                    return;
                                } else {
                                    inputConnection17.setComposingRegion(message.arg1, message.arg2);
                                    return;
                                }
                            case 65:
                                if (isFinished()) {
                                    return;
                                }
                                InputConnection inputConnection18 = getInputConnection();
                                if (inputConnection18 == null) {
                                    Log.w(TAG, "finishComposingText on inactive InputConnection");
                                    return;
                                } else {
                                    inputConnection18.finishComposingText();
                                    return;
                                }
                            case 70:
                                InputConnection inputConnection19 = getInputConnection();
                                if (inputConnection19 == null || !isActive()) {
                                    Log.w(TAG, "sendKeyEvent on inactive InputConnection");
                                    return;
                                } else {
                                    inputConnection19.sendKeyEvent((KeyEvent) message.obj);
                                    onUserAction();
                                    return;
                                }
                            case 90:
                                InputConnection inputConnection20 = getInputConnection();
                                if (inputConnection20 == null || !isActive()) {
                                    Log.w(TAG, "beginBatchEdit on inactive InputConnection");
                                    return;
                                } else {
                                    inputConnection20.beginBatchEdit();
                                    return;
                                }
                            case 95:
                                InputConnection inputConnection21 = getInputConnection();
                                if (inputConnection21 == null || !isActive()) {
                                    Log.w(TAG, "endBatchEdit on inactive InputConnection");
                                    return;
                                } else {
                                    inputConnection21.endBatchEdit();
                                    return;
                                }
                            case 120:
                                someArgs = (SomeArgs) message.obj;
                                try {
                                    String str = (String) someArgs.arg1;
                                    Bundle bundle = (Bundle) someArgs.arg2;
                                    InputConnection inputConnection22 = getInputConnection();
                                    if (inputConnection22 != null && isActive()) {
                                        inputConnection22.performPrivateCommand(str, bundle);
                                        return;
                                    }
                                    Log.w(TAG, "performPrivateCommand on inactive InputConnection");
                                    return;
                                } finally {
                                }
                            case 130:
                                InputConnection inputConnection23 = getInputConnection();
                                if (inputConnection23 == null || !isActive()) {
                                    Log.w(TAG, "clearMetaKeyStates on inactive InputConnection");
                                    return;
                                } else {
                                    inputConnection23.clearMetaKeyStates(message.arg1);
                                    return;
                                }
                            case 140:
                                someArgs = (SomeArgs) message.obj;
                                try {
                                    try {
                                        iInputContextCallback6 = (IInputContextCallback) someArgs.arg6;
                                        i6 = someArgs.argi6;
                                        inputConnection6 = getInputConnection();
                                    } finally {
                                    }
                                } catch (RemoteException e6) {
                                    Log.w(TAG, "Got RemoteException calling requestCursorAnchorInfo", e6);
                                }
                                if (inputConnection6 != null && isActive()) {
                                    iInputContextCallback6.setRequestUpdateCursorAnchorInfoResult(inputConnection6.requestCursorUpdates(message.arg1), i6);
                                    return;
                                }
                                Log.w(TAG, "requestCursorAnchorInfo on inactive InputConnection");
                                iInputContextCallback6.setRequestUpdateCursorAnchorInfoResult(false, i6);
                                return;
                            case 150:
                                if (isFinished()) {
                                    return;
                                }
                                try {
                                    InputConnection inputConnection24 = getInputConnection();
                                    if (inputConnection24 == null) {
                                        synchronized (this.mLock) {
                                            this.mInputConnection = null;
                                            this.mFinished = true;
                                            break;
                                        }
                                        return;
                                    }
                                    if ((InputConnectionInspector.getMissingMethodFlags(inputConnection24) & 64) == 0) {
                                        inputConnection24.closeConnection();
                                        break;
                                    }
                                    synchronized (this.mLock) {
                                        this.mInputConnection = null;
                                        this.mFinished = true;
                                        break;
                                    }
                                    return;
                                } catch (Throwable th) {
                                    synchronized (this.mLock) {
                                        this.mInputConnection = null;
                                        this.mFinished = true;
                                        throw th;
                                    }
                                }
                            case 160:
                                int i9 = message.arg1;
                                someArgs = (SomeArgs) message.obj;
                                try {
                                    try {
                                        iInputContextCallback7 = (IInputContextCallback) someArgs.arg6;
                                        i7 = someArgs.argi6;
                                        inputConnection7 = getInputConnection();
                                    } finally {
                                    }
                                } catch (RemoteException e7) {
                                    Log.w(TAG, "Got RemoteException calling commitContent", e7);
                                }
                                if (inputConnection7 != null && isActive()) {
                                    InputContentInfo inputContentInfo = (InputContentInfo) someArgs.arg1;
                                    if (inputContentInfo != null && inputContentInfo.validate()) {
                                        iInputContextCallback7.setCommitContentResult(inputConnection7.commitContent(inputContentInfo, i9, (Bundle) someArgs.arg2), i7);
                                        return;
                                    }
                                    Log.w(TAG, "commitContent with invalid inputContentInfo=" + inputContentInfo);
                                    iInputContextCallback7.setCommitContentResult(false, i7);
                                    return;
                                }
                                Log.w(TAG, "commitContent on inactive InputConnection");
                                iInputContextCallback7.setCommitContentResult(false, i7);
                                return;
                            default:
                                Log.w(TAG, "Unhandled message code: " + message.what);
                                return;
                        }
                }
        }
    }

    Message obtainMessage(int i) {
        return this.mH.obtainMessage(i);
    }

    Message obtainMessageII(int i, int i2, int i3) {
        return this.mH.obtainMessage(i, i2, i3);
    }

    Message obtainMessageO(int i, Object obj) {
        return this.mH.obtainMessage(i, 0, 0, obj);
    }

    Message obtainMessageISC(int i, int i2, int i3, IInputContextCallback iInputContextCallback) {
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg6 = iInputContextCallback;
        someArgsObtain.argi6 = i3;
        return this.mH.obtainMessage(i, i2, 0, someArgsObtain);
    }

    Message obtainMessageIISC(int i, int i2, int i3, int i4, IInputContextCallback iInputContextCallback) {
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg6 = iInputContextCallback;
        someArgsObtain.argi6 = i4;
        return this.mH.obtainMessage(i, i2, i3, someArgsObtain);
    }

    Message obtainMessageIOOSC(int i, int i2, Object obj, Object obj2, int i3, IInputContextCallback iInputContextCallback) {
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = obj;
        someArgsObtain.arg2 = obj2;
        someArgsObtain.arg6 = iInputContextCallback;
        someArgsObtain.argi6 = i3;
        return this.mH.obtainMessage(i, i2, 0, someArgsObtain);
    }

    Message obtainMessageIOSC(int i, int i2, Object obj, int i3, IInputContextCallback iInputContextCallback) {
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = obj;
        someArgsObtain.arg6 = iInputContextCallback;
        someArgsObtain.argi6 = i3;
        return this.mH.obtainMessage(i, i2, 0, someArgsObtain);
    }

    Message obtainMessageIO(int i, int i2, Object obj) {
        return this.mH.obtainMessage(i, i2, 0, obj);
    }

    Message obtainMessageOO(int i, Object obj, Object obj2) {
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = obj;
        someArgsObtain.arg2 = obj2;
        return this.mH.obtainMessage(i, 0, 0, someArgsObtain);
    }
}
