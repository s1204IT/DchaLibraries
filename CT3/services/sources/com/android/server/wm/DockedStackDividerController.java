package com.android.server.wm;

import android.R;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.IDockedStackListener;
import android.view.SurfaceControl;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.InputMethodManagerInternal;
import com.android.internal.policy.DividerSnapAlgorithm;
import com.android.internal.policy.DockedDividerUtils;
import com.android.server.LocalServices;
import com.android.server.wm.DimLayer;
import java.io.PrintWriter;
import java.util.ArrayList;

public class DockedStackDividerController implements DimLayer.DimLayerUser {
    private static final float CLIP_REVEAL_MEET_EARLIEST = 0.6f;
    private static final float CLIP_REVEAL_MEET_FRACTION_MAX = 0.8f;
    private static final float CLIP_REVEAL_MEET_FRACTION_MIN = 0.4f;
    private static final int DIVIDER_WIDTH_INACTIVE_DP = 4;
    private static final long IME_ADJUST_ANIM_DURATION = 280;
    private static final long IME_ADJUST_DRAWN_TIMEOUT = 200;
    private boolean mAdjustedForDivider;
    private boolean mAdjustedForIme;
    private boolean mAnimatingForIme;
    private boolean mAnimatingForMinimizedDockedStack;
    private long mAnimationDuration;
    private float mAnimationStart;
    private boolean mAnimationStartDelayed;
    private long mAnimationStartTime;
    private boolean mAnimationStarted;
    private float mAnimationTarget;
    private WindowState mDelayedImeWin;
    private final DimLayer mDimLayer;
    private final DisplayContent mDisplayContent;
    private float mDividerAnimationStart;
    private float mDividerAnimationTarget;
    private int mDividerInsets;
    private int mDividerWindowWidth;
    private int mDividerWindowWidthInactive;
    private int mImeHeight;
    private boolean mImeHideRequested;
    private float mLastAnimationProgress;
    private float mLastDividerProgress;
    private float mMaximizeMeetFraction;
    private boolean mMinimizedDock;
    private final Interpolator mMinimizedDockInterpolator;
    private boolean mResizing;
    private final WindowManagerService mService;
    private WindowState mWindow;
    private static final String TAG = "WindowManager";
    private static final float CLIP_REVEAL_MEET_LAST = 1.0f;
    private static final Interpolator IME_ADJUST_ENTRY_INTERPOLATOR = new PathInterpolator(0.2f, 0.0f, 0.1f, CLIP_REVEAL_MEET_LAST);
    private final Rect mTmpRect = new Rect();
    private final Rect mTmpRect2 = new Rect();
    private final Rect mTmpRect3 = new Rect();
    private final Rect mLastRect = new Rect();
    private boolean mLastVisibility = false;
    private final RemoteCallbackList<IDockedStackListener> mDockedStackListeners = new RemoteCallbackList<>();
    private final Rect mTouchRegion = new Rect();
    private final DividerSnapAlgorithm[] mSnapAlgorithmForRotation = new DividerSnapAlgorithm[4];

    DockedStackDividerController(WindowManagerService service, DisplayContent displayContent) {
        this.mService = service;
        this.mDisplayContent = displayContent;
        Context context = service.mContext;
        this.mDimLayer = new DimLayer(displayContent.mService, this, displayContent.getDisplayId(), "DockedStackDim");
        this.mMinimizedDockInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.fast_out_slow_in);
        loadDimens();
    }

    int getSmallestWidthDpForBounds(Rect bounds) {
        int orientation;
        DisplayInfo di = this.mDisplayContent.getDisplayInfo();
        if (bounds == null || (bounds.left == 0 && bounds.top == 0 && bounds.right == di.logicalWidth && bounds.bottom == di.logicalHeight)) {
            return this.mService.mCurConfiguration.smallestScreenWidthDp;
        }
        int baseDisplayWidth = this.mDisplayContent.mBaseDisplayWidth;
        int baseDisplayHeight = this.mDisplayContent.mBaseDisplayHeight;
        int minWidth = Integer.MAX_VALUE;
        int rotation = 0;
        while (rotation < 4) {
            this.mTmpRect.set(bounds);
            this.mDisplayContent.rotateBounds(di.rotation, rotation, this.mTmpRect);
            boolean rotated = rotation == 1 || rotation == 3;
            this.mTmpRect2.set(0, 0, rotated ? baseDisplayHeight : baseDisplayWidth, rotated ? baseDisplayWidth : baseDisplayHeight);
            if (this.mTmpRect2.width() <= this.mTmpRect2.height()) {
                orientation = 1;
            } else {
                orientation = 2;
            }
            int dockSide = TaskStack.getDockSideUnchecked(this.mTmpRect, this.mTmpRect2, orientation);
            int position = DockedDividerUtils.calculatePositionForBounds(this.mTmpRect, dockSide, getContentWidth());
            int snappedPosition = this.mSnapAlgorithmForRotation[rotation].calculateNonDismissingSnapTarget(position).position;
            DockedDividerUtils.calculateBoundsForPosition(snappedPosition, dockSide, this.mTmpRect, this.mTmpRect2.width(), this.mTmpRect2.height(), getContentWidth());
            this.mService.mPolicy.getStableInsetsLw(rotation, this.mTmpRect2.width(), this.mTmpRect2.height(), this.mTmpRect3);
            this.mService.subtractInsets(this.mTmpRect2, this.mTmpRect3, this.mTmpRect);
            minWidth = Math.min(this.mTmpRect.width(), minWidth);
            rotation++;
        }
        return (int) (minWidth / this.mDisplayContent.getDisplayMetrics().density);
    }

    private void initSnapAlgorithmForRotations() {
        int dw;
        int dh;
        Configuration baseConfig = this.mService.mCurConfiguration;
        Configuration config = new Configuration();
        int rotation = 0;
        while (rotation < 4) {
            boolean rotated = rotation == 1 || rotation == 3;
            if (rotated) {
                dw = this.mDisplayContent.mBaseDisplayHeight;
            } else {
                dw = this.mDisplayContent.mBaseDisplayWidth;
            }
            if (rotated) {
                dh = this.mDisplayContent.mBaseDisplayWidth;
            } else {
                dh = this.mDisplayContent.mBaseDisplayHeight;
            }
            this.mService.mPolicy.getStableInsetsLw(rotation, dw, dh, this.mTmpRect);
            config.setToDefaults();
            config.orientation = dw <= dh ? 1 : 2;
            config.screenWidthDp = (int) (this.mService.mPolicy.getConfigDisplayWidth(dw, dh, rotation, baseConfig.uiMode) / this.mDisplayContent.getDisplayMetrics().density);
            config.screenHeightDp = (int) (this.mService.mPolicy.getConfigDisplayHeight(dw, dh, rotation, baseConfig.uiMode) / this.mDisplayContent.getDisplayMetrics().density);
            Context rotationContext = this.mService.mContext.createConfigurationContext(config);
            this.mSnapAlgorithmForRotation[rotation] = new DividerSnapAlgorithm(rotationContext.getResources(), dw, dh, getContentWidth(), config.orientation == 1, this.mTmpRect);
            rotation++;
        }
    }

    private void loadDimens() {
        Context context = this.mService.mContext;
        this.mDividerWindowWidth = context.getResources().getDimensionPixelSize(R.dimen.action_bar_elevation_material);
        this.mDividerInsets = context.getResources().getDimensionPixelSize(R.dimen.action_bar_icon_vertical_padding);
        this.mDividerWindowWidthInactive = WindowManagerService.dipToPixel(4, this.mDisplayContent.getDisplayMetrics());
        initSnapAlgorithmForRotations();
    }

    void onConfigurationChanged() {
        loadDimens();
    }

    boolean isResizing() {
        return this.mResizing;
    }

    int getContentWidth() {
        return this.mDividerWindowWidth - (this.mDividerInsets * 2);
    }

    int getContentInsets() {
        return this.mDividerInsets;
    }

    int getContentWidthInactive() {
        return this.mDividerWindowWidthInactive;
    }

    void setResizing(boolean resizing) {
        if (this.mResizing == resizing) {
            return;
        }
        this.mResizing = resizing;
        resetDragResizingChangeReported();
    }

    void setTouchRegion(Rect touchRegion) {
        this.mTouchRegion.set(touchRegion);
    }

    void getTouchRegion(Rect outRegion) {
        outRegion.set(this.mTouchRegion);
        outRegion.offset(this.mWindow.getFrameLw().left, this.mWindow.getFrameLw().top);
    }

    private void resetDragResizingChangeReported() {
        WindowList windowList = this.mDisplayContent.getWindowList();
        for (int i = windowList.size() - 1; i >= 0; i--) {
            windowList.get(i).resetDragResizingChangeReported();
        }
    }

    void setWindow(WindowState window) {
        this.mWindow = window;
        reevaluateVisibility(false);
    }

    void reevaluateVisibility(boolean force) {
        if (this.mWindow == null) {
            return;
        }
        TaskStack stack = this.mDisplayContent.mService.mStackIdToStack.get(3);
        boolean visible = stack != null;
        if (this.mLastVisibility == visible && !force) {
            return;
        }
        this.mLastVisibility = visible;
        notifyDockedDividerVisibilityChanged(visible);
        if (visible) {
            return;
        }
        setResizeDimLayer(false, -1, 0.0f);
    }

    boolean wasVisible() {
        return this.mLastVisibility;
    }

    void setAdjustedForIme(boolean adjustedForIme, boolean adjustedForDivider, boolean animate, WindowState imeWin, int imeHeight) {
        if (this.mAdjustedForIme == adjustedForIme && ((!adjustedForIme || this.mImeHeight == imeHeight) && this.mAdjustedForDivider == adjustedForDivider)) {
            return;
        }
        if (animate && !this.mAnimatingForMinimizedDockedStack) {
            startImeAdjustAnimation(adjustedForIme, adjustedForDivider, imeWin);
        } else {
            notifyAdjustedForImeChanged(!adjustedForIme ? adjustedForDivider : true, 0L);
        }
        this.mAdjustedForIme = adjustedForIme;
        this.mImeHeight = imeHeight;
        this.mAdjustedForDivider = adjustedForDivider;
    }

    int getImeHeightAdjustedFor() {
        return this.mImeHeight;
    }

    void positionDockedStackedDivider(Rect frame) {
        TaskStack stack = this.mDisplayContent.getDockedStackLocked();
        if (stack == null) {
            frame.set(this.mLastRect);
            return;
        }
        stack.getDimBounds(this.mTmpRect);
        int side = stack.getDockSide();
        switch (side) {
            case 1:
                frame.set(this.mTmpRect.right - this.mDividerInsets, frame.top, (this.mTmpRect.right + frame.width()) - this.mDividerInsets, frame.bottom);
                break;
            case 2:
                frame.set(frame.left, this.mTmpRect.bottom - this.mDividerInsets, this.mTmpRect.right, (this.mTmpRect.bottom + frame.height()) - this.mDividerInsets);
                break;
            case 3:
                frame.set((this.mTmpRect.left - frame.width()) + this.mDividerInsets, frame.top, this.mTmpRect.left + this.mDividerInsets, frame.bottom);
                break;
            case 4:
                frame.set(frame.left, (this.mTmpRect.top - frame.height()) + this.mDividerInsets, frame.right, this.mTmpRect.top + this.mDividerInsets);
                break;
        }
        this.mLastRect.set(frame);
    }

    void notifyDockedDividerVisibilityChanged(boolean visible) {
        int size = this.mDockedStackListeners.beginBroadcast();
        for (int i = 0; i < size; i++) {
            IDockedStackListener listener = this.mDockedStackListeners.getBroadcastItem(i);
            try {
                listener.onDividerVisibilityChanged(visible);
            } catch (RemoteException e) {
                Slog.e("WindowManager", "Error delivering divider visibility changed event.", e);
            }
        }
        this.mDockedStackListeners.finishBroadcast();
    }

    void notifyDockedStackExistsChanged(boolean exists) {
        int size = this.mDockedStackListeners.beginBroadcast();
        for (int i = 0; i < size; i++) {
            IDockedStackListener listener = this.mDockedStackListeners.getBroadcastItem(i);
            try {
                listener.onDockedStackExistsChanged(exists);
            } catch (RemoteException e) {
                Slog.e("WindowManager", "Error delivering docked stack exists changed event.", e);
            }
        }
        this.mDockedStackListeners.finishBroadcast();
        if (exists) {
            InputMethodManagerInternal inputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
            if (inputMethodManagerInternal == null) {
                return;
            }
            inputMethodManagerInternal.hideCurrentInputMethod();
            this.mImeHideRequested = true;
            return;
        }
        setMinimizedDockedStack(false);
    }

    void resetImeHideRequested() {
        this.mImeHideRequested = false;
    }

    boolean isImeHideRequested() {
        return this.mImeHideRequested;
    }

    void notifyDockedStackMinimizedChanged(boolean minimizedDock, long animDuration) {
        this.mService.mH.removeMessages(53);
        this.mService.mH.obtainMessage(53, minimizedDock ? 1 : 0, 0).sendToTarget();
        int size = this.mDockedStackListeners.beginBroadcast();
        for (int i = 0; i < size; i++) {
            IDockedStackListener listener = this.mDockedStackListeners.getBroadcastItem(i);
            try {
                listener.onDockedStackMinimizedChanged(minimizedDock, animDuration);
            } catch (RemoteException e) {
                Slog.e("WindowManager", "Error delivering minimized dock changed event.", e);
            }
        }
        this.mDockedStackListeners.finishBroadcast();
    }

    void notifyDockSideChanged(int newDockSide) {
        int size = this.mDockedStackListeners.beginBroadcast();
        for (int i = 0; i < size; i++) {
            IDockedStackListener listener = this.mDockedStackListeners.getBroadcastItem(i);
            try {
                listener.onDockSideChanged(newDockSide);
            } catch (RemoteException e) {
                Slog.e("WindowManager", "Error delivering dock side changed event.", e);
            }
        }
        this.mDockedStackListeners.finishBroadcast();
    }

    void notifyAdjustedForImeChanged(boolean adjustedForIme, long animDuration) {
        int size = this.mDockedStackListeners.beginBroadcast();
        for (int i = 0; i < size; i++) {
            IDockedStackListener listener = this.mDockedStackListeners.getBroadcastItem(i);
            try {
                listener.onAdjustedForImeChanged(adjustedForIme, animDuration);
            } catch (RemoteException e) {
                Slog.e("WindowManager", "Error delivering adjusted for ime changed event.", e);
            }
        }
        this.mDockedStackListeners.finishBroadcast();
    }

    void registerDockedStackListener(IDockedStackListener listener) {
        this.mDockedStackListeners.register(listener);
        notifyDockedDividerVisibilityChanged(wasVisible());
        notifyDockedStackExistsChanged(this.mDisplayContent.mService.mStackIdToStack.get(3) != null);
        notifyDockedStackMinimizedChanged(this.mMinimizedDock, 0L);
        notifyAdjustedForImeChanged(this.mAdjustedForIme, 0L);
    }

    void setResizeDimLayer(boolean visible, int targetStackId, float alpha) {
        boolean z = false;
        SurfaceControl.openTransaction();
        TaskStack stack = this.mDisplayContent.mService.mStackIdToStack.get(targetStackId);
        TaskStack dockedStack = this.mDisplayContent.getDockedStackLocked();
        if (visible && stack != null && dockedStack != null) {
            z = true;
        }
        boolean visibleAndValid = z;
        if (visibleAndValid) {
            stack.getDimBounds(this.mTmpRect);
            if (this.mTmpRect.height() > 0 && this.mTmpRect.width() > 0) {
                this.mDimLayer.setBounds(this.mTmpRect);
                this.mDimLayer.show(this.mService.mLayersController.getResizeDimLayer(), alpha, 0L);
            } else {
                visibleAndValid = false;
            }
        }
        if (!visibleAndValid) {
            this.mDimLayer.hide();
        }
        SurfaceControl.closeTransaction();
    }

    void notifyAppVisibilityChanged() {
        checkMinimizeChanged(false);
    }

    void notifyAppTransitionStarting() {
        checkMinimizeChanged(true);
    }

    boolean isMinimizedDock() {
        return this.mMinimizedDock;
    }

    private void checkMinimizeChanged(boolean animate) {
        TaskStack homeStack;
        Task homeTask;
        boolean homeBehind;
        boolean z = false;
        if (this.mDisplayContent.getDockedStackVisibleForUserLocked() == null || (homeStack = this.mDisplayContent.getHomeStack()) == null || (homeTask = homeStack.findHomeTask()) == null || !isWithinDisplay(homeTask)) {
            return;
        }
        TaskStack fullscreenStack = this.mService.mStackIdToStack.get(1);
        ArrayList<Task> homeStackTasks = homeStack.getTasks();
        Task topHomeStackTask = homeStackTasks.get(homeStackTasks.size() - 1);
        boolean homeVisible = homeTask.getTopVisibleAppToken() != null;
        if (fullscreenStack != null && fullscreenStack.isVisibleLocked()) {
            homeBehind = true;
        } else {
            homeBehind = homeStackTasks.size() > 1 && topHomeStackTask != homeTask;
        }
        if (homeVisible && !homeBehind) {
            z = true;
        }
        setMinimizedDockedStack(z, animate);
    }

    private boolean isWithinDisplay(Task task) {
        task.mStack.getBounds(this.mTmpRect);
        this.mDisplayContent.getLogicalDisplayRect(this.mTmpRect2);
        return this.mTmpRect.intersect(this.mTmpRect2);
    }

    private void setMinimizedDockedStack(boolean minimizedDock, boolean animate) {
        boolean wasMinimized = this.mMinimizedDock;
        this.mMinimizedDock = minimizedDock;
        if (minimizedDock == wasMinimized) {
            return;
        }
        clearImeAdjustAnimation();
        if (minimizedDock) {
            if (animate) {
                startAdjustAnimation(0.0f, CLIP_REVEAL_MEET_LAST);
                return;
            } else {
                setMinimizedDockedStack(true);
                return;
            }
        }
        if (animate) {
            startAdjustAnimation(CLIP_REVEAL_MEET_LAST, 0.0f);
        } else {
            setMinimizedDockedStack(false);
        }
    }

    private void clearImeAdjustAnimation() {
        ArrayList<TaskStack> stacks = this.mDisplayContent.getStacks();
        for (int i = stacks.size() - 1; i >= 0; i--) {
            TaskStack stack = stacks.get(i);
            if (stack != null && stack.isAdjustedForIme()) {
                stack.resetAdjustedForIme(true);
            }
        }
        this.mAnimatingForIme = false;
    }

    private void startAdjustAnimation(float from, float to) {
        this.mAnimatingForMinimizedDockedStack = true;
        this.mAnimationStarted = false;
        this.mAnimationStart = from;
        this.mAnimationTarget = to;
    }

    private void startImeAdjustAnimation(final boolean adjustedForIme, final boolean adjustedForDivider, WindowState imeWin) {
        if (!this.mAnimatingForIme) {
            this.mAnimationStart = this.mAdjustedForIme ? 1 : 0;
            this.mDividerAnimationStart = this.mAdjustedForDivider ? 1 : 0;
            this.mLastAnimationProgress = this.mAnimationStart;
            this.mLastDividerProgress = this.mDividerAnimationStart;
        } else {
            this.mAnimationStart = this.mLastAnimationProgress;
            this.mDividerAnimationStart = this.mLastDividerProgress;
        }
        this.mAnimatingForIme = true;
        this.mAnimationStarted = false;
        this.mAnimationTarget = adjustedForIme ? 1 : 0;
        this.mDividerAnimationTarget = adjustedForDivider ? 1 : 0;
        ArrayList<TaskStack> stacks = this.mDisplayContent.getStacks();
        for (int i = stacks.size() - 1; i >= 0; i--) {
            TaskStack stack = stacks.get(i);
            if (stack.isVisibleLocked() && stack.isAdjustedForIme()) {
                stack.beginImeAdjustAnimation();
            }
        }
        if (!this.mService.mWaitingForDrawn.isEmpty()) {
            this.mService.mH.removeMessages(24);
            this.mService.mH.sendEmptyMessageDelayed(24, IME_ADJUST_DRAWN_TIMEOUT);
            this.mAnimationStartDelayed = true;
            if (imeWin != null) {
                if (this.mDelayedImeWin != null) {
                    this.mDelayedImeWin.mWinAnimator.endDelayingAnimationStart();
                }
                this.mDelayedImeWin = imeWin;
                imeWin.mWinAnimator.startDelayingAnimationStart();
            }
            this.mService.mWaitingForDrawnCallback = new Runnable() {
                @Override
                public void run() {
                    DockedStackDividerController.this.m3303com_android_server_wm_DockedStackDividerController_lambda$1(adjustedForIme, adjustedForDivider);
                }
            };
            return;
        }
        notifyAdjustedForImeChanged(adjustedForIme ? true : adjustedForDivider, IME_ADJUST_ANIM_DURATION);
    }

    void m3303com_android_server_wm_DockedStackDividerController_lambda$1(boolean adjustedForIme, boolean adjustedForDivider) {
        this.mAnimationStartDelayed = false;
        if (this.mDelayedImeWin != null) {
            this.mDelayedImeWin.mWinAnimator.endDelayingAnimationStart();
        }
        if (adjustedForIme) {
            adjustedForDivider = true;
        }
        notifyAdjustedForImeChanged(adjustedForDivider, IME_ADJUST_ANIM_DURATION);
    }

    private void setMinimizedDockedStack(boolean minimized) {
        TaskStack stack = this.mDisplayContent.getDockedStackVisibleForUserLocked();
        notifyDockedStackMinimizedChanged(minimized, 0L);
        if (stack == null) {
            return;
        }
        if (!stack.setAdjustedForMinimizedDock(minimized ? CLIP_REVEAL_MEET_LAST : 0.0f)) {
            return;
        }
        this.mService.mWindowPlacerLocked.performSurfacePlacement();
    }

    private boolean isAnimationMaximizing() {
        return this.mAnimationTarget == 0.0f;
    }

    public boolean animate(long now) {
        if (this.mWindow == null) {
            return false;
        }
        if (this.mAnimatingForMinimizedDockedStack) {
            return animateForMinimizedDockedStack(now);
        }
        if (this.mAnimatingForIme) {
            return animateForIme(now);
        }
        if (this.mDimLayer != null && this.mDimLayer.isDimming()) {
            this.mDimLayer.setLayer(this.mService.mLayersController.getResizeDimLayer());
        }
        return false;
    }

    private boolean animateForIme(long now) {
        if (!this.mAnimationStarted || this.mAnimationStartDelayed) {
            this.mAnimationStarted = true;
            this.mAnimationStartTime = now;
            this.mAnimationDuration = (long) (this.mService.getWindowAnimationScaleLocked() * 280.0f);
        }
        float t = (this.mAnimationTarget == CLIP_REVEAL_MEET_LAST ? IME_ADJUST_ENTRY_INTERPOLATOR : AppTransition.TOUCH_RESPONSE_INTERPOLATOR).getInterpolation(Math.min(CLIP_REVEAL_MEET_LAST, (now - this.mAnimationStartTime) / this.mAnimationDuration));
        ArrayList<TaskStack> stacks = this.mDisplayContent.getStacks();
        boolean updated = false;
        for (int i = stacks.size() - 1; i >= 0; i--) {
            TaskStack stack = stacks.get(i);
            if (stack != null && stack.isAdjustedForIme()) {
                if (t >= CLIP_REVEAL_MEET_LAST && this.mAnimationTarget == 0.0f && this.mDividerAnimationTarget == 0.0f) {
                    stack.resetAdjustedForIme(true);
                    updated = true;
                } else {
                    this.mLastAnimationProgress = getInterpolatedAnimationValue(t);
                    this.mLastDividerProgress = getInterpolatedDividerValue(t);
                    updated |= stack.updateAdjustForIme(this.mLastAnimationProgress, this.mLastDividerProgress, false);
                }
                if (t >= CLIP_REVEAL_MEET_LAST) {
                    stack.endImeAdjustAnimation();
                }
            }
        }
        if (updated) {
            this.mService.mWindowPlacerLocked.performSurfacePlacement();
        }
        if (t < CLIP_REVEAL_MEET_LAST) {
            return true;
        }
        this.mLastAnimationProgress = this.mAnimationTarget;
        this.mLastDividerProgress = this.mDividerAnimationTarget;
        this.mAnimatingForIme = false;
        return false;
    }

    private boolean animateForMinimizedDockedStack(long now) {
        long transitionDuration;
        TaskStack stack = this.mService.mStackIdToStack.get(3);
        if (!this.mAnimationStarted) {
            this.mAnimationStarted = true;
            this.mAnimationStartTime = now;
            if (isAnimationMaximizing()) {
                transitionDuration = this.mService.mAppTransition.getLastClipRevealTransitionDuration();
            } else {
                transitionDuration = 336;
            }
            this.mAnimationDuration = (long) (transitionDuration * this.mService.getTransitionAnimationScaleLocked());
            this.mMaximizeMeetFraction = getClipRevealMeetFraction(stack);
            notifyDockedStackMinimizedChanged(this.mMinimizedDock, (long) (this.mAnimationDuration * this.mMaximizeMeetFraction));
        }
        float t = (isAnimationMaximizing() ? AppTransition.TOUCH_RESPONSE_INTERPOLATOR : this.mMinimizedDockInterpolator).getInterpolation(Math.min(CLIP_REVEAL_MEET_LAST, (now - this.mAnimationStartTime) / this.mAnimationDuration));
        if (stack != null && stack.setAdjustedForMinimizedDock(getMinimizeAmount(stack, t))) {
            this.mService.mWindowPlacerLocked.performSurfacePlacement();
        }
        if (t < CLIP_REVEAL_MEET_LAST) {
            return true;
        }
        this.mAnimatingForMinimizedDockedStack = false;
        return false;
    }

    private float getInterpolatedAnimationValue(float t) {
        return (this.mAnimationTarget * t) + ((CLIP_REVEAL_MEET_LAST - t) * this.mAnimationStart);
    }

    private float getInterpolatedDividerValue(float t) {
        return (this.mDividerAnimationTarget * t) + ((CLIP_REVEAL_MEET_LAST - t) * this.mDividerAnimationStart);
    }

    private float getMinimizeAmount(TaskStack stack, float t) {
        float naturalAmount = getInterpolatedAnimationValue(t);
        if (isAnimationMaximizing()) {
            return adjustMaximizeAmount(stack, t, naturalAmount);
        }
        return naturalAmount;
    }

    private float adjustMaximizeAmount(TaskStack stack, float t, float naturalAmount) {
        if (this.mMaximizeMeetFraction == CLIP_REVEAL_MEET_LAST) {
            return naturalAmount;
        }
        int minimizeDistance = stack.getMinimizeDistance();
        float startPrime = this.mService.mAppTransition.getLastClipRevealMaxTranslation() / minimizeDistance;
        float amountPrime = (this.mAnimationTarget * t) + ((CLIP_REVEAL_MEET_LAST - t) * startPrime);
        float t2 = Math.min(t / this.mMaximizeMeetFraction, CLIP_REVEAL_MEET_LAST);
        return (amountPrime * t2) + ((CLIP_REVEAL_MEET_LAST - t2) * naturalAmount);
    }

    private float getClipRevealMeetFraction(TaskStack stack) {
        if (!isAnimationMaximizing() || stack == null || !this.mService.mAppTransition.hadClipRevealAnimation()) {
            return CLIP_REVEAL_MEET_LAST;
        }
        int minimizeDistance = stack.getMinimizeDistance();
        float fraction = Math.abs(this.mService.mAppTransition.getLastClipRevealMaxTranslation()) / minimizeDistance;
        float t = Math.max(0.0f, Math.min(CLIP_REVEAL_MEET_LAST, (fraction - CLIP_REVEAL_MEET_FRACTION_MIN) / CLIP_REVEAL_MEET_FRACTION_MIN));
        return ((CLIP_REVEAL_MEET_LAST - t) * 0.39999998f) + 0.6f;
    }

    @Override
    public boolean dimFullscreen() {
        return false;
    }

    @Override
    public DisplayInfo getDisplayInfo() {
        return this.mDisplayContent.getDisplayInfo();
    }

    @Override
    public void getDimBounds(Rect outBounds) {
    }

    @Override
    public String toShortString() {
        return TAG;
    }

    WindowState getWindow() {
        return this.mWindow;
    }

    void dump(String prefix, PrintWriter pw) {
        pw.println(prefix + "DockedStackDividerController");
        pw.println(prefix + "  mLastVisibility=" + this.mLastVisibility);
        pw.println(prefix + "  mMinimizedDock=" + this.mMinimizedDock);
        pw.println(prefix + "  mAdjustedForIme=" + this.mAdjustedForIme);
        pw.println(prefix + "  mAdjustedForDivider=" + this.mAdjustedForDivider);
        if (!this.mDimLayer.isDimming()) {
            return;
        }
        pw.println(prefix + "  Dim layer is dimming: ");
        this.mDimLayer.printTo(prefix + "    ", pw);
    }
}
