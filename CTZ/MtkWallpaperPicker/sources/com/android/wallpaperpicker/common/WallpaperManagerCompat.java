package com.android.wallpaperpicker.common;

import android.content.Context;
import android.graphics.Rect;
import java.io.IOException;
import java.io.InputStream;

public abstract class WallpaperManagerCompat {
    private static WallpaperManagerCompat sInstance;
    private static final Object sInstanceLock = new Object();

    public abstract void clear(int i) throws IOException;

    public abstract void setStream(InputStream inputStream, Rect rect, boolean z, int i) throws IOException;

    public static WallpaperManagerCompat getInstance(Context context) {
        WallpaperManagerCompat wallpaperManagerCompat;
        synchronized (sInstanceLock) {
            if (sInstance == null) {
                if (Utilities.isAtLeastN()) {
                    sInstance = new WallpaperManagerCompatVN(context.getApplicationContext());
                } else {
                    sInstance = new WallpaperManagerCompatV16(context.getApplicationContext());
                }
            }
            wallpaperManagerCompat = sInstance;
        }
        return wallpaperManagerCompat;
    }
}
