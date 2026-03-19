package android.telephony;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface INetworkServiceCallback extends IInterface {
    void onGetNetworkRegistrationStateComplete(int i, NetworkRegistrationState networkRegistrationState) throws RemoteException;

    void onNetworkStateChanged() throws RemoteException;

    public static abstract class Stub extends Binder implements INetworkServiceCallback {
        private static final String DESCRIPTOR = "android.telephony.INetworkServiceCallback";
        static final int TRANSACTION_onGetNetworkRegistrationStateComplete = 1;
        static final int TRANSACTION_onNetworkStateChanged = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INetworkServiceCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof INetworkServiceCallback)) {
                return (INetworkServiceCallback) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            NetworkRegistrationState networkRegistrationStateCreateFromParcel;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    int i3 = parcel.readInt();
                    if (parcel.readInt() != 0) {
                        networkRegistrationStateCreateFromParcel = NetworkRegistrationState.CREATOR.createFromParcel(parcel);
                    } else {
                        networkRegistrationStateCreateFromParcel = null;
                    }
                    onGetNetworkRegistrationStateComplete(i3, networkRegistrationStateCreateFromParcel);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    onNetworkStateChanged();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements INetworkServiceCallback {
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
            public void onGetNetworkRegistrationStateComplete(int i, NetworkRegistrationState networkRegistrationState) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (networkRegistrationState != null) {
                        parcelObtain.writeInt(1);
                        networkRegistrationState.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onNetworkStateChanged() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
