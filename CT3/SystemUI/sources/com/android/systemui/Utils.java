package com.android.systemui;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.GraphicBuffer;

public final class Utils {
    public static boolean useYv12;

    public static native int BitmapToYv12(Bitmap bitmap, GraphicBuffer graphicBuffer);

    public static native void createTexture2D(int i, GraphicBuffer graphicBuffer);

    static {
        try {
            useYv12 = true;
            System.loadLibrary("yv12util");
            Log.w("Utils", " yv12util library loading");
        } catch (SecurityException e) {
            Log.w("Utils", "security manager exists and its checkLink method doesn't allow loading", e);
            useYv12 = false;
        } catch (UnsatisfiedLinkError e2) {
            Log.w("Utils", "library does not exist.", e2);
            useYv12 = false;
        }
    }

    private Utils() {
        Log.w("Utils", " utils constructor");
    }
}
