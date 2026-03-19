package com.android.phone;

import java.util.HashMap;
import java.util.Map;

public class CarrierLogo {
    private static Map<String, Integer> sLogoMap = null;

    private CarrierLogo() {
    }

    private static Map<String, Integer> getLogoMap() {
        if (sLogoMap == null) {
            sLogoMap = new HashMap();
        }
        return sLogoMap;
    }

    public static int getLogo(String str) {
        Integer num = getLogoMap().get(str);
        if (num != null) {
            return num.intValue();
        }
        return -1;
    }
}
