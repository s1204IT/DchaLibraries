package com.android.systemui.recents;

import android.app.Activity;
import android.app.ActivityOptions;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewStub;
import android.widget.Toast;
import com.android.systemui.R;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.recents.Constants;
import com.android.systemui.recents.RecentsAppWidgetHost;
import com.android.systemui.recents.misc.DebugTrigger;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.RecentsTaskLoadPlan;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.SpaceNode;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.views.DebugOverlayView;
import com.android.systemui.recents.views.RecentsView;
import com.android.systemui.recents.views.SystemBarScrimViews;
import com.android.systemui.recents.views.ViewAnimation;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class RecentsActivity extends Activity implements RecentsAppWidgetHost.RecentsAppWidgetHostCallbacks, DebugOverlayView.DebugOverlayViewCallbacks, RecentsView.RecentsViewCallbacks {
    RecentsAppWidgetHost mAppWidgetHost;
    RecentsConfiguration mConfig;
    DebugOverlayView mDebugOverlay;
    ViewStub mDebugOverlayStub;
    View mEmptyView;
    ViewStub mEmptyViewStub;
    FinishRecentsRunnable mFinishLaunchHomeRunnable;
    long mLastTabKeyEventTime;
    RecentsView mRecentsView;
    SystemBarScrimViews mScrimViews;
    AppWidgetHostView mSearchAppWidgetHostView;
    AppWidgetProviderInfo mSearchAppWidgetInfo;
    private PhoneStatusBar mStatusBar;
    final BroadcastReceiver mServiceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("action_hide_recents_activity")) {
                if (intent.getBooleanExtra("triggeredFromAltTab", false)) {
                    RecentsActivity.this.dismissRecentsToFocusedTaskOrHome(false);
                    return;
                } else {
                    if (intent.getBooleanExtra("triggeredFromHomeKey", false)) {
                        RecentsActivity.this.dismissRecentsToHome(true);
                        return;
                    }
                    return;
                }
            }
            if (action.equals("action_toggle_recents_activity")) {
                RecentsActivity.this.dismissRecentsToFocusedTaskOrHome(true);
            } else if (action.equals("action_start_enter_animation")) {
                RecentsActivity.this.onEnterAnimationTriggered();
                setResultCode(-1);
            }
        }
    };
    final BroadcastReceiver mSystemBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.SCREEN_OFF")) {
                RecentsActivity.this.dismissRecentsToHome(false);
            } else if (action.equals("android.search.action.GLOBAL_SEARCH_ACTIVITY_CHANGED")) {
                RecentsActivity.this.refreshSearchWidget();
            }
        }
    };
    final DebugTrigger mDebugTrigger = new DebugTrigger(new Runnable() {
        @Override
        public void run() {
            RecentsActivity.this.onDebugModeTriggered();
        }
    });

    class FinishRecentsRunnable implements Runnable {
        Intent mLaunchIntent;
        ActivityOptions mLaunchOpts;

        public FinishRecentsRunnable(Intent launchIntent, ActivityOptions opts) {
            this.mLaunchIntent = launchIntent;
            this.mLaunchOpts = opts;
        }

        @Override
        public void run() {
            if (this.mLaunchIntent != null) {
                if (this.mLaunchOpts != null) {
                    RecentsActivity.this.startActivityAsUser(this.mLaunchIntent, this.mLaunchOpts.toBundle(), UserHandle.CURRENT);
                    return;
                } else {
                    RecentsActivity.this.startActivityAsUser(this.mLaunchIntent, UserHandle.CURRENT);
                    return;
                }
            }
            RecentsActivity.this.finish();
            RecentsActivity.this.overridePendingTransition(R.anim.recents_to_launcher_enter, R.anim.recents_to_launcher_exit);
        }
    }

    void updateRecentsTasks(Intent launchIntent) {
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        RecentsTaskLoadPlan plan = AlternateRecentsComponent.consumeInstanceLoadPlan();
        if (plan == null) {
            plan = loader.createLoadPlan(this);
        }
        if (plan.getTaskStack() == null) {
            loader.preloadTasks(plan, this.mConfig.launchedFromHome);
        }
        RecentsTaskLoadPlan.Options loadOpts = new RecentsTaskLoadPlan.Options();
        loadOpts.runningTaskId = this.mConfig.launchedToTaskId;
        loadOpts.numVisibleTasks = this.mConfig.launchedNumVisibleTasks;
        loadOpts.numVisibleTaskThumbnails = this.mConfig.launchedNumVisibleThumbnails;
        loader.loadTasks(this, plan, loadOpts);
        SpaceNode root = plan.getSpaceNode();
        ArrayList<TaskStack> stacks = root.getStacks();
        boolean hasTasks = root.hasTasks();
        if (hasTasks) {
            this.mRecentsView.setTaskStacks(stacks);
        }
        this.mConfig.launchedWithNoRecentTasks = !hasTasks;
        Intent homeIntent = new Intent("android.intent.action.MAIN", (Uri) null);
        homeIntent.addCategory("android.intent.category.HOME");
        homeIntent.addFlags(270532608);
        this.mFinishLaunchHomeRunnable = new FinishRecentsRunnable(homeIntent, ActivityOptions.makeCustomAnimation(this, this.mConfig.launchedFromSearchHome ? R.anim.recents_to_search_launcher_enter : R.anim.recents_to_launcher_enter, this.mConfig.launchedFromSearchHome ? R.anim.recents_to_search_launcher_exit : R.anim.recents_to_launcher_exit));
        int taskStackCount = stacks.size();
        if (this.mConfig.launchedToTaskId != -1) {
            for (int i = 0; i < taskStackCount; i++) {
                TaskStack stack = stacks.get(i);
                ArrayList<Task> tasks = stack.getTasks();
                int taskCount = tasks.size();
                int j = 0;
                while (true) {
                    if (j < taskCount) {
                        Task t = tasks.get(j);
                        if (t.key.id != this.mConfig.launchedToTaskId) {
                            j++;
                        } else {
                            t.isLaunchTarget = true;
                            break;
                        }
                    }
                }
            }
        }
        if (this.mConfig.launchedWithNoRecentTasks) {
            if (this.mEmptyView == null) {
                this.mEmptyView = this.mEmptyViewStub.inflate();
            }
            this.mEmptyView.setVisibility(0);
            this.mRecentsView.setSearchBarVisibility(8);
        } else {
            if (this.mEmptyView != null) {
                this.mEmptyView.setVisibility(8);
            }
            if (this.mRecentsView.hasSearchBar()) {
                this.mRecentsView.setSearchBarVisibility(0);
            } else {
                addSearchBarAppWidgetView();
            }
        }
        this.mScrimViews.prepareEnterRecentsAnimation();
    }

    void bindSearchBarAppWidget() {
        Pair<Integer, AppWidgetProviderInfo> widgetInfo;
        SystemServicesProxy ssp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
        this.mSearchAppWidgetHostView = null;
        this.mSearchAppWidgetInfo = null;
        int appWidgetId = this.mConfig.searchBarAppWidgetId;
        if (appWidgetId >= 0) {
            this.mSearchAppWidgetInfo = ssp.getAppWidgetInfo(appWidgetId);
            if (this.mSearchAppWidgetInfo == null) {
                ssp.unbindSearchAppWidget(this.mAppWidgetHost, appWidgetId);
                appWidgetId = -1;
            }
        }
        if (appWidgetId < 0 && (widgetInfo = ssp.bindSearchAppWidget(this.mAppWidgetHost)) != null) {
            this.mConfig.updateSearchBarAppWidgetId(this, ((Integer) widgetInfo.first).intValue());
            this.mSearchAppWidgetInfo = (AppWidgetProviderInfo) widgetInfo.second;
        }
    }

    void addSearchBarAppWidgetView() {
        int appWidgetId = this.mConfig.searchBarAppWidgetId;
        if (appWidgetId >= 0) {
            this.mSearchAppWidgetHostView = this.mAppWidgetHost.createView(this, appWidgetId, this.mSearchAppWidgetInfo);
            Bundle opts = new Bundle();
            opts.putInt("appWidgetCategory", 4);
            this.mSearchAppWidgetHostView.updateAppWidgetOptions(opts);
            this.mSearchAppWidgetHostView.setPadding(0, 0, 0, 0);
            this.mRecentsView.setSearchBar(this.mSearchAppWidgetHostView);
            return;
        }
        this.mRecentsView.setSearchBar(null);
    }

    boolean dismissRecentsToFocusedTaskOrHome(boolean checkFilteredStackState) {
        SystemServicesProxy ssp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
        if (ssp.isRecentsTopMost(ssp.getTopMostTask(), null)) {
            if ((checkFilteredStackState && this.mRecentsView.unfilterFilteredStacks()) || this.mRecentsView.launchFocusedTask()) {
                return true;
            }
            if (this.mConfig.launchedFromHome) {
                dismissRecentsToHomeRaw(true);
                return true;
            }
            if (this.mRecentsView.launchPreviousTask()) {
                return true;
            }
            dismissRecentsToHomeRaw(true);
            return true;
        }
        return false;
    }

    void dismissRecentsToHomeRaw(boolean animated) {
        if (animated) {
            ReferenceCountedTrigger exitTrigger = new ReferenceCountedTrigger(this, null, this.mFinishLaunchHomeRunnable, null);
            this.mRecentsView.startExitToHomeAnimation(new ViewAnimation.TaskViewExitContext(exitTrigger));
        } else {
            this.mFinishLaunchHomeRunnable.run();
        }
    }

    boolean dismissRecentsToHome(boolean animated) {
        SystemServicesProxy ssp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
        if (!ssp.isRecentsTopMost(ssp.getTopMostTask(), null)) {
            return false;
        }
        dismissRecentsToHomeRaw(animated);
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RecentsTaskLoader.initialize(this);
        SystemServicesProxy ssp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
        this.mConfig = RecentsConfiguration.reinitialize(this, ssp);
        this.mAppWidgetHost = new RecentsAppWidgetHost(this, Constants.Values.App.AppWidgetHostId);
        setContentView(R.layout.recents);
        this.mRecentsView = (RecentsView) findViewById(R.id.recents_view);
        this.mRecentsView.setCallbacks(this);
        this.mRecentsView.setSystemUiVisibility(1792);
        this.mEmptyViewStub = (ViewStub) findViewById(R.id.empty_view_stub);
        this.mDebugOverlayStub = (ViewStub) findViewById(R.id.debug_overlay_stub);
        this.mScrimViews = new SystemBarScrimViews(this, this.mConfig);
        this.mStatusBar = (PhoneStatusBar) ((SystemUIApplication) getApplication()).getComponent(PhoneStatusBar.class);
        inflateDebugOverlay();
        bindSearchBarAppWidget();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.search.action.GLOBAL_SEARCH_ACTIVITY_CHANGED");
        registerReceiver(this.mSystemBroadcastReceiver, filter);
        try {
            Utilities.setShadowProperty("ambientRatio", String.valueOf(1.5f));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e2) {
            e2.printStackTrace();
        }
    }

    void inflateDebugOverlay() {
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (this.mDebugOverlay != null) {
            this.mDebugOverlay.clear();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        SystemServicesProxy ssp = loader.getSystemServicesProxy();
        AlternateRecentsComponent.notifyVisibilityChanged(this, ssp, true);
        IntentFilter filter = new IntentFilter();
        filter.addAction("action_hide_recents_activity");
        filter.addAction("action_toggle_recents_activity");
        filter.addAction("action_start_enter_animation");
        registerReceiver(this.mServiceBroadcastReceiver, filter);
        loader.registerReceivers(this, this.mRecentsView);
        updateRecentsTasks(getIntent());
        if (this.mConfig.launchedHasConfigurationChanged) {
            onEnterAnimationTriggered();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        SystemServicesProxy ssp = loader.getSystemServicesProxy();
        AlternateRecentsComponent.notifyVisibilityChanged(this, ssp, false);
        this.mRecentsView.onRecentsHidden();
        unregisterReceiver(this.mServiceBroadcastReceiver);
        loader.unregisterReceivers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.mSystemBroadcastReceiver);
        this.mAppWidgetHost.stopListening();
    }

    public void onEnterAnimationTriggered() {
        ReferenceCountedTrigger t = new ReferenceCountedTrigger(this, null, null, null);
        ViewAnimation.TaskViewEnterContext ctx = new ViewAnimation.TaskViewEnterContext(t);
        this.mRecentsView.startEnterRecentsAnimation(ctx);
        if (this.mConfig.searchBarAppWidgetId >= 0) {
            final WeakReference<RecentsAppWidgetHost.RecentsAppWidgetHostCallbacks> cbRef = new WeakReference<>(this);
            ctx.postAnimationTrigger.addLastDecrementRunnable(new Runnable() {
                @Override
                public void run() {
                    RecentsAppWidgetHost.RecentsAppWidgetHostCallbacks cb = (RecentsAppWidgetHost.RecentsAppWidgetHostCallbacks) cbRef.get();
                    if (cb != null) {
                        RecentsActivity.this.mAppWidgetHost.startListening(cb);
                    }
                }
            });
        }
        this.mScrimViews.startEnterRecentsAnimation();
    }

    @Override
    public void onTrimMemory(int level) {
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        if (loader != null) {
            loader.onTrimMemory(level);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 19:
                this.mRecentsView.focusNextTask(true);
                return true;
            case 20:
                this.mRecentsView.focusNextTask(false);
                return true;
            case 61:
                boolean hasRepKeyTimeElapsed = SystemClock.elapsedRealtime() - this.mLastTabKeyEventTime > ((long) this.mConfig.altTabKeyDelay);
                if (event.getRepeatCount() > 0 && !hasRepKeyTimeElapsed) {
                    return true;
                }
                boolean backward = event.isShiftPressed();
                this.mRecentsView.focusNextTask(backward ? false : true);
                this.mLastTabKeyEventTime = SystemClock.elapsedRealtime();
                return true;
            case 67:
            case 112:
                this.mRecentsView.dismissFocusedTask();
                return true;
            default:
                this.mDebugTrigger.onKeyEvent(keyCode);
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onUserInteraction() {
        this.mRecentsView.onUserInteraction();
    }

    @Override
    public void onBackPressed() {
        if (!this.mConfig.debugModeEnabled) {
            dismissRecentsToFocusedTaskOrHome(true);
        }
    }

    public void onDebugModeTriggered() {
        if (this.mConfig.developerOptionsEnabled) {
            SharedPreferences settings = getSharedPreferences(getPackageName(), 0);
            if (settings.getBoolean(Constants.Values.App.Key_DebugModeEnabled, false)) {
                settings.edit().remove(Constants.Values.App.Key_DebugModeEnabled).apply();
                this.mConfig.debugModeEnabled = false;
                inflateDebugOverlay();
                if (this.mDebugOverlay != null) {
                    this.mDebugOverlay.disable();
                }
            } else {
                settings.edit().putBoolean(Constants.Values.App.Key_DebugModeEnabled, true).apply();
                this.mConfig.debugModeEnabled = true;
                inflateDebugOverlay();
                if (this.mDebugOverlay != null) {
                    this.mDebugOverlay.enable();
                }
            }
            Toast.makeText(this, "Debug mode (" + Constants.Values.App.DebugModeVersion + ") " + (this.mConfig.debugModeEnabled ? "Enabled" : "Disabled") + ", please restart Recents now", 0).show();
        }
    }

    @Override
    public void onExitToHomeAnimationTriggered() {
        this.mScrimViews.startExitRecentsAnimation();
    }

    @Override
    public void onTaskViewClicked() {
    }

    @Override
    public void onTaskLaunchFailed() {
        dismissRecentsToHomeRaw(true);
    }

    @Override
    public void onAllTaskViewsDismissed() {
        this.mFinishLaunchHomeRunnable.run();
    }

    @Override
    public void onScreenPinningRequest() {
        if (this.mStatusBar != null) {
            this.mStatusBar.showScreenPinningRequest(false);
        }
    }

    @Override
    public void refreshSearchWidget() {
        bindSearchBarAppWidget();
        addSearchBarAppWidgetView();
    }

    @Override
    public void onPrimarySeekBarChanged(float progress) {
    }

    @Override
    public void onSecondarySeekBarChanged(float progress) {
    }
}
