package com.android.server.policy;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.util.DisplayMetrics;

public final class IconUtilities {
    private ColorFilter mDisabledColorFilter;
    private final DisplayMetrics mDisplayMetrics;
    private int mIconHeight;
    private int mIconTextureHeight;
    private int mIconTextureWidth;
    private int mIconWidth;
    private final Rect mOldBounds = new Rect();
    private final Canvas mCanvas = new Canvas();

    public IconUtilities(Context context) {
        this.mIconWidth = -1;
        this.mIconHeight = -1;
        this.mIconTextureWidth = -1;
        this.mIconTextureHeight = -1;
        Resources resources = context.getResources();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        this.mDisplayMetrics = displayMetrics;
        float f = 5.0f * displayMetrics.density;
        int dimension = (int) resources.getDimension(R.dimen.app_icon_size);
        this.mIconHeight = dimension;
        this.mIconWidth = dimension;
        int i = this.mIconWidth + ((int) (f * 2.0f));
        this.mIconTextureHeight = i;
        this.mIconTextureWidth = i;
        this.mCanvas.setDrawFilter(new PaintFlagsDrawFilter(4, 2));
    }

    public Bitmap createIconBitmap(Drawable drawable) {
        int i = this.mIconWidth;
        int i2 = this.mIconHeight;
        if (drawable instanceof PaintDrawable) {
            PaintDrawable paintDrawable = (PaintDrawable) drawable;
            paintDrawable.setIntrinsicWidth(i);
            paintDrawable.setIntrinsicHeight(i2);
        } else if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap().getDensity() == 0) {
                bitmapDrawable.setTargetDensity(this.mDisplayMetrics);
            }
        }
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        if (intrinsicWidth > 0 && intrinsicHeight > 0) {
            if (i < intrinsicWidth || i2 < intrinsicHeight) {
                float f = intrinsicWidth / intrinsicHeight;
                if (intrinsicWidth > intrinsicHeight) {
                    i2 = (int) (i / f);
                } else if (intrinsicHeight > intrinsicWidth) {
                    i = (int) (i2 * f);
                }
            } else if (intrinsicWidth < i && intrinsicHeight < i2) {
                i = intrinsicWidth;
                i2 = intrinsicHeight;
            }
        }
        int i3 = this.mIconTextureWidth;
        int i4 = this.mIconTextureHeight;
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i3, i4, Bitmap.Config.ARGB_8888);
        Canvas canvas = this.mCanvas;
        canvas.setBitmap(bitmapCreateBitmap);
        int i5 = (i3 - i) / 2;
        int i6 = (i4 - i2) / 2;
        this.mOldBounds.set(drawable.getBounds());
        drawable.setBounds(i5, i6, i + i5, i2 + i6);
        drawable.draw(canvas);
        drawable.setBounds(this.mOldBounds);
        return bitmapCreateBitmap;
    }

    public ColorFilter getDisabledColorFilter() {
        if (this.mDisabledColorFilter != null) {
            return this.mDisabledColorFilter;
        }
        ColorMatrix colorMatrix = new ColorMatrix();
        float[] array = colorMatrix.getArray();
        array[0] = 0.5f;
        array[6] = 0.5f;
        array[12] = 0.5f;
        float f = (int) 127.5f;
        array[4] = f;
        array[9] = f;
        array[14] = f;
        ColorMatrix colorMatrix2 = new ColorMatrix();
        colorMatrix2.setSaturation(0.0f);
        colorMatrix2.preConcat(colorMatrix);
        this.mDisabledColorFilter = new ColorMatrixColorFilter(colorMatrix2);
        return this.mDisabledColorFilter;
    }
}
