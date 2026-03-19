package android.content;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.RemoteException;

public interface IRestrictionsManager extends IInterface {
    Intent createLocalApprovalIntent() throws RemoteException;

    Bundle getApplicationRestrictions(String str) throws RemoteException;

    boolean hasRestrictionsProvider() throws RemoteException;

    void notifyPermissionResponse(String str, PersistableBundle persistableBundle) throws RemoteException;

    void requestPermission(String str, String str2, String str3, PersistableBundle persistableBundle) throws RemoteException;

    public static abstract class Stub extends Binder implements IRestrictionsManager {
        private static final String DESCRIPTOR = "android.content.IRestrictionsManager";
        static final int TRANSACTION_createLocalApprovalIntent = 5;
        static final int TRANSACTION_getApplicationRestrictions = 1;
        static final int TRANSACTION_hasRestrictionsProvider = 2;
        static final int TRANSACTION_notifyPermissionResponse = 4;
        static final int TRANSACTION_requestPermission = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IRestrictionsManager asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IRestrictionsManager)) {
                return (IRestrictionsManager) iInterfaceQueryLocalInterface;
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
                    Bundle applicationRestrictions = getApplicationRestrictions(parcel.readString());
                    parcel2.writeNoException();
                    if (applicationRestrictions != null) {
                        parcel2.writeInt(1);
                        applicationRestrictions.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zHasRestrictionsProvider = hasRestrictionsProvider();
                    parcel2.writeNoException();
                    parcel2.writeInt(zHasRestrictionsProvider ? 1 : 0);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    requestPermission(parcel.readString(), parcel.readString(), parcel.readString(), parcel.readInt() != 0 ? PersistableBundle.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    notifyPermissionResponse(parcel.readString(), parcel.readInt() != 0 ? PersistableBundle.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    Intent intentCreateLocalApprovalIntent = createLocalApprovalIntent();
                    parcel2.writeNoException();
                    if (intentCreateLocalApprovalIntent != null) {
                        parcel2.writeInt(1);
                        intentCreateLocalApprovalIntent.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IRestrictionsManager {
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
            public Bundle getApplicationRestrictions(String str) throws RemoteException {
                Bundle bundleCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        bundleCreateFromParcel = null;
                    }
                    return bundleCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean hasRestrictionsProvider() throws RemoteException {
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
            public void requestPermission(String str, String str2, String str3, PersistableBundle persistableBundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    if (persistableBundle != null) {
                        parcelObtain.writeInt(1);
                        persistableBundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyPermissionResponse(String str, PersistableBundle persistableBundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (persistableBundle != null) {
                        parcelObtain.writeInt(1);
                        persistableBundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Intent createLocalApprovalIntent() throws RemoteException {
                Intent intentCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        intentCreateFromParcel = Intent.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        intentCreateFromParcel = null;
                    }
                    return intentCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
