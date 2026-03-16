package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.bumptech.glide.Glide;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.SkipCache;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.load.model.NullEncoder;
import com.bumptech.glide.load.resource.NullDecoder;
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.request.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import java.io.InputStream;

class GifFrameManager {
    static final long MIN_FRAME_DELAY = 16;
    private final ResourceDecoder<InputStream, Bitmap> cacheDecoder;
    private final MemorySizeCalculator calculator;
    private final Context context;
    private DelayTarget current;
    private final GifDecoder decoder;
    private final ResourceEncoder<Bitmap> encoder;
    private final GifFrameModelLoader frameLoader;
    private final GifFrameResourceDecoder frameResourceDecoder;
    private int frameSize;
    private final Handler mainHandler;
    private DelayTarget next;
    private final Encoder<GifDecoder> sourceEncoder;
    private final int targetHeight;
    private final int targetWidth;
    private Transformation<Bitmap> transformation;

    public interface FrameCallback {
        void onFrameRead(Bitmap bitmap);
    }

    public GifFrameManager(Context context, GifDecoder decoder, Transformation<Bitmap> transformation, int targetWidth, int targetHeight) {
        this(context, Glide.get(context).getBitmapPool(), decoder, new Handler(Looper.getMainLooper()), transformation, targetWidth, targetHeight);
    }

    public GifFrameManager(Context context, BitmapPool bitmapPool, GifDecoder decoder, Handler mainHandler, Transformation<Bitmap> transformation, int targetWidth, int targetHeight) {
        this.frameSize = -1;
        this.context = context;
        this.decoder = decoder;
        this.mainHandler = mainHandler;
        this.transformation = transformation;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.calculator = new MemorySizeCalculator(context);
        this.frameLoader = new GifFrameModelLoader();
        this.frameResourceDecoder = new GifFrameResourceDecoder(bitmapPool);
        this.sourceEncoder = NullEncoder.get();
        if (!decoder.isTransparent()) {
            this.cacheDecoder = new StreamBitmapDecoder(context);
            this.encoder = new BitmapEncoder(Bitmap.CompressFormat.JPEG, 70);
        } else {
            this.cacheDecoder = NullDecoder.get();
            this.encoder = SkipCache.get();
        }
    }

    Transformation<Bitmap> getTransformation() {
        return this.transformation;
    }

    private int getEstimatedTotalFrameSize() {
        return this.frameSize == -1 ? this.decoder.getDecodedFramesByteSizeSum() : this.frameSize * this.decoder.getFrameCount();
    }

    public void getNextFrame(FrameCallback cb) {
        this.decoder.advance();
        boolean skipCache = getEstimatedTotalFrameSize() > this.calculator.getMemoryCacheSize() / 2;
        long targetTime = SystemClock.uptimeMillis() + Math.max(16L, this.decoder.getNextDelay());
        this.next = new DelayTarget(cb, targetTime);
        Glide.with(this.context).using(this.frameLoader, GifDecoder.class).load(this.decoder).as(Bitmap.class).decoder(this.frameResourceDecoder).cacheDecoder(this.cacheDecoder).transform(this.transformation).encoder(this.encoder).sourceEncoder(this.sourceEncoder).skipMemoryCache(skipCache).into(this.next);
    }

    public void clear() {
        if (this.current != null) {
            Glide.clear(this.current);
            this.mainHandler.removeCallbacks(this.current);
        }
        if (this.next != null) {
            Glide.clear(this.next);
            this.mainHandler.removeCallbacks(this.next);
        }
    }

    class DelayTarget extends SimpleTarget<Bitmap> implements Runnable {
        private FrameCallback cb;
        private Bitmap resource;
        private long targetTime;

        @Override
        public void onResourceReady(Object obj, GlideAnimation glideAnimation) {
            onResourceReady((Bitmap) obj, (GlideAnimation<Bitmap>) glideAnimation);
        }

        public DelayTarget(FrameCallback cb, long targetTime) {
            super(GifFrameManager.this.targetWidth, GifFrameManager.this.targetHeight);
            this.cb = cb;
            this.targetTime = targetTime;
        }

        public void onResourceReady(Bitmap resource, GlideAnimation<Bitmap> glideAnimation) {
            GifFrameManager.this.frameSize = resource.getHeight() * resource.getRowBytes();
            this.resource = resource;
            GifFrameManager.this.mainHandler.postAtTime(this, this.targetTime);
            if (GifFrameManager.this.current != null) {
                Glide.clear(GifFrameManager.this.current);
            }
            GifFrameManager.this.current = GifFrameManager.this.next;
            GifFrameManager.this.next = null;
        }

        @Override
        public void run() {
            this.cb.onFrameRead(this.resource);
        }
    }
}
