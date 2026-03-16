package android.service.trust;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.service.trust.ITrustAgentServiceCallback;
import java.util.List;

public interface ITrustAgentService extends IInterface {
    void onConfigure(List<PersistableBundle> list, IBinder iBinder) throws RemoteException;

    void onDeviceLocked() throws RemoteException;

    void onDeviceUnlocked() throws RemoteException;

    void onTrustTimeout() throws RemoteException;

    void onUnlockAttempt(boolean z) throws RemoteException;

    void setCallback(ITrustAgentServiceCallback iTrustAgentServiceCallback) throws RemoteException;

    public static abstract class Stub extends Binder implements ITrustAgentService {
        private static final String DESCRIPTOR = "android.service.trust.ITrustAgentService";
        static final int TRANSACTION_onConfigure = 5;
        static final int TRANSACTION_onDeviceLocked = 3;
        static final int TRANSACTION_onDeviceUnlocked = 4;
        static final int TRANSACTION_onTrustTimeout = 2;
        static final int TRANSACTION_onUnlockAttempt = 1;
        static final int TRANSACTION_setCallback = 6;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ITrustAgentService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ITrustAgentService)) {
                return (ITrustAgentService) iin;
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
                    boolean _arg0 = data.readInt() != 0;
                    onUnlockAttempt(_arg0);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    onTrustTimeout();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    onDeviceLocked();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    onDeviceUnlocked();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    List<PersistableBundle> _arg02 = data.createTypedArrayList(PersistableBundle.CREATOR);
                    IBinder _arg1 = data.readStrongBinder();
                    onConfigure(_arg02, _arg1);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    ITrustAgentServiceCallback _arg03 = ITrustAgentServiceCallback.Stub.asInterface(data.readStrongBinder());
                    setCallback(_arg03);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements ITrustAgentService {
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
            public void onUnlockAttempt(boolean successful) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(successful ? 1 : 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onTrustTimeout() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onDeviceLocked() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onDeviceUnlocked() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onConfigure(List<PersistableBundle> options, IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(options);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setCallback(ITrustAgentServiceCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
