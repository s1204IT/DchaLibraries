package android.os;

public interface IProcessInfoService extends IInterface {
    void getProcessStatesAndOomScoresFromPids(int[] iArr, int[] iArr2, int[] iArr3) throws RemoteException;

    void getProcessStatesFromPids(int[] iArr, int[] iArr2) throws RemoteException;

    public static abstract class Stub extends Binder implements IProcessInfoService {
        private static final String DESCRIPTOR = "android.os.IProcessInfoService";
        static final int TRANSACTION_getProcessStatesAndOomScoresFromPids = 2;
        static final int TRANSACTION_getProcessStatesFromPids = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IProcessInfoService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IProcessInfoService)) {
                return (IProcessInfoService) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int[] iArr;
            int[] iArr2;
            int[] iArr3;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    int[] _arg0 = data.createIntArray();
                    int _arg1_length = data.readInt();
                    if (_arg1_length < 0) {
                        iArr3 = null;
                    } else {
                        iArr3 = new int[_arg1_length];
                    }
                    getProcessStatesFromPids(_arg0, iArr3);
                    reply.writeNoException();
                    reply.writeIntArray(iArr3);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int[] _arg02 = data.createIntArray();
                    int _arg1_length2 = data.readInt();
                    if (_arg1_length2 < 0) {
                        iArr = null;
                    } else {
                        iArr = new int[_arg1_length2];
                    }
                    int _arg2_length = data.readInt();
                    if (_arg2_length < 0) {
                        iArr2 = null;
                    } else {
                        iArr2 = new int[_arg2_length];
                    }
                    getProcessStatesAndOomScoresFromPids(_arg02, iArr, iArr2);
                    reply.writeNoException();
                    reply.writeIntArray(iArr);
                    reply.writeIntArray(iArr2);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IProcessInfoService {
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
            public void getProcessStatesFromPids(int[] pids, int[] states) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(pids);
                    if (states == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(states.length);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    _reply.readIntArray(states);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void getProcessStatesAndOomScoresFromPids(int[] pids, int[] states, int[] scores) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(pids);
                    if (states == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(states.length);
                    }
                    if (scores == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(scores.length);
                    }
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    _reply.readIntArray(states);
                    _reply.readIntArray(scores);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
