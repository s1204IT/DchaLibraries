package com.android.systemui.shared.recents.model;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.os.Trace;
import android.util.LruCache;
import com.android.internal.annotations.GuardedBy;
import com.android.systemui.shared.recents.model.BackgroundTaskLoader;
import com.android.systemui.shared.recents.model.IconLoader;
import com.android.systemui.shared.recents.model.RecentsTaskLoadPlan;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.TaskKeyLruCache;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import java.io.PrintWriter;
import java.util.Objects;

public class RecentsTaskLoader {
    private final LruCache<ComponentName, ActivityInfo> mActivityInfoCache;
    private final TaskKeyLruCache<String> mActivityLabelCache;
    private final TaskKeyLruCache<String> mContentDescriptionCache;
    private int mDefaultTaskBarBackgroundColor;
    private int mDefaultTaskViewBackgroundColor;
    private final HighResThumbnailLoader mHighResThumbnailLoader;
    private final TaskKeyLruCache<Drawable> mIconCache;
    private final IconLoader mIconLoader;
    private final TaskResourceLoadQueue mLoadQueue;
    private final BackgroundTaskLoader mLoader;
    private final int mMaxIconCacheSize;
    private final int mMaxThumbnailCacheSize;
    private int mNumVisibleTasksLoaded;
    private int mSvelteLevel;

    @GuardedBy("this")
    private final TaskKeyStrongCache<ThumbnailData> mThumbnailCache = new TaskKeyStrongCache<>();

    @GuardedBy("this")
    private final TaskKeyStrongCache<ThumbnailData> mTempCache = new TaskKeyStrongCache<>();
    private TaskKeyLruCache.EvictionCallback mClearActivityInfoOnEviction = new TaskKeyLruCache.EvictionCallback() {
        @Override
        public void onEntryEvicted(Task.TaskKey taskKey) {
            if (taskKey != null) {
                RecentsTaskLoader.this.mActivityInfoCache.remove(taskKey.getComponent());
            }
        }
    };

    public RecentsTaskLoader(Context context, int i, int i2, int i3) {
        this.mMaxThumbnailCacheSize = i;
        this.mMaxIconCacheSize = i2;
        this.mSvelteLevel = i3;
        int maxRecentTasksStatic = ActivityManager.getMaxRecentTasksStatic();
        this.mHighResThumbnailLoader = new HighResThumbnailLoader(ActivityManagerWrapper.getInstance(), Looper.getMainLooper(), ActivityManager.isLowRamDeviceStatic());
        this.mLoadQueue = new TaskResourceLoadQueue();
        this.mIconCache = new TaskKeyLruCache<>(this.mMaxIconCacheSize, this.mClearActivityInfoOnEviction);
        this.mActivityLabelCache = new TaskKeyLruCache<>(maxRecentTasksStatic, this.mClearActivityInfoOnEviction);
        this.mContentDescriptionCache = new TaskKeyLruCache<>(maxRecentTasksStatic, this.mClearActivityInfoOnEviction);
        this.mActivityInfoCache = new LruCache<>(maxRecentTasksStatic);
        this.mIconLoader = createNewIconLoader(context, this.mIconCache, this.mActivityInfoCache);
        TaskResourceLoadQueue taskResourceLoadQueue = this.mLoadQueue;
        IconLoader iconLoader = this.mIconLoader;
        final HighResThumbnailLoader highResThumbnailLoader = this.mHighResThumbnailLoader;
        Objects.requireNonNull(highResThumbnailLoader);
        this.mLoader = new BackgroundTaskLoader(taskResourceLoadQueue, iconLoader, new BackgroundTaskLoader.OnIdleChangedListener() {
            @Override
            public final void onIdleChanged(boolean z) {
                highResThumbnailLoader.setTaskLoadQueueIdle(z);
            }
        });
    }

    protected IconLoader createNewIconLoader(Context context, TaskKeyLruCache<Drawable> taskKeyLruCache, LruCache<ComponentName, ActivityInfo> lruCache) {
        return new IconLoader.DefaultIconLoader(context, taskKeyLruCache, lruCache);
    }

    public void setDefaultColors(int i, int i2) {
        this.mDefaultTaskBarBackgroundColor = i;
        this.mDefaultTaskViewBackgroundColor = i2;
    }

    public int getIconCacheSize() {
        return this.mMaxIconCacheSize;
    }

    public int getThumbnailCacheSize() {
        return this.mMaxThumbnailCacheSize;
    }

    public HighResThumbnailLoader getHighResThumbnailLoader() {
        return this.mHighResThumbnailLoader;
    }

    public synchronized void preloadTasks(RecentsTaskLoadPlan recentsTaskLoadPlan, int i) {
        preloadTasks(recentsTaskLoadPlan, i, ActivityManagerWrapper.getInstance().getCurrentUserId());
    }

    public synchronized void preloadTasks(RecentsTaskLoadPlan recentsTaskLoadPlan, int i, int i2) {
        try {
            Trace.beginSection("preloadPlan");
            recentsTaskLoadPlan.preloadPlan(new RecentsTaskLoadPlan.PreloadOptions(), this, i, i2);
        } finally {
            Trace.endSection();
        }
    }

    public synchronized void loadTasks(RecentsTaskLoadPlan recentsTaskLoadPlan, RecentsTaskLoadPlan.Options options) {
        if (options == null) {
            throw new RuntimeException("Requires load options");
        }
        if (options.onlyLoadForCache && options.loadThumbnails) {
            this.mTempCache.copyEntries(this.mThumbnailCache);
            this.mThumbnailCache.evictAll();
        }
        recentsTaskLoadPlan.executePlan(options, this);
        this.mTempCache.evictAll();
        if (!options.onlyLoadForCache) {
            this.mNumVisibleTasksLoaded = options.numVisibleTasks;
        }
    }

    public void loadTaskData(Task task) {
        Drawable andInvalidateIfModified = this.mIconCache.getAndInvalidateIfModified(task.key);
        if (andInvalidateIfModified == null) {
            andInvalidateIfModified = this.mIconLoader.getDefaultIcon(task.key.userId);
        }
        this.mLoadQueue.addTask(task);
        task.notifyTaskDataLoaded(task.thumbnail, andInvalidateIfModified);
    }

    public void unloadTaskData(Task task) {
        this.mLoadQueue.removeTask(task);
        task.notifyTaskDataUnloaded(this.mIconLoader.getDefaultIcon(task.key.userId));
    }

    public void deleteTaskData(Task task, boolean z) {
        this.mLoadQueue.removeTask(task);
        this.mIconCache.remove(task.key);
        this.mActivityLabelCache.remove(task.key);
        this.mContentDescriptionCache.remove(task.key);
        if (z) {
            task.notifyTaskDataUnloaded(this.mIconLoader.getDefaultIcon(task.key.userId));
        }
    }

    public synchronized void onTrimMemory(int i) {
        if (i != 5) {
            if (i != 10) {
                if (i != 15) {
                    if (i == 20) {
                        stopLoader();
                        this.mIconCache.trimToSize(Math.max(this.mNumVisibleTasksLoaded, this.mMaxIconCacheSize / 2));
                    } else if (i != 40) {
                        if (i != 60) {
                        }
                    }
                }
                this.mIconCache.evictAll();
                this.mActivityInfoCache.evictAll();
                this.mActivityLabelCache.evictAll();
                this.mContentDescriptionCache.evictAll();
                this.mThumbnailCache.evictAll();
            }
            this.mIconCache.trimToSize(Math.max(1, this.mMaxIconCacheSize / 4));
            this.mActivityInfoCache.trimToSize(Math.max(1, ActivityManager.getMaxRecentTasksStatic() / 4));
        }
        this.mIconCache.trimToSize(Math.max(1, this.mMaxIconCacheSize / 2));
        this.mActivityInfoCache.trimToSize(Math.max(1, ActivityManager.getMaxRecentTasksStatic() / 2));
    }

    public void onPackageChanged(String str) {
        for (ComponentName componentName : this.mActivityInfoCache.snapshot().keySet()) {
            if (componentName.getPackageName().equals(str)) {
                this.mActivityInfoCache.remove(componentName);
            }
        }
    }

    String getAndUpdateActivityTitle(Task.TaskKey taskKey, ActivityManager.TaskDescription taskDescription) {
        if (taskDescription != null && taskDescription.getLabel() != null) {
            return taskDescription.getLabel();
        }
        String andInvalidateIfModified = this.mActivityLabelCache.getAndInvalidateIfModified(taskKey);
        if (andInvalidateIfModified != null) {
            return andInvalidateIfModified;
        }
        ActivityInfo andUpdateActivityInfo = getAndUpdateActivityInfo(taskKey);
        if (andUpdateActivityInfo != null) {
            String badgedActivityLabel = ActivityManagerWrapper.getInstance().getBadgedActivityLabel(andUpdateActivityInfo, taskKey.userId);
            this.mActivityLabelCache.put(taskKey, badgedActivityLabel);
            return badgedActivityLabel;
        }
        return "";
    }

    String getAndUpdateContentDescription(Task.TaskKey taskKey, ActivityManager.TaskDescription taskDescription) {
        String andInvalidateIfModified = this.mContentDescriptionCache.getAndInvalidateIfModified(taskKey);
        if (andInvalidateIfModified != null) {
            return andInvalidateIfModified;
        }
        ActivityInfo andUpdateActivityInfo = getAndUpdateActivityInfo(taskKey);
        if (andUpdateActivityInfo != null) {
            String badgedContentDescription = ActivityManagerWrapper.getInstance().getBadgedContentDescription(andUpdateActivityInfo, taskKey.userId, taskDescription);
            if (taskDescription == null) {
                this.mContentDescriptionCache.put(taskKey, badgedContentDescription);
            }
            return badgedContentDescription;
        }
        return "";
    }

    Drawable getAndUpdateActivityIcon(Task.TaskKey taskKey, ActivityManager.TaskDescription taskDescription, boolean z) {
        return this.mIconLoader.getAndInvalidateIfModified(taskKey, taskDescription, z);
    }

    synchronized ThumbnailData getAndUpdateThumbnail(Task.TaskKey taskKey, boolean z, boolean z2) {
        ThumbnailData andInvalidateIfModified = this.mThumbnailCache.getAndInvalidateIfModified(taskKey);
        if (andInvalidateIfModified != null) {
            return andInvalidateIfModified;
        }
        ThumbnailData andInvalidateIfModified2 = this.mTempCache.getAndInvalidateIfModified(taskKey);
        if (andInvalidateIfModified2 != null) {
            this.mThumbnailCache.put(taskKey, andInvalidateIfModified2);
            return andInvalidateIfModified2;
        }
        if (z && this.mSvelteLevel < 3) {
            ThumbnailData taskThumbnail = ActivityManagerWrapper.getInstance().getTaskThumbnail(taskKey.id, true);
            if (taskThumbnail.thumbnail != null) {
                if (z2) {
                    this.mThumbnailCache.put(taskKey, taskThumbnail);
                }
                return taskThumbnail;
            }
        }
        return null;
    }

    int getActivityPrimaryColor(ActivityManager.TaskDescription taskDescription) {
        if (taskDescription != null && taskDescription.getPrimaryColor() != 0) {
            return taskDescription.getPrimaryColor();
        }
        return this.mDefaultTaskBarBackgroundColor;
    }

    int getActivityBackgroundColor(ActivityManager.TaskDescription taskDescription) {
        if (taskDescription != null && taskDescription.getBackgroundColor() != 0) {
            return taskDescription.getBackgroundColor();
        }
        return this.mDefaultTaskViewBackgroundColor;
    }

    ActivityInfo getAndUpdateActivityInfo(Task.TaskKey taskKey) {
        return this.mIconLoader.getAndUpdateActivityInfo(taskKey);
    }

    public void startLoader(Context context) {
        this.mLoader.start(context);
    }

    private void stopLoader() {
        this.mLoader.stop();
        this.mLoadQueue.clearTasks();
    }

    public synchronized void dump(String str, PrintWriter printWriter) {
        String str2 = str + "  ";
        printWriter.print(str);
        printWriter.println("RecentsTaskLoader");
        printWriter.print(str);
        printWriter.println("Icon Cache");
        this.mIconCache.dump(str2, printWriter);
        printWriter.print(str);
        printWriter.println("Thumbnail Cache");
        this.mThumbnailCache.dump(str2, printWriter);
        printWriter.print(str);
        printWriter.println("Temp Thumbnail Cache");
        this.mTempCache.dump(str2, printWriter);
    }
}
