package org.gsma.joyn.gsh;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;
import org.gsma.joyn.IJoynServiceRegistrationListener;
import org.gsma.joyn.chat.Geoloc;
import org.gsma.joyn.gsh.IGeolocSharing;
import org.gsma.joyn.gsh.IGeolocSharingListener;
import org.gsma.joyn.gsh.INewGeolocSharingListener;

public interface IGeolocSharingService extends IInterface {
    void addNewGeolocSharingListener(INewGeolocSharingListener iNewGeolocSharingListener) throws RemoteException;

    void addServiceRegistrationListener(IJoynServiceRegistrationListener iJoynServiceRegistrationListener) throws RemoteException;

    IGeolocSharing getGeolocSharing(String str) throws RemoteException;

    List<IBinder> getGeolocSharings() throws RemoteException;

    int getServiceVersion() throws RemoteException;

    boolean isServiceRegistered() throws RemoteException;

    void removeNewGeolocSharingListener(INewGeolocSharingListener iNewGeolocSharingListener) throws RemoteException;

    void removeServiceRegistrationListener(IJoynServiceRegistrationListener iJoynServiceRegistrationListener) throws RemoteException;

    IGeolocSharing shareGeoloc(String str, Geoloc geoloc, IGeolocSharingListener iGeolocSharingListener) throws RemoteException;

    public static abstract class Stub extends Binder implements IGeolocSharingService {
        private static final String DESCRIPTOR = "org.gsma.joyn.gsh.IGeolocSharingService";
        static final int TRANSACTION_addNewGeolocSharingListener = 7;
        static final int TRANSACTION_addServiceRegistrationListener = 2;
        static final int TRANSACTION_getGeolocSharing = 5;
        static final int TRANSACTION_getGeolocSharings = 4;
        static final int TRANSACTION_getServiceVersion = 9;
        static final int TRANSACTION_isServiceRegistered = 1;
        static final int TRANSACTION_removeNewGeolocSharingListener = 8;
        static final int TRANSACTION_removeServiceRegistrationListener = 3;
        static final int TRANSACTION_shareGeoloc = 6;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IGeolocSharingService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IGeolocSharingService)) {
                return (IGeolocSharingService) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Geoloc geolocCreateFromParcel;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = isServiceRegistered();
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    IJoynServiceRegistrationListener _arg0 = IJoynServiceRegistrationListener.Stub.asInterface(data.readStrongBinder());
                    addServiceRegistrationListener(_arg0);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    IJoynServiceRegistrationListener _arg02 = IJoynServiceRegistrationListener.Stub.asInterface(data.readStrongBinder());
                    removeServiceRegistrationListener(_arg02);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    List<IBinder> _result2 = getGeolocSharings();
                    reply.writeNoException();
                    reply.writeBinderList(_result2);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    IGeolocSharing _result3 = getGeolocSharing(_arg03);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result3 != null ? _result3.asBinder() : null);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    if (data.readInt() != 0) {
                        geolocCreateFromParcel = Geoloc.CREATOR.createFromParcel(data);
                    } else {
                        geolocCreateFromParcel = null;
                    }
                    IGeolocSharingListener _arg2 = IGeolocSharingListener.Stub.asInterface(data.readStrongBinder());
                    IGeolocSharing _result4 = shareGeoloc(_arg04, geolocCreateFromParcel, _arg2);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result4 != null ? _result4.asBinder() : null);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    INewGeolocSharingListener _arg05 = INewGeolocSharingListener.Stub.asInterface(data.readStrongBinder());
                    addNewGeolocSharingListener(_arg05);
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    INewGeolocSharingListener _arg06 = INewGeolocSharingListener.Stub.asInterface(data.readStrongBinder());
                    removeNewGeolocSharingListener(_arg06);
                    reply.writeNoException();
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    int _result5 = getServiceVersion();
                    reply.writeNoException();
                    reply.writeInt(_result5);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IGeolocSharingService {
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
            public boolean isServiceRegistered() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addServiceRegistrationListener(IJoynServiceRegistrationListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<IBinder> getGeolocSharings() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    List<IBinder> _result = _reply.createBinderArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IGeolocSharing getGeolocSharing(String sharingId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(sharingId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    IGeolocSharing _result = IGeolocSharing.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IGeolocSharing shareGeoloc(String contact, Geoloc geoloc, IGeolocSharingListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    if (geoloc != null) {
                        _data.writeInt(1);
                        geoloc.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    IGeolocSharing _result = IGeolocSharing.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addNewGeolocSharingListener(INewGeolocSharingListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeNewGeolocSharingListener(INewGeolocSharingListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getServiceVersion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
