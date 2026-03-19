package com.android.wallpaperpicker.tileinfo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.gallery3d.common.Utils;
import com.android.wallpaperpicker.R;
import com.android.wallpaperpicker.WallpaperPickerActivity;
import com.android.wallpaperpicker.common.InputStreamProvider;

public abstract class WallpaperTileInfo {
    protected View mView;

    public abstract View createView(Context context, LayoutInflater layoutInflater, ViewGroup viewGroup);

    public void onClick(WallpaperPickerActivity wallpaperPickerActivity) {
    }

    public void onSave(WallpaperPickerActivity wallpaperPickerActivity) {
    }

    public void onDelete(WallpaperPickerActivity wallpaperPickerActivity) {
    }

    public boolean isSelectable() {
        return false;
    }

    public boolean isNamelessWallpaper() {
        return false;
    }

    public void onIndexUpdated(CharSequence charSequence) {
        if (isNamelessWallpaper()) {
            this.mView.setContentDescription(charSequence);
        }
    }

    protected static Point getDefaultThumbSize(Resources resources) {
        return new Point(resources.getDimensionPixelSize(R.dimen.wallpaperThumbnailWidth), resources.getDimensionPixelSize(R.dimen.wallpaperThumbnailHeight));
    }

    protected static Bitmap createThumbnail(InputStreamProvider inputStreamProvider, Context context, int i, boolean z) {
        Point defaultThumbSize = getDefaultThumbSize(context.getResources());
        int i2 = defaultThumbSize.x;
        int i3 = defaultThumbSize.y;
        if (inputStreamProvider.getImageBounds() == null) {
            return null;
        }
        Matrix matrix = new Matrix();
        matrix.setRotate(i);
        float[] fArr = {r1.x, r1.y};
        matrix.mapPoints(fArr);
        fArr[0] = Math.abs(fArr[0]);
        fArr[1] = Math.abs(fArr[1]);
        return inputStreamProvider.readCroppedBitmap(Utils.getMaxCropRect((int) fArr[0], (int) fArr[1], i2, i3, z), i2, i3, i);
    }
}
