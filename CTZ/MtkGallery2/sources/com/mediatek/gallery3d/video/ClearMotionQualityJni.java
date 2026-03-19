package com.mediatek.gallery3d.video;

public class ClearMotionQualityJni {
    public static native int nativeGetDemoMode();

    public static native int nativeGetFallbackIndex();

    public static native int nativeGetFallbackRange();

    public static native boolean nativeSetDemoMode(int i);

    public static native boolean nativeSetFallbackIndex(int i);

    private ClearMotionQualityJni() {
    }

    static {
        System.loadLibrary("MJCjni");
    }
}
