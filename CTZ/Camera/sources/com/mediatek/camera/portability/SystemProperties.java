package com.mediatek.camera.portability;

public final class SystemProperties {
    public static int getInt(String str, int i) {
        try {
            return android.os.SystemProperties.getInt(str, i);
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }

    public static String getString(String str, String str2) {
        try {
            return android.os.SystemProperties.get(str, str2);
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }
}
