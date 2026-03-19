package android.net.wifi.nan;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IWifiNanEventListener extends IInterface {
    void onConfigCompleted(ConfigRequest configRequest) throws RemoteException;

    void onConfigFailed(ConfigRequest configRequest, int i) throws RemoteException;

    void onIdentityChanged() throws RemoteException;

    void onNanDown(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IWifiNanEventListener {
        private static final String DESCRIPTOR = "android.net.wifi.nan.IWifiNanEventListener";
        static final int TRANSACTION_onConfigCompleted = 1;
        static final int TRANSACTION_onConfigFailed = 2;
        static final int TRANSACTION_onIdentityChanged = 4;
        static final int TRANSACTION_onNanDown = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IWifiNanEventListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IWifiNanEventListener)) {
                return (IWifiNanEventListener) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            ConfigRequest configRequestCreateFromParcel;
            ConfigRequest configRequestCreateFromParcel2;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        configRequestCreateFromParcel2 = ConfigRequest.CREATOR.createFromParcel(data);
                    } else {
                        configRequestCreateFromParcel2 = null;
                    }
                    onConfigCompleted(configRequestCreateFromParcel2);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        configRequestCreateFromParcel = ConfigRequest.CREATOR.createFromParcel(data);
                    } else {
                        configRequestCreateFromParcel = null;
                    }
                    int _arg1 = data.readInt();
                    onConfigFailed(configRequestCreateFromParcel, _arg1);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    onNanDown(_arg0);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    onIdentityChanged();
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IWifiNanEventListener {
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
            public void onConfigCompleted(ConfigRequest completedConfig) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (completedConfig != null) {
                        _data.writeInt(1);
                        completedConfig.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onConfigFailed(ConfigRequest failedConfig, int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (failedConfig != null) {
                        _data.writeInt(1);
                        failedConfig.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(reason);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onNanDown(int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(reason);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onIdentityChanged() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
