package android.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ITetheringStatsProvider extends IInterface {
    public static final int QUOTA_UNLIMITED = -1;

    NetworkStats getTetherStats(int i) throws RemoteException;

    void setInterfaceQuota(String str, long j) throws RemoteException;

    public static abstract class Stub extends Binder implements ITetheringStatsProvider {
        private static final String DESCRIPTOR = "android.net.ITetheringStatsProvider";
        static final int TRANSACTION_getTetherStats = 1;
        static final int TRANSACTION_setInterfaceQuota = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ITetheringStatsProvider asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof ITetheringStatsProvider)) {
                return (ITetheringStatsProvider) iInterfaceQueryLocalInterface;
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
                    NetworkStats tetherStats = getTetherStats(parcel.readInt());
                    parcel2.writeNoException();
                    if (tetherStats != null) {
                        parcel2.writeInt(1);
                        tetherStats.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    setInterfaceQuota(parcel.readString(), parcel.readLong());
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements ITetheringStatsProvider {
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
            public NetworkStats getTetherStats(int i) throws RemoteException {
                NetworkStats networkStatsCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        networkStatsCreateFromParcel = NetworkStats.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        networkStatsCreateFromParcel = null;
                    }
                    return networkStatsCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setInterfaceQuota(String str, long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
