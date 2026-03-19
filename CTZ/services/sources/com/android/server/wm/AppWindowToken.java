package com.android.server.wm;

import android.R;
import android.content.res.Configuration;
import android.graphics.GraphicBuffer;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Debug;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Trace;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayInfo;
import android.view.IApplicationToken;
import android.view.RemoteAnimationDefinition;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.animation.Animation;
import com.android.internal.util.ToBooleanFunction;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.input.InputApplicationHandle;
import com.android.server.pm.DumpState;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowState;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.wm.WmsExt;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AppWindowToken extends WindowToken implements WindowManagerService.AppFreezeListener {
    private static final String TAG = "WindowManager";
    private static final int Z_BOOST_BASE = 800570000;
    boolean allDrawn;
    final IApplicationToken appToken;
    boolean deferClearAllDrawn;
    boolean firstWindowDrawn;
    boolean hiddenRequested;
    boolean inPendingTransaction;
    public boolean isFullscreenOn;
    boolean layoutConfigChanges;
    private boolean mAlwaysFocusable;
    private AnimatingAppWindowTokenRegistry mAnimatingAppWindowTokenRegistry;
    boolean mAppStopped;
    private boolean mCanTurnScreenOn;
    private boolean mClientHidden;
    boolean mDeferHidingClient;
    private boolean mDisablePreviewScreenshots;
    boolean mEnteringAnimation;
    private boolean mFillsParent;
    private boolean mFreezingScreen;
    ArrayDeque<Rect> mFrozenBounds;
    ArrayDeque<Configuration> mFrozenMergedConfig;
    private boolean mHiddenSetFromTransferredStartingWindow;
    final InputApplicationHandle mInputApplicationHandle;
    long mInputDispatchingTimeoutNanos;
    boolean mIsExiting;
    private boolean mLastAllDrawn;
    private boolean mLastContainsDismissKeyguardWindow;
    private boolean mLastContainsShowWhenLockedWindow;
    private Task mLastParent;
    private boolean mLastSurfaceShowing;
    private long mLastTransactionSequence;
    boolean mLaunchTaskBehind;
    private Letterbox mLetterbox;
    private boolean mNeedsZBoost;
    private int mNumDrawnWindows;
    private int mNumInterestingWindows;
    private int mPendingRelaunchCount;
    private RemoteAnimationDefinition mRemoteAnimationDefinition;
    private boolean mRemovingFromDisplay;
    private boolean mReparenting;
    private final WindowState.UpdateReportedVisibilityResults mReportedVisibilityResults;
    int mRotationAnimationHint;
    boolean mShowForAllUsers;
    int mTargetSdk;
    private AppWindowThumbnail mThumbnail;
    private final Point mTmpPoint;
    private final Rect mTmpRect;
    private int mTransit;
    private int mTransitFlags;
    final boolean mVoiceInteraction;
    private boolean mWillCloseOrEnterPip;
    private WmsExt mWmsExt;
    boolean removed;
    private boolean reportedDrawn;
    boolean reportedVisible;
    StartingData startingData;
    boolean startingDisplayed;
    boolean startingMoved;
    WindowManagerPolicy.StartingSurface startingSurface;
    WindowState startingWindow;

    AppWindowToken(WindowManagerService windowManagerService, IApplicationToken iApplicationToken, boolean z, DisplayContent displayContent, long j, boolean z2, boolean z3, int i, int i2, int i3, int i4, boolean z4, boolean z5, AppWindowContainerController appWindowContainerController) {
        this(windowManagerService, iApplicationToken, z, displayContent, z2);
        setController(appWindowContainerController);
        this.mInputDispatchingTimeoutNanos = j;
        this.mShowForAllUsers = z3;
        this.mTargetSdk = i;
        this.mOrientation = i2;
        this.layoutConfigChanges = (i4 & 1152) != 0;
        this.mLaunchTaskBehind = z4;
        this.mAlwaysFocusable = z5;
        this.mRotationAnimationHint = i3;
        setHidden(true);
        this.hiddenRequested = true;
    }

    AppWindowToken(WindowManagerService windowManagerService, IApplicationToken iApplicationToken, boolean z, DisplayContent displayContent, boolean z2) {
        super(windowManagerService, iApplicationToken != null ? iApplicationToken.asBinder() : null, 2, true, displayContent, false);
        this.mRemovingFromDisplay = false;
        this.mLastTransactionSequence = Long.MIN_VALUE;
        this.mReportedVisibilityResults = new WindowState.UpdateReportedVisibilityResults();
        this.mFrozenBounds = new ArrayDeque<>();
        this.mFrozenMergedConfig = new ArrayDeque<>();
        this.mCanTurnScreenOn = true;
        this.mLastSurfaceShowing = true;
        this.mTmpPoint = new Point();
        this.mTmpRect = new Rect();
        this.isFullscreenOn = true;
        this.mWmsExt = MtkSystemServiceFactory.getInstance().makeWmsExt();
        this.appToken = iApplicationToken;
        this.mVoiceInteraction = z;
        this.mFillsParent = z2;
        this.mInputApplicationHandle = new InputApplicationHandle(this);
        if (this.mWmsExt.isFullscreenSwitchSupport()) {
            this.isFullscreenOn = this.mWmsExt.initFullscreenSwitchState(this.token);
        }
    }

    void onFirstWindowDrawn(WindowState windowState, WindowStateAnimator windowStateAnimator) {
        this.firstWindowDrawn = true;
        removeDeadWindows();
        if (this.startingWindow != null) {
            if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW || WindowManagerDebugConfig.DEBUG_ANIM) {
                Slog.v("WindowManager", "Finish starting " + windowState.mToken + ": first real window is shown, no animation");
            }
            windowState.cancelAnimation();
            if (getController() != null) {
                getController().removeStartingWindow();
            }
        }
        updateReportedVisibilityLocked();
    }

    void updateReportedVisibilityLocked() {
        if (this.appToken == null) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v("WindowManager", "Update reported visibility: " + this);
        }
        int size = this.mChildren.size();
        this.mReportedVisibilityResults.reset();
        boolean z = false;
        for (int i = 0; i < size; i++) {
            ((WindowState) this.mChildren.get(i)).updateReportedVisibility(this.mReportedVisibilityResults);
        }
        int i2 = this.mReportedVisibilityResults.numInteresting;
        int i3 = this.mReportedVisibilityResults.numVisible;
        int i4 = this.mReportedVisibilityResults.numDrawn;
        boolean z2 = this.mReportedVisibilityResults.nowGone;
        boolean z3 = i2 > 0 && i4 >= i2;
        if (i2 > 0 && i3 >= i2 && !isHidden()) {
            z = true;
        }
        if (!z2) {
            if (!z3) {
                z3 = this.reportedDrawn;
            }
            if (!z) {
                z = this.reportedVisible;
            }
        }
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v("WindowManager", "VIS " + this + ": interesting=" + i2 + " visible=" + i3);
        }
        AppWindowContainerController controller = getController();
        if (z3 != this.reportedDrawn) {
            if (z3 && controller != null) {
                controller.reportWindowsDrawn();
            }
            this.reportedDrawn = z3;
        }
        if (z != this.reportedVisible) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v("WindowManager", "Visibility changed in " + this + ": vis=" + z);
            }
            this.reportedVisible = z;
            if (controller != null) {
                if (z) {
                    controller.reportWindowsVisible();
                } else {
                    controller.reportWindowsGone();
                }
            }
        }
    }

    boolean isClientHidden() {
        return this.mClientHidden;
    }

    void setClientHidden(boolean z) {
        if (this.mClientHidden != z) {
            if (z && this.mDeferHidingClient) {
                return;
            }
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v("WindowManager", "setClientHidden: " + this + " clientHidden=" + z + " Callers=" + Debug.getCallers(5));
            }
            this.mClientHidden = z;
            sendAppVisibilityToClients();
        }
    }

    boolean setVisibility(WindowManager.LayoutParams layoutParams, boolean z, int i, boolean z2, boolean z3) {
        boolean z4;
        boolean z5;
        boolean z6;
        boolean z7;
        boolean z8 = false;
        this.inPendingTransaction = false;
        this.mHiddenSetFromTransferredStartingWindow = false;
        if (isHidden() == z || ((isHidden() && this.mIsExiting) || (z && waitingForReplacement()))) {
            AccessibilityController accessibilityController = this.mService.mAccessibilityController;
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v("WindowManager", "Changing app " + this + " hidden=" + isHidden() + " performLayout=" + z2);
            }
            if (i == -1) {
                z4 = false;
                z5 = false;
                z6 = false;
            } else {
                if (!applyAnimationLocked(layoutParams, i, z, z3)) {
                    z7 = false;
                } else {
                    z7 = true;
                }
                z6 = z7;
                WindowState windowStateFindMainWindow = findMainWindow();
                if (windowStateFindMainWindow != null && accessibilityController != null && getDisplayContent().getDisplayId() == 0) {
                    accessibilityController.onAppWindowTransitionLocked(windowStateFindMainWindow, i);
                }
                z5 = z7;
                z4 = true;
            }
            int size = this.mChildren.size();
            boolean zOnAppVisibilityChanged = z4;
            for (int i2 = 0; i2 < size; i2++) {
                zOnAppVisibilityChanged |= ((WindowState) this.mChildren.get(i2)).onAppVisibilityChanged(z, z5);
            }
            setHidden(!z);
            this.hiddenRequested = !z;
            if (!z) {
                stopFreezingScreen(true, true);
            } else {
                if (this.startingWindow != null && !this.startingWindow.isDrawnLw()) {
                    this.startingWindow.mPolicyVisibility = false;
                    this.startingWindow.mPolicyVisibilityAfterAnim = false;
                }
                final WindowManagerService windowManagerService = this.mService;
                Objects.requireNonNull(windowManagerService);
                forAllWindows(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        windowManagerService.makeWindowFreezingScreenIfNeededLocked((WindowState) obj);
                    }
                }, true);
            }
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v("WindowManager", "setVisibility: " + this + ": hidden=" + isHidden() + " hiddenRequested=" + this.hiddenRequested);
            }
            if (zOnAppVisibilityChanged) {
                this.mService.mInputMonitor.setUpdateInputWindowsNeededLw();
                if (z2) {
                    this.mService.updateFocusedWindowLocked(3, false);
                    this.mService.mWindowPlacerLocked.performSurfacePlacement();
                }
                this.mService.mInputMonitor.updateInputWindowsLw(false);
            }
            z8 = true;
        } else {
            z6 = false;
        }
        if (!isReallyAnimating()) {
            onAnimationFinished();
        } else {
            z6 = true;
        }
        for (int size2 = this.mChildren.size() - 1; size2 >= 0 && !z6; size2--) {
            if (((WindowState) this.mChildren.get(size2)).isSelfOrChildAnimating()) {
                z6 = true;
            }
        }
        if (z8) {
            if (z && !z6) {
                this.mEnteringAnimation = true;
                this.mService.mActivityManagerAppTransitionNotifier.onAppTransitionFinishedLocked(this.token);
            }
            if (z || !isReallyAnimating()) {
                setClientHidden(!z);
            }
            if (!this.mService.mClosingApps.contains(this) && !this.mService.mOpeningApps.contains(this)) {
                this.mService.getDefaultDisplayContentLocked().getDockedDividerController().notifyAppVisibilityChanged();
                this.mService.mTaskSnapshotController.notifyAppVisibilityChanged(this, z);
            }
            if (isHidden() && !z6 && !this.mService.mAppTransition.isTransitionSet()) {
                SurfaceControl.openTransaction();
                for (int size3 = this.mChildren.size() - 1; size3 >= 0; size3--) {
                    ((WindowState) this.mChildren.get(size3)).mWinAnimator.hide("immediately hidden");
                }
                SurfaceControl.closeTransaction();
            }
        }
        return z6;
    }

    WindowState getTopFullscreenWindow() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            WindowState windowState = (WindowState) this.mChildren.get(size);
            if (windowState != null && windowState.mAttrs.isFullscreen()) {
                return windowState;
            }
        }
        return null;
    }

    WindowState findMainWindow() {
        return findMainWindow(true);
    }

    WindowState findMainWindow(boolean z) {
        WindowState windowState = null;
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            WindowState windowState2 = (WindowState) this.mChildren.get(size);
            int i = windowState2.mAttrs.type;
            if (i == 1 || (z && i == 3)) {
                if (windowState2.mAnimatingExit) {
                    windowState = windowState2;
                } else {
                    return windowState2;
                }
            }
        }
        return windowState;
    }

    boolean windowsAreFocusable() {
        return getWindowConfiguration().canReceiveKeys() || this.mAlwaysFocusable;
    }

    @Override
    AppWindowContainerController getController() {
        WindowContainerController controller = super.getController();
        if (controller != null) {
            return (AppWindowContainerController) controller;
        }
        return null;
    }

    @Override
    boolean isVisible() {
        return !isHidden();
    }

    @Override
    void removeImmediately() {
        onRemovedFromDisplay();
        super.removeImmediately();
    }

    @Override
    void removeIfPossible() {
        this.mIsExiting = false;
        removeAllWindowsIfPossible();
        removeImmediately();
    }

    @Override
    boolean checkCompleteDeferredRemoval() {
        if (this.mIsExiting) {
            removeIfPossible();
        }
        return super.checkCompleteDeferredRemoval();
    }

    void onRemovedFromDisplay() {
        if (this.mRemovingFromDisplay) {
            return;
        }
        this.mRemovingFromDisplay = true;
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
            Slog.v("WindowManager", "Removing app token: " + this);
        }
        boolean visibility = setVisibility(null, false, -1, true, this.mVoiceInteraction);
        this.mService.mOpeningApps.remove(this);
        this.mService.mUnknownAppVisibilityController.appRemovedOrHidden(this);
        this.mService.mTaskSnapshotController.onAppRemoved(this);
        this.waitingToShow = false;
        if (!this.mService.mClosingApps.contains(this)) {
            if (this.mService.mAppTransition.isTransitionSet()) {
                this.mService.mClosingApps.add(this);
                visibility = true;
            }
        } else {
            visibility = true;
        }
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
            Slog.v("WindowManager", "Removing app " + this + " delayed=" + visibility + " animation=" + getAnimation() + " animating=" + isSelfAnimating());
        }
        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE || WindowManagerDebugConfig.DEBUG_TOKEN_MOVEMENT) {
            Slog.v("WindowManager", "removeAppToken: " + this + " delayed=" + visibility + " Callers=" + Debug.getCallers(4));
        }
        if (this.startingData != null && getController() != null) {
            getController().removeStartingWindow();
        }
        if (isSelfAnimating()) {
            this.mService.mNoAnimationNotifyOnTransitionFinished.add(this.token);
        }
        TaskStack stack = getStack();
        if (visibility && !isEmpty()) {
            if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE || WindowManagerDebugConfig.DEBUG_TOKEN_MOVEMENT) {
                Slog.v("WindowManager", "removeAppToken make exiting: " + this);
            }
            if (stack != null) {
                stack.mExitingAppTokens.add(this);
            }
            this.mIsExiting = true;
        } else {
            cancelAnimation();
            if (stack != null) {
                stack.mExitingAppTokens.remove(this);
            }
            removeIfPossible();
        }
        this.removed = true;
        stopFreezingScreen(true, true);
        if (this.mService.mFocusedApp == this) {
            if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                Slog.v("WindowManager", "Removing focused app token:" + this);
            }
            this.mService.mFocusedApp = null;
            this.mService.updateFocusedWindowLocked(0, true);
            this.mService.mInputMonitor.setFocusedAppLw(null);
        }
        if (!visibility) {
            updateReportedVisibilityLocked();
        }
        this.mRemovingFromDisplay = false;
    }

    void clearAnimatingFlags() {
        boolean zClearAnimatingFlags = false;
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            zClearAnimatingFlags |= ((WindowState) this.mChildren.get(size)).clearAnimatingFlags();
        }
        if (zClearAnimatingFlags) {
            requestUpdateWallpaperIfNeeded();
        }
    }

    void destroySurfaces() {
        destroySurfaces(false);
    }

    private void destroySurfaces(boolean z) {
        ArrayList arrayList = new ArrayList(this.mChildren);
        boolean zDestroySurface = false;
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            zDestroySurface |= ((WindowState) arrayList.get(size)).destroySurface(z, this.mAppStopped);
        }
        if (zDestroySurface) {
            getDisplayContent().assignWindowLayers(true);
            updateLetterboxSurface(null);
        }
    }

    void notifyAppResumed(boolean z) {
        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.v("WindowManager", "notifyAppResumed: wasStopped=" + z + " " + this);
        }
        this.mAppStopped = false;
        setCanTurnScreenOn(true);
        if (!z) {
            destroySurfaces(true);
        }
    }

    void notifyAppStopped() {
        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.v("WindowManager", "notifyAppStopped: " + this);
        }
        this.mAppStopped = true;
        destroySurfaces();
        if (getController() != null) {
            getController().removeStartingWindow();
        }
    }

    void clearAllDrawn() {
        this.allDrawn = false;
        this.deferClearAllDrawn = false;
    }

    public Task getTask() {
        return (Task) getParent();
    }

    TaskStack getStack() {
        Task task = getTask();
        if (task != null) {
            return task.mStack;
        }
        return null;
    }

    @Override
    void onParentSet() {
        AnimatingAppWindowTokenRegistry animatingAppWindowTokenRegistry;
        super.onParentSet();
        Task task = getTask();
        if (!this.mReparenting) {
            if (task == null) {
                this.mService.mClosingApps.remove(this);
            } else if (this.mLastParent != null && this.mLastParent.mStack != null) {
                task.mStack.mExitingAppTokens.remove(this);
            }
        }
        TaskStack stack = getStack();
        if (this.mAnimatingAppWindowTokenRegistry != null) {
            this.mAnimatingAppWindowTokenRegistry.notifyFinished(this);
        }
        if (stack != null) {
            animatingAppWindowTokenRegistry = stack.getAnimatingAppWindowTokenRegistry();
        } else {
            animatingAppWindowTokenRegistry = null;
        }
        this.mAnimatingAppWindowTokenRegistry = animatingAppWindowTokenRegistry;
        this.mLastParent = task;
    }

    void postWindowRemoveStartingWindowCleanup(WindowState windowState) {
        if (this.startingWindow == windowState) {
            if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                Slog.v("WindowManager", "Notify removed startingWindow " + windowState);
            }
            if (getController() != null) {
                getController().removeStartingWindow();
                return;
            }
            return;
        }
        if (this.mChildren.size() == 0) {
            if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                Slog.v("WindowManager", "Nulling last startingData");
            }
            this.startingData = null;
            if (this.mHiddenSetFromTransferredStartingWindow) {
                setHidden(true);
                return;
            }
            return;
        }
        if (this.mChildren.size() == 1 && this.startingSurface != null && !isRelaunching()) {
            if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                Slog.v("WindowManager", "Last window, removing starting window " + windowState);
            }
            if (getController() != null) {
                getController().removeStartingWindow();
            }
        }
    }

    void removeDeadWindows() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            WindowState windowState = (WindowState) this.mChildren.get(size);
            if (windowState.mAppDied) {
                if (WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT || WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                    Slog.w("WindowManager", "removeDeadWindows: " + windowState);
                }
                windowState.mDestroying = true;
                windowState.removeIfPossible();
            }
        }
    }

    boolean hasWindowsAlive() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            if (!((WindowState) this.mChildren.get(size)).mAppDied) {
                return true;
            }
        }
        return false;
    }

    void setWillReplaceWindows(boolean z) {
        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.d("WindowManager", "Marking app token " + this + " with replacing windows.");
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((WindowState) this.mChildren.get(size)).setWillReplaceWindow(z);
        }
    }

    void setWillReplaceChildWindows() {
        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.d("WindowManager", "Marking app token " + this + " with replacing child windows.");
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((WindowState) this.mChildren.get(size)).setWillReplaceChildWindows();
        }
    }

    void clearWillReplaceWindows() {
        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.d("WindowManager", "Resetting app token " + this + " of replacing window marks.");
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((WindowState) this.mChildren.get(size)).clearWillReplaceWindow();
        }
    }

    void requestUpdateWallpaperIfNeeded() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((WindowState) this.mChildren.get(size)).requestUpdateWallpaperIfNeeded();
        }
    }

    boolean isRelaunching() {
        return this.mPendingRelaunchCount > 0;
    }

    boolean shouldFreezeBounds() {
        Task task = getTask();
        if (task == null || task.inFreeformWindowingMode()) {
            return false;
        }
        return getTask().isDragResizing();
    }

    void startRelaunching() {
        if (shouldFreezeBounds()) {
            freezeBounds();
        }
        detachChildren();
        this.mPendingRelaunchCount++;
    }

    void detachChildren() {
        SurfaceControl.openTransaction();
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((WindowState) this.mChildren.get(size)).mWinAnimator.detachChildren();
        }
        SurfaceControl.closeTransaction();
    }

    void finishRelaunching() {
        unfreezeBounds();
        if (this.mPendingRelaunchCount > 0) {
            this.mPendingRelaunchCount--;
        } else {
            checkKeyguardFlagsChanged();
        }
    }

    void clearRelaunching() {
        if (this.mPendingRelaunchCount == 0) {
            return;
        }
        unfreezeBounds();
        this.mPendingRelaunchCount = 0;
    }

    @Override
    protected boolean isFirstChildWindowGreaterThanSecond(WindowState windowState, WindowState windowState2) {
        int i = windowState.mAttrs.type;
        int i2 = windowState2.mAttrs.type;
        if (i == 1 && i2 != 1) {
            return false;
        }
        if (i == 1 || i2 != 1) {
            return (i == 3 && i2 != 3) || i == 3 || i2 != 3;
        }
        return true;
    }

    @Override
    void addWindow(WindowState windowState) {
        super.addWindow(windowState);
        boolean replacementWindowIfNeeded = false;
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            replacementWindowIfNeeded |= ((WindowState) this.mChildren.get(size)).setReplacementWindowIfNeeded(windowState);
        }
        if (replacementWindowIfNeeded) {
            this.mService.scheduleWindowReplacementTimeouts(this);
        }
        checkKeyguardFlagsChanged();
    }

    @Override
    void removeChild(WindowState windowState) {
        super.removeChild(windowState);
        checkKeyguardFlagsChanged();
        updateLetterboxSurface(windowState);
    }

    private boolean waitingForReplacement() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            if (((WindowState) this.mChildren.get(size)).waitingForReplacement()) {
                return true;
            }
        }
        return false;
    }

    void onWindowReplacementTimeout() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((WindowState) this.mChildren.get(size)).onWindowReplacementTimeout();
        }
    }

    void reparent(Task task, int i) {
        Task task2 = getTask();
        if (task == task2) {
            throw new IllegalArgumentException("window token=" + this + " already child of task=" + task2);
        }
        if (task2.mStack != task.mStack) {
            throw new IllegalArgumentException("window token=" + this + " current task=" + task2 + " belongs to a different stack than " + task);
        }
        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.i("WindowManager", "reParentWindowToken: removing window token=" + this + " from task=" + task2);
        }
        DisplayContent displayContent = getDisplayContent();
        this.mReparenting = true;
        getParent().removeChild(this);
        task.addChild(this, i);
        this.mReparenting = false;
        DisplayContent displayContent2 = task.getDisplayContent();
        displayContent2.setLayoutNeeded();
        if (displayContent != displayContent2) {
            onDisplayChanged(displayContent2);
            displayContent.setLayoutNeeded();
        }
    }

    private void freezeBounds() {
        Task task = getTask();
        this.mFrozenBounds.offer(new Rect(task.mPreparedFrozenBounds));
        if (task.mPreparedFrozenMergedConfig.equals(Configuration.EMPTY)) {
            this.mFrozenMergedConfig.offer(new Configuration(task.getConfiguration()));
        } else {
            this.mFrozenMergedConfig.offer(new Configuration(task.mPreparedFrozenMergedConfig));
        }
        task.mPreparedFrozenMergedConfig.unset();
    }

    private void unfreezeBounds() {
        if (this.mFrozenBounds.isEmpty()) {
            return;
        }
        this.mFrozenBounds.remove();
        if (!this.mFrozenMergedConfig.isEmpty()) {
            this.mFrozenMergedConfig.remove();
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((WindowState) this.mChildren.get(size)).onUnfreezeBounds();
        }
        this.mService.mWindowPlacerLocked.performSurfacePlacement();
    }

    void setAppLayoutChanges(int i, String str) {
        if (!this.mChildren.isEmpty()) {
            DisplayContent displayContent = getDisplayContent();
            displayContent.pendingLayoutChanges = i | displayContent.pendingLayoutChanges;
            if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                this.mService.mWindowPlacerLocked.debugLayoutRepeats(str, displayContent.pendingLayoutChanges);
            }
        }
    }

    void removeReplacedWindowIfNeeded(WindowState windowState) {
        for (int size = this.mChildren.size() - 1; size >= 0 && !((WindowState) this.mChildren.get(size)).removeReplacedWindowIfNeeded(windowState); size--) {
        }
    }

    void startFreezingScreen() {
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            WindowManagerService.logWithStack("WindowManager", "Set freezing of " + this.appToken + ": hidden=" + isHidden() + " freezing=" + this.mFreezingScreen + " hiddenRequested=" + this.hiddenRequested);
        }
        if (!this.hiddenRequested) {
            if (!this.mFreezingScreen) {
                this.mFreezingScreen = true;
                this.mService.registerAppFreezeListener(this);
                this.mService.mAppsFreezingScreen++;
                if (this.mService.mAppsFreezingScreen == 1) {
                    this.mService.startFreezingDisplayLocked(0, 0, getDisplayContent());
                    this.mService.mH.removeMessages(17);
                    this.mService.mH.sendEmptyMessageDelayed(17, 2000L);
                }
            }
            int size = this.mChildren.size();
            for (int i = 0; i < size; i++) {
                ((WindowState) this.mChildren.get(i)).onStartFreezingScreen();
            }
        }
    }

    void stopFreezingScreen(boolean z, boolean z2) {
        if (!this.mFreezingScreen) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.v("WindowManager", "Clear freezing of " + this + " force=" + z2);
        }
        int size = this.mChildren.size();
        boolean zOnStopFreezingScreen = false;
        for (int i = 0; i < size; i++) {
            zOnStopFreezingScreen |= ((WindowState) this.mChildren.get(i)).onStopFreezingScreen();
        }
        if (z2 || zOnStopFreezingScreen) {
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.v("WindowManager", "No longer freezing: " + this);
            }
            this.mFreezingScreen = false;
            this.mService.unregisterAppFreezeListener(this);
            WindowManagerService windowManagerService = this.mService;
            windowManagerService.mAppsFreezingScreen--;
            this.mService.mLastFinishedFreezeSource = this;
        }
        if (z) {
            if (zOnStopFreezingScreen) {
                this.mService.mWindowPlacerLocked.performSurfacePlacement();
            }
            this.mService.stopFreezingDisplayLocked();
        }
    }

    @Override
    public void onAppFreezeTimeout() {
        Slog.w("WindowManager", "Force clearing freeze: " + this);
        stopFreezingScreen(true, true);
    }

    void transferStartingWindowFromHiddenAboveTokenIfNeeded() {
        Task task = getTask();
        for (int size = task.mChildren.size() - 1; size >= 0; size--) {
            AppWindowToken appWindowToken = (AppWindowToken) task.mChildren.get(size);
            if (appWindowToken == this) {
                return;
            }
            if (appWindowToken.hiddenRequested && transferStartingWindow(appWindowToken.token)) {
                return;
            }
        }
    }

    boolean transferStartingWindow(IBinder iBinder) {
        AppWindowToken appWindowToken = getDisplayContent().getAppWindowToken(iBinder);
        if (appWindowToken == null) {
            return false;
        }
        if ((this.startingSurface != null || this.startingData != null) && appWindowToken.getController() != null) {
            Slog.v("WindowManager", "transferStartingWindow, fromToken already add a starting window.");
            appWindowToken.getController().removeStartingWindow();
        }
        WindowState windowState = appWindowToken.startingWindow;
        if (windowState != null && appWindowToken.startingSurface != null) {
            this.mService.mSkipAppTransitionAnimation = true;
            if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                Slog.v("WindowManager", "Moving existing starting " + windowState + " from " + appWindowToken + " to " + this);
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.startingData = appWindowToken.startingData;
                this.startingSurface = appWindowToken.startingSurface;
                this.startingDisplayed = appWindowToken.startingDisplayed;
                appWindowToken.startingDisplayed = false;
                this.startingWindow = windowState;
                this.reportedVisible = appWindowToken.reportedVisible;
                appWindowToken.startingData = null;
                appWindowToken.startingSurface = null;
                appWindowToken.startingWindow = null;
                appWindowToken.startingMoved = true;
                windowState.mToken = this;
                windowState.mAppToken = this;
                if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE || WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                    Slog.v("WindowManager", "Removing starting " + windowState + " from " + appWindowToken);
                }
                appWindowToken.removeChild(windowState);
                appWindowToken.postWindowRemoveStartingWindowCleanup(windowState);
                appWindowToken.mHiddenSetFromTransferredStartingWindow = false;
                addWindow(windowState);
                if (appWindowToken.allDrawn) {
                    this.allDrawn = true;
                    this.deferClearAllDrawn = appWindowToken.deferClearAllDrawn;
                }
                if (appWindowToken.firstWindowDrawn) {
                    this.firstWindowDrawn = true;
                }
                if (!appWindowToken.isHidden()) {
                    setHidden(false);
                    this.hiddenRequested = false;
                    this.mHiddenSetFromTransferredStartingWindow = true;
                }
                setClientHidden(appWindowToken.mClientHidden);
                transferAnimation(appWindowToken);
                this.mService.mOpeningApps.remove(this);
                this.mService.updateFocusedWindowLocked(3, true);
                getDisplayContent().setLayoutNeeded();
                this.mService.mWindowPlacerLocked.performSurfacePlacement();
                return true;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        if (appWindowToken.startingData == null) {
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
            Slog.v("WindowManager", "Moving pending starting from " + appWindowToken + " to " + this);
        }
        this.startingData = appWindowToken.startingData;
        appWindowToken.startingData = null;
        appWindowToken.startingMoved = true;
        if (getController() != null) {
            getController().scheduleAddStartingWindow();
        }
        return true;
    }

    boolean isLastWindow(WindowState windowState) {
        return this.mChildren.size() == 1 && this.mChildren.get(0) == windowState;
    }

    @Override
    void onAppTransitionDone() {
        this.sendingToBottom = false;
    }

    @Override
    int getOrientation(int i) {
        if (i == 3) {
            return this.mOrientation;
        }
        if (!this.sendingToBottom && !this.mService.mClosingApps.contains(this)) {
            if (isVisible() || this.mService.mOpeningApps.contains(this)) {
                return this.mOrientation;
            }
            return -2;
        }
        return -2;
    }

    int getOrientationIgnoreVisibility() {
        return this.mOrientation;
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        TaskStack pinnedStack;
        Rect rect;
        int windowingMode = getWindowingMode();
        super.onConfigurationChanged(configuration);
        int windowingMode2 = getWindowingMode();
        if (windowingMode == windowingMode2) {
            return;
        }
        if (windowingMode != 0 && windowingMode2 == 2) {
            this.mDisplayContent.mPinnedStackControllerLocked.resetReentrySnapFraction(this);
            return;
        }
        if (windowingMode == 2 && windowingMode2 != 0 && !isHidden() && (pinnedStack = this.mDisplayContent.getPinnedStack()) != null) {
            if (pinnedStack.lastAnimatingBoundsWasToFullscreen()) {
                rect = pinnedStack.mPreAnimationBounds;
            } else {
                Rect rect2 = this.mTmpRect;
                pinnedStack.getBounds(rect2);
                rect = rect2;
            }
            this.mDisplayContent.mPinnedStackControllerLocked.saveReentrySnapFraction(this, rect);
        }
    }

    @Override
    void checkAppWindowsReadyToShow() {
        if (this.allDrawn == this.mLastAllDrawn) {
            return;
        }
        this.mLastAllDrawn = this.allDrawn;
        if (!this.allDrawn) {
            return;
        }
        if (this.mFreezingScreen) {
            showAllWindowsLocked();
            stopFreezingScreen(false, true);
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.i("WindowManager", "Setting mOrientationChangeComplete=true because wtoken " + this + " numInteresting=" + this.mNumInterestingWindows + " numDrawn=" + this.mNumDrawnWindows);
            }
            setAppLayoutChanges(4, "checkAppWindowsReadyToShow: freezingScreen");
            return;
        }
        setAppLayoutChanges(8, "checkAppWindowsReadyToShow");
        if (!this.mService.mOpeningApps.contains(this)) {
            showAllWindowsLocked();
        }
    }

    private boolean allDrawnStatesConsidered() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            WindowState windowState = (WindowState) this.mChildren.get(size);
            if (windowState.mightAffectAllDrawn() && !windowState.getDrawnStateEvaluated()) {
                return false;
            }
        }
        return true;
    }

    void updateAllDrawn() {
        int i;
        if (!this.allDrawn && (i = this.mNumInterestingWindows) > 0 && allDrawnStatesConsidered() && this.mNumDrawnWindows >= i && !isRelaunching()) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v("WindowManager", "allDrawn: " + this + " interesting=" + i + " drawn=" + this.mNumDrawnWindows);
            }
            this.allDrawn = true;
            if (this.mDisplayContent != null) {
                this.mDisplayContent.setLayoutNeeded();
            }
            this.mService.mH.obtainMessage(32, this.token).sendToTarget();
            TaskStack pinnedStack = this.mDisplayContent.getPinnedStack();
            if (pinnedStack != null) {
                pinnedStack.onAllWindowsDrawn();
            }
        }
    }

    boolean updateDrawnWindowStates(WindowState windowState) {
        windowState.setDrawnStateEvaluated(true);
        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW_VERBOSE && windowState == this.startingWindow) {
            Slog.d("WindowManager", "updateWindows: starting " + windowState + " isOnScreen=" + windowState.isOnScreen() + " allDrawn=" + this.allDrawn + " freezingScreen=" + this.mFreezingScreen);
        }
        if (this.allDrawn && !this.mFreezingScreen) {
            return false;
        }
        if (this.mLastTransactionSequence != this.mService.mTransactionSequence) {
            this.mLastTransactionSequence = this.mService.mTransactionSequence;
            this.mNumDrawnWindows = 0;
            this.startingDisplayed = false;
            this.mNumInterestingWindows = findMainWindow(false) != null ? 1 : 0;
        }
        WindowStateAnimator windowStateAnimator = windowState.mWinAnimator;
        if (!this.allDrawn && windowState.mightAffectAllDrawn()) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY || WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.v("WindowManager", "Eval win " + windowState + ": isDrawn=" + windowState.isDrawnLw() + ", isAnimationSet=" + isSelfAnimating());
                if (!windowState.isDrawnLw()) {
                    Slog.v("WindowManager", "Not displayed: s=" + windowStateAnimator.mSurfaceController + " pv=" + windowState.mPolicyVisibility + " mDrawState=" + windowStateAnimator.drawStateToString() + " ph=" + windowState.isParentWindowHidden() + " th=" + this.hiddenRequested + " a=" + isSelfAnimating());
                }
            }
            if (windowState != this.startingWindow) {
                if (windowState.isInteresting()) {
                    if (findMainWindow(false) != windowState) {
                        this.mNumInterestingWindows++;
                    }
                    if (windowState.isDrawnLw()) {
                        this.mNumDrawnWindows++;
                        if (WindowManagerDebugConfig.DEBUG_VISIBILITY || WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                            Slog.v("WindowManager", "tokenMayBeDrawn: " + this + " w=" + windowState + " numInteresting=" + this.mNumInterestingWindows + " freezingScreen=" + this.mFreezingScreen + " mAppFreezing=" + windowState.mAppFreezing);
                            return true;
                        }
                        return true;
                    }
                }
            } else if (windowState.isDrawnLw()) {
                if (getController() != null) {
                    getController().reportStartingWindowDrawn();
                }
                this.startingDisplayed = true;
            }
        }
        return false;
    }

    void layoutLetterbox(WindowState windowState) {
        WindowState windowStateFindMainWindow = findMainWindow();
        if (windowStateFindMainWindow != null) {
            if (windowState == null || windowStateFindMainWindow == windowState) {
                if (windowStateFindMainWindow.isLetterboxedAppWindow() && fillsParent() && (windowStateFindMainWindow.isDrawnLw() || windowStateFindMainWindow.mWinAnimator.mSurfaceDestroyDeferred || windowStateFindMainWindow.isDragResizeChanged())) {
                    if (this.mLetterbox == null) {
                        this.mLetterbox = new Letterbox(new Supplier() {
                            @Override
                            public final Object get() {
                                return this.f$0.makeChildSurface(null);
                            }
                        });
                    }
                    this.mLetterbox.layout(getParent().getBounds(), windowStateFindMainWindow.mFrame);
                } else if (this.mLetterbox != null) {
                    this.mLetterbox.hide();
                }
            }
        }
    }

    void updateLetterboxSurface(WindowState windowState) {
        WindowState windowStateFindMainWindow = findMainWindow();
        if (windowStateFindMainWindow != windowState && windowState != null && windowStateFindMainWindow != null) {
            return;
        }
        layoutLetterbox(windowState);
        if (this.mLetterbox != null && this.mLetterbox.needsApplySurfaceChanges()) {
            this.mLetterbox.applySurfaceChanges(this.mPendingTransaction);
        }
    }

    @Override
    boolean forAllWindows(ToBooleanFunction<WindowState> toBooleanFunction, boolean z) {
        if (this.mIsExiting && !waitingForReplacement()) {
            return false;
        }
        return forAllWindowsUnchecked(toBooleanFunction, z);
    }

    boolean forAllWindowsUnchecked(ToBooleanFunction<WindowState> toBooleanFunction, boolean z) {
        return super.forAllWindows(toBooleanFunction, z);
    }

    @Override
    AppWindowToken asAppWindowToken() {
        return this;
    }

    @Override
    boolean fillsParent() {
        return this.mFillsParent;
    }

    void setFillsParent(boolean z) {
        this.mFillsParent = z;
    }

    boolean containsDismissKeyguardWindow() {
        if (isRelaunching()) {
            return this.mLastContainsDismissKeyguardWindow;
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            if ((((WindowState) this.mChildren.get(size)).mAttrs.flags & DumpState.DUMP_CHANGES) != 0) {
                return true;
            }
        }
        return false;
    }

    boolean containsShowWhenLockedWindow() {
        if (isRelaunching()) {
            return this.mLastContainsShowWhenLockedWindow;
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            if ((((WindowState) this.mChildren.get(size)).mAttrs.flags & DumpState.DUMP_FROZEN) != 0) {
                return true;
            }
        }
        return false;
    }

    void checkKeyguardFlagsChanged() {
        boolean zContainsDismissKeyguardWindow = containsDismissKeyguardWindow();
        boolean zContainsShowWhenLockedWindow = containsShowWhenLockedWindow();
        if (zContainsDismissKeyguardWindow != this.mLastContainsDismissKeyguardWindow || zContainsShowWhenLockedWindow != this.mLastContainsShowWhenLockedWindow) {
            this.mService.notifyKeyguardFlagsChanged(null);
        }
        this.mLastContainsDismissKeyguardWindow = zContainsDismissKeyguardWindow;
        this.mLastContainsShowWhenLockedWindow = zContainsShowWhenLockedWindow;
    }

    WindowState getImeTargetBelowWindow(WindowState windowState) {
        int iIndexOf = this.mChildren.indexOf(windowState);
        if (iIndexOf > 0) {
            WindowState windowState2 = (WindowState) this.mChildren.get(iIndexOf - 1);
            if (windowState2.canBeImeTarget()) {
                return windowState2;
            }
            return null;
        }
        return null;
    }

    int getLowestAnimLayer() {
        for (int i = 0; i < this.mChildren.size(); i++) {
            WindowState windowState = (WindowState) this.mChildren.get(i);
            if (!windowState.mRemoved) {
                return windowState.mWinAnimator.mAnimLayer;
            }
        }
        return Integer.MAX_VALUE;
    }

    WindowState getHighestAnimLayerWindow(WindowState windowState) {
        WindowState windowState2 = null;
        for (int iIndexOf = this.mChildren.indexOf(windowState); iIndexOf >= 0; iIndexOf--) {
            WindowState windowState3 = (WindowState) this.mChildren.get(iIndexOf);
            if (!windowState3.mRemoved && (windowState2 == null || windowState3.mWinAnimator.mAnimLayer > windowState2.mWinAnimator.mAnimLayer)) {
                windowState2 = windowState3;
            }
        }
        return windowState2;
    }

    void setDisablePreviewScreenshots(boolean z) {
        this.mDisablePreviewScreenshots = z;
    }

    void setCanTurnScreenOn(boolean z) {
        this.mCanTurnScreenOn = z;
    }

    boolean canTurnScreenOn() {
        return this.mCanTurnScreenOn;
    }

    static boolean lambda$shouldUseAppThemeSnapshot$1(WindowState windowState) {
        return (windowState.mAttrs.flags & 8192) != 0;
    }

    boolean shouldUseAppThemeSnapshot() {
        return this.mDisablePreviewScreenshots || forAllWindows((ToBooleanFunction<WindowState>) new ToBooleanFunction() {
            public final boolean apply(Object obj) {
                return AppWindowToken.lambda$shouldUseAppThemeSnapshot$1((WindowState) obj);
            }
        }, true);
    }

    SurfaceControl getAppAnimationLayer() {
        int i;
        if (isActivityTypeHome()) {
            i = 2;
        } else {
            i = needsZBoost() ? 1 : 0;
        }
        return getAppAnimationLayer(i);
    }

    @Override
    public SurfaceControl getAnimationLeashParent() {
        if (!inPinnedWindowingMode()) {
            return getAppAnimationLayer();
        }
        return getStack().getSurfaceControl();
    }

    private boolean shouldAnimate(int i) {
        return !(getWindowingMode() == 3) || (i != 13);
    }

    boolean applyAnimationLocked(WindowManager.LayoutParams layoutParams, int i, boolean z, boolean z2) {
        AnimationAdapter animationAdapterCreateAnimationAdapter;
        if (this.mService.mDisableTransitionAnimation || !shouldAnimate(i)) {
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_ANIM) {
                Slog.v("WindowManager", "applyAnimation: transition animation is disabled or skipped. atoken=" + this);
            }
            cancelAnimation();
            return false;
        }
        Trace.traceBegin(32L, "AWT#applyAnimationLocked");
        if (okToAnimate()) {
            TaskStack stack = getStack();
            this.mTmpPoint.set(0, 0);
            this.mTmpRect.setEmpty();
            if (stack != null) {
                stack.getRelativePosition(this.mTmpPoint);
                stack.getBounds(this.mTmpRect);
                this.mTmpRect.offsetTo(0, 0);
            }
            if (this.mService.mAppTransition.getRemoteAnimationController() != null && !this.mSurfaceAnimator.isAnimationStartDelayed()) {
                animationAdapterCreateAnimationAdapter = this.mService.mAppTransition.getRemoteAnimationController().createAnimationAdapter(this, this.mTmpPoint, this.mTmpRect);
            } else {
                Animation animationLoadAnimation = loadAnimation(layoutParams, i, z, z2);
                if (animationLoadAnimation != null) {
                    LocalAnimationAdapter localAnimationAdapter = new LocalAnimationAdapter(new WindowAnimationSpec(animationLoadAnimation, this.mTmpPoint, this.mTmpRect, this.mService.mAppTransition.canSkipFirstFrame(), this.mService.mAppTransition.getAppStackClipMode(), true), this.mService.mSurfaceAnimationRunner);
                    if (animationLoadAnimation.getZAdjustment() == 1) {
                        this.mNeedsZBoost = true;
                    }
                    this.mTransit = i;
                    this.mTransitFlags = this.mService.mAppTransition.getTransitFlags();
                    animationAdapterCreateAnimationAdapter = localAnimationAdapter;
                } else {
                    animationAdapterCreateAnimationAdapter = null;
                }
            }
            if (animationAdapterCreateAnimationAdapter != null) {
                startAnimation(getPendingTransaction(), animationAdapterCreateAnimationAdapter, !isVisible());
                if (animationAdapterCreateAnimationAdapter.getShowWallpaper()) {
                    this.mDisplayContent.pendingLayoutChanges |= 4;
                }
            }
        } else {
            cancelAnimation();
        }
        Trace.traceEnd(32L);
        return isReallyAnimating();
    }

    private Animation loadAnimation(WindowManager.LayoutParams layoutParams, int i, boolean z, boolean z2) {
        DisplayContent displayContent = getTask().getDisplayContent();
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        int i2 = displayInfo.appWidth;
        int i3 = displayInfo.appHeight;
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.v("WindowManager", "applyAnimation: atoken=" + this);
        }
        WindowState windowStateFindMainWindow = findMainWindow();
        Rect rect = new Rect(0, 0, i2, i3);
        Rect rect2 = new Rect(0, 0, displayInfo.logicalWidth, displayInfo.logicalHeight);
        Rect rect3 = new Rect();
        Rect rect4 = new Rect();
        Rect rect5 = null;
        boolean z3 = windowStateFindMainWindow != null && windowStateFindMainWindow.inFreeformWindowingMode();
        if (windowStateFindMainWindow != null) {
            if (z3) {
                rect.set(windowStateFindMainWindow.mFrame);
            } else if (windowStateFindMainWindow.isLetterboxedAppWindow()) {
                rect.set(getTask().getBounds());
            } else if (windowStateFindMainWindow.isDockedResizing()) {
                rect.set(getTask().getParent().getBounds());
            } else {
                rect.set(windowStateFindMainWindow.mContainingFrame);
            }
            rect5 = windowStateFindMainWindow.getAttrs().surfaceInsets;
            rect3.set(windowStateFindMainWindow.mContentInsets);
            rect4.set(windowStateFindMainWindow.mStableInsets);
        }
        Rect rect6 = rect5;
        boolean z4 = this.mLaunchTaskBehind ? false : z;
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
            Slog.d("WindowManager", "Loading animation for app transition. transit=" + AppTransition.appTransitionToString(i) + " enter=" + z4 + " frame=" + rect + " insets=" + rect3 + " surfaceInsets=" + rect6);
        }
        Configuration configuration = displayContent.getConfiguration();
        Animation animationLoadAnimation = this.mService.mAppTransition.loadAnimation(layoutParams, i, z4, configuration.uiMode, configuration.orientation, rect, rect2, rect3, rect6, rect4, z2, z3, getTask().mTaskId);
        if (animationLoadAnimation != null) {
            if (animationLoadAnimation != null) {
                animationLoadAnimation.restrictDuration(3000L);
            }
            if (WindowManagerDebugConfig.DEBUG_ANIM) {
                StringBuilder sb = new StringBuilder();
                sb.append("Loaded animation ");
                sb.append(animationLoadAnimation);
                sb.append(" for ");
                sb.append(this);
                sb.append(", duration: ");
                sb.append(animationLoadAnimation != null ? animationLoadAnimation.getDuration() : 0L);
                WindowManagerService.logWithStack("WindowManager", sb.toString());
            }
            animationLoadAnimation.initialize(rect.width(), rect.height(), i2, i3);
            animationLoadAnimation.scaleCurrentDuration(this.mService.getTransitionAnimationScaleLocked());
        }
        return animationLoadAnimation;
    }

    @Override
    public boolean shouldDeferAnimationFinish(Runnable runnable) {
        return this.mAnimatingAppWindowTokenRegistry != null && this.mAnimatingAppWindowTokenRegistry.notifyAboutToFinish(this, runnable);
    }

    @Override
    public void onAnimationLeashDestroyed(SurfaceControl.Transaction transaction) {
        super.onAnimationLeashDestroyed(transaction);
        if (this.mAnimatingAppWindowTokenRegistry != null) {
            this.mAnimatingAppWindowTokenRegistry.notifyFinished(this);
        }
    }

    @Override
    protected void setLayer(SurfaceControl.Transaction transaction, int i) {
        if (!this.mSurfaceAnimator.hasLeash()) {
            transaction.setLayer(this.mSurfaceControl, i);
        }
    }

    @Override
    protected void setRelativeLayer(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl, int i) {
        if (!this.mSurfaceAnimator.hasLeash()) {
            transaction.setRelativeLayer(this.mSurfaceControl, surfaceControl, i);
        }
    }

    @Override
    protected void reparentSurfaceControl(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl) {
        if (!this.mSurfaceAnimator.hasLeash()) {
            transaction.reparent(this.mSurfaceControl, surfaceControl.getHandle());
        }
    }

    @Override
    public void onAnimationLeashCreated(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl) {
        int prefixOrderIndex;
        if (!inPinnedWindowingMode()) {
            prefixOrderIndex = getPrefixOrderIndex();
        } else {
            prefixOrderIndex = getParent().getPrefixOrderIndex();
        }
        if (this.mNeedsZBoost) {
            prefixOrderIndex += Z_BOOST_BASE;
        }
        surfaceControl.setLayer(prefixOrderIndex);
        getDisplayContent().assignStackOrdering();
        if (this.mAnimatingAppWindowTokenRegistry != null) {
            this.mAnimatingAppWindowTokenRegistry.notifyStarting(this);
        }
    }

    void showAllWindowsLocked() {
        forAllWindows((Consumer<WindowState>) new Consumer() {
            @Override
            public final void accept(Object obj) {
                AppWindowToken.lambda$showAllWindowsLocked$2((WindowState) obj);
            }
        }, false);
    }

    static void lambda$showAllWindowsLocked$2(WindowState windowState) {
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v("WindowManager", "performing show on: " + windowState);
        }
        windowState.performShowLocked();
    }

    @Override
    protected void onAnimationFinished() {
        super.onAnimationFinished();
        this.mTransit = -1;
        boolean z = false;
        this.mTransitFlags = 0;
        this.mNeedsZBoost = false;
        setAppLayoutChanges(12, "AppWindowToken");
        clearThumbnail();
        if (isHidden() && this.hiddenRequested) {
            z = true;
        }
        setClientHidden(z);
        if (this.mService.mInputMethodTarget != null && this.mService.mInputMethodTarget.mAppToken == this) {
            getDisplayContent().computeImeTarget(true);
        }
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.v("WindowManager", "Animation done in " + this + ": reportedVisible=" + this.reportedVisible + " okToDisplay=" + okToDisplay() + " okToAnimate=" + okToAnimate() + " startingDisplayed=" + this.startingDisplayed);
        }
        new ArrayList(this.mChildren).forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((WindowState) obj).onExitAnimationDone();
            }
        });
        this.mService.mAppTransition.notifyAppTransitionFinishedLocked(this.token);
        scheduleAnimation();
    }

    @Override
    boolean isAppAnimating() {
        return isSelfAnimating();
    }

    @Override
    boolean isSelfAnimating() {
        return isWaitingForTransitionStart() || isReallyAnimating();
    }

    private boolean isReallyAnimating() {
        return super.isSelfAnimating();
    }

    @Override
    void cancelAnimation() {
        super.cancelAnimation();
        clearThumbnail();
    }

    boolean isWaitingForTransitionStart() {
        return this.mService.mAppTransition.isTransitionSet() && (this.mService.mOpeningApps.contains(this) || this.mService.mClosingApps.contains(this));
    }

    public int getTransit() {
        return this.mTransit;
    }

    int getTransitFlags() {
        return this.mTransitFlags;
    }

    void attachThumbnailAnimation() {
        if (!isReallyAnimating()) {
            return;
        }
        int i = getTask().mTaskId;
        GraphicBuffer appTransitionThumbnailHeader = this.mService.mAppTransition.getAppTransitionThumbnailHeader(i);
        if (appTransitionThumbnailHeader == null) {
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.d("WindowManager", "No thumbnail header bitmap for: " + i);
                return;
            }
            return;
        }
        clearThumbnail();
        this.mThumbnail = new AppWindowThumbnail(getPendingTransaction(), this, appTransitionThumbnailHeader);
        this.mThumbnail.startAnimation(getPendingTransaction(), loadThumbnailAnimation(appTransitionThumbnailHeader));
    }

    void attachCrossProfileAppsThumbnailAnimation() {
        int i;
        if (!isReallyAnimating()) {
            return;
        }
        clearThumbnail();
        WindowState windowStateFindMainWindow = findMainWindow();
        if (windowStateFindMainWindow == null) {
            return;
        }
        Rect rect = windowStateFindMainWindow.mFrame;
        if (getTask().mUserId == this.mService.mCurrentUserId) {
            i = R.drawable.contact_header_bg;
        } else {
            i = R.drawable.edit_query_background_pressed;
        }
        GraphicBuffer graphicBufferCreateCrossProfileAppsThumbnail = this.mService.mAppTransition.createCrossProfileAppsThumbnail(i, rect);
        if (graphicBufferCreateCrossProfileAppsThumbnail == null) {
            return;
        }
        this.mThumbnail = new AppWindowThumbnail(getPendingTransaction(), this, graphicBufferCreateCrossProfileAppsThumbnail);
        this.mThumbnail.startAnimation(getPendingTransaction(), this.mService.mAppTransition.createCrossProfileAppsThumbnailAnimationLocked(windowStateFindMainWindow.mFrame), new Point(rect.left, rect.top));
    }

    private Animation loadThumbnailAnimation(GraphicBuffer graphicBuffer) {
        Rect rect;
        DisplayInfo displayInfo = this.mDisplayContent.getDisplayInfo();
        WindowState windowStateFindMainWindow = findMainWindow();
        if (windowStateFindMainWindow == null) {
            rect = new Rect(0, 0, displayInfo.appWidth, displayInfo.appHeight);
        } else {
            rect = windowStateFindMainWindow.getContentFrameLw();
        }
        Rect rect2 = windowStateFindMainWindow != null ? windowStateFindMainWindow.mContentInsets : null;
        Configuration configuration = this.mDisplayContent.getConfiguration();
        return this.mService.mAppTransition.createThumbnailAspectScaleAnimationLocked(rect, rect2, graphicBuffer, getTask().mTaskId, configuration.uiMode, configuration.orientation);
    }

    private void clearThumbnail() {
        if (this.mThumbnail == null) {
            return;
        }
        this.mThumbnail.destroy();
        this.mThumbnail = null;
    }

    void registerRemoteAnimations(RemoteAnimationDefinition remoteAnimationDefinition) {
        this.mRemoteAnimationDefinition = remoteAnimationDefinition;
    }

    RemoteAnimationDefinition getRemoteAnimationDefinition() {
        return this.mRemoteAnimationDefinition;
    }

    @Override
    void dump(PrintWriter printWriter, String str, boolean z) {
        String str2;
        super.dump(printWriter, str, z);
        if (this.appToken != null) {
            printWriter.println(str + "app=true mVoiceInteraction=" + this.mVoiceInteraction);
        }
        printWriter.print(str);
        printWriter.print("task=");
        printWriter.println(getTask());
        printWriter.print(str);
        printWriter.print(" mFillsParent=");
        printWriter.print(this.mFillsParent);
        printWriter.print(" mOrientation=");
        printWriter.println(this.mOrientation);
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append("hiddenRequested=");
        sb.append(this.hiddenRequested);
        sb.append(" mClientHidden=");
        sb.append(this.mClientHidden);
        if (this.mDeferHidingClient) {
            str2 = " mDeferHidingClient=" + this.mDeferHidingClient;
        } else {
            str2 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        sb.append(str2);
        sb.append(" reportedDrawn=");
        sb.append(this.reportedDrawn);
        sb.append(" reportedVisible=");
        sb.append(this.reportedVisible);
        printWriter.println(sb.toString());
        if (this.paused) {
            printWriter.print(str);
            printWriter.print("paused=");
            printWriter.println(this.paused);
        }
        if (this.mAppStopped) {
            printWriter.print(str);
            printWriter.print("mAppStopped=");
            printWriter.println(this.mAppStopped);
        }
        if (this.mNumInterestingWindows != 0 || this.mNumDrawnWindows != 0 || this.allDrawn || this.mLastAllDrawn) {
            printWriter.print(str);
            printWriter.print("mNumInterestingWindows=");
            printWriter.print(this.mNumInterestingWindows);
            printWriter.print(" mNumDrawnWindows=");
            printWriter.print(this.mNumDrawnWindows);
            printWriter.print(" inPendingTransaction=");
            printWriter.print(this.inPendingTransaction);
            printWriter.print(" allDrawn=");
            printWriter.print(this.allDrawn);
            printWriter.print(" lastAllDrawn=");
            printWriter.print(this.mLastAllDrawn);
            printWriter.println(")");
        }
        if (this.inPendingTransaction) {
            printWriter.print(str);
            printWriter.print("inPendingTransaction=");
            printWriter.println(this.inPendingTransaction);
        }
        if (this.startingData != null || this.removed || this.firstWindowDrawn || this.mIsExiting) {
            printWriter.print(str);
            printWriter.print("startingData=");
            printWriter.print(this.startingData);
            printWriter.print(" removed=");
            printWriter.print(this.removed);
            printWriter.print(" firstWindowDrawn=");
            printWriter.print(this.firstWindowDrawn);
            printWriter.print(" mIsExiting=");
            printWriter.println(this.mIsExiting);
        }
        if (this.startingWindow != null || this.startingSurface != null || this.startingDisplayed || this.startingMoved || this.mHiddenSetFromTransferredStartingWindow) {
            printWriter.print(str);
            printWriter.print("startingWindow=");
            printWriter.print(this.startingWindow);
            printWriter.print(" startingSurface=");
            printWriter.print(this.startingSurface);
            printWriter.print(" startingDisplayed=");
            printWriter.print(this.startingDisplayed);
            printWriter.print(" startingMoved=");
            printWriter.print(this.startingMoved);
            printWriter.println(" mHiddenSetFromTransferredStartingWindow=" + this.mHiddenSetFromTransferredStartingWindow);
        }
        if (!this.mFrozenBounds.isEmpty()) {
            printWriter.print(str);
            printWriter.print("mFrozenBounds=");
            printWriter.println(this.mFrozenBounds);
            printWriter.print(str);
            printWriter.print("mFrozenMergedConfig=");
            printWriter.println(this.mFrozenMergedConfig);
        }
        if (this.mPendingRelaunchCount != 0) {
            printWriter.print(str);
            printWriter.print("mPendingRelaunchCount=");
            printWriter.println(this.mPendingRelaunchCount);
        }
        if (getController() != null) {
            printWriter.print(str);
            printWriter.print("controller=");
            printWriter.println(getController());
        }
        if (this.mRemovingFromDisplay) {
            printWriter.println(str + "mRemovingFromDisplay=" + this.mRemovingFromDisplay);
        }
    }

    @Override
    void setHidden(boolean z) {
        super.setHidden(z);
        if (z) {
            this.mDisplayContent.mPinnedStackControllerLocked.resetReentrySnapFraction(this);
        }
        scheduleAnimation();
    }

    @Override
    void prepareSurfaces() {
        boolean z = !isHidden() || super.isSelfAnimating();
        if (z && !this.mLastSurfaceShowing) {
            this.mPendingTransaction.show(this.mSurfaceControl);
        } else if (!z && this.mLastSurfaceShowing) {
            this.mPendingTransaction.hide(this.mSurfaceControl);
        }
        if (this.mThumbnail != null) {
            this.mThumbnail.setShowing(this.mPendingTransaction, z);
        }
        this.mLastSurfaceShowing = z;
        super.prepareSurfaces();
    }

    boolean isSurfaceShowing() {
        return this.mLastSurfaceShowing;
    }

    boolean isFreezingScreen() {
        return this.mFreezingScreen;
    }

    @Override
    boolean needsZBoost() {
        return this.mNeedsZBoost || super.needsZBoost();
    }

    @Override
    public void writeToProto(ProtoOutputStream protoOutputStream, long j, boolean z) {
        long jStart = protoOutputStream.start(j);
        writeNameToProto(protoOutputStream, 1138166333441L);
        super.writeToProto(protoOutputStream, 1146756268034L, z);
        protoOutputStream.write(1133871366147L, this.mLastSurfaceShowing);
        protoOutputStream.write(1133871366148L, isWaitingForTransitionStart());
        protoOutputStream.write(1133871366149L, isReallyAnimating());
        if (this.mThumbnail != null) {
            this.mThumbnail.writeToProto(protoOutputStream, 1146756268038L);
        }
        protoOutputStream.write(1133871366151L, this.mFillsParent);
        protoOutputStream.write(1133871366152L, this.mAppStopped);
        protoOutputStream.write(1133871366153L, this.hiddenRequested);
        protoOutputStream.write(1133871366154L, this.mClientHidden);
        protoOutputStream.write(1133871366155L, this.mDeferHidingClient);
        protoOutputStream.write(1133871366156L, this.reportedDrawn);
        protoOutputStream.write(1133871366157L, this.reportedVisible);
        protoOutputStream.write(1120986464270L, this.mNumInterestingWindows);
        protoOutputStream.write(1120986464271L, this.mNumDrawnWindows);
        protoOutputStream.write(1133871366160L, this.allDrawn);
        protoOutputStream.write(1133871366161L, this.mLastAllDrawn);
        protoOutputStream.write(1133871366162L, this.removed);
        if (this.startingWindow != null) {
            this.startingWindow.writeIdentifierToProto(protoOutputStream, 1146756268051L);
        }
        protoOutputStream.write(1133871366164L, this.startingDisplayed);
        protoOutputStream.write(1133871366165L, this.startingMoved);
        protoOutputStream.write(1133871366166L, this.mHiddenSetFromTransferredStartingWindow);
        Iterator<Rect> it = this.mFrozenBounds.iterator();
        while (it.hasNext()) {
            it.next().writeToProto(protoOutputStream, 2246267895831L);
        }
        protoOutputStream.end(jStart);
    }

    void writeNameToProto(ProtoOutputStream protoOutputStream, long j) {
        if (this.appToken == null) {
            return;
        }
        try {
            protoOutputStream.write(j, this.appToken.getName());
        } catch (RemoteException e) {
            Slog.e("WindowManager", e.toString());
        }
    }

    @Override
    public String toString() {
        if (this.stringName == null) {
            this.stringName = "AppWindowToken{" + Integer.toHexString(System.identityHashCode(this)) + " token=" + this.token + '}';
        }
        StringBuilder sb = new StringBuilder();
        sb.append(this.stringName);
        sb.append(this.mIsExiting ? " mIsExiting=" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        return sb.toString();
    }

    Rect getLetterboxInsets() {
        if (this.mLetterbox != null) {
            return this.mLetterbox.getInsets();
        }
        return new Rect();
    }

    boolean isLetterboxOverlappingWith(Rect rect) {
        return this.mLetterbox != null && this.mLetterbox.isOverlappingWith(rect);
    }

    void setWillCloseOrEnterPip(boolean z) {
        this.mWillCloseOrEnterPip = z;
    }

    boolean isClosingOrEnteringPip() {
        return (isAnimating() && this.hiddenRequested) || this.mWillCloseOrEnterPip;
    }
}
