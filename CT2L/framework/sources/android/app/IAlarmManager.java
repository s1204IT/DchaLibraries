package android.app;

import android.app.AlarmManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.WorkSource;

public interface IAlarmManager extends IInterface {
    AlarmManager.AlarmClockInfo getNextAlarmClock(int i) throws RemoteException;

    void remove(PendingIntent pendingIntent) throws RemoteException;

    void set(int i, long j, long j2, long j3, PendingIntent pendingIntent, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClockInfo) throws RemoteException;

    boolean setTime(long j) throws RemoteException;

    void setTimeZone(String str) throws RemoteException;

    public static abstract class Stub extends Binder implements IAlarmManager {
        private static final String DESCRIPTOR = "android.app.IAlarmManager";
        static final int TRANSACTION_getNextAlarmClock = 5;
        static final int TRANSACTION_remove = 4;
        static final int TRANSACTION_set = 1;
        static final int TRANSACTION_setTime = 2;
        static final int TRANSACTION_setTimeZone = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAlarmManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IAlarmManager)) {
                return (IAlarmManager) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            PendingIntent _arg0;
            PendingIntent _arg4;
            WorkSource _arg5;
            AlarmManager.AlarmClockInfo _arg6;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg02 = data.readInt();
                    long _arg1 = data.readLong();
                    long _arg2 = data.readLong();
                    long _arg3 = data.readLong();
                    if (data.readInt() != 0) {
                        _arg4 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg4 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg5 = WorkSource.CREATOR.createFromParcel(data);
                    } else {
                        _arg5 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg6 = AlarmManager.AlarmClockInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg6 = null;
                    }
                    set(_arg02, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    long _arg03 = data.readLong();
                    boolean _result = setTime(_arg03);
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    setTimeZone(_arg04);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    remove(_arg0);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg05 = data.readInt();
                    AlarmManager.AlarmClockInfo _result2 = getNextAlarmClock(_arg05);
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
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

        private static class Proxy implements IAlarmManager {
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
            public void set(int type, long triggerAtTime, long windowLength, long interval, PendingIntent operation, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClock) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeLong(triggerAtTime);
                    _data.writeLong(windowLength);
                    _data.writeLong(interval);
                    if (operation != null) {
                        _data.writeInt(1);
                        operation.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (workSource != null) {
                        _data.writeInt(1);
                        workSource.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (alarmClock != null) {
                        _data.writeInt(1);
                        alarmClock.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setTime(long millis) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(millis);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setTimeZone(String zone) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(zone);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void remove(PendingIntent operation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (operation != null) {
                        _data.writeInt(1);
                        operation.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public AlarmManager.AlarmClockInfo getNextAlarmClock(int userId) throws RemoteException {
                AlarmManager.AlarmClockInfo _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = AlarmManager.AlarmClockInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
