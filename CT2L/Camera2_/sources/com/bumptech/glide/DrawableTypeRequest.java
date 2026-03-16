package com.bumptech.glide;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.model.ImageVideoModelLoader;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.gifbitmap.GifBitmapWrapper;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.provider.FixedLoadProvider;
import java.io.InputStream;

public class DrawableTypeRequest<A> extends DrawableRequestBuilder<A> {
    private final Context context;
    private final ModelLoader<A, ParcelFileDescriptor> fileDescriptorModelLoader;
    private final Glide glide;
    private final A model;
    private RequestManager.OptionsApplier optionsApplier;
    private RequestTracker requestTracker;
    private final ModelLoader<A, InputStream> streamModelLoader;

    private static <A, Z, R> FixedLoadProvider<A, ImageVideoWrapper, Z, R> buildProvider(Glide glide, ModelLoader<A, InputStream> streamModelLoader, ModelLoader<A, ParcelFileDescriptor> fileDescriptorModelLoader, Class<Z> resourceClass, Class<R> transcodedClass, ResourceTranscoder<Z, R> transcoder) {
        if (streamModelLoader == null && fileDescriptorModelLoader == null) {
            return null;
        }
        ImageVideoModelLoader imageVideoModelLoader = new ImageVideoModelLoader(streamModelLoader, fileDescriptorModelLoader);
        if (transcoder == null) {
            transcoder = glide.buildTranscoder(resourceClass, transcodedClass);
        }
        return new FixedLoadProvider<>(imageVideoModelLoader, transcoder, glide.buildDataProvider(ImageVideoWrapper.class, resourceClass));
    }

    DrawableTypeRequest(A model, ModelLoader<A, InputStream> streamModelLoader, ModelLoader<A, ParcelFileDescriptor> fileDescriptorModelLoader, Context context, Glide glide, RequestTracker requestTracker, RequestManager.OptionsApplier optionsApplier) {
        super(context, model, buildProvider(glide, streamModelLoader, fileDescriptorModelLoader, GifBitmapWrapper.class, Drawable.class, null), glide, requestTracker);
        this.model = model;
        this.streamModelLoader = streamModelLoader;
        this.fileDescriptorModelLoader = fileDescriptorModelLoader;
        this.context = context;
        this.glide = glide;
        this.requestTracker = requestTracker;
        this.optionsApplier = optionsApplier;
    }

    public BitmapTypeRequest<A> asBitmap() {
        return (BitmapTypeRequest) this.optionsApplier.apply(this.model, new BitmapTypeRequest(this.context, this.model, this.streamModelLoader, this.fileDescriptorModelLoader, this.glide, this.requestTracker, this.optionsApplier));
    }

    public GifTypeRequest<A> asGif() {
        return (GifTypeRequest) this.optionsApplier.apply(this.model, new GifTypeRequest(this.context, this.model, this.streamModelLoader, this.glide, this.requestTracker, this.optionsApplier));
    }
}
