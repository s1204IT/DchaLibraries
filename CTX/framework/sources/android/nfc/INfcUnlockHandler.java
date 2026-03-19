package android.nfc;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface INfcUnlockHandler extends IInterface {
    boolean onUnlockAttempted(Tag tag) throws RemoteException;

    public static abstract class Stub extends Binder implements INfcUnlockHandler {
        private static final String DESCRIPTOR = "android.nfc.INfcUnlockHandler";
        static final int TRANSACTION_onUnlockAttempted = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INfcUnlockHandler asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof INfcUnlockHandler)) {
                return (INfcUnlockHandler) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            Tag tagCreateFromParcel;
            if (i != 1) {
                if (i == 1598968902) {
                    parcel2.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(i, parcel, parcel2, i2);
            }
            parcel.enforceInterface(DESCRIPTOR);
            if (parcel.readInt() != 0) {
                tagCreateFromParcel = Tag.CREATOR.createFromParcel(parcel);
            } else {
                tagCreateFromParcel = null;
            }
            boolean zOnUnlockAttempted = onUnlockAttempted(tagCreateFromParcel);
            parcel2.writeNoException();
            parcel2.writeInt(zOnUnlockAttempted ? 1 : 0);
            return true;
        }

        private static class Proxy implements INfcUnlockHandler {
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
            public boolean onUnlockAttempted(Tag tag) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (tag != null) {
                        parcelObtain.writeInt(1);
                        tag.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
