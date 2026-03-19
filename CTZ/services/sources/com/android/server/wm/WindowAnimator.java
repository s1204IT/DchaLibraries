package com.android.server.wm;

import android.content.Context;
import android.os.Trace;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.view.Choreographer;
import android.view.SurfaceControl;
import com.android.server.AnimationThread;
import com.android.server.policy.WindowManagerPolicy;
import java.io.PrintWriter;
import java.util.ArrayList;

public class WindowAnimator {
    private static final String TAG = "WindowManager";
    int mAnimTransactionSequence;
    private boolean mAnimating;
    final Choreographer.FrameCallback mAnimationFrameCallback;
    private boolean mAnimationFrameCallbackScheduled;
    boolean mAppWindowAnimating;
    private Choreographer mChoreographer;
    final Context mContext;
    long mCurrentTime;
    private boolean mInExecuteAfterPrepareSurfacesRunnables;
    private boolean mLastRootAnimating;
    Object mLastWindowFreezeSource;
    final WindowManagerPolicy mPolicy;
    final WindowManagerService mService;
    WindowState mWindowDetachedWallpaper = null;
    int mBulkUpdateParams = 0;
    SparseArray<DisplayContentsAnimator> mDisplayContentsAnimators = new SparseArray<>(2);
    private boolean mInitialized = false;
    private boolean mRemoveReplacedWindows = false;
    private final ArrayList<Runnable> mAfterPrepareSurfacesRunnables = new ArrayList<>();
    private final SurfaceControl.Transaction mTransaction = new SurfaceControl.Transaction();

    WindowAnimator(WindowManagerService windowManagerService) {
        this.mService = windowManagerService;
        this.mContext = windowManagerService.mContext;
        this.mPolicy = windowManagerService.mPolicy;
        AnimationThread.getHandler().runWithScissors(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mChoreographer = Choreographer.getSfInstance();
            }
        }, 0L);
        this.mAnimationFrameCallback = new Choreographer.FrameCallback() {
            @Override
            public final void doFrame(long j) {
                WindowAnimator.lambda$new$1(this.f$0, j);
            }
        };
    }

    public static void lambda$new$1(WindowAnimator windowAnimator, long j) {
        synchronized (windowAnimator.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                windowAnimator.mAnimationFrameCallbackScheduled = false;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        Trace.traceBegin(32L, "wmAnimate");
        windowAnimator.animate(j);
        Trace.traceEnd(32L);
    }

    void addDisplayLocked(int i) {
        getDisplayContentsAnimatorLocked(i);
        if (i == 0) {
            this.mInitialized = true;
        }
    }

    void removeDisplayLocked(int i) {
        DisplayContentsAnimator displayContentsAnimator = this.mDisplayContentsAnimators.get(i);
        if (displayContentsAnimator != null && displayContentsAnimator.mScreenRotationAnimation != null) {
            displayContentsAnimator.mScreenRotationAnimation.kill();
            displayContentsAnimator.mScreenRotationAnimation = null;
        }
        this.mDisplayContentsAnimators.delete(i);
    }

    private void animate(long j) {
        String str;
        String str2;
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mInitialized) {
                    scheduleAnimation();
                    WindowManagerService.resetPriorityAfterLockedSection();
                    synchronized (this.mService.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            this.mCurrentTime = j / 1000000;
                            this.mBulkUpdateParams = 8;
                            this.mAnimating = false;
                            if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
                                Slog.i("WindowManager", "!!! animate: entry time=" + this.mCurrentTime);
                            }
                            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                                Slog.i("WindowManager", ">>> OPEN TRANSACTION animate");
                            }
                            this.mService.openSurfaceTransaction();
                            try {
                                try {
                                    AccessibilityController accessibilityController = this.mService.mAccessibilityController;
                                    int size = this.mDisplayContentsAnimators.size();
                                    for (int i = 0; i < size; i++) {
                                        DisplayContent displayContent = this.mService.mRoot.getDisplayContent(this.mDisplayContentsAnimators.keyAt(i));
                                        DisplayContentsAnimator displayContentsAnimatorValueAt = this.mDisplayContentsAnimators.valueAt(i);
                                        ScreenRotationAnimation screenRotationAnimation = displayContentsAnimatorValueAt.mScreenRotationAnimation;
                                        if (screenRotationAnimation != null && screenRotationAnimation.isAnimating()) {
                                            if (screenRotationAnimation.stepAnimationLocked(this.mCurrentTime)) {
                                                setAnimating(true);
                                            } else {
                                                this.mBulkUpdateParams |= 1;
                                                screenRotationAnimation.kill();
                                                displayContentsAnimatorValueAt.mScreenRotationAnimation = null;
                                                if (accessibilityController != null && displayContent.isDefaultDisplay) {
                                                    accessibilityController.onRotationChangedLocked(this.mService.getDefaultDisplayContentLocked());
                                                }
                                            }
                                        }
                                        this.mAnimTransactionSequence++;
                                        displayContent.updateWindowsForAnimator(this);
                                        displayContent.updateWallpaperForAnimator(this);
                                        displayContent.prepareSurfaces();
                                    }
                                    for (int i2 = 0; i2 < size; i2++) {
                                        DisplayContent displayContent2 = this.mService.mRoot.getDisplayContent(this.mDisplayContentsAnimators.keyAt(i2));
                                        displayContent2.checkAppWindowsReadyToShow();
                                        ScreenRotationAnimation screenRotationAnimation2 = this.mDisplayContentsAnimators.valueAt(i2).mScreenRotationAnimation;
                                        if (screenRotationAnimation2 != null) {
                                            screenRotationAnimation2.updateSurfaces(this.mTransaction);
                                        }
                                        orAnimating(displayContent2.getDockedDividerController().animate(this.mCurrentTime));
                                        if (accessibilityController != null && displayContent2.isDefaultDisplay) {
                                            accessibilityController.drawMagnifiedRegionBorderIfNeededLocked();
                                        }
                                    }
                                    if (!this.mAnimating) {
                                        cancelAnimation();
                                    }
                                    if (this.mService.mWatermark != null) {
                                        this.mService.mWatermark.drawIfNeeded();
                                    }
                                    SurfaceControl.mergeToGlobalTransaction(this.mTransaction);
                                    this.mService.closeSurfaceTransaction("WindowAnimator");
                                } catch (Throwable th) {
                                    this.mService.closeSurfaceTransaction("WindowAnimator");
                                    if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                                        Slog.i("WindowManager", "<<< CLOSE TRANSACTION animate");
                                    }
                                    throw th;
                                }
                            } catch (RuntimeException e) {
                                Slog.wtf("WindowManager", "Unhandled exception in Window Manager", e);
                                this.mService.closeSurfaceTransaction("WindowAnimator");
                                if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                                    str = "WindowManager";
                                    str2 = "<<< CLOSE TRANSACTION animate";
                                }
                            }
                            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                                str = "WindowManager";
                                str2 = "<<< CLOSE TRANSACTION animate";
                                Slog.i(str, str2);
                            }
                            boolean zHasPendingLayoutChanges = this.mService.mRoot.hasPendingLayoutChanges(this);
                            boolean zCopyAnimToLayoutParams = this.mBulkUpdateParams != 0 ? this.mService.mRoot.copyAnimToLayoutParams() : false;
                            if (zHasPendingLayoutChanges || zCopyAnimToLayoutParams) {
                                this.mService.mWindowPlacerLocked.requestTraversal();
                            }
                            boolean zIsSelfOrChildAnimating = this.mService.mRoot.isSelfOrChildAnimating();
                            if (zIsSelfOrChildAnimating && !this.mLastRootAnimating) {
                                this.mService.mTaskSnapshotController.setPersisterPaused(true);
                                Trace.asyncTraceBegin(32L, "animating", 0);
                            }
                            if (!zIsSelfOrChildAnimating && this.mLastRootAnimating) {
                                this.mService.mWindowPlacerLocked.requestTraversal();
                                this.mService.mTaskSnapshotController.setPersisterPaused(false);
                                Trace.asyncTraceEnd(32L, "animating", 0);
                            }
                            this.mLastRootAnimating = zIsSelfOrChildAnimating;
                            if (this.mRemoveReplacedWindows) {
                                this.mService.mRoot.removeReplacedWindows();
                                this.mRemoveReplacedWindows = false;
                            }
                            this.mService.destroyPreservedSurfaceLocked();
                            executeAfterPrepareSurfacesRunnables();
                            if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
                                Slog.i("WindowManager", "!!! animate: exit mAnimating=" + this.mAnimating + " mBulkUpdateParams=" + Integer.toHexString(this.mBulkUpdateParams) + " mPendingLayoutChanges(DEFAULT_DISPLAY)=" + Integer.toHexString(getPendingLayoutChanges(0)));
                            }
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    private static String bulkUpdateParamsToString(int i) {
        StringBuilder sb = new StringBuilder(128);
        if ((i & 1) != 0) {
            sb.append(" UPDATE_ROTATION");
        }
        if ((i & 2) != 0) {
            sb.append(" WALLPAPER_MAY_CHANGE");
        }
        if ((i & 4) != 0) {
            sb.append(" FORCE_HIDING_CHANGED");
        }
        if ((i & 8) != 0) {
            sb.append(" ORIENTATION_CHANGE_COMPLETE");
        }
        return sb.toString();
    }

    public void dumpLocked(PrintWriter printWriter, String str, boolean z) {
        String str2 = "  " + str;
        String str3 = "  " + str2;
        for (int i = 0; i < this.mDisplayContentsAnimators.size(); i++) {
            printWriter.print(str);
            printWriter.print("DisplayContentsAnimator #");
            printWriter.print(this.mDisplayContentsAnimators.keyAt(i));
            printWriter.println(":");
            DisplayContentsAnimator displayContentsAnimatorValueAt = this.mDisplayContentsAnimators.valueAt(i);
            this.mService.mRoot.getDisplayContent(this.mDisplayContentsAnimators.keyAt(i)).dumpWindowAnimators(printWriter, str2);
            if (displayContentsAnimatorValueAt.mScreenRotationAnimation != null) {
                printWriter.print(str2);
                printWriter.println("mScreenRotationAnimation:");
                displayContentsAnimatorValueAt.mScreenRotationAnimation.printTo(str3, printWriter);
            } else if (z) {
                printWriter.print(str2);
                printWriter.println("no ScreenRotationAnimation ");
            }
            printWriter.println();
        }
        printWriter.println();
        if (z) {
            printWriter.print(str);
            printWriter.print("mAnimTransactionSequence=");
            printWriter.print(this.mAnimTransactionSequence);
            printWriter.print(str);
            printWriter.print("mCurrentTime=");
            printWriter.println(TimeUtils.formatUptime(this.mCurrentTime));
        }
        if (this.mBulkUpdateParams != 0) {
            printWriter.print(str);
            printWriter.print("mBulkUpdateParams=0x");
            printWriter.print(Integer.toHexString(this.mBulkUpdateParams));
            printWriter.println(bulkUpdateParamsToString(this.mBulkUpdateParams));
        }
        if (this.mWindowDetachedWallpaper != null) {
            printWriter.print(str);
            printWriter.print("mWindowDetachedWallpaper=");
            printWriter.println(this.mWindowDetachedWallpaper);
        }
    }

    int getPendingLayoutChanges(int i) {
        DisplayContent displayContent;
        if (i >= 0 && (displayContent = this.mService.mRoot.getDisplayContent(i)) != null) {
            return displayContent.pendingLayoutChanges;
        }
        return 0;
    }

    void setPendingLayoutChanges(int i, int i2) {
        DisplayContent displayContent;
        if (i >= 0 && (displayContent = this.mService.mRoot.getDisplayContent(i)) != null) {
            displayContent.pendingLayoutChanges = i2 | displayContent.pendingLayoutChanges;
        }
    }

    private DisplayContentsAnimator getDisplayContentsAnimatorLocked(int i) {
        if (i < 0) {
            return null;
        }
        DisplayContentsAnimator displayContentsAnimator = this.mDisplayContentsAnimators.get(i);
        if (displayContentsAnimator == null && this.mService.mRoot.getDisplayContent(i) != null) {
            DisplayContentsAnimator displayContentsAnimator2 = new DisplayContentsAnimator();
            this.mDisplayContentsAnimators.put(i, displayContentsAnimator2);
            return displayContentsAnimator2;
        }
        return displayContentsAnimator;
    }

    void setScreenRotationAnimationLocked(int i, ScreenRotationAnimation screenRotationAnimation) {
        DisplayContentsAnimator displayContentsAnimatorLocked = getDisplayContentsAnimatorLocked(i);
        if (displayContentsAnimatorLocked != null) {
            displayContentsAnimatorLocked.mScreenRotationAnimation = screenRotationAnimation;
        }
    }

    ScreenRotationAnimation getScreenRotationAnimationLocked(int i) {
        DisplayContentsAnimator displayContentsAnimatorLocked;
        if (i >= 0 && (displayContentsAnimatorLocked = getDisplayContentsAnimatorLocked(i)) != null) {
            return displayContentsAnimatorLocked.mScreenRotationAnimation;
        }
        return null;
    }

    void requestRemovalOfReplacedWindows(WindowState windowState) {
        this.mRemoveReplacedWindows = true;
    }

    void scheduleAnimation() {
        if (!this.mAnimationFrameCallbackScheduled) {
            this.mAnimationFrameCallbackScheduled = true;
            this.mChoreographer.postFrameCallback(this.mAnimationFrameCallback);
        }
    }

    private void cancelAnimation() {
        if (this.mAnimationFrameCallbackScheduled) {
            this.mAnimationFrameCallbackScheduled = false;
            this.mChoreographer.removeFrameCallback(this.mAnimationFrameCallback);
        }
    }

    private class DisplayContentsAnimator {
        ScreenRotationAnimation mScreenRotationAnimation;

        private DisplayContentsAnimator() {
            this.mScreenRotationAnimation = null;
        }
    }

    boolean isAnimating() {
        return this.mAnimating;
    }

    boolean isAnimationScheduled() {
        return this.mAnimationFrameCallbackScheduled;
    }

    Choreographer getChoreographer() {
        return this.mChoreographer;
    }

    void setAnimating(boolean z) {
        this.mAnimating = z;
    }

    void orAnimating(boolean z) {
        this.mAnimating = z | this.mAnimating;
    }

    void addAfterPrepareSurfacesRunnable(Runnable runnable) {
        if (this.mInExecuteAfterPrepareSurfacesRunnables) {
            runnable.run();
        } else {
            this.mAfterPrepareSurfacesRunnables.add(runnable);
            scheduleAnimation();
        }
    }

    void executeAfterPrepareSurfacesRunnables() {
        if (this.mInExecuteAfterPrepareSurfacesRunnables) {
            return;
        }
        this.mInExecuteAfterPrepareSurfacesRunnables = true;
        int size = this.mAfterPrepareSurfacesRunnables.size();
        for (int i = 0; i < size; i++) {
            this.mAfterPrepareSurfacesRunnables.get(i).run();
        }
        this.mAfterPrepareSurfacesRunnables.clear();
        this.mInExecuteAfterPrepareSurfacesRunnables = false;
    }
}
