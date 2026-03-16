package com.bumptech.glide.load.engine;

import android.os.SystemClock;
import android.util.Log;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.CacheLoader;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.executor.Prioritized;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.request.ResourceCallback;
import java.io.InputStream;
import java.io.OutputStream;

public class SourceResourceRunner<T, Z, R> implements Runnable, DiskCache.Writer, Prioritized {
    private static final String TAG = "SourceRunner";
    private final ResourceDecoder<InputStream, Z> cacheDecoder;
    private final CacheLoader cacheLoader;
    private final boolean cacheSource;
    private final ResourceCallback cb;
    private final ResourceDecoder<T, Z> decoder;
    private final DiskCache diskCache;
    private final ResourceEncoder<Z> encoder;
    private final DataFetcher<T> fetcher;
    private final int height;
    private volatile boolean isCancelled;
    private final EngineKey key;
    private final Priority priority;
    private Resource<Z> result;
    private final Encoder<T> sourceEncoder;
    private final ResourceTranscoder<Z, R> transcoder;
    private final Transformation<Z> transformation;
    private final int width;

    public SourceResourceRunner(EngineKey key, int width, int height, CacheLoader cacheLoader, ResourceDecoder<InputStream, Z> cacheDecoder, DataFetcher<T> dataFetcher, boolean cacheSource, Encoder<T> sourceEncoder, ResourceDecoder<T, Z> decoder, Transformation<Z> transformation, ResourceEncoder<Z> encoder, ResourceTranscoder<Z, R> transcoder, DiskCache diskCache, Priority priority, ResourceCallback cb) {
        this.key = key;
        this.width = width;
        this.height = height;
        this.cacheLoader = cacheLoader;
        this.cacheDecoder = cacheDecoder;
        this.fetcher = dataFetcher;
        this.cacheSource = cacheSource;
        this.sourceEncoder = sourceEncoder;
        this.decoder = decoder;
        this.transformation = transformation;
        this.encoder = encoder;
        this.transcoder = transcoder;
        this.diskCache = diskCache;
        this.priority = priority;
        this.cb = cb;
    }

    public void cancel() {
        this.isCancelled = true;
        if (this.fetcher != null) {
            this.fetcher.cancel();
        }
    }

    @Override
    public void run() {
        if (!this.isCancelled) {
            try {
                long start = SystemClock.currentThreadTimeMillis();
                Resource<Z> decoded = this.cacheLoader.load(this.key.getOriginalKey(), this.cacheDecoder, this.width, this.height);
                if (decoded == null) {
                    decoded = decodeFromSource();
                    if (Log.isLoggable(TAG, 2)) {
                        Log.v(TAG, "Decoded from source in " + (SystemClock.currentThreadTimeMillis() - start) + " cache");
                        start = SystemClock.currentThreadTimeMillis();
                    }
                }
                if (decoded != null) {
                    Resource<Z> transformed = this.transformation.transform(decoded, this.width, this.height);
                    if (decoded != transformed) {
                        decoded.recycle();
                    }
                    this.result = transformed;
                }
                if (Log.isLoggable(TAG, 2)) {
                    Log.v(TAG, "transformed in " + (SystemClock.currentThreadTimeMillis() - start));
                }
                if (this.result != null) {
                    this.diskCache.put(this.key, this);
                    long start2 = SystemClock.currentThreadTimeMillis();
                    Resource<R> transcoded = this.transcoder.transcode(this.result);
                    if (Log.isLoggable(TAG, 2)) {
                        Log.d(TAG, "transcoded in " + (SystemClock.currentThreadTimeMillis() - start2));
                    }
                    this.cb.onResourceReady(transcoded);
                    return;
                }
                this.cb.onException(null);
            } catch (Exception e) {
                this.cb.onException(e);
            }
        }
    }

    private Resource<Z> encodeSourceAndDecodeFromCache(final T data) {
        this.diskCache.put(this.key.getOriginalKey(), new DiskCache.Writer() {
            @Override
            public boolean write(OutputStream os) {
                return SourceResourceRunner.this.sourceEncoder.encode(data, os);
            }
        });
        return this.cacheLoader.load(this.key.getOriginalKey(), this.cacheDecoder, this.width, this.height);
    }

    private Resource<Z> decodeFromSource() throws Exception {
        try {
            T data = this.fetcher.loadData(this.priority);
            if (data != null) {
                if (this.cacheSource) {
                    return encodeSourceAndDecodeFromCache(data);
                }
                return this.decoder.decode(data, this.width, this.height);
            }
            this.fetcher.cleanup();
            return null;
        } finally {
            this.fetcher.cleanup();
        }
    }

    @Override
    public boolean write(OutputStream outputStream) {
        long jCurrentThreadTimeMillis = SystemClock.currentThreadTimeMillis();
        boolean zEncode = this.encoder.encode((Z) this.result, outputStream);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "wrote to disk cache in " + (SystemClock.currentThreadTimeMillis() - jCurrentThreadTimeMillis));
        }
        return zEncode;
    }

    @Override
    public int getPriority() {
        return this.priority.ordinal();
    }
}
