package com.android.systemui.recents;

import android.app.ActivityManager;
import android.app.trust.TrustManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.EventLog;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Dependency;
import com.android.systemui.OverviewProxyService;
import com.android.systemui.R;
import com.android.systemui.RecentsComponent;
import com.android.systemui.SystemUI;
import com.android.systemui.recents.IRecentsSystemUserCallbacks;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.ConfigurationChangedEvent;
import com.android.systemui.recents.events.activity.DockedFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.DockedTopTaskEvent;
import com.android.systemui.recents.events.activity.LaunchTaskFailedEvent;
import com.android.systemui.recents.events.activity.RecentsActivityStartingEvent;
import com.android.systemui.recents.events.component.RecentsVisibilityChangedEvent;
import com.android.systemui.recents.events.component.ScreenPinningRequestEvent;
import com.android.systemui.recents.events.component.SetWaitingForTransitionStartEvent;
import com.android.systemui.recents.events.component.ShowUserToastEvent;
import com.android.systemui.recents.events.ui.RecentsDrawnEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.shared.recents.IOverviewProxy;
import com.android.systemui.shared.recents.model.RecentsTaskLoader;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.phone.StatusBar;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Recents extends SystemUI implements RecentsComponent, CommandQueue.Callbacks {
    public static final Set<String> RECENTS_ACTIVITIES = new HashSet();
    private static RecentsConfiguration sConfiguration;
    private static RecentsDebugFlags sDebugFlags;
    private static SystemServicesProxy sSystemServicesProxy;
    private static RecentsTaskLoader sTaskLoader;
    private int mDraggingInRecentsCurrentUser;
    private Handler mHandler;
    private RecentsImpl mImpl;
    private OverviewProxyService mOverviewProxyService;
    private RecentsSystemUser mSystemToUserCallbacks;
    private TrustManager mTrustManager;
    private IRecentsSystemUserCallbacks mUserToSystemCallbacks;
    private final ArrayList<Runnable> mOnConnectRunnables = new ArrayList<>();
    private final IBinder.DeathRecipient mUserToSystemCallbacksDeathRcpt = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            Recents.this.mUserToSystemCallbacks = null;
            EventLog.writeEvent(36060, 3, Integer.valueOf(Recents.sSystemServicesProxy.getProcessUser()));
            Recents.this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Recents.this.registerWithSystemUser();
                }
            }, 5000L);
        }
    };
    private final ServiceConnection mUserToSystemServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (iBinder != null) {
                Recents.this.mUserToSystemCallbacks = IRecentsSystemUserCallbacks.Stub.asInterface(iBinder);
                EventLog.writeEvent(36060, 2, Integer.valueOf(Recents.sSystemServicesProxy.getProcessUser()));
                try {
                    iBinder.linkToDeath(Recents.this.mUserToSystemCallbacksDeathRcpt, 0);
                } catch (RemoteException e) {
                    Log.e("Recents", "Lost connection to (System) SystemUI", e);
                }
                Recents.this.runAndFlushOnConnectRunnables();
            }
            Recents.this.mContext.unbindService(this);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    static {
        RECENTS_ACTIVITIES.add("com.android.systemui.recents.RecentsActivity");
    }

    public IBinder getSystemUserCallbacks() {
        return this.mSystemToUserCallbacks;
    }

    public static RecentsTaskLoader getTaskLoader() {
        return sTaskLoader;
    }

    public static SystemServicesProxy getSystemServices() {
        return sSystemServicesProxy;
    }

    public static RecentsConfiguration getConfiguration() {
        return sConfiguration;
    }

    public static RecentsDebugFlags getDebugFlags() {
        return sDebugFlags;
    }

    @Override
    public void start() {
        Resources resources = this.mContext.getResources();
        int color = this.mContext.getColor(R.color.recents_task_bar_default_background_color);
        int color2 = this.mContext.getColor(R.color.recents_task_view_default_background_color);
        sDebugFlags = new RecentsDebugFlags();
        sSystemServicesProxy = SystemServicesProxy.getInstance(this.mContext);
        sConfiguration = new RecentsConfiguration(this.mContext);
        sTaskLoader = new RecentsTaskLoader(this.mContext, resources.getInteger(R.integer.config_recents_max_thumbnail_count), resources.getInteger(R.integer.config_recents_max_icon_count), resources.getInteger(R.integer.recents_svelte_level));
        sTaskLoader.setDefaultColors(color, color2);
        this.mHandler = new Handler();
        this.mImpl = new RecentsImpl(this.mContext);
        this.mOverviewProxyService = (OverviewProxyService) Dependency.get(OverviewProxyService.class);
        EventBus.getDefault().register(this, 1);
        EventBus.getDefault().register(sSystemServicesProxy, 1);
        EventBus.getDefault().register(sTaskLoader, 1);
        if (sSystemServicesProxy.isSystemUser(sSystemServicesProxy.getProcessUser())) {
            ((CommandQueue) getComponent(CommandQueue.class)).addCallbacks(this);
            this.mSystemToUserCallbacks = new RecentsSystemUser(this.mContext, this.mImpl);
        } else {
            registerWithSystemUser();
        }
        putComponent(Recents.class, this);
        this.mTrustManager = (TrustManager) this.mContext.getSystemService("trust");
    }

    @Override
    public void onBootCompleted() {
        this.mImpl.onBootCompleted();
    }

    @Override
    public void showRecentApps(boolean z) {
        if (!isUserSetup()) {
            return;
        }
        IOverviewProxy proxy = this.mOverviewProxyService.getProxy();
        if (proxy != null) {
            try {
                proxy.onOverviewShown(z);
                return;
            } catch (RemoteException e) {
                Log.e("Recents", "Failed to send overview show event to launcher.", e);
            }
        }
        ActivityManagerWrapper.getInstance().closeSystemWindows("recentapps");
        int iGrowsRecents = ((Divider) getComponent(Divider.class)).getView().growsRecents();
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.showRecents(z, false, true, iGrowsRecents);
            return;
        }
        if (this.mSystemToUserCallbacks != null) {
            IRecentsNonSystemUserCallbacks nonSystemUserRecentsForUser = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
            if (nonSystemUserRecentsForUser != null) {
                try {
                    nonSystemUserRecentsForUser.showRecents(z, false, true, iGrowsRecents);
                    return;
                } catch (RemoteException e2) {
                    Log.e("Recents", "Callback failed", e2);
                    return;
                }
            }
            Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
        }
    }

    @Override
    public void hideRecentApps(boolean z, boolean z2) {
        if (!isUserSetup()) {
            return;
        }
        IOverviewProxy proxy = this.mOverviewProxyService.getProxy();
        if (proxy != null) {
            try {
                proxy.onOverviewHidden(z, z2);
                return;
            } catch (RemoteException e) {
                Log.e("Recents", "Failed to send overview hide event to launcher.", e);
            }
        }
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.hideRecents(z, z2);
            return;
        }
        if (this.mSystemToUserCallbacks != null) {
            IRecentsNonSystemUserCallbacks nonSystemUserRecentsForUser = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
            if (nonSystemUserRecentsForUser != null) {
                try {
                    nonSystemUserRecentsForUser.hideRecents(z, z2);
                    return;
                } catch (RemoteException e2) {
                    Log.e("Recents", "Callback failed", e2);
                    return;
                }
            }
            Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
        }
    }

    @Override
    public void toggleRecentApps() {
        if (!isUserSetup()) {
            return;
        }
        if (this.mOverviewProxyService.getProxy() != null) {
            final Runnable runnable = new Runnable() {
                @Override
                public final void run() {
                    Recents.lambda$toggleRecentApps$0(this.f$0);
                }
            };
            StatusBar statusBar = (StatusBar) getComponent(StatusBar.class);
            if (statusBar != null && statusBar.isKeyguardShowing()) {
                statusBar.executeRunnableDismissingKeyguard(new Runnable() {
                    @Override
                    public final void run() {
                        Recents.lambda$toggleRecentApps$1(this.f$0, runnable);
                    }
                }, null, true, false, true);
                return;
            } else {
                runnable.run();
                return;
            }
        }
        int iGrowsRecents = ((Divider) getComponent(Divider.class)).getView().growsRecents();
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.toggleRecents(iGrowsRecents);
            return;
        }
        if (this.mSystemToUserCallbacks != null) {
            IRecentsNonSystemUserCallbacks nonSystemUserRecentsForUser = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
            if (nonSystemUserRecentsForUser != null) {
                try {
                    nonSystemUserRecentsForUser.toggleRecents(iGrowsRecents);
                    return;
                } catch (RemoteException e) {
                    Log.e("Recents", "Callback failed", e);
                    return;
                }
            }
            Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
        }
    }

    public static void lambda$toggleRecentApps$0(Recents recents) {
        try {
            if (recents.mOverviewProxyService.getProxy() != null) {
                recents.mOverviewProxyService.getProxy().onOverviewToggle();
            }
        } catch (RemoteException e) {
            Log.e("Recents", "Cannot send toggle recents through proxy service.", e);
        }
    }

    public static void lambda$toggleRecentApps$1(Recents recents, Runnable runnable) {
        recents.mTrustManager.reportKeyguardShowingChanged();
        recents.mHandler.post(runnable);
    }

    @Override
    public void preloadRecentApps() {
        if (!isUserSetup() || this.mOverviewProxyService.getProxy() != null) {
            return;
        }
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.preloadRecents();
            return;
        }
        if (this.mSystemToUserCallbacks != null) {
            IRecentsNonSystemUserCallbacks nonSystemUserRecentsForUser = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
            if (nonSystemUserRecentsForUser != null) {
                try {
                    nonSystemUserRecentsForUser.preloadRecents();
                    return;
                } catch (RemoteException e) {
                    Log.e("Recents", "Callback failed", e);
                    return;
                }
            }
            Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
        }
    }

    @Override
    public void cancelPreloadRecentApps() {
        if (!isUserSetup() || this.mOverviewProxyService.getProxy() != null) {
            return;
        }
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.cancelPreloadingRecents();
            return;
        }
        if (this.mSystemToUserCallbacks != null) {
            IRecentsNonSystemUserCallbacks nonSystemUserRecentsForUser = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
            if (nonSystemUserRecentsForUser != null) {
                try {
                    nonSystemUserRecentsForUser.cancelPreloadingRecents();
                    return;
                } catch (RemoteException e) {
                    Log.e("Recents", "Callback failed", e);
                    return;
                }
            }
            Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
        }
    }

    @Override
    public boolean splitPrimaryTask(int i, int i2, Rect rect, int i3) {
        int activityType;
        if (!isUserSetup()) {
            return false;
        }
        Point point = new Point();
        if (rect == null) {
            ((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).getDisplay(0).getRealSize(point);
            rect = new Rect(0, 0, point.x, point.y);
        }
        int currentUser = sSystemServicesProxy.getCurrentUser();
        ActivityManager.RunningTaskInfo runningTask = ActivityManagerWrapper.getInstance().getRunningTask();
        if (runningTask != null) {
            activityType = runningTask.configuration.windowConfiguration.getActivityType();
        } else {
            activityType = 0;
        }
        boolean zIsScreenPinningActive = ActivityManagerWrapper.getInstance().isScreenPinningActive();
        boolean z = activityType == 2 || activityType == 3;
        if (runningTask == null || z || zIsScreenPinningActive) {
            return false;
        }
        logDockAttempt(this.mContext, runningTask.topActivity, runningTask.resizeMode);
        if (runningTask.supportsSplitScreenMultiWindow) {
            if (i3 != -1) {
                MetricsLogger.action(this.mContext, i3, runningTask.topActivity.flattenToShortString());
            }
            if (sSystemServicesProxy.isSystemUser(currentUser)) {
                this.mImpl.splitPrimaryTask(runningTask.id, i, i2, rect);
            } else if (this.mSystemToUserCallbacks != null) {
                IRecentsNonSystemUserCallbacks nonSystemUserRecentsForUser = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
                if (nonSystemUserRecentsForUser != null) {
                    try {
                        nonSystemUserRecentsForUser.splitPrimaryTask(runningTask.id, i, i2, rect);
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                } else {
                    Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
                }
            }
            this.mDraggingInRecentsCurrentUser = currentUser;
            if (this.mOverviewProxyService.getProxy() != null) {
                EventBus.getDefault().post(new RecentsDrawnEvent());
            }
            return true;
        }
        EventBus.getDefault().send(new ShowUserToastEvent(R.string.dock_non_resizeble_failed_to_dock_text, 0));
        return false;
    }

    public static void logDockAttempt(Context context, ComponentName componentName, int i) {
        if (i == 0) {
            MetricsLogger.action(context, 391, componentName.flattenToShortString());
        }
        MetricsLogger.count(context, getMetricsCounterForResizeMode(i), 1);
    }

    private static String getMetricsCounterForResizeMode(int i) {
        if (i == 4) {
            return "window_enter_unsupported";
        }
        switch (i) {
            case 1:
            case 2:
                return "window_enter_supported";
            default:
                return "window_enter_incompatible";
        }
    }

    @Override
    public void onDraggingInRecents(float f) {
        if (sSystemServicesProxy.isSystemUser(this.mDraggingInRecentsCurrentUser)) {
            this.mImpl.onDraggingInRecents(f);
            return;
        }
        if (this.mSystemToUserCallbacks != null) {
            IRecentsNonSystemUserCallbacks nonSystemUserRecentsForUser = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(this.mDraggingInRecentsCurrentUser);
            if (nonSystemUserRecentsForUser != null) {
                try {
                    nonSystemUserRecentsForUser.onDraggingInRecents(f);
                    return;
                } catch (RemoteException e) {
                    Log.e("Recents", "Callback failed", e);
                    return;
                }
            }
            Log.e("Recents", "No SystemUI callbacks found for user: " + this.mDraggingInRecentsCurrentUser);
        }
    }

    @Override
    public void onDraggingInRecentsEnded(float f) {
        if (sSystemServicesProxy.isSystemUser(this.mDraggingInRecentsCurrentUser)) {
            this.mImpl.onDraggingInRecentsEnded(f);
            return;
        }
        if (this.mSystemToUserCallbacks != null) {
            IRecentsNonSystemUserCallbacks nonSystemUserRecentsForUser = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(this.mDraggingInRecentsCurrentUser);
            if (nonSystemUserRecentsForUser != null) {
                try {
                    nonSystemUserRecentsForUser.onDraggingInRecentsEnded(f);
                    return;
                } catch (RemoteException e) {
                    Log.e("Recents", "Callback failed", e);
                    return;
                }
            }
            Log.e("Recents", "No SystemUI callbacks found for user: " + this.mDraggingInRecentsCurrentUser);
        }
    }

    @Override
    public void appTransitionFinished() {
        if (!getConfiguration().isLowRamDevice) {
            EventBus.getDefault().send(new SetWaitingForTransitionStartEvent(false));
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.onConfigurationChanged();
            return;
        }
        if (this.mSystemToUserCallbacks != null) {
            IRecentsNonSystemUserCallbacks nonSystemUserRecentsForUser = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
            if (nonSystemUserRecentsForUser != null) {
                try {
                    nonSystemUserRecentsForUser.onConfigurationChanged();
                    return;
                } catch (RemoteException e) {
                    Log.e("Recents", "Callback failed", e);
                    return;
                }
            }
            Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
        }
    }

    public final void onBusEvent(final RecentsVisibilityChangedEvent recentsVisibilityChangedEvent) {
        SystemServicesProxy systemServices = getSystemServices();
        if (systemServices.isSystemUser(systemServices.getProcessUser())) {
            this.mImpl.onVisibilityChanged(recentsVisibilityChangedEvent.applicationContext, recentsVisibilityChangedEvent.visible);
        } else {
            postToSystemUser(new Runnable() {
                @Override
                public void run() {
                    try {
                        Recents.this.mUserToSystemCallbacks.updateRecentsVisibility(recentsVisibilityChangedEvent.visible);
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                }
            });
        }
        if (!recentsVisibilityChangedEvent.visible) {
            this.mImpl.setWaitingForTransitionStart(false);
        }
    }

    public final void onBusEvent(DockedFirstAnimationFrameEvent dockedFirstAnimationFrameEvent) {
        SystemServicesProxy systemServices = getSystemServices();
        if (!systemServices.isSystemUser(systemServices.getProcessUser())) {
            postToSystemUser(new Runnable() {
                @Override
                public void run() {
                    try {
                        Recents.this.mUserToSystemCallbacks.sendDockedFirstAnimationFrameEvent();
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                }
            });
        }
    }

    public final void onBusEvent(final ScreenPinningRequestEvent screenPinningRequestEvent) {
        if (sSystemServicesProxy.isSystemUser(sSystemServicesProxy.getProcessUser())) {
            this.mImpl.onStartScreenPinning(screenPinningRequestEvent.applicationContext, screenPinningRequestEvent.taskId);
        } else {
            postToSystemUser(new Runnable() {
                @Override
                public void run() {
                    try {
                        Recents.this.mUserToSystemCallbacks.startScreenPinning(screenPinningRequestEvent.taskId);
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                }
            });
        }
    }

    public final void onBusEvent(RecentsDrawnEvent recentsDrawnEvent) {
        if (!sSystemServicesProxy.isSystemUser(sSystemServicesProxy.getProcessUser())) {
            postToSystemUser(new Runnable() {
                @Override
                public void run() {
                    try {
                        Recents.this.mUserToSystemCallbacks.sendRecentsDrawnEvent();
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                }
            });
        }
    }

    public final void onBusEvent(final DockedTopTaskEvent dockedTopTaskEvent) {
        if (!sSystemServicesProxy.isSystemUser(sSystemServicesProxy.getProcessUser())) {
            postToSystemUser(new Runnable() {
                @Override
                public void run() {
                    try {
                        Recents.this.mUserToSystemCallbacks.sendDockingTopTaskEvent(dockedTopTaskEvent.dragMode, dockedTopTaskEvent.initialRect);
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                }
            });
        }
    }

    public final void onBusEvent(RecentsActivityStartingEvent recentsActivityStartingEvent) {
        if (!sSystemServicesProxy.isSystemUser(sSystemServicesProxy.getProcessUser())) {
            postToSystemUser(new Runnable() {
                @Override
                public void run() {
                    try {
                        Recents.this.mUserToSystemCallbacks.sendLaunchRecentsEvent();
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                }
            });
        }
    }

    public final void onBusEvent(LaunchTaskFailedEvent launchTaskFailedEvent) {
        this.mImpl.setWaitingForTransitionStart(false);
    }

    public final void onBusEvent(ConfigurationChangedEvent configurationChangedEvent) {
        this.mImpl.onConfigurationChanged();
    }

    public final void onBusEvent(ShowUserToastEvent showUserToastEvent) {
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.onShowCurrentUserToast(showUserToastEvent.msgResId, showUserToastEvent.msgLength);
            return;
        }
        if (this.mSystemToUserCallbacks != null) {
            IRecentsNonSystemUserCallbacks nonSystemUserRecentsForUser = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
            if (nonSystemUserRecentsForUser != null) {
                try {
                    nonSystemUserRecentsForUser.showCurrentUserToast(showUserToastEvent.msgResId, showUserToastEvent.msgLength);
                    return;
                } catch (RemoteException e) {
                    Log.e("Recents", "Callback failed", e);
                    return;
                }
            }
            Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
        }
    }

    public final void onBusEvent(final SetWaitingForTransitionStartEvent setWaitingForTransitionStartEvent) {
        if (sSystemServicesProxy.isSystemUser(sSystemServicesProxy.getProcessUser())) {
            this.mImpl.setWaitingForTransitionStart(setWaitingForTransitionStartEvent.waitingForTransitionStart);
        } else {
            postToSystemUser(new Runnable() {
                @Override
                public void run() {
                    try {
                        Recents.this.mUserToSystemCallbacks.setWaitingForTransitionStartEvent(setWaitingForTransitionStartEvent.waitingForTransitionStart);
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                }
            });
        }
    }

    public void registerWithSystemUser() {
        final int processUser = sSystemServicesProxy.getProcessUser();
        postToSystemUser(new Runnable() {
            @Override
            public void run() {
                try {
                    Recents.this.mUserToSystemCallbacks.registerNonSystemUserCallbacks(new RecentsImplProxy(Recents.this.mImpl), processUser);
                } catch (RemoteException e) {
                    Log.e("Recents", "Failed to register", e);
                }
            }
        });
    }

    private void postToSystemUser(Runnable runnable) {
        this.mOnConnectRunnables.add(runnable);
        if (this.mUserToSystemCallbacks == null) {
            Intent intent = new Intent();
            intent.setClass(this.mContext, RecentsSystemUserService.class);
            boolean zBindServiceAsUser = this.mContext.bindServiceAsUser(intent, this.mUserToSystemServiceConnection, 1, UserHandle.SYSTEM);
            EventLog.writeEvent(36060, 1, Integer.valueOf(sSystemServicesProxy.getProcessUser()));
            if (!zBindServiceAsUser) {
                this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Recents.this.registerWithSystemUser();
                    }
                }, 5000L);
                return;
            }
            return;
        }
        runAndFlushOnConnectRunnables();
    }

    public void runAndFlushOnConnectRunnables() {
        Iterator<Runnable> it = this.mOnConnectRunnables.iterator();
        while (it.hasNext()) {
            it.next().run();
        }
        this.mOnConnectRunnables.clear();
    }

    private boolean isUserSetup() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        return (Settings.Global.getInt(contentResolver, "device_provisioned", 0) == 0 || Settings.Secure.getInt(contentResolver, "user_setup_complete", 0) == 0) ? false : true;
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("Recents");
        printWriter.println("  currentUserId=" + SystemServicesProxy.getInstance(this.mContext).getCurrentUser());
    }
}
