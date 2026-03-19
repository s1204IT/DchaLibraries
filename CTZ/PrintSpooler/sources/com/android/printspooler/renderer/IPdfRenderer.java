package com.android.printspooler.renderer;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.print.PrintAttributes;

public interface IPdfRenderer extends IInterface {
    void closeDocument() throws RemoteException;

    int openDocument(ParcelFileDescriptor parcelFileDescriptor) throws RemoteException;

    void renderPage(int i, int i2, int i3, PrintAttributes printAttributes, ParcelFileDescriptor parcelFileDescriptor) throws RemoteException;

    public static abstract class Stub extends Binder implements IPdfRenderer {
        public Stub() {
            attachInterface(this, "com.android.printspooler.renderer.IPdfRenderer");
        }

        public static IPdfRenderer asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.android.printspooler.renderer.IPdfRenderer");
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IPdfRenderer)) {
                return (IPdfRenderer) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            PrintAttributes printAttributes;
            if (i == 1598968902) {
                parcel2.writeString("com.android.printspooler.renderer.IPdfRenderer");
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface("com.android.printspooler.renderer.IPdfRenderer");
                    int iOpenDocument = openDocument(parcel.readInt() != 0 ? (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(iOpenDocument);
                    return true;
                case 2:
                    parcel.enforceInterface("com.android.printspooler.renderer.IPdfRenderer");
                    int i3 = parcel.readInt();
                    int i4 = parcel.readInt();
                    int i5 = parcel.readInt();
                    if (parcel.readInt() != 0) {
                        printAttributes = (PrintAttributes) PrintAttributes.CREATOR.createFromParcel(parcel);
                    } else {
                        printAttributes = null;
                    }
                    renderPage(i3, i4, i5, printAttributes, parcel.readInt() != 0 ? (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 3:
                    parcel.enforceInterface("com.android.printspooler.renderer.IPdfRenderer");
                    closeDocument();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IPdfRenderer {
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
                    parcelObtain.writeInterfaceToken("com.android.printspooler.renderer.IPdfRenderer");
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
            public void renderPage(int i, int i2, int i3, PrintAttributes printAttributes, ParcelFileDescriptor parcelFileDescriptor) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.printspooler.renderer.IPdfRenderer");
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    if (printAttributes != null) {
                        parcelObtain.writeInt(1);
                        printAttributes.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (parcelFileDescriptor != null) {
                        parcelObtain.writeInt(1);
                        parcelFileDescriptor.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void closeDocument() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.printspooler.renderer.IPdfRenderer");
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
