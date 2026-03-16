package com.bumptech.glide.load.engine.bitmap_recycle;

import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

public class LruBitmapPool implements BitmapPool {
    private static final String TAG = "LruBitmapPool";
    private int currentSize = 0;
    private int evictions;
    private int hits;
    private final int initialMaxSize;
    private int maxSize;
    private int misses;
    private int puts;
    private final LruPoolStrategy strategy;

    LruBitmapPool(int maxSize, LruPoolStrategy strategy) {
        this.initialMaxSize = maxSize;
        this.maxSize = maxSize;
        this.strategy = strategy;
    }

    public LruBitmapPool(int maxSize) {
        this.initialMaxSize = maxSize;
        this.maxSize = maxSize;
        if (Build.VERSION.SDK_INT >= 19) {
            this.strategy = new SizeStrategy();
        } else {
            this.strategy = new AttributeStrategy();
        }
    }

    @Override
    public void setSizeMultiplier(float sizeMultiplier) {
        this.maxSize = Math.round(this.initialMaxSize * sizeMultiplier);
        evict();
    }

    @Override
    public synchronized boolean put(Bitmap bitmap) {
        boolean z;
        if (!bitmap.isMutable() || this.strategy.getSize(bitmap) > this.maxSize) {
            z = false;
        } else {
            int size = this.strategy.getSize(bitmap);
            this.strategy.put(bitmap);
            this.puts++;
            this.currentSize += size;
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "Put bitmap in pool=" + this.strategy.logBitmap(bitmap));
            }
            dump();
            evict();
            z = true;
        }
        return z;
    }

    private void evict() {
        trimToSize(this.maxSize);
    }

    @Override
    public synchronized Bitmap get(int width, int height, Bitmap.Config config) {
        Bitmap result;
        result = this.strategy.get(width, height, config);
        if (result == null) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "Missing bitmap=" + this.strategy.logBitmap(width, height, config));
            }
            this.misses++;
        } else {
            this.hits++;
            this.currentSize -= this.strategy.getSize(result);
        }
        if (Log.isLoggable(TAG, 3)) {
            Log.d(TAG, "Get bitmap=" + this.strategy.logBitmap(width, height, config));
        }
        dump();
        return result;
    }

    @Override
    public void clearMemory() {
        trimToSize(0);
    }

    @Override
    public void trimMemory(int level) {
        if (level >= 60) {
            clearMemory();
        } else if (level >= 40) {
            trimToSize(this.maxSize / 2);
        }
    }

    private void trimToSize(int size) {
        while (this.currentSize > size) {
            Bitmap removed = this.strategy.removeLast();
            this.currentSize -= this.strategy.getSize(removed);
            removed.recycle();
            this.evictions++;
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "Evicting bitmap=" + this.strategy.logBitmap(removed));
            }
            dump();
        }
    }

    private void dump() {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "Hits=" + this.hits + " misses=" + this.misses + " puts=" + this.puts + " evictions=" + this.evictions + " currentSize=" + this.currentSize + " maxSize=" + this.maxSize + "\nStrategy=" + this.strategy);
        }
    }
}
