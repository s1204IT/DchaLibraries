package org.gsma.joyn;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ICoreServiceWrapper extends IInterface {
    IBinder getCapabilitiesServiceBinder() throws RemoteException;

    IBinder getChatServiceBinder() throws RemoteException;

    IBinder getContactsServiceBinder() throws RemoteException;

    IBinder getFileTransferServiceBinder() throws RemoteException;

    IBinder getGeolocServiceBinder() throws RemoteException;

    IBinder getIPCallServiceBinder() throws RemoteException;

    IBinder getImageSharingServiceBinder() throws RemoteException;

    IBinder getMultimediaSessionServiceBinder() throws RemoteException;

    IBinder getNetworkConnectivityApiBinder() throws RemoteException;

    IBinder getVideoSharingServiceBinder() throws RemoteException;

    public static abstract class Stub extends Binder implements ICoreServiceWrapper {
        private static final String DESCRIPTOR = "org.gsma.joyn.ICoreServiceWrapper";
        static final int TRANSACTION_getCapabilitiesServiceBinder = 3;
        static final int TRANSACTION_getChatServiceBinder = 1;
        static final int TRANSACTION_getContactsServiceBinder = 4;
        static final int TRANSACTION_getFileTransferServiceBinder = 2;
        static final int TRANSACTION_getGeolocServiceBinder = 5;
        static final int TRANSACTION_getIPCallServiceBinder = 8;
        static final int TRANSACTION_getImageSharingServiceBinder = 7;
        static final int TRANSACTION_getMultimediaSessionServiceBinder = 9;
        static final int TRANSACTION_getNetworkConnectivityApiBinder = 10;
        static final int TRANSACTION_getVideoSharingServiceBinder = 6;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ICoreServiceWrapper asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ICoreServiceWrapper)) {
                return (ICoreServiceWrapper) iin;
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
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _result = getChatServiceBinder();
                    reply.writeNoException();
                    reply.writeStrongBinder(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _result2 = getFileTransferServiceBinder();
                    reply.writeNoException();
                    reply.writeStrongBinder(_result2);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _result3 = getCapabilitiesServiceBinder();
                    reply.writeNoException();
                    reply.writeStrongBinder(_result3);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _result4 = getContactsServiceBinder();
                    reply.writeNoException();
                    reply.writeStrongBinder(_result4);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _result5 = getGeolocServiceBinder();
                    reply.writeNoException();
                    reply.writeStrongBinder(_result5);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _result6 = getVideoSharingServiceBinder();
                    reply.writeNoException();
                    reply.writeStrongBinder(_result6);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _result7 = getImageSharingServiceBinder();
                    reply.writeNoException();
                    reply.writeStrongBinder(_result7);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _result8 = getIPCallServiceBinder();
                    reply.writeNoException();
                    reply.writeStrongBinder(_result8);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _result9 = getMultimediaSessionServiceBinder();
                    reply.writeNoException();
                    reply.writeStrongBinder(_result9);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _result10 = getNetworkConnectivityApiBinder();
                    reply.writeNoException();
                    reply.writeStrongBinder(_result10);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements ICoreServiceWrapper {
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
            public IBinder getChatServiceBinder() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IBinder getFileTransferServiceBinder() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IBinder getCapabilitiesServiceBinder() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IBinder getContactsServiceBinder() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IBinder getGeolocServiceBinder() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IBinder getVideoSharingServiceBinder() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IBinder getImageSharingServiceBinder() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IBinder getIPCallServiceBinder() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IBinder getMultimediaSessionServiceBinder() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IBinder getNetworkConnectivityApiBinder() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
