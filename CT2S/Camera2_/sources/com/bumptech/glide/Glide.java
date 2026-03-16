package com.bumptech.glide;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.android.volley.RequestQueue;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.file_descriptor.FileDescriptorFileLoader;
import com.bumptech.glide.load.model.file_descriptor.FileDescriptorResourceLoader;
import com.bumptech.glide.load.model.file_descriptor.FileDescriptorStringLoader;
import com.bumptech.glide.load.model.file_descriptor.FileDescriptorUriLoader;
import com.bumptech.glide.load.model.stream.StreamFileLoader;
import com.bumptech.glide.load.model.stream.StreamResourceLoader;
import com.bumptech.glide.load.model.stream.StreamStringLoader;
import com.bumptech.glide.load.model.stream.StreamUriLoader;
import com.bumptech.glide.load.model.stream.StreamUrlLoader;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.FileDescriptorBitmapDataLoadProvider;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.ImageVideoDataLoadProvider;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDataLoadProvider;
import com.bumptech.glide.load.resource.gif.GifData;
import com.bumptech.glide.load.resource.gif.GifDataLoadProvider;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.gifbitmap.GifBitmapWrapper;
import com.bumptech.glide.load.resource.gifbitmap.GifBitmapWrapperTransformation;
import com.bumptech.glide.load.resource.gifbitmap.ImageVideoGifDataLoadProvider;
import com.bumptech.glide.load.resource.transcode.BitmapDrawableTranscoder;
import com.bumptech.glide.load.resource.transcode.GifBitmapWrapperDrawableTranscoder;
import com.bumptech.glide.load.resource.transcode.GifDataDrawableTranscoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.load.resource.transcode.TranscoderFactory;
import com.bumptech.glide.manager.RequestManagerRetriever;
import com.bumptech.glide.provider.DataLoadProviderFactory;
import com.bumptech.glide.request.GlideAnimation;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.target.ImageViewTargetFactory;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.volley.VolleyUrlLoader;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

public class Glide {
    private static final String DEFAULT_DISK_CACHE_DIR = "image_manager_disk_cache";
    static final int DEFAULT_DISK_CACHE_SIZE = 262144000;
    private static Glide GLIDE = null;
    private static final String TAG = "Glide";
    private final CenterCrop bitmapCenterCrop;
    private final FitCenter bitmapFitCenter;
    private final BitmapPool bitmapPool;
    private final GifBitmapWrapperTransformation drawableCenterCrop;
    private final GifBitmapWrapperTransformation drawableFitCenter;
    private final Engine engine;
    private final MemoryCache memoryCache;
    private final RequestQueue requestQueue;
    private final GenericLoaderFactory loaderFactory = new GenericLoaderFactory();
    private final ImageViewTargetFactory imageViewTargetFactory = new ImageViewTargetFactory();
    private final TranscoderFactory transcoderFactory = new TranscoderFactory();
    private final DataLoadProviderFactory dataLoadProviderFactory = new DataLoadProviderFactory();

    public static File getPhotoCacheDir(Context context) {
        return getPhotoCacheDir(context, DEFAULT_DISK_CACHE_DIR);
    }

    public static File getPhotoCacheDir(Context context, String cacheName) {
        File cacheDir = context.getCacheDir();
        if (cacheDir != null) {
            File result = new File(cacheDir, cacheName);
            result.mkdirs();
            return result;
        }
        if (Log.isLoggable(TAG, 6)) {
            Log.e(TAG, "default disk cache dir is null");
        }
        return null;
    }

    public static Glide get(Context context) {
        if (GLIDE == null) {
            GLIDE = new GlideBuilder(context).createGlide();
        }
        return GLIDE;
    }

    public static boolean isSetup() {
        return GLIDE != null;
    }

    public static void setup(GlideBuilder builder) {
        if (isSetup()) {
            throw new IllegalArgumentException("Glide is already setup, check with isSetup() first");
        }
        GLIDE = builder.createGlide();
    }

    static void tearDown() {
        GLIDE = null;
    }

    Glide(Engine engine, RequestQueue requestQueue, MemoryCache memoryCache, BitmapPool bitmapPool, Context context) {
        this.engine = engine;
        this.requestQueue = requestQueue;
        this.bitmapPool = bitmapPool;
        this.memoryCache = memoryCache;
        this.dataLoadProviderFactory.register(InputStream.class, Bitmap.class, new StreamBitmapDataLoadProvider(bitmapPool));
        this.dataLoadProviderFactory.register(ParcelFileDescriptor.class, Bitmap.class, new FileDescriptorBitmapDataLoadProvider(bitmapPool));
        ImageVideoDataLoadProvider imageVideoDataLoadProvider = new ImageVideoDataLoadProvider(bitmapPool);
        this.dataLoadProviderFactory.register(ImageVideoWrapper.class, Bitmap.class, imageVideoDataLoadProvider);
        GifDataLoadProvider gifDataLoadProvider = new GifDataLoadProvider(context, bitmapPool);
        this.dataLoadProviderFactory.register(InputStream.class, GifData.class, gifDataLoadProvider);
        this.dataLoadProviderFactory.register(ImageVideoWrapper.class, GifBitmapWrapper.class, new ImageVideoGifDataLoadProvider(imageVideoDataLoadProvider, gifDataLoadProvider));
        register(File.class, ParcelFileDescriptor.class, new FileDescriptorFileLoader.Factory());
        register(File.class, InputStream.class, new StreamFileLoader.Factory());
        register(Integer.class, ParcelFileDescriptor.class, new FileDescriptorResourceLoader.Factory());
        register(Integer.class, InputStream.class, new StreamResourceLoader.Factory());
        register(String.class, ParcelFileDescriptor.class, new FileDescriptorStringLoader.Factory());
        register(String.class, InputStream.class, new StreamStringLoader.Factory());
        register(Uri.class, ParcelFileDescriptor.class, new FileDescriptorUriLoader.Factory());
        register(Uri.class, InputStream.class, new StreamUriLoader.Factory());
        register(URL.class, InputStream.class, new StreamUrlLoader.Factory());
        register(GlideUrl.class, InputStream.class, new VolleyUrlLoader.Factory(requestQueue));
        this.transcoderFactory.register(Bitmap.class, BitmapDrawable.class, new BitmapDrawableTranscoder(context.getResources(), bitmapPool));
        this.transcoderFactory.register(GifBitmapWrapper.class, Drawable.class, new GifBitmapWrapperDrawableTranscoder(new BitmapDrawableTranscoder(context.getResources(), bitmapPool), new GifDataDrawableTranscoder()));
        this.transcoderFactory.register(GifData.class, GifDrawable.class, new GifDataDrawableTranscoder());
        this.bitmapCenterCrop = new CenterCrop(bitmapPool);
        this.drawableCenterCrop = new GifBitmapWrapperTransformation(this.bitmapCenterCrop);
        this.bitmapFitCenter = new FitCenter(bitmapPool);
        this.drawableFitCenter = new GifBitmapWrapperTransformation(this.bitmapFitCenter);
    }

    public BitmapPool getBitmapPool() {
        return this.bitmapPool;
    }

    <Z, R> ResourceTranscoder<Z, R> buildTranscoder(Class<Z> decodedClass, Class<R> transcodedClass) {
        return this.transcoderFactory.get(decodedClass, transcodedClass);
    }

    <T, Z> DataLoadProvider<T, Z> buildDataProvider(Class<T> dataClass, Class<Z> decodedClass) {
        return this.dataLoadProviderFactory.get(dataClass, decodedClass);
    }

    <R> Target<R> buildImageViewTarget(ImageView imageView, Class<R> transcodedClass) {
        return this.imageViewTargetFactory.buildTarget(imageView, transcodedClass);
    }

    Engine getEngine() {
        return this.engine;
    }

    CenterCrop getBitmapCenterCrop() {
        return this.bitmapCenterCrop;
    }

    FitCenter getBitmapFitCenter() {
        return this.bitmapFitCenter;
    }

    GifBitmapWrapperTransformation getDrawableCenterCrop() {
        return this.drawableCenterCrop;
    }

    GifBitmapWrapperTransformation getDrawableFitCenter() {
        return this.drawableFitCenter;
    }

    private GenericLoaderFactory getLoaderFactory() {
        return this.loaderFactory;
    }

    public RequestQueue getRequestQueue() {
        return this.requestQueue;
    }

    public void clearMemory() {
        this.bitmapPool.clearMemory();
        this.memoryCache.clearMemory();
    }

    public void trimMemory(int level) {
        this.bitmapPool.trimMemory(level);
        this.memoryCache.trimMemory(level);
    }

    public void setMemoryCategory(MemoryCategory memoryCategory) {
        this.memoryCache.setSizeMultiplier(memoryCategory.getMultiplier());
        this.bitmapPool.setSizeMultiplier(memoryCategory.getMultiplier());
    }

    public static void clear(Target target) {
        Request request = target.getRequest();
        if (request != null) {
            request.clear();
        }
    }

    public static void clear(View view) {
        Target viewTarget = new ClearTarget(view);
        clear(viewTarget);
    }

    public <T, Y> void register(Class<T> modelClass, Class<Y> resourceClass, ModelLoaderFactory<T, Y> factory) {
        ModelLoaderFactory<T, Y> removed = this.loaderFactory.register(modelClass, resourceClass, factory);
        if (removed != null) {
            removed.teardown();
        }
    }

    public <T, Y> void unregister(Class<T> modelClass, Class<Y> resourceClass) {
        ModelLoaderFactory<T, Y> removed = this.loaderFactory.unregister(modelClass, resourceClass);
        if (removed != null) {
            removed.teardown();
        }
    }

    public static <T, Y> ModelLoader<T, Y> buildModelLoader(Class<T> modelClass, Class<Y> resourceClass, Context context) {
        return get(context).getLoaderFactory().buildModelLoader(modelClass, resourceClass, context);
    }

    public static <T, Y> ModelLoader<T, Y> buildModelLoader(T model, Class<Y> resourceClass, Context context) {
        if (model != null) {
            return buildModelLoader((Class) model.getClass(), (Class) resourceClass, context);
        }
        if (Log.isLoggable(TAG, 3)) {
            Log.d(TAG, "Unable to load null model, setting placeholder only");
        }
        return null;
    }

    public static <T> ModelLoader<T, InputStream> buildStreamModelLoader(Class<T> modelClass, Context context) {
        return buildModelLoader((Class) modelClass, InputStream.class, context);
    }

    public static <T> ModelLoader<T, InputStream> buildStreamModelLoader(T model, Context context) {
        return buildModelLoader(model, InputStream.class, context);
    }

    public static <T> ModelLoader<T, ParcelFileDescriptor> buildFileDescriptorModelLoader(Class<T> modelClass, Context context) {
        return buildModelLoader((Class) modelClass, ParcelFileDescriptor.class, context);
    }

    public static <T> ModelLoader<T, ParcelFileDescriptor> buildFileDescriptorModelLoader(T model, Context context) {
        return buildModelLoader(model, ParcelFileDescriptor.class, context);
    }

    public static RequestManager with(Context context) {
        return RequestManagerRetriever.get(context);
    }

    public static RequestManager with(Activity activity) {
        return RequestManagerRetriever.get(activity);
    }

    public static RequestManager with(FragmentActivity activity) {
        return RequestManagerRetriever.get(activity);
    }

    @TargetApi(11)
    public static RequestManager with(Fragment fragment) {
        return RequestManagerRetriever.get(fragment);
    }

    public static RequestManager with(android.support.v4.app.Fragment fragment) {
        return RequestManagerRetriever.get(fragment);
    }

    private static class ClearTarget extends ViewTarget<View, Object> {
        public ClearTarget(View view) {
            super(view);
        }

        @Override
        public void onResourceReady(Object resource, GlideAnimation<Object> glideAnimation) {
        }

        @Override
        public void setPlaceholder(Drawable placeholder) {
        }
    }
}
