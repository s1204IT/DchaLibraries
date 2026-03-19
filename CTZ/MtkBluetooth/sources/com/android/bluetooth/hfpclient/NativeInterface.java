package com.android.bluetooth.hfpclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

class NativeInterface {
    private static final boolean DBG = false;
    private static final String TAG = "NativeInterface";

    static native void classInitNative();

    public native void cleanupNative();

    public native boolean connectAudioNative(byte[] bArr);

    public native boolean connectNative(byte[] bArr);

    public native boolean dialMemoryNative(byte[] bArr, int i);

    public native boolean dialNative(byte[] bArr, String str);

    public native boolean disconnectAudioNative(byte[] bArr);

    public native boolean disconnectNative(byte[] bArr);

    public native boolean handleCallActionNative(byte[] bArr, int i, int i2);

    public native void initializeNative();

    public native boolean queryCurrentCallsNative(byte[] bArr);

    public native boolean queryCurrentOperatorNameNative(byte[] bArr);

    public native boolean requestLastVoiceTagNumberNative(byte[] bArr);

    public native boolean retrieveSubscriberInfoNative(byte[] bArr);

    public native boolean sendATCmdNative(byte[] bArr, int i, int i2, int i3, String str);

    public native boolean sendDtmfNative(byte[] bArr, byte b);

    public native boolean setVolumeNative(byte[] bArr, int i, int i2);

    public native boolean startVoiceRecognitionNative(byte[] bArr);

    public native boolean stopVoiceRecognitionNative(byte[] bArr);

    static {
        classInitNative();
    }

    NativeInterface() {
        initializeNative();
    }

    public void cleanup() {
        cleanupNative();
    }

    private BluetoothDevice getDevice(byte[] bArr) {
        return BluetoothAdapter.getDefaultAdapter().getRemoteDevice(bArr);
    }

    private void onConnectionStateChanged(int i, int i2, int i3, byte[] bArr) {
        StackEvent stackEvent = new StackEvent(1);
        stackEvent.valueInt = i;
        stackEvent.valueInt2 = i2;
        stackEvent.valueInt3 = i3;
        stackEvent.device = getDevice(bArr);
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        if (headsetClientService != null) {
            headsetClientService.messageFromNative(stackEvent);
            return;
        }
        Log.w(TAG, "Ignoring message because service not available: " + stackEvent);
    }

    private void onAudioStateChanged(int i, byte[] bArr) {
        StackEvent stackEvent = new StackEvent(2);
        stackEvent.valueInt = i;
        stackEvent.device = getDevice(bArr);
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        if (headsetClientService != null) {
            headsetClientService.messageFromNative(stackEvent);
            return;
        }
        Log.w(TAG, "onAudioStateChanged: Ignoring message because service not available: " + stackEvent);
    }

    private void onVrStateChanged(int i, byte[] bArr) {
        StackEvent stackEvent = new StackEvent(3);
        stackEvent.valueInt = i;
        stackEvent.device = getDevice(bArr);
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        if (headsetClientService != null) {
            headsetClientService.messageFromNative(stackEvent);
            return;
        }
        Log.w(TAG, "onVrStateChanged: Ignoring message because service not available: " + stackEvent);
    }

    private void onNetworkState(int i, byte[] bArr) {
        StackEvent stackEvent = new StackEvent(4);
        stackEvent.valueInt = i;
        stackEvent.device = getDevice(bArr);
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        if (headsetClientService != null) {
            headsetClientService.messageFromNative(stackEvent);
            return;
        }
        Log.w(TAG, "onNetworkStateChanged: Ignoring message because service not available: " + stackEvent);
    }

    private void onNetworkRoaming(int i, byte[] bArr) {
        StackEvent stackEvent = new StackEvent(5);
        stackEvent.valueInt = i;
        stackEvent.device = getDevice(bArr);
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        if (headsetClientService != null) {
            headsetClientService.messageFromNative(stackEvent);
            return;
        }
        Log.w(TAG, "onNetworkRoaming: Ignoring message because service not available: " + stackEvent);
    }

    private void onNetworkSignal(int i, byte[] bArr) {
        StackEvent stackEvent = new StackEvent(6);
        stackEvent.valueInt = i;
        stackEvent.device = getDevice(bArr);
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        if (headsetClientService != null) {
            headsetClientService.messageFromNative(stackEvent);
            return;
        }
        Log.w(TAG, "onNetworkSignal: Ignoring message because service not available: " + stackEvent);
    }

    private void onBatteryLevel(int i, byte[] bArr) {
        StackEvent stackEvent = new StackEvent(7);
        stackEvent.valueInt = i;
        stackEvent.device = getDevice(bArr);
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        if (headsetClientService != null) {
            headsetClientService.messageFromNative(stackEvent);
            return;
        }
        Log.w(TAG, "onBatteryLevel: Ignoring message because service not available: " + stackEvent);
    }

    private void onCurrentOperator(String str, byte[] bArr) {
        StackEvent stackEvent = new StackEvent(8);
        stackEvent.valueString = str;
        stackEvent.device = getDevice(bArr);
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        if (headsetClientService != null) {
            headsetClientService.messageFromNative(stackEvent);
            return;
        }
        Log.w(TAG, "onCurrentOperator: Ignoring message because service not available: " + stackEvent);
    }

    private void onCall(int i, byte[] bArr) {
        StackEvent stackEvent = new StackEvent(9);
        stackEvent.valueInt = i;
        stackEvent.device = getDevice(bArr);
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        if (headsetClientService != null) {
            headsetClientService.messageFromNative(stackEvent);
            return;
        }
        Log.w(TAG, "onCall: Ignoring message because service not available: " + stackEvent);
    }

    private void onCallSetup(int i, byte[] bArr) {
        StackEvent stackEvent = new StackEvent(10);
        stackEvent.valueInt = i;
        stackEvent.device = getDevice(bArr);
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        if (headsetClientService != null) {
            headsetClientService.messageFromNative(stackEvent);
            return;
        }
        Log.w(TAG, "onCallSetup: Ignoring message because service not available: " + stackEvent);
    }

    private void onCallHeld(int i, byte[] bArr) {
        StackEvent stackEvent = new StackEvent(11);
        stackEvent.valueInt = i;
        stackEvent.device = getDevice(bArr);
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        if (headsetClientService != null) {
            headsetClientService.messageFromNative(stackEvent);
            return;
        }
        Log.w(TAG, "onCallHeld: Ignoring message because service not available: " + stackEvent);
    }

    private void onRespAndHold(int i, byte[] bArr) {
        StackEvent stackEvent = new StackEvent(12);
        stackEvent.valueInt = i;
        stackEvent.device = getDevice(bArr);
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        if (headsetClientService != null) {
            headsetClientService.messageFromNative(stackEvent);
            return;
        }
        Log.w(TAG, "onRespAndHold: Ignoring message because service not available: " + stackEvent);
    }

    private void onClip(String str, byte[] bArr) {
        StackEvent stackEvent = new StackEvent(13);
        stackEvent.valueString = str;
        stackEvent.device = getDevice(bArr);
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        if (headsetClientService != null) {
            headsetClientService.messageFromNative(stackEvent);
            return;
        }
        Log.w(TAG, "onClip: Ignoring message because service not available: " + stackEvent);
    }

    private void onCallWaiting(String str, byte[] bArr) {
        StackEvent stackEvent = new StackEvent(14);
        stackEvent.valueString = str;
        stackEvent.device = getDevice(bArr);
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        if (headsetClientService != null) {
            headsetClientService.messageFromNative(stackEvent);
            return;
        }
        Log.w(TAG, "onCallWaiting: Ignoring message because service not available: " + stackEvent);
    }

    private void onCurrentCalls(int i, int i2, int i3, int i4, String str, byte[] bArr) {
        StackEvent stackEvent = new StackEvent(15);
        stackEvent.valueInt = i;
        stackEvent.valueInt2 = i2;
        stackEvent.valueInt3 = i3;
        stackEvent.valueInt4 = i4;
        stackEvent.valueString = str;
        stackEvent.device = getDevice(bArr);
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        if (headsetClientService != null) {
            headsetClientService.messageFromNative(stackEvent);
            return;
        }
        Log.w(TAG, "onCurrentCalls: Ignoring message because service not available: " + stackEvent);
    }

    private void onVolumeChange(int i, int i2, byte[] bArr) {
        StackEvent stackEvent = new StackEvent(16);
        stackEvent.valueInt = i;
        stackEvent.valueInt2 = i2;
        stackEvent.device = getDevice(bArr);
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        if (headsetClientService != null) {
            headsetClientService.messageFromNative(stackEvent);
            return;
        }
        Log.w(TAG, "onVolumeChange: Ignoring message because service not available: " + stackEvent);
    }

    private void onCmdResult(int i, int i2, byte[] bArr) {
        StackEvent stackEvent = new StackEvent(17);
        stackEvent.valueInt = i;
        stackEvent.valueInt2 = i2;
        stackEvent.device = getDevice(bArr);
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        if (headsetClientService != null) {
            headsetClientService.messageFromNative(stackEvent);
            return;
        }
        Log.w(TAG, "onCmdResult: Ignoring message because service not available: " + stackEvent);
    }

    private void onSubscriberInfo(String str, int i, byte[] bArr) {
        StackEvent stackEvent = new StackEvent(18);
        stackEvent.valueInt = i;
        stackEvent.valueString = str;
        stackEvent.device = getDevice(bArr);
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        if (headsetClientService != null) {
            headsetClientService.messageFromNative(stackEvent);
            return;
        }
        Log.w(TAG, "onSubscriberInfo: Ignoring message because service not available: " + stackEvent);
    }

    private void onInBandRing(int i, byte[] bArr) {
        StackEvent stackEvent = new StackEvent(19);
        stackEvent.valueInt = i;
        stackEvent.device = getDevice(bArr);
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        if (headsetClientService != null) {
            headsetClientService.messageFromNative(stackEvent);
            return;
        }
        Log.w(TAG, "onInBandRing: Ignoring message because service not available: " + stackEvent);
    }

    private void onLastVoiceTagNumber(String str, byte[] bArr) {
        Log.w(TAG, "onLastVoiceTagNumber not supported");
    }

    private void onRingIndication(byte[] bArr) {
        StackEvent stackEvent = new StackEvent(21);
        stackEvent.device = getDevice(bArr);
        HeadsetClientService headsetClientService = HeadsetClientService.getHeadsetClientService();
        if (headsetClientService != null) {
            headsetClientService.messageFromNative(stackEvent);
            return;
        }
        Log.w(TAG, "onRingIndication: Ignoring message because service not available: " + stackEvent);
    }
}
