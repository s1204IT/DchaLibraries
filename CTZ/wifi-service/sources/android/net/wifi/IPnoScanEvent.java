package android.net.wifi;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IPnoScanEvent extends IInterface {
    public static final int PNO_SCAN_OVER_OFFLOAD_BINDER_FAILURE = 0;
    public static final int PNO_SCAN_OVER_OFFLOAD_REMOTE_FAILURE = 1;

    void OnPnoNetworkFound() throws RemoteException;

    void OnPnoScanFailed() throws RemoteException;

    void OnPnoScanOverOffloadFailed(int i) throws RemoteException;

    void OnPnoScanOverOffloadStarted() throws RemoteException;

    public static abstract class Stub extends Binder implements IPnoScanEvent {
        private static final String DESCRIPTOR = "android.net.wifi.IPnoScanEvent";
        static final int TRANSACTION_OnPnoNetworkFound = 1;
        static final int TRANSACTION_OnPnoScanFailed = 2;
        static final int TRANSACTION_OnPnoScanOverOffloadFailed = 4;
        static final int TRANSACTION_OnPnoScanOverOffloadStarted = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IPnoScanEvent asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IPnoScanEvent)) {
                return (IPnoScanEvent) iInterfaceQueryLocalInterface;
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
                    OnPnoNetworkFound();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    OnPnoScanFailed();
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    OnPnoScanOverOffloadStarted();
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    OnPnoScanOverOffloadFailed(parcel.readInt());
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IPnoScanEvent {
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
            public void OnPnoNetworkFound() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void OnPnoScanFailed() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void OnPnoScanOverOffloadStarted() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void OnPnoScanOverOffloadFailed(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(4, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
