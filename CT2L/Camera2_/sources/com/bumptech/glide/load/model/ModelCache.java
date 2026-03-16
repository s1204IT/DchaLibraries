package com.bumptech.glide.load.model;

import com.bumptech.glide.util.LruCache;
import java.util.ArrayDeque;
import java.util.Queue;

public class ModelCache<A, B> {
    private static final int DEFAULT_SIZE = 250;
    private final LruCache<ModelKey<A>, B> cache;

    private static class ModelKey<A> {
        private static final Queue<ModelKey> KEY_QUEUE = new ArrayDeque();
        private int height;
        private A model;
        private int width;

        public static <A> ModelKey<A> get(A model, int width, int height) {
            ModelKey<A> modelKey = KEY_QUEUE.poll();
            if (modelKey == null) {
                modelKey = new ModelKey<>();
            }
            modelKey.init(model, width, height);
            return modelKey;
        }

        private ModelKey() {
        }

        private void init(A model, int width, int height) {
            this.model = model;
            this.width = width;
            this.height = height;
        }

        public void release() {
            KEY_QUEUE.offer(this);
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ModelKey modelKey = (ModelKey) o;
            return this.height == modelKey.height && this.width == modelKey.width && this.model.equals(modelKey.model);
        }

        public int hashCode() {
            int result = this.height;
            return (((result * 31) + this.width) * 31) + this.model.hashCode();
        }
    }

    public ModelCache() {
        this(DEFAULT_SIZE);
    }

    public ModelCache(int size) {
        this.cache = new LruCache<ModelKey<A>, B>(size) {
            @Override
            protected void onItemRemoved(ModelKey<A> key, B item) {
                key.release();
            }
        };
    }

    public B get(A model, int width, int height) {
        ModelKey<A> key = ModelKey.get(model, width, height);
        B result = this.cache.get(key);
        key.release();
        return result;
    }

    public void put(A model, int width, int height, B value) {
        ModelKey<A> key = ModelKey.get(model, width, height);
        this.cache.put(key, value);
    }
}
