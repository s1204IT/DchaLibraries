package com.android.printservice.recommendation.plugin.samsung;

import android.net.nsd.NsdServiceInfo;
import android.text.TextUtils;
import android.util.Log;
import com.android.printservice.recommendation.util.MDNSFilteredDiscovery;
import com.android.printservice.recommendation.util.MDNSUtils;
import java.util.HashSet;
import java.util.Set;

class PrinterFilterMopria implements MDNSFilteredDiscovery.PrinterFilter {
    static final Set<String> MOPRIA_MDNS_SERVICES = new HashSet<String>() {
        {
            add("_ipp._tcp");
            add("_ipps._tcp");
        }
    };

    PrinterFilterMopria() {
    }

    @Override
    public boolean matchesCriteria(NsdServiceInfo nsdServiceInfo) {
        boolean z = false;
        if (!MDNSUtils.isSupportedServiceType(nsdServiceInfo, MOPRIA_MDNS_SERVICES)) {
            return false;
        }
        String string = MDNSUtils.getString(nsdServiceInfo.getAttributes().get("pdl"));
        if (!TextUtils.isEmpty(string) && (string.contains("application/pdf") || string.contains("application/PCLm") || string.contains("image/pwg-raster"))) {
            z = true;
        }
        if (z) {
            Log.d("PrinterFilterMopria", "Mopria printer found: " + nsdServiceInfo.getServiceName());
        }
        return z;
    }
}
