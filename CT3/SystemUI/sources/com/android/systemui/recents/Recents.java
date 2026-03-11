package com.android.systemui.recents;

import android.app.ActivityManager;
import android.app.UiModeManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.EventLog;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.RecentsComponent;
import com.android.systemui.SystemUI;
import com.android.systemui.recents.IRecentsSystemUserCallbacks;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.ConfigurationChangedEvent;
import com.android.systemui.recents.events.activity.DockedTopTaskEvent;
import com.android.systemui.recents.events.activity.RecentsActivityStartingEvent;
import com.android.systemui.recents.events.component.RecentsVisibilityChangedEvent;
import com.android.systemui.recents.events.component.ScreenPinningRequestEvent;
import com.android.systemui.recents.events.ui.RecentsDrawnEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.tv.RecentsTvImpl;
import com.android.systemui.stackdivider.Divider;
import java.util.ArrayList;

public class Recents extends SystemUI implements RecentsComponent {
    private static RecentsConfiguration sConfiguration;
    private static RecentsDebugFlags sDebugFlags;
    private static SystemServicesProxy sSystemServicesProxy;
    private static RecentsTaskLoader sTaskLoader;
    private int mDraggingInRecentsCurrentUser;
    private Handler mHandler;
    private RecentsImpl mImpl;
    private String mOverrideRecentsPackageName;
    private RecentsSystemUser mSystemToUserCallbacks;
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
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service != null) {
                Recents.this.mUserToSystemCallbacks = IRecentsSystemUserCallbacks.Stub.asInterface(service);
                EventLog.writeEvent(36060, 2, Integer.valueOf(Recents.sSystemServicesProxy.getProcessUser()));
                try {
                    service.linkToDeath(Recents.this.mUserToSystemCallbacksDeathRcpt, 0);
                } catch (RemoteException e) {
                    Log.e("Recents", "Lost connection to (System) SystemUI", e);
                }
                Recents.this.runAndFlushOnConnectRunnables();
            }
            Recents.this.mContext.unbindService(this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

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
        sDebugFlags = new RecentsDebugFlags(this.mContext);
        sSystemServicesProxy = SystemServicesProxy.getInstance(this.mContext);
        sTaskLoader = new RecentsTaskLoader(this.mContext);
        sConfiguration = new RecentsConfiguration(this.mContext);
        this.mHandler = new Handler();
        UiModeManager uiModeManager = (UiModeManager) this.mContext.getSystemService("uimode");
        if (uiModeManager.getCurrentModeType() == 4) {
            this.mImpl = new RecentsTvImpl(this.mContext);
        } else {
            this.mImpl = new RecentsImpl(this.mContext);
        }
        if ("userdebug".equals(Build.TYPE) || "eng".equals(Build.TYPE)) {
            String cnStr = SystemProperties.get("persist.recents_override_pkg");
            if (!cnStr.isEmpty()) {
                this.mOverrideRecentsPackageName = cnStr;
            }
        }
        EventBus.getDefault().register(this, 1);
        EventBus.getDefault().register(sSystemServicesProxy, 1);
        EventBus.getDefault().register(sTaskLoader, 1);
        int processUser = sSystemServicesProxy.getProcessUser();
        if (sSystemServicesProxy.isSystemUser(processUser)) {
            this.mSystemToUserCallbacks = new RecentsSystemUser(this.mContext, this.mImpl);
        } else {
            registerWithSystemUser();
        }
        putComponent(Recents.class, this);
    }

    @Override
    public void onBootCompleted() {
        this.mImpl.onBootCompleted();
    }

    @Override
    public void showRecents(boolean triggeredFromAltTab, boolean fromHome) {
        if (!isUserSetup() || proxyToOverridePackage("com.android.systemui.recents.ACTION_SHOW")) {
            return;
        }
        int recentsGrowTarget = ((Divider) getComponent(Divider.class)).getView().growsRecents();
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.showRecents(triggeredFromAltTab, false, true, false, fromHome, recentsGrowTarget);
            return;
        }
        if (this.mSystemToUserCallbacks == null) {
            return;
        }
        IRecentsNonSystemUserCallbacks callbacks = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
        if (callbacks != null) {
            try {
                callbacks.showRecents(triggeredFromAltTab, false, true, false, fromHome, recentsGrowTarget);
                return;
            } catch (RemoteException e) {
                Log.e("Recents", "Callback failed", e);
                return;
            }
        }
        Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
    }

    @Override
    public void hideRecents(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        if (!isUserSetup() || proxyToOverridePackage("com.android.systemui.recents.ACTION_HIDE")) {
            return;
        }
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.hideRecents(triggeredFromAltTab, triggeredFromHomeKey);
            return;
        }
        if (this.mSystemToUserCallbacks == null) {
            return;
        }
        IRecentsNonSystemUserCallbacks callbacks = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
        if (callbacks != null) {
            try {
                callbacks.hideRecents(triggeredFromAltTab, triggeredFromHomeKey);
                return;
            } catch (RemoteException e) {
                Log.e("Recents", "Callback failed", e);
                return;
            }
        }
        Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
    }

    @Override
    public void toggleRecents(Display display) {
        if (!isUserSetup() || proxyToOverridePackage("com.android.systemui.recents.ACTION_TOGGLE")) {
            return;
        }
        int growTarget = ((Divider) getComponent(Divider.class)).getView().growsRecents();
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.toggleRecents(growTarget);
            return;
        }
        if (this.mSystemToUserCallbacks == null) {
            return;
        }
        IRecentsNonSystemUserCallbacks callbacks = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
        if (callbacks != null) {
            try {
                callbacks.toggleRecents(growTarget);
                return;
            } catch (RemoteException e) {
                Log.e("Recents", "Callback failed", e);
                return;
            }
        }
        Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
    }

    @Override
    public void preloadRecents() {
        if (!isUserSetup()) {
            return;
        }
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.preloadRecents();
            return;
        }
        if (this.mSystemToUserCallbacks == null) {
            return;
        }
        IRecentsNonSystemUserCallbacks callbacks = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
        if (callbacks != null) {
            try {
                callbacks.preloadRecents();
                return;
            } catch (RemoteException e) {
                Log.e("Recents", "Callback failed", e);
                return;
            }
        }
        Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
    }

    @Override
    public void cancelPreloadingRecents() {
        if (!isUserSetup()) {
            return;
        }
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.cancelPreloadingRecents();
            return;
        }
        if (this.mSystemToUserCallbacks == null) {
            return;
        }
        IRecentsNonSystemUserCallbacks callbacks = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
        if (callbacks != null) {
            try {
                callbacks.cancelPreloadingRecents();
                return;
            } catch (RemoteException e) {
                Log.e("Recents", "Callback failed", e);
                return;
            }
        }
        Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
    }

    @Override
    public boolean dockTopTask(int dragMode, int stackCreateMode, Rect initialBounds, int metricsDockAction) {
        boolean zIsHomeStack;
        if (!isUserSetup()) {
            return false;
        }
        Point realSize = new Point();
        if (initialBounds == null) {
            ((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).getDisplay(0).getRealSize(realSize);
            initialBounds = new Rect(0, 0, realSize.x, realSize.y);
        }
        int currentUser = sSystemServicesProxy.getCurrentUser();
        SystemServicesProxy ssp = getSystemServices();
        ActivityManager.RunningTaskInfo runningTask = ssp.getRunningTask();
        boolean screenPinningActive = ssp.isScreenPinningActive();
        if (runningTask == null) {
            zIsHomeStack = false;
        } else {
            zIsHomeStack = SystemServicesProxy.isHomeStack(runningTask.stackId);
        }
        if (runningTask != null && !zIsHomeStack && !screenPinningActive) {
            logDockAttempt(this.mContext, runningTask.topActivity, runningTask.resizeMode);
            if (runningTask.isDockable) {
                if (metricsDockAction != -1) {
                    MetricsLogger.action(this.mContext, metricsDockAction, runningTask.topActivity.flattenToShortString());
                }
                if (sSystemServicesProxy.isSystemUser(currentUser)) {
                    this.mImpl.dockTopTask(runningTask.id, dragMode, stackCreateMode, initialBounds);
                } else if (this.mSystemToUserCallbacks != null) {
                    IRecentsNonSystemUserCallbacks callbacks = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
                    if (callbacks != null) {
                        try {
                            callbacks.dockTopTask(runningTask.id, dragMode, stackCreateMode, initialBounds);
                        } catch (RemoteException e) {
                            Log.e("Recents", "Callback failed", e);
                        }
                    } else {
                        Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
                    }
                }
                this.mDraggingInRecentsCurrentUser = currentUser;
                return true;
            }
            Toast.makeText(this.mContext, R.string.recents_incompatible_app_message, 0).show();
            return false;
        }
        return false;
    }

    public static void logDockAttempt(Context ctx, ComponentName activity, int resizeMode) {
        if (resizeMode == 0) {
            MetricsLogger.action(ctx, 391, activity.flattenToShortString());
        }
        MetricsLogger.count(ctx, getMetricsCounterForResizeMode(resizeMode), 1);
    }

    private static String getMetricsCounterForResizeMode(int resizeMode) {
        switch (resizeMode) {
            case 2:
            case 3:
                return "window_enter_supported";
            case 4:
                return "window_enter_unsupported";
            default:
                return "window_enter_incompatible";
        }
    }

    @Override
    public void onDraggingInRecents(float distanceFromTop) {
        if (sSystemServicesProxy.isSystemUser(this.mDraggingInRecentsCurrentUser)) {
            this.mImpl.onDraggingInRecents(distanceFromTop);
            return;
        }
        if (this.mSystemToUserCallbacks == null) {
            return;
        }
        IRecentsNonSystemUserCallbacks callbacks = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(this.mDraggingInRecentsCurrentUser);
        if (callbacks != null) {
            try {
                callbacks.onDraggingInRecents(distanceFromTop);
                return;
            } catch (RemoteException e) {
                Log.e("Recents", "Callback failed", e);
                return;
            }
        }
        Log.e("Recents", "No SystemUI callbacks found for user: " + this.mDraggingInRecentsCurrentUser);
    }

    @Override
    public void onDraggingInRecentsEnded(float velocity) {
        if (sSystemServicesProxy.isSystemUser(this.mDraggingInRecentsCurrentUser)) {
            this.mImpl.onDraggingInRecentsEnded(velocity);
            return;
        }
        if (this.mSystemToUserCallbacks == null) {
            return;
        }
        IRecentsNonSystemUserCallbacks callbacks = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(this.mDraggingInRecentsCurrentUser);
        if (callbacks != null) {
            try {
                callbacks.onDraggingInRecentsEnded(velocity);
                return;
            } catch (RemoteException e) {
                Log.e("Recents", "Callback failed", e);
                return;
            }
        }
        Log.e("Recents", "No SystemUI callbacks found for user: " + this.mDraggingInRecentsCurrentUser);
    }

    @Override
    public void showNextAffiliatedTask() {
        if (!isUserSetup()) {
            return;
        }
        this.mImpl.showNextAffiliatedTask();
    }

    @Override
    public void showPrevAffiliatedTask() {
        if (!isUserSetup()) {
            return;
        }
        this.mImpl.showPrevAffiliatedTask();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        int currentUser = sSystemServicesProxy.getCurrentUser();
        if (sSystemServicesProxy.isSystemUser(currentUser)) {
            this.mImpl.onConfigurationChanged();
            return;
        }
        if (this.mSystemToUserCallbacks == null) {
            return;
        }
        IRecentsNonSystemUserCallbacks callbacks = this.mSystemToUserCallbacks.getNonSystemUserRecentsForUser(currentUser);
        if (callbacks != null) {
            try {
                callbacks.onConfigurationChanged();
                return;
            } catch (RemoteException e) {
                Log.e("Recents", "Callback failed", e);
                return;
            }
        }
        Log.e("Recents", "No SystemUI callbacks found for user: " + currentUser);
    }

    public final void onBusEvent(final RecentsVisibilityChangedEvent event) {
        SystemServicesProxy ssp = getSystemServices();
        int processUser = ssp.getProcessUser();
        if (ssp.isSystemUser(processUser)) {
            this.mImpl.onVisibilityChanged(event.applicationContext, event.visible);
        } else {
            postToSystemUser(new Runnable() {
                @Override
                public void run() {
                    try {
                        Recents.this.mUserToSystemCallbacks.updateRecentsVisibility(event.visible);
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                }
            });
        }
    }

    public final void onBusEvent(final ScreenPinningRequestEvent event) {
        int processUser = sSystemServicesProxy.getProcessUser();
        if (sSystemServicesProxy.isSystemUser(processUser)) {
            this.mImpl.onStartScreenPinning(event.applicationContext, event.taskId);
        } else {
            postToSystemUser(new Runnable() {
                @Override
                public void run() {
                    try {
                        Recents.this.mUserToSystemCallbacks.startScreenPinning(event.taskId);
                    } catch (RemoteException e) {
                        Log.e("Recents", "Callback failed", e);
                    }
                }
            });
        }
    }

    public final void onBusEvent(RecentsDrawnEvent event) {
        int processUser = sSystemServicesProxy.getProcessUser();
        if (sSystemServicesProxy.isSystemUser(processUser)) {
            return;
        }
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

    public final void onBusEvent(final DockedTopTaskEvent event) {
        int processUser = sSystemServicesProxy.getProcessUser();
        if (sSystemServicesProxy.isSystemUser(processUser)) {
            return;
        }
        postToSystemUser(new Runnable() {
            @Override
            public void run() {
                try {
                    Recents.this.mUserToSystemCallbacks.sendDockingTopTaskEvent(event.dragMode, event.initialRect);
                } catch (RemoteException e) {
                    Log.e("Recents", "Callback failed", e);
                }
            }
        });
    }

    public final void onBusEvent(RecentsActivityStartingEvent event) {
        int processUser = sSystemServicesProxy.getProcessUser();
        if (sSystemServicesProxy.isSystemUser(processUser)) {
            return;
        }
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

    public final void onBusEvent(ConfigurationChangedEvent event) {
        this.mImpl.onConfigurationChanged();
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

    private void postToSystemUser(Runnable onConnectRunnable) {
        this.mOnConnectRunnables.add(onConnectRunnable);
        if (this.mUserToSystemCallbacks == null) {
            Intent systemUserServiceIntent = new Intent();
            systemUserServiceIntent.setClass(this.mContext, RecentsSystemUserService.class);
            boolean bound = this.mContext.bindServiceAsUser(systemUserServiceIntent, this.mUserToSystemServiceConnection, 1, UserHandle.SYSTEM);
            EventLog.writeEvent(36060, 1, Integer.valueOf(sSystemServicesProxy.getProcessUser()));
            if (bound) {
                return;
            }
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Recents.this.registerWithSystemUser();
                }
            }, 5000L);
            return;
        }
        runAndFlushOnConnectRunnables();
    }

    public void runAndFlushOnConnectRunnables() {
        for (Runnable r : this.mOnConnectRunnables) {
            r.run();
        }
        this.mOnConnectRunnables.clear();
    }

    private boolean isUserSetup() {
        ContentResolver cr = this.mContext.getContentResolver();
        return (Settings.Global.getInt(cr, "device_provisioned", 0) == 0 || Settings.Secure.getInt(cr, "user_setup_complete", 0) == 0) ? false : true;
    }

    private boolean proxyToOverridePackage(String action) {
        if (this.mOverrideRecentsPackageName != null) {
            Intent intent = new Intent(action);
            intent.setPackage(this.mOverrideRecentsPackageName);
            intent.addFlags(268435456);
            this.mContext.sendBroadcast(intent);
            return true;
        }
        return false;
    }
}
