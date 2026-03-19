package android.bluetooth;

import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface IBluetoothAvrcpController extends IInterface {
    List<BluetoothDevice> getConnectedDevices() throws RemoteException;

    int getConnectionState(BluetoothDevice bluetoothDevice) throws RemoteException;

    List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) throws RemoteException;

    MediaMetadata getMetadata(BluetoothDevice bluetoothDevice) throws RemoteException;

    PlaybackState getPlaybackState(BluetoothDevice bluetoothDevice) throws RemoteException;

    BluetoothAvrcpPlayerSettings getPlayerSettings(BluetoothDevice bluetoothDevice) throws RemoteException;

    void sendGroupNavigationCmd(BluetoothDevice bluetoothDevice, int i, int i2) throws RemoteException;

    void sendPassThroughCmd(BluetoothDevice bluetoothDevice, int i, int i2) throws RemoteException;

    boolean setPlayerApplicationSetting(BluetoothAvrcpPlayerSettings bluetoothAvrcpPlayerSettings) throws RemoteException;

    public static abstract class Stub extends Binder implements IBluetoothAvrcpController {
        private static final String DESCRIPTOR = "android.bluetooth.IBluetoothAvrcpController";
        static final int TRANSACTION_getConnectedDevices = 1;
        static final int TRANSACTION_getConnectionState = 3;
        static final int TRANSACTION_getDevicesMatchingConnectionStates = 2;
        static final int TRANSACTION_getMetadata = 6;
        static final int TRANSACTION_getPlaybackState = 7;
        static final int TRANSACTION_getPlayerSettings = 5;
        static final int TRANSACTION_sendGroupNavigationCmd = 9;
        static final int TRANSACTION_sendPassThroughCmd = 4;
        static final int TRANSACTION_setPlayerApplicationSetting = 8;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IBluetoothAvrcpController asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IBluetoothAvrcpController)) {
                return (IBluetoothAvrcpController) iin;
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
            BluetoothAvrcpPlayerSettings bluetoothAvrcpPlayerSettingsCreateFromParcel;
            BluetoothDevice bluetoothDeviceCreateFromParcel2;
            BluetoothDevice bluetoothDeviceCreateFromParcel3;
            BluetoothDevice bluetoothDeviceCreateFromParcel4;
            BluetoothDevice bluetoothDeviceCreateFromParcel5;
            BluetoothDevice bluetoothDeviceCreateFromParcel6;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    List<BluetoothDevice> _result = getConnectedDevices();
                    reply.writeNoException();
                    reply.writeTypedList(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int[] _arg0 = data.createIntArray();
                    List<BluetoothDevice> _result2 = getDevicesMatchingConnectionStates(_arg0);
                    reply.writeNoException();
                    reply.writeTypedList(_result2);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothDeviceCreateFromParcel6 = BluetoothDevice.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothDeviceCreateFromParcel6 = null;
                    }
                    int _result3 = getConnectionState(bluetoothDeviceCreateFromParcel6);
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothDeviceCreateFromParcel5 = BluetoothDevice.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothDeviceCreateFromParcel5 = null;
                    }
                    int _arg1 = data.readInt();
                    int _arg2 = data.readInt();
                    sendPassThroughCmd(bluetoothDeviceCreateFromParcel5, _arg1, _arg2);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothDeviceCreateFromParcel4 = BluetoothDevice.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothDeviceCreateFromParcel4 = null;
                    }
                    BluetoothAvrcpPlayerSettings _result4 = getPlayerSettings(bluetoothDeviceCreateFromParcel4);
                    reply.writeNoException();
                    if (_result4 != null) {
                        reply.writeInt(1);
                        _result4.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothDeviceCreateFromParcel3 = BluetoothDevice.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothDeviceCreateFromParcel3 = null;
                    }
                    MediaMetadata _result5 = getMetadata(bluetoothDeviceCreateFromParcel3);
                    reply.writeNoException();
                    if (_result5 != null) {
                        reply.writeInt(1);
                        _result5.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothDeviceCreateFromParcel2 = BluetoothDevice.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothDeviceCreateFromParcel2 = null;
                    }
                    PlaybackState _result6 = getPlaybackState(bluetoothDeviceCreateFromParcel2);
                    reply.writeNoException();
                    if (_result6 != null) {
                        reply.writeInt(1);
                        _result6.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothAvrcpPlayerSettingsCreateFromParcel = BluetoothAvrcpPlayerSettings.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothAvrcpPlayerSettingsCreateFromParcel = null;
                    }
                    boolean _result7 = setPlayerApplicationSetting(bluetoothAvrcpPlayerSettingsCreateFromParcel);
                    reply.writeNoException();
                    reply.writeInt(_result7 ? 1 : 0);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bluetoothDeviceCreateFromParcel = BluetoothDevice.CREATOR.createFromParcel(data);
                    } else {
                        bluetoothDeviceCreateFromParcel = null;
                    }
                    int _arg12 = data.readInt();
                    int _arg22 = data.readInt();
                    sendGroupNavigationCmd(bluetoothDeviceCreateFromParcel, _arg12, _arg22);
                    reply.writeNoException();
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IBluetoothAvrcpController {
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
            public List<BluetoothDevice> getConnectedDevices() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    List<BluetoothDevice> _result = _reply.createTypedArrayList(BluetoothDevice.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(states);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    List<BluetoothDevice> _result = _reply.createTypedArrayList(BluetoothDevice.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getConnectionState(BluetoothDevice device) throws RemoteException {
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
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void sendPassThroughCmd(BluetoothDevice device, int keyCode, int keyState) throws RemoteException {
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
                    _data.writeInt(keyCode);
                    _data.writeInt(keyState);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public BluetoothAvrcpPlayerSettings getPlayerSettings(BluetoothDevice device) throws RemoteException {
                BluetoothAvrcpPlayerSettings bluetoothAvrcpPlayerSettingsCreateFromParcel;
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
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        bluetoothAvrcpPlayerSettingsCreateFromParcel = BluetoothAvrcpPlayerSettings.CREATOR.createFromParcel(_reply);
                    } else {
                        bluetoothAvrcpPlayerSettingsCreateFromParcel = null;
                    }
                    return bluetoothAvrcpPlayerSettingsCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public MediaMetadata getMetadata(BluetoothDevice device) throws RemoteException {
                MediaMetadata mediaMetadataCreateFromParcel;
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
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        mediaMetadataCreateFromParcel = MediaMetadata.CREATOR.createFromParcel(_reply);
                    } else {
                        mediaMetadataCreateFromParcel = null;
                    }
                    return mediaMetadataCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public PlaybackState getPlaybackState(BluetoothDevice device) throws RemoteException {
                PlaybackState playbackStateCreateFromParcel;
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
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        playbackStateCreateFromParcel = PlaybackState.CREATOR.createFromParcel(_reply);
                    } else {
                        playbackStateCreateFromParcel = null;
                    }
                    return playbackStateCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setPlayerApplicationSetting(BluetoothAvrcpPlayerSettings plAppSetting) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (plAppSetting != null) {
                        _data.writeInt(1);
                        plAppSetting.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void sendGroupNavigationCmd(BluetoothDevice device, int keyCode, int keyState) throws RemoteException {
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
                    _data.writeInt(keyCode);
                    _data.writeInt(keyState);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
