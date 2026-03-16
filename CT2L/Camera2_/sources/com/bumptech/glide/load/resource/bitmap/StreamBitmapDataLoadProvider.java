package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.DataLoadProvider;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.model.StreamEncoder;
import java.io.InputStream;

public class StreamBitmapDataLoadProvider implements DataLoadProvider<InputStream, Bitmap> {
    private final StreamBitmapDecoder decoder;
    private final StreamEncoder sourceEncoder = new StreamEncoder();
    private final BitmapEncoder encoder = new BitmapEncoder();

    public StreamBitmapDataLoadProvider(BitmapPool bitmapPool) {
        this.decoder = new StreamBitmapDecoder(bitmapPool);
    }

    @Override
    public ResourceDecoder<InputStream, Bitmap> getCacheDecoder() {
        return this.decoder;
    }

    @Override
    public ResourceDecoder<InputStream, Bitmap> getSourceDecoder() {
        return this.decoder;
    }

    @Override
    public Encoder<InputStream> getSourceEncoder() {
        return this.sourceEncoder;
    }

    @Override
    public ResourceEncoder<Bitmap> getEncoder() {
        return this.encoder;
    }
}
