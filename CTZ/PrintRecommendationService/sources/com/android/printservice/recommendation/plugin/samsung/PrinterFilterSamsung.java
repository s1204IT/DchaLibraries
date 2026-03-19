package com.android.printservice.recommendation.plugin.samsung;

import android.net.nsd.NsdServiceInfo;
import android.text.TextUtils;
import android.util.Log;
import com.android.printservice.recommendation.util.MDNSFilteredDiscovery;
import com.android.printservice.recommendation.util.MDNSUtils;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

class PrinterFilterSamsung implements MDNSFilteredDiscovery.PrinterFilter {
    static final Set<String> SAMSUNG_MDNS_SERVICES = new HashSet<String>() {
        {
            add("_pdl-datastream._tcp");
        }
    };
    private static final String[] NOT_SUPPORTED_MODELS = {"SCX-5x15", "SF-555P", "CF-555P", "SCX-4x16", "SCX-4214F", "CLP-500", "CJX-", "MJC-"};
    private static Set<String> SAMUNG_VENDOR_SET = new HashSet<String>() {
        {
            add("samsung");
        }
    };

    PrinterFilterSamsung() {
    }

    @Override
    public boolean matchesCriteria(NsdServiceInfo nsdServiceInfo) {
        String samsungModelName;
        if (!MDNSUtils.isSupportedServiceType(nsdServiceInfo, SAMSUNG_MDNS_SERVICES) || !MDNSUtils.isVendorPrinter(nsdServiceInfo, SAMUNG_VENDOR_SET) || (samsungModelName = getSamsungModelName(nsdServiceInfo)) == null || !isSupportedSamsungModel(samsungModelName)) {
            return false;
        }
        Log.d("PrinterFilterSamsung", "Samsung printer found: " + nsdServiceInfo.getServiceName());
        return true;
    }

    private boolean isSupportedSamsungModel(String str) {
        if (!TextUtils.isEmpty(str)) {
            String upperCase = str.toUpperCase(Locale.US);
            for (String str2 : NOT_SUPPORTED_MODELS) {
                if (upperCase.contains(str2)) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private String getSamsungModelName(NsdServiceInfo nsdServiceInfo) {
        Map<String, byte[]> attributes = nsdServiceInfo.getAttributes();
        String string = MDNSUtils.getString(attributes.get("usb_MFG"));
        if (TextUtils.isEmpty(string)) {
            string = MDNSUtils.getString(attributes.get("mfg"));
        }
        String string2 = MDNSUtils.getString(attributes.get("usb_MDL"));
        if (TextUtils.isEmpty(string2)) {
            string2 = MDNSUtils.getString(attributes.get("mdl"));
        }
        if (!TextUtils.isEmpty(string) && !TextUtils.isEmpty(string2)) {
            return string.trim() + " " + string2.trim();
        }
        String string3 = MDNSUtils.getString(attributes.get("product"));
        if (TextUtils.isEmpty(string3)) {
            return MDNSUtils.getString(attributes.get("ty"));
        }
        return string3;
    }
}
