package com.android.internal.telephony.euicc;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.service.euicc.EuiccProfileInfo;

public interface ISwitchToProfileCallback extends IInterface {
    void onComplete(int i, EuiccProfileInfo euiccProfileInfo) throws RemoteException;

    public static abstract class Stub extends Binder implements ISwitchToProfileCallback {
        private static final String DESCRIPTOR = "com.android.internal.telephony.euicc.ISwitchToProfileCallback";
        static final int TRANSACTION_onComplete = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISwitchToProfileCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof ISwitchToProfileCallback)) {
                return (ISwitchToProfileCallback) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            EuiccProfileInfo euiccProfileInfoCreateFromParcel;
            if (i != 1) {
                if (i == 1598968902) {
                    parcel2.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(i, parcel, parcel2, i2);
            }
            parcel.enforceInterface(DESCRIPTOR);
            int i3 = parcel.readInt();
            if (parcel.readInt() != 0) {
                euiccProfileInfoCreateFromParcel = EuiccProfileInfo.CREATOR.createFromParcel(parcel);
            } else {
                euiccProfileInfoCreateFromParcel = null;
            }
            onComplete(i3, euiccProfileInfoCreateFromParcel);
            return true;
        }

        private static class Proxy implements ISwitchToProfileCallback {
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
            public void onComplete(int i, EuiccProfileInfo euiccProfileInfo) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (euiccProfileInfo != null) {
                        parcelObtain.writeInt(1);
                        euiccProfileInfo.writeToParcel(parcelObtain, 0);
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
