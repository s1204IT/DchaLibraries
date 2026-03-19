package com.android.server.telecom.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telecom.Log;
import android.telecom.Logging.Session;
import com.android.internal.os.SomeArgs;
import com.android.server.telecom.CallState;

public class BluetoothStateReceiver extends BroadcastReceiver {
    private final BluetoothDeviceManager mBluetoothDeviceManager;
    private final BluetoothRouteManager mBluetoothRouteManager;
    private boolean mIsInCall = false;
    private static final String LOG_TAG = BluetoothStateReceiver.class.getSimpleName();
    public static final IntentFilter INTENT_FILTER = new IntentFilter();

    static {
        INTENT_FILTER.addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED");
        INTENT_FILTER.addAction("android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED");
        INTENT_FILTER.addAction("android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.startSession("BSR.oR");
        try {
            String action = intent.getAction();
            byte b = -1;
            int iHashCode = action.hashCode();
            if (iHashCode != -1435586571) {
                if (iHashCode != 17117692) {
                    if (iHashCode == 545516589 && action.equals("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")) {
                        b = 1;
                    }
                } else if (action.equals("android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED")) {
                    b = 2;
                }
            } else if (action.equals("android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED")) {
                b = 0;
            }
            switch (b) {
                case CallState.NEW:
                    handleAudioStateChanged(intent);
                    break;
                case 1:
                    handleConnectionStateChanged(intent);
                    break;
                case CallState.SELECT_PHONE_ACCOUNT:
                    handleActiveDeviceChanged(intent);
                    break;
            }
        } finally {
            Log.endSession();
        }
    }

    private void handleAudioStateChanged(Intent intent) {
        if (!this.mIsInCall) {
            Log.i(LOG_TAG, "Ignoring BT audio state change since we're not in a call", new Object[0]);
            return;
        }
        int intExtra = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 10);
        BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        if (bluetoothDevice == null) {
            Log.w(LOG_TAG, "Got null device from broadcast. Ignoring.", new Object[0]);
            return;
        }
        Log.i(LOG_TAG, "Device %s transitioned to audio state %d", new Object[]{bluetoothDevice.getAddress(), Integer.valueOf(intExtra)});
        Session sessionCreateSubsession = Log.createSubsession();
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = sessionCreateSubsession;
        someArgsObtain.arg2 = bluetoothDevice.getAddress();
        if (intExtra == 10) {
            this.mBluetoothRouteManager.sendMessage(201, someArgsObtain);
        } else if (intExtra == 12) {
            this.mBluetoothRouteManager.sendMessage(200, someArgsObtain);
        }
    }

    private void handleConnectionStateChanged(Intent intent) {
        int intExtra = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0);
        BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        if (bluetoothDevice == null) {
            Log.w(LOG_TAG, "Got null device from broadcast. Ignoring.", new Object[0]);
            return;
        }
        Log.i(LOG_TAG, "Device %s changed state to %d", new Object[]{bluetoothDevice.getAddress(), Integer.valueOf(intExtra)});
        if (intExtra == 2) {
            this.mBluetoothDeviceManager.onDeviceConnected(bluetoothDevice);
        } else if (intExtra == 0 || intExtra == 3) {
            this.mBluetoothDeviceManager.onDeviceDisconnected(bluetoothDevice);
        }
    }

    private void handleActiveDeviceChanged(Intent intent) {
        BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        Log.i(LOG_TAG, "Device %s is now the preferred HFP device", new Object[]{bluetoothDevice});
        this.mBluetoothRouteManager.onActiveDeviceChanged(bluetoothDevice);
    }

    public BluetoothStateReceiver(BluetoothDeviceManager bluetoothDeviceManager, BluetoothRouteManager bluetoothRouteManager) {
        this.mBluetoothDeviceManager = bluetoothDeviceManager;
        this.mBluetoothRouteManager = bluetoothRouteManager;
    }

    public void setIsInCall(boolean z) {
        this.mIsInCall = z;
    }
}
