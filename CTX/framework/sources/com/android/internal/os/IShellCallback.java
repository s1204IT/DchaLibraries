package com.android.internal.os;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

public interface IShellCallback extends IInterface {
    ParcelFileDescriptor openFile(String str, String str2, String str3) throws RemoteException;

    public static abstract class Stub extends Binder implements IShellCallback {
        private static final String DESCRIPTOR = "com.android.internal.os.IShellCallback";
        static final int TRANSACTION_openFile = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IShellCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IShellCallback)) {
                return (IShellCallback) iInterfaceQueryLocalInterface;
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
            ParcelFileDescriptor parcelFileDescriptorOpenFile = openFile(parcel.readString(), parcel.readString(), parcel.readString());
            parcel2.writeNoException();
            if (parcelFileDescriptorOpenFile != null) {
                parcel2.writeInt(1);
                parcelFileDescriptorOpenFile.writeToParcel(parcel2, 1);
            } else {
                parcel2.writeInt(0);
            }
            return true;
        }

        private static class Proxy implements IShellCallback {
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
            public ParcelFileDescriptor openFile(String str, String str2, String str3) throws RemoteException {
                ParcelFileDescriptor parcelFileDescriptorCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parcelFileDescriptorCreateFromParcel = ParcelFileDescriptor.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parcelFileDescriptorCreateFromParcel = null;
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
