package com.android.wallpaperpicker.tileinfo;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.wallpaperpicker.R;

public abstract class DrawableThumbWallpaperInfo extends WallpaperTileInfo {
    private final Drawable mThumb;

    DrawableThumbWallpaperInfo(Drawable drawable) {
        this.mThumb = drawable;
    }

    @Override
    public View createView(Context context, LayoutInflater layoutInflater, ViewGroup viewGroup) {
        this.mView = layoutInflater.inflate(R.layout.wallpaper_picker_item, viewGroup, false);
        setThumb(this.mThumb);
        return this.mView;
    }

    public void setThumb(Drawable drawable) {
        if (this.mView != null && drawable != null) {
            drawable.setDither(true);
            ((ImageView) this.mView.findViewById(R.id.wallpaper_image)).setImageDrawable(drawable);
        }
    }
}
