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
import java.util.Map;
import java.util.Objects;

/* loaded from: classes.dex */
public class RecentsTaskLoader {
    private static final boolean DEBUG = false;
    public static final int SVELTE_DISABLE_CACHE = 2;
    public static final int SVELTE_DISABLE_LOADING = 3;
    public static final int SVELTE_LIMIT_CACHE = 1;
    public static final int SVELTE_NONE = 0;
    private static final String TAG = "RecentsTaskLoader";
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
    private TaskKeyLruCache.EvictionCallback mClearActivityInfoOnEviction = new TaskKeyLruCache.EvictionCallback() { // from class: com.android.systemui.shared.recents.model.RecentsTaskLoader.1
        AnonymousClass1() {
        }

        @Override // com.android.systemui.shared.recents.model.TaskKeyLruCache.EvictionCallback
        public void onEntryEvicted(Task.TaskKey key) {
            if (key != null) {
                RecentsTaskLoader.this.mActivityInfoCache.remove(key.getComponent());
            }
        }
    };

    /* renamed from: com.android.systemui.shared.recents.model.RecentsTaskLoader$1 */
    class AnonymousClass1 implements TaskKeyLruCache.EvictionCallback {
        AnonymousClass1() {
        }

        @Override // com.android.systemui.shared.recents.model.TaskKeyLruCache.EvictionCallback
        public void onEntryEvicted(Task.TaskKey key) {
            if (key != null) {
                RecentsTaskLoader.this.mActivityInfoCache.remove(key.getComponent());
            }
        }
    }

    public RecentsTaskLoader(Context context, int maxThumbnailCacheSize, int maxIconCacheSize, int svelteLevel) {
        this.mMaxThumbnailCacheSize = maxThumbnailCacheSize;
        this.mMaxIconCacheSize = maxIconCacheSize;
        this.mSvelteLevel = svelteLevel;
        int numRecentTasks = ActivityManager.getMaxRecentTasksStatic();
        this.mHighResThumbnailLoader = new HighResThumbnailLoader(ActivityManagerWrapper.getInstance(), Looper.getMainLooper(), ActivityManager.isLowRamDeviceStatic());
        this.mLoadQueue = new TaskResourceLoadQueue();
        this.mIconCache = new TaskKeyLruCache<>(this.mMaxIconCacheSize, this.mClearActivityInfoOnEviction);
        this.mActivityLabelCache = new TaskKeyLruCache<>(numRecentTasks, this.mClearActivityInfoOnEviction);
        this.mContentDescriptionCache = new TaskKeyLruCache<>(numRecentTasks, this.mClearActivityInfoOnEviction);
        this.mActivityInfoCache = new LruCache<>(numRecentTasks);
        this.mIconLoader = createNewIconLoader(context, this.mIconCache, this.mActivityInfoCache);
        TaskResourceLoadQueue taskResourceLoadQueue = this.mLoadQueue;
        IconLoader iconLoader = this.mIconLoader;
        final HighResThumbnailLoader highResThumbnailLoader = this.mHighResThumbnailLoader;
        Objects.requireNonNull(highResThumbnailLoader);
        this.mLoader = new BackgroundTaskLoader(taskResourceLoadQueue, iconLoader, new BackgroundTaskLoader.OnIdleChangedListener() { // from class: com.android.systemui.shared.recents.model.-$$Lambda$vKccogRiJjM5FBa4zs196J3w_Fs
            @Override // com.android.systemui.shared.recents.model.BackgroundTaskLoader.OnIdleChangedListener
            public final void onIdleChanged(boolean z) {
                highResThumbnailLoader.setTaskLoadQueueIdle(z);
            }
        });
    }

    protected IconLoader createNewIconLoader(Context context, TaskKeyLruCache<Drawable> iconCache, LruCache<ComponentName, ActivityInfo> activityInfoCache) {
        return new IconLoader.DefaultIconLoader(context, iconCache, activityInfoCache);
    }

    public void setDefaultColors(int defaultTaskBarBackgroundColor, int defaultTaskViewBackgroundColor) {
        this.mDefaultTaskBarBackgroundColor = defaultTaskBarBackgroundColor;
        this.mDefaultTaskViewBackgroundColor = defaultTaskViewBackgroundColor;
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

    public synchronized void preloadTasks(RecentsTaskLoadPlan plan, int runningTaskId) {
        preloadTasks(plan, runningTaskId, ActivityManagerWrapper.getInstance().getCurrentUserId());
    }

    public synchronized void preloadTasks(RecentsTaskLoadPlan plan, int runningTaskId, int currentUserId) {
        try {
            Trace.beginSection("preloadPlan");
            plan.preloadPlan(new RecentsTaskLoadPlan.PreloadOptions(), this, runningTaskId, currentUserId);
        } finally {
            Trace.endSection();
        }
    }

    public synchronized void loadTasks(RecentsTaskLoadPlan plan, RecentsTaskLoadPlan.Options opts) {
        if (opts == null) {
            throw new RuntimeException("Requires load options");
        }
        if (opts.onlyLoadForCache && opts.loadThumbnails) {
            this.mTempCache.copyEntries(this.mThumbnailCache);
            this.mThumbnailCache.evictAll();
        }
        plan.executePlan(opts, this);
        this.mTempCache.evictAll();
        if (!opts.onlyLoadForCache) {
            this.mNumVisibleTasksLoaded = opts.numVisibleTasks;
        }
    }

    public void loadTaskData(Task t) {
        Drawable icon = this.mIconCache.getAndInvalidateIfModified(t.key);
        Drawable icon2 = icon != null ? icon : this.mIconLoader.getDefaultIcon(t.key.userId);
        this.mLoadQueue.addTask(t);
        t.notifyTaskDataLoaded(t.thumbnail, icon2);
    }

    public void unloadTaskData(Task t) {
        this.mLoadQueue.removeTask(t);
        t.notifyTaskDataUnloaded(this.mIconLoader.getDefaultIcon(t.key.userId));
    }

    public void deleteTaskData(Task t, boolean notifyTaskDataUnloaded) {
        this.mLoadQueue.removeTask(t);
        this.mIconCache.remove(t.key);
        this.mActivityLabelCache.remove(t.key);
        this.mContentDescriptionCache.remove(t.key);
        if (notifyTaskDataUnloaded) {
            t.notifyTaskDataUnloaded(this.mIconLoader.getDefaultIcon(t.key.userId));
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:46:0x001b, code lost:
    
        if (r4 != 80) goto L52;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public synchronized void onTrimMemory(int level) {
        if (level != 5) {
            if (level != 10) {
                if (level != 15) {
                    if (level == 20) {
                        stopLoader();
                        this.mIconCache.trimToSize(Math.max(this.mNumVisibleTasksLoaded, this.mMaxIconCacheSize / 2));
                    } else if (level != 40) {
                        if (level != 60) {
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

    public void onPackageChanged(String packageName) {
        Map<ComponentName, ActivityInfo> activityInfoCache = this.mActivityInfoCache.snapshot();
        for (ComponentName cn : activityInfoCache.keySet()) {
            if (cn.getPackageName().equals(packageName)) {
                this.mActivityInfoCache.remove(cn);
            }
        }
    }

    String getAndUpdateActivityTitle(Task.TaskKey taskKey, ActivityManager.TaskDescription td) {
        if (td != null && td.getLabel() != null) {
            return td.getLabel();
        }
        String label = this.mActivityLabelCache.getAndInvalidateIfModified(taskKey);
        if (label != null) {
            return label;
        }
        ActivityInfo activityInfo = getAndUpdateActivityInfo(taskKey);
        if (activityInfo != null) {
            String label2 = ActivityManagerWrapper.getInstance().getBadgedActivityLabel(activityInfo, taskKey.userId);
            this.mActivityLabelCache.put(taskKey, label2);
            return label2;
        }
        return "";
    }

    String getAndUpdateContentDescription(Task.TaskKey taskKey, ActivityManager.TaskDescription td) {
        String label = this.mContentDescriptionCache.getAndInvalidateIfModified(taskKey);
        if (label != null) {
            return label;
        }
        ActivityInfo activityInfo = getAndUpdateActivityInfo(taskKey);
        if (activityInfo != null) {
            String label2 = ActivityManagerWrapper.getInstance().getBadgedContentDescription(activityInfo, taskKey.userId, td);
            if (td == null) {
                this.mContentDescriptionCache.put(taskKey, label2);
            }
            return label2;
        }
        return "";
    }

    Drawable getAndUpdateActivityIcon(Task.TaskKey taskKey, ActivityManager.TaskDescription td, boolean loadIfNotCached) {
        return this.mIconLoader.getAndInvalidateIfModified(taskKey, td, loadIfNotCached);
    }

    synchronized ThumbnailData getAndUpdateThumbnail(Task.TaskKey taskKey, boolean loadIfNotCached, boolean storeInCache) {
        ThumbnailData cached = this.mThumbnailCache.getAndInvalidateIfModified(taskKey);
        if (cached != null) {
            return cached;
        }
        ThumbnailData cached2 = this.mTempCache.getAndInvalidateIfModified(taskKey);
        if (cached2 != null) {
            this.mThumbnailCache.put(taskKey, cached2);
            return cached2;
        }
        if (loadIfNotCached && this.mSvelteLevel < 3) {
            ThumbnailData thumbnailData = ActivityManagerWrapper.getInstance().getTaskThumbnail(taskKey.id, true);
            if (thumbnailData.thumbnail != null) {
                if (storeInCache) {
                    this.mThumbnailCache.put(taskKey, thumbnailData);
                }
                return thumbnailData;
            }
        }
        return null;
    }

    int getActivityPrimaryColor(ActivityManager.TaskDescription td) {
        if (td != null && td.getPrimaryColor() != 0) {
            return td.getPrimaryColor();
        }
        return this.mDefaultTaskBarBackgroundColor;
    }

    int getActivityBackgroundColor(ActivityManager.TaskDescription td) {
        if (td != null && td.getBackgroundColor() != 0) {
            return td.getBackgroundColor();
        }
        return this.mDefaultTaskViewBackgroundColor;
    }

    ActivityInfo getAndUpdateActivityInfo(Task.TaskKey taskKey) {
        return this.mIconLoader.getAndUpdateActivityInfo(taskKey);
    }

    public void startLoader(Context ctx) {
        this.mLoader.start(ctx);
    }

    private void stopLoader() {
        this.mLoader.stop();
        this.mLoadQueue.clearTasks();
    }

    public synchronized void dump(String prefix, PrintWriter writer) {
        String innerPrefix = prefix + "  ";
        writer.print(prefix);
        writer.println(TAG);
        writer.print(prefix);
        writer.println("Icon Cache");
        this.mIconCache.dump(innerPrefix, writer);
        writer.print(prefix);
        writer.println("Thumbnail Cache");
        this.mThumbnailCache.dump(innerPrefix, writer);
        writer.print(prefix);
        writer.println("Temp Thumbnail Cache");
        this.mTempCache.dump(innerPrefix, writer);
    }
}
