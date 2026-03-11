package com.android.systemui.recents;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.trust.TrustManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.Log;
import android.util.MutableBoolean;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Toast;
import com.android.internal.policy.DockedDividerUtils;
import com.android.systemui.Dependency;
import com.android.systemui.OverviewProxyService;
import com.android.systemui.R;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.DockedTopTaskEvent;
import com.android.systemui.recents.events.activity.EnterRecentsWindowLastAnimationFrameEvent;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.activity.LaunchMostRecentTaskRequestEvent;
import com.android.systemui.recents.events.activity.LaunchNextTaskRequestEvent;
import com.android.systemui.recents.events.activity.RecentsActivityStartingEvent;
import com.android.systemui.recents.events.activity.ToggleRecentsEvent;
import com.android.systemui.recents.events.component.ActivityPinnedEvent;
import com.android.systemui.recents.events.component.ActivityUnpinnedEvent;
import com.android.systemui.recents.events.component.HidePipMenuEvent;
import com.android.systemui.recents.events.ui.DraggingInRecentsEndedEvent;
import com.android.systemui.recents.events.ui.DraggingInRecentsEvent;
import com.android.systemui.recents.events.ui.TaskSnapshotChangedEvent;
import com.android.systemui.recents.misc.DozeTrigger;
import com.android.systemui.recents.misc.ForegroundThread;
import com.android.systemui.recents.misc.SysUiTaskStackChangeListener;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.views.TaskStackLayoutAlgorithm;
import com.android.systemui.recents.views.TaskStackView;
import com.android.systemui.recents.views.TaskViewHeader;
import com.android.systemui.recents.views.TaskViewTransform;
import com.android.systemui.recents.views.grid.TaskGridLayoutAlgorithm;
import com.android.systemui.shared.recents.model.RecentsTaskLoadPlan;
import com.android.systemui.shared.recents.model.RecentsTaskLoader;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.TaskStack;
import com.android.systemui.shared.recents.model.ThumbnailData;
import com.android.systemui.shared.recents.view.AppTransitionAnimationSpecCompat;
import com.android.systemui.shared.recents.view.AppTransitionAnimationSpecsFuture;
import com.android.systemui.shared.recents.view.RecentsTransition;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.statusbar.phone.StatusBar;
import com.google.android.collect.Lists;
import java.util.ArrayList;
import java.util.List;

public class RecentsImpl implements ActivityOptions.OnAnimationFinishedListener {
    protected static RecentsTaskLoadPlan sInstanceLoadPlan;
    private TaskStackLayoutAlgorithm mBackgroundLayoutAlgorithm;
    protected Context mContext;
    boolean mDraggingInRecents;
    private TaskStackView mDummyStackView;
    TaskViewHeader mHeaderBar;
    protected long mLastToggleTime;
    boolean mLaunchedWhileDocking;
    int mTaskBarHeight;
    TaskStackListenerImpl mTaskStackListener;
    protected boolean mTriggeredFromAltTab;
    private TrustManager mTrustManager;
    private static final ArraySet<Task.TaskKey> EMPTY_SET = new ArraySet<>();
    protected static long sLastPipTime = -1;
    private static boolean mWaitingForTransitionStart = false;
    private static boolean mToggleFollowingTransitionStart = true;
    private Runnable mResetToggleFlagListener = new Runnable() {
        @Override
        public void run() {
            RecentsImpl.this.setWaitingForTransitionStart(false);
        }
    };
    Rect mTmpBounds = new Rect();
    TaskViewTransform mTmpTransform = new TaskViewTransform();
    final Object mHeaderBarLock = new Object();
    DozeTrigger mFastAltTabTrigger = new DozeTrigger(225, new Runnable() {
        @Override
        public void run() {
            RecentsImpl.this.showRecents(RecentsImpl.this.mTriggeredFromAltTab, false, true, -1);
        }
    });
    private OverviewProxyService.OverviewProxyListener mOverviewProxyListener = new OverviewProxyService.OverviewProxyListener() {
        @Override
        public void onConnectionChanged(boolean z) {
            if (!z) {
                Recents.getTaskLoader().onTrimMemory(80);
            }
        }
    };
    private final TaskStack mEmptyTaskStack = new TaskStack();
    protected Handler mHandler = new Handler();

    class TaskStackListenerImpl extends SysUiTaskStackChangeListener {
        private OverviewProxyService mOverviewProxyService = (OverviewProxyService) Dependency.get(OverviewProxyService.class);

        public TaskStackListenerImpl() {
        }

        @Override
        public void onTaskStackChangedBackground() {
            if (!this.mOverviewProxyService.isEnabled() && checkCurrentUserId(RecentsImpl.this.mContext, false) && Recents.getConfiguration().svelteLevel == 0) {
                Rect windowRect = RecentsImpl.this.getWindowRect(null);
                if (windowRect.isEmpty()) {
                    return;
                }
                ActivityManager.RunningTaskInfo runningTask = ActivityManagerWrapper.getInstance().getRunningTask();
                RecentsTaskLoader taskLoader = Recents.getTaskLoader();
                RecentsTaskLoadPlan recentsTaskLoadPlan = new RecentsTaskLoadPlan(RecentsImpl.this.mContext);
                taskLoader.preloadTasks(recentsTaskLoadPlan, -1);
                TaskStack taskStack = recentsTaskLoadPlan.getTaskStack();
                RecentsActivityLaunchState recentsActivityLaunchState = new RecentsActivityLaunchState();
                RecentsTaskLoadPlan.Options options = new RecentsTaskLoadPlan.Options();
                synchronized (RecentsImpl.this.mBackgroundLayoutAlgorithm) {
                    RecentsImpl.this.updateDummyStackViewLayout(RecentsImpl.this.mBackgroundLayoutAlgorithm, taskStack, windowRect);
                    recentsActivityLaunchState.launchedFromApp = true;
                    RecentsImpl.this.mBackgroundLayoutAlgorithm.update(recentsTaskLoadPlan.getTaskStack(), RecentsImpl.EMPTY_SET, recentsActivityLaunchState, -1.0f);
                    TaskStackLayoutAlgorithm.VisibilityReport visibilityReportComputeStackVisibilityReport = RecentsImpl.this.mBackgroundLayoutAlgorithm.computeStackVisibilityReport(taskStack.getTasks());
                    options.runningTaskId = runningTask != null ? runningTask.id : -1;
                    options.numVisibleTasks = visibilityReportComputeStackVisibilityReport.numVisibleTasks;
                    options.numVisibleTaskThumbnails = visibilityReportComputeStackVisibilityReport.numVisibleThumbnails;
                    options.onlyLoadForCache = true;
                    options.onlyLoadPausedActivities = true;
                    options.loadThumbnails = true;
                }
                taskLoader.loadTasks(recentsTaskLoadPlan, options);
            }
        }

        @Override
        public void onActivityPinned(String str, int i, int i2, int i3) {
            if (!checkCurrentUserId(RecentsImpl.this.mContext, false)) {
                return;
            }
            Recents.getConfiguration().getLaunchState().launchedFromPipApp = true;
            Recents.getConfiguration().getLaunchState().launchedWithNextPipApp = false;
            EventBus.getDefault().send(new ActivityPinnedEvent(i2));
            RecentsImpl.consumeInstanceLoadPlan();
            RecentsImpl.sLastPipTime = System.currentTimeMillis();
        }

        @Override
        public void onActivityUnpinned() {
            if (!checkCurrentUserId(RecentsImpl.this.mContext, false)) {
                return;
            }
            EventBus.getDefault().send(new ActivityUnpinnedEvent());
            RecentsImpl.sLastPipTime = -1L;
        }

        @Override
        public void onTaskSnapshotChanged(int i, ThumbnailData thumbnailData) {
            if (!checkCurrentUserId(RecentsImpl.this.mContext, false)) {
                return;
            }
            EventBus.getDefault().send(new TaskSnapshotChangedEvent(i, thumbnailData));
        }
    }

    public RecentsImpl(Context context) {
        this.mContext = context;
        this.mBackgroundLayoutAlgorithm = new TaskStackLayoutAlgorithm(context, null);
        ForegroundThread.get();
        this.mTaskStackListener = new TaskStackListenerImpl();
        ActivityManagerWrapper.getInstance().registerTaskStackListener(this.mTaskStackListener);
        this.mDummyStackView = new TaskStackView(this.mContext);
        reloadResources();
        this.mTrustManager = (TrustManager) this.mContext.getSystemService("trust");
    }

    public void onBootCompleted() {
        if (((OverviewProxyService) Dependency.get(OverviewProxyService.class)).isEnabled()) {
            return;
        }
        RecentsTaskLoader taskLoader = Recents.getTaskLoader();
        RecentsTaskLoadPlan recentsTaskLoadPlan = new RecentsTaskLoadPlan(this.mContext);
        taskLoader.preloadTasks(recentsTaskLoadPlan, -1);
        RecentsTaskLoadPlan.Options options = new RecentsTaskLoadPlan.Options();
        options.numVisibleTasks = taskLoader.getIconCacheSize();
        options.numVisibleTaskThumbnails = taskLoader.getThumbnailCacheSize();
        options.onlyLoadForCache = true;
        taskLoader.loadTasks(recentsTaskLoadPlan, options);
    }

    public void onConfigurationChanged() {
        reloadResources();
        this.mDummyStackView.reloadOnConfigurationChange();
        synchronized (this.mBackgroundLayoutAlgorithm) {
            this.mBackgroundLayoutAlgorithm.reloadOnConfigurationChange(this.mContext);
        }
    }

    public void onVisibilityChanged(Context context, boolean z) {
        Recents.getSystemServices().setRecentsVisibility(z);
    }

    public void onStartScreenPinning(Context context, int i) {
        StatusBar statusBar = getStatusBar();
        if (statusBar != null) {
            statusBar.showScreenPinningRequest(i, false);
        }
    }

    public void showRecents(boolean z, boolean z2, boolean z3, int i) {
        Recents.getSystemServices();
        boolean z4 = true;
        MutableBoolean mutableBoolean = new MutableBoolean(true);
        boolean zIsRecentsActivityVisible = Recents.getSystemServices().isRecentsActivityVisible(mutableBoolean);
        boolean z5 = mutableBoolean.value;
        boolean z6 = Recents.getSystemServices().getSplitScreenPrimaryStack() != null;
        this.mTriggeredFromAltTab = z;
        this.mDraggingInRecents = z2;
        this.mLaunchedWhileDocking = z6;
        if (this.mFastAltTabTrigger.isAsleep()) {
            this.mFastAltTabTrigger.stopDozing();
        } else if (this.mFastAltTabTrigger.isDozing()) {
            if (!z) {
                return;
            } else {
                this.mFastAltTabTrigger.stopDozing();
            }
        } else if (z) {
            this.mFastAltTabTrigger.startDozing();
            return;
        }
        if ((z6 || z2) || !zIsRecentsActivityVisible) {
            try {
                ActivityManager.RunningTaskInfo runningTask = ActivityManagerWrapper.getInstance().getRunningTask();
                if (!mutableBoolean.value && !z5) {
                    z4 = false;
                }
                startRecentsActivityAndDismissKeyguardIfNeeded(runningTask, z4, z3, i);
            } catch (ActivityNotFoundException e) {
                Log.e("RecentsImpl", "Failed to launch RecentsActivity", e);
            }
        }
    }

    public void hideRecents(boolean z, boolean z2) {
        if (z && this.mFastAltTabTrigger.isDozing()) {
            showNextTask();
            this.mFastAltTabTrigger.stopDozing();
        } else {
            EventBus.getDefault().post(new HideRecentsEvent(z, z2));
        }
    }

    public void toggleRecents(int i) {
        if (ActivityManagerWrapper.getInstance().isScreenPinningActive() || this.mFastAltTabTrigger.isDozing()) {
            return;
        }
        if (mWaitingForTransitionStart) {
            mToggleFollowingTransitionStart = true;
            return;
        }
        boolean z = false;
        this.mDraggingInRecents = false;
        this.mLaunchedWhileDocking = false;
        this.mTriggeredFromAltTab = false;
        try {
            MutableBoolean mutableBoolean = new MutableBoolean(true);
            long jElapsedRealtime = SystemClock.elapsedRealtime() - this.mLastToggleTime;
            if (!Recents.getSystemServices().isRecentsActivityVisible(mutableBoolean)) {
                if (jElapsedRealtime < 350) {
                    return;
                }
                startRecentsActivityAndDismissKeyguardIfNeeded(ActivityManagerWrapper.getInstance().getRunningTask(), mutableBoolean.value, true, i);
                ActivityManagerWrapper.getInstance().closeSystemWindows("recentapps");
                this.mLastToggleTime = SystemClock.elapsedRealtime();
                return;
            }
            if (Recents.getConfiguration().getLaunchState().launchedWithAltTab) {
                if (jElapsedRealtime < 350) {
                    return;
                }
                EventBus.getDefault().post(new ToggleRecentsEvent());
                this.mLastToggleTime = SystemClock.elapsedRealtime();
                return;
            }
            if (Recents.getConfiguration().isGridEnabled) {
                if (jElapsedRealtime < ViewConfiguration.getDoubleTapTimeout()) {
                    z = true;
                }
                if (z) {
                    EventBus.getDefault().post(new LaunchNextTaskRequestEvent());
                } else {
                    EventBus.getDefault().post(new LaunchMostRecentTaskRequestEvent());
                }
                return;
            }
            EventBus.getDefault().post(new LaunchNextTaskRequestEvent());
        } catch (ActivityNotFoundException e) {
            Log.e("RecentsImpl", "Failed to launch RecentsActivity", e);
        }
    }

    public void preloadRecents() {
        if (ActivityManagerWrapper.getInstance().isScreenPinningActive()) {
            return;
        }
        StatusBar statusBar = getStatusBar();
        if (statusBar != null && statusBar.isKeyguardShowing()) {
            return;
        }
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                RecentsImpl.lambda$preloadRecents$0(this.f$0);
            }
        });
    }

    public static void lambda$preloadRecents$0(RecentsImpl recentsImpl) {
        ActivityManager.RunningTaskInfo runningTask;
        if (Recents.getSystemServices().isRecentsActivityVisible(null) || (runningTask = ActivityManagerWrapper.getInstance().getRunningTask()) == null) {
            return;
        }
        RecentsTaskLoader taskLoader = Recents.getTaskLoader();
        sInstanceLoadPlan = new RecentsTaskLoadPlan(recentsImpl.mContext);
        taskLoader.preloadTasks(sInstanceLoadPlan, runningTask.id);
        TaskStack taskStack = sInstanceLoadPlan.getTaskStack();
        if (taskStack.getTaskCount() > 0) {
            recentsImpl.preloadIcon(runningTask.id);
            recentsImpl.updateHeaderBarLayout(taskStack, null);
        }
    }

    public void cancelPreloadingRecents() {
    }

    public void onDraggingInRecents(float f) {
        EventBus.getDefault().sendOntoMainThread(new DraggingInRecentsEvent(f));
    }

    public void onDraggingInRecentsEnded(float f) {
        EventBus.getDefault().sendOntoMainThread(new DraggingInRecentsEndedEvent(f));
    }

    public void onShowCurrentUserToast(int i, int i2) {
        Toast.makeText(this.mContext, i, i2).show();
    }

    public void showNextTask() {
        ActivityManager.RunningTaskInfo runningTask;
        boolean z;
        Task task;
        ActivityOptions activityOptionsMakeCustomAnimation;
        SystemServicesProxy systemServices = Recents.getSystemServices();
        RecentsTaskLoader taskLoader = Recents.getTaskLoader();
        RecentsTaskLoadPlan recentsTaskLoadPlan = new RecentsTaskLoadPlan(this.mContext);
        taskLoader.preloadTasks(recentsTaskLoadPlan, -1);
        TaskStack taskStack = recentsTaskLoadPlan.getTaskStack();
        if (taskStack == null || taskStack.getTaskCount() == 0 || (runningTask = ActivityManagerWrapper.getInstance().getRunningTask()) == null) {
            return;
        }
        if (runningTask.configuration.windowConfiguration.getActivityType() != 2) {
            z = false;
        } else {
            z = true;
        }
        ArrayList<Task> tasks = taskStack.getTasks();
        int size = tasks.size() - 1;
        while (true) {
            if (size >= 1) {
                Task task2 = tasks.get(size);
                if (z) {
                    task = tasks.get(size - 1);
                    activityOptionsMakeCustomAnimation = ActivityOptions.makeCustomAnimation(this.mContext, R.anim.recents_launch_next_affiliated_task_target, R.anim.recents_fast_toggle_app_home_exit);
                    break;
                } else if (task2.key.id != runningTask.id) {
                    size--;
                } else {
                    task = tasks.get(size - 1);
                    activityOptionsMakeCustomAnimation = ActivityOptions.makeCustomAnimation(this.mContext, R.anim.recents_launch_prev_affiliated_task_target, R.anim.recents_launch_prev_affiliated_task_source);
                    break;
                }
            } else {
                task = null;
                activityOptionsMakeCustomAnimation = null;
                break;
            }
        }
        if (task == null) {
            systemServices.startInPlaceAnimationOnFrontMostApplication(ActivityOptions.makeCustomInPlaceAnimation(this.mContext, R.anim.recents_launch_prev_affiliated_task_bounce));
        } else {
            ActivityManagerWrapper.getInstance().startActivityFromRecentsAsync(task.key, activityOptionsMakeCustomAnimation, null, null);
        }
    }

    public void splitPrimaryTask(int i, int i2, int i3, Rect rect) {
        if (Recents.getSystemServices().setTaskWindowingModeSplitScreenPrimary(i, i3, rect)) {
            EventBus.getDefault().send(new DockedTopTaskEvent(i2, rect));
        }
    }

    public void setWaitingForTransitionStart(boolean z) {
        if (mWaitingForTransitionStart == z) {
            return;
        }
        mWaitingForTransitionStart = z;
        if (!z && mToggleFollowingTransitionStart) {
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.toggleRecents(-1);
                }
            });
        }
        mToggleFollowingTransitionStart = false;
    }

    public static RecentsTaskLoadPlan consumeInstanceLoadPlan() {
        RecentsTaskLoadPlan recentsTaskLoadPlan = sInstanceLoadPlan;
        sInstanceLoadPlan = null;
        return recentsTaskLoadPlan;
    }

    public static long getLastPipTime() {
        return sLastPipTime;
    }

    private void reloadResources() {
        Resources resources = this.mContext.getResources();
        this.mTaskBarHeight = TaskStackLayoutAlgorithm.getDimensionForDevice(this.mContext, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height_tablet_land, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height_tablet_land, R.dimen.recents_grid_task_view_header_height);
        this.mHeaderBar = (TaskViewHeader) LayoutInflater.from(this.mContext).inflate(R.layout.recents_task_view_header, (ViewGroup) null, false);
        this.mHeaderBar.setLayoutDirection(resources.getConfiguration().getLayoutDirection());
    }

    public void updateDummyStackViewLayout(TaskStackLayoutAlgorithm taskStackLayoutAlgorithm, TaskStack taskStack, Rect rect) {
        SystemServicesProxy systemServices = Recents.getSystemServices();
        Rect displayRect = systemServices.getDisplayRect();
        Rect rect2 = new Rect();
        systemServices.getStableInsets(rect2);
        if (systemServices.hasDockedTask()) {
            if (rect2.bottom < rect.height()) {
                rect.bottom -= rect2.bottom;
            }
            rect2.bottom = 0;
        }
        calculateWindowStableInsets(rect2, rect, displayRect);
        rect.offsetTo(0, 0);
        taskStackLayoutAlgorithm.setSystemInsets(rect2);
        if (taskStack != null) {
            taskStackLayoutAlgorithm.getTaskStackBounds(displayRect, rect, rect2.top, rect2.left, rect2.right, this.mTmpBounds);
            taskStackLayoutAlgorithm.reset();
            taskStackLayoutAlgorithm.initialize(displayRect, rect, this.mTmpBounds);
        }
    }

    public Rect getWindowRect(Rect rect) {
        if (rect != null) {
            return new Rect(rect);
        }
        return Recents.getSystemServices().getWindowRect();
    }

    private void updateHeaderBarLayout(TaskStack taskStack, Rect rect) {
        int iWidth;
        Rect windowRect = getWindowRect(rect);
        boolean zUseGridLayout = this.mDummyStackView.useGridLayout();
        updateDummyStackViewLayout(this.mDummyStackView.getStackAlgorithm(), taskStack, windowRect);
        if (taskStack != null) {
            TaskStackLayoutAlgorithm stackAlgorithm = this.mDummyStackView.getStackAlgorithm();
            this.mDummyStackView.getStack().removeAllTasks(false);
            this.mDummyStackView.setTasks(taskStack, false);
            if (zUseGridLayout) {
                TaskGridLayoutAlgorithm gridAlgorithm = this.mDummyStackView.getGridAlgorithm();
                gridAlgorithm.initialize(windowRect);
                iWidth = (int) gridAlgorithm.getTransform(0, taskStack.getTaskCount(), new TaskViewTransform(), stackAlgorithm).rect.width();
            } else {
                Rect untransformedTaskViewBounds = stackAlgorithm.getUntransformedTaskViewBounds();
                if (!untransformedTaskViewBounds.isEmpty()) {
                    iWidth = untransformedTaskViewBounds.width();
                } else {
                    iWidth = 0;
                }
            }
        }
        if (taskStack != null && iWidth > 0) {
            synchronized (this.mHeaderBarLock) {
                if (this.mHeaderBar.getMeasuredWidth() != iWidth || this.mHeaderBar.getMeasuredHeight() != this.mTaskBarHeight) {
                    if (zUseGridLayout) {
                        this.mHeaderBar.setShouldDarkenBackgroundColor(true);
                        this.mHeaderBar.setNoUserInteractionState();
                    }
                    this.mHeaderBar.forceLayout();
                    this.mHeaderBar.measure(View.MeasureSpec.makeMeasureSpec(iWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(this.mTaskBarHeight, 1073741824));
                }
                this.mHeaderBar.layout(0, 0, iWidth, this.mTaskBarHeight);
            }
        }
    }

    private void calculateWindowStableInsets(Rect rect, Rect rect2, Rect rect3) {
        Rect rect4 = new Rect(rect3);
        rect4.inset(rect);
        Rect rect5 = new Rect(rect2);
        rect5.intersect(rect4);
        rect.left = rect5.left - rect2.left;
        rect.top = rect5.top - rect2.top;
        rect.right = rect2.right - rect5.right;
        rect.bottom = rect2.bottom - rect5.bottom;
    }

    private void preloadIcon(int i) {
        RecentsTaskLoadPlan.Options options = new RecentsTaskLoadPlan.Options();
        options.runningTaskId = i;
        options.loadThumbnails = false;
        options.onlyLoadForCache = true;
        Recents.getTaskLoader().loadTasks(sInstanceLoadPlan, options);
    }

    protected ActivityOptions getUnknownTransitionActivityOptions() {
        return ActivityOptions.makeCustomAnimation(this.mContext, R.anim.recents_from_unknown_enter, R.anim.recents_from_unknown_exit, this.mHandler, null);
    }

    protected ActivityOptions getHomeTransitionActivityOptions() {
        return ActivityOptions.makeCustomAnimation(this.mContext, R.anim.recents_from_launcher_enter, R.anim.recents_from_launcher_exit, this.mHandler, null);
    }

    private Pair<ActivityOptions, AppTransitionAnimationSpecsFuture> getThumbnailTransitionActivityOptions(ActivityManager.RunningTaskInfo runningTaskInfo, Rect rect) {
        boolean z = Recents.getConfiguration().isLowRamDevice;
        final Task task = new Task();
        final TaskViewTransform thumbnailTransitionTransform = getThumbnailTransitionTransform(this.mDummyStackView, task, rect);
        final RectF rectF = thumbnailTransitionTransform.rect;
        AppTransitionAnimationSpecsFuture appTransitionAnimationSpecsFuture = new AppTransitionAnimationSpecsFuture(this.mHandler) {
            @Override
            public List<AppTransitionAnimationSpecCompat> composeSpecs() {
                Rect rect2 = new Rect();
                rectF.round(rect2);
                return Lists.newArrayList(new AppTransitionAnimationSpecCompat[]{new AppTransitionAnimationSpecCompat(task.key.id, RecentsImpl.this.drawThumbnailTransitionBitmap(task, thumbnailTransitionTransform), rect2)});
            }
        };
        return new Pair<>(RecentsTransition.createAspectScaleAnimation(this.mContext, this.mHandler, false, appTransitionAnimationSpecsFuture, z ? null : this.mResetToggleFlagListener), appTransitionAnimationSpecsFuture);
    }

    private TaskViewTransform getThumbnailTransitionTransform(TaskStackView taskStackView, Task task, Rect rect) {
        TaskStack stack = taskStackView.getStack();
        Task launchTarget = stack.getLaunchTarget();
        if (launchTarget != null) {
            task.copyFrom(launchTarget);
        } else {
            launchTarget = stack.getFrontMostTask();
            task.copyFrom(launchTarget);
        }
        Task task2 = launchTarget;
        taskStackView.updateLayoutAlgorithm(true);
        taskStackView.updateToInitialState();
        taskStackView.getStackAlgorithm().getStackTransformScreenCoordinates(task2, taskStackView.getScroller().getStackScroll(), this.mTmpTransform, null, rect);
        return this.mTmpTransform;
    }

    public Bitmap drawThumbnailTransitionBitmap(Task task, TaskViewTransform taskViewTransform) {
        Bitmap bitmapDrawViewIntoHardwareBitmap;
        SystemServicesProxy systemServices = Recents.getSystemServices();
        int iWidth = (int) taskViewTransform.rect.width();
        int iHeight = (int) taskViewTransform.rect.height();
        if (taskViewTransform == null || task.key == null || iWidth <= 0 || iHeight <= 0) {
            return null;
        }
        synchronized (this.mHeaderBarLock) {
            boolean z = !task.isSystemApp && systemServices.isInSafeMode();
            this.mHeaderBar.onTaskViewSizeChanged(iWidth, iHeight);
            Drawable drawable = this.mHeaderBar.getIconView().getDrawable();
            if (drawable != null) {
                drawable.setCallback(null);
            }
            this.mHeaderBar.bindToTask(task, false, z);
            this.mHeaderBar.onTaskDataLoaded();
            this.mHeaderBar.setDimAlpha(taskViewTransform.dimAlpha);
            bitmapDrawViewIntoHardwareBitmap = RecentsTransition.drawViewIntoHardwareBitmap(iWidth, this.mTaskBarHeight, this.mHeaderBar, 1.0f, 0);
        }
        return bitmapDrawViewIntoHardwareBitmap;
    }

    protected void startRecentsActivityAndDismissKeyguardIfNeeded(final ActivityManager.RunningTaskInfo runningTaskInfo, final boolean z, final boolean z2, final int i) {
        StatusBar statusBar = getStatusBar();
        if (statusBar != null && statusBar.isKeyguardShowing()) {
            statusBar.executeRunnableDismissingKeyguard(new Runnable() {
                @Override
                public final void run() {
                    RecentsImpl.lambda$startRecentsActivityAndDismissKeyguardIfNeeded$3(this.f$0, runningTaskInfo, z, z2, i);
                }
            }, null, true, false, true);
        } else {
            startRecentsActivity(runningTaskInfo, z, z2, i);
        }
    }

    public static void lambda$startRecentsActivityAndDismissKeyguardIfNeeded$3(final RecentsImpl recentsImpl, final ActivityManager.RunningTaskInfo runningTaskInfo, final boolean z, final boolean z2, final int i) {
        recentsImpl.mTrustManager.reportKeyguardShowingChanged();
        recentsImpl.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.startRecentsActivity(runningTaskInfo, z, z2, i);
            }
        });
    }

    public void startRecentsActivity(ActivityManager.RunningTaskInfo runningTaskInfo, boolean z, boolean z2, int i) {
        int i2;
        Pair<ActivityOptions, AppTransitionAnimationSpecsFuture> pair;
        ActivityOptions unknownTransitionActivityOptions;
        RecentsTaskLoader taskLoader = Recents.getTaskLoader();
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        if (!this.mLaunchedWhileDocking && runningTaskInfo != null) {
            i2 = runningTaskInfo.id;
        } else {
            i2 = -1;
        }
        if (this.mLaunchedWhileDocking || this.mTriggeredFromAltTab || sInstanceLoadPlan == null) {
            sInstanceLoadPlan = new RecentsTaskLoadPlan(this.mContext);
        }
        if (this.mLaunchedWhileDocking || this.mTriggeredFromAltTab || !sInstanceLoadPlan.hasTasks()) {
            taskLoader.preloadTasks(sInstanceLoadPlan, i2);
        }
        TaskStack taskStack = sInstanceLoadPlan.getTaskStack();
        boolean z3 = true;
        boolean z4 = taskStack.getTaskCount() > 0;
        boolean z5 = (runningTaskInfo == null || z || !z4) ? false : true;
        launchState.launchedFromHome = (z5 || this.mLaunchedWhileDocking) ? false : true;
        if (!z5 && !this.mLaunchedWhileDocking) {
            z3 = false;
        }
        launchState.launchedFromApp = z3;
        launchState.launchedFromPipApp = false;
        launchState.launchedWithNextPipApp = taskStack.isNextLaunchTargetPip(getLastPipTime());
        launchState.launchedViaDockGesture = this.mLaunchedWhileDocking;
        launchState.launchedViaDragGesture = this.mDraggingInRecents;
        launchState.launchedToTaskId = i2;
        launchState.launchedWithAltTab = this.mTriggeredFromAltTab;
        setWaitingForTransitionStart(z5);
        preloadIcon(i2);
        Rect windowRectOverride = getWindowRectOverride(i);
        updateHeaderBarLayout(taskStack, windowRectOverride);
        TaskStackLayoutAlgorithm.VisibilityReport visibilityReportComputeStackVisibilityReport = this.mDummyStackView.computeStackVisibilityReport();
        launchState.launchedNumVisibleTasks = visibilityReportComputeStackVisibilityReport.numVisibleTasks;
        launchState.launchedNumVisibleThumbnails = visibilityReportComputeStackVisibilityReport.numVisibleThumbnails;
        if (!z2) {
            startRecentsActivity(ActivityOptions.makeCustomAnimation(this.mContext, -1, -1), null);
            return;
        }
        if (z5) {
            pair = getThumbnailTransitionActivityOptions(runningTaskInfo, windowRectOverride);
        } else {
            if (z4) {
                unknownTransitionActivityOptions = getHomeTransitionActivityOptions();
            } else {
                unknownTransitionActivityOptions = getUnknownTransitionActivityOptions();
            }
            pair = new Pair<>(unknownTransitionActivityOptions, null);
        }
        startRecentsActivity((ActivityOptions) pair.first, (AppTransitionAnimationSpecsFuture) pair.second);
        this.mLastToggleTime = SystemClock.elapsedRealtime();
    }

    private Rect getWindowRectOverride(int i) {
        if (i == -1) {
            return SystemServicesProxy.getInstance(this.mContext).getWindowRect();
        }
        Rect rect = new Rect();
        Rect displayRect = Recents.getSystemServices().getDisplayRect();
        DockedDividerUtils.calculateBoundsForPosition(i, 4, rect, displayRect.width(), displayRect.height(), Recents.getSystemServices().getDockedDividerSize(this.mContext));
        return rect;
    }

    private StatusBar getStatusBar() {
        return (StatusBar) ((SystemUIApplication) this.mContext).getComponent(StatusBar.class);
    }

    private void startRecentsActivity(final ActivityOptions activityOptions, final AppTransitionAnimationSpecsFuture appTransitionAnimationSpecsFuture) {
        final Intent intent = new Intent();
        intent.setClassName("com.android.systemui", "com.android.systemui.recents.RecentsActivity");
        intent.setFlags(276840448);
        HidePipMenuEvent hidePipMenuEvent = new HidePipMenuEvent();
        hidePipMenuEvent.addPostAnimationCallback(new Runnable() {
            @Override
            public final void run() {
                RecentsImpl.lambda$startRecentsActivity$4(intent, activityOptions, appTransitionAnimationSpecsFuture);
            }
        });
        EventBus.getDefault().send(hidePipMenuEvent);
        this.mDummyStackView.setTasks(this.mEmptyTaskStack, false);
    }

    static void lambda$startRecentsActivity$4(Intent intent, ActivityOptions activityOptions, AppTransitionAnimationSpecsFuture appTransitionAnimationSpecsFuture) {
        Recents.getSystemServices().startActivityAsUserAsync(intent, activityOptions);
        EventBus.getDefault().send(new RecentsActivityStartingEvent());
        if (appTransitionAnimationSpecsFuture != null) {
            appTransitionAnimationSpecsFuture.composeSpecsSynchronous();
        }
    }

    public void onAnimationFinished() {
        EventBus.getDefault().post(new EnterRecentsWindowLastAnimationFrameEvent());
    }
}
