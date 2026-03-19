package com.android.server.wm;

import android.R;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.ArraySet;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.IDockedStackListener;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.InputMethodManagerInternal;
import com.android.internal.policy.DividerSnapAlgorithm;
import com.android.internal.policy.DockedDividerUtils;
import com.android.server.LocalServices;
import java.io.PrintWriter;
import java.util.function.Consumer;

public class DockedStackDividerController {
    private static final float CLIP_REVEAL_MEET_EARLIEST = 0.6f;
    private static final float CLIP_REVEAL_MEET_FRACTION_MAX = 0.8f;
    private static final float CLIP_REVEAL_MEET_FRACTION_MIN = 0.4f;
    private static final float CLIP_REVEAL_MEET_LAST = 1.0f;
    private static final int DIVIDER_WIDTH_INACTIVE_DP = 4;
    private static final long IME_ADJUST_ANIM_DURATION = 280;
    private static final long IME_ADJUST_DRAWN_TIMEOUT = 200;
    private static final Interpolator IME_ADJUST_ENTRY_INTERPOLATOR = new PathInterpolator(0.2f, 0.0f, 0.1f, 1.0f);
    private static final String TAG = "WindowManager";
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
    private TaskStack mDimmedStack;
    private final DisplayContent mDisplayContent;
    private float mDividerAnimationStart;
    private float mDividerAnimationTarget;
    private int mDividerInsets;
    private int mDividerWindowWidth;
    private int mDividerWindowWidthInactive;
    private int mImeHeight;
    private boolean mImeHideRequested;
    float mLastAnimationProgress;
    private float mLastDimLayerAlpha;
    float mLastDividerProgress;
    private float mMaximizeMeetFraction;
    private boolean mMinimizedDock;
    private final Interpolator mMinimizedDockInterpolator;
    private boolean mResizing;
    private final WindowManagerService mService;
    private int mTaskHeightInMinimizedMode;
    private WindowState mWindow;
    private final Rect mTmpRect = new Rect();
    private final Rect mTmpRect2 = new Rect();
    private final Rect mTmpRect3 = new Rect();
    private final Rect mLastRect = new Rect();
    private boolean mLastVisibility = false;
    private final RemoteCallbackList<IDockedStackListener> mDockedStackListeners = new RemoteCallbackList<>();
    private int mOriginalDockedSide = -1;
    private final Rect mTouchRegion = new Rect();
    private final DividerSnapAlgorithm[] mSnapAlgorithmForRotation = new DividerSnapAlgorithm[4];
    private final Rect mLastDimLayerRect = new Rect();

    DockedStackDividerController(WindowManagerService windowManagerService, DisplayContent displayContent) {
        this.mService = windowManagerService;
        this.mDisplayContent = displayContent;
        this.mMinimizedDockInterpolator = AnimationUtils.loadInterpolator(windowManagerService.mContext, R.interpolator.fast_out_slow_in);
        loadDimens();
    }

    int getSmallestWidthDpForBounds(Rect rect) {
        DisplayInfo displayInfo = this.mDisplayContent.getDisplayInfo();
        int i = this.mDisplayContent.mBaseDisplayWidth;
        int i2 = this.mDisplayContent.mBaseDisplayHeight;
        int iMin = Integer.MAX_VALUE;
        int i3 = 0;
        while (i3 < 4) {
            this.mTmpRect.set(rect);
            this.mDisplayContent.rotateBounds(displayInfo.rotation, i3, this.mTmpRect);
            int i4 = 1;
            boolean z = i3 == 1 || i3 == 3;
            this.mTmpRect2.set(0, 0, z ? i2 : i, z ? i : i2);
            if (this.mTmpRect2.width() > this.mTmpRect2.height()) {
                i4 = 2;
            }
            int dockSide = getDockSide(this.mTmpRect, this.mTmpRect2, i4);
            int iCalculatePositionForBounds = DockedDividerUtils.calculatePositionForBounds(this.mTmpRect, dockSide, getContentWidth());
            DisplayCutout displayCutout = this.mDisplayContent.calculateDisplayCutoutForRotation(i3).getDisplayCutout();
            DockedDividerUtils.calculateBoundsForPosition(this.mSnapAlgorithmForRotation[i3].calculateNonDismissingSnapTarget(iCalculatePositionForBounds).position, dockSide, this.mTmpRect, this.mTmpRect2.width(), this.mTmpRect2.height(), getContentWidth());
            this.mService.mPolicy.getStableInsetsLw(i3, this.mTmpRect2.width(), this.mTmpRect2.height(), displayCutout, this.mTmpRect3);
            this.mService.intersectDisplayInsetBounds(this.mTmpRect2, this.mTmpRect3, this.mTmpRect);
            iMin = Math.min(this.mTmpRect.width(), iMin);
            i3++;
        }
        return (int) (iMin / this.mDisplayContent.getDisplayMetrics().density);
    }

    int getDockSide(Rect rect, Rect rect2, int i) {
        if (i == 1) {
            int i2 = (rect2.bottom - rect.bottom) - (rect.top - rect2.top);
            if (i2 > 0) {
                return 2;
            }
            if (i2 >= 0 && canPrimaryStackDockTo(2)) {
                return 2;
            }
            return 4;
        }
        if (i == 2) {
            int i3 = (rect2.right - rect.right) - (rect.left - rect2.left);
            if (i3 > 0) {
                return 1;
            }
            return (i3 >= 0 && canPrimaryStackDockTo(1)) ? 1 : 3;
        }
        return -1;
    }

    void getHomeStackBoundsInDockedMode(Rect rect) {
        DisplayInfo displayInfo = this.mDisplayContent.getDisplayInfo();
        this.mService.mPolicy.getStableInsetsLw(displayInfo.rotation, displayInfo.logicalWidth, displayInfo.logicalHeight, displayInfo.displayCutout, this.mTmpRect);
        int i = this.mDividerWindowWidth - (2 * this.mDividerInsets);
        if (this.mDisplayContent.getConfiguration().orientation == 1) {
            rect.set(0, this.mTaskHeightInMinimizedMode + i + this.mTmpRect.top, displayInfo.logicalWidth, displayInfo.logicalHeight);
            return;
        }
        TaskStack splitScreenPrimaryStackIgnoringVisibility = this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility();
        int i2 = this.mTaskHeightInMinimizedMode + i + this.mTmpRect.top;
        int i3 = this.mTmpRect.left;
        int i4 = displayInfo.logicalWidth - this.mTmpRect.right;
        if (splitScreenPrimaryStackIgnoringVisibility != null) {
            if (splitScreenPrimaryStackIgnoringVisibility.getDockSide() == 1) {
                i3 += i2;
            } else if (splitScreenPrimaryStackIgnoringVisibility.getDockSide() == 3) {
                i4 -= i2;
            }
        }
        rect.set(i3, 0, i4, displayInfo.logicalHeight);
    }

    boolean isHomeStackResizable() {
        Task taskFindHomeTask;
        TaskStack homeStack = this.mDisplayContent.getHomeStack();
        return (homeStack == null || (taskFindHomeTask = homeStack.findHomeTask()) == null || !taskFindHomeTask.isResizeable()) ? false : true;
    }

    private void initSnapAlgorithmForRotations() {
        int i;
        int i2;
        boolean z;
        Configuration configuration = this.mDisplayContent.getConfiguration();
        Configuration configuration2 = new Configuration();
        int i3 = 0;
        while (i3 < 4) {
            boolean z2 = i3 == 1 || i3 == 3;
            if (z2) {
                i = this.mDisplayContent.mBaseDisplayHeight;
            } else {
                i = this.mDisplayContent.mBaseDisplayWidth;
            }
            int i4 = i;
            if (z2) {
                i2 = this.mDisplayContent.mBaseDisplayWidth;
            } else {
                i2 = this.mDisplayContent.mBaseDisplayHeight;
            }
            int i5 = i2;
            DisplayCutout displayCutout = this.mDisplayContent.calculateDisplayCutoutForRotation(i3).getDisplayCutout();
            this.mService.mPolicy.getStableInsetsLw(i3, i4, i5, displayCutout, this.mTmpRect);
            configuration2.unset();
            configuration2.orientation = i4 <= i5 ? 1 : 2;
            int displayId = this.mDisplayContent.getDisplayId();
            int i6 = i3;
            int nonDecorDisplayWidth = this.mService.mPolicy.getNonDecorDisplayWidth(i4, i5, i6, configuration.uiMode, displayId, displayCutout);
            int nonDecorDisplayHeight = this.mService.mPolicy.getNonDecorDisplayHeight(i4, i5, i6, configuration.uiMode, displayId, displayCutout);
            this.mService.mPolicy.getNonDecorInsetsLw(i3, i4, i5, displayCutout, this.mTmpRect);
            int i7 = this.mTmpRect.left;
            int i8 = this.mTmpRect.top;
            configuration2.windowConfiguration.setAppBounds(i7, i8, i7 + nonDecorDisplayWidth, nonDecorDisplayHeight + i8);
            float f = this.mDisplayContent.getDisplayMetrics().density;
            int i9 = i3;
            configuration2.screenWidthDp = (int) (this.mService.mPolicy.getConfigDisplayWidth(i4, i5, i9, configuration.uiMode, displayId, displayCutout) / f);
            configuration2.screenHeightDp = (int) (this.mService.mPolicy.getConfigDisplayHeight(i4, i5, i9, configuration.uiMode, displayId, displayCutout) / f);
            Context contextCreateConfigurationContext = this.mService.mContext.createConfigurationContext(configuration2);
            DividerSnapAlgorithm[] dividerSnapAlgorithmArr = this.mSnapAlgorithmForRotation;
            Resources resources = contextCreateConfigurationContext.getResources();
            int contentWidth = getContentWidth();
            if (configuration2.orientation != 1) {
                z = false;
            } else {
                z = true;
            }
            dividerSnapAlgorithmArr[i3] = new DividerSnapAlgorithm(resources, i4, i5, contentWidth, z, this.mTmpRect);
            i3++;
        }
    }

    private void loadDimens() {
        Context context = this.mService.mContext;
        this.mDividerWindowWidth = context.getResources().getDimensionPixelSize(R.dimen.car_borderless_button_horizontal_padding);
        this.mDividerInsets = context.getResources().getDimensionPixelSize(R.dimen.car_body5_size);
        this.mDividerWindowWidthInactive = WindowManagerService.dipToPixel(4, this.mDisplayContent.getDisplayMetrics());
        this.mTaskHeightInMinimizedMode = context.getResources().getDimensionPixelSize(R.dimen.harmful_app_message_line_spacing_modifier);
        initSnapAlgorithmForRotations();
    }

    void onConfigurationChanged() {
        loadDimens();
    }

    boolean isResizing() {
        return this.mResizing;
    }

    int getContentWidth() {
        return this.mDividerWindowWidth - (2 * this.mDividerInsets);
    }

    int getContentInsets() {
        return this.mDividerInsets;
    }

    int getContentWidthInactive() {
        return this.mDividerWindowWidthInactive;
    }

    void setResizing(boolean z) {
        if (this.mResizing != z) {
            this.mResizing = z;
            resetDragResizingChangeReported();
        }
    }

    void setTouchRegion(Rect rect) {
        this.mTouchRegion.set(rect);
    }

    void getTouchRegion(Rect rect) {
        rect.set(this.mTouchRegion);
        rect.offset(this.mWindow.getFrameLw().left, this.mWindow.getFrameLw().top);
    }

    private void resetDragResizingChangeReported() {
        this.mDisplayContent.forAllWindows((Consumer<WindowState>) new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((WindowState) obj).resetDragResizingChangeReported();
            }
        }, true);
    }

    void setWindow(WindowState windowState) {
        this.mWindow = windowState;
        reevaluateVisibility(false);
    }

    void reevaluateVisibility(boolean z) {
        if (this.mWindow == null) {
            return;
        }
        boolean z2 = this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility() != null;
        if (this.mLastVisibility == z2 && !z) {
            return;
        }
        this.mLastVisibility = z2;
        notifyDockedDividerVisibilityChanged(z2);
        if (!z2) {
            setResizeDimLayer(false, 0, 0.0f);
        }
    }

    private boolean wasVisible() {
        return this.mLastVisibility;
    }

    void setAdjustedForIme(boolean z, boolean z2, boolean z3, WindowState windowState, int i) {
        if (this.mAdjustedForIme != z || ((z && this.mImeHeight != i) || this.mAdjustedForDivider != z2)) {
            if (z3 && !this.mAnimatingForMinimizedDockedStack) {
                startImeAdjustAnimation(z, z2, windowState);
            } else {
                notifyAdjustedForImeChanged(z || z2, 0L);
            }
            this.mAdjustedForIme = z;
            this.mImeHeight = i;
            this.mAdjustedForDivider = z2;
        }
    }

    int getImeHeightAdjustedFor() {
        return this.mImeHeight;
    }

    void positionDockedStackedDivider(Rect rect) {
        TaskStack splitScreenPrimaryStackIgnoringVisibility = this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility();
        if (splitScreenPrimaryStackIgnoringVisibility == null) {
            rect.set(this.mLastRect);
            return;
        }
        splitScreenPrimaryStackIgnoringVisibility.getDimBounds(this.mTmpRect);
        switch (splitScreenPrimaryStackIgnoringVisibility.getDockSide()) {
            case 1:
                rect.set(this.mTmpRect.right - this.mDividerInsets, rect.top, (this.mTmpRect.right + rect.width()) - this.mDividerInsets, rect.bottom);
                break;
            case 2:
                rect.set(rect.left, this.mTmpRect.bottom - this.mDividerInsets, this.mTmpRect.right, (this.mTmpRect.bottom + rect.height()) - this.mDividerInsets);
                break;
            case 3:
                rect.set((this.mTmpRect.left - rect.width()) + this.mDividerInsets, rect.top, this.mTmpRect.left + this.mDividerInsets, rect.bottom);
                break;
            case 4:
                rect.set(rect.left, (this.mTmpRect.top - rect.height()) + this.mDividerInsets, rect.right, this.mTmpRect.top + this.mDividerInsets);
                break;
        }
        this.mLastRect.set(rect);
    }

    private void notifyDockedDividerVisibilityChanged(boolean z) {
        int iBeginBroadcast = this.mDockedStackListeners.beginBroadcast();
        for (int i = 0; i < iBeginBroadcast; i++) {
            try {
                this.mDockedStackListeners.getBroadcastItem(i).onDividerVisibilityChanged(z);
            } catch (RemoteException e) {
                Slog.e("WindowManager", "Error delivering divider visibility changed event.", e);
            }
        }
        this.mDockedStackListeners.finishBroadcast();
    }

    boolean canPrimaryStackDockTo(int i) {
        DisplayInfo displayInfo = this.mDisplayContent.getDisplayInfo();
        return this.mService.mPolicy.isDockSideAllowed(i, this.mOriginalDockedSide, displayInfo.logicalWidth, displayInfo.logicalHeight, displayInfo.rotation);
    }

    void notifyDockedStackExistsChanged(boolean z) {
        int iBeginBroadcast = this.mDockedStackListeners.beginBroadcast();
        for (int i = 0; i < iBeginBroadcast; i++) {
            try {
                this.mDockedStackListeners.getBroadcastItem(i).onDockedStackExistsChanged(z);
            } catch (RemoteException e) {
                Slog.e("WindowManager", "Error delivering docked stack exists changed event.", e);
            }
        }
        this.mDockedStackListeners.finishBroadcast();
        if (z) {
            InputMethodManagerInternal inputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
            if (inputMethodManagerInternal != null) {
                inputMethodManagerInternal.hideCurrentInputMethod();
                this.mImeHideRequested = true;
            }
            this.mOriginalDockedSide = this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility().getDockSideForDisplay(this.mDisplayContent);
            return;
        }
        this.mOriginalDockedSide = -1;
        setMinimizedDockedStack(false, false);
        if (this.mDimmedStack != null) {
            this.mDimmedStack.stopDimming();
            this.mDimmedStack = null;
        }
    }

    void resetImeHideRequested() {
        this.mImeHideRequested = false;
    }

    boolean isImeHideRequested() {
        return this.mImeHideRequested;
    }

    private void notifyDockedStackMinimizedChanged(boolean z, boolean z2, boolean z3) {
        long j;
        long lastClipRevealTransitionDuration;
        if (z2) {
            TaskStack splitScreenPrimaryStackIgnoringVisibility = this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility();
            if (isAnimationMaximizing()) {
                lastClipRevealTransitionDuration = this.mService.mAppTransition.getLastClipRevealTransitionDuration();
            } else {
                lastClipRevealTransitionDuration = 336;
            }
            this.mAnimationDuration = (long) (lastClipRevealTransitionDuration * this.mService.getTransitionAnimationScaleLocked());
            this.mMaximizeMeetFraction = getClipRevealMeetFraction(splitScreenPrimaryStackIgnoringVisibility);
            j = (long) (this.mAnimationDuration * this.mMaximizeMeetFraction);
        } else {
            j = 0;
        }
        this.mService.mH.removeMessages(53);
        this.mService.mH.obtainMessage(53, z ? 1 : 0, 0).sendToTarget();
        int iBeginBroadcast = this.mDockedStackListeners.beginBroadcast();
        for (int i = 0; i < iBeginBroadcast; i++) {
            try {
                this.mDockedStackListeners.getBroadcastItem(i).onDockedStackMinimizedChanged(z, j, z3);
            } catch (RemoteException e) {
                Slog.e("WindowManager", "Error delivering minimized dock changed event.", e);
            }
        }
        this.mDockedStackListeners.finishBroadcast();
    }

    void notifyDockSideChanged(int i) {
        int iBeginBroadcast = this.mDockedStackListeners.beginBroadcast();
        for (int i2 = 0; i2 < iBeginBroadcast; i2++) {
            try {
                this.mDockedStackListeners.getBroadcastItem(i2).onDockSideChanged(i);
            } catch (RemoteException e) {
                Slog.e("WindowManager", "Error delivering dock side changed event.", e);
            }
        }
        this.mDockedStackListeners.finishBroadcast();
    }

    private void notifyAdjustedForImeChanged(boolean z, long j) {
        int iBeginBroadcast = this.mDockedStackListeners.beginBroadcast();
        for (int i = 0; i < iBeginBroadcast; i++) {
            try {
                this.mDockedStackListeners.getBroadcastItem(i).onAdjustedForImeChanged(z, j);
            } catch (RemoteException e) {
                Slog.e("WindowManager", "Error delivering adjusted for ime changed event.", e);
            }
        }
        this.mDockedStackListeners.finishBroadcast();
    }

    void registerDockedStackListener(IDockedStackListener iDockedStackListener) {
        this.mDockedStackListeners.register(iDockedStackListener);
        notifyDockedDividerVisibilityChanged(wasVisible());
        notifyDockedStackExistsChanged(this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility() != null);
        notifyDockedStackMinimizedChanged(this.mMinimizedDock, false, isHomeStackResizable());
        notifyAdjustedForImeChanged(this.mAdjustedForIme, 0L);
    }

    void setResizeDimLayer(boolean z, int i, float f) {
        TaskStack topStackInWindowingMode;
        if (i != 0) {
            topStackInWindowingMode = this.mDisplayContent.getTopStackInWindowingMode(i);
        } else {
            topStackInWindowingMode = null;
        }
        boolean z2 = (!z || topStackInWindowingMode == null || this.mDisplayContent.getSplitScreenPrimaryStack() == null) ? false : true;
        if (this.mDimmedStack != null && this.mDimmedStack != topStackInWindowingMode) {
            this.mDimmedStack.stopDimming();
            this.mDimmedStack = null;
        }
        if (z2) {
            this.mDimmedStack = topStackInWindowingMode;
            topStackInWindowingMode.dim(f);
        }
        if (!z2 && topStackInWindowingMode != null) {
            this.mDimmedStack = null;
            topStackInWindowingMode.stopDimming();
        }
    }

    private int getResizeDimLayer() {
        if (this.mWindow != null) {
            return this.mWindow.mLayer - 1;
        }
        return 1;
    }

    void notifyAppVisibilityChanged() {
        checkMinimizeChanged(false);
    }

    void notifyAppTransitionStarting(ArraySet<AppWindowToken> arraySet, int i) {
        boolean z = this.mMinimizedDock;
        checkMinimizeChanged(true);
        if (z && this.mMinimizedDock && containsAppInDockedStack(arraySet) && i != 0 && !AppTransition.isKeyguardGoingAwayTransit(i) && !this.mService.mAmInternal.isRecentsComponentHomeActivity(this.mService.mCurrentUserId)) {
            this.mService.showRecentApps();
        }
    }

    private boolean containsAppInDockedStack(ArraySet<AppWindowToken> arraySet) {
        for (int size = arraySet.size() - 1; size >= 0; size--) {
            AppWindowToken appWindowTokenValueAt = arraySet.valueAt(size);
            if (appWindowTokenValueAt.getTask() != null && appWindowTokenValueAt.inSplitScreenPrimaryWindowingMode()) {
                return true;
            }
        }
        return false;
    }

    boolean isMinimizedDock() {
        return this.mMinimizedDock;
    }

    void checkMinimizeChanged(boolean z) {
        TaskStack homeStack;
        Task taskFindHomeTask;
        if (this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility() == null || (homeStack = this.mDisplayContent.getHomeStack()) == null || (taskFindHomeTask = homeStack.findHomeTask()) == null || !isWithinDisplay(taskFindHomeTask)) {
            return;
        }
        if (this.mMinimizedDock && this.mService.mKeyguardOrAodShowingOnDefaultDisplay) {
            return;
        }
        TaskStack topStackInWindowingMode = this.mDisplayContent.getTopStackInWindowingMode(4);
        RecentsAnimationController recentsAnimationController = this.mService.getRecentsAnimationController();
        boolean z2 = recentsAnimationController != null && recentsAnimationController.isSplitScreenMinimized();
        boolean z3 = taskFindHomeTask.getTopVisibleAppToken() != null;
        if (z3 && topStackInWindowingMode != null) {
            z3 = homeStack.compareTo((WindowContainer) topStackInWindowingMode) >= 0;
        }
        setMinimizedDockedStack(z3 || z2, z);
    }

    private boolean isWithinDisplay(Task task) {
        task.getBounds(this.mTmpRect);
        this.mDisplayContent.getBounds(this.mTmpRect2);
        return this.mTmpRect.intersect(this.mTmpRect2);
    }

    private void setMinimizedDockedStack(boolean z, boolean z2) {
        boolean z3 = this.mMinimizedDock;
        this.mMinimizedDock = z;
        if (z == z3) {
            return;
        }
        boolean zClearImeAdjustAnimation = clearImeAdjustAnimation();
        boolean minimizedDockedStack = false;
        if (isHomeStackResizable()) {
            notifyDockedStackMinimizedChanged(z, z2, true);
            minimizedDockedStack = true;
        } else if (z) {
            if (z2) {
                startAdjustAnimation(0.0f, 1.0f);
            } else {
                minimizedDockedStack = false | setMinimizedDockedStack(true);
            }
        } else if (z2) {
            startAdjustAnimation(1.0f, 0.0f);
        } else {
            minimizedDockedStack = false | setMinimizedDockedStack(false);
        }
        if (zClearImeAdjustAnimation || minimizedDockedStack) {
            if (zClearImeAdjustAnimation && !minimizedDockedStack) {
                Slog.d("WindowManager", "setMinimizedDockedStack: IME adjust changed due to minimizing, minimizedDock=" + z + " minimizedChange=" + minimizedDockedStack);
            }
            this.mService.mWindowPlacerLocked.performSurfacePlacement();
        }
    }

    private boolean clearImeAdjustAnimation() {
        boolean zClearImeAdjustAnimation = this.mDisplayContent.clearImeAdjustAnimation();
        this.mAnimatingForIme = false;
        return zClearImeAdjustAnimation;
    }

    private void startAdjustAnimation(float f, float f2) {
        this.mAnimatingForMinimizedDockedStack = true;
        this.mAnimationStarted = false;
        this.mAnimationStart = f;
        this.mAnimationTarget = f2;
    }

    private void startImeAdjustAnimation(final boolean z, final boolean z2, WindowState windowState) {
        if (!this.mAnimatingForIme) {
            this.mAnimationStart = this.mAdjustedForIme ? 1.0f : 0.0f;
            this.mDividerAnimationStart = this.mAdjustedForDivider ? 1.0f : 0.0f;
            this.mLastAnimationProgress = this.mAnimationStart;
            this.mLastDividerProgress = this.mDividerAnimationStart;
        } else {
            this.mAnimationStart = this.mLastAnimationProgress;
            this.mDividerAnimationStart = this.mLastDividerProgress;
        }
        boolean z3 = true;
        this.mAnimatingForIme = true;
        this.mAnimationStarted = false;
        this.mAnimationTarget = z ? 1.0f : 0.0f;
        this.mDividerAnimationTarget = z2 ? 1.0f : 0.0f;
        this.mDisplayContent.beginImeAdjustAnimation();
        if (!this.mService.mWaitingForDrawn.isEmpty()) {
            this.mService.mH.removeMessages(24);
            this.mService.mH.sendEmptyMessageDelayed(24, IME_ADJUST_DRAWN_TIMEOUT);
            this.mAnimationStartDelayed = true;
            if (windowState != null) {
                if (this.mDelayedImeWin != null) {
                    this.mDelayedImeWin.endDelayingAnimationStart();
                }
                this.mDelayedImeWin = windowState;
                windowState.startDelayingAnimationStart();
            }
            if (this.mService.mWaitingForDrawnCallback != null) {
                this.mService.mWaitingForDrawnCallback.run();
            }
            this.mService.mWaitingForDrawnCallback = new Runnable() {
                @Override
                public final void run() {
                    DockedStackDividerController.lambda$startImeAdjustAnimation$0(this.f$0, z, z2);
                }
            };
            return;
        }
        if (!z && !z2) {
            z3 = false;
        }
        notifyAdjustedForImeChanged(z3, IME_ADJUST_ANIM_DURATION);
    }

    public static void lambda$startImeAdjustAnimation$0(DockedStackDividerController dockedStackDividerController, boolean z, boolean z2) {
        synchronized (dockedStackDividerController.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                boolean z3 = false;
                dockedStackDividerController.mAnimationStartDelayed = false;
                if (dockedStackDividerController.mDelayedImeWin != null) {
                    dockedStackDividerController.mDelayedImeWin.endDelayingAnimationStart();
                }
                long j = 0;
                if (dockedStackDividerController.mAdjustedForIme == z && dockedStackDividerController.mAdjustedForDivider == z2) {
                    j = IME_ADJUST_ANIM_DURATION;
                } else {
                    Slog.w("WindowManager", "IME adjust changed while waiting for drawn: adjustedForIme=" + z + " adjustedForDivider=" + z2 + " mAdjustedForIme=" + dockedStackDividerController.mAdjustedForIme + " mAdjustedForDivider=" + dockedStackDividerController.mAdjustedForDivider);
                }
                if (dockedStackDividerController.mAdjustedForIme || dockedStackDividerController.mAdjustedForDivider) {
                    z3 = true;
                }
                dockedStackDividerController.notifyAdjustedForImeChanged(z3, j);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    private boolean setMinimizedDockedStack(boolean z) {
        TaskStack splitScreenPrimaryStackIgnoringVisibility = this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility();
        notifyDockedStackMinimizedChanged(z, false, isHomeStackResizable());
        if (splitScreenPrimaryStackIgnoringVisibility != null) {
            return splitScreenPrimaryStackIgnoringVisibility.setAdjustedForMinimizedDock(z ? 1.0f : 0.0f);
        }
        return false;
    }

    private boolean isAnimationMaximizing() {
        return this.mAnimationTarget == 0.0f;
    }

    public boolean animate(long j) {
        if (this.mWindow == null) {
            return false;
        }
        if (this.mAnimatingForMinimizedDockedStack) {
            return animateForMinimizedDockedStack(j);
        }
        if (this.mAnimatingForIme) {
            return animateForIme(j);
        }
        return false;
    }

    private boolean animateForIme(long j) {
        if (!this.mAnimationStarted || this.mAnimationStartDelayed) {
            this.mAnimationStarted = true;
            this.mAnimationStartTime = j;
            this.mAnimationDuration = (long) (280.0f * this.mService.getWindowAnimationScaleLocked());
        }
        float interpolation = (this.mAnimationTarget == 1.0f ? IME_ADJUST_ENTRY_INTERPOLATOR : AppTransition.TOUCH_RESPONSE_INTERPOLATOR).getInterpolation(Math.min(1.0f, (j - this.mAnimationStartTime) / this.mAnimationDuration));
        if (this.mDisplayContent.animateForIme(interpolation, this.mAnimationTarget, this.mDividerAnimationTarget)) {
            this.mService.mWindowPlacerLocked.performSurfacePlacement();
        }
        if (interpolation < 1.0f) {
            return true;
        }
        this.mLastAnimationProgress = this.mAnimationTarget;
        this.mLastDividerProgress = this.mDividerAnimationTarget;
        this.mAnimatingForIme = false;
        return false;
    }

    private boolean animateForMinimizedDockedStack(long j) {
        TaskStack splitScreenPrimaryStackIgnoringVisibility = this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility();
        if (!this.mAnimationStarted) {
            this.mAnimationStarted = true;
            this.mAnimationStartTime = j;
            notifyDockedStackMinimizedChanged(this.mMinimizedDock, true, isHomeStackResizable());
        }
        float interpolation = (isAnimationMaximizing() ? AppTransition.TOUCH_RESPONSE_INTERPOLATOR : this.mMinimizedDockInterpolator).getInterpolation(Math.min(1.0f, (j - this.mAnimationStartTime) / this.mAnimationDuration));
        if (splitScreenPrimaryStackIgnoringVisibility != null && splitScreenPrimaryStackIgnoringVisibility.setAdjustedForMinimizedDock(getMinimizeAmount(splitScreenPrimaryStackIgnoringVisibility, interpolation))) {
            this.mService.mWindowPlacerLocked.performSurfacePlacement();
        }
        if (interpolation < 1.0f) {
            return true;
        }
        this.mAnimatingForMinimizedDockedStack = false;
        return false;
    }

    float getInterpolatedAnimationValue(float f) {
        return (this.mAnimationTarget * f) + ((1.0f - f) * this.mAnimationStart);
    }

    float getInterpolatedDividerValue(float f) {
        return (this.mDividerAnimationTarget * f) + ((1.0f - f) * this.mDividerAnimationStart);
    }

    private float getMinimizeAmount(TaskStack taskStack, float f) {
        float interpolatedAnimationValue = getInterpolatedAnimationValue(f);
        if (isAnimationMaximizing()) {
            return adjustMaximizeAmount(taskStack, f, interpolatedAnimationValue);
        }
        return interpolatedAnimationValue;
    }

    private float adjustMaximizeAmount(TaskStack taskStack, float f, float f2) {
        if (this.mMaximizeMeetFraction == 1.0f) {
            return f2;
        }
        float lastClipRevealMaxTranslation = (this.mAnimationTarget * f) + ((1.0f - f) * (this.mService.mAppTransition.getLastClipRevealMaxTranslation() / taskStack.getMinimizeDistance()));
        float fMin = Math.min(f / this.mMaximizeMeetFraction, 1.0f);
        return (lastClipRevealMaxTranslation * fMin) + (f2 * (1.0f - fMin));
    }

    private float getClipRevealMeetFraction(TaskStack taskStack) {
        if (!isAnimationMaximizing() || taskStack == null || !this.mService.mAppTransition.hadClipRevealAnimation()) {
            return 1.0f;
        }
        return CLIP_REVEAL_MEET_EARLIEST + ((1.0f - Math.max(0.0f, Math.min(1.0f, ((Math.abs(this.mService.mAppTransition.getLastClipRevealMaxTranslation()) / taskStack.getMinimizeDistance()) - CLIP_REVEAL_MEET_FRACTION_MIN) / CLIP_REVEAL_MEET_FRACTION_MIN))) * 0.39999998f);
    }

    public String toShortString() {
        return "WindowManager";
    }

    WindowState getWindow() {
        return this.mWindow;
    }

    void dump(String str, PrintWriter printWriter) {
        printWriter.println(str + "DockedStackDividerController");
        printWriter.println(str + "  mLastVisibility=" + this.mLastVisibility);
        printWriter.println(str + "  mMinimizedDock=" + this.mMinimizedDock);
        printWriter.println(str + "  mAdjustedForIme=" + this.mAdjustedForIme);
        printWriter.println(str + "  mAdjustedForDivider=" + this.mAdjustedForDivider);
    }

    void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1133871366145L, this.mMinimizedDock);
        protoOutputStream.end(jStart);
    }
}
