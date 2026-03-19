package com.mediatek.camera.portability;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.util.Log;

public class WifiDisplayStatusEx {
    public static boolean isWfdEnabled(Context context) {
        int activeDisplayState = ((DisplayManager) context.getSystemService("display")).getWifiDisplayStatus().getActiveDisplayState();
        boolean z = activeDisplayState == 2;
        Log.d("WifiDisplayStatusEx", "[isWfdEnabled()] activeDisplayState= " + activeDisplayState + ", return " + z);
        return z;
    }
}
