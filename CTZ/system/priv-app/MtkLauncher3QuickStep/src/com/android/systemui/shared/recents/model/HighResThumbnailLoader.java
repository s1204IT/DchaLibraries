package com.android.systemui.shared.recents.model;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.util.ArraySet;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.shared.recents.model.HighResThumbnailLoader;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import java.util.ArrayDeque;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class HighResThumbnailLoader implements Task.TaskCallbacks {
    private final ActivityManagerWrapper mActivityManager;
    private boolean mFlingingFast;
    private final boolean mIsLowRamDevice;

    @GuardedBy("mLoadQueue")
    private boolean mLoaderIdling;
    private boolean mLoading;
    private final Handler mMainThreadHandler;
    private boolean mTaskLoadQueueIdle;
    private boolean mVisible;

    @GuardedBy("mLoadQueue")
    private final ArrayDeque<Task> mLoadQueue = new ArrayDeque<>();

    @GuardedBy("mLoadQueue")
    private final ArraySet<Task> mLoadingTasks = new ArraySet<>();
    private final ArrayList<Task> mVisibleTasks = new ArrayList<>();
    private final Runnable mLoader = new AnonymousClass1();
    private final Thread mLoadThread = new Thread(this.mLoader, "Recents-HighResThumbnailLoader");

    public HighResThumbnailLoader(ActivityManagerWrapper activityManager, Looper looper, boolean isLowRamDevice) {
        this.mActivityManager = activityManager;
        this.mMainThreadHandler = new Handler(looper);
        this.mLoadThread.start();
        this.mIsLowRamDevice = isLowRamDevice;
    }

    public void setVisible(boolean visible) {
        if (this.mIsLowRamDevice) {
            return;
        }
        this.mVisible = visible;
        updateLoading();
    }

    public void setFlingingFast(boolean flingingFast) {
        if (this.mFlingingFast == flingingFast || this.mIsLowRamDevice) {
            return;
        }
        this.mFlingingFast = flingingFast;
        updateLoading();
    }

    public void setTaskLoadQueueIdle(boolean idle) {
        if (this.mIsLowRamDevice) {
            return;
        }
        this.mTaskLoadQueueIdle = idle;
        updateLoading();
    }

    @VisibleForTesting
    boolean isLoading() {
        return this.mLoading;
    }

    private void updateLoading() {
        setLoading(this.mVisible && !this.mFlingingFast && this.mTaskLoadQueueIdle);
    }

    private void setLoading(boolean loading) {
        if (loading == this.mLoading) {
            return;
        }
        synchronized (this.mLoadQueue) {
            this.mLoading = loading;
            if (!loading) {
                stopLoading();
            } else {
                startLoading();
            }
        }
    }

    @GuardedBy("mLoadQueue")
    private void startLoading() {
        for (int i = this.mVisibleTasks.size() - 1; i >= 0; i--) {
            Task t = this.mVisibleTasks.get(i);
            if ((t.thumbnail == null || t.thumbnail.reducedResolution) && !this.mLoadQueue.contains(t) && !this.mLoadingTasks.contains(t)) {
                this.mLoadQueue.add(t);
            }
        }
        this.mLoadQueue.notifyAll();
    }

    @GuardedBy("mLoadQueue")
    private void stopLoading() {
        this.mLoadQueue.clear();
        this.mLoadQueue.notifyAll();
    }

    public void onTaskVisible(Task t) {
        t.addCallback(this);
        this.mVisibleTasks.add(t);
        if ((t.thumbnail == null || t.thumbnail.reducedResolution) && this.mLoading) {
            synchronized (this.mLoadQueue) {
                this.mLoadQueue.add(t);
                this.mLoadQueue.notifyAll();
            }
        }
    }

    public void onTaskInvisible(Task t) {
        t.removeCallback(this);
        this.mVisibleTasks.remove(t);
        synchronized (this.mLoadQueue) {
            this.mLoadQueue.remove(t);
        }
    }

    @VisibleForTesting
    void waitForLoaderIdle() {
        while (true) {
            synchronized (this.mLoadQueue) {
                if (this.mLoadQueue.isEmpty() && this.mLoaderIdling) {
                    return;
                }
            }
            SystemClock.sleep(100L);
        }
    }

    @Override // com.android.systemui.shared.recents.model.Task.TaskCallbacks
    public void onTaskDataLoaded(Task task, ThumbnailData thumbnailData) {
        if (thumbnailData != null && !thumbnailData.reducedResolution) {
            synchronized (this.mLoadQueue) {
                this.mLoadQueue.remove(task);
            }
        }
    }

    @Override // com.android.systemui.shared.recents.model.Task.TaskCallbacks
    public void onTaskDataUnloaded() {
    }

    @Override // com.android.systemui.shared.recents.model.Task.TaskCallbacks
    public void onTaskWindowingModeChanged() {
    }

    /* renamed from: com.android.systemui.shared.recents.model.HighResThumbnailLoader$1, reason: invalid class name */
    class AnonymousClass1 implements Runnable {
        AnonymousClass1() {
        }

        @Override // java.lang.Runnable
        public void run() throws SecurityException, IllegalArgumentException {
            Process.setThreadPriority(11);
            while (true) {
                Task next = null;
                synchronized (HighResThumbnailLoader.this.mLoadQueue) {
                    if (!HighResThumbnailLoader.this.mLoading || HighResThumbnailLoader.this.mLoadQueue.isEmpty()) {
                        try {
                            HighResThumbnailLoader.this.mLoaderIdling = true;
                            HighResThumbnailLoader.this.mLoadQueue.wait();
                            HighResThumbnailLoader.this.mLoaderIdling = false;
                        } catch (InterruptedException e) {
                        }
                    } else {
                        next = (Task) HighResThumbnailLoader.this.mLoadQueue.poll();
                        if (next != null) {
                            HighResThumbnailLoader.this.mLoadingTasks.add(next);
                        }
                    }
                }
                if (next != null) {
                    loadTask(next);
                }
            }
        }

        private void loadTask(final Task t) {
            final ThumbnailData thumbnail = HighResThumbnailLoader.this.mActivityManager.getTaskThumbnail(t.key.id, false);
            HighResThumbnailLoader.this.mMainThreadHandler.post(new Runnable() { // from class: com.android.systemui.shared.recents.model.-$$Lambda$HighResThumbnailLoader$1$s-1KK0EnA0WJuK_oehEz11H5MbU
                @Override // java.lang.Runnable
                public final void run() {
                    HighResThumbnailLoader.AnonymousClass1.lambda$loadTask$0(this.f$0, t, thumbnail);
                }
            });
        }

        public static /* synthetic */ void lambda$loadTask$0(AnonymousClass1 anonymousClass1, Task t, ThumbnailData thumbnail) {
            synchronized (HighResThumbnailLoader.this.mLoadQueue) {
                HighResThumbnailLoader.this.mLoadingTasks.remove(t);
            }
            if (HighResThumbnailLoader.this.mVisibleTasks.contains(t)) {
                t.notifyTaskDataLoaded(thumbnail, t.icon);
            }
        }
    }
}
