package com.bumptech.glide.load.resource.bitmap;

import android.graphics.drawable.BitmapDrawable;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Util;

public class BitmapDrawableResource extends Resource<BitmapDrawable> {
    private BitmapPool bitmapPool;
    private BitmapDrawable drawable;

    public BitmapDrawableResource(BitmapDrawable drawable, BitmapPool bitmapPool) {
        this.drawable = drawable;
        this.bitmapPool = bitmapPool;
    }

    @Override
    public BitmapDrawable get() {
        return this.drawable;
    }

    @Override
    public int getSize() {
        return Util.getSize(this.drawable.getBitmap());
    }

    @Override
    protected void recycleInternal() {
        this.bitmapPool.put(this.drawable.getBitmap());
    }
}
