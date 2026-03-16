package com.android.internal.view;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.ExtractedTextRequest;
import com.android.internal.view.IInputContextCallback;

public interface IInputContext extends IInterface {
    void beginBatchEdit() throws RemoteException;

    void clearMetaKeyStates(int i) throws RemoteException;

    void commitCompletion(CompletionInfo completionInfo) throws RemoteException;

    void commitCorrection(CorrectionInfo correctionInfo) throws RemoteException;

    void commitText(CharSequence charSequence, int i) throws RemoteException;

    void deleteSurroundingText(int i, int i2) throws RemoteException;

    void endBatchEdit() throws RemoteException;

    void finishComposingText() throws RemoteException;

    void getCursorCapsMode(int i, int i2, IInputContextCallback iInputContextCallback) throws RemoteException;

    void getExtractedText(ExtractedTextRequest extractedTextRequest, int i, int i2, IInputContextCallback iInputContextCallback) throws RemoteException;

    void getSelectedText(int i, int i2, IInputContextCallback iInputContextCallback) throws RemoteException;

    void getTextAfterCursor(int i, int i2, int i3, IInputContextCallback iInputContextCallback) throws RemoteException;

    void getTextBeforeCursor(int i, int i2, int i3, IInputContextCallback iInputContextCallback) throws RemoteException;

    void performContextMenuAction(int i) throws RemoteException;

    void performEditorAction(int i) throws RemoteException;

    void performPrivateCommand(String str, Bundle bundle) throws RemoteException;

    void reportFullscreenMode(boolean z) throws RemoteException;

    void requestUpdateCursorAnchorInfo(int i, int i2, IInputContextCallback iInputContextCallback) throws RemoteException;

    void sendKeyEvent(KeyEvent keyEvent) throws RemoteException;

    void setComposingRegion(int i, int i2) throws RemoteException;

    void setComposingText(CharSequence charSequence, int i) throws RemoteException;

    void setSelection(int i, int i2) throws RemoteException;

    public static abstract class Stub extends Binder implements IInputContext {
        private static final String DESCRIPTOR = "com.android.internal.view.IInputContext";
        static final int TRANSACTION_beginBatchEdit = 14;
        static final int TRANSACTION_clearMetaKeyStates = 18;
        static final int TRANSACTION_commitCompletion = 9;
        static final int TRANSACTION_commitCorrection = 10;
        static final int TRANSACTION_commitText = 8;
        static final int TRANSACTION_deleteSurroundingText = 5;
        static final int TRANSACTION_endBatchEdit = 15;
        static final int TRANSACTION_finishComposingText = 7;
        static final int TRANSACTION_getCursorCapsMode = 3;
        static final int TRANSACTION_getExtractedText = 4;
        static final int TRANSACTION_getSelectedText = 21;
        static final int TRANSACTION_getTextAfterCursor = 2;
        static final int TRANSACTION_getTextBeforeCursor = 1;
        static final int TRANSACTION_performContextMenuAction = 13;
        static final int TRANSACTION_performEditorAction = 12;
        static final int TRANSACTION_performPrivateCommand = 19;
        static final int TRANSACTION_reportFullscreenMode = 16;
        static final int TRANSACTION_requestUpdateCursorAnchorInfo = 22;
        static final int TRANSACTION_sendKeyEvent = 17;
        static final int TRANSACTION_setComposingRegion = 20;
        static final int TRANSACTION_setComposingText = 6;
        static final int TRANSACTION_setSelection = 11;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IInputContext asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IInputContext)) {
                return (IInputContext) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Bundle _arg1;
            KeyEvent _arg0;
            CorrectionInfo _arg02;
            CompletionInfo _arg03;
            CharSequence _arg04;
            CharSequence _arg05;
            ExtractedTextRequest _arg06;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg07 = data.readInt();
                    int _arg12 = data.readInt();
                    int _arg2 = data.readInt();
                    IInputContextCallback _arg3 = IInputContextCallback.Stub.asInterface(data.readStrongBinder());
                    getTextBeforeCursor(_arg07, _arg12, _arg2, _arg3);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg08 = data.readInt();
                    int _arg13 = data.readInt();
                    int _arg22 = data.readInt();
                    IInputContextCallback _arg32 = IInputContextCallback.Stub.asInterface(data.readStrongBinder());
                    getTextAfterCursor(_arg08, _arg13, _arg22, _arg32);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg09 = data.readInt();
                    int _arg14 = data.readInt();
                    IInputContextCallback _arg23 = IInputContextCallback.Stub.asInterface(data.readStrongBinder());
                    getCursorCapsMode(_arg09, _arg14, _arg23);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg06 = ExtractedTextRequest.CREATOR.createFromParcel(data);
                    } else {
                        _arg06 = null;
                    }
                    int _arg15 = data.readInt();
                    int _arg24 = data.readInt();
                    IInputContextCallback _arg33 = IInputContextCallback.Stub.asInterface(data.readStrongBinder());
                    getExtractedText(_arg06, _arg15, _arg24, _arg33);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg010 = data.readInt();
                    int _arg16 = data.readInt();
                    deleteSurroundingText(_arg010, _arg16);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg05 = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(data);
                    } else {
                        _arg05 = null;
                    }
                    int _arg17 = data.readInt();
                    setComposingText(_arg05, _arg17);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    finishComposingText();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg04 = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(data);
                    } else {
                        _arg04 = null;
                    }
                    int _arg18 = data.readInt();
                    commitText(_arg04, _arg18);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg03 = CompletionInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg03 = null;
                    }
                    commitCompletion(_arg03);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = CorrectionInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    commitCorrection(_arg02);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg011 = data.readInt();
                    int _arg19 = data.readInt();
                    setSelection(_arg011, _arg19);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg012 = data.readInt();
                    performEditorAction(_arg012);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg013 = data.readInt();
                    performContextMenuAction(_arg013);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    beginBatchEdit();
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    endBatchEdit();
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg014 = data.readInt() != 0;
                    reportFullscreenMode(_arg014);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = KeyEvent.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    sendKeyEvent(_arg0);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg015 = data.readInt();
                    clearMetaKeyStates(_arg015);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg016 = data.readString();
                    if (data.readInt() != 0) {
                        _arg1 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    performPrivateCommand(_arg016, _arg1);
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg017 = data.readInt();
                    int _arg110 = data.readInt();
                    setComposingRegion(_arg017, _arg110);
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg018 = data.readInt();
                    int _arg111 = data.readInt();
                    IInputContextCallback _arg25 = IInputContextCallback.Stub.asInterface(data.readStrongBinder());
                    getSelectedText(_arg018, _arg111, _arg25);
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg019 = data.readInt();
                    int _arg112 = data.readInt();
                    IInputContextCallback _arg26 = IInputContextCallback.Stub.asInterface(data.readStrongBinder());
                    requestUpdateCursorAnchorInfo(_arg019, _arg112, _arg26);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IInputContext {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public void getTextBeforeCursor(int length, int flags, int seq, IInputContextCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(length);
                    _data.writeInt(flags);
                    _data.writeInt(seq);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void getTextAfterCursor(int length, int flags, int seq, IInputContextCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(length);
                    _data.writeInt(flags);
                    _data.writeInt(seq);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void getCursorCapsMode(int reqModes, int seq, IInputContextCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(reqModes);
                    _data.writeInt(seq);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void getExtractedText(ExtractedTextRequest request, int flags, int seq, IInputContextCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(flags);
                    _data.writeInt(seq);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void deleteSurroundingText(int leftLength, int rightLength) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(leftLength);
                    _data.writeInt(rightLength);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setComposingText(CharSequence text, int newCursorPosition) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (text != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(text, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(newCursorPosition);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void finishComposingText() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void commitText(CharSequence text, int newCursorPosition) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (text != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(text, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(newCursorPosition);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void commitCompletion(CompletionInfo completion) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (completion != null) {
                        _data.writeInt(1);
                        completion.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void commitCorrection(CorrectionInfo correction) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (correction != null) {
                        _data.writeInt(1);
                        correction.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setSelection(int start, int end) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(start);
                    _data.writeInt(end);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void performEditorAction(int actionCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(actionCode);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void performContextMenuAction(int id) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(id);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void beginBatchEdit() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void endBatchEdit() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void reportFullscreenMode(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enabled ? 1 : 0);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void sendKeyEvent(KeyEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (event != null) {
                        _data.writeInt(1);
                        event.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void clearMetaKeyStates(int states) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(states);
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void performPrivateCommand(String action, Bundle data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(action);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setComposingRegion(int start, int end) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(start);
                    _data.writeInt(end);
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void getSelectedText(int flags, int seq, IInputContextCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flags);
                    _data.writeInt(seq);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void requestUpdateCursorAnchorInfo(int cursorUpdateMode, int seq, IInputContextCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(cursorUpdateMode);
                    _data.writeInt(seq);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
