package com.bumptech.glide.load.engine;

import android.os.SystemClock;
import android.util.Log;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.CacheLoader;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.executor.Prioritized;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ResourceRunner<Z, R> implements Runnable, Prioritized {
    private static final String TAG = "ResourceRunner";
    private final ResourceDecoder<InputStream, Z> cacheDecoder;
    private final CacheLoader cacheLoader;
    private final ExecutorService diskCacheService;
    private volatile Future<?> future;
    private final int height;
    private volatile boolean isCancelled;
    private final EngineJob job;
    private final EngineKey key;
    private final Priority priority;
    private final ExecutorService resizeService;
    private final SourceResourceRunner sourceRunner;
    private final ResourceTranscoder<Z, R> transcoder;
    private final Transformation<Z> transformation;
    private final int width;

    public ResourceRunner(EngineKey key, int width, int height, CacheLoader cacheLoader, ResourceDecoder<InputStream, Z> cacheDecoder, Transformation<Z> transformation, ResourceTranscoder<Z, R> transcoder, SourceResourceRunner sourceRunner, ExecutorService diskCacheService, ExecutorService resizeService, EngineJob job, Priority priority) {
        this.key = key;
        this.width = width;
        this.height = height;
        this.cacheLoader = cacheLoader;
        this.cacheDecoder = cacheDecoder;
        this.transformation = transformation;
        this.transcoder = transcoder;
        this.sourceRunner = sourceRunner;
        this.diskCacheService = diskCacheService;
        this.resizeService = resizeService;
        this.job = job;
        this.priority = priority;
    }

    public EngineJob getJob() {
        return this.job;
    }

    public void cancel() {
        this.isCancelled = true;
        if (this.future != null) {
            this.future.cancel(false);
        }
        this.sourceRunner.cancel();
    }

    public void queue() {
        this.future = this.diskCacheService.submit(this);
    }

    @Override
    public void run() {
        if (!this.isCancelled) {
            long start = SystemClock.currentThreadTimeMillis();
            Resource<Z> fromCache = this.cacheLoader.load(this.key, this.cacheDecoder, this.width, this.height);
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "loaded from disk cache in " + (SystemClock.currentThreadTimeMillis() - start));
            }
            if (fromCache != null) {
                Resource<Z> transformed = this.transformation.transform(fromCache, this.width, this.height);
                if (transformed != fromCache) {
                    fromCache.recycle();
                }
                Resource<R> transcoded = this.transcoder.transcode(transformed);
                this.job.onResourceReady(transcoded);
                return;
            }
            this.future = this.resizeService.submit(this.sourceRunner);
        }
    }

    @Override
    public int getPriority() {
        return this.priority.ordinal();
    }
}
