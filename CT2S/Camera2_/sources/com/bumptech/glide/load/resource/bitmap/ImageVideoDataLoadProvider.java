package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.DataLoadProvider;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.model.ImageVideoWrapperEncoder;
import com.bumptech.glide.load.model.NullEncoder;
import com.bumptech.glide.load.model.StreamEncoder;
import java.io.InputStream;

public class ImageVideoDataLoadProvider implements DataLoadProvider<ImageVideoWrapper, Bitmap> {
    private final StreamBitmapDecoder cacheDecoder;
    private final BitmapEncoder encoder = new BitmapEncoder();
    private final ImageVideoBitmapDecoder sourceDecoder;
    private final ImageVideoWrapperEncoder sourceEncoder;

    public ImageVideoDataLoadProvider(BitmapPool bitmapPool) {
        Encoder<ParcelFileDescriptor> fileDescriptorEncoder = NullEncoder.get();
        this.sourceEncoder = new ImageVideoWrapperEncoder(new StreamEncoder(), fileDescriptorEncoder);
        this.cacheDecoder = new StreamBitmapDecoder(bitmapPool);
        this.sourceDecoder = new ImageVideoBitmapDecoder(this.cacheDecoder, new FileDescriptorBitmapDecoder(bitmapPool));
    }

    @Override
    public ResourceDecoder<InputStream, Bitmap> getCacheDecoder() {
        return this.cacheDecoder;
    }

    @Override
    public ResourceDecoder<ImageVideoWrapper, Bitmap> getSourceDecoder() {
        return this.sourceDecoder;
    }

    @Override
    public Encoder<ImageVideoWrapper> getSourceEncoder() {
        return this.sourceEncoder;
    }

    @Override
    public ResourceEncoder<Bitmap> getEncoder() {
        return this.encoder;
    }
}
