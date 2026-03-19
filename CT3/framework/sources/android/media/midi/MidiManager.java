package android.media.midi;

import android.bluetooth.BluetoothDevice;
import android.media.midi.IMidiDeviceListener;
import android.media.midi.IMidiDeviceOpenCallback;
import android.media.midi.MidiDeviceServer;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import java.util.concurrent.ConcurrentHashMap;

public final class MidiManager {
    public static final String BLUETOOTH_MIDI_SERVICE_CLASS = "com.android.bluetoothmidiservice.BluetoothMidiService";
    public static final String BLUETOOTH_MIDI_SERVICE_INTENT = "android.media.midi.BluetoothMidiService";
    public static final String BLUETOOTH_MIDI_SERVICE_PACKAGE = "com.android.bluetoothmidiservice";
    private static final String TAG = "MidiManager";
    private final IMidiManager mService;
    private final IBinder mToken = new Binder();
    private ConcurrentHashMap<DeviceCallback, DeviceListener> mDeviceListeners = new ConcurrentHashMap<>();

    public interface OnDeviceOpenedListener {
        void onDeviceOpened(MidiDevice midiDevice);
    }

    private class DeviceListener extends IMidiDeviceListener.Stub {
        private final DeviceCallback mCallback;
        private final Handler mHandler;

        public DeviceListener(DeviceCallback callback, Handler handler) {
            this.mCallback = callback;
            this.mHandler = handler;
        }

        @Override
        public void onDeviceAdded(final MidiDeviceInfo device) {
            if (this.mHandler != null) {
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        DeviceListener.this.mCallback.onDeviceAdded(device);
                    }
                });
            } else {
                this.mCallback.onDeviceAdded(device);
            }
        }

        @Override
        public void onDeviceRemoved(final MidiDeviceInfo device) {
            if (this.mHandler != null) {
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        DeviceListener.this.mCallback.onDeviceRemoved(device);
                    }
                });
            } else {
                this.mCallback.onDeviceRemoved(device);
            }
        }

        @Override
        public void onDeviceStatusChanged(final MidiDeviceStatus status) {
            if (this.mHandler != null) {
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        DeviceListener.this.mCallback.onDeviceStatusChanged(status);
                    }
                });
            } else {
                this.mCallback.onDeviceStatusChanged(status);
            }
        }
    }

    public static class DeviceCallback {
        public void onDeviceAdded(MidiDeviceInfo device) {
        }

        public void onDeviceRemoved(MidiDeviceInfo device) {
        }

        public void onDeviceStatusChanged(MidiDeviceStatus status) {
        }
    }

    public MidiManager(IMidiManager service) {
        this.mService = service;
    }

    public void registerDeviceCallback(DeviceCallback callback, Handler handler) {
        DeviceListener deviceListener = new DeviceListener(callback, handler);
        try {
            this.mService.registerListener(this.mToken, deviceListener);
            this.mDeviceListeners.put(callback, deviceListener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterDeviceCallback(DeviceCallback callback) {
        DeviceListener deviceListener = this.mDeviceListeners.remove(callback);
        if (deviceListener == null) {
            return;
        }
        try {
            this.mService.unregisterListener(this.mToken, deviceListener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public MidiDeviceInfo[] getDevices() {
        try {
            return this.mService.getDevices();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void sendOpenDeviceResponse(final MidiDevice device, final OnDeviceOpenedListener listener, Handler handler) {
        if (handler != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onDeviceOpened(device);
                }
            });
        } else {
            listener.onDeviceOpened(device);
        }
    }

    public void openDevice(final MidiDeviceInfo deviceInfo, final OnDeviceOpenedListener listener, final Handler handler) {
        IMidiDeviceOpenCallback callback = new IMidiDeviceOpenCallback.Stub() {
            @Override
            public void onDeviceOpened(IMidiDeviceServer server, IBinder deviceToken) {
                MidiDevice midiDevice;
                if (server != null) {
                    midiDevice = new MidiDevice(deviceInfo, server, MidiManager.this.mService, MidiManager.this.mToken, deviceToken);
                } else {
                    midiDevice = null;
                }
                MidiManager.this.sendOpenDeviceResponse(midiDevice, listener, handler);
            }
        };
        try {
            this.mService.openDevice(this.mToken, deviceInfo, callback);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void openBluetoothDevice(BluetoothDevice bluetoothDevice, final OnDeviceOpenedListener listener, final Handler handler) {
        IMidiDeviceOpenCallback callback = new IMidiDeviceOpenCallback.Stub() {
            @Override
            public void onDeviceOpened(IMidiDeviceServer server, IBinder deviceToken) {
                MidiDevice device;
                if (server == null) {
                    device = null;
                } else {
                    try {
                        MidiDeviceInfo deviceInfo = server.getDeviceInfo();
                        device = new MidiDevice(deviceInfo, server, MidiManager.this.mService, MidiManager.this.mToken, deviceToken);
                    } catch (RemoteException e) {
                        Log.e(MidiManager.TAG, "remote exception in getDeviceInfo()");
                        device = null;
                    }
                }
                MidiManager.this.sendOpenDeviceResponse(device, listener, handler);
            }
        };
        try {
            this.mService.openBluetoothDevice(this.mToken, bluetoothDevice, callback);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public MidiDeviceServer createDeviceServer(MidiReceiver[] inputPortReceivers, int numOutputPorts, String[] inputPortNames, String[] outputPortNames, Bundle properties, int type, MidiDeviceServer.Callback callback) {
        try {
            MidiDeviceServer server = new MidiDeviceServer(this.mService, inputPortReceivers, numOutputPorts, callback);
            MidiDeviceInfo deviceInfo = this.mService.registerDeviceServer(server.getBinderInterface(), inputPortReceivers.length, numOutputPorts, inputPortNames, outputPortNames, properties, type);
            if (deviceInfo == null) {
                Log.e(TAG, "registerVirtualDevice failed");
                return null;
            }
            return server;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
