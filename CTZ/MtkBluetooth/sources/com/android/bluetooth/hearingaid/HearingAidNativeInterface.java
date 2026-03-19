package com.android.bluetooth.hearingaid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.android.bluetooth.Utils;
import com.android.internal.annotations.GuardedBy;

public class HearingAidNativeInterface {
    private static final boolean DBG = true;
    private static final Object INSTANCE_LOCK = new Object();
    private static final String TAG = "HearingAidNativeInterface";

    @GuardedBy("INSTANCE_LOCK")
    private static HearingAidNativeInterface sInstance;
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    private static native void classInitNative();

    private native void cleanupNative();

    private native boolean connectHearingAidNative(byte[] bArr);

    private native boolean disconnectHearingAidNative(byte[] bArr);

    private native void initNative();

    private native void setVolumeNative(int i);

    static {
        classInitNative();
    }

    private HearingAidNativeInterface() {
        if (this.mAdapter == null) {
            Log.wtfStack(TAG, "No Bluetooth Adapter Available");
        }
    }

    public static HearingAidNativeInterface getInstance() {
        HearingAidNativeInterface hearingAidNativeInterface;
        synchronized (INSTANCE_LOCK) {
            if (sInstance == null) {
                sInstance = new HearingAidNativeInterface();
            }
            hearingAidNativeInterface = sInstance;
        }
        return hearingAidNativeInterface;
    }

    @VisibleForTesting(otherwise = 3)
    public void init() {
        initNative();
    }

    @VisibleForTesting(otherwise = 3)
    public void cleanup() {
        cleanupNative();
    }

    @VisibleForTesting(otherwise = 3)
    public boolean connectHearingAid(BluetoothDevice bluetoothDevice) {
        return connectHearingAidNative(getByteAddress(bluetoothDevice));
    }

    @VisibleForTesting(otherwise = 3)
    public boolean disconnectHearingAid(BluetoothDevice bluetoothDevice) {
        return disconnectHearingAidNative(getByteAddress(bluetoothDevice));
    }

    @VisibleForTesting(otherwise = 3)
    public void setVolume(int i) {
        setVolumeNative(i);
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

    private void sendMessageToService(HearingAidStackEvent hearingAidStackEvent) {
        HearingAidService hearingAidService = HearingAidService.getHearingAidService();
        if (hearingAidService != null) {
            hearingAidService.messageFromNative(hearingAidStackEvent);
            return;
        }
        Log.e(TAG, "Event ignored, service not available: " + hearingAidStackEvent);
    }

    private void onConnectionStateChanged(int i, byte[] bArr) {
        HearingAidStackEvent hearingAidStackEvent = new HearingAidStackEvent(1);
        hearingAidStackEvent.device = getDevice(bArr);
        hearingAidStackEvent.valueInt1 = i;
        Log.d(TAG, "onConnectionStateChanged: " + hearingAidStackEvent);
        sendMessageToService(hearingAidStackEvent);
    }

    private void onDeviceAvailable(byte b, long j, byte[] bArr) {
        HearingAidStackEvent hearingAidStackEvent = new HearingAidStackEvent(2);
        hearingAidStackEvent.device = getDevice(bArr);
        hearingAidStackEvent.valueInt1 = b;
        hearingAidStackEvent.valueLong2 = j;
        Log.d(TAG, "onDeviceAvailable: " + hearingAidStackEvent);
        sendMessageToService(hearingAidStackEvent);
    }
}
