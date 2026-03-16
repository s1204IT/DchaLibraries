package com.android.camera.util;

import android.os.Build;
import java.lang.reflect.Field;

public class ApiHelper {
    public static final boolean AT_LEAST_16;
    public static final boolean HAS_ANNOUNCE_FOR_ACCESSIBILITY;
    public static final boolean HAS_APP_GALLERY;
    public static final boolean HAS_AUTO_FOCUS_MOVE_CALLBACK;
    public static final boolean HAS_CAMERA_2_API;
    public static final boolean HAS_CAMERA_HDR;
    public static final boolean HAS_CAMERA_HDR_PLUS;
    public static final boolean HAS_DISPLAY_LISTENER;
    public static final boolean HAS_HIDEYBARS;
    public static final boolean HAS_MEDIA_ACTION_SOUND;
    public static final boolean HAS_MEDIA_COLUMNS_WIDTH_AND_HEIGHT;
    public static final boolean HAS_ORIENTATION_LOCK;
    public static final boolean HAS_ROBOTO_MEDIUM_FONT;
    public static final boolean HAS_ROTATION_ANIMATION;
    public static final boolean HAS_SET_BEAM_PUSH_URIS;
    public static final boolean HAS_SURFACE_TEXTURE_RECORDING;
    public static final boolean HDR_PLUS_CAN_USE_ARBITRARY_ASPECT_RATIOS;
    public static final boolean IS_HTC;
    public static final boolean IS_NEXUS_4;
    public static final boolean IS_NEXUS_5;
    public static final boolean IS_NEXUS_6;
    public static final boolean IS_NEXUS_9;

    static {
        boolean z = true;
        AT_LEAST_16 = Build.VERSION.SDK_INT >= 16;
        HAS_APP_GALLERY = Build.VERSION.SDK_INT >= 15;
        HAS_ANNOUNCE_FOR_ACCESSIBILITY = Build.VERSION.SDK_INT >= 16;
        HAS_AUTO_FOCUS_MOVE_CALLBACK = Build.VERSION.SDK_INT >= 16;
        HAS_MEDIA_ACTION_SOUND = Build.VERSION.SDK_INT >= 16;
        HAS_MEDIA_COLUMNS_WIDTH_AND_HEIGHT = Build.VERSION.SDK_INT >= 16;
        HAS_SET_BEAM_PUSH_URIS = Build.VERSION.SDK_INT >= 16;
        HAS_SURFACE_TEXTURE_RECORDING = Build.VERSION.SDK_INT >= 16;
        HAS_ROBOTO_MEDIUM_FONT = Build.VERSION.SDK_INT >= 16;
        HAS_CAMERA_HDR_PLUS = isKitKatOrHigher();
        HDR_PLUS_CAN_USE_ARBITRARY_ASPECT_RATIOS = isKitKatMR2OrHigher();
        HAS_CAMERA_HDR = Build.VERSION.SDK_INT >= 17;
        HAS_DISPLAY_LISTENER = Build.VERSION.SDK_INT >= 17;
        HAS_ORIENTATION_LOCK = Build.VERSION.SDK_INT >= 18;
        HAS_ROTATION_ANIMATION = Build.VERSION.SDK_INT >= 18;
        HAS_HIDEYBARS = isKitKatOrHigher();
        IS_NEXUS_4 = "mako".equalsIgnoreCase(Build.DEVICE);
        IS_NEXUS_5 = "LGE".equalsIgnoreCase(Build.MANUFACTURER) && "hammerhead".equalsIgnoreCase(Build.DEVICE);
        IS_NEXUS_6 = "motorola".equalsIgnoreCase(Build.MANUFACTURER) && "shamu".equalsIgnoreCase(Build.DEVICE);
        if (!"htc".equalsIgnoreCase(Build.MANUFACTURER) || (!"flounder".equalsIgnoreCase(Build.DEVICE) && !"flounder_lte".equalsIgnoreCase(Build.DEVICE))) {
            z = false;
        }
        IS_NEXUS_9 = z;
        IS_HTC = "htc".equalsIgnoreCase(Build.MANUFACTURER);
        HAS_CAMERA_2_API = isLOrHigher();
    }

    public static int getIntFieldIfExists(Class<?> klass, String fieldName, Class<?> obj, int defaultVal) {
        try {
            Field f = klass.getDeclaredField(fieldName);
            int defaultVal2 = f.getInt(obj);
            return defaultVal2;
        } catch (Exception e) {
            return defaultVal;
        }
    }

    public static boolean isKitKatOrHigher() {
        return Build.VERSION.SDK_INT >= 19 || "KeyLimePie".equals(Build.VERSION.CODENAME);
    }

    public static boolean isKitKatMR2OrHigher() {
        return isLOrHigher() || (isKitKatOrHigher() && ("4.4.4".equals(Build.VERSION.RELEASE) || "4.4.3".equals(Build.VERSION.RELEASE)));
    }

    public static boolean isLOrHigher() {
        return Build.VERSION.SDK_INT >= 21 || "L".equals(Build.VERSION.CODENAME);
    }
}
