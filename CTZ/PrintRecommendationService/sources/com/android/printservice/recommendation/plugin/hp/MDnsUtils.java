package com.android.printservice.recommendation.plugin.hp;

import android.net.nsd.NsdServiceInfo;
import android.text.TextUtils;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

public class MDnsUtils {
    public static String getString(byte[] bArr) {
        if (bArr != null) {
            return new String(bArr, StandardCharsets.UTF_8);
        }
        return null;
    }

    public static boolean isVendorPrinter(NsdServiceInfo nsdServiceInfo, String[] strArr) {
        Map<String, byte[]> attributes = nsdServiceInfo.getAttributes();
        return containsVendor(getString(attributes.get("product")), strArr) || containsVendor(getString(attributes.get("ty")), strArr) || containsVendor(getString(attributes.get("usb_MFG")), strArr) || containsVendor(getString(attributes.get("mfg")), strArr);
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
