package android.service.euicc;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IGetDownloadableSubscriptionMetadataCallback extends IInterface {
    void onComplete(GetDownloadableSubscriptionMetadataResult getDownloadableSubscriptionMetadataResult) throws RemoteException;

    public static abstract class Stub extends Binder implements IGetDownloadableSubscriptionMetadataCallback {
        private static final String DESCRIPTOR = "android.service.euicc.IGetDownloadableSubscriptionMetadataCallback";
        static final int TRANSACTION_onComplete = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IGetDownloadableSubscriptionMetadataCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IGetDownloadableSubscriptionMetadataCallback)) {
                return (IGetDownloadableSubscriptionMetadataCallback) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            GetDownloadableSubscriptionMetadataResult getDownloadableSubscriptionMetadataResultCreateFromParcel;
            if (i != 1) {
                if (i == 1598968902) {
                    parcel2.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(i, parcel, parcel2, i2);
            }
            parcel.enforceInterface(DESCRIPTOR);
            if (parcel.readInt() != 0) {
                getDownloadableSubscriptionMetadataResultCreateFromParcel = GetDownloadableSubscriptionMetadataResult.CREATOR.createFromParcel(parcel);
            } else {
                getDownloadableSubscriptionMetadataResultCreateFromParcel = null;
            }
            onComplete(getDownloadableSubscriptionMetadataResultCreateFromParcel);
            return true;
        }

        private static class Proxy implements IGetDownloadableSubscriptionMetadataCallback {
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
            public void onComplete(GetDownloadableSubscriptionMetadataResult getDownloadableSubscriptionMetadataResult) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (getDownloadableSubscriptionMetadataResult != null) {
                        parcelObtain.writeInt(1);
                        getDownloadableSubscriptionMetadataResult.writeToParcel(parcelObtain, 0);
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
