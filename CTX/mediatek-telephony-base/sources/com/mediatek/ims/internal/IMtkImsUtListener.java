package com.mediatek.ims.internal;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.mediatek.ims.MtkImsCallForwardInfo;
import com.mediatek.ims.internal.IMtkImsUt;

public interface IMtkImsUtListener extends IInterface {
    void utConfigurationCallForwardInTimeSlotQueried(IMtkImsUt iMtkImsUt, int i, MtkImsCallForwardInfo[] mtkImsCallForwardInfoArr) throws RemoteException;

    public static abstract class Stub extends Binder implements IMtkImsUtListener {
        private static final String DESCRIPTOR = "com.mediatek.ims.internal.IMtkImsUtListener";
        static final int TRANSACTION_utConfigurationCallForwardInTimeSlotQueried = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMtkImsUtListener asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IMtkImsUtListener)) {
                return (IMtkImsUtListener) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i != 1) {
                if (i == 1598968902) {
                    parcel2.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(i, parcel, parcel2, i2);
            }
            parcel.enforceInterface(DESCRIPTOR);
            utConfigurationCallForwardInTimeSlotQueried(IMtkImsUt.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), (MtkImsCallForwardInfo[]) parcel.createTypedArray(MtkImsCallForwardInfo.CREATOR));
            parcel2.writeNoException();
            return true;
        }

        private static class Proxy implements IMtkImsUtListener {
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
            public void utConfigurationCallForwardInTimeSlotQueried(IMtkImsUt iMtkImsUt, int i, MtkImsCallForwardInfo[] mtkImsCallForwardInfoArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iMtkImsUt != null ? iMtkImsUt.asBinder() : null);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeTypedArray(mtkImsCallForwardInfoArr, 0);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
