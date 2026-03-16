package com.bumptech.glide.load.engine.bitmap_recycle;

import android.os.Build;
import com.bumptech.glide.load.engine.bitmap_recycle.Poolable;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;

abstract class BaseKeyPool<T extends Poolable> {
    private static final int MAX_SIZE = 20;
    private final Queue<T> keyPool;

    protected abstract T create();

    public BaseKeyPool() {
        if (Build.VERSION.SDK_INT >= 9) {
            this.keyPool = new ArrayDeque(MAX_SIZE);
        } else {
            this.keyPool = new LinkedList();
        }
    }

    protected T get() {
        T tPoll = this.keyPool.poll();
        if (tPoll == null) {
            return (T) create();
        }
        return tPoll;
    }

    public void offer(T key) {
        if (this.keyPool.size() < MAX_SIZE) {
            this.keyPool.offer(key);
        }
    }
}
