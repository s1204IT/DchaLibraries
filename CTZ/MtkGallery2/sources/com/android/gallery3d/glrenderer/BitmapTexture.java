package com.android.gallery3d.glrenderer;

import android.graphics.Bitmap;
import com.android.gallery3d.common.Utils;
import com.android.photos.data.GalleryBitmapPool;

public class BitmapTexture extends UploadedTexture {
    protected Bitmap mContentBitmap;
    private boolean mNeedRecycleBitmap;

    public BitmapTexture(Bitmap bitmap) {
        this(bitmap, false);
    }

    public BitmapTexture(boolean z, Bitmap bitmap) {
        this(bitmap, false);
        this.mNeedRecycleBitmap = z;
    }

    public BitmapTexture(Bitmap bitmap, boolean z) {
        super(z);
        Utils.assertTrue((bitmap == null || bitmap.isRecycled()) ? false : true);
        this.mContentBitmap = bitmap;
    }

    @Override
    protected void onFreeBitmap(Bitmap bitmap) {
        if (this.mNeedRecycleBitmap && bitmap != null) {
            GalleryBitmapPool.getInstance().put(bitmap);
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
