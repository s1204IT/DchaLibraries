package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.model.ImageVideoModelLoader;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.transcode.BitmapBytesTranscoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.provider.FixedLoadProvider;
import java.io.InputStream;

public class BitmapTypeRequest<A> extends BitmapRequestBuilder<A, Bitmap> {
    private final Context context;
    private ModelLoader<A, ParcelFileDescriptor> fileDescriptorModelLoader;
    private final Glide glide;
    private final A model;
    private RequestManager.OptionsApplier optionsApplier;
    private RequestTracker requestTracker;
    private final ModelLoader<A, InputStream> streamModelLoader;

    private static <A, R> FixedLoadProvider<A, ImageVideoWrapper, Bitmap, R> buildProvider(Glide glide, ModelLoader<A, InputStream> streamModelLoader, ModelLoader<A, ParcelFileDescriptor> fileDescriptorModelLoader, Class<R> transcodedClass, ResourceTranscoder<Bitmap, R> transcoder) {
        if (streamModelLoader == null && fileDescriptorModelLoader == null) {
            return null;
        }
        ImageVideoModelLoader imageVideoModelLoader = new ImageVideoModelLoader(streamModelLoader, fileDescriptorModelLoader);
        if (transcoder == null) {
            transcoder = glide.buildTranscoder(Bitmap.class, transcodedClass);
        }
        return new FixedLoadProvider<>(imageVideoModelLoader, transcoder, glide.buildDataProvider(ImageVideoWrapper.class, Bitmap.class));
    }

    BitmapTypeRequest(Context context, A model, ModelLoader<A, InputStream> streamModelLoader, ModelLoader<A, ParcelFileDescriptor> fileDescriptorModelLoader, Glide glide, RequestTracker requestTracker, RequestManager.OptionsApplier optionsApplier) {
        super(context, model, buildProvider(glide, streamModelLoader, fileDescriptorModelLoader, Bitmap.class, null), Bitmap.class, glide, requestTracker);
        this.context = context;
        this.model = model;
        this.streamModelLoader = streamModelLoader;
        this.fileDescriptorModelLoader = fileDescriptorModelLoader;
        this.glide = glide;
        this.requestTracker = requestTracker;
        this.optionsApplier = optionsApplier;
    }

    public <R> BitmapRequestBuilder<A, R> transcode(ResourceTranscoder<Bitmap, R> transcoder, Class<R> transcodeClass) {
        return (BitmapRequestBuilder) this.optionsApplier.apply(this.model, new BitmapRequestBuilder(this.context, this.model, buildProvider(this.glide, this.streamModelLoader, this.fileDescriptorModelLoader, transcodeClass, transcoder), transcodeClass, this.glide, this.requestTracker));
    }

    public BitmapRequestBuilder<A, byte[]> toBytes() {
        return (BitmapRequestBuilder<A, byte[]>) transcode(new BitmapBytesTranscoder(), byte[].class);
    }

    public BitmapRequestBuilder<A, byte[]> toBytes(Bitmap.CompressFormat compressFormat, int i) {
        return (BitmapRequestBuilder<A, byte[]>) transcode(new BitmapBytesTranscoder(compressFormat, i), byte[].class);
    }
}
