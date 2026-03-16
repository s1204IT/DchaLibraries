package com.bumptech.glide.load.resource.gif;

import com.bumptech.glide.load.engine.Resource;

public class GifDrawableResource extends Resource<GifDrawable> {
    private Resource<GifData> wrapped;

    public GifDrawableResource(Resource<GifData> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public GifDrawable get() {
        return this.wrapped.get().getDrawable();
    }

    @Override
    public int getSize() {
        return this.wrapped.getSize();
    }

    @Override
    protected void recycleInternal() {
        this.wrapped.recycle();
    }
}
