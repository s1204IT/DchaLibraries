package com.android.systemui.recents.model;

import android.util.LruCache;
import com.android.systemui.recents.model.Task;
import java.util.HashMap;

public class KeyStoreLruCache<V> {
    LruCache<Integer, V> mCache;
    HashMap<Integer, Task.TaskKey> mTaskKeys = new HashMap<>();

    public KeyStoreLruCache(int cacheSize) {
        this.mCache = new LruCache<Integer, V>(cacheSize) {
            @Override
            public void entryRemoved(boolean evicted, Integer taskId, V oldV, V newV) {
                KeyStoreLruCache.this.mTaskKeys.remove(taskId);
            }
        };
    }

    final V get(Task.TaskKey key) {
        return this.mCache.get(Integer.valueOf(key.id));
    }

    final V getAndInvalidateIfModified(Task.TaskKey key) {
        Task.TaskKey lastKey = this.mTaskKeys.get(Integer.valueOf(key.id));
        if (lastKey == null || lastKey.lastActiveTime >= key.lastActiveTime) {
            return this.mCache.get(Integer.valueOf(key.id));
        }
        remove(key);
        return null;
    }

    final void put(Task.TaskKey key, V value) {
        this.mCache.put(Integer.valueOf(key.id), value);
        this.mTaskKeys.put(Integer.valueOf(key.id), key);
    }

    final void remove(Task.TaskKey key) {
        this.mCache.remove(Integer.valueOf(key.id));
        this.mTaskKeys.remove(Integer.valueOf(key.id));
    }

    final void evictAll() {
        this.mCache.evictAll();
        this.mTaskKeys.clear();
    }

    final void trimToSize(int cacheSize) {
        this.mCache.resize(cacheSize);
    }
}
