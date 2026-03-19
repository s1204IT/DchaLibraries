package com.mediatek.omadrm;

import android.app.DialogFragment;
import android.net.Uri;
import android.os.Build;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

public class OmaDrmUtils {
    private static final boolean DEBUG;
    private static final Uri FILE_URI;
    private static DialogFragment sConsumeDialog;
    private static boolean sIsOmaDrmEnabled;
    private static DialogFragment sProtectionInfoDialog;

    static {
        DEBUG = Log.isLoggable("OmaDrmUtils", 3) || "eng".equals(Build.TYPE);
        FILE_URI = MediaStore.Files.getContentUri("external");
        sConsumeDialog = null;
        sProtectionInfoDialog = null;
        sIsOmaDrmEnabled = SystemProperties.getBoolean("ro.vendor.mtk_oma_drm_support", false);
    }

    public static boolean isOmaDrmEnabled() {
        return sIsOmaDrmEnabled;
    }

    public static int getActionByMimetype(String str) {
        int i = 0;
        if (!TextUtils.isEmpty(str)) {
            if (str.startsWith("image/")) {
                i = 7;
            } else if (str.startsWith("video/") || str.startsWith("audio/")) {
                i = 1;
            }
        }
        if (DEBUG) {
            Log.d("OmaDrmUtils", "getActionByMimetype: mimetype=" + str + ", action=" + i);
        }
        return i;
    }
}
