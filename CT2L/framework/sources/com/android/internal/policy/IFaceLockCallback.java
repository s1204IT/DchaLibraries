package com.android.internal.policy;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IFaceLockCallback extends IInterface {
    void cancel() throws RemoteException;

    void pokeWakelock(int i) throws RemoteException;

    void reportFailedAttempt() throws RemoteException;

    void unlock() throws RemoteException;

    public static abstract class Stub extends Binder implements IFaceLockCallback {
        private static final String DESCRIPTOR = "com.android.internal.policy.IFaceLockCallback";
        static final int TRANSACTION_cancel = 2;
        static final int TRANSACTION_pokeWakelock = 4;
        static final int TRANSACTION_reportFailedAttempt = 3;
        static final int TRANSACTION_unlock = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IFaceLockCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IFaceLockCallback)) {
                return (IFaceLockCallback) iin;
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
                    unlock();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    cancel();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    reportFailedAttempt();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    pokeWakelock(_arg0);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IFaceLockCallback {
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
            public void unlock() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void cancel() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void reportFailedAttempt() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void pokeWakelock(int millis) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(millis);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
