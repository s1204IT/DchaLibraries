package com.bumptech.glide.load.resource.gifbitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.gif.GifData;
import java.io.OutputStream;

public class GifBitmapWrapperResourceEncoder implements ResourceEncoder<GifBitmapWrapper> {
    private final ResourceEncoder<Bitmap> bitmapEncoder;
    private final ResourceEncoder<GifData> gifEncoder;
    private String id;

    public GifBitmapWrapperResourceEncoder(ResourceEncoder<Bitmap> bitmapEncoder, ResourceEncoder<GifData> gifEncoder) {
        this.bitmapEncoder = bitmapEncoder;
        this.gifEncoder = gifEncoder;
    }

    @Override
    public boolean encode(Resource<GifBitmapWrapper> resource, OutputStream os) {
        GifBitmapWrapper gifBitmap = resource.get();
        Resource<Bitmap> bitmapResource = gifBitmap.getBitmapResource();
        return bitmapResource != null ? this.bitmapEncoder.encode(bitmapResource, os) : this.gifEncoder.encode(gifBitmap.getGifResource(), os);
    }

    @Override
    public String getId() {
        if (this.id == null) {
            this.id = this.bitmapEncoder.getId() + this.gifEncoder.getId();
        }
        return this.id;
    }
}
