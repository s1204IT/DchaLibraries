package com.android.providers.media;

import android.os.Build;
import android.os.SystemProperties;

public final class MediaUtils {
    public static final boolean IS_SUPPORT_DRM = SystemProperties.getBoolean("ro.vendor.mtk_oma_drm_support", false);
    private static final boolean ENG_LOAD = "eng".equals(Build.TYPE);
    public static final boolean LOG_QUERY = SystemProperties.getBoolean("vendor.debug.log_query", ENG_LOAD);
    public static final boolean LOG_INSERT = SystemProperties.getBoolean("vendor.debug.log_insert", ENG_LOAD);
    public static final boolean LOG_UPDATE = SystemProperties.getBoolean("vendor.debug.log_update", ENG_LOAD);
    public static final boolean LOG_DELETE = SystemProperties.getBoolean("vendor.debug.log_delete", ENG_LOAD);
    public static final boolean LOG_SCAN = SystemProperties.getBoolean("vendor.debug.log_scan", ENG_LOAD);
}
