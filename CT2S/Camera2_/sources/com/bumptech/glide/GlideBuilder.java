package com.bumptech.glide;

import android.content.Context;
import android.os.Build;
import com.android.volley.RequestQueue;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskCacheAdapter;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.load.engine.executor.FifoPriorityThreadPoolExecutor;
import com.bumptech.glide.volley.RequestQueueWrapper;
import java.io.File;
import java.util.concurrent.ExecutorService;

public class GlideBuilder {
    private BitmapPool bitmapPool;
    private Context context;
    private DiskCache diskCache;
    private ExecutorService diskCacheService;
    private Engine engine;
    private MemoryCache memoryCache;
    private RequestQueue requestQueue;
    private ExecutorService resizeService;

    public GlideBuilder(Context context) {
        this.context = context.getApplicationContext();
    }

    public GlideBuilder setRequestQueue(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
        return this;
    }

    public GlideBuilder setBitmapPool(BitmapPool bitmapPool) {
        this.bitmapPool = bitmapPool;
        return this;
    }

    public GlideBuilder setMemoryCache(MemoryCache memoryCache) {
        this.memoryCache = memoryCache;
        return this;
    }

    public GlideBuilder setDiskCache(DiskCache diskCache) {
        this.diskCache = diskCache;
        return this;
    }

    public GlideBuilder setResizeService(ExecutorService service) {
        this.resizeService = service;
        return this;
    }

    public GlideBuilder setDiskCacheService(ExecutorService service) {
        this.diskCacheService = service;
        return this;
    }

    GlideBuilder setEngine(Engine engine) {
        this.engine = engine;
        return this;
    }

    Glide createGlide() {
        if (this.resizeService == null) {
            int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
            this.resizeService = new FifoPriorityThreadPoolExecutor(cores);
        }
        if (this.diskCacheService == null) {
            this.diskCacheService = new FifoPriorityThreadPoolExecutor(1);
        }
        if (this.requestQueue == null) {
            this.requestQueue = RequestQueueWrapper.getRequestQueue(this.context);
        }
        MemorySizeCalculator calculator = new MemorySizeCalculator(this.context);
        if (this.bitmapPool == null) {
            if (Build.VERSION.SDK_INT >= 11) {
                this.bitmapPool = new LruBitmapPool(calculator.getBitmapPoolSize());
            } else {
                this.bitmapPool = new BitmapPoolAdapter();
            }
        }
        if (this.memoryCache == null) {
            this.memoryCache = new LruResourceCache(calculator.getMemoryCacheSize());
        }
        if (this.diskCache == null) {
            File cacheDir = Glide.getPhotoCacheDir(this.context);
            if (cacheDir != null) {
                this.diskCache = DiskLruCacheWrapper.get(cacheDir, 262144000);
            }
            if (this.diskCache == null) {
                this.diskCache = new DiskCacheAdapter();
            }
        }
        if (this.engine == null) {
            this.engine = new Engine(this.memoryCache, this.diskCache, this.resizeService, this.diskCacheService);
        }
        return new Glide(this.engine, this.requestQueue, this.memoryCache, this.bitmapPool, this.context);
    }
}
