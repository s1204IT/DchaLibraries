package com.bumptech.glide.load.model.stream;

import android.content.Context;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelCache;
import com.bumptech.glide.load.model.ModelLoader;
import java.io.InputStream;

public abstract class BaseGlideUrlLoader<T> implements StreamModelLoader<T> {
    private final ModelLoader<GlideUrl, InputStream> concreteLoader;
    private final ModelCache<T, GlideUrl> modelCache;

    protected abstract String getUrl(T t, int i, int i2);

    public BaseGlideUrlLoader(Context context) {
        this(context, (ModelCache) null);
    }

    public BaseGlideUrlLoader(Context context, ModelCache<T, GlideUrl> modelCache) {
        this((ModelLoader<GlideUrl, InputStream>) Glide.buildModelLoader(GlideUrl.class, InputStream.class, context), modelCache);
    }

    public BaseGlideUrlLoader(ModelLoader<GlideUrl, InputStream> concreteLoader) {
        this(concreteLoader, (ModelCache) null);
    }

    public BaseGlideUrlLoader(ModelLoader<GlideUrl, InputStream> concreteLoader, ModelCache<T, GlideUrl> modelCache) {
        this.concreteLoader = concreteLoader;
        this.modelCache = modelCache;
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(T model, int width, int height) {
        GlideUrl result = null;
        if (this.modelCache != null) {
            GlideUrl result2 = this.modelCache.get(model, width, height);
            result = result2;
        }
        if (result == null) {
            String stringURL = getUrl(model, width, height);
            result = new GlideUrl(stringURL);
            if (this.modelCache != null) {
                this.modelCache.put(model, width, height, result);
            }
        }
        return this.concreteLoader.getResourceFetcher(result, width, height);
    }
}
