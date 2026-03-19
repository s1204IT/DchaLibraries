package android.bluetooth;

import android.bluetooth.IBluetooth;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IBluetoothManagerCallback extends IInterface {
    void onBluetoothServiceDown() throws RemoteException;

    void onBluetoothServiceUp(IBluetooth iBluetooth) throws RemoteException;

    void onBrEdrDown() throws RemoteException;

    public static abstract class Stub extends Binder implements IBluetoothManagerCallback {
        private static final String DESCRIPTOR = "android.bluetooth.IBluetoothManagerCallback";
        static final int TRANSACTION_onBluetoothServiceDown = 2;
        static final int TRANSACTION_onBluetoothServiceUp = 1;
        static final int TRANSACTION_onBrEdrDown = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IBluetoothManagerCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IBluetoothManagerCallback)) {
                return (IBluetoothManagerCallback) iInterfaceQueryLocalInterface;
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
                    onBluetoothServiceUp(IBluetooth.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    onBluetoothServiceDown();
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    onBrEdrDown();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IBluetoothManagerCallback {
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
            public void onBluetoothServiceUp(IBluetooth iBluetooth) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBluetooth != null ? iBluetooth.asBinder() : null);
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onBluetoothServiceDown() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onBrEdrDown() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
