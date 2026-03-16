package com.bumptech.glide.load.model;

import android.net.Uri;
import com.bumptech.glide.load.data.DataFetcher;
import java.io.File;

public class StringLoader<T> implements ModelLoader<String, T> {
    private final ModelLoader<Uri, T> uriLoader;

    public StringLoader(ModelLoader<Uri, T> uriLoader) {
        this.uriLoader = uriLoader;
    }

    @Override
    public DataFetcher<T> getResourceFetcher(String model, int width, int height) {
        Uri uri = Uri.parse(model);
        String scheme = uri.getScheme();
        if (scheme == null) {
            uri = Uri.fromFile(new File(model));
        }
        return this.uriLoader.getResourceFetcher(uri, width, height);
    }
}
