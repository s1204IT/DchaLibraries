package com.android.server.wm;

import android.app.ActivityManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Message;
import android.os.RemoteException;
import android.util.Slog;
import android.view.IApplicationToken;
import android.view.View;
import com.android.server.input.InputApplicationHandle;
import com.android.server.wm.WindowSurfaceController;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;

class AppWindowToken extends WindowToken {
    private static final String TAG = "WindowManager";
    final WindowList allAppWindows;
    boolean allDrawn;
    boolean allDrawnExcludingSaved;
    boolean appFullscreen;
    final IApplicationToken appToken;
    boolean clientHidden;
    boolean deferClearAllDrawn;
    boolean firstWindowDrawn;
    boolean hiddenRequested;
    boolean inPendingTransaction;
    long inputDispatchingTimeoutNanos;
    long lastTransactionSequence;
    boolean layoutConfigChanges;
    boolean mAlwaysFocusable;
    final AppWindowAnimator mAppAnimator;
    boolean mAppStopped;
    boolean mEnteringAnimation;
    ArrayDeque<Rect> mFrozenBounds;
    ArrayDeque<Configuration> mFrozenMergedConfig;
    final InputApplicationHandle mInputApplicationHandle;
    boolean mIsExiting;
    boolean mLaunchTaskBehind;
    int mPendingRelaunchCount;
    private ArrayList<WindowSurfaceController.SurfaceControlWithBackground> mSurfaceViewBackgrounds;
    Task mTask;
    int numDrawnWindows;
    int numDrawnWindowsExclusingSaved;
    int numInterestingWindows;
    int numInterestingWindowsExcludingSaved;
    boolean removed;
    boolean reportedDrawn;
    boolean reportedVisible;
    int requestedOrientation;
    boolean showForAllUsers;
    StartingData startingData;
    boolean startingDisplayed;
    boolean startingMoved;
    View startingView;
    WindowState startingWindow;
    int targetSdk;
    final boolean voiceInteraction;

    AppWindowToken(WindowManagerService _service, IApplicationToken _token, boolean _voiceInteraction) {
        super(_service, _token.asBinder(), 2, true);
        this.allAppWindows = new WindowList();
        this.requestedOrientation = -1;
        this.lastTransactionSequence = Long.MIN_VALUE;
        this.mSurfaceViewBackgrounds = new ArrayList<>();
        this.mFrozenBounds = new ArrayDeque<>();
        this.mFrozenMergedConfig = new ArrayDeque<>();
        this.appWindowToken = this;
        this.appToken = _token;
        this.voiceInteraction = _voiceInteraction;
        this.mInputApplicationHandle = new InputApplicationHandle(this);
        this.mAppAnimator = new AppWindowAnimator(this);
    }

    void sendAppVisibilityToClients() {
        int N = this.allAppWindows.size();
        for (int i = 0; i < N; i++) {
            WindowState win = this.allAppWindows.get(i);
            if (win != this.startingWindow || !this.clientHidden) {
                try {
                    if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                        Slog.v(TAG, "Setting visibility of " + win + ": " + (!this.clientHidden));
                    }
                    win.mClient.dispatchAppVisibility(!this.clientHidden);
                } catch (RemoteException e) {
                }
            }
        }
    }

    void setVisibleBeforeClientHidden() {
        for (int i = this.allAppWindows.size() - 1; i >= 0; i--) {
            WindowState w = this.allAppWindows.get(i);
            w.setVisibleBeforeClientHidden();
        }
    }

    void onFirstWindowDrawn(WindowState win, WindowStateAnimator winAnimator) {
        this.firstWindowDrawn = true;
        removeAllDeadWindows();
        if (this.startingData != null) {
            if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW || WindowManagerDebugConfig.DEBUG_ANIM) {
                Slog.v(TAG, "Finish starting " + win.mToken + ": first real window is shown, no animation");
            }
            winAnimator.clearAnimation();
            winAnimator.mService.mFinishedStarting.add(this);
            winAnimator.mService.mH.sendEmptyMessage(7);
        }
        updateReportedVisibilityLocked();
    }

    void updateReportedVisibilityLocked() {
        if (this.appToken == null) {
            return;
        }
        int numInteresting = 0;
        int numVisible = 0;
        int numDrawn = 0;
        boolean nowGone = true;
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(TAG, "Update reported visibility: " + this);
        }
        int N = this.allAppWindows.size();
        for (int i = 0; i < N; i++) {
            WindowState win = this.allAppWindows.get(i);
            if (win != this.startingWindow && !win.mAppFreezing && win.mViewVisibility == 0 && win.mAttrs.type != 3 && !win.mDestroying) {
                if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                    Slog.v(TAG, "Win " + win + ": isDrawn=" + win.isDrawnLw() + ", isAnimationSet=" + win.mWinAnimator.isAnimationSet());
                    if (!win.isDrawnLw()) {
                        Slog.v(TAG, "Not displayed: s=" + win.mWinAnimator.mSurfaceController + " pv=" + win.mPolicyVisibility + " mDrawState=" + win.mWinAnimator.mDrawState + " ah=" + win.mAttachedHidden + " th=" + (win.mAppToken != null ? win.mAppToken.hiddenRequested : false) + " a=" + win.mWinAnimator.mAnimating);
                    }
                }
                numInteresting++;
                if (win.isDrawnLw()) {
                    numDrawn++;
                    if (!win.mWinAnimator.isAnimationSet()) {
                        numVisible++;
                    }
                    nowGone = false;
                } else if (win.mWinAnimator.isAnimationSet()) {
                    nowGone = false;
                }
            }
        }
        boolean nowDrawn = numInteresting > 0 && numDrawn >= numInteresting;
        boolean nowVisible = numInteresting > 0 && numVisible >= numInteresting;
        if (!nowGone) {
            if (!nowDrawn) {
                nowDrawn = this.reportedDrawn;
            }
            if (!nowVisible) {
                nowVisible = this.reportedVisible;
            }
        }
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(TAG, "VIS " + this + ": interesting=" + numInteresting + " visible=" + numVisible);
        }
        if (nowDrawn != this.reportedDrawn) {
            if (nowDrawn) {
                Message m = this.service.mH.obtainMessage(9, this);
                this.service.mH.sendMessage(m);
            }
            this.reportedDrawn = nowDrawn;
        }
        if (nowVisible != this.reportedVisible) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG, "Visibility changed in " + this + ": vis=" + nowVisible);
            }
            this.reportedVisible = nowVisible;
            Message m2 = this.service.mH.obtainMessage(8, nowVisible ? 1 : 0, nowGone ? 1 : 0, this);
            this.service.mH.sendMessage(m2);
        }
    }

    WindowState findMainWindow() {
        WindowState candidate = null;
        int j = this.windows.size();
        while (j > 0) {
            j--;
            WindowState win = this.windows.get(j);
            if (win.mAttrs.type == 1 || win.mAttrs.type == 3) {
                if (win.mAnimatingExit) {
                    candidate = win;
                } else {
                    return win;
                }
            }
        }
        return candidate;
    }

    boolean windowsAreFocusable() {
        if (ActivityManager.StackId.canReceiveKeys(this.mTask.mStack.mStackId)) {
            return true;
        }
        return this.mAlwaysFocusable;
    }

    boolean isVisible() {
        int N = this.allAppWindows.size();
        for (int i = 0; i < N; i++) {
            WindowState win = this.allAppWindows.get(i);
            if (!win.mAppFreezing && ((win.mViewVisibility == 0 || win.isAnimatingWithSavedSurface() || (win.mWinAnimator.isAnimationSet() && !this.service.mAppTransition.isTransitionSet())) && !win.mDestroying && win.isDrawnLw())) {
                return true;
            }
        }
        return false;
    }

    void removeAppFromTaskLocked() {
        this.mIsExiting = false;
        removeAllWindows();
        Task task = this.mTask;
        if (task == null) {
            return;
        }
        if (!task.removeAppToken(this)) {
            Slog.e(TAG, "removeAppFromTaskLocked: token=" + this + " not found.");
        }
        task.mStack.mExitingAppTokens.remove(this);
    }

    void destroySurfaces() {
        ArrayList<WindowState> allWindows = (ArrayList) this.allAppWindows.clone();
        DisplayContentList displayList = new DisplayContentList();
        for (int i = allWindows.size() - 1; i >= 0; i--) {
            WindowState win = allWindows.get(i);
            if (!this.mAppStopped ? win.mWindowRemovalAllowed : true) {
                win.mWinAnimator.destroyPreservedSurfaceLocked();
                if (win.mDestroying) {
                    if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                        Slog.e("WindowManager", "win=" + win + " destroySurfaces: mAppStopped=" + this.mAppStopped + " win.mWindowRemovalAllowed=" + win.mWindowRemovalAllowed + " win.mRemoveOnExit=" + win.mRemoveOnExit);
                    }
                    win.destroyOrSaveSurface();
                    if (win.mRemoveOnExit) {
                        this.service.removeWindowInnerLocked(win);
                    }
                    DisplayContent displayContent = win.getDisplayContent();
                    if (displayContent != null && !displayList.contains(displayContent)) {
                        displayList.add(displayContent);
                    }
                    win.mDestroying = false;
                }
            }
        }
        for (int i2 = 0; i2 < displayList.size(); i2++) {
            DisplayContent displayContent2 = displayList.get(i2);
            this.service.mLayersController.assignLayersLocked(displayContent2.getWindowList());
            displayContent2.layoutNeeded = true;
        }
    }

    void notifyAppStopped(boolean stopped) {
        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.v(TAG, "notifyAppStopped: stopped=" + stopped + " " + this);
        }
        this.mAppStopped = stopped;
        if (!stopped) {
            return;
        }
        destroySurfaces();
        this.mTask.mService.scheduleRemoveStartingWindowLocked(this);
    }

    boolean shouldSaveSurface() {
        return this.allDrawn;
    }

    boolean canRestoreSurfaces() {
        for (int i = this.allAppWindows.size() - 1; i >= 0; i--) {
            WindowState w = this.allAppWindows.get(i);
            if (w.canRestoreSurface()) {
                return true;
            }
        }
        return false;
    }

    void clearVisibleBeforeClientHidden() {
        for (int i = this.allAppWindows.size() - 1; i >= 0; i--) {
            WindowState w = this.allAppWindows.get(i);
            w.clearVisibleBeforeClientHidden();
        }
    }

    boolean isAnimatingInvisibleWithSavedSurface() {
        for (int i = this.allAppWindows.size() - 1; i >= 0; i--) {
            WindowState w = this.allAppWindows.get(i);
            if (w.isAnimatingInvisibleWithSavedSurface()) {
                return true;
            }
        }
        return false;
    }

    void stopUsingSavedSurfaceLocked() {
        for (int i = this.allAppWindows.size() - 1; i >= 0; i--) {
            WindowState w = this.allAppWindows.get(i);
            if (w.isAnimatingInvisibleWithSavedSurface()) {
                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_ANIM) {
                    Slog.d(TAG, "stopUsingSavedSurfaceLocked: " + w);
                }
                w.clearAnimatingWithSavedSurface();
                w.mDestroying = true;
                w.mWinAnimator.hide("stopUsingSavedSurfaceLocked");
                w.mWinAnimator.mWallpaperControllerLocked.hideWallpapers(w);
            }
        }
        destroySurfaces();
    }

    void markSavedSurfaceExiting() {
        for (int i = this.allAppWindows.size() - 1; i >= 0; i--) {
            WindowState w = this.allAppWindows.get(i);
            if (w.isAnimatingInvisibleWithSavedSurface()) {
                w.mAnimatingExit = true;
                w.mWinAnimator.mAnimating = true;
            }
        }
    }

    void restoreSavedSurfaces() {
        boolean z = false;
        if (!canRestoreSurfaces()) {
            clearVisibleBeforeClientHidden();
            return;
        }
        int numInteresting = 0;
        int numDrawn = 0;
        for (int i = this.allAppWindows.size() - 1; i >= 0; i--) {
            WindowState w = this.allAppWindows.get(i);
            if (w != this.startingWindow && !w.mAppDied && w.wasVisibleBeforeClientHidden() && (!this.mAppAnimator.freezingScreen || !w.mAppFreezing)) {
                numInteresting++;
                if (w.hasSavedSurface()) {
                    w.restoreSavedSurface();
                }
                if (w.isDrawnLw()) {
                    numDrawn++;
                }
            }
        }
        if (!this.allDrawn) {
            if (numInteresting > 0 && numInteresting == numDrawn) {
                z = true;
            }
            this.allDrawn = z;
            if (this.allDrawn) {
                this.service.mH.obtainMessage(32, this.token).sendToTarget();
            }
        }
        clearVisibleBeforeClientHidden();
        if (!WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS && !WindowManagerDebugConfig.DEBUG_ANIM) {
            return;
        }
        Slog.d(TAG, "restoreSavedSurfaces: " + this.appWindowToken + " allDrawn=" + this.allDrawn + " numInteresting=" + numInteresting + " numDrawn=" + numDrawn);
    }

    void destroySavedSurfaces() {
        for (int i = this.allAppWindows.size() - 1; i >= 0; i--) {
            WindowState win = this.allAppWindows.get(i);
            win.destroySavedSurface();
        }
    }

    void clearAllDrawn() {
        this.allDrawn = false;
        this.deferClearAllDrawn = false;
        this.allDrawnExcludingSaved = false;
    }

    @Override
    void removeAllWindows() {
        int winNdx = this.allAppWindows.size() - 1;
        while (winNdx >= 0) {
            WindowState win = this.allAppWindows.get(winNdx);
            if (WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT) {
                Slog.w(TAG, "removeAllWindows: removing win=" + win);
            }
            this.service.removeWindowLocked(win);
            if (this.allAppWindows.size() == 0) {
                break;
            } else {
                winNdx = Math.min(winNdx - 1, this.allAppWindows.size() - 1);
            }
        }
        this.allAppWindows.clear();
        this.windows.clear();
    }

    void removeAllDeadWindows() {
        int winNdx = this.allAppWindows.size() - 1;
        while (winNdx >= 0) {
            WindowState win = this.allAppWindows.get(winNdx);
            if (win.mAppDied) {
                if (WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT || WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                    Slog.w(TAG, "removeAllDeadWindows: " + win);
                }
                win.mDestroying = true;
                this.service.removeWindowLocked(win);
            }
            winNdx = Math.min(winNdx - 1, this.allAppWindows.size() - 1);
        }
    }

    boolean hasWindowsAlive() {
        for (int i = this.allAppWindows.size() - 1; i >= 0; i--) {
            if (!this.allAppWindows.get(i).mAppDied) {
                return true;
            }
        }
        return false;
    }

    void setReplacingWindows(boolean animate) {
        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.d("WindowManager", "Marking app token " + this.appWindowToken + " with replacing windows.");
        }
        for (int i = this.allAppWindows.size() - 1; i >= 0; i--) {
            WindowState w = this.allAppWindows.get(i);
            w.setReplacing(animate);
        }
        if (!animate) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
            Slog.v("WindowManager", "setReplacingWindow() Setting dummy animation on: " + this);
        }
        this.mAppAnimator.setDummyAnimation();
    }

    void setReplacingChildren() {
        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.d("WindowManager", "Marking app token " + this.appWindowToken + " with replacing child windows.");
        }
        for (int i = this.allAppWindows.size() - 1; i >= 0; i--) {
            WindowState w = this.allAppWindows.get(i);
            if (w.shouldBeReplacedWithChildren()) {
                w.setReplacing(false);
            }
        }
    }

    void resetReplacingWindows() {
        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.d("WindowManager", "Resetting app token " + this.appWindowToken + " of replacing window marks.");
        }
        for (int i = this.allAppWindows.size() - 1; i >= 0; i--) {
            WindowState w = this.allAppWindows.get(i);
            w.resetReplacing();
        }
    }

    void requestUpdateWallpaperIfNeeded() {
        for (int i = this.allAppWindows.size() - 1; i >= 0; i--) {
            WindowState w = this.allAppWindows.get(i);
            w.requestUpdateWallpaperIfNeeded();
        }
    }

    boolean isRelaunching() {
        return this.mPendingRelaunchCount > 0;
    }

    void startRelaunching() {
        if (canFreezeBounds()) {
            freezeBounds();
        }
        this.mPendingRelaunchCount++;
    }

    void finishRelaunching() {
        if (canFreezeBounds()) {
            unfreezeBounds();
        }
        if (this.mPendingRelaunchCount <= 0) {
            return;
        }
        this.mPendingRelaunchCount--;
    }

    void clearRelaunching() {
        while (isRelaunching()) {
            finishRelaunching();
        }
    }

    void addWindow(WindowState w) {
        for (int i = this.allAppWindows.size() - 1; i >= 0; i--) {
            WindowState candidate = this.allAppWindows.get(i);
            if (candidate.mWillReplaceWindow && candidate.mReplacingWindow == null && candidate.getWindowTag().toString().equals(w.getWindowTag().toString())) {
                candidate.mReplacingWindow = w;
                w.mSkipEnterAnimationForSeamlessReplacement = !candidate.mAnimateReplacingWindow;
                this.service.scheduleReplacingWindowTimeouts(this);
            }
        }
        this.allAppWindows.add(w);
    }

    boolean waitingForReplacement() {
        for (int i = this.allAppWindows.size() - 1; i >= 0; i--) {
            WindowState candidate = this.allAppWindows.get(i);
            if (candidate.mWillReplaceWindow) {
                return true;
            }
        }
        return false;
    }

    void clearTimedoutReplacesLocked() {
        int i = this.allAppWindows.size() - 1;
        while (i >= 0) {
            WindowState candidate = this.allAppWindows.get(i);
            if (candidate.mWillReplaceWindow) {
                candidate.mWillReplaceWindow = false;
                if (candidate.mReplacingWindow != null) {
                    candidate.mReplacingWindow.mSkipEnterAnimationForSeamlessReplacement = false;
                }
                this.service.removeWindowInnerLocked(candidate);
            }
            i = Math.min(i - 1, this.allAppWindows.size() - 1);
        }
    }

    private boolean canFreezeBounds() {
        return (this.mTask == null || this.mTask.inFreeformWorkspace()) ? false : true;
    }

    private void freezeBounds() {
        this.mFrozenBounds.offer(new Rect(this.mTask.mPreparedFrozenBounds));
        if (this.mTask.mPreparedFrozenMergedConfig.equals(Configuration.EMPTY)) {
            Configuration config = new Configuration(this.service.mCurConfiguration);
            config.updateFrom(this.mTask.mOverrideConfig);
            this.mFrozenMergedConfig.offer(config);
        } else {
            this.mFrozenMergedConfig.offer(new Configuration(this.mTask.mPreparedFrozenMergedConfig));
        }
        this.mTask.mPreparedFrozenMergedConfig.setToDefaults();
    }

    private void unfreezeBounds() {
        this.mFrozenBounds.remove();
        this.mFrozenMergedConfig.remove();
        for (int i = this.windows.size() - 1; i >= 0; i--) {
            WindowState win = this.windows.get(i);
            if (win.mHasSurface) {
                win.mLayoutNeeded = true;
                win.setDisplayLayoutNeeded();
                if (!this.service.mResizingWindows.contains(win)) {
                    this.service.mResizingWindows.add(win);
                }
            }
        }
        this.service.mWindowPlacerLocked.performSurfacePlacement();
    }

    void addSurfaceViewBackground(WindowSurfaceController.SurfaceControlWithBackground background) {
        this.mSurfaceViewBackgrounds.add(background);
    }

    void removeSurfaceViewBackground(WindowSurfaceController.SurfaceControlWithBackground background) {
        this.mSurfaceViewBackgrounds.remove(background);
        updateSurfaceViewBackgroundVisibilities();
    }

    void updateSurfaceViewBackgroundVisibilities() {
        WindowSurfaceController.SurfaceControlWithBackground bottom = null;
        int bottomLayer = Integer.MAX_VALUE;
        for (int i = 0; i < this.mSurfaceViewBackgrounds.size(); i++) {
            WindowSurfaceController.SurfaceControlWithBackground sc = this.mSurfaceViewBackgrounds.get(i);
            if (sc.mVisible && sc.mLayer < bottomLayer) {
                bottomLayer = sc.mLayer;
                bottom = sc;
            }
        }
        for (int i2 = 0; i2 < this.mSurfaceViewBackgrounds.size(); i2++) {
            WindowSurfaceController.SurfaceControlWithBackground sc2 = this.mSurfaceViewBackgrounds.get(i2);
            sc2.updateBackgroundVisibility(sc2 != bottom);
        }
    }

    @Override
    void dump(PrintWriter pw, String prefix) {
        super.dump(pw, prefix);
        if (this.appToken != null) {
            pw.print(prefix);
            pw.print("app=true voiceInteraction=");
            pw.println(this.voiceInteraction);
        }
        if (this.allAppWindows.size() > 0) {
            pw.print(prefix);
            pw.print("allAppWindows=");
            pw.println(this.allAppWindows);
        }
        pw.print(prefix);
        pw.print("task=");
        pw.println(this.mTask);
        pw.print(prefix);
        pw.print(" appFullscreen=");
        pw.print(this.appFullscreen);
        pw.print(" requestedOrientation=");
        pw.println(this.requestedOrientation);
        pw.print(prefix);
        pw.print("hiddenRequested=");
        pw.print(this.hiddenRequested);
        pw.print(" clientHidden=");
        pw.print(this.clientHidden);
        pw.print(" reportedDrawn=");
        pw.print(this.reportedDrawn);
        pw.print(" reportedVisible=");
        pw.println(this.reportedVisible);
        if (this.paused) {
            pw.print(prefix);
            pw.print("paused=");
            pw.println(this.paused);
        }
        if (this.mAppStopped) {
            pw.print(prefix);
            pw.print("mAppStopped=");
            pw.println(this.mAppStopped);
        }
        if (this.numInterestingWindows != 0 || this.numDrawnWindows != 0 || this.allDrawn || this.mAppAnimator.allDrawn) {
            pw.print(prefix);
            pw.print("numInterestingWindows=");
            pw.print(this.numInterestingWindows);
            pw.print(" numDrawnWindows=");
            pw.print(this.numDrawnWindows);
            pw.print(" inPendingTransaction=");
            pw.print(this.inPendingTransaction);
            pw.print(" allDrawn=");
            pw.print(this.allDrawn);
            pw.print(" (animator=");
            pw.print(this.mAppAnimator.allDrawn);
            pw.println(")");
        }
        if (this.inPendingTransaction) {
            pw.print(prefix);
            pw.print("inPendingTransaction=");
            pw.println(this.inPendingTransaction);
        }
        if (this.startingData != null || this.removed || this.firstWindowDrawn || this.mIsExiting) {
            pw.print(prefix);
            pw.print("startingData=");
            pw.print(this.startingData);
            pw.print(" removed=");
            pw.print(this.removed);
            pw.print(" firstWindowDrawn=");
            pw.print(this.firstWindowDrawn);
            pw.print(" mIsExiting=");
            pw.println(this.mIsExiting);
        }
        if (this.startingWindow != null || this.startingView != null || this.startingDisplayed || this.startingMoved) {
            pw.print(prefix);
            pw.print("startingWindow=");
            pw.print(this.startingWindow);
            pw.print(" startingView=");
            pw.print(this.startingView);
            pw.print(" startingDisplayed=");
            pw.print(this.startingDisplayed);
            pw.print(" startingMoved=");
            pw.println(this.startingMoved);
        }
        if (!this.mFrozenBounds.isEmpty()) {
            pw.print(prefix);
            pw.print("mFrozenBounds=");
            pw.println(this.mFrozenBounds);
            pw.print(prefix);
            pw.print("mFrozenMergedConfig=");
            pw.println(this.mFrozenMergedConfig);
        }
        if (this.mPendingRelaunchCount == 0) {
            return;
        }
        pw.print(prefix);
        pw.print("mPendingRelaunchCount=");
        pw.println(this.mPendingRelaunchCount);
    }

    @Override
    public String toString() {
        if (this.stringName == null) {
            this.stringName = "AppWindowToken{" + Integer.toHexString(System.identityHashCode(this)) + " token=" + this.token + '}';
        }
        return this.stringName;
    }
}
