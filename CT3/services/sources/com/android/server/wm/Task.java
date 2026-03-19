package com.android.server.wm;

import android.app.ActivityManager;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.EventLog;
import android.util.Slog;
import android.view.DisplayInfo;
import com.android.server.EventLogTags;
import com.android.server.wm.DimLayer;
import com.mediatek.multiwindow.MultiWindowManager;
import java.io.PrintWriter;
import java.util.ArrayList;

class Task implements DimLayer.DimLayerUser {
    static final int BOUNDS_CHANGE_NONE = 0;
    static final int BOUNDS_CHANGE_POSITION = 1;
    static final int BOUNDS_CHANGE_SIZE = 2;
    static final String TAG = "WindowManager";
    private int mDragResizeMode;
    private boolean mDragResizing;
    private boolean mHomeTask;
    private int mResizeMode;
    int mRotation;
    private boolean mScrollValid;
    final WindowManagerService mService;
    TaskStack mStack;
    final int mTaskId;
    final int mUserId;
    final AppTokenList mAppTokens = new AppTokenList();
    boolean mDeferRemoval = false;
    private Rect mBounds = new Rect();
    final Rect mPreparedFrozenBounds = new Rect();
    final Configuration mPreparedFrozenMergedConfig = new Configuration();
    private Rect mPreScrollBounds = new Rect();
    private final Rect mTempInsetBounds = new Rect();
    private boolean mFullscreen = true;
    Configuration mOverrideConfig = Configuration.EMPTY;
    private Rect mTmpRect = new Rect();
    private Rect mTmpRect2 = new Rect();
    boolean mSticky = false;

    Task(int taskId, TaskStack stack, int userId, WindowManagerService service, Rect bounds, Configuration config) {
        this.mTaskId = taskId;
        this.mStack = stack;
        this.mUserId = userId;
        this.mService = service;
        setBounds(bounds, config);
    }

    DisplayContent getDisplayContent() {
        return this.mStack.getDisplayContent();
    }

    void addAppToken(int addPos, AppWindowToken wtoken, int resizeMode, boolean homeTask) {
        int lastPos = this.mAppTokens.size();
        if (addPos >= lastPos) {
            addPos = lastPos;
        } else {
            for (int pos = 0; pos < lastPos && pos < addPos; pos++) {
                if (this.mAppTokens.get(pos).removed) {
                    addPos++;
                }
            }
        }
        this.mAppTokens.add(addPos, wtoken);
        wtoken.mTask = this;
        this.mDeferRemoval = false;
        this.mResizeMode = resizeMode;
        this.mHomeTask = homeTask;
    }

    private boolean hasWindowsAlive() {
        for (int i = this.mAppTokens.size() - 1; i >= 0; i--) {
            if (this.mAppTokens.get(i).hasWindowsAlive()) {
                return true;
            }
        }
        return false;
    }

    void removeLocked() {
        if (hasWindowsAlive() && this.mStack.isAnimating()) {
            if (WindowManagerDebugConfig.DEBUG_STACK) {
                Slog.i(TAG, "removeTask: deferring removing taskId=" + this.mTaskId);
            }
            this.mDeferRemoval = true;
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_STACK) {
            Slog.i(TAG, "removeTask: removing taskId=" + this.mTaskId);
        }
        EventLog.writeEvent(EventLogTags.WM_TASK_REMOVED, Integer.valueOf(this.mTaskId), "removeTask");
        this.mDeferRemoval = false;
        DisplayContent content = getDisplayContent();
        if (content != null) {
            content.mDimLayerController.removeDimLayerUser(this);
        }
        this.mStack.removeTask(this);
        this.mService.mTaskIdToTask.delete(this.mTaskId);
    }

    void moveTaskToStack(TaskStack stack, boolean toTop) {
        if (stack == this.mStack) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_STACK) {
            Slog.i(TAG, "moveTaskToStack: removing taskId=" + this.mTaskId + " from stack=" + this.mStack);
        }
        EventLog.writeEvent(EventLogTags.WM_TASK_REMOVED, Integer.valueOf(this.mTaskId), "moveTask");
        if (this.mStack != null) {
            this.mStack.removeTask(this);
        }
        stack.addTask(this, toTop);
    }

    void positionTaskInStack(TaskStack stack, int position, Rect bounds, Configuration config) {
        boolean isMoveFromDockedStack = false;
        if (this.mStack != null && stack != this.mStack) {
            isMoveFromDockedStack = inDockedWorkspace();
            if (WindowManagerDebugConfig.DEBUG_STACK) {
                Slog.i(TAG, "positionTaskInStack: removing taskId=" + this.mTaskId + " from stack=" + this.mStack);
            }
            EventLog.writeEvent(EventLogTags.WM_TASK_REMOVED, Integer.valueOf(this.mTaskId), "moveTask");
            this.mStack.removeTask(this);
        }
        stack.positionTask(this, position, showForAllUsers());
        resizeLocked(bounds, config, false);
        for (int activityNdx = this.mAppTokens.size() - 1; activityNdx >= 0; activityNdx--) {
            ArrayList<WindowState> windows = this.mAppTokens.get(activityNdx).allAppWindows;
            for (int winNdx = windows.size() - 1; winNdx >= 0; winNdx--) {
                WindowState win = windows.get(winNdx);
                win.notifyMovedInStack();
                if (isMoveFromDockedStack) {
                    win.mResizedWhileGone = true;
                }
            }
        }
    }

    boolean removeAppToken(AppWindowToken wtoken) {
        boolean removed = this.mAppTokens.remove(wtoken);
        if (this.mAppTokens.size() == 0) {
            EventLog.writeEvent(EventLogTags.WM_TASK_REMOVED, Integer.valueOf(this.mTaskId), "removeAppToken: last token");
            if (this.mDeferRemoval) {
                removeLocked();
            }
        }
        wtoken.mTask = null;
        return removed;
    }

    void setSendingToBottom(boolean toBottom) {
        for (int appTokenNdx = 0; appTokenNdx < this.mAppTokens.size(); appTokenNdx++) {
            this.mAppTokens.get(appTokenNdx).sendingToBottom = toBottom;
        }
    }

    private int setBounds(Rect bounds, Configuration config) {
        if (config == null) {
            config = Configuration.EMPTY;
        }
        if (bounds == null && !Configuration.EMPTY.equals(config)) {
            throw new IllegalArgumentException("null bounds but non empty configuration: " + config);
        }
        if (bounds != null && Configuration.EMPTY.equals(config)) {
            throw new IllegalArgumentException("non null bounds, but empty configuration");
        }
        boolean oldFullscreen = this.mFullscreen;
        int rotation = 0;
        DisplayContent displayContent = this.mStack.getDisplayContent();
        if (displayContent != null) {
            displayContent.getLogicalDisplayRect(this.mTmpRect);
            rotation = displayContent.getDisplayInfo().rotation;
            this.mFullscreen = bounds == null;
            if (this.mFullscreen) {
                bounds = this.mTmpRect;
            }
        }
        if (bounds == null) {
            return 0;
        }
        if (this.mPreScrollBounds.equals(bounds) && oldFullscreen == this.mFullscreen && this.mRotation == rotation) {
            return 0;
        }
        int boundsChange = 0;
        if (this.mPreScrollBounds.left != bounds.left || this.mPreScrollBounds.top != bounds.top) {
            boundsChange = 1;
        }
        if (this.mPreScrollBounds.width() != bounds.width() || this.mPreScrollBounds.height() != bounds.height()) {
            boundsChange |= 2;
        }
        this.mPreScrollBounds.set(bounds);
        resetScrollLocked();
        this.mRotation = rotation;
        if (displayContent != null) {
            displayContent.mDimLayerController.updateDimLayer(this);
        }
        if (this.mFullscreen) {
            config = Configuration.EMPTY;
        }
        this.mOverrideConfig = config;
        return boundsChange;
    }

    void setTempInsetBounds(Rect tempInsetBounds) {
        if (tempInsetBounds != null) {
            this.mTempInsetBounds.set(tempInsetBounds);
        } else {
            this.mTempInsetBounds.setEmpty();
        }
    }

    void getTempInsetBounds(Rect out) {
        out.set(this.mTempInsetBounds);
    }

    void setResizeable(int resizeMode) {
        this.mResizeMode = resizeMode;
    }

    boolean isResizeable() {
        if (this.mHomeTask) {
            return false;
        }
        if (ActivityInfo.isResizeableMode(this.mResizeMode)) {
            return true;
        }
        return this.mService.mForceResizableTasks;
    }

    boolean cropWindowsToStackBounds() {
        return !this.mHomeTask && (isResizeable() || this.mResizeMode == 1);
    }

    boolean isHomeTask() {
        return this.mHomeTask;
    }

    private boolean inCropWindowsResizeMode() {
        return (this.mHomeTask || isResizeable() || this.mResizeMode != 1) ? false : true;
    }

    boolean resizeLocked(Rect bounds, Configuration configuration, boolean forced) {
        int boundsChanged = setBounds(bounds, configuration);
        if (forced) {
            boundsChanged |= 2;
        }
        if (boundsChanged == 0) {
            return false;
        }
        if ((boundsChanged & 2) == 2) {
            resizeWindows();
            return true;
        }
        moveWindows();
        return true;
    }

    void prepareFreezingBounds() {
        this.mPreparedFrozenBounds.set(this.mBounds);
        this.mPreparedFrozenMergedConfig.setTo(this.mService.mCurConfiguration);
        this.mPreparedFrozenMergedConfig.updateFrom(this.mOverrideConfig);
    }

    void alignToAdjustedBounds(Rect adjustedBounds, Rect tempInsetBounds, boolean alignBottom) {
        if (!isResizeable() || this.mOverrideConfig == Configuration.EMPTY) {
            return;
        }
        getBounds(this.mTmpRect2);
        if (alignBottom) {
            int offsetY = adjustedBounds.bottom - this.mTmpRect2.bottom;
            this.mTmpRect2.offset(0, offsetY);
        } else {
            this.mTmpRect2.offsetTo(adjustedBounds.left, adjustedBounds.top);
        }
        setTempInsetBounds(tempInsetBounds);
        resizeLocked(this.mTmpRect2, this.mOverrideConfig, false);
    }

    void resetScrollLocked() {
        if (this.mScrollValid) {
            this.mScrollValid = false;
            applyScrollToAllWindows(0, 0);
        }
        this.mBounds.set(this.mPreScrollBounds);
    }

    void applyScrollToAllWindows(int xOffset, int yOffset) {
        for (int activityNdx = this.mAppTokens.size() - 1; activityNdx >= 0; activityNdx--) {
            ArrayList<WindowState> windows = this.mAppTokens.get(activityNdx).allAppWindows;
            for (int winNdx = windows.size() - 1; winNdx >= 0; winNdx--) {
                WindowState win = windows.get(winNdx);
                win.mXOffset = xOffset;
                win.mYOffset = yOffset;
            }
        }
    }

    void applyScrollToWindowIfNeeded(WindowState win) {
        if (!this.mScrollValid) {
            return;
        }
        win.mXOffset = this.mBounds.left;
        win.mYOffset = this.mBounds.top;
    }

    boolean scrollLocked(Rect bounds) {
        this.mStack.getDimBounds(this.mTmpRect);
        if (this.mService.mCurConfiguration.orientation == 2) {
            if (bounds.left > this.mTmpRect.left) {
                bounds.left = this.mTmpRect.left;
                bounds.right = this.mTmpRect.left + this.mBounds.width();
            } else if (bounds.right < this.mTmpRect.right) {
                bounds.left = this.mTmpRect.right - this.mBounds.width();
                bounds.right = this.mTmpRect.right;
            }
        } else if (bounds.top > this.mTmpRect.top) {
            bounds.top = this.mTmpRect.top;
            bounds.bottom = this.mTmpRect.top + this.mBounds.height();
        } else if (bounds.bottom < this.mTmpRect.bottom) {
            bounds.top = this.mTmpRect.bottom - this.mBounds.height();
            bounds.bottom = this.mTmpRect.bottom;
        }
        if (this.mScrollValid && bounds.equals(this.mBounds)) {
            return false;
        }
        this.mBounds.set(bounds);
        this.mScrollValid = true;
        applyScrollToAllWindows(bounds.left, bounds.top);
        return true;
    }

    private boolean useCurrentBounds() {
        DisplayContent displayContent = this.mStack.getDisplayContent();
        if (this.mFullscreen || !ActivityManager.StackId.isTaskResizeableByDockedStack(this.mStack.mStackId) || displayContent == null || displayContent.getDockedStackVisibleForUserLocked() != null) {
            return true;
        }
        return false;
    }

    void getBounds(Rect out) {
        if (useCurrentBounds()) {
            out.set(this.mBounds);
        } else {
            this.mStack.getDisplayContent().getLogicalDisplayRect(out);
        }
    }

    boolean getMaxVisibleBounds(Rect out) {
        WindowState win;
        boolean foundTop = false;
        for (int i = this.mAppTokens.size() - 1; i >= 0; i--) {
            AppWindowToken token = this.mAppTokens.get(i);
            if (!token.mIsExiting && !token.clientHidden && !token.hiddenRequested && (win = token.findMainWindow()) != null) {
                if (!foundTop) {
                    out.set(win.mVisibleFrame);
                    foundTop = true;
                } else {
                    if (win.mVisibleFrame.left < out.left) {
                        out.left = win.mVisibleFrame.left;
                    }
                    if (win.mVisibleFrame.top < out.top) {
                        out.top = win.mVisibleFrame.top;
                    }
                    if (win.mVisibleFrame.right > out.right) {
                        out.right = win.mVisibleFrame.right;
                    }
                    if (win.mVisibleFrame.bottom > out.bottom) {
                        out.bottom = win.mVisibleFrame.bottom;
                    }
                }
            }
        }
        return foundTop;
    }

    @Override
    public void getDimBounds(Rect out) {
        DisplayContent displayContent = this.mStack.getDisplayContent();
        boolean zIsResizing = displayContent != null ? displayContent.mDividerControllerLocked.isResizing() : false;
        if (useCurrentBounds()) {
            if (inFreeformWorkspace() && getMaxVisibleBounds(out)) {
                return;
            }
            if (!this.mFullscreen) {
                if (zIsResizing) {
                    this.mStack.getBounds(out);
                } else {
                    this.mStack.getBounds(this.mTmpRect);
                    this.mTmpRect.intersect(this.mBounds);
                }
                out.set(this.mTmpRect);
                return;
            }
            out.set(this.mBounds);
            return;
        }
        displayContent.getLogicalDisplayRect(out);
    }

    void setDragResizing(boolean dragResizing, int dragResizeMode) {
        if (this.mDragResizing == dragResizing) {
            return;
        }
        if (!DragResizeMode.isModeAllowedForStack(this.mStack.mStackId, dragResizeMode) && !MultiWindowManager.isSupported()) {
            throw new IllegalArgumentException("Drag resize mode not allow for stack stackId=" + this.mStack.mStackId + " dragResizeMode=" + dragResizeMode);
        }
        this.mDragResizing = dragResizing;
        this.mDragResizeMode = dragResizeMode;
        resetDragResizingChangeReported();
    }

    void resetDragResizingChangeReported() {
        for (int activityNdx = this.mAppTokens.size() - 1; activityNdx >= 0; activityNdx--) {
            ArrayList<WindowState> windows = this.mAppTokens.get(activityNdx).allAppWindows;
            for (int winNdx = windows.size() - 1; winNdx >= 0; winNdx--) {
                WindowState win = windows.get(winNdx);
                win.resetDragResizingChangeReported();
            }
        }
    }

    boolean isDragResizing() {
        if (this.mDragResizing) {
            return true;
        }
        if (this.mStack != null) {
            return this.mStack.isDragResizing();
        }
        return false;
    }

    int getDragResizeMode() {
        return this.mDragResizeMode;
    }

    void addWindowsWaitingForDrawnIfResizingChanged() {
        for (int activityNdx = this.mAppTokens.size() - 1; activityNdx >= 0; activityNdx--) {
            ArrayList<WindowState> windows = this.mAppTokens.get(activityNdx).allAppWindows;
            for (int winNdx = windows.size() - 1; winNdx >= 0; winNdx--) {
                WindowState win = windows.get(winNdx);
                if (win.isDragResizeChanged()) {
                    this.mService.mWaitingForDrawn.add(win);
                }
            }
        }
    }

    void updateDisplayInfo(DisplayContent displayContent) {
        if (displayContent == null) {
            return;
        }
        if (this.mFullscreen) {
            setBounds(null, Configuration.EMPTY);
            return;
        }
        int newRotation = displayContent.getDisplayInfo().rotation;
        if (this.mRotation == newRotation) {
            return;
        }
        this.mTmpRect2.set(this.mPreScrollBounds);
        if (!ActivityManager.StackId.isTaskResizeAllowed(this.mStack.mStackId)) {
            setBounds(this.mTmpRect2, this.mOverrideConfig);
            return;
        }
        displayContent.rotateBounds(this.mRotation, newRotation, this.mTmpRect2);
        if (setBounds(this.mTmpRect2, this.mOverrideConfig) == 0) {
            return;
        }
        this.mService.mH.obtainMessage(43, this.mTaskId, 1, this.mPreScrollBounds).sendToTarget();
    }

    void resizeWindows() {
        ArrayList<WindowState> resizingWindows = this.mService.mResizingWindows;
        for (int activityNdx = this.mAppTokens.size() - 1; activityNdx >= 0; activityNdx--) {
            AppWindowToken atoken = this.mAppTokens.get(activityNdx);
            atoken.destroySavedSurfaces();
            ArrayList<WindowState> windows = atoken.allAppWindows;
            for (int winNdx = windows.size() - 1; winNdx >= 0; winNdx--) {
                WindowState win = windows.get(winNdx);
                if (win.mHasSurface && !resizingWindows.contains(win)) {
                    if (WindowManagerDebugConfig.DEBUG_RESIZE) {
                        Slog.d(TAG, "resizeWindows: Resizing " + win);
                    }
                    resizingWindows.add(win);
                    if (!win.computeDragResizing() && win.mAttrs.type == 1 && !this.mStack.getBoundsAnimating() && !win.isGoneForLayoutLw() && !inPinnedWorkspace()) {
                        win.setResizedWhileNotDragResizing(true);
                    }
                }
                if (win.isGoneForLayoutLw()) {
                    win.mResizedWhileGone = true;
                }
            }
        }
    }

    void moveWindows() {
        for (int activityNdx = this.mAppTokens.size() - 1; activityNdx >= 0; activityNdx--) {
            ArrayList<WindowState> windows = this.mAppTokens.get(activityNdx).allAppWindows;
            for (int winNdx = windows.size() - 1; winNdx >= 0; winNdx--) {
                WindowState win = windows.get(winNdx);
                if (WindowManagerDebugConfig.DEBUG_RESIZE) {
                    Slog.d(TAG, "moveWindows: Moving " + win);
                }
                win.mMovedByResize = true;
            }
        }
    }

    void cancelTaskWindowTransition() {
        for (int activityNdx = this.mAppTokens.size() - 1; activityNdx >= 0; activityNdx--) {
            this.mAppTokens.get(activityNdx).mAppAnimator.clearAnimation();
        }
    }

    void cancelTaskThumbnailTransition() {
        for (int activityNdx = this.mAppTokens.size() - 1; activityNdx >= 0; activityNdx--) {
            this.mAppTokens.get(activityNdx).mAppAnimator.clearThumbnail();
        }
    }

    boolean showForAllUsers() {
        int tokensCount = this.mAppTokens.size();
        if (tokensCount != 0) {
            return this.mAppTokens.get(tokensCount - 1).showForAllUsers;
        }
        return false;
    }

    boolean isVisibleForUser() {
        for (int i = this.mAppTokens.size() - 1; i >= 0; i--) {
            AppWindowToken appToken = this.mAppTokens.get(i);
            for (int j = appToken.allAppWindows.size() - 1; j >= 0; j--) {
                WindowState window = appToken.allAppWindows.get(j);
                if (!window.isHiddenFromUserLocked()) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean isVisible() {
        for (int i = this.mAppTokens.size() - 1; i >= 0; i--) {
            AppWindowToken appToken = this.mAppTokens.get(i);
            if (appToken.isVisible()) {
                return true;
            }
        }
        return false;
    }

    boolean inHomeStack() {
        return this.mStack != null && this.mStack.mStackId == 0;
    }

    boolean inFreeformWorkspace() {
        return this.mStack != null && this.mStack.mStackId == 2;
    }

    boolean inDockedWorkspace() {
        return this.mStack != null && this.mStack.mStackId == 3;
    }

    boolean inPinnedWorkspace() {
        return this.mStack != null && this.mStack.mStackId == 4;
    }

    boolean isResizeableByDockedStack() {
        DisplayContent displayContent = getDisplayContent();
        if (displayContent == null || displayContent.getDockedStackLocked() == null || this.mStack == null) {
            return false;
        }
        return ActivityManager.StackId.isTaskResizeableByDockedStack(this.mStack.mStackId);
    }

    boolean isFloating() {
        return ActivityManager.StackId.tasksAreFloating(this.mStack.mStackId);
    }

    boolean isDockedInEffect() {
        if (inDockedWorkspace()) {
            return true;
        }
        return isResizeableByDockedStack();
    }

    boolean isTwoFingerScrollMode() {
        if (inCropWindowsResizeMode()) {
            return isDockedInEffect();
        }
        return false;
    }

    WindowState getTopVisibleAppMainWindow() {
        AppWindowToken token = getTopVisibleAppToken();
        if (token != null) {
            return token.findMainWindow();
        }
        return null;
    }

    AppWindowToken getTopVisibleAppToken() {
        for (int i = this.mAppTokens.size() - 1; i >= 0; i--) {
            AppWindowToken token = this.mAppTokens.get(i);
            if (!token.mIsExiting && !token.clientHidden && !token.hiddenRequested) {
                return token;
            }
        }
        return null;
    }

    AppWindowToken getTopAppToken() {
        if (this.mAppTokens.size() > 0) {
            return this.mAppTokens.get(this.mAppTokens.size() - 1);
        }
        return null;
    }

    @Override
    public boolean dimFullscreen() {
        if (isHomeTask()) {
            return true;
        }
        return isFullscreen();
    }

    boolean isFullscreen() {
        if (useCurrentBounds()) {
            return this.mFullscreen;
        }
        return true;
    }

    @Override
    public DisplayInfo getDisplayInfo() {
        return this.mStack.getDisplayContent().getDisplayInfo();
    }

    public String toString() {
        return "{taskId=" + this.mTaskId + " appTokens=" + this.mAppTokens + " mdr=" + this.mDeferRemoval + "}";
    }

    @Override
    public String toShortString() {
        return "Task=" + this.mTaskId;
    }

    public void dump(String prefix, PrintWriter pw) {
        String doublePrefix = prefix + "  ";
        pw.println(prefix + "taskId=" + this.mTaskId);
        if (MultiWindowManager.isSupported()) {
            pw.println(doublePrefix + "mSticky=" + this.mSticky);
        }
        pw.println(doublePrefix + "mFullscreen=" + this.mFullscreen);
        pw.println(doublePrefix + "mBounds=" + this.mBounds.toShortString());
        pw.println(doublePrefix + "mdr=" + this.mDeferRemoval);
        pw.println(doublePrefix + "appTokens=" + this.mAppTokens);
        pw.println(doublePrefix + "mTempInsetBounds=" + this.mTempInsetBounds.toShortString());
        String triplePrefix = doublePrefix + "  ";
        for (int i = this.mAppTokens.size() - 1; i >= 0; i--) {
            AppWindowToken wtoken = this.mAppTokens.get(i);
            pw.println(triplePrefix + "Activity #" + i + " " + wtoken);
            wtoken.dump(pw, triplePrefix);
        }
    }
}
