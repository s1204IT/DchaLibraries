package android.content.pm.permission;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteCallback;
import android.os.RemoteException;

public interface IRuntimePermissionPresenter extends IInterface {
    void getAppPermissions(String str, RemoteCallback remoteCallback) throws RemoteException;

    void revokeRuntimePermission(String str, String str2) throws RemoteException;

    public static abstract class Stub extends Binder implements IRuntimePermissionPresenter {
        private static final String DESCRIPTOR = "android.content.pm.permission.IRuntimePermissionPresenter";
        static final int TRANSACTION_getAppPermissions = 1;
        static final int TRANSACTION_revokeRuntimePermission = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IRuntimePermissionPresenter asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IRuntimePermissionPresenter)) {
                return (IRuntimePermissionPresenter) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            RemoteCallback remoteCallbackCreateFromParcel;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string = parcel.readString();
                    if (parcel.readInt() != 0) {
                        remoteCallbackCreateFromParcel = RemoteCallback.CREATOR.createFromParcel(parcel);
                    } else {
                        remoteCallbackCreateFromParcel = null;
                    }
                    getAppPermissions(string, remoteCallbackCreateFromParcel);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    revokeRuntimePermission(parcel.readString(), parcel.readString());
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IRuntimePermissionPresenter {
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
            public void getAppPermissions(String str, RemoteCallback remoteCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (remoteCallback != null) {
                        parcelObtain.writeInt(1);
                        remoteCallback.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void revokeRuntimePermission(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
