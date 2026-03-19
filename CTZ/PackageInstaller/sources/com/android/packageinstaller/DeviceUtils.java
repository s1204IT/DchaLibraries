package com.android.packageinstaller;

import android.content.Context;

public class DeviceUtils {
    public static boolean isTelevision(Context context) {
        return (context.getResources().getConfiguration().uiMode & 15) == 4;
    }

    public static boolean isWear(Context context) {
        return context.getPackageManager().hasSystemFeature("android.hardware.type.watch");
    }

    public static boolean isAuto(Context context) {
        return context.getPackageManager().hasSystemFeature("android.hardware.type.automotive");
    }
}
