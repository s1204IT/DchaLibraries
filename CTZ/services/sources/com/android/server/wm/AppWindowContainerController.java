package com.android.server.wm;

import android.app.ActivityManager;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Slog;
import android.view.IApplicationToken;
import android.view.RemoteAnimationDefinition;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.AttributeCache;
import com.android.server.pm.DumpState;
import com.android.server.policy.WindowManagerPolicy;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.wm.WindowManagerDebugger;
import com.mediatek.server.wm.WmsExt;

public class AppWindowContainerController extends WindowContainerController<AppWindowToken, AppWindowContainerListener> {
    private static final int STARTING_WINDOW_TYPE_NONE = 0;
    private static final int STARTING_WINDOW_TYPE_SNAPSHOT = 1;
    private static final int STARTING_WINDOW_TYPE_SPLASH_SCREEN = 2;
    private final Runnable mAddStartingWindow;
    private final Handler mHandler;
    private final Runnable mOnWindowsGone;
    private final Runnable mOnWindowsVisible;
    private IApplicationToken mToken;
    private WindowManagerDebugger mWindowManagerDebugger;

    @Override
    public void onOverrideConfigurationChanged(Configuration configuration) {
        super.onOverrideConfigurationChanged(configuration);
    }

    private final class H extends Handler {
        public static final int NOTIFY_STARTING_WINDOW_DRAWN = 2;
        public static final int NOTIFY_WINDOWS_DRAWN = 1;

        public H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    if (AppWindowContainerController.this.mListener != 0) {
                        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                            Slog.v(WmsExt.TAG, "Reporting drawn in " + AppWindowContainerController.this.mToken);
                        }
                        ((AppWindowContainerListener) AppWindowContainerController.this.mListener).onWindowsDrawn(message.getWhen());
                        break;
                    }
                    break;
                case 2:
                    if (AppWindowContainerController.this.mListener != 0) {
                        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                            Slog.v(WmsExt.TAG, "Reporting starting window drawn in " + AppWindowContainerController.this.mToken);
                        }
                        ((AppWindowContainerListener) AppWindowContainerController.this.mListener).onStartingWindowDrawn(message.getWhen());
                        break;
                    }
                    break;
            }
        }
    }

    public static void lambda$new$0(AppWindowContainerController appWindowContainerController) {
        if (appWindowContainerController.mListener == 0) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(WmsExt.TAG, "Reporting visible in " + appWindowContainerController.mToken);
        }
        ((AppWindowContainerListener) appWindowContainerController.mListener).onWindowsVisible();
    }

    public static void lambda$new$1(AppWindowContainerController appWindowContainerController) {
        if (appWindowContainerController.mListener == 0) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(WmsExt.TAG, "Reporting gone in " + appWindowContainerController.mToken);
        }
        ((AppWindowContainerListener) appWindowContainerController.mListener).onWindowsGone();
    }

    public AppWindowContainerController(TaskWindowContainerController taskWindowContainerController, IApplicationToken iApplicationToken, AppWindowContainerListener appWindowContainerListener, int i, int i2, boolean z, boolean z2, int i3, boolean z3, boolean z4, boolean z5, int i4, int i5, long j) {
        this(taskWindowContainerController, iApplicationToken, appWindowContainerListener, i, i2, z, z2, i3, z3, z4, z5, i4, i5, j, WindowManagerService.getInstance());
    }

    public AppWindowContainerController(TaskWindowContainerController taskWindowContainerController, IApplicationToken iApplicationToken, AppWindowContainerListener appWindowContainerListener, int i, int i2, boolean z, boolean z2, int i3, boolean z3, boolean z4, boolean z5, int i4, int i5, long j, WindowManagerService windowManagerService) throws Throwable {
        int i6;
        super(appWindowContainerListener, windowManagerService);
        this.mWindowManagerDebugger = MtkSystemServiceFactory.getInstance().makeWindowManagerDebugger();
        this.mOnWindowsVisible = new Runnable() {
            @Override
            public final void run() {
                AppWindowContainerController.lambda$new$0(this.f$0);
            }
        };
        this.mOnWindowsGone = new Runnable() {
            @Override
            public final void run() {
                AppWindowContainerController.lambda$new$1(this.f$0);
            }
        };
        this.mAddStartingWindow = new Runnable() {
            @Override
            public void run() {
                WindowManagerPolicy.StartingSurface startingSurfaceCreateStartingSurface;
                synchronized (AppWindowContainerController.this.mWindowMap) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        if (AppWindowContainerController.this.mContainer == 0) {
                            if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                                Slog.v(WmsExt.TAG, "mContainer was null while trying to add starting window");
                            }
                            return;
                        }
                        if (((AppWindowToken) AppWindowContainerController.this.mContainer).startingSurface != null) {
                            Slog.v(WmsExt.TAG, "already has a starting surface!!!");
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                        AppWindowContainerController.this.mService.mAnimationHandler.removeCallbacks(this);
                        StartingData startingData = ((AppWindowToken) AppWindowContainerController.this.mContainer).startingData;
                        AppWindowToken appWindowToken = (AppWindowToken) AppWindowContainerController.this.mContainer;
                        WindowManagerService.resetPriorityAfterLockedSection();
                        if (startingData == null) {
                            if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                                Slog.v(WmsExt.TAG, "startingData was nulled out before handling mAddStartingWindow: " + AppWindowContainerController.this.mContainer);
                                return;
                            }
                            return;
                        }
                        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                            Slog.v(WmsExt.TAG, "Add starting " + AppWindowContainerController.this + ": startingData=" + appWindowToken.startingData);
                        }
                        try {
                            startingSurfaceCreateStartingSurface = startingData.createStartingSurface(appWindowToken);
                        } catch (Exception e) {
                            Slog.w(WmsExt.TAG, "Exception when adding starting window", e);
                            startingSurfaceCreateStartingSurface = null;
                        }
                        if (startingSurfaceCreateStartingSurface != null) {
                            boolean z6 = false;
                            synchronized (AppWindowContainerController.this.mWindowMap) {
                                try {
                                    WindowManagerService.boostPriorityForLockedSection();
                                    if (appWindowToken.removed || appWindowToken.startingData == null) {
                                        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                                            Slog.v(WmsExt.TAG, "Aborted starting " + appWindowToken + ": removed=" + appWindowToken.removed + " startingData=" + appWindowToken.startingData);
                                        }
                                        appWindowToken.startingWindow = null;
                                        appWindowToken.startingData = null;
                                        z6 = true;
                                    } else {
                                        appWindowToken.startingSurface = startingSurfaceCreateStartingSurface;
                                    }
                                    if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW && !z6) {
                                        Slog.v(WmsExt.TAG, "Added starting " + AppWindowContainerController.this.mContainer + ": startingWindow=" + appWindowToken.startingWindow + " startingView=" + appWindowToken.startingSurface);
                                    }
                                } finally {
                                }
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            if (z6) {
                                startingSurfaceCreateStartingSurface.remove();
                                return;
                            }
                            return;
                        }
                        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                            Slog.v(WmsExt.TAG, "Surface returned was null: " + AppWindowContainerController.this.mContainer);
                        }
                    } finally {
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
            }
        };
        this.mHandler = new H(windowManagerService.mH.getLooper());
        this.mToken = iApplicationToken;
        synchronized (this.mWindowMap) {
            try {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (this.mRoot.getAppWindowToken(this.mToken.asBinder()) != null) {
                        Slog.w(WmsExt.TAG, "Attempted to add existing app token: " + this.mToken);
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    Task task = (Task) taskWindowContainerController.mContainer;
                    if (task == null) {
                        throw new IllegalArgumentException("AppWindowContainerController: invalid  controller=" + taskWindowContainerController);
                    }
                    AppWindowToken appWindowTokenCreateAppWindow = createAppWindow(this.mService, iApplicationToken, z3, task.getDisplayContent(), j, z, z2, i4, i2, i5, i3, z4, z5, this);
                    if (WindowManagerDebugConfig.DEBUG_TOKEN_MOVEMENT || WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("addAppToken: ");
                        sb.append(appWindowTokenCreateAppWindow);
                        sb.append(" controller=");
                        sb.append(taskWindowContainerController);
                        sb.append(" at ");
                        i6 = i;
                        sb.append(i6);
                        Slog.v(WmsExt.TAG, sb.toString());
                    } else {
                        i6 = i;
                    }
                    task.addChild(appWindowTokenCreateAppWindow, i6);
                    WindowManagerService.resetPriorityAfterLockedSection();
                } catch (Throwable th) {
                    th = th;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    @VisibleForTesting
    AppWindowToken createAppWindow(WindowManagerService windowManagerService, IApplicationToken iApplicationToken, boolean z, DisplayContent displayContent, long j, boolean z2, boolean z3, int i, int i2, int i3, int i4, boolean z4, boolean z5, AppWindowContainerController appWindowContainerController) {
        return new AppWindowToken(windowManagerService, iApplicationToken, z, displayContent, j, z2, z3, i, i2, i3, i4, z4, z5, appWindowContainerController);
    }

    public void removeContainer(int i) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(i);
                if (displayContent == null) {
                    Slog.w(WmsExt.TAG, "removeAppToken: Attempted to remove binder token: " + this.mToken + " from non-existing displayId=" + i);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                displayContent.removeAppToken(this.mToken.asBinder());
                super.removeContainer();
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    @Override
    public void removeContainer() {
        throw new UnsupportedOperationException("Use removeContainer(displayId) instead.");
    }

    public void reparent(TaskWindowContainerController taskWindowContainerController, int i) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                    Slog.i(WmsExt.TAG, "reparent: moving app token=" + this.mToken + " to task=" + taskWindowContainerController + " at " + i);
                }
                if (this.mContainer == 0) {
                    if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                        Slog.i(WmsExt.TAG, "reparent: could not find app token=" + this.mToken);
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                Task task = (Task) taskWindowContainerController.mContainer;
                if (task == null) {
                    throw new IllegalArgumentException("reparent: could not find task=" + taskWindowContainerController);
                }
                ((AppWindowToken) this.mContainer).reparent(task, i);
                ((AppWindowToken) this.mContainer).getDisplayContent().layoutAndAssignWindowLayersIfNeeded();
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public Configuration setOrientation(int i, int i2, Configuration configuration, boolean z) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    Slog.w(WmsExt.TAG, "Attempted to set orientation of non-existing app token: " + this.mToken);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return null;
                }
                ((AppWindowToken) this.mContainer).setOrientation(i);
                IBinder iBinderAsBinder = z ? this.mToken.asBinder() : null;
                WindowManagerDebugger windowManagerDebugger = this.mWindowManagerDebugger;
                if (WindowManagerDebugger.WMS_DEBUG_ENG) {
                    Slog.d(WmsExt.TAG, "setAppOrientation to " + i + ", app:" + this.mToken);
                }
                Configuration configurationUpdateOrientationFromAppTokens = this.mService.updateOrientationFromAppTokens(configuration, iBinderAsBinder, i2);
                WindowManagerService.resetPriorityAfterLockedSection();
                return configurationUpdateOrientationFromAppTokens;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public int getOrientation() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return -1;
                }
                int orientationIgnoreVisibility = ((AppWindowToken) this.mContainer).getOrientationIgnoreVisibility();
                WindowManagerService.resetPriorityAfterLockedSection();
                return orientationIgnoreVisibility;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setDisablePreviewScreenshots(boolean z) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    Slog.w(WmsExt.TAG, "Attempted to set disable screenshots of non-existing app token: " + this.mToken);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ((AppWindowToken) this.mContainer).setDisablePreviewScreenshots(z);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setVisibility(boolean z, boolean z2) {
        WindowState windowStateFindFocusedWindow;
        AppWindowToken appWindowToken;
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    Slog.w(WmsExt.TAG, "Attempted to set visibility of non-existing app token: " + this.mToken);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                AppWindowToken appWindowToken2 = (AppWindowToken) this.mContainer;
                if (!z && appWindowToken2.hiddenRequested) {
                    if (!z2 && appWindowToken2.mDeferHidingClient) {
                        appWindowToken2.mDeferHidingClient = z2;
                        appWindowToken2.setClientHidden(true);
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v(WmsExt.TAG, "setAppVisibility(" + this.mToken + ", visible=" + z + "): " + this.mService.mAppTransition + " hidden=" + appWindowToken2.isHidden() + " hiddenRequested=" + appWindowToken2.hiddenRequested + " Callers=" + Debug.getCallers(6));
                } else {
                    WindowManagerDebugger windowManagerDebugger = this.mWindowManagerDebugger;
                    if (WindowManagerDebugger.WMS_DEBUG_ENG) {
                    }
                }
                this.mService.mOpeningApps.remove(appWindowToken2);
                this.mService.mClosingApps.remove(appWindowToken2);
                appWindowToken2.waitingToShow = false;
                appWindowToken2.hiddenRequested = !z;
                appWindowToken2.mDeferHidingClient = z2;
                if (!z) {
                    appWindowToken2.removeDeadWindows();
                } else {
                    if (!this.mService.mAppTransition.isTransitionSet() && this.mService.mAppTransition.isReady()) {
                        this.mService.mOpeningApps.add(appWindowToken2);
                    }
                    appWindowToken2.startingMoved = false;
                    if (appWindowToken2.isHidden() || appWindowToken2.mAppStopped) {
                        appWindowToken2.clearAllDrawn();
                        if (appWindowToken2.isHidden()) {
                            appWindowToken2.waitingToShow = true;
                        }
                    }
                    appWindowToken2.setClientHidden(false);
                    appWindowToken2.requestUpdateWallpaperIfNeeded();
                    if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                        Slog.v(WmsExt.TAG, "No longer Stopped: " + appWindowToken2);
                    }
                    appWindowToken2.mAppStopped = false;
                    ((AppWindowToken) this.mContainer).transferStartingWindowFromHiddenAboveTokenIfNeeded();
                }
                if (appWindowToken2.okToAnimate() && this.mService.mAppTransition.isTransitionSet()) {
                    appWindowToken2.inPendingTransaction = true;
                    if (z) {
                        this.mService.mOpeningApps.add(appWindowToken2);
                        appWindowToken2.mEnteringAnimation = true;
                    } else {
                        this.mService.mClosingApps.add(appWindowToken2);
                        appWindowToken2.mEnteringAnimation = false;
                    }
                    if (this.mService.mAppTransition.getAppTransition() == 16 && (windowStateFindFocusedWindow = this.mService.getDefaultDisplayContentLocked().findFocusedWindow()) != null && (appWindowToken = windowStateFindFocusedWindow.mAppToken) != null) {
                        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                            Slog.d(WmsExt.TAG, "TRANSIT_TASK_OPEN_BEHIND,  adding " + appWindowToken + " to mOpeningApps");
                        }
                        appWindowToken.setHidden(true);
                        this.mService.mOpeningApps.add(appWindowToken);
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                appWindowToken2.setVisibility(null, z, -1, true, appWindowToken2.mVoiceInteraction);
                appWindowToken2.updateReportedVisibilityLocked();
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void notifyUnknownVisibilityLaunched() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer != 0) {
                    this.mService.mUnknownAppVisibilityController.notifyLaunched((AppWindowToken) this.mContainer);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public boolean addStartingWindow(String str, int i, CompatibilityInfo compatibilityInfo, CharSequence charSequence, int i2, int i3, int i4, int i5, IBinder iBinder, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6) {
        boolean z7;
        boolean z8;
        boolean z9;
        boolean z10;
        int i6;
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("setAppStartingWindow: token=");
                    sb.append(this.mToken);
                    sb.append(" pkg=");
                    sb.append(str);
                    sb.append(" transferFrom=");
                    sb.append(iBinder);
                    sb.append(" newTask=");
                    z7 = z;
                    sb.append(z7);
                    sb.append(" taskSwitch=");
                    z8 = z2;
                    sb.append(z8);
                    sb.append(" processRunning=");
                    z9 = z3;
                    sb.append(z9);
                    sb.append(" allowTaskSnapshot=");
                    z10 = z4;
                    sb.append(z10);
                    Slog.v(WmsExt.TAG, sb.toString());
                } else {
                    z7 = z;
                    z8 = z2;
                    z9 = z3;
                    z10 = z4;
                }
                if (this.mContainer == 0) {
                    Slog.w(WmsExt.TAG, "Attempted to set icon of non-existing app token: " + this.mToken);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                if (!((AppWindowToken) this.mContainer).okToDisplay()) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                if (((AppWindowToken) this.mContainer).startingData != null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                WindowState windowStateFindMainWindow = ((AppWindowToken) this.mContainer).findMainWindow();
                if (windowStateFindMainWindow != null && windowStateFindMainWindow.mWinAnimator.getShown()) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                ActivityManager.TaskSnapshot snapshot = this.mService.mTaskSnapshotController.getSnapshot(((AppWindowToken) this.mContainer).getTask().mTaskId, ((AppWindowToken) this.mContainer).getTask().mUserId, false, false);
                int startingWindowType = getStartingWindowType(z7, z8, z9, z10, z5, z6, snapshot);
                if (startingWindowType == 1) {
                    boolean zCreateSnapshot = createSnapshot(snapshot);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return zCreateSnapshot;
                }
                if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                    Slog.v(WmsExt.TAG, "Checking theme of starting window: 0x" + Integer.toHexString(i));
                }
                if (i != 0) {
                    AttributeCache.Entry entry = AttributeCache.instance().get(str, i, R.styleable.Window, this.mService.mCurrentUserId);
                    if (entry == null) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return false;
                    }
                    boolean z11 = entry.array.getBoolean(5, false);
                    boolean z12 = entry.array.getBoolean(4, false);
                    boolean z13 = entry.array.getBoolean(14, false);
                    boolean z14 = entry.array.getBoolean(12, false);
                    if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                        Slog.v(WmsExt.TAG, "Translucent=" + z11 + " Floating=" + z12 + " ShowWallpaper=" + z13);
                    }
                    if (z11) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return false;
                    }
                    if (z12 || z14) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return false;
                    }
                    if (z13) {
                        if (((AppWindowToken) this.mContainer).getDisplayContent().mWallpaperController.getWallpaperTarget() != null) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return false;
                        }
                        i6 = i5 | DumpState.DUMP_DEXOPT;
                    }
                } else {
                    i6 = i5;
                }
                if (((AppWindowToken) this.mContainer).transferStartingWindow(iBinder)) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return true;
                }
                if (startingWindowType != 2) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                    Slog.v(WmsExt.TAG, "Creating SplashScreenStartingData");
                }
                ((AppWindowToken) this.mContainer).startingData = new SplashScreenStartingData(this.mService, str, i, compatibilityInfo, charSequence, i2, i3, i4, i6, ((AppWindowToken) this.mContainer).getMergedOverrideConfiguration());
                scheduleAddStartingWindow();
                WindowManagerService.resetPriorityAfterLockedSection();
                return true;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private int getStartingWindowType(boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, ActivityManager.TaskSnapshot taskSnapshot) {
        if (this.mService.mAppTransition.getAppTransition() == 19) {
            return 0;
        }
        if (z || !z3 || (z2 && !z5)) {
            return 2;
        }
        if (!z2 || !z4 || taskSnapshot == null) {
            return 0;
        }
        if (!snapshotOrientationSameAsTask(taskSnapshot) && !z6) {
            return 2;
        }
        return 1;
    }

    void scheduleAddStartingWindow() {
        if (!this.mService.mAnimationHandler.hasCallbacks(this.mAddStartingWindow)) {
            if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                Slog.v(WmsExt.TAG, "Enqueueing ADD_STARTING");
            }
            this.mService.mAnimationHandler.postAtFrontOfQueue(this.mAddStartingWindow);
        }
    }

    private boolean createSnapshot(ActivityManager.TaskSnapshot taskSnapshot) {
        if (taskSnapshot == null) {
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
            Slog.v(WmsExt.TAG, "Creating SnapshotStartingData");
        }
        ((AppWindowToken) this.mContainer).startingData = new SnapshotStartingData(this.mService, taskSnapshot);
        scheduleAddStartingWindow();
        return true;
    }

    private boolean snapshotOrientationSameAsTask(ActivityManager.TaskSnapshot taskSnapshot) {
        return taskSnapshot != null && ((AppWindowToken) this.mContainer).getTask().getConfiguration().orientation == taskSnapshot.getOrientation();
    }

    public void removeStartingWindow() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (((AppWindowToken) this.mContainer).startingWindow == null) {
                    if (((AppWindowToken) this.mContainer).startingData != null) {
                        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                            Slog.v(WmsExt.TAG, "Clearing startingData for token=" + this.mContainer);
                        }
                        ((AppWindowToken) this.mContainer).startingData = null;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (((AppWindowToken) this.mContainer).startingData != null) {
                    final WindowManagerPolicy.StartingSurface startingSurface = ((AppWindowToken) this.mContainer).startingSurface;
                    ((AppWindowToken) this.mContainer).startingData = null;
                    ((AppWindowToken) this.mContainer).startingSurface = null;
                    ((AppWindowToken) this.mContainer).startingWindow = null;
                    ((AppWindowToken) this.mContainer).startingDisplayed = false;
                    if (startingSurface == null) {
                        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                            Slog.v(WmsExt.TAG, "startingWindow was set but startingSurface==null, couldn't remove");
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                        Slog.v(WmsExt.TAG, "Schedule remove starting " + this.mContainer + " startingWindow=" + ((AppWindowToken) this.mContainer).startingWindow + " startingView=" + ((AppWindowToken) this.mContainer).startingSurface + " Callers=" + Debug.getCallers(5));
                    }
                    this.mService.mAnimationHandler.post(new Runnable() {
                        @Override
                        public final void run() {
                            AppWindowContainerController.lambda$removeStartingWindow$2(startingSurface);
                        }
                    });
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                    Slog.v(WmsExt.TAG, "Tried to remove starting window but startingWindow was null:" + this.mContainer);
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    static void lambda$removeStartingWindow$2(WindowManagerPolicy.StartingSurface startingSurface) {
        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
            Slog.v(WmsExt.TAG, "Removing startingView=" + startingSurface);
        }
        try {
            startingSurface.remove();
        } catch (Exception e) {
            Slog.w(WmsExt.TAG, "Exception when removing starting window", e);
        }
    }

    public void pauseKeyDispatching() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer != 0) {
                    this.mService.mInputMonitor.pauseDispatchingLw((WindowToken) this.mContainer);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void resumeKeyDispatching() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer != 0) {
                    this.mService.mInputMonitor.resumeDispatchingLw((WindowToken) this.mContainer);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void notifyAppResumed(boolean z) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    Slog.w(WmsExt.TAG, "Attempted to notify resumed of non-existing app token: " + this.mToken);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ((AppWindowToken) this.mContainer).notifyAppResumed(z);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void notifyAppStopping() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    Slog.w(WmsExt.TAG, "Attempted to notify stopping on non-existing app token: " + this.mToken);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ((AppWindowToken) this.mContainer).detachChildren();
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void notifyAppStopped() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    Slog.w(WmsExt.TAG, "Attempted to notify stopped of non-existing app token: " + this.mToken);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ((AppWindowToken) this.mContainer).notifyAppStopped();
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void startFreezingScreen(int i) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    Slog.w(WmsExt.TAG, "Attempted to freeze screen with non-existing app token: " + this.mContainer);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (i == 0 && ((AppWindowToken) this.mContainer).okToDisplay()) {
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        Slog.v(WmsExt.TAG, "Skipping set freeze of " + this.mToken);
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ((AppWindowToken) this.mContainer).startFreezingScreen();
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void stopFreezingScreen(boolean z) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v(WmsExt.TAG, "Clear freezing of " + this.mToken + ": hidden=" + ((AppWindowToken) this.mContainer).isHidden() + " freezing=" + ((AppWindowToken) this.mContainer).isFreezingScreen());
                }
                ((AppWindowToken) this.mContainer).stopFreezingScreen(true, z);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void registerRemoteAnimations(RemoteAnimationDefinition remoteAnimationDefinition) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    Slog.w(WmsExt.TAG, "Attempted to register remote animations with non-existing app token: " + this.mToken);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ((AppWindowToken) this.mContainer).registerRemoteAnimations(remoteAnimationDefinition);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    void reportStartingWindowDrawn() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2));
    }

    void reportWindowsDrawn() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1));
    }

    void reportWindowsVisible() {
        this.mHandler.post(this.mOnWindowsVisible);
    }

    void reportWindowsGone() {
        this.mHandler.post(this.mOnWindowsGone);
    }

    boolean keyDispatchingTimedOut(String str, int i) {
        return this.mListener != 0 && ((AppWindowContainerListener) this.mListener).keyDispatchingTimedOut(str, i);
    }

    public void setWillCloseOrEnterPip(boolean z) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                } else {
                    ((AppWindowToken) this.mContainer).setWillCloseOrEnterPip(z);
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public String toString() {
        return "AppWindowContainerController{ token=" + this.mToken + " mContainer=" + this.mContainer + " mListener=" + this.mListener + "}";
    }
}
