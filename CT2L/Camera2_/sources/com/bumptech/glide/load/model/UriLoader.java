package com.bumptech.glide.load.model;

import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.load.data.DataFetcher;

public abstract class UriLoader<T> implements ModelLoader<Uri, T> {
    private final Context context;
    private final ModelLoader<GlideUrl, T> urlLoader;

    protected abstract DataFetcher<T> getLocalUriFetcher(Context context, Uri uri);

    public UriLoader(Context context, ModelLoader<GlideUrl, T> urlLoader) {
        this.context = context;
        this.urlLoader = urlLoader;
    }

    @Override
    public final DataFetcher<T> getResourceFetcher(Uri model, int width, int height) {
        String scheme = model.getScheme();
        if (isLocalUri(scheme)) {
            DataFetcher<T> result = getLocalUriFetcher(this.context, model);
            return result;
        }
        if (this.urlLoader == null) {
            return null;
        }
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return null;
        }
        DataFetcher<T> result2 = this.urlLoader.getResourceFetcher(new GlideUrl(model.toString()), width, height);
        return result2;
    }

    private boolean isLocalUri(String scheme) {
        return "file".equals(scheme) || "content".equals(scheme) || "android.resource".equals(scheme);
    }
}
