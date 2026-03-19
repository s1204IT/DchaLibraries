package com.android.printservice.recommendation.util;

import android.net.nsd.NsdServiceInfo;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MDNSUtils {
    public static boolean isVendorPrinter(NsdServiceInfo nsdServiceInfo, Set<String> set) {
        Iterator<Map.Entry<String, byte[]>> it = nsdServiceInfo.getAttributes().entrySet().iterator();
        while (true) {
            byte b = 0;
            if (!it.hasNext()) {
                return false;
            }
            Map.Entry<String, byte[]> next = it.next();
            String lowerCase = next.getKey().toLowerCase();
            int iHashCode = lowerCase.hashCode();
            if (iHashCode != -309474065) {
                if (iHashCode != -150456141) {
                    if (iHashCode != 3717) {
                        b = (iHashCode == 108014 && lowerCase.equals("mfg")) ? (byte) 3 : (byte) -1;
                    } else if (!lowerCase.equals("ty")) {
                    }
                } else if (lowerCase.equals("usb_mfg")) {
                    b = 2;
                }
            } else if (lowerCase.equals("product")) {
                b = 1;
            }
            switch (b) {
                case 0:
                case 1:
                case 2:
                case 3:
                    if (next.getValue() != null && containsVendor(new String(next.getValue(), StandardCharsets.UTF_8), set)) {
                        return true;
                    }
                    break;
                    break;
            }
        }
    }

    private static boolean containsVendor(String str, Set<String> set) {
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            if (containsString(str.toLowerCase(), it.next().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsString(String str, String str2) {
        if (!str.equalsIgnoreCase(str2)) {
            if (!str.contains(str2 + " ")) {
                return false;
            }
        }
        return true;
    }

    public static String getString(byte[] bArr) {
        if (bArr != null) {
            return new String(bArr, StandardCharsets.UTF_8);
        }
        return null;
    }

    public static boolean isSupportedServiceType(NsdServiceInfo nsdServiceInfo, Set<String> set) {
        String lowerCase = nsdServiceInfo.getServiceType().toLowerCase();
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            if (lowerCase.contains(it.next().toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
