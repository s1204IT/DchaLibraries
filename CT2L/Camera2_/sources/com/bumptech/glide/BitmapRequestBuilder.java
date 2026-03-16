package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.ParcelFileDescriptor;
import android.view.animation.Animation;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.load.resource.bitmap.FileDescriptorBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.ImageVideoBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.VideoBitmapDecoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.ViewPropertyAnimation;
import java.io.InputStream;

public class BitmapRequestBuilder<ModelType, TranscodeType> extends GenericRequestBuilder<ModelType, ImageVideoWrapper, Bitmap, TranscodeType> {
    private final BitmapPool bitmapPool;
    private DecodeFormat decodeFormat;
    private Downsampler downsampler;
    private Glide glide;
    private ResourceDecoder<InputStream, Bitmap> imageDecoder;
    private ResourceDecoder<ParcelFileDescriptor, Bitmap> videoDecoder;

    BitmapRequestBuilder(Context context, ModelType model, LoadProvider<ModelType, ImageVideoWrapper, Bitmap, TranscodeType> streamLoadProvider, Class<TranscodeType> transcodeClass, Glide glide, RequestTracker requestTracker) {
        super(context, model, streamLoadProvider, transcodeClass, glide, requestTracker);
        this.downsampler = Downsampler.AT_LEAST;
        this.decodeFormat = DecodeFormat.ALWAYS_ARGB_8888;
        this.glide = glide;
        this.bitmapPool = glide.getBitmapPool();
        this.imageDecoder = new StreamBitmapDecoder(this.bitmapPool);
        this.videoDecoder = new FileDescriptorBitmapDecoder(this.bitmapPool);
    }

    public BitmapRequestBuilder<ModelType, TranscodeType> approximate() {
        return downsample(Downsampler.AT_LEAST);
    }

    public BitmapRequestBuilder<ModelType, TranscodeType> asIs() {
        return downsample(Downsampler.NONE);
    }

    private BitmapRequestBuilder<ModelType, TranscodeType> downsample(Downsampler downsampler) {
        this.downsampler = downsampler;
        this.imageDecoder = new StreamBitmapDecoder(downsampler, this.bitmapPool, this.decodeFormat);
        super.decoder((ResourceDecoder) new ImageVideoBitmapDecoder(this.imageDecoder, this.videoDecoder));
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> thumbnail(float sizeMultiplier) {
        super.thumbnail(sizeMultiplier);
        return this;
    }

    public BitmapRequestBuilder<ModelType, TranscodeType> thumbnail(BitmapRequestBuilder<ModelType, TranscodeType> thumbnailRequest) {
        super.thumbnail((GenericRequestBuilder) thumbnailRequest);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> sizeMultiplier(float sizeMultiplier) {
        super.sizeMultiplier(sizeMultiplier);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> decoder(ResourceDecoder<ImageVideoWrapper, Bitmap> resourceDecoder) {
        super.decoder((ResourceDecoder) resourceDecoder);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> cacheDecoder(ResourceDecoder<InputStream, Bitmap> resourceDecoder) {
        super.cacheDecoder((ResourceDecoder) resourceDecoder);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> encoder(ResourceEncoder<Bitmap> resourceEncoder) {
        super.encoder((ResourceEncoder) resourceEncoder);
        return this;
    }

    public BitmapRequestBuilder<ModelType, TranscodeType> imageDecoder(ResourceDecoder<InputStream, Bitmap> decoder) {
        this.imageDecoder = decoder;
        super.decoder((ResourceDecoder) new ImageVideoBitmapDecoder(decoder, this.videoDecoder));
        return this;
    }

    public BitmapRequestBuilder<ModelType, TranscodeType> videoDecoder(ResourceDecoder<ParcelFileDescriptor, Bitmap> decoder) {
        this.videoDecoder = decoder;
        super.decoder((ResourceDecoder) new ImageVideoBitmapDecoder(this.imageDecoder, decoder));
        return this;
    }

    public BitmapRequestBuilder<ModelType, TranscodeType> format(DecodeFormat format) {
        this.decodeFormat = format;
        this.imageDecoder = new StreamBitmapDecoder(this.downsampler, this.bitmapPool, format);
        this.videoDecoder = new FileDescriptorBitmapDecoder(new VideoBitmapDecoder(), this.bitmapPool, format);
        super.decoder((ResourceDecoder) new ImageVideoBitmapDecoder(this.imageDecoder, this.videoDecoder));
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> priority(Priority priority) {
        super.priority(priority);
        return this;
    }

    public BitmapRequestBuilder<ModelType, TranscodeType> centerCrop() {
        return transform((Transformation<Bitmap>) this.glide.getBitmapCenterCrop());
    }

    public BitmapRequestBuilder<ModelType, TranscodeType> fitCenter() {
        return transform((Transformation<Bitmap>) this.glide.getBitmapFitCenter());
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> transform(Transformation<Bitmap> transformation) {
        super.transform((Transformation) transformation);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> transcoder(ResourceTranscoder<Bitmap, TranscodeType> resourceTranscoder) {
        super.transcoder((ResourceTranscoder) resourceTranscoder);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> animate(int animationId) {
        super.animate(animationId);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> animate(Animation animation) {
        super.animate(animation);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> animate(ViewPropertyAnimation.Animator animator) {
        super.animate(animator);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> placeholder(int resourceId) {
        super.placeholder(resourceId);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> placeholder(Drawable drawable) {
        super.placeholder(drawable);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> error(int resourceId) {
        super.error(resourceId);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> error(Drawable drawable) {
        super.error(drawable);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> listener(RequestListener<ModelType, TranscodeType> requestListener) {
        super.listener((RequestListener) requestListener);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> skipMemoryCache(boolean skip) {
        super.skipMemoryCache(skip);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> skipDiskCache(boolean skip) {
        super.skipDiskCache(skip);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> skipCache(boolean skip) {
        super.skipCache(skip);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> override(int width, int height) {
        super.override(width, height);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> thumbnail(GenericRequestBuilder<ModelType, ImageVideoWrapper, Bitmap, TranscodeType> genericRequestBuilder) {
        super.thumbnail((GenericRequestBuilder) genericRequestBuilder);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> sourceEncoder(Encoder<ImageVideoWrapper> encoder) {
        super.sourceEncoder((Encoder) encoder);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> cacheSource(boolean cacheSource) {
        super.cacheSource(cacheSource);
        return this;
    }
}
