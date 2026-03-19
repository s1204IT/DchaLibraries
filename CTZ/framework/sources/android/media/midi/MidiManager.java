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

        public DeviceListener(DeviceCallback deviceCallback, Handler handler) {
            this.mCallback = deviceCallback;
            this.mHandler = handler;
        }

        @Override
        public void onDeviceAdded(final MidiDeviceInfo midiDeviceInfo) {
            if (this.mHandler != null) {
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        DeviceListener.this.mCallback.onDeviceAdded(midiDeviceInfo);
                    }
                });
            } else {
                this.mCallback.onDeviceAdded(midiDeviceInfo);
            }
        }

        @Override
        public void onDeviceRemoved(final MidiDeviceInfo midiDeviceInfo) {
            if (this.mHandler != null) {
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        DeviceListener.this.mCallback.onDeviceRemoved(midiDeviceInfo);
                    }
                });
            } else {
                this.mCallback.onDeviceRemoved(midiDeviceInfo);
            }
        }

        @Override
        public void onDeviceStatusChanged(final MidiDeviceStatus midiDeviceStatus) {
            if (this.mHandler != null) {
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        DeviceListener.this.mCallback.onDeviceStatusChanged(midiDeviceStatus);
                    }
                });
            } else {
                this.mCallback.onDeviceStatusChanged(midiDeviceStatus);
            }
        }
    }

    public static class DeviceCallback {
        public void onDeviceAdded(MidiDeviceInfo midiDeviceInfo) {
        }

        public void onDeviceRemoved(MidiDeviceInfo midiDeviceInfo) {
        }

        public void onDeviceStatusChanged(MidiDeviceStatus midiDeviceStatus) {
        }
    }

    public MidiManager(IMidiManager iMidiManager) {
        this.mService = iMidiManager;
    }

    public void registerDeviceCallback(DeviceCallback deviceCallback, Handler handler) {
        DeviceListener deviceListener = new DeviceListener(deviceCallback, handler);
        try {
            this.mService.registerListener(this.mToken, deviceListener);
            this.mDeviceListeners.put(deviceCallback, deviceListener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterDeviceCallback(DeviceCallback deviceCallback) {
        DeviceListener deviceListenerRemove = this.mDeviceListeners.remove(deviceCallback);
        if (deviceListenerRemove != null) {
            try {
                this.mService.unregisterListener(this.mToken, deviceListenerRemove);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public MidiDeviceInfo[] getDevices() {
        try {
            return this.mService.getDevices();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void sendOpenDeviceResponse(final MidiDevice midiDevice, final OnDeviceOpenedListener onDeviceOpenedListener, Handler handler) {
        if (handler != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onDeviceOpenedListener.onDeviceOpened(midiDevice);
                }
            });
        } else {
            onDeviceOpenedListener.onDeviceOpened(midiDevice);
        }
    }

    public void openDevice(final MidiDeviceInfo midiDeviceInfo, final OnDeviceOpenedListener onDeviceOpenedListener, final Handler handler) {
        try {
            this.mService.openDevice(this.mToken, midiDeviceInfo, new IMidiDeviceOpenCallback.Stub() {
                @Override
                public void onDeviceOpened(IMidiDeviceServer iMidiDeviceServer, IBinder iBinder) {
                    MidiDevice midiDevice;
                    if (iMidiDeviceServer != null) {
                        midiDevice = new MidiDevice(midiDeviceInfo, iMidiDeviceServer, MidiManager.this.mService, MidiManager.this.mToken, iBinder);
                    } else {
                        midiDevice = null;
                    }
                    MidiManager.this.sendOpenDeviceResponse(midiDevice, onDeviceOpenedListener, handler);
                }
            });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void openBluetoothDevice(BluetoothDevice bluetoothDevice, final OnDeviceOpenedListener onDeviceOpenedListener, final Handler handler) {
        try {
            this.mService.openBluetoothDevice(this.mToken, bluetoothDevice, new IMidiDeviceOpenCallback.Stub() {
                @Override
                public void onDeviceOpened(IMidiDeviceServer iMidiDeviceServer, IBinder iBinder) {
                    MidiDevice midiDevice;
                    if (iMidiDeviceServer != null) {
                        try {
                            midiDevice = new MidiDevice(iMidiDeviceServer.getDeviceInfo(), iMidiDeviceServer, MidiManager.this.mService, MidiManager.this.mToken, iBinder);
                        } catch (RemoteException e) {
                            Log.e(MidiManager.TAG, "remote exception in getDeviceInfo()");
                            midiDevice = null;
                        }
                    } else {
                        midiDevice = null;
                    }
                    MidiManager.this.sendOpenDeviceResponse(midiDevice, onDeviceOpenedListener, handler);
                }
            });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public MidiDeviceServer createDeviceServer(MidiReceiver[] midiReceiverArr, int i, String[] strArr, String[] strArr2, Bundle bundle, int i2, MidiDeviceServer.Callback callback) {
        try {
            MidiDeviceServer midiDeviceServer = new MidiDeviceServer(this.mService, midiReceiverArr, i, callback);
            if (this.mService.registerDeviceServer(midiDeviceServer.getBinderInterface(), midiReceiverArr.length, i, strArr, strArr2, bundle, i2) == null) {
                Log.e(TAG, "registerVirtualDevice failed");
                return null;
            }
            return midiDeviceServer;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
