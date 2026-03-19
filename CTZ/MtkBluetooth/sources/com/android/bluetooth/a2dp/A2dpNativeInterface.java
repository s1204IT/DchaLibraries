package com.android.bluetooth.a2dp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.android.bluetooth.Utils;
import com.android.internal.annotations.GuardedBy;

public class A2dpNativeInterface {
    private static final boolean DBG = true;
    private static final Object INSTANCE_LOCK = new Object();
    private static final String TAG = "A2dpNativeInterface";

    @GuardedBy("INSTANCE_LOCK")
    private static A2dpNativeInterface sInstance;
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    private static native void classInitNative();

    private native void cleanupNative();

    private native boolean connectA2dpNative(byte[] bArr);

    private native boolean disconnectA2dpNative(byte[] bArr);

    private native void initNative(int i, BluetoothCodecConfig[] bluetoothCodecConfigArr);

    private native boolean setActiveDeviceNative(byte[] bArr);

    private native boolean setCodecConfigPreferenceNative(byte[] bArr, BluetoothCodecConfig[] bluetoothCodecConfigArr);

    static {
        classInitNative();
    }

    @VisibleForTesting
    private A2dpNativeInterface() {
        if (this.mAdapter == null) {
            Log.wtfStack(TAG, "No Bluetooth Adapter Available");
        }
    }

    public static A2dpNativeInterface getInstance() {
        A2dpNativeInterface a2dpNativeInterface;
        synchronized (INSTANCE_LOCK) {
            if (sInstance == null) {
                sInstance = new A2dpNativeInterface();
            }
            a2dpNativeInterface = sInstance;
        }
        return a2dpNativeInterface;
    }

    public void init(int i, BluetoothCodecConfig[] bluetoothCodecConfigArr) {
        initNative(i, bluetoothCodecConfigArr);
    }

    public void cleanup() {
        cleanupNative();
    }

    public boolean connectA2dp(BluetoothDevice bluetoothDevice) {
        return connectA2dpNative(getByteAddress(bluetoothDevice));
    }

    public boolean disconnectA2dp(BluetoothDevice bluetoothDevice) {
        return disconnectA2dpNative(getByteAddress(bluetoothDevice));
    }

    public boolean setActiveDevice(BluetoothDevice bluetoothDevice) {
        return setActiveDeviceNative(getByteAddress(bluetoothDevice));
    }

    public boolean setCodecConfigPreference(BluetoothDevice bluetoothDevice, BluetoothCodecConfig[] bluetoothCodecConfigArr) {
        return setCodecConfigPreferenceNative(getByteAddress(bluetoothDevice), bluetoothCodecConfigArr);
    }

    private BluetoothDevice getDevice(byte[] bArr) {
        return this.mAdapter.getRemoteDevice(bArr);
    }

    private byte[] getByteAddress(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            return Utils.getBytesFromAddress("00:00:00:00:00:00");
        }
        return Utils.getBytesFromAddress(bluetoothDevice.getAddress());
    }

    private void sendMessageToService(A2dpStackEvent a2dpStackEvent) {
        A2dpService a2dpService = A2dpService.getA2dpService();
        if (a2dpService != null) {
            a2dpService.messageFromNative(a2dpStackEvent);
            return;
        }
        Log.w(TAG, "Event ignored, service not available: " + a2dpStackEvent);
    }

    private void onConnectionStateChanged(byte[] bArr, int i) {
        A2dpStackEvent a2dpStackEvent = new A2dpStackEvent(1);
        a2dpStackEvent.device = getDevice(bArr);
        a2dpStackEvent.valueInt = i;
        Log.d(TAG, "onConnectionStateChanged: " + a2dpStackEvent);
        sendMessageToService(a2dpStackEvent);
    }

    private void onAudioStateChanged(byte[] bArr, int i) {
        A2dpStackEvent a2dpStackEvent = new A2dpStackEvent(2);
        a2dpStackEvent.device = getDevice(bArr);
        a2dpStackEvent.valueInt = i;
        Log.d(TAG, "onAudioStateChanged: " + a2dpStackEvent);
        sendMessageToService(a2dpStackEvent);
    }

    private void onCodecConfigChanged(byte[] bArr, BluetoothCodecConfig bluetoothCodecConfig, BluetoothCodecConfig[] bluetoothCodecConfigArr, BluetoothCodecConfig[] bluetoothCodecConfigArr2) {
        A2dpStackEvent a2dpStackEvent = new A2dpStackEvent(3);
        a2dpStackEvent.device = getDevice(bArr);
        a2dpStackEvent.codecStatus = new BluetoothCodecStatus(bluetoothCodecConfig, bluetoothCodecConfigArr, bluetoothCodecConfigArr2);
        Log.d(TAG, "onCodecConfigChanged: " + a2dpStackEvent);
        sendMessageToService(a2dpStackEvent);
    }
}
