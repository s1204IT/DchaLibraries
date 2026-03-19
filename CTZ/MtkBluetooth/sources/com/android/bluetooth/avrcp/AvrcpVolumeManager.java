package com.android.bluetooth.avrcp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.SystemProperties;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class AvrcpVolumeManager extends AudioDeviceCallback {
    public static final int AVRCP_MAX_VOL = 127;
    public static final int STREAM_MUSIC = 3;
    public static final String TAG = "NewAvrcpVolumeManager";
    public static final String VOLUME_BLACKLIST = "absolute_volume_blacklist";
    public static final String VOLUME_MAP = "bluetooth_volume_map";
    AudioManager mAudioManager;
    Context mContext;
    AvrcpNativeInterface mNativeInterface;
    public static final boolean DEBUG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    public static int sDeviceMaxVolume = 0;
    HashMap<BluetoothDevice, Boolean> mDeviceMap = new HashMap<>();
    HashMap<BluetoothDevice, Integer> mVolumeMap = new HashMap<>();
    BluetoothDevice mCurrentDevice = null;
    boolean mAbsoluteVolumeSupported = false;

    static int avrcpToSystemVolume(int i) {
        return (int) Math.round((((double) i) * ((double) sDeviceMaxVolume)) / 127.0d);
    }

    static int systemToAvrcpVolume(int i) {
        int iFloor = (int) Math.floor((((double) i) * 127.0d) / ((double) sDeviceMaxVolume));
        if (iFloor > 127) {
            return 127;
        }
        return iFloor;
    }

    private SharedPreferences getVolumeMap() {
        return this.mContext.getSharedPreferences(VOLUME_MAP, 0);
    }

    private void switchVolumeDevice(BluetoothDevice bluetoothDevice) {
        synchronized (this.mDeviceMap) {
            if (!this.mDeviceMap.containsKey(bluetoothDevice)) {
                Log.w(TAG, "switchVolumeDevice: Device isn't connected: " + bluetoothDevice);
                return;
            }
            d("switchVolumeDevice: Set Absolute volume support to " + this.mDeviceMap.get(bluetoothDevice));
            this.mAudioManager.avrcpSupportsAbsoluteVolume(bluetoothDevice.getAddress(), this.mDeviceMap.get(bluetoothDevice).booleanValue());
            int streamVolume = this.mAudioManager.getStreamVolume(3);
            int volume = getVolume(bluetoothDevice, streamVolume);
            d("switchVolumeDevice: currVolume=" + streamVolume + " savedVolume=" + volume);
            if (this.mDeviceMap.get(bluetoothDevice).booleanValue()) {
                int iSystemToAvrcpVolume = systemToAvrcpVolume(volume);
                Log.i(TAG, "switchVolumeDevice: Updating device volume: avrcpVolume=" + iSystemToAvrcpVolume);
                this.mNativeInterface.sendVolumeChanged(iSystemToAvrcpVolume);
            }
        }
    }

    AvrcpVolumeManager(Context context, AudioManager audioManager, AvrcpNativeInterface avrcpNativeInterface) {
        this.mContext = context;
        this.mAudioManager = audioManager;
        this.mNativeInterface = avrcpNativeInterface;
        sDeviceMaxVolume = this.mAudioManager.getStreamMaxVolume(3);
        this.mAudioManager.registerAudioDeviceCallback(this, null);
        Map<String, ?> all = getVolumeMap().getAll();
        SharedPreferences.Editor editorEdit = getVolumeMap().edit();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(key);
            if ((value instanceof Integer) && remoteDevice.getBondState() == 12) {
                this.mVolumeMap.put(remoteDevice, (Integer) value);
            } else {
                d("Removing " + key + " from the volume map");
                editorEdit.remove(key);
            }
        }
        editorEdit.apply();
    }

    void storeVolumeForDevice(BluetoothDevice bluetoothDevice) {
        SharedPreferences.Editor editorEdit = getVolumeMap().edit();
        int streamVolume = this.mAudioManager.getStreamVolume(3);
        Log.i(TAG, "storeVolume: Storing stream volume level for device " + bluetoothDevice + " : " + streamVolume);
        this.mVolumeMap.put(bluetoothDevice, Integer.valueOf(streamVolume));
        editorEdit.putInt(bluetoothDevice.getAddress(), streamVolume);
        editorEdit.apply();
    }

    int getVolume(BluetoothDevice bluetoothDevice, int i) {
        if (!this.mVolumeMap.containsKey(bluetoothDevice)) {
            Log.w(TAG, "getVolume: Couldn't find volume preference for device: " + bluetoothDevice);
            return i;
        }
        d("getVolume: Returning volume " + this.mVolumeMap.get(bluetoothDevice));
        return this.mVolumeMap.get(bluetoothDevice).intValue();
    }

    @Override
    public synchronized void onAudioDevicesAdded(AudioDeviceInfo[] audioDeviceInfoArr) {
        if (this.mCurrentDevice == null) {
            d("onAudioDevicesAdded: Not expecting device changed");
            return;
        }
        d("onAudioDevicesAdded: size: " + audioDeviceInfoArr.length);
        boolean z = false;
        int i = 0;
        while (true) {
            if (i >= audioDeviceInfoArr.length) {
                break;
            }
            d("onAudioDevicesAdded: address=" + audioDeviceInfoArr[i].getAddress());
            if (audioDeviceInfoArr[i].getType() != 8 || !Objects.equals(audioDeviceInfoArr[i].getAddress(), this.mCurrentDevice.getAddress())) {
                i++;
            } else {
                z = true;
                break;
            }
        }
        if (!z) {
            d("Didn't find deferred device in list: device=" + this.mCurrentDevice);
            return;
        }
        if (!this.mDeviceMap.containsKey(this.mCurrentDevice)) {
            Log.w(TAG, "volumeDeviceSwitched: Device isn't connected: " + this.mCurrentDevice);
            return;
        }
        switchVolumeDevice(this.mCurrentDevice);
    }

    synchronized void deviceConnected(BluetoothDevice bluetoothDevice, boolean z) {
        d("deviceConnected: device=" + bluetoothDevice + " absoluteVolume=" + z);
        this.mDeviceMap.put(bluetoothDevice, Boolean.valueOf(z));
        if (bluetoothDevice.equals(this.mCurrentDevice)) {
            switchVolumeDevice(bluetoothDevice);
        }
    }

    synchronized void volumeDeviceSwitched(BluetoothDevice bluetoothDevice) {
        d("volumeDeviceSwitched: mCurrentDevice=" + this.mCurrentDevice + " device=" + bluetoothDevice);
        if (Objects.equals(bluetoothDevice, this.mCurrentDevice)) {
            return;
        }
        this.mCurrentDevice = bluetoothDevice;
    }

    void deviceDisconnected(BluetoothDevice bluetoothDevice) {
        d("deviceDisconnected: device=" + bluetoothDevice);
        this.mDeviceMap.remove(bluetoothDevice);
    }

    public void dump(StringBuilder sb) {
        sb.append("AvrcpVolumeManager:\n");
        sb.append("  mCurrentDevice: " + this.mCurrentDevice + "\n");
        sb.append("  Current System Volume: " + this.mAudioManager.getStreamVolume(3) + "\n");
        sb.append("  Device Volume Memory Map:\n");
        sb.append(String.format("    %-17s : %-14s : %3s : %s\n", "Device Address", "Device Name", "Vol", "AbsVol"));
        for (Map.Entry<String, ?> entry : getVolumeMap().getAll().entrySet()) {
            Object value = entry.getValue();
            BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(entry.getKey());
            String name = remoteDevice.getName();
            if (name == null) {
                name = "";
            } else if (name.length() > 14) {
                name = name.substring(0, 11).concat("...");
            }
            String string = "NotConnected";
            if (this.mDeviceMap.containsKey(remoteDevice)) {
                string = this.mDeviceMap.get(remoteDevice).toString();
            }
            if (value instanceof Integer) {
                sb.append(String.format("    %-17s : %-14s : %3d : %s\n", remoteDevice.getAddress(), name, value, string));
            }
        }
    }

    static void d(String str) {
        if (DEBUG) {
            Log.d(TAG, str);
        }
    }
}
