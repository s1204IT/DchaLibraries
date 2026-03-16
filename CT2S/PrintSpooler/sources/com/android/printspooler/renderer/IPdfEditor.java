package com.android.printspooler.renderer;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.print.PageRange;
import android.print.PrintAttributes;

public interface IPdfEditor extends IInterface {
    void applyPrintAttributes(PrintAttributes printAttributes) throws RemoteException;

    void closeDocument() throws RemoteException;

    int openDocument(ParcelFileDescriptor parcelFileDescriptor) throws RemoteException;

    void removePages(PageRange[] pageRangeArr) throws RemoteException;

    void write(ParcelFileDescriptor parcelFileDescriptor) throws RemoteException;

    public static abstract class Stub extends Binder implements IPdfEditor {
        public Stub() {
            attachInterface(this, "com.android.printspooler.renderer.IPdfEditor");
        }

        public static IPdfEditor asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("com.android.printspooler.renderer.IPdfEditor");
            if (iin != null && (iin instanceof IPdfEditor)) {
                return (IPdfEditor) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            ParcelFileDescriptor _arg0;
            PrintAttributes _arg02;
            ParcelFileDescriptor _arg03;
            switch (code) {
                case 1:
                    data.enforceInterface("com.android.printspooler.renderer.IPdfEditor");
                    if (data.readInt() != 0) {
                        _arg03 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(data);
                    } else {
                        _arg03 = null;
                    }
                    int _result = openDocument(_arg03);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 2:
                    data.enforceInterface("com.android.printspooler.renderer.IPdfEditor");
                    PageRange[] _arg04 = (PageRange[]) data.createTypedArray(PageRange.CREATOR);
                    removePages(_arg04);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface("com.android.printspooler.renderer.IPdfEditor");
                    if (data.readInt() != 0) {
                        _arg02 = (PrintAttributes) PrintAttributes.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    applyPrintAttributes(_arg02);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface("com.android.printspooler.renderer.IPdfEditor");
                    if (data.readInt() != 0) {
                        _arg0 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    write(_arg0);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface("com.android.printspooler.renderer.IPdfEditor");
                    closeDocument();
                    reply.writeNoException();
                    return true;
                case 1598968902:
                    reply.writeString("com.android.printspooler.renderer.IPdfEditor");
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IPdfEditor {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override
            public int openDocument(ParcelFileDescriptor source) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.printspooler.renderer.IPdfEditor");
                    if (source != null) {
                        _data.writeInt(1);
                        source.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removePages(PageRange[] pages) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.printspooler.renderer.IPdfEditor");
                    _data.writeTypedArray(pages, 0);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void applyPrintAttributes(PrintAttributes attributes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.printspooler.renderer.IPdfEditor");
                    if (attributes != null) {
                        _data.writeInt(1);
                        attributes.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void write(ParcelFileDescriptor destination) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.printspooler.renderer.IPdfEditor");
                    if (destination != null) {
                        _data.writeInt(1);
                        destination.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void closeDocument() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.printspooler.renderer.IPdfEditor");
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
