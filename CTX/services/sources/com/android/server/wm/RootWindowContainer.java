package com.android.server.wm;

import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseIntArray;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import android.view.WindowManager;
import com.android.internal.util.ArrayUtils;
import com.android.server.EventLogTags;
import com.android.server.backup.BackupManagerConstants;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

class RootWindowContainer extends WindowContainer<DisplayContent> {
    private static final int SET_SCREEN_BRIGHTNESS_OVERRIDE = 1;
    private static final int SET_USER_ACTIVITY_TIMEOUT = 2;
    private static final String TAG = "WindowManager";
    private static final Consumer<WindowState> sRemoveReplacedWindowsConsumer = new Consumer() {
        @Override
        public final void accept(Object obj) {
            RootWindowContainer.lambda$static$1((WindowState) obj);
        }
    };
    private final Consumer<WindowState> mCloseSystemDialogsConsumer;
    private String mCloseSystemDialogsReason;
    private final SurfaceControl.Transaction mDisplayTransaction;
    private final Handler mHandler;
    private Session mHoldScreen;
    WindowState mHoldScreenWindow;
    private Object mLastWindowFreezeSource;
    private boolean mObscureApplicationContentOnSecondaryDisplays;
    WindowState mObscuringWindow;
    boolean mOrientationChangeComplete;
    private float mScreenBrightness;
    private boolean mSustainedPerformanceModeCurrent;
    private boolean mSustainedPerformanceModeEnabled;
    private final ArrayList<Integer> mTmpStackIds;
    private final ArrayList<TaskStack> mTmpStackList;
    private boolean mUpdateRotation;
    private long mUserActivityTimeout;
    boolean mWallpaperActionPending;
    final WallpaperController mWallpaperController;
    private boolean mWallpaperForceHidingChanged;
    boolean mWallpaperMayChange;

    public static void lambda$new$0(RootWindowContainer rootWindowContainer, WindowState windowState) {
        if (windowState.mHasSurface) {
            try {
                windowState.mClient.closeSystemDialogs(rootWindowContainer.mCloseSystemDialogsReason);
            } catch (RemoteException e) {
            }
        }
    }

    static void lambda$static$1(WindowState windowState) {
        AppWindowToken appWindowToken = windowState.mAppToken;
        if (appWindowToken != null) {
            appWindowToken.removeReplacedWindowIfNeeded(windowState);
        }
    }

    RootWindowContainer(WindowManagerService windowManagerService) {
        super(windowManagerService);
        this.mWallpaperForceHidingChanged = false;
        this.mLastWindowFreezeSource = null;
        this.mHoldScreen = null;
        this.mScreenBrightness = -1.0f;
        this.mUserActivityTimeout = -1L;
        this.mUpdateRotation = false;
        this.mHoldScreenWindow = null;
        this.mObscuringWindow = null;
        this.mObscureApplicationContentOnSecondaryDisplays = false;
        this.mSustainedPerformanceModeEnabled = false;
        this.mSustainedPerformanceModeCurrent = false;
        this.mWallpaperMayChange = false;
        this.mOrientationChangeComplete = true;
        this.mWallpaperActionPending = false;
        this.mTmpStackList = new ArrayList<>();
        this.mTmpStackIds = new ArrayList<>();
        this.mDisplayTransaction = new SurfaceControl.Transaction();
        this.mCloseSystemDialogsConsumer = new Consumer() {
            @Override
            public final void accept(Object obj) {
                RootWindowContainer.lambda$new$0(this.f$0, (WindowState) obj);
            }
        };
        this.mHandler = new MyHandler(windowManagerService.mH.getLooper());
        this.mWallpaperController = new WallpaperController(this.mService);
    }

    WindowState computeFocusedWindow() {
        boolean zIsKeyguardShowingAndNotOccluded = this.mService.isKeyguardShowingAndNotOccluded();
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            DisplayContent displayContent = (DisplayContent) this.mChildren.get(size);
            WindowState windowStateFindFocusedWindow = displayContent.findFocusedWindow();
            if (windowStateFindFocusedWindow != null) {
                if (zIsKeyguardShowingAndNotOccluded && !displayContent.isDefaultDisplay) {
                    EventLog.writeEvent(1397638484, "71786287", Integer.valueOf(windowStateFindFocusedWindow.mOwnerUid), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                } else {
                    return windowStateFindFocusedWindow;
                }
            }
        }
        return null;
    }

    void getDisplaysInFocusOrder(SparseIntArray sparseIntArray) {
        sparseIntArray.clear();
        int size = this.mChildren.size();
        for (int i = 0; i < size; i++) {
            DisplayContent displayContent = (DisplayContent) this.mChildren.get(i);
            if (!displayContent.isRemovalDeferred()) {
                sparseIntArray.put(i, displayContent.getDisplayId());
            }
        }
    }

    DisplayContent getDisplayContent(int i) {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            DisplayContent displayContent = (DisplayContent) this.mChildren.get(size);
            if (displayContent.getDisplayId() == i) {
                return displayContent;
            }
        }
        return null;
    }

    DisplayContent createDisplayContent(Display display, DisplayWindowController displayWindowController) {
        int displayId = display.getDisplayId();
        DisplayContent displayContent = getDisplayContent(displayId);
        if (displayContent != null) {
            displayContent.setController(displayWindowController);
            return displayContent;
        }
        DisplayContent displayContent2 = new DisplayContent(display, this.mService, this.mWallpaperController, displayWindowController);
        if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
            Slog.v("WindowManager", "Adding display=" + display);
        }
        DisplayInfo displayInfo = displayContent2.getDisplayInfo();
        Rect rect = new Rect();
        this.mService.mDisplaySettings.getOverscanLocked(displayInfo.name, displayInfo.uniqueId, rect);
        displayInfo.overscanLeft = rect.left;
        displayInfo.overscanTop = rect.top;
        displayInfo.overscanRight = rect.right;
        displayInfo.overscanBottom = rect.bottom;
        if (this.mService.mDisplayManagerInternal != null) {
            this.mService.mDisplayManagerInternal.setDisplayInfoOverrideFromWindowManager(displayId, displayInfo);
            displayContent2.configureDisplayPolicy();
            if (this.mService.canDispatchPointerEvents()) {
                if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
                    Slog.d("WindowManager", "Registering PointerEventListener for DisplayId: " + displayId);
                }
                displayContent2.mTapDetector = new TaskTapPointerEventListener(this.mService, displayContent2);
                this.mService.registerPointerEventListener(displayContent2.mTapDetector);
                if (displayId == 0) {
                    this.mService.registerPointerEventListener(this.mService.mMousePositionTracker);
                }
            }
        }
        return displayContent2;
    }

    boolean isLayoutNeeded() {
        int size = this.mChildren.size();
        for (int i = 0; i < size; i++) {
            if (((DisplayContent) this.mChildren.get(i)).isLayoutNeeded()) {
                return true;
            }
        }
        return false;
    }

    void getWindowsByName(ArrayList<WindowState> arrayList, String str) {
        int i;
        try {
            i = Integer.parseInt(str, 16);
            str = null;
        } catch (RuntimeException e) {
            i = 0;
        }
        getWindowsByName(arrayList, str, i);
    }

    private void getWindowsByName(final ArrayList<WindowState> arrayList, final String str, final int i) {
        forAllWindows(new Consumer() {
            @Override
            public final void accept(Object obj) {
                RootWindowContainer.lambda$getWindowsByName$2(str, arrayList, i, (WindowState) obj);
            }
        }, true);
    }

    static void lambda$getWindowsByName$2(String str, ArrayList arrayList, int i, WindowState windowState) {
        if (str != null) {
            if (windowState.mAttrs.getTitle().toString().contains(str)) {
                arrayList.add(windowState);
            }
        } else if (System.identityHashCode(windowState) == i) {
            arrayList.add(windowState);
        }
    }

    AppWindowToken getAppWindowToken(IBinder iBinder) {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            AppWindowToken appWindowToken = ((DisplayContent) this.mChildren.get(size)).getAppWindowToken(iBinder);
            if (appWindowToken != null) {
                return appWindowToken;
            }
        }
        return null;
    }

    DisplayContent getWindowTokenDisplay(WindowToken windowToken) {
        if (windowToken == null) {
            return null;
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            DisplayContent displayContent = (DisplayContent) this.mChildren.get(size);
            if (displayContent.getWindowToken(windowToken.token) == windowToken) {
                return displayContent;
            }
        }
        return null;
    }

    int[] setDisplayOverrideConfigurationIfNeeded(Configuration configuration, int i) {
        DisplayContent displayContent = getDisplayContent(i);
        if (displayContent == null) {
            throw new IllegalArgumentException("Display not found for id: " + i);
        }
        if (!(displayContent.getOverrideConfiguration().diff(configuration) != 0)) {
            return null;
        }
        displayContent.onOverrideConfigurationChanged(configuration);
        this.mTmpStackList.clear();
        if (i == 0) {
            setGlobalConfigurationIfNeeded(configuration, this.mTmpStackList);
        } else {
            updateStackBoundsAfterConfigChange(i, this.mTmpStackList);
        }
        this.mTmpStackIds.clear();
        int size = this.mTmpStackList.size();
        for (int i2 = 0; i2 < size; i2++) {
            TaskStack taskStack = this.mTmpStackList.get(i2);
            if (!taskStack.mDeferRemoval) {
                this.mTmpStackIds.add(Integer.valueOf(taskStack.mStackId));
            }
        }
        if (this.mTmpStackIds.isEmpty()) {
            return null;
        }
        return ArrayUtils.convertToIntArray(this.mTmpStackIds);
    }

    private void setGlobalConfigurationIfNeeded(Configuration configuration, List<TaskStack> list) {
        if (!(getConfiguration().diff(configuration) != 0)) {
            return;
        }
        onConfigurationChanged(configuration);
        updateStackBoundsAfterConfigChange(list);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        prepareFreezingTaskBounds();
        super.onConfigurationChanged(configuration);
        this.mService.mPolicy.onConfigurationChanged();
    }

    private void updateStackBoundsAfterConfigChange(List<TaskStack> list) {
        int size = this.mChildren.size();
        for (int i = 0; i < size; i++) {
            ((DisplayContent) this.mChildren.get(i)).updateStackBoundsAfterConfigChange(list);
        }
    }

    private void updateStackBoundsAfterConfigChange(int i, List<TaskStack> list) {
        getDisplayContent(i).updateStackBoundsAfterConfigChange(list);
    }

    private void prepareFreezingTaskBounds() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((DisplayContent) this.mChildren.get(size)).prepareFreezingTaskBounds();
        }
    }

    TaskStack getStack(int i, int i2) {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            TaskStack stack = ((DisplayContent) this.mChildren.get(size)).getStack(i, i2);
            if (stack != null) {
                return stack;
            }
        }
        return null;
    }

    void setSecureSurfaceState(final int i, final boolean z) {
        forAllWindows(new Consumer() {
            @Override
            public final void accept(Object obj) {
                RootWindowContainer.lambda$setSecureSurfaceState$3(i, z, (WindowState) obj);
            }
        }, true);
    }

    static void lambda$setSecureSurfaceState$3(int i, boolean z, WindowState windowState) {
        if (windowState.mHasSurface && i == UserHandle.getUserId(windowState.mOwnerUid)) {
            windowState.mWinAnimator.setSecureLocked(z);
        }
    }

    void updateHiddenWhileSuspendedState(final ArraySet<String> arraySet, final boolean z) {
        forAllWindows(new Consumer() {
            @Override
            public final void accept(Object obj) {
                RootWindowContainer.lambda$updateHiddenWhileSuspendedState$4(arraySet, z, (WindowState) obj);
            }
        }, false);
    }

    static void lambda$updateHiddenWhileSuspendedState$4(ArraySet arraySet, boolean z, WindowState windowState) {
        if (arraySet.contains(windowState.getOwningPackage())) {
            windowState.setHiddenWhileSuspended(z);
        }
    }

    void updateAppOpsState() {
        forAllWindows((Consumer<WindowState>) new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((WindowState) obj).updateAppOpsState();
            }
        }, false);
    }

    static boolean lambda$canShowStrictModeViolation$6(int i, WindowState windowState) {
        return windowState.mSession.mPid == i && windowState.isVisibleLw();
    }

    boolean canShowStrictModeViolation(final int i) {
        return getWindow(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return RootWindowContainer.lambda$canShowStrictModeViolation$6(i, (WindowState) obj);
            }
        }) != null;
    }

    void closeSystemDialogs(String str) {
        this.mCloseSystemDialogsReason = str;
        forAllWindows(this.mCloseSystemDialogsConsumer, false);
    }

    void removeReplacedWindows() {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            Slog.i("WindowManager", ">>> OPEN TRANSACTION removeReplacedWindows");
        }
        this.mService.openSurfaceTransaction();
        try {
            forAllWindows(sRemoveReplacedWindowsConsumer, true);
        } finally {
            this.mService.closeSurfaceTransaction("removeReplacedWindows");
            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                Slog.i("WindowManager", "<<< CLOSE TRANSACTION removeReplacedWindows");
            }
        }
    }

    boolean hasPendingLayoutChanges(WindowAnimator windowAnimator) {
        int size = this.mChildren.size();
        boolean z = false;
        for (int i = 0; i < size; i++) {
            int pendingLayoutChanges = windowAnimator.getPendingLayoutChanges(((DisplayContent) this.mChildren.get(i)).getDisplayId());
            if ((pendingLayoutChanges & 4) != 0) {
                windowAnimator.mBulkUpdateParams |= 16;
            }
            if (pendingLayoutChanges != 0) {
                z = true;
            }
        }
        return z;
    }

    boolean reclaimSomeSurfaceMemory(WindowStateAnimator windowStateAnimator, String str, boolean z) {
        boolean z2;
        WindowSurfaceController windowSurfaceController = windowStateAnimator.mSurfaceController;
        EventLog.writeEvent(EventLogTags.WM_NO_SURFACE_MEMORY, windowStateAnimator.mWin.toString(), Integer.valueOf(windowStateAnimator.mSession.mPid), str);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Slog.i("WindowManager", "Out of memory for surface!  Looking for leaks...");
            int size = this.mChildren.size();
            boolean zDestroyLeakedSurfaces = false;
            for (int i = 0; i < size; i++) {
                zDestroyLeakedSurfaces |= ((DisplayContent) this.mChildren.get(i)).destroyLeakedSurfaces();
            }
            if (!zDestroyLeakedSurfaces) {
                Slog.w("WindowManager", "No leaked surfaces; killing applications!");
                final SparseIntArray sparseIntArray = new SparseIntArray();
                z2 = false;
                for (int i2 = 0; i2 < size; i2++) {
                    ((DisplayContent) this.mChildren.get(i2)).forAllWindows(new Consumer() {
                        @Override
                        public final void accept(Object obj) {
                            RootWindowContainer.lambda$reclaimSomeSurfaceMemory$7(this.f$0, sparseIntArray, (WindowState) obj);
                        }
                    }, false);
                    if (sparseIntArray.size() > 0) {
                        int[] iArr = new int[sparseIntArray.size()];
                        for (int i3 = 0; i3 < iArr.length; i3++) {
                            iArr[i3] = sparseIntArray.keyAt(i3);
                        }
                        try {
                            if (this.mService.mActivityManager.killPids(iArr, "Free memory", z)) {
                                z2 = true;
                            }
                        } catch (RemoteException e) {
                        }
                    }
                }
            } else {
                z2 = false;
            }
            if (zDestroyLeakedSurfaces || z2) {
                Slog.w("WindowManager", "Looks like we have reclaimed some memory, clearing surface for retry.");
                if (windowSurfaceController != null) {
                    if (WindowManagerDebugConfig.SHOW_TRANSACTIONS || WindowManagerDebugConfig.SHOW_SURFACE_ALLOC) {
                        WindowManagerService.logSurface(windowStateAnimator.mWin, "RECOVER DESTROY", false);
                    }
                    windowStateAnimator.destroySurface();
                    if (windowStateAnimator.mWin.mAppToken != null && windowStateAnimator.mWin.mAppToken.getController() != null) {
                        windowStateAnimator.mWin.mAppToken.getController().removeStartingWindow();
                    }
                }
                try {
                    windowStateAnimator.mWin.mClient.dispatchGetNewSurface();
                } catch (RemoteException e2) {
                }
            }
            return zDestroyLeakedSurfaces || z2;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public static void lambda$reclaimSomeSurfaceMemory$7(RootWindowContainer rootWindowContainer, SparseIntArray sparseIntArray, WindowState windowState) {
        if (rootWindowContainer.mService.mForceRemoves.contains(windowState)) {
            return;
        }
        WindowStateAnimator windowStateAnimator = windowState.mWinAnimator;
        if (windowStateAnimator.mSurfaceController != null) {
            sparseIntArray.append(windowStateAnimator.mSession.mPid, windowStateAnimator.mSession.mPid);
        }
    }

    void performSurfacePlacement(boolean z) {
        boolean zUpdateFocusedWindowLocked;
        boolean z2;
        if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
            Slog.v("WindowManager", "performSurfacePlacementInner: entry. Called by " + Debug.getCallers(3));
        }
        if (this.mService.mFocusMayChange) {
            this.mService.mFocusMayChange = false;
            zUpdateFocusedWindowLocked = this.mService.updateFocusedWindowLocked(3, false);
        } else {
            zUpdateFocusedWindowLocked = false;
        }
        int size = this.mChildren.size();
        for (int i = 0; i < size; i++) {
            ((DisplayContent) this.mChildren.get(i)).setExitingTokensHasVisible(false);
        }
        this.mHoldScreen = null;
        this.mScreenBrightness = -1.0f;
        this.mUserActivityTimeout = -1L;
        this.mObscureApplicationContentOnSecondaryDisplays = false;
        this.mSustainedPerformanceModeCurrent = false;
        this.mService.mTransactionSequence++;
        DisplayContent defaultDisplayContentLocked = this.mService.getDefaultDisplayContentLocked();
        DisplayInfo displayInfo = defaultDisplayContentLocked.getDisplayInfo();
        int i2 = displayInfo.logicalWidth;
        int i3 = displayInfo.logicalHeight;
        if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
            Slog.i("WindowManager", ">>> OPEN TRANSACTION performLayoutAndPlaceSurfaces");
        }
        this.mService.openSurfaceTransaction();
        try {
            try {
                applySurfaceChangesTransaction(z, i2, i3);
            } catch (RuntimeException e) {
                Slog.wtf("WindowManager", "Unhandled exception in Window Manager", e);
                this.mService.closeSurfaceTransaction("performLayoutAndPlaceSurfaces");
                if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                }
            }
            this.mService.mAnimator.executeAfterPrepareSurfacesRunnables();
            WindowSurfacePlacer windowSurfacePlacer = this.mService.mWindowPlacerLocked;
            if (this.mService.mAppTransition.isReady()) {
                defaultDisplayContentLocked.pendingLayoutChanges = windowSurfacePlacer.handleAppTransitionReadyLocked() | defaultDisplayContentLocked.pendingLayoutChanges;
                if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                    windowSurfacePlacer.debugLayoutRepeats("after handleAppTransitionReadyLocked", defaultDisplayContentLocked.pendingLayoutChanges);
                }
            }
            if (!isAppAnimating() && this.mService.mAppTransition.isRunning()) {
                defaultDisplayContentLocked.pendingLayoutChanges |= this.mService.handleAnimatingStoppedAndTransitionLocked();
                if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                    windowSurfacePlacer.debugLayoutRepeats("after handleAnimStopAndXitionLock", defaultDisplayContentLocked.pendingLayoutChanges);
                }
            }
            RecentsAnimationController recentsAnimationController = this.mService.getRecentsAnimationController();
            if (recentsAnimationController != null) {
                recentsAnimationController.checkAnimationReady(this.mWallpaperController);
            }
            if (this.mWallpaperForceHidingChanged && defaultDisplayContentLocked.pendingLayoutChanges == 0 && !this.mService.mAppTransition.isReady()) {
                defaultDisplayContentLocked.pendingLayoutChanges |= 1;
                if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                    windowSurfacePlacer.debugLayoutRepeats("after animateAwayWallpaperLocked", defaultDisplayContentLocked.pendingLayoutChanges);
                }
            }
            this.mWallpaperForceHidingChanged = false;
            if (this.mWallpaperMayChange) {
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                    Slog.v("WindowManager", "Wallpaper may change!  Adjusting");
                }
                defaultDisplayContentLocked.pendingLayoutChanges |= 4;
                if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                    windowSurfacePlacer.debugLayoutRepeats("WallpaperMayChange", defaultDisplayContentLocked.pendingLayoutChanges);
                }
            }
            if (this.mService.mFocusMayChange) {
                this.mService.mFocusMayChange = false;
                if (this.mService.updateFocusedWindowLocked(2, false)) {
                    defaultDisplayContentLocked.pendingLayoutChanges |= 8;
                    zUpdateFocusedWindowLocked = true;
                }
            }
            if (isLayoutNeeded()) {
                defaultDisplayContentLocked.pendingLayoutChanges |= 1;
                if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                    windowSurfacePlacer.debugLayoutRepeats("mLayoutNeeded", defaultDisplayContentLocked.pendingLayoutChanges);
                }
            }
            ArraySet<DisplayContent> arraySetHandleResizingWindows = handleResizingWindows();
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION && this.mService.mDisplayFrozen) {
                Slog.v("WindowManager", "With display frozen, orientationChangeComplete=" + this.mOrientationChangeComplete);
            }
            if (this.mOrientationChangeComplete) {
                if (this.mService.mWindowsFreezingScreen != 0) {
                    this.mService.mWindowsFreezingScreen = 0;
                    this.mService.mLastFinishedFreezeSource = this.mLastWindowFreezeSource;
                    this.mService.mH.removeMessages(11);
                }
                this.mService.stopFreezingDisplayLocked();
            }
            int size2 = this.mService.mDestroySurface.size();
            if (size2 > 0) {
                boolean z3 = false;
                do {
                    size2--;
                    WindowState windowState = this.mService.mDestroySurface.get(size2);
                    windowState.mDestroying = false;
                    if (this.mService.mInputMethodWindow == windowState) {
                        this.mService.setInputMethodWindowLocked(null);
                    }
                    z3 = z3;
                    if (windowState.getDisplayContent().mWallpaperController.isWallpaperTarget(windowState)) {
                        z3 = true;
                    }
                    windowState.destroySurfaceUnchecked();
                    windowState.mWinAnimator.destroyPreservedSurfaceLocked();
                } while (size2 > 0);
                this.mService.mDestroySurface.clear();
                z2 = z3;
            } else {
                z2 = false;
            }
            for (int i4 = 0; i4 < size; i4++) {
                ((DisplayContent) this.mChildren.get(i4)).removeExistingTokensIfPossible();
            }
            if (z2) {
                defaultDisplayContentLocked.pendingLayoutChanges |= 4;
                defaultDisplayContentLocked.setLayoutNeeded();
            }
            for (int i5 = 0; i5 < size; i5++) {
                DisplayContent displayContent = (DisplayContent) this.mChildren.get(i5);
                if (displayContent.pendingLayoutChanges != 0) {
                    displayContent.setLayoutNeeded();
                }
            }
            this.mService.mInputMonitor.updateInputWindowsLw(true);
            this.mService.setHoldScreenLocked(this.mHoldScreen);
            if (!this.mService.mDisplayFrozen) {
                this.mHandler.obtainMessage(1, (this.mScreenBrightness < 0.0f || this.mScreenBrightness > 1.0f) ? -1 : toBrightnessOverride(this.mScreenBrightness), 0).sendToTarget();
                this.mHandler.obtainMessage(2, Long.valueOf(this.mUserActivityTimeout)).sendToTarget();
            }
            if (this.mSustainedPerformanceModeCurrent != this.mSustainedPerformanceModeEnabled) {
                this.mSustainedPerformanceModeEnabled = this.mSustainedPerformanceModeCurrent;
                this.mService.mPowerManagerInternal.powerHint(6, this.mSustainedPerformanceModeEnabled ? 1 : 0);
            }
            if (this.mUpdateRotation) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.d("WindowManager", "Performing post-rotate rotation");
                }
                int displayId = defaultDisplayContentLocked.getDisplayId();
                if (defaultDisplayContentLocked.updateRotationUnchecked()) {
                    this.mService.mH.obtainMessage(18, Integer.valueOf(displayId)).sendToTarget();
                } else {
                    this.mUpdateRotation = false;
                }
                DisplayContent displayContent2 = this.mService.mVr2dDisplayId != -1 ? getDisplayContent(this.mService.mVr2dDisplayId) : null;
                if (displayContent2 != null && displayContent2.updateRotationUnchecked()) {
                    this.mService.mH.obtainMessage(18, Integer.valueOf(this.mService.mVr2dDisplayId)).sendToTarget();
                }
            }
            if (this.mService.mWaitingForDrawnCallback != null || (this.mOrientationChangeComplete && !defaultDisplayContentLocked.isLayoutNeeded() && !this.mUpdateRotation)) {
                this.mService.checkDrawnWindowsLocked();
            }
            int size3 = this.mService.mPendingRemove.size();
            if (size3 > 0) {
                if (this.mService.mPendingRemoveTmp.length < size3) {
                    this.mService.mPendingRemoveTmp = new WindowState[size3 + 10];
                }
                this.mService.mPendingRemove.toArray(this.mService.mPendingRemoveTmp);
                this.mService.mPendingRemove.clear();
                ArrayList arrayList = new ArrayList();
                for (int i6 = 0; i6 < size3; i6++) {
                    WindowState windowState2 = this.mService.mPendingRemoveTmp[i6];
                    windowState2.removeImmediately();
                    DisplayContent displayContent3 = windowState2.getDisplayContent();
                    if (displayContent3 != null && !arrayList.contains(displayContent3)) {
                        arrayList.add(displayContent3);
                    }
                }
                for (int size4 = arrayList.size() - 1; size4 >= 0; size4--) {
                    ((DisplayContent) arrayList.get(size4)).assignWindowLayers(true);
                }
            }
            for (int size5 = this.mChildren.size() - 1; size5 >= 0; size5--) {
                ((DisplayContent) this.mChildren.get(size5)).checkCompleteDeferredRemoval();
            }
            if (zUpdateFocusedWindowLocked) {
                this.mService.mInputMonitor.updateInputWindowsLw(false);
            }
            this.mService.setFocusTaskRegionLocked(null);
            if (arraySetHandleResizingWindows != null) {
                DisplayContent displayContent4 = this.mService.mFocusedApp != null ? this.mService.mFocusedApp.getDisplayContent() : null;
                for (DisplayContent displayContent5 : arraySetHandleResizingWindows) {
                    if (displayContent4 != displayContent5) {
                        displayContent5.setTouchExcludeRegion(null);
                    }
                }
            }
            this.mService.enableScreenIfNeededLocked();
            this.mService.scheduleAnimationLocked();
            if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
                Slog.e("WindowManager", "performSurfacePlacementInner exit: animating=" + this.mService.mAnimator.isAnimating());
            }
        } finally {
            this.mService.closeSurfaceTransaction("performLayoutAndPlaceSurfaces");
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i("WindowManager", "<<< CLOSE TRANSACTION performLayoutAndPlaceSurfaces");
            }
        }
    }

    private void applySurfaceChangesTransaction(boolean z, int i, int i2) {
        this.mHoldScreenWindow = null;
        this.mObscuringWindow = null;
        if (this.mService.mWatermark != null) {
            this.mService.mWatermark.positionSurface(i, i2);
        }
        if (this.mService.mStrictModeFlash != null) {
            this.mService.mStrictModeFlash.positionSurface(i, i2);
        }
        if (this.mService.mCircularDisplayMask != null) {
            this.mService.mCircularDisplayMask.positionSurface(i, i2, this.mService.getDefaultDisplayRotation());
        }
        if (this.mService.mEmulatorDisplayOverlay != null) {
            this.mService.mEmulatorDisplayOverlay.positionSurface(i, i2, this.mService.getDefaultDisplayRotation());
        }
        int size = this.mChildren.size();
        boolean zApplySurfaceChangesTransaction = false;
        for (int i3 = 0; i3 < size; i3++) {
            zApplySurfaceChangesTransaction |= ((DisplayContent) this.mChildren.get(i3)).applySurfaceChangesTransaction(z);
        }
        if (zApplySurfaceChangesTransaction) {
            this.mService.mH.sendEmptyMessage(3);
        }
        this.mService.mDisplayManagerInternal.performTraversal(this.mDisplayTransaction);
        SurfaceControl.mergeToGlobalTransaction(this.mDisplayTransaction);
    }

    private ArraySet<DisplayContent> handleResizingWindows() {
        ArraySet<DisplayContent> arraySet = null;
        for (int size = this.mService.mResizingWindows.size() - 1; size >= 0; size--) {
            WindowState windowState = this.mService.mResizingWindows.get(size);
            if (!windowState.mAppFreezing) {
                windowState.reportResized();
                this.mService.mResizingWindows.remove(size);
                if (WindowManagerService.excludeWindowTypeFromTapOutTask(windowState.mAttrs.type)) {
                    DisplayContent displayContent = windowState.getDisplayContent();
                    if (arraySet == null) {
                        arraySet = new ArraySet<>();
                    }
                    arraySet.add(displayContent);
                }
            }
        }
        return arraySet;
    }

    boolean handleNotObscuredLocked(WindowState windowState, boolean z, boolean z2) {
        WindowManager.LayoutParams layoutParams = windowState.mAttrs;
        int i = layoutParams.flags;
        boolean zIsOnScreen = windowState.isOnScreen();
        boolean zIsDisplayedLw = windowState.isDisplayedLw();
        int i2 = layoutParams.privateFlags;
        if (WindowManagerDebugConfig.DEBUG_KEEP_SCREEN_ON) {
            Slog.d("DebugKeepScreenOn", "handleNotObscuredLocked w: " + windowState + ", w.mHasSurface: " + windowState.mHasSurface + ", w.isOnScreen(): " + zIsOnScreen + ", w.isDisplayedLw(): " + windowState.isDisplayedLw() + ", w.mAttrs.userActivityTimeout: " + windowState.mAttrs.userActivityTimeout);
        }
        if (windowState.mHasSurface && zIsOnScreen && !z2 && windowState.mAttrs.userActivityTimeout >= 0 && this.mUserActivityTimeout < 0) {
            this.mUserActivityTimeout = windowState.mAttrs.userActivityTimeout;
            if (WindowManagerDebugConfig.DEBUG_KEEP_SCREEN_ON) {
                Slog.d("WindowManager", "mUserActivityTimeout set to " + this.mUserActivityTimeout);
            }
        }
        boolean z3 = false;
        if (windowState.mHasSurface && zIsDisplayedLw) {
            if ((i & 128) != 0) {
                this.mHoldScreen = windowState.mSession;
                this.mHoldScreenWindow = windowState;
            } else if (WindowManagerDebugConfig.DEBUG_KEEP_SCREEN_ON && windowState == this.mService.mLastWakeLockHoldingWindow) {
                Slog.d("DebugKeepScreenOn", "handleNotObscuredLocked: " + windowState + " was holding screen wakelock but no longer has FLAG_KEEP_SCREEN_ON!!! called by" + Debug.getCallers(10));
            }
            if (!z2 && windowState.mAttrs.screenBrightness >= 0.0f && this.mScreenBrightness < 0.0f) {
                this.mScreenBrightness = windowState.mAttrs.screenBrightness;
            }
            int i3 = layoutParams.type;
            DisplayContent displayContent = windowState.getDisplayContent();
            if (displayContent != null && displayContent.isDefaultDisplay) {
                if (i3 == 2023 || (layoutParams.privateFlags & 1024) != 0) {
                    this.mObscureApplicationContentOnSecondaryDisplays = true;
                }
            } else {
                if (displayContent != null && (!this.mObscureApplicationContentOnSecondaryDisplays || (z && i3 == 2009))) {
                }
                if ((262144 & i2) != 0) {
                    this.mSustainedPerformanceModeCurrent = true;
                }
            }
            z3 = true;
            if ((262144 & i2) != 0) {
            }
        }
        return z3;
    }

    boolean copyAnimToLayoutParams() {
        boolean z;
        int i = this.mService.mAnimator.mBulkUpdateParams;
        if ((i & 1) != 0) {
            this.mUpdateRotation = true;
            z = true;
        } else {
            z = false;
        }
        if ((i & 2) != 0) {
            this.mWallpaperMayChange = true;
            z = true;
        }
        if ((i & 4) != 0) {
            this.mWallpaperForceHidingChanged = true;
            z = true;
        }
        if ((i & 8) == 0) {
            this.mOrientationChangeComplete = false;
        } else {
            this.mOrientationChangeComplete = true;
            this.mLastWindowFreezeSource = this.mService.mAnimator.mLastWindowFreezeSource;
            if (this.mService.mWindowsFreezingScreen != 0) {
                z = true;
            }
        }
        if ((i & 16) != 0) {
            this.mWallpaperActionPending = true;
        }
        return z;
    }

    private static int toBrightnessOverride(float f) {
        return (int) (f * 255.0f);
    }

    private final class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    RootWindowContainer.this.mService.mPowerManagerInternal.setScreenBrightnessOverrideFromWindowManager(message.arg1);
                    break;
                case 2:
                    RootWindowContainer.this.mService.mPowerManagerInternal.setUserActivityTimeoutOverrideFromWindowManager(((Long) message.obj).longValue());
                    break;
            }
        }
    }

    void dumpDisplayContents(PrintWriter printWriter) {
        printWriter.println("WINDOW MANAGER DISPLAY CONTENTS (dumpsys window displays)");
        if (this.mService.mDisplayReady) {
            int size = this.mChildren.size();
            for (int i = 0; i < size; i++) {
                ((DisplayContent) this.mChildren.get(i)).dump(printWriter, "  ", true);
            }
            return;
        }
        printWriter.println("  NO DISPLAY");
    }

    void dumpLayoutNeededDisplayIds(PrintWriter printWriter) {
        if (!isLayoutNeeded()) {
            return;
        }
        printWriter.print("  mLayoutNeeded on displays=");
        int size = this.mChildren.size();
        for (int i = 0; i < size; i++) {
            DisplayContent displayContent = (DisplayContent) this.mChildren.get(i);
            if (displayContent.isLayoutNeeded()) {
                printWriter.print(displayContent.getDisplayId());
            }
        }
        printWriter.println();
    }

    void dumpWindowsNoHeader(final PrintWriter printWriter, final boolean z, final ArrayList<WindowState> arrayList) {
        final int[] iArr = new int[1];
        forAllWindows(new Consumer() {
            @Override
            public final void accept(Object obj) {
                RootWindowContainer.lambda$dumpWindowsNoHeader$8(arrayList, printWriter, iArr, z, (WindowState) obj);
            }
        }, true);
    }

    static void lambda$dumpWindowsNoHeader$8(ArrayList arrayList, PrintWriter printWriter, int[] iArr, boolean z, WindowState windowState) {
        if (arrayList == null || arrayList.contains(windowState)) {
            printWriter.println("  Window #" + iArr[0] + " " + windowState + ":");
            windowState.dump(printWriter, "    ", z || arrayList != null);
            iArr[0] = iArr[0] + 1;
        }
    }

    void dumpTokens(PrintWriter printWriter, boolean z) {
        printWriter.println("  All tokens:");
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((DisplayContent) this.mChildren.get(size)).dumpTokens(printWriter, z);
        }
    }

    @Override
    public void writeToProto(final ProtoOutputStream protoOutputStream, long j, boolean z) {
        long jStart = protoOutputStream.start(j);
        super.writeToProto(protoOutputStream, 1146756268033L, z);
        if (this.mService.mDisplayReady) {
            int size = this.mChildren.size();
            for (int i = 0; i < size; i++) {
                ((DisplayContent) this.mChildren.get(i)).writeToProto(protoOutputStream, 2246267895810L, z);
            }
        }
        if (!z) {
            forAllWindows(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ((WindowState) obj).writeIdentifierToProto(protoOutputStream, 2246267895811L);
                }
            }, true);
        }
        protoOutputStream.end(jStart);
    }

    @Override
    String getName() {
        return "ROOT";
    }

    @Override
    void scheduleAnimation() {
        this.mService.scheduleAnimationLocked();
    }
}
