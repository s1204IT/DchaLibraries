package android.app.trust;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.text.TextUtils;

public interface ITrustListener extends IInterface {
    void onTrustChanged(boolean z, int i, int i2) throws RemoteException;

    void onTrustError(CharSequence charSequence) throws RemoteException;

    void onTrustManagedChanged(boolean z, int i) throws RemoteException;

    public static abstract class Stub extends Binder implements ITrustListener {
        private static final String DESCRIPTOR = "android.app.trust.ITrustListener";
        static final int TRANSACTION_onTrustChanged = 1;
        static final int TRANSACTION_onTrustError = 3;
        static final int TRANSACTION_onTrustManagedChanged = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ITrustListener asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof ITrustListener)) {
                return (ITrustListener) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            CharSequence charSequenceCreateFromParcel;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    onTrustChanged(parcel.readInt() != 0, parcel.readInt(), parcel.readInt());
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    onTrustManagedChanged(parcel.readInt() != 0, parcel.readInt());
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        charSequenceCreateFromParcel = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
                    } else {
                        charSequenceCreateFromParcel = null;
                    }
                    onTrustError(charSequenceCreateFromParcel);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements ITrustListener {
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
            public void onTrustChanged(boolean z, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onTrustManagedChanged(boolean z, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onTrustError(CharSequence charSequence) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (charSequence != null) {
                        parcelObtain.writeInt(1);
                        TextUtils.writeToParcel(charSequence, parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
