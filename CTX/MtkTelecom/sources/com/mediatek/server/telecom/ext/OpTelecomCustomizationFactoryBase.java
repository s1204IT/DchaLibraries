package com.mediatek.server.telecom.ext;

import android.telecom.Log;

public class OpTelecomCustomizationFactoryBase {
    public ICallMgrExt makeCallMgrExt() {
        Log.d("OpTelecomCustomizationFactoryBase", "return DefaultCallMgrExt", new Object[0]);
        return new DefaultCallMgrExt();
    }

    public IDigitsUtilExt makeDigitsUtilExt() {
        return new DefaultDigitsUtilExt();
    }
}
