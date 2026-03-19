package android.print;

import android.content.pm.ParceledListSlice;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IPrinterDiscoveryObserver extends IInterface {
    void onPrintersAdded(ParceledListSlice parceledListSlice) throws RemoteException;

    void onPrintersRemoved(ParceledListSlice parceledListSlice) throws RemoteException;

    public static abstract class Stub extends Binder implements IPrinterDiscoveryObserver {
        private static final String DESCRIPTOR = "android.print.IPrinterDiscoveryObserver";
        static final int TRANSACTION_onPrintersAdded = 1;
        static final int TRANSACTION_onPrintersRemoved = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IPrinterDiscoveryObserver asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IPrinterDiscoveryObserver)) {
                return (IPrinterDiscoveryObserver) iInterfaceQueryLocalInterface;
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
                    onPrintersAdded(parcel.readInt() != 0 ? ParceledListSlice.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    onPrintersRemoved(parcel.readInt() != 0 ? ParceledListSlice.CREATOR.createFromParcel(parcel) : null);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IPrinterDiscoveryObserver {
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
            public void onPrintersAdded(ParceledListSlice parceledListSlice) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (parceledListSlice != null) {
                        parcelObtain.writeInt(1);
                        parceledListSlice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPrintersRemoved(ParceledListSlice parceledListSlice) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (parceledListSlice != null) {
                        parcelObtain.writeInt(1);
                        parceledListSlice.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
