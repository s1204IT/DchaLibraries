package com.mediatek.net.connectivity;

import android.net.INetdEventCallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IMtkIpConnectivityMetrics extends IInterface {
    boolean registerMtkNetdEventCallback(INetdEventCallback iNetdEventCallback) throws RemoteException;

    boolean registerMtkSocketEventCallback(INetdEventCallback iNetdEventCallback) throws RemoteException;

    void setSpeedDownload(int i) throws RemoteException;

    boolean unregisterMtkNetdEventCallback() throws RemoteException;

    boolean unregisterMtkSocketEventCallback() throws RemoteException;

    void updateCtaAppStatus(int i, boolean z) throws RemoteException;

    public static abstract class Stub extends Binder implements IMtkIpConnectivityMetrics {
        private static final String DESCRIPTOR = "com.mediatek.net.connectivity.IMtkIpConnectivityMetrics";
        static final int TRANSACTION_registerMtkNetdEventCallback = 1;
        static final int TRANSACTION_registerMtkSocketEventCallback = 3;
        static final int TRANSACTION_setSpeedDownload = 6;
        static final int TRANSACTION_unregisterMtkNetdEventCallback = 2;
        static final int TRANSACTION_unregisterMtkSocketEventCallback = 4;
        static final int TRANSACTION_updateCtaAppStatus = 5;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMtkIpConnectivityMetrics asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IMtkIpConnectivityMetrics)) {
                return (IMtkIpConnectivityMetrics) iInterfaceQueryLocalInterface;
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
                    boolean zRegisterMtkNetdEventCallback = registerMtkNetdEventCallback(INetdEventCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(zRegisterMtkNetdEventCallback ? 1 : 0);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zUnregisterMtkNetdEventCallback = unregisterMtkNetdEventCallback();
                    parcel2.writeNoException();
                    parcel2.writeInt(zUnregisterMtkNetdEventCallback ? 1 : 0);
                    return true;
                case TRANSACTION_registerMtkSocketEventCallback:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zRegisterMtkSocketEventCallback = registerMtkSocketEventCallback(INetdEventCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(zRegisterMtkSocketEventCallback ? 1 : 0);
                    return true;
                case TRANSACTION_unregisterMtkSocketEventCallback:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zUnregisterMtkSocketEventCallback = unregisterMtkSocketEventCallback();
                    parcel2.writeNoException();
                    parcel2.writeInt(zUnregisterMtkSocketEventCallback ? 1 : 0);
                    return true;
                case TRANSACTION_updateCtaAppStatus:
                    parcel.enforceInterface(DESCRIPTOR);
                    int i3 = parcel.readInt();
                    if (parcel.readInt() == 0) {
                        z = false;
                    } else {
                        z = true;
                    }
                    updateCtaAppStatus(i3, z);
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_setSpeedDownload:
                    parcel.enforceInterface(DESCRIPTOR);
                    setSpeedDownload(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IMtkIpConnectivityMetrics {
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
            public boolean registerMtkNetdEventCallback(INetdEventCallback iNetdEventCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iNetdEventCallback != null ? iNetdEventCallback.asBinder() : null);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean unregisterMtkNetdEventCallback() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean registerMtkSocketEventCallback(INetdEventCallback iNetdEventCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iNetdEventCallback != null ? iNetdEventCallback.asBinder() : null);
                    this.mRemote.transact(Stub.TRANSACTION_registerMtkSocketEventCallback, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean unregisterMtkSocketEventCallback() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_unregisterMtkSocketEventCallback, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void updateCtaAppStatus(int i, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(Stub.TRANSACTION_updateCtaAppStatus, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setSpeedDownload(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_setSpeedDownload, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
