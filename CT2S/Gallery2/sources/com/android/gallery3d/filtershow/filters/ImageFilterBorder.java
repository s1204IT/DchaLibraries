package com.android.gallery3d.filtershow.filters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import java.lang.ref.WeakReference;
import java.util.HashMap;

public class ImageFilterBorder extends ImageFilter {
    private FilterImageBorderRepresentation mParameters = null;
    private Resources mResources = null;
    private HashMap<Integer, WeakReference<Drawable>> mDrawables = new HashMap<>();

    public ImageFilterBorder() {
        this.mName = "Border";
    }

    @Override
    public void useRepresentation(FilterRepresentation representation) {
        FilterImageBorderRepresentation parameters = (FilterImageBorderRepresentation) representation;
        this.mParameters = parameters;
    }

    public FilterImageBorderRepresentation getParameters() {
        return this.mParameters;
    }

    @Override
    public void freeResources() {
        this.mDrawables.clear();
    }

    public Bitmap applyHelper(Bitmap bitmap, float scale1, float scale2) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Rect bounds = new Rect(0, 0, (int) (w * scale1), (int) (h * scale1));
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(scale2, scale2);
        Drawable drawable = getDrawable(getParameters().getDrawableResource());
        drawable.setBounds(bounds);
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (getParameters() != null && getParameters().getDrawableResource() != 0) {
            float scale2 = scaleFactor * 2.0f;
            float scale1 = 1.0f / scale2;
            return applyHelper(bitmap, scale1, scale2);
        }
        return bitmap;
    }

    public void setResources(Resources resources) {
        if (this.mResources != resources) {
            this.mResources = resources;
            this.mDrawables.clear();
        }
    }

    public Drawable getDrawable(int rsc) {
        WeakReference<Drawable> ref = this.mDrawables.get(Integer.valueOf(rsc));
        Drawable drawable = null;
        if (ref != null) {
            Drawable drawable2 = ref.get();
            drawable = drawable2;
        }
        if (drawable == null && this.mResources != null && rsc != 0) {
            Drawable drawable3 = new BitmapDrawable(this.mResources, BitmapFactory.decodeResource(this.mResources, rsc));
            this.mDrawables.put(Integer.valueOf(rsc), new WeakReference<>(drawable3));
            return drawable3;
        }
        return drawable;
    }
}
