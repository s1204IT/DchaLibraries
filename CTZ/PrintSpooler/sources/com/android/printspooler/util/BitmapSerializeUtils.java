package com.android.printspooler.util;

import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;

public final class BitmapSerializeUtils {
    private static native void nativeReadBitmapPixels(Bitmap bitmap, int i);

    private static native void nativeWriteBitmapPixels(Bitmap bitmap, int i);

    static {
        System.loadLibrary("printspooler_jni");
    }

    public static void readBitmapPixels(Bitmap bitmap, ParcelFileDescriptor parcelFileDescriptor) {
        nativeReadBitmapPixels(bitmap, parcelFileDescriptor.getFd());
    }

    public static void writeBitmapPixels(Bitmap bitmap, ParcelFileDescriptor parcelFileDescriptor) {
        nativeWriteBitmapPixels(bitmap, parcelFileDescriptor.getFd());
    }
}
