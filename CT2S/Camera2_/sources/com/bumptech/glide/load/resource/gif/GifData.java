package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.UnitTransformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import java.util.ArrayList;
import java.util.List;

public class GifData {
    private final GifDecoderBitmapProvider bitmapProvider;
    private final Context context;
    private final byte[] data;
    private final List<GifDrawable> drawables = new ArrayList();
    private Transformation<Bitmap> frameTransformation;
    private final String gifId;
    private final GifHeader header;
    private final int targetHeight;
    private final int targetWidth;

    public GifData(Context context, BitmapPool bitmapPool, String gifId, GifHeader header, byte[] data, int targetWidth, int targetHeight) {
        this.context = context;
        this.header = header;
        this.data = data;
        this.gifId = gifId;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.bitmapProvider = new GifDecoderBitmapProvider(bitmapPool);
    }

    public Transformation<Bitmap> getFrameTransformation() {
        return this.frameTransformation != null ? this.frameTransformation : UnitTransformation.get();
    }

    public void setFrameTransformation(Transformation<Bitmap> transformation) {
        this.frameTransformation = transformation;
    }

    public int getByteSize() {
        return this.data.length;
    }

    public byte[] getData() {
        return this.data;
    }

    public GifDrawable getDrawable() {
        GifDecoder gifDecoder = new GifDecoder(this.bitmapProvider);
        gifDecoder.setData(this.gifId, this.header, this.data);
        GifFrameManager frameManager = new GifFrameManager(this.context, gifDecoder, getFrameTransformation(), this.targetWidth, this.targetHeight);
        GifDrawable result = new GifDrawable(gifDecoder, frameManager);
        this.drawables.add(result);
        return result;
    }

    public void recycle() {
        for (GifDrawable drawable : this.drawables) {
            drawable.stop();
            drawable.recycle();
        }
    }

    private static class GifDecoderBitmapProvider implements GifDecoder.BitmapProvider {
        private BitmapPool bitmapPool;

        public GifDecoderBitmapProvider(BitmapPool bitmapPool) {
            this.bitmapPool = bitmapPool;
        }

        @Override
        public Bitmap obtain(int width, int height, Bitmap.Config config) {
            return this.bitmapPool.get(width, height, config);
        }
    }
}
