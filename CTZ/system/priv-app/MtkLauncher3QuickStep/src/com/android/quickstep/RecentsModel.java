package com.android.quickstep;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.util.LruCache;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityManager;
import com.android.launcher3.MainThreadExecutor;
import com.android.launcher3.R;
import com.android.launcher3.util.Preconditions;
import com.android.systemui.shared.recents.ISystemUiProxy;
import com.android.systemui.shared.recents.model.IconLoader;
import com.android.systemui.shared.recents.model.RecentsTaskLoadPlan;
import com.android.systemui.shared.recents.model.RecentsTaskLoader;
import com.android.systemui.shared.recents.model.TaskKeyLruCache;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.BackgroundExecutor;
import com.android.systemui.shared.system.TaskStackChangeListener;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@TargetApi(26)
/* loaded from: classes.dex */
public class RecentsModel extends TaskStackChangeListener {
    private static RecentsModel INSTANCE;
    private final AccessibilityManager mAccessibilityManager;
    private final Context mContext;
    private final boolean mIsLowRamDevice;
    private RecentsTaskLoadPlan mLastLoadPlan;
    private int mLastLoadPlanId;
    private boolean mPreloadTasksInBackground;
    private final RecentsTaskLoader mRecentsTaskLoader;
    private ISystemUiProxy mSystemUiProxy;
    private int mTaskChangeId;
    private final SparseArray<Bundle> mCachedAssistData = new SparseArray<>(1);
    private final ArrayList<AssistDataListener> mAssistDataListeners = new ArrayList<>();
    private boolean mClearAssistCacheOnStackChange = true;
    private final MainThreadExecutor mMainThreadExecutor = new MainThreadExecutor();

    public interface AssistDataListener {
        void onAssistDataReceived(int i);
    }

    public static RecentsModel getInstance(final Context context) {
        if (INSTANCE == null) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                INSTANCE = new RecentsModel(context.getApplicationContext());
            } else {
                try {
                    return (RecentsModel) new MainThreadExecutor().submit(new Callable() { // from class: com.android.quickstep.-$$Lambda$RecentsModel$rpZwGbzmm_8iDmpR9nLZpbXBWE4
                        @Override // java.util.concurrent.Callable
                        public final Object call() {
                            return RecentsModel.getInstance(context);
                        }
                    }).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return INSTANCE;
    }

    private RecentsModel(Context context) {
        this.mContext = context;
        this.mIsLowRamDevice = ((ActivityManager) context.getSystemService("activity")).isLowRamDevice();
        Resources resources = context.getResources();
        this.mRecentsTaskLoader = new RecentsTaskLoader(this.mContext, resources.getInteger(R.integer.config_recentsMaxThumbnailCacheSize), resources.getInteger(R.integer.config_recentsMaxIconCacheSize), 0) { // from class: com.android.quickstep.RecentsModel.1
            @Override // com.android.systemui.shared.recents.model.RecentsTaskLoader
            protected IconLoader createNewIconLoader(Context context2, TaskKeyLruCache<Drawable> taskKeyLruCache, LruCache<ComponentName, ActivityInfo> lruCache) {
                return new NormalizedIconLoader(context2, taskKeyLruCache, lruCache);
            }
        };
        this.mRecentsTaskLoader.startLoader(this.mContext);
        ActivityManagerWrapper.getInstance().registerTaskStackListener(this);
        this.mTaskChangeId = 1;
        loadTasks(-1, null);
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService(AccessibilityManager.class);
    }

    public RecentsTaskLoader getRecentsTaskLoader() {
        return this.mRecentsTaskLoader;
    }

    public int loadTasks(final int i, final Consumer<RecentsTaskLoadPlan> consumer) {
        final int i2 = this.mTaskChangeId;
        if (this.mLastLoadPlanId == this.mTaskChangeId) {
            if (consumer != null) {
                final RecentsTaskLoadPlan recentsTaskLoadPlan = this.mLastLoadPlan;
                this.mMainThreadExecutor.execute(new Runnable() { // from class: com.android.quickstep.-$$Lambda$RecentsModel$rZAN1-eRHSYycmOWL-UzK9dzJ1Q
                    @Override // java.lang.Runnable
                    public final void run() {
                        consumer.accept(recentsTaskLoadPlan);
                    }
                });
            }
            return i2;
        }
        BackgroundExecutor.get().submit(new Runnable() { // from class: com.android.quickstep.-$$Lambda$RecentsModel$TrXIU6ZxHTUnPVNnZksG925eyyw
            @Override // java.lang.Runnable
            public final void run() {
                RecentsModel.lambda$loadTasks$3(this.f$0, i, i2, consumer);
            }
        });
        return i2;
    }

    public static /* synthetic */ void lambda$loadTasks$3(final RecentsModel recentsModel, int i, final int i2, final Consumer consumer) {
        final RecentsTaskLoadPlan recentsTaskLoadPlan = new RecentsTaskLoadPlan(recentsModel.mContext);
        RecentsTaskLoadPlan.PreloadOptions preloadOptions = new RecentsTaskLoadPlan.PreloadOptions();
        preloadOptions.loadTitles = recentsModel.mAccessibilityManager.isEnabled();
        recentsTaskLoadPlan.preloadPlan(preloadOptions, recentsModel.mRecentsTaskLoader, i, UserHandle.myUserId());
        recentsModel.mMainThreadExecutor.execute(new Runnable() { // from class: com.android.quickstep.-$$Lambda$RecentsModel$eCIpmjyPaNRHj2iYjydHcwF4Xp8
            @Override // java.lang.Runnable
            public final void run() {
                RecentsModel.lambda$loadTasks$2(this.f$0, recentsTaskLoadPlan, i2, consumer);
            }
        });
    }

    public static /* synthetic */ void lambda$loadTasks$2(RecentsModel recentsModel, RecentsTaskLoadPlan recentsTaskLoadPlan, int i, Consumer consumer) {
        recentsModel.mLastLoadPlan = recentsTaskLoadPlan;
        recentsModel.mLastLoadPlanId = i;
        if (consumer != null) {
            consumer.accept(recentsTaskLoadPlan);
        }
    }

    public void setPreloadTasksInBackground(boolean z) {
        this.mPreloadTasksInBackground = z && !this.mIsLowRamDevice;
    }

    @Override // com.android.systemui.shared.system.TaskStackChangeListener
    public void onActivityPinned(String str, int i, int i2, int i3) {
        this.mTaskChangeId++;
    }

    @Override // com.android.systemui.shared.system.TaskStackChangeListener
    public void onActivityUnpinned() {
        this.mTaskChangeId++;
    }

    @Override // com.android.systemui.shared.system.TaskStackChangeListener
    public void onTaskStackChanged() {
        this.mTaskChangeId++;
        Preconditions.assertUIThread();
        if (this.mClearAssistCacheOnStackChange) {
            this.mCachedAssistData.clear();
        } else {
            this.mClearAssistCacheOnStackChange = true;
        }
    }

    @Override // com.android.systemui.shared.system.TaskStackChangeListener
    public void onTaskStackChangedBackground() {
        int iMyUserId = UserHandle.myUserId();
        if (!this.mPreloadTasksInBackground || !TaskUtils.checkCurrentOrManagedUserId(iMyUserId, this.mContext)) {
            return;
        }
        ActivityManager.RunningTaskInfo runningTask = ActivityManagerWrapper.getInstance().getRunningTask();
        RecentsTaskLoadPlan recentsTaskLoadPlan = new RecentsTaskLoadPlan(this.mContext);
        RecentsTaskLoadPlan.Options options = new RecentsTaskLoadPlan.Options();
        options.runningTaskId = runningTask != null ? runningTask.id : -1;
        options.numVisibleTasks = 2;
        options.numVisibleTaskThumbnails = 2;
        options.onlyLoadForCache = true;
        options.onlyLoadPausedActivities = true;
        options.loadThumbnails = true;
        RecentsTaskLoadPlan.PreloadOptions preloadOptions = new RecentsTaskLoadPlan.PreloadOptions();
        preloadOptions.loadTitles = this.mAccessibilityManager.isEnabled();
        recentsTaskLoadPlan.preloadPlan(preloadOptions, this.mRecentsTaskLoader, -1, iMyUserId);
        this.mRecentsTaskLoader.loadTasks(recentsTaskLoadPlan, options);
    }

    public boolean isLoadPlanValid(int i) {
        return this.mTaskChangeId == i;
    }

    public RecentsTaskLoadPlan getLastLoadPlan() {
        return this.mLastLoadPlan;
    }

    public void setSystemUiProxy(ISystemUiProxy iSystemUiProxy) {
        this.mSystemUiProxy = iSystemUiProxy;
    }

    public ISystemUiProxy getSystemUiProxy() {
        return this.mSystemUiProxy;
    }

    public void onStart() {
        this.mRecentsTaskLoader.startLoader(this.mContext);
        this.mRecentsTaskLoader.getHighResThumbnailLoader().setVisible(true);
    }

    public void onTrimMemory(int i) {
        if (i == 20) {
            this.mRecentsTaskLoader.getHighResThumbnailLoader().setVisible(false);
        }
        this.mRecentsTaskLoader.onTrimMemory(i);
    }

    public void onOverviewShown(boolean z, String str) {
        if (this.mSystemUiProxy == null) {
            return;
        }
        try {
            this.mSystemUiProxy.onOverviewShown(z);
        } catch (RemoteException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Failed to notify SysUI of overview shown from ");
            sb.append(z ? "home" : "app");
            sb.append(": ");
            Log.w(str, sb.toString(), e);
        }
    }

    @WorkerThread
    public void preloadAssistData(final int i, final Bundle bundle) {
        this.mMainThreadExecutor.execute(new Runnable() { // from class: com.android.quickstep.-$$Lambda$RecentsModel$RaFBhHlycM2m2KiZfBOTPCQlHu8
            @Override // java.lang.Runnable
            public final void run() {
                RecentsModel.lambda$preloadAssistData$4(this.f$0, i, bundle);
            }
        });
    }

    public static /* synthetic */ void lambda$preloadAssistData$4(RecentsModel recentsModel, int i, Bundle bundle) {
        recentsModel.mCachedAssistData.put(i, bundle);
        recentsModel.mClearAssistCacheOnStackChange = false;
        int size = recentsModel.mAssistDataListeners.size();
        for (int i2 = 0; i2 < size; i2++) {
            recentsModel.mAssistDataListeners.get(i2).onAssistDataReceived(i);
        }
    }

    public Bundle getAssistData(int i) {
        Preconditions.assertUIThread();
        return this.mCachedAssistData.get(i);
    }

    public void addAssistDataListener(AssistDataListener assistDataListener) {
        this.mAssistDataListeners.add(assistDataListener);
    }

    public void removeAssistDataListener(AssistDataListener assistDataListener) {
        this.mAssistDataListeners.remove(assistDataListener);
    }
}
