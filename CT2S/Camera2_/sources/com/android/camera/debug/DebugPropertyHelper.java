package com.android.camera.debug;

import com.android.camera.util.SystemProperties;

public class DebugPropertyHelper {
    private static final boolean FORCE_ZSL_APP = false;
    private static final String OFF_VALUE = "0";
    private static final String ON_VALUE = "1";
    private static final String PREFIX = "persist.camera";
    private static final String PROP_CAPTURE_DEBUG_UI = "persist.camera.debug_ui";
    private static final String PROP_ENABLE_CAPTURE_MODULE = "persist.camera.newcapture";
    private static final String PROP_ENABLE_ZSL = "persist.camera.zsl";
    private static final String PROP_FRAME_LOG = "persist.camera.frame_log";
    private static final String PROP_WRITE_CAPTURE_DATA = "persist.camera.capture_write";

    private static boolean isPropertyOn(String property) {
        return ON_VALUE.equals(SystemProperties.get(property, OFF_VALUE));
    }

    public static boolean isCaptureModuleEnabled() {
        if (isPropertyOn(PROP_ENABLE_CAPTURE_MODULE)) {
            return true;
        }
        return FORCE_ZSL_APP;
    }

    public static boolean isZslEnabled() {
        if (isPropertyOn(PROP_ENABLE_ZSL)) {
            return true;
        }
        return FORCE_ZSL_APP;
    }

    public static boolean showFrameDebugLog() {
        return isPropertyOn(PROP_FRAME_LOG);
    }

    public static boolean showCaptureDebugUI() {
        return isPropertyOn(PROP_CAPTURE_DEBUG_UI);
    }

    public static boolean writeCaptureData() {
        return isPropertyOn(PROP_WRITE_CAPTURE_DATA);
    }
}
