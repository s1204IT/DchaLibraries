package com.android.wallpaperpicker.tileinfo;

import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import com.android.photos.BitmapRegionTileSource;
import com.android.wallpaperpicker.WallpaperCropActivity;
import com.android.wallpaperpicker.WallpaperPickerActivity;

public class ResourceWallpaperInfo extends DrawableThumbWallpaperInfo {
    private final int mResId;
    private final Resources mResources;

    public ResourceWallpaperInfo(Resources resources, int i, Drawable drawable) {
        super(drawable);
        this.mResources = resources;
        this.mResId = i;
    }

    @Override
    public void onClick(final WallpaperPickerActivity wallpaperPickerActivity) {
        wallpaperPickerActivity.setWallpaperButtonEnabled(false);
        final BitmapRegionTileSource.InputStreamSource inputStreamSource = new BitmapRegionTileSource.InputStreamSource(this.mResources, this.mResId, wallpaperPickerActivity);
        wallpaperPickerActivity.setCropViewTileSource(inputStreamSource, false, false, new WallpaperCropActivity.CropViewScaleAndOffsetProvider() {
            @Override
            public float getScale(Point point, RectF rectF) {
                return point.x / rectF.width();
            }

            @Override
            public float getParallaxOffset() {
                return wallpaperPickerActivity.getWallpaperParallaxOffset();
            }
        }, new Runnable() {
            @Override
            public void run() {
                if (inputStreamSource.getLoadingState() == BitmapRegionTileSource.BitmapSource.State.LOADED) {
                    wallpaperPickerActivity.setWallpaperButtonEnabled(true);
                }
            }
        });
    }

    @Override
    public void onSave(WallpaperPickerActivity wallpaperPickerActivity) {
        wallpaperPickerActivity.cropImageAndSetWallpaper(this.mResources, this.mResId, true);
    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public boolean isNamelessWallpaper() {
        return true;
    }
}
