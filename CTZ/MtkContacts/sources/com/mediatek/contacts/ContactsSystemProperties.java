package com.mediatek.contacts;

import android.os.SystemProperties;
import android.telephony.TelephonyManager;

public class ContactsSystemProperties {
    public static final boolean MTK_DRM_SUPPORT;
    public static final boolean MTK_GEMINI_SUPPORT;

    static {
        MTK_GEMINI_SUPPORT = TelephonyManager.getDefault().getSimCount() > 1;
        MTK_DRM_SUPPORT = isPropertyEnabled("ro.vendor.mtk_oma_drm_support");
    }

    private static boolean isPropertyEnabled(String str) {
        return "1".equals(SystemProperties.get(str));
    }
}
