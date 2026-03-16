package com.android.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IProxyService extends IInterface {
    String resolvePacFile(String str, String str2) throws RemoteException;

    void setPacFile(String str) throws RemoteException;

    void startPacSystem() throws RemoteException;

    void stopPacSystem() throws RemoteException;

    public static abstract class Stub extends Binder implements IProxyService {
        private static final String DESCRIPTOR = "com.android.net.IProxyService";
        static final int TRANSACTION_resolvePacFile = 1;
        static final int TRANSACTION_setPacFile = 2;
        static final int TRANSACTION_startPacSystem = 3;
        static final int TRANSACTION_stopPacSystem = 4;

        public Stub() {
            attachInterface(this, "com.android.net.IProxyService");
        }

        public static IProxyService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("com.android.net.IProxyService");
            if (iin != null && (iin instanceof IProxyService)) {
                return (IProxyService) iin;
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
                    data.enforceInterface("com.android.net.IProxyService");
                    String _arg0 = data.readString();
                    String _arg1 = data.readString();
                    String _result = resolvePacFile(_arg0, _arg1);
                    reply.writeNoException();
                    reply.writeString(_result);
                    return true;
                case 2:
                    data.enforceInterface("com.android.net.IProxyService");
                    String _arg02 = data.readString();
                    setPacFile(_arg02);
                    return true;
                case 3:
                    data.enforceInterface("com.android.net.IProxyService");
                    startPacSystem();
                    return true;
                case 4:
                    data.enforceInterface("com.android.net.IProxyService");
                    stopPacSystem();
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString("com.android.net.IProxyService");
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IProxyService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return "com.android.net.IProxyService";
            }

            @Override
            public String resolvePacFile(String host, String url) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.net.IProxyService");
                    _data.writeString(host);
                    _data.writeString(url);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setPacFile(String scriptContents) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.net.IProxyService");
                    _data.writeString(scriptContents);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void startPacSystem() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.net.IProxyService");
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void stopPacSystem() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.net.IProxyService");
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
