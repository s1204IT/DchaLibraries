package com.android.wallpaperpicker.common;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Rect;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

public class WallpaperManagerCompatVN extends WallpaperManagerCompatV16 {
    public WallpaperManagerCompatVN(Context context) {
        super(context);
    }

    @Override
    public void setStream(InputStream inputStream, Rect rect, boolean z, int i) throws IOException {
        try {
            WallpaperManager.class.getMethod("setStream", InputStream.class, Rect.class, Boolean.TYPE, Integer.TYPE).invoke(this.mWallpaperManager, inputStream, rect, Boolean.valueOf(z), Integer.valueOf(i));
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            super.setStream(inputStream, rect, z, i);
        }
    }

    @Override
    public void clear(int i) throws IOException {
        try {
            WallpaperManager.class.getMethod("clear", Integer.TYPE).invoke(this.mWallpaperManager, Integer.valueOf(i));
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            super.clear(i);
        }
    }
}
