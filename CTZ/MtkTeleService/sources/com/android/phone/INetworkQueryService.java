package com.android.phone;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.phone.INetworkQueryServiceCallback;

public interface INetworkQueryService extends IInterface {
    void startNetworkQuery(INetworkQueryServiceCallback iNetworkQueryServiceCallback, int i, boolean z) throws RemoteException;

    void stopNetworkQuery() throws RemoteException;

    void unregisterCallback(INetworkQueryServiceCallback iNetworkQueryServiceCallback) throws RemoteException;

    public static abstract class Stub extends Binder implements INetworkQueryService {
        private static final String DESCRIPTOR = "com.android.phone.INetworkQueryService";
        static final int TRANSACTION_startNetworkQuery = 1;
        static final int TRANSACTION_stopNetworkQuery = 2;
        static final int TRANSACTION_unregisterCallback = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INetworkQueryService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof INetworkQueryService)) {
                return (INetworkQueryService) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            boolean z;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    INetworkQueryServiceCallback iNetworkQueryServiceCallbackAsInterface = INetworkQueryServiceCallback.Stub.asInterface(parcel.readStrongBinder());
                    int i3 = parcel.readInt();
                    if (parcel.readInt() == 0) {
                        z = false;
                    } else {
                        z = true;
                    }
                    startNetworkQuery(iNetworkQueryServiceCallbackAsInterface, i3, z);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    stopNetworkQuery();
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    unregisterCallback(INetworkQueryServiceCallback.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements INetworkQueryService {
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
            public void startNetworkQuery(INetworkQueryServiceCallback iNetworkQueryServiceCallback, int i, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iNetworkQueryServiceCallback != null ? iNetworkQueryServiceCallback.asBinder() : null);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stopNetworkQuery() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unregisterCallback(INetworkQueryServiceCallback iNetworkQueryServiceCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iNetworkQueryServiceCallback != null ? iNetworkQueryServiceCallback.asBinder() : null);
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
