package com.mediatek.mmsdk.callback;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.Surface;
import com.mediatek.mmsdk.BaseParameters;
import java.util.List;

public interface ICallbackClient extends IInterface {
    int setOutputSurfaces(List<Surface> list, List<BaseParameters> list2) throws RemoteException;

    long start() throws RemoteException;

    long stop() throws RemoteException;

    public static abstract class Stub extends Binder implements ICallbackClient {
        private static final String DESCRIPTOR = "com.mediatek.mmsdk.callback.ICallbackClient";
        static final int TRANSACTION_setOutputSurfaces = 3;
        static final int TRANSACTION_start = 1;
        static final int TRANSACTION_stop = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ICallbackClient asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof ICallbackClient)) {
                return (ICallbackClient) iInterfaceQueryLocalInterface;
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
                    long jStart = start();
                    parcel2.writeNoException();
                    parcel2.writeLong(jStart);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    long jStop = stop();
                    parcel2.writeNoException();
                    parcel2.writeLong(jStop);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    int outputSurfaces = setOutputSurfaces(parcel.createTypedArrayList(Surface.CREATOR), parcel.createTypedArrayList(BaseParameters.CREATOR));
                    parcel2.writeNoException();
                    parcel2.writeInt(outputSurfaces);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements ICallbackClient {
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
            public long start() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readLong();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long stop() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readLong();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setOutputSurfaces(List<Surface> list, List<BaseParameters> list2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeTypedList(list);
                    parcelObtain.writeTypedList(list2);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
