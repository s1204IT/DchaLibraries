package android.media;

import android.media.IMediaRouterClient;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IMediaRouterService extends IInterface {
    MediaRouterClientState getState(IMediaRouterClient iMediaRouterClient) throws RemoteException;

    void registerClientAsUser(IMediaRouterClient iMediaRouterClient, String str, int i) throws RemoteException;

    void requestSetVolume(IMediaRouterClient iMediaRouterClient, String str, int i) throws RemoteException;

    void requestUpdateVolume(IMediaRouterClient iMediaRouterClient, String str, int i) throws RemoteException;

    void setDiscoveryRequest(IMediaRouterClient iMediaRouterClient, int i, boolean z) throws RemoteException;

    void setSelectedRoute(IMediaRouterClient iMediaRouterClient, String str, boolean z) throws RemoteException;

    void unregisterClient(IMediaRouterClient iMediaRouterClient) throws RemoteException;

    public static abstract class Stub extends Binder implements IMediaRouterService {
        private static final String DESCRIPTOR = "android.media.IMediaRouterService";
        static final int TRANSACTION_getState = 3;
        static final int TRANSACTION_registerClientAsUser = 1;
        static final int TRANSACTION_requestSetVolume = 6;
        static final int TRANSACTION_requestUpdateVolume = 7;
        static final int TRANSACTION_setDiscoveryRequest = 4;
        static final int TRANSACTION_setSelectedRoute = 5;
        static final int TRANSACTION_unregisterClient = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMediaRouterService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IMediaRouterService)) {
                return (IMediaRouterService) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            boolean _arg2;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    IMediaRouterClient _arg0 = IMediaRouterClient.Stub.asInterface(data.readStrongBinder());
                    String _arg1 = data.readString();
                    registerClientAsUser(_arg0, _arg1, data.readInt());
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    IMediaRouterClient _arg02 = IMediaRouterClient.Stub.asInterface(data.readStrongBinder());
                    unregisterClient(_arg02);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    IMediaRouterClient _arg03 = IMediaRouterClient.Stub.asInterface(data.readStrongBinder());
                    MediaRouterClientState _result = getState(_arg03);
                    reply.writeNoException();
                    if (_result != null) {
                        reply.writeInt(1);
                        _result.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    IMediaRouterClient _arg04 = IMediaRouterClient.Stub.asInterface(data.readStrongBinder());
                    int _arg12 = data.readInt();
                    _arg2 = data.readInt() != 0;
                    setDiscoveryRequest(_arg04, _arg12, _arg2);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    IMediaRouterClient _arg05 = IMediaRouterClient.Stub.asInterface(data.readStrongBinder());
                    String _arg13 = data.readString();
                    _arg2 = data.readInt() != 0;
                    setSelectedRoute(_arg05, _arg13, _arg2);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    IMediaRouterClient _arg06 = IMediaRouterClient.Stub.asInterface(data.readStrongBinder());
                    String _arg14 = data.readString();
                    requestSetVolume(_arg06, _arg14, data.readInt());
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    IMediaRouterClient _arg07 = IMediaRouterClient.Stub.asInterface(data.readStrongBinder());
                    String _arg15 = data.readString();
                    requestUpdateVolume(_arg07, _arg15, data.readInt());
                    reply.writeNoException();
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IMediaRouterService {
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
            public void registerClientAsUser(IMediaRouterClient client, String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unregisterClient(IMediaRouterClient client) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public MediaRouterClientState getState(IMediaRouterClient client) throws RemoteException {
                MediaRouterClientState _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = MediaRouterClientState.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setDiscoveryRequest(IMediaRouterClient client, int routeTypes, boolean activeScan) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    _data.writeInt(routeTypes);
                    _data.writeInt(activeScan ? 1 : 0);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setSelectedRoute(IMediaRouterClient client, String routeId, boolean explicit) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    _data.writeString(routeId);
                    _data.writeInt(explicit ? 1 : 0);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void requestSetVolume(IMediaRouterClient client, String routeId, int volume) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    _data.writeString(routeId);
                    _data.writeInt(volume);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void requestUpdateVolume(IMediaRouterClient client, String routeId, int direction) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    _data.writeString(routeId);
                    _data.writeInt(direction);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
