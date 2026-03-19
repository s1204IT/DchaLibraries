package android.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface INetworkStatsSession extends IInterface {
    void close() throws RemoteException;

    NetworkStats getDeviceSummaryForNetwork(NetworkTemplate networkTemplate, long j, long j2) throws RemoteException;

    NetworkStatsHistory getHistoryForNetwork(NetworkTemplate networkTemplate, int i) throws RemoteException;

    NetworkStatsHistory getHistoryForUid(NetworkTemplate networkTemplate, int i, int i2, int i3, int i4) throws RemoteException;

    NetworkStatsHistory getHistoryIntervalForUid(NetworkTemplate networkTemplate, int i, int i2, int i3, int i4, long j, long j2) throws RemoteException;

    long getMobileTotalBytes(int i, long j, long j2) throws RemoteException;

    int[] getRelevantUids() throws RemoteException;

    NetworkStats getSummaryForAllUid(NetworkTemplate networkTemplate, long j, long j2, boolean z) throws RemoteException;

    NetworkStats getSummaryForNetwork(NetworkTemplate networkTemplate, long j, long j2) throws RemoteException;

    public static abstract class Stub extends Binder implements INetworkStatsSession {
        private static final String DESCRIPTOR = "android.net.INetworkStatsSession";
        static final int TRANSACTION_close = 8;
        static final int TRANSACTION_getDeviceSummaryForNetwork = 1;
        static final int TRANSACTION_getHistoryForNetwork = 3;
        static final int TRANSACTION_getHistoryForUid = 5;
        static final int TRANSACTION_getHistoryIntervalForUid = 6;
        static final int TRANSACTION_getMobileTotalBytes = 9;
        static final int TRANSACTION_getRelevantUids = 7;
        static final int TRANSACTION_getSummaryForAllUid = 4;
        static final int TRANSACTION_getSummaryForNetwork = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INetworkStatsSession asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof INetworkStatsSession)) {
                return (INetworkStatsSession) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            NetworkTemplate networkTemplateCreateFromParcel;
            NetworkTemplate networkTemplateCreateFromParcel2;
            NetworkTemplate networkTemplateCreateFromParcel3;
            NetworkTemplate networkTemplateCreateFromParcel4;
            NetworkTemplate networkTemplateCreateFromParcel5;
            NetworkTemplate networkTemplateCreateFromParcel6;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        networkTemplateCreateFromParcel6 = NetworkTemplate.CREATOR.createFromParcel(data);
                    } else {
                        networkTemplateCreateFromParcel6 = null;
                    }
                    long _arg1 = data.readLong();
                    long _arg2 = data.readLong();
                    NetworkStats _result = getDeviceSummaryForNetwork(networkTemplateCreateFromParcel6, _arg1, _arg2);
                    reply.writeNoException();
                    if (_result != null) {
                        reply.writeInt(1);
                        _result.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        networkTemplateCreateFromParcel5 = NetworkTemplate.CREATOR.createFromParcel(data);
                    } else {
                        networkTemplateCreateFromParcel5 = null;
                    }
                    long _arg12 = data.readLong();
                    long _arg22 = data.readLong();
                    NetworkStats _result2 = getSummaryForNetwork(networkTemplateCreateFromParcel5, _arg12, _arg22);
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        networkTemplateCreateFromParcel4 = NetworkTemplate.CREATOR.createFromParcel(data);
                    } else {
                        networkTemplateCreateFromParcel4 = null;
                    }
                    int _arg13 = data.readInt();
                    NetworkStatsHistory _result3 = getHistoryForNetwork(networkTemplateCreateFromParcel4, _arg13);
                    reply.writeNoException();
                    if (_result3 != null) {
                        reply.writeInt(1);
                        _result3.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        networkTemplateCreateFromParcel3 = NetworkTemplate.CREATOR.createFromParcel(data);
                    } else {
                        networkTemplateCreateFromParcel3 = null;
                    }
                    long _arg14 = data.readLong();
                    long _arg23 = data.readLong();
                    boolean _arg3 = data.readInt() != 0;
                    NetworkStats _result4 = getSummaryForAllUid(networkTemplateCreateFromParcel3, _arg14, _arg23, _arg3);
                    reply.writeNoException();
                    if (_result4 != null) {
                        reply.writeInt(1);
                        _result4.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        networkTemplateCreateFromParcel2 = NetworkTemplate.CREATOR.createFromParcel(data);
                    } else {
                        networkTemplateCreateFromParcel2 = null;
                    }
                    int _arg15 = data.readInt();
                    int _arg24 = data.readInt();
                    int _arg32 = data.readInt();
                    int _arg4 = data.readInt();
                    NetworkStatsHistory _result5 = getHistoryForUid(networkTemplateCreateFromParcel2, _arg15, _arg24, _arg32, _arg4);
                    reply.writeNoException();
                    if (_result5 != null) {
                        reply.writeInt(1);
                        _result5.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        networkTemplateCreateFromParcel = NetworkTemplate.CREATOR.createFromParcel(data);
                    } else {
                        networkTemplateCreateFromParcel = null;
                    }
                    int _arg16 = data.readInt();
                    int _arg25 = data.readInt();
                    int _arg33 = data.readInt();
                    int _arg42 = data.readInt();
                    long _arg5 = data.readLong();
                    long _arg6 = data.readLong();
                    NetworkStatsHistory _result6 = getHistoryIntervalForUid(networkTemplateCreateFromParcel, _arg16, _arg25, _arg33, _arg42, _arg5, _arg6);
                    reply.writeNoException();
                    if (_result6 != null) {
                        reply.writeInt(1);
                        _result6.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    int[] _result7 = getRelevantUids();
                    reply.writeNoException();
                    reply.writeIntArray(_result7);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    close();
                    reply.writeNoException();
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    long _arg17 = data.readLong();
                    long _arg26 = data.readLong();
                    long _result8 = getMobileTotalBytes(_arg0, _arg17, _arg26);
                    reply.writeNoException();
                    reply.writeLong(_result8);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements INetworkStatsSession {
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
            public NetworkStats getDeviceSummaryForNetwork(NetworkTemplate template, long start, long end) throws RemoteException {
                NetworkStats networkStatsCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (template != null) {
                        _data.writeInt(1);
                        template.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(start);
                    _data.writeLong(end);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        networkStatsCreateFromParcel = NetworkStats.CREATOR.createFromParcel(_reply);
                    } else {
                        networkStatsCreateFromParcel = null;
                    }
                    return networkStatsCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public NetworkStats getSummaryForNetwork(NetworkTemplate template, long start, long end) throws RemoteException {
                NetworkStats networkStatsCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (template != null) {
                        _data.writeInt(1);
                        template.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(start);
                    _data.writeLong(end);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        networkStatsCreateFromParcel = NetworkStats.CREATOR.createFromParcel(_reply);
                    } else {
                        networkStatsCreateFromParcel = null;
                    }
                    return networkStatsCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public NetworkStatsHistory getHistoryForNetwork(NetworkTemplate template, int fields) throws RemoteException {
                NetworkStatsHistory networkStatsHistoryCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (template != null) {
                        _data.writeInt(1);
                        template.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(fields);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        networkStatsHistoryCreateFromParcel = NetworkStatsHistory.CREATOR.createFromParcel(_reply);
                    } else {
                        networkStatsHistoryCreateFromParcel = null;
                    }
                    return networkStatsHistoryCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public NetworkStats getSummaryForAllUid(NetworkTemplate template, long start, long end, boolean includeTags) throws RemoteException {
                NetworkStats networkStatsCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (template != null) {
                        _data.writeInt(1);
                        template.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(start);
                    _data.writeLong(end);
                    _data.writeInt(includeTags ? 1 : 0);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        networkStatsCreateFromParcel = NetworkStats.CREATOR.createFromParcel(_reply);
                    } else {
                        networkStatsCreateFromParcel = null;
                    }
                    return networkStatsCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public NetworkStatsHistory getHistoryForUid(NetworkTemplate template, int uid, int set, int tag, int fields) throws RemoteException {
                NetworkStatsHistory networkStatsHistoryCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (template != null) {
                        _data.writeInt(1);
                        template.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(uid);
                    _data.writeInt(set);
                    _data.writeInt(tag);
                    _data.writeInt(fields);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        networkStatsHistoryCreateFromParcel = NetworkStatsHistory.CREATOR.createFromParcel(_reply);
                    } else {
                        networkStatsHistoryCreateFromParcel = null;
                    }
                    return networkStatsHistoryCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public NetworkStatsHistory getHistoryIntervalForUid(NetworkTemplate template, int uid, int set, int tag, int fields, long start, long end) throws RemoteException {
                NetworkStatsHistory networkStatsHistoryCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (template != null) {
                        _data.writeInt(1);
                        template.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(uid);
                    _data.writeInt(set);
                    _data.writeInt(tag);
                    _data.writeInt(fields);
                    _data.writeLong(start);
                    _data.writeLong(end);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        networkStatsHistoryCreateFromParcel = NetworkStatsHistory.CREATOR.createFromParcel(_reply);
                    } else {
                        networkStatsHistoryCreateFromParcel = null;
                    }
                    return networkStatsHistoryCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int[] getRelevantUids() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void close() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long getMobileTotalBytes(int subSim, long start_time, long end_time) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subSim);
                    _data.writeLong(start_time);
                    _data.writeLong(end_time);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
