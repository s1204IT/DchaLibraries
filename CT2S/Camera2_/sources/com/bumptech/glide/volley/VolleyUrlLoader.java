package com.bumptech.glide.volley;

import android.content.Context;
import com.android.volley.RequestQueue;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import java.io.InputStream;

public class VolleyUrlLoader implements ModelLoader<GlideUrl, InputStream> {
    private final FutureFactory futureFactory;
    private final RequestQueue requestQueue;

    public interface FutureFactory {
        VolleyRequestFuture<InputStream> build();
    }

    public static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {
        private final FutureFactory futureFactory;
        private RequestQueue requestQueue;

        public Factory(RequestQueue requestQueue) {
            this(requestQueue, new DefaultFutureFactory());
        }

        public Factory(RequestQueue requestQueue, FutureFactory futureFactory) {
            this.requestQueue = requestQueue;
            this.futureFactory = futureFactory;
        }

        @Override
        public ModelLoader<GlideUrl, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new VolleyUrlLoader(this.requestQueue, this.futureFactory);
        }

        @Override
        public void teardown() {
        }
    }

    public VolleyUrlLoader(RequestQueue requestQueue, FutureFactory futureFactory) {
        this.requestQueue = requestQueue;
        this.futureFactory = futureFactory;
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(GlideUrl url, int width, int height) {
        return new VolleyStreamFetcher(this.requestQueue, url.toString(), this.futureFactory.build());
    }

    private static class DefaultFutureFactory implements FutureFactory {
        private DefaultFutureFactory() {
        }

        @Override
        public VolleyRequestFuture<InputStream> build() {
            return VolleyRequestFuture.newFuture();
        }
    }
}
