package android.net.wifi.nan;

import android.net.wifi.nan.IWifiNanEventListener;
import android.net.wifi.nan.IWifiNanSessionListener;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IWifiNanManager extends IInterface {
    void connect(IBinder iBinder, IWifiNanEventListener iWifiNanEventListener, int i) throws RemoteException;

    int createSession(IWifiNanSessionListener iWifiNanSessionListener, int i) throws RemoteException;

    void destroySession(int i) throws RemoteException;

    void disconnect(IBinder iBinder) throws RemoteException;

    void publish(int i, PublishData publishData, PublishSettings publishSettings) throws RemoteException;

    void requestConfig(ConfigRequest configRequest) throws RemoteException;

    void sendMessage(int i, int i2, byte[] bArr, int i3, int i4) throws RemoteException;

    void stopSession(int i) throws RemoteException;

    void subscribe(int i, SubscribeData subscribeData, SubscribeSettings subscribeSettings) throws RemoteException;

    public static abstract class Stub extends Binder implements IWifiNanManager {
        private static final String DESCRIPTOR = "android.net.wifi.nan.IWifiNanManager";
        static final int TRANSACTION_connect = 1;
        static final int TRANSACTION_createSession = 4;
        static final int TRANSACTION_destroySession = 9;
        static final int TRANSACTION_disconnect = 2;
        static final int TRANSACTION_publish = 5;
        static final int TRANSACTION_requestConfig = 3;
        static final int TRANSACTION_sendMessage = 7;
        static final int TRANSACTION_stopSession = 8;
        static final int TRANSACTION_subscribe = 6;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IWifiNanManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IWifiNanManager)) {
                return (IWifiNanManager) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            SubscribeData subscribeDataCreateFromParcel;
            SubscribeSettings subscribeSettingsCreateFromParcel;
            PublishData publishDataCreateFromParcel;
            PublishSettings publishSettingsCreateFromParcel;
            ConfigRequest configRequestCreateFromParcel;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _arg0 = data.readStrongBinder();
                    IWifiNanEventListener _arg1 = IWifiNanEventListener.Stub.asInterface(data.readStrongBinder());
                    int _arg2 = data.readInt();
                    connect(_arg0, _arg1, _arg2);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _arg02 = data.readStrongBinder();
                    disconnect(_arg02);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        configRequestCreateFromParcel = ConfigRequest.CREATOR.createFromParcel(data);
                    } else {
                        configRequestCreateFromParcel = null;
                    }
                    requestConfig(configRequestCreateFromParcel);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    IWifiNanSessionListener _arg03 = IWifiNanSessionListener.Stub.asInterface(data.readStrongBinder());
                    int _arg12 = data.readInt();
                    int _result = createSession(_arg03, _arg12);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    if (data.readInt() != 0) {
                        publishDataCreateFromParcel = PublishData.CREATOR.createFromParcel(data);
                    } else {
                        publishDataCreateFromParcel = null;
                    }
                    if (data.readInt() != 0) {
                        publishSettingsCreateFromParcel = PublishSettings.CREATOR.createFromParcel(data);
                    } else {
                        publishSettingsCreateFromParcel = null;
                    }
                    publish(_arg04, publishDataCreateFromParcel, publishSettingsCreateFromParcel);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg05 = data.readInt();
                    if (data.readInt() != 0) {
                        subscribeDataCreateFromParcel = SubscribeData.CREATOR.createFromParcel(data);
                    } else {
                        subscribeDataCreateFromParcel = null;
                    }
                    if (data.readInt() != 0) {
                        subscribeSettingsCreateFromParcel = SubscribeSettings.CREATOR.createFromParcel(data);
                    } else {
                        subscribeSettingsCreateFromParcel = null;
                    }
                    subscribe(_arg05, subscribeDataCreateFromParcel, subscribeSettingsCreateFromParcel);
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg06 = data.readInt();
                    int _arg13 = data.readInt();
                    byte[] _arg22 = data.createByteArray();
                    int _arg3 = data.readInt();
                    int _arg4 = data.readInt();
                    sendMessage(_arg06, _arg13, _arg22, _arg3, _arg4);
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg07 = data.readInt();
                    stopSession(_arg07);
                    reply.writeNoException();
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg08 = data.readInt();
                    destroySession(_arg08);
                    reply.writeNoException();
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IWifiNanManager {
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
            public void connect(IBinder binder, IWifiNanEventListener listener, int events) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(binder);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    _data.writeInt(events);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void disconnect(IBinder binder) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(binder);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void requestConfig(ConfigRequest configRequest) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (configRequest != null) {
                        _data.writeInt(1);
                        configRequest.writeToParcel(_data, 0);
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
            public int createSession(IWifiNanSessionListener listener, int events) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    _data.writeInt(events);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void publish(int sessionId, PublishData publishData, PublishSettings publishSettings) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(sessionId);
                    if (publishData != null) {
                        _data.writeInt(1);
                        publishData.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (publishSettings != null) {
                        _data.writeInt(1);
                        publishSettings.writeToParcel(_data, 0);
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
            public void subscribe(int sessionId, SubscribeData subscribeData, SubscribeSettings subscribeSettings) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(sessionId);
                    if (subscribeData != null) {
                        _data.writeInt(1);
                        subscribeData.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (subscribeSettings != null) {
                        _data.writeInt(1);
                        subscribeSettings.writeToParcel(_data, 0);
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
            public void sendMessage(int sessionId, int peerId, byte[] message, int messageLength, int messageId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(sessionId);
                    _data.writeInt(peerId);
                    _data.writeByteArray(message);
                    _data.writeInt(messageLength);
                    _data.writeInt(messageId);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void stopSession(int sessionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(sessionId);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void destroySession(int sessionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(sessionId);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
