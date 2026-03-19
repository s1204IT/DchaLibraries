package com.mediatek.common.wallpaper;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.util.Log;

public class DefaultWallpaperPlugin extends ContextWrapper implements IWallpaperPlugin {
    private static final String TAG = "DefaultWallpaperPlugin";

    public DefaultWallpaperPlugin(Context context) {
        super(context);
    }

    @Override
    public Resources getPluginResources(Context context) {
        Log.d(TAG, "into getPluginResources: null");
        return null;
    }

    @Override
    public int getPluginDefaultImage() {
        Log.d(TAG, "into getPluginDefaultImage: null");
        return 0;
    }
}
