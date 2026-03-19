package com.android.printservice.recommendation.plugin.hp;

import android.net.nsd.NsdServiceInfo;
import java.util.HashMap;

final class PrinterHashMap extends HashMap<String, NsdServiceInfo> {
    PrinterHashMap() {
    }

    public static String getKey(NsdServiceInfo nsdServiceInfo) {
        return nsdServiceInfo.getServiceName();
    }

    public NsdServiceInfo addPrinter(NsdServiceInfo nsdServiceInfo) {
        return put(getKey(nsdServiceInfo), nsdServiceInfo);
    }

    public NsdServiceInfo removePrinter(NsdServiceInfo nsdServiceInfo) {
        return remove(getKey(nsdServiceInfo));
    }
}
