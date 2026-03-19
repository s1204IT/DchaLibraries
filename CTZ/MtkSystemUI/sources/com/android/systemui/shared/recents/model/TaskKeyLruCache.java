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

    public TaskKeyLruCache(int i, EvictionCallback evictionCallback) {
        this.mEvictionCallback = evictionCallback;
        this.mCache = new LruCache<Integer, V>(i) {
            @Override
            protected void entryRemoved(boolean z, Integer num, V v, V v2) {
                if (TaskKeyLruCache.this.mEvictionCallback != null) {
                    TaskKeyLruCache.this.mEvictionCallback.onEntryEvicted(TaskKeyLruCache.this.mKeys.get(num.intValue()));
                }
                TaskKeyLruCache.this.mKeys.remove(num.intValue());
            }
        };
    }

    final void trimToSize(int i) {
        this.mCache.trimToSize(i);
    }

    public void dump(String str, PrintWriter printWriter) {
        String str2 = str + "  ";
        printWriter.print(str);
        printWriter.print("TaskKeyCache");
        printWriter.print(" numEntries=");
        printWriter.print(this.mKeys.size());
        printWriter.println();
        int size = this.mKeys.size();
        for (int i = 0; i < size; i++) {
            printWriter.print(str2);
            printWriter.println(this.mKeys.get(this.mKeys.keyAt(i)));
        }
    }

    @Override
    protected V getCacheEntry(int i) {
        return this.mCache.get(Integer.valueOf(i));
    }

    @Override
    protected void putCacheEntry(int i, V v) {
        this.mCache.put(Integer.valueOf(i), v);
    }

    @Override
    protected void removeCacheEntry(int i) {
        this.mCache.remove(Integer.valueOf(i));
    }

    @Override
    protected void evictAllCache() {
        this.mCache.evictAll();
    }
}
