package com.android.gallery3d.filtershow.filters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.android.gallery3d.app.Log;

public class ImageFilterFx extends ImageFilter {
    private FilterFxRepresentation mParameters = null;
    private Bitmap mFxBitmap = null;
    private Resources mResources = null;
    private int mFxBitmapId = 0;

    protected native void nativeApplyFilter(Bitmap bitmap, int i, int i2, Bitmap bitmap2, int i3, int i4, int i5, int i6);

    @Override
    public void freeResources() {
        if (this.mFxBitmap != null) {
            this.mFxBitmap.recycle();
        }
        this.mFxBitmap = null;
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        return null;
    }

    @Override
    public void useRepresentation(FilterRepresentation filterRepresentation) {
        this.mParameters = (FilterFxRepresentation) filterRepresentation;
    }

    public FilterFxRepresentation getParameters() {
        return this.mParameters;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float f, int i) {
        int i2;
        int i3;
        if (getParameters() == null || this.mResources == null) {
            return bitmap;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int bitmapResource = getParameters().getBitmapResource();
        if (bitmapResource == 0) {
            return bitmap;
        }
        if (this.mFxBitmap == null || this.mFxBitmapId != bitmapResource) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            this.mFxBitmapId = bitmapResource;
            if (this.mFxBitmapId != 0) {
                this.mFxBitmap = BitmapFactory.decodeResource(this.mResources, this.mFxBitmapId, options);
            } else {
                Log.w("ImageFilterFx", "bad resource for filter: " + this.mName);
            }
        }
        if (this.mFxBitmap == null) {
            return bitmap;
        }
        int width2 = this.mFxBitmap.getWidth();
        int height2 = this.mFxBitmap.getHeight();
        int i4 = width * 4;
        int i5 = i4 * height;
        int i6 = i4 * 256;
        int i7 = 0;
        while (i7 < i5) {
            int i8 = i7 + i6;
            int i9 = i8 > i5 ? i5 : i8;
            if (getEnvironment().needsStop()) {
                i2 = i8;
                i3 = i6;
            } else {
                i2 = i8;
                i3 = i6;
                nativeApplyFilter(bitmap, width, height, this.mFxBitmap, width2, height2, i7, i9);
            }
            i7 = i2;
            i6 = i3;
        }
        return bitmap;
    }

    public void setResources(Resources resources) {
        this.mResources = resources;
    }
}
