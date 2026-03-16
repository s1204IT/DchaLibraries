package com.bumptech.glide;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import android.widget.ImageView;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.SkipCache;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.UnitTransformation;
import com.bumptech.glide.load.model.NullEncoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.provider.ChildLoadProvider;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.GenericRequest;
import com.bumptech.glide.request.GlideAnimationFactory;
import com.bumptech.glide.request.NoAnimation;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestCoordinator;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.ThumbnailRequestCoordinator;
import com.bumptech.glide.request.ViewAnimation;
import com.bumptech.glide.request.ViewPropertyAnimation;
import com.bumptech.glide.request.target.Target;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> {
    private final Context context;
    private int errorId;
    private Drawable errorPlaceholder;
    private final Glide glide;
    private final ChildLoadProvider<ModelType, DataType, ResourceType, TranscodeType> loadProvider;
    private final ModelType model;
    private Drawable placeholderDrawable;
    private int placeholderId;
    private ResourceEncoder<ResourceType> preSkipEncoder;
    private Encoder<DataType> preSkipSourceEncoder;
    private RequestListener<ModelType, TranscodeType> requestListener;
    private final RequestTracker requestTracker;
    private Float thumbSizeMultiplier;
    private GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> thumbnailRequestBuilder;
    private final Class<TranscodeType> transcodeClass;
    private List<Transformation<ResourceType>> transformations = null;
    private Transformation<ResourceType> singleTransformation = UnitTransformation.get();
    private Float sizeMultiplier = Float.valueOf(1.0f);
    private Priority priority = null;
    private boolean isCacheable = true;
    private GlideAnimationFactory<TranscodeType> animationFactory = NoAnimation.getFactory();
    private int overrideHeight = -1;
    private int overrideWidth = -1;
    private boolean cacheSource = false;

    GenericRequestBuilder(Context context, ModelType model, LoadProvider<ModelType, DataType, ResourceType, TranscodeType> loadProvider, Class<TranscodeType> transcodeClass, Glide glide, RequestTracker requestTracker) {
        this.transcodeClass = transcodeClass;
        this.glide = glide;
        this.requestTracker = requestTracker;
        this.loadProvider = loadProvider != null ? new ChildLoadProvider<>(loadProvider) : null;
        this.preSkipEncoder = loadProvider != null ? loadProvider.getEncoder() : null;
        if (context == null) {
            throw new NullPointerException("Context can't be null");
        }
        if (model != null && loadProvider == null) {
            throw new NullPointerException("LoadProvider must not be null");
        }
        this.context = context;
        this.model = model;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> thumbnail(GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> thumbnailRequest) {
        this.thumbnailRequestBuilder = thumbnailRequest;
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> thumbnail(float sizeMultiplier) {
        if (sizeMultiplier < 0.0f || sizeMultiplier > 1.0f) {
            throw new IllegalArgumentException("sizeMultiplier must be between 0 and 1");
        }
        this.thumbSizeMultiplier = Float.valueOf(sizeMultiplier);
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> sizeMultiplier(float sizeMultiplier) {
        if (sizeMultiplier < 0.0f || sizeMultiplier > 1.0f) {
            throw new IllegalArgumentException("sizeMultiplier must be between 0 and 1");
        }
        this.sizeMultiplier = Float.valueOf(sizeMultiplier);
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> decoder(ResourceDecoder<DataType, ResourceType> decoder) {
        if (this.loadProvider != null) {
            this.loadProvider.setSourceDecoder(decoder);
        }
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> cacheDecoder(ResourceDecoder<InputStream, ResourceType> cacheDecoder) {
        if (this.loadProvider != null) {
            this.loadProvider.setCacheDecoder(cacheDecoder);
        }
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> sourceEncoder(Encoder<DataType> sourceEncoder) {
        if (this.loadProvider != null) {
            this.loadProvider.setSourceEncoder(sourceEncoder);
            this.preSkipSourceEncoder = sourceEncoder;
        }
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> cacheSource(boolean cacheSource) {
        this.cacheSource = cacheSource;
        if (cacheSource) {
            return sourceEncoder(this.preSkipSourceEncoder);
        }
        if (this.loadProvider != null) {
            this.preSkipSourceEncoder = this.loadProvider.getSourceEncoder();
        }
        Encoder<DataType> skipCache = NullEncoder.get();
        return sourceEncoder(skipCache);
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> encoder(ResourceEncoder<ResourceType> encoder) {
        if (this.loadProvider != null) {
            this.loadProvider.setEncoder(encoder);
            this.preSkipEncoder = encoder;
        }
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> priority(Priority priority) {
        this.priority = priority;
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> transform(Transformation<ResourceType> transformation) {
        if (this.singleTransformation == UnitTransformation.get()) {
            this.singleTransformation = transformation;
        } else {
            this.transformations = new ArrayList();
            this.transformations.add(this.singleTransformation);
            this.transformations.add(transformation);
        }
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> transcoder(ResourceTranscoder<ResourceType, TranscodeType> transcoder) {
        if (this.loadProvider != null) {
            this.loadProvider.setTranscoder(transcoder);
        }
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> animate(int animationId) {
        return animate(new ViewAnimation.ViewAnimationFactory(this.context, animationId));
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> animate(Animation animation) {
        return animate(new ViewAnimation.ViewAnimationFactory(animation));
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> animate(ViewPropertyAnimation.Animator animator) {
        return animate(new ViewPropertyAnimation.ViewPropertyAnimationFactory(animator));
    }

    GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> animate(GlideAnimationFactory<TranscodeType> animationFactory) {
        if (animationFactory == null) {
            throw new NullPointerException("Animation factory must not be null!");
        }
        this.animationFactory = animationFactory;
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> placeholder(int resourceId) {
        this.placeholderId = resourceId;
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> placeholder(Drawable drawable) {
        this.placeholderDrawable = drawable;
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> error(int resourceId) {
        this.errorId = resourceId;
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> error(Drawable drawable) {
        this.errorPlaceholder = drawable;
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> listener(RequestListener<ModelType, TranscodeType> requestListener) {
        this.requestListener = requestListener;
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> skipMemoryCache(boolean skip) {
        this.isCacheable = !skip;
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> skipDiskCache(boolean skip) {
        if (!skip) {
            return encoder(this.preSkipEncoder);
        }
        if (this.loadProvider != null) {
            this.preSkipEncoder = this.loadProvider.getEncoder();
        }
        SkipCache<ResourceType> skipCache = SkipCache.get();
        return encoder(skipCache);
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> skipCache(boolean skip) {
        skipMemoryCache(skip);
        skipDiskCache(skip);
        cacheSource(false);
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> override(int width, int height) {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be >= 0");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be >= 0");
        }
        this.overrideWidth = width;
        this.overrideHeight = height;
        return this;
    }

    public <Y extends Target<TranscodeType>> Y into(Y target) {
        Request previous = target.getRequest();
        if (previous != null) {
            previous.clear();
            this.requestTracker.removeRequest(previous);
            previous.recycle();
        }
        Request request = buildRequest(target);
        target.setRequest(request);
        this.requestTracker.addRequest(request);
        request.run();
        return target;
    }

    public Target<TranscodeType> into(ImageView view) {
        return into(this.glide.buildImageViewTarget(view, this.transcodeClass));
    }

    private Priority getThumbnailPriority() {
        if (this.priority == Priority.LOW) {
            Priority result = Priority.NORMAL;
            return result;
        }
        if (this.priority == Priority.NORMAL) {
            Priority result2 = Priority.HIGH;
            return result2;
        }
        Priority result3 = Priority.IMMEDIATE;
        return result3;
    }

    private Request buildRequest(Target<TranscodeType> target) {
        if (this.priority == null) {
            this.priority = Priority.NORMAL;
        }
        return buildRequestRecursive(target, null);
    }

    private Request buildRequestRecursive(Target<TranscodeType> target, ThumbnailRequestCoordinator parentCoordinator) {
        if (this.thumbnailRequestBuilder != null) {
            if (this.thumbnailRequestBuilder.animationFactory.equals(NoAnimation.getFactory())) {
                this.thumbnailRequestBuilder.animationFactory = this.animationFactory;
            }
            if (this.thumbnailRequestBuilder.requestListener == null && this.requestListener != null) {
                this.thumbnailRequestBuilder.requestListener = this.requestListener;
            }
            if (this.thumbnailRequestBuilder.priority == null) {
                this.thumbnailRequestBuilder.priority = getThumbnailPriority();
            }
            ThumbnailRequestCoordinator coordinator = new ThumbnailRequestCoordinator(parentCoordinator);
            Request fullRequest = obtainRequest(target, this.sizeMultiplier.floatValue(), this.priority, coordinator);
            Request thumbRequest = this.thumbnailRequestBuilder.buildRequestRecursive(target, coordinator);
            coordinator.setRequests(fullRequest, thumbRequest);
            return coordinator;
        }
        if (this.thumbSizeMultiplier != null) {
            ThumbnailRequestCoordinator coordinator2 = new ThumbnailRequestCoordinator(parentCoordinator);
            Request fullRequest2 = obtainRequest(target, this.sizeMultiplier.floatValue(), this.priority, coordinator2);
            Request thumbnailRequest = obtainRequest(target, this.thumbSizeMultiplier.floatValue(), getThumbnailPriority(), coordinator2);
            coordinator2.setRequests(fullRequest2, thumbnailRequest);
            return coordinator2;
        }
        return obtainRequest(target, this.sizeMultiplier.floatValue(), this.priority, parentCoordinator);
    }

    private <Z> Request obtainRequest(Target<TranscodeType> target, float sizeMultiplier, Priority priority, RequestCoordinator requestCoordinator) {
        if (this.model == null) {
            requestCoordinator = null;
        }
        return GenericRequest.obtain(this.loadProvider, this.model, this.context, priority, target, sizeMultiplier, this.placeholderDrawable, this.placeholderId, this.errorPlaceholder, this.errorId, this.requestListener, requestCoordinator, this.glide.getEngine(), getFinalTransformation(), this.transcodeClass, this.isCacheable, this.animationFactory, this.overrideWidth, this.overrideHeight, this.cacheSource);
    }

    private Transformation<ResourceType> getFinalTransformation() {
        return this.transformations == null ? this.singleTransformation : new MultiTransformation(this.transformations);
    }
}
