package com.android.server.wm;

import android.R;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import com.android.internal.policy.DividerSnapAlgorithm;
import com.android.internal.policy.DockedDividerUtils;
import com.android.server.EventLogTags;
import com.android.server.wm.DisplayContent;
import com.mediatek.server.wm.WmsExt;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.function.Consumer;

public class TaskStack extends WindowContainer<Task> implements BoundsAnimationTarget {
    private static final float ADJUSTED_STACK_FRACTION_MIN = 0.3f;
    private static final float IME_ADJUST_DIM_AMOUNT = 0.25f;
    private float mAdjustDividerAmount;
    private float mAdjustImeAmount;
    private final Rect mAdjustedBounds;
    private boolean mAdjustedForIme;
    private final AnimatingAppWindowTokenRegistry mAnimatingAppWindowTokenRegistry;
    private WindowStateAnimator mAnimationBackgroundAnimator;
    private SurfaceControl mAnimationBackgroundSurface;
    private boolean mAnimationBackgroundSurfaceIsShown;
    private final Rect mBoundsAfterRotation;
    private boolean mBoundsAnimating;
    private boolean mBoundsAnimatingRequested;
    private boolean mBoundsAnimatingToFullscreen;
    private Rect mBoundsAnimationSourceHintBounds;
    private Rect mBoundsAnimationTarget;
    private boolean mCancelCurrentBoundsAnimation;
    boolean mDeferRemoval;
    private int mDensity;
    private Dimmer mDimmer;
    private DisplayContent mDisplayContent;
    private final int mDockedStackMinimizeThickness;
    final AppTokenList mExitingAppTokens;
    private final Rect mFullyAdjustedImeBounds;
    private boolean mImeGoingAway;
    private WindowState mImeWin;
    private final Point mLastSurfaceSize;
    private float mMinimizeAmount;
    Rect mPreAnimationBounds;
    private int mRotation;
    final int mStackId;
    private final Rect mTmpAdjustedBounds;
    final AppTokenList mTmpAppTokens;
    final Rect mTmpDimBoundsRect;
    private Rect mTmpRect;
    private Rect mTmpRect2;
    private Rect mTmpRect3;

    @Override
    public void commitPendingTransaction() {
        super.commitPendingTransaction();
    }

    @Override
    public int compareTo(WindowContainer windowContainer) {
        return super.compareTo(windowContainer);
    }

    @Override
    public SurfaceControl getAnimationLeashParent() {
        return super.getAnimationLeashParent();
    }

    @Override
    public SurfaceControl getParentSurfaceControl() {
        return super.getParentSurfaceControl();
    }

    @Override
    public SurfaceControl.Transaction getPendingTransaction() {
        return super.getPendingTransaction();
    }

    @Override
    public SurfaceControl getSurfaceControl() {
        return super.getSurfaceControl();
    }

    @Override
    public int getSurfaceHeight() {
        return super.getSurfaceHeight();
    }

    @Override
    public int getSurfaceWidth() {
        return super.getSurfaceWidth();
    }

    @Override
    public SurfaceControl.Builder makeAnimationLeash() {
        return super.makeAnimationLeash();
    }

    @Override
    public void onAnimationLeashCreated(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl) {
        super.onAnimationLeashCreated(transaction, surfaceControl);
    }

    @Override
    public void onAnimationLeashDestroyed(SurfaceControl.Transaction transaction) {
        super.onAnimationLeashDestroyed(transaction);
    }

    @Override
    public void onOverrideConfigurationChanged(Configuration configuration) {
        super.onOverrideConfigurationChanged(configuration);
    }

    TaskStack(WindowManagerService windowManagerService, int i, StackWindowController stackWindowController) {
        super(windowManagerService);
        this.mTmpRect = new Rect();
        this.mTmpRect2 = new Rect();
        this.mTmpRect3 = new Rect();
        this.mAdjustedBounds = new Rect();
        this.mFullyAdjustedImeBounds = new Rect();
        this.mAnimationBackgroundSurfaceIsShown = false;
        this.mExitingAppTokens = new AppTokenList();
        this.mTmpAppTokens = new AppTokenList();
        this.mTmpAdjustedBounds = new Rect();
        this.mBoundsAnimating = false;
        this.mBoundsAnimatingRequested = false;
        this.mBoundsAnimatingToFullscreen = false;
        this.mCancelCurrentBoundsAnimation = false;
        this.mBoundsAnimationTarget = new Rect();
        this.mBoundsAnimationSourceHintBounds = new Rect();
        this.mBoundsAfterRotation = new Rect();
        this.mPreAnimationBounds = new Rect();
        this.mDimmer = new Dimmer(this);
        this.mTmpDimBoundsRect = new Rect();
        this.mLastSurfaceSize = new Point();
        this.mAnimatingAppWindowTokenRegistry = new AnimatingAppWindowTokenRegistry();
        this.mStackId = i;
        setController(stackWindowController);
        this.mDockedStackMinimizeThickness = windowManagerService.mContext.getResources().getDimensionPixelSize(R.dimen.car_button_height);
        EventLog.writeEvent(EventLogTags.WM_STACK_CREATED, i);
    }

    DisplayContent getDisplayContent() {
        return this.mDisplayContent;
    }

    Task findHomeTask() {
        if (!isActivityTypeHome() || this.mChildren.isEmpty()) {
            return null;
        }
        return (Task) this.mChildren.get(this.mChildren.size() - 1);
    }

    boolean setBounds(Rect rect, SparseArray<Rect> sparseArray, SparseArray<Rect> sparseArray2) {
        setBounds(rect);
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            Task task = (Task) this.mChildren.get(size);
            task.setBounds(sparseArray.get(task.mTaskId), false);
            task.setTempInsetBounds(sparseArray2 != null ? sparseArray2.get(task.mTaskId) : null);
        }
        return true;
    }

    void prepareFreezingTaskBounds() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((Task) this.mChildren.get(size)).prepareFreezingBounds();
        }
    }

    private void setAdjustedBounds(Rect rect) {
        if (this.mAdjustedBounds.equals(rect) && !isAnimatingForIme()) {
            return;
        }
        this.mAdjustedBounds.set(rect);
        boolean z = !this.mAdjustedBounds.isEmpty();
        Rect rawBounds = null;
        if (z && isAdjustedForMinimizedDockedStack()) {
            rawBounds = getRawBounds();
        } else if (z && this.mAdjustedForIme) {
            if (this.mImeGoingAway) {
                rawBounds = getRawBounds();
            } else {
                rawBounds = this.mFullyAdjustedImeBounds;
            }
        }
        alignTasksToAdjustedBounds(z ? this.mAdjustedBounds : getRawBounds(), rawBounds);
        this.mDisplayContent.setLayoutNeeded();
        updateSurfaceBounds();
    }

    private void alignTasksToAdjustedBounds(Rect rect, Rect rect2) {
        boolean z;
        if (matchParentBounds()) {
            return;
        }
        if (!this.mAdjustedForIme || getDockSide() != 2) {
            z = false;
        } else {
            z = true;
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((Task) this.mChildren.get(size)).alignToAdjustedBounds(rect, rect2, z);
        }
    }

    private void updateAnimationBackgroundBounds() {
        if (this.mAnimationBackgroundSurface == null) {
            return;
        }
        getRawBounds(this.mTmpRect);
        Rect bounds = getBounds();
        getPendingTransaction().setSize(this.mAnimationBackgroundSurface, this.mTmpRect.width(), this.mTmpRect.height()).setPosition(this.mAnimationBackgroundSurface, this.mTmpRect.left - bounds.left, this.mTmpRect.top - bounds.top);
        scheduleAnimation();
    }

    private void hideAnimationSurface() {
        if (this.mAnimationBackgroundSurface == null) {
            return;
        }
        getPendingTransaction().hide(this.mAnimationBackgroundSurface);
        this.mAnimationBackgroundSurfaceIsShown = false;
        scheduleAnimation();
    }

    private void showAnimationSurface(float f) {
        if (this.mAnimationBackgroundSurface == null) {
            return;
        }
        getPendingTransaction().setLayer(this.mAnimationBackgroundSurface, Integer.MIN_VALUE).setAlpha(this.mAnimationBackgroundSurface, f).show(this.mAnimationBackgroundSurface);
        this.mAnimationBackgroundSurfaceIsShown = true;
        scheduleAnimation();
    }

    @Override
    public int setBounds(Rect rect) {
        return setBounds(getOverrideBounds(), rect);
    }

    private int setBounds(Rect rect, Rect rect2) {
        int i;
        int i2;
        if (WindowManagerDebugConfig.DEBUG_STACK) {
            Slog.d(WmsExt.TAG, "setBounds bound = " + rect2 + ", stackId = " + this.mStackId, new Throwable("setBounds"));
        }
        if (this.mDisplayContent != null) {
            this.mDisplayContent.getBounds(this.mTmpRect);
            i = this.mDisplayContent.getDisplayInfo().rotation;
            i2 = this.mDisplayContent.getDisplayInfo().logicalDensityDpi;
        } else {
            i = 0;
            i2 = 0;
        }
        if (equivalentBounds(rect, rect2) && this.mRotation == i) {
            return 0;
        }
        int bounds = super.setBounds(rect2);
        if (this.mDisplayContent != null) {
            updateAnimationBackgroundBounds();
        }
        this.mRotation = i;
        this.mDensity = i2;
        updateAdjustedBounds();
        updateSurfaceBounds();
        return bounds;
    }

    void getRawBounds(Rect rect) {
        rect.set(getRawBounds());
    }

    Rect getRawBounds() {
        return super.getBounds();
    }

    private boolean useCurrentBounds() {
        if (matchParentBounds() || !inSplitScreenSecondaryWindowingMode() || this.mDisplayContent == null || this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility() != null) {
            return true;
        }
        return false;
    }

    @Override
    public void getBounds(Rect rect) {
        rect.set(getBounds());
    }

    @Override
    public Rect getBounds() {
        if (useCurrentBounds()) {
            if (!this.mAdjustedBounds.isEmpty()) {
                return this.mAdjustedBounds;
            }
            return super.getBounds();
        }
        return this.mDisplayContent.getBounds();
    }

    void setAnimationFinalBounds(Rect rect, Rect rect2, boolean z) {
        this.mBoundsAnimatingRequested = true;
        this.mBoundsAnimatingToFullscreen = z;
        if (rect2 != null) {
            this.mBoundsAnimationTarget.set(rect2);
        } else {
            this.mBoundsAnimationTarget.setEmpty();
        }
        if (rect != null) {
            this.mBoundsAnimationSourceHintBounds.set(rect);
        } else {
            this.mBoundsAnimationSourceHintBounds.setEmpty();
        }
        this.mPreAnimationBounds.set(getRawBounds());
    }

    void getFinalAnimationBounds(Rect rect) {
        rect.set(this.mBoundsAnimationTarget);
    }

    void getFinalAnimationSourceHintBounds(Rect rect) {
        rect.set(this.mBoundsAnimationSourceHintBounds);
    }

    void getAnimationOrCurrentBounds(Rect rect) {
        if ((this.mBoundsAnimatingRequested || this.mBoundsAnimating) && !this.mBoundsAnimationTarget.isEmpty()) {
            getFinalAnimationBounds(rect);
        } else {
            getBounds(rect);
        }
    }

    public void getDimBounds(Rect rect) {
        getBounds(rect);
    }

    void updateDisplayInfo(Rect rect) {
        if (this.mDisplayContent == null) {
            return;
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((Task) this.mChildren.get(size)).updateDisplayInfo(this.mDisplayContent);
        }
        if (rect != null) {
            setBounds(rect);
            return;
        }
        if (matchParentBounds()) {
            setBounds(null);
            return;
        }
        this.mTmpRect2.set(getRawBounds());
        int i = this.mDisplayContent.getDisplayInfo().rotation;
        int i2 = this.mDisplayContent.getDisplayInfo().logicalDensityDpi;
        if (this.mRotation == i && this.mDensity == i2) {
            setBounds(this.mTmpRect2);
        }
    }

    boolean updateBoundsAfterConfigChange() {
        int i = 0;
        if (this.mDisplayContent == null) {
            return false;
        }
        if (inPinnedWindowingMode()) {
            getAnimationOrCurrentBounds(this.mTmpRect2);
            if (this.mDisplayContent.mPinnedStackControllerLocked.onTaskStackBoundsChanged(this.mTmpRect2, this.mTmpRect3)) {
                this.mBoundsAfterRotation.set(this.mTmpRect3);
                this.mBoundsAnimationTarget.setEmpty();
                this.mBoundsAnimationSourceHintBounds.setEmpty();
                this.mCancelCurrentBoundsAnimation = true;
                return true;
            }
        }
        int i2 = getDisplayInfo().rotation;
        int i3 = getDisplayInfo().logicalDensityDpi;
        if (this.mRotation == i2 && this.mDensity == i3) {
            return false;
        }
        if (matchParentBounds()) {
            setBounds(null);
            return false;
        }
        this.mTmpRect2.set(getRawBounds());
        this.mDisplayContent.rotateBounds(this.mRotation, i2, this.mTmpRect2);
        if (inSplitScreenPrimaryWindowingMode()) {
            repositionPrimarySplitScreenStackAfterRotation(this.mTmpRect2);
            snapDockedStackAfterRotation(this.mTmpRect2);
            int dockSide = getDockSide(this.mTmpRect2);
            WindowManagerService windowManagerService = this.mService;
            if (dockSide != 1 && dockSide != 2) {
                i = 1;
            }
            windowManagerService.setDockedStackCreateStateLocked(i, null);
            this.mDisplayContent.getDockedDividerController().notifyDockSideChanged(dockSide);
        }
        this.mBoundsAfterRotation.set(this.mTmpRect2);
        return true;
    }

    void getBoundsForNewConfiguration(Rect rect) {
        rect.set(this.mBoundsAfterRotation);
        this.mBoundsAfterRotation.setEmpty();
    }

    private void repositionPrimarySplitScreenStackAfterRotation(Rect rect) {
        int dockSide = getDockSide(rect);
        if (this.mDisplayContent.getDockedDividerController().canPrimaryStackDockTo(dockSide)) {
        }
        this.mDisplayContent.getBounds(this.mTmpRect);
        switch (DockedDividerUtils.invertDockSide(dockSide)) {
            case 1:
                int i = rect.left;
                rect.left -= i;
                rect.right -= i;
                break;
            case 2:
                int i2 = rect.top;
                rect.top -= i2;
                rect.bottom -= i2;
                break;
            case 3:
                int i3 = this.mTmpRect.right - rect.right;
                rect.left += i3;
                rect.right += i3;
                break;
            case 4:
                int i4 = this.mTmpRect.bottom - rect.bottom;
                rect.top += i4;
                rect.bottom += i4;
                break;
        }
    }

    private void snapDockedStackAfterRotation(Rect rect) {
        DisplayInfo displayInfo = this.mDisplayContent.getDisplayInfo();
        int contentWidth = this.mDisplayContent.getDockedDividerController().getContentWidth();
        int dockSide = getDockSide(rect);
        int iCalculatePositionForBounds = DockedDividerUtils.calculatePositionForBounds(rect, dockSide, contentWidth);
        int i = displayInfo.logicalWidth;
        int i2 = displayInfo.logicalHeight;
        int i3 = displayInfo.rotation;
        int i4 = this.mDisplayContent.getConfiguration().orientation;
        this.mService.mPolicy.getStableInsetsLw(i3, i, i2, displayInfo.displayCutout, rect);
        DockedDividerUtils.calculateBoundsForPosition(new DividerSnapAlgorithm(this.mService.mContext.getResources(), i, i2, contentWidth, i4 == 1, rect, getDockSide(), isMinimizedDockAndHomeStackResizable()).calculateNonDismissingSnapTarget(iCalculatePositionForBounds).position, dockSide, rect, displayInfo.logicalWidth, displayInfo.logicalHeight, contentWidth);
    }

    void addTask(Task task, int i) {
        addTask(task, i, task.showForAllUsers(), true);
    }

    void addTask(Task task, int i, boolean z, boolean z2) {
        TaskStack taskStack = task.mStack;
        if (taskStack != null && taskStack.mStackId != this.mStackId) {
            throw new IllegalStateException("Trying to add taskId=" + task.mTaskId + " to stackId=" + this.mStackId + ", but it is already attached to stackId=" + task.mStack.mStackId);
        }
        task.mStack = this;
        addChild(task, (Comparator<Task>) null);
        positionChildAt(i, task, z2, z);
    }

    @Override
    void positionChildAt(int i, Task task, boolean z) {
        positionChildAt(i, task, z, task.showForAllUsers());
    }

    private void positionChildAt(int i, Task task, boolean z, boolean z2) {
        int iFindPositionForTask = findPositionForTask(task, i, z2, false);
        super.positionChildAt(iFindPositionForTask, task, z);
        if (WindowManagerDebugConfig.DEBUG_TASK_MOVEMENT) {
            Slog.d(WmsExt.TAG, "positionTask: task=" + this + " position=" + i);
        }
        EventLog.writeEvent(EventLogTags.WM_TASK_MOVED, Integer.valueOf(task.mTaskId), Integer.valueOf(iFindPositionForTask == this.mChildren.size() - 1 ? 1 : 0), Integer.valueOf(iFindPositionForTask));
    }

    private int findPositionForTask(Task task, int i, boolean z, boolean z2) {
        int iComputeMaxPosition;
        int iComputeMinPosition = 0;
        boolean z3 = z || this.mService.isCurrentProfileLocked(task.mUserId);
        int size = this.mChildren.size();
        if (!z2) {
            iComputeMaxPosition = size - 1;
        } else {
            iComputeMaxPosition = size;
        }
        if (z3) {
            iComputeMinPosition = computeMinPosition(0, size);
        } else {
            iComputeMaxPosition = computeMaxPosition(iComputeMaxPosition);
        }
        if (i == Integer.MIN_VALUE && iComputeMinPosition == 0) {
            return Integer.MIN_VALUE;
        }
        if (i == Integer.MAX_VALUE) {
            if (!z2) {
                size--;
            }
            if (iComputeMaxPosition == size) {
                return Integer.MAX_VALUE;
            }
        }
        return Math.min(Math.max(i, iComputeMinPosition), iComputeMaxPosition);
    }

    private int computeMinPosition(int i, int i2) {
        while (i < i2) {
            Task task = (Task) this.mChildren.get(i);
            if (task.showForAllUsers() || this.mService.isCurrentProfileLocked(task.mUserId)) {
                break;
            }
            i++;
        }
        return i;
    }

    private int computeMaxPosition(int i) {
        while (i > 0) {
            Task task = (Task) this.mChildren.get(i);
            if (!(task.showForAllUsers() || this.mService.isCurrentProfileLocked(task.mUserId))) {
                break;
            }
            i--;
        }
        return i;
    }

    @Override
    void removeChild(Task task) {
        if (WindowManagerDebugConfig.DEBUG_TASK_MOVEMENT) {
            Slog.d(WmsExt.TAG, "removeChild: task=" + task);
        }
        super.removeChild(task);
        task.mStack = null;
        if (this.mDisplayContent != null) {
            if (this.mChildren.isEmpty()) {
                getParent().positionChildAt(Integer.MIN_VALUE, this, false);
            }
            this.mDisplayContent.setLayoutNeeded();
        }
        for (int size = this.mExitingAppTokens.size() - 1; size >= 0; size--) {
            AppWindowToken appWindowToken = this.mExitingAppTokens.get(size);
            if (appWindowToken.getTask() == task) {
                appWindowToken.mIsExiting = false;
                this.mExitingAppTokens.remove(size);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        int windowingMode = getWindowingMode();
        super.onConfigurationChanged(configuration);
        updateSurfaceSize(getPendingTransaction());
        int windowingMode2 = getWindowingMode();
        if (this.mDisplayContent == null || windowingMode == windowingMode2) {
            return;
        }
        this.mDisplayContent.onStackWindowingModeChanged(this);
        updateBoundsForWindowModeChange();
    }

    private void updateSurfaceBounds() {
        updateSurfaceSize(getPendingTransaction());
        updateSurfacePosition();
        scheduleAnimation();
    }

    int getStackOutset() {
        DisplayContent displayContent = getDisplayContent();
        if (inPinnedWindowingMode() && displayContent != null) {
            DisplayMetrics displayMetrics = displayContent.getDisplayMetrics();
            WindowManagerService windowManagerService = this.mService;
            return (int) Math.ceil(WindowManagerService.dipToPixel(5, displayMetrics) * 2);
        }
        return 0;
    }

    private void updateSurfaceSize(SurfaceControl.Transaction transaction) {
        if (this.mSurfaceControl == null) {
            return;
        }
        Rect bounds = getBounds();
        int iWidth = bounds.width();
        int iHeight = bounds.height();
        int stackOutset = 2 * getStackOutset();
        int i = iWidth + stackOutset;
        int i2 = iHeight + stackOutset;
        if (i == this.mLastSurfaceSize.x && i2 == this.mLastSurfaceSize.y) {
            return;
        }
        transaction.setSize(this.mSurfaceControl, i, i2);
        this.mLastSurfaceSize.set(i, i2);
    }

    @Override
    void onDisplayChanged(DisplayContent displayContent) {
        if (this.mDisplayContent != null) {
            throw new IllegalStateException("onDisplayChanged: Already attached");
        }
        this.mDisplayContent = displayContent;
        updateBoundsForWindowModeChange();
        this.mAnimationBackgroundSurface = makeChildSurface(null).setColorLayer(true).setName("animation background stackId=" + this.mStackId).build();
        super.onDisplayChanged(displayContent);
    }

    private void updateBoundsForWindowModeChange() {
        Rect rectCalculateBoundsForWindowModeChange = calculateBoundsForWindowModeChange();
        if (inSplitScreenSecondaryWindowingMode()) {
            forAllWindows((Consumer<WindowState>) new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ((WindowState) obj).mWinAnimator.setOffsetPositionForStackResize(true);
                }
            }, true);
        }
        updateDisplayInfo(rectCalculateBoundsForWindowModeChange);
        updateSurfaceBounds();
    }

    private Rect calculateBoundsForWindowModeChange() {
        boolean zInSplitScreenPrimaryWindowingMode = inSplitScreenPrimaryWindowingMode();
        TaskStack splitScreenPrimaryStackIgnoringVisibility = this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility();
        if (zInSplitScreenPrimaryWindowingMode || (splitScreenPrimaryStackIgnoringVisibility != null && inSplitScreenSecondaryWindowingMode() && !splitScreenPrimaryStackIgnoringVisibility.fillsParent())) {
            Rect rect = new Rect();
            this.mDisplayContent.getBounds(this.mTmpRect);
            this.mTmpRect2.setEmpty();
            if (splitScreenPrimaryStackIgnoringVisibility != null) {
                if (inSplitScreenSecondaryWindowingMode() && this.mDisplayContent.mDividerControllerLocked.isMinimizedDock() && splitScreenPrimaryStackIgnoringVisibility.getTopChild() != null) {
                    splitScreenPrimaryStackIgnoringVisibility.getTopChild().getBounds(this.mTmpRect2);
                } else {
                    splitScreenPrimaryStackIgnoringVisibility.getRawBounds(this.mTmpRect2);
                }
            }
            getStackDockedModeBounds(this.mTmpRect, rect, this.mTmpRect2, this.mDisplayContent.mDividerControllerLocked.getContentWidth(), this.mService.mDockedStackCreateMode == 0);
            return rect;
        }
        if (inPinnedWindowingMode()) {
            getAnimationOrCurrentBounds(this.mTmpRect2);
            if (this.mDisplayContent.mPinnedStackControllerLocked.onTaskStackBoundsChanged(this.mTmpRect2, this.mTmpRect3)) {
                return new Rect(this.mTmpRect3);
            }
            return null;
        }
        return null;
    }

    void getStackDockedModeBoundsLocked(Rect rect, Rect rect2, Rect rect3, boolean z) {
        rect3.setEmpty();
        if (isActivityTypeHome()) {
            Task taskFindHomeTask = findHomeTask();
            if (taskFindHomeTask != null && taskFindHomeTask.isResizeable()) {
                getDisplayContent().mDividerControllerLocked.getHomeStackBoundsInDockedMode(rect2);
            } else {
                rect2.setEmpty();
            }
            rect3.set(rect2);
            return;
        }
        if (isMinimizedDockAndHomeStackResizable() && rect != null) {
            rect2.set(rect);
            return;
        }
        if (!inSplitScreenWindowingMode() || this.mDisplayContent == null) {
            rect2.set(getRawBounds());
            return;
        }
        TaskStack splitScreenPrimaryStackIgnoringVisibility = this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility();
        if (splitScreenPrimaryStackIgnoringVisibility == null) {
            throw new IllegalStateException("Calling getStackDockedModeBoundsLocked() when there is no docked stack.");
        }
        if (!z && !splitScreenPrimaryStackIgnoringVisibility.isVisible()) {
            this.mDisplayContent.getBounds(rect2);
            return;
        }
        int dockSide = splitScreenPrimaryStackIgnoringVisibility.getDockSide();
        if (dockSide == -1) {
            Slog.e(WmsExt.TAG, "Failed to get valid docked side for docked stack=" + splitScreenPrimaryStackIgnoringVisibility);
            rect2.set(getRawBounds());
            return;
        }
        this.mDisplayContent.getBounds(this.mTmpRect);
        splitScreenPrimaryStackIgnoringVisibility.getRawBounds(this.mTmpRect2);
        boolean z2 = true;
        if (dockSide != 2 && dockSide != 1) {
            z2 = false;
        }
        getStackDockedModeBounds(this.mTmpRect, rect2, this.mTmpRect2, this.mDisplayContent.mDividerControllerLocked.getContentWidth(), z2);
    }

    private void getStackDockedModeBounds(Rect rect, Rect rect2, Rect rect3, int i, boolean z) {
        boolean zInSplitScreenPrimaryWindowingMode = inSplitScreenPrimaryWindowingMode();
        boolean z2 = rect.width() > rect.height();
        rect2.set(rect);
        if (zInSplitScreenPrimaryWindowingMode) {
            if (this.mService.mDockedStackCreateBounds != null) {
                rect2.set(this.mService.mDockedStackCreateBounds);
                return;
            }
            DisplayInfo displayInfo = this.mDisplayContent.getDisplayInfo();
            this.mService.mPolicy.getStableInsetsLw(displayInfo.rotation, displayInfo.logicalWidth, displayInfo.logicalHeight, displayInfo.displayCutout, this.mTmpRect2);
            int i2 = new DividerSnapAlgorithm(this.mService.mContext.getResources(), displayInfo.logicalWidth, displayInfo.logicalHeight, i, this.mDisplayContent.getConfiguration().orientation == 1, this.mTmpRect2).getMiddleTarget().position;
            if (z) {
                if (z2) {
                    rect2.right = i2;
                    return;
                } else {
                    rect2.bottom = i2;
                    return;
                }
            }
            if (z2) {
                rect2.left = i2 + i;
                return;
            } else {
                rect2.top = i2 + i;
                return;
            }
        }
        if (!z) {
            if (z2) {
                rect2.right = rect3.left - i;
            } else {
                rect2.bottom = rect3.top - i;
            }
        } else if (z2) {
            rect2.left = rect3.right + i;
        } else {
            rect2.top = rect3.bottom + i;
        }
        DockedDividerUtils.sanitizeStackBounds(rect2, !z);
    }

    void resetDockedStackToMiddle() {
        if (inSplitScreenPrimaryWindowingMode()) {
            throw new IllegalStateException("Not a docked stack=" + this);
        }
        this.mService.mDockedStackCreateBounds = null;
        Rect rect = new Rect();
        getStackDockedModeBoundsLocked(null, rect, new Rect(), true);
        getController().requestResize(rect);
    }

    @Override
    StackWindowController getController() {
        return (StackWindowController) super.getController();
    }

    @Override
    void removeIfPossible() {
        if (isSelfOrChildAnimating()) {
            this.mDeferRemoval = true;
        } else {
            removeImmediately();
        }
    }

    @Override
    void onParentSet() {
        super.onParentSet();
        if (getParent() != null || this.mDisplayContent == null) {
            return;
        }
        EventLog.writeEvent(EventLogTags.WM_STACK_REMOVED, this.mStackId);
        if (this.mAnimationBackgroundSurface != null) {
            this.mAnimationBackgroundSurface.destroy();
            this.mAnimationBackgroundSurface = null;
        }
        this.mDisplayContent = null;
        this.mService.mWindowPlacerLocked.requestTraversal();
    }

    void resetAnimationBackgroundAnimator() {
        this.mAnimationBackgroundAnimator = null;
        hideAnimationSurface();
    }

    void setAnimationBackground(WindowStateAnimator windowStateAnimator, int i) {
        int i2 = windowStateAnimator.mAnimLayer;
        if (this.mAnimationBackgroundAnimator == null || i2 < this.mAnimationBackgroundAnimator.mAnimLayer) {
            this.mAnimationBackgroundAnimator = windowStateAnimator;
            this.mDisplayContent.getLayerForAnimationBackground(windowStateAnimator);
            showAnimationSurface(((i >> 24) & 255) / 255.0f);
        }
    }

    @Override
    void switchUser() {
        super.switchUser();
        int size = this.mChildren.size();
        for (int i = 0; i < size; i++) {
            Task task = (Task) this.mChildren.get(i);
            if (this.mService.isCurrentProfileLocked(task.mUserId) || task.showForAllUsers()) {
                this.mChildren.remove(i);
                this.mChildren.add(task);
                size--;
            }
        }
    }

    void setAdjustedForIme(WindowState windowState, boolean z) {
        this.mImeWin = windowState;
        this.mImeGoingAway = false;
        if (!this.mAdjustedForIme || z) {
            this.mAdjustedForIme = true;
            this.mAdjustImeAmount = 0.0f;
            this.mAdjustDividerAmount = 0.0f;
            updateAdjustForIme(0.0f, 0.0f, true);
        }
    }

    boolean isAdjustedForIme() {
        return this.mAdjustedForIme;
    }

    boolean isAnimatingForIme() {
        return this.mImeWin != null && this.mImeWin.isAnimatingLw();
    }

    boolean updateAdjustForIme(float f, float f2, boolean z) {
        if (f != this.mAdjustImeAmount || f2 != this.mAdjustDividerAmount || z) {
            this.mAdjustImeAmount = f;
            this.mAdjustDividerAmount = f2;
            updateAdjustedBounds();
            return isVisible();
        }
        return false;
    }

    void resetAdjustedForIme(boolean z) {
        if (z) {
            this.mImeWin = null;
            this.mImeGoingAway = false;
            this.mAdjustImeAmount = 0.0f;
            this.mAdjustDividerAmount = 0.0f;
            if (!this.mAdjustedForIme) {
                return;
            }
            this.mAdjustedForIme = false;
            updateAdjustedBounds();
            this.mService.setResizeDimLayer(false, getWindowingMode(), 1.0f);
            return;
        }
        this.mImeGoingAway |= this.mAdjustedForIme;
    }

    boolean setAdjustedForMinimizedDock(float f) {
        if (f != this.mMinimizeAmount) {
            this.mMinimizeAmount = f;
            updateAdjustedBounds();
            return isVisible();
        }
        return false;
    }

    boolean shouldIgnoreInput() {
        return isAdjustedForMinimizedDockedStack() || (inSplitScreenPrimaryWindowingMode() && isMinimizedDockAndHomeStackResizable());
    }

    void beginImeAdjustAnimation() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            Task task = (Task) this.mChildren.get(size);
            if (task.hasContentToDisplay()) {
                task.setDragResizing(true, 1);
                task.setWaitingForDrawnIfResizingChanged();
            }
        }
    }

    void endImeAdjustAnimation() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((Task) this.mChildren.get(size)).setDragResizing(false, 1);
        }
    }

    int getMinTopStackBottom(Rect rect, int i) {
        return rect.top + ((int) ((i - rect.top) * ADJUSTED_STACK_FRACTION_MIN));
    }

    private boolean adjustForIME(WindowState windowState) {
        int dockSide = getDockSide();
        boolean z = dockSide == 2 || dockSide == 4;
        if (windowState == null || !z) {
            return false;
        }
        Rect rect = this.mTmpRect;
        Rect rect2 = this.mTmpRect2;
        getDisplayContent().getStableRect(rect);
        rect2.set(rect);
        int iMax = Math.max(windowState.getFrameLw().top, rect2.top) + windowState.getGivenContentInsetsLw().top;
        if (rect2.bottom > iMax) {
            rect2.bottom = iMax;
        }
        int i = rect.bottom - rect2.bottom;
        int contentWidth = getDisplayContent().mDividerControllerLocked.getContentWidth();
        int contentWidthInactive = getDisplayContent().mDividerControllerLocked.getContentWidthInactive();
        if (dockSide == 2) {
            int iMax2 = Math.max(((getRawBounds().bottom - i) + contentWidth) - contentWidthInactive, getMinTopStackBottom(rect, getRawBounds().bottom));
            this.mTmpAdjustedBounds.set(getRawBounds());
            this.mTmpAdjustedBounds.bottom = (int) ((this.mAdjustImeAmount * iMax2) + ((1.0f - this.mAdjustImeAmount) * getRawBounds().bottom));
            this.mFullyAdjustedImeBounds.set(getRawBounds());
        } else {
            int i2 = (getRawBounds().top - contentWidth) + contentWidthInactive;
            int iMax3 = Math.max(getRawBounds().top - i, getMinTopStackBottom(rect, getRawBounds().top - contentWidth) + contentWidthInactive);
            this.mTmpAdjustedBounds.set(getRawBounds());
            this.mTmpAdjustedBounds.top = getRawBounds().top + ((int) ((this.mAdjustImeAmount * (iMax3 - i2)) + (this.mAdjustDividerAmount * (contentWidthInactive - contentWidth))));
            this.mFullyAdjustedImeBounds.set(getRawBounds());
            this.mFullyAdjustedImeBounds.top = iMax3;
            this.mFullyAdjustedImeBounds.bottom = iMax3 + getRawBounds().height();
        }
        return true;
    }

    private boolean adjustForMinimizedDockedStack(float f) {
        int dockSide = getDockSide();
        if (dockSide == -1 && !this.mTmpAdjustedBounds.isEmpty()) {
            return false;
        }
        if (dockSide == 2) {
            this.mService.getStableInsetsLocked(0, this.mTmpRect);
            int i = this.mTmpRect.top;
            this.mTmpAdjustedBounds.set(getRawBounds());
            this.mTmpAdjustedBounds.bottom = (int) ((i * f) + ((1.0f - f) * getRawBounds().bottom));
        } else if (dockSide == 1) {
            this.mTmpAdjustedBounds.set(getRawBounds());
            int iWidth = getRawBounds().width();
            this.mTmpAdjustedBounds.right = (int) ((this.mDockedStackMinimizeThickness * f) + ((1.0f - f) * getRawBounds().right));
            this.mTmpAdjustedBounds.left = this.mTmpAdjustedBounds.right - iWidth;
        } else if (dockSide == 3) {
            this.mTmpAdjustedBounds.set(getRawBounds());
            this.mTmpAdjustedBounds.left = (int) (((getRawBounds().right - this.mDockedStackMinimizeThickness) * f) + ((1.0f - f) * getRawBounds().left));
        }
        return true;
    }

    private boolean isMinimizedDockAndHomeStackResizable() {
        return this.mDisplayContent.mDividerControllerLocked.isMinimizedDock() && this.mDisplayContent.mDividerControllerLocked.isHomeStackResizable();
    }

    int getMinimizeDistance() {
        int dockSide = getDockSide();
        if (dockSide == -1) {
            return 0;
        }
        if (dockSide == 2) {
            this.mService.getStableInsetsLocked(0, this.mTmpRect);
            return getRawBounds().bottom - this.mTmpRect.top;
        }
        if (dockSide != 1 && dockSide != 3) {
            return 0;
        }
        return getRawBounds().width() - this.mDockedStackMinimizeThickness;
    }

    private void updateAdjustedBounds() {
        boolean zAdjustForIME;
        if (this.mMinimizeAmount != 0.0f) {
            zAdjustForIME = adjustForMinimizedDockedStack(this.mMinimizeAmount);
        } else if (this.mAdjustedForIme) {
            zAdjustForIME = adjustForIME(this.mImeWin);
        } else {
            zAdjustForIME = false;
        }
        if (!zAdjustForIME) {
            this.mTmpAdjustedBounds.setEmpty();
        }
        setAdjustedBounds(this.mTmpAdjustedBounds);
        boolean z = this.mService.getImeFocusStackLocked() == this;
        if (this.mAdjustedForIme && zAdjustForIME && !z) {
            this.mService.setResizeDimLayer(true, getWindowingMode(), Math.max(this.mAdjustImeAmount, this.mAdjustDividerAmount) * IME_ADJUST_DIM_AMOUNT);
        }
    }

    void applyAdjustForImeIfNeeded(Task task) {
        if (this.mMinimizeAmount != 0.0f || !this.mAdjustedForIme || this.mAdjustedBounds.isEmpty()) {
            return;
        }
        task.alignToAdjustedBounds(this.mAdjustedBounds, this.mImeGoingAway ? getRawBounds() : this.mFullyAdjustedImeBounds, getDockSide() == 2);
        this.mDisplayContent.setLayoutNeeded();
    }

    boolean isAdjustedForMinimizedDockedStack() {
        return this.mMinimizeAmount != 0.0f;
    }

    boolean isTaskAnimating() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            if (((Task) this.mChildren.get(size)).isTaskAnimating()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void writeToProto(ProtoOutputStream protoOutputStream, long j, boolean z) {
        long jStart = protoOutputStream.start(j);
        super.writeToProto(protoOutputStream, 1146756268033L, z);
        protoOutputStream.write(1120986464258L, this.mStackId);
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((Task) this.mChildren.get(size)).writeToProto(protoOutputStream, 2246267895811L, z);
        }
        protoOutputStream.write(1133871366148L, matchParentBounds());
        getRawBounds().writeToProto(protoOutputStream, 1146756268037L);
        protoOutputStream.write(1133871366150L, this.mAnimationBackgroundSurfaceIsShown);
        protoOutputStream.write(1133871366151L, this.mDeferRemoval);
        protoOutputStream.write(1108101562376L, this.mMinimizeAmount);
        protoOutputStream.write(1133871366153L, this.mAdjustedForIme);
        protoOutputStream.write(1108101562378L, this.mAdjustImeAmount);
        protoOutputStream.write(1108101562379L, this.mAdjustDividerAmount);
        this.mAdjustedBounds.writeToProto(protoOutputStream, 1146756268044L);
        protoOutputStream.write(1133871366157L, this.mBoundsAnimating);
        protoOutputStream.end(jStart);
    }

    @Override
    void dump(PrintWriter printWriter, String str, boolean z) {
        printWriter.println(str + "mStackId=" + this.mStackId);
        printWriter.println(str + "mDeferRemoval=" + this.mDeferRemoval);
        printWriter.println(str + "mBounds=" + getRawBounds().toShortString());
        if (this.mMinimizeAmount != 0.0f) {
            printWriter.println(str + "mMinimizeAmount=" + this.mMinimizeAmount);
        }
        if (this.mAdjustedForIme) {
            printWriter.println(str + "mAdjustedForIme=true");
            printWriter.println(str + "mAdjustImeAmount=" + this.mAdjustImeAmount);
            printWriter.println(str + "mAdjustDividerAmount=" + this.mAdjustDividerAmount);
        }
        if (!this.mAdjustedBounds.isEmpty()) {
            printWriter.println(str + "mAdjustedBounds=" + this.mAdjustedBounds.toShortString());
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size += -1) {
            ((Task) this.mChildren.get(size)).dump(printWriter, str + "  ", z);
        }
        if (this.mAnimationBackgroundSurfaceIsShown) {
            printWriter.println(str + "mWindowAnimationBackgroundSurface is shown");
        }
        if (!this.mExitingAppTokens.isEmpty()) {
            printWriter.println();
            printWriter.println("  Exiting application tokens:");
            for (int size2 = this.mExitingAppTokens.size() - 1; size2 >= 0; size2--) {
                AppWindowToken appWindowToken = this.mExitingAppTokens.get(size2);
                printWriter.print("  Exiting App #");
                printWriter.print(size2);
                printWriter.print(' ');
                printWriter.print(appWindowToken);
                printWriter.println(':');
                appWindowToken.dump(printWriter, "    ", z);
            }
        }
        this.mAnimatingAppWindowTokenRegistry.dump(printWriter, "AnimatingApps:", str);
    }

    @Override
    boolean fillsParent() {
        if (useCurrentBounds()) {
            return matchParentBounds();
        }
        return true;
    }

    public String toString() {
        return "{stackId=" + this.mStackId + " tasks=" + this.mChildren + "}";
    }

    @Override
    String getName() {
        return toShortString();
    }

    public String toShortString() {
        return "Stack=" + this.mStackId;
    }

    int getDockSide() {
        return getDockSide(getRawBounds());
    }

    int getDockSideForDisplay(DisplayContent displayContent) {
        return getDockSide(displayContent, getRawBounds());
    }

    private int getDockSide(Rect rect) {
        if (this.mDisplayContent == null) {
            return -1;
        }
        return getDockSide(this.mDisplayContent, rect);
    }

    private int getDockSide(DisplayContent displayContent, Rect rect) {
        if (!inSplitScreenWindowingMode()) {
            return -1;
        }
        displayContent.getBounds(this.mTmpRect);
        return displayContent.getDockedDividerController().getDockSide(rect, this.mTmpRect, displayContent.getConfiguration().orientation);
    }

    boolean hasTaskForUser(int i) {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            if (((Task) this.mChildren.get(size)).mUserId == i) {
                return true;
            }
        }
        return false;
    }

    int taskIdFromPoint(int i, int i2) {
        getBounds(this.mTmpRect);
        if (!this.mTmpRect.contains(i, i2) || isAdjustedForMinimizedDockedStack()) {
            return -1;
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            Task task = (Task) this.mChildren.get(size);
            if (task.getTopVisibleAppMainWindow() != null) {
                task.getDimBounds(this.mTmpRect);
                if (this.mTmpRect.contains(i, i2)) {
                    return task.mTaskId;
                }
            }
        }
        return -1;
    }

    void findTaskForResizePoint(int i, int i2, int i3, DisplayContent.TaskForResizePointSearchResult taskForResizePointSearchResult) {
        if (!getWindowConfiguration().canResizeTask()) {
            taskForResizePointSearchResult.searchDone = true;
            return;
        }
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            Task task = (Task) this.mChildren.get(size);
            if (task.isFullscreen()) {
                taskForResizePointSearchResult.searchDone = true;
                return;
            }
            task.getDimBounds(this.mTmpRect);
            int i4 = -i3;
            this.mTmpRect.inset(i4, i4);
            if (this.mTmpRect.contains(i, i2)) {
                this.mTmpRect.inset(i3, i3);
                taskForResizePointSearchResult.searchDone = true;
                if (!this.mTmpRect.contains(i, i2)) {
                    taskForResizePointSearchResult.taskForResize = task;
                    return;
                }
                return;
            }
        }
    }

    void setTouchExcludeRegion(Task task, int i, Region region, Rect rect, Rect rect2) {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            Task task2 = (Task) this.mChildren.get(size);
            AppWindowToken topVisibleAppToken = task2.getTopVisibleAppToken();
            if (topVisibleAppToken != null && topVisibleAppToken.hasContentToDisplay()) {
                if (task2.isActivityTypeHome() && isMinimizedDockAndHomeStackResizable()) {
                    this.mDisplayContent.getBounds(this.mTmpRect);
                } else {
                    task2.getDimBounds(this.mTmpRect);
                }
                if (task2 == task) {
                    rect2.set(this.mTmpRect);
                }
                boolean zInFreeformWindowingMode = task2.inFreeformWindowingMode();
                if (task2 != task || zInFreeformWindowingMode) {
                    if (zInFreeformWindowingMode) {
                        int i2 = -i;
                        this.mTmpRect.inset(i2, i2);
                        this.mTmpRect.intersect(rect);
                    }
                    region.op(this.mTmpRect, Region.Op.DIFFERENCE);
                }
            }
        }
    }

    @Override
    public boolean setPinnedStackSize(Rect rect, Rect rect2) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mCancelCurrentBoundsAnimation) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                try {
                    this.mService.mActivityManager.resizePinnedStack(rect, rect2);
                    return true;
                } catch (RemoteException e) {
                    return true;
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    void onAllWindowsDrawn() {
        if (!this.mBoundsAnimating && !this.mBoundsAnimatingRequested) {
            return;
        }
        this.mService.mBoundsAnimationController.onAllWindowsDrawn();
    }

    @Override
    public void onAnimationStart(boolean z, boolean z2) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mBoundsAnimatingRequested = false;
                this.mBoundsAnimating = true;
                this.mCancelCurrentBoundsAnimation = false;
                if (z) {
                    forAllWindows((Consumer<WindowState>) new Consumer() {
                        @Override
                        public final void accept(Object obj) {
                            ((WindowState) obj).mWinAnimator.resetDrawState();
                        }
                    }, false);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        if (inPinnedWindowingMode()) {
            try {
                this.mService.mActivityManager.notifyPinnedStackAnimationStarted();
            } catch (RemoteException e) {
            }
            PinnedStackWindowController pinnedStackWindowController = (PinnedStackWindowController) getController();
            if (z && pinnedStackWindowController != null) {
                pinnedStackWindowController.updatePictureInPictureModeForPinnedStackAnimation(null, z2);
            }
        }
    }

    @Override
    public void onAnimationEnd(boolean z, Rect rect, boolean z2) {
        if (inPinnedWindowingMode()) {
            PinnedStackWindowController pinnedStackWindowController = (PinnedStackWindowController) getController();
            if (z && pinnedStackWindowController != null) {
                pinnedStackWindowController.updatePictureInPictureModeForPinnedStackAnimation(this.mBoundsAnimationTarget, false);
            }
            if (rect != null) {
                setPinnedStackSize(rect, null);
            } else {
                onPipAnimationEndResize();
            }
            try {
                this.mService.mActivityManager.notifyPinnedStackAnimationEnded();
                if (z2) {
                    this.mService.mActivityManager.moveTasksToFullscreenStack(this.mStackId, true);
                    return;
                }
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        onPipAnimationEndResize();
    }

    public void onPipAnimationEndResize() {
        this.mBoundsAnimating = false;
        for (int i = 0; i < this.mChildren.size(); i++) {
            ((Task) this.mChildren.get(i)).clearPreserveNonFloatingState();
        }
        this.mService.requestTraversal();
    }

    @Override
    public boolean shouldDeferStartOnMoveToFullscreen() {
        Task topChild;
        TaskStack homeStack = this.mDisplayContent.getHomeStack();
        if (homeStack == null || (topChild = homeStack.getTopChild()) == null) {
            return true;
        }
        AppWindowToken topVisibleAppToken = topChild.getTopVisibleAppToken();
        if (!topChild.isVisible() || topVisibleAppToken == null) {
            return true;
        }
        return !topVisibleAppToken.allDrawn;
    }

    public boolean deferScheduleMultiWindowModeChanged() {
        if (inPinnedWindowingMode()) {
            return this.mBoundsAnimatingRequested || this.mBoundsAnimating;
        }
        return false;
    }

    public boolean isForceScaled() {
        return this.mBoundsAnimating;
    }

    public boolean isAnimatingBounds() {
        return this.mBoundsAnimating;
    }

    public boolean lastAnimatingBoundsWasToFullscreen() {
        return this.mBoundsAnimatingToFullscreen;
    }

    public boolean isAnimatingBoundsToFullscreen() {
        return isAnimatingBounds() && lastAnimatingBoundsWasToFullscreen();
    }

    public boolean pinnedStackResizeDisallowed() {
        if (this.mBoundsAnimating && this.mCancelCurrentBoundsAnimation) {
            return true;
        }
        return false;
    }

    @Override
    boolean checkCompleteDeferredRemoval() {
        if (isSelfOrChildAnimating()) {
            return true;
        }
        if (this.mDeferRemoval) {
            removeImmediately();
        }
        return super.checkCompleteDeferredRemoval();
    }

    @Override
    int getOrientation() {
        if (canSpecifyOrientation()) {
            return super.getOrientation();
        }
        return -2;
    }

    private boolean canSpecifyOrientation() {
        int windowingMode = getWindowingMode();
        int activityType = getActivityType();
        return windowingMode == 1 || activityType == 2 || activityType == 3 || activityType == 4;
    }

    @Override
    Dimmer getDimmer() {
        return this.mDimmer;
    }

    @Override
    void prepareSurfaces() {
        this.mDimmer.resetDimStates();
        super.prepareSurfaces();
        getDimBounds(this.mTmpDimBoundsRect);
        this.mTmpDimBoundsRect.offsetTo(0, 0);
        if (this.mDimmer.updateDims(getPendingTransaction(), this.mTmpDimBoundsRect)) {
            scheduleAnimation();
        }
    }

    public DisplayInfo getDisplayInfo() {
        return this.mDisplayContent.getDisplayInfo();
    }

    void dim(float f) {
        this.mDimmer.dimAbove(getPendingTransaction(), f);
        scheduleAnimation();
    }

    void stopDimming() {
        this.mDimmer.stopDim(getPendingTransaction());
        scheduleAnimation();
    }

    @Override
    void getRelativePosition(Point point) {
        super.getRelativePosition(point);
        int stackOutset = getStackOutset();
        point.x -= stackOutset;
        point.y -= stackOutset;
    }

    AnimatingAppWindowTokenRegistry getAnimatingAppWindowTokenRegistry() {
        return this.mAnimatingAppWindowTokenRegistry;
    }
}
