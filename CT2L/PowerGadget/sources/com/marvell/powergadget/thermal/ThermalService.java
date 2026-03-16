package com.marvell.powergadget.thermal;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import java.io.File;
import java.util.HashMap;

public class ThermalService extends Service {
    public static String TAG = "ThermalService";
    private HashMap<String, ThermalListener> mListeners;
    private ThermalUEventObserver mUEventObserver = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Run Thermal Service!");
        thermalStateInit(this);
        this.mUEventObserver = new ThermalUEventObserver(this);
        this.mUEventObserver.startObserving("SUBSYSTEM=thermal");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return 1;
    }

    @Override
    public void onDestroy() {
        this.mUEventObserver.stopObserving();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void thermalStateInit(Context context) {
        ThermalListener listener;
        this.mListeners = new HashMap<>();
        int i = 0;
        while (true) {
            int i2 = i + 1;
            String thermalPath = "/sys/class/thermal/thermal_zone" + Integer.toString(i);
            if (!new File(thermalPath).exists()) {
                Log.i(TAG, "Total " + (i2 - 1) + " thermal are inited");
                return;
            }
            String thermalName = Utils.readInfo(thermalPath + "/type");
            if (thermalName == null) {
                i = i2;
            } else {
                if (thermalName.indexOf("cpu") != -1) {
                    listener = new ThermalListener(context, thermalPath, thermalName);
                } else if (thermalName.indexOf("vpu") != -1) {
                    listener = new ThermalListener(context, thermalPath, thermalName);
                } else if (thermalName.indexOf("gc") != -1) {
                    listener = new ThermalListener(context, thermalPath, thermalName);
                } else {
                    Log.w(TAG, "no such thermal Type : " + thermalName);
                    i = i2;
                }
                registerThermalListener(thermalName, listener);
                i = i2;
            }
        }
    }

    void thermalStateChanged(String name, int temp) {
        ThermalListener listener = this.mListeners.get(name);
        if (listener == null) {
            Log.w(TAG, "Error type of thermals: " + name);
        } else {
            listener.tempChanged(temp);
        }
    }

    void registerThermalListener(String name, ThermalListener listener) {
        this.mListeners.put(name, listener);
        Log.i(TAG, "register thermal listener: " + name);
    }
}
