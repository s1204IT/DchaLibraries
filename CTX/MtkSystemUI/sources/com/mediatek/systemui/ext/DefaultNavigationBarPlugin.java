package com.mediatek.systemui.ext;

import android.content.Context;
import android.graphics.drawable.Drawable;

public class DefaultNavigationBarPlugin implements INavigationBarPlugin {
    private Context mContext;

    public DefaultNavigationBarPlugin(Context context) {
        this.mContext = context;
    }

    @Override
    public Drawable getBackImage(Drawable drawable) {
        return drawable;
    }

    @Override
    public Drawable getBackLandImage(Drawable drawable) {
        return drawable;
    }

    @Override
    public Drawable getBackImeImage(Drawable drawable) {
        return drawable;
    }

    @Override
    public Drawable getBackImelandImage(Drawable drawable) {
        return drawable;
    }

    @Override
    public Drawable getHomeImage(Drawable drawable) {
        return drawable;
    }

    @Override
    public Drawable getHomeLandImage(Drawable drawable) {
        return drawable;
    }

    @Override
    public Drawable getRecentImage(Drawable drawable) {
        return drawable;
    }

    @Override
    public Drawable getRecentLandImage(Drawable drawable) {
        return drawable;
    }
}
