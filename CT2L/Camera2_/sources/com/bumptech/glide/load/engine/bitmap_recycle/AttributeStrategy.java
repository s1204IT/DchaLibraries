package com.bumptech.glide.load.engine.bitmap_recycle;

import android.graphics.Bitmap;
import com.bumptech.glide.util.Util;

class AttributeStrategy implements LruPoolStrategy {
    private final KeyPool keyPool = new KeyPool();
    private final GroupedLinkedMap<Key, Bitmap> groupedMap = new GroupedLinkedMap<>();

    AttributeStrategy() {
    }

    @Override
    public void put(Bitmap bitmap) {
        Key key = this.keyPool.get(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        this.groupedMap.put(key, bitmap);
    }

    @Override
    public Bitmap get(int width, int height, Bitmap.Config config) {
        Key key = this.keyPool.get(width, height, config);
        return this.groupedMap.get(key);
    }

    @Override
    public Bitmap removeLast() {
        return this.groupedMap.removeLast();
    }

    @Override
    public String logBitmap(Bitmap bitmap) {
        return getBitmapString(bitmap);
    }

    @Override
    public String logBitmap(int width, int height, Bitmap.Config config) {
        return getBitmapString(width, height, config);
    }

    @Override
    public int getSize(Bitmap bitmap) {
        return Util.getSize(bitmap);
    }

    public String toString() {
        return "AttributeStrategy:\n  " + this.groupedMap;
    }

    private static String getBitmapString(Bitmap bitmap) {
        return getBitmapString(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
    }

    private static String getBitmapString(int width, int height, Bitmap.Config config) {
        return "[" + width + "x" + height + "], " + config;
    }

    private static class KeyPool extends BaseKeyPool<Key> {
        private KeyPool() {
        }

        public Key get(int width, int height, Bitmap.Config config) {
            Key result = get();
            result.init(width, height, config);
            return result;
        }

        @Override
        protected Key create() {
            return new Key(this);
        }
    }

    private static class Key implements Poolable {
        private Bitmap.Config config;
        private int height;
        private final KeyPool pool;
        private int width;

        public Key(KeyPool pool) {
            this.pool = pool;
        }

        public void init(int width, int height, Bitmap.Config config) {
            this.width = width;
            this.height = height;
            this.config = config;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Key key = (Key) o;
            return this.height == key.height && this.width == key.width && this.config == key.config;
        }

        public int hashCode() {
            int result = this.width;
            return (((result * 31) + this.height) * 31) + (this.config != null ? this.config.hashCode() : 0);
        }

        public String toString() {
            return AttributeStrategy.getBitmapString(this.width, this.height, this.config);
        }

        @Override
        public void offer() {
            this.pool.offer(this);
        }
    }
}
