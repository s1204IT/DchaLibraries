package android.service.euicc;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IGetEuiccProfileInfoListCallback extends IInterface {
    void onComplete(GetEuiccProfileInfoListResult getEuiccProfileInfoListResult) throws RemoteException;

    public static abstract class Stub extends Binder implements IGetEuiccProfileInfoListCallback {
        private static final String DESCRIPTOR = "android.service.euicc.IGetEuiccProfileInfoListCallback";
        static final int TRANSACTION_onComplete = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IGetEuiccProfileInfoListCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IGetEuiccProfileInfoListCallback)) {
                return (IGetEuiccProfileInfoListCallback) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            GetEuiccProfileInfoListResult getEuiccProfileInfoListResultCreateFromParcel;
            if (i != 1) {
                if (i == 1598968902) {
                    parcel2.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(i, parcel, parcel2, i2);
            }
            parcel.enforceInterface(DESCRIPTOR);
            if (parcel.readInt() != 0) {
                getEuiccProfileInfoListResultCreateFromParcel = GetEuiccProfileInfoListResult.CREATOR.createFromParcel(parcel);
            } else {
                getEuiccProfileInfoListResultCreateFromParcel = null;
            }
            onComplete(getEuiccProfileInfoListResultCreateFromParcel);
            return true;
        }

        private static class Proxy implements IGetEuiccProfileInfoListCallback {
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
            public void onComplete(GetEuiccProfileInfoListResult getEuiccProfileInfoListResult) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (getEuiccProfileInfoListResult != null) {
                        parcelObtain.writeInt(1);
                        getEuiccProfileInfoListResult.writeToParcel(parcelObtain, 0);
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
