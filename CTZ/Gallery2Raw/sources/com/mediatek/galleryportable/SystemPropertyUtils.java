package com.mediatek.galleryportable;

import android.os.SystemProperties;

public class SystemPropertyUtils {
    private static boolean sIsSystemPropertiesExist = false;
    private static boolean sHasChecked = false;

    public static int getInt(String key, int defaultValue) {
        if (isSystemPropertiesExist()) {
            return SystemProperties.getInt(key, defaultValue);
        }
        return defaultValue;
    }

    public static String get(String key) {
        if (isSystemPropertiesExist()) {
            return SystemProperties.get(key);
        }
        return "";
    }

    private static boolean isSystemPropertiesExist() {
        if (!sHasChecked) {
            try {
                Class<?> clazz = SystemPropertyUtils.class.getClassLoader().loadClass("android.os.SystemProperties");
                sIsSystemPropertiesExist = clazz != null;
                sHasChecked = true;
            } catch (ClassNotFoundException e) {
                sIsSystemPropertiesExist = false;
                sHasChecked = true;
            }
        }
        return sIsSystemPropertiesExist;
    }
}
