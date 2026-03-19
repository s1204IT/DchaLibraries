package com.android.server.wm;

import android.R;
import android.app.ActivityManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.dhcp.DhcpPacket;
import android.os.Debug;
import android.os.RemoteException;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseArray;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import com.android.internal.policy.DividerSnapAlgorithm;
import com.android.internal.policy.DockedDividerUtils;
import com.android.server.EventLogTags;
import com.android.server.wm.BoundsAnimationController;
import com.android.server.wm.DimLayer;
import java.io.PrintWriter;
import java.util.ArrayList;

public class TaskStack implements DimLayer.DimLayerUser, BoundsAnimationController.AnimateBoundsUser {
    private static final float ADJUSTED_STACK_FRACTION_MIN = 0.3f;
    private static final float IME_ADJUST_DIM_AMOUNT = 0.25f;
    private float mAdjustDividerAmount;
    private float mAdjustImeAmount;
    private boolean mAdjustedForIme;
    WindowStateAnimator mAnimationBackgroundAnimator;
    DimLayer mAnimationBackgroundSurface;
    boolean mDeferDetach;
    int mDensity;
    private DisplayContent mDisplayContent;
    private final int mDockedStackMinimizeThickness;
    private boolean mDragResizing;
    private boolean mImeGoingAway;
    private WindowState mImeWin;
    private float mMinimizeAmount;
    int mRotation;
    private final WindowManagerService mService;
    final int mStackId;
    private final ArrayList<Task> mTasks = new ArrayList<>();
    private Rect mTmpRect = new Rect();
    private Rect mTmpRect2 = new Rect();
    private Rect mBounds = new Rect();
    private final Rect mAdjustedBounds = new Rect();
    private final Rect mFullyAdjustedImeBounds = new Rect();
    private boolean mFullscreen = true;
    final AppTokenList mExitingAppTokens = new AppTokenList();
    private final Rect mTmpAdjustedBounds = new Rect();
    private boolean mBoundsAnimating = false;
    private final Rect mBoundsAfterRotation = new Rect();

    TaskStack(WindowManagerService service, int stackId) {
        this.mService = service;
        this.mStackId = stackId;
        this.mDockedStackMinimizeThickness = service.mContext.getResources().getDimensionPixelSize(R.dimen.action_bar_icon_vertical_padding_material);
        EventLog.writeEvent(EventLogTags.WM_STACK_CREATED, stackId);
    }

    DisplayContent getDisplayContent() {
        return this.mDisplayContent;
    }

    ArrayList<Task> getTasks() {
        return this.mTasks;
    }

    Task findHomeTask() {
        if (this.mStackId != 0) {
            return null;
        }
        for (int i = this.mTasks.size() - 1; i >= 0; i--) {
            if (this.mTasks.get(i).isHomeTask()) {
                return this.mTasks.get(i);
            }
        }
        return null;
    }

    boolean setBounds(Rect stackBounds, SparseArray<Configuration> configs, SparseArray<Rect> taskBounds, SparseArray<Rect> taskTempInsetBounds) {
        setBounds(stackBounds);
        for (int taskNdx = this.mTasks.size() - 1; taskNdx >= 0; taskNdx--) {
            Task task = this.mTasks.get(taskNdx);
            Configuration config = configs.get(task.mTaskId);
            if (config != null) {
                Rect bounds = taskBounds.get(task.mTaskId);
                if (task.isTwoFingerScrollMode()) {
                    task.resizeLocked(bounds, config, false);
                    task.getBounds(this.mTmpRect);
                    task.scrollLocked(this.mTmpRect);
                } else {
                    task.resizeLocked(bounds, config, false);
                    task.setTempInsetBounds(taskTempInsetBounds != null ? taskTempInsetBounds.get(task.mTaskId) : null);
                }
            } else if (task.mDeferRemoval) {
                Slog.e("WindowManager", "Skip defering removal task: " + task);
            } else {
                Slog.e("WindowManager", "No config for task: " + task + ", is there a mismatch with AM?");
            }
        }
        return true;
    }

    void prepareFreezingTaskBounds() {
        for (int taskNdx = this.mTasks.size() - 1; taskNdx >= 0; taskNdx--) {
            Task task = this.mTasks.get(taskNdx);
            task.prepareFreezingBounds();
        }
    }

    boolean isFullscreenBounds(Rect bounds) {
        if (this.mDisplayContent == null || bounds == null) {
            return true;
        }
        this.mDisplayContent.getLogicalDisplayRect(this.mTmpRect);
        return this.mTmpRect.equals(bounds);
    }

    private void setAdjustedBounds(Rect bounds) {
        if (this.mAdjustedBounds.equals(bounds) && !isAnimatingForIme()) {
            return;
        }
        this.mAdjustedBounds.set(bounds);
        boolean adjusted = !this.mAdjustedBounds.isEmpty();
        Rect insetBounds = null;
        if (adjusted && isAdjustedForMinimizedDock()) {
            insetBounds = this.mBounds;
        } else if (adjusted && this.mAdjustedForIme) {
            insetBounds = this.mImeGoingAway ? this.mBounds : this.mFullyAdjustedImeBounds;
        }
        alignTasksToAdjustedBounds(adjusted ? this.mAdjustedBounds : this.mBounds, insetBounds);
        this.mDisplayContent.layoutNeeded = true;
    }

    private void alignTasksToAdjustedBounds(Rect adjustedBounds, Rect tempInsetBounds) {
        if (this.mFullscreen) {
            return;
        }
        for (int taskNdx = this.mTasks.size() - 1; taskNdx >= 0; taskNdx--) {
            Task task = this.mTasks.get(taskNdx);
            if (task.isTwoFingerScrollMode()) {
                task.resizeLocked(null, null, false);
                task.getBounds(this.mTmpRect2);
                task.scrollLocked(this.mTmpRect2);
            } else {
                boolean alignBottom = this.mAdjustedForIme && getDockSide() == 2;
                task.alignToAdjustedBounds(adjustedBounds, tempInsetBounds, alignBottom);
            }
        }
    }

    private boolean setBounds(Rect bounds) {
        if (WindowManagerDebugConfig.DEBUG_STACK) {
            Slog.d("WindowManager", "setBounds bound = " + bounds + ", stackId = " + this.mStackId, new Throwable("setBounds"));
        }
        boolean oldFullscreen = this.mFullscreen;
        int rotation = 0;
        int density = 0;
        if (this.mDisplayContent != null) {
            this.mDisplayContent.getLogicalDisplayRect(this.mTmpRect);
            rotation = this.mDisplayContent.getDisplayInfo().rotation;
            density = this.mDisplayContent.getDisplayInfo().logicalDensityDpi;
            this.mFullscreen = bounds == null;
            if (this.mFullscreen) {
                bounds = this.mTmpRect;
            }
        }
        if (bounds == null) {
            return false;
        }
        if (this.mBounds.equals(bounds) && oldFullscreen == this.mFullscreen && this.mRotation == rotation) {
            return false;
        }
        if (this.mDisplayContent != null) {
            this.mDisplayContent.mDimLayerController.updateDimLayer(this);
            this.mAnimationBackgroundSurface.setBounds(bounds);
        }
        this.mBounds.set(bounds);
        this.mRotation = rotation;
        this.mDensity = density;
        updateAdjustedBounds();
        return true;
    }

    void getRawBounds(Rect out) {
        out.set(this.mBounds);
    }

    private boolean useCurrentBounds() {
        if (this.mFullscreen || !ActivityManager.StackId.isResizeableByDockedStack(this.mStackId) || this.mDisplayContent == null || this.mDisplayContent.getDockedStackLocked() != null) {
            return true;
        }
        return false;
    }

    public void getBounds(Rect out) {
        if (useCurrentBounds()) {
            if (!this.mAdjustedBounds.isEmpty()) {
                out.set(this.mAdjustedBounds);
                return;
            } else {
                out.set(this.mBounds);
                return;
            }
        }
        this.mDisplayContent.getLogicalDisplayRect(out);
    }

    @Override
    public void getDimBounds(Rect out) {
        getBounds(out);
    }

    void updateDisplayInfo(Rect bounds) {
        if (this.mDisplayContent == null) {
            return;
        }
        for (int taskNdx = this.mTasks.size() - 1; taskNdx >= 0; taskNdx--) {
            this.mTasks.get(taskNdx).updateDisplayInfo(this.mDisplayContent);
        }
        if (bounds != null) {
            setBounds(bounds);
            return;
        }
        if (this.mFullscreen) {
            setBounds(null);
            return;
        }
        this.mTmpRect2.set(this.mBounds);
        int newRotation = this.mDisplayContent.getDisplayInfo().rotation;
        int newDensity = this.mDisplayContent.getDisplayInfo().logicalDensityDpi;
        if (this.mRotation != newRotation || this.mDensity != newDensity) {
            return;
        }
        setBounds(this.mTmpRect2);
    }

    boolean onConfigurationChanged() {
        return updateBoundsAfterConfigChange();
    }

    private boolean updateBoundsAfterConfigChange() {
        int i = 0;
        if (this.mDisplayContent == null) {
            return false;
        }
        int newRotation = getDisplayInfo().rotation;
        int newDensity = getDisplayInfo().logicalDensityDpi;
        if (this.mRotation == newRotation && this.mDensity == newDensity) {
            return false;
        }
        if (this.mFullscreen) {
            setBounds(null);
            return false;
        }
        int oldDockSide = this.mStackId == 3 ? getDockSide() : -1;
        this.mTmpRect2.set(this.mBounds);
        this.mDisplayContent.rotateBounds(this.mRotation, newRotation, this.mTmpRect2);
        if (this.mStackId == 3) {
            repositionDockedStackAfterRotation(this.mTmpRect2);
            snapDockedStackAfterRotation(this.mTmpRect2);
            int newDockSide = getDockSide(this.mTmpRect2);
            if (oldDockSide != newDockSide) {
                WindowManagerService windowManagerService = this.mService;
                if (newDockSide != 1 && newDockSide != 2) {
                    i = 1;
                }
                windowManagerService.setDockedStackCreateStateLocked(i, null);
                this.mDisplayContent.getDockedDividerController().notifyDockSideChanged(newDockSide);
            }
        }
        this.mBoundsAfterRotation.set(this.mTmpRect2);
        return true;
    }

    void getBoundsForNewConfiguration(Rect outBounds) {
        outBounds.set(this.mBoundsAfterRotation);
        this.mBoundsAfterRotation.setEmpty();
    }

    private void repositionDockedStackAfterRotation(Rect inOutBounds) {
        int dockSide = getDockSide(inOutBounds);
        if (this.mService.mPolicy.isDockSideAllowed(dockSide)) {
        }
        this.mDisplayContent.getLogicalDisplayRect(this.mTmpRect);
        switch (DockedDividerUtils.invertDockSide(dockSide)) {
            case 1:
                int movement = inOutBounds.left;
                inOutBounds.left -= movement;
                inOutBounds.right -= movement;
                break;
            case 2:
                int movement2 = inOutBounds.top;
                inOutBounds.top -= movement2;
                inOutBounds.bottom -= movement2;
                break;
            case 3:
                int movement3 = this.mTmpRect.right - inOutBounds.right;
                inOutBounds.left += movement3;
                inOutBounds.right += movement3;
                break;
            case 4:
                int movement4 = this.mTmpRect.bottom - inOutBounds.bottom;
                inOutBounds.top += movement4;
                inOutBounds.bottom += movement4;
                break;
        }
    }

    private void snapDockedStackAfterRotation(Rect outBounds) {
        DisplayInfo displayInfo = this.mDisplayContent.getDisplayInfo();
        int dividerSize = this.mService.getDefaultDisplayContentLocked().getDockedDividerController().getContentWidth();
        int dockSide = getDockSide(outBounds);
        int dividerPosition = DockedDividerUtils.calculatePositionForBounds(outBounds, dockSide, dividerSize);
        int displayWidth = this.mDisplayContent.getDisplayInfo().logicalWidth;
        int displayHeight = this.mDisplayContent.getDisplayInfo().logicalHeight;
        int rotation = displayInfo.rotation;
        int orientation = this.mService.mCurConfiguration.orientation;
        this.mService.mPolicy.getStableInsetsLw(rotation, displayWidth, displayHeight, outBounds);
        DividerSnapAlgorithm algorithm = new DividerSnapAlgorithm(this.mService.mContext.getResources(), displayWidth, displayHeight, dividerSize, orientation == 1, outBounds);
        DividerSnapAlgorithm.SnapTarget target = algorithm.calculateNonDismissingSnapTarget(dividerPosition);
        DockedDividerUtils.calculateBoundsForPosition(target.position, dockSide, outBounds, displayInfo.logicalWidth, displayInfo.logicalHeight, dividerSize);
    }

    boolean isAnimating() {
        for (int taskNdx = this.mTasks.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<AppWindowToken> activities = this.mTasks.get(taskNdx).mAppTokens;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ArrayList<WindowState> windows = activities.get(activityNdx).allAppWindows;
                for (int winNdx = windows.size() - 1; winNdx >= 0; winNdx--) {
                    WindowStateAnimator winAnimator = windows.get(winNdx).mWinAnimator;
                    if (winAnimator.isAnimationSet() || winAnimator.mWin.mAnimatingExit) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    void addTask(Task task, boolean toTop) {
        addTask(task, toTop, task.showForAllUsers());
    }

    void addTask(Task task, boolean toTop, boolean showForAllUsers) {
        positionTask(task, toTop ? this.mTasks.size() : 0, showForAllUsers);
    }

    void positionTask(Task task, int position, boolean showForAllUsers) {
        boolean zIsCurrentProfileLocked = !showForAllUsers ? this.mService.isCurrentProfileLocked(task.mUserId) : true;
        this.mTasks.remove(task);
        int stackSize = this.mTasks.size();
        int minPosition = 0;
        int maxPosition = stackSize;
        if (zIsCurrentProfileLocked) {
            minPosition = computeMinPosition(0, stackSize);
        } else {
            maxPosition = computeMaxPosition(stackSize);
        }
        int position2 = Math.min(Math.max(position, minPosition), maxPosition);
        if (WindowManagerDebugConfig.DEBUG_TASK_MOVEMENT) {
            Slog.d("WindowManager", "positionTask: task=" + task + " position=" + position2);
        }
        this.mTasks.add(position2, task);
        if (task.mStack != this) {
            task.resetScrollLocked();
        }
        task.mStack = this;
        task.updateDisplayInfo(this.mDisplayContent);
        boolean toTop = position2 == this.mTasks.size() + (-1);
        if (toTop) {
            this.mDisplayContent.moveStack(this, true);
        }
        if (ActivityManager.StackId.windowsAreScaleable(this.mStackId)) {
            forceWindowsScaleable(task, true);
        } else {
            forceWindowsScaleable(task, false);
        }
        Object[] objArr = new Object[3];
        objArr[0] = Integer.valueOf(task.mTaskId);
        objArr[1] = Integer.valueOf(toTop ? 1 : 0);
        objArr[2] = Integer.valueOf(position2);
        EventLog.writeEvent(EventLogTags.WM_TASK_MOVED, objArr);
    }

    private int computeMinPosition(int minPosition, int size) {
        boolean canShowTmpTask;
        while (minPosition < size) {
            Task tmpTask = this.mTasks.get(minPosition);
            if (tmpTask.showForAllUsers()) {
                canShowTmpTask = true;
            } else {
                canShowTmpTask = this.mService.isCurrentProfileLocked(tmpTask.mUserId);
            }
            if (canShowTmpTask) {
                break;
            }
            minPosition++;
        }
        return minPosition;
    }

    private int computeMaxPosition(int maxPosition) {
        boolean canShowTmpTask;
        while (maxPosition > 0) {
            Task tmpTask = this.mTasks.get(maxPosition - 1);
            if (tmpTask.showForAllUsers()) {
                canShowTmpTask = true;
            } else {
                canShowTmpTask = this.mService.isCurrentProfileLocked(tmpTask.mUserId);
            }
            if (!canShowTmpTask) {
                break;
            }
            maxPosition--;
        }
        return maxPosition;
    }

    void moveTaskToTop(Task task) {
        if (WindowManagerDebugConfig.DEBUG_TASK_MOVEMENT) {
            Slog.d("WindowManager", "moveTaskToTop: task=" + task + " Callers=" + Debug.getCallers(6));
        }
        this.mTasks.remove(task);
        addTask(task, true);
    }

    void moveTaskToBottom(Task task) {
        if (WindowManagerDebugConfig.DEBUG_TASK_MOVEMENT) {
            Slog.d("WindowManager", "moveTaskToBottom: task=" + task);
        }
        this.mTasks.remove(task);
        addTask(task, false);
    }

    void removeTask(Task task) {
        if (WindowManagerDebugConfig.DEBUG_TASK_MOVEMENT) {
            Slog.d("WindowManager", "removeTask: task=" + task);
        }
        this.mTasks.remove(task);
        if (this.mDisplayContent != null) {
            if (this.mTasks.isEmpty()) {
                this.mDisplayContent.moveStack(this, false);
            }
            this.mDisplayContent.layoutNeeded = true;
        }
        for (int appNdx = this.mExitingAppTokens.size() - 1; appNdx >= 0; appNdx--) {
            AppWindowToken wtoken = this.mExitingAppTokens.get(appNdx);
            if (wtoken.mTask == task) {
                wtoken.mIsExiting = false;
                this.mExitingAppTokens.remove(appNdx);
            }
        }
    }

    void attachDisplayContent(DisplayContent displayContent) {
        if (this.mDisplayContent != null) {
            throw new IllegalStateException("attachDisplayContent: Already attached");
        }
        this.mDisplayContent = displayContent;
        this.mAnimationBackgroundSurface = new DimLayer(this.mService, this, this.mDisplayContent.getDisplayId(), "animation background stackId=" + this.mStackId);
        Rect bounds = null;
        TaskStack dockedStack = this.mService.mStackIdToStack.get(3);
        if (this.mStackId == 3 || (dockedStack != null && ActivityManager.StackId.isResizeableByDockedStack(this.mStackId) && !dockedStack.isFullscreen())) {
            bounds = new Rect();
            displayContent.getLogicalDisplayRect(this.mTmpRect);
            this.mTmpRect2.setEmpty();
            if (dockedStack != null) {
                dockedStack.getRawBounds(this.mTmpRect2);
            }
            boolean dockedOnTopOrLeft = this.mService.mDockedStackCreateMode == 0;
            getStackDockedModeBounds(this.mTmpRect, bounds, this.mStackId, this.mTmpRect2, this.mDisplayContent.mDividerControllerLocked.getContentWidth(), dockedOnTopOrLeft);
        }
        updateDisplayInfo(bounds);
    }

    void getStackDockedModeBoundsLocked(Rect outBounds, boolean ignoreVisibility) {
        if ((this.mStackId != 3 && !ActivityManager.StackId.isResizeableByDockedStack(this.mStackId)) || this.mDisplayContent == null) {
            outBounds.set(this.mBounds);
            return;
        }
        TaskStack dockedStack = this.mService.mStackIdToStack.get(3);
        if (dockedStack == null) {
            throw new IllegalStateException("Calling getStackDockedModeBoundsLocked() when there is no docked stack.");
        }
        if (!ignoreVisibility && !dockedStack.isVisibleLocked()) {
            this.mDisplayContent.getLogicalDisplayRect(outBounds);
            return;
        }
        int dockedSide = dockedStack.getDockSide();
        if (dockedSide == -1) {
            Slog.e("WindowManager", "Failed to get valid docked side for docked stack=" + dockedStack);
            outBounds.set(this.mBounds);
        } else {
            this.mDisplayContent.getLogicalDisplayRect(this.mTmpRect);
            dockedStack.getRawBounds(this.mTmpRect2);
            boolean dockedOnTopOrLeft = dockedSide == 2 || dockedSide == 1;
            getStackDockedModeBounds(this.mTmpRect, outBounds, this.mStackId, this.mTmpRect2, this.mDisplayContent.mDividerControllerLocked.getContentWidth(), dockedOnTopOrLeft);
        }
    }

    private void getStackDockedModeBounds(Rect displayRect, Rect outBounds, int stackId, Rect dockedBounds, int dockDividerWidth, boolean dockOnTopOrLeft) {
        boolean dockedStack = stackId == 3;
        boolean splitHorizontally = displayRect.width() > displayRect.height();
        outBounds.set(displayRect);
        if (dockedStack) {
            if (this.mService.mDockedStackCreateBounds != null) {
                outBounds.set(this.mService.mDockedStackCreateBounds);
                return;
            }
            DisplayInfo di = this.mDisplayContent.getDisplayInfo();
            this.mService.mPolicy.getStableInsetsLw(di.rotation, di.logicalWidth, di.logicalHeight, this.mTmpRect2);
            int position = new DividerSnapAlgorithm(this.mService.mContext.getResources(), di.logicalWidth, di.logicalHeight, dockDividerWidth, this.mService.mCurConfiguration.orientation == 1, this.mTmpRect2).getMiddleTarget().position;
            if (dockOnTopOrLeft) {
                if (splitHorizontally) {
                    outBounds.right = position;
                    return;
                } else {
                    outBounds.bottom = position;
                    return;
                }
            }
            if (splitHorizontally) {
                outBounds.left = position + dockDividerWidth;
                return;
            } else {
                outBounds.top = position + dockDividerWidth;
                return;
            }
        }
        if (!dockOnTopOrLeft) {
            if (splitHorizontally) {
                outBounds.right = dockedBounds.left - dockDividerWidth;
            } else {
                outBounds.bottom = dockedBounds.top - dockDividerWidth;
            }
        } else if (splitHorizontally) {
            outBounds.left = dockedBounds.right + dockDividerWidth;
        } else {
            outBounds.top = dockedBounds.bottom + dockDividerWidth;
        }
        DockedDividerUtils.sanitizeStackBounds(outBounds, !dockOnTopOrLeft);
    }

    void resetDockedStackToMiddle() {
        if (this.mStackId != 3) {
            throw new IllegalStateException("Not a docked stack=" + this);
        }
        this.mService.mDockedStackCreateBounds = null;
        Rect bounds = new Rect();
        getStackDockedModeBoundsLocked(bounds, true);
        this.mService.mH.obtainMessage(42, 3, 1, bounds).sendToTarget();
    }

    void detachDisplay() {
        EventLog.writeEvent(EventLogTags.WM_STACK_REMOVED, this.mStackId);
        boolean doAnotherLayoutPass = false;
        for (int taskNdx = this.mTasks.size() - 1; taskNdx >= 0; taskNdx--) {
            AppTokenList appWindowTokens = this.mTasks.get(taskNdx).mAppTokens;
            for (int appNdx = appWindowTokens.size() - 1; appNdx >= 0; appNdx--) {
                WindowList appWindows = appWindowTokens.get(appNdx).allAppWindows;
                for (int winNdx = appWindows.size() - 1; winNdx >= 0; winNdx--) {
                    this.mService.removeWindowLocked(appWindows.get(winNdx));
                    doAnotherLayoutPass = true;
                }
            }
        }
        if (doAnotherLayoutPass) {
            this.mService.mWindowPlacerLocked.requestTraversal();
        }
        close();
    }

    void resetAnimationBackgroundAnimator() {
        this.mAnimationBackgroundAnimator = null;
        if (this.mAnimationBackgroundSurface == null) {
            return;
        }
        this.mAnimationBackgroundSurface.hide();
    }

    void setAnimationBackground(WindowStateAnimator winAnimator, int color) {
        int animLayer = winAnimator.mAnimLayer;
        if (this.mAnimationBackgroundAnimator != null && animLayer >= this.mAnimationBackgroundAnimator.mAnimLayer) {
            return;
        }
        this.mAnimationBackgroundAnimator = winAnimator;
        int animLayer2 = this.mService.adjustAnimationBackground(winAnimator);
        this.mAnimationBackgroundSurface.show(animLayer2 - 1, ((color >> 24) & DhcpPacket.MAX_OPTION_LEN) / 255.0f, 0L);
    }

    void switchUser() {
        int top = this.mTasks.size();
        for (int taskNdx = 0; taskNdx < top; taskNdx++) {
            Task task = this.mTasks.get(taskNdx);
            if (this.mService.isCurrentProfileLocked(task.mUserId) || task.showForAllUsers()) {
                this.mTasks.remove(taskNdx);
                this.mTasks.add(task);
                top--;
            }
        }
    }

    void close() {
        if (this.mAnimationBackgroundSurface != null) {
            this.mAnimationBackgroundSurface.destroySurface();
            this.mAnimationBackgroundSurface = null;
        }
        this.mDisplayContent = null;
    }

    void setAdjustedForIme(WindowState imeWin, boolean forceUpdate) {
        this.mImeWin = imeWin;
        this.mImeGoingAway = false;
        if (this.mAdjustedForIme && !forceUpdate) {
            return;
        }
        this.mAdjustedForIme = true;
        this.mAdjustImeAmount = 0.0f;
        this.mAdjustDividerAmount = 0.0f;
        updateAdjustForIme(0.0f, 0.0f, true);
    }

    boolean isAdjustedForIme() {
        return this.mAdjustedForIme;
    }

    boolean isAnimatingForIme() {
        if (this.mImeWin != null) {
            return this.mImeWin.isAnimatingLw();
        }
        return false;
    }

    boolean updateAdjustForIme(float adjustAmount, float adjustDividerAmount, boolean force) {
        if (adjustAmount != this.mAdjustImeAmount || adjustDividerAmount != this.mAdjustDividerAmount || force) {
            this.mAdjustImeAmount = adjustAmount;
            this.mAdjustDividerAmount = adjustDividerAmount;
            updateAdjustedBounds();
            return isVisibleForUserLocked();
        }
        return false;
    }

    void resetAdjustedForIme(boolean adjustBoundsNow) {
        if (adjustBoundsNow) {
            this.mImeWin = null;
            this.mAdjustedForIme = false;
            this.mImeGoingAway = false;
            this.mAdjustImeAmount = 0.0f;
            this.mAdjustDividerAmount = 0.0f;
            updateAdjustedBounds();
            this.mService.setResizeDimLayer(false, this.mStackId, 1.0f);
            return;
        }
        this.mImeGoingAway |= this.mAdjustedForIme;
    }

    boolean setAdjustedForMinimizedDock(float minimizeAmount) {
        if (minimizeAmount != this.mMinimizeAmount) {
            this.mMinimizeAmount = minimizeAmount;
            updateAdjustedBounds();
            return isVisibleForUserLocked();
        }
        return false;
    }

    boolean isAdjustedForMinimizedDock() {
        return this.mMinimizeAmount != 0.0f;
    }

    void beginImeAdjustAnimation() {
        for (int j = this.mTasks.size() - 1; j >= 0; j--) {
            Task task = this.mTasks.get(j);
            if (task.isVisibleForUser()) {
                task.setDragResizing(true, 1);
                task.addWindowsWaitingForDrawnIfResizingChanged();
            }
        }
    }

    void endImeAdjustAnimation() {
        for (int j = this.mTasks.size() - 1; j >= 0; j--) {
            this.mTasks.get(j).setDragResizing(false, 1);
        }
    }

    int getMinTopStackBottom(Rect displayContentRect, int originalStackBottom) {
        return displayContentRect.top + ((int) ((originalStackBottom - displayContentRect.top) * ADJUSTED_STACK_FRACTION_MIN));
    }

    private boolean adjustForIME(WindowState imeWin) {
        int dockedSide = getDockSide();
        boolean dockedTopOrBottom = dockedSide == 2 || dockedSide == 4;
        if (imeWin == null || !dockedTopOrBottom) {
            return false;
        }
        Rect displayContentRect = this.mTmpRect;
        Rect contentBounds = this.mTmpRect2;
        getDisplayContent().getContentRect(displayContentRect);
        contentBounds.set(displayContentRect);
        int imeTop = Math.max(imeWin.getFrameLw().top, contentBounds.top) + imeWin.getGivenContentInsetsLw().top;
        if (contentBounds.bottom > imeTop) {
            contentBounds.bottom = imeTop;
        }
        int yOffset = displayContentRect.bottom - contentBounds.bottom;
        int dividerWidth = getDisplayContent().mDividerControllerLocked.getContentWidth();
        int dividerWidthInactive = getDisplayContent().mDividerControllerLocked.getContentWidthInactive();
        if (dockedSide == 2) {
            int minTopStackBottom = getMinTopStackBottom(displayContentRect, this.mBounds.bottom);
            int bottom = Math.max(((this.mBounds.bottom - yOffset) + dividerWidth) - dividerWidthInactive, minTopStackBottom);
            this.mTmpAdjustedBounds.set(this.mBounds);
            this.mTmpAdjustedBounds.bottom = (int) ((this.mAdjustImeAmount * bottom) + ((1.0f - this.mAdjustImeAmount) * this.mBounds.bottom));
            this.mFullyAdjustedImeBounds.set(this.mBounds);
            return true;
        }
        int dividerWidthDelta = dividerWidthInactive - dividerWidth;
        int topBeforeImeAdjust = (this.mBounds.top - dividerWidth) + dividerWidthInactive;
        int minTopStackBottom2 = getMinTopStackBottom(displayContentRect, this.mBounds.top - dividerWidth);
        int top = Math.max(this.mBounds.top - yOffset, minTopStackBottom2 + dividerWidthInactive);
        this.mTmpAdjustedBounds.set(this.mBounds);
        this.mTmpAdjustedBounds.top = this.mBounds.top + ((int) ((this.mAdjustImeAmount * (top - topBeforeImeAdjust)) + (this.mAdjustDividerAmount * dividerWidthDelta)));
        this.mFullyAdjustedImeBounds.set(this.mBounds);
        this.mFullyAdjustedImeBounds.top = top;
        this.mFullyAdjustedImeBounds.bottom = this.mBounds.height() + top;
        return true;
    }

    private boolean adjustForMinimizedDockedStack(float minimizeAmount) {
        int dockSide = getDockSide();
        if (dockSide == -1 && !this.mTmpAdjustedBounds.isEmpty()) {
            return false;
        }
        if (dockSide == 2) {
            this.mService.getStableInsetsLocked(this.mTmpRect);
            int topInset = this.mTmpRect.top;
            this.mTmpAdjustedBounds.set(this.mBounds);
            this.mTmpAdjustedBounds.bottom = (int) ((topInset * minimizeAmount) + ((1.0f - minimizeAmount) * this.mBounds.bottom));
        } else if (dockSide == 1) {
            this.mTmpAdjustedBounds.set(this.mBounds);
            int width = this.mBounds.width();
            this.mTmpAdjustedBounds.right = (int) ((this.mDockedStackMinimizeThickness * minimizeAmount) + ((1.0f - minimizeAmount) * this.mBounds.right));
            this.mTmpAdjustedBounds.left = this.mTmpAdjustedBounds.right - width;
        } else if (dockSide == 3) {
            this.mTmpAdjustedBounds.set(this.mBounds);
            this.mTmpAdjustedBounds.left = (int) (((this.mBounds.right - this.mDockedStackMinimizeThickness) * minimizeAmount) + ((1.0f - minimizeAmount) * this.mBounds.left));
        }
        return true;
    }

    int getMinimizeDistance() {
        int dockSide = getDockSide();
        if (dockSide == -1) {
            return 0;
        }
        if (dockSide == 2) {
            this.mService.getStableInsetsLocked(this.mTmpRect);
            int topInset = this.mTmpRect.top;
            return this.mBounds.bottom - topInset;
        }
        if (dockSide == 1 || dockSide == 3) {
            return this.mBounds.width() - this.mDockedStackMinimizeThickness;
        }
        return 0;
    }

    private void updateAdjustedBounds() {
        boolean adjust = false;
        if (this.mMinimizeAmount != 0.0f) {
            adjust = adjustForMinimizedDockedStack(this.mMinimizeAmount);
        } else if (this.mAdjustedForIme) {
            adjust = adjustForIME(this.mImeWin);
        }
        if (!adjust) {
            this.mTmpAdjustedBounds.setEmpty();
        }
        setAdjustedBounds(this.mTmpAdjustedBounds);
        boolean isImeTarget = this.mService.getImeFocusStackLocked() == this;
        if (!this.mAdjustedForIme || !adjust || isImeTarget) {
            return;
        }
        float alpha = Math.max(this.mAdjustImeAmount, this.mAdjustDividerAmount) * IME_ADJUST_DIM_AMOUNT;
        this.mService.setResizeDimLayer(true, this.mStackId, alpha);
    }

    void applyAdjustForImeIfNeeded(Task task) {
        if (this.mMinimizeAmount != 0.0f || !this.mAdjustedForIme || this.mAdjustedBounds.isEmpty()) {
            return;
        }
        Rect insetBounds = this.mImeGoingAway ? this.mBounds : this.mFullyAdjustedImeBounds;
        task.alignToAdjustedBounds(this.mAdjustedBounds, insetBounds, getDockSide() == 2);
        this.mDisplayContent.layoutNeeded = true;
    }

    boolean isAdjustedForMinimizedDockedStack() {
        return this.mMinimizeAmount != 0.0f;
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.println(prefix + "mStackId=" + this.mStackId);
        pw.println(prefix + "mDeferDetach=" + this.mDeferDetach);
        pw.println(prefix + "mFullscreen=" + this.mFullscreen);
        pw.println(prefix + "mBounds=" + this.mBounds.toShortString());
        if (this.mMinimizeAmount != 0.0f) {
            pw.println(prefix + "mMinimizeAmout=" + this.mMinimizeAmount);
        }
        if (this.mAdjustedForIme) {
            pw.println(prefix + "mAdjustedForIme=true");
            pw.println(prefix + "mAdjustImeAmount=" + this.mAdjustImeAmount);
            pw.println(prefix + "mAdjustDividerAmount=" + this.mAdjustDividerAmount);
        }
        if (!this.mAdjustedBounds.isEmpty()) {
            pw.println(prefix + "mAdjustedBounds=" + this.mAdjustedBounds.toShortString());
        }
        for (int taskNdx = this.mTasks.size() - 1; taskNdx >= 0; taskNdx--) {
            this.mTasks.get(taskNdx).dump(prefix + "  ", pw);
        }
        if (this.mAnimationBackgroundSurface.isDimming()) {
            pw.println(prefix + "mWindowAnimationBackgroundSurface:");
            this.mAnimationBackgroundSurface.printTo(prefix + "  ", pw);
        }
        if (this.mExitingAppTokens.isEmpty()) {
            return;
        }
        pw.println();
        pw.println("  Exiting application tokens:");
        for (int i = this.mExitingAppTokens.size() - 1; i >= 0; i--) {
            WindowToken token = this.mExitingAppTokens.get(i);
            pw.print("  Exiting App #");
            pw.print(i);
            pw.print(' ');
            pw.print(token);
            pw.println(':');
            token.dump(pw, "    ");
        }
    }

    boolean getRawFullscreen() {
        return this.mFullscreen;
    }

    @Override
    public boolean dimFullscreen() {
        if (this.mStackId != 0) {
            return isFullscreen();
        }
        return true;
    }

    boolean isFullscreen() {
        if (useCurrentBounds()) {
            return this.mFullscreen;
        }
        return true;
    }

    @Override
    public DisplayInfo getDisplayInfo() {
        return this.mDisplayContent.getDisplayInfo();
    }

    public String toString() {
        return "{stackId=" + this.mStackId + " tasks=" + this.mTasks + "}";
    }

    @Override
    public String toShortString() {
        return "Stack=" + this.mStackId;
    }

    int getDockSide() {
        return getDockSide(this.mBounds);
    }

    int getDockSide(Rect bounds) {
        if ((this.mStackId != 3 && !ActivityManager.StackId.isResizeableByDockedStack(this.mStackId)) || this.mDisplayContent == null) {
            return -1;
        }
        this.mDisplayContent.getLogicalDisplayRect(this.mTmpRect);
        int orientation = this.mService.mCurConfiguration.orientation;
        return getDockSideUnchecked(bounds, this.mTmpRect, orientation);
    }

    static int getDockSideUnchecked(Rect bounds, Rect displayRect, int orientation) {
        if (orientation == 1) {
            return bounds.top - displayRect.top <= displayRect.bottom - bounds.bottom ? 2 : 4;
        }
        if (orientation == 2) {
            return bounds.left - displayRect.left <= displayRect.right - bounds.right ? 1 : 3;
        }
        return -1;
    }

    boolean isVisibleLocked() {
        boolean keyguardOn = this.mService.mPolicy.isKeyguardShowingOrOccluded() && !this.mService.mAnimator.mKeyguardGoingAway;
        if (keyguardOn && !ActivityManager.StackId.isAllowedOverLockscreen(this.mStackId)) {
            return false;
        }
        for (int i = this.mTasks.size() - 1; i >= 0; i--) {
            Task task = this.mTasks.get(i);
            for (int j = task.mAppTokens.size() - 1; j >= 0; j--) {
                if (!task.mAppTokens.get(j).hidden) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean isVisibleForUserLocked() {
        for (int i = this.mTasks.size() - 1; i >= 0; i--) {
            Task task = this.mTasks.get(i);
            if (task.isVisibleForUser()) {
                return true;
            }
        }
        return false;
    }

    boolean isDragResizing() {
        return this.mDragResizing;
    }

    void setDragResizingLocked(boolean resizing) {
        if (this.mDragResizing == resizing) {
            return;
        }
        this.mDragResizing = resizing;
        for (int i = this.mTasks.size() - 1; i >= 0; i--) {
            this.mTasks.get(i).resetDragResizingChangeReported();
        }
    }

    @Override
    public boolean setSize(Rect bounds) {
        synchronized (this.mService.mWindowMap) {
            if (this.mDisplayContent == null) {
                return false;
            }
            try {
                this.mService.mActivityManager.resizeStack(this.mStackId, bounds, false, true, false, -1);
            } catch (RemoteException e) {
            }
            return true;
        }
    }

    @Override
    public boolean setPinnedStackSize(Rect bounds, Rect tempTaskBounds) {
        synchronized (this.mService.mWindowMap) {
            if (this.mDisplayContent == null) {
                return false;
            }
            if (this.mStackId != 4) {
                Slog.w("WindowManager", "Attempt to use pinned stack resize animation helper onnon pinned stack");
                return false;
            }
            try {
                this.mService.mActivityManager.resizePinnedStack(bounds, tempTaskBounds);
                return true;
            } catch (RemoteException e) {
                return true;
            }
        }
    }

    void forceWindowsScaleable(Task task, boolean force) {
        SurfaceControl.openTransaction();
        try {
            ArrayList<AppWindowToken> activities = task.mAppTokens;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ArrayList<WindowState> windows = activities.get(activityNdx).allAppWindows;
                for (int winNdx = windows.size() - 1; winNdx >= 0; winNdx--) {
                    WindowStateAnimator winAnimator = windows.get(winNdx).mWinAnimator;
                    if (winAnimator != null && winAnimator.hasSurface()) {
                        winAnimator.mSurfaceController.forceScaleableInTransaction(force);
                    }
                }
            }
        } finally {
            SurfaceControl.closeTransaction();
        }
    }

    @Override
    public void onAnimationStart() {
        synchronized (this.mService.mWindowMap) {
            this.mBoundsAnimating = true;
        }
    }

    @Override
    public void onAnimationEnd() {
        synchronized (this.mService.mWindowMap) {
            this.mBoundsAnimating = false;
            this.mService.requestTraversal();
        }
        if (this.mStackId != 4) {
            return;
        }
        try {
            this.mService.mActivityManager.notifyPinnedStackAnimationEnded();
        } catch (RemoteException e) {
        }
    }

    @Override
    public void moveToFullscreen() {
        try {
            this.mService.mActivityManager.moveTasksToFullscreenStack(this.mStackId, true);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void getFullScreenBounds(Rect bounds) {
        getDisplayContent().getContentRect(bounds);
    }

    public boolean hasMovementAnimations() {
        return ActivityManager.StackId.hasMovementAnimations(this.mStackId);
    }

    public boolean getForceScaleToCrop() {
        return this.mBoundsAnimating;
    }

    public boolean getBoundsAnimating() {
        return this.mBoundsAnimating;
    }
}
