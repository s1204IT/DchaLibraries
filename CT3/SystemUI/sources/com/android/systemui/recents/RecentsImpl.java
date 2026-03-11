package com.android.systemui.recents;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import android.util.MutableBoolean;
import android.view.AppTransitionAnimationSpec;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.policy.DockedDividerUtils;
import com.android.systemui.R;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.DockedTopTaskEvent;
import com.android.systemui.recents.events.activity.EnterRecentsWindowLastAnimationFrameEvent;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.activity.IterateRecentsEvent;
import com.android.systemui.recents.events.activity.LaunchNextTaskRequestEvent;
import com.android.systemui.recents.events.activity.RecentsActivityStartingEvent;
import com.android.systemui.recents.events.activity.ToggleRecentsEvent;
import com.android.systemui.recents.events.ui.DraggingInRecentsEndedEvent;
import com.android.systemui.recents.events.ui.DraggingInRecentsEvent;
import com.android.systemui.recents.misc.DozeTrigger;
import com.android.systemui.recents.misc.ForegroundThread;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.RecentsTaskLoadPlan;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskGrouping;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.views.TaskStackLayoutAlgorithm;
import com.android.systemui.recents.views.TaskStackView;
import com.android.systemui.recents.views.TaskStackViewScroller;
import com.android.systemui.recents.views.TaskViewHeader;
import com.android.systemui.recents.views.TaskViewTransform;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import java.util.ArrayList;

public class RecentsImpl implements ActivityOptions.OnAnimationFinishedListener {
    protected static RecentsTaskLoadPlan sInstanceLoadPlan;
    protected Context mContext;
    boolean mDraggingInRecents;
    protected TaskStackView mDummyStackView;
    TaskViewHeader mHeaderBar;
    protected long mLastToggleTime;
    boolean mLaunchedWhileDocking;
    int mNavBarHeight;
    int mNavBarWidth;
    int mStatusBarHeight;
    int mTaskBarHeight;
    TaskStackListenerImpl mTaskStackListener;
    protected Bitmap mThumbTransitionBitmapCache;
    protected boolean mTriggeredFromAltTab;
    Rect mTaskStackBounds = new Rect();
    TaskViewTransform mTmpTransform = new TaskViewTransform();
    final Object mHeaderBarLock = new Object();
    DozeTrigger mFastAltTabTrigger = new DozeTrigger(225, new Runnable() {
        @Override
        public void run() {
            RecentsImpl.this.showRecents(RecentsImpl.this.mTriggeredFromAltTab, false, true, false, false, -1);
        }
    });
    protected Handler mHandler = new Handler();

    class TaskStackListenerImpl extends SystemServicesProxy.TaskStackListener {
        TaskStackListenerImpl() {
        }

        @Override
        public void onTaskStackChanged() {
            RecentsConfiguration config = Recents.getConfiguration();
            if (config.svelteLevel != 0) {
                return;
            }
            RecentsTaskLoader loader = Recents.getTaskLoader();
            SystemServicesProxy ssp = Recents.getSystemServices();
            ActivityManager.RunningTaskInfo runningTaskInfo = ssp.getRunningTask();
            RecentsTaskLoadPlan plan = loader.createLoadPlan(RecentsImpl.this.mContext);
            loader.preloadTasks(plan, -1, false);
            RecentsTaskLoadPlan.Options launchOpts = new RecentsTaskLoadPlan.Options();
            if (runningTaskInfo != null) {
                launchOpts.runningTaskId = runningTaskInfo.id;
            }
            launchOpts.numVisibleTasks = 2;
            launchOpts.numVisibleTaskThumbnails = 2;
            launchOpts.onlyLoadForCache = true;
            launchOpts.onlyLoadPausedActivities = true;
            loader.loadTasks(RecentsImpl.this.mContext, plan, launchOpts);
        }
    }

    public RecentsImpl(Context context) {
        this.mContext = context;
        ForegroundThread.get();
        this.mTaskStackListener = new TaskStackListenerImpl();
        SystemServicesProxy ssp = Recents.getSystemServices();
        ssp.registerTaskStackListener(this.mTaskStackListener);
        LayoutInflater inflater = LayoutInflater.from(this.mContext);
        this.mDummyStackView = new TaskStackView(this.mContext);
        this.mHeaderBar = (TaskViewHeader) inflater.inflate(R.layout.recents_task_view_header, (ViewGroup) null, false);
        reloadResources();
    }

    public void onBootCompleted() {
        RecentsTaskLoader loader = Recents.getTaskLoader();
        RecentsTaskLoadPlan plan = loader.createLoadPlan(this.mContext);
        loader.preloadTasks(plan, -1, false);
        RecentsTaskLoadPlan.Options launchOpts = new RecentsTaskLoadPlan.Options();
        launchOpts.numVisibleTasks = loader.getIconCacheSize();
        launchOpts.numVisibleTaskThumbnails = loader.getThumbnailCacheSize();
        launchOpts.onlyLoadForCache = true;
        loader.loadTasks(this.mContext, plan, launchOpts);
    }

    public void onConfigurationChanged() {
        reloadResources();
        this.mDummyStackView.reloadOnConfigurationChange();
        this.mHeaderBar.onConfigurationChanged();
    }

    public void onVisibilityChanged(Context context, boolean visible) {
        SystemUIApplication app = (SystemUIApplication) context;
        PhoneStatusBar statusBar = (PhoneStatusBar) app.getComponent(PhoneStatusBar.class);
        if (statusBar == null) {
            return;
        }
        statusBar.updateRecentsVisibility(visible);
    }

    public void onStartScreenPinning(Context context, int taskId) {
        SystemUIApplication app = (SystemUIApplication) context;
        PhoneStatusBar statusBar = (PhoneStatusBar) app.getComponent(PhoneStatusBar.class);
        if (statusBar == null) {
            return;
        }
        statusBar.showScreenPinningRequest(taskId, false);
    }

    public void showRecents(boolean triggeredFromAltTab, boolean draggingInRecents, boolean animate, boolean launchedWhileDockingTask, boolean fromHome, int growTarget) {
        this.mTriggeredFromAltTab = triggeredFromAltTab;
        this.mDraggingInRecents = draggingInRecents;
        this.mLaunchedWhileDocking = launchedWhileDockingTask;
        if (this.mFastAltTabTrigger.isAsleep()) {
            this.mFastAltTabTrigger.stopDozing();
        } else if (this.mFastAltTabTrigger.isDozing()) {
            if (!triggeredFromAltTab) {
                return;
            } else {
                this.mFastAltTabTrigger.stopDozing();
            }
        } else if (triggeredFromAltTab) {
            this.mFastAltTabTrigger.startDozing();
            return;
        }
        try {
            SystemServicesProxy ssp = Recents.getSystemServices();
            boolean z = !launchedWhileDockingTask ? draggingInRecents : true;
            MutableBoolean isHomeStackVisible = new MutableBoolean(z);
            if (!z && ssp.isRecentsActivityVisible(isHomeStackVisible)) {
                return;
            }
            ActivityManager.RunningTaskInfo runningTask = ssp.getRunningTask();
            if (isHomeStackVisible.value) {
                fromHome = true;
            }
            startRecentsActivity(runningTask, fromHome, animate, growTarget);
        } catch (ActivityNotFoundException e) {
            Log.e("RecentsImpl", "Failed to launch RecentsActivity", e);
        }
    }

    public void hideRecents(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        if (triggeredFromAltTab && this.mFastAltTabTrigger.isDozing()) {
            showNextTask();
            this.mFastAltTabTrigger.stopDozing();
        } else {
            EventBus.getDefault().post(new HideRecentsEvent(triggeredFromAltTab, triggeredFromHomeKey));
        }
    }

    public void toggleRecents(int growTarget) {
        if (this.mFastAltTabTrigger.isDozing()) {
            return;
        }
        this.mDraggingInRecents = false;
        this.mLaunchedWhileDocking = false;
        this.mTriggeredFromAltTab = false;
        try {
            SystemServicesProxy ssp = Recents.getSystemServices();
            MutableBoolean isHomeStackVisible = new MutableBoolean(true);
            long elapsedTime = SystemClock.elapsedRealtime() - this.mLastToggleTime;
            if (!ssp.isRecentsActivityVisible(isHomeStackVisible)) {
                if (elapsedTime < 350) {
                    return;
                }
                ActivityManager.RunningTaskInfo runningTask = ssp.getRunningTask();
                startRecentsActivity(runningTask, isHomeStackVisible.value, true, growTarget);
                ssp.sendCloseSystemWindows("recentapps");
                this.mLastToggleTime = SystemClock.elapsedRealtime();
                return;
            }
            RecentsDebugFlags debugFlags = Recents.getDebugFlags();
            RecentsConfiguration config = Recents.getConfiguration();
            RecentsActivityLaunchState launchState = config.getLaunchState();
            if (launchState.launchedWithAltTab) {
                if (elapsedTime < 350) {
                    return;
                }
                EventBus.getDefault().post(new ToggleRecentsEvent());
                this.mLastToggleTime = SystemClock.elapsedRealtime();
                return;
            }
            if (!debugFlags.isPagingEnabled() || (ViewConfiguration.getDoubleTapMinTime() < elapsedTime && elapsedTime < ViewConfiguration.getDoubleTapTimeout())) {
                EventBus.getDefault().post(new LaunchNextTaskRequestEvent());
            } else {
                EventBus.getDefault().post(new IterateRecentsEvent());
            }
        } catch (ActivityNotFoundException e) {
            Log.e("RecentsImpl", "Failed to launch RecentsActivity", e);
        }
    }

    public void preloadRecents() {
        SystemServicesProxy ssp = Recents.getSystemServices();
        MutableBoolean isHomeStackVisible = new MutableBoolean(true);
        if (ssp.isRecentsActivityVisible(isHomeStackVisible)) {
            return;
        }
        ActivityManager.RunningTaskInfo runningTask = ssp.getRunningTask();
        if (runningTask == null) {
            Log.e("RecentsImpl", "preloadRecents runningTask is null");
            return;
        }
        RecentsTaskLoader loader = Recents.getTaskLoader();
        sInstanceLoadPlan = loader.createLoadPlan(this.mContext);
        sInstanceLoadPlan.preloadRawTasks(!isHomeStackVisible.value);
        loader.preloadTasks(sInstanceLoadPlan, runningTask.id, isHomeStackVisible.value ? false : true);
        TaskStack stack = sInstanceLoadPlan.getTaskStack();
        if (stack.getTaskCount() <= 0) {
            return;
        }
        preloadIcon(runningTask.id);
        updateHeaderBarLayout(stack, null);
    }

    public void cancelPreloadingRecents() {
    }

    public void onDraggingInRecents(float distanceFromTop) {
        EventBus.getDefault().sendOntoMainThread(new DraggingInRecentsEvent(distanceFromTop));
    }

    public void onDraggingInRecentsEnded(float velocity) {
        EventBus.getDefault().sendOntoMainThread(new DraggingInRecentsEndedEvent(velocity));
    }

    public void showNextTask() {
        ActivityManager.RunningTaskInfo runningTask;
        SystemServicesProxy ssp = Recents.getSystemServices();
        RecentsTaskLoader loader = Recents.getTaskLoader();
        RecentsTaskLoadPlan plan = loader.createLoadPlan(this.mContext);
        loader.preloadTasks(plan, -1, false);
        TaskStack focusedStack = plan.getTaskStack();
        if (focusedStack == null || focusedStack.getTaskCount() == 0 || (runningTask = ssp.getRunningTask()) == null) {
            return;
        }
        boolean isRunningTaskInHomeStack = SystemServicesProxy.isHomeStack(runningTask.stackId);
        ArrayList<Task> tasks = focusedStack.getStackTasks();
        Task toTask = null;
        ActivityOptions launchOpts = null;
        int taskCount = tasks.size();
        int i = taskCount - 1;
        while (true) {
            if (i < 1) {
                break;
            }
            Task task = tasks.get(i);
            if (isRunningTaskInHomeStack) {
                Task toTask2 = tasks.get(i - 1);
                toTask = toTask2;
                launchOpts = ActivityOptions.makeCustomAnimation(this.mContext, R.anim.recents_launch_next_affiliated_task_target, R.anim.recents_fast_toggle_app_home_exit);
                break;
            } else if (task.key.id != runningTask.id) {
                i--;
            } else {
                Task toTask3 = tasks.get(i - 1);
                toTask = toTask3;
                launchOpts = ActivityOptions.makeCustomAnimation(this.mContext, R.anim.recents_launch_prev_affiliated_task_target, R.anim.recents_launch_prev_affiliated_task_source);
                break;
            }
        }
        if (toTask == null) {
            ssp.startInPlaceAnimationOnFrontMostApplication(ActivityOptions.makeCustomInPlaceAnimation(this.mContext, R.anim.recents_launch_prev_affiliated_task_bounce));
        } else {
            ssp.startActivityFromRecents(this.mContext, toTask.key, toTask.title, launchOpts);
        }
    }

    public void showRelativeAffiliatedTask(boolean showNextTask) {
        ActivityManager.RunningTaskInfo runningTask;
        Task.TaskKey toTaskKey;
        SystemServicesProxy ssp = Recents.getSystemServices();
        RecentsTaskLoader loader = Recents.getTaskLoader();
        RecentsTaskLoadPlan plan = loader.createLoadPlan(this.mContext);
        loader.preloadTasks(plan, -1, false);
        TaskStack focusedStack = plan.getTaskStack();
        if (focusedStack == null || focusedStack.getTaskCount() == 0 || (runningTask = ssp.getRunningTask()) == null || SystemServicesProxy.isHomeStack(runningTask.stackId)) {
            return;
        }
        ArrayList<Task> tasks = focusedStack.getStackTasks();
        Task toTask = null;
        ActivityOptions launchOpts = null;
        int taskCount = tasks.size();
        int numAffiliatedTasks = 0;
        int i = 0;
        while (true) {
            if (i >= taskCount) {
                break;
            }
            Task task = tasks.get(i);
            if (task.key.id != runningTask.id) {
                i++;
            } else {
                TaskGrouping group = task.group;
                if (showNextTask) {
                    toTaskKey = group.getNextTaskInGroup(task);
                    launchOpts = ActivityOptions.makeCustomAnimation(this.mContext, R.anim.recents_launch_next_affiliated_task_target, R.anim.recents_launch_next_affiliated_task_source);
                } else {
                    toTaskKey = group.getPrevTaskInGroup(task);
                    launchOpts = ActivityOptions.makeCustomAnimation(this.mContext, R.anim.recents_launch_prev_affiliated_task_target, R.anim.recents_launch_prev_affiliated_task_source);
                }
                if (toTaskKey != null) {
                    toTask = focusedStack.findTaskWithId(toTaskKey.id);
                }
                numAffiliatedTasks = group.getTaskCount();
            }
        }
        if (toTask == null) {
            if (numAffiliatedTasks > 1) {
                if (showNextTask) {
                    ssp.startInPlaceAnimationOnFrontMostApplication(ActivityOptions.makeCustomInPlaceAnimation(this.mContext, R.anim.recents_launch_next_affiliated_task_bounce));
                    return;
                } else {
                    ssp.startInPlaceAnimationOnFrontMostApplication(ActivityOptions.makeCustomInPlaceAnimation(this.mContext, R.anim.recents_launch_prev_affiliated_task_bounce));
                    return;
                }
            }
            return;
        }
        MetricsLogger.count(this.mContext, "overview_affiliated_task_launch", 1);
        ssp.startActivityFromRecents(this.mContext, toTask.key, toTask.title, launchOpts);
    }

    public void showNextAffiliatedTask() {
        MetricsLogger.count(this.mContext, "overview_affiliated_task_next", 1);
        showRelativeAffiliatedTask(true);
    }

    public void showPrevAffiliatedTask() {
        MetricsLogger.count(this.mContext, "overview_affiliated_task_prev", 1);
        showRelativeAffiliatedTask(false);
    }

    public void dockTopTask(int topTaskId, int dragMode, int stackCreateMode, Rect initialBounds) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (!ssp.moveTaskToDockedStack(topTaskId, stackCreateMode, initialBounds)) {
            return;
        }
        EventBus.getDefault().send(new DockedTopTaskEvent(dragMode, initialBounds));
        showRecents(false, dragMode == 0, false, true, false, -1);
    }

    public static RecentsTaskLoadPlan consumeInstanceLoadPlan() {
        RecentsTaskLoadPlan plan = sInstanceLoadPlan;
        sInstanceLoadPlan = null;
        return plan;
    }

    private void reloadResources() {
        Resources res = this.mContext.getResources();
        this.mStatusBarHeight = res.getDimensionPixelSize(android.R.dimen.accessibility_touch_slop);
        this.mNavBarHeight = res.getDimensionPixelSize(android.R.dimen.accessibility_window_magnifier_min_size);
        this.mNavBarWidth = res.getDimensionPixelSize(android.R.dimen.action_bar_button_max_width);
        this.mTaskBarHeight = TaskStackLayoutAlgorithm.getDimensionForDevice(this.mContext, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height_tablet_land, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height_tablet_land);
    }

    private void updateHeaderBarLayout(TaskStack stack, Rect windowRectOverride) {
        Rect windowRect;
        SystemServicesProxy ssp = Recents.getSystemServices();
        Rect displayRect = ssp.getDisplayRect();
        Rect systemInsets = new Rect();
        ssp.getStableInsets(systemInsets);
        if (windowRectOverride != null) {
            windowRect = new Rect(windowRectOverride);
        } else {
            windowRect = ssp.getWindowRect();
        }
        if (ssp.hasDockedTask()) {
            windowRect.bottom -= systemInsets.bottom;
            systemInsets.bottom = 0;
        }
        calculateWindowStableInsets(systemInsets, windowRect);
        windowRect.offsetTo(0, 0);
        TaskStackLayoutAlgorithm stackLayout = this.mDummyStackView.getStackAlgorithm();
        stackLayout.setSystemInsets(systemInsets);
        if (stack == null) {
            return;
        }
        stackLayout.getTaskStackBounds(displayRect, windowRect, systemInsets.top, systemInsets.right, this.mTaskStackBounds);
        stackLayout.reset();
        stackLayout.initialize(displayRect, windowRect, this.mTaskStackBounds, TaskStackLayoutAlgorithm.StackState.getStackStateForStack(stack));
        this.mDummyStackView.setTasks(stack, false);
        Rect taskViewBounds = stackLayout.getUntransformedTaskViewBounds();
        if (taskViewBounds.isEmpty()) {
            return;
        }
        int taskViewWidth = taskViewBounds.width();
        synchronized (this.mHeaderBarLock) {
            if (this.mHeaderBar.getMeasuredWidth() != taskViewWidth || this.mHeaderBar.getMeasuredHeight() != this.mTaskBarHeight) {
                this.mHeaderBar.measure(View.MeasureSpec.makeMeasureSpec(taskViewWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(this.mTaskBarHeight, 1073741824));
            }
            this.mHeaderBar.layout(0, 0, taskViewWidth, this.mTaskBarHeight);
        }
        if (this.mThumbTransitionBitmapCache != null && this.mThumbTransitionBitmapCache.getWidth() == taskViewWidth && this.mThumbTransitionBitmapCache.getHeight() == this.mTaskBarHeight) {
            return;
        }
        this.mThumbTransitionBitmapCache = Bitmap.createBitmap(taskViewWidth, this.mTaskBarHeight, Bitmap.Config.ARGB_8888);
    }

    private void calculateWindowStableInsets(Rect inOutInsets, Rect windowRect) {
        Rect displayRect = Recents.getSystemServices().getDisplayRect();
        Rect appRect = new Rect(displayRect);
        appRect.inset(inOutInsets);
        Rect windowRectWithInsets = new Rect(windowRect);
        windowRectWithInsets.intersect(appRect);
        inOutInsets.left = windowRectWithInsets.left - windowRect.left;
        inOutInsets.top = windowRectWithInsets.top - windowRect.top;
        inOutInsets.right = windowRect.right - windowRectWithInsets.right;
        inOutInsets.bottom = windowRect.bottom - windowRectWithInsets.bottom;
    }

    private void preloadIcon(int runningTaskId) {
        RecentsTaskLoadPlan.Options launchOpts = new RecentsTaskLoadPlan.Options();
        launchOpts.runningTaskId = runningTaskId;
        launchOpts.loadThumbnails = false;
        launchOpts.onlyLoadForCache = true;
        Recents.getTaskLoader().loadTasks(this.mContext, sInstanceLoadPlan, launchOpts);
    }

    protected ActivityOptions getUnknownTransitionActivityOptions() {
        return ActivityOptions.makeCustomAnimation(this.mContext, R.anim.recents_from_unknown_enter, R.anim.recents_from_unknown_exit, this.mHandler, null);
    }

    protected ActivityOptions getHomeTransitionActivityOptions() {
        return ActivityOptions.makeCustomAnimation(this.mContext, R.anim.recents_from_launcher_enter, R.anim.recents_from_launcher_exit, this.mHandler, null);
    }

    private ActivityOptions getThumbnailTransitionActivityOptions(ActivityManager.RunningTaskInfo runningTask, TaskStackView stackView, Rect windowOverrideRect) {
        if (runningTask != null && runningTask.stackId == 2) {
            ArrayList<AppTransitionAnimationSpec> specs = new ArrayList<>();
            ArrayList<Task> tasks = stackView.getStack().getStackTasks();
            TaskStackLayoutAlgorithm stackLayout = stackView.getStackAlgorithm();
            TaskStackViewScroller stackScroller = stackView.getScroller();
            stackView.updateLayoutAlgorithm(true);
            stackView.updateToInitialState();
            for (int i = tasks.size() - 1; i >= 0; i--) {
                Task task = tasks.get(i);
                if (task.isFreeformTask()) {
                    this.mTmpTransform = stackLayout.getStackTransformScreenCoordinates(task, stackScroller.getStackScroll(), this.mTmpTransform, null, windowOverrideRect);
                    Bitmap thumbnail = drawThumbnailTransitionBitmap(task, this.mTmpTransform, this.mThumbTransitionBitmapCache);
                    Rect toTaskRect = new Rect();
                    this.mTmpTransform.rect.round(toTaskRect);
                    specs.add(new AppTransitionAnimationSpec(task.key.id, thumbnail, toTaskRect));
                }
            }
            AppTransitionAnimationSpec[] specsArray = new AppTransitionAnimationSpec[specs.size()];
            specs.toArray(specsArray);
            return ActivityOptions.makeThumbnailAspectScaleDownAnimation(this.mDummyStackView, specsArray, this.mHandler, null, this);
        }
        Task toTask = new Task();
        TaskViewTransform toTransform = getThumbnailTransitionTransform(stackView, toTask, windowOverrideRect);
        Bitmap thumbnail2 = drawThumbnailTransitionBitmap(toTask, toTransform, this.mThumbTransitionBitmapCache);
        if (thumbnail2 != null) {
            RectF toTaskRect2 = toTransform.rect;
            return ActivityOptions.makeThumbnailAspectScaleDownAnimation(this.mDummyStackView, thumbnail2, (int) toTaskRect2.left, (int) toTaskRect2.top, (int) toTaskRect2.width(), (int) toTaskRect2.height(), this.mHandler, null);
        }
        return getUnknownTransitionActivityOptions();
    }

    private TaskViewTransform getThumbnailTransitionTransform(TaskStackView stackView, Task runningTaskOut, Rect windowOverrideRect) {
        TaskStack stack = stackView.getStack();
        Task launchTask = stack.getLaunchTarget();
        if (launchTask != null) {
            runningTaskOut.copyFrom(launchTask);
        } else {
            launchTask = stack.getStackFrontMostTask(true);
            runningTaskOut.copyFrom(launchTask);
        }
        stackView.updateLayoutAlgorithm(true);
        stackView.updateToInitialState();
        stackView.getStackAlgorithm().getStackTransformScreenCoordinates(launchTask, stackView.getScroller().getStackScroll(), this.mTmpTransform, null, windowOverrideRect);
        return this.mTmpTransform;
    }

    private Bitmap drawThumbnailTransitionBitmap(Task toTask, TaskViewTransform toTransform, Bitmap thumbnail) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (toTransform == null || toTask.key == null || thumbnail == null) {
            return null;
        }
        synchronized (this.mHeaderBarLock) {
            boolean zIsInSafeMode = !toTask.isSystemApp ? ssp.isInSafeMode() : false;
            this.mHeaderBar.onTaskViewSizeChanged((int) toTransform.rect.width(), (int) toTransform.rect.height());
            thumbnail.eraseColor(0);
            Canvas c = new Canvas(thumbnail);
            Drawable icon = this.mHeaderBar.getIconView().getDrawable();
            if (icon != null) {
                icon.setCallback(null);
            }
            this.mHeaderBar.bindToTask(toTask, false, zIsInSafeMode);
            this.mHeaderBar.onTaskDataLoaded();
            this.mHeaderBar.setDimAlpha(toTransform.dimAlpha);
            this.mHeaderBar.draw(c);
            c.setBitmap(null);
        }
        return thumbnail.createAshmemBitmap();
    }

    protected void startRecentsActivity(ActivityManager.RunningTaskInfo runningTask, boolean isHomeStackVisible, boolean animate, int growTarget) {
        int runningTaskId;
        ActivityOptions opts;
        RecentsTaskLoader loader = Recents.getTaskLoader();
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        if (!this.mLaunchedWhileDocking && runningTask != null) {
            runningTaskId = runningTask.id;
        } else {
            runningTaskId = -1;
        }
        if (this.mLaunchedWhileDocking || this.mTriggeredFromAltTab || sInstanceLoadPlan == null) {
            sInstanceLoadPlan = loader.createLoadPlan(this.mContext);
        }
        if (this.mLaunchedWhileDocking || this.mTriggeredFromAltTab || !sInstanceLoadPlan.hasTasks()) {
            loader.preloadTasks(sInstanceLoadPlan, runningTaskId, !isHomeStackVisible);
        }
        TaskStack stack = sInstanceLoadPlan.getTaskStack();
        boolean hasRecentTasks = stack.getTaskCount() > 0;
        boolean useThumbnailTransition = (runningTask == null || isHomeStackVisible) ? false : hasRecentTasks;
        launchState.launchedFromHome = (useThumbnailTransition || this.mLaunchedWhileDocking) ? false : true;
        launchState.launchedFromApp = !useThumbnailTransition ? this.mLaunchedWhileDocking : true;
        launchState.launchedViaDockGesture = this.mLaunchedWhileDocking;
        launchState.launchedViaDragGesture = this.mDraggingInRecents;
        launchState.launchedToTaskId = runningTaskId;
        launchState.launchedWithAltTab = this.mTriggeredFromAltTab;
        preloadIcon(runningTaskId);
        Rect windowOverrideRect = getWindowRectOverride(growTarget);
        updateHeaderBarLayout(stack, windowOverrideRect);
        TaskStackLayoutAlgorithm.VisibilityReport stackVr = this.mDummyStackView.computeStackVisibilityReport();
        launchState.launchedNumVisibleTasks = stackVr.numVisibleTasks;
        launchState.launchedNumVisibleThumbnails = stackVr.numVisibleThumbnails;
        if (!animate) {
            startRecentsActivity(ActivityOptions.makeCustomAnimation(this.mContext, -1, -1));
            return;
        }
        if (useThumbnailTransition) {
            opts = getThumbnailTransitionActivityOptions(runningTask, this.mDummyStackView, windowOverrideRect);
        } else if (hasRecentTasks) {
            opts = getHomeTransitionActivityOptions();
        } else {
            opts = getUnknownTransitionActivityOptions();
        }
        startRecentsActivity(opts);
        this.mLastToggleTime = SystemClock.elapsedRealtime();
    }

    private Rect getWindowRectOverride(int growTarget) {
        if (growTarget == -1) {
            return null;
        }
        Rect result = new Rect();
        Rect displayRect = Recents.getSystemServices().getDisplayRect();
        DockedDividerUtils.calculateBoundsForPosition(growTarget, 4, result, displayRect.width(), displayRect.height(), Recents.getSystemServices().getDockedDividerSize(this.mContext));
        return result;
    }

    private void startRecentsActivity(ActivityOptions opts) {
        Intent intent = new Intent();
        intent.setClassName("com.android.systemui", "com.android.systemui.recents.RecentsActivity");
        intent.setFlags(276840448);
        if (opts != null) {
            this.mContext.startActivityAsUser(intent, opts.toBundle(), UserHandle.CURRENT);
        } else {
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        }
        EventBus.getDefault().send(new RecentsActivityStartingEvent());
    }

    public void onAnimationFinished() {
        EventBus.getDefault().post(new EnterRecentsWindowLastAnimationFrameEvent());
    }
}
