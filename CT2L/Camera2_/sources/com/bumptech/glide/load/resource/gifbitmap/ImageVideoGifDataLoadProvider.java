package com.bumptech.glide.load.resource.gifbitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.DataLoadProvider;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.model.NullEncoder;
import com.bumptech.glide.load.resource.gif.GifData;
import java.io.InputStream;

public class ImageVideoGifDataLoadProvider implements DataLoadProvider<ImageVideoWrapper, GifBitmapWrapper> {
    private final GifBitmapWrapperStreamResourceDecoder cacheDecoder;
    private final GifBitmapWrapperResourceEncoder encoder;
    private final GifBitmapWrapperResourceDecoder sourceDecoder;
    private final Encoder<ImageVideoWrapper> sourceEncoder;

    public ImageVideoGifDataLoadProvider(DataLoadProvider<ImageVideoWrapper, Bitmap> bitmapProvider, DataLoadProvider<InputStream, GifData> gifProvider) {
        this.cacheDecoder = new GifBitmapWrapperStreamResourceDecoder(new GifBitmapWrapperResourceDecoder(bitmapProvider.getSourceDecoder(), gifProvider.getCacheDecoder()));
        this.sourceDecoder = new GifBitmapWrapperResourceDecoder(bitmapProvider.getSourceDecoder(), gifProvider.getSourceDecoder());
        this.encoder = new GifBitmapWrapperResourceEncoder(bitmapProvider.getEncoder(), gifProvider.getEncoder());
        NullEncoder.get();
        this.sourceEncoder = bitmapProvider.getSourceEncoder();
    }

    @Override
    public ResourceDecoder<InputStream, GifBitmapWrapper> getCacheDecoder() {
        return this.cacheDecoder;
    }

    @Override
    public ResourceDecoder<ImageVideoWrapper, GifBitmapWrapper> getSourceDecoder() {
        return this.sourceDecoder;
    }

    @Override
    public Encoder<ImageVideoWrapper> getSourceEncoder() {
        return this.sourceEncoder;
    }

    @Override
    public ResourceEncoder<GifBitmapWrapper> getEncoder() {
        return this.encoder;
    }
}
