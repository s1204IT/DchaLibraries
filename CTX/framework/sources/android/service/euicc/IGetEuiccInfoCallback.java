package android.service.euicc;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.euicc.EuiccInfo;

public interface IGetEuiccInfoCallback extends IInterface {
    void onSuccess(EuiccInfo euiccInfo) throws RemoteException;

    public static abstract class Stub extends Binder implements IGetEuiccInfoCallback {
        private static final String DESCRIPTOR = "android.service.euicc.IGetEuiccInfoCallback";
        static final int TRANSACTION_onSuccess = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IGetEuiccInfoCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IGetEuiccInfoCallback)) {
                return (IGetEuiccInfoCallback) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            EuiccInfo euiccInfoCreateFromParcel;
            if (i != 1) {
                if (i == 1598968902) {
                    parcel2.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(i, parcel, parcel2, i2);
            }
            parcel.enforceInterface(DESCRIPTOR);
            if (parcel.readInt() != 0) {
                euiccInfoCreateFromParcel = EuiccInfo.CREATOR.createFromParcel(parcel);
            } else {
                euiccInfoCreateFromParcel = null;
            }
            onSuccess(euiccInfoCreateFromParcel);
            return true;
        }

        private static class Proxy implements IGetEuiccInfoCallback {
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
            public void onSuccess(EuiccInfo euiccInfo) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (euiccInfo != null) {
                        parcelObtain.writeInt(1);
                        euiccInfo.writeToParcel(parcelObtain, 0);
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
