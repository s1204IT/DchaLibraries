package android.service.notification;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IStatusBarNotificationHolder extends IInterface {
    StatusBarNotification get() throws RemoteException;

    public static abstract class Stub extends Binder implements IStatusBarNotificationHolder {
        private static final String DESCRIPTOR = "android.service.notification.IStatusBarNotificationHolder";
        static final int TRANSACTION_get = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IStatusBarNotificationHolder asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IStatusBarNotificationHolder)) {
                return (IStatusBarNotificationHolder) iin;
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
                    StatusBarNotification _result = get();
                    reply.writeNoException();
                    if (_result != null) {
                        reply.writeInt(1);
                        _result.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IStatusBarNotificationHolder {
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
            public StatusBarNotification get() throws RemoteException {
                StatusBarNotification statusBarNotificationCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        statusBarNotificationCreateFromParcel = StatusBarNotification.CREATOR.createFromParcel(_reply);
                    } else {
                        statusBarNotificationCreateFromParcel = null;
                    }
                    return statusBarNotificationCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
