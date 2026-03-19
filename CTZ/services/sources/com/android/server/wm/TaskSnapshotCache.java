package com.android.server.wm;

import android.app.ActivityManager;
import android.util.ArrayMap;
import java.io.PrintWriter;

class TaskSnapshotCache {
    private final TaskSnapshotLoader mLoader;
    private final WindowManagerService mService;
    private final ArrayMap<AppWindowToken, Integer> mAppTaskMap = new ArrayMap<>();
    private final ArrayMap<Integer, CacheEntry> mRunningCache = new ArrayMap<>();

    TaskSnapshotCache(WindowManagerService windowManagerService, TaskSnapshotLoader taskSnapshotLoader) {
        this.mService = windowManagerService;
        this.mLoader = taskSnapshotLoader;
    }

    void putSnapshot(Task task, ActivityManager.TaskSnapshot taskSnapshot) {
        CacheEntry cacheEntry = this.mRunningCache.get(Integer.valueOf(task.mTaskId));
        if (cacheEntry != null) {
            this.mAppTaskMap.remove(cacheEntry.topApp);
        }
        this.mAppTaskMap.put(task.getTopChild(), Integer.valueOf(task.mTaskId));
        this.mRunningCache.put(Integer.valueOf(task.mTaskId), new CacheEntry(taskSnapshot, task.getTopChild()));
    }

    ActivityManager.TaskSnapshot getSnapshot(int i, int i2, boolean z, boolean z2) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                CacheEntry cacheEntry = this.mRunningCache.get(Integer.valueOf(i));
                if (cacheEntry != null) {
                    ActivityManager.TaskSnapshot taskSnapshot = cacheEntry.snapshot;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return taskSnapshot;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                if (!z) {
                    return null;
                }
                return tryRestoreFromDisk(i, i2, z2);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private ActivityManager.TaskSnapshot tryRestoreFromDisk(int i, int i2, boolean z) {
        ActivityManager.TaskSnapshot taskSnapshotLoadTask = this.mLoader.loadTask(i, i2, z);
        if (taskSnapshotLoadTask == null) {
            return null;
        }
        return taskSnapshotLoadTask;
    }

    void onAppRemoved(AppWindowToken appWindowToken) {
        Integer num = this.mAppTaskMap.get(appWindowToken);
        if (num != null) {
            removeRunningEntry(num.intValue());
        }
    }

    void onAppDied(AppWindowToken appWindowToken) {
        Integer num = this.mAppTaskMap.get(appWindowToken);
        if (num != null) {
            removeRunningEntry(num.intValue());
        }
    }

    void onTaskRemoved(int i) {
        removeRunningEntry(i);
    }

    private void removeRunningEntry(int i) {
        CacheEntry cacheEntry = this.mRunningCache.get(Integer.valueOf(i));
        if (cacheEntry != null) {
            this.mAppTaskMap.remove(cacheEntry.topApp);
            this.mRunningCache.remove(Integer.valueOf(i));
        }
    }

    void dump(PrintWriter printWriter, String str) {
        String str2 = str + "  ";
        String str3 = str2 + "  ";
        printWriter.println(str + "SnapshotCache");
        for (int size = this.mRunningCache.size() + (-1); size >= 0; size += -1) {
            CacheEntry cacheEntryValueAt = this.mRunningCache.valueAt(size);
            printWriter.println(str2 + "Entry taskId=" + this.mRunningCache.keyAt(size));
            printWriter.println(str3 + "topApp=" + cacheEntryValueAt.topApp);
            printWriter.println(str3 + "snapshot=" + cacheEntryValueAt.snapshot);
        }
    }

    private static final class CacheEntry {
        final ActivityManager.TaskSnapshot snapshot;
        final AppWindowToken topApp;

        CacheEntry(ActivityManager.TaskSnapshot taskSnapshot, AppWindowToken appWindowToken) {
            this.snapshot = taskSnapshot;
            this.topApp = appWindowToken;
        }
    }
}
