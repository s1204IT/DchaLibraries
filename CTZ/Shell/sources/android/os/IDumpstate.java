package android.os;

import android.os.IDumpstateListener;
import android.os.IDumpstateToken;

public interface IDumpstate extends IInterface {
    IDumpstateToken setListener(String str, IDumpstateListener iDumpstateListener, boolean z) throws RemoteException;

    public static abstract class Stub extends Binder implements IDumpstate {
        public static IDumpstate asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("android.os.IDumpstate");
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IDumpstate)) {
                return (IDumpstate) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            boolean z;
            if (i != 1) {
                if (i == 1598968902) {
                    parcel2.writeString("android.os.IDumpstate");
                    return true;
                }
                return super.onTransact(i, parcel, parcel2, i2);
            }
            parcel.enforceInterface("android.os.IDumpstate");
            String string = parcel.readString();
            IDumpstateListener iDumpstateListenerAsInterface = IDumpstateListener.Stub.asInterface(parcel.readStrongBinder());
            if (parcel.readInt() == 0) {
                z = false;
            } else {
                z = true;
            }
            IDumpstateToken listener = setListener(string, iDumpstateListenerAsInterface, z);
            parcel2.writeNoException();
            parcel2.writeStrongBinder(listener != null ? listener.asBinder() : null);
            return true;
        }

        private static class Proxy implements IDumpstate {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override
            public IDumpstateToken setListener(String str, IDumpstateListener iDumpstateListener, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.os.IDumpstate");
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iDumpstateListener != null ? iDumpstateListener.asBinder() : null);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IDumpstateToken.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
