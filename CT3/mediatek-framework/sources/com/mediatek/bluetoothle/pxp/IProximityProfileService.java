package com.mediatek.bluetoothle.pxp;

import android.bluetooth.BluetoothDevice;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.mediatek.bluetoothle.pxp.IProximityProfileServiceCallback;

public interface IProximityProfileService extends IInterface {
    int getPathLoss(BluetoothDevice bluetoothDevice) throws RemoteException;

    boolean getPxpParameters(BluetoothDevice bluetoothDevice, int[] iArr, int[] iArr2, int[] iArr3, int[] iArr4, int[] iArr5) throws RemoteException;

    boolean isAlertOn(BluetoothDevice bluetoothDevice) throws RemoteException;

    boolean registerStatusChangeCallback(BluetoothDevice bluetoothDevice, IProximityProfileServiceCallback iProximityProfileServiceCallback) throws RemoteException;

    boolean setPxpParameters(BluetoothDevice bluetoothDevice, int i, int i2, int i3, int i4, int i5) throws RemoteException;

    boolean stopRemoteAlert(BluetoothDevice bluetoothDevice) throws RemoteException;

    boolean unregisterStatusChangeCallback(BluetoothDevice bluetoothDevice, IProximityProfileServiceCallback iProximityProfileServiceCallback) throws RemoteException;

    public static abstract class Stub extends Binder implements IProximityProfileService {
        private static final String DESCRIPTOR = "com.mediatek.bluetoothle.pxp.IProximityProfileService";
        static final int TRANSACTION_getPathLoss = 1;
        static final int TRANSACTION_getPxpParameters = 7;
        static final int TRANSACTION_isAlertOn = 2;
        static final int TRANSACTION_registerStatusChangeCallback = 4;
        static final int TRANSACTION_setPxpParameters = 6;
        static final int TRANSACTION_stopRemoteAlert = 3;
        static final int TRANSACTION_unregisterStatusChangeCallback = 5;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IProximityProfileService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IProximityProfileService)) {
                return (IProximityProfileService) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            BluetoothDevice bluetoothDevice;
            int[] iArr;
            int[] iArr2;
            int[] iArr3;
            int[] iArr4;
            int[] iArr5;
            BluetoothDevice bluetoothDevice2;
            BluetoothDevice bluetoothDevice3;
            BluetoothDevice bluetoothDevice4;
            BluetoothDevice bluetoothDevice5;
            BluetoothDevice bluetoothDevice6;
            BluetoothDevice bluetoothDevice7;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothDevice7 = (BluetoothDevice) BluetoothDevice.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothDevice7 = null;
                    }
                    int _result = getPathLoss(bluetoothDevice7);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothDevice6 = (BluetoothDevice) BluetoothDevice.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothDevice6 = null;
                    }
                    boolean _result2 = isAlertOn(bluetoothDevice6);
                    reply.writeNoException();
                    reply.writeInt(_result2 ? 1 : 0);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothDevice5 = (BluetoothDevice) BluetoothDevice.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothDevice5 = null;
                    }
                    boolean _result3 = stopRemoteAlert(bluetoothDevice5);
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothDevice4 = (BluetoothDevice) BluetoothDevice.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothDevice4 = null;
                    }
                    IProximityProfileServiceCallback _arg1 = IProximityProfileServiceCallback.Stub.asInterface(data.readStrongBinder());
                    boolean _result4 = registerStatusChangeCallback(bluetoothDevice4, _arg1);
                    reply.writeNoException();
                    reply.writeInt(_result4 ? 1 : 0);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothDevice3 = (BluetoothDevice) BluetoothDevice.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothDevice3 = null;
                    }
                    IProximityProfileServiceCallback _arg12 = IProximityProfileServiceCallback.Stub.asInterface(data.readStrongBinder());
                    boolean _result5 = unregisterStatusChangeCallback(bluetoothDevice3, _arg12);
                    reply.writeNoException();
                    reply.writeInt(_result5 ? 1 : 0);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothDevice2 = (BluetoothDevice) BluetoothDevice.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothDevice2 = null;
                    }
                    int _arg13 = data.readInt();
                    int _arg2 = data.readInt();
                    int _arg3 = data.readInt();
                    int _arg4 = data.readInt();
                    int _arg5 = data.readInt();
                    boolean _result6 = setPxpParameters(bluetoothDevice2, _arg13, _arg2, _arg3, _arg4, _arg5);
                    reply.writeNoException();
                    reply.writeInt(_result6 ? 1 : 0);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothDevice = (BluetoothDevice) BluetoothDevice.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothDevice = null;
                    }
                    int _arg1_length = data.readInt();
                    if (_arg1_length < 0) {
                        iArr = null;
                    } else {
                        iArr = new int[_arg1_length];
                    }
                    int _arg2_length = data.readInt();
                    if (_arg2_length < 0) {
                        iArr2 = null;
                    } else {
                        iArr2 = new int[_arg2_length];
                    }
                    int _arg3_length = data.readInt();
                    if (_arg3_length < 0) {
                        iArr3 = null;
                    } else {
                        iArr3 = new int[_arg3_length];
                    }
                    int _arg4_length = data.readInt();
                    if (_arg4_length < 0) {
                        iArr4 = null;
                    } else {
                        iArr4 = new int[_arg4_length];
                    }
                    int _arg5_length = data.readInt();
                    if (_arg5_length < 0) {
                        iArr5 = null;
                    } else {
                        iArr5 = new int[_arg5_length];
                    }
                    boolean _result7 = getPxpParameters(bluetoothDevice, iArr, iArr2, iArr3, iArr4, iArr5);
                    reply.writeNoException();
                    reply.writeInt(_result7 ? 1 : 0);
                    reply.writeIntArray(iArr);
                    reply.writeIntArray(iArr2);
                    reply.writeIntArray(iArr3);
                    reply.writeIntArray(iArr4);
                    reply.writeIntArray(iArr5);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IProximityProfileService {
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
            public int getPathLoss(BluetoothDevice device) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isAlertOn(BluetoothDevice device) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean stopRemoteAlert(BluetoothDevice device) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean registerStatusChangeCallback(BluetoothDevice device, IProximityProfileServiceCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean unregisterStatusChangeCallback(BluetoothDevice device, IProximityProfileServiceCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setPxpParameters(BluetoothDevice device, int alertEnabler, int rangeAlertEnabler, int rangeType, int rangeValue, int disconnectEnabler) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(alertEnabler);
                    _data.writeInt(rangeAlertEnabler);
                    _data.writeInt(rangeType);
                    _data.writeInt(rangeValue);
                    _data.writeInt(disconnectEnabler);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getPxpParameters(BluetoothDevice device, int[] alertEnabler, int[] rangeAlertEnabler, int[] rangeType, int[] rangeValue, int[] disconnectEnabler) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (alertEnabler == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(alertEnabler.length);
                    }
                    if (rangeAlertEnabler == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(rangeAlertEnabler.length);
                    }
                    if (rangeType == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(rangeType.length);
                    }
                    if (rangeValue == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(rangeValue.length);
                    }
                    if (disconnectEnabler == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(disconnectEnabler.length);
                    }
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    _reply.readIntArray(alertEnabler);
                    _reply.readIntArray(rangeAlertEnabler);
                    _reply.readIntArray(rangeType);
                    _reply.readIntArray(rangeValue);
                    _reply.readIntArray(disconnectEnabler);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
