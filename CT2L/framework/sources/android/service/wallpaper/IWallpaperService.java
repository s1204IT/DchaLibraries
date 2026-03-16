package android.service.wallpaper;

import android.graphics.Rect;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.service.wallpaper.IWallpaperConnection;

public interface IWallpaperService extends IInterface {
    void attach(IWallpaperConnection iWallpaperConnection, IBinder iBinder, int i, boolean z, int i2, int i3, Rect rect) throws RemoteException;

    public static abstract class Stub extends Binder implements IWallpaperService {
        private static final String DESCRIPTOR = "android.service.wallpaper.IWallpaperService";
        static final int TRANSACTION_attach = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IWallpaperService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IWallpaperService)) {
                return (IWallpaperService) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Rect _arg6;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    IWallpaperConnection _arg0 = IWallpaperConnection.Stub.asInterface(data.readStrongBinder());
                    IBinder _arg1 = data.readStrongBinder();
                    int _arg2 = data.readInt();
                    boolean _arg3 = data.readInt() != 0;
                    int _arg4 = data.readInt();
                    int _arg5 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg6 = Rect.CREATOR.createFromParcel(data);
                    } else {
                        _arg6 = null;
                    }
                    attach(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IWallpaperService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public void attach(IWallpaperConnection connection, IBinder windowToken, int windowType, boolean isPreview, int reqWidth, int reqHeight, Rect padding) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(connection != null ? connection.asBinder() : null);
                    _data.writeStrongBinder(windowToken);
                    _data.writeInt(windowType);
                    _data.writeInt(isPreview ? 1 : 0);
                    _data.writeInt(reqWidth);
                    _data.writeInt(reqHeight);
                    if (padding != null) {
                        _data.writeInt(1);
                        padding.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
