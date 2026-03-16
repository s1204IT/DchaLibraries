package android.bluetooth;

import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.ScanResult;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.RemoteException;
import java.util.List;

public interface IBluetoothGattCallback extends IInterface {
    void onBatchScanResults(List<ScanResult> list) throws RemoteException;

    void onCharacteristicRead(String str, int i, int i2, int i3, ParcelUuid parcelUuid, int i4, ParcelUuid parcelUuid2, byte[] bArr) throws RemoteException;

    void onCharacteristicWrite(String str, int i, int i2, int i3, ParcelUuid parcelUuid, int i4, ParcelUuid parcelUuid2) throws RemoteException;

    void onClientConnectionState(int i, int i2, boolean z, String str) throws RemoteException;

    void onClientRegistered(int i, int i2) throws RemoteException;

    void onConfigureMTU(String str, int i, int i2) throws RemoteException;

    void onDescriptorRead(String str, int i, int i2, int i3, ParcelUuid parcelUuid, int i4, ParcelUuid parcelUuid2, int i5, ParcelUuid parcelUuid3, byte[] bArr) throws RemoteException;

    void onDescriptorWrite(String str, int i, int i2, int i3, ParcelUuid parcelUuid, int i4, ParcelUuid parcelUuid2, int i5, ParcelUuid parcelUuid3) throws RemoteException;

    void onExecuteWrite(String str, int i) throws RemoteException;

    void onFoundOrLost(boolean z, ScanResult scanResult) throws RemoteException;

    void onGetCharacteristic(String str, int i, int i2, ParcelUuid parcelUuid, int i3, ParcelUuid parcelUuid2, int i4) throws RemoteException;

    void onGetDescriptor(String str, int i, int i2, ParcelUuid parcelUuid, int i3, ParcelUuid parcelUuid2, int i4, ParcelUuid parcelUuid3) throws RemoteException;

    void onGetIncludedService(String str, int i, int i2, ParcelUuid parcelUuid, int i3, int i4, ParcelUuid parcelUuid2) throws RemoteException;

    void onGetService(String str, int i, int i2, ParcelUuid parcelUuid) throws RemoteException;

    void onMultiAdvertiseCallback(int i, boolean z, AdvertiseSettings advertiseSettings) throws RemoteException;

    void onNotify(String str, int i, int i2, ParcelUuid parcelUuid, int i3, ParcelUuid parcelUuid2, byte[] bArr) throws RemoteException;

    void onReadRemoteRssi(String str, int i, int i2) throws RemoteException;

    void onScanResult(ScanResult scanResult) throws RemoteException;

    void onSearchComplete(String str, int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IBluetoothGattCallback {
        private static final String DESCRIPTOR = "android.bluetooth.IBluetoothGattCallback";
        static final int TRANSACTION_onBatchScanResults = 4;
        static final int TRANSACTION_onCharacteristicRead = 10;
        static final int TRANSACTION_onCharacteristicWrite = 11;
        static final int TRANSACTION_onClientConnectionState = 2;
        static final int TRANSACTION_onClientRegistered = 1;
        static final int TRANSACTION_onConfigureMTU = 18;
        static final int TRANSACTION_onDescriptorRead = 13;
        static final int TRANSACTION_onDescriptorWrite = 14;
        static final int TRANSACTION_onExecuteWrite = 12;
        static final int TRANSACTION_onFoundOrLost = 19;
        static final int TRANSACTION_onGetCharacteristic = 7;
        static final int TRANSACTION_onGetDescriptor = 8;
        static final int TRANSACTION_onGetIncludedService = 6;
        static final int TRANSACTION_onGetService = 5;
        static final int TRANSACTION_onMultiAdvertiseCallback = 17;
        static final int TRANSACTION_onNotify = 15;
        static final int TRANSACTION_onReadRemoteRssi = 16;
        static final int TRANSACTION_onScanResult = 3;
        static final int TRANSACTION_onSearchComplete = 9;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IBluetoothGattCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IBluetoothGattCallback)) {
                return (IBluetoothGattCallback) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            ScanResult _arg1;
            AdvertiseSettings _arg2;
            ParcelUuid _arg3;
            ParcelUuid _arg5;
            ParcelUuid _arg4;
            ParcelUuid _arg6;
            ParcelUuid _arg8;
            ParcelUuid _arg42;
            ParcelUuid _arg62;
            ParcelUuid _arg82;
            ParcelUuid _arg43;
            ParcelUuid _arg63;
            ParcelUuid _arg44;
            ParcelUuid _arg64;
            ParcelUuid _arg32;
            ParcelUuid _arg52;
            ParcelUuid _arg7;
            ParcelUuid _arg33;
            ParcelUuid _arg53;
            ParcelUuid _arg34;
            ParcelUuid _arg65;
            ParcelUuid _arg35;
            ScanResult _arg0;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg02 = data.readInt();
                    int _arg12 = data.readInt();
                    onClientRegistered(_arg02, _arg12);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg03 = data.readInt();
                    int _arg13 = data.readInt();
                    boolean _arg22 = data.readInt() != 0;
                    String _arg36 = data.readString();
                    onClientConnectionState(_arg03, _arg13, _arg22, _arg36);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = ScanResult.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    onScanResult(_arg0);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    List<ScanResult> _arg04 = data.createTypedArrayList(ScanResult.CREATOR);
                    onBatchScanResults(_arg04);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    int _arg14 = data.readInt();
                    int _arg23 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg35 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        _arg35 = null;
                    }
                    onGetService(_arg05, _arg14, _arg23, _arg35);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg06 = data.readString();
                    int _arg15 = data.readInt();
                    int _arg24 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg34 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        _arg34 = null;
                    }
                    int _arg45 = data.readInt();
                    int _arg54 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg65 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        _arg65 = null;
                    }
                    onGetIncludedService(_arg06, _arg15, _arg24, _arg34, _arg45, _arg54, _arg65);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg07 = data.readString();
                    int _arg16 = data.readInt();
                    int _arg25 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg33 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        _arg33 = null;
                    }
                    int _arg46 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg53 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        _arg53 = null;
                    }
                    int _arg66 = data.readInt();
                    onGetCharacteristic(_arg07, _arg16, _arg25, _arg33, _arg46, _arg53, _arg66);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg08 = data.readString();
                    int _arg17 = data.readInt();
                    int _arg26 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg32 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        _arg32 = null;
                    }
                    int _arg47 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg52 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        _arg52 = null;
                    }
                    int _arg67 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg7 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        _arg7 = null;
                    }
                    onGetDescriptor(_arg08, _arg17, _arg26, _arg32, _arg47, _arg52, _arg67, _arg7);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg09 = data.readString();
                    int _arg18 = data.readInt();
                    onSearchComplete(_arg09, _arg18);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg010 = data.readString();
                    int _arg19 = data.readInt();
                    int _arg27 = data.readInt();
                    int _arg37 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg44 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        _arg44 = null;
                    }
                    int _arg55 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg64 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        _arg64 = null;
                    }
                    byte[] _arg72 = data.createByteArray();
                    onCharacteristicRead(_arg010, _arg19, _arg27, _arg37, _arg44, _arg55, _arg64, _arg72);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg011 = data.readString();
                    int _arg110 = data.readInt();
                    int _arg28 = data.readInt();
                    int _arg38 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg43 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        _arg43 = null;
                    }
                    int _arg56 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg63 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        _arg63 = null;
                    }
                    onCharacteristicWrite(_arg011, _arg110, _arg28, _arg38, _arg43, _arg56, _arg63);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg012 = data.readString();
                    int _arg111 = data.readInt();
                    onExecuteWrite(_arg012, _arg111);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg013 = data.readString();
                    int _arg112 = data.readInt();
                    int _arg29 = data.readInt();
                    int _arg39 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg42 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        _arg42 = null;
                    }
                    int _arg57 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg62 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        _arg62 = null;
                    }
                    int _arg73 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg82 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        _arg82 = null;
                    }
                    byte[] _arg9 = data.createByteArray();
                    onDescriptorRead(_arg013, _arg112, _arg29, _arg39, _arg42, _arg57, _arg62, _arg73, _arg82, _arg9);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg014 = data.readString();
                    int _arg113 = data.readInt();
                    int _arg210 = data.readInt();
                    int _arg310 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg4 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        _arg4 = null;
                    }
                    int _arg58 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg6 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        _arg6 = null;
                    }
                    int _arg74 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg8 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        _arg8 = null;
                    }
                    onDescriptorWrite(_arg014, _arg113, _arg210, _arg310, _arg4, _arg58, _arg6, _arg74, _arg8);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg015 = data.readString();
                    int _arg114 = data.readInt();
                    int _arg211 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg3 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        _arg3 = null;
                    }
                    int _arg48 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg5 = ParcelUuid.CREATOR.createFromParcel(data);
                    } else {
                        _arg5 = null;
                    }
                    byte[] _arg68 = data.createByteArray();
                    onNotify(_arg015, _arg114, _arg211, _arg3, _arg48, _arg5, _arg68);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg016 = data.readString();
                    int _arg115 = data.readInt();
                    int _arg212 = data.readInt();
                    onReadRemoteRssi(_arg016, _arg115, _arg212);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg017 = data.readInt();
                    boolean _arg116 = data.readInt() != 0;
                    if (data.readInt() != 0) {
                        _arg2 = AdvertiseSettings.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    onMultiAdvertiseCallback(_arg017, _arg116, _arg2);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg018 = data.readString();
                    int _arg117 = data.readInt();
                    int _arg213 = data.readInt();
                    onConfigureMTU(_arg018, _arg117, _arg213);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg019 = data.readInt() != 0;
                    if (data.readInt() != 0) {
                        _arg1 = ScanResult.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    onFoundOrLost(_arg019, _arg1);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IBluetoothGattCallback {
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
            public void onClientRegistered(int status, int clientIf) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(status);
                    _data.writeInt(clientIf);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onClientConnectionState(int status, int clientIf, boolean connected, String address) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(status);
                    _data.writeInt(clientIf);
                    _data.writeInt(connected ? 1 : 0);
                    _data.writeString(address);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onScanResult(ScanResult scanResult) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (scanResult != null) {
                        _data.writeInt(1);
                        scanResult.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> batchResults) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(batchResults);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onGetService(String address, int srvcType, int srvcInstId, ParcelUuid srvcUuid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(srvcType);
                    _data.writeInt(srvcInstId);
                    if (srvcUuid != null) {
                        _data.writeInt(1);
                        srvcUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onGetIncludedService(String address, int srvcType, int srvcInstId, ParcelUuid srvcUuid, int inclSrvcType, int inclSrvcInstId, ParcelUuid inclSrvcUuid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(srvcType);
                    _data.writeInt(srvcInstId);
                    if (srvcUuid != null) {
                        _data.writeInt(1);
                        srvcUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(inclSrvcType);
                    _data.writeInt(inclSrvcInstId);
                    if (inclSrvcUuid != null) {
                        _data.writeInt(1);
                        inclSrvcUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onGetCharacteristic(String address, int srvcType, int srvcInstId, ParcelUuid srvcUuid, int charInstId, ParcelUuid charUuid, int charProps) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(srvcType);
                    _data.writeInt(srvcInstId);
                    if (srvcUuid != null) {
                        _data.writeInt(1);
                        srvcUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(charInstId);
                    if (charUuid != null) {
                        _data.writeInt(1);
                        charUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(charProps);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onGetDescriptor(String address, int srvcType, int srvcInstId, ParcelUuid srvcUuid, int charInstId, ParcelUuid charUuid, int descrInstId, ParcelUuid descrUuid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(srvcType);
                    _data.writeInt(srvcInstId);
                    if (srvcUuid != null) {
                        _data.writeInt(1);
                        srvcUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(charInstId);
                    if (charUuid != null) {
                        _data.writeInt(1);
                        charUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(descrInstId);
                    if (descrUuid != null) {
                        _data.writeInt(1);
                        descrUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onSearchComplete(String address, int status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(status);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onCharacteristicRead(String address, int status, int srvcType, int srvcInstId, ParcelUuid srvcUuid, int charInstId, ParcelUuid charUuid, byte[] value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(status);
                    _data.writeInt(srvcType);
                    _data.writeInt(srvcInstId);
                    if (srvcUuid != null) {
                        _data.writeInt(1);
                        srvcUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(charInstId);
                    if (charUuid != null) {
                        _data.writeInt(1);
                        charUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeByteArray(value);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onCharacteristicWrite(String address, int status, int srvcType, int srvcInstId, ParcelUuid srvcUuid, int charInstId, ParcelUuid charUuid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(status);
                    _data.writeInt(srvcType);
                    _data.writeInt(srvcInstId);
                    if (srvcUuid != null) {
                        _data.writeInt(1);
                        srvcUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(charInstId);
                    if (charUuid != null) {
                        _data.writeInt(1);
                        charUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onExecuteWrite(String address, int status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(status);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onDescriptorRead(String address, int status, int srvcType, int srvcInstId, ParcelUuid srvcUuid, int charInstId, ParcelUuid charUuid, int descrInstId, ParcelUuid descrUuid, byte[] value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(status);
                    _data.writeInt(srvcType);
                    _data.writeInt(srvcInstId);
                    if (srvcUuid != null) {
                        _data.writeInt(1);
                        srvcUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(charInstId);
                    if (charUuid != null) {
                        _data.writeInt(1);
                        charUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(descrInstId);
                    if (descrUuid != null) {
                        _data.writeInt(1);
                        descrUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeByteArray(value);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onDescriptorWrite(String address, int status, int srvcType, int srvcInstId, ParcelUuid srvcUuid, int charInstId, ParcelUuid charUuid, int descrInstId, ParcelUuid descrUuid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(status);
                    _data.writeInt(srvcType);
                    _data.writeInt(srvcInstId);
                    if (srvcUuid != null) {
                        _data.writeInt(1);
                        srvcUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(charInstId);
                    if (charUuid != null) {
                        _data.writeInt(1);
                        charUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(descrInstId);
                    if (descrUuid != null) {
                        _data.writeInt(1);
                        descrUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onNotify(String address, int srvcType, int srvcInstId, ParcelUuid srvcUuid, int charInstId, ParcelUuid charUuid, byte[] value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(srvcType);
                    _data.writeInt(srvcInstId);
                    if (srvcUuid != null) {
                        _data.writeInt(1);
                        srvcUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(charInstId);
                    if (charUuid != null) {
                        _data.writeInt(1);
                        charUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeByteArray(value);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onReadRemoteRssi(String address, int rssi, int status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(rssi);
                    _data.writeInt(status);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onMultiAdvertiseCallback(int status, boolean isStart, AdvertiseSettings advertiseSettings) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(status);
                    _data.writeInt(isStart ? 1 : 0);
                    if (advertiseSettings != null) {
                        _data.writeInt(1);
                        advertiseSettings.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onConfigureMTU(String address, int mtu, int status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(mtu);
                    _data.writeInt(status);
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onFoundOrLost(boolean onFound, ScanResult scanResult) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(onFound ? 1 : 0);
                    if (scanResult != null) {
                        _data.writeInt(1);
                        scanResult.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
