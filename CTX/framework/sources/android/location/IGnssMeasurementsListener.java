package android.location;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IGnssMeasurementsListener extends IInterface {
    void onGnssMeasurementsReceived(GnssMeasurementsEvent gnssMeasurementsEvent) throws RemoteException;

    void onStatusChanged(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IGnssMeasurementsListener {
        private static final String DESCRIPTOR = "android.location.IGnssMeasurementsListener";
        static final int TRANSACTION_onGnssMeasurementsReceived = 1;
        static final int TRANSACTION_onStatusChanged = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IGnssMeasurementsListener asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IGnssMeasurementsListener)) {
                return (IGnssMeasurementsListener) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            GnssMeasurementsEvent gnssMeasurementsEventCreateFromParcel;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        gnssMeasurementsEventCreateFromParcel = GnssMeasurementsEvent.CREATOR.createFromParcel(parcel);
                    } else {
                        gnssMeasurementsEventCreateFromParcel = null;
                    }
                    onGnssMeasurementsReceived(gnssMeasurementsEventCreateFromParcel);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    onStatusChanged(parcel.readInt());
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IGnssMeasurementsListener {
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
            public void onGnssMeasurementsReceived(GnssMeasurementsEvent gnssMeasurementsEvent) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (gnssMeasurementsEvent != null) {
                        parcelObtain.writeInt(1);
                        gnssMeasurementsEvent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onStatusChanged(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
