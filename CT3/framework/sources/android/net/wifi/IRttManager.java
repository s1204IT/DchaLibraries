package android.net.wifi;

import android.net.wifi.RttManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Messenger;
import android.os.Parcel;
import android.os.RemoteException;

public interface IRttManager extends IInterface {
    Messenger getMessenger() throws RemoteException;

    RttManager.RttCapabilities getRttCapabilities() throws RemoteException;

    public static abstract class Stub extends Binder implements IRttManager {
        private static final String DESCRIPTOR = "android.net.wifi.IRttManager";
        static final int TRANSACTION_getMessenger = 1;
        static final int TRANSACTION_getRttCapabilities = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IRttManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IRttManager)) {
                return (IRttManager) iin;
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
                    Messenger _result = getMessenger();
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
                    RttManager.RttCapabilities _result2 = getRttCapabilities();
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

        private static class Proxy implements IRttManager {
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
            public Messenger getMessenger() throws RemoteException {
                Messenger messengerCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        messengerCreateFromParcel = Messenger.CREATOR.createFromParcel(_reply);
                    } else {
                        messengerCreateFromParcel = null;
                    }
                    return messengerCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public RttManager.RttCapabilities getRttCapabilities() throws RemoteException {
                RttManager.RttCapabilities rttCapabilitiesCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        rttCapabilitiesCreateFromParcel = RttManager.RttCapabilities.CREATOR.createFromParcel(_reply);
                    } else {
                        rttCapabilitiesCreateFromParcel = null;
                    }
                    return rttCapabilitiesCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
