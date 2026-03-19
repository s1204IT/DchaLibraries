package android.service.dreams;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.IRemoteCallback;
import android.os.Parcel;
import android.os.RemoteException;

public interface IDreamService extends IInterface {
    void attach(IBinder iBinder, boolean z, IRemoteCallback iRemoteCallback) throws RemoteException;

    void detach() throws RemoteException;

    void wakeUp() throws RemoteException;

    public static abstract class Stub extends Binder implements IDreamService {
        private static final String DESCRIPTOR = "android.service.dreams.IDreamService";
        static final int TRANSACTION_attach = 1;
        static final int TRANSACTION_detach = 2;
        static final int TRANSACTION_wakeUp = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IDreamService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IDreamService)) {
                return (IDreamService) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            boolean z;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    IBinder strongBinder = parcel.readStrongBinder();
                    if (parcel.readInt() == 0) {
                        z = false;
                    } else {
                        z = true;
                    }
                    attach(strongBinder, z, IRemoteCallback.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    detach();
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    wakeUp();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IDreamService {
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
            public void attach(IBinder iBinder, boolean z, IRemoteCallback iRemoteCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeStrongBinder(iRemoteCallback != null ? iRemoteCallback.asBinder() : null);
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void detach() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void wakeUp() throws RemoteException {
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
