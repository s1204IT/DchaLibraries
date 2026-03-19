package com.android.gallery3d.filtershow.filters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.mediatek.gallery3d.util.Log;
import java.util.HashMap;

public class ImageFilterBorder extends ImageFilter {
    private static int sBorderSampleSize = 2;
    private FilterImageBorderRepresentation mParameters = null;
    private Resources mResources = null;
    private HashMap<Integer, Drawable> mDrawables = new HashMap<>();

    public ImageFilterBorder() {
        this.mName = "Border";
    }

    @Override
    public void useRepresentation(FilterRepresentation filterRepresentation) {
        this.mParameters = (FilterImageBorderRepresentation) filterRepresentation;
    }

    public FilterImageBorderRepresentation getParameters() {
        return this.mParameters;
    }

    @Override
    public void freeResources() {
        this.mDrawables.clear();
    }

    public Bitmap applyHelper(Bitmap bitmap, float f, float f2) {
        Rect rect = new Rect(0, 0, (int) Math.ceil(bitmap.getWidth() * f), (int) Math.ceil(bitmap.getHeight() * f));
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(f2, f2);
        Drawable drawable = getDrawable(getParameters().getDrawableResource());
        drawable.setBounds(rect);
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float f, int i) {
        if (getParameters() == null || getParameters().getDrawableResource() == 0) {
            return bitmap;
        }
        float f2 = f * 2.0f;
        return applyHelper(bitmap, 1.0f / f2, f2);
    }

    public void setResources(Resources resources) {
        if (this.mResources != resources) {
            this.mResources = resources;
            this.mDrawables.clear();
        }
    }

    public Drawable getDrawable(int i) {
        Drawable drawable = this.mDrawables.get(Integer.valueOf(i));
        if (drawable != null || this.mResources == null || i == 0) {
            return drawable;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sBorderSampleSize;
        Log.d("Gallery2/ImageFilterBorder", "getDrawable, set border sampleSize to " + options.inSampleSize);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(this.mResources, BitmapFactory.decodeResource(this.mResources, i, options));
        this.mDrawables.put(Integer.valueOf(i), bitmapDrawable);
        return bitmapDrawable;
    }

    public static void setBorderSampleSize(int i) {
        sBorderSampleSize = i;
    }
}
