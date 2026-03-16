package com.bumptech.glide.request;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.LogTime;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Queue;

public class GenericRequest<A, T, Z, R> implements Request, Target.SizeReadyCallback, ResourceCallback {
    private static final String TAG = "GenericRequest";
    private static final Queue<GenericRequest> queue = new ArrayDeque();
    private GlideAnimationFactory<R> animationFactory;
    private boolean cacheSource;
    private Context context;
    private Engine engine;
    private Drawable errorDrawable;
    private int errorResourceId;
    private boolean isCancelled;
    private boolean isError;
    private boolean isMemoryCacheable;
    private boolean isRunning;
    private LoadProvider<A, T, Z, R> loadProvider;
    private Engine.LoadStatus loadStatus;
    private boolean loadedFromMemoryCache;
    private A model;
    private int overrideHeight;
    private int overrideWidth;
    private Drawable placeholderDrawable;
    private int placeholderResourceId;
    private Priority priority;
    private RequestCoordinator requestCoordinator;
    private RequestListener<A, R> requestListener;
    private Resource resource;
    private float sizeMultiplier;
    private long startTime;
    private String tag = String.valueOf(hashCode());
    private Target<R> target;
    private Class<R> transcodeClass;
    private Transformation<Z> transformation;

    public static <A, T, Z, R> GenericRequest<A, T, Z, R> obtain(LoadProvider<A, T, Z, R> loadProvider, A model, Context context, Priority priority, Target<R> target, float sizeMultiplier, Drawable placeholderDrawable, int placeholderResourceId, Drawable errorDrawable, int errorResourceId, RequestListener<A, R> requestListener, RequestCoordinator requestCoordinator, Engine engine, Transformation<Z> transformation, Class<R> transcodeClass, boolean isMemoryCacheable, GlideAnimationFactory<R> animationFactory, int overrideWidth, int overrideHeight, boolean cacheSource) {
        GenericRequest<A, T, Z, R> genericRequestPoll = queue.poll();
        if (genericRequestPoll == null) {
            genericRequestPoll = new GenericRequest<>();
        }
        genericRequestPoll.init(loadProvider, model, context, priority, target, sizeMultiplier, placeholderDrawable, placeholderResourceId, errorDrawable, errorResourceId, requestListener, requestCoordinator, engine, transformation, transcodeClass, isMemoryCacheable, animationFactory, overrideWidth, overrideHeight, cacheSource);
        return genericRequestPoll;
    }

    private GenericRequest() {
    }

    @Override
    public void recycle() {
        this.loadProvider = null;
        this.model = null;
        this.context = null;
        this.target = null;
        this.placeholderDrawable = null;
        this.errorDrawable = null;
        this.requestListener = null;
        this.requestCoordinator = null;
        this.engine = null;
        this.transformation = null;
        this.animationFactory = null;
        this.isCancelled = false;
        this.isError = false;
        this.loadedFromMemoryCache = false;
        this.loadStatus = null;
        this.isRunning = false;
        this.cacheSource = false;
        queue.offer(this);
    }

    private void init(LoadProvider<A, T, Z, R> loadProvider, A model, Context context, Priority priority, Target<R> target, float sizeMultiplier, Drawable placeholderDrawable, int placeholderResourceId, Drawable errorDrawable, int errorResourceId, RequestListener<A, R> requestListener, RequestCoordinator requestCoordinator, Engine engine, Transformation<Z> transformation, Class<R> transcodeClass, boolean isMemoryCacheable, GlideAnimationFactory<R> animationFactory, int overrideWidth, int overrideHeight, boolean cacheSource) {
        this.loadProvider = loadProvider;
        this.model = model;
        this.context = context;
        this.priority = priority;
        this.target = target;
        this.sizeMultiplier = sizeMultiplier;
        this.placeholderDrawable = placeholderDrawable;
        this.placeholderResourceId = placeholderResourceId;
        this.errorDrawable = errorDrawable;
        this.errorResourceId = errorResourceId;
        this.requestListener = requestListener;
        this.requestCoordinator = requestCoordinator;
        this.engine = engine;
        this.transformation = transformation;
        this.transcodeClass = transcodeClass;
        this.isMemoryCacheable = isMemoryCacheable;
        this.animationFactory = animationFactory;
        this.overrideWidth = overrideWidth;
        this.overrideHeight = overrideHeight;
        this.cacheSource = cacheSource;
        if (model != null) {
            if (loadProvider.getCacheDecoder() == null) {
                throw new NullPointerException("CacheDecoder must not be null, try .cacheDecoder(ResouceDecoder)");
            }
            if (loadProvider.getSourceDecoder() == null) {
                throw new NullPointerException("SourceDecoder must not be null, try .imageDecoder(ResourceDecoder) and/or .videoDecoder()");
            }
            if (loadProvider.getEncoder() == null) {
                throw new NullPointerException("Encoder must not be null, try .encode(ResourceEncoder)");
            }
            if (loadProvider.getTranscoder() == null) {
                throw new NullPointerException("Transcoder must not be null, try .as(Class, ResourceTranscoder)");
            }
            if (loadProvider.getModelLoader() == null) {
                throw new NullPointerException("ModelLoader must not be null, try .using(ModelLoader)");
            }
            if (loadProvider.getSourceEncoder() == null) {
                throw new NullPointerException("SourceEncoder must not be null, try .sourceEncoder(Encoder)");
            }
        }
    }

    @Override
    public void run() {
        this.startTime = LogTime.getLogTime();
        if (this.model == null) {
            onException(null);
            return;
        }
        if (this.overrideWidth > 0 && this.overrideHeight > 0) {
            onSizeReady(this.overrideWidth, this.overrideHeight);
        } else {
            this.target.getSize(this);
        }
        if (!isComplete() && !isFailed()) {
            setPlaceHolder();
            this.isRunning = true;
        }
        if (Log.isLoggable(TAG, 2)) {
            logV("finished run method in " + LogTime.getElapsedMillis(this.startTime));
        }
    }

    public void cancel() {
        this.isRunning = false;
        this.isCancelled = true;
        if (this.loadStatus != null) {
            this.loadStatus.cancel();
            this.loadStatus = null;
        }
    }

    @Override
    public void clear() {
        cancel();
        setPlaceHolder();
        if (this.resource != null) {
            this.resource.release();
            this.resource = null;
        }
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    @Override
    public boolean isComplete() {
        return this.resource != null;
    }

    @Override
    public boolean isFailed() {
        return this.isError;
    }

    private void setPlaceHolder() {
        if (canSetPlaceholder()) {
            this.target.setPlaceholder(getPlaceholderDrawable());
        }
    }

    private void setErrorPlaceholder() {
        if (canSetPlaceholder()) {
            Drawable error = getErrorDrawable();
            if (error != null) {
                this.target.setPlaceholder(error);
            } else {
                setPlaceHolder();
            }
        }
    }

    private Drawable getErrorDrawable() {
        if (this.errorDrawable == null && this.errorResourceId > 0) {
            this.errorDrawable = this.context.getResources().getDrawable(this.errorResourceId);
        }
        return this.errorDrawable;
    }

    private Drawable getPlaceholderDrawable() {
        if (this.placeholderDrawable == null && this.placeholderResourceId > 0) {
            this.placeholderDrawable = this.context.getResources().getDrawable(this.placeholderResourceId);
        }
        return this.placeholderDrawable;
    }

    @Override
    public void onSizeReady(int width, int height) {
        if (Log.isLoggable(TAG, 2)) {
            logV("Got onSizeReady in " + LogTime.getElapsedMillis(this.startTime));
        }
        if (!this.isCancelled) {
            int width2 = Math.round(this.sizeMultiplier * width);
            int height2 = Math.round(this.sizeMultiplier * height);
            ResourceDecoder<InputStream, Z> cacheDecoder = this.loadProvider.getCacheDecoder();
            Encoder<T> sourceEncoder = this.loadProvider.getSourceEncoder();
            ResourceDecoder<T, Z> decoder = this.loadProvider.getSourceDecoder();
            ResourceEncoder<Z> encoder = this.loadProvider.getEncoder();
            ResourceTranscoder<Z, R> transcoder = this.loadProvider.getTranscoder();
            ModelLoader<A, T> modelLoader = this.loadProvider.getModelLoader();
            DataFetcher<T> dataFetcher = modelLoader.getResourceFetcher(this.model, width2, height2);
            if (Log.isLoggable(TAG, 2)) {
                logV("finished setup for calling load in " + LogTime.getElapsedMillis(this.startTime));
            }
            this.loadedFromMemoryCache = true;
            this.loadStatus = this.engine.load(width2, height2, cacheDecoder, dataFetcher, this.cacheSource, sourceEncoder, decoder, this.transformation, encoder, transcoder, this.priority, this.isMemoryCacheable, this);
            this.loadedFromMemoryCache = this.resource != null;
            if (Log.isLoggable(TAG, 2)) {
                logV("finished onSizeReady in " + LogTime.getElapsedMillis(this.startTime));
            }
        }
    }

    private boolean canSetImage() {
        return this.requestCoordinator == null || this.requestCoordinator.canSetImage(this);
    }

    private boolean canSetPlaceholder() {
        return this.requestCoordinator == null || this.requestCoordinator.canSetPlaceholder(this);
    }

    private boolean isFirstImage() {
        return this.requestCoordinator == null || !this.requestCoordinator.isAnyRequestComplete();
    }

    @Override
    public void onResourceReady(Resource resource) {
        this.isRunning = false;
        if (!canSetImage()) {
            resource.release();
            return;
        }
        if (resource == null || !this.transcodeClass.isAssignableFrom(resource.get().getClass())) {
            if (resource != null) {
                resource.release();
            }
            onException(new Exception("Expected to receive an object of " + this.transcodeClass + " but instead got " + (resource != null ? resource.get() : null)));
            return;
        }
        Object obj = resource.get();
        if (this.requestListener != null) {
            if (!this.requestListener.onResourceReady((R) obj, this.model, this.target, this.loadedFromMemoryCache, isFirstImage())) {
                this.target.onResourceReady((R) obj, this.animationFactory.build(this.loadedFromMemoryCache, isFirstImage()));
            }
        }
        this.resource = resource;
        if (Log.isLoggable(TAG, 2)) {
            logV("Resource ready in " + LogTime.getElapsedMillis(this.startTime) + " size: " + (((double) resource.getSize()) / 1048576.0d) + " fromCache: " + this.loadedFromMemoryCache);
        }
    }

    @Override
    public void onException(Exception e) {
        if (Log.isLoggable(TAG, 3)) {
            Log.d(TAG, "load failed", e);
        }
        this.isRunning = false;
        this.isError = true;
        if (this.requestListener == null || !this.requestListener.onException(e, this.model, this.target, isFirstImage())) {
            setErrorPlaceholder();
        }
    }

    private void logV(String message) {
        Log.v(TAG, message + " this: " + this.tag);
    }
}
