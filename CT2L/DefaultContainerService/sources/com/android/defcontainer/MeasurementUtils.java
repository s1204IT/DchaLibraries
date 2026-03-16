package com.android.defcontainer;

public class MeasurementUtils {
    private static native long native_measureDirectory(String str);

    static {
        System.loadLibrary("defcontainer_jni");
    }

    public static long measureDirectory(String path) {
        return native_measureDirectory(path);
    }
}
