package com.android.bluetooth.gatt;

import android.os.SystemProperties;

class GattServiceConfig {
    public static final boolean DEBUG_ADMIN = true;
    public static final String TAG_PREFIX = "BtGatt.";
    public static final boolean DBG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    public static final boolean VDBG = DBG;

    GattServiceConfig() {
    }
}
