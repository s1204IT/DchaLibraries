package com.android.bluetoothmidiservice;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.media.midi.IBluetoothMidiService;
import android.os.IBinder;
import android.util.Log;
import java.util.HashMap;

public class BluetoothMidiService extends Service {
    private final HashMap<BluetoothDevice, BluetoothMidiDevice> mDeviceServerMap = new HashMap<>();
    private final IBluetoothMidiService.Stub mBinder = new IBluetoothMidiService.Stub() {
        public IBinder addBluetoothDevice(BluetoothDevice bluetoothDevice) {
            BluetoothMidiDevice bluetoothMidiDevice;
            if (bluetoothDevice != null) {
                synchronized (BluetoothMidiService.this.mDeviceServerMap) {
                    bluetoothMidiDevice = (BluetoothMidiDevice) BluetoothMidiService.this.mDeviceServerMap.get(bluetoothDevice);
                    if (bluetoothMidiDevice == null) {
                        bluetoothMidiDevice = new BluetoothMidiDevice(BluetoothMidiService.this, bluetoothDevice, BluetoothMidiService.this);
                        BluetoothMidiService.this.mDeviceServerMap.put(bluetoothDevice, bluetoothMidiDevice);
                    }
                }
                return bluetoothMidiDevice.getBinder();
            }
            Log.e("BluetoothMidiService", "no BluetoothDevice in addBluetoothDevice()");
            return null;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    void deviceClosed(BluetoothDevice bluetoothDevice) {
        synchronized (this.mDeviceServerMap) {
            this.mDeviceServerMap.remove(bluetoothDevice);
        }
    }
}
