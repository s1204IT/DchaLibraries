package com.android.bluetooth.hfp;

import android.bluetooth.BluetoothDevice;
import android.os.Looper;
import android.util.Log;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AdapterService;

public class HeadsetObjectsFactory {
    private static HeadsetObjectsFactory sInstance;
    private static final String TAG = HeadsetObjectsFactory.class.getSimpleName();
    private static final Object INSTANCE_LOCK = new Object();

    private HeadsetObjectsFactory() {
    }

    public static HeadsetObjectsFactory getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (sInstance == null) {
                sInstance = new HeadsetObjectsFactory();
            }
        }
        return sInstance;
    }

    private static void setInstanceForTesting(HeadsetObjectsFactory headsetObjectsFactory) {
        Utils.enforceInstrumentationTestMode();
        synchronized (INSTANCE_LOCK) {
            Log.d(TAG, "setInstanceForTesting(), set to " + headsetObjectsFactory);
            sInstance = headsetObjectsFactory;
        }
    }

    public HeadsetStateMachine makeStateMachine(BluetoothDevice bluetoothDevice, Looper looper, HeadsetService headsetService, AdapterService adapterService, HeadsetNativeInterface headsetNativeInterface, HeadsetSystemInterface headsetSystemInterface) {
        return HeadsetStateMachine.make(bluetoothDevice, looper, headsetService, adapterService, headsetNativeInterface, headsetSystemInterface);
    }

    public void destroyStateMachine(HeadsetStateMachine headsetStateMachine) {
        HeadsetStateMachine.destroy(headsetStateMachine);
    }

    public HeadsetSystemInterface makeSystemInterface(HeadsetService headsetService) {
        return new HeadsetSystemInterface(headsetService);
    }

    public HeadsetNativeInterface getNativeInterface() {
        return HeadsetNativeInterface.getInstance();
    }
}
