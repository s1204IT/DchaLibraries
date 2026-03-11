package com.android.systemui.recents.tv;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsActivityLaunchState;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.RecentsImpl;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.CancelEnterRecentsWindowAnimationEvent;
import com.android.systemui.recents.events.activity.DismissRecentsToHomeAnimationStarted;
import com.android.systemui.recents.events.activity.EnterRecentsWindowAnimationCompletedEvent;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.activity.LaunchTaskFailedEvent;
import com.android.systemui.recents.events.activity.ToggleRecentsEvent;
import com.android.systemui.recents.events.component.RecentsVisibilityChangedEvent;
import com.android.systemui.recents.events.ui.AllTaskViewsDismissedEvent;
import com.android.systemui.recents.events.ui.DeleteTaskDataEvent;
import com.android.systemui.recents.events.ui.UserInteractionEvent;
import com.android.systemui.recents.events.ui.focus.DismissFocusedTaskViewEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.RecentsPackageMonitor;
import com.android.systemui.recents.model.RecentsTaskLoadPlan;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.tv.animations.HomeRecentsEnterExitAnimationHolder;
import com.android.systemui.recents.tv.views.RecentsTvView;
import com.android.systemui.recents.tv.views.TaskCardView;
import com.android.systemui.recents.tv.views.TaskStackHorizontalGridView;
import com.android.systemui.recents.tv.views.TaskStackHorizontalViewAdapter;
import com.android.systemui.tv.pip.PipManager;
import com.android.systemui.tv.pip.PipRecentsOverlayManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecentsTvActivity extends Activity implements ViewTreeObserver.OnPreDrawListener {
    private FinishRecentsRunnable mFinishLaunchHomeRunnable;
    private boolean mFinishedOnStartup;
    private HomeRecentsEnterExitAnimationHolder mHomeRecentsEnterExitAnimationHolder;
    private boolean mIgnoreAltTabRelease;
    private boolean mLaunchedFromHome;
    private RecentsPackageMonitor mPackageMonitor;
    private PipRecentsOverlayManager mPipRecentsOverlayManager;
    private View mPipView;
    private RecentsTvView mRecentsView;
    private boolean mTalkBackEnabled;
    private TaskStackHorizontalGridView mTaskStackHorizontalGridView;
    private TaskStackHorizontalViewAdapter mTaskStackViewAdapter;
    private final PipManager mPipManager = PipManager.getInstance();
    private final PipManager.Listener mPipListener = new PipManager.Listener() {
        @Override
        public void onPipEntered() {
            RecentsTvActivity.this.updatePipUI();
        }

        @Override
        public void onPipActivityClosed() {
            RecentsTvActivity.this.updatePipUI();
        }

        @Override
        public void onShowPipMenu() {
            RecentsTvActivity.this.updatePipUI();
        }

        @Override
        public void onMoveToFullscreen() {
            RecentsTvActivity.this.dismissRecentsToLaunchTargetTaskOrHome(false);
        }

        @Override
        public void onPipResizeAboutToStart() {
        }
    };
    private final PipRecentsOverlayManager.Callback mPipRecentsOverlayManagerCallback = new PipRecentsOverlayManager.Callback() {
        @Override
        public void onClosed() {
            RecentsTvActivity.this.dismissRecentsToLaunchTargetTaskOrHome(true);
        }

        @Override
        public void onBackPressed() {
            RecentsTvActivity.this.onBackPressed();
        }

        @Override
        public void onRecentsFocused() {
            if (RecentsTvActivity.this.mTalkBackEnabled) {
                RecentsTvActivity.this.mTaskStackHorizontalGridView.requestFocus();
                RecentsTvActivity.this.mTaskStackHorizontalGridView.sendAccessibilityEvent(8);
            }
            RecentsTvActivity.this.mTaskStackHorizontalGridView.startFocusGainAnimation();
        }
    };
    private final View.OnFocusChangeListener mPipViewFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus) {
                return;
            }
            RecentsTvActivity.this.requestPipControlsFocus();
        }
    };

    class FinishRecentsRunnable implements Runnable {
        Intent mLaunchIntent;

        public FinishRecentsRunnable(Intent launchIntent) {
            this.mLaunchIntent = launchIntent;
        }

        @Override
        public void run() {
            try {
                ActivityOptions opts = ActivityOptions.makeCustomAnimation(RecentsTvActivity.this, R.anim.recents_to_launcher_enter, R.anim.recents_to_launcher_exit);
                RecentsTvActivity.this.startActivityAsUser(this.mLaunchIntent, opts.toBundle(), UserHandle.CURRENT);
            } catch (Exception e) {
                Log.e("RecentsTvActivity", RecentsTvActivity.this.getString(R.string.recents_launch_error_message, new Object[]{"Home"}), e);
            }
        }
    }

    private void updateRecentsTasks() {
        RecentsTaskLoader loader = Recents.getTaskLoader();
        RecentsTaskLoadPlan plan = RecentsImpl.consumeInstanceLoadPlan();
        if (plan == null) {
            plan = loader.createLoadPlan(this);
        }
        RecentsConfiguration config = Recents.getConfiguration();
        RecentsActivityLaunchState launchState = config.getLaunchState();
        if (!plan.hasTasks()) {
            loader.preloadTasks(plan, -1, !launchState.launchedFromHome);
        }
        int numVisibleTasks = TaskCardView.getNumberOfVisibleTasks(getApplicationContext());
        this.mLaunchedFromHome = launchState.launchedFromHome;
        TaskStack stack = plan.getTaskStack();
        RecentsTaskLoadPlan.Options loadOpts = new RecentsTaskLoadPlan.Options();
        loadOpts.runningTaskId = launchState.launchedToTaskId;
        loadOpts.numVisibleTasks = numVisibleTasks;
        loadOpts.numVisibleTaskThumbnails = numVisibleTasks;
        loader.loadTasks(this, plan, loadOpts);
        List stackTasks = stack.getStackTasks();
        Collections.reverse(stackTasks);
        if (this.mTaskStackViewAdapter == null) {
            this.mTaskStackViewAdapter = new TaskStackHorizontalViewAdapter(stackTasks);
            this.mTaskStackHorizontalGridView = this.mRecentsView.setTaskStackViewAdapter(this.mTaskStackViewAdapter);
            this.mHomeRecentsEnterExitAnimationHolder = new HomeRecentsEnterExitAnimationHolder(getApplicationContext(), this.mTaskStackHorizontalGridView);
        } else {
            this.mTaskStackViewAdapter.setNewStackTasks(stackTasks);
        }
        this.mRecentsView.init(stack);
        if (launchState.launchedToTaskId == -1) {
            return;
        }
        ArrayList<Task> tasks = stack.getStackTasks();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task t = tasks.get(i);
            if (t.key.id == launchState.launchedToTaskId) {
                t.isLaunchTarget = true;
                return;
            }
        }
    }

    boolean dismissRecentsToLaunchTargetTaskOrHome(boolean animate) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (ssp.isRecentsActivityVisible()) {
            if (this.mRecentsView.launchPreviousTask(animate)) {
                return true;
            }
            dismissRecentsToHome(animate);
            return false;
        }
        return false;
    }

    boolean dismissRecentsToFocusedTaskOrHome() {
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (ssp.isRecentsActivityVisible()) {
            if (this.mRecentsView.launchFocusedTask()) {
                return true;
            }
            dismissRecentsToHome(true);
            return true;
        }
        return false;
    }

    void dismissRecentsToHome(boolean animateTaskViews) {
        Runnable closeSystemWindows = new Runnable() {
            @Override
            public void run() {
                Recents.getSystemServices().sendCloseSystemWindows("homekey");
            }
        };
        DismissRecentsToHomeAnimationStarted dismissEvent = new DismissRecentsToHomeAnimationStarted(animateTaskViews);
        dismissEvent.addPostAnimationCallback(this.mFinishLaunchHomeRunnable);
        dismissEvent.addPostAnimationCallback(closeSystemWindows);
        if (this.mTaskStackHorizontalGridView.getChildCount() > 0 && animateTaskViews) {
            this.mHomeRecentsEnterExitAnimationHolder.startExitAnimation(dismissEvent);
        } else {
            closeSystemWindows.run();
            this.mFinishLaunchHomeRunnable.run();
        }
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
        this.mPipRecentsOverlayManager = PipManager.getInstance().getPipRecentsOverlayManager();
        EventBus.getDefault().register(this, 2);
        this.mPackageMonitor = new RecentsPackageMonitor();
        this.mPackageMonitor.register(this);
        setContentView(R.layout.recents_on_tv);
        this.mRecentsView = (RecentsTvView) findViewById(R.id.recents_view);
        this.mRecentsView.setSystemUiVisibility(1792);
        this.mPipView = findViewById(R.id.pip);
        this.mPipView.setOnFocusChangeListener(this.mPipViewFocusChangeListener);
        Rect pipBounds = this.mPipManager.getRecentsFocusedPipBounds();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.mPipView.getLayoutParams();
        lp.width = pipBounds.width();
        lp.height = pipBounds.height();
        lp.leftMargin = pipBounds.left;
        lp.topMargin = pipBounds.top;
        this.mPipView.setLayoutParams(lp);
        this.mPipRecentsOverlayManager.setCallback(this.mPipRecentsOverlayManagerCallback);
        getWindow().getAttributes().privateFlags |= 16384;
        Intent homeIntent = new Intent("android.intent.action.MAIN", (Uri) null);
        homeIntent.addCategory("android.intent.category.HOME");
        homeIntent.addFlags(270532608);
        homeIntent.putExtra("com.android.systemui.recents.tv.RecentsTvActivity.RECENTS_HOME_INTENT_EXTRA", true);
        this.mFinishLaunchHomeRunnable = new FinishRecentsRunnable(homeIntent);
        this.mPipManager.addListener(this.mPipListener);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        if (this.mLaunchedFromHome) {
            this.mHomeRecentsEnterExitAnimationHolder.startEnterAnimation(this.mPipManager.isPipShown());
        }
        EventBus.getDefault().send(new EnterRecentsWindowAnimationCompletedEvent());
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mPipRecentsOverlayManager.onRecentsResumed();
        updateRecentsTasks();
        RecentsConfiguration config = Recents.getConfiguration();
        RecentsActivityLaunchState launchState = config.getLaunchState();
        boolean wasLaunchedByAm = (launchState.launchedFromHome || launchState.launchedFromApp) ? false : true;
        if (wasLaunchedByAm) {
            EventBus.getDefault().send(new EnterRecentsWindowAnimationCompletedEvent());
        }
        SystemServicesProxy ssp = Recents.getSystemServices();
        EventBus.getDefault().send(new RecentsVisibilityChangedEvent(this, true));
        if (this.mTaskStackHorizontalGridView.getStack().getTaskCount() > 1 && !this.mLaunchedFromHome) {
            this.mTaskStackHorizontalGridView.setSelectedPosition(1);
        } else {
            this.mTaskStackHorizontalGridView.setSelectedPosition(0);
        }
        this.mRecentsView.getViewTreeObserver().addOnPreDrawListener(this);
        View dismissPlaceholder = findViewById(R.id.dismiss_placeholder);
        this.mTalkBackEnabled = ssp.isTouchExplorationEnabled();
        if (this.mTalkBackEnabled) {
            dismissPlaceholder.setAccessibilityTraversalBefore(R.id.task_list);
            dismissPlaceholder.setAccessibilityTraversalAfter(R.id.dismiss_placeholder);
            this.mTaskStackHorizontalGridView.setAccessibilityTraversalAfter(R.id.dismiss_placeholder);
            this.mTaskStackHorizontalGridView.setAccessibilityTraversalBefore(R.id.pip);
            dismissPlaceholder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RecentsTvActivity.this.mTaskStackHorizontalGridView.requestFocus();
                    RecentsTvActivity.this.mTaskStackHorizontalGridView.sendAccessibilityEvent(8);
                    Task focusedTask = RecentsTvActivity.this.mTaskStackHorizontalGridView.getFocusedTask();
                    if (focusedTask == null) {
                        return;
                    }
                    RecentsTvActivity.this.mTaskStackViewAdapter.removeTask(focusedTask);
                    EventBus.getDefault().send(new DeleteTaskDataEvent(focusedTask));
                }
            });
        }
        if (this.mPipManager.isPipShown()) {
            if (this.mTalkBackEnabled) {
                this.mPipView.setVisibility(0);
            } else {
                this.mPipView.setVisibility(8);
            }
            this.mPipRecentsOverlayManager.requestFocus(this.mTaskStackViewAdapter.getItemCount() > 0);
            return;
        }
        this.mPipView.setVisibility(8);
        this.mPipRecentsOverlayManager.removePipRecentsOverlayView();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mPipRecentsOverlayManager.onRecentsPaused();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.mIgnoreAltTabRelease = false;
        EventBus.getDefault().send(new RecentsVisibilityChangedEvent(this, false));
        RecentsConfiguration config = Recents.getConfiguration();
        RecentsActivityLaunchState launchState = config.getLaunchState();
        launchState.reset();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.mPipManager.removeListener(this.mPipListener);
        if (this.mFinishedOnStartup) {
            return;
        }
        this.mPackageMonitor.unregister();
        EventBus.getDefault().unregister(this);
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
            case 67:
            case 112:
                EventBus.getDefault().send(new DismissFocusedTaskViewEvent());
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onUserInteraction() {
        EventBus.getDefault().send(new UserInteractionEvent());
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
            dismissRecentsToLaunchTargetTaskOrHome(true);
        }
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
        }
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

    public final void onBusEvent(DeleteTaskDataEvent event) {
        RecentsTaskLoader loader = Recents.getTaskLoader();
        loader.deleteTaskData(event.task, false);
        SystemServicesProxy ssp = Recents.getSystemServices();
        ssp.removeTask(event.task.key.id);
    }

    public final void onBusEvent(AllTaskViewsDismissedEvent event) {
        if (this.mPipManager.isPipShown()) {
            this.mRecentsView.showEmptyView();
            this.mPipRecentsOverlayManager.requestFocus(false);
        } else {
            dismissRecentsToHome(false);
        }
    }

    public final void onBusEvent(LaunchTaskFailedEvent event) {
        dismissRecentsToHome(true);
    }

    @Override
    public boolean onPreDraw() {
        this.mRecentsView.getViewTreeObserver().removeOnPreDrawListener(this);
        if (this.mLaunchedFromHome) {
            this.mHomeRecentsEnterExitAnimationHolder.setEnterFromHomeStartingAnimationValues(this.mPipManager.isPipShown());
        } else {
            this.mHomeRecentsEnterExitAnimationHolder.setEnterFromAppStartingAnimationValues(this.mPipManager.isPipShown());
        }
        this.mRecentsView.post(new Runnable() {
            @Override
            public void run() {
                Recents.getSystemServices().endProlongedAnimations();
            }
        });
        return true;
    }

    public void updatePipUI() {
        if (!this.mPipManager.isPipShown()) {
            this.mPipRecentsOverlayManager.removePipRecentsOverlayView();
            this.mTaskStackHorizontalGridView.startFocusLossAnimation();
        } else {
            Log.w("RecentsTvActivity", "An activity entered PIP mode while Recents is shown");
        }
    }

    public void requestPipControlsFocus() {
        if (!this.mPipManager.isPipShown()) {
            return;
        }
        this.mTaskStackHorizontalGridView.startFocusLossAnimation();
        this.mPipRecentsOverlayManager.requestFocus(this.mTaskStackViewAdapter.getItemCount() > 0);
    }
}
