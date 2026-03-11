package com.mediatek.settingslib;

import android.os.SystemProperties;

public class FeatureOption {
    public static final boolean MTK_Bluetooth_DUN = getValue("bt.profiles.dun.enabled");

    private static boolean getValue(String key) {
        return SystemProperties.get(key).equals("1");
    }
}
