package com.mediatek.mmprofile;

public class MMProfile {
    public static final int MMP_RootEvent = 1;
    public static final int MMProfileFlagEnd = 2;
    public static final int MMProfileFlagEventSeparator = 8;
    public static final int MMProfileFlagPulse = 4;
    public static final int MMProfileFlagStart = 1;
    public static final int MMProfileFlagSystrace = Integer.MIN_VALUE;

    public static native void MMProfileEnableEvent(int i, int i2);

    public static native void MMProfileEnableEventRecursive(int i, int i2);

    public static native int MMProfileFindEvent(int i, String str);

    public static native void MMProfileLog(int i, int i2);

    public static native void MMProfileLogEx(int i, int i2, int i3, int i4);

    public static native int MMProfileLogMetaString(int i, int i2, String str);

    public static native int MMProfileLogMetaStringEx(int i, int i2, int i3, int i4, String str);

    public static native int MMProfileQueryEnable(int i);

    public static native int MMProfileRegisterEvent(int i, String str);

    static {
        System.loadLibrary("mmprofile_jni");
    }
}
