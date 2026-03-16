package com.bumptech.glide.load.model;

import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.load.data.DataFetcher;

public class ResourceLoader<T> implements ModelLoader<Integer, T> {
    private final Context context;
    private final ModelLoader<Uri, T> uriLoader;

    public ResourceLoader(Context context, ModelLoader<Uri, T> uriLoader) {
        this.context = context;
        this.uriLoader = uriLoader;
    }

    @Override
    public DataFetcher<T> getResourceFetcher(Integer model, int width, int height) {
        Uri uri = Uri.parse("android.resource://" + this.context.getPackageName() + "/" + model.toString());
        return this.uriLoader.getResourceFetcher(uri, width, height);
    }
}
