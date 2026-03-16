package com.bumptech.glide.load.resource.transcode;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.gif.GifData;
import com.bumptech.glide.load.resource.gifbitmap.GifBitmapWrapper;

public class GifBitmapWrapperDrawableTranscoder implements ResourceTranscoder<GifBitmapWrapper, Drawable> {
    private final ResourceTranscoder<Bitmap, ? extends Drawable> bitmapDrawableResourceTranscoder;
    private final ResourceTranscoder<GifData, ? extends Drawable> gifDrawableResourceTranscoder;

    public GifBitmapWrapperDrawableTranscoder(ResourceTranscoder<Bitmap, ? extends Drawable> bitmapDrawableResourceTranscoder, ResourceTranscoder<GifData, ? extends Drawable> gifDrawableResourceTranscoder) {
        this.bitmapDrawableResourceTranscoder = bitmapDrawableResourceTranscoder;
        this.gifDrawableResourceTranscoder = gifDrawableResourceTranscoder;
    }

    @Override
    public Resource<Drawable> transcode(Resource<GifBitmapWrapper> toTranscode) {
        GifBitmapWrapper gifBitmap = toTranscode.get();
        Resource<Bitmap> bitmapResource = gifBitmap.getBitmapResource();
        if (bitmapResource != null) {
            return this.bitmapDrawableResourceTranscoder.transcode(bitmapResource);
        }
        return this.gifDrawableResourceTranscoder.transcode(gifBitmap.getGifResource());
    }

    @Override
    public String getId() {
        return "GifBitmapWrapperDrawableTranscoder.com.bumptech.glide.load.resource.transcode";
    }
}
