package com.mediatek.gba;

import android.net.Network;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IGbaService extends IInterface {
    NafSessionKey getCachedKey(String str, byte[] bArr, int i) throws RemoteException;

    int getGbaSupported() throws RemoteException;

    int getGbaSupportedForSubscriber(int i) throws RemoteException;

    boolean isGbaKeyExpired(String str, byte[] bArr) throws RemoteException;

    boolean isGbaKeyExpiredForSubscriber(String str, byte[] bArr, int i) throws RemoteException;

    NafSessionKey runGbaAuthentication(String str, byte[] bArr, boolean z) throws RemoteException;

    NafSessionKey runGbaAuthenticationForSubscriber(String str, byte[] bArr, boolean z, int i) throws RemoteException;

    void setNetwork(Network network) throws RemoteException;

    void updateCachedKey(String str, byte[] bArr, int i, NafSessionKey nafSessionKey) throws RemoteException;

    public static abstract class Stub extends Binder implements IGbaService {
        private static final String DESCRIPTOR = "com.mediatek.gba.IGbaService";
        static final int TRANSACTION_getCachedKey = 8;
        static final int TRANSACTION_getGbaSupported = 1;
        static final int TRANSACTION_getGbaSupportedForSubscriber = 2;
        static final int TRANSACTION_isGbaKeyExpired = 3;
        static final int TRANSACTION_isGbaKeyExpiredForSubscriber = 4;
        static final int TRANSACTION_runGbaAuthentication = 5;
        static final int TRANSACTION_runGbaAuthenticationForSubscriber = 6;
        static final int TRANSACTION_setNetwork = 7;
        static final int TRANSACTION_updateCachedKey = 9;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IGbaService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IGbaService)) {
                return (IGbaService) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    int gbaSupported = getGbaSupported();
                    parcel2.writeNoException();
                    parcel2.writeInt(gbaSupported);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    int gbaSupportedForSubscriber = getGbaSupportedForSubscriber(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(gbaSupportedForSubscriber);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsGbaKeyExpired = isGbaKeyExpired(parcel.readString(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsGbaKeyExpired ? 1 : 0);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsGbaKeyExpiredForSubscriber = isGbaKeyExpiredForSubscriber(parcel.readString(), parcel.createByteArray(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsGbaKeyExpiredForSubscriber ? 1 : 0);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    NafSessionKey nafSessionKeyRunGbaAuthentication = runGbaAuthentication(parcel.readString(), parcel.createByteArray(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    if (nafSessionKeyRunGbaAuthentication != null) {
                        parcel2.writeInt(1);
                        nafSessionKeyRunGbaAuthentication.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    NafSessionKey nafSessionKeyRunGbaAuthenticationForSubscriber = runGbaAuthenticationForSubscriber(parcel.readString(), parcel.createByteArray(), parcel.readInt() != 0, parcel.readInt());
                    parcel2.writeNoException();
                    if (nafSessionKeyRunGbaAuthenticationForSubscriber != null) {
                        parcel2.writeInt(1);
                        nafSessionKeyRunGbaAuthenticationForSubscriber.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    setNetwork(parcel.readInt() != 0 ? (Network) Network.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    NafSessionKey cachedKey = getCachedKey(parcel.readString(), parcel.createByteArray(), parcel.readInt());
                    parcel2.writeNoException();
                    if (cachedKey != null) {
                        parcel2.writeInt(1);
                        cachedKey.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    updateCachedKey(parcel.readString(), parcel.createByteArray(), parcel.readInt(), parcel.readInt() != 0 ? NafSessionKey.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IGbaService {
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
            public int getGbaSupported() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getGbaSupportedForSubscriber(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isGbaKeyExpired(String str, byte[] bArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeByteArray(bArr);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isGbaKeyExpiredForSubscriber(String str, byte[] bArr, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public NafSessionKey runGbaAuthentication(String str, byte[] bArr, boolean z) throws RemoteException {
                NafSessionKey nafSessionKeyCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        nafSessionKeyCreateFromParcel = NafSessionKey.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        nafSessionKeyCreateFromParcel = null;
                    }
                    return nafSessionKeyCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public NafSessionKey runGbaAuthenticationForSubscriber(String str, byte[] bArr, boolean z, int i) throws RemoteException {
                NafSessionKey nafSessionKeyCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        nafSessionKeyCreateFromParcel = NafSessionKey.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        nafSessionKeyCreateFromParcel = null;
                    }
                    return nafSessionKeyCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setNetwork(Network network) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (network != null) {
                        parcelObtain.writeInt(1);
                        network.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public NafSessionKey getCachedKey(String str, byte[] bArr, int i) throws RemoteException {
                NafSessionKey nafSessionKeyCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        nafSessionKeyCreateFromParcel = NafSessionKey.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        nafSessionKeyCreateFromParcel = null;
                    }
                    return nafSessionKeyCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void updateCachedKey(String str, byte[] bArr, int i, NafSessionKey nafSessionKey) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeInt(i);
                    if (nafSessionKey != null) {
                        parcelObtain.writeInt(1);
                        nafSessionKey.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
