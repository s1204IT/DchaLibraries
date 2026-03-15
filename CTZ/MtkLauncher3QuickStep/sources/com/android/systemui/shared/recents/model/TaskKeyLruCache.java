package com.android.systemui.shared.recents.model;

import android.util.LruCache;
import com.android.systemui.shared.recents.model.Task;
import java.io.PrintWriter;

public class TaskKeyLruCache<V> extends TaskKeyCache<V> {
    private final LruCache<Integer, V> mCache;
    private final EvictionCallback mEvictionCallback;

    public interface EvictionCallback {
        void onEntryEvicted(Task.TaskKey taskKey);
    }

    public TaskKeyLruCache(int cacheSize) {
        this(cacheSize, null);
    }

    public TaskKeyLruCache(int cacheSize, EvictionCallback evictionCallback) {
        this.mEvictionCallback = evictionCallback;
        this.mCache = new LruCache<Integer, V>(cacheSize) {
            @Override
            protected void entryRemoved(boolean evicted, Integer taskId, V oldV, V newV) {
                if (TaskKeyLruCache.this.mEvictionCallback != null) {
                    TaskKeyLruCache.this.mEvictionCallback.onEntryEvicted(TaskKeyLruCache.this.mKeys.get(taskId.intValue()));
                }
                TaskKeyLruCache.this.mKeys.remove(taskId.intValue());
            }
        };
    }

    final void trimToSize(int cacheSize) {
        this.mCache.trimToSize(cacheSize);
    }

    public void dump(String prefix, PrintWriter writer) {
        String innerPrefix = prefix + "  ";
        writer.print(prefix);
        writer.print("TaskKeyCache");
        writer.print(" numEntries=");
        writer.print(this.mKeys.size());
        writer.println();
        int keyCount = this.mKeys.size();
        for (int i = 0; i < keyCount; i++) {
            writer.print(innerPrefix);
            writer.println(this.mKeys.get(this.mKeys.keyAt(i)));
        }
    }

    @Override
    protected V getCacheEntry(int id) {
        return this.mCache.get(Integer.valueOf(id));
    }

    @Override
    protected void putCacheEntry(int id, V value) {
        this.mCache.put(Integer.valueOf(id), value);
    }

    @Override
    protected void removeCacheEntry(int id) {
        this.mCache.remove(Integer.valueOf(id));
    }

    @Override
    protected void evictAllCache() {
        this.mCache.evictAll();
    }
}
