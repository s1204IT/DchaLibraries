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

        public static IEffectListener asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IEffectListener)) {
                return (IEffectListener) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            BaseParameters baseParametersCreateFromParcel;
            BaseParameters baseParametersCreateFromParcel2;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    onPrepared(IEffectHalClient.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? BaseParameters.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    IEffectHalClient iEffectHalClientAsInterface = IEffectHalClient.Stub.asInterface(parcel.readStrongBinder());
                    if (parcel.readInt() != 0) {
                        baseParametersCreateFromParcel = BaseParameters.CREATOR.createFromParcel(parcel);
                    } else {
                        baseParametersCreateFromParcel = null;
                    }
                    onInputFrameProcessed(iEffectHalClientAsInterface, baseParametersCreateFromParcel, parcel.readInt() != 0 ? BaseParameters.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    IEffectHalClient iEffectHalClientAsInterface2 = IEffectHalClient.Stub.asInterface(parcel.readStrongBinder());
                    if (parcel.readInt() != 0) {
                        baseParametersCreateFromParcel2 = BaseParameters.CREATOR.createFromParcel(parcel);
                    } else {
                        baseParametersCreateFromParcel2 = null;
                    }
                    onOutputFrameProcessed(iEffectHalClientAsInterface2, baseParametersCreateFromParcel2, parcel.readInt() != 0 ? BaseParameters.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    onCompleted(IEffectHalClient.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? BaseParameters.CREATOR.createFromParcel(parcel) : null, parcel.readLong());
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    onAborted(IEffectHalClient.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? BaseParameters.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    onFailed(IEffectHalClient.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? BaseParameters.CREATOR.createFromParcel(parcel) : null);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IEffectListener {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public void onPrepared(IEffectHalClient iEffectHalClient, BaseParameters baseParameters) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iEffectHalClient != null ? iEffectHalClient.asBinder() : null);
                    if (baseParameters != null) {
                        parcelObtain.writeInt(1);
                        baseParameters.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onInputFrameProcessed(IEffectHalClient iEffectHalClient, BaseParameters baseParameters, BaseParameters baseParameters2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iEffectHalClient != null ? iEffectHalClient.asBinder() : null);
                    if (baseParameters != null) {
                        parcelObtain.writeInt(1);
                        baseParameters.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (baseParameters2 != null) {
                        parcelObtain.writeInt(1);
                        baseParameters2.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onOutputFrameProcessed(IEffectHalClient iEffectHalClient, BaseParameters baseParameters, BaseParameters baseParameters2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iEffectHalClient != null ? iEffectHalClient.asBinder() : null);
                    if (baseParameters != null) {
                        parcelObtain.writeInt(1);
                        baseParameters.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (baseParameters2 != null) {
                        parcelObtain.writeInt(1);
                        baseParameters2.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onCompleted(IEffectHalClient iEffectHalClient, BaseParameters baseParameters, long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iEffectHalClient != null ? iEffectHalClient.asBinder() : null);
                    if (baseParameters != null) {
                        parcelObtain.writeInt(1);
                        baseParameters.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(4, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onAborted(IEffectHalClient iEffectHalClient, BaseParameters baseParameters) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iEffectHalClient != null ? iEffectHalClient.asBinder() : null);
                    if (baseParameters != null) {
                        parcelObtain.writeInt(1);
                        baseParameters.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(5, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onFailed(IEffectHalClient iEffectHalClient, BaseParameters baseParameters) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iEffectHalClient != null ? iEffectHalClient.asBinder() : null);
                    if (baseParameters != null) {
                        parcelObtain.writeInt(1);
                        baseParameters.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(6, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
