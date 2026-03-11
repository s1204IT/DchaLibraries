package com.android.systemui.recents.model;

import android.util.Log;
import android.util.LruCache;
import android.util.SparseArray;
import com.android.systemui.recents.model.Task;
import java.io.PrintWriter;

public class TaskKeyLruCache<V> {
    private final LruCache<Integer, V> mCache;
    private final EvictionCallback mEvictionCallback;
    private final SparseArray<Task.TaskKey> mKeys;

    public interface EvictionCallback {
        void onEntryEvicted(Task.TaskKey taskKey);
    }

    public TaskKeyLruCache(int cacheSize) {
        this(cacheSize, null);
    }

    public TaskKeyLruCache(int cacheSize, EvictionCallback evictionCallback) {
        this.mKeys = new SparseArray<>();
        this.mEvictionCallback = evictionCallback;
        this.mCache = new LruCache<Integer, V>(cacheSize) {
            @Override
            public void entryRemoved(boolean evicted, Integer taskId, V oldV, V newV) {
                if (TaskKeyLruCache.this.mEvictionCallback != null) {
                    TaskKeyLruCache.this.mEvictionCallback.onEntryEvicted((Task.TaskKey) TaskKeyLruCache.this.mKeys.get(taskId.intValue()));
                }
                TaskKeyLruCache.this.mKeys.remove(taskId.intValue());
            }
        };
    }

    final V get(Task.TaskKey key) {
        return this.mCache.get(Integer.valueOf(key.id));
    }

    final V getAndInvalidateIfModified(Task.TaskKey key) {
        Task.TaskKey lastKey = this.mKeys.get(key.id);
        if (lastKey != null && (lastKey.stackId != key.stackId || lastKey.lastActiveTime != key.lastActiveTime)) {
            remove(key);
            return null;
        }
        return this.mCache.get(Integer.valueOf(key.id));
    }

    final void put(Task.TaskKey key, V value) {
        if (key == null || value == null) {
            Log.e("TaskKeyLruCache", "Unexpected null key or value: " + key + ", " + value);
        } else {
            this.mKeys.put(key.id, key);
            this.mCache.put(Integer.valueOf(key.id), value);
        }
    }

    final void remove(Task.TaskKey key) {
        this.mCache.remove(Integer.valueOf(key.id));
        this.mKeys.remove(key.id);
    }

    final void evictAll() {
        this.mCache.evictAll();
        this.mKeys.clear();
    }

    final void trimToSize(int cacheSize) {
        this.mCache.trimToSize(cacheSize);
    }

    public void dump(String prefix, PrintWriter writer) {
        String innerPrefix = prefix + "  ";
        writer.print(prefix);
        writer.print("TaskKeyLruCache");
        writer.print(" numEntries=");
        writer.print(this.mKeys.size());
        writer.println();
        int keyCount = this.mKeys.size();
        for (int i = 0; i < keyCount; i++) {
            writer.print(innerPrefix);
            writer.println(this.mKeys.get(this.mKeys.keyAt(i)));
        }
    }
}
