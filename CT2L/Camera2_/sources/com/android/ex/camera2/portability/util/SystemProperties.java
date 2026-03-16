package com.android.ex.camera2.portability.util;

import com.android.ex.camera2.portability.debug.Log;
import java.lang.reflect.Method;

public final class SystemProperties {
    private static final Log.Tag TAG = new Log.Tag("SysProps");

    public static String get(String key, String defaultValue) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method get = systemProperties.getMethod("get", String.class, String.class);
            return (String) get.invoke(null, key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "Exception while getting system property: ", e);
            return defaultValue;
        }
    }

    private SystemProperties() {
    }
}
