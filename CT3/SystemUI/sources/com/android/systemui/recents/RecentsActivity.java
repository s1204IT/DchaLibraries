package com.android.systemui.recents;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Interpolators;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.CancelEnterRecentsWindowAnimationEvent;
import com.android.systemui.recents.events.activity.ConfigurationChangedEvent;
import com.android.systemui.recents.events.activity.DebugFlagsChangedEvent;
import com.android.systemui.recents.events.activity.DismissRecentsToHomeAnimationStarted;
import com.android.systemui.recents.events.activity.DockedFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.DockedTopTaskEvent;
import com.android.systemui.recents.events.activity.EnterRecentsWindowAnimationCompletedEvent;
import com.android.systemui.recents.events.activity.EnterRecentsWindowLastAnimationFrameEvent;
import com.android.systemui.recents.events.activity.ExitRecentsWindowFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.activity.IterateRecentsEvent;
import com.android.systemui.recents.events.activity.LaunchTaskFailedEvent;
import com.android.systemui.recents.events.activity.LaunchTaskSucceededEvent;
import com.android.systemui.recents.events.activity.MultiWindowStateChangedEvent;
import com.android.systemui.recents.events.activity.ToggleRecentsEvent;
import com.android.systemui.recents.events.component.RecentsVisibilityChangedEvent;
import com.android.systemui.recents.events.component.ScreenPinningRequestEvent;
import com.android.systemui.recents.events.ui.AllTaskViewsDismissedEvent;
import com.android.systemui.recents.events.ui.DeleteTaskDataEvent;
import com.android.systemui.recents.events.ui.HideIncompatibleAppOverlayEvent;
import com.android.systemui.recents.events.ui.RecentsDrawnEvent;
import com.android.systemui.recents.events.ui.ShowApplicationInfoEvent;
import com.android.systemui.recents.events.ui.ShowIncompatibleAppOverlayEvent;
import com.android.systemui.recents.events.ui.StackViewScrolledEvent;
import com.android.systemui.recents.events.ui.UpdateFreeformTaskViewVisibilityEvent;
import com.android.systemui.recents.events.ui.UserInteractionEvent;
import com.android.systemui.recents.events.ui.focus.DismissFocusedTaskViewEvent;
import com.android.systemui.recents.events.ui.focus.FocusNextTaskViewEvent;
import com.android.systemui.recents.events.ui.focus.FocusPreviousTaskViewEvent;
import com.android.systemui.recents.misc.DozeTrigger;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.RecentsPackageMonitor;
import com.android.systemui.recents.model.RecentsTaskLoadPlan;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.views.RecentsView;
import com.android.systemui.recents.views.SystemBarScrimViews;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class RecentsActivity extends Activity implements ViewTreeObserver.OnPreDrawListener {
    private boolean mFinishedOnStartup;
    private int mFocusTimerDuration;
    private Intent mHomeIntent;
    private boolean mIgnoreAltTabRelease;
    private View mIncompatibleAppOverlay;
    private boolean mIsVisible;
    private DozeTrigger mIterateTrigger;
    private int mLastDisplayDensity;
    private long mLastTabKeyEventTime;
    private RecentsPackageMonitor mPackageMonitor;
    private boolean mReceivedNewIntent;
    private RecentsView mRecentsView;
    private SystemBarScrimViews mScrimViews;
    private Handler mHandler = new Handler();
    private int mLastDeviceOrientation = 0;
    private final UserInteractionEvent mUserInteractionEvent = new UserInteractionEvent();
    private final Runnable mSendEnterWindowAnimationCompleteRunnable = new Runnable() {
        @Override
        public void run() {
            RecentsActivity.m1025com_android_systemui_recents_RecentsActivity_lambda$2();
        }
    };
    final BroadcastReceiver mSystemBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.SCREEN_OFF")) {
                RecentsActivity.this.dismissRecentsToHomeIfVisible(false);
            } else {
                if (!action.equals("android.intent.action.TIME_SET")) {
                    return;
                }
                Prefs.putLong(RecentsActivity.this, "OverviewLastStackTaskActiveTime", 0L);
            }
        }
    };
    private final ViewTreeObserver.OnPreDrawListener mRecentsDrawnEventListener = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            RecentsActivity.this.mRecentsView.getViewTreeObserver().removeOnPreDrawListener(this);
            EventBus.getDefault().post(new RecentsDrawnEvent());
            return true;
        }
    };

    static void m1025com_android_systemui_recents_RecentsActivity_lambda$2() {
        EventBus.getDefault().send(new EnterRecentsWindowAnimationCompletedEvent());
    }

    class LaunchHomeRunnable implements Runnable {
        Intent mLaunchIntent;
        ActivityOptions mOpts;

        public LaunchHomeRunnable(Intent launchIntent, ActivityOptions opts) {
            this.mLaunchIntent = launchIntent;
            this.mOpts = opts;
        }

        @Override
        public void run() {
            try {
                RecentsActivity.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        LaunchHomeRunnable.this.m1028x203d731f();
                    }
                });
            } catch (Exception e) {
                Log.e("RecentsActivity", RecentsActivity.this.getString(R.string.recents_launch_error_message, new Object[]{"Home"}), e);
            }
        }

        void m1028x203d731f() {
            ActivityOptions opts = this.mOpts;
            if (opts == null) {
                opts = ActivityOptions.makeCustomAnimation(RecentsActivity.this, R.anim.recents_to_launcher_enter, R.anim.recents_to_launcher_exit);
            }
            RecentsActivity.this.startActivityAsUser(this.mLaunchIntent, opts.toBundle(), UserHandle.CURRENT);
        }
    }

    boolean dismissRecentsToFocusedTask(int logCategory) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        return ssp.isRecentsActivityVisible() && this.mRecentsView.launchFocusedTask(logCategory);
    }

    boolean dismissRecentsToLaunchTargetTaskOrHome() {
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (ssp.isRecentsActivityVisible()) {
            if (this.mRecentsView.launchPreviousTask()) {
                return true;
            }
            dismissRecentsToHome(true);
            return false;
        }
        return false;
    }

    boolean dismissRecentsToFocusedTaskOrHome() {
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (!ssp.isRecentsActivityVisible()) {
            return false;
        }
        if (this.mRecentsView.launchFocusedTask(0)) {
            return true;
        }
        dismissRecentsToHome(true);
        return true;
    }

    void dismissRecentsToHome(boolean animateTaskViews) {
        dismissRecentsToHome(animateTaskViews, null);
    }

    void dismissRecentsToHome(boolean animateTaskViews, ActivityOptions overrideAnimation) {
        DismissRecentsToHomeAnimationStarted dismissEvent = new DismissRecentsToHomeAnimationStarted(animateTaskViews);
        dismissEvent.addPostAnimationCallback(new LaunchHomeRunnable(this.mHomeIntent, overrideAnimation));
        Recents.getSystemServices().sendCloseSystemWindows("homekey");
        EventBus.getDefault().send(dismissEvent);
    }

    boolean dismissRecentsToHomeIfVisible(boolean animated) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (ssp.isRecentsActivityVisible()) {
            dismissRecentsToHome(animated);
            return true;
        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mFinishedOnStartup = false;
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (ssp == null) {
            this.mFinishedOnStartup = true;
            finish();
            return;
        }
        EventBus.getDefault().register(this, 2);
        this.mPackageMonitor = new RecentsPackageMonitor();
        this.mPackageMonitor.register(this);
        setContentView(R.layout.recents);
        takeKeyEvents(true);
        this.mRecentsView = (RecentsView) findViewById(R.id.recents_view);
        this.mRecentsView.setSystemUiVisibility(1792);
        this.mScrimViews = new SystemBarScrimViews(this);
        getWindow().getAttributes().privateFlags |= 16384;
        Configuration appConfiguration = Utilities.getAppConfiguration(this);
        this.mLastDeviceOrientation = appConfiguration.orientation;
        this.mLastDisplayDensity = appConfiguration.densityDpi;
        this.mFocusTimerDuration = getResources().getInteger(R.integer.recents_auto_advance_duration);
        this.mIterateTrigger = new DozeTrigger(this.mFocusTimerDuration, new Runnable() {
            @Override
            public void run() {
                RecentsActivity.this.dismissRecentsToFocusedTask(288);
            }
        });
        getWindow().setBackgroundDrawable(this.mRecentsView.getBackgroundScrim());
        this.mHomeIntent = new Intent("android.intent.action.MAIN", (Uri) null);
        this.mHomeIntent.addCategory("android.intent.category.HOME");
        this.mHomeIntent.addFlags(270532608);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.TIME_SET");
        registerReceiver(this.mSystemBroadcastReceiver, filter);
        getWindow().addPrivateFlags(64);
        reloadStackView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().send(new RecentsVisibilityChangedEvent(this, true));
        MetricsLogger.visible(this, 224);
        this.mRecentsView.getViewTreeObserver().addOnPreDrawListener(this.mRecentsDrawnEventListener);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.mReceivedNewIntent = true;
        reloadStackView();
    }

    private void reloadStackView() {
        int launchTaskIndexInStack;
        RecentsTaskLoader loader = Recents.getTaskLoader();
        RecentsTaskLoadPlan loadPlan = RecentsImpl.consumeInstanceLoadPlan();
        if (loadPlan == null) {
            loadPlan = loader.createLoadPlan(this);
        }
        RecentsConfiguration config = Recents.getConfiguration();
        RecentsActivityLaunchState launchState = config.getLaunchState();
        if (!loadPlan.hasTasks()) {
            loader.preloadTasks(loadPlan, launchState.launchedToTaskId, !launchState.launchedFromHome);
        }
        RecentsTaskLoadPlan.Options loadOpts = new RecentsTaskLoadPlan.Options();
        loadOpts.runningTaskId = launchState.launchedToTaskId;
        loadOpts.numVisibleTasks = launchState.launchedNumVisibleTasks;
        loadOpts.numVisibleTaskThumbnails = launchState.launchedNumVisibleThumbnails;
        loader.loadTasks(this, loadPlan, loadOpts);
        TaskStack stack = loadPlan.getTaskStack();
        this.mRecentsView.onReload(this.mIsVisible, stack.getTaskCount() == 0);
        this.mRecentsView.updateStack(stack, true);
        boolean animateNavBarScrim = !launchState.launchedViaDockGesture;
        this.mScrimViews.updateNavBarScrim(animateNavBarScrim, stack.getTaskCount() > 0, null);
        boolean wasLaunchedByAm = (launchState.launchedFromHome || launchState.launchedFromApp) ? false : true;
        if (wasLaunchedByAm) {
            EventBus.getDefault().send(new EnterRecentsWindowAnimationCompletedEvent());
        }
        if (launchState.launchedWithAltTab) {
            MetricsLogger.count(this, "overview_trigger_alttab", 1);
        } else {
            MetricsLogger.count(this, "overview_trigger_nav_btn", 1);
        }
        if (launchState.launchedFromApp) {
            Task launchTarget = stack.getLaunchTarget();
            if (launchTarget != null) {
                launchTaskIndexInStack = stack.indexOfStackTask(launchTarget);
            } else {
                launchTaskIndexInStack = 0;
            }
            MetricsLogger.count(this, "overview_source_app", 1);
            MetricsLogger.histogram(this, "overview_source_app_index", launchTaskIndexInStack);
        } else {
            MetricsLogger.count(this, "overview_source_home", 1);
        }
        int taskCount = this.mRecentsView.getStack().getTaskCount();
        MetricsLogger.histogram(this, "overview_task_count", taskCount);
        this.mIsVisible = true;
    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        this.mHandler.removeCallbacks(this.mSendEnterWindowAnimationCompleteRunnable);
        if (!this.mReceivedNewIntent) {
            this.mHandler.post(this.mSendEnterWindowAnimationCompleteRunnable);
        } else {
            this.mSendEnterWindowAnimationCompleteRunnable.run();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mIgnoreAltTabRelease = false;
        this.mIterateTrigger.stopDozing();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Configuration newDeviceConfiguration = Utilities.getAppConfiguration(this);
        int numStackTasks = this.mRecentsView.getStack().getStackTaskCount();
        EventBus.getDefault().send(new ConfigurationChangedEvent(false, this.mLastDeviceOrientation != newDeviceConfiguration.orientation, this.mLastDisplayDensity != newDeviceConfiguration.densityDpi, numStackTasks > 0));
        this.mLastDeviceOrientation = newDeviceConfiguration.orientation;
        this.mLastDisplayDensity = newDeviceConfiguration.densityDpi;
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        RecentsConfiguration config = Recents.getConfiguration();
        RecentsActivityLaunchState launchState = config.getLaunchState();
        RecentsTaskLoader loader = Recents.getTaskLoader();
        RecentsTaskLoadPlan loadPlan = loader.createLoadPlan(this);
        loader.preloadTasks(loadPlan, -1, false);
        RecentsTaskLoadPlan.Options loadOpts = new RecentsTaskLoadPlan.Options();
        loadOpts.numVisibleTasks = launchState.launchedNumVisibleTasks;
        loadOpts.numVisibleTaskThumbnails = launchState.launchedNumVisibleThumbnails;
        loader.loadTasks(this, loadPlan, loadOpts);
        TaskStack stack = loadPlan.getTaskStack();
        int numStackTasks = stack.getStackTaskCount();
        boolean showDeferredAnimation = numStackTasks > 0;
        EventBus.getDefault().send(new ConfigurationChangedEvent(true, false, false, numStackTasks > 0));
        EventBus.getDefault().send(new MultiWindowStateChangedEvent(isInMultiWindowMode, showDeferredAnimation, stack));
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.mIsVisible = false;
        this.mReceivedNewIntent = false;
        EventBus.getDefault().send(new RecentsVisibilityChangedEvent(this, false));
        MetricsLogger.hidden(this, 224);
        RecentsConfiguration config = Recents.getConfiguration();
        RecentsActivityLaunchState launchState = config.getLaunchState();
        launchState.reset();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.mFinishedOnStartup) {
            return;
        }
        unregisterReceiver(this.mSystemBroadcastReceiver);
        this.mPackageMonitor.unregister();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this.mScrimViews, 2);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this.mScrimViews);
    }

    @Override
    public void onTrimMemory(int level) {
        RecentsTaskLoader loader = Recents.getTaskLoader();
        if (loader == null) {
            return;
        }
        loader.onTrimMemory(level);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 19:
                EventBus.getDefault().send(new FocusNextTaskViewEvent(0));
                return true;
            case 20:
                EventBus.getDefault().send(new FocusPreviousTaskViewEvent());
                return true;
            case 61:
                int altTabKeyDelay = getResources().getInteger(R.integer.recents_alt_tab_key_delay);
                boolean hasRepKeyTimeElapsed = SystemClock.elapsedRealtime() - this.mLastTabKeyEventTime > ((long) altTabKeyDelay);
                if (event.getRepeatCount() <= 0 || hasRepKeyTimeElapsed) {
                    boolean backward = event.isShiftPressed();
                    if (backward) {
                        EventBus.getDefault().send(new FocusPreviousTaskViewEvent());
                    } else {
                        EventBus.getDefault().send(new FocusNextTaskViewEvent(0));
                    }
                    this.mLastTabKeyEventTime = SystemClock.elapsedRealtime();
                    if (event.isAltPressed()) {
                        this.mIgnoreAltTabRelease = false;
                    }
                }
                return true;
            case 67:
            case 112:
                if (event.getRepeatCount() <= 0) {
                    EventBus.getDefault().send(new DismissFocusedTaskViewEvent());
                    MetricsLogger.histogram(this, "overview_task_dismissed_source", 0);
                    return true;
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onUserInteraction() {
        EventBus.getDefault().send(this.mUserInteractionEvent);
    }

    @Override
    public void onBackPressed() {
        EventBus.getDefault().send(new ToggleRecentsEvent());
    }

    public final void onBusEvent(ToggleRecentsEvent event) {
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        if (launchState.launchedFromHome) {
            dismissRecentsToHome(true);
        } else {
            dismissRecentsToLaunchTargetTaskOrHome();
        }
    }

    public final void onBusEvent(IterateRecentsEvent event) {
        RecentsDebugFlags debugFlags = Recents.getDebugFlags();
        int timerIndicatorDuration = 0;
        if (debugFlags.isFastToggleRecentsEnabled()) {
            timerIndicatorDuration = getResources().getInteger(R.integer.recents_subsequent_auto_advance_duration);
            this.mIterateTrigger.setDozeDuration(timerIndicatorDuration);
            if (!this.mIterateTrigger.isDozing()) {
                this.mIterateTrigger.startDozing();
            } else {
                this.mIterateTrigger.poke();
            }
        }
        EventBus.getDefault().send(new FocusNextTaskViewEvent(timerIndicatorDuration));
        MetricsLogger.action(this, 276);
    }

    public final void onBusEvent(UserInteractionEvent event) {
        this.mIterateTrigger.stopDozing();
    }

    public final void onBusEvent(HideRecentsEvent event) {
        if (event.triggeredFromAltTab) {
            if (this.mIgnoreAltTabRelease) {
                return;
            }
            dismissRecentsToFocusedTaskOrHome();
        } else {
            if (!event.triggeredFromHomeKey) {
                return;
            }
            dismissRecentsToHome(true);
            EventBus.getDefault().send(this.mUserInteractionEvent);
        }
    }

    public final void onBusEvent(EnterRecentsWindowLastAnimationFrameEvent event) {
        EventBus.getDefault().send(new UpdateFreeformTaskViewVisibilityEvent(true));
        this.mRecentsView.getViewTreeObserver().addOnPreDrawListener(this);
        this.mRecentsView.invalidate();
    }

    public final void onBusEvent(ExitRecentsWindowFirstAnimationFrameEvent event) {
        if (this.mRecentsView.isLastTaskLaunchedFreeform()) {
            EventBus.getDefault().send(new UpdateFreeformTaskViewVisibilityEvent(false));
        }
        this.mRecentsView.getViewTreeObserver().addOnPreDrawListener(this);
        this.mRecentsView.invalidate();
    }

    public final void onBusEvent(DockedFirstAnimationFrameEvent event) {
        this.mRecentsView.getViewTreeObserver().addOnPreDrawListener(this);
        this.mRecentsView.invalidate();
    }

    public final void onBusEvent(CancelEnterRecentsWindowAnimationEvent event) {
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        int launchToTaskId = launchState.launchedToTaskId;
        if (launchToTaskId == -1) {
            return;
        }
        if (event.launchTask != null && launchToTaskId == event.launchTask.key.id) {
            return;
        }
        SystemServicesProxy ssp = Recents.getSystemServices();
        ssp.cancelWindowTransition(launchState.launchedToTaskId);
        ssp.cancelThumbnailTransition(getTaskId());
    }

    public final void onBusEvent(ShowApplicationInfoEvent event) {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.fromParts("package", event.task.key.getComponent().getPackageName(), null));
        intent.setComponent(intent.resolveActivity(getPackageManager()));
        TaskStackBuilder.create(this).addNextIntentWithParentStack(intent).startActivities(null, new UserHandle(event.task.key.userId));
        MetricsLogger.count(this, "overview_app_info", 1);
    }

    public final void onBusEvent(ShowIncompatibleAppOverlayEvent event) {
        if (this.mIncompatibleAppOverlay == null) {
            this.mIncompatibleAppOverlay = Utilities.findViewStubById(this, R.id.incompatible_app_overlay_stub).inflate();
            this.mIncompatibleAppOverlay.setWillNotDraw(false);
            this.mIncompatibleAppOverlay.setVisibility(0);
        }
        this.mIncompatibleAppOverlay.animate().alpha(1.0f).setDuration(150L).setInterpolator(Interpolators.ALPHA_IN).start();
    }

    public final void onBusEvent(HideIncompatibleAppOverlayEvent event) {
        if (this.mIncompatibleAppOverlay != null) {
            this.mIncompatibleAppOverlay.animate().alpha(0.0f).setDuration(150L).setInterpolator(Interpolators.ALPHA_OUT).start();
        }
    }

    public final void onBusEvent(DeleteTaskDataEvent event) {
        RecentsTaskLoader loader = Recents.getTaskLoader();
        loader.deleteTaskData(event.task, false);
        SystemServicesProxy ssp = Recents.getSystemServices();
        ssp.removeTask(event.task.key.id);
    }

    public final void onBusEvent(AllTaskViewsDismissedEvent event) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (ssp.hasDockedTask()) {
            this.mRecentsView.showEmptyView(event.msgResId);
        } else {
            dismissRecentsToHome(false);
        }
        MetricsLogger.count(this, "overview_task_all_dismissed", 1);
    }

    public final void onBusEvent(LaunchTaskSucceededEvent event) {
        MetricsLogger.histogram(this, "overview_task_launch_index", event.taskIndexFromStackFront);
    }

    public final void onBusEvent(LaunchTaskFailedEvent event) {
        dismissRecentsToHome(true);
        MetricsLogger.count(this, "overview_task_launch_failed", 1);
    }

    public final void onBusEvent(ScreenPinningRequestEvent event) {
        MetricsLogger.count(this, "overview_screen_pinned", 1);
    }

    public final void onBusEvent(DebugFlagsChangedEvent event) {
        finish();
    }

    public final void onBusEvent(StackViewScrolledEvent event) {
        this.mIgnoreAltTabRelease = true;
    }

    public final void onBusEvent(DockedTopTaskEvent event) {
        this.mRecentsView.getViewTreeObserver().addOnPreDrawListener(this.mRecentsDrawnEventListener);
        this.mRecentsView.invalidate();
    }

    @Override
    public boolean onPreDraw() {
        this.mRecentsView.getViewTreeObserver().removeOnPreDrawListener(this);
        this.mRecentsView.post(new Runnable() {
            @Override
            public void run() {
                Recents.getSystemServices().endProlongedAnimations();
            }
        });
        return true;
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        EventBus.getDefault().dump(prefix, writer);
        Recents.getTaskLoader().dump(prefix, writer);
        String id = Integer.toHexString(System.identityHashCode(this));
        writer.print(prefix);
        writer.print("RecentsActivity");
        writer.print(" visible=");
        writer.print(this.mIsVisible ? "Y" : "N");
        writer.print(" [0x");
        writer.print(id);
        writer.print("]");
        writer.println();
        if (this.mRecentsView == null) {
            return;
        }
        this.mRecentsView.dump(prefix, writer);
    }
}
