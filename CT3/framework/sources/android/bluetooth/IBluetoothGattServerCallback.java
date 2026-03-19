package android.bluetooth;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.RemoteException;

public interface IBluetoothGattServerCallback extends IInterface {
    void onCharacteristicReadRequest(String str, int i, int i2, boolean z, int i3, int i4, ParcelUuid parcelUuid, int i5, ParcelUuid parcelUuid2) throws RemoteException;

    void onCharacteristicWriteRequest(String str, int i, int i2, int i3, boolean z, boolean z2, int i4, int i5, ParcelUuid parcelUuid, int i6, ParcelUuid parcelUuid2, byte[] bArr) throws RemoteException;

    void onDescriptorReadRequest(String str, int i, int i2, boolean z, int i3, int i4, ParcelUuid parcelUuid, int i5, ParcelUuid parcelUuid2, ParcelUuid parcelUuid3) throws RemoteException;

    void onDescriptorWriteRequest(String str, int i, int i2, int i3, boolean z, boolean z2, int i4, int i5, ParcelUuid parcelUuid, int i6, ParcelUuid parcelUuid2, ParcelUuid parcelUuid3, byte[] bArr) throws RemoteException;

    void onExecuteWrite(String str, int i, boolean z) throws RemoteException;

    void onMtuChanged(String str, int i) throws RemoteException;

    void onNotificationSent(String str, int i) throws RemoteException;

    void onScanResult(String str, int i, byte[] bArr) throws RemoteException;

    void onServerConnectionState(int i, int i2, boolean z, String str) throws RemoteException;

    void onServerRegistered(int i, int i2) throws RemoteException;

    void onServiceAdded(int i, int i2, int i3, ParcelUuid parcelUuid) throws RemoteException;

    public static abstract class Stub extends Binder implements IBluetoothGattServerCallback {
        private static final String DESCRIPTOR = "android.bluetooth.IBluetoothGattServerCallback";
        static final int TRANSACTION_onCharacteristicReadRequest = 5;
        static final int TRANSACTION_onCharacteristicWriteRequest = 7;
        static final int TRANSACTION_onDescriptorReadRequest = 6;
        static final int TRANSACTION_onDescriptorWriteRequest = 8;
        static final int TRANSACTION_onExecuteWrite = 9;
        static final int TRANSACTION_onMtuChanged = 11;
        static final int TRANSACTION_onNotificationSent = 10;
        static final int TRANSACTION_onScanResult = 2;
        static final int TRANSACTION_onServerConnectionState = 3;
        static final int TRANSACTION_onServerRegistered = 1;
        static final int TRANSACTION_onServiceAdded = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IBluetoothGattServerCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IBluetoothGattServerCallback)) {
                return (IBluetoothGattServerCallback) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            ParcelUuid parcelUuidCreateFromParcel;
            ParcelUuid parcelUuidCreateFromParcel2;
            ParcelUuid parcelUuidCreateFromParcel3;
            ParcelUuid parcelUuidCreateFromParcel4;
            ParcelUuid parcelUuidCreateFromParcel5;
            ParcelUuid parcelUuidCreateFromParcel6;
            ParcelUuid parcelUuidCreateFromParcel7;
            ParcelUuid parcelUuidCreateFromParcel8;
            ParcelUuid parcelUuidCreateFromParcel9;
            ParcelUuid parcelUuidCreateFromParcel10;
            ParcelUuid parcelUuidCreateFromParcel11;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    int _arg1 = data.readInt();
                    onServerRegistered(_arg0, _arg1);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg02 = data.readString();
                    int _arg12 = data.readInt();
                    byte[] _arg2 = data.createByteArray();
                    onScanResult(_arg02, _arg12, _arg2);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg03 = data.readInt();
                    int _arg13 = data.readInt();
                    boolean _arg22 = data.readInt() != 0;
                    String _arg3 = data.readString();
                    onServerConnectionState(_arg03, _arg13, _arg22, _arg3);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    int _arg14 = data.readInt();
                    int _arg23 = data.readInt();
                    if (data.readInt() != 0) {
                        parcelUuidCreateFromParcel11 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        parcelUuidCreateFromParcel11 = null;
                    }
                    onServiceAdded(_arg04, _arg14, _arg23, parcelUuidCreateFromParcel11);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    int _arg15 = data.readInt();
                    int _arg24 = data.readInt();
                    boolean _arg32 = data.readInt() != 0;
                    int _arg4 = data.readInt();
                    int _arg5 = data.readInt();
                    if (data.readInt() != 0) {
                        parcelUuidCreateFromParcel9 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        parcelUuidCreateFromParcel9 = null;
                    }
                    int _arg7 = data.readInt();
                    if (data.readInt() != 0) {
                        parcelUuidCreateFromParcel10 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        parcelUuidCreateFromParcel10 = null;
                    }
                    onCharacteristicReadRequest(_arg05, _arg15, _arg24, _arg32, _arg4, _arg5, parcelUuidCreateFromParcel9, _arg7, parcelUuidCreateFromParcel10);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg06 = data.readString();
                    int _arg16 = data.readInt();
                    int _arg25 = data.readInt();
                    boolean _arg33 = data.readInt() != 0;
                    int _arg42 = data.readInt();
                    int _arg52 = data.readInt();
                    if (data.readInt() != 0) {
                        parcelUuidCreateFromParcel6 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        parcelUuidCreateFromParcel6 = null;
                    }
                    int _arg72 = data.readInt();
                    if (data.readInt() != 0) {
                        parcelUuidCreateFromParcel7 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        parcelUuidCreateFromParcel7 = null;
                    }
                    if (data.readInt() != 0) {
                        parcelUuidCreateFromParcel8 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        parcelUuidCreateFromParcel8 = null;
                    }
                    onDescriptorReadRequest(_arg06, _arg16, _arg25, _arg33, _arg42, _arg52, parcelUuidCreateFromParcel6, _arg72, parcelUuidCreateFromParcel7, parcelUuidCreateFromParcel8);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg07 = data.readString();
                    int _arg17 = data.readInt();
                    int _arg26 = data.readInt();
                    int _arg34 = data.readInt();
                    boolean _arg43 = data.readInt() != 0;
                    boolean _arg53 = data.readInt() != 0;
                    int _arg6 = data.readInt();
                    int _arg73 = data.readInt();
                    if (data.readInt() != 0) {
                        parcelUuidCreateFromParcel4 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        parcelUuidCreateFromParcel4 = null;
                    }
                    int _arg9 = data.readInt();
                    if (data.readInt() != 0) {
                        parcelUuidCreateFromParcel5 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        parcelUuidCreateFromParcel5 = null;
                    }
                    byte[] _arg11 = data.createByteArray();
                    onCharacteristicWriteRequest(_arg07, _arg17, _arg26, _arg34, _arg43, _arg53, _arg6, _arg73, parcelUuidCreateFromParcel4, _arg9, parcelUuidCreateFromParcel5, _arg11);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg08 = data.readString();
                    int _arg18 = data.readInt();
                    int _arg27 = data.readInt();
                    int _arg35 = data.readInt();
                    boolean _arg44 = data.readInt() != 0;
                    boolean _arg54 = data.readInt() != 0;
                    int _arg62 = data.readInt();
                    int _arg74 = data.readInt();
                    if (data.readInt() != 0) {
                        parcelUuidCreateFromParcel = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        parcelUuidCreateFromParcel = null;
                    }
                    int _arg92 = data.readInt();
                    if (data.readInt() != 0) {
                        parcelUuidCreateFromParcel2 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        parcelUuidCreateFromParcel2 = null;
                    }
                    if (data.readInt() != 0) {
                        parcelUuidCreateFromParcel3 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        parcelUuidCreateFromParcel3 = null;
                    }
                    byte[] _arg122 = data.createByteArray();
                    onDescriptorWriteRequest(_arg08, _arg18, _arg27, _arg35, _arg44, _arg54, _arg62, _arg74, parcelUuidCreateFromParcel, _arg92, parcelUuidCreateFromParcel2, parcelUuidCreateFromParcel3, _arg122);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg09 = data.readString();
                    int _arg19 = data.readInt();
                    boolean _arg28 = data.readInt() != 0;
                    onExecuteWrite(_arg09, _arg19, _arg28);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg010 = data.readString();
                    int _arg110 = data.readInt();
                    onNotificationSent(_arg010, _arg110);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg011 = data.readString();
                    int _arg111 = data.readInt();
                    onMtuChanged(_arg011, _arg111);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IBluetoothGattServerCallback {
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
            public void onServerRegistered(int status, int serverIf) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(status);
                    _data.writeInt(serverIf);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onScanResult(String address, int rssi, byte[] advData) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(rssi);
                    _data.writeByteArray(advData);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onServerConnectionState(int status, int serverIf, boolean connected, String address) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(status);
                    _data.writeInt(serverIf);
                    _data.writeInt(connected ? 1 : 0);
                    _data.writeString(address);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onServiceAdded(int status, int srvcType, int srvcInstId, ParcelUuid srvcId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(status);
                    _data.writeInt(srvcType);
                    _data.writeInt(srvcInstId);
                    if (srvcId != null) {
                        _data.writeInt(1);
                        srvcId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onCharacteristicReadRequest(String address, int transId, int offset, boolean isLong, int srvcType, int srvcInstId, ParcelUuid srvcId, int charInstId, ParcelUuid charId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(transId);
                    _data.writeInt(offset);
                    _data.writeInt(isLong ? 1 : 0);
                    _data.writeInt(srvcType);
                    _data.writeInt(srvcInstId);
                    if (srvcId != null) {
                        _data.writeInt(1);
                        srvcId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(charInstId);
                    if (charId != null) {
                        _data.writeInt(1);
                        charId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onDescriptorReadRequest(String address, int transId, int offset, boolean isLong, int srvcType, int srvcInstId, ParcelUuid srvcId, int charInstId, ParcelUuid charId, ParcelUuid descrId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(transId);
                    _data.writeInt(offset);
                    _data.writeInt(isLong ? 1 : 0);
                    _data.writeInt(srvcType);
                    _data.writeInt(srvcInstId);
                    if (srvcId != null) {
                        _data.writeInt(1);
                        srvcId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(charInstId);
                    if (charId != null) {
                        _data.writeInt(1);
                        charId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (descrId != null) {
                        _data.writeInt(1);
                        descrId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onCharacteristicWriteRequest(String address, int transId, int offset, int length, boolean isPrep, boolean needRsp, int srvcType, int srvcInstId, ParcelUuid srvcId, int charInstId, ParcelUuid charId, byte[] value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(transId);
                    _data.writeInt(offset);
                    _data.writeInt(length);
                    _data.writeInt(isPrep ? 1 : 0);
                    _data.writeInt(needRsp ? 1 : 0);
                    _data.writeInt(srvcType);
                    _data.writeInt(srvcInstId);
                    if (srvcId != null) {
                        _data.writeInt(1);
                        srvcId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(charInstId);
                    if (charId != null) {
                        _data.writeInt(1);
                        charId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeByteArray(value);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onDescriptorWriteRequest(String address, int transId, int offset, int length, boolean isPrep, boolean needRsp, int srvcType, int srvcInstId, ParcelUuid srvcId, int charInstId, ParcelUuid charId, ParcelUuid descrId, byte[] value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(transId);
                    _data.writeInt(offset);
                    _data.writeInt(length);
                    _data.writeInt(isPrep ? 1 : 0);
                    _data.writeInt(needRsp ? 1 : 0);
                    _data.writeInt(srvcType);
                    _data.writeInt(srvcInstId);
                    if (srvcId != null) {
                        _data.writeInt(1);
                        srvcId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(charInstId);
                    if (charId != null) {
                        _data.writeInt(1);
                        charId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (descrId != null) {
                        _data.writeInt(1);
                        descrId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeByteArray(value);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onExecuteWrite(String address, int transId, boolean execWrite) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(transId);
                    _data.writeInt(execWrite ? 1 : 0);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onNotificationSent(String address, int status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(status);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onMtuChanged(String address, int mtu) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(mtu);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
