package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import com.bumptech.glide.DataLoadProvider;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.model.StreamEncoder;
import java.io.InputStream;

public class GifDataLoadProvider implements DataLoadProvider<InputStream, GifData> {
    private final GifResourceDecoder decoder;
    private final GifResourceEncoder encoder = new GifResourceEncoder();
    private final StreamEncoder sourceEncoder = new StreamEncoder();

    public GifDataLoadProvider(Context context, BitmapPool bitmapPool) {
        this.decoder = new GifResourceDecoder(context, bitmapPool);
    }

    @Override
    public ResourceDecoder<InputStream, GifData> getCacheDecoder() {
        return this.decoder;
    }

    @Override
    public ResourceDecoder<InputStream, GifData> getSourceDecoder() {
        return this.decoder;
    }

    @Override
    public Encoder<InputStream> getSourceEncoder() {
        return this.sourceEncoder;
    }

    @Override
    public ResourceEncoder<GifData> getEncoder() {
        return this.encoder;
    }
}
