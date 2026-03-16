package android.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface INetworkManagementEventObserver extends IInterface {
    void addressRemoved(String str, LinkAddress linkAddress) throws RemoteException;

    void addressUpdated(String str, LinkAddress linkAddress) throws RemoteException;

    void interfaceAdded(String str) throws RemoteException;

    void interfaceClassDataActivityChanged(String str, boolean z, long j) throws RemoteException;

    void interfaceDnsServerInfo(String str, long j, String[] strArr) throws RemoteException;

    void interfaceLinkStateChanged(String str, boolean z) throws RemoteException;

    void interfaceRemoved(String str) throws RemoteException;

    void interfaceStatusChanged(String str, boolean z) throws RemoteException;

    void limitReached(String str, String str2) throws RemoteException;

    void routeRemoved(RouteInfo routeInfo) throws RemoteException;

    void routeUpdated(RouteInfo routeInfo) throws RemoteException;

    public static abstract class Stub extends Binder implements INetworkManagementEventObserver {
        private static final String DESCRIPTOR = "android.net.INetworkManagementEventObserver";
        static final int TRANSACTION_addressRemoved = 6;
        static final int TRANSACTION_addressUpdated = 5;
        static final int TRANSACTION_interfaceAdded = 3;
        static final int TRANSACTION_interfaceClassDataActivityChanged = 8;
        static final int TRANSACTION_interfaceDnsServerInfo = 9;
        static final int TRANSACTION_interfaceLinkStateChanged = 2;
        static final int TRANSACTION_interfaceRemoved = 4;
        static final int TRANSACTION_interfaceStatusChanged = 1;
        static final int TRANSACTION_limitReached = 7;
        static final int TRANSACTION_routeRemoved = 11;
        static final int TRANSACTION_routeUpdated = 10;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INetworkManagementEventObserver asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof INetworkManagementEventObserver)) {
                return (INetworkManagementEventObserver) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            RouteInfo _arg0;
            RouteInfo _arg02;
            boolean _arg1;
            LinkAddress _arg12;
            LinkAddress _arg13;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    _arg1 = data.readInt() != 0;
                    interfaceStatusChanged(_arg03, _arg1);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    _arg1 = data.readInt() != 0;
                    interfaceLinkStateChanged(_arg04, _arg1);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    interfaceAdded(_arg05);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg06 = data.readString();
                    interfaceRemoved(_arg06);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg07 = data.readString();
                    if (data.readInt() != 0) {
                        _arg13 = LinkAddress.CREATOR.createFromParcel(data);
                    } else {
                        _arg13 = null;
                    }
                    addressUpdated(_arg07, _arg13);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg08 = data.readString();
                    if (data.readInt() != 0) {
                        _arg12 = LinkAddress.CREATOR.createFromParcel(data);
                    } else {
                        _arg12 = null;
                    }
                    addressRemoved(_arg08, _arg12);
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg09 = data.readString();
                    limitReached(_arg09, data.readString());
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg010 = data.readString();
                    _arg1 = data.readInt() != 0;
                    long _arg2 = data.readLong();
                    interfaceClassDataActivityChanged(_arg010, _arg1, _arg2);
                    reply.writeNoException();
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg011 = data.readString();
                    long _arg14 = data.readLong();
                    String[] _arg22 = data.createStringArray();
                    interfaceDnsServerInfo(_arg011, _arg14, _arg22);
                    reply.writeNoException();
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = RouteInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    routeUpdated(_arg02);
                    reply.writeNoException();
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = RouteInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    routeRemoved(_arg0);
                    reply.writeNoException();
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements INetworkManagementEventObserver {
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
            public void interfaceStatusChanged(String iface, boolean up) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeInt(up ? 1 : 0);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void interfaceLinkStateChanged(String iface, boolean up) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeInt(up ? 1 : 0);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void interfaceAdded(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void interfaceRemoved(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addressUpdated(String iface, LinkAddress address) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    if (address != null) {
                        _data.writeInt(1);
                        address.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addressRemoved(String iface, LinkAddress address) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    if (address != null) {
                        _data.writeInt(1);
                        address.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void limitReached(String limitName, String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(limitName);
                    _data.writeString(iface);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void interfaceClassDataActivityChanged(String label, boolean active, long tsNanos) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(label);
                    _data.writeInt(active ? 1 : 0);
                    _data.writeLong(tsNanos);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void interfaceDnsServerInfo(String iface, long lifetime, String[] servers) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeLong(lifetime);
                    _data.writeStringArray(servers);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void routeUpdated(RouteInfo route) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (route != null) {
                        _data.writeInt(1);
                        route.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void routeRemoved(RouteInfo route) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (route != null) {
                        _data.writeInt(1);
                        route.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
