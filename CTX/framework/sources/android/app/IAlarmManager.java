package android.app;

import android.app.AlarmManager;
import android.app.IAlarmListener;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.WorkSource;

public interface IAlarmManager extends IInterface {
    void cancelPoweroffAlarm(String str) throws RemoteException;

    long currentNetworkTimeMillis() throws RemoteException;

    AlarmManager.AlarmClockInfo getNextAlarmClock(int i) throws RemoteException;

    long getNextWakeFromIdleTime() throws RemoteException;

    void remove(PendingIntent pendingIntent, IAlarmListener iAlarmListener) throws RemoteException;

    void set(String str, int i, long j, long j2, long j3, int i2, PendingIntent pendingIntent, IAlarmListener iAlarmListener, String str2, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClockInfo) throws RemoteException;

    boolean setTime(long j) throws RemoteException;

    void setTimeZone(String str) throws RemoteException;

    public static abstract class Stub extends Binder implements IAlarmManager {
        private static final String DESCRIPTOR = "android.app.IAlarmManager";
        static final int TRANSACTION_cancelPoweroffAlarm = 7;
        static final int TRANSACTION_currentNetworkTimeMillis = 8;
        static final int TRANSACTION_getNextAlarmClock = 6;
        static final int TRANSACTION_getNextWakeFromIdleTime = 5;
        static final int TRANSACTION_remove = 4;
        static final int TRANSACTION_set = 1;
        static final int TRANSACTION_setTime = 2;
        static final int TRANSACTION_setTimeZone = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAlarmManager asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IAlarmManager)) {
                return (IAlarmManager) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            PendingIntent pendingIntentCreateFromParcel;
            WorkSource workSourceCreateFromParcel;
            AlarmManager.AlarmClockInfo alarmClockInfoCreateFromParcel;
            PendingIntent pendingIntentCreateFromParcel2;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string = parcel.readString();
                    int i3 = parcel.readInt();
                    long j = parcel.readLong();
                    long j2 = parcel.readLong();
                    long j3 = parcel.readLong();
                    int i4 = parcel.readInt();
                    if (parcel.readInt() != 0) {
                        pendingIntentCreateFromParcel = PendingIntent.CREATOR.createFromParcel(parcel);
                    } else {
                        pendingIntentCreateFromParcel = null;
                    }
                    IAlarmListener iAlarmListenerAsInterface = IAlarmListener.Stub.asInterface(parcel.readStrongBinder());
                    String string2 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        workSourceCreateFromParcel = WorkSource.CREATOR.createFromParcel(parcel);
                    } else {
                        workSourceCreateFromParcel = null;
                    }
                    if (parcel.readInt() != 0) {
                        alarmClockInfoCreateFromParcel = AlarmManager.AlarmClockInfo.CREATOR.createFromParcel(parcel);
                    } else {
                        alarmClockInfoCreateFromParcel = null;
                    }
                    set(string, i3, j, j2, j3, i4, pendingIntentCreateFromParcel, iAlarmListenerAsInterface, string2, workSourceCreateFromParcel, alarmClockInfoCreateFromParcel);
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean time = setTime(parcel.readLong());
                    parcel2.writeNoException();
                    parcel2.writeInt(time ? 1 : 0);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    setTimeZone(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        pendingIntentCreateFromParcel2 = PendingIntent.CREATOR.createFromParcel(parcel);
                    } else {
                        pendingIntentCreateFromParcel2 = null;
                    }
                    remove(pendingIntentCreateFromParcel2, IAlarmListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    long nextWakeFromIdleTime = getNextWakeFromIdleTime();
                    parcel2.writeNoException();
                    parcel2.writeLong(nextWakeFromIdleTime);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    AlarmManager.AlarmClockInfo nextAlarmClock = getNextAlarmClock(parcel.readInt());
                    parcel2.writeNoException();
                    if (nextAlarmClock != null) {
                        parcel2.writeInt(1);
                        nextAlarmClock.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    cancelPoweroffAlarm(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    long jCurrentNetworkTimeMillis = currentNetworkTimeMillis();
                    parcel2.writeNoException();
                    parcel2.writeLong(jCurrentNetworkTimeMillis);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IAlarmManager {
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
            public void set(String str, int i, long j, long j2, long j3, int i2, PendingIntent pendingIntent, IAlarmListener iAlarmListener, String str2, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClockInfo) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeLong(j2);
                    parcelObtain.writeLong(j3);
                    parcelObtain.writeInt(i2);
                    if (pendingIntent != null) {
                        parcelObtain.writeInt(1);
                        pendingIntent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iAlarmListener != null ? iAlarmListener.asBinder() : null);
                    parcelObtain.writeString(str2);
                    if (workSource != null) {
                        parcelObtain.writeInt(1);
                        workSource.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (alarmClockInfo != null) {
                        parcelObtain.writeInt(1);
                        alarmClockInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setTime(long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setTimeZone(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void remove(PendingIntent pendingIntent, IAlarmListener iAlarmListener) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (pendingIntent != null) {
                        parcelObtain.writeInt(1);
                        pendingIntent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iAlarmListener != null ? iAlarmListener.asBinder() : null);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long getNextWakeFromIdleTime() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readLong();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public AlarmManager.AlarmClockInfo getNextAlarmClock(int i) throws RemoteException {
                AlarmManager.AlarmClockInfo alarmClockInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        alarmClockInfoCreateFromParcel = AlarmManager.AlarmClockInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        alarmClockInfoCreateFromParcel = null;
                    }
                    return alarmClockInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void cancelPoweroffAlarm(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long currentNetworkTimeMillis() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readLong();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
