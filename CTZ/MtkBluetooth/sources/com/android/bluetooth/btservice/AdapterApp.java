package com.android.bluetooth.btservice;

import android.app.Application;
import android.os.SystemProperties;
import android.util.Log;

public class AdapterApp extends Application {
    private static final String TAG = "BluetoothAdapterApp";
    private static final boolean DBG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    private static int sRefCount = 0;

    static {
        if (DBG) {
            Log.d(TAG, "Loading JNI Library");
        }
        System.loadLibrary("mtkbluetooth_jni");
    }

    public AdapterApp() {
        if (DBG) {
            synchronized (AdapterApp.class) {
                sRefCount++;
                Log.d(TAG, "REFCOUNT: Constructed " + this + " Instance Count = " + sRefCount);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (DBG) {
            Log.d(TAG, "onCreate");
        }
        Config.init(this);
    }

    protected void finalize() {
        if (DBG) {
            synchronized (AdapterApp.class) {
                sRefCount--;
                Log.d(TAG, "REFCOUNT: Finalized: " + this + ", Instance Count = " + sRefCount);
            }
        }
    }
}
