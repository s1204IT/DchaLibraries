package com.android.bluetooth.hfp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.android.bluetooth.Utils;

public class HeadsetNativeInterface {
    private static final Object INSTANCE_LOCK;
    private static final String TAG = "HeadsetNativeInterface";
    private static HeadsetNativeInterface sInterface;
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    private native boolean atResponseCodeNative(int i, int i2, byte[] bArr);

    private native boolean atResponseStringNative(String str, byte[] bArr);

    private native boolean cindResponseNative(int i, int i2, int i3, int i4, int i5, int i6, int i7, byte[] bArr);

    private static native void classInitNative();

    private native boolean clccResponseNative(int i, int i2, int i3, int i4, boolean z, String str, int i5, byte[] bArr);

    private native void cleanupNative();

    private native boolean connectAudioNative(byte[] bArr);

    private native boolean connectHfpNative(byte[] bArr);

    private native boolean copsResponseNative(String str, byte[] bArr);

    private native boolean disconnectAudioNative(byte[] bArr);

    private native boolean disconnectHfpNative(byte[] bArr);

    private native void initializeNative(int i, boolean z);

    private native boolean notifyDeviceStatusNative(int i, int i2, int i3, int i4, byte[] bArr);

    private native boolean phoneStateChangeNative(int i, int i2, int i3, String str, int i4, byte[] bArr);

    private native boolean sendBsirNative(boolean z, byte[] bArr);

    private native boolean setActiveDeviceNative(byte[] bArr);

    private native boolean setScoAllowedNative(boolean z);

    private native boolean setVolumeNative(int i, int i2, byte[] bArr);

    private native boolean startVoiceRecognitionNative(byte[] bArr);

    private native boolean stopVoiceRecognitionNative(byte[] bArr);

    static {
        classInitNative();
        INSTANCE_LOCK = new Object();
    }

    private HeadsetNativeInterface() {
    }

    public static HeadsetNativeInterface getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (sInterface == null) {
                sInterface = new HeadsetNativeInterface();
            }
        }
        return sInterface;
    }

    private void sendMessageToService(HeadsetStackEvent headsetStackEvent) {
        HeadsetService headsetService = HeadsetService.getHeadsetService();
        if (headsetService != null) {
            headsetService.messageFromNative(headsetStackEvent);
            return;
        }
        Log.w(TAG, "FATAL: Stack sent event while service is not available: " + headsetStackEvent);
    }

    private BluetoothDevice getDevice(byte[] bArr) {
        return this.mAdapter.getRemoteDevice(Utils.getAddressStringFromByte(bArr));
    }

    void onConnectionStateChanged(int i, byte[] bArr) {
        sendMessageToService(new HeadsetStackEvent(1, i, getDevice(bArr)));
    }

    private void onAudioStateChanged(int i, byte[] bArr) {
        sendMessageToService(new HeadsetStackEvent(2, i, getDevice(bArr)));
    }

    private void onVrStateChanged(int i, byte[] bArr) {
        sendMessageToService(new HeadsetStackEvent(3, i, getDevice(bArr)));
    }

    private void onAnswerCall(byte[] bArr) {
        sendMessageToService(new HeadsetStackEvent(4, getDevice(bArr)));
    }

    private void onHangupCall(byte[] bArr) {
        sendMessageToService(new HeadsetStackEvent(5, getDevice(bArr)));
    }

    private void onVolumeChanged(int i, int i2, byte[] bArr) {
        sendMessageToService(new HeadsetStackEvent(6, i, i2, getDevice(bArr)));
    }

    private void onDialCall(String str, byte[] bArr) {
        sendMessageToService(new HeadsetStackEvent(7, str, getDevice(bArr)));
    }

    private void onSendDtmf(int i, byte[] bArr) {
        sendMessageToService(new HeadsetStackEvent(8, i, getDevice(bArr)));
    }

    private void onNoiceReductionEnable(boolean z, byte[] bArr) {
        sendMessageToService(new HeadsetStackEvent(9, z ? 1 : 0, getDevice(bArr)));
    }

    private void onWBS(int i, byte[] bArr) {
        sendMessageToService(new HeadsetStackEvent(17, i, getDevice(bArr)));
    }

    private void onAtChld(int i, byte[] bArr) {
        sendMessageToService(new HeadsetStackEvent(10, i, getDevice(bArr)));
    }

    private void onAtCnum(byte[] bArr) {
        sendMessageToService(new HeadsetStackEvent(11, getDevice(bArr)));
    }

    private void onAtCind(byte[] bArr) {
        sendMessageToService(new HeadsetStackEvent(12, getDevice(bArr)));
    }

    private void onAtCops(byte[] bArr) {
        sendMessageToService(new HeadsetStackEvent(13, getDevice(bArr)));
    }

    private void onAtClcc(byte[] bArr) {
        sendMessageToService(new HeadsetStackEvent(14, getDevice(bArr)));
    }

    private void onUnknownAt(String str, byte[] bArr) {
        sendMessageToService(new HeadsetStackEvent(15, str, getDevice(bArr)));
    }

    private void onKeyPressed(byte[] bArr) {
        sendMessageToService(new HeadsetStackEvent(16, getDevice(bArr)));
    }

    private void onATBind(String str, byte[] bArr) {
        sendMessageToService(new HeadsetStackEvent(18, str, getDevice(bArr)));
    }

    private void onATBiev(int i, int i2, byte[] bArr) {
        sendMessageToService(new HeadsetStackEvent(19, i, i2, getDevice(bArr)));
    }

    private void onAtBia(boolean z, boolean z2, boolean z3, boolean z4, byte[] bArr) {
        sendMessageToService(new HeadsetStackEvent(20, new HeadsetAgIndicatorEnableState(z, z2, z3, z4), getDevice(bArr)));
    }

    @VisibleForTesting
    public void init(int i, boolean z) {
        initializeNative(i, z);
    }

    @VisibleForTesting
    public void cleanup() {
        cleanupNative();
    }

    @VisibleForTesting
    public boolean atResponseCode(BluetoothDevice bluetoothDevice, int i, int i2) {
        return atResponseCodeNative(i, i2, Utils.getByteAddress(bluetoothDevice));
    }

    @VisibleForTesting
    public boolean atResponseString(BluetoothDevice bluetoothDevice, String str) {
        return atResponseStringNative(str, Utils.getByteAddress(bluetoothDevice));
    }

    @VisibleForTesting
    public boolean connectHfp(BluetoothDevice bluetoothDevice) {
        return connectHfpNative(Utils.getByteAddress(bluetoothDevice));
    }

    @VisibleForTesting
    public boolean disconnectHfp(BluetoothDevice bluetoothDevice) {
        return disconnectHfpNative(Utils.getByteAddress(bluetoothDevice));
    }

    @VisibleForTesting
    public boolean connectAudio(BluetoothDevice bluetoothDevice) {
        return connectAudioNative(Utils.getByteAddress(bluetoothDevice));
    }

    @VisibleForTesting
    public boolean disconnectAudio(BluetoothDevice bluetoothDevice) {
        return disconnectAudioNative(Utils.getByteAddress(bluetoothDevice));
    }

    @VisibleForTesting
    public boolean startVoiceRecognition(BluetoothDevice bluetoothDevice) {
        return startVoiceRecognitionNative(Utils.getByteAddress(bluetoothDevice));
    }

    @VisibleForTesting
    public boolean stopVoiceRecognition(BluetoothDevice bluetoothDevice) {
        return stopVoiceRecognitionNative(Utils.getByteAddress(bluetoothDevice));
    }

    @VisibleForTesting
    public boolean setVolume(BluetoothDevice bluetoothDevice, int i, int i2) {
        return setVolumeNative(i, i2, Utils.getByteAddress(bluetoothDevice));
    }

    @VisibleForTesting
    public boolean cindResponse(BluetoothDevice bluetoothDevice, int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        return cindResponseNative(i, i2, i3, i4, i5, i6, i7, Utils.getByteAddress(bluetoothDevice));
    }

    @VisibleForTesting
    public boolean notifyDeviceStatus(BluetoothDevice bluetoothDevice, HeadsetDeviceState headsetDeviceState) {
        return notifyDeviceStatusNative(headsetDeviceState.mService, headsetDeviceState.mRoam, headsetDeviceState.mSignal, headsetDeviceState.mBatteryCharge, Utils.getByteAddress(bluetoothDevice));
    }

    @VisibleForTesting
    public boolean clccResponse(BluetoothDevice bluetoothDevice, int i, int i2, int i3, int i4, boolean z, String str, int i5) {
        return clccResponseNative(i, i2, i3, i4, z, str, i5, Utils.getByteAddress(bluetoothDevice));
    }

    @VisibleForTesting
    public boolean copsResponse(BluetoothDevice bluetoothDevice, String str) {
        return copsResponseNative(str, Utils.getByteAddress(bluetoothDevice));
    }

    @VisibleForTesting
    public boolean phoneStateChange(BluetoothDevice bluetoothDevice, HeadsetCallState headsetCallState) {
        return phoneStateChangeNative(headsetCallState.mNumActive, headsetCallState.mNumHeld, headsetCallState.mCallState, headsetCallState.mNumber, headsetCallState.mType, Utils.getByteAddress(bluetoothDevice));
    }

    @VisibleForTesting
    public boolean setScoAllowed(boolean z) {
        return setScoAllowedNative(z);
    }

    @VisibleForTesting
    public boolean sendBsir(BluetoothDevice bluetoothDevice, boolean z) {
        return sendBsirNative(z, Utils.getByteAddress(bluetoothDevice));
    }

    @VisibleForTesting
    public boolean setActiveDevice(BluetoothDevice bluetoothDevice) {
        return setActiveDeviceNative(Utils.getByteAddress(bluetoothDevice));
    }
}
