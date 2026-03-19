package com.mediatek.gallerybasic.gl;

import android.graphics.Bitmap;

public class MBitmapTexture extends MUploadedTexture {
    protected Bitmap mContentBitmap;
    private boolean mNeedRecycleBitmap;

    public MBitmapTexture(Bitmap bitmap) {
        this(bitmap, false);
    }

    public MBitmapTexture(Bitmap bitmap, boolean z) {
        super(z);
        this.mNeedRecycleBitmap = true;
        com.android.gallery3d.common.Utils.assertTrue((bitmap == null || bitmap.isRecycled()) ? false : true);
        this.mContentBitmap = bitmap;
    }

    public MBitmapTexture(Bitmap bitmap, boolean z, boolean z2) {
        super(z);
        this.mNeedRecycleBitmap = true;
        com.android.gallery3d.common.Utils.assertTrue((bitmap == null || bitmap.isRecycled()) ? false : true);
        this.mContentBitmap = bitmap;
        this.mNeedRecycleBitmap = z2;
    }

    @Override
    protected void onFreeBitmap(Bitmap bitmap) {
        if (this.mNeedRecycleBitmap && !inFinalizer() && bitmap != null) {
            bitmap.recycle();
        }
    }

    @Override
    protected Bitmap onGetBitmap() {
        return this.mContentBitmap;
    }

    public Bitmap getBitmap() {
        return this.mContentBitmap;
    }
}
