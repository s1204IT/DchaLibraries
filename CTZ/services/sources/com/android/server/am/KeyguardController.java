package com.android.server.am;

import android.app.ActivityManagerInternal;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Trace;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.server.wm.WindowManagerService;
import java.io.PrintWriter;

class KeyguardController {
    private static final String TAG = "ActivityManager";
    private boolean mAodShowing;
    private int mBeforeUnoccludeTransit;
    private boolean mDismissalRequested;
    private ActivityRecord mDismissingKeyguardActivity;
    private boolean mKeyguardGoingAway;
    private boolean mKeyguardShowing;
    private boolean mOccluded;
    private int mSecondaryDisplayShowing = -1;
    private final ActivityManagerService mService;
    private ActivityManagerInternal.SleepToken mSleepToken;
    private final ActivityStackSupervisor mStackSupervisor;
    private int mVisibilityTransactionDepth;
    private WindowManagerService mWindowManager;

    KeyguardController(ActivityManagerService activityManagerService, ActivityStackSupervisor activityStackSupervisor) {
        this.mService = activityManagerService;
        this.mStackSupervisor = activityStackSupervisor;
    }

    void setWindowManager(WindowManagerService windowManagerService) {
        this.mWindowManager = windowManagerService;
    }

    boolean isKeyguardOrAodShowing(int i) {
        return (this.mKeyguardShowing || this.mAodShowing) && !this.mKeyguardGoingAway && (i != 0 ? i == this.mSecondaryDisplayShowing : !this.mOccluded);
    }

    boolean isKeyguardShowing(int i) {
        return this.mKeyguardShowing && !this.mKeyguardGoingAway && (i != 0 ? i == this.mSecondaryDisplayShowing : !this.mOccluded);
    }

    boolean isKeyguardLocked() {
        return this.mKeyguardShowing && !this.mKeyguardGoingAway;
    }

    boolean isKeyguardGoingAway() {
        return this.mKeyguardGoingAway && this.mKeyguardShowing;
    }

    void setKeyguardShown(boolean z, boolean z2, int i) {
        boolean z3 = ((z == this.mKeyguardShowing && z2 == this.mAodShowing) ? false : true) | (this.mKeyguardGoingAway && z);
        if (!z3 && i == this.mSecondaryDisplayShowing) {
            return;
        }
        this.mKeyguardShowing = z;
        this.mAodShowing = z2;
        this.mSecondaryDisplayShowing = i;
        this.mWindowManager.setAodShowing(z2);
        if (z3) {
            dismissDockedStackIfNeeded();
            setKeyguardGoingAway(false);
            this.mWindowManager.setKeyguardOrAodShowingOnDefaultDisplay(isKeyguardOrAodShowing(0));
            if (z) {
                this.mDismissalRequested = false;
            }
        }
        this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
        updateKeyguardSleepToken();
    }

    void keyguardGoingAway(int i) {
        if (!this.mKeyguardShowing) {
            return;
        }
        Trace.traceBegin(64L, "keyguardGoingAway");
        this.mWindowManager.deferSurfaceLayout();
        try {
            setKeyguardGoingAway(true);
            this.mWindowManager.prepareAppTransition(20, false, convertTransitFlags(i), false);
            updateKeyguardSleepToken();
            this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
            this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
            this.mStackSupervisor.addStartingWindowsForVisibleActivities(true);
            this.mWindowManager.executeAppTransition();
        } finally {
            Trace.traceBegin(64L, "keyguardGoingAway: surfaceLayout");
            this.mWindowManager.continueSurfaceLayout();
            Trace.traceEnd(64L);
            Trace.traceEnd(64L);
        }
    }

    void dismissKeyguard(IBinder iBinder, IKeyguardDismissCallback iKeyguardDismissCallback, CharSequence charSequence) {
        ActivityRecord activityRecordForTokenLocked = ActivityRecord.forTokenLocked(iBinder);
        if (activityRecordForTokenLocked == null || !activityRecordForTokenLocked.visibleIgnoringKeyguard) {
            failCallback(iKeyguardDismissCallback);
            return;
        }
        Slog.i(TAG, "Activity requesting to dismiss Keyguard: " + activityRecordForTokenLocked);
        if (activityRecordForTokenLocked.getTurnScreenOnFlag() && activityRecordForTokenLocked.isTopRunningActivity()) {
            this.mStackSupervisor.wakeUp("dismissKeyguard");
        }
        this.mWindowManager.dismissKeyguard(iKeyguardDismissCallback, charSequence);
    }

    private void setKeyguardGoingAway(boolean z) {
        this.mKeyguardGoingAway = z;
        this.mWindowManager.setKeyguardGoingAway(z);
    }

    private void failCallback(IKeyguardDismissCallback iKeyguardDismissCallback) {
        try {
            iKeyguardDismissCallback.onDismissError();
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to call callback", e);
        }
    }

    private int convertTransitFlags(int i) {
        int i2 = (i & 1) != 0 ? 1 : 0;
        if ((i & 2) != 0) {
            i2 |= 2;
        }
        if ((i & 4) != 0) {
            return i2 | 4;
        }
        return i2;
    }

    void beginActivityVisibilityUpdate() {
        this.mVisibilityTransactionDepth++;
    }

    void endActivityVisibilityUpdate() {
        this.mVisibilityTransactionDepth--;
        if (this.mVisibilityTransactionDepth == 0) {
            visibilitiesUpdated();
        }
    }

    boolean canShowActivityWhileKeyguardShowing(ActivityRecord activityRecord, boolean z) {
        return z && canDismissKeyguard() && !this.mAodShowing && (this.mDismissalRequested || activityRecord != this.mDismissingKeyguardActivity);
    }

    boolean canShowWhileOccluded(boolean z, boolean z2) {
        return z2 || (z && !this.mWindowManager.isKeyguardSecure());
    }

    private void visibilitiesUpdated() {
        boolean z = this.mOccluded;
        ActivityRecord activityRecord = this.mDismissingKeyguardActivity;
        this.mOccluded = false;
        this.mDismissingKeyguardActivity = null;
        for (int childCount = this.mStackSupervisor.getChildCount() - 1; childCount >= 0; childCount--) {
            ActivityDisplay childAt = this.mStackSupervisor.getChildAt(childCount);
            for (int childCount2 = childAt.getChildCount() - 1; childCount2 >= 0; childCount2--) {
                ActivityStack childAt2 = childAt.getChildAt(childCount2);
                if (childAt.mDisplayId == 0 && this.mStackSupervisor.isFocusedStack(childAt2)) {
                    ActivityRecord topDismissingKeyguardActivity = childAt2.getTopDismissingKeyguardActivity();
                    this.mOccluded = childAt2.topActivityOccludesKeyguard() || (topDismissingKeyguardActivity != null && childAt2.topRunningActivityLocked() == topDismissingKeyguardActivity && canShowWhileOccluded(true, false));
                }
                if (this.mDismissingKeyguardActivity == null && childAt2.getTopDismissingKeyguardActivity() != null) {
                    this.mDismissingKeyguardActivity = childAt2.getTopDismissingKeyguardActivity();
                }
            }
        }
        this.mOccluded |= this.mWindowManager.isShowingDream();
        if (this.mOccluded != z) {
            handleOccludedChanged();
        }
        if (this.mDismissingKeyguardActivity != activityRecord) {
            handleDismissKeyguard();
        }
    }

    private void handleOccludedChanged() {
        this.mWindowManager.onKeyguardOccludedChanged(this.mOccluded);
        if (isKeyguardLocked()) {
            this.mWindowManager.deferSurfaceLayout();
            try {
                this.mWindowManager.prepareAppTransition(resolveOccludeTransit(), false, 0, true);
                updateKeyguardSleepToken();
                this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
                this.mWindowManager.executeAppTransition();
            } finally {
                this.mWindowManager.continueSurfaceLayout();
            }
        }
        dismissDockedStackIfNeeded();
    }

    private void handleDismissKeyguard() {
        if (!this.mOccluded && this.mDismissingKeyguardActivity != null && this.mWindowManager.isKeyguardSecure()) {
            this.mWindowManager.dismissKeyguard(null, null);
            this.mDismissalRequested = true;
            if (this.mKeyguardShowing && canDismissKeyguard() && this.mWindowManager.getPendingAppTransition() == 23) {
                this.mWindowManager.prepareAppTransition(this.mBeforeUnoccludeTransit, false, 0, true);
                this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
                this.mWindowManager.executeAppTransition();
            }
        }
    }

    boolean canDismissKeyguard() {
        return this.mWindowManager.isKeyguardTrusted() || !this.mWindowManager.isKeyguardSecure();
    }

    private int resolveOccludeTransit() {
        if (this.mBeforeUnoccludeTransit != -1 && this.mWindowManager.getPendingAppTransition() == 23 && this.mOccluded) {
            return this.mBeforeUnoccludeTransit;
        }
        if (!this.mOccluded) {
            this.mBeforeUnoccludeTransit = this.mWindowManager.getPendingAppTransition();
            return 23;
        }
        return 22;
    }

    private void dismissDockedStackIfNeeded() {
        ActivityStack splitScreenPrimaryStack;
        if (!this.mKeyguardShowing || !this.mOccluded || (splitScreenPrimaryStack = this.mStackSupervisor.getDefaultDisplay().getSplitScreenPrimaryStack()) == null) {
            return;
        }
        this.mStackSupervisor.moveTasksToFullscreenStackLocked(splitScreenPrimaryStack, this.mStackSupervisor.mFocusedStack == splitScreenPrimaryStack);
    }

    private void updateKeyguardSleepToken() {
        if (this.mSleepToken == null && isKeyguardOrAodShowing(0)) {
            this.mSleepToken = this.mService.acquireSleepToken("Keyguard", 0);
        } else if (this.mSleepToken != null && !isKeyguardOrAodShowing(0)) {
            this.mSleepToken.release();
            this.mSleepToken = null;
        }
    }

    void dump(PrintWriter printWriter, String str) {
        printWriter.println(str + "KeyguardController:");
        printWriter.println(str + "  mKeyguardShowing=" + this.mKeyguardShowing);
        printWriter.println(str + "  mAodShowing=" + this.mAodShowing);
        printWriter.println(str + "  mKeyguardGoingAway=" + this.mKeyguardGoingAway);
        printWriter.println(str + "  mOccluded=" + this.mOccluded);
        printWriter.println(str + "  mDismissingKeyguardActivity=" + this.mDismissingKeyguardActivity);
        printWriter.println(str + "  mDismissalRequested=" + this.mDismissalRequested);
        printWriter.println(str + "  mVisibilityTransactionDepth=" + this.mVisibilityTransactionDepth);
    }

    void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1133871366145L, this.mKeyguardShowing);
        protoOutputStream.write(1133871366146L, this.mOccluded);
        protoOutputStream.end(jStart);
    }
}
