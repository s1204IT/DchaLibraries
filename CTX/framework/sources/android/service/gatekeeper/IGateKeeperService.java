package android.service.gatekeeper;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IGateKeeperService extends IInterface {
    void clearSecureUserId(int i) throws RemoteException;

    GateKeeperResponse enroll(int i, byte[] bArr, byte[] bArr2, byte[] bArr3) throws RemoteException;

    long getSecureUserId(int i) throws RemoteException;

    void reportDeviceSetupComplete() throws RemoteException;

    GateKeeperResponse verify(int i, byte[] bArr, byte[] bArr2) throws RemoteException;

    GateKeeperResponse verifyChallenge(int i, long j, byte[] bArr, byte[] bArr2) throws RemoteException;

    public static abstract class Stub extends Binder implements IGateKeeperService {
        private static final String DESCRIPTOR = "android.service.gatekeeper.IGateKeeperService";
        static final int TRANSACTION_clearSecureUserId = 5;
        static final int TRANSACTION_enroll = 1;
        static final int TRANSACTION_getSecureUserId = 4;
        static final int TRANSACTION_reportDeviceSetupComplete = 6;
        static final int TRANSACTION_verify = 2;
        static final int TRANSACTION_verifyChallenge = 3;

        public Stub() {
            attachInterface(this, "android.service.gatekeeper.IGateKeeperService");
        }

        public static IGateKeeperService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("android.service.gatekeeper.IGateKeeperService");
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IGateKeeperService)) {
                return (IGateKeeperService) iInterfaceQueryLocalInterface;
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
                parcel2.writeString("android.service.gatekeeper.IGateKeeperService");
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface("android.service.gatekeeper.IGateKeeperService");
                    GateKeeperResponse gateKeeperResponseEnroll = enroll(parcel.readInt(), parcel.createByteArray(), parcel.createByteArray(), parcel.createByteArray());
                    parcel2.writeNoException();
                    if (gateKeeperResponseEnroll != null) {
                        parcel2.writeInt(1);
                        gateKeeperResponseEnroll.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 2:
                    parcel.enforceInterface("android.service.gatekeeper.IGateKeeperService");
                    GateKeeperResponse gateKeeperResponseVerify = verify(parcel.readInt(), parcel.createByteArray(), parcel.createByteArray());
                    parcel2.writeNoException();
                    if (gateKeeperResponseVerify != null) {
                        parcel2.writeInt(1);
                        gateKeeperResponseVerify.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 3:
                    parcel.enforceInterface("android.service.gatekeeper.IGateKeeperService");
                    GateKeeperResponse gateKeeperResponseVerifyChallenge = verifyChallenge(parcel.readInt(), parcel.readLong(), parcel.createByteArray(), parcel.createByteArray());
                    parcel2.writeNoException();
                    if (gateKeeperResponseVerifyChallenge != null) {
                        parcel2.writeInt(1);
                        gateKeeperResponseVerifyChallenge.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 4:
                    parcel.enforceInterface("android.service.gatekeeper.IGateKeeperService");
                    long secureUserId = getSecureUserId(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeLong(secureUserId);
                    return true;
                case 5:
                    parcel.enforceInterface("android.service.gatekeeper.IGateKeeperService");
                    clearSecureUserId(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 6:
                    parcel.enforceInterface("android.service.gatekeeper.IGateKeeperService");
                    reportDeviceSetupComplete();
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IGateKeeperService {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return "android.service.gatekeeper.IGateKeeperService";
            }

            @Override
            public GateKeeperResponse enroll(int i, byte[] bArr, byte[] bArr2, byte[] bArr3) throws RemoteException {
                GateKeeperResponse gateKeeperResponseCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.service.gatekeeper.IGateKeeperService");
                    parcelObtain.writeInt(i);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeByteArray(bArr2);
                    parcelObtain.writeByteArray(bArr3);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        gateKeeperResponseCreateFromParcel = GateKeeperResponse.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        gateKeeperResponseCreateFromParcel = null;
                    }
                    return gateKeeperResponseCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public GateKeeperResponse verify(int i, byte[] bArr, byte[] bArr2) throws RemoteException {
                GateKeeperResponse gateKeeperResponseCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.service.gatekeeper.IGateKeeperService");
                    parcelObtain.writeInt(i);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeByteArray(bArr2);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        gateKeeperResponseCreateFromParcel = GateKeeperResponse.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        gateKeeperResponseCreateFromParcel = null;
                    }
                    return gateKeeperResponseCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public GateKeeperResponse verifyChallenge(int i, long j, byte[] bArr, byte[] bArr2) throws RemoteException {
                GateKeeperResponse gateKeeperResponseCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.service.gatekeeper.IGateKeeperService");
                    parcelObtain.writeInt(i);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeByteArray(bArr2);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        gateKeeperResponseCreateFromParcel = GateKeeperResponse.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        gateKeeperResponseCreateFromParcel = null;
                    }
                    return gateKeeperResponseCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long getSecureUserId(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.service.gatekeeper.IGateKeeperService");
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readLong();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void clearSecureUserId(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.service.gatekeeper.IGateKeeperService");
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void reportDeviceSetupComplete() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.service.gatekeeper.IGateKeeperService");
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
