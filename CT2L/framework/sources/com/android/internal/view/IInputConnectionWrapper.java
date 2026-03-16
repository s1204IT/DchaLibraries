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
import com.android.internal.view.IInputContext;
import java.lang.ref.WeakReference;

public class IInputConnectionWrapper extends IInputContext.Stub {
    private static final int DO_BEGIN_BATCH_EDIT = 90;
    private static final int DO_CLEAR_META_KEY_STATES = 130;
    private static final int DO_COMMIT_COMPLETION = 55;
    private static final int DO_COMMIT_CORRECTION = 56;
    private static final int DO_COMMIT_TEXT = 50;
    private static final int DO_DELETE_SURROUNDING_TEXT = 80;
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
    private static final int DO_REPORT_FULLSCREEN_MODE = 100;
    private static final int DO_REQUEST_UPDATE_CURSOR_ANCHOR_INFO = 140;
    private static final int DO_SEND_KEY_EVENT = 70;
    private static final int DO_SET_COMPOSING_REGION = 63;
    private static final int DO_SET_COMPOSING_TEXT = 60;
    private static final int DO_SET_SELECTION = 57;
    static final String TAG = "IInputConnectionWrapper";
    private Handler mH;
    private WeakReference<InputConnection> mInputConnection;
    private Looper mMainLooper;

    static class SomeArgs {
        Object arg1;
        Object arg2;
        IInputContextCallback callback;
        int seq;

        SomeArgs() {
        }
    }

    class MyHandler extends Handler {
        MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            IInputConnectionWrapper.this.executeMessage(msg);
        }
    }

    public IInputConnectionWrapper(Looper mainLooper, InputConnection conn) {
        this.mInputConnection = new WeakReference<>(conn);
        this.mMainLooper = mainLooper;
        this.mH = new MyHandler(this.mMainLooper);
    }

    public boolean isActive() {
        return true;
    }

    @Override
    public void getTextAfterCursor(int length, int flags, int seq, IInputContextCallback callback) {
        dispatchMessage(obtainMessageIISC(10, length, flags, seq, callback));
    }

    @Override
    public void getTextBeforeCursor(int length, int flags, int seq, IInputContextCallback callback) {
        dispatchMessage(obtainMessageIISC(20, length, flags, seq, callback));
    }

    @Override
    public void getSelectedText(int flags, int seq, IInputContextCallback callback) {
        dispatchMessage(obtainMessageISC(25, flags, seq, callback));
    }

    @Override
    public void getCursorCapsMode(int reqModes, int seq, IInputContextCallback callback) {
        dispatchMessage(obtainMessageISC(30, reqModes, seq, callback));
    }

    @Override
    public void getExtractedText(ExtractedTextRequest request, int flags, int seq, IInputContextCallback callback) {
        dispatchMessage(obtainMessageIOSC(40, flags, request, seq, callback));
    }

    @Override
    public void commitText(CharSequence text, int newCursorPosition) {
        dispatchMessage(obtainMessageIO(50, newCursorPosition, text));
    }

    @Override
    public void commitCompletion(CompletionInfo text) {
        dispatchMessage(obtainMessageO(55, text));
    }

    @Override
    public void commitCorrection(CorrectionInfo info) {
        dispatchMessage(obtainMessageO(56, info));
    }

    @Override
    public void setSelection(int start, int end) {
        dispatchMessage(obtainMessageII(57, start, end));
    }

    @Override
    public void performEditorAction(int id) {
        dispatchMessage(obtainMessageII(58, id, 0));
    }

    @Override
    public void performContextMenuAction(int id) {
        dispatchMessage(obtainMessageII(59, id, 0));
    }

    @Override
    public void setComposingRegion(int start, int end) {
        dispatchMessage(obtainMessageII(63, start, end));
    }

    @Override
    public void setComposingText(CharSequence text, int newCursorPosition) {
        dispatchMessage(obtainMessageIO(60, newCursorPosition, text));
    }

    @Override
    public void finishComposingText() {
        dispatchMessage(obtainMessage(65));
    }

    @Override
    public void sendKeyEvent(KeyEvent event) {
        dispatchMessage(obtainMessageO(70, event));
    }

    @Override
    public void clearMetaKeyStates(int states) {
        dispatchMessage(obtainMessageII(130, states, 0));
    }

    @Override
    public void deleteSurroundingText(int leftLength, int rightLength) {
        dispatchMessage(obtainMessageII(80, leftLength, rightLength));
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
    public void reportFullscreenMode(boolean enabled) {
        dispatchMessage(obtainMessageII(100, enabled ? 1 : 0, 0));
    }

    @Override
    public void performPrivateCommand(String action, Bundle data) {
        dispatchMessage(obtainMessageOO(120, action, data));
    }

    @Override
    public void requestUpdateCursorAnchorInfo(int cursorUpdateMode, int seq, IInputContextCallback callback) {
        dispatchMessage(obtainMessageISC(140, cursorUpdateMode, seq, callback));
    }

    void dispatchMessage(Message msg) {
        if (Looper.myLooper() == this.mMainLooper) {
            executeMessage(msg);
            msg.recycle();
        } else {
            this.mH.sendMessage(msg);
        }
    }

    void executeMessage(Message msg) {
        switch (msg.what) {
            case 10:
                SomeArgs args = (SomeArgs) msg.obj;
                try {
                    InputConnection ic = this.mInputConnection.get();
                    if (ic == null || !isActive()) {
                        Log.w(TAG, "getTextAfterCursor on inactive InputConnection");
                        args.callback.setTextAfterCursor(null, args.seq);
                    } else {
                        args.callback.setTextAfterCursor(ic.getTextAfterCursor(msg.arg1, msg.arg2), args.seq);
                    }
                } catch (RemoteException e) {
                    Log.w(TAG, "Got RemoteException calling setTextAfterCursor", e);
                    return;
                }
                break;
            case 20:
                SomeArgs args2 = (SomeArgs) msg.obj;
                try {
                    InputConnection ic2 = this.mInputConnection.get();
                    if (ic2 == null || !isActive()) {
                        Log.w(TAG, "getTextBeforeCursor on inactive InputConnection");
                        args2.callback.setTextBeforeCursor(null, args2.seq);
                    } else {
                        args2.callback.setTextBeforeCursor(ic2.getTextBeforeCursor(msg.arg1, msg.arg2), args2.seq);
                    }
                } catch (RemoteException e2) {
                    Log.w(TAG, "Got RemoteException calling setTextBeforeCursor", e2);
                    return;
                }
                break;
            case 25:
                SomeArgs args3 = (SomeArgs) msg.obj;
                try {
                    InputConnection ic3 = this.mInputConnection.get();
                    if (ic3 == null || !isActive()) {
                        Log.w(TAG, "getSelectedText on inactive InputConnection");
                        args3.callback.setSelectedText(null, args3.seq);
                    } else {
                        args3.callback.setSelectedText(ic3.getSelectedText(msg.arg1), args3.seq);
                    }
                } catch (RemoteException e3) {
                    Log.w(TAG, "Got RemoteException calling setSelectedText", e3);
                    return;
                }
                break;
            case 30:
                SomeArgs args4 = (SomeArgs) msg.obj;
                try {
                    InputConnection ic4 = this.mInputConnection.get();
                    if (ic4 == null || !isActive()) {
                        Log.w(TAG, "getCursorCapsMode on inactive InputConnection");
                        args4.callback.setCursorCapsMode(0, args4.seq);
                    } else {
                        args4.callback.setCursorCapsMode(ic4.getCursorCapsMode(msg.arg1), args4.seq);
                    }
                } catch (RemoteException e4) {
                    Log.w(TAG, "Got RemoteException calling setCursorCapsMode", e4);
                    return;
                }
                break;
            case 40:
                SomeArgs args5 = (SomeArgs) msg.obj;
                try {
                    InputConnection ic5 = this.mInputConnection.get();
                    if (ic5 == null || !isActive()) {
                        Log.w(TAG, "getExtractedText on inactive InputConnection");
                        args5.callback.setExtractedText(null, args5.seq);
                    } else {
                        args5.callback.setExtractedText(ic5.getExtractedText((ExtractedTextRequest) args5.arg1, msg.arg1), args5.seq);
                    }
                } catch (RemoteException e5) {
                    Log.w(TAG, "Got RemoteException calling setExtractedText", e5);
                    return;
                }
                break;
            case 50:
                InputConnection ic6 = this.mInputConnection.get();
                if (ic6 == null || !isActive()) {
                    Log.w(TAG, "commitText on inactive InputConnection");
                } else {
                    ic6.commitText((CharSequence) msg.obj, msg.arg1);
                }
                break;
            case 55:
                InputConnection ic7 = this.mInputConnection.get();
                if (ic7 == null || !isActive()) {
                    Log.w(TAG, "commitCompletion on inactive InputConnection");
                } else {
                    ic7.commitCompletion((CompletionInfo) msg.obj);
                }
                break;
            case 56:
                InputConnection ic8 = this.mInputConnection.get();
                if (ic8 == null || !isActive()) {
                    Log.w(TAG, "commitCorrection on inactive InputConnection");
                } else {
                    ic8.commitCorrection((CorrectionInfo) msg.obj);
                }
                break;
            case 57:
                InputConnection ic9 = this.mInputConnection.get();
                if (ic9 == null || !isActive()) {
                    Log.w(TAG, "setSelection on inactive InputConnection");
                } else {
                    ic9.setSelection(msg.arg1, msg.arg2);
                }
                break;
            case 58:
                InputConnection ic10 = this.mInputConnection.get();
                if (ic10 == null || !isActive()) {
                    Log.w(TAG, "performEditorAction on inactive InputConnection");
                } else {
                    ic10.performEditorAction(msg.arg1);
                }
                break;
            case 59:
                InputConnection ic11 = this.mInputConnection.get();
                if (ic11 == null || !isActive()) {
                    Log.w(TAG, "performContextMenuAction on inactive InputConnection");
                } else {
                    ic11.performContextMenuAction(msg.arg1);
                }
                break;
            case 60:
                InputConnection ic12 = this.mInputConnection.get();
                if (ic12 == null || !isActive()) {
                    Log.w(TAG, "setComposingText on inactive InputConnection");
                } else {
                    ic12.setComposingText((CharSequence) msg.obj, msg.arg1);
                }
                break;
            case 63:
                InputConnection ic13 = this.mInputConnection.get();
                if (ic13 == null || !isActive()) {
                    Log.w(TAG, "setComposingRegion on inactive InputConnection");
                } else {
                    ic13.setComposingRegion(msg.arg1, msg.arg2);
                }
                break;
            case 65:
                InputConnection ic14 = this.mInputConnection.get();
                if (ic14 == null) {
                    Log.w(TAG, "finishComposingText on inactive InputConnection");
                } else {
                    ic14.finishComposingText();
                }
                break;
            case 70:
                InputConnection ic15 = this.mInputConnection.get();
                if (ic15 == null || !isActive()) {
                    Log.w(TAG, "sendKeyEvent on inactive InputConnection");
                } else {
                    ic15.sendKeyEvent((KeyEvent) msg.obj);
                }
                break;
            case 80:
                InputConnection ic16 = this.mInputConnection.get();
                if (ic16 == null || !isActive()) {
                    Log.w(TAG, "deleteSurroundingText on inactive InputConnection");
                } else {
                    ic16.deleteSurroundingText(msg.arg1, msg.arg2);
                }
                break;
            case 90:
                InputConnection ic17 = this.mInputConnection.get();
                if (ic17 == null || !isActive()) {
                    Log.w(TAG, "beginBatchEdit on inactive InputConnection");
                } else {
                    ic17.beginBatchEdit();
                }
                break;
            case 95:
                InputConnection ic18 = this.mInputConnection.get();
                if (ic18 == null || !isActive()) {
                    Log.w(TAG, "endBatchEdit on inactive InputConnection");
                } else {
                    ic18.endBatchEdit();
                }
                break;
            case 100:
                InputConnection ic19 = this.mInputConnection.get();
                if (ic19 == null || !isActive()) {
                    Log.w(TAG, "showStatusIcon on inactive InputConnection");
                } else {
                    ic19.reportFullscreenMode(msg.arg1 == 1);
                }
                break;
            case 120:
                InputConnection ic20 = this.mInputConnection.get();
                if (ic20 == null || !isActive()) {
                    Log.w(TAG, "performPrivateCommand on inactive InputConnection");
                } else {
                    SomeArgs args6 = (SomeArgs) msg.obj;
                    ic20.performPrivateCommand((String) args6.arg1, (Bundle) args6.arg2);
                }
                break;
            case 130:
                InputConnection ic21 = this.mInputConnection.get();
                if (ic21 == null || !isActive()) {
                    Log.w(TAG, "clearMetaKeyStates on inactive InputConnection");
                } else {
                    ic21.clearMetaKeyStates(msg.arg1);
                }
                break;
            case 140:
                SomeArgs args7 = (SomeArgs) msg.obj;
                try {
                    InputConnection ic22 = this.mInputConnection.get();
                    if (ic22 == null || !isActive()) {
                        Log.w(TAG, "requestCursorAnchorInfo on inactive InputConnection");
                        args7.callback.setRequestUpdateCursorAnchorInfoResult(false, args7.seq);
                    } else {
                        args7.callback.setRequestUpdateCursorAnchorInfoResult(ic22.requestCursorUpdates(msg.arg1), args7.seq);
                    }
                } catch (RemoteException e6) {
                    Log.w(TAG, "Got RemoteException calling requestCursorAnchorInfo", e6);
                    return;
                }
                break;
            default:
                Log.w(TAG, "Unhandled message code: " + msg.what);
                break;
        }
    }

    Message obtainMessage(int what) {
        return this.mH.obtainMessage(what);
    }

    Message obtainMessageII(int what, int arg1, int arg2) {
        return this.mH.obtainMessage(what, arg1, arg2);
    }

    Message obtainMessageO(int what, Object arg1) {
        return this.mH.obtainMessage(what, 0, 0, arg1);
    }

    Message obtainMessageISC(int what, int arg1, int seq, IInputContextCallback callback) {
        SomeArgs args = new SomeArgs();
        args.callback = callback;
        args.seq = seq;
        return this.mH.obtainMessage(what, arg1, 0, args);
    }

    Message obtainMessageIISC(int what, int arg1, int arg2, int seq, IInputContextCallback callback) {
        SomeArgs args = new SomeArgs();
        args.callback = callback;
        args.seq = seq;
        return this.mH.obtainMessage(what, arg1, arg2, args);
    }

    Message obtainMessageOSC(int what, Object arg1, int seq, IInputContextCallback callback) {
        SomeArgs args = new SomeArgs();
        args.arg1 = arg1;
        args.callback = callback;
        args.seq = seq;
        return this.mH.obtainMessage(what, 0, 0, args);
    }

    Message obtainMessageIOSC(int what, int arg1, Object arg2, int seq, IInputContextCallback callback) {
        SomeArgs args = new SomeArgs();
        args.arg1 = arg2;
        args.callback = callback;
        args.seq = seq;
        return this.mH.obtainMessage(what, arg1, 0, args);
    }

    Message obtainMessageIO(int what, int arg1, Object arg2) {
        return this.mH.obtainMessage(what, arg1, 0, arg2);
    }

    Message obtainMessageOO(int what, Object arg1, Object arg2) {
        SomeArgs args = new SomeArgs();
        args.arg1 = arg1;
        args.arg2 = arg2;
        return this.mH.obtainMessage(what, 0, 0, args);
    }
}
