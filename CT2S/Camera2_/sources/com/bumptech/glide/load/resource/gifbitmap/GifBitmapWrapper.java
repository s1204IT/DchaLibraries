package com.bumptech.glide.load.resource.gifbitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.gif.GifData;

public class GifBitmapWrapper {
    private final Resource<Bitmap> bitmapResource;
    private final Resource<GifData> gifResource;

    public GifBitmapWrapper(Resource<Bitmap> bitmapResource, Resource<GifData> gifResource) {
        if (bitmapResource != null && gifResource != null) {
            throw new IllegalArgumentException("Can only contain either a bitmap resource or a gif resource, not both");
        }
        if (bitmapResource == null && gifResource == null) {
            throw new IllegalArgumentException("Must contain either a bitmap resource or a gif resource");
        }
        this.bitmapResource = bitmapResource;
        this.gifResource = gifResource;
    }

    public int getSize() {
        return this.bitmapResource != null ? this.bitmapResource.getSize() : this.gifResource.getSize();
    }

    public Resource<Bitmap> getBitmapResource() {
        return this.bitmapResource;
    }

    public Resource<GifData> getGifResource() {
        return this.gifResource;
    }
}
