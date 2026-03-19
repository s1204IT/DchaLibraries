package com.android.bluetoothmidiservice;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.media.midi.MidiDeviceServer;
import android.media.midi.MidiDeviceStatus;
import android.media.midi.MidiManager;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import com.android.bluetoothmidiservice.PacketEncoder;
import com.android.internal.midi.MidiEventScheduler;
import java.io.IOException;
import java.util.UUID;
import libcore.io.IoUtils;

public final class BluetoothMidiDevice {
    private final BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mCharacteristic;
    private MidiDeviceServer mDeviceServer;
    private final MidiManager mMidiManager;
    private MidiReceiver mOutputReceiver;
    private final BluetoothMidiService mService;
    private static final UUID MIDI_SERVICE = UUID.fromString("03B80E5A-EDE8-4B33-A751-6CE34EC4C700");
    private static final UUID MIDI_CHARACTERISTIC = UUID.fromString("7772E5DB-3868-4112-A1A9-F2669D106BF3");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final MidiEventScheduler mEventScheduler = new MidiEventScheduler();
    private final PacketReceiver mPacketReceiver = new PacketReceiver();
    private final BluetoothPacketEncoder mPacketEncoder = new BluetoothPacketEncoder(this.mPacketReceiver, 20);
    private final BluetoothPacketDecoder mPacketDecoder = new BluetoothPacketDecoder(20);
    private final MidiDeviceServer.Callback mDeviceServerCallback = new MidiDeviceServer.Callback() {
        public void onDeviceStatusChanged(MidiDeviceServer midiDeviceServer, MidiDeviceStatus midiDeviceStatus) {
        }

        public void onClose() {
            BluetoothMidiDevice.this.close();
        }
    };
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt bluetoothGatt, int i, int i2) {
            if (i2 == 2) {
                Log.d("BluetoothMidiDevice", "Connected to GATT server.");
                Log.d("BluetoothMidiDevice", "Attempting to start service discovery:" + BluetoothMidiDevice.this.mBluetoothGatt.discoverServices());
                return;
            }
            if (i2 == 0) {
                Log.i("BluetoothMidiDevice", "Disconnected from GATT server.");
                BluetoothMidiDevice.this.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int i) {
            if (i == 0) {
                BluetoothGattService service = bluetoothGatt.getService(BluetoothMidiDevice.MIDI_SERVICE);
                if (service != null) {
                    Log.d("BluetoothMidiDevice", "found MIDI_SERVICE");
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(BluetoothMidiDevice.MIDI_CHARACTERISTIC);
                    if (characteristic != null) {
                        Log.d("BluetoothMidiDevice", "found MIDI_CHARACTERISTIC");
                        BluetoothMidiDevice.this.mCharacteristic = characteristic;
                        Log.d("BluetoothMidiDevice", "requestConnectionPriority(CONNECTION_PRIORITY_HIGH):" + bluetoothGatt.requestConnectionPriority(1));
                        BluetoothMidiDevice.this.mBluetoothGatt.readCharacteristic(characteristic);
                        return;
                    }
                    return;
                }
                return;
            }
            Log.e("BluetoothMidiDevice", "onServicesDiscovered received: " + i);
            BluetoothMidiDevice.this.close();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic, int i) {
            Log.d("BluetoothMidiDevice", "onCharacteristicRead " + i);
            BluetoothMidiDevice.this.mBluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
            int writeType = bluetoothGattCharacteristic.getWriteType();
            bluetoothGattCharacteristic.setWriteType(2);
            BluetoothGattDescriptor descriptor = bluetoothGattCharacteristic.getDescriptor(BluetoothMidiDevice.CLIENT_CHARACTERISTIC_CONFIG);
            if (descriptor == null) {
                Log.e("BluetoothMidiDevice", "No CLIENT_CHARACTERISTIC_CONFIG for device " + BluetoothMidiDevice.this.mBluetoothDevice);
            } else {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                Log.d("BluetoothMidiDevice", "writeDescriptor returned " + BluetoothMidiDevice.this.mBluetoothGatt.writeDescriptor(descriptor));
            }
            bluetoothGattCharacteristic.setWriteType(writeType);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic, int i) {
            Log.d("BluetoothMidiDevice", "onCharacteristicWrite " + i);
            BluetoothMidiDevice.this.mPacketEncoder.writeComplete();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic) {
            BluetoothMidiDevice.this.mPacketDecoder.decodePacket(bluetoothGattCharacteristic.getValue(), BluetoothMidiDevice.this.mOutputReceiver);
        }
    };

    private class PacketReceiver implements PacketEncoder.PacketReceiver {
        private final byte[][] mWriteBuffers = new byte[21][];

        public PacketReceiver() {
            for (int i = 0; i <= 20; i++) {
                this.mWriteBuffers[i] = new byte[i];
            }
        }

        @Override
        public void writePacket(byte[] bArr, int i) {
            if (BluetoothMidiDevice.this.mCharacteristic == null) {
                Log.w("BluetoothMidiDevice", "not ready to send packet yet");
                return;
            }
            byte[] bArr2 = this.mWriteBuffers[i];
            System.arraycopy(bArr, 0, bArr2, 0, i);
            BluetoothMidiDevice.this.mCharacteristic.setValue(bArr2);
            BluetoothMidiDevice.this.mBluetoothGatt.writeCharacteristic(BluetoothMidiDevice.this.mCharacteristic);
        }
    }

    public BluetoothMidiDevice(Context context, BluetoothDevice bluetoothDevice, BluetoothMidiService bluetoothMidiService) {
        this.mBluetoothDevice = bluetoothDevice;
        this.mService = bluetoothMidiService;
        this.mBluetoothGatt = this.mBluetoothDevice.connectGatt(context, false, this.mGattCallback);
        this.mMidiManager = (MidiManager) context.getSystemService("midi");
        Bundle bundle = new Bundle();
        bundle.putString("name", this.mBluetoothGatt.getDevice().getName());
        bundle.putParcelable("bluetooth_device", this.mBluetoothGatt.getDevice());
        this.mDeviceServer = this.mMidiManager.createDeviceServer(new MidiReceiver[]{this.mEventScheduler.getReceiver()}, 1, null, null, bundle, 3, this.mDeviceServerCallback);
        this.mOutputReceiver = this.mDeviceServer.getOutputPortReceivers()[0];
        new Thread("BluetoothMidiDevice " + this.mBluetoothDevice) {
            @Override
            public void run() {
                MidiEventScheduler.MidiEvent midiEventWaitNextEvent;
                while (true) {
                    try {
                        midiEventWaitNextEvent = BluetoothMidiDevice.this.mEventScheduler.waitNextEvent();
                    } catch (InterruptedException e) {
                    }
                    if (midiEventWaitNextEvent != null) {
                        try {
                            BluetoothMidiDevice.this.mPacketEncoder.send(midiEventWaitNextEvent.data, 0, midiEventWaitNextEvent.count, midiEventWaitNextEvent.getTimestamp());
                        } catch (IOException e2) {
                            Log.e("BluetoothMidiDevice", "mPacketAccumulator.send failed", e2);
                        }
                        BluetoothMidiDevice.this.mEventScheduler.addEventToPool(midiEventWaitNextEvent);
                    } else {
                        Log.d("BluetoothMidiDevice", "BluetoothMidiDevice thread exit");
                        return;
                    }
                }
            }
        }.start();
    }

    private void close() {
        synchronized (this.mBluetoothDevice) {
            this.mEventScheduler.close();
            this.mService.deviceClosed(this.mBluetoothDevice);
            if (this.mDeviceServer != null) {
                IoUtils.closeQuietly(this.mDeviceServer);
                this.mDeviceServer = null;
            }
            if (this.mBluetoothGatt != null) {
                this.mBluetoothGatt.close();
                this.mBluetoothGatt = null;
            }
        }
    }

    public IBinder getBinder() {
        return this.mDeviceServer.asBinder();
    }
}
