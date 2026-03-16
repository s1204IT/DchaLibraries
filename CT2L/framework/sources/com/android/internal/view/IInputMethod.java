package com.android.internal.view;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.view.InputChannel;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputBinding;
import android.view.inputmethod.InputMethodSubtype;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethodSession;
import com.android.internal.view.IInputSessionCallback;

public interface IInputMethod extends IInterface {
    void attachToken(IBinder iBinder) throws RemoteException;

    void bindInput(InputBinding inputBinding) throws RemoteException;

    void changeInputMethodSubtype(InputMethodSubtype inputMethodSubtype) throws RemoteException;

    void createSession(InputChannel inputChannel, IInputSessionCallback iInputSessionCallback) throws RemoteException;

    void hideSoftInput(int i, ResultReceiver resultReceiver) throws RemoteException;

    void restartInput(IInputContext iInputContext, EditorInfo editorInfo) throws RemoteException;

    void revokeSession(IInputMethodSession iInputMethodSession) throws RemoteException;

    void setSessionEnabled(IInputMethodSession iInputMethodSession, boolean z) throws RemoteException;

    void showSoftInput(int i, ResultReceiver resultReceiver) throws RemoteException;

    void startInput(IInputContext iInputContext, EditorInfo editorInfo) throws RemoteException;

    void unbindInput() throws RemoteException;

    public static abstract class Stub extends Binder implements IInputMethod {
        private static final String DESCRIPTOR = "com.android.internal.view.IInputMethod";
        static final int TRANSACTION_attachToken = 1;
        static final int TRANSACTION_bindInput = 2;
        static final int TRANSACTION_changeInputMethodSubtype = 11;
        static final int TRANSACTION_createSession = 6;
        static final int TRANSACTION_hideSoftInput = 10;
        static final int TRANSACTION_restartInput = 5;
        static final int TRANSACTION_revokeSession = 8;
        static final int TRANSACTION_setSessionEnabled = 7;
        static final int TRANSACTION_showSoftInput = 9;
        static final int TRANSACTION_startInput = 4;
        static final int TRANSACTION_unbindInput = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IInputMethod asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IInputMethod)) {
                return (IInputMethod) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            InputMethodSubtype _arg0;
            ResultReceiver _arg1;
            ResultReceiver _arg12;
            InputChannel _arg02;
            EditorInfo _arg13;
            EditorInfo _arg14;
            InputBinding _arg03;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _arg04 = data.readStrongBinder();
                    attachToken(_arg04);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg03 = InputBinding.CREATOR.createFromParcel(data);
                    } else {
                        _arg03 = null;
                    }
                    bindInput(_arg03);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    unbindInput();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    IInputContext _arg05 = IInputContext.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg14 = EditorInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg14 = null;
                    }
                    startInput(_arg05, _arg14);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    IInputContext _arg06 = IInputContext.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg13 = EditorInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg13 = null;
                    }
                    restartInput(_arg06, _arg13);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = InputChannel.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    IInputSessionCallback _arg15 = IInputSessionCallback.Stub.asInterface(data.readStrongBinder());
                    createSession(_arg02, _arg15);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    IInputMethodSession _arg07 = IInputMethodSession.Stub.asInterface(data.readStrongBinder());
                    boolean _arg16 = data.readInt() != 0;
                    setSessionEnabled(_arg07, _arg16);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    IInputMethodSession _arg08 = IInputMethodSession.Stub.asInterface(data.readStrongBinder());
                    revokeSession(_arg08);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg09 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg12 = ResultReceiver.CREATOR.createFromParcel(data);
                    } else {
                        _arg12 = null;
                    }
                    showSoftInput(_arg09, _arg12);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg010 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg1 = ResultReceiver.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    hideSoftInput(_arg010, _arg1);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = InputMethodSubtype.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    changeInputMethodSubtype(_arg0);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IInputMethod {
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
            public void attachToken(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void bindInput(InputBinding binding) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (binding != null) {
                        _data.writeInt(1);
                        binding.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void unbindInput() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void startInput(IInputContext inputContext, EditorInfo attribute) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(inputContext != null ? inputContext.asBinder() : null);
                    if (attribute != null) {
                        _data.writeInt(1);
                        attribute.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void restartInput(IInputContext inputContext, EditorInfo attribute) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(inputContext != null ? inputContext.asBinder() : null);
                    if (attribute != null) {
                        _data.writeInt(1);
                        attribute.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void createSession(InputChannel channel, IInputSessionCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (channel != null) {
                        _data.writeInt(1);
                        channel.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setSessionEnabled(IInputMethodSession session, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeInt(enabled ? 1 : 0);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void revokeSession(IInputMethodSession session) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void showSoftInput(int flags, ResultReceiver resultReceiver) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flags);
                    if (resultReceiver != null) {
                        _data.writeInt(1);
                        resultReceiver.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void hideSoftInput(int flags, ResultReceiver resultReceiver) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flags);
                    if (resultReceiver != null) {
                        _data.writeInt(1);
                        resultReceiver.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void changeInputMethodSubtype(InputMethodSubtype subtype) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (subtype != null) {
                        _data.writeInt(1);
                        subtype.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
