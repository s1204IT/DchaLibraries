package android.os;

public interface IBatteryPropertiesListener extends IInterface {
    void batteryPropertiesChanged(BatteryProperties batteryProperties) throws RemoteException;

    public static abstract class Stub extends Binder implements IBatteryPropertiesListener {
        private static final String DESCRIPTOR = "android.os.IBatteryPropertiesListener";
        static final int TRANSACTION_batteryPropertiesChanged = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IBatteryPropertiesListener asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IBatteryPropertiesListener)) {
                return (IBatteryPropertiesListener) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            BatteryProperties batteryPropertiesCreateFromParcel;
            if (i != 1) {
                if (i == 1598968902) {
                    parcel2.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(i, parcel, parcel2, i2);
            }
            parcel.enforceInterface(DESCRIPTOR);
            if (parcel.readInt() != 0) {
                batteryPropertiesCreateFromParcel = BatteryProperties.CREATOR.createFromParcel(parcel);
            } else {
                batteryPropertiesCreateFromParcel = null;
            }
            batteryPropertiesChanged(batteryPropertiesCreateFromParcel);
            return true;
        }

        private static class Proxy implements IBatteryPropertiesListener {
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
            public void batteryPropertiesChanged(BatteryProperties batteryProperties) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (batteryProperties != null) {
                        parcelObtain.writeInt(1);
                        batteryProperties.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
