package com.android.internal.app;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.text.TextUtils;
import com.android.internal.app.IVoiceInteractorCallback;
import com.android.internal.app.IVoiceInteractorRequest;

public interface IVoiceInteractor extends IInterface {
    IVoiceInteractorRequest startAbortVoice(String str, IVoiceInteractorCallback iVoiceInteractorCallback, CharSequence charSequence, Bundle bundle) throws RemoteException;

    IVoiceInteractorRequest startCommand(String str, IVoiceInteractorCallback iVoiceInteractorCallback, String str2, Bundle bundle) throws RemoteException;

    IVoiceInteractorRequest startCompleteVoice(String str, IVoiceInteractorCallback iVoiceInteractorCallback, CharSequence charSequence, Bundle bundle) throws RemoteException;

    IVoiceInteractorRequest startConfirmation(String str, IVoiceInteractorCallback iVoiceInteractorCallback, CharSequence charSequence, Bundle bundle) throws RemoteException;

    boolean[] supportsCommands(String str, String[] strArr) throws RemoteException;

    public static abstract class Stub extends Binder implements IVoiceInteractor {
        private static final String DESCRIPTOR = "com.android.internal.app.IVoiceInteractor";
        static final int TRANSACTION_startAbortVoice = 3;
        static final int TRANSACTION_startCommand = 4;
        static final int TRANSACTION_startCompleteVoice = 2;
        static final int TRANSACTION_startConfirmation = 1;
        static final int TRANSACTION_supportsCommands = 5;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IVoiceInteractor asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IVoiceInteractor)) {
                return (IVoiceInteractor) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Bundle _arg3;
            CharSequence _arg2;
            Bundle _arg32;
            CharSequence _arg22;
            Bundle _arg33;
            CharSequence _arg23;
            Bundle _arg34;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0 = data.readString();
                    IVoiceInteractorCallback _arg1 = IVoiceInteractorCallback.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg23 = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(data);
                    } else {
                        _arg23 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg34 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg34 = null;
                    }
                    IVoiceInteractorRequest _result = startConfirmation(_arg0, _arg1, _arg23, _arg34);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result != null ? _result.asBinder() : null);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg02 = data.readString();
                    IVoiceInteractorCallback _arg12 = IVoiceInteractorCallback.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg22 = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(data);
                    } else {
                        _arg22 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg33 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg33 = null;
                    }
                    IVoiceInteractorRequest _result2 = startCompleteVoice(_arg02, _arg12, _arg22, _arg33);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result2 != null ? _result2.asBinder() : null);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    IVoiceInteractorCallback _arg13 = IVoiceInteractorCallback.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg2 = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg32 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg32 = null;
                    }
                    IVoiceInteractorRequest _result3 = startAbortVoice(_arg03, _arg13, _arg2, _arg32);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result3 != null ? _result3.asBinder() : null);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    IVoiceInteractorCallback _arg14 = IVoiceInteractorCallback.Stub.asInterface(data.readStrongBinder());
                    String _arg24 = data.readString();
                    if (data.readInt() != 0) {
                        _arg3 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg3 = null;
                    }
                    IVoiceInteractorRequest _result4 = startCommand(_arg04, _arg14, _arg24, _arg3);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result4 != null ? _result4.asBinder() : null);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    String[] _arg15 = data.createStringArray();
                    boolean[] _result5 = supportsCommands(_arg05, _arg15);
                    reply.writeNoException();
                    reply.writeBooleanArray(_result5);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IVoiceInteractor {
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
            public IVoiceInteractorRequest startConfirmation(String callingPackage, IVoiceInteractorCallback callback, CharSequence prompt, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    if (prompt != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(prompt, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    IVoiceInteractorRequest _result = IVoiceInteractorRequest.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IVoiceInteractorRequest startCompleteVoice(String callingPackage, IVoiceInteractorCallback callback, CharSequence message, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    if (message != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(message, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    IVoiceInteractorRequest _result = IVoiceInteractorRequest.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IVoiceInteractorRequest startAbortVoice(String callingPackage, IVoiceInteractorCallback callback, CharSequence message, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    if (message != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(message, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    IVoiceInteractorRequest _result = IVoiceInteractorRequest.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IVoiceInteractorRequest startCommand(String callingPackage, IVoiceInteractorCallback callback, String command, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeString(command);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    IVoiceInteractorRequest _result = IVoiceInteractorRequest.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean[] supportsCommands(String callingPackage, String[] commands) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeStringArray(commands);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    boolean[] _result = _reply.createBooleanArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
