package com.marvell.powergadget.thermal;

import android.os.UEventObserver;
import android.util.Log;

public class ThermalUEventObserver extends UEventObserver {
    public static String TAG = "ThermalService";
    private ThermalService service;

    public ThermalUEventObserver(ThermalService thermalService) {
        this.service = null;
        this.service = thermalService;
    }

    public void onUEvent(UEventObserver.UEvent event) {
        Log.d(TAG, "ThermalUEventObserver: Received thermal uevent: " + event.toString());
        String type = event.get("TYPE");
        String temp = event.get("TEMP");
        if (type != null && temp != null) {
            update(type, Integer.parseInt(temp));
        } else {
            Log.w(TAG, "Uevent format is not correct.");
        }
    }

    public void update(String name, int temp) {
        Log.d(TAG, "ThermalUEventObserver: thermal : " + name + ", " + temp);
        this.service.thermalStateChanged(name, temp);
    }
}
