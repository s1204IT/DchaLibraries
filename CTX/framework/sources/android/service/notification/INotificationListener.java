package android.service.notification;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.notification.IStatusBarNotificationHolder;

public interface INotificationListener extends IInterface {
    void onInterruptionFilterChanged(int i) throws RemoteException;

    void onListenerConnected(NotificationRankingUpdate notificationRankingUpdate) throws RemoteException;

    void onListenerHintsChanged(int i) throws RemoteException;

    void onNotificationChannelGroupModification(String str, UserHandle userHandle, NotificationChannelGroup notificationChannelGroup, int i) throws RemoteException;

    void onNotificationChannelModification(String str, UserHandle userHandle, NotificationChannel notificationChannel, int i) throws RemoteException;

    void onNotificationEnqueued(IStatusBarNotificationHolder iStatusBarNotificationHolder) throws RemoteException;

    void onNotificationPosted(IStatusBarNotificationHolder iStatusBarNotificationHolder, NotificationRankingUpdate notificationRankingUpdate) throws RemoteException;

    void onNotificationRankingUpdate(NotificationRankingUpdate notificationRankingUpdate) throws RemoteException;

    void onNotificationRemoved(IStatusBarNotificationHolder iStatusBarNotificationHolder, NotificationRankingUpdate notificationRankingUpdate, NotificationStats notificationStats, int i) throws RemoteException;

    void onNotificationSnoozedUntilContext(IStatusBarNotificationHolder iStatusBarNotificationHolder, String str) throws RemoteException;

    public static abstract class Stub extends Binder implements INotificationListener {
        private static final String DESCRIPTOR = "android.service.notification.INotificationListener";
        static final int TRANSACTION_onInterruptionFilterChanged = 6;
        static final int TRANSACTION_onListenerConnected = 1;
        static final int TRANSACTION_onListenerHintsChanged = 5;
        static final int TRANSACTION_onNotificationChannelGroupModification = 8;
        static final int TRANSACTION_onNotificationChannelModification = 7;
        static final int TRANSACTION_onNotificationEnqueued = 9;
        static final int TRANSACTION_onNotificationPosted = 2;
        static final int TRANSACTION_onNotificationRankingUpdate = 4;
        static final int TRANSACTION_onNotificationRemoved = 3;
        static final int TRANSACTION_onNotificationSnoozedUntilContext = 10;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INotificationListener asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof INotificationListener)) {
                return (INotificationListener) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            NotificationRankingUpdate notificationRankingUpdateCreateFromParcel;
            UserHandle userHandleCreateFromParcel;
            UserHandle userHandleCreateFromParcel2;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    onListenerConnected(parcel.readInt() != 0 ? NotificationRankingUpdate.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    onNotificationPosted(IStatusBarNotificationHolder.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? NotificationRankingUpdate.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    IStatusBarNotificationHolder iStatusBarNotificationHolderAsInterface = IStatusBarNotificationHolder.Stub.asInterface(parcel.readStrongBinder());
                    if (parcel.readInt() != 0) {
                        notificationRankingUpdateCreateFromParcel = NotificationRankingUpdate.CREATOR.createFromParcel(parcel);
                    } else {
                        notificationRankingUpdateCreateFromParcel = null;
                    }
                    onNotificationRemoved(iStatusBarNotificationHolderAsInterface, notificationRankingUpdateCreateFromParcel, parcel.readInt() != 0 ? NotificationStats.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    onNotificationRankingUpdate(parcel.readInt() != 0 ? NotificationRankingUpdate.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    onListenerHintsChanged(parcel.readInt());
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    onInterruptionFilterChanged(parcel.readInt());
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string = parcel.readString();
                    if (parcel.readInt() != 0) {
                        userHandleCreateFromParcel = UserHandle.CREATOR.createFromParcel(parcel);
                    } else {
                        userHandleCreateFromParcel = null;
                    }
                    onNotificationChannelModification(string, userHandleCreateFromParcel, parcel.readInt() != 0 ? NotificationChannel.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string2 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        userHandleCreateFromParcel2 = UserHandle.CREATOR.createFromParcel(parcel);
                    } else {
                        userHandleCreateFromParcel2 = null;
                    }
                    onNotificationChannelGroupModification(string2, userHandleCreateFromParcel2, parcel.readInt() != 0 ? NotificationChannelGroup.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    onNotificationEnqueued(IStatusBarNotificationHolder.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    onNotificationSnoozedUntilContext(IStatusBarNotificationHolder.Stub.asInterface(parcel.readStrongBinder()), parcel.readString());
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements INotificationListener {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public void onListenerConnected(NotificationRankingUpdate notificationRankingUpdate) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (notificationRankingUpdate != null) {
                        parcelObtain.writeInt(1);
                        notificationRankingUpdate.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onNotificationPosted(IStatusBarNotificationHolder iStatusBarNotificationHolder, NotificationRankingUpdate notificationRankingUpdate) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iStatusBarNotificationHolder != null ? iStatusBarNotificationHolder.asBinder() : null);
                    if (notificationRankingUpdate != null) {
                        parcelObtain.writeInt(1);
                        notificationRankingUpdate.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onNotificationRemoved(IStatusBarNotificationHolder iStatusBarNotificationHolder, NotificationRankingUpdate notificationRankingUpdate, NotificationStats notificationStats, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iStatusBarNotificationHolder != null ? iStatusBarNotificationHolder.asBinder() : null);
                    if (notificationRankingUpdate != null) {
                        parcelObtain.writeInt(1);
                        notificationRankingUpdate.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (notificationStats != null) {
                        parcelObtain.writeInt(1);
                        notificationStats.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onNotificationRankingUpdate(NotificationRankingUpdate notificationRankingUpdate) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (notificationRankingUpdate != null) {
                        parcelObtain.writeInt(1);
                        notificationRankingUpdate.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(4, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onListenerHintsChanged(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(5, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onInterruptionFilterChanged(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(6, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onNotificationChannelModification(String str, UserHandle userHandle, NotificationChannel notificationChannel, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (userHandle != null) {
                        parcelObtain.writeInt(1);
                        userHandle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (notificationChannel != null) {
                        parcelObtain.writeInt(1);
                        notificationChannel.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(7, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onNotificationChannelGroupModification(String str, UserHandle userHandle, NotificationChannelGroup notificationChannelGroup, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (userHandle != null) {
                        parcelObtain.writeInt(1);
                        userHandle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (notificationChannelGroup != null) {
                        parcelObtain.writeInt(1);
                        notificationChannelGroup.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(8, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onNotificationEnqueued(IStatusBarNotificationHolder iStatusBarNotificationHolder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iStatusBarNotificationHolder != null ? iStatusBarNotificationHolder.asBinder() : null);
                    this.mRemote.transact(9, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onNotificationSnoozedUntilContext(IStatusBarNotificationHolder iStatusBarNotificationHolder, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iStatusBarNotificationHolder != null ? iStatusBarNotificationHolder.asBinder() : null);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(10, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
