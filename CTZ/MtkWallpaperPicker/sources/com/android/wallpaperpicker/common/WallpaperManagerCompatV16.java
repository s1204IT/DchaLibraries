package com.android.wallpaperpicker.common;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Rect;
import java.io.IOException;
import java.io.InputStream;

public class WallpaperManagerCompatV16 extends WallpaperManagerCompat {
    protected WallpaperManager mWallpaperManager;

    public WallpaperManagerCompatV16(Context context) {
        this.mWallpaperManager = WallpaperManager.getInstance(context.getApplicationContext());
    }

    @Override
    public void setStream(InputStream inputStream, Rect rect, boolean z, int i) throws IOException {
        this.mWallpaperManager.setStream(inputStream);
    }

    @Override
    public void clear(int i) throws IOException {
        this.mWallpaperManager.clear();
    }
}
