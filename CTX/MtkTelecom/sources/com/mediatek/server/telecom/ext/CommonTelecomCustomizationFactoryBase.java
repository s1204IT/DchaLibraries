package com.mediatek.server.telecom.ext;

import android.telecom.Log;

public class CommonTelecomCustomizationFactoryBase {
    public IGttEventExt makeGttEventExt() {
        Log.d("CommonTelecomCustomizationFactoryBase", "return DefaultGttEventExt", new Object[0]);
        return new DefaultGttEventExt();
    }

    public IRttUtilExt makeRttUtilExt() {
        return new DefaultRttUtilExt();
    }

    public IGttUtilExt makeGttUtilExt() {
        Log.d("CommonTelecomCustomizationFactoryBase", "return DefaultGttUtilExt", new Object[0]);
        return new DefaultGttUtilExt();
    }

    public IRttEventExt makeRttEventExt() {
        Log.d("CommonTelecomCustomizationFactoryBase", "DefaultRttEventExt made", new Object[0]);
        return new DefaultRttEventExt();
    }
}
