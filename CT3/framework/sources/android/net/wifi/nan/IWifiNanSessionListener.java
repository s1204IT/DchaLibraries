package android.net.wifi.nan;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IWifiNanSessionListener extends IInterface {
    void onMatch(int i, byte[] bArr, int i2, byte[] bArr2, int i3) throws RemoteException;

    void onMessageReceived(int i, byte[] bArr, int i2) throws RemoteException;

    void onMessageSendFail(int i, int i2) throws RemoteException;

    void onMessageSendSuccess(int i) throws RemoteException;

    void onPublishFail(int i) throws RemoteException;

    void onPublishTerminated(int i) throws RemoteException;

    void onSubscribeFail(int i) throws RemoteException;

    void onSubscribeTerminated(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IWifiNanSessionListener {
        private static final String DESCRIPTOR = "android.net.wifi.nan.IWifiNanSessionListener";
        static final int TRANSACTION_onMatch = 5;
        static final int TRANSACTION_onMessageReceived = 8;
        static final int TRANSACTION_onMessageSendFail = 7;
        static final int TRANSACTION_onMessageSendSuccess = 6;
        static final int TRANSACTION_onPublishFail = 1;
        static final int TRANSACTION_onPublishTerminated = 2;
        static final int TRANSACTION_onSubscribeFail = 3;
        static final int TRANSACTION_onSubscribeTerminated = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IWifiNanSessionListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IWifiNanSessionListener)) {
                return (IWifiNanSessionListener) iin;
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
                    onPublishFail(_arg0);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg02 = data.readInt();
                    onPublishTerminated(_arg02);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg03 = data.readInt();
                    onSubscribeFail(_arg03);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    onSubscribeTerminated(_arg04);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg05 = data.readInt();
                    byte[] _arg1 = data.createByteArray();
                    int _arg2 = data.readInt();
                    byte[] _arg3 = data.createByteArray();
                    int _arg4 = data.readInt();
                    onMatch(_arg05, _arg1, _arg2, _arg3, _arg4);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg06 = data.readInt();
                    onMessageSendSuccess(_arg06);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg07 = data.readInt();
                    int _arg12 = data.readInt();
                    onMessageSendFail(_arg07, _arg12);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg08 = data.readInt();
                    byte[] _arg13 = data.createByteArray();
                    int _arg22 = data.readInt();
                    onMessageReceived(_arg08, _arg13, _arg22);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IWifiNanSessionListener {
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
            public void onPublishFail(int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(reason);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onPublishTerminated(int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(reason);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onSubscribeFail(int reason) throws RemoteException {
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
            public void onSubscribeTerminated(int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(reason);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onMatch(int peerId, byte[] serviceSpecificInfo, int serviceSpecificInfoLength, byte[] matchFilter, int matchFilterLength) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(peerId);
                    _data.writeByteArray(serviceSpecificInfo);
                    _data.writeInt(serviceSpecificInfoLength);
                    _data.writeByteArray(matchFilter);
                    _data.writeInt(matchFilterLength);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onMessageSendSuccess(int messageId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(messageId);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onMessageSendFail(int messageId, int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(messageId);
                    _data.writeInt(reason);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onMessageReceived(int peerId, byte[] message, int messageLength) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(peerId);
                    _data.writeByteArray(message);
                    _data.writeInt(messageLength);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
