package com.bumptech.glide.load.engine;

import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;
import android.util.Log;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.util.LogTime;
import java.io.InputStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class Engine implements EngineJobListener, MemoryCache.ResourceRemovedListener, Resource.ResourceListener {
    private static final String TAG = "Engine";
    private final Map<Key, WeakReference<Resource>> activeResources;
    private final MemoryCache cache;
    private final ResourceRunnerFactory factory;
    private final EngineKeyFactory keyFactory;
    private final ReferenceQueue<Resource> resourceReferenceQueue;
    private final Map<Key, ResourceRunner> runners;

    public static class LoadStatus {
        private final ResourceCallback cb;
        private final EngineJob engineJob;

        public LoadStatus(ResourceCallback cb, EngineJob engineJob) {
            this.cb = cb;
            this.engineJob = engineJob;
        }

        public void cancel() {
            this.engineJob.removeCallback(this.cb);
        }
    }

    public Engine(MemoryCache memoryCache, DiskCache diskCache, ExecutorService resizeService, ExecutorService diskCacheService) {
        this(null, memoryCache, diskCache, resizeService, diskCacheService, null, null, null);
    }

    Engine(ResourceRunnerFactory factory, MemoryCache cache, DiskCache diskCache, ExecutorService resizeService, ExecutorService diskCacheService, Map<Key, ResourceRunner> runners, EngineKeyFactory keyFactory, Map<Key, WeakReference<Resource>> activeResources) {
        this.cache = cache;
        activeResources = activeResources == null ? new HashMap<>() : activeResources;
        this.activeResources = activeResources;
        this.keyFactory = keyFactory == null ? new EngineKeyFactory() : keyFactory;
        this.runners = runners == null ? new HashMap<>() : runners;
        this.factory = factory == null ? new DefaultResourceRunnerFactory(diskCache, new Handler(Looper.getMainLooper()), diskCacheService, resizeService) : factory;
        this.resourceReferenceQueue = new ReferenceQueue<>();
        MessageQueue queue = Looper.myQueue();
        queue.addIdleHandler(new RefQueueIdleHandler(activeResources, this.resourceReferenceQueue));
        cache.setResourceRemovedListener(this);
    }

    public <T, Z, R> LoadStatus load(int width, int height, ResourceDecoder<InputStream, Z> cacheDecoder, DataFetcher<T> fetcher, boolean cacheSource, Encoder<T> sourceEncoder, ResourceDecoder<T, Z> decoder, Transformation<Z> transformation, ResourceEncoder<Z> encoder, ResourceTranscoder<Z, R> transcoder, Priority priority, boolean isMemoryCacheable, ResourceCallback cb) {
        long startTime = LogTime.getLogTime();
        String id = fetcher.getId();
        EngineKey key = this.keyFactory.buildKey(id, width, height, cacheDecoder, decoder, transformation, encoder, transcoder, sourceEncoder);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "loading: " + key);
        }
        Resource cached = this.cache.remove(key);
        if (cached != null) {
            cached.acquire(1);
            this.activeResources.put(key, new ResourceWeakReference(key, cached, this.resourceReferenceQueue));
            cb.onResourceReady(cached);
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "loaded resource from cache in " + LogTime.getElapsedMillis(startTime));
            }
            return null;
        }
        WeakReference<Resource> activeRef = this.activeResources.get(key);
        if (activeRef != null) {
            Resource active = activeRef.get();
            if (active != null) {
                active.acquire(1);
                cb.onResourceReady(active);
                if (Log.isLoggable(TAG, 2)) {
                    Log.v(TAG, "loaded resource from active resources in " + LogTime.getElapsedMillis(startTime));
                }
                return null;
            }
            this.activeResources.remove(key);
        }
        ResourceRunner current = this.runners.get(key);
        if (current != null) {
            EngineJob job = current.getJob();
            job.addCallback(cb);
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "added to existing load in " + LogTime.getElapsedMillis(startTime));
            }
            return new LoadStatus(cb, job);
        }
        long start = LogTime.getLogTime();
        ResourceRunner<Z, R> runner = this.factory.build(key, width, height, cacheDecoder, fetcher, cacheSource, sourceEncoder, decoder, transformation, encoder, transcoder, priority, isMemoryCacheable, this);
        runner.getJob().addCallback(cb);
        this.runners.put(key, runner);
        runner.queue();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "queued new load in " + LogTime.getElapsedMillis(start));
            Log.v(TAG, "finished load in engine in " + LogTime.getElapsedMillis(startTime));
        }
        return new LoadStatus(cb, runner.getJob());
    }

    @Override
    public void onEngineJobComplete(Key key, Resource resource) {
        if (resource != null) {
            resource.setResourceListener(key, this);
            this.activeResources.put(key, new ResourceWeakReference(key, resource, this.resourceReferenceQueue));
        }
        this.runners.remove(key);
    }

    @Override
    public void onEngineJobCancelled(Key key) {
        ResourceRunner runner = this.runners.remove(key);
        runner.cancel();
    }

    @Override
    public void onResourceRemoved(Resource resource) {
        resource.recycle();
    }

    @Override
    public void onResourceReleased(Key cacheKey, Resource resource) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "released: " + cacheKey);
        }
        this.activeResources.remove(cacheKey);
        if (resource.isCacheable()) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "recaching: " + cacheKey);
            }
            this.cache.put(cacheKey, resource);
        } else {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "recycling: " + cacheKey);
            }
            resource.recycle();
        }
    }

    private static class ResourceWeakReference extends WeakReference<Resource> {
        public final Key key;
        public final Object resource;

        public ResourceWeakReference(Key key, Resource r, ReferenceQueue<? super Resource> q) {
            super(r, q);
            this.key = key;
            this.resource = r.get();
        }
    }

    private static class RefQueueIdleHandler implements MessageQueue.IdleHandler {
        private Map<Key, WeakReference<Resource>> activeResources;
        private ReferenceQueue<Resource> queue;

        public RefQueueIdleHandler(Map<Key, WeakReference<Resource>> activeResources, ReferenceQueue<Resource> queue) {
            this.activeResources = activeResources;
            this.queue = queue;
        }

        @Override
        public boolean queueIdle() {
            ResourceWeakReference ref = (ResourceWeakReference) this.queue.poll();
            if (ref != null) {
                this.activeResources.remove(ref.key);
                if (Log.isLoggable(Engine.TAG, 3)) {
                    Log.d(Engine.TAG, "Maybe leaked a resource: " + ref.resource);
                    return true;
                }
                return true;
            }
            return true;
        }
    }
}
