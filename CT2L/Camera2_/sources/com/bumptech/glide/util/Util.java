package com.bumptech.glide.util;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.util.TimeUtils;

public class Util {
    private static final char[] hexArray = "0123456789abcdef".toCharArray();
    private static final char[] sha256Chars = new char[64];

    public static String sha256BytesToHex(byte[] bytes) {
        return bytesToHex(bytes, sha256Chars);
    }

    private static String bytesToHex(byte[] bytes, char[] hexChars) {
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 255;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[(j * 2) + 1] = hexArray[v & 15];
        }
        return new String(hexChars);
    }

    @TargetApi(TimeUtils.HUNDRED_DAY_FIELD_LEN)
    public static int getSize(Bitmap bitmap) {
        return Build.VERSION.SDK_INT >= 19 ? bitmap.getAllocationByteCount() : bitmap.getHeight() * bitmap.getRowBytes();
    }
}
