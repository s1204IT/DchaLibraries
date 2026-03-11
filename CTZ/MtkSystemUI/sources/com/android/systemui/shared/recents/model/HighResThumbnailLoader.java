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

    public HighResThumbnailLoader(ActivityManagerWrapper activityManagerWrapper, Looper looper, boolean z) {
        this.mActivityManager = activityManagerWrapper;
        this.mMainThreadHandler = new Handler(looper);
        this.mLoadThread.start();
        this.mIsLowRamDevice = z;
    }

    public void setVisible(boolean z) {
        if (this.mIsLowRamDevice) {
            return;
        }
        this.mVisible = z;
        updateLoading();
    }

    public void setFlingingFast(boolean z) {
        if (this.mFlingingFast == z || this.mIsLowRamDevice) {
            return;
        }
        this.mFlingingFast = z;
        updateLoading();
    }

    public void setTaskLoadQueueIdle(boolean z) {
        if (this.mIsLowRamDevice) {
            return;
        }
        this.mTaskLoadQueueIdle = z;
        updateLoading();
    }

    @VisibleForTesting
    boolean isLoading() {
        return this.mLoading;
    }

    private void updateLoading() {
        setLoading(this.mVisible && !this.mFlingingFast && this.mTaskLoadQueueIdle);
    }

    private void setLoading(boolean z) {
        if (z == this.mLoading) {
            return;
        }
        synchronized (this.mLoadQueue) {
            this.mLoading = z;
            if (!z) {
                stopLoading();
            } else {
                startLoading();
            }
        }
    }

    @GuardedBy("mLoadQueue")
    private void startLoading() {
        for (int size = this.mVisibleTasks.size() - 1; size >= 0; size--) {
            Task task = this.mVisibleTasks.get(size);
            if ((task.thumbnail == null || task.thumbnail.reducedResolution) && !this.mLoadQueue.contains(task) && !this.mLoadingTasks.contains(task)) {
                this.mLoadQueue.add(task);
            }
        }
        this.mLoadQueue.notifyAll();
    }

    @GuardedBy("mLoadQueue")
    private void stopLoading() {
        this.mLoadQueue.clear();
        this.mLoadQueue.notifyAll();
    }

    public void onTaskVisible(Task task) {
        task.addCallback(this);
        this.mVisibleTasks.add(task);
        if ((task.thumbnail == null || task.thumbnail.reducedResolution) && this.mLoading) {
            synchronized (this.mLoadQueue) {
                this.mLoadQueue.add(task);
                this.mLoadQueue.notifyAll();
            }
        }
    }

    public void onTaskInvisible(Task task) {
        task.removeCallback(this);
        this.mVisibleTasks.remove(task);
        synchronized (this.mLoadQueue) {
            this.mLoadQueue.remove(task);
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

    @Override
    public void onTaskDataLoaded(Task task, ThumbnailData thumbnailData) {
        if (thumbnailData != null && !thumbnailData.reducedResolution) {
            synchronized (this.mLoadQueue) {
                this.mLoadQueue.remove(task);
            }
        }
    }

    @Override
    public void onTaskDataUnloaded() {
    }

    class AnonymousClass1 implements Runnable {
        AnonymousClass1() {
        }

        @Override
        public void run() {
            Process.setThreadPriority(11);
            while (true) {
                Task task = null;
                synchronized (HighResThumbnailLoader.this.mLoadQueue) {
                    if (!HighResThumbnailLoader.this.mLoading || HighResThumbnailLoader.this.mLoadQueue.isEmpty()) {
                        try {
                            HighResThumbnailLoader.this.mLoaderIdling = true;
                            HighResThumbnailLoader.this.mLoadQueue.wait();
                            HighResThumbnailLoader.this.mLoaderIdling = false;
                        } catch (InterruptedException e) {
                        }
                    } else {
                        task = (Task) HighResThumbnailLoader.this.mLoadQueue.poll();
                        if (task != null) {
                            HighResThumbnailLoader.this.mLoadingTasks.add(task);
                        }
                    }
                }
                if (task != null) {
                    loadTask(task);
                }
            }
        }

        private void loadTask(final Task task) {
            final ThumbnailData taskThumbnail = HighResThumbnailLoader.this.mActivityManager.getTaskThumbnail(task.key.id, false);
            HighResThumbnailLoader.this.mMainThreadHandler.post(new Runnable() {
                @Override
                public final void run() {
                    HighResThumbnailLoader.AnonymousClass1.lambda$loadTask$0(this.f$0, task, taskThumbnail);
                }
            });
        }

        public static void lambda$loadTask$0(AnonymousClass1 anonymousClass1, Task task, ThumbnailData thumbnailData) {
            synchronized (HighResThumbnailLoader.this.mLoadQueue) {
                HighResThumbnailLoader.this.mLoadingTasks.remove(task);
            }
            if (HighResThumbnailLoader.this.mVisibleTasks.contains(task)) {
                task.notifyTaskDataLoaded(thumbnailData, task.icon);
            }
        }
    }
}
