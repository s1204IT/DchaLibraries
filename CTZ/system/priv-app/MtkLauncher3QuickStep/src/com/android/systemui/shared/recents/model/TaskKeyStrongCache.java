package com.android.systemui.shared.recents.model;

import android.util.ArrayMap;
import com.android.systemui.shared.recents.model.Task;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class TaskKeyStrongCache<V> extends TaskKeyCache<V> {
    private static final String TAG = "TaskKeyCache";
    private final ArrayMap<Integer, V> mCache = new ArrayMap<>();

    final void copyEntries(TaskKeyStrongCache<V> other) {
        for (int i = other.mKeys.size() - 1; i >= 0; i--) {
            Task.TaskKey key = other.mKeys.valueAt(i);
            put(key, other.mCache.get(Integer.valueOf(key.id)));
        }
    }

    public void dump(String prefix, PrintWriter writer) {
        String innerPrefix = prefix + "  ";
        writer.print(prefix);
        writer.print(TAG);
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
        this.mCache.clear();
    }
}
