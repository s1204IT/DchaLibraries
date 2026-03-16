package com.android.server.wm;

import android.R;
import android.graphics.Rect;
import android.util.EventLog;
import android.util.TypedValue;
import com.android.server.EventLogTags;
import java.io.PrintWriter;
import java.util.ArrayList;

public class TaskStack {
    private static final int DEFAULT_DIM_DURATION = 200;
    WindowStateAnimator mAnimationBackgroundAnimator;
    DimLayer mAnimationBackgroundSurface;
    boolean mDeferDetach;
    private DimLayer mDimLayer;
    WindowStateAnimator mDimWinAnimator;
    boolean mDimmingTag;
    private DisplayContent mDisplayContent;
    private final WindowManagerService mService;
    final int mStackId;
    private final ArrayList<Task> mTasks = new ArrayList<>();
    private Rect mTmpRect = new Rect();
    private Rect mBounds = new Rect();
    private boolean mFullscreen = true;
    final AppTokenList mExitingAppTokens = new AppTokenList();

    TaskStack(WindowManagerService service, int stackId) {
        this.mService = service;
        this.mStackId = stackId;
        EventLog.writeEvent(EventLogTags.WM_STACK_CREATED, Integer.valueOf(stackId), Integer.valueOf(this.mBounds.left), Integer.valueOf(this.mBounds.top), Integer.valueOf(this.mBounds.right), Integer.valueOf(this.mBounds.bottom));
    }

    DisplayContent getDisplayContent() {
        return this.mDisplayContent;
    }

    ArrayList<Task> getTasks() {
        return this.mTasks;
    }

    void resizeWindows() {
        boolean underStatusBar = this.mBounds.top == 0;
        ArrayList<WindowState> resizingWindows = this.mService.mResizingWindows;
        for (int taskNdx = this.mTasks.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<AppWindowToken> activities = this.mTasks.get(taskNdx).mAppTokens;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ArrayList<WindowState> windows = activities.get(activityNdx).allAppWindows;
                for (int winNdx = windows.size() - 1; winNdx >= 0; winNdx--) {
                    WindowState win = windows.get(winNdx);
                    if (!resizingWindows.contains(win)) {
                        resizingWindows.add(win);
                    }
                    win.mUnderStatusBar = underStatusBar;
                }
            }
        }
    }

    boolean setBounds(Rect bounds) {
        boolean oldFullscreen = this.mFullscreen;
        if (this.mDisplayContent != null) {
            this.mDisplayContent.getLogicalDisplayRect(this.mTmpRect);
            this.mFullscreen = this.mTmpRect.equals(bounds);
        }
        if (this.mBounds.equals(bounds) && oldFullscreen == this.mFullscreen) {
            return false;
        }
        this.mDimLayer.setBounds(bounds);
        this.mAnimationBackgroundSurface.setBounds(bounds);
        this.mBounds.set(bounds);
        return true;
    }

    void getBounds(Rect out) {
        out.set(this.mBounds);
    }

    void updateDisplayInfo() {
        if (this.mFullscreen && this.mDisplayContent != null) {
            this.mDisplayContent.getLogicalDisplayRect(this.mTmpRect);
            setBounds(this.mTmpRect);
        }
    }

    boolean isFullscreen() {
        return this.mFullscreen;
    }

    boolean isAnimating() {
        for (int taskNdx = this.mTasks.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<AppWindowToken> activities = this.mTasks.get(taskNdx).mAppTokens;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ArrayList<WindowState> windows = activities.get(activityNdx).allAppWindows;
                for (int winNdx = windows.size() - 1; winNdx >= 0; winNdx--) {
                    WindowStateAnimator winAnimator = windows.get(winNdx).mWinAnimator;
                    if (winAnimator.isAnimating() || winAnimator.mWin.mExiting) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    void addTask(Task task, boolean toTop) {
        int stackNdx;
        if (!toTop) {
            stackNdx = 0;
        } else {
            stackNdx = this.mTasks.size();
            if (!this.mService.isCurrentProfileLocked(task.mUserId)) {
                do {
                    stackNdx--;
                    if (stackNdx < 0) {
                        break;
                    }
                } while (this.mService.isCurrentProfileLocked(this.mTasks.get(stackNdx).mUserId));
                stackNdx++;
            }
        }
        this.mTasks.add(stackNdx, task);
        task.mStack = this;
        this.mDisplayContent.moveStack(this, true);
        Object[] objArr = new Object[3];
        objArr[0] = Integer.valueOf(task.taskId);
        objArr[1] = Integer.valueOf(toTop ? 1 : 0);
        objArr[2] = Integer.valueOf(stackNdx);
        EventLog.writeEvent(EventLogTags.WM_TASK_MOVED, objArr);
    }

    void moveTaskToTop(Task task) {
        this.mTasks.remove(task);
        addTask(task, true);
    }

    void moveTaskToBottom(Task task) {
        this.mTasks.remove(task);
        addTask(task, false);
    }

    void removeTask(Task task) {
        this.mTasks.remove(task);
        if (this.mDisplayContent != null) {
            if (this.mTasks.isEmpty()) {
                this.mDisplayContent.moveStack(this, false);
            }
            this.mDisplayContent.layoutNeeded = true;
        }
    }

    void attachDisplayContent(DisplayContent displayContent) {
        if (this.mDisplayContent != null) {
            throw new IllegalStateException("attachDisplayContent: Already attached");
        }
        this.mDisplayContent = displayContent;
        this.mDimLayer = new DimLayer(this.mService, this, displayContent);
        this.mAnimationBackgroundSurface = new DimLayer(this.mService, this, displayContent);
        updateDisplayInfo();
    }

    void detachDisplay() {
        EventLog.writeEvent(EventLogTags.WM_STACK_REMOVED, this.mStackId);
        boolean doAnotherLayoutPass = false;
        for (int taskNdx = this.mTasks.size() - 1; taskNdx >= 0; taskNdx--) {
            AppTokenList appWindowTokens = this.mTasks.get(taskNdx).mAppTokens;
            for (int appNdx = appWindowTokens.size() - 1; appNdx >= 0; appNdx--) {
                WindowList appWindows = appWindowTokens.get(appNdx).allAppWindows;
                for (int winNdx = appWindows.size() - 1; winNdx >= 0; winNdx--) {
                    this.mService.removeWindowInnerLocked(null, appWindows.get(winNdx));
                    doAnotherLayoutPass = true;
                }
            }
        }
        if (doAnotherLayoutPass) {
            this.mService.requestTraversalLocked();
        }
        this.mAnimationBackgroundSurface.destroySurface();
        this.mAnimationBackgroundSurface = null;
        this.mDimLayer.destroySurface();
        this.mDimLayer = null;
        this.mDisplayContent = null;
    }

    void resetAnimationBackgroundAnimator() {
        this.mAnimationBackgroundAnimator = null;
        this.mAnimationBackgroundSurface.hide();
    }

    private long getDimBehindFadeDuration(long duration) {
        TypedValue tv = new TypedValue();
        this.mService.mContext.getResources().getValue(R.fraction.config_autoBrightnessAdjustmentMaxGamma, tv, true);
        if (tv.type == 6) {
            return (long) tv.getFraction(duration, duration);
        }
        if (tv.type >= 16 && tv.type <= 31) {
            return tv.data;
        }
        return duration;
    }

    boolean animateDimLayers() {
        int dimLayer;
        float dimAmount;
        long duration = 200;
        if (this.mDimWinAnimator == null) {
            dimLayer = this.mDimLayer.getLayer();
            dimAmount = 0.0f;
        } else {
            dimLayer = this.mDimWinAnimator.mAnimLayer - 1;
            dimAmount = this.mDimWinAnimator.mWin.mAttrs.dimAmount;
        }
        float targetAlpha = this.mDimLayer.getTargetAlpha();
        if (targetAlpha != dimAmount) {
            if (this.mDimWinAnimator == null) {
                this.mDimLayer.hide(200L);
            } else {
                if (this.mDimWinAnimator.mAnimating && this.mDimWinAnimator.mAnimation != null) {
                    duration = this.mDimWinAnimator.mAnimation.computeDurationHint();
                }
                if (targetAlpha > dimAmount) {
                    duration = getDimBehindFadeDuration(duration);
                }
                this.mDimLayer.show(dimLayer, dimAmount, duration);
            }
        } else if (this.mDimLayer.getLayer() != dimLayer) {
            this.mDimLayer.setLayer(dimLayer);
        }
        if (this.mDimLayer.isAnimating()) {
            if (!this.mService.okToDisplay()) {
                this.mDimLayer.show();
            } else {
                return this.mDimLayer.stepAnimation();
            }
        }
        return false;
    }

    void resetDimmingTag() {
        this.mDimmingTag = false;
    }

    void setDimmingTag() {
        this.mDimmingTag = true;
    }

    boolean testDimmingTag() {
        return this.mDimmingTag;
    }

    boolean isDimming() {
        return this.mDimLayer.isDimming();
    }

    boolean isDimming(WindowStateAnimator winAnimator) {
        return this.mDimWinAnimator == winAnimator && this.mDimLayer.isDimming();
    }

    void startDimmingIfNeeded(WindowStateAnimator newWinAnimator) {
        WindowStateAnimator existingDimWinAnimator = this.mDimWinAnimator;
        if (newWinAnimator.mSurfaceShown) {
            if (existingDimWinAnimator == null || !existingDimWinAnimator.mSurfaceShown || existingDimWinAnimator.mAnimLayer < newWinAnimator.mAnimLayer) {
                this.mDimWinAnimator = newWinAnimator;
            }
        }
    }

    void stopDimmingIfNeeded() {
        if (!this.mDimmingTag && isDimming()) {
            this.mDimWinAnimator = null;
        }
    }

    void setAnimationBackground(WindowStateAnimator winAnimator, int color) {
        int animLayer = winAnimator.mAnimLayer;
        if (this.mAnimationBackgroundAnimator == null || animLayer < this.mAnimationBackgroundAnimator.mAnimLayer) {
            this.mAnimationBackgroundAnimator = winAnimator;
            int animLayer2 = this.mService.adjustAnimationBackground(winAnimator);
            this.mAnimationBackgroundSurface.show(animLayer2 - 1, ((color >> 24) & 255) / 255.0f, 0L);
        }
    }

    void switchUser(int userId) {
        int top = this.mTasks.size();
        for (int taskNdx = 0; taskNdx < top; taskNdx++) {
            Task task = this.mTasks.get(taskNdx);
            if (this.mService.isCurrentProfileLocked(task.mUserId)) {
                this.mTasks.remove(taskNdx);
                this.mTasks.add(task);
                top--;
            }
        }
    }

    void close() {
        this.mDimLayer.mDimSurface.destroy();
        this.mAnimationBackgroundSurface.mDimSurface.destroy();
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("mStackId=");
        pw.println(this.mStackId);
        pw.print(prefix);
        pw.print("mDeferDetach=");
        pw.println(this.mDeferDetach);
        for (int taskNdx = 0; taskNdx < this.mTasks.size(); taskNdx++) {
            pw.print(prefix);
            pw.println(this.mTasks.get(taskNdx));
        }
        if (this.mAnimationBackgroundSurface.isDimming()) {
            pw.print(prefix);
            pw.println("mWindowAnimationBackgroundSurface:");
            this.mAnimationBackgroundSurface.printTo(prefix + "  ", pw);
        }
        if (this.mDimLayer.isDimming()) {
            pw.print(prefix);
            pw.println("mDimLayer:");
            this.mDimLayer.printTo(prefix, pw);
            pw.print(prefix);
            pw.print("mDimWinAnimator=");
            pw.println(this.mDimWinAnimator);
        }
        if (!this.mExitingAppTokens.isEmpty()) {
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
    }

    public String toString() {
        return "{stackId=" + this.mStackId + " tasks=" + this.mTasks + "}";
    }
}
