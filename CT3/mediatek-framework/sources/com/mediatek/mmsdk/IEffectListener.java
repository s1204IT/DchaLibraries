package com.mediatek.mmsdk;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.mediatek.mmsdk.IEffectHalClient;

public interface IEffectListener extends IInterface {
    void onAborted(IEffectHalClient iEffectHalClient, BaseParameters baseParameters) throws RemoteException;

    void onCompleted(IEffectHalClient iEffectHalClient, BaseParameters baseParameters, long j) throws RemoteException;

    void onFailed(IEffectHalClient iEffectHalClient, BaseParameters baseParameters) throws RemoteException;

    void onInputFrameProcessed(IEffectHalClient iEffectHalClient, BaseParameters baseParameters, BaseParameters baseParameters2) throws RemoteException;

    void onOutputFrameProcessed(IEffectHalClient iEffectHalClient, BaseParameters baseParameters, BaseParameters baseParameters2) throws RemoteException;

    void onPrepared(IEffectHalClient iEffectHalClient, BaseParameters baseParameters) throws RemoteException;

    public static abstract class Stub extends Binder implements IEffectListener {
        private static final String DESCRIPTOR = "com.mediatek.mmsdk.IEffectListener";
        static final int TRANSACTION_onAborted = 5;
        static final int TRANSACTION_onCompleted = 4;
        static final int TRANSACTION_onFailed = 6;
        static final int TRANSACTION_onInputFrameProcessed = 2;
        static final int TRANSACTION_onOutputFrameProcessed = 3;
        static final int TRANSACTION_onPrepared = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IEffectListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IEffectListener)) {
                return (IEffectListener) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            BaseParameters baseParametersCreateFromParcel;
            BaseParameters baseParametersCreateFromParcel2;
            BaseParameters baseParametersCreateFromParcel3;
            BaseParameters baseParametersCreateFromParcel4;
            BaseParameters baseParametersCreateFromParcel5;
            BaseParameters baseParametersCreateFromParcel6;
            BaseParameters baseParametersCreateFromParcel7;
            BaseParameters baseParametersCreateFromParcel8;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    IEffectHalClient _arg0 = IEffectHalClient.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        baseParametersCreateFromParcel8 = BaseParameters.CREATOR.createFromParcel(data);
                    } else {
                        baseParametersCreateFromParcel8 = null;
                    }
                    onPrepared(_arg0, baseParametersCreateFromParcel8);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    IEffectHalClient _arg02 = IEffectHalClient.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        baseParametersCreateFromParcel6 = BaseParameters.CREATOR.createFromParcel(data);
                    } else {
                        baseParametersCreateFromParcel6 = null;
                    }
                    if (data.readInt() != 0) {
                        baseParametersCreateFromParcel7 = BaseParameters.CREATOR.createFromParcel(data);
                    } else {
                        baseParametersCreateFromParcel7 = null;
                    }
                    onInputFrameProcessed(_arg02, baseParametersCreateFromParcel6, baseParametersCreateFromParcel7);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    IEffectHalClient _arg03 = IEffectHalClient.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        baseParametersCreateFromParcel4 = BaseParameters.CREATOR.createFromParcel(data);
                    } else {
                        baseParametersCreateFromParcel4 = null;
                    }
                    if (data.readInt() != 0) {
                        baseParametersCreateFromParcel5 = BaseParameters.CREATOR.createFromParcel(data);
                    } else {
                        baseParametersCreateFromParcel5 = null;
                    }
                    onOutputFrameProcessed(_arg03, baseParametersCreateFromParcel4, baseParametersCreateFromParcel5);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    IEffectHalClient _arg04 = IEffectHalClient.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        baseParametersCreateFromParcel3 = BaseParameters.CREATOR.createFromParcel(data);
                    } else {
                        baseParametersCreateFromParcel3 = null;
                    }
                    long _arg2 = data.readLong();
                    onCompleted(_arg04, baseParametersCreateFromParcel3, _arg2);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    IEffectHalClient _arg05 = IEffectHalClient.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        baseParametersCreateFromParcel2 = BaseParameters.CREATOR.createFromParcel(data);
                    } else {
                        baseParametersCreateFromParcel2 = null;
                    }
                    onAborted(_arg05, baseParametersCreateFromParcel2);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    IEffectHalClient _arg06 = IEffectHalClient.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        baseParametersCreateFromParcel = BaseParameters.CREATOR.createFromParcel(data);
                    } else {
                        baseParametersCreateFromParcel = null;
                    }
                    onFailed(_arg06, baseParametersCreateFromParcel);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IEffectListener {
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
            public void onPrepared(IEffectHalClient effect, BaseParameters result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(effect != null ? effect.asBinder() : null);
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
            public void onInputFrameProcessed(IEffectHalClient effect, BaseParameters parameter, BaseParameters partialResult) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(effect != null ? effect.asBinder() : null);
                    if (parameter != null) {
                        _data.writeInt(1);
                        parameter.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (partialResult != null) {
                        _data.writeInt(1);
                        partialResult.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onOutputFrameProcessed(IEffectHalClient effect, BaseParameters parameter, BaseParameters partialResult) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(effect != null ? effect.asBinder() : null);
                    if (parameter != null) {
                        _data.writeInt(1);
                        parameter.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (partialResult != null) {
                        _data.writeInt(1);
                        partialResult.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onCompleted(IEffectHalClient effect, BaseParameters partialResult, long uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(effect != null ? effect.asBinder() : null);
                    if (partialResult != null) {
                        _data.writeInt(1);
                        partialResult.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(uid);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onAborted(IEffectHalClient effect, BaseParameters result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(effect != null ? effect.asBinder() : null);
                    if (result != null) {
                        _data.writeInt(1);
                        result.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onFailed(IEffectHalClient effect, BaseParameters result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(effect != null ? effect.asBinder() : null);
                    if (result != null) {
                        _data.writeInt(1);
                        result.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
