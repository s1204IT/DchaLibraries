package com.bumptech.glide.load.engine;

import android.os.Handler;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.CacheLoader;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

class DefaultResourceRunnerFactory implements ResourceRunnerFactory {
    private final CacheLoader cacheLoader;
    private DiskCache diskCache;
    private ExecutorService diskCacheService;
    private Handler mainHandler;
    private ExecutorService service;

    public DefaultResourceRunnerFactory(DiskCache diskCache, Handler mainHandler, ExecutorService diskCacheService, ExecutorService resizeService) {
        this.diskCache = diskCache;
        this.mainHandler = mainHandler;
        this.diskCacheService = diskCacheService;
        this.service = resizeService;
        this.cacheLoader = new CacheLoader(diskCache);
    }

    @Override
    public <T, Z, R> ResourceRunner<Z, R> build(EngineKey key, int width, int height, ResourceDecoder<InputStream, Z> cacheDecoder, DataFetcher<T> fetcher, boolean cacheSource, Encoder<T> sourceEncoder, ResourceDecoder<T, Z> decoder, Transformation<Z> transformation, ResourceEncoder<Z> encoder, ResourceTranscoder<Z, R> transcoder, Priority priority, boolean isMemoryCacheable, EngineJobListener listener) {
        EngineJob engineJob = new EngineJob(key, this.mainHandler, isMemoryCacheable, listener);
        SourceResourceRunner<T, Z, R> sourceRunner = new SourceResourceRunner<>(key, width, height, this.cacheLoader, cacheDecoder, fetcher, cacheSource, sourceEncoder, decoder, transformation, encoder, transcoder, this.diskCache, priority, engineJob);
        return new ResourceRunner<>(key, width, height, this.cacheLoader, cacheDecoder, transformation, transcoder, sourceRunner, this.diskCacheService, this.service, engineJob, priority);
    }
}
