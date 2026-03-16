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

        public static IPdfRenderer asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("com.android.printspooler.renderer.IPdfRenderer");
            if (iin != null && (iin instanceof IPdfRenderer)) {
                return (IPdfRenderer) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            PrintAttributes _arg3;
            ParcelFileDescriptor _arg4;
            ParcelFileDescriptor _arg0;
            switch (code) {
                case 1:
                    data.enforceInterface("com.android.printspooler.renderer.IPdfRenderer");
                    if (data.readInt() != 0) {
                        _arg0 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    int _result = openDocument(_arg0);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 2:
                    data.enforceInterface("com.android.printspooler.renderer.IPdfRenderer");
                    int _arg02 = data.readInt();
                    int _arg1 = data.readInt();
                    int _arg2 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg3 = (PrintAttributes) PrintAttributes.CREATOR.createFromParcel(data);
                    } else {
                        _arg3 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg4 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(data);
                    } else {
                        _arg4 = null;
                    }
                    renderPage(_arg02, _arg1, _arg2, _arg3, _arg4);
                    return true;
                case 3:
                    data.enforceInterface("com.android.printspooler.renderer.IPdfRenderer");
                    closeDocument();
                    return true;
                case 1598968902:
                    reply.writeString("com.android.printspooler.renderer.IPdfRenderer");
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IPdfRenderer {
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
                    _data.writeInterfaceToken("com.android.printspooler.renderer.IPdfRenderer");
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
            public void renderPage(int pageIndex, int bitmapWidth, int bitmapHeight, PrintAttributes attributes, ParcelFileDescriptor destination) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.printspooler.renderer.IPdfRenderer");
                    _data.writeInt(pageIndex);
                    _data.writeInt(bitmapWidth);
                    _data.writeInt(bitmapHeight);
                    if (attributes != null) {
                        _data.writeInt(1);
                        attributes.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (destination != null) {
                        _data.writeInt(1);
                        destination.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void closeDocument() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.printspooler.renderer.IPdfRenderer");
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
