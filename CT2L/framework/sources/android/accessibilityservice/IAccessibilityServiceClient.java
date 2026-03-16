package android.accessibilityservice;

import android.accessibilityservice.IAccessibilityServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public interface IAccessibilityServiceClient extends IInterface {
    void clearAccessibilityCache() throws RemoteException;

    void init(IAccessibilityServiceConnection iAccessibilityServiceConnection, int i, IBinder iBinder) throws RemoteException;

    void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) throws RemoteException;

    void onGesture(int i) throws RemoteException;

    void onInterrupt() throws RemoteException;

    void onKeyEvent(KeyEvent keyEvent, int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IAccessibilityServiceClient {
        private static final String DESCRIPTOR = "android.accessibilityservice.IAccessibilityServiceClient";
        static final int TRANSACTION_clearAccessibilityCache = 5;
        static final int TRANSACTION_init = 1;
        static final int TRANSACTION_onAccessibilityEvent = 2;
        static final int TRANSACTION_onGesture = 4;
        static final int TRANSACTION_onInterrupt = 3;
        static final int TRANSACTION_onKeyEvent = 6;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAccessibilityServiceClient asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IAccessibilityServiceClient)) {
                return (IAccessibilityServiceClient) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            KeyEvent _arg0;
            AccessibilityEvent _arg02;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    IAccessibilityServiceConnection _arg03 = IAccessibilityServiceConnection.Stub.asInterface(data.readStrongBinder());
                    int _arg1 = data.readInt();
                    IBinder _arg2 = data.readStrongBinder();
                    init(_arg03, _arg1, _arg2);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = AccessibilityEvent.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    onAccessibilityEvent(_arg02);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    onInterrupt();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    onGesture(_arg04);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    clearAccessibilityCache();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = KeyEvent.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    int _arg12 = data.readInt();
                    onKeyEvent(_arg0, _arg12);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IAccessibilityServiceClient {
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
            public void init(IAccessibilityServiceConnection connection, int connectionId, IBinder windowToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(connection != null ? connection.asBinder() : null);
                    _data.writeInt(connectionId);
                    _data.writeStrongBinder(windowToken);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onAccessibilityEvent(AccessibilityEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (event != null) {
                        _data.writeInt(1);
                        event.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onInterrupt() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onGesture(int gesture) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(gesture);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void clearAccessibilityCache() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onKeyEvent(KeyEvent event, int sequence) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (event != null) {
                        _data.writeInt(1);
                        event.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(sequence);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
