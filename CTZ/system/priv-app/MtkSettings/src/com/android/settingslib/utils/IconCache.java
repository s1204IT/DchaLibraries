package com.android.settingslib.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.support.v4.util.ArrayMap;

/* loaded from: classes.dex */
public class IconCache {
    private final Context mContext;
    final ArrayMap<Icon, Drawable> mMap = new ArrayMap<>();

    public IconCache(Context context) {
        this.mContext = context;
    }

    public Drawable getIcon(Icon icon) {
        if (icon == null) {
            return null;
        }
        Drawable drawable = this.mMap.get(icon);
        if (drawable == null) {
            Drawable drawableLoadDrawable = icon.loadDrawable(this.mContext);
            updateIcon(icon, drawableLoadDrawable);
            return drawableLoadDrawable;
        }
        return drawable;
    }

    public void updateIcon(Icon icon, Drawable drawable) {
        this.mMap.put(icon, drawable);
    }
}
