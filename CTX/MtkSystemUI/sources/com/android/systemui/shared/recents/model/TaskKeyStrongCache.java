package com.android.systemui.shared.recents.model;

import android.util.ArrayMap;
import com.android.systemui.shared.recents.model.Task;
import java.io.PrintWriter;

public class TaskKeyStrongCache<V> extends TaskKeyCache<V> {
    private final ArrayMap<Integer, V> mCache = new ArrayMap<>();

    final void copyEntries(TaskKeyStrongCache<V> taskKeyStrongCache) {
        for (int size = taskKeyStrongCache.mKeys.size() - 1; size >= 0; size--) {
            Task.TaskKey taskKeyValueAt = taskKeyStrongCache.mKeys.valueAt(size);
            put(taskKeyValueAt, taskKeyStrongCache.mCache.get(Integer.valueOf(taskKeyValueAt.id)));
        }
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
        this.mCache.clear();
    }
}
