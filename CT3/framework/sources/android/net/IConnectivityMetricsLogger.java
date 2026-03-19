package android.net;

import android.app.PendingIntent;
import android.net.ConnectivityMetricsEvent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IConnectivityMetricsLogger extends IInterface {
    ConnectivityMetricsEvent[] getEvents(ConnectivityMetricsEvent.Reference reference) throws RemoteException;

    long logEvent(ConnectivityMetricsEvent connectivityMetricsEvent) throws RemoteException;

    long logEvents(ConnectivityMetricsEvent[] connectivityMetricsEventArr) throws RemoteException;

    boolean register(PendingIntent pendingIntent) throws RemoteException;

    void unregister(PendingIntent pendingIntent) throws RemoteException;

    public static abstract class Stub extends Binder implements IConnectivityMetricsLogger {
        private static final String DESCRIPTOR = "android.net.IConnectivityMetricsLogger";
        static final int TRANSACTION_getEvents = 3;
        static final int TRANSACTION_logEvent = 1;
        static final int TRANSACTION_logEvents = 2;
        static final int TRANSACTION_register = 4;
        static final int TRANSACTION_unregister = 5;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IConnectivityMetricsLogger asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IConnectivityMetricsLogger)) {
                return (IConnectivityMetricsLogger) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            PendingIntent pendingIntentCreateFromParcel;
            PendingIntent pendingIntentCreateFromParcel2;
            ConnectivityMetricsEvent.Reference referenceCreateFromParcel;
            ConnectivityMetricsEvent connectivityMetricsEventCreateFromParcel;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        connectivityMetricsEventCreateFromParcel = ConnectivityMetricsEvent.CREATOR.createFromParcel(data);
                    } else {
                        connectivityMetricsEventCreateFromParcel = null;
                    }
                    long _result = logEvent(connectivityMetricsEventCreateFromParcel);
                    reply.writeNoException();
                    reply.writeLong(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    ConnectivityMetricsEvent[] _arg0 = (ConnectivityMetricsEvent[]) data.createTypedArray(ConnectivityMetricsEvent.CREATOR);
                    long _result2 = logEvents(_arg0);
                    reply.writeNoException();
                    reply.writeLong(_result2);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        referenceCreateFromParcel = ConnectivityMetricsEvent.Reference.CREATOR.createFromParcel(data);
                    } else {
                        referenceCreateFromParcel = null;
                    }
                    ConnectivityMetricsEvent[] _result3 = getEvents(referenceCreateFromParcel);
                    reply.writeNoException();
                    reply.writeTypedArray(_result3, 1);
                    if (referenceCreateFromParcel != null) {
                        reply.writeInt(1);
                        referenceCreateFromParcel.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        pendingIntentCreateFromParcel2 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        pendingIntentCreateFromParcel2 = null;
                    }
                    boolean _result4 = register(pendingIntentCreateFromParcel2);
                    reply.writeNoException();
                    reply.writeInt(_result4 ? 1 : 0);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        pendingIntentCreateFromParcel = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        pendingIntentCreateFromParcel = null;
                    }
                    unregister(pendingIntentCreateFromParcel);
                    reply.writeNoException();
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IConnectivityMetricsLogger {
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
            public long logEvent(ConnectivityMetricsEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (event != null) {
                        _data.writeInt(1);
                        event.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long logEvents(ConnectivityMetricsEvent[] events) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedArray(events, 0);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ConnectivityMetricsEvent[] getEvents(ConnectivityMetricsEvent.Reference reference) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (reference != null) {
                        _data.writeInt(1);
                        reference.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    ConnectivityMetricsEvent[] _result = (ConnectivityMetricsEvent[]) _reply.createTypedArray(ConnectivityMetricsEvent.CREATOR);
                    if (_reply.readInt() != 0) {
                        reference.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean register(PendingIntent newEventsIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (newEventsIntent != null) {
                        _data.writeInt(1);
                        newEventsIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unregister(PendingIntent newEventsIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (newEventsIntent != null) {
                        _data.writeInt(1);
                        newEventsIntent.writeToParcel(_data, 0);
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
        }
    }
}
