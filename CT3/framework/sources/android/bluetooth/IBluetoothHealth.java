package android.bluetooth;

import android.bluetooth.IBluetoothHealthCallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import java.util.List;

public interface IBluetoothHealth extends IInterface {
    boolean connectChannelToSink(BluetoothDevice bluetoothDevice, BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration, int i) throws RemoteException;

    boolean connectChannelToSource(BluetoothDevice bluetoothDevice, BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration) throws RemoteException;

    boolean disconnectChannel(BluetoothDevice bluetoothDevice, BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration, int i) throws RemoteException;

    List<BluetoothDevice> getConnectedHealthDevices() throws RemoteException;

    int getHealthDeviceConnectionState(BluetoothDevice bluetoothDevice) throws RemoteException;

    List<BluetoothDevice> getHealthDevicesMatchingConnectionStates(int[] iArr) throws RemoteException;

    ParcelFileDescriptor getMainChannelFd(BluetoothDevice bluetoothDevice, BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration) throws RemoteException;

    boolean registerAppConfiguration(BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration, IBluetoothHealthCallback iBluetoothHealthCallback) throws RemoteException;

    boolean unregisterAppConfiguration(BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration) throws RemoteException;

    public static abstract class Stub extends Binder implements IBluetoothHealth {
        private static final String DESCRIPTOR = "android.bluetooth.IBluetoothHealth";
        static final int TRANSACTION_connectChannelToSink = 4;
        static final int TRANSACTION_connectChannelToSource = 3;
        static final int TRANSACTION_disconnectChannel = 5;
        static final int TRANSACTION_getConnectedHealthDevices = 7;
        static final int TRANSACTION_getHealthDeviceConnectionState = 9;
        static final int TRANSACTION_getHealthDevicesMatchingConnectionStates = 8;
        static final int TRANSACTION_getMainChannelFd = 6;
        static final int TRANSACTION_registerAppConfiguration = 1;
        static final int TRANSACTION_unregisterAppConfiguration = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IBluetoothHealth asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IBluetoothHealth)) {
                return (IBluetoothHealth) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            BluetoothDevice bluetoothDeviceCreateFromParcel;
            BluetoothDevice bluetoothDeviceCreateFromParcel2;
            BluetoothHealthAppConfiguration bluetoothHealthAppConfigurationCreateFromParcel;
            BluetoothDevice bluetoothDeviceCreateFromParcel3;
            BluetoothHealthAppConfiguration bluetoothHealthAppConfigurationCreateFromParcel2;
            BluetoothDevice bluetoothDeviceCreateFromParcel4;
            BluetoothHealthAppConfiguration bluetoothHealthAppConfigurationCreateFromParcel3;
            BluetoothDevice bluetoothDeviceCreateFromParcel5;
            BluetoothHealthAppConfiguration bluetoothHealthAppConfigurationCreateFromParcel4;
            BluetoothHealthAppConfiguration bluetoothHealthAppConfigurationCreateFromParcel5;
            BluetoothHealthAppConfiguration bluetoothHealthAppConfigurationCreateFromParcel6;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothHealthAppConfigurationCreateFromParcel6 = BluetoothHealthAppConfiguration.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothHealthAppConfigurationCreateFromParcel6 = null;
                    }
                    IBluetoothHealthCallback _arg1 = IBluetoothHealthCallback.Stub.asInterface(data.readStrongBinder());
                    boolean _result = registerAppConfiguration(bluetoothHealthAppConfigurationCreateFromParcel6, _arg1);
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothHealthAppConfigurationCreateFromParcel5 = BluetoothHealthAppConfiguration.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothHealthAppConfigurationCreateFromParcel5 = null;
                    }
                    boolean _result2 = unregisterAppConfiguration(bluetoothHealthAppConfigurationCreateFromParcel5);
                    reply.writeNoException();
                    reply.writeInt(_result2 ? 1 : 0);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothDeviceCreateFromParcel5 = BluetoothDevice.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothDeviceCreateFromParcel5 = null;
                    }
                    if (data.readInt() != 0) {
                        bluetoothHealthAppConfigurationCreateFromParcel4 = BluetoothHealthAppConfiguration.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothHealthAppConfigurationCreateFromParcel4 = null;
                    }
                    boolean _result3 = connectChannelToSource(bluetoothDeviceCreateFromParcel5, bluetoothHealthAppConfigurationCreateFromParcel4);
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothDeviceCreateFromParcel4 = BluetoothDevice.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothDeviceCreateFromParcel4 = null;
                    }
                    if (data.readInt() != 0) {
                        bluetoothHealthAppConfigurationCreateFromParcel3 = BluetoothHealthAppConfiguration.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothHealthAppConfigurationCreateFromParcel3 = null;
                    }
                    int _arg2 = data.readInt();
                    boolean _result4 = connectChannelToSink(bluetoothDeviceCreateFromParcel4, bluetoothHealthAppConfigurationCreateFromParcel3, _arg2);
                    reply.writeNoException();
                    reply.writeInt(_result4 ? 1 : 0);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothDeviceCreateFromParcel3 = BluetoothDevice.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothDeviceCreateFromParcel3 = null;
                    }
                    if (data.readInt() != 0) {
                        bluetoothHealthAppConfigurationCreateFromParcel2 = BluetoothHealthAppConfiguration.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothHealthAppConfigurationCreateFromParcel2 = null;
                    }
                    int _arg22 = data.readInt();
                    boolean _result5 = disconnectChannel(bluetoothDeviceCreateFromParcel3, bluetoothHealthAppConfigurationCreateFromParcel2, _arg22);
                    reply.writeNoException();
                    reply.writeInt(_result5 ? 1 : 0);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothDeviceCreateFromParcel2 = BluetoothDevice.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothDeviceCreateFromParcel2 = null;
                    }
                    if (data.readInt() != 0) {
                        bluetoothHealthAppConfigurationCreateFromParcel = BluetoothHealthAppConfiguration.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothHealthAppConfigurationCreateFromParcel = null;
                    }
                    ParcelFileDescriptor _result6 = getMainChannelFd(bluetoothDeviceCreateFromParcel2, bluetoothHealthAppConfigurationCreateFromParcel);
                    reply.writeNoException();
                    if (_result6 != null) {
                        reply.writeInt(1);
                        _result6.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    List<BluetoothDevice> _result7 = getConnectedHealthDevices();
                    reply.writeNoException();
                    reply.writeTypedList(_result7);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    int[] _arg0 = data.createIntArray();
                    List<BluetoothDevice> _result8 = getHealthDevicesMatchingConnectionStates(_arg0);
                    reply.writeNoException();
                    reply.writeTypedList(_result8);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothDeviceCreateFromParcel = BluetoothDevice.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothDeviceCreateFromParcel = null;
                    }
                    int _result9 = getHealthDeviceConnectionState(bluetoothDeviceCreateFromParcel);
                    reply.writeNoException();
                    reply.writeInt(_result9);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IBluetoothHealth {
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
            public boolean registerAppConfiguration(BluetoothHealthAppConfiguration config, IBluetoothHealthCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (config != null) {
                        _data.writeInt(1);
                        config.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean unregisterAppConfiguration(BluetoothHealthAppConfiguration config) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (config != null) {
                        _data.writeInt(1);
                        config.writeToParcel(_data, 0);
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
            public boolean connectChannelToSource(BluetoothDevice device, BluetoothHealthAppConfiguration config) throws RemoteException {
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
                    if (config != null) {
                        _data.writeInt(1);
                        config.writeToParcel(_data, 0);
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
            public boolean connectChannelToSink(BluetoothDevice device, BluetoothHealthAppConfiguration config, int channelType) throws RemoteException {
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
                    if (config != null) {
                        _data.writeInt(1);
                        config.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(channelType);
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
            public boolean disconnectChannel(BluetoothDevice device, BluetoothHealthAppConfiguration config, int id) throws RemoteException {
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
                    if (config != null) {
                        _data.writeInt(1);
                        config.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(id);
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
            public ParcelFileDescriptor getMainChannelFd(BluetoothDevice device, BluetoothHealthAppConfiguration config) throws RemoteException {
                ParcelFileDescriptor parcelFileDescriptorCreateFromParcel;
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
                    if (config != null) {
                        _data.writeInt(1);
                        config.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        parcelFileDescriptorCreateFromParcel = ParcelFileDescriptor.CREATOR.createFromParcel(_reply);
                    } else {
                        parcelFileDescriptorCreateFromParcel = null;
                    }
                    return parcelFileDescriptorCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<BluetoothDevice> getConnectedHealthDevices() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    List<BluetoothDevice> _result = _reply.createTypedArrayList(BluetoothDevice.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<BluetoothDevice> getHealthDevicesMatchingConnectionStates(int[] states) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(states);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    List<BluetoothDevice> _result = _reply.createTypedArrayList(BluetoothDevice.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getHealthDeviceConnectionState(BluetoothDevice device) throws RemoteException {
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
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
