package com.mediatek.systemui.statusbar.util;

import android.os.SystemProperties;
import android.util.Log;

public class FeatureOptions {
    public static final boolean LOG_ENABLE;
    public static final boolean LOW_RAM_SUPPORT = isPropertyEnabledBoolean("ro.config.low_ram");
    public static final boolean MTK_CTA_SET = isPropertyEnabledInt("ro.vendor.mtk_cta_set");
    public static final boolean MTK_CT_MIXED_VOLTE_SUPPORT = isMtkCtMixedVolteSupport();

    static {
        LOG_ENABLE = SystemProperties.get("ro.build.type").equals("eng") || SystemProperties.get("ro.build.type").equals("userdebug");
    }

    private static boolean isPropertyEnabledBoolean(String str) {
        return "true".equals(SystemProperties.get(str, "false"));
    }

    private static boolean isPropertyEnabledInt(String str) {
        return "1".equals(SystemProperties.get(str));
    }

    private static boolean isMtkCtMixedVolteSupport() {
        if (LOG_ENABLE) {
            Log.d("FeatureOptions", "isMtkCtMixedVolteSupport: false");
        }
        return false;
    }
}
