package com.bumptech.glide.load.resource.gifbitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.gif.GifData;
import com.bumptech.glide.load.resource.gif.GifDataTransformation;

public class GifBitmapWrapperTransformation implements Transformation<GifBitmapWrapper> {
    private Transformation<Bitmap> bitmapTransformation;
    private Transformation<GifData> gifDataTransformation;

    public GifBitmapWrapperTransformation(Transformation<Bitmap> bitmapTransformation) {
        this(bitmapTransformation, new GifDataTransformation(bitmapTransformation));
    }

    GifBitmapWrapperTransformation(Transformation<Bitmap> bitmapTransformation, Transformation<GifData> gifDataTransformation) {
        this.bitmapTransformation = bitmapTransformation;
        this.gifDataTransformation = gifDataTransformation;
    }

    @Override
    public Resource<GifBitmapWrapper> transform(Resource<GifBitmapWrapper> resource, int outWidth, int outHeight) {
        Resource<GifData> transformed;
        Resource<Bitmap> bitmapResource = resource.get().getBitmapResource();
        Resource<GifData> gifResource = resource.get().getGifResource();
        if (bitmapResource != null && this.bitmapTransformation != null) {
            Resource<Bitmap> transformed2 = this.bitmapTransformation.transform(bitmapResource, outWidth, outHeight);
            if (transformed2 != bitmapResource) {
                GifBitmapWrapper gifBitmap = new GifBitmapWrapper(transformed2, resource.get().getGifResource());
                return new GifBitmapWrapperResource(gifBitmap);
            }
            return resource;
        }
        if (gifResource != null && this.gifDataTransformation != null && (transformed = this.gifDataTransformation.transform(gifResource, outWidth, outHeight)) != gifResource) {
            GifBitmapWrapper gifBitmap2 = new GifBitmapWrapper(resource.get().getBitmapResource(), transformed);
            return new GifBitmapWrapperResource(gifBitmap2);
        }
        return resource;
    }

    @Override
    public String getId() {
        return this.bitmapTransformation.getId();
    }
}
