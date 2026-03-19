package com.android.ims;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ImsConfigListener extends IInterface {
    void onGetFeatureResponse(int i, int i2, int i3, int i4) throws RemoteException;

    void onGetVideoQuality(int i, int i2) throws RemoteException;

    void onSetFeatureResponse(int i, int i2, int i3, int i4) throws RemoteException;

    void onSetVideoQuality(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements ImsConfigListener {
        private static final String DESCRIPTOR = "com.android.ims.ImsConfigListener";
        static final int TRANSACTION_onGetFeatureResponse = 1;
        static final int TRANSACTION_onGetVideoQuality = 3;
        static final int TRANSACTION_onSetFeatureResponse = 2;
        static final int TRANSACTION_onSetVideoQuality = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ImsConfigListener asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof ImsConfigListener)) {
                return (ImsConfigListener) iInterfaceQueryLocalInterface;
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
                    onGetFeatureResponse(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    onSetFeatureResponse(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    onGetVideoQuality(parcel.readInt(), parcel.readInt());
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    onSetVideoQuality(parcel.readInt());
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements ImsConfigListener {
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
            public void onGetFeatureResponse(int i, int i2, int i3, int i4) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeInt(i4);
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onSetFeatureResponse(int i, int i2, int i3, int i4) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeInt(i4);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onGetVideoQuality(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onSetVideoQuality(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(4, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
