package com.bumptech.glide.load.engine.bitmap_recycle;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.support.v4.util.TimeUtils;
import java.util.TreeMap;

@TargetApi(TimeUtils.HUNDRED_DAY_FIELD_LEN)
class SizeStrategy implements LruPoolStrategy {
    private static final int MAX_SIZE_MULTIPLE = 4;
    private final KeyPool keyPool = new KeyPool(null);
    private final GroupedLinkedMap<Key, Bitmap> groupedMap = new GroupedLinkedMap<>();
    private final TreeMap<Integer, Integer> sortedSizes = new TreeMap<>();

    SizeStrategy() {
    }

    @Override
    public void put(Bitmap bitmap) {
        Key key = this.keyPool.get(bitmap.getAllocationByteCount());
        this.groupedMap.put(key, bitmap);
        Integer current = this.sortedSizes.get(Integer.valueOf(key.size));
        this.sortedSizes.put(Integer.valueOf(key.size), Integer.valueOf(current == null ? 1 : current.intValue() + 1));
    }

    @Override
    public Bitmap get(int width, int height, Bitmap.Config config) {
        int size = getSize(width, height, config);
        Key key = this.keyPool.get(size);
        Integer possibleSize = this.sortedSizes.ceilingKey(Integer.valueOf(size));
        if (possibleSize != null && possibleSize.intValue() != size && possibleSize.intValue() <= size * 4) {
            this.keyPool.offer(key);
            key = this.keyPool.get(possibleSize.intValue());
        }
        Bitmap result = this.groupedMap.get(key);
        if (result != null) {
            result.reconfigure(width, height, config);
            decrementBitmapOfSize(possibleSize);
        }
        return result;
    }

    @Override
    public Bitmap removeLast() {
        Bitmap removed = this.groupedMap.removeLast();
        if (removed != null) {
            int removedSize = removed.getAllocationByteCount();
            decrementBitmapOfSize(Integer.valueOf(removedSize));
        }
        return removed;
    }

    private void decrementBitmapOfSize(Integer size) {
        Integer current = this.sortedSizes.get(size);
        if (current.intValue() == 1) {
            this.sortedSizes.remove(size);
        } else {
            this.sortedSizes.put(size, Integer.valueOf(current.intValue() - 1));
        }
    }

    @Override
    public String logBitmap(Bitmap bitmap) {
        return getBitmapString(bitmap);
    }

    @Override
    public String logBitmap(int width, int height, Bitmap.Config config) {
        return getBitmapString(getSize(width, height, config));
    }

    @Override
    public int getSize(Bitmap bitmap) {
        return bitmap.getAllocationByteCount();
    }

    public String toString() {
        String result = "SizeStrategy:\n  " + this.groupedMap + "\n  SortedSizes( ";
        boolean hadAtLeastOneKey = false;
        for (Integer size : this.sortedSizes.keySet()) {
            hadAtLeastOneKey = true;
            result = result + "{" + getBitmapString(size.intValue()) + ":" + this.sortedSizes.get(size) + "}, ";
        }
        if (hadAtLeastOneKey) {
            result = result.substring(0, result.length() - 2);
        }
        return result + " )";
    }

    private static String getBitmapString(Bitmap bitmap) {
        return getBitmapString(bitmap.getAllocationByteCount());
    }

    private static String getBitmapString(int size) {
        return "[" + size + "]";
    }

    private static int getSize(int width, int height, Bitmap.Config config) {
        return width * height * getBytesPerPixel(config);
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$android$graphics$Bitmap$Config = new int[Bitmap.Config.values().length];

        static {
            try {
                $SwitchMap$android$graphics$Bitmap$Config[Bitmap.Config.ARGB_8888.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$graphics$Bitmap$Config[Bitmap.Config.RGB_565.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$graphics$Bitmap$Config[Bitmap.Config.ARGB_4444.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$graphics$Bitmap$Config[Bitmap.Config.ALPHA_8.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    private static int getBytesPerPixel(Bitmap.Config config) {
        switch (AnonymousClass1.$SwitchMap$android$graphics$Bitmap$Config[config.ordinal()]) {
            case 1:
            default:
                return 4;
            case 2:
                return 2;
            case 3:
                return 2;
            case 4:
                return 1;
        }
    }

    private static class KeyPool extends BaseKeyPool<Key> {
        private KeyPool() {
        }

        KeyPool(AnonymousClass1 x0) {
            this();
        }

        public Key get(int size) {
            Key result = get();
            result.init(size);
            return result;
        }

        @Override
        protected Key create() {
            return new Key(this, null);
        }
    }

    private static class Key implements Poolable {
        private final KeyPool pool;
        private int size;

        Key(KeyPool x0, AnonymousClass1 x1) {
            this(x0);
        }

        private Key(KeyPool pool) {
            this.pool = pool;
        }

        public void init(int size) {
            this.size = size;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Key key = (Key) o;
            return this.size == key.size;
        }

        public int hashCode() {
            return this.size;
        }

        public String toString() {
            return SizeStrategy.getBitmapString(this.size);
        }

        @Override
        public void offer() {
            this.pool.offer(this);
        }
    }
}
