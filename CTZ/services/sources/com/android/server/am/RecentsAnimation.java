package com.android.server.am;

import android.app.ActivityOptions;
import android.app.AppOpsManager;
import android.app.IAssistDataReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.os.RemoteException;
import android.os.Trace;
import android.util.Slog;
import android.view.IRecentsAnimationRunner;
import com.android.server.am.ActivityDisplay;
import com.android.server.wm.RecentsAnimationController;
import com.android.server.wm.WindowManagerService;

class RecentsAnimation implements RecentsAnimationController.RecentsAnimationCallbacks, ActivityDisplay.OnStackOrderChangedListener {
    private static final boolean DEBUG = false;
    private static final String TAG = RecentsAnimation.class.getSimpleName();
    private final ActivityStartController mActivityStartController;
    private AssistDataRequester mAssistDataRequester;
    private final int mCallingPid;
    private final ActivityDisplay mDefaultDisplay;
    private ActivityStack mRestoreTargetBehindStack;
    private final ActivityManagerService mService;
    private final ActivityStackSupervisor mStackSupervisor;
    private int mTargetActivityType;
    private final UserController mUserController;
    private final WindowManagerService mWindowManager;

    RecentsAnimation(ActivityManagerService activityManagerService, ActivityStackSupervisor activityStackSupervisor, ActivityStartController activityStartController, WindowManagerService windowManagerService, UserController userController, int i) {
        this.mService = activityManagerService;
        this.mStackSupervisor = activityStackSupervisor;
        this.mDefaultDisplay = activityStackSupervisor.getDefaultDisplay();
        this.mActivityStartController = activityStartController;
        this.mWindowManager = windowManagerService;
        this.mUserController = userController;
        this.mCallingPid = i;
    }

    void startRecentsActivity(Intent intent, IRecentsAnimationRunner iRecentsAnimationRunner, ComponentName componentName, int i, IAssistDataReceiver iAssistDataReceiver) {
        ActivityRecord topActivity;
        ActivityStack activityStack;
        ?? r11;
        Trace.traceBegin(64L, "RecentsAnimation#startRecentsActivity");
        if (!this.mWindowManager.canStartRecentsAnimation()) {
            notifyAnimationCancelBeforeStart(iRecentsAnimationRunner);
            return;
        }
        this.mTargetActivityType = (intent.getComponent() == null || !componentName.equals(intent.getComponent())) ? 2 : 3;
        ActivityStack stack = this.mDefaultDisplay.getStack(0, this.mTargetActivityType);
        ActivityRecord targetActivity = getTargetActivity(stack, intent.getComponent());
        ?? r16 = targetActivity != null;
        if (r16 != false) {
            this.mRestoreTargetBehindStack = targetActivity.getDisplay().getStackAbove(stack);
            if (this.mRestoreTargetBehindStack == null) {
                notifyAnimationCancelBeforeStart(iRecentsAnimationRunner);
                return;
            }
        }
        if (targetActivity == null || !targetActivity.visible) {
            this.mStackSupervisor.sendPowerHintForLaunchStartIfNeeded(true, targetActivity);
        }
        this.mStackSupervisor.getActivityMetricsLogger().notifyActivityLaunching();
        this.mService.setRunningRemoteAnimation(this.mCallingPid, true);
        this.mWindowManager.deferSurfaceLayout();
        if (iAssistDataReceiver != null) {
            try {
                try {
                    topActivity = targetActivity;
                    activityStack = stack;
                    r11 = 0;
                    this.mAssistDataRequester = new AssistDataRequester(this.mService.mContext, this.mService, this.mWindowManager, (AppOpsManager) this.mService.mContext.getSystemService("appops"), new AssistDataReceiverProxy(iAssistDataReceiver, componentName.getPackageName()), this, 49, -1);
                    this.mAssistDataRequester.requestAssistData(this.mStackSupervisor.getTopVisibleActivities(), true, false, true, false, i, componentName.getPackageName());
                } catch (Exception e) {
                    Slog.e(TAG, "Failed to start recents activity", e);
                    throw e;
                }
            } catch (Throwable th) {
                this.mWindowManager.continueSurfaceLayout();
                Trace.traceEnd(64L);
                throw th;
            }
        } else {
            topActivity = targetActivity;
            activityStack = stack;
            r11 = 0;
        }
        if (r16 != false) {
            this.mDefaultDisplay.moveStackBehindBottomMostVisibleStack(activityStack);
            if (activityStack.topTask() != topActivity.getTask()) {
                activityStack.addTask(topActivity.getTask(), true, "startRecentsActivity");
            }
        } else {
            ActivityOptions activityOptionsMakeBasic = ActivityOptions.makeBasic();
            activityOptionsMakeBasic.setLaunchActivityType(this.mTargetActivityType);
            activityOptionsMakeBasic.setAvoidMoveToFront();
            intent.addFlags(268500992);
            this.mActivityStartController.obtainStarter(intent, "startRecentsActivity_noTargetActivity").setCallingUid(i).setCallingPackage(componentName.getPackageName()).setActivityOptions(SafeActivityOptions.fromBundle(activityOptionsMakeBasic.toBundle())).setMayWait(this.mUserController.getCurrentUserId()).execute();
            this.mWindowManager.prepareAppTransition(r11, r11);
            this.mWindowManager.executeAppTransition();
            topActivity = this.mDefaultDisplay.getStack(r11, this.mTargetActivityType).getTopActivity();
        }
        topActivity.mLaunchTaskBehind = true;
        this.mWindowManager.cancelRecentsAnimationSynchronously(2, "startRecentsActivity");
        this.mWindowManager.initializeRecentsAnimation(this.mTargetActivityType, iRecentsAnimationRunner, this, this.mDefaultDisplay.mDisplayId, this.mStackSupervisor.mRecentTasks.getRecentTaskIds());
        this.mStackSupervisor.ensureActivitiesVisibleLocked(null, r11 == true ? 1 : 0, true);
        this.mStackSupervisor.getActivityMetricsLogger().notifyActivityLaunched(2, topActivity);
        this.mDefaultDisplay.registerStackOrderChangedListener(this);
        this.mWindowManager.continueSurfaceLayout();
        Trace.traceEnd(64L);
    }

    private void finishAnimation(@RecentsAnimationController.ReorderMode final int i) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (this.mAssistDataRequester != null) {
                    this.mAssistDataRequester.cancel();
                    this.mAssistDataRequester = null;
                }
                this.mDefaultDisplay.unregisterStackOrderChangedListener(this);
                if (this.mWindowManager.getRecentsAnimationController() == null) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (i != 0) {
                    this.mStackSupervisor.sendPowerHintForLaunchEndIfNeeded();
                }
                this.mService.setRunningRemoteAnimation(this.mCallingPid, false);
                this.mWindowManager.inSurfaceTransaction(new Runnable() {
                    @Override
                    public final void run() {
                        RecentsAnimation.lambda$finishAnimation$0(this.f$0, i);
                    }
                });
                ActivityManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public static void lambda$finishAnimation$0(RecentsAnimation recentsAnimation, int i) {
        Trace.traceBegin(64L, "RecentsAnimation#onAnimationFinished_inSurfaceTransaction");
        recentsAnimation.mWindowManager.deferSurfaceLayout();
        try {
            try {
                recentsAnimation.mWindowManager.cleanupRecentsAnimation(i);
                ActivityStack stack = recentsAnimation.mDefaultDisplay.getStack(0, recentsAnimation.mTargetActivityType);
                ActivityRecord topActivity = stack != null ? stack.getTopActivity() : null;
                if (topActivity == null) {
                    return;
                }
                topActivity.mLaunchTaskBehind = false;
                if (i == 1) {
                    recentsAnimation.mStackSupervisor.mNoAnimActivities.add(topActivity);
                    stack.moveToFront("RecentsAnimation.onAnimationFinished()");
                } else if (i != 2) {
                    return;
                } else {
                    topActivity.getDisplay().moveStackBehindStack(stack, recentsAnimation.mRestoreTargetBehindStack);
                }
                recentsAnimation.mWindowManager.prepareAppTransition(0, false);
                recentsAnimation.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
                recentsAnimation.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                recentsAnimation.mWindowManager.executeAppTransition();
                recentsAnimation.mWindowManager.checkSplitScreenMinimizedChanged(true);
                return;
            } catch (Exception e) {
                Slog.e(TAG, "Failed to clean up recents activity", e);
                throw e;
            }
        } finally {
            recentsAnimation.mWindowManager.continueSurfaceLayout();
            Trace.traceEnd(64L);
        }
        recentsAnimation.mWindowManager.continueSurfaceLayout();
        Trace.traceEnd(64L);
    }

    @Override
    public void onAnimationFinished(@RecentsAnimationController.ReorderMode final int i, boolean z) {
        if (z) {
            finishAnimation(i);
        } else {
            this.mService.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.finishAnimation(i);
                }
            });
        }
    }

    @Override
    public void onStackOrderChanged() {
        this.mWindowManager.cancelRecentsAnimationSynchronously(0, "stackOrderChanged");
    }

    private void notifyAnimationCancelBeforeStart(IRecentsAnimationRunner iRecentsAnimationRunner) {
        try {
            iRecentsAnimationRunner.onAnimationCanceled();
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to cancel recents animation before start", e);
        }
    }

    private ActivityStack getTopNonAlwaysOnTopStack() {
        for (int childCount = this.mDefaultDisplay.getChildCount() - 1; childCount >= 0; childCount--) {
            ActivityStack childAt = this.mDefaultDisplay.getChildAt(childCount);
            if (!childAt.getWindowConfiguration().isAlwaysOnTop()) {
                return childAt;
            }
        }
        return null;
    }

    private ActivityRecord getTargetActivity(ActivityStack activityStack, ComponentName componentName) {
        if (activityStack == null) {
            return null;
        }
        for (int childCount = activityStack.getChildCount() - 1; childCount >= 0; childCount--) {
            TaskRecord taskRecord = (TaskRecord) activityStack.getChildAt(childCount);
            if (taskRecord.getBaseIntent().getComponent().equals(componentName)) {
                return taskRecord.getTopActivity();
            }
        }
        return activityStack.getTopActivity();
    }
}
