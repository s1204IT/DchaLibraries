package android.view;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.view.IGraphicsStatsCallback;

public interface IGraphicsStats extends IInterface {
    ParcelFileDescriptor requestBufferForProcess(String str, IGraphicsStatsCallback iGraphicsStatsCallback) throws RemoteException;

    public static abstract class Stub extends Binder implements IGraphicsStats {
        private static final String DESCRIPTOR = "android.view.IGraphicsStats";
        static final int TRANSACTION_requestBufferForProcess = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IGraphicsStats asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IGraphicsStats)) {
                return (IGraphicsStats) iInterfaceQueryLocalInterface;
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
            ParcelFileDescriptor parcelFileDescriptorRequestBufferForProcess = requestBufferForProcess(parcel.readString(), IGraphicsStatsCallback.Stub.asInterface(parcel.readStrongBinder()));
            parcel2.writeNoException();
            if (parcelFileDescriptorRequestBufferForProcess != null) {
                parcel2.writeInt(1);
                parcelFileDescriptorRequestBufferForProcess.writeToParcel(parcel2, 1);
            } else {
                parcel2.writeInt(0);
            }
            return true;
        }

        private static class Proxy implements IGraphicsStats {
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
            public ParcelFileDescriptor requestBufferForProcess(String str, IGraphicsStatsCallback iGraphicsStatsCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    ParcelFileDescriptor parcelFileDescriptorCreateFromParcel = null;
                    parcelObtain.writeStrongBinder(iGraphicsStatsCallback != null ? iGraphicsStatsCallback.asBinder() : null);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parcelFileDescriptorCreateFromParcel = ParcelFileDescriptor.CREATOR.createFromParcel(parcelObtain2);
                    }
                    return parcelFileDescriptorCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
