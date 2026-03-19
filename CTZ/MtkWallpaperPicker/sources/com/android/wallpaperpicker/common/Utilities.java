package com.android.wallpaperpicker.common;

import android.app.WallpaperManager;

public class Utilities {
    public static boolean isAtLeastN() {
        try {
            WallpaperManager.class.getMethod("getWallpaperFile", Integer.TYPE);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
