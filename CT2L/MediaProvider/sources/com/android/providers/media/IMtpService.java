package com.android.providers.media;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IMtpService extends IInterface {
    void sendObjectAdded(int i) throws RemoteException;

    void sendObjectRemoved(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IMtpService {
        public Stub() {
            attachInterface(this, "com.android.providers.media.IMtpService");
        }

        public static IMtpService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("com.android.providers.media.IMtpService");
            if (iin != null && (iin instanceof IMtpService)) {
                return (IMtpService) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case 1:
                    data.enforceInterface("com.android.providers.media.IMtpService");
                    int _arg0 = data.readInt();
                    sendObjectAdded(_arg0);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface("com.android.providers.media.IMtpService");
                    int _arg02 = data.readInt();
                    sendObjectRemoved(_arg02);
                    reply.writeNoException();
                    return true;
                case 1598968902:
                    reply.writeString("com.android.providers.media.IMtpService");
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IMtpService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override
            public void sendObjectAdded(int objectHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.providers.media.IMtpService");
                    _data.writeInt(objectHandle);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void sendObjectRemoved(int objectHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.providers.media.IMtpService");
                    _data.writeInt(objectHandle);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
