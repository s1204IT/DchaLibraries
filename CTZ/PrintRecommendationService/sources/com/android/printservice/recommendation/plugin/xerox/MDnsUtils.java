package com.android.printservice.recommendation.plugin.xerox;

import android.net.nsd.NsdServiceInfo;
import android.text.TextUtils;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

class MDnsUtils {
    public static boolean isVendorPrinter(NsdServiceInfo nsdServiceInfo, String[] strArr) {
        Map<String, byte[]> attributes = nsdServiceInfo.getAttributes();
        String string = getString(attributes.get("product"));
        String string2 = getString(attributes.get("ty"));
        return ((!containsVendor(string, strArr) && !containsVendor(string2, strArr) && !containsVendor(getString(attributes.get("usb_MFG")), strArr) && !containsVendor(getString(attributes.get("mfg")), strArr)) || containsString(string2, "fuji") || containsString(string, "fuji") || containsString(getString(attributes.get("usb_MDL")), "fuji")) ? false : true;
    }

    public static String getVendor(NsdServiceInfo nsdServiceInfo) {
        Map<String, byte[]> attributes = nsdServiceInfo.getAttributes();
        String string = getString(attributes.get("mfg"));
        if (!TextUtils.isEmpty(string)) {
            return string;
        }
        String string2 = getString(attributes.get("usb_MFG"));
        if (TextUtils.isEmpty(string2)) {
            return null;
        }
        return string2;
    }

    public static boolean checkPDLSupport(NsdServiceInfo nsdServiceInfo, String[] strArr) {
        String string;
        if (strArr != null && (string = getString(nsdServiceInfo.getAttributes().get("pdl"))) != null) {
            for (String str : strArr) {
                if (string.contains(str)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsVendor(String str, String[] strArr) {
        if (str == null || strArr == null) {
            return false;
        }
        for (String str2 : strArr) {
            if (containsString(str, str2) || containsString(str.toLowerCase(Locale.US), str2.toLowerCase(Locale.US)) || containsString(str.toUpperCase(Locale.US), str2.toUpperCase(Locale.US))) {
                return true;
            }
        }
        return false;
    }

    private static String getString(byte[] bArr) {
        if (bArr != null) {
            return new String(bArr, StandardCharsets.UTF_8);
        }
        return null;
    }

    private static boolean containsString(String str, String str2) {
        if (str != null && str2 != null) {
            if (!str.equalsIgnoreCase(str2)) {
                if (str.contains(str2 + " ")) {
                }
            }
            return true;
        }
        return false;
    }
}
