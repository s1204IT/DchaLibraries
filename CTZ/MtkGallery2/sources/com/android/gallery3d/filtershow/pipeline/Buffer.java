package com.android.gallery3d.filtershow.pipeline;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

public class Buffer {
    private Allocation mAllocation;
    private Bitmap mBitmap;
    private ImagePreset mPreset;
    private boolean mUseAllocation = false;

    public Buffer(Bitmap bitmap) {
        RenderScript renderScriptContext = CachingPipeline.getRenderScriptContext();
        if (bitmap != null) {
            this.mBitmap = MasterImage.getImage().getBitmapCache().getBitmapCopy(bitmap, 1);
        }
        if (this.mUseAllocation) {
            this.mAllocation = Allocation.createFromBitmap(renderScriptContext, this.mBitmap, Allocation.MipmapControl.MIPMAP_NONE, 129);
        }
    }

    public boolean isSameSize(Bitmap bitmap) {
        return this.mBitmap != null && bitmap != null && this.mBitmap.getWidth() == bitmap.getWidth() && this.mBitmap.getHeight() == bitmap.getHeight();
    }

    public synchronized void useBitmap(Bitmap bitmap) {
        if (this.mBitmap != null) {
            this.mBitmap.eraseColor(0);
        }
        new Canvas(this.mBitmap).drawBitmap(bitmap, 0.0f, 0.0f, (Paint) null);
    }

    public synchronized Bitmap getBitmap() {
        return this.mBitmap;
    }

    public void sync() {
        if (this.mUseAllocation) {
            this.mAllocation.copyTo(this.mBitmap);
        }
    }

    public ImagePreset getPreset() {
        return this.mPreset;
    }

    public void setPreset(ImagePreset imagePreset) {
        if (this.mPreset == null || !this.mPreset.same(imagePreset)) {
            this.mPreset = new ImagePreset(imagePreset);
        } else {
            this.mPreset.updateWith(imagePreset);
        }
    }

    public void remove() {
        if (MasterImage.getImage().getBitmapCache().cache(this.mBitmap)) {
            this.mBitmap = null;
        }
    }
}
