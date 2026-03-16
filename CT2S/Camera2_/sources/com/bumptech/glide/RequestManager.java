package com.bumptech.glide;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.file_descriptor.FileDescriptorModelLoader;
import com.bumptech.glide.load.model.stream.MediaStoreStreamLoader;
import com.bumptech.glide.load.model.stream.StreamByteArrayLoader;
import com.bumptech.glide.load.model.stream.StreamModelLoader;
import com.bumptech.glide.manager.ConnectivityMonitor;
import com.bumptech.glide.manager.ConnectivityMonitorFactory;
import com.bumptech.glide.manager.RequestTracker;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

public class RequestManager {
    private final ConnectivityMonitor connectivityMonitor;
    private final Context context;
    private final Glide glide;
    private DefaultOptions options;
    private final OptionsApplier optionsApplier;
    private final RequestTracker requestTracker;

    public interface DefaultOptions {
        <T> void apply(T t, GenericRequestBuilder<T, ?, ?, ?> genericRequestBuilder);
    }

    public RequestManager(Context context) {
        this(context, new RequestTracker(), new ConnectivityMonitorFactory());
    }

    RequestManager(Context context, RequestTracker requestTracker, ConnectivityMonitorFactory factory) {
        this.context = context;
        this.requestTracker = requestTracker;
        this.connectivityMonitor = factory.build(context, new RequestManagerConnectivityListener(requestTracker));
        this.connectivityMonitor.register();
        this.glide = Glide.get(context);
        this.optionsApplier = new OptionsApplier();
    }

    public void setDefaultOptions(DefaultOptions options) {
        this.options = options;
    }

    public void pauseRequests() {
        this.requestTracker.pauseRequests();
    }

    public void resumeRequests() {
        this.requestTracker.resumeRequests();
    }

    public void onStart() {
        this.connectivityMonitor.register();
        this.requestTracker.resumeRequests();
    }

    public void onStop() {
        this.connectivityMonitor.unregister();
        this.requestTracker.pauseRequests();
    }

    public void onDestroy() {
        this.requestTracker.clearRequests();
    }

    public <A, T> GenericModelRequest<A, T> using(ModelLoader<A, T> modelLoader, Class<T> dataClass) {
        return new GenericModelRequest<>(modelLoader, dataClass);
    }

    public <T> ImageModelRequest<T> using(StreamModelLoader<T> modelLoader) {
        return new ImageModelRequest<>(modelLoader);
    }

    public ImageModelRequest<byte[]> using(StreamByteArrayLoader modelLoader) {
        return new ImageModelRequest<>(modelLoader);
    }

    public <T> VideoModelRequest<T> using(FileDescriptorModelLoader<T> modelLoader) {
        return new VideoModelRequest<>(modelLoader);
    }

    public DrawableTypeRequest<String> load(String string) {
        return loadGeneric(string);
    }

    public DrawableTypeRequest<Uri> load(Uri uri) {
        return loadGeneric(uri);
    }

    public DrawableTypeRequest<Uri> loadFromMediaStore(Uri uri, String mimeType, long dateModified, int orientation) {
        ModelLoader<Uri, InputStream> genericStreamLoader = Glide.buildStreamModelLoader(uri, this.context);
        ModelLoader<Uri, InputStream> mediaStoreLoader = new MediaStoreStreamLoader(this.context, genericStreamLoader, mimeType, dateModified, orientation);
        ModelLoader<Uri, ParcelFileDescriptor> fileDescriptorModelLoader = Glide.buildFileDescriptorModelLoader(uri, this.context);
        return (DrawableTypeRequest) this.optionsApplier.apply(uri, new DrawableTypeRequest(uri, mediaStoreLoader, fileDescriptorModelLoader, this.context, this.glide, this.requestTracker, this.optionsApplier));
    }

    public DrawableTypeRequest<File> load(File file) {
        return loadGeneric(file);
    }

    public DrawableTypeRequest<Integer> load(Integer resourceId) {
        return loadGeneric(resourceId);
    }

    public <T> DrawableTypeRequest<T> loadFromImage(T model) {
        return loadGeneric(model);
    }

    public DrawableTypeRequest<URL> loadFromImage(URL url) {
        return loadGeneric(url);
    }

    public DrawableTypeRequest<byte[]> loadFromImage(byte[] model, String id) {
        StreamByteArrayLoader loader = new StreamByteArrayLoader(id);
        return (DrawableTypeRequest) this.optionsApplier.apply(model, new DrawableTypeRequest(model, loader, null, this.context, this.glide, this.requestTracker, this.optionsApplier));
    }

    public DrawableTypeRequest<byte[]> loadFromImage(byte[] model) {
        return loadFromImage(model, UUID.randomUUID().toString());
    }

    public <T> DrawableTypeRequest<T> loadFromVideo(T model) {
        return loadGeneric(model);
    }

    public <T> DrawableTypeRequest<T> load(T model) {
        return loadGeneric(model);
    }

    private <T> DrawableTypeRequest<T> loadGeneric(T model) {
        ModelLoader<T, InputStream> streamModelLoader = Glide.buildStreamModelLoader(model, this.context);
        ModelLoader<T, ParcelFileDescriptor> fileDescriptorModelLoader = Glide.buildFileDescriptorModelLoader(model, this.context);
        if (model != null && streamModelLoader == null && fileDescriptorModelLoader == null) {
            throw new IllegalArgumentException("Unknown type " + model + ". You must provide a Model of a type for which there is a registered ModelLoader, if you are using a custom model, you must first call Glide#register with a ModelLoaderFactory for your custom model class");
        }
        return (DrawableTypeRequest) this.optionsApplier.apply(model, new DrawableTypeRequest(model, streamModelLoader, fileDescriptorModelLoader, this.context, this.glide, this.requestTracker, this.optionsApplier));
    }

    public class VideoModelRequest<T> {
        private final ModelLoader<T, ParcelFileDescriptor> loader;

        private VideoModelRequest(ModelLoader<T, ParcelFileDescriptor> loader) {
            this.loader = loader;
        }

        public BitmapTypeRequest<T> loadFromVideo(T model) {
            return (BitmapTypeRequest) RequestManager.this.optionsApplier.apply(model, new BitmapTypeRequest(RequestManager.this.context, model, null, this.loader, RequestManager.this.glide, RequestManager.this.requestTracker, RequestManager.this.optionsApplier));
        }
    }

    public class ImageModelRequest<T> {
        private final ModelLoader<T, InputStream> loader;

        private ImageModelRequest(ModelLoader<T, InputStream> loader) {
            this.loader = loader;
        }

        public DrawableTypeRequest<T> load(T model) {
            return (DrawableTypeRequest) RequestManager.this.optionsApplier.apply(model, new DrawableTypeRequest(model, this.loader, null, RequestManager.this.context, RequestManager.this.glide, RequestManager.this.requestTracker, RequestManager.this.optionsApplier));
        }
    }

    class OptionsApplier {
        OptionsApplier() {
        }

        public <A, X extends GenericRequestBuilder<A, ?, ?, ?>> X apply(A model, X builder) {
            if (RequestManager.this.options != null) {
                RequestManager.this.options.apply(model, builder);
            }
            return builder;
        }
    }

    public class GenericModelRequest<A, T> {
        private final Class<T> dataClass;
        private final ModelLoader<A, T> modelLoader;

        private GenericModelRequest(ModelLoader<A, T> modelLoader, Class<T> dataClass) {
            this.modelLoader = modelLoader;
            this.dataClass = dataClass;
        }

        public GenericModelRequest<A, T>.GenericTypeRequest load(A model) {
            return new GenericTypeRequest(model, this.modelLoader, this.dataClass);
        }

        public class GenericTypeRequest {
            private final Class<T> dataClass;
            private final A model;
            private final ModelLoader<A, T> modelLoader;

            private GenericTypeRequest(A model, ModelLoader<A, T> modelLoader, Class<T> dataClass) {
                this.model = model;
                this.modelLoader = modelLoader;
                this.dataClass = dataClass;
            }

            public <Z> GenericTranscodeRequest<A, T, Z> as(Class<Z> resourceClass) {
                return (GenericTranscodeRequest) RequestManager.this.optionsApplier.apply(this.model, new GenericTranscodeRequest(RequestManager.this.context, RequestManager.this.glide, this.model, this.modelLoader, this.dataClass, resourceClass, RequestManager.this.requestTracker, RequestManager.this.optionsApplier));
            }
        }
    }

    private static class RequestManagerConnectivityListener implements ConnectivityMonitor.ConnectivityListener {
        private RequestTracker requestTracker;

        public RequestManagerConnectivityListener(RequestTracker requestTracker) {
            this.requestTracker = requestTracker;
        }

        @Override
        public void onConnectivityChanged(boolean isConnected) {
            if (isConnected) {
                this.requestTracker.restartRequests();
            }
        }
    }
}
