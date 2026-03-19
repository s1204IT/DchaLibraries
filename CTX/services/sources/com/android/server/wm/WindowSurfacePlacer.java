package com.android.server.wm;

import android.os.Debug;
import android.os.Trace;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseIntArray;
import android.view.RemoteAnimationAdapter;
import android.view.RemoteAnimationDefinition;
import android.view.WindowManager;
import android.view.animation.Animation;
import com.android.internal.annotations.VisibleForTesting;
import java.io.PrintWriter;
import java.util.function.Predicate;

class WindowSurfacePlacer {
    static final int SET_FORCE_HIDING_CHANGED = 4;
    static final int SET_ORIENTATION_CHANGE_COMPLETE = 8;
    static final int SET_UPDATE_ROTATION = 1;
    static final int SET_WALLPAPER_ACTION_PENDING = 16;
    static final int SET_WALLPAPER_MAY_CHANGE = 2;
    private static final String TAG = "WindowManager";
    private int mLayoutRepeatCount;
    private final WindowManagerService mService;
    private boolean mTraversalScheduled;
    private final WallpaperController mWallpaperControllerLocked;
    private boolean mInLayout = false;
    private int mDeferDepth = 0;
    private final LayerAndToken mTmpLayerAndToken = new LayerAndToken();
    private final SparseIntArray mTempTransitionReasons = new SparseIntArray();
    private final Runnable mPerformSurfacePlacement = new Runnable() {
        @Override
        public final void run() {
            WindowSurfacePlacer.lambda$new$0(this.f$0);
        }
    };

    private static final class LayerAndToken {
        public int layer;
        public AppWindowToken token;

        private LayerAndToken() {
        }
    }

    public WindowSurfacePlacer(WindowManagerService windowManagerService) {
        this.mService = windowManagerService;
        this.mWallpaperControllerLocked = this.mService.mRoot.mWallpaperController;
    }

    public static void lambda$new$0(WindowSurfacePlacer windowSurfacePlacer) {
        synchronized (windowSurfacePlacer.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                windowSurfacePlacer.performSurfacePlacement();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    void deferLayout() {
        this.mDeferDepth++;
    }

    void continueLayout() {
        this.mDeferDepth--;
        if (this.mDeferDepth <= 0) {
            performSurfacePlacement();
        }
    }

    boolean isLayoutDeferred() {
        return this.mDeferDepth > 0;
    }

    final void performSurfacePlacement() {
        performSurfacePlacement(false);
    }

    final void performSurfacePlacement(boolean z) {
        if (this.mDeferDepth > 0 && !z) {
            return;
        }
        int i = 6;
        do {
            this.mTraversalScheduled = false;
            performSurfacePlacementLoop();
            this.mService.mAnimationHandler.removeCallbacks(this.mPerformSurfacePlacement);
            i--;
            if (!this.mTraversalScheduled) {
                break;
            }
        } while (i > 0);
        this.mService.mRoot.mWallpaperActionPending = false;
    }

    private void performSurfacePlacementLoop() {
        boolean z;
        if (this.mInLayout) {
            if (WindowManagerDebugConfig.DEBUG) {
                throw new RuntimeException("Recursive call!");
            }
            Slog.w("WindowManager", "performLayoutAndPlaceSurfacesLocked called while in layout. Callers=" + Debug.getCallers(3));
            return;
        }
        if (this.mService.mWaitingForConfig || !this.mService.mDisplayReady) {
            return;
        }
        Trace.traceBegin(32L, "wmLayout");
        this.mInLayout = true;
        if (!this.mService.mForceRemoves.isEmpty()) {
            while (!this.mService.mForceRemoves.isEmpty()) {
                WindowState windowStateRemove = this.mService.mForceRemoves.remove(0);
                Slog.i("WindowManager", "Force removing: " + windowStateRemove);
                windowStateRemove.removeImmediately();
            }
            Slog.w("WindowManager", "Due to memory failure, waiting a bit for next layout");
            Object obj = new Object();
            synchronized (obj) {
                try {
                    obj.wait(250L);
                } catch (InterruptedException e) {
                }
            }
            z = true;
        } else {
            z = false;
        }
        try {
            this.mService.mRoot.performSurfacePlacement(z);
            this.mInLayout = false;
            if (this.mService.mRoot.isLayoutNeeded()) {
                int i = this.mLayoutRepeatCount + 1;
                this.mLayoutRepeatCount = i;
                if (i < 6) {
                    requestTraversal();
                } else {
                    Slog.e("WindowManager", "Performed 6 layouts in a row. Skipping");
                    this.mLayoutRepeatCount = 0;
                }
            } else {
                this.mLayoutRepeatCount = 0;
            }
            if (this.mService.mWindowsChanged && !this.mService.mWindowChangeListeners.isEmpty()) {
                this.mService.mH.removeMessages(19);
                this.mService.mH.sendEmptyMessage(19);
            }
        } catch (RuntimeException e2) {
            this.mInLayout = false;
            Slog.wtf("WindowManager", "Unhandled exception while laying out windows", e2);
        }
        Trace.traceEnd(32L);
    }

    void debugLayoutRepeats(String str, int i) {
        if (this.mLayoutRepeatCount >= 4) {
            Slog.v("WindowManager", "Layouts looping: " + str + ", mPendingLayoutChanges = 0x" + Integer.toHexString(i));
        }
    }

    boolean isInLayout() {
        return this.mInLayout;
    }

    int handleAppTransitionReadyLocked() {
        AppWindowToken appWindowTokenFindAnimLayoutParamsToken;
        int size = this.mService.mOpeningApps.size();
        if (!transitionGoodToGo(size, this.mTempTransitionReasons)) {
            return 0;
        }
        Trace.traceBegin(32L, "AppTransitionReady");
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
            Slog.v("WindowManager", "**** GOOD TO GO");
        }
        int appTransition = this.mService.mAppTransition.getAppTransition();
        if (this.mService.mSkipAppTransitionAnimation && !AppTransition.isKeyguardGoingAwayTransit(appTransition)) {
            appTransition = -1;
        }
        this.mService.mSkipAppTransitionAnimation = false;
        this.mService.mNoAnimationNotifyOnTransitionFinished.clear();
        this.mService.mH.removeMessages(13);
        DisplayContent defaultDisplayContentLocked = this.mService.getDefaultDisplayContentLocked();
        this.mService.mRoot.mWallpaperMayChange = false;
        for (int i = 0; i < size; i++) {
            this.mService.mOpeningApps.valueAt(i).clearAnimatingFlags();
        }
        this.mWallpaperControllerLocked.adjustWallpaperWindowsForAppTransitionIfNeeded(defaultDisplayContentLocked, this.mService.mOpeningApps);
        boolean z = this.mWallpaperControllerLocked.getWallpaperTarget() != null;
        int iMaybeUpdateTransitToWallpaper = maybeUpdateTransitToWallpaper(maybeUpdateTransitToTranslucentAnim(appTransition), canBeWallpaperTarget(this.mService.mOpeningApps) && z, canBeWallpaperTarget(this.mService.mClosingApps) && z);
        ArraySet<Integer> arraySetCollectActivityTypes = collectActivityTypes(this.mService.mOpeningApps, this.mService.mClosingApps);
        if (this.mService.mPolicy.allowAppAnimationsLw()) {
            appWindowTokenFindAnimLayoutParamsToken = findAnimLayoutParamsToken(iMaybeUpdateTransitToWallpaper, arraySetCollectActivityTypes);
        } else {
            appWindowTokenFindAnimLayoutParamsToken = null;
        }
        WindowManager.LayoutParams animLp = getAnimLp(appWindowTokenFindAnimLayoutParamsToken);
        overrideWithRemoteAnimationIfSet(appWindowTokenFindAnimLayoutParamsToken, iMaybeUpdateTransitToWallpaper, arraySetCollectActivityTypes);
        boolean z2 = containsVoiceInteraction(this.mService.mOpeningApps) || containsVoiceInteraction(this.mService.mOpeningApps);
        this.mService.mSurfaceAnimationRunner.deferStartingAnimations();
        try {
            processApplicationsAnimatingInPlace(iMaybeUpdateTransitToWallpaper);
            this.mTmpLayerAndToken.token = null;
            handleClosingApps(iMaybeUpdateTransitToWallpaper, animLp, z2, this.mTmpLayerAndToken);
            AppWindowToken appWindowToken = this.mTmpLayerAndToken.token;
            AppWindowToken appWindowTokenHandleOpeningApps = handleOpeningApps(iMaybeUpdateTransitToWallpaper, animLp, z2);
            this.mService.mAppTransition.setLastAppTransition(iMaybeUpdateTransitToWallpaper, appWindowTokenHandleOpeningApps, appWindowToken);
            int transitFlags = this.mService.mAppTransition.getTransitFlags();
            int iGoodToGo = this.mService.mAppTransition.goodToGo(iMaybeUpdateTransitToWallpaper, appWindowTokenHandleOpeningApps, appWindowToken, this.mService.mOpeningApps, this.mService.mClosingApps);
            handleNonAppWindowsInTransition(iMaybeUpdateTransitToWallpaper, transitFlags);
            this.mService.mAppTransition.postAnimationCallback();
            this.mService.mAppTransition.clear();
            this.mService.mSurfaceAnimationRunner.continueStartingAnimations();
            this.mService.mTaskSnapshotController.onTransitionStarting();
            this.mService.mOpeningApps.clear();
            this.mService.mClosingApps.clear();
            this.mService.mUnknownAppVisibilityController.clear();
            defaultDisplayContentLocked.setLayoutNeeded();
            this.mService.getDefaultDisplayContentLocked().computeImeTarget(true);
            this.mService.updateFocusedWindowLocked(2, true);
            this.mService.mFocusMayChange = false;
            this.mService.mH.obtainMessage(47, this.mTempTransitionReasons.clone()).sendToTarget();
            Trace.traceEnd(32L);
            return iGoodToGo | 1 | 2;
        } catch (Throwable th) {
            this.mService.mSurfaceAnimationRunner.continueStartingAnimations();
            throw th;
        }
    }

    private static WindowManager.LayoutParams getAnimLp(AppWindowToken appWindowToken) {
        WindowState windowStateFindMainWindow = appWindowToken != null ? appWindowToken.findMainWindow() : null;
        if (windowStateFindMainWindow != null) {
            return windowStateFindMainWindow.mAttrs;
        }
        return null;
    }

    private void overrideWithRemoteAnimationIfSet(AppWindowToken appWindowToken, int i, ArraySet<Integer> arraySet) {
        RemoteAnimationDefinition remoteAnimationDefinition;
        RemoteAnimationAdapter adapter;
        if (i != 26 && appWindowToken != null && (remoteAnimationDefinition = appWindowToken.getRemoteAnimationDefinition()) != null && (adapter = remoteAnimationDefinition.getAdapter(i, arraySet)) != null) {
            this.mService.mAppTransition.overridePendingAppTransitionRemote(adapter);
        }
    }

    private AppWindowToken findAnimLayoutParamsToken(final int i, final ArraySet<Integer> arraySet) {
        AppWindowToken appWindowTokenLookForHighestTokenWithFilter = lookForHighestTokenWithFilter(this.mService.mClosingApps, this.mService.mOpeningApps, new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return WindowSurfacePlacer.lambda$findAnimLayoutParamsToken$1(i, arraySet, (AppWindowToken) obj);
            }
        });
        if (appWindowTokenLookForHighestTokenWithFilter != null) {
            return appWindowTokenLookForHighestTokenWithFilter;
        }
        AppWindowToken appWindowTokenLookForHighestTokenWithFilter2 = lookForHighestTokenWithFilter(this.mService.mClosingApps, this.mService.mOpeningApps, new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return WindowSurfacePlacer.lambda$findAnimLayoutParamsToken$2((AppWindowToken) obj);
            }
        });
        if (appWindowTokenLookForHighestTokenWithFilter2 != null) {
            return appWindowTokenLookForHighestTokenWithFilter2;
        }
        return lookForHighestTokenWithFilter(this.mService.mClosingApps, this.mService.mOpeningApps, new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return WindowSurfacePlacer.lambda$findAnimLayoutParamsToken$3((AppWindowToken) obj);
            }
        });
    }

    static boolean lambda$findAnimLayoutParamsToken$1(int i, ArraySet arraySet, AppWindowToken appWindowToken) {
        return appWindowToken.getRemoteAnimationDefinition() != null && appWindowToken.getRemoteAnimationDefinition().hasTransition(i, arraySet);
    }

    static boolean lambda$findAnimLayoutParamsToken$2(AppWindowToken appWindowToken) {
        return appWindowToken.fillsParent() && appWindowToken.findMainWindow() != null;
    }

    static boolean lambda$findAnimLayoutParamsToken$3(AppWindowToken appWindowToken) {
        return appWindowToken.findMainWindow() != null;
    }

    private ArraySet<Integer> collectActivityTypes(ArraySet<AppWindowToken> arraySet, ArraySet<AppWindowToken> arraySet2) {
        ArraySet<Integer> arraySet3 = new ArraySet<>();
        for (int size = arraySet.size() - 1; size >= 0; size--) {
            arraySet3.add(Integer.valueOf(arraySet.valueAt(size).getActivityType()));
        }
        for (int size2 = arraySet2.size() - 1; size2 >= 0; size2--) {
            arraySet3.add(Integer.valueOf(arraySet2.valueAt(size2).getActivityType()));
        }
        return arraySet3;
    }

    private AppWindowToken lookForHighestTokenWithFilter(ArraySet<AppWindowToken> arraySet, ArraySet<AppWindowToken> arraySet2, Predicate<AppWindowToken> predicate) {
        AppWindowToken appWindowTokenValueAt;
        int size = arraySet.size();
        int size2 = arraySet2.size() + size;
        int i = Integer.MIN_VALUE;
        AppWindowToken appWindowToken = null;
        for (int i2 = 0; i2 < size2; i2++) {
            if (i2 < size) {
                appWindowTokenValueAt = arraySet.valueAt(i2);
            } else {
                appWindowTokenValueAt = arraySet2.valueAt(i2 - size);
            }
            int prefixOrderIndex = appWindowTokenValueAt.getPrefixOrderIndex();
            if (predicate.test(appWindowTokenValueAt) && prefixOrderIndex > i) {
                appWindowToken = appWindowTokenValueAt;
                i = prefixOrderIndex;
            }
        }
        return appWindowToken;
    }

    private boolean containsVoiceInteraction(ArraySet<AppWindowToken> arraySet) {
        for (int size = arraySet.size() - 1; size >= 0; size--) {
            if (arraySet.valueAt(size).mVoiceInteraction) {
                return true;
            }
        }
        return false;
    }

    private AppWindowToken handleOpeningApps(int i, WindowManager.LayoutParams layoutParams, boolean z) {
        int size = this.mService.mOpeningApps.size();
        int i2 = Integer.MIN_VALUE;
        AppWindowToken appWindowToken = null;
        for (int i3 = 0; i3 < size; i3++) {
            AppWindowToken appWindowTokenValueAt = this.mService.mOpeningApps.valueAt(i3);
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v("WindowManager", "Now opening app" + appWindowTokenValueAt);
            }
            if (!appWindowTokenValueAt.setVisibility(layoutParams, true, i, false, z)) {
                this.mService.mNoAnimationNotifyOnTransitionFinished.add(appWindowTokenValueAt.token);
            }
            appWindowTokenValueAt.updateReportedVisibilityLocked();
            appWindowTokenValueAt.waitingToShow = false;
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i("WindowManager", ">>> OPEN TRANSACTION handleAppTransitionReadyLocked()");
            }
            this.mService.openSurfaceTransaction();
            try {
                appWindowTokenValueAt.showAllWindowsLocked();
                if (layoutParams != null) {
                    int highestAnimLayer = appWindowTokenValueAt.getHighestAnimLayer();
                    if (appWindowToken == null || highestAnimLayer > i2) {
                        appWindowToken = appWindowTokenValueAt;
                        i2 = highestAnimLayer;
                    }
                }
                if (this.mService.mAppTransition.isNextAppTransitionThumbnailUp()) {
                    appWindowTokenValueAt.attachThumbnailAnimation();
                } else if (this.mService.mAppTransition.isNextAppTransitionOpenCrossProfileApps()) {
                    appWindowTokenValueAt.attachCrossProfileAppsThumbnailAnimation();
                }
            } finally {
                this.mService.closeSurfaceTransaction("handleAppTransitionReadyLocked");
                if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                    Slog.i("WindowManager", "<<< CLOSE TRANSACTION handleAppTransitionReadyLocked()");
                }
            }
        }
        return appWindowToken;
    }

    private void handleClosingApps(int i, WindowManager.LayoutParams layoutParams, boolean z, LayerAndToken layerAndToken) {
        int size = this.mService.mClosingApps.size();
        for (int i2 = 0; i2 < size; i2++) {
            AppWindowToken appWindowTokenValueAt = this.mService.mClosingApps.valueAt(i2);
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v("WindowManager", "Now closing app " + appWindowTokenValueAt);
            }
            appWindowTokenValueAt.setVisibility(layoutParams, false, i, false, z);
            appWindowTokenValueAt.updateReportedVisibilityLocked();
            appWindowTokenValueAt.allDrawn = true;
            appWindowTokenValueAt.deferClearAllDrawn = false;
            if (appWindowTokenValueAt.startingWindow != null && !appWindowTokenValueAt.startingWindow.mAnimatingExit && appWindowTokenValueAt.getController() != null) {
                appWindowTokenValueAt.getController().removeStartingWindow();
            }
            if (layoutParams != null) {
                int highestAnimLayer = appWindowTokenValueAt.getHighestAnimLayer();
                if (layerAndToken.token == null || highestAnimLayer > layerAndToken.layer) {
                    layerAndToken.token = appWindowTokenValueAt;
                    layerAndToken.layer = highestAnimLayer;
                }
            }
            if (this.mService.mAppTransition.isNextAppTransitionThumbnailDown()) {
                appWindowTokenValueAt.attachThumbnailAnimation();
            }
        }
    }

    private void handleNonAppWindowsInTransition(int i, int i2) {
        if (i == 20 && (i2 & 4) != 0 && (i2 & 2) == 0) {
            Animation animationCreateKeyguardWallpaperExit = this.mService.mPolicy.createKeyguardWallpaperExit((i2 & 1) != 0);
            if (animationCreateKeyguardWallpaperExit != null) {
                this.mService.getDefaultDisplayContentLocked().mWallpaperController.startWallpaperAnimation(animationCreateKeyguardWallpaperExit);
            }
        }
        if (i == 20 || i == 21) {
            this.mService.getDefaultDisplayContentLocked().startKeyguardExitOnNonAppWindows(i == 21, (i2 & 1) != 0);
        }
    }

    private boolean transitionGoodToGo(int i, SparseIntArray sparseIntArray) {
        int i2;
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
            Slog.v("WindowManager", "Checking " + i + " opening apps (frozen=" + this.mService.mDisplayFrozen + " timeout=" + this.mService.mAppTransition.isTimeout() + ")...");
        }
        ScreenRotationAnimation screenRotationAnimationLocked = this.mService.mAnimator.getScreenRotationAnimationLocked(0);
        sparseIntArray.clear();
        if (this.mService.mAppTransition.isTimeout()) {
            return true;
        }
        if (screenRotationAnimationLocked != null && screenRotationAnimationLocked.isAnimating() && this.mService.rotationNeedsUpdateLocked()) {
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v("WindowManager", "Delaying app transition for screen rotation animation to finish");
            }
            return false;
        }
        for (int i3 = 0; i3 < i; i3++) {
            AppWindowToken appWindowTokenValueAt = this.mService.mOpeningApps.valueAt(i3);
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v("WindowManager", "Check opening app=" + appWindowTokenValueAt + ": allDrawn=" + appWindowTokenValueAt.allDrawn + " startingDisplayed=" + appWindowTokenValueAt.startingDisplayed + " startingMoved=" + appWindowTokenValueAt.startingMoved + " isRelaunching()=" + appWindowTokenValueAt.isRelaunching() + " startingWindow=" + appWindowTokenValueAt.startingWindow);
            }
            boolean z = appWindowTokenValueAt.allDrawn && !appWindowTokenValueAt.isRelaunching();
            if (!z && !appWindowTokenValueAt.startingDisplayed && !appWindowTokenValueAt.startingMoved) {
                return false;
            }
            int windowingMode = appWindowTokenValueAt.getWindowingMode();
            if (z) {
                sparseIntArray.put(windowingMode, 2);
            } else {
                if (appWindowTokenValueAt.startingData instanceof SplashScreenStartingData) {
                    i2 = 1;
                } else {
                    i2 = 4;
                }
                sparseIntArray.put(windowingMode, i2);
            }
        }
        if (this.mService.mAppTransition.isFetchingAppTransitionsSpecs()) {
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v("WindowManager", "isFetchingAppTransitionSpecs=true");
            }
            return false;
        }
        if (this.mService.mUnknownAppVisibilityController.allResolved()) {
            return !this.mWallpaperControllerLocked.isWallpaperVisible() || this.mWallpaperControllerLocked.wallpaperTransitionReady();
        }
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
            Slog.v("WindowManager", "unknownApps is not empty: " + this.mService.mUnknownAppVisibilityController.getDebugMessage());
        }
        return false;
    }

    private int maybeUpdateTransitToWallpaper(int i, boolean z, boolean z2) {
        WindowState windowState;
        if (i == 0 || i == 26 || i == 19) {
            return i;
        }
        WindowState wallpaperTarget = this.mWallpaperControllerLocked.getWallpaperTarget();
        if (this.mWallpaperControllerLocked.isWallpaperTargetAnimating()) {
            windowState = null;
        } else {
            windowState = wallpaperTarget;
        }
        ArraySet<AppWindowToken> arraySet = this.mService.mOpeningApps;
        ArraySet<AppWindowToken> arraySet2 = this.mService.mClosingApps;
        AppWindowToken topApp = getTopApp(this.mService.mOpeningApps, false);
        AppWindowToken topApp2 = getTopApp(this.mService.mClosingApps, true);
        boolean zCanBeWallpaperTarget = canBeWallpaperTarget(arraySet);
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
            Slog.v("WindowManager", "New wallpaper target=" + wallpaperTarget + ", oldWallpaper=" + windowState + ", openingApps=" + arraySet + ", closingApps=" + arraySet2);
        }
        if (zCanBeWallpaperTarget && i == 20) {
            i = 21;
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v("WindowManager", "New transit: " + AppTransition.appTransitionToString(21));
            }
        } else if (!AppTransition.isKeyguardGoingAwayTransit(i)) {
            if (z2 && z) {
                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                    Slog.v("WindowManager", "Wallpaper animation!");
                }
                switch (i) {
                    case 6:
                    case 8:
                    case 10:
                        i = 14;
                        break;
                    case 7:
                    case 9:
                    case 11:
                        i = 15;
                        break;
                }
                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                    Slog.v("WindowManager", "New transit: " + AppTransition.appTransitionToString(i));
                }
            } else if (windowState != null && !this.mService.mOpeningApps.isEmpty() && !arraySet.contains(windowState.mAppToken) && arraySet2.contains(windowState.mAppToken) && topApp2 == windowState.mAppToken) {
                i = 12;
                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                    Slog.v("WindowManager", "New transit away from wallpaper: " + AppTransition.appTransitionToString(12));
                }
            } else if (wallpaperTarget != null && wallpaperTarget.isVisibleLw() && arraySet.contains(wallpaperTarget.mAppToken) && topApp == wallpaperTarget.mAppToken && i != 25) {
                i = 13;
                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                    Slog.v("WindowManager", "New transit into wallpaper: " + AppTransition.appTransitionToString(13));
                }
            }
        }
        return i;
    }

    @VisibleForTesting
    int maybeUpdateTransitToTranslucentAnim(int i) {
        boolean z = false;
        boolean z2 = AppTransition.isTaskTransit(i) || AppTransition.isActivityTransit(i);
        boolean z3 = !this.mService.mOpeningApps.isEmpty();
        boolean z4 = z3;
        boolean z5 = true;
        for (int size = this.mService.mOpeningApps.size() - 1; size >= 0; size--) {
            AppWindowToken appWindowTokenValueAt = this.mService.mOpeningApps.valueAt(size);
            if (!appWindowTokenValueAt.isVisible()) {
                if (appWindowTokenValueAt.fillsParent()) {
                    z5 = false;
                    z4 = false;
                } else {
                    z5 = false;
                }
            }
        }
        boolean z6 = !this.mService.mClosingApps.isEmpty();
        int size2 = this.mService.mClosingApps.size() - 1;
        while (true) {
            if (size2 >= 0) {
                if (this.mService.mClosingApps.valueAt(size2).fillsParent()) {
                    break;
                }
                size2--;
            } else {
                z = z6;
                break;
            }
        }
        if (z2 && z && z5) {
            return 25;
        }
        if (z2 && z4 && this.mService.mClosingApps.isEmpty()) {
            return 24;
        }
        return i;
    }

    private boolean canBeWallpaperTarget(ArraySet<AppWindowToken> arraySet) {
        for (int size = arraySet.size() - 1; size >= 0; size--) {
            if (arraySet.valueAt(size).windowsCanBeWallpaperTarget()) {
                return true;
            }
        }
        return false;
    }

    private AppWindowToken getTopApp(ArraySet<AppWindowToken> arraySet, boolean z) {
        int prefixOrderIndex;
        int i = Integer.MIN_VALUE;
        AppWindowToken appWindowToken = null;
        for (int size = arraySet.size() - 1; size >= 0; size--) {
            AppWindowToken appWindowTokenValueAt = arraySet.valueAt(size);
            if ((!z || !appWindowTokenValueAt.isHidden()) && (prefixOrderIndex = appWindowTokenValueAt.getPrefixOrderIndex()) > i) {
                appWindowToken = appWindowTokenValueAt;
                i = prefixOrderIndex;
            }
        }
        return appWindowToken;
    }

    private void processApplicationsAnimatingInPlace(int i) {
        WindowState windowStateFindFocusedWindow;
        if (i == 17 && (windowStateFindFocusedWindow = this.mService.getDefaultDisplayContentLocked().findFocusedWindow()) != null) {
            AppWindowToken appWindowToken = windowStateFindFocusedWindow.mAppToken;
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v("WindowManager", "Now animating app in place " + appWindowToken);
            }
            appWindowToken.cancelAnimation();
            appWindowToken.applyAnimationLocked(null, i, false, false);
            appWindowToken.updateReportedVisibilityLocked();
            appWindowToken.showAllWindowsLocked();
        }
    }

    void requestTraversal() {
        if (!this.mTraversalScheduled) {
            this.mTraversalScheduled = true;
            this.mService.mAnimationHandler.post(this.mPerformSurfacePlacement);
        }
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.println(str + "mTraversalScheduled=" + this.mTraversalScheduled);
        printWriter.println(str + "mHoldScreenWindow=" + this.mService.mRoot.mHoldScreenWindow);
        printWriter.println(str + "mObscuringWindow=" + this.mService.mRoot.mObscuringWindow);
    }
}
