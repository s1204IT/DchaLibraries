package android.app.usage;

import android.content.pm.ParceledListSlice;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IUsageStatsManager extends IInterface {
    ParceledListSlice queryConfigurationStats(int i, long j, long j2, String str) throws RemoteException;

    UsageEvents queryEvents(long j, long j2, String str) throws RemoteException;

    ParceledListSlice queryUsageStats(int i, long j, long j2, String str) throws RemoteException;

    public static abstract class Stub extends Binder implements IUsageStatsManager {
        private static final String DESCRIPTOR = "android.app.usage.IUsageStatsManager";
        static final int TRANSACTION_queryConfigurationStats = 2;
        static final int TRANSACTION_queryEvents = 3;
        static final int TRANSACTION_queryUsageStats = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IUsageStatsManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IUsageStatsManager)) {
                return (IUsageStatsManager) iin;
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
                    int _arg0 = data.readInt();
                    long _arg1 = data.readLong();
                    long _arg2 = data.readLong();
                    String _arg3 = data.readString();
                    ParceledListSlice _result = queryUsageStats(_arg0, _arg1, _arg2, _arg3);
                    reply.writeNoException();
                    if (_result != null) {
                        reply.writeInt(1);
                        _result.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg02 = data.readInt();
                    long _arg12 = data.readLong();
                    long _arg22 = data.readLong();
                    String _arg32 = data.readString();
                    ParceledListSlice _result2 = queryConfigurationStats(_arg02, _arg12, _arg22, _arg32);
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    long _arg03 = data.readLong();
                    long _arg13 = data.readLong();
                    String _arg23 = data.readString();
                    UsageEvents _result3 = queryEvents(_arg03, _arg13, _arg23);
                    reply.writeNoException();
                    if (_result3 != null) {
                        reply.writeInt(1);
                        _result3.writeToParcel(reply, 1);
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

        private static class Proxy implements IUsageStatsManager {
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
            public ParceledListSlice queryUsageStats(int bucketType, long beginTime, long endTime, String callingPackage) throws RemoteException {
                ParceledListSlice _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(bucketType);
                    _data.writeLong(beginTime);
                    _data.writeLong(endTime);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = ParceledListSlice.CREATOR.createFromParcel(_reply);
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
            public ParceledListSlice queryConfigurationStats(int bucketType, long beginTime, long endTime, String callingPackage) throws RemoteException {
                ParceledListSlice _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(bucketType);
                    _data.writeLong(beginTime);
                    _data.writeLong(endTime);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = ParceledListSlice.CREATOR.createFromParcel(_reply);
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
            public UsageEvents queryEvents(long beginTime, long endTime, String callingPackage) throws RemoteException {
                UsageEvents _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(beginTime);
                    _data.writeLong(endTime);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = UsageEvents.CREATOR.createFromParcel(_reply);
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
