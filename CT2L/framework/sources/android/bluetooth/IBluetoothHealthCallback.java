package android.bluetooth;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

public interface IBluetoothHealthCallback extends IInterface {
    void onHealthAppConfigurationStatusChange(BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration, int i) throws RemoteException;

    void onHealthChannelStateChange(BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration, BluetoothDevice bluetoothDevice, int i, int i2, ParcelFileDescriptor parcelFileDescriptor, int i3) throws RemoteException;

    public static abstract class Stub extends Binder implements IBluetoothHealthCallback {
        private static final String DESCRIPTOR = "android.bluetooth.IBluetoothHealthCallback";
        static final int TRANSACTION_onHealthAppConfigurationStatusChange = 1;
        static final int TRANSACTION_onHealthChannelStateChange = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IBluetoothHealthCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IBluetoothHealthCallback)) {
                return (IBluetoothHealthCallback) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            BluetoothHealthAppConfiguration _arg0;
            BluetoothDevice _arg1;
            ParcelFileDescriptor _arg4;
            BluetoothHealthAppConfiguration _arg02;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = BluetoothHealthAppConfiguration.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    int _arg12 = data.readInt();
                    onHealthAppConfigurationStatusChange(_arg02, _arg12);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = BluetoothHealthAppConfiguration.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg1 = BluetoothDevice.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    int _arg2 = data.readInt();
                    int _arg3 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg4 = ParcelFileDescriptor.CREATOR.createFromParcel(data);
                    } else {
                        _arg4 = null;
                    }
                    int _arg5 = data.readInt();
                    onHealthChannelStateChange(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
                    reply.writeNoException();
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IBluetoothHealthCallback {
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
            public void onHealthAppConfigurationStatusChange(BluetoothHealthAppConfiguration config, int status) throws RemoteException {
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
                    _data.writeInt(status);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onHealthChannelStateChange(BluetoothHealthAppConfiguration config, BluetoothDevice device, int prevState, int newState, ParcelFileDescriptor fd, int id) throws RemoteException {
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
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(prevState);
                    _data.writeInt(newState);
                    if (fd != null) {
                        _data.writeInt(1);
                        fd.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(id);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
