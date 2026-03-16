package com.android.gallery3d.glrenderer;

import android.graphics.Bitmap;
import junit.framework.Assert;

public class BitmapTexture extends UploadedTexture {
    protected Bitmap mContentBitmap;

    public BitmapTexture(Bitmap bitmap) {
        this(bitmap, false);
    }

    public BitmapTexture(Bitmap bitmap, boolean hasBorder) {
        super(hasBorder);
        Assert.assertTrue((bitmap == null || bitmap.isRecycled()) ? false : true);
        this.mContentBitmap = bitmap;
    }

    @Override
    protected void onFreeBitmap(Bitmap bitmap) {
    }

    @Override
    protected Bitmap onGetBitmap() {
        return this.mContentBitmap;
    }
}
