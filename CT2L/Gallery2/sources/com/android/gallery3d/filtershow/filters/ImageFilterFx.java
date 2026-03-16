package com.android.gallery3d.filtershow.filters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
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
    public void useRepresentation(FilterRepresentation representation) {
        FilterFxRepresentation parameters = (FilterFxRepresentation) representation;
        this.mParameters = parameters;
    }

    public FilterFxRepresentation getParameters() {
        return this.mParameters;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (getParameters() != null && this.mResources != null) {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            int bitmapResourceId = getParameters().getBitmapResource();
            if (bitmapResourceId != 0) {
                if (this.mFxBitmap == null || this.mFxBitmapId != bitmapResourceId) {
                    BitmapFactory.Options o = new BitmapFactory.Options();
                    o.inScaled = false;
                    this.mFxBitmapId = bitmapResourceId;
                    if (this.mFxBitmapId != 0) {
                        this.mFxBitmap = BitmapFactory.decodeResource(this.mResources, this.mFxBitmapId, o);
                    } else {
                        Log.w("ImageFilterFx", "bad resource for filter: " + this.mName);
                    }
                }
                if (this.mFxBitmap != null) {
                    int fxw = this.mFxBitmap.getWidth();
                    int fxh = this.mFxBitmap.getHeight();
                    int stride = w * 4;
                    int max = stride * h;
                    int increment = stride * NotificationCompat.FLAG_LOCAL_ONLY;
                    for (int i = 0; i < max; i += increment) {
                        int start = i;
                        int end = i + increment;
                        if (end > max) {
                            end = max;
                        }
                        if (!getEnvironment().needsStop()) {
                            nativeApplyFilter(bitmap, w, h, this.mFxBitmap, fxw, fxh, start, end);
                        }
                    }
                }
            }
        }
        return bitmap;
    }

    public void setResources(Resources resources) {
        this.mResources = resources;
    }
}
