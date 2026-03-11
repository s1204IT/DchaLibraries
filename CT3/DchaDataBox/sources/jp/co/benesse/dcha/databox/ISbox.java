package jp.co.benesse.dcha.databox;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ISbox extends IInterface {
    String getAppIdentifier(int i) throws RemoteException;

    String getArrayValues(String str) throws RemoteException;

    String getAuthUrl(int i) throws RemoteException;

    String getStringValue(String str) throws RemoteException;

    void setArrayValues(String str, String str2) throws RemoteException;

    void setStringValue(String str, String str2) throws RemoteException;

    public static abstract class Stub extends Binder implements ISbox {
        private static final String DESCRIPTOR = "jp.co.benesse.dcha.databox.ISbox";
        static final int TRANSACTION_getAppIdentifier = 5;
        static final int TRANSACTION_getArrayValues = 3;
        static final int TRANSACTION_getAuthUrl = 6;
        static final int TRANSACTION_getStringValue = 1;
        static final int TRANSACTION_setArrayValues = 4;
        static final int TRANSACTION_setStringValue = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISbox asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ISbox)) {
                return (ISbox) iin;
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
                    String _arg0 = data.readString();
                    String _result = getStringValue(_arg0);
                    reply.writeNoException();
                    reply.writeString(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg02 = data.readString();
                    String _arg1 = data.readString();
                    setStringValue(_arg02, _arg1);
                    reply.writeNoException();
                    return true;
                case TRANSACTION_getArrayValues:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    String _result2 = getArrayValues(_arg03);
                    reply.writeNoException();
                    reply.writeString(_result2);
                    return true;
                case TRANSACTION_setArrayValues:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    String _arg12 = data.readString();
                    setArrayValues(_arg04, _arg12);
                    reply.writeNoException();
                    return true;
                case TRANSACTION_getAppIdentifier:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg05 = data.readInt();
                    String _result3 = getAppIdentifier(_arg05);
                    reply.writeNoException();
                    reply.writeString(_result3);
                    return true;
                case TRANSACTION_getAuthUrl:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg06 = data.readInt();
                    String _result4 = getAuthUrl(_arg06);
                    reply.writeNoException();
                    reply.writeString(_result4);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements ISbox {
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
            public String getStringValue(String key) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(key);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setStringValue(String key, String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(key);
                    _data.writeString(value);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getArrayValues(String key) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(key);
                    this.mRemote.transact(Stub.TRANSACTION_getArrayValues, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setArrayValues(String key, String values) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(key);
                    _data.writeString(values);
                    this.mRemote.transact(Stub.TRANSACTION_setArrayValues, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getAppIdentifier(int serverType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(serverType);
                    this.mRemote.transact(Stub.TRANSACTION_getAppIdentifier, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getAuthUrl(int serverType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(serverType);
                    this.mRemote.transact(Stub.TRANSACTION_getAuthUrl, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
