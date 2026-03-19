package com.android.storagemanager.automatic;

import android.content.Context;
import android.os.BatteryManager;

public class JobPreconditions {
    public static boolean isCharging(Context context) {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService("batterymanager");
        if (batteryManager != null) {
            return batteryManager.isCharging();
        }
        return false;
    }
}
