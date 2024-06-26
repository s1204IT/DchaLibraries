package com.android.systemui.statusbar.policy;

import android.os.SystemProperties;
/* loaded from: classes.dex */
public class EncryptionHelper {
    public static final boolean IS_DATA_ENCRYPTED = isDataEncrypted();

    private static boolean isDataEncrypted() {
        String str = SystemProperties.get("vold.decrypt");
        return "1".equals(str) || "trigger_restart_min_framework".equals(str);
    }
}
