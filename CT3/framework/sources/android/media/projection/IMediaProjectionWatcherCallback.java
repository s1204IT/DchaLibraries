package android.media.projection;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IMediaProjectionWatcherCallback extends IInterface {
    void onStart(MediaProjectionInfo mediaProjectionInfo) throws RemoteException;

    void onStop(MediaProjectionInfo mediaProjectionInfo) throws RemoteException;

    public static abstract class Stub extends Binder implements IMediaProjectionWatcherCallback {
        private static final String DESCRIPTOR = "android.media.projection.IMediaProjectionWatcherCallback";
        static final int TRANSACTION_onStart = 1;
        static final int TRANSACTION_onStop = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMediaProjectionWatcherCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IMediaProjectionWatcherCallback)) {
                return (IMediaProjectionWatcherCallback) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            MediaProjectionInfo mediaProjectionInfoCreateFromParcel;
            MediaProjectionInfo mediaProjectionInfoCreateFromParcel2;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        mediaProjectionInfoCreateFromParcel2 = MediaProjectionInfo.CREATOR.createFromParcel(data);
                    } else {
                        mediaProjectionInfoCreateFromParcel2 = null;
                    }
                    onStart(mediaProjectionInfoCreateFromParcel2);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        mediaProjectionInfoCreateFromParcel = MediaProjectionInfo.CREATOR.createFromParcel(data);
                    } else {
                        mediaProjectionInfoCreateFromParcel = null;
                    }
                    onStop(mediaProjectionInfoCreateFromParcel);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IMediaProjectionWatcherCallback {
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
            public void onStart(MediaProjectionInfo info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (info != null) {
                        _data.writeInt(1);
                        info.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onStop(MediaProjectionInfo info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (info != null) {
                        _data.writeInt(1);
                        info.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
