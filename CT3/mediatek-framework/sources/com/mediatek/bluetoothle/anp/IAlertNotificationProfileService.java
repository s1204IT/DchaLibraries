package com.mediatek.bluetoothle.anp;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IAlertNotificationProfileService extends IInterface {
    int[] getDeviceSettings(String str, int[] iArr) throws RemoteException;

    int[] getRemoteSettings(String str, int[] iArr) throws RemoteException;

    boolean updateDeviceSettings(String str, int[] iArr, int[] iArr2) throws RemoteException;

    public static abstract class Stub extends Binder implements IAlertNotificationProfileService {
        private static final String DESCRIPTOR = "com.mediatek.bluetoothle.anp.IAlertNotificationProfileService";
        static final int TRANSACTION_getDeviceSettings = 1;
        static final int TRANSACTION_getRemoteSettings = 2;
        static final int TRANSACTION_updateDeviceSettings = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAlertNotificationProfileService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IAlertNotificationProfileService)) {
                return (IAlertNotificationProfileService) iin;
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
                    int[] _arg1 = data.createIntArray();
                    int[] _result = getDeviceSettings(_arg0, _arg1);
                    reply.writeNoException();
                    reply.writeIntArray(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg02 = data.readString();
                    int[] _arg12 = data.createIntArray();
                    int[] _result2 = getRemoteSettings(_arg02, _arg12);
                    reply.writeNoException();
                    reply.writeIntArray(_result2);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    int[] _arg13 = data.createIntArray();
                    int[] _arg2 = data.createIntArray();
                    boolean _result3 = updateDeviceSettings(_arg03, _arg13, _arg2);
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IAlertNotificationProfileService {
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
            public int[] getDeviceSettings(String address, int[] categoryArray) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeIntArray(categoryArray);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int[] getRemoteSettings(String address, int[] categoryArray) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeIntArray(categoryArray);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean updateDeviceSettings(String address, int[] categoryArray, int[] valueArray) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeIntArray(categoryArray);
                    _data.writeIntArray(valueArray);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
