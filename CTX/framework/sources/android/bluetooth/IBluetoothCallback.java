package android.bluetooth;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IBluetoothCallback extends IInterface {
    void onBluetoothStateChange(int i, int i2) throws RemoteException;

    public static abstract class Stub extends Binder implements IBluetoothCallback {
        private static final String DESCRIPTOR = "android.bluetooth.IBluetoothCallback";
        static final int TRANSACTION_onBluetoothStateChange = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IBluetoothCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IBluetoothCallback)) {
                return (IBluetoothCallback) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i != 1) {
                if (i == 1598968902) {
                    parcel2.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(i, parcel, parcel2, i2);
            }
            parcel.enforceInterface(DESCRIPTOR);
            onBluetoothStateChange(parcel.readInt(), parcel.readInt());
            parcel2.writeNoException();
            return true;
        }

        private static class Proxy implements IBluetoothCallback {
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
            public void onBluetoothStateChange(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
