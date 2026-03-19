package com.mediatek.wallpaper;

import android.content.Context;
import android.util.Log;
import dalvik.system.PathClassLoader;
import java.io.InputStream;

public class MtkWallpaperFactory {
    private static MtkWallpaperFactory sInstance = null;
    private static String TAG = "MtkWallpaperFactory";

    public static MtkWallpaperFactory getInstance() {
        if (sInstance == null) {
            try {
                sInstance = (MtkWallpaperFactory) Class.forName("com.mediatek.wallpaper.MtkWallpaperFactoryImpl", false, new PathClassLoader("/system/framework/mediatek-framework.jar", MtkWallpaperFactory.class.getClassLoader())).getConstructor(new Class[0]).newInstance(new Object[0]);
            } catch (Exception e) {
                Log.e(TAG, "Exception happened");
            }
            if (sInstance == null) {
                sInstance = new MtkWallpaperFactory();
            }
        }
        return sInstance;
    }

    public InputStream openDefaultWallpaper(Context context, int i) {
        return null;
    }
}
