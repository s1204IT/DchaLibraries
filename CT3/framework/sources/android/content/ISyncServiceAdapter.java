package android.content;

import android.content.ISyncContext;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ISyncServiceAdapter extends IInterface {
    void cancelSync(ISyncContext iSyncContext) throws RemoteException;

    void startSync(ISyncContext iSyncContext, Bundle bundle) throws RemoteException;

    public static abstract class Stub extends Binder implements ISyncServiceAdapter {
        private static final String DESCRIPTOR = "android.content.ISyncServiceAdapter";
        static final int TRANSACTION_cancelSync = 2;
        static final int TRANSACTION_startSync = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISyncServiceAdapter asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ISyncServiceAdapter)) {
                return (ISyncServiceAdapter) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Bundle bundleCreateFromParcel;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    ISyncContext _arg0 = ISyncContext.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        bundleCreateFromParcel = null;
                    }
                    startSync(_arg0, bundleCreateFromParcel);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    ISyncContext _arg02 = ISyncContext.Stub.asInterface(data.readStrongBinder());
                    cancelSync(_arg02);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements ISyncServiceAdapter {
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
            public void startSync(ISyncContext syncContext, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(syncContext != null ? syncContext.asBinder() : null);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void cancelSync(ISyncContext syncContext) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(syncContext != null ? syncContext.asBinder() : null);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
