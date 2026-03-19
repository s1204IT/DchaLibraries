package com.android.bips.render;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import com.android.bips.jni.BackendConstants;
import com.android.bips.jni.SizeD;

public interface IPdfRender extends IInterface {
    void closeDocument() throws RemoteException;

    SizeD getPageSize(int i) throws RemoteException;

    int openDocument(ParcelFileDescriptor parcelFileDescriptor) throws RemoteException;

    ParcelFileDescriptor renderPageStripe(int i, int i2, int i3, int i4, double d) throws RemoteException;

    public static abstract class Stub extends Binder implements IPdfRender {
        public Stub() {
            attachInterface(this, "com.android.bips.render.IPdfRender");
        }

        public static IPdfRender asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.android.bips.render.IPdfRender");
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IPdfRender)) {
                return (IPdfRender) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            ParcelFileDescriptor parcelFileDescriptor;
            if (i == 1598968902) {
                parcel2.writeString("com.android.bips.render.IPdfRender");
                return true;
            }
            switch (i) {
                case BackendConstants.ALIGN_CENTER_HORIZONTAL:
                    parcel.enforceInterface("com.android.bips.render.IPdfRender");
                    if (parcel.readInt() != 0) {
                        parcelFileDescriptor = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                    } else {
                        parcelFileDescriptor = null;
                    }
                    int iOpenDocument = openDocument(parcelFileDescriptor);
                    parcel2.writeNoException();
                    parcel2.writeInt(iOpenDocument);
                    return true;
                case 2:
                    parcel.enforceInterface("com.android.bips.render.IPdfRender");
                    SizeD pageSize = getPageSize(parcel.readInt());
                    parcel2.writeNoException();
                    if (pageSize != null) {
                        parcel2.writeInt(1);
                        pageSize.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 3:
                    parcel.enforceInterface("com.android.bips.render.IPdfRender");
                    ParcelFileDescriptor parcelFileDescriptorRenderPageStripe = renderPageStripe(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readDouble());
                    parcel2.writeNoException();
                    if (parcelFileDescriptorRenderPageStripe != null) {
                        parcel2.writeInt(1);
                        parcelFileDescriptorRenderPageStripe.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 4:
                    parcel.enforceInterface("com.android.bips.render.IPdfRender");
                    closeDocument();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IPdfRender {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override
            public int openDocument(ParcelFileDescriptor parcelFileDescriptor) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.bips.render.IPdfRender");
                    if (parcelFileDescriptor != null) {
                        parcelObtain.writeInt(1);
                        parcelFileDescriptor.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public SizeD getPageSize(int i) throws RemoteException {
                SizeD sizeDCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.bips.render.IPdfRender");
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        sizeDCreateFromParcel = SizeD.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        sizeDCreateFromParcel = null;
                    }
                    return sizeDCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParcelFileDescriptor renderPageStripe(int i, int i2, int i3, int i4, double d) throws RemoteException {
                ParcelFileDescriptor parcelFileDescriptor;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.bips.render.IPdfRender");
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeInt(i4);
                    parcelObtain.writeDouble(d);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parcelFileDescriptor = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parcelFileDescriptor = null;
                    }
                    return parcelFileDescriptor;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void closeDocument() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.bips.render.IPdfRender");
                    this.mRemote.transact(4, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
