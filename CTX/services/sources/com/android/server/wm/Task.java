package com.android.server.wm;

import android.app.ActivityManager;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.EventLog;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.EventLogTags;
import java.io.PrintWriter;
import java.util.function.Consumer;

public class Task extends WindowContainer<AppWindowToken> {
    static final String TAG = "WindowManager";
    private boolean mCanAffectSystemUiFlags;
    private boolean mDeferRemoval;
    private Dimmer mDimmer;
    private int mDragResizeMode;
    private boolean mDragResizing;
    final Rect mPreparedFrozenBounds;
    final Configuration mPreparedFrozenMergedConfig;
    private boolean mPreserveNonFloatingState;
    private int mResizeMode;
    private int mRotation;
    TaskStack mStack;
    private boolean mSupportsPictureInPicture;
    private ActivityManager.TaskDescription mTaskDescription;
    final int mTaskId;
    private final Rect mTempInsetBounds;
    private final Rect mTmpDimBoundsRect;
    private Rect mTmpRect;
    private Rect mTmpRect2;
    private Rect mTmpRect3;
    final int mUserId;

    @Override
    public void commitPendingTransaction() {
        super.commitPendingTransaction();
    }

    @Override
    public int compareTo(WindowContainer windowContainer) {
        return super.compareTo(windowContainer);
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
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
    }

    @Override
    public void onOverrideConfigurationChanged(Configuration configuration) {
        super.onOverrideConfigurationChanged(configuration);
    }

    Task(int i, TaskStack taskStack, int i2, WindowManagerService windowManagerService, int i3, boolean z, ActivityManager.TaskDescription taskDescription, TaskWindowContainerController taskWindowContainerController) {
        super(windowManagerService);
        this.mDeferRemoval = false;
        this.mPreparedFrozenBounds = new Rect();
        this.mPreparedFrozenMergedConfig = new Configuration();
        this.mTempInsetBounds = new Rect();
        this.mTmpRect = new Rect();
        this.mTmpRect2 = new Rect();
        this.mTmpRect3 = new Rect();
        this.mPreserveNonFloatingState = false;
        this.mDimmer = new Dimmer(this);
        this.mTmpDimBoundsRect = new Rect();
        this.mCanAffectSystemUiFlags = true;
        this.mTaskId = i;
        this.mStack = taskStack;
        this.mUserId = i2;
        this.mResizeMode = i3;
        this.mSupportsPictureInPicture = z;
        setController(taskWindowContainerController);
        setBounds(getOverrideBounds());
        this.mTaskDescription = taskDescription;
        setOrientation(-2);
    }

    DisplayContent getDisplayContent() {
        if (this.mStack != null) {
            return this.mStack.getDisplayContent();
        }
        return null;
    }

    private int getAdjustedAddPosition(int i) {
        int size = this.mChildren.size();
        if (i >= size) {
            return Math.min(size, i);
        }
        for (int i2 = 0; i2 < size && i2 < i; i2++) {
            if (((AppWindowToken) this.mChildren.get(i2)).removed) {
                i++;
            }
        }
        return Math.min(size, i);
    }

    @Override
    void addChild(AppWindowToken appWindowToken, int i) {
        super.addChild(appWindowToken, getAdjustedAddPosition(i));
        this.mDeferRemoval = false;
    }

    @Override
    void positionChildAt(int i, AppWindowToken appWindowToken, boolean z) {
        super.positionChildAt(getAdjustedAddPosition(i), appWindowToken, z);
        this.mDeferRemoval = false;
    }

    private boolean hasWindowsAlive() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            if (((AppWindowToken) this.mChildren.get(size)).hasWindowsAlive()) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    boolean shouldDeferRemoval() {
        return hasWindowsAlive() && this.mStack.isSelfOrChildAnimating();
    }

    @Override
    void removeIfPossible() {
        if (shouldDeferRemoval()) {
            if (WindowManagerDebugConfig.DEBUG_STACK) {
                Slog.i("WindowManager", "removeTask: deferring removing taskId=" + this.mTaskId);
            }
            this.mDeferRemoval = true;
            return;
        }
        removeImmediately();
    }

    @Override
    void removeImmediately() {
        if (WindowManagerDebugConfig.DEBUG_STACK) {
            Slog.i("WindowManager", "removeTask: removing taskId=" + this.mTaskId);
        }
        EventLog.writeEvent(EventLogTags.WM_TASK_REMOVED, Integer.valueOf(this.mTaskId), "removeTask");
        this.mDeferRemoval = false;
        super.removeImmediately();
    }

    void reparent(TaskStack taskStack, int i, boolean z) {
        if (taskStack == this.mStack) {
            throw new IllegalArgumentException("task=" + this + " already child of stack=" + this.mStack);
        }
        if (WindowManagerDebugConfig.DEBUG_STACK) {
            Slog.i("WindowManager", "reParentTask: removing taskId=" + this.mTaskId + " from stack=" + this.mStack);
        }
        EventLog.writeEvent(EventLogTags.WM_TASK_REMOVED, Integer.valueOf(this.mTaskId), "reParentTask");
        DisplayContent displayContent = getDisplayContent();
        if (taskStack.inPinnedWindowingMode()) {
            this.mPreserveNonFloatingState = true;
        } else {
            this.mPreserveNonFloatingState = false;
        }
        getParent().removeChild(this);
        taskStack.addTask(this, i, showForAllUsers(), z);
        DisplayContent displayContent2 = taskStack.getDisplayContent();
        displayContent2.setLayoutNeeded();
        if (displayContent != displayContent2) {
            onDisplayChanged(displayContent2);
            displayContent.setLayoutNeeded();
        }
    }

    void positionAt(int i) {
        this.mStack.positionChildAt(i, this, false);
    }

    @Override
    void onParentSet() {
        super.onParentSet();
        updateDisplayInfo(getDisplayContent());
        if (getWindowConfiguration().windowsAreScaleable()) {
            forceWindowsScaleable(true);
        } else {
            forceWindowsScaleable(false);
        }
    }

    @Override
    void removeChild(AppWindowToken appWindowToken) {
        if (!this.mChildren.contains(appWindowToken)) {
            Slog.e("WindowManager", "removeChild: token=" + this + " not found.");
            return;
        }
        super.removeChild(appWindowToken);
        if (this.mChildren.isEmpty()) {
            EventLog.writeEvent(EventLogTags.WM_TASK_REMOVED, Integer.valueOf(this.mTaskId), "removeAppToken: last token");
            if (this.mDeferRemoval) {
                removeIfPossible();
            }
        }
    }

    void setSendingToBottom(boolean z) {
        for (int i = 0; i < this.mChildren.size(); i++) {
            ((AppWindowToken) this.mChildren.get(i)).sendingToBottom = z;
        }
    }

    public int setBounds(Rect rect, boolean z) {
        int bounds = setBounds(rect);
        if (z && (bounds & 2) != 2) {
            onResize();
            return bounds | 2;
        }
        return bounds;
    }

    @Override
    public int setBounds(Rect rect) {
        int i;
        DisplayContent displayContent = this.mStack.getDisplayContent();
        if (displayContent != null) {
            i = displayContent.getDisplayInfo().rotation;
        } else {
            if (rect == null) {
                return 0;
            }
            i = 0;
        }
        if (equivalentOverrideBounds(rect)) {
            return 0;
        }
        int bounds = super.setBounds(rect);
        this.mRotation = i;
        return bounds;
    }

    void setTempInsetBounds(Rect rect) {
        if (rect != null) {
            this.mTempInsetBounds.set(rect);
        } else {
            this.mTempInsetBounds.setEmpty();
        }
    }

    void getTempInsetBounds(Rect rect) {
        rect.set(this.mTempInsetBounds);
    }

    void setResizeable(int i) {
        this.mResizeMode = i;
    }

    boolean isResizeable() {
        return ActivityInfo.isResizeableMode(this.mResizeMode) || this.mSupportsPictureInPicture || this.mService.mForceResizableTasks;
    }

    boolean preserveOrientationOnResize() {
        return this.mResizeMode == 6 || this.mResizeMode == 5 || this.mResizeMode == 7;
    }

    boolean cropWindowsToStackBounds() {
        return isResizeable();
    }

    void prepareFreezingBounds() {
        this.mPreparedFrozenBounds.set(getBounds());
        this.mPreparedFrozenMergedConfig.setTo(getConfiguration());
    }

    void alignToAdjustedBounds(Rect rect, Rect rect2, boolean z) {
        if (!isResizeable() || Configuration.EMPTY.equals(getOverrideConfiguration())) {
            return;
        }
        getBounds(this.mTmpRect2);
        if (z) {
            this.mTmpRect2.offset(0, rect.bottom - this.mTmpRect2.bottom);
        } else {
            this.mTmpRect2.offsetTo(rect.left, rect.top);
        }
        setTempInsetBounds(rect2);
        setBounds(this.mTmpRect2, false);
    }

    private boolean useCurrentBounds() {
        DisplayContent displayContent = getDisplayContent();
        return matchParentBounds() || !inSplitScreenSecondaryWindowingMode() || displayContent == null || displayContent.getSplitScreenPrimaryStackIgnoringVisibility() != null;
    }

    @Override
    public void getBounds(Rect rect) {
        if (useCurrentBounds()) {
            super.getBounds(rect);
        } else {
            this.mStack.getDisplayContent().getBounds(rect);
        }
    }

    boolean getMaxVisibleBounds(Rect rect) {
        WindowState windowStateFindMainWindow;
        boolean z = false;
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            AppWindowToken appWindowToken = (AppWindowToken) this.mChildren.get(size);
            if (!appWindowToken.mIsExiting && !appWindowToken.isClientHidden() && !appWindowToken.hiddenRequested && (windowStateFindMainWindow = appWindowToken.findMainWindow()) != null) {
                if (!z) {
                    rect.set(windowStateFindMainWindow.mVisibleFrame);
                    z = true;
                } else {
                    if (windowStateFindMainWindow.mVisibleFrame.left < rect.left) {
                        rect.left = windowStateFindMainWindow.mVisibleFrame.left;
                    }
                    if (windowStateFindMainWindow.mVisibleFrame.top < rect.top) {
                        rect.top = windowStateFindMainWindow.mVisibleFrame.top;
                    }
                    if (windowStateFindMainWindow.mVisibleFrame.right > rect.right) {
                        rect.right = windowStateFindMainWindow.mVisibleFrame.right;
                    }
                    if (windowStateFindMainWindow.mVisibleFrame.bottom > rect.bottom) {
                        rect.bottom = windowStateFindMainWindow.mVisibleFrame.bottom;
                    }
                }
            }
        }
        return z;
    }

    public void getDimBounds(Rect rect) {
        if (this.mStack == null) {
            Slog.w("WindowManager", "getDimBounds: mStack has been removed.");
            return;
        }
        DisplayContent displayContent = this.mStack.getDisplayContent();
        boolean z = displayContent != null && displayContent.mDividerControllerLocked.isResizing();
        if (useCurrentBounds()) {
            if (inFreeformWindowingMode() && getMaxVisibleBounds(rect)) {
                return;
            }
            if (!matchParentBounds()) {
                if (z) {
                    this.mStack.getBounds(rect);
                    return;
                }
                this.mStack.getBounds(this.mTmpRect);
                this.mTmpRect.intersect(getBounds());
                rect.set(this.mTmpRect);
                return;
            }
            rect.set(getBounds());
            return;
        }
        if (displayContent != null) {
            displayContent.getBounds(rect);
        }
    }

    void setDragResizing(boolean z, int i) {
        if (this.mDragResizing != z) {
            if (!DragResizeMode.isModeAllowedForStack(this.mStack, i)) {
                throw new IllegalArgumentException("Drag resize mode not allow for stack stackId=" + this.mStack.mStackId + " dragResizeMode=" + i);
            }
            this.mDragResizing = z;
            this.mDragResizeMode = i;
            resetDragResizingChangeReported();
        }
    }

    boolean isDragResizing() {
        return this.mDragResizing;
    }

    int getDragResizeMode() {
        return this.mDragResizeMode;
    }

    void updateDisplayInfo(DisplayContent displayContent) {
        TaskWindowContainerController controller;
        if (displayContent == null) {
            return;
        }
        if (matchParentBounds()) {
            setBounds(null);
            return;
        }
        int i = displayContent.getDisplayInfo().rotation;
        if (this.mRotation == i) {
            return;
        }
        this.mTmpRect2.set(getBounds());
        if (!getWindowConfiguration().canResizeTask()) {
            setBounds(this.mTmpRect2);
            return;
        }
        displayContent.rotateBounds(this.mRotation, i, this.mTmpRect2);
        if (setBounds(this.mTmpRect2) != 0 && (controller = getController()) != null) {
            controller.requestResize(getBounds(), 1);
        }
    }

    void cancelTaskWindowTransition() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((AppWindowToken) this.mChildren.get(size)).cancelAnimation();
        }
    }

    boolean showForAllUsers() {
        int size = this.mChildren.size();
        return size != 0 && ((AppWindowToken) this.mChildren.get(size - 1)).mShowForAllUsers;
    }

    boolean isFloating() {
        return (!getWindowConfiguration().tasksAreFloating() || this.mStack.isAnimatingBoundsToFullscreen() || this.mPreserveNonFloatingState) ? false : true;
    }

    @Override
    public SurfaceControl getAnimationLeashParent() {
        return getAppAnimationLayer(0);
    }

    boolean isTaskAnimating() {
        RecentsAnimationController recentsAnimationController = this.mService.getRecentsAnimationController();
        if (recentsAnimationController != null && recentsAnimationController.isAnimatingTask(this)) {
            return true;
        }
        return false;
    }

    WindowState getTopVisibleAppMainWindow() {
        AppWindowToken topVisibleAppToken = getTopVisibleAppToken();
        if (topVisibleAppToken != null) {
            return topVisibleAppToken.findMainWindow();
        }
        return null;
    }

    AppWindowToken getTopFullscreenAppToken() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            AppWindowToken appWindowToken = (AppWindowToken) this.mChildren.get(size);
            WindowState windowStateFindMainWindow = appWindowToken.findMainWindow();
            if (windowStateFindMainWindow != null && windowStateFindMainWindow.mAttrs.isFullscreen()) {
                return appWindowToken;
            }
        }
        return null;
    }

    AppWindowToken getTopVisibleAppToken() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            AppWindowToken appWindowToken = (AppWindowToken) this.mChildren.get(size);
            if (!appWindowToken.mIsExiting && !appWindowToken.isClientHidden() && !appWindowToken.hiddenRequested) {
                return appWindowToken;
            }
        }
        return null;
    }

    public boolean isFullscreen() {
        if (useCurrentBounds()) {
            return matchParentBounds();
        }
        return true;
    }

    void forceWindowsScaleable(boolean z) {
        this.mService.openSurfaceTransaction();
        try {
            for (int size = this.mChildren.size() - 1; size >= 0; size--) {
                ((AppWindowToken) this.mChildren.get(size)).forceWindowsScaleableInTransaction(z);
            }
        } finally {
            this.mService.closeSurfaceTransaction("forceWindowsScaleable");
        }
    }

    void setTaskDescription(ActivityManager.TaskDescription taskDescription) {
        this.mTaskDescription = taskDescription;
    }

    ActivityManager.TaskDescription getTaskDescription() {
        return this.mTaskDescription;
    }

    @Override
    boolean fillsParent() {
        return matchParentBounds() || !getWindowConfiguration().canResizeTask();
    }

    @Override
    TaskWindowContainerController getController() {
        return (TaskWindowContainerController) super.getController();
    }

    @Override
    void forAllTasks(Consumer<Task> consumer) {
        consumer.accept(this);
    }

    void setCanAffectSystemUiFlags(boolean z) {
        this.mCanAffectSystemUiFlags = z;
    }

    boolean canAffectSystemUiFlags() {
        return this.mCanAffectSystemUiFlags;
    }

    void dontAnimateDimExit() {
        this.mDimmer.dontAnimateExit();
    }

    public String toString() {
        return "{taskId=" + this.mTaskId + " appTokens=" + this.mChildren + " mdr=" + this.mDeferRemoval + "}";
    }

    @Override
    String getName() {
        return toShortString();
    }

    void clearPreserveNonFloatingState() {
        this.mPreserveNonFloatingState = false;
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

    @Override
    public void writeToProto(ProtoOutputStream protoOutputStream, long j, boolean z) {
        long jStart = protoOutputStream.start(j);
        super.writeToProto(protoOutputStream, 1146756268033L, z);
        protoOutputStream.write(1120986464258L, this.mTaskId);
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((AppWindowToken) this.mChildren.get(size)).writeToProto(protoOutputStream, 2246267895811L, z);
        }
        protoOutputStream.write(1133871366148L, matchParentBounds());
        getBounds().writeToProto(protoOutputStream, 1146756268037L);
        this.mTempInsetBounds.writeToProto(protoOutputStream, 1146756268038L);
        protoOutputStream.write(1133871366151L, this.mDeferRemoval);
        protoOutputStream.end(jStart);
    }

    @Override
    public void dump(PrintWriter printWriter, String str, boolean z) {
        super.dump(printWriter, str, z);
        String str2 = str + "  ";
        printWriter.println(str + "taskId=" + this.mTaskId);
        printWriter.println(str2 + "mBounds=" + getBounds().toShortString());
        printWriter.println(str2 + "mdr=" + this.mDeferRemoval);
        printWriter.println(str2 + "appTokens=" + this.mChildren);
        printWriter.println(str2 + "mTempInsetBounds=" + this.mTempInsetBounds.toShortString());
        String str3 = str2 + "  ";
        String str4 = str3 + "  ";
        for (int size = this.mChildren.size() - 1; size >= 0; size += -1) {
            AppWindowToken appWindowToken = (AppWindowToken) this.mChildren.get(size);
            printWriter.println(str3 + "Activity #" + size + " " + appWindowToken);
            appWindowToken.dump(printWriter, str4, z);
        }
    }

    String toShortString() {
        return "Task=" + this.mTaskId;
    }
}
