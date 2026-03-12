package com.android.systemui.recents;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.ITaskStackListener;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.RecentsComponent;
import com.android.systemui.recents.Constants;
import com.android.systemui.recents.misc.Console;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.RecentsTaskLoadPlan;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskGrouping;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.views.TaskStackView;
import com.android.systemui.recents.views.TaskStackViewLayoutAlgorithm;
import com.android.systemui.recents.views.TaskViewHeader;
import com.android.systemui.recents.views.TaskViewTransform;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class AlternateRecentsComponent implements ActivityOptions.OnAnimationStartedListener {
    static RecentsTaskLoadPlan sInstanceLoadPlan;
    static RecentsComponent.Callbacks sRecentsComponentCallbacks;
    boolean mBootCompleted;
    RecentsConfiguration mConfig;
    Context mContext;
    TaskStackView mDummyStackView;
    Handler mHandler;
    TaskViewHeader mHeaderBar;
    LayoutInflater mInflater;
    long mLastToggleTime;
    int mNavBarHeight;
    int mNavBarWidth;
    RecentsOwnerEventProxyReceiver mProxyBroadcastReceiver;
    boolean mStartAnimationTriggered;
    int mStatusBarHeight;
    SystemServicesProxy mSystemServicesProxy;
    Rect mTaskStackBounds;
    TaskStackListenerImpl mTaskStackListener;
    boolean mTriggeredFromAltTab;
    boolean mCanReuseTaskStackViews = true;
    Rect mWindowRect = new Rect();
    Rect mSystemInsets = new Rect();
    TaskViewTransform mTmpTransform = new TaskViewTransform();

    class TaskStackListenerImpl extends ITaskStackListener.Stub implements Runnable {
        Handler mHandler;

        public TaskStackListenerImpl(Handler handler) {
            this.mHandler = handler;
        }

        public void onTaskStackChanged() {
            this.mHandler.removeCallbacks(this);
            this.mHandler.post(this);
        }

        @Override
        public void run() {
            RecentsConfiguration config = RecentsConfiguration.getInstance();
            if (config.svelteLevel == 0) {
                RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
                SystemServicesProxy ssp = loader.getSystemServicesProxy();
                ActivityManager.RunningTaskInfo runningTaskInfo = ssp.getTopMostTask();
                RecentsTaskLoadPlan plan = loader.createLoadPlan(AlternateRecentsComponent.this.mContext);
                loader.preloadTasks(plan, true);
                RecentsTaskLoadPlan.Options launchOpts = new RecentsTaskLoadPlan.Options();
                if (runningTaskInfo != null) {
                    launchOpts.runningTaskId = runningTaskInfo.id;
                }
                launchOpts.numVisibleTasks = 2;
                launchOpts.numVisibleTaskThumbnails = 2;
                launchOpts.onlyLoadForCache = true;
                launchOpts.onlyLoadPausedActivities = true;
                loader.loadTasks(AlternateRecentsComponent.this.mContext, plan, launchOpts);
            }
        }
    }

    class RecentsOwnerEventProxyReceiver extends BroadcastReceiver {
        RecentsOwnerEventProxyReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case "action_notify_recents_visibility_change":
                    AlternateRecentsComponent.visibilityChanged(intent.getBooleanExtra("recentsVisibility", false));
                    break;
            }
        }
    }

    public AlternateRecentsComponent(Context context) {
        this.mTaskStackBounds = new Rect();
        RecentsTaskLoader.initialize(context);
        this.mInflater = LayoutInflater.from(context);
        this.mContext = context;
        this.mSystemServicesProxy = new SystemServicesProxy(context);
        this.mHandler = new Handler();
        this.mTaskStackBounds = new Rect();
        this.mTaskStackListener = new TaskStackListenerImpl(this.mHandler);
        this.mSystemServicesProxy.registerTaskStackListener(this.mTaskStackListener);
        if (this.mSystemServicesProxy.isForegroundUserOwner()) {
            this.mProxyBroadcastReceiver = new RecentsOwnerEventProxyReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("action_notify_recents_visibility_change");
            this.mContext.registerReceiverAsUser(this.mProxyBroadcastReceiver, UserHandle.CURRENT, filter, null, this.mHandler);
        }
    }

    static Intent createLocalBroadcastIntent(Context context, String action) {
        Intent intent = new Intent(action);
        intent.setPackage(context.getPackageName());
        intent.addFlags(335544320);
        return intent;
    }

    @ProxyFromPrimaryToCurrentUser
    public void onStart() {
        TaskStackViewLayoutAlgorithm.initializeCurve();
        reloadHeaderBarLayout(true);
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        RecentsTaskLoadPlan plan = loader.createLoadPlan(this.mContext);
        loader.preloadTasks(plan, true);
        RecentsTaskLoadPlan.Options launchOpts = new RecentsTaskLoadPlan.Options();
        launchOpts.numVisibleTasks = loader.getApplicationIconCacheSize();
        launchOpts.numVisibleTaskThumbnails = loader.getThumbnailCacheSize();
        launchOpts.onlyLoadForCache = true;
        loader.loadTasks(this.mContext, plan, launchOpts);
    }

    public void onBootCompleted() {
        this.mBootCompleted = true;
    }

    @ProxyFromPrimaryToCurrentUser
    public void onShowRecents(boolean triggeredFromAltTab) {
        if (isDeviceProvisioned()) {
            if (this.mSystemServicesProxy.isForegroundUserOwner()) {
                showRecents(triggeredFromAltTab);
                return;
            }
            Intent intent = createLocalBroadcastIntent(this.mContext, "com.android.systemui.recents.action.SHOW_RECENTS_FOR_USER");
            intent.putExtra("triggeredFromAltTab", triggeredFromAltTab);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        }
    }

    void showRecents(boolean triggeredFromAltTab) {
        this.mTriggeredFromAltTab = triggeredFromAltTab;
        try {
            startRecentsActivity();
        } catch (ActivityNotFoundException e) {
            Console.logRawError("Failed to launch RecentAppsIntent", e);
        }
    }

    @ProxyFromPrimaryToCurrentUser
    public void onHideRecents(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        if (isDeviceProvisioned()) {
            if (this.mSystemServicesProxy.isForegroundUserOwner()) {
                hideRecents(triggeredFromAltTab, triggeredFromHomeKey);
                return;
            }
            Intent intent = createLocalBroadcastIntent(this.mContext, "com.android.systemui.recents.action.HIDE_RECENTS_FOR_USER");
            intent.putExtra("triggeredFromAltTab", triggeredFromAltTab);
            intent.putExtra("triggeredFromHomeKey", triggeredFromHomeKey);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        }
    }

    void hideRecents(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        ActivityManager.RunningTaskInfo topTask;
        if (this.mBootCompleted && (topTask = this.mSystemServicesProxy.getTopMostTask()) != null && this.mSystemServicesProxy.isRecentsTopMost(topTask, null)) {
            Intent intent = createLocalBroadcastIntent(this.mContext, "action_hide_recents_activity");
            intent.putExtra("triggeredFromAltTab", triggeredFromAltTab);
            intent.putExtra("triggeredFromHomeKey", triggeredFromHomeKey);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        }
    }

    @ProxyFromPrimaryToCurrentUser
    public void onToggleRecents() {
        if (isDeviceProvisioned()) {
            if (this.mSystemServicesProxy.isForegroundUserOwner()) {
                toggleRecents();
            } else {
                Intent intent = createLocalBroadcastIntent(this.mContext, "com.android.systemui.recents.action.TOGGLE_RECENTS_FOR_USER");
                this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
            }
        }
    }

    void toggleRecents() {
        this.mTriggeredFromAltTab = false;
        try {
            toggleRecentsActivity();
        } catch (ActivityNotFoundException e) {
            Console.logRawError("Failed to launch RecentAppsIntent", e);
        }
    }

    @ProxyFromPrimaryToCurrentUser
    public void onPreloadRecents() {
        if (isDeviceProvisioned()) {
            if (this.mSystemServicesProxy.isForegroundUserOwner()) {
                preloadRecents();
            } else {
                Intent intent = createLocalBroadcastIntent(this.mContext, "com.android.systemui.recents.action.PRELOAD_RECENTS_FOR_USER");
                this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
            }
        }
    }

    void preloadRecents() {
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        sInstanceLoadPlan = loader.createLoadPlan(this.mContext);
        sInstanceLoadPlan.preloadRawTasks(true);
    }

    public void onCancelPreloadingRecents() {
    }

    void showRelativeAffiliatedTask(boolean showNextTask) {
        ActivityManager.RunningTaskInfo runningTask;
        Task.TaskKey toTaskKey;
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        RecentsTaskLoadPlan plan = loader.createLoadPlan(this.mContext);
        loader.preloadTasks(plan, true);
        TaskStack stack = plan.getTaskStack();
        if (stack.getTaskCount() != 0 && (runningTask = this.mSystemServicesProxy.getTopMostTask()) != null && !this.mSystemServicesProxy.isInHomeStack(runningTask.id)) {
            ArrayList<Task> tasks = stack.getTasks();
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
                        toTask = stack.findTaskWithId(toTaskKey.id);
                    }
                    numAffiliatedTasks = group.getTaskCount();
                }
            }
            if (toTask == null) {
                if (numAffiliatedTasks > 1) {
                    if (showNextTask) {
                        this.mSystemServicesProxy.startInPlaceAnimationOnFrontMostApplication(ActivityOptions.makeCustomInPlaceAnimation(this.mContext, R.anim.recents_launch_next_affiliated_task_bounce));
                        return;
                    } else {
                        this.mSystemServicesProxy.startInPlaceAnimationOnFrontMostApplication(ActivityOptions.makeCustomInPlaceAnimation(this.mContext, R.anim.recents_launch_prev_affiliated_task_bounce));
                        return;
                    }
                }
                return;
            }
            if (toTask.isActive) {
                this.mSystemServicesProxy.moveTaskToFront(toTask.key.id, launchOpts);
            } else {
                this.mSystemServicesProxy.startActivityFromRecents(this.mContext, toTask.key.id, toTask.activityLabel, launchOpts);
            }
        }
    }

    public void onShowNextAffiliatedTask() {
        if (isDeviceProvisioned()) {
            showRelativeAffiliatedTask(true);
        }
    }

    public void onShowPrevAffiliatedTask() {
        if (isDeviceProvisioned()) {
            showRelativeAffiliatedTask(false);
        }
    }

    @ProxyFromPrimaryToCurrentUser
    public void onConfigurationChanged(Configuration newConfig) {
        if (this.mSystemServicesProxy.isForegroundUserOwner()) {
            configurationChanged();
        } else {
            Intent intent = createLocalBroadcastIntent(this.mContext, "com.android.systemui.recents.action.CONFIG_CHANGED_FOR_USER");
            this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        }
    }

    void configurationChanged() {
        this.mCanReuseTaskStackViews = false;
        reloadHeaderBarLayout(false);
    }

    void reloadHeaderBarLayout(boolean reloadWidget) {
        Resources res = this.mContext.getResources();
        this.mWindowRect = this.mSystemServicesProxy.getWindowRect();
        this.mStatusBarHeight = res.getDimensionPixelSize(android.R.dimen.accessibility_focus_highlight_stroke_width);
        this.mNavBarHeight = res.getDimensionPixelSize(android.R.dimen.accessibility_fullscreen_magnification_gesture_edge_slop);
        this.mNavBarWidth = res.getDimensionPixelSize(android.R.dimen.accessibility_magnification_indicator_width);
        this.mConfig = RecentsConfiguration.reinitialize(this.mContext, this.mSystemServicesProxy);
        this.mConfig.updateOnConfigurationChange();
        if (reloadWidget) {
            reloadSearchBarAppWidget(this.mContext, this.mSystemServicesProxy);
        }
        this.mConfig.getTaskStackBounds(this.mWindowRect.width(), this.mWindowRect.height(), this.mStatusBarHeight, this.mConfig.hasTransposedNavBar ? this.mNavBarWidth : 0, this.mTaskStackBounds);
        if (this.mConfig.isLandscape && this.mConfig.hasTransposedNavBar) {
            this.mSystemInsets.set(0, this.mStatusBarHeight, this.mNavBarWidth, 0);
        } else {
            this.mSystemInsets.set(0, this.mStatusBarHeight, 0, this.mNavBarHeight);
        }
        TaskStack stack = new TaskStack();
        this.mDummyStackView = new TaskStackView(this.mContext, stack);
        TaskStackViewLayoutAlgorithm algo = this.mDummyStackView.getStackAlgorithm();
        Rect taskStackBounds = new Rect(this.mTaskStackBounds);
        taskStackBounds.bottom -= this.mSystemInsets.bottom;
        algo.computeRects(this.mWindowRect.width(), this.mWindowRect.height(), taskStackBounds);
        Rect taskViewSize = algo.getUntransformedTaskViewSize();
        int taskBarHeight = res.getDimensionPixelSize(R.dimen.recents_task_bar_height);
        this.mHeaderBar = (TaskViewHeader) this.mInflater.inflate(R.layout.recents_task_view_header, (ViewGroup) null, false);
        this.mHeaderBar.measure(View.MeasureSpec.makeMeasureSpec(taskViewSize.width(), 1073741824), View.MeasureSpec.makeMeasureSpec(taskBarHeight, 1073741824));
        this.mHeaderBar.layout(0, 0, taskViewSize.width(), taskBarHeight);
    }

    void reloadSearchBarAppWidget(Context context, SystemServicesProxy ssp) {
        if (this.mConfig.searchBarAppWidgetId < 0) {
            AppWidgetHost host = new RecentsAppWidgetHost(context, Constants.Values.App.AppWidgetHostId);
            Pair<Integer, AppWidgetProviderInfo> widgetInfo = ssp.bindSearchAppWidget(host);
            if (widgetInfo != null) {
                this.mConfig.updateSearchBarAppWidgetId(context, ((Integer) widgetInfo.first).intValue());
            }
        }
    }

    void toggleRecentsActivity() {
        if (SystemClock.elapsedRealtime() - this.mLastToggleTime >= 350) {
            ActivityManager.RunningTaskInfo topTask = this.mSystemServicesProxy.getTopMostTask();
            AtomicBoolean isTopTaskHome = new AtomicBoolean(true);
            if (topTask != null && this.mSystemServicesProxy.isRecentsTopMost(topTask, isTopTaskHome)) {
                Intent intent = createLocalBroadcastIntent(this.mContext, "action_toggle_recents_activity");
                this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
                this.mLastToggleTime = SystemClock.elapsedRealtime();
                return;
            }
            startRecentsActivity(topTask, isTopTaskHome.get());
        }
    }

    void startRecentsActivity() {
        ActivityManager.RunningTaskInfo topTask = this.mSystemServicesProxy.getTopMostTask();
        AtomicBoolean isTopTaskHome = new AtomicBoolean(true);
        if (topTask == null || !this.mSystemServicesProxy.isRecentsTopMost(topTask, isTopTaskHome)) {
            startRecentsActivity(topTask, isTopTaskHome.get());
        }
    }

    ActivityOptions getUnknownTransitionActivityOptions() {
        this.mStartAnimationTriggered = false;
        return ActivityOptions.makeCustomAnimation(this.mContext, R.anim.recents_from_unknown_enter, R.anim.recents_from_unknown_exit, this.mHandler, this);
    }

    ActivityOptions getHomeTransitionActivityOptions(boolean fromSearchHome) {
        this.mStartAnimationTriggered = false;
        return fromSearchHome ? ActivityOptions.makeCustomAnimation(this.mContext, R.anim.recents_from_search_launcher_enter, R.anim.recents_from_search_launcher_exit, this.mHandler, this) : ActivityOptions.makeCustomAnimation(this.mContext, R.anim.recents_from_launcher_enter, R.anim.recents_from_launcher_exit, this.mHandler, this);
    }

    ActivityOptions getThumbnailTransitionActivityOptions(ActivityManager.RunningTaskInfo topTask, TaskStack stack, TaskStackView stackView) {
        Task toTask = new Task();
        TaskViewTransform toTransform = getThumbnailTransitionTransform(stack, stackView, topTask.id, toTask);
        if (toTransform == null || toTask.key == null) {
            return getUnknownTransitionActivityOptions();
        }
        Rect toTaskRect = toTransform.rect;
        int toHeaderWidth = (int) (this.mHeaderBar.getMeasuredWidth() * toTransform.scale);
        int toHeaderHeight = (int) (this.mHeaderBar.getMeasuredHeight() * toTransform.scale);
        Bitmap thumbnail = Bitmap.createBitmap(toHeaderWidth, toHeaderHeight, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(thumbnail);
        c.scale(toTransform.scale, toTransform.scale);
        this.mHeaderBar.rebindToTask(toTask);
        this.mHeaderBar.draw(c);
        c.setBitmap(null);
        this.mStartAnimationTriggered = false;
        return ActivityOptions.makeThumbnailAspectScaleDownAnimation(this.mDummyStackView, thumbnail, toTaskRect.left, toTaskRect.top, toTaskRect.width(), toTaskRect.height(), this.mHandler, this);
    }

    TaskViewTransform getThumbnailTransitionTransform(TaskStack stack, TaskStackView stackView, int runningTaskId, Task runningTaskOut) {
        Task task = null;
        ArrayList<Task> tasks = stack.getTasks();
        if (runningTaskId != -1) {
            int taskCount = tasks.size();
            int i = taskCount - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                Task t = tasks.get(i);
                if (t.key.id != runningTaskId) {
                    i--;
                } else {
                    task = t;
                    runningTaskOut.copyFrom(t);
                    break;
                }
            }
        }
        if (task == null) {
            task = tasks.get(tasks.size() - 1);
        }
        stackView.getScroller().setStackScrollToInitialState();
        this.mTmpTransform = stackView.getStackAlgorithm().getStackTransform(task, stackView.getScroller().getStackScroll(), this.mTmpTransform, (TaskViewTransform) null);
        return this.mTmpTransform;
    }

    void startRecentsActivity(ActivityManager.RunningTaskInfo topTask, boolean isTopTaskHome) {
        AppWidgetProviderInfo searchWidget;
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        RecentsConfiguration.reinitialize(this.mContext, this.mSystemServicesProxy);
        if (sInstanceLoadPlan == null) {
            sInstanceLoadPlan = loader.createLoadPlan(this.mContext);
        }
        loader.preloadTasks(sInstanceLoadPlan, isTopTaskHome);
        TaskStack stack = sInstanceLoadPlan.getTaskStack();
        this.mDummyStackView.updateMinMaxScrollForStack(stack, this.mTriggeredFromAltTab, isTopTaskHome);
        TaskStackViewLayoutAlgorithm.VisibilityReport stackVr = this.mDummyStackView.computeStackVisibilityReport();
        boolean hasRecentTasks = stack.getTaskCount() > 0;
        boolean useThumbnailTransition = (topTask == null || isTopTaskHome || !hasRecentTasks) ? false : true;
        if (useThumbnailTransition) {
            RecentsTaskLoadPlan.Options launchOpts = new RecentsTaskLoadPlan.Options();
            launchOpts.runningTaskId = topTask.id;
            launchOpts.loadThumbnails = false;
            launchOpts.onlyLoadForCache = true;
            loader.loadTasks(this.mContext, sInstanceLoadPlan, launchOpts);
            ActivityOptions opts = getThumbnailTransitionActivityOptions(topTask, stack, this.mDummyStackView);
            if (opts != null) {
                startAlternateRecentsActivity(topTask, opts, false, false, true, stackVr);
            } else {
                useThumbnailTransition = false;
            }
        }
        if (!useThumbnailTransition) {
            if (hasRecentTasks) {
                String homeActivityPackage = this.mSystemServicesProxy.getHomeActivityPackageName();
                String searchWidgetPackage = null;
                if (this.mConfig.hasSearchBarAppWidget()) {
                    searchWidget = this.mSystemServicesProxy.getAppWidgetInfo(this.mConfig.searchBarAppWidgetId);
                } else {
                    searchWidget = this.mSystemServicesProxy.resolveSearchAppWidget();
                }
                if (searchWidget != null && searchWidget.provider != null) {
                    searchWidgetPackage = searchWidget.provider.getPackageName();
                }
                boolean fromSearchHome = false;
                if (homeActivityPackage != null && searchWidgetPackage != null && homeActivityPackage.equals(searchWidgetPackage)) {
                    fromSearchHome = true;
                }
                ActivityOptions opts2 = getHomeTransitionActivityOptions(fromSearchHome);
                startAlternateRecentsActivity(topTask, opts2, true, fromSearchHome, false, stackVr);
            } else {
                ActivityOptions opts3 = getUnknownTransitionActivityOptions();
                startAlternateRecentsActivity(topTask, opts3, true, false, false, stackVr);
            }
        }
        this.mLastToggleTime = SystemClock.elapsedRealtime();
    }

    void startAlternateRecentsActivity(ActivityManager.RunningTaskInfo topTask, ActivityOptions opts, boolean fromHome, boolean fromSearchHome, boolean fromThumbnail, TaskStackViewLayoutAlgorithm.VisibilityReport vr) {
        this.mConfig.launchedFromHome = fromSearchHome || fromHome;
        this.mConfig.launchedFromSearchHome = fromSearchHome;
        this.mConfig.launchedFromAppWithThumbnail = fromThumbnail;
        this.mConfig.launchedToTaskId = topTask != null ? topTask.id : -1;
        this.mConfig.launchedWithAltTab = this.mTriggeredFromAltTab;
        this.mConfig.launchedReuseTaskStackViews = this.mCanReuseTaskStackViews;
        this.mConfig.launchedNumVisibleTasks = vr.numVisibleTasks;
        this.mConfig.launchedNumVisibleThumbnails = vr.numVisibleThumbnails;
        this.mConfig.launchedHasConfigurationChanged = false;
        Intent intent = new Intent("com.android.systemui.recents.SHOW_RECENTS");
        intent.setClassName("com.android.systemui", "com.android.systemui.recents.RecentsActivity");
        intent.setFlags(276840448);
        if (opts != null) {
            this.mContext.startActivityAsUser(intent, opts.toBundle(), UserHandle.CURRENT);
        } else {
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        }
        this.mCanReuseTaskStackViews = true;
    }

    public void setRecentsComponentCallback(RecentsComponent.Callbacks cb) {
        sRecentsComponentCallbacks = cb;
    }

    @ProxyFromAnyToPrimaryUser
    public static void notifyVisibilityChanged(Context context, SystemServicesProxy ssp, boolean visible) {
        if (ssp.isForegroundUserOwner()) {
            visibilityChanged(visible);
            return;
        }
        Intent intent = createLocalBroadcastIntent(context, "action_notify_recents_visibility_change");
        intent.putExtra("recentsVisibility", visible);
        context.sendBroadcastAsUser(intent, UserHandle.OWNER);
    }

    static void visibilityChanged(boolean visible) {
        if (sRecentsComponentCallbacks != null) {
            sRecentsComponentCallbacks.onVisibilityChanged(visible);
        }
    }

    public static RecentsTaskLoadPlan consumeInstanceLoadPlan() {
        RecentsTaskLoadPlan plan = sInstanceLoadPlan;
        sInstanceLoadPlan = null;
        return plan;
    }

    private boolean isDeviceProvisioned() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
    }

    public void onAnimationStarted() {
        if (!this.mStartAnimationTriggered) {
            BroadcastReceiver fallbackReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (getResultCode() == -1) {
                        AlternateRecentsComponent.this.mStartAnimationTriggered = true;
                    } else {
                        AlternateRecentsComponent.this.mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                AlternateRecentsComponent.this.onAnimationStarted();
                            }
                        }, 25L);
                    }
                }
            };
            Intent intent = createLocalBroadcastIntent(this.mContext, "action_start_enter_animation");
            this.mContext.sendOrderedBroadcastAsUser(intent, UserHandle.CURRENT, null, fallbackReceiver, null, 0, null, null);
        }
    }
}
