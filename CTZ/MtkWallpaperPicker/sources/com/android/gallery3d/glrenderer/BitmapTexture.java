package com.android.gallery3d.glrenderer;

import android.graphics.Bitmap;
import com.android.gallery3d.common.Utils;

public class BitmapTexture extends UploadedTexture {
    protected Bitmap mContentBitmap;

    public BitmapTexture(Bitmap bitmap) {
        Utils.assertTrue((bitmap == null || bitmap.isRecycled()) ? false : true);
        this.mContentBitmap = bitmap;
    }

    @Override
    protected void onFreeBitmap(Bitmap bitmap) {
    }

    @Override
    protected Bitmap onGetBitmap() {
        return this.mContentBitmap;
    }

    public Bitmap getBitmap() {
        return this.mContentBitmap;
    }
}
