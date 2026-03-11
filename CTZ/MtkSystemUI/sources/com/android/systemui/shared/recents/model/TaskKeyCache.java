package com.android.systemui.shared.recents.model;

import android.util.Log;
import android.util.SparseArray;
import com.android.systemui.shared.recents.model.Task;

public abstract class TaskKeyCache<V> {
    protected final SparseArray<Task.TaskKey> mKeys = new SparseArray<>();

    protected abstract void evictAllCache();

    protected abstract V getCacheEntry(int i);

    protected abstract void putCacheEntry(int i, V v);

    protected abstract void removeCacheEntry(int i);

    final V get(Task.TaskKey taskKey) {
        return getCacheEntry(taskKey.id);
    }

    final V getAndInvalidateIfModified(Task.TaskKey taskKey) {
        Task.TaskKey taskKey2 = this.mKeys.get(taskKey.id);
        if (taskKey2 != null && (taskKey2.windowingMode != taskKey.windowingMode || taskKey2.lastActiveTime != taskKey.lastActiveTime)) {
            remove(taskKey);
            return null;
        }
        return getCacheEntry(taskKey.id);
    }

    final void put(Task.TaskKey taskKey, V v) {
        if (taskKey == null || v == null) {
            Log.e("TaskKeyCache", "Unexpected null key or value: " + taskKey + ", " + v);
            return;
        }
        this.mKeys.put(taskKey.id, taskKey);
        putCacheEntry(taskKey.id, v);
    }

    final void remove(Task.TaskKey taskKey) {
        removeCacheEntry(taskKey.id);
        this.mKeys.remove(taskKey.id);
    }

    final void evictAll() {
        evictAllCache();
        this.mKeys.clear();
    }
}
