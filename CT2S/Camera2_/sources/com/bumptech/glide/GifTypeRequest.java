package com.bumptech.glide;

import android.content.Context;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.gif.GifData;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.transcode.GifDataBytesTranscoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.provider.FixedLoadProvider;
import java.io.InputStream;

public class GifTypeRequest<A> extends GifRequestBuilder<A, GifDrawable> {
    private final Context context;
    private final Glide glide;
    private final A model;
    private RequestManager.OptionsApplier optionsApplier;
    private final RequestTracker requestTracker;
    private final ModelLoader<A, InputStream> streamModelLoader;

    private static <A, R> FixedLoadProvider<A, InputStream, GifData, R> buildProvider(Glide glide, ModelLoader<A, InputStream> streamModelLoader, Class<R> transcodeClass, ResourceTranscoder<GifData, R> transcoder) {
        if (transcoder == null) {
            transcoder = glide.buildTranscoder(GifData.class, transcodeClass);
        }
        if (streamModelLoader == null) {
            return null;
        }
        return new FixedLoadProvider<>(streamModelLoader, transcoder, glide.buildDataProvider(InputStream.class, GifData.class));
    }

    GifTypeRequest(Context context, A model, ModelLoader<A, InputStream> streamModelLoader, Glide glide, RequestTracker requestTracker, RequestManager.OptionsApplier optionsApplier) {
        super(context, model, buildProvider(glide, streamModelLoader, GifDrawable.class, null), GifDrawable.class, glide, requestTracker);
        this.context = context;
        this.model = model;
        this.streamModelLoader = streamModelLoader;
        this.glide = glide;
        this.requestTracker = requestTracker;
        this.optionsApplier = optionsApplier;
    }

    public <R> GifRequestBuilder<A, R> transcode(ResourceTranscoder<GifData, R> transcoder, Class<R> transcodeClass) {
        return (GifRequestBuilder) this.optionsApplier.apply(this.model, new GifRequestBuilder(this.context, this.model, buildProvider(this.glide, this.streamModelLoader, transcodeClass, transcoder), transcodeClass, this.glide, this.requestTracker));
    }

    public GifRequestBuilder<A, byte[]> toBytes() {
        return (GifRequestBuilder<A, byte[]>) transcode(new GifDataBytesTranscoder(), byte[].class);
    }
}
