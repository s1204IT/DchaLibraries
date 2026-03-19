package android.service.notification;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.service.notification.IStatusBarNotificationHolder;

public interface INotificationListener extends IInterface {
    void onInterruptionFilterChanged(int i) throws RemoteException;

    void onListenerConnected(NotificationRankingUpdate notificationRankingUpdate) throws RemoteException;

    void onListenerHintsChanged(int i) throws RemoteException;

    void onNotificationActionClick(String str, long j, int i) throws RemoteException;

    void onNotificationClick(String str, long j) throws RemoteException;

    void onNotificationEnqueued(IStatusBarNotificationHolder iStatusBarNotificationHolder, int i, boolean z) throws RemoteException;

    void onNotificationPosted(IStatusBarNotificationHolder iStatusBarNotificationHolder, NotificationRankingUpdate notificationRankingUpdate) throws RemoteException;

    void onNotificationRankingUpdate(NotificationRankingUpdate notificationRankingUpdate) throws RemoteException;

    void onNotificationRemoved(IStatusBarNotificationHolder iStatusBarNotificationHolder, NotificationRankingUpdate notificationRankingUpdate) throws RemoteException;

    void onNotificationRemovedReason(String str, long j, int i) throws RemoteException;

    void onNotificationVisibilityChanged(String str, long j, boolean z) throws RemoteException;

    public static abstract class Stub extends Binder implements INotificationListener {
        private static final String DESCRIPTOR = "android.service.notification.INotificationListener";
        static final int TRANSACTION_onInterruptionFilterChanged = 6;
        static final int TRANSACTION_onListenerConnected = 1;
        static final int TRANSACTION_onListenerHintsChanged = 5;
        static final int TRANSACTION_onNotificationActionClick = 10;
        static final int TRANSACTION_onNotificationClick = 9;
        static final int TRANSACTION_onNotificationEnqueued = 7;
        static final int TRANSACTION_onNotificationPosted = 2;
        static final int TRANSACTION_onNotificationRankingUpdate = 4;
        static final int TRANSACTION_onNotificationRemoved = 3;
        static final int TRANSACTION_onNotificationRemovedReason = 11;
        static final int TRANSACTION_onNotificationVisibilityChanged = 8;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INotificationListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof INotificationListener)) {
                return (INotificationListener) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            NotificationRankingUpdate notificationRankingUpdateCreateFromParcel;
            NotificationRankingUpdate notificationRankingUpdateCreateFromParcel2;
            NotificationRankingUpdate notificationRankingUpdateCreateFromParcel3;
            NotificationRankingUpdate notificationRankingUpdateCreateFromParcel4;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        notificationRankingUpdateCreateFromParcel4 = NotificationRankingUpdate.CREATOR.createFromParcel(data);
                    } else {
                        notificationRankingUpdateCreateFromParcel4 = null;
                    }
                    onListenerConnected(notificationRankingUpdateCreateFromParcel4);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    IStatusBarNotificationHolder _arg0 = IStatusBarNotificationHolder.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        notificationRankingUpdateCreateFromParcel3 = NotificationRankingUpdate.CREATOR.createFromParcel(data);
                    } else {
                        notificationRankingUpdateCreateFromParcel3 = null;
                    }
                    onNotificationPosted(_arg0, notificationRankingUpdateCreateFromParcel3);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    IStatusBarNotificationHolder _arg02 = IStatusBarNotificationHolder.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        notificationRankingUpdateCreateFromParcel2 = NotificationRankingUpdate.CREATOR.createFromParcel(data);
                    } else {
                        notificationRankingUpdateCreateFromParcel2 = null;
                    }
                    onNotificationRemoved(_arg02, notificationRankingUpdateCreateFromParcel2);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        notificationRankingUpdateCreateFromParcel = NotificationRankingUpdate.CREATOR.createFromParcel(data);
                    } else {
                        notificationRankingUpdateCreateFromParcel = null;
                    }
                    onNotificationRankingUpdate(notificationRankingUpdateCreateFromParcel);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg03 = data.readInt();
                    onListenerHintsChanged(_arg03);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    onInterruptionFilterChanged(_arg04);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    IStatusBarNotificationHolder _arg05 = IStatusBarNotificationHolder.Stub.asInterface(data.readStrongBinder());
                    int _arg1 = data.readInt();
                    boolean _arg2 = data.readInt() != 0;
                    onNotificationEnqueued(_arg05, _arg1, _arg2);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg06 = data.readString();
                    long _arg12 = data.readLong();
                    boolean _arg22 = data.readInt() != 0;
                    onNotificationVisibilityChanged(_arg06, _arg12, _arg22);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg07 = data.readString();
                    long _arg13 = data.readLong();
                    onNotificationClick(_arg07, _arg13);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg08 = data.readString();
                    long _arg14 = data.readLong();
                    int _arg23 = data.readInt();
                    onNotificationActionClick(_arg08, _arg14, _arg23);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg09 = data.readString();
                    long _arg15 = data.readLong();
                    int _arg24 = data.readInt();
                    onNotificationRemovedReason(_arg09, _arg15, _arg24);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements INotificationListener {
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
            public void onListenerConnected(NotificationRankingUpdate update) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (update != null) {
                        _data.writeInt(1);
                        update.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onNotificationPosted(IStatusBarNotificationHolder notificationHolder, NotificationRankingUpdate update) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(notificationHolder != null ? notificationHolder.asBinder() : null);
                    if (update != null) {
                        _data.writeInt(1);
                        update.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onNotificationRemoved(IStatusBarNotificationHolder notificationHolder, NotificationRankingUpdate update) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(notificationHolder != null ? notificationHolder.asBinder() : null);
                    if (update != null) {
                        _data.writeInt(1);
                        update.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onNotificationRankingUpdate(NotificationRankingUpdate update) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (update != null) {
                        _data.writeInt(1);
                        update.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onListenerHintsChanged(int hints) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(hints);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onInterruptionFilterChanged(int interruptionFilter) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(interruptionFilter);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onNotificationEnqueued(IStatusBarNotificationHolder notificationHolder, int importance, boolean user) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(notificationHolder != null ? notificationHolder.asBinder() : null);
                    _data.writeInt(importance);
                    _data.writeInt(user ? 1 : 0);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onNotificationVisibilityChanged(String key, long time, boolean visible) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(key);
                    _data.writeLong(time);
                    _data.writeInt(visible ? 1 : 0);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onNotificationClick(String key, long time) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(key);
                    _data.writeLong(time);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onNotificationActionClick(String key, long time, int actionIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(key);
                    _data.writeLong(time);
                    _data.writeInt(actionIndex);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onNotificationRemovedReason(String key, long time, int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(key);
                    _data.writeLong(time);
                    _data.writeInt(reason);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
