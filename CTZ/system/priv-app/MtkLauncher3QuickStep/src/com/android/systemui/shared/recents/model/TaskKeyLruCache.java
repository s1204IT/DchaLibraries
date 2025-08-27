package com.android.systemui.shared.recents.model;

import android.util.LruCache;
import com.android.systemui.shared.recents.model.Task;
import java.io.PrintWriter;

/* loaded from: classes.dex */
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
        this.mCache = new LruCache<Integer, V>(cacheSize) { // from class: com.android.systemui.shared.recents.model.TaskKeyLruCache.1
            /* JADX DEBUG: Method merged with bridge method: entryRemoved(ZLjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V */
            @Override // android.util.LruCache
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

    @Override // com.android.systemui.shared.recents.model.TaskKeyCache
    protected V getCacheEntry(int id) {
        return this.mCache.get(Integer.valueOf(id));
    }

    @Override // com.android.systemui.shared.recents.model.TaskKeyCache
    protected void putCacheEntry(int id, V value) {
        this.mCache.put(Integer.valueOf(id), value);
    }

    @Override // com.android.systemui.shared.recents.model.TaskKeyCache
    protected void removeCacheEntry(int id) {
        this.mCache.remove(Integer.valueOf(id));
    }

    @Override // com.android.systemui.shared.recents.model.TaskKeyCache
    protected void evictAllCache() {
        this.mCache.evictAll();
    }
}
