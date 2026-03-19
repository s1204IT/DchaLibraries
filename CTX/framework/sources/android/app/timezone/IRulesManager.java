package android.app.timezone;

import android.app.timezone.ICallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

public interface IRulesManager extends IInterface {
    RulesState getRulesState() throws RemoteException;

    int requestInstall(ParcelFileDescriptor parcelFileDescriptor, byte[] bArr, ICallback iCallback) throws RemoteException;

    void requestNothing(byte[] bArr, boolean z) throws RemoteException;

    int requestUninstall(byte[] bArr, ICallback iCallback) throws RemoteException;

    public static abstract class Stub extends Binder implements IRulesManager {
        private static final String DESCRIPTOR = "android.app.timezone.IRulesManager";
        static final int TRANSACTION_getRulesState = 1;
        static final int TRANSACTION_requestInstall = 2;
        static final int TRANSACTION_requestNothing = 4;
        static final int TRANSACTION_requestUninstall = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IRulesManager asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IRulesManager)) {
                return (IRulesManager) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            ParcelFileDescriptor parcelFileDescriptorCreateFromParcel;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    RulesState rulesState = getRulesState();
                    parcel2.writeNoException();
                    if (rulesState != null) {
                        parcel2.writeInt(1);
                        rulesState.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        parcelFileDescriptorCreateFromParcel = ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                    } else {
                        parcelFileDescriptorCreateFromParcel = null;
                    }
                    int iRequestInstall = requestInstall(parcelFileDescriptorCreateFromParcel, parcel.createByteArray(), ICallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(iRequestInstall);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iRequestUninstall = requestUninstall(parcel.createByteArray(), ICallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(iRequestUninstall);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    requestNothing(parcel.createByteArray(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IRulesManager {
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
            public RulesState getRulesState() throws RemoteException {
                RulesState rulesStateCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        rulesStateCreateFromParcel = RulesState.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        rulesStateCreateFromParcel = null;
                    }
                    return rulesStateCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int requestInstall(ParcelFileDescriptor parcelFileDescriptor, byte[] bArr, ICallback iCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (parcelFileDescriptor != null) {
                        parcelObtain.writeInt(1);
                        parcelFileDescriptor.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeStrongBinder(iCallback != null ? iCallback.asBinder() : null);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int requestUninstall(byte[] bArr, ICallback iCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeStrongBinder(iCallback != null ? iCallback.asBinder() : null);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void requestNothing(byte[] bArr, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
