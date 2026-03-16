package com.android.gallery3d.filtershow.pipeline;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.RenderScript;
import com.android.gallery3d.filtershow.cache.BitmapCache;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

public class Buffer {
    private Allocation mAllocation;
    private Bitmap mBitmap;
    private ImagePreset mPreset;
    private boolean mUseAllocation = false;

    public Buffer(Bitmap bitmap) {
        RenderScript rs = CachingPipeline.getRenderScriptContext();
        if (bitmap != null) {
            BitmapCache cache = MasterImage.getImage().getBitmapCache();
            this.mBitmap = cache.getBitmapCopy(bitmap, 1);
        }
        if (this.mUseAllocation) {
            this.mAllocation = Allocation.createFromBitmap(rs, this.mBitmap, Allocation.MipmapControl.MIPMAP_NONE, 129);
        }
    }

    public boolean isSameSize(Bitmap bitmap) {
        return this.mBitmap != null && bitmap != null && this.mBitmap.getWidth() == bitmap.getWidth() && this.mBitmap.getHeight() == bitmap.getHeight();
    }

    public synchronized void useBitmap(Bitmap bitmap) {
        Canvas canvas = new Canvas(this.mBitmap);
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, (Paint) null);
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

    public void setPreset(ImagePreset preset) {
        if (this.mPreset == null || !this.mPreset.same(preset)) {
            this.mPreset = new ImagePreset(preset);
        } else {
            this.mPreset.updateWith(preset);
        }
    }

    public void remove() {
        BitmapCache cache = MasterImage.getImage().getBitmapCache();
        if (cache.cache(this.mBitmap)) {
            this.mBitmap = null;
        }
    }
}
