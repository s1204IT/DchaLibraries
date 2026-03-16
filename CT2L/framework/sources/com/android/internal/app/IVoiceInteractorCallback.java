package com.android.internal.app;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.internal.app.IVoiceInteractorRequest;

public interface IVoiceInteractorCallback extends IInterface {
    void deliverAbortVoiceResult(IVoiceInteractorRequest iVoiceInteractorRequest, Bundle bundle) throws RemoteException;

    void deliverCancel(IVoiceInteractorRequest iVoiceInteractorRequest) throws RemoteException;

    void deliverCommandResult(IVoiceInteractorRequest iVoiceInteractorRequest, boolean z, Bundle bundle) throws RemoteException;

    void deliverCompleteVoiceResult(IVoiceInteractorRequest iVoiceInteractorRequest, Bundle bundle) throws RemoteException;

    void deliverConfirmationResult(IVoiceInteractorRequest iVoiceInteractorRequest, boolean z, Bundle bundle) throws RemoteException;

    public static abstract class Stub extends Binder implements IVoiceInteractorCallback {
        private static final String DESCRIPTOR = "com.android.internal.app.IVoiceInteractorCallback";
        static final int TRANSACTION_deliverAbortVoiceResult = 3;
        static final int TRANSACTION_deliverCancel = 5;
        static final int TRANSACTION_deliverCommandResult = 4;
        static final int TRANSACTION_deliverCompleteVoiceResult = 2;
        static final int TRANSACTION_deliverConfirmationResult = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IVoiceInteractorCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IVoiceInteractorCallback)) {
                return (IVoiceInteractorCallback) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            boolean _arg1;
            Bundle _arg2;
            Bundle _arg12;
            Bundle _arg13;
            Bundle _arg22;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    IVoiceInteractorRequest _arg0 = IVoiceInteractorRequest.Stub.asInterface(data.readStrongBinder());
                    _arg1 = data.readInt() != 0;
                    if (data.readInt() != 0) {
                        _arg22 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg22 = null;
                    }
                    deliverConfirmationResult(_arg0, _arg1, _arg22);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    IVoiceInteractorRequest _arg02 = IVoiceInteractorRequest.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg13 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg13 = null;
                    }
                    deliverCompleteVoiceResult(_arg02, _arg13);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    IVoiceInteractorRequest _arg03 = IVoiceInteractorRequest.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg12 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg12 = null;
                    }
                    deliverAbortVoiceResult(_arg03, _arg12);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    IVoiceInteractorRequest _arg04 = IVoiceInteractorRequest.Stub.asInterface(data.readStrongBinder());
                    _arg1 = data.readInt() != 0;
                    if (data.readInt() != 0) {
                        _arg2 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    deliverCommandResult(_arg04, _arg1, _arg2);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    IVoiceInteractorRequest _arg05 = IVoiceInteractorRequest.Stub.asInterface(data.readStrongBinder());
                    deliverCancel(_arg05);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IVoiceInteractorCallback {
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
            public void deliverConfirmationResult(IVoiceInteractorRequest request, boolean confirmed, Bundle result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(request != null ? request.asBinder() : null);
                    _data.writeInt(confirmed ? 1 : 0);
                    if (result != null) {
                        _data.writeInt(1);
                        result.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void deliverCompleteVoiceResult(IVoiceInteractorRequest request, Bundle result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(request != null ? request.asBinder() : null);
                    if (result != null) {
                        _data.writeInt(1);
                        result.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void deliverAbortVoiceResult(IVoiceInteractorRequest request, Bundle result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(request != null ? request.asBinder() : null);
                    if (result != null) {
                        _data.writeInt(1);
                        result.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void deliverCommandResult(IVoiceInteractorRequest request, boolean complete, Bundle result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(request != null ? request.asBinder() : null);
                    _data.writeInt(complete ? 1 : 0);
                    if (result != null) {
                        _data.writeInt(1);
                        result.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void deliverCancel(IVoiceInteractorRequest request) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(request != null ? request.asBinder() : null);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
