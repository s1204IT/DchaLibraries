package com.android.ims.internal;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.ims.internal.IImsSmsListener;

public interface IImsSms extends IInterface {
    void acknowledgeIncomingGsmSmsWithPdu(int i, boolean z, String str) throws RemoteException;

    void acknowledgeLastIncomingCdmaSms(int i, boolean z, int i2) throws RemoteException;

    void acknowledgeLastIncomingGsmSms(int i, boolean z, int i2) throws RemoteException;

    void sendImsCdmaSms(int i, byte[] bArr, int i2, int i3) throws RemoteException;

    void sendImsGsmSms(int i, String str, String str2, int i2, int i3) throws RemoteException;

    void setListener(IImsSmsListener iImsSmsListener) throws RemoteException;

    public static abstract class Stub extends Binder implements IImsSms {
        private static final String DESCRIPTOR = "com.android.ims.internal.IImsSms";
        static final int TRANSACTION_acknowledgeIncomingGsmSmsWithPdu = 5;
        static final int TRANSACTION_acknowledgeLastIncomingCdmaSms = 4;
        static final int TRANSACTION_acknowledgeLastIncomingGsmSms = 3;
        static final int TRANSACTION_sendImsCdmaSms = 2;
        static final int TRANSACTION_sendImsGsmSms = 1;
        static final int TRANSACTION_setListener = 6;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IImsSms asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IImsSms)) {
                return (IImsSms) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            boolean _arg1;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    String _arg12 = data.readString();
                    String _arg2 = data.readString();
                    int _arg3 = data.readInt();
                    int _arg4 = data.readInt();
                    sendImsGsmSms(_arg0, _arg12, _arg2, _arg3, _arg4);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg02 = data.readInt();
                    byte[] _arg13 = data.createByteArray();
                    int _arg22 = data.readInt();
                    int _arg32 = data.readInt();
                    sendImsCdmaSms(_arg02, _arg13, _arg22, _arg32);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg03 = data.readInt();
                    _arg1 = data.readInt() != 0;
                    int _arg23 = data.readInt();
                    acknowledgeLastIncomingGsmSms(_arg03, _arg1, _arg23);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    _arg1 = data.readInt() != 0;
                    int _arg24 = data.readInt();
                    acknowledgeLastIncomingCdmaSms(_arg04, _arg1, _arg24);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg05 = data.readInt();
                    _arg1 = data.readInt() != 0;
                    String _arg25 = data.readString();
                    acknowledgeIncomingGsmSmsWithPdu(_arg05, _arg1, _arg25);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    IImsSmsListener _arg06 = IImsSmsListener.Stub.asInterface(data.readStrongBinder());
                    setListener(_arg06);
                    reply.writeNoException();
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IImsSms {
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
            public void sendImsGsmSms(int serial, String smscPDU, String pdu, int retry, int messageRef) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(serial);
                    _data.writeString(smscPDU);
                    _data.writeString(pdu);
                    _data.writeInt(retry);
                    _data.writeInt(messageRef);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void sendImsCdmaSms(int serial, byte[] pdu, int retry, int messageRef) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(serial);
                    _data.writeByteArray(pdu);
                    _data.writeInt(retry);
                    _data.writeInt(messageRef);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void acknowledgeLastIncomingGsmSms(int serial, boolean success, int cause) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(serial);
                    _data.writeInt(success ? 1 : 0);
                    _data.writeInt(cause);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void acknowledgeLastIncomingCdmaSms(int serial, boolean success, int cause) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(serial);
                    _data.writeInt(success ? 1 : 0);
                    _data.writeInt(cause);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void acknowledgeIncomingGsmSmsWithPdu(int serial, boolean success, String ackPdu) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(serial);
                    _data.writeInt(success ? 1 : 0);
                    _data.writeString(ackPdu);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setListener(IImsSmsListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
